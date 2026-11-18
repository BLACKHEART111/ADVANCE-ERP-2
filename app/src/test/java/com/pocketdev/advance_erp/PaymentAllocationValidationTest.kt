package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.PaymentAllocationValidation
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.domain.repository.CustomerPaymentDraft
import com.pocketdev.advance_erp.domain.repository.InvoiceAllocationDraft
import org.junit.Test

class PaymentAllocationValidationTest {
    private val invoice=SalesInvoice(id="i",invoiceNumber="INV",customerId="c",customerName="Customer",warehouseId="w",warehouseName="WH",isPosted=true,status="UNPAID",dueAmountMinor=1000)
    private fun draft(amount:Long=1000,allocation:Long=1000)=CustomerPaymentDraft("c",amount,allocations=listOf(InvoiceAllocationDraft("i",allocation)))
    @Test fun explicitInvoiceAndStandaloneCreditAreValid() {
        PaymentAllocationValidation.validate(draft(),mapOf("i" to invoice))
        PaymentAllocationValidation.validate(CustomerPaymentDraft("c",1000),emptyMap())
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsCrossCustomerInvoice() { PaymentAllocationValidation.validate(draft(),mapOf("i" to invoice.copy(customerId="other"))) }
    @Test(expected=IllegalArgumentException::class) fun rejectsDraft() { PaymentAllocationValidation.validate(draft(),mapOf("i" to invoice.copy(isPosted=false))) }
    @Test(expected=IllegalArgumentException::class) fun rejectsCancelledInvoice() { PaymentAllocationValidation.validate(draft(),mapOf("i" to invoice.copy(status="CANCELLED"))) }
    @Test(expected=IllegalArgumentException::class) fun rejectsOverallocatedPayment() { PaymentAllocationValidation.validate(draft(amount=500),mapOf("i" to invoice)) }
    @Test(expected=IllegalArgumentException::class) fun rejectsStaleInvoiceDue() { PaymentAllocationValidation.validate(draft(),mapOf("i" to invoice.copy(dueAmountMinor=500))) }
    @Test(expected=IllegalArgumentException::class) fun rejectsDuplicateAllocation() { val d=draft();PaymentAllocationValidation.validate(d.copy(allocations=d.allocations+d.allocations),mapOf("i" to invoice)) }
    @Test(expected=IllegalArgumentException::class) fun rejectsNegativeReceipt() { PaymentAllocationValidation.validate(CustomerPaymentDraft("c",-1),emptyMap()) }
    @Test fun receiptReducesTheExistingMasterDueInsteadOfCreatingNewOpeningDebt() {
        org.junit.Assert.assertEquals(600L, PaymentAllocationValidation.outstandingAfterPayment(1000,1000,400))
    }
    @Test fun legacyOpeningSurvivesPartialPaymentAndAdvanceStaysNonReceivable() {
        org.junit.Assert.assertEquals(1000L, PaymentAllocationValidation.outstandingAfterPayment(1400,1000,400))
        org.junit.Assert.assertEquals(0L, PaymentAllocationValidation.outstandingAfterPayment(0,-100,200))
    }
}
