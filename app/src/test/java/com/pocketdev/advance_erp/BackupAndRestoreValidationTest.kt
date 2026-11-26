package com.pocketdev.advance_erp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.pocketdev.advance_erp.core.database.BackupArchiveContract
import com.pocketdev.advance_erp.core.database.PortableBackupPreferences
import com.pocketdev.advance_erp.data.repository.isSafeBackupZipEntry
import com.pocketdev.advance_erp.data.repository.verifyBackupZipEntriesIntegrity
import com.pocketdev.advance_erp.data.repository.BackupEncryption
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupAndRestoreValidationTest {

    @Test
    fun backupZipRejectsPathTraversalAndUnknownEntries() {
        assertTrue(isSafeBackupZipEntry("database/advance_erp.db"))
        assertTrue(isSafeBackupZipEntry("metadata.json"))
        assertTrue(isSafeBackupZipEntry("preferences.json"))
        assertTrue(isSafeBackupZipEntry("control_center_presets.json"))
        assertTrue(isSafeBackupZipEntry("product_images/item_01.jpg"))
        assertTrue(isSafeBackupZipEntry("po_attachments/po_123__invoice.pdf"))
        assertTrue(isSafeBackupZipEntry("avatars/avatar_user.jpg"))
        assertTrue(isSafeBackupZipEntry("vault_attachments/"))
        assertTrue(isSafeBackupZipEntry("vault_attachments/vault_shp1__inv.pdf"))
        assertTrue(isSafeBackupZipEntry("customer_photos/cust_1.jpg"))
        assertTrue(isSafeBackupZipEntry("company_logo/company_logo_1.png"))
        assertTrue(isSafeBackupZipEntry("company_signature/authorized_signature.png"))
        assertFalse(isSafeBackupZipEntry("product_images/../../database.db"))
        assertFalse(isSafeBackupZipEntry("po_attachments/../../database.db"))
        assertFalse(isSafeBackupZipEntry("avatars/..\\database.db"))
        assertFalse(isSafeBackupZipEntry("vault_attachments/../../advance_erp.db"))
        assertFalse(isSafeBackupZipEntry("vault_attachments/..\\advance_erp.db"))
        assertFalse(isSafeBackupZipEntry("customer_photos/../../advance_erp.db"))
        assertFalse(isSafeBackupZipEntry("company_logo/nested/logo.png"))
        assertFalse(isSafeBackupZipEntry("vault_attachments/nested/file.pdf"))
        assertFalse(isSafeBackupZipEntry("/database/advance_erp.db"))
        assertFalse(isSafeBackupZipEntry("database/other.db"))
        assertFalse(isSafeBackupZipEntry("unknown_root.json"))
    }

    @Test
    fun backupArchiveContractRewritesManagedAndCsvPathsSafely() {
        val oldFilesDir = "/data/user/0/com.pocketdev.advance_erp2/files"
        val targetFilesDir = "/data/user/10/com.pocketdev.advance_erp2.qa/files"

        assertEquals(
            "customer_photos/cust_1.jpg",
            BackupArchiveContract.extractManagedRelativePath(
                "file://$oldFilesDir/customer_photos/cust_1.jpg",
                oldFilesDir
            )
        )
        assertEquals(
            "avatar_123.jpg",
            BackupArchiveContract.extractManagedRelativePath(
                "$oldFilesDir/avatar_123.jpg",
                oldFilesDir
            )
        )
        assertNull(
            BackupArchiveContract.extractManagedRelativePath(
                "content://media/external/images/media/99",
                oldFilesDir
            )
        )
        assertNull(
            BackupArchiveContract.extractManagedRelativePath(
                "$oldFilesDir/customer_photos/../secret.db",
                oldFilesDir
            )
        )

        assertEquals(
            "$targetFilesDir/company_logo/logo_1.png",
            BackupArchiveContract.anchorManagedRelativePath("company_logo/logo_1.png", targetFilesDir)
        )
        assertEquals(
            "$targetFilesDir/avatar_123.jpg",
            BackupArchiveContract.anchorManagedRelativePath("avatar_123.jpg", targetFilesDir)
        )
        assertNull(BackupArchiveContract.anchorManagedRelativePath("../evil.db", targetFilesDir))

        assertEquals(
            "$targetFilesDir/customer_photos/cust_1.jpg",
            BackupArchiveContract.rewriteManagedSinglePath(
                "$oldFilesDir/customer_photos/cust_1.jpg",
                targetFilesDir,
                "customer_photos"
            )
        )
        assertEquals(
            "$targetFilesDir/avatar_old.jpg",
            BackupArchiveContract.rewriteManagedRootPrefixPath(
                "$oldFilesDir/avatar_old.jpg",
                targetFilesDir,
                "avatar_"
            )
        )
        assertEquals(
            "$targetFilesDir/po_attachments/po_doc.pdf",
            BackupArchiveContract.rewriteManagedAnySubdirPath(
                "file://$oldFilesDir/po_attachments/po_doc.pdf",
                targetFilesDir
            )
        )

        val csvInput = "file://$oldFilesDir/po_attachments/po_1__inv.pdf, https://example.com/inv, $oldFilesDir/po_attachments/po_1__inv.pdf, $oldFilesDir/po_attachments/po_2__pack.jpg"
        assertEquals(
            "$targetFilesDir/po_attachments/po_1__inv.pdf,https://example.com/inv,$targetFilesDir/po_attachments/po_2__pack.jpg",
            BackupArchiveContract.rewriteManagedCsvPaths(csvInput, targetFilesDir, "po_attachments")
        )
        assertNull(
            BackupArchiveContract.rewriteManagedCsvPaths(
                "https://example.com/inv",
                targetFilesDir,
                "po_attachments"
            )
        )
    }

    @Test
    fun portableBackupPreferencesJsonRoundTripsSpecialCharacters() {
        val original = PortableBackupPreferences(
            appTheme = "dark",
            appLanguage = "bn",
            appCurrency = "USD",
            userName = "অ্যাডভান্স \"ট্রেডার্স\" \\ BD\nLine2",
            userRole = "OWNER_ADMIN",
            profilePhotoRelativePath = "avatar_999.jpg",
            companyLogoRelativePath = "company_logo/company_logo_10.png",
            fridayAutoBackupEnabled = false,
            legacyMode = false
        )
        val encoded = BackupArchiveContract.encodePreferencesJson(original)
        val decoded = BackupArchiveContract.decodePreferencesJson(encoded)
        assertEquals(original, decoded)

        val unknownAndNullJson = """
            {
              "appTheme": "light",
              "unknownNested": {"a": [1, true, null, "x"]},
              "userName": null,
              "fridayAutoBackupEnabled": true
            }
        """.trimIndent()
        val parsed = BackupArchiveContract.decodePreferencesJson(unknownAndNullJson)
        assertEquals("light", parsed.appTheme)
        assertNull(parsed.userName)
        assertEquals(true, parsed.fridayAutoBackupEnabled)
        assertFalse(parsed.legacyMode)
    }

    @Test
    fun v2ComprehensiveBackupZipIntegrityVerificationPassesWithAllModules() {
        val tmpDir = Files.createTempDirectory("v2_backup_integrity_").toFile()
        try {
            val zipFile = File(tmpDir, "full_v2_backup.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                fun writeEntry(name: String, content: ByteArray) {
                    zos.putNextEntry(ZipEntry(name))
                    zos.write(content)
                    zos.closeEntry()
                }
                writeEntry("database/advance_erp.db", "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII))
                writeEntry("product_images/img_1.jpg", byteArrayOf(1, 2, 3))
                writeEntry("po_attachments/po_1__inv.pdf", byteArrayOf(4, 5, 6))
                writeEntry("vault_attachments/vault_1__doc.pdf", byteArrayOf(7, 8, 9))
                writeEntry("customer_photos/cust_1.jpg", byteArrayOf(10, 11, 12))
                writeEntry("company_logo/company_logo_1.png", byteArrayOf(13, 14, 15))
                writeEntry("company_signature/authorized_signature.png", byteArrayOf(16, 17, 18))
                writeEntry("avatars/avatar_1.jpg", byteArrayOf(19, 20, 21))
                writeEntry(
                    "preferences.json",
                    BackupArchiveContract.encodePreferencesJson(
                        PortableBackupPreferences(appTheme = "dark", appLanguage = "bn")
                    ).toByteArray(Charsets.UTF_8)
                )
                writeEntry("control_center_presets.json", """{"presetsListJson":"[]"}""".toByteArray(Charsets.UTF_8))
                writeEntry("metadata.json", """{"backupFormatVersion":2}""".toByteArray(Charsets.UTF_8))
            }
            assertTrue(verifyBackupZipEntriesIntegrity(zipFile))
        } finally {
            tmpDir.deleteRecursively()
        }
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
