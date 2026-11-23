package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.repository.BackupArchiveFormat
import com.pocketdev.advance_erp.data.repository.BackupEncryption
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class PlainBackupExportTest {
    @get:Rule val temp = TemporaryFolder()
    private fun zip(name: String = "backup.zip"): File = temp.newFile(name).also { file ->
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("database/advance_erp.db"))
            zip.write("SQLite format 3\u0000fixture".toByteArray())
            zip.closeEntry()
        }
    }

    @Test fun sharingPlainArchiveNeverUsesPreviouslySavedPassword() {
        val source = zip()
        val exported = BackupArchiveFormat.createPlainShareCopy(source, temp.root, "old-saved-key")
        assertEquals("zip", exported.extension)
        assertFalse(BackupEncryption.isEncrypted(exported))
        assertArrayEquals(source.readBytes(), exported.readBytes())
        ZipFile(exported).use { assertNotNull(it.getEntry("database/advance_erp.db")) }
    }

    @Test fun legacyEncryptedArchiveUsesSavedKeyButExportIsPlainZip() {
        val source = zip()
        val encrypted = temp.newFile("old.zip.enc")
        BackupEncryption.encrypt(source, encrypted, "legacy-key".toCharArray())
        val original = encrypted.readBytes()
        val exported = BackupArchiveFormat.createPlainShareCopy(encrypted, temp.root, "legacy-key")
        assertTrue(BackupArchiveFormat.isZip(exported))
        assertFalse(BackupEncryption.isEncrypted(exported))
        assertArrayEquals(source.readBytes(), exported.readBytes())
        assertArrayEquals(original, encrypted.readBytes())
    }

    @Test fun missingLegacyKeyDoesNotCreateOrAlterBackupFiles() {
        val encrypted = temp.newFile("old.enc")
        BackupEncryption.encrypt(zip(), encrypted, "legacy-key".toCharArray())
        val filesBefore = temp.root.listFiles()!!.map { it.name }.toSet()
        try {
            BackupArchiveFormat.createPlainShareCopy(encrypted, temp.root, null)
            fail("An encrypted legacy archive cannot be read without its existing key")
        } catch (expected: IllegalArgumentException) {
            assertEquals(BackupArchiveFormat.LEGACY_KEY_UNAVAILABLE, expected.message)
        }
        assertEquals(filesBefore, temp.root.listFiles()!!.map { it.name }.toSet())
    }

    @Test fun archiveDetectionUsesContentRatherThanFilename() {
        assertTrue(BackupArchiveFormat.isZip(zip("renamed-backup.bin")))
        val notZip = temp.newFile("incorrect.zip").also { it.writeText("SQLite format 3\u0000fixture") }
        assertFalse(BackupArchiveFormat.isZip(notZip))
        val exported = BackupArchiveFormat.createPlainShareCopy(notZip, temp.root, null)
        assertEquals("db", exported.extension)
        assertArrayEquals(notZip.readBytes(), exported.readBytes())
    }
}
