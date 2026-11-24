package com.pocketdev.advance_erp.feature.imports

import com.pocketdev.advance_erp.core.engine.AllocationItemInput
import com.pocketdev.advance_erp.core.engine.AllocationMethod
import com.pocketdev.advance_erp.core.engine.CartonWeightAllocationEngine
import com.pocketdev.advance_erp.core.engine.LandedCostAllocationEngine
import com.pocketdev.advance_erp.core.engine.MixedCartonLineInput
import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.data.entity.CostCategory
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.LandedCostSnapshot
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.ShipmentFreightRate
import java.math.BigDecimal
import java.math.RoundingMode

internal data class PoItemLandedBreakdown(
    val itemId: String,
    val productId: String,
    val variantId: String?,
    val productTitle: String,
    val variantLabel: String,
    val sku: String,
    val categoryId: String,
    val quantity: Int,
    val cartonCount: Int,
    val grossWeightKg: BigDecimal,
    val purchaseForeignMinor: Long,
    val purchaseBdtMinor: Long,
    val freightBdtMinor: Long,
    val otherOverheadBdtMinor: Long,
    val totalLandedBdtMinor: Long,
    val unitLandedBdt: BigDecimal
)

internal data class PoProductLandedBreakdown(
    val productId: String,
    val productTitle: String,
    val baseSku: String,
    val categoryId: String,
    val categoryTitle: String,
    val totalPcs: Int,
    val totalCartons: Int,
    val grossWeightKg: BigDecimal,
    val purchaseForeignMinor: Long,
    val purchaseBdtMinor: Long,
    val freightBdtMinor: Long,
    val otherOverheadBdtMinor: Long,
    val totalLandedBdtMinor: Long,
    val unitLandedBdt: BigDecimal,
    val itemBreakdowns: List<PoItemLandedBreakdown>
)

internal data class PoLandedCalculationSummary(
    val totalPurchaseBdtMinor: Long,
    val totalFreightBdtMinor: Long,
    val derivedRateFreightMinor: Long,
    val totalOtherOverheadBdtMinor: Long,
    val totalLandedBdtMinor: Long,
    val overallUnitLandedBdt: BigDecimal?,
    val isFinalizedSnapshotBased: Boolean,
    val itemBreakdownsById: Map<String, PoItemLandedBreakdown>,
    val productBreakdowns: List<PoProductLandedBreakdown>,
    val categoryBreakdowns: List<PoCategoryLandedBreakdown>
)

internal data class PoFreightPayableSummary(
    val totalFreightBillMinor: Long,
    val paidFreightBillMinor: Long,
    val remainingFreightBillMinor: Long,
    val progress: Float
)

internal fun calculatePoFreightPayableSummary(
    landedSummary: PoLandedCalculationSummary,
    costItems: List<ImportCostItem>,
    additionalCosts: List<AdditionalProductCost>,
    paidFreightBillMinor: Long,
    ledgerAccruedBillMinor: Long? = null
): PoFreightPayableSummary {
    val alreadyDirectlyPaidMinor = costItems
        .filter { it.isPosted && it.costCategory != CostCategory.PRODUCT_PURCHASE.name }
        .sumOf { it.bdtAmountMinor } +
        additionalCosts
            .filter { it.isCapitalizable }
            .sumOf { it.bdtAmountMinor }
    val totalBillMinor = if (ledgerAccruedBillMinor != null && ledgerAccruedBillMinor > 0L) {
        ledgerAccruedBillMinor
    } else {
        (landedSummary.totalFreightBdtMinor + landedSummary.totalOtherOverheadBdtMinor - alreadyDirectlyPaidMinor)
            .coerceAtLeast(0L)
    }
    val effectiveTotalMinor = maxOf(totalBillMinor, paidFreightBillMinor.coerceAtLeast(0L))
    val paidMinor = paidFreightBillMinor.coerceAtLeast(0L)
    val remainingMinor = (effectiveTotalMinor - paidMinor).coerceAtLeast(0L)
    val progress = if (effectiveTotalMinor > 0L) {
        (paidMinor.toFloat() / effectiveTotalMinor.toFloat()).coerceIn(0f, 1f)
    } else 0f
    return PoFreightPayableSummary(
        totalFreightBillMinor = effectiveTotalMinor,
        paidFreightBillMinor = paidMinor,
        remainingFreightBillMinor = remainingMinor,
        progress = progress
    )
}

internal data class PoDraftCostCalcItem(
    val itemId: String,
    val productId: String,
    val variantId: String?,
    val quantity: Int,
    val purchaseForeignMinor: Long,
    val purchaseBdtMinor: Long,
    val grossWeightKg: BigDecimal,
    val cartonCount: Int
)

private val FREIGHT_CATEGORY_NAMES = setOf(
    CostCategory.INTERNATIONAL_FREIGHT.name,
    CostCategory.AIR_FREIGHT.name,
    CostCategory.SEA_FREIGHT.name
)

internal fun poItemKey(productId: String, variantId: String?): String =
    if (variantId != null) "$productId:$variantId" else productId

private fun allocateLongProportionally(
    totalMinor: Long,
    weights: List<BigDecimal>
): List<Long> {
    if (weights.isEmpty() || totalMinor == 0L) return List(weights.size) { 0L }
    val weightSum = weights.fold(BigDecimal.ZERO, BigDecimal::add)
    val effectiveWeights = if (weightSum > BigDecimal.ZERO) weights else List(weights.size) { BigDecimal.ONE }
    val effectiveSum = effectiveWeights.fold(BigDecimal.ZERO, BigDecimal::add)
    val lastPositiveIdx = effectiveWeights.indexOfLast { it > BigDecimal.ZERO }.takeIf { it >= 0 } ?: effectiveWeights.lastIndex
    var allocated = 0L
    return effectiveWeights.indices.map { idx ->
        if (idx == lastPositiveIdx) {
            totalMinor - allocated
        } else {
            val share = BigDecimal.valueOf(totalMinor)
                .multiply(effectiveWeights[idx])
                .divide(effectiveSum, 0, RoundingMode.FLOOR)
                .toLong()
            allocated += share
            share
        }
    }
}

private fun allocateDecimalProportionally(
    totalValue: BigDecimal,
    weights: List<BigDecimal>,
    scale: Int
): List<BigDecimal> {
    if (weights.isEmpty() || totalValue <= BigDecimal.ZERO) {
        return List(weights.size) { BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP) }
    }
    val weightSum = weights.fold(BigDecimal.ZERO, BigDecimal::add)
    val effectiveWeights = if (weightSum > BigDecimal.ZERO) weights else List(weights.size) { BigDecimal.ONE }
    val effectiveSum = effectiveWeights.fold(BigDecimal.ZERO, BigDecimal::add)
    val lastPositiveIdx = effectiveWeights.indexOfLast { it > BigDecimal.ZERO }.takeIf { it >= 0 } ?: effectiveWeights.lastIndex
    var allocated = BigDecimal.ZERO
    return effectiveWeights.indices.map { idx ->
        if (idx == lastPositiveIdx) {
            (totalValue - allocated).max(BigDecimal.ZERO).setScale(scale, RoundingMode.HALF_UP)
        } else {
            val share = totalValue.multiply(effectiveWeights[idx]).divide(effectiveSum, scale, RoundingMode.FLOOR)
            allocated = allocated.add(share)
            share
        }
    }
}

private fun safeAllocateCostAcrossItems(
    totalCostBdtMinor: Long,
    baseInputs: List<AllocationItemInput>,
    method: AllocationMethod,
    manualMap: Map<String, Long> = emptyMap(),
    fallbackKeyByItemId: Map<String, Pair<String, String>> = emptyMap()
): Map<String, Long> {
    if (baseInputs.isEmpty() || totalCostBdtMinor == 0L) return emptyMap()
    if (method == AllocationMethod.MANUAL) {
        if (manualMap.isNotEmpty()) {
            val resolvedInputs = baseInputs.map { inp ->
                val (itemKey, productId) = fallbackKeyByItemId[inp.id] ?: (inp.id to inp.id)
                val manualAmt = manualMap[inp.id] ?: manualMap[itemKey] ?: manualMap[productId] ?: 0L
                inp.copy(manualAmountBdtMinor = manualAmt)
            }
            val manualSum = resolvedInputs.sumOf { it.manualAmountBdtMinor ?: 0L }
            if (manualSum == totalCostBdtMinor) {
                return LandedCostAllocationEngine.allocate(totalCostBdtMinor, resolvedInputs, AllocationMethod.MANUAL)
                    .associate { it.itemId to it.allocatedBdtMinor }
            }
        }
        return LandedCostAllocationEngine.allocate(totalCostBdtMinor, baseInputs, AllocationMethod.PURCHASE_VALUE)
            .associate { it.itemId to it.allocatedBdtMinor }
    }
    return LandedCostAllocationEngine.allocate(totalCostBdtMinor, baseInputs, method)
        .associate { it.itemId to it.allocatedBdtMinor }
}

private data class ResolvedPackingMetrics(
    val grossWeightKg: BigDecimal,
    val cbm: BigDecimal,
    val cartonCount: Int
)

private fun resolvePackingMetricsForItems(
    items: List<PurchaseOrderItem>,
    cartons: List<ShipmentCarton>
): Map<String, ResolvedPackingMetrics> {
    if (items.isEmpty()) return emptyMap()
    if (cartons.isEmpty()) {
        return items.associate { item ->
            item.id to ResolvedPackingMetrics(
                grossWeightKg = item.grossWeightKg.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO,
                cbm = item.cbm.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO,
                cartonCount = item.cartonQuantity
            )
        }
    }

    val weightByKey = mutableMapOf<String, BigDecimal>()
    val cbmByKey = mutableMapOf<String, BigDecimal>()
    val cartonNumsByKey = mutableMapOf<String, MutableSet<String>>()

    cartons.groupBy { it.cartonNumber }.forEach { (cartonNo, cLines) ->
        val boxCbm = cLines.firstOrNull()?.cbm?.trim()?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val cLineCbmTotal = if (cLines.size == 1) {
            listOf(cLines.first().cbm.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO)
        } else {
            val allEqualBox = boxCbm > BigDecimal.ZERO && cLines.all {
                (it.cbm.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO).compareTo(boxCbm) == 0
            }
            if (allEqualBox) {
                allocateDecimalProportionally(
                    boxCbm,
                    cLines.map { BigDecimal(it.quantity.coerceAtLeast(1)) },
                    scale = 6
                )
            } else {
                cLines.map { it.cbm.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO }
            }
        }

        val allLinesHaveWeight = cLines.all {
            (it.grossWeightKg.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO
        }
        if (allLinesHaveWeight) {
            cLines.forEachIndexed { idx, line ->
                val key = poItemKey(line.productId, line.variantId)
                val w = line.grossWeightKg.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                weightByKey[key] = (weightByKey[key] ?: BigDecimal.ZERO).add(w)
                cbmByKey[key] = (cbmByKey[key] ?: BigDecimal.ZERO).add(cLineCbmTotal[idx])
                cartonNumsByKey.getOrPut(key) { mutableSetOf() }.add(cartonNo)
            }
        } else {
            val boxGw = cLines.firstOrNull()?.cartonGrossWeightKg?.trim()?.toBigDecimalOrNull()
                ?.takeIf { it > BigDecimal.ZERO }
                ?: cLines.fold(BigDecimal.ZERO) { acc, l ->
                    acc.add(l.grossWeightKg.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO)
                }
            val allocated = if (boxGw > BigDecimal.ZERO) {
                runCatching {
                    CartonWeightAllocationEngine.allocateWeights(
                        cartonGrossWeightKg = boxGw,
                        lines = cLines.map { l ->
                            MixedCartonLineInput(
                                productId = l.productId,
                                variantId = l.variantId,
                                quantity = l.quantity.coerceAtLeast(1),
                                manualGrossWeightKg = l.grossWeightKg.trim().toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
                            )
                        }
                    )
                }.getOrNull()
            } else null

            if (allocated != null) {
                allocated.forEachIndexed { idx, alloc ->
                    val key = poItemKey(alloc.productId, alloc.variantId)
                    weightByKey[key] = (weightByKey[key] ?: BigDecimal.ZERO).add(alloc.allocatedGrossWeightKg)
                    cbmByKey[key] = (cbmByKey[key] ?: BigDecimal.ZERO).add(cLineCbmTotal[idx])
                    cartonNumsByKey.getOrPut(key) { mutableSetOf() }.add(cartonNo)
                }
            } else {
                cLines.forEachIndexed { idx, line ->
                    val key = poItemKey(line.productId, line.variantId)
                    val w = line.grossWeightKg.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                    weightByKey[key] = (weightByKey[key] ?: BigDecimal.ZERO).add(w)
                    cbmByKey[key] = (cbmByKey[key] ?: BigDecimal.ZERO).add(cLineCbmTotal[idx])
                    cartonNumsByKey.getOrPut(key) { mutableSetOf() }.add(cartonNo)
                }
            }
        }
    }

    val result = mutableMapOf<String, ResolvedPackingMetrics>()
    items.groupBy { poItemKey(it.productId, it.variantId) }.forEach { (key, keyItems) ->
        val hasCartonData = key in cartonNumsByKey || key in weightByKey
        if (!hasCartonData) {
            keyItems.forEach { item ->
                result[item.id] = ResolvedPackingMetrics(
                    grossWeightKg = item.grossWeightKg.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    cbm = item.cbm.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    cartonCount = item.cartonQuantity
                )
            }
        } else if (keyItems.size == 1) {
            val item = keyItems.first()
            result[item.id] = ResolvedPackingMetrics(
                grossWeightKg = weightByKey[key] ?: BigDecimal.ZERO,
                cbm = cbmByKey[key] ?: BigDecimal.ZERO,
                cartonCount = cartonNumsByKey[key]?.size ?: item.cartonQuantity
            )
        } else {
            val totalKeyWeight = weightByKey[key] ?: BigDecimal.ZERO
            val totalKeyCbm = cbmByKey[key] ?: BigDecimal.ZERO
            val itemFallbackWeights = keyItems.map { it.grossWeightKg.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO }
            val splitBasis = if (itemFallbackWeights.any { it > BigDecimal.ZERO }) {
                itemFallbackWeights
            } else {
                keyItems.map { BigDecimal(it.quantity.coerceAtLeast(1)) }
            }
            val splitWeights = allocateDecimalProportionally(totalKeyWeight, splitBasis, scale = 3)
            val splitCbms = allocateDecimalProportionally(totalKeyCbm, splitBasis, scale = 6)
            val keyCartons = cartonNumsByKey[key]?.size ?: 0
            keyItems.forEachIndexed { idx, item ->
                val ctnCount = if (item.cartonQuantity > 0) item.cartonQuantity else if (idx == 0) keyCartons else 0
                result[item.id] = ResolvedPackingMetrics(
                    grossWeightKg = splitWeights[idx],
                    cbm = splitCbms[idx],
                    cartonCount = ctnCount
                )
            }
        }
    }
    return result
}

private fun resolveCategoryTitle(
    categoryId: String,
    categoryMap: Map<String, ProductCategory>,
    rateByCat: Map<String, ShipmentFreightRate>,
    isBangla: Boolean
): String {
    val catObj = categoryMap[categoryId]
    val rateObj = rateByCat[categoryId]
    return when {
        catObj != null -> if (isBangla && catObj.nameBn.isNotBlank()) catObj.nameBn else catObj.nameEn
        rateObj != null -> if (isBangla && rateObj.categoryNameBn.isNotBlank()) rateObj.categoryNameBn else rateObj.categoryNameEn
        else -> if (isBangla) "অন্যান্য ক্যাটাগরি" else "Uncategorized"
    }
}

internal fun calculatePoLandedCostSummary(
    order: PurchaseOrder,
    items: List<PurchaseOrderItem>,
    cartons: List<ShipmentCarton>,
    costItems: List<ImportCostItem>,
    additionalCosts: List<AdditionalProductCost>,
    freightRates: List<ShipmentFreightRate>,
    categories: List<ProductCategory>,
    products: List<Product>,
    variants: List<ProductVariant>,
    snapshots: List<LandedCostSnapshot> = emptyList(),
    isBangla: Boolean = false,
    isCostFinalized: Boolean = order.isActualCostFinalized,
    ledgerAccruedImportMinor: Long? = null
): PoLandedCalculationSummary {
    val productMap = products.associateBy { it.id }
    val variantMap = variants.associateBy { it.id }
    val categoryMap = categories.associateBy { it.id }
    val rateByCat = freightRates.associateBy { it.categoryId }

    if (items.isEmpty()) {
        val explicitCapital = costItems.filter {
            (it.isCapitalizableCost || it.isCapitalized || it.costCategory == CostCategory.OTHER_CHARGES.name) &&
                it.costCategory != CostCategory.PRODUCT_PURCHASE.name
        }
        val explicitFreight = explicitCapital.filter { it.costCategory in FREIGHT_CATEGORY_NAMES }.sumOf { it.bdtAmountMinor }
        val otherCost = explicitCapital.filterNot { it.costCategory in FREIGHT_CATEGORY_NAMES }.sumOf { it.bdtAmountMinor } +
            additionalCosts.filter { it.isCapitalizable }.sumOf { it.bdtAmountMinor }
        val total = order.totalBdtAmountMinor + explicitFreight + otherCost
        return PoLandedCalculationSummary(
            totalPurchaseBdtMinor = order.totalBdtAmountMinor,
            totalFreightBdtMinor = explicitFreight,
            derivedRateFreightMinor = 0L,
            totalOtherOverheadBdtMinor = otherCost,
            totalLandedBdtMinor = total,
            overallUnitLandedBdt = null,
            isFinalizedSnapshotBased = false,
            itemBreakdownsById = emptyMap(),
            productBreakdowns = emptyList(),
            categoryBreakdowns = emptyList()
        )
    }

    val packingMetricsByItemId = resolvePackingMetricsForItems(items, cartons)

    // Base Purchase BDT per item, reconciled cleanly against order.totalBdtAmountMinor if > 0
    val rawPurchaseMinors = items.map { it.totalBdtAmountMinor }.toMutableList()
    if (order.totalBdtAmountMinor > 0L) {
        val delta = order.totalBdtAmountMinor - rawPurchaseMinors.sum()
        if (delta != 0L) {
            val targetIdx = rawPurchaseMinors.indexOfLast { it > 0L }.takeIf { it >= 0 } ?: rawPurchaseMinors.lastIndex
            rawPurchaseMinors[targetIdx] = (rawPurchaseMinors[targetIdx] + delta).coerceAtLeast(0L)
        }
    }

    val purchaseByItemId = mutableMapOf<String, Long>()
    val freightByItemId = mutableMapOf<String, Long>()
    val otherOverheadByItemId = mutableMapOf<String, Long>()
    var derivedRateFreightMinor = 0L
    var snapshotModeUsed = false

    val allocInputs = items.mapIndexed { idx, item ->
        val prod = productMap[item.productId]
        val variant = item.variantId?.let { variantMap[it] }
        val metrics = packingMetricsByItemId[item.id]!!
        AllocationItemInput(
            id = item.id,
            name = "${prod?.nameEn ?: item.productId}${variant?.sku?.let { " ($it)" }.orEmpty()}",
            quantity = item.quantity.coerceAtLeast(0),
            grossWeightKg = metrics.grossWeightKg,
            cbm = metrics.cbm,
            purchaseValueBdtMinor = rawPurchaseMinors[idx],
            cartonCount = metrics.cartonCount
        )
    }
    val fallbackKeyByItemId = items.associate { it.id to (poItemKey(it.productId, it.variantId) to it.productId) }

    // Prefer finalized LandedCostSnapshots when present for all items
    if (isCostFinalized && snapshots.isNotEmpty()) {
        val matchedSnapshotsByItemId = items.associate { item ->
            val match = snapshots.find { it.productId == item.productId && it.variantId == item.variantId }
                ?: if (item.variantId == null) {
                    snapshots.find { it.productId == item.productId && it.variantId == null }
                        ?: snapshots.singleOrNull { it.productId == item.productId }
                } else {
                    null
                }
            item.id to match
        }

        if (items.all { matchedSnapshotsByItemId[it.id] != null }) {
            snapshotModeUsed = true
            val hasAnyNonFreightCostItem = costItems.any { ci ->
                (ci.isCapitalizableCost || ci.isCapitalized || ci.costCategory == CostCategory.OTHER_CHARGES.name) &&
                    ci.costCategory != CostCategory.PRODUCT_PURCHASE.name &&
                    ci.costCategory !in FREIGHT_CATEGORY_NAMES &&
                    ci.bdtAmountMinor > 0L
            }
            items.groupBy { matchedSnapshotsByItemId[it.id]!!.id }.forEach { (_, groupItems) ->
                val snap = matchedSnapshotsByItemId[groupItems.first().id]!!
                val effectiveSnapLanded = if (snap.unitLandedCostBdtMinor > 0L && snap.totalReceivedQuantity > 0) {
                    snap.totalReceivedQuantity.toLong() * snap.unitLandedCostBdtMinor
                } else {
                    snap.totalLandedCostBdtMinor
                }
                val effectiveSnapAllocated = (effectiveSnapLanded - snap.totalPurchaseCostBdtMinor).coerceAtLeast(0L)
                val rawSnapOther = (snap.chinaDeliveryBdtMinor + snap.bdTransportBdtMinor + snap.otherExpensesBdtMinor).coerceAtLeast(0L)
                val rawSnapTotalOverhead = snap.internationalFreightBdtMinor + rawSnapOther
                val effectiveSnapFreight = when {
                    rawSnapOther == 0L && !hasAnyNonFreightCostItem -> effectiveSnapAllocated
                    rawSnapTotalOverhead > 0L -> BigDecimal.valueOf(effectiveSnapAllocated)
                        .multiply(BigDecimal.valueOf(snap.internationalFreightBdtMinor))
                        .divide(BigDecimal.valueOf(rawSnapTotalOverhead), 0, RoundingMode.HALF_UP)
                        .longValueExact()
                        .coerceIn(0L, effectiveSnapAllocated)
                    else -> snap.internationalFreightBdtMinor.coerceAtMost(effectiveSnapAllocated)
                }
                val snapOther = (effectiveSnapAllocated - effectiveSnapFreight).coerceAtLeast(0L)
                if (groupItems.size == 1) {
                    val only = groupItems.first()
                    purchaseByItemId[only.id] = snap.totalPurchaseCostBdtMinor
                    freightByItemId[only.id] = effectiveSnapFreight
                    otherOverheadByItemId[only.id] = snapOther
                } else {
                    val qtyWeights = groupItems.map { BigDecimal(it.quantity.coerceAtLeast(1)) }
                    val purchSplit = allocateLongProportionally(snap.totalPurchaseCostBdtMinor, qtyWeights)
                    val freightSplit = allocateLongProportionally(effectiveSnapFreight, qtyWeights)
                    val otherSplit = allocateLongProportionally(snapOther, qtyWeights)
                    groupItems.forEachIndexed { idx, itm ->
                        purchaseByItemId[itm.id] = purchSplit[idx]
                        freightByItemId[itm.id] = freightSplit[idx]
                        otherOverheadByItemId[itm.id] = otherSplit[idx]
                    }
                }
            }

            if (ledgerAccruedImportMinor != null && ledgerAccruedImportMinor >= 0L) {
                val alreadyPostedDirectMinor = costItems
                    .filter { it.isPosted && it.costCategory != CostCategory.PRODUCT_PURCHASE.name }
                    .sumOf { it.bdtAmountMinor }
                val targetTotalAllocated = ledgerAccruedImportMinor + alreadyPostedDirectMinor
                val oldFreightTotal = items.sumOf { freightByItemId[it.id] ?: 0L }
                val oldOtherTotal = items.sumOf { otherOverheadByItemId[it.id] ?: 0L }
                val currentTotalAllocated = oldFreightTotal + oldOtherTotal
                val snapshotsHaveNoOtherCosts = snapshots.all {
                    (it.chinaDeliveryBdtMinor + it.bdTransportBdtMinor + it.otherExpensesBdtMinor) <= 0L
                } && !hasAnyNonFreightCostItem
                if ((currentTotalAllocated != targetTotalAllocated || (snapshotsHaveNoOtherCosts && oldOtherTotal > 0L)) && currentTotalAllocated > 0L) {
                    val newFreightTotal = if (oldOtherTotal == 0L || snapshotsHaveNoOtherCosts) {
                        targetTotalAllocated
                    } else {
                        BigDecimal.valueOf(targetTotalAllocated)
                            .multiply(BigDecimal.valueOf(oldFreightTotal))
                            .divide(BigDecimal.valueOf(currentTotalAllocated), 0, RoundingMode.HALF_UP)
                            .longValueExact()
                            .coerceIn(0L, targetTotalAllocated)
                    }
                    val newOtherTotal = (targetTotalAllocated - newFreightTotal).coerceAtLeast(0L)
                    val fWeights = items.map {
                        val fw = freightByItemId[it.id] ?: 0L
                        BigDecimal(if (snapshotsHaveNoOtherCosts) fw + (otherOverheadByItemId[it.id] ?: 0L) else fw)
                    }
                    val newFSplit = allocateLongProportionally(newFreightTotal, fWeights)
                    val newOSplit = if (newOtherTotal > 0L) {
                        val oWeights = items.map { BigDecimal(otherOverheadByItemId[it.id] ?: 0L) }
                        allocateLongProportionally(newOtherTotal, oWeights)
                    } else {
                        List(items.size) { 0L }
                    }
                    items.forEachIndexed { idx, itm ->
                        freightByItemId[itm.id] = newFSplit[idx]
                        otherOverheadByItemId[itm.id] = newOSplit[idx]
                    }
                }
            }
        }
    }

    if (!snapshotModeUsed) {
        items.forEachIndexed { idx, item ->
            purchaseByItemId[item.id] = rawPurchaseMinors[idx]
        }

        val capitalizableCostItems = costItems.filter {
            (it.isCapitalizableCost || it.isCapitalized || it.costCategory == CostCategory.OTHER_CHARGES.name) &&
                it.costCategory != CostCategory.PRODUCT_PURCHASE.name
        }
        val hasAnyFreightCostItem = costItems.any { it.costCategory in FREIGHT_CATEGORY_NAMES }
        val explicitFreightItems = capitalizableCostItems.filter { it.costCategory in FREIGHT_CATEGORY_NAMES }

        if (explicitFreightItems.isNotEmpty()) {
            explicitFreightItems.forEach { ci ->
                val method = runCatching { AllocationMethod.valueOf(ci.allocationMethod) }
                    .getOrDefault(AllocationMethod.GROSS_WEIGHT)
                val manualMap = LandedCostAllocationEngine.parseManualAllocations(ci.manualAllocationsJson)
                val allocated = safeAllocateCostAcrossItems(
                    totalCostBdtMinor = ci.bdtAmountMinor,
                    baseInputs = allocInputs,
                    method = method,
                    manualMap = manualMap,
                    fallbackKeyByItemId = fallbackKeyByItemId
                )
                allocated.forEach { (itemId, amt) ->
                    freightByItemId[itemId] = (freightByItemId[itemId] ?: 0L) + amt
                }
            }
        } else if (!hasAnyFreightCostItem) {
            val useCategoryRates = freightRates.isNotEmpty() &&
                (!order.currency.equals("BDT", ignoreCase = true) || freightRates.any { (it.overrideRatePerKgBdtMinor ?: 0L) > 0L })

            if (useCategoryRates) {
                items.groupBy { productMap[it.productId]?.categoryId ?: "uncategorized" }.forEach { (catId, catItems) ->
                    val rateRow = rateByCat[catId]
                    val appliedMinor = rateRow?.overrideRatePerKgBdtMinor
                        ?: rateRow?.appliedRatePerKgBdtMinor?.takeIf { it > 0L }
                        ?: categoryMap[catId]?.defaultFreightRatePerKgBdtMinor
                        ?: 0L
                    val catWeight = catItems.fold(BigDecimal.ZERO) { acc, itm ->
                        acc.add(packingMetricsByItemId[itm.id]?.grossWeightKg ?: BigDecimal.ZERO)
                    }
                    val catFreightMinor = if (appliedMinor > 0L && catWeight > BigDecimal.ZERO) {
                        catWeight.multiply(BigDecimal(appliedMinor)).setScale(0, RoundingMode.HALF_UP).longValueExact()
                    } else 0L

                    if (catFreightMinor > 0L) {
                        derivedRateFreightMinor += catFreightMinor
                        val catAllocInputs = catItems.map { itm -> allocInputs.first { a -> a.id == itm.id } }
                        val catMethod = when {
                            catAllocInputs.any { it.grossWeightKg > BigDecimal.ZERO } -> AllocationMethod.GROSS_WEIGHT
                            catAllocInputs.any { it.quantity > 0 } -> AllocationMethod.QUANTITY
                            else -> AllocationMethod.PURCHASE_VALUE
                        }
                        val allocated = safeAllocateCostAcrossItems(catFreightMinor, catAllocInputs, catMethod)
                        allocated.forEach { (itemId, amt) ->
                            freightByItemId[itemId] = (freightByItemId[itemId] ?: 0L) + amt
                        }
                    }
                }
            } else if (!order.currency.equals("BDT", ignoreCase = true)) {
                val poRatePerKgBdt = order.shippingRatePerKgBdt.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                val totalPoWeight = allocInputs.fold(BigDecimal.ZERO) { acc, a -> acc.add(a.grossWeightKg) }
                if (poRatePerKgBdt > BigDecimal.ZERO && totalPoWeight > BigDecimal.ZERO) {
                    val poFreightMinor = totalPoWeight
                        .multiply(poRatePerKgBdt)
                        .movePointRight(2)
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValueExact()
                    derivedRateFreightMinor = poFreightMinor
                    val allocated = safeAllocateCostAcrossItems(poFreightMinor, allocInputs, AllocationMethod.GROSS_WEIGHT)
                    allocated.forEach { (itemId, amt) ->
                        freightByItemId[itemId] = (freightByItemId[itemId] ?: 0L) + amt
                    }
                }
            }
        }

        // Non-freight capitalizable import cost items (China Local, Handling, Customs, Sample, etc.)
        capitalizableCostItems
            .filterNot { it.costCategory in FREIGHT_CATEGORY_NAMES }
            .forEach { ci ->
                val defaultMethod = runCatching { CostCategory.valueOf(ci.costCategory).defaultAllocationMethod }
                    .getOrDefault(AllocationMethod.QUANTITY)
                val method = runCatching { AllocationMethod.valueOf(ci.allocationMethod) }
                    .getOrDefault(defaultMethod)
                val manualMap = LandedCostAllocationEngine.parseManualAllocations(ci.manualAllocationsJson)
                val allocated = safeAllocateCostAcrossItems(
                    totalCostBdtMinor = ci.bdtAmountMinor,
                    baseInputs = allocInputs,
                    method = method,
                    manualMap = manualMap,
                    fallbackKeyByItemId = fallbackKeyByItemId
                )
                allocated.forEach { (itemId, amt) ->
                    otherOverheadByItemId[itemId] = (otherOverheadByItemId[itemId] ?: 0L) + amt
                }
            }
    }

    // Capitalizable post-arrival additional costs apply on top in both live and snapshot modes
    additionalCosts
        .filter { it.isCapitalizable && it.bdtAmountMinor > 0L }
        .forEach { ac ->
            val method = runCatching { AllocationMethod.valueOf(ac.allocationMethod) }
                .getOrDefault(AllocationMethod.QUANTITY)
                .let { if (it == AllocationMethod.MANUAL) AllocationMethod.QUANTITY else it }
            val allocated = safeAllocateCostAcrossItems(
                totalCostBdtMinor = ac.bdtAmountMinor,
                baseInputs = allocInputs,
                method = method
            )
            allocated.forEach { (itemId, amt) ->
                otherOverheadByItemId[itemId] = (otherOverheadByItemId[itemId] ?: 0L) + amt
            }
        }

    val itemBreakdowns = items.map { item ->
        val prod = productMap[item.productId]
        val variant = item.variantId?.let { variantMap[it] }
        val metrics = packingMetricsByItemId[item.id]!!
        val prodTitle = if (isBangla && !prod?.nameBn.isNullOrBlank()) prod!!.nameBn else prod?.nameEn ?: item.productId
        val varLabel = variant?.let {
            listOfNotNull(
                it.color?.takeIf { c -> c.isNotBlank() },
                it.size?.takeIf { s -> s.isNotBlank() }
            ).joinToString(" • ")
        }.orEmpty()
        val sku = variant?.sku?.takeIf { it.isNotBlank() } ?: prod?.baseSku.orEmpty()
        val catId = prod?.categoryId ?: "uncategorized"

        val purchBdt = purchaseByItemId[item.id] ?: 0L
        val freightBdt = freightByItemId[item.id] ?: 0L
        val overheadBdt = otherOverheadByItemId[item.id] ?: 0L
        val totalItemLanded = purchBdt + freightBdt + overheadBdt
        val unitItemLanded = if (item.quantity > 0) {
            BigDecimal.valueOf(totalItemLanded, 2)
                .divide(BigDecimal(item.quantity), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }

        PoItemLandedBreakdown(
            itemId = item.id,
            productId = item.productId,
            variantId = item.variantId,
            productTitle = prodTitle,
            variantLabel = varLabel,
            sku = sku,
            categoryId = catId,
            quantity = item.quantity,
            cartonCount = metrics.cartonCount,
            grossWeightKg = metrics.grossWeightKg.setScale(3, RoundingMode.HALF_UP),
            purchaseForeignMinor = item.totalAmountMinor,
            purchaseBdtMinor = purchBdt,
            freightBdtMinor = freightBdt,
            otherOverheadBdtMinor = overheadBdt,
            totalLandedBdtMinor = totalItemLanded,
            unitLandedBdt = unitItemLanded
        )
    }

    val productBreakdowns = itemBreakdowns
        .groupBy { it.productId }
        .map { (productId, prodItems) ->
            val prod = productMap[productId]
            val first = prodItems.first()
            val catId = first.categoryId
            val catTitle = resolveCategoryTitle(catId, categoryMap, rateByCat, isBangla)
            val totalPcs = prodItems.sumOf { it.quantity }
            val totalCartons = prodItems.sumOf { it.cartonCount }
            val totalWeight = prodItems.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.grossWeightKg) }
            val purchForeign = prodItems.sumOf { it.purchaseForeignMinor }
            val purchBdt = prodItems.sumOf { it.purchaseBdtMinor }
            val freightBdt = prodItems.sumOf { it.freightBdtMinor }
            val otherBdt = prodItems.sumOf { it.otherOverheadBdtMinor }
            val totalLanded = prodItems.sumOf { it.totalLandedBdtMinor }
            val unitLanded = if (totalPcs > 0) {
                BigDecimal.valueOf(totalLanded, 2).divide(BigDecimal(totalPcs), 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            }

            PoProductLandedBreakdown(
                productId = productId,
                productTitle = first.productTitle,
                baseSku = prod?.baseSku?.takeIf { it.isNotBlank() } ?: first.sku,
                categoryId = catId,
                categoryTitle = catTitle,
                totalPcs = totalPcs,
                totalCartons = totalCartons,
                grossWeightKg = totalWeight.setScale(3, RoundingMode.HALF_UP),
                purchaseForeignMinor = purchForeign,
                purchaseBdtMinor = purchBdt,
                freightBdtMinor = freightBdt,
                otherOverheadBdtMinor = otherBdt,
                totalLandedBdtMinor = totalLanded,
                unitLandedBdt = unitLanded,
                itemBreakdowns = prodItems
            )
        }

    val categoryBreakdowns = itemBreakdowns
        .groupBy { it.categoryId }
        .map { (catId, catItems) ->
            val catTitle = resolveCategoryTitle(catId, categoryMap, rateByCat, isBangla)
            val totalPcs = catItems.sumOf { it.quantity }
            val totalWeight = catItems.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.grossWeightKg) }.setScale(2, RoundingMode.HALF_UP)
            val prodBdt = BigDecimal.valueOf(catItems.sumOf { it.purchaseBdtMinor }, 2)
            val freightBdt = BigDecimal.valueOf(catItems.sumOf { it.freightBdtMinor }, 2)
            val allocBdt = BigDecimal.valueOf(catItems.sumOf { it.otherOverheadBdtMinor }, 2)
            val totalLandedBdt = BigDecimal.valueOf(catItems.sumOf { it.totalLandedBdtMinor }, 2)
            val avgPerPcs = if (totalPcs > 0) {
                totalLandedBdt.divide(BigDecimal(totalPcs), 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            }
            PoCategoryLandedBreakdown(
                categoryId = catId,
                categoryTitle = catTitle,
                totalPcs = totalPcs,
                grossWeightKg = totalWeight,
                productCostBdt = prodBdt,
                freightCostBdt = freightBdt,
                allocatedOverheadBdt = allocBdt,
                totalLandedCostBdt = totalLandedBdt,
                averagePerPcsBdt = avgPerPcs
            )
        }

    val totalPurch = itemBreakdowns.sumOf { it.purchaseBdtMinor }
    val totalFr = itemBreakdowns.sumOf { it.freightBdtMinor }
    val totalOther = itemBreakdowns.sumOf { it.otherOverheadBdtMinor }
    val totalLanded = itemBreakdowns.sumOf { it.totalLandedBdtMinor }
    val totalPcsAll = itemBreakdowns.sumOf { it.quantity }
    val overallUnit = if (totalPcsAll > 0) {
        BigDecimal.valueOf(totalLanded, 2).divide(BigDecimal(totalPcsAll), 2, RoundingMode.HALF_UP)
    } else null

    return PoLandedCalculationSummary(
        totalPurchaseBdtMinor = totalPurch,
        totalFreightBdtMinor = totalFr,
        derivedRateFreightMinor = derivedRateFreightMinor,
        totalOtherOverheadBdtMinor = totalOther,
        totalLandedBdtMinor = totalLanded,
        overallUnitLandedBdt = overallUnit,
        isFinalizedSnapshotBased = snapshotModeUsed,
        itemBreakdownsById = itemBreakdowns.associateBy { it.itemId },
        productBreakdowns = productBreakdowns,
        categoryBreakdowns = categoryBreakdowns
    )
}

internal fun calculateDraftPoProductLandedBreakdowns(
    draftItems: List<PoDraftCostCalcItem>,
    products: List<Product>,
    variants: List<ProductVariant>,
    categories: List<ProductCategory>,
    isLocalPurchase: Boolean,
    categoryFreightRatesMinor: Map<String, Long>,
    sharedOverheadBdt: BigDecimal,
    expectedTotalProductBdt: BigDecimal,
    expectedTotalFreightBdt: BigDecimal,
    isBangla: Boolean
): List<PoProductLandedBreakdown> {
    if (draftItems.isEmpty()) return emptyList()
    val productMap = products.associateBy { it.id }
    val variantMap = variants.associateBy { it.id }
    val categoryMap = categories.associateBy { it.id }

    // Reconcile draft product purchase BDT against expectedTotalProductBdt
    val expectedProdMinor = expectedTotalProductBdt.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
    val prodMinors = draftItems.map { it.purchaseBdtMinor }.toMutableList()
    val prodDelta = expectedProdMinor - prodMinors.sum()
    if (prodDelta != 0L) {
        val targetIdx = prodMinors.indexOfLast { it > 0L }.takeIf { it >= 0 } ?: prodMinors.lastIndex
        prodMinors[targetIdx] = (prodMinors[targetIdx] + prodDelta).coerceAtLeast(0L)
    }

    val freightByItemId = mutableMapOf<String, Long>()
    if (!isLocalPurchase) {
        val expectedFreightMinor = expectedTotalFreightBdt.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        if (expectedFreightMinor > 0L) {
            draftItems.groupBy { productMap[it.productId]?.categoryId ?: "uncategorized" }.forEach { (catId, catItems) ->
                val rateMinor = categoryFreightRatesMinor[catId] ?: 0L
                val catWeight = catItems.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.grossWeightKg) }
                val catFreightMinor = if (rateMinor > 0L && catWeight > BigDecimal.ZERO) {
                    catWeight.multiply(BigDecimal(rateMinor)).setScale(0, RoundingMode.HALF_UP).longValueExact()
                } else 0L
                if (catFreightMinor > 0L) {
                    val catInputs = catItems.mapIndexed { _, i ->
                        AllocationItemInput(
                            id = i.itemId,
                            name = i.productId,
                            quantity = i.quantity.coerceAtLeast(0),
                            grossWeightKg = i.grossWeightKg,
                            purchaseValueBdtMinor = i.purchaseBdtMinor,
                            cartonCount = i.cartonCount
                        )
                    }
                    val method = when {
                        catInputs.any { it.grossWeightKg > BigDecimal.ZERO } -> AllocationMethod.GROSS_WEIGHT
                        catInputs.any { it.quantity > 0 } -> AllocationMethod.QUANTITY
                        else -> AllocationMethod.PURCHASE_VALUE
                    }
                    safeAllocateCostAcrossItems(catFreightMinor, catInputs, method).forEach { (id, amt) ->
                        freightByItemId[id] = amt
                    }
                }
            }
            val freightDelta = expectedFreightMinor - freightByItemId.values.sum()
            if (freightDelta != 0L) {
                val targetId = draftItems.lastOrNull { (freightByItemId[it.itemId] ?: 0L) > 0L }?.itemId
                    ?: draftItems.last().itemId
                freightByItemId[targetId] = ((freightByItemId[targetId] ?: 0L) + freightDelta).coerceAtLeast(0L)
            }
        }
    }

    val sharedOverheadMinor = sharedOverheadBdt.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
    val qtyWeights = draftItems.map { BigDecimal(it.quantity.coerceAtLeast(0)) }
    val overheadSplit = allocateLongProportionally(sharedOverheadMinor, qtyWeights)

    val itemBreakdowns = draftItems.mapIndexed { idx, itm ->
        val prod = productMap[itm.productId]
        val variant = itm.variantId?.let { variantMap[it] }
        val catId = prod?.categoryId ?: "uncategorized"
        val prodTitle = if (isBangla && !prod?.nameBn.isNullOrBlank()) prod!!.nameBn else prod?.nameEn ?: itm.productId
        val varLabel = variant?.let {
            listOfNotNull(
                it.color?.takeIf { c -> c.isNotBlank() },
                it.size?.takeIf { s -> s.isNotBlank() }
            ).joinToString(" • ")
        }.orEmpty()
        val sku = variant?.sku?.takeIf { it.isNotBlank() } ?: prod?.baseSku.orEmpty()
        val purchBdt = prodMinors[idx]
        val freightBdt = freightByItemId[itm.itemId] ?: 0L
        val otherBdt = overheadSplit[idx]
        val totalBdt = purchBdt + freightBdt + otherBdt
        val unitBdt = if (itm.quantity > 0) {
            BigDecimal.valueOf(totalBdt, 2).divide(BigDecimal(itm.quantity), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }
        PoItemLandedBreakdown(
            itemId = itm.itemId,
            productId = itm.productId,
            variantId = itm.variantId,
            productTitle = prodTitle,
            variantLabel = varLabel,
            sku = sku,
            categoryId = catId,
            quantity = itm.quantity,
            cartonCount = itm.cartonCount,
            grossWeightKg = itm.grossWeightKg.setScale(3, RoundingMode.HALF_UP),
            purchaseForeignMinor = itm.purchaseForeignMinor,
            purchaseBdtMinor = purchBdt,
            freightBdtMinor = freightBdt,
            otherOverheadBdtMinor = otherBdt,
            totalLandedBdtMinor = totalBdt,
            unitLandedBdt = unitBdt
        )
    }

    return itemBreakdowns
        .groupBy { it.productId }
        .map { (productId, prodItems) ->
            val prod = productMap[productId]
            val first = prodItems.first()
            val catId = first.categoryId
            val catObj = categoryMap[catId]
            val catTitle = if (catObj != null) {
                if (isBangla && catObj.nameBn.isNotBlank()) catObj.nameBn else catObj.nameEn
            } else {
                if (isBangla) "অন্যান্য ক্যাটাগরি" else "Uncategorized"
            }
            val totalPcs = prodItems.sumOf { it.quantity }
            val totalCartons = prodItems.sumOf { it.cartonCount }
            val totalWeight = prodItems.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.grossWeightKg) }.setScale(3, RoundingMode.HALF_UP)
            val purchForeign = prodItems.sumOf { it.purchaseForeignMinor }
            val purchBdt = prodItems.sumOf { it.purchaseBdtMinor }
            val freightBdt = prodItems.sumOf { it.freightBdtMinor }
            val otherBdt = prodItems.sumOf { it.otherOverheadBdtMinor }
            val totalLanded = prodItems.sumOf { it.totalLandedBdtMinor }
            val unitLanded = if (totalPcs > 0) {
                BigDecimal.valueOf(totalLanded, 2).divide(BigDecimal(totalPcs), 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            }
            PoProductLandedBreakdown(
                productId = productId,
                productTitle = first.productTitle,
                baseSku = prod?.baseSku?.takeIf { it.isNotBlank() } ?: first.sku,
                categoryId = catId,
                categoryTitle = catTitle,
                totalPcs = totalPcs,
                totalCartons = totalCartons,
                grossWeightKg = totalWeight,
                purchaseForeignMinor = purchForeign,
                purchaseBdtMinor = purchBdt,
                freightBdtMinor = freightBdt,
                otherOverheadBdtMinor = otherBdt,
                totalLandedBdtMinor = totalLanded,
                unitLandedBdt = unitLanded,
                itemBreakdowns = prodItems
            )
        }
}
