package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.LedgerReportEngine
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.core.util.CustomReportPdfConfig
import com.pocketdev.advance_erp.core.util.DueCollectionCaptionHelper
import com.pocketdev.advance_erp.core.util.DueCollectionStatementData
import com.pocketdev.advance_erp.core.util.ImportExcelGenerator
import com.pocketdev.advance_erp.core.util.PoExportDataBuilder
import com.pocketdev.advance_erp.core.util.PoNotesMetadataParser
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.repository.AccountingRepositoryImpl
import com.pocketdev.advance_erp.domain.model.ControlCenterFilterState
import com.pocketdev.advance_erp.domain.model.DateRangeOption
import com.pocketdev.advance_erp.domain.model.LedgerParty
import com.pocketdev.advance_erp.domain.model.PaymentTermType
import com.pocketdev.advance_erp.domain.model.PaymentTermsCalculator
import com.pocketdev.advance_erp.domain.model.ReportCategory
import com.pocketdev.advance_erp.domain.model.UnifiedTransactionItem
import com.pocketdev.advance_erp.domain.repository.DateWiseMetrics
import com.pocketdev.advance_erp.feature.imports.PoCategoryLandedInput
import com.pocketdev.advance_erp.feature.imports.calculatePoCategoryLandedBreakdowns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

class Phase6UserRequestsRegressionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun customPartialPaymentCalculatorComputesAdvanceAndRemainingBalance() {
        val schedule = PaymentTermsCalculator.calculateSchedule(
            termType = PaymentTermType.CUSTOM_PARTIAL,
            totalAmountMinor = 500_000L, // 5,000.00 CNY
            exchangeRateToBdt = BigDecimal("17.00"),
            customAdvanceAmountMinor = 200_000L // 2,000.00 CNY
        )
        assertEquals(2, schedule.size)
        assertEquals(200_000L, schedule[0].amountMinor)
        assertEquals(300_000L, schedule[1].amountMinor)
        assertEquals(0, BigDecimal("40").compareTo(schedule[0].percentage))
        assertEquals(0, BigDecimal("60").compareTo(schedule[1].percentage))
        assertEquals(3_400_000L, schedule[0].bdtAmountMinor)
        assertEquals(5_100_000L, schedule[1].bdtAmountMinor)
    }

    @Test
    fun poNotesParserRoundTripsCustomAdvanceAndFormattedTerms() {
        val formattedTerms = PoNotesMetadataParser.formatCustomPaymentTermsLabel(
            currency = "CNY",
            totalAmountMinor = 500_000L,
            customAdvanceMinor = 200_000L
        )
        val raw = PoNotesMetadataParser.serialize(
            paymentTermLabel = formattedTerms,
            forwarderAddress = "Yiwu Warehouse 88",
            userNotes = "Handle carefully",
            defaultShippingMark = "TUKTAK BD / {CTN}",
            customAdvanceMinor = 200_000L
        )
        val parsed = PoNotesMetadataParser.parse(raw)
        assertEquals(PaymentTermType.CUSTOM_PARTIAL, parsed.paymentTerm)
        assertEquals(200_000L, parsed.customAdvanceMinor)
        assertTrue(parsed.rawTermsText.contains("2,000.00"))
        assertTrue(parsed.rawTermsText.contains("3,000.00"))
        assertEquals("Handle carefully", parsed.userNotes)
    }

    @Test
    fun freightForwarderExcelIncludesPhotosDimensionsWeightsCbmAndExcludesColorAndPrices() {
        val product = Product(
            id = "p-ff-1", categoryId = "cat-1", nameEn = "4D CLOUD INSOLE PU", nameBn = "৪ডি ইনসোল",
            baseSku = "SA9-4CI-001", purchasePriceMinor = 210L, sellingPriceMinor = 500L
        )
        val variant = ProductVariant(
            "v-ff-1", "p-ff-1", "SA9-4CI-001-RED-40",
            color = "CrimsonRedColor", size = "40-41",
            variantCostMinor = 210L, variantPriceMinor = 500L
        )
        val order = PurchaseOrder(
            id = "po-ff-1", poNumber = "PO-2026-099", supplierId = "sup-1",
            status = PurchaseOrderStatus.CONFIRMED.name, currency = "CNY", exchangeRateToBdt = "17.50",
            totalAmountMinor = 84_000L, totalBdtAmountMinor = 1_470_000L, totalCartons = 1,
            totalGrossWeightKg = "30.000", totalNetWeightKg = "28.000", totalCbm = "0.108376",
            notes = "[Terms: 30% Advance + 70% Balance] • [Forwarder: Yiwu Warehouse 18] • [ShippingMark: TUKTAK BD / PO-2026-099 / {CTN}]"
        )
        val items = listOf(
            PurchaseOrderItem("i-ff-1", "po-ff-1", "p-ff-1", "v-ff-1", 400, 210L, 84_000L, 1_470_000L, cartonQuantity = 1, pcsPerCarton = 400)
        )
        val cartons = listOf(
            ShipmentCarton("c-ff-1", "shp-1", "CTN-001", "p-ff-1", "v-ff-1", 400, "62", "46", "38", "30.000", "28.000", "0.108376", "po-ff-1", isMixedCarton = false, totalCartonQuantity = 400, cartonGrossWeightKg = "30.000")
        )
        val supplier = Supplier("sup-1", "SUP-1", "Guangzhou Shoes Factory", "Lin", "15200001111", address = "Guangzhou", country = "China")
        val profile = BusinessProfile(companyName = "TUKTAK BD", phone = "+8801639484547", address = "41 New Elephant Road, Dhaka")

        val fakeJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val ffXlsx = tempFolder.newFile("forwarder_packing.xlsx")
        ImportExcelGenerator.generateFreightForwarderPackingListExcelToFile(
            file = ffXlsx,
            order = order,
            items = items,
            productsMap = mapOf(product.id to product),
            supplier = supplier,
            businessProfile = profile,
            variantsMap = mapOf(variant.id to variant),
            itemImagePaths = mapOf("${product.id}:${variant.id}" to "/fake/variant_photo.jpg"),
            cartons = cartons,
            thumbnailBytesProvider = { fakeJpeg }
        )

        ZipFile(ffXlsx).use { zip ->
            val wb = zip.getInputStream(zip.getEntry("xl/workbook.xml")).readBytes().toString(StandardCharsets.UTF_8)
            assertTrue(wb.contains("Packing List"))
            assertTrue(wb.contains("Carton Summary"))
            assertTrue(wb.contains("Forwarder &amp; Delivery"))

            assertTrue(zip.getEntry("xl/drawings/drawing1.xml") != null)
            assertTrue(zip.getEntry("xl/media/image1.jpg") != null)

            val s1 = zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).readBytes().toString(StandardCharsets.UTF_8)
            val s2 = zip.getInputStream(zip.getEntry("xl/worksheets/sheet2.xml")).readBytes().toString(StandardCharsets.UTF_8)
            val s3 = zip.getInputStream(zip.getEntry("xl/worksheets/sheet3.xml")).readBytes().toString(StandardCharsets.UTF_8)

            // Photos, Weight, CBM, Dimensions, Shipping Mark included
            assertTrue(s1.contains("Photo"))
            assertTrue(s1.contains("4D CLOUD INSOLE PU"))
            assertTrue(s1.contains("30.000"))
            assertTrue(s1.contains("28.000"))
            assertTrue(s1.contains("0.1084"))
            assertTrue(s1.contains("TUKTAK BD / PO-2026-099 / CTN-001"))

            // Privacy: NO Color, NO Prices, NO Payment Terms, NO "ADVANCE ERP • " prefix
            listOf(s1, s2, s3).forEach { sheet ->
                assertFalse(sheet.contains("CrimsonRedColor"))
                assertFalse(sheet.contains(">Color<"))
                assertFalse(sheet.contains("Payment Terms"))
                assertFalse(sheet.contains("840.00"))
                assertFalse(sheet.contains("Total Amount"))
                assertFalse(sheet.contains("ADVANCE ERP •"))
            }
        }

        // Also check supplier PO excel omits "ADVANCE ERP • " prefix
        val supXlsx = tempFolder.newFile("supplier_banner.xlsx")
        ImportExcelGenerator.generateSupplierOrderExcelToFile(
            file = supXlsx,
            order = order,
            items = items,
            productsMap = mapOf(product.id to product),
            supplier = supplier,
            businessProfile = profile,
            variantsMap = mapOf(variant.id to variant),
            cartons = cartons
        )
        ZipFile(supXlsx).use { zip ->
            val s1 = zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).readBytes().toString(StandardCharsets.UTF_8)
            assertFalse(s1.contains("ADVANCE ERP • "))
            assertTrue(s1.contains("TUKTAK BD"))
        }
    }

    @Test
    fun bankAndMfsDefaultAccountsAreDeletableWhenZeroBalanceWhileMainCashRemainsProtected() {
        assertTrue(AccountingRepositoryImpl.MAIN_CASH_ACCOUNT_ID in AccountingRepositoryImpl.PROTECTED_ACCOUNT_IDS)
        assertFalse("coa-1020" in AccountingRepositoryImpl.PROTECTED_ACCOUNT_IDS)
        assertFalse("coa-1040" in AccountingRepositoryImpl.PROTECTED_ACCOUNT_IDS)
        assertFalse("coa-1050" in AccountingRepositoryImpl.PROTECTED_ACCOUNT_IDS)
    }

    @Test
    fun dueCollectionWhatsappCaptionBoldsFullSummaryLines() {
        val data = DueCollectionStatementData(
            customerId = "c-1",
            customerName = "Rahim Traders",
            customerPhone = "01700000000",
            customerCode = "CUST-01",
            companyName = "TUKTAK BD",
            companyPhone = "01639484547",
            paymentNumber = "PAY-101",
            paymentDate = 1_795_200_000_000L,
            paymentMethod = "CASH",
            amountReceivedMinor = 40_000L,
            previousDueMinor = 100_000L,
            currentDueMinor = 60_000L,
            isFullPayment = false,
            isPartialPayment = true,
            isAdvancePayment = false
        )
        val bnCaption = DueCollectionCaptionHelper.generateCaption(data, isBangla = true)
        assertTrue(bnCaption.contains("*পূর্বের মোট বকেয়া: ৳1,000.00*"))
        assertTrue(bnCaption.contains("*আজকের জমার পরিমাণ: ৳400.00*"))
        assertTrue(bnCaption.contains("*বর্তমান অবশিষ্ট বকেয়া: ৳600.00*"))

        val enCaption = DueCollectionCaptionHelper.generateCaption(data, isBangla = false)
        assertTrue(enCaption.contains("*Previous Total Due: ৳1,000.00*"))
        assertTrue(enCaption.contains("*Amount Received Today: ৳400.00*"))
        assertTrue(enCaption.contains("*Current Outstanding Due: ৳600.00*"))
    }

    @Test
    fun centralizedCustomerDueMatchesAcrossSummaryAndStatementForRahim() {
        val rahim = LedgerParty("c-rahim", "Rahim", "CUST-R", "CUSTOMER", "01711111111", null)
        // Suppose Rahim has 200,000 opening + 300,000 unpaid invoice = 500,000 (৳5,000.00) canonical balance
        val rows = listOf(
            UnifiedTransactionItem(
                id = "open-rahim", referenceNumber = "OPENING", date = 0L,
                category = ReportCategory.CUSTOMER_LEDGER, primaryEntityName = "Rahim", primaryEntityId = "c-rahim",
                particulars = "Opening balance", amountMinor = 200_000L, isInflow = true, status = "OPENING",
                rawType = "OPENING_CUSTOMER", effectSign = 1
            ),
            UnifiedTransactionItem(
                id = "inv-rahim", referenceNumber = "INV-101", date = 1_795_200_000_000L,
                category = ReportCategory.SALES, primaryEntityName = "Rahim", primaryEntityId = "c-rahim",
                particulars = "Credit Sale", amountMinor = 300_000L, secondaryAmountMinor = 300_000L,
                isInflow = true, status = "POSTED", rawType = "INVOICE", effectSign = 1
            )
        )
        val filter = ControlCenterFilterState(
            dateRangeOption = DateRangeOption.ALL_TIME,
            category = ReportCategory.CUSTOMER_LEDGER,
            selectedCustomerId = "c-rahim"
        )
        val report = LedgerReportEngine.build(rows, filter, rahim, 1_795_300_000_000L)
        assertEquals(500_000L, report.statement?.closingBalanceMinor)
        assertEquals(500_000L, report.summary.totalOutstandingReceivableMinor)
    }

    @Test
    fun dateWiseMetricsSupportsSingleAndRangeMode() {
        val zero = Money.ofMinor(0L, CurrencyCode.BDT)
        val singleDay = DateWiseMetrics(
            selectedDateMillis = 1_795_200_000_000L,
            totalSales = zero, totalPurchases = zero, totalCollections = zero,
            totalPayments = zero, totalExpenses = zero, totalReturns = zero,
            stockInUnits = 0, stockOutUnits = 0, netCashFlow = zero,
            dailyGrossProfit = zero, transactionCount = 0
        )
        assertFalse(singleDay.isRangeMode)

        val multiDay = singleDay.copy(
            startDateMillis = 1_794_600_000_000L,
            endDateMillis = 1_795_200_000_000L
        )
        assertTrue(multiDay.isRangeMode)

        val customConfig = CustomReportPdfConfig(title = "Weekly Audit", includeCustomerDues = true)
        assertTrue(customConfig.includeCustomerDues)
        assertTrue(customConfig.includeAuthorizedSignature)
    }

    @Test
    fun categoryWisePoLandingCostComputesAccuratePerCategoryAveragePerPcsAndReconcilesGrandTotal() {
        val inputs = listOf(
            PoCategoryLandedInput(
                categoryId = "cat-other",
                categoryTitle = "Sneakers",
                totalPcs = 1000,
                grossWeightKg = BigDecimal("120.00"),
                productCostBdt = BigDecimal("50000.00"),
                freightCostBdt = BigDecimal("38400.00")
            ),
            PoCategoryLandedInput(
                categoryId = "cat-4d",
                categoryTitle = "4D Cloud Technology",
                totalPcs = 942,
                grossWeightKg = BigDecimal("70.00"),
                productCostBdt = BigDecimal("45640.00"),
                freightCostBdt = BigDecimal("22400.00")
            )
        )
        // Shared overhead: China Local 6000 + Handling 2000 + Sample 5000 = 13000.00
        val sharedOverhead = BigDecimal("13000.00")
        val breakdowns = calculatePoCategoryLandedBreakdowns(
            inputs = inputs,
            sharedOverheadBdt = sharedOverhead,
            expectedTotalProductBdt = BigDecimal("95640.00"),
            expectedTotalFreightBdt = BigDecimal("60800.00")
        )

        assertEquals(2, breakdowns.size)
        val totalSum = breakdowns.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.totalLandedCostBdt) }
        assertEquals(BigDecimal("169440.00"), totalSum)

        val sneakers = breakdowns[0]
        val cloud4d = breakdowns[1]
        // Sneakers overhead share: 13000 * 1000 / 1942 = 6694.12 -> total 50000 + 38400 + 6694.12 = 95094.12 -> /1000 = 95.09
        // 4D overhead share: 13000 - 6694.12 = 6305.88 -> total 45640 + 22400 + 6305.88 = 74345.88 -> /942 = 78.92
        assertEquals(BigDecimal("95094.12"), sneakers.totalLandedCostBdt)
        assertEquals(BigDecimal("95.09"), sneakers.averagePerPcsBdt)
        assertEquals(BigDecimal("74345.88"), cloud4d.totalLandedCostBdt)
        assertEquals(BigDecimal("78.92"), cloud4d.averagePerPcsBdt)
    }

    @Test
    fun singleCategoryPoLandingCostMatchesOverallAveragePerPcs() {
        val inputs = listOf(
            PoCategoryLandedInput(
                categoryId = "cat-4d",
                categoryTitle = "4D Cloud Technology",
                totalPcs = 1942,
                grossWeightKg = BigDecimal("190.00"),
                productCostBdt = BigDecimal("95640.00"),
                freightCostBdt = BigDecimal("60800.00")
            )
        )
        val breakdowns = calculatePoCategoryLandedBreakdowns(
            inputs = inputs,
            sharedOverheadBdt = BigDecimal("13000.00"),
            expectedTotalProductBdt = BigDecimal("95640.00"),
            expectedTotalFreightBdt = BigDecimal("60800.00")
        )
        assertEquals(1, breakdowns.size)
        assertEquals(BigDecimal("169440.00"), breakdowns[0].totalLandedCostBdt)
        assertEquals(BigDecimal("87.25"), breakdowns[0].averagePerPcsBdt)
    }

    @Test
    fun forwarderPackingListResolvesCartonLineImagesEvenWhenKeyedByPoItemId() {
        val product = Product(
            id = "p-img-1", categoryId = "cat-1", nameEn = "EVA Height", nameBn = "ইভা হাইট",
            baseSku = "HE-EVA-003", purchasePriceMinor = 100L, sellingPriceMinor = 200L
        )
        val v3940 = ProductVariant(
            "v-3940", "p-img-1", "HE-EVA-003-BLA-3940", color = "Black", size = "39-40",
            variantCostMinor = 100L, variantPriceMinor = 200L
        )
        val v4142 = ProductVariant(
            "v-4142", "p-img-1", "HE-EVA-003-BLA-4142", color = "Black", size = "41-42",
            variantCostMinor = 100L, variantPriceMinor = 200L
        )
        val order = PurchaseOrder(
            id = "po-img-1", poNumber = "PO-2026-001", supplierId = "sup-1", status = PurchaseOrderStatus.CONFIRMED.name,
            orderDate = 1_795_370_000_000L, currency = "CNY", exchangeRateToBdt = "20.0",
            totalAmountMinor = 4_000L, totalBdtAmountMinor = 80_000L, totalCartons = 1,
            totalGrossWeightKg = "5.000", totalNetWeightKg = "5.000", totalCbm = "0.050000"
        )
        val items = listOf(
            PurchaseOrderItem("poi-1", "po-img-1", "p-img-1", "v-3940", 20, 100L, 2_000L, 40_000L),
            PurchaseOrderItem("poi-2", "po-img-1", "p-img-1", "v-4142", 20, 100L, 2_000L, 40_000L)
        )
        val cartons = listOf(
            ShipmentCarton("c-1", "shp-1", "CTN-001", "p-img-1", "v-3940", 20, "40", "30", "25", "2.500", "2.500", "0.030000", "po-img-1", isMixedCarton = true, totalCartonQuantity = 40, cartonGrossWeightKg = "5.000"),
            ShipmentCarton("c-2", "shp-1", "CTN-001", "p-img-1", "v-4142", 20, "40", "30", "25", "2.500", "2.500", "0.030000", "po-img-1", isMixedCarton = true, totalCartonQuantity = 40, cartonGrossWeightKg = "5.000")
        )
        val profile = BusinessProfile(companyName = "TUKTAK BD", phone = "+8801639484547", address = "41 New Elephant Road, Dhaka")
        // Simulate legacy caller passing only item.id keys
        val itemKeyedImages = mapOf("poi-1" to "/fake/eva_3940.jpg")

        val exportData = PoExportDataBuilder.build(
            order = order,
            items = items,
            productsMap = mapOf(product.id to product),
            supplier = null,
            businessProfile = profile,
            variantsMap = mapOf(v3940.id to v3940, v4142.id to v4142),
            itemImagePaths = itemKeyedImages,
            cartons = cartons
        )

        assertEquals(1, exportData.allCartons.size)
        assertEquals("/fake/eva_3940.jpg", exportData.allCartons[0].lines[0].imagePath)
        assertEquals("/fake/eva_3940.jpg", exportData.allCartons[0].lines[1].imagePath)

        val fakeJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val ffXlsx = tempFolder.newFile("forwarder_item_keyed_photos.xlsx")
        ImportExcelGenerator.generateFreightForwarderPackingListExcelToFile(
            file = ffXlsx,
            order = order,
            items = items,
            productsMap = mapOf(product.id to product),
            supplier = null,
            businessProfile = profile,
            variantsMap = mapOf(v3940.id to v3940, v4142.id to v4142),
            itemImagePaths = itemKeyedImages,
            cartons = cartons,
            thumbnailBytesProvider = { fakeJpeg }
        )

        ZipFile(ffXlsx).use { zip ->
            assertTrue(zip.getEntry("xl/media/image1.jpg") != null)
            assertTrue(zip.getEntry("xl/drawings/drawing1.xml") != null)
        }
    }
}
