package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.communication.CommunicationAccountingReconciler
import com.pocketdev.advance_erp.core.communication.CommunicationModule
import com.pocketdev.advance_erp.core.communication.CommunicationPhone
import com.pocketdev.advance_erp.core.communication.CommunicationRequest
import com.pocketdev.advance_erp.core.communication.CommunicationStatus
import com.pocketdev.advance_erp.core.communication.DynamicTemplateEngine
import com.pocketdev.advance_erp.core.communication.InvoiceFinancialBreakdown
import com.pocketdev.advance_erp.core.communication.RichDocumentLineItem
import com.pocketdev.advance_erp.core.communication.SafeDocument
import com.pocketdev.advance_erp.core.communication.ShareOutputFormat
import com.pocketdev.advance_erp.core.communication.defaultSelectedFormats
import com.pocketdev.advance_erp.core.engine.PaymentAllocationValidation
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import com.pocketdev.advance_erp.data.entity.CommunicationSnapshot
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.data.entity.PaymentAllocation
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import com.pocketdev.advance_erp.domain.repository.CustomerLedgerEntry
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

    @Test
    fun invoiceBreakdown_reconcilesPromptVerificationAndHistoricalReshareWithoutFabrication() {
        val t0 = 1_764_200_000_000L
        val invoice = SalesInvoice(
            id = "inv-001",
            invoiceNumber = "INV-2026-0001",
            customerId = "cust-1",
            customerName = "Hasan Mojumdar",
            customerPhone = "01866482142",
            warehouseId = "wh-1",
            warehouseName = "Main Warehouse",
            status = "POSTED",
            subtotalMinor = 1380_00L,
            discountMinor = 0L,
            taxMinor = 0L,
            totalAmountMinor = 1380_00L,
            paidAmountMinor = 0L,
            dueAmountMinor = 1380_00L,
            invoiceDate = t0,
            isPosted = true,
            salesType = "WHOLESALE"
        )
        val item = SalesInvoiceItem(
            id = "item-1",
            invoiceId = invoice.id,
            productId = "p-1",
            variantId = "v-1",
            productNameEn = "4D Cloud Insole Wholesale",
            productNameBn = "4D Cloud Insole Wholesale",
            sku = "4DCI-WS-001",
            quantity = 1,
            unitPriceMinor = 1380_00L,
            discountMinor = 0L,
            lineTotalMinor = 1380_00L
        )
        val snapshot = CommunicationSnapshot(
            module = "INVOICE",
            documentId = invoice.id,
            number = invoice.invoiceNumber,
            partyId = invoice.customerId,
            createdAt = t0,
            originalAmountMinor = 1380_00L,
            previousBalanceMinor = 29560_00L,
            allocatedPaymentMinor = 0L,
            invoiceDueMinor = 1380_00L,
            closingBalanceMinor = 30940_00L
        )

        // Fresh post verification: 29,560.00 + 1,380.00 - 0.00 = 30,940.00
        val fresh = CommunicationAccountingReconciler.buildInvoiceBreakdown(
            invoice = invoice,
            items = listOf(item),
            allocatedPaymentMinor = 0L,
            creditReturnMinor = 0L,
            snapshot = snapshot,
            currentAccountBalanceMinor = 30940_00L,
            status = CommunicationStatus.UNPAID,
            showQrCode = true,
            asOfMillis = t0
        )
        assertEquals(1380_00L, fresh.netInvoiceAmountMinor)
        assertEquals(1380_00L, fresh.invoiceDueMinor)
        assertEquals(29560_00L, fresh.historicalPreviousBalanceMinor)
        assertEquals(30940_00L, fresh.historicalClosingBalanceMinor)
        assertEquals(30940_00L, fresh.currentAccountBalanceMinor)
        assertFalse(fresh.isReshareAfterLaterActivity)
        assertTrue(fresh.qrVerificationPayload.startsWith("ADV-ERP|INV|INV-2026-0001|"))
        assertTrue("QR payload must stay compact for local matrix generation", fresh.qrVerificationPayload.length <= 80)

        // Re-share after later 5,000.00 standalone payment: invoice remains 1,380.00 due, live customer due is 25,940.00
        val reshare = CommunicationAccountingReconciler.buildInvoiceBreakdown(
            invoice = invoice,
            items = listOf(item),
            allocatedPaymentMinor = 0L,
            creditReturnMinor = 0L,
            snapshot = snapshot,
            currentAccountBalanceMinor = 25940_00L,
            status = CommunicationStatus.UNPAID,
            showQrCode = false,
            asOfMillis = t0 + 86_400_000L
        )
        assertTrue(reshare.hasHistoricalSnapshot)
        assertTrue(reshare.isReshareAfterLaterActivity)
        assertEquals(1380_00L, reshare.netInvoiceAmountMinor)
        assertEquals(1380_00L, reshare.invoiceDueMinor)
        assertEquals(29560_00L, reshare.historicalPreviousBalanceMinor)
        assertEquals(30940_00L, reshare.historicalClosingBalanceMinor)
        assertEquals(25940_00L, reshare.currentAccountBalanceMinor)
        assertEquals("", reshare.qrVerificationPayload)

        // Legacy invoice without posting snapshot must NEVER fabricate historical balances
        val legacy = CommunicationAccountingReconciler.buildInvoiceBreakdown(
            invoice = invoice,
            items = listOf(item),
            allocatedPaymentMinor = 0L,
            creditReturnMinor = 0L,
            snapshot = null,
            currentAccountBalanceMinor = 25940_00L,
            status = CommunicationStatus.UNPAID,
            showQrCode = true,
            asOfMillis = t0 + 86_400_000L
        )
        assertFalse(legacy.hasHistoricalSnapshot)
        assertTrue(legacy.isReshareAfterLaterActivity)
        assertNull(legacy.historicalPreviousBalanceMinor)
        assertNull(legacy.historicalClosingBalanceMinor)
        assertEquals(25940_00L, legacy.currentAccountBalanceMinor)
    }

    @Test
    fun paymentReminderAndStatementBreakdowns_reconcileWithLedgerAndPreserveCostPrivacy() {
        val t0 = 1_764_200_000_000L
        val ledger = listOf(
            CustomerLedgerEntry("opening-cust-1", t0 - 10_000L, "OPENING_BALANCE", "OPENING", "Opening", 29560_00L, 0L, 29560_00L),
            CustomerLedgerEntry("inv-001", t0, "INVOICE", "INV-2026-0001", "Sales Invoice", 1380_00L, 0L, 30940_00L),
            CustomerLedgerEntry("pay-005", t0 + 5_000L, "PAYMENT", "PAY-2026-0005", "Payment Receipt", 0L, 5000_00L, 25940_00L)
        )

        val payment = CustomerPayment(
            id = "pay-005",
            paymentNumber = "PAY-2026-0005",
            customerId = "cust-1",
            customerName = "Hasan Mojumdar",
            amountMinor = 5000_00L,
            allocatedAmountMinor = 5000_00L,
            unallocatedCreditMinor = 0L,
            paymentDate = t0 + 5_000L,
            paymentMethod = "Cash"
        )
        val paySnap = CommunicationSnapshot(
            module = "PAYMENT",
            documentId = payment.id,
            number = payment.paymentNumber,
            partyId = payment.customerId,
            createdAt = payment.paymentDate,
            originalAmountMinor = 5000_00L,
            previousBalanceMinor = 30940_00L,
            allocatedPaymentMinor = 5000_00L,
            invoiceDueMinor = 0L,
            closingBalanceMinor = 25940_00L
        )
        val payBreakdown = CommunicationAccountingReconciler.buildPaymentBreakdown(
            payment = payment,
            allocations = listOf(PaymentAllocation("a-1", payment.id, "inv-001", "INV-2026-0001", 1380_00L)),
            snapshot = paySnap,
            customerLedger = ledger,
            currentAccountBalanceMinor = 25940_00L
        )
        assertEquals(30940_00L, payBreakdown.previousBalanceMinor)
        assertEquals(25940_00L, payBreakdown.balanceAfterPaymentMinor)
        assertEquals(25940_00L, payBreakdown.currentAccountBalanceMinor)
        assertEquals(1380_00L, payBreakdown.allocatedToInvoicesMinor)
        assertFalse(payBreakdown.hasLaterActivityAfterPayment)

        // Legacy payment without snapshot derives from ledger entry cleanly
        val legacyPayBreakdown = CommunicationAccountingReconciler.buildPaymentBreakdown(
            payment = payment,
            allocations = emptyList(),
            snapshot = null,
            customerLedger = ledger,
            currentAccountBalanceMinor = 25940_00L
        )
        assertEquals(30940_00L, legacyPayBreakdown.previousBalanceMinor)
        assertEquals(25940_00L, legacyPayBreakdown.balanceAfterPaymentMinor)

        // Due reminder: 29,560 + 1,380 - 5,000 = 25,940
        val reminder = CommunicationAccountingReconciler.buildDueReminderBreakdown(
            customerLedger = ledger,
            customerOutstandingFallback = 25940_00L,
            unpaidInvoiceDueDates = listOf(t0 + 86_400_000L),
            asOfMillis = t0 + 10_000L
        )
        assertEquals(25940_00L, reminder.currentTotalDueMinor)
        assertEquals(29560_00L, reminder.priorBalanceMinor)
        assertEquals("INV-2026-0001", reminder.latestInvoiceNumber)
        assertEquals(1380_00L, reminder.latestInvoiceAmountMinor)
        assertEquals("PAY-2026-0005", reminder.latestPaymentNumber)
        assertEquals(5000_00L, reminder.latestPaymentAmountMinor)
        assertEquals(0L, reminder.otherNetAdjustmentMinor)

        // Multi-transaction reminder with intervening return preserves exact mathematical sum invariant
        val extendedLedger = listOf(
            CustomerLedgerEntry("opening-cust-1", t0 - 10_000L, "OPENING_BALANCE", "OPENING", "Opening", 29560_00L, 0L, 29560_00L),
            CustomerLedgerEntry("inv-001", t0, "INVOICE", "INV-2026-0001", "Sales", 2000_00L, 0L, 31560_00L),
            CustomerLedgerEntry("ret-001", t0 + 2_000L, "RETURN", "RET-1", "Return", 0L, 620_00L, 30940_00L),
            CustomerLedgerEntry("pay-005", t0 + 5_000L, "PAYMENT", "PAY-2026-0005", "Pay", 0L, 5000_00L, 25940_00L)
        )
        val extReminder = CommunicationAccountingReconciler.buildDueReminderBreakdown(
            customerLedger = extendedLedger,
            customerOutstandingFallback = 25940_00L,
            unpaidInvoiceDueDates = emptyList(),
            asOfMillis = t0 + 10_000L
        )
        val reconstructedTotal = extReminder.priorBalanceMinor +
            (extReminder.latestInvoiceAmountMinor ?: 0L) -
            (extReminder.latestPaymentAmountMinor ?: 0L) +
            extReminder.otherNetAdjustmentMinor
        assertEquals(25940_00L, reconstructedTotal)

        // All-time customer ledger statement: Opening 29,560 + Sales 1,380 - Return 0 - Payment 5,000 = Closing 25,940
        val stmt = CommunicationAccountingReconciler.buildCustomerStatementBreakdown(
            customerLedger = ledger,
            fromMillis = 0L,
            toMillis = t0 + 10_000L,
            isAllTime = true
        )
        assertEquals(29560_00L, stmt.openingBalanceMinor)
        assertEquals(1380_00L, stmt.totalSalesOrPurchasesMinor)
        assertEquals(0L, stmt.totalReturnMinor)
        assertEquals(5000_00L, stmt.totalPaymentMinor)
        assertEquals(25940_00L, stmt.closingBalanceMinor)
        assertEquals(2, stmt.transactions.size)

        // Default format selection respects settings and never returns empty set
        val allOff = CommunicationSettings(includePdf = false, includeImageCard = false, includeTextMessage = false)
        assertEquals(setOf(ShareOutputFormat.TEXT_MESSAGE), allOff.defaultSelectedFormats())
        val pdfAndImage = CommunicationSettings(includePdf = true, includeImageCard = true, includeTextMessage = false)
        assertEquals(setOf(ShareOutputFormat.PDF, ShareOutputFormat.IMAGE_CARD), pdfAndImage.defaultSelectedFormats())

        // Strict cost/COGS/profit privacy check across communication model declarations
        val forbiddenFragments = listOf("cost", "cogs", "profit", "landed", "margin")
        val modelClasses = listOf(
            RichDocumentLineItem::class.java,
            InvoiceFinancialBreakdown::class.java,
            SafeDocument::class.java
        )
        modelClasses.forEach { cls ->
            cls.declaredFields.forEach { field ->
                val name = field.name.lowercase()
                assertFalse("Forbidden internal field '${field.name}' found on ${cls.simpleName}", forbiddenFragments.any { it in name })
            }
        }
    }
}
