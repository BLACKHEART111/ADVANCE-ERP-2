package com.pocketdev.advance_erp.feature.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.core.designsystem.DashboardColorTokens
import com.pocketdev.advance_erp.core.designsystem.ERPLoadingState
import com.pocketdev.advance_erp.core.designsystem.LocalDashboardTokens
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.pdf.PdfViewerDialog
import com.pocketdev.advance_erp.core.security.rememberBiometricCostVisibility
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.feature.dashboard.components.CollapsibleBusinessOverviewCard
import com.pocketdev.advance_erp.feature.dashboard.components.CollapsibleSectionHeader
import com.pocketdev.advance_erp.feature.dashboard.components.CompactDateSelectorCard
import com.pocketdev.advance_erp.feature.dashboard.components.DashboardEmptyTransactionsCard
import com.pocketdev.advance_erp.feature.dashboard.components.DashboardPdfShortcutsRow
import com.pocketdev.advance_erp.feature.dashboard.components.DashboardTransactionsCard
import com.pocketdev.advance_erp.feature.dashboard.components.ExpandMoreMetricsButton
import com.pocketdev.advance_erp.feature.dashboard.components.PremiumDashboardHeader
import com.pocketdev.advance_erp.feature.dashboard.components.PremiumDashboardStatCard
import com.pocketdev.advance_erp.feature.dashboard.components.StockAndTransactionStatsBar
import com.pocketdev.advance_erp.feature.dashboard.components.rememberFormattedDashboardDate

private data class DashboardFabAction(
    val label: String,
    val icon: ImageVector,
    val iconTint: Color,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToStockAdjust: () -> Unit,
    onNavigateToImports: () -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToLedger: () -> Unit = onNavigateToStockAdjust
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    DashboardContent(
        state = state,
        onSelectPreviousDay = viewModel::selectPreviousDay,
        onSelectNextDay = viewModel::selectNextDay,
        onSelectToday = viewModel::selectToday,
        onSelectDate = viewModel::selectDate,
        onGenerateDailySnapshotPdf = { viewModel.generateDailySnapshotPdf(context) },
        onGenerateMonthlyPerformancePdf = { viewModel.generateMonthlyPerformancePdf(context) },
        onClearGeneratedPdf = viewModel::clearGeneratedPdf,
        onNavigateToAddProduct = onNavigateToAddProduct,
        onNavigateToImports = onNavigateToImports,
        onNavigateToSales = onNavigateToSales,
        onNavigateToTransactions = onNavigateToTransactions,
        onNavigateToLedger = onNavigateToLedger,
        modifier = modifier
    )
}

@Composable
internal fun DashboardContent(
    state: DashboardUiState,
    onSelectPreviousDay: () -> Unit,
    onSelectNextDay: () -> Unit,
    onSelectToday: () -> Unit,
    onSelectDate: (Long) -> Unit,
    onGenerateDailySnapshotPdf: () -> Unit,
    onGenerateMonthlyPerformancePdf: () -> Unit,
    onClearGeneratedPdf: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToImports: () -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToLedger: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        ERPLoadingState(modifier = modifier.fillMaxSize())
        return
    }

    val tokens = LocalDashboardTokens.current
    val summary = state.summary
    val language = state.language
    val isBangla = language == AppLanguage.BANGLA
    val dateMetrics = summary?.dateWiseMetrics
    val closingBalances = summary?.closingBalances

    var showCalendarPicker by rememberSaveable { mutableStateOf(false) }
    var isFabExpanded by rememberSaveable { mutableStateOf(false) }
    var overviewExpanded by rememberSaveable { mutableStateOf(true) }
    var dailySummaryExpanded by rememberSaveable { mutableStateOf(true) }
    var dailyExtraExpanded by rememberSaveable { mutableStateOf(false) }
    var closingPositionExpanded by rememberSaveable { mutableStateOf(false) }

    val profitVisibility = rememberBiometricCostVisibility(
        key = listOf("dashboard-daily-profit", state.selectedDateMillis, state.canViewProfit),
        allowed = state.canViewProfit,
        isBangla = isBangla
    )

    BackHandler(enabled = isFabExpanded) {
        isFabExpanded = false
    }

    val selectedDateText = rememberFormattedDashboardDate(state.selectedDateMillis, language)

    val fabActions = listOf(
        DashboardFabAction(
            label = "PO",
            icon = Icons.Outlined.Inventory,
            iconTint = tokens.accentPurple,
            testTag = "dashboard_fab_action_po",
            onClick = onNavigateToImports
        ),
        DashboardFabAction(
            label = "Sales",
            icon = Icons.Outlined.ShoppingCart,
            iconTint = tokens.accentGreen,
            testTag = "dashboard_fab_action_sales",
            onClick = onNavigateToSales
        ),
        DashboardFabAction(
            label = "Add Product",
            icon = Icons.Outlined.Inventory2,
            iconTint = tokens.accentBlue,
            testTag = "dashboard_fab_action_add_product",
            onClick = onNavigateToAddProduct
        ),
        DashboardFabAction(
            label = "Ledger",
            icon = Icons.Outlined.ReceiptLong,
            iconTint = tokens.accentAmber,
            testTag = "dashboard_fab_action_ledger",
            onClick = onNavigateToLedger
        )
    )

    val fabRotation by animateFloatAsState(
        targetValue = if (isFabExpanded) 45f else 0f,
        label = "dashboard_fab_rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(tokens.pageBgTop, tokens.pageBgBottom)))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_scroll_list"),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Item 0: Premium Shipping-Port Gradient Hero + Compact Date Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumDashboardHeader(
                        companyName = state.companyName,
                        language = language,
                        tokens = tokens
                    )

                    CompactDateSelectorCard(
                        formattedDate = selectedDateText,
                        language = language,
                        selectedDateMillis = state.selectedDateMillis,
                        showCalendarPicker = showCalendarPicker,
                        tokens = tokens,
                        onSelectPreviousDay = onSelectPreviousDay,
                        onSelectNextDay = onSelectNextDay,
                        onSelectToday = onSelectToday,
                        onToggleCalendarPicker = { showCalendarPicker = !showCalendarPicker },
                        onDateSelected = { millis ->
                            onSelectDate(millis)
                            showCalendarPicker = false
                        },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .testTag("dashboard_date_selector")
                    )
                }
            }

            // Item 1: Collapsible Business Overview Banner (Expanded by default)
            item {
                CollapsibleBusinessOverviewCard(
                    productCount = summary?.productCount ?: 0,
                    lowStockCount = summary?.lowStockCount ?: 0,
                    customerCount = summary?.customerCount ?: 0,
                    supplierCount = summary?.supplierCount ?: 0,
                    isLiveData = summary?.isLiveDatabaseData == true,
                    language = language,
                    expanded = overviewExpanded,
                    tokens = tokens,
                    onToggleExpand = { overviewExpanded = !overviewExpanded },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("dashboard_business_overview")
                )
            }

            // Item 2: Collapsible Daily Financial Summary (Primary 4 + Expandable 4 + Stock Stats Bar)
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("dashboard_daily_metrics"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CollapsibleSectionHeader(
                        icon = Icons.Outlined.BarChart,
                        title = if (isBangla) "$selectedDateText এর ব্যবসায়িক তথ্য" else "Business Metrics for $selectedDateText",
                        subtitle = if (isBangla) {
                            "নির্বাচিত দিনের মোট ক্রয়, বিক্রয়, নগদ প্রবাহ ও লাভ"
                        } else {
                            "Sales, procurement, net cash flow & daily profit"
                        },
                        tokens = tokens,
                        expanded = dailySummaryExpanded,
                        onToggleExpand = { dailySummaryExpanded = !dailySummaryExpanded },
                        collapsedHint = if (isBangla) "দেখুন" else "Expand",
                        headerTestTag = "dashboard_daily_summary_header"
                    )

                    AnimatedVisibility(
                        visible = dailySummaryExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Row 1: Sales & Purchases
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PremiumDashboardStatCard(
                                    title = if (isBangla) "দিনের বিক্রয় আয়" else "Daily Sales Revenue",
                                    value = Formatters.formatMoney(dateMetrics?.totalSales?.minorUnits ?: 0L, state.currency, language),
                                    icon = Icons.Outlined.PointOfSale,
                                    accentColor = tokens.accentPurple,
                                    badgeText = if (isBangla) "বিক্রয়" else "Day Sales",
                                    tokens = tokens,
                                    modifier = Modifier.weight(1f)
                                )
                                PremiumDashboardStatCard(
                                    title = if (isBangla) "দিনের ক্রয় মূল্য" else "Daily Purchase Value",
                                    value = Formatters.formatMoney(dateMetrics?.totalPurchases?.minorUnits ?: 0L, state.currency, language),
                                    icon = Icons.Outlined.ShoppingCart,
                                    accentColor = tokens.accentBlue,
                                    badgeText = if (isBangla) "ক্রয়" else "Procurement",
                                    tokens = tokens,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Row 2: Customer Payments Received & Supplier Payments Made
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PremiumDashboardStatCard(
                                    title = if (isBangla) "গ্রাহক আদায় (কালেকশন)" else "Customer Collections",
                                    value = Formatters.formatMoney(dateMetrics?.totalCollections?.minorUnits ?: 0L, state.currency, language),
                                    icon = Icons.Outlined.ArrowDownward,
                                    accentColor = tokens.accentGreen,
                                    badgeText = if (isBangla) "নগদ প্রবাহ" else "Cash In",
                                    tokens = tokens,
                                    modifier = Modifier.weight(1f)
                                )
                                PremiumDashboardStatCard(
                                    title = if (isBangla) "সরবরাহকারী পরিশোধ" else "Supplier Payments",
                                    value = Formatters.formatMoney(dateMetrics?.totalPayments?.minorUnits ?: 0L, state.currency, language),
                                    icon = Icons.Outlined.ArrowUpward,
                                    accentColor = tokens.accentRed,
                                    badgeText = if (isBangla) "নগদ বহিঃপ্রবাহ" else "Cash Out",
                                    tokens = tokens,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            ExpandMoreMetricsButton(
                                expanded = dailyExtraExpanded,
                                extraCount = 4,
                                language = language,
                                tokens = tokens,
                                onClick = { dailyExtraExpanded = !dailyExtraExpanded }
                            )

                            AnimatedVisibility(
                                visible = dailyExtraExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Row 3: Operating Expenses & Sales Returns
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        PremiumDashboardStatCard(
                                            title = if (isBangla) "দৈনিক খরচ" else "Daily Expenses",
                                            value = Formatters.formatMoney(dateMetrics?.totalExpenses?.minorUnits ?: 0L, state.currency, language),
                                            icon = Icons.Outlined.ReceiptLong,
                                            accentColor = tokens.accentAmber,
                                            badgeText = if (isBangla) "পরিচালন ব্যয়" else "Operating",
                                            tokens = tokens,
                                            modifier = Modifier.weight(1f)
                                        )
                                        PremiumDashboardStatCard(
                                            title = if (isBangla) "বিক্রয় ফেরত" else "Sales Returns",
                                            value = Formatters.formatMoney(dateMetrics?.totalReturns?.minorUnits ?: 0L, state.currency, language),
                                            icon = Icons.Outlined.SwapHoriz,
                                            accentColor = tokens.accentPurple,
                                            badgeText = if (isBangla) "ফেরত" else "Returns",
                                            tokens = tokens,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Row 4: Net Cash Flow & Owner-Only Daily Gross Profit
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        val netCashMinor = dateMetrics?.netCashFlow?.minorUnits ?: 0L
                                        PremiumDashboardStatCard(
                                            title = if (isBangla) "নিট নগদ প্রবাহ" else "Net Cash Flow",
                                            value = Formatters.formatMoney(netCashMinor, state.currency, language),
                                            icon = Icons.Outlined.ReceiptLong,
                                            accentColor = if (netCashMinor >= 0) tokens.accentGreen else tokens.accentRed,
                                            badgeText = if (isBangla) "আদায় − পরিশোধ" else "In - Out",
                                            tokens = tokens,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (state.canViewProfit) {
                                            val dailyProfitMinor = dateMetrics?.dailyGrossProfit?.minorUnits ?: 0L
                                            val profitDisplay = if (profitVisibility.isVisible) {
                                                Formatters.formatMoney(dailyProfitMinor, state.currency, language)
                                            } else {
                                                "${state.currency.symbol} •••••"
                                            }
                                            PremiumDashboardStatCard(
                                                title = if (isBangla) "দৈনিক মোট লাভ" else "Daily Gross Profit",
                                                value = profitDisplay,
                                                icon = Icons.Outlined.CheckCircle,
                                                accentColor = if (!profitVisibility.isVisible || dailyProfitMinor >= 0) tokens.accentTeal else tokens.accentRed,
                                                badgeText = if (isBangla) "বিক্রয় − ব্যয়" else "Sales - COGS",
                                                tokens = tokens,
                                                onClick = profitVisibility.toggle,
                                                trailingIcon = if (profitVisibility.isVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("dashboard_daily_profit_card")
                                            )
                                        } else {
                                            PremiumDashboardStatCard(
                                                title = if (isBangla) "দৈনিক লেনদেন সংখ্যা" else "Daily Transactions",
                                                value = "${dateMetrics?.transactionCount ?: 0} events",
                                                icon = Icons.Outlined.Assessment,
                                                accentColor = tokens.accentBlue,
                                                badgeText = if (isBangla) "লেনদেন" else "Activity",
                                                tokens = tokens,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            // 3-Column Stock Movement & Activity Statistics Bar
                            StockAndTransactionStatsBar(
                                stockInUnits = dateMetrics?.stockInUnits ?: 0,
                                stockOutUnits = dateMetrics?.stockOutUnits ?: 0,
                                transactionCount = dateMetrics?.transactionCount ?: 0,
                                language = language,
                                tokens = tokens
                            )
                        }
                    }
                }
            }

            // Item 3: Report Shortcut Pills (Daily PDF Snapshot & Monthly Performance Review)
            item {
                DashboardPdfShortcutsRow(
                    isBangla = isBangla,
                    tokens = tokens,
                    onGenerateDailySnapshotPdf = onGenerateDailySnapshotPdf,
                    onGenerateMonthlyPerformancePdf = onGenerateMonthlyPerformancePdf,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Item 4: Collapsible Closing Position Section (Collapsed by default)
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("dashboard_closing_position"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CollapsibleSectionHeader(
                        icon = Icons.Outlined.CalendarMonth,
                        title = if (isBangla) "$selectedDateText পর্যন্ত সমাপনী স্থিতি" else "Closing Balances as of $selectedDateText",
                        subtitle = if (isBangla) {
                            "নগদ ও ব্যাংক, গ্রাহক পাওনা, সরবরাহকারী দেনা ও মজুদ মূল্যায়ন"
                        } else {
                            "Cumulative closing cash, receivables, payables & inventory"
                        },
                        tokens = tokens,
                        expanded = closingPositionExpanded,
                        onToggleExpand = { closingPositionExpanded = !closingPositionExpanded },
                        collapsedHint = if (isBangla) "দেখুন" else "Expand",
                        headerTestTag = "dashboard_closing_position_header"
                    )

                    AnimatedVisibility(
                        visible = closingPositionExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PremiumDashboardStatCard(
                                    title = if (isBangla) "সমাপনী নগদ ও ব্যাংক" else "Closing Cash & Bank",
                                    value = Formatters.formatMoney(closingBalances?.closingCashBank?.minorUnits ?: 0L, state.currency, language),
                                    icon = Icons.Outlined.ReceiptLong,
                                    accentColor = tokens.accentBlue,
                                    badgeText = if (isBangla) "তরল তহবিল" else "Liquid Funds",
                                    tokens = tokens,
                                    modifier = Modifier.weight(1f)
                                )
                                PremiumDashboardStatCard(
                                    title = if (isBangla) "গ্রাহক বকেয়া (পাওনা)" else "Customer Receivables",
                                    value = Formatters.formatMoney(closingBalances?.customerOutstanding?.minorUnits ?: 0L, state.currency, language),
                                    icon = Icons.Outlined.ArrowDownward,
                                    accentColor = tokens.accentGreen,
                                    badgeText = if (isBangla) "আদায়যোগ্য" else "AR Due",
                                    tokens = tokens,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PremiumDashboardStatCard(
                                    title = if (isBangla) "সরবরাহকারী দেনা" else "Supplier Payables",
                                    value = Formatters.formatMoney(closingBalances?.supplierOutstanding?.minorUnits ?: 0L, state.currency, language),
                                    icon = Icons.Outlined.ArrowUpward,
                                    accentColor = tokens.accentRed,
                                    badgeText = if (isBangla) "পরিশোধযোগ্য" else "AP Due",
                                    tokens = tokens,
                                    modifier = Modifier.weight(1f)
                                )
                                PremiumDashboardStatCard(
                                    title = if (isBangla) "মজুদ সম্পদ মূল্যায়ন" else "FIFO Inventory Valuation",
                                    value = Formatters.formatMoney(closingBalances?.inventoryValuation?.minorUnits ?: 0L, state.currency, language),
                                    icon = Icons.Outlined.Inventory2,
                                    accentColor = tokens.accentTeal,
                                    badgeText = if (isBangla) "FIFO সম্পদ" else "FIFO Asset",
                                    tokens = tokens,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Item 5: Recent Transactions Section (Visible)
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CollapsibleSectionHeader(
                        icon = Icons.Outlined.Layers,
                        title = if (isBangla) "$selectedDateText এর লেনদেন সমূহ" else "Transactions on $selectedDateText",
                        tokens = tokens,
                        actionText = if (isBangla) "সব দেখুন" else "View All",
                        onActionClick = onNavigateToTransactions
                    )

                    val transactions = summary?.recentTransactions.orEmpty()
                    if (transactions.isEmpty()) {
                        DashboardEmptyTransactionsCard(
                            isBangla = isBangla,
                            tokens = tokens
                        )
                    } else {
                        DashboardTransactionsCard(
                            transactions = transactions,
                            language = language,
                            tokens = tokens
                        )
                    }
                }
            }
        }

        if (isFabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tokens.fabScrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isFabExpanded = false }
                    .testTag("dashboard_fab_scrim")
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = isFabExpanded,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom) + slideInVertically { it / 2 },
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom) + slideOutVertically { it / 2 }
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("dashboard_fab_menu")
                ) {
                    fabActions.forEach { action ->
                        DashboardFabActionPill(
                            action = action,
                            tokens = tokens,
                            onClick = {
                                if (isFabExpanded) {
                                    isFabExpanded = false
                                    action.onClick()
                                }
                            }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation = if (tokens.isDark) 12.dp else 8.dp,
                        shape = CircleShape,
                        ambientColor = tokens.accentPurple,
                        spotColor = tokens.accentPurple
                    )
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(tokens.fabBgStart, tokens.fabBgEnd)))
                    .border(1.2.dp, tokens.fabBorder, CircleShape)
                    .clickable { isFabExpanded = !isFabExpanded }
                    .testTag("dashboard_fab_main"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Quick Actions",
                    tint = Color.White,
                    modifier = Modifier
                        .size(26.dp)
                        .rotate(fabRotation)
                )
            }
        }
    }

    if (state.generatedPdf != null) {
        PdfViewerDialog(
            pdfFile = state.generatedPdf,
            reportTitle = state.generatedPdfTitle,
            isBangla = isBangla,
            onDismiss = onClearGeneratedPdf
        )
    }
}

@Composable
private fun DashboardFabActionPill(
    action: DashboardFabAction,
    tokens: DashboardColorTokens,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = tokens.fabPillBg,
        shadowElevation = if (tokens.isDark) 6.dp else 4.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = action.iconTint.copy(alpha = if (tokens.isDark) 0.45f else 0.24f)
        ),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag(action.testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(action.iconTint.copy(alpha = if (tokens.isDark) 0.24f else 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = if (tokens.isDark) Color.White else action.iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = tokens.fabPillText,
                maxLines = 1
            )
        }
    }
}
