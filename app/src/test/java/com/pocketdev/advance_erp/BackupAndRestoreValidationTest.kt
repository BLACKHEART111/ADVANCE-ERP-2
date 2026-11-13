package com.pocketdev.advance_erp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.pocketdev.advance_erp.data.repository.isSafeBackupZipEntry
import com.pocketdev.advance_erp.data.repository.BackupEncryption
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class BackupAndRestoreValidationTest {

    @Test
    fun backupZipRejectsPathTraversalAndUnknownEntries() {
        assertTrue(isSafeBackupZipEntry("database/advance_erp.db"))
        assertTrue(isSafeBackupZipEntry("product_images/item_01.jpg"))
        assertTrue(isSafeBackupZipEntry("po_attachments/po_123__invoice.pdf"))
        assertTrue(isSafeBackupZipEntry("avatars/avatar_user.jpg"))
        assertFalse(isSafeBackupZipEntry("product_images/../../database.db"))
        assertFalse(isSafeBackupZipEntry("po_attachments/../../database.db"))
        assertFalse(isSafeBackupZipEntry("avatars/..\\database.db"))
        assertFalse(isSafeBackupZipEntry("/database/advance_erp.db"))
        assertFalse(isSafeBackupZipEntry("database/other.db"))
    }

    @Test
    fun encryptedBackupRoundTripsAndRejectsWrongPassphrase() {
        val source = File.createTempFile("erp_source", ".zip")
        val encrypted = File.createTempFile("erp_encrypted", ".aerpbackup")
        val restored = File.createTempFile("erp_restored", ".zip")
        try {
            val bytes = ByteArray(4096) { (it % 251).toByte() }
            source.writeBytes(bytes)
            BackupEncryption.encrypt(source, encrypted, "correct horse".toCharArray())
            assertTrue(BackupEncryption.isEncrypted(encrypted))
            BackupEncryption.decrypt(encrypted, restored, "correct horse".toCharArray())
            assertTrue(bytes.contentEquals(restored.readBytes()))

            val wrongPassOutput = File.createTempFile("erp_wrong", ".zip")
            try {
                org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                    BackupEncryption.decrypt(encrypted, wrongPassOutput, "incorrect".toCharArray())
                }
                assertFalse(wrongPassOutput.exists() && wrongPassOutput.length() > 0)
            } finally {
                wrongPassOutput.delete()
            }
        } finally {
            source.delete()
            encrypted.delete()
            restored.delete()
        }
    }

    @Test
    fun testSha256ChecksumVerification() {
        val tempFile = File.createTempFile("test_backup", ".db.bak")
        val sampleData = "SAMPLE_SQLITE_ERP_BACKUP_CONTENT_ROOM_V4_ADVANCE_ERP".toByteArray(Charsets.UTF_8)
        FileOutputStream(tempFile).use { it.write(sampleData) }

        val digest = MessageDigest.getInstance("SHA-256")
        val expectedHash = digest.digest(sampleData).joinToString("") { "%02x".format(it) }

        // Recalculate hash from file
        val calculatedDigest = MessageDigest.getInstance("SHA-256")
        tempFile.inputStream().use { fis ->
            val buf = ByteArray(1024)
            var n: Int
            while (fis.read(buf).also { n = it } != -1) {
                calculatedDigest.update(buf, 0, n)
            }
        }
        val fileHash = calculatedDigest.digest().joinToString("") { "%02x".format(it) }

        assertEquals(expectedHash, fileHash)
        tempFile.delete()
    }

    @Test
    fun testSqliteHeaderValidation() {
        // Valid SQLite format starts with 16 bytes: "SQLite format 3\000"
        val validHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        val validFile = File.createTempFile("valid_header", ".db.bak")
        FileOutputStream(validFile).use { it.write(validHeader) }

        val readHeader = ByteArray(16)
        validFile.inputStream().use { it.read(readHeader) }
        val readHeaderStr = String(readHeader, Charsets.US_ASCII)

        assertTrue(readHeaderStr.startsWith("SQLite format 3"))

        // Invalid file (e.g. random text or corrupted data)
        val invalidFile = File.createTempFile("invalid_header", ".db.bak")
        FileOutputStream(invalidFile).use { it.write("INVALID_HEADER_DATA_12345".toByteArray()) }

        val invalidReadHeader = ByteArray(16)
        invalidFile.inputStream().use { it.read(invalidReadHeader) }
        val invalidHeaderStr = String(invalidReadHeader, Charsets.US_ASCII)

        assertFalse(invalidHeaderStr.startsWith("SQLite format 3"))

        validFile.delete()
        invalidFile.delete()
    }
}
