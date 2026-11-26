package com.pocketdev.advance_erp.data.repository

import androidx.room.withTransaction
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.engine.CustomerLedgerAdjustment
import com.pocketdev.advance_erp.core.engine.SupplierLedgerAdjustment
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.dao.CustomerDao
import com.pocketdev.advance_erp.data.dao.CustomerPaymentDao
import com.pocketdev.advance_erp.data.dao.PurchaseOrderDao
import com.pocketdev.advance_erp.data.dao.SalesDao
import com.pocketdev.advance_erp.data.dao.SalesReturnDao
import com.pocketdev.advance_erp.data.dao.SupplierDao
import com.pocketdev.advance_erp.data.dao.SupplierPaymentDao
import com.pocketdev.advance_erp.data.entity.AccountType
import com.pocketdev.advance_erp.data.entity.AuditLog
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.NormalBalance
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.domain.repository.AccountingRepository
import com.pocketdev.advance_erp.domain.repository.ContactsRepository
import com.pocketdev.advance_erp.domain.repository.CustomerDeleteResult
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    private val db: AdvanceErpDatabase,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val salesDao: SalesDao,
    private val customerPaymentDao: CustomerPaymentDao,
    private val salesReturnDao: SalesReturnDao,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val supplierPaymentDao: SupplierPaymentDao,
    private val accountingRepository: AccountingRepository
) : ContactsRepository {

    override fun getAllCustomers(): Flow<List<Customer>> = customerDao.getAllCustomers()

    override fun getAllSuppliers(): Flow<List<Supplier>> = supplierDao.getAllSuppliers()

    override fun getTotalCustomerReceivedMinor(): Flow<Long> = customerPaymentDao.getTotalCollectedMinor()

    override fun getTotalSupplierPaidBdtMinor(): Flow<Long> = supplierPaymentDao.getTotalPaidBdtMinor()

    override suspend fun addCustomer(customer: Customer) {
        customerDao.insertCustomer(customer)
    }

    override suspend fun updateCustomer(customer: Customer) {
        db.withTransaction {
            val existing = customerDao.getCustomerById(customer.id)
                ?: throw IllegalArgumentException("Customer not found: ${customer.id}")
            val updated = customer.copy(
                outstandingBalanceMinor = existing.outstandingBalanceMinor,
                createdAt = existing.createdAt,
                isActive = existing.isActive
            )
            customerDao.updateCustomer(updated)
            db.auditLogDao().insertAuditLog(
                AuditLog(
                    recordType = "CUSTOMER",
                    recordId = updated.id,
                    action = "UPDATE",
                    details = "Customer '${updated.name}' (${updated.customerCode}) profile updated.",
                    operator = "SYSTEM_ADMIN",
                    documentNumber = updated.customerCode,
                    operationType = "UPDATE"
                )
            )
        }
    }

    override suspend fun addSupplier(supplier: Supplier) {
        supplierDao.insertSupplier(supplier)
    }

    override suspend fun canChangeSupplierCurrency(supplierId: String): Boolean {
        val activeOrders = purchaseOrderDao.getLedgerOrdersSync(supplierId)
            .any { it.status != "CANCELLED" }
        if (activeOrders) return false
        return supplierPaymentDao.getLedgerPaymentsSync(supplierId).isEmpty()
    }

    override suspend fun updateSupplier(supplier: Supplier) {
        db.withTransaction {
            val existing = supplierDao.getSupplierById(supplier.id)
                ?: throw IllegalArgumentException("Supplier not found: ${supplier.id}")
            if (!existing.currency.equals(supplier.currency, ignoreCase = true)) {
                require(canChangeSupplierCurrency(supplier.id)) {
                    "সরবরাহকারী '${existing.companyName}'-এর নামে ইতিমধ্যে ক্রয় আদেশ (PO) বা পেমেন্ট থাকায় মুদ্রা (Currency) পরিবর্তন করা যাবে না।"
                }
            }
            val updated = supplier.copy(
                outstandingPayableMinor = existing.outstandingPayableMinor,
                createdAt = existing.createdAt,
                isActive = existing.isActive
            )
            supplierDao.updateSupplier(updated)
            db.auditLogDao().insertAuditLog(
                AuditLog(
                    recordType = "SUPPLIER",
                    recordId = updated.id,
                    action = "UPDATE",
                    details = "Supplier '${updated.companyName}' (${updated.supplierCode}) profile updated.",
                    operator = "SYSTEM_ADMIN",
                    documentNumber = updated.supplierCode,
                    operationType = "UPDATE"
                )
            )
        }
    }

    private suspend fun ensureOpeningAccountsExist(vararg ids: String) {
        val accountingDao = db.accountingDao()
        ids.forEach { accountId ->
            if (accountingDao.getAccountById(accountId) == null) {
                val fallback = when (accountId) {
                    "coa-1040" -> ChartOfAccount(
                        id = "coa-1040",
                        accountCode = "1040",
                        nameEn = "Accounts Receivable (Trade Debtors)",
                        nameBn = "প্রাপ্য হিসাব (ব্যবসায়িক দেনাদার)",
                        accountType = AccountType.ASSET.name,
                        normalBalance = NormalBalance.DEBIT.name
                    )
                    "coa-2010" -> ChartOfAccount(
                        id = "coa-2010",
                        accountCode = "2010",
                        nameEn = "Accounts Payable (China Suppliers)",
                        nameBn = "প্রদেয় হিসাব (চীন সরবরাহকারী)",
                        accountType = AccountType.LIABILITY.name,
                        normalBalance = NormalBalance.CREDIT.name
                    )
                    "coa-3010" -> ChartOfAccount(
                        id = "coa-3010",
                        accountCode = "3010",
                        nameEn = "Owner's Capital & Retained Equity",
                        nameBn = "মালিকের মূলধন ও সংরক্ষিত তহবিল",
                        accountType = AccountType.EQUITY.name,
                        normalBalance = NormalBalance.CREDIT.name
                    )
                    else -> null
                }
                fallback?.let { accountingDao.insertAccount(it) }
            }
        }
    }

    override suspend fun recordCustomerOpeningDue(
        customerId: String,
        amountMinor: Long,
        entryDate: Long,
        note: String?
    ): String {
        require(amountMinor > 0L) { "পূর্বের বকেয়ার পরিমাণ শূন্যের বেশি হতে হবে" }
        return db.withTransaction {
            val customer = customerDao.getCustomerById(customerId)
                ?: throw IllegalArgumentException("Customer not found: $customerId")
            ensureOpeningAccountsExist("coa-1040", "coa-3010")
            val cleanNote = note?.trim()?.takeIf { it.isNotBlank() }
            val noteSuffix = cleanNote?.let { " ($it)" } ?: ""
            val amountFormatted = Formatters.formatMinorUnit(amountMinor)
            val entryId = accountingRepository.postJournalEntry(
                entryDate = entryDate,
                descEn = "Opening Due / Previous Receivable for ${customer.name} (${customer.customerCode})$noteSuffix",
                descBn = "গ্রাহক ${customer.name}-এর পূর্বের বকেয়া যোগ (৳$amountFormatted)$noteSuffix",
                referenceType = CustomerLedgerAdjustment.DEBIT,
                referenceId = customer.id,
                lines = listOf(
                    JournalLineDraft(
                        accountId = "coa-1040",
                        isDebit = true,
                        amountMinor = amountMinor,
                        description = "Customer opening receivable: ${customer.name}$noteSuffix"
                    ),
                    JournalLineDraft(
                        accountId = "coa-3010",
                        isDebit = false,
                        amountMinor = amountMinor,
                        description = "Opening equity offset for customer due: ${customer.name}$noteSuffix"
                    )
                )
            )
            val newBalance = Math.addExact(customer.outstandingBalanceMinor, amountMinor)
            customerDao.updateOutstandingBalance(customer.id, newBalance)
            db.auditLogDao().insertAuditLog(
                AuditLog(
                    recordType = "CUSTOMER",
                    recordId = customer.id,
                    action = "OPENING_DUE",
                    details = "Recorded opening receivable of ৳$amountFormatted for '${customer.name}' (${customer.customerCode}).$noteSuffix",
                    operator = "SYSTEM_ADMIN",
                    documentNumber = customer.customerCode,
                    operationType = "OPENING_DUE"
                )
            )
            entryId
        }
    }

    override suspend fun recordSupplierOpeningPayable(
        supplierId: String,
        amountBdtMinor: Long,
        entryDate: Long,
        note: String?
    ): String {
        require(amountBdtMinor > 0L) { "পূর্বের প্রদেয় বকেয়ার পরিমাণ শূন্যের বেশি হতে হবে" }
        return db.withTransaction {
            val supplier = supplierDao.getSupplierById(supplierId)
                ?: throw IllegalArgumentException("Supplier not found: $supplierId")
            ensureOpeningAccountsExist("coa-2010", "coa-3010")
            val cleanNote = note?.trim()?.takeIf { it.isNotBlank() }
            val noteSuffix = cleanNote?.let { " ($it)" } ?: ""
            val amountFormatted = Formatters.formatMinorUnit(amountBdtMinor)
            val entryId = accountingRepository.postJournalEntry(
                entryDate = entryDate,
                descEn = "Opening Payable / Previous Supplier Due for ${supplier.companyName} (${supplier.supplierCode})$noteSuffix",
                descBn = "সরবরাহকারী ${supplier.companyName}-এর পূর্বের প্রদেয় বকেয়া যোগ (৳$amountFormatted)$noteSuffix",
                referenceType = SupplierLedgerAdjustment.CREDIT,
                referenceId = supplier.id,
                lines = listOf(
                    JournalLineDraft(
                        accountId = "coa-3010",
                        isDebit = true,
                        amountMinor = amountBdtMinor,
                        description = "Opening equity offset for supplier payable: ${supplier.companyName}$noteSuffix"
                    ),
                    JournalLineDraft(
                        accountId = "coa-2010",
                        isDebit = false,
                        amountMinor = amountBdtMinor,
                        description = "Supplier opening payable: ${supplier.companyName}$noteSuffix"
                    )
                )
            )
            val newPayable = Math.addExact(supplier.outstandingPayableMinor, amountBdtMinor)
            supplierDao.updateOutstandingPayable(supplier.id, newPayable)
            db.auditLogDao().insertAuditLog(
                AuditLog(
                    recordType = "SUPPLIER",
                    recordId = supplier.id,
                    action = "OPENING_PAYABLE",
                    details = "Recorded opening payable of ৳$amountFormatted for '${supplier.companyName}' (${supplier.supplierCode}).$noteSuffix",
                    operator = "SYSTEM_ADMIN",
                    documentNumber = supplier.supplierCode,
                    operationType = "OPENING_PAYABLE"
                )
            )
            entryId
        }
    }

    override suspend fun addCustomerWithOptionalOpeningDue(
        customer: Customer,
        openingDueMinor: Long,
        entryDate: Long,
        openingNote: String?
    ) {
        db.withTransaction {
            customerDao.insertCustomer(customer.copy(outstandingBalanceMinor = 0L))
            if (openingDueMinor > 0L) {
                recordCustomerOpeningDue(customer.id, openingDueMinor, entryDate, openingNote)
            }
        }
    }

    override suspend fun addSupplierWithOptionalOpeningPayable(
        supplier: Supplier,
        openingPayableBdtMinor: Long,
        entryDate: Long,
        openingNote: String?
    ) {
        db.withTransaction {
            supplierDao.insertSupplier(supplier.copy(outstandingPayableMinor = 0L))
            if (openingPayableBdtMinor > 0L) {
                recordSupplierOpeningPayable(supplier.id, openingPayableBdtMinor, entryDate, openingNote)
            }
        }
    }

    override suspend fun deleteOrArchiveCustomer(customerId: String, operator: String): CustomerDeleteResult {
        val customer = customerDao.getCustomerById(customerId)
            ?: return CustomerDeleteResult.Blocked("Customer not found: $customerId", 0L)

        // 1. Block if outstanding balance is non-zero
        if (customer.outstandingBalanceMinor != 0L) {
            val balanceFormatted = "৳" + String.format("%.2f", customer.outstandingBalanceMinor / 100.0)
            return CustomerDeleteResult.Blocked(
                reason = "Cannot delete customer '${customer.name}'. Outstanding due balance is $balanceFormatted. Settle all dues first.",
                balanceMinor = customer.outstandingBalanceMinor
            )
        }

        // 2. Check financial history: invoices, payments, returns
        val invoiceCount = salesDao.getInvoiceCountForCustomer(customerId)
        val paymentCount = customerPaymentDao.getPaymentCountForCustomer(customerId)
        val returnCount = salesReturnDao.getReturnCountForCustomer(customerId)
        val totalHistoryCount = invoiceCount + paymentCount + returnCount

        return db.withTransaction {
            if (totalHistoryCount > 0) {
                // Archive / Soft Delete
                customerDao.archiveCustomer(customerId)
                db.auditLogDao().insertAuditLog(
                    AuditLog(
                        recordType = "CUSTOMER",
                        recordId = customerId,
                        action = "ARCHIVE",
                        details = "Customer '${customer.name}' (${customer.customerCode}) archived. Found $totalHistoryCount financial records ($invoiceCount invoices, $paymentCount payments, $returnCount returns). Historical references preserved.",
                        operator = operator,
                        documentNumber = customer.customerCode,
                        operationType = "ARCHIVE",
                        previousStatus = "ACTIVE",
                        newStatus = "ARCHIVED"
                    )
                )
                CustomerDeleteResult.Archived(
                    message = "Customer '${customer.name}' has $totalHistoryCount historical transactions. Customer archived safely to preserve ledger audit trail.",
                    transactionCount = totalHistoryCount
                )
            } else {
                // Hard delete - absolutely no financial history
                customerDao.hardDeleteCustomer(customerId)
                db.auditLogDao().insertAuditLog(
                    AuditLog(
                        recordType = "CUSTOMER",
                        recordId = customerId,
                        action = "HARD_DELETE",
                        details = "Customer '${customer.name}' (${customer.customerCode}) permanently deleted (0 transactions found).",
                        operator = operator,
                        documentNumber = customer.customerCode,
                        operationType = "HARD_DELETE",
                        previousStatus = "ACTIVE",
                        newStatus = "DELETED"
                    )
                )
                CustomerDeleteResult.HardDeleted
            }
        }
    }

    override suspend fun deleteOrArchiveSupplier(supplierId: String, operator: String): CustomerDeleteResult {
        val supplier = supplierDao.getSupplierById(supplierId)
            ?: return CustomerDeleteResult.Blocked("Supplier not found: $supplierId", 0L)

        if (supplier.outstandingPayableMinor != 0L) {
            val balanceFormatted = "৳" + Formatters.formatMinorUnit(supplier.outstandingPayableMinor)
            return CustomerDeleteResult.Blocked(
                reason = "Cannot delete supplier '${supplier.companyName}'. Outstanding payable is $balanceFormatted. Settle all dues first.",
                balanceMinor = supplier.outstandingPayableMinor
            )
        }

        val poCount = purchaseOrderDao.getLedgerOrdersSync(supplierId).size
        val paymentCount = supplierPaymentDao.getLedgerPaymentsSync(supplierId).size
        val totalHistoryCount = poCount + paymentCount

        return db.withTransaction {
            if (totalHistoryCount > 0) {
                supplierDao.archiveSupplier(supplierId)
                db.auditLogDao().insertAuditLog(
                    AuditLog(
                        recordType = "SUPPLIER",
                        recordId = supplierId,
                        action = "ARCHIVE",
                        details = "Supplier '${supplier.companyName}' (${supplier.supplierCode}) archived. Found $totalHistoryCount financial records ($poCount POs, $paymentCount payments). Historical references preserved.",
                        operator = operator,
                        documentNumber = supplier.supplierCode,
                        operationType = "ARCHIVE",
                        previousStatus = "ACTIVE",
                        newStatus = "ARCHIVED"
                    )
                )
                CustomerDeleteResult.Archived(
                    message = "Supplier '${supplier.companyName}' has $totalHistoryCount historical transactions. Safely archived to preserve audit trail.",
                    transactionCount = totalHistoryCount
                )
            } else {
                supplierDao.hardDeleteSupplier(supplierId)
                db.auditLogDao().insertAuditLog(
                    AuditLog(
                        recordType = "SUPPLIER",
                        recordId = supplierId,
                        action = "HARD_DELETE",
                        details = "Supplier '${supplier.companyName}' (${supplier.supplierCode}) permanently deleted (0 transactions found).",
                        operator = operator,
                        documentNumber = supplier.supplierCode,
                        operationType = "HARD_DELETE",
                        previousStatus = "ACTIVE",
                        newStatus = "DELETED"
                    )
                )
                CustomerDeleteResult.HardDeleted
            }
        }
    }
}
