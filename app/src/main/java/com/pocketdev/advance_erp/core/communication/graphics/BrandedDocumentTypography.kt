package com.pocketdev.advance_erp.core.communication.graphics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.graphics.fonts.FontStyle
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.RequiresApi
import java.util.concurrent.ConcurrentHashMap

enum class DocumentFontWeight(val weight: Int) {
    REGULAR(400),
    MEDIUM(500),
    SEMI_BOLD(600),
    BOLD(700)
}

/**
 * Provides cached Roboto + embedded NotoSansBengaliUI-VF fallback typefaces
 * so Bengali conjuncts, vowel signs, and Taka symbol render deterministically offline.
 */
object BrandedTypefaceProvider {

    private val cache = ConcurrentHashMap<DocumentFontWeight, Typeface>()

    fun typeface(
        context: Context,
        weight: DocumentFontWeight = DocumentFontWeight.REGULAR
    ): Typeface {
        return cache.getOrPut(weight) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { buildCustomFallbackApi29(context, weight) }.getOrNull()
            } else {
                null
            } ?: fallbackSystemTypeface(weight)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun buildCustomFallbackApi29(
        context: Context,
        weight: DocumentFontWeight
    ): Typeface {
        val assets = context.assets
        val robotoRegular = Font.Builder(assets, "fonts/Roboto-Regular.ttf")
            .setWeight(400)
            .setSlant(FontStyle.FONT_SLANT_UPRIGHT)
            .build()
        val robotoMedium = Font.Builder(assets, "fonts/Roboto-Medium.ttf")
            .setWeight(500)
            .setSlant(FontStyle.FONT_SLANT_UPRIGHT)
            .build()
        val robotoBold = Font.Builder(assets, "fonts/Roboto-Bold.ttf")
            .setWeight(700)
            .setSlant(FontStyle.FONT_SLANT_UPRIGHT)
            .build()
        val robotoFamily = FontFamily.Builder(robotoRegular)
            .addFont(robotoMedium)
            .addFont(robotoBold)
            .build()

        val bn400 = Font.Builder(assets, "fonts/NotoSansBengaliUI-VF.ttf")
            .setFontVariationSettings("'wght' 400")
            .setWeight(400)
            .setSlant(FontStyle.FONT_SLANT_UPRIGHT)
            .build()
        val bn500 = Font.Builder(assets, "fonts/NotoSansBengaliUI-VF.ttf")
            .setFontVariationSettings("'wght' 500")
            .setWeight(500)
            .setSlant(FontStyle.FONT_SLANT_UPRIGHT)
            .build()
        val bn600 = Font.Builder(assets, "fonts/NotoSansBengaliUI-VF.ttf")
            .setFontVariationSettings("'wght' 600")
            .setWeight(600)
            .setSlant(FontStyle.FONT_SLANT_UPRIGHT)
            .build()
        val bn700 = Font.Builder(assets, "fonts/NotoSansBengaliUI-VF.ttf")
            .setFontVariationSettings("'wght' 700")
            .setWeight(700)
            .setSlant(FontStyle.FONT_SLANT_UPRIGHT)
            .build()
        val bengaliFamily = FontFamily.Builder(bn400)
            .addFont(bn500)
            .addFont(bn600)
            .addFont(bn700)
            .build()

        return Typeface.CustomFallbackBuilder(robotoFamily)
            .addCustomFallback(bengaliFamily)
            .setSystemFallback("sans-serif")
            .setStyle(FontStyle(weight.weight, FontStyle.FONT_SLANT_UPRIGHT))
            .build()
    }

    private fun fallbackSystemTypeface(weight: DocumentFontWeight): Typeface {
        return Typeface.create(Typeface.SANS_SERIF, weight.weight, false)
    }

    fun textPaint(
        context: Context,
        sizePx: Float,
        color: Int = BrandedDocumentPalette.TextPrimary,
        weight: DocumentFontWeight = DocumentFontWeight.REGULAR
    ): TextPaint {
        return TextPaint(
            Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG
        ).apply {
            textSize = sizePx
            this.color = color
            typeface = typeface(context, weight)
        }
    }
}

/**
 * Multi-line HarfBuzz StaticLayout and non-truncating single-line fitting utilities
 * for Bengali + English document rendering.
 */
object BrandedTextLayout {

    fun layoutMultiLine(
        text: CharSequence,
        paint: TextPaint,
        maxWidthPx: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        lineSpacingMult: Float = 1.15f,
        maxLines: Int = Int.MAX_VALUE
    ): StaticLayout {
        val safeWidth = maxWidthPx.coerceAtLeast(1)
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, safeWidth)
            .setAlignment(alignment)
            .setLineSpacing(0f, lineSpacingMult)
            .setIncludePad(true)
        if (maxLines < Int.MAX_VALUE) {
            builder.setMaxLines(maxLines.coerceAtLeast(1))
            builder.setEllipsize(TextUtils.TruncateAt.END)
        }
        return builder.build()
    }

    fun drawLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float): Float {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return y + layout.height
    }

    fun drawTextBlock(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidthPx: Int,
        paint: TextPaint,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        lineSpacingMult: Float = 1.15f,
        maxLines: Int = Int.MAX_VALUE
    ): Float {
        if (text.isEmpty()) return y
        val layout = layoutMultiLine(
            text = text,
            paint = paint,
            maxWidthPx = maxWidthPx,
            alignment = alignment,
            lineSpacingMult = lineSpacingMult,
            maxLines = maxLines
        )
        return drawLayout(canvas, layout, x, y)
    }

    fun drawFittedSingleLine(
        canvas: Canvas,
        text: String,
        x: Float,
        baselineY: Float,
        maxWidthPx: Float,
        paint: TextPaint,
        align: Paint.Align = Paint.Align.LEFT,
        minTextSizePx: Float = 7f
    ) {
        if (text.isEmpty()) return
        val origSize = paint.textSize
        val origAlign = paint.textAlign
        try {
            paint.textAlign = align
            val safeMax = maxWidthPx.coerceAtLeast(8f)
            var currentSize = origSize
            while (currentSize > minTextSizePx && paint.measureText(text) > safeMax) {
                currentSize = (currentSize - 0.5f).coerceAtLeast(minTextSizePx)
                paint.textSize = currentSize
            }
            canvas.drawText(text, x, baselineY, paint)
        } finally {
            paint.textSize = origSize
            paint.textAlign = origAlign
        }
    }
}
