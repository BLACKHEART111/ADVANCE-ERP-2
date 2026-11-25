package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.ExpenseAllocation
import com.pocketdev.advance_erp.data.entity.ExpenseAllocationRule
import com.pocketdev.advance_erp.data.entity.ImportVaultAttachment
import com.pocketdev.advance_erp.data.entity.ImportVaultEntry
import com.pocketdev.advance_erp.data.entity.SaleLayerAllocation
import com.pocketdev.advance_erp.data.entity.SalesReturnLayerAllocation
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportVaultDao {

    // --- Sale Layer Allocations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleLayerAllocation(allocation: SaleLayerAllocation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleLayerAllocations(allocations: List<SaleLayerAllocation>)

    @Query("SELECT * FROM sale_layer_allocations ORDER BY createdAt ASC, id ASC")
    suspend fun getAllSaleLayerAllocationsSync(): List<SaleLayerAllocation>

    @Query("SELECT * FROM sale_layer_allocations WHERE invoiceId = :invoiceId ORDER BY createdAt ASC, id ASC")
    suspend fun getSaleLayerAllocationsForInvoiceSync(invoiceId: String): List<SaleLayerAllocation>

    @Query("SELECT * FROM sale_layer_allocations WHERE invoiceItemId = :invoiceItemId AND isReversed = 0 ORDER BY createdAt DESC, id DESC")
    suspend fun getSaleLayerAllocationsForInvoiceItemSync(invoiceItemId: String): List<SaleLayerAllocation>

    @Query("UPDATE sale_layer_allocations SET isReversed = 1 WHERE invoiceId = :invoiceId")
    suspend fun markSaleLayerAllocationsReversedForInvoice(invoiceId: String)

    // --- Sales Return Layer Allocations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalesReturnLayerAllocation(allocation: SalesReturnLayerAllocation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalesReturnLayerAllocations(allocations: List<SalesReturnLayerAllocation>)

    @Query("SELECT * FROM sales_return_layer_allocations ORDER BY createdAt ASC, id ASC")
    suspend fun getAllSalesReturnLayerAllocationsSync(): List<SalesReturnLayerAllocation>

    @Query("SELECT * FROM sales_return_layer_allocations WHERE invoiceId = :invoiceId ORDER BY createdAt ASC, id ASC")
    suspend fun getReturnLayerAllocationsForInvoiceSync(invoiceId: String): List<SalesReturnLayerAllocation>

    @Query("SELECT * FROM sales_return_layer_allocations WHERE returnId = :returnId ORDER BY createdAt ASC, id ASC")
    suspend fun getReturnLayerAllocationsForReturnSync(returnId: String): List<SalesReturnLayerAllocation>

    // --- Expense Allocation Rules & Allocations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseAllocationRule(rule: ExpenseAllocationRule)

    @Update
    suspend fun updateExpenseAllocationRule(rule: ExpenseAllocationRule)

    @Query("SELECT * FROM expense_allocation_rules ORDER BY updatedAt DESC, createdAt DESC")
    fun getAllExpenseAllocationRules(): Flow<List<ExpenseAllocationRule>>

    @Query("SELECT * FROM expense_allocation_rules ORDER BY updatedAt DESC, createdAt DESC")
    suspend fun getAllExpenseAllocationRulesSync(): List<ExpenseAllocationRule>

    @Query("SELECT * FROM expense_allocation_rules WHERE isActive = 1 ORDER BY updatedAt DESC")
    suspend fun getActiveExpenseAllocationRulesSync(): List<ExpenseAllocationRule>

    @Query("UPDATE expense_allocation_rules SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :ruleId")
    suspend fun setExpenseAllocationRuleActive(ruleId: String, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseAllocations(allocations: List<ExpenseAllocation>)

    @Query("DELETE FROM expense_allocations WHERE ruleId = :ruleId")
    suspend fun deleteExpenseAllocationsForRule(ruleId: String)

    @Query("SELECT * FROM expense_allocations ORDER BY createdAt ASC")
    suspend fun getAllExpenseAllocationsSync(): List<ExpenseAllocation>

    @Query("SELECT * FROM expense_allocations WHERE ruleId = :ruleId ORDER BY createdAt ASC")
    suspend fun getExpenseAllocationsForRuleSync(ruleId: String): List<ExpenseAllocation>

    // --- Import Vault Archive Entries ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVaultEntryIgnore(entry: ImportVaultEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVaultEntry(entry: ImportVaultEntry)

    @Update
    suspend fun updateVaultEntry(entry: ImportVaultEntry)

    @Query("SELECT * FROM import_vault_entries ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllVaultEntries(): Flow<List<ImportVaultEntry>>

    @Query("SELECT * FROM import_vault_entries ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllVaultEntriesSync(): List<ImportVaultEntry>

    @Query("SELECT * FROM import_vault_entries WHERE scopeType = :scopeType AND sourceId = :sourceId LIMIT 1")
    suspend fun getVaultEntryByScopeAndSource(scopeType: String, sourceId: String): ImportVaultEntry?

    // --- Import Vault Attachments ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultAttachment(attachment: ImportVaultAttachment)

    @Query("DELETE FROM import_vault_attachments WHERE id = :attachmentId")
    suspend fun deleteVaultAttachment(attachmentId: String)

    @Query("SELECT * FROM import_vault_attachments WHERE id = :attachmentId LIMIT 1")
    suspend fun getVaultAttachmentByIdSync(attachmentId: String): ImportVaultAttachment?

    @Query("SELECT * FROM import_vault_attachments ORDER BY createdAt DESC")
    suspend fun getAllVaultAttachmentsSync(): List<ImportVaultAttachment>

    @Query("SELECT * FROM import_vault_attachments WHERE scopeType = :scopeType AND sourceId = :sourceId ORDER BY createdAt DESC")
    fun getAttachmentsForSource(scopeType: String, sourceId: String): Flow<List<ImportVaultAttachment>>

    @Query("SELECT * FROM import_vault_attachments WHERE scopeType = :scopeType AND sourceId = :sourceId ORDER BY createdAt DESC")
    suspend fun getAttachmentsForSourceSync(scopeType: String, sourceId: String): List<ImportVaultAttachment>
}
