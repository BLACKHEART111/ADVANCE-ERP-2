package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.PackingCartonLine
import com.pocketdev.advance_erp.core.engine.PackingIssueType
import com.pocketdev.advance_erp.core.engine.PackingOrderLine
import com.pocketdev.advance_erp.core.engine.PoPackingEngine
import com.pocketdev.advance_erp.core.engine.VariantKey
import com.pocketdev.advance_erp.core.engine.VariantVisibility
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

class PoSmartPackingAndExportTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val insoleBlack39 = VariantKey("prod-cloud", "var-blk-39")
    private val insoleGray39 = VariantKey("prod-cloud", "var-gry-39")

    @Test
    fun scenario02_03_04_strictOverpackPartialPackAndQuantityRestoration() {
        val orders = listOf(PackingOrderLine(insoleBlack39, 200))

        // Attempting to pack 300 PCS > 200 PCS must be rejected
        val overpackLines = listOf(PackingCartonLine("uid-1", insoleBlack39, 300))
        val overIssues = PoPackingEngine.draftBlockers(orders, overpackLines)
        assertTrue(overIssues.any { it.type == PackingIssueType.OVERPACKED && it.ordered == 200 && it.packed == 300 })

        // Pack 120 PCS in CTN-001 -> remaining 80 PCS
        val ctn1Only = listOf(PackingCartonLine("uid-1", insoleBlack39, 120))
        val sumAfterCtn1 = PoPackingEngine.summarize(orders, ctn1Only)
        assertEquals(80, sumAfterCtn1.variants.single().remaining)
        assertEquals(80, PoPackingEngine.maxAllowedForCarton(orders, ctn1Only, "uid-2", insoleBlack39))
        // Editing CTN-001 itself still allows up to 200 (does not count itself against itself)
        assertEquals(200, PoPackingEngine.maxAllowedForCarton(orders, ctn1Only, "uid-1", insoleBlack39))

        // Pack 80 PCS in CTN-002 -> remaining 0 PCS; variant disappears from Available and enters Completed
        val bothCtns = listOf(
            PackingCartonLine("uid-1", insoleBlack39, 120),
            PackingCartonLine("uid-2", insoleBlack39, 80)
        )
        val sumComplete = PoPackingEngine.summarize(orders, bothCtns)
        assertEquals(0, sumComplete.totalRemaining)
        assertEquals(100, sumComplete.percent)
        assertTrue(PoPackingEngine.filterByVisibility(sumComplete.variants, VariantVisibility.AVAILABLE).isEmpty())
        assertEquals(1, PoPackingEngine.filterByVisibility(sumComplete.variants, VariantVisibility.COMPLETED).size)
        assertTrue(PoPackingEngine.confirmBlockers(orders, bothCtns).isEmpty())

        // Delete CTN-002 -> remaining immediately recalculates to 80 and variant becomes Available again
        val afterDeleteCtn2 = listOf(PackingCartonLine("uid-1", insoleBlack39, 120))
        val sumRestored = PoPackingEngine.summarize(orders, afterDeleteCtn2)
        assertEquals(80, sumRestored.variants.single().remaining)
        assertEquals(1, PoPackingEngine.filterByVisibility(sumRestored.variants, VariantVisibility.AVAILABLE).size)
    }

    @Test
    fun scenario05_optionABlocksPoQtyReductionBelowPacked() {
        val cartons = listOf(
            PackingCartonLine("uid-1", insoleBlack39, 120),
            PackingCartonLine("uid-2", insoleBlack39, 80)
        )
        val reduceCheck = PoPackingEngine.checkOrderQuantityChange(insoleBlack39, newQuantity = 150, cartons = cartons)
        assertFalse(reduceCheck.allowed)
        assertEquals(200, reduceCheck.packed)

        val validIncreaseCheck = PoPackingEngine.checkOrderQuantityChange(insoleBlack39, newQuantity = 250, cartons = cartons)
        assertTrue(validIncreaseCheck.allowed)
    }

    @Test
    fun scenario06_07_incompleteDraftAllowedWithWarningButConfirmBlockedUntil100Percent() {
        val orders = listOf(
            PackingOrderLine(insoleBlack39, 1200),
            PackingOrderLine(insoleGray39, 800)
        )
        val partialCartons = listOf(
            PackingCartonLine("uid-1", insoleBlack39, 1000),
            PackingCartonLine("uid-2", insoleGray39, 500)
        )

        val summary = PoPackingEngine.summarize(orders, partialCartons)
        assertEquals(2000, summary.totalOrdered)
        assertEquals(1500, summary.totalPacked)
        assertEquals(500, summary.totalRemaining)
        assertEquals(75, summary.percent)

        // Draft allowed (0 draft blockers), Confirm blocked with 500 unallocated
        assertTrue(PoPackingEngine.draftBlockers(orders, partialCartons).isEmpty())
        assertEquals(500, PoPackingEngine.unallocatedQuantity(orders, partialCartons))
        val confirmBlockers = PoPackingEngine.confirmBlockers(orders, partialCartons)
        assertEquals(2, confirmBlockers.size)
        assertTrue(confirmBlockers.all { it.type == PackingIssueType.UNDERPACKED })

        // Allocate remaining 500 correctly -> Confirm passes
        val fullCartons = partialCartons + listOf(
            PackingCartonLine("uid-3", insoleBlack39, 200),
            PackingCartonLine("uid-3", insoleGray39, 300)
        )
        assertTrue(PoPackingEngine.confirmBlockers(orders, fullCartons).isEmpty())

        // Bulk identical cartons limit cannot overpack
        val maxBulk = PoPackingEngine.maxIdenticalCartons(
            orders,
            partialCartons,
            mapOf(insoleBlack39 to 50, insoleGray39 to 100)
        )
        assertEquals(3, maxBulk) // 800-500=300 gray / 100 = 3 cartons max (even though black could do 200/50=4)
    }

    @Test
    fun scenario08_09_continuousCartonRenumberingAndShippingMarkDefaultVsOverride() {
        // 5 cartons: CTN-001..CTN-005 -> delete CTN-003 -> CTN-001..CTN-004 preserving UID & overrides
        val remainingUids = listOf("uid-1", "uid-2", "uid-4", "uid-5")
        val renumbered = PoPackingEngine.renumber(remainingUids)
        assertEquals("CTN-001", renumbered["uid-1"])
        assertEquals("CTN-002", renumbered["uid-2"])
        assertEquals("CTN-003", renumbered["uid-4"])
        assertEquals("CTN-004", renumbered["uid-5"])

        val oldToNew = mapOf("CTN-001" to "CTN-001", "CTN-002" to "CTN-002", "CTN-004" to "CTN-003", "CTN-005" to "CTN-004")
        val remappedMarks = PoNotesMetadataParser.remapCartonShippingMarks(
            mapOf("CTN-004" to "CUSTOM FRAGILE {CTN}"),
            oldToNew
        )
        assertEquals(mapOf("CTN-003" to "CUSTOM FRAGILE {CTN}"), remappedMarks)

        // Default shipping mark change updates non-overridden cartons while leaving custom intact
        val updatedDefault = "TUKTAK BD / PO-2026-004 / {CTN}"
        assertEquals(
            "TUKTAK BD / PO-2026-004 / CTN-001",
            PoPackingEngine.resolveShippingMark(updatedDefault, null, "CTN-001")
        )
        assertEquals(
            "CUSTOM FRAGILE CTN-003",
            PoPackingEngine.resolveShippingMark(updatedDefault, remappedMarks["CTN-003"], "CTN-003")
        )
    }

    @Test
    fun scenario11_12_13_fourSheetExcelAndSelectedCartonPartialExportAndConsistency() {
        val product = Product(
            id = "p-1", categoryId = "c-1", nameEn = "4D CLOUD INSOLE PU", nameBn = "৪ডি ইনসোল",
            baseSku = "SA9-4CI-001", purchasePriceMinor = 210L, sellingPriceMinor = 500L
        )
        val v1 = ProductVariant("v-1", "p-1", "SA9-4CI-001-BLA-3536", color = "Black", size = "35-36", variantCostMinor = 210L, variantPriceMinor = 500L)
        val v2 = ProductVariant("v-2", "p-1", "SA9-4CI-001-GRY-3536", color = "Gray", size = "35-36", variantCostMinor = 210L, variantPriceMinor = 500L)

        val serializedNotes = PoNotesMetadataParser.serialize(
            paymentTermLabel = "30% Advance + 70% Balance",
            forwarderAddress = "Yiwu Port Warehouse No. 18",
            userNotes = "Pack strictly by size",
            defaultShippingMark = "TUKTAK BD / PO-2026-004 / {CTN}",
            cartonShippingMarks = mapOf("CTN-002" to "URGENT AIR MARK / {CTN}")
        )
        val order = PurchaseOrder(
            id = "po-4", poNumber = "PO-2026-004", supplierId = "sup-1",
            status = PurchaseOrderStatus.CONFIRMED.name, currency = "CNY", exchangeRateToBdt = "17.50",
            totalAmountMinor = 168000L, totalBdtAmountMinor = 2940000L, totalCartons = 2,
            totalGrossWeightKg = "60.000", totalNetWeightKg = "56.000", totalCbm = "0.216000",
            notes = serializedNotes
        )
        val items = listOf(
            PurchaseOrderItem("i-1", "po-4", "p-1", "v-1", 400, 210L, 84000L, 1470000L, cartonQuantity = 1, pcsPerCarton = 400),
            PurchaseOrderItem("i-2", "po-4", "p-1", "v-2", 400, 210L, 84000L, 1470000L, cartonQuantity = 2, pcsPerCarton = 200)
        )
        val cartons = listOf(
            ShipmentCarton("c-1", "shp-1", "CTN 1", "p-1", "v-1", 400, "62", "46", "38", "30.000", "28.000", "0.108376", "po-4", isMixedCarton = false, totalCartonQuantity = 400, cartonGrossWeightKg = "30.000"),
            ShipmentCarton("c-2", "shp-1", "CTN-02", "p-1", "v-2", 200, "62", "46", "38", "15.000", "14.000", "0.108376", "po-4", isMixedCarton = false, totalCartonQuantity = 200, cartonGrossWeightKg = "15.000"),
            ShipmentCarton("c-3", "shp-1", "CTN-03", "p-1", "v-2", 200, "62", "46", "38", "15.000", "14.000", "0.108376", "po-4", isMixedCarton = false, totalCartonQuantity = 200, cartonGrossWeightKg = "15.000")
        )
        val supplier = Supplier("sup-1", "SUP-1", "Wangdu Jiezu Xian Deng", "Li Yanan", "15233736047", address = "Hebei", country = "China")
        val profile = BusinessProfile(companyName = "TUKTAK BD", taxNumber = "99281726-01", phone = "+8801639484547", address = "41, New Elephant Road, Dhaka")

        // 1. Validate full export consistency model
        val fullExport = PoExportDataBuilder.build(
            order, items, mapOf(product.id to product), supplier, profile,
            variantsMap = mapOf(v1.id to v1, v2.id to v2), cartons = cartons
        )
        assertTrue(fullExport.isValidForSupplierExport)
        assertFalse(fullExport.isPartialCartonExport)
        assertEquals(3, fullExport.allCartons.size)
        assertEquals("CTN-001", fullExport.items[0].cartonDisplay)
        // v-2 split across CTN-002 (200) and CTN-003 (200) -> actual references!
        assertEquals("CTN-002 (200), CTN-003 (200)", fullExport.items[1].cartonDisplay)
        assertEquals("URGENT AIR MARK / CTN-002", fullExport.allCartons[1].shippingMark)
        assertEquals("TUKTAK BD / PO-2026-004 / CTN-003", fullExport.allCartons[2].shippingMark)

        // 2. Generate 4-sheet XLSX and verify structure + formulas + autoFilter + frozen headers
        val fullXlsx = tempFolder.newFile("full.xlsx")
        ImportExcelGenerator.generateSupplierOrderExcelToFile(
            file = fullXlsx, order = order, items = items, productsMap = mapOf(product.id to product),
            supplier = supplier, businessProfile = profile, variantsMap = mapOf(v1.id to v1, v2.id to v2), cartons = cartons
        )
        ZipFile(fullXlsx).use { zip ->
            val wb = zip.getInputStream(zip.getEntry("xl/workbook.xml")).readBytes().toString(StandardCharsets.UTF_8)
            assertTrue(wb.contains("Purchase Order"))
            assertTrue(wb.contains("Packing List (Carton Wise)"))
            assertTrue(wb.contains("Carton Summary"))
            assertTrue(wb.contains("Shipping Mark &amp; Delivery"))

            val s1 = zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).readBytes().toString(StandardCharsets.UTF_8)
            assertTrue(s1.contains("<autoFilter"))
            assertTrue(s1.contains("state=\"frozen\""))
            assertTrue(s1.contains("<f>SUM(H"))
            assertTrue(s1.contains("<f>SUM(J"))
            assertTrue(s1.contains("CTN-002 (200), CTN-003 (200)"))
            assertFalse(s1.contains("1 CTN")) // Never misleading "1 CTN"!

            val s2 = zip.getInputStream(zip.getEntry("xl/worksheets/sheet2.xml")).readBytes().toString(StandardCharsets.UTF_8)
            assertTrue(s2.contains("<f>SUM(K")) // CBM sum without double counting
            val s3 = zip.getInputStream(zip.getEntry("xl/worksheets/sheet3.xml")).readBytes().toString(StandardCharsets.UTF_8)
            assertTrue(s3.contains("CTN-001") && s3.contains("CTN-002") && s3.contains("CTN-003"))
            val s4 = zip.getInputStream(zip.getEntry("xl/worksheets/sheet4.xml")).readBytes().toString(StandardCharsets.UTF_8)
            assertTrue(s4.contains("URGENT AIR MARK / CTN-002"))
        }

        // 3. Selected-Carton Partial Export (only CTN-002 selected)
        val partialExport = PoExportDataBuilder.build(
            order, items, mapOf(product.id to product), supplier, profile,
            variantsMap = mapOf(v1.id to v1, v2.id to v2), cartons = cartons,
            selectedCartonNumbers = setOf("CTN-002")
        )
        assertTrue(partialExport.isPartialCartonExport)
        assertEquals(1, partialExport.exportedCartonCount)
        assertEquals(200, partialExport.exportedPackedQuantity)
        assertEquals(800, partialExport.poTotalQuantity) // Complete PO total preserved!
        assertEquals(BigDecimal("15.000"), partialExport.exportedGrossWeightKg)
    }
}
