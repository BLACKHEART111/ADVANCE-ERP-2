package com.pocketdev.advance_erp.core.communication.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.pdf.PdfDocument
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
import com.pocketdev.advance_erp.core.util.CompanyBrandingHelper
import com.pocketdev.advance_erp.core.util.QrCodeGenerator
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import java.io.File
import java.io.FileOutputStream

/**
 * Offline A4 PDF renderer implementing the approved TUKTAK / ADVANCE ERP wave-branded design
 * across all communication modules with HarfBuzz BN+EN shaping, multi-page repeated headers,
 * QR verification, and atomic FileOutputStream persistence.
 */
object BrandedCommunicationPdfGenerator {

    private const val PAGE_WIDTH = BrandedPdfSpecBuilder.A4_WIDTH_PT
    private const val PAGE_HEIGHT = BrandedPdfSpecBuilder.A4_HEIGHT_PT
    private const val MARGIN_X = BrandedPdfSpecBuilder.MARGIN_X_PT
    private const val CONTENT_W = BrandedPdfSpecBuilder.CONTENT_WIDTH_PT
    private const val FOOTER_H = 36f
    private const val USABLE_BOTTOM_Y = PAGE_HEIGHT - FOOTER_H - 10f
    private const val HEADER_H = 66f
    private const val CONT_HEADER_H = 34f
    private const val CARD_GAP = 12f
    private const val COL_CARD_W = (CONTENT_W - CARD_GAP) / 2f

    fun generate(
        context: Context,
        document: SafeDocument,
        settings: CommunicationSettings,
        language: String,
        outputFile: File
    ): File {
        val spec = BrandedPdfSpecBuilder.buildSpec(document, settings, language)
        val brand = document.brand ?: CompanyBrandInfo(companyName = "ADVANCE ERP")
        val fonts = PdfFontSet.create(context)

        val contactStripLayout = measureContactStrip(spec.contactStrip, fonts)
        val infoCardH = maxOf(
            measureInfoCardHeight(spec.leftInfoCard, COL_CARD_W, fonts),
            measureInfoCardHeight(spec.rightInfoCard, COL_CARD_W, fonts)
        )
        val heroH = if (spec.heroBanner != null) 46f else 0f
        val kpiH = if (spec.kpiStrip.isNotEmpty()) 38f else 0f

        var firstPageTopH = HEADER_H + 8f
        if (contactStripLayout != null) firstPageTopH += contactStripLayout.height + 8f
        firstPageTopH += infoCardH + 8f
        if (heroH > 0f) firstPageTopH += heroH + 8f
        if (kpiH > 0f) firstPageTopH += kpiH + 8f

        val contPageTopH = CONT_HEADER_H + 10f

        val hasTableTitle = !spec.tableTitle.isNullOrBlank() && spec.tableRows.isNotEmpty()
        val hasEmptyNotice = spec.tableRows.isEmpty() && !spec.emptyTableMessage.isNullOrBlank()
        if (hasEmptyNotice) {
            firstPageTopH += 28f + 8f
        }

        val hasGroupedCols = spec.tableColumns.any { it.groupHeaderTitle != null }
        val tableHeaderH = if (spec.tableColumns.isNotEmpty() && spec.tableRows.isNotEmpty()) {
            if (hasGroupedCols) 28f else 22f
        } else 0f
        val sectionTitleH = if (hasTableTitle) 18f else 0f

        val measuredRows = measureTableRows(spec, fonts)
        val rowHeights = measuredRows.map { it.height }

        val summaryH = maxOf(
            measureSummaryCardHeight(spec.leftSummaryCard, COL_CARD_W, fonts),
            spec.rightSummaryCard?.let { measureSummaryCardHeight(it, COL_CARD_W, fonts) } ?: 0f
        )

        val bottomMeasured = measureBottomBlock(spec.bottomBlock, fonts)

        val slices = BrandedPdfSpecBuilder.paginateDocument(
            rowHeights = rowHeights,
            firstPageTopHeight = firstPageTopH,
            contPageTopHeight = contPageTopH,
            sectionTitleHeight = sectionTitleH,
            tableHeaderHeight = tableHeaderH,
            summaryHeight = summaryH,
            bottomHeight = bottomMeasured.totalHeight,
            usableBottomY = USABLE_BOTTOM_Y
        )

        val thumbnailCache = mutableMapOf<String, Bitmap>()
        var qrBitmap: Bitmap? = null
        val pdfDocument = PdfDocument()

        try {
            if (settings.showProductPhoto) {
                spec.tableRows.mapNotNull { it.thumbnailPath }.distinct().forEach { path ->
                    BrandedCanvasComponents.loadProductThumbnailBitmap(path, targetPx = 96)?.let { bmp ->
                        thumbnailCache[path] = bmp
                    }
                }
            }
            spec.bottomBlock.qrPayload?.let { payload ->
                qrBitmap = QrCodeGenerator.generateQrBitmap(
                    content = payload,
                    targetSize = 256,
                    darkColor = BrandedDocumentPalette.TextPrimary,
                    lightColor = Color.WHITE
                )
            }

            val totalPages = slices.size.coerceAtLeast(1)
            for (slice in slices) {
                val pageInfo = PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH.toInt(),
                    PAGE_HEIGHT.toInt(),
                    slice.pageNumber
                ).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(BrandedDocumentPalette.PageWhite)

                var y: Float
                if (slice.isFirstPage) {
                    BrandedCanvasComponents.drawWaveHeaderBanner(
                        canvas = canvas,
                        context = context,
                        bounds = RectF(0f, 0f, PAGE_WIDTH, HEADER_H),
                        brand = brand,
                        scale = 1.0f,
                        cornerRadiusTopPx = 0f,
                        rightTitleTop = spec.headerRightTop,
                        rightTitleBottom = spec.headerRightBottom.takeIf { it.isNotBlank() },
                        rightIcon = spec.headerRightIcon
                    )
                    y = HEADER_H + 8f

                    if (contactStripLayout != null) {
                        drawContactStrip(canvas, spec.contactStrip, contactStripLayout, y, fonts)
                        y += contactStripLayout.height + 8f
                    }

                    drawInfoCard(canvas, spec.leftInfoCard, RectF(MARGIN_X, y, MARGIN_X + COL_CARD_W, y + infoCardH), fonts)
                    drawInfoCard(canvas, spec.rightInfoCard, RectF(MARGIN_X + COL_CARD_W + CARD_GAP, y, PAGE_WIDTH - MARGIN_X, y + infoCardH), fonts)
                    y += infoCardH + 8f

                    spec.heroBanner?.let { hero ->
                        drawHeroBanner(canvas, hero, RectF(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + heroH), fonts)
                        y += heroH + 8f
                    }

                    if (spec.kpiStrip.isNotEmpty()) {
                        drawKpiStrip(canvas, spec.kpiStrip, MARGIN_X, y, CONTENT_W, kpiH, fonts)
                        y += kpiH + 8f
                    }

                    if (hasEmptyNotice) {
                        val emptyRect = RectF(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 28f)
                        BrandedCanvasComponents.drawSectionCard(canvas, emptyRect, 6f)
                        BrandedTextLayout.drawFittedSingleLine(
                            canvas = canvas,
                            text = spec.emptyTableMessage!!,
                            x = emptyRect.centerX(),
                            baselineY = emptyRect.centerY() + 3f,
                            maxWidthPx = CONTENT_W - 20f,
                            paint = fonts.bodyMuted8,
                            align = Paint.Align.CENTER,
                            minTextSizePx = 6.5f
                        )
                        y += 36f
                    }
                } else {
                    drawContinuationHeader(canvas, brand, spec, fonts)
                    y = contPageTopH
                }

                if (slice.rowEndExclusive > slice.rowStartIndex) {
                    if (hasTableTitle) {
                        y = drawSectionHeading(
                            canvas = canvas,
                            title = if (slice.isFirstPage) spec.tableTitle!! else "${spec.tableTitle!!} (${if (spec.isBangla) "চলমান" else "Continued"})",
                            x = MARGIN_X,
                            topY = y,
                            fonts = fonts
                        )
                    }
                    y = drawTableHeader(canvas, spec.tableColumns, y, tableHeaderH, fonts)
                    for (idx in slice.rowStartIndex until slice.rowEndExclusive) {
                        val measured = measuredRows[idx]
                        drawTableRow(
                            canvas = canvas,
                            mode = spec.tableMode!!,
                            columns = spec.tableColumns,
                            row = spec.tableRows[idx],
                            measured = measured,
                            y = y,
                            isAlt = ((idx - slice.rowStartIndex) % 2 == 1),
                            isLastVisibleRow = (idx == slice.rowEndExclusive - 1),
                            thumbnailCache = thumbnailCache,
                            fonts = fonts
                        )
                        y += measured.height
                    }
                    y += 8f
                }

                if (slice.includeSummarySection) {
                    val leftRect = RectF(
                        MARGIN_X, y,
                        if (spec.rightSummaryCard != null) MARGIN_X + COL_CARD_W else PAGE_WIDTH - MARGIN_X,
                        y + summaryH
                    )
                    drawSummaryCard(canvas, context, spec.leftSummaryCard, leftRect, fonts)
                    spec.rightSummaryCard?.let { rightCard ->
                        val rightRect = RectF(MARGIN_X + COL_CARD_W + CARD_GAP, y, PAGE_WIDTH - MARGIN_X, y + summaryH)
                        drawSummaryCard(canvas, context, rightCard, rightRect, fonts)
                    }
                    y += summaryH + 8f
                }

                if (slice.includeBottomSection) {
                    drawBottomBlock(
                        canvas = canvas,
                        context = context,
                        block = spec.bottomBlock,
                        measured = bottomMeasured,
                        topY = y,
                        qrBitmap = qrBitmap,
                        fonts = fonts
                    )
                }

                BrandedCanvasComponents.drawPdfWaveFooter(
                    canvas = canvas,
                    context = context,
                    pageWidth = PAGE_WIDTH,
                    pageHeight = PAGE_HEIGHT,
                    footerHeight = FOOTER_H,
                    generatedOnText = spec.generatedOnText,
                    pageNumber = slice.pageNumber,
                    totalPages = totalPages
                )

                pdfDocument.finishPage(page)
            }

            outputFile.parentFile?.mkdirs()
            val tmpFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
            FileOutputStream(tmpFile).use { fos ->
                pdfDocument.writeTo(fos)
                fos.flush()
            }
            if (outputFile.exists()) outputFile.delete()
            check(tmpFile.renameTo(outputFile)) { "Failed to persist PDF output file" }
            return outputFile
        } finally {
            pdfDocument.close()
            thumbnailCache.values.forEach { if (!it.isRecycled) it.recycle() }
            if (qrBitmap != null && !qrBitmap!!.isRecycled) qrBitmap!!.recycle()
        }
    }

    // region Font Bundle

    private class PdfFontSet(
        val h1Bold11: TextPaint,
        val secTitle9: TextPaint,
        val cardTitle85: TextPaint,
        val bodyBold85: TextPaint,
        val bodyReg8: TextPaint,
        val bodyBold8: TextPaint,
        val bodyMuted75: TextPaint,
        val bodyMuted8: TextPaint,
        val tableHdr78: TextPaint,
        val tableHdrSub65: TextPaint,
        val cellBold8: TextPaint,
        val cellReg78: TextPaint,
        val cellSmall68: TextPaint,
        val cellDisc7: TextPaint,
        val heroVal16: TextPaint
    ) {
        companion object {
            fun create(context: Context): PdfFontSet = PdfFontSet(
                h1Bold11 = BrandedTypefaceProvider.textPaint(context, 11f, BrandedDocumentPalette.BrandPurpleDeep, DocumentFontWeight.BOLD),
                secTitle9 = BrandedTypefaceProvider.textPaint(context, 9.5f, BrandedDocumentPalette.BrandPurpleDeep, DocumentFontWeight.BOLD),
                cardTitle85 = BrandedTypefaceProvider.textPaint(context, 8.8f, BrandedDocumentPalette.BrandPurpleDeep, DocumentFontWeight.BOLD),
                bodyBold85 = BrandedTypefaceProvider.textPaint(context, 8.5f, BrandedDocumentPalette.TextPrimary, DocumentFontWeight.BOLD),
                bodyReg8 = BrandedTypefaceProvider.textPaint(context, 8f, BrandedDocumentPalette.TextSecondary, DocumentFontWeight.REGULAR),
                bodyBold8 = BrandedTypefaceProvider.textPaint(context, 8f, BrandedDocumentPalette.TextPrimary, DocumentFontWeight.BOLD),
                bodyMuted75 = BrandedTypefaceProvider.textPaint(context, 7.2f, BrandedDocumentPalette.TextMuted, DocumentFontWeight.REGULAR),
                bodyMuted8 = BrandedTypefaceProvider.textPaint(context, 8f, BrandedDocumentPalette.TextMuted, DocumentFontWeight.REGULAR),
                tableHdr78 = BrandedTypefaceProvider.textPaint(context, 7.8f, Color.WHITE, DocumentFontWeight.BOLD),
                tableHdrSub65 = BrandedTypefaceProvider.textPaint(context, 6.4f, Color.argb(230, 240, 232, 255), DocumentFontWeight.REGULAR),
                cellBold8 = BrandedTypefaceProvider.textPaint(context, 8.2f, BrandedDocumentPalette.TextPrimary, DocumentFontWeight.BOLD),
                cellReg78 = BrandedTypefaceProvider.textPaint(context, 7.8f, BrandedDocumentPalette.TextPrimary, DocumentFontWeight.REGULAR),
                cellSmall68 = BrandedTypefaceProvider.textPaint(context, 6.8f, BrandedDocumentPalette.TextMuted, DocumentFontWeight.REGULAR),
                cellDisc7 = BrandedTypefaceProvider.textPaint(context, 6.8f, BrandedDocumentPalette.DueRedText, DocumentFontWeight.BOLD),
                heroVal16 = BrandedTypefaceProvider.textPaint(context, 16f, BrandedDocumentPalette.PaymentGreenText, DocumentFontWeight.BOLD)
            )
        }
    }

    // endregion

    // region Contact Strip

    private data class ContactItem(val icon: BrandedIconType, val text: String, val width: Float)
    private data class ContactStripLayout(val rows: List<List<ContactItem>>, val height: Float, val lineHeight: Float)

    private fun measureContactStrip(strip: PdfContactStripSpec, fonts: PdfFontSet): ContactStripLayout? {
        if (strip.isEmpty) return null
        val items = mutableListOf<ContactItem>()
        val paint = fonts.bodyReg8
        val iconAndGap = 11f
        val maxSegW = CONTENT_W - 24f

        fun addIfPresent(icon: BrandedIconType, raw: String?) {
            val t = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return
            val w = (iconAndGap + paint.measureText(t)).coerceAtMost(maxSegW)
            items.add(ContactItem(icon, t, w))
        }
        addIfPresent(BrandedIconType.LOCATION_PIN, strip.address)
        addIfPresent(BrandedIconType.PHONE, strip.phone)
        addIfPresent(BrandedIconType.INFO_CIRCLE, strip.website)
        strip.binNumber?.trim()?.takeIf { it.isNotEmpty() }?.let { bin ->
            addIfPresent(BrandedIconType.INVOICE_DOC, "BIN: $bin")
        }
        if (items.isEmpty()) return null

        val rows = mutableListOf<MutableList<ContactItem>>()
        var currentRow = mutableListOf<ContactItem>()
        var currentW = 0f
        val sepW = 14f

        for (item in items) {
            val addW = if (currentRow.isEmpty()) item.width else sepW + item.width
            if (currentRow.isNotEmpty() && currentW + addW > maxSegW) {
                rows.add(currentRow)
                currentRow = mutableListOf(item)
                currentW = item.width
            } else {
                currentRow.add(item)
                currentW += addW
            }
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)

        val lineH = 13.5f
        val totalH = 8f + rows.size * lineH
        return ContactStripLayout(rows, totalH, lineH)
    }

    private fun drawContactStrip(
        canvas: Canvas,
        strip: PdfContactStripSpec,
        layout: ContactStripLayout,
        topY: Float,
        fonts: PdfFontSet
    ) {
        val rect = RectF(MARGIN_X, topY, PAGE_WIDTH - MARGIN_X, topY + layout.height)
        BrandedCanvasComponents.drawSectionCard(
            canvas = canvas,
            rect = rect,
            radiusPx = 6f,
            fillColor = BrandedDocumentPalette.SectionBoxBg,
            borderColor = BrandedDocumentPalette.SectionBoxBorder,
            borderWidthPx = 0.8f
        )

        val sepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BrandedDocumentPalette.SectionBoxBorder
            strokeWidth = 1f
        }
        val sepW = 14f
        var rowCenterY = topY + 4f + layout.lineHeight / 2f

        for (row in layout.rows) {
            val rowW = row.sumOf { it.width.toDouble() }.toFloat() + sepW * (row.size - 1).coerceAtLeast(0)
            var x = rect.centerX() - rowW / 2f
            row.forEachIndexed { idx, item ->
                if (idx > 0) {
                    val divX = x + sepW / 2f
                    canvas.drawLine(divX, rowCenterY - 4f, divX, rowCenterY + 4f, sepPaint)
                    x += sepW
                }
                BrandedVectorIcons.drawIcon(
                    canvas = canvas,
                    type = item.icon,
                    centerX = x + 4.5f,
                    centerY = rowCenterY,
                    sizePx = 8.5f,
                    color = BrandedDocumentPalette.BrandPurplePrimary
                )
                val maxTextW = (rect.right - 8f - (x + 11f)).coerceAtLeast(24f)
                BrandedTextLayout.drawFittedSingleLine(
                    canvas = canvas,
                    text = item.text,
                    x = x + 11f,
                    baselineY = rowCenterY + 2.8f,
                    maxWidthPx = maxTextW,
                    paint = fonts.bodyReg8,
                    minTextSizePx = 5.5f
                )
                x += item.width
            }
            rowCenterY += layout.lineHeight
        }
    }

    // endregion

    // region Continuation Header

    private fun drawContinuationHeader(
        canvas: Canvas,
        brand: CompanyBrandInfo,
        spec: BrandedPdfDocumentSpec,
        fonts: PdfFontSet
    ) {
        val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, PAGE_WIDTH, CONT_HEADER_H,
                BrandedDocumentPalette.BrandPurpleDeep,
                BrandedDocumentPalette.BrandPurplePrimary,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH, CONT_HEADER_H, gradPaint)
        canvas.drawRect(
            0f, CONT_HEADER_H - 2.5f, PAGE_WIDTH, CONT_HEADER_H,
            Paint().apply { color = BrandedDocumentPalette.BrandTealAccent }
        )

        val leftPaint = TextPaint(fonts.bodyBold85).apply {
            textSize = 10f
            color = Color.WHITE
        }
        val rightTitle = listOf(spec.headerRightTop, spec.leftInfoCard.lines.firstOrNull()?.value)
            .filterNotNull()
            .filter { it.isNotBlank() }
            .joinToString(" • ")

        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = brand.companyName,
            x = MARGIN_X,
            baselineY = CONT_HEADER_H * 0.62f,
            maxWidthPx = 220f,
            paint = leftPaint,
            minTextSizePx = 7.5f
        )

        val rightPaint = TextPaint(fonts.bodyReg8).apply {
            textSize = 8.5f
            color = Color.argb(235, 240, 232, 255)
        }
        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = rightTitle,
            x = PAGE_WIDTH - MARGIN_X,
            baselineY = CONT_HEADER_H * 0.60f,
            maxWidthPx = 290f,
            paint = rightPaint,
            align = Paint.Align.RIGHT,
            minTextSizePx = 6.5f
        )
    }

    // endregion

    // region Two-Column Info Cards

    private fun measureInfoCardHeight(card: PdfInfoCardSpec, width: Float, fonts: PdfFontSet): Float {
        var h = 23f // header + divider + top pad
        val innerPad = 10f
        val labelW = 76f
        val valMaxW = (width - innerPad * 2 - labelW - 10f).coerceAtLeast(60f).toInt()
        for (line in card.lines) {
            val lLayout = BrandedTextLayout.layoutMultiLine(line.label, fonts.bodyMuted75, labelW.toInt(), maxLines = 2)
            val vPaint = if (line.boldValue) fonts.bodyBold8 else fonts.bodyReg8
            val vLayout = BrandedTextLayout.layoutMultiLine(line.value, vPaint, valMaxW, lineSpacingMult = 1.08f)
            h += maxOf(lLayout.height, vLayout.height) + 3.5f
        }
        return h + 6f
    }

    private fun drawInfoCard(canvas: Canvas, card: PdfInfoCardSpec, rect: RectF, fonts: PdfFontSet) {
        BrandedCanvasComponents.drawSectionCard(
            canvas = canvas,
            rect = rect,
            radiusPx = 8f,
            fillColor = BrandedDocumentPalette.SectionBoxBg,
            borderColor = BrandedDocumentPalette.SectionBoxBorder,
            borderWidthPx = 0.9f
        )

        val pad = 10f
        BrandedVectorIcons.drawIcon(
            canvas = canvas,
            type = card.icon,
            centerX = rect.left + pad + 6f,
            centerY = rect.top + 11.5f,
            sizePx = 11.5f,
            color = BrandedDocumentPalette.BrandPurplePrimary
        )
        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = card.title,
            x = rect.left + pad + 16f,
            baselineY = rect.top + 14.5f,
            maxWidthPx = rect.width() - pad * 2 - 18f,
            paint = fonts.cardTitle85,
            minTextSizePx = 7f
        )

        val divY = rect.top + 20f
        canvas.drawLine(
            rect.left + pad, divY, rect.right - pad, divY,
            Paint().apply { color = BrandedDocumentPalette.DividerSubtle; strokeWidth = 0.8f }
        )

        var y = divY + 4f
        val labelW = 76f
        val colonX = rect.left + pad + labelW + 1f
        val valX = colonX + 6f
        val valMaxW = (rect.right - pad - valX).coerceAtLeast(48f).toInt()

        for (line in card.lines) {
            val lLayout = BrandedTextLayout.layoutMultiLine(line.label, fonts.bodyMuted75, labelW.toInt(), maxLines = 2)
            val vPaint = TextPaint(if (line.boldValue) fonts.bodyBold8 else fonts.bodyReg8).apply {
                color = line.valueColor
            }
            val vLayout = BrandedTextLayout.layoutMultiLine(line.value, vPaint, valMaxW, lineSpacingMult = 1.08f)

            BrandedTextLayout.drawLayout(canvas, lLayout, rect.left + pad, y)
            canvas.drawText(":", colonX, y + lLayout.getLineBaseline(0), fonts.bodyMuted75)
            BrandedTextLayout.drawLayout(canvas, vLayout, valX, y - 0.5f)
            y += maxOf(lLayout.height, vLayout.height) + 3.5f
        }
    }

    // endregion

    // region Hero & KPI Strip

    private fun drawHeroBanner(canvas: Canvas, hero: PdfHeroBannerSpec, rect: RectF, fonts: PdfFontSet) {
        BrandedCanvasComponents.drawSectionCard(
            canvas = canvas,
            rect = rect,
            radiusPx = 8f,
            fillColor = hero.bgColor,
            borderColor = hero.borderColor,
            borderWidthPx = 1f
        )
        val iconSz = 26f
        BrandedVectorIcons.drawTintedIconBox(
            canvas = canvas,
            type = hero.icon,
            left = rect.left + 12f,
            top = rect.centerY() - iconSz / 2f,
            boxSizePx = iconSz,
            cornerRadiusPx = 7f,
            bgColor = Color.argb(40, Color.red(hero.valueColor), Color.green(hero.valueColor), Color.blue(hero.valueColor)),
            iconColor = hero.valueColor
        )

        val textX = rect.left + 46f
        val labelPaint = TextPaint(fonts.bodyBold85).apply { color = BrandedDocumentPalette.TextPrimary }
        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = hero.label,
            x = textX,
            baselineY = if (!hero.subtitle.isNullOrBlank()) rect.centerY() - 2f else rect.centerY() + 3f,
            maxWidthPx = rect.width() * 0.54f,
            paint = labelPaint,
            minTextSizePx = 7f
        )
        hero.subtitle?.takeIf { it.isNotBlank() }?.let { sub ->
            BrandedTextLayout.drawFittedSingleLine(
                canvas = canvas,
                text = sub,
                x = textX,
                baselineY = rect.centerY() + 11f,
                maxWidthPx = rect.width() * 0.54f,
                paint = fonts.bodyMuted75,
                minTextSizePx = 6f
            )
        }

        val valPaint = TextPaint(fonts.heroVal16).apply { color = hero.valueColor }
        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = hero.primaryValue,
            x = rect.right - 14f,
            baselineY = rect.centerY() + 5.5f,
            maxWidthPx = rect.width() * 0.36f,
            paint = valPaint,
            align = Paint.Align.RIGHT,
            minTextSizePx = 10f
        )
    }

    private fun drawKpiStrip(
        canvas: Canvas,
        kpis: List<PdfKpiBoxSpec>,
        left: Float,
        top: Float,
        totalWidth: Float,
        height: Float,
        fonts: PdfFontSet
    ) {
        val gap = 6f
        val boxW = (totalWidth - gap * (kpis.size - 1)) / kpis.size
        kpis.forEachIndexed { idx, box ->
            val bx = left + idx * (boxW + gap)
            val rect = RectF(bx, top, bx + boxW, top + height)
            if (box.highlightedPurple) {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        rect.left, rect.top, rect.right, rect.bottom,
                        BrandedDocumentPalette.BrandPurpleDeep,
                        BrandedDocumentPalette.BrandPurpleBright,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(rect, 7f, 7f, p)
            } else {
                BrandedCanvasComponents.drawSectionCard(canvas, rect, 7f)
            }

            val labelPaint = TextPaint(fonts.bodyMuted75).apply {
                color = if (box.highlightedPurple) Color.argb(230, 240, 232, 255) else BrandedDocumentPalette.TextMuted
            }
            val valPaint = TextPaint(fonts.bodyBold85).apply {
                textSize = 8.8f
                color = if (box.highlightedPurple) Color.WHITE else box.valueColor
            }
            BrandedTextLayout.drawFittedSingleLine(
                canvas = canvas,
                text = box.label,
                x = rect.left + 7f,
                baselineY = rect.top + 13f,
                maxWidthPx = boxW - 14f,
                paint = labelPaint,
                minTextSizePx = 5.5f
            )
            BrandedTextLayout.drawFittedSingleLine(
                canvas = canvas,
                text = box.value,
                x = rect.left + 7f,
                baselineY = rect.top + 28f,
                maxWidthPx = boxW - 14f,
                paint = valPaint,
                minTextSizePx = 6f
            )
        }
    }

    // endregion

    // region Table Measurement & Rendering

    private data class MeasuredTableRow(
        val height: Float,
        val primaryLayout: StaticLayout?,
        val skuLayout: StaticLayout?,
        val extraCellLayouts: Map<Int, StaticLayout>
    )

    private fun drawSectionHeading(canvas: Canvas, title: String, x: Float, topY: Float, fonts: PdfFontSet): Float {
        BrandedVectorIcons.drawIcon(
            canvas = canvas,
            type = BrandedIconType.SHOPPING_CART,
            centerX = x + 6f,
            centerY = topY + 7.5f,
            sizePx = 11.5f,
            color = BrandedDocumentPalette.BrandPurplePrimary
        )
        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = title,
            x = x + 16f,
            baselineY = topY + 11f,
            maxWidthPx = CONTENT_W - 20f,
            paint = fonts.secTitle9,
            minTextSizePx = 7.5f
        )
        return topY + 18f
    }

    private fun drawTableHeader(
        canvas: Canvas,
        columns: List<PdfTableColumnSpec>,
        topY: Float,
        height: Float,
        fonts: PdfFontSet
    ): Float {
        val rect = RectF(MARGIN_X, topY, MARGIN_X + CONTENT_W, topY + height)
        val clip = Path().apply {
            addRoundRect(rect, floatArrayOf(5f, 5f, 5f, 5f, 0f, 0f, 0f, 0f), Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clip)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                BrandedDocumentPalette.TableHeaderStart,
                BrandedDocumentPalette.TableHeaderEnd,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(rect, bgPaint)

        val sepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(68, 255, 255, 255)
            strokeWidth = 0.7f
        }

        val scaleX = CONTENT_W / columns.sumOf { it.widthPt.toDouble() }.toFloat().coerceAtLeast(1f)
        val hasGroups = columns.any { it.groupHeaderTitle != null }

        var colIdx = 0
        var curX = MARGIN_X
        while (colIdx < columns.size) {
            val col = columns[colIdx]
            if (hasGroups && col.groupHeaderTitle != null) {
                val groupTitle = col.groupHeaderTitle
                var endIdx = colIdx
                var groupW = col.widthPt * scaleX
                while (endIdx + 1 < columns.size && columns[endIdx + 1].groupHeaderTitle == groupTitle) {
                    endIdx++
                    groupW += columns[endIdx].widthPt * scaleX
                }
                if (colIdx > 0) canvas.drawLine(curX, rect.top, curX, rect.bottom, sepPaint)
                val midY = rect.top + height * 0.5f
                canvas.drawLine(curX, midY, curX + groupW, midY, sepPaint)

                BrandedTextLayout.drawFittedSingleLine(
                    canvas = canvas,
                    text = groupTitle,
                    x = curX + groupW / 2f,
                    baselineY = rect.top + 10.5f,
                    maxWidthPx = groupW - 6f,
                    paint = fonts.tableHdr78,
                    align = Paint.Align.CENTER,
                    minTextSizePx = 5.8f
                )

                var subX = curX
                for (subI in colIdx..endIdx) {
                    val subCol = columns[subI]
                    val subW = subCol.widthPt * scaleX
                    if (subI > colIdx) canvas.drawLine(subX, midY, subX, rect.bottom, sepPaint)
                    BrandedTextLayout.drawFittedSingleLine(
                        canvas = canvas,
                        text = subCol.titleTop,
                        x = subX + subW / 2f,
                        baselineY = midY + 10.5f,
                        maxWidthPx = subW - 4f,
                        paint = TextPaint(fonts.tableHdr78).apply { textSize = 7.2f },
                        align = Paint.Align.CENTER,
                        minTextSizePx = 5.5f
                    )
                    subX += subW
                }
                curX += groupW
                colIdx = endIdx + 1
            } else {
                val colW = col.widthPt * scaleX
                if (colIdx > 0) canvas.drawLine(curX, rect.top, curX, rect.bottom, sepPaint)
                val (anchorX, paintAlign) = when (col.align) {
                    PdfColAlign.LEFT -> (curX + 6f) to Paint.Align.LEFT
                    PdfColAlign.CENTER -> (curX + colW / 2f) to Paint.Align.CENTER
                    PdfColAlign.RIGHT -> (curX + colW - 6f) to Paint.Align.RIGHT
                }
                if (!col.titleBottom.isNullOrBlank()) {
                    BrandedTextLayout.drawFittedSingleLine(
                        canvas = canvas,
                        text = col.titleTop,
                        x = anchorX,
                        baselineY = rect.top + height * 0.44f,
                        maxWidthPx = colW - 8f,
                        paint = fonts.tableHdr78,
                        align = paintAlign,
                        minTextSizePx = 5.5f
                    )
                    BrandedTextLayout.drawFittedSingleLine(
                        canvas = canvas,
                        text = col.titleBottom,
                        x = anchorX,
                        baselineY = rect.top + height * 0.78f,
                        maxWidthPx = colW - 8f,
                        paint = fonts.tableHdrSub65,
                        align = paintAlign,
                        minTextSizePx = 5f
                    )
                } else {
                    BrandedTextLayout.drawFittedSingleLine(
                        canvas = canvas,
                        text = col.titleTop,
                        x = anchorX,
                        baselineY = rect.centerY() + 3f,
                        maxWidthPx = colW - 8f,
                        paint = fonts.tableHdr78,
                        align = paintAlign,
                        minTextSizePx = 5.5f
                    )
                }
                curX += colW
                colIdx++
            }
        }

        canvas.restore()
        return topY + height
    }

    private fun measureTableRows(spec: BrandedPdfDocumentSpec, fonts: PdfFontSet): List<MeasuredTableRow> {
        val mode = spec.tableMode ?: return emptyList()
        val columns = spec.tableColumns
        if (columns.isEmpty()) return emptyList()
        val scaleX = CONTENT_W / columns.sumOf { it.widthPt.toDouble() }.toFloat().coerceAtLeast(1f)

        val primaryColIdx = when (mode) {
            PdfTableMode.EXCHANGE_ITEMS -> 2
            else -> 1
        }
        val primaryColW = columns[primaryColIdx].widthPt * scaleX
        val thumbReserve = 30f

        return spec.tableRows.map { row ->
            val hasThumb = row.showThumbnailSlot
            val primaryAvailW = (primaryColW - 10f - if (hasThumb) thumbReserve else 0f).coerceAtLeast(48f).toInt()
            val primaryLayout = BrandedTextLayout.layoutMultiLine(row.primaryTitle, fonts.cellBold8, primaryAvailW, lineSpacingMult = 1.08f)
            val skuLayout = row.skuSubtitle?.takeIf { it.isNotBlank() }?.let { sku ->
                BrandedTextLayout.layoutMultiLine(sku, fonts.cellSmall68, primaryAvailW, maxLines = 2)
            }
            var primaryStackH = primaryLayout.height.toFloat()
            if (skuLayout != null) primaryStackH += skuLayout.height + 1.5f
            if (!row.discountBadgeText.isNullOrBlank()) primaryStackH += 10f

            val extraLayouts = mutableMapOf<Int, StaticLayout>()
            var extraMaxH = 0f
            if (mode == PdfTableMode.STATEMENT_LEDGER) {
                // Reference & particulars columns can wrap cleanly
                val refW = (columns[2].widthPt * scaleX - 8f).coerceAtLeast(36f).toInt()
                val descW = (columns[3].widthPt * scaleX - 8f).coerceAtLeast(44f).toInt()
                val refLayout = BrandedTextLayout.layoutMultiLine(row.extraCells.getOrElse(0) { "" }, fonts.cellReg78, refW, maxLines = 2)
                val descLayout = BrandedTextLayout.layoutMultiLine(row.extraCells.getOrElse(1) { "" }, fonts.cellReg78, descW, maxLines = 3)
                extraLayouts[0] = refLayout
                extraLayouts[1] = descLayout
                extraMaxH = maxOf(refLayout.height, descLayout.height).toFloat()
            }

            val contentH = maxOf(primaryStackH, extraMaxH)
            val minH = if (hasThumb) 34f else 21f
            val rowH = maxOf(minH, contentH + 10f)
            MeasuredTableRow(rowH, primaryLayout, skuLayout, extraLayouts)
        }
    }

    private fun drawTableRow(
        canvas: Canvas,
        mode: PdfTableMode,
        columns: List<PdfTableColumnSpec>,
        row: PdfTableRowSpec,
        measured: MeasuredTableRow,
        y: Float,
        isAlt: Boolean,
        isLastVisibleRow: Boolean,
        thumbnailCache: Map<String, Bitmap>,
        fonts: PdfFontSet
    ) {
        val rowH = measured.height
        val rightEdge = MARGIN_X + CONTENT_W
        if (isAlt) {
            canvas.drawRect(
                MARGIN_X, y, rightEdge, y + rowH,
                Paint().apply { color = BrandedDocumentPalette.TableRowAltBg }
            )
        }

        val borderPaint = Paint().apply {
            color = BrandedDocumentPalette.SectionBoxBorder
            strokeWidth = if (isLastVisibleRow) 0.9f else 0.6f
        }
        // Left, right, bottom borders
        canvas.drawLine(MARGIN_X, y, MARGIN_X, y + rowH, borderPaint)
        canvas.drawLine(rightEdge, y, rightEdge, y + rowH, borderPaint)
        canvas.drawLine(MARGIN_X, y + rowH, rightEdge, y + rowH, borderPaint)

        val scaleX = CONTENT_W / columns.sumOf { it.widthPt.toDouble() }.toFloat().coerceAtLeast(1f)
        val colDivPaint = Paint().apply {
            color = BrandedDocumentPalette.DividerSubtle
            strokeWidth = 0.5f
        }

        var curX = MARGIN_X
        var extraCursor = 0

        columns.forEachIndexed { colIdx, col ->
            val colW = col.widthPt * scaleX
            if (colIdx > 0) {
                canvas.drawLine(curX, y, curX, y + rowH, colDivPaint)
            }

            val isPrimaryProductCol = (mode == PdfTableMode.EXCHANGE_ITEMS && colIdx == 2) ||
                (mode != PdfTableMode.EXCHANGE_ITEMS && colIdx == 1)

            when {
                colIdx == 0 -> {
                    BrandedTextLayout.drawFittedSingleLine(
                        canvas = canvas,
                        text = row.indexText,
                        x = curX + colW / 2f,
                        baselineY = y + rowH / 2f + 2.8f,
                        maxWidthPx = colW - 4f,
                        paint = fonts.cellReg78,
                        align = Paint.Align.CENTER,
                        minTextSizePx = 6f
                    )
                }
                isPrimaryProductCol && (mode != PdfTableMode.PAYMENT_ALLOCATIONS && mode != PdfTableMode.STATEMENT_LEDGER) -> {
                    var textX = curX + 6f
                    if (row.showThumbnailSlot) {
                        val thumbSz = 24f
                        val thumbRect = RectF(
                            curX + 5f,
                            y + (rowH - thumbSz) / 2f,
                            curX + 5f + thumbSz,
                            y + (rowH - thumbSz) / 2f + thumbSz
                        )
                        val bmp = row.thumbnailPath?.let { thumbnailCache[it] }
                        BrandedCanvasComponents.drawProductThumbnail(
                            canvas = canvas,
                            rect = thumbRect,
                            bitmap = bmp,
                            radiusPx = 4f,
                            drawPlaceholderIfNull = true
                        )
                        textX = thumbRect.right + 6f
                    }

                    val pLayout = measured.primaryLayout
                    val sLayout = measured.skuLayout
                    var totalTextH = (pLayout?.height ?: 0).toFloat()
                    if (sLayout != null) totalTextH += sLayout.height + 1.5f
                    if (!row.discountBadgeText.isNullOrBlank()) totalTextH += 9.5f

                    var ty = y + ((rowH - totalTextH) / 2f).coerceAtLeast(4f)
                    if (pLayout != null) {
                        ty = BrandedTextLayout.drawLayout(canvas, pLayout, textX, ty)
                    }
                    if (sLayout != null) {
                        ty += 1f
                        ty = BrandedTextLayout.drawLayout(canvas, sLayout, textX, ty)
                    }
                    row.discountBadgeText?.takeIf { it.isNotBlank() }?.let { badge ->
                        val availW = (curX + colW - 4f - textX).coerceAtLeast(24f)
                        BrandedTextLayout.drawFittedSingleLine(
                            canvas = canvas,
                            text = badge,
                            x = textX,
                            baselineY = ty + 7.5f,
                            maxWidthPx = availW,
                            paint = fonts.cellDisc7,
                            minTextSizePx = 5.5f
                        )
                    }
                }
                isPrimaryProductCol -> {
                    // Non-product text column (Invoice Number in Payment, Date in Statement)
                    val textVal = row.primaryTitle
                    BrandedTextLayout.drawFittedSingleLine(
                        canvas = canvas,
                        text = textVal,
                        x = curX + 6f,
                        baselineY = y + rowH / 2f + 2.8f,
                        maxWidthPx = colW - 10f,
                        paint = fonts.cellBold8,
                        align = Paint.Align.LEFT,
                        minTextSizePx = 5.8f
                    )
                }
                else -> {
                    val extraIdx = extraCursor++
                    val cellText = row.extraCells.getOrElse(extraIdx) { "-" }
                    val preMeasured = measured.extraCellLayouts[extraIdx]
                    if (preMeasured != null) {
                        val ty = y + ((rowH - preMeasured.height) / 2f).coerceAtLeast(3f)
                        BrandedTextLayout.drawLayout(canvas, preMeasured, curX + 4f, ty)
                    } else if (mode == PdfTableMode.EXCHANGE_ITEMS && colIdx == 1 && !row.badgeTagText.isNullOrBlank()) {
                        val badgePaint = TextPaint(fonts.cellBold8).apply {
                            textSize = 7.4f
                            color = row.badgeTagColor
                        }
                        BrandedTextLayout.drawFittedSingleLine(
                            canvas = canvas,
                            text = cellText,
                            x = curX + colW / 2f,
                            baselineY = y + rowH / 2f + 2.6f,
                            maxWidthPx = colW - 6f,
                            paint = badgePaint,
                            align = Paint.Align.CENTER,
                            minTextSizePx = 5.5f
                        )
                    } else {
                        val isLastCol = colIdx == columns.lastIndex
                        val paintToUse = if (isLastCol) {
                            TextPaint(fonts.cellBold8).apply { color = row.highlightAmountColor }
                        } else {
                            fonts.cellReg78
                        }
                        val (anchorX, paintAlign) = when (col.align) {
                            PdfColAlign.LEFT -> (curX + 6f) to Paint.Align.LEFT
                            PdfColAlign.CENTER -> (curX + colW / 2f) to Paint.Align.CENTER
                            PdfColAlign.RIGHT -> (curX + colW - 6f) to Paint.Align.RIGHT
                        }
                        if (cellText.contains('\n')) {
                            val parts = cellText.split('\n')
                            BrandedTextLayout.drawFittedSingleLine(
                                canvas, parts.first(), anchorX, y + rowH / 2f - 1.5f, colW - 8f, paintToUse, paintAlign, 5.5f
                            )
                            BrandedTextLayout.drawFittedSingleLine(
                                canvas, parts.getOrElse(1) { "" }, anchorX, y + rowH / 2f + 7.5f, colW - 8f, fonts.cellSmall68, paintAlign, 5f
                            )
                        } else {
                            BrandedTextLayout.drawFittedSingleLine(
                                canvas = canvas,
                                text = cellText,
                                x = anchorX,
                                baselineY = y + rowH / 2f + 2.8f,
                                maxWidthPx = colW - 8f,
                                paint = paintToUse,
                                align = paintAlign,
                                minTextSizePx = 5.5f
                            )
                        }
                    }
                }
            }

            curX += colW
        }
    }

    // endregion

    // region Summary Cards

    private fun measureSummaryCardHeight(card: PdfSummaryCardSpec, width: Float, fonts: PdfFontSet): Float {
        var h = 23f // header
        h += card.rows.size * 15f
        if (card.dividerBeforeLastRow && card.rows.size > 1) h += 5f
        if (card.totalPill != null) h += 34f
        if (!card.footnote.isNullOrBlank()) h += 12f
        return h + 8f
    }

    private fun drawSummaryCard(
        canvas: Canvas,
        context: Context,
        card: PdfSummaryCardSpec,
        rect: RectF,
        fonts: PdfFontSet
    ) {
        BrandedCanvasComponents.drawSectionCard(
            canvas = canvas,
            rect = rect,
            radiusPx = 8f,
            fillColor = BrandedDocumentPalette.SectionBoxBg,
            borderColor = BrandedDocumentPalette.SectionBoxBorder,
            borderWidthPx = 0.9f
        )

        val pad = 10f
        BrandedVectorIcons.drawIcon(
            canvas = canvas,
            type = card.icon,
            centerX = rect.left + pad + 6f,
            centerY = rect.top + 11.5f,
            sizePx = 11.5f,
            color = BrandedDocumentPalette.BrandPurplePrimary
        )
        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = card.title,
            x = rect.left + pad + 16f,
            baselineY = rect.top + 14.5f,
            maxWidthPx = rect.width() - pad * 2 - 18f,
            paint = fonts.cardTitle85,
            minTextSizePx = 6.8f
        )
        val divY = rect.top + 20f
        canvas.drawLine(
            rect.left + pad, divY, rect.right - pad, divY,
            Paint().apply { color = BrandedDocumentPalette.DividerSubtle; strokeWidth = 0.8f }
        )

        var y = divY + 12f
        val innerW = rect.width() - pad * 2

        card.rows.forEachIndexed { idx, row ->
            if (card.dividerBeforeLastRow && idx == card.rows.lastIndex && card.rows.size > 1) {
                val sepY = y - 8f
                canvas.drawLine(
                    rect.left + pad, sepY, rect.right - pad, sepY,
                    Paint().apply { color = BrandedDocumentPalette.SectionBoxBorder; strokeWidth = 0.9f }
                )
                y += 4f
            }
            val labelPaint = if (row.boldValue && idx == card.rows.lastIndex && card.dividerBeforeLastRow) fonts.bodyBold8 else fonts.bodyReg8
            val valuePaint = TextPaint(if (row.boldValue) fonts.bodyBold85 else fonts.bodyBold8).apply {
                textSize = if (row.boldValue) 8.5f else 8f
                color = row.valueColor
            }
            BrandedTextLayout.drawFittedSingleLine(
                canvas = canvas,
                text = row.label,
                x = rect.left + pad,
                baselineY = y,
                maxWidthPx = innerW * 0.56f,
                paint = labelPaint,
                minTextSizePx = 6f
            )
            BrandedTextLayout.drawFittedSingleLine(
                canvas = canvas,
                text = row.value,
                x = rect.right - pad,
                baselineY = y,
                maxWidthPx = innerW * 0.43f,
                paint = valuePaint,
                align = Paint.Align.RIGHT,
                minTextSizePx = 6f
            )
            y += 15f
        }

        card.totalPill?.let { pill ->
            val footnoteReserve = if (!card.footnote.isNullOrBlank()) 13f else 0f
            val pillTop = (rect.bottom - 8f - footnoteReserve - 26f).coerceAtLeast(y - 6f)
            val pillRect = RectF(rect.left + pad, pillTop, rect.right - pad, pillTop + 26f)
            BrandedCanvasComponents.drawPurpleTotalPill(
                canvas = canvas,
                context = context,
                rect = pillRect,
                icon = pill.icon,
                label = pill.label,
                amountText = pill.amountText,
                scale = 0.72f
            )
            card.footnote?.takeIf { it.isNotBlank() }?.let { note ->
                BrandedTextLayout.drawFittedSingleLine(
                    canvas = canvas,
                    text = note,
                    x = rect.left + pad,
                    baselineY = rect.bottom - 5f,
                    maxWidthPx = innerW,
                    paint = fonts.bodyMuted75,
                    minTextSizePx = 5.5f
                )
            }
        }
    }

    // endregion

    // region Bottom Notes, Thank You, QR & Authorised Signature

    private data class MeasuredBottomBlock(
        val totalHeight: Float,
        val notesPanelW: Float,
        val notesCardH: Float,
        val qrSlotW: Float,
        val sigSlotW: Float,
        val bulletLayouts: List<StaticLayout>
    )

    private fun measureBottomBlock(block: PdfBottomFooterBlockSpec, fonts: PdfFontSet): MeasuredBottomBlock {
        val qrSlotW = if (block.qrPayload != null) 88f else 0f
        val hasAnySig = block.showAuthorizedSignature || block.receiverSignatureLabel != null
        val sigSlotW = when {
            block.showAuthorizedSignature && block.receiverSignatureLabel != null -> 150f
            hasAnySig -> 108f
            else -> 0f
        }
        val gaps = (if (qrSlotW > 0f) 10f else 0f) + (if (sigSlotW > 0f) 10f else 0f)
        val notesW = (CONTENT_W - qrSlotW - sigSlotW - gaps).coerceAtLeast(200f)

        val bulletTextW = (notesW - 28f).coerceAtLeast(120f).toInt()
        val bulletLayouts = block.notesBullets.map { text ->
            BrandedTextLayout.layoutMultiLine(text, fonts.bodyReg8, bulletTextW, lineSpacingMult = 1.08f)
        }
        var notesCardH = 24f
        for (bl in bulletLayouts) {
            notesCardH += bl.height + 4f
        }
        notesCardH += 6f

        val thankYouH = if (!block.thankYouSecondary.isNullOrBlank()) 28f else 16f
        val leftTotalH = notesCardH + thankYouH
        val rightTotalH = maxOf(
            if (qrSlotW > 0f) 86f else 0f,
            if (hasAnySig) 76f else 0f
        )
        return MeasuredBottomBlock(
            totalHeight = maxOf(leftTotalH, rightTotalH),
            notesPanelW = notesW,
            notesCardH = notesCardH,
            qrSlotW = qrSlotW,
            sigSlotW = sigSlotW,
            bulletLayouts = bulletLayouts
        )
    }

    private fun drawBottomBlock(
        canvas: Canvas,
        context: Context,
        block: PdfBottomFooterBlockSpec,
        measured: MeasuredBottomBlock,
        topY: Float,
        qrBitmap: Bitmap?,
        fonts: PdfFontSet
    ) {
        // 1. Left Notes Card
        val notesRect = RectF(MARGIN_X, topY, MARGIN_X + measured.notesPanelW, topY + measured.notesCardH)
        BrandedCanvasComponents.drawSectionCard(
            canvas = canvas,
            rect = notesRect,
            radiusPx = 8f,
            fillColor = BrandedDocumentPalette.SectionBoxBg,
            borderColor = BrandedDocumentPalette.SectionBoxBorder,
            borderWidthPx = 0.9f
        )

        val pad = 10f
        BrandedVectorIcons.drawIcon(
            canvas = canvas,
            type = BrandedIconType.INFO_CIRCLE,
            centerX = notesRect.left + pad + 6f,
            centerY = notesRect.top + 11.5f,
            sizePx = 11.5f,
            color = BrandedDocumentPalette.BrandPurplePrimary
        )
        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = block.notesTitle,
            x = notesRect.left + pad + 16f,
            baselineY = notesRect.top + 14.5f,
            maxWidthPx = notesRect.width() - pad * 2 - 18f,
            paint = fonts.cardTitle85,
            minTextSizePx = 7f
        )

        var ny = notesRect.top + 22f
        measured.bulletLayouts.forEach { layout ->
            BrandedVectorIcons.drawIcon(
                canvas = canvas,
                type = BrandedIconType.CHECK_CIRCLE,
                centerX = notesRect.left + pad + 4f,
                centerY = ny + 5f,
                sizePx = 7.5f,
                color = BrandedDocumentPalette.BrandPurpleBright
            )
            ny = BrandedTextLayout.drawLayout(canvas, layout, notesRect.left + pad + 12f, ny)
            ny += 3.5f
        }

        // Thank-you note below notes card
        var ty = notesRect.bottom + 12f
        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = block.thankYouPrimary,
            x = MARGIN_X + 2f,
            baselineY = ty,
            maxWidthPx = measured.notesPanelW,
            paint = fonts.bodyBold8,
            minTextSizePx = 6.5f
        )
        block.thankYouSecondary?.takeIf { it.isNotBlank() }?.let { sec ->
            ty += 11f
            BrandedTextLayout.drawFittedSingleLine(
                canvas = canvas,
                text = sec,
                x = MARGIN_X + 2f,
                baselineY = ty,
                maxWidthPx = measured.notesPanelW,
                paint = fonts.bodyMuted75,
                minTextSizePx = 6f
            )
        }

        var cursorX = notesRect.right + 10f

        // 2. Functional QR Block (when enabled)
        if (measured.qrSlotW > 0f && qrBitmap != null && !qrBitmap.isRecycled) {
            val qrSize = 60f
            val qrLeft = cursorX + (measured.qrSlotW - qrSize) / 2f
            val qrRect = RectF(qrLeft, topY + 2f, qrLeft + qrSize, topY + 2f + qrSize)
            BrandedCanvasComponents.drawSectionCard(
                canvas = canvas,
                rect = RectF(qrRect.left - 3f, qrRect.top - 3f, qrRect.right + 3f, qrRect.bottom + 3f),
                radiusPx = 6f,
                fillColor = Color.WHITE,
                borderColor = BrandedDocumentPalette.SectionBoxBorder,
                borderWidthPx = 0.8f
            )
            canvas.drawBitmap(qrBitmap, Rect(0, 0, qrBitmap.width, qrBitmap.height), qrRect, Paint(Paint.FILTER_BITMAP_FLAG))

            val qrCenter = cursorX + measured.qrSlotW / 2f
            block.qrCaptionPrimary?.let { cap1 ->
                BrandedTextLayout.drawFittedSingleLine(
                    canvas = canvas,
                    text = cap1,
                    x = qrCenter,
                    baselineY = qrRect.bottom + 12f,
                    maxWidthPx = measured.qrSlotW,
                    paint = fonts.bodyBold8,
                    align = Paint.Align.CENTER,
                    minTextSizePx = 6f
                )
            }
            block.qrCaptionSecondary?.let { cap2 ->
                BrandedTextLayout.drawFittedSingleLine(
                    canvas = canvas,
                    text = cap2,
                    x = qrCenter,
                    baselineY = qrRect.bottom + 21f,
                    maxWidthPx = measured.qrSlotW,
                    paint = fonts.bodyMuted75,
                    align = Paint.Align.CENTER,
                    minTextSizePx = 5.5f
                )
            }
            cursorX += measured.qrSlotW + 10f
        }

        // 3. Signature Block (Authorised + optional Receiver Signature on Delivery Challan)
        if (measured.sigSlotW > 0f) {
            val sigPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val lineY = topY + 50f
            if (block.receiverSignatureLabel != null && block.showAuthorizedSignature) {
                val halfW = (measured.sigSlotW - 8f) / 2f
                drawSingleSignatureSlot(
                    canvas = canvas,
                    context = context,
                    leftX = cursorX,
                    rightX = cursorX + halfW,
                    lineY = lineY,
                    primaryLabel = block.receiverSignatureLabel,
                    secondaryLabel = "",
                    drawStampBitmap = false,
                    sigPaint = sigPaint,
                    fonts = fonts
                )
                drawSingleSignatureSlot(
                    canvas = canvas,
                    context = context,
                    leftX = cursorX + halfW + 8f,
                    rightX = cursorX + measured.sigSlotW,
                    lineY = lineY,
                    primaryLabel = block.signaturePrimaryLabel,
                    secondaryLabel = block.signatureSecondaryLabel,
                    drawStampBitmap = true,
                    sigPaint = sigPaint,
                    fonts = fonts
                )
            } else if (block.showAuthorizedSignature) {
                drawSingleSignatureSlot(
                    canvas = canvas,
                    context = context,
                    leftX = cursorX,
                    rightX = cursorX + measured.sigSlotW,
                    lineY = lineY,
                    primaryLabel = block.signaturePrimaryLabel,
                    secondaryLabel = block.signatureSecondaryLabel,
                    drawStampBitmap = true,
                    sigPaint = sigPaint,
                    fonts = fonts
                )
            } else if (block.receiverSignatureLabel != null) {
                drawSingleSignatureSlot(
                    canvas = canvas,
                    context = context,
                    leftX = cursorX,
                    rightX = cursorX + measured.sigSlotW,
                    lineY = lineY,
                    primaryLabel = block.receiverSignatureLabel,
                    secondaryLabel = "",
                    drawStampBitmap = false,
                    sigPaint = sigPaint,
                    fonts = fonts
                )
            }
        }
    }

    private fun drawSingleSignatureSlot(
        canvas: Canvas,
        context: Context,
        leftX: Float,
        rightX: Float,
        lineY: Float,
        primaryLabel: String,
        secondaryLabel: String,
        drawStampBitmap: Boolean,
        sigPaint: Paint,
        fonts: PdfFontSet
    ) {
        if (drawStampBitmap) {
            CompanyBrandingHelper.drawAuthorizedSignature(
                context = context,
                canvas = canvas,
                lineStartX = leftX + 4f,
                lineEndX = rightX - 4f,
                lineY = lineY,
                paint = sigPaint
            )
        }
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BrandedDocumentPalette.TextMuted
            strokeWidth = 0.8f
        }
        canvas.drawLine(leftX + 4f, lineY, rightX - 4f, lineY, rulePaint)

        val cx = (leftX + rightX) / 2f
        val maxW = (rightX - leftX - 4f).coerceAtLeast(36f)
        BrandedTextLayout.drawFittedSingleLine(
            canvas = canvas,
            text = primaryLabel,
            x = cx,
            baselineY = lineY + 11f,
            maxWidthPx = maxW,
            paint = fonts.bodyMuted75,
            align = Paint.Align.CENTER,
            minTextSizePx = 5.5f
        )
        if (secondaryLabel.isNotBlank()) {
            BrandedTextLayout.drawFittedSingleLine(
                canvas = canvas,
                text = secondaryLabel,
                x = cx,
                baselineY = lineY + 21f,
                maxWidthPx = maxW,
                paint = fonts.bodyBold8,
                align = Paint.Align.CENTER,
                minTextSizePx = 6f
            )
        }
    }

    // endregion
}
