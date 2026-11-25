package com.pocketdev.advance_erp.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.engine.CustomerLedgerAdjustment
import com.pocketdev.advance_erp.core.engine.ImportVaultAnalyticsEngine
import com.pocketdev.advance_erp.core.security.ImportVaultAccessGuard
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.core.util.ImportVaultAttachmentFileStore
import com.pocketdev.advance_erp.data.entity.ExpenseAllocation
import com.pocketdev.advance_erp.data.entity.ExpenseAllocationRule
import com.pocketdev.advance_erp.data.entity.ImportVaultAttachment
import com.pocketdev.advance_erp.data.entity.ImportVaultEntry
import com.pocketdev.advance_erp.data.entity.InvoiceStatus
import com.pocketdev.advance_erp.data.entity.ReturnRefundOption
import com.pocketdev.advance_erp.domain.model.ImportVaultDataset
import com.pocketdev.advance_erp.domain.model.ImportVaultExportSource
import com.pocketdev.advance_erp.domain.model.ImportVaultFilter
import com.pocketdev.advance_erp.domain.model.ImportVaultFilterOptions
import com.pocketdev.advance_erp.domain.model.ImportVaultOverview
import com.pocketdev.advance_erp.domain.model.ProductVaultRow
import com.pocketdev.advance_erp.domain.model.PurchaseOrderVaultDetail
import com.pocketdev.advance_erp.domain.model.ShipmentVaultDetail
import com.pocketdev.advance_erp.domain.model.VaultScopeType
import com.pocketdev.advance_erp.domain.repository.ImportVaultRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportVaultRepositoryImpl @Inject constructor(
    private val db: AdvanceErpDatabase,
    @ApplicationContext private val context: Context
) : ImportVaultRepository {

    override suspend fun getOverview(role: UserRole, filter: ImportVaultFilter): ImportVaultOverview {
        ImportVaultAccessGuard.requireVaultAccess(role)
        val dataset = loadDataset()
        return withContext(Dispatchers.Default) {
            ImportVaultAnalyticsEngine.computeOverview(dataset, filter)
        }
    }

    override suspend fun getFilterOptions(role: UserRole): ImportVaultFilterOptions {
        ImportVaultAccessGuard.requireVaultAccess(role)
        return withContext(Dispatchers.IO) {
            val suppliers = db.supplierDao().getAllSuppliersIncludingInactiveSync()
                .map { it.id to it.companyName }
                .sortedBy { it.second.lowercase() }
            val warehouses = db.warehouseDao().getAllWarehousesIncludingInactiveSync()
                .map { it.id to it.name }
                .sortedBy { it.second.lowercase() }
            val rules = db.importVaultDao().getAllExpenseAllocationRulesSync()
            ImportVaultFilterOptions(
                suppliers = suppliers,
                warehouses = warehouses,
                allocationRules = rules
            )
        }
    }

    override suspend fun getShipmentDetail(
        role: UserRole,
        shipmentId: String,
        filter: ImportVaultFilter
    ): ShipmentVaultDetail? {
        ImportVaultAccessGuard.requireVaultAccess(role)
        val dataset = loadDataset()
        return withContext(Dispatchers.Default) {
            ImportVaultAnalyticsEngine.computeShipmentDetail(dataset, shipmentId, filter)
        }
    }

    override suspend fun getPurchaseOrderDetail(
        role: UserRole,
        purchaseOrderId: String,
        filter: ImportVaultFilter
    ): PurchaseOrderVaultDetail? {
        ImportVaultAccessGuard.requireVaultAccess(role)
        val dataset = loadDataset()
        return withContext(Dispatchers.Default) {
            ImportVaultAnalyticsEngine.computePurchaseOrderDetail(dataset, purchaseOrderId, filter)
        }
    }

    override suspend fun getProductProfitability(
        role: UserRole,
        filter: ImportVaultFilter
    ): List<ProductVaultRow> {
        ImportVaultAccessGuard.requireVaultAccess(role)
        val dataset = loadDataset()
        return withContext(Dispatchers.Default) {
            ImportVaultAnalyticsEngine.computeProductProfitability(dataset, filter)
        }
    }

    override suspend fun loadExportSource(role: UserRole): ImportVaultExportSource {
        ImportVaultAccessGuard.requireVaultAccess(role)
        val dataset = loadDataset()
        val profile = withContext(Dispatchers.IO) {
            db.businessProfileDao().getProfileSync()
        }
        return ImportVaultExportSource(dataset = dataset, businessProfile = profile)
    }

    override suspend fun syncVaultEntries(role: UserRole) {
        ImportVaultAccessGuard.requireVaultAccess(role)
        withContext(Dispatchers.IO) {
            db.withTransaction { syncVaultEntriesInternal() }
        }
    }

    override fun observeVaultEntries(role: UserRole): Flow<List<ImportVaultEntry>> {
        ImportVaultAccessGuard.requireVaultAccess(role)
        return db.importVaultDao().getAllVaultEntries()
    }

    override suspend fun updateVaultEntryMetadata(
        role: UserRole,
        scopeType: VaultScopeType,
        sourceId: String,
        archiveStatus: String?,
        isPinned: Boolean?,
        tagsJson: String?,
        notes: String?,
        displayTitle: String?
    ) {
        ImportVaultAccessGuard.requireVaultAccess(role)
        withContext(Dispatchers.IO) {
            db.withTransaction {
                syncVaultEntriesInternal()
                val existing = db.importVaultDao().getVaultEntryByScopeAndSource(scopeType.name, sourceId)
                val now = System.currentTimeMillis()
                if (existing != null) {
                    db.importVaultDao().updateVaultEntry(
                        existing.copy(
                            displayTitle = displayTitle?.trim()?.takeIf { it.isNotEmpty() } ?: existing.displayTitle,
                            archiveStatus = archiveStatus ?: existing.archiveStatus,
                            isPinned = isPinned ?: existing.isPinned,
                            tagsJson = if (tagsJson != null) tagsJson.ifBlank { null } else existing.tagsJson,
                            notes = if (notes != null) notes.ifBlank { null } else existing.notes,
                            updatedAt = now
                        )
                    )
                } else {
                    db.importVaultDao().upsertVaultEntry(
                        ImportVaultEntry(
                            id = "ive-${scopeType.name.lowercase()}-$sourceId",
                            scopeType = scopeType.name,
                            sourceId = sourceId,
                            displayTitle = displayTitle?.trim()?.takeIf { it.isNotEmpty() } ?: sourceId,
                            archiveStatus = archiveStatus ?: "ACTIVE",
                            isPinned = isPinned ?: false,
                            tagsJson = tagsJson?.ifBlank { null },
                            notes = notes?.ifBlank { null },
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
            }
        }
    }

    private suspend fun syncVaultEntriesInternal() {
        val now = System.currentTimeMillis()
        val shipments = db.importShipmentDao().getAllShipmentsSync()
        val orders = db.purchaseOrderDao().getAllOrdersSync()
        val vaultDao = db.importVaultDao()
        for (shp in shipments) {
            vaultDao.insertVaultEntryIgnore(
                ImportVaultEntry(
                    id = "ive-shp-${shp.id}",
                    scopeType = VaultScopeType.SHIPMENT.name,
                    sourceId = shp.id,
                    displayTitle = shp.shipmentNumber,
                    archiveStatus = "ACTIVE",
                    createdAt = shp.createdAt,
                    updatedAt = now
                )
            )
        }
        for (po in orders) {
            vaultDao.insertVaultEntryIgnore(
                ImportVaultEntry(
                    id = "ive-po-${po.id}",
                    scopeType = VaultScopeType.PURCHASE_ORDER.name,
                    sourceId = po.id,
                    displayTitle = po.poNumber,
                    archiveStatus = "ACTIVE",
                    createdAt = po.createdAt,
                    updatedAt = now
                )
            )
        }
    }

    override fun observeAttachments(
        role: UserRole,
        scopeType: String,
        sourceId: String
    ): Flow<List<ImportVaultAttachment>> {
        ImportVaultAccessGuard.requireVaultAccess(role)
        return db.importVaultDao().getAttachmentsForSource(scopeType, sourceId)
    }

    override suspend fun addAttachment(
        role: UserRole,
        scopeType: String,
        sourceId: String,
        category: String,
        title: String,
        filePath: String,
        mimeType: String,
        fileSizeBytes: Long,
        isImmutableSnapshot: Boolean,
        createdBy: String
    ): String {
        ImportVaultAccessGuard.requireVaultAccess(role)
        val attachmentId = "iva-${UUID.randomUUID()}"
        withContext(Dispatchers.IO) {
            db.withTransaction {
                syncVaultEntriesInternal()
                val entry = if (scopeType == VaultScopeType.SHIPMENT.name || scopeType == VaultScopeType.PURCHASE_ORDER.name) {
                    db.importVaultDao().getVaultEntryByScopeAndSource(scopeType, sourceId)
                } else null
                db.importVaultDao().insertVaultAttachment(
                    ImportVaultAttachment(
                        id = attachmentId,
                        vaultEntryId = entry?.id,
                        scopeType = scopeType,
                        sourceId = sourceId,
                        category = category,
                        title = title,
                        filePath = filePath,
                        mimeType = mimeType,
                        fileSizeBytes = fileSizeBytes,
                        isImmutableSnapshot = isImmutableSnapshot,
                        createdBy = createdBy,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
        return attachmentId
    }

    override suspend fun deleteAttachment(role: UserRole, attachmentId: String) {
        ImportVaultAccessGuard.requireVaultAccess(role)
        withContext(Dispatchers.IO) {
            val existing = db.importVaultDao().getVaultAttachmentByIdSync(attachmentId)
            db.withTransaction {
                db.importVaultDao().deleteVaultAttachment(attachmentId)
            }
            if (existing != null) {
                ImportVaultAttachmentFileStore.deleteManagedAttachmentFile(
                    filesDir = context.filesDir,
                    filePath = existing.filePath,
                    role = role
                )
            }
        }
    }

    override fun observeExpenseAllocationRules(role: UserRole): Flow<List<ExpenseAllocationRule>> {
        ImportVaultAccessGuard.requireVaultAccess(role)
        return db.importVaultDao().getAllExpenseAllocationRules()
    }

    override suspend fun createOrActivateExpenseAllocationRule(
        role: UserRole,
        ruleName: String,
        allocationBasis: String,
        periodStartMillis: Long,
        periodEndMillis: Long,
        targetScopeType: VaultScopeType,
        activateImmediately: Boolean,
        notes: String?
    ): String {
        ImportVaultAccessGuard.requireVaultAccess(role)
        val ruleId = "ear-${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val rule = ExpenseAllocationRule(
            id = ruleId,
            ruleName = ruleName,
            ruleVersion = 1,
            allocationBasis = allocationBasis,
            periodStartMillis = periodStartMillis,
            periodEndMillis = periodEndMillis,
            targetScopeType = targetScopeType.name,
            isActive = activateImmediately,
            notes = notes,
            createdBy = role.code,
            createdAt = now,
            updatedAt = now
        )
        val dataset = loadDataset()
        val allocations = ImportVaultAnalyticsEngine.previewExpenseRuleAllocations(dataset, rule, now)
        db.withTransaction {
            if (activateImmediately) {
                db.importVaultDao().getAllExpenseAllocationRulesSync().forEach { existing ->
                    if (existing.isActive) {
                        db.importVaultDao().setExpenseAllocationRuleActive(existing.id, false, now)
                    }
                }
            }
            db.importVaultDao().insertExpenseAllocationRule(rule)
            db.importVaultDao().deleteExpenseAllocationsForRule(ruleId)
            if (allocations.isNotEmpty()) {
                db.importVaultDao().insertExpenseAllocations(allocations)
            }
        }
        return ruleId
    }

    override suspend fun setExpenseAllocationRuleActive(role: UserRole, ruleId: String, isActive: Boolean) {
        ImportVaultAccessGuard.requireVaultAccess(role)
        db.withTransaction {
            val now = System.currentTimeMillis()
            if (isActive) {
                db.importVaultDao().getAllExpenseAllocationRulesSync().forEach { existing ->
                    if (existing.id != ruleId && existing.isActive) {
                        db.importVaultDao().setExpenseAllocationRuleActive(existing.id, false, now)
                    }
                }
            }
            db.importVaultDao().setExpenseAllocationRuleActive(ruleId, isActive, now)
        }
    }

    override suspend fun previewRuleAllocations(
        role: UserRole,
        rule: ExpenseAllocationRule
    ): List<ExpenseAllocation> {
        ImportVaultAccessGuard.requireVaultAccess(role)
        val dataset = loadDataset()
        return withContext(Dispatchers.Default) {
            ImportVaultAnalyticsEngine.previewExpenseRuleAllocations(dataset, rule)
        }
    }

    private suspend fun loadDataset(): ImportVaultDataset = withContext(Dispatchers.IO) {
        val customers = db.customerDao().getLedgerCustomersSync()
        val invoices = db.salesDao().getAllInvoicesSync()
        val payments = db.customerPaymentDao().getAllPaymentsSync()
        val returns = db.salesReturnDao().getAllReturnsSync()
        val accountingDao = db.accountingDao()

        var openingDueTotal = 0L
        for (cust in customers) {
            if (cust.outstandingBalanceMinor <= 0L) continue
            val postedInvs = invoices.filter {
                it.customerId == cust.id && it.isPosted && it.status != InvoiceStatus.CANCELLED.name
            }.sumOf { it.totalAmountMinor }
            val activePays = payments.filter {
                it.customerId == cust.id && it.isPosted && !it.isVoided
            }.sumOf { it.amountMinor }
            val creditRets = returns.filter {
                it.customerId == cust.id && it.isPosted && it.refundOption == ReturnRefundOption.CUSTOMER_CREDIT.name
            }.sumOf { it.totalReturnAmountMinor }
            val adjs = accountingDao.getCustomerLedgerAdjustments(cust.id)
                .sumOf { CustomerLedgerAdjustment.delta(it) }
            val txNet = postedInvs - activePays - creditRets + adjs
            openingDueTotal += maxOf(0L, cust.outstandingBalanceMinor - txNet)
        }

        ImportVaultDataset(
            products = db.productDao().getAllProductsIncludingInactiveSync(),
            variants = db.productVariantDao().getAllVariantsIncludingInactiveSync(),
            categories = db.productCategoryDao().getAllCategoriesSync(),
            warehouses = db.warehouseDao().getAllWarehousesIncludingInactiveSync(),
            suppliers = db.supplierDao().getAllSuppliersIncludingInactiveSync(),
            purchaseOrders = db.purchaseOrderDao().getAllOrdersSync(),
            purchaseOrderItems = db.purchaseOrderDao().getAllOrderItemsSync(),
            supplierPayments = db.supplierPaymentDao().getAllPaymentsSync(),
            shipments = db.importShipmentDao().getAllShipmentsSync(),
            cartons = db.importShipmentDao().getAllCartonsSync(),
            shipmentSuppliers = db.importShipmentDao().getAllShipmentSuppliersSync(),
            freightRates = db.importShipmentDao().getAllFreightRatesSync(),
            importCosts = db.importCostDao().getAllCostItemsSync(),
            landedAllocations = db.importCostDao().getAllAllocationsSync(),
            landedSnapshots = db.importCostDao().getAllSnapshotsSync(),
            warehouseReceipts = db.warehouseReceiptDao().getAllReceiptsSync(),
            warehouseReceiptItems = db.warehouseReceiptDao().getAllReceiptItemsSync(),
            costLayers = db.inventoryCostLayerDao().getAllCostLayersSync(),
            costAdjustments = db.inventoryCostAdjustmentDao().getAllAdjustmentsSync(),
            fifoConsumptions = db.fifoConsumptionDao().getAllConsumptionsSync(),
            stockTransfers = db.stockTransferDao().getAllTransfersSync(),
            stockTransferCostLayers = db.stockTransferDao().getAllTransferCostLayersSync(),
            damageRecords = db.minimalImportDao().getAllDamageRecordsSync(),
            shortageRecords = db.minimalImportDao().getAllShortageRecordsSync(),
            additionalProductCosts = db.minimalImportDao().getAllAdditionalCostsSync(),
            salesInvoices = invoices,
            salesInvoiceItems = db.salesDao().getAllInvoiceItemsSync(),
            saleLayerAllocations = db.importVaultDao().getAllSaleLayerAllocationsSync(),
            salesReturns = returns,
            salesReturnItems = db.salesReturnDao().getAllReturnItemsSync(),
            salesReturnLayerAllocations = db.importVaultDao().getAllSalesReturnLayerAllocationsSync(),
            customerPayments = payments,
            paymentAllocations = db.customerPaymentDao().getAllAllocationsSync(),
            expenseVouchers = db.expenseDao().getAllExpensesSync(),
            expenseAllocationRules = db.importVaultDao().getAllExpenseAllocationRulesSync(),
            expenseAllocations = db.importVaultDao().getAllExpenseAllocationsSync(),
            vaultEntries = db.importVaultDao().getAllVaultEntriesSync(),
            vaultAttachments = db.importVaultDao().getAllVaultAttachmentsSync(),
            customerOpeningDueTotalMinor = openingDueTotal,
            productImages = db.productImageDao().getAllImagesSync()
        )
    }
}
