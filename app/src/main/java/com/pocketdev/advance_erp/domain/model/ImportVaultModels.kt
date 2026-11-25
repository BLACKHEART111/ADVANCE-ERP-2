package com.pocketdev.advance_erp.domain.model

import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.data.entity.DamageRecord
import com.pocketdev.advance_erp.data.entity.ExpenseAllocation
import com.pocketdev.advance_erp.data.entity.ExpenseAllocationRule
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.data.entity.FifoConsumption
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.ImportVaultAttachment
import com.pocketdev.advance_erp.data.entity.ImportVaultEntry
import com.pocketdev.advance_erp.data.entity.InventoryCostAdjustment
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.LandedCostAllocation
import com.pocketdev.advance_erp.data.entity.LandedCostSnapshot
import com.pocketdev.advance_erp.data.entity.PaymentAllocation
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductImage
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.SaleLayerAllocation
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import com.pocketdev.advance_erp.data.entity.SalesReturn
import com.pocketdev.advance_erp.data.entity.SalesReturnItem
import com.pocketdev.advance_erp.data.entity.SalesReturnLayerAllocation
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.ShipmentFreightRate
import com.pocketdev.advance_erp.data.entity.ShipmentSupplier
import com.pocketdev.advance_erp.data.entity.ShortageRecord
import com.pocketdev.advance_erp.data.entity.StockTransfer
import com.pocketdev.advance_erp.data.entity.StockTransferCostLayer
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.SupplierPayment
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.data.entity.WarehouseReceiptItem

enum class VaultScopeType {
    SHIPMENT,
    PURCHASE_ORDER
}

enum class OperationalStatus {
    DRAFT,
    ORDERED,
    IN_TRANSIT,
    PARTIALLY_RECEIVED,
    RECEIVED,
    CLOSED,
    CANCELLED
}

enum class CommercialStatus {
    UNSOLD,
    SELLING,
    PROFITABLE,
    LOSS_MAKING,
    SOLD_OUT,
    CASH_RECOVERED,
    UNATTRIBUTED
}

enum class CostFinalizationStatus {
    FINALIZED,
    PROVISIONAL
}

enum class ExpenseAllocationMode {
    UNALLOCATED,
    ALLOCATED
}

enum class StockAgingBucket(val labelEn: String, val labelBn: String) {
    DAYS_0_30("0-30 Days", "০-৩০ দিন"),
    DAYS_31_60("31-60 Days", "৩১-৬০ দিন"),
    DAYS_61_90("61-90 Days", "৬১-৯০ দিন"),
    DAYS_90_PLUS("90+ Days", "৯০+ দিন")
}

enum class ProfitContributionBaseStatus {
    POSITIVE_BASE,
    ZERO_BASE,
    LOSS_BASE
}

enum class VaultDateFilterTarget {
    ACTIVITY_DATES,
    IMPORT_COHORT_DATES
}

data class VaultTrendPoint(
    val bucketKey: String,
    val labelEn: String,
    val labelBn: String,
    val startMillis: Long,
    val endMillis: Long,
    val netRevenueMinor: Long,
    val cogsMinor: Long,
    val grossProfitMinor: Long,
    val netQtySold: Int,
    val invoiceCount: Int
)

data class ImportVaultFilter(
    val dateFromMillis: Long? = null,
    val dateToMillis: Long? = null,
    val asOfMillis: Long? = null,
    val supplierId: String? = null,
    val warehouseId: String? = null,
    val productId: String? = null,
    val variantId: String? = null,
    val operationalStatus: OperationalStatus? = null,
    val commercialStatus: CommercialStatus? = null,
    val onlyProfitable: Boolean = false,
    val onlyLossMaking: Boolean = false,
    val onlyHighAging: Boolean = false,
    val onlyPendingCost: Boolean = false,
    val searchQuery: String? = null,
    val allocationRuleId: String? = null,
    val nowMillis: Long = System.currentTimeMillis(),
    val dateFilterTarget: VaultDateFilterTarget = VaultDateFilterTarget.ACTIVITY_DATES
)

data class QuantityByUnit(
    val byUom: Map<String, Int>,
    val hasMixedUnits: Boolean,
    val singleUnitTotal: Int?
) {
    companion object {
        fun fromItems(items: List<Pair<String, Int>>): QuantityByUnit {
            val grouped = linkedMapOf<String, Int>()
            for ((uom, qty) in items) {
                val normalized = uom.trim().ifBlank { "PCS" }
                grouped[normalized] = (grouped[normalized] ?: 0) + qty
            }
            val activeKeys = grouped.filterValues { it != 0 }.keys.ifEmpty { grouped.keys }
            val mixed = activeKeys.size > 1
            val singleTotal = if (mixed) null else grouped.values.sum()
            return QuantityByUnit(
                byUom = grouped,
                hasMixedUnits = mixed,
                singleUnitTotal = singleTotal
            )
        }
    }
}

data class ProfitContributionMetric(
    val childProfitMinor: Long,
    val parentProfitMinor: Long,
    val contributionPercent: Double?,
    val baseStatus: ProfitContributionBaseStatus
)

data class AgingSummary(
    val bucketQuantities: Map<StockAgingBucket, Int>,
    val bucketValuesMinor: Map<StockAgingBucket, Long>,
    val oldestAgeDays: Int
)

data class ExpectedRemainingProfitEstimate(
    val remainingGoodQty: Int,
    val lockedGoodInventoryCostMinor: Long,
    val expectedSellingRevenueMinor: Long,
    val expectedGrossProfitMinor: Long,
    val priceBasisLabel: String,
    val asOfMillis: Long,
    val missingPriceSkuCount: Int
)

data class CashRecoverySummary(
    val totalInvestedCapitalMinor: Long,
    val attributableCashCollectedMinor: Long,
    val attributableReceivableMinor: Long,
    val unrecoveredCapitalMinor: Long,
    val cashRecoveryRatioPercent: Double?,
    val breakEvenReached: Boolean
)

data class UnknownAttributionBucket(
    val unattributedRevenueMinor: Long,
    val unattributedCogsMinor: Long,
    val unattributedInventoryCostMinor: Long,
    val unattributedCollectionsMinor: Long,
    val openingDueReceivableMinor: Long,
    val nonMerchandiseAdditionalChargesMinor: Long,
    val revenueAttributionCoveragePercent: Double?
)

data class VariantVaultRow(
    val productId: String,
    val variantId: String?,
    val sku: String,
    val variantLabel: String,
    val uom: String,
    val purchaseOrderId: String?,
    val shipmentId: String?,
    val receivedGoodQty: Int,
    val netSoldQty: Int,
    val returnedGoodQty: Int,
    val returnedDamagedQty: Int,
    val writeOffLossQty: Int,
    val remainingGoodQty: Int,
    val remainingInTransitTransferQty: Int,
    val unitLandedCostMinor: Long,
    val netSalesRevenueMinor: Long,
    val cogsMinor: Long,
    val grossProfitMinor: Long,
    val grossMarginPercent: Double?,
    val inventoryLossMinor: Long,
    val directExpensesMinor: Long,
    val allocatedSharedOverheadMinor: Long,
    val attributedNetProfitMinor: Long,
    val netMarginPercent: Double?,
    val lockedInventoryCostMinor: Long,
    val sellThroughPercent: Double?,
    val profitContributionToProduct: ProfitContributionMetric?,
    val profitContributionToParentScope: ProfitContributionMetric?,
    val agingSummary: AgingSummary,
    val expectedRemainingProfit: ExpectedRemainingProfitEstimate,
    val profitContributionToPo: ProfitContributionMetric? = null,
    val profitContributionToShipment: ProfitContributionMetric? = null,
    val primaryImageUri: String? = null,
    val roiPercent: Double? = null,
    val activeAgeDays: Int = 0,
    val sellingVelocityPerDay: Double? = null
)

data class ProductVaultRow(
    val productId: String,
    val productNameEn: String,
    val productNameBn: String,
    val baseSku: String,
    val uom: String,
    val variants: List<VariantVaultRow>,
    val receivedGoodQty: Int,
    val netSoldQty: Int,
    val returnedGoodQty: Int,
    val returnedDamagedQty: Int,
    val writeOffLossQty: Int,
    val remainingGoodQty: Int,
    val remainingInTransitTransferQty: Int,
    val averageUnitLandedCostMinor: Long?,
    val netSalesRevenueMinor: Long,
    val cogsMinor: Long,
    val grossProfitMinor: Long,
    val grossMarginPercent: Double?,
    val inventoryLossMinor: Long,
    val directExpensesMinor: Long,
    val allocatedSharedOverheadMinor: Long,
    val attributedNetProfitMinor: Long,
    val netMarginPercent: Double?,
    val lockedInventoryCostMinor: Long,
    val sellThroughPercent: Double?,
    val profitContributionToParentScope: ProfitContributionMetric?,
    val agingSummary: AgingSummary,
    val expectedRemainingProfit: ExpectedRemainingProfitEstimate,
    val profitContributionToPo: ProfitContributionMetric? = null,
    val profitContributionToShipment: ProfitContributionMetric? = null,
    val primaryImageUri: String? = null,
    val roiPercent: Double? = null,
    val activeAgeDays: Int = 0,
    val sellingVelocityPerDay: Double? = null
)

data class PurchaseOrderVaultSummary(
    val purchaseOrderId: String,
    val poNumber: String,
    val supplierId: String,
    val supplierName: String,
    val shipmentIds: List<String>,
    val shipmentNumbers: List<String>,
    val orderDate: Long,
    val operationalStatus: OperationalStatus,
    val commercialStatus: CommercialStatus,
    val costFinalizationStatus: CostFinalizationStatus,
    val hasPendingReceiving: Boolean,
    val archiveStatus: String,
    val isPinned: Boolean,
    val orderedQtyByUnit: QuantityByUnit,
    val receivedGoodQtyByUnit: QuantityByUnit,
    val pendingReceivingQtyByUnit: QuantityByUnit,
    val netSoldQtyByUnit: QuantityByUnit,
    val remainingStockQtyByUnit: QuantityByUnit,
    val purchaseCostBdtMinor: Long,
    val capitalizedImportCostsMinor: Long,
    val capitalizedAdditionalCostsMinor: Long,
    val totalInvestedCapitalMinor: Long,
    val supplierPaidBdtMinor: Long,
    val supplierPayableBdtMinor: Long,
    val lockedInventoryCapitalMinor: Long,
    val netSalesRevenueMinor: Long,
    val cogsMinor: Long,
    val grossProfitMinor: Long,
    val grossMarginPercent: Double?,
    val directAttributedExpensesMinor: Long,
    val allocatedSharedOverheadMinor: Long,
    val inventoryLossesMinor: Long,
    val attributedNetProfitMinor: Long,
    val profitLabel: String,
    val netMarginPercent: Double?,
    val roiPercent: Double?,
    val sellThroughPercent: Double?,
    val cashRecovery: CashRecoverySummary,
    val agingSummary: AgingSummary,
    val expectedRemainingProfit: ExpectedRemainingProfitEstimate,
    val displayTitle: String = poNumber,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
    val productCount: Int = 0,
    val originalCurrency: String = "BDT",
    val originalTotalAmountMinor: Long = 0L,
    val exchangeRateToBdt: String = "1.0",
    val lifetimeNetProfitMinor: Long = attributedNetProfitMinor,
    val lifetimeRoiPercent: Double? = roiPercent
)

data class ShipmentVaultSummary(
    val shipmentId: String,
    val shipmentNumber: String,
    val shippingMethod: String,
    val supplierNames: List<String>,
    val purchaseOrderIds: List<String>,
    val purchaseOrderNumbers: List<String>,
    val shippingDate: Long,
    val arrivalDate: Long?,
    val operationalStatus: OperationalStatus,
    val commercialStatus: CommercialStatus,
    val costFinalizationStatus: CostFinalizationStatus,
    val hasPendingReceiving: Boolean,
    val archiveStatus: String,
    val isPinned: Boolean,
    val verifiedBillableWeightKg: String,
    val supplierDeclaredWeightKg: String?,
    val receivedGoodQtyByUnit: QuantityByUnit,
    val netSoldQtyByUnit: QuantityByUnit,
    val remainingStockQtyByUnit: QuantityByUnit,
    val totalInvestedCapitalMinor: Long,
    val purchaseCostBdtMinor: Long,
    val chinaDeliveryBdtMinor: Long,
    val internationalFreightBdtMinor: Long,
    val bdTransportAndOtherImportMinor: Long,
    val capitalizedAdditionalCostsMinor: Long,
    val lockedInventoryCapitalMinor: Long,
    val netSalesRevenueMinor: Long,
    val cogsMinor: Long,
    val grossProfitMinor: Long,
    val grossMarginPercent: Double?,
    val directAttributedExpensesMinor: Long,
    val allocatedSharedOverheadMinor: Long,
    val inventoryLossesMinor: Long,
    val attributedNetProfitMinor: Long,
    val profitLabel: String,
    val netMarginPercent: Double?,
    val roiPercent: Double?,
    val sellThroughPercent: Double?,
    val cashRecovery: CashRecoverySummary,
    val agingSummary: AgingSummary,
    val expectedRemainingProfit: ExpectedRemainingProfitEstimate,
    val displayTitle: String = shipmentNumber,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
    val productCount: Int = 0,
    val cartonCount: Int = 0,
    val originCurrency: String? = null,
    val originPurchaseAmountMinor: Long? = null,
    val lifetimeNetProfitMinor: Long = attributedNetProfitMinor,
    val lifetimeRoiPercent: Double? = roiPercent
)

data class ShipmentPoAllocationSummary(
    val shipmentId: String,
    val shipmentNumber: String,
    val purchaseOrderId: String,
    val poNumber: String,
    val supplierName: String,
    val receivedQtyByUnit: QuantityByUnit,
    val netSoldQtyByUnit: QuantityByUnit,
    val remainingStockQtyByUnit: QuantityByUnit,
    val allocatedInvestmentMinor: Long,
    val netSalesRevenueMinor: Long,
    val cogsMinor: Long,
    val attributedNetProfitMinor: Long,
    val netMarginPercent: Double?,
    val sellThroughPercent: Double?,
    val lockedInventoryCostMinor: Long,
    val shareOfPoPercent: Double
)

data class VaultSaleSourceRow(
    val allocationId: String,
    val invoiceId: String?,
    val invoiceNumber: String,
    val invoiceDate: Long,
    val productId: String,
    val productNameEn: String,
    val productNameBn: String,
    val variantLabel: String,
    val quantity: Int,
    val netRevenueMinor: Long,
    val cogsMinor: Long,
    val grossProfitMinor: Long,
    val collectedCashMinor: Long,
    val receivableMinor: Long,
    val costLayerId: String?,
    val purchaseOrderId: String?,
    val shipmentId: String?
)

data class VaultReturnSourceRow(
    val returnId: String,
    val returnNumber: String,
    val returnDate: Long,
    val invoiceId: String,
    val productId: String,
    val productNameEn: String,
    val productNameBn: String,
    val variantLabel: String,
    val soundQuantity: Int,
    val damagedQuantity: Int,
    val reversedNetRevenueMinor: Long,
    val reversedCogsMinor: Long,
    val damageLossMinor: Long
)

data class VaultCostLayerRow(
    val layerId: String,
    val productId: String,
    val productNameEn: String,
    val productNameBn: String,
    val variantId: String?,
    val variantLabel: String,
    val warehouseId: String,
    val warehouseName: String,
    val receiptNumber: String,
    val originReceiptDate: Long,
    val initialQuantity: Int,
    val remainingQuantity: Int,
    val unitLandedCostBdtMinor: Long,
    val carryingValueMinor: Long,
    val layerSourceType: String,
    val agingBucket: StockAgingBucket,
    val ageDays: Int,
    val purchaseOrderId: String?,
    val shipmentId: String?
)

enum class VaultDocumentCategory {
    PURCHASE,
    PACKING,
    FREIGHT,
    RECEIVING,
    SALES,
    PAYMENTS,
    REPORTS,
    ATTACHMENTS
}

data class VaultDocumentRef(
    val id: String,
    val category: VaultDocumentCategory,
    val titleEn: String,
    val titleBn: String,
    val subtitle: String,
    val referenceNumber: String,
    val timestampMillis: Long,
    val amountMinor: Long?,
    val currencyCode: String,
    val targetRoute: String?,
    val isImmutableSnapshot: Boolean = false,
    val attachmentId: String? = null,
    val localFilePath: String? = null,
    val mimeType: String? = null,
    val fileSizeBytes: Long? = null,
    val isLiveRegeneratedReport: Boolean = false
)

enum class VaultTimelineEventType {
    PO_CREATED,
    SHIPMENT_DISPATCHED,
    SHIPMENT_ARRIVED,
    COST_FINALIZED,
    WAREHOUSE_RECEIVED,
    SALE_POSTED,
    SALES_RETURN,
    SUPPLIER_PAYMENT,
    CUSTOMER_PAYMENT,
    EXPENSE_RECORDED,
    COST_ADJUSTMENT
}

data class VaultTimelineEvent(
    val id: String,
    val type: VaultTimelineEventType,
    val timestampMillis: Long,
    val titleEn: String,
    val titleBn: String,
    val subtitle: String,
    val referenceNumber: String,
    val amountMinor: Long?,
    val currencyCode: String,
    val targetRoute: String?
)

data class ImportVaultFilterOptions(
    val suppliers: List<Pair<String, String>> = emptyList(),
    val warehouses: List<Pair<String, String>> = emptyList(),
    val allocationRules: List<ExpenseAllocationRule> = emptyList()
)

data class ImportVaultExportSource(
    val dataset: ImportVaultDataset,
    val businessProfile: BusinessProfile? = null
)

data class ImportVaultOverview(
    val asOfMillis: Long,
    val expenseAllocationMode: ExpenseAllocationMode,
    val activeAllocationRuleId: String?,
    val profitLabel: String,
    val totalInvestedCapitalMinor: Long,
    val totalLockedInventoryCapitalMinor: Long,
    val totalAttributedNetRevenueMinor: Long,
    val totalAttributedCogsMinor: Long,
    val totalAttributedGrossProfitMinor: Long,
    val attributedGrossMarginPercent: Double?,
    val totalDirectAttributedExpensesMinor: Long,
    val totalAllocatedSharedOverheadMinor: Long,
    val unallocatedSharedOverheadMinor: Long,
    val totalInventoryLossesMinor: Long,
    val totalAttributedNetProfitMinor: Long,
    val attributedNetMarginPercent: Double?,
    val portfolioRoiPercent: Double?,
    val cashRecovery: CashRecoverySummary,
    val expectedRemainingProfit: ExpectedRemainingProfitEstimate,
    val agingSummary: AgingSummary,
    val unknownAttribution: UnknownAttributionBucket,
    val shipmentCount: Int,
    val purchaseOrderCount: Int,
    val totalAttachmentCount: Int,
    val shipments: List<ShipmentVaultSummary>,
    val purchaseOrders: List<PurchaseOrderVaultSummary>,
    val isPeriodActivityFiltered: Boolean = false,
    val openingStockCostMinor: Long? = null,
    val closingStockCostMinor: Long? = null,
    val lifetimeNetProfitMinor: Long = totalAttributedNetProfitMinor,
    val lifetimePortfolioRoiPercent: Double? = portfolioRoiPercent,
    val salesTrend: List<VaultTrendPoint> = emptyList(),
    val portfolioSaleSources: List<VaultSaleSourceRow> = emptyList(),
    val portfolioCostLayers: List<VaultCostLayerRow> = emptyList(),
    val portfolioAttachments: List<VaultDocumentRef> = emptyList()
)

data class ShipmentVaultDetail(
    val summary: ShipmentVaultSummary,
    val purchaseOrders: List<PurchaseOrderVaultSummary>,
    val products: List<ProductVaultRow>,
    val importCosts: List<ImportCostItem>,
    val landedCostAllocations: List<LandedCostAllocation>,
    val landedCostSnapshots: List<LandedCostSnapshot>,
    val warehouseReceipts: List<WarehouseReceipt>,
    val costAdjustments: List<InventoryCostAdjustment>,
    val directExpenseVouchers: List<ExpenseVoucher>,
    val sharedExpenseAllocations: List<ExpenseAllocation>,
    val vaultEntry: ImportVaultEntry?,
    val attachments: List<ImportVaultAttachment>,
    val poAllocations: List<ShipmentPoAllocationSummary> = emptyList(),
    val cartons: List<ShipmentCarton> = emptyList(),
    val saleSources: List<VaultSaleSourceRow> = emptyList(),
    val returnSources: List<VaultReturnSourceRow> = emptyList(),
    val costLayers: List<VaultCostLayerRow> = emptyList(),
    val timelineEvents: List<VaultTimelineEvent> = emptyList(),
    val documentRefs: List<VaultDocumentRef> = emptyList(),
    val supplierPaidMinor: Long = 0L,
    val supplierPayableMinor: Long = 0L,
    val additionalCosts: List<AdditionalProductCost> = emptyList(),
    val damageRecords: List<DamageRecord> = emptyList(),
    val shortageRecords: List<ShortageRecord> = emptyList(),
    val salesTrend: List<VaultTrendPoint> = emptyList()
)

data class PurchaseOrderVaultDetail(
    val summary: PurchaseOrderVaultSummary,
    val shipments: List<ImportShipment>,
    val products: List<ProductVaultRow>,
    val additionalCosts: List<AdditionalProductCost>,
    val supplierPayments: List<SupplierPayment>,
    val damageRecords: List<DamageRecord>,
    val shortageRecords: List<ShortageRecord>,
    val costAdjustments: List<InventoryCostAdjustment>,
    val directExpenseVouchers: List<ExpenseVoucher>,
    val sharedExpenseAllocations: List<ExpenseAllocation>,
    val vaultEntry: ImportVaultEntry?,
    val attachments: List<ImportVaultAttachment>,
    val shipmentAllocations: List<ShipmentPoAllocationSummary> = emptyList(),
    val warehouseReceipts: List<WarehouseReceipt> = emptyList(),
    val cartons: List<ShipmentCarton> = emptyList(),
    val saleSources: List<VaultSaleSourceRow> = emptyList(),
    val returnSources: List<VaultReturnSourceRow> = emptyList(),
    val costLayers: List<VaultCostLayerRow> = emptyList(),
    val timelineEvents: List<VaultTimelineEvent> = emptyList(),
    val documentRefs: List<VaultDocumentRef> = emptyList(),
    val importCosts: List<ImportCostItem> = emptyList(),
    val landedCostSnapshots: List<LandedCostSnapshot> = emptyList(),
    val salesTrend: List<VaultTrendPoint> = emptyList()
)

data class ImportVaultDataset(
    val products: List<Product>,
    val variants: List<ProductVariant>,
    val categories: List<ProductCategory>,
    val warehouses: List<Warehouse>,
    val suppliers: List<Supplier>,
    val purchaseOrders: List<PurchaseOrder>,
    val purchaseOrderItems: List<PurchaseOrderItem>,
    val supplierPayments: List<SupplierPayment>,
    val shipments: List<ImportShipment>,
    val cartons: List<ShipmentCarton>,
    val shipmentSuppliers: List<ShipmentSupplier>,
    val freightRates: List<ShipmentFreightRate>,
    val importCosts: List<ImportCostItem>,
    val landedAllocations: List<LandedCostAllocation>,
    val landedSnapshots: List<LandedCostSnapshot>,
    val warehouseReceipts: List<WarehouseReceipt>,
    val warehouseReceiptItems: List<WarehouseReceiptItem>,
    val costLayers: List<InventoryCostLayer>,
    val costAdjustments: List<InventoryCostAdjustment>,
    val fifoConsumptions: List<FifoConsumption>,
    val stockTransfers: List<StockTransfer>,
    val stockTransferCostLayers: List<StockTransferCostLayer>,
    val damageRecords: List<DamageRecord>,
    val shortageRecords: List<ShortageRecord>,
    val additionalProductCosts: List<AdditionalProductCost>,
    val salesInvoices: List<SalesInvoice>,
    val salesInvoiceItems: List<SalesInvoiceItem>,
    val saleLayerAllocations: List<SaleLayerAllocation>,
    val salesReturns: List<SalesReturn>,
    val salesReturnItems: List<SalesReturnItem>,
    val salesReturnLayerAllocations: List<SalesReturnLayerAllocation>,
    val customerPayments: List<CustomerPayment>,
    val paymentAllocations: List<PaymentAllocation>,
    val expenseVouchers: List<ExpenseVoucher>,
    val expenseAllocationRules: List<ExpenseAllocationRule>,
    val expenseAllocations: List<ExpenseAllocation>,
    val vaultEntries: List<ImportVaultEntry>,
    val vaultAttachments: List<ImportVaultAttachment>,
    val customerOpeningDueTotalMinor: Long = 0L,
    val productImages: List<ProductImage> = emptyList()
)
