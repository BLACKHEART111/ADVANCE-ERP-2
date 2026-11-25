package com.pocketdev.advance_erp.core.engine

import com.pocketdev.advance_erp.core.util.Formatters
import java.math.BigDecimal
import java.math.RoundingMode

data class LineCalculation(
    val quantity: Int,
    val unitPriceMinor: Long,
    val grossMinor: Long,
    val discountMinor: Long,
    val netTotalMinor: Long,
    val unitCostMinor: Long,
    val totalCostMinor: Long,
    val profitMinor: Long,
    val profitMarginPercent: Double
)

data class InvoiceTotalsCalculation(
    val subtotalMinor: Long,
    val invoiceDiscountMinor: Long,
    val additionalChargesMinor: Long,
    val grandTotalMinor: Long,
    val paidMinor: Long,
    val dueMinor: Long,
    val totalCostMinor: Long,
    val estimatedProfitMinor: Long,
    val estimatedProfitMarginPercent: Double
)

/**
 * Pure financial calculator for New Sales Invoice pricing, item/invoice % and fixed ৳ discounts,
 * additional charges, settlement splitting, and live cost/profit margin metrics.
 */
object SalesInvoiceCalculator {

    private val BASIS_DIVISOR = BigDecimal(10_000L)

    fun parseMajorToMinor(raw: String): Long {
        val cleaned = Formatters.normalizeAsciiNumberInput(raw)
        if (cleaned.isBlank() || cleaned == "." || cleaned == "-") return 0L
        return runCatching {
            val bd = BigDecimal(cleaned)
            if (bd < BigDecimal.ZERO) 0L
            else bd.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        }.getOrDefault(0L)
    }

    /** Parses a percentage string (e.g. "12.5") into clamped basis points (0..10_000 = 0.00%..100.00%). */
    fun parsePercentBasisPoints(raw: String): Long {
        val cleaned = Formatters.normalizeAsciiNumberInput(raw)
        if (cleaned.isBlank() || cleaned == "." || cleaned == "-") return 0L
        return runCatching {
            val bd = BigDecimal(cleaned)
            val bps = bd.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
            bps.coerceIn(0L, 10_000L)
        }.getOrDefault(0L)
    }

    fun formatMajorEditable(minorUnits: Long): String {
        val safe = maxOf(0L, minorUnits)
        return BigDecimal.valueOf(safe, 2).setScale(2, RoundingMode.HALF_UP).toPlainString()
    }

    fun computeLineAmounts(
        quantity: Int,
        unitPriceMinor: Long,
        discountInput: String,
        isPercentDiscount: Boolean,
        unitCostMinor: Long = 0L
    ): LineCalculation {
        val safeQty = maxOf(0, quantity)
        val safePrice = maxOf(0L, unitPriceMinor)
        val safeCostPerUnit = maxOf(0L, unitCostMinor)
        val grossMinor = safeQty.toLong() * safePrice

        val discountMinor = if (grossMinor <= 0L) {
            0L
        } else if (isPercentDiscount) {
            val bps = parsePercentBasisPoints(discountInput)
            BigDecimal.valueOf(grossMinor)
                .multiply(BigDecimal.valueOf(bps))
                .divide(BASIS_DIVISOR, 0, RoundingMode.HALF_UP)
                .toLong()
                .coerceIn(0L, grossMinor)
        } else {
            parseMajorToMinor(discountInput).coerceIn(0L, grossMinor)
        }

        val netTotalMinor = (grossMinor - discountMinor).coerceAtLeast(0L)
        val totalCostMinor = safeQty.toLong() * safeCostPerUnit
        val profitMinor = netTotalMinor - totalCostMinor
        val marginPercent = if (netTotalMinor > 0L) {
            (profitMinor.toDouble() * 100.0) / netTotalMinor.toDouble()
        } else {
            0.0
        }

        return LineCalculation(
            quantity = safeQty,
            unitPriceMinor = safePrice,
            grossMinor = grossMinor,
            discountMinor = discountMinor,
            netTotalMinor = netTotalMinor,
            unitCostMinor = safeCostPerUnit,
            totalCostMinor = totalCostMinor,
            profitMinor = profitMinor,
            profitMarginPercent = marginPercent
        )
    }

    fun computeInvoiceTotals(
        lines: List<LineCalculation>,
        invoiceDiscountInput: String,
        isInvoicePercentDiscount: Boolean,
        additionalChargesInput: String,
        paidAmountInput: String,
        isFullPay: Boolean,
        isCreditMethod: Boolean
    ): InvoiceTotalsCalculation {
        val subtotalMinor = lines.fold(0L) { acc, line -> acc + line.netTotalMinor }
        val invoiceDiscountMinor = if (subtotalMinor <= 0L) {
            0L
        } else if (isInvoicePercentDiscount) {
            val bps = parsePercentBasisPoints(invoiceDiscountInput)
            BigDecimal.valueOf(subtotalMinor)
                .multiply(BigDecimal.valueOf(bps))
                .divide(BASIS_DIVISOR, 0, RoundingMode.HALF_UP)
                .toLong()
                .coerceIn(0L, subtotalMinor)
        } else {
            parseMajorToMinor(invoiceDiscountInput).coerceIn(0L, subtotalMinor)
        }

        val additionalChargesMinor = parseMajorToMinor(additionalChargesInput)
        val grandTotalMinor = (subtotalMinor - invoiceDiscountMinor + additionalChargesMinor).coerceAtLeast(0L)

        val paidMinor = when {
            isCreditMethod -> 0L
            isFullPay -> grandTotalMinor
            else -> parseMajorToMinor(paidAmountInput).coerceIn(0L, grandTotalMinor)
        }
        val dueMinor = (grandTotalMinor - paidMinor).coerceAtLeast(0L)
        val totalCostMinor = lines.fold(0L) { acc, line -> acc + line.totalCostMinor }
        val estimatedProfitMinor = grandTotalMinor - totalCostMinor
        val estimatedProfitMarginPercent = if (grandTotalMinor > 0L) {
            (estimatedProfitMinor.toDouble() * 100.0) / grandTotalMinor.toDouble()
        } else {
            0.0
        }

        return InvoiceTotalsCalculation(
            subtotalMinor = subtotalMinor,
            invoiceDiscountMinor = invoiceDiscountMinor,
            additionalChargesMinor = additionalChargesMinor,
            grandTotalMinor = grandTotalMinor,
            paidMinor = paidMinor,
            dueMinor = dueMinor,
            totalCostMinor = totalCostMinor,
            estimatedProfitMinor = estimatedProfitMinor,
            estimatedProfitMarginPercent = estimatedProfitMarginPercent
        )
    }
}
