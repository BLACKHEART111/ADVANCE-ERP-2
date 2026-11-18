package com.pocketdev.advance_erp.feature.controlcenter

import com.pocketdev.advance_erp.feature.controlcenter.components.PartyLedgerPanel
import com.pocketdev.advance_erp.feature.controlcenter.components.PartyLedgerRow
import com.pocketdev.advance_erp.feature.sales.RecordCustomerPaymentDialog
import com.pocketdev.advance_erp.feature.imports.RecordSupplierPaymentDialog
import com.pocketdev.advance_erp.core.security.rememberBiometricCostVisibility
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.domain.model.FinancialMetricFilter
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPEmptyState
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.security.SecurityManager
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.domain.model.ControlCenterFilterState
import com.pocketdev.advance_erp.domain.model.DateRangeOption
import com.pocketdev.advance_erp.domain.model.ReportCategory
import com.pocketdev.advance_erp.domain.model.UnifiedTransactionItem
import com.pocketdev.advance_erp.feature.controlcenter.components.ControlCenterFilterDialog
import com.pocketdev.advance_erp.feature.controlcenter.components.ControlCenterSummaryCards
import com.pocketdev.advance_erp.feature.controlcenter.components.ControlCenterTransactionDetailDialog
import com.pocketdev.advance_erp.feature.controlcenter.components.CustomDateRangeDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessControlCenterScreen(
    modifier: Modifier = Modifier,
    viewModel: BusinessControlCenterViewModel = hiltViewModel(),
    initialCategory: ReportCategory? = null
) {
    val context = LocalContext.current
    val filterState by viewModel.filterState.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val detailLoading by viewModel.detailLoading.collectAsState()
    val report by viewModel.report.collectAsState()
    val ready = !report.isLoading && report.error == null && report.filter == filterState
    val transactions = if (ready) report.transactions else emptyList()
    val summary = report.summary
    val paymentInvoices by viewModel.paymentInvoices.collectAsState()
    val paymentOrders by viewModel.paymentOrders.collectAsState()
    val paymentSaving by viewModel.paymentSaving.collectAsState()
    val paymentError by viewModel.paymentError.collectAsState()
    var showPayment by rememberSaveable { mutableStateOf(false) }
    var visibleCount by remember(filterState) { mutableStateOf(100) }
    val ledgerMode = filterState.category in setOf(ReportCategory.CUSTOMER_LEDGER, ReportCategory.SUPPLIER_LEDGER)
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()
    val savedPresets by viewModel.savedPresets.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val currencyCode = CurrencyCode.BDT
    val selectedDetail by viewModel.selectedTransactionDetail.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    val isBn = language == AppLanguage.BANGLA
    val profitVisibility = rememberBiometricCostVisibility("ledger-profit", SecurityManager.canViewProfit(userRole), isBn)
    val canViewProfit = SecurityManager.canViewProfit(userRole) && profitVisibility.isVisible
    val stockVisibility = rememberBiometricCostVisibility("ledger-stock", SecurityManager.canViewCost(userRole), isBn)
    val exportVisibility = rememberBiometricCostVisibility("ledger-export", SecurityManager.canViewCost(userRole), isBn)
    val profitFilterLocked = filterState.financialMetric in setOf(FinancialMetricFilter.PROFIT, FinancialMetricFilter.LOSS) && !canViewProfit

    var showFilterDialog by remember { mutableStateOf(false) }
    var showCustomDateRangeDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showPresetsMenu by remember { mutableStateOf(false) }

    LaunchedEffect(initialCategory) {
        if (initialCategory != null) viewModel.selectCategory(initialCategory)
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    fun formatCustomDate(millis: Long?): String {
        if (millis == null) return if (isBn) "নির্বাচন" else "Select"
        val formatted = dateFormat.format(Date(millis))
        return if (isBn) Formatters.toBanglaDigits(formatted) else formatted
    }

    // Helper for Custom Date Picker - End Date
    fun openEndDatePicker(startDateMillis: Long?) {
        val cal = Calendar.getInstance()
        val currentEnd = filterState.customEndDate ?: System.currentTimeMillis()
        val initialEnd = if (startDateMillis != null && currentEnd < startDateMillis) startDateMillis else currentEnd
        cal.timeInMillis = initialEnd

        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val endCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val start = startDateMillis ?: filterState.customStartDate ?: (endCal.timeInMillis - 7L * 86400000L)
                viewModel.setCustomDateRange(start, endCal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.setTitle(if (isBn) "শেষের তারিখ নির্বাচন করুন (End Date)" else "Select End Date")
        if (startDateMillis != null) {
            datePicker.datePicker.minDate = startDateMillis
        }
        datePicker.show()
    }

    // Helper for Custom Date Picker - Start Date
    fun openCustomDatePicker(isStart: Boolean, chainToEnd: Boolean = false) {
        if (!isStart) {
            openEndDatePicker(filterState.customStartDate)
            return
        }
        val cal = Calendar.getInstance()
        val currentMillis = filterState.customStartDate ?: (System.currentTimeMillis() - 7L * 86400000L)
        cal.timeInMillis = currentMillis

        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val selectedStart = selectedCal.timeInMillis
                if (chainToEnd) {
                    openEndDatePicker(selectedStart)
                } else {
                    viewModel.setCustomStartDate(selectedStart)
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.setTitle(if (isBn) "শুরুর তারিখ নির্বাচন করুন (Start Date)" else "Select Start Date")
        datePicker.show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ERPBackground)
    ) {
        // Compact Top Action Strip (Replaces tall duplicate title block)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Subtitle / Scope indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryPurple.copy(alpha = 0.12f),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (isBn) "কেন্দ্রীয় লেজার ও স্টেটমেন্ট" else "Central Ledger & Reports",
                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = if (isBn) "রিয়েলটাইম ফিল্টারিং ও এক্সপোর্ট" else "Real-time filters & export",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    // Top Action buttons: Presets Menu + Export Button (Fixed height, no vertical stretch)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(2.dp))
                        }

                        // Presets Menu Button
                        Box {
                            IconButton(
                                onClick = { showPresetsMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Saved Presets",
                                    tint = if (savedPresets.isNotEmpty()) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
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
                                            text = {
                                                Column {
                                                    Text(preset.name, fontWeight = FontWeight.Bold)
                                                    Text(preset.filterState.category.titleEn, style = MaterialTheme.typography.labelSmall)
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

                        // Export & Share Button (Compact & Sturdy)
                        Button(
                            onClick = { if (ready) showExportSheet = true },
                            enabled = ready && !isExporting && !profitFilterLocked,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple, contentColor = Color.White)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBn) "এক্সপোর্ট" else "Export", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 5.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // Date Range Horizontal Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBn) "তারিখ (Date Range):" else "Date Range:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (filterState.dateRangeOption == DateRangeOption.CUSTOM) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Start Date Pill (Clickable directly for Start Date)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PrimaryPurple.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.6f)),
                                modifier = Modifier.clickable { openCustomDatePicker(isStart = true, chainToEnd = false) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = PrimaryPurple
                                    )
                                    Text(
                                        text = "${if (isBn) "শুরু:" else "Start:"} ${formatCustomDate(filterState.customStartDate)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = PrimaryPurple,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "→",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // End Date Pill (Clickable directly for End Date)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SecondaryTeal.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.6f)),
                                modifier = Modifier.clickable { openCustomDatePicker(isStart = false, chainToEnd = false) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = SecondaryTeal
                                    )
                                    Text(
                                        text = "${if (isBn) "শেষ:" else "End:"} ${formatCustomDate(filterState.customEndDate)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = SecondaryTeal,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Edit Icon Button to open CustomDateRangeDialog
                            IconButton(
                                onClick = { showCustomDateRangeDialog = true },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = "Edit Date Range",
                                    modifier = Modifier.size(13.dp),
                                    tint = PrimaryPurple
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(DateRangeOption.entries.toTypedArray()) { option ->
                        val isSelected = filterState.dateRangeOption == option
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (option == DateRangeOption.CUSTOM) {
                                    showCustomDateRangeDialog = true
                                } else {
                                    viewModel.selectDateRange(option)
                                }
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color.White
                                    )
                                }
                            } else if (option == DateRangeOption.CUSTOM) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = PrimaryPurple
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = if (isBn) option.titleBn else option.titleEn,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = PrimaryPurple,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                selectedBorderColor = PrimaryPurple,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.5.dp
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Category Filter Horizontal Row
                Text(
                    text = if (isBn) "ক্যাটাগরি (Report Category):" else "Report Category:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(ReportCategory.entries.toTypedArray()) { cat ->
                        val isSelected = filterState.category == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(cat) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color.White
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = if (isBn) cat.titleBn else cat.titleEn,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = SecondaryTeal,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                selectedBorderColor = SecondaryTeal,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.5.dp
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // Main Scrollable Content (Compact Spacing & High Information Density)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Business Summary Dashboard Cards (Collapsible / Expandable)
            if (ledgerMode) item {
                PartyLedgerPanel(report, filterState, customers, suppliers, language, ready && !profitFilterLocked,
                    onParty = viewModel::selectParty,
                    onPayment = { viewModel.clearPaymentError(); showPayment = true },
                    onShare = { viewModel.shareReminder(context, profitVisibility.isVisible) })
            }
            if (!ledgerMode && ready && !profitFilterLocked) {
                item {
                    ControlCenterSummaryCards(
                        summary = summary,
                        canViewProfit = canViewProfit,
                        language = language,
                        currencyCode = currencyCode,
                        onCategoryClick = { viewModel.selectCategory(it) }
                    )
                }
            }
            if (SecurityManager.canViewProfit(userRole)) item {
                androidx.compose.material3.TextButton(onClick = profitVisibility.toggle) {
                    Text(if (isBn) { if(canViewProfit) "লাভের তথ্য লুকান" else "লাভ দেখতে fingerprint যাচাই" } else if(canViewProfit) "Hide profit" else "Verify biometric to view profit")
                }
            }
            if (!ready) item {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    if (report.error != null) {
                        Text(report.error ?: "Ledger error", color = MaterialTheme.colorScheme.error)
                        androidx.compose.material3.TextButton(onClick=viewModel::retry) { Text(if(isBn) "আবার চেষ্টা" else "Retry") }
                    } else { CircularProgressIndicator(); Text(if(isBn) "হিসাব লোড হচ্ছে…" else "Loading ledger…") }
                }
            }
            if (profitFilterLocked) item {
                Text(if(isBn) "এই filter-এর ফল দেখতে লাভের biometric যাচাই করুন।" else "Authenticate profit access to view this filter.")
            }

            // 2. Compact Search & Filter Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = filterState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text(if (isBn) "ইনভয়েস, পিও, পার্টি বা পণ্য খুঁজুন..." else "Search ref, party, items...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (filterState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    )

                    if (showPayment && report.statement != null) {
        val statement = report.statement!!
        if (statement.partyType == "CUSTOMER") RecordCustomerPaymentDialog(
            customers=customers.filter { it.id==statement.partyId },
            invoices=paymentInvoices.filter { it.customerId==statement.partyId && it.isPosted && it.status!="CANCELLED" },
            preselectedCustomerId=statement.partyId, isBangla=isBn, isSaving=paymentSaving, saveError=paymentError,
            onDismiss={if(!viewModel.paymentSaving.value) showPayment=false},
            onSubmit={draft -> viewModel.collectPayment(draft){showPayment=false}}
        ) else RecordSupplierPaymentDialog(
            suppliers=suppliers.filter { it.id==statement.partyId },
            orders=paymentOrders.filter { it.supplierId==statement.partyId && it.status !in setOf("DRAFT","CANCELLED") },
            preselectedSupplierId=statement.partyId, dismissOnSubmit=false, isSaving=paymentSaving, saveError=paymentError,
            onDismiss={if(!viewModel.paymentSaving.value) showPayment=false},
            onConfirm={supplierId,orderId,amount,currency,rate,method,account,reference,notes ->
                viewModel.paySupplier(supplierId,orderId,amount,currency,rate,method,account,reference,notes){showPayment=false}
            }
        )
    }

    // Filter Dialog Trigger Button with Active Count Badge
                    BadgedBox(
                        badge = {
                            if (filterState.activeFilterCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFDC2626),
                                    contentColor = Color.White
                                ) {
                                    Text(filterState.activeFilterCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (filterState.activeFilterCount > 0) PrimaryPurple.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (filterState.activeFilterCount > 0) PrimaryPurple else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .size(46.dp)
                                .clickable { showFilterDialog = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filters",
                                    tint = if (filterState.activeFilterCount > 0) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Reset Filters Button (if active)
                    if (filterState.activeFilterCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier
                                .size(46.dp)
                                .clickable { viewModel.resetFilters() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            if(detailLoading) item { androidx.compose.material3.LinearProgressIndicator(modifier=Modifier.fillMaxWidth()) }
            actionError?.let { message -> item {
                Text(message,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
                androidx.compose.material3.TextButton(onClick=viewModel::dismissActionError) { Text(if(isBn) "বন্ধ করুন" else "Dismiss") }
            } }
            // 3. Transactions Count & Active Filter Indicator
            if (!profitFilterLocked) item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isBn) "লেনদেন তালিকা (${transactions.size} টি রেকর্ড)" else "Ledger Entries (${transactions.size} Records)",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val totalAmt = if (stockVisibility.isVisible) transactions.sumOf { it.amountMinor } else transactions.filterNot { it.rawType in setOf("STOCK_MOVEMENT", "STOCK_TRANSFER") }.sumOf { it.amountMinor }
                    Text(
                        text = "${if (isBn) "নির্বাচিত লেনদেনের অঙ্ক: " else "Selected amounts: "} ${Formatters.formatMoney(totalAmt, currencyCode, language)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                }
            }

            // 4. Empty State
            if (ready && !profitFilterLocked && transactions.isEmpty()) {
                item {
                    ERPEmptyState(
                        icon = Icons.Default.FilterList,
                        title = if (isBn) "কোনো তথ্য পাওয়া যায়নি" else "No Records Found",
                        message = if (isBn) "নির্বাচিত তারিখ ও ফিল্টার অনুযায়ী কোনো লেনদেন নেই।" else "Try adjusting the date range or clear some filters.",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                // 5. Transaction Cards (Compact & Readable)
                if (!profitFilterLocked) items(transactions.take(visibleCount), key = { it.rawType + ":" + it.id }) { item ->
                    if (report.statement != null) PartyLedgerRow(item, language) {
                        viewModel.openTransactionDetail(item.rawType, item.sourceId)
                    } else if (item.rawType in setOf("STOCK_MOVEMENT", "STOCK_TRANSFER") && !stockVisibility.isVisible) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(item.referenceNumber, fontWeight=FontWeight.SemiBold)
                            androidx.compose.material3.TextButton(onClick=stockVisibility.toggle, enabled=SecurityManager.canViewCost(userRole)) {
                                Text(if(isBn) "খরচ দেখতে fingerprint যাচাই" else "Verify biometric to view cost")
                            }
                        }
                    } else UnifiedTransactionCard(item, language, currencyCode, dateFormat,
                        onClick = { viewModel.openTransactionDetail(item.rawType, item.sourceId) })
                }
                if (!profitFilterLocked && transactions.size > visibleCount) item {
                    androidx.compose.material3.OutlinedButton(onClick={visibleCount+=100},modifier=Modifier.fillMaxWidth()) {
                        Text(if(isBn) "আরও দেখুন ($visibleCount/${transactions.size})" else "Load more ($visibleCount/${transactions.size})")
                    }
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        ControlCenterFilterDialog(
            currentFilter = filterState,
            canViewProfit = canViewProfit,
            customers = customers,
            suppliers = suppliers,
            warehouses = warehouses,
            language = language,
            onApply = { viewModel.updateFilter(it) },
            onReset = { viewModel.resetFilters() },
            onSavePreset = { viewModel.savePreset(it) },
            onDismiss = { showFilterDialog = false }
        )
    }

    // Custom Date Range Dialog
    if (showCustomDateRangeDialog) {
        CustomDateRangeDialog(
            initialStartDate = filterState.customStartDate,
            initialEndDate = filterState.customEndDate,
            language = language,
            onApply = { start, end ->
                viewModel.setCustomDateRange(start, end)
            },
            onDismiss = { showCustomDateRangeDialog = false }
        )
    }

    // Detail Dialog
    selectedDetail?.let { detail ->
        ControlCenterTransactionDetailDialog(
            record = detail,
            language = language,
            currencyCode = currencyCode,
            onLinkedDocument = { type, id -> viewModel.openTransactionDetail(type,id) },
            onDismiss = { viewModel.dismissTransactionDetail() }
        )
    }

    // Export Options Modal Bottom Sheet
    if (showExportSheet) {
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
                    text = if (isBn) "রিপোর্ট এক্সপোর্ট ও শেয়ারিং অপশন" else "Export & Share Business Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isBn) "পিডিএফ, এক্সেল বা সিএসভি ফরম্যাটে ডাউনলোড ও শেয়ার করুন" else "Generate professional PDF & XLSX workbooks directly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                if (SecurityManager.canViewCost(userRole)) {
                    androidx.compose.material3.TextButton(onClick=exportVisibility.toggle) {
                        Text(if(isBn) { if(exportVisibility.isVisible) "Cost/profit export বন্ধ করুন" else "Cost/profit সহ export: fingerprint যাচাই" }
                            else if(exportVisibility.isVisible) "Exclude cost/profit" else "Authenticate to include cost/profit")
                    }
                }
                // 1. Share PDF
                ExportActionItem(
                    title = if (isBn) "পিডিএফ রিপোর্ট শেয়ার করুন (PDF Report)" else "Share PDF Document",
                    subtitle = if (isBn) "WhatsApp, Gmail বা অন্য অ্যাপে শেয়ার করুন" else "A4 printable report with header & summary",
                    icon = Icons.Default.Share,
                    iconColor = Color(0xFFDC2626),
                    onClick = {
                        showExportSheet = false
                        viewModel.exportAndSharePdf(context, exportVisibility.isVisible)
                    }
                )

                // 2. Share Excel XLSX
                ExportActionItem(
                    title = if (isBn) "এক্সেল ওয়ার্কবুক শেয়ার করুন (Excel XLSX)" else "Share Excel (XLSX) Workbook",
                    subtitle = if (isBn) "ক্যাটাগরিভিত্তিক আলাদা শীট ও বিশ্লেষণ" else "Multi-sheet spreadsheet with summary & ledgers",
                    icon = Icons.Default.TableChart,
                    iconColor = Color(0xFF16A34A),
                    onClick = {
                        showExportSheet = false
                        viewModel.exportAndShareExcel(context, exportVisibility.isVisible)
                    }
                )

                // 3. Share CSV
                ExportActionItem(
                    title = if (isBn) "সিএসভি ফাইল শেয়ার করুন (CSV Export)" else "Share CSV Data File",
                    subtitle = if (isBn) "সহজ ডাটা এনালাইসিসের জন্য" else "Plain comma-separated transaction records",
                    icon = Icons.Default.Share,
                    iconColor = Color(0xFF0284C7),
                    onClick = {
                        showExportSheet = false
                        viewModel.exportAndShareCsv(context, exportVisibility.isVisible)
                    }
                )

                // 4. Print Report
                ExportActionItem(
                    title = if (isBn) "সরাসরি প্রিন্ট করুন (Print Report)" else "Print Report Directly",
                    subtitle = if (isBn) "Android PrintManager ব্যবহার করে প্রিন্ট" else "Send formatted document to WiFi or Bluetooth printer",
                    icon = Icons.Default.Print,
                    iconColor = Color(0xFF7C3AED),
                    onClick = {
                        showExportSheet = false
                        viewModel.printReport(context, exportVisibility.isVisible)
                    }
                )

                // 5. Save to Downloads
                ExportActionItem(
                    title = if (isBn) "ডিভাইসে ডাউনলোড করুন (Save to Downloads)" else "Save to Downloads Folder",
                    subtitle = if (isBn) "মোবাইলের ইন্টারনাল স্টোরেজে সংরক্ষণ" else "Save PDF & XLSX directly to device storage",
                    icon = Icons.Default.Download,
                    iconColor = Color(0xFFD97706),
                    onClick = {
                        showExportSheet = false
                        viewModel.savePdfToStorage(context, exportVisibility.isVisible)
                        viewModel.saveExcelToStorage(context, exportVisibility.isVisible)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row: Reference & Date & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = item.referenceNumber,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(item.statusColorHex.toInt()).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = item.status,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(item.statusColorHex.toInt()),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Text(
                    text = dateFormat.format(Date(item.date)),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Entity Name & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.primaryEntityName,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = item.category.titleEn,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            // Particulars
            if (item.particulars.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.particulars,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = listOfNotNull(item.paymentMethod,item.warehouseName).joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            val sign = if (item.effectSign < 0) "↶ " else if (item.isInflow) "+" else "−"
            Text(
                text = "$sign${Formatters.formatMoney(item.amountMinor, currencyCode, language)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                maxLines = 1, softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                color = if (item.isInflow) Color(0xFF16A34A) else Color(0xFFDC2626)
            )
        }
    }
}

@Composable
private fun ExportActionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(34.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(7.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
