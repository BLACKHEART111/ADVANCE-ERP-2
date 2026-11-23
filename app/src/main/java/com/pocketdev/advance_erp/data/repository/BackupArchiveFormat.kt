package com.pocketdev.advance_erp.data.repository

import java.io.File
import java.util.UUID

/** New backups/exports are plain archives. Encryption is read-only legacy compatibility. */
internal object BackupArchiveFormat {
    const val LEGACY_KEY_UNAVAILABLE = "পুরোনো এনক্রিপ্টেড ব্যাকআপের সংরক্ষিত key পাওয়া যায়নি। আগে এটিকে সাধারণ ZIP-এ রূপান্তর করতে হবে। (Legacy encrypted backup key unavailable; convert it to a plain ZIP first.)"

    fun isZip(file: File): Boolean = file.inputStream().use { input ->
        val header = ByteArray(4)
        input.read(header) == 4 && header[0] == 0x50.toByte() && header[1] == 0x4b.toByte() &&
            ((header[2] == 3.toByte() && header[3] == 4.toByte()) ||
                (header[2] == 5.toByte() && header[3] == 6.toByte()))
    }

    fun createPlainShareCopy(source: File, cacheDir: File, legacyKey: String?): File {
        require(source.isFile) { "Backup file does not exist" }
        val encrypted = BackupEncryption.isEncrypted(source)
        val extension = if (encrypted || isZip(source)) "zip" else "db"
        val output = File(cacheDir, "AdvanceERP_Export_${UUID.randomUUID()}.$extension")
        try {
            if (encrypted) {
                require(!legacyKey.isNullOrBlank()) { LEGACY_KEY_UNAVAILABLE }
                val key = legacyKey.toCharArray()
                try { BackupEncryption.decrypt(source, output, key) } finally { key.fill('\u0000') }
            } else {
                source.copyTo(output, overwrite = false)
            }
            return output
        } catch (e: Exception) {
            output.delete()
            throw e
        }
    }
}
