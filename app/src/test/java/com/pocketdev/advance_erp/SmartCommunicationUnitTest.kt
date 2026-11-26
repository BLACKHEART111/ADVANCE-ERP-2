package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.communication.CommunicationModule
import com.pocketdev.advance_erp.core.communication.CommunicationPhone
import com.pocketdev.advance_erp.core.communication.CommunicationRequest
import com.pocketdev.advance_erp.core.communication.CommunicationStatus
import com.pocketdev.advance_erp.core.communication.DynamicTemplateEngine
import com.pocketdev.advance_erp.core.communication.SafeDocument
import com.pocketdev.advance_erp.core.engine.PaymentAllocationValidation
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartCommunicationUnitTest {

    @Test
    fun phoneNormalization_supportsBanglaDigitsBdLandlinesAndInternationalE164() {
        assertEquals("+8801711223344", CommunicationPhone.normalize("০১৭১১-২২৩৩৪৪"))
        assertEquals("+8801711223344", CommunicationPhone.normalize("+880 1711 223344"))
        assertEquals("+8801812345678", CommunicationPhone.normalize("8801812345678"))
        assertEquals("+8801912345678", CommunicationPhone.normalize("008801912345678"))
        assertEquals("+88029551234", CommunicationPhone.normalize("02-9551234"))
        assertEquals("+88031654321", CommunicationPhone.normalize("031654321"))
        assertEquals("+8613800138000", CommunicationPhone.normalize("+86 138 0013 8000"))
        assertEquals("+971501234567", CommunicationPhone.normalize("00971501234567"))

        assertNull(CommunicationPhone.normalize(""))
        assertNull(CommunicationPhone.normalize("01712"))
        assertNull(CommunicationPhone.normalize("+8801012345678"))
        assertNull(CommunicationPhone.normalize("call 01711223344"))
        assertNull(CommunicationPhone.normalize("017+12345678"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun templateValidation_rejectsNonCostAndUnknownPlaceholders() {
        DynamicTemplateEngine.validate("Customer: {customer_name}\nSecret: {unit_cost}")
    }

    @Test(expected = IllegalArgumentException::class)
    fun templateValidation_rejectsMalformedBraces() {
        DynamicTemplateEngine.validate("Customer: {customer_name")
    }

    @Test
    fun templateEngine_omitsMissingLinesRespectsSettingsAndCoversAllModules() {
        for (mod in CommunicationModule.entries) {
            val allowed = DynamicTemplateEngine.allowed(mod)
            assertFalse("COGS/cost must never be exposed in $mod", "unit_cost" in allowed || "profit" in allowed)
            val enSample = DynamicTemplateEngine.sampleFields(mod, "en")
            val bnSample = DynamicTemplateEngine.sampleFields(mod, "bn")
            assertTrue("EN sample missing keys for $mod", enSample.keys.all { it in allowed })
            assertTrue("BN sample missing keys for $mod", bnSample.keys.all { it in allowed })

            val tplEn = DynamicTemplateEngine.defaultTemplate(mod, CommunicationStatus.GENERAL, "en")
            val renderedEn = DynamicTemplateEngine.render(tplEn, enSample)
            assertTrue("Rendered EN template empty for $mod", renderedEn.isNotBlank())
        }

        val doc = SafeDocument(
            request = CommunicationRequest(CommunicationModule.INVOICE, "inv-1"),
            number = "INV-100",
            partyId = "c-1",
            partyName = "Rahim Traders",
            phone = "+8801700000000",
            fields = mapOf(
                "customer_name" to "Rahim Traders",
                "invoice_number" to "INV-100",
                "invoice_amount" to "BDT 15000.00",
                "return_amount" to "BDT 5000.00",
                "received_payment" to "BDT 0.00",
                "invoice_due" to "BDT 10000.00",
                "total_outstanding" to "BDT 20000.00 due",
                "company_name" to "Advance ERP",
                "company_phone" to "+8801700000000"
            ),
            status = CommunicationStatus.PARTIAL,
            balanceMinor = 20000_00L
        )
        val settingsWithReminderNoSig = CommunicationSettings(
            includeReminder = true,
            includeSignature = false
        )
        val fields = DynamicTemplateEngine.fields(doc, settingsWithReminderNoSig, "en")
        assertTrue(fields.containsKey("return_amount"))
        assertTrue(fields.containsKey("reminder_text"))
        assertFalse(fields.containsKey("company_name"))

        val customTpl = "Inv: {invoice_number}\nNote: {payment_reference}\n{reminder_text}\nCompany: {company_name}"
        val rendered = DynamicTemplateEngine.render(customTpl, fields)
        assertTrue(rendered.contains("Inv: INV-100"))
        assertTrue(rendered.contains("Kindly arrange payment"))
        assertFalse("Blank payment_reference line must be omitted cleanly", rendered.contains("Note:"))
        assertFalse("Company signature disabled by settings must be omitted", rendered.contains("Company:"))

        // Customer in advance/credit must NOT receive due reminder text even if setting enabled
        val creditDoc = doc.copy(balanceMinor = -5000_00L, status = CommunicationStatus.CREDIT)
        val creditFields = DynamicTemplateEngine.fields(creditDoc, settingsWithReminderNoSig, "en")
        assertFalse(creditFields.containsKey("reminder_text"))
        assertTrue(creditFields["status_text"].orEmpty().contains("advance / credit"))
    }

    @Test
    fun invoiceSettlementAndCustomerOpeningCredit_recalculateAccuratelyAfterReturnAllocationAndVoid() {
        // Invoice 15,000 -> CUSTOMER_CREDIT return 5,000 => due 10,000, status POSTED
        val afterReturn = PaymentAllocationValidation.computeInvoiceSettlement(
            totalAmountMinor = 15000_00L,
            activeAllocatedMinor = 0L,
            creditReturnMinor = 5000_00L,
            isPosted = true,
            currentStatus = "POSTED"
        )
        assertEquals(0L, afterReturn.paidAmountMinor)
        assertEquals(5000_00L, afterReturn.creditReturnMinor)
        assertEquals(10000_00L, afterReturn.dueAmountMinor)
        assertEquals("POSTED", afterReturn.status)

        // Later payment allocation 6,000 => paid 6,000, due 4,000 (not 9,000!), PARTIALLY_PAID
        val afterPayment = PaymentAllocationValidation.computeInvoiceSettlement(
            totalAmountMinor = 15000_00L,
            activeAllocatedMinor = 6000_00L,
            creditReturnMinor = 5000_00L,
            isPosted = true,
            currentStatus = afterReturn.status
        )
        assertEquals(6000_00L, afterPayment.paidAmountMinor)
        assertEquals(4000_00L, afterPayment.dueAmountMinor)
        assertEquals("PARTIALLY_PAID", afterPayment.status)

        // Void payment => paid 0, due 10,000 (not 15,000!), POSTED
        val afterVoid = PaymentAllocationValidation.computeInvoiceSettlement(
            totalAmountMinor = 15000_00L,
            activeAllocatedMinor = 0L,
            creditReturnMinor = 5000_00L,
            isPosted = true,
            currentStatus = afterPayment.status
        )
        assertEquals(0L, afterVoid.paidAmountMinor)
        assertEquals(10000_00L, afterVoid.dueAmountMinor)
        assertEquals("POSTED", afterVoid.status)

        // Remaining 10,000 paid => PAID, due 0
        val fullyPaid = PaymentAllocationValidation.computeInvoiceSettlement(
            totalAmountMinor = 15000_00L,
            activeAllocatedMinor = 10000_00L,
            creditReturnMinor = 5000_00L,
            isPosted = true,
            currentStatus = "PARTIALLY_PAID"
        )
        assertEquals(0L, fullyPaid.dueAmountMinor)
        assertEquals("PAID", fullyPaid.status)

        // Negative opening credit (-5,000) is preserved across new 3,000 invoice => -2,000 balance
        assertEquals(
            -5000_00L,
            PaymentAllocationValidation.computePreservedOpeningBalance(
                masterDueBefore = -5000_00L,
                transactionNetBefore = 0L
            )
        )
        assertEquals(
            -2000_00L,
            PaymentAllocationValidation.outstandingAfterTransaction(
                masterDueBefore = -5000_00L,
                transactionNetBefore = 0L,
                transactionNetAfter = 3000_00L
            )
        )
        // New 8,000 invoice settles the -5,000 credit + leaves +3,000 due
        assertEquals(
            3000_00L,
            PaymentAllocationValidation.outstandingAfterTransaction(
                masterDueBefore = -5000_00L,
                transactionNetBefore = 0L,
                transactionNetAfter = 8000_00L
            )
        )
    }
}
