package com.pocketdev.advance_erp.core.engine

import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.core.util.ImportVaultAttachmentFileStore
import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.data.entity.ExpenseAllocation
import com.pocketdev.advance_erp.data.entity.ExpenseAllocationRule
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.InvoiceStatus
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ReturnRefundOption
import com.pocketdev.advance_erp.data.entity.SaleLayerAllocation
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesReturn
import com.pocketdev.advance_erp.data.entity.SalesReturnLayerAllocation
import com.pocketdev.advance_erp.data.entity.ShipmentStatus
import com.pocketdev.advance_erp.data.entity.StockTransferStatus
import com.pocketdev.advance_erp.domain.model.AgingSummary
import com.pocketdev.advance_erp.domain.model.CashRecoverySummary
import com.pocketdev.advance_erp.domain.model.CommercialStatus
import com.pocketdev.advance_erp.domain.model.CostFinalizationStatus
import com.pocketdev.advance_erp.domain.model.ExpectedRemainingProfitEstimate
import com.pocketdev.advance_erp.domain.model.ExpenseAllocationMode
import com.pocketdev.advance_erp.domain.model.ImportVaultDataset
import com.pocketdev.advance_erp.domain.model.ImportVaultFilter
import com.pocketdev.advance_erp.domain.model.ImportVaultOverview
import com.pocketdev.advance_erp.domain.model.OperationalStatus
import com.pocketdev.advance_erp.domain.model.ProductVaultRow
import com.pocketdev.advance_erp.domain.model.ProfitContributionBaseStatus
import com.pocketdev.advance_erp.domain.model.ProfitContributionMetric
import com.pocketdev.advance_erp.domain.model.PurchaseOrderVaultDetail
import com.pocketdev.advance_erp.domain.model.PurchaseOrderVaultSummary
import com.pocketdev.advance_erp.domain.model.QuantityByUnit
import com.pocketdev.advance_erp.domain.model.ShipmentPoAllocationSummary
import com.pocketdev.advance_erp.domain.model.ShipmentVaultDetail
import com.pocketdev.advance_erp.domain.model.ShipmentVaultSummary
import com.pocketdev.advance_erp.domain.model.StockAgingBucket
import com.pocketdev.advance_erp.domain.model.UnknownAttributionBucket
import com.pocketdev.advance_erp.domain.model.VaultCostLayerRow
import com.pocketdev.advance_erp.domain.model.VaultDateFilterTarget
import com.pocketdev.advance_erp.domain.model.VaultDocumentCategory
import com.pocketdev.advance_erp.domain.model.VaultDocumentRef
import com.pocketdev.advance_erp.domain.model.VaultReturnSourceRow
import com.pocketdev.advance_erp.domain.model.VaultSaleSourceRow
import com.pocketdev.advance_erp.domain.model.VaultScopeType
import com.pocketdev.advance_erp.domain.model.VaultTimelineEvent
import com.pocketdev.advance_erp.domain.model.VaultTimelineEventType
import com.pocketdev.advance_erp.domain.model.VaultTrendPoint
import com.pocketdev.advance_erp.domain.model.VariantVaultRow
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Locale

/**
 * Pure Kotlin Import Vault Calculation, Reconciliation & Attribution Engine.
 * All financial values are calculated in integer minor BDT units (paisa) with deterministic rounding.
 */
object ImportVaultAnalyticsEngine {

    private const val DAY_MS = 24L * 3600L * 1000L
    private const val LABEL_ATTRIBUTED_NET_PROFIT = "Attributed Net Profit"
    private const val LABEL_ALLOCATED_NET_PROFIT = "Net Profit (Allocated)"
    private const val PRICE_BASIS_CURRENT = "Current Selling Price"
    private val SYNTHETIC_SHIPMENT_IDS = setOf("RET", "ADJ", "RESTOCK", "INIT", "")
    private val SYNTHETIC_LAYER_SOURCES = setOf("OPENING_STOCK", "MANUAL_ADJUSTMENT", "LEGACY_UNKNOWN")

    private data class LayerAsOfState(
        val layer: InventoryCostLayer,
        val asOfRemainingQty: Int,
        val effectivePoId: String?,
        val effectiveShipmentId: String?
    )

    private data class NetSaleSliceMetrics(
        val allocation: SaleLayerAllocation,
        val eventDate: Long,
        val netSoldQty: Int,
        val returnedGoodQty: Int,
        val returnedDamagedQty: Int,
        val netRevenueMinor: Long,
        val netCogsMinor: Long,
        val damageLossMinor: Long,
        val collectedCashMinor: Long,
        val receivableMinor: Long,
        val effectivePoId: String?,
        val effectiveShipmentId: String?
    )

    private data class InTransitTransferSlice(
        val transferId: String,
        val destinationWarehouseId: String,
        val productId: String,
        val variantId: String?,
        val purchaseOrderId: String?,
        val shipmentId: String?,
        val quantity: Int,
        val unitCostMinor: Long,
        val originReceiptDate: Long
    )

    private data class ResolvedExpenseContext(
        val mode: ExpenseAllocationMode,
        val ruleId: String?,
        val profitLabel: String,
        val sharedOverheadPoolMinor: Long,
        val allocatedByShipment: Map<String, Long>,
        val allocatedByPo: Map<String, Long>,
        val allComputedAllocations: List<ExpenseAllocation>
    )

    fun classifyAgingBucket(originReceiptDate: Long, asOfMillis: Long): StockAgingBucket {
        val ageDays = maxOf(0L, (asOfMillis - originReceiptDate) / DAY_MS).toInt()
        return when {
            ageDays <= 30 -> StockAgingBucket.DAYS_0_30
            ageDays <= 60 -> StockAgingBucket.DAYS_31_60
            ageDays <= 90 -> StockAgingBucket.DAYS_61_90
            else -> StockAgingBucket.DAYS_90_PLUS
        }
    }

    fun calculateContribution(childProfitMinor: Long, parentProfitMinor: Long): ProfitContributionMetric {
        return when {
            parentProfitMinor > 0L -> ProfitContributionMetric(
                childProfitMinor = childProfitMinor,
                parentProfitMinor = parentProfitMinor,
                contributionPercent = (childProfitMinor.toDouble() / parentProfitMinor.toDouble()) * 100.0,
                baseStatus = ProfitContributionBaseStatus.POSITIVE_BASE
            )
            parentProfitMinor == 0L -> ProfitContributionMetric(
                childProfitMinor = childProfitMinor,
                parentProfitMinor = parentProfitMinor,
                contributionPercent = null,
                baseStatus = ProfitContributionBaseStatus.ZERO_BASE
            )
            else -> ProfitContributionMetric(
                childProfitMinor = childProfitMinor,
                parentProfitMinor = parentProfitMinor,
                contributionPercent = null,
                baseStatus = ProfitContributionBaseStatus.LOSS_BASE
            )
        }
    }

    fun previewExpenseRuleAllocations(
        dataset: ImportVaultDataset,
        rule: ExpenseAllocationRule,
        asOfMillis: Long = System.currentTimeMillis()
    ): List<ExpenseAllocation> {
        if (rule.allocationBasis == "UNALLOCATED") return emptyList()
        val ctx = buildContext(dataset, ImportVaultFilter(asOfMillis = asOfMillis, nowMillis = asOfMillis))
        val poSummaries = computeAllPoSummaries(ctx, emptyMap(), LABEL_ALLOCATED_NET_PROFIT)
        val shipSummaries = computeAllShipmentSummaries(ctx, emptyMap(), LABEL_ALLOCATED_NET_PROFIT)

        if (rule.allocationBasis == "QUANTITY") {
            validateSameUnitForQuantityAllocation(ctx, rule.targetScopeType, poSummaries, shipSummaries)
        }

        val targetVouchers = dataset.expenseVouchers.filter {
            it.isPosted && !it.isReversed &&
                it.purchaseOrderId == null && it.shipmentId == null && it.salesInvoiceId == null &&
                it.voucherDate in rule.periodStartMillis..rule.periodEndMillis &&
                it.voucherDate <= asOfMillis
        }
        if (targetVouchers.isEmpty()) return emptyList()

        val out = mutableListOf<ExpenseAllocation>()

        val targets: List<Pair<String, BigDecimal>> = if (rule.targetScopeType == VaultScopeType.PURCHASE_ORDER.name) {
            poSummaries.filter { it.operationalStatus != OperationalStatus.CANCELLED }.map { po ->
                val landedFallback = (po.lockedInventoryCapitalMinor + po.cogsMinor).takeIf { it > 0L } ?: po.purchaseCostBdtMinor
                val weight = when (rule.allocationBasis) {
                    "NET_REVENUE" -> BigDecimal(maxOf(0L, po.netSalesRevenueMinor))
                    "LANDED_VALUE" -> BigDecimal(maxOf(0L, po.totalInvestedCapitalMinor.takeIf { it > 0L } ?: landedFallback))
                    "QUANTITY" -> BigDecimal(maxOf(0, ctx.receivedGoodQtyForPo(po.purchaseOrderId)))
                    else -> BigDecimal(maxOf(0L, po.totalInvestedCapitalMinor.takeIf { it > 0L } ?: landedFallback))
                }
                po.purchaseOrderId to weight
            }
        } else {
            shipSummaries.filter { it.operationalStatus != OperationalStatus.CANCELLED }.map { shp ->
                val landedFallback = (shp.lockedInventoryCapitalMinor + shp.cogsMinor).takeIf { it > 0L }
                    ?: (shp.purchaseCostBdtMinor + shp.chinaDeliveryBdtMinor + shp.internationalFreightBdtMinor + shp.bdTransportAndOtherImportMinor)
                val weight = when (rule.allocationBasis) {
                    "NET_REVENUE" -> BigDecimal(maxOf(0L, shp.netSalesRevenueMinor))
                    "LANDED_VALUE" -> BigDecimal(maxOf(0L, shp.totalInvestedCapitalMinor.takeIf { it > 0L } ?: landedFallback))
                    "QUANTITY" -> BigDecimal(maxOf(0, ctx.receivedGoodQtyForShipment(shp.shipmentId)))
                    "WEIGHT" -> shp.verifiedBillableWeightKg.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    else -> BigDecimal(maxOf(0L, shp.totalInvestedCapitalMinor.takeIf { it > 0L } ?: landedFallback))
                }
                shp.shipmentId to weight
            }
        }
        if (targets.isEmpty()) return emptyList()

        val totalBasis = targets.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.second) }
        for (voucher in targetVouchers) {
            val allocatedMap = DeterministicAllocation.allocateProportionally(voucher.bdtAmountMinor, targets)
            for ((targetId, basisVal) in targets) {
                val amt = allocatedMap[targetId] ?: 0L
                val pct = if (totalBasis > BigDecimal.ZERO) {
                    basisVal.multiply(BigDecimal("100")).divide(totalBasis, 4, RoundingMode.HALF_UP)
                } else {
                    BigDecimal("100").divide(BigDecimal(targets.size), 4, RoundingMode.HALF_UP)
                }
                out.add(
                    ExpenseAllocation(
                        ruleId = rule.id,
                        expenseVoucherId = voucher.id,
                        targetScopeType = rule.targetScopeType,
                        targetId = targetId,
                        allocationBasis = rule.allocationBasis,
                        basisValue = basisVal.toPlainString(),
                        basisPercentage = pct.toPlainString(),
                        allocatedAmountBdtMinor = amt,
                        createdAt = asOfMillis
                    )
                )
            }
        }
        return out
    }

    private fun validateSameUnitForQuantityAllocation(
        ctx: Context,
        targetScopeType: String,
        poSummaries: List<PurchaseOrderVaultSummary>,
        shipSummaries: List<ShipmentVaultSummary>
    ) {
        val observedUnits = linkedSetOf<String>()
        var hasMixed = false
        if (targetScopeType == VaultScopeType.PURCHASE_ORDER.name) {
            poSummaries.filter { it.operationalStatus != OperationalStatus.CANCELLED }.forEach { po ->
                val q = if (po.receivedGoodQtyByUnit.byUom.values.sum() > 0) {
                    po.receivedGoodQtyByUnit
                } else {
                    po.orderedQtyByUnit
                }
                if (q.hasMixedUnits) hasMixed = true
                val activeKeys = q.byUom.filterValues { it > 0 }.keys.ifEmpty { q.byUom.keys }
                observedUnits.addAll(activeKeys)
            }
        } else {
            shipSummaries.filter { it.operationalStatus != OperationalStatus.CANCELLED }.forEach { shp ->
                val q = if (shp.receivedGoodQtyByUnit.byUom.values.sum() > 0) {
                    shp.receivedGoodQtyByUnit
                } else {
                    val linkedItems = ctx.dataset.purchaseOrderItems.filter { it.purchaseOrderId in shp.purchaseOrderIds }
                    QuantityByUnit.fromItems(
                        linkedItems.map { (ctx.productMap[it.productId]?.unit ?: "PCS") to it.quantity }
                    )
                }
                if (q.hasMixedUnits) hasMixed = true
                val activeKeys = q.byUom.filterValues { it > 0 }.keys.ifEmpty { q.byUom.keys }
                observedUnits.addAll(activeKeys)
            }
        }
        if (hasMixed || observedUnits.size > 1) {
            throw IllegalArgumentException(
                "Shared overhead allocation by QUANTITY is only supported when all scopes share the same unit " +
                    "(found mixed units: ${observedUnits.sorted().joinToString(", ")}). Use LANDED_VALUE, NET_REVENUE, or WEIGHT instead."
            )
        }
    }

    fun computeOverview(dataset: ImportVaultDataset, filter: ImportVaultFilter = ImportVaultFilter()): ImportVaultOverview {
        val ctx = buildContext(dataset, filter)
        val expCtx = resolveExpenses(ctx, filter)

        val allShipments = computeAllShipmentSummaries(ctx, expCtx.allocatedByShipment, expCtx.profitLabel)
            .filter { matchesShipmentFilter(it, ctx, filter) }
        val allPos = computeAllPoSummaries(ctx, expCtx.allocatedByPo, expCtx.profitLabel)
            .filter { matchesPoFilter(it, ctx, filter) }

        val activePos = ctx.filteredPos(filter)
        val activePoIds = activePos.map { it.id }.toSet()
        val filteredLayers = ctx.filteredAttributedLayers(filter)
        val filteredTransit = ctx.filteredAttributedTransit(filter)
        val filteredSales = ctx.filteredAttributedSales(filter)

        val totalInvestedCapital = activePos.sumOf { po ->
            val rawPoSummary = allPos.find { it.purchaseOrderId == po.id }
            rawPoSummary?.totalInvestedCapitalMinor ?: computePoInvestedCapital(ctx, po.id)
        } + ctx.orphanShipmentExtraCapital(filter)

        val lockedGoodCost = filteredLayers.sumOf { it.asOfRemainingQty.toLong() * it.layer.unitLandedCostBdtMinor }
        val lockedTransitCost = filteredTransit.sumOf { it.quantity.toLong() * it.unitCostMinor }
        val totalLockedCapital = lockedGoodCost + lockedTransitCost

        val totalAttributedRev = filteredSales.sumOf { it.netRevenueMinor }
        val totalAttributedSaleCogs = filteredSales.sumOf { it.netCogsMinor }
        val capitalizableAdditionalCogs = ctx.additionalProductCosts
            .filter { it.isCapitalizable && ctx.inActivityWindow(it.expenseDate) && (it.purchaseOrderId in activePoIds) }
            .filter { filter.supplierId == null || ctx.poMap[it.purchaseOrderId]?.supplierId == filter.supplierId }
            .sumOf { it.allocatedToCogsMinor }
        val postFinalizeCogsAdj = ctx.costAdjustments
            .filter { it.referenceType == "POST_FINALIZE_IMPORT_EXPENSE" && ctx.inActivityWindow(it.createdAt) }
            .filter { it.purchaseOrderId == null || it.purchaseOrderId in activePoIds }
            .filter { filter.supplierId == null || ctx.poMap[it.purchaseOrderId]?.supplierId == filter.supplierId }
            .filter { filter.productId == null || it.productId == filter.productId }
            .filter { filter.variantId == null || it.variantId == filter.variantId }
            .sumOf { it.cogsAdjustmentAmountMinor }
        val totalAttributedCogs = totalAttributedSaleCogs + capitalizableAdditionalCogs + postFinalizeCogsAdj
        val totalAttributedGp = totalAttributedRev - totalAttributedCogs

        val directExpensePos = activePos.sumOf { computePoDirectOperatingExpense(ctx, it.id) }
        val directExpenseOrphanShipments = ctx.orphanShipmentDirectExpenses(filter)
        val directDeliveryExpenses = filteredSales.mapNotNull { it.allocation.invoiceId }.distinct()
            .sumOf { invId -> ctx.attributedDeliveryExpenseForInvoice(invId) }
        val totalDirectExpenses = directExpensePos + directExpenseOrphanShipments + directDeliveryExpenses

        val totalAllocatedShared = if (expCtx.mode == ExpenseAllocationMode.ALLOCATED) {
            expCtx.allocatedByPo.values.sum() + expCtx.allocatedByShipment.values.sum()
        } else 0L
        val unallocatedShared = if (expCtx.mode == ExpenseAllocationMode.UNALLOCATED) {
            expCtx.sharedOverheadPoolMinor
        } else 0L

        val totalInventoryLosses = computePortfolioInventoryLosses(ctx, filter, filteredSales)
        val totalNetProfit = totalAttributedGp - totalDirectExpenses - totalAllocatedShared - totalInventoryLosses

        val isPeriodActivityFiltered = ctx.isActivityDateFiltered
        val (lifetimeNetProfit, lifetimeRoi) = if (isPeriodActivityFiltered) {
            val lifetimeFilter = filter.copy(dateFromMillis = null, dateToMillis = null, asOfMillis = ctx.asOfMillis)
            val lCtx = buildContext(dataset, lifetimeFilter)
            val lExpCtx = resolveExpenses(lCtx, lifetimeFilter)
            val lActivePos = lCtx.filteredPos(lifetimeFilter)
            val lActivePoIds = lActivePos.map { it.id }.toSet()
            val lSales = lCtx.filteredAttributedSales(lifetimeFilter)
            val lRev = lSales.sumOf { it.netRevenueMinor }
            val lCapAddCogs = lCtx.additionalProductCosts
                .filter { it.isCapitalizable && it.expenseDate <= lCtx.asOfMillis && it.purchaseOrderId in lActivePoIds }
                .filter { filter.supplierId == null || lCtx.poMap[it.purchaseOrderId]?.supplierId == filter.supplierId }
                .sumOf { it.allocatedToCogsMinor }
            val lPostFinCogs = lCtx.costAdjustments
                .filter { it.referenceType == "POST_FINALIZE_IMPORT_EXPENSE" && it.createdAt <= lCtx.asOfMillis }
                .filter { it.purchaseOrderId == null || it.purchaseOrderId in lActivePoIds }
                .filter { filter.supplierId == null || lCtx.poMap[it.purchaseOrderId]?.supplierId == filter.supplierId }
                .filter { filter.productId == null || it.productId == filter.productId }
                .filter { filter.variantId == null || it.variantId == filter.variantId }
                .sumOf { it.cogsAdjustmentAmountMinor }
            val lCogs = lSales.sumOf { it.netCogsMinor } + lCapAddCogs + lPostFinCogs
            val lDirExp = lActivePos.sumOf { computePoDirectOperatingExpense(lCtx, it.id, applyActivityWindow = false) } +
                lCtx.orphanShipmentDirectExpenses(lifetimeFilter) +
                lSales.mapNotNull { it.allocation.invoiceId }.distinct().sumOf { lCtx.attributedDeliveryExpenseForInvoice(it) }
            val lAllocShared = if (lExpCtx.mode == ExpenseAllocationMode.ALLOCATED) {
                lExpCtx.allocatedByPo.values.sum() + lExpCtx.allocatedByShipment.values.sum()
            } else 0L
            val lInvLoss = computePortfolioInventoryLosses(lCtx, lifetimeFilter, lSales, applyActivityWindow = false)
            val lProfit = (lRev - lCogs) - lDirExp - lAllocShared - lInvLoss
            lProfit to roiPercent(lProfit, totalInvestedCapital)
        } else {
            totalNetProfit to roiPercent(totalNetProfit, totalInvestedCapital)
        }

        val openingStockCostMinor: Long? = if (filter.dateFromMillis != null && filter.dateFilterTarget == VaultDateFilterTarget.ACTIVITY_DATES) {
            val openingAsOf = filter.dateFromMillis - 1L
            val openingFilter = filter.copy(dateFromMillis = null, dateToMillis = null, asOfMillis = openingAsOf)
            val openCtx = buildContext(dataset, openingFilter)
            val openLayers = openCtx.filteredAttributedLayers(openingFilter)
            val openTransit = openCtx.filteredAttributedTransit(openingFilter)
            openLayers.sumOf { it.asOfRemainingQty.toLong() * it.layer.unitLandedCostBdtMinor } +
                openTransit.sumOf { it.quantity.toLong() * it.unitCostMinor }
        } else null

        val cashCollected = filteredSales.sumOf { it.collectedCashMinor }
        val attributableReceivable = filteredSales.sumOf { it.receivableMinor }
        val unrecovered = maxOf(0L, totalInvestedCapital - cashCollected)
        val cashRecovery = CashRecoverySummary(
            totalInvestedCapitalMinor = totalInvestedCapital,
            attributableCashCollectedMinor = cashCollected,
            attributableReceivableMinor = attributableReceivable,
            unrecoveredCapitalMinor = unrecovered,
            cashRecoveryRatioPercent = if (totalInvestedCapital > 0L) (cashCollected.toDouble() / totalInvestedCapital.toDouble()) * 100.0 else null,
            breakEvenReached = totalInvestedCapital > 0L && cashCollected >= totalInvestedCapital
        )

        val expectedRemaining = computeExpectedRemainingProfit(
            ctx = ctx,
            layers = filteredLayers,
            transfers = filteredTransit
        )
        val agingSummary = computeAgingSummary(filteredLayers, filteredTransit, ctx.asOfMillis)
        val unknownBucket = computeUnknownBucket(ctx, totalAttributedRev)
        val portfolioSaleRows = buildSaleSourceRows(ctx, filteredSales)
        val portfolioCostLayerRows = buildCostLayerRows(ctx, filteredLayers)
        val salesTrend = buildSalesTrend(portfolioSaleRows, filter, ctx.asOfMillis)
        val portfolioAttachments = dataset.vaultAttachments
            .filter { it.scopeType == "PORTFOLIO" }
            .sortedByDescending { it.createdAt }
            .map { att ->
                val resolvedCategory = runCatching { VaultDocumentCategory.valueOf(att.category) }.getOrElse {
                    if (att.isImmutableSnapshot) VaultDocumentCategory.REPORTS else VaultDocumentCategory.ATTACHMENTS
                }
                VaultDocumentRef(
                    id = "doc-att-${att.id}",
                    category = resolvedCategory,
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

        return ImportVaultOverview(
            asOfMillis = ctx.asOfMillis,
            expenseAllocationMode = expCtx.mode,
            activeAllocationRuleId = expCtx.ruleId,
            profitLabel = expCtx.profitLabel,
            totalInvestedCapitalMinor = totalInvestedCapital,
            totalLockedInventoryCapitalMinor = totalLockedCapital,
            totalAttributedNetRevenueMinor = totalAttributedRev,
            totalAttributedCogsMinor = totalAttributedCogs,
            totalAttributedGrossProfitMinor = totalAttributedGp,
            attributedGrossMarginPercent = marginPercent(totalAttributedGp, totalAttributedRev),
            totalDirectAttributedExpensesMinor = totalDirectExpenses,
            totalAllocatedSharedOverheadMinor = totalAllocatedShared,
            unallocatedSharedOverheadMinor = unallocatedShared,
            totalInventoryLossesMinor = totalInventoryLosses,
            totalAttributedNetProfitMinor = totalNetProfit,
            attributedNetMarginPercent = marginPercent(totalNetProfit, totalAttributedRev),
            portfolioRoiPercent = roiPercent(totalNetProfit, totalInvestedCapital),
            cashRecovery = cashRecovery,
            expectedRemainingProfit = expectedRemaining,
            agingSummary = agingSummary,
            unknownAttribution = unknownBucket,
            shipmentCount = allShipments.size,
            purchaseOrderCount = allPos.size,
            totalAttachmentCount = dataset.vaultAttachments.size,
            shipments = allShipments,
            purchaseOrders = allPos,
            isPeriodActivityFiltered = isPeriodActivityFiltered,
            openingStockCostMinor = openingStockCostMinor,
            closingStockCostMinor = totalLockedCapital,
            lifetimeNetProfitMinor = lifetimeNetProfit,
            lifetimePortfolioRoiPercent = lifetimeRoi,
            salesTrend = salesTrend,
            portfolioSaleSources = portfolioSaleRows,
            portfolioCostLayers = portfolioCostLayerRows,
            portfolioAttachments = portfolioAttachments
        )
    }

    fun computeShipmentDetail(
        dataset: ImportVaultDataset,
        shipmentId: String,
        filter: ImportVaultFilter = ImportVaultFilter()
    ): ShipmentVaultDetail? {
        val shipment = dataset.shipments.find { it.id == shipmentId } ?: return null
        val ctx = buildContext(dataset, filter)
        val expCtx = resolveExpenses(ctx, filter)
        val summary = computeShipmentSummary(ctx, shipment, expCtx.allocatedByShipment[shipmentId] ?: 0L, expCtx.profitLabel)

        val linkedPoIds = ctx.poIdsForShipment(shipmentId)
        val linkedPos = linkedPoIds.mapNotNull { poId ->
            ctx.poMap[poId]?.let { computePoSummary(ctx, it, expCtx.allocatedByPo[poId] ?: 0L, expCtx.profitLabel) }
        }
        val poNetProfitMap = linkedPos.associate { it.purchaseOrderId to it.attributedNetProfitMinor }
        val shipmentNetProfitMap = mapOf(shipmentId to summary.attributedNetProfitMinor)

        val scopeLayers = ctx.attributedLayers.filter { it.effectiveShipmentId == shipmentId }
        val scopeTransit = ctx.attributedTransit.filter { it.shipmentId == shipmentId }
        val scopeSales = ctx.filteredAttributedSales(filter.copy(productId = null, variantId = null, supplierId = null, warehouseId = null))
            .filter { it.effectiveShipmentId == shipmentId }
        val scopeDamages = ctx.damageRecordsForShipment(shipmentId, applyActivityWindow = true)
        val scopeShortages = ctx.shortageRecordsForShipment(shipmentId, applyActivityWindow = true)
        val scopeWriteOffs = ctx.manualWriteOffsForScope(shipmentId = shipmentId, poId = null, applyActivityWindow = true)

        val products = buildProductRowsForScope(
            ctx = ctx,
            scopeLayers = scopeLayers,
            scopeTransit = scopeTransit,
            scopeSales = scopeSales,
            scopeDamages = scopeDamages,
            scopeShortages = scopeShortages,
            scopeWriteOffs = scopeWriteOffs,
            parentNetProfitMinor = summary.attributedNetProfitMinor,
            directExpenseTotalMinor = summary.directAttributedExpensesMinor,
            sharedExpenseTotalMinor = summary.allocatedSharedOverheadMinor,
            additionalCogsTotalMinor = ctx.capitalizableAdditionalCogsForShipment(shipmentId, applyActivityWindow = true),
            postFinalizeCogsTotalMinor = ctx.postFinalizeCogsForShipment(shipmentId, applyActivityWindow = true),
            poNetProfitMap = poNetProfitMap,
            shipmentNetProfitMap = shipmentNetProfitMap,
            fixedParentPoId = null,
            fixedParentShipmentId = shipmentId
        )

        val receipts = dataset.warehouseReceipts.filter { it.shipmentId == shipmentId && it.receiptDate <= ctx.asOfMillis }
        val receiptIdSet = receipts.map { it.id }.toSet()
        val layerIds = ctx.costLayers.filter { it.shipmentId == shipmentId }.map { it.id }.toSet()
        val cartons = dataset.cartons.filter { it.shipmentId == shipmentId }
        val importCosts = dataset.importCosts.filter { it.shipmentId == shipmentId }
        val landedAllocations = dataset.landedAllocations.filter { it.shipmentId == shipmentId }
        val landedSnapshots = dataset.landedSnapshots.filter { it.shipmentId == shipmentId }
        val costAdjustments = dataset.costAdjustments.filter { it.referenceId == shipmentId || it.costLayerId in layerIds }
        val directExpenses = dataset.expenseVouchers.filter {
            it.isPosted && !it.isReversed && (it.shipmentId == shipmentId || it.purchaseOrderId in linkedPoIds)
        }
        val additionalCosts = dataset.additionalProductCosts.filter { it.purchaseOrderId in linkedPoIds }
        val allShipmentShortages = dataset.shortageRecords.filter {
            it.warehouseReceiptId in receiptIdSet && ctx.inActivityWindow(it.createdAt)
        }
        val weightedSupplierPaid = linkedPos.sumOf { po ->
            val share = ctx.shipmentShareOfPo(po.purchaseOrderId, shipmentId)
            BigDecimal(po.supplierPaidBdtMinor).multiply(share).setScale(0, RoundingMode.HALF_UP).longValueExact()
        }
        val weightedSupplierPayable = linkedPos.sumOf { po ->
            val share = ctx.shipmentShareOfPo(po.purchaseOrderId, shipmentId)
            BigDecimal(po.supplierPayableBdtMinor).multiply(share).setScale(0, RoundingMode.HALF_UP).longValueExact()
        }
        val vaultEntry = dataset.vaultEntries.find { it.scopeType == VaultScopeType.SHIPMENT.name && it.sourceId == shipmentId }
        val attachments = dataset.vaultAttachments.filter { it.scopeType == VaultScopeType.SHIPMENT.name && it.sourceId == shipmentId }

        val poAllocations = linkedPoIds.mapNotNull { poId ->
            computeShipmentPoAllocationSummary(ctx, shipmentId, poId, expCtx)
        }
        val saleSources = buildSaleSourceRows(ctx, scopeSales)
        val returnSources = buildReturnSourceRows(ctx, scopeSales)
        val costLayerRows = buildCostLayerRows(ctx, scopeLayers)
        val salesTrend = buildSalesTrend(saleSources, filter, ctx.asOfMillis)
        val docRefs = buildDocumentRefs(
            ctx = ctx,
            scopeKey = "shp-$shipmentId",
            scopeRefNumber = shipment.shipmentNumber,
            scopeTitleEn = shipment.shipmentNumber,
            scopeTitleBn = shipment.shipmentNumber,
            pos = linkedPoIds.mapNotNull { ctx.poMap[it] },
            shipments = listOf(shipment),
            cartons = cartons,
            importCosts = importCosts,
            additionalCosts = additionalCosts,
            snapshots = landedSnapshots,
            receipts = receipts,
            saleSources = saleSources,
            returnSources = returnSources,
            supplierPayments = dataset.supplierPayments.filter { it.purchaseOrderId in linkedPoIds },
            directExpenses = directExpenses,
            attachments = attachments
        )
        val timeline = buildTimelineEvents(
            ctx = ctx,
            pos = linkedPoIds.mapNotNull { ctx.poMap[it] },
            shipments = listOf(shipment),
            snapshots = landedSnapshots,
            receipts = receipts,
            saleSources = saleSources,
            returnSources = returnSources,
            supplierPayments = dataset.supplierPayments.filter { it.purchaseOrderId in linkedPoIds },
            directExpenses = directExpenses,
            costAdjustments = costAdjustments
        )

        return ShipmentVaultDetail(
            summary = summary,
            purchaseOrders = linkedPos,
            products = products,
            importCosts = importCosts,
            landedCostAllocations = landedAllocations,
            landedCostSnapshots = landedSnapshots,
            warehouseReceipts = receipts,
            costAdjustments = costAdjustments,
            directExpenseVouchers = directExpenses,
            sharedExpenseAllocations = expCtx.allComputedAllocations.filter {
                it.targetScopeType == VaultScopeType.SHIPMENT.name && it.targetId == shipmentId
            },
            vaultEntry = vaultEntry,
            attachments = attachments,
            poAllocations = poAllocations,
            cartons = cartons,
            saleSources = saleSources,
            returnSources = returnSources,
            costLayers = costLayerRows,
            timelineEvents = timeline,
            documentRefs = docRefs,
            supplierPaidMinor = weightedSupplierPaid,
            supplierPayableMinor = weightedSupplierPayable,
            additionalCosts = additionalCosts,
            damageRecords = scopeDamages,
            shortageRecords = allShipmentShortages,
            salesTrend = salesTrend
        )
    }

    fun computePurchaseOrderDetail(
        dataset: ImportVaultDataset,
        purchaseOrderId: String,
        filter: ImportVaultFilter = ImportVaultFilter()
    ): PurchaseOrderVaultDetail? {
        val po = dataset.purchaseOrders.find { it.id == purchaseOrderId } ?: return null
        val ctx = buildContext(dataset, filter)
        val expCtx = resolveExpenses(ctx, filter)
        val summary = computePoSummary(ctx, po, expCtx.allocatedByPo[purchaseOrderId] ?: 0L, expCtx.profitLabel)
        val shipmentIds = ctx.shipmentIdsForPo(purchaseOrderId).filter { !ctx.isBatchShipmentShadowedByDispatchedShipment(it) }.distinct()
        val shipments = dataset.shipments.filter { it.id in shipmentIds }
        val shipmentNetProfitMap = shipments.associate { shp ->
            shp.id to computeShipmentSummary(ctx, shp, expCtx.allocatedByShipment[shp.id] ?: 0L, expCtx.profitLabel).attributedNetProfitMinor
        }
        val poNetProfitMap = mapOf(purchaseOrderId to summary.attributedNetProfitMinor)
        val layerIds = ctx.costLayers.filter { it.purchaseOrderId == purchaseOrderId }.map { it.id }.toSet()

        val scopeLayers = ctx.attributedLayers.filter { it.effectivePoId == purchaseOrderId }
        val scopeTransit = ctx.attributedTransit.filter { it.purchaseOrderId == purchaseOrderId }
        val scopeSales = ctx.filteredAttributedSales(filter.copy(productId = null, variantId = null, supplierId = null, warehouseId = null))
            .filter { it.effectivePoId == purchaseOrderId }
        val scopeDamages = ctx.damageRecords.filter { it.purchaseOrderId == purchaseOrderId && ctx.inActivityWindow(it.createdAt) }
        val scopeShortages = ctx.shortageRecords.filter {
            it.purchaseOrderId == purchaseOrderId && it.disposition == "APPROVED_WRITE_OFF" && ctx.inActivityWindow(it.createdAt)
        }
        val scopeWriteOffs = ctx.manualWriteOffsForScope(shipmentId = null, poId = purchaseOrderId, applyActivityWindow = true)

        val products = buildProductRowsForScope(
            ctx = ctx,
            scopeLayers = scopeLayers,
            scopeTransit = scopeTransit,
            scopeSales = scopeSales,
            scopeDamages = scopeDamages,
            scopeShortages = scopeShortages,
            scopeWriteOffs = scopeWriteOffs,
            parentNetProfitMinor = summary.attributedNetProfitMinor,
            directExpenseTotalMinor = summary.directAttributedExpensesMinor,
            sharedExpenseTotalMinor = summary.allocatedSharedOverheadMinor,
            additionalCogsTotalMinor = ctx.capitalizableAdditionalCogsForPo(purchaseOrderId, applyActivityWindow = true),
            postFinalizeCogsTotalMinor = ctx.postFinalizeCogsForPo(purchaseOrderId, applyActivityWindow = true),
            poNetProfitMap = poNetProfitMap,
            shipmentNetProfitMap = shipmentNetProfitMap,
            fixedParentPoId = purchaseOrderId,
            fixedParentShipmentId = shipmentIds.singleOrNull()
        )

        val supplierPayments = dataset.supplierPayments.filter { it.purchaseOrderId == purchaseOrderId }
        val costAdjustments = dataset.costAdjustments.filter { it.purchaseOrderId == purchaseOrderId || it.costLayerId in layerIds }
        val directExpenses = dataset.expenseVouchers.filter {
            it.isPosted && !it.isReversed && it.purchaseOrderId == purchaseOrderId
        }
        val vaultEntry = dataset.vaultEntries.find { it.scopeType == VaultScopeType.PURCHASE_ORDER.name && it.sourceId == purchaseOrderId }
        val attachments = dataset.vaultAttachments.filter { it.scopeType == VaultScopeType.PURCHASE_ORDER.name && it.sourceId == purchaseOrderId }
        val receipts = dataset.warehouseReceipts.filter {
            (it.purchaseOrderId == purchaseOrderId || it.shipmentId in shipmentIds) && it.receiptDate <= ctx.asOfMillis
        }
        val cartons = dataset.cartons.filter {
            it.purchaseOrderId == purchaseOrderId || (it.shipmentId in shipmentIds && it.purchaseOrderId == null)
        }
        val importCosts = dataset.importCosts.filter { it.purchaseOrderId == purchaseOrderId || it.shipmentId in shipmentIds }
        val snapshots = dataset.landedSnapshots.filter {
            it.purchaseOrderId == purchaseOrderId || (it.shipmentId in shipmentIds && it.purchaseOrderId == null)
        }

        val shipmentAllocations = shipmentIds.mapNotNull { sId ->
            computeShipmentPoAllocationSummary(ctx, sId, purchaseOrderId, expCtx)
        }
        val saleSources = buildSaleSourceRows(ctx, scopeSales)
        val returnSources = buildReturnSourceRows(ctx, scopeSales)
        val costLayerRows = buildCostLayerRows(ctx, scopeLayers)
        val additionalCosts = dataset.additionalProductCosts.filter { it.purchaseOrderId == purchaseOrderId }
        val docRefs = buildDocumentRefs(
            ctx = ctx,
            scopeKey = "po-$purchaseOrderId",
            scopeRefNumber = po.poNumber,
            scopeTitleEn = po.poNumber,
            scopeTitleBn = po.poNumber,
            pos = listOf(po),
            shipments = shipments,
            cartons = cartons,
            importCosts = importCosts,
            additionalCosts = additionalCosts,
            snapshots = snapshots,
            receipts = receipts,
            saleSources = saleSources,
            returnSources = returnSources,
            supplierPayments = supplierPayments,
            directExpenses = directExpenses,
            attachments = attachments
        )
        val timeline = buildTimelineEvents(
            ctx = ctx,
            pos = listOf(po),
            shipments = shipments,
            snapshots = snapshots,
            receipts = receipts,
            saleSources = saleSources,
            returnSources = returnSources,
            supplierPayments = supplierPayments,
            directExpenses = directExpenses,
            costAdjustments = costAdjustments
        )

        return PurchaseOrderVaultDetail(
            summary = summary,
            shipments = shipments,
            products = products,
            additionalCosts = additionalCosts,
            supplierPayments = supplierPayments,
            damageRecords = dataset.damageRecords.filter { it.purchaseOrderId == purchaseOrderId },
            shortageRecords = dataset.shortageRecords.filter { it.purchaseOrderId == purchaseOrderId },
            costAdjustments = costAdjustments,
            directExpenseVouchers = directExpenses,
            sharedExpenseAllocations = expCtx.allComputedAllocations.filter {
                it.targetScopeType == VaultScopeType.PURCHASE_ORDER.name && it.targetId == purchaseOrderId
            },
            vaultEntry = vaultEntry,
            attachments = attachments,
            shipmentAllocations = shipmentAllocations,
            warehouseReceipts = receipts,
            cartons = cartons,
            saleSources = saleSources,
            returnSources = returnSources,
            costLayers = costLayerRows,
            timelineEvents = timeline,
            documentRefs = docRefs,
            importCosts = importCosts,
            landedCostSnapshots = snapshots,
            salesTrend = buildSalesTrend(saleSources, filter, ctx.asOfMillis)
        )
    }

    fun computeProductProfitability(
        dataset: ImportVaultDataset,
        filter: ImportVaultFilter = ImportVaultFilter()
    ): List<ProductVaultRow> {
        val ctx = buildContext(dataset, filter)
        val filteredLayers = ctx.filteredAttributedLayers(filter)
        val filteredTransit = ctx.filteredAttributedTransit(filter)
        val filteredSales = ctx.filteredAttributedSales(filter)
        val overview = computeOverview(dataset, filter)
        val poNetProfitMap = overview.purchaseOrders.associate { it.purchaseOrderId to it.attributedNetProfitMinor }
        val shipmentNetProfitMap = overview.shipments.associate { it.shipmentId to it.attributedNetProfitMinor }
        return buildProductRowsForScope(
            ctx = ctx,
            scopeLayers = filteredLayers,
            scopeTransit = filteredTransit,
            scopeSales = filteredSales,
            scopeDamages = emptyList(),
            scopeShortages = emptyList(),
            scopeWriteOffs = ctx.filteredManualWriteOffs(filter),
            parentNetProfitMinor = overview.totalAttributedNetProfitMinor,
            directExpenseTotalMinor = overview.totalDirectAttributedExpensesMinor,
            sharedExpenseTotalMinor = overview.totalAllocatedSharedOverheadMinor,
            additionalCogsTotalMinor = 0L,
            postFinalizeCogsTotalMinor = 0L,
            poNetProfitMap = poNetProfitMap,
            shipmentNetProfitMap = shipmentNetProfitMap
        )
    }

    // -------------------------------------------------------------------------
    // Internal Context & Deterministic Computation Helpers
    // -------------------------------------------------------------------------

    private class Context(
        val dataset: ImportVaultDataset,
        val filter: ImportVaultFilter,
        val asOfMillis: Long
    ) {
        val productMap = dataset.products.associateBy { it.id }
        val variantMap = dataset.variants.associateBy { it.id }
        val warehouseMap = dataset.warehouses.associateBy { it.id }
        val supplierMap = dataset.suppliers.associateBy { it.id }
        private val primaryProductImageByProductId: Map<String, String> = dataset.productImages
            .filter { it.variantId.isNullOrBlank() }
            .groupBy { it.productId }
            .mapValues { (_, list) ->
                list.sortedWith(compareByDescending<com.pocketdev.advance_erp.data.entity.ProductImage> { it.isPrimary }
                    .thenBy { it.sortOrder }
                    .thenBy { it.createdAt })
                    .first().imageUri
            }
        private val primaryVariantImageByVariantId: Map<String, String> = dataset.productImages
            .filter { !it.variantId.isNullOrBlank() }
            .groupBy { it.variantId!! }
            .mapValues { (_, list) ->
                list.sortedWith(compareByDescending<com.pocketdev.advance_erp.data.entity.ProductImage> { it.isPrimary }
                    .thenBy { it.sortOrder }
                    .thenBy { it.createdAt })
                    .first().imageUri
            }

        fun resolveProductImageUri(productId: String): String? =
            primaryProductImageByProductId[productId]
                ?: dataset.productImages.firstOrNull { it.productId == productId }?.imageUri

        fun resolveVariantImageUri(productId: String, variantId: String?): String? =
            variantId?.let { primaryVariantImageByVariantId[it] } ?: resolveProductImageUri(productId)
        val poMap = dataset.purchaseOrders.associateBy { it.id }
        val shipmentMap = dataset.shipments.associateBy { it.id }
        val receiptMap = dataset.warehouseReceipts.associateBy { it.id }
        val returnMap = dataset.salesReturns.associateBy { it.id }
        val layerMap = dataset.costLayers.associateBy { it.id }
        val invoiceMap = dataset.salesInvoices.associateBy { it.id }
        val additionalProductCosts = dataset.additionalProductCosts
        val costAdjustments = dataset.costAdjustments
        val costLayers = dataset.costLayers
        val damageRecords = dataset.damageRecords
        val shortageRecords = dataset.shortageRecords

        fun supplierCashPaidForPo(poId: String): Long {
            val po = poMap[poId] ?: return 0L
            val allRowsForPo = dataset.supplierPayments.filter { it.purchaseOrderId == poId }
            return if (allRowsForPo.isNotEmpty()) {
                allRowsForPo.filter { it.paymentDate <= asOfMillis }.sumOf { it.bdtAmountMinor }
            } else {
                if (po.orderDate <= asOfMillis) po.paidBdtAmountMinor else 0L
            }
        }

        fun freightCashPaidForReference(referenceId: String): Long =
            dataset.activeImportCashJournals
                .filter {
                    it.isPosted && !it.isReversed &&
                        it.referenceType in setOf("PO_FREIGHT_PAYMENT", "FREIGHT_DUTY_PAYMENT") &&
                        it.referenceId == referenceId &&
                        it.entryDate <= asOfMillis
                }
                .sumOf { it.totalDebitMinor }

        fun isCashPaidImportCost(cost: com.pocketdev.advance_erp.data.entity.ImportCostItem): Boolean {
            if (cost.isEstimated || cost.costDate > asOfMillis) return false
            return if (dataset.hasImportCashJournalTracking) {
                dataset.activeImportCashJournals.any {
                    it.isPosted && !it.isReversed &&
                        it.referenceType in setOf("IMPORT_EXPENSE", "POST_FINALIZE_IMPORT_EXPENSE") &&
                        it.referenceId == cost.id &&
                        it.entryDate <= asOfMillis
                }
            } else {
                cost.isPosted || cost.isPaid
            }
        }

        fun isCashPaidCapitalizableAdditionalCost(cost: com.pocketdev.advance_erp.data.entity.AdditionalProductCost): Boolean {
            if (!cost.isCapitalizable || cost.expenseDate > asOfMillis) return false
            return if (dataset.hasImportCashJournalTracking) {
                dataset.activeImportCashJournals.any {
                    it.isPosted && !it.isReversed &&
                        it.referenceType in setOf("ADDITIONAL_COST", "ADDITIONAL_PRODUCT_COST") &&
                        (it.referenceId == cost.costNumber || it.referenceId == cost.id) &&
                        it.entryDate <= asOfMillis
                }
            } else {
                true
            }
        }

        fun paidAdditionalCapForPo(poId: String): Long =
            additionalProductCosts
                .filter { it.purchaseOrderId == poId && isCashPaidCapitalizableAdditionalCost(it) }
                .sumOf { it.bdtAmountMinor }

        fun poOwnCashInvested(poId: String): Long {
            if (poMap[poId] == null) return 0L
            val supplierPaid = supplierCashPaidForPo(poId)
            val freightPaid = freightCashPaidForReference(poId)
            val directPaidImportCosts = dataset.importCosts
                .filter { it.purchaseOrderId == poId && isCashPaidImportCost(it) }
                .sumOf { it.bdtAmountMinor }
            return supplierPaid + freightPaid + directPaidImportCosts + paidAdditionalCapForPo(poId)
        }

        fun shipmentUnlinkedCashPaidCosts(shipmentId: String): Long =
            dataset.importCosts
                .filter { it.shipmentId == shipmentId && it.purchaseOrderId.isNullOrBlank() && isCashPaidImportCost(it) }
                .sumOf { it.bdtAmountMinor } + freightCashPaidForReference(shipmentId)

        private val receiptDateCache: Map<String, Long> = dataset.costLayers.associate { layer ->
            val arrival = when (layer.layerSourceType) {
                "SALES_RETURN_RESTORE" -> returnMap[layer.warehouseReceiptId]?.returnDate ?: layer.receiptDate
                "TRANSFER_REPLICA" -> {
                    val tr = dataset.stockTransfers.find { it.id == layer.warehouseReceiptId }
                    tr?.receivedAt ?: tr?.transferDate ?: layer.receiptDate
                }
                else -> layer.receiptDate
            }
            layer.id to arrival
        }

        private val layerStates: List<LayerAsOfState> = buildLayerAsOfStates()
        val attributedLayers: List<LayerAsOfState> = layerStates.filter { it.effectivePoId != null || it.effectiveShipmentId != null }
        val unattributedLayers: List<LayerAsOfState> = layerStates.filter { it.effectivePoId == null && it.effectiveShipmentId == null }

        val attributedTransit: List<InTransitTransferSlice> = buildInTransitTransfers()
        val attributedSales: List<NetSaleSliceMetrics> = buildNetSaleSlices().filter {
            !it.allocation.isUnattributedFallback && (it.effectivePoId != null || it.effectiveShipmentId != null)
        }
        val unattributedSales: List<NetSaleSliceMetrics> = buildNetSaleSlices().filter {
            it.allocation.isUnattributedFallback || (it.effectivePoId == null && it.effectiveShipmentId == null)
        }

        val poToShipments: Map<String, List<String>>
        val shipmentToPos: Map<String, List<String>>

        init {
            val poShip = mutableMapOf<String, MutableSet<String>>()
            val shipPo = mutableMapOf<String, MutableSet<String>>()
            fun link(poId: String?, shipId: String?) {
                if (!poId.isNullOrBlank() && !shipId.isNullOrBlank() && shipId !in SYNTHETIC_SHIPMENT_IDS && !shipId.startsWith("TR-")) {
                    poShip.getOrPut(poId) { linkedSetOf() }.add(shipId)
                    shipPo.getOrPut(shipId) { linkedSetOf() }.add(poId)
                }
            }
            dataset.shipments.forEach { link(it.purchaseOrderId, it.id) }
            dataset.shipmentSuppliers.forEach { link(it.purchaseOrderId, it.shipmentId) }
            dataset.cartons.forEach { link(it.purchaseOrderId, it.shipmentId) }
            dataset.warehouseReceipts.forEach { link(it.purchaseOrderId, it.shipmentId) }
            dataset.costLayers.forEach { link(it.purchaseOrderId, it.shipmentId) }
            poToShipments = poShip.mapValues { it.value.toList() }
            shipmentToPos = shipPo.mapValues { it.value.toList() }
        }

        fun shipmentIdsForPo(poId: String): List<String> = poToShipments[poId].orEmpty()
        fun poIdsForShipment(shipmentId: String): List<String> = shipmentToPos[shipmentId].orEmpty()

        fun isBatchShipmentShadowedByDispatchedShipment(shipmentId: String): Boolean {
            val shp = shipmentMap[shipmentId] ?: return false
            if (!shp.shipmentNumber.startsWith("BATCH-")) return false
            val poId = shp.purchaseOrderId ?: return false
            val realShipments = shipmentIdsForPo(poId).filter { otherId ->
                otherId != shipmentId && shipmentMap[otherId]?.shipmentNumber?.startsWith("BATCH-") == false
            }
            val hasCartons = dataset.cartons.any { it.shipmentId == shipmentId }
            val hasReceipts = dataset.warehouseReceipts.any { it.shipmentId == shipmentId }
            val hasCosts = dataset.importCosts.any { it.shipmentId == shipmentId }
            return realShipments.isNotEmpty() && !hasCartons && !hasReceipts && !hasCosts
        }

        fun receivedGoodQtyForPo(poId: String): Int =
            layerStates.filter {
                it.effectivePoId == poId &&
                    it.layer.layerSourceType !in setOf("TRANSFER_REPLICA", "SALES_RETURN_RESTORE") &&
                    (receiptDateCache[it.layer.id] ?: it.layer.receiptDate) <= asOfMillis
            }.sumOf { it.layer.initialQuantity }

        fun receivedGoodQtyForShipment(shipmentId: String): Int =
            layerStates.filter {
                it.effectiveShipmentId == shipmentId &&
                    it.layer.layerSourceType !in setOf("TRANSFER_REPLICA", "SALES_RETURN_RESTORE") &&
                    (receiptDateCache[it.layer.id] ?: it.layer.receiptDate) <= asOfMillis
            }.sumOf { it.layer.initialQuantity }

        val isActivityDateFiltered: Boolean =
            filter.dateFilterTarget == VaultDateFilterTarget.ACTIVITY_DATES &&
                (filter.dateFromMillis != null || filter.dateToMillis != null)

        val isCohortDateFiltered: Boolean =
            filter.dateFilterTarget == VaultDateFilterTarget.IMPORT_COHORT_DATES &&
                (filter.dateFromMillis != null || filter.dateToMillis != null)

        fun effectivePeriodEnd(): Long =
            filter.dateToMillis?.let { minOf(it, asOfMillis) } ?: asOfMillis

        fun inActivityWindow(ts: Long, applyPeriodFilter: Boolean = true): Boolean {
            if (ts > asOfMillis) return false
            if (!applyPeriodFilter || filter.dateFilterTarget != VaultDateFilterTarget.ACTIVITY_DATES) return true
            val fromOk = filter.dateFromMillis == null || ts >= filter.dateFromMillis
            val toOk = filter.dateToMillis == null || ts <= effectivePeriodEnd()
            return fromOk && toOk
        }

        fun inCohortWindow(ts: Long): Boolean {
            if (ts > asOfMillis) return false
            if (filter.dateFilterTarget != VaultDateFilterTarget.IMPORT_COHORT_DATES) return true
            val fromOk = filter.dateFromMillis == null || ts >= filter.dateFromMillis
            val toOk = filter.dateToMillis == null || ts <= effectivePeriodEnd()
            return fromOk && toOk
        }

        fun capitalizableAdditionalCogsForPo(poId: String, applyActivityWindow: Boolean = true): Long =
            additionalProductCosts.filter {
                it.purchaseOrderId == poId && it.isCapitalizable && inActivityWindow(it.expenseDate, applyActivityWindow)
            }.sumOf { it.allocatedToCogsMinor }

        fun capitalizableAdditionalCogsForShipment(shipmentId: String, applyActivityWindow: Boolean = true): Long {
            val linkedPos = poIdsForShipment(shipmentId)
            return linkedPos.sumOf { poId ->
                val shareWeight = shipmentShareOfPo(poId, shipmentId)
                val totalPoCap = capitalizableAdditionalCogsForPo(poId, applyActivityWindow)
                if (shareWeight == BigDecimal.ONE) totalPoCap else {
                    BigDecimal(totalPoCap).multiply(shareWeight).setScale(0, RoundingMode.HALF_UP).longValueExact()
                }
            }
        }

        fun postFinalizeCogsForPo(poId: String, applyActivityWindow: Boolean = true): Long =
            costAdjustments.filter {
                it.purchaseOrderId == poId && it.referenceType == "POST_FINALIZE_IMPORT_EXPENSE" &&
                    inActivityWindow(it.createdAt, applyActivityWindow)
            }.sumOf { it.cogsAdjustmentAmountMinor }

        fun postFinalizeCogsForShipment(shipmentId: String, applyActivityWindow: Boolean = true): Long =
            costAdjustments.filter {
                it.referenceId == shipmentId && it.referenceType == "POST_FINALIZE_IMPORT_EXPENSE" &&
                    inActivityWindow(it.createdAt, applyActivityWindow)
            }.sumOf { it.cogsAdjustmentAmountMinor }

        fun shipmentShareOfPo(poId: String, shipmentId: String): BigDecimal {
            val shipments = shipmentIdsForPo(poId).filter { !isBatchShipmentShadowedByDispatchedShipment(it) }
            if (shipments.size <= 1) return if (shipmentId in shipments) BigDecimal.ONE else BigDecimal.ZERO
            val values = shipments.associateWith { sId ->
                val shipLayerCost = layerStates.filter {
                    it.effectivePoId == poId && it.effectiveShipmentId == sId &&
                        it.layer.layerSourceType !in setOf("TRANSFER_REPLICA", "SALES_RETURN_RESTORE")
                }.sumOf { it.layer.initialQuantity.toLong() * it.layer.unitLandedCostBdtMinor }
                val snapCost = dataset.landedSnapshots.filter {
                    it.shipmentId == sId && (it.purchaseOrderId == null || it.purchaseOrderId == poId)
                }.sumOf { it.totalLandedCostBdtMinor }
                BigDecimal(maxOf(shipLayerCost, snapCost, 1L))
            }
            val total = values.values.fold(BigDecimal.ZERO) { a, b -> a.add(b) }
            val mine = values[shipmentId] ?: return BigDecimal.ZERO
            return if (total > BigDecimal.ZERO) mine.divide(total, 8, RoundingMode.HALF_UP) else BigDecimal.ZERO
        }

        fun damageRecordsForShipment(
            shipmentId: String,
            applyActivityWindow: Boolean = true
        ): List<com.pocketdev.advance_erp.data.entity.DamageRecord> {
            val receiptIds = dataset.warehouseReceipts.filter { it.shipmentId == shipmentId }.map { it.id }.toSet()
            return damageRecords.filter { it.warehouseReceiptId in receiptIds && inActivityWindow(it.createdAt, applyActivityWindow) }
        }

        fun shortageRecordsForShipment(
            shipmentId: String,
            applyActivityWindow: Boolean = true
        ): List<com.pocketdev.advance_erp.data.entity.ShortageRecord> {
            val receiptIds = dataset.warehouseReceipts.filter { it.shipmentId == shipmentId }.map { it.id }.toSet()
            return shortageRecords.filter {
                it.warehouseReceiptId in receiptIds && it.disposition == "APPROVED_WRITE_OFF" &&
                    inActivityWindow(it.createdAt, applyActivityWindow)
            }
        }

        fun manualWriteOffsForScope(
            shipmentId: String?,
            poId: String?,
            applyActivityWindow: Boolean = true
        ): List<com.pocketdev.advance_erp.data.entity.FifoConsumption> {
            return dataset.fifoConsumptions.filter { fc ->
                fc.referenceType in setOf("DAMAGE_WRITE_OFF", "ADJUSTMENT") &&
                    inActivityWindow(fc.createdAt, applyActivityWindow) && run {
                        val l = layerMap[fc.costLayerId]
                        val matchesShip = shipmentId == null || resolveEffectiveShipmentId(l) == shipmentId
                        val matchesPo = poId == null || l?.purchaseOrderId == poId
                        matchesShip && matchesPo
                    }
            }
        }

        fun attributedDeliveryExpenseForInvoice(invoiceId: String): Long {
            val totalDelivery = dataset.expenseVouchers.filter {
                it.isPosted && !it.isReversed && it.salesInvoiceId == invoiceId && it.voucherDate <= asOfMillis
            }.sumOf { it.bdtAmountMinor }
            if (totalDelivery == 0L) return 0L
            val allAllocs = dataset.saleLayerAllocations.filter { !it.isReversed && it.invoiceId == invoiceId }
            val totalRev = allAllocs.sumOf { it.allocatedNetRevenueMinor }
            if (totalRev <= 0L) return 0L
            val attrRev = allAllocs.filter { !it.isUnattributedFallback && (it.purchaseOrderId != null || it.shipmentId != null) }
                .sumOf { it.allocatedNetRevenueMinor }
            return BigDecimal(totalDelivery).multiply(BigDecimal(attrRev))
                .divide(BigDecimal(totalRev), 0, RoundingMode.HALF_UP)
                .longValueExact()
        }

        fun filteredPos(f: ImportVaultFilter): List<PurchaseOrder> =
            dataset.purchaseOrders.filter { po ->
                po.status != PurchaseOrderStatus.CANCELLED.name &&
                    po.orderDate <= asOfMillis &&
                    (f.supplierId == null || po.supplierId == f.supplierId) &&
                    (f.dateFilterTarget != VaultDateFilterTarget.IMPORT_COHORT_DATES || inCohortWindow(po.orderDate))
            }

        private fun cohortShipmentIds(f: ImportVaultFilter): Set<String> =
            dataset.shipments.filter { shp ->
                shp.status != ShipmentStatus.CANCELLED.name &&
                    shp.shippingDate <= asOfMillis &&
                    (f.supplierId == null || shp.supplierId == f.supplierId) &&
                    (f.dateFilterTarget != VaultDateFilterTarget.IMPORT_COHORT_DATES || inCohortWindow(shp.shippingDate))
            }.map { it.id }.toSet()

        private fun matchesCohortScope(
            f: ImportVaultFilter,
            effectivePoId: String?,
            effectiveShipmentId: String?,
            activePoIds: Set<String>,
            activeShipIds: Set<String>
        ): Boolean {
            if (f.dateFilterTarget != VaultDateFilterTarget.IMPORT_COHORT_DATES ||
                (f.dateFromMillis == null && f.dateToMillis == null)
            ) return true
            return (effectivePoId != null && effectivePoId in activePoIds) ||
                (effectiveShipmentId != null && effectiveShipmentId in activeShipIds)
        }

        fun filteredAttributedLayers(f: ImportVaultFilter): List<LayerAsOfState> {
            val activePoIds = filteredPos(f).map { it.id }.toSet()
            val activeShipIds = cohortShipmentIds(f)
            return attributedLayers.filter { st ->
                (f.warehouseId == null || st.layer.warehouseId == f.warehouseId) &&
                    (f.productId == null || st.layer.productId == f.productId) &&
                    (f.variantId == null || st.layer.variantId == f.variantId) &&
                    (f.supplierId == null || poMap[st.effectivePoId]?.supplierId == f.supplierId || shipmentMap[st.effectiveShipmentId]?.supplierId == f.supplierId) &&
                    matchesCohortScope(f, st.effectivePoId, st.effectiveShipmentId, activePoIds, activeShipIds)
            }
        }

        fun filteredAttributedTransit(f: ImportVaultFilter): List<InTransitTransferSlice> {
            val activePoIds = filteredPos(f).map { it.id }.toSet()
            val activeShipIds = cohortShipmentIds(f)
            return attributedTransit.filter { tr ->
                (f.warehouseId == null || tr.destinationWarehouseId == f.warehouseId) &&
                    (f.productId == null || tr.productId == f.productId) &&
                    (f.variantId == null || tr.variantId == f.variantId) &&
                    (f.supplierId == null || poMap[tr.purchaseOrderId]?.supplierId == f.supplierId || shipmentMap[tr.shipmentId]?.supplierId == f.supplierId) &&
                    matchesCohortScope(f, tr.purchaseOrderId, tr.shipmentId, activePoIds, activeShipIds)
            }
        }

        fun filteredAttributedSales(
            f: ImportVaultFilter,
            applyActivityWindow: Boolean = true
        ): List<NetSaleSliceMetrics> {
            val activePoIds = filteredPos(f).map { it.id }.toSet()
            val activeShipIds = cohortShipmentIds(f)
            return attributedSales.filter { s ->
                (f.warehouseId == null || s.allocation.warehouseId == f.warehouseId) &&
                    (f.productId == null || s.allocation.productId == f.productId) &&
                    (f.variantId == null || s.allocation.variantId == f.variantId) &&
                    (f.supplierId == null || poMap[s.effectivePoId]?.supplierId == f.supplierId || shipmentMap[s.effectiveShipmentId]?.supplierId == f.supplierId) &&
                    matchesCohortScope(f, s.effectivePoId, s.effectiveShipmentId, activePoIds, activeShipIds) &&
                    inActivityWindow(s.eventDate, applyPeriodFilter = applyActivityWindow)
            }
        }

        fun filteredManualWriteOffs(
            f: ImportVaultFilter,
            applyActivityWindow: Boolean = true
        ): List<com.pocketdev.advance_erp.data.entity.FifoConsumption> =
            dataset.fifoConsumptions.filter { fc ->
                fc.referenceType in setOf("DAMAGE_WRITE_OFF", "ADJUSTMENT") &&
                    inActivityWindow(fc.createdAt, applyPeriodFilter = applyActivityWindow) &&
                    (f.warehouseId == null || fc.warehouseId == f.warehouseId) &&
                    (f.productId == null || fc.productId == f.productId) &&
                    (f.variantId == null || fc.variantId == f.variantId) &&
                    run {
                        val l = layerMap[fc.costLayerId]
                        (l?.purchaseOrderId != null || resolveEffectiveShipmentId(l) != null) &&
                            (f.supplierId == null || poMap[l?.purchaseOrderId]?.supplierId == f.supplierId)
                    }
            }

        fun orphanShipmentExtraCapital(f: ImportVaultFilter): Long {
            if (f.productId != null || f.variantId != null) return 0L
            return dataset.shipments.filter { shp ->
                shp.status != ShipmentStatus.CANCELLED.name &&
                    shp.shippingDate <= asOfMillis &&
                    poIdsForShipment(shp.id).isEmpty() &&
                    (f.supplierId == null || shp.supplierId == f.supplierId) &&
                    (f.dateFilterTarget != VaultDateFilterTarget.IMPORT_COHORT_DATES || inCohortWindow(shp.shippingDate))
            }.sumOf { shp ->
                shipmentUnlinkedCashPaidCosts(shp.id)
            }
        }

        fun orphanShipmentDirectExpenses(f: ImportVaultFilter, applyActivityWindow: Boolean = true): Long {
            return dataset.expenseVouchers.filter { ev ->
                ev.isPosted && !ev.isReversed && inActivityWindow(ev.voucherDate, applyActivityWindow) &&
                    ev.purchaseOrderId == null && ev.shipmentId != null &&
                    poIdsForShipment(ev.shipmentId).isEmpty() &&
                    (f.supplierId == null || shipmentMap[ev.shipmentId]?.supplierId == f.supplierId)
            }.sumOf { it.bdtAmountMinor }
        }

        private fun resolveEffectiveShipmentId(layer: InventoryCostLayer?): String? {
            if (layer == null) return null
            if (layer.layerSourceType in SYNTHETIC_LAYER_SOURCES) return null
            return layer.shipmentId.takeIf { it !in SYNTHETIC_SHIPMENT_IDS && !it.startsWith("TR-") }
        }

        private fun buildLayerAsOfStates(): List<LayerAsOfState> {
            val activeSaleAllocs = dataset.saleLayerAllocations.filter { alloc ->
                !alloc.isReversed && (alloc.invoiceId == null || invoiceMap[alloc.invoiceId]?.let {
                    it.isPosted && it.status != InvoiceStatus.CANCELLED.name
                } == true)
            }
            val transferMap = dataset.stockTransfers.associateBy { it.id }

            // Group future deductions (> asOfMillis) per layerId to reconstruct exact as-of remaining quantity
            val futureSalesByLayer = mutableMapOf<String, Int>()
            val coveredFifoByInvoiceItemAndLayer = mutableSetOf<Triple<String?, String?, String>>()
            for (alloc in activeSaleAllocs) {
                val cId = alloc.costLayerId ?: continue
                val saleDate = alloc.invoiceId?.let { invoiceMap[it]?.invoiceDate } ?: alloc.createdAt
                coveredFifoByInvoiceItemAndLayer.add(Triple(alloc.invoiceId, alloc.invoiceItemId, cId))
                if (saleDate > asOfMillis) {
                    futureSalesByLayer[cId] = (futureSalesByLayer[cId] ?: 0) + alloc.quantity
                }
            }
            val futureFifoOtherByLayer = mutableMapOf<String, Int>()
            for (fc in dataset.fifoConsumptions) {
                val fcDate = if (fc.referenceType == "SALE" && fc.invoiceId != null) {
                    invoiceMap[fc.invoiceId]?.invoiceDate ?: fc.createdAt
                } else fc.createdAt
                if (fc.referenceType == "SALE") {
                    if (Triple(fc.invoiceId, fc.invoiceItemId, fc.costLayerId) !in coveredFifoByInvoiceItemAndLayer) {
                        val inv = fc.invoiceId?.let { invoiceMap[it] }
                        val invValid = inv == null || (inv.isPosted && inv.status != InvoiceStatus.CANCELLED.name)
                        if (invValid && fcDate > asOfMillis) {
                            futureSalesByLayer[fc.costLayerId] = (futureSalesByLayer[fc.costLayerId] ?: 0) + fc.quantity
                        }
                    }
                } else if (fc.createdAt > asOfMillis) {
                    futureFifoOtherByLayer[fc.costLayerId] = (futureFifoOtherByLayer[fc.costLayerId] ?: 0) + fc.quantity
                }
            }
            val futureTransferOutByLayer = mutableMapOf<String, Int>()
            for (tl in dataset.stockTransferCostLayers) {
                val tr = transferMap[tl.transferId] ?: continue
                if (tr.status == StockTransferStatus.CANCELLED.name) continue
                val dispatchTime = tr.dispatchedAt ?: tr.transferDate
                if (dispatchTime > asOfMillis) {
                    futureTransferOutByLayer[tl.costLayerId] = (futureTransferOutByLayer[tl.costLayerId] ?: 0) + tl.quantity
                }
            }

            return dataset.costLayers.map { layer ->
                val arrivedAt = receiptDateCache[layer.id] ?: layer.receiptDate
                val remQty = if (arrivedAt > asOfMillis) {
                    0
                } else {
                    val futureReAdd = (futureSalesByLayer[layer.id] ?: 0) +
                        (futureFifoOtherByLayer[layer.id] ?: 0) +
                        (futureTransferOutByLayer[layer.id] ?: 0)
                    minOf(layer.initialQuantity, maxOf(0, layer.remainingQuantity) + futureReAdd)
                }
                val isSynthetic = layer.layerSourceType in SYNTHETIC_LAYER_SOURCES ||
                    layer.shipmentId in setOf("ADJ", "RESTOCK", "INIT") ||
                    (layer.shipmentId == "RET" && layer.purchaseOrderId == null)
                val effPo = if (isSynthetic) null else layer.purchaseOrderId
                val effShip = if (isSynthetic) null else resolveEffectiveShipmentId(layer)
                LayerAsOfState(
                    layer = layer,
                    asOfRemainingQty = remQty,
                    effectivePoId = effPo,
                    effectiveShipmentId = effShip
                )
            }
        }

        private fun buildInTransitTransfers(): List<InTransitTransferSlice> {
            val result = mutableListOf<InTransitTransferSlice>()
            val transferMap = dataset.stockTransfers.associateBy { it.id }
            for (tl in dataset.stockTransferCostLayers) {
                val tr = transferMap[tl.transferId] ?: continue
                if (tr.status == StockTransferStatus.CANCELLED.name || tr.status == StockTransferStatus.DRAFT.name) continue
                val dispatchedAt = tr.dispatchedAt ?: tr.transferDate
                val receivedAt = tr.receivedAt
                val inTransitAtAsOf = dispatchedAt <= asOfMillis && (receivedAt == null || receivedAt > asOfMillis)
                if (!inTransitAtAsOf) continue

                val orig = layerMap[tl.costLayerId]
                val isSynthetic = orig == null || orig.layerSourceType in SYNTHETIC_LAYER_SOURCES
                val effPo = if (isSynthetic) null else orig?.purchaseOrderId
                val effShip = if (isSynthetic) null else resolveEffectiveShipmentId(orig)
                if (effPo == null && effShip == null) continue

                result.add(
                    InTransitTransferSlice(
                        transferId = tr.id,
                        destinationWarehouseId = tr.destinationWarehouseId,
                        productId = tr.productId,
                        variantId = tr.variantId,
                        purchaseOrderId = effPo,
                        shipmentId = effShip,
                        quantity = tl.quantity,
                        unitCostMinor = tl.unitCostMinor,
                        originReceiptDate = orig?.originReceiptDate ?: orig?.receiptDate ?: tr.transferDate
                    )
                )
            }
            return result
        }

        private fun buildNetSaleSlices(): List<NetSaleSliceMetrics> {
            val activeReturnAllocsBySaleAllocId = dataset.salesReturnLayerAllocations.filter { rAlloc ->
                val ret = returnMap[rAlloc.returnId]
                val date = ret?.returnDate ?: rAlloc.createdAt
                (ret == null || ret.isPosted) && date <= asOfMillis
            }.groupBy { it.saleLayerAllocationId }

            // Payment allocations (posted & non-voided <= asOf) per invoice
            val validPayments = dataset.customerPayments.associateBy { it.id }
            val collectedAllocByInvoice = dataset.paymentAllocations.filter { pAlloc ->
                val p = validPayments[pAlloc.paymentId]
                p != null && p.isPosted && !p.isVoided && p.paymentDate <= asOfMillis && pAlloc.allocatedAt <= asOfMillis
            }.groupBy { it.invoiceId }.mapValues { (_, list) -> list.sumOf { it.amountMinor } }

            val cashRefundByInvoice = dataset.salesReturns.filter { ret ->
                ret.isPosted && ret.returnDate <= asOfMillis &&
                    ret.refundOption in setOf(ReturnRefundOption.CASH_REFUND.name, ReturnRefundOption.BANK_REFUND.name)
            }.groupBy { it.invoiceId }.mapValues { (_, list) -> list.sumOf { it.totalReturnAmountMinor } }

            val validSaleAllocs = dataset.saleLayerAllocations.filter { alloc ->
                if (alloc.isReversed) return@filter false
                val inv = alloc.invoiceId?.let { invoiceMap[it] }
                if (inv != null) {
                    inv.isPosted && inv.status != InvoiceStatus.CANCELLED.name && inv.invoiceDate <= asOfMillis
                } else {
                    alloc.createdAt <= asOfMillis
                }
            }

            // Group valid allocations by invoiceId so cash/receivables allocate proportionally without rounding drift
            val allocsByInvoiceId = validSaleAllocs.groupBy { it.invoiceId }
            val result = mutableListOf<NetSaleSliceMetrics>()

            for ((invId, allocList) in allocsByInvoiceId) {
                val inv = invId?.let { invoiceMap[it] }
                val rawSlices = allocList.map { alloc ->
                    val retAllocs = activeReturnAllocsBySaleAllocId[alloc.id].orEmpty()
                    val retGood = retAllocs.sumOf { it.soundQuantity }
                    val retDmg = retAllocs.sumOf { it.damagedQuantity }
                    val netQty = maxOf(0, alloc.quantity - retGood - retDmg)
                    val netRev = maxOf(0L, alloc.allocatedNetRevenueMinor - retAllocs.sumOf { it.reversedNetRevenueMinor })
                    val netCogs = maxOf(0L, alloc.totalCogsBdtMinor - retAllocs.sumOf { it.reversedCogsMinor + it.damageLossMinor })
                    val dmgLoss = retAllocs.sumOf { it.damageLossMinor }
                    Triple(alloc, listOf(netQty, retGood, retDmg), listOf(netRev, netCogs, dmgLoss))
                }

                val collectedMap: Map<String, Long>
                val receivableMap: Map<String, Long>
                if (inv != null) {
                    val grossAllocCash = maxOf(0L, (collectedAllocByInvoice[inv.id] ?: 0L) - (cashRefundByInvoice[inv.id] ?: 0L))
                    val creditReturns = dataset.salesReturns.filter {
                        it.isPosted && it.invoiceId == inv.id && it.returnDate <= asOfMillis &&
                            it.refundOption == ReturnRefundOption.CUSTOMER_CREDIT.name
                    }.sumOf { it.totalReturnAmountMinor }
                    val invTotalNetRev = rawSlices.sumOf { it.third[0] }
                    val nonMerchTax = maxOf(0L, inv.taxMinor)
                    val totalCollectibleDenom = invTotalNetRev + nonMerchTax
                    val merchCashTarget = if (totalCollectibleDenom > 0L) {
                        val merchPortion = DeterministicAllocation.allocateProportionally(
                            grossAllocCash,
                            listOf("MERCH" to BigDecimal(invTotalNetRev), "TAX" to BigDecimal(nonMerchTax))
                        )["MERCH"] ?: 0L
                        merchPortion
                    } else 0L

                    val cappedCash = minOf(merchCashTarget, invTotalNetRev)
                    val weights = rawSlices.map { (a, _, num) -> a.id to BigDecimal(num[0]) }
                    collectedMap = if (invTotalNetRev > 0L) {
                        DeterministicAllocation.allocateProportionally(cappedCash, weights)
                    } else {
                        allocList.associate { it.id to 0L }
                    }
                    // Invoice receivable = netRev - cashCollected (respecting customer credit already subtracted in netRev)
                    receivableMap = rawSlices.associate { (a, _, num) ->
                        val rev = num[0]
                        val paid = collectedMap[a.id] ?: 0L
                        val outstandingRaw = maxOf(0L, rev - paid)
                        // Also cap if total invoice due as of today is 0 due to settlement/credit
                        val finalRec = if (invTotalNetRev > 0L && creditReturns >= inv.totalAmountMinor) 0L else outstandingRaw
                        a.id to finalRec
                    }
                } else {
                    collectedMap = allocList.associate { it.id to it.allocatedNetRevenueMinor }
                    receivableMap = allocList.associate { it.id to 0L }
                }

                for ((alloc, qtys, nums) in rawSlices) {
                    val layer = alloc.costLayerId?.let { layerMap[it] }
                    val effPo = alloc.purchaseOrderId ?: layer?.purchaseOrderId
                    val effShip = alloc.shipmentId ?: resolveEffectiveShipmentId(layer)
                    val eventDate = inv?.invoiceDate ?: alloc.createdAt
                    result.add(
                        NetSaleSliceMetrics(
                            allocation = alloc,
                            eventDate = eventDate,
                            netSoldQty = qtys[0],
                            returnedGoodQty = qtys[1],
                            returnedDamagedQty = qtys[2],
                            netRevenueMinor = nums[0],
                            netCogsMinor = nums[1],
                            damageLossMinor = nums[2],
                            collectedCashMinor = collectedMap[alloc.id] ?: 0L,
                            receivableMinor = receivableMap[alloc.id] ?: 0L,
                            effectivePoId = effPo,
                            effectiveShipmentId = effShip
                        )
                    )
                }
            }
            return result
        }
    }

    private fun buildContext(dataset: ImportVaultDataset, filter: ImportVaultFilter): Context {
        val asOf = filter.asOfMillis ?: filter.dateToMillis ?: filter.nowMillis
        return Context(dataset = dataset, filter = filter, asOfMillis = asOf)
    }

    private fun resolveExpenses(ctx: Context, filter: ImportVaultFilter): ResolvedExpenseContext {
        val selectedRule: ExpenseAllocationRule? = if (filter.allocationRuleId != null) {
            ctx.dataset.expenseAllocationRules.find { it.id == filter.allocationRuleId }
        } else {
            ctx.dataset.expenseAllocationRules.firstOrNull { it.isActive && it.allocationBasis != "UNALLOCATED" }
        }

        val sharedVouchers = ctx.dataset.expenseVouchers.filter { ev ->
            ev.isPosted && !ev.isReversed &&
                ctx.inActivityWindow(ev.voucherDate) &&
                ev.purchaseOrderId == null && ev.shipmentId == null && ev.salesInvoiceId == null
        }
        val sharedPoolMinor = sharedVouchers.sumOf { it.bdtAmountMinor }

        if (selectedRule == null || selectedRule.allocationBasis == "UNALLOCATED") {
            return ResolvedExpenseContext(
                mode = ExpenseAllocationMode.UNALLOCATED,
                ruleId = null,
                profitLabel = LABEL_ATTRIBUTED_NET_PROFIT,
                sharedOverheadPoolMinor = sharedPoolMinor,
                allocatedByShipment = emptyMap(),
                allocatedByPo = emptyMap(),
                allComputedAllocations = emptyList()
            )
        }

        val persisted = ctx.dataset.expenseAllocations.filter { it.ruleId == selectedRule.id }
        val effectiveAllocs = if (persisted.isNotEmpty()) {
            persisted
        } else {
            previewExpenseRuleAllocations(ctx.dataset, selectedRule, ctx.asOfMillis)
        }
        val byShipment = effectiveAllocs.filter { it.targetScopeType == VaultScopeType.SHIPMENT.name }
            .groupBy { it.targetId }
            .mapValues { (_, list) -> list.sumOf { it.allocatedAmountBdtMinor } }
        val byPo = effectiveAllocs.filter { it.targetScopeType == VaultScopeType.PURCHASE_ORDER.name }
            .groupBy { it.targetId }
            .mapValues { (_, list) -> list.sumOf { it.allocatedAmountBdtMinor } }

        return ResolvedExpenseContext(
            mode = ExpenseAllocationMode.ALLOCATED,
            ruleId = selectedRule.id,
            profitLabel = LABEL_ALLOCATED_NET_PROFIT,
            sharedOverheadPoolMinor = sharedPoolMinor,
            allocatedByShipment = byShipment,
            allocatedByPo = byPo,
            allComputedAllocations = effectiveAllocs
        )
    }

    private fun computePoInvestedCapital(ctx: Context, poId: String): Long {
        if (ctx.poMap[poId] == null) return 0L
        val ownCash = ctx.poOwnCashInvested(poId)
        val linkedShipIds = ctx.shipmentIdsForPo(poId).filter { !ctx.isBatchShipmentShadowedByDispatchedShipment(it) }
        val sharedShipmentPaidCosts = linkedShipIds.sumOf { shpId ->
            val poIdsOnShip = ctx.poIdsForShipment(shpId)
            val share = if (poIdsOnShip.size <= 1) BigDecimal.ONE else {
                val myOrderBdt = BigDecimal(maxOf(1L, ctx.poMap[poId]?.totalBdtAmountMinor ?: 1L))
                val allOrderBdt = poIdsOnShip.fold(BigDecimal.ZERO) { acc, id ->
                    acc.add(BigDecimal(maxOf(1L, ctx.poMap[id]?.totalBdtAmountMinor ?: 1L)))
                }
                if (allOrderBdt > BigDecimal.ZERO) myOrderBdt.divide(allOrderBdt, 8, RoundingMode.HALF_UP) else BigDecimal.ZERO
            }
            BigDecimal(ctx.shipmentUnlinkedCashPaidCosts(shpId)).multiply(share).setScale(0, RoundingMode.HALF_UP).longValueExact()
        }
        return ownCash + sharedShipmentPaidCosts
    }

    private fun computePoDirectOperatingExpense(
        ctx: Context,
        poId: String,
        applyActivityWindow: Boolean = true
    ): Long {
        val nonCapAdditional = ctx.additionalProductCosts
            .filter { it.purchaseOrderId == poId && !it.isCapitalizable && ctx.inActivityWindow(it.expenseDate, applyActivityWindow) }
            .sumOf { it.bdtAmountMinor }
        val directVouchers = ctx.dataset.expenseVouchers
            .filter { it.isPosted && !it.isReversed && it.purchaseOrderId == poId && ctx.inActivityWindow(it.voucherDate, applyActivityWindow) }
            .sumOf { it.bdtAmountMinor }
        return nonCapAdditional + directVouchers
    }

    private fun computeAllShipmentSummaries(
        ctx: Context,
        allocatedByShipment: Map<String, Long>,
        profitLabel: String
    ): List<ShipmentVaultSummary> {
        return ctx.dataset.shipments.filter { !ctx.isBatchShipmentShadowedByDispatchedShipment(it.id) }
            .map { shp -> computeShipmentSummary(ctx, shp, allocatedByShipment[shp.id] ?: 0L, profitLabel) }
    }

    private fun computeShipmentSummary(
        ctx: Context,
        shp: com.pocketdev.advance_erp.data.entity.ImportShipment,
        allocatedSharedOverheadMinor: Long,
        profitLabel: String
    ): ShipmentVaultSummary {
        val linkedPoIds = ctx.poIdsForShipment(shp.id)
        val linkedPos = linkedPoIds.mapNotNull { ctx.poMap[it] }
        val snapshots = ctx.dataset.landedSnapshots.filter { it.shipmentId == shp.id }
        val importCosts = ctx.dataset.importCosts.filter { it.shipmentId == shp.id && it.costDate <= ctx.asOfMillis }
        val cartons = ctx.dataset.cartons.filter { it.shipmentId == shp.id }
        val shipSuppliers = ctx.dataset.shipmentSuppliers.filter { it.shipmentId == shp.id }

        val supplierNames = (linkedPos.map { it.supplierId } + shipSuppliers.map { it.supplierId } + listOf(shp.supplierId))
            .distinct()
            .mapNotNull { ctx.supplierMap[it]?.companyName }
            .ifEmpty { listOf(shp.supplierId) }

        val layers = ctx.attributedLayers.filter { it.effectiveShipmentId == shp.id }
        val originalReceiptLayers = layers.filter { it.layer.layerSourceType !in setOf("TRANSFER_REPLICA", "SALES_RETURN_RESTORE") }
        val transit = ctx.attributedTransit.filter { it.shipmentId == shp.id }
        val allShipmentSales = ctx.attributedSales.filter { it.effectiveShipmentId == shp.id }
        val sales = allShipmentSales.filter { ctx.inActivityWindow(it.eventDate) }

        val purchaseBdt = if (snapshots.isNotEmpty()) {
            snapshots.sumOf { it.totalPurchaseCostBdtMinor }
        } else {
            linkedPos.sumOf { po ->
                val share = ctx.shipmentShareOfPo(po.id, shp.id)
                BigDecimal(po.totalBdtAmountMinor).multiply(share).setScale(0, RoundingMode.HALF_UP).longValueExact()
            }
        }
        val chinaBdt = if (snapshots.isNotEmpty()) snapshots.sumOf { it.chinaDeliveryBdtMinor }
        else shipSuppliers.sumOf { it.chinaDeliveryBdtMinor }
        val freightBdt = if (snapshots.isNotEmpty()) snapshots.sumOf { it.internationalFreightBdtMinor }
        else ctx.dataset.freightRates.filter { it.shipmentId == shp.id }.sumOf { it.totalCategoryFreightBdtMinor }
        val bdAndOtherBdt = if (snapshots.isNotEmpty()) {
            snapshots.sumOf { it.bdTransportBdtMinor + it.otherExpensesBdtMinor }
        } else {
            importCosts.sumOf { it.bdtAmountMinor }
        }
        val addCapTotal = linkedPoIds.sumOf { poId ->
            val share = ctx.shipmentShareOfPo(poId, shp.id)
            val totalCap = ctx.additionalProductCosts
                .filter { it.purchaseOrderId == poId && it.isCapitalizable && it.expenseDate <= ctx.asOfMillis }
                .sumOf { it.bdtAmountMinor }
            BigDecimal(totalCap).multiply(share).setScale(0, RoundingMode.HALF_UP).longValueExact()
        }

        val lockedGoodCost = layers.sumOf { it.asOfRemainingQty.toLong() * it.layer.unitLandedCostBdtMinor }
        val lockedTransitCost = transit.sumOf { it.quantity.toLong() * it.unitCostMinor }
        val totalLocked = lockedGoodCost + lockedTransitCost

        val netRev = sales.sumOf { it.netRevenueMinor }
        val cogs = sales.sumOf { it.netCogsMinor } +
            ctx.capitalizableAdditionalCogsForShipment(shp.id) +
            ctx.postFinalizeCogsForShipment(shp.id)
        val gp = netRev - cogs

        val directVouchers = ctx.dataset.expenseVouchers.filter {
            it.isPosted && !it.isReversed && it.shipmentId == shp.id && ctx.inActivityWindow(it.voucherDate)
        }.sumOf { it.bdtAmountMinor }
        val poDirectShared = linkedPoIds.sumOf { poId ->
            val share = ctx.shipmentShareOfPo(poId, shp.id)
            BigDecimal(computePoDirectOperatingExpense(ctx, poId)).multiply(share).setScale(0, RoundingMode.HALF_UP).longValueExact()
        }
        val directExpenses = directVouchers + poDirectShared

        val dmgRecLoss = ctx.damageRecordsForShipment(shp.id).sumOf { it.totalLossMinor }
        val shortageWriteOffLoss = ctx.shortageRecordsForShipment(shp.id).sumOf { it.totalAmountMinor }
        val returnDmgLoss = sales.sumOf { it.damageLossMinor }
        val manualWriteOffs = ctx.manualWriteOffsForScope(shipmentId = shp.id, poId = null).sumOf { it.totalCogsBdtMinor }
        val totalInvLosses = dmgRecLoss + shortageWriteOffLoss + returnDmgLoss + manualWriteOffs

        val lifetimeCogs = allShipmentSales.sumOf { it.netCogsMinor } +
            ctx.capitalizableAdditionalCogsForShipment(shp.id, applyActivityWindow = false) +
            ctx.postFinalizeCogsForShipment(shp.id, applyActivityWindow = false)
        val lifetimeDmgLoss = ctx.damageRecordsForShipment(shp.id, applyActivityWindow = false).sumOf { it.totalLossMinor }
        val lifetimeShortageWo = ctx.shortageRecordsForShipment(shp.id, applyActivityWindow = false).sumOf { it.totalAmountMinor }
        val lifetimeReturnDmg = allShipmentSales.sumOf { it.damageLossMinor }
        val lifetimeManWo = ctx.manualWriteOffsForScope(shipmentId = shp.id, poId = null, applyActivityWindow = false)
            .sumOf { it.totalCogsBdtMinor }
        val lifetimeInvLosses = lifetimeDmgLoss + lifetimeShortageWo + lifetimeReturnDmg + lifetimeManWo

        val poInvestedShare = linkedPoIds.sumOf { poId ->
            val share = ctx.shipmentShareOfPo(poId, shp.id)
            BigDecimal(ctx.poOwnCashInvested(poId)).multiply(share).setScale(0, RoundingMode.HALF_UP).longValueExact()
        }
        val totalInvested = poInvestedShare + ctx.shipmentUnlinkedCashPaidCosts(shp.id)

        val netProfit = gp - directExpenses - allocatedSharedOverheadMinor - totalInvLosses
        val lifetimeDirectExp = ctx.dataset.expenseVouchers.filter {
            it.isPosted && !it.isReversed && it.shipmentId == shp.id && it.voucherDate <= ctx.asOfMillis
        }.sumOf { it.bdtAmountMinor } + linkedPoIds.sumOf { poId ->
            val share = ctx.shipmentShareOfPo(poId, shp.id)
            BigDecimal(computePoDirectOperatingExpense(ctx, poId, applyActivityWindow = false))
                .multiply(share).setScale(0, RoundingMode.HALF_UP).longValueExact()
        }
        val lifetimeNetProfit = if (ctx.isActivityDateFiltered) {
            (allShipmentSales.sumOf { it.netRevenueMinor } - lifetimeCogs) -
                lifetimeDirectExp - allocatedSharedOverheadMinor - lifetimeInvLosses
        } else netProfit

        val receivedQtyByUnit = QuantityByUnit.fromItems(
            originalReceiptLayers.map {
                (ctx.productMap[it.layer.productId]?.unit ?: "PCS") to it.layer.initialQuantity
            }
        )
        val netSoldQtyByUnit = QuantityByUnit.fromItems(
            sales.map { (ctx.productMap[it.allocation.productId]?.unit ?: "PCS") to it.netSoldQty }
        )
        val remainingQtyByUnit = QuantityByUnit.fromItems(
            layers.map { (ctx.productMap[it.layer.productId]?.unit ?: "PCS") to it.asOfRemainingQty } +
                transit.map { (ctx.productMap[it.productId]?.unit ?: "PCS") to it.quantity }
        )

        val totalCashCollected = sales.sumOf { it.collectedCashMinor }
        val totalReceivable = sales.sumOf { it.receivableMinor }
        val cashRecovery = CashRecoverySummary(
            totalInvestedCapitalMinor = totalInvested,
            attributableCashCollectedMinor = totalCashCollected,
            attributableReceivableMinor = totalReceivable,
            unrecoveredCapitalMinor = maxOf(0L, totalInvested - totalCashCollected),
            cashRecoveryRatioPercent = if (totalInvested > 0L) (totalCashCollected.toDouble() / totalInvested.toDouble()) * 100.0 else null,
            breakEvenReached = totalInvested > 0L && totalCashCollected >= totalInvested
        )

        val hasPending = ctx.dataset.warehouseReceiptItems.any { wri ->
            val rec = ctx.receiptMap[wri.receiptId]
            rec?.shipmentId == shp.id && wri.pendingQuantity > 0
        } || originalReceiptLayers.isEmpty()

        val costStatus = if (snapshots.isNotEmpty() && snapshots.all { it.isFinal } && importCosts.none { it.isEstimated } && !hasPending) {
            CostFinalizationStatus.FINALIZED
        } else if (linkedPos.isNotEmpty() && linkedPos.all { it.isActualCostFinalized } && importCosts.none { it.isEstimated }) {
            CostFinalizationStatus.FINALIZED
        } else {
            CostFinalizationStatus.PROVISIONAL
        }

        val opStatus = mapShipmentOperationalStatus(shp.status, hasPending, originalReceiptLayers.isNotEmpty())
        val comStatus = deriveCommercialStatus(
            receivedSingle = receivedQtyByUnit.singleUnitTotal ?: originalReceiptLayers.sumOf { it.layer.initialQuantity },
            remainingSingle = remainingQtyByUnit.singleUnitTotal ?: (layers.sumOf { it.asOfRemainingQty } + transit.sumOf { it.quantity }),
            netSoldSingle = netSoldQtyByUnit.singleUnitTotal ?: sales.sumOf { it.netSoldQty },
            netProfitMinor = netProfit,
            breakEvenReached = cashRecovery.breakEvenReached
        )

        val supplierDeclaredWeights = cartons.mapNotNull { it.supplierDeclaredGrossWeightKg?.toBigDecimalOrNull() }
        val declaredTotalWeight = if (supplierDeclaredWeights.isNotEmpty()) {
            supplierDeclaredWeights.fold(BigDecimal.ZERO) { a, b -> a.add(b) }.setScale(3, RoundingMode.HALF_UP).toPlainString()
        } else null

        val vaultEntry = ctx.dataset.vaultEntries.find {
            it.scopeType == VaultScopeType.SHIPMENT.name && it.sourceId == shp.id
        }
        val productCount = (layers.map { it.layer.productId } +
            cartons.map { it.productId } +
            linkedPoIds.flatMap { id -> ctx.dataset.purchaseOrderItems.filter { it.purchaseOrderId == id }.map { it.productId } }
            ).distinct().size
        val cartonCount = cartons.map { it.cartonNumber.trim().ifBlank { it.id } }.distinct().size.takeIf { it > 0 } ?: shp.totalCartons
        val distinctCurrencies = linkedPos.map { it.currency }.distinct()
        val originCurrency = distinctCurrencies.singleOrNull()
        val originPurchaseMinor = if (originCurrency != null) {
            linkedPos.sumOf { po ->
                val share = ctx.shipmentShareOfPo(po.id, shp.id)
                BigDecimal(po.totalAmountMinor).multiply(share).setScale(0, RoundingMode.HALF_UP).longValueExact()
            }
        } else null

        return ShipmentVaultSummary(
            shipmentId = shp.id,
            shipmentNumber = shp.shipmentNumber,
            shippingMethod = shp.shippingMethod,
            supplierNames = supplierNames,
            purchaseOrderIds = linkedPoIds,
            purchaseOrderNumbers = linkedPos.map { it.poNumber },
            shippingDate = shp.shippingDate,
            arrivalDate = shp.actualArrivalDate ?: shp.estimatedArrivalDate,
            operationalStatus = opStatus,
            commercialStatus = comStatus,
            costFinalizationStatus = costStatus,
            hasPendingReceiving = hasPending,
            archiveStatus = vaultEntry?.archiveStatus ?: "ACTIVE",
            isPinned = vaultEntry?.isPinned ?: false,
            verifiedBillableWeightKg = shp.totalGrossWeightKg,
            supplierDeclaredWeightKg = declaredTotalWeight,
            receivedGoodQtyByUnit = receivedQtyByUnit,
            netSoldQtyByUnit = netSoldQtyByUnit,
            remainingStockQtyByUnit = remainingQtyByUnit,
            totalInvestedCapitalMinor = totalInvested,
            purchaseCostBdtMinor = purchaseBdt,
            chinaDeliveryBdtMinor = chinaBdt,
            internationalFreightBdtMinor = freightBdt,
            bdTransportAndOtherImportMinor = bdAndOtherBdt,
            capitalizedAdditionalCostsMinor = addCapTotal,
            lockedInventoryCapitalMinor = totalLocked,
            netSalesRevenueMinor = netRev,
            cogsMinor = cogs,
            grossProfitMinor = gp,
            grossMarginPercent = marginPercent(gp, netRev),
            directAttributedExpensesMinor = directExpenses,
            allocatedSharedOverheadMinor = allocatedSharedOverheadMinor,
            inventoryLossesMinor = totalInvLosses,
            attributedNetProfitMinor = netProfit,
            profitLabel = profitLabel,
            netMarginPercent = marginPercent(netProfit, netRev),
            roiPercent = roiPercent(netProfit, totalInvested),
            sellThroughPercent = sellThroughPercent(
                netSoldQtyByUnit.singleUnitTotal ?: sales.sumOf { it.netSoldQty },
                receivedQtyByUnit.singleUnitTotal ?: originalReceiptLayers.sumOf { it.layer.initialQuantity }
            ),
            cashRecovery = cashRecovery,
            agingSummary = computeAgingSummary(layers, transit, ctx.asOfMillis),
            expectedRemainingProfit = computeExpectedRemainingProfit(ctx, layers, transit),
            displayTitle = vaultEntry?.displayTitle?.takeIf { it.isNotBlank() } ?: shp.shipmentNumber,
            tags = parseTags(vaultEntry?.tagsJson),
            notes = vaultEntry?.notes,
            productCount = productCount,
            cartonCount = cartonCount,
            originCurrency = originCurrency,
            originPurchaseAmountMinor = originPurchaseMinor,
            lifetimeNetProfitMinor = lifetimeNetProfit,
            lifetimeRoiPercent = roiPercent(lifetimeNetProfit, totalInvested)
        )
    }

    private fun computeAllPoSummaries(
        ctx: Context,
        allocatedByPo: Map<String, Long>,
        profitLabel: String
    ): List<PurchaseOrderVaultSummary> {
        return ctx.dataset.purchaseOrders.map { po ->
            computePoSummary(ctx, po, allocatedByPo[po.id] ?: 0L, profitLabel)
        }
    }

    private fun computePoSummary(
        ctx: Context,
        po: PurchaseOrder,
        allocatedSharedOverheadMinor: Long,
        profitLabel: String
    ): PurchaseOrderVaultSummary {
        val shipmentIds = ctx.shipmentIdsForPo(po.id).filter { !ctx.isBatchShipmentShadowedByDispatchedShipment(it) }
        val shipmentNumbers = shipmentIds.mapNotNull { ctx.shipmentMap[it]?.shipmentNumber }
        val poItems = ctx.dataset.purchaseOrderItems.filter { it.purchaseOrderId == po.id }
        val layers = ctx.attributedLayers.filter { it.effectivePoId == po.id }
        val originalReceiptLayers = layers.filter { it.layer.layerSourceType !in setOf("TRANSFER_REPLICA", "SALES_RETURN_RESTORE") }
        val transit = ctx.attributedTransit.filter { it.purchaseOrderId == po.id }
        val allPoSales = ctx.attributedSales.filter { it.effectivePoId == po.id }
        val sales = allPoSales.filter { ctx.inActivityWindow(it.eventDate) }

        val orderedQtyByUnit = QuantityByUnit.fromItems(
            poItems.map { (ctx.productMap[it.productId]?.unit ?: "PCS") to it.quantity }
        )
        val receivedGoodQtyByUnit = QuantityByUnit.fromItems(
            originalReceiptLayers.map { (ctx.productMap[it.layer.productId]?.unit ?: "PCS") to it.layer.initialQuantity }
        )
        val pendingByUom = orderedQtyByUnit.byUom.map { (uom, ordQty) ->
            val recQty = receivedGoodQtyByUnit.byUom[uom] ?: 0
            uom to maxOf(0, ordQty - recQty)
        }
        val pendingQtyByUnit = QuantityByUnit.fromItems(pendingByUom)

        val netSoldQtyByUnit = QuantityByUnit.fromItems(
            sales.map { (ctx.productMap[it.allocation.productId]?.unit ?: "PCS") to it.netSoldQty }
        )
        val remainingQtyByUnit = QuantityByUnit.fromItems(
            layers.map { (ctx.productMap[it.layer.productId]?.unit ?: "PCS") to it.asOfRemainingQty } +
                transit.map { (ctx.productMap[it.productId]?.unit ?: "PCS") to it.quantity }
        )

        val capAddTotal = ctx.paidAdditionalCapForPo(po.id)
        val supplierPaid = ctx.supplierCashPaidForPo(po.id)
        val totalInvested = computePoInvestedCapital(ctx, po.id)
        val capitalizedImport = maxOf(0L, totalInvested - supplierPaid - capAddTotal)
        val supplierPayable = maxOf(0L, po.totalBdtAmountMinor - supplierPaid)

        val lockedGood = layers.sumOf { it.asOfRemainingQty.toLong() * it.layer.unitLandedCostBdtMinor }
        val lockedTransit = transit.sumOf { it.quantity.toLong() * it.unitCostMinor }
        val totalLocked = lockedGood + lockedTransit

        val netRev = sales.sumOf { it.netRevenueMinor }
        val cogs = sales.sumOf { it.netCogsMinor } + ctx.capitalizableAdditionalCogsForPo(po.id) + ctx.postFinalizeCogsForPo(po.id)
        val gp = netRev - cogs
        val directExpenses = computePoDirectOperatingExpense(ctx, po.id)

        val dmgRec = ctx.damageRecords.filter { it.purchaseOrderId == po.id && ctx.inActivityWindow(it.createdAt) }.sumOf { it.totalLossMinor }
        val shtWriteOff = ctx.shortageRecords.filter {
            it.purchaseOrderId == po.id && it.disposition == "APPROVED_WRITE_OFF" && ctx.inActivityWindow(it.createdAt)
        }.sumOf { it.totalAmountMinor }
        val retDmg = sales.sumOf { it.damageLossMinor }
        val manWriteOff = ctx.manualWriteOffsForScope(shipmentId = null, poId = po.id).sumOf { it.totalCogsBdtMinor }
        val totalLosses = dmgRec + shtWriteOff + retDmg + manWriteOff

        val netProfit = gp - directExpenses - allocatedSharedOverheadMinor - totalLosses
        val lifetimeNetProfit = if (ctx.isActivityDateFiltered) {
            val lCogs = allPoSales.sumOf { it.netCogsMinor } +
                ctx.capitalizableAdditionalCogsForPo(po.id, applyActivityWindow = false) +
                ctx.postFinalizeCogsForPo(po.id, applyActivityWindow = false)
            val lDir = computePoDirectOperatingExpense(ctx, po.id, applyActivityWindow = false)
            val lDmg = ctx.damageRecords.filter { it.purchaseOrderId == po.id && it.createdAt <= ctx.asOfMillis }.sumOf { it.totalLossMinor }
            val lSht = ctx.shortageRecords.filter {
                it.purchaseOrderId == po.id && it.disposition == "APPROVED_WRITE_OFF" && it.createdAt <= ctx.asOfMillis
            }.sumOf { it.totalAmountMinor }
            val lRetDmg = allPoSales.sumOf { it.damageLossMinor }
            val lManWo = ctx.manualWriteOffsForScope(shipmentId = null, poId = po.id, applyActivityWindow = false)
                .sumOf { it.totalCogsBdtMinor }
            (allPoSales.sumOf { it.netRevenueMinor } - lCogs) - lDir - allocatedSharedOverheadMinor - (lDmg + lSht + lRetDmg + lManWo)
        } else netProfit

        val cashCollected = sales.sumOf { it.collectedCashMinor }
        val receivable = sales.sumOf { it.receivableMinor }
        val cashRecovery = CashRecoverySummary(
            totalInvestedCapitalMinor = totalInvested,
            attributableCashCollectedMinor = cashCollected,
            attributableReceivableMinor = receivable,
            unrecoveredCapitalMinor = maxOf(0L, totalInvested - cashCollected),
            cashRecoveryRatioPercent = if (totalInvested > 0L) (cashCollected.toDouble() / totalInvested.toDouble()) * 100.0 else null,
            breakEvenReached = totalInvested > 0L && cashCollected >= totalInvested
        )

        val orderedTotal = poItems.sumOf { it.quantity }
        val receivedTotal = originalReceiptLayers.sumOf { it.layer.initialQuantity }
        val hasPending = receivedTotal < orderedTotal || ctx.dataset.warehouseReceiptItems.any {
            it.purchaseOrderId == po.id && it.pendingQuantity > 0
        }
        val estimatedCostExists = ctx.dataset.importCosts.any { it.purchaseOrderId == po.id && it.isEstimated }
        val costStatus = if (po.isActualCostFinalized && !estimatedCostExists) {
            CostFinalizationStatus.FINALIZED
        } else {
            CostFinalizationStatus.PROVISIONAL
        }

        val opStatus = mapPoOperationalStatus(po.status, hasPending, receivedTotal > 0, shipmentIds.isNotEmpty())
        val comStatus = deriveCommercialStatus(
            receivedSingle = receivedTotal,
            remainingSingle = layers.sumOf { it.asOfRemainingQty } + transit.sumOf { it.quantity },
            netSoldSingle = sales.sumOf { it.netSoldQty },
            netProfitMinor = netProfit,
            breakEvenReached = cashRecovery.breakEvenReached
        )

        val vaultEntry = ctx.dataset.vaultEntries.find {
            it.scopeType == VaultScopeType.PURCHASE_ORDER.name && it.sourceId == po.id
        }
        val poProductCount = poItems.map { it.productId }
            .distinct()
            .ifEmpty { originalReceiptLayers.map { it.layer.productId }.distinct() }
            .size

        return PurchaseOrderVaultSummary(
            purchaseOrderId = po.id,
            poNumber = po.poNumber,
            supplierId = po.supplierId,
            supplierName = ctx.supplierMap[po.supplierId]?.companyName ?: po.supplierId,
            shipmentIds = shipmentIds,
            shipmentNumbers = shipmentNumbers,
            orderDate = po.orderDate,
            operationalStatus = opStatus,
            commercialStatus = comStatus,
            costFinalizationStatus = costStatus,
            hasPendingReceiving = hasPending,
            archiveStatus = vaultEntry?.archiveStatus ?: "ACTIVE",
            isPinned = vaultEntry?.isPinned ?: false,
            orderedQtyByUnit = orderedQtyByUnit,
            receivedGoodQtyByUnit = receivedGoodQtyByUnit,
            pendingReceivingQtyByUnit = pendingQtyByUnit,
            netSoldQtyByUnit = netSoldQtyByUnit,
            remainingStockQtyByUnit = remainingQtyByUnit,
            purchaseCostBdtMinor = po.totalBdtAmountMinor,
            capitalizedImportCostsMinor = capitalizedImport,
            capitalizedAdditionalCostsMinor = capAddTotal,
            totalInvestedCapitalMinor = totalInvested,
            supplierPaidBdtMinor = supplierPaid,
            supplierPayableBdtMinor = supplierPayable,
            lockedInventoryCapitalMinor = totalLocked,
            netSalesRevenueMinor = netRev,
            cogsMinor = cogs,
            grossProfitMinor = gp,
            grossMarginPercent = marginPercent(gp, netRev),
            directAttributedExpensesMinor = directExpenses,
            allocatedSharedOverheadMinor = allocatedSharedOverheadMinor,
            inventoryLossesMinor = totalLosses,
            attributedNetProfitMinor = netProfit,
            profitLabel = profitLabel,
            netMarginPercent = marginPercent(netProfit, netRev),
            roiPercent = roiPercent(netProfit, totalInvested),
            sellThroughPercent = sellThroughPercent(sales.sumOf { it.netSoldQty }, receivedTotal),
            cashRecovery = cashRecovery,
            agingSummary = computeAgingSummary(layers, transit, ctx.asOfMillis),
            expectedRemainingProfit = computeExpectedRemainingProfit(ctx, layers, transit),
            displayTitle = vaultEntry?.displayTitle?.takeIf { it.isNotBlank() } ?: po.poNumber,
            tags = parseTags(vaultEntry?.tagsJson),
            notes = vaultEntry?.notes,
            productCount = poProductCount,
            originalCurrency = po.currency,
            originalTotalAmountMinor = po.totalAmountMinor,
            exchangeRateToBdt = po.exchangeRateToBdt,
            lifetimeNetProfitMinor = lifetimeNetProfit,
            lifetimeRoiPercent = roiPercent(lifetimeNetProfit, totalInvested)
        )
    }

    private fun buildProductRowsForScope(
        ctx: Context,
        scopeLayers: List<LayerAsOfState>,
        scopeTransit: List<InTransitTransferSlice>,
        scopeSales: List<NetSaleSliceMetrics>,
        scopeDamages: List<com.pocketdev.advance_erp.data.entity.DamageRecord>,
        scopeShortages: List<com.pocketdev.advance_erp.data.entity.ShortageRecord>,
        scopeWriteOffs: List<com.pocketdev.advance_erp.data.entity.FifoConsumption>,
        parentNetProfitMinor: Long,
        directExpenseTotalMinor: Long,
        sharedExpenseTotalMinor: Long,
        additionalCogsTotalMinor: Long,
        postFinalizeCogsTotalMinor: Long,
        poNetProfitMap: Map<String, Long> = emptyMap(),
        shipmentNetProfitMap: Map<String, Long> = emptyMap(),
        fixedParentPoId: String? = null,
        fixedParentShipmentId: String? = null
    ): List<ProductVaultRow> {
        val productIds = (
            scopeLayers.map { it.layer.productId } +
                scopeTransit.map { it.productId } +
                scopeSales.map { it.allocation.productId } +
                scopeDamages.map { it.productId }
            ).distinct()
        if (productIds.isEmpty()) return emptyList()

        // Pre-allocate PO/shipment-level direct/shared expenses and additional COGS deterministically across SKUs by net revenue (fallback received value)
        val variantKeys = (
            scopeLayers.map { it.layer.productId to it.layer.variantId } +
                scopeTransit.map { it.productId to it.variantId } +
                scopeSales.map { it.allocation.productId to it.allocation.variantId }
            ).distinct()

        val keyWeights = variantKeys.map { (pId, vId) ->
            val keyStr = "${pId}|${vId.orEmpty()}"
            val rev = scopeSales.filter { it.allocation.productId == pId && it.allocation.variantId == vId }.sumOf { it.netRevenueMinor }
            val fallbackVal = scopeLayers.filter { it.layer.productId == pId && it.layer.variantId == vId }
                .sumOf { it.layer.initialQuantity.toLong() * it.layer.unitLandedCostBdtMinor }
            keyStr to BigDecimal(maxOf(rev, fallbackVal, 1L))
        }
        val directByKey = DeterministicAllocation.allocateProportionally(directExpenseTotalMinor, keyWeights)
        val sharedByKey = DeterministicAllocation.allocateProportionally(sharedExpenseTotalMinor, keyWeights)
        val addCogsWeights = variantKeys.map { (pId, vId) ->
            val keyStr = "${pId}|${vId.orEmpty()}"
            val soldQ = scopeSales.filter { it.allocation.productId == pId && it.allocation.variantId == vId }.sumOf { it.netSoldQty }
            keyStr to BigDecimal(soldQ)
        }
        val addCogsByKey = DeterministicAllocation.allocateProportionally(additionalCogsTotalMinor + postFinalizeCogsTotalMinor, addCogsWeights)

        return productIds.map { prodId ->
            val prod = ctx.productMap[prodId]
            val uom = prod?.unit ?: "PCS"
            val pVariants = variantKeys.filter { it.first == prodId }.map { it.second }.distinct()

            val provisionalVariantRows = pVariants.map { varId ->
                val vKey = "${prodId}|${varId.orEmpty()}"
                val vObj = varId?.let { ctx.variantMap[it] }
                val vLayers = scopeLayers.filter { it.layer.productId == prodId && it.layer.variantId == varId }
                val vOrigLayers = vLayers.filter { it.layer.layerSourceType !in setOf("TRANSFER_REPLICA", "SALES_RETURN_RESTORE") }
                val vTransit = scopeTransit.filter { it.productId == prodId && it.variantId == varId }
                val vSales = scopeSales.filter { it.allocation.productId == prodId && it.allocation.variantId == varId }

                val receivedGoodQty = vOrigLayers.sumOf { it.layer.initialQuantity }
                val netSoldQty = vSales.sumOf { it.netSoldQty }
                val retGoodQty = vSales.sumOf { it.returnedGoodQty }
                val retDmgQty = vSales.sumOf { it.returnedDamagedQty }
                val dmgRec = scopeDamages.filter { it.productId == prodId && it.variantId == varId }
                val shtRec = scopeShortages.filter { it.productId == prodId && it.variantId == varId }
                val woRec = scopeWriteOffs.filter { it.productId == prodId && it.variantId == varId }

                val writeOffQty = dmgRec.sumOf { it.damagedQuantity } + shtRec.sumOf { it.missingQuantity } + woRec.sumOf { it.quantity }
                val remGoodQty = vLayers.sumOf { it.asOfRemainingQty }
                val remTransitQty = vTransit.sumOf { it.quantity }

                val lockedCost = vLayers.sumOf { it.asOfRemainingQty.toLong() * it.layer.unitLandedCostBdtMinor } +
                    vTransit.sumOf { it.quantity.toLong() * it.unitCostMinor }
                val unitCostWeighted = if (vOrigLayers.isNotEmpty() && receivedGoodQty > 0) {
                    vOrigLayers.sumOf { it.layer.initialQuantity.toLong() * it.layer.unitLandedCostBdtMinor } / receivedGoodQty
                } else {
                    vLayers.firstOrNull()?.layer?.unitLandedCostBdtMinor ?: vSales.firstOrNull()?.allocation?.unitCostBdtMinor ?: 0L
                }

                val netRev = vSales.sumOf { it.netRevenueMinor }
                val cogs = vSales.sumOf { it.netCogsMinor } + (addCogsByKey[vKey] ?: 0L)
                val gp = netRev - cogs
                val invLoss = dmgRec.sumOf { it.totalLossMinor } + shtRec.sumOf { it.totalAmountMinor } +
                    woRec.sumOf { it.totalCogsBdtMinor } + vSales.sumOf { it.damageLossMinor }
                val dirExp = directByKey[vKey] ?: 0L
                val shrExp = sharedByKey[vKey] ?: 0L
                val netProfit = gp - dirExp - shrExp - invLoss

                val vLabel = listOfNotNull(vObj?.color, vObj?.size).filter { it.isNotBlank() }.joinToString(" / ")
                    .ifBlank { vObj?.sku ?: prod?.nameEn ?: prodId }
                val singlePoId = fixedParentPoId ?: (vLayers.mapNotNull { it.effectivePoId } + vSales.mapNotNull { it.effectivePoId }).distinct().singleOrNull()
                val singleShipId = fixedParentShipmentId ?: (vLayers.mapNotNull { it.effectiveShipmentId } + vSales.mapNotNull { it.effectiveShipmentId }).distinct().singleOrNull()
                val poShare = singlePoId?.let { poNetProfitMap[it] }?.let { calculateContribution(netProfit, it) }
                val shpShare = singleShipId?.let { shipmentNetProfitMap[it] }?.let { calculateContribution(netProfit, it) }
                val vInvested = maxOf(
                    vOrigLayers.sumOf { it.layer.initialQuantity.toLong() * it.layer.unitLandedCostBdtMinor },
                    cogs + lockedCost + invLoss
                )
                val vEarliestOrigin = (
                    vOrigLayers.map { it.layer.originReceiptDate } +
                        vLayers.map { it.layer.originReceiptDate } +
                        vTransit.map { it.originReceiptDate }
                    ).minOrNull()
                val vActiveDays = if (vEarliestOrigin != null && (receivedGoodQty > 0 || netSoldQty > 0)) {
                    maxOf(1, maxOf(0L, (ctx.asOfMillis - vEarliestOrigin) / DAY_MS).toInt())
                } else 0
                val vVelocity = if (vActiveDays > 0) netSoldQty.toDouble() / vActiveDays.toDouble() else null

                VariantVaultRow(
                    productId = prodId,
                    variantId = varId,
                    sku = vObj?.sku ?: prod?.baseSku ?: prodId,
                    variantLabel = vLabel,
                    uom = uom,
                    purchaseOrderId = singlePoId ?: vLayers.firstOrNull()?.effectivePoId ?: vSales.firstOrNull()?.effectivePoId,
                    shipmentId = singleShipId ?: vLayers.firstOrNull()?.effectiveShipmentId ?: vSales.firstOrNull()?.effectiveShipmentId,
                    receivedGoodQty = receivedGoodQty,
                    netSoldQty = netSoldQty,
                    returnedGoodQty = retGoodQty,
                    returnedDamagedQty = retDmgQty,
                    writeOffLossQty = writeOffQty,
                    remainingGoodQty = remGoodQty,
                    remainingInTransitTransferQty = remTransitQty,
                    unitLandedCostMinor = unitCostWeighted,
                    netSalesRevenueMinor = netRev,
                    cogsMinor = cogs,
                    grossProfitMinor = gp,
                    grossMarginPercent = marginPercent(gp, netRev),
                    inventoryLossMinor = invLoss,
                    directExpensesMinor = dirExp,
                    allocatedSharedOverheadMinor = shrExp,
                    attributedNetProfitMinor = netProfit,
                    netMarginPercent = marginPercent(netProfit, netRev),
                    lockedInventoryCostMinor = lockedCost,
                    sellThroughPercent = sellThroughPercent(netSoldQty, receivedGoodQty),
                    profitContributionToProduct = null,
                    profitContributionToParentScope = calculateContribution(netProfit, parentNetProfitMinor),
                    agingSummary = computeAgingSummary(vLayers, vTransit, ctx.asOfMillis),
                    expectedRemainingProfit = computeExpectedRemainingProfit(ctx, vLayers, vTransit),
                    profitContributionToPo = poShare,
                    profitContributionToShipment = shpShare,
                    primaryImageUri = ctx.resolveVariantImageUri(prodId, varId),
                    roiPercent = roiPercent(netProfit, vInvested),
                    activeAgeDays = vActiveDays,
                    sellingVelocityPerDay = vVelocity
                )
            }

            val prodNetRev = provisionalVariantRows.sumOf { it.netSalesRevenueMinor }
            val prodCogs = provisionalVariantRows.sumOf { it.cogsMinor }
            val prodGp = prodNetRev - prodCogs
            val prodInvLoss = provisionalVariantRows.sumOf { it.inventoryLossMinor }
            val prodDirExp = provisionalVariantRows.sumOf { it.directExpensesMinor }
            val prodShrExp = provisionalVariantRows.sumOf { it.allocatedSharedOverheadMinor }
            val prodNetProfit = prodGp - prodDirExp - prodShrExp - prodInvLoss
            val prodRecQty = provisionalVariantRows.sumOf { it.receivedGoodQty }
            val prodNetSold = provisionalVariantRows.sumOf { it.netSoldQty }
            val prodLocked = provisionalVariantRows.sumOf { it.lockedInventoryCostMinor }

            val finalizedVariants = provisionalVariantRows.map { vRow ->
                vRow.copy(profitContributionToProduct = calculateContribution(vRow.attributedNetProfitMinor, prodNetProfit))
            }

            val prodLayers = scopeLayers.filter { it.layer.productId == prodId }
            val prodOrigLayers = prodLayers.filter { it.layer.layerSourceType !in setOf("TRANSFER_REPLICA", "SALES_RETURN_RESTORE") }
            val prodTransit = scopeTransit.filter { it.productId == prodId }
            val prodSales = scopeSales.filter { it.allocation.productId == prodId }
            val avgCost = if (prodRecQty > 0 && prodOrigLayers.isNotEmpty()) {
                prodOrigLayers.sumOf { it.layer.initialQuantity.toLong() * it.layer.unitLandedCostBdtMinor } / prodRecQty
            } else null

            val prodSinglePoId = fixedParentPoId ?: (prodLayers.mapNotNull { it.effectivePoId } + prodSales.mapNotNull { it.effectivePoId }).distinct().singleOrNull()
            val prodSingleShipId = fixedParentShipmentId ?: (prodLayers.mapNotNull { it.effectiveShipmentId } + prodSales.mapNotNull { it.effectiveShipmentId }).distinct().singleOrNull()
            val prodPoShare = prodSinglePoId?.let { poNetProfitMap[it] }?.let { calculateContribution(prodNetProfit, it) }
            val prodShpShare = prodSingleShipId?.let { shipmentNetProfitMap[it] }?.let { calculateContribution(prodNetProfit, it) }
            val prodInvested = maxOf(
                prodOrigLayers.sumOf { it.layer.initialQuantity.toLong() * it.layer.unitLandedCostBdtMinor },
                prodCogs + prodLocked + prodInvLoss
            )
            val prodEarliestOrigin = (
                prodOrigLayers.map { it.layer.originReceiptDate } +
                    prodLayers.map { it.layer.originReceiptDate } +
                    prodTransit.map { it.originReceiptDate }
                ).minOrNull()
            val prodActiveDays = if (prodEarliestOrigin != null && (prodRecQty > 0 || prodNetSold > 0)) {
                maxOf(1, maxOf(0L, (ctx.asOfMillis - prodEarliestOrigin) / DAY_MS).toInt())
            } else 0
            val prodVelocity = if (prodActiveDays > 0) prodNetSold.toDouble() / prodActiveDays.toDouble() else null

            ProductVaultRow(
                productId = prodId,
                productNameEn = prod?.nameEn ?: prodId,
                productNameBn = prod?.nameBn ?: prodId,
                baseSku = prod?.baseSku ?: prodId,
                uom = uom,
                variants = finalizedVariants,
                receivedGoodQty = prodRecQty,
                netSoldQty = prodNetSold,
                returnedGoodQty = finalizedVariants.sumOf { it.returnedGoodQty },
                returnedDamagedQty = finalizedVariants.sumOf { it.returnedDamagedQty },
                writeOffLossQty = finalizedVariants.sumOf { it.writeOffLossQty },
                remainingGoodQty = finalizedVariants.sumOf { it.remainingGoodQty },
                remainingInTransitTransferQty = finalizedVariants.sumOf { it.remainingInTransitTransferQty },
                averageUnitLandedCostMinor = avgCost,
                netSalesRevenueMinor = prodNetRev,
                cogsMinor = prodCogs,
                grossProfitMinor = prodGp,
                grossMarginPercent = marginPercent(prodGp, prodNetRev),
                inventoryLossMinor = prodInvLoss,
                directExpensesMinor = prodDirExp,
                allocatedSharedOverheadMinor = prodShrExp,
                attributedNetProfitMinor = prodNetProfit,
                netMarginPercent = marginPercent(prodNetProfit, prodNetRev),
                lockedInventoryCostMinor = prodLocked,
                sellThroughPercent = sellThroughPercent(prodNetSold, prodRecQty),
                profitContributionToParentScope = calculateContribution(prodNetProfit, parentNetProfitMinor),
                agingSummary = computeAgingSummary(prodLayers, prodTransit, ctx.asOfMillis),
                expectedRemainingProfit = computeExpectedRemainingProfit(ctx, prodLayers, prodTransit),
                profitContributionToPo = prodPoShare,
                profitContributionToShipment = prodShpShare,
                primaryImageUri = ctx.resolveProductImageUri(prodId),
                roiPercent = roiPercent(prodNetProfit, prodInvested),
                activeAgeDays = prodActiveDays,
                sellingVelocityPerDay = prodVelocity
            )
        }.sortedByDescending { it.attributedNetProfitMinor }
    }

    private fun computePortfolioInventoryLosses(
        ctx: Context,
        filter: ImportVaultFilter,
        filteredSales: List<NetSaleSliceMetrics>,
        applyActivityWindow: Boolean = true
    ): Long {
        val activePoIds = ctx.filteredPos(filter).map { it.id }.toSet()
        val dmgRec = ctx.damageRecords.filter {
            ctx.inActivityWindow(it.createdAt, applyActivityWindow) && it.purchaseOrderId in activePoIds &&
                (filter.warehouseId == null || it.warehouseId == filter.warehouseId) &&
                (filter.productId == null || it.productId == filter.productId) &&
                (filter.variantId == null || it.variantId == filter.variantId)
        }.sumOf { it.totalLossMinor }
        val shtRec = ctx.shortageRecords.filter {
            ctx.inActivityWindow(it.createdAt, applyActivityWindow) &&
                it.disposition == "APPROVED_WRITE_OFF" && it.purchaseOrderId in activePoIds &&
                (filter.warehouseId == null || it.warehouseId == filter.warehouseId) &&
                (filter.productId == null || it.productId == filter.productId) &&
                (filter.variantId == null || it.variantId == filter.variantId)
        }.sumOf { it.totalAmountMinor }
        val retDmg = filteredSales.sumOf { it.damageLossMinor }
        val manWriteOff = ctx.filteredManualWriteOffs(filter, applyActivityWindow).sumOf { it.totalCogsBdtMinor }
        return dmgRec + shtRec + retDmg + manWriteOff
    }

    private fun computeAgingSummary(
        layers: List<LayerAsOfState>,
        transfers: List<InTransitTransferSlice>,
        asOfMillis: Long
    ): AgingSummary {
        val qtyMap = StockAgingBucket.entries.associateWith { 0 }.toMutableMap()
        val valMap = StockAgingBucket.entries.associateWith { 0L }.toMutableMap()
        var oldestDays = 0

        for (st in layers) {
            if (st.asOfRemainingQty <= 0) continue
            val originDate = st.layer.originReceiptDate
            val age = maxOf(0L, (asOfMillis - originDate) / DAY_MS).toInt()
            if (age > oldestDays) oldestDays = age
            val bucket = classifyAgingBucket(originDate, asOfMillis)
            qtyMap[bucket] = (qtyMap[bucket] ?: 0) + st.asOfRemainingQty
            valMap[bucket] = (valMap[bucket] ?: 0L) + (st.asOfRemainingQty.toLong() * st.layer.unitLandedCostBdtMinor)
        }
        for (tr in transfers) {
            if (tr.quantity <= 0) continue
            val originDate = tr.originReceiptDate
            val age = maxOf(0L, (asOfMillis - originDate) / DAY_MS).toInt()
            if (age > oldestDays) oldestDays = age
            val bucket = classifyAgingBucket(originDate, asOfMillis)
            qtyMap[bucket] = (qtyMap[bucket] ?: 0) + tr.quantity
            valMap[bucket] = (valMap[bucket] ?: 0L) + (tr.quantity.toLong() * tr.unitCostMinor)
        }
        return AgingSummary(bucketQuantities = qtyMap, bucketValuesMinor = valMap, oldestAgeDays = oldestDays)
    }

    private fun computeExpectedRemainingProfit(
        ctx: Context,
        layers: List<LayerAsOfState>,
        transfers: List<InTransitTransferSlice>
    ): ExpectedRemainingProfitEstimate {
        var totalQty = 0
        var totalCost = 0L
        var totalExpectedRev = 0L
        val missingPriceKeys = mutableSetOf<String>()

        fun accumulate(productId: String, variantId: String?, qty: Int, unitCost: Long) {
            if (qty <= 0) return
            totalQty += qty
            totalCost += qty.toLong() * unitCost
            val variantPrice = variantId?.let { ctx.variantMap[it]?.variantPriceMinor?.takeIf { p -> p > 0L } }
            val prodPrice = ctx.productMap[productId]?.sellingPriceMinor?.takeIf { p -> p > 0L }
            val sellingPrice = variantPrice ?: prodPrice
            if (sellingPrice == null) {
                missingPriceKeys.add("$productId:${variantId.orEmpty()}")
            } else {
                totalExpectedRev += qty.toLong() * sellingPrice
            }
        }

        layers.forEach { st -> accumulate(st.layer.productId, st.layer.variantId, st.asOfRemainingQty, st.layer.unitLandedCostBdtMinor) }
        transfers.forEach { tr -> accumulate(tr.productId, tr.variantId, tr.quantity, tr.unitCostMinor) }

        return ExpectedRemainingProfitEstimate(
            remainingGoodQty = totalQty,
            lockedGoodInventoryCostMinor = totalCost,
            expectedSellingRevenueMinor = totalExpectedRev,
            expectedGrossProfitMinor = totalExpectedRev - totalCost,
            priceBasisLabel = PRICE_BASIS_CURRENT,
            asOfMillis = ctx.asOfMillis,
            missingPriceSkuCount = missingPriceKeys.size
        )
    }

    private fun computeUnknownBucket(ctx: Context, attributedRevenueMinor: Long): UnknownAttributionBucket {
        val unattrSalesRev = ctx.unattributedSales.sumOf { it.netRevenueMinor }
        val unattrSalesCogs = ctx.unattributedSales.sumOf { it.netCogsMinor }

        // Legacy pre-v14 posted invoices with zero sale_layer_allocations rows
        val coveredInvoiceIds = ctx.dataset.saleLayerAllocations.filter { !it.isReversed }.mapNotNull { it.invoiceId }.toSet()
        val legacyInvoices = ctx.dataset.salesInvoices.filter {
            it.isPosted && it.status != InvoiceStatus.CANCELLED.name && it.invoiceDate <= ctx.asOfMillis && it.id !in coveredInvoiceIds
        }
        val legacyRev = legacyInvoices.sumOf { inv ->
            val returnsOnInv = ctx.dataset.salesReturns.filter { it.isPosted && it.invoiceId == inv.id && it.returnDate <= ctx.asOfMillis }
                .sumOf { it.totalReturnAmountMinor }
            maxOf(0L, (inv.subtotalMinor - inv.discountMinor) - returnsOnInv)
        }
        val legacyCogs = legacyInvoices.sumOf { inv ->
            val returnsCogs = ctx.dataset.salesReturns.filter { it.isPosted && it.invoiceId == inv.id && it.returnDate <= ctx.asOfMillis }
                .sumOf { it.totalCogsReversalMinor }
            maxOf(0L, inv.totalCogsMinor - returnsCogs)
        }

        val totalUnattrRev = unattrSalesRev + legacyRev
        val totalUnattrCogs = unattrSalesCogs + legacyCogs
        val totalMerchRev = attributedRevenueMinor + totalUnattrRev

        val unattrInvCost = ctx.unattributedLayers.sumOf { it.asOfRemainingQty.toLong() * it.layer.unitLandedCostBdtMinor }

        val validPayments = ctx.dataset.customerPayments.filter { it.isPosted && !it.isVoided && it.paymentDate <= ctx.asOfMillis }
        val validInvoiceIds = ctx.dataset.salesInvoices.filter {
            it.isPosted && it.status != InvoiceStatus.CANCELLED.name && it.invoiceDate <= ctx.asOfMillis
        }.map { it.id }.toSet()
        val allocatedPerPayment = ctx.dataset.paymentAllocations.filter {
            it.allocatedAt <= ctx.asOfMillis && it.invoiceId in validInvoiceIds
        }.groupBy { it.paymentId }.mapValues { (_, list) -> list.sumOf { it.amountMinor } }
        val unattrCollections = validPayments.sumOf { pay ->
            maxOf(0L, pay.amountMinor - (allocatedPerPayment[pay.id] ?: 0L))
        }

        val nonMerchTaxCharges = ctx.dataset.salesInvoices.filter {
            it.isPosted && it.status != InvoiceStatus.CANCELLED.name && it.invoiceDate <= ctx.asOfMillis
        }.sumOf { it.taxMinor }

        val coverage = if (totalMerchRev > 0L) (attributedRevenueMinor.toDouble() / totalMerchRev.toDouble()) * 100.0 else null

        return UnknownAttributionBucket(
            unattributedRevenueMinor = totalUnattrRev,
            unattributedCogsMinor = totalUnattrCogs,
            unattributedInventoryCostMinor = unattrInvCost,
            unattributedCollectionsMinor = unattrCollections,
            openingDueReceivableMinor = ctx.dataset.customerOpeningDueTotalMinor,
            nonMerchandiseAdditionalChargesMinor = nonMerchTaxCharges,
            revenueAttributionCoveragePercent = coverage
        )
    }

    private fun mapShipmentOperationalStatus(raw: String, hasPending: Boolean, hasReceivedLayers: Boolean): OperationalStatus {
        return when (raw) {
            ShipmentStatus.CANCELLED.name -> OperationalStatus.CANCELLED
            ShipmentStatus.DRAFT.name -> OperationalStatus.DRAFT
            ShipmentStatus.BOOKED.name -> OperationalStatus.ORDERED
            ShipmentStatus.IN_TRANSIT.name, ShipmentStatus.CUSTOMS.name, ShipmentStatus.ARRIVED.name -> OperationalStatus.IN_TRANSIT
            ShipmentStatus.RECEIVED.name -> if (hasPending) OperationalStatus.PARTIALLY_RECEIVED else OperationalStatus.RECEIVED
            ShipmentStatus.COMPLETED.name -> if (hasPending) OperationalStatus.PARTIALLY_RECEIVED else OperationalStatus.CLOSED
            else -> if (hasReceivedLayers) OperationalStatus.RECEIVED else OperationalStatus.ORDERED
        }
    }

    private fun mapPoOperationalStatus(raw: String, hasPending: Boolean, hasReceived: Boolean, hasShipment: Boolean): OperationalStatus {
        return when (raw) {
            PurchaseOrderStatus.CANCELLED.name -> OperationalStatus.CANCELLED
            PurchaseOrderStatus.DRAFT.name -> OperationalStatus.DRAFT
            PurchaseOrderStatus.COMPLETED.name -> if (hasPending) OperationalStatus.PARTIALLY_RECEIVED else OperationalStatus.CLOSED
            PurchaseOrderStatus.RECEIVED.name -> OperationalStatus.RECEIVED
            PurchaseOrderStatus.PARTIALLY_RECEIVED.name -> OperationalStatus.PARTIALLY_RECEIVED
            PurchaseOrderStatus.SHIPPED.name -> if (hasReceived) (if (hasPending) OperationalStatus.PARTIALLY_RECEIVED else OperationalStatus.RECEIVED) else OperationalStatus.IN_TRANSIT
            else -> when {
                hasReceived && hasPending -> OperationalStatus.PARTIALLY_RECEIVED
                hasReceived -> OperationalStatus.RECEIVED
                hasShipment -> OperationalStatus.IN_TRANSIT
                else -> OperationalStatus.ORDERED
            }
        }
    }

    private fun deriveCommercialStatus(
        receivedSingle: Int,
        remainingSingle: Int,
        netSoldSingle: Int,
        netProfitMinor: Long,
        breakEvenReached: Boolean
    ): CommercialStatus {
        return when {
            netSoldSingle <= 0 -> CommercialStatus.UNSOLD
            remainingSingle <= 0 && receivedSingle > 0 && netProfitMinor >= 0L -> CommercialStatus.SOLD_OUT
            breakEvenReached && netProfitMinor > 0L -> CommercialStatus.CASH_RECOVERED
            netProfitMinor < 0L -> CommercialStatus.LOSS_MAKING
            netProfitMinor > 0L -> CommercialStatus.PROFITABLE
            else -> CommercialStatus.SELLING
        }
    }

    private fun matchesShipmentFilter(s: ShipmentVaultSummary, ctx: Context, f: ImportVaultFilter): Boolean {
        if (f.operationalStatus != null && s.operationalStatus != f.operationalStatus) return false
        if (f.commercialStatus != null && s.commercialStatus != f.commercialStatus) return false
        if (f.onlyProfitable && s.attributedNetProfitMinor <= 0L) return false
        if (f.onlyLossMaking && s.attributedNetProfitMinor >= 0L) return false
        if (f.onlyPendingCost && s.costFinalizationStatus != CostFinalizationStatus.PROVISIONAL) return false
        if (f.onlyHighAging && (s.agingSummary.bucketQuantities[StockAgingBucket.DAYS_90_PLUS] ?: 0) <= 0) return false
        if (f.supplierId != null) {
            val shp = ctx.shipmentMap[s.shipmentId]
            val matches = shp?.supplierId == f.supplierId ||
                s.purchaseOrderIds.any { ctx.poMap[it]?.supplierId == f.supplierId }
            if (!matches) return false
        }
        if (f.warehouseId != null) {
            val hasWh = ctx.attributedLayers.any { it.effectiveShipmentId == s.shipmentId && it.layer.warehouseId == f.warehouseId } ||
                ctx.dataset.warehouseReceipts.any { it.shipmentId == s.shipmentId && it.warehouseId == f.warehouseId } ||
                ctx.attributedTransit.any { it.shipmentId == s.shipmentId && it.destinationWarehouseId == f.warehouseId }
            if (!hasWh) return false
        }
        if (s.shippingDate > ctx.asOfMillis) return false
        if (f.dateFilterTarget == VaultDateFilterTarget.IMPORT_COHORT_DATES && !ctx.inCohortWindow(s.shippingDate)) {
            return false
        }
        if (!f.searchQuery.isNullOrBlank()) {
            val q = Formatters.toEnglishDigits(f.searchQuery.trim()).lowercase()
            val shp = ctx.shipmentMap[s.shipmentId]
            val cartons = ctx.dataset.cartons.filter { it.shipmentId == s.shipmentId }
            val prodTokens = productSearchTokensForIds(
                ctx = ctx,
                productIds = (cartons.map { it.productId } +
                    ctx.attributedLayers.filter { it.effectiveShipmentId == s.shipmentId }.map { it.layer.productId } +
                    s.purchaseOrderIds.flatMap { poId ->
                        ctx.dataset.purchaseOrderItems.filter { it.purchaseOrderId == poId }.map { it.productId }
                    }).distinct(),
                variantIds = (cartons.mapNotNull { it.variantId } +
                    ctx.attributedLayers.filter { it.effectiveShipmentId == s.shipmentId }.mapNotNull { it.layer.variantId }).distinct()
            )
            val hay = Formatters.toEnglishDigits(
                (listOfNotNull(
                    s.shipmentNumber,
                    s.displayTitle,
                    s.notes,
                    shp?.trackingReference,
                    shp?.shippingLine,
                    shp?.notes
                ) + s.tags + s.supplierNames + s.purchaseOrderNumbers + cartons.map { it.cartonNumber } + prodTokens)
                    .joinToString(" ")
            ).lowercase()
            if (q !in hay) return false
        }
        return true
    }

    private fun matchesPoFilter(p: PurchaseOrderVaultSummary, ctx: Context, f: ImportVaultFilter): Boolean {
        if (p.operationalStatus == OperationalStatus.CANCELLED && f.operationalStatus != OperationalStatus.CANCELLED) return false
        if (f.operationalStatus != null && p.operationalStatus != f.operationalStatus) return false
        if (f.commercialStatus != null && p.commercialStatus != f.commercialStatus) return false
        if (f.onlyProfitable && p.attributedNetProfitMinor <= 0L) return false
        if (f.onlyLossMaking && p.attributedNetProfitMinor >= 0L) return false
        if (f.onlyPendingCost && p.costFinalizationStatus != CostFinalizationStatus.PROVISIONAL) return false
        if (f.onlyHighAging && (p.agingSummary.bucketQuantities[StockAgingBucket.DAYS_90_PLUS] ?: 0) <= 0) return false
        if (f.supplierId != null && p.supplierId != f.supplierId) return false
        if (f.warehouseId != null) {
            val hasWh = ctx.attributedLayers.any { it.effectivePoId == p.purchaseOrderId && it.layer.warehouseId == f.warehouseId } ||
                ctx.dataset.warehouseReceipts.any { it.purchaseOrderId == p.purchaseOrderId && it.warehouseId == f.warehouseId } ||
                ctx.attributedTransit.any { it.purchaseOrderId == p.purchaseOrderId && it.destinationWarehouseId == f.warehouseId }
            if (!hasWh) return false
        }
        if (p.orderDate > ctx.asOfMillis) return false
        if (f.dateFilterTarget == VaultDateFilterTarget.IMPORT_COHORT_DATES && !ctx.inCohortWindow(p.orderDate)) {
            return false
        }
        if (!f.searchQuery.isNullOrBlank()) {
            val q = Formatters.toEnglishDigits(f.searchQuery.trim()).lowercase()
            val po = ctx.poMap[p.purchaseOrderId]
            val poItems = ctx.dataset.purchaseOrderItems.filter { it.purchaseOrderId == p.purchaseOrderId }
            val cartons = ctx.dataset.cartons.filter { it.purchaseOrderId == p.purchaseOrderId }
            val prodTokens = productSearchTokensForIds(
                ctx = ctx,
                productIds = (poItems.map { it.productId } +
                    ctx.attributedLayers.filter { it.effectivePoId == p.purchaseOrderId }.map { it.layer.productId }).distinct(),
                variantIds = (poItems.mapNotNull { it.variantId } +
                    ctx.attributedLayers.filter { it.effectivePoId == p.purchaseOrderId }.mapNotNull { it.layer.variantId }).distinct()
            )
            val hay = Formatters.toEnglishDigits(
                (listOfNotNull(p.poNumber, p.displayTitle, p.supplierName, p.notes, po?.notes) +
                    p.tags + p.shipmentNumbers + cartons.map { it.cartonNumber } + prodTokens)
                    .joinToString(" ")
            ).lowercase()
            if (q !in hay) return false
        }
        return true
    }

    private fun productSearchTokensForIds(
        ctx: Context,
        productIds: List<String>,
        variantIds: List<String>
    ): List<String> {
        val tokens = mutableListOf<String>()
        for (pId in productIds) {
            val p = ctx.productMap[pId] ?: continue
            tokens.add(p.nameEn)
            tokens.add(p.nameBn)
            tokens.add(p.baseSku)
            p.barcode?.let { tokens.add(it) }
            p.qrCode?.let { tokens.add(it) }
            ctx.dataset.variants.filter { it.productId == pId }.forEach { v ->
                tokens.add(v.sku)
                v.color?.let { tokens.add(it) }
                v.size?.let { tokens.add(it) }
                v.barcode?.let { tokens.add(it) }
                v.qrCode?.let { tokens.add(it) }
            }
        }
        for (vId in variantIds) {
            val v = ctx.variantMap[vId] ?: continue
            tokens.add(v.sku)
            v.color?.let { tokens.add(it) }
            v.size?.let { tokens.add(it) }
            v.barcode?.let { tokens.add(it) }
            v.qrCode?.let { tokens.add(it) }
        }
        return tokens
    }

    private fun computeShipmentPoAllocationSummary(
        ctx: Context,
        shipmentId: String,
        poId: String,
        expCtx: ResolvedExpenseContext
    ): ShipmentPoAllocationSummary? {
        val shp = ctx.shipmentMap[shipmentId] ?: return null
        val po = ctx.poMap[poId] ?: return null
        val layers = ctx.attributedLayers.filter { it.effectiveShipmentId == shipmentId && it.effectivePoId == poId }
        val origLayers = layers.filter { it.layer.layerSourceType !in setOf("TRANSFER_REPLICA", "SALES_RETURN_RESTORE") }
        val transit = ctx.attributedTransit.filter { it.shipmentId == shipmentId && it.purchaseOrderId == poId }
        val sales = ctx.attributedSales.filter { it.effectiveShipmentId == shipmentId && it.effectivePoId == poId }

        val shareFraction = ctx.shipmentShareOfPo(poId, shipmentId)
        val sharePercent = shareFraction.multiply(BigDecimal("100")).toDouble()

        val receivedByUnit = QuantityByUnit.fromItems(
            origLayers.map { (ctx.productMap[it.layer.productId]?.unit ?: "PCS") to it.layer.initialQuantity }
        )
        val netSoldByUnit = QuantityByUnit.fromItems(
            sales.map { (ctx.productMap[it.allocation.productId]?.unit ?: "PCS") to it.netSoldQty }
        )
        val remainingByUnit = QuantityByUnit.fromItems(
            layers.map { (ctx.productMap[it.layer.productId]?.unit ?: "PCS") to it.asOfRemainingQty } +
                transit.map { (ctx.productMap[it.productId]?.unit ?: "PCS") to it.quantity }
        )

        val locked = layers.sumOf { it.asOfRemainingQty.toLong() * it.layer.unitLandedCostBdtMinor } +
            transit.sumOf { it.quantity.toLong() * it.unitCostMinor }
        val dmgRec = ctx.damageRecordsForShipment(shipmentId).filter { it.purchaseOrderId == poId }.sumOf { it.totalLossMinor }
        val shipmentReceiptIds = ctx.dataset.warehouseReceipts.filter { it.shipmentId == shipmentId }.map { it.id }.toSet()
        val shortWriteOff = ctx.shortageRecords
            .filter { it.purchaseOrderId == poId && it.warehouseReceiptId in shipmentReceiptIds && it.disposition == "APPROVED_WRITE_OFF" && it.createdAt <= ctx.asOfMillis }
            .sumOf { it.totalAmountMinor }
        val retDmg = sales.sumOf { it.damageLossMinor }
        val manWo = ctx.manualWriteOffsForScope(shipmentId = shipmentId, poId = poId).sumOf { it.totalCogsBdtMinor }

        val capAddCogs = BigDecimal(ctx.capitalizableAdditionalCogsForPo(poId))
            .multiply(shareFraction).setScale(0, RoundingMode.HALF_UP).longValueExact()
        val postFinCogs = ctx.costAdjustments
            .filter { it.purchaseOrderId == poId && it.referenceId == shipmentId && it.referenceType == "POST_FINALIZE_IMPORT_EXPENSE" && it.createdAt <= ctx.asOfMillis }
            .sumOf { it.cogsAdjustmentAmountMinor }
        val cogs = sales.sumOf { it.netCogsMinor } + capAddCogs + postFinCogs

        val allocatedInvestment = BigDecimal(ctx.poOwnCashInvested(poId))
            .multiply(shareFraction).setScale(0, RoundingMode.HALF_UP).longValueExact()

        val netRev = sales.sumOf { it.netRevenueMinor }
        val gp = netRev - cogs
        val dirExp = BigDecimal(computePoDirectOperatingExpense(ctx, poId))
            .multiply(shareFraction).setScale(0, RoundingMode.HALF_UP).longValueExact()
        val shrExp = BigDecimal(expCtx.allocatedByPo[poId] ?: 0L)
            .multiply(shareFraction).setScale(0, RoundingMode.HALF_UP).longValueExact()
        val invLoss = dmgRec + shortWriteOff + retDmg + manWo
        val netProfit = gp - dirExp - shrExp - invLoss

        val receivedTotal = origLayers.sumOf { it.layer.initialQuantity }
        val netSoldTotal = sales.sumOf { it.netSoldQty }

        return ShipmentPoAllocationSummary(
            shipmentId = shipmentId,
            shipmentNumber = shp.shipmentNumber,
            purchaseOrderId = poId,
            poNumber = po.poNumber,
            supplierName = ctx.supplierMap[po.supplierId]?.companyName ?: po.supplierId,
            receivedQtyByUnit = receivedByUnit,
            netSoldQtyByUnit = netSoldByUnit,
            remainingStockQtyByUnit = remainingByUnit,
            allocatedInvestmentMinor = allocatedInvestment,
            netSalesRevenueMinor = netRev,
            cogsMinor = cogs,
            attributedNetProfitMinor = netProfit,
            netMarginPercent = marginPercent(netProfit, netRev),
            sellThroughPercent = sellThroughPercent(netSoldTotal, receivedTotal),
            lockedInventoryCostMinor = locked,
            shareOfPoPercent = sharePercent
        )
    }

    private fun buildSaleSourceRows(ctx: Context, sales: List<NetSaleSliceMetrics>): List<VaultSaleSourceRow> {
        return sales.map { slice ->
            val a = slice.allocation
            val inv = a.invoiceId?.let { ctx.invoiceMap[it] }
            val prod = ctx.productMap[a.productId]
            val vObj = a.variantId?.let { ctx.variantMap[it] }
            val vLabel = listOfNotNull(vObj?.color, vObj?.size).filter { it.isNotBlank() }.joinToString(" / ")
                .ifBlank { vObj?.sku ?: prod?.baseSku ?: "" }
            VaultSaleSourceRow(
                allocationId = a.id,
                invoiceId = a.invoiceId,
                invoiceNumber = inv?.invoiceNumber ?: a.referenceId ?: a.sourceType,
                invoiceDate = slice.eventDate,
                productId = a.productId,
                productNameEn = prod?.nameEn ?: a.productId,
                productNameBn = prod?.nameBn ?: prod?.nameEn ?: a.productId,
                variantLabel = vLabel,
                quantity = slice.netSoldQty,
                netRevenueMinor = slice.netRevenueMinor,
                cogsMinor = slice.netCogsMinor,
                grossProfitMinor = slice.netRevenueMinor - slice.netCogsMinor,
                collectedCashMinor = slice.collectedCashMinor,
                receivableMinor = slice.receivableMinor,
                costLayerId = a.costLayerId,
                purchaseOrderId = slice.effectivePoId,
                shipmentId = slice.effectiveShipmentId
            )
        }.sortedByDescending { it.invoiceDate }
    }

    private fun buildReturnSourceRows(ctx: Context, sales: List<NetSaleSliceMetrics>): List<VaultReturnSourceRow> {
        val allocIds = sales.map { it.allocation.id }.toSet()
        return ctx.dataset.salesReturnLayerAllocations.filter { it.saleLayerAllocationId in allocIds }
            .map { ra ->
                val ret = ctx.returnMap[ra.returnId]
                val prod = ctx.productMap[ra.productId]
                val vObj = ra.variantId?.let { ctx.variantMap[it] }
                val vLabel = listOfNotNull(vObj?.color, vObj?.size).filter { it.isNotBlank() }.joinToString(" / ")
                    .ifBlank { vObj?.sku ?: "" }
                VaultReturnSourceRow(
                    returnId = ra.returnId,
                    returnNumber = ret?.returnNumber ?: ra.returnId,
                    returnDate = ret?.returnDate ?: ra.createdAt,
                    invoiceId = ra.invoiceId,
                    productId = ra.productId,
                    productNameEn = prod?.nameEn ?: ra.productId,
                    productNameBn = prod?.nameBn ?: prod?.nameEn ?: ra.productId,
                    variantLabel = vLabel,
                    soundQuantity = ra.soundQuantity,
                    damagedQuantity = ra.damagedQuantity,
                    reversedNetRevenueMinor = ra.reversedNetRevenueMinor,
                    reversedCogsMinor = ra.reversedCogsMinor,
                    damageLossMinor = ra.damageLossMinor
                )
            }.sortedByDescending { it.returnDate }
    }

    private fun buildCostLayerRows(ctx: Context, layers: List<LayerAsOfState>): List<VaultCostLayerRow> {
        return layers.map { st ->
            val l = st.layer
            val prod = ctx.productMap[l.productId]
            val vObj = l.variantId?.let { ctx.variantMap[it] }
            val vLabel = listOfNotNull(vObj?.color, vObj?.size).filter { it.isNotBlank() }.joinToString(" / ")
                .ifBlank { vObj?.sku ?: "" }
            val ageDays = maxOf(0L, (ctx.asOfMillis - l.originReceiptDate) / DAY_MS).toInt()
            VaultCostLayerRow(
                layerId = l.id,
                productId = l.productId,
                productNameEn = prod?.nameEn ?: l.productId,
                productNameBn = prod?.nameBn ?: prod?.nameEn ?: l.productId,
                variantId = l.variantId,
                variantLabel = vLabel,
                warehouseId = l.warehouseId,
                warehouseName = ctx.warehouseMap[l.warehouseId]?.name ?: l.warehouseId,
                receiptNumber = l.receiptNumber,
                originReceiptDate = l.originReceiptDate,
                initialQuantity = l.initialQuantity,
                remainingQuantity = st.asOfRemainingQty,
                unitLandedCostBdtMinor = l.unitLandedCostBdtMinor,
                carryingValueMinor = st.asOfRemainingQty.toLong() * l.unitLandedCostBdtMinor,
                layerSourceType = l.layerSourceType,
                agingBucket = classifyAgingBucket(l.originReceiptDate, ctx.asOfMillis),
                ageDays = ageDays,
                purchaseOrderId = st.effectivePoId,
                shipmentId = st.effectiveShipmentId
            )
        }.sortedByDescending { it.originReceiptDate }
    }

    private fun buildDocumentRefs(
        ctx: Context,
        scopeKey: String,
        scopeRefNumber: String,
        scopeTitleEn: String,
        scopeTitleBn: String,
        pos: List<PurchaseOrder>,
        shipments: List<com.pocketdev.advance_erp.data.entity.ImportShipment>,
        cartons: List<com.pocketdev.advance_erp.data.entity.ShipmentCarton>,
        importCosts: List<com.pocketdev.advance_erp.data.entity.ImportCostItem>,
        additionalCosts: List<AdditionalProductCost>,
        snapshots: List<com.pocketdev.advance_erp.data.entity.LandedCostSnapshot>,
        receipts: List<com.pocketdev.advance_erp.data.entity.WarehouseReceipt>,
        saleSources: List<VaultSaleSourceRow>,
        returnSources: List<VaultReturnSourceRow>,
        supplierPayments: List<com.pocketdev.advance_erp.data.entity.SupplierPayment>,
        directExpenses: List<com.pocketdev.advance_erp.data.entity.ExpenseVoucher>,
        attachments: List<com.pocketdev.advance_erp.data.entity.ImportVaultAttachment>
    ): List<VaultDocumentRef> {
        val docs = mutableListOf<VaultDocumentRef>()
        val liveTimestamp = minOf(ctx.asOfMillis, ctx.filter.nowMillis)
        docs.add(
            VaultDocumentRef(
                id = "doc-live-$scopeKey",
                category = VaultDocumentCategory.REPORTS,
                titleEn = "Live Regenerated Statement ($scopeTitleEn)",
                titleBn = "লাইভ রিজেনারেটেড স্টেটমেন্ট ($scopeTitleBn)",
                subtitle = "Recomputed dynamically from live ERP records",
                referenceNumber = scopeRefNumber,
                timestampMillis = liveTimestamp,
                amountMinor = null,
                currencyCode = "BDT",
                targetRoute = null,
                isImmutableSnapshot = false,
                isLiveRegeneratedReport = true
            )
        )
        pos.forEach { po ->
            val supName = ctx.supplierMap[po.supplierId]?.companyName ?: po.supplierId
            docs.add(
                VaultDocumentRef(
                    id = "doc-po-${po.id}",
                    category = VaultDocumentCategory.PURCHASE,
                    titleEn = "Purchase Order ${po.poNumber}",
                    titleBn = "ক্রয় আদেশ ${po.poNumber}",
                    subtitle = supName,
                    referenceNumber = po.poNumber,
                    timestampMillis = po.orderDate,
                    amountMinor = po.totalBdtAmountMinor,
                    currencyCode = "BDT",
                    targetRoute = "purchases"
                )
            )
            val poAttachmentPaths = po.attachments
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                .orEmpty()
            poAttachmentPaths.forEachIndexed { idx, rawPath ->
                val isWebLink = rawPath.startsWith("http://", true) || rawPath.startsWith("https://", true)
                val cleanLocal = if (!isWebLink) rawPath.removePrefix("file://") else null
                val rawFileName = cleanLocal?.substringAfterLast('/')?.substringAfter("__", cleanLocal.substringAfterLast('/'))
                    ?: rawPath
                docs.add(
                    VaultDocumentRef(
                        id = "doc-poatt-${po.id}-$idx",
                        category = VaultDocumentCategory.PURCHASE,
                        titleEn = "Supplier Invoice / File (${po.poNumber})",
                        titleBn = "সরবরাহকারী চালান / ফাইল (${po.poNumber})",
                        subtitle = rawFileName,
                        referenceNumber = po.poNumber,
                        timestampMillis = po.updatedAt,
                        amountMinor = null,
                        currencyCode = "BDT",
                        targetRoute = if (isWebLink) "purchases" else null,
                        localFilePath = cleanLocal,
                        mimeType = if (cleanLocal != null) ImportVaultAttachmentFileStore.detectMimeType(rawFileName) else null
                    )
                )
            }
        }
        shipments.forEach { shp ->
            val distinctCartons = cartons.filter { it.shipmentId == shp.id }.map { it.cartonNumber }.distinct().size
            if (distinctCartons > 0 || shp.totalCartons > 0) {
                docs.add(
                    VaultDocumentRef(
                        id = "doc-pack-${shp.id}",
                        category = VaultDocumentCategory.PACKING,
                        titleEn = "Packing List & Carton Summary (${shp.shipmentNumber})",
                        titleBn = "প্যাকিং তালিকা ও কার্টন সামারি (${shp.shipmentNumber})",
                        subtitle = "$distinctCartons cartons · ${shp.totalGrossWeightKg} kg",
                        referenceNumber = shp.shipmentNumber,
                        timestampMillis = shp.shippingDate,
                        amountMinor = null,
                        currencyCode = "BDT",
                        targetRoute = "shipments"
                    )
                )
            }
        }
        importCosts.forEach { cost ->
            val costLocal = cost.attachmentPath?.trim()?.takeIf { it.isNotBlank() }?.removePrefix("file://")
            docs.add(
                VaultDocumentRef(
                    id = "doc-cost-${cost.id}",
                    category = VaultDocumentCategory.FREIGHT,
                    titleEn = cost.description.ifBlank { cost.costCategory },
                    titleBn = cost.description.ifBlank { cost.costCategory },
                    subtitle = cost.documentReference ?: cost.paymentAccount,
                    referenceNumber = cost.documentReference ?: cost.costCategory,
                    timestampMillis = cost.costDate,
                    amountMinor = cost.bdtAmountMinor,
                    currencyCode = "BDT",
                    targetRoute = "import_management",
                    localFilePath = costLocal,
                    mimeType = costLocal?.let { ImportVaultAttachmentFileStore.detectMimeType(it) }
                )
            )
        }
        additionalCosts.forEach { ac ->
            docs.add(
                VaultDocumentRef(
                    id = "doc-acost-${ac.id}",
                    category = VaultDocumentCategory.FREIGHT,
                    titleEn = ac.description.ifBlank { ac.category },
                    titleBn = ac.description.ifBlank { ac.category },
                    subtitle = "${ac.costNumber} · ${if (ac.isCapitalizable) "Capitalized" else "Operating"}",
                    referenceNumber = ac.costNumber,
                    timestampMillis = ac.expenseDate,
                    amountMinor = ac.bdtAmountMinor,
                    currencyCode = "BDT",
                    targetRoute = "purchases"
                )
            )
        }
        receipts.forEach { rec ->
            docs.add(
                VaultDocumentRef(
                    id = "doc-rec-${rec.id}",
                    category = VaultDocumentCategory.RECEIVING,
                    titleEn = "Warehouse Receipt ${rec.receiptNumber}",
                    titleBn = "গুদাম রসিদ ${rec.receiptNumber}",
                    subtitle = ctx.warehouseMap[rec.warehouseId]?.name ?: rec.warehouseId,
                    referenceNumber = rec.receiptNumber,
                    timestampMillis = rec.receiptDate,
                    amountMinor = null,
                    currencyCode = "BDT",
                    targetRoute = "inventory"
                )
            )
        }
        val invoiceGroups = saleSources.filter { it.invoiceId != null }.groupBy { it.invoiceId!! }
        invoiceGroups.forEach { (invId, rows) ->
            val inv = ctx.invoiceMap[invId] ?: return@forEach
            docs.add(
                VaultDocumentRef(
                    id = "doc-sale-$invId",
                    category = VaultDocumentCategory.SALES,
                    titleEn = "Sales Invoice ${inv.invoiceNumber}",
                    titleBn = "বিক্রয় চালান ${inv.invoiceNumber}",
                    subtitle = inv.customerName,
                    referenceNumber = inv.invoiceNumber,
                    timestampMillis = inv.invoiceDate,
                    amountMinor = rows.sumOf { it.netRevenueMinor },
                    currencyCode = "BDT",
                    targetRoute = "sales"
                )
            )
        }
        returnSources.groupBy { it.returnId }.forEach { (retId, rows) ->
            val first = rows.first()
            val qty = rows.sumOf { it.soundQuantity + it.damagedQuantity }
            docs.add(
                VaultDocumentRef(
                    id = "doc-ret-$retId",
                    category = VaultDocumentCategory.SALES,
                    titleEn = "Sales Return ${first.returnNumber}",
                    titleBn = "বিক্রয় রিটার্ন ${first.returnNumber}",
                    subtitle = "${first.productNameEn} · $qty qty",
                    referenceNumber = first.returnNumber,
                    timestampMillis = first.returnDate,
                    amountMinor = rows.sumOf { it.reversedNetRevenueMinor },
                    currencyCode = "BDT",
                    targetRoute = "sales"
                )
            )
        }
        supplierPayments.forEach { sp ->
            docs.add(
                VaultDocumentRef(
                    id = "doc-sp-${sp.id}",
                    category = VaultDocumentCategory.PAYMENTS,
                    titleEn = "Supplier Payment ${sp.paymentNumber}",
                    titleBn = "সরবরাহকারী পেমেন্ট ${sp.paymentNumber}",
                    subtitle = sp.paymentMethod,
                    referenceNumber = sp.paymentNumber,
                    timestampMillis = sp.paymentDate,
                    amountMinor = sp.bdtAmountMinor,
                    currencyCode = "BDT",
                    targetRoute = "accounts"
                )
            )
        }
        val invIds = invoiceGroups.keys
        val linkedAllocPayments = ctx.dataset.paymentAllocations.filter { it.invoiceId in invIds }.map { it.paymentId }.distinct()
        linkedAllocPayments.forEach { payId ->
            val pay = ctx.dataset.customerPayments.find { it.id == payId } ?: return@forEach
            docs.add(
                VaultDocumentRef(
                    id = "doc-cp-${pay.id}",
                    category = VaultDocumentCategory.PAYMENTS,
                    titleEn = "Customer Receipt ${pay.paymentNumber}",
                    titleBn = "গ্রাহক কালেকশন ${pay.paymentNumber}",
                    subtitle = pay.customerName,
                    referenceNumber = pay.paymentNumber,
                    timestampMillis = pay.paymentDate,
                    amountMinor = pay.amountMinor,
                    currencyCode = "BDT",
                    targetRoute = "ledger"
                )
            )
        }
        directExpenses.forEach { ev ->
            val evLocal = ev.attachmentPath?.trim()?.takeIf { it.isNotBlank() }?.removePrefix("file://")
            docs.add(
                VaultDocumentRef(
                    id = "doc-exp-${ev.id}",
                    category = VaultDocumentCategory.PAYMENTS,
                    titleEn = "Expense ${ev.voucherNumber}",
                    titleBn = "খরচ ভাউচার ${ev.voucherNumber}",
                    subtitle = ev.descriptionEn,
                    referenceNumber = ev.voucherNumber,
                    timestampMillis = ev.voucherDate,
                    amountMinor = ev.bdtAmountMinor,
                    currencyCode = "BDT",
                    targetRoute = "accounts",
                    localFilePath = evLocal,
                    mimeType = evLocal?.let { ImportVaultAttachmentFileStore.detectMimeType(it) }
                )
            )
        }
        if (snapshots.isNotEmpty()) {
            val latest = snapshots.maxBy { it.snapshotDate }
            docs.add(
                VaultDocumentRef(
                    id = "doc-snap-${latest.shipmentId}",
                    category = VaultDocumentCategory.REPORTS,
                    titleEn = "Finalized Landed Cost Snapshot (${snapshots.size} SKUs)",
                    titleBn = "চূড়ান্ত ল্যান্ডেড কস্ট স্ন্যাপশট (${snapshots.size} আইটেম)",
                    subtitle = if (latest.isFinal) "Finalized" else "Provisional",
                    referenceNumber = latest.sku,
                    timestampMillis = latest.snapshotDate,
                    amountMinor = snapshots.sumOf { it.totalLandedCostBdtMinor },
                    currencyCode = "BDT",
                    targetRoute = "import_management",
                    isImmutableSnapshot = latest.isFinal
                )
            )
        }
        attachments.forEach { att ->
            val resolvedCategory = runCatching { VaultDocumentCategory.valueOf(att.category) }.getOrElse {
                if (att.isImmutableSnapshot) VaultDocumentCategory.REPORTS else VaultDocumentCategory.ATTACHMENTS
            }
            docs.add(
                VaultDocumentRef(
                    id = "doc-att-${att.id}",
                    category = resolvedCategory,
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
            )
        }
        return docs.sortedByDescending { it.timestampMillis }
    }

    private fun buildTimelineEvents(
        ctx: Context,
        pos: List<PurchaseOrder>,
        shipments: List<com.pocketdev.advance_erp.data.entity.ImportShipment>,
        snapshots: List<com.pocketdev.advance_erp.data.entity.LandedCostSnapshot>,
        receipts: List<com.pocketdev.advance_erp.data.entity.WarehouseReceipt>,
        saleSources: List<VaultSaleSourceRow>,
        returnSources: List<VaultReturnSourceRow>,
        supplierPayments: List<com.pocketdev.advance_erp.data.entity.SupplierPayment>,
        directExpenses: List<com.pocketdev.advance_erp.data.entity.ExpenseVoucher>,
        costAdjustments: List<com.pocketdev.advance_erp.data.entity.InventoryCostAdjustment>
    ): List<VaultTimelineEvent> {
        val events = mutableListOf<VaultTimelineEvent>()
        pos.forEach { po ->
            events.add(
                VaultTimelineEvent(
                    id = "tl-po-${po.id}",
                    type = VaultTimelineEventType.PO_CREATED,
                    timestampMillis = po.orderDate,
                    titleEn = "PO ${po.poNumber} Created",
                    titleBn = "ক্রয় আদেশ ${po.poNumber} তৈরি",
                    subtitle = ctx.supplierMap[po.supplierId]?.companyName ?: po.supplierId,
                    referenceNumber = po.poNumber,
                    amountMinor = po.totalBdtAmountMinor,
                    currencyCode = "BDT",
                    targetRoute = "purchases"
                )
            )
            if (po.isActualCostFinalized && po.actualCostFinalizedAt != null) {
                events.add(
                    VaultTimelineEvent(
                        id = "tl-pofin-${po.id}",
                        type = VaultTimelineEventType.COST_FINALIZED,
                        timestampMillis = po.actualCostFinalizedAt,
                        titleEn = "Cost Finalized for ${po.poNumber}",
                        titleBn = "${po.poNumber}-এর খরচ চূড়ান্ত",
                        subtitle = po.poNumber,
                        referenceNumber = po.poNumber,
                        amountMinor = null,
                        currencyCode = "BDT",
                        targetRoute = "import_management"
                    )
                )
            }
        }
        shipments.forEach { shp ->
            events.add(
                VaultTimelineEvent(
                    id = "tl-shp-${shp.id}",
                    type = VaultTimelineEventType.SHIPMENT_DISPATCHED,
                    timestampMillis = shp.shippingDate,
                    titleEn = "Shipment ${shp.shipmentNumber} Dispatched",
                    titleBn = "চালান ${shp.shipmentNumber} পাঠানো হয়েছে",
                    subtitle = "${shp.origin} -> ${shp.destination}",
                    referenceNumber = shp.shipmentNumber,
                    amountMinor = null,
                    currencyCode = "BDT",
                    targetRoute = "shipments"
                )
            )
            shp.actualArrivalDate?.let { arr ->
                events.add(
                    VaultTimelineEvent(
                        id = "tl-arr-${shp.id}",
                        type = VaultTimelineEventType.SHIPMENT_ARRIVED,
                        timestampMillis = arr,
                        titleEn = "Shipment ${shp.shipmentNumber} Arrived",
                        titleBn = "চালান ${shp.shipmentNumber} পৌঁছেছে",
                        subtitle = shp.destination,
                        referenceNumber = shp.shipmentNumber,
                        amountMinor = null,
                        currencyCode = "BDT",
                        targetRoute = "shipments"
                    )
                )
            }
        }
        if (snapshots.isNotEmpty()) {
            val latest = snapshots.maxBy { it.snapshotDate }
            events.add(
                VaultTimelineEvent(
                    id = "tl-snap-${latest.id}",
                    type = VaultTimelineEventType.COST_FINALIZED,
                    timestampMillis = latest.snapshotDate,
                    titleEn = if (latest.isFinal) "Landed Cost Finalized" else "Provisional Landed Cost Snapshot",
                    titleBn = if (latest.isFinal) "ল্যান্ডেড কস্ট চূড়ান্ত" else "প্রাথমিক ল্যান্ডেড কস্ট স্ন্যাপশট",
                    subtitle = "${snapshots.size} SKUs",
                    referenceNumber = latest.sku,
                    amountMinor = snapshots.sumOf { it.totalLandedCostBdtMinor },
                    currencyCode = "BDT",
                    targetRoute = "import_management"
                )
            )
        }
        receipts.forEach { rec ->
            events.add(
                VaultTimelineEvent(
                    id = "tl-rec-${rec.id}",
                    type = VaultTimelineEventType.WAREHOUSE_RECEIVED,
                    timestampMillis = rec.postedAt ?: rec.receiptDate,
                    titleEn = "Received ${rec.receiptNumber}",
                    titleBn = "গুদামে গ্রহণ ${rec.receiptNumber}",
                    subtitle = ctx.warehouseMap[rec.warehouseId]?.name ?: rec.warehouseId,
                    referenceNumber = rec.receiptNumber,
                    amountMinor = null,
                    currencyCode = "BDT",
                    targetRoute = "inventory"
                )
            )
        }
        val salesByInv = saleSources.groupBy { it.invoiceNumber }
        salesByInv.forEach { (invNo, rows) ->
            val first = rows.first()
            events.add(
                VaultTimelineEvent(
                    id = "tl-sale-${first.invoiceId ?: invNo}",
                    type = VaultTimelineEventType.SALE_POSTED,
                    timestampMillis = first.invoiceDate,
                    titleEn = "Sale $invNo (${rows.sumOf { it.quantity }} units)",
                    titleBn = "বিক্রয় $invNo (${rows.sumOf { it.quantity }} ইউনিট)",
                    subtitle = first.productNameEn,
                    referenceNumber = invNo,
                    amountMinor = rows.sumOf { it.netRevenueMinor },
                    currencyCode = "BDT",
                    targetRoute = "sales"
                )
            )
        }
        returnSources.groupBy { it.returnId }.forEach { (retId, rows) ->
            val first = rows.first()
            events.add(
                VaultTimelineEvent(
                    id = "tl-ret-$retId",
                    type = VaultTimelineEventType.SALES_RETURN,
                    timestampMillis = first.returnDate,
                    titleEn = "Sales Return ${first.returnNumber}",
                    titleBn = "বিক্রয় ফেরত ${first.returnNumber}",
                    subtitle = first.productNameEn,
                    referenceNumber = first.returnNumber,
                    amountMinor = rows.sumOf { it.reversedNetRevenueMinor },
                    currencyCode = "BDT",
                    targetRoute = "sales"
                )
            )
        }
        supplierPayments.forEach { sp ->
            events.add(
                VaultTimelineEvent(
                    id = "tl-sp-${sp.id}",
                    type = VaultTimelineEventType.SUPPLIER_PAYMENT,
                    timestampMillis = sp.paymentDate,
                    titleEn = "Supplier Payment ${sp.paymentNumber}",
                    titleBn = "সরবরাহকারী পেমেন্ট ${sp.paymentNumber}",
                    subtitle = sp.paymentMethod,
                    referenceNumber = sp.paymentNumber,
                    amountMinor = sp.bdtAmountMinor,
                    currencyCode = "BDT",
                    targetRoute = "accounts"
                )
            )
        }
        val invIds = saleSources.mapNotNull { it.invoiceId }.toSet()
        val payIds = ctx.dataset.paymentAllocations.filter { it.invoiceId in invIds }.map { it.paymentId }.distinct()
        payIds.forEach { pid ->
            val pay = ctx.dataset.customerPayments.find { it.id == pid } ?: return@forEach
            val linkedAmt = ctx.dataset.paymentAllocations
                .filter { it.paymentId == pid && it.invoiceId in invIds }
                .sumOf { it.amountMinor }
            events.add(
                VaultTimelineEvent(
                    id = "tl-cp-${pay.id}",
                    type = VaultTimelineEventType.CUSTOMER_PAYMENT,
                    timestampMillis = pay.paymentDate,
                    titleEn = "Customer Payment ${pay.paymentNumber}",
                    titleBn = "গ্রাহক পেমেন্ট ${pay.paymentNumber}",
                    subtitle = pay.customerName,
                    referenceNumber = pay.paymentNumber,
                    amountMinor = linkedAmt,
                    currencyCode = "BDT",
                    targetRoute = "ledger"
                )
            )
        }
        directExpenses.forEach { ev ->
            events.add(
                VaultTimelineEvent(
                    id = "tl-ev-${ev.id}",
                    type = VaultTimelineEventType.EXPENSE_RECORDED,
                    timestampMillis = ev.voucherDate,
                    titleEn = "Expense ${ev.voucherNumber}",
                    titleBn = "খরচ ${ev.voucherNumber}",
                    subtitle = ev.descriptionEn,
                    referenceNumber = ev.voucherNumber,
                    amountMinor = ev.bdtAmountMinor,
                    currencyCode = "BDT",
                    targetRoute = "accounts"
                )
            )
        }
        costAdjustments.forEach { adj ->
            events.add(
                VaultTimelineEvent(
                    id = "tl-adj-${adj.id}",
                    type = VaultTimelineEventType.COST_ADJUSTMENT,
                    timestampMillis = adj.createdAt,
                    titleEn = "Cost Adjustment ${adj.adjustmentNumber}",
                    titleBn = "খরচ সমন্বয় ${adj.adjustmentNumber}",
                    subtitle = adj.reason,
                    referenceNumber = adj.adjustmentNumber,
                    amountMinor = adj.totalAdjustmentAmountMinor,
                    currencyCode = "BDT",
                    targetRoute = "inventory"
                )
            )
        }
        return events.sortedByDescending { it.timestampMillis }
    }

    private fun buildSalesTrend(
        saleSources: List<VaultSaleSourceRow>,
        filter: ImportVaultFilter,
        asOfMillis: Long
    ): List<VaultTrendPoint> {
        if (saleSources.isEmpty()) return emptyList()
        val minTs = filter.dateFromMillis ?: saleSources.minOf { it.invoiceDate }
        val maxTs = filter.dateToMillis?.let { minOf(it, asOfMillis) } ?: asOfMillis
        val spanDays = maxOf(0L, (maxTs - minTs) / DAY_MS)
        val useDailyBuckets = spanDays <= 45L

        val shortEnMonths = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val shortBnMonths = arrayOf("জানু", "ফেব্রু", "মার্চ", "এপ্রি", "মে", "জুন", "জুলা", "আগ", "সেপ্টে", "অক্টো", "নভে", "ডিসে")

        val cal = Calendar.getInstance()
        val grouped = saleSources.groupBy { row ->
            cal.timeInMillis = row.invoiceDate
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            if (useDailyBuckets) {
                val d = cal.get(Calendar.DAY_OF_MONTH)
                "%04d-%02d-%02d".format(Locale.US, y, m + 1, d)
            } else {
                "%04d-%02d".format(Locale.US, y, m + 1)
            }
        }

        return grouped.entries.sortedBy { it.key }.map { (key, rows) ->
            cal.timeInMillis = rows.first().invoiceDate
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH).coerceIn(0, 11)
            val startMs: Long
            val endMs: Long
            val labelEn: String
            val labelBn: String
            if (useDailyBuckets) {
                val d = cal.get(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                startMs = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                endMs = cal.timeInMillis
                labelEn = "%02d %s".format(Locale.US, d, shortEnMonths[m])
                labelBn = "${Formatters.toBanglaDigits("%02d".format(Locale.US, d))} ${shortBnMonths[m]}"
            } else {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                startMs = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                endMs = cal.timeInMillis
                val yy = (y % 100)
                labelEn = "${shortEnMonths[m]} %02d".format(Locale.US, yy)
                labelBn = "${shortBnMonths[m]} ${Formatters.toBanglaDigits("%02d".format(Locale.US, yy))}"
            }
            val rev = rows.sumOf { it.netRevenueMinor }
            val cogs = rows.sumOf { it.cogsMinor }
            VaultTrendPoint(
                bucketKey = key,
                labelEn = labelEn,
                labelBn = labelBn,
                startMillis = startMs,
                endMillis = endMs,
                netRevenueMinor = rev,
                cogsMinor = cogs,
                grossProfitMinor = rev - cogs,
                netQtySold = rows.sumOf { it.quantity },
                invoiceCount = rows.mapNotNull { it.invoiceId }.distinct().size.takeIf { it > 0 } ?: rows.size
            )
        }
    }

    private fun parseTags(tagsJson: String?): List<String> {
        val raw = tagsJson?.trim().orEmpty()
        if (raw.isEmpty()) return emptyList()
        val inner = if (raw.startsWith("[") && raw.endsWith("]")) raw.substring(1, raw.length - 1) else raw
        return inner.split(",")
            .map { it.trim().removePrefix("\"").removeSuffix("\"").trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private fun marginPercent(profit: Long, revenue: Long): Double? =
        if (revenue > 0L) (profit.toDouble() / revenue.toDouble()) * 100.0 else null

    private fun roiPercent(netProfit: Long, investedCapital: Long): Double? =
        if (investedCapital > 0L) (netProfit.toDouble() / investedCapital.toDouble()) * 100.0 else null

    private fun sellThroughPercent(sold: Int, receivedGood: Int): Double? =
        if (receivedGood > 0) (sold.toDouble() / receivedGood.toDouble()) * 100.0 else null
}
