package com.pocketdev.advance_erp.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.database.BackupArchiveContract
import com.pocketdev.advance_erp.core.database.PortableBackupPreferences
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.notification.BackupNotificationHelper
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.data.dao.AuditLogDao
import com.pocketdev.advance_erp.data.dao.BackupDao
import com.pocketdev.advance_erp.data.entity.AuditLog
import com.pocketdev.advance_erp.data.entity.BackupRecord
import com.pocketdev.advance_erp.data.entity.JournalEntry
import com.pocketdev.advance_erp.data.entity.JournalLine
import com.pocketdev.advance_erp.domain.model.DataClearImpactPreview
import com.pocketdev.advance_erp.domain.model.DataClearModule
import com.pocketdev.advance_erp.domain.model.ModuleImpactCount
import com.pocketdev.advance_erp.domain.model.SelectiveDataClearRequest
import com.pocketdev.advance_erp.domain.model.SelectiveDataClearResult
import com.pocketdev.advance_erp.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AdvanceErpDatabase,
    private val backupDao: BackupDao,
    private val auditLogDao: AuditLogDao,
    private val preferencesRepository: UserPreferencesRepository
) : BackupRepository {

    override fun getAllBackupRecords(): Flow<List<BackupRecord>> = backupDao.getAllBackupRecords()

    override suspend fun createBackup(isAutomatic: Boolean, notes: String?): BackupRecord = withContext(Dispatchers.IO) {
        val snapshotFile = createConsistentDatabaseSnapshot()
        try {
            val backupDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
            // Format file name based on date/mm/yy (dd_MM_yy_HHmmss)
            val timeStamp = SimpleDateFormat("dd_MM_yy_HHmmss", Locale.US).format(Date())
            val fileName = "AdvanceERP_Backup_${timeStamp}.zip"
            val zipBackupFile = File(backupDir, fileName)

            val mediaFilesByDir = BackupArchiveContract.MANAGED_MEDIA_DIRS.associateWith { dirName ->
                val dir = File(context.filesDir, dirName)
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.filter { it.isFile }?.sortedBy { it.name }.orEmpty()
                } else {
                    emptyList()
                }
            }
            val avatarFiles = context.filesDir.listFiles()
                ?.filter {
                    it.isFile &&
                        (it.name.startsWith("avatar_") || it.name.startsWith("company_logo") ||
                            it.name.endsWith(".jpg") || it.name.endsWith(".png")) &&
                        !it.name.endsWith(".zip")
                }
                ?.sortedBy { it.name }
                ?: emptyList()

            val schemaVersion = database.openHelper.readableDatabase.version
            val dbChecksum = calculateSha256(snapshotFile)

            val filesDirCanonical = runCatching { context.filesDir.canonicalFile }.getOrNull()
            fun verifiedManagedRelativePath(rawPath: String?): String? {
                val rel = BackupArchiveContract.extractManagedRelativePath(rawPath, context.filesDir.absolutePath)
                    ?: return null
                val anchored = BackupArchiveContract.anchorManagedRelativePath(rel, context.filesDir.absolutePath)
                    ?: return null
                val candidate = runCatching { File(anchored).canonicalFile }.getOrNull() ?: return null
                if (!candidate.isFile) return null
                if (filesDirCanonical != null) {
                    val basePrefix = filesDirCanonical.path.trimEnd(File.separatorChar) + File.separator
                    if (!candidate.path.startsWith(basePrefix)) return null
                }
                return rel
            }

            val businessProfile = runCatching { database.businessProfileDao().getProfileSync() }.getOrNull()
            val rawPrefProfilePhoto = runCatching { preferencesRepository.profilePhotoUri.firstOrNull() }.getOrNull()
            val rawPrefCompanyLogo = runCatching { preferencesRepository.companyLogoPath.firstOrNull() }.getOrNull()
            val resolvedLogoRelativePath = verifiedManagedRelativePath(rawPrefCompanyLogo)
                ?: verifiedManagedRelativePath(businessProfile?.logoPath)

            val portablePrefs = PortableBackupPreferences(
                appTheme = runCatching { preferencesRepository.selectedTheme.firstOrNull() }.getOrNull(),
                appLanguage = runCatching { preferencesRepository.selectedLanguage.firstOrNull()?.code }.getOrNull(),
                appCurrency = runCatching { preferencesRepository.selectedCurrency.firstOrNull()?.code }.getOrNull(),
                userName = runCatching { preferencesRepository.userName.firstOrNull() }.getOrNull()
                    ?.trim()?.ifBlank { null },
                userRole = runCatching { preferencesRepository.selectedUserRole.firstOrNull()?.code }.getOrNull(),
                profilePhotoRelativePath = verifiedManagedRelativePath(rawPrefProfilePhoto),
                companyLogoRelativePath = resolvedLogoRelativePath,
                fridayAutoBackupEnabled = runCatching { preferencesRepository.isFridayAutoBackupEnabled.firstOrNull() }
                    .getOrNull(),
                legacyMode = false
            )
            val preferencesJsonText = BackupArchiveContract.encodePreferencesJson(portablePrefs)

            val rawPresets = context.getSharedPreferences("control_center_presets", Context.MODE_PRIVATE)
                .getString("presets_list", null)
            val validPresetsArrayJson = rawPresets?.let { raw ->
                runCatching { org.json.JSONArray(raw).toString() }.getOrNull()
            } ?: "[]"
            val presetsPayloadText = org.json.JSONObject().apply {
                put("presetsListJson", validPresetsArrayJson)
            }.toString(2)

            val productImagesCount = mediaFilesByDir["product_images"]?.size ?: 0
            val poAttachmentsCount = mediaFilesByDir["po_attachments"]?.size ?: 0
            val vaultAttachmentsCount = mediaFilesByDir["vault_attachments"]?.size ?: 0
            val customerPhotosCount = mediaFilesByDir["customer_photos"]?.size ?: 0
            val companyLogosCount = mediaFilesByDir["company_logo"]?.size ?: 0
            val companySignaturesCount = mediaFilesByDir["company_signature"]?.size ?: 0
            val totalMediaImagesCount = productImagesCount + avatarFiles.size + customerPhotosCount +
                companyLogosCount + companySignaturesCount

            // Write comprehensive ZIP archive bundling SQLite snapshot + all managed media + preferences + presets + metadata
            ZipOutputStream(FileOutputStream(zipBackupFile)).use { zos ->
                // Entry 1: Atomic Database Snapshot
                zos.putNextEntry(ZipEntry(BackupArchiveContract.DB_ENTRY))
                FileInputStream(snapshotFile).use { fis -> fis.copyTo(zos) }
                zos.closeEntry()

                // Entries 2..N: All managed media & attachment directories
                for ((dirName, files) in mediaFilesByDir) {
                    for (file in files) {
                        zos.putNextEntry(ZipEntry("$dirName/${file.name}"))
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }

                // Avatar & root branding images
                for (av in avatarFiles) {
                    zos.putNextEntry(ZipEntry("avatars/${av.name}"))
                    FileInputStream(av).use { fis -> fis.copyTo(zos) }
                    zos.closeEntry()
                }

                // Portable DataStore Preferences
                zos.putNextEntry(ZipEntry(BackupArchiveContract.PREFERENCES_ENTRY))
                zos.write(preferencesJsonText.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Control Center Saved Filter Presets
                zos.putNextEntry(ZipEntry(BackupArchiveContract.CONTROL_CENTER_PRESETS_ENTRY))
                zos.write(presetsPayloadText.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Metadata JSON (valid JSON escaping via org.json.JSONObject)
                val metadataJson = org.json.JSONObject().apply {
                    put("application", "ADVANCE ERP")
                    put("package", context.packageName)
                    put("backupFormatVersion", BackupArchiveContract.BACKUP_FORMAT_VERSION)
                    put("timestamp", System.currentTimeMillis())
                    put("schemaVersion", schemaVersion)
                    put("dbChecksumSha256", dbChecksum)
                    put("totalImages", totalMediaImagesCount)
                    put("totalProductImages", productImagesCount)
                    put("totalAvatars", avatarFiles.size)
                    put("totalPoAttachments", poAttachmentsCount)
                    put("totalVaultAttachments", vaultAttachmentsCount)
                    put("totalCustomerPhotos", customerPhotosCount)
                    put("totalCompanyLogos", companyLogosCount)
                    put("totalCompanySignatures", companySignaturesCount)
                    put("hasPreferences", true)
                    put("hasControlCenterPresets", true)
                    put("notes", notes ?: "Offline Comprehensive Backup")
                }
                zos.putNextEntry(ZipEntry(BackupArchiveContract.METADATA_ENTRY))
                zos.write(metadataJson.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            // Manual and automatic backups always use a password-free ZIP.
            val finalFileName = fileName
            val finalBackupFile = zipBackupFile

            // Calculate final SHA-256
            val checksum = calculateSha256(finalBackupFile)

            // Automatically save a copy to Phone File Manager (User-selected SAF folder OR Downloads/AdvanceERP_Backups)
            val exportedLocationDesc = exportBackupToPhoneFileManager(
                backupFile = finalBackupFile,
                fileName = finalFileName,
                mimeType = "application/zip"
            )
            val combinedNotes = buildString {
                if (!notes.isNullOrBlank()) append(notes).append(" • ")
                append("Saved to File Manager: $exportedLocationDesc")
            }

            val record = BackupRecord(
                id = "bak-${UUID.randomUUID()}",
                fileName = finalFileName,
                filePath = finalBackupFile.absolutePath,
                fileSizeBytes = finalBackupFile.length(),
                checksumSha256 = checksum,
                schemaVersion = schemaVersion,
                isAutomatic = isAutomatic,
                notes = combinedNotes,
                createdAt = System.currentTimeMillis()
            )

            backupDao.insertBackupRecord(record)

            auditLogDao.insertAuditLog(
                AuditLog(
                    id = "audit-${UUID.randomUUID()}",
                    recordType = "BACKUP",
                    recordId = record.id,
                    action = "BACKUP",
                    details = "Created comprehensive ZIP backup v${BackupArchiveContract.BACKUP_FORMAT_VERSION} $finalFileName (${record.fileSizeBytes / 1024} KB, $totalMediaImagesCount images, $poAttachmentsCount PO files, $vaultAttachmentsCount Vault files, schema v$schemaVersion, File Manager: $exportedLocationDesc, SHA-256: ${checksum.take(8)}...)"
                )
            )

            // Post notification to phone status bar
            BackupNotificationHelper.notifyBackupSuccess(
                context = context,
                fileName = finalFileName,
                locationDesc = exportedLocationDesc,
                isEncrypted = false,
                isAutomatic = isAutomatic
            )

            record
        } finally {
            snapshotFile.delete()
            File(snapshotFile.path + "-wal").delete()
            File(snapshotFile.path + "-shm").delete()
        }
    }

    private fun createConsistentDatabaseSnapshot(): File {
        context.cacheDir.mkdirs()
        val snapshotFile = File(context.cacheDir, "backup_snapshot_${UUID.randomUUID()}.db")
        if (snapshotFile.exists()) snapshotFile.delete()

        val vacuumSucceeded = runCatching {
            val escapedPath = snapshotFile.absolutePath.replace("'", "''")
            database.openHelper.writableDatabase.execSQL("VACUUM INTO '$escapedPath'")
        }.isSuccess

        if (vacuumSucceeded && snapshotFile.exists() && snapshotFile.length() > 100L && verifySqliteDbFile(snapshotFile)) {
            return snapshotFile
        }

        snapshotFile.delete()
        File(snapshotFile.path + "-wal").delete()
        File(snapshotFile.path + "-shm").delete()

        checkpointDatabase()
        val dbFile = context.getDatabasePath(AdvanceErpDatabase.DATABASE_NAME)
        require(dbFile.exists()) { "Database file does not exist at ${dbFile.absolutePath}" }
        FileInputStream(dbFile).use { input ->
            FileOutputStream(snapshotFile).use { output ->
                input.copyTo(output)
            }
        }
        return snapshotFile
    }

    private suspend fun exportBackupToPhoneFileManager(
        backupFile: File,
        fileName: String,
        mimeType: String = "application/zip"
    ): String {
        val customFolderUriStr = try {
            preferencesRepository.backupFolderUri.firstOrNull()
        } catch (_: Exception) {
            null
        }

        // 1. Try saving to User-Selected Phone File Manager Folder (SAF Tree URI)
        if (!customFolderUriStr.isNullOrBlank()) {
            try {
                val treeUri = Uri.parse(customFolderUriStr)
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                val createdDocUri = DocumentsContract.createDocument(
                    context.contentResolver,
                    parentDocumentUri,
                    mimeType,
                    fileName
                )
                if (createdDocUri != null) {
                    context.contentResolver.openOutputStream(createdDocUri)?.use { out ->
                        FileInputStream(backupFile).use { input ->
                            input.copyTo(out)
                        }
                    }
                    return Uri.decode(treeUri.lastPathSegment ?: "Selected Folder") + "/$fileName"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Default / Fallback: Save to Public Downloads/AdvanceERP_Backups so it is always visible in Phone File Manager
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AdvanceERP_Backups")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, contentValues)
                if (itemUri != null) {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        FileInputStream(backupFile).use { input ->
                            input.copyTo(out)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)
                    "Downloads/AdvanceERP_Backups/$fileName"
                } else {
                    saveToLegacyDownloadsFolder(backupFile, fileName)
                }
            } else {
                saveToLegacyDownloadsFolder(backupFile, fileName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            backupFile.absolutePath
        }
    }

    private fun saveToLegacyDownloadsFolder(backupFile: File, fileName: String): String {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = File(publicDownloads, "AdvanceERP_Backups").apply { if (!exists()) mkdirs() }
        val targetFile = File(targetDir, fileName)
        FileInputStream(backupFile).use { input ->
            FileOutputStream(targetFile).use { out ->
                input.copyTo(out)
            }
        }
        return "Downloads/AdvanceERP_Backups/$fileName"
    }

    override suspend fun verifyBackupFile(backupFile: File): Boolean = withContext(Dispatchers.IO) {
        if (!backupFile.exists() || backupFile.length() < 100) return@withContext false

        val effectivePassphrase = try {
            preferencesRepository.backupPassphrase.firstOrNull()?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }

        if (BackupEncryption.isEncrypted(backupFile)) {
            if (effectivePassphrase.isNullOrEmpty()) return@withContext false
            val decrypted = File(context.cacheDir, "verify_${UUID.randomUUID()}.zip")
            try {
                BackupEncryption.decrypt(backupFile, decrypted, effectivePassphrase.toCharArray())
                verifyZipBackup(decrypted)
            } catch (_: Exception) {
                false
            } finally {
                decrypted.delete()
            }
        } else if (BackupArchiveFormat.isZip(backupFile)) {
            verifyZipBackup(backupFile)
        } else {
            verifySqliteDbFile(backupFile)
        }
    }

    private fun verifyZipBackup(zipBackupFile: File): Boolean {
        return try {
            if (!verifyBackupZipEntriesIntegrity(zipBackupFile)) return false
            ZipFile(zipBackupFile).use { zip ->
                val dbEntry = zip.getEntry("database/advance_erp.db") ?: return false
                val tempDbFile = File(context.cacheDir, "verify_db_${UUID.randomUUID()}.db")
                try {
                    zip.getInputStream(dbEntry).use { input ->
                        FileOutputStream(tempDbFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    verifySqliteDbFile(tempDbFile)
                } finally {
                    if (tempDbFile.exists()) tempDbFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun verifySqliteDbFile(dbFile: File): Boolean {
        val header = ByteArray(16)
        return try {
            FileInputStream(dbFile).use { fis ->
                fis.read(header)
            }
            val headerStr = String(header, Charsets.US_ASCII)
            if (!headerStr.startsWith("SQLite format 3")) {
                return false
            }

            val testDb = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            val cursor = testDb.rawQuery("PRAGMA quick_check;", null)
            var isOk = false
            if (cursor.moveToFirst()) {
                val result = cursor.getString(0)
                isOk = result.equals("ok", ignoreCase = true)
            }
            cursor.close()
            testDb.close()
            isOk
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun restoreBackup(backupFile: File): Result<Boolean> = withContext(Dispatchers.IO) {
        var decryptedFile: File? = null
        try {
            val effectivePassphrase = try {
                preferencesRepository.backupPassphrase.firstOrNull()?.takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }

            val restoreFile = if (BackupEncryption.isEncrypted(backupFile)) {
                require(!effectivePassphrase.isNullOrEmpty()) { BackupArchiveFormat.LEGACY_KEY_UNAVAILABLE }
                File(context.cacheDir, "restore_${UUID.randomUUID()}.zip").also {
                    decryptedFile = it
                    BackupEncryption.decrypt(backupFile, it, effectivePassphrase.toCharArray())
                }
            } else backupFile

            if (!verifyBackupFile(restoreFile)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid or corrupted backup file"))
            }

            com.pocketdev.advance_erp.core.database.StagedRestore.stage(context, restoreFile, BackupArchiveFormat.isZip(restoreFile))
            // Live Room instance stays untouched. Apply only in a new process before Room opens.
            Result.success(true)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            Result.failure(e)
        } finally {
            decryptedFile?.delete()
        }
    }

    override suspend fun restoreFromUri(context: Context, uri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        val tempRestoreFile = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}.zip")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempRestoreFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalArgumentException("Cannot open or read chosen backup file"))

            restoreBackup(tempRestoreFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            if (tempRestoreFile.exists()) {
                tempRestoreFile.delete()
            }
        }
    }

    override suspend fun shareBackup(context: Context, backupFile: File) = withContext(Dispatchers.IO) {
        require(backupFile.exists()) { "Backup file does not exist at ${backupFile.absolutePath}" }
        val exportRetentionMillis = 7L * 24 * 60 * 60 * 1000
        context.cacheDir.listFiles { file -> file.name.startsWith("AdvanceERP_Export_") }
            ?.filter { System.currentTimeMillis() - it.lastModified() > exportRetentionMillis }
            ?.forEach { it.delete() }
        val legacyKey = if (BackupEncryption.isEncrypted(backupFile)) {
            preferencesRepository.backupPassphrase.firstOrNull()
        } else null
        val sharedBackup = BackupArchiveFormat.createPlainShareCopy(backupFile, context.cacheDir, legacyKey)
        if (!verifyBackupFile(sharedBackup)) {
            sharedBackup.delete()
            throw IllegalArgumentException("Invalid or corrupted backup file")
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            sharedBackup
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (BackupArchiveFormat.isZip(sharedBackup)) "application/zip" else "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Advance ERP Backup - ${sharedBackup.name}")
            putExtra(Intent.EXTRA_TEXT, "Advance ERP backup. No password is required to restore this file.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "শেয়ার করুন / Share Backup").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    override suspend fun deleteBackup(recordId: String): Boolean = withContext(Dispatchers.IO) {
        val records = backupDao.getAllBackupRecordsSync()
        val record = records.firstOrNull { it.id == recordId }
        if (record != null) {
            val file = File(record.filePath)
            if (file.exists()) file.delete()
            backupDao.deleteBackupRecord(recordId)
            true
        } else false
    }

    /**
     * Clears all transactional, ledger, and stock tables in foreign-key-safe order while
     * preserving products, categories, variants, product photos, customers, and suppliers.
     */
    override suspend fun resetTestTransactionsKeepMasters(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(AdvanceErpDatabase.DATABASE_NAME)
            if (dbFile.exists()) {
                checkpointDatabase()
                val safetyDir = File(context.filesDir, "safety_backups").apply { if (!exists()) mkdirs() }
                val safetyFile = File(safetyDir, "safety_before_test_reset_${System.currentTimeMillis()}.db.bak")
                FileInputStream(dbFile).use { input ->
                    FileOutputStream(safetyFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            database.withTransaction {
                database.communicationDao().clearHistory()
                database.communicationDao().clearSnapshots()
                // 1. Sales, Layer Attributions & Customer Payments
                backupDao.clearSalesReturnLayerAllocations()
                backupDao.clearSaleLayerAllocations()
                backupDao.clearPaymentAllocations()
                backupDao.clearCustomerPayments()
                backupDao.clearSalesReturnItems()
                backupDao.clearSalesReturns()
                backupDao.clearSalesCostLayerUsages()
                backupDao.clearSalesInvoiceItems()
                backupDao.clearSalesInvoices()

                // 2. Inventory Stock, Movements, Transfers, FIFO & Reconciliations
                backupDao.clearFifoConsumptions()
                backupDao.clearInventoryCostAdjustments()
                backupDao.clearStockTransferCostLayers()
                backupDao.clearStockTransfers()
                backupDao.clearInventoryReconciliationItems()
                backupDao.clearInventoryReconciliations()
                backupDao.clearInventoryCostLayers()
                backupDao.clearStockMovements()
                backupDao.clearStockBalances()

                // 3. Purchase Orders, Shipments, Receipts & Landed Costs
                backupDao.clearDamageRecords()
                backupDao.clearShortageRecords()
                backupDao.clearWarehouseReceiptItems()
                backupDao.clearWarehouseReceipts()
                backupDao.clearLandedCostAllocations()
                backupDao.clearLandedCostSnapshots()
                backupDao.clearImportCostItems()
                backupDao.clearShipmentCartons()
                backupDao.clearShipmentSuppliers()
                backupDao.clearShipmentFreightRates()
                backupDao.clearImportShipments()
                backupDao.clearAdditionalProductCosts()
                backupDao.clearPurchaseCompletionRecords()
                backupDao.clearSupplierPayments()
                backupDao.clearPurchaseOrderItems()
                backupDao.clearPurchaseOrders()

                // 4. Accounting, Vault Metadata & Financial Transactions (keep chart_of_accounts)
                backupDao.clearExpenseAllocations()
                backupDao.clearExpenseAllocationRules()
                backupDao.clearImportVaultAttachments()
                backupDao.clearImportVaultEntries()
                backupDao.clearBankReconciliationItems()
                backupDao.clearBankReconciliations()
                backupDao.clearAccountTransfers()
                backupDao.clearExpenseVouchers()
                backupDao.clearJournalLines()
                backupDao.clearJournalEntries()

                // 5. Reset Customer & Supplier Opening/Outstanding Balances to 0
                backupDao.resetAllCustomerBalances()
                backupDao.resetAllSupplierPayables()

                // 6. Clear transaction audit logs (keep backup logs) and log this reset
                backupDao.clearTransactionAuditLogs()
                auditLogDao.insertAuditLog(
                    AuditLog(
                        id = "audit-${UUID.randomUUID()}",
                        recordType = "SYSTEM_RESET",
                        recordId = "TEST_DATA_RESET",
                        action = "RESET_TEST_DATA",
                        details = "Cleared all test transactions, ledgers, and stock to 0 while preserving products, categories, variants, images, customers, and suppliers."
                    )
                )
            }

            // Clear unposted editor draft buffers and transactional PO/Vault attachments (keep product_images & avatars)
            context.getSharedPreferences("editor_drafts_v1", Context.MODE_PRIVATE).edit().clear().commit()
            val poAttachmentsDir = File(context.filesDir, "po_attachments")
            if (poAttachmentsDir.exists()) {
                poAttachmentsDir.listFiles()?.filter { it.isFile }?.forEach { it.delete() }
            }
            val vaultAttachmentsDir = File(context.filesDir, "vault_attachments")
            if (vaultAttachmentsDir.exists()) {
                vaultAttachmentsDir.listFiles()?.filter { it.isFile }?.forEach { it.delete() }
            }

            Result.success(true)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getDataClearImpactPreview(): DataClearImpactPreview = withContext(Dispatchers.IO) {
        val activeProducts = backupDao.countActiveProducts()
        val activeVariants = backupDao.countActiveVariants()
        val accountsCount = backupDao.countChartOfAccounts()
        val customersCount = backupDao.countCustomers()
        val suppliersCount = backupDao.countSuppliers()
        val poCount = backupDao.countPurchaseOrders()
        val receiptsCount = backupDao.countWarehouseReceipts()
        val activeOrPaidPoCount = backupDao.countReceivedOrPaidPurchaseOrders()
        val supplierPaymentsCount = backupDao.countSupplierPayments()
        val stockUnits = backupDao.countTotalStockUnits()
        val stockMovements = backupDao.countStockMovements()
        val invoicesCount = backupDao.countSalesInvoices()
        val returnsCount = backupDao.countSalesReturns()
        val customerPaymentsCount = backupDao.countCustomerPayments()
        val journalCount = backupDao.countJournalEntries()
        val expenseCount = backupDao.countExpenseVouchers()
        val transferCount = backupDao.countAccountTransfers()
        val vaultEntriesCount = backupDao.countImportVaultEntries()
        val vaultAttachCount = backupDao.countImportVaultAttachments()
        val commHistoryCount = backupDao.countCommunicationHistory()
        val commSnapshotsCount = backupDao.countCommunicationSnapshots()
        val auditLogsCount = backupDao.countTransactionAuditLogs()
        val suggestionsCount = backupDao.countSavedSuggestions()
        val poReceiptLayerCount = backupDao.countPoReceiptCostLayers()

        val counts = mapOf(
            DataClearModule.INVENTORY_STOCK_RESET to ModuleImpactCount(
                module = DataClearModule.INVENTORY_STOCK_RESET,
                primaryCount = stockUnits,
                secondaryCount = stockMovements,
                badgeTextEn = "$stockUnits pcs stock -> 0 • $stockMovements movements",
                badgeTextBn = "$stockUnits পিস স্টক -> ০ • $stockMovements মুভমেন্ট"
            ),
            DataClearModule.PO_RESET_KEEP_STRUCTURE to ModuleImpactCount(
                module = DataClearModule.PO_RESET_KEEP_STRUCTURE,
                primaryCount = receiptsCount + supplierPaymentsCount,
                secondaryCount = poCount,
                badgeTextEn = "$poCount PO kept intact • $receiptsCount receipts & $supplierPaymentsCount payments reset ($activeOrPaidPoCount active)",
                badgeTextBn = "$poCount PO অক্ষত থাকবে • $receiptsCount রিসিপ্ট ও $supplierPaymentsCount পেমেন্ট রিসেট ($activeOrPaidPoCount সক্রিয়)"
            ),
            DataClearModule.SALES_FULL_CLEAR to ModuleImpactCount(
                module = DataClearModule.SALES_FULL_CLEAR,
                primaryCount = invoicesCount,
                secondaryCount = customerPaymentsCount + returnsCount,
                badgeTextEn = "$invoicesCount invoices • $customerPaymentsCount payments • $returnsCount returns",
                badgeTextBn = "$invoicesCount ইনভয়েস • $customerPaymentsCount পেমেন্ট • $returnsCount রিটার্ন"
            ),
            DataClearModule.LEDGER_AMOUNTS_RESET to ModuleImpactCount(
                module = DataClearModule.LEDGER_AMOUNTS_RESET,
                primaryCount = journalCount,
                secondaryCount = expenseCount + transferCount,
                badgeTextEn = "$journalCount journals • $expenseCount expenses • $transferCount transfers -> ৳0.00",
                badgeTextBn = "$journalCount জার্নাল • $expenseCount খরচ • $transferCount ট্রান্সফার -> ৳০.০০"
            ),
            DataClearModule.IMPORT_VAULT_CLEAR to ModuleImpactCount(
                module = DataClearModule.IMPORT_VAULT_CLEAR,
                primaryCount = vaultEntriesCount,
                secondaryCount = vaultAttachCount,
                badgeTextEn = "$vaultEntriesCount entries • $vaultAttachCount files",
                badgeTextBn = "$vaultEntriesCount এন্ট্রি • $vaultAttachCount ফাইল"
            ),
            DataClearModule.COMMUNICATION_LOGS_CLEAR to ModuleImpactCount(
                module = DataClearModule.COMMUNICATION_LOGS_CLEAR,
                primaryCount = commHistoryCount,
                secondaryCount = commSnapshotsCount,
                badgeTextEn = "$commHistoryCount logs • $commSnapshotsCount snapshots",
                badgeTextBn = "$commHistoryCount লগ • $commSnapshotsCount স্ন্যাপশট"
            ),
            DataClearModule.AUDIT_AND_DRAFTS_CLEAR to ModuleImpactCount(
                module = DataClearModule.AUDIT_AND_DRAFTS_CLEAR,
                primaryCount = auditLogsCount,
                secondaryCount = suggestionsCount,
                badgeTextEn = "$auditLogsCount audit logs • $suggestionsCount suggestions",
                badgeTextBn = "$auditLogsCount অডিট লগ • $suggestionsCount সাজেশন"
            )
        )

        DataClearImpactPreview(
            moduleCounts = counts,
            preservedProductCount = activeProducts,
            preservedVariantCount = activeVariants,
            preservedPoCount = poCount,
            preservedAccountCount = accountsCount,
            preservedCustomerCount = customersCount,
            preservedSupplierCount = suppliersCount,
            poReceiptLayerCount = poReceiptLayerCount
        )
    }

    override suspend fun executeSelectiveDataClear(
        request: SelectiveDataClearRequest
    ): Result<SelectiveDataClearResult> = withContext(Dispatchers.IO) {
        try {
            val currentRole = preferencesRepository.selectedUserRole.firstOrNull() ?: UserRole.OWNER_ADMIN
            require(request.operatorRole == UserRole.OWNER_ADMIN && currentRole == UserRole.OWNER_ADMIN) {
                "Only Owner / Admin can clear module data"
            }
            require(request.selectedModules.isNotEmpty()) {
                "Select at least one module to clear"
            }

            val safetyRecord = if (request.createSafetyBackup) {
                val notes = "Pre-Reset Safety Backup: " + request.selectedModules.joinToString(", ") { it.name }
                createBackup(isAutomatic = true, notes = notes)
            } else {
                null
            }

            val now = System.currentTimeMillis()
            val selected = request.selectedModules

            database.withTransaction {
                // 1. Sales Invoices, Returns & Customer Payments (Full Clear)
                if (DataClearModule.SALES_FULL_CLEAR in selected) {
                    backupDao.clearSalesReturnLayerAllocations()
                    backupDao.clearSaleLayerAllocations()
                    backupDao.clearPaymentAllocations()
                    backupDao.clearCustomerPayments()
                    backupDao.clearSalesReturnItems()
                    backupDao.clearSalesReturns()
                    backupDao.clearSalesCostLayerUsages()
                    backupDao.clearSalesInvoiceItems()
                    backupDao.clearSalesInvoices()
                    backupDao.deleteCommunicationSnapshotsByModules(
                        listOf("INVOICE", "PAYMENT", "DELIVERY", "RETURN", "EXCHANGE", "QUOTATION", "SALES_ORDER")
                    )
                    if (DataClearModule.LEDGER_AMOUNTS_RESET !in selected) {
                        backupDao.deleteReversalEntriesForReferenceTypes(SALES_JOURNAL_TYPES)
                        backupDao.deleteJournalEntriesByReferenceTypes(SALES_JOURNAL_TYPES)
                    }
                }

                // 2. Inventory Stock Quantities Reset (keep products, variants, photos & warehouses)
                if (DataClearModule.INVENTORY_STOCK_RESET in selected) {
                    backupDao.clearFifoConsumptions()
                    backupDao.clearInventoryCostAdjustments()
                    backupDao.clearStockTransferCostLayers()
                    backupDao.clearStockTransfers()
                    backupDao.clearInventoryReconciliationItems()
                    backupDao.clearInventoryReconciliations()
                    backupDao.clearInventoryCostLayers()
                    backupDao.clearStockMovements()
                    backupDao.clearStockBalances()
                    if (DataClearModule.LEDGER_AMOUNTS_RESET !in selected) {
                        backupDao.deleteReversalEntriesForReferenceTypes(INVENTORY_JOURNAL_TYPES)
                        backupDao.deleteJournalEntriesByReferenceTypes(INVENTORY_JOURNAL_TYPES)
                    }
                }

                // 3. PO Unreceive & Supplier Payment Reset (keep entire PO, ordered qty, price, cartons & packaging)
                if (DataClearModule.PO_RESET_KEEP_STRUCTURE in selected) {
                    // If inventory isn't being fully reset, strip PO-received FIFO layers safely first
                    if (DataClearModule.INVENTORY_STOCK_RESET !in selected) {
                        backupDao.deleteFifoConsumptionsForPoReceiptLayers()
                        backupDao.deleteStockTransferLayersForPoReceiptLayers()
                        backupDao.deleteCostAdjustmentsForPoReceiptLayers()
                        backupDao.deletePoReceiptCostLayers()
                        backupDao.deletePoStockMovements()
                        backupDao.syncStockBalancesWithActiveCostLayers(now)
                        backupDao.deleteEmptyStockBalances()
                    }
                    backupDao.clearDamageRecords()
                    backupDao.clearShortageRecords()
                    backupDao.clearWarehouseReceiptItems()
                    backupDao.clearWarehouseReceipts()
                    backupDao.clearLandedCostAllocations()
                    backupDao.clearLandedCostSnapshots()
                    backupDao.clearAdditionalProductCosts()
                    backupDao.clearPurchaseCompletionRecords()
                    backupDao.clearSupplierPayments()
                    backupDao.deletePostFinalizationImportCostItems()
                    backupDao.resetImportCostItemsPaymentAndCapitalization()
                    backupDao.resetAllPurchaseOrderItemsReceivedQuantity()
                    backupDao.resetAllPurchaseOrdersToUnreceivedConfirmed(now)
                    backupDao.resetShipmentsToUnreceivedDraftKeepPacking(now)
                    backupDao.deleteCommunicationSnapshotsByModules(
                        listOf("SUPPLIER_PAYMENT", "SUPPLIER_STATEMENT")
                    )
                    if (DataClearModule.LEDGER_AMOUNTS_RESET !in selected) {
                        backupDao.deleteReversalEntriesForReferenceTypes(PO_JOURNAL_TYPES)
                        backupDao.deleteJournalEntriesByReferenceTypes(PO_JOURNAL_TYPES)
                    }
                }

                // 4. Ledger, Expenses, Transfers & Party Balances Reset (keep Chart of Accounts & Contact names)
                if (DataClearModule.LEDGER_AMOUNTS_RESET in selected) {
                    backupDao.clearExpenseAllocations()
                    backupDao.clearExpenseAllocationRules()
                    backupDao.clearBankReconciliationItems()
                    backupDao.clearBankReconciliations()
                    backupDao.clearAccountTransfers()
                    backupDao.clearExpenseVouchers()
                    backupDao.clearJournalLines()
                    backupDao.clearJournalEntries()
                    backupDao.resetAllCustomerBalances()
                    backupDao.resetAllSupplierPayables()
                    if (DataClearModule.PO_RESET_KEEP_STRUCTURE !in selected) {
                        backupDao.resetImportCostItemsPaymentAndCapitalization()
                    }
                }

                // 5. Import Vault Clear
                if (DataClearModule.IMPORT_VAULT_CLEAR in selected) {
                    backupDao.clearImportVaultAttachments()
                    backupDao.clearImportVaultEntries()
                }

                // 6. Smart Communication History & Snapshots Clear (keep settings & custom templates)
                if (DataClearModule.COMMUNICATION_LOGS_CLEAR in selected) {
                    database.communicationDao().clearHistory()
                    database.communicationDao().clearSnapshots()
                }

                // 7. Audit Logs (except BACKUP), Saved Suggestions & Unsaved Drafts Clear
                if (DataClearModule.AUDIT_AND_DRAFTS_CLEAR in selected) {
                    backupDao.clearTransactionAuditLogs()
                    backupDao.clearSavedSuggestions()
                }

                // 8. Re-sync PO commitment journals & party balances whenever sales, POs, or ledgers changed
                if (DataClearModule.PO_RESET_KEEP_STRUCTURE in selected ||
                    DataClearModule.LEDGER_AMOUNTS_RESET in selected
                ) {
                    val accountingDao = database.accountingDao()
                    val accTransit = accountingDao.getAccountById("coa-1220")
                    val accPayable = accountingDao.getAccountById("coa-2010")
                    if (accTransit != null && accPayable != null) {
                        backupDao.deleteReversalEntriesForReferenceTypes(listOf("PO_COMMITMENT"))
                        backupDao.deleteJournalEntriesByReferenceTypes(listOf("PO_COMMITMENT"))
                        backupDao.getConfirmedPurchaseOrdersForCommitmentSync().forEachIndexed { idx, po ->
                            val jeId = "je-pocom-${UUID.randomUUID()}"
                            accountingDao.postJournalEntryWithLines(
                                entry = JournalEntry(
                                    id = jeId,
                                    entryNumber = "JE-POC-${idx + 1}",
                                    entryDate = po.orderDate,
                                    descriptionEn = "PO Confirmed (Goods In-Transit): ${po.poNumber}",
                                    descriptionBn = "ক্রয়াদেশ নিশ্চিতকরণ ও ট্রানজিট পণ্য: ${po.poNumber}",
                                    referenceType = "PO_COMMITMENT",
                                    referenceId = po.id,
                                    totalDebitMinor = po.totalBdtAmountMinor,
                                    totalCreditMinor = po.totalBdtAmountMinor,
                                    isPosted = true
                                ),
                                lines = listOf(
                                    JournalLine(
                                        id = "jl-${UUID.randomUUID()}",
                                        journalEntryId = jeId,
                                        accountId = accTransit.id,
                                        accountCode = accTransit.accountCode,
                                        accountName = accTransit.nameEn,
                                        isDebit = true,
                                        amountMinor = po.totalBdtAmountMinor,
                                        description = "Merchandise in transit for ${po.poNumber}"
                                    ),
                                    JournalLine(
                                        id = "jl-${UUID.randomUUID()}",
                                        journalEntryId = jeId,
                                        accountId = accPayable.id,
                                        accountCode = accPayable.accountCode,
                                        accountName = accPayable.nameEn,
                                        isDebit = false,
                                        amountMinor = po.totalBdtAmountMinor,
                                        description = "Supplier payable for ${po.poNumber}"
                                    )
                                )
                            )
                        }
                    }
                }

                if (DataClearModule.SALES_FULL_CLEAR in selected ||
                    DataClearModule.PO_RESET_KEEP_STRUCTURE in selected ||
                    DataClearModule.LEDGER_AMOUNTS_RESET in selected
                ) {
                    backupDao.recalculateAllCustomerBalancesFromRemainingRecords()
                    backupDao.recalculateAllSupplierPayablesFromRemainingRecords()
                }

                val moduleNames = selected.joinToString(", ") { it.name }
                auditLogDao.insertAuditLog(
                    AuditLog(
                        id = "audit-${UUID.randomUUID()}",
                        recordType = "SYSTEM_RESET",
                        recordId = "SELECTIVE_DATA_CLEAR",
                        action = "SELECTIVE_RESET",
                        details = "Selective data clear completed for [$moduleNames]. SafetyBackup=${safetyRecord?.fileName ?: "Skipped"}."
                    )
                )
            }

            // Post-transaction disk cache / SharedPreferences cleanup matching selected modules
            if (DataClearModule.IMPORT_VAULT_CLEAR in selected) {
                val vaultDir = File(context.filesDir, "vault_attachments")
                if (vaultDir.exists()) {
                    vaultDir.listFiles()?.filter { it.isFile }?.forEach { it.delete() }
                }
            }

            if (DataClearModule.COMMUNICATION_LOGS_CLEAR in selected) {
                listOf("communication_docs", "communication_exports", "communication_cards", "shared_pdfs").forEach { dirName ->
                    val dir = File(context.cacheDir, dirName)
                    if (dir.exists()) {
                        dir.deleteRecursively()
                    }
                }
            }

            val draftPrefs = context.getSharedPreferences("editor_drafts_v1", Context.MODE_PRIVATE)
            if (DataClearModule.AUDIT_AND_DRAFTS_CLEAR in selected) {
                draftPrefs.edit().clear().commit()
            } else {
                val editor = draftPrefs.edit()
                var changed = false
                if (DataClearModule.SALES_FULL_CLEAR in selected) {
                    draftPrefs.all.keys.filter { it.startsWith("sales") || it.startsWith("invoice") }.forEach {
                        editor.remove(it)
                        changed = true
                    }
                }
                if (DataClearModule.PO_RESET_KEEP_STRUCTURE in selected) {
                    draftPrefs.all.keys.filter { it.startsWith("purchase") || it.startsWith("po") }.forEach {
                        editor.remove(it)
                        changed = true
                    }
                }
                if (changed) editor.commit()
            }

            val namesEn = selected.joinToString(", ") { it.titleEn }
            val namesBn = selected.joinToString(", ") { it.titleBn }
            Result.success(
                SelectiveDataClearResult(
                    clearedModules = selected,
                    safetyBackupRecordId = safetyRecord?.id,
                    safetyBackupFileName = safetyRecord?.fileName,
                    summaryEn = "Successfully reset: $namesEn",
                    summaryBn = "সফলভাবে রিসেট সম্পন্ন হয়েছে: $namesBn"
                )
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private companion object {
        val SALES_JOURNAL_TYPES = listOf(
            "SALE",
            "SALES_INVOICE",
            "SALES_COGS",
            "CUSTOMER_PAYMENT",
            "SALES_RETURN",
            "RETURN_COGS",
            "RETURN_DAMAGE",
            "DELIVERY_CHALLAN",
            "DIRECT_EXCHANGE",
            "DIRECT_EXCHANGE_RETURN_COGS",
            "DIRECT_EXCHANGE_DAMAGE",
            "DIRECT_EXCHANGE_COGS",
            "VOID_PAYMENT",
            "SALES_INVOICE_CANCEL",
            "PAYMENT_TO_ADVANCE",
            "VOID_INVOICE_SALES",
            "VOID_INVOICE_COGS",
            "INVOICE"
        )

        val INVENTORY_JOURNAL_TYPES = listOf(
            "OPENING_STOCK",
            "INITIAL_STOCK",
            "RESTOCK_ADD",
            "STOCK_ADJUSTMENT",
            "MANUAL_ADJUSTMENT",
            "ADJUSTMENT",
            "INVENTORY_COST_ADJUSTMENT",
            "STOCK_TRANSFER",
            "TRANSFER_CANCELLATION"
        )

        val PO_JOURNAL_TYPES = listOf(
            "PO_COMMITMENT",
            "PO_IMPORT_EXPENSE",
            "PO_LANDED_CAPITALIZE",
            "PO_FREIGHT_PAYMENT",
            "SUPPLIER_PAYMENT",
            "IMPORT_EXPENSE",
            "LANDED_COST_ADJUSTMENT",
            "LANDING_COST_FINALIZE",
            "WAREHOUSE_RECEIPT",
            "DAMAGE_WRITE_OFF",
            "SHORTAGE_CLAIM",
            "ADDITIONAL_COST",
            "ADDITIONAL_PRODUCT_COST",
            "PO_REOPEN_EDIT",
            "POST_FINALIZE_IMPORT_EXPENSE",
            "PURCHASE",
            "FREIGHT_DUTY_PAYMENT",
            "WAREHOUSE_RENT_EXPENSE",
            "CUSTOMER_DELIVERY_EXPENSE"
        )
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun checkpointDatabase() {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
            check(cursor.moveToFirst() && cursor.getInt(0) == 0) {
                "Database is busy; could not safely checkpoint before backup operation"
            }
        }
    }
}

internal fun isSafeBackupZipEntry(name: String): Boolean =
    BackupArchiveContract.isSafeBackupZipEntry(name)

internal fun verifyBackupZipEntriesIntegrity(zipBackupFile: File): Boolean {
    return try {
        ZipFile(zipBackupFile).use { zip ->
            var hasDatabaseEntry = false
            val buffer = ByteArray(8192)
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!isSafeBackupZipEntry(entry.name)) return false
                if (entry.name == "database/advance_erp.db") {
                    hasDatabaseEntry = true
                }
                if (!entry.isDirectory) {
                    zip.getInputStream(entry).use { input ->
                        while (input.read(buffer) != -1) {
                            // Drain full stream so ZIP CRC32 and deflate blocks are validated
                        }
                    }
                }
            }
            hasDatabaseEntry
        }
    } catch (_: Exception) {
        false
    }
}

internal fun extractVaultAttachmentsFromZip(zip: ZipFile, targetVaultAttachmentsDir: File): Int {
    if (!targetVaultAttachmentsDir.exists()) targetVaultAttachmentsDir.mkdirs()
    var count = 0
    val entries = zip.entries()
    while (entries.hasMoreElements()) {
        val entry = entries.nextElement()
        if (entry.name.startsWith("vault_attachments/") && !entry.isDirectory) {
            val vaultFileName = safeBackupEntryFileName(entry.name, "vault_attachments/")
                ?: throw IllegalArgumentException("Unsafe backup vault attachment path")
            val destVaultFile = File(targetVaultAttachmentsDir, vaultFileName)
            zip.getInputStream(entry).use { input ->
                FileOutputStream(destVaultFile).use { output ->
                    input.copyTo(output)
                }
            }
            count++
        }
    }
    return count
}

private fun safeBackupEntryFileName(entryName: String, prefix: String): String? {
    if (!isSafeBackupZipEntry(entryName) || !entryName.startsWith(prefix)) return null
    val fileName = entryName.removePrefix(prefix)
    return fileName.takeIf { it.isNotBlank() && '/' !in it && '\\' !in it }
}
