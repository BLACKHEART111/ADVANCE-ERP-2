package com.pocketdev.advance_erp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.pocketdev.advance_erp.core.designsystem.DarkDashboardTokens
import com.pocketdev.advance_erp.core.designsystem.LightDashboardTokens
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.core.pdf.CentralizedPdfReportRegistry
import com.pocketdev.advance_erp.domain.repository.DashboardSummary
import com.pocketdev.advance_erp.domain.repository.DashboardTransaction
import com.pocketdev.advance_erp.domain.repository.DateWiseMetrics
import com.pocketdev.advance_erp.domain.repository.HistoricalClosingBalances
import com.pocketdev.advance_erp.domain.repository.MonthlyPerformanceMetrics
import com.pocketdev.advance_erp.feature.dashboard.components.DASHBOARD_TRANSACTIONS_PREVIEW_LIMIT
import com.pocketdev.advance_erp.feature.dashboard.components.DashboardCollapseState
import com.pocketdev.advance_erp.feature.dashboard.components.DashboardSection
import com.pocketdev.advance_erp.feature.dashboard.components.formatDashboardDate
import com.pocketdev.advance_erp.feature.dashboard.components.sliceDashboardTransactions
import com.pocketdev.advance_erp.feature.dashboard.components.toggle
import com.pocketdev.advance_erp.feature.dashboard.maskDashboardSummaryForRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DashboardLogicTest {

    @Test
    fun formatDashboardDate_formatsBanglaAndEnglishAccurately() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 21)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val millis = cal.timeInMillis

        assertEquals("২১ নভেম্বর ২০২৬", formatDashboardDate(millis, AppLanguage.BANGLA, shortMonth = false))
        assertEquals("২১ নভে ২০২৬", formatDashboardDate(millis, AppLanguage.BANGLA, shortMonth = true))
        assertEquals("21 November 2026", formatDashboardDate(millis, AppLanguage.ENGLISH, shortMonth = false))
        assertEquals("21 Nov 2026", formatDashboardDate(millis, AppLanguage.ENGLISH, shortMonth = true))
    }

    @Test
    fun collapseState_preservesDefaultsAndTogglesEachSectionIndependently() {
        val initial = DashboardCollapseState()
        assertTrue(initial.overviewExpanded)
        assertTrue(initial.dailySummaryExpanded)
        assertFalse(initial.dailyExtraExpanded)
        assertFalse(initial.closingPositionExpanded)
        assertTrue(initial.recentTransactionsExpanded)

        val step1 = initial.toggle(DashboardSection.CLOSING_POSITION)
        assertTrue(step1.overviewExpanded)
        assertTrue(step1.dailySummaryExpanded)
        assertFalse(step1.dailyExtraExpanded)
        assertTrue(step1.closingPositionExpanded)
        assertTrue(step1.recentTransactionsExpanded)

        val step2 = step1.toggle(DashboardSection.OVERVIEW)
        assertFalse(step2.overviewExpanded)
        assertTrue(step2.dailySummaryExpanded)
        assertTrue(step2.closingPositionExpanded)
        assertTrue(step2.recentTransactionsExpanded)

        val step3 = step2.toggle(DashboardSection.DAILY_EXTRA_METRICS)
        assertTrue(step3.dailyExtraExpanded)
        assertTrue(step3.dailySummaryExpanded)

        val step4 = step3.toggle(DashboardSection.DAILY_SUMMARY)
        assertFalse(step4.dailySummaryExpanded)
        assertTrue(step4.dailyExtraExpanded)

        val step5 = step4.toggle(DashboardSection.RECENT_TRANSACTIONS)
        assertFalse(step5.recentTransactionsExpanded)
        assertTrue(step5.closingPositionExpanded)
    }

    @Test
    fun sliceDashboardTransactions_capsLongListsByDefaultAndSupportsExpandAll() {
        fun sampleTx(index: Int) = DashboardTransaction(
            id = "tx-$index",
            title = "PU STONE WHOLESALE #$index",
            subtitle = "Main Warehouse • 40 units",
            amountMinor = 3_237_880L,
            currency = CurrencyCode.BDT,
            isCredit = true,
            timestamp = 1_795_680_000_000L + index,
            statusText = "In Stock",
            type = "PURCHASE_RECEIPT"
        )

        val emptySlice = sliceDashboardTransactions(emptyList(), showAll = false)
        assertEquals(0, emptySlice.visibleTransactions.size)
        assertEquals(0, emptySlice.totalCount)
        assertEquals(0, emptySlice.hiddenCount)
        assertFalse(emptySlice.canToggleExpandAll)

        val shortList = (1..DASHBOARD_TRANSACTIONS_PREVIEW_LIMIT).map(::sampleTx)
        val shortSlice = sliceDashboardTransactions(shortList, showAll = false)
        assertEquals(DASHBOARD_TRANSACTIONS_PREVIEW_LIMIT, shortSlice.visibleTransactions.size)
        assertEquals(DASHBOARD_TRANSACTIONS_PREVIEW_LIMIT, shortSlice.totalCount)
        assertEquals(0, shortSlice.hiddenCount)
        assertFalse(shortSlice.canToggleExpandAll)

        val longList = (1..32).map(::sampleTx)
        val collapsedSlice = sliceDashboardTransactions(longList, showAll = false)
        assertEquals(DASHBOARD_TRANSACTIONS_PREVIEW_LIMIT, collapsedSlice.visibleTransactions.size)
        assertEquals(32, collapsedSlice.totalCount)
        assertEquals(32 - DASHBOARD_TRANSACTIONS_PREVIEW_LIMIT, collapsedSlice.hiddenCount)
        assertTrue(collapsedSlice.canToggleExpandAll)

        val expandedSlice = sliceDashboardTransactions(longList, showAll = true)
        assertEquals(32, expandedSlice.visibleTransactions.size)
        assertEquals(32, expandedSlice.totalCount)
        assertEquals(0, expandedSlice.hiddenCount)
        assertTrue(expandedSlice.canToggleExpandAll)
    }

    @Test
    fun maskDashboardSummaryForRole_hidesAllProfitForNonAdminAndPreservesOtherData() {
        val sample = DashboardSummary(
            todaysSales = Money.ofMinor(50_000L, CurrencyCode.BDT),
            totalReceivable = Money.ofMinor(25_000L, CurrencyCode.BDT),
            totalPayable = Money.ofMinor(10_000L, CurrencyCode.BDT),
            stockValuation = Money.ofMinor(120_000L, CurrencyCode.BDT),
            productCount = 15,
            lowStockCount = 2,
            customerCount = 8,
            supplierCount = 4,
            recentTransactions = emptyList(),
            isLiveDatabaseData = true,
            totalCashBank = Money.ofMinor(95_000L, CurrencyCode.BDT),
            totalExpenses = Money.ofMinor(5_000L, CurrencyCode.BDT),
            grossProfit = Money.ofMinor(18_000L, CurrencyCode.BDT),
            netProfit = Money.ofMinor(13_000L, CurrencyCode.BDT),
            dateWiseMetrics = DateWiseMetrics(
                selectedDateMillis = 1_795_200_000_000L,
                totalSales = Money.ofMinor(50_000L, CurrencyCode.BDT),
                totalPurchases = Money.ofMinor(30_000L, CurrencyCode.BDT),
                totalCollections = Money.ofMinor(20_000L, CurrencyCode.BDT),
                totalPayments = Money.ofMinor(12_000L, CurrencyCode.BDT),
                totalExpenses = Money.ofMinor(5_000L, CurrencyCode.BDT),
                totalReturns = Money.ofMinor(1_000L, CurrencyCode.BDT),
                stockInUnits = 20,
                stockOutUnits = 8,
                netCashFlow = Money.ofMinor(8_000L, CurrencyCode.BDT),
                dailyGrossProfit = Money.ofMinor(18_000L, CurrencyCode.BDT),
                transactionCount = 4
            ),
            closingBalances = HistoricalClosingBalances(
                asOfDateMillis = 1_795_200_000_000L,
                closingCashBank = Money.ofMinor(95_000L, CurrencyCode.BDT),
                customerOutstanding = Money.ofMinor(25_000L, CurrencyCode.BDT),
                supplierOutstanding = Money.ofMinor(10_000L, CurrencyCode.BDT),
                inventoryValuation = Money.ofMinor(120_000L, CurrencyCode.BDT)
            ),
            monthlyMetrics = MonthlyPerformanceMetrics(
                totalSales = Money.ofMinor(400_000L, CurrencyCode.BDT),
                totalPurchases = Money.ofMinor(250_000L, CurrencyCode.BDT),
                totalCollections = Money.ofMinor(320_000L, CurrencyCode.BDT),
                totalExpenses = Money.ofMinor(30_000L, CurrencyCode.BDT),
                netProfit = Money.ofMinor(120_000L, CurrencyCode.BDT)
            )
        )

        val adminView = maskDashboardSummaryForRole(sample, canViewProfit = true, currency = CurrencyCode.BDT)
        assertEquals(18_000L, adminView.grossProfit.minorUnits)
        assertEquals(13_000L, adminView.netProfit.minorUnits)
        assertEquals(18_000L, adminView.dateWiseMetrics?.dailyGrossProfit?.minorUnits)
        assertEquals(120_000L, adminView.monthlyMetrics?.netProfit?.minorUnits)

        val staffView = maskDashboardSummaryForRole(sample, canViewProfit = false, currency = CurrencyCode.BDT)
        assertEquals(0L, staffView.grossProfit.minorUnits)
        assertEquals(0L, staffView.netProfit.minorUnits)
        assertEquals(0L, staffView.dateWiseMetrics?.dailyGrossProfit?.minorUnits)
        assertEquals(0L, staffView.monthlyMetrics?.netProfit?.minorUnits)
        assertEquals(50_000L, staffView.dateWiseMetrics?.totalSales?.minorUnits)
        assertEquals(95_000L, staffView.closingBalances?.closingCashBank?.minorUnits)
        assertEquals(400_000L, staffView.monthlyMetrics?.totalSales?.minorUnits)
    }

    @Test
    fun pdfReports_excludeProfitRowsAndKpisWhenIncludeProfitIsFalse() {
        val dailyHidden = CentralizedPdfReportRegistry.buildDailyBusinessSnapshotReport(
            dateMillis = 1_795_200_000_000L,
            totalSalesMinor = 50_000L,
            totalPurchasesMinor = 20_000L,
            totalCollectionsMinor = 15_000L,
            totalPaymentsMinor = 5_000L,
            totalExpensesMinor = 2_000L,
            netCashFlowMinor = 10_000L,
            dailyProfitMinor = 12_000L,
            closingCashBankMinor = 100_000L,
            closingArMinor = 25_000L,
            closingApMinor = 10_000L,
            closingInventoryMinor = 80_000L,
            isBangla = true,
            includeProfit = false
        )
        assertFalse(dailyHidden.sections.first().rows.any { row -> row.cells.any { it.contains("Profit", ignoreCase = true) || it.contains("লাভ") } })
        assertFalse(dailyHidden.kpiBadges.any { it.labelEn.contains("Profit", ignoreCase = true) || it.labelBn?.contains("লাভ") == true })

        val dailyVisible = CentralizedPdfReportRegistry.buildDailyBusinessSnapshotReport(
            dateMillis = 1_795_200_000_000L,
            totalSalesMinor = 50_000L,
            totalPurchasesMinor = 20_000L,
            totalCollectionsMinor = 15_000L,
            totalPaymentsMinor = 5_000L,
            totalExpensesMinor = 2_000L,
            netCashFlowMinor = 10_000L,
            dailyProfitMinor = 12_000L,
            closingCashBankMinor = 100_000L,
            closingArMinor = 25_000L,
            closingApMinor = 10_000L,
            closingInventoryMinor = 80_000L,
            isBangla = true,
            includeProfit = true
        )
        assertTrue(dailyVisible.sections.first().rows.any { row -> row.cells.any { it.contains("Profit", ignoreCase = true) } })
        assertTrue(dailyVisible.kpiBadges.any { it.labelEn.contains("Profit", ignoreCase = true) })

        val monthlyHidden = CentralizedPdfReportRegistry.buildMonthlyPerformanceReport(
            monthName = "November 2026",
            salesMinor = 400_000L,
            purchasesMinor = 200_000L,
            collectionsMinor = 300_000L,
            expensesMinor = 40_000L,
            netProfitMinor = 160_000L,
            isBangla = false,
            includeProfit = false
        )
        assertFalse(monthlyHidden.sections.first().rows.any { row -> row.cells.any { it.contains("Profit", ignoreCase = true) || it.contains("লাভ") } })
        assertFalse(monthlyHidden.kpiBadges.any { it.labelEn.contains("Profit", ignoreCase = true) || it.labelBn?.contains("মুনাফা") == true })
    }

    @Test
    fun dashboardTokens_enforceContrastAndNonPureBlackDarkMode() {
        assertFalse(LightDashboardTokens.isDark)
        assertTrue(DarkDashboardTokens.isDark)

        assertNotEquals(Color.Black, DarkDashboardTokens.pageBgTop)
        assertNotEquals(Color.Black, DarkDashboardTokens.pageBgBottom)
        assertNotEquals(Color.Black, DarkDashboardTokens.cardBgStart)
        assertNotEquals(Color.Black, DarkDashboardTokens.cardBgEnd)
        assertNotEquals(Color.Black, DarkDashboardTokens.overviewBgTop)

        assertTrue(DarkDashboardTokens.pageBgTop.luminance() < 0.05f)
        assertTrue(DarkDashboardTokens.cardBgStart.luminance() < 0.05f)
        assertTrue(DarkDashboardTokens.cardValueText.luminance() > 0.90f)
        assertTrue(DarkDashboardTokens.sectionTitleText.luminance() > 0.85f)

        assertTrue(LightDashboardTokens.pageBgTop.luminance() > 0.85f)
        assertTrue(LightDashboardTokens.cardBgStart.luminance() > 0.90f)
        assertTrue(LightDashboardTokens.cardValueText.luminance() < 0.08f)
        assertTrue(LightDashboardTokens.sectionTitleText.luminance() < 0.08f)
    }
}
