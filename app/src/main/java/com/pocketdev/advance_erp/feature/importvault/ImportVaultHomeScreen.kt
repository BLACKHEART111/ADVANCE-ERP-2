@file:OptIn(ExperimentalLayoutApi::class)

package com.pocketdev.advance_erp.feature.importvault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import com.pocketdev.advance_erp.core.designsystem.AutoShrinkSingleLineText
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.advance_erp.core.designsystem.DashboardColorTokens
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
            .background(Brush.verticalGradient(listOf(tokens.pageBgTop, tokens.pageBgBottom))),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultQuickActionTile(
                        title = if (isBn) "মুনাফা" else "Profit",
                        subtitle = if (isBn) "বিশ্লেষণ" else "Analytics",
                        icon = Icons.Outlined.Assessment,
                        accentColor = tokens.accentPurple,
                        tokens = tokens,
                        onClick = onOpenPortfolioProfit,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    VaultQuickActionTile(
                        title = if (isBn) "মূলধন" else "Capital",
                        subtitle = if (isBn) "স্টক ও নগদ" else "Stock & Cash",
                        icon = Icons.Outlined.AccountBalanceWallet,
                        accentColor = tokens.accentTeal,
                        tokens = tokens,
                        onClick = onOpenPortfolioCapital,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    VaultQuickActionTile(
                        title = if (isBn) "এক্সপোর্ট" else "Export",
                        subtitle = "PDF / XLSX",
                        icon = Icons.Outlined.Download,
                        accentColor = tokens.accentBlue,
                        tokens = tokens,
                        onClick = onOpenExport,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.searchText,
                            onValueChange = onSearchChange,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 54.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = tokens.dateCardBg,
                                unfocusedContainerColor = tokens.dateCardBg,
                                focusedBorderColor = tokens.accentPurple,
                                unfocusedBorderColor = tokens.cardBorderStart
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = tokens.sectionIconTint
                                )
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (uiState.searchText.isNotEmpty()) {
                                        IconButton(onClick = { onSearchChange("") }) {
                                            Icon(Icons.Outlined.Close, contentDescription = null)
                                        }
                                    }
                                    IconButton(onClick = { showScannerDialog = true }) {
                                        Icon(
                                            Icons.Outlined.QrCodeScanner,
                                            contentDescription = if (isBn) "QR/বারকোড স্ক্যান" else "Scan QR/Barcode",
                                            tint = tokens.accentPurple
                                        )
                                    }
                                }
                            },
                            placeholder = {
                                Text(
                                    text = if (isBn) {
                                        "খুঁজুন: PO, চালান, SKU, কার্টন..."
                                    } else {
                                        "Search PO, shipment, SKU..."
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                        val filterShape = RoundedCornerShape(14.dp)
                        Surface(
                            modifier = Modifier
                                .heightIn(min = 54.dp)
                                .then(
                                    if (!tokens.isDark) {
                                        Modifier.shadow(2.dp, filterShape, ambientColor = tokens.accentPurple.copy(alpha = 0.1f))
                                    } else {
                                        Modifier
                                    }
                                )
                                .clip(filterShape)
                                .clickable(role = Role.Button) { showFilterDialog = true },
                            shape = filterShape,
                            color = when {
                                activeFilterCount > 0 && tokens.isDark -> Color(0xFF281D4B)
                                activeFilterCount > 0 -> colors.primaryContainer
                                else -> tokens.dateCardBg
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (activeFilterCount > 0) tokens.accentPurple else tokens.cardBorderStart
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.FilterList,
                                    contentDescription = if (isBn) "ফিল্টার" else "Filter",
                                    tint = tokens.accentPurple
                                )
                                if (activeFilterCount > 0) {
                                    Text(
                                        text = Formatters.formatNumber(activeFilterCount.toLong(), language),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = tokens.accentPurple
                                    )
                                }
                            }
                        }
                    }

                    VaultControlTierBar(
                        shipmentCount = overview.shipmentCount,
                        purchaseOrderCount = overview.purchaseOrderCount,
                        shelfMode = uiState.shelfMode,
                        layoutMode = uiState.layoutMode,
                        archiveFilter = uiState.archiveFilter,
                        language = language,
                        tokens = tokens,
                        onShelfModeChange = onShelfModeChange,
                        onLayoutModeChange = onLayoutModeChange,
                        onArchiveFilterChange = onArchiveFilterChange
                    )
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
                        val localizedLabel = when {
                            overview.profitLabel.equals("Attributed Net Profit", ignoreCase = true) ->
                                "মোট অবদানকৃত নিট মুনাফা"
                            overview.profitLabel.contains("Overhead", ignoreCase = true) ||
                                overview.profitLabel.contains("Allocated", ignoreCase = true) ->
                                "ওভারহেড সমন্বিত নিট মুনাফা"
                            else -> "মোট অবদানকৃত নিট মুনাফা"
                        }
                        if (overview.isPeriodActivityFiltered) "$localizedLabel · সময়সীমা অ্যাক্টিভিটি" else localizedLabel
                    } else {
                        if (overview.isPeriodActivityFiltered) "${overview.profitLabel} (Period Activity)" else overview.profitLabel
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
                AutoShrinkSingleLineText(
                    text = ImportVaultUiLogic.formatBdtNoBreak(overview.totalAttributedNetProfitMinor, language),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = netProfitColor,
                    minFontSizeSp = 18f
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeroMiniMetric(
                label = if (isBn) "বিনিয়োগ" else "Investment",
                value = ImportVaultUiLogic.formatBdt(overview.totalInvestedCapitalMinor, language),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            HeroMiniMetric(
                label = if (isBn) "নিট বিক্রয়" else "Net Sales",
                value = ImportVaultUiLogic.formatBdt(overview.totalAttributedNetRevenueMinor, language),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeroMiniMetric(
                label = if (isBn) "বদ্ধ স্টক মূল্য" else "Locked Stock",
                value = ImportVaultUiLogic.formatBdt(overview.totalLockedInventoryCapitalMinor, language),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            HeroMiniMetric(
                label = if (isBn) "বিক্রয় বকেয়া" else "Attributable Due",
                value = ImportVaultUiLogic.formatBdtNoBreak(overview.cashRecovery.attributableReceivableMinor, language),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onToggleExpand),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.12f),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.16f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
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
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroMiniMetric(
                        label = if (isBn) "গ্রস মুনাফা" else "Gross Profit",
                        value = ImportVaultUiLogic.formatBdt(overview.totalAttributedGrossProfitMinor, language),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    HeroMiniMetric(
                        label = if (isBn) "নিট মার্জিন %" else "Net Margin %",
                        value = ImportVaultUiLogic.formatPercent(overview.attributedNetMarginPercent, language),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroMiniMetric(
                        label = if (isBn) "অবন্টিত ওভারহেড" else "Unallocated Overhead",
                        value = ImportVaultUiLogic.formatBdt(overview.unallocatedSharedOverheadMinor, language),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    HeroMiniMetric(
                        label = if (isBn) "বিক্রয় কভারেজ" else "Attribution Coverage",
                        value = ImportVaultUiLogic.formatPercent(overview.unknownAttribution.revenueAttributionCoveragePercent, language),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                if (overview.isPeriodActivityFiltered &&
                    overview.openingStockCostMinor != null &&
                    overview.closingStockCostMinor != null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeroMiniMetric(
                            label = if (isBn) "প্রারম্ভিক স্টক" else "Opening Stock",
                            value = ImportVaultUiLogic.formatBdtNoBreak(overview.openingStockCostMinor, language),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        HeroMiniMetric(
                            label = if (isBn) "সমাপনী স্টক" else "Closing Stock",
                            value = ImportVaultUiLogic.formatBdtNoBreak(overview.closingStockCostMinor, language),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
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
        color = Color.White.copy(alpha = 0.11f),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AutoShrinkSingleLineText(
                text = value,
                style = LocalTextStyle.current.copy(
                    fontSize = 14.5.sp,
                    lineHeight = 19.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                minFontSizeSp = 11f
            )
        }
    }
}

@Composable
private fun VaultQuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    tokens: DashboardColorTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val borderBrush = Brush.horizontalGradient(listOf(tokens.reportBtnBorderStart, tokens.reportBtnBorderEnd))
    Box(
        modifier = modifier
            .heightIn(min = 64.dp)
            .then(
                if (!tokens.isDark) {
                    Modifier.shadow(2.dp, shape, ambientColor = accentColor.copy(alpha = 0.12f))
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(tokens.reportBtnBgStart, tokens.reportBtnBgEnd)))
            .border(1.dp, borderBrush, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = if (tokens.isDark) 0.24f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (tokens.isDark) Color.White else accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.reportBtnText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = tokens.sectionSubtitleText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VaultControlTierBar(
    shipmentCount: Int,
    purchaseOrderCount: Int,
    shelfMode: VaultShelfMode,
    layoutMode: VaultLayoutMode,
    archiveFilter: VaultArchiveFilter,
    language: AppLanguage,
    tokens: DashboardColorTokens,
    onShelfModeChange: (VaultShelfMode) -> Unit,
    onLayoutModeChange: (VaultLayoutMode) -> Unit,
    onArchiveFilterChange: (VaultArchiveFilter) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    val trackColor = if (tokens.isDark) Color(0xFF151830) else Color(0xFFF0E8F9)
    val trackBorder = if (tokens.isDark) Color(0xFF2D365F) else tokens.cardBorderStart

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Tier 1: Full-width segmented scope toggle (Shipments vs PO View)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = trackColor,
            border = BorderStroke(1.dp, trackBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val scopeOptions = listOf(
                    VaultShelfMode.SHIPMENTS to (
                        if (isBn) "চালান ভিউ (${Formatters.formatNumber(shipmentCount.toLong(), language)})"
                        else "Shipments ($shipmentCount)"
                    ),
                    VaultShelfMode.PURCHASE_ORDERS to (
                        if (isBn) "PO ভিউ (${Formatters.formatNumber(purchaseOrderCount.toLong(), language)})"
                        else "PO View ($purchaseOrderCount)"
                    )
                )
                scopeOptions.forEach { (mode, label) ->
                    val selected = shelfMode == mode
                    val pillShape = RoundedCornerShape(11.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .clip(pillShape)
                            .then(
                                if (selected) {
                                    Modifier.background(
                                        Brush.horizontalGradient(listOf(tokens.heroBgStart, tokens.accentPurple))
                                    )
                                } else {
                                    Modifier.background(Color.Transparent)
                                }
                            )
                            .clickable(role = Role.Tab) { onShelfModeChange(mode) }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (selected) Color.White else tokens.sectionTitleText,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Tier 2: Archive filter pills (left) + Compact Card/Grid toggle (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val archiveOptions = listOf(
                    VaultArchiveFilter.ACTIVE to (if (isBn) "চলমান" else "Active"),
                    VaultArchiveFilter.ARCHIVED to (if (isBn) "আর্কাইভড" else "Archived"),
                    VaultArchiveFilter.ALL to (if (isBn) "সব" else "All")
                )
                archiveOptions.forEach { (filter, label) ->
                    val selected = archiveFilter == filter
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.Tab) { onArchiveFilterChange(filter) },
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            selected && tokens.isDark -> Color(0xFF281D4B)
                            selected -> colors.primaryContainer
                            else -> tokens.cardBgStart
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) tokens.accentPurple else trackBorder
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) tokens.accentPurple else tokens.sectionSubtitleText,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = trackColor,
                border = BorderStroke(1.dp, trackBorder)
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val layoutModes = listOf(
                        Triple(
                            VaultLayoutMode.COMPACT_LIST,
                            Icons.Outlined.ViewAgenda,
                            if (isBn) "কার্ড ভিউ" else "Card View"
                        ),
                        Triple(
                            VaultLayoutMode.SHELF_GRID,
                            Icons.Outlined.GridView,
                            if (isBn) "গ্রিড শেলফ" else "Grid Shelf"
                        )
                    )
                    layoutModes.forEach { (mode, icon, desc) ->
                        val selected = layoutMode == mode
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) tokens.accentPurple else Color.Transparent)
                                .clickable(role = Role.Tab) { onLayoutModeChange(mode) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = desc,
                                tint = if (selected) Color.White else tokens.sectionSubtitleText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
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
    val tokens = LocalDashboardTokens.current
    val shape = RoundedCornerShape(16.dp)
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "month_shelf_chevron"
    )
    val countText = Formatters.formatNumber(itemCount.toLong(), language)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!tokens.isDark) {
                    Modifier.shadow(2.dp, shape, ambientColor = tokens.accentPurple.copy(alpha = 0.1f))
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(tokens.cardBgStart, tokens.cardBgEnd)))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(tokens.cardBorderStart, tokens.cardBorderEnd)),
                shape = shape
            )
            .clickable(role = Role.Button, onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tokens.accentPurple.copy(alpha = if (tokens.isDark) 0.24f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderSpecial,
                    contentDescription = null,
                    tint = if (tokens.isDark) Color.White else tokens.accentPurple,
                    modifier = Modifier.size(19.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.sectionTitleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = tokens.accentPurple.copy(alpha = if (tokens.isDark) 0.24f else 0.12f)
                    ) {
                        Text(
                            text = countText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.accentPurple,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${if (isBn) "বিনিয়োগ" else "Inv"} ${ImportVaultUiLogic.formatBdt(totalInvestmentMinor, language)}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.sectionSubtitleText,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "${if (isBn) "মুনাফা" else "Profit"} ${ImportVaultUiLogic.formatBdt(totalNetProfitMinor, language)}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalNetProfitMinor >= 0L) StatusSuccess else StatusError,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (tokens.isDark) Color(0xFF202546) else Color(0xFFF0E8F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = tokens.sectionIconTint,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(chevronRotation)
                )
            }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShipmentFolderCard(
                        shipment = pair[0],
                        compact = true,
                        language = language,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
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
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
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
    val tokens = LocalDashboardTokens.current
    val colors = MaterialTheme.colorScheme
    val poCountStr = Formatters.formatNumber(shipment.purchaseOrderNumbers.size.toLong(), language)
    val productCountStr = Formatters.formatNumber(shipment.productCount.toLong(), language)
    val supplierText = shipment.supplierNames.joinToString(", ")
    val metaLine = "${Formatters.formatDate(shipment.shippingDate, language)} · $poCountStr PO · $productCountStr ${if (isBn) "পণ্য" else "SKUs"}"
    val stRatio = ((shipment.sellThroughPercent ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    val profitColor = if (shipment.attributedNetProfitMinor >= 0L) StatusSuccess else StatusError

    VaultCard(
        modifier = modifier,
        borderColor = if (shipment.isPinned) tokens.accentPurple.copy(alpha = 0.6f) else null,
        contentPadding = if (compact) PaddingValues(11.dp) else PaddingValues(14.dp),
        verticalSpacing = if (compact) 8.dp else 10.dp,
        onClick = onClick
    ) {
        if (compact) {
            // Re-engineered 2-column grid header: icon + status pill + menu; full width below for title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tokens.accentPurple.copy(alpha = if (tokens.isDark) 0.24f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (shipment.isPinned) Icons.Outlined.Bookmark else Icons.Outlined.FolderSpecial,
                        contentDescription = null,
                        tint = if (tokens.isDark) Color.White else tokens.accentPurple,
                        modifier = Modifier.size(16.dp)
                    )
                }
                val (opBg, opFg) = operationalBadgeColors(shipment.operationalStatus, colors)
                VaultStatusBadge(
                    label = operationalStatusLabel(shipment.operationalStatus, language),
                    containerColor = opBg,
                    contentColor = opFg
                )
                Spacer(modifier = Modifier.weight(1f))
                FolderOverflowMenu(
                    isPinned = shipment.isPinned,
                    archiveStatus = shipment.archiveStatus,
                    isBn = isBn,
                    buttonSize = 30.dp,
                    iconSize = 18.dp,
                    onTogglePin = onTogglePin,
                    onEditMetadata = onEditMetadata,
                    onToggleArchive = onToggleArchive
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = shipment.displayTitle,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.cardValueText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (shipment.displayTitle != shipment.shipmentNumber) {
                    Text(
                        text = shipment.shipmentNumber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.accentPurple,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (supplierText.isNotBlank()) {
                    Text(
                        text = supplierText,
                        fontSize = 11.sp,
                        color = tokens.sectionSubtitleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = metaLine,
                    fontSize = 10.5.sp,
                    color = tokens.sectionSubtitleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val (comBg, comFg) = commercialBadgeColors(shipment.commercialStatus, colors)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VaultStatusBadge(
                    label = commercialStatusLabel(shipment.commercialStatus, language),
                    containerColor = comBg,
                    contentColor = comFg
                )
                if (shipment.costFinalizationStatus == com.pocketdev.advance_erp.domain.model.CostFinalizationStatus.PROVISIONAL) {
                    VaultStatusBadge(
                        label = if (isBn) "অসমাপ্ত" else "Provisional",
                        containerColor = com.pocketdev.advance_erp.core.designsystem.StatusWarningContainer,
                        contentColor = com.pocketdev.advance_erp.core.designsystem.StatusWarning
                    )
                }
                shipment.tags.take(2).forEach { tag ->
                    VaultStatusBadge(
                        label = "#$tag",
                        containerColor = colors.secondaryContainer.copy(alpha = 0.7f),
                        contentColor = colors.onSecondaryContainer
                    )
                }
            }

            CompactGridMetricsBox(
                leftLabel = if (isBn) "বিনিয়োগ" else "Invested",
                leftValue = ImportVaultUiLogic.formatBdt(shipment.totalInvestedCapitalMinor, language),
                rightLabel = if (isBn) "নিট মুনাফা" else "Net Profit",
                rightValue = ImportVaultUiLogic.formatBdt(shipment.attributedNetProfitMinor, language),
                rightColor = profitColor,
                tokens = tokens
            )

            FolderProgressFooter(
                leftText = "${if (isBn) "বিক্রি" else "Sold"} ${ImportVaultUiLogic.formatPercentCompact(shipment.sellThroughPercent, language)}",
                rightText = "${if (isBn) "মার্জিন" else "Mgn"} ${ImportVaultUiLogic.formatPercentCompact(shipment.netMarginPercent, language)}",
                progress = stRatio,
                barColor = tokens.accentTeal,
                compact = true,
                tokens = tokens
            )
        } else {
            // Full-width premium Card View
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(tokens.accentPurple.copy(alpha = if (tokens.isDark) 0.24f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (shipment.isPinned) Icons.Outlined.Bookmark else Icons.Outlined.FolderSpecial,
                        contentDescription = null,
                        tint = if (tokens.isDark) Color.White else tokens.accentPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = shipment.displayTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.cardValueText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (shipment.displayTitle != shipment.shipmentNumber) {
                        Text(
                            text = shipment.shipmentNumber,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.accentPurple,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (supplierText.isNotBlank()) {
                        Text(
                            text = supplierText,
                            fontSize = 12.sp,
                            color = tokens.sectionSubtitleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = metaLine,
                        fontSize = 11.sp,
                        color = tokens.sectionSubtitleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FolderOverflowMenu(
                    isPinned = shipment.isPinned,
                    archiveStatus = shipment.archiveStatus,
                    isBn = isBn,
                    buttonSize = 40.dp,
                    iconSize = 20.dp,
                    onTogglePin = onTogglePin,
                    onEditMetadata = onEditMetadata,
                    onToggleArchive = onToggleArchive
                )
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
                rightColor = profitColor
            )

            VaultKpiPairRow(
                leftLabel = if (isBn) "নিট বিক্রয়" else "Net Sales",
                leftValue = ImportVaultUiLogic.formatBdt(shipment.netSalesRevenueMinor, language),
                rightLabel = if (isBn) "বদ্ধ স্টক মূল্য" else "Stock Value",
                rightValue = ImportVaultUiLogic.formatBdt(shipment.lockedInventoryCapitalMinor, language)
            )

            FolderProgressFooter(
                leftText = "${if (isBn) "বিক্রি হার" else "Sold"} ${ImportVaultUiLogic.formatPercentCompact(shipment.sellThroughPercent, language)} (${ImportVaultUiLogic.formatQuantityByUnit(shipment.netSoldQtyByUnit, language)})",
                rightText = "${if (isBn) "মার্জিন" else "Margin"} ${ImportVaultUiLogic.formatPercentCompact(shipment.netMarginPercent, language)}",
                progress = stRatio,
                barColor = tokens.accentTeal,
                compact = false,
                tokens = tokens
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PoFolderCard(
                        po = pair[0],
                        compact = true,
                        language = language,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
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
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
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
    val tokens = LocalDashboardTokens.current
    val colors = MaterialTheme.colorScheme
    val productCountStr = Formatters.formatNumber(po.productCount.toLong(), language)
    val shipmentTag = if (po.shipmentNumbers.isEmpty()) {
        if (isBn) "কোনো চালান নেই" else "No shipment"
    } else {
        po.shipmentNumbers.joinToString(", ")
    }
    val metaLine = "${Formatters.formatDate(po.orderDate, language)} · $shipmentTag · $productCountStr ${if (isBn) "পণ্য" else "SKUs"}"
    val stRatio = ((po.sellThroughPercent ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    val profitColor = if (po.attributedNetProfitMinor >= 0L) StatusSuccess else StatusError

    VaultCard(
        modifier = modifier,
        borderColor = if (po.isPinned) tokens.accentPurple.copy(alpha = 0.6f) else null,
        contentPadding = if (compact) PaddingValues(11.dp) else PaddingValues(14.dp),
        verticalSpacing = if (compact) 8.dp else 10.dp,
        onClick = onClick
    ) {
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tokens.accentTeal.copy(alpha = if (tokens.isDark) 0.24f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (po.isPinned) Icons.Outlined.Bookmark else Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = if (tokens.isDark) Color.White else tokens.accentTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }
                val (opBg, opFg) = operationalBadgeColors(po.operationalStatus, colors)
                VaultStatusBadge(
                    label = operationalStatusLabel(po.operationalStatus, language),
                    containerColor = opBg,
                    contentColor = opFg
                )
                Spacer(modifier = Modifier.weight(1f))
                FolderOverflowMenu(
                    isPinned = po.isPinned,
                    archiveStatus = po.archiveStatus,
                    isBn = isBn,
                    buttonSize = 30.dp,
                    iconSize = 18.dp,
                    onTogglePin = onTogglePin,
                    onEditMetadata = onEditMetadata,
                    onToggleArchive = onToggleArchive
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = po.displayTitle,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.cardValueText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (po.displayTitle != po.poNumber) {
                    Text(
                        text = po.poNumber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.accentPurple,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = po.supplierName,
                    fontSize = 11.sp,
                    color = tokens.sectionSubtitleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = metaLine,
                    fontSize = 10.5.sp,
                    color = tokens.sectionSubtitleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val (comBg, comFg) = commercialBadgeColors(po.commercialStatus, colors)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VaultStatusBadge(
                    label = commercialStatusLabel(po.commercialStatus, language),
                    containerColor = comBg,
                    contentColor = comFg
                )
                if (po.costFinalizationStatus == com.pocketdev.advance_erp.domain.model.CostFinalizationStatus.PROVISIONAL) {
                    VaultStatusBadge(
                        label = if (isBn) "অসমাপ্ত" else "Provisional",
                        containerColor = com.pocketdev.advance_erp.core.designsystem.StatusWarningContainer,
                        contentColor = com.pocketdev.advance_erp.core.designsystem.StatusWarning
                    )
                }
            }

            CompactGridMetricsBox(
                leftLabel = if (isBn) "বিনিয়োগ" else "Invested",
                leftValue = ImportVaultUiLogic.formatBdt(po.totalInvestedCapitalMinor, language),
                rightLabel = if (isBn) "নিট মুনাফা" else "Net Profit",
                rightValue = ImportVaultUiLogic.formatBdt(po.attributedNetProfitMinor, language),
                rightColor = profitColor,
                tokens = tokens
            )

            FolderProgressFooter(
                leftText = "${if (isBn) "বিক্রি" else "Sold"} ${ImportVaultUiLogic.formatPercentCompact(po.sellThroughPercent, language)}",
                rightText = "${if (isBn) "বকেয়া" else "Due"} ${ImportVaultUiLogic.formatBdt(po.supplierPayableBdtMinor, language)}",
                progress = stRatio,
                barColor = tokens.accentPurple,
                compact = true,
                tokens = tokens
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(tokens.accentTeal.copy(alpha = if (tokens.isDark) 0.24f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (po.isPinned) Icons.Outlined.Bookmark else Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = if (tokens.isDark) Color.White else tokens.accentTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = po.displayTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.cardValueText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (po.displayTitle != po.poNumber) {
                        Text(
                            text = po.poNumber,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.accentPurple,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = po.supplierName,
                        fontSize = 12.sp,
                        color = tokens.sectionSubtitleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = metaLine,
                        fontSize = 11.sp,
                        color = tokens.sectionSubtitleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FolderOverflowMenu(
                    isPinned = po.isPinned,
                    archiveStatus = po.archiveStatus,
                    isBn = isBn,
                    buttonSize = 40.dp,
                    iconSize = 20.dp,
                    onTogglePin = onTogglePin,
                    onEditMetadata = onEditMetadata,
                    onToggleArchive = onToggleArchive
                )
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
                rightColor = profitColor
            )

            if (po.originalCurrency != "BDT") {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (tokens.isDark) Color(0xFF1A1F3C) else Color(0xFFF7F3FC),
                    border = BorderStroke(0.7.dp, if (tokens.isDark) Color(0xFF2E3663) else Color(0xFFE8DDF6))
                ) {
                    Text(
                        text = "${if (isBn) "ক্রয়মূল্য" else "PO Cost"}: ${ImportVaultUiLogic.formatCurrency(po.originalTotalAmountMinor, po.originalCurrency, language)} @ ${po.exchangeRateToBdt}",
                        fontSize = 11.sp,
                        color = tokens.sectionSubtitleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            FolderProgressFooter(
                leftText = "${if (isBn) "বিক্রি হার" else "Sold"} ${ImportVaultUiLogic.formatPercentCompact(po.sellThroughPercent, language)} (${ImportVaultUiLogic.formatQuantityByUnit(po.netSoldQtyByUnit, language)})",
                rightText = "${if (isBn) "বকেয়া" else "Payable"} ${ImportVaultUiLogic.formatBdt(po.supplierPayableBdtMinor, language)}",
                progress = stRatio,
                barColor = tokens.accentPurple,
                compact = false,
                tokens = tokens
            )
        }
    }
}

@Composable
private fun FolderOverflowMenu(
    isPinned: Boolean,
    archiveStatus: String,
    isBn: Boolean,
    buttonSize: Dp,
    iconSize: Dp,
    onTogglePin: () -> Unit,
    onEditMetadata: () -> Unit,
    onToggleArchive: () -> Unit
) {
    val tokens = LocalDashboardTokens.current
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = null,
                tint = tokens.sectionSubtitleText,
                modifier = Modifier.size(iconSize)
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (isPinned) (if (isBn) "আনপিন" else "Unpin") else (if (isBn) "পিন করুন" else "Pin")) },
                leadingIcon = { Icon(if (isPinned) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, null) },
                onClick = { menuExpanded = false; onTogglePin() }
            )
            DropdownMenuItem(
                text = { Text(if (isBn) "শিরোনাম/ট্যাগ/নোট" else "Rename / Tags / Notes") },
                leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                onClick = { menuExpanded = false; onEditMetadata() }
            )
            DropdownMenuItem(
                text = { Text(if (archiveStatus == "ARCHIVED") (if (isBn) "আনআর্কাইভ" else "Unarchive") else (if (isBn) "আর্কাইভ" else "Archive")) },
                leadingIcon = { Icon(Icons.Outlined.Archive, null) },
                onClick = { menuExpanded = false; onToggleArchive() }
            )
        }
    }
}

@Composable
private fun CompactGridMetricsBox(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
    rightColor: Color,
    tokens: DashboardColorTokens
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        color = if (tokens.isDark) Color(0xFF1A1F3C) else Color(0xFFF7F3FC),
        border = BorderStroke(
            width = 0.8.dp,
            color = if (tokens.isDark) Color(0xFF2E3663) else Color(0xFFE8DDF6)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = leftLabel,
                    fontSize = 10.5.sp,
                    color = tokens.sectionSubtitleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = leftValue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tokens.cardValueText,
                    maxLines = 1,
                    softWrap = false
                )
            }
            HorizontalDivider(thickness = 0.6.dp, color = tokens.dividerColor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rightLabel,
                    fontSize = 10.5.sp,
                    color = tokens.sectionSubtitleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = rightValue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = rightColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun FolderProgressFooter(
    leftText: String,
    rightText: String,
    progress: Float,
    barColor: Color,
    compact: Boolean,
    tokens: DashboardColorTokens
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = leftText,
                fontSize = if (compact) 10.5.sp else 11.5.sp,
                color = tokens.sectionSubtitleText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = rightText,
                fontSize = if (compact) 10.5.sp else 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = tokens.sectionTitleText,
                maxLines = 1,
                softWrap = false
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 5.dp else 6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = if (tokens.isDark) Color(0xFF252A4A) else Color(0xFFEBE3F5)
        )
    }
}

@Composable
private fun operationalBadgeColors(
    status: com.pocketdev.advance_erp.domain.model.OperationalStatus,
    colors: androidx.compose.material3.ColorScheme
): Pair<Color, Color> = when (status) {
    com.pocketdev.advance_erp.domain.model.OperationalStatus.CANCELLED ->
        com.pocketdev.advance_erp.core.designsystem.StatusErrorContainer to StatusError
    com.pocketdev.advance_erp.domain.model.OperationalStatus.CLOSED,
    com.pocketdev.advance_erp.domain.model.OperationalStatus.RECEIVED ->
        com.pocketdev.advance_erp.core.designsystem.StatusSuccessContainer to StatusSuccess
    com.pocketdev.advance_erp.domain.model.OperationalStatus.PARTIALLY_RECEIVED ->
        StatusWarningContainer to StatusWarning
    com.pocketdev.advance_erp.domain.model.OperationalStatus.IN_TRANSIT,
    com.pocketdev.advance_erp.domain.model.OperationalStatus.ORDERED ->
        StatusInfoContainer to StatusInfo
    com.pocketdev.advance_erp.domain.model.OperationalStatus.DRAFT ->
        colors.surfaceVariant to colors.onSurfaceVariant
}

@Composable
private fun commercialBadgeColors(
    status: com.pocketdev.advance_erp.domain.model.CommercialStatus,
    colors: androidx.compose.material3.ColorScheme
): Pair<Color, Color> = when (status) {
    com.pocketdev.advance_erp.domain.model.CommercialStatus.LOSS_MAKING ->
        com.pocketdev.advance_erp.core.designsystem.StatusErrorContainer to StatusError
    com.pocketdev.advance_erp.domain.model.CommercialStatus.PROFITABLE,
    com.pocketdev.advance_erp.domain.model.CommercialStatus.CASH_RECOVERED,
    com.pocketdev.advance_erp.domain.model.CommercialStatus.SOLD_OUT ->
        com.pocketdev.advance_erp.core.designsystem.StatusSuccessContainer to StatusSuccess
    com.pocketdev.advance_erp.domain.model.CommercialStatus.UNATTRIBUTED ->
        StatusWarningContainer to StatusWarning
    com.pocketdev.advance_erp.domain.model.CommercialStatus.SELLING ->
        colors.secondaryContainer to colors.onSecondaryContainer
    com.pocketdev.advance_erp.domain.model.CommercialStatus.UNSOLD ->
        colors.surfaceVariant to colors.onSurfaceVariant
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
