package com.pocketdev.advance_erp.feature.accounts

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import com.pocketdev.advance_erp.core.designsystem.StatusErrorContainer
import com.pocketdev.advance_erp.core.designsystem.ERPSurfaceVariant
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.ERPLoadingState
import com.pocketdev.advance_erp.core.designsystem.ERPSearchBar
import com.pocketdev.advance_erp.core.designsystem.ERPSectionHeader
import com.pocketdev.advance_erp.core.designsystem.LocalIsDarkTheme
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.security.SecurityManager
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.AccountTransfer
import com.pocketdev.advance_erp.data.entity.AccountType
import com.pocketdev.advance_erp.data.entity.AuditLog
import com.pocketdev.advance_erp.data.entity.BackupRecord
import com.pocketdev.advance_erp.data.entity.BankReconciliation
import com.pocketdev.advance_erp.data.entity.BankReconciliationItem
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.ExpenseCategory
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.data.entity.JournalEntry
import com.pocketdev.advance_erp.data.entity.LiquidAccountKind
import com.pocketdev.advance_erp.data.entity.NormalBalance
import com.pocketdev.advance_erp.data.entity.ReconciliationStatus
import com.pocketdev.advance_erp.data.entity.TrialBalanceItem
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import kotlin.math.abs

enum class AccountsTab(val titleEn: String, val titleBn: String, val icon: ImageVector) {
    CASH_BANK("Cash & Bank", "নগদ ও ব্যাংক", Icons.Outlined.AccountBalance),
    EXPENSES("Expenses", "ব্যয় ব্যবস্থাপনা", Icons.Outlined.ReceiptLong),
    STATEMENTS("Financial Statements", "আর্থিক বিবরণী", Icons.Outlined.Assessment),
    RECONCILIATION("Bank Reconcile", "ব্যাংক সমন্বয়", Icons.Outlined.CheckCircle),
    CHART_OF_ACCOUNTS("Chart of Accounts", "হিসাব তালিকা", Icons.Outlined.FormatListNumbered),
    JOURNAL("Journal Entries", "জাবেদা দাখিলা", Icons.Outlined.MenuBook),
    TRIAL_BALANCE("Trial Balance", "রেওয়ামিল", Icons.Outlined.Equalizer),
    BACKUP_RESTORE("Backup & Safety", "ব্যাকআপ ও নিরাপত্তা", Icons.Outlined.Security)
}

private enum class CoaFilterCategory(val titleEn: String, val titleBn: String) {
    ALL("All", "সকল"),
    LIQUID("Cash & Bank", "নগদ ও ব্যাংক"),
    ASSET("Assets", "সম্পদ"),
    LIABILITY("Liabilities", "দায়"),
    EQUITY("Equity", "মূলধন"),
    REVENUE("Revenue", "আয়"),
    EXPENSE("Expense", "ব্যয়")
}

private enum class JournalPeriod { ALL, THIS_MONTH, THIS_YEAR }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountsScreen(
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = hiltViewModel(),
    initialTab: AccountsTab = AccountsTab.CASH_BANK
) {
    val context = LocalContext.current
    val currentConfiguration = LocalConfiguration.current
    val uiState by viewModel.uiState.collectAsState()
    val isBangla = uiState.language == AppLanguage.BANGLA
    val accountingConfiguration = remember(uiState.language, currentConfiguration) {
        Configuration(currentConfiguration).apply { setLocale(uiState.language.locale) }
    }
    val canViewProfit = SecurityManager.canViewProfit(uiState.userRole)
    val canExportReports = SecurityManager.canExportReport(uiState.userRole)
    val canRecordExpenses = SecurityManager.canRecordExpense(uiState.userRole)
    val canPostJournals = SecurityManager.canPostJournal(uiState.userRole)
    var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    val pagerState = rememberPagerState(initialPage = initialTab.ordinal, pageCount = { AccountsTab.entries.size })
    val categoryListState = rememberLazyListState()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            AccountsTab.entries.getOrNull(page)?.let { tab ->
                selectedTab = tab
                if (tab == AccountsTab.TRIAL_BALANCE) {
                    viewModel.refreshTrialBalance()
                } else if (tab == AccountsTab.STATEMENTS) {
                    viewModel.loadFinancialStatements()
                }
            }
        }
    }
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab.ordinal) pagerState.animateScrollToPage(selectedTab.ordinal)
        categoryListState.animateScrollToItem(selectedTab.ordinal)
    }

    // Dialog state
    var showNewAccountDialog by rememberSaveable { mutableStateOf(false) }
    var newAccountInitialKind by remember { mutableStateOf<LiquidAccountKind?>(LiquidAccountKind.BANK) }
    var newAccountGeneralMode by rememberSaveable { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<ChartOfAccount?>(null) }
    var detailAccount by remember { mutableStateOf<ChartOfAccount?>(null) }

    var coaSearchQuery by rememberSaveable { mutableStateOf("") }
    var coaCategoryFilter by remember { mutableStateOf(CoaFilterCategory.ALL) }
    var journalSearchQuery by rememberSaveable { mutableStateOf("") }
    var journalPeriod by rememberSaveable { mutableStateOf(JournalPeriod.ALL) }

    var showNewJournalDialog by rememberSaveable { mutableStateOf(false) }
    var showRecordExpenseDialog by rememberSaveable { mutableStateOf(false) }
    var showTransferDialog by rememberSaveable { mutableStateOf(false) }
    var showReconciliationDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateBackupDialog by rememberSaveable { mutableStateOf(false) }

    var entryToReverse by remember { mutableStateOf<JournalEntry?>(null) }
    var expenseToReverse by remember { mutableStateOf<ExpenseVoucher?>(null) }
    var backupToRestore by remember { mutableStateOf<BackupRecord?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val trialBalanceMap = remember(uiState.trialBalance) {
        uiState.trialBalance.associateBy { it.accountId }
    }

    val filteredCoaAccounts = remember(uiState.accounts, coaSearchQuery, coaCategoryFilter) {
        val q = coaSearchQuery.trim().lowercase()
        uiState.accounts.filter { acc ->
            val matchesCat = when (coaCategoryFilter) {
                CoaFilterCategory.ALL -> true
                CoaFilterCategory.LIQUID -> acc.liquidKind != null
                CoaFilterCategory.ASSET -> acc.accountType == AccountType.ASSET.name
                CoaFilterCategory.LIABILITY -> acc.accountType == AccountType.LIABILITY.name
                CoaFilterCategory.EQUITY -> acc.accountType == AccountType.EQUITY.name
                CoaFilterCategory.REVENUE -> acc.accountType == AccountType.REVENUE.name
                CoaFilterCategory.EXPENSE -> acc.accountType == AccountType.EXPENSE.name
            }
            val matchesQuery = q.isBlank() ||
                acc.accountCode.lowercase().contains(q) ||
                acc.nameEn.lowercase().contains(q) ||
                acc.nameBn.contains(q) ||
                acc.institutionName?.lowercase()?.contains(q) == true ||
                acc.accountNumber?.lowercase()?.contains(q) == true
            matchesCat && matchesQuery
        }
    }
    val filteredJournalEntries = remember(uiState.journalEntries, journalSearchQuery, journalPeriod) {
        val query = journalSearchQuery.trim().lowercase()
        val now = Calendar.getInstance()
        uiState.journalEntries.filter { entry ->
            val date = Calendar.getInstance().apply { timeInMillis = entry.entryDate }
            val matchesPeriod = when (journalPeriod) {
                JournalPeriod.ALL -> true
                JournalPeriod.THIS_MONTH -> date.get(Calendar.YEAR) == now.get(Calendar.YEAR) && date.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                JournalPeriod.THIS_YEAR -> date.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            }
            val matchesQuery = query.isBlank() || listOfNotNull(
                entry.entryNumber, entry.descriptionEn, entry.descriptionBn, entry.referenceType, entry.referenceId
            ).any { it.lowercase().contains(query) }
            matchesPeriod && matchesQuery
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (uiState.isLoading && uiState.accounts.isEmpty()) {
        ERPLoadingState(modifier = modifier.fillMaxSize())
        return
    }

    CompositionLocalProvider(LocalConfiguration provides accountingConfiguration) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ERPBackground)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Module Overview Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            if (LocalIsDarkTheme.current) {
                                listOf(DeepNavy, Color(0xFF282044), Color(0xFF064C54))
                            } else {
                                listOf(Color(0xFF792B87), Color(0xFF8B45A7), Color(0xFF008F91))
                            }
                        )
                    )
                    .padding(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Outlined.Assessment,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.align(Alignment.CenterEnd).size(88.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalance,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(27.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.accounts_ledger),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.accounting_header_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(PrimaryPurple.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.accounting_account_count, uiState.accounts.count { it.isActive }),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs
            LazyRow(
                state = categoryListState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(AccountsTab.entries) { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) Brush.linearGradient(listOf(PrimaryPurple, SecondaryTeal))
                                else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
                            )
                            .border(1.dp, if (isSelected) Color.Transparent else PrimaryPurple.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
                            .defaultMinSize(minHeight = 48.dp)
                            .selectable(selected = isSelected, role = Role.Tab) {
                                selectedTab = tab
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else PrimaryPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.language == AppLanguage.BANGLA) tab.titleBn else tab.titleEn,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Each category has an independent vertically scrollable page.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                key = { page -> AccountsTab.entries[page].name }
            ) { page ->
                val pageTab = AccountsTab.entries[page]
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                // ==========================================
                // TAB 1: CASH & BANK
                // ==========================================
                if (pageTab == AccountsTab.CASH_BANK) {
                    val activeLiquidAccounts = uiState.liquidAccounts.filter { it.isActive }
                    val totalLiquidMinor = activeLiquidAccounts.sumOf { acc ->
                        val tb = trialBalanceMap[acc.id]
                        (tb?.totalDebitMinor ?: 0L) - (tb?.totalCreditMinor ?: 0L)
                    }
                    val totalCashMinor = activeLiquidAccounts
                        .filter { it.liquidKind == LiquidAccountKind.CASH }
                        .sumOf { acc ->
                            val tb = trialBalanceMap[acc.id]
                            (tb?.totalDebitMinor ?: 0L) - (tb?.totalCreditMinor ?: 0L)
                        }
                    val totalBankMinor = activeLiquidAccounts
                        .filter { it.liquidKind == LiquidAccountKind.BANK }
                        .sumOf { acc ->
                            val tb = trialBalanceMap[acc.id]
                            (tb?.totalDebitMinor ?: 0L) - (tb?.totalCreditMinor ?: 0L)
                        }
                    val totalMfsMinor = activeLiquidAccounts
                        .filter { it.liquidKind == LiquidAccountKind.MOBILE_BANKING }
                        .sumOf { acc ->
                            val tb = trialBalanceMap[acc.id]
                            (tb?.totalDebitMinor ?: 0L) - (tb?.totalCreditMinor ?: 0L)
                        }

                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ERPSectionHeader(
                                title = stringResource(R.string.cash_and_bank),
                                subtitle = stringResource(R.string.accounting_cash_subtitle)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        newAccountInitialKind = LiquidAccountKind.BANK
                                        newAccountGeneralMode = false
                                        showNewAccountDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.accounting_add_account), fontSize = 12.sp, maxLines = 1)
                                }
                                Button(
                                    onClick = { showTransferDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.accounting_transfer_funds), fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }

                    // Total Liquid Summary Card
                    item {
                        FinanceCard(backgroundColor = DeepNavy, contentPadding = 14.dp) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (isBangla) "মোট তরল তহবিল (Cash + Bank + MFS)" else "Total Liquid Balance (Cash + Bank + MFS)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = Formatters.formatMoney(totalLiquidMinor, CurrencyCode.BDT, uiState.language),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LiquidSummaryPill(
                                        label = if (isBangla) "নগদ" else "Cash",
                                        amount = Formatters.formatMoney(totalCashMinor, CurrencyCode.BDT, uiState.language),
                                        modifier = Modifier.weight(1f)
                                    )
                                    LiquidSummaryPill(
                                        label = if (isBangla) "ব্যাংক" else "Bank",
                                        amount = Formatters.formatMoney(totalBankMinor, CurrencyCode.BDT, uiState.language),
                                        modifier = Modifier.weight(1f)
                                    )
                                    LiquidSummaryPill(
                                        label = "bKash/MFS",
                                        amount = Formatters.formatMoney(totalMfsMinor, CurrencyCode.BDT, uiState.language),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Dynamic Cash, Bank & MFS Accounts
                    items(uiState.liquidAccounts, key = { it.id }) { acc ->
                        val tb = trialBalanceMap[acc.id]
                        val balMinor = (tb?.totalDebitMinor ?: 0L) - (tb?.totalCreditMinor ?: 0L)
                        val accentColor = when (acc.liquidKind) {
                            LiquidAccountKind.CASH -> PrimaryPurple
                            LiquidAccountKind.BANK -> SecondaryTeal
                            LiquidAccountKind.MOBILE_BANKING -> PrimaryPurple
                            null -> DeepNavy
                        }
                        val subDetails = buildList {
                            add(acc.accountCode)
                            acc.institutionName?.let { add(it) }
                            acc.branchName?.let { add(it) }
                            acc.accountNumber?.let { add("A/C $it") }
                            if (!acc.isActive) add(if (isBangla) "নিষ্ক্রিয়" else "Inactive")
                        }.joinToString(" • ")

                        AccountBalanceCard(
                            accountName = if (isBangla && acc.nameBn.isNotBlank()) acc.nameBn else acc.nameEn,
                            accountSubtitle = subDetails,
                            balanceMinor = balMinor,
                            kind = acc.liquidKind ?: LiquidAccountKind.CASH,
                            isActive = acc.isActive,
                            color = accentColor,
                            language = uiState.language,
                            onClick = { detailAccount = acc }
                        )
                    }

                    // Cash & Bank Book Section
                    val selectedBookAccount = uiState.liquidAccounts.find { it.id == uiState.selectedBankAccountId }
                        ?: uiState.liquidAccounts.firstOrNull()
                    val isCashBookSelected = selectedBookAccount?.id == "coa-1010"
                    val displayedBookEntries = if (isCashBookSelected) uiState.cashBookEntries else uiState.bankBookEntries

                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ERPSectionHeader(
                                    modifier = Modifier.weight(1f),
                                    title = stringResource(R.string.accounting_cash_book),
                                    subtitle = selectedBookAccount?.let {
                                        if (isBangla && it.nameBn.isNotBlank()) it.nameBn else it.nameEn
                                    } ?: "Running Balance"
                                )
                                OutlinedButton(
                                    onClick = {
                                        viewModel.exportFinancialCsv(
                                            context,
                                            if (isCashBookSelected) "CASH_BOOK" else "BANK_BOOK"
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CSV", fontSize = 11.sp)
                                }
                            }

                            if (uiState.liquidAccounts.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(uiState.liquidAccounts, key = { "book-${it.id}" }) { acc ->
                                        val isSel = selectedBookAccount?.id == acc.id
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(if (isSel) SecondaryTeal else MaterialTheme.colorScheme.surface)
                                                .border(1.dp, if (isSel) SecondaryTeal else ERPDivider, RoundedCornerShape(16.dp))
                                                .clickable { viewModel.selectBankAccountForBook(acc.id) }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = if (isBangla && acc.nameBn.isNotBlank()) acc.nameBn else acc.nameEn,
                                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (displayedBookEntries.isEmpty()) {
                        item {
                            FinanceCard {
                                Text(
                                    text = stringResource(R.string.accounting_no_book_entries),
                                    color = TextMuted
                                )
                            }
                        }
                    } else {
                        items(displayedBookEntries.reversed().take(20), key = { it.id }) { entry ->
                            FinanceCard(contentPadding = 10.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${entry.referenceNumber} • ${entry.description}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatDate(entry.date, uiState.language), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (entry.receiptMinor > 0L) {
                                            Text("+ ${Formatters.formatMoney(entry.receiptMinor, CurrencyCode.BDT, uiState.language)}", color = StatusSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        if (entry.paymentMinor > 0L) {
                                            Text("- ${Formatters.formatMoney(entry.paymentMinor, CurrencyCode.BDT, uiState.language)}", color = StatusError, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Text("${if (isBangla) "ব্যালেন্স" else "Balance"}: ${Formatters.formatMoney(entry.runningBalanceMinor, CurrencyCode.BDT, uiState.language)}", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 2: EXPENSES
                // ==========================================
                if (pageTab == AccountsTab.EXPENSES) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ERPSectionHeader(
                                title = stringResource(R.string.expenses),
                                subtitle = stringResource(R.string.accounting_expense_subtitle)
                            )
                            if (canRecordExpenses && uiState.expenses.isNotEmpty()) Button(
                                onClick = { showRecordExpenseDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.accounting_new_expense), fontSize = 12.sp)
                            }
                        }
                    }

                    val postedExpenses = uiState.expenses.filter { it.isPosted && !it.isReversed }
                    item {
                        FinanceCard(
                            backgroundColor = DeepNavy,
                            border = BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.35f)),
                            contentPadding = 14.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(if (isBangla) "মোট নথিভুক্ত ব্যয়" else "Total posted expenses", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
                                    Text(Formatters.formatMoney(postedExpenses.sumOf { it.bdtAmountMinor }, CurrencyCode.BDT, uiState.language), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(if (isBangla) "ভাউচার" else "Vouchers", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                                    Text(Formatters.formatNumber(postedExpenses.size.toLong(), uiState.language), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (uiState.expenses.isEmpty()) {
                        item {
                            FinanceCard(contentPadding = 24.dp) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(40.dp))
                                    Text(if (isBangla) "এখনও কোনো ব্যয় নেই" else "No expenses yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.accounting_empty_expenses), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    if (canRecordExpenses) {
                                        OutlinedButton(onClick = { showRecordExpenseDialog = true }) {
                                            Text(stringResource(R.string.accounting_new_expense))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(uiState.expenses) { exp ->
                            val categoryTitle = ExpenseCategory.entries.firstOrNull { it.name == exp.category || it.code == exp.category }
                                ?.let { if (isBangla) it.titleBn else it.titleEn } ?: exp.category
                            val paymentAccountName = uiState.accounts.firstOrNull { it.id == exp.paymentAccountId }
                                ?.let { if (isBangla && it.nameBn.isNotBlank()) it.nameBn else it.nameEn }
                                ?: exp.paymentAccountId
                            FinanceCard(contentPadding = 14.dp) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(exp.voucherNumber, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                if (exp.isReversed) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .background(StatusErrorContainer, RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(if (isBangla) "বাতিল" else "VOID/REVERSED", color = StatusError, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Text(if (isBangla && exp.descriptionBn.isNotBlank()) exp.descriptionBn else exp.descriptionEn, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                            Text("$categoryTitle • ${Formatters.formatDate(exp.voucherDate, uiState.language)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Text("${if (isBangla) "পেমেন্ট হিসাব" else "Payment account"}: $paymentAccountName", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                Formatters.formatMoney(exp.bdtAmountMinor, CurrencyCode.BDT, uiState.language),
                                                fontWeight = FontWeight.Bold,
                                                color = StatusError
                                            )
                                            if (exp.currency != "BDT") {
                                                Text("${exp.currency} ${exp.amountMinor / 100.0} @ ${exp.exchangeRateToBdt}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            }
                                            if (!exp.isReversed) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                OutlinedButton(
                                                    onClick = { expenseToReverse = exp },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(26.dp)
                                                ) {
                                                    Icon(Icons.Outlined.Replay, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(if (isBangla) "বিপরীত এন্ট্রি" else "Reverse", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 3: FINANCIAL STATEMENTS
                // ==========================================
                if (pageTab == AccountsTab.STATEMENTS) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ERPSectionHeader(
                                title = stringResource(R.string.financial_reports),
                                subtitle = stringResource(R.string.accounting_statements_subtitle)
                            )
                            if (canExportReports) Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (canViewProfit) {
                                Button(
                                    onClick = { viewModel.exportProfitAndLossPdf(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isBangla) "লাভ-ক্ষতি PDF" else "P&L PDF", fontSize = 11.sp, maxLines = 1)
                                }
                                }
                                Button(
                                    onClick = { viewModel.exportBalanceSheetPdf(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isBangla) "উদ্বৃত্তপত্র PDF" else "Balance Sheet PDF", fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }

                    // Profit & Loss Card
                    if (canViewProfit) uiState.profitAndLoss?.let { pnl ->
                        item {
                            FinanceCard(backgroundColor = ERPSurfaceVariant, contentPadding = 14.dp) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(if (isBangla) "লাভ-ক্ষতি সারসংক্ষেপ" else "Profit & Loss Summary (Income Statement)", fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isBangla) "মোট বিক্রয় আয়" else "Gross Sales Revenue", style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatMoney(pnl.grossSalesMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isBangla) "বিক্রয় ফেরত / সমন্বয়" else "Sales Returns / Adjustments", style = MaterialTheme.typography.bodySmall)
                                        Text("- ${Formatters.formatMoney(pnl.salesReturnsMinor + pnl.salesDiscountsMinor, CurrencyCode.BDT, uiState.language)}", color = StatusError)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isBangla) "নিট বিক্রয় আয়" else "Net Sales Revenue", style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatMoney(pnl.netSalesRevenueMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isBangla) "বিক্রিত পণ্যের ব্যয় (COGS)" else "Cost of Goods Sold (COGS)", style = MaterialTheme.typography.bodySmall)
                                        Text("- ${Formatters.formatMoney(pnl.costOfGoodsSoldMinor, CurrencyCode.BDT, uiState.language)}", color = StatusError)
                                    }
                                    Divider(color = ERPDivider, modifier = Modifier.padding(vertical = 4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isBangla) "মোট মুনাফা" else "Gross Profit", fontWeight = FontWeight.Bold)
                                        Text(Formatters.formatMoney(pnl.grossProfitMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Black, color = SecondaryTeal)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isBangla) "পরিচালন ব্যয়" else "Operating Expenses", style = MaterialTheme.typography.bodySmall)
                                        Text("- ${Formatters.formatMoney(pnl.totalOperatingExpensesMinor, CurrencyCode.BDT, uiState.language)}", color = StatusError)
                                    }
                                    Divider(color = ERPDivider, modifier = Modifier.padding(vertical = 4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isBangla) "নিট মুনাফা / (লোকসান)" else "NET PROFIT / (LOSS)", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall)
                                        val netProfitColor = if (pnl.netProfitMinor >= 0) StatusSuccess else StatusError
                                        Text(Formatters.formatMoney(pnl.netProfitMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Black, color = netProfitColor, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                    } else if (!canViewProfit) {
                        item {
                            FinanceCard(border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.22f))) {
                                Text(
                                    if (isBangla) "লাভ-ক্ষতি ও COGS তথ্য দেখতে মালিক/প্রশাসকের অনুমতি প্রয়োজন।" else "Owner/Admin access is required to view profit and COGS details.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Balance Sheet Card
                    uiState.balanceSheet?.let { bs ->
                        item {
                            FinanceCard(backgroundColor = ERPSurfaceVariant, contentPadding = 14.dp) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(stringResource(R.string.accounting_balance_sheet), fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Box(
                                            modifier = Modifier
                                                .background(if (bs.isBalanced) StatusSuccess.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(if (bs.isBalanced) "Assets = Liab + Equity ✓" else "Discrepancy ✕", color = if (bs.isBalanced) StatusSuccess else StatusError, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isBangla) "মোট সম্পদ" else "Total Assets", style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatMoney(bs.totalAssetsMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Bold, color = SecondaryTeal)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isBangla) "মোট দায়" else "Total Liabilities", style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatMoney(bs.totalLiabilitiesMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Bold, color = StatusError)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isBangla) "মোট মূলধন (মূলধন + সঞ্চিত আয়)" else "Total Equity (Capital + Retained)", style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatMoney(bs.totalEquityMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Bold, color = PrimaryPurple)
                                    }
                                }
                            }
                        }
                    }

                    uiState.cashFlow?.let { flow ->
                        item {
                            FinanceCard(
                                backgroundColor = ERPSurfaceVariant,
                                contentPadding = 14.dp,
                                border = BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.24f))
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(if (isBangla) "নগদ প্রবাহ বিবরণী" else "Cash Flow Statement", fontWeight = FontWeight.Bold, color = TextPrimary)
                                    FinanceValueRow(if (isBangla) "গ্রাহকের কাছ থেকে আদায়" else "Customer receipts", flow.cashInflowCustomersMinor, uiState.language, StatusSuccess)
                                    FinanceValueRow(if (isBangla) "সরবরাহকারীকে পরিশোধ" else "Supplier payments", -flow.cashOutflowSuppliersMinor, uiState.language, StatusError)
                                    FinanceValueRow(if (isBangla) "ব্যয় পরিশোধ" else "Expense payments", -flow.cashOutflowExpensesMinor, uiState.language, StatusError)
                                    Divider(color = ERPDivider)
                                    FinanceValueRow(if (isBangla) "পরিচালন থেকে নিট নগদ" else "Net cash from operations", flow.netCashFromOperationsMinor, uiState.language, PrimaryPurple, emphasized = true)
                                    FinanceValueRow(if (isBangla) "বিনিয়োগ থেকে নিট নগদ" else "Net cash from investing", flow.netCashFromInvestingMinor, uiState.language, TextPrimary)
                                    FinanceValueRow(if (isBangla) "অর্থায়ন থেকে নিট নগদ" else "Net cash from financing", flow.netCashFromFinancingMinor, uiState.language, TextPrimary)
                                    FinanceValueRow(if (isBangla) "নগদে নিট পরিবর্তন" else "Net change in cash", flow.netChangeInCashMinor, uiState.language, SecondaryTeal, emphasized = true)
                                    FinanceValueRow(if (isBangla) "সমাপনী নগদ ও ব্যাংক" else "Closing cash & bank", flow.closingCashBankMinor, uiState.language, TextPrimary, emphasized = true)
                                }
                            }
                        }
                    }

                    // Receivables & Payables Aging Card
                    item {
                        FinanceCard(contentPadding = 14.dp) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.accounting_working_capital_aging), fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(if (isBangla) "গ্রাহকের পাওনা:" else "Customer Receivables Aging:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                uiState.receivablesAging?.buckets?.forEach { b ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isBangla) b.labelBn else b.labelEn, style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatMoney(b.amountMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Medium)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(if (isBangla) "সরবরাহকারীর দেনা:" else "Supplier Payables Aging:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                uiState.payablesAging?.buckets?.forEach { b ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isBangla) b.labelBn else b.labelEn, style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatMoney(b.amountMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 4: BANK RECONCILIATION
                // ==========================================
                if (pageTab == AccountsTab.RECONCILIATION) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                modifier = Modifier.weight(1f),
                                title = if (isBangla) "ব্যাংক সমন্বয়" else "Bank Reconciliation",
                                subtitle = stringResource(R.string.accounting_reconciliation_subtitle)
                            )
                            Button(
                                onClick = { showReconciliationDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.accounting_new_reconciliation), fontSize = 12.sp)
                            }
                        }
                    }

                    if (uiState.reconciliations.isEmpty()) {
                        item {
                            FinanceCard {
                                Text(if (isBangla) "এখনও কোনো ব্যাংক সমন্বয় তৈরি হয়নি। ব্যাংক স্টেটমেন্টের লেনদেন ERP জাবেদার সঙ্গে মিলাতে নতুন সমন্বয় শুরু করুন।" else "No reconciliation statements created yet. Start a reconciliation to match bank statement transactions against ERP journal records.", color = TextMuted)
                            }
                        }
                    } else {
                        items(uiState.reconciliations) { rec ->
                            val reconciliationStatus = ReconciliationStatus.entries.firstOrNull { it.name == rec.status }
                            FinanceCard(contentPadding = 14.dp) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(rec.reconciliationNumber, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            val bankName = uiState.accounts.firstOrNull { it.id == rec.bankAccountId }
                                                ?.let { if (isBangla && it.nameBn.isNotBlank()) it.nameBn else it.nameEn }
                                                ?: rec.bankAccountId
                                            Text("${if (isBangla) "ব্যাংক হিসাব" else "Bank account"}: $bankName • ${reconciliationStatus?.let { if (isBangla) it.labelBn else it.labelEn } ?: rec.status}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                            Text("${if (isBangla) "সময়কাল" else "Period"}: ${Formatters.formatDate(rec.statementStartDate, uiState.language)} - ${Formatters.formatDate(rec.statementEndDate, uiState.language)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            val isZero = rec.status == ReconciliationStatus.RECONCILED.name
                                            Box(
                                                modifier = Modifier
                                                    .background(if (isZero) StatusSuccess.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    reconciliationStatus?.let { if (isBangla) it.labelBn else it.labelEn } ?: rec.status,
                                                    color = when (reconciliationStatus) {
                                                        ReconciliationStatus.RECONCILED -> StatusSuccess
                                                        ReconciliationStatus.IN_PROGRESS -> StatusWarning
                                                        else -> StatusError
                                                    },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Text("ERP ${if (isBangla) "ব্যালেন্স" else "balance"}: ${Formatters.formatMoney(rec.erpClosingBalanceMinor, CurrencyCode.BDT, uiState.language)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 5: CHART OF ACCOUNTS
                // ==========================================
                if (pageTab == AccountsTab.CHART_OF_ACCOUNTS) {
                    val filteredAccounts = filteredCoaAccounts

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.chart_of_accounts),
                                subtitle = if (isBangla) "সম্পদ, দায়, মূলধন, আয় ও ব্যয়ের হিসাব" else "Assets, Liabilities, Equity, Revenue & Expenses"
                            )
                            Button(
                                onClick = {
                                    newAccountInitialKind = LiquidAccountKind.BANK
                                    newAccountGeneralMode = false
                                    showNewAccountDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ ${stringResource(R.string.accounting_new_account)}", fontSize = 12.sp)
                            }
                        }
                    }

                    item {
                        ERPSearchBar(
                            query = coaSearchQuery,
                            onQueryChange = { coaSearchQuery = it },
                            placeholder = if (isBangla) "কোড, নাম, ব্যাংক বা অ্যাকাউন্ট নং খুঁজুন..." else "Search by code, name, bank or A/C #..."
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(CoaFilterCategory.entries) { cat ->
                                val count = when (cat) {
                                    CoaFilterCategory.ALL -> uiState.accounts.size
                                    CoaFilterCategory.LIQUID -> uiState.accounts.count { it.liquidKind != null }
                                    CoaFilterCategory.ASSET -> uiState.accounts.count { it.accountType == AccountType.ASSET.name }
                                    CoaFilterCategory.LIABILITY -> uiState.accounts.count { it.accountType == AccountType.LIABILITY.name }
                                    CoaFilterCategory.EQUITY -> uiState.accounts.count { it.accountType == AccountType.EQUITY.name }
                                    CoaFilterCategory.REVENUE -> uiState.accounts.count { it.accountType == AccountType.REVENUE.name }
                                    CoaFilterCategory.EXPENSE -> uiState.accounts.count { it.accountType == AccountType.EXPENSE.name }
                                }
                                val isSel = coaCategoryFilter == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSel) PrimaryPurple else MaterialTheme.colorScheme.surface)
                                        .border(1.dp, if (isSel) PrimaryPurple else ERPDivider, RoundedCornerShape(18.dp))
                                        .clickable { coaCategoryFilter = cat }
                                        .padding(horizontal = 11.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${if (isBangla) cat.titleBn else cat.titleEn} ($count)",
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    if (filteredAccounts.isEmpty()) {
                        item {
                            FinanceCard {
                                Text(
                                    text = if (isBangla) "কোনো অ্যাকাউন্ট পাওয়া যায়নি।" else "No matching accounts found.",
                                    color = TextMuted
                                )
                            }
                        }
                    } else if (coaCategoryFilter == CoaFilterCategory.ALL && coaSearchQuery.isBlank()) {
                        AccountType.entries.forEach { groupType ->
                            val groupAccounts = filteredAccounts.filter { it.accountType == groupType.name }
                            if (groupAccounts.isNotEmpty()) {
                                item(key = "header-${groupType.name}") {
                                    CoaGroupHeader(
                                        accountType = groupType,
                                        count = groupAccounts.size,
                                        isBangla = isBangla
                                    )
                                }
                                items(groupAccounts, key = { it.id }) { acc ->
                                    ChartOfAccountCard(
                                        account = acc,
                                        trialItem = trialBalanceMap[acc.id],
                                        language = uiState.language,
                                        onClick = { detailAccount = acc }
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredAccounts, key = { it.id }) { acc ->
                            ChartOfAccountCard(
                                account = acc,
                                trialItem = trialBalanceMap[acc.id],
                                language = uiState.language,
                                onClick = { detailAccount = acc }
                            )
                        }
                    }
                }

                // ==========================================
                // TAB 6: JOURNAL ENTRIES
                // ==========================================
                if (pageTab == AccountsTab.JOURNAL) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.journal_entries),
                                subtitle = stringResource(R.string.accounting_journal_subtitle)
                            )
                            if (canPostJournals) Button(
                                onClick = { showNewJournalDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.accounting_new_entry),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = journalSearchQuery,
                                onValueChange = { journalSearchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                placeholder = { Text(if (isBangla) "রেফারেন্স, বিবরণ বা জাবেদা নম্বর খুঁজুন" else "Search reference, description or journal number") },
                                leadingIcon = { Icon(Icons.Outlined.Assessment, contentDescription = null) }
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(JournalPeriod.entries) { period ->
                                    val selected = journalPeriod == period
                                    val title = when (period) {
                                        JournalPeriod.ALL -> if (isBangla) "সব তারিখ" else "All time"
                                        JournalPeriod.THIS_MONTH -> if (isBangla) "এই মাস" else "This month"
                                        JournalPeriod.THIS_YEAR -> if (isBangla) "এই বছর" else "This year"
                                    }
                                    Text(
                                        text = title,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(if (selected) PrimaryPurple else MaterialTheme.colorScheme.surface)
                                            .selectable(selected = selected, role = Role.Tab) { journalPeriod = period }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    if (filteredJournalEntries.isEmpty()) {
                        item {
                            FinanceCard {
                                Text(
                                    if (uiState.journalEntries.isEmpty()) {
                                        if (isBangla) "এখনও কোনো জাবেদা পোস্ট হয়নি। সরবরাহকারী পেমেন্ট ও গুদাম গ্রহণের এন্ট্রি এখানে স্বয়ংক্রিয়ভাবে আসে।" else "No journal entries posted yet. Supplier payments and warehouse receipts auto-post entries here."
                                    } else {
                                        if (isBangla) "এই খোঁজ বা তারিখে কোনো জাবেদা নেই।" else "No journal entries match this search or date filter."
                                    },
                                    color = TextMuted
                                )
                            }
                        }
                    } else {
                        items(filteredJournalEntries, key = { it.id }) { entry ->
                            FinanceCard(contentPadding = 14.dp) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Top Row: Entry number + badges (left) & Reverse button (right)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = entry.entryNumber,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (entry.isPosted) StatusSuccess.copy(alpha = 0.14f)
                                                        else MaterialTheme.colorScheme.surfaceVariant,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (entry.isPosted) {
                                                        if (isBangla) "পোস্ট হয়েছে" else "Posted"
                                                    } else {
                                                        if (isBangla) "খসড়া" else "Draft"
                                                    },
                                                    color = if (entry.isPosted) StatusSuccess else TextMuted,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1
                                                )
                                            }
                                            if (entry.isReversed) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(StatusErrorContainer, RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "REVERSED",
                                                        color = StatusError,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }

                                        if (!entry.isReversed && entry.referenceType != "REVERSAL") {
                                            OutlinedButton(
                                                onClick = { entryToReverse = entry },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.heightIn(min = 30.dp)
                                            ) {
                                                Icon(Icons.Outlined.Replay, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isBangla) "রিভার্স" else "Reverse",
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }

                                    // Description (full width, clean line height)
                                    Text(
                                        text = if (isBangla && entry.descriptionBn.isNotBlank()) entry.descriptionBn else entry.descriptionEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Reference & Date
                                    Text(
                                        text = "Ref: ${entry.referenceType}  •  ${Formatters.formatDate(entry.entryDate, uiState.language)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Balanced Debit / Credit Split Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ERPSurfaceVariant.copy(alpha = 0.65f))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isBangla) "ডেবিট (Dr)" else "Debit (Dr)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = Formatters.formatMoney(entry.totalDebitMinor, CurrencyCode.BDT, uiState.language),
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryPurple,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = if (isBangla) "ক্রেডিট (Cr)" else "Credit (Cr)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = Formatters.formatMoney(entry.totalCreditMinor, CurrencyCode.BDT, uiState.language),
                                                fontWeight = FontWeight.Bold,
                                                color = SecondaryTeal,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 7: TRIAL BALANCE
                // ==========================================
                if (pageTab == AccountsTab.TRIAL_BALANCE) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.trial_balance),
                                subtitle = stringResource(R.string.accounting_trial_subtitle)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (uiState.isBalanced) StatusSuccess.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (uiState.isBalanced) Icons.Outlined.CheckCircle else Icons.Outlined.Error,
                                            contentDescription = null,
                                            tint = if (uiState.isBalanced) StatusSuccess else StatusError,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (uiState.isBalanced) { if (isBangla) "সমতাপূর্ণ" else "Balanced" } else { if (isBangla) "অমিল" else "Out of Balance" },
                                            color = if (uiState.isBalanced) StatusSuccess else StatusError,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.exportFinancialCsv(context, "TRIAL_BALANCE") }) {
                                    Icon(Icons.Outlined.Download, contentDescription = "CSV", tint = PrimaryPurple)
                                }
                            }
                        }
                    }

                    // Trial Balance Summary Card
                    item {
                        FinanceCard(backgroundColor = DeepNavy, contentPadding = 14.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (isBangla) "মোট ডেবিট" else "Total Debit", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        Formatters.formatMoney(uiState.totalTrialDebit, CurrencyCode.BDT, uiState.language),
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (isBangla) "মোট ক্রেডিট" else "Total Credit", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        Formatters.formatMoney(uiState.totalTrialCredit, CurrencyCode.BDT, uiState.language),
                                        color = SecondaryTeal,
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            FinanceValueRow(
                                label = if (isBangla) "পার্থক্য" else "Difference",
                                amountMinor = abs(uiState.totalTrialDebit - uiState.totalTrialCredit),
                                language = uiState.language,
                                amountColor = if (uiState.isBalanced) StatusSuccess else StatusError,
                                emphasized = true
                            )
                        }
                    }

                    items(uiState.trialBalance) { tb ->
                        if (tb.totalDebitMinor > 0L || tb.totalCreditMinor > 0L) {
                            FinanceCard(contentPadding = 10.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${tb.accountCode} - ${if (isBangla && tb.accountNameBn.isNotBlank()) tb.accountNameBn else tb.accountNameEn}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(tb.accountType, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(if (isBangla) "ডেবিট" else "Debit", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Text(
                                                if (tb.netDebitMinor > 0L) Formatters.formatMoney(tb.netDebitMinor, CurrencyCode.BDT, uiState.language) else "-",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextPrimary
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(if (isBangla) "ক্রেডিট" else "Credit", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Text(
                                                if (tb.netCreditMinor > 0L) Formatters.formatMoney(tb.netCreditMinor, CurrencyCode.BDT, uiState.language) else "-",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SecondaryTeal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 8: BACKUP & RESTORE
                // ==========================================
                if (pageTab == AccountsTab.BACKUP_RESTORE) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.backup_safety),
                                subtitle = stringResource(R.string.accounting_backup_subtitle)
                            )
                            Button(
                                onClick = { showCreateBackupDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.accounting_create_backup), fontSize = 12.sp)
                            }
                        }
                    }

                    if (uiState.backupRecords.isEmpty()) {
                        item {
                            FinanceCard {
                                Text(stringResource(R.string.accounting_empty_backups), color = TextMuted)
                            }
                        }
                    } else {
                        items(uiState.backupRecords) { bak ->
                            FinanceCard(contentPadding = 12.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bak.fileName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        Text("${if (isBangla) "আকার" else "Size"}: ${bak.fileSizeBytes / 1024} KB • Schema v${bak.schemaVersion}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Text("SHA-256: ${bak.checksumSha256.take(12)}…", style = MaterialTheme.typography.labelSmall, color = StatusSuccess)
                                        Text(
                                            "${if (isBangla) "স্থানীয় অবস্থান" else "Local location"}: ${File(bak.filePath).parent ?: "App storage"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(Formatters.formatDate(bak.createdAt, uiState.language), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                    Button(
                                        onClick = { backupToRestore = bak },
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(if (isBangla) "পুনরুদ্ধার" else "Restore", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Audit Logs Header & List
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        ERPSectionHeader(
                            title = stringResource(R.string.accounting_audit_trail),
                            subtitle = stringResource(R.string.accounting_financial_operations_log)
                        )
                    }

                    items(uiState.auditLogs.take(20)) { log ->
                        FinanceCard(contentPadding = 10.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("[${log.recordType}] ${log.details}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text("${log.operator} • ${Formatters.formatDate(log.timestamp, uiState.language)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(PrimaryPurple.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(log.action, color = PrimaryPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 70.dp)
        )
    }

    // Confirmation dialog for Journal Reversal
    entryToReverse?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToReverse = null },
            title = { Text(if (isBangla) "জাবেদা বিপরীত করার নিশ্চয়তা" else "Confirm Journal Reversal", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isBangla) "আপনি কি ${entry.entryNumber} জাবেদাটি বিপরীত করতে চান?\n\n" +
                        "ডেবিট ও ক্রেডিট উল্টে একটি নতুন সমন্বয়কারী এন্ট্রি পোস্ট হবে এবং নিরীক্ষা ইতিহাস অক্ষত থাকবে।"
                    else "Are you sure you want to reverse journal entry ${entry.entryNumber}?\n\n" +
                        "A new balancing reversal entry will be posted, swapping debits and credits, preserving the complete immutable audit trail."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reverseJournalEntry(entry.id)
                        entryToReverse = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text(if (isBangla) "বিপরীত এন্ট্রি নিশ্চিত করুন" else "Confirm Reversal")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToReverse = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Confirmation dialog for Expense Reversal
    expenseToReverse?.let { exp ->
        AlertDialog(
            onDismissRequest = { expenseToReverse = null },
            title = { Text(if (isBangla) "ব্যয় ভাউচার বাতিলের নিশ্চয়তা" else "Confirm Expense Voucher Void", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isBangla) "আপনি কি ${exp.voucherNumber} ব্যয় ভাউচারটি বিপরীত করতে চান?\n\n" +
                        "হিসাবের ভারসাম্য রাখতে বিপরীত জাবেদা স্বয়ংক্রিয়ভাবে পোস্ট হবে।"
                    else "Are you sure you want to reverse expense voucher ${exp.voucherNumber}?\n\n" +
                        "The offsetting journal entry will be posted automatically to balance the accounts."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reverseExpense(exp.id, "User voided voucher")
                        expenseToReverse = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(if (isBangla) "ভাউচার বাতিল করুন" else "Void Voucher")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToReverse = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Confirmation dialog for Backup Restore
    backupToRestore?.let { bak ->
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            title = { Text(if (isBangla) "ডেটাবেস পুনরুদ্ধার নিশ্চিত করুন" else "Confirm Database Restore", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isBangla) "${bak.fileName} ব্যাকআপ থেকে ডেটাবেস পুনরুদ্ধার করবেন?\n\n" +
                        "পুনরুদ্ধারের আগে বর্তমান ডেটাবেসের একটি নিরাপত্তা কপি তৈরি হবে। এরপর সক্রিয় ডেটাবেসের তথ্য এই ব্যাকআপ দিয়ে প্রতিস্থাপিত হবে।"
                    else "Are you sure you want to restore database from ${bak.fileName}?\n\n" +
                        "A safety snapshot of your current database will be saved before restoring. System will overwrite active database tables with this backup."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(File(bak.filePath))
                        backupToRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                ) {
                    Text(if (isBangla) "ডেটাবেস পুনরুদ্ধার" else "Restore Database")
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // New Account Dialog (Create Cash / Bank / MFS or GL Account)
    if (showNewAccountDialog) {
        CreateOrEditAccountDialog(
            editingAccount = null,
            initialLiquidKind = newAccountInitialKind,
            initialGeneralMode = newAccountGeneralMode,
            isBangla = isBangla,
            suggestCode = { type, kind -> viewModel.suggestNextAccountCode(type, kind) },
            onDismiss = { showNewAccountDialog = false },
            onCreateLiquidAccount = { kind, nameEn, nameBn, inst, branch, accNo, desc, openBal ->
                viewModel.createLiquidAccount(
                    kind = kind,
                    nameEn = nameEn,
                    nameBn = nameBn,
                    institutionName = inst,
                    branchName = branch,
                    accountNumber = accNo,
                    description = desc,
                    openingBalanceMinor = openBal,
                    onSuccess = { showNewAccountDialog = false }
                )
            },
            onCreateGeneralAccount = { code, nameEn, nameBn, type, normalBal, desc ->
                viewModel.createAccount(
                    code = code,
                    nameEn = nameEn,
                    nameBn = nameBn,
                    type = type,
                    normalBalance = normalBal,
                    desc = desc,
                    onSuccess = { showNewAccountDialog = false }
                )
            },
            onUpdateAccount = {}
        )
    }

    // Edit Account Dialog
    editingAccount?.let { targetAccount ->
        CreateOrEditAccountDialog(
            editingAccount = targetAccount,
            initialLiquidKind = targetAccount.liquidKind,
            initialGeneralMode = targetAccount.liquidKind == null,
            isBangla = isBangla,
            suggestCode = { type, kind -> viewModel.suggestNextAccountCode(type, kind) },
            onDismiss = { editingAccount = null },
            onCreateLiquidAccount = { _, _, _, _, _, _, _, _ -> },
            onCreateGeneralAccount = { _, _, _, _, _, _ -> },
            onUpdateAccount = { updated ->
                viewModel.updateAccount(updated) { editingAccount = null }
            }
        )
    }

    // Account Detail Modal
    detailAccount?.let { acc ->
        AccountDetailDialog(
            account = acc,
            trialItem = trialBalanceMap[acc.id],
            language = uiState.language,
            onDismiss = { detailAccount = null },
            onEdit = {
                detailAccount = null
                editingAccount = acc
            },
            onDeleteAccount = { onErr ->
                viewModel.deleteLiquidAccount(
                    account = acc,
                    onSuccess = { detailAccount = null },
                    onError = onErr
                )
            }
        )
    }

    // Record Expense Dialog
    if (showRecordExpenseDialog) {
        RecordExpenseDialog(
            accounts = uiState.accounts.filter { it.isActive },
            isBangla = isBangla,
            onDismiss = { showRecordExpenseDialog = false },
            onConfirm = { cat, descEn, descBn, amtMinor, curr, rate, payAcc, expAcc, date, ref, notes ->
                viewModel.recordExpense(cat, descEn, descBn, amtMinor, curr, rate, payAcc, expAcc, date, ref, notes)
            }
        )
    }

    // Transfer Funds Dialog
    if (showTransferDialog) {
        AccountTransferDialog(
            accounts = uiState.liquidAccounts.filter { it.isActive }
                .ifEmpty { uiState.accounts.filter { it.isActive && it.accountType == "ASSET" } },
            isBangla = isBangla,
            onDismiss = { showTransferDialog = false },
            onConfirm = { src, dst, amt, date, ref, notes ->
                viewModel.transferFunds(src, dst, amt, date, ref, notes)
            }
        )
    }

    // Start Reconciliation Dialog
    if (showReconciliationDialog) {
        StartReconciliationDialog(
            bankAccounts = uiState.liquidAccounts.filter {
                it.isActive && it.liquidKind != LiquidAccountKind.CASH
            }.ifEmpty { uiState.accounts.filter { it.accountCode in listOf("1020", "1030") } },
            isBangla = isBangla,
            onDismiss = { showReconciliationDialog = false },
            onConfirm = { accId, start, end, openBal, closeBal, notes ->
                viewModel.startReconciliation(accId, start, end, openBal, closeBal, notes)
            }
        )
    }

    // Create Backup Dialog
    if (showCreateBackupDialog) {
        CreateBackupDialog(
            isBangla = isBangla,
            onDismiss = { showCreateBackupDialog = false },
            onConfirm = { notes ->
                viewModel.createBackup(notes)
            }
        )
    }

    // New Journal Entry Dialog
    if (showNewJournalDialog) {
        NewJournalEntryDialog(
            accounts = uiState.accounts.filter { it.isActive },
            isBangla = isBangla,
            onDismiss = { showNewJournalDialog = false },
            onConfirm = { descEn, descBn, lines ->
                viewModel.postManualJournalEntry(descEn, descBn, lines)
            }
        )
    }
    }
}

@Composable
private fun FinanceCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = 3.dp,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    ERPCard(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        elevation = elevation,
        border = border ?: BorderStroke(
            1.dp,
            (if (LocalIsDarkTheme.current) SecondaryTeal else PrimaryPurple).copy(alpha = if (LocalIsDarkTheme.current) 0.24f else 0.11f)
        ),
        onClick = onClick,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
private fun FinanceValueRow(
    label: String,
    amountMinor: Long,
    language: AppLanguage,
    amountColor: Color,
    emphasized: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = if (emphasized) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            Formatters.formatMoney(amountMinor, CurrencyCode.BDT, language),
            style = if (emphasized) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

@Composable
private fun LiquidSummaryPill(
    label: String,
    amount: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
        Text(amount, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AccountBalanceCard(
    accountName: String,
    accountSubtitle: String,
    balanceMinor: Long,
    kind: LiquidAccountKind,
    isActive: Boolean,
    color: Color,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val icon = when (kind) {
        LiquidAccountKind.CASH -> Icons.Outlined.AccountBalanceWallet
        LiquidAccountKind.BANK -> Icons.Outlined.AccountBalance
        LiquidAccountKind.MOBILE_BANKING -> Icons.Outlined.PhoneAndroid
    }
    FinanceCard(
        contentPadding = 14.dp,
        onClick = onClick,
        border = BorderStroke(1.dp, color.copy(alpha = if (LocalIsDarkTheme.current) 0.32f else 0.18f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color.copy(alpha = if (isActive) 0.12f else 0.06f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = if (isActive) color else TextMuted, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) TextPrimary else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = accountSubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = Formatters.formatMoney(balanceMinor, CurrencyCode.BDT, language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isActive) color else TextMuted
            )
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CoaGroupHeader(
    accountType: AccountType,
    count: Int,
    isBangla: Boolean
) {
    val accent = accountTypeColor(accountType.name)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(accent, RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isBangla) "${accountType.titleBn} (${accountType.titleEn})" else accountType.titleEn.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
        }
        Box(
            modifier = Modifier
                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$count",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ChartOfAccountCard(
    account: ChartOfAccount,
    trialItem: TrialBalanceItem?,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val isBangla = language == AppLanguage.BANGLA
    val typeColor = accountTypeColor(account.accountType)
    val netBalanceMinor = if (account.normalBalance == NormalBalance.DEBIT.name) {
        (trialItem?.totalDebitMinor ?: 0L) - (trialItem?.totalCreditMinor ?: 0L)
    } else {
        (trialItem?.totalCreditMinor ?: 0L) - (trialItem?.totalDebitMinor ?: 0L)
    }

    val primaryName = if (isBangla && account.nameBn.isNotBlank()) account.nameBn else account.nameEn
    val secondaryLine = buildList {
        val alt = if (isBangla) account.nameEn else account.nameBn
        if (alt.isNotBlank() && alt != primaryName) add(alt)
        account.institutionName?.takeIf { it !in primaryName }?.let { add(it) }
        account.accountNumber?.let { add("A/C $it") }
    }.joinToString(" • ")

    FinanceCard(contentPadding = 12.dp, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 50.dp)
                    .background(typeColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = account.accountCode,
                    fontWeight = FontWeight.Black,
                    color = typeColor,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = primaryName,
                    fontWeight = FontWeight.Bold,
                    color = if (account.isActive) TextPrimary else TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (secondaryLine.isNotBlank()) {
                    Text(
                        text = secondaryLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    account.liquidKind?.let { kind ->
                        Box(
                            modifier = Modifier
                                .background(SecondaryTeal.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isBangla) kind.titleBn else kind.titleEn,
                                color = SecondaryTeal,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (!account.isActive) {
                        Box(
                            modifier = Modifier
                                .background(StatusError.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isBangla) "নিষ্ক্রিয়" else "INACTIVE",
                                color = StatusError,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatMoney(netBalanceMinor, CurrencyCode.BDT, language),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (netBalanceMinor < 0L) StatusError else DeepNavy
                )
                Text(
                    text = "${account.accountType} • ${if (account.normalBalance == "DEBIT") "Dr" else "Cr"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = typeColor,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun accountTypeColor(accountType: String): Color = when (accountType) {
    "ASSET" -> StatusSuccess
    "LIABILITY" -> PrimaryPurple
    "EQUITY" -> DeepNavy
    "REVENUE" -> SecondaryTeal
    else -> StatusError
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdownField(
    label: String,
    isBangla: Boolean = false,
    accounts: List<ChartOfAccount>,
    selectedAccountId: String,
    onSelect: (ChartOfAccount) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selected = accounts.find { it.id == selectedAccountId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected?.let { acc ->
                val inst = acc.institutionName?.takeIf { it !in acc.nameEn }?.let { " • $it" } ?: ""
                val name = if (isBangla && acc.nameBn.isNotBlank()) acc.nameBn else acc.nameEn
                "${acc.accountCode} - $name$inst"
            } ?: if (isBangla) "হিসাব নির্বাচন করুন" else "Select Account",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("${acc.accountCode} • ${if (isBangla && acc.nameBn.isNotBlank()) acc.nameBn else acc.nameEn}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            val sub = listOfNotNull(
                                acc.liquidKind?.let { if (isBangla) it.titleBn else it.titleEn },
                                acc.institutionName,
                                acc.accountNumber?.let { "A/C $it" }
                            )
                                .joinToString(" • ")
                            if (sub.isNotBlank()) {
                                Text(sub, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    },
                    onClick = {
                        onSelect(acc)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun RecordExpenseDialog(
    accounts: List<ChartOfAccount>,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        category: String,
        descEn: String,
        descBn: String,
        amountMinor: Long,
        currency: String,
        exchangeRate: String,
        paymentAccountId: String,
        expenseAccountId: String,
        voucherDate: Long,
        ref: String?,
        notes: String?
    ) -> Unit
) {
    val categories = ExpenseCategory.entries
    var selectedCat by remember { mutableStateOf(categories.first()) }
    var descEn by rememberSaveable { mutableStateOf("") }
    var descBn by rememberSaveable { mutableStateOf("") }
    var amountStr by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("BDT") }
    var exchangeRate by rememberSaveable { mutableStateOf("1.0000") }
    val paymentAccounts = remember(accounts) {
        val liquid = accounts.filter { it.liquidKind != null }
        val others = accounts.filter { it.liquidKind == null && it.accountType in listOf("ASSET", "LIABILITY") }
        liquid + others
    }
    var paymentAccountId by remember(paymentAccounts) {
        mutableStateOf(paymentAccounts.find { it.accountCode == "1010" }?.id ?: paymentAccounts.firstOrNull()?.id ?: "")
    }
    var refNumber by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(if (isBangla) "পরিচালন ব্যয় নথিভুক্ত করুন" else "Record Operating Expense", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                Text(if (isBangla) "ব্যয়ের খাত:" else "Expense Category:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val isSel = selectedCat == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) PrimaryPurple else ERPSurfaceVariant)
                                .clickable { selectedCat = cat }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(if (isBangla) cat.titleBn else cat.titleEn, color = if (isSel) MaterialTheme.colorScheme.onPrimary else TextPrimary, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = descEn,
                    onValueChange = { descEn = it },
                    label = { Text(if (isBangla) "বিবরণ (ইংরেজি)" else "Description (English)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = descBn,
                    onValueChange = { descBn = it },
                    label = { Text(if (isBangla) "বিবরণ (বাংলা)" else "Description (Bangla)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text(if (isBangla) "পরিমাণ" else "Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it.uppercase() },
                        label = { Text(if (isBangla) "মুদ্রা" else "Currency") },
                        modifier = Modifier.width(90.dp)
                    )
                }

                if (currency != "BDT") {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = exchangeRate,
                        onValueChange = { exchangeRate = it },
                        label = { Text(if (isBangla) "টাকায় বিনিময় হার (যেমন 17.20)" else "Rate to BDT (e.g. 17.20)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                AccountDropdownField(
                    label = if (isBangla) "পেমেন্ট হিসাব (নগদ / ব্যাংক / MFS)" else "Payment Account (Cash / Bank / MFS)",
                    isBangla = isBangla,
                    accounts = paymentAccounts,
                    selectedAccountId = paymentAccountId,
                    onSelect = { paymentAccountId = it.id }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = refNumber,
                    onValueChange = { refNumber = it },
                    label = { Text(if (isBangla) "রসিদ / ইনভয়েস রেফারেন্স" else "Receipt / Invoice Ref") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text(if (isBangla) "বাতিল" else "Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val amtBd = BigDecimal(amountStr.trim())
                                require(amtBd > BigDecimal.ZERO) { if (isBangla) "পরিমাণ শূন্যের বেশি হতে হবে" else "Amount must be positive" }
                                require(descEn.isNotBlank()) { if (isBangla) "বিবরণ আবশ্যক" else "Description is required" }

                                val minor = amtBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                                onConfirm(
                                    selectedCat.name,
                                    descEn,
                                    descBn.ifBlank { descEn },
                                    minor,
                                    currency,
                                    exchangeRate,
                                    paymentAccountId,
                                    selectedCat.defaultAccountId,
                                    System.currentTimeMillis(),
                                    refNumber.ifBlank { null },
                                    null
                                )
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: if (isBangla) "ব্যয়ের তথ্য সঠিক নয়" else "Invalid expense details"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text(if (isBangla) "ব্যয় পোস্ট করুন" else "Post Expense")
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountTransferDialog(
    accounts: List<ChartOfAccount>,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (sourceId: String, destId: String, amountMinor: Long, date: Long, ref: String?, notes: String?) -> Unit
) {
    var sourceId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var destId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: "") }
    var amountStr by rememberSaveable { mutableStateOf("") }
    var refStr by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(18.dp)
            ) {
                Text(if (isBangla) "হিসাবের মধ্যে তহবিল স্থানান্তর" else "Inter-Account Transfer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                AccountDropdownField(
                    label = if (isBangla) "যে হিসাব থেকে" else "From (Source Account)",
                    isBangla = isBangla,
                    accounts = accounts,
                    selectedAccountId = sourceId,
                    onSelect = { sourceId = it.id }
                )

                Spacer(modifier = Modifier.height(10.dp))

                AccountDropdownField(
                    label = if (isBangla) "যে হিসাবে" else "To (Destination Account)",
                    isBangla = isBangla,
                    accounts = accounts,
                    selectedAccountId = destId,
                    onSelect = { destId = it.id }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (isBangla) "স্থানান্তরের পরিমাণ (BDT)" else "Transfer Amount (BDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = refStr,
                    onValueChange = { refStr = it },
                    label = { Text(if (isBangla) "রেফারেন্স (চেক / জমা স্লিপ)" else "Reference (Cheque / Deposit Slip)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text(if (isBangla) "বাতিল" else "Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                require(sourceId != destId) { "Source and destination accounts must be different" }
                                val amtBd = BigDecimal(amountStr.trim())
                                require(amtBd > BigDecimal.ZERO) { "Amount must be positive" }

                                val minor = amtBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                                onConfirm(sourceId, destId, minor, System.currentTimeMillis(), refStr.ifBlank { null }, null)
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Invalid transfer parameters"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                    ) {
                        Text(if (isBangla) "স্থানান্তর সম্পন্ন করুন" else "Execute Transfer")
                    }
                }
            }
        }
    }
}

@Composable
private fun StartReconciliationDialog(
    bankAccounts: List<ChartOfAccount>,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (bankAccountId: String, start: Long, end: Long, openBal: Long, closeBal: Long, notes: String?) -> Unit
) {
    var selectedAccId by remember { mutableStateOf(bankAccounts.firstOrNull()?.id ?: "") }
    var openBalStr by rememberSaveable { mutableStateOf("0.00") }
    var closeBalStr by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(18.dp)
            ) {
                Text(if (isBangla) "ব্যাংক সমন্বয় শুরু করুন" else "Start Bank Reconciliation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                AccountDropdownField(
                    label = if (isBangla) "ব্যাংক / MFS হিসাব" else "Bank / MFS Account",
                    isBangla = isBangla,
                    accounts = bankAccounts,
                    selectedAccountId = selectedAccId,
                    onSelect = { selectedAccId = it.id }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = openBalStr,
                    onValueChange = { openBalStr = it },
                    label = { Text(if (isBangla) "স্টেটমেন্টের প্রারম্ভিক ব্যালেন্স (BDT)" else "Statement Opening Balance (BDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = closeBalStr,
                    onValueChange = { closeBalStr = it },
                    label = { Text(if (isBangla) "স্টেটমেন্টের সমাপনী ব্যালেন্স (BDT)" else "Statement Closing Balance (BDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text(if (isBangla) "বাতিল" else "Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val openBd = BigDecimal(openBalStr.trim())
                                val closeBd = BigDecimal(closeBalStr.trim())
                                val openMinor = openBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                                val closeMinor = closeBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()

                                val now = System.currentTimeMillis()
                                val monthStart = now - (30L * 86400000L)
                                onConfirm(selectedAccId, monthStart, now, openMinor, closeMinor, notes.ifBlank { null })
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: if (isBangla) "সমন্বয়ের ব্যালেন্স সঠিক নয়" else "Invalid reconciliation balance"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                    ) {
                        Text(if (isBangla) "সমন্বয় সেশন তৈরি" else "Create Session")
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateBackupDialog(
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (notes: String?) -> Unit
) {
    var notes by rememberSaveable { mutableStateOf("Manual offline archive snapshot") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(18.dp)
            ) {
                Text(if (isBangla) "অফলাইন নিরাপত্তা ব্যাকআপ তৈরি" else "Create Offline Safety Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (isBangla) "এটি advance_erp.db-এর একটি অপরিবর্তনীয় স্থানীয় কপি তৈরি করে, SQLite WAL journal সংরক্ষণ করে এবং SHA-256 checksum রাখে।" else "This creates an immutable copy of advance_erp.db on local storage, flushes active SQLite WAL journals, and records a SHA-256 integrity checksum.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isBangla) "ব্যাকআপ নোট" else "Backup Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text(if (isBangla) "বাতিল" else "Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(notes)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text(if (isBangla) "এখনই সংরক্ষণ" else "Archive Now")
                    }
                }
            }
        }
    }
}

@Composable
private fun NewJournalEntryDialog(
    accounts: List<ChartOfAccount>,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (descEn: String, descBn: String, lines: List<JournalLineDraft>) -> Unit
) {
    var descEn by rememberSaveable { mutableStateOf("Manual Adjustment Entry") }
    var descBn by rememberSaveable { mutableStateOf("") }
    var debitAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var creditAccountId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: "") }
    var amountStr by rememberSaveable { mutableStateOf("10000.00") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(18.dp)
            ) {
                Text(if (isBangla) "নতুন ডাবল-এন্ট্রি জাবেদা" else "New Double-Entry Journal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = descEn,
                    onValueChange = { descEn = it },
                    label = { Text(if (isBangla) "বিবরণ (ইংরেজি)" else "Description (English)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = descBn,
                    onValueChange = { descBn = it },
                    label = { Text(if (isBangla) "বিবরণ (বাংলা)" else "Description (Bangla)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(if (isBangla) "ডেবিট ও ক্রেডিট হিসাব:" else "Debit & Credit Accounts:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(6.dp))
                AccountDropdownField(
                    label = if (isBangla) "ডেবিট হিসাব (Dr)" else "Debit Account (Dr)",
                    isBangla = isBangla,
                    accounts = accounts,
                    selectedAccountId = debitAccountId,
                    onSelect = { debitAccountId = it.id }
                )

                Spacer(modifier = Modifier.height(8.dp))

                AccountDropdownField(
                    label = if (isBangla) "ক্রেডিট হিসাব (Cr)" else "Credit Account (Cr)",
                    isBangla = isBangla,
                    accounts = accounts,
                    selectedAccountId = creditAccountId,
                    onSelect = { creditAccountId = it.id }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (isBangla) "পরিমাণ (BDT)" else "Amount (BDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text(if (isBangla) "বাতিল" else "Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val amtBd = BigDecimal(amountStr.trim())
                                require(amtBd > BigDecimal.ZERO) { if (isBangla) "পরিমাণ শূন্যের বেশি হতে হবে" else "Amount must be positive" }
                                require(debitAccountId != creditAccountId) { if (isBangla) "ডেবিট ও ক্রেডিট হিসাব আলাদা হতে হবে" else "Debit and Credit accounts must be different" }

                                val minor = amtBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                                val lines = listOf(
                                    JournalLineDraft(accountId = debitAccountId, isDebit = true, amountMinor = minor),
                                    JournalLineDraft(accountId = creditAccountId, isDebit = false, amountMinor = minor)
                                )
                                onConfirm(descEn, descBn.ifBlank { descEn }, lines)
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: if (isBangla) "জাবেদার তথ্য সঠিক নয়" else "Invalid journal parameters"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text(if (isBangla) "দাখিলা পোস্ট করুন" else "Post Entry")
                    }
                }
            }
        }
    }
}
