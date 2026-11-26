@file:OptIn(ExperimentalLayoutApi::class)

package com.pocketdev.advance_erp.feature.importvault

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.core.designsystem.LocalDashboardTokens
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.StatusWarningContainer
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.domain.model.ExpenseAllocationMode
import com.pocketdev.advance_erp.domain.model.ProductVaultRow
import com.pocketdev.advance_erp.domain.model.PurchaseOrderVaultDetail
import com.pocketdev.advance_erp.domain.model.ShipmentPoAllocationSummary
import com.pocketdev.advance_erp.domain.model.ShipmentVaultDetail
import com.pocketdev.advance_erp.domain.model.VaultDocumentCategory
import com.pocketdev.advance_erp.domain.model.VaultDocumentRef
import com.pocketdev.advance_erp.domain.model.VaultSaleSourceRow
import com.pocketdev.advance_erp.domain.model.VaultScopeType
import com.pocketdev.advance_erp.domain.model.VaultTrendPoint

@Composable
fun ImportVaultShipmentDetailScreen(
    uiState: ImportVaultUiState,
    onBack: () -> Unit,
    onOpenPo: (String) -> Unit,
    onOpenProduct: (String) -> Unit,
    onSortChange: (ProfitSortOption) -> Unit,
    onTogglePin: (VaultScopeType, String, Boolean) -> Unit,
    onSaveMetadata: (VaultScopeType, String, String, String, List<String>) -> Unit,
    onOpenExport: () -> Unit = {},
    onAddAttachmentClick: () -> Unit = {},
    onPreviewDocument: (VaultDocumentRef) -> Unit = {},
    onShareDocument: (VaultDocumentRef) -> Unit = {},
    onDeleteAttachment: (String) -> Unit = {},
    onNavigateToRoute: (String) -> Unit
) {
    val detail = uiState.selectedShipmentDetail
    val language = uiState.language
    val isBn = language == AppLanguage.BANGLA

    if (detail == null) {
        DetailLoadingOrEmptyScaffold(
            isLoading = uiState.isLoadingDetail,
            title = if (isBn) "চালান ফোল্ডার" else "Shipment Folder",
            language = language,
            onBack = onBack
        )
        return
    }

    val summary = detail.summary
    ImportVaultFolderScaffold(
        scopeType = VaultScopeType.SHIPMENT,
        sourceId = summary.shipmentId,
        displayTitle = summary.displayTitle,
        codeSubtitle = if (summary.displayTitle != summary.shipmentNumber) summary.shipmentNumber else summary.shippingMethod,
        dateLabel = "${Formatters.formatDate(summary.shippingDate, language)} · ${summary.supplierNames.joinToString(", ")}",
        isPinned = summary.isPinned,
        notes = summary.notes,
        tags = summary.tags,
        operationalStatus = summary.operationalStatus,
        commercialStatus = summary.commercialStatus,
        costFinalizationStatus = summary.costFinalizationStatus,
        profitLabel = summary.profitLabel,
        investedCapitalMinor = summary.totalInvestedCapitalMinor,
        netRevenueMinor = summary.netSalesRevenueMinor,
        cogsMinor = summary.cogsMinor,
        directExpensesMinor = summary.directAttributedExpensesMinor,
        allocatedSharedOverheadMinor = summary.allocatedSharedOverheadMinor,
        inventoryLossesMinor = summary.inventoryLossesMinor,
        attributedNetProfitMinor = summary.attributedNetProfitMinor,
        grossProfitMinor = summary.grossProfitMinor,
        grossMarginPercent = summary.grossMarginPercent,
        netMarginPercent = summary.netMarginPercent,
        roiPercent = summary.roiPercent,
        sellThroughPercent = summary.sellThroughPercent,
        lockedInventoryCapitalMinor = summary.lockedInventoryCapitalMinor,
        cashRecovery = summary.cashRecovery,
        expectedRemainingProfit = summary.expectedRemainingProfit,
        agingSummary = summary.agingSummary,
        products = detail.products,
        allocations = detail.poAllocations,
        isAllocationForPoChildren = true,
        supplierPaidMinor = detail.supplierPaidMinor,
        supplierPayableMinor = detail.supplierPayableMinor,
        shipmentWeightInfo = "${summary.verifiedBillableWeightKg} kg (${if (isBn) "যাচাইকৃত" else "Verified"})" +
            (summary.supplierDeclaredWeightKg?.let { " · $it kg (${if (isBn) "সরবরাহকারী" else "Supplier"})" } ?: ""),
        originFxInfo = if (summary.originCurrency != null && summary.originPurchaseAmountMinor != null) {
            ImportVaultUiLogic.formatCurrency(summary.originPurchaseAmountMinor, summary.originCurrency, language)
        } else null,
        cartons = detail.cartons,
        importCosts = detail.importCosts,
        landedSnapshots = detail.landedCostSnapshots,
        additionalCosts = detail.additionalCosts,
        damageRecords = detail.damageRecords,
        shortageRecords = detail.shortageRecords,
        costAdjustments = detail.costAdjustments,
        directExpenseVouchers = detail.directExpenseVouchers,
        saleSources = detail.saleSources,
        salesTrend = detail.salesTrend,
        returnSources = detail.returnSources,
        costLayers = detail.costLayers,
        timelineEvents = detail.timelineEvents,
        documentRefs = detail.documentRefs,
        sortOption = uiState.profitSort,
        expenseAllocationMode = uiState.overview?.expenseAllocationMode ?: ExpenseAllocationMode.UNALLOCATED,
        sourceModuleRoute = "import_management",
        sourceModuleLabel = if (isBn) "আমদানি ব্যবস্থাপনায় খুলুন" else "Open in Import Mgmt",
        language = language,
        onBack = onBack,
        onOpenChildScope = onOpenPo,
        onOpenProduct = onOpenProduct,
        onSortChange = onSortChange,
        onTogglePin = { onTogglePin(VaultScopeType.SHIPMENT, summary.shipmentId, summary.isPinned) },
        onSaveMetadata = { title, n, t -> onSaveMetadata(VaultScopeType.SHIPMENT, summary.shipmentId, title, n, t) },
        onOpenExport = onOpenExport,
        onAddAttachmentClick = onAddAttachmentClick,
        onPreviewDocument = onPreviewDocument,
        onShareDocument = onShareDocument,
        onDeleteAttachment = onDeleteAttachment,
        onNavigateToRoute = onNavigateToRoute
    )
}

@Composable
fun ImportVaultPoDetailScreen(
    uiState: ImportVaultUiState,
    onBack: () -> Unit,
    onOpenShipment: (String) -> Unit,
    onOpenProduct: (String) -> Unit,
    onSortChange: (ProfitSortOption) -> Unit,
    onTogglePin: (VaultScopeType, String, Boolean) -> Unit,
    onSaveMetadata: (VaultScopeType, String, String, String, List<String>) -> Unit,
    onOpenExport: () -> Unit = {},
    onAddAttachmentClick: () -> Unit = {},
    onPreviewDocument: (VaultDocumentRef) -> Unit = {},
    onShareDocument: (VaultDocumentRef) -> Unit = {},
    onDeleteAttachment: (String) -> Unit = {},
    onNavigateToRoute: (String) -> Unit
) {
    val detail = uiState.selectedPoDetail
    val language = uiState.language
    val isBn = language == AppLanguage.BANGLA

    if (detail == null) {
        DetailLoadingOrEmptyScaffold(
            isLoading = uiState.isLoadingDetail,
            title = if (isBn) "ক্রয় আদেশ ফোল্ডার (PO)" else "Purchase Order Folder",
            language = language,
            onBack = onBack
        )
        return
    }

    val summary = detail.summary
    ImportVaultFolderScaffold(
        scopeType = VaultScopeType.PURCHASE_ORDER,
        sourceId = summary.purchaseOrderId,
        displayTitle = summary.displayTitle,
        codeSubtitle = if (summary.displayTitle != summary.poNumber) summary.poNumber else summary.supplierName,
        dateLabel = "${Formatters.formatDate(summary.orderDate, language)} · ${summary.supplierName}",
        isPinned = summary.isPinned,
        notes = summary.notes,
        tags = summary.tags,
        operationalStatus = summary.operationalStatus,
        commercialStatus = summary.commercialStatus,
        costFinalizationStatus = summary.costFinalizationStatus,
        profitLabel = summary.profitLabel,
        investedCapitalMinor = summary.totalInvestedCapitalMinor,
        netRevenueMinor = summary.netSalesRevenueMinor,
        cogsMinor = summary.cogsMinor,
        directExpensesMinor = summary.directAttributedExpensesMinor,
        allocatedSharedOverheadMinor = summary.allocatedSharedOverheadMinor,
        inventoryLossesMinor = summary.inventoryLossesMinor,
        attributedNetProfitMinor = summary.attributedNetProfitMinor,
        grossProfitMinor = summary.grossProfitMinor,
        grossMarginPercent = summary.grossMarginPercent,
        netMarginPercent = summary.netMarginPercent,
        roiPercent = summary.roiPercent,
        sellThroughPercent = summary.sellThroughPercent,
        lockedInventoryCapitalMinor = summary.lockedInventoryCapitalMinor,
        cashRecovery = summary.cashRecovery,
        expectedRemainingProfit = summary.expectedRemainingProfit,
        agingSummary = summary.agingSummary,
        products = detail.products,
        allocations = detail.shipmentAllocations,
        isAllocationForPoChildren = false,
        supplierPaidMinor = summary.supplierPaidBdtMinor,
        supplierPayableMinor = summary.supplierPayableBdtMinor,
        shipmentWeightInfo = null,
        originFxInfo = "${ImportVaultUiLogic.formatCurrency(summary.originalTotalAmountMinor, summary.originalCurrency, language)} @ ${summary.exchangeRateToBdt}",
        cartons = detail.cartons,
        importCosts = detail.importCosts,
        landedSnapshots = detail.landedCostSnapshots,
        additionalCosts = detail.additionalCosts,
        damageRecords = detail.damageRecords,
        shortageRecords = detail.shortageRecords,
        costAdjustments = detail.costAdjustments,
        directExpenseVouchers = detail.directExpenseVouchers,
        saleSources = detail.saleSources,
        salesTrend = detail.salesTrend,
        returnSources = detail.returnSources,
        costLayers = detail.costLayers,
        timelineEvents = detail.timelineEvents,
        documentRefs = detail.documentRefs,
        sortOption = uiState.profitSort,
        expenseAllocationMode = uiState.overview?.expenseAllocationMode ?: ExpenseAllocationMode.UNALLOCATED,
        sourceModuleRoute = "purchases",
        sourceModuleLabel = if (isBn) "ক্রয় মডিউলে খুলুন" else "Open in Purchases",
        language = language,
        onBack = onBack,
        onOpenChildScope = onOpenShipment,
        onOpenProduct = onOpenProduct,
        onSortChange = onSortChange,
        onTogglePin = { onTogglePin(VaultScopeType.PURCHASE_ORDER, summary.purchaseOrderId, summary.isPinned) },
        onSaveMetadata = { title, n, t -> onSaveMetadata(VaultScopeType.PURCHASE_ORDER, summary.purchaseOrderId, title, n, t) },
        onOpenExport = onOpenExport,
        onAddAttachmentClick = onAddAttachmentClick,
        onPreviewDocument = onPreviewDocument,
        onShareDocument = onShareDocument,
        onDeleteAttachment = onDeleteAttachment,
        onNavigateToRoute = onNavigateToRoute
    )
}

@Composable
private fun DetailLoadingOrEmptyScaffold(
    isLoading: Boolean,
    title: String,
    language: AppLanguage,
    onBack: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val tokens = LocalDashboardTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(tokens.pageBgTop, tokens.pageBgBottom)))
    ) {
        VaultSubScreenTopBar(
            title = title,
            onBack = onBack
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text(if (isBn) "ফোল্ডারের তথ্য পাওয়া যায়নি।" else "Folder record not found.")
            }
        }
    }
}

@Composable
private fun ImportVaultFolderScaffold(
    scopeType: VaultScopeType,
    sourceId: String,
    displayTitle: String,
    codeSubtitle: String,
    dateLabel: String,
    isPinned: Boolean,
    notes: String?,
    tags: List<String>,
    operationalStatus: com.pocketdev.advance_erp.domain.model.OperationalStatus,
    commercialStatus: com.pocketdev.advance_erp.domain.model.CommercialStatus,
    costFinalizationStatus: com.pocketdev.advance_erp.domain.model.CostFinalizationStatus,
    profitLabel: String,
    investedCapitalMinor: Long,
    netRevenueMinor: Long,
    cogsMinor: Long,
    directExpensesMinor: Long,
    allocatedSharedOverheadMinor: Long,
    inventoryLossesMinor: Long,
    attributedNetProfitMinor: Long,
    grossProfitMinor: Long,
    grossMarginPercent: Double?,
    netMarginPercent: Double?,
    roiPercent: Double?,
    sellThroughPercent: Double?,
    lockedInventoryCapitalMinor: Long,
    cashRecovery: com.pocketdev.advance_erp.domain.model.CashRecoverySummary,
    expectedRemainingProfit: com.pocketdev.advance_erp.domain.model.ExpectedRemainingProfitEstimate,
    agingSummary: com.pocketdev.advance_erp.domain.model.AgingSummary,
    products: List<ProductVaultRow>,
    allocations: List<ShipmentPoAllocationSummary>,
    isAllocationForPoChildren: Boolean,
    supplierPaidMinor: Long?,
    supplierPayableMinor: Long?,
    shipmentWeightInfo: String?,
    originFxInfo: String?,
    cartons: List<com.pocketdev.advance_erp.data.entity.ShipmentCarton>,
    importCosts: List<com.pocketdev.advance_erp.data.entity.ImportCostItem>,
    landedSnapshots: List<com.pocketdev.advance_erp.data.entity.LandedCostSnapshot>,
    additionalCosts: List<com.pocketdev.advance_erp.data.entity.AdditionalProductCost>,
    damageRecords: List<com.pocketdev.advance_erp.data.entity.DamageRecord>,
    shortageRecords: List<com.pocketdev.advance_erp.data.entity.ShortageRecord>,
    costAdjustments: List<com.pocketdev.advance_erp.data.entity.InventoryCostAdjustment>,
    directExpenseVouchers: List<com.pocketdev.advance_erp.data.entity.ExpenseVoucher>,
    saleSources: List<VaultSaleSourceRow>,
    salesTrend: List<VaultTrendPoint>,
    returnSources: List<com.pocketdev.advance_erp.domain.model.VaultReturnSourceRow>,
    costLayers: List<com.pocketdev.advance_erp.domain.model.VaultCostLayerRow>,
    timelineEvents: List<com.pocketdev.advance_erp.domain.model.VaultTimelineEvent>,
    documentRefs: List<com.pocketdev.advance_erp.domain.model.VaultDocumentRef>,
    sortOption: ProfitSortOption,
    expenseAllocationMode: ExpenseAllocationMode,
    sourceModuleRoute: String,
    sourceModuleLabel: String,
    language: AppLanguage,
    onBack: () -> Unit,
    onOpenChildScope: (String) -> Unit,
    onOpenProduct: (String) -> Unit,
    onSortChange: (ProfitSortOption) -> Unit,
    onTogglePin: () -> Unit,
    onSaveMetadata: (String, String, List<String>) -> Unit,
    onOpenExport: () -> Unit,
    onAddAttachmentClick: () -> Unit,
    onPreviewDocument: (VaultDocumentRef) -> Unit,
    onShareDocument: (VaultDocumentRef) -> Unit,
    onDeleteAttachment: (String) -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val tokens = LocalDashboardTokens.current
    val colors = MaterialTheme.colorScheme

    var activeTab by remember { mutableIntStateOf(0) }
    var profitSubTab by remember { mutableIntStateOf(0) }
    var compareSubIdx by remember { mutableIntStateOf(0) }
    var tlDocSubTab by remember { mutableIntStateOf(0) }
    var selectedDocCategory by remember { mutableStateOf<VaultDocumentCategory?>(null) }
    var docSearchQuery by remember { mutableStateOf("") }
    var showMetadataDialog by remember { mutableStateOf(false) }
    var drillDialogState by remember { mutableStateOf<Pair<String, List<VaultSaleSourceRow>>?>(null) }

    drillDialogState?.let { (title, rows) ->
        VaultSourceTransactionsDrillDialog(
            title = title,
            sales = rows,
            language = language,
            onDismiss = { drillDialogState = null },
            onOpenSalesModule = { onNavigateToRoute("sales") }
        )
    }

    if (showMetadataDialog) {
        VaultMetadataEditDialog(
            initialTitle = displayTitle,
            initialNotes = notes,
            initialTags = tags,
            language = language,
            onDismiss = { showMetadataDialog = false },
            onSave = { t, n, tg ->
                showMetadataDialog = false
                onSaveMetadata(t, n, tg)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(tokens.pageBgTop, tokens.pageBgBottom)))
    ) {
        VaultSubScreenTopBar(
            title = displayTitle,
            subtitle = "$codeSubtitle · $dateLabel",
            onBack = onBack
        ) {
            IconButton(onClick = onTogglePin, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (isPinned) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    tint = if (isPinned) colors.primary else tokens.sectionSubtitleText
                )
            }
            IconButton(onClick = { showMetadataDialog = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.Edit, contentDescription = null, tint = tokens.sectionSubtitleText)
            }
            IconButton(onClick = onOpenExport, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Outlined.Download,
                    contentDescription = if (isBn) "এক্সপোর্ট" else "Export Report",
                    tint = colors.primary
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "detail_status_badges") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VaultStatusBadgeRow(
                        operationalStatus = operationalStatus,
                        commercialStatus = commercialStatus,
                        costFinalizationStatus = costFinalizationStatus,
                        language = language
                    )
                    if (!notes.isNullOrBlank()) {
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { onNavigateToRoute(sourceModuleRoute) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(sourceModuleLabel, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            item(key = "detail_main_tabs") {
                VaultWrappedTabBar(
                    options = listOf(
                        0 to (if (isBn) "সারসংক্ষেপ" else "Overview"),
                        1 to (if (isBn) "মুনাফা" else "Profit"),
                        2 to (if (isBn) "মূলধন" else "Capital"),
                        3 to (if (isBn) "নথি ও সময়রেখা" else "Docs & Timeline")
                    ),
                    selectedIndex = activeTab,
                    onSelected = { activeTab = it }
                )
            }

            when (activeTab) {
                0 -> {
                    item(key = "overview_pnl_hero") {
                        VaultHeroCard {
                            Text(
                                text = profitLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = ImportVaultUiLogic.formatBdt(attributedNetProfitMinor, language),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (attributedNetProfitMinor >= 0L) Color(0xFF42F5AA) else Color(0xFFFF9DB0)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${if (isBn) "নিট মার্জিন" else "Net Margin"} ${ImportVaultUiLogic.formatPercent(netMarginPercent, language)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "${if (isBn) "ROI (÷ বিনিয়োগ)" else "ROI (÷ Invested)"} ${ImportVaultUiLogic.formatPercent(roiPercent, language)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF81CDD4)
                                )
                                Text(
                                    text = "${if (isBn) "বিক্রি হার" else "Sell-Through"} ${ImportVaultUiLogic.formatPercent(sellThroughPercent, language)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFD5A4E0)
                                )
                            }
                        }
                    }

                    if (costFinalizationStatus == com.pocketdev.advance_erp.domain.model.CostFinalizationStatus.PROVISIONAL) {
                        item(key = "provisional_warning") {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusWarningContainer,
                                border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = StatusWarning)
                                    Text(
                                        text = if (isBn) {
                                            "ল্যান্ডেড খরচ এখনো চূড়ান্ত হয়নি বা আংশিক মাল গ্রহণ বাকি রয়েছে (Provisional)।"
                                        } else {
                                            "Landed cost is provisional or receiving is pending — cost/profit figures may update upon finalization."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StatusWarning
                                    )
                                }
                            }
                        }
                    }

                    item(key = "overview_financial_statement") {
                        VaultCard {
                            Text(
                                text = if (isBn) "আয়-ব্যয় ও মূলধন বিবরণী" else "Financial Statement",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            VaultKpiPairRow(
                                leftLabel = if (isBn) "মোট বিনিয়োগ (Landed)" else "Landed Investment",
                                leftValue = ImportVaultUiLogic.formatBdt(investedCapitalMinor, language),
                                rightLabel = if (isBn) "নিট বিক্রয় আয়" else "Net Revenue",
                                rightValue = ImportVaultUiLogic.formatBdt(netRevenueMinor, language)
                            )
                            VaultKpiPairRow(
                                leftLabel = if (isBn) "বিক্রীত ব্যয় (COGS)" else "COGS",
                                leftValue = ImportVaultUiLogic.formatBdt(cogsMinor, language),
                                rightLabel = if (isBn) "গ্রস মুনাফা (${ImportVaultUiLogic.formatPercent(grossMarginPercent, language)})" else "Gross Profit (${ImportVaultUiLogic.formatPercent(grossMarginPercent, language)})",
                                rightValue = ImportVaultUiLogic.formatBdt(grossProfitMinor, language)
                            )
                            VaultKpiPairRow(
                                leftLabel = if (isBn) "পরিচালন ব্যয়" else "Direct + Shared Expense",
                                leftValue = ImportVaultUiLogic.formatBdt(directExpensesMinor + allocatedSharedOverheadMinor, language),
                                rightLabel = if (isBn) "ক্ষতি / নষ্ট স্টক" else "Inventory Losses",
                                rightValue = ImportVaultUiLogic.formatBdt(inventoryLossesMinor, language),
                                rightColor = if (inventoryLossesMinor > 0L) StatusError else colors.onSurface
                            )
                            VaultKpiPairRow(
                                leftLabel = if (isBn) "বদ্ধ স্টক মূল্য" else "Locked Stock Value",
                                leftValue = ImportVaultUiLogic.formatBdt(lockedInventoryCapitalMinor, language),
                                rightLabel = if (isBn) "আদায়কৃত নগদ" else "Cash Collected",
                                rightValue = ImportVaultUiLogic.formatBdt(cashRecovery.attributableCashCollectedMinor, language),
                                rightColor = StatusSuccess
                            )
                            if (!originFxInfo.isNullOrBlank() || !shipmentWeightInfo.isNullOrBlank()) {
                                HorizontalDivider()
                                originFxInfo?.let { fx ->
                                    Text(
                                        text = "${if (isBn) "উৎস মুদ্রা ও বিনিময় হার" else "Origin Currency & FX"}: $fx",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                                shipmentWeightInfo?.let { w ->
                                    Text(
                                        text = "${if (isBn) "ওজন তথ্য" else "Weight"}: $w",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (allocations.isNotEmpty()) {
                        item(key = "overview_child_allocations") {
                            VaultExpandableSection(
                                title = if (isAllocationForPoChildren) {
                                    if (isBn) "এই চালানে যুক্ত PO বরাদ্দ অংশ" else "Allocated PO Portions in Shipment"
                                } else {
                                    if (isBn) "চালানভিত্তিক বরাদ্দ অংশ (Split Shipments)" else "Shipment Allocations for PO"
                                },
                                subtitle = if (isBn) {
                                    "দ্বৈত হিসাব ছাড়াই প্রতিটি চালানের সঠিক অংশ (ট্যাপ করে খুলুন)"
                                } else {
                                    "Non-duplicated allocated slice per linkage (tap to inspect)"
                                },
                                initiallyExpanded = true,
                                badgeText = Formatters.formatNumber(allocations.size.toLong(), language)
                            ) {
                                allocations.forEach { alloc ->
                                    val targetId = if (isAllocationForPoChildren) alloc.purchaseOrderId else alloc.shipmentId
                                    val title = if (isAllocationForPoChildren) alloc.poNumber else alloc.shipmentNumber
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(role = Role.Button) { onOpenChildScope(targetId) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = colors.surfaceVariant.copy(alpha = 0.55f),
                                        border = BorderStroke(0.8.dp, colors.outlineVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "$title · ${alloc.supplierName}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                VaultStatusBadge(
                                                    label = ImportVaultUiLogic.formatPercent(alloc.shareOfPoPercent, language),
                                                    containerColor = colors.primaryContainer,
                                                    contentColor = colors.onPrimaryContainer
                                                )
                                            }
                                            VaultKpiPairRow(
                                                leftLabel = if (isBn) "বরাদ্দ বিনিয়োগ" else "Allocated Investment",
                                                leftValue = ImportVaultUiLogic.formatBdt(alloc.allocatedInvestmentMinor, language),
                                                rightLabel = if (isBn) "নিট মুনাফা" else "Net Profit",
                                                rightValue = ImportVaultUiLogic.formatBdt(alloc.attributedNetProfitMinor, language),
                                                rightColor = if (alloc.attributedNetProfitMinor >= 0L) StatusSuccess else StatusError
                                            )
                                            Text(
                                                text = "${if (isBn) "গৃহীত" else "Rcv"} ${ImportVaultUiLogic.formatQuantityByUnit(alloc.receivedQtyByUnit, language)} · ${if (isBn) "বিক্রীত" else "Sold"} ${ImportVaultUiLogic.formatQuantityByUnit(alloc.netSoldQtyByUnit, language)} · ${if (isBn) "মার্জিন" else "Margin"} ${ImportVaultUiLogic.formatPercent(alloc.netMarginPercent, language)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (products.isNotEmpty()) {
                        item(key = "overview_products_preview") {
                            VaultExpandableSection(
                                title = if (isBn) "পণ্যের পারফরম্যান্স" else "Product Cards (Tap for Variants)",
                                badgeText = Formatters.formatNumber(products.size.toLong(), language),
                                initiallyExpanded = true
                            ) {
                                products.forEach { prod ->
                                    val pName = if (isBn) prod.productNameBn.ifBlank { prod.productNameEn } else prod.productNameEn
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(role = Role.Button) { onOpenProduct(prod.productId) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = colors.surfaceVariant.copy(alpha = 0.55f),
                                        border = BorderStroke(0.8.dp, colors.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            VaultProductThumbnail(
                                                imageUri = prod.primaryImageUri,
                                                contentDescription = pName,
                                                size = 46.dp
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(pName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = "${prod.baseSku} · ${ImportVaultUiLogic.formatBdt(prod.attributedNetProfitMinor, language)} (${ImportVaultUiLogic.formatPercent(prod.netMarginPercent, language)})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (prod.attributedNetProfitMinor >= 0L) StatusSuccess else StatusError
                                                )
                                            }
                                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = colors.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (cartons.isNotEmpty()) {
                        item(key = "overview_packing_cartons") {
                            VaultExpandableSection(
                                title = if (isBn) "প্যাকিং ও কার্টনসমূহ" else "Purchase & Packing Cartons",
                                badgeText = Formatters.formatNumber(cartons.size.toLong(), language)
                            ) {
                                cartons.forEach { c ->
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "${c.cartonNumber} · ${c.quantity} pcs · ${c.grossWeightKg} kg${c.supplierDeclaredGrossWeightKg?.let { " (Supplier: $it kg)" } ?: ""}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "CBM ${c.cbm} · ${c.lengthCm}×${c.widthCm}×${c.heightCm} cm",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.onSurfaceVariant
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }

                    if (importCosts.isNotEmpty() || landedSnapshots.isNotEmpty() || additionalCosts.isNotEmpty() ||
                        costAdjustments.isNotEmpty() || directExpenseVouchers.isNotEmpty()
                    ) {
                        item(key = "overview_landed_costs") {
                            VaultExpandableSection(
                                title = if (isBn) "ল্যান্ডেড কস্ট ও সরাসরি ব্যয়" else "Landed Cost, Adjustments & Expenses",
                                badgeText = Formatters.formatNumber(
                                    (importCosts.size + additionalCosts.size + costAdjustments.size + directExpenseVouchers.size).toLong(),
                                    language
                                )
                            ) {
                                importCosts.forEach { cost ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(cost.description.ifBlank { cost.costCategory }, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        Text(ImportVaultUiLogic.formatBdt(cost.bdtAmountMinor, language), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                                additionalCosts.forEach { ac ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${ac.description} (${if (ac.isCapitalizable) "Cap" else "Op"})", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        Text(ImportVaultUiLogic.formatBdt(ac.bdtAmountMinor, language), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                                costAdjustments.forEach { adj ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${adj.adjustmentNumber}: ${adj.reason}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        Text(ImportVaultUiLogic.formatBdt(adj.totalAdjustmentAmountMinor, language), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                                directExpenseVouchers.forEach { ev ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${ev.voucherNumber}: ${if (isBn) ev.descriptionBn.ifBlank { ev.descriptionEn } else ev.descriptionEn}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        Text(ImportVaultUiLogic.formatBdt(ev.bdtAmountMinor, language), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item(key = "overview_source_transactions") {
                        VaultExpandableSection(
                            title = if (isBn) "ইনভেন্টরি, বিক্রয়, ফেরত ও ক্ষতি রেকর্ড" else "Inventory, Sales, Returns & Losses Sources",
                            badgeText = Formatters.formatNumber(
                                (costLayers.size + saleSources.size + returnSources.size + damageRecords.size + shortageRecords.size).toLong(),
                                language
                            )
                        ) {
                            Text(
                                text = "${if (isBn) "বিক্রয় অ্যালোকেশন" else "Sales Lines"}: ${saleSources.size} · ${if (isBn) "ফেরত" else "Returns"}: ${returnSources.size} · ${if (isBn) "FIFO লেয়ার" else "Cost Layers"}: ${costLayers.size}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            saleSources.take(8).forEach { s ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${s.invoiceNumber} · ${s.quantity}u", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(ImportVaultUiLogic.formatBdt(s.netRevenueMinor, language), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            returnSources.take(5).forEach { r ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${r.returnNumber} (Good ${r.soundQuantity}/Dmg ${r.damagedQuantity})", style = MaterialTheme.typography.bodySmall, color = StatusError, modifier = Modifier.weight(1f))
                                    Text("-${ImportVaultUiLogic.formatBdt(r.reversedNetRevenueMinor, language)}", style = MaterialTheme.typography.bodySmall, color = StatusError)
                                }
                            }
                            damageRecords.forEach { d ->
                                Text(
                                    text = "${d.recordNumber} · Damage ${d.damagedQuantity}u · ${ImportVaultUiLogic.formatBdt(d.totalLossMinor, language)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusError
                                )
                            }
                            shortageRecords.forEach { sh ->
                                Text(
                                    text = "${sh.recordNumber} · Missing ${sh.missingQuantity}u (${sh.disposition}) · ${ImportVaultUiLogic.formatBdt(sh.totalAmountMinor, language)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusWarning
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onNavigateToRoute("sales") },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                ) {
                                    Text(if (isBn) "বিক্রয় মডিউল" else "Sales")
                                }
                                OutlinedButton(
                                    onClick = { onNavigateToRoute("inventory") },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                ) {
                                    Text(if (isBn) "ইনভেন্টরি" else "Inventory")
                                }
                            }
                        }
                    }
                }

                1 -> {
                    profitIntelligenceItems(
                        scopeTitle = displayTitle,
                        parentNetProfitMinor = attributedNetProfitMinor,
                        netRevenueMinor = netRevenueMinor,
                        cogsMinor = cogsMinor,
                        expenseAndLossMinor = directExpensesMinor + allocatedSharedOverheadMinor + inventoryLossesMinor,
                        profitLabel = profitLabel,
                        products = products,
                        shipments = emptyList(),
                        purchaseOrders = emptyList(),
                        sortOption = sortOption,
                        selectedTab = profitSubTab,
                        compareScopeIdx = compareSubIdx,
                        language = language,
                        onTabSelected = { profitSubTab = it },
                        onCompareScopeSelected = { compareSubIdx = it },
                        onSortChange = onSortChange,
                        onOpenProduct = onOpenProduct,
                        salesTrend = salesTrend,
                        saleSources = saleSources,
                        onDrillTransactions = { title, rows -> drillDialogState = title to rows }
                    )
                }

                2 -> {
                    capitalIntelligenceItems(
                        scopeTitle = displayTitle,
                        cashRecovery = cashRecovery,
                        lockedInventoryCapitalMinor = lockedInventoryCapitalMinor,
                        supplierPaidMinor = supplierPaidMinor,
                        supplierPayableMinor = supplierPayableMinor,
                        expectedRemainingProfit = expectedRemainingProfit,
                        agingSummary = agingSummary,
                        unknownBucket = null,
                        expenseAllocationMode = expenseAllocationMode,
                        activeRuleId = null,
                        unallocatedSharedOverheadMinor = null,
                        allocatedSharedOverheadMinor = allocatedSharedOverheadMinor,
                        availableRules = emptyList(),
                        costLayers = costLayers,
                        saleSources = saleSources,
                        language = language,
                        onSelectAllocationRule = null,
                        onNavigateToRoute = onNavigateToRoute
                    )
                }

                3 -> {
                    timelineDocumentsItems(
                        documents = documentRefs,
                        timelineEvents = timelineEvents,
                        subTabIdx = tlDocSubTab,
                        selectedDocCategory = selectedDocCategory,
                        docSearchQuery = docSearchQuery,
                        language = language,
                        onSubTabChange = { tlDocSubTab = it },
                        onDocCategorySelected = { selectedDocCategory = it },
                        onDocSearchQueryChange = { docSearchQuery = it },
                        onAddAttachmentClick = onAddAttachmentClick,
                        onOpenExportClick = onOpenExport,
                        onPreviewDocument = onPreviewDocument,
                        onShareDocument = onShareDocument,
                        onDeleteAttachment = onDeleteAttachment,
                        onNavigateToRoute = onNavigateToRoute
                    )
                }
            }
        }
    }
}
