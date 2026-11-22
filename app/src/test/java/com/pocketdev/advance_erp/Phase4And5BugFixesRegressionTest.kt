package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.PaymentAllocationValidation
import com.pocketdev.advance_erp.core.util.ControlCenterExcelGenerator
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.repository.formatDocumentNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Locale

class Phase4And5BugFixesRegressionTest {

    @Test
    fun customerCreditReturnReducesMasterDueWithoutGhostOpeningBalance() {
        // Reproduces Metro Fashion House: ৳260 + ৳6,280 - ৳2,000 = ৳4,540 before return, - ৳1,560 return -> ৳2,980
        val updatedDue = PaymentAllocationValidation.outstandingAfterTransaction(
            masterDueBefore = 454_000L,
            transactionNetBefore = 454_000L,
            transactionNetAfter = 298_000L
        )
        assertEquals(298_000L, updatedDue)
    }

    @Test
    fun genuineOpeningBalanceSurvivesReturnsVoidsCancelsAndNewInvoices() {
        // Master due 554,000 includes 100,000 genuine opening balance + 454,000 ledger net
        assertEquals(
            100_000L,
            PaymentAllocationValidation.computePreservedOpeningBalance(554_000L, 454_000L)
        )
        // After 156,000 customer-credit return (net = 298,000) -> 398,000
        assertEquals(
            398_000L,
            PaymentAllocationValidation.outstandingAfterTransaction(554_000L, 454_000L, 298_000L)
        )
        // After voiding a 150,000 payment (net = 448,000) -> 548,000
        assertEquals(
            548_000L,
            PaymentAllocationValidation.outstandingAfterTransaction(398_000L, 298_000L, 448_000L)
        )
        // After cancelling a 628,000 invoice with 200,000 cash paid -> advance credit clamps safely to 0
        assertEquals(
            0L,
            PaymentAllocationValidation.outstandingAfterTransaction(428_000L, 428_000L, -200_000L)
        )
    }

    @Test
    fun normalizeAsciiNumberInputConvertsBanglaDigitsAndSeparators() {
        assertEquals("4800", Formatters.normalizeAsciiNumberInput("৪৮০০"))
        assertEquals("4800.50", Formatters.normalizeAsciiNumberInput("৪,৮০০.৫০"))
        assertEquals("350.00", Formatters.normalizeAsciiNumberInput(" 350.00 "))
        val parsed = Formatters.normalizeAsciiNumberInput("৪৮০০").toBigDecimalOrNull()
        assertEquals(0, java.math.BigDecimal("4800").compareTo(parsed))
    }

    @Test
    fun sanitizeExportFileSlugAvoidsUnderscoreOnlyNamesForBanglaTitles() {
        val bnOnly = ControlCenterExcelGenerator.sanitizeExportFileSlug(
            reportTitle = "গ্রাহক খতিয়ান ও রিপোর্ট",
            fallback = "customer_ledger"
        )
        assertEquals("customer_ledger", bnOnly)
        assertFalse(bnOnly.all { it == '_' })

        assertEquals(
            "party_statement_2026",
            ControlCenterExcelGenerator.sanitizeExportFileSlug("Party Statement 2026!", "fallback")
        )
        assertEquals(
            "rahman_mart",
            ControlCenterExcelGenerator.sanitizeExportFileSlug("গ্রাহক Rahman Mart বিবরণী", "customer_ledger")
        )
    }

    @Test
    fun financialDocumentNumbersAlwaysUseAsciiDigitsUnderBanglaLocale() {
        val prev = Locale.getDefault()
        try {
            Locale.setDefault(Locale("bn", "BD"))
            assertEquals("EXP-20261123-0001", formatDocumentNumber("EXP", "20261123", 1))
            assertEquals("TRF-20261123-0001", formatDocumentNumber("TRF", "20261123", 1))
            assertEquals("BRC-202611-0007", formatDocumentNumber("BRC", "202611", 7))
        } finally {
            Locale.setDefault(prev)
        }
    }
}
