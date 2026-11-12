package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.LandingCostEngine
import com.pocketdev.advance_erp.core.util.ImportPdfGenerator
import com.pocketdev.advance_erp.domain.model.PaymentTermType
import com.pocketdev.advance_erp.feature.imports.parsePoNotesMetadata
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PurchaseReceivingAndPdfShareFixesTest {

    @Test
    fun `sanitizeFileName removes path separators and unsafe characters for supplier PDFs`() {
        assertEquals("PO_2026_001", ImportPdfGenerator.sanitizeFileName("PO/2026\\001"))
        assertEquals("PO_CN_12_34", ImportPdfGenerator.sanitizeFileName(" PO:CN 12/34 "))
        assertEquals("document", ImportPdfGenerator.sanitizeFileName("///"))
    }

    @Test
    fun `mixed carton with 2 category products in 3 physical cartons counts as 3 CTNs`() {
        val cartons = listOf(
            LandingCostEngine.ShipmentCartonInput(
                cartonNumber = "CTN-01",
                supplierId = "sup-1",
                productId = "prod-shoe",
                categoryId = "cat-footwear",
                quantity = 50,
                grossWeightKg = BigDecimal("12.000"),
                cbm = BigDecimal("0.050000")
            ),
            LandingCostEngine.ShipmentCartonInput(
                cartonNumber = "CTN-02",
                supplierId = "sup-1",
                productId = "prod-bag",
                categoryId = "cat-bag",
                quantity = 30,
                grossWeightKg = BigDecimal("10.000"),
                cbm = BigDecimal("0.040000")
            ),
            // CTN-03 is a single physical mixed carton containing 2 different category products (4 rows total)
            LandingCostEngine.ShipmentCartonInput(
                cartonNumber = "CTN-03",
                supplierId = "sup-1",
                productId = "prod-shoe",
                categoryId = "cat-footwear",
                quantity = 15,
                grossWeightKg = BigDecimal("6.000"),
                cbm = BigDecimal("0.030000")
            ),
            LandingCostEngine.ShipmentCartonInput(
                cartonNumber = "CTN-03",
                supplierId = "sup-1",
                productId = "prod-belt",
                categoryId = "cat-accessories",
                quantity = 20,
                grossWeightKg = BigDecimal("4.000"),
                cbm = BigDecimal("0.030000")
            )
        )

        val summary = LandingCostEngine.summarizePackagingList(cartons)
        assertEquals(3, summary.totalCartonCount)
        assertEquals(0, BigDecimal("0.150000").compareTo(summary.totalCbm))
    }

    @Test
    fun `parsePoNotesMetadata restores terms forwarder and clean user notes without duplication`() {
        val raw = "[Terms: 50% Advance + 50% Balance] • [Forwarder: Yiwu Hub Gate 3] • Urgent winter batch"
        val parsed = parsePoNotesMetadata(raw)

        assertEquals(PaymentTermType.FIFTY_FIFTY, parsed.paymentTerm)
        assertEquals("Yiwu Hub Gate 3", parsed.forwarderAddress)
        assertEquals("Urgent winter batch", parsed.userNotes)
    }
}
