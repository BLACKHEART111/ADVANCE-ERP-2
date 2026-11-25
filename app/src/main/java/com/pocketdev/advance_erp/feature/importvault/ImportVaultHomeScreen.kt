@file:OptIn(ExperimentalLayoutApi::class)

package com.pocketdev.advance_erp.feature.importvault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.core.designsystem.LocalDashboardTokens
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusInfo
import com.pocketdev.advance_erp.core.designsystem.StatusInfoContainer
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.StatusWarningContainer
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.domain.model.ImportVaultFilter
import com.pocketdev.advance_erp.domain.model.ImportVaultOverview
import com.pocketdev.advance_erp.domain.model.PurchaseOrderVaultSummary
import com.pocketdev.advance_erp.domain.model.ShipmentVaultSummary
import com.pocketdev.advance_erp.domain.model.VaultDateFilterTarget
import com.pocketdev.advance_erp.domain.model.VaultDocumentRef
import com.pocketdev.advance_erp.domain.model.VaultScopeType
import com.pocketdev.advance_erp.feature.inventory.BarcodeQrScannerDialog

@Composable
fun ImportVaultHomeScreen(
    uiState: ImportVaultUiState,
    onRefresh: () -> Unit,
    onSearchChange: (String) -> Unit,
    onApplyFilter: (
        datePreset: VaultDatePreset,
        customFromMillis: Long?,
        customToMillis: Long?,
        customAsOfMillis: Long?,
        dateFilterTarget: VaultDateFilterTarget,
        supplierId: String?,
        warehouseId: String?,
        operationalStatus: com.pocketdev.advance_erp.domain.model.OperationalStatus?,
        commercialStatus: com.pocketdev.advance_erp.domain.model.CommercialStatus?,
        onlyProfitable: Boolean,
        onlyLossMaking: Boolean,
        onlyHighAging: Boolean,
        onlyPendingCost: Boolean,
        allocationRuleId: String?
    ) -> Unit,
    onClearFilters: () -> Unit,
    onShelfModeChange: (VaultShelfMode) -> Unit,
    onLayoutModeChange: (VaultLayoutMode) -> Unit,
    onArchiveFilterChange: (VaultArchiveFilter) -> Unit,
    onOpenShipment: (String) -> Unit,
    onOpenPo: (String) -> Unit,
    onOpenPortfolioProfit: () -> Unit,
    onOpenPortfolioCapital: () -> Unit,
    onTogglePin: (VaultScopeType, String, Boolean) -> Unit,
    onToggleArchive: (VaultScopeType, String, String) -> Unit,
    onSaveMetadata: (VaultScopeType, String, String, String, List<String>) -> Unit,
    onOpenExport: () -> Unit = {},
    onAddPortfolioAttachmentClick: () -> Unit = {},
    onPreviewDocument: (VaultDocumentRef) -> Unit = {},
    onShareDocument: (VaultDocumentRef) -> Unit = {},
    onDeleteAttachment: (String) -> Unit = {},
    onNavigateToRoute: (String) -> Unit
) {
    val language = uiState.language
    val isBn = language == AppLanguage.BANGLA
    val tokens = LocalDashboardTokens.current
    val colors = MaterialTheme.colorScheme

    var showScannerDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var expandHeroDetails by remember { mutableStateOf(false) }
    var metadataEditTarget by remember { mutableStateOf<MetadataEditTarget?>(null) }

    val expandedMonths = remember { mutableStateMapOf<String, Boolean>() }

    val overview = uiState.overview
    val activeFilterCount = remember(uiState.filter, uiState.datePreset) {
        countActiveFilters(uiState.filter, uiState.datePreset)
    }

    if (showScannerDialog) {
        BarcodeQrScannerDialog(
            language = language,
            onDismiss = { showScannerDialog = false },
            onCodeScanned = { code ->
                showScannerDialog = false
                onSearchChange(code)
            }
        )
    }

    if (showFilterDialog) {
        VaultFilterDialog(
            currentFilter = uiState.filter,
            currentDatePreset = uiState.datePreset,
            filterOptions = uiState.filterOptions,
            language = language,
            onDismiss = { showFilterDialog = false },
            onClear = onClearFilters,
            onApply = onApplyFilter
        )
    }

    metadataEditTarget?.let { target ->
        VaultMetadataEditDialog(
            initialTitle = target.title,
            initialNotes = target.notes,
            initialTags = target.tags,
            language = language,
            onDismiss = { metadataEditTarget = null },
            onSave = { newTitle, newNotes, newTags ->
                metadataEditTarget = null
                onSaveMetadata(target.scopeType, target.sourceId, newTitle, newNotes, newTags)
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.pageBgTop),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.isBiometricFallbackNotice) {
            item(key = "bio_fallback") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StatusInfoContainer,
                    border = BorderStroke(1.dp, StatusInfo.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = StatusInfo)
                        Text(
                            text = if (isBn) {
                                "ডিভাইসে বায়োমেট্রিক সেট করা নেই — Owner/Admin ভূমিকার ভিত্তিতে নিরাপদে খোলা হয়েছে।"
                            } else {
                                "Device biometric unavailable/not enrolled — safe Owner/Admin fallback active."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusInfo
                        )
                    }
                }
            }
        }

        if (uiState.isLoadingOverview && overview == null) {
            item(key = "loading") {
                VaultCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = if (isBn) "আমদানি আর্কাইভ হিসাব করা হচ্ছে…" else "Computing Import Vault archive…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (uiState.errorMessage != null && overview == null) {
            item(key = "error") {
                VaultCard(borderColor = StatusError.copy(alpha = 0.5f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = StatusError)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBn) "তথ্য লোড করা যায়নি" else "Failed to load Import Vault",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusError
                            )
                            Text(
                                text = uiState.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = onRefresh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isBn) "আবার চেষ্টা করুন" else "Retry")
                    }
                }
            }
        } else if (overview != null) {
            item(key = "hero") {
                VaultHomeHeroSection(
                    overview = overview,
                    language = language,
                    expanded = expandHeroDetails,
                    isRefreshing = uiState.isLoadingOverview,
                    onToggleExpand = { expandHeroDetails = !expandHeroDetails },
                    onRefresh = onRefresh
                )
            }

            item(key = "analytics_shortcuts") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenPortfolioProfit,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Icon(Icons.Outlined.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "মুনাফা বিশ্লেষণ" else "Profit Intel",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = onOpenPortfolioCapital,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.secondary)
                        ) {
                            Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "মূলধন বিশ্লেষণ" else "Capital Intel",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenExport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isBn) "আর্কাইভ রিপোর্ট এক্সপোর্ট (PDF / XLSX)" else "Export Portfolio Report (PDF / XLSX)",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (overview.portfolioAttachments.isNotEmpty()) {
                item(key = "portfolio_snapshots_card") {
                    VaultExpandableSection(
                        title = if (isBn) "সংরক্ষিত পোর্টফোলিও স্ন্যাপশট ও ফাইল" else "Saved Portfolio Snapshots & Files",
                        subtitle = if (isBn) "অপরিবর্তনীয় রিপোর্ট স্ন্যাপশট ও যুক্ত নথি" else "Immutable report snapshots & attached files",
                        badgeText = Formatters.formatNumber(overview.portfolioAttachments.size.toLong(), language)
                    ) {
                        OutlinedButton(
                            onClick = onAddPortfolioAttachmentClick,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isBn) "পোর্টফোলিও ফাইল যোগ করুন" else "Attach Portfolio File")
                        }
                        overview.portfolioAttachments.forEach { doc ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(0.8.dp, colors.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Outlined.AttachFile, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isBn) doc.titleBn else doc.titleEn,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${Formatters.formatDate(doc.timestampMillis, language)} · ${if (doc.isImmutableSnapshot) (if (isBn) "স্ন্যাপশট (Immutable)" else "Immutable Snapshot") else doc.subtitle}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.onSurfaceVariant
                                            )
                                        }
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (!doc.localFilePath.isNullOrBlank()) {
                                            OutlinedButton(
                                                onClick = { onPreviewDocument(doc) },
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            ) {
                                                Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(if (isBn) "দেখুন" else "Preview")
                                            }
                                            OutlinedButton(
                                                onClick = { onShareDocument(doc) },
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            ) {
                                                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(if (isBn) "শেয়ার" else "Share")
                                            }
                                        }
                                        doc.attachmentId?.let { attId ->
                                            OutlinedButton(
                                                onClick = { onDeleteAttachment(attId) },
                                                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.5f)),
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            ) {
                                                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = colors.error, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(if (isBn) "মুছুন" else "Delete", color = colors.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (overview.unknownAttribution.unattributedRevenueMinor > 0L ||
                overview.unknownAttribution.unattributedInventoryCostMinor > 0L ||
                overview.unknownAttribution.unattributedCollectionsMinor > 0L
            ) {
                item(key = "unattributed_banner") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StatusWarningContainer,
                        border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(role = Role.Button, onClick = onOpenPortfolioCapital)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = StatusWarning)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBn) {
                                        "লিগ্যাসি / অনির্ধারিত ডাটা শনাক্ত (${ImportVaultUiLogic.formatPercent(overview.unknownAttribution.revenueAttributionCoveragePercent, language)} কভারেজ)"
                                    } else {
                                        "Legacy / Unattributed Bucket Active (${ImportVaultUiLogic.formatPercent(overview.unknownAttribution.revenueAttributionCoveragePercent, language)} coverage)"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusWarning
                                )
                                Text(
                                    text = if (isBn) {
                                        "অনুমাননির্ভর FIFO ছাড়াই আলাদা রাখা হয়েছে। বিস্তারিত দেখতে ট্যাপ করুন।"
                                    } else {
                                        "Isolated without guessed FIFO reconstruction. Tap for Capital breakdown."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item(key = "search_filter_controls") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.searchText,
                            onValueChange = onSearchChange,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            trailingIcon = {
                                Row {
                                    if (uiState.searchText.isNotEmpty()) {
                                        IconButton(onClick = { onSearchChange("") }) {
                                            Icon(Icons.Outlined.Close, contentDescription = null)
                                        }
                                    }
                                    IconButton(onClick = { showScannerDialog = true }) {
                                        Icon(
                                            Icons.Outlined.QrCodeScanner,
                                            contentDescription = if (isBn) "QR/বারকোড স্ক্যান" else "Scan QR/Barcode",
                                            tint = colors.primary
                                        )
                                    }
                                }
                            },
                            placeholder = {
                                Text(
                                    text = if (isBn) {
                                        "PO/চালান, সরবরাহকারী, SKU, বারকোড, কার্টন..."
                                    } else {
                                        "PO/Shipment, supplier, SKU, barcode, carton..."
                                    }
                                )
                            }
                        )
                        Surface(
                            modifier = Modifier
                                .heightIn(min = 56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(role = Role.Button) { showFilterDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            color = if (activeFilterCount > 0) colors.primaryContainer else colors.surface,
                            border = BorderStroke(1.dp, if (activeFilterCount > 0) colors.primary else colors.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.FilterList,
                                    contentDescription = if (isBn) "ফিল্টার" else "Filter",
                                    tint = colors.primary
                                )
                                if (activeFilterCount > 0) {
                                    Text(
                                        text = Formatters.formatNumber(activeFilterCount.toLong(), language),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary
                                    )
                                }
                            }
                        }
                    }

                    // Wrapped controls — no horizontal scrolling
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterOptionChip(
                            label = if (isBn) "চালান ভিউ (${Formatters.formatNumber(overview.shipmentCount.toLong(), language)})" else "Shipments (${overview.shipmentCount})",
                            selected = uiState.shelfMode == VaultShelfMode.SHIPMENTS,
                            onClick = { onShelfModeChange(VaultShelfMode.SHIPMENTS) }
                        )
                        FilterOptionChip(
                            label = if (isBn) "PO ভিউ (${Formatters.formatNumber(overview.purchaseOrderCount.toLong(), language)})" else "PO View (${overview.purchaseOrderCount})",
                            selected = uiState.shelfMode == VaultShelfMode.PURCHASE_ORDERS,
                            onClick = { onShelfModeChange(VaultShelfMode.PURCHASE_ORDERS) }
                        )
                        FilterOptionChip(
                            label = if (uiState.layoutMode == VaultLayoutMode.SHELF_GRID) {
                                if (isBn) "গ্রিড শেলফ" else "Shelf Grid"
                            } else {
                                if (isBn) "তালিকা ভিউ" else "Compact List"
                            },
                            selected = true,
                            onClick = {
                                val next = if (uiState.layoutMode == VaultLayoutMode.SHELF_GRID) {
                                    VaultLayoutMode.COMPACT_LIST
                                } else {
                                    VaultLayoutMode.SHELF_GRID
                                }
                                onLayoutModeChange(next)
                            }
                        )
                        FilterOptionChip(
                            label = when (uiState.archiveFilter) {
                                VaultArchiveFilter.ACTIVE -> if (isBn) "চলমান" else "Active"
                                VaultArchiveFilter.ARCHIVED -> if (isBn) "আর্কাইভড" else "Archived"
                                VaultArchiveFilter.ALL -> if (isBn) "সব" else "All"
                            },
                            selected = uiState.archiveFilter != VaultArchiveFilter.ACTIVE,
                            onClick = {
                                val next = when (uiState.archiveFilter) {
                                    VaultArchiveFilter.ACTIVE -> VaultArchiveFilter.ALL
                                    VaultArchiveFilter.ALL -> VaultArchiveFilter.ARCHIVED
                                    VaultArchiveFilter.ARCHIVED -> VaultArchiveFilter.ACTIVE
                                }
                                onArchiveFilterChange(next)
                            }
                        )
                    }
                }
            }

            if (uiState.shelfMode == VaultShelfMode.SHIPMENTS) {
                val (pinned, monthGroups) = ImportVaultUiLogic.groupShipmentsByYearMonth(
                    overview.shipments,
                    uiState.archiveFilter
                )
                if (pinned.isEmpty() && monthGroups.isEmpty()) {
                    item(key = "empty_shipments") {
                        VaultEmptyArchiveCard(
                            isBn = isBn,
                            onOpenImports = { onNavigateToRoute("import_management") },
                            onOpenPurchases = { onNavigateToRoute("purchases") }
                        )
                    }
                } else {
                    if (pinned.isNotEmpty()) {
                        item(key = "pinned_shipments_header") {
                            ShelfSectionLabel(
                                icon = Icons.Outlined.Bookmark,
                                title = if (isBn) "পিন করা চালান ফোল্ডার" else "Pinned Shipment Folders"
                            )
                        }
                        item(key = "pinned_shipments_body") {
                            ShipmentShelfItemsBlock(
                                items = pinned,
                                layoutMode = uiState.layoutMode,
                                language = language,
                                onOpenShipment = onOpenShipment,
                                onTogglePin = { s -> onTogglePin(VaultScopeType.SHIPMENT, s.shipmentId, s.isPinned) },
                                onToggleArchive = { s -> onToggleArchive(VaultScopeType.SHIPMENT, s.shipmentId, s.archiveStatus) },
                                onEditMetadata = { s ->
                                    metadataEditTarget = MetadataEditTarget(
                                        scopeType = VaultScopeType.SHIPMENT,
                                        sourceId = s.shipmentId,
                                        title = s.displayTitle,
                                        notes = s.notes,
                                        tags = s.tags
                                    )
                                }
                            )
                        }
                    }

                    monthGroups.forEach { group ->
                        val isExpanded = expandedMonths[group.yearMonthKey] ?: true
                        item(key = "shp_grp_${group.yearMonthKey}") {
                            MonthShelfHeaderCard(
                                title = ImportVaultUiLogic.formatYearMonthHeader(group.year, group.monthZeroBased, language),
                                itemCount = group.items.size,
                                totalInvestmentMinor = group.totalInvestmentMinor,
                                totalNetProfitMinor = group.totalNetProfitMinor,
                                expanded = isExpanded,
                                language = language,
                                onToggle = { expandedMonths[group.yearMonthKey] = !isExpanded }
                            )
                        }
                        if (isExpanded) {
                            item(key = "shp_items_${group.yearMonthKey}") {
                                ShipmentShelfItemsBlock(
                                    items = group.items,
                                    layoutMode = uiState.layoutMode,
                                    language = language,
                                    onOpenShipment = onOpenShipment,
                                    onTogglePin = { s -> onTogglePin(VaultScopeType.SHIPMENT, s.shipmentId, s.isPinned) },
                                    onToggleArchive = { s -> onToggleArchive(VaultScopeType.SHIPMENT, s.shipmentId, s.archiveStatus) },
                                    onEditMetadata = { s ->
                                        metadataEditTarget = MetadataEditTarget(
                                            scopeType = VaultScopeType.SHIPMENT,
                                            sourceId = s.shipmentId,
                                            title = s.displayTitle,
                                            notes = s.notes,
                                            tags = s.tags
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                val (pinned, monthGroups) = ImportVaultUiLogic.groupPurchaseOrdersByYearMonth(
                    overview.purchaseOrders,
                    uiState.archiveFilter
                )
                if (pinned.isEmpty() && monthGroups.isEmpty()) {
                    item(key = "empty_pos") {
                        VaultEmptyArchiveCard(
                            isBn = isBn,
                            onOpenImports = { onNavigateToRoute("import_management") },
                            onOpenPurchases = { onNavigateToRoute("purchases") }
                        )
                    }
                } else {
                    if (pinned.isNotEmpty()) {
                        item(key = "pinned_po_header") {
                            ShelfSectionLabel(
                                icon = Icons.Outlined.Bookmark,
                                title = if (isBn) "পিন করা ক্রয় আদেশ (PO)" else "Pinned Purchase Orders"
                            )
                        }
                        item(key = "pinned_po_body") {
                            PoShelfItemsBlock(
                                items = pinned,
                                layoutMode = uiState.layoutMode,
                                language = language,
                                onOpenPo = onOpenPo,
                                onTogglePin = { p -> onTogglePin(VaultScopeType.PURCHASE_ORDER, p.purchaseOrderId, p.isPinned) },
                                onToggleArchive = { p -> onToggleArchive(VaultScopeType.PURCHASE_ORDER, p.purchaseOrderId, p.archiveStatus) },
                                onEditMetadata = { p ->
                                    metadataEditTarget = MetadataEditTarget(
                                        scopeType = VaultScopeType.PURCHASE_ORDER,
                                        sourceId = p.purchaseOrderId,
                                        title = p.displayTitle,
                                        notes = p.notes,
                                        tags = p.tags
                                    )
                                }
                            )
                        }
                    }

                    monthGroups.forEach { group ->
                        val isExpanded = expandedMonths["po_${group.yearMonthKey}"] ?: true
                        item(key = "po_grp_${group.yearMonthKey}") {
                            MonthShelfHeaderCard(
                                title = ImportVaultUiLogic.formatYearMonthHeader(group.year, group.monthZeroBased, language),
                                itemCount = group.items.size,
                                totalInvestmentMinor = group.totalInvestmentMinor,
                                totalNetProfitMinor = group.totalNetProfitMinor,
                                expanded = isExpanded,
                                language = language,
                                onToggle = { expandedMonths["po_${group.yearMonthKey}"] = !isExpanded }
                            )
                        }
                        if (isExpanded) {
                            item(key = "po_items_${group.yearMonthKey}") {
                                PoShelfItemsBlock(
                                    items = group.items,
                                    layoutMode = uiState.layoutMode,
                                    language = language,
                                    onOpenPo = onOpenPo,
                                    onTogglePin = { p -> onTogglePin(VaultScopeType.PURCHASE_ORDER, p.purchaseOrderId, p.isPinned) },
                                    onToggleArchive = { p -> onToggleArchive(VaultScopeType.PURCHASE_ORDER, p.purchaseOrderId, p.archiveStatus) },
                                    onEditMetadata = { p ->
                                        metadataEditTarget = MetadataEditTarget(
                                            scopeType = VaultScopeType.PURCHASE_ORDER,
                                            sourceId = p.purchaseOrderId,
                                            title = p.displayTitle,
                                            notes = p.notes,
                                            tags = p.tags
                                        )
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

private data class MetadataEditTarget(
    val scopeType: VaultScopeType,
    val sourceId: String,
    val title: String,
    val notes: String?,
    val tags: List<String>
)

@Composable
private fun VaultHomeHeroSection(
    overview: ImportVaultOverview,
    language: AppLanguage,
    expanded: Boolean,
    isRefreshing: Boolean,
    onToggleExpand: () -> Unit,
    onRefresh: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val netProfitColor = if (overview.totalAttributedNetProfitMinor >= 0L) Color(0xFF42F5AA) else Color(0xFFFF9DB0)

    VaultHeroCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isBn) {
                        val base = "মোট অবদানকৃত নিট মুনাফা (${overview.profitLabel})"
                        if (overview.isPeriodActivityFiltered) "$base · সময়সীমা অ্যাক্টিভিটি" else base
                    } else {
                        if (overview.isPeriodActivityFiltered) "${overview.profitLabel} (Period Activity)" else overview.profitLabel
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = ImportVaultUiLogic.formatBdt(overview.totalAttributedNetProfitMinor, language),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = netProfitColor
                )
                Text(
                    text = if (isBn) {
                        val roiDisplay = if (overview.isPeriodActivityFiltered) {
                            "আজীবন ROI ${ImportVaultUiLogic.formatPercent(overview.lifetimePortfolioRoiPercent, language)}"
                        } else {
                            "ROI ${ImportVaultUiLogic.formatPercent(overview.portfolioRoiPercent, language)}"
                        }
                        "চালান ${Formatters.formatNumber(overview.shipmentCount.toLong(), language)} · ক্রয় আদেশ ${Formatters.formatNumber(overview.purchaseOrderCount.toLong(), language)} · $roiDisplay"
                    } else {
                        val roiDisplay = if (overview.isPeriodActivityFiltered) {
                            "Lifetime ROI ${ImportVaultUiLogic.formatPercent(overview.lifetimePortfolioRoiPercent, language)}"
                        } else {
                            "ROI ${ImportVaultUiLogic.formatPercent(overview.portfolioRoiPercent, language)}"
                        }
                        "Shipments ${overview.shipmentCount} · POs ${overview.purchaseOrderCount} · $roiDisplay"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(48.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, tint = Color.White)
                }
            }
        }

        // Primary concise 2x2 white-on-dark KPI strip
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroMiniMetric(
                label = if (isBn) "বিনিয়োগ" else "Investment",
                value = ImportVaultUiLogic.formatBdt(overview.totalInvestedCapitalMinor, language),
                modifier = Modifier.weight(1f)
            )
            HeroMiniMetric(
                label = if (isBn) "নিট বিক্রয়" else "Net Sales",
                value = ImportVaultUiLogic.formatBdt(overview.totalAttributedNetRevenueMinor, language),
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroMiniMetric(
                label = if (isBn) "বদ্ধ স্টক মূল্য" else "Locked Stock",
                value = ImportVaultUiLogic.formatBdt(overview.totalLockedInventoryCapitalMinor, language),
                modifier = Modifier.weight(1f)
            )
            HeroMiniMetric(
                label = if (isBn) "বকেয়া (Attributable)" else "Attributable Due",
                value = ImportVaultUiLogic.formatBdt(overview.cashRecovery.attributableReceivableMinor, language),
                modifier = Modifier.weight(1f)
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onToggleExpand),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (expanded) {
                        if (isBn) "বিস্তারিত কম দেখুন" else "Hide Portfolio Metrics"
                    } else {
                        if (isBn) "মার্জিন, ওভারহেড ও কভারেজ দেখুন" else "Show Margin, Overhead & Coverage"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroMiniMetric(
                        label = if (isBn) "গ্রস মুনাফা" else "Gross Profit",
                        value = ImportVaultUiLogic.formatBdt(overview.totalAttributedGrossProfitMinor, language),
                        modifier = Modifier.weight(1f)
                    )
                    HeroMiniMetric(
                        label = if (isBn) "নিট মার্জিন %" else "Net Margin %",
                        value = ImportVaultUiLogic.formatPercent(overview.attributedNetMarginPercent, language),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroMiniMetric(
                        label = if (isBn) "অবন্টিত ওভারহেড" else "Unallocated Overhead",
                        value = ImportVaultUiLogic.formatBdt(overview.unallocatedSharedOverheadMinor, language),
                        modifier = Modifier.weight(1f)
                    )
                    HeroMiniMetric(
                        label = if (isBn) "বিক্রয় কভারেজ" else "Attribution Coverage",
                        value = ImportVaultUiLogic.formatPercent(overview.unknownAttribution.revenueAttributionCoveragePercent, language),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (overview.isPeriodActivityFiltered &&
                    overview.openingStockCostMinor != null &&
                    overview.closingStockCostMinor != null
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeroMiniMetric(
                            label = if (isBn) "প্রারম্ভিক স্টক (Opening)" else "Opening Stock",
                            value = ImportVaultUiLogic.formatBdt(overview.openingStockCostMinor, language),
                            modifier = Modifier.weight(1f)
                        )
                        HeroMiniMetric(
                            label = if (isBn) "সমাপনী স্টক (Closing)" else "Closing Stock",
                            value = ImportVaultUiLogic.formatBdt(overview.closingStockCostMinor, language),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMiniMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun ShelfSectionLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun MonthShelfHeaderCard(
    title: String,
    itemCount: Int,
    totalInvestmentMinor: Long,
    totalNetProfitMinor: Long,
    expanded: Boolean,
    language: AppLanguage,
    onToggle: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onToggle),
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.FolderSpecial, contentDescription = null, tint = colors.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$title (${Formatters.formatNumber(itemCount.toLong(), language)})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = "${if (isBn) "বিনিয়োগ" else "Inv"} ${ImportVaultUiLogic.formatBdt(totalInvestmentMinor, language)} · ${if (isBn) "মুনাফা" else "Profit"} ${ImportVaultUiLogic.formatBdt(totalNetProfitMinor, language)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (totalNetProfitMinor >= 0L) StatusSuccess else StatusError
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShipmentShelfItemsBlock(
    items: List<ShipmentVaultSummary>,
    layoutMode: VaultLayoutMode,
    language: AppLanguage,
    onOpenShipment: (String) -> Unit,
    onTogglePin: (ShipmentVaultSummary) -> Unit,
    onToggleArchive: (ShipmentVaultSummary) -> Unit,
    onEditMetadata: (ShipmentVaultSummary) -> Unit
) {
    if (layoutMode == VaultLayoutMode.SHELF_GRID) {
        val rows = items.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShipmentFolderCard(
                        shipment = pair[0],
                        compact = true,
                        language = language,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenShipment(pair[0].shipmentId) },
                        onTogglePin = { onTogglePin(pair[0]) },
                        onToggleArchive = { onToggleArchive(pair[0]) },
                        onEditMetadata = { onEditMetadata(pair[0]) }
                    )
                    if (pair.size > 1) {
                        ShipmentFolderCard(
                            shipment = pair[1],
                            compact = true,
                            language = language,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenShipment(pair[1].shipmentId) },
                            onTogglePin = { onTogglePin(pair[1]) },
                            onToggleArchive = { onToggleArchive(pair[1]) },
                            onEditMetadata = { onEditMetadata(pair[1]) }
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { shp ->
                ShipmentFolderCard(
                    shipment = shp,
                    compact = false,
                    language = language,
                    onClick = { onOpenShipment(shp.shipmentId) },
                    onTogglePin = { onTogglePin(shp) },
                    onToggleArchive = { onToggleArchive(shp) },
                    onEditMetadata = { onEditMetadata(shp) }
                )
            }
        }
    }
}

@Composable
private fun ShipmentFolderCard(
    shipment: ShipmentVaultSummary,
    compact: Boolean,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onEditMetadata: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    var menuExpanded by remember { mutableStateOf(false) }

    VaultCard(
        modifier = modifier,
        borderColor = if (shipment.isPinned) colors.primary.copy(alpha = 0.5f) else colors.outlineVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (shipment.isPinned) Icons.Outlined.Bookmark else Icons.Outlined.FolderSpecial,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shipment.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                if (shipment.displayTitle != shipment.shipmentNumber) {
                    Text(
                        text = shipment.shipmentNumber,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary
                    )
                }
                Text(
                    text = shipment.supplierNames.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                Text(
                    text = "${Formatters.formatDate(shipment.shippingDate, language)} · ${shipment.purchaseOrderNumbers.size} PO · ${shipment.productCount} ${if (isBn) "পণ্য" else "SKUs"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = colors.onSurfaceVariant)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (shipment.isPinned) (if (isBn) "আনপিন" else "Unpin") else (if (isBn) "পিন করুন" else "Pin")) },
                        leadingIcon = { Icon(if (shipment.isPinned) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, null) },
                        onClick = { menuExpanded = false; onTogglePin() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isBn) "শিরোনাম/ট্যাগ/নোট" else "Rename / Tags / Notes") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                        onClick = { menuExpanded = false; onEditMetadata() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (shipment.archiveStatus == "ARCHIVED") (if (isBn) "আনআর্কাইভ" else "Unarchive") else (if (isBn) "আর্কাইভ" else "Archive")) },
                        leadingIcon = { Icon(Icons.Outlined.Archive, null) },
                        onClick = { menuExpanded = false; onToggleArchive() }
                    )
                }
            }
        }

        VaultStatusBadgeRow(
            operationalStatus = shipment.operationalStatus,
            commercialStatus = shipment.commercialStatus,
            costFinalizationStatus = shipment.costFinalizationStatus,
            language = language
        )

        if (shipment.tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                shipment.tags.forEach { tag ->
                    VaultStatusBadge(
                        label = "#$tag",
                        containerColor = colors.secondaryContainer.copy(alpha = 0.7f),
                        contentColor = colors.onSecondaryContainer
                    )
                }
            }
        }

        VaultKpiPairRow(
            leftLabel = if (isBn) "বিনিয়োগ" else "Investment",
            leftValue = ImportVaultUiLogic.formatBdt(shipment.totalInvestedCapitalMinor, language),
            rightLabel = if (isBn) "নিট মুনাফা" else "Net Profit",
            rightValue = ImportVaultUiLogic.formatBdt(shipment.attributedNetProfitMinor, language),
            rightColor = if (shipment.attributedNetProfitMinor >= 0L) StatusSuccess else StatusError
        )

        if (!compact) {
            VaultKpiPairRow(
                leftLabel = if (isBn) "নিট বিক্রয়" else "Net Sales",
                leftValue = ImportVaultUiLogic.formatBdt(shipment.netSalesRevenueMinor, language),
                rightLabel = if (isBn) "স্টক মূল্য" else "Stock Value",
                rightValue = ImportVaultUiLogic.formatBdt(shipment.lockedInventoryCapitalMinor, language)
            )
        }

        val stRatio = ((shipment.sellThroughPercent ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${if (isBn) "বিক্রি হার" else "Sold"} ${ImportVaultUiLogic.formatPercent(shipment.sellThroughPercent, language)} (${ImportVaultUiLogic.formatQuantityByUnit(shipment.netSoldQtyByUnit, language)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                Text(
                    text = "${if (isBn) "মার্জিন" else "Margin"} ${ImportVaultUiLogic.formatPercent(shipment.netMarginPercent, language)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
            }
            LinearProgressIndicator(
                progress = { stRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = colors.secondary,
                trackColor = colors.surfaceVariant
            )
        }
    }
}

@Composable
private fun PoShelfItemsBlock(
    items: List<PurchaseOrderVaultSummary>,
    layoutMode: VaultLayoutMode,
    language: AppLanguage,
    onOpenPo: (String) -> Unit,
    onTogglePin: (PurchaseOrderVaultSummary) -> Unit,
    onToggleArchive: (PurchaseOrderVaultSummary) -> Unit,
    onEditMetadata: (PurchaseOrderVaultSummary) -> Unit
) {
    if (layoutMode == VaultLayoutMode.SHELF_GRID) {
        val rows = items.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PoFolderCard(
                        po = pair[0],
                        compact = true,
                        language = language,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenPo(pair[0].purchaseOrderId) },
                        onTogglePin = { onTogglePin(pair[0]) },
                        onToggleArchive = { onToggleArchive(pair[0]) },
                        onEditMetadata = { onEditMetadata(pair[0]) }
                    )
                    if (pair.size > 1) {
                        PoFolderCard(
                            po = pair[1],
                            compact = true,
                            language = language,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenPo(pair[1].purchaseOrderId) },
                            onTogglePin = { onTogglePin(pair[1]) },
                            onToggleArchive = { onToggleArchive(pair[1]) },
                            onEditMetadata = { onEditMetadata(pair[1]) }
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { po ->
                PoFolderCard(
                    po = po,
                    compact = false,
                    language = language,
                    onClick = { onOpenPo(po.purchaseOrderId) },
                    onTogglePin = { onTogglePin(po) },
                    onToggleArchive = { onToggleArchive(po) },
                    onEditMetadata = { onEditMetadata(po) }
                )
            }
        }
    }
}

@Composable
private fun PoFolderCard(
    po: PurchaseOrderVaultSummary,
    compact: Boolean,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onEditMetadata: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    var menuExpanded by remember { mutableStateOf(false) }

    VaultCard(
        modifier = modifier,
        borderColor = if (po.isPinned) colors.primary.copy(alpha = 0.5f) else colors.outlineVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (po.isPinned) Icons.Outlined.Bookmark else Icons.Outlined.MenuBook,
                contentDescription = null,
                tint = colors.secondary,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = po.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                if (po.displayTitle != po.poNumber) {
                    Text(po.poNumber, style = MaterialTheme.typography.labelSmall, color = colors.primary)
                }
                Text(po.supplierName, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                val shipmentTag = if (po.shipmentNumbers.isEmpty()) {
                    if (isBn) "কোনো চালানে যুক্ত নয়" else "No linked shipment"
                } else {
                    po.shipmentNumbers.joinToString(", ")
                }
                Text(
                    text = "${Formatters.formatDate(po.orderDate, language)} · $shipmentTag · ${po.productCount} ${if (isBn) "পণ্য" else "SKUs"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = colors.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(if (po.isPinned) (if (isBn) "আনপিন" else "Unpin") else (if (isBn) "পিন করুন" else "Pin")) },
                        leadingIcon = { Icon(if (po.isPinned) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, null) },
                        onClick = { menuExpanded = false; onTogglePin() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isBn) "শিরোনাম/ট্যাগ/নোট" else "Rename / Tags / Notes") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                        onClick = { menuExpanded = false; onEditMetadata() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (po.archiveStatus == "ARCHIVED") (if (isBn) "আনআর্কাইভ" else "Unarchive") else (if (isBn) "আর্কাইভ" else "Archive")) },
                        leadingIcon = { Icon(Icons.Outlined.Archive, null) },
                        onClick = { menuExpanded = false; onToggleArchive() }
                    )
                }
            }
        }

        VaultStatusBadgeRow(
            operationalStatus = po.operationalStatus,
            commercialStatus = po.commercialStatus,
            costFinalizationStatus = po.costFinalizationStatus,
            language = language
        )

        VaultKpiPairRow(
            leftLabel = if (isBn) "বিনিয়োগ (BDT)" else "Investment (BDT)",
            leftValue = ImportVaultUiLogic.formatBdt(po.totalInvestedCapitalMinor, language),
            rightLabel = if (isBn) "নিট মুনাফা" else "Net Profit",
            rightValue = ImportVaultUiLogic.formatBdt(po.attributedNetProfitMinor, language),
            rightColor = if (po.attributedNetProfitMinor >= 0L) StatusSuccess else StatusError
        )

        if (!compact && po.originalCurrency != "BDT") {
            Text(
                text = "${if (isBn) "ক্রয়মূল্য" else "PO Cost"}: ${ImportVaultUiLogic.formatCurrency(po.originalTotalAmountMinor, po.originalCurrency, language)} @ ${po.exchangeRateToBdt}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }

        val stRatio = ((po.sellThroughPercent ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${if (isBn) "বিক্রি হার" else "Sold"} ${ImportVaultUiLogic.formatPercent(po.sellThroughPercent, language)} (${ImportVaultUiLogic.formatQuantityByUnit(po.netSoldQtyByUnit, language)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                Text(
                    text = "${if (isBn) "বকেয়া" else "Payable"} ${ImportVaultUiLogic.formatBdt(po.supplierPayableBdtMinor, language)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { stRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = colors.primary,
                trackColor = colors.surfaceVariant
            )
        }
    }
}

@Composable
private fun VaultEmptyArchiveCard(
    isBn: Boolean,
    onOpenImports: () -> Unit,
    onOpenPurchases: () -> Unit
) {
    VaultCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = if (isBn) "কোনো আমদানি রেকর্ড পাওয়া যায়নি" else "No matching Import Vault records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isBn) {
                    "ক্রয় আদেশ বা চালান তৈরি করলে স্বয়ংক্রিয়ভাবে এখানে ডিজিটাল ফোল্ডার তৈরি হবে।"
                } else {
                    "Digital archive folders appear automatically as purchase orders and shipments are created."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenPurchases,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(if (isBn) "ক্রয় মডিউল" else "Purchases")
                }
                Button(
                    onClick = onOpenImports,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(if (isBn) "আমদানি ব্যবস্থাপনা" else "Import Mgmt")
                }
            }
        }
    }
}

private fun countActiveFilters(filter: ImportVaultFilter, preset: VaultDatePreset): Int {
    var count = 0
    if (preset != VaultDatePreset.LIFETIME) count++
    if (filter.dateFilterTarget == VaultDateFilterTarget.IMPORT_COHORT_DATES && preset != VaultDatePreset.LIFETIME) count++
    if (filter.supplierId != null) count++
    if (filter.warehouseId != null) count++
    if (filter.operationalStatus != null) count++
    if (filter.commercialStatus != null) count++
    if (filter.onlyProfitable) count++
    if (filter.onlyLossMaking) count++
    if (filter.onlyHighAging) count++
    if (filter.onlyPendingCost) count++
    if (filter.allocationRuleId != null) count++
    return count
}
