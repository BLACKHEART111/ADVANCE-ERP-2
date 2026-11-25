package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Persists line-to-FIFO-cost-layer revenue, discount, and COGS attribution at sale/dispatch/exchange time.
 * [costLayerId] is nullable so legacy/unattributed fallback units are explicitly flagged as Unknown/Unattributed.
 */
@Entity(
    tableName = "sale_layer_allocations",
    foreignKeys = [
        ForeignKey(
            entity = SalesInvoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SalesInvoiceItem::class,
            parentColumns = ["id"],
            childColumns = ["invoiceItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InventoryCostLayer::class,
            parentColumns = ["id"],
            childColumns = ["costLayerId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProductVariant::class,
            parentColumns = ["id"],
            childColumns = ["variantId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Warehouse::class,
            parentColumns = ["id"],
            childColumns = ["warehouseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("invoiceId"),
        Index("invoiceItemId"),
        Index("costLayerId"),
        Index("productId"),
        Index("variantId"),
        Index("warehouseId"),
        Index("purchaseOrderId"),
        Index("shipmentId"),
        Index("createdAt")
    ]
)
data class SaleLayerAllocation(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val invoiceId: String? = null,
    val invoiceItemId: String? = null,
    val costLayerId: String? = null,
    val productId: String,
    val variantId: String? = null,
    val warehouseId: String,
    val purchaseOrderId: String? = null,
    val purchaseOrderItemId: String? = null,
    val shipmentId: String? = null,
    val quantity: Int,
    val unitCostBdtMinor: Long,
    val totalCogsBdtMinor: Long,
    val allocatedGrossRevenueMinor: Long,
    val allocatedLineDiscountMinor: Long,
    val allocatedInvoiceDiscountMinor: Long,
    val allocatedNetRevenueMinor: Long,
    val isUnattributedFallback: Boolean = false,
    val isReversed: Boolean = false,
    val sourceType: String = "INVOICE_POST",
    val referenceId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Persists exact layer-linked sales return reversals (sound restock vs damaged write-off)
 * along with discount-adjusted net revenue and actual layer COGS reversal.
 */
@Entity(
    tableName = "sales_return_layer_allocations",
    foreignKeys = [
        ForeignKey(
            entity = SalesReturn::class,
            parentColumns = ["id"],
            childColumns = ["returnId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SalesReturnItem::class,
            parentColumns = ["id"],
            childColumns = ["returnItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SalesInvoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SaleLayerAllocation::class,
            parentColumns = ["id"],
            childColumns = ["saleLayerAllocationId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = InventoryCostLayer::class,
            parentColumns = ["id"],
            childColumns = ["costLayerId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProductVariant::class,
            parentColumns = ["id"],
            childColumns = ["variantId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("returnId"),
        Index("returnItemId"),
        Index("invoiceId"),
        Index("saleLayerAllocationId"),
        Index("costLayerId"),
        Index("productId"),
        Index("variantId"),
        Index("purchaseOrderId"),
        Index("shipmentId")
    ]
)
data class SalesReturnLayerAllocation(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val returnId: String,
    val returnItemId: String,
    val invoiceId: String,
    val invoiceItemId: String,
    val saleLayerAllocationId: String? = null,
    val costLayerId: String? = null,
    val restoredCostLayerId: String? = null,
    val productId: String,
    val variantId: String? = null,
    val purchaseOrderId: String? = null,
    val purchaseOrderItemId: String? = null,
    val shipmentId: String? = null,
    val soundQuantity: Int,
    val damagedQuantity: Int,
    val reversedGrossRevenueMinor: Long,
    val reversedDiscountMinor: Long,
    val reversedNetRevenueMinor: Long,
    val reversedCogsMinor: Long,
    val damageLossMinor: Long,
    val isUnattributed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Opt-in rule for allocating shared overhead expenses (rent, salaries, etc.) across shipments or POs.
 * Inactive (UNALLOCATED) by default so PO/Shipment reports show Attributed Net Profit honestly.
 */
@Entity(
    tableName = "expense_allocation_rules",
    indices = [
        Index("allocationBasis"),
        Index("isActive"),
        Index("periodStartMillis"),
        Index("periodEndMillis")
    ]
)
data class ExpenseAllocationRule(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val ruleName: String,
    val ruleVersion: Int = 1,
    val allocationBasis: String = "UNALLOCATED",
    val periodStartMillis: Long,
    val periodEndMillis: Long,
    val targetScopeType: String = "SHIPMENT",
    val isActive: Boolean = false,
    val notes: String? = null,
    val createdBy: String = "OWNER_ADMIN",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Materialized allocation rows linking a shared ExpenseVoucher to a Shipment/PO under an ExpenseAllocationRule.
 */
@Entity(
    tableName = "expense_allocations",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseAllocationRule::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExpenseVoucher::class,
            parentColumns = ["id"],
            childColumns = ["expenseVoucherId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("ruleId"),
        Index("expenseVoucherId"),
        Index("targetScopeType"),
        Index("targetId")
    ]
)
data class ExpenseAllocation(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val ruleId: String,
    val expenseVoucherId: String,
    val targetScopeType: String,
    val targetId: String,
    val allocationBasis: String,
    val basisValue: String,
    val basisPercentage: String,
    val allocatedAmountBdtMinor: Long,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Stable vault metadata/archive index synced cleanly with existing shipments and purchase orders.
 */
@Entity(
    tableName = "import_vault_entries",
    indices = [
        Index(value = ["scopeType", "sourceId"], unique = true),
        Index("scopeType"),
        Index("sourceId"),
        Index("archiveStatus")
    ]
)
data class ImportVaultEntry(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val scopeType: String,
    val sourceId: String,
    val displayTitle: String,
    val archiveStatus: String = "ACTIVE",
    val isPinned: Boolean = false,
    val tagsJson: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Vault document/evidence attachment metadata linked to an Import Vault entry and/or source Shipment/PO.
 */
@Entity(
    tableName = "import_vault_attachments",
    foreignKeys = [
        ForeignKey(
            entity = ImportVaultEntry::class,
            parentColumns = ["id"],
            childColumns = ["vaultEntryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("vaultEntryId"),
        Index("scopeType"),
        Index("sourceId"),
        Index("category")
    ]
)
data class ImportVaultAttachment(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val vaultEntryId: String? = null,
    val scopeType: String,
    val sourceId: String,
    val category: String,
    val title: String,
    val filePath: String,
    val mimeType: String = "application/octet-stream",
    val fileSizeBytes: Long = 0L,
    val isImmutableSnapshot: Boolean = false,
    val createdBy: String = "OWNER_ADMIN",
    val createdAt: Long = System.currentTimeMillis()
)
