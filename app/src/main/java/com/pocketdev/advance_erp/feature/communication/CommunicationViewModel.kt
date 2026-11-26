package com.pocketdev.advance_erp.feature.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.communication.*
import com.pocketdev.advance_erp.data.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject

data class CommunicationUiState(
    val busy: Boolean = false, val error: String? = null, val prepared: PreparedCommunication? = null,
    val message: String = "", val phone: String = "", val application: String = "ASK",
    val settings: CommunicationSettings = CommunicationSettings(),
    val choices: List<CommunicationChoice> = emptyList(), val module: CommunicationModule = CommunicationModule.INVOICE,
    val editor: String = "", val editorLanguage: String = "bn", val editorStatus: CommunicationStatus = CommunicationStatus.GENERAL,
    val history: List<CommunicationHistory> = emptyList(), val notice: String? = null
)
@HiltViewModel
class CommunicationViewModel @Inject constructor(val engine: CommunicationEngine) : ViewModel() {
    private val mutable = MutableStateFlow(CommunicationUiState())
    val state = mutable.asStateFlow()
    private var preparation: Job? = null
    init {
        viewModelScope.launch { engine.dao.observeSettings().collect { s -> mutable.update { it.copy(settings = s ?: CommunicationSettings()) } } }
        viewModelScope.launch { engine.dao.history().collect { h -> mutable.update { it.copy(history = h) } } }
    }
    fun reset() { preparation?.cancel(); mutable.update { it.copy(prepared = null, error = null, notice = null, message = "") } }
    fun selectModule(module: CommunicationModule) {
        preparation?.cancel()
        preparation = viewModelScope.launch {
            mutable.update { it.copy(module = module, busy = true, choices = emptyList(), error = null) }
            try { val choices = engine.choices(module); mutable.update { it.copy(choices = choices, busy = false) } }
            catch(e: Exception) { failure(e) }
        }
    }
    fun prepare(request: CommunicationRequest, language: String? = null, status: CommunicationStatus? = null, forcePdf: Boolean = false) {
        preparation?.cancel()
        preparation = viewModelScope.launch {
            mutable.update { it.copy(busy = true, error = null) }
            try {
                val p = engine.prepare(request, language, status, forcePdf)
                mutable.update { it.copy(prepared = p, message = p.message, phone = p.document.phone,
                    application = it.settings.preferredApp, busy = false) }
            } catch(e: Exception) { failure(e) }
        }
    }
    fun generateXlsx() = action {
        val p = state.value.prepared ?: return@action
        mutable.update { it.copy(busy = true, error = null) }
        val file = engine.xlsx(p.document)
        mutable.update { it.copy(busy = false, prepared = p.copy(attachment = file, attachmentError = null)) }
    }
    fun message(value: String) { mutable.update { it.copy(message = value) } }
    fun phone(value: String) { mutable.update { it.copy(phone = value) } }
    fun application(value: String) { mutable.update { it.copy(application = value) } }
    fun notice(value: String) { mutable.update { it.copy(notice = value) } }
    fun error(value: String) { mutable.update { it.copy(error = value) } }
    fun saveSettings(value: CommunicationSettings) = action { engine.dao.saveSettings(value) }
    fun savePartyLanguage(language: String) = action {
        val d = state.value.prepared?.document ?: return@action
        engine.dao.savePreference(CommunicationPartyPreference((if(d.supplier) "supplier:" else "customer:") + d.partyId, language))
        notice("Language saved / ভাষা সংরক্ষিত")
    }
    fun loadTemplate(module: CommunicationModule, status: CommunicationStatus, language: String) = action {
        val text = engine.dao.template(module.name, status.name, language)?.body ?: DynamicTemplateEngine.defaultTemplate(module, status, language)
        mutable.update { it.copy(module = module, editor = text, editorStatus = status, editorLanguage = language) }
    }
    fun editTemplate(text: String) { mutable.update { it.copy(editor = text) } }
    fun saveTemplate() = action {
        val s = state.value; DynamicTemplateEngine.validate(s.editor)
        val used = Regex("\\{([^{}]+)}").findAll(s.editor).map { it.groupValues[1] }.toSet()
        require((used - DynamicTemplateEngine.allowed(s.module)).isEmpty()) { "Placeholder does not belong to this module" }
        engine.dao.saveTemplate(CommunicationTemplate(s.module.name, s.editorStatus.name, s.editorLanguage, s.editor)); notice("Template saved / টেমপ্লেট সংরক্ষিত")
    }
    fun resetTemplate() = action {
        val s = state.value; engine.dao.resetTemplate(s.module.name, s.editorStatus.name, s.editorLanguage)
        mutable.update { it.copy(editor = DynamicTemplateEngine.defaultTemplate(s.module, s.editorStatus, s.editorLanguage)) }
    }
    fun record(event: String, application: String, attachment: Boolean, message: String = state.value.message, phone: String = state.value.phone) {
        val p = state.value.prepared ?: return
        val d = p.document
        action {
            engine.dao.record(CommunicationHistory(UUID.randomUUID().toString(), d.request.module.name, d.request.documentId,
                d.number, d.partyId, d.partyName, phone, System.currentTimeMillis(), message,
                if(attachment) p.attachment?.name else null, p.language, application, event, d.request.from, d.request.to))
        }
    }
    private fun action(block: suspend () -> Unit) = viewModelScope.launch { try { block() } catch(e: Exception) { failure(e) } }
    private fun failure(e: Exception) { if(e is CancellationException) throw e; mutable.update { it.copy(busy = false, error = e.message ?: "Operation failed") } }
}
