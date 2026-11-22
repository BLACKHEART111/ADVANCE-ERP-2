package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.feature.product.GalleryItemDraft
import com.pocketdev.advance_erp.feature.product.NewVariantDraft
import com.pocketdev.advance_erp.feature.product.computeEffectiveAddProductStock
import com.pocketdev.advance_erp.feature.product.findVariantDuplicateError
import com.pocketdev.advance_erp.feature.product.normalizeProductDescription
import com.pocketdev.advance_erp.feature.product.removePhotoAndReassignPrimary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the 3-step Add Product workflow helper logic:
 * description bounds, opening stock calculation, variant duplicate checks, and photo ordering.
 */
class AddProductWorkflowLogicTest {

    @Test
    fun descriptionIsBoundedTo500Characters() {
        val raw = "A".repeat(620)
        val normalized = normalizeProductDescription(raw)
        assertEquals(500, normalized.length)

        val short = "Premium cotton t-shirt"
        assertEquals(short, normalizeProductDescription(short))
    }

    @Test
    fun openingStockUsesVariantSumWhenEnabledAndNonZero() {
        val variants = listOf(
            NewVariantDraft(sku = "PRD-R-M", color = "Red", size = "M", costMinor = 10000L, priceMinor = 15000L, initialStock = 12),
            NewVariantDraft(sku = "PRD-B-L", color = "Blue", size = "L", costMinor = 10000L, priceMinor = 15000L, initialStock = 8)
        )

        val effectiveWithVariants = computeEffectiveAddProductStock(
            hideStock = false,
            variantsEnabled = true,
            variants = variants,
            initialStockInput = "50"
        )
        assertEquals(20, effectiveWithVariants)

        val effectiveWithVariantsDisabled = computeEffectiveAddProductStock(
            hideStock = false,
            variantsEnabled = false,
            variants = variants,
            initialStockInput = "50"
        )
        assertEquals(50, effectiveWithVariantsDisabled)

        val effectiveHidden = computeEffectiveAddProductStock(
            hideStock = true,
            variantsEnabled = true,
            variants = variants,
            initialStockInput = "50"
        )
        assertEquals(0, effectiveHidden)
    }

    @Test
    fun duplicateVariantSkuAndBarcodeAreRejectedExceptSelfWhenEditing() {
        val existing = listOf(
            NewVariantDraft(sku = "SHIRT-RED-M", color = "Red", size = "M", costMinor = 50000L, priceMinor = 80000L, barcode = "890123456001"),
            NewVariantDraft(sku = "SHIRT-BLU-L", color = "Blue", size = "L", costMinor = 50000L, priceMinor = 80000L, barcode = "890123456002")
        )

        // Matching base SKU
        assertNotNull(
            findVariantDuplicateError(
                candidateSku = "shirt-001",
                candidateBarcode = null,
                baseSku = "SHIRT-001",
                baseBarcode = "890000000001",
                existingVariants = existing,
                editingIndex = null,
                isBangla = false
            )
        )

        // Matching another variant SKU
        assertNotNull(
            findVariantDuplicateError(
                candidateSku = "SHIRT-RED-M",
                candidateBarcode = null,
                baseSku = "SHIRT-001",
                baseBarcode = null,
                existingVariants = existing,
                editingIndex = null,
                isBangla = true
            )
        )

        // Matching own SKU/barcode while editing index 0 is allowed
        assertNull(
            findVariantDuplicateError(
                candidateSku = "SHIRT-RED-M",
                candidateBarcode = "890123456001",
                baseSku = "SHIRT-001",
                baseBarcode = "890000000001",
                existingVariants = existing,
                editingIndex = 0,
                isBangla = false
            )
        )

        // Matching base barcode is rejected
        assertNotNull(
            findVariantDuplicateError(
                candidateSku = "SHIRT-GRN-S",
                candidateBarcode = "890000000001",
                baseSku = "SHIRT-001",
                baseBarcode = "890000000001",
                existingVariants = existing,
                editingIndex = null,
                isBangla = false
            )
        )
    }

    @Test
    fun removePrimaryPhotoReassignsFirstRemainingAndNormalizesSortOrder() {
        val photos = listOf(
            GalleryItemDraft(id = "p1", uriString = "file:///1.jpg", isPrimary = true, sortOrder = 0),
            GalleryItemDraft(id = "p2", uriString = "file:///2.jpg", isPrimary = false, sortOrder = 1),
            GalleryItemDraft(id = "p3", uriString = "file:///3.jpg", isPrimary = false, sortOrder = 2)
        )

        val updated = removePhotoAndReassignPrimary(photos, "p1")
        assertEquals(2, updated.size)
        assertEquals("p2", updated[0].id)
        assertTrue(updated[0].isPrimary)
        assertEquals(0, updated[0].sortOrder)
        assertEquals("p3", updated[1].id)
        assertEquals(1, updated[1].sortOrder)
    }

    @Test
    fun moneyParsesBdtDecimalsIntoMinorUnits() {
        assertEquals(12550L, Money.parseFromString("125.50").minorUnits)
        assertEquals(0L, Money.parseFromString("").minorUnits)
    }
}
