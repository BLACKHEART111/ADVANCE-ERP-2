package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.communication.CommunicationModule
import com.pocketdev.advance_erp.core.communication.CommunicationRequest
import com.pocketdev.advance_erp.core.communication.CommunicationStatus
import com.pocketdev.advance_erp.core.communication.CompanyBrandInfo
import com.pocketdev.advance_erp.core.communication.DeliveryChallanBreakdown
import com.pocketdev.advance_erp.core.communication.DueReminderBreakdown
import com.pocketdev.advance_erp.core.communication.InvoiceFinancialBreakdown
import com.pocketdev.advance_erp.core.communication.LedgerStatementBreakdown
import com.pocketdev.advance_erp.core.communication.LedgerStatementRow
import com.pocketdev.advance_erp.core.communication.PaymentReceiptAllocation
import com.pocketdev.advance_erp.core.communication.PaymentReceiptBreakdown
import com.pocketdev.advance_erp.core.communication.RecipientPartyInfo
import com.pocketdev.advance_erp.core.communication.ReturnOrExchangeBreakdown
import com.pocketdev.advance_erp.core.communication.RichDocumentLineItem
import com.pocketdev.advance_erp.core.communication.SafeDocument
import com.pocketdev.advance_erp.core.communication.graphics.BrandedDocumentPalette
import com.pocketdev.advance_erp.core.communication.image.BrandedImageCardSpecBuilder
import com.pocketdev.advance_erp.core.communication.image.ImageCardBodySection
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandedImageCardSpecBuilderTest {

    private val brand = CompanyBrandInfo(
        companyName = "TUKTAK BD",
        tagline = "Your Trusted Sourcing & Wholesale Partner",
        address = "41, New Elephant Road, Dhaka-1205",
        phone = "01639484547",
        currencyCode = "BDT"
    )

    private val customer = RecipientPartyInfo(
        id = "cust-1",
        code = "CUST-9668",
        name = "Hasan Mojumdar",
        phone = "01866482142",
        address = "Dhaka, Bangladesh",
        isSupplier = false
    )

    private val t0 = 1_795_722_000_000L

    private fun baseItem(idx: Int = 1) = RichDocumentLineItem(
        index = idx,
        productId = "p-$idx",
        variantId = "v-$idx",
        titleEn = "4D Cloud Insole Wholesale #$idx",
        titleBn = "4D Cloud Insole Wholesale #$idx",
        sku = "4DCI-WS-00$idx",
        color = "Gray",
        size = "37-38",
        unit = "PCS",
        quantity = 1,
        unitPriceMinor = 1380_00L,
        lineTotalMinor = 1380_00L
    )

    @Test
    fun ref02SalesInvoiceCard_matchesApprovedReferenceAndPagination() {
        val breakdown = InvoiceFinancialBreakdown(
            salesType = "WHOLESALE",
            grossItemsTotalMinor = 1380_00L,
            itemDiscountTotalMinor = 0L,
            subtotalMinor = 1380_00L,
            invoiceDiscountMinor = 0L,
            additionalChargesMinor = 0L,
            netInvoiceAmountMinor = 1380_00L,
            creditReturnMinor = 0L,
            allocatedPaymentMinor = 0L,
            invoiceDueMinor = 1380_00L,
            qrVerificationPayload = "ADV-ERP|INV|INV-2026-0001|20261127|1380.00|UNPAID",
            hasHistoricalSnapshot = true,
            historicalAsOfMillis = t0,
            historicalPreviousBalanceMinor = 29560_00L,
            historicalAllocatedPaymentMinor = 0L,
            historicalInvoiceDueMinor = 1380_00L,
            historicalClosingBalanceMinor = 30940_00L,
            currentAccountBalanceMinor = 30940_00L,
            asOfMillis = t0,
            isReshareAfterLaterActivity = false
        )
        val doc = SafeDocument(
            request = CommunicationRequest(CommunicationModule.INVOICE, "inv-1"),
            number = "INV-2026-0001",
            partyId = customer.id,
            partyName = customer.name,
            phone = customer.phone,
            fields = emptyMap(),
            status = CommunicationStatus.UNPAID,
            balanceMinor = 30940_00L,
            previousMinor = 29560_00L,
            documentDateMillis = t0,
            asOfMillis = t0,
            brand = brand,
            party = customer,
            lineItems = listOf(baseItem(1)),
            invoiceBreakdown = breakdown
        )

        val pages = BrandedImageCardSpecBuilder.buildPages(doc, CommunicationSettings(), "bn")
        assertEquals(1, pages.size)
        val card = pages.single()
        assertEquals("INV-2026-0001_card.png", card.safeFileName)
        assertEquals("বিক্রয় ইনভয়েস", card.titlePrimary)
        assertEquals("INV-2026-0001", card.rightBadge.topChipText)
        assertEquals(BrandedDocumentPalette.DueRedText, card.rightBadge.statusBadge!!.textColor)
        assertTrue(card.greeting!!.recipientLine.contains("Hasan Mojumdar"))

        val itemsSec = card.sections.filterIsInstance<ImageCardBodySection.ItemsListSection>().single()
        assertEquals("Gray | Size: 37-38 | SKU: 4DCI-WS-001", itemsSec.items[0].variantAndSkuLine)
        assertEquals("৳1,380.00", itemsSec.items[0].rightAmountText)

        val split = card.sections.filterIsInstance<ImageCardBodySection.SplitSummaryCardsSection>().single()
        assertEquals(BrandedDocumentPalette.PaymentGreenText, split.leftRows.first { it.label == "পরিশোধ" }.valueColor)
        assertEquals(BrandedDocumentPalette.DueRedText, split.leftRows.first { it.label == "ইনভয়েস বকেয়া" }.valueColor)
        assertTrue(split.rightRows.any { it.label == "পূর্বের বকেয়া" && it.value.contains("29,560.00") })

        val pill = card.sections.filterIsInstance<ImageCardBodySection.PurpleTotalPillSection>().single()
        assertEquals("সর্বমোট বকেয়া", pill.label)
        assertEquals("৳ 30,940.00", pill.amountText)

        // Multi-page splitting for 20 line items -> 3 cards (8 + 8 + 4)
        val multiDoc = doc.copy(lineItems = (1..20).map { baseItem(it) })
        val multiPages = BrandedImageCardSpecBuilder.buildPages(
            multiDoc,
            CommunicationSettings(showProductPhoto = false, showVariantSku = false),
            "en"
        )
        assertEquals(3, multiPages.size)
        assertEquals("INV-2026-0001_card_1.png", multiPages[0].safeFileName)
        assertEquals("INV-2026-0001_card_3.png", multiPages[2].safeFileName)
        assertNotNull(multiPages[0].greeting)
        assertNull(multiPages[1].greeting)
        assertNull(multiPages[0].thankYouText)
        assertNotNull(multiPages[2].thankYouText)
        assertEquals(8, multiPages[0].sections.filterIsInstance<ImageCardBodySection.ItemsListSection>().single().items.size)
        assertEquals(4, multiPages[2].sections.filterIsInstance<ImageCardBodySection.ItemsListSection>().single().items.size)
        val firstMultiItem = multiPages[0].sections.filterIsInstance<ImageCardBodySection.ItemsListSection>().single().items[0]
        assertFalse(firstMultiItem.showThumbnailSlot)
        assertFalse(firstMultiItem.variantAndSkuLine!!.contains("SKU:"))
        assertEquals(1, multiPages[2].sections.filterIsInstance<ImageCardBodySection.PurpleTotalPillSection>().size)
    }

    @Test
    fun ref03PaymentReceivedCard_usesGreenPaymentHighlightsAndExactReconciliation() {
        val doc = SafeDocument(
            request = CommunicationRequest(CommunicationModule.PAYMENT, "pay-5"),
            number = "PAY-2026-0005",
            partyId = customer.id,
            partyName = customer.name,
            phone = customer.phone,
            fields = emptyMap(),
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
                referenceNote = null,
                allocatedToInvoicesMinor = 5000_00L,
                unallocatedAdvanceMinor = 0L,
                allocations = listOf(PaymentReceiptAllocation("inv-1", "INV-2026-0001", 1380_00L)),
                previousBalanceMinor = 30940_00L,
                balanceAfterPaymentMinor = 25940_00L,
                currentAccountBalanceMinor = 25940_00L
            )
        )

        val card = BrandedImageCardSpecBuilder.buildPages(doc, CommunicationSettings(), "bn").single()
        assertEquals("PAY-2026-0005_card.png", card.safeFileName)
        assertEquals(BrandedDocumentPalette.PaymentGreenText, card.rightBadge.statusBadge!!.textColor)
        val refPill = card.sections.filterIsInstance<ImageCardBodySection.ReferencePillSection>().single()
        assertTrue(refPill.primaryLine.contains("PAY-2026-0005"))

        val metrics = card.sections.filterIsInstance<ImageCardBodySection.MetricListCardSection>()
        assertEquals(2, metrics.size)
        val paidRow = metrics[0].rows.first { it.label == "পরিশোধের পরিমাণ" }
        assertEquals("৳5,000.00", paidRow.value)
        assertEquals(BrandedDocumentPalette.PaymentGreenText, paidRow.valueColor)
        val deductRow = metrics[1].rows.first { it.label == "পরিশোধিত" }
        assertEquals("- ৳5,000.00", deductRow.value)
        assertEquals(BrandedDocumentPalette.PaymentGreenText, deductRow.valueColor)

        val pill = card.sections.filterIsInstance<ImageCardBodySection.PurpleTotalPillSection>().single()
        assertEquals("৳ 25,940.00", pill.amountText)
    }

    @Test
    fun ref04ReminderAndRef05LedgerCards_reconcileDueCreditAndPeriodSummary() {
        val remDoc = SafeDocument(
            request = CommunicationRequest(CommunicationModule.REMINDER, customer.id),
            number = "REMINDER-Hasan",
            partyId = customer.id,
            partyName = customer.name,
            phone = customer.phone,
            fields = emptyMap(),
            balanceMinor = 25940_00L,
            asOfMillis = t0,
            brand = brand,
            party = customer,
            reminderBreakdown = DueReminderBreakdown(
                currentTotalDueMinor = 25940_00L,
                priorBalanceMinor = 29560_00L,
                latestInvoiceNumber = "INV-2026-0001",
                latestInvoiceDateMillis = t0,
                latestInvoiceAmountMinor = 1380_00L,
                latestPaymentNumber = "PAY-2026-0005",
                latestPaymentDateMillis = t0,
                latestPaymentAmountMinor = 5000_00L,
                otherNetAdjustmentMinor = 0L,
                asOfMillis = t0
            )
        )
        val remCard = BrandedImageCardSpecBuilder.buildPages(remDoc, CommunicationSettings(), "bn").single()
        val hero = remCard.sections.filterIsInstance<ImageCardBodySection.HeroAmountSection>().single()
        assertEquals(BrandedDocumentPalette.DueRedText, hero.amountColor)
        assertEquals("৳ 25,940.00", hero.amountText)
        val summary = remCard.sections.filterIsInstance<ImageCardBodySection.MetricListCardSection>().single()
        assertEquals(BrandedDocumentPalette.PaymentGreenText, summary.rows.first { it.label.contains("PAY-2026-0005") }.valueColor)

        // Advance credit customer must get BLUE hero and NO payment demand notice
        val creditRemCard = BrandedImageCardSpecBuilder.buildPages(
            remDoc.copy(
                balanceMinor = -4000_00L,
                reminderBreakdown = remDoc.reminderBreakdown!!.copy(currentTotalDueMinor = -4000_00L)
            ),
            CommunicationSettings(),
            "en"
        ).single()
        val creditHero = creditRemCard.sections.filterIsInstance<ImageCardBodySection.HeroAmountSection>().single()
        assertEquals(BrandedDocumentPalette.CreditBlueText, creditHero.amountColor)
        assertFalse(creditRemCard.greeting!!.introParagraph.contains("outstanding", ignoreCase = true))

        // REF-05 Customer Ledger Summary
        val stmtDoc = SafeDocument(
            request = CommunicationRequest(CommunicationModule.STATEMENT, customer.id, from = t0 - 86_400_000L * 26, to = t0),
            number = "STATEMENT-Hasan",
            partyId = customer.id,
            partyName = customer.name,
            phone = customer.phone,
            fields = emptyMap(),
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
                closingBalanceMinor = 25940_00L,
                transactions = listOf(LedgerStatementRow(t0, "INV-2026-0001", "INVOICE", "Sales", 1380_00L, 0L, 30940_00L))
            )
        )
        val stmtCard = BrandedImageCardSpecBuilder.buildPages(stmtDoc, CommunicationSettings(), "bn").single()
        assertTrue(stmtCard.rightBadge.bottomSubText!!.contains("Nov 2026"))
        val ledgerRows = stmtCard.sections.filterIsInstance<ImageCardBodySection.MetricListCardSection>().single().rows
        assertEquals(4, ledgerRows.size)
        assertEquals("৳29,560.00", ledgerRows[0].value)
        assertEquals("৳1,380.00", ledgerRows[1].value)
        assertEquals("৳0.00", ledgerRows[2].value)
        assertEquals("৳5,000.00", ledgerRows[3].value)
        assertEquals(BrandedDocumentPalette.PaymentGreenText, ledgerRows[3].valueColor)
        val stmtPill = stmtCard.sections.filterIsInstance<ImageCardBodySection.PurpleTotalPillSection>().single()
        assertEquals("৳25,940.00", stmtPill.amountText)
    }

    @Test
    fun allModules_buildValidImageCardsInBothLanguagesWithoutFinancialLeaks() {
        val sampleItem = baseItem(1).copy(secondaryQuantityText = "REPLACED")
        for (mod in CommunicationModule.entries) {
            val isSup = mod.isSupplierModule()
            val doc = SafeDocument(
                request = CommunicationRequest(mod, "id-1", from = t0 - 86_400_000L, to = t0),
                number = "${mod.name}-1",
                partyId = "p-1",
                partyName = "Party",
                phone = "01700000000",
                supplier = isSup,
                fields = mapOf(
                    "document_amount" to "৳1,380.00",
                    "total_quantity" to "5",
                    "total_carton" to "2",
                    "payment_method" to "Cash"
                ),
                status = CommunicationStatus.PARTIAL,
                balanceMinor = 1380_00L,
                documentDateMillis = t0,
                asOfMillis = t0,
                brand = brand,
                party = customer.copy(isSupplier = isSup),
                lineItems = if (mod in setOf(
                        CommunicationModule.INVOICE, CommunicationModule.QUOTATION, CommunicationModule.SALES_ORDER,
                        CommunicationModule.PURCHASE_ORDER, CommunicationModule.RETURN, CommunicationModule.EXCHANGE,
                        CommunicationModule.DELIVERY, CommunicationModule.PACKING
                    )
                ) listOf(sampleItem) else emptyList(),
                returnExchangeBreakdown = ReturnOrExchangeBreakdown("INV-1", 1380_00L, 0L, 1380_00L, 500_00L, "Cash", 1380_00L),
                deliveryBreakdown = DeliveryChallanBreakdown("INV-1", "Kamal", "Van", totalDispatchedQuantity = 1)
            )
            for (lang in listOf("bn", "en")) {
                val pages = BrandedImageCardSpecBuilder.buildPages(doc, CommunicationSettings(), lang)
                assertTrue("Expected >= 1 card page for $mod/$lang", pages.isNotEmpty())
                val first = pages.first()
                assertTrue(first.titlePrimary.isNotBlank())
                assertTrue(first.sections.isNotEmpty())
                if (mod == CommunicationModule.DELIVERY) {
                    val items = first.sections.filterIsInstance<ImageCardBodySection.ItemsListSection>().single().items
                    assertFalse(items[0].qtyAndPriceLine.contains("1,380"))
                }
            }
        }
    }
}
