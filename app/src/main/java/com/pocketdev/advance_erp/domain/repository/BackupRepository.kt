package com.pocketdev.advance_erp.domain.repository

import android.content.Context
import android.net.Uri
import com.pocketdev.advance_erp.data.entity.BackupRecord
import com.pocketdev.advance_erp.domain.model.DataClearImpactPreview
import com.pocketdev.advance_erp.domain.model.SelectiveDataClearRequest
import com.pocketdev.advance_erp.domain.model.SelectiveDataClearResult
import kotlinx.coroutines.flow.Flow
import java.io.File

interface BackupRepository {
    fun getAllBackupRecords(): Flow<List<BackupRecord>>
    suspend fun createBackup(isAutomatic: Boolean = false, notes: String? = null): BackupRecord
    suspend fun restoreBackup(backupFile: File): Result<Boolean>
    suspend fun restoreFromUri(context: Context, uri: Uri): Result<Boolean>
    suspend fun verifyBackupFile(backupFile: File): Boolean
    suspend fun shareBackup(context: Context, backupFile: File)
    suspend fun deleteBackup(recordId: String): Boolean
    suspend fun resetTestTransactionsKeepMasters(): Result<Boolean>
    suspend fun getDataClearImpactPreview(): DataClearImpactPreview
    suspend fun executeSelectiveDataClear(request: SelectiveDataClearRequest): Result<SelectiveDataClearResult>
}
