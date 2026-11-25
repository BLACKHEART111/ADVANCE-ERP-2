package com.pocketdev.advance_erp.core.util

import com.pocketdev.advance_erp.core.engine.ImportVaultAnalyticsEngine
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.security.ImportVaultAccessGuard
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.ExpenseAllocation
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.domain.model.AgingSummary
import com.pocketdev.advance_erp.domain.model.CostFinalizationStatus
import com.pocketdev.advance_erp.domain.model.ExpenseAllocationMode
import com.pocketdev.advance_erp.domain.model.ImportVaultDataset
import com.pocketdev.advance_erp.domain.model.ImportVaultFilter
import com.pocketdev.advance_erp.domain.model.ProductVaultRow
import com.pocketdev.advance_erp.domain.model.PurchaseOrderVaultSummary
import com.pocketdev.advance_erp.domain.model.ShipmentVaultSummary
import com.pocketdev.advance_erp.domain.model.StockAgingBucket
import com.pocketdev.advance_erp.domain.model.UnknownAttributionBucket
import com.pocketdev.advance_erp.domain.model.VaultCostLayerRow
import com.pocketdev.advance_erp.domain.model.VaultDocumentCategory
import com.pocketdev.advance_erp.domain.model.VaultDocumentRef
import com.pocketdev.advance_erp.domain.model.VaultSaleSourceRow
import com.pocketdev.advance_erp.domain.model.VaultScopeType
import com.pocketdev.advance_erp.domain.model.VariantVaultRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class VaultExportScope {
    PORTFOLIO,
    SHIPMENT,
    PURCHASE_ORDER,
    PRODUCT,
    VARIANT
}

enum class VaultExportFormat {
    PDF,
    XLSX
}

data class VaultExportRequest(
    val scope: VaultExportScope = VaultExportScope.PORTFOLIO,
    val scopeId: String? = null,
    val variantId: String? = null,
    val parentScopeType: VaultScopeType? = null,
    val parentScopeId: String? = null,
    val filter: ImportVaultFilter = ImportVaultFilter(),
    val language: AppLanguage = AppLanguage.BANGLA,
    val role: UserRole = UserRole.OWNER_ADMIN,
    val businessProfile: BusinessProfile? = null
)

data class VaultExportSummaryTotals(
    val totalInvestedCapitalMinor: Long,
    val lockedInventoryCapitalMinor: Long,
    val netSalesRevenueMinor: Long,
    val cogsMinor: Long,
    val grossProfitMinor: Long,
    val directAttributedExpensesMinor: Long,
    val allocatedSharedOverheadMinor: Long,
    val unallocatedSharedOverheadMinor: Long,
    val inventoryLossesMinor: Long,
    val attributedNetProfitMinor: Long,
    val attributableCashCollectedMinor: Long,
    val attributableReceivableMinor: Long,
    val unrecoveredCapitalMinor: Long,
    val unattributedRevenueMinor: Long,
    val unattributedCogsMinor: Long,
    val unattributedInventoryCostMinor: Long,
    val unattributedCollectionsMinor: Long,
    val openingDueReceivableMinor: Long,
    val nonMerchandiseAdditionalChargesMinor: Long,
    val expectedRemainingRevenueMinor: Long,
    val expectedRemainingGrossProfitMinor: Long,
    val missingPriceSkuCount: Int,
    val profitLabel: String,
    val priceBasisLabel: String,
    val costFinalizationStatusLabel: String,
    val hasPendingReceiving: Boolean,
    val expenseAllocationMode: ExpenseAllocationMode
)

data class VaultExportExpenseRow(
    val expenseType: String,
    val id: String,
    val referenceNumber: String,
    val timestampMillis: Long,
    val category: String,
    val description: String,
    val treatmentLabel: String,
    val targetScopeId: String,
    val amountBdtMinor: Long,
    val notes: String
)

data class VaultExportReceivableRow(
    val invoiceNumber: String,
    val invoiceDateMillis: Long,
    val customerName: String,
    val attributedRevenueMinor: Long,
    val collectedCashMinor: Long,
    val receivableMinor: Long,
    val ageDays: Int,
    val notes: String
)

data class VaultExportPackage(
    val scope: VaultExportScope,
    val titleEn: String,
    val titleBn: String,
    val scopeReference: String,
    val currencyCode: String,
    val asOfMillis: Long,
    val filterSummary: String,
    val assumptions: List<String>,
    val warnings: List<String>,
    val totals: VaultExportSummaryTotals,
    val products: List<ProductVaultRow>,
    val variants: List<Pair<ProductVaultRow, VariantVaultRow>>,
    val salesAllocations: List<VaultSaleSourceRow>,
    val expenses: List<VaultExportExpenseRow>,
    val stockLayers: List<VaultCostLayerRow>,
    val receivables: List<VaultExportReceivableRow>,
    val documentsIndex: List<VaultDocumentRef>,
    val shipmentsIncluded: List<ShipmentVaultSummary>,
    val purchaseOrdersIncluded: List<PurchaseOrderVaultSummary>,
    val agingSummary: AgingSummary,
    val businessProfile: BusinessProfile?,
    val language: AppLanguage
) {
    val isBangla: Boolean get() = language == AppLanguage.BANGLA
}

/**
 * Pure Kotlin Import Vault Export Reconciliation Builder.
 * Builds a single UI-reconciled dataset snapshot across Portfolio, Shipment, PO, Product, or Variant scopes
 * for both Bangla-capable PDF and 8-sheet OpenXML XLSX generators.
 */
object ImportVaultExportBuilder {

    private const val DAY_MS = 24L * 3600L * 1000L

    fun buildPackage(
        dataset: ImportVaultDataset,
        request: VaultExportRequest
    ): VaultExportPackage {
        ImportVaultAccessGuard.requireVaultAccess(request.role)
        val filter = request.filter
        val effectiveAsOfMillis = minOf(filter.asOfMillis ?: filter.nowMillis, filter.nowMillis)
        val filterSummary = buildFilterSummary(dataset, filter, effectiveAsOfMillis)

        return when (request.scope) {
            VaultExportScope.PORTFOLIO -> buildPortfolioPackage(dataset, request, effectiveAsOfMillis, filterSummary)
            VaultExportScope.SHIPMENT -> buildShipmentPackage(dataset, request, effectiveAsOfMillis, filterSummary)
            VaultExportScope.PURCHASE_ORDER -> buildPurchaseOrderPackage(dataset, request, effectiveAsOfMillis, filterSummary)
            VaultExportScope.PRODUCT -> buildProductOrVariantPackage(dataset, request, effectiveAsOfMillis, filterSummary, isVariantScope = false)
            VaultExportScope.VARIANT -> buildProductOrVariantPackage(dataset, request, effectiveAsOfMillis, filterSummary, isVariantScope = true)
        }
    }

    private fun buildPortfolioPackage(
        dataset: ImportVaultDataset,
        request: VaultExportRequest,
        effectiveAsOfMillis: Long,
        filterSummary: String
    ): VaultExportPackage {
        val overview = ImportVaultAnalyticsEngine.computeOverview(dataset, request.filter)
        val products = ImportVaultAnalyticsEngine.computeProductProfitability(dataset, request.filter)
        val shipmentDetails = overview.shipments.mapNotNull {
            ImportVaultAnalyticsEngine.computeShipmentDetail(dataset, it.shipmentId, request.filter)
        }
        val poDetails = overview.purchaseOrders.mapNotNull {
            ImportVaultAnalyticsEngine.computePurchaseOrderDetail(dataset, it.purchaseOrderId, request.filter)
        }

        val sales = (shipmentDetails.flatMap { it.saleSources } + poDetails.flatMap { it.saleSources })
            .distinctBy { it.allocationId }
            .sortedByDescending { it.invoiceDate }
        val layers = (shipmentDetails.flatMap { it.costLayers } + poDetails.flatMap { it.costLayers })
            .distinctBy { it.layerId }
            .sortedByDescending { it.originReceiptDate }
        val scopedPoIds = overview.purchaseOrders.map { it.purchaseOrderId }.toSet()
        val scopedShipmentIds = overview.shipments.map { it.shipmentId }.toSet()
        val importCosts = (shipmentDetails.flatMap { it.importCosts } +
            dataset.importCosts.filter { it.purchaseOrderId in scopedPoIds || it.shipmentId in scopedShipmentIds })
            .distinctBy { it.id }
        val additionalCosts = poDetails.flatMap { it.additionalCosts }.distinctBy { it.id }
        val directVouchers = (shipmentDetails.flatMap { it.directExpenseVouchers } + poDetails.flatMap { it.directExpenseVouchers })
            .distinctBy { it.id }
        val sharedAllocations = (shipmentDetails.flatMap { it.sharedExpenseAllocations } + poDetails.flatMap { it.sharedExpenseAllocations })
            .distinctBy { it.id }

        val expenses = buildExpenseRows(
            dataset = dataset,
            importCosts = importCosts,
            additionalCosts = additionalCosts,
            directVouchers = directVouchers,
            sharedAllocations = sharedAllocations
        )
        val receivables = buildReceivableRows(dataset, sales, effectiveAsOfMillis)

        val portfolioLiveDoc = VaultDocumentRef(
            id = "doc-live-portfolio",
            category = VaultDocumentCategory.REPORTS,
            titleEn = "Live Regenerated Portfolio Report",
            titleBn = "লাইভ রিজেনারেটেড পোর্টফোলিও রিপোর্ট",
            subtitle = "Recomputed dynamically from live ERP records",
            referenceNumber = "PORTFOLIO",
            timestampMillis = effectiveAsOfMillis,
            amountMinor = null,
            currencyCode = "BDT",
            targetRoute = null,
            isImmutableSnapshot = false,
            isLiveRegeneratedReport = true
        )
        val portfolioAttachments = dataset.vaultAttachments
            .filter { it.scopeType == "PORTFOLIO" }
            .map { att ->
                val cat = runCatching { VaultDocumentCategory.valueOf(att.category) }.getOrElse {
                    if (att.isImmutableSnapshot) VaultDocumentCategory.REPORTS else VaultDocumentCategory.ATTACHMENTS
                }
                VaultDocumentRef(
                    id = "doc-att-${att.id}",
                    category = cat,
                    titleEn = att.title,
                    titleBn = att.title,
                    subtitle = "${att.category} · ${att.mimeType}",
                    referenceNumber = att.category,
                    timestampMillis = att.createdAt,
                    amountMinor = null,
                    currencyCode = "BDT",
                    targetRoute = null,
                    isImmutableSnapshot = att.isImmutableSnapshot,
                    attachmentId = att.id,
                    localFilePath = att.filePath,
                    mimeType = att.mimeType,
                    fileSizeBytes = att.fileSizeBytes
                )
            }
        val docs = (listOf(portfolioLiveDoc) +
            portfolioAttachments +
            (shipmentDetails.flatMap { it.documentRefs } + poDetails.flatMap { it.documentRefs })
                .filterNot { it.isLiveRegeneratedReport })
            .distinctBy { it.id }
            .sortedByDescending { it.timestampMillis }

        val hasProvisionalCost = overview.shipments.any { it.costFinalizationStatus == CostFinalizationStatus.PROVISIONAL } ||
            overview.purchaseOrders.any { it.costFinalizationStatus == CostFinalizationStatus.PROVISIONAL }
        val hasPendingRec = overview.shipments.any { it.hasPendingReceiving } ||
            overview.purchaseOrders.any { it.hasPendingReceiving }

        val totals = VaultExportSummaryTotals(
            totalInvestedCapitalMinor = overview.totalInvestedCapitalMinor,
            lockedInventoryCapitalMinor = overview.totalLockedInventoryCapitalMinor,
            netSalesRevenueMinor = overview.totalAttributedNetRevenueMinor,
            cogsMinor = overview.totalAttributedCogsMinor,
            grossProfitMinor = overview.totalAttributedGrossProfitMinor,
            directAttributedExpensesMinor = overview.totalDirectAttributedExpensesMinor,
            allocatedSharedOverheadMinor = overview.totalAllocatedSharedOverheadMinor,
            unallocatedSharedOverheadMinor = overview.unallocatedSharedOverheadMinor,
            inventoryLossesMinor = overview.totalInventoryLossesMinor,
            attributedNetProfitMinor = overview.totalAttributedNetProfitMinor,
            attributableCashCollectedMinor = overview.cashRecovery.attributableCashCollectedMinor,
            attributableReceivableMinor = overview.cashRecovery.attributableReceivableMinor,
            unrecoveredCapitalMinor = overview.cashRecovery.unrecoveredCapitalMinor,
            unattributedRevenueMinor = overview.unknownAttribution.unattributedRevenueMinor,
            unattributedCogsMinor = overview.unknownAttribution.unattributedCogsMinor,
            unattributedInventoryCostMinor = overview.unknownAttribution.unattributedInventoryCostMinor,
            unattributedCollectionsMinor = overview.unknownAttribution.unattributedCollectionsMinor,
            openingDueReceivableMinor = overview.unknownAttribution.openingDueReceivableMinor,
            nonMerchandiseAdditionalChargesMinor = overview.unknownAttribution.nonMerchandiseAdditionalChargesMinor,
            expectedRemainingRevenueMinor = overview.expectedRemainingProfit.expectedSellingRevenueMinor,
            expectedRemainingGrossProfitMinor = overview.expectedRemainingProfit.expectedGrossProfitMinor,
            missingPriceSkuCount = overview.expectedRemainingProfit.missingPriceSkuCount,
            profitLabel = overview.profitLabel,
            priceBasisLabel = overview.expectedRemainingProfit.priceBasisLabel,
            costFinalizationStatusLabel = if (hasProvisionalCost) "PROVISIONAL (Mixed)" else "FINALIZED",
            hasPendingReceiving = hasPendingRec,
            expenseAllocationMode = overview.expenseAllocationMode
        )

        val (assumptions, warnings) = buildAssumptionsAndWarnings(
            totals = totals,
            unknownBucket = overview.unknownAttribution,
            asOfMillis = effectiveAsOfMillis,
            language = request.language,
            openingStockCostMinor = overview.openingStockCostMinor,
            closingStockCostMinor = overview.closingStockCostMinor,
            isPeriodActivityFiltered = overview.isPeriodActivityFiltered
        )

        return VaultExportPackage(
            scope = VaultExportScope.PORTFOLIO,
            titleEn = "Import Vault Portfolio Report",
            titleBn = "ইমপোর্ট ভল্ট পোর্টফোলিও রিপোর্ট",
            scopeReference = "PORTFOLIO (${overview.shipmentCount} Shp / ${overview.purchaseOrderCount} PO)",
            currencyCode = "BDT",
            asOfMillis = effectiveAsOfMillis,
            filterSummary = filterSummary,
            assumptions = assumptions,
            warnings = warnings,
            totals = totals,
            products = products,
            variants = products.flatMap { p -> p.variants.map { p to it } },
            salesAllocations = sales,
            expenses = expenses,
            stockLayers = layers,
            receivables = receivables,
            documentsIndex = docs,
            shipmentsIncluded = overview.shipments,
            purchaseOrdersIncluded = overview.purchaseOrders,
            agingSummary = overview.agingSummary,
            businessProfile = request.businessProfile,
            language = request.language
        )
    }

    private fun buildShipmentPackage(
        dataset: ImportVaultDataset,
        request: VaultExportRequest,
        effectiveAsOfMillis: Long,
        filterSummary: String
    ): VaultExportPackage {
        val shipmentId = request.scopeId
            ?: throw IllegalArgumentException("Shipment ID is required for SHIPMENT export")
        val overview = ImportVaultAnalyticsEngine.computeOverview(dataset, request.filter)
        val detail = ImportVaultAnalyticsEngine.computeShipmentDetail(dataset, shipmentId, request.filter)
            ?: throw IllegalArgumentException("Shipment not found: $shipmentId")
        val s = detail.summary
        val mode = if (s.allocatedSharedOverheadMinor > 0L) ExpenseAllocationMode.ALLOCATED else overview.expenseAllocationMode
        val linkedPoIds = detail.purchaseOrders.map { it.purchaseOrderId }.toSet()
        val linkedAdditionalCosts = dataset.additionalProductCosts.filter { it.purchaseOrderId in linkedPoIds }
        val expenses = buildExpenseRows(
            dataset = dataset,
            importCosts = detail.importCosts,
            additionalCosts = linkedAdditionalCosts,
            directVouchers = detail.directExpenseVouchers,
            sharedAllocations = detail.sharedExpenseAllocations
        )
        val receivables = buildReceivableRows(dataset, detail.saleSources, effectiveAsOfMillis)

        val totals = VaultExportSummaryTotals(
            totalInvestedCapitalMinor = s.totalInvestedCapitalMinor,
            lockedInventoryCapitalMinor = s.lockedInventoryCapitalMinor,
            netSalesRevenueMinor = s.netSalesRevenueMinor,
            cogsMinor = s.cogsMinor,
            grossProfitMinor = s.grossProfitMinor,
            directAttributedExpensesMinor = s.directAttributedExpensesMinor,
            allocatedSharedOverheadMinor = s.allocatedSharedOverheadMinor,
            unallocatedSharedOverheadMinor = overview.unallocatedSharedOverheadMinor,
            inventoryLossesMinor = s.inventoryLossesMinor,
            attributedNetProfitMinor = s.attributedNetProfitMinor,
            attributableCashCollectedMinor = s.cashRecovery.attributableCashCollectedMinor,
            attributableReceivableMinor = s.cashRecovery.attributableReceivableMinor,
            unrecoveredCapitalMinor = s.cashRecovery.unrecoveredCapitalMinor,
            unattributedRevenueMinor = 0L,
            unattributedCogsMinor = 0L,
            unattributedInventoryCostMinor = 0L,
            unattributedCollectionsMinor = overview.unknownAttribution.unattributedCollectionsMinor,
            openingDueReceivableMinor = overview.unknownAttribution.openingDueReceivableMinor,
            nonMerchandiseAdditionalChargesMinor = overview.unknownAttribution.nonMerchandiseAdditionalChargesMinor,
            expectedRemainingRevenueMinor = s.expectedRemainingProfit.expectedSellingRevenueMinor,
            expectedRemainingGrossProfitMinor = s.expectedRemainingProfit.expectedGrossProfitMinor,
            missingPriceSkuCount = s.expectedRemainingProfit.missingPriceSkuCount,
            profitLabel = s.profitLabel,
            priceBasisLabel = s.expectedRemainingProfit.priceBasisLabel,
            costFinalizationStatusLabel = s.costFinalizationStatus.name,
            hasPendingReceiving = s.hasPendingReceiving,
            expenseAllocationMode = mode
        )
        val (assumptions, warnings) = buildAssumptionsAndWarnings(
            totals = totals,
            unknownBucket = null,
            asOfMillis = effectiveAsOfMillis,
            language = request.language
        )

        return VaultExportPackage(
            scope = VaultExportScope.SHIPMENT,
            titleEn = "Shipment Report · ${s.shipmentNumber}",
            titleBn = "শিপমেন্ট রিপোর্ট · ${s.shipmentNumber}",
            scopeReference = s.shipmentNumber,
            currencyCode = "BDT",
            asOfMillis = effectiveAsOfMillis,
            filterSummary = filterSummary,
            assumptions = assumptions,
            warnings = warnings,
            totals = totals,
            products = detail.products,
            variants = detail.products.flatMap { p -> p.variants.map { p to it } },
            salesAllocations = detail.saleSources,
            expenses = expenses,
            stockLayers = detail.costLayers,
            receivables = receivables,
            documentsIndex = detail.documentRefs,
            shipmentsIncluded = listOf(s),
            purchaseOrdersIncluded = detail.purchaseOrders,
            agingSummary = s.agingSummary,
            businessProfile = request.businessProfile,
            language = request.language
        )
    }

    private fun buildPurchaseOrderPackage(
        dataset: ImportVaultDataset,
        request: VaultExportRequest,
        effectiveAsOfMillis: Long,
        filterSummary: String
    ): VaultExportPackage {
        val poId = request.scopeId
            ?: throw IllegalArgumentException("Purchase Order ID is required for PURCHASE_ORDER export")
        val overview = ImportVaultAnalyticsEngine.computeOverview(dataset, request.filter)
        val detail = ImportVaultAnalyticsEngine.computePurchaseOrderDetail(dataset, poId, request.filter)
            ?: throw IllegalArgumentException("Purchase Order not found: $poId")
        val p = detail.summary
        val mode = if (p.allocatedSharedOverheadMinor > 0L) ExpenseAllocationMode.ALLOCATED else overview.expenseAllocationMode
        val linkedShipmentIds = detail.shipments.map { it.id }.toSet()
        val importCosts = dataset.importCosts.filter { it.purchaseOrderId == poId || it.shipmentId in linkedShipmentIds }
        val expenses = buildExpenseRows(
            dataset = dataset,
            importCosts = importCosts,
            additionalCosts = detail.additionalCosts,
            directVouchers = detail.directExpenseVouchers,
            sharedAllocations = detail.sharedExpenseAllocations
        )
        val receivables = buildReceivableRows(dataset, detail.saleSources, effectiveAsOfMillis)

        val totals = VaultExportSummaryTotals(
            totalInvestedCapitalMinor = p.totalInvestedCapitalMinor,
            lockedInventoryCapitalMinor = p.lockedInventoryCapitalMinor,
            netSalesRevenueMinor = p.netSalesRevenueMinor,
            cogsMinor = p.cogsMinor,
            grossProfitMinor = p.grossProfitMinor,
            directAttributedExpensesMinor = p.directAttributedExpensesMinor,
            allocatedSharedOverheadMinor = p.allocatedSharedOverheadMinor,
            unallocatedSharedOverheadMinor = overview.unallocatedSharedOverheadMinor,
            inventoryLossesMinor = p.inventoryLossesMinor,
            attributedNetProfitMinor = p.attributedNetProfitMinor,
            attributableCashCollectedMinor = p.cashRecovery.attributableCashCollectedMinor,
            attributableReceivableMinor = p.cashRecovery.attributableReceivableMinor,
            unrecoveredCapitalMinor = p.cashRecovery.unrecoveredCapitalMinor,
            unattributedRevenueMinor = 0L,
            unattributedCogsMinor = 0L,
            unattributedInventoryCostMinor = 0L,
            unattributedCollectionsMinor = overview.unknownAttribution.unattributedCollectionsMinor,
            openingDueReceivableMinor = overview.unknownAttribution.openingDueReceivableMinor,
            nonMerchandiseAdditionalChargesMinor = overview.unknownAttribution.nonMerchandiseAdditionalChargesMinor,
            expectedRemainingRevenueMinor = p.expectedRemainingProfit.expectedSellingRevenueMinor,
            expectedRemainingGrossProfitMinor = p.expectedRemainingProfit.expectedGrossProfitMinor,
            missingPriceSkuCount = p.expectedRemainingProfit.missingPriceSkuCount,
            profitLabel = p.profitLabel,
            priceBasisLabel = p.expectedRemainingProfit.priceBasisLabel,
            costFinalizationStatusLabel = p.costFinalizationStatus.name,
            hasPendingReceiving = p.hasPendingReceiving,
            expenseAllocationMode = mode
        )
        val (assumptions, warnings) = buildAssumptionsAndWarnings(
            totals = totals,
            unknownBucket = null,
            asOfMillis = effectiveAsOfMillis,
            language = request.language
        )

        return VaultExportPackage(
            scope = VaultExportScope.PURCHASE_ORDER,
            titleEn = "Purchase Order Report · ${p.poNumber}",
            titleBn = "পারচেজ অর্ডার রিপোর্ট · ${p.poNumber}",
            scopeReference = p.poNumber,
            currencyCode = "BDT",
            asOfMillis = effectiveAsOfMillis,
            filterSummary = filterSummary,
            assumptions = assumptions,
            warnings = warnings,
            totals = totals,
            products = detail.products,
            variants = detail.products.flatMap { prod -> prod.variants.map { prod to it } },
            salesAllocations = detail.saleSources,
            expenses = expenses,
            stockLayers = detail.costLayers,
            receivables = receivables,
            documentsIndex = detail.documentRefs,
            shipmentsIncluded = emptyList(),
            purchaseOrdersIncluded = listOf(p),
            agingSummary = p.agingSummary,
            businessProfile = request.businessProfile,
            language = request.language
        )
    }

    private fun buildProductOrVariantPackage(
        dataset: ImportVaultDataset,
        request: VaultExportRequest,
        effectiveAsOfMillis: Long,
        filterSummary: String,
        isVariantScope: Boolean
    ): VaultExportPackage {
        val productId = request.scopeId
            ?: throw IllegalArgumentException("Product ID is required for PRODUCT/VARIANT export")
        val overview = ImportVaultAnalyticsEngine.computeOverview(dataset, request.filter)

        val shipmentDetail = if (request.parentScopeType == VaultScopeType.SHIPMENT && request.parentScopeId != null) {
            ImportVaultAnalyticsEngine.computeShipmentDetail(dataset, request.parentScopeId, request.filter)
        } else null
        val poDetail = if (request.parentScopeType == VaultScopeType.PURCHASE_ORDER && request.parentScopeId != null) {
            ImportVaultAnalyticsEngine.computePurchaseOrderDetail(dataset, request.parentScopeId, request.filter)
        } else null

        val allProductsInScope: List<ProductVaultRow> = when {
            shipmentDetail != null -> shipmentDetail.products
            poDetail != null -> poDetail.products
            else -> ImportVaultAnalyticsEngine.computeProductProfitability(dataset, request.filter)
        }
        val productRow = allProductsInScope.find { it.productId == productId }
            ?: throw IllegalArgumentException("Product not found in selected scope: $productId")

        val allSalesInScope: List<VaultSaleSourceRow>
        val allLayersInScope: List<VaultCostLayerRow>
        val allDocsInScope: List<VaultDocumentRef>
        when {
            shipmentDetail != null -> {
                allSalesInScope = shipmentDetail.saleSources
                allLayersInScope = shipmentDetail.costLayers
                allDocsInScope = shipmentDetail.documentRefs
            }
            poDetail != null -> {
                allSalesInScope = poDetail.saleSources
                allLayersInScope = poDetail.costLayers
                allDocsInScope = poDetail.documentRefs
            }
            else -> {
                val shpDetails = overview.shipments.mapNotNull {
                    ImportVaultAnalyticsEngine.computeShipmentDetail(dataset, it.shipmentId, request.filter)
                }
                val poDetails = overview.purchaseOrders.mapNotNull {
                    ImportVaultAnalyticsEngine.computePurchaseOrderDetail(dataset, it.purchaseOrderId, request.filter)
                }
                allSalesInScope = (shpDetails.flatMap { it.saleSources } + poDetails.flatMap { it.saleSources }).distinctBy { it.allocationId }
                allLayersInScope = (shpDetails.flatMap { it.costLayers } + poDetails.flatMap { it.costLayers }).distinctBy { it.layerId }
                allDocsInScope = (shpDetails.flatMap { it.documentRefs } + poDetails.flatMap { it.documentRefs })
                    .filterNot { it.isLiveRegeneratedReport }
                    .distinctBy { it.id }
            }
        }

        val parentSuffixEn = when {
            shipmentDetail != null -> " (${shipmentDetail.summary.shipmentNumber})"
            poDetail != null -> " (${poDetail.summary.poNumber})"
            else -> ""
        }
        val costFinalization = when {
            shipmentDetail != null -> shipmentDetail.summary.costFinalizationStatus.name
            poDetail != null -> poDetail.summary.costFinalizationStatus.name
            overview.shipments.any { it.costFinalizationStatus == CostFinalizationStatus.PROVISIONAL } -> "PROVISIONAL (Mixed)"
            else -> "FINALIZED"
        }
        val pendingRec = when {
            shipmentDetail != null -> shipmentDetail.summary.hasPendingReceiving
            poDetail != null -> poDetail.summary.hasPendingReceiving
            else -> false
        }

        val selectedVariant: VariantVaultRow? = if (isVariantScope) {
            val vId = request.variantId
            productRow.variants.find { (vId != null && it.variantId == vId) || (vId == null && it.variantId == null) }
                ?: productRow.variants.find { it.sku == vId }
                ?: productRow.variants.firstOrNull()
        } else null

        val filteredSales = allSalesInScope.filter { row ->
            if (row.productId != productId) return@filter false
            if (selectedVariant != null) {
                val layerVariantId = allLayersInScope.find { it.layerId == row.costLayerId }?.variantId
                if (layerVariantId != null && selectedVariant.variantId != null) {
                    layerVariantId == selectedVariant.variantId
                } else {
                    row.variantLabel.equals(selectedVariant.variantLabel, ignoreCase = true)
                }
            } else {
                true
            }
        }
        val filteredLayers = allLayersInScope.filter { layer ->
            layer.productId == productId &&
                (selectedVariant == null || layer.variantId == selectedVariant.variantId)
        }
        val receivables = buildReceivableRows(dataset, filteredSales, effectiveAsOfMillis)

        val collectedCash = filteredSales.sumOf { it.collectedCashMinor }
        val receivable = filteredSales.sumOf { it.receivableMinor }
        val totals: VaultExportSummaryTotals
        val exportedVariants: List<Pair<ProductVaultRow, VariantVaultRow>>
        val aging: AgingSummary
        val titleEn: String
        val titleBn: String
        val refCode: String
        val expenses: List<VaultExportExpenseRow>

        if (selectedVariant != null) {
            exportedVariants = listOf(productRow to selectedVariant)
            aging = selectedVariant.agingSummary
            refCode = selectedVariant.sku
            titleEn = "Variant Report · ${productRow.productNameEn} (${selectedVariant.variantLabel})$parentSuffixEn"
            titleBn = "ভ্যারিয়েন্ট রিপোর্ট · ${productRow.productNameBn} (${selectedVariant.variantLabel})$parentSuffixEn"
            val invested = selectedVariant.cogsMinor + selectedVariant.lockedInventoryCostMinor + selectedVariant.inventoryLossMinor
            totals = VaultExportSummaryTotals(
                totalInvestedCapitalMinor = invested,
                lockedInventoryCapitalMinor = selectedVariant.lockedInventoryCostMinor,
                netSalesRevenueMinor = selectedVariant.netSalesRevenueMinor,
                cogsMinor = selectedVariant.cogsMinor,
                grossProfitMinor = selectedVariant.grossProfitMinor,
                directAttributedExpensesMinor = selectedVariant.directExpensesMinor,
                allocatedSharedOverheadMinor = selectedVariant.allocatedSharedOverheadMinor,
                unallocatedSharedOverheadMinor = overview.unallocatedSharedOverheadMinor,
                inventoryLossesMinor = selectedVariant.inventoryLossMinor,
                attributedNetProfitMinor = selectedVariant.attributedNetProfitMinor,
                attributableCashCollectedMinor = collectedCash,
                attributableReceivableMinor = receivable,
                unrecoveredCapitalMinor = maxOf(0L, invested - collectedCash),
                unattributedRevenueMinor = 0L,
                unattributedCogsMinor = 0L,
                unattributedInventoryCostMinor = 0L,
                unattributedCollectionsMinor = 0L,
                openingDueReceivableMinor = 0L,
                nonMerchandiseAdditionalChargesMinor = 0L,
                expectedRemainingRevenueMinor = selectedVariant.expectedRemainingProfit.expectedSellingRevenueMinor,
                expectedRemainingGrossProfitMinor = selectedVariant.expectedRemainingProfit.expectedGrossProfitMinor,
                missingPriceSkuCount = selectedVariant.expectedRemainingProfit.missingPriceSkuCount,
                profitLabel = overview.profitLabel,
                priceBasisLabel = selectedVariant.expectedRemainingProfit.priceBasisLabel,
                costFinalizationStatusLabel = costFinalization,
                hasPendingReceiving = pendingRec,
                expenseAllocationMode = if (selectedVariant.allocatedSharedOverheadMinor > 0L) ExpenseAllocationMode.ALLOCATED else overview.expenseAllocationMode
            )
            expenses = buildScopedProductExpenseRows(
                scopeLabel = "${productRow.productNameEn} (${selectedVariant.variantLabel})",
                scopeId = selectedVariant.sku,
                asOfMillis = effectiveAsOfMillis,
                directExpensesMinor = selectedVariant.directExpensesMinor,
                allocatedOverheadMinor = selectedVariant.allocatedSharedOverheadMinor,
                inventoryLossMinor = selectedVariant.inventoryLossMinor
            )
        } else {
            exportedVariants = productRow.variants.map { productRow to it }
            aging = productRow.agingSummary
            refCode = productRow.baseSku
            titleEn = "Product Report · ${productRow.productNameEn}$parentSuffixEn"
            titleBn = "প্রোডাক্ট রিপোর্ট · ${productRow.productNameBn}$parentSuffixEn"
            val invested = productRow.cogsMinor + productRow.lockedInventoryCostMinor + productRow.inventoryLossMinor
            totals = VaultExportSummaryTotals(
                totalInvestedCapitalMinor = invested,
                lockedInventoryCapitalMinor = productRow.lockedInventoryCostMinor,
                netSalesRevenueMinor = productRow.netSalesRevenueMinor,
                cogsMinor = productRow.cogsMinor,
                grossProfitMinor = productRow.grossProfitMinor,
                directAttributedExpensesMinor = productRow.directExpensesMinor,
                allocatedSharedOverheadMinor = productRow.allocatedSharedOverheadMinor,
                unallocatedSharedOverheadMinor = overview.unallocatedSharedOverheadMinor,
                inventoryLossesMinor = productRow.inventoryLossMinor,
                attributedNetProfitMinor = productRow.attributedNetProfitMinor,
                attributableCashCollectedMinor = collectedCash,
                attributableReceivableMinor = receivable,
                unrecoveredCapitalMinor = maxOf(0L, invested - collectedCash),
                unattributedRevenueMinor = 0L,
                unattributedCogsMinor = 0L,
                unattributedInventoryCostMinor = 0L,
                unattributedCollectionsMinor = 0L,
                openingDueReceivableMinor = 0L,
                nonMerchandiseAdditionalChargesMinor = 0L,
                expectedRemainingRevenueMinor = productRow.expectedRemainingProfit.expectedSellingRevenueMinor,
                expectedRemainingGrossProfitMinor = productRow.expectedRemainingProfit.expectedGrossProfitMinor,
                missingPriceSkuCount = productRow.expectedRemainingProfit.missingPriceSkuCount,
                profitLabel = overview.profitLabel,
                priceBasisLabel = productRow.expectedRemainingProfit.priceBasisLabel,
                costFinalizationStatusLabel = costFinalization,
                hasPendingReceiving = pendingRec,
                expenseAllocationMode = if (productRow.allocatedSharedOverheadMinor > 0L) ExpenseAllocationMode.ALLOCATED else overview.expenseAllocationMode
            )
            expenses = buildScopedProductExpenseRows(
                scopeLabel = productRow.productNameEn,
                scopeId = productRow.baseSku,
                asOfMillis = effectiveAsOfMillis,
                directExpensesMinor = productRow.directExpensesMinor,
                allocatedOverheadMinor = productRow.allocatedSharedOverheadMinor,
                inventoryLossMinor = productRow.inventoryLossMinor
            )
        }

        val liveDoc = VaultDocumentRef(
            id = "doc-live-${if (selectedVariant != null) "var-${selectedVariant.sku}" else "prod-$productId"}",
            category = VaultDocumentCategory.REPORTS,
            titleEn = "Live Regenerated Statement ($refCode)",
            titleBn = "লাইভ রিজেনারেটেড স্টেটমেন্ট ($refCode)",
            subtitle = "Recomputed dynamically from live ERP records",
            referenceNumber = refCode,
            timestampMillis = effectiveAsOfMillis,
            amountMinor = null,
            currencyCode = "BDT",
            targetRoute = null,
            isImmutableSnapshot = false,
            isLiveRegeneratedReport = true
        )
        val invoiceRefs = filteredSales.mapNotNull { it.invoiceNumber }.toSet()
        val relatedDocs = allDocsInScope.filter { doc ->
            !doc.isLiveRegeneratedReport && (
                doc.category == VaultDocumentCategory.ATTACHMENTS ||
                    doc.category == VaultDocumentCategory.PURCHASE ||
                    doc.referenceNumber in invoiceRefs ||
                    doc.titleEn.contains(productRow.productNameEn, ignoreCase = true)
                )
        }
        val docs = (listOf(liveDoc) + relatedDocs).distinctBy { it.id }.sortedByDescending { it.timestampMillis }

        val (assumptions, warnings) = buildAssumptionsAndWarnings(
            totals = totals,
            unknownBucket = null,
            asOfMillis = effectiveAsOfMillis,
            language = request.language
        )

        return VaultExportPackage(
            scope = request.scope,
            titleEn = titleEn,
            titleBn = titleBn,
            scopeReference = refCode,
            currencyCode = "BDT",
            asOfMillis = effectiveAsOfMillis,
            filterSummary = filterSummary,
            assumptions = assumptions,
            warnings = warnings,
            totals = totals,
            products = listOf(productRow),
            variants = exportedVariants,
            salesAllocations = filteredSales,
            expenses = expenses,
            stockLayers = filteredLayers,
            receivables = receivables,
            documentsIndex = docs,
            shipmentsIncluded = shipmentDetail?.summary?.let { listOf(it) } ?: emptyList(),
            purchaseOrdersIncluded = poDetail?.summary?.let { listOf(it) } ?: emptyList(),
            agingSummary = aging,
            businessProfile = request.businessProfile,
            language = request.language
        )
    }

    private fun buildScopedProductExpenseRows(
        scopeLabel: String,
        scopeId: String,
        asOfMillis: Long,
        directExpensesMinor: Long,
        allocatedOverheadMinor: Long,
        inventoryLossMinor: Long
    ): List<VaultExportExpenseRow> {
        val rows = mutableListOf<VaultExportExpenseRow>()
        if (directExpensesMinor != 0L) {
            rows += VaultExportExpenseRow(
                expenseType = "Attributed Direct Operating Expense",
                id = "exp-direct-$scopeId",
                referenceNumber = scopeId,
                timestampMillis = asOfMillis,
                category = "DIRECT_EXPENSE",
                description = "Direct operating expenses attributed to $scopeLabel",
                treatmentLabel = "Attributed (Revenue/Cost Pro-Rata)",
                targetScopeId = scopeId,
                amountBdtMinor = directExpensesMinor,
                notes = "Reconciles to product/variant directExpensesMinor"
            )
        }
        if (allocatedOverheadMinor != 0L) {
            rows += VaultExportExpenseRow(
                expenseType = "Allocated Shared Overhead",
                id = "exp-oh-$scopeId",
                referenceNumber = scopeId,
                timestampMillis = asOfMillis,
                category = "SHARED_OVERHEAD",
                description = "Allocated overhead for $scopeLabel",
                treatmentLabel = "Allocated Overhead",
                targetScopeId = scopeId,
                amountBdtMinor = allocatedOverheadMinor,
                notes = "Reconciles to allocatedSharedOverheadMinor"
            )
        }
        if (inventoryLossMinor != 0L) {
            rows += VaultExportExpenseRow(
                expenseType = "Inventory Loss / Damage / Shortage",
                id = "exp-loss-$scopeId",
                referenceNumber = scopeId,
                timestampMillis = asOfMillis,
                category = "INVENTORY_LOSS",
                description = "Damage, shortage, or write-off loss for $scopeLabel",
                treatmentLabel = "FIFO Loss Deduction",
                targetScopeId = scopeId,
                amountBdtMinor = inventoryLossMinor,
                notes = "Reconciles to inventoryLossMinor"
            )
        }
        return rows
    }

    private fun buildExpenseRows(
        dataset: ImportVaultDataset,
        importCosts: List<ImportCostItem>,
        additionalCosts: List<AdditionalProductCost>,
        directVouchers: List<ExpenseVoucher>,
        sharedAllocations: List<ExpenseAllocation>
    ): List<VaultExportExpenseRow> {
        val voucherMap = dataset.expenseVouchers.associateBy { it.id }
        val ruleMap = dataset.expenseAllocationRules.associateBy { it.id }
        val rows = mutableListOf<VaultExportExpenseRow>()
        importCosts.forEach { cost ->
            rows += VaultExportExpenseRow(
                expenseType = "Import Cost / Freight",
                id = cost.id,
                referenceNumber = cost.documentReference ?: cost.costCategory,
                timestampMillis = cost.costDate,
                category = cost.costCategory,
                description = cost.description.ifBlank { cost.costCategory },
                treatmentLabel = if (cost.isEstimated) "Capitalized (Estimated)" else "Capitalized (${cost.allocationMethod})",
                targetScopeId = cost.purchaseOrderId ?: cost.shipmentId,
                amountBdtMinor = cost.bdtAmountMinor,
                notes = cost.notes.orEmpty()
            )
        }
        additionalCosts.forEach { ac ->
            rows += VaultExportExpenseRow(
                expenseType = "Additional PO Cost",
                id = ac.id,
                referenceNumber = ac.costNumber,
                timestampMillis = ac.expenseDate,
                category = ac.category,
                description = ac.description,
                treatmentLabel = if (ac.isCapitalizable) "Capitalized into FIFO Layers" else "Direct Operating Expense",
                targetScopeId = ac.purchaseOrderId,
                amountBdtMinor = ac.bdtAmountMinor,
                notes = ac.notes.orEmpty()
            )
        }
        directVouchers.forEach { ev ->
            rows += VaultExportExpenseRow(
                expenseType = "Direct Expense Voucher",
                id = ev.id,
                referenceNumber = ev.voucherNumber,
                timestampMillis = ev.voucherDate,
                category = ev.category,
                description = ev.descriptionEn.ifBlank { ev.descriptionBn },
                treatmentLabel = "Direct Attributed Operating Expense",
                targetScopeId = ev.shipmentId ?: ev.purchaseOrderId.orEmpty(),
                amountBdtMinor = ev.bdtAmountMinor,
                notes = ev.notes.orEmpty()
            )
        }
        sharedAllocations.forEach { alloc ->
            val v = voucherMap[alloc.expenseVoucherId]
            val r = ruleMap[alloc.ruleId]
            rows += VaultExportExpenseRow(
                expenseType = "Shared Overhead Allocation",
                id = alloc.id,
                referenceNumber = v?.voucherNumber ?: alloc.expenseVoucherId,
                timestampMillis = v?.voucherDate ?: alloc.createdAt,
                category = v?.category ?: "SHARED_OVERHEAD",
                description = v?.descriptionEn ?: (r?.ruleName ?: "Allocated Overhead"),
                treatmentLabel = "Allocated (${alloc.allocationBasis} · ${alloc.basisPercentage}%)",
                targetScopeId = "${alloc.targetScopeType}:${alloc.targetId}",
                amountBdtMinor = alloc.allocatedAmountBdtMinor,
                notes = r?.ruleName.orEmpty()
            )
        }
        return rows.sortedByDescending { it.timestampMillis }
    }

    private fun buildReceivableRows(
        dataset: ImportVaultDataset,
        sales: List<VaultSaleSourceRow>,
        effectiveAsOfMillis: Long
    ): List<VaultExportReceivableRow> {
        val invoiceMap = dataset.salesInvoices.associateBy { it.id }
        return sales
            .filter { it.receivableMinor > 0L || it.collectedCashMinor > 0L }
            .groupBy { it.invoiceId ?: it.allocationId }
            .mapNotNull { (key, rows) ->
                val outstanding = rows.sumOf { it.receivableMinor }
                val collected = rows.sumOf { it.collectedCashMinor }
                if (outstanding <= 0L && collected <= 0L) return@mapNotNull null
                val first = rows.first()
                val inv = first.invoiceId?.let { invoiceMap[it] }
                val dateMs = inv?.invoiceDate ?: first.invoiceDate
                val ageDays = maxOf(0L, (effectiveAsOfMillis - dateMs) / DAY_MS).toInt()
                VaultExportReceivableRow(
                    invoiceNumber = inv?.invoiceNumber ?: first.invoiceNumber.ifBlank { key },
                    invoiceDateMillis = dateMs,
                    customerName = inv?.customerName ?: "Linked Customer",
                    attributedRevenueMinor = rows.sumOf { it.netRevenueMinor },
                    collectedCashMinor = collected,
                    receivableMinor = outstanding,
                    ageDays = ageDays,
                    notes = if (outstanding > 0L) "Attributed from linked invoice payment allocations" else "Settled"
                )
            }
            .sortedByDescending { it.receivableMinor }
    }

    private fun buildFilterSummary(
        dataset: ImportVaultDataset,
        filter: ImportVaultFilter,
        effectiveAsOfMillis: Long
    ): String {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parts = mutableListOf<String>()
        parts += "As-Of: ${dateFmt.format(Date(effectiveAsOfMillis))}"
        if (filter.dateFromMillis != null || filter.dateToMillis != null) {
            val fromStr = filter.dateFromMillis?.let { dateFmt.format(Date(it)) } ?: "Start"
            val toStr = filter.dateToMillis?.let { dateFmt.format(Date(minOf(it, effectiveAsOfMillis))) } ?: dateFmt.format(Date(effectiveAsOfMillis))
            val modeLabel = if (filter.dateFilterTarget == com.pocketdev.advance_erp.domain.model.VaultDateFilterTarget.IMPORT_COHORT_DATES) {
                "Cohort"
            } else {
                "Activity"
            }
            parts += "Range ($modeLabel): $fromStr to $toStr"
        } else {
            parts += "Range: Lifetime"
        }
        filter.supplierId?.let { supId ->
            val name = dataset.suppliers.find { it.id == supId }?.companyName ?: supId
            parts += "Supplier: $name"
        }
        filter.warehouseId?.let { whId ->
            val name = dataset.warehouses.find { it.id == whId }?.name ?: whId
            parts += "Warehouse: $name"
        }
        filter.operationalStatus?.let { parts += "OpStatus: ${it.name}" }
        filter.commercialStatus?.let { parts += "CommStatus: ${it.name}" }
        if (filter.onlyProfitable) parts += "Only Profitable"
        if (filter.onlyLossMaking) parts += "Only Loss-Making"
        if (filter.onlyHighAging) parts += "Aging 90+ Only"
        if (filter.onlyPendingCost) parts += "Pending Cost Only"
        filter.searchQuery?.takeIf { it.isNotBlank() }?.let { parts += "Search: \"$it\"" }
        filter.allocationRuleId?.let { parts += "OverheadRule: $it" }
        return parts.joinToString(" | ")
    }

    private fun buildAssumptionsAndWarnings(
        totals: VaultExportSummaryTotals,
        unknownBucket: UnknownAttributionBucket?,
        asOfMillis: Long,
        language: AppLanguage,
        openingStockCostMinor: Long? = null,
        closingStockCostMinor: Long? = null,
        isPeriodActivityFiltered: Boolean = false
    ): Pair<List<String>, List<String>> {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val isBn = language == AppLanguage.BANGLA
        val assumptions = mutableListOf<String>()
        assumptions += if (isBn) {
            "মুদ্রা ও মূল্যায়ন: সব অঙ্ক BDT-তে; FIFO কস্ট-লেয়ার ও মূল রসিদ তারিখ অনুযায়ী স্টক এজিং (০-৩০, ৩১-৬০, ৬১-৯০, ৯০+ দিন) হিসাব করা হয়েছে।"
        } else {
            "Currency & Valuation: All values normalized to BDT; FIFO cost-layer attribution with origin receipt date stock aging (0-30, 31-60, 61-90, 90+ days)."
        }
        if (isPeriodActivityFiltered && openingStockCostMinor != null && closingStockCostMinor != null) {
            val openFmt = Formatters.formatMoney(openingStockCostMinor, CurrencyCode.BDT, language)
            val closeFmt = Formatters.formatMoney(closingStockCostMinor, CurrencyCode.BDT, language)
            assumptions += if (isBn) {
                "সময়সীমা অ্যাক্টিভিটি ফিল্টার: প্রারম্ভিক স্টক (Opening Stock) $openFmt এবং সমাপনী স্টক (Closing Stock) $closeFmt; আজীবন কোহর্ট মূলধন আলাদাভাবে প্রদর্শিত।"
            } else {
                "Period Activity Filter: Opening Stock $openFmt vs Closing Stock $closeFmt; lifetime cohort investment tracked separately from period sales/expense activity."
            }
        }
        assumptions += if (totals.expenseAllocationMode == ExpenseAllocationMode.UNALLOCATED) {
            if (isBn) {
                "শেয়ার্ড ওভারহেড নীতি: Default Unallocated — নিট মুনাফা '${totals.profitLabel}' হিসেবে দেখানো হয়েছে; অবন্টিত ওভারহেড (${Formatters.formatMoney(totals.unallocatedSharedOverheadMinor, CurrencyCode.BDT, language)}) আলাদাভাবে প্রদর্শিত।"
            } else {
                "Overhead Policy: Default Unallocated — Net Profit labeled '${totals.profitLabel}'; unallocated shared overhead (${Formatters.formatMoney(totals.unallocatedSharedOverheadMinor, CurrencyCode.BDT, language)}) kept separate."
            }
        } else {
            if (isBn) {
                "শেয়ার্ড ওভারহেড নীতি: অ্যাক্টিভ অ্যালোকেশন রুল অনুযায়ী বণ্টিত ওভারহেড (${Formatters.formatMoney(totals.allocatedSharedOverheadMinor, CurrencyCode.BDT, language)}) বাদ দেওয়া হয়েছে।"
            } else {
                "Overhead Policy: Opt-in allocation rule active (${Formatters.formatMoney(totals.allocatedSharedOverheadMinor, CurrencyCode.BDT, language)} allocated)."
            }
        }
        assumptions += if (isBn) {
            "সম্ভাব্য অবশিষ্ট লাভ: ${dateFmt.format(Date(asOfMillis))} তারিখের '${totals.priceBasisLabel}' অনুযায়ী হিসাবকৃত।"
        } else {
            "Expected Remaining Profit: Estimated using '${totals.priceBasisLabel}' as of ${dateFmt.format(Date(asOfMillis))}."
        }

        val warnings = mutableListOf<String>()
        if (totals.costFinalizationStatusLabel.contains("PROVISIONAL", ignoreCase = true)) {
            warnings += if (isBn) {
                "সতর্কতা (Provisional Cost): চূড়ান্ত ল্যান্ডেড কস্ট স্ন্যাপশট এখনো অনুমোদিত নয় বা আনুমানিক খরচ আছে।"
            } else {
                "Warning (Provisional Cost): Includes estimated/unfinalized landed cost figures."
            }
        }
        if (totals.hasPendingReceiving) {
            warnings += if (isBn) {
                "সতর্কতা (Pending Receiving): কিছু পণ্যের গুদাম রিসিভিং এখনো বাকি আছে।"
            } else {
                "Warning (Pending Receiving): Pending warehouse receipts remain for one or more items."
            }
        }
        if (totals.missingPriceSkuCount > 0) {
            warnings += if (isBn) {
                "সতর্কতা (Missing Price): ${totals.missingPriceSkuCount} টি SKU-তে বিক্রয়মূল্য সেট নেই, তাই সম্ভাব্য লাভ থেকে বাদ রাখা হয়েছে।"
            } else {
                "Warning (Missing Selling Price): ${totals.missingPriceSkuCount} SKUs have no configured selling price and are excluded from expected gross profit."
            }
        }
        if (unknownBucket != null) {
            if (unknownBucket.unattributedRevenueMinor != 0L || unknownBucket.unattributedInventoryCostMinor != 0L) {
                warnings += if (isBn) {
                    "অচিহ্নিত (Unattributed): রেভিনিউ ${Formatters.formatMoney(unknownBucket.unattributedRevenueMinor, CurrencyCode.BDT, language)}, স্টক কস্ট ${Formatters.formatMoney(unknownBucket.unattributedInventoryCostMinor, CurrencyCode.BDT, language)}।"
                } else {
                    "Unattributed Stock/Sales: Revenue ${Formatters.formatMoney(unknownBucket.unattributedRevenueMinor, CurrencyCode.BDT, language)}, Stock Cost ${Formatters.formatMoney(unknownBucket.unattributedInventoryCostMinor, CurrencyCode.BDT, language)}."
                }
            }
            if (unknownBucket.unattributedCollectionsMinor != 0L || unknownBucket.openingDueReceivableMinor != 0L) {
                warnings += if (isBn) {
                    "আলাদা কালেকশন/ওপেনিং বকেয়া: অচিহ্নিত কালেকশন ${Formatters.formatMoney(unknownBucket.unattributedCollectionsMinor, CurrencyCode.BDT, language)}, ওপেনিং বকেয়া ${Formatters.formatMoney(unknownBucket.openingDueReceivableMinor, CurrencyCode.BDT, language)}।"
                } else {
                    "Unattributed Collections / Opening Due: Collections ${Formatters.formatMoney(unknownBucket.unattributedCollectionsMinor, CurrencyCode.BDT, language)}, Opening Due ${Formatters.formatMoney(unknownBucket.openingDueReceivableMinor, CurrencyCode.BDT, language)}."
                }
            }
            if (unknownBucket.nonMerchandiseAdditionalChargesMinor != 0L) {
                warnings += if (isBn) {
                    "নন-মার্চেন্ডাইজ চার্জ (Tax/Additional Charges): ${Formatters.formatMoney(unknownBucket.nonMerchandiseAdditionalChargesMinor, CurrencyCode.BDT, language)} প্রোডাক্ট লাভ থেকে আলাদা রাখা হয়েছে।"
                } else {
                    "Non-Merchandise Additional Charges: ${Formatters.formatMoney(unknownBucket.nonMerchandiseAdditionalChargesMinor, CurrencyCode.BDT, language)} excluded from merchandise Gross Profit."
                }
            }
        }
        return assumptions to warnings
    }
}
