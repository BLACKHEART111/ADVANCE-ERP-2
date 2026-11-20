package com.pocketdev.advance_erp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.designsystem.DarkSettingsTokens
import com.pocketdev.advance_erp.core.designsystem.LightSettingsTokens
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.feature.settings.components.SettingsCategory
import com.pocketdev.advance_erp.feature.settings.components.requiresCurrencyChangeConfirmation
import com.pocketdev.advance_erp.feature.settings.components.toggleCategoryExpanded
import com.pocketdev.advance_erp.feature.settings.components.validateBusinessProfileName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLogicTest {

    @Test
    fun accordionState_supportsAllCollapsedDefaultAndMultiExpand() {
        val initial = emptySet<SettingsCategory>()
        assertTrue(initial.isEmpty())

        val step1 = toggleCategoryExpanded(initial, SettingsCategory.APPEARANCE_LANGUAGE)
        val step2 = toggleCategoryExpanded(step1, SettingsCategory.BUSINESS_PROFILE)
        assertEquals(
            setOf(SettingsCategory.APPEARANCE_LANGUAGE, SettingsCategory.BUSINESS_PROFILE),
            step2
        )

        val step3 = toggleCategoryExpanded(step2, SettingsCategory.APPEARANCE_LANGUAGE)
        assertEquals(setOf(SettingsCategory.BUSINESS_PROFILE), step3)
    }

    @Test
    fun validateBusinessProfileName_rejectsBlankStrings() {
        assertFalse(validateBusinessProfileName(""))
        assertFalse(validateBusinessProfileName("   "))
        assertTrue(validateBusinessProfileName("My Business"))
        assertTrue(validateBusinessProfileName("অ্যাডভান্স ট্রেডার্স"))
    }

    @Test
    fun currencyConfirmation_onlyPromptsOnActualChange() {
        assertFalse(requiresCurrencyChangeConfirmation(CurrencyCode.BDT, CurrencyCode.BDT))
        assertTrue(requiresCurrencyChangeConfirmation(CurrencyCode.BDT, CurrencyCode.USD))
        assertTrue(requiresCurrencyChangeConfirmation(CurrencyCode.USD, CurrencyCode.CNY))
    }

    @Test
    fun settingsTokens_enforceContrastAndNonPureBlackDarkMode() {
        assertFalse(LightSettingsTokens.isDark)
        assertTrue(DarkSettingsTokens.isDark)

        assertNotEquals(Color.Black, DarkSettingsTokens.pageBgTop)
        assertNotEquals(Color.Black, DarkSettingsTokens.cardBg)
        assertTrue(DarkSettingsTokens.cardBg.luminance() < 0.08f)
        assertTrue(DarkSettingsTokens.titleText.luminance() > 0.85f)

        assertTrue(LightSettingsTokens.cardBg.luminance() > 0.90f)
        assertTrue(LightSettingsTokens.titleText.luminance() < 0.12f)
    }

    @Test
    fun databaseVersionConstant_matchesRoomSchemaV13() {
        assertEquals(13, AdvanceErpDatabase.DATABASE_VERSION)
    }
}
