package com.pocketdev.advance_erp.domain.repository

import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.Supplier
import kotlinx.coroutines.flow.Flow

sealed class CustomerDeleteResult {
    data class Blocked(val reason: String, val balanceMinor: Long) : CustomerDeleteResult()
    data class Archived(val message: String, val transactionCount: Int) : CustomerDeleteResult()
    object HardDeleted : CustomerDeleteResult()
}

interface ContactsRepository {
    fun getAllCustomers(): Flow<List<Customer>>
    fun getAllSuppliers(): Flow<List<Supplier>>
    suspend fun addCustomer(customer: Customer)
    suspend fun updateCustomer(customer: Customer)
    suspend fun addSupplier(supplier: Supplier)
    suspend fun updateSupplier(supplier: Supplier)
    suspend fun canChangeSupplierCurrency(supplierId: String): Boolean
    suspend fun recordCustomerOpeningDue(
        customerId: String,
        amountMinor: Long,
        entryDate: Long = System.currentTimeMillis(),
        note: String? = null
    ): String
    suspend fun recordSupplierOpeningPayable(
        supplierId: String,
        amountBdtMinor: Long,
        entryDate: Long = System.currentTimeMillis(),
        note: String? = null
    ): String
    suspend fun addCustomerWithOptionalOpeningDue(
        customer: Customer,
        openingDueMinor: Long = 0L,
        entryDate: Long = System.currentTimeMillis(),
        openingNote: String? = null
    )
    suspend fun addSupplierWithOptionalOpeningPayable(
        supplier: Supplier,
        openingPayableBdtMinor: Long = 0L,
        entryDate: Long = System.currentTimeMillis(),
        openingNote: String? = null
    )
    suspend fun deleteOrArchiveCustomer(customerId: String, operator: String = "SYSTEM_ADMIN"): CustomerDeleteResult
}
