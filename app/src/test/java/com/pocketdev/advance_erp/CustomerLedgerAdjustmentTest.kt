package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.CustomerLedgerAdjustment
import com.pocketdev.advance_erp.core.engine.LedgerReportEngine
import com.pocketdev.advance_erp.data.entity.JournalEntry
import com.pocketdev.advance_erp.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class CustomerLedgerAdjustmentTest {
    private fun journal() = JournalEntry("adj", "ADJ", 2000, "First order discount", "ছাড়",
        CustomerLedgerAdjustment.CREDIT, "c", 24000, 24000)

    @Test fun discountIsDatedCreditAndDoesNotBecomePayment() {
        assertEquals(-24000L, CustomerLedgerAdjustment.delta(journal()))
        val invoice = UnifiedTransactionItem("i", "INV", 1000, ReportCategory.SALES, "Customer", "c",
            "Invoice", 2024000, isInflow = true, status = "POSTED", rawType = "INVOICE")
        val adjustment = UnifiedTransactionItem("a", "ADJ", 2000, ReportCategory.CUSTOMER_LEDGER, "Customer", "c",
            "First order discount", 24000, isInflow = false, status = "POSTED", rawType = "CUSTOMER_ADJUSTMENT", effectSign = -1)
        val payment = UnifiedTransactionItem("p", "PAY", 3000, ReportCategory.RECEIPT, "Customer", "c",
            "Payment", 2000000, isInflow = true, status = "POSTED", rawType = "PAYMENT")
        val filter = ControlCenterFilterState(category = ReportCategory.CUSTOMER_LEDGER,
            selectedCustomerId = "c", dateRangeOption = DateRangeOption.ALL_TIME, sortBy = SortOption.OLDEST_FIRST)
        val report = LedgerReportEngine.build(listOf(payment, adjustment, invoice), filter,
            LedgerParty("c", "Customer", "C", "CUSTOMER", "", ""))
        assertEquals(listOf("i", "a", "p"), report.transactions.map { it.id })
        assertEquals(0L, report.statement!!.closingBalanceMinor)
        assertEquals(24000L, report.transactions[1].ledgerCreditMinor)
        assertEquals(0L, report.legacyOpeningMinor)
    }

    @Test fun unpostedOrReversedAdjustmentHasNoEffect() {
        assertEquals(0L, CustomerLedgerAdjustment.delta(journal().copy(isPosted = false)))
        assertEquals(0L, CustomerLedgerAdjustment.delta(journal().copy(isReversed = true)))
        assertEquals(24000L, CustomerLedgerAdjustment.delta(journal().copy(referenceType = CustomerLedgerAdjustment.DEBIT)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unbalancedAdjustmentIsRejected() {
        CustomerLedgerAdjustment.delta(journal().copy(totalCreditMinor = 0))
    }
}
