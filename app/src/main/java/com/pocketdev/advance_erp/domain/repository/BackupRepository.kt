package com.pocketdev.advance_erp.domain.repository

import android.content.Context
import android.net.Uri
import com.pocketdev.advance_erp.data.entity.BackupRecord
import kotlinx.coroutines.flow.Flow
import java.io.File

interface BackupRepository {
    fun getAllBackupRecords(): Flow<List<BackupRecord>>
    suspend fun createBackup(isAutomatic: Boolean = false, notes: String? = null): BackupRecord
    suspend fun restoreBackup(backupFile: File, passphrase: String? = null): Result<Boolean>
    suspend fun restoreFromUri(context: Context, uri: Uri, passphrase: String? = null): Result<Boolean>
    suspend fun verifyBackupFile(backupFile: File, passphrase: String? = null): Boolean
    suspend fun shareBackup(context: Context, backupFile: File, passphrase: String)
    suspend fun deleteBackup(recordId: String): Boolean
}
