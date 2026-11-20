package com.pocketdev.advance_erp.feature.controlcenter.components

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusInfo
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.model.AmountRangeFilter
import com.pocketdev.advance_erp.domain.model.ControlCenterFilterState
import com.pocketdev.advance_erp.domain.model.DateRangeOption
import com.pocketdev.advance_erp.domain.model.DeliveryReturnFilter
import com.pocketdev.advance_erp.domain.model.FinancialMetricFilter
import com.pocketdev.advance_erp.domain.model.PaymentMethodFilter
import com.pocketdev.advance_erp.domain.model.PaymentStatusFilter
import com.pocketdev.advance_erp.domain.model.ReportCategory
import com.pocketdev.advance_erp.domain.model.SaleChannelFilter
import com.pocketdev.advance_erp.domain.model.SortOption
import com.pocketdev.advance_erp.domain.model.TransactionDirectionFilter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class FilterAccordionSection {
    NONE,
    CUSTOMER,
    SUPPLIER,
    PRODUCT_CATEGORY,
    PAYMENT_METHOD,
    PAYMENT_STATUS,
    TRANSACTION_TYPE,
    SALE_CHANNEL,
    WAREHOUSE,
    AMOUNT_RANGE,
    SORT_BY
}

@Composable
fun ControlCenterFilterDialog(
    currentFilter: ControlCenterFilterState,
    canViewProfit: Boolean = false,
    customers: List<Customer>,
    suppliers: List<Supplier>,
    warehouses: List<Warehouse>,
    language: AppLanguage,
    onApply: (ControlCenterFilterState) -> Unit,
    onReset: () -> Unit,
    onSavePreset: (String) -> Unit,
    onDismiss: () -> Unit,
    categories: List<ProductCategory> = emptyList(),
    products: List<Product> = emptyList()
) {
    ControlCenterFilterDrawer(
        currentFilter = currentFilter,
        canViewProfit = canViewProfit,
        customers = customers,
        suppliers = suppliers,
        warehouses = warehouses,
        categories = categories,
        products = products,
        language = language,
        onApply = onApply,
        onReset = onReset,
        onSavePreset = onSavePreset,
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlCenterFilterDrawer(
    currentFilter: ControlCenterFilterState,
    canViewProfit: Boolean = false,
    customers: List<Customer>,
    suppliers: List<Supplier>,
    warehouses: List<Warehouse>,
    categories: List<ProductCategory>,
    products: List<Product>,
    language: AppLanguage,
    onApply: (ControlCenterFilterState) -> Unit,
    onReset: () -> Unit,
    onSavePreset: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var dateRangeOption by remember(currentFilter) { mutableStateOf(currentFilter.dateRangeOption) }
    var customStartDate by remember(currentFilter) { mutableStateOf(currentFilter.customStartDate) }
    var customEndDate by remember(currentFilter) { mutableStateOf(currentFilter.customEndDate) }
    var category by remember(currentFilter) { mutableStateOf(currentFilter.category) }

    var selectedCustomerId by remember(currentFilter) { mutableStateOf(currentFilter.selectedCustomerId) }
    var selectedSupplierId by remember(currentFilter) { mutableStateOf(currentFilter.selectedSupplierId) }
    var selectedCategoryId by remember(currentFilter) { mutableStateOf(currentFilter.selectedCategoryId) }
    var selectedProductId by remember(currentFilter) { mutableStateOf(currentFilter.selectedProductId) }
    var selectedWarehouseId by remember(currentFilter) { mutableStateOf(currentFilter.selectedWarehouseId) }
    var paymentMethodFilter by remember(currentFilter) { mutableStateOf(currentFilter.paymentMethodFilter) }
    var paymentStatus by remember(currentFilter) { mutableStateOf(currentFilter.paymentStatus) }
    var transactionDirection by remember(currentFilter) { mutableStateOf(currentFilter.transactionDirection) }
    var saleChannel by remember(currentFilter) { mutableStateOf(currentFilter.saleChannel) }
    var deliveryReturn by remember(currentFilter) { mutableStateOf(currentFilter.deliveryReturn) }
    var financialMetric by remember(currentFilter) { mutableStateOf(currentFilter.financialMetric) }
    var amountFilter by remember(currentFilter) { mutableStateOf(currentFilter.amountFilter) }
    var minAmountText by remember(currentFilter) { mutableStateOf(currentFilter.customMinAmountMinor?.let { (it / 100).toString() } ?: "") }
    var maxAmountText by remember(currentFilter) { mutableStateOf(currentFilter.customMaxAmountMinor?.let { (it / 100).toString() } ?: "") }
    var sortBy by remember(currentFilter) { mutableStateOf(currentFilter.sortBy) }

    var showDateDropdown by remember { mutableStateOf(false) }
    var activeSection by remember { mutableStateOf(FilterAccordionSection.NONE) }
    var showSavePresetField by rememberSaveable { mutableStateOf(false) }
    var presetName by rememberSaveable { mutableStateOf("") }

    var drawerVisible by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        drawerVisible = true
    }

    fun closeDrawerAnimated() {
        scope.launch {
            drawerVisible = false
            delay(200)
            onDismiss()
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }
    fun formatFilterDate(millis: Long?): String {
        if (millis == null) return if (isBn) "তারিখ নির্বাচন" else "Select Date"
        val formatted = dateFormat.format(Date(millis))
        return if (isBn) Formatters.toBanglaDigits(formatted) else formatted
    }

    fun openStartDatePicker() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = customStartDate ?: (System.currentTimeMillis() - 7L * 86400000L)
        }
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val newStart = selectedCal.timeInMillis
                customStartDate = newStart
                if (customEndDate != null && customEndDate!! < newStart) {
                    customEndDate = newStart
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.setTitle(if (isBn) "শুরুর তারিখ নির্বাচন করুন" else "Select Start Date")
        datePicker.show()
    }

    fun openEndDatePicker() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = customEndDate ?: System.currentTimeMillis()
        }
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val newEnd = selectedCal.timeInMillis
                customEndDate = newEnd
                if (dateRangeOption == DateRangeOption.CUSTOM && customStartDate != null && customStartDate!! > newEnd) {
                    customStartDate = newEnd
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.setTitle(if (isBn) "শেষের তারিখ নির্বাচন করুন" else "Select End Date")
        if (dateRangeOption == DateRangeOption.CUSTOM && customStartDate != null) {
            datePicker.datePicker.minDate = customStartDate!!
        }
        datePicker.show()
    }

    fun buildAppliedFilter(): ControlCenterFilterState {
        val minVal = minAmountText.toLongOrNull()?.let { it * 100 }
        val maxVal = maxAmountText.toLongOrNull()?.let { it * 100 }
        val resolvedAmountFilter = if ((minVal != null || maxVal != null) && amountFilter == AmountRangeFilter.ALL) {
            AmountRangeFilter.CUSTOM
        } else {
            amountFilter
        }
        return currentFilter.copy(
            dateRangeOption = dateRangeOption,
            customStartDate = if (dateRangeOption == DateRangeOption.CUSTOM) customStartDate else null,
            customEndDate = if (dateRangeOption in setOf(DateRangeOption.CUSTOM, DateRangeOption.INCEPTION_TO_DATE)) customEndDate else null,
            category = category,
            selectedCustomerId = selectedCustomerId,
            selectedSupplierId = selectedSupplierId,
            selectedCategoryId = selectedCategoryId,
            selectedProductId = selectedProductId,
            selectedWarehouseId = selectedWarehouseId,
            paymentMethodFilter = paymentMethodFilter,
            paymentStatus = paymentStatus,
            transactionDirection = transactionDirection,
            saleChannel = saleChannel,
            deliveryReturn = deliveryReturn,
            financialMetric = financialMetric,
            amountFilter = resolvedAmountFilter,
            customMinAmountMinor = minVal,
            customMaxAmountMinor = maxVal,
            sortBy = sortBy
        )
    }

    Dialog(
        onDismissRequest = { closeDrawerAnimated() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
    ) {
        BackHandler { closeDrawerAnimated() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("filter_drawer_backdrop")
        ) {
            // Dimmed Scrim
            AnimatedVisibility(
                visible = drawerVisible,
                enter = fadeIn(animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(180))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { closeDrawerAnimated() }
                )
            }

            // Right-Side Sliding Drawer (88% screen width, rounded left corners)
            AnimatedVisibility(
                visible = drawerVisible,
                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(250)),
                exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(200)),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .fillMaxHeight()
                        .padding(bottom = 72.dp)
                        .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (dragOffsetX > 110f) {
                                        closeDrawerAnimated()
                                    } else {
                                        dragOffsetX = 0f
                                    }
                                },
                                onDragCancel = { dragOffsetX = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    val next = (dragOffsetX + dragAmount).coerceAtLeast(0f)
                                    dragOffsetX = next
                                }
                            )
                        }
                        .testTag("filter_drawer_panel"),
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                    ) {
                        // Top Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.FilterAlt,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBn) "ফিল্টার ও অনুসন্ধান" else "Filter & Search",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { closeDrawerAnimated() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("filter_drawer_close")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Close Filter Drawer",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        HorizontalDivider(color = ERPDivider.copy(alpha = 0.6f))

                        // Scrollable Filter Body
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. Date Range Section
                            Text(
                                text = if (isBn) "তারিখ (Date Range)" else "Date Range",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Box {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showDateDropdown = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, ERPDivider)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isBn) dateRangeOption.titleBn else dateRangeOption.titleEn,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = TextSecondary)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showDateDropdown,
                                    onDismissRequest = { showDateDropdown = false }
                                ) {
                                    DateRangeOption.entries.forEach { opt ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (isBn) opt.titleBn else opt.titleEn,
                                                    fontWeight = if (dateRangeOption == opt) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (dateRangeOption == opt) PrimaryPurple else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                dateRangeOption = opt
                                                if (opt == DateRangeOption.CUSTOM && customStartDate == null) {
                                                    val now = System.currentTimeMillis()
                                                    customStartDate = now - 7L * 86400000L
                                                    customEndDate = now
                                                } else if (opt == DateRangeOption.INCEPTION_TO_DATE && customEndDate == null) {
                                                    customEndDate = System.currentTimeMillis()
                                                }
                                                showDateDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Quick Date Range Chips (compact horizontal row)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val quickDates = listOf(
                                    DateRangeOption.ALL_TIME,
                                    DateRangeOption.TODAY,
                                    DateRangeOption.YESTERDAY,
                                    DateRangeOption.LAST_7_DAYS,
                                    DateRangeOption.LAST_30_DAYS,
                                    DateRangeOption.THIS_MONTH,
                                    DateRangeOption.LAST_MONTH,
                                    DateRangeOption.INCEPTION_TO_DATE,
                                    DateRangeOption.CUSTOM
                                )
                                quickDates.forEach { opt ->
                                    DrawerChoiceChip(
                                        selected = dateRangeOption == opt,
                                        label = if (isBn) opt.titleBn else opt.titleEn,
                                        selectedColor = PrimaryPurple,
                                        onClick = {
                                            dateRangeOption = opt
                                            if (opt == DateRangeOption.CUSTOM && customStartDate == null) {
                                                val now = System.currentTimeMillis()
                                                customStartDate = now - 7L * 86400000L
                                                customEndDate = now
                                            } else if (opt == DateRangeOption.INCEPTION_TO_DATE && customEndDate == null) {
                                                customEndDate = System.currentTimeMillis()
                                            }
                                        }
                                    )
                                }
                            }

                            if (dateRangeOption == DateRangeOption.CUSTOM || dateRangeOption == DateRangeOption.INCEPTION_TO_DATE) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (dateRangeOption == DateRangeOption.CUSTOM) {
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { openStartDatePicker() },
                                            shape = RoundedCornerShape(10.dp),
                                            color = PrimaryPurple.copy(alpha = 0.08f),
                                            border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.45f))
                                        ) {
                                            Column(modifier = Modifier.padding(9.dp)) {
                                                Text(
                                                    text = if (isBn) "শুরুর তারিখ" else "Start Date",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PrimaryPurple,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = formatFilterDate(customStartDate),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ) {
                                            Column(modifier = Modifier.padding(9.dp)) {
                                                Text(
                                                    text = if (isBn) "শুরু" else "From",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = if (isBn) "ব্যবসা শুরুর দিন" else "Business Start",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { openEndDatePicker() },
                                        shape = RoundedCornerShape(10.dp),
                                        color = SecondaryTeal.copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.45f))
                                    ) {
                                        Column(modifier = Modifier.padding(9.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Outlined.Event, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isBn) "নির্বাচিত শেষ তারিখ" else "Selected End Date",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SecondaryTeal,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = formatFilterDate(customEndDate),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Transaction Category Grid (2-column tiles matching reference image)
                            Text(
                                text = if (isBn) "লেনদেনের ধরন" else "Transaction Category",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            val drawerCategories = listOf(
                                Triple(ReportCategory.ALL_TRANSACTIONS, if (isBn) "সকল" else "All", Icons.Outlined.Check),
                                Triple(ReportCategory.SALES, if (isBn) "বিক্রয়" else "Sales", Icons.Outlined.ShoppingCart),
                                Triple(ReportCategory.PURCHASE, if (isBn) "ক্রয়" else "Purchases", Icons.Outlined.Inventory2),
                                Triple(ReportCategory.IMPORT, if (isBn) "আমদানি" else "Import", Icons.Outlined.LocalShipping),
                                Triple(ReportCategory.PAYMENT, if (isBn) "পেমেন্ট" else "Payments", Icons.Outlined.AccountBalanceWallet),
                                Triple(ReportCategory.RECEIPT, if (isBn) "টাকা গ্রহণ" else "Receipts", Icons.Outlined.Payments),
                                Triple(ReportCategory.EXPENSE, if (isBn) "ব্যয়" else "Expenses", Icons.Outlined.MoneyOff),
                                Triple(ReportCategory.SALES_RETURN, if (isBn) "রিটার্ন" else "Returns", Icons.Outlined.Replay),
                                Triple(ReportCategory.STOCK_ADJUSTMENT, if (isBn) "সমন্বয়" else "Adjustments", Icons.Outlined.SwapHoriz)
                            )

                            drawerCategories.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { (catOption, label, icon) ->
                                        val isSel = category == catOption
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { category = catOption },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSel) SecondaryTeal else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(
                                                width = if (isSel) 1.5.dp else 1.dp,
                                                color = if (isSel) SecondaryTeal else ERPDivider
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = if (isSel) Color.White else PrimaryPurple,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            // 3. Advanced Filters (Accordion Rows matching reference image)
                            Text(
                                text = if (isBn) "ক্যাটাগরি / উৎস (উন্নত ফিল্টার)" else "Category / Source (Advanced Filters)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Customer
                            val selectedCustName = customers.firstOrNull { it.id == selectedCustomerId }?.name
                            FilterAccordionItem(
                                title = if (isBn) "গ্রাহক নির্বাচন" else "Customer",
                                selectedSummary = selectedCustName ?: (if (isBn) "সকল গ্রাহক" else "All Customers"),
                                isActive = selectedCustomerId != null,
                                icon = Icons.Outlined.PersonOutline,
                                expanded = activeSection == FilterAccordionSection.CUSTOMER,
                                onToggle = {
                                    activeSection = if (activeSection == FilterAccordionSection.CUSTOMER) FilterAccordionSection.NONE else FilterAccordionSection.CUSTOMER
                                }
                            ) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    DrawerChoiceChip(
                                        selected = selectedCustomerId == null,
                                        label = if (isBn) "সকল গ্রাহক" else "All Customers",
                                        selectedColor = PrimaryPurple,
                                        onClick = { selectedCustomerId = null }
                                    )
                                    customers.forEach { cust ->
                                        DrawerChoiceChip(
                                            selected = selectedCustomerId == cust.id,
                                            label = cust.name,
                                            selectedColor = PrimaryPurple,
                                            onClick = {
                                                selectedCustomerId = cust.id
                                                selectedSupplierId = null
                                            }
                                        )
                                    }
                                }
                            }

                            // Supplier
                            val selectedSuppName = suppliers.firstOrNull { it.id == selectedSupplierId }?.companyName
                            FilterAccordionItem(
                                title = if (isBn) "সরবরাহকারী নির্বাচন" else "Supplier",
                                selectedSummary = selectedSuppName ?: (if (isBn) "সকল সরবরাহকারী" else "All Suppliers"),
                                isActive = selectedSupplierId != null,
                                icon = Icons.Outlined.LocalShipping,
                                expanded = activeSection == FilterAccordionSection.SUPPLIER,
                                onToggle = {
                                    activeSection = if (activeSection == FilterAccordionSection.SUPPLIER) FilterAccordionSection.NONE else FilterAccordionSection.SUPPLIER
                                }
                            ) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    DrawerChoiceChip(
                                        selected = selectedSupplierId == null,
                                        label = if (isBn) "সকল সরবরাহকারী" else "All Suppliers",
                                        selectedColor = PrimaryPurple,
                                        onClick = { selectedSupplierId = null }
                                    )
                                    suppliers.forEach { sup ->
                                        DrawerChoiceChip(
                                            selected = selectedSupplierId == sup.id,
                                            label = sup.companyName,
                                            selectedColor = PrimaryPurple,
                                            onClick = {
                                                selectedSupplierId = sup.id
                                                selectedCustomerId = null
                                            }
                                        )
                                    }
                                }
                            }

                            // Product / Category
                            val selectedCatName = categories.firstOrNull { it.id == selectedCategoryId }?.let { if (isBn) it.nameBn else it.nameEn }
                            val selectedProdName = products.firstOrNull { it.id == selectedProductId }?.let { if (isBn) it.nameBn else it.nameEn }
                            val prodCatSummary = listOfNotNull(selectedCatName, selectedProdName).joinToString(" • ").ifBlank {
                                if (isBn) "সকল পণ্য ও ক্যাটাগরি" else "All Products & Categories"
                            }
                            FilterAccordionItem(
                                title = if (isBn) "পণ্য / ক্যাটাগরি" else "Product / Category",
                                selectedSummary = prodCatSummary,
                                isActive = selectedCategoryId != null || selectedProductId != null,
                                icon = Icons.Outlined.Category,
                                expanded = activeSection == FilterAccordionSection.PRODUCT_CATEGORY,
                                onToggle = {
                                    activeSection = if (activeSection == FilterAccordionSection.PRODUCT_CATEGORY) FilterAccordionSection.NONE else FilterAccordionSection.PRODUCT_CATEGORY
                                }
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (categories.isNotEmpty()) {
                                        Text(if (isBn) "ক্যাটাগরি:" else "Category:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            DrawerChoiceChip(
                                                selected = selectedCategoryId == null,
                                                label = if (isBn) "সকল" else "All",
                                                selectedColor = PrimaryPurple,
                                                onClick = { selectedCategoryId = null }
                                            )
                                            categories.forEach { cat ->
                                                DrawerChoiceChip(
                                                    selected = selectedCategoryId == cat.id,
                                                    label = if (isBn) cat.nameBn else cat.nameEn,
                                                    selectedColor = PrimaryPurple,
                                                    onClick = { selectedCategoryId = cat.id }
                                                )
                                            }
                                        }
                                    }
                                    if (products.isNotEmpty()) {
                                        Text(if (isBn) "পণ্য:" else "Product:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            DrawerChoiceChip(
                                                selected = selectedProductId == null,
                                                label = if (isBn) "সকল পণ্য" else "All Products",
                                                selectedColor = SecondaryTeal,
                                                onClick = { selectedProductId = null }
                                            )
                                            products.take(20).forEach { prod ->
                                                DrawerChoiceChip(
                                                    selected = selectedProductId == prod.id,
                                                    label = if (isBn && prod.nameBn.isNotBlank()) prod.nameBn else prod.nameEn,
                                                    selectedColor = SecondaryTeal,
                                                    onClick = { selectedProductId = prod.id }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Payment Method
                            FilterAccordionItem(
                                title = if (isBn) "পেমেন্ট মাধ্যম" else "Payment Method",
                                selectedSummary = if (isBn) paymentMethodFilter.titleBn else paymentMethodFilter.titleEn,
                                isActive = paymentMethodFilter != PaymentMethodFilter.ALL,
                                icon = Icons.Outlined.Payments,
                                expanded = activeSection == FilterAccordionSection.PAYMENT_METHOD,
                                onToggle = {
                                    activeSection = if (activeSection == FilterAccordionSection.PAYMENT_METHOD) FilterAccordionSection.NONE else FilterAccordionSection.PAYMENT_METHOD
                                }
                            ) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    PaymentMethodFilter.entries.forEach { opt ->
                                        DrawerChoiceChip(
                                            selected = paymentMethodFilter == opt,
                                            label = if (isBn) opt.titleBn else opt.titleEn,
                                            selectedColor = PrimaryPurple,
                                            onClick = { paymentMethodFilter = opt }
                                        )
                                    }
                                }
                            }

                            // Payment Status
                            FilterAccordionItem(
                                title = if (isBn) "পেমেন্ট স্ট্যাটাস" else "Payment Status",
                                selectedSummary = if (isBn) paymentStatus.titleBn else paymentStatus.titleEn,
                                isActive = paymentStatus != PaymentStatusFilter.ALL,
                                icon = Icons.Outlined.AccountBalanceWallet,
                                expanded = activeSection == FilterAccordionSection.PAYMENT_STATUS,
                                onToggle = {
                                    activeSection = if (activeSection == FilterAccordionSection.PAYMENT_STATUS) FilterAccordionSection.NONE else FilterAccordionSection.PAYMENT_STATUS
                                }
                            ) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        PaymentStatusFilter.ALL,
                                        PaymentStatusFilter.PAID,
                                        PaymentStatusFilter.UNPAID,
                                        PaymentStatusFilter.PARTIAL_PAID,
                                        PaymentStatusFilter.OVERDUE,
                                        PaymentStatusFilter.ADVANCE
                                    ).forEach { opt ->
                                        DrawerChoiceChip(
                                            selected = paymentStatus == opt,
                                            label = if (isBn) opt.titleBn else opt.titleEn,
                                            selectedColor = PrimaryPurple,
                                            onClick = { paymentStatus = opt }
                                        )
                                    }
                                }
                            }

                            // Transaction Type (Inflow / Outflow + Financial/Delivery)
                            FilterAccordionItem(
                                title = if (isBn) "লেনদেনের ধরন (প্রবাহ / ডেলিভারি)" else "Transaction Type & Flow",
                                selectedSummary = if (isBn) transactionDirection.titleBn else transactionDirection.titleEn,
                                isActive = transactionDirection != TransactionDirectionFilter.ALL || deliveryReturn != DeliveryReturnFilter.ALL || financialMetric != FinancialMetricFilter.ALL,
                                icon = Icons.Outlined.LocalOffer,
                                expanded = activeSection == FilterAccordionSection.TRANSACTION_TYPE,
                                onToggle = {
                                    activeSection = if (activeSection == FilterAccordionSection.TRANSACTION_TYPE) FilterAccordionSection.NONE else FilterAccordionSection.TRANSACTION_TYPE
                                }
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TransactionDirectionFilter.entries.forEach { opt ->
                                            DrawerChoiceChip(
                                                selected = transactionDirection == opt,
                                                label = if (isBn) opt.titleBn else opt.titleEn,
                                                selectedColor = PrimaryPurple,
                                                onClick = { transactionDirection = opt }
                                            )
                                        }
                                    }
                                    Text(if (isBn) "আর্থিক সূচক:" else "Financial Metric:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FinancialMetricFilter.entries.filterNot {
                                            it in setOf(FinancialMetricFilter.OPENING_BALANCE, FinancialMetricFilter.CLOSING_BALANCE) ||
                                                (!canViewProfit && it in setOf(FinancialMetricFilter.PROFIT, FinancialMetricFilter.LOSS))
                                        }.forEach { opt ->
                                            DrawerChoiceChip(
                                                selected = financialMetric == opt,
                                                label = if (isBn) opt.titleBn else opt.titleEn,
                                                selectedColor = StatusInfo,
                                                onClick = { financialMetric = opt }
                                            )
                                        }
                                    }
                                    Text(if (isBn) "ডেলিভারি / রিটার্ন:" else "Delivery / Return:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        DeliveryReturnFilter.entries.forEach { opt ->
                                            DrawerChoiceChip(
                                                selected = deliveryReturn == opt,
                                                label = if (isBn) opt.titleBn else opt.titleEn,
                                                selectedColor = SecondaryTeal,
                                                onClick = { deliveryReturn = opt }
                                            )
                                        }
                                    }
                                }
                            }

                            // Wholesale / Retail
                            FilterAccordionItem(
                                title = if (isBn) "লেনদেনের ধরন (খুচরা / পাইকারি)" else "Wholesale / Retail",
                                selectedSummary = if (isBn) saleChannel.titleBn else saleChannel.titleEn,
                                isActive = saleChannel != SaleChannelFilter.ALL,
                                icon = Icons.Outlined.Store,
                                expanded = activeSection == FilterAccordionSection.SALE_CHANNEL,
                                onToggle = {
                                    activeSection = if (activeSection == FilterAccordionSection.SALE_CHANNEL) FilterAccordionSection.NONE else FilterAccordionSection.SALE_CHANNEL
                                }
                            ) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SaleChannelFilter.entries.forEach { opt ->
                                        DrawerChoiceChip(
                                            selected = saleChannel == opt,
                                            label = if (isBn) opt.titleBn else opt.titleEn,
                                            selectedColor = SecondaryTeal,
                                            onClick = { saleChannel = opt }
                                        )
                                    }
                                }
                            }

                            // Warehouse
                            if (warehouses.isNotEmpty()) {
                                val selectedWhName = warehouses.firstOrNull { it.id == selectedWarehouseId }?.name
                                FilterAccordionItem(
                                    title = if (isBn) "ওয়্যারহাউস (গুদাম)" else "Warehouse",
                                    selectedSummary = selectedWhName ?: (if (isBn) "সকল ওয়্যারহাউস" else "All Warehouses"),
                                    isActive = selectedWarehouseId != null,
                                    icon = Icons.Outlined.Warehouse,
                                    expanded = activeSection == FilterAccordionSection.WAREHOUSE,
                                    onToggle = {
                                        activeSection = if (activeSection == FilterAccordionSection.WAREHOUSE) FilterAccordionSection.NONE else FilterAccordionSection.WAREHOUSE
                                    }
                                ) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        DrawerChoiceChip(
                                            selected = selectedWarehouseId == null,
                                            label = if (isBn) "সকল ওয়্যারহাউস" else "All Warehouses",
                                            selectedColor = PrimaryPurple,
                                            onClick = { selectedWarehouseId = null }
                                        )
                                        warehouses.forEach { wh ->
                                            DrawerChoiceChip(
                                                selected = selectedWarehouseId == wh.id,
                                                label = wh.name,
                                                selectedColor = PrimaryPurple,
                                                onClick = { selectedWarehouseId = wh.id }
                                            )
                                        }
                                    }
                                }
                            }

                            // Amount Range (Min & Max)
                            FilterAccordionItem(
                                title = if (isBn) "পরিমাণ রেঞ্জ (৳)" else "Amount Range (Min / Max)",
                                selectedSummary = if (isBn) amountFilter.titleBn else amountFilter.titleEn,
                                isActive = amountFilter != AmountRangeFilter.ALL,
                                icon = Icons.Outlined.Payments,
                                expanded = activeSection == FilterAccordionSection.AMOUNT_RANGE,
                                onToggle = {
                                    activeSection = if (activeSection == FilterAccordionSection.AMOUNT_RANGE) FilterAccordionSection.NONE else FilterAccordionSection.AMOUNT_RANGE
                                }
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        AmountRangeFilter.entries.forEach { opt ->
                                            DrawerChoiceChip(
                                                selected = amountFilter == opt,
                                                label = if (isBn) opt.titleBn else opt.titleEn,
                                                selectedColor = StatusWarning,
                                                onClick = { amountFilter = opt }
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = minAmountText,
                                            onValueChange = {
                                                minAmountText = it
                                                amountFilter = AmountRangeFilter.CUSTOM
                                            },
                                            label = { Text(if (isBn) "সর্বনিম্ন (৳)" else "Min (৳)", fontSize = 11.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        OutlinedTextField(
                                            value = maxAmountText,
                                            onValueChange = {
                                                maxAmountText = it
                                                amountFilter = AmountRangeFilter.CUSTOM
                                            },
                                            label = { Text(if (isBn) "সর্বোচ্চ (৳)" else "Max (৳)", fontSize = 11.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }

                            // Sorting
                            FilterAccordionItem(
                                title = if (isBn) "তালিকা সাজানো (Sort)" else "Sort By",
                                selectedSummary = if (isBn) sortBy.titleBn else sortBy.titleEn,
                                isActive = sortBy != SortOption.NEWEST_FIRST,
                                icon = Icons.Outlined.Tune,
                                expanded = activeSection == FilterAccordionSection.SORT_BY,
                                onToggle = {
                                    activeSection = if (activeSection == FilterAccordionSection.SORT_BY) FilterAccordionSection.NONE else FilterAccordionSection.SORT_BY
                                }
                            ) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SortOption.entries.forEach { opt ->
                                        DrawerChoiceChip(
                                            selected = sortBy == opt,
                                            label = if (isBn) opt.titleBn else opt.titleEn,
                                            selectedColor = PrimaryPurple,
                                            onClick = { sortBy = opt }
                                        )
                                    }
                                }
                            }

                            // Optional Save Preset Input
                            if (showSavePresetField) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = PrimaryPurple.copy(alpha = 0.06f),
                                    border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = presetName,
                                            onValueChange = { presetName = it },
                                            label = { Text(if (isBn) "প্রিসেটের নাম লিখুন" else "Preset Name") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { showSavePresetField = false },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(if (isBn) "বাতিল" else "Cancel", fontSize = 12.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    if (presetName.isNotBlank()) {
                                                        onApply(buildAppliedFilter())
                                                        onSavePreset(presetName.trim())
                                                        presetName = ""
                                                        showSavePresetField = false
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                                            ) {
                                                Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isBn) "সেভ" else "Save", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = ERPDivider.copy(alpha = 0.6f))

                        // Sticky Bottom Action Bar
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onApply(buildAppliedFilter())
                                    closeDrawerAnimated()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("filter_drawer_apply"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                            ) {
                                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBn) "ফিল্টার প্রয়োগ করুন" else "Apply Filters",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onReset()
                                        closeDrawerAnimated()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("filter_drawer_reset"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isBn) "রিসেট" else "Reset", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = { showSavePresetField = !showSavePresetField },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Outlined.Description, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBn) "সংরক্ষণ করুন" else "Save Preset",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterAccordionItem(
    title: String,
    selectedSummary: String,
    isActive: Boolean,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) PrimaryPurple.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isActive || expanded) PrimaryPurple.copy(alpha = 0.5f) else ERPDivider
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isActive) PrimaryPurple else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isActive || expanded) {
                            Text(
                                text = selectedSummary,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isActive) PrimaryPurple else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    HorizontalDivider(color = ERPDivider.copy(alpha = 0.4f), modifier = Modifier.padding(bottom = 8.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun DrawerChoiceChip(
    selected: Boolean,
    label: String,
    selectedColor: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
            }
        } else null,
        label = {
            Text(
                text = label,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.5.sp
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = selectedColor,
            selectedLabelColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            selectedBorderColor = selectedColor,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.5.dp
        ),
        shape = RoundedCornerShape(8.dp)
    )
}
