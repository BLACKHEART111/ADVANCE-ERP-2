package com.pocketdev.advance_erp.data.repository

import com.pocketdev.advance_erp.core.communication.CommunicationChoice
import com.pocketdev.advance_erp.core.communication.CommunicationEngine
import com.pocketdev.advance_erp.core.communication.CommunicationModule
import com.pocketdev.advance_erp.core.communication.CommunicationRequest
import com.pocketdev.advance_erp.core.communication.CommunicationStatus
import com.pocketdev.advance_erp.core.communication.DynamicTemplateEngine
import com.pocketdev.advance_erp.core.communication.PreparedCommunication
import com.pocketdev.advance_erp.core.communication.SafeDocument
import com.pocketdev.advance_erp.data.dao.CommunicationDao
import com.pocketdev.advance_erp.data.entity.CommunicationHistory
import com.pocketdev.advance_erp.data.entity.CommunicationPartyPreference
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import com.pocketdev.advance_erp.data.entity.CommunicationTemplate
import com.pocketdev.advance_erp.domain.repository.CommunicationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunicationRepositoryImpl @Inject constructor(
    private val engine: CommunicationEngine,
    private val dao: CommunicationDao
) : CommunicationRepository {

    override fun observeSettings(): Flow<CommunicationSettings?> = dao.observeSettings()

    override fun observeAuthorizedHistory(): Flow<List<CommunicationHistory>> =
        dao.history().map { rows -> engine.filterAuthorizedHistory(rows) }

    override suspend fun getSettings(): CommunicationSettings = engine.settings()

    override suspend fun saveSettings(settings: CommunicationSettings) {
        engine.requireReportAccess()
        dao.saveSettings(settings)
    }

    override suspend fun getVisibleModules(): List<CommunicationModule> = engine.visibleModules()

    override suspend fun getChoices(module: CommunicationModule): List<CommunicationChoice> = engine.choices(module)

    override suspend fun prepare(
        request: CommunicationRequest,
        languageOverride: String?,
        statusOverride: CommunicationStatus?,
        forcePdf: Boolean,
        useSystemDefaultTemplate: Boolean
    ): PreparedCommunication = engine.prepare(
        request = request,
        languageOverride = languageOverride,
        statusOverride = statusOverride,
        forcePdf = forcePdf,
        useSystemDefaultTemplate = useSystemDefaultTemplate
    )

    override suspend fun generateXlsx(document: SafeDocument): File = engine.xlsx(document)

    override suspend fun savePartyLanguage(partyId: String, isSupplier: Boolean, language: String) {
        engine.requireReportAccess()
        val key = (if (isSupplier) "supplier:" else "customer:") + partyId
        dao.savePreference(CommunicationPartyPreference(key, language))
    }

    override suspend fun loadTemplate(
        module: CommunicationModule,
        status: CommunicationStatus,
        language: String
    ): String {
        engine.checkAccess(module)
        return dao.template(module.name, status.name, language)?.body
            ?: DynamicTemplateEngine.defaultTemplate(module, status, language)
    }

    override suspend fun hasCustomTemplate(
        module: CommunicationModule,
        status: CommunicationStatus,
        language: String
    ): Boolean {
        engine.checkAccess(module)
        return dao.template(module.name, status.name, language) != null
    }

    override suspend fun saveCustomTemplate(
        module: CommunicationModule,
        status: CommunicationStatus,
        language: String,
        body: String
    ) {
        engine.checkAccess(module)
        DynamicTemplateEngine.validate(body)
        val used = Regex("\\{([^{}]+)\\}").findAll(body).map { it.groupValues[1] }.toSet()
        require((used - DynamicTemplateEngine.allowed(module)).isEmpty()) {
            "Placeholder does not belong to this module"
        }
        dao.saveTemplate(CommunicationTemplate(module.name, status.name, language, body))
    }

    override suspend fun resetCustomTemplate(
        module: CommunicationModule,
        status: CommunicationStatus,
        language: String
    ): String {
        engine.checkAccess(module)
        dao.resetTemplate(module.name, status.name, language)
        return DynamicTemplateEngine.defaultTemplate(module, status, language)
    }

    override suspend fun recordHandoffEvent(history: CommunicationHistory) {
        val mod = runCatching { CommunicationModule.valueOf(history.module) }.getOrNull()
        if (mod != null) engine.checkAccess(mod) else engine.requireReportAccess()
        dao.record(history)
    }
}
