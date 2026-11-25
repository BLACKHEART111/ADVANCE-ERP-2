package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketdev.advance_erp.data.entity.BackupRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupRecord(record: BackupRecord)

    @Query("SELECT * FROM backup_records ORDER BY createdAt DESC")
    fun getAllBackupRecords(): Flow<List<BackupRecord>>

    @Query("SELECT * FROM backup_records ORDER BY createdAt DESC")
    suspend fun getAllBackupRecordsSync(): List<BackupRecord>

    @Query("SELECT * FROM backup_records ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestBackup(): BackupRecord?

    @Query("DELETE FROM backup_records WHERE id = :id")
    suspend fun deleteBackupRecord(id: String)

    // --- Transactional & Stock Reset Queries (Preserve Products, Variants, Images, Categories & Contacts) ---

    @Query("DELETE FROM sales_return_layer_allocations")
    suspend fun clearSalesReturnLayerAllocations()

    @Query("DELETE FROM sale_layer_allocations")
    suspend fun clearSaleLayerAllocations()

    @Query("DELETE FROM payment_allocations")
    suspend fun clearPaymentAllocations()

    @Query("DELETE FROM customer_payments")
    suspend fun clearCustomerPayments()

    @Query("DELETE FROM sales_return_items")
    suspend fun clearSalesReturnItems()

    @Query("DELETE FROM sales_returns")
    suspend fun clearSalesReturns()

    @Query("DELETE FROM sales_cost_layer_usages")
    suspend fun clearSalesCostLayerUsages()

    @Query("DELETE FROM sales_invoice_items")
    suspend fun clearSalesInvoiceItems()

    @Query("DELETE FROM sales_invoices")
    suspend fun clearSalesInvoices()

    @Query("DELETE FROM fifo_consumptions")
    suspend fun clearFifoConsumptions()

    @Query("DELETE FROM inventory_cost_adjustments")
    suspend fun clearInventoryCostAdjustments()

    @Query("DELETE FROM stock_transfer_cost_layers")
    suspend fun clearStockTransferCostLayers()

    @Query("DELETE FROM stock_transfers")
    suspend fun clearStockTransfers()

    @Query("DELETE FROM inventory_reconciliation_items")
    suspend fun clearInventoryReconciliationItems()

    @Query("DELETE FROM inventory_reconciliations")
    suspend fun clearInventoryReconciliations()

    @Query("DELETE FROM inventory_cost_layers")
    suspend fun clearInventoryCostLayers()

    @Query("DELETE FROM stock_movements")
    suspend fun clearStockMovements()

    @Query("DELETE FROM stock_balances")
    suspend fun clearStockBalances()

    @Query("DELETE FROM damage_records")
    suspend fun clearDamageRecords()

    @Query("DELETE FROM shortage_records")
    suspend fun clearShortageRecords()

    @Query("DELETE FROM warehouse_receipt_items")
    suspend fun clearWarehouseReceiptItems()

    @Query("DELETE FROM warehouse_receipts")
    suspend fun clearWarehouseReceipts()

    @Query("DELETE FROM landed_cost_allocations")
    suspend fun clearLandedCostAllocations()

    @Query("DELETE FROM landed_cost_snapshots")
    suspend fun clearLandedCostSnapshots()

    @Query("DELETE FROM import_cost_items")
    suspend fun clearImportCostItems()

    @Query("DELETE FROM shipment_cartons")
    suspend fun clearShipmentCartons()

    @Query("DELETE FROM shipment_suppliers")
    suspend fun clearShipmentSuppliers()

    @Query("DELETE FROM shipment_freight_rates")
    suspend fun clearShipmentFreightRates()

    @Query("DELETE FROM import_shipments")
    suspend fun clearImportShipments()

    @Query("DELETE FROM additional_product_costs")
    suspend fun clearAdditionalProductCosts()

    @Query("DELETE FROM purchase_completion_records")
    suspend fun clearPurchaseCompletionRecords()

    @Query("DELETE FROM supplier_payments")
    suspend fun clearSupplierPayments()

    @Query("DELETE FROM purchase_order_items")
    suspend fun clearPurchaseOrderItems()

    @Query("DELETE FROM purchase_orders")
    suspend fun clearPurchaseOrders()

    @Query("DELETE FROM bank_reconciliation_items")
    suspend fun clearBankReconciliationItems()

    @Query("DELETE FROM bank_reconciliations")
    suspend fun clearBankReconciliations()

    @Query("DELETE FROM account_transfers")
    suspend fun clearAccountTransfers()

    @Query("DELETE FROM expense_allocations")
    suspend fun clearExpenseAllocations()

    @Query("DELETE FROM expense_allocation_rules")
    suspend fun clearExpenseAllocationRules()

    @Query("DELETE FROM import_vault_attachments")
    suspend fun clearImportVaultAttachments()

    @Query("DELETE FROM import_vault_entries")
    suspend fun clearImportVaultEntries()

    @Query("DELETE FROM expense_vouchers")
    suspend fun clearExpenseVouchers()

    @Query("DELETE FROM journal_lines")
    suspend fun clearJournalLines()

    @Query("DELETE FROM journal_entries")
    suspend fun clearJournalEntries()

    @Query("UPDATE customers SET outstandingBalanceMinor = 0, isActive = 1")
    suspend fun resetAllCustomerBalances()

    @Query("UPDATE suppliers SET outstandingPayableMinor = 0, isActive = 1")
    suspend fun resetAllSupplierPayables()

    @Query("DELETE FROM audit_logs WHERE recordType != 'BACKUP'")
    suspend fun clearTransactionAuditLogs()
}
