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
import com.pocketdev.advance_erp.core.communication.pdf.BrandedPdfSpecBuilder
import com.pocketdev.advance_erp.core.communication.pdf.PdfTableMode
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandedPdfSpecBuilderTest {

    private val brand = CompanyBrandInfo(
        companyName = "TUKTAK BD",
        tagline = "Your Trusted Sourcing & Wholesale Partner",
        address = "41, New Elephant Road, Dhaka-1205",
        phone = "01639484547",
        website = "https://tuktakbd.example",
        binNumber = "002891928-0101",
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

    private val t0 = 1_764_200_000_000L

    private fun sampleInvoiceDoc(
        breakdown: InvoiceFinancialBreakdown,
        items: List<RichDocumentLineItem> = listOf(
            RichDocumentLineItem(
                index = 1,
                productId = "p-1",
                variantId = "v-1",
                titleEn = "4D Cloud Insole Wholesale",
                titleBn = "4D Cloud Insole Wholesale",
                sku = "4DCI-WS-001",
                color = "Gray",
                size = "37-38",
                unit = "PCS",
                thumbnailPath = null,
                quantity = 1,
                unitPriceMinor = 1380_00L,
                discountMinor = 0L,
                discountPercentBps = null,
                lineTotalMinor = 1380_00L
            )
        ),
        brandOverride: CompanyBrandInfo = brand,
        status: CommunicationStatus = CommunicationStatus.UNPAID
    ): SafeDocument = SafeDocument(
        request = CommunicationRequest(CommunicationModule.INVOICE, "inv-001"),
        number = "INV-2026-0001",
        partyId = customer.id,
        partyName = customer.name,
        phone = customer.phone,
        fields = mapOf("invoice_number" to "INV-2026-0001"),
        status = status,
        balanceMinor = breakdown.currentAccountBalanceMinor,
        previousMinor = breakdown.historicalPreviousBalanceMinor,
        documentDateMillis = t0,
        asOfMillis = breakdown.asOfMillis,
        brand = brandOverride,
        party = customer,
        lineItems = items,
        invoiceBreakdown = breakdown
    )

    @Test
    fun invoiceSpec_matchesApprovedReferenceAndRespectsVisibilityToggles() {
        val breakdown = InvoiceFinancialBreakdown(
            salesType = "WHOLESALE",
            grossItemsTotalMinor = 1380_00L,
            itemDiscountTotalMinor = 0L,
            subtotalMinor = 1380_00L,
            invoiceDiscountMinor = 0L,
            invoiceDiscountPercentBps = null,
            additionalChargesMinor = 0L,
            netInvoiceAmountMinor = 1380_00L,
            creditReturnMinor = 0L,
            allocatedPaymentMinor = 0L,
            invoiceDueMinor = 1380_00L,
            dueDateMillis = null,
            paymentTerms = "Cash on Delivery",
            notes = null,
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
        val doc = sampleInvoiceDoc(breakdown)
        val settingsAllOn = CommunicationSettings(
            showProductPhoto = true,
            showVariantSku = true,
            showQrCode = true,
            includeSignature = true
        )
        val spec = BrandedPdfSpecBuilder.buildSpec(doc, settingsAllOn, "bn", t0)

        assertEquals("INV-2026-0001", spec.safeFileStem)
        assertEquals("বিক্রয় ইনভয়েস", spec.headerRightTop)
        assertEquals("Sales Invoice", spec.headerRightBottom)
        assertEquals("https://tuktakbd.example", spec.contactStrip.website)
        assertEquals("002891928-0101", spec.contactStrip.binNumber)

        // Two-tier variant column split (#, Product, Color, Size, Qty, Unit Price, Amount)
        assertEquals(PdfTableMode.COMMERCIAL_VARIANT_SPLIT, spec.tableMode)
        assertEquals(7, spec.tableColumns.size)
        assertEquals(539f, spec.tableColumns.sumOf { it.widthPt.toDouble() }.toFloat(), 0.01f)
        assertEquals("ভ্যারিয়েন্ট (Variant)", spec.tableColumns[2].groupHeaderTitle)
        assertEquals("ভ্যারিয়েন্ট (Variant)", spec.tableColumns[3].groupHeaderTitle)
        assertEquals(1, spec.tableRows.size)
        assertTrue(spec.tableRows[0].showThumbnailSlot)
        assertEquals("SKU: 4DCI-WS-001", spec.tableRows[0].skuSubtitle)
        assertEquals("Gray", spec.tableRows[0].extraCells[0])
        assertEquals("37-38", spec.tableRows[0].extraCells[1])

        // Reconciled financial summary & purple pill total (29,560 + 1,380 = 30,940)
        assertTrue(spec.leftSummaryCard.rows.any { it.value.contains("1,380.00") })
        val rightPill = spec.rightSummaryCard!!.totalPill!!
        assertEquals("সর্বমোট বকেয়া", rightPill.label)
        assertEquals("৳ 30,940.00", rightPill.amountText)
        assertNull(spec.rightSummaryCard!!.footnote)

        // QR & Signature enabled
        assertNotNull(spec.bottomBlock.qrPayload)
        assertTrue(spec.bottomBlock.showAuthorizedSignature)

        // Visibility toggles + unconfigured website/BIN omitted cleanly
        val noExtrasBrand = brand.copy(website = "", binNumber = null)
        val minimalSettings = CommunicationSettings(
            showProductPhoto = false,
            showVariantSku = false,
            showQrCode = false,
            includeSignature = false
        )
        val hiddenSpec = BrandedPdfSpecBuilder.buildSpec(
            sampleInvoiceDoc(breakdown, brandOverride = noExtrasBrand),
            minimalSettings,
            "en",
            t0
        )
        assertNull(hiddenSpec.contactStrip.website)
        assertNull(hiddenSpec.contactStrip.binNumber)
        assertFalse(hiddenSpec.tableRows[0].showThumbnailSlot)
        assertNull(hiddenSpec.tableRows[0].skuSubtitle)
        assertNull(hiddenSpec.bottomBlock.qrPayload)
        assertFalse(hiddenSpec.bottomBlock.showAuthorizedSignature)
    }

    @Test
    fun invoiceSpec_handlesReshareLegacyMissingSnapshotAndNegativeCredit() {
        val resharedBreakdown = InvoiceFinancialBreakdown(
            salesType = "RETAIL",
            grossItemsTotalMinor = 2000_00L,
            itemDiscountTotalMinor = 200_00L,
            subtotalMinor = 1800_00L,
            invoiceDiscountMinor = 180_00L,
            invoiceDiscountPercentBps = 1000L,
            additionalChargesMinor = 50_00L,
            netInvoiceAmountMinor = 1670_00L,
            creditReturnMinor = 0L,
            allocatedPaymentMinor = 0L,
            invoiceDueMinor = 1670_00L,
            qrVerificationPayload = "ADV-ERP|INV|INV-2026-0001|20261127|1670.00|UNPAID",
            hasHistoricalSnapshot = true,
            historicalAsOfMillis = t0,
            historicalPreviousBalanceMinor = 1000_00L,
            historicalAllocatedPaymentMinor = 0L,
            historicalInvoiceDueMinor = 1670_00L,
            historicalClosingBalanceMinor = 2670_00L,
            currentAccountBalanceMinor = -500_00L, // Customer now has advance credit after later payment
            asOfMillis = t0 + 86_400_000L,
            isReshareAfterLaterActivity = true
        )
        val reshareSpec = BrandedPdfSpecBuilder.buildSpec(
            sampleInvoiceDoc(resharedBreakdown),
            CommunicationSettings(),
            "en",
            t0
        )
        assertNotNull(reshareSpec.rightSummaryCard!!.footnote)
        assertTrue(reshareSpec.rightSummaryCard!!.rows.any { it.label.contains("Prev. Bal") && it.value.contains("1,000.00") })
        assertTrue(reshareSpec.rightSummaryCard!!.rows.any { it.label.contains("Closing") && it.value.contains("2,670.00") })
        assertEquals("Advance Credit", reshareSpec.rightSummaryCard!!.totalPill!!.label)
        assertEquals("৳ 500.00", reshareSpec.rightSummaryCard!!.totalPill!!.amountText)
        assertTrue(reshareSpec.leftSummaryCard.rows.any { it.label.contains("10%") && it.value.contains("180.00") })

        val legacyNoSnap = resharedBreakdown.copy(
            hasHistoricalSnapshot = false,
            historicalPreviousBalanceMinor = null,
            historicalClosingBalanceMinor = null,
            currentAccountBalanceMinor = 1670_00L
        )
        val legacySpec = BrandedPdfSpecBuilder.buildSpec(sampleInvoiceDoc(legacyNoSnap), CommunicationSettings(), "en", t0)
        assertTrue(legacySpec.rightSummaryCard!!.rows.any { it.value.contains("Unavailable") })
    }

    @Test
    fun allModules_buildSpecsCleanlyWithPrivacyAndFinancialInvariants() {
        val sampleItem = RichDocumentLineItem(
            index = 1,
            productId = "p-1",
            titleEn = "Premium Insole",
            titleBn = "প্রিমিয়াম ইনসোল",
            sku = "SKU-1",
            color = "Black",
            size = "40-41",
            unit = "PCS",
            quantity = 4,
            secondaryQuantityText = "RETURNED",
            unitPriceMinor = 500_00L,
            lineTotalMinor = 2000_00L
        )
        val settings = CommunicationSettings(includeInstructions = true, paymentInstructions = "bKash 01700000000")

        for (mod in CommunicationModule.entries) {
            val isSup = mod.isSupplierModule()
            val doc = SafeDocument(
                request = CommunicationRequest(mod, "doc-1", from = t0 - 86_400_000L, to = t0),
                number = "${mod.name}-001",
                partyId = "party-1",
                partyName = "Test Party",
                phone = "01700000000",
                supplier = isSup,
                fields = mapOf(
                    "document_date" to "27 Nov 2026",
                    "document_amount" to "BDT 2000.00",
                    "subtotal_amount" to "BDT 2000.00",
                    "total_quantity" to "4",
                    "total_carton" to "2",
                    "payment_method" to "Cash",
                    "date_range" to "26 Nov 2026 - 27 Nov 2026"
                ),
                status = if (mod == CommunicationModule.INVOICE) CommunicationStatus.PARTIAL else CommunicationStatus.GENERAL,
                balanceMinor = if (mod == CommunicationModule.REMINDER) -1500_00L else 2000_00L,
                validUntilMillis = if (mod == CommunicationModule.QUOTATION) t0 + 86_400_000L else null,
                scheduledDeliveryInfo = if (mod == CommunicationModule.SALES_ORDER) "Van Dispatch" else null,
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
                invoiceBreakdown = if (mod == CommunicationModule.INVOICE) {
                    InvoiceFinancialBreakdown(
                        grossItemsTotalMinor = 2000_00L, itemDiscountTotalMinor = 0L, subtotalMinor = 2000_00L,
                        invoiceDiscountMinor = 0L, additionalChargesMinor = 0L, netInvoiceAmountMinor = 2000_00L,
                        creditReturnMinor = 0L, allocatedPaymentMinor = 500_00L, invoiceDueMinor = 1500_00L,
                        hasHistoricalSnapshot = true, historicalPreviousBalanceMinor = 500_00L,
                        historicalClosingBalanceMinor = 2000_00L, currentAccountBalanceMinor = 2000_00L,
                        asOfMillis = t0, isReshareAfterLaterActivity = false
                    )
                } else null,
                paymentBreakdown = if (mod in setOf(CommunicationModule.PAYMENT, CommunicationModule.SUPPLIER_PAYMENT)) {
                    PaymentReceiptBreakdown(
                        paymentNumber = "PAY-1", paymentDateMillis = t0, paymentAmountMinor = 1000_00L,
                        paymentMethod = "Cash", allocatedToInvoicesMinor = 1000_00L, unallocatedAdvanceMinor = 0L,
                        allocations = listOf(PaymentReceiptAllocation("i-1", "INV-1", 1000_00L)),
                        previousBalanceMinor = 3000_00L, balanceAfterPaymentMinor = 2000_00L,
                        currentAccountBalanceMinor = 2000_00L
                    )
                } else null,
                reminderBreakdown = if (mod == CommunicationModule.REMINDER) {
                    DueReminderBreakdown(
                        currentTotalDueMinor = -1500_00L, priorBalanceMinor = -1500_00L, asOfMillis = t0
                    )
                } else null,
                statementBreakdown = if (mod in setOf(CommunicationModule.LEDGER, CommunicationModule.STATEMENT, CommunicationModule.SUPPLIER_STATEMENT)) {
                    LedgerStatementBreakdown(
                        fromMillis = t0 - 86_400_000L, toMillis = t0, isAllTime = false,
                        openingBalanceMinor = 500_00L, totalSalesOrPurchasesMinor = 1500_00L,
                        totalReturnMinor = 0L, totalRefundPaidMinor = 0L, totalPaymentMinor = 0L,
                        netAdjustmentMinor = 0L, closingBalanceMinor = 2000_00L,
                        transactions = listOf(LedgerStatementRow(t0, "INV-1", "INVOICE", "Sales", 1500_00L, 0L, 2000_00L))
                    )
                } else null,
                returnExchangeBreakdown = if (mod in setOf(CommunicationModule.RETURN, CommunicationModule.EXCHANGE)) {
                    ReturnOrExchangeBreakdown(
                        referenceInvoiceNumber = "INV-1", returnAmountMinor = 2000_00L,
                        creditedToAccountMinor = 2000_00L, netPriceDifferenceMinor = 500_00L,
                        refundOrPaymentMethod = "CUSTOMER_CREDIT", currentAccountBalanceMinor = 2000_00L
                    )
                } else null,
                deliveryBreakdown = if (mod == CommunicationModule.DELIVERY) {
                    DeliveryChallanBreakdown("INV-1", "Kamal", "Van", totalDispatchedQuantity = 4)
                } else null
            )

            for (lang in listOf("bn", "en")) {
                val spec = BrandedPdfSpecBuilder.buildSpec(doc, settings, lang, t0)
                assertTrue("Header badge empty for $mod/$lang", spec.headerRightTop.isNotBlank())
                assertTrue("Notes empty for $mod/$lang", spec.bottomBlock.notesBullets.isNotEmpty())
                if (spec.tableColumns.isNotEmpty()) {
                    assertEquals(
                        "Table columns must sum to 539pt for $mod/$lang",
                        539f,
                        spec.tableColumns.sumOf { it.widthPt.toDouble() }.toFloat(),
                        0.05f
                    )
                }
                if (mod == CommunicationModule.REMINDER) {
                    assertEquals(BrandedDocumentPalette.CreditBlueText, spec.heroBanner!!.valueColor)
                    assertEquals(
                        if (lang == "bn") "বর্তমান অগ্রিম ক্রেডিট" else "Advance Credit",
                        spec.rightSummaryCard!!.totalPill!!.label
                    )
                }
                if (mod == CommunicationModule.DELIVERY) {
                    assertNotNull(spec.bottomBlock.receiverSignatureLabel)
                    assertTrue(spec.tableColumns.none { it.id == "unit_price" || it.id == "amount" })
                }
            }
        }
    }

    @Test
    fun pagination_coversAllRowsContiguouslyAndRepeatsHeadersCleanlyAcrossPages() {
        // Small 3-row invoice fits on 1 page
        val singleSlices = BrandedPdfSpecBuilder.paginateDocument(
            rowHeights = listOf(34f, 34f, 34f),
            firstPageTopHeight = 200f,
            contPageTopHeight = 44f,
            sectionTitleHeight = 18f,
            tableHeaderHeight = 28f,
            summaryHeight = 130f,
            bottomHeight = 115f,
            usableBottomY = 796f
        )
        assertEquals(1, singleSlices.size)
        assertEquals(0, singleSlices[0].rowStartIndex)
        assertEquals(3, singleSlices[0].rowEndExclusive)
        assertTrue(singleSlices[0].includeSummarySection)
        assertTrue(singleSlices[0].includeBottomSection)

        // 35 rows must span multiple pages without dropping/duplicating rows, placing summary+bottom exactly once
        val largeRows = List(35) { 40f }
        val multiSlices = BrandedPdfSpecBuilder.paginateDocument(
            rowHeights = largeRows,
            firstPageTopHeight = 210f,
            contPageTopHeight = 44f,
            sectionTitleHeight = 18f,
            tableHeaderHeight = 28f,
            summaryHeight = 140f,
            bottomHeight = 120f,
            usableBottomY = 796f
        )
        assertTrue("Expected multi-page split for 35 rows, got ${multiSlices.size}", multiSlices.size >= 3)
        var expectedNextRow = 0
        var summaryCount = 0
        var bottomCount = 0
        for ((idx, slice) in multiSlices.withIndex()) {
            assertEquals(idx + 1, slice.pageNumber)
            assertEquals(idx == 0, slice.isFirstPage)
            assertEquals("Row continuity broken on page ${slice.pageNumber}", expectedNextRow, slice.rowStartIndex)
            expectedNextRow = slice.rowEndExclusive
            if (slice.includeSummarySection) summaryCount++
            if (slice.includeBottomSection) bottomCount++
        }
        assertEquals(35, expectedNextRow)
        assertEquals(1, summaryCount)
        assertEquals(1, bottomCount)

        // Zero rows (Due Reminder) stays on 1 page
        val emptySlices = BrandedPdfSpecBuilder.paginateDocument(
            rowHeights = emptyList(),
            firstPageTopHeight = 240f,
            contPageTopHeight = 44f,
            sectionTitleHeight = 0f,
            tableHeaderHeight = 0f,
            summaryHeight = 120f,
            bottomHeight = 110f,
            usableBottomY = 796f
        )
        assertEquals(1, emptySlices.size)
        assertTrue(emptySlices[0].includeSummarySection)
        assertTrue(emptySlices[0].includeBottomSection)
    }
}
