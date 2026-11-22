package com.pocketdev.advance_erp.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.math.RoundingMode

data class ProductLabelData(
    val productName: String,
    val sku: String,
    val barcode: String,
    val qrPayload: String,
    val variantDescription: String? = null,
    val priceMinor: Long,
    val companyName: String = "ADVANCE ERP"
)

enum class LabelSheetFormat(val displayName: String, val cols: Int, val rows: Int) {
    THERMAL_50X30("Thermal Roll 50x30mm", 1, 1),
    A4_24_LABELS("A4 Sticker Sheet (24 Labels - 3x8)", 3, 8),
    A4_30_LABELS("A4 Sticker Sheet (30 Labels - 3x10)", 3, 10)
}

object LabelPrintingEngine {

    // 50mm x 30mm at 72dpi: 142 x 85 points
    private const val THERMAL_WIDTH = 142
    private const val THERMAL_HEIGHT = 85

    // Standard A4 at 72dpi: 595 x 842 points
    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842

    /**
     * Generates a multi-page PDF for thermal roll printers (50x30mm) with [count] labels.
     */
    fun generateThermalRollPdf(
        context: Context,
        labelData: ProductLabelData,
        count: Int = 1
    ): File {
        val document = PdfDocument()

        val barcodeBitmap = try {
            BarcodeGenerator.generateBarcodeBitmap(
                content = labelData.barcode,
                width = 560,
                height = 220,
                includeText = true
            )
        } catch (e: Exception) {
            null
        }

        val qrBitmap = try {
            QrCodeGenerator.generateQrBitmap(
                content = labelData.qrPayload,
                targetSize = 220
            )
        } catch (e: Exception) {
            null
        }

        for (pageIndex in 1..count) {
            val pageInfo = PdfDocument.PageInfo.Builder(THERMAL_WIDTH, THERMAL_HEIGHT, pageIndex).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawSingleLabel(
                canvas = canvas,
                x = 4f,
                y = 4f,
                w = (THERMAL_WIDTH - 8).toFloat(),
                h = (THERMAL_HEIGHT - 8).toFloat(),
                data = labelData,
                barcodeBitmap = barcodeBitmap,
                qrBitmap = qrBitmap,
                isThermal = true
            )

            document.finishPage(page)
        }

        val dir = File(context.cacheDir, "labels").apply { if (!exists()) mkdirs() }
        val file = File(dir, "Label_${labelData.sku}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
            out.flush()
        }
        document.close()

        return file
    }

    /**
     * Generates an A4 sticker sheet PDF (grid of 24 or 30 labels) repeated [count] times.
     */
    fun generateA4StickerSheetPdf(
        context: Context,
        labelData: ProductLabelData,
        format: LabelSheetFormat = LabelSheetFormat.A4_24_LABELS,
        totalLabelsToPrint: Int = 24
    ): File {
        val document = PdfDocument()
        val labelsPerPage = format.cols * format.rows
        val totalPages = (totalLabelsToPrint + labelsPerPage - 1) / labelsPerPage

        val barcodeBitmap = try {
            BarcodeGenerator.generateBarcodeBitmap(
                content = labelData.barcode,
                width = 560,
                height = 220,
                includeText = true
            )
        } catch (e: Exception) {
            null
        }

        val qrBitmap = try {
            QrCodeGenerator.generateQrBitmap(
                content = labelData.qrPayload,
                targetSize = 220
            )
        } catch (e: Exception) {
            null
        }

        val marginX = 20f
        val marginY = 24f
        val availableW = A4_WIDTH - (marginX * 2)
        val availableH = A4_HEIGHT - (marginY * 2)

        val cellW = availableW / format.cols
        val cellH = availableH / format.rows

        var labelsDrawn = 0

        for (pageIndex in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageIndex).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            for (r in 0 until format.rows) {
                for (c in 0 until format.cols) {
                    if (labelsDrawn >= totalLabelsToPrint) break

                    val left = marginX + (c * cellW) + 2f
                    val top = marginY + (r * cellH) + 2f
                    val w = cellW - 4f
                    val h = cellH - 4f

                    val borderPaint = Paint().apply {
                        color = Color.parseColor("#E2E8F0")
                        style = Paint.Style.STROKE
                        strokeWidth = 0.5f
                    }
                    canvas.drawRoundRect(RectF(left, top, left + w, top + h), 4f, 4f, borderPaint)

                    drawSingleLabel(
                        canvas = canvas,
                        x = left + 4f,
                        y = top + 4f,
                        w = w - 8f,
                        h = h - 8f,
                        data = labelData,
                        barcodeBitmap = barcodeBitmap,
                        qrBitmap = qrBitmap,
                        isThermal = false
                    )

                    labelsDrawn++
                }
            }

            document.finishPage(page)
        }

        val dir = File(context.cacheDir, "labels").apply { if (!exists()) mkdirs() }
        val file = File(dir, "A4_Labels_${labelData.sku}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
            out.flush()
        }
        document.close()

        return file
    }

    /**
     * Generates professional PO Carton Labels (CTN Content Label + distinct Shipping Mark block).
     * Uses the actual validated carton allocations (single and mixed cartons).
     */
    fun generatePoCartonLabelsPdf(
        context: Context,
        exportData: ValidatedSupplierPoExport
    ): File {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val marginX = 32f
        val marginY = 32f
        val cardW = A4_WIDTH - marginX * 2f

        val cartons = exportData.exportedCartons
        if (cartons.isEmpty()) {
            val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            paint.color = Color.parseColor("#1B2A4A")
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("NO CARTONS AVAILABLE FOR ${exportData.poNumber}", marginX, marginY + 30f, paint)
            document.finishPage(page)
        } else {
            var pageNo = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNo).create())
            var canvas = page.canvas
            var cursorY = marginY

            cartons.forEach { ctn ->
                val markLines = ctn.shippingMark.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("-") }
                val lineRowsHeight = maxOf(1, ctn.lines.size) * 20f
                val markHeight = 28f + markLines.size * 14f
                val cardH = 54f + 22f + lineRowsHeight + 28f + markHeight + 16f

                if (cursorY + cardH > A4_HEIGHT - marginY) {
                    document.finishPage(page)
                    pageNo++
                    page = document.startPage(PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNo).create())
                    canvas = page.canvas
                    cursorY = marginY
                }

                // Outer Card Frame
                paint.style = Paint.Style.FILL
                paint.color = Color.WHITE
                canvas.drawRoundRect(RectF(marginX, cursorY, marginX + cardW, cursorY + cardH), 10f, 10f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.2f
                paint.color = Color.parseColor("#CBD5E1")
                canvas.drawRoundRect(RectF(marginX, cursorY, marginX + cardW, cursorY + cardH), 10f, 10f, paint)
                paint.style = Paint.Style.FILL

                // Navy Header Strip
                paint.color = Color.parseColor("#1B2A4A")
                canvas.drawRoundRect(RectF(marginX, cursorY, marginX + cardW, cursorY + 46f), 10f, 10f, paint)
                canvas.drawRect(marginX, cursorY + 32f, marginX + cardW, cursorY + 46f, paint)

                // Header Texts
                paint.color = Color.WHITE
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 18f
                canvas.drawText(ctn.cartonNumber, marginX + 14f, cursorY + 29f, paint)

                paint.textSize = 9.5f
                paint.color = Color.parseColor("#67E8F9")
                val typeBadge = if (ctn.isMixed) "MIXED CARTON (${ctn.lines.size} VARIANTS)" else "SINGLE VARIANT"
                canvas.drawText("CTN ${ctn.sequence} OF ${exportData.allCartons.size} • $typeBadge", marginX + 112f, cursorY + 28f, paint)

                paint.textAlign = Paint.Align.RIGHT
                paint.color = Color.WHITE
                paint.textSize = 10f
                canvas.drawText("PO #: ${exportData.poNumber}", marginX + cardW - 14f, cursorY + 20f, paint)
                paint.textSize = 8.2f
                paint.typeface = Typeface.DEFAULT
                paint.color = Color.parseColor("#CBD5E1")
                canvas.drawText(exportData.orderDateFormatted, marginX + cardW - 14f, cursorY + 35f, paint)
                paint.textAlign = Paint.Align.LEFT

                // Contents Table Header
                var y = cursorY + 56f
                paint.color = Color.parseColor("#F1F5F9")
                canvas.drawRect(marginX + 10f, y, marginX + cardW - 10f, y + 19f, paint)
                paint.color = Color.parseColor("#1E293B")
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("PRODUCT NAME / SKU", marginX + 16f, y + 13f, paint)
                canvas.drawText("COLOR", marginX + 278f, y + 13f, paint)
                canvas.drawText("SIZE", marginX + 356f, y + 13f, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("QTY (PCS)", marginX + cardW - 16f, y + 13f, paint)
                paint.textAlign = Paint.Align.LEFT
                y += 19f

                ctn.lines.forEachIndexed { idx, line ->
                    if (idx % 2 == 1) {
                        paint.color = Color.parseColor("#F8FAFC")
                        canvas.drawRect(marginX + 10f, y, marginX + cardW - 10f, y + 20f, paint)
                    }
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.color = Color.parseColor("#0F172A")
                    paint.textSize = 8.4f
                    val prodText = "${line.productName} (${line.sku})".take(48)
                    canvas.drawText(prodText, marginX + 16f, y + 13f, paint)

                    paint.typeface = Typeface.DEFAULT
                    paint.color = Color.parseColor("#334155")
                    canvas.drawText(line.color.take(14), marginX + 278f, y + 13f, paint)
                    canvas.drawText(line.size.take(12), marginX + 356f, y + 13f, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.color = Color.parseColor("#792B87")
                    paint.textAlign = Paint.Align.RIGHT
                    canvas.drawText("${line.quantity}", marginX + cardW - 16f, y + 13f, paint)
                    paint.textAlign = Paint.Align.LEFT
                    y += 20f
                }

                // Carton Totals Bar
                y += 4f
                paint.color = Color.parseColor("#FEF3C7")
                canvas.drawRoundRect(RectF(marginX + 10f, y, marginX + cardW - 10f, y + 22f), 6f, 6f, paint)
                paint.color = Color.parseColor("#92400E")
                paint.textSize = 8.2f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val gwStr = ctn.grossWeightKg.setScale(1, RoundingMode.HALF_UP).toPlainString()
                val nwStr = ctn.netWeightKg.setScale(1, RoundingMode.HALF_UP).toPlainString()
                val cbmStr = ctn.cbm.setScale(3, RoundingMode.HALF_UP).toPlainString()
                canvas.drawText(
                    "TOTAL QTY: ${ctn.totalQty} PCS   |   G.W.: $gwStr KG   |   N.W.: $nwStr KG   |   DIM: ${ctn.dimensionsDisplay} cm   |   CBM: $cbmStr m³",
                    marginX + 18f,
                    y + 14.5f,
                    paint
                )
                y += 28f

                // Dashed Separator before Shipping Mark Block
                paint.style = Paint.Style.STROKE
                paint.color = Color.parseColor("#94A3B8")
                paint.strokeWidth = 1f
                paint.pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
                canvas.drawLine(marginX + 12f, y, marginX + cardW - 12f, y, paint)
                paint.pathEffect = null
                paint.style = Paint.Style.FILL
                y += 10f

                // Distinct Shipping Mark Box
                val markBoxColor = if (ctn.isCustomShippingMark) Color.parseColor("#ECFDF5") else Color.parseColor("#F8FAFC")
                paint.color = markBoxColor
                canvas.drawRoundRect(RectF(marginX + 10f, y, marginX + cardW - 10f, y + 18f + markLines.size * 14f), 6f, 6f, paint)
                paint.color = if (ctn.isCustomShippingMark) Color.parseColor("#047857") else Color.parseColor("#475569")
                paint.textSize = 7.8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val markHeader = if (ctn.isCustomShippingMark) "SHIPPING MARK (CUSTOM CARTON OVERRIDE):" else "SHIPPING MARK (PO DEFAULT):"
                canvas.drawText(markHeader, marginX + 18f, y + 12f, paint)
                var my = y + 24f
                paint.color = Color.parseColor("#0F172A")
                paint.textSize = 9.5f
                markLines.forEach { mLine ->
                    canvas.drawText(mLine, marginX + 18f, my, paint)
                    my += 14f
                }

                cursorY += cardH + 16f
            }

            document.finishPage(page)
        }

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "CTN_Labels_${ImportPdfGenerator.sanitizeFileName(exportData.poNumber)}.pdf")
        try {
            FileOutputStream(file).use { out ->
                document.writeTo(out)
                out.flush()
            }
        } finally {
            document.close()
        }
        return file
    }

    private fun drawSingleLabel(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        data: ProductLabelData,
        barcodeBitmap: Bitmap?,
        qrBitmap: Bitmap?,
        isThermal: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Company Banner
        paint.color = Color.parseColor("#263849")
        paint.textSize = if (isThermal) 6.5f else 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(data.companyName.take(22), x, y + 8f, paint)

        // Price Badge on right
        paint.color = Color.parseColor("#792B87")
        paint.textSize = if (isThermal) 7.5f else 8.5f
        paint.textAlign = Paint.Align.RIGHT
        val priceText = "MRP: ৳" + Formatters.formatMinorUnit(data.priceMinor)
        canvas.drawText(priceText, x + w, y + 8f, paint)

        // Product Name
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.BLACK
        paint.textSize = if (isThermal) 7.5f else 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        var nameToDisplay = data.productName
        while (paint.measureText(nameToDisplay) > w && nameToDisplay.length > 6) {
            nameToDisplay = nameToDisplay.dropLast(2) + ".."
        }
        canvas.drawText(nameToDisplay, x, y + 18f, paint)

        // Variant info or SKU (auto-fit font size so SKU is not cut off)
        paint.typeface = Typeface.DEFAULT
        var subTextSize = if (isThermal) 6f else 6.5f
        paint.textSize = subTextSize
        paint.color = Color.DKGRAY
        val subLine = data.variantDescription?.let { "$it | SKU: ${data.sku}" } ?: "SKU: ${data.sku}"
        val subWidth = paint.measureText(subLine)
        if (subWidth > w && subWidth > 0f) {
            subTextSize = (subTextSize * (w / subWidth)).coerceAtLeast(4.5f)
            paint.textSize = subTextSize
        }
        canvas.drawText(subLine, x, y + 26.5f, paint)

        // Visuals Area: Barcode on Left, QR Code on Right
        val visualsTop = y + 29.5f
        val availableVisualH = (h - 29.5f).coerceAtLeast(32f)
        val qrSize = availableVisualH.coerceAtMost(if (isThermal) 42f else 46f)
        paint.isFilterBitmap = true

        if (qrBitmap != null) {
            val qrTop = visualsTop + (availableVisualH - qrSize) / 2f
            val qrDest = RectF(x + w - qrSize, qrTop, x + w, qrTop + qrSize)
            canvas.drawBitmap(qrBitmap, null, qrDest, paint)
        }

        val barcodeW = if (qrBitmap != null) w - qrSize - 5f else w
        val barcodeH = availableVisualH.coerceAtMost(if (isThermal) 44f else 48f)
        if (barcodeBitmap != null) {
            val barcodeTop = visualsTop + (availableVisualH - barcodeH) / 2f
            val barcodeDest = RectF(x, barcodeTop, x + barcodeW, barcodeTop + barcodeH)
            canvas.drawBitmap(barcodeBitmap, null, barcodeDest, paint)
        } else {
            paint.textSize = 7f
            paint.color = Color.BLACK
            canvas.drawText("*${data.barcode}*", x + 4f, visualsTop + 18f, paint)
        }
    }
}
