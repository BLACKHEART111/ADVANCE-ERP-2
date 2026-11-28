package com.pocketdev.advance_erp.core.engine

import com.pocketdev.advance_erp.data.entity.InvoiceStatus
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem

/**
 * Pure-JVM guard, committed-stock offset, and payment-allocation reconciliation helpers
 * for editing draft and posted sales invoices.
 */
object SalesInvoiceEditPlanner {

    data class AllocationSlice(
        val allocationId: String,
        val paymentId: String,
        val amountMinor: Long
    )

    data class EditPaymentReconciliation(
        val keptInlineAmounts: Map<String, Long>,
        val freedInlineByPayment: Map<String, Long>,
        val keptReceiptAmounts: Map<String, Long>,
        val freedReceiptByPayment: Map<String, Long>,
        val cashPaidForJournal: Long,
        val arDueForJournal: Long,
        val advanceOverpayForJournal: Long
    )

    fun canEditInvoice(
        invoiceNumber: String,
        status: String,
        hasPostedReturns: Boolean,
        hasDeliveryChallans: Boolean
    ): Result<Unit> {
        if (status == InvoiceStatus.CANCELLED.name) {
            return Result.failure(
                IllegalStateException("Cancelled invoice #$invoiceNumber cannot be edited.")
            )
        }
        if (hasPostedReturns) {
            return Result.failure(
                IllegalStateException(
                    "Cannot edit invoice #$invoiceNumber because a sales return has already been recorded on it."
                )
            )
        }
        if (hasDeliveryChallans) {
            return Result.failure(
                IllegalStateException(
                    "Cannot edit #$invoiceNumber because delivery challan(s) have already been dispatched."
                )
            )
        }
        return Result.success(Unit)
    }

    fun computeCommittedQuantities(
        isPosted: Boolean,
        items: List<SalesInvoiceItem>
    ): Map<Pair<String, String?>, Int> {
        if (!isPosted) return emptyMap()
        return items.groupBy { it.productId to it.variantId }
            .mapValues { (_, list) -> list.sumOf { maxOf(0, it.quantity - it.returnedQuantity) } }
            .filterValues { it > 0 }
    }

    fun effectiveCommittedForWarehouse(
        isPosted: Boolean,
        originalWarehouseId: String?,
        selectedWarehouseId: String,
        committed: Map<Pair<String, String?>, Int>
    ): Map<Pair<String, String?>, Int> {
        if (!isPosted || originalWarehouseId.isNullOrBlank() || originalWarehouseId != selectedWarehouseId) {
            return emptyMap()
        }
        return committed
    }

    fun expectedCustomerDueAfterEdit(
        existingCustomerDueMinor: Long,
        isEditingPostedInvoice: Boolean,
        sameCustomer: Boolean,
        previousInvoiceDueMinor: Long,
        newInvoiceDueMinor: Long
    ): Long {
        val baseDue = if (isEditingPostedInvoice && sameCustomer) {
            existingCustomerDueMinor - previousInvoiceDueMinor
        } else {
            existingCustomerDueMinor
        }
        return maxOf(0L, baseDue + newInvoiceDueMinor)
    }

    fun reconcileSeparateReceiptAllocations(
        newTotalAmountMinor: Long,
        inlineSlices: List<AllocationSlice>,
        receiptSlices: List<AllocationSlice>
    ): EditPaymentReconciliation {
        require(newTotalAmountMinor >= 0L) { "Invoice total cannot be negative" }
        var remainingToCover = newTotalAmountMinor
        var inlineTotal = 0L

        val keptInlineAmounts = linkedMapOf<String, Long>()
        val freedInlineByPayment = linkedMapOf<String, Long>()

        for (slice in inlineSlices) {
            inlineTotal += slice.amountMinor
            val keep = minOf(slice.amountMinor, remainingToCover)
            remainingToCover -= keep
            val freed = slice.amountMinor - keep
            if (keep > 0L) {
                keptInlineAmounts[slice.allocationId] = keep
            }
            if (freed > 0L) {
                freedInlineByPayment[slice.paymentId] =
                    (freedInlineByPayment[slice.paymentId] ?: 0L) + freed
            }
        }

        val keptReceiptAmounts = linkedMapOf<String, Long>()
        val freedReceiptByPayment = linkedMapOf<String, Long>()

        for (slice in receiptSlices) {
            val keep = minOf(slice.amountMinor, remainingToCover)
            remainingToCover -= keep
            val freed = slice.amountMinor - keep
            if (keep > 0L) {
                keptReceiptAmounts[slice.allocationId] = keep
            }
            if (freed > 0L) {
                freedReceiptByPayment[slice.paymentId] =
                    (freedReceiptByPayment[slice.paymentId] ?: 0L) + freed
            }
        }

        return EditPaymentReconciliation(
            keptInlineAmounts = keptInlineAmounts,
            freedInlineByPayment = freedInlineByPayment,
            keptReceiptAmounts = keptReceiptAmounts,
            freedReceiptByPayment = freedReceiptByPayment,
            cashPaidForJournal = inlineTotal,
            arDueForJournal = maxOf(0L, newTotalAmountMinor - inlineTotal),
            advanceOverpayForJournal = maxOf(0L, inlineTotal - newTotalAmountMinor)
        )
    }
}
