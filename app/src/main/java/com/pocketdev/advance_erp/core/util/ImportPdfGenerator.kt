package com.pocketdev.advance_erp.core.util

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.LandedCostSnapshot
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.SupplierPayment
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.data.entity.WarehouseReceiptItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImportPdfGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 points
    private const val PAGE_HEIGHT = 842

    internal fun sanitizeFileName(raw: String): String {
        val cleaned = raw.trim().replace(Regex("[^A-Za-z0-9._-]"), "_")
        return cleaned.trim('_').ifBlank { "document" }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    internal fun wrapTextWithMeasurer(
        text: String,
        maxWidth: Float,
        maxLines: Int = Int.MAX_VALUE,
        measure: (String) -> Float
    ): List<String> {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return emptyList()
        val paragraphs = cleaned.split('\n')
        val lines = mutableListOf<String>()

        for (para in paragraphs) {
            val words = para.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (words.isEmpty()) continue
            var current = ""
            for (word in words) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (measure(candidate) <= maxWidth) {
                    current = candidate
                } else {
                    if (current.isNotEmpty()) {
                        lines += current
                        current = ""
                        if (lines.size >= maxLines) break
                    }
                    // Break oversized single token to stay strictly inside maxWidth
                    if (measure(word) <= maxWidth) {
                        current = word
                    } else {
                        var chunk = ""
                        for (ch in word) {
                            val nextChunk = chunk + ch
                            if (measure(nextChunk) <= maxWidth || chunk.isEmpty()) {
                                chunk = nextChunk
                            } else {
                                lines += chunk
                                if (lines.size >= maxLines) break
                                chunk = ch.toString()
                            }
                        }
                        if (lines.size >= maxLines) break
                        current = chunk
                    }
                }
            }
            if (lines.size >= maxLines) break
            if (current.isNotEmpty()) {
                lines += current
                if (lines.size >= maxLines) break
            }
        }

        if (lines.size > maxLines) {
            return lines.take(maxLines)
        }
        return lines
    }

    internal fun fitSingleLineWithMeasurer(
        text: String,
        maxWidth: Float,
        measure: (String) -> Float
    ): String {
        if (measure(text) <= maxWidth) return text
        val ellipsis = "..."
        var end = text.length
        while (end > 0 && measure(text.substring(0, end) + ellipsis) > maxWidth) {
            end--
        }
        return if (end <= 0) "" else text.substring(0, end) + ellipsis
    }

    private fun wrapText(
        text: String,
        paint: Paint,
        maxWidth: Float,
        maxLines: Int = Int.MAX_VALUE
    ): List<String> {
        val wrapped = wrapTextWithMeasurer(text, maxWidth, Int.MAX_VALUE) { paint.measureText(it) }
        if (wrapped.size <= maxLines) return wrapped
        val kept = wrapped.take(maxLines).toMutableList()
        if (kept.isNotEmpty()) {
            kept[kept.lastIndex] = fitSingleLineText("${kept.last()}...", paint, maxWidth)
        }
        return kept
    }

    private fun fitSingleLineText(text: String, paint: Paint, maxWidth: Float): String =
        fitSingleLineWithMeasurer(text, maxWidth) { paint.measureText(it) }

    private fun drawRightAlignedText(
        canvas: Canvas,
        text: String,
        rightX: Float,
        y: Float,
        paint: Paint,
        maxWidth: Float = Float.MAX_VALUE
    ) {
        val fitted = if (maxWidth.isFinite()) fitSingleLineText(text, paint, maxWidth) else text
        val w = paint.measureText(fitted)
        canvas.drawText(fitted, rightX - w, y, paint)
    }

    private fun drawBrandHeader(
        context: Context,
        canvas: Canvas,
        paint: Paint,
        businessProfile: BusinessProfile?,
        documentTitle: String,
        documentNumber: String,
        dateFormatted: String
    ) {
        // Deep Navy Header Banner
        paint.color = Color.parseColor("#263849")
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 110f, paint)

        // Accent Stripes: Royal Purple & Secondary Teal
        paint.color = Color.parseColor("#792B87")
        canvas.drawRect(0f, 110f, PAGE_WIDTH.toFloat() * 0.5f, 115f, paint)
        paint.color = Color.parseColor("#299DB2")
        canvas.drawRect(PAGE_WIDTH.toFloat() * 0.5f, 110f, PAGE_WIDTH.toFloat(), 115f, paint)

        // Official White Logo (downsampled & recycled to avoid OOM crashes on repeated shares)
        val logoBitmap = try {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeResource(context.resources, R.drawable.logo_compact_white, opts)
        } catch (_: Throwable) {
            null
        }
        if (logoBitmap != null) {
            try {
                if (logoBitmap.height > 0 && logoBitmap.width > 0) {
                    val logoH = 30f
                    val logoW = logoH * (logoBitmap.width.toFloat() / logoBitmap.height.toFloat())
                    val logoDest = RectF(36f, 14f, (36f + logoW).coerceAtMost(330f), 14f + logoH)
                    canvas.drawBitmap(logoBitmap, null, logoDest, paint)
                }
            } finally {
                logoBitmap.recycle()
            }
        }

        // Left column strictly bounded inside x = 36f..336f (maxWidth = 300f)
        val leftMaxWidth = 300f
        paint.color = Color.WHITE
        paint.textSize = 12.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val companyName = businessProfile?.companyName ?: "ADVANCE IMPORT & EXPORT ERP"
        canvas.drawText(fitSingleLineText(companyName, paint, leftMaxWidth), 36f, 64f, paint)

        paint.textSize = 8.2f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#E2E8F0")
        val address = businessProfile?.address ?: "Motijheel C/A, Dhaka-1000, Bangladesh"
        val contact = "Phone: ${businessProfile?.phone ?: "+880 1711-000000"}  |  BIN/Tax: ${businessProfile?.taxNumber ?: "BIN-99281726-01"}"
        val infoLines = wrapText("$address  |  $contact", paint, leftMaxWidth, maxLines = 2)
        infoLines.forEachIndexed { idx, line ->
            canvas.drawText(line, 36f, 79f + idx * 11f, paint)
        }

        // Right column strictly bounded inside x = 354f..559f (maxWidth = 205f, leaving >=18pt gutter)
        val rightEdge = PAGE_WIDTH - 36f
        val rightMaxWidth = 205f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#299DB2") // Teal

        var titleSize = 14f
        paint.textSize = titleSize
        while (titleSize > 11f && wrapText(documentTitle, paint, rightMaxWidth, maxLines = 3).size > 2) {
            titleSize -= 0.5f
            paint.textSize = titleSize
        }
        val titleLines = wrapText(documentTitle, paint, rightMaxWidth, maxLines = 2)
        val titleStartY = if (titleLines.size <= 1) 36f else 30f
        titleLines.forEachIndexed { idx, line ->
            drawRightAlignedText(canvas, line, rightEdge, titleStartY + idx * 16f, paint, rightMaxWidth)
        }

        paint.textSize = 9.5f
        paint.color = Color.LTGRAY
        paint.typeface = Typeface.DEFAULT
        drawRightAlignedText(canvas, "Ref #: $documentNumber", rightEdge, 72f, paint, rightMaxWidth)
        drawRightAlignedText(canvas, "Date: $dateFormatted", rightEdge, 87f, paint, rightMaxWidth)
    }

    private fun drawSignatures(canvas: Canvas, paint: Paint, leftLabel: String, rightLabel: String) {
        val footerY = PAGE_HEIGHT - 55f
        paint.color = Color.parseColor("#CBD5E0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawLine(48f, footerY, 210f, footerY, paint)
        canvas.drawLine(PAGE_WIDTH - 210f, footerY, PAGE_WIDTH - 48f, footerY, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = 8.5f
        paint.color = Color.parseColor("#718096")
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(fitSingleLineText(leftLabel, paint, 165f), 48f, footerY + 14f, paint)
        drawRightAlignedText(canvas, rightLabel, PAGE_WIDTH - 48f, footerY + 14f, paint, 165f)
    }

    // 1. PURCHASE ORDER PDF (Company Internal Audit Copy)
    fun generateCompanyInternalOrderPdf(
        context: Context,
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        isBangla: Boolean = false
    ): File {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val marginX = 36f
        val contentRight = PAGE_WIDTH - 36f
        val maxContentBottom = PAGE_HEIGHT - 90f

        val docTitle = if (isBangla) "অভ্যন্তরীণ ক্রয়াদেশ অডিট / INTERNAL PO AUDIT" else "COMPANY INTERNAL PURCHASE ORDER AUDIT"

        fun drawInternalTableHeader(canvas: Canvas, tableY: Float): Float {
            paint.color = Color.parseColor("#EDF2F7")
            canvas.drawRect(marginX, tableY, contentRight, tableY + 24f, paint)
            paint.color = Color.parseColor("#2D3748")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("#", 40f, tableY + 15f, paint)
            canvas.drawText("PRODUCT / DESCRIPTION", 60f, tableY + 15f, paint)
            canvas.drawText("SKU", 240f, tableY + 15f, paint)
            drawRightAlignedText(canvas, "QTY", 342f, tableY + 15f, paint, 36f)
            drawRightAlignedText(canvas, "UNIT (${order.currency})", 406f, tableY + 15f, paint, 58f)
            drawRightAlignedText(canvas, "TOTAL (${order.currency})", 478f, tableY + 15f, paint, 66f)
            drawRightAlignedText(canvas, "TOTAL (৳)", 553f, tableY + 15f, paint, 69f)
            return tableY + 24f
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas

        drawBrandHeader(
            context, canvas, paint, businessProfile,
            docTitle, order.poNumber, dateFormat.format(Date(order.orderDate))
        )

        // Supplier & Order Meta Card with non-overlapping left/right columns and dynamic height
        val boxY = 128f
        val supLeftX = 48f
        val supLeftMaxW = 285f
        val supRightX = 348f
        val supRightMaxW = contentRight - 12f - supRightX

        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val supNameLines = wrapText(supplier?.companyName ?: "Supplier / Factory", paint, supLeftMaxW, maxLines = 2)

        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        val supContactLines = wrapText(
            "Attn: ${supplier?.contactPerson ?: "Export Dept"}  |  ${supplier?.phone ?: "N/A"}",
            paint, supLeftMaxW, maxLines = 2
        )
        val supAddressLines = wrapText(
            "${supplier?.address ?: "Dhaka / Overseas"} • ${supplier?.country ?: ""}".trim(' ', '•'),
            paint, supLeftMaxW, maxLines = 2
        )
        val rightFxLines = wrapText(
            "Exchange Rate: 1 ${order.currency} = ${order.exchangeRateToBdt} BDT",
            paint, supRightMaxW, maxLines = 2
        )

        val leftCardHeight = 28f + supNameLines.size * 14f + supContactLines.size * 12f + supAddressLines.size * 12f + 10f
        val rightCardHeight = 46f + rightFxLines.size * 12f + 10f
        val metaBoxHeight = maxOf(leftCardHeight, rightCardHeight, 84f)

        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(RectF(marginX, boxY, contentRight, boxY + metaBoxHeight), 8f, 8f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRoundRect(RectF(marginX, boxY, contentRight, boxY + metaBoxHeight), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        var ly = boxY + 17f
        paint.color = Color.parseColor("#263849")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SUPPLIER / VENDOR:", supLeftX, ly, paint)
        ly += 16f

        paint.color = Color.parseColor("#1A202C")
        paint.textSize = 11.5f
        supNameLines.forEach { line ->
            canvas.drawText(line, supLeftX, ly, paint)
            ly += 14f
        }

        paint.color = Color.parseColor("#718096")
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        supContactLines.forEach { line ->
            canvas.drawText(line, supLeftX, ly, paint)
            ly += 12f
        }
        supAddressLines.forEach { line ->
            canvas.drawText(line, supLeftX, ly, paint)
            ly += 12f
        }

        var ry = boxY + 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#792B87")
        paint.textSize = 9.5f
        canvas.drawText(fitSingleLineText("STATUS: ${order.status}", paint, supRightMaxW), supRightX, ry, paint)
        ry += 16f
        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(fitSingleLineText("Currency: ${order.currency}", paint, supRightMaxW), supRightX, ry, paint)
        ry += 14f
        rightFxLines.forEach { line ->
            canvas.drawText(line, supRightX, ry, paint)
            ry += 12f
        }

        var currentY = drawInternalTableHeader(canvas, boxY + metaBoxHeight + 14f)

        items.forEachIndexed { index, item ->
            val product = productsMap[item.productId]
            val pName = if (isBangla && product?.nameBn?.isNotBlank() == true) product.nameBn else (product?.nameEn ?: item.productId)
            paint.textSize = 8.5f
            paint.typeface = Typeface.DEFAULT
            val descLines = wrapText(pName, paint, 174f, maxLines = 2).ifEmpty { listOf(item.productId) }
            val rowHeight = if (descLines.size > 1) 30f else 22f

            if (currentY + rowHeight > maxContentBottom) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                drawBrandHeader(
                    context, canvas, paint, businessProfile,
                    docTitle, "${order.poNumber} (P.$pageNumber)", dateFormat.format(Date(order.orderDate))
                )
                currentY = drawInternalTableHeader(canvas, 128f)
            }

            if (index % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(marginX, currentY, contentRight, currentY + rowHeight, paint)
            }

            paint.textSize = 8.5f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#4A5568")
            canvas.drawText("${index + 1}", 40f, currentY + 14f, paint)

            paint.color = Color.parseColor("#1A202C")
            descLines.forEachIndexed { lineIdx, line ->
                canvas.drawText(line, 60f, currentY + 13f + lineIdx * 11f, paint)
            }

            paint.color = Color.parseColor("#718096")
            canvas.drawText(fitSingleLineText(product?.baseSku ?: "SKU", paint, 62f), 240f, currentY + 14f, paint)

            paint.color = Color.parseColor("#1A202C")
            drawRightAlignedText(canvas, "${item.quantity}", 342f, currentY + 14f, paint, 36f)
            drawRightAlignedText(canvas, Formatters.formatMinorUnit(item.unitPriceMinor), 406f, currentY + 14f, paint, 58f)
            drawRightAlignedText(canvas, Formatters.formatMinorUnit(item.totalAmountMinor), 478f, currentY + 14f, paint, 66f)

            paint.color = Color.parseColor("#792B87")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawRightAlignedText(canvas, "৳" + Formatters.formatMinorUnit(item.totalBdtAmountMinor), 553f, currentY + 14f, paint, 69f)

            currentY += rowHeight
        }

        // Totals Card & Left Remarks Section
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        val notesLines = if (!order.notes.isNullOrBlank()) {
            wrapText(order.notes, paint, 250f, maxLines = 8)
        } else {
            emptyList()
        }
        val notesSectionHeight = if (notesLines.isNotEmpty()) 26f + notesLines.size * 12f else 0f
        val totalsBoxHeight = 92f
        val bottomSectionHeight = maxOf(notesSectionHeight, totalsBoxHeight)

        var totalBoxY = currentY + 14f
        if (totalBoxY + bottomSectionHeight > PAGE_HEIGHT - 80f) {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            drawBrandHeader(
                context, canvas, paint, businessProfile,
                docTitle, "${order.poNumber} (P.$pageNumber)", dateFormat.format(Date(order.orderDate))
            )
            totalBoxY = 132f
        }

        if (notesLines.isNotEmpty()) {
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(RectF(marginX, totalBoxY, 290f, totalBoxY + bottomSectionHeight), 8f, 8f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#4A5568")
            paint.textSize = 9f
            canvas.drawText("Consignment / Order Remarks:", 44f, totalBoxY + 18f, paint)
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#2D3748")
            notesLines.forEachIndexed { idx, line ->
                canvas.drawText(line, 44f, totalBoxY + 34f + idx * 12f, paint)
            }
        }

        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(RectF(300f, totalBoxY, contentRight, totalBoxY + totalsBoxHeight), 8f, 8f, paint)

        val totalQty = items.sumOf { it.quantity }
        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#4A5568")
        canvas.drawText("Total Items / Quantity:", 314f, totalBoxY + 20f, paint)
        drawRightAlignedText(canvas, "$totalQty pcs", contentRight - 12f, totalBoxY + 20f, paint, 95f)

        canvas.drawText("Total (${order.currency}):", 314f, totalBoxY + 38f, paint)
        val origTotalStr = "${order.currency} ${Formatters.formatMinorUnit(order.totalAmountMinor)}"
        drawRightAlignedText(canvas, origTotalStr, contentRight - 12f, totalBoxY + 38f, paint, 115f)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#263849")
        canvas.drawText("Total Equivalent (BDT):", 314f, totalBoxY + 56f, paint)
        val bdtTotalStr = "৳" + Formatters.formatMinorUnit(order.totalBdtAmountMinor)
        drawRightAlignedText(canvas, bdtTotalStr, contentRight - 12f, totalBoxY + 56f, paint, 110f)

        paint.color = if (order.paidAmountMinor >= order.totalAmountMinor) Color.parseColor("#276749") else Color.parseColor("#C53030")
        val outstanding = (order.totalAmountMinor - order.paidAmountMinor).coerceAtLeast(0L)
        canvas.drawText("Outstanding:", 314f, totalBoxY + 76f, paint)
        val outStr = "${order.currency} ${Formatters.formatMinorUnit(outstanding)}"
        drawRightAlignedText(canvas, outStr, contentRight - 12f, totalBoxY + 76f, paint, 125f)

        drawSignatures(canvas, paint, "Prepared by Import Officer", "Managing Director / Authorized Signatory")

        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "Internal_${sanitizeFileName(order.poNumber)}.pdf")
        try {
            FileOutputStream(file).use { out -> document.writeTo(out) }
        } finally {
            document.close()
        }
        return file
    }

    // Supplier Commercial Copy (Foreign currency or local supplier order, NO internal FX/profit rates)
    fun generateSupplierCommercialOrderPdf(
        context: Context,
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?
    ): File {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val marginX = 36f
        val contentRight = PAGE_WIDTH - 36f
        val maxContentBottom = PAGE_HEIGHT - 90f
        val isLocalBdtOrder = order.currency.equals("BDT", ignoreCase = true)
        val docTitle = if (isLocalBdtOrder) "SUPPLIER PURCHASE ORDER" else "SUPPLIER PURCHASE ORDER / 采购订单"

        fun drawSupplierTableHeader(canvas: Canvas, tableY: Float): Float {
            paint.color = Color.parseColor("#EDF2F7")
            canvas.drawRect(marginX, tableY, contentRight, tableY + 24f, paint)
            paint.color = Color.parseColor("#2D3748")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("#", 40f, tableY + 15f, paint)
            canvas.drawText("ITEM DESCRIPTION", 60f, tableY + 15f, paint)
            canvas.drawText("MODEL / SKU", 245f, tableY + 15f, paint)
            canvas.drawText("PACKING", 320f, tableY + 15f, paint)
            drawRightAlignedText(canvas, "QTY (PCS)", 425f, tableY + 15f, paint, 48f)
            drawRightAlignedText(canvas, "UNIT (${order.currency})", 488f, tableY + 15f, paint, 58f)
            drawRightAlignedText(canvas, "TOTAL (${order.currency})", 553f, tableY + 15f, paint, 60f)
            return tableY + 24f
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas

        drawBrandHeader(
            context, canvas, paint, businessProfile,
            docTitle, order.poNumber, dateFormat.format(Date(order.orderDate))
        )

        val boxY = 128f
        val supLeftX = 48f
        val supLeftMaxW = 285f
        val supRightX = 348f
        val supRightMaxW = contentRight - 12f - supRightX

        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val supNameLines = wrapText(supplier?.companyName ?: "Supplier / Overseas Factory", paint, supLeftMaxW, maxLines = 2)

        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        val supContactLines = wrapText(
            "Attn: ${supplier?.contactPerson ?: "Export Manager"}  |  Tel: ${supplier?.phone ?: "N/A"}",
            paint, supLeftMaxW, maxLines = 2
        )
        val supAddressLines = wrapText(
            "${supplier?.address ?: "Dhaka / Overseas"} • ${supplier?.country ?: ""}".trim(' ', '•'),
            paint, supLeftMaxW, maxLines = 2
        )

        val leftCardHeight = 28f + supNameLines.size * 14f + supContactLines.size * 12f + supAddressLines.size * 12f + 10f
        val rightCardHeight = 82f
        val metaBoxHeight = maxOf(leftCardHeight, rightCardHeight, 84f)

        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(RectF(marginX, boxY, contentRight, boxY + metaBoxHeight), 8f, 8f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRoundRect(RectF(marginX, boxY, contentRight, boxY + metaBoxHeight), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        var ly = boxY + 17f
        paint.color = Color.parseColor("#263849")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(
            if (isLocalBdtOrder) "SUPPLIER / VENDOR:" else "SUPPLIER / OVERSEAS FACTORY:",
            supLeftX, ly, paint
        )
        ly += 16f

        paint.color = Color.parseColor("#1A202C")
        paint.textSize = 11.5f
        supNameLines.forEach { line ->
            canvas.drawText(line, supLeftX, ly, paint)
            ly += 14f
        }

        paint.color = Color.parseColor("#718096")
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        supContactLines.forEach { line ->
            canvas.drawText(line, supLeftX, ly, paint)
            ly += 12f
        }
        supAddressLines.forEach { line ->
            canvas.drawText(line, supLeftX, ly, paint)
            ly += 12f
        }

        // Meta on Right (Commercial Currency, Packing Marks - STRICTLY NO Internal FX)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#792B87")
        paint.textSize = 9.5f
        canvas.drawText(fitSingleLineText("PO STATUS: ${order.status}", paint, supRightMaxW), supRightX, boxY + 17f, paint)
        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(fitSingleLineText("Commercial Currency: ${order.currency}", paint, supRightMaxW), supRightX, boxY + 34f, paint)
        canvas.drawText(fitSingleLineText("Total Cartons: ${order.totalCartons} CTN", paint, supRightMaxW), supRightX, boxY + 49f, paint)
        canvas.drawText(fitSingleLineText("Est. Gross Weight: ${order.totalGrossWeightKg} KG", paint, supRightMaxW), supRightX, boxY + 64f, paint)

        var currentY = drawSupplierTableHeader(canvas, boxY + metaBoxHeight + 14f)

        items.forEachIndexed { index, item ->
            val product = productsMap[item.productId]
            val pName = product?.nameEn ?: item.productId
            paint.textSize = 8.5f
            paint.typeface = Typeface.DEFAULT
            val descLines = wrapText(pName, paint, 178f, maxLines = 2).ifEmpty { listOf(item.productId) }
            val rowHeight = if (descLines.size > 1) 30f else 22f

            if (currentY + rowHeight > maxContentBottom) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                drawBrandHeader(
                    context, canvas, paint, businessProfile,
                    docTitle, "${order.poNumber} (P.$pageNumber)", dateFormat.format(Date(order.orderDate))
                )
                currentY = drawSupplierTableHeader(canvas, 128f)
            }

            if (index % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(marginX, currentY, contentRight, currentY + rowHeight, paint)
            }

            paint.textSize = 8.5f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#4A5568")
            canvas.drawText("${index + 1}", 40f, currentY + 14f, paint)

            paint.color = Color.parseColor("#1A202C")
            descLines.forEachIndexed { lineIdx, line ->
                canvas.drawText(line, 60f, currentY + 13f + lineIdx * 11f, paint)
            }

            paint.color = Color.parseColor("#718096")
            canvas.drawText(fitSingleLineText(product?.baseSku ?: "SKU", paint, 68f), 245f, currentY + 14f, paint)

            val packingStr = if (item.cartonQuantity > 0) "${item.cartonQuantity} CTN" else "-"
            canvas.drawText(fitSingleLineText(packingStr, paint, 52f), 320f, currentY + 14f, paint)

            paint.color = Color.parseColor("#1A202C")
            drawRightAlignedText(canvas, "${item.quantity}", 425f, currentY + 14f, paint, 48f)
            drawRightAlignedText(canvas, Formatters.formatMinorUnit(item.unitPriceMinor), 488f, currentY + 14f, paint, 58f)

            paint.color = Color.parseColor("#792B87")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawRightAlignedText(canvas, Formatters.formatMinorUnit(item.totalAmountMinor), 553f, currentY + 14f, paint, 60f)

            currentY += rowHeight
        }

        // Commercial Totals & Instructions Card cleanly split into Left (instructions <= 338f) and Right (totals 360f..547f)
        val notesStr = order.notes?.takeIf { it.isNotBlank() } ?: "[Terms: As agreed in quotation / proforma]"
        val instructionBullets = if (isLocalBdtOrder) {
            listOf(
                "Payment Terms: $notesStr",
                "Delivery Location: ${businessProfile?.address ?: "Company Central Warehouse"}",
                "Reference Marks: [${businessProfile?.companyName ?: "BUYER"}] / ${order.poNumber}",
                "Please inspect item count and packaging before dispatch."
            )
        } else {
            listOf(
                "Payment Terms: $notesStr",
                "Forwarder Warehouse: Guangzhou Baiyun / Yiwu Port Cargo Terminal",
                "Shipping Marks: [${businessProfile?.companyName ?: "IMPORT CO"}] / ${order.poNumber} / CTG PORT",
                "Please inspect packing quality before handover to logistics forwarder."
            )
        }

        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        val leftInstructMaxW = 290f // x=48..338, 22pt clear gap before Totals at x=360
        val wrappedInstructions = instructionBullets.flatMap { bullet ->
            val lines = wrapText("• $bullet", paint, leftInstructMaxW, maxLines = 3)
            lines
        }
        val boxHeight = maxOf(34f + wrappedInstructions.size * 12f + 10f, 100f)

        var totalBoxY = currentY + 14f
        if (totalBoxY + boxHeight > PAGE_HEIGHT - 80f) {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            drawBrandHeader(
                context, canvas, paint, businessProfile,
                docTitle, "${order.poNumber} (P.$pageNumber)", dateFormat.format(Date(order.orderDate))
            )
            totalBoxY = 132f
        }

        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(RectF(marginX, totalBoxY, contentRight, totalBoxY + boxHeight), 8f, 8f, paint)

        // Left Side: Delivery Instructions & Payment Terms (x = 48f..338f)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#263849")
        paint.textSize = 9f
        canvas.drawText(
            fitSingleLineText(
                if (isLocalBdtOrder) "DELIVERY & ORDER INSTRUCTIONS:" else "DELIVERY & SHIPPING INSTRUCTIONS (国内交货指引):",
                paint, leftInstructMaxW
            ),
            48f, totalBoxY + 19f, paint
        )

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8.5f
        paint.color = Color.parseColor("#4A5568")
        var instY = totalBoxY + 35f
        wrappedInstructions.forEach { line ->
            canvas.drawText(line, 48f, instY, paint)
            instY += 12f
        }

        // Right Side: Grand Totals in Order Currency Only (x = 360f..547f)
        val totalQty = items.sumOf { it.quantity }
        paint.textSize = 9.5f
        paint.color = Color.parseColor("#4A5568")
        canvas.drawText("Total Qty:", 360f, totalBoxY + 24f, paint)
        drawRightAlignedText(canvas, "$totalQty pcs", contentRight - 12f, totalBoxY + 24f, paint, 110f)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#792B87")
        paint.textSize = 11f
        canvas.drawText("TOTAL (${order.currency}):", 360f, totalBoxY + 50f, paint)
        val origTotalStr = "${order.currency} ${Formatters.formatMinorUnit(order.totalAmountMinor)}"
        drawRightAlignedText(canvas, origTotalStr, contentRight - 12f, totalBoxY + 50f, paint, 185f)

        drawSignatures(canvas, paint, "Prepared by Purchasing", "Confirmed by Supplier")

        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "Supplier_${sanitizeFileName(order.poNumber)}.pdf")
        try {
            FileOutputStream(file).use { out -> document.writeTo(out) }
        } finally {
            document.close()
        }
        return file
    }

    // Backward-compatible alias
    fun generatePurchaseOrderPdf(
        context: Context,
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        isBangla: Boolean = false
    ): File = generateCompanyInternalOrderPdf(context, order, items, productsMap, supplier, businessProfile, isBangla)

    // 2. SUPPLIER PAYMENT RECEIPT VOUCHER PDF
    fun generateSupplierPaymentReceiptPdf(
        context: Context,
        payment: SupplierPayment,
        order: PurchaseOrder?,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        isBangla: Boolean = false
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

        val docTitle = if (isBangla) "বৈদেশিক পেমেন্ট ভাউচার / PAYMENT RECEIPT" else "SUPPLIER PAYMENT VOUCHER"
        drawBrandHeader(
            context, canvas, paint, businessProfile,
            docTitle, payment.paymentNumber, dateFormat.format(Date(payment.paymentDate))
        )

        var y = 145f
        paint.color = Color.parseColor("#1A202C")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Remittance Paid To (Beneficiary): ${supplier?.companyName ?: "China Supplier"}", 36f, y, paint)

        y += 24f
        paint.textSize = 11f
        paint.color = Color.parseColor("#4A5568")
        paint.typeface = Typeface.DEFAULT
        val poLinked = if (order != null) "Linked PO: ${order.poNumber}" else "General Supplier Advance"
        canvas.drawText("$poLinked  |  Method: ${payment.paymentMethod}  |  Ref: ${payment.referenceNumber ?: "N/A"}", 36f, y, paint)

        y += 20f
        canvas.drawText("Disbursed Account: ${payment.paymentAccount}", 36f, y, paint)

        y += 32f
        paint.color = Color.parseColor("#276749")
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val amtStr = "Amount Remitted: ${payment.currency} ${Formatters.formatMinorUnit(payment.amountMinor)}  (৳ ${Formatters.formatMinorUnit(payment.bdtAmountMinor)} BDT)"
        canvas.drawText(amtStr, 36f, y, paint)

        y += 20f
        paint.textSize = 10f
        paint.color = Color.parseColor("#718096")
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Applied Exchange Rate: 1 ${payment.currency} = ${payment.exchangeRateToBdt} BDT", 36f, y, paint)

        // Summary Breakdown Box
        y += 35f
        paint.color = Color.parseColor("#EDF2F7")
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 24f, paint)

        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FINANCIAL RECONCILIATION SUMMARY", 48f, y + 16f, paint)

        y += 24f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#1A202C")
        if (order != null) {
            canvas.drawText("Total Purchase Order Value (${order.currency}):", 48f, y + 18f, paint)
            canvas.drawText("${order.currency} ${Formatters.formatMinorUnit(order.totalAmountMinor)}", PAGE_WIDTH - 180f, y + 18f, paint)
            y += 22f

            canvas.drawText("Total Cumulative Paid (${order.currency}):", 48f, y + 18f, paint)
            canvas.drawText("${order.currency} ${Formatters.formatMinorUnit(order.paidAmountMinor)}", PAGE_WIDTH - 180f, y + 18f, paint)
            y += 22f

            val rem = (order.totalAmountMinor - order.paidAmountMinor).coerceAtLeast(0L)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = if (rem == 0L) Color.parseColor("#276749") else Color.parseColor("#C53030")
            canvas.drawText("Remaining Outstanding (${order.currency}):", 48f, y + 18f, paint)
            canvas.drawText("${order.currency} ${Formatters.formatMinorUnit(rem)}", PAGE_WIDTH - 180f, y + 18f, paint)
            y += 22f
        } else {
            canvas.drawText("Supplier Current Outstanding Payable:", 48f, y + 18f, paint)
            canvas.drawText("৳ ${Formatters.formatMinorUnit(supplier?.outstandingPayableMinor ?: 0L)} BDT", PAGE_WIDTH - 180f, y + 18f, paint)
            y += 22f
        }

        drawSignatures(canvas, paint, "Remittance Officer / Cashier", "Chief Financial Officer / Authorized Bank Signatory")

        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "${sanitizeFileName(payment.paymentNumber)}.pdf")
        try {
            FileOutputStream(file).use { out -> document.writeTo(out) }
        } finally {
            document.close()
        }
        return file
    }

    // 3. SHIPMENT PACKING LIST & CARTON REPORT PDF
    fun generatePackingListPdf(
        context: Context,
        shipment: ImportShipment,
        cartons: List<ShipmentCarton>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        isBangla: Boolean = false
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

        val docTitle = if (isBangla) "প্যাকিং লিস্ট ও কার্টন রিপোর্ট / PACKING LIST" else "CARGO PACKING LIST & CBM"
        drawBrandHeader(
            context, canvas, paint, businessProfile,
            docTitle, shipment.shipmentNumber, dateFormat.format(Date(shipment.shippingDate))
        )

        val boxY = 135f
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(RectF(36f, boxY, PAGE_WIDTH - 36f, boxY + 70f), 8f, 8f, paint)

        paint.color = Color.parseColor("#263849")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CONSIGNMENT DETAILS:", 48f, boxY + 18f, paint)

        paint.color = Color.parseColor("#4A5568")
        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Origin: ${shipment.origin}  →  Destination: ${shipment.destination}", 48f, boxY + 34f, paint)
        canvas.drawText("Carrier / Line: ${shipment.shippingLine ?: "Air/Sea Carrier"}  |  Tracking/AWB: ${shipment.trackingReference ?: "N/A"}", 48f, boxY + 48f, paint)
        canvas.drawText("Supplier: ${supplier?.companyName ?: "China Supplier"}  |  Method: ${shipment.shippingMethod}", 48f, boxY + 62f, paint)

        // Summary Bar on Right
        paint.color = Color.parseColor("#792B87")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Cartons: ${shipment.totalCartons} | Total Qty: ${shipment.totalQuantity} pcs", 330f, boxY + 34f, paint)
        paint.color = Color.parseColor("#299DB2")
        canvas.drawText("Volume: ${shipment.totalCbm} m³ | Weight: ${shipment.totalGrossWeightKg} kg", 330f, boxY + 48f, paint)

        // Table Header
        val tableY = 220f
        paint.color = Color.parseColor("#EDF2F7")
        canvas.drawRect(36f, tableY, PAGE_WIDTH - 36f, tableY + 24f, paint)

        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("CARTON #", 44f, tableY + 16f, paint)
        canvas.drawText("PRODUCT / DESCRIPTION", 140f, tableY + 16f, paint)
        canvas.drawText("QTY", 335f, tableY + 16f, paint)
        canvas.drawText("DIMS (L×W×H cm)", 380f, tableY + 16f, paint)
        canvas.drawText("WEIGHT (kg)", 465f, tableY + 16f, paint)
        canvas.drawText("CBM (m³)", 525f, tableY + 16f, paint)

        var currentY = tableY + 24f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9f

        cartons.forEachIndexed { index, carton ->
            if (index % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(36f, currentY, PAGE_WIDTH - 36f, currentY + 22f, paint)
            }

            paint.color = Color.parseColor("#1A202C")
            canvas.drawText(carton.cartonNumber, 44f, currentY + 15f, paint)

            val product = productsMap[carton.productId]
            val pName = product?.nameEn ?: carton.productId
            val truncated = if (pName.length > 28) pName.take(26) + ".." else pName
            canvas.drawText(truncated, 140f, currentY + 15f, paint)

            canvas.drawText("${carton.quantity}", 338f, currentY + 15f, paint)
            canvas.drawText("${carton.lengthCm}×${carton.widthCm}×${carton.heightCm}", 380f, currentY + 15f, paint)
            canvas.drawText("${carton.grossWeightKg} kg", 465f, currentY + 15f, paint)
            paint.color = Color.parseColor("#299DB2")
            canvas.drawText("${carton.cbm} m³", 525f, currentY + 15f, paint)

            currentY += 22f
        }

        drawSignatures(canvas, paint, "Forwarding Agent / Inspector", "Receiving Warehouse Officer")

        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "packing_list_${sanitizeFileName(shipment.shipmentNumber)}.pdf")
        try {
            FileOutputStream(file).use { out -> document.writeTo(out) }
        } finally {
            document.close()
        }
        return file
    }

    // 4. LANDED COST ALLOCATION REPORT PDF
    fun generateLandedCostReportPdf(
        context: Context,
        shipment: ImportShipment,
        snapshots: List<LandedCostSnapshot>,
        costItems: List<ImportCostItem>,
        businessProfile: BusinessProfile?,
        isBangla: Boolean = false
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

        val docTitle = if (isBangla) "ল্যান্ডেড কস্ট রিপোর্ট / LANDED COST ENGINE" else "TRUE LANDED COST REPORT"
        drawBrandHeader(
            context, canvas, paint, businessProfile,
            docTitle, shipment.shipmentNumber, dateFormat.format(Date(System.currentTimeMillis()))
        )

        var y = 135f
        paint.color = Color.parseColor("#263849")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("IMPORT EXPENSES BREAKDOWN (ACTUAL DISBURSED):", 36f, y, paint)

        y += 18f
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 20f, paint)
        paint.color = Color.parseColor("#4A5568")
        paint.textSize = 9f
        canvas.drawText("CATEGORY", 44f, y + 14f, paint)
        canvas.drawText("DESCRIPTION", 180f, y + 14f, paint)
        canvas.drawText("BASIS METHOD", 360f, y + 14f, paint)
        canvas.drawText("AMOUNT (৳ BDT)", PAGE_WIDTH - 120f, y + 14f, paint)

        y += 20f
        paint.typeface = Typeface.DEFAULT
        var totalExpensesBdt = 0L

        costItems.forEach { cost ->
            totalExpensesBdt += cost.bdtAmountMinor
            paint.color = Color.parseColor("#1A202C")
            canvas.drawText(cost.costCategory, 44f, y + 14f, paint)
            canvas.drawText(cost.description.take(24), 180f, y + 14f, paint)
            canvas.drawText(cost.allocationMethod, 360f, y + 14f, paint)
            canvas.drawText("৳" + Formatters.formatMinorUnit(cost.bdtAmountMinor), PAGE_WIDTH - 120f, y + 14f, paint)
            y += 18f
        }

        y += 10f
        paint.color = Color.parseColor("#792B87")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val expSummary = "Total Import Expenses: ৳${Formatters.formatMinorUnit(totalExpensesBdt)} BDT"
        canvas.drawText(expSummary, PAGE_WIDTH - 36f - paint.measureText(expSummary), y, paint)

        y += 24f
        paint.color = Color.parseColor("#263849")
        paint.textSize = 11f
        canvas.drawText("FINAL PRODUCT LANDED COST ALLOCATION SNAPSHOT:", 36f, y, paint)

        y += 16f
        paint.color = Color.parseColor("#EDF2F7")
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 22f, paint)
        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 8.5f
        canvas.drawText("PRODUCT / SKU", 44f, y + 15f, paint)
        canvas.drawText("QTY", 210f, y + 15f, paint)
        canvas.drawText("FOB PURCHASE (৳)", 260f, y + 15f, paint)
        canvas.drawText("ALLOCATED EXP (৳)", 360f, y + 15f, paint)
        canvas.drawText("TOTAL BATCH (৳)", 460f, y + 15f, paint)
        canvas.drawText("UNIT COST (৳)", 530f, y + 15f, paint)

        y += 22f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8.5f

        snapshots.forEachIndexed { idx, s ->
            if (idx % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 20f, paint)
            }
            paint.color = Color.parseColor("#1A202C")
            canvas.drawText(s.productNameEn.take(24), 44f, y + 14f, paint)
            canvas.drawText("${s.totalReceivedQuantity}", 210f, y + 14f, paint)
            canvas.drawText("৳" + Formatters.formatMinorUnit(s.totalPurchaseCostBdtMinor), 260f, y + 14f, paint)
            paint.color = Color.parseColor("#299DB2")
            canvas.drawText("৳" + Formatters.formatMinorUnit(s.totalAllocatedCostsBdtMinor), 360f, y + 14f, paint)
            paint.color = Color.parseColor("#1A202C")
            canvas.drawText("৳" + Formatters.formatMinorUnit(s.totalLandedCostBdtMinor), 460f, y + 14f, paint)
            paint.color = Color.parseColor("#792B87")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("৳" + Formatters.formatMinorUnit(s.unitLandedCostBdtMinor), 530f, y + 14f, paint)
            paint.typeface = Typeface.DEFAULT
            y += 20f
        }

        drawSignatures(canvas, paint, "Costing & Allocation Accountant", "Chief Financial Officer")

        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "landed_cost_${sanitizeFileName(shipment.shipmentNumber)}.pdf")
        try {
            FileOutputStream(file).use { out -> document.writeTo(out) }
        } finally {
            document.close()
        }
        return file
    }

    // 5. WAREHOUSE RECEIVING REPORT PDF
    fun generateWarehouseReceivingReportPdf(
        context: Context,
        receipt: WarehouseReceipt,
        items: List<WarehouseReceiptItem>,
        productsMap: Map<String, Product>,
        businessProfile: BusinessProfile?,
        isBangla: Boolean = false
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

        val docTitle = if (isBangla) "ওয়্যারহাউস গ্রহণ রিপোর্ট / RECEIVING REPORT" else "WAREHOUSE RECEIVING REPORT"
        drawBrandHeader(
            context, canvas, paint, businessProfile,
            docTitle, receipt.receiptNumber, dateFormat.format(Date(receipt.receiptDate))
        )

        var y = 145f
        paint.color = Color.parseColor("#1A202C")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Facility: Central Warehouse (${receipt.warehouseId})  |  Inspector: ${receipt.receivedBy}", 36f, y, paint)

        y += 18f
        paint.textSize = 10f
        paint.color = Color.parseColor("#4A5568")
        paint.typeface = Typeface.DEFAULT
        val disStr = if (receipt.hasDiscrepancies) "Status: Discrepancies Noted (Inspection Pending Approval)" else "Status: Clean Inspection • Posted to Stock"
        canvas.drawText("$disStr  |  Remarks: ${receipt.notes ?: "Check-in complete"}", 36f, y, paint)

        // Table
        y += 30f
        paint.color = Color.parseColor("#EDF2F7")
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 24f, paint)

        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PRODUCT / DESCRIPTION", 44f, y + 16f, paint)
        canvas.drawText("EXPECTED", 220f, y + 16f, paint)
        canvas.drawText("RECEIVED GOOD", 290f, y + 16f, paint)
        canvas.drawText("DAMAGED", 380f, y + 16f, paint)
        canvas.drawText("MISSING", 440f, y + 16f, paint)
        canvas.drawText("UNIT LANDED COST", 500f, y + 16f, paint)

        y += 24f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9f

        items.forEachIndexed { index, item ->
            if (index % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 22f, paint)
            }
            val product = productsMap[item.productId]
            paint.color = Color.parseColor("#1A202C")
            canvas.drawText(product?.nameEn ?: item.productId, 44f, y + 15f, paint)
            canvas.drawText("${item.expectedQuantity}", 220f, y + 15f, paint)
            paint.color = Color.parseColor("#276749")
            canvas.drawText("${item.receivedQuantity}", 290f, y + 15f, paint)
            paint.color = if (item.damagedQuantity > 0) Color.parseColor("#C53030") else Color.parseColor("#718096")
            canvas.drawText("${item.damagedQuantity}", 380f, y + 15f, paint)
            paint.color = if (item.missingQuantity > 0) Color.parseColor("#C53030") else Color.parseColor("#718096")
            canvas.drawText("${item.missingQuantity}", 440f, y + 15f, paint)
            paint.color = Color.parseColor("#792B87")
            canvas.drawText("৳" + Formatters.formatMinorUnit(item.unitLandedCostMinor), 500f, y + 15f, paint)

            y += 22f
        }

        drawSignatures(canvas, paint, "Warehouse Officer / Storekeeper", "Inventory Manager / Auditor")

        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "${sanitizeFileName(receipt.receiptNumber)}.pdf")
        try {
            FileOutputStream(file).use { out -> document.writeTo(out) }
        } finally {
            document.close()
        }
        return file
    }

    fun generateCostVarianceReportPdf(
        context: Context,
        shipment: ImportShipment,
        costVarianceBdtMinor: Long,
        oldTotalExpenseBdtMinor: Long,
        newTotalExpenseBdtMinor: Long,
        reason: String,
        businessProfile: BusinessProfile?
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        drawBrandHeader(
            context = context,
            canvas = canvas,
            paint = paint,
            businessProfile = businessProfile,
            documentTitle = "COST VARIANCE REPORT",
            documentNumber = "VAR-${shipment.shipmentNumber}",
            dateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
        )

        var y = 140f
        paint.color = Color.parseColor("#1A202C")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SHIPMENT REFERENCE: ${shipment.shipmentNumber}", 36f, y, paint)
        canvas.drawText("ORIGIN / DEST: ${shipment.origin} -> ${shipment.destination}", 36f, y + 16f, paint)
        canvas.drawText("ADJUSTMENT REASON: $reason", 36f, y + 32f, paint)

        y += 60f
        // Summary Table
        paint.color = Color.parseColor("#EDF2F7")
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 26f, paint)

        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("COST METRIC", 44f, y + 17f, paint)
        canvas.drawText("AMOUNT (BDT)", PAGE_WIDTH - 150f, y + 17f, paint)

        y += 26f
        val metrics = listOf(
            "Original Estimated Import Cost" to oldTotalExpenseBdtMinor,
            "Actual Incurred Import Cost" to newTotalExpenseBdtMinor,
            "Landed Cost Variance" to costVarianceBdtMinor
        )

        metrics.forEachIndexed { index, (label, amount) ->
            if (index % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 24f, paint)
            }
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#2D3748")
            canvas.drawText(label, 44f, y + 16f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = if (label.contains("Variance")) Color.parseColor("#792B87") else Color.parseColor("#1A202C")
            val amountStr = "৳ " + Formatters.formatMinorUnit(amount)
            canvas.drawText(amountStr, PAGE_WIDTH - 150f, y + 16f, paint)
            y += 24f
        }

        drawSignatures(canvas, paint, "Cost Accountant / Analyst", "Chief Financial Officer (CFO)")
        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "Variance-${sanitizeFileName(shipment.shipmentNumber)}.pdf")
        try {
            FileOutputStream(file).use { out -> document.writeTo(out) }
        } finally {
            document.close()
        }
        return file
    }

    fun generateDamageLossReportPdf(
        context: Context,
        receipt: WarehouseReceipt,
        shipment: ImportShipment,
        items: List<WarehouseReceiptItem>,
        productsMap: Map<String, Product>,
        businessProfile: BusinessProfile?
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        drawBrandHeader(
            context = context,
            canvas = canvas,
            paint = paint,
            businessProfile = businessProfile,
            documentTitle = "DAMAGE & LOSS REPORT",
            documentNumber = "DMR-${receipt.receiptNumber}",
            dateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(receipt.receiptDate))
        )

        var y = 140f
        paint.color = Color.parseColor("#1A202C")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RECEIPT REF: ${receipt.receiptNumber}  |  SHIPMENT: ${shipment.shipmentNumber}", 36f, y, paint)
        canvas.drawText("INSPECTED BY: ${receipt.receivedBy}  |  WAREHOUSE: Tejgaon Central WH", 36f, y + 16f, paint)

        y += 45f
        paint.color = Color.parseColor("#FEE2E2")
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 24f, paint)

        paint.color = Color.parseColor("#991B1B")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PRODUCT", 44f, y + 16f, paint)
        canvas.drawText("EXPECTED", 240f, y + 16f, paint)
        canvas.drawText("DAMAGED", 320f, y + 16f, paint)
        canvas.drawText("MISSING", 400f, y + 16f, paint)
        canvas.drawText("STATUS / CLAIM", 470f, y + 16f, paint)

        y += 24f
        items.filter { it.damagedQuantity > 0 || it.missingQuantity > 0 }.forEachIndexed { index, item ->
            if (index % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 22f, paint)
            }
            val product = productsMap[item.productId]
            paint.color = Color.parseColor("#1A202C")
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(product?.nameEn ?: item.productId, 44f, y + 15f, paint)
            canvas.drawText("${item.expectedQuantity} pcs", 240f, y + 15f, paint)
            paint.color = Color.parseColor("#C53030")
            canvas.drawText("${item.damagedQuantity} pcs", 320f, y + 15f, paint)
            canvas.drawText("${item.missingQuantity} pcs", 400f, y + 15f, paint)
            paint.color = Color.parseColor("#792B87")
            canvas.drawText("Supplier Claim Pending", 470f, y + 15f, paint)
            y += 22f
        }

        drawSignatures(canvas, paint, "Warehouse Inspector", "Insurance / Claim Officer")
        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "DamageReport-${sanitizeFileName(receipt.receiptNumber)}.pdf")
        try {
            FileOutputStream(file).use { out -> document.writeTo(out) }
        } finally {
            document.close()
        }
        return file
    }

    fun generateInventoryCostLayerReportPdf(
        context: Context,
        layers: List<InventoryCostLayer>,
        productsMap: Map<String, Product>,
        businessProfile: BusinessProfile?
    ): File {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val marginX = 36f
        val contentRight = PAGE_WIDTH - 36f
        val maxContentBottom = PAGE_HEIGHT - 95f
        val docNo = "ICL-${System.currentTimeMillis() % 10000}"
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())

        fun drawLayerTableHeader(canvas: Canvas, headerY: Float): Float {
            paint.color = Color.parseColor("#EDF2F7")
            canvas.drawRect(marginX, headerY, contentRight, headerY + 24f, paint)
            paint.color = Color.parseColor("#2D3748")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("PRODUCT", 40f, headerY + 15f, paint)
            canvas.drawText("RECEIPT #", 182f, headerY + 15f, paint)
            drawRightAlignedText(canvas, "INIT", 302f, headerY + 15f, paint, 38f)
            drawRightAlignedText(canvas, "REM", 352f, headerY + 15f, paint, 42f)
            drawRightAlignedText(canvas, "UNIT COST", 448f, headerY + 15f, paint, 88f)
            drawRightAlignedText(canvas, "LAYER VALUE", 553f, headerY + 15f, paint, 96f)
            return headerY + 24f
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas

        drawBrandHeader(
            context = context,
            canvas = canvas,
            paint = paint,
            businessProfile = businessProfile,
            documentTitle = "FIFO COST LAYER VALUATION",
            documentNumber = docNo,
            dateFormatted = dateStr
        )

        var y = drawLayerTableHeader(canvas, 134f)
        var totalValuation = 0L

        layers.forEachIndexed { index, layer: InventoryCostLayer ->
            val product = productsMap[layer.productId]
            paint.textSize = 8.5f
            paint.typeface = Typeface.DEFAULT
            val prodLines = wrapText(product?.nameEn ?: layer.productId, paint, 136f, maxLines = 2)
                .ifEmpty { listOf(layer.productId) }
            val rowHeight = if (prodLines.size > 1) 28f else 22f

            if (y + rowHeight > maxContentBottom) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                drawBrandHeader(
                    context = context,
                    canvas = canvas,
                    paint = paint,
                    businessProfile = businessProfile,
                    documentTitle = "FIFO COST LAYER VALUATION",
                    documentNumber = "$docNo (P.$pageNumber)",
                    dateFormatted = dateStr
                )
                y = drawLayerTableHeader(canvas, 134f)
            }

            if (index % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(marginX, y, contentRight, y + rowHeight, paint)
            }

            val layerValue: Long = layer.remainingQuantity.toLong() * layer.unitLandedCostBdtMinor
            totalValuation += layerValue

            paint.textSize = 8.5f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#1A202C")
            prodLines.forEachIndexed { lineIdx, line ->
                canvas.drawText(line, 40f, y + 13f + lineIdx * 11f, paint)
            }

            paint.color = Color.parseColor("#4A5568")
            canvas.drawText(fitSingleLineText(layer.receiptNumber, paint, 76f), 182f, y + 14f, paint)
            drawRightAlignedText(canvas, "${layer.initialQuantity}", 302f, y + 14f, paint, 38f)

            paint.color = if (layer.remainingQuantity > 0) Color.parseColor("#276749") else Color.parseColor("#718096")
            drawRightAlignedText(canvas, "${layer.remainingQuantity}", 352f, y + 14f, paint, 42f)

            paint.color = Color.parseColor("#1A202C")
            drawRightAlignedText(canvas, "৳ " + Formatters.formatMinorUnit(layer.unitLandedCostBdtMinor), 448f, y + 14f, paint, 88f)

            paint.color = Color.parseColor("#792B87")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawRightAlignedText(canvas, "৳ " + Formatters.formatMinorUnit(layerValue), 553f, y + 14f, paint, 96f)
            y += rowHeight
        }

        if (y + 34f > PAGE_HEIGHT - 80f) {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            drawBrandHeader(
                context = context,
                canvas = canvas,
                paint = paint,
                businessProfile = businessProfile,
                documentTitle = "FIFO COST LAYER VALUATION",
                documentNumber = "$docNo (P.$pageNumber)",
                dateFormatted = dateStr
            )
            y = 136f
        } else {
            y += 12f
        }

        paint.color = Color.parseColor("#263849")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(
            fitSingleLineText("TOTAL INVENTORY VALUATION: ৳ ${Formatters.formatMinorUnit(totalValuation)}", paint, contentRight - 44f),
            44f, y + 16f, paint
        )

        drawSignatures(canvas, paint, "Warehouse Inventory Head", "Internal Audit / Controller")
        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "CostLayers-Valuation.pdf")
        try {
            FileOutputStream(file).use { out -> document.writeTo(out) }
        } finally {
            document.close()
        }
        return file
    }

    fun generateCbmReportPdf(
        context: Context,
        shipment: ImportShipment,
        cartons: List<ShipmentCarton>,
        productsMap: Map<String, Product>,
        businessProfile: BusinessProfile?
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        drawBrandHeader(
            context = context,
            canvas = canvas,
            paint = paint,
            businessProfile = businessProfile,
            documentTitle = "CBM & PACKING REPORT",
            documentNumber = "CBM-${shipment.shipmentNumber}",
            dateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(shipment.shippingDate))
        )

        var y = 140f
        paint.color = Color.parseColor("#1A202C")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SHIPMENT: ${shipment.shipmentNumber}  |  METHOD: ${shipment.shippingMethod}", 36f, y, paint)
        canvas.drawText("TOTAL CARTONS: ${shipment.totalCartons}  |  TOTAL CBM: ${shipment.totalCbm} m³  |  GROSS WEIGHT: ${shipment.totalGrossWeightKg} kg", 36f, y + 16f, paint)

        y += 45f
        paint.color = Color.parseColor("#EDF2F7")
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 24f, paint)

        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CARTON #", 44f, y + 16f, paint)
        canvas.drawText("PRODUCT", 120f, y + 16f, paint)
        canvas.drawText("QTY", 250f, y + 16f, paint)
        canvas.drawText("DIMS (L×W×H cm)", 300f, y + 16f, paint)
        canvas.drawText("WEIGHT (kg)", 420f, y + 16f, paint)
        canvas.drawText("CBM (m³)", 500f, y + 16f, paint)

        y += 24f
        cartons.forEachIndexed { index, ctn ->
            if (index % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 22f, paint)
            }
            val product = productsMap[ctn.productId]
            paint.color = Color.parseColor("#1A202C")
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(ctn.cartonNumber, 44f, y + 15f, paint)
            canvas.drawText(product?.nameEn ?: ctn.productId, 120f, y + 15f, paint)
            canvas.drawText("${ctn.quantity} pcs", 250f, y + 15f, paint)
            canvas.drawText("${ctn.lengthCm}×${ctn.widthCm}×${ctn.heightCm}", 300f, y + 15f, paint)
            canvas.drawText("${ctn.grossWeightKg} / ${ctn.netWeightKg}", 420f, y + 15f, paint)
            paint.color = Color.parseColor("#299DB2")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(ctn.cbm, 500f, y + 15f, paint)
            y += 22f
        }

        drawSignatures(canvas, paint, "Shipping Forwarder / Agent", "Receiving Port Logistics")
        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "CBM-${sanitizeFileName(shipment.shipmentNumber)}.pdf")
        try {
            FileOutputStream(file).use { out -> document.writeTo(out) }
        } finally {
            document.close()
        }
        return file
    }

    fun createSharePdfIntent(context: Context, pdfFile: File): Intent =
        createShareFileIntent(context, pdfFile, "application/pdf")

    fun createShareFileIntent(context: Context, file: File, mimeType: String): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun launchShareChooser(context: Context, file: File, mimeType: String, chooserTitle: String) {
        require(file.exists() && file.length() > 0L) { "Generated file is empty or missing" }
        val sendIntent = createShareFileIntent(context, file, mimeType)
        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context.findActivity() == null) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(chooser)
    }
}
