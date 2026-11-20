package com.pocketdev.advance_erp

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.designsystem.AdvanceErpTheme
import com.pocketdev.advance_erp.core.designsystem.ERPBottomNavigation
import com.pocketdev.advance_erp.core.designsystem.ERPTopBar
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.core.util.DocumentNumberService
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.SupplierPayment
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.data.repository.AccountingRepositoryImpl
import com.pocketdev.advance_erp.data.repository.BusinessControlCenterRepositoryImpl
import com.pocketdev.advance_erp.data.repository.FinancialRepositoryImpl
import com.pocketdev.advance_erp.data.repository.ImportRepositoryImpl
import com.pocketdev.advance_erp.data.repository.SalesRepositoryImpl
import com.pocketdev.advance_erp.domain.model.BusinessSummaryDashboardData
import com.pocketdev.advance_erp.domain.model.ControlCenterFilterState
import com.pocketdev.advance_erp.domain.model.DateRangeOption
import com.pocketdev.advance_erp.domain.model.ReportCategory
import com.pocketdev.advance_erp.feature.controlcenter.BusinessControlCenterScreen
import com.pocketdev.advance_erp.feature.controlcenter.BusinessControlCenterViewModel
import com.pocketdev.advance_erp.feature.controlcenter.components.ControlCenterFilterDrawer
import com.pocketdev.advance_erp.feature.controlcenter.components.ControlCenterSummaryCards
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CentralLedgerUiTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun summaryCardsRespectProfitPrivacyAndSeparatePayableFromPaymentsMade() {
        val summary = BusinessSummaryDashboardData(
            totalSalesMinor = 25_000_000L,
            totalReceivedMinor = 18_000_000L,
            totalPurchaseMinor = 12_000_000L,
            totalPaidMinor = 5_000_000L,
            totalOutstandingPayableMinor = 7_000_000L,
            totalOutstandingReceivableMinor = 7_000_000L,
            totalExpensesMinor = 1_000_000L,
            grossProfitMinor = 8_000_000L,
            netProfitMinor = 7_000_000L,
            totalDeliveryCount = 4
        )
        var isOwnerAdmin by mutableStateOf(false)
        var canViewProfit by mutableStateOf(false)

        compose.setContent {
            AdvanceErpTheme(darkTheme = false) {
                ControlCenterSummaryCards(
                    summary = summary,
                    canViewProfit = canViewProfit,
                    isOwnerAdmin = isOwnerAdmin,
                    language = AppLanguage.BANGLA,
                    currencyCode = CurrencyCode.BDT,
                    onCategoryClick = {}
                )
            }
        }

        // Primary 2x2 grid cards render
        compose.onNodeWithText("মোট বিক্রয়").assertExists()
        compose.onNodeWithText("মোট টাকা গ্রহণ").assertExists()
        compose.onNodeWithText("মোট ক্রয়").assertExists()
        compose.onNodeWithText("মোট দেনা").assertExists()

        // Expand extra KPIs — Payments Made is separate from Payable, and Profit is hidden from non-Owner/Admin
        compose.onNodeWithText("আরও সারসংক্ষেপ দেখুন").performClick()
        compose.onNodeWithText("মোট পরিশোধ").assertExists()
        compose.onNodeWithText("মোট পাওনা (বকেয়া)").assertExists()
        compose.onNodeWithText("মোট ব্যয়").assertExists()
        compose.onNodeWithText("মোট মুনাফা (Gross)").assertDoesNotExist()
        compose.onNodeWithText("নিট মুনাফা (Net)").assertDoesNotExist()

        // Owner/Admin with unlocked profit sees profit KPIs
        compose.runOnIdle {
            isOwnerAdmin = true
            canViewProfit = true
        }
        compose.onNodeWithText("মোট মুনাফা (Gross)").assertExists()
        compose.onNodeWithText("নিট মুনাফা (Net)").assertExists()
    }

    @Test
    fun filterDrawerAppliesResetsAndClosesOnSwipeOrCloseButton() {
        var appliedFilter: ControlCenterFilterState? = null
        var resetCalls = 0
        var dismissCalls = 0
        var showDrawer by mutableStateOf(true)

        compose.setContent {
            AdvanceErpTheme(darkTheme = false) {
                if (showDrawer) {
                    ControlCenterFilterDrawer(
                        currentFilter = ControlCenterFilterState(),
                        canViewProfit = true,
                        customers = listOf(Customer(id = "c1", customerCode = "C-01", name = "রহিম ট্রেডার্স", phone = "01711", address = "Dhaka")),
                        suppliers = listOf(Supplier(id = "s1", supplierCode = "S-01", companyName = "China Supplier", phone = "01811", address = "Guangzhou")),
                        warehouses = listOf(Warehouse(id = "w1", name = "Main Warehouse", code = "WH-1", location = "Dhaka")),
                        categories = emptyList(),
                        products = emptyList(),
                        language = AppLanguage.BANGLA,
                        onApply = { appliedFilter = it },
                        onReset = { resetCalls++ },
                        onSavePreset = {},
                        onDismiss = {
                            dismissCalls++
                            showDrawer = false
                        }
                    )
                }
            }
        }

        compose.onNodeWithText("ফিল্টার ও অনুসন্ধান").assertExists()
        compose.onNodeWithText("ব্যবসা শুরু থেকে নির্বাচিত তারিখ").assertExists()
        compose.onNodeWithText("গত ৭ দিন").performClick()
        compose.onNodeWithText("বিক্রয়").performClick()
        compose.onNodeWithTag("filter_drawer_apply").performClick()
        compose.waitForIdle()

        assertEquals(DateRangeOption.LAST_7_DAYS, appliedFilter?.dateRangeOption)
        assertEquals(ReportCategory.SALES, appliedFilter?.category)
        assertEquals(1, dismissCalls)

        // Reopen and test swipe right to close
        compose.runOnIdle { showDrawer = true }
        compose.onNodeWithTag("filter_drawer_panel").performTouchInput { swipeRight() }
        compose.waitForIdle()
        assertEquals(2, dismissCalls)

        // Reopen and test Reset button
        compose.runOnIdle { showDrawer = true }
        compose.onNodeWithTag("filter_drawer_reset").performClick()
        compose.waitForIdle()
        assertEquals(1, resetCalls)
        assertEquals(3, dismissCalls)
    }

    @Test
    fun ledgerScreenTabsSwipeFilterFabAndScrollWorkEndToEnd() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AdvanceErpDatabase::class.java).build()
        val prefs = UserPreferencesRepository(context)
        val prevLang = prefs.selectedLanguage.first()
        val prevRole = prefs.selectedUserRole.first()
        prefs.setLanguage(AppLanguage.BANGLA)
        prefs.setUserRole(UserRole.OWNER_ADMIN)

        try {
            val now = System.currentTimeMillis()
            db.warehouseDao().insertWarehouse(Warehouse("w1", "Main WH", "WH1", "Dhaka"))
            db.productCategoryDao().insertCategory(ProductCategory("cat1", "Electronics", "ইলেকট্রনিক্স", "EL"))
            db.productDao().insertProduct(Product("p1", "cat1", "Smart Watch", "স্মার্ট ওয়াচ", "SW-01", purchasePriceMinor = 100_000, sellingPriceMinor = 150_000))
            db.customerDao().insertCustomer(Customer("c1", "C-001", "রহিম ট্রেডার্স", "01711000001", address = "Dhaka", outstandingBalanceMinor = 500_000L))
            db.supplierDao().insertSupplier(Supplier(id = "s1", supplierCode = "S-001", companyName = "China Supplier", phone = "01811000001", address = "Shenzhen", outstandingPayableMinor = 4_200_000L))
            listOf(
                ChartOfAccount("coa-1010", "1010", "Cash in Hand", "নগদ তহবিল", "ASSET", "DEBIT", accountSubType = "CASH"),
                ChartOfAccount("coa-1020", "1020", "City Bank", "সিটি ব্যাংক", "ASSET", "DEBIT", accountSubType = "BANK"),
                ChartOfAccount("coa-1040", "1040", "Accounts Receivable", "প্রাপ্য হিসাব", "ASSET", "DEBIT"),
                ChartOfAccount("coa-2010", "2010", "Accounts Payable", "প্রদেয় হিসাব", "LIABILITY", "CREDIT"),
                ChartOfAccount("coa-2030", "2030", "Customer Advance", "গ্রাহক অগ্রিম", "LIABILITY", "CREDIT"),
                ChartOfAccount("coa-5190", "5190", "General Admin Expense", "সাধারণ ব্যয়", "EXPENSE", "DEBIT")
            ).forEach { db.accountingDao().insertAccount(it) }

            repeat(12) { idx ->
                db.salesDao().insertInvoice(
                    SalesInvoice(
                        id = "inv-$idx",
                        invoiceNumber = "INV-10$idx",
                        customerId = "c1",
                        customerName = "রহিম ট্রেডার্স",
                        warehouseId = "w1",
                        warehouseName = "Main WH",
                        status = if (idx == 0) "PARTIALLY_PAID" else "PAID",
                        totalAmountMinor = 1_500_000L,
                        paidAmountMinor = if (idx == 0) 1_000_000L else 1_500_000L,
                        dueAmountMinor = if (idx == 0) 500_000L else 0L,
                        invoiceDate = now - idx * 60_000L,
                        createdAt = now - idx * 60_000L,
                        isPosted = true
                    )
                )
            }
            db.customerPaymentDao().insertPayment(
                CustomerPayment("cp-1", "PAY-1042", "c1", "রহিম ট্রেডার্স", 1_000_000L, paymentDate = now - 30_000L)
            )
            db.purchaseOrderDao().insertOrder(
                PurchaseOrder(
                    id = "po-1",
                    poNumber = "PO-2031",
                    supplierId = "s1",
                    currency = "BDT",
                    exchangeRateToBdt = "1",
                    totalAmountMinor = 4_200_000L,
                    totalBdtAmountMinor = 4_200_000L,
                    status = "CONFIRMED",
                    orderDate = now - 90_000L,
                    createdAt = now - 90_000L
                )
            )
            db.supplierPaymentDao().insertPayment(
                SupplierPayment("sp-1", "SP-2026-0001", "s1", "po-1", 500_000L, "BDT", "1", 500_000L, paymentDate = now - 20_000L)
            )
            db.expenseDao().insertExpense(
                ExpenseVoucher("exp-1", "EXP-001", "RENT_UTILITIES", "Office Rent", "অফিস ভাড়া", 200_000L, "BDT", "1", 200_000L, "coa-1010", "coa-5190", voucherDate = now - 10_000L)
            )

            val accountingRepo = AccountingRepositoryImpl(db, db.accountingDao())
            val salesRepo = SalesRepositoryImpl(
                db, db.salesDao(), db.customerPaymentDao(), db.salesReturnDao(),
                db.customerDao(), db.warehouseDao(), db.productDao(), db.stockBalanceDao(),
                db.stockMovementDao(), db.inventoryCostLayerDao(), accountingRepo, DocumentNumberService(db)
            )
            val importRepo = ImportRepositoryImpl(
                db, db.purchaseOrderDao(), db.supplierPaymentDao(), db.importShipmentDao(),
                db.importCostDao(), db.warehouseReceiptDao(), db.inventoryCostLayerDao(),
                db.minimalImportDao(), db.supplierDao(), db.productDao(), db.productVariantDao(),
                db.stockBalanceDao(), db.stockMovementDao(), accountingRepo
            )
            val financialRepo = FinancialRepositoryImpl(
                db, db.accountingDao(), db.expenseDao(), db.cashBankDao(), db.auditLogDao(),
                db.salesDao(), db.customerPaymentDao(), db.supplierPaymentDao(),
                db.purchaseOrderDao(), db.customerDao(), db.supplierDao(), accountingRepo
            )
            val ledgerRepo = BusinessControlCenterRepositoryImpl(
                context, db, db.salesDao(), db.customerPaymentDao(), db.salesReturnDao(),
                db.purchaseOrderDao(), db.supplierPaymentDao(), db.expenseDao(),
                db.stockMovementDao(), db.stockTransferDao(), db.customerDao(), db.supplierDao(),
                db.warehouseDao(), db.productCategoryDao(), db.productDao(), db.productVariantDao(),
                db.businessProfileDao()
            )
            val vm = BusinessControlCenterViewModel(ledgerRepo, salesRepo, importRepo, financialRepo, accountingRepo, prefs)

            compose.setContent {
                AdvanceErpTheme(darkTheme = false) {
                    Scaffold(
                        topBar = {
                            ERPTopBar(
                                title = "কেন্দ্রীয় লেজার",
                                subtitle = "My Business",
                                currentLanguage = AppLanguage.BANGLA,
                                onOpenDrawer = {},
                                onToggleLanguage = {}
                            )
                        },
                        bottomBar = {
                            ERPBottomNavigation(
                                currentRoute = "ledger",
                                onNavigate = {},
                                modifier = Modifier.testTag("ledger_test_bottom_nav")
                            )
                        }
                    ) { padding ->
                        BusinessControlCenterScreen(
                            viewModel = vm,
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }

            compose.waitUntil(8_000) {
                compose.onAllNodesWithText("মোট বিক্রয়").fetchSemanticsNodes().isNotEmpty()
            }

            // 1. Segmented 4 tabs & Summary Cards render
            compose.onNodeWithTag("ledger_tab_SUMMARY").assertExists()
            compose.onNodeWithTag("ledger_tab_TRANSACTIONS").assertExists()
            compose.onNodeWithTag("ledger_tab_OUTSTANDING_DUE").assertExists()
            compose.onNodeWithTag("ledger_tab_STATEMENTS").assertExists()
            compose.onNodeWithText("মোট বিক্রয়").assertExists()
            compose.onNodeWithText("মোট দেনা").assertExists()

            // 2. Open Filter Drawer via Filter icon and close via Back
            compose.onNodeWithTag("ledger_filter_button").performClick()
            compose.onNodeWithTag("filter_drawer_panel").assertExists()
            compose.onNodeWithTag("filter_drawer_close").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("filter_drawer_panel").assertDoesNotExist()

            // 3. Open Filter Drawer via Right-to-Left swipe, apply Sales filter, verify indicator, then reset
            compose.onNodeWithTag("ledger_transaction_list").performTouchInput { swipeLeft() }
            compose.onNodeWithTag("filter_drawer_panel").assertExists()
            compose.onNodeWithText("বিক্রয়").performClick()
            compose.onNodeWithTag("filter_drawer_apply").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("ledger_active_filter_indicator").assertExists()
            compose.onNodeWithText("রিসেট").performClick()
            compose.waitForIdle()

            // 4. FAB sits above BottomNav, expands/collapses smoothly, excludes Filter & Date Range
            val fabBounds = compose.onNodeWithTag("ledger_fab_main").fetchSemanticsNode().boundsInRoot
            val bottomNavBounds = compose.onNodeWithTag("ledger_test_bottom_nav").fetchSemanticsNode().boundsInRoot
            assertTrue("FAB must sit above Bottom Navigation", fabBounds.bottom <= bottomNavBounds.top)

            compose.onNodeWithTag("ledger_fab_main").performClick()
            compose.onNodeWithTag("ledger_fab_menu").assertExists()
            compose.onNodeWithTag("ledger_fab_export").assertExists()
            compose.onNodeWithTag("ledger_fab_make_payment").assertExists()
            compose.onNodeWithTag("ledger_fab_receive_payment").assertExists()
            compose.onNodeWithTag("ledger_fab_expense").assertExists()
            compose.onNodeWithTag("ledger_fab_journal").assertExists()
            compose.onNodeWithText("উন্নত ফিল্টার").assertDoesNotExist()
            compose.onNodeWithText("তারিখ (Date Range)").assertDoesNotExist()

            // Close FAB via outside scrim tap
            compose.onNodeWithTag("ledger_fab_scrim").performClick()
            compose.onNodeWithTag("ledger_fab_menu").assertDoesNotExist()

            // 5. Scroll down collapses summary cards
            compose.onNodeWithTag("ledger_transaction_list").performTouchInput { swipeUp() }
            compose.waitForIdle()

            // 6. Switch across Transactions, Outstanding Due, and Statements tabs smoothly
            compose.onNodeWithTag("ledger_tab_TRANSACTIONS").performClick()
            compose.onNodeWithTag("ledger_sticky_search").assertIsDisplayed()

            compose.onNodeWithTag("ledger_tab_OUTSTANDING_DUE").performClick()
            compose.onNodeWithTag("ledger_due_list").assertExists()
            compose.onNodeWithText("রহিম ট্রেডার্স").assertExists()
            compose.onNodeWithText("China Supplier").assertExists()

            compose.onNodeWithTag("ledger_tab_STATEMENTS").performClick()
            compose.onNodeWithTag("ledger_statements_list").assertExists()
        } finally {
            prefs.setLanguage(prevLang)
            prefs.setUserRole(prevRole)
            db.close()
        }
    }
}
