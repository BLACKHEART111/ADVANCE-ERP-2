package com.pocketdev.advance_erp.core.engine

import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.domain.repository.CustomerPaymentDraft

object PaymentAllocationValidation {
    fun outstandingAfterPayment(masterDue: Long, transactionNet: Long, payment: Long): Long {
        require(payment > 0) { "Payment amount must be positive" }
        val signedBefore = if (masterDue != 0L) masterDue else transactionNet
        return Math.subtractExact(signedBefore, payment).coerceAtLeast(0L)
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
