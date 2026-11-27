package com.pocketdev.advance_erp.feature.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.communication.*
import com.pocketdev.advance_erp.data.entity.*
import com.pocketdev.advance_erp.domain.repository.CommunicationRepository
import com.pocketdev.advance_erp.domain.repository.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class CommunicationUiState(
    val busy: Boolean = false,
    val error: String? = null,
    val prepared: PreparedCommunication? = null,
    val activeRequest: CommunicationRequest? = null,
    val message: String = "",
    val loadedMessageBaseline: String = "",
    val isMessageEdited: Boolean = false,
    val phone: String = "",
    val isPhoneOverridden: Boolean = false,
    val application: String = "ASK",
    val settings: CommunicationSettings = CommunicationSettings(),
    val visibleModules: List<CommunicationModule> = CommunicationModule.entries,
    val choices: List<CommunicationChoice> = emptyList(),
    val module: CommunicationModule = CommunicationModule.INVOICE,
    val editor: String = "",
    val loadedEditorBaseline: String = "",
    val isTemplateEdited: Boolean = false,
    val isCustomTemplateLoaded: Boolean = false,
    val editorLanguage: String = "bn",
    val editorStatus: CommunicationStatus = CommunicationStatus.GENERAL,
    val history: List<CommunicationHistory> = emptyList(),
    val notice: String? = null,
    val selectedFormats: Set<ShareOutputFormat> = setOf(
        ShareOutputFormat.PDF,
        ShareOutputFormat.IMAGE_CARD,
        ShareOutputFormat.TEXT_MESSAGE
    ),
    val activePreviewTab: SharePreviewTab = SharePreviewTab.PDF,
    val generatingFormat: ShareOutputFormat? = null,
    val plannedSteps: List<PlannedShareStep> = emptyList(),
    val activeStepIndex: Int = 0,
    val completedStepIndices: Set<Int> = emptySet(),
    val lastSavedPath: String? = null,
    val cacheStats: CommunicationCacheStats = CommunicationCacheStats()
)

@HiltViewModel
class CommunicationViewModel @Inject constructor(
    private val repository: CommunicationRepository,
    private val salesRepository: SalesRepository
) : ViewModel() {

    private val mutable = MutableStateFlow(CommunicationUiState())
    val state = mutable.asStateFlow()
    private var preparation: Job? = null

    init {
        viewModelScope.launch {
            val modules = runCatching { repository.getVisibleModules() }.getOrDefault(emptyList())
            mutable.update { current ->
                val activeModule = if (current.module in modules || modules.isEmpty()) current.module else modules.first()
                current.copy(visibleModules = modules, module = activeModule)
            }
        }
        viewModelScope.launch {
            repository.observeSettings().collect { s ->
                val resolved = s ?: CommunicationSettings()
                mutable.update { current ->
                    val app = if (current.application == "ASK") resolved.preferredApp else current.application
                    val syncedFormats = if (current.prepared == null && current.activeRequest == null) {
                        resolved.defaultSelectedFormats()
                    } else {
                        current.selectedFormats
                    }
                    current.copy(
                        settings = resolved,
                        application = app,
                        selectedFormats = syncedFormats
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.observeAuthorizedHistory().collect { h ->
                mutable.update { it.copy(history = h) }
            }
        }
        refreshCacheStats()
    }

    fun refreshCacheStats() {
        viewModelScope.launch {
            val stats = runCatching { repository.getAttachmentCacheStats() }.getOrDefault(CommunicationCacheStats())
            mutable.update { it.copy(cacheStats = stats) }
        }
    }

    fun clearAttachmentCache() {
        viewModelScope.launch {
            runCatching { repository.clearAttachmentCache() }
                .onSuccess { deleted ->
                    val bn = (state.value.prepared?.language ?: state.value.settings.defaultLanguage) == "bn"
                    mutable.update {
                        it.copy(
                            cacheStats = CommunicationCacheStats(),
                            notice = if (bn) {
                                "অস্থায়ী এক্সপোর্ট ক্যাশ পরিষ্কার হয়েছে ($deleted টি ফাইল)।"
                            } else {
                                "Temporary export cache cleared ($deleted file(s))."
                            }
                        )
                    }
                }
                .onFailure { e ->
                    mutable.update { it.copy(error = e.message ?: "Failed to clear cache") }
                }
        }
    }

    fun clearShareHistory() {
        viewModelScope.launch {
            runCatching { repository.clearHistory() }
                .onSuccess {
                    val bn = (state.value.prepared?.language ?: state.value.settings.defaultLanguage) == "bn"
                    mutable.update {
                        it.copy(
                            notice = if (bn) "শেয়ার ইতিহাস মুছে ফেলা হয়েছে।" else "Share history cleared."
                        )
                    }
                }
                .onFailure { e ->
                    mutable.update { it.copy(error = e.message ?: "Failed to clear history") }
                }
        }
    }

    fun reset() {
        preparation?.cancel()
        mutable.update {
            it.copy(
                busy = false,
                prepared = null,
                activeRequest = null,
                error = null,
                notice = null,
                message = "",
                loadedMessageBaseline = "",
                isMessageEdited = false,
                phone = "",
                isPhoneOverridden = false,
                selectedFormats = it.settings.defaultSelectedFormats(),
                activePreviewTab = ShareWorkflowPlanner.resolveInitialPreviewTab(it.settings.defaultSelectedFormats()),
                generatingFormat = null,
                plannedSteps = emptyList(),
                activeStepIndex = 0,
                completedStepIndices = emptySet(),
                lastSavedPath = null
            )
        }
    }

    fun selectModule(module: CommunicationModule) {
        preparation?.cancel()
        preparation = viewModelScope.launch {
            mutable.update { it.copy(module = module, busy = true, choices = emptyList(), error = null, notice = null) }
            try {
                val choices = repository.getChoices(module)
                mutable.update { it.copy(choices = choices, busy = false) }
            } catch (e: Exception) {
                failure(e)
            }
        }
    }

    fun prepare(
        request: CommunicationRequest,
        language: String? = null,
        status: CommunicationStatus? = null,
        forcePdf: Boolean = false,
        useSystemDefaultTemplate: Boolean = false,
        forceImage: Boolean = false
    ) {
        preparation?.cancel()
        val sameDocument = state.value.activeRequest == request
        val keepOverriddenPhone = sameDocument && state.value.isPhoneOverridden
        val prevPhone = state.value.phone
        val prevFormats = state.value.selectedFormats
        preparation = viewModelScope.launch {
            // Always clear stale prepared document before preparing so errors never leave a previous doc shareable
            mutable.update {
                it.copy(
                    busy = true,
                    error = null,
                    notice = null,
                    prepared = null,
                    activeRequest = request,
                    plannedSteps = emptyList(),
                    activeStepIndex = 0,
                    completedStepIndices = emptySet(),
                    lastSavedPath = null,
                    generatingFormat = null
                )
            }
            try {
                val latestSettings = repository.getSettings()
                val baseFormats = if (sameDocument) prevFormats else latestSettings.defaultSelectedFormats()
                val targetFormats = buildSet {
                    addAll(baseFormats)
                    if (forcePdf) add(ShareOutputFormat.PDF)
                    if (forceImage) add(ShareOutputFormat.IMAGE_CARD)
                }
                val p = repository.prepare(
                    request = request,
                    languageOverride = language,
                    statusOverride = status,
                    forcePdf = forcePdf || (ShareOutputFormat.PDF in targetFormats),
                    useSystemDefaultTemplate = useSystemDefaultTemplate,
                    forceImage = forceImage || (ShareOutputFormat.IMAGE_CARD in targetFormats)
                )
                val preparedWithSelection = p.copy(selectedFormats = targetFormats)
                mutable.update {
                    val effectiveApp = if (it.application == "ASK") latestSettings.preferredApp else it.application
                    val nextTab = if (sameDocument) it.activePreviewTab else ShareWorkflowPlanner.resolveInitialPreviewTab(targetFormats)
                    it.copy(
                        settings = latestSettings,
                        prepared = preparedWithSelection,
                        module = request.module,
                        message = preparedWithSelection.message,
                        loadedMessageBaseline = preparedWithSelection.message,
                        isMessageEdited = false,
                        phone = if (keepOverriddenPhone) prevPhone else preparedWithSelection.document.phone,
                        isPhoneOverridden = keepOverriddenPhone,
                        application = effectiveApp,
                        selectedFormats = targetFormats,
                        activePreviewTab = nextTab,
                        busy = false
                    )
                }
                refreshCacheStats()
            } catch (e: Exception) {
                failure(e)
            }
        }
    }

    fun toggleShareFormat(format: ShareOutputFormat) {
        val outcome = ShareWorkflowPlanner.toggleFormatSelection(state.value.selectedFormats, format)
        val bn = (state.value.prepared?.language ?: state.value.settings.defaultLanguage) == "bn"
        if (outcome.blockedLastUncheck) {
            notice(
                if (bn) "কমপক্ষে একটি শেয়ার ফরম্যাট নির্বাচন করুন"
                else "Select at least one share format"
            )
            return
        }
        mutable.update { current ->
            current.copy(
                selectedFormats = outcome.selectedFormats,
                prepared = current.prepared?.copy(selectedFormats = outcome.selectedFormats),
                plannedSteps = emptyList(),
                activeStepIndex = 0,
                completedStepIndices = emptySet(),
                error = null,
                notice = null
            )
        }
        val p = state.value.prepared ?: return
        if (format in outcome.selectedFormats) {
            when (format) {
                ShareOutputFormat.PDF -> if (p.pdfFile == null && p.attachment?.extension != "xlsx") {
                    ensurePdfGenerated()
                }
                ShareOutputFormat.IMAGE_CARD -> if (p.imageCardPages.isEmpty()) {
                    ensureImageCardsGenerated()
                }
                ShareOutputFormat.TEXT_MESSAGE -> Unit
            }
        }
    }

    fun selectPreviewTab(tab: SharePreviewTab) {
        mutable.update { it.copy(activePreviewTab = tab, error = null) }
        val p = state.value.prepared ?: return
        when (tab) {
            SharePreviewTab.PDF -> if (p.pdfFile == null && p.attachment?.extension != "xlsx") {
                ensurePdfGenerated()
            }
            SharePreviewTab.IMAGE -> if (p.imageCardPages.isEmpty()) {
                ensureImageCardsGenerated()
            }
            SharePreviewTab.TEXT -> Unit
        }
    }

    fun ensurePdfGenerated(onReady: ((File) -> Unit)? = null) {
        val p = state.value.prepared ?: return
        viewModelScope.launch {
            mutable.update { it.copy(generatingFormat = ShareOutputFormat.PDF, error = null) }
            try {
                val file = repository.generatePdf(p.document, p.language)
                mutable.update { cur ->
                    cur.copy(
                        generatingFormat = null,
                        prepared = cur.prepared?.copy(
                            pdfFile = file,
                            attachment = file,
                            attachmentError = null
                        )
                    )
                }
                onReady?.invoke(file)
                refreshCacheStats()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.update { cur ->
                    cur.copy(
                        generatingFormat = null,
                        prepared = cur.prepared?.copy(attachmentError = e.message ?: "PDF generation failed"),
                        error = e.message ?: "PDF generation failed"
                    )
                }
            }
        }
    }

    fun ensureImageCardsGenerated(onReady: ((List<File>) -> Unit)? = null) {
        val p = state.value.prepared ?: return
        viewModelScope.launch {
            mutable.update { it.copy(generatingFormat = ShareOutputFormat.IMAGE_CARD, error = null) }
            try {
                val pages = repository.generateImageCards(p.document, p.language)
                mutable.update { cur ->
                    cur.copy(
                        generatingFormat = null,
                        prepared = cur.prepared?.copy(
                            imageCardFile = pages.firstOrNull(),
                            imageCardPages = pages,
                            imageError = null
                        )
                    )
                }
                onReady?.invoke(pages)
                refreshCacheStats()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.update { cur ->
                    cur.copy(
                        generatingFormat = null,
                        prepared = cur.prepared?.copy(imageError = e.message ?: "Image card generation failed"),
                        error = e.message ?: "Image card generation failed"
                    )
                }
            }
        }
    }

    fun activatePdfAttachment() {
        val p = state.value.prepared ?: return
        val existingPdf = p.pdfFile
        if (existingPdf != null && existingPdf.isFile && existingPdf.length() > 0L) {
            mutable.update { cur ->
                cur.copy(prepared = cur.prepared?.copy(attachment = existingPdf, attachmentError = null))
            }
        } else {
            ensurePdfGenerated()
        }
    }

    fun generateXlsx() {
        val p = state.value.prepared ?: return
        viewModelScope.launch {
            mutable.update { it.copy(busy = true, generatingFormat = ShareOutputFormat.PDF, error = null) }
            try {
                val file = repository.generateXlsx(p.document)
                mutable.update { cur ->
                    cur.copy(
                        busy = false,
                        generatingFormat = null,
                        prepared = cur.prepared?.copy(attachment = file, attachmentError = null)
                    )
                }
                refreshCacheStats()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.update { it.copy(busy = false, generatingFormat = null, error = e.message ?: "XLSX generation failed") }
            }
        }
    }

    fun savePdfOrXlsxLocally(preferredFile: File? = null, onSaved: ((String) -> Unit)? = null) {
        val p = state.value.prepared ?: return
        viewModelScope.launch {
            mutable.update { it.copy(generatingFormat = ShareOutputFormat.PDF, error = null) }
            try {
                val file = preferredFile ?: p.attachment ?: p.pdfFile ?: repository.generatePdf(p.document, p.language)
                if (p.pdfFile == null && file.extension.equals("pdf", ignoreCase = true)) {
                    mutable.update { cur ->
                        cur.copy(prepared = cur.prepared?.copy(pdfFile = file, attachment = file, attachmentError = null))
                    }
                }
                val savedPath = repository.saveAttachmentToDownloads(file)
                val bn = p.language == "bn"
                mutable.update {
                    it.copy(
                        generatingFormat = null,
                        lastSavedPath = savedPath,
                        notice = if (bn) "ফাইল সেভ হয়েছে: $savedPath" else "Saved to Downloads: $savedPath"
                    )
                }
                val fmtToken = if (file.extension.equals("xlsx", ignoreCase = true)) "XLSX" else "PDF"
                record(
                    event = "SAVED_LOCALLY",
                    application = "LOCAL_STORAGE",
                    attachment = true,
                    sharedFormatsOverride = fmtToken,
                    includePdfRef = true,
                    includeImageRef = false,
                    savedPath = savedPath
                )
                onSaved?.invoke(savedPath)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.update { it.copy(generatingFormat = null, error = e.message ?: "Save failed") }
            }
        }
    }

    fun saveImageCardsLocally(onSaved: ((List<String>) -> Unit)? = null) {
        val p = state.value.prepared ?: return
        viewModelScope.launch {
            mutable.update { it.copy(generatingFormat = ShareOutputFormat.IMAGE_CARD, error = null) }
            try {
                val pages = if (p.imageCardPages.isNotEmpty()) {
                    p.imageCardPages
                } else {
                    repository.generateImageCards(p.document, p.language).also { fresh ->
                        mutable.update { cur ->
                            cur.copy(
                                prepared = cur.prepared?.copy(
                                    imageCardFile = fresh.firstOrNull(),
                                    imageCardPages = fresh,
                                    imageError = null
                                )
                            )
                        }
                    }
                }
                val savedPaths = repository.saveImagesToGallery(pages)
                val summary = savedPaths.joinToString(", ").ifBlank { null }
                val bn = p.language == "bn"
                mutable.update {
                    it.copy(
                        generatingFormat = null,
                        lastSavedPath = summary,
                        notice = if (bn) {
                            "গ্যালারিতে সংরক্ষিত হয়েছে (${savedPaths.size}টি): ${summary.orEmpty()}"
                        } else {
                            "Saved ${savedPaths.size} image(s) to Gallery: ${summary.orEmpty()}"
                        }
                    )
                }
                record(
                    event = "SAVED_LOCALLY",
                    application = "LOCAL_STORAGE",
                    attachment = false,
                    sharedFormatsOverride = ShareOutputFormat.IMAGE_CARD.name,
                    includePdfRef = false,
                    includeImageRef = true,
                    savedPath = summary
                )
                onSaved?.invoke(savedPaths)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.update { it.copy(generatingFormat = null, error = e.message ?: "Gallery save failed") }
            }
        }
    }

    fun updateShareWorkflow(steps: List<PlannedShareStep>, completedIndex: Int?) {
        mutable.update { current ->
            val updatedCompleted = if (completedIndex != null) {
                current.completedStepIndices + completedIndex
            } else {
                current.completedStepIndices
            }
            val nextIdx = (0 until steps.size).firstOrNull { it !in updatedCompleted } ?: (steps.size - 1).coerceAtLeast(0)
            current.copy(
                plannedSteps = steps,
                completedStepIndices = updatedCompleted,
                activeStepIndex = nextIdx
            )
        }
    }

    fun clearShareWorkflow() {
        mutable.update {
            it.copy(
                plannedSteps = emptyList(),
                activeStepIndex = 0,
                completedStepIndices = emptySet()
            )
        }
    }

    fun updateQuotationValidity(validUntilMillis: Long) = action {
        val p = state.value.prepared ?: return@action
        mutable.update { it.copy(busy = true, error = null) }
        salesRepository.updateDocumentCommunicationMetadata(
            invoiceId = p.document.request.documentId,
            validUntilDate = validUntilMillis,
            scheduledDeliveryInfo = p.document.scheduledDeliveryInfo
        )
        prepare(p.document.request, p.language)
    }

    fun updateSalesOrderDeliveryInfo(deliveryInfo: String) = action {
        val p = state.value.prepared ?: return@action
        mutable.update { it.copy(busy = true, error = null) }
        salesRepository.updateDocumentCommunicationMetadata(
            invoiceId = p.document.request.documentId,
            validUntilDate = p.document.validUntilMillis,
            scheduledDeliveryInfo = deliveryInfo.trim().ifBlank { null }
        )
        prepare(p.document.request, p.language)
    }

    fun message(value: String) {
        mutable.update {
            it.copy(
                message = value,
                isMessageEdited = value != it.loadedMessageBaseline
            )
        }
    }

    fun phone(value: String, manualOverride: Boolean = true) {
        mutable.update { it.copy(phone = value, isPhoneOverridden = manualOverride) }
    }

    fun application(value: String) {
        mutable.update { it.copy(application = value, error = null) }
    }

    fun notice(value: String?) {
        mutable.update { it.copy(notice = value) }
    }

    fun error(value: String?) {
        mutable.update { it.copy(error = value) }
    }

    fun saveSettings(value: CommunicationSettings) = action {
        repository.saveSettings(value)
        mutable.update { it.copy(settings = value, application = value.preferredApp) }
    }

    fun savePartyLanguage(language: String) = action {
        val d = state.value.prepared?.document ?: return@action
        repository.savePartyLanguage(d.partyId, d.supplier, language)
        notice(if (language == "bn") "গ্রাহকের ভাষা সংরক্ষিত হয়েছে" else "Recipient language saved")
    }

    fun loadTemplate(module: CommunicationModule, status: CommunicationStatus, language: String) = action {
        val text = repository.loadTemplate(module, status, language)
        val hasCustom = repository.hasCustomTemplate(module, status, language)
        mutable.update {
            it.copy(
                module = module,
                editor = text,
                loadedEditorBaseline = text,
                isTemplateEdited = false,
                isCustomTemplateLoaded = hasCustom,
                editorStatus = status,
                editorLanguage = language,
                error = null
            )
        }
    }

    fun editTemplate(text: String) {
        mutable.update {
            it.copy(
                editor = text,
                isTemplateEdited = text != it.loadedEditorBaseline,
                error = null
            )
        }
    }

    fun saveTemplate() = action {
        val s = state.value
        repository.saveCustomTemplate(s.module, s.editorStatus, s.editorLanguage, s.editor)
        mutable.update {
            it.copy(
                loadedEditorBaseline = s.editor,
                isTemplateEdited = false,
                isCustomTemplateLoaded = true,
                notice = if (s.editorLanguage == "bn") "কাস্টম টেমপ্লেট সংরক্ষিত হয়েছে" else "Custom template saved"
            )
        }
    }

    fun resetTemplate() = action {
        val s = state.value
        val resetText = repository.resetCustomTemplate(s.module, s.editorStatus, s.editorLanguage)
        mutable.update {
            it.copy(
                editor = resetText,
                loadedEditorBaseline = resetText,
                isTemplateEdited = false,
                isCustomTemplateLoaded = false,
                notice = if (s.editorLanguage == "bn") "সিস্টেম ডিফল্ট টেমপ্লেট পুনরুদ্ধার হয়েছে" else "Reset to system default template"
            )
        }
    }

    fun record(
        event: String,
        application: String,
        attachment: Boolean,
        message: String = state.value.message,
        phone: String = state.value.phone,
        sharedFormatsOverride: String? = null,
        includePdfRef: Boolean = attachment,
        includeImageRef: Boolean = false,
        savedPath: String? = state.value.lastSavedPath
    ) {
        val cur = state.value
        val p = cur.prepared ?: return
        val d = p.document
        val docExt = (p.attachment ?: p.pdfFile)?.extension ?: "pdf"
        val resolvedFormats = sharedFormatsOverride ?: ShareWorkflowPlanner.formatSharedFormatsCsv(
            selectedFormats = cur.selectedFormats,
            documentExtension = docExt
        ).ifBlank { "TEXT" }
        val resolvedPdfName = if (includePdfRef) (p.attachment ?: p.pdfFile)?.name else null
        val resolvedImageName = if (includeImageRef) p.imageCardFile?.name else null

        viewModelScope.launch {
            try {
                repository.recordHandoffEvent(
                    CommunicationHistory(
                        id = UUID.randomUUID().toString(),
                        module = d.request.module.name,
                        documentId = d.request.documentId,
                        documentNumber = d.number,
                        partyId = d.partyId,
                        partyName = d.partyName,
                        phone = phone,
                        createdAt = System.currentTimeMillis(),
                        message = message,
                        attachmentReference = resolvedPdfName,
                        language = p.language,
                        application = application,
                        event = event,
                        rangeFrom = d.request.from,
                        rangeTo = d.request.to,
                        sharedFormats = resolvedFormats,
                        imageReference = resolvedImageName,
                        localSavedPath = savedPath
                    )
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.update {
                    it.copy(notice = "Share opened, but local history write failed: ${e.message}")
                }
            }
        }
    }

    private fun action(block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
        } catch (e: Exception) {
            failure(e)
        }
    }

    private fun failure(e: Exception) {
        if (e is CancellationException) throw e
        mutable.update {
            it.copy(
                busy = false,
                generatingFormat = null,
                prepared = null,
                error = e.message ?: "Operation failed"
            )
        }
    }
}
