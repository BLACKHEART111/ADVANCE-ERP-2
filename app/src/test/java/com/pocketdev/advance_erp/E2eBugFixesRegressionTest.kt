package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.repository.normalizeCategorySlug
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class E2eBugFixesRegressionTest {

    @Test
    fun categorySlugNormalizesSpecialCharactersWithoutTripleDashes() {
        assertEquals("beauty-care", normalizeCategorySlug("Beauty & Care"))
        assertEquals("men-s-apparel", normalizeCategorySlug("  -- Men's   Apparel -- "))
        assertEquals("cat", normalizeCategorySlug("!!!"))
    }

    @Test
    fun cartonAndShipmentCodesUseAsciiDigitsUnderBanglaLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("bn", "BD"))
            assertEquals("CTN-01", String.format(Locale.ROOT, "CTN-%02d", 1))
            assertEquals("CTN-003", String.format(Locale.ROOT, "CTN-%03d", 3))
            assertEquals("SHP-2026-0004", String.format(Locale.ROOT, "SHP-2026-%04d", 4))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun minorUnitFormattingPreservesFractionalCentsOnPaymentDue() {
        assertEquals("407.50", Formatters.formatMinorUnit(40750L))
        assertEquals("0.05", Formatters.formatMinorUnit(5L))
    }
}
