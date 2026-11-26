package com.pocketdev.advance_erp.feature.importvault

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.security.SecurityManager
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.core.util.ImportVaultAttachmentStorage
import com.pocketdev.advance_erp.core.util.ImportVaultExcelGenerator
import com.pocketdev.advance_erp.core.util.ImportVaultExportBuilder
import com.pocketdev.advance_erp.core.util.ImportVaultPdfGenerator
import com.pocketdev.advance_erp.core.util.VaultExportFormat
import com.pocketdev.advance_erp.core.util.VaultExportPackage
import com.pocketdev.advance_erp.core.util.VaultExportRequest
import com.pocketdev.advance_erp.core.util.VaultExportScope
import com.pocketdev.advance_erp.domain.model.CommercialStatus
import com.pocketdev.advance_erp.domain.model.ImportVaultFilter
import com.pocketdev.advance_erp.domain.model.ImportVaultFilterOptions
import com.pocketdev.advance_erp.domain.model.ImportVaultOverview
import com.pocketdev.advance_erp.domain.model.OperationalStatus
import com.pocketdev.advance_erp.domain.model.ProductVaultRow
import com.pocketdev.advance_erp.domain.model.PurchaseOrderVaultDetail
import com.pocketdev.advance_erp.domain.model.ShipmentVaultDetail
import com.pocketdev.advance_erp.domain.model.VaultDateFilterTarget
import com.pocketdev.advance_erp.domain.model.VaultDocumentCategory
import com.pocketdev.advance_erp.domain.model.VaultScopeType
import com.pocketdev.advance_erp.domain.repository.ImportVaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed interface VaultRoute {
    data object Home : VaultRoute
    data class ShipmentDetail(val shipmentId: String) : VaultRoute
    data class PoDetail(val purchaseOrderId: String) : VaultRoute
    data object PortfolioProfit : VaultRoute
    data object PortfolioCapital : VaultRoute
    data class ProductDrillDown(
        val scopeType: VaultScopeType?,
        val scopeId: String?,
        val productId: String
    ) : VaultRoute
}

data class ImportVaultUiState(
    val userRole: UserRole = UserRole.OWNER_ADMIN,
    val language: AppLanguage = AppLanguage.BANGLA,
    val isUnlocked: Boolean = false,
    val isBiometricFallbackNotice: Boolean = false,
    val isLoadingOverview: Boolean = false,
    val isLoadingDetail: Boolean = false,
    val isSavingMetadata: Boolean = false,
    val errorMessage: String? = null,
    val overview: ImportVaultOverview? = null,
    val portfolioProducts: List<ProductVaultRow> = emptyList(),
    val selectedShipmentDetail: ShipmentVaultDetail? = null,
    val selectedPoDetail: PurchaseOrderVaultDetail? = null,
    val filter: ImportVaultFilter = ImportVaultFilter(),
    val filterOptions: ImportVaultFilterOptions = ImportVaultFilterOptions(),
    val datePreset: VaultDatePreset = VaultDatePreset.LIFETIME,
    val searchText: String = "",
    val shelfMode: VaultShelfMode = VaultShelfMode.SHIPMENTS,
    val layoutMode: VaultLayoutMode = VaultLayoutMode.COMPACT_LIST,
    val archiveFilter: VaultArchiveFilter = VaultArchiveFilter.ACTIVE,
    val profitSort: ProfitSortOption = ProfitSortOption.HIGHEST_PROFIT,
    val vaultStack: List<VaultRoute> = listOf(VaultRoute.Home),
    val isExporting: Boolean = false,
    val isUploadingAttachment: Boolean = false,
    val exportPreviewPackage: VaultExportPackage? = null,
    val actionMessage: String? = null
)

@HiltViewModel
class ImportVaultViewModel @Inject constructor(
    private val repository: ImportVaultRepository,
    private val preferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportVaultUiState())
    val uiState: StateFlow<ImportVaultUiState> = _uiState.asStateFlow()

    private var hasSyncedEntries = false
    private var overviewJob: Job? = null
    private var detailJob: Job? = null
    private var searchDebounceJob: Job? = null

    init {
        viewModelScope.launch {
            preferencesRepository.selectedUserRole.collect { role ->
                val allowed = SecurityManager.canAccessImportVault(role)
                _uiState.update { state ->
                    if (!allowed) {
                        state.copy(
                            userRole = role,
                            isUnlocked = false,
                            overview = null,
                            portfolioProducts = emptyList(),
                            selectedShipmentDetail = null,
                            selectedPoDetail = null
                        )
                    } else {
                        state.copy(userRole = role)
                    }
                }
            }
        }
        viewModelScope.launch {
            preferencesRepository.selectedLanguage.collect { lang ->
                _uiState.update { it.copy(language = lang) }
            }
        }
    }

    fun onVaultUnlocked(biometricFallbackActive: Boolean) {
        val current = _uiState.value
        if (!SecurityManager.canAccessImportVault(current.userRole)) return
        val wasUnlocked = current.isUnlocked
        _uiState.update {
            it.copy(
                isUnlocked = true,
                isBiometricFallbackNotice = biometricFallbackActive,
                errorMessage = null
            )
        }
        if (!wasUnlocked || current.overview == null) {
            refreshAll()
        }
    }

    fun onVaultLocked() {
        overviewJob?.cancel()
        detailJob?.cancel()
        _uiState.update {
            it.copy(
                isUnlocked = false,
                isLoadingOverview = false,
                isLoadingDetail = false,
                overview = null,
                portfolioProducts = emptyList(),
                selectedShipmentDetail = null,
                selectedPoDetail = null
            )
        }
    }

    fun refreshAll() {
        val current = _uiState.value
        if (!current.isUnlocked || !SecurityManager.canAccessImportVault(current.userRole)) return

        overviewJob?.cancel()
        overviewJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOverview = true, errorMessage = null) }
            try {
                if (!hasSyncedEntries) {
                    repository.syncVaultEntries(_uiState.value.userRole)
                    hasSyncedEntries = true
                }
                val activeFilter = _uiState.value.filter.copy(nowMillis = System.currentTimeMillis())
                val options = repository.getFilterOptions(_uiState.value.userRole)
                val overview = repository.getOverview(_uiState.value.userRole, activeFilter)
                val products = repository.getProductProfitability(_uiState.value.userRole, activeFilter)
                _uiState.update {
                    it.copy(
                        isLoadingOverview = false,
                        overview = overview,
                        portfolioProducts = products,
                        filterOptions = options,
                        errorMessage = null
                    )
                }
                reloadActiveDetailIfNeeded()
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingOverview = false,
                        errorMessage = e.message ?: "Failed to load Import Vault analytics"
                    )
                }
            }
        }
    }

    private fun reloadActiveDetailIfNeeded() {
        val current = _uiState.value
        if (!current.isUnlocked) return
        val topRoute = current.vaultStack.lastOrNull()
        val scopeToLoad: Pair<VaultScopeType, String>? = when (topRoute) {
            is VaultRoute.ShipmentDetail -> VaultScopeType.SHIPMENT to topRoute.shipmentId
            is VaultRoute.PoDetail -> VaultScopeType.PURCHASE_ORDER to topRoute.purchaseOrderId
            is VaultRoute.ProductDrillDown -> {
                if (topRoute.scopeType != null && topRoute.scopeId != null) {
                    topRoute.scopeType to topRoute.scopeId
                } else null
            }
            else -> null
        }
        if (scopeToLoad == null) return
        val (scopeType, id) = scopeToLoad
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true) }
            try {
                val activeFilter = _uiState.value.filter.copy(nowMillis = System.currentTimeMillis())
                when (scopeType) {
                    VaultScopeType.SHIPMENT -> {
                        val detail = repository.getShipmentDetail(_uiState.value.userRole, id, activeFilter)
                        _uiState.update {
                            it.copy(isLoadingDetail = false, selectedShipmentDetail = detail)
                        }
                    }
                    VaultScopeType.PURCHASE_ORDER -> {
                        val detail = repository.getPurchaseOrderDetail(_uiState.value.userRole, id, activeFilter)
                        _uiState.update {
                            it.copy(isLoadingDetail = false, selectedPoDetail = detail)
                        }
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoadingDetail = false, errorMessage = e.message ?: "Failed to load folder details")
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchText = query,
                filter = state.filter.copy(searchQuery = query.ifBlank { null })
            )
        }
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(200)
            refreshAll()
        }
    }

    fun applyFilter(
        datePreset: VaultDatePreset,
        customFromMillis: Long? = null,
        customToMillis: Long? = null,
        customAsOfMillis: Long? = null,
        dateFilterTarget: VaultDateFilterTarget = VaultDateFilterTarget.ACTIVITY_DATES,
        supplierId: String?,
        warehouseId: String?,
        operationalStatus: OperationalStatus?,
        commercialStatus: CommercialStatus?,
        onlyProfitable: Boolean,
        onlyLossMaking: Boolean,
        onlyHighAging: Boolean,
        onlyPendingCost: Boolean,
        allocationRuleId: String?
    ) {
        val now = System.currentTimeMillis()
        val (dateFrom, dateTo, asOf) = ImportVaultUiLogic.computeDateRangeForPreset(
            preset = datePreset,
            customFromMillis = customFromMillis,
            customToMillis = customToMillis,
            customAsOfMillis = customAsOfMillis,
            nowMillis = now
        )

        _uiState.update { state ->
            state.copy(
                datePreset = datePreset,
                filter = state.filter.copy(
                    dateFromMillis = dateFrom,
                    dateToMillis = dateTo,
                    asOfMillis = asOf,
                    supplierId = supplierId,
                    warehouseId = warehouseId,
                    operationalStatus = operationalStatus,
                    commercialStatus = commercialStatus,
                    onlyProfitable = onlyProfitable,
                    onlyLossMaking = onlyLossMaking,
                    onlyHighAging = onlyHighAging,
                    onlyPendingCost = onlyPendingCost,
                    allocationRuleId = allocationRuleId,
                    nowMillis = now,
                    dateFilterTarget = dateFilterTarget
                )
            )
        }
        refreshAll()
    }

    fun clearFilters() {
        _uiState.update { state ->
            state.copy(
                datePreset = VaultDatePreset.LIFETIME,
                searchText = "",
                filter = ImportVaultFilter()
            )
        }
        refreshAll()
    }

    fun setShelfMode(mode: VaultShelfMode) {
        _uiState.update { it.copy(shelfMode = mode) }
    }

    fun setLayoutMode(mode: VaultLayoutMode) {
        _uiState.update { it.copy(layoutMode = mode) }
    }

    fun setArchiveFilter(filter: VaultArchiveFilter) {
        _uiState.update { it.copy(archiveFilter = filter) }
    }

    fun setProfitSort(sort: ProfitSortOption) {
        _uiState.update { it.copy(profitSort = sort) }
    }

    fun openShipment(shipmentId: String) {
        _uiState.update {
            it.copy(
                selectedShipmentDetail = null,
                vaultStack = it.vaultStack + VaultRoute.ShipmentDetail(shipmentId)
            )
        }
        reloadActiveDetailIfNeeded()
    }

    fun openPurchaseOrder(purchaseOrderId: String) {
        _uiState.update {
            it.copy(
                selectedPoDetail = null,
                vaultStack = it.vaultStack + VaultRoute.PoDetail(purchaseOrderId)
            )
        }
        reloadActiveDetailIfNeeded()
    }

    fun openPortfolioProfit() {
        _uiState.update { it.copy(vaultStack = it.vaultStack + VaultRoute.PortfolioProfit) }
    }

    fun openPortfolioCapital() {
        _uiState.update { it.copy(vaultStack = it.vaultStack + VaultRoute.PortfolioCapital) }
    }

    fun openProductDrillDown(scopeType: VaultScopeType?, scopeId: String?, productId: String) {
        _uiState.update {
            it.copy(vaultStack = it.vaultStack + VaultRoute.ProductDrillDown(scopeType, scopeId, productId))
        }
    }

    fun navigateBack(): Boolean {
        val stack = _uiState.value.vaultStack
        if (stack.size <= 1) return false
        _uiState.update { it.copy(vaultStack = stack.dropLast(1)) }
        reloadActiveDetailIfNeeded()
        return true
    }

    fun togglePin(scopeType: VaultScopeType, sourceId: String, currentlyPinned: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingMetadata = true) }
            try {
                repository.updateVaultEntryMetadata(
                    role = _uiState.value.userRole,
                    scopeType = scopeType,
                    sourceId = sourceId,
                    isPinned = !currentlyPinned
                )
                refreshAll()
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                _uiState.update { it.copy(isSavingMetadata = false) }
            }
        }
    }

    fun toggleArchive(scopeType: VaultScopeType, sourceId: String, currentStatus: String) {
        val nextStatus = if (currentStatus == "ARCHIVED") "ACTIVE" else "ARCHIVED"
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingMetadata = true) }
            try {
                repository.updateVaultEntryMetadata(
                    role = _uiState.value.userRole,
                    scopeType = scopeType,
                    sourceId = sourceId,
                    archiveStatus = nextStatus
                )
                refreshAll()
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                _uiState.update { it.copy(isSavingMetadata = false) }
            }
        }
    }

    fun saveEntryMetadata(
        scopeType: VaultScopeType,
        sourceId: String,
        displayTitle: String,
        notes: String,
        tags: List<String>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingMetadata = true) }
            try {
                repository.updateVaultEntryMetadata(
                    role = _uiState.value.userRole,
                    scopeType = scopeType,
                    sourceId = sourceId,
                    displayTitle = displayTitle,
                    notes = notes,
                    tagsJson = ImportVaultUiLogic.encodeTags(tags)
                )
                refreshAll()
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                _uiState.update { it.copy(isSavingMetadata = false) }
            }
        }
    }

    fun setAllocationRuleOverride(ruleId: String?) {
        _uiState.update { it.copy(filter = it.filter.copy(allocationRuleId = ruleId)) }
        refreshAll()
    }

    fun createOverheadAllocationRule(
        ruleName: String,
        allocationBasis: String,
        periodStartMillis: Long,
        periodEndMillis: Long,
        targetScopeType: VaultScopeType,
        activateImmediately: Boolean,
        notes: String?
    ) {
        val role = _uiState.value.userRole
        if (!SecurityManager.canAccessImportVault(role)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingMetadata = true) }
            try {
                val ruleId = repository.createOrActivateExpenseAllocationRule(
                    role = role,
                    ruleName = ruleName.trim(),
                    allocationBasis = allocationBasis,
                    periodStartMillis = periodStartMillis,
                    periodEndMillis = periodEndMillis,
                    targetScopeType = targetScopeType,
                    activateImmediately = activateImmediately,
                    notes = notes?.trim()?.takeIf { it.isNotEmpty() }
                )
                val msg = if (_uiState.value.language == AppLanguage.BANGLA) {
                    "ওভারহেড বণ্টন নিয়ম সংরক্ষিত হয়েছে"
                } else {
                    "Shared overhead rule saved"
                }
                _uiState.update { state ->
                    state.copy(
                        isSavingMetadata = false,
                        filter = if (activateImmediately) state.filter.copy(allocationRuleId = ruleId) else state.filter,
                        actionMessage = msg
                    )
                }
                refreshAll()
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingMetadata = false,
                        actionMessage = e.message ?: "Failed to create overhead allocation rule"
                    )
                }
            }
        }
    }

    fun setOverheadRuleActive(ruleId: String, isActive: Boolean) {
        val role = _uiState.value.userRole
        if (!SecurityManager.canAccessImportVault(role)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingMetadata = true) }
            try {
                repository.setExpenseAllocationRuleActive(role = role, ruleId = ruleId, isActive = isActive)
                val nextOverride = if (isActive) ruleId else null
                _uiState.update { state ->
                    state.copy(
                        isSavingMetadata = false,
                        filter = state.filter.copy(allocationRuleId = nextOverride)
                    )
                }
                refreshAll()
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingMetadata = false,
                        actionMessage = e.message ?: "Failed to update overhead rule"
                    )
                }
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    private fun buildEffectiveExportRequest(
        config: VaultExportSheetLaunchConfig,
        datePresetOverride: VaultDatePreset?
    ): VaultExportRequest {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val effectiveFilter = if (datePresetOverride != null) {
            val (from, to, asOf) = ImportVaultUiLogic.computeDateRangeForPreset(
                preset = datePresetOverride,
                nowMillis = now
            )
            state.filter.copy(
                dateFromMillis = from,
                dateToMillis = to,
                asOfMillis = asOf,
                nowMillis = now
            )
        } else {
            state.filter.copy(nowMillis = now)
        }
        return VaultExportRequest(
            scope = config.scope,
            scopeId = config.scopeId,
            variantId = config.variantId,
            parentScopeType = config.parentScopeType,
            parentScopeId = config.parentScopeId,
            filter = effectiveFilter,
            language = state.language,
            role = state.userRole,
            businessProfile = null
        )
    }

    fun prepareExportPreview(config: VaultExportSheetLaunchConfig, datePresetOverride: VaultDatePreset?) {
        val role = _uiState.value.userRole
        if (!SecurityManager.canAccessImportVault(role)) return
        viewModelScope.launch {
            _uiState.update { it.copy(exportPreviewPackage = null) }
            try {
                val source = repository.loadExportSource(role)
                val req = buildEffectiveExportRequest(config, datePresetOverride)
                    .copy(businessProfile = source.businessProfile)
                val pkg = withContext(Dispatchers.Default) {
                    ImportVaultExportBuilder.buildPackage(source.dataset, req)
                }
                _uiState.update { it.copy(exportPreviewPackage = pkg) }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiState.update { it.copy(actionMessage = e.message ?: "Export preview failed") }
            }
        }
    }

    fun generateExportFile(
        config: VaultExportSheetLaunchConfig,
        format: VaultExportFormat,
        datePresetOverride: VaultDatePreset?,
        saveAsImmutableSnapshot: Boolean,
        onGenerated: (File, VaultExportPackage) -> Unit
    ) {
        val role = _uiState.value.userRole
        if (!SecurityManager.canAccessImportVault(role)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val source = repository.loadExportSource(role)
                val req = buildEffectiveExportRequest(config, datePresetOverride)
                    .copy(businessProfile = source.businessProfile)
                val (file, pkg) = withContext(Dispatchers.Default) {
                    val builtPkg = ImportVaultExportBuilder.buildPackage(source.dataset, req)
                    val outFile = withContext(Dispatchers.IO) {
                        when (format) {
                            VaultExportFormat.PDF -> ImportVaultPdfGenerator.generatePdfFile(context, builtPkg, role)
                            VaultExportFormat.XLSX -> ImportVaultExcelGenerator.exportToXlsx(context, builtPkg, role)
                        }
                    }
                    outFile to builtPkg
                }

                if (saveAsImmutableSnapshot) {
                    val targetScopeAndId: Pair<String, String> = when (config.scope) {
                        VaultExportScope.SHIPMENT -> VaultScopeType.SHIPMENT.name to (config.scopeId ?: "portfolio")
                        VaultExportScope.PURCHASE_ORDER -> VaultScopeType.PURCHASE_ORDER.name to (config.scopeId ?: "portfolio")
                        VaultExportScope.PRODUCT, VaultExportScope.VARIANT -> {
                            val pType = config.parentScopeType?.name ?: "PORTFOLIO"
                            val pId = config.parentScopeId ?: "portfolio"
                            pType to pId
                        }
                        VaultExportScope.PORTFOLIO -> "PORTFOLIO" to "portfolio"
                    }
                    val snapMime = if (format == VaultExportFormat.PDF) {
                        "application/pdf"
                    } else {
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    }
                    val stored = ImportVaultAttachmentStorage.saveImmutableSnapshot(
                        context = context,
                        sourceFile = file,
                        preferredName = file.name,
                        scopePrefix = targetScopeAndId.second,
                        mimeType = snapMime,
                        role = role
                    )
                    repository.addAttachment(
                        role = role,
                        scopeType = targetScopeAndId.first,
                        sourceId = targetScopeAndId.second,
                        category = VaultDocumentCategory.REPORTS.name,
                        title = "${if (pkg.isBangla) pkg.titleBn else pkg.titleEn} (${format.name})",
                        filePath = stored.absolutePath,
                        mimeType = stored.mimeType,
                        fileSizeBytes = stored.fileSizeBytes,
                        isImmutableSnapshot = true,
                        createdBy = role.code
                    )
                    refreshAll()
                }
                _uiState.update { it.copy(isExporting = false) }
                onGenerated(file, pkg)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        actionMessage = e.message ?: "Export generation failed"
                    )
                }
            }
        }
    }

    fun attachLocalDocument(
        scopeType: VaultScopeType,
        sourceId: String,
        sourceUri: Uri,
        category: VaultDocumentCategory,
        title: String
    ) = attachLocalDocumentForScope(
        scopeType = scopeType.name,
        sourceId = sourceId,
        sourceUri = sourceUri,
        category = category,
        title = title
    )

    fun attachLocalDocumentForScope(
        scopeType: String,
        sourceId: String,
        sourceUri: Uri,
        category: VaultDocumentCategory,
        title: String
    ) {
        val role = _uiState.value.userRole
        if (!SecurityManager.canAccessImportVault(role)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAttachment = true) }
            try {
                val stored = ImportVaultAttachmentStorage.saveFromUri(
                    context = context,
                    sourceUri = sourceUri,
                    preferredName = title,
                    scopePrefix = sourceId,
                    role = role
                )
                repository.addAttachment(
                    role = role,
                    scopeType = scopeType,
                    sourceId = sourceId,
                    category = category.name,
                    title = title,
                    filePath = stored.absolutePath,
                    mimeType = stored.mimeType,
                    fileSizeBytes = stored.fileSizeBytes,
                    isImmutableSnapshot = false,
                    createdBy = role.code
                )
                val msg = if (_uiState.value.language == AppLanguage.BANGLA) {
                    "অ্যাটাচমেন্ট সংরক্ষিত হয়েছে"
                } else {
                    "Attachment saved to Vault"
                }
                _uiState.update { it.copy(isUploadingAttachment = false, actionMessage = msg) }
                refreshAll()
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUploadingAttachment = false,
                        actionMessage = e.message ?: "Attachment save failed"
                    )
                }
            }
        }
    }

    fun deleteLocalVaultAttachment(attachmentId: String) {
        val role = _uiState.value.userRole
        if (!SecurityManager.canAccessImportVault(role)) return
        viewModelScope.launch {
            try {
                repository.deleteAttachment(role = role, attachmentId = attachmentId)
                val msg = if (_uiState.value.language == AppLanguage.BANGLA) {
                    "শুধু ভল্ট অ্যাটাচমেন্ট মুছে ফেলা হয়েছে"
                } else {
                    "Deleted Vault attachment only"
                }
                _uiState.update { it.copy(actionMessage = msg) }
                refreshAll()
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiState.update { it.copy(actionMessage = e.message ?: "Delete failed") }
            }
        }
    }
}
