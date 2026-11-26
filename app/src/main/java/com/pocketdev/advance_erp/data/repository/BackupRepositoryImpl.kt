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
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.notification.BackupNotificationHelper
import com.pocketdev.advance_erp.data.dao.AuditLogDao
import com.pocketdev.advance_erp.data.dao.BackupDao
import com.pocketdev.advance_erp.data.entity.AuditLog
import com.pocketdev.advance_erp.data.entity.BackupRecord
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
        checkpointDatabase()

        val dbFile = context.getDatabasePath(AdvanceErpDatabase.DATABASE_NAME)
        require(dbFile.exists()) { "Database file does not exist at ${dbFile.absolutePath}" }

        val backupDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
        // Format file name based on date/mm/yy (dd_MM_yy_HHmmss)
        val timeStamp = SimpleDateFormat("dd_MM_yy_HHmmss", Locale.US).format(Date())
        val fileName = "AdvanceERP_Backup_${timeStamp}.zip"
        val zipBackupFile = File(backupDir, fileName)

        val imagesDir = File(context.filesDir, "product_images")
        val imageFiles = if (imagesDir.exists()) imagesDir.listFiles()?.filter { it.isFile } ?: emptyList() else emptyList()
        val poAttachmentsDir = File(context.filesDir, "po_attachments")
        val poAttachmentFiles = if (poAttachmentsDir.exists()) poAttachmentsDir.listFiles()?.filter { it.isFile } ?: emptyList() else emptyList()
        val vaultAttachmentsDir = File(context.filesDir, "vault_attachments")
        val vaultAttachmentFiles = if (vaultAttachmentsDir.exists()) vaultAttachmentsDir.listFiles()?.filter { it.isFile } ?: emptyList() else emptyList()
        val avatarFiles = context.filesDir.listFiles()?.filter { it.isFile && (it.name.startsWith("avatar_") || it.name.endsWith(".jpg") || it.name.endsWith(".png")) && !it.name.endsWith(".zip") } ?: emptyList()

        val schemaVersion = database.openHelper.readableDatabase.version
        val dbChecksum = calculateSha256(dbFile)

        // Write comprehensive ZIP archive bundling SQLite database + product_images/ + avatars/ + metadata.json
        ZipOutputStream(FileOutputStream(zipBackupFile)).use { zos ->
            // Entry 1: Database
            val dbEntry = ZipEntry("database/advance_erp.db")
            zos.putNextEntry(dbEntry)
            FileInputStream(dbFile).use { fis -> fis.copyTo(zos) }
            zos.closeEntry()

            // Entry 2..N: Product Images
            for (img in imageFiles) {
                val imgEntry = ZipEntry("product_images/${img.name}")
                zos.putNextEntry(imgEntry)
                FileInputStream(img).use { fis -> fis.copyTo(zos) }
                zos.closeEntry()
            }

            // Entry N..M: Avatar & Profile Images
            for (av in avatarFiles) {
                val avEntry = ZipEntry("avatars/${av.name}")
                zos.putNextEntry(avEntry)
                FileInputStream(av).use { fis -> fis.copyTo(zos) }
                zos.closeEntry()
            }

            // Entry M..K: PO Attachments (Invoices, PDFs & Photos)
            for (att in poAttachmentFiles) {
                val attEntry = ZipEntry("po_attachments/${att.name}")
                zos.putNextEntry(attEntry)
                FileInputStream(att).use { fis -> fis.copyTo(zos) }
                zos.closeEntry()
            }

            // Entry K..L: Import Vault Attachments & Immutable Snapshots
            for (vAtt in vaultAttachmentFiles) {
                val vEntry = ZipEntry("vault_attachments/${vAtt.name}")
                zos.putNextEntry(vEntry)
                FileInputStream(vAtt).use { fis -> fis.copyTo(zos) }
                zos.closeEntry()
            }

            // Entry M+1: Metadata JSON
            val metadataContent = buildString {
                append("{\n")
                append("  \"application\": \"ADVANCE ERP\",\n")
                append("  \"package\": \"com.pocketdev.advance_erp\",\n")
                append("  \"timestamp\": ${System.currentTimeMillis()},\n")
                append("  \"schemaVersion\": $schemaVersion,\n")
                append("  \"dbChecksumSha256\": \"$dbChecksum\",\n")
                append("  \"totalImages\": ${imageFiles.size + avatarFiles.size},\n")
                append("  \"totalVaultAttachments\": ${vaultAttachmentFiles.size},\n")
                append("  \"notes\": \"${notes?.replace("\"", "\\\"") ?: "Offline Comprehensive Backup"}\"\n")
                append("}\n")
            }
            val metaEntry = ZipEntry("metadata.json")
            zos.putNextEntry(metaEntry)
            zos.write(metadataContent.toByteArray(Charsets.UTF_8))
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
                details = "Created comprehensive ZIP backup $finalFileName (${record.fileSizeBytes / 1024} KB, ${imageFiles.size} images, schema v$schemaVersion, File Manager: $exportedLocationDesc, SHA-256: ${checksum.take(8)}...)"
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

internal fun isSafeBackupZipEntry(name: String): Boolean {
    if (name.isBlank() || name.startsWith('/') || '\\' in name) return false
    val parts = name.split('/')
    if (parts.any { it == "." || it == ".." }) return false
    if (name == "metadata.json" || name == "database/advance_erp.db") return true
    if (name == "product_images/" || name == "avatars/" || name == "po_attachments/" || name == "vault_attachments/") return true
    return (parts.size == 2 && parts[1].isNotBlank() &&
        (parts[0] == "product_images" || parts[0] == "avatars" || parts[0] == "po_attachments" || parts[0] == "vault_attachments"))
}

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
