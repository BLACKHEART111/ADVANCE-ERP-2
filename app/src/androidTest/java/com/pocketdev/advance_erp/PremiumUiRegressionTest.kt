package com.pocketdev.advance_erp

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.platform.app.InstrumentationRegistry
import com.pocketdev.advance_erp.core.designsystem.*
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.core.util.EditorDraftStore
import com.pocketdev.advance_erp.data.entity.*
import com.pocketdev.advance_erp.domain.repository.DashboardSummary
import com.pocketdev.advance_erp.domain.repository.DateWiseMetrics
import com.pocketdev.advance_erp.domain.repository.HistoricalClosingBalances
import com.pocketdev.advance_erp.feature.dashboard.DashboardContent
import com.pocketdev.advance_erp.feature.dashboard.DashboardUiState
import com.pocketdev.advance_erp.feature.imports.CreatePurchaseOrderDialog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

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

    @Test fun dashboardBusinessOverviewBelowDateSelectorAndFabMenuNavigates() {
        val selectedDate = 1_795_142_400_000L
        val baseSummary = DashboardSummary(
            todaysSales = Money.ofMinor(50_000L, CurrencyCode.BDT),
            totalReceivable = Money.ofMinor(25_000L, CurrencyCode.BDT),
            totalPayable = Money.ofMinor(10_000L, CurrencyCode.BDT),
            stockValuation = Money.ofMinor(120_000L, CurrencyCode.BDT),
            productCount = 12,
            lowStockCount = 2,
            customerCount = 8,
            supplierCount = 5,
            recentTransactions = emptyList(),
            isLiveDatabaseData = true,
            dateWiseMetrics = DateWiseMetrics(
                selectedDateMillis = selectedDate,
                totalSales = Money.ofMinor(50_000L, CurrencyCode.BDT),
                totalPurchases = Money.ofMinor(30_000L, CurrencyCode.BDT),
                totalCollections = Money.ofMinor(20_000L, CurrencyCode.BDT),
                totalPayments = Money.ofMinor(10_000L, CurrencyCode.BDT),
                totalExpenses = Money.ofMinor(5_000L, CurrencyCode.BDT),
                totalReturns = Money.ofMinor(0L, CurrencyCode.BDT),
                stockInUnits = 15,
                stockOutUnits = 5,
                netCashFlow = Money.ofMinor(10_000L, CurrencyCode.BDT),
                dailyGrossProfit = Money.ofMinor(15_000L, CurrencyCode.BDT),
                transactionCount = 3
            ),
            closingBalances = HistoricalClosingBalances(
                asOfDateMillis = selectedDate,
                closingCashBank = Money.ofMinor(100_000L, CurrencyCode.BDT),
                customerOutstanding = Money.ofMinor(25_000L, CurrencyCode.BDT),
                supplierOutstanding = Money.ofMinor(10_000L, CurrencyCode.BDT),
                inventoryValuation = Money.ofMinor(120_000L, CurrencyCode.BDT)
            )
        )

        var currentLanguage by mutableStateOf(AppLanguage.BANGLA)
        var poClicks = 0
        var salesClicks = 0
        var addProductClicks = 0
        var ledgerClicks = 0
        var prevDayClicks = 0
        var nextDayClicks = 0
        var todayClicks = 0
        var dailyPdfClicks = 0
        var monthlyPdfClicks = 0

        compose.setContent {
            val baseConfig = LocalConfiguration.current
            val baseContext = LocalContext.current
            val localizedConfig = remember(currentLanguage, baseConfig) {
                Configuration(baseConfig).apply { setLocale(currentLanguage.locale) }
            }
            val localizedContext = remember(currentLanguage, baseContext) {
                baseContext.createConfigurationContext(localizedConfig)
            }
            val uiState = remember(currentLanguage) {
                DashboardUiState(
                    summary = baseSummary,
                    selectedDateMillis = selectedDate,
                    language = currentLanguage,
                    currency = CurrencyCode.BDT,
                    canViewProfit = true,
                    isLoading = false
                )
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedConfig,
                LocalContext provides localizedContext
            ) {
                AdvanceErpTheme(darkTheme = false) {
                    Scaffold(
                        topBar = {
                            ERPTopBar(
                                title = stringResource(R.string.nav_dashboard),
                                subtitle = "My Business",
                                currentLanguage = currentLanguage,
                                onOpenDrawer = {},
                                onToggleLanguage = {
                                    currentLanguage = if (currentLanguage == AppLanguage.BANGLA) {
                                        AppLanguage.ENGLISH
                                    } else {
                                        AppLanguage.BANGLA
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            ERPBottomNavigation(
                                currentRoute = "dashboard",
                                onNavigate = {},
                                modifier = Modifier.testTag("dashboard_test_bottom_nav")
                            )
                        }
                    ) { padding ->
                        DashboardContent(
                            state = uiState,
                            onSelectPreviousDay = { prevDayClicks++ },
                            onSelectNextDay = { nextDayClicks++ },
                            onSelectToday = { todayClicks++ },
                            onSelectDate = {},
                            onGenerateDailySnapshotPdf = { dailyPdfClicks++ },
                            onGenerateMonthlyPerformancePdf = { monthlyPdfClicks++ },
                            onClearGeneratedPdf = {},
                            onNavigateToAddProduct = { addProductClicks++ },
                            onNavigateToImports = { poClicks++ },
                            onNavigateToSales = { salesClicks++ },
                            onNavigateToTransactions = {},
                            onNavigateToLedger = { ledgerClicks++ },
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }
        }

        fun saveScreenCapture(fileName: String) {
            compose.waitForIdle()
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            val dir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
            File(dir, fileName).outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }

        val activity = compose.activity
        compose.onAllNodesWithText("ব্যবসায়িক পর্যালোচনা").assertCountEquals(1)

        // Old top quick action buttons are removed
        compose.onNodeWithText("ক্রয়াদেশ").assertDoesNotExist()
        compose.onNodeWithText("নতুন বিক্রয়").assertDoesNotExist()
        compose.onNodeWithText("পণ্য যোগ").assertDoesNotExist()
        compose.onNodeWithText("লেজার").assertDoesNotExist()

        // Vertical order: Date Selector -> Business Overview -> Daily Business Information
        val dateBounds = compose.onNodeWithTag("dashboard_date_selector").fetchSemanticsNode().boundsInRoot
        val overviewBounds = compose.onNodeWithTag("dashboard_business_overview").fetchSemanticsNode().boundsInRoot
        val dailyMetricsBounds = compose.onNodeWithTag("dashboard_daily_metrics").fetchSemanticsNode().boundsInRoot
        assertTrue("Business Overview must sit below Date Selector", dateBounds.bottom <= overviewBounds.top)
        assertTrue("Daily Business Information must sit below Business Overview", overviewBounds.bottom <= dailyMetricsBounds.top)

        // All four Business Overview metrics render
        compose.onNodeWithText("পণ্যসমূহ").assertExists()
        compose.onNodeWithText("স্বল্প মজুদের পণ্য").assertExists()
        compose.onNodeWithText("গ্রাহকবৃন্দ").assertExists()
        compose.onNodeWithText("সরবরাহকারীগণ").assertExists()
        compose.onNodeWithText("১২").assertExists()
        compose.onNodeWithText("২").assertExists()
        compose.onNodeWithText("৮").assertExists()
        compose.onNodeWithText("৫").assertExists()

        // FAB does not overlap Bottom Navigation
        val fabBounds = compose.onNodeWithTag("dashboard_fab_main").fetchSemanticsNode().boundsInRoot
        val bottomNavBounds = compose.onNodeWithTag("dashboard_test_bottom_nav").fetchSemanticsNode().boundsInRoot
        assertTrue("FAB must sit above Bottom Navigation", fabBounds.bottom <= bottomNavBounds.top)

        val fabItemSpecs = listOf(
            Triple("dashboard_fab_action_po", "PO", { poClicks }),
            Triple("dashboard_fab_action_sales", "Sales", { salesClicks }),
            Triple("dashboard_fab_action_add_product", "Add Product", { addProductClicks }),
            Triple("dashboard_fab_action_ledger", "Ledger", { ledgerClicks })
        )

        // Collapsed initially — capture unobstructed screenshot
        compose.onNodeWithTag("dashboard_fab_menu").assertDoesNotExist()
        fabItemSpecs.forEach { (tag, _, _) ->
            compose.onNodeWithTag(tag).assertDoesNotExist()
        }
        saveScreenCapture("dashboard_fab_collapsed.png")

        // Expand FAB — capture unobstructed screenshot, then collapse by tapping "+" again
        compose.onNodeWithContentDescription("Quick Actions").performClick()
        compose.onNodeWithTag("dashboard_fab_menu").assertExists()
        fabItemSpecs.forEach { (tag, label, _) ->
            compose.onNodeWithTag(tag).assertExists().assertTextEquals(label)
        }
        saveScreenCapture("dashboard_fab_expanded.png")
        compose.onNodeWithContentDescription("Quick Actions").performClick()
        compose.onNodeWithTag("dashboard_fab_menu").assertDoesNotExist()

        // Expand FAB and collapse via outside scrim tap
        compose.onNodeWithContentDescription("Quick Actions").performClick()
        compose.onNodeWithTag("dashboard_fab_menu").assertExists()
        compose.onNodeWithTag("dashboard_fab_scrim").performClick()
        compose.onNodeWithTag("dashboard_fab_menu").assertDoesNotExist()

        // Expand FAB and collapse via Back button
        compose.onNodeWithContentDescription("Quick Actions").performClick()
        compose.onNodeWithTag("dashboard_fab_menu").assertExists()
        compose.runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithTag("dashboard_fab_menu").assertDoesNotExist()

        // Verify all four FAB menu actions collapse menu and invoke callbacks
        fabItemSpecs.forEach { (tag, label, counter) ->
            compose.onNodeWithContentDescription("Quick Actions").performClick()
            compose.onNodeWithTag(tag).assertTextEquals(label).performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("dashboard_fab_menu").assertDoesNotExist()
            assertEquals("Callback for $label must trigger once", 1, counter())
        }

        // Verify Date Selector controls & ERPCalendar toggle
        compose.onNodeWithContentDescription("Prev Day").performClick()
        compose.onNodeWithContentDescription("Next Day").performClick()
        compose.onNodeWithText("আজ").performClick()
        assertEquals(1, prevDayClicks)
        assertEquals(1, nextDayClicks)
        assertEquals(1, todayClicks)

        compose.onNode(hasText("তারিখ:", substring = true)).performClick()
        compose.onNodeWithText("রবি").assertExists()
        compose.onNode(hasText("তারিখ:", substring = true)).performClick()
        compose.onNodeWithText("রবি").assertDoesNotExist()

        // Scroll through remaining sections (Inventory Summary, Report Actions, Closing Position, Recent Transactions)
        compose.onNodeWithTag("dashboard_scroll_list").performScrollToIndex(2)
        compose.onNodeWithText("স্টক ইন").assertExists()
        compose.onNodeWithText("স্টক আউট").assertExists()
        compose.onNodeWithText("লেনদেন সংখ্যা").assertExists()

        compose.onNodeWithTag("dashboard_scroll_list").performScrollToIndex(3)
        compose.onNodeWithText("দৈনিক পিডিএফ চিত্র").performClick()
        compose.onNodeWithText("মাসিক রিপোর্ট").performClick()
        assertEquals(1, dailyPdfClicks)
        assertEquals(1, monthlyPdfClicks)

        compose.onNodeWithTag("dashboard_scroll_list").performScrollToIndex(4)
        compose.onNodeWithText("সমাপনী নগদ ও ব্যাংক").assertExists()
        compose.onNodeWithText("গ্রাহক বকেয়া (পাওনা)").assertExists()
        compose.onNodeWithText("সরবরাহকারী দেনা").assertExists()
        compose.onNodeWithText("মজুদ সম্পদ মূল্যায়ন").assertExists()

        compose.onNodeWithTag("dashboard_scroll_list").performScrollToIndex(5)
        compose.onNodeWithText("এই তারিখে কোনো লেনদেন নেই").assertExists()
        compose.onNodeWithTag("dashboard_fab_main").assertIsDisplayed()

        // Toggle language to English and verify section order remains intact
        compose.onNodeWithContentDescription("Language").performClick()
        compose.onNodeWithTag("dashboard_scroll_list").performScrollToIndex(0)
        compose.waitForIdle()
        compose.onAllNodesWithText("Business Overview").assertCountEquals(1)
        val enDateBounds = compose.onNodeWithTag("dashboard_date_selector").fetchSemanticsNode().boundsInRoot
        val enOverviewBounds = compose.onNodeWithTag("dashboard_business_overview").fetchSemanticsNode().boundsInRoot
        val enDailyMetricsBounds = compose.onNodeWithTag("dashboard_daily_metrics").fetchSemanticsNode().boundsInRoot
        assertTrue("English Business Overview must sit below Date Selector", enDateBounds.bottom <= enOverviewBounds.top)
        assertTrue("English Daily Metrics must sit below Business Overview", enOverviewBounds.bottom <= enDailyMetricsBounds.top)
    }
}
