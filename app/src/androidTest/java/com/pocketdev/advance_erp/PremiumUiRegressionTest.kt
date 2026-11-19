package com.pocketdev.advance_erp

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.pocketdev.advance_erp.core.designsystem.*
import com.pocketdev.advance_erp.core.util.EditorDraftStore
import com.pocketdev.advance_erp.data.entity.*
import com.pocketdev.advance_erp.feature.imports.CreatePurchaseOrderDialog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PremiumUiRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun purchaseNavigationIsSelectedForShipmentAndAccountsRemainInMore() {
        compose.setContent {
            AdvanceErpTheme(darkTheme = true) { ERPBottomNavigation("shipments", {}) }
        }
        compose.onAllNodes(isSelectable()).assertCountEquals(5)
        compose.onAllNodes(isSelected()).assertCountEquals(1)
        assertTrue(BottomNavDestination.PURCHASES.contains("shipments"))
        assertTrue(BottomNavDestination.MORE.contains("accounts"))
        assertFalse(BottomNavDestination.MORE.contains("purchases"))
    }

    @Test fun incompletePurchaseDraftSurvivesRecreationAndExplicitSave() {
        val restoration = StateRestorationTester(compose)
        var visible by mutableStateOf(true)
        val context = compose.activity
        val store = EditorDraftStore(context)
        // Unique order ID: tests never read/write the user's purchase:new buffer.
        val order = PurchaseOrder("premium-test", "PO-TEST", "s", currency = "CNY", exchangeRateToBdt = "17.00")
        val key = "purchase:premium-test"
        store.remove(key)
        try {
            restoration.setContent {
                AdvanceErpTheme(darkTheme = true) {
                    if (visible) CreatePurchaseOrderDialog(
                        suppliers = listOf(Supplier("s", "S", "TEST SUPPLIER", phone = "", address = "")),
                        products = emptyList(), editingOrder = order,
                        onDismiss = { visible = false }, onSavePo = { _,_,_,_,_,_,_,_,_,_,_,_ -> fail("Editor draft must not post an order") }
                    )
                }
            }
            compose.onNodeWithText("17.00").performTextReplacement("18.25")
            restoration.emulateSavedInstanceStateRestore()
            compose.onNodeWithText("18.25").assertExists()
            compose.onNodeWithContentDescription("Close").performClick()
            compose.onNodeWithText("Save your changes?").assertExists()
            compose.onNodeWithText("Continue editing").performClick()
            compose.onNodeWithText("18.25").assertExists()
            compose.onNodeWithContentDescription("Close").performClick()
            compose.onNodeWithText("Save draft").performClick()
            compose.waitUntil(5000) { !visible }
            assertEquals("18.25", store.read(key)!!.getString("rate"))
            compose.runOnIdle { visible = true }
            compose.onNodeWithText("Resumed saved draft").assertExists()
            compose.onNodeWithText("18.25").assertExists()
        } finally { store.remove(key) }
    }
}
