package com.pocketdev.advance_erp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.pocketdev.advance_erp.core.designsystem.DarkDrawerTokens
import com.pocketdev.advance_erp.core.designsystem.DrawerDestination
import com.pocketdev.advance_erp.core.designsystem.LightDrawerTokens
import com.pocketdev.advance_erp.core.designsystem.drawerDestinationLabel
import com.pocketdev.advance_erp.core.designsystem.drawerPrimaryMenuItems
import com.pocketdev.advance_erp.core.designsystem.isDrawerDestinationSelected
import com.pocketdev.advance_erp.core.designsystem.visibleDrawerMenuItems
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.navigation.formatDrawerDate
import com.pocketdev.advance_erp.navigation.resolveActiveWarehouse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DrawerLogicTest {

    @Test
    fun resolveActiveWarehouse_prefersDefaultActiveWarehouse() {
        val wh1 = Warehouse(id = "w1", name = "Branch Godown", code = "W1", location = "Dhaka", isDefault = false, isActive = true)
        val wh2 = Warehouse(id = "w2", name = "সেন্ট্রাল গুদাম", code = "W2", location = "Dhaka", isDefault = true, isActive = true)
        val wh3 = Warehouse(id = "w3", name = "Closed WH", code = "W3", location = "Chattogram", isDefault = true, isActive = false)

        val (name, hasActive) = resolveActiveWarehouse(listOf(wh1, wh2))
        assertEquals("সেন্ট্রাল গুদাম", name)
        assertTrue(hasActive)

        val (fallbackName, fallbackActive) = resolveActiveWarehouse(listOf(wh3))
        assertNull(fallbackName)
        assertFalse(fallbackActive)
    }

    @Test
    fun formatDrawerDate_formatsBanglaAndEnglishAccurately() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 21)
        }
        val bn = formatDrawerDate(cal, AppLanguage.BANGLA)
        val en = formatDrawerDate(cal, AppLanguage.ENGLISH)

        assertEquals("শনিবার, ২১ নভেম্বর ২০২৬", bn)
        assertEquals("Saturday, 21 Nov 2026", en)
    }

    @Test
    fun drawerSelection_highlightsAtMostOneItemAtATime() {
        val selectedForProducts = drawerPrimaryMenuItems.filter {
            isDrawerDestinationSelected(it, "products")
        }
        assertEquals(listOf(DrawerDestination.PRODUCTS), selectedForProducts)

        val selectedForReports = drawerPrimaryMenuItems.filter {
            isDrawerDestinationSelected(it, "reports")
        }
        assertEquals(listOf(DrawerDestination.LEDGER), selectedForReports)

        val selectedForVault = drawerPrimaryMenuItems.filter {
            isDrawerDestinationSelected(it, "import_vault")
        }
        assertEquals(listOf(DrawerDestination.IMPORT_VAULT), selectedForVault)

        val selectedForUnknown = drawerPrimaryMenuItems.filter {
            isDrawerDestinationSelected(it, "nonexistent")
        }
        assertTrue(selectedForUnknown.isEmpty())
    }

    @Test
    fun drawerLabels_preserveBilingualMappings() {
        assertEquals("ড্যাশবোর্ড", drawerDestinationLabel(DrawerDestination.DASHBOARD, AppLanguage.BANGLA))
        assertEquals("Dashboard", drawerDestinationLabel(DrawerDestination.DASHBOARD, AppLanguage.ENGLISH))
        assertEquals("চালানসমূহ", drawerDestinationLabel(DrawerDestination.SHIPMENTS, AppLanguage.BANGLA))
        assertEquals("Delivery Challans", drawerDestinationLabel(DrawerDestination.SHIPMENTS, AppLanguage.ENGLISH))
        assertEquals("আমদানি আর্কাইভ", drawerDestinationLabel(DrawerDestination.IMPORT_VAULT, AppLanguage.BANGLA))
        assertEquals("Import Vault", drawerDestinationLabel(DrawerDestination.IMPORT_VAULT, AppLanguage.ENGLISH))
        assertEquals("হিসাব ও লেজার", drawerDestinationLabel(DrawerDestination.ACCOUNTS, AppLanguage.BANGLA))
        assertEquals("Accounting & Ledger", drawerDestinationLabel(DrawerDestination.ACCOUNTS, AppLanguage.ENGLISH))
        assertEquals("খতিয়ান ও রিপোর্ট", drawerDestinationLabel(DrawerDestination.LEDGER, AppLanguage.BANGLA))
        assertEquals("Reports & Statements", drawerDestinationLabel(DrawerDestination.LEDGER, AppLanguage.ENGLISH))
    }

    @Test
    fun drawerTokens_enforceDarkModeContrastAndNonBlackSurfaces() {
        assertFalse(LightDrawerTokens.isDark)
        assertTrue(DarkDrawerTokens.isDark)
        // Dark surface should be deep navy, not pure #000000 black or bright white
        assertNotEquals(Color.Black, DarkDrawerTokens.drawerBackground)
        assertTrue(DarkDrawerTokens.drawerBackground.luminance() < 0.05f)
        assertTrue(DarkDrawerTokens.miniCardBg.luminance() < 0.10f)
        assertTrue(DarkDrawerTokens.menuItemText.luminance() > 0.80f)
        // Light mode surface remains light and readable
        assertTrue(LightDrawerTokens.drawerBackground.luminance() > 0.85f)
        assertTrue(LightDrawerTokens.menuItemText.luminance() < 0.15f)
    }

    @Test
    fun ownerAdminRole_hasCrownAuthorizationCode() {
        assertEquals(UserRole.OWNER_ADMIN, UserRole.fromCode("OWNER_ADMIN"))
        assertEquals("Owner / Admin", UserRole.OWNER_ADMIN.titleEn)
        assertNotEquals(UserRole.OWNER_ADMIN, UserRole.MANAGER)
    }

    @Test
    fun visibleDrawerMenuItems_showsImportVaultOnlyToOwnerAdmin() {
        val ownerMenu = visibleDrawerMenuItems(UserRole.OWNER_ADMIN)
        assertTrue(DrawerDestination.IMPORT_VAULT in ownerMenu)

        val nonOwnerRoles = listOf(
            UserRole.MANAGER,
            UserRole.SALES_OPERATOR,
            UserRole.PURCHASE_OPERATOR,
            UserRole.ACCOUNTANT,
            UserRole.INVENTORY_OPERATOR
        )
        for (role in nonOwnerRoles) {
            val menu = visibleDrawerMenuItems(role)
            assertFalse(
                "Expected IMPORT_VAULT hidden for role ${role.code}",
                DrawerDestination.IMPORT_VAULT in menu
            )
            assertTrue(DrawerDestination.DASHBOARD in menu)
            assertTrue(DrawerDestination.SETTINGS in menu)
        }
    }
}
