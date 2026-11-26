package com.pocketdev.advance_erp.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipFile

data class StagedRestoreOutcome(
    val success: Boolean,
    val message: String
)

/** Restore is applied before any Room/WorkManager initialization in a fresh process. */
object StagedRestore {
    private val mediaDirs = listOf("product_images", "po_attachments", "vault_attachments")
    private const val STAGING_DIR = "restore_staging"
    private const val RESULT_FILE = "restore_result.txt"
    private const val MAX_ZIP_ENTRIES = 10_000
    private const val MAX_RESTORE_BYTES = 2L * 1024 * 1024 * 1024

    fun isRestorePending(context: Context): Boolean =
        File(File(context.filesDir, STAGING_DIR), "ready").exists()

    fun cancelPending(context: Context): Boolean {
        val root = File(context.filesDir, STAGING_DIR)
        if (File(root, "applying").exists()) return false
        return if (root.exists()) root.deleteRecursively() else true
    }

    fun consumeRestoreResult(context: Context): StagedRestoreOutcome? {
        val resultFile = File(context.filesDir, RESULT_FILE)
        if (!resultFile.exists()) return null
        val raw = runCatching { resultFile.readText() }.getOrNull()
        runCatching { resultFile.delete() }
        val text = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val parts = text.split("|", limit = 3)
        return if (parts.size == 3 && (parts[0] == "SUCCESS" || parts[0] == "FAILED")) {
            StagedRestoreOutcome(success = parts[0] == "SUCCESS", message = parts[2])
        } else {
            StagedRestoreOutcome(
                success = !text.contains("failed", ignoreCase = true),
                message = text
            )
        }
    }

    fun stage(context: Context, source: File, zip: Boolean) {
        val root = File(context.filesDir, STAGING_DIR)
        require(!File(root, "ready").exists()) {
            "A restore is already staged. Restart or cancel the pending restore first."
        }
        require(!File(root, "applying").exists()) {
            "A restore operation is currently in progress."
        }
        root.deleteRecursively()
        check(root.mkdirs()) { "Could not create restore staging directory" }
        try {
            val dbFile = File(root, "database.db")
            var metadataJson: JSONObject? = null
            if (zip) {
                ZipFile(source).use { archive ->
                    val dbEntry = requireNotNull(archive.getEntry("database/advance_erp.db")) { "Database missing in backup ZIP" }
                    archive.getInputStream(dbEntry).use { input -> dbFile.outputStream().use { input.copyTo(it) } }

                    archive.getEntry("metadata.json")?.takeIf { !it.isDirectory }?.let { metaEntry ->
                        val metaBytes = archive.getInputStream(metaEntry).use { it.readBytes() }
                        if (metaBytes.size <= 512 * 1024) {
                            metadataJson = runCatching { JSONObject(metaBytes.toString(Charsets.UTF_8)) }.getOrNull()
                        }
                    }

                    val entries = archive.entries()
                    val seenNames = HashSet<String>()
                    var entryCount = 0
                    var total = dbFile.length()
                    require(total <= MAX_RESTORE_BYTES) { "Restore exceeds 2 GiB" }
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        entryCount++
                        require(entryCount <= MAX_ZIP_ENTRIES) { "Backup ZIP contains too many entries" }
                        require(seenNames.add(entry.name)) { "Duplicate ZIP entry: ${entry.name}" }
                        val name = entry.name
                        require(isSafeStageEntry(name)) { "Unsafe restore path: $name" }
                        val dir = name.substringBefore('/')
                        if (entry.isDirectory || (dir !in mediaDirs && dir != "avatars")) continue
                        val fileName = name.substringAfter('/')
                        val target = File(File(root, dir).apply { mkdirs() }, fileName)
                        archive.getInputStream(entry).use { input ->
                            target.outputStream().use { output ->
                                val buffer = ByteArray(8192)
                                while (true) {
                                    val n = input.read(buffer)
                                    if (n < 0) break
                                    total += n
                                    require(total <= MAX_RESTORE_BYTES) { "Restore exceeds 2 GiB" }
                                    output.write(buffer, 0, n)
                                }
                            }
                        }
                    }
                }
            } else {
                source.copyTo(dbFile)
            }

            verifySqliteHeader(dbFile)
            metadataJson?.let { meta ->
                if (meta.has("schemaVersion")) {
                    val v = meta.optInt("schemaVersion", -1)
                    require(v in 1..AdvanceErpDatabase.DATABASE_VERSION) {
                        "Unsupported backup metadata schema version: $v"
                    }
                }
                val expectedSha = meta.optString("dbChecksumSha256", "").trim()
                if (expectedSha.isNotBlank()) {
                    val actualSha = sha256Hex(dbFile)
                    require(actualSha.equals(expectedSha, ignoreCase = true)) {
                        "Backup database checksum mismatch"
                    }
                }
            }

            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                require(db.version in 1..AdvanceErpDatabase.DATABASE_VERSION) {
                    "Unsupported backup schema version: ${db.version}"
                }
                db.rawQuery("PRAGMA quick_check", null).use { c ->
                    require(c.moveToFirst() && c.getString(0).equals("ok", ignoreCase = true)) {
                        "Corrupt backup SQLite database"
                    }
                }
                val requiredTables = listOf("customers", "products", "sales_invoices", "chart_of_accounts")
                for (tbl in requiredTables) {
                    db.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                        arrayOf(tbl)
                    ).use { c ->
                        require(c.moveToFirst()) { "Invalid ERP backup: missing core table '$tbl'" }
                    }
                }
            }

            // Validate and migrate an isolated staged copy; never open/replace the live Room database here.
            val staged = Room.databaseBuilder(context.applicationContext, AdvanceErpDatabase::class.java, dbFile.absolutePath)
                .addMigrations(
                    DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3, DatabaseMigrations.MIGRATION_3_4,
                    DatabaseMigrations.MIGRATION_4_5, DatabaseMigrations.MIGRATION_5_6, DatabaseMigrations.MIGRATION_6_7,
                    DatabaseMigrations.MIGRATION_7_8, DatabaseMigrations.MIGRATION_8_9, DatabaseMigrations.MIGRATION_9_10,
                    DatabaseMigrations.MIGRATION_10_11, DatabaseMigrations.MIGRATION_11_12, DatabaseMigrations.MIGRATION_12_13,
                    DatabaseMigrations.MIGRATION_13_14, DatabaseMigrations.MIGRATION_14_15
                ).build()
            try {
                val writable = staged.openHelper.writableDatabase
                writable.query("PRAGMA foreign_key_check").use { fk ->
                    require(!fk.moveToFirst()) { "Backup failed foreign-key integrity check" }
                }
                if (zip) {
                    rewriteManagedPathsForTargetFilesDir(writable, context.filesDir.absolutePath)
                }
                writable.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            } finally {
                staged.close()
            }
            File(root, "ready").writeText(if (zip) "ZIP" else "DATABASE")
        } catch (e: Exception) {
            root.deleteRecursively()
            throw e
        }
    }

    /** Called only from Application.attachBaseContext, before provider/Room initialization. */
    fun applyPending(context: Context) {
        val root = File(context.filesDir, STAGING_DIR)
        val readyFile = File(root, "ready")
        val marker = File(root, "applying")
        if (!readyFile.exists() && !marker.exists()) return

        val live = context.getDatabasePath(AdvanceErpDatabase.DATABASE_NAME)
        val rollback = File(root, "rollback")
        if (marker.exists() && runCatching { marker.readText() }.getOrNull() == "committed") {
            root.deleteRecursively()
            return
        }

        val isZipRestore = runCatching { readyFile.readText() }.getOrNull() == "ZIP"

        fun restorePreviousAndVerify() {
            val rollbackDb = File(rollback, "database.db")
            if (rollbackDb.exists()) {
                live.parentFile?.mkdirs()
                rollbackDb.copyTo(live, overwrite = true)
                File(live.path + "-wal").delete()
                File(live.path + "-shm").delete()
                verifySqliteHealthy(live)
            }
            if (isZipRestore) {
                mediaDirs.forEach { dirName ->
                    val liveDir = File(context.filesDir, dirName)
                    liveDir.deleteRecursively()
                    File(rollback, dirName).takeIf { it.exists() }?.copyRecursively(liveDir, overwrite = true)
                }
                // Remove managed avatars added by failed restore and restore previous ones
                context.filesDir.listFiles()?.filter { isManagedRootAvatar(it) }?.forEach { it.delete() }
                File(rollback, "avatars").listFiles()?.forEach { old ->
                    old.copyTo(File(context.filesDir, old.name), overwrite = true)
                }
            }
        }

        try {
            if (marker.exists()) {
                restorePreviousAndVerify()
                error("Interrupted restore rolled back")
            }
            rollback.mkdirs()
            if (live.exists()) {
                SQLiteDatabase.openDatabase(live.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                    db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
                }
                live.copyTo(File(rollback, "database.db"), overwrite = true)
            }
            if (isZipRestore) {
                mediaDirs.forEach { dirName ->
                    File(context.filesDir, dirName).takeIf { it.exists() }?.copyRecursively(File(rollback, dirName), overwrite = true)
                }
                val rollbackAvatars = File(rollback, "avatars").apply { mkdirs() }
                context.filesDir.listFiles()?.filter { isManagedRootAvatar(it) }?.forEach { existingAvatar ->
                    existingAvatar.copyTo(File(rollbackAvatars, existingAvatar.name), overwrite = true)
                }
            }

            marker.writeText("prepared")
            live.parentFile?.mkdirs()
            File(live.path + "-wal").delete()
            File(live.path + "-shm").delete()
            File(root, "database.db").copyTo(live, overwrite = true)

            if (isZipRestore) {
                mediaDirs.forEach { dirName ->
                    val liveDir = File(context.filesDir, dirName)
                    liveDir.deleteRecursively()
                    File(root, dirName).takeIf { it.exists() }?.copyRecursively(liveDir, overwrite = true)
                }
                context.filesDir.listFiles()?.filter { isManagedRootAvatar(it) }?.forEach { it.delete() }
                File(root, "avatars").listFiles()?.forEach { incoming ->
                    incoming.copyTo(File(context.filesDir, incoming.name), overwrite = true)
                }
            }

            SQLiteDatabase.openDatabase(live.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.rawQuery("PRAGMA quick_check", null).use { c ->
                    check(c.moveToFirst() && c.getString(0).equals("ok", ignoreCase = true)) {
                        "Restored database failed integrity check"
                    }
                }
                db.execSQL(
                    "INSERT INTO audit_logs (id, recordType, recordId, action, details, operator, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(UUID.randomUUID().toString(), "RESTORE", "staged", "RESTORE", "Validated staged backup restored at startup", "SYSTEM", System.currentTimeMillis())
                )
            }

            val ts = System.currentTimeMillis()
            val safetyParent = File(context.filesDir, "safety_backups").apply { mkdirs() }
            val safety = File(safetyParent, "restore_$ts")
            if (!rollback.renameTo(safety)) {
                check(rollback.copyRecursively(safety, overwrite = false)) { "Could not persist pre-restore safety backup" }
            }
            pruneOldRestoreSafetyBackups(safetyParent, keepCount = 3)

            marker.writeText("committed")
            root.deleteRecursively()
            File(context.filesDir, RESULT_FILE).writeText(
                "SUCCESS|$ts|ব্যাকআপ সফলভাবে রিস্টোর সম্পন্ন হয়েছে / Validated backup restored successfully"
            )
        } catch (e: Exception) {
            if (marker.exists() && rollback.exists()) {
                try {
                    restorePreviousAndVerify()
                } catch (rollbackErr: Exception) {
                    // Never boot Room against an unrecovered partial state
                    throw IllegalStateException("Critical restore rollback failure: ${rollbackErr.message}", rollbackErr)
                }
            }
            File(context.filesDir, RESULT_FILE).writeText(
                "FAILED|${System.currentTimeMillis()}|রিস্টোর ব্যর্থ হয়েছে; পূর্বের সব ডাটা অক্ষত রাখা হয়েছে (${e.message}) / Restore failed; previous data retained (${e.message})"
            )
            readyFile.delete()
            marker.delete()
        }
    }

    private fun isSafeStageEntry(name: String): Boolean {
        if (name.isBlank() || name.startsWith('/') || '\\' in name) return false
        val parts = name.split('/')
        if (parts.any { it == "." || it == ".." }) return false
        if (name == "metadata.json" || name == "database/advance_erp.db") return true
        if (name in setOf("product_images/", "avatars/", "po_attachments/", "vault_attachments/")) return true
        return parts.size == 2 && parts[1].isNotBlank() &&
            parts[0] in setOf("product_images", "avatars", "po_attachments", "vault_attachments")
    }

    private fun isManagedRootAvatar(file: File): Boolean =
        file.isFile && (file.name.startsWith("avatar_") || file.name.startsWith("company_logo"))

    private fun verifySqliteHeader(dbFile: File) {
        val header = ByteArray(16)
        FileInputStream(dbFile).use { fis ->
            val read = fis.read(header)
            require(read == 16 && String(header, Charsets.US_ASCII).startsWith("SQLite format 3")) {
                "Invalid SQLite database header"
            }
        }
    }

    private fun verifySqliteHealthy(dbFile: File) {
        verifySqliteHeader(dbFile)
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("PRAGMA quick_check", null).use { c ->
                check(c.moveToFirst() && c.getString(0).equals("ok", ignoreCase = true)) {
                    "Rollback database integrity verification failed"
                }
            }
        }
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buf = ByteArray(8192)
            while (true) {
                val n = fis.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun pruneOldRestoreSafetyBackups(safetyParent: File, keepCount: Int) {
        val dirs = safetyParent.listFiles { f -> f.isDirectory && f.name.startsWith("restore_") }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        dirs.drop(keepCount).forEach { it.deleteRecursively() }
    }

    /** Re-anchors local `/files/...` paths inside a restored DB to the active package's `filesDir`. */
    private fun rewriteManagedPathsForTargetFilesDir(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        targetFilesDirPath: String
    ) {
        val base = targetFilesDirPath.trimEnd('/')
        fun rewriteSubdir(table: String, column: String, subdir: String) {
            db.query("SELECT rowid, $column FROM `$table` WHERE $column LIKE ?", arrayOf("%/files/$subdir/%")).use { c ->
                while (c.moveToNext()) {
                    val rowId = c.getLong(0)
                    val oldPath = c.getString(1) ?: continue
                    val fileName = oldPath.substringAfter("/files/$subdir/").takeIf { it.isNotBlank() && '/' !in it } ?: continue
                    db.execSQL("UPDATE `$table` SET $column = ? WHERE rowid = ?", arrayOf("$base/$subdir/$fileName", rowId))
                }
            }
        }
        fun rewriteRootPrefix(table: String, column: String, prefix: String) {
            db.query("SELECT rowid, $column FROM `$table` WHERE $column LIKE ?", arrayOf("%/files/$prefix%")).use { c ->
                while (c.moveToNext()) {
                    val rowId = c.getLong(0)
                    val oldPath = c.getString(1) ?: continue
                    val fileName = oldPath.substringAfterLast("/files/").takeIf { it.isNotBlank() && '/' !in it } ?: continue
                    db.execSQL("UPDATE `$table` SET $column = ? WHERE rowid = ?", arrayOf("$base/$fileName", rowId))
                }
            }
        }
        runCatching {
            rewriteSubdir("product_images", "imageUri", "product_images")
            rewriteSubdir("import_vault_attachments", "filePath", "vault_attachments")
            rewriteRootPrefix("customers", "profileImagePath", "avatar_")
            rewriteRootPrefix("business_profiles", "logoPath", "company_logo")
            rewriteRootPrefix("business_profiles", "logoPath", "avatar_")
        }
    }
}
