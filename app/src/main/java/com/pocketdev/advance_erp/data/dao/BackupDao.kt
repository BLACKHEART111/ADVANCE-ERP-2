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

    // --- Advanced Selective Module Clear: Live Impact Count Queries ---

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1")
    suspend fun countActiveProducts(): Int

    @Query("SELECT COUNT(*) FROM product_variants WHERE isActive = 1")
    suspend fun countActiveVariants(): Int

    @Query("SELECT COUNT(*) FROM chart_of_accounts")
    suspend fun countChartOfAccounts(): Int

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun countCustomers(): Int

    @Query("SELECT COUNT(*) FROM suppliers")
    suspend fun countSuppliers(): Int

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM stock_balances")
    suspend fun countTotalStockUnits(): Int

    @Query("SELECT COUNT(*) FROM stock_movements")
    suspend fun countStockMovements(): Int

    @Query("SELECT COUNT(*) FROM purchase_orders")
    suspend fun countPurchaseOrders(): Int

    @Query("SELECT COUNT(*) FROM warehouse_receipts")
    suspend fun countWarehouseReceipts(): Int

    @Query("SELECT COUNT(*) FROM purchase_orders WHERE status NOT IN ('DRAFT', 'CONFIRMED', 'CANCELLED') OR paidAmountMinor > 0 OR isActualCostFinalized = 1")
    suspend fun countReceivedOrPaidPurchaseOrders(): Int

    @Query("SELECT COUNT(*) FROM supplier_payments")
    suspend fun countSupplierPayments(): Int

    @Query("SELECT COUNT(*) FROM sales_invoices")
    suspend fun countSalesInvoices(): Int

    @Query("SELECT COUNT(*) FROM sales_returns")
    suspend fun countSalesReturns(): Int

    @Query("SELECT COUNT(*) FROM customer_payments")
    suspend fun countCustomerPayments(): Int

    @Query("SELECT COUNT(*) FROM journal_entries")
    suspend fun countJournalEntries(): Int

    @Query("SELECT COUNT(*) FROM expense_vouchers")
    suspend fun countExpenseVouchers(): Int

    @Query("SELECT COUNT(*) FROM account_transfers")
    suspend fun countAccountTransfers(): Int

    @Query("SELECT COUNT(*) FROM import_vault_entries")
    suspend fun countImportVaultEntries(): Int

    @Query("SELECT COUNT(*) FROM import_vault_attachments")
    suspend fun countImportVaultAttachments(): Int

    @Query("SELECT COUNT(*) FROM communication_history")
    suspend fun countCommunicationHistory(): Int

    @Query("SELECT COUNT(*) FROM communication_snapshots")
    suspend fun countCommunicationSnapshots(): Int

    @Query("SELECT COUNT(*) FROM audit_logs WHERE recordType != 'BACKUP'")
    suspend fun countTransactionAuditLogs(): Int

    @Query("SELECT COUNT(*) FROM saved_suggestions")
    suspend fun countSavedSuggestions(): Int

    @Query("SELECT COUNT(*) FROM inventory_cost_layers WHERE (purchaseOrderId IS NOT NULL AND purchaseOrderId != '') OR warehouseReceiptId IN (SELECT id FROM warehouse_receipts) OR shipmentId IN (SELECT id FROM import_shipments)")
    suspend fun countPoReceiptCostLayers(): Int

    // --- PO Full-Structure-Preserving Unreceive & Payment Reset ---

    @Query("UPDATE purchase_order_items SET receivedQuantity = 0")
    suspend fun resetAllPurchaseOrderItemsReceivedQuantity()

    @Query("UPDATE purchase_orders SET status = CASE WHEN status IN ('DRAFT', 'CANCELLED') THEN status ELSE 'CONFIRMED' END, paidAmountMinor = 0, paidBdtAmountMinor = 0, isActualCostFinalized = 0, actualCostFinalizedAt = NULL, updatedAt = :updatedAt")
    suspend fun resetAllPurchaseOrdersToUnreceivedConfirmed(updatedAt: Long)

    @Query("UPDATE import_shipments SET status = 'DRAFT', actualArrivalDate = NULL, updatedAt = :updatedAt")
    suspend fun resetShipmentsToUnreceivedDraftKeepPacking(updatedAt: Long)

    @Query("DELETE FROM import_cost_items WHERE isPostFinalization = 1")
    suspend fun deletePostFinalizationImportCostItems()

    @Query("UPDATE import_cost_items SET isCapitalized = 0, isPosted = 0, isPaid = 0, costAdjustmentReference = NULL")
    suspend fun resetImportCostItemsPaymentAndCapitalization()

    @Query("SELECT * FROM purchase_orders WHERE status = 'CONFIRMED' AND totalBdtAmountMinor > 0 ORDER BY createdAt ASC")
    suspend fun getConfirmedPurchaseOrdersForCommitmentSync(): List<com.pocketdev.advance_erp.data.entity.PurchaseOrder>

    // --- Partial-PO-Reset Stock Batch Removal (preserves manual opening/adjustment stock) ---

    @Query("DELETE FROM fifo_consumptions WHERE costLayerId IN (SELECT id FROM inventory_cost_layers WHERE (purchaseOrderId IS NOT NULL AND purchaseOrderId != '') OR warehouseReceiptId IN (SELECT id FROM warehouse_receipts) OR shipmentId IN (SELECT id FROM import_shipments))")
    suspend fun deleteFifoConsumptionsForPoReceiptLayers()

    @Query("DELETE FROM stock_transfer_cost_layers WHERE costLayerId IN (SELECT id FROM inventory_cost_layers WHERE (purchaseOrderId IS NOT NULL AND purchaseOrderId != '') OR warehouseReceiptId IN (SELECT id FROM warehouse_receipts) OR shipmentId IN (SELECT id FROM import_shipments))")
    suspend fun deleteStockTransferLayersForPoReceiptLayers()

    @Query("DELETE FROM inventory_cost_adjustments WHERE costLayerId IN (SELECT id FROM inventory_cost_layers WHERE (purchaseOrderId IS NOT NULL AND purchaseOrderId != '') OR warehouseReceiptId IN (SELECT id FROM warehouse_receipts) OR shipmentId IN (SELECT id FROM import_shipments))")
    suspend fun deleteCostAdjustmentsForPoReceiptLayers()

    @Query("DELETE FROM inventory_cost_layers WHERE (purchaseOrderId IS NOT NULL AND purchaseOrderId != '') OR warehouseReceiptId IN (SELECT id FROM warehouse_receipts) OR shipmentId IN (SELECT id FROM import_shipments)")
    suspend fun deletePoReceiptCostLayers()

    @Query("DELETE FROM stock_movements WHERE referenceType IN ('WAREHOUSE_RECEIPT', 'PO_REOPEN_EDIT', 'PURCHASE_RECEIPT') OR purchaseOrderId IS NOT NULL")
    suspend fun deletePoStockMovements()

    @Query("UPDATE stock_balances SET quantity = COALESCE((SELECT SUM(icl.remainingQuantity) FROM inventory_cost_layers icl WHERE icl.warehouseId = stock_balances.warehouseId AND icl.productId = stock_balances.productId AND ((icl.variantId IS NULL AND stock_balances.variantId IS NULL) OR icl.variantId = stock_balances.variantId) AND icl.isActive = 1), 0), updatedAt = :updatedAt")
    suspend fun syncStockBalancesWithActiveCostLayers(updatedAt: Long)

    @Query("DELETE FROM stock_balances WHERE quantity <= 0 AND reservedQuantity <= 0")
    suspend fun deleteEmptyStockBalances()

    // --- Selective Journal, Communication & Suggestion Cleanup ---

    @Query("DELETE FROM journal_entries WHERE reversalOfEntryId IN (SELECT id FROM journal_entries WHERE referenceType IN (:types))")
    suspend fun deleteReversalEntriesForReferenceTypes(types: List<String>)

    @Query("DELETE FROM journal_entries WHERE referenceType IN (:types)")
    suspend fun deleteJournalEntriesByReferenceTypes(types: List<String>)

    @Query("DELETE FROM communication_snapshots WHERE module IN (:modules)")
    suspend fun deleteCommunicationSnapshotsByModules(modules: List<String>)

    @Query("DELETE FROM saved_suggestions")
    suspend fun clearSavedSuggestions()

    // --- Exact Customer & Supplier Balance Recalculation From Remaining Active Records ---

    @Query("""
        UPDATE customers SET outstandingBalanceMinor = (
            COALESCE((SELECT SUM(totalAmountMinor) FROM sales_invoices si WHERE si.customerId = customers.id AND si.isPosted = 1 AND si.status != 'CANCELLED'), 0)
            - COALESCE((SELECT SUM(amountMinor) FROM customer_payments cp WHERE cp.customerId = customers.id AND cp.isPosted = 1 AND cp.isVoided = 0), 0)
            - COALESCE((SELECT SUM(totalReturnAmountMinor) FROM sales_returns sr WHERE sr.customerId = customers.id AND sr.isPosted = 1 AND sr.refundOption = 'CUSTOMER_CREDIT'), 0)
            + COALESCE((SELECT SUM(CASE WHEN je.referenceType = 'CUSTOMER_LEDGER_DEBIT_ADJUSTMENT' THEN je.totalDebitMinor WHEN je.referenceType = 'CUSTOMER_LEDGER_CREDIT_ADJUSTMENT' THEN -je.totalDebitMinor ELSE 0 END) FROM journal_entries je WHERE je.referenceId = customers.id AND je.isPosted = 1 AND je.isReversed = 0 AND je.referenceType IN ('CUSTOMER_LEDGER_DEBIT_ADJUSTMENT', 'CUSTOMER_LEDGER_CREDIT_ADJUSTMENT')), 0)
        )
    """)
    suspend fun recalculateAllCustomerBalancesFromRemainingRecords()

    @Query("""
        UPDATE suppliers SET outstandingPayableMinor = MAX(0,
            COALESCE((SELECT SUM(totalBdtAmountMinor) FROM purchase_orders po WHERE po.supplierId = suppliers.id AND po.status NOT IN ('DRAFT', 'CANCELLED')), 0)
            - COALESCE((SELECT SUM(bdtAmountMinor) FROM supplier_payments sp WHERE sp.supplierId = suppliers.id), 0)
            + COALESCE((SELECT SUM(CASE WHEN je.referenceType = 'SUPPLIER_LEDGER_CREDIT_ADJUSTMENT' THEN je.totalCreditMinor WHEN je.referenceType = 'SUPPLIER_LEDGER_DEBIT_ADJUSTMENT' THEN -je.totalDebitMinor ELSE 0 END) FROM journal_entries je WHERE je.referenceId = suppliers.id AND je.isPosted = 1 AND je.isReversed = 0 AND je.referenceType IN ('SUPPLIER_LEDGER_CREDIT_ADJUSTMENT', 'SUPPLIER_LEDGER_DEBIT_ADJUSTMENT')), 0)
        )
    """)
    suspend fun recalculateAllSupplierPayablesFromRemainingRecords()
}
