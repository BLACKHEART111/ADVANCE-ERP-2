package com.pocketdev.advance_erp.feature.importvault

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketdev.advance_erp.core.designsystem.FullScreenImageDialog
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.pdf.PdfViewerDialog
import com.pocketdev.advance_erp.core.util.ControlCenterShareService
import com.pocketdev.advance_erp.core.util.ImportVaultAttachmentFileStore
import com.pocketdev.advance_erp.core.util.ImportVaultAttachmentStorage
import com.pocketdev.advance_erp.core.util.VaultExportFormat
import com.pocketdev.advance_erp.core.util.VaultExportScope
import com.pocketdev.advance_erp.domain.model.VaultDocumentRef
import com.pocketdev.advance_erp.domain.model.VaultScopeType
import java.io.File

@Composable
fun ImportVaultRootScreen(
    onNavigateToRoute: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ImportVaultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isBn = uiState.language == AppLanguage.BANGLA

    var exportSheetConfig by remember { mutableStateOf<VaultExportSheetLaunchConfig?>(null) }
    var previewImagePath by remember { mutableStateOf<String?>(null) }
    var previewPdfPair by remember { mutableStateOf<Pair<File, String>?>(null) }
    var pendingAttachScope by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pickedAttachment by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    var pendingDeleteAttachmentId by remember { mutableStateOf<String?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val scopePair = pendingAttachScope
        if (uri != null && scopePair != null) {
            val defaultTitle = resolvePickedDisplayName(context, uri)
            pickedAttachment = uri to defaultTitle
        } else {
            pendingAttachScope = null
        }
    }

    LaunchedEffect(uiState.actionMessage) {
        val msg = uiState.actionMessage
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
    }

    BackHandler(enabled = uiState.vaultStack.size > 1) {
        viewModel.navigateBack()
    }

    fun handlePreviewDocument(doc: VaultDocumentRef) {
        val path = doc.localFilePath?.trim().orEmpty()
        if (path.isBlank()) return
        val file = File(path.removePrefix("file://"))
        if (!file.exists()) {
            Toast.makeText(
                context,
                if (isBn) "নথিটি ডিভাইসে পাওয়া যায়নি" else "Document file not found on device",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        when {
            ImportVaultAttachmentFileStore.isImage(file.absolutePath, doc.mimeType) -> {
                previewImagePath = file.absolutePath
            }
            ImportVaultAttachmentFileStore.isPdf(file.absolutePath, doc.mimeType) -> {
                previewPdfPair = file to (if (isBn) doc.titleBn else doc.titleEn)
            }
            else -> {
                ImportVaultAttachmentStorage.openVaultFile(context, file.absolutePath, isBn, uiState.userRole)
            }
        }
    }

    fun handleShareDocument(doc: VaultDocumentRef) {
        val path = doc.localFilePath?.trim().orEmpty()
        if (path.isBlank()) return
        ImportVaultAttachmentStorage.shareVaultFile(
            context = context,
            filePath = path,
            mimeType = doc.mimeType,
            isBangla = isBn,
            role = uiState.userRole
        )
    }

    ImportVaultAccessGate(
        userRole = uiState.userRole,
        language = uiState.language,
        onUnlocked = { fallbackNotice -> viewModel.onVaultUnlocked(fallbackNotice) },
        onLocked = { viewModel.onVaultLocked() },
        onBack = {
            if (!viewModel.navigateBack()) {
                onNavigateBack()
            }
        }
    ) {
        when (val activeRoute = uiState.vaultStack.lastOrNull() ?: VaultRoute.Home) {
            is VaultRoute.Home -> {
                ImportVaultHomeScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.refreshAll() },
                    onSearchChange = { viewModel.updateSearchQuery(it) },
                    onApplyFilter = { preset, customFrom, customTo, customAsOf, dateFilterTarget, supplierId, warehouseId, opStatus, comStatus, onlyProf, onlyLoss, onlyAging, onlyPending, ruleId ->
                        viewModel.applyFilter(
                            datePreset = preset,
                            customFromMillis = customFrom,
                            customToMillis = customTo,
                            customAsOfMillis = customAsOf,
                            dateFilterTarget = dateFilterTarget,
                            supplierId = supplierId,
                            warehouseId = warehouseId,
                            operationalStatus = opStatus,
                            commercialStatus = comStatus,
                            onlyProfitable = onlyProf,
                            onlyLossMaking = onlyLoss,
                            onlyHighAging = onlyAging,
                            onlyPendingCost = onlyPending,
                            allocationRuleId = ruleId
                        )
                    },
                    onClearFilters = { viewModel.clearFilters() },
                    onShelfModeChange = { viewModel.setShelfMode(it) },
                    onLayoutModeChange = { viewModel.setLayoutMode(it) },
                    onArchiveFilterChange = { viewModel.setArchiveFilter(it) },
                    onOpenShipment = { viewModel.openShipment(it) },
                    onOpenPo = { viewModel.openPurchaseOrder(it) },
                    onOpenPortfolioProfit = { viewModel.openPortfolioProfit() },
                    onOpenPortfolioCapital = { viewModel.openPortfolioCapital() },
                    onTogglePin = { scopeType, sourceId, pinned ->
                        viewModel.togglePin(scopeType, sourceId, pinned)
                    },
                    onToggleArchive = { scopeType, sourceId, status ->
                        viewModel.toggleArchive(scopeType, sourceId, status)
                    },
                    onSaveMetadata = { scopeType, sourceId, title, notes, tags ->
                        viewModel.saveEntryMetadata(scopeType, sourceId, title, notes, tags)
                    },
                    onOpenExport = {
                        exportSheetConfig = VaultExportSheetLaunchConfig(
                            scope = VaultExportScope.PORTFOLIO,
                            targetTitle = if (isBn) "পোর্টফোলিও" else "Portfolio"
                        )
                    },
                    onAddPortfolioAttachmentClick = {
                        pendingAttachScope = "PORTFOLIO" to "portfolio"
                        openDocumentLauncher.launch(arrayOf("image/*", "application/pdf"))
                    },
                    onPreviewDocument = ::handlePreviewDocument,
                    onShareDocument = ::handleShareDocument,
                    onDeleteAttachment = { attId -> pendingDeleteAttachmentId = attId },
                    onNavigateToRoute = onNavigateToRoute
                )
            }

            is VaultRoute.ShipmentDetail -> {
                val shpTitle = uiState.selectedShipmentDetail?.summary?.displayTitle ?: activeRoute.shipmentId
                ImportVaultShipmentDetailScreen(
                    uiState = uiState,
                    onBack = { viewModel.navigateBack() },
                    onOpenPo = { viewModel.openPurchaseOrder(it) },
                    onOpenProduct = { productId ->
                        viewModel.openProductDrillDown(
                            scopeType = VaultScopeType.SHIPMENT,
                            scopeId = activeRoute.shipmentId,
                            productId = productId
                        )
                    },
                    onSortChange = { viewModel.setProfitSort(it) },
                    onTogglePin = { scopeType, id, pinned -> viewModel.togglePin(scopeType, id, pinned) },
                    onSaveMetadata = { scopeType, id, title, notes, tags ->
                        viewModel.saveEntryMetadata(scopeType, id, title, notes, tags)
                    },
                    onOpenExport = {
                        exportSheetConfig = VaultExportSheetLaunchConfig(
                            scope = VaultExportScope.SHIPMENT,
                            scopeId = activeRoute.shipmentId,
                            targetTitle = shpTitle
                        )
                    },
                    onAddAttachmentClick = {
                        pendingAttachScope = VaultScopeType.SHIPMENT.name to activeRoute.shipmentId
                        openDocumentLauncher.launch(arrayOf("image/*", "application/pdf"))
                    },
                    onPreviewDocument = ::handlePreviewDocument,
                    onShareDocument = ::handleShareDocument,
                    onDeleteAttachment = { attId -> pendingDeleteAttachmentId = attId },
                    onNavigateToRoute = onNavigateToRoute
                )
            }

            is VaultRoute.PoDetail -> {
                val poTitle = uiState.selectedPoDetail?.summary?.displayTitle ?: activeRoute.purchaseOrderId
                ImportVaultPoDetailScreen(
                    uiState = uiState,
                    onBack = { viewModel.navigateBack() },
                    onOpenShipment = { viewModel.openShipment(it) },
                    onOpenProduct = { productId ->
                        viewModel.openProductDrillDown(
                            scopeType = VaultScopeType.PURCHASE_ORDER,
                            scopeId = activeRoute.purchaseOrderId,
                            productId = productId
                        )
                    },
                    onSortChange = { viewModel.setProfitSort(it) },
                    onTogglePin = { scopeType, id, pinned -> viewModel.togglePin(scopeType, id, pinned) },
                    onSaveMetadata = { scopeType, id, title, notes, tags ->
                        viewModel.saveEntryMetadata(scopeType, id, title, notes, tags)
                    },
                    onOpenExport = {
                        exportSheetConfig = VaultExportSheetLaunchConfig(
                            scope = VaultExportScope.PURCHASE_ORDER,
                            scopeId = activeRoute.purchaseOrderId,
                            targetTitle = poTitle
                        )
                    },
                    onAddAttachmentClick = {
                        pendingAttachScope = VaultScopeType.PURCHASE_ORDER.name to activeRoute.purchaseOrderId
                        openDocumentLauncher.launch(arrayOf("image/*", "application/pdf"))
                    },
                    onPreviewDocument = ::handlePreviewDocument,
                    onShareDocument = ::handleShareDocument,
                    onDeleteAttachment = { attId -> pendingDeleteAttachmentId = attId },
                    onNavigateToRoute = onNavigateToRoute
                )
            }

            is VaultRoute.PortfolioProfit -> {
                ImportVaultPortfolioProfitScreen(
                    uiState = uiState,
                    onBack = { viewModel.navigateBack() },
                    onSortChange = { viewModel.setProfitSort(it) },
                    onOpenProduct = { productId ->
                        viewModel.openProductDrillDown(
                            scopeType = null,
                            scopeId = null,
                            productId = productId
                        )
                    },
                    onOpenExport = {
                        exportSheetConfig = VaultExportSheetLaunchConfig(
                            scope = VaultExportScope.PORTFOLIO,
                            targetTitle = if (isBn) "মুনাফা বিশ্লেষণ" else "Profit Intelligence"
                        )
                    },
                    onNavigateToRoute = onNavigateToRoute
                )
            }

            is VaultRoute.PortfolioCapital -> {
                ImportVaultPortfolioCapitalScreen(
                    uiState = uiState,
                    onBack = { viewModel.navigateBack() },
                    onSelectAllocationRule = { viewModel.setAllocationRuleOverride(it) },
                    onOpenExport = {
                        exportSheetConfig = VaultExportSheetLaunchConfig(
                            scope = VaultExportScope.PORTFOLIO,
                            targetTitle = if (isBn) "মূলধন বিশ্লেষণ" else "Capital Intelligence"
                        )
                    },
                    onCreateAllocationRule = { ruleName, basis, startMs, endMs, targetScope, activate, ruleNotes ->
                        viewModel.createOverheadAllocationRule(
                            ruleName = ruleName,
                            allocationBasis = basis,
                            periodStartMillis = startMs,
                            periodEndMillis = endMs,
                            targetScopeType = targetScope,
                            activateImmediately = activate,
                            notes = ruleNotes
                        )
                    },
                    onToggleAllocationRuleActive = { ruleId, isActive ->
                        viewModel.setOverheadRuleActive(ruleId, isActive)
                    },
                    onNavigateToRoute = onNavigateToRoute
                )
            }

            is VaultRoute.ProductDrillDown -> {
                val scopeTitle = when (activeRoute.scopeType) {
                    VaultScopeType.SHIPMENT -> uiState.selectedShipmentDetail?.summary?.displayTitle
                        ?: activeRoute.scopeId.orEmpty()
                    VaultScopeType.PURCHASE_ORDER -> uiState.selectedPoDetail?.summary?.displayTitle
                        ?: activeRoute.scopeId.orEmpty()
                    null -> if (isBn) "পোর্টফোলিও বিশ্লেষণ" else "Portfolio Analysis"
                }
                val product = when (activeRoute.scopeType) {
                    VaultScopeType.SHIPMENT -> uiState.selectedShipmentDetail?.products
                        ?.find { it.productId == activeRoute.productId }
                    VaultScopeType.PURCHASE_ORDER -> uiState.selectedPoDetail?.products
                        ?.find { it.productId == activeRoute.productId }
                    null -> uiState.portfolioProducts.find { it.productId == activeRoute.productId }
                }
                ImportVaultProductDrillDownScreen(
                    product = product,
                    scopeTitle = scopeTitle,
                    language = uiState.language,
                    sortOption = uiState.profitSort,
                    onBack = { viewModel.navigateBack() },
                    onSortChange = { viewModel.setProfitSort(it) },
                    onOpenExportProduct = {
                        if (product != null) {
                            exportSheetConfig = VaultExportSheetLaunchConfig(
                                scope = VaultExportScope.PRODUCT,
                                scopeId = product.productId,
                                parentScopeType = activeRoute.scopeType,
                                parentScopeId = activeRoute.scopeId,
                                targetTitle = if (isBn) product.productNameBn else product.productNameEn
                            )
                        }
                    },
                    onOpenExportVariant = { variant ->
                        if (product != null) {
                            exportSheetConfig = VaultExportSheetLaunchConfig(
                                scope = VaultExportScope.VARIANT,
                                scopeId = product.productId,
                                variantId = variant.variantId ?: variant.sku,
                                parentScopeType = activeRoute.scopeType,
                                parentScopeId = activeRoute.scopeId,
                                targetTitle = "${if (isBn) product.productNameBn else product.productNameEn} (${variant.variantLabel})"
                            )
                        }
                    },
                    onNavigateToRoute = onNavigateToRoute
                )
            }
        }
    }

    exportSheetConfig?.let { launchCfg ->
        ImportVaultExportSheet(
            launchConfig = launchCfg,
            uiState = uiState,
            onDismiss = { exportSheetConfig = null },
            onRequestPreview = { cfg, presetOverride ->
                viewModel.prepareExportPreview(cfg, presetOverride)
            },
            onGenerateAndPreview = { cfg, format, presetOverride, saveSnapshot ->
                viewModel.generateExportFile(cfg, format, presetOverride, saveSnapshot) { file, pkg ->
                    exportSheetConfig = null
                    if (format == VaultExportFormat.PDF) {
                        previewPdfPair = file to (if (pkg.isBangla) pkg.titleBn else pkg.titleEn)
                    } else {
                        ImportVaultAttachmentStorage.openVaultFile(context, file.absolutePath, pkg.isBangla, uiState.userRole)
                    }
                }
            },
            onGenerateAndShare = { cfg, format, presetOverride ->
                viewModel.generateExportFile(cfg, format, presetOverride, saveAsImmutableSnapshot = false) { file, pkg ->
                    val mime = if (format == VaultExportFormat.PDF) {
                        "application/pdf"
                    } else {
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    }
                    ControlCenterShareService.shareFile(
                        context = context,
                        file = file,
                        mimeType = mime,
                        chooserTitle = if (pkg.isBangla) pkg.titleBn else pkg.titleEn
                    )
                }
            }
        )
    }

    previewImagePath?.let { imgPath ->
        FullScreenImageDialog(
            imageUriOrPath = imgPath,
            onDismiss = { previewImagePath = null }
        )
    }

    previewPdfPair?.let { (pdfFile, title) ->
        PdfViewerDialog(
            pdfFile = pdfFile,
            reportTitle = title,
            isBangla = isBn,
            onDismiss = { previewPdfPair = null }
        )
    }

    val activeAttachTarget = pendingAttachScope
    val activePicked = pickedAttachment
    if (activeAttachTarget != null && activePicked != null) {
        VaultSaveAttachmentDialog(
            initialTitle = activePicked.second,
            language = uiState.language,
            onDismiss = {
                pickedAttachment = null
                pendingAttachScope = null
            },
            onSave = { title, category ->
                val (scopeType, sourceId) = activeAttachTarget
                val (uri, _) = activePicked
                pickedAttachment = null
                pendingAttachScope = null
                viewModel.attachLocalDocumentForScope(
                    scopeType = scopeType,
                    sourceId = sourceId,
                    sourceUri = uri,
                    category = category,
                    title = title
                )
            }
        )
    }

    pendingDeleteAttachmentId?.let { attId ->
        VaultAttachmentDeleteConfirmDialog(
            language = uiState.language,
            onDismiss = { pendingDeleteAttachmentId = null },
            onConfirmDelete = {
                pendingDeleteAttachmentId = null
                viewModel.deleteLocalVaultAttachment(attId)
            }
        )
    }
}

private fun resolvePickedDisplayName(context: Context, uri: Uri): String {
    if (uri.scheme == "file") {
        return File(uri.path.orEmpty()).nameWithoutExtension.ifBlank { "Vault Document" }
    }
    try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    val name = cursor.getString(idx)
                    if (!name.isNullOrBlank()) {
                        return name.substringBeforeLast('.').ifBlank { name }
                    }
                }
            }
        }
    } catch (_: Exception) {
    }
    return uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')?.ifBlank { "Vault Document" }
        ?: "Vault Document"
}
