package com.pocketdev.advance_erp.core.communication.image

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.pocketdev.advance_erp.core.communication.CompanyBrandInfo
import com.pocketdev.advance_erp.core.communication.SafeDocument
import com.pocketdev.advance_erp.core.communication.graphics.BrandedCanvasComponents
import com.pocketdev.advance_erp.core.communication.graphics.BrandedDocumentPalette
import com.pocketdev.advance_erp.core.communication.graphics.BrandedIconType
import com.pocketdev.advance_erp.core.communication.graphics.BrandedTextLayout
import com.pocketdev.advance_erp.core.communication.graphics.BrandedTypefaceProvider
import com.pocketdev.advance_erp.core.communication.graphics.BrandedVectorIcons
import com.pocketdev.advance_erp.core.communication.graphics.DocumentFontWeight
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * High-resolution (1080px portrait) offline Canvas/Bitmap PNG card generator matching
 * the approved REF-02..REF-05 visual language and extended ERP communication modules.
 * Includes MediaStore Pictures/AdvanceERP gallery saving for API 28+.
 */
object BrandedImageCardGenerator {

    private const val BITMAP_WIDTH = 1080
    private const val SCALE = 3.0f
    private const val OUTER_MARGIN = 10f * SCALE // 30px -> inner card width 1020px
    private const val CARD_CORNER_RADIUS = 24f * SCALE
    private const val HEADER_HEIGHT = 82f * SCALE
    private const val FOOTER_HEIGHT = 46f * SCALE
    private const val INNER_PAD_X = 16f * SCALE
    private const val SECTION_GAP = 10f * SCALE

    fun generatePages(
        context: Context,
        document: SafeDocument,
        settings: CommunicationSettings,
        language: String,
        outputDir: File
    ): List<File> {
        outputDir.mkdirs()
        val pages = BrandedImageCardSpecBuilder.buildPages(document, settings, language)
        val brand = document.brand ?: CompanyBrandInfo(companyName = "ADVANCE ERP")
        val fonts = CardFontSet.create(context)

        val thumbCache = mutableMapOf<String, Bitmap>()
        val results = mutableListOf<File>()
        try {
            if (settings.showProductPhoto) {
                pages.asSequence()
                    .flatMap { it.sections.asSequence() }
                    .filterIsInstance<ImageCardBodySection.ItemsListSection>()
                    .flatMap { it.items.asSequence() }
                    .mapNotNull { it.thumbnailPath }
                    .distinct()
                    .forEach { path ->
                        BrandedCanvasComponents.loadProductThumbnailBitmap(path, targetPx = 156)?.let { bmp ->
                            thumbCache[path] = bmp
                        }
                    }
            }

            for (pageSpec in pages) {
                val targetFile = File(outputDir, pageSpec.safeFileName)
                renderSingleCard(context, brand, pageSpec, fonts, thumbCache, targetFile)
                results.add(targetFile)
            }
            return results
        } finally {
            thumbCache.values.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    fun saveToGallery(context: Context, imageFiles: List<File>): List<String> {
        val savedPaths = mutableListOf<String>()
        for (src in imageFiles) {
            if (!src.isFile || src.length() <= 0L) continue
            val path = saveSinglePngToGallery(context, src)
            savedPaths.add(path)
        }
        return savedPaths
    }

    private fun saveSinglePngToGallery(context: Context, srcFile: File): String {
        val displayName = "${srcFile.nameWithoutExtension}_${System.currentTimeMillis() % 10000}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AdvanceERP")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(srcFile).use { input ->
                        input.copyTo(out)
                        out.flush()
                    }
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return "Pictures/AdvanceERP/$displayName"
            }
        }

        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val folder = File(picturesDir, "AdvanceERP").apply { mkdirs() }
        val dst = File(folder, displayName)
        FileInputStream(srcFile).use { input ->
            FileOutputStream(dst).use { out ->
                input.copyTo(out)
                out.flush()
            }
        }
        MediaScannerConnection.scanFile(context, arrayOf(dst.absolutePath), arrayOf("image/png"), null)
        return "Pictures/AdvanceERP/$displayName"
    }

    private class CardFontSet(
        val titleBold14: TextPaint,
        val subtitleReg95: TextPaint,
        val greetingSalutation10: TextPaint,
        val greetingRecipient115: TextPaint,
        val greetingBody95: TextPaint,
        val sectionTitle10: TextPaint,
        val itemTitle10: TextPaint,
        val itemMeta85: TextPaint,
        val itemPriceRight105: TextPaint,
        val metricLabel9: TextPaint,
        val metricValue95: TextPaint,
        val smallCardTitle9: TextPaint,
        val smallCardLabel85: TextPaint,
        val smallCardVal9: TextPaint,
        val heroLabel105: TextPaint,
        val heroAmount20: TextPaint,
        val notice9: TextPaint,
        val thankYou95: TextPaint,
        val chipText8: TextPaint,
        val chipSubText75: TextPaint
    ) {
        companion object {
            fun create(context: Context): CardFontSet = CardFontSet(
                titleBold14 = BrandedTypefaceProvider.textPaint(context, 14f * SCALE, BrandedDocumentPalette.BrandPurpleDeep, DocumentFontWeight.BOLD),
                subtitleReg95 = BrandedTypefaceProvider.textPaint(context, 9.5f * SCALE, BrandedDocumentPalette.TextMuted, DocumentFontWeight.MEDIUM),
                greetingSalutation10 = BrandedTypefaceProvider.textPaint(context, 9.8f * SCALE, BrandedDocumentPalette.TextSecondary, DocumentFontWeight.REGULAR),
                greetingRecipient115 = BrandedTypefaceProvider.textPaint(context, 11.2f * SCALE, BrandedDocumentPalette.TextPrimary, DocumentFontWeight.BOLD),
                greetingBody95 = BrandedTypefaceProvider.textPaint(context, 9.3f * SCALE, BrandedDocumentPalette.TextSecondary, DocumentFontWeight.REGULAR),
                sectionTitle10 = BrandedTypefaceProvider.textPaint(context, 10f * SCALE, BrandedDocumentPalette.BrandPurpleDeep, DocumentFontWeight.BOLD),
                itemTitle10 = BrandedTypefaceProvider.textPaint(context, 9.8f * SCALE, BrandedDocumentPalette.TextPrimary, DocumentFontWeight.BOLD),
                itemMeta85 = BrandedTypefaceProvider.textPaint(context, 8.3f * SCALE, BrandedDocumentPalette.TextMuted, DocumentFontWeight.REGULAR),
                itemPriceRight105 = BrandedTypefaceProvider.textPaint(context, 10.5f * SCALE, BrandedDocumentPalette.TextPrimary, DocumentFontWeight.BOLD),
                metricLabel9 = BrandedTypefaceProvider.textPaint(context, 9.2f * SCALE, BrandedDocumentPalette.TextSecondary, DocumentFontWeight.MEDIUM),
                metricValue95 = BrandedTypefaceProvider.textPaint(context, 9.6f * SCALE, BrandedDocumentPalette.TextPrimary, DocumentFontWeight.BOLD),
                smallCardTitle9 = BrandedTypefaceProvider.textPaint(context, 9f * SCALE, BrandedDocumentPalette.BrandPurpleDeep, DocumentFontWeight.BOLD),
                smallCardLabel85 = BrandedTypefaceProvider.textPaint(context, 8.4f * SCALE, BrandedDocumentPalette.TextSecondary, DocumentFontWeight.REGULAR),
                smallCardVal9 = BrandedTypefaceProvider.textPaint(context, 8.8f * SCALE, BrandedDocumentPalette.TextPrimary, DocumentFontWeight.BOLD),
                heroLabel105 = BrandedTypefaceProvider.textPaint(context, 10.5f * SCALE, BrandedDocumentPalette.TextPrimary, DocumentFontWeight.BOLD),
                heroAmount20 = BrandedTypefaceProvider.textPaint(context, 20f * SCALE, BrandedDocumentPalette.DueRedText, DocumentFontWeight.BOLD),
                notice9 = BrandedTypefaceProvider.textPaint(context, 9f * SCALE, BrandedDocumentPalette.TextSecondary, DocumentFontWeight.MEDIUM),
                thankYou95 = BrandedTypefaceProvider.textPaint(context, 9.3f * SCALE, BrandedDocumentPalette.TextPrimary, DocumentFontWeight.BOLD),
                chipText8 = BrandedTypefaceProvider.textPaint(context, 8f * SCALE, BrandedDocumentPalette.BrandPurpleDeep, DocumentFontWeight.BOLD),
                chipSubText75 = BrandedTypefaceProvider.textPaint(context, 7.2f * SCALE, BrandedDocumentPalette.TextPrimary, DocumentFontWeight.MEDIUM)
            )
        }
    }

    private data class MeasuredSection(
        val section: ImageCardBodySection,
        val height: Float,
        val extraHeights: List<Float> = emptyList(),
        val layouts: List<StaticLayout> = emptyList()
    )

    private fun renderSingleCard(
        context: Context,
        brand: CompanyBrandInfo,
        spec: BrandedImageCardPageSpec,
        fonts: CardFontSet,
        thumbCache: Map<String, Bitmap>,
        outputFile: File
    ) {
        val cardLeft = OUTER_MARGIN
        val cardRight = BITMAP_WIDTH - OUTER_MARGIN
        val cardW = cardRight - cardLeft // 1020px
        val contentLeft = cardLeft + INNER_PAD_X
        val contentW = cardW - INNER_PAD_X * 2f // 924px

        // Pass 1: Measure dynamic portrait card height
        var y = OUTER_MARGIN + HEADER_HEIGHT + 12f * SCALE

        val titleBlockH = 38f * SCALE
        y += titleBlockH + 8f * SCALE

        val greetingIntroLayout = spec.greeting?.let { g ->
            BrandedTextLayout.layoutMultiLine(g.introParagraph, fonts.greetingBody95, contentW.toInt(), lineSpacingMult = 1.15f)
        }
        val greetingH = spec.greeting?.let {
            13f * SCALE + 15f * SCALE + (greetingIntroLayout?.height ?: 0).toFloat() + 6f * SCALE
        } ?: 0f
        y += greetingH

        val measuredSections = spec.sections.map { sec -> measureSection(sec, contentW, fonts) }
        measuredSections.forEach { ms ->
            y += ms.height + SECTION_GAP
        }

        val thankYouLayout = spec.thankYouText?.let { ty ->
            BrandedTextLayout.layoutMultiLine(ty, fonts.thankYou95, (contentW - 36f * SCALE).toInt(), lineSpacingMult = 1.12f)
        }
        val thankYouH = thankYouLayout?.let { maxOf(22f * SCALE, it.height.toFloat()) + 10f * SCALE } ?: 0f
        y += thankYouH

        y += 4f * SCALE + FOOTER_HEIGHT + OUTER_MARGIN
        val totalBitmapHeight = y.toInt().coerceAtLeast((HEADER_HEIGHT + FOOTER_HEIGHT + 240f * SCALE).toInt())

        // Pass 2: Draw high-DPI Bitmap
        val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, totalBitmapHeight, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(BrandedDocumentPalette.CardOuterBackdrop)

            val cardRect = RectF(cardLeft, OUTER_MARGIN, cardRight, totalBitmapHeight - OUTER_MARGIN)
            // Soft outer elevation ring
            val shadowRect = RectF(cardRect.left - 3f, cardRect.top - 2f, cardRect.right + 3f, cardRect.bottom + 5f)
            canvas.drawRoundRect(
                shadowRect,
                CARD_CORNER_RADIUS + 4f,
                CARD_CORNER_RADIUS + 4f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(26, 59, 14, 104) }
            )
            BrandedCanvasComponents.drawSectionCard(
                canvas = canvas,
                rect = cardRect,
                radiusPx = CARD_CORNER_RADIUS,
                fillColor = BrandedDocumentPalette.PageWhite,
                borderColor = BrandedDocumentPalette.SectionBoxBorder,
                borderWidthPx = 1.2f * SCALE
            )

            // Top Brand Wave Header
            val headerRect = RectF(cardLeft, cardRect.top, cardRight, cardRect.top + HEADER_HEIGHT)
            BrandedCanvasComponents.drawWaveHeaderBanner(
                canvas = canvas,
                context = context,
                bounds = headerRect,
                brand = brand,
                scale = SCALE,
                cornerRadiusTopPx = CARD_CORNER_RADIUS,
                rightTitleTop = null,
                rightTitleBottom = null,
                rightIcon = null
            )

            var curY = headerRect.bottom + 12f * SCALE

            // Title + Right Badge Row
            drawTitleAndBadgeRow(canvas, context, spec, contentLeft, curY, contentW, fonts)
            curY += titleBlockH + 8f * SCALE

            // Personalized Greeting
            spec.greeting?.let { g ->
                BrandedTextLayout.drawFittedSingleLine(
                    canvas = canvas,
                    text = g.salutationLine,
                    x = contentLeft,
                    baselineY = curY + 9.5f * SCALE,
                    maxWidthPx = contentW,
                    paint = fonts.greetingSalutation10,
                    minTextSizePx = 7f * SCALE
                )
                curY += 13f * SCALE
                BrandedTextLayout.drawFittedSingleLine(
                    canvas = canvas,
                    text = g.recipientLine,
                    x = contentLeft,
                    baselineY = curY + 11f * SCALE,
                    maxWidthPx = contentW,
                    paint = fonts.greetingRecipient115,
                    minTextSizePx = 8f * SCALE
                )
                curY += 15f * SCALE
                if (greetingIntroLayout != null) {
                    curY = BrandedTextLayout.drawLayout(canvas, greetingIntroLayout, contentLeft, curY)
                }
                curY += 6f * SCALE
            }

            // Body Sections
            for (ms in measuredSections) {
                drawSection(canvas, context, ms, contentLeft, curY, contentW, fonts, thumbCache)
                curY += ms.height + SECTION_GAP
            }

            // Thank You Row
            if (thankYouLayout != null) {
                val iconSz = 18f * SCALE
                val blockH = maxOf(iconSz, thankYouLayout.height.toFloat())
                BrandedVectorIcons.drawIcon(
                    canvas = canvas,
                    type = BrandedIconType.HANDSHAKE,
                    centerX = contentLeft + 10f * SCALE,
                    centerY = curY + blockH / 2f,
                    sizePx = iconSz,
                    color = BrandedDocumentPalette.BrandPurplePrimary
                )
                BrandedTextLayout.drawLayout(
                    canvas = canvas,
                    layout = thankYouLayout,
                    x = contentLeft + 26f * SCALE,
                    y = curY + (blockH - thankYouLayout.height) / 2f
                )
            }

            // Bottom Brand Wave Footer
            val footerRect = RectF(cardLeft, cardRect.bottom - FOOTER_HEIGHT, cardRight, cardRect.bottom)
            BrandedCanvasComponents.drawCardWaveFooter(
                canvas = canvas,
                context = context,
                bounds = footerRect,
                phone = spec.footerPhone,
                address = spec.footerAddress,
                cornerRadiusBottomPx = CARD_CORNER_RADIUS,
                scale = SCALE
            )

            // Atomic PNG output write
            outputFile.parentFile?.mkdirs()
            val tmpFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
            FileOutputStream(tmpFile).use { fos ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                    "PNG bitmap compression failed"
                }
                fos.flush()
            }
            if (outputFile.exists()) outputFile.delete()
            check(tmpFile.renameTo(outputFile)) { "Failed to persist PNG file" }
        } finally {
            bitmap.recycle()
        }
    }

    private fun drawTitleAndBadgeRow(
        canvas: Canvas,
        context: Context,
        spec: BrandedImageCardPageSpec,
        left: Float,
        topY: Float,
        contentW: Float,
        fonts: CardFontSet
    ) {
        val rightEdge = left + contentW
        val badge = spec.rightBadge
        val hasBadge = badge.topChipText != null || badge.statusBadge != null
        val badgeReserveW = if (hasBadge) contentW * 0.42f else 0f
        val leftMaxW = (contentW - badgeReserveW - 8f * SCALE).coerceAtLeast(contentW * 0.50f)

        val iconSize = 26f * SCALE
        BrandedCanvasComponents.drawSectionCard(
            canvas = canvas,
            rect = RectF(left, topY + 2f * SCALE, left + iconSize, topY + 2f * SCALE + iconSize),
            radiusPx = 7f * SCALE,
            fillColor = BrandedDocumentPalette.SectionBoxAltBg,
            borderColor = BrandedDocumentPalette.SectionBoxBorder,
            borderWidthPx = 1f * SCALE
        )
        BrandedVectorIcons.drawIcon(
            canvas = canvas,
            type = spec.titleIcon,
            centerX = left + iconSize / 2f,
            centerY = topY + 2f * SCALE + iconSize / 2f,
            sizePx = iconSize * 0.62f,
            color = BrandedDocumentPalette.BrandPurplePrimary
        )

        val textX = left + iconSize + 8f * SCALE
        val textMaxW = (leftMaxW - iconSize - 8f * SCALE).coerceAtLeast(40f * SCALE)
        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = spec.titlePrimary,
            x = textX,
            baselineY = topY + 15f * SCALE,
            maxWidthPx = textMaxW,
            paint = fonts.titleBold14,
            minTextSizePx = 9f * SCALE
        )
        if (spec.titleSecondary.isNotBlank()) {
            BrandedTextLayout.drawFittedSingleLine(
                canvas = canvas,
                text = spec.titleSecondary,
                x = textX,
                baselineY = topY + 28f * SCALE,
                maxWidthPx = textMaxW,
                paint = fonts.subtitleReg95,
                minTextSizePx = 7f * SCALE
            )
        }

        var badgeY = topY
        if (!badge.topChipText.isNullOrBlank()) {
            if (!badge.bottomSubText.isNullOrBlank()) {
                // Two-line period badge (REF-05)
                val chipW = (contentW * 0.41f).coerceAtLeast(126f * SCALE)
                val chipRect = RectF(rightEdge - chipW, badgeY, rightEdge, badgeY + 32f * SCALE)
                BrandedCanvasComponents.drawSectionCard(
                    canvas = canvas,
                    rect = chipRect,
                    radiusPx = 8f * SCALE,
                    fillColor = BrandedDocumentPalette.SectionBoxAltBg,
                    borderColor = BrandedDocumentPalette.SectionBoxBorder,
                    borderWidthPx = 1f * SCALE
                )
                val padH = 7f * SCALE
                var headerTextX = chipRect.left + padH
                if (badge.topChipIcon != null) {
                    BrandedVectorIcons.drawIcon(
                        canvas = canvas,
                        type = badge.topChipIcon,
                        centerX = headerTextX + 5f * SCALE,
                        centerY = chipRect.top + 10.5f * SCALE,
                        sizePx = 9.5f * SCALE,
                        color = BrandedDocumentPalette.BrandPurplePrimary
                    )
                    headerTextX += 13f * SCALE
                }
                BrandedTextLayout.drawFittedSingleLine(
                    canvas = canvas,
                    text = badge.topChipText,
                    x = headerTextX,
                    baselineY = chipRect.top + 13f * SCALE,
                    maxWidthPx = chipRect.right - padH - headerTextX,
                    paint = fonts.chipText8,
                    minTextSizePx = 6f * SCALE
                )
                BrandedTextLayout.drawFittedSingleLine(
                    canvas = canvas,
                    text = badge.bottomSubText,
                    x = chipRect.centerX(),
                    baselineY = chipRect.top + 25f * SCALE,
                    maxWidthPx = chipRect.width() - padH * 1.5f,
                    paint = fonts.chipSubText75,
                    align = Paint.Align.CENTER,
                    minTextSizePx = 5.5f * SCALE
                )
                badgeY = chipRect.bottom + 4f * SCALE
            } else {
                // Compact doc number chip (REF-02)
                val textW = fonts.chipText8.measureText(badge.topChipText)
                val chipW = (textW + 14f * SCALE).coerceAtMost(contentW * 0.42f)
                val chipRect = RectF(rightEdge - chipW, badgeY, rightEdge, badgeY + 16f * SCALE)
                BrandedCanvasComponents.drawSectionCard(
                    canvas = canvas,
                    rect = chipRect,
                    radiusPx = 8f * SCALE,
                    fillColor = BrandedDocumentPalette.SectionBoxAltBg,
                    borderColor = BrandedDocumentPalette.SectionBoxBorder,
                    borderWidthPx = 0.9f * SCALE
                )
                BrandedTextLayout.drawFittedSingleLine(
                    canvas = canvas,
                    text = badge.topChipText,
                    x = chipRect.centerX(),
                    baselineY = chipRect.centerY() + 2.8f * SCALE,
                    maxWidthPx = chipRect.width() - 8f * SCALE,
                    paint = fonts.chipText8,
                    align = Paint.Align.CENTER,
                    minTextSizePx = 6f * SCALE
                )
                badgeY = chipRect.bottom + 4f * SCALE
            }
        }

        badge.statusBadge?.let { status ->
            BrandedCanvasComponents.drawStatusPill(
                canvas = canvas,
                context = context,
                anchorX = rightEdge,
                topY = badgeY + if (badge.topChipText == null) 4f * SCALE else 0f,
                badge = status,
                scale = SCALE * 0.88f,
                alignRight = true
            )
        }
    }

    private fun measureSection(
        section: ImageCardBodySection,
        contentW: Float,
        fonts: CardFontSet
    ): MeasuredSection {
        return when (section) {
            is ImageCardBodySection.ReferencePillSection -> {
                val h = if (!section.secondaryLine.isNullOrBlank()) 44f * SCALE else 34f * SCALE
                MeasuredSection(section, h)
            }
            is ImageCardBodySection.HeroAmountSection -> {
                val h = if (!section.subtitle.isNullOrBlank()) 62f * SCALE else 52f * SCALE
                MeasuredSection(section, h)
            }
            is ImageCardBodySection.ItemsListSection -> {
                val hdrH = 20f * SCALE
                val innerPad = 10f * SCALE
                val itemHeights = mutableListOf<Float>()
                val itemLayouts = mutableListOf<StaticLayout>()
                val rightPriceReserve = 76f * SCALE
                var boxInnerH = innerPad
                for (item in section.items) {
                    val thumbReserve = if (item.showThumbnailSlot) 44f * SCALE else 0f
                    val availTitleW = (contentW - innerPad * 2f - thumbReserve - rightPriceReserve).coerceAtLeast(90f * SCALE).toInt()
                    val tLayout = BrandedTextLayout.layoutMultiLine(item.title, fonts.itemTitle10, availTitleW, lineSpacingMult = 1.08f, maxLines = 2)
                    itemLayouts.add(tLayout)
                    var rowH = tLayout.height.toFloat()
                    if (!item.variantAndSkuLine.isNullOrBlank()) rowH += 12f * SCALE
                    rowH += 12f * SCALE // qty & price
                    if (!item.discountBadgeText.isNullOrBlank()) rowH += 11f * SCALE
                    if (!item.directionTagText.isNullOrBlank()) rowH += 11f * SCALE
                    val minThumbH = if (item.showThumbnailSlot) 40f * SCALE else 28f * SCALE
                    val finalRowH = maxOf(minThumbH, rowH) + 8f * SCALE
                    itemHeights.add(finalRowH)
                    boxInnerH += finalRowH
                }
                boxInnerH += innerPad * 0.4f
                MeasuredSection(section, hdrH + boxInnerH, extraHeights = itemHeights, layouts = itemLayouts)
            }
            is ImageCardBodySection.MetricListCardSection -> {
                val hasHeader = !section.title.isNullOrBlank()
                val hdrH = if (hasHeader) 20f * SCALE else 0f
                val rowH = 20f * SCALE
                val padV = 10f * SCALE
                val totalCardH = padV * 2f + section.rows.size * rowH
                MeasuredSection(section, hdrH + totalCardH)
            }
            is ImageCardBodySection.SplitSummaryCardsSection -> {
                val cardHdrH = 24f * SCALE
                val rowH = 16.5f * SCALE
                val leftH = cardHdrH + section.leftRows.size * rowH + 8f * SCALE
                val footnoteH = if (!section.rightFootnote.isNullOrBlank()) 11f * SCALE else 0f
                val rightH = cardHdrH + section.rightRows.size * rowH + footnoteH + 8f * SCALE
                MeasuredSection(section, maxOf(leftH, rightH))
            }
            is ImageCardBodySection.PurpleTotalPillSection -> {
                MeasuredSection(section, 38f * SCALE)
            }
            is ImageCardBodySection.NoticeStripSection -> {
                val textW = (contentW - 42f * SCALE).coerceAtLeast(120f * SCALE).toInt()
                val layout = BrandedTextLayout.layoutMultiLine(section.text, fonts.notice9, textW, lineSpacingMult = 1.12f)
                val h = maxOf(26f * SCALE, layout.height.toFloat() + 14f * SCALE)
                MeasuredSection(section, h, layouts = listOf(layout))
            }
        }
    }

    private fun drawSection(
        canvas: Canvas,
        context: Context,
        measured: MeasuredSection,
        left: Float,
        topY: Float,
        contentW: Float,
        fonts: CardFontSet,
        thumbCache: Map<String, Bitmap>
    ) {
        when (val sec = measured.section) {
            is ImageCardBodySection.ReferencePillSection -> {
                val rect = RectF(left, topY, left + contentW, topY + measured.height)
                BrandedCanvasComponents.drawSectionCard(
                    canvas = canvas,
                    rect = rect,
                    radiusPx = 12f * SCALE,
                    fillColor = BrandedDocumentPalette.SectionBoxAltBg,
                    borderColor = BrandedDocumentPalette.SectionBoxBorder,
                    borderWidthPx = 1f * SCALE
                )
                val iconSz = 26f * SCALE
                BrandedVectorIcons.drawTintedIconBox(
                    canvas = canvas,
                    type = sec.icon,
                    left = rect.left + 12f * SCALE,
                    top = rect.centerY() - iconSz / 2f,
                    boxSizePx = iconSz,
                    cornerRadiusPx = iconSz / 2f,
                    bgColor = Color.WHITE,
                    iconColor = BrandedDocumentPalette.BrandPurplePrimary
                )
                val textX = rect.left + 12f * SCALE + iconSz + 10f * SCALE
                val maxTextW = (rect.right - 12f * SCALE - textX).coerceAtLeast(60f * SCALE)
                BrandedTextLayout.drawFittedSingleLine(
                    canvas = canvas,
                    text = sec.primaryLine,
                    x = textX,
                    baselineY = if (!sec.secondaryLine.isNullOrBlank()) rect.centerY() - 2f * SCALE else rect.centerY() + 3.5f * SCALE,
                    maxWidthPx = maxTextW,
                    paint = fonts.itemTitle10,
                    minTextSizePx = 7.5f * SCALE
                )
                sec.secondaryLine?.takeIf { it.isNotBlank() }?.let { sub ->
                    BrandedTextLayout.drawFittedSingleLine(
                        canvas = canvas,
                        text = sub,
                        x = textX,
                        baselineY = rect.centerY() + 11.5f * SCALE,
                        maxWidthPx = maxTextW,
                        paint = fonts.itemMeta85,
                        minTextSizePx = 6.5f * SCALE
                    )
                }
            }

            is ImageCardBodySection.HeroAmountSection -> {
                val rect = RectF(left, topY, left + contentW, topY + measured.height)
                BrandedCanvasComponents.drawSectionCard(
                    canvas = canvas,
                    rect = rect,
                    radiusPx = 14f * SCALE,
                    fillColor = sec.bgColor,
                    borderColor = sec.borderColor,
                    borderWidthPx = 1.2f * SCALE
                )
                val iconSz = 28f * SCALE
                BrandedVectorIcons.drawTintedIconBox(
                    canvas = canvas,
                    type = sec.icon,
                    left = rect.left + 14f * SCALE,
                    top = rect.centerY() - iconSz / 2f,
                    boxSizePx = iconSz,
                    cornerRadiusPx = 14f * SCALE,
                    bgColor = Color.WHITE,
                    iconColor = BrandedDocumentPalette.BrandPurplePrimary
                )
                val valAreaLeft = rect.left + 50f * SCALE
                val valCenterX = (valAreaLeft + rect.right - 12f * SCALE) / 2f
                val availW = (rect.right - 12f * SCALE - valAreaLeft).coerceAtLeast(80f * SCALE)
                BrandedTextLayout.drawFittedSingleLine(
                    canvas = canvas,
                    text = sec.label,
                    x = valCenterX,
                    baselineY = rect.top + 16f * SCALE,
                    maxWidthPx = availW,
                    paint = fonts.heroLabel105,
                    align = Paint.Align.CENTER,
                    minTextSizePx = 7.5f * SCALE
                )
                val valPaint = TextPaint(fonts.heroAmount20).apply { color = sec.amountColor }
                BrandedTextLayout.drawFittedSingleLine(
                    canvas = canvas,
                    text = sec.amountText,
                    x = valCenterX,
                    baselineY = rect.top + 41f * SCALE,
                    maxWidthPx = availW,
                    paint = valPaint,
                    align = Paint.Align.CENTER,
                    minTextSizePx = 12f * SCALE
                )
                sec.subtitle?.takeIf { it.isNotBlank() }?.let { sub ->
                    BrandedTextLayout.drawFittedSingleLine(
                        canvas = canvas,
                        text = sub,
                        x = valCenterX,
                        baselineY = rect.bottom - 6f * SCALE,
                        maxWidthPx = availW,
                        paint = fonts.itemMeta85,
                        align = Paint.Align.CENTER,
                        minTextSizePx = 6f * SCALE
                    )
                }
            }

            is ImageCardBodySection.ItemsListSection -> {
                val hdrH = 20f * SCALE
                BrandedVectorIcons.drawIcon(
                    canvas = canvas,
                    type = sec.icon,
                    centerX = left + 7f * SCALE,
                    centerY = topY + 9f * SCALE,
                    sizePx = 13f * SCALE,
                    color = BrandedDocumentPalette.BrandPurplePrimary
                )
                BrandedTextLayout.drawFittedSingleLine(
                    canvas = canvas,
                    text = sec.title,
                    x = left + 18f * SCALE,
                    baselineY = topY + 13f * SCALE,
                    maxWidthPx = contentW * 0.72f,
                    paint = fonts.sectionTitle10,
                    minTextSizePx = 8f * SCALE
                )
                sec.subtitleRight?.takeIf { it.isNotBlank() }?.let { sub ->
                    BrandedTextLayout.drawFittedSingleLine(
                        canvas = canvas,
                        text = sub,
                        x = left + contentW,
                        baselineY = topY + 12.5f * SCALE,
                        maxWidthPx = contentW * 0.25f,
                        paint = fonts.itemMeta85,
                        align = Paint.Align.RIGHT,
                        minTextSizePx = 6.5f * SCALE
                    )
                }

                val boxRect = RectF(left, topY + hdrH, left + contentW, topY + measured.height)
                BrandedCanvasComponents.drawSectionCard(
                    canvas = canvas,
                    rect = boxRect,
                    radiusPx = 12f * SCALE,
                    fillColor = BrandedDocumentPalette.SectionBoxBg,
                    borderColor = BrandedDocumentPalette.SectionBoxBorder,
                    borderWidthPx = 1f * SCALE
                )

                val innerPad = 10f * SCALE
                var ry = boxRect.top + innerPad
                sec.items.forEachIndexed { idx, item ->
                    val rowH = measured.extraHeights[idx]
                    val titleLayout = measured.layouts[idx]
                    var textLeft = boxRect.left + innerPad
                    if (item.showThumbnailSlot) {
                        val thumbSz = 36f * SCALE
                        val thumbRect = RectF(
                            boxRect.left + innerPad,
                            ry + (rowH - 8f * SCALE - thumbSz).coerceAtLeast(0f) / 2f,
                            boxRect.left + innerPad + thumbSz,
                            ry + (rowH - 8f * SCALE - thumbSz).coerceAtLeast(0f) / 2f + thumbSz
                        )
                        BrandedCanvasComponents.drawProductThumbnail(
                            canvas = canvas,
                            rect = thumbRect,
                            bitmap = item.thumbnailPath?.let { thumbCache[it] },
                            radiusPx = 7f * SCALE,
                            drawPlaceholderIfNull = true
                        )
                        textLeft = thumbRect.right + 8f * SCALE
                    }

                    var ty = ry + 2f * SCALE
                    item.directionTagText?.let { tag ->
                        val tagPaint = TextPaint(fonts.smallCardVal9).apply {
                            textSize = 7.6f * SCALE
                            color = item.directionTagColor
                        }
                        BrandedTextLayout.drawFittedSingleLine(
                            canvas = canvas,
                            text = tag,
                            x = textLeft,
                            baselineY = ty + 7.5f * SCALE,
                            maxWidthPx = 90f * SCALE,
                            paint = tagPaint,
                            minTextSizePx = 6f * SCALE
                        )
                        ty += 11f * SCALE
                    }
                    ty = BrandedTextLayout.drawLayout(canvas, titleLayout, textLeft, ty)

                    item.variantAndSkuLine?.takeIf { it.isNotBlank() }?.let { variantLine ->
                        BrandedTextLayout.drawFittedSingleLine(
                            canvas = canvas,
                            text = variantLine,
                            x = textLeft,
                            baselineY = ty + 9.5f * SCALE,
                            maxWidthPx = (boxRect.right - innerPad - textLeft - 72f * SCALE).coerceAtLeast(60f * SCALE),
                            paint = fonts.itemMeta85,
                            minTextSizePx = 6.5f * SCALE
                        )
                        ty += 12f * SCALE
                    }

                    BrandedTextLayout.drawFittedSingleLine(
                        canvas = canvas,
                        text = item.qtyAndPriceLine,
                        x = textLeft,
                        baselineY = ty + 9.5f * SCALE,
                        maxWidthPx = (boxRect.right - innerPad - textLeft - 72f * SCALE).coerceAtLeast(60f * SCALE),
                        paint = fonts.itemMeta85,
                        minTextSizePx = 6.5f * SCALE
                    )
                    ty += 12f * SCALE

                    item.discountBadgeText?.takeIf { it.isNotBlank() }?.let { disc ->
                        val discPaint = TextPaint(fonts.smallCardVal9).apply {
                            textSize = 7.6f * SCALE
                            color = BrandedDocumentPalette.DueRedText
                        }
                        BrandedTextLayout.drawFittedSingleLine(
                            canvas = canvas,
                            text = disc,
                            x = textLeft,
                            baselineY = ty + 8.5f * SCALE,
                            maxWidthPx = 120f * SCALE,
                            paint = discPaint,
                            minTextSizePx = 6f * SCALE
                        )
                    }

                    item.rightAmountText?.takeIf { it.isNotBlank() }?.let { amt ->
                        BrandedTextLayout.drawFittedSingleLine(
                            canvas = canvas,
                            text = amt,
                            x = boxRect.right - innerPad,
                            baselineY = ry + (rowH - 6f * SCALE) / 2f + 3f * SCALE,
                            maxWidthPx = 74f * SCALE,
                            paint = fonts.itemPriceRight105,
                            align = Paint.Align.RIGHT,
                            minTextSizePx = 7f * SCALE
                        )
                    }

                    ry += rowH
                    if (idx < sec.items.lastIndex) {
                        canvas.drawLine(
                            boxRect.left + innerPad,
                            ry - 3.5f * SCALE,
                            boxRect.right - innerPad,
                            ry - 3.5f * SCALE,
                            Paint().apply {
                                color = BrandedDocumentPalette.DividerSubtle
                                strokeWidth = 0.8f * SCALE
                            }
                        )
                    }
                }
            }

            is ImageCardBodySection.MetricListCardSection -> {
                val hasHeader = !sec.title.isNullOrBlank()
                val hdrH = if (hasHeader) 20f * SCALE else 0f
                if (hasHeader) {
                    sec.icon?.let { ic ->
                        BrandedVectorIcons.drawIcon(
                            canvas = canvas,
                            type = ic,
                            centerX = left + 7f * SCALE,
                            centerY = topY + 9f * SCALE,
                            sizePx = 13f * SCALE,
                            color = BrandedDocumentPalette.BrandPurplePrimary
                        )
                    }
                    BrandedTextLayout.drawFittedSingleLine(
                        canvas = canvas,
                        text = sec.title!!,
                        x = left + (if (sec.icon != null) 18f * SCALE else 2f * SCALE),
                        baselineY = topY + 13f * SCALE,
                        maxWidthPx = contentW - 20f * SCALE,
                        paint = fonts.sectionTitle10,
                        minTextSizePx = 8f * SCALE
                    )
                }

                val cardRect = RectF(left, topY + hdrH, left + contentW, topY + measured.height)
                BrandedCanvasComponents.drawSectionCard(
                    canvas = canvas,
                    rect = cardRect,
                    radiusPx = 12f * SCALE,
                    fillColor = sec.bgColor,
                    borderColor = BrandedDocumentPalette.SectionBoxBorder,
                    borderWidthPx = 1f * SCALE
                )

                val padX = 12f * SCALE
                val rowH = 20f * SCALE
                var ry = cardRect.top + 10f * SCALE
                sec.rows.forEachIndexed { idx, row ->
                    val centerY = ry + rowH / 2f
                    var labelX = cardRect.left + padX
                    if (row.icon != null) {
                        BrandedVectorIcons.drawIcon(
                            canvas = canvas,
                            type = row.icon,
                            centerX = labelX + 5.5f * SCALE,
                            centerY = centerY,
                            sizePx = 11f * SCALE,
                            color = BrandedDocumentPalette.BrandPurplePrimary
                        )
                        labelX += 16f * SCALE
                    }
                    val availW = cardRect.right - padX - labelX
                    BrandedTextLayout.drawFittedSingleLine(
                        canvas = canvas,
                        text = row.label,
                        x = labelX,
                        baselineY = centerY + 3.2f * SCALE,
                        maxWidthPx = availW * 0.60f,
                        paint = fonts.metricLabel9,
                        minTextSizePx = 6.5f * SCALE
                    )
                    val vPaint = TextPaint(fonts.metricValue95).apply {
                        color = row.valueColor
                        if (!row.boldValue) {
                            typeface = BrandedTypefaceProvider.typeface(context, DocumentFontWeight.MEDIUM)
                        }
                    }
                    BrandedTextLayout.drawFittedSingleLine(
                        canvas = canvas,
                        text = row.value,
                        x = cardRect.right - padX,
                        baselineY = centerY + 3.4f * SCALE,
                        maxWidthPx = availW * 0.39f,
                        paint = vPaint,
                        align = Paint.Align.RIGHT,
                        minTextSizePx = 7f * SCALE
                    )
                    ry += rowH
                    if (idx < sec.rows.lastIndex) {
                        canvas.drawLine(
                            cardRect.left + padX,
                            ry,
                            cardRect.right - padX,
                            ry,
                            Paint().apply {
                                color = BrandedDocumentPalette.DividerSubtle
                                strokeWidth = 0.7f * SCALE
                            }
                        )
                    }
                }
            }

            is ImageCardBodySection.SplitSummaryCardsSection -> {
                val gap = 8f * SCALE
                val colW = (contentW - gap) / 2f
                val leftRect = RectF(left, topY, left + colW, topY + measured.height)
                val rightRect = RectF(left + colW + gap, topY, left + contentW, topY + measured.height)
                drawSmallSummaryCard(canvas, sec.leftIcon, sec.leftTitle, sec.leftRows, null, leftRect, fonts)
                drawSmallSummaryCard(canvas, sec.rightIcon, sec.rightTitle, sec.rightRows, sec.rightFootnote, rightRect, fonts)
            }

            is ImageCardBodySection.PurpleTotalPillSection -> {
                val pillRect = RectF(left, topY, left + contentW, topY + measured.height)
                BrandedCanvasComponents.drawPurpleTotalPill(
                    canvas = canvas,
                    context = context,
                    rect = pillRect,
                    icon = sec.icon,
                    label = sec.label,
                    amountText = sec.amountText,
                    scale = SCALE * 0.85f
                )
            }

            is ImageCardBodySection.NoticeStripSection -> {
                val rect = RectF(left, topY, left + contentW, topY + measured.height)
                BrandedCanvasComponents.drawSectionCard(
                    canvas = canvas,
                    rect = rect,
                    radiusPx = 10f * SCALE,
                    fillColor = sec.bgColor,
                    borderColor = sec.borderColor,
                    borderWidthPx = 0.9f * SCALE
                )
                BrandedVectorIcons.drawIcon(
                    canvas = canvas,
                    type = sec.icon,
                    centerX = rect.left + 16f * SCALE,
                    centerY = rect.centerY(),
                    sizePx = 13f * SCALE,
                    color = sec.iconAndTextColor
                )
                measured.layouts.firstOrNull()?.let { textLayout ->
                    // Paint color matches semantic strip
                    val paintCopy = TextPaint(fonts.notice9).apply { color = sec.iconAndTextColor }
                    val coloredLayout = BrandedTextLayout.layoutMultiLine(sec.text, paintCopy, textLayout.width, lineSpacingMult = 1.12f)
                    BrandedTextLayout.drawLayout(
                        canvas = canvas,
                        layout = coloredLayout,
                        x = rect.left + 28f * SCALE,
                        y = rect.top + (rect.height() - coloredLayout.height) / 2f
                    )
                }
            }
        }
    }

    private fun drawSmallSummaryCard(
        canvas: Canvas,
        icon: BrandedIconType,
        title: String,
        rows: List<ImageCardMetricRowSpec>,
        footnote: String?,
        rect: RectF,
        fonts: CardFontSet
    ) {
        BrandedCanvasComponents.drawSectionCard(
            canvas = canvas,
            rect = rect,
            radiusPx = 12f * SCALE,
            fillColor = BrandedDocumentPalette.SectionBoxBg,
            borderColor = BrandedDocumentPalette.SectionBoxBorder,
            borderWidthPx = 1f * SCALE
        )
        val pad = 9f * SCALE
        BrandedVectorIcons.drawIcon(
            canvas = canvas,
            type = icon,
            centerX = rect.left + pad + 5f * SCALE,
            centerY = rect.top + 11.5f * SCALE,
            sizePx = 10.5f * SCALE,
            color = BrandedDocumentPalette.BrandPurplePrimary
        )
        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = title,
            x = rect.left + pad + 14f * SCALE,
            baselineY = rect.top + 14.5f * SCALE,
            maxWidthPx = rect.width() - pad * 2f - 15f * SCALE,
            paint = fonts.smallCardTitle9,
            minTextSizePx = 6.8f * SCALE
        )
        canvas.drawLine(
            rect.left + pad,
            rect.top + 20f * SCALE,
            rect.right - pad,
            rect.top + 20f * SCALE,
            Paint().apply {
                color = BrandedDocumentPalette.DividerSubtle
                strokeWidth = 0.8f * SCALE
            }
        )

        var ry = rect.top + 32f * SCALE
        val innerW = rect.width() - pad * 2f
        for (row in rows) {
            BrandedTextLayout.drawFittedSingleLine(
                canvas = canvas,
                text = row.label,
                x = rect.left + pad,
                baselineY = ry,
                maxWidthPx = innerW * 0.50f,
                paint = fonts.smallCardLabel85,
                minTextSizePx = 6f * SCALE
            )
            val valPaint = TextPaint(fonts.smallCardVal9).apply {
                color = row.valueColor
            }
            BrandedTextLayout.drawFittedSingleLine(
                canvas = canvas,
                text = row.value,
                x = rect.right - pad,
                baselineY = ry,
                maxWidthPx = innerW * 0.48f,
                paint = valPaint,
                align = Paint.Align.RIGHT,
                minTextSizePx = 6f * SCALE
            )
            ry += 16.5f * SCALE
        }

        footnote?.takeIf { it.isNotBlank() }?.let { note ->
            BrandedTextLayout.drawFittedSingleLine(
                canvas = canvas,
                text = note,
                x = rect.left + pad,
                baselineY = rect.bottom - 5.5f * SCALE,
                maxWidthPx = innerW,
                paint = fonts.itemMeta85.let { TextPaint(it).apply { textSize = 6.8f * SCALE } },
                minTextSizePx = 5.5f * SCALE
            )
        }
    }
}
