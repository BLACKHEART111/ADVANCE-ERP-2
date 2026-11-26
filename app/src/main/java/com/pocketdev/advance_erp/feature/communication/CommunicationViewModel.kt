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
    val notice: String? = null
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
                    current.copy(settings = resolved, application = app)
                }
            }
        }
        viewModelScope.launch {
            repository.observeAuthorizedHistory().collect { h ->
                mutable.update { it.copy(history = h) }
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
                isPhoneOverridden = false
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
        useSystemDefaultTemplate: Boolean = false
    ) {
        preparation?.cancel()
        val sameDocument = state.value.activeRequest == request
        val keepOverriddenPhone = sameDocument && state.value.isPhoneOverridden
        val prevPhone = state.value.phone
        preparation = viewModelScope.launch {
            // Always clear stale prepared document before preparing so errors never leave a previous doc shareable
            mutable.update {
                it.copy(
                    busy = true,
                    error = null,
                    notice = null,
                    prepared = null,
                    activeRequest = request
                )
            }
            try {
                val p = repository.prepare(
                    request = request,
                    languageOverride = language,
                    statusOverride = status,
                    forcePdf = forcePdf,
                    useSystemDefaultTemplate = useSystemDefaultTemplate
                )
                mutable.update {
                    val effectiveApp = if (it.application == "ASK") it.settings.preferredApp else it.application
                    it.copy(
                        prepared = p,
                        module = request.module,
                        message = p.message,
                        loadedMessageBaseline = p.message,
                        isMessageEdited = false,
                        phone = if (keepOverriddenPhone) prevPhone else p.document.phone,
                        isPhoneOverridden = keepOverriddenPhone,
                        application = effectiveApp,
                        busy = false
                    )
                }
            } catch (e: Exception) {
                failure(e)
            }
        }
    }

    fun generateXlsx() = action {
        val p = state.value.prepared ?: return@action
        mutable.update { it.copy(busy = true, error = null) }
        val file = repository.generateXlsx(p.document)
        mutable.update {
            it.copy(
                busy = false,
                prepared = p.copy(attachment = file, attachmentError = null)
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
        phone: String = state.value.phone
    ) {
        val p = state.value.prepared ?: return
        val d = p.document
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
                        attachmentReference = if (attachment) p.attachment?.name else null,
                        language = p.language,
                        application = application,
                        event = event,
                        rangeFrom = d.request.from,
                        rangeTo = d.request.to
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
                prepared = null,
                error = e.message ?: "Operation failed"
            )
        }
    }
}
