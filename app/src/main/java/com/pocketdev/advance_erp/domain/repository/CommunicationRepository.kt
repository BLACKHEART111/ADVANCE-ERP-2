package com.pocketdev.advance_erp.domain.repository

import com.pocketdev.advance_erp.core.communication.CommunicationChoice
import com.pocketdev.advance_erp.core.communication.CommunicationModule
import com.pocketdev.advance_erp.core.communication.CommunicationRequest
import com.pocketdev.advance_erp.core.communication.CommunicationStatus
import com.pocketdev.advance_erp.core.communication.PreparedCommunication
import com.pocketdev.advance_erp.core.communication.SafeDocument
import com.pocketdev.advance_erp.data.entity.CommunicationHistory
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import kotlinx.coroutines.flow.Flow
import java.io.File

interface CommunicationRepository {
    fun observeSettings(): Flow<CommunicationSettings?>
    fun observeAuthorizedHistory(): Flow<List<CommunicationHistory>>
    suspend fun getSettings(): CommunicationSettings
    suspend fun saveSettings(settings: CommunicationSettings)
    suspend fun getVisibleModules(): List<CommunicationModule>
    suspend fun getChoices(module: CommunicationModule): List<CommunicationChoice>
    suspend fun prepare(
        request: CommunicationRequest,
        languageOverride: String? = null,
        statusOverride: CommunicationStatus? = null,
        forcePdf: Boolean = false,
        useSystemDefaultTemplate: Boolean = false
    ): PreparedCommunication
    suspend fun generateXlsx(document: SafeDocument): File
    suspend fun savePartyLanguage(partyId: String, isSupplier: Boolean, language: String)
    suspend fun loadTemplate(module: CommunicationModule, status: CommunicationStatus, language: String): String
    suspend fun hasCustomTemplate(module: CommunicationModule, status: CommunicationStatus, language: String): Boolean
    suspend fun saveCustomTemplate(module: CommunicationModule, status: CommunicationStatus, language: String, body: String)
    suspend fun resetCustomTemplate(module: CommunicationModule, status: CommunicationStatus, language: String): String
    suspend fun recordHandoffEvent(history: CommunicationHistory)
}
