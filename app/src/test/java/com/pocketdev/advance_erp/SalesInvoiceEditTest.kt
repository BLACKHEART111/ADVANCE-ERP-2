package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.PaymentAllocationValidation
import com.pocketdev.advance_erp.core.engine.SalesInvoiceEditPlanner
import com.pocketdev.advance_erp.data.entity.InvoiceStatus
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SalesInvoiceEditTest {

    @Test
    fun canEditInvoice_allowsDraftAndPosted_blocksCancelledReturnsAndChallans() {
        assertTrue(
            SalesInvoiceEditPlanner.canEditInvoice(
                invoiceNumber = "INV-1001",
                status = InvoiceStatus.DRAFT.name,
                hasPostedReturns = false,
                hasDeliveryChallans = false
            ).isSuccess
        )
        assertTrue(
            SalesInvoiceEditPlanner.canEditInvoice(
                invoiceNumber = "INV-1002",
                status = InvoiceStatus.PAID.name,
                hasPostedReturns = false,
                hasDeliveryChallans = false
            ).isSuccess
        )

        val cancelled = SalesInvoiceEditPlanner.canEditInvoice(
            invoiceNumber = "INV-1003",
            status = InvoiceStatus.CANCELLED.name,
            hasPostedReturns = false,
            hasDeliveryChallans = false
        )
        assertTrue(cancelled.isFailure)

        val returned = SalesInvoiceEditPlanner.canEditInvoice(
            invoiceNumber = "INV-1004",
            status = InvoiceStatus.POSTED.name,
            hasPostedReturns = true,
            hasDeliveryChallans = false
        )
        assertTrue(returned.isFailure)

        val dispatched = SalesInvoiceEditPlanner.canEditInvoice(
            invoiceNumber = "INV-1005",
            status = InvoiceStatus.SALES_ORDER.name,
            hasPostedReturns = false,
            hasDeliveryChallans = true
        )
        assertTrue(dispatched.isFailure)
    }

    @Test
    fun effectiveCommittedForWarehouse_addsBackPostedQtyOnlyOnOriginalWarehouse() {
        val item = SalesInvoiceItem(
            id = "item-1",
            invoiceId = "inv-1",
            productId = "prod-a",
            variantId = "var-black",
            productNameEn = "Insole",
            productNameBn = "ইনসোল",
            sku = "INS-BLK",
            quantity = 5,
            returnedQuantity = 1,
            unitPriceMinor = 50_000L,
            discountMinor = 0L,
            lineTotalMinor = 200_000L,
            unitCostMinor = 25_000L,
            totalCogsMinor = 100_000L
        )
        val postedCommitted = SalesInvoiceEditPlanner.computeCommittedQuantities(
            isPosted = true,
            items = listOf(item)
        )
        assertEquals(mapOf(("prod-a" to "var-black") to 4), postedCommitted)

        val sameWarehouseOffset = SalesInvoiceEditPlanner.effectiveCommittedForWarehouse(
            isPosted = true,
            originalWarehouseId = "wh-main",
            selectedWarehouseId = "wh-main",
            committed = postedCommitted
        )
        assertEquals(4, sameWarehouseOffset["prod-a" to "var-black"])

        val switchedWarehouseOffset = SalesInvoiceEditPlanner.effectiveCommittedForWarehouse(
            isPosted = true,
            originalWarehouseId = "wh-main",
            selectedWarehouseId = "wh-second",
            committed = postedCommitted
        )
        assertTrue(switchedWarehouseOffset.isEmpty())

        val draftCommitted = SalesInvoiceEditPlanner.computeCommittedQuantities(
            isPosted = false,
            items = listOf(item)
        )
        assertTrue(draftCommitted.isEmpty())
    }

    @Test
    fun reconcileSeparateReceipts_downsizingPaidInvoiceMovesExcessToAdvanceWithBalancedJournals() {
        // Originally INV total = ৳1,000 paid via separate receipt RCPT-1 (৳1,000)
        // Edited down to ৳600 -> keeps ৳600 allocated, frees ৳400 to Customer Advance (coa-2030)
        val reconciliation = SalesInvoiceEditPlanner.reconcileSeparateReceiptAllocations(
            newTotalAmountMinor = 60_000L,
            inlineSlices = emptyList(),
            receiptSlices = listOf(
                SalesInvoiceEditPlanner.AllocationSlice(
                    allocationId = "alloc-1",
                    paymentId = "pay-1",
                    amountMinor = 100_000L
                )
            )
        )

        assertEquals(mapOf("alloc-1" to 60_000L), reconciliation.keptReceiptAmounts)
        assertEquals(mapOf("pay-1" to 40_000L), reconciliation.freedReceiptByPayment)
        assertEquals(0L, reconciliation.cashPaidForJournal)
        assertEquals(60_000L, reconciliation.arDueForJournal)
        assertEquals(0L, reconciliation.advanceOverpayForJournal)

        val settlement = PaymentAllocationValidation.computeInvoiceSettlement(
            totalAmountMinor = 60_000L,
            activeAllocatedMinor = reconciliation.keptReceiptAmounts.values.sum(),
            creditReturnMinor = 0L,
            isPosted = true,
            currentStatus = InvoiceStatus.PAID.name
        )
        assertEquals(InvoiceStatus.PAID.name, settlement.status)
        assertEquals(60_000L, settlement.paidAmountMinor)
        assertEquals(0L, settlement.dueAmountMinor)

        // Trial Balance check across CUSTOMER_PAYMENT + edited SALES_INVOICE + PAYMENT_TO_ADVANCE:
        val cashDebit = 100_000L
        val arNet = -100_000L + reconciliation.arDueForJournal + (reconciliation.freedReceiptByPayment["pay-1"] ?: 0L)
        val revenueCredit = 60_000L
        val advanceCredit = reconciliation.freedReceiptByPayment["pay-1"] ?: 0L
        assertEquals(0L, arNet)
        assertEquals(cashDebit, revenueCredit + advanceCredit)
    }

    @Test
    fun reconcileSeparateReceipts_upsizingPaidInvoiceTransitionsToPartiallyPaidDue() {
        // Originally INV total = ৳800 paid via receipt (৳800), edited up to ৳1,200
        val reconciliation = SalesInvoiceEditPlanner.reconcileSeparateReceiptAllocations(
            newTotalAmountMinor = 120_000L,
            inlineSlices = emptyList(),
            receiptSlices = listOf(
                SalesInvoiceEditPlanner.AllocationSlice("alloc-1", "pay-1", 80_000L)
            )
        )

        assertEquals(mapOf("alloc-1" to 80_000L), reconciliation.keptReceiptAmounts)
        assertTrue(reconciliation.freedReceiptByPayment.isEmpty())
        assertEquals(120_000L, reconciliation.arDueForJournal)

        val settlement = PaymentAllocationValidation.computeInvoiceSettlement(
            totalAmountMinor = 120_000L,
            activeAllocatedMinor = reconciliation.keptReceiptAmounts.values.sum(),
            creditReturnMinor = 0L,
            isPosted = true,
            currentStatus = InvoiceStatus.PAID.name
        )
        assertEquals(InvoiceStatus.PARTIALLY_PAID.name, settlement.status)
        assertEquals(80_000L, settlement.paidAmountMinor)
        assertEquals(40_000L, settlement.dueAmountMinor)
    }

    @Test
    fun reconcileSeparateReceipts_mixedInlineAndReceiptOverpaymentStaysBalanced() {
        // ৳200 inline cash + ৳300 separate receipt on original ৳1,000 invoice, edited down to ৳150
        val reconciliation = SalesInvoiceEditPlanner.reconcileSeparateReceiptAllocations(
            newTotalAmountMinor = 15_000L,
            inlineSlices = listOf(
                SalesInvoiceEditPlanner.AllocationSlice("alloc-inline", "pay-inline", 20_000L)
            ),
            receiptSlices = listOf(
                SalesInvoiceEditPlanner.AllocationSlice("alloc-rcpt", "pay-rcpt", 30_000L)
            )
        )

        assertEquals(mapOf("alloc-inline" to 15_000L), reconciliation.keptInlineAmounts)
        assertEquals(mapOf("pay-inline" to 5_000L), reconciliation.freedInlineByPayment)
        assertTrue(reconciliation.keptReceiptAmounts.isEmpty())
        assertEquals(mapOf("pay-rcpt" to 30_000L), reconciliation.freedReceiptByPayment)
        assertEquals(20_000L, reconciliation.cashPaidForJournal)
        assertEquals(0L, reconciliation.arDueForJournal)
        assertEquals(5_000L, reconciliation.advanceOverpayForJournal)

        // Net Cash (+500) = Revenue (-150) + Customer Advance (-50 -300 = -350), AR net = 0
        val arNet = -30_000L + reconciliation.arDueForJournal + 30_000L
        assertEquals(0L, arNet)
    }

    @Test
    fun expectedCustomerDueAfterEdit_avoidsDoubleCountingPriorInvoiceDueOnSameCustomer() {
        // Customer has ৳1,500 existing due including ৳500 from this invoice; edited down to ৳300 due
        val sameCustomerExpected = SalesInvoiceEditPlanner.expectedCustomerDueAfterEdit(
            existingCustomerDueMinor = 150_000L,
            isEditingPostedInvoice = true,
            sameCustomer = true,
            previousInvoiceDueMinor = 50_000L,
            newInvoiceDueMinor = 30_000L
        )
        assertEquals(130_000L, sameCustomerExpected)

        val differentCustomerExpected = SalesInvoiceEditPlanner.expectedCustomerDueAfterEdit(
            existingCustomerDueMinor = 40_000L,
            isEditingPostedInvoice = true,
            sameCustomer = false,
            previousInvoiceDueMinor = 50_000L,
            newInvoiceDueMinor = 30_000L
        )
        assertEquals(70_000L, differentCustomerExpected)
    }
}
