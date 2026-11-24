package com.pocketdev.advance_erp.core.engine

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Pure (Android-free) packing rules for Purchase Orders.
 *
 * Every rule is keyed by the stable database identifiers (productId + variantId) and never by
 * display names. A carton is identified by an immutable [cartonUid]; the visible "CTN-001"
 * label is only a mutable display sequence (see [formatCartonNumber]).
 */
data class VariantKey(val productId: String, val variantId: String?)

data class PoOrderItemGroupLine(
    val productId: String,
    val variantId: String?,
    val quantityStr: String = "",
    val unitPriceStr: String = "",
    val manualQuantityOverride: Boolean = false,
    val manualPriceOverride: Boolean = false
)

data class PoProductGroupSummary(
    val productId: String,
    val variantCount: Int,
    val totalQuantity: Int,
    val totalAmountMajor: BigDecimal
)

data class PackingOrderLine(val key: VariantKey, val orderedQty: Int)

data class PackingCartonLine(val cartonUid: String, val key: VariantKey, val quantity: Int)

enum class PackingIssueType {
    /** Sum of carton allocations for a variant is larger than the ordered quantity. */
    OVERPACKED,
    /** The same variant appears twice inside one carton. */
    DUPLICATE_VARIANT_IN_CARTON,
    /** Allocation quantity is zero/negative. */
    INVALID_QUANTITY,
    /** Carton references a variant that is not part of the order. */
    UNORDERED_VARIANT,
    /** Ordered quantity not fully allocated (draft warning, confirm blocker). */
    UNDERPACKED,
    /** Carton contains no allocation. */
    EMPTY_CARTON
}

data class PackingIssue(
    val type: PackingIssueType,
    val key: VariantKey? = null,
    val cartonUid: String? = null,
    val ordered: Int = 0,
    val packed: Int = 0
) {
    val isBlockingForDraft: Boolean
        get() = type != PackingIssueType.UNDERPACKED
}

data class VariantPackingStatus(
    val key: VariantKey,
    val ordered: Int,
    val packed: Int
) {
    /** Can be negative when over-packed; never hidden. */
    val remaining: Int get() = ordered - packed
    val isComplete: Boolean get() = ordered > 0 && packed == ordered
    val isOverpacked: Boolean get() = packed > ordered
}

data class PackingSummary(
    val variants: List<VariantPackingStatus>,
    val cartonCount: Int
) {
    val totalOrdered: Int get() = variants.sumOf { it.ordered }
    val totalPacked: Int get() = variants.sumOf { it.packed }
    val totalRemaining: Int get() = totalOrdered - totalPacked
    /** 0..100, integer percent of ordered quantity that is packed (capped at 100). */
    val percent: Int
        get() = if (totalOrdered <= 0) 0 else
            ((minOf(totalPacked, totalOrdered).toLong() * 100L) / totalOrdered).toInt()
    val isComplete: Boolean get() = variants.isNotEmpty() && variants.all { it.isComplete }
}

data class QuantityChangeCheck(val allowed: Boolean, val packed: Int)

enum class VariantVisibility { AVAILABLE, ALL, COMPLETED }

object PoPackingEngine {

    /** Continuous, gap-free, zero padded: CTN-001, CTN-002 ... */
    fun formatCartonNumber(sequence: Int): String =
        String.format(Locale.ROOT, "CTN-%03d", sequence)

    /** Maps immutable carton UIDs to their display numbers by current list order (1-based). */
    fun renumber(cartonUidsInOrder: List<String>): Map<String, String> =
        cartonUidsInOrder.mapIndexed { index, uid -> uid to formatCartonNumber(index + 1) }.toMap()

    /** Extracts the numeric part of "CTN-12" / "CTN-012" / "12" for natural ordering. */
    fun cartonSortKey(cartonNumber: String): Long =
        Regex("(\\d+)").findAll(cartonNumber).lastOrNull()?.value?.toLongOrNull() ?: Long.MAX_VALUE

    /**
     * Resolves the shipping mark text for one carton.
     * `{CTN}` placeholders follow the current carton number; all other text is literal.
     */
    fun resolveShippingMark(defaultMark: String?, individualMark: String?, cartonNumber: String): String {
        val source = individualMark?.takeIf { it.isNotBlank() } ?: defaultMark.orEmpty()
        return source.replace("{CTN}", cartonNumber)
    }

    fun hasIndividualMark(individualMark: String?): Boolean = !individualMark.isNullOrBlank()

    private fun orderedByKey(orders: List<PackingOrderLine>): Map<VariantKey, Int> {
        val map = LinkedHashMap<VariantKey, Int>()
        orders.forEach { map[it.key] = (map[it.key] ?: 0) + it.orderedQty.coerceAtLeast(0) }
        return map
    }

    private fun packedByKey(cartons: List<PackingCartonLine>): Map<VariantKey, Int> {
        val map = HashMap<VariantKey, Int>()
        cartons.forEach { map[it.key] = (map[it.key] ?: 0) + it.quantity.coerceAtLeast(0) }
        return map
    }

    fun summarize(orders: List<PackingOrderLine>, cartons: List<PackingCartonLine>): PackingSummary {
        val packed = packedByKey(cartons)
        val ordered = orderedByKey(orders)
        val variants = ordered.map { (key, qty) -> VariantPackingStatus(key, qty, packed[key] ?: 0) }
        return PackingSummary(variants, cartons.map { it.cartonUid }.distinct().size)
    }

    /**
     * Maximum quantity a given carton may hold of [key]:
     * ordered quantity minus allocations in all OTHER cartons.
     * The carton's own current allocation is therefore never counted against itself,
     * so a valid edit is never blocked.
     */
    fun maxAllowedForCarton(
        orders: List<PackingOrderLine>,
        cartons: List<PackingCartonLine>,
        cartonUid: String,
        key: VariantKey
    ): Int {
        val ordered = orderedByKey(orders)[key] ?: 0
        val others = cartons.filter { it.key == key && it.cartonUid != cartonUid }.sumOf { it.quantity.coerceAtLeast(0) }
        return (ordered - others).coerceAtLeast(0)
    }

    /** Variants offered by the picker for a carton, filtered by visibility mode. */
    fun filterByVisibility(statuses: List<VariantPackingStatus>, mode: VariantVisibility): List<VariantPackingStatus> =
        when (mode) {
            VariantVisibility.AVAILABLE -> statuses.filter { it.remaining > 0 }
            VariantVisibility.COMPLETED -> statuses.filter { it.remaining <= 0 }
            VariantVisibility.ALL -> statuses
        }

    /**
     * Hard errors (cannot be saved, even as Draft) plus UNDERPACKED entries
     * (allowed for Draft, blocking for Confirm).
     */
    fun validate(orders: List<PackingOrderLine>, cartons: List<PackingCartonLine>): List<PackingIssue> {
        val issues = mutableListOf<PackingIssue>()
        val ordered = orderedByKey(orders)
        val packed = packedByKey(cartons)

        cartons.groupBy { it.cartonUid }.forEach { (uid, lines) ->
            if (lines.isEmpty()) issues += PackingIssue(PackingIssueType.EMPTY_CARTON, cartonUid = uid)
            lines.groupBy { it.key }.forEach { (key, dup) ->
                if (dup.size > 1) issues += PackingIssue(PackingIssueType.DUPLICATE_VARIANT_IN_CARTON, key, uid)
            }
            lines.forEach { line ->
                if (line.quantity <= 0) issues += PackingIssue(PackingIssueType.INVALID_QUANTITY, line.key, uid)
                if (line.key !in ordered) issues += PackingIssue(PackingIssueType.UNORDERED_VARIANT, line.key, uid)
            }
        }
        ordered.forEach { (key, qty) ->
            val p = packed[key] ?: 0
            if (p > qty) issues += PackingIssue(PackingIssueType.OVERPACKED, key, ordered = qty, packed = p)
        }
        ordered.forEach { (key, qty) ->
            val p = packed[key] ?: 0
            if (p < qty) issues += PackingIssue(PackingIssueType.UNDERPACKED, key, ordered = qty, packed = p)
        }
        return issues
    }

    /** Issues that block saving even as a Draft. */
    fun draftBlockers(orders: List<PackingOrderLine>, cartons: List<PackingCartonLine>): List<PackingIssue> =
        validate(orders, cartons).filter { it.isBlockingForDraft }

    /** Issues that block confirmation: every variant must be exactly fully allocated. */
    fun confirmBlockers(orders: List<PackingOrderLine>, cartons: List<PackingCartonLine>): List<PackingIssue> =
        validate(orders, cartons)

    /** Unallocated quantity for the Draft warning ("500 PCS remain unallocated"). */
    fun unallocatedQuantity(orders: List<PackingOrderLine>, cartons: List<PackingCartonLine>): Int =
        summarize(orders, cartons).variants.sumOf { it.remaining.coerceAtLeast(0) }

    /**
     * Option A: never reduce an ordered quantity below what is already allocated to cartons.
     */
    fun checkOrderQuantityChange(
        key: VariantKey,
        newQuantity: Int,
        cartons: List<PackingCartonLine>
    ): QuantityChangeCheck {
        val packed = cartons.filter { it.key == key }.sumOf { it.quantity.coerceAtLeast(0) }
        return QuantityChangeCheck(allowed = newQuantity >= packed, packed = packed)
    }

    /**
     * Largest number of identical cartons that can be created from [perCartonLines]
     * without overpacking any variant (each entry: key -> pcs per carton).
     */
    fun maxIdenticalCartons(
        orders: List<PackingOrderLine>,
        cartons: List<PackingCartonLine>,
        perCartonLines: Map<VariantKey, Int>
    ): Int {
        if (perCartonLines.isEmpty() || perCartonLines.values.any { it <= 0 }) return 0
        val summary = summarize(orders, cartons).variants.associateBy { it.key }
        return perCartonLines.minOf { (key, perCarton) ->
            val remaining = summary[key]?.remaining?.coerceAtLeast(0) ?: 0
            remaining / perCarton
        }
    }

    private inline fun updateProductVariantField(
        lines: List<PoOrderItemGroupLine>,
        targetIndex: Int,
        newValue: String,
        getValue: (PoOrderItemGroupLine) -> String,
        isOverridden: (PoOrderItemGroupLine) -> Boolean,
        copyLine: (PoOrderItemGroupLine, value: String, override: Boolean) -> PoOrderItemGroupLine
    ): List<PoOrderItemGroupLine> {
        val target = lines.getOrNull(targetIndex) ?: return lines
        val firstIdx = lines.indexOfFirst { it.productId == target.productId }
        if (firstIdx < 0) return lines
        val firstValue = getValue(lines[firstIdx])
        return lines.mapIndexed { idx, line ->
            when {
                line.productId != target.productId -> line
                targetIndex == firstIdx && idx == firstIdx ->
                    copyLine(line, newValue, isOverridden(line))
                targetIndex == firstIdx && !isOverridden(line) ->
                    copyLine(line, newValue, false)
                targetIndex != firstIdx && idx == targetIndex ->
                    copyLine(line, newValue, newValue.isNotBlank() && newValue != firstValue)
                else -> line
            }
        }
    }

    /**
     * Updates a variant's quantity in [lines]; editing the 1st variant of a product propagates
     * the value to all other variants of the same product without a manual override.
     */
    fun updateProductVariantQuantity(
        lines: List<PoOrderItemGroupLine>,
        targetIndex: Int,
        newQuantityStr: String
    ): List<PoOrderItemGroupLine> = updateProductVariantField(
        lines = lines,
        targetIndex = targetIndex,
        newValue = newQuantityStr,
        getValue = { it.quantityStr },
        isOverridden = { it.manualQuantityOverride },
        copyLine = { line, value, override ->
            line.copy(quantityStr = value, manualQuantityOverride = override)
        }
    )

    /**
     * Updates a variant's unit price in [lines]; editing the 1st variant of a product propagates
     * the price to all other variants of the same product without a manual override.
     */
    fun updateProductVariantPrice(
        lines: List<PoOrderItemGroupLine>,
        targetIndex: Int,
        newPriceStr: String
    ): List<PoOrderItemGroupLine> = updateProductVariantField(
        lines = lines,
        targetIndex = targetIndex,
        newValue = newPriceStr,
        getValue = { it.unitPriceStr },
        isOverridden = { it.manualPriceOverride },
        copyLine = { line, value, override ->
            line.copy(unitPriceStr = value, manualPriceOverride = override)
        }
    )

    /** Computes total ordered quantity and total amount in PO currency for a single product group. */
    fun summarizeProductGroup(
        lines: List<PoOrderItemGroupLine>,
        productId: String
    ): PoProductGroupSummary {
        val group = lines.filter { it.productId == productId }
        val totalQuantity = group.sumOf { (it.quantityStr.trim().toIntOrNull() ?: 0).coerceAtLeast(0) }
        val totalAmountMajor = group.fold(BigDecimal.ZERO) { acc, item ->
            val q = BigDecimal((item.quantityStr.trim().toIntOrNull() ?: 0).coerceAtLeast(0))
            val p = (item.unitPriceStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO).coerceAtLeast(BigDecimal.ZERO)
            acc.add(q.multiply(p))
        }.setScale(2, RoundingMode.HALF_UP)
        return PoProductGroupSummary(
            productId = productId,
            variantCount = group.size,
            totalQuantity = totalQuantity,
            totalAmountMajor = totalAmountMajor
        )
    }
}
