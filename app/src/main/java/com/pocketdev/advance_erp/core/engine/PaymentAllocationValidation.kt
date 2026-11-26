package com.pocketdev.advance_erp.core.engine

import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.domain.repository.CustomerPaymentDraft

object PaymentAllocationValidation {
    data class InvoiceSettlementState(
        val paidAmountMinor: Long,
        val creditReturnMinor: Long,
        val dueAmountMinor: Long,
        val status: String
    )

    fun computeInvoiceSettlement(
        totalAmountMinor: Long,
        activeAllocatedMinor: Long,
        creditReturnMinor: Long,
        isPosted: Boolean,
        currentStatus: String
    ): InvoiceSettlementState {
        if (currentStatus == "CANCELLED") {
            return InvoiceSettlementState(0L, creditReturnMinor.coerceAtLeast(0L), 0L, "CANCELLED")
        }
        val paid = activeAllocatedMinor.coerceAtLeast(0L)
        val credRet = creditReturnMinor.coerceAtLeast(0L)
        val due = (totalAmountMinor - paid - credRet).coerceAtLeast(0L)
        val status = when {
            !isPosted -> currentStatus
            due == 0L && (totalAmountMinor > 0L || paid > 0L || credRet > 0L) -> "PAID"
            paid > 0L -> "PARTIALLY_PAID"
            else -> if (currentStatus in setOf("DELIVERED", "PARTIALLY_DELIVERED", "CONFIRMED")) currentStatus else "POSTED"
        }
        return InvoiceSettlementState(paid, credRet, due, status)
    }

    fun outstandingAfterPayment(masterDue: Long, transactionNet: Long, payment: Long): Long {
        require(payment > 0) { "Payment amount must be positive" }
        val signedBefore = if (masterDue != 0L) masterDue else transactionNet
        return Math.subtractExact(signedBefore, payment).coerceAtLeast(0L)
    }

    /** Preserves genuine opening balance (positive due or explicit negative credit) while isolating ledger net. */
    fun computePreservedOpeningBalance(masterDueBefore: Long, transactionNetBefore: Long): Long =
        when {
            masterDueBefore > 0L -> (masterDueBefore - transactionNetBefore).coerceAtLeast(0L)
            masterDueBefore < 0L -> masterDueBefore - transactionNetBefore
            else -> 0L
        }

    /** Recomputes customer master due after posting, returning, voiding, or cancelling. */
    fun outstandingAfterTransaction(
        masterDueBefore: Long,
        transactionNetBefore: Long,
        transactionNetAfter: Long
    ): Long {
        val preservedOpening = computePreservedOpeningBalance(masterDueBefore, transactionNetBefore)
        val raw = Math.addExact(preservedOpening, transactionNetAfter)
        return if (masterDueBefore < 0L && raw < 0L) raw else raw.coerceAtLeast(0L)
    }

    /**
     * Splits a customer receipt into Accounts Receivable (1040) settlement vs Customer Advance (2030).
     * Any payment up to the customer's pre-payment receivable settles 1040 (including opening dues
     * and standalone due collections); only overpayment above pre-payment receivable becomes 2030 advance.
     */
    fun splitPaymentIntoReceivableAndAdvance(
        paymentMinor: Long,
        explicitAllocatedMinor: Long,
        prePaymentReceivableMinor: Long
    ): Pair<Long, Long> {
        require(paymentMinor > 0L) { "Payment amount must be positive" }
        require(explicitAllocatedMinor in 0L..paymentMinor) { "Explicit invoice allocation out of bounds" }
        val receivableFloor = prePaymentReceivableMinor.coerceAtLeast(0L)
        val arSettledMinor = maxOf(explicitAllocatedMinor, minOf(paymentMinor, receivableFloor))
        val advanceCreditMinor = (paymentMinor - arSettledMinor).coerceAtLeast(0L)
        return arSettledMinor to advanceCreditMinor
    }

    fun validate(draft: CustomerPaymentDraft, invoices: Map<String, SalesInvoice>) {
        require(draft.amountMinor > 0) { "Payment amount must be positive" }
        require(draft.allocations.map { it.invoiceId }.distinct().size == draft.allocations.size) { "Duplicate invoice allocation" }
        val total = draft.allocations.fold(0L) { sum, allocation ->
            val invoice = requireNotNull(invoices[allocation.invoiceId]) { "Invoice no longer exists" }
            require(invoice.customerId == draft.customerId) { "Invoice belongs to another customer" }
            require(invoice.isPosted && invoice.status != "CANCELLED") { "Allocate only to a posted, active invoice" }
            require(allocation.amountMinor > 0 && allocation.amountMinor <= invoice.dueAmountMinor) { "Allocation exceeds current invoice due" }
            Math.addExact(sum, allocation.amountMinor)
        }
        require(total <= draft.amountMinor) { "Allocations exceed payment amount" }
    }
}
