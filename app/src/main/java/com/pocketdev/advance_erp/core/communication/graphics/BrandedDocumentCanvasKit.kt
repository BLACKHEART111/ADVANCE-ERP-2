package com.pocketdev.advance_erp.core.communication.graphics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import com.pocketdev.advance_erp.core.communication.CompanyBrandInfo
import com.pocketdev.advance_erp.core.util.CompanyBrandingHelper
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

enum class BrandedIconType {
    INVOICE_DOC,
    PAYMENT_RECEIPT,
    BELL_REMINDER,
    LEDGER_BOOK,
    SHOPPING_CART,
    CUSTOMER_USER,
    CALCULATOR,
    BALANCE_SCALE,
    MONEY_BAG,
    COIN_TAKA,
    PAYMENT_CARD,
    CALENDAR,
    BANK_BUILDING,
    RETURN_ARROW,
    DELIVERY_TRUCK,
    EXCHANGE_ARROWS,
    INFO_CIRCLE,
    CHECK_CIRCLE,
    ALERT_CIRCLE,
    PHONE,
    LOCATION_PIN,
    HANDSHAKE,
    QR_SCAN
}

/**
 * Pure Canvas + Path vector icon drawer mapped onto a 24x24 virtual grid so icons stay sharp
 * at any scale on both 72-dpi A4 PDFs and high-resolution PNG cards.
 */
object BrandedVectorIcons {

    fun drawIcon(
        canvas: Canvas,
        type: BrandedIconType,
        centerX: Float,
        centerY: Float,
        sizePx: Float,
        color: Int
    ) {
        if (sizePx <= 0f) return
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 1.9f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        canvas.save()
        canvas.translate(centerX - sizePx / 2f, centerY - sizePx / 2f)
        canvas.scale(sizePx / 24f, sizePx / 24f)

        when (type) {
            BrandedIconType.INVOICE_DOC, BrandedIconType.PAYMENT_RECEIPT -> {
                val doc = Path().apply {
                    moveTo(6f, 3f)
                    lineTo(14f, 3f)
                    lineTo(18f, 7f)
                    lineTo(18f, 21f)
                    lineTo(6f, 21f)
                    close()
                }
                canvas.drawPath(doc, stroke)
                canvas.drawLine(14f, 3f, 14f, 7f, stroke)
                canvas.drawLine(14f, 7f, 18f, 7f, stroke)
                canvas.drawLine(9f, 11f, 15f, 11f, stroke)
                canvas.drawLine(9f, 14.5f, 15f, 14.5f, stroke)
                canvas.drawLine(9f, 18f, 12.5f, 18f, stroke)
            }
            BrandedIconType.BELL_REMINDER -> {
                val bell = Path().apply {
                    moveTo(18f, 15f)
                    lineTo(18f, 10f)
                    cubicTo(18f, 6.5f, 15.5f, 4f, 12f, 4f)
                    cubicTo(8.5f, 4f, 6f, 6.5f, 6f, 10f)
                    lineTo(6f, 15f)
                    lineTo(4f, 17.5f)
                    lineTo(20f, 17.5f)
                    close()
                }
                canvas.drawPath(bell, stroke)
                canvas.drawArc(RectF(9.5f, 18f, 14.5f, 21.5f), 0f, 180f, false, stroke)
            }
            BrandedIconType.LEDGER_BOOK -> {
                canvas.drawRoundRect(RectF(5f, 3f, 19f, 21f), 2f, 2f, stroke)
                canvas.drawLine(8.5f, 3f, 8.5f, 21f, stroke)
                canvas.drawLine(11.5f, 8.5f, 16f, 8.5f, stroke)
                canvas.drawLine(11.5f, 12.5f, 16f, 12.5f, stroke)
                canvas.drawLine(11.5f, 16.5f, 14.5f, 16.5f, stroke)
            }
            BrandedIconType.SHOPPING_CART -> {
                val cart = Path().apply {
                    moveTo(2.5f, 4f)
                    lineTo(5.5f, 4f)
                    lineTo(8f, 14.5f)
                    lineTo(18.5f, 14.5f)
                    lineTo(20.5f, 7.5f)
                    lineTo(6.5f, 7.5f)
                }
                canvas.drawPath(cart, stroke)
                canvas.drawCircle(9.5f, 18.5f, 1.5f, fill)
                canvas.drawCircle(17f, 18.5f, 1.5f, fill)
            }
            BrandedIconType.CUSTOMER_USER -> {
                canvas.drawCircle(12f, 8f, 3.6f, stroke)
                val body = Path().apply {
                    moveTo(5f, 20f)
                    cubicTo(5f, 15.5f, 8f, 13.5f, 12f, 13.5f)
                    cubicTo(16f, 13.5f, 19f, 15.5f, 19f, 20f)
                }
                canvas.drawPath(body, stroke)
            }
            BrandedIconType.CALCULATOR -> {
                canvas.drawRoundRect(RectF(5f, 3f, 19f, 21f), 2f, 2f, stroke)
                canvas.drawRect(RectF(8f, 6f, 16f, 9.5f), stroke)
                for (row in 0..2) {
                    for (col in 0..2) {
                        canvas.drawCircle(8.5f + col * 3.5f, 12.5f + row * 3f, 0.9f, fill)
                    }
                }
            }
            BrandedIconType.BALANCE_SCALE -> {
                canvas.drawLine(12f, 4f, 12f, 20f, stroke)
                canvas.drawLine(7f, 20f, 17f, 20f, stroke)
                canvas.drawLine(5f, 7.5f, 19f, 7.5f, stroke)
                canvas.drawLine(5f, 7.5f, 3.2f, 13.5f, stroke)
                canvas.drawLine(5f, 7.5f, 6.8f, 13.5f, stroke)
                canvas.drawArc(RectF(2.5f, 12f, 7.5f, 16f), 0f, 180f, false, stroke)
                canvas.drawLine(19f, 7.5f, 17.2f, 13.5f, stroke)
                canvas.drawLine(19f, 7.5f, 20.8f, 13.5f, stroke)
                canvas.drawArc(RectF(16.5f, 12f, 21.5f, 16f), 0f, 180f, false, stroke)
            }
            BrandedIconType.MONEY_BAG -> {
                val bag = Path().apply {
                    moveTo(9f, 4f)
                    lineTo(15f, 4f)
                    lineTo(13.5f, 7.5f)
                    cubicTo(17.5f, 9f, 19f, 12.5f, 19f, 16f)
                    cubicTo(19f, 19f, 16.5f, 20.5f, 12f, 20.5f)
                    cubicTo(7.5f, 20.5f, 5f, 19f, 5f, 16f)
                    cubicTo(5f, 12.5f, 6.5f, 9f, 10.5f, 7.5f)
                    close()
                }
                canvas.drawPath(bag, stroke)
                canvas.drawLine(9.5f, 8f, 14.5f, 8f, stroke)
                canvas.drawLine(12f, 11f, 12f, 17f, stroke)
                canvas.drawLine(9.5f, 13f, 14.5f, 13f, stroke)
            }
            BrandedIconType.COIN_TAKA -> {
                canvas.drawCircle(12f, 12f, 8.5f, stroke)
                canvas.drawLine(10.5f, 7.5f, 10.5f, 16.5f, stroke)
                canvas.drawLine(8.5f, 11f, 14.5f, 11f, stroke)
                canvas.drawArc(RectF(10.5f, 12f, 15.5f, 16.5f), 0f, 180f, false, stroke)
            }
            BrandedIconType.PAYMENT_CARD -> {
                canvas.drawRoundRect(RectF(3f, 5.5f, 21f, 18.5f), 2.5f, 2.5f, stroke)
                canvas.drawLine(3f, 10f, 21f, 10f, stroke)
                canvas.drawLine(6.5f, 14.5f, 11.5f, 14.5f, stroke)
            }
            BrandedIconType.CALENDAR -> {
                canvas.drawRoundRect(RectF(4f, 5f, 20f, 20f), 2.2f, 2.2f, stroke)
                canvas.drawLine(4f, 9.5f, 20f, 9.5f, stroke)
                canvas.drawLine(8f, 3f, 8f, 6.5f, stroke)
                canvas.drawLine(16f, 3f, 16f, 6.5f, stroke)
                canvas.drawCircle(8.5f, 13.5f, 1f, fill)
                canvas.drawCircle(12f, 13.5f, 1f, fill)
                canvas.drawCircle(15.5f, 13.5f, 1f, fill)
            }
            BrandedIconType.BANK_BUILDING -> {
                val roof = Path().apply {
                    moveTo(3.5f, 8.5f)
                    lineTo(12f, 3.5f)
                    lineTo(20.5f, 8.5f)
                    close()
                }
                canvas.drawPath(roof, stroke)
                canvas.drawLine(4f, 20f, 20f, 20f, stroke)
                canvas.drawLine(7f, 9f, 7f, 19.5f, stroke)
                canvas.drawLine(12f, 9f, 12f, 19.5f, stroke)
                canvas.drawLine(17f, 9f, 17f, 19.5f, stroke)
            }
            BrandedIconType.RETURN_ARROW -> {
                canvas.drawArc(RectF(6f, 7f, 19f, 18f), -90f, 210f, false, stroke)
                canvas.drawLine(11f, 7f, 5f, 7f, stroke)
                canvas.drawLine(5f, 7f, 8.5f, 4f, stroke)
                canvas.drawLine(5f, 7f, 8.5f, 10f, stroke)
            }
            BrandedIconType.DELIVERY_TRUCK -> {
                canvas.drawRect(RectF(2.5f, 6f, 15f, 15.5f), stroke)
                val cab = Path().apply {
                    moveTo(15f, 9f)
                    lineTo(19f, 9f)
                    lineTo(21.5f, 12.5f)
                    lineTo(21.5f, 15.5f)
                    lineTo(15f, 15.5f)
                    close()
                }
                canvas.drawPath(cab, stroke)
                canvas.drawCircle(7f, 17.5f, 1.8f, stroke)
                canvas.drawCircle(18f, 17.5f, 1.8f, stroke)
            }
            BrandedIconType.EXCHANGE_ARROWS -> {
                canvas.drawLine(4f, 8.5f, 19f, 8.5f, stroke)
                canvas.drawLine(15.5f, 5f, 19.5f, 8.5f, stroke)
                canvas.drawLine(15.5f, 12f, 19.5f, 8.5f, stroke)
                canvas.drawLine(5f, 15.5f, 20f, 15.5f, stroke)
                canvas.drawLine(8.5f, 12f, 4.5f, 15.5f, stroke)
                canvas.drawLine(8.5f, 19f, 4.5f, 15.5f, stroke)
            }
            BrandedIconType.INFO_CIRCLE -> {
                canvas.drawCircle(12f, 12f, 8.5f, stroke)
                canvas.drawCircle(12f, 8f, 1f, fill)
                canvas.drawLine(12f, 11f, 12f, 16.5f, stroke)
            }
            BrandedIconType.CHECK_CIRCLE -> {
                canvas.drawCircle(12f, 12f, 8.5f, stroke)
                canvas.drawLine(8f, 12.3f, 10.8f, 15f, stroke)
                canvas.drawLine(10.8f, 15f, 16.2f, 9.2f, stroke)
            }
            BrandedIconType.ALERT_CIRCLE -> {
                canvas.drawCircle(12f, 12f, 8.5f, stroke)
                canvas.drawLine(12f, 7.8f, 12f, 13.2f, stroke)
                canvas.drawCircle(12f, 16.2f, 1f, fill)
            }
            BrandedIconType.PHONE -> {
                val phonePath = Path().apply {
                    moveTo(6.5f, 4.5f)
                    lineTo(9.5f, 4.5f)
                    lineTo(10.8f, 8.2f)
                    lineTo(8.8f, 9.8f)
                    cubicTo(9.8f, 12f, 12f, 14.2f, 14.2f, 15.2f)
                    lineTo(15.8f, 13.2f)
                    lineTo(19.5f, 14.5f)
                    lineTo(19.5f, 17.5f)
                    cubicTo(19.5f, 18.8f, 18.2f, 19.8f, 16.8f, 19.5f)
                    cubicTo(10.5f, 18.5f, 5.5f, 13.5f, 4.5f, 7.2f)
                    cubicTo(4.2f, 5.8f, 5.2f, 4.5f, 6.5f, 4.5f)
                    close()
                }
                canvas.drawPath(phonePath, stroke)
            }
            BrandedIconType.LOCATION_PIN -> {
                val pin = Path().apply {
                    moveTo(12f, 21f)
                    cubicTo(12f, 21f, 6f, 14.5f, 6f, 10f)
                    cubicTo(6f, 6.5f, 8.7f, 4f, 12f, 4f)
                    cubicTo(15.3f, 4f, 18f, 6.5f, 18f, 10f)
                    cubicTo(18f, 14.5f, 12f, 21f, 12f, 21f)
                    close()
                }
                canvas.drawPath(pin, stroke)
                canvas.drawCircle(12f, 9.8f, 2f, fill)
            }
            BrandedIconType.HANDSHAKE -> {
                canvas.drawRoundRect(RectF(4f, 8.5f, 20f, 16.5f), 4f, 4f, stroke)
                canvas.drawLine(8.5f, 10f, 12f, 13.5f, stroke)
                canvas.drawLine(12f, 13.5f, 15.5f, 10f, stroke)
                canvas.drawLine(10f, 16.5f, 14f, 16.5f, stroke)
            }
            BrandedIconType.QR_SCAN -> {
                canvas.drawRect(RectF(4f, 4f, 10f, 10f), stroke)
                canvas.drawRect(RectF(14f, 4f, 20f, 10f), stroke)
                canvas.drawRect(RectF(4f, 14f, 10f, 20f), stroke)
                canvas.drawRect(RectF(6f, 6f, 8f, 8f), fill)
                canvas.drawRect(RectF(16f, 6f, 18f, 8f), fill)
                canvas.drawRect(RectF(6f, 16f, 8f, 18f), fill)
                canvas.drawRect(RectF(14f, 14f, 17f, 17f), fill)
                canvas.drawRect(RectF(17.5f, 17.5f, 20f, 20f), fill)
            }
        }
        canvas.restore()
    }

    fun drawTintedIconBox(
        canvas: Canvas,
        type: BrandedIconType,
        left: Float,
        top: Float,
        boxSizePx: Float,
        cornerRadiusPx: Float,
        bgColor: Int,
        iconColor: Int
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        canvas.drawRoundRect(
            RectF(left, top, left + boxSizePx, top + boxSizePx),
            cornerRadiusPx,
            cornerRadiusPx,
            bgPaint
        )
        drawIcon(
            canvas = canvas,
            type = type,
            centerX = left + boxSizePx / 2f,
            centerY = top + boxSizePx / 2f,
            sizePx = boxSizePx * 0.62f,
            color = iconColor
        )
    }
}

/**
 * Shared branded wave banners, rounded section boxes, total pills, status chips,
 * and product thumbnail helpers shared by both PDF and Image Card exporters.
 */
object BrandedCanvasComponents {

    fun drawWaveHeaderBanner(
        canvas: Canvas,
        context: Context,
        bounds: RectF,
        brand: CompanyBrandInfo,
        scale: Float,
        cornerRadiusTopPx: Float = 0f,
        rightTitleTop: String? = null,
        rightTitleBottom: String? = null,
        rightIcon: BrandedIconType? = null
    ) {
        val clipPath = Path().apply {
            if (cornerRadiusTopPx > 0f) {
                addRoundRect(
                    bounds,
                    floatArrayOf(
                        cornerRadiusTopPx, cornerRadiusTopPx,
                        cornerRadiusTopPx, cornerRadiusTopPx,
                        0f, 0f, 0f, 0f
                    ),
                    Path.Direction.CW
                )
            } else {
                addRect(bounds, Path.Direction.CW)
            }
        }

        canvas.save()
        canvas.clipPath(clipPath)

        val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                bounds.left, bounds.top, bounds.right, bounds.bottom,
                intArrayOf(
                    BrandedDocumentPalette.BrandPurpleDeep,
                    BrandedDocumentPalette.BrandPurplePrimary,
                    BrandedDocumentPalette.BrandPurpleBright
                ),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(bounds, gradPaint)

        // Translucent purple & teal Bezier wave ribbons
        val w = bounds.width()
        val h = bounds.height()
        val wave1 = Path().apply {
            moveTo(bounds.left, bounds.bottom)
            lineTo(bounds.left, bounds.top + h * 0.78f)
            cubicTo(
                bounds.left + w * 0.28f, bounds.top + h * 0.42f,
                bounds.left + w * 0.65f, bounds.bottom + h * 0.05f,
                bounds.right, bounds.top + h * 0.35f
            )
            lineTo(bounds.right, bounds.bottom)
            close()
        }
        val wave1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(46, 209, 179, 255)
        }
        canvas.drawPath(wave1, wave1Paint)

        val wave2 = Path().apply {
            moveTo(bounds.left + w * 0.30f, bounds.bottom)
            cubicTo(
                bounds.left + w * 0.56f, bounds.top + h * 0.56f,
                bounds.left + w * 0.80f, bounds.bottom - h * 0.12f,
                bounds.right, bounds.top + h * 0.62f
            )
            lineTo(bounds.right, bounds.bottom)
            close()
        }
        val wave2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(72, 41, 157, 178)
        }
        canvas.drawPath(wave2, wave2Paint)

        val padX = 16f * scale
        val logoSize = (h * 0.60f).coerceIn(22f * scale, 56f * scale)
        val logoLeft = bounds.left + padX
        val logoTop = bounds.top + (h - logoSize) / 2f
        val logoRect = RectF(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)

        val profileForLogo = BusinessProfile(
            companyName = brand.companyName,
            phone = brand.phone,
            address = brand.address,
            logoPath = brand.logoPath
        )
        val logoBitmap = CompanyBrandingHelper.loadCompanyLogoBitmap(context, profileForLogo)
        if (logoBitmap != null) {
            try {
                if (logoBitmap.width > 0 && logoBitmap.height > 0) {
                    val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(38, 255, 255, 255)
                    }
                    canvas.drawRoundRect(logoRect, 8f * scale, 8f * scale, badgeBg)
                    val inset = 3f * scale
                    val dest = RectF(
                        logoRect.left + inset, logoRect.top + inset,
                        logoRect.right - inset, logoRect.bottom - inset
                    )
                    canvas.drawBitmap(logoBitmap, null, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                }
            } finally {
                logoBitmap.recycle()
            }
        } else {
            BrandedVectorIcons.drawTintedIconBox(
                canvas = canvas,
                type = BrandedIconType.SHOPPING_CART,
                left = logoRect.left,
                top = logoRect.top,
                boxSizePx = logoSize,
                cornerRadiusPx = 8f * scale,
                bgColor = Color.argb(46, 255, 255, 255),
                iconColor = Color.WHITE
            )
        }

        val textLeft = logoRect.right + 10f * scale
        val rightReserved = if (!rightTitleTop.isNullOrBlank()) w * 0.36f else padX
        val textMaxWidth = (bounds.right - rightReserved - textLeft).coerceAtLeast(40f * scale)

        val titlePaint = BrandedTypefaceProvider.textPaint(
            context = context,
            sizePx = 15.5f * scale,
            color = Color.WHITE,
            weight = DocumentFontWeight.BOLD
        )
        val hasTagline = !brand.tagline.isNullOrBlank()
        val companyBaseline = if (hasTagline) {
            bounds.top + h * 0.48f
        } else {
            bounds.centerY() + titlePaint.textSize * 0.35f
        }
        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = brand.companyName,
            x = textLeft,
            baselineY = companyBaseline,
            maxWidthPx = textMaxWidth,
            paint = titlePaint,
            minTextSizePx = 9f * scale
        )

        if (hasTagline) {
            val tagPaint = BrandedTypefaceProvider.textPaint(
                context = context,
                sizePx = 8.5f * scale,
                color = Color.argb(228, 240, 232, 255),
                weight = DocumentFontWeight.REGULAR
            )
            BrandedTextLayout.drawFittedSingleLine(
                canvas = canvas,
                text = brand.tagline!!,
                x = textLeft,
                baselineY = companyBaseline + 12.5f * scale,
                maxWidthPx = textMaxWidth,
                paint = tagPaint,
                minTextSizePx = 6.5f * scale
            )
        }

        if (!rightTitleTop.isNullOrBlank()) {
            val badgeMaxW = w * 0.34f
            val pillRight = bounds.right - padX
            val pillTop = bounds.top + h * 0.16f
            val pillBottom = bounds.bottom - h * 0.16f
            val pillLeft = (pillRight - badgeMaxW).coerceAtLeast(textLeft + 12f * scale)
            val pillRect = RectF(pillLeft, pillTop, pillRight, pillBottom)
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(40, 255, 255, 255)
            }
            canvas.drawRoundRect(pillRect, 10f * scale, 10f * scale, pillPaint)

            var textRight = pillRect.right - 10f * scale
            if (rightIcon != null) {
                val iconSz = 18f * scale
                BrandedVectorIcons.drawIcon(
                    canvas = canvas,
                    type = rightIcon,
                    centerX = pillRect.left + 16f * scale,
                    centerY = pillRect.centerY(),
                    sizePx = iconSz,
                    color = Color.WHITE
                )
                textRight = pillRect.right - 10f * scale
            }
            val rightTextMaxW = (pillRect.width() - (if (rightIcon != null) 34f * scale else 18f * scale)).coerceAtLeast(24f * scale)
            val rtTopPaint = BrandedTypefaceProvider.textPaint(
                context = context,
                sizePx = 13f * scale,
                color = Color.WHITE,
                weight = DocumentFontWeight.BOLD
            )
            BrandedTextLayout.drawFittedSingleLine(
                canvas = canvas,
                text = rightTitleTop,
                x = textRight,
                baselineY = if (!rightTitleBottom.isNullOrBlank()) pillRect.centerY() - 2f * scale else pillRect.centerY() + rtTopPaint.textSize * 0.35f,
                maxWidthPx = rightTextMaxW,
                paint = rtTopPaint,
                align = Paint.Align.RIGHT,
                minTextSizePx = 8f * scale
            )
            if (!rightTitleBottom.isNullOrBlank()) {
                val rtSubPaint = BrandedTypefaceProvider.textPaint(
                    context = context,
                    sizePx = 9.5f * scale,
                    color = Color.argb(230, 232, 245, 255),
                    weight = DocumentFontWeight.MEDIUM
                )
                BrandedTextLayout.drawFittedSingleLine(
                    canvas = canvas,
                    text = rightTitleBottom,
                    x = textRight,
                    baselineY = pillRect.centerY() + 12f * scale,
                    maxWidthPx = rightTextMaxW,
                    paint = rtSubPaint,
                    align = Paint.Align.RIGHT,
                    minTextSizePx = 7f * scale
                )
            }
        }

        canvas.restore()
    }

    fun drawCardWaveFooter(
        canvas: Canvas,
        context: Context,
        bounds: RectF,
        phone: String,
        address: String,
        cornerRadiusBottomPx: Float,
        scale: Float
    ) {
        val clipPath = Path().apply {
            addRoundRect(
                bounds,
                floatArrayOf(
                    0f, 0f, 0f, 0f,
                    cornerRadiusBottomPx, cornerRadiusBottomPx,
                    cornerRadiusBottomPx, cornerRadiusBottomPx
                ),
                Path.Direction.CW
            )
        }
        canvas.save()
        canvas.clipPath(clipPath)

        val w = bounds.width()
        val h = bounds.height()
        // Subtle teal ribbon crest on top edge of footer
        val tealWave = Path().apply {
            moveTo(bounds.left, bounds.top + h * 0.25f)
            cubicTo(
                bounds.left + w * 0.35f, bounds.top - h * 0.15f,
                bounds.left + w * 0.65f, bounds.top + h * 0.45f,
                bounds.right, bounds.top + h * 0.10f
            )
            lineTo(bounds.right, bounds.bottom)
            lineTo(bounds.left, bounds.bottom)
            close()
        }
        canvas.drawPath(tealWave, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BrandedDocumentPalette.BrandTealAccent
        })

        val purpleWave = Path().apply {
            moveTo(bounds.left, bounds.top + h * 0.38f)
            cubicTo(
                bounds.left + w * 0.30f, bounds.top + h * 0.10f,
                bounds.left + w * 0.72f, bounds.top + h * 0.46f,
                bounds.right, bounds.top + h * 0.24f
            )
            lineTo(bounds.right, bounds.bottom)
            lineTo(bounds.left, bounds.bottom)
            close()
        }
        canvas.drawPath(purpleWave, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                bounds.left, bounds.top, bounds.right, bounds.bottom,
                BrandedDocumentPalette.BrandPurpleDeep,
                BrandedDocumentPalette.BrandPurplePrimary,
                Shader.TileMode.CLAMP
            )
        })

        val contentY = bounds.top + h * 0.68f
        val padX = 14f * scale
        val textPaint = BrandedTypefaceProvider.textPaint(
            context = context,
            sizePx = 9f * scale,
            color = Color.WHITE,
            weight = DocumentFontWeight.MEDIUM
        )
        val iconSz = 10.5f * scale

        val cleanPhone = phone.trim()
        val cleanAddress = address.trim()
        val leftStart = bounds.left + padX
        if (cleanPhone.isNotEmpty()) {
            BrandedVectorIcons.drawIcon(
                canvas = canvas,
                type = BrandedIconType.PHONE,
                centerX = leftStart + iconSz / 2f,
                centerY = contentY - 2.5f * scale,
                sizePx = iconSz,
                color = BrandedDocumentPalette.BrandTealBright
            )
            val phoneMaxW = if (cleanAddress.isNotEmpty()) w * 0.36f else w - padX * 2 - iconSz - 6f * scale
            BrandedTextLayout.drawFittedSingleLine(
                canvas = canvas,
                text = cleanPhone,
                x = leftStart + iconSz + 5f * scale,
                baselineY = contentY + 1f * scale,
                maxWidthPx = phoneMaxW,
                paint = textPaint,
                minTextSizePx = 6.5f * scale
            )
        }
        if (cleanAddress.isNotEmpty()) {
            val addrMaxW = if (cleanPhone.isNotEmpty()) w * 0.50f else w - padX * 2 - iconSz - 6f * scale
            val textW = textPaint.measureText(cleanAddress).coerceAtMost(addrMaxW)
            val rightAnchor = bounds.right - padX
            val pinCenterX = rightAnchor - textW - 5f * scale - iconSz / 2f
            BrandedVectorIcons.drawIcon(
                canvas = canvas,
                type = BrandedIconType.LOCATION_PIN,
                centerX = pinCenterX.coerceAtLeast(bounds.left + w * 0.42f),
                centerY = contentY - 2.5f * scale,
                sizePx = iconSz,
                color = BrandedDocumentPalette.BrandTealBright
            )
            BrandedTextLayout.drawFittedSingleLine(
                canvas = canvas,
                text = cleanAddress,
                x = rightAnchor,
                baselineY = contentY + 1f * scale,
                maxWidthPx = addrMaxW,
                paint = textPaint,
                align = Paint.Align.RIGHT,
                minTextSizePx = 6.5f * scale
            )
        }

        canvas.restore()
    }

    fun drawPdfWaveFooter(
        canvas: Canvas,
        context: Context,
        pageWidth: Float,
        pageHeight: Float,
        footerHeight: Float,
        generatedOnText: String,
        pageNumber: Int,
        totalPages: Int
    ) {
        val top = pageHeight - footerHeight
        val bounds = RectF(0f, top, pageWidth, pageHeight)
        val tealAccent = Path().apply {
            moveTo(0f, top + footerHeight * 0.30f)
            cubicTo(
                pageWidth * 0.35f, top,
                pageWidth * 0.70f, top + footerHeight * 0.42f,
                pageWidth, top + footerHeight * 0.14f
            )
            lineTo(pageWidth, pageHeight)
            lineTo(0f, pageHeight)
            close()
        }
        canvas.drawPath(tealAccent, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BrandedDocumentPalette.BrandTealAccent
        })

        val purpleBand = Path().apply {
            moveTo(0f, top + footerHeight * 0.42f)
            cubicTo(
                pageWidth * 0.32f, top + footerHeight * 0.18f,
                pageWidth * 0.68f, top + footerHeight * 0.48f,
                pageWidth, top + footerHeight * 0.28f
            )
            lineTo(pageWidth, pageHeight)
            lineTo(0f, pageHeight)
            close()
        }
        canvas.drawPath(purpleBand, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                bounds.left, top, bounds.right, pageHeight,
                BrandedDocumentPalette.BrandPurpleDeep,
                BrandedDocumentPalette.BrandPurplePrimary,
                Shader.TileMode.CLAMP
            )
        })

        val margin = 28f
        val baseline = pageHeight - footerHeight * 0.26f
        val leftPaint = BrandedTypefaceProvider.textPaint(
            context = context,
            sizePx = 8f,
            color = Color.WHITE,
            weight = DocumentFontWeight.SEMI_BOLD
        )
        canvas.drawText("Powered by ADVANCE ERP", margin, baseline, leftPaint)

        val rightPaint = BrandedTypefaceProvider.textPaint(
            context = context,
            sizePx = 7.5f,
            color = Color.argb(232, 238, 232, 255),
            weight = DocumentFontWeight.REGULAR
        )
        val pageLabel = "Page $pageNumber of $totalPages"
        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = pageLabel,
            x = pageWidth / 2f,
            baselineY = baseline,
            maxWidthPx = 110f,
            paint = rightPaint,
            align = Paint.Align.CENTER,
            minTextSizePx = 6.5f
        )

        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = generatedOnText,
            x = pageWidth - margin,
            baselineY = baseline,
            maxWidthPx = 210f,
            paint = rightPaint,
            align = Paint.Align.RIGHT,
            minTextSizePx = 6f
        )
    }

    fun drawSectionCard(
        canvas: Canvas,
        rect: RectF,
        radiusPx: Float,
        fillColor: Int = BrandedDocumentPalette.SectionBoxBg,
        borderColor: Int = BrandedDocumentPalette.SectionBoxBorder,
        borderWidthPx: Float = 1f
    ) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, radiusPx, radiusPx, fillPaint)
        if (borderWidthPx > 0f) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = borderColor
                style = Paint.Style.STROKE
                strokeWidth = borderWidthPx
            }
            canvas.drawRoundRect(rect, radiusPx, radiusPx, borderPaint)
        }
    }

    fun drawStatusPill(
        canvas: Canvas,
        context: Context,
        anchorX: Float,
        topY: Float,
        badge: BrandedDocumentPalette.StatusBadgeSpec,
        scale: Float,
        alignRight: Boolean = true
    ): RectF {
        val textPaint = BrandedTypefaceProvider.textPaint(
            context = context,
            sizePx = 8.5f * scale,
            color = badge.textColor,
            weight = DocumentFontWeight.BOLD
        )
        val hasIcon = badge.isCheckIcon || badge.isAlertIcon
        val iconSz = if (hasIcon) 10.5f * scale else 0f
        val iconGap = if (hasIcon) 4f * scale else 0f
        val padH = 9f * scale
        val padV = 4.5f * scale
        val textW = textPaint.measureText(badge.text)
        val pillW = padH * 2 + iconSz + iconGap + textW
        val pillH = textPaint.textSize + padV * 2.1f
        val left = if (alignRight) anchorX - pillW else anchorX
        val rect = RectF(left, topY, left + pillW, topY + pillH)

        drawSectionCard(
            canvas = canvas,
            rect = rect,
            radiusPx = pillH / 2f,
            fillColor = badge.bgColor,
            borderColor = badge.borderColor,
            borderWidthPx = 1f * scale
        )

        var textX = rect.left + padH
        if (hasIcon) {
            val iconType = if (badge.isCheckIcon) BrandedIconType.CHECK_CIRCLE else BrandedIconType.ALERT_CIRCLE
            BrandedVectorIcons.drawIcon(
                canvas = canvas,
                type = iconType,
                centerX = textX + iconSz / 2f,
                centerY = rect.centerY(),
                sizePx = iconSz,
                color = badge.textColor
            )
            textX += iconSz + iconGap
        }
        val baseline = rect.centerY() + (textPaint.textSize * 0.34f)
        canvas.drawText(badge.text, textX, baseline, textPaint)
        return rect
    }

    fun drawPurpleTotalPill(
        canvas: Canvas,
        context: Context,
        rect: RectF,
        icon: BrandedIconType,
        label: String,
        amountText: String,
        scale: Float
    ) {
        val radius = (rect.height() * 0.45f).coerceAtMost(14f * scale)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                BrandedDocumentPalette.BrandPurpleDeep,
                BrandedDocumentPalette.BrandPurpleBright,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, radius, radius, bgPaint)

        val padX = 12f * scale
        val iconSz = 16f * scale
        BrandedVectorIcons.drawIcon(
            canvas = canvas,
            type = icon,
            centerX = rect.left + padX + iconSz / 2f,
            centerY = rect.centerY(),
            sizePx = iconSz,
            color = Color.WHITE
        )

        val labelPaint = BrandedTypefaceProvider.textPaint(
            context = context,
            sizePx = 11f * scale,
            color = Color.WHITE,
            weight = DocumentFontWeight.BOLD
        )
        val amountPaint = BrandedTypefaceProvider.textPaint(
            context = context,
            sizePx = 13f * scale,
            color = Color.WHITE,
            weight = DocumentFontWeight.BOLD
        )
        val labelX = rect.left + padX + iconSz + 7f * scale
        val availableTotal = (rect.right - padX) - labelX
        val labelMaxW = (availableTotal * 0.54f).coerceAtLeast(32f * scale)
        val amountMaxW = (availableTotal * 0.44f).coerceAtLeast(32f * scale)

        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = label,
            x = labelX,
            baselineY = rect.centerY() + labelPaint.textSize * 0.34f,
            maxWidthPx = labelMaxW,
            paint = labelPaint,
            minTextSizePx = 7.5f * scale
        )
        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = amountText,
            x = rect.right - padX,
            baselineY = rect.centerY() + amountPaint.textSize * 0.35f,
            maxWidthPx = amountMaxW,
            paint = amountPaint,
            align = Paint.Align.RIGHT,
            minTextSizePx = 8f * scale
        )
    }

    fun loadProductThumbnailBitmap(path: String?, targetPx: Int = 120): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path.removePrefix("file://").trim())
        if (!file.isFile || file.length() <= 0L) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w <= 0 || h <= 0) return null
            var sampleSize = 1
            while ((w / (sampleSize * 2)) >= targetPx && (h / (sampleSize * 2)) >= targetPx) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
            val side = minOf(decoded.width, decoded.height).coerceAtLeast(1)
            val left = (decoded.width - side) / 2
            val top = (decoded.height - side) / 2
            val square = if (decoded.width != side || decoded.height != side) {
                Bitmap.createBitmap(decoded, left, top, side, side).also { decoded.recycle() }
            } else {
                decoded
            }
            if (side > targetPx && targetPx > 0) {
                Bitmap.createScaledBitmap(square, targetPx, targetPx, true).also {
                    if (it != square) square.recycle()
                }
            } else {
                square
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun drawProductThumbnail(
        canvas: Canvas,
        rect: RectF,
        bitmap: Bitmap?,
        radiusPx: Float,
        drawPlaceholderIfNull: Boolean = true
    ) {
        val clip = Path().apply { addRoundRect(rect, radiusPx, radiusPx, Path.Direction.CW) }
        if (bitmap != null && !bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0) {
            canvas.save()
            canvas.clipPath(clip)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(
                bitmap,
                Rect(0, 0, bitmap.width, bitmap.height),
                rect,
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            )
            canvas.restore()
            drawSectionCard(
                canvas = canvas,
                rect = rect,
                radiusPx = radiusPx,
                fillColor = Color.TRANSPARENT,
                borderColor = BrandedDocumentPalette.SectionBoxBorder,
                borderWidthPx = 0.8f
            )
        } else if (drawPlaceholderIfNull) {
            drawSectionCard(
                canvas = canvas,
                rect = rect,
                radiusPx = radiusPx,
                fillColor = BrandedDocumentPalette.SectionBoxAltBg,
                borderColor = BrandedDocumentPalette.SectionBoxBorder,
                borderWidthPx = 0.8f
            )
            BrandedVectorIcons.drawIcon(
                canvas = canvas,
                type = BrandedIconType.SHOPPING_CART,
                centerX = rect.centerX(),
                centerY = rect.centerY(),
                sizePx = rect.width() * 0.50f,
                color = BrandedDocumentPalette.BrandPurplePrimary
            )
        }
    }
}
