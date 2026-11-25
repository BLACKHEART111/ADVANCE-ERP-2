package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.CustomerLedgerAdjustment
import com.pocketdev.advance_erp.core.engine.LedgerReportEngine
import com.pocketdev.advance_erp.core.engine.PaymentAllocationValidation
import com.pocketdev.advance_erp.core.engine.SupplierLedgerAdjustment
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

    @Test fun customerOpeningDueDebitAdjustmentProducesZeroLegacyResidualAndReducesOnStandaloneCollection() {
        val openingEntry = JournalEntry(
            id = "je-open-c",
            entryNumber = "JE-2026-0001",
            entryDate = 1000L,
            descriptionEn = "Opening Due for Customer",
            descriptionBn = "পূর্বের বকেয়া",
            referenceType = CustomerLedgerAdjustment.DEBIT,
            referenceId = "c",
            totalDebitMinor = 1500000L,
            totalCreditMinor = 1500000L
        )
        val delta = CustomerLedgerAdjustment.delta(openingEntry)
        assertEquals(1500000L, delta)

        val openingRow = UnifiedTransactionItem(
            id = openingEntry.id,
            referenceNumber = openingEntry.entryNumber,
            date = openingEntry.entryDate,
            category = ReportCategory.CUSTOMER_LEDGER,
            primaryEntityName = "Rahim",
            primaryEntityId = "c",
            particulars = openingEntry.descriptionEn,
            amountMinor = delta,
            isInflow = true,
            status = "POSTED",
            rawType = "CUSTOMER_ADJUSTMENT",
            effectSign = 1
        )
        val collectionRow = UnifiedTransactionItem(
            id = "p-open",
            referenceNumber = "CP-001",
            date = 2000L,
            category = ReportCategory.RECEIPT,
            primaryEntityName = "Rahim",
            primaryEntityId = "c",
            particulars = "Opening due partial collection",
            amountMinor = 500000L,
            isInflow = true,
            status = "POSTED",
            rawType = "PAYMENT"
        )
        val filter = ControlCenterFilterState(
            category = ReportCategory.CUSTOMER_LEDGER,
            selectedCustomerId = "c",
            dateRangeOption = DateRangeOption.ALL_TIME,
            sortBy = SortOption.OLDEST_FIRST
        )
        val report = LedgerReportEngine.build(
            listOf(collectionRow, openingRow),
            filter,
            LedgerParty("c", "Rahim", "CUST-01", "CUSTOMER", "017", "Dhaka")
        )
        assertEquals(1000000L, report.statement!!.closingBalanceMinor)
        assertEquals(1500000L, report.transactions[0].ledgerDebitMinor)
        assertEquals(500000L, report.transactions[1].ledgerCreditMinor)
        assertEquals(1000000L, report.transactions[1].runningBalanceMinor)
        assertEquals(0L, report.legacyOpeningMinor)
    }

    @Test fun supplierOpeningPayableCreditAdjustmentProducesBalancedSupplierStatement() {
        val creditEntry = JournalEntry(
            id = "je-open-s",
            entryNumber = "JE-2026-0002",
            entryDate = 1000L,
            descriptionEn = "Opening Payable for Supplier",
            descriptionBn = "সরবরাহকারীর পূর্বের বকেয়া",
            referenceType = SupplierLedgerAdjustment.CREDIT,
            referenceId = "s",
            totalDebitMinor = 5000000L,
            totalCreditMinor = 5000000L
        )
        assertEquals(5000000L, SupplierLedgerAdjustment.delta(creditEntry))
        assertEquals(-5000000L, SupplierLedgerAdjustment.delta(creditEntry.copy(referenceType = SupplierLedgerAdjustment.DEBIT)))
        assertEquals(0L, SupplierLedgerAdjustment.delta(creditEntry.copy(isReversed = true)))
        assertEquals(0L, SupplierLedgerAdjustment.delta(creditEntry.copy(isPosted = false)))

        val openingRow = UnifiedTransactionItem(
            id = creditEntry.id,
            referenceNumber = creditEntry.entryNumber,
            date = creditEntry.entryDate,
            category = ReportCategory.SUPPLIER_LEDGER,
            primaryEntityName = "Guangzhou Co.",
            primaryEntityId = "s",
            particulars = creditEntry.descriptionEn,
            amountMinor = 5000000L,
            isInflow = false,
            status = "POSTED",
            rawType = "SUPPLIER_ADJUSTMENT",
            effectSign = 1
        )
        val paymentRow = UnifiedTransactionItem(
            id = "sp-1",
            referenceNumber = "SP-2026-0001",
            date = 2000L,
            category = ReportCategory.PAYMENT,
            primaryEntityName = "Guangzhou Co.",
            primaryEntityId = "s",
            particulars = "Partial settlement of opening payable",
            amountMinor = 2000000L,
            isInflow = false,
            status = "PAID",
            rawType = "SUPPLIER_PAYMENT"
        )
        val filter = ControlCenterFilterState(
            category = ReportCategory.SUPPLIER_LEDGER,
            selectedSupplierId = "s",
            dateRangeOption = DateRangeOption.ALL_TIME,
            sortBy = SortOption.OLDEST_FIRST
        )
        val report = LedgerReportEngine.build(
            listOf(paymentRow, openingRow),
            filter,
            LedgerParty("s", "Guangzhou Co.", "SUP-01", "SUPPLIER", "018", "China")
        )
        assertEquals(3000000L, report.statement!!.closingBalanceMinor)
        assertEquals(5000000L, report.transactions[0].ledgerCreditMinor)
        assertEquals(2000000L, report.transactions[1].ledgerDebitMinor)
        assertEquals(3000000L, report.transactions[1].runningBalanceMinor)
        assertEquals(0L, report.legacyOpeningMinor)
    }

    @Test fun paymentSplitSettlesOpeningReceivableBeforeCreatingCustomerAdvance() {
        // 1. Standalone partial collection against ৳15,000 opening due -> full ৳5,000 reduces AR (1040), ৳0 advance (2030)
        assertEquals(
            500000L to 0L,
            PaymentAllocationValidation.splitPaymentIntoReceivableAndAdvance(
                paymentMinor = 500000L,
                explicitAllocatedMinor = 0L,
                prePaymentReceivableMinor = 1500000L
            )
        )
        // 2. Overpayment: ৳18,000 paid against ৳15,000 opening due -> ৳15,000 settles 1040, ৳3,000 becomes 2030 advance
        assertEquals(
            1500000L to 300000L,
            PaymentAllocationValidation.splitPaymentIntoReceivableAndAdvance(
                paymentMinor = 1800000L,
                explicitAllocatedMinor = 0L,
                prePaymentReceivableMinor = 1500000L
            )
        )
        // 3. Pure advance when customer has ৳0 due -> ৳0 AR, full ৳10,000 goes to 2030 advance
        assertEquals(
            0L to 1000000L,
            PaymentAllocationValidation.splitPaymentIntoReceivableAndAdvance(
                paymentMinor = 1000000L,
                explicitAllocatedMinor = 0L,
                prePaymentReceivableMinor = 0L
            )
        )
    }
}
