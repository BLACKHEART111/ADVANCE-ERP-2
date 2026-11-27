package com.pocketdev.advance_erp.feature.inventory

import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.StatusErrorContainer
import com.pocketdev.advance_erp.core.security.BiometricCostGuard
import com.pocketdev.advance_erp.core.security.rememberBiometricCostVisibility
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPActionButton
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPButtonVariant
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.ERPEmptyState
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.designsystem.rememberImageModel
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.dao.ProductWithDetails
import com.pocketdev.advance_erp.data.dao.StockItemOverview
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.StockMovementType
import com.pocketdev.advance_erp.data.entity.StockTransferStatus
import com.pocketdev.advance_erp.feature.inventory.cogs.CogsTraceabilityView
import com.pocketdev.advance_erp.feature.inventory.costhistory.CostAdjustmentInputDialog
import com.pocketdev.advance_erp.feature.inventory.costhistory.ProductCostHistoryDialog
import com.pocketdev.advance_erp.feature.inventory.reconciliation.InventoryReconciliationView
import com.pocketdev.advance_erp.feature.inventory.valuation.InventoryValuationView
import com.pocketdev.advance_erp.feature.product.ProductDetailDialog

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier,
    initialAction: String = ""
) {
    val state by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var actionHandled by rememberSaveable(initialAction) { mutableStateOf(false) }
    LaunchedEffect(initialAction) {
        if (!actionHandled && initialAction == "adjust") {
            viewModel.setAdjustDialogVisible(true)
            actionHandled = true
        }
    }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var showQuickActionDialog by rememberSaveable { mutableStateOf(false) }
    var isSummaryExpanded by rememberSaveable { mutableStateOf(false) }
    var labelProductAndVariants by remember { mutableStateOf<Pair<ProductWithDetails, List<ProductVariant>>?>(null) }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ERPBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Consolidated Single Floating Action Button (Eliminates product list obstruction)
            FloatingActionButton(
                onClick = { showQuickActionDialog = true },
                containerColor = PrimaryPurple,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.quick_actions),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            // 1. Compact Inventory Summary Component (~38dp collapsed strip)
            CompactInventorySummary(
                state = state,
                isExpanded = isSummaryExpanded,
                onToggleExpand = { isSummaryExpanded = !isSummaryExpanded }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Compact Search & Filter Bar (46dp height)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Compact Search Input Field using BasicTextField so height doesn't clip text
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (searchQuery.isNotEmpty()) PrimaryPurple else ERPDivider)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.search),
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_inventory_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                                cursorBrush = SolidColor(PrimaryPurple),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onSearchQueryChange("") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = stringResource(R.string.clear),
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Barcode / QR Scanner Quick Action
                IconButton(
                    onClick = { viewModel.setScannerDialogVisible(true) },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryPurple.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = stringResource(R.string.action_barcode_scanner),
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Filter & Sort Button (with active filter badge indicator)
                val isFilterActive = state.selectedWarehouseId != null ||
                        state.stockStatusFilter != StockStatusFilter.ALL ||
                        state.sortOption != InventorySortOption.NAME_ASC

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isFilterActive) PrimaryPurple.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            if (isFilterActive) PrimaryPurple else ERPDivider,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { showFilterDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = stringResource(R.string.filter_and_sort),
                        tint = if (isFilterActive) PrimaryPurple else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    if (isFilterActive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // AI Stock Forecast & Reorder Quick Action
                val urgentReorders = state.stockForecastSummary?.urgentReorderCount ?: 0
                val deadStockItems = state.stockForecastSummary?.deadStockCount ?: 0
                val hasAlerts = urgentReorders > 0 || deadStockItems > 0

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (hasAlerts) StatusErrorContainer else PrimaryPurple.copy(alpha = 0.1f))
                        .border(
                            1.dp,
                            if (hasAlerts) StatusError else PrimaryPurple.copy(alpha = 0.2f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.openStockForecastDialog() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "AI Stock Forecast",
                        tint = if (hasAlerts) StatusError else PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    if (hasAlerts) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StatusError)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Compact Horizontal Scrollable Category Chips (Single-row, ~28dp)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // "All Categories" Chip
                item {
                    val isAllSelected = state.selectedCategoryId == null
                    CategoryFilterChip(
                        name = stringResource(R.string.all_categories),
                        isSelected = isAllSelected,
                        onClick = { viewModel.onCategorySelect(null) }
                    )
                }

                // Individual Category Chips
                items(state.categories, key = { it.id }) { cat ->
                    val isSelected = state.selectedCategoryId == cat.id
                    val catName = if (state.language == AppLanguage.BANGLA) cat.nameBn else cat.nameEn
                    CategoryFilterChip(
                        name = catName,
                        isSelected = isSelected,
                        onClick = { viewModel.onCategorySelect(cat.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Compact Secondary Navigation Tabs (Single-row pills, RBAC filtered)
            val visibleTabs = remember(state.canViewCost) {
                if (state.canViewCost) {
                    InventoryTab.entries
                } else {
                    InventoryTab.entries.filter { it != InventoryTab.VALUATION && it != InventoryTab.COGS_TRACE }
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(visibleTabs) { tab ->
                    val isSelected = state.selectedTab == tab
                    val label = when (tab) {
                        InventoryTab.STOCK_OVERVIEW -> stringResource(R.string.stock_overview)
                        InventoryTab.LOW_STOCK -> stringResource(R.string.low_stock_warning)
                        InventoryTab.TRANSFERS -> stringResource(R.string.stock_transfers)
                        InventoryTab.MOVEMENTS -> if (state.language == AppLanguage.BANGLA) "মুভমেন্ট অডিট" else "Movements"
                        InventoryTab.VALUATION -> if (state.language == AppLanguage.BANGLA) "মজুদ মূল্যায়ন" else "Valuation"
                        InventoryTab.RECONCILIATION -> if (state.language == AppLanguage.BANGLA) "পুনর্মিলন" else "Reconciliation"
                        InventoryTab.COGS_TRACE -> if (state.language == AppLanguage.BANGLA) "COGS ট্রেস" else "COGS Trace"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryPurple else Color.Transparent)
                            .clickable { viewModel.onTabSelect(tab) }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                            if (tab == InventoryTab.LOW_STOCK && state.lowStockItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else StatusError)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${state.lowStockItems.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) PrimaryPurple else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 5. Tab Content Views
            when (state.selectedTab) {
                InventoryTab.STOCK_OVERVIEW -> {
                    if (state.stockItems.isEmpty()) {
                        ERPEmptyState(
                            icon = Icons.Outlined.Inventory2,
                            title = stringResource(R.string.no_stock_data),
                            message = if (state.language == AppLanguage.BANGLA)
                                "কোনো পণ্য বা স্টক রেকর্ড খুঁজে পাওয়া যায়নি।"
                            else
                                "No matching stock balances recorded."
                        )
                    } else {
                        // High-density Compact Product Cards List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 128.dp)
                        ) {
                            items(state.stockItems, key = { it.productId }) { item ->
                                CompactStockProductCard(
                                    item = item,
                                    language = state.language,
                                    canViewCost = state.canViewCost,
                                    onClick = { viewModel.openProductDetail(item.productId) }
                                )
                            }
                        }
                    }
                }

                InventoryTab.LOW_STOCK -> {
                    if (state.lowStockItems.isEmpty()) {
                        ERPEmptyState(
                            icon = Icons.Outlined.Inventory2,
                            title = if (state.language == AppLanguage.BANGLA)
                                "সকল পণ্যের পর্যাপ্ত মজুদ আছে"
                            else
                                "All items sufficiently stocked",
                            message = if (state.language == AppLanguage.BANGLA)
                                "কোনো পণ্যের মজুদ সতর্কতার সীমার নিচে নামেনি।"
                            else
                                "No products have fallen below their minimum stock threshold."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 128.dp)
                        ) {
                            items(state.lowStockItems, key = { it.productId }) { item ->
                                CompactStockProductCard(
                                    item = item,
                                    language = state.language,
                                    canViewCost = state.canViewCost,
                                    onClick = { viewModel.openProductDetail(item.productId) }
                                )
                            }
                        }
                    }
                }

                InventoryTab.MOVEMENTS -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (state.language == AppLanguage.BANGLA) "স্থায়ী স্টক মুভমেন্ট অডিট লেজার" else "Permanent Movement Ledger",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (state.movements.isNotEmpty()) {
                                ERPActionButton(
                                    text = if (state.language == AppLanguage.BANGLA) "PDF রপ্তানি" else "Export PDF",
                                    onClick = { viewModel.exportMovementLedgerPdf(context) },
                                    icon = Icons.Outlined.PictureAsPdf,
                                    variant = ERPButtonVariant.OUTLINED
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (state.movements.isEmpty()) {
                            ERPEmptyState(
                                icon = Icons.Outlined.History,
                                title = stringResource(R.string.no_movements_data),
                                message = if (state.language == AppLanguage.BANGLA) {
                                    "পণ্য গ্রহণ, বিক্রয় ডিসপ্যাচ ও স্টক স্থানান্তরের মুভমেন্ট এখানে দেখা যাবে।"
                                } else {
                                    "Stock movements like receipts, dispatches and transfers will appear here."
                                }
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(bottom = 128.dp)
                            ) {
                                items(state.movements, key = { it.id }) { mov ->
                                    val movType = StockMovementType.fromCode(mov.movementType)
                                    val isIncrease = movType.isIncoming

                                    val movementLabel = when (movType) {
                                        StockMovementType.PURCHASE_RECEIPT -> stringResource(R.string.movement_purchase_receipt)
                                        StockMovementType.SALE_DISPATCH -> stringResource(R.string.movement_sale_dispatch)
                                        StockMovementType.SALE_RETURN -> stringResource(R.string.movement_sale_return)
                                        StockMovementType.PURCHASE_RETURN -> stringResource(R.string.movement_purchase_return)
                                        StockMovementType.STOCK_ADJUSTMENT_IN -> stringResource(R.string.movement_adjustment_inc)
                                        StockMovementType.STOCK_ADJUSTMENT_OUT -> stringResource(R.string.movement_adjustment_dec)
                                        StockMovementType.TRANSFER_OUT -> stringResource(R.string.movement_transfer_out)
                                        StockMovementType.TRANSFER_IN -> stringResource(R.string.movement_transfer_in)
                                        StockMovementType.DAMAGE_WRITE_OFF -> stringResource(R.string.movement_damage_writeoff)
                                        StockMovementType.SHORTAGE -> if (state.language == AppLanguage.BANGLA) "ঘাটতি" else "Shortage"
                                        StockMovementType.OPENING_STOCK -> if (state.language == AppLanguage.BANGLA) "প্রারম্ভিক মজুদ" else "Opening Stock"
                                        StockMovementType.INVENTORY_COST_ADJUSTMENT -> if (state.language == AppLanguage.BANGLA) "ব্যয় সমন্বয়" else "Cost Adjustment"
                                    }

                                    ERPCard(contentPadding = 10.dp) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .background(
                                                        (if (isIncrease) StatusSuccess else PrimaryPurple).copy(alpha = 0.12f),
                                                        RoundedCornerShape(8.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isIncrease) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                                                    contentDescription = null,
                                                    tint = if (isIncrease) StatusSuccess else PrimaryPurple,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (state.language == AppLanguage.BANGLA) mov.productNameBn else mov.productNameEn,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "${mov.movementNumber} • ${mov.warehouseName} • ${Formatters.formatDate(mov.createdAt, state.language)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = if (state.language == AppLanguage.BANGLA) {
                                                        "স্থিতি: ${mov.balanceAfterMovement} পিস • ব্যয়: ${Formatters.formatCurrency(mov.totalCostImpactMinor, state.currency, state.language)}"
                                                    } else {
                                                        "Bal: ${mov.balanceAfterMovement} u • Cost: ${Formatters.formatCurrency(mov.totalCostImpactMinor, state.currency, state.language)}"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SecondaryTeal,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = (if (isIncrease) "+" else "-") + "${mov.quantity}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isIncrease) StatusSuccess else StatusError
                                                )
                                                Text(
                                                    text = movementLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                InventoryTab.VALUATION -> {
                    BiometricCostGuard(
                        scopeKey = "inventory-VALUATION:${state.selectedWarehouseId}",
                        allowed = state.canViewCost,
                        isBangla = state.language == AppLanguage.BANGLA
                    ) {
                        InventoryValuationView(
                            breakdown = state.valuationBreakdown,
                            language = state.language,
                            currency = state.currency,
                            onExportPdf = { viewModel.exportValuationPdf(context) }
                        )
                    }
                }

                InventoryTab.RECONCILIATION -> {
                    BiometricCostGuard(
                        scopeKey = "inventory-RECONCILIATION:${state.selectedWarehouseId}",
                        allowed = state.canViewCost,
                        isBangla = state.language == AppLanguage.BANGLA
                    ) {
                        InventoryReconciliationView(
                            reconciliation = state.latestReconciliation,
                            items = state.reconciliationItems,
                            isReconciling = state.isReconciling,
                            language = state.language,
                            currency = state.currency,
                            onRunReconciliation = { viewModel.runReconciliationAudit() },
                            onExportPdf = { viewModel.exportReconciliationPdf(context) }
                        )
                    }
                }

                InventoryTab.COGS_TRACE -> {
                    BiometricCostGuard(
                        scopeKey = "inventory-COGS_TRACE:${state.selectedWarehouseId}",
                        allowed = state.canViewCost,
                        isBangla = state.language == AppLanguage.BANGLA
                    ) {
                        CogsTraceabilityView(
                            consumptions = state.fifoConsumptions,
                            totalCogsMinor = state.totalCogsMinor,
                            language = state.language,
                            currency = state.currency
                        )
                    }
                }

                InventoryTab.TRANSFERS -> {
                    if (state.transfers.isEmpty()) {
                        ERPEmptyState(
                            icon = Icons.Outlined.LocalShipping,
                            title = stringResource(R.string.stock_transfers),
                            message = if (state.language == AppLanguage.BANGLA)
                                "ওয়্যারহাউসগুলোর মধ্যে কোনো স্থানান্তর নেই। স্থানান্তর তৈরি করতে + চাপুন।"
                            else
                                "Transfers between warehouses will appear here. Click + to create a transfer."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 128.dp)
                        ) {
                            items(state.transfers, key = { it.id }) { transfer ->
                                val status = try {
                                    StockTransferStatus.valueOf(transfer.status)
                                } catch (e: Exception) {
                                    StockTransferStatus.DRAFT
                                }

                                val statusColor = when (status) {
                                    StockTransferStatus.DRAFT -> TextMuted
                                    StockTransferStatus.IN_TRANSIT -> StatusWarning
                                    StockTransferStatus.RECEIVED -> StatusSuccess
                                    StockTransferStatus.CANCELLED -> StatusError
                                }

                                ERPCard(contentPadding = 10.dp) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = transfer.transferNumber,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryPurple
                                                )
                                                Text(
                                                    text = "${transfer.sourceWarehouseName} ➔ ${transfer.destinationWarehouseName}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextPrimary
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = statusColor.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = if (state.language == AppLanguage.BANGLA) status.titleBn else status.titleEn,
                                                    color = statusColor,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Divider(color = ERPDivider)
                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = if (state.language == AppLanguage.BANGLA) transfer.productNameBn else transfer.productNameEn,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                val variantPart = if (!transfer.variantSku.isNullOrBlank()) " • Variant: ${transfer.variantSku}" else ""
                                                Text(
                                                    text = "SKU: ${transfer.baseSku}$variantPart",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }

                                            Text(
                                                text = "${transfer.quantity} ${if (state.language == AppLanguage.BANGLA) "পিস" else "units"}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }

                                        // Action buttons for workflow state transitions
                                        if (status == StockTransferStatus.DRAFT || status == StockTransferStatus.IN_TRANSIT) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                ERPActionButton(
                                                    text = stringResource(R.string.transfer_cancel),
                                                    onClick = { viewModel.cancelStockTransfer(transfer.id, "User cancelled") },
                                                    variant = ERPButtonVariant.GHOST
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))

                                                if (status == StockTransferStatus.DRAFT) {
                                                    ERPActionButton(
                                                        text = stringResource(R.string.transfer_dispatch),
                                                        onClick = { viewModel.dispatchStockTransfer(transfer.id) },
                                                        variant = ERPButtonVariant.PRIMARY
                                                    )
                                                } else if (status == StockTransferStatus.IN_TRANSIT) {
                                                    ERPActionButton(
                                                        text = stringResource(R.string.transfer_receive),
                                                        onClick = { viewModel.receiveStockTransfer(transfer.id) },
                                                        variant = ERPButtonVariant.SECONDARY
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
            }
        }
    }

    // 6. Filter & Sort Dialog (Multi-dimensional filtering & sorting)
    if (showFilterDialog) {
        InventoryFilterDialog(
            state = state,
            onWarehouseSelect = { viewModel.onWarehouseSelect(it) },
            onStockStatusFilterChange = { viewModel.onStockStatusFilterChange(it) },
            onSortOptionChange = { viewModel.onSortOptionChange(it) },
            onResetFilters = { viewModel.resetFilters() },
            onDismiss = { showFilterDialog = false }
        )
    }

    // 7. Consolidated Quick Action Dialog
    if (showQuickActionDialog) {
        InventoryQuickActionDialog(
            language = state.language,
            onAdjustStock = { viewModel.setAdjustDialogVisible(true) },
            onTransferStock = { viewModel.setTransferDialogVisible(true) },
            onScanCode = { viewModel.setScannerDialogVisible(true) },
            onReconciliation = { viewModel.onTabSelect(InventoryTab.RECONCILIATION) },
            onExportLedger = { viewModel.exportMovementLedgerPdf(context) },
            onDismiss = { showQuickActionDialog = false }
        )
    }

    // Adjustment Dialog (Requirement 3: Variant-Aware)
    if (state.showAdjustDialog) {
        StockAdjustmentDialog(
            warehouses = state.warehouses,
            products = state.allProducts,
            language = state.language,
            onLoadVariants = { prodId -> viewModel.loadVariantsForProduct(prodId) },
            onDismiss = { viewModel.setAdjustDialogVisible(false) },
            onConfirm = { whId, prodId, varId, qty, isInc, reason ->
                viewModel.performAdjustment(whId, prodId, varId, qty, isInc, reason)
            }
        )
    }

    // Stock Transfer Dialog (Requirement 2)
    if (state.showTransferDialog) {
        StockTransferDialog(
            warehouses = state.warehouses,
            products = state.allProducts,
            language = state.language,
            onLoadVariants = { prodId -> viewModel.loadVariantsForProduct(prodId) },
            onDismiss = { viewModel.setTransferDialogVisible(false) },
            onConfirm = { draft ->
                viewModel.createStockTransfer(draft)
            }
        )
    }

    // Barcode / QR Scanner Dialog (Phase 06 Part 2)
    if (state.showScannerDialog) {
        BarcodeQrScannerDialog(
            language = state.language,
            onDismiss = { viewModel.setScannerDialogVisible(false) },
            onCodeScanned = { code ->
                viewModel.onCodeScanned(code)
            }
        )
    }

    // Product & Variant Details Dialog with Photo Gallery, Barcode & QR (Phase 06 Part 2 & 3)
    state.selectedProductForDetail?.let { product ->
        ProductDetailDialog(
            product = product,
            variants = state.selectedProductVariants,
            productImages = state.selectedProductImages,
            variantStockMap = state.variantStockMap,
            language = state.language,
            currency = state.currency,
            preSelectedVariantId = state.preSelectedVariantId,
            canViewCost = state.canViewCost,
            onObserveCostLayers = viewModel::observeProductCostLayers,
            onLoadVariantImages = { variantId ->
                viewModel.loadVariantImages(variantId)
            },
            onViewCostHistory = { prodId ->
                viewModel.openCostHistory(prodId)
            },
            onPrintLabels = { prodWithDetails, vars ->
                labelProductAndVariants = Pair(prodWithDetails, vars)
            },
            onDismiss = { viewModel.closeProductDetail() }
        )
    }

    // Product Label Printing Dialog (Phase 06 Part 4)
    labelProductAndVariants?.let { (productWithDetails, vars) ->
        val baseProduct = Product(
            id = productWithDetails.id,
            categoryId = productWithDetails.categoryId,
            nameEn = productWithDetails.nameEn,
            nameBn = productWithDetails.nameBn,
            baseSku = productWithDetails.baseSku,
            barcode = productWithDetails.barcode,
            description = productWithDetails.description,
            unit = productWithDetails.unit,
            purchasePriceMinor = productWithDetails.purchasePriceMinor,
            sellingPriceMinor = productWithDetails.sellingPriceMinor,
            minStockAlert = productWithDetails.minStockAlert,
            isActive = productWithDetails.isActive,
            createdAt = productWithDetails.createdAt,
            updatedAt = productWithDetails.createdAt
        )
        ProductLabelPrintDialog(
            product = baseProduct,
            variants = vars,
            onDismiss = { labelProductAndVariants = null }
        )
    }

    // Product Cost History & FIFO Layers Dialog (Phase 06 Part 3)
    if (state.showCostHistoryDialog) {
        ProductCostHistoryDialog(
            summary = state.selectedProductCostSummary,
            layers = state.selectedProductCostLayers,
            canViewCost = state.canViewCost,
            adjustments = state.selectedProductCostAdjustments,
            language = state.language,
            currency = state.currency,
            onAdjustLayer = { layer ->
                viewModel.openCostAdjustment(layer)
            },
            onDismiss = { viewModel.closeCostHistory() }
        )
    }

    // Cost Adjustment Input Dialog (Phase 06 Part 3)
    if (state.showCostAdjustmentDialog && state.selectedLayerForAdjustment != null) {
        CostAdjustmentInputDialog(
            layer = state.selectedLayerForAdjustment!!,
            canViewCost = state.canViewCost,
            language = state.language,
            currency = state.currency,
            onConfirm = { costLayerId, newUnitCostMinor, reason ->
                viewModel.performCostAdjustment(costLayerId, newUnitCostMinor, reason)
            },
            onDismiss = { viewModel.closeCostAdjustment() }
        )
    }

    // AI Stock Forecast & Dead Stock Dialog
    if (state.showStockForecastDialog && state.stockForecastSummary != null) {
        StockForecastDialog(
            summary = state.stockForecastSummary!!,
            isBangla = state.language == com.pocketdev.advance_erp.core.model.AppLanguage.BANGLA,
            onDismiss = { viewModel.closeStockForecastDialog() }
        )
    }
}

/**
 * Compact Inventory Summary Strip with expand/collapse capability.
 * Collapsed height ~38dp.
 */
@Composable
private fun CompactInventorySummary(
    state: InventoryUiState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF7B1FA2), Color(0xFF4A148C), Color(0xFF00796B))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Horizontal collapsed summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Metric 1: Physical Stock
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.stock_physical_short),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.80f)
                    )
                    Text(
                        text = Formatters.formatNumber(state.totalInventoryQuantity.toLong(), state.language),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Metric 2: Sellable Stock
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.stock_sellable_short),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.80f)
                    )
                    Text(
                        text = Formatters.formatNumber(state.totalSellableStock.toLong(), state.language),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF69F0AE)
                    )
                }

                // Metric 3: Damaged Stock
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.stock_damaged_short),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.80f)
                    )
                    Text(
                        text = Formatters.formatNumber(state.totalDamagedStock.toLong(), state.language),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.totalDamagedStock > 0) Color(0xFFFF8A80) else Color.White.copy(alpha = 0.78f)
                    )
                }

                // Metric 4: Valuation (RBAC Protected: Only shown if canViewCost is true)
                if (state.canViewCost) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = stringResource(R.string.stock_valuation_short),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.80f)
                        )
                        Text(
                            text = Formatters.formatMoney(
                                state.totalInventoryValueMinor,
                                state.currency,
                                state.language
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF80DEEA)
                        )
                    }
                }

                // Expand / Collapse Chevron Toggle
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse summary" else "Expand summary",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded Detail View
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.22f))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val reservedStock = (state.totalInventoryQuantity - state.totalSellableStock - state.totalDamagedStock).coerceAtLeast(0)
                        Column {
                            Text(
                                text = if (state.language == AppLanguage.BANGLA) "সংরক্ষিত মজুদ:" else "Reserved Stock:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.80f)
                            )
                            Text(
                                text = Formatters.formatNumber(reservedStock.toLong(), state.language),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = if (state.language == AppLanguage.BANGLA) "স্বল্প মজুদ সতর্কতা:" else "Low Stock Alerts:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.80f)
                            )
                            Text(
                                text = "${state.lowStockItems.size} ${if (state.language == AppLanguage.BANGLA) "আইটেম" else "items"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (state.lowStockItems.isNotEmpty()) Color(0xFFFFCC80) else Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Column {
                            val selectedWh = state.warehouses.firstOrNull { it.id == state.selectedWarehouseId }
                            Text(
                                text = if (state.language == AppLanguage.BANGLA) "ওয়্যারহাউস:" else "Warehouse Scope:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.80f)
                            )
                            Text(
                                text = selectedWh?.name ?: stringResource(R.string.all_warehouses),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF80DEEA)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ultra-efficient Compact Product Card (~105-115dp height).
 * Displays product thumbnail, name (up to 2 lines), SKU, category, variant pill badge,
 * low stock status badge, chevron tap indicator, and bottom stock metrics pill row.
 */
@Composable
private fun CompactStockProductCard(
    item: StockItemOverview,
    language: AppLanguage,
    canViewCost: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val costVisibility = rememberBiometricCostVisibility(listOf(item.productId, item.minCostMinor, item.maxCostMinor), canViewCost, language == AppLanguage.BANGLA)
    val costVisible = costVisibility.isVisible
    val stockColor = when {
        item.totalQuantity == 0 -> StatusError
        item.isLowStock -> StatusWarning // Amber
        else -> StatusSuccess
    }

    val statusText = when {
        item.totalQuantity == 0 -> if (language == AppLanguage.BANGLA) "স্টক শেষ" else "OUT OF STOCK"
        item.isLowStock -> if (language == AppLanguage.BANGLA) "স্বল্প মজুদ" else "LOW STOCK"
        else -> if (language == AppLanguage.BANGLA) "মজুদ আছে" else "IN STOCK"
    }

    val variantCount = remember(item.variantSkus) {
        if (item.variantSkus.isNullOrBlank()) 0
        else item.variantSkus.split(",").map { it.trim() }.count { it.isNotEmpty() }
    }

    val imageModel = rememberImageModel(item.primaryImageUri)

    ERPCard(
        contentPadding = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main Row: Thumbnail + Product Info + Status Pill & Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Photo Thumbnail (44dp x 44dp)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryPurple.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageModel != null) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = item.productNameEn,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = PrimaryPurple.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Product Name, SKU, Category & Variants Badge
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (language == AppLanguage.BANGLA) item.productNameBn else item.productNameEn,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val catLabel = if (language == AppLanguage.BANGLA) {
                            item.categoryNameBn.ifBlank { item.categoryNameEn }
                        } else {
                            item.categoryNameEn
                        }
                        Text(
                            text = "SKU: ${item.baseSku}${if (catLabel.isNotBlank()) " • $catLabel" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (variantCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PrimaryPurple.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BANGLA) "$variantCount ভেরিয়েন্ট" else "$variantCount vars",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryPurple,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Status Badge & Chevron Icon
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(stockColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = stockColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "Details",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Divider(color = ERPDivider.copy(alpha = 0.6f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(4.dp))

            // Bottom Stock Metrics Row (In Stock, Reserved, Available/Sellable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ERPBackground.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactMetricPill(
                    label = stringResource(R.string.in_stock),
                    value = "${Formatters.formatNumber(item.totalQuantity.toLong(), language)} ${item.unit}",
                    valueColor = stockColor,
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                )

                CompactMetricPill(
                    label = stringResource(R.string.reserved_stock),
                    value = "${Formatters.formatNumber(item.totalReserved.toLong(), language)} ${item.unit}",
                    valueColor = TextMuted,
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                )

                CompactMetricPill(
                    label = stringResource(R.string.available_stock),
                    value = "${Formatters.formatNumber(item.availableQuantity.toLong(), language)} ${item.unit}",
                    valueColor = if (item.availableQuantity > 0) SecondaryTeal else StatusError,
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                )
            }

            if (canViewCost) {
                Spacer(modifier = Modifier.height(4.dp))
                val costText = if (item.minCostMinor == item.maxCostMinor || item.minCostMinor == 0L) {
                    val cost = if (item.maxCostMinor > 0L) item.maxCostMinor else item.minCostMinor
                    Formatters.formatMoney(cost, CurrencyCode.BDT, language)
                } else {
                    "${Formatters.formatMoney(item.minCostMinor, CurrencyCode.BDT, language)} – ${Formatters.formatMoney(item.maxCostMinor, CurrencyCode.BDT, language)}"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "কেনা / ল্যান্ডিং খরচ:" else "Cost / Landed:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = if (costVisible) costText else "••••",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    IconButton(onClick = costVisibility.toggle, modifier = Modifier.size(48.dp)) {
                        Icon(if (costVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (language == AppLanguage.BANGLA) {
                                if (costVisible) "ল্যান্ডেড খরচ লুকান" else "ল্যান্ডেড খরচ দেখুন"
                            } else if (costVisible) "Hide landed cost" else "Show landed cost",
                            tint = SecondaryTeal)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactMetricPill(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CategoryFilterChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else BorderStroke(1.dp, ERPDivider),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else TextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/**
 * Filter and Sort Dialog allowing multi-dimensional filtering by warehouse,
 * stock status, and sorting by name, SKU, or quantity.
 */
@Composable
private fun InventoryFilterDialog(
    state: InventoryUiState,
    onWarehouseSelect: (String?) -> Unit,
    onStockStatusFilterChange: (StockStatusFilter) -> Unit,
    onSortOptionChange: (InventorySortOption) -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filter_and_sort),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section 1: Warehouse Filter
                Text(
                    text = stringResource(R.string.warehouse_select),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        val isAllSelected = state.selectedWarehouseId == null
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAllSelected) SecondaryTeal else ERPBackground,
                            border = if (isAllSelected) null else BorderStroke(1.dp, ERPDivider),
                            modifier = Modifier.clickable { onWarehouseSelect(null) }
                        ) {
                            Text(
                                text = stringResource(R.string.all_warehouses),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAllSelected) Color.White else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    items(state.warehouses, key = { it.id }) { wh ->
                        val isSelected = state.selectedWarehouseId == wh.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SecondaryTeal else ERPBackground,
                            border = if (isSelected) null else BorderStroke(1.dp, ERPDivider),
                            modifier = Modifier.clickable { onWarehouseSelect(wh.id) }
                        ) {
                            Text(
                                text = wh.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Divider(color = ERPDivider)

                // Section 2: Stock Status Filter
                Text(
                    text = stringResource(R.string.stock_status),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(StockStatusFilter.entries) { filter ->
                        val isSelected = state.stockStatusFilter == filter
                        val filterLabel = when (filter) {
                            StockStatusFilter.ALL -> stringResource(R.string.filter_status_all)
                            StockStatusFilter.IN_STOCK -> stringResource(R.string.filter_status_in_stock)
                            StockStatusFilter.LOW_STOCK -> stringResource(R.string.filter_status_low_stock)
                            StockStatusFilter.OUT_OF_STOCK -> stringResource(R.string.filter_status_out_of_stock)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) PrimaryPurple else ERPBackground,
                            border = if (isSelected) null else BorderStroke(1.dp, ERPDivider),
                            modifier = Modifier.clickable { onStockStatusFilterChange(filter) }
                        ) {
                            Text(
                                text = filterLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Divider(color = ERPDivider)

                // Section 3: Sort Options
                Text(
                    text = stringResource(R.string.sort_by),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    InventorySortOption.entries.forEach { option ->
                        val isSelected = state.sortOption == option
                        val sortLabel = when (option) {
                            InventorySortOption.NAME_ASC -> stringResource(R.string.sort_name_asc)
                            InventorySortOption.NAME_DESC -> stringResource(R.string.sort_name_desc)
                            InventorySortOption.SKU_ASC -> stringResource(R.string.sort_sku_asc)
                            InventorySortOption.QUANTITY_DESC -> stringResource(R.string.sort_qty_desc)
                            InventorySortOption.QUANTITY_ASC -> stringResource(R.string.sort_qty_asc)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryPurple.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable { onSortOptionChange(option) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.dp,
                                        if (isSelected) PrimaryPurple else TextMuted,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryPurple)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = sortLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PrimaryPurple else TextPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            ERPActionButton(
                text = stringResource(R.string.apply_filters),
                onClick = onDismiss,
                variant = ERPButtonVariant.PRIMARY
            )
        },
        dismissButton = {
            ERPActionButton(
                text = stringResource(R.string.reset_filters),
                onClick = { onResetFilters() },
                variant = ERPButtonVariant.GHOST
            )
        }
    )
}

/**
 * Consolidated Quick Actions Dialog.
 * Provides accessible triggers for stock adjustments, transfers, scanner, reconciliation,
 * and ledger PDF export from a single floating action button.
 */
@Composable
private fun InventoryQuickActionDialog(
    language: AppLanguage,
    onAdjustStock: () -> Unit,
    onTransferStock: () -> Unit,
    onScanCode: () -> Unit,
    onReconciliation: () -> Unit,
    onExportLedger: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.quick_actions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionItem(
                    icon = Icons.Outlined.SwapHoriz,
                    title = stringResource(R.string.action_stock_adjustment),
                    tint = SecondaryTeal,
                    onClick = {
                        onDismiss()
                        onAdjustStock()
                    }
                )
                QuickActionItem(
                    icon = Icons.Outlined.LocalShipping,
                    title = stringResource(R.string.action_stock_transfer),
                    tint = PrimaryPurple,
                    onClick = {
                        onDismiss()
                        onTransferStock()
                    }
                )
                QuickActionItem(
                    icon = Icons.Outlined.QrCodeScanner,
                    title = stringResource(R.string.action_barcode_scanner),
                    tint = PrimaryPurple,
                    onClick = {
                        onDismiss()
                        onScanCode()
                    }
                )
                QuickActionItem(
                    icon = Icons.Outlined.Tune,
                    title = stringResource(R.string.action_reconciliation),
                    tint = StatusWarning,
                    onClick = {
                        onDismiss()
                        onReconciliation()
                    }
                )
                QuickActionItem(
                    icon = Icons.Outlined.PictureAsPdf,
                    title = stringResource(R.string.action_export_ledger),
                    tint = TextSecondary,
                    onClick = {
                        onDismiss()
                        onExportLedger()
                    }
                )
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    title: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}
