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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

enum class PoAttachmentKind {
    IMAGE,
    PDF,
    DOCUMENT,
    LINK
}

data class PoAttachmentInfo(
    val rawValue: String,
    val displayName: String,
    val kind: PoAttachmentKind,
    val localFile: File?
)

/**
 * Manages persistent local storage, metadata inspection, and viewing for Purchase Order
 * invoice photos and document attachments inside app-private storage.
 */
object PoAttachmentStorage {

    const val ATTACHMENTS_DIR_NAME = "po_attachments"
    private const val MAX_IMAGE_DIMENSION = 1600
    private const val JPEG_QUALITY = 85

    fun getAttachmentsDir(context: Context): File =
        File(context.filesDir, ATTACHMENTS_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }

    fun parseAttachments(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun serializeAttachments(items: List<String>): String? {
        val cleaned = items.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        return if (cleaned.isEmpty()) null else cleaned.joinToString(",")
    }

    fun inspectAttachment(raw: String): PoAttachmentInfo {
        val trimmed = raw.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return PoAttachmentInfo(
                rawValue = trimmed,
                displayName = trimmed,
                kind = PoAttachmentKind.LINK,
                localFile = null
            )
        }

        val cleanPath = trimmed.removePrefix("file://")
        val file = File(cleanPath)
        val rawName = file.name.ifBlank { trimmed }
        val friendlyName = rawName.substringAfter("__", rawName)
        val ext = friendlyName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val kind = when (ext) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic" -> PoAttachmentKind.IMAGE
            "pdf" -> PoAttachmentKind.PDF
            else -> PoAttachmentKind.DOCUMENT
        }
        return PoAttachmentInfo(
            rawValue = trimmed,
            displayName = friendlyName,
            kind = kind,
            localFile = file.takeIf { it.exists() && it.isFile }
        )
    }

    fun createCameraTempUri(context: Context): Pair<File, Uri> {
        val tmpDir = File(context.cacheDir, "po_camera_tmp").apply {
            if (!exists()) mkdirs()
        }
        val tmpFile = File(tmpDir, "po_cam_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tmpFile
        )
        return tmpFile to uri
    }

    suspend fun saveDocumentOrImageFromUri(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(sourceUri)?.lowercase(Locale.ROOT)
        val displayName = resolveDisplayName(context, sourceUri)
        val isImage = mimeType?.startsWith("image/") == true ||
            displayName.substringAfterLast('.', "").lowercase(Locale.ROOT) in setOf("jpg", "jpeg", "png", "webp", "heic", "bmp")

        if (isImage) {
            saveImageFromUri(context, sourceUri, displayName)
        } else {
            saveRawDocumentFromUri(context, sourceUri, displayName, mimeType)
        }
    }

    suspend fun saveImageFromUri(
        context: Context,
        sourceUri: Uri,
        preferredName: String? = null
    ): String = withContext(Dispatchers.IO) {
        val baseTitle = sanitizeFileSegment((preferredName ?: resolveDisplayName(context, sourceUri)).substringBeforeLast('.'))
            .ifBlank { "invoice_photo" }
            .take(40)
        val fileName = "po_${UUID.randomUUID()}__${baseTitle}.jpg"
        val destFile = File(getAttachmentsDir(context), fileName)

        var rotationDegrees = 0
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                val exif = ExifInterface(input)
                rotationDegrees = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (_: Exception) {
        }

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOptions)
        }

        var sampleSize = 1
        val origW = boundsOptions.outWidth
        val origH = boundsOptions.outHeight
        if (origW > MAX_IMAGE_DIMENSION || origH > MAX_IMAGE_DIMENSION) {
            val halfW = origW / 2
            val halfH = origH / 2
            while ((halfW / sampleSize) >= MAX_IMAGE_DIMENSION && (halfH / sampleSize) >= MAX_IMAGE_DIMENSION) {
                sampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        var bitmap: Bitmap? = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        }

        if (bitmap == null) {
            // Fallback to raw copy if bitmap decoding is not supported by the device codec.
            return@withContext saveRawDocumentFromUri(
                context = context,
                sourceUri = sourceUri,
                preferredName = preferredName,
                mimeType = context.contentResolver.getType(sourceUri)
            )
        }

        if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
                bitmap = rotated
            }
        }

        val curW = bitmap.width
        val curH = bitmap.height
        if (curW > MAX_IMAGE_DIMENSION || curH > MAX_IMAGE_DIMENSION) {
            val ratio = minOf(MAX_IMAGE_DIMENSION.toFloat() / curW, MAX_IMAGE_DIMENSION.toFloat() / curH)
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                (curW * ratio).toInt().coerceAtLeast(1),
                (curH * ratio).toInt().coerceAtLeast(1),
                true
            )
            if (scaled != bitmap) {
                bitmap.recycle()
                bitmap = scaled
            }
        }

        FileOutputStream(destFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        bitmap.recycle()

        destFile.absolutePath
    }

    private suspend fun saveRawDocumentFromUri(
        context: Context,
        sourceUri: Uri,
        preferredName: String?,
        mimeType: String?
    ): String = withContext(Dispatchers.IO) {
        val rawName = preferredName ?: resolveDisplayName(context, sourceUri)
        val extFromName = rawName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val extFromMime = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }?.lowercase(Locale.ROOT)
        val finalExt = extFromName.ifBlank { extFromMime ?: "bin" }
        val baseTitle = sanitizeFileSegment(rawName.substringBeforeLast('.')).ifBlank { "document" }.take(40)
        val destFile = File(getAttachmentsDir(context), "po_${UUID.randomUUID()}__${baseTitle}.${finalExt}")

        val copiedBytes = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Could not open selected file")

        if (copiedBytes <= 0L || !destFile.exists()) {
            destFile.delete()
            throw IllegalStateException("Selected file is empty")
        }

        destFile.absolutePath
    }

    fun deleteManagedFileIfExists(context: Context, rawValue: String) {
        val cleanPath = rawValue.trim().removePrefix("file://")
        val target = File(cleanPath)
        val managedDir = getAttachmentsDir(context).canonicalFile
        val canonicalTarget = runCatching { target.canonicalFile }.getOrNull() ?: return
        if (canonicalTarget.parentFile == managedDir && canonicalTarget.exists()) {
            canonicalTarget.delete()
        }
    }

    fun getMimeTypeForFile(file: File): String {
        return when (val ext = file.extension.lowercase(Locale.ROOT)) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        }
    }

    fun openAttachment(context: Context, rawValue: String, isBangla: Boolean) {
        val info = inspectAttachment(rawValue)
        if (info.kind == PoAttachmentKind.LINK) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.rawValue)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    if (isBangla) "লিংক ওপেন করা যায়নি" else "Unable to open link",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        val file = info.localFile
        if (file == null || !file.exists()) {
            Toast.makeText(
                context,
                if (isBangla) "ফাইলটি ডিভাইসে পাওয়া যায়নি" else "Attachment file not found on device",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val mime = getMimeTypeForFile(file)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(
                viewIntent,
                if (isBangla) "ফাইল ওপেন করুন" else "Open attachment"
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
                if (isBangla) "ফাইল ওপেন করা যায়নি: ${t.message}" else "Failed to open file: ${t.message}",
                Toast.LENGTH_SHORT
            ).show()
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

    private fun sanitizeFileSegment(raw: String): String {
        return raw.replace(",", "_")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
    }
}
