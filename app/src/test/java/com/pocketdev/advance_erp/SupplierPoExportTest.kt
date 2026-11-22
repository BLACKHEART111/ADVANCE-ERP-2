package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.util.ImportExcelGenerator
import com.pocketdev.advance_erp.core.util.PoNotesMetadataParser
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

class SupplierPoExportTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun poNotesMetadataParserExtractsTermsForwarderNotesAndCleanShippingMark() {
        val raw = "[Terms: 50% Advance + 50% On Delivery] • [Forwarder: Yiwu International Port Warehouse 88] • Please inspect packing quality"
        val parsed = PoNotesMetadataParser.parse(raw)

        assertEquals("50% Advance + 50% On Delivery", parsed.rawTermsText)
        assertEquals("Yiwu International Port Warehouse 88", parsed.forwarderAddress)
        assertEquals("Please inspect packing quality", parsed.userNotes)

        val intlMark = PoNotesMetadataParser.buildShippingMark("My Business", "PO-2026-CN-001", isLocalOrder = false)
        assertEquals("My Business / PO-2026-CN-001 / CTG PORT", intlMark)
        assertFalse(intlMark.contains("["))
        assertFalse(intlMark.contains("]"))

        val localMark = PoNotesMetadataParser.buildShippingMark("My Business", "PO-2026-BD-001", isLocalOrder = true)
        assertEquals("My Business / PO-2026-BD-001", localMark)
        val customNotes = PoNotesMetadataParser.serialize(
            paymentTermLabel = "50% Advance + 50% On Delivery",
            forwarderAddress = "Yiwu International Port Warehouse 88",
            userNotes = "Please inspect packing quality",
            defaultShippingMark = "TUKTAK BD / {CTN}",
            cartonShippingMarks = mapOf("CTN-001" to "FRAGILE {CTN}")
        )
        val parsedCustom = PoNotesMetadataParser.parse(customNotes)
        assertEquals("50% Advance + 50% On Delivery", parsedCustom.rawTermsText)
        assertEquals("Yiwu International Port Warehouse 88", parsedCustom.forwarderAddress)
        assertEquals("Please inspect packing quality", parsedCustom.userNotes)
        assertEquals("TUKTAK BD / {CTN}", parsedCustom.defaultShippingMark)
        assertEquals(mapOf("CTN-001" to "FRAGILE {CTN}"), parsedCustom.cartonShippingMarks)
    }

    @Test
    fun supplierExcelIncludesApproved4SheetsVariantsPhotosCartonNumbersAndFormulas() {
        val product = Product(
            id = "prod-1",
            categoryId = "cat-1",
            nameEn = "DOCTOR INSOLE",
            nameBn = "ডক্টর ইনসোল",
            baseSku = "SAB-DOC-001",
            purchasePriceMinor = 200L,
            sellingPriceMinor = 500L
        )
        val variant1 = ProductVariant(
            id = "var-1",
            productId = "prod-1",
            sku = "SAB-DOC-001-BRN-40",
            color = "Brown",
            size = "40",
            variantPriceMinor = 500L,
            variantCostMinor = 200L
        )
        val variant2 = ProductVariant(
            id = "var-2",
            productId = "prod-1",
            sku = "SAB-DOC-001-BLK-42",
            color = "Black",
            size = "42",
            variantPriceMinor = 500L,
            variantCostMinor = 200L
        )
        val order = PurchaseOrder(
            id = "po-1",
            poNumber = "PO-2026-CN-003",
            supplierId = "sup-1",
            status = PurchaseOrderStatus.CONFIRMED.name,
            currency = "CNY",
            exchangeRateToBdt = "17.5000",
            totalAmountMinor = 200000L,
            totalBdtAmountMinor = 3500000L,
            totalCartons = 2,
            totalGrossWeightKg = "45.5",
            notes = "[Terms: 30% Advance + 70% Balance] • [Forwarder: Guangzhou Baiyun Cargo Terminal] • Pack double bubble wrap"
        )
        val items = listOf(
            PurchaseOrderItem(
                id = "item-1",
                purchaseOrderId = "po-1",
                productId = "prod-1",
                variantId = "var-1",
                quantity = 400,
                unitPriceMinor = 200L,
                totalAmountMinor = 80000L,
                totalBdtAmountMinor = 1400000L,
                cartonQuantity = 1,
                pcsPerCarton = 400
            ),
            PurchaseOrderItem(
                id = "item-2",
                purchaseOrderId = "po-1",
                productId = "prod-1",
                variantId = "var-2",
                quantity = 600,
                unitPriceMinor = 200L,
                totalAmountMinor = 120000L,
                totalBdtAmountMinor = 2100000L,
                cartonQuantity = 1,
                pcsPerCarton = 600
            )
        )
        val cartons = listOf(
            ShipmentCarton(
                id = "c-1",
                shipmentId = "ship-1",
                cartonNumber = "1",
                productId = "prod-1",
                variantId = "var-1",
                quantity = 200,
                purchaseOrderId = "po-1",
                isMixedCarton = true
            ),
            ShipmentCarton(
                id = "c-2",
                shipmentId = "ship-1",
                cartonNumber = "1",
                productId = "prod-1",
                variantId = "var-2",
                quantity = 300,
                purchaseOrderId = "po-1",
                isMixedCarton = true
            )
        )
        val profile = BusinessProfile(
            companyName = "My Business",
            phone = "01700000000",
            address = "Dhaka"
        )
        val outFile = tempFolder.newFile("supplier_po_test.xlsx")
        val fakeJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())

        ImportExcelGenerator.generateSupplierOrderExcelToFile(
            file = outFile,
            order = order,
            items = items,
            productsMap = mapOf(product.id to product),
            supplier = null,
            businessProfile = profile,
            variantsMap = mapOf(variant1.id to variant1, variant2.id to variant2),
            itemImagePaths = mapOf("item-1" to "/fake/var1.jpg"),
            cartons = cartons,
            thumbnailBytesProvider = { path -> if (path == "/fake/var1.jpg") fakeJpeg else null }
        )

        ZipFile(outFile).use { zip ->
            val sheetXml = zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).readBytes().toString(StandardCharsets.UTF_8)
            val sheet2Xml = zip.getInputStream(zip.getEntry("xl/worksheets/sheet2.xml")).readBytes().toString(StandardCharsets.UTF_8)
            val sheet3Xml = zip.getInputStream(zip.getEntry("xl/worksheets/sheet3.xml")).readBytes().toString(StandardCharsets.UTF_8)
            val sheet4Xml = zip.getInputStream(zip.getEntry("xl/worksheets/sheet4.xml")).readBytes().toString(StandardCharsets.UTF_8)
            val workbookXml = zip.getInputStream(zip.getEntry("xl/workbook.xml")).readBytes().toString(StandardCharsets.UTF_8)
            val contentTypesXml = zip.getInputStream(zip.getEntry("[Content_Types].xml")).readBytes().toString(StandardCharsets.UTF_8)
            val drawingEntry = zip.getEntry("xl/drawings/drawing1.xml")
            val mediaEntry = zip.getEntry("xl/media/image1.jpg")

            assertTrue("Drawing XML missing", drawingEntry != null)
            assertTrue("Embedded image1.jpg missing", mediaEntry != null)
            assertTrue("Content types should register jpg", contentTypesXml.contains("Extension=\"jpg\""))
            assertTrue("Workbook missing 4 sheets", workbookXml.contains("Purchase Order") && workbookXml.contains("Packing List (Carton Wise)") && workbookXml.contains("Carton Summary") && workbookXml.contains("Shipping Mark"))

            val drawingXml = zip.getInputStream(drawingEntry).readBytes().toString(StandardCharsets.UTF_8)
            assertTrue(drawingXml.contains("<xdr:col>3</xdr:col>"))
            assertTrue(drawingXml.contains("r:embed=\"rId1\""))

            // 10-Column Reference Header check
            listOf("Product Name", "SKU", "Photo", "Color", "Size", "CTN", "Qty (pcs)", "Unit (CNY)", "Total (CNY)").forEach { header ->
                assertTrue("Missing header: $header", sheetXml.contains(header))
            }

            // Variant info, SKU, Formula & actual CTN-001 mapping check
            assertTrue(sheetXml.contains("SAB-DOC-001-BRN-40"))
            assertTrue(sheetXml.contains("Brown"))
            assertTrue(sheetXml.contains("Black"))
            assertTrue(sheetXml.contains("CTN-001 (200)"))
            assertTrue(sheetXml.contains("<f>H"))
            assertTrue(sheetXml.contains("<drawing r:id=\"rId1\"/>"))

            // Clean Bottom Info sections & no raw tags
            assertFalse(sheetXml.contains("[Terms:"))
            assertFalse(sheetXml.contains("[Forwarder:"))
            assertFalse(sheetXml.contains("[My Business]"))
            assertTrue(sheetXml.contains("DELIVERY &amp; COMMERCIAL TERMS"))
            assertTrue(sheetXml.contains("China Warehouse Address"))
            assertTrue(sheetXml.contains("Guangzhou Baiyun Cargo Terminal"))
            assertTrue(sheetXml.contains("My Business / PO-2026-CN-003 / CTG PORT"))
            assertTrue(sheetXml.contains("Payment Terms"))
            assertTrue(sheetXml.contains("30% Advance + 70% Balance"))
            assertTrue(sheetXml.contains("Note"))
            assertTrue(sheetXml.contains("Pack double bubble wrap"))
            assertTrue(sheet2Xml.contains("CTN-001") && sheet2Xml.contains("Mixed"))
            assertTrue(sheet3Xml.contains("CTN-001"))
            assertTrue(sheet4Xml.contains("My Business / PO-2026-CN-003 / CTG PORT"))
        }
    }

    @Test
    fun supplierExcelHidesZeroCartonsAndZeroGrossWeight() {
        val product = Product(
            id = "prod-1",
            categoryId = "cat-1",
            nameEn = "INSOLE",
            nameBn = "ইনসোল",
            baseSku = "SKU-1",
            purchasePriceMinor = 100L,
            sellingPriceMinor = 200L
        )
        val order = PurchaseOrder(
            id = "po-zero",
            poNumber = "PO-ZERO-1",
            supplierId = "sup-1",
            status = PurchaseOrderStatus.DRAFT.name,
            currency = "BDT",
            exchangeRateToBdt = "1.0000",
            totalAmountMinor = 1000L,
            totalBdtAmountMinor = 1000L,
            totalCartons = 0,
            totalGrossWeightKg = "0.000"
        )
        val items = listOf(
            PurchaseOrderItem(
                id = "i-1",
                purchaseOrderId = "po-zero",
                productId = "prod-1",
                quantity = 10,
                unitPriceMinor = 100L,
                totalAmountMinor = 1000L,
                totalBdtAmountMinor = 1000L,
                cartonQuantity = 0,
                pcsPerCarton = 0
            )
        )
        val outFile = tempFolder.newFile("supplier_po_zero.xlsx")
        ImportExcelGenerator.generateSupplierOrderExcelToFile(
            file = outFile,
            order = order,
            items = items,
            productsMap = mapOf(product.id to product),
            supplier = null,
            businessProfile = null
        )

        ZipFile(outFile).use { zip ->
            val sheetXml = zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).readBytes().toString(StandardCharsets.UTF_8)
            val sheet2Xml = zip.getInputStream(zip.getEntry("xl/worksheets/sheet2.xml")).readBytes().toString(StandardCharsets.UTF_8)
            assertTrue(sheetXml.contains("0 pcs"))
            assertFalse(sheet2Xml.contains("CTN-001"))
            assertTrue(sheetXml.contains("Delivery Address"))
        }
    }
}