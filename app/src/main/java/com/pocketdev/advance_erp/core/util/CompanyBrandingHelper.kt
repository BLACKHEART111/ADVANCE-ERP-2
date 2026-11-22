package com.pocketdev.advance_erp.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import java.io.File
import java.io.FileOutputStream

/**
 * Shared branding helper for Company Logo (uploaded in Settings) and
 * Authorized Signature (drawn on mobile in Settings), reused across PDF generators.
 */
object CompanyBrandingHelper {

    private const val SIGNATURE_DIR = "company_signature"
    private const val SIGNATURE_FILE = "authorized_signature.png"
    private const val LOGO_DIR = "company_logo"

    fun getSignatureFile(context: Context): File {
        val dir = File(context.filesDir, SIGNATURE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, SIGNATURE_FILE)
    }

    fun hasAuthorizedSignature(context: Context): Boolean {
        val file = getSignatureFile(context)
        return file.exists() && file.length() > 0L
    }

    fun saveAuthorizedSignatureBitmap(context: Context, bitmap: Bitmap): File {
        val file = getSignatureFile(context)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        return file
    }

    fun clearAuthorizedSignature(context: Context) {
        val file = getSignatureFile(context)
        if (file.exists()) {
            file.delete()
        }
    }

    fun loadAuthorizedSignatureBitmap(context: Context): Bitmap? {
        val file = getSignatureFile(context)
        if (!file.exists() || !file.isFile || file.length() <= 0L) return null
        return decodeDownsampledFile(file, maxHeightPx = 220)
    }

    fun drawAuthorizedSignature(
        context: Context,
        canvas: Canvas,
        lineStartX: Float,
        lineEndX: Float,
        lineY: Float,
        paint: Paint
    ) {
        val sig = loadAuthorizedSignatureBitmap(context) ?: return
        try {
            if (sig.width <= 0 || sig.height <= 0) return
            val slotW = (lineEndX - lineStartX - 8f).coerceAtLeast(48f)
            val aspect = sig.width.toFloat() / sig.height.toFloat()
            var targetH = 26f
            var targetW = targetH * aspect
            if (targetW > slotW) {
                targetW = slotW
                targetH = (targetW / aspect).coerceAtLeast(10f)
            }
            val left = lineStartX + ((lineEndX - lineStartX) - targetW) / 2f
            val bottom = lineY - 2f
            val top = bottom - targetH
            val dest = RectF(left, top, left + targetW, bottom)
            canvas.drawBitmap(sig, Rect(0, 0, sig.width, sig.height), dest, paint)
        } finally {
            sig.recycle()
        }
    }

    fun loadCompanyLogoBitmap(
        context: Context,
        businessProfile: BusinessProfile?,
        fallbackDrawableRes: Int = R.drawable.logo_compact_white
    ): Bitmap? {
        val explicitFile = businessProfile?.logoPath
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { File(it.removePrefix("file://")) }
            ?.takeIf { it.exists() && it.isFile && it.length() > 0L }

        val customFile = explicitFile ?: File(context.filesDir, LOGO_DIR)
            .takeIf { it.exists() && it.isDirectory }
            ?.listFiles()
            ?.filter { it.isFile && it.length() > 0L }
            ?.maxByOrNull { it.lastModified() }

        if (customFile != null) {
            val decoded = decodeDownsampledFile(customFile, maxHeightPx = 180)
            if (decoded != null) return decoded
        }

        return try {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeResource(context.resources, fallbackDrawableRes, opts)
        } catch (_: Throwable) {
            null
        }
    }

    private fun decodeDownsampledFile(file: File, maxHeightPx: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w <= 0 || h <= 0) return null
            var sampleSize = 1
            while ((h / (sampleSize * 2)) >= maxHeightPx && (w / (sampleSize * 2)) >= maxHeightPx) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } catch (_: Throwable) {
            null
        }
    }
}
