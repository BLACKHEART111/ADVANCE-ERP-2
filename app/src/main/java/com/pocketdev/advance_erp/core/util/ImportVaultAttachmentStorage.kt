package com.pocketdev.advance_erp.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.pocketdev.advance_erp.core.security.ImportVaultAccessGuard
import com.pocketdev.advance_erp.core.security.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.UUID

data class StoredVaultFileInfo(
    val absolutePath: String,
    val fileName: String,
    val mimeType: String,
    val fileSizeBytes: Long
)

/**
 * Pure-JVM file store helpers for Import Vault managed attachments and immutable snapshots.
 * Safe to invoke directly inside JVM unit tests without Robolectric.
 */
object ImportVaultAttachmentFileStore {

    const val VAULT_ATTACHMENTS_DIR_NAME = "vault_attachments"
    const val VAULT_EXPORTS_CACHE_DIR_NAME = "vault_exports"

    fun resolveManagedAttachmentsDir(filesDir: File): File =
        File(filesDir, VAULT_ATTACHMENTS_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }

    fun resolveExportCacheDir(cacheDir: File): File =
        File(cacheDir, VAULT_EXPORTS_CACHE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }

    fun sanitizeFileSegment(raw: String, fallback: String = "document"): String {
        val cleaned = raw
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_', '.')
        return cleaned.ifBlank { fallback }.take(48)
    }

    fun detectMimeType(fileName: String, explicitMimeType: String? = null): String {
        val cleanedExplicit = explicitMimeType?.trim()?.takeIf { it.isNotBlank() && it != "application/octet-stream" }
        if (cleanedExplicit != null) return cleanedExplicit
        return when (fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
            "pdf" -> "application/pdf"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "csv" -> "text/csv"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    fun defaultExtensionForMime(mimeType: String?): String = when (mimeType?.lowercase(Locale.ROOT)) {
        "application/pdf" -> "pdf"
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "text/csv" -> "csv"
        "text/plain" -> "txt"
        else -> "bin"
    }

    fun isImage(filePath: String?, mimeType: String? = null): Boolean {
        if (mimeType?.startsWith("image/", ignoreCase = true) == true) return true
        val ext = filePath.orEmpty().substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")
    }

    fun isPdf(filePath: String?, mimeType: String? = null): Boolean {
        if (mimeType.equals("application/pdf", ignoreCase = true)) return true
        return filePath.orEmpty().substringAfterLast('.', "").equals("pdf", ignoreCase = true)
    }

    fun saveStreamAttachment(
        filesDir: File,
        inputStream: InputStream,
        preferredName: String,
        scopePrefix: String,
        mimeType: String? = null,
        role: UserRole = UserRole.OWNER_ADMIN
    ): StoredVaultFileInfo {
        ImportVaultAccessGuard.requireVaultAccess(role)
        val rawName = preferredName.trim().ifBlank { "document.bin" }
        val rawExt = rawName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val resolvedMime = detectMimeType(rawName, mimeType)
        val finalExt = rawExt.ifBlank { defaultExtensionForMime(resolvedMime) }
        val baseTitle = sanitizeFileSegment(rawName.substringBeforeLast('.'), "document")
        val prefix = sanitizeFileSegment(scopePrefix, "vault")
        val destFile = File(
            resolveManagedAttachmentsDir(filesDir),
            "vault_${prefix}__${UUID.randomUUID()}__${baseTitle}.${finalExt}"
        )

        val copiedBytes = inputStream.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }

        if (copiedBytes <= 0L || !destFile.exists()) {
            destFile.delete()
            throw IllegalStateException("Selected vault attachment file is empty")
        }

        return StoredVaultFileInfo(
            absolutePath = destFile.absolutePath,
            fileName = destFile.name,
            mimeType = detectMimeType(destFile.name, resolvedMime),
            fileSizeBytes = destFile.length()
        )
    }

    fun saveImmutableSnapshotFile(
        filesDir: File,
        sourceFile: File,
        preferredName: String,
        scopePrefix: String,
        mimeType: String? = null,
        role: UserRole = UserRole.OWNER_ADMIN
    ): StoredVaultFileInfo {
        ImportVaultAccessGuard.requireVaultAccess(role)
        require(sourceFile.exists() && sourceFile.isFile && sourceFile.length() > 0L) {
            "Snapshot source file does not exist or is empty: ${sourceFile.absolutePath}"
        }
        val rawName = preferredName.trim().ifBlank { sourceFile.name }
        val ext = rawName.substringAfterLast('.', "").ifBlank { sourceFile.extension }.ifBlank { "bin" }
            .lowercase(Locale.ROOT)
        val baseTitle = sanitizeFileSegment(rawName.substringBeforeLast('.'), "snapshot")
        val prefix = sanitizeFileSegment(scopePrefix, "vault")
        val destFile = File(
            resolveManagedAttachmentsDir(filesDir),
            "snapshot_${prefix}__${UUID.randomUUID()}__${baseTitle}.${ext}"
        )
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        return StoredVaultFileInfo(
            absolutePath = destFile.absolutePath,
            fileName = destFile.name,
            mimeType = detectMimeType(destFile.name, mimeType),
            fileSizeBytes = destFile.length()
        )
    }

    fun isSafeManagedVaultAttachmentPath(filesDir: File, filePath: String?): Boolean {
        val cleanPath = filePath?.trim()?.removePrefix("file://").orEmpty()
        if (cleanPath.isBlank()) return false
        val managedDir = runCatching { resolveManagedAttachmentsDir(filesDir).canonicalFile }.getOrNull() ?: return false
        val canonicalTarget = runCatching { File(cleanPath).canonicalFile }.getOrNull() ?: return false
        return canonicalTarget.parentFile == managedDir
    }

    /**
     * Deletes ONLY the managed local file inside filesDir/vault_attachments/.
     * Underlying ERP records, PO attachments, and product images are strictly untouched.
     */
    fun deleteManagedAttachmentFile(
        filesDir: File,
        filePath: String?,
        role: UserRole = UserRole.OWNER_ADMIN
    ): Boolean {
        ImportVaultAccessGuard.requireVaultAccess(role)
        if (!isSafeManagedVaultAttachmentPath(filesDir, filePath)) return false
        val cleanPath = filePath!!.trim().removePrefix("file://")
        val canonicalTarget = runCatching { File(cleanPath).canonicalFile }.getOrNull() ?: return false
        return if (canonicalTarget.exists() && canonicalTarget.isFile) {
            canonicalTarget.delete()
        } else {
            false
        }
    }

    fun clearManagedAttachmentsDir(filesDir: File): Int {
        val dir = File(filesDir, VAULT_ATTACHMENTS_DIR_NAME)
        if (!dir.exists() || !dir.isDirectory) return 0
        var deleted = 0
        dir.listFiles()?.filter { it.isFile }?.forEach { file ->
            if (file.delete()) deleted++
        }
        return deleted
    }
}

/**
 * Android-aware storage, preview, and sharing helper for Import Vault local documents
 * (photos, PDFs, spreadsheets, and immutable saved snapshots).
 */
object ImportVaultAttachmentStorage {

    private const val MAX_IMAGE_DIMENSION = 1600
    private const val JPEG_QUALITY = 85

    fun getAttachmentsDir(context: Context): File =
        ImportVaultAttachmentFileStore.resolveManagedAttachmentsDir(context.filesDir)

    fun getExportCacheDir(context: Context): File =
        ImportVaultAttachmentFileStore.resolveExportCacheDir(context.cacheDir)

    suspend fun saveFromUri(
        context: Context,
        sourceUri: Uri,
        preferredName: String? = null,
        scopePrefix: String = "vault",
        role: UserRole = UserRole.OWNER_ADMIN
    ): StoredVaultFileInfo = withContext(Dispatchers.IO) {
        ImportVaultAccessGuard.requireVaultAccess(role)
        val rawName = preferredName?.takeIf { it.isNotBlank() } ?: resolveDisplayName(context, sourceUri)
        val rawMime = context.contentResolver.getType(sourceUri)
        val extFromName = rawName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val extFromMime = rawMime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }?.lowercase(Locale.ROOT)
        val mime = ImportVaultAttachmentFileStore.detectMimeType(
            fileName = if (extFromName.isNotBlank()) rawName else "$rawName.${extFromMime ?: "bin"}",
            explicitMimeType = rawMime
        )

        if (mime.startsWith("image/", ignoreCase = true) && mime != "image/gif") {
            val optimizedBytes = tryCompressAndOrientImage(context, sourceUri)
            if (optimizedBytes != null && optimizedBytes.isNotEmpty()) {
                val baseName = rawName.substringBeforeLast('.').ifBlank { "photo" } + ".jpg"
                return@withContext ImportVaultAttachmentFileStore.saveStreamAttachment(
                    filesDir = context.filesDir,
                    inputStream = optimizedBytes.inputStream(),
                    preferredName = baseName,
                    scopePrefix = scopePrefix,
                    mimeType = "image/jpeg",
                    role = role
                )
            }
        }

        val input = context.contentResolver.openInputStream(sourceUri)
            ?: throw IllegalStateException("Could not open selected document URI")
        ImportVaultAttachmentFileStore.saveStreamAttachment(
            filesDir = context.filesDir,
            inputStream = input,
            preferredName = rawName,
            scopePrefix = scopePrefix,
            mimeType = mime,
            role = role
        )
    }

    suspend fun saveImmutableSnapshot(
        context: Context,
        sourceFile: File,
        preferredName: String,
        scopePrefix: String,
        mimeType: String? = null,
        role: UserRole = UserRole.OWNER_ADMIN
    ): StoredVaultFileInfo = withContext(Dispatchers.IO) {
        ImportVaultAttachmentFileStore.saveImmutableSnapshotFile(
            filesDir = context.filesDir,
            sourceFile = sourceFile,
            preferredName = preferredName,
            scopePrefix = scopePrefix,
            mimeType = mimeType,
            role = role
        )
    }

    fun deleteManagedFileIfExists(
        context: Context,
        filePath: String?,
        role: UserRole = UserRole.OWNER_ADMIN
    ): Boolean = ImportVaultAttachmentFileStore.deleteManagedAttachmentFile(
        filesDir = context.filesDir,
        filePath = filePath,
        role = role
    )

    fun openVaultFile(
        context: Context,
        filePath: String,
        isBangla: Boolean,
        role: UserRole = UserRole.OWNER_ADMIN
    ) {
        ImportVaultAccessGuard.requireVaultAccess(role)
        val cleanPath = filePath.trim().removePrefix("file://")
        val file = File(cleanPath)
        if (!file.exists() || !file.isFile) {
            Toast.makeText(
                context,
                if (isBangla) "ফাইলটি ডিভাইসে পাওয়া যায়নি" else "Attachment file not found on device",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val mime = ImportVaultAttachmentFileStore.detectMimeType(file.name)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(
                intent,
                if (isBangla) "ডকুমেন্ট ওপেন করুন" else "Open Vault Document"
            ).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                if (isBangla) "এই ফাইল খোলার জন্য উপযুক্ত অ্যাপ নেই" else "No app available to open this file",
                Toast.LENGTH_SHORT
            ).show()
        } catch (t: Throwable) {
            Toast.makeText(
                context,
                if (isBangla) "ডকুমেন্ট খোলা যায়নি: ${t.message}" else "Failed to open document: ${t.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun shareVaultFile(
        context: Context,
        filePath: String,
        mimeType: String? = null,
        isBangla: Boolean,
        role: UserRole = UserRole.OWNER_ADMIN
    ) {
        ImportVaultAccessGuard.requireVaultAccess(role)
        val cleanPath = filePath.trim().removePrefix("file://")
        val file = File(cleanPath)
        if (!file.exists() || !file.isFile) {
            Toast.makeText(
                context,
                if (isBangla) "শেয়ার করার মতো ফাইল পাওয়া যায়নি" else "File not found to share",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val resolvedMime = ImportVaultAttachmentFileStore.detectMimeType(file.name, mimeType)
        val title = if (isBangla) "আর্কাইভ ডকুমেন্ট শেয়ার করুন" else "Share Vault Document"
        ControlCenterShareService.shareFile(
            context = context,
            file = file,
            mimeType = resolvedMime,
            chooserTitle = title
        )
    }

    private fun tryCompressAndOrientImage(context: Context, sourceUri: Uri): ByteArray? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sampleSize = 1
            while (bounds.outWidth / sampleSize > MAX_IMAGE_DIMENSION * 2 ||
                bounds.outHeight / sampleSize > MAX_IMAGE_DIMENSION * 2
            ) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            var bitmap = context.contentResolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            val rotation = readExifRotation(context, sourceUri)
            if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) {
                    bitmap.recycle()
                    bitmap = rotated
                }
            }

            val maxSide = maxOf(bitmap.width, bitmap.height)
            if (maxSide > MAX_IMAGE_DIMENSION) {
                val ratio = MAX_IMAGE_DIMENSION.toFloat() / maxSide.toFloat()
                val scaled = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt().coerceAtLeast(1),
                    (bitmap.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
                if (scaled != bitmap) {
                    bitmap.recycle()
                    bitmap = scaled
                }
            }

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
            bitmap.recycle()
            baos.toByteArray()
        } catch (_: Throwable) {
            null
        }
    }

    private fun readExifRotation(context: Context, uri: Uri): Float {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (_: Throwable) {
            0f
        }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        if (uri.scheme == "file") {
            return File(uri.path.orEmpty()).name.ifBlank { "attachment" }
        }
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        } catch (_: Exception) {
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "attachment" } ?: "attachment"
    }
}
