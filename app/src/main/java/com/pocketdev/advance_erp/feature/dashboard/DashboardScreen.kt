package com.pocketdev.advance_erp.feature.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPCalendar
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPEmptyState
import com.pocketdev.advance_erp.core.designsystem.ERPLoadingState
import com.pocketdev.advance_erp.core.designsystem.ERPSectionHeader
import com.pocketdev.advance_erp.core.designsystem.ERPStatCard
import com.pocketdev.advance_erp.core.designsystem.ERPTransactionItem
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurpleContainer
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusInfo
import com.pocketdev.advance_erp.core.designsystem.StatusInfoContainer
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusSuccessContainer
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.StatusWarningContainer
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.pdf.PdfViewerDialog
import com.pocketdev.advance_erp.core.util.Formatters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DashboardFabPurple = Color(0xFF792B87)

private data class DashboardFabAction(
    val label: String,
    val icon: ImageVector,
    val iconTint: Color,
    val containerColor: Color,
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

    val summary = state.summary
    val language = state.language
    val isBangla = language == AppLanguage.BANGLA
    val dateMetrics = summary?.dateWiseMetrics
    val closingBalances = summary?.closingBalances

    var showCalendarPicker by rememberSaveable { mutableStateOf(false) }
    var isFabExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isFabExpanded) {
        isFabExpanded = false
    }

    val dateFormat = remember(isBangla) {
        SimpleDateFormat("dd MMM yyyy", if (isBangla) Locale("bn", "BD") else Locale.US)
    }
    val selectedDateText = dateFormat.format(Date(state.selectedDateMillis))

    val fabActions = listOf(
        DashboardFabAction(
            label = "PO",
            icon = Icons.Outlined.Inventory,
            iconTint = PrimaryPurple,
            containerColor = PrimaryPurpleContainer,
            testTag = "dashboard_fab_action_po",
            onClick = onNavigateToImports
        ),
        DashboardFabAction(
            label = "Sales",
            icon = Icons.Outlined.ShoppingCart,
            iconTint = StatusSuccess,
            containerColor = StatusSuccessContainer,
            testTag = "dashboard_fab_action_sales",
            onClick = onNavigateToSales
        ),
        DashboardFabAction(
            label = "Add Product",
            icon = Icons.Outlined.Inventory2,
            iconTint = StatusInfo,
            containerColor = StatusInfoContainer,
            testTag = "dashboard_fab_action_add_product",
            onClick = onNavigateToAddProduct
        ),
        DashboardFabAction(
            label = "Ledger",
            icon = Icons.Outlined.ReceiptLong,
            iconTint = StatusWarning,
            containerColor = StatusWarningContainer,
            testTag = "dashboard_fab_action_ledger",
            onClick = onNavigateToLedger
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ERPBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_scroll_list"),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Calendar Date Control Bar (ERPCalendar Navigation)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_date_selector"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onSelectPreviousDay,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Day", tint = PrimaryPurple)
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showCalendarPicker = !showCalendarPicker }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar", tint = TextPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBangla) "তারিখ: $selectedDateText" else "Selected: $selectedDateText",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                }

                                IconButton(
                                    onClick = onSelectNextDay,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Day", tint = PrimaryPurple)
                                }
                            }

                            OutlinedButton(
                                onClick = onSelectToday,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Outlined.Today, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isBangla) "আজ" else "Today", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (showCalendarPicker) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = DeepNavy.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(10.dp))
                            ERPCalendar(
                                language = language,
                                selectedTimestamp = state.selectedDateMillis,
                                onDateSelected = { millis ->
                                    onSelectDate(millis)
                                    showCalendarPicker = false
                                }
                            )
                        }
                    }
                }
            }

            // 2. Business Overview & Live DB Indicator Banner
            item {
                ERPCard(
                    modifier = Modifier.testTag("dashboard_business_overview"),
                    backgroundColor = DeepNavy,
                    contentPadding = 12.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.dashboard_overview),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(
                                        if (summary?.isLiveDatabaseData == true) R.string.dashboard_live_metrics else R.string.dashboard_saved_metrics
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(StatusSuccess.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(StatusSuccess, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(
                                            if (summary?.isLiveDatabaseData == true) R.string.dashboard_live else R.string.dashboard_saved
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusSuccess,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OverviewMetric(
                                label = stringResource(R.string.nav_products),
                                value = Formatters.formatNumber(summary?.productCount?.toLong() ?: 0L, language)
                            )
                            OverviewMetric(
                                label = stringResource(R.string.low_stock_warning),
                                value = Formatters.formatNumber(summary?.lowStockCount?.toLong() ?: 0L, language),
                                highlightColor = if ((summary?.lowStockCount ?: 0) > 0) StatusError else Color.White
                            )
                            OverviewMetric(
                                label = stringResource(R.string.customers),
                                value = Formatters.formatNumber(summary?.customerCount?.toLong() ?: 0L, language)
                            )
                            OverviewMetric(
                                label = stringResource(R.string.suppliers),
                                value = Formatters.formatNumber(summary?.supplierCount?.toLong() ?: 0L, language)
                            )
                        }
                    }
                }
            }

            // 3. Date-wise Business Metrics Grid & Inventory Summary
            item {
                Column(
                    modifier = Modifier.testTag("dashboard_daily_metrics"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ERPSectionHeader(
                        title = if (isBangla) "$selectedDateText এর ব্যবসায়িক তথ্য" else "Business Metrics for $selectedDateText",
                        subtitle = if (isBangla) "নির্বাচিত দিনের মোট ক্রয়, বিক্রয়, নগদ প্রবাহ ও লাভ" else "Sales, procurement, net cash flow & daily profit"
                    )

                    // Row 1: Sales & Purchases on selected date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ERPStatCard(
                            title = if (isBangla) "দিনের বিক্রয় আয়" else "Date Sales Revenue",
                            value = Formatters.formatMoney(dateMetrics?.totalSales?.minorUnits ?: 0L, state.currency, language),
                            icon = Icons.Outlined.PointOfSale,
                            accentColor = PrimaryPurple,
                            badgeText = if (isBangla) "বিক্রয়" else "Day Sales",
                            modifier = Modifier.weight(1f)
                        )
                        ERPStatCard(
                            title = if (isBangla) "দিনের ক্রয় মূল্য" else "Date Purchases (PO)",
                            value = Formatters.formatMoney(dateMetrics?.totalPurchases?.minorUnits ?: 0L, state.currency, language),
                            icon = Icons.Outlined.ShoppingCart,
                            accentColor = StatusInfo,
                            badgeText = if (isBangla) "ক্রয়" else "Procurement",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Customer Collections & Supplier Payments
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ERPStatCard(
                            title = if (isBangla) "গ্রাহক আদায় (কালেকশন)" else "Customer Collections",
                            value = Formatters.formatMoney(dateMetrics?.totalCollections?.minorUnits ?: 0L, state.currency, language),
                            icon = Icons.Outlined.ArrowDownward,
                            accentColor = StatusSuccess,
                            badgeText = if (isBangla) "নগদ প্রবাহ" else "Cash In",
                            modifier = Modifier.weight(1f)
                        )
                        ERPStatCard(
                            title = if (isBangla) "সরবরাহকারী পরিশোধ" else "Supplier Payments",
                            value = Formatters.formatMoney(dateMetrics?.totalPayments?.minorUnits ?: 0L, state.currency, language),
                            icon = Icons.Outlined.ArrowUpward,
                            accentColor = StatusError,
                            badgeText = if (isBangla) "নগদ বহিঃপ্রবাহ" else "Cash Out",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 3: Operating Expenses & Sales Returns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ERPStatCard(
                            title = if (isBangla) "দৈনিক খরচ" else "Daily Expenses",
                            value = Formatters.formatMoney(dateMetrics?.totalExpenses?.minorUnits ?: 0L, state.currency, language),
                            icon = Icons.Outlined.ReceiptLong,
                            accentColor = StatusWarning,
                            badgeText = if (isBangla) "পরিচালন ব্যয়" else "Operating",
                            modifier = Modifier.weight(1f)
                        )
                        ERPStatCard(
                            title = if (isBangla) "বিক্রয় ফেরত" else "Sales Returns",
                            value = Formatters.formatMoney(dateMetrics?.totalReturns?.minorUnits ?: 0L, state.currency, language),
                            icon = Icons.Outlined.SwapHoriz,
                            accentColor = PrimaryPurple,
                            badgeText = if (isBangla) "ফেরত" else "Returns",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 4: Net Cash Flow & Daily Gross Profit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val netCashMinor = dateMetrics?.netCashFlow?.minorUnits ?: 0L
                        ERPStatCard(
                            title = if (isBangla) "নিট নগদ প্রবাহ" else "Net Cash Flow",
                            value = Formatters.formatMoney(netCashMinor, state.currency, language),
                            icon = Icons.Outlined.ReceiptLong,
                            accentColor = if (netCashMinor >= 0) StatusSuccess else StatusError,
                            badgeText = if (isBangla) "আদায় − পরিশোধ" else "In - Out",
                            modifier = Modifier.weight(1f)
                        )
                        if (state.canViewProfit) {
                            val dailyProfitMinor = dateMetrics?.dailyGrossProfit?.minorUnits ?: 0L
                            ERPStatCard(
                                title = if (isBangla) "দৈনিক মোট লাভ" else "Daily Gross Profit",
                                value = Formatters.formatMoney(dailyProfitMinor, state.currency, language),
                                icon = Icons.Outlined.CheckCircle,
                                accentColor = if (dailyProfitMinor >= 0) SecondaryTeal else StatusError,
                                badgeText = if (isBangla) "বিক্রয় − বিক্রীত পণ্যের ব্যয়" else "Sales - COGS",
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            ERPStatCard(
                                title = if (isBangla) "দৈনিক লেনদেন সংখ্যা" else "Daily Transactions",
                                value = "${dateMetrics?.transactionCount ?: 0} events",
                                icon = Icons.Outlined.Assessment,
                                accentColor = StatusInfo,
                                badgeText = if (isBangla) "লেনদেন" else "Activity",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Row 5: Stock Movement & Activity Counts (Inventory Summary)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = if (isBangla) "স্টক ইন" else "Stock In", fontSize = 11.sp, color = TextSecondary)
                                Text(text = "+${dateMetrics?.stockInUnits ?: 0} units", fontWeight = FontWeight.Bold, color = StatusSuccess, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = if (isBangla) "স্টক আউট" else "Stock Out", fontSize = 11.sp, color = TextSecondary)
                                Text(text = "-${dateMetrics?.stockOutUnits ?: 0} units", fontWeight = FontWeight.Bold, color = StatusError, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = if (isBangla) "লেনদেন সংখ্যা" else "Transactions", fontSize = 11.sp, color = TextSecondary)
                                Text(text = "${dateMetrics?.transactionCount ?: 0} events", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 4. Centralized PDF Export Quick Buttons (Report Actions)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onGenerateDailySnapshotPdf,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBangla) "দৈনিক পিডিএফ চিত্র" else "Daily PDF Snapshot", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onGenerateMonthlyPerformancePdf,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBangla) "মাসিক রিপোর্ট" else "Monthly Review", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 5. Closing Balances as of Selected Date (Historical Cumulative)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ERPSectionHeader(
                        title = if (isBangla) "$selectedDateText পর্যন্ত সমাপনী স্থিতি" else "Closing Balances as of $selectedDateText",
                        subtitle = if (isBangla) "নগদ ও ব্যাংক, গ্রাহক পাওনা, সরবরাহকারী দেনা ও মজুদ মূল্যায়ন" else "Cumulative closing cash, receivables, payables & inventory"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ERPStatCard(
                            title = if (isBangla) "সমাপনী নগদ ও ব্যাংক" else "Closing Cash & Bank",
                            value = Formatters.formatMoney(closingBalances?.closingCashBank?.minorUnits ?: 0L, state.currency, language),
                            icon = Icons.Outlined.ReceiptLong,
                            accentColor = StatusInfo,
                            badgeText = if (isBangla) "তরল তহবিল" else "Liquid Funds",
                            modifier = Modifier.weight(1f)
                        )
                        ERPStatCard(
                            title = if (isBangla) "গ্রাহক বকেয়া (পাওনা)" else "Customer Receivables",
                            value = Formatters.formatMoney(closingBalances?.customerOutstanding?.minorUnits ?: 0L, state.currency, language),
                            icon = Icons.Outlined.ArrowDownward,
                            accentColor = StatusSuccess,
                            badgeText = if (isBangla) "আদায়যোগ্য" else "AR Due",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ERPStatCard(
                            title = if (isBangla) "সরবরাহকারী দেনা" else "Supplier Payables",
                            value = Formatters.formatMoney(closingBalances?.supplierOutstanding?.minorUnits ?: 0L, state.currency, language),
                            icon = Icons.Outlined.ArrowUpward,
                            accentColor = StatusError,
                            badgeText = if (isBangla) "পরিশোধযোগ্য" else "AP Due",
                            modifier = Modifier.weight(1f)
                        )
                        ERPStatCard(
                            title = if (isBangla) "মজুদ সম্পদ মূল্যায়ন" else "Inventory Valuation",
                            value = Formatters.formatMoney(closingBalances?.inventoryValuation?.minorUnits ?: 0L, state.currency, language),
                            icon = Icons.Outlined.Inventory2,
                            accentColor = SecondaryTeal,
                            badgeText = if (isBangla) "FIFO সম্পদ" else "FIFO Asset",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 6. Date-filtered Transaction List
            item {
                Column {
                    ERPSectionHeader(
                        title = if (isBangla) "$selectedDateText এর লেনদেন সমূহ" else "Transactions on $selectedDateText",
                        actionText = stringResource(R.string.view_all),
                        onActionClick = onNavigateToTransactions
                    )

                    val transactions = summary?.recentTransactions.orEmpty()
                    if (transactions.isEmpty()) {
                        ERPCard {
                            ERPEmptyState(
                                icon = Icons.Outlined.ReceiptLong,
                                title = if (isBangla) "এই তারিখে কোনো লেনদেন নেই" else "No transactions on this date",
                                message = if (isBangla) "নির্বাচিত তারিখে কোনো বিক্রয় বা স্টক স্থানান্তর রেকর্ড করা হয়নি।" else "No movement or sales recorded on selected date."
                            )
                        }
                    } else {
                        ERPCard {
                            Column {
                                transactions.forEachIndexed { index, tx ->
                                    ERPTransactionItem(
                                        title = tx.title,
                                        subtitle = "${tx.subtitle} • ${Formatters.formatDate(tx.timestamp, language)}",
                                        amountText = Formatters.formatMoney(tx.amountMinor, tx.currency, language),
                                        isPositive = tx.isCredit,
                                        icon = if (tx.isCredit) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                                        statusText = tx.statusText,
                                        statusColor = if (tx.isCredit) StatusSuccess else PrimaryPurple
                                    )
                                    if (index < transactions.lastIndex) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isFabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.14f))
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

            FloatingActionButton(
                onClick = { isFabExpanded = !isFabExpanded },
                shape = CircleShape,
                containerColor = DashboardFabPurple,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("dashboard_fab_main")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Quick Actions"
                )
            }
        }
    }

    // PDF Viewer / Action Dialog
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
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = action.containerColor,
        shadowElevation = 4.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, action.iconTint.copy(alpha = 0.18f)),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag(action.testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = action.iconTint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun OverviewMetric(
    label: String,
    value: String,
    highlightColor: Color = Color.White
) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = highlightColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
