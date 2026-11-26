package com.pocketdev.advance_erp.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile

/** Restore is applied before any Room/WorkManager initialization in a fresh process. */
object StagedRestore {
    private val media = listOf("product_images", "po_attachments", "vault_attachments")
    fun stage(context: Context, source: File, zip: Boolean) {
        val root = File(context.filesDir, "restore_staging")
        require(!File(root, "ready").exists()) { "A restore is already staged. Restart the app first." }
        root.deleteRecursively(); check(root.mkdirs())
        try {
            val dbFile = File(root, "database.db")
            if(zip) ZipFile(source).use { archive ->
                val dbEntry = requireNotNull(archive.getEntry("database/advance_erp.db")) { "Database missing" }
                archive.getInputStream(dbEntry).use { input -> dbFile.outputStream().use { input.copyTo(it) } }
                val entries = archive.entries()
                var total = dbFile.length()
                while(entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val dir = entry.name.substringBefore('/')
                    if(dir !in media && dir != "avatars") continue
                    require(!entry.isDirectory && entry.name.count { it == '/' } == 1) { "Unsafe restore path" }
                    val name = entry.name.substringAfter('/')
                    require(name.isNotBlank() && name != "." && name != ".." && !name.contains('\\')) { "Unsafe restore path" }
                    val target = File(File(root, dir).apply { mkdirs() }, name)
                    archive.getInputStream(entry).use { input -> target.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        while(true) { val n = input.read(buffer); if(n < 0) break; total += n; require(total <= 2L * 1024 * 1024 * 1024) { "Restore exceeds 2 GiB" }; output.write(buffer, 0, n) }
                    } }
                }
            } else source.copyTo(dbFile)
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                require(db.version in 1..AdvanceErpDatabase.DATABASE_VERSION) { "Unsupported backup schema" }
                db.rawQuery("PRAGMA quick_check", null).use { require(it.moveToFirst() && it.getString(0) == "ok") { "Corrupt backup" } }
            }
            // Validate and migrate an isolated staged copy; never open/replace the live Room database here.
            val staged = Room.databaseBuilder(context, AdvanceErpDatabase::class.java, dbFile.absolutePath)
                .addMigrations(DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3, DatabaseMigrations.MIGRATION_3_4,
                    DatabaseMigrations.MIGRATION_4_5, DatabaseMigrations.MIGRATION_5_6, DatabaseMigrations.MIGRATION_6_7,
                    DatabaseMigrations.MIGRATION_7_8, DatabaseMigrations.MIGRATION_8_9, DatabaseMigrations.MIGRATION_9_10,
                    DatabaseMigrations.MIGRATION_10_11, DatabaseMigrations.MIGRATION_11_12, DatabaseMigrations.MIGRATION_12_13,
                    DatabaseMigrations.MIGRATION_13_14, DatabaseMigrations.MIGRATION_14_15).build()
            try { staged.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() } }
            finally { staged.close() }
            File(root, "ready").writeText(if(zip) "ZIP" else "DATABASE")
        } catch(e: Exception) { root.deleteRecursively(); throw e }
    }
    /** Called only from Application.attachBaseContext, before provider initialization. */
    fun applyPending(context: Context) {
        val root = File(context.filesDir, "restore_staging")
        if(!File(root, "ready").exists()) return
        val live = context.getDatabasePath(AdvanceErpDatabase.DATABASE_NAME)
        val rollback = File(root, "rollback")
        val marker = File(root, "applying")
        if(marker.exists() && marker.readText() == "committed") { root.deleteRecursively(); return }
        fun restorePrevious() {
            File(rollback, "database.db").takeIf { it.exists() }?.copyTo(live, overwrite = true)
            File(live.path + "-wal").delete(); File(live.path + "-shm").delete()
            media.forEach { name ->
                File(context.filesDir, name).deleteRecursively()
                File(rollback, name).takeIf { it.exists() }?.copyRecursively(File(context.filesDir, name), overwrite = true)
            }
            File(root, "avatars").listFiles()?.forEach { incoming ->
                val old = File(File(rollback, "avatars"), incoming.name)
                val target = File(context.filesDir, incoming.name)
                if(old.exists()) old.copyTo(target, overwrite = true) else target.delete()
            }
        }
        try {
            if(marker.exists()) { restorePrevious(); error("Interrupted restore rolled back") }
            rollback.mkdirs()
            if(live.exists()) {
                SQLiteDatabase.openDatabase(live.path, null, SQLiteDatabase.OPEN_READWRITE).use { db -> db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() } }
                live.copyTo(File(rollback, "database.db"), overwrite = true)
            }
            media.forEach { name -> File(context.filesDir, name).takeIf { it.exists() }?.copyRecursively(File(rollback, name), overwrite = true) }
            File(root, "avatars").listFiles()?.forEach { file -> File(context.filesDir, file.name).takeIf { it.exists() }?.let { old ->
                val dir = File(rollback, "avatars").apply { mkdirs() }; old.copyTo(File(dir, old.name), overwrite = true)
            } }
            marker.writeText("prepared")
            File(live.path + "-wal").delete(); File(live.path + "-shm").delete()
            File(root, "database.db").copyTo(live, overwrite = true)
            if(File(root, "ready").readText() == "ZIP") media.forEach { name ->
                File(context.filesDir, name).deleteRecursively()
                File(root, name).takeIf { it.exists() }?.copyRecursively(File(context.filesDir, name), overwrite = true)
            }
            File(root, "avatars").listFiles()?.forEach { it.copyTo(File(context.filesDir, it.name), overwrite = true) }
            SQLiteDatabase.openDatabase(live.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.execSQL("INSERT INTO audit_logs (id, recordType, recordId, action, details, operator, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(UUID.randomUUID().toString(), "RESTORE", "staged", "RESTORE", "Validated staged backup restored at startup", "SYSTEM", System.currentTimeMillis()))
            }
            val safety = File(context.filesDir, "safety_backups/restore_${System.currentTimeMillis()}")
            safety.parentFile?.mkdirs(); check(rollback.copyRecursively(safety, overwrite = false))
            marker.writeText("committed")
            root.deleteRecursively()
            File(context.filesDir, "restore_result.txt").writeText("Restore completed / রিস্টোর সম্পন্ন")
        } catch(e: Exception) {
            if(marker.exists() && rollback.exists()) restorePrevious()
            File(context.filesDir, "restore_result.txt").writeText("Restore failed; previous data retained: ${e.message}")
            File(root, "ready").delete(); marker.delete()
        }
    }
}
