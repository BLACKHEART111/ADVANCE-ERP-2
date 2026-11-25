package com.pocketdev.advance_erp.core.engine

import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Deterministic proportional allocator for integer minor-unit BDT amounts.
 * Guarantees exact reconciliation: sum(allocated.values) == totalAmountMinor.
 */
object DeterministicAllocation {

    fun allocateProportionally(
        totalAmountMinor: Long,
        weights: List<Pair<String, BigDecimal>>
    ): Map<String, Long> {
        if (weights.isEmpty()) return emptyMap()
        if (totalAmountMinor == 0L) {
            return weights.associate { it.first to 0L }
        }
        if (totalAmountMinor < 0L) {
            return allocateProportionally(-totalAmountMinor, weights).mapValues { -it.value }
        }

        val sanitized = weights.map { (key, weight) ->
            key to if (weight > BigDecimal.ZERO) weight else BigDecimal.ZERO
        }
        val totalPositiveWeight = sanitized.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.second) }
        val effectiveWeights = if (totalPositiveWeight > BigDecimal.ZERO) {
            sanitized
        } else {
            weights.map { it.first to BigDecimal.ONE }
        }
        val denominator = effectiveWeights.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.second) }

        val totalBd = BigDecimal(totalAmountMinor)
        data class IndexedRemainder(
            val key: String,
            val floorValue: Long,
            val remainder: BigDecimal
        )

        val interim = effectiveWeights.map { (key, weight) ->
            val exact = totalBd.multiply(weight).divide(denominator, 12, RoundingMode.DOWN)
            val floorVal = exact.setScale(0, RoundingMode.DOWN).longValueExact()
            val rem = exact.subtract(BigDecimal(floorVal))
            IndexedRemainder(key = key, floorValue = floorVal, remainder = rem)
        }

        val allocatedSum = interim.sumOf { it.floorValue }
        var residual = totalAmountMinor - allocatedSum

        val result = interim.associate { it.key to it.floorValue }.toMutableMap()
        if (residual > 0L) {
            val sortedForResidual = interim.sortedWith(
                compareByDescending<IndexedRemainder> { it.remainder }
                    .thenBy { it.key }
            )
            var idx = 0
            while (residual > 0L) {
                val target = sortedForResidual[idx % sortedForResidual.size]
                result[target.key] = (result[target.key] ?: 0L) + 1L
                residual--
                idx++
            }
        }
        return result
    }

    /**
     * Policy Gate #4(b): Allocates invoice-wide discount proportionally across invoice lines
     * by Eligible Line Net Value = max(0, quantity * unitPriceMinor - lineDiscountMinor).
     */
    fun allocateInvoiceDiscountAcrossItems(
        invoiceDiscountMinor: Long,
        items: List<SalesInvoiceItem>
    ): Map<String, Long> {
        if (items.isEmpty()) return emptyMap()
        val eligibleTotal = items.sumOf { eligibleLineNetValue(it) }
        val cappedDiscount = minOf(maxOf(0L, invoiceDiscountMinor), eligibleTotal)
        if (cappedDiscount == 0L) return items.associate { it.id to 0L }

        val weights = items.map { item ->
            item.id to BigDecimal(eligibleLineNetValue(item))
        }
        return allocateProportionally(cappedDiscount, weights)
    }

    fun eligibleLineNetValue(item: SalesInvoiceItem): Long {
        val gross = item.quantity.toLong() * item.unitPriceMinor
        return maxOf(0L, gross - item.discountMinor)
    }
}
