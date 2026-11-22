package com.pocketdev.advance_erp.feature.controlcenter

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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.ERPEmptyState
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurpleContainer
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusErrorContainer
import com.pocketdev.advance_erp.core.designsystem.StatusInfo
import com.pocketdev.advance_erp.core.designsystem.StatusInfoContainer
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusSuccessContainer
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.StatusWarningContainer
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.pdf.PdfViewerDialog
import com.pocketdev.advance_erp.core.security.SecurityManager
import com.pocketdev.advance_erp.core.security.rememberBiometricCostVisibility
import com.pocketdev.advance_erp.core.util.CustomReportPdfConfig
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.domain.model.FinancialMetricFilter
import com.pocketdev.advance_erp.domain.model.ReportCategory
import com.pocketdev.advance_erp.domain.model.SortOption
import com.pocketdev.advance_erp.domain.model.UnifiedTransactionItem
import com.pocketdev.advance_erp.feature.controlcenter.components.ControlCenterFilterDrawer
import com.pocketdev.advance_erp.feature.controlcenter.components.ControlCenterSummaryCards
import com.pocketdev.advance_erp.feature.controlcenter.components.ControlCenterTransactionDetailDialog
import com.pocketdev.advance_erp.feature.controlcenter.components.LedgerExpenseEntryDialog
import com.pocketdev.advance_erp.feature.controlcenter.components.LedgerJournalEntryDialog
import com.pocketdev.advance_erp.feature.controlcenter.components.PartyLedgerPanel
import com.pocketdev.advance_erp.feature.controlcenter.components.PartyLedgerRow
import com.pocketdev.advance_erp.feature.imports.RecordSupplierPaymentDialog
import com.pocketdev.advance_erp.feature.inventory.BarcodeQrScannerDialog
import com.pocketdev.advance_erp.feature.sales.RecordCustomerPaymentDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CentralLedgerTab(val titleEn: String, val titleBn: String) {
    SUMMARY("Summary", "সারসংক্ষেপ"),
    TRANSACTIONS("Transactions", "লেনদেন"),
    OUTSTANDING_DUE("Outstanding Due", "বকেয়া"),
    STATEMENTS("Statements", "স্টেটমেন্ট")
}

private enum class DueTabFilter(val titleEn: String, val titleBn: String) {
    ALL("All Dues", "সকল বকেয়া"),
    RECEIVABLE("Receivable", "পাওনা"),
    PAYABLE("Payable", "দেনা")
}

private data class LedgerFabAction(
    val label: String,
    val icon: ImageVector,
    val iconTint: Color,
    val containerColor: Color,
    val testTag: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BusinessControlCenterScreen(
    modifier: Modifier = Modifier,
    viewModel: BusinessControlCenterViewModel = hiltViewModel(),
    initialCategory: ReportCategory? = null,
    initialTab: CentralLedgerTab = CentralLedgerTab.SUMMARY
) {
    val context = LocalContext.current
    val filterState by viewModel.filterState.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val detailLoading by viewModel.detailLoading.collectAsState()
    val report by viewModel.report.collectAsState()
    val ready = !report.isLoading && report.error == null && report.filter == filterState
    val transactions = if (ready) report.transactions else emptyList()
    val summary = report.summary

    val paymentInvoices by viewModel.paymentInvoices.collectAsState()
    val paymentOrders by viewModel.paymentOrders.collectAsState()
    val paymentSaving by viewModel.paymentSaving.collectAsState()
    val paymentError by viewModel.paymentError.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val liquidAccounts by viewModel.liquidAccounts.collectAsState()
    val savedPresets by viewModel.savedPresets.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val currencyCode = CurrencyCode.BDT
    val selectedDetail by viewModel.selectedTransactionDetail.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val nextCustomerPaymentNumber by viewModel.nextCustomerPaymentNumber.collectAsState()
    val nextSupplierPaymentNumber by viewModel.nextSupplierPaymentNumber.collectAsState()
    val previewPdfFile by viewModel.previewPdfFile.collectAsState()

    val isBn = language == AppLanguage.BANGLA
    val isOwnerAdmin = SecurityManager.canViewProfit(userRole)
    val profitVisibility = rememberBiometricCostVisibility("ledger-profit", isOwnerAdmin, isBn)
    val canViewProfit = isOwnerAdmin && profitVisibility.isVisible
    val stockVisibility = rememberBiometricCostVisibility("ledger-stock", SecurityManager.canViewCost(userRole), isBn)
    val exportVisibility = rememberBiometricCostVisibility("ledger-export", SecurityManager.canViewCost(userRole), isBn)
    val profitFilterLocked = filterState.financialMetric in setOf(FinancialMetricFilter.PROFIT, FinancialMetricFilter.LOSS) && !canViewProfit

    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    var visibleCount by remember(filterState) { mutableIntStateOf(100) }

    var showFilterDrawer by rememberSaveable { mutableStateOf(false) }
    var showExportSheet by rememberSaveable { mutableStateOf(false) }
    var showCustomReportDialog by rememberSaveable { mutableStateOf(false) }
    var showPresetsMenu by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by rememberSaveable { mutableStateOf(false) }
    var showSearchField by rememberSaveable { mutableStateOf(false) }
    var showScannerDialog by rememberSaveable { mutableStateOf(false) }
    var isFabExpanded by rememberSaveable { mutableStateOf(false) }

    // Collapsible summary states
    var isSummaryMetricsExpanded by rememberSaveable { mutableStateOf(false) }
    var isSummaryAutoCollapsed by remember { mutableStateOf(false) }

    // Workflow Dialogs
    var showMakePaymentDialog by rememberSaveable { mutableStateOf(false) }
    var preselectedSupplierForPayment by rememberSaveable { mutableStateOf<String?>(null) }
    var showReceivePaymentDialog by rememberSaveable { mutableStateOf(false) }
    var preselectedCustomerForPayment by rememberSaveable { mutableStateOf<String?>(null) }
    var showExpenseDialog by rememberSaveable { mutableStateOf(false) }
    var showJournalDialog by rememberSaveable { mutableStateOf(false) }
    var dueTabFilter by rememberSaveable { mutableStateOf(DueTabFilter.ALL) }

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val isScrolledDown by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 80 }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -18f) {
                    isSummaryMetricsExpanded = false
                    if (isScrolledDown) {
                        isSummaryAutoCollapsed = true
                    }
                } else if (available.y > 18f && !isScrolledDown) {
                    isSummaryAutoCollapsed = false
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(isScrolledDown) {
        if (!isScrolledDown) {
            isSummaryAutoCollapsed = false
        }
    }

    LaunchedEffect(initialCategory, initialTab) {
        if (initialCategory != null) {
            viewModel.selectCategory(initialCategory)
            if (initialCategory in setOf(ReportCategory.CUSTOMER_LEDGER, ReportCategory.SUPPLIER_LEDGER)) {
                selectedTab = CentralLedgerTab.STATEMENTS
            }
        } else {
            selectedTab = initialTab
        }
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    BackHandler(enabled = isFabExpanded) {
        isFabExpanded = false
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    val fabActions = buildList {
        if (SecurityManager.canExportReport(userRole)) {
            add(
                LedgerFabAction(
                    label = if (isBn) "রিপোর্ট / এক্সপোর্ট" else "Export Report",
                    icon = Icons.Outlined.PictureAsPdf,
                    iconTint = PrimaryPurple,
                    containerColor = PrimaryPurpleContainer,
                    testTag = "ledger_fab_export",
                    onClick = { if (ready && !profitFilterLocked) showExportSheet = true }
                )
            )
        }
        if (SecurityManager.canMakePayment(userRole)) {
            add(
                LedgerFabAction(
                    label = if (isBn) "পেমেন্ট করুন" else "Make Payment",
                    icon = Icons.Outlined.Payments,
                    iconTint = SecondaryTeal,
                    containerColor = StatusInfoContainer,
                    testTag = "ledger_fab_make_payment",
                    onClick = {
                        viewModel.clearPaymentError()
                        viewModel.refreshNextPaymentNumbers()
                        preselectedSupplierForPayment = filterState.selectedSupplierId
                        showMakePaymentDialog = true
                    }
                )
            )
        }
        if (SecurityManager.canReceivePayment(userRole)) {
            add(
                LedgerFabAction(
                    label = if (isBn) "টাকা গ্রহণ" else "Receive Payment",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    iconTint = StatusSuccess,
                    containerColor = StatusSuccessContainer,
                    testTag = "ledger_fab_receive_payment",
                    onClick = {
                        viewModel.clearPaymentError()
                        viewModel.refreshNextPaymentNumbers()
                        preselectedCustomerForPayment = filterState.selectedCustomerId
                        showReceivePaymentDialog = true
                    }
                )
            )
        }
        if (SecurityManager.canRecordExpense(userRole)) {
            add(
                LedgerFabAction(
                    label = if (isBn) "ব্যয় এন্ট্রি" else "Expense Entry",
                    icon = Icons.Outlined.MoneyOff,
                    iconTint = StatusWarning,
                    containerColor = StatusWarningContainer,
                    testTag = "ledger_fab_expense",
                    onClick = {
                        viewModel.clearPaymentError()
                        showExpenseDialog = true
                    }
                )
            )
        }
        if (SecurityManager.canPostJournal(userRole)) {
            add(
                LedgerFabAction(
                    label = if (isBn) "জার্নাল এন্ট্রি" else "Journal Entry",
                    icon = Icons.Outlined.EditNote,
                    iconTint = PrimaryPurple,
                    containerColor = MaterialTheme.colorScheme.surface,
                    testTag = "ledger_fab_journal",
                    onClick = {
                        viewModel.clearPaymentError()
                        showJournalDialog = true
                    }
                )
            )
        }
    }

    var swipeDragX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ERPBackground)
            .pointerInput(showFilterDrawer) {
                detectHorizontalDragGestures(
                    onDragStart = { swipeDragX = 0f },
                    onDragCancel = { swipeDragX = 0f },
                    onDragEnd = {
                        if (swipeDragX < -90f && !showFilterDrawer) {
                            showFilterDrawer = true
                        }
                        swipeDragX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeDragX += dragAmount
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Segmented 4-Tab Navigation Strip
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ERPBackground,
                        border = BorderStroke(1.dp, ERPDivider.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ledger_segmented_tabs")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            CentralLedgerTab.entries.forEach { tab ->
                                val isSel = selectedTab == tab
                                val tabWeight = if (isBn) 1f else when (tab) {
                                    CentralLedgerTab.SUMMARY -> 0.88f
                                    CentralLedgerTab.TRANSACTIONS -> 1.12f
                                    CentralLedgerTab.OUTSTANDING_DUE -> 1.38f
                                    CentralLedgerTab.STATEMENTS -> 1.02f
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(tabWeight)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(if (isSel) PrimaryPurple else Color.Transparent)
                                        .clickable {
                                            selectedTab = tab
                                            if (tab == CentralLedgerTab.STATEMENTS &&
                                                filterState.category !in setOf(ReportCategory.CUSTOMER_LEDGER, ReportCategory.SUPPLIER_LEDGER)
                                            ) {
                                                viewModel.selectCategory(ReportCategory.CUSTOMER_LEDGER)
                                            } else if (tab != CentralLedgerTab.STATEMENTS &&
                                                filterState.category in setOf(ReportCategory.CUSTOMER_LEDGER, ReportCategory.SUPPLIER_LEDGER) &&
                                                filterState.selectedCustomerId == null && filterState.selectedSupplierId == null
                                            ) {
                                                viewModel.selectCategory(ReportCategory.ALL_TRANSACTIONS)
                                            }
                                        }
                                        .padding(vertical = 8.dp, horizontal = if (isBn) 4.dp else 2.dp)
                                        .testTag("ledger_tab_${tab.name}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isBn) tab.titleBn else tab.titleEn,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = if (isBn) 11.5.sp else 10.5.sp
                                        ),
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // 2. Compact Action & Active-Filter Control Row (replaces large chip strips)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Date Range Selector Pill (opens Filter Drawer)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showFilterDrawer = true }
                                .testTag("ledger_date_pill"),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, ERPDivider)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBn) filterState.dateRangeOption.titleBn else filterState.dateRangeOption.titleEn,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Search toggle
                        Surface(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showSearchField = !showSearchField }
                                .testTag("ledger_search_toggle"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (showSearchField || filterState.searchQuery.isNotBlank()) PrimaryPurple.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (showSearchField || filterState.searchQuery.isNotBlank()) PrimaryPurple else ERPDivider)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Search",
                                    tint = if (showSearchField || filterState.searchQuery.isNotBlank()) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // QR / Barcode Scanner shortcut
                        Surface(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showScannerDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, ERPDivider)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.QrCodeScanner,
                                    contentDescription = "QR / Barcode Search",
                                    tint = SecondaryTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Filter Drawer Icon Button with Active Count Badge
                        BadgedBox(
                            badge = {
                                if (filterState.activeFilterCount > 0) {
                                    Badge(
                                        containerColor = StatusError,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = filterState.activeFilterCount.toString(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showFilterDrawer = true }
                                    .testTag("ledger_filter_button"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (filterState.activeFilterCount > 0) PrimaryPurple.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (filterState.activeFilterCount > 0) PrimaryPurple else ERPDivider
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterList,
                                        contentDescription = "Open Filter Drawer",
                                        tint = if (filterState.activeFilterCount > 0) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Presets Menu Button
                        Box {
                            Surface(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showPresetsMenu = true },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, ERPDivider)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = "Saved Presets",
                                        tint = if (savedPresets.isNotEmpty()) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showPresetsMenu,
                                onDismissRequest = { showPresetsMenu = false }
                            ) {
                                if (savedPresets.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(if (isBn) "কোনো সংরক্ষিত প্রিসেট নেই" else "No saved presets yet") },
                                        onClick = { showPresetsMenu = false }
                                    )
                                } else {
                                    savedPresets.forEach { preset ->
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Outlined.Bookmark, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(16.dp)) },
                                            text = {
                                                Column {
                                                    Text(preset.name, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        text = if (isBn) preset.filterState.category.titleBn else preset.filterState.category.titleEn,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.applyPreset(preset)
                                                showPresetsMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Active Filter Indicator Strip
                    if (filterState.activeFilterCount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ledger_active_filter_indicator"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryPurple.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { showFilterDrawer = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.FilterList, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val countText = Formatters.formatNumber(filterState.activeFilterCount.toLong(), language)
                                    val catText = if (isBn) filterState.category.titleBn else filterState.category.titleEn
                                    Text(
                                        text = if (isBn) "$countText টি ফিল্টার সক্রিয় • $catText" else "$countText active • $catText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryPurple,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            TextButton(
                                onClick = { viewModel.resetFilters() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = StatusError, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isBn) "রিসেট" else "Reset",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusError,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (isExporting || detailLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            actionError?.let { message ->
                Surface(
                    color = StatusErrorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(message, color = StatusError, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::dismissActionError) {
                            Text(if (isBn) "বন্ধ" else "Dismiss")
                        }
                    }
                }
            }

            // 3. Main Tab Content Area
            when (selectedTab) {
                CentralLedgerTab.SUMMARY,
                CentralLedgerTab.TRANSACTIONS -> {
                    val showSummaryCards = selectedTab == CentralLedgerTab.SUMMARY
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection)
                            .padding(horizontal = 12.dp)
                            .testTag("ledger_transaction_list"),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Collapsible Summary Cards & Part-by-Part Custom Report Studio (Summary tab)
                        if (showSummaryCards && ready && !profitFilterLocked) {
                            item(key = "custom_report_studio_banner") {
                                Surface(
                                    onClick = { showCustomReportDialog = true },
                                    shape = RoundedCornerShape(14.dp),
                                    color = PrimaryPurple.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth().testTag("open_custom_report_studio_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(PrimaryPurple),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Outlined.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isBn) "পার্ট-বাই-পার্ট কাস্টম রিপোর্ট ও PDF সেভ" else "Part-by-Part Custom Report Builder (PDF)",
                                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryPurple
                                            )
                                            Text(
                                                text = if (isBn) "বিক্রয়, ক্রয়, বকেয়া, স্টক ও লেনদেন আলাদা অংশ বাছাই করে PDF সেভ করুন" else "Pick specific business report modules & save custom PDF",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            item(key = "summary_cards") {
                                AnimatedVisibility(
                                    visible = !isSummaryAutoCollapsed,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    ControlCenterSummaryCards(
                                        summary = summary,
                                        canViewProfit = canViewProfit,
                                        isOwnerAdmin = isOwnerAdmin,
                                        isExpanded = isSummaryMetricsExpanded,
                                        onToggleExpand = { isSummaryMetricsExpanded = !isSummaryMetricsExpanded },
                                        onUnlockProfit = profitVisibility.toggle,
                                        language = language,
                                        currencyCode = currencyCode,
                                        onCategoryClick = { cat ->
                                            viewModel.selectCategory(cat)
                                            selectedTab = CentralLedgerTab.TRANSACTIONS
                                        },
                                        modifier = Modifier.testTag("ledger_summary_cards")
                                    )
                                }
                            }
                        }

                        // Sticky Search + Transactions Count & Sort Header
                        stickyHeader(key = "sticky_search_and_header") {
                            Surface(
                                color = ERPBackground,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (showSearchField || selectedTab == CentralLedgerTab.TRANSACTIONS || filterState.searchQuery.isNotBlank()) {
                                        OutlinedTextField(
                                            value = filterState.searchQuery,
                                            onValueChange = { viewModel.setSearchQuery(it) },
                                            placeholder = {
                                                Text(
                                                    text = if (isBn) "ইনভয়েস, পেমেন্ট নং, গ্রাহক বা রেফারেন্স খুঁজুন..." else "Search invoice, payment #, party, ref...",
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            trailingIcon = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (filterState.searchQuery.isNotBlank()) {
                                                        IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(28.dp)) {
                                                            Icon(Icons.Outlined.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                    IconButton(onClick = { showScannerDialog = true }, modifier = Modifier.size(30.dp)) {
                                                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Scan", tint = SecondaryTeal, modifier = Modifier.size(17.dp))
                                                    }
                                                }
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedBorderColor = ERPDivider
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("ledger_sticky_search")
                                        )
                                    }

                                    if (!profitFilterLocked) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val countFormatted = Formatters.formatNumber(transactions.size.toLong(), language)
                                            Text(
                                                text = if (isBn) "সকল লেনদেন ($countFormatted টি)" else "All Transactions ($countFormatted)",
                                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            Box {
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { showSortMenu = true }
                                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val shortSort = when (filterState.sortBy) {
                                                        SortOption.NEWEST_FIRST -> if (isBn) "সর্বশেষ" else "Newest"
                                                        SortOption.OLDEST_FIRST -> if (isBn) "পুরোনো" else "Oldest"
                                                        SortOption.HIGHEST_AMOUNT -> if (isBn) "সর্বোচ্চ" else "Highest"
                                                        SortOption.LOWEST_AMOUNT -> if (isBn) "সর্বনিম্ন" else "Lowest"
                                                        SortOption.TRANSACTION_TYPE -> if (isBn) "ধরন" else "Type"
                                                        SortOption.NAME_ASC -> "A–Z"
                                                        SortOption.NAME_DESC -> "Z–A"
                                                    }
                                                    Text(
                                                        text = "$shortSort ↓",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = PrimaryPurple
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = showSortMenu,
                                                    onDismissRequest = { showSortMenu = false }
                                                ) {
                                                    SortOption.entries.forEach { opt ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    text = if (isBn) opt.titleBn else opt.titleEn,
                                                                    fontWeight = if (filterState.sortBy == opt) FontWeight.Bold else FontWeight.Normal,
                                                                    color = if (filterState.sortBy == opt) PrimaryPurple else MaterialTheme.colorScheme.onSurface
                                                                )
                                                            },
                                                            onClick = {
                                                                viewModel.updateFilter(filterState.copy(sortBy = opt))
                                                                showSortMenu = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!ready) {
                            item(key = "loading_or_error") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (report.error != null) {
                                        Text(report.error ?: "Ledger error", color = StatusError)
                                        OutlinedButton(onClick = viewModel::retry) {
                                            Text(if (isBn) "আবার চেষ্টা" else "Retry")
                                        }
                                    } else {
                                        CircularProgressIndicator(color = PrimaryPurple)
                                        Text(if (isBn) "হিসাব লোড হচ্ছে…" else "Loading ledger…")
                                    }
                                }
                            }
                        } else if (profitFilterLocked) {
                            item(key = "profit_locked_notice") {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (isBn) "এই ফিল্টারের ফল দেখতে লাভের বায়োমেট্রিক যাচাই করুন।" else "Authenticate profit access to view this filter.",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (isOwnerAdmin) {
                                            Button(onClick = profitVisibility.toggle) {
                                                Text(if (isBn) "ফিঙ্গারপ্রিন্ট যাচাই করুন" else "Verify Biometric")
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (transactions.isEmpty()) {
                            item(key = "empty_transactions") {
                                ERPEmptyState(
                                    icon = Icons.Outlined.FilterList,
                                    title = if (isBn) "কোনো লেনদেন পাওয়া যায়নি" else "No Transactions Found",
                                    message = if (isBn) "নির্বাচিত তারিখ ও ফিল্টার অনুযায়ী কোনো লেনদেন নেই।" else "Try adjusting the date range or resetting filters.",
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                        } else {
                            items(transactions.take(visibleCount), key = { it.rawType + ":" + it.id }) { item ->
                                if (item.rawType in setOf("STOCK_MOVEMENT", "STOCK_TRANSFER") && !stockVisibility.isVisible) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.primaryEntityName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                                Text(item.referenceNumber, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            TextButton(
                                                onClick = stockVisibility.toggle,
                                                enabled = SecurityManager.canViewCost(userRole)
                                            ) {
                                                Text(if (isBn) "খরচ দেখতে যাচাই করুন" else "Verify to view cost", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                } else {
                                    UnifiedTransactionCard(
                                        item = item,
                                        language = language,
                                        currencyCode = currencyCode,
                                        dateFormat = dateFormat,
                                        onClick = { viewModel.openTransactionDetail(item.rawType, item.sourceId) }
                                    )
                                }
                            }

                            if (transactions.size > visibleCount) {
                                item(key = "load_more") {
                                    OutlinedButton(
                                        onClick = { visibleCount += 100 },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = if (isBn) "আরও দেখুন ($visibleCount/${transactions.size})" else "Load more ($visibleCount/${transactions.size})"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                CentralLedgerTab.OUTSTANDING_DUE -> {
                    val dueCustomers = remember(customers, filterState.searchQuery) {
                        val q = filterState.searchQuery.trim()
                        customers.filter {
                            it.outstandingBalanceMinor > 0L && (q.isEmpty() || it.name.contains(q, true) || it.customerCode.contains(q, true) || it.phone.contains(q, true))
                        }
                    }
                    val dueSuppliers = remember(suppliers, filterState.searchQuery) {
                        val q = filterState.searchQuery.trim()
                        suppliers.filter {
                            it.outstandingPayableMinor > 0L && (q.isEmpty() || it.companyName.contains(q, true) || it.supplierCode.contains(q, true) || it.phone.contains(q, true))
                        }
                    }
                    val totalCustomerDue = dueCustomers.sumOf { it.outstandingBalanceMinor }
                    val totalSupplierDue = dueSuppliers.sumOf { it.outstandingPayableMinor }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                            .testTag("ledger_due_list"),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (isBn) "গ্রাহক বকেয়া (পাওনা)" else "Receivable Due",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = Formatters.formatMoney(totalCustomerDue, currencyCode, language),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusSuccess,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (isBn) "সাপ্লায়ার বকেয়া (দেনা)" else "Payable Due",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = Formatters.formatMoney(totalSupplierDue, currencyCode, language),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusError,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DueTabFilter.entries.forEach { opt ->
                                    val isSel = dueTabFilter == opt
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { dueTabFilter = opt },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) PrimaryPurple else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (isSel) PrimaryPurple else ERPDivider)
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (isBn) opt.titleBn else opt.titleEn,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (report.aging.isNotEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (isBn) "মেয়াদভিত্তিক খোলা বিলের বকেয়া (Aging)" else "Open-Document Due Aging",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val order = listOf("NOT_DUE", "1_30", "31_60", "61_90", "90_PLUS", "NO_DUE_DATE")
                                        report.aging.sortedBy { order.indexOf(it.key) }.forEach { bucket ->
                                            val label = when (bucket.key) {
                                                "NOT_DUE" -> if (isBn) "মেয়াদ হয়নি" else "Not due"
                                                "NO_DUE_DATE" -> if (isBn) "নির্দিষ্ট তারিখ নেই" else "No due date"
                                                "90_PLUS" -> if (isBn) "৯০+ দিন" else "Over 90 days"
                                                else -> bucket.key.replace('_', '–') + (if (isBn) " দিন" else " days")
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("$label (${bucket.documents})", style = MaterialTheme.typography.bodySmall)
                                                Text(
                                                    Formatters.formatMoney(bucket.amountMinor, currencyCode, language),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (dueTabFilter != DueTabFilter.PAYABLE) {
                            if (dueCustomers.isNotEmpty()) {
                                item {
                                    Text(
                                        text = if (isBn) "বকেয়া গ্রাহক তালিকা (${dueCustomers.size})" else "Customers with Due (${dueCustomers.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                items(dueCustomers, key = { "due-c-${it.id}" }) { cust ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectParty(cust.id, supplier = false)
                                                selectedTab = CentralLedgerTab.STATEMENTS
                                            },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(cust.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("${cust.customerCode} • ${cust.phone}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = Formatters.formatMoney(cust.outstandingBalanceMinor, currencyCode, language),
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusSuccess,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                if (SecurityManager.canReceivePayment(userRole)) {
                                                    TextButton(
                                                        onClick = {
                                                            viewModel.clearPaymentError()
                                                            viewModel.refreshNextPaymentNumbers()
                                                            preselectedCustomerForPayment = cust.id
                                                            showReceivePaymentDialog = true
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                        modifier = Modifier.height(26.dp)
                                                    ) {
                                                        Text(if (isBn) "আদায় করুন" else "Collect", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (dueTabFilter != DueTabFilter.RECEIVABLE) {
                            if (dueSuppliers.isNotEmpty()) {
                                item {
                                    Text(
                                        text = if (isBn) "বকেয়া সাপ্লায়ার তালিকা (${dueSuppliers.size})" else "Suppliers Payable (${dueSuppliers.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                items(dueSuppliers, key = { "due-s-${it.id}" }) { sup ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectParty(sup.id, supplier = true)
                                                selectedTab = CentralLedgerTab.STATEMENTS
                                            },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(sup.companyName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("${sup.supplierCode} • ${sup.phone}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = Formatters.formatMoney(sup.outstandingPayableMinor, currencyCode, language),
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusError,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                if (SecurityManager.canMakePayment(userRole)) {
                                                    TextButton(
                                                        onClick = {
                                                            viewModel.clearPaymentError()
                                                            viewModel.refreshNextPaymentNumbers()
                                                            preselectedSupplierForPayment = sup.id
                                                            showMakePaymentDialog = true
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                        modifier = Modifier.height(26.dp)
                                                    ) {
                                                        Text(if (isBn) "পরিশোধ" else "Pay", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (dueCustomers.isEmpty() && dueSuppliers.isEmpty()) {
                            item {
                                ERPEmptyState(
                                    icon = Icons.Outlined.AccountBalanceWallet,
                                    title = if (isBn) "কোনো বকেয়া পাওয়া যায়নি" else "No Outstanding Dues",
                                    message = if (isBn) "সব গ্রাহক ও সরবরাহকারীর হিসাব পরিশোধিত রয়েছে।" else "All customer and supplier balances are settled."
                                )
                            }
                        }
                    }
                }

                CentralLedgerTab.STATEMENTS -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                            .testTag("ledger_statements_list"),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            PartyLedgerPanel(
                                report = report,
                                filter = filterState,
                                customers = customers,
                                suppliers = suppliers,
                                language = language,
                                ready = ready && !profitFilterLocked,
                                onParty = viewModel::selectParty,
                                onPayment = {
                                    viewModel.clearPaymentError()
                                    viewModel.refreshNextPaymentNumbers()
                                    val stmt = report.statement
                                    if (stmt?.partyType == "SUPPLIER") {
                                        preselectedSupplierForPayment = stmt.partyId
                                        showMakePaymentDialog = true
                                    } else {
                                        preselectedCustomerForPayment = stmt?.partyId
                                        showReceivePaymentDialog = true
                                    }
                                },
                                onShare = { viewModel.shareReminder(context, isOwnerAdmin && profitVisibility.isVisible) }
                            )
                        }

                        if (report.statement != null && ready && !profitFilterLocked) {
                            items(transactions.take(visibleCount), key = { "stmt-" + it.rawType + ":" + it.id }) { item ->
                                PartyLedgerRow(item, language) {
                                    viewModel.openTransactionDetail(item.rawType, item.sourceId)
                                }
                            }
                            if (transactions.size > visibleCount) {
                                item {
                                    OutlinedButton(
                                        onClick = { visibleCount += 100 },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (isBn) "আরও দেখুন ($visibleCount/${transactions.size})" else "Load more ($visibleCount/${transactions.size})")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Scrim overlay when FAB menu is expanded
        if (isFabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isFabExpanded = false }
                    .testTag("ledger_fab_scrim")
            )
        }

        // Snackbar Host above FAB
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 84.dp)
        )

        // Expandable Purple Circular FAB at Bottom-Right (above Bottom Navigation)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedVisibility(
                visible = isFabExpanded,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom) + slideInVertically { it / 2 },
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom) + slideOutVertically { it / 2 }
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("ledger_fab_menu")
                ) {
                    fabActions.forEach { action ->
                        LedgerFabActionPill(
                            action = action,
                            onClick = {
                                isFabExpanded = false
                                action.onClick()
                            }
                        )
                    }
                }
            }

            val rotation by animateFloatAsState(
                targetValue = if (isFabExpanded) 45f else 0f,
                label = "fab_rotation"
            )
            FloatingActionButton(
                onClick = { isFabExpanded = !isFabExpanded },
                shape = CircleShape,
                containerColor = PrimaryPurple,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("ledger_fab_main")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = if (isFabExpanded) "Close Quick Actions" else "Open Quick Actions",
                    modifier = Modifier
                        .size(26.dp)
                        .rotate(rotation)
                )
            }
        }
    }

    // 1. Right-to-Left Swipe Filter Drawer
    if (showFilterDrawer) {
        ControlCenterFilterDrawer(
            currentFilter = filterState,
            canViewProfit = canViewProfit,
            customers = customers,
            suppliers = suppliers,
            warehouses = warehouses,
            categories = categories,
            products = products,
            language = language,
            onApply = { viewModel.updateFilter(it) },
            onReset = { viewModel.resetFilters() },
            onSavePreset = { viewModel.savePreset(it) },
            onDismiss = { showFilterDrawer = false }
        )
    }

    // 2. Make Payment (Supplier Payment / Other Outgoing Payment)
    if (showMakePaymentDialog) {
        RecordSupplierPaymentDialog(
            suppliers = suppliers,
            orders = paymentOrders.filter { it.status !in setOf("DRAFT", "CANCELLED") },
            preselectedSupplierId = preselectedSupplierForPayment,
            isSaving = paymentSaving,
            dismissOnSubmit = false,
            saveError = paymentError,
            liquidAccounts = liquidAccounts,
            nextPaymentNumber = nextSupplierPaymentNumber,
            isBangla = isBn,
            onSwitchToOtherPayment = if (SecurityManager.canRecordExpense(userRole)) {
                {
                    showMakePaymentDialog = false
                    viewModel.clearPaymentError()
                    showExpenseDialog = true
                }
            } else null,
            onConfirmWithDetails = { supplierId, orderId, amount, currency, rate, method, accountName, accountId, paymentDate, reference, notes ->
                viewModel.paySupplier(
                    supplierId = supplierId,
                    orderId = orderId,
                    amount = amount,
                    currency = currency,
                    rate = rate,
                    method = method,
                    account = accountName,
                    reference = reference,
                    notes = notes,
                    paymentDate = paymentDate,
                    paymentAccountId = accountId
                ) {
                    showMakePaymentDialog = false
                }
            },
            onDismiss = { if (!paymentSaving) showMakePaymentDialog = false },
            onConfirm = { supplierId, orderId, amount, currency, rate, method, account, reference, notes ->
                viewModel.paySupplier(supplierId, orderId, amount, currency, rate, method, account, reference, notes) {
                    showMakePaymentDialog = false
                }
            }
        )
    }

    // 3. Receive Payment (Customer Collection)
    if (showReceivePaymentDialog) {
        RecordCustomerPaymentDialog(
            customers = customers,
            invoices = paymentInvoices.filter { it.isPosted && it.status != "CANCELLED" },
            liquidAccounts = liquidAccounts,
            preselectedCustomerId = preselectedCustomerForPayment,
            nextPaymentNumber = nextCustomerPaymentNumber,
            isBangla = isBn,
            isSaving = paymentSaving,
            saveError = paymentError,
            onDismiss = { if (!paymentSaving) showReceivePaymentDialog = false },
            onSubmit = { draft ->
                viewModel.collectPayment(draft) { showReceivePaymentDialog = false }
            }
        )
    }

    // 4. Expense Entry Dialog
    if (showExpenseDialog) {
        LedgerExpenseEntryDialog(
            accounts = accounts,
            isBangla = isBn,
            isSaving = paymentSaving,
            saveError = paymentError,
            onDismiss = { if (!paymentSaving) showExpenseDialog = false },
            onConfirm = { cat, descEn, descBn, amtMinor, curr, rate, payAccId, expAccId, date, ref, notes ->
                viewModel.recordExpense(cat, descEn, descBn, amtMinor, curr, rate, payAccId, expAccId, date, ref, notes) {
                    showExpenseDialog = false
                }
            }
        )
    }

    // 5. Journal Entry Dialog
    if (showJournalDialog) {
        LedgerJournalEntryDialog(
            accounts = accounts,
            isBangla = isBn,
            isSaving = paymentSaving,
            saveError = paymentError,
            onDismiss = { if (!paymentSaving) showJournalDialog = false },
            onConfirm = { entryDate, descEn, descBn, lines ->
                viewModel.postManualJournalEntry(entryDate, descEn, descBn, lines) {
                    showJournalDialog = false
                }
            }
        )
    }

    // 6. QR / Barcode Search Dialog
    if (showScannerDialog) {
        BarcodeQrScannerDialog(
            language = language,
            onDismiss = { showScannerDialog = false },
            onCodeScanned = { code ->
                showScannerDialog = false
                showSearchField = true
                viewModel.setSearchQuery(code)
            }
        )
    }

    // 7. Transaction Detail Dialog
    selectedDetail?.let { detail ->
        ControlCenterTransactionDetailDialog(
            record = detail,
            language = language,
            currencyCode = currencyCode,
            onLinkedDocument = { type, id -> viewModel.openTransactionDetail(type, id) },
            onDismiss = { viewModel.dismissTransactionDetail() }
        )
    }

    // 8. PDF Preview Dialog
    previewPdfFile?.let { (file, title) ->
        PdfViewerDialog(
            pdfFile = file,
            reportTitle = title,
            isBangla = isBn,
            onDismiss = { viewModel.clearPreviewPdf() }
        )
    }

    // 9. Export Options Modal Bottom Sheet
    if (showExportSheet) {
        val exportSensitive = if (isOwnerAdmin) profitVisibility.isVisible else exportVisibility.isVisible
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (isBn) "রিপোর্ট প্রিভিউ, এক্সপোর্ট ও শেয়ার" else "Preview, Export & Share Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isBn) "নির্বাচিত ফিল্টার ও তারিখ অনুযায়ী PDF ও XLSX তৈরি করুন" else "Generate formatted PDF & XLSX for active filters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                if (isOwnerAdmin) {
                    TextButton(onClick = profitVisibility.toggle) {
                        Text(
                            text = if (isBn) {
                                if (profitVisibility.isVisible) "লাভ ও খরচের তথ্য এক্সপোর্ট থেকে লুকান" else "লাভসহ এক্সপোর্ট করতে ফিঙ্গারপ্রিন্ট যাচাই"
                            } else {
                                if (profitVisibility.isVisible) "Exclude profit/cost from export" else "Authenticate to include profit in export"
                            }
                        )
                    }
                } else if (SecurityManager.canViewCost(userRole)) {
                    TextButton(onClick = exportVisibility.toggle) {
                        Text(
                            text = if (isBn) {
                                if (exportVisibility.isVisible) "খরচ এক্সপোর্ট বন্ধ করুন" else "খরচসহ এক্সপোর্ট: ফিঙ্গারপ্রিন্ট যাচাই"
                            } else {
                                if (exportVisibility.isVisible) "Exclude cost" else "Authenticate to include cost"
                            }
                        )
                    }
                }

                ExportActionItem(
                    title = if (isBn) "কাস্টম পার্ট-বাই-পার্ট রিপোর্ট তৈরি করুন" else "Build Custom Part-by-Part PDF Report",
                    subtitle = if (isBn) "প্রয়োজনীয় সেকশন বাছাই করে কাস্টম PDF প্রিভিউ ও সেভ" else "Select specific report modules to preview, save & share",
                    icon = Icons.Outlined.Tune,
                    iconColor = SecondaryTeal,
                    onClick = {
                        showExportSheet = false
                        showCustomReportDialog = true
                    }
                )

                ExportActionItem(
                    title = if (isBn) "পিডিএফ প্রিভিউ দেখুন (Preview PDF)" else "Preview PDF Report",
                    subtitle = if (isBn) "প্রিভিউ দেখে শেয়ার বা সংরক্ষণ করুন" else "Open preview before saving or sharing",
                    icon = Icons.Outlined.Visibility,
                    iconColor = PrimaryPurple,
                    onClick = {
                        showExportSheet = false
                        viewModel.previewReportPdf(context, exportSensitive)
                    }
                )

                ExportActionItem(
                    title = if (isBn) "পিডিএফ রিপোর্ট শেয়ার করুন (PDF)" else "Share PDF Document",
                    subtitle = if (isBn) "WhatsApp, Gmail বা অন্য অ্যাপে শেয়ার" else "A4 printable report with header & summary",
                    icon = Icons.Outlined.Share,
                    iconColor = StatusError,
                    onClick = {
                        showExportSheet = false
                        viewModel.exportAndSharePdf(context, exportSensitive)
                    }
                )

                ExportActionItem(
                    title = if (isBn) "এক্সেল ওয়ার্কবুক শেয়ার করুন (XLSX)" else "Share Excel (XLSX) Workbook",
                    subtitle = if (isBn) "সারসংক্ষেপ ও লেজারসহ আলাদা শিট" else "Multi-sheet spreadsheet with summary & ledgers",
                    icon = Icons.Outlined.TableChart,
                    iconColor = StatusSuccess,
                    onClick = {
                        showExportSheet = false
                        viewModel.exportAndShareExcel(context, exportSensitive)
                    }
                )

                ExportActionItem(
                    title = if (isBn) "সিএসভি ফাইল শেয়ার করুন (CSV)" else "Share CSV Data File",
                    subtitle = if (isBn) "সহজ ডেটা বিশ্লেষণের জন্য" else "Plain comma-separated transaction records",
                    icon = Icons.Outlined.Share,
                    iconColor = StatusInfo,
                    onClick = {
                        showExportSheet = false
                        viewModel.exportAndShareCsv(context, exportSensitive)
                    }
                )

                ExportActionItem(
                    title = if (isBn) "সরাসরি প্রিন্ট করুন (Print)" else "Print Report Directly",
                    subtitle = if (isBn) "Android PrintManager ব্যবহার করে প্রিন্ট" else "Send document to printer",
                    icon = Icons.Outlined.Print,
                    iconColor = PrimaryPurple,
                    onClick = {
                        showExportSheet = false
                        viewModel.printReport(context, exportSensitive)
                    }
                )

                ExportActionItem(
                    title = if (isBn) "পিডিএফ ডাউনলোড / সংরক্ষণ করুন (Save PDF)" else "Save PDF Report to Downloads",
                    subtitle = if (isBn) "সরাসরি ফোনের Downloads ফোল্ডারে PDF সেভ করুন" else "Save A4 PDF report directly to Downloads",
                    icon = Icons.Outlined.PictureAsPdf,
                    iconColor = StatusError,
                    onClick = {
                        showExportSheet = false
                        viewModel.savePdfToStorage(context, exportSensitive)
                    }
                )

                ExportActionItem(
                    title = if (isBn) "পিডিএফ ও এক্সেল একসাথে সেভ (Save PDF + XLSX)" else "Save Both PDF & XLSX to Downloads",
                    subtitle = if (isBn) "ফোনের Downloads ফোল্ডারে দুটি ফাইলই সেভ করুন" else "Save PDF & XLSX to device Downloads",
                    icon = Icons.Outlined.Download,
                    iconColor = StatusWarning,
                    onClick = {
                        showExportSheet = false
                        viewModel.savePdfToStorage(context, exportSensitive)
                        viewModel.saveExcelToStorage(context, exportSensitive)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showCustomReportDialog) {
        val exportSensitive = if (isOwnerAdmin) profitVisibility.isVisible else exportVisibility.isVisible
        CustomReportPdfDialog(
            isBangla = isBn,
            canViewProfit = canViewProfit,
            onDismiss = { showCustomReportDialog = false },
            onPreview = { config ->
                showCustomReportDialog = false
                viewModel.previewCustomReportPdf(context, config, exportSensitive)
            },
            onSavePdf = { config ->
                showCustomReportDialog = false
                viewModel.saveCustomReportPdf(context, config, exportSensitive)
            },
            onSharePdf = { config ->
                showCustomReportDialog = false
                viewModel.shareCustomReportPdf(context, config, exportSensitive)
            }
        )
    }
}

private data class ReportModuleToggle(
    val label: String,
    val checked: Boolean,
    val onChanged: (Boolean) -> Unit
)

@Composable
private fun CustomReportPdfDialog(
    isBangla: Boolean,
    canViewProfit: Boolean,
    onDismiss: () -> Unit,
    onPreview: (CustomReportPdfConfig) -> Unit,
    onSavePdf: (CustomReportPdfConfig) -> Unit,
    onSharePdf: (CustomReportPdfConfig) -> Unit
) {
    var reportTitle by rememberSaveable {
        mutableStateOf(if (isBangla) "কাস্টমায়িজড বিজনেস রিপোর্ট" else "Custom Modular Business Report")
    }
    var incKpis by rememberSaveable { mutableStateOf(true) }
    var incSalesPurchase by rememberSaveable { mutableStateOf(true) }
    var incCustomerDues by rememberSaveable { mutableStateOf(true) }
    var incSupplierDues by rememberSaveable { mutableStateOf(true) }
    var incExpenseProfit by rememberSaveable { mutableStateOf(true) }
    var incInventory by rememberSaveable { mutableStateOf(true) }
    var incTransactions by rememberSaveable { mutableStateOf(true) }
    var incSignature by rememberSaveable { mutableStateOf(true) }

    fun buildConfig() = CustomReportPdfConfig(
        title = reportTitle,
        includeExecutiveKpis = incKpis,
        includeSalesPurchaseSummary = incSalesPurchase,
        includeCustomerDues = incCustomerDues,
        includeSupplierDues = incSupplierDues,
        includeExpensesAndProfit = incExpenseProfit,
        includeInventorySummary = incInventory,
        includeTransactionsTable = incTransactions,
        includeAuthorizedSignature = incSignature
    )

    val anySelected = incKpis || incSalesPurchase || incCustomerDues || incSupplierDues || incExpenseProfit || incInventory || incTransactions

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 720.dp)
                .clip(RoundedCornerShape(18.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "কাস্টম পার্ট-বাই-পার্ট রিপোর্ট" else "Custom Part-by-Part Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBangla) "রিপোর্টের প্রয়োজনীয় অংশগুলো বাছাই করে PDF তৈরি ও সংরক্ষণ করুন" else "Select report modules to include and export as PDF",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = reportTitle,
                    onValueChange = { reportTitle = it },
                    label = { Text(if (isBangla) "রিপোর্টের শিরোনাম" else "Report Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                val toggles = listOf(
                    ReportModuleToggle(
                        label = if (isBangla) "১. মূল ব্যবসায়িক সূচক (KPI Summary)" else "1. Executive KPI Summary",
                        checked = incKpis,
                        onChanged = { v -> incKpis = v }
                    ),
                    ReportModuleToggle(
                        label = if (isBangla) "২. বিক্রয়, ক্রয় ও আমদানি সারসংক্ষেপ" else "2. Sales, Purchase & Import Overview",
                        checked = incSalesPurchase,
                        onChanged = { v -> incSalesPurchase = v }
                    ),
                    ReportModuleToggle(
                        label = if (isBangla) "৩. কাস্টমার বকেয়া তালিকা (কেন্দ্রীয় একক ব্যালেন্স)" else "3. Customer Due Ledger (Centralized Balances)",
                        checked = incCustomerDues,
                        onChanged = { v -> incCustomerDues = v }
                    ),
                    ReportModuleToggle(
                        label = if (isBangla) "৪. সাপ্লায়ার দেনা তালিকা" else "4. Supplier Payable Ledger",
                        checked = incSupplierDues,
                        onChanged = { v -> incSupplierDues = v }
                    ),
                    ReportModuleToggle(
                        label = if (isBangla) {
                            if (canViewProfit) "৫. ব্যয়, নগদ জের ও লাভ/ক্ষতি" else "৫. ব্যয় ও নগদ জের (লাভ অ্যাডমিন সীমাবদ্ধ)"
                        } else {
                            if (canViewProfit) "5. Expenses, Cash Position & Profit" else "5. Expenses & Cash Position"
                        },
                        checked = incExpenseProfit,
                        onChanged = { v -> incExpenseProfit = v }
                    ),
                    ReportModuleToggle(
                        label = if (isBangla) "৬. পণ্য ও ইনভেন্টরি তালিকা" else "6. Inventory & Product Catalog Summary",
                        checked = incInventory,
                        onChanged = { v -> incInventory = v }
                    ),
                    ReportModuleToggle(
                        label = if (isBangla) "৭. বিস্তারিত লেনদেনের তালিকা (Transactions Table)" else "7. Detailed Transaction Register",
                        checked = incTransactions,
                        onChanged = { v -> incTransactions = v }
                    ),
                    ReportModuleToggle(
                        label = if (isBangla) "৮. অথরাইজড সিগনেচার ব্লক (সেটিংস থেকে)" else "8. Authorized Signature & Seal Block",
                        checked = incSignature,
                        onChanged = { v -> incSignature = v }
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    toggles.forEach { (label, checked, onChanged) ->
                        Surface(
                            onClick = { onChanged(!checked) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (checked) PrimaryPurple.copy(alpha = 0.08f) else ERPBackground,
                            border = BorderStroke(1.dp, if (checked) PrimaryPurple.copy(alpha = 0.35f) else ERPDivider),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = checked, onCheckedChange = onChanged)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        enabled = anySelected,
                        onClick = { onPreview(buildConfig()) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBangla) "প্রিভিউ" else "Preview", fontSize = 11.5.sp, maxLines = 1)
                    }
                    Button(
                        enabled = anySelected,
                        onClick = { onSavePdf(buildConfig()) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBangla) "সেভ PDF" else "Save PDF", fontSize = 11.5.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        enabled = anySelected,
                        onClick = { onSharePdf(buildConfig()) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBangla) "শেয়ার" else "Share", fontSize = 11.5.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerFabActionPill(
    action: LedgerFabAction,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = action.containerColor,
        shadowElevation = 4.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, action.iconTint.copy(alpha = 0.22f)),
        modifier = Modifier
            .heightIn(min = 44.dp)
            .testTag(action.testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(action.iconTint.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = action.iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun UnifiedTransactionCard(
    item: UnifiedTransactionItem,
    language: AppLanguage,
    currencyCode: CurrencyCode,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val (typeIcon, iconTint) = when (item.category) {
        ReportCategory.SALES -> Icons.Outlined.ShoppingBag to PrimaryPurple
        ReportCategory.DELIVERY -> Icons.Outlined.LocalShipping to SecondaryTeal
        ReportCategory.PURCHASE -> Icons.Outlined.Inventory2 to PrimaryPurple
        ReportCategory.IMPORT -> Icons.Outlined.LocalShipping to PrimaryPurple
        ReportCategory.PAYMENT, ReportCategory.RECEIPT -> Icons.Outlined.AccountBalanceWallet to SecondaryTeal
        ReportCategory.EXPENSE -> Icons.Outlined.MoneyOff to StatusWarning
        ReportCategory.SALES_RETURN, ReportCategory.PURCHASE_RETURN -> Icons.Outlined.Replay to StatusError
        ReportCategory.STOCK_TRANSFER, ReportCategory.STOCK_ADJUSTMENT, ReportCategory.INVENTORY -> Icons.Outlined.SwapHoriz to SecondaryTeal
        else -> Icons.Outlined.ReceiptLong to PrimaryPurple
    }
    val categoryLabel = if (isBn) item.category.titleBn else item.category.titleEn
    val amountSign = if (item.effectSign < 0) "↶ " else if (item.isInflow) "+ " else "- "
    val amountColor = when {
        item.effectSign < 0 || !item.accountingPosted -> MaterialTheme.colorScheme.onSurfaceVariant
        item.isInflow -> StatusSuccess
        else -> StatusError
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.6.dp, ERPDivider.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Category Icon Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconTint.copy(alpha = 0.1f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Middle: Party Name + Type/Ref/Status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.primaryEntityName,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "$categoryLabel · ${item.referenceNumber}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.status in setOf("UNPAID", "PARTIALLY_PAID", "CANCELLED", "VOIDED", "REVERSED", "DRAFT")) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(item.statusColorHex.toInt()).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = item.status,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(item.statusColorHex.toInt()),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Signed Amount + Compact Date/Time
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountSign${Formatters.formatMoney(item.amountMinor, currencyCode, language)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatCompactTransactionDateTime(item.date, language, dateFormat),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatCompactTransactionDateTime(
    timestampMs: Long,
    language: AppLanguage,
    dateFormat: SimpleDateFormat
): String {
    if (timestampMs <= 0L) return if (language == AppLanguage.BANGLA) "প্রারম্ভিক" else "Opening"
    val calNow = Calendar.getInstance()
    val calItem = Calendar.getInstance().apply { timeInMillis = timestampMs }
    val sameDay = calNow.get(Calendar.YEAR) == calItem.get(Calendar.YEAR) &&
        calNow.get(Calendar.DAY_OF_YEAR) == calItem.get(Calendar.DAY_OF_YEAR)
    val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = calYesterday.get(Calendar.YEAR) == calItem.get(Calendar.YEAR) &&
        calYesterday.get(Calendar.DAY_OF_YEAR) == calItem.get(Calendar.DAY_OF_YEAR)

    return when {
        sameDay -> {
            val prefix = if (language == AppLanguage.BANGLA) "আজ, " else "Today, "
            prefix + Formatters.formatTime(timestampMs, language)
        }
        isYesterday -> {
            val prefix = if (language == AppLanguage.BANGLA) "গতকাল, " else "Yesterday, "
            prefix + Formatters.formatTime(timestampMs, language)
        }
        else -> {
            val formattedDate = dateFormat.format(Date(timestampMs))
            if (language == AppLanguage.BANGLA) Formatters.toBanglaDigits(formattedDate) else formattedDate
        }
    }
}

@Composable
private fun ExportActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
