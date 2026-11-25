package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.SalesInvoiceCalculator
import com.pocketdev.advance_erp.core.security.SecurityManager
import com.pocketdev.advance_erp.core.security.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SalesInvoiceRedesignCalculationsTest {

    @Test
    fun referenceInvoiceCalculationMatchesSummaryAndProfitExactly() {
        val item1 = SalesInvoiceCalculator.computeLineAmounts(
            quantity = 15,
            unitPriceMinor = 13000L, // ৳130.00
            discountInput = "0.00",
            isPercentDiscount = true,
            unitCostMinor = 6507L // ৳65.07/pc
        )
        assertEquals(195000L, item1.netTotalMinor)
        assertEquals(97395L, item1.profitMinor) // ৳973.95
        assertEquals(49.9, item1.profitMarginPercent, 0.05)

        val item2 = SalesInvoiceCalculator.computeLineAmounts(
            quantity = 15,
            unitPriceMinor = 13000L,
            discountInput = "0.00",
            isPercentDiscount = true,
            unitCostMinor = 6507L
        )
        assertEquals(195000L, item2.netTotalMinor)

        val item3 = SalesInvoiceCalculator.computeLineAmounts(
            quantity = 1,
            unitPriceMinor = 0L,
            discountInput = "0.00",
            isPercentDiscount = true,
            unitCostMinor = 82067L // ৳820.67
        )
        assertEquals(0L, item3.netTotalMinor)

        val totals = SalesInvoiceCalculator.computeInvoiceTotals(
            lines = listOf(item1, item2, item3),
            invoiceDiscountInput = "0.00",
            isInvoicePercentDiscount = true,
            additionalChargesInput = "0.00",
            paidAmountInput = "0.00",
            isFullPay = false,
            isCreditMethod = false
        )
        assertEquals(390000L, totals.subtotalMinor)
        assertEquals(390000L, totals.grandTotalMinor)
        assertEquals(0L, totals.paidMinor)
        assertEquals(390000L, totals.dueMinor)
        assertEquals(112723L, totals.estimatedProfitMinor) // ৳1,127.23
        assertEquals(28.9, totals.estimatedProfitMarginPercent, 0.05)
    }

    @Test
    fun percentageAndFixedDiscountsWithChargesAndFullPaySettleCleanly() {
        // Item 1: qty 4 @ ৳250 = ৳1,000, 12.5% discount = ৳125 -> ৳875.00
        val line1 = SalesInvoiceCalculator.computeLineAmounts(
            quantity = 4,
            unitPriceMinor = 25000L,
            discountInput = "12.5",
            isPercentDiscount = true,
            unitCostMinor = 15000L
        )
        assertEquals(12500L, line1.discountMinor)
        assertEquals(87500L, line1.netTotalMinor)

        // Item 2: qty 2 @ ৳300 = ৳600, fixed discount ৳75 -> ৳525.00
        val line2 = SalesInvoiceCalculator.computeLineAmounts(
            quantity = 2,
            unitPriceMinor = 30000L,
            discountInput = "75.00",
            isPercentDiscount = false,
            unitCostMinor = 20000L
        )
        assertEquals(7500L, line2.discountMinor)
        assertEquals(52500L, line2.netTotalMinor)

        // Invoice subtotal = 875 + 525 = 1,400.00; 10% invoice discount = ৳140; add charges = ৳60
        // Grand total = 1,400 - 140 + 60 = 1,320.00
        val fullPayTotals = SalesInvoiceCalculator.computeInvoiceTotals(
            lines = listOf(line1, line2),
            invoiceDiscountInput = "10",
            isInvoicePercentDiscount = true,
            additionalChargesInput = "60.00",
            paidAmountInput = "500.00",
            isFullPay = true,
            isCreditMethod = false
        )
        assertEquals(140000L, fullPayTotals.subtotalMinor)
        assertEquals(14000L, fullPayTotals.invoiceDiscountMinor)
        assertEquals(6000L, fullPayTotals.additionalChargesMinor)
        assertEquals(132000L, fullPayTotals.grandTotalMinor)
        assertEquals(132000L, fullPayTotals.paidMinor)
        assertEquals(0L, fullPayTotals.dueMinor)

        // Credit payment forces paid = 0 regardless of paidAmountInput or isFullPay
        val creditTotals = SalesInvoiceCalculator.computeInvoiceTotals(
            lines = listOf(line1, line2),
            invoiceDiscountInput = "10",
            isInvoicePercentDiscount = true,
            additionalChargesInput = "60.00",
            paidAmountInput = "1320.00",
            isFullPay = true,
            isCreditMethod = true
        )
        assertEquals(0L, creditTotals.paidMinor)
        assertEquals(132000L, creditTotals.dueMinor)
    }

    @Test
    fun excessDiscountsAreClampedSafelyAndBanglaDigitsNormalize() {
        val clampedItem = SalesInvoiceCalculator.computeLineAmounts(
            quantity = 2,
            unitPriceMinor = 5000L,
            discountInput = "২৫০", // > 100% and > gross
            isPercentDiscount = true,
            unitCostMinor = 3000L
        )
        assertEquals(10000L, clampedItem.discountMinor)
        assertEquals(0L, clampedItem.netTotalMinor)

        val fixedClamped = SalesInvoiceCalculator.computeLineAmounts(
            quantity = 2,
            unitPriceMinor = 5000L,
            discountInput = "500",
            isPercentDiscount = false,
            unitCostMinor = 3000L
        )
        assertEquals(10000L, fixedClamped.discountMinor)
        assertEquals(0L, fixedClamped.netTotalMinor)
    }

    @Test
    fun costAndProfitVisibilityRemainsRestrictedAndHistoricalDueIsolated() {
        assertTrue(SecurityManager.canViewCost(UserRole.OWNER_ADMIN))
        assertTrue(SecurityManager.canViewProfit(UserRole.OWNER_ADMIN))

        assertTrue(SecurityManager.canViewCost(UserRole.MANAGER))
        assertFalse(SecurityManager.canViewProfit(UserRole.MANAGER))

        assertFalse(SecurityManager.canViewCost(UserRole.SALES_OPERATOR))
        assertFalse(SecurityManager.canViewProfit(UserRole.SALES_OPERATOR))

        val line = SalesInvoiceCalculator.computeLineAmounts(
            quantity = 1,
            unitPriceMinor = 100000L,
            discountInput = "0",
            isPercentDiscount = false,
            unitCostMinor = 60000L
        )
        val totals = SalesInvoiceCalculator.computeInvoiceTotals(
            lines = listOf(line),
            invoiceDiscountInput = "0",
            isInvoicePercentDiscount = false,
            additionalChargesInput = "0",
            paidAmountInput = "400.00",
            isFullPay = false,
            isCreditMethod = false
        )
        // Customer historical due (e.g. ৳17,455.00) does NOT enter current invoice due calculation:
        val customerHistoricalDueMinor = 1745500L
        assertEquals(60000L, totals.dueMinor)
        assertFalse(totals.dueMinor == totals.dueMinor + customerHistoricalDueMinor)
    }
}
