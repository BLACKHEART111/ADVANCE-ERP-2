package com.pocketdev.advance_erp.data.dao

import androidx.room.*
import com.pocketdev.advance_erp.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunicationDao {
    @Query("SELECT * FROM communication_settings WHERE id = 1") suspend fun settings(): CommunicationSettings?
    @Query("SELECT * FROM communication_settings WHERE id = 1") fun observeSettings(): Flow<CommunicationSettings?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSettings(value: CommunicationSettings)
    @Query("SELECT * FROM communication_templates WHERE module = :module AND status = :status AND language = :language")
    suspend fun template(module: String, status: String, language: String): CommunicationTemplate?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTemplate(value: CommunicationTemplate)
    @Query("DELETE FROM communication_templates WHERE module = :module AND status = :status AND language = :language")
    suspend fun resetTemplate(module: String, status: String, language: String)
    @Query("SELECT * FROM communication_party_preferences WHERE partyKey = :key") suspend fun preference(key: String): CommunicationPartyPreference?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePreference(value: CommunicationPartyPreference)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun snapshot(value: CommunicationSnapshot)
    @Query("SELECT * FROM communication_snapshots WHERE module = :module AND documentId = :id") suspend fun snapshot(module: String, id: String): CommunicationSnapshot?
    @Query("SELECT * FROM communication_snapshots WHERE module = :module ORDER BY createdAt DESC") suspend fun snapshots(module: String): List<CommunicationSnapshot>
    @Insert suspend fun record(value: CommunicationHistory)
    @Query("SELECT * FROM communication_history ORDER BY createdAt DESC LIMIT 300") fun history(): Flow<List<CommunicationHistory>>
    @Query("DELETE FROM communication_history") suspend fun clearHistory()
    @Query("DELETE FROM communication_snapshots") suspend fun clearSnapshots()
}
