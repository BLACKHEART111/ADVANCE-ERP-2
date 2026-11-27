package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.communication.CommunicationModule
import com.pocketdev.advance_erp.core.communication.CommunicationRequest
import com.pocketdev.advance_erp.core.communication.CommunicationStatus
import com.pocketdev.advance_erp.core.communication.CompanyBrandInfo
import com.pocketdev.advance_erp.core.communication.DeliveryChallanBreakdown
import com.pocketdev.advance_erp.core.communication.DueReminderBreakdown
import com.pocketdev.advance_erp.core.communication.DynamicTemplateEngine
import com.pocketdev.advance_erp.core.communication.InvoiceFinancialBreakdown
import com.pocketdev.advance_erp.core.communication.LedgerStatementBreakdown
import com.pocketdev.advance_erp.core.communication.PaymentReceiptAllocation
import com.pocketdev.advance_erp.core.communication.PaymentReceiptBreakdown
import com.pocketdev.advance_erp.core.communication.RecipientPartyInfo
import com.pocketdev.advance_erp.core.communication.ReturnOrExchangeBreakdown
import com.pocketdev.advance_erp.core.communication.RichDocumentLineItem
import com.pocketdev.advance_erp.core.communication.SafeDocument
import com.pocketdev.advance_erp.core.communication.graphics.BrandedDocumentFormatter
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumTextMessageEngineTest {

    private val brand = CompanyBrandInfo(
        companyName = "TUKTAK BD",
        phone = "01639484547",
        address = "41, New Elephant Road, Dhaka-1205",
        currencyCode = "BDT"
    )

    private val customer = RecipientPartyInfo(
        id = "cust-1",
        code = "CUST-9668",
        name = "Hasan Mojumdar",
        phone = "01866482142"
    )

    private val t0 = 1_795_722_000_000L // 27 Nov 2026

    private fun countOccurrences(haystack: String, needle: String): Int {
        if (needle.isEmpty()) return 0
        var count = 0
        var idx = haystack.indexOf(needle)
        while (idx >= 0) {
            count++
            idx = haystack.indexOf(needle, idx + needle.length)
        }
        return count
    }

    private fun invoiceDoc(
        status: CommunicationStatus,
        dueDateMillis: Long? = null,
        paidMinor: Long = 0L,
        invDueMinor: Long = 1380_00L,
        prevMinor: Long = 29560_00L,
        currentBalMinor: Long = 30940_00L,
        hasSnapshot: Boolean = true,
        isReshare: Boolean = false,
        asOf: Long = t0
    ): SafeDocument = SafeDocument(
        request = CommunicationRequest(CommunicationModule.INVOICE, "inv-1"),
        number = "INV-2026-0001",
        partyId = customer.id,
        partyName = customer.name,
        phone = customer.phone,
        fields = mapOf(
            "customer_name" to customer.name,
            "invoice_number" to "INV-2026-0001",
            "document_number" to "INV-2026-0001",
            "invoice_date" to "27 Nov 2026 01:40",
            "document_date" to "27 Nov 2026 01:40",
            "as_of" to "27 Nov 2026 01:40",
            "company_name" to brand.companyName,
            "company_phone" to brand.phone
        ),
        status = status,
        balanceMinor = currentBalMinor,
        previousMinor = if (hasSnapshot) prevMinor else null,
        documentDateMillis = t0,
        asOfMillis = asOf,
        brand = brand,
        party = customer,
        invoiceBreakdown = InvoiceFinancialBreakdown(
            salesType = "WHOLESALE",
            grossItemsTotalMinor = 1380_00L,
            itemDiscountTotalMinor = 0L,
            subtotalMinor = 1380_00L,
            invoiceDiscountMinor = 0L,
            additionalChargesMinor = 0L,
            netInvoiceAmountMinor = 1380_00L,
            creditReturnMinor = 0L,
            allocatedPaymentMinor = paidMinor,
            invoiceDueMinor = invDueMinor,
            dueDateMillis = dueDateMillis,
            hasHistoricalSnapshot = hasSnapshot,
            historicalAsOfMillis = if (hasSnapshot) t0 else null,
            historicalPreviousBalanceMinor = if (hasSnapshot) prevMinor else null,
            historicalAllocatedPaymentMinor = if (hasSnapshot) paidMinor else null,
            historicalInvoiceDueMinor = if (hasSnapshot) invDueMinor else null,
            historicalClosingBalanceMinor = if (hasSnapshot) currentBalMinor else null,
            currentAccountBalanceMinor = currentBalMinor,
            asOfMillis = asOf,
            isReshareAfterLaterActivity = isReshare
        )
    )

    @Test
    fun invoiceMessages_matchConciseWhatsAppReferenceWithoutDuplicateMetadata() {
        val doc = invoiceDoc(status = CommunicationStatus.UNPAID)
        val settings = CommunicationSettings()

        for (lang in listOf("bn", "en")) {
            val chosenStatus = CommunicationStatus.PREVIOUS_DUE
            val tpl = DynamicTemplateEngine.defaultTemplate(CommunicationModule.INVOICE, chosenStatus, lang)
            val msg = DynamicTemplateEngine.render(tpl, DynamicTemplateEngine.fields(doc, settings, lang))

            assertTrue(msg.contains("Hasan Mojumdar"))
            assertEquals("Document number must appear once in $lang message", 1, countOccurrences(msg, "INV-2026-0001"))
            val expectedDateToken = BrandedDocumentFormatter.formatDateTime(t0, lang == "bn", includeTime = false, shortMonth = true)
            assertEquals("Date must appear once in $lang message", 1, countOccurrences(msg, expectedDateToken))
            assertTrue(msg.contains("৳1,380.00"))
            assertTrue(msg.contains("৳29,560.00"))
            assertTrue(msg.contains("৳30,940.00"))
            assertTrue(msg.contains("TUKTAK BD"))
            assertTrue(msg.contains("01639484547"))
            // Zero discount & zero return lines must be cleanly omitted
            assertFalse(msg.contains("ডিসকাউন্ট"))
            assertFalse(msg.contains("Discount"))
            assertFalse(msg.contains("ফেরত সমন্বয়"))
            assertFalse(msg.contains("Return Credit"))
            // Posting-time duplicate snapshot lines must not clutter the concise WhatsApp output
            assertFalse(msg.contains("ঐতিহাসিক হিসাবের সময়"))
            assertFalse(msg.contains("Historical snapshot"))
        }
    }

    @Test
    fun invoiceConditionalStates_distinguishPaidPartialUnpaidOverdueCreditAndReshare() {
        val settings = CommunicationSettings(includeReminder = true)

        // 1. PAID
        val paidDoc = invoiceDoc(CommunicationStatus.PAID, paidMinor = 1380_00L, invDueMinor = 0L, prevMinor = 0L, currentBalMinor = 0L)
        val paidBn = DynamicTemplateEngine.render(
            DynamicTemplateEngine.defaultTemplate(CommunicationModule.INVOICE, CommunicationStatus.PAID, "bn"),
            DynamicTemplateEngine.fields(paidDoc, settings, "bn")
        )
        assertTrue(paidBn.contains("সম্পূর্ণ পরিশোধিত"))
        assertTrue(paidBn.contains("🟢 ইনভয়েস বকেয়া: ৳0.00"))
        assertFalse("Zero previous balance must be hidden on clean PAID invoice", paidBn.contains("পূর্বের বকেয়া"))
        assertFalse("Zero balance must not trigger reminder", paidBn.contains("পরিশোধের অনুরোধ"))

        // 2. PARTIAL
        val partialDoc = invoiceDoc(CommunicationStatus.PARTIAL, paidMinor = 500_00L, invDueMinor = 880_00L, currentBalMinor = 880_00L, prevMinor = 0L)
        val partialEn = DynamicTemplateEngine.render(
            DynamicTemplateEngine.defaultTemplate(CommunicationModule.INVOICE, CommunicationStatus.PARTIAL, "en"),
            DynamicTemplateEngine.fields(partialDoc, settings, "en")
        )
        assertTrue(partialEn.contains("partial payment"))
        assertTrue(partialEn.contains("Paid: ৳500.00"))
        assertTrue(partialEn.contains("Invoice Due: ৳880.00"))

        // 3. OVERDUE
        val overdueDoc = invoiceDoc(
            status = CommunicationStatus.UNPAID,
            dueDateMillis = t0 - 86_400_000L * 2,
            asOf = t0
        )
        val overdueBn = DynamicTemplateEngine.render(
            DynamicTemplateEngine.defaultTemplate(CommunicationModule.INVOICE, CommunicationStatus.OVERDUE, "bn"),
            DynamicTemplateEngine.fields(overdueDoc, settings, "bn")
        )
        assertTrue(overdueBn.contains("তারিখ অতিক্রান্ত"))
        assertTrue(overdueBn.contains("পরিশোধের তারিখ"))
        assertTrue(overdueBn.contains("অনুরোধ রইল"))

        // 4. CREDIT
        val creditDoc = invoiceDoc(
            status = CommunicationStatus.PAID,
            paidMinor = 1380_00L,
            invDueMinor = 0L,
            prevMinor = -5000_00L,
            currentBalMinor = -3620_00L
        )
        val creditBn = DynamicTemplateEngine.render(
            DynamicTemplateEngine.defaultTemplate(CommunicationModule.INVOICE, CommunicationStatus.CREDIT, "bn"),
            DynamicTemplateEngine.fields(creditDoc, settings, "bn")
        )
        assertTrue(creditBn.contains("অগ্রিম ক্রেডিট"))
        assertTrue(creditBn.contains("৳3,620.00 (অগ্রিম ক্রেডিট)"))
        assertFalse("Credit customer must never get due payment demand", creditBn.contains("অনুরোধ রইল"))

        // 5. Reshare after later activity vs Legacy missing snapshot
        val reshareDoc = invoiceDoc(
            status = CommunicationStatus.UNPAID,
            currentBalMinor = 25940_00L,
            isReshare = true,
            asOf = t0 + 86_400_000L
        )
        val reshareBn = DynamicTemplateEngine.render(
            DynamicTemplateEngine.defaultTemplate(CommunicationModule.INVOICE, CommunicationStatus.PREVIOUS_DUE, "bn"),
            DynamicTemplateEngine.fields(reshareDoc, settings, "bn")
        )
        assertTrue(reshareBn.contains("হালনাগাদ মোট ব্যালেন্স"))
        assertTrue(reshareBn.contains("৳25,940.00"))

        val legacyDoc = invoiceDoc(CommunicationStatus.UNPAID, hasSnapshot = false)
        val legacyEn = DynamicTemplateEngine.render(
            DynamicTemplateEngine.defaultTemplate(CommunicationModule.INVOICE, CommunicationStatus.UNPAID, "en"),
            DynamicTemplateEngine.fields(legacyDoc, settings, "en")
        )
        assertFalse("Unavailable snapshot must omit previous_due line cleanly from message", legacyEn.contains("Previous Balance"))
    }

    @Test
    fun paymentReminderStatementDeliveryAndExchangeMessages_areConciseConsistentAndSafe() {
        val settings = CommunicationSettings()

        // Payment Received
        val payDoc = SafeDocument(
            request = CommunicationRequest(CommunicationModule.PAYMENT, "pay-5"),
            number = "PAY-2026-0005",
            partyId = customer.id,
            partyName = customer.name,
            phone = customer.phone,
            fields = mapOf("company_name" to brand.companyName, "company_phone" to brand.phone),
            balanceMinor = 25940_00L,
            previousMinor = 30940_00L,
            documentDateMillis = t0,
            asOfMillis = t0,
            brand = brand,
            party = customer,
            paymentBreakdown = PaymentReceiptBreakdown(
                paymentNumber = "PAY-2026-0005",
                paymentDateMillis = t0,
                paymentAmountMinor = 5000_00L,
                paymentMethod = "Cash",
                allocatedToInvoicesMinor = 1380_00L,
                unallocatedAdvanceMinor = 3620_00L,
                allocations = listOf(PaymentReceiptAllocation("inv-1", "INV-2026-0001", 1380_00L)),
                previousBalanceMinor = 30940_00L,
                balanceAfterPaymentMinor = 25940_00L,
                currentAccountBalanceMinor = 25940_00L
            )
        )
        val payMsgBn = DynamicTemplateEngine.render(
            DynamicTemplateEngine.defaultTemplate(CommunicationModule.PAYMENT, CommunicationStatus.GENERAL, "bn"),
            DynamicTemplateEngine.fields(payDoc, settings, "bn")
        )
        assertTrue(payMsgBn.contains("PAY-2026-0005"))
        assertTrue(payMsgBn.contains("৳5,000.00"))
        assertTrue(payMsgBn.contains("৳30,940.00"))
        assertTrue(payMsgBn.contains("৳25,940.00"))

        // Due Reminder with overdue vs advance credit
        val remDoc = SafeDocument(
            request = CommunicationRequest(CommunicationModule.REMINDER, customer.id),
            number = "REM-1",
            partyId = customer.id,
            partyName = customer.name,
            phone = customer.phone,
            fields = mapOf("company_name" to brand.companyName, "company_phone" to brand.phone),
            balanceMinor = 25940_00L,
            asOfMillis = t0,
            brand = brand,
            party = customer,
            reminderBreakdown = DueReminderBreakdown(
                currentTotalDueMinor = 25940_00L,
                earliestDueDateMillis = t0 - 86_400_000L,
                unpaidInvoiceCount = 1,
                priorBalanceMinor = 29560_00L,
                latestInvoiceNumber = "INV-2026-0001",
                latestInvoiceAmountMinor = 1380_00L,
                latestPaymentNumber = "PAY-2026-0005",
                latestPaymentAmountMinor = 5000_00L,
                asOfMillis = t0
            )
        )
        val remOverdueEn = DynamicTemplateEngine.render(
            DynamicTemplateEngine.defaultTemplate(CommunicationModule.REMINDER, CommunicationStatus.OVERDUE, "en"),
            DynamicTemplateEngine.fields(remDoc, settings, "en")
        )
        assertTrue(remOverdueEn.contains("past due"))
        assertTrue(remOverdueEn.contains("INV-2026-0001 (৳1,380.00)"))
        assertTrue(remOverdueEn.contains("PAY-2026-0005 (৳5,000.00)"))
        assertTrue(remOverdueEn.contains("৳25,940.00"))

        // Statement omits zero returns cleanly
        val stmtDoc = SafeDocument(
            request = CommunicationRequest(CommunicationModule.STATEMENT, customer.id),
            number = "STMT-1",
            partyId = customer.id,
            partyName = customer.name,
            phone = customer.phone,
            fields = mapOf("company_name" to brand.companyName, "company_phone" to brand.phone),
            balanceMinor = 25940_00L,
            asOfMillis = t0,
            brand = brand,
            party = customer,
            statementBreakdown = LedgerStatementBreakdown(
                fromMillis = t0 - 86_400_000L * 26,
                toMillis = t0,
                isAllTime = false,
                openingBalanceMinor = 29560_00L,
                totalSalesOrPurchasesMinor = 1380_00L,
                totalReturnMinor = 0L,
                totalRefundPaidMinor = 0L,
                totalPaymentMinor = 5000_00L,
                netAdjustmentMinor = 0L,
                closingBalanceMinor = 25940_00L
            )
        )
        val stmtBn = DynamicTemplateEngine.render(
            DynamicTemplateEngine.defaultTemplate(CommunicationModule.STATEMENT, CommunicationStatus.GENERAL, "bn"),
            DynamicTemplateEngine.fields(stmtDoc, settings, "bn")
        )
        assertTrue(stmtBn.contains("৳29,560.00"))
        assertTrue(stmtBn.contains("৳5,000.00"))
        assertTrue(stmtBn.contains("৳25,940.00"))
        assertFalse(stmtBn.contains("মোট ফেরত"))

        // Delivery Challan excludes all prices & balances
        val delDoc = SafeDocument(
            request = CommunicationRequest(CommunicationModule.DELIVERY, "inv-1|DEL-1"),
            number = "DEL-2026-0001",
            partyId = customer.id,
            partyName = customer.name,
            phone = customer.phone,
            fields = mapOf("document_number" to "DEL-2026-0001", "company_name" to brand.companyName, "company_phone" to brand.phone),
            documentDateMillis = t0,
            asOfMillis = t0,
            brand = brand,
            party = customer,
            lineItems = listOf(RichDocumentLineItem(1, "p-1", null, "4D Cloud Insole", "4D Cloud Insole", color = "Gray", size = "37-38", quantity = 10, unitPriceMinor = 1380_00L, lineTotalMinor = 13800_00L)),
            deliveryBreakdown = DeliveryChallanBreakdown("INV-2026-0001", "Kamal", "Van", totalDispatchedQuantity = 10)
        )
        val delBn = DynamicTemplateEngine.render(
            DynamicTemplateEngine.defaultTemplate(CommunicationModule.DELIVERY, CommunicationStatus.GENERAL, "bn"),
            DynamicTemplateEngine.fields(delDoc, settings, "bn")
        )
        assertTrue(delBn.contains("DEL-2026-0001"))
        assertTrue(delBn.contains("10 PCS"))
        assertFalse(delBn.contains("1,380"))
        assertFalse(delBn.contains("13,800"))

        // Return & Exchange
        val exDoc = SafeDocument(
            request = CommunicationRequest(CommunicationModule.EXCHANGE, "EXC-1"),
            number = "EXC-2026-0001",
            partyId = customer.id,
            partyName = customer.name,
            phone = customer.phone,
            fields = mapOf("document_number" to "EXC-2026-0001", "payment_method" to "Cash", "company_name" to brand.companyName, "company_phone" to brand.phone),
            balanceMinor = 1500_00L,
            documentDateMillis = t0,
            asOfMillis = t0,
            brand = brand,
            party = customer,
            lineItems = listOf(
                RichDocumentLineItem(1, "p-1", null, "Insole A", "ইনসোল এ", quantity = 1, secondaryQuantityText = "RETURNED"),
                RichDocumentLineItem(2, "p-2", null, "Insole B", "ইনসোল বি", quantity = 1, secondaryQuantityText = "REPLACED")
            ),
            returnExchangeBreakdown = ReturnOrExchangeBreakdown("INV-1", netPriceDifferenceMinor = 500_00L, refundOrPaymentMethod = "Cash", currentAccountBalanceMinor = 1500_00L)
        )
        val exEn = DynamicTemplateEngine.render(
            DynamicTemplateEngine.defaultTemplate(CommunicationModule.EXCHANGE, CommunicationStatus.GENERAL, "en"),
            DynamicTemplateEngine.fields(exDoc, settings, "en")
        )
        assertTrue(exEn.contains("EXC-2026-0001"))
        assertTrue(exEn.contains("+ ৳500.00"))
        assertTrue(exEn.contains("৳1,500.00"))
    }
}
