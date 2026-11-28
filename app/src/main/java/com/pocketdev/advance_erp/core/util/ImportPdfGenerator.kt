package com.pocketdev.advance_erp.core.util

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.LandedCostSnapshot
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.SupplierPayment
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.data.entity.WarehouseReceiptItem
import java.io.ByteArrayOutputStream
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

    internal fun loadSquareThumbnailBitmap(pathOrUri: String?, targetPx: Int = 120): Bitmap? {
        if (pathOrUri.isNullOrBlank()) return null
        val file = File(pathOrUri.removePrefix("file://"))
        if (!file.exists() || !file.isFile) return null
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
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return null
            val side = minOf(decoded.width, decoded.height).coerceAtLeast(1)
            val left = (decoded.width - side) / 2
            val top = (decoded.height - side) / 2
            val square = Bitmap.createBitmap(decoded, left, top, side, side)
            if (square != decoded) decoded.recycle()
            val scaled = if (side > targetPx) Bitmap.createScaledBitmap(square, targetPx, targetPx, true) else square
            if (scaled != square) square.recycle()
            scaled
        } catch (_: Throwable) {
            null
        }
    }

    internal fun loadSquareThumbnailJpegBytes(pathOrUri: String?, targetPx: Int = 120): ByteArray? {
        val bmp = loadSquareThumbnailBitmap(pathOrUri, targetPx) ?: return null
        return try {
            ByteArrayOutputStream().use { baos ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 82, baos)
                baos.toByteArray()
            }
        } finally {
            bmp.recycle()
        }
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

        // Uploaded Company Logo (downsampled & recycled)
        val logoBitmap = CompanyBrandingHelper.loadCompanyLogoBitmap(
            context = context,
            businessProfile = businessProfile,
            fallbackDrawableRes = R.drawable.logo_monochrome_white
        )
        if (logoBitmap != null) {
            try {
                if (logoBitmap.height > 0 && logoBitmap.width > 0) {
                    val logoH = 28f
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

    private fun drawSignatures(
        canvas: Canvas,
        paint: Paint,
        leftLabel: String,
        rightLabel: String,
        context: Context? = null,
        signLeft: Boolean = false,
        signRight: Boolean = true
    ) {
        val footerY = PAGE_HEIGHT - 55f
        if (context != null) {
            if (signLeft) {
                CompanyBrandingHelper.drawAuthorizedSignature(context, canvas, 48f, 210f, footerY, paint)
            }
            if (signRight) {
                CompanyBrandingHelper.drawAuthorizedSignature(context, canvas, PAGE_WIDTH - 210f, PAGE_WIDTH - 48f, footerY, paint)
            }
        }
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

        drawSignatures(canvas, paint, "Prepared by Import Officer", "Managing Director / Authorized Signatory", context, signLeft = false, signRight = true)

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

    // Supplier Commercial Copy (Approved Reference Design: Deep Navy + Purple/Teal, Actual CTN Mapping, Structured Carton Table)
    fun generateSupplierCommercialOrderPdf(
        context: Context,
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        variantsMap: Map<String, ProductVariant> = emptyMap(),
        itemImagePaths: Map<String, String> = emptyMap(),
        cartons: List<ShipmentCarton> = emptyList(),
        selectedCartonNumbers: Set<String>? = null
    ): File {
        val exportData = PoExportDataBuilder.build(
            order = order,
            items = items,
            productsMap = productsMap,
            supplier = supplier,
            businessProfile = businessProfile,
            variantsMap = variantsMap,
            itemImagePaths = itemImagePaths,
            cartons = cartons,
            selectedCartonNumbers = selectedCartonNumbers
        )

        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val marginX = 30f
        val contentRight = PAGE_WIDTH - 30f
        val contentW = contentRight - marginX
        val maxContentBottom = PAGE_HEIGHT - 84f
        val navyDark = Color.parseColor("#1B2A4A")
        val purpleAccent = Color.parseColor("#792B87")
        val tealAccent = Color.parseColor("#0D9488")
        val docTitle = if (exportData.isLocalOrder) {
            if (exportData.isPartialCartonExport) "SUPPLIER PO (PARTIAL CARTONS)" else "SUPPLIER PURCHASE ORDER"
        } else {
            "SUPPLIER PURCHASE ORDER / 采购订单"
        }

        fun drawSupplierHeaderBanner(canvas: Canvas, pageLabel: String) {
            paint.color = navyDark
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 106f, paint)

            // Gradient-style dual accent bar
            paint.color = purpleAccent
            canvas.drawRect(0f, 106f, PAGE_WIDTH * 0.52f, 111f, paint)
            paint.color = Color.parseColor("#299DB2")
            canvas.drawRect(PAGE_WIDTH * 0.52f, 106f, PAGE_WIDTH.toFloat(), 111f, paint)

            val logoBitmap = CompanyBrandingHelper.loadCompanyLogoBitmap(
                context = context,
                businessProfile = businessProfile,
                fallbackDrawableRes = R.drawable.logo_monochrome_white
            )
            if (logoBitmap != null) {
                try {
                    if (logoBitmap.height > 0 && logoBitmap.width > 0) {
                        val logoH = 28f
                        val logoW = logoH * (logoBitmap.width.toFloat() / logoBitmap.height.toFloat())
                        val logoDest = RectF(marginX, 14f, (marginX + logoW).coerceAtMost(310f), 14f + logoH)
                        canvas.drawBitmap(logoBitmap, null, logoDest, paint)
                    }
                } finally {
                    logoBitmap.recycle()
                }
            }

            // Company info on Left
            val leftMaxW = 305f
            paint.color = Color.WHITE
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(fitSingleLineText(exportData.companyName, paint, leftMaxW), marginX, 58f, paint)

            paint.textSize = 7.8f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#E2E8F0")
            val compContactParts = buildList {
                if (exportData.companyAddress.isNotBlank()) add(exportData.companyAddress)
                if (exportData.companyPhone.isNotBlank()) add("Phone: ${exportData.companyPhone}")
                if (exportData.companyBin.isNotBlank()) add("BIN: ${exportData.companyBin}")
            }.joinToString("  |  ")
            if (compContactParts.isNotBlank()) {
                wrapText(compContactParts, paint, leftMaxW, maxLines = 2).forEachIndexed { idx, line ->
                    canvas.drawText(line, marginX, 72f + idx * 10.5f, paint)
                }
            }

            // Right PO title + Pill Badge
            val rightMaxW = 210f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#67E8F9")
            paint.textSize = 12.5f
            val titleLines = wrapText(docTitle, paint, rightMaxW, maxLines = 2)
            titleLines.forEachIndexed { idx, line ->
                drawRightAlignedText(canvas, line, contentRight, 28f + idx * 14f, paint, rightMaxW)
            }

            val poBadgeText = if (pageLabel.isBlank()) "PO #: ${exportData.poNumber}" else "PO #: ${exportData.poNumber} ($pageLabel)"
            paint.textSize = 9.2f
            val badgeW = (paint.measureText(poBadgeText) + 18f).coerceAtMost(rightMaxW)
            val badgeRect = RectF(contentRight - badgeW, 56f, contentRight, 75f)
            paint.color = Color.parseColor("#0F172A")
            canvas.drawRoundRect(badgeRect, 9f, 9f, paint)
            paint.color = Color.WHITE
            drawRightAlignedText(canvas, poBadgeText, contentRight - 9f, 69f, paint, badgeW - 14f)

            paint.textSize = 8.4f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#CBD5E1")
            drawRightAlignedText(canvas, "Date: ${exportData.orderDateFormatted}", contentRight, 91f, paint, rightMaxW)
        }

        // 9-Column Product Table Header
        // Cols: #(30..50) Product(50..186) Photo(188..218) Color(220..268) Size(270..312) CTN(314..382) Qty(384..432) Unit(434..495) Total(497..565)
        fun drawProductTableHeader(canvas: Canvas, y: Float, isPartialLabel: Boolean = false): Float {
            var curY = y
            // Navy section banner
            paint.color = navyDark
            canvas.drawRoundRect(RectF(marginX, curY, contentRight, curY + 20f), 4f, 4f, paint)
            paint.color = Color.WHITE
            paint.textSize = 8.8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val secTitle = if (isPartialLabel) {
                "PRODUCT ORDER DETAILS (CTN ALLOCATIONS FOR SELECTED ${exportData.exportedCartonCount} CARTONS)"
            } else {
                "PRODUCT ORDER DETAILS"
            }
            canvas.drawText(secTitle, marginX + 8f, curY + 13.5f, paint)
            curY += 23f

            paint.color = Color.parseColor("#EDF2F7")
            canvas.drawRect(marginX, curY, contentRight, curY + 21f, paint)
            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 7.4f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("#", marginX + 5f, curY + 13.5f, paint)
            canvas.drawText("Product Name / SKU", 50f, curY + 13.5f, paint)
            canvas.drawText("Photo", 189f, curY + 13.5f, paint)
            canvas.drawText("Color", 222f, curY + 13.5f, paint)
            canvas.drawText("Size", 271f, curY + 13.5f, paint)
            canvas.drawText("CTN", 315f, curY + 13.5f, paint)
            drawRightAlignedText(canvas, "Qty (pcs)", 432f, curY + 13.5f, paint, 48f)
            drawRightAlignedText(canvas, "Unit (${exportData.currency})", 494f, curY + 13.5f, paint, 56f)
            drawRightAlignedText(canvas, "Total (${exportData.currency})", contentRight - 4f, curY + 13.5f, paint, 64f)
            return curY + 21f
        }

        // 6-Column Carton Packing Table Header
        // Cols: CTN(30..92) Details(94..352) Qty(354..404) G.W.(406..452) Dim(454..522) CBM(524..565)
        fun drawCartonTableHeader(canvas: Canvas, y: Float): Float {
            var curY = y
            paint.color = navyDark
            canvas.drawRoundRect(RectF(marginX, curY, contentRight, curY + 20f), 4f, 4f, paint)
            paint.color = Color.WHITE
            paint.textSize = 8.8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val title = if (exportData.isPartialCartonExport) {
                "CARTON PACKING BREAKDOWN (SELECTED ${exportData.exportedCartonCount} OF ${exportData.allCartons.size} CTN)"
            } else {
                "CARTON PACKING BREAKDOWN (${exportData.exportedCartonCount} CTN)"
            }
            canvas.drawText(title, marginX + 8f, curY + 13.5f, paint)
            curY += 23f

            paint.color = Color.parseColor("#EDF2F7")
            canvas.drawRect(marginX, curY, contentRight, curY + 20f, paint)
            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 7.4f
            canvas.drawText("CTN No.", marginX + 6f, curY + 13f, paint)
            canvas.drawText("Packing Details (Product / Color / Size × Qty)", 94f, curY + 13f, paint)
            drawRightAlignedText(canvas, "Qty (pcs)", 402f, curY + 13f, paint, 46f)
            drawRightAlignedText(canvas, "G.W. (KG)", 450f, curY + 13f, paint, 46f)
            drawRightAlignedText(canvas, "Dimension (cm)", 520f, curY + 13f, paint, 66f)
            drawRightAlignedText(canvas, "CBM", contentRight - 4f, curY + 13f, paint, 40f)
            return curY + 20f
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas

        drawSupplierHeaderBanner(canvas, "")

        // Side-by-side Supplier & Order Summary Cards
        val boxY = 122f
        val gap = 10f
        val halfW = (contentW - gap) / 2f
        val leftRight = marginX + halfW
        val rightStart = leftRight + gap

        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val supNameLines = wrapText(exportData.supplierName, paint, halfW - 22f, maxLines = 2)
        paint.textSize = 8.3f
        paint.typeface = Typeface.DEFAULT
        val supContactLine = "Attn: ${exportData.supplierContactPerson}  |  Tel: ${exportData.supplierPhone}"
        val supAddrLines = wrapText(exportData.supplierAddress, paint, halfW - 22f, maxLines = 2)
        val leftBoxH = 26f + supNameLines.size * 13f + 12f + supAddrLines.size * 11f + 10f
        val summaryRowCount = 6 +
            (if (exportData.isPartialCartonExport) 1 else 0) +
            (if (exportData.supplierChinaDeliveryMinor > 0L) 1 else 0)
        val rightBoxH = 26f + summaryRowCount * 14f + 10f
        val cardsH = maxOf(leftBoxH, rightBoxH, 102f)

        // Left Card: Supplier
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(RectF(marginX, boxY, leftRight, boxY + cardsH), 8f, 8f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.9f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawRoundRect(RectF(marginX, boxY, leftRight, boxY + cardsH), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        var ly = boxY + 16f
        paint.color = navyDark
        paint.textSize = 8.4f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(
            if (exportData.isLocalOrder) "SUPPLIER / LOCAL VENDOR" else "SUPPLIER / OVERSEAS FACTORY",
            marginX + 10f, ly, paint
        )
        ly += 14f
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 10.5f
        supNameLines.forEach { line ->
            canvas.drawText(line, marginX + 10f, ly, paint)
            ly += 13f
        }
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8.2f
        paint.color = Color.parseColor("#475569")
        canvas.drawText(fitSingleLineText(supContactLine, paint, halfW - 20f), marginX + 10f, ly, paint)
        ly += 12f
        supAddrLines.forEach { line ->
            canvas.drawText(line, marginX + 10f, ly, paint)
            ly += 11f
        }

        // Right Card: Order Summary
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(RectF(rightStart, boxY, contentRight, boxY + cardsH), 8f, 8f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawRoundRect(RectF(rightStart, boxY, contentRight, boxY + cardsH), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        var ry = boxY + 16f
        paint.color = navyDark
        paint.textSize = 8.4f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ORDER SUMMARY", rightStart + 10f, ry, paint)
        ry += 14f

        fun drawSummaryRow(label: String, value: String, boldValue: Boolean = false, pillColor: Int? = null) {
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8.2f
            paint.color = Color.parseColor("#475569")
            canvas.drawText(label, rightStart + 10f, ry, paint)
            if (pillColor != null) {
                paint.textSize = 7.6f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val pw = paint.measureText(value) + 12f
                val pillRect = RectF(contentRight - 10f - pw, ry - 9.5f, contentRight - 10f, ry + 3f)
                paint.color = pillColor
                canvas.drawRoundRect(pillRect, 6f, 6f, paint)
                paint.color = Color.WHITE
                drawRightAlignedText(canvas, value, contentRight - 16f, ry, paint, pw)
            } else {
                paint.typeface = Typeface.create(Typeface.DEFAULT, if (boldValue) Typeface.BOLD else Typeface.NORMAL)
                paint.color = if (boldValue) purpleAccent else Color.parseColor("#0F172A")
                paint.textSize = if (boldValue) 8.8f else 8.2f
                drawRightAlignedText(canvas, value, contentRight - 10f, ry, paint, halfW * 0.55f)
            }
            ry += 13.5f
        }

        val statusPillColor = when (exportData.status) {
            PurchaseOrderStatus.DRAFT.name -> Color.parseColor("#D97706")
            PurchaseOrderStatus.CANCELLED.name -> Color.parseColor("#DC2626")
            else -> Color.parseColor("#16A34A")
        }
        drawSummaryRow("PO Status:", exportData.status, pillColor = statusPillColor)
        drawSummaryRow("Currency:", exportData.currency, boldValue = true)
        drawSummaryRow("Total Quantity:", "${Formatters.formatNumber(exportData.poTotalQuantity.toLong(), com.pocketdev.advance_erp.core.model.AppLanguage.ENGLISH)} pcs")
        drawSummaryRow("Total Cartons:", if (exportData.poTotalCartons > 0) "${exportData.poTotalCartons} CTN" else "-")
        val gwLabel = if (order.isActualCostFinalized) "Actual Gross Weight:" else "Est. Gross Weight:"
        val gwValue = if (exportData.poTotalGrossWeightKg > java.math.BigDecimal.ZERO) {
            "${exportData.poTotalGrossWeightKg.setScale(1, java.math.RoundingMode.HALF_UP)} KG"
        } else "-"
        drawSummaryRow(gwLabel, gwValue)
        if (exportData.isPartialCartonExport) {
            drawSummaryRow("Selected Export:", "${exportData.exportedCartonCount} CTN (${exportData.exportedPackedQuantity} pcs)", boldValue = true)
        }
        if (exportData.supplierChinaDeliveryMinor > 0L) {
            drawSummaryRow("China Local Del.:", "${exportData.currency} ${Formatters.formatMinorUnit(exportData.supplierChinaDeliveryMinor)}")
        }
        drawSummaryRow("Total Order Amount:", "${exportData.currency} ${Formatters.formatMinorUnit(exportData.poGrandTotalMinor)}", boldValue = true)

        var currentY = drawProductTableHeader(canvas, boxY + cardsH + 12f, isPartialLabel = exportData.isPartialCartonExport)

        // Render Product Rows with Pagination
        exportData.items.forEach { row ->
            paint.textSize = 7.8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val nameLines = wrapText(row.productName, paint, 134f, maxLines = 2).ifEmpty { listOf(row.item.productId) }
            paint.textSize = 7.4f
            paint.typeface = Typeface.DEFAULT
            val colorLines = wrapText(row.color, paint, 46f, maxLines = 2).ifEmpty { listOf("-") }
            val sizeLines = wrapText(row.size, paint, 40f, maxLines = 2).ifEmpty { listOf("-") }
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val ctnLines = wrapText(row.cartonDisplay, paint, 66f, maxLines = 3).ifEmpty { listOf("-") }

            val dynamicH = maxOf(
                38f,
                nameLines.size * 9.5f + 16f,
                ctnLines.size * 9.5f + 10f
            )

            if (currentY + dynamicH > maxContentBottom) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                drawSupplierHeaderBanner(canvas, "P.$pageNumber")
                currentY = drawProductTableHeader(canvas, 122f, isPartialLabel = exportData.isPartialCartonExport)
            }

            if (row.index % 2 == 0) {
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(marginX, currentY, contentRight, currentY + dynamicH, paint)
            }
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(marginX, currentY + dynamicH, contentRight, currentY + dynamicH, paint)

            // #
            paint.color = Color.parseColor("#475569")
            paint.textSize = 7.8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${row.index}", marginX + 5f, currentY + 18f, paint)

            // Product Name + SKU
            paint.color = Color.parseColor("#0F172A")
            var ny = currentY + 12f
            nameLines.forEach { line ->
                canvas.drawText(line, 50f, ny, paint)
                ny += 9.5f
            }
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 6.6f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(fitSingleLineText("SKU: ${row.sku}", paint, 134f), 50f, (ny + 1f).coerceAtMost(currentY + dynamicH - 4f), paint)

            // Thumbnail Photo (28x28)
            val thumbTop = currentY + (dynamicH - 28f) / 2f
            val thumbRect = RectF(189f, thumbTop, 217f, thumbTop + 28f)
            val thumbBmp = loadSquareThumbnailBitmap(row.imagePath, targetPx = 120)
            if (thumbBmp != null) {
                try {
                    canvas.drawBitmap(thumbBmp, Rect(0, 0, thumbBmp.width, thumbBmp.height), thumbRect, paint)
                } finally {
                    thumbBmp.recycle()
                }
            } else {
                paint.color = Color.parseColor("#F1F5F9")
                canvas.drawRoundRect(thumbRect, 3f, 3f, paint)
                paint.color = Color.parseColor("#94A3B8")
                paint.textSize = 7f
                canvas.drawText("-", 201f, thumbTop + 17f, paint)
            }
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.6f
            paint.color = Color.parseColor("#CBD5E1")
            canvas.drawRoundRect(thumbRect, 3f, 3f, paint)
            paint.style = Paint.Style.FILL

            // Color & Size
            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 7.5f
            paint.typeface = Typeface.DEFAULT
            colorLines.forEachIndexed { idx, line -> canvas.drawText(line, 222f, currentY + 15f + idx * 9.5f, paint) }
            sizeLines.forEachIndexed { idx, line -> canvas.drawText(line, 271f, currentY + 15f + idx * 9.5f, paint) }

            // Actual CTN(s)
            paint.color = purpleAccent
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 7.4f
            ctnLines.forEachIndexed { idx, line -> canvas.drawText(line, 315f, currentY + 15f + idx * 9.5f, paint) }

            // Qty, Unit, Total
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 7.8f
            paint.typeface = Typeface.DEFAULT
            drawRightAlignedText(canvas, "${row.quantity}", 432f, currentY + 20f, paint, 48f)
            drawRightAlignedText(canvas, Formatters.formatMinorUnit(row.unitPriceMinor), 494f, currentY + 20f, paint, 56f)
            paint.color = purpleAccent
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawRightAlignedText(canvas, Formatters.formatMinorUnit(row.lineTotalMinor), contentRight - 4f, currentY + 20f, paint, 64f)

            currentY += dynamicH
        }

        // Product Table Total Bar
        if (currentY + 24f > maxContentBottom) {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            drawSupplierHeaderBanner(canvas, "P.$pageNumber")
            currentY = 122f
        }
        val hasSupplierChinaDel = exportData.supplierChinaDeliveryMinor > 0L
        val itemsBarTotalMinor = if (hasSupplierChinaDel && exportData.items.isNotEmpty()) {
            exportData.poItemsSubtotalMinor
        } else {
            exportData.poGrandTotalMinor
        }
        paint.color = Color.parseColor("#FEF3C7")
        canvas.drawRect(marginX, currentY, contentRight, currentY + 22f, paint)
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 8.2f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(if (hasSupplierChinaDel) "PRODUCT SUBTOTAL" else "TOTAL PRODUCT ORDER", marginX + 8f, currentY + 14.5f, paint)
        drawRightAlignedText(canvas, "${Formatters.formatNumber(exportData.poTotalQuantity.toLong(), com.pocketdev.advance_erp.core.model.AppLanguage.ENGLISH)} pcs", 432f, currentY + 14.5f, paint, 68f)
        paint.color = purpleAccent
        drawRightAlignedText(canvas, "${exportData.currency} ${Formatters.formatMinorUnit(itemsBarTotalMinor)}", contentRight - 6f, currentY + 14.5f, paint, 115f)
        currentY += if (hasSupplierChinaDel) 22f else 32f
        if (hasSupplierChinaDel) {
            if (currentY + 46f > maxContentBottom) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                drawSupplierHeaderBanner(canvas, "P.$pageNumber")
                currentY = 122f
            }
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRect(marginX, currentY, contentRight, currentY + 19f, paint)
            paint.color = Color.parseColor("#334155")
            paint.textSize = 8.0f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("CHINA LOCAL DELIVERY (SUPPLIER)", marginX + 8f, currentY + 13f, paint)
            drawRightAlignedText(canvas, "+ ${exportData.currency} ${Formatters.formatMinorUnit(exportData.supplierChinaDeliveryMinor)}", contentRight - 6f, currentY + 13f, paint, 120f)
            currentY += 19f

            paint.color = Color.parseColor("#EDE9FE")
            canvas.drawRect(marginX, currentY, contentRight, currentY + 22f, paint)
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 8.4f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("GRAND TOTAL (SUPPLIER PAYABLE)", marginX + 8f, currentY + 14.5f, paint)
            paint.color = purpleAccent
            drawRightAlignedText(canvas, "${exportData.currency} ${Formatters.formatMinorUnit(exportData.poGrandTotalMinor)}", contentRight - 6f, currentY + 14.5f, paint, 125f)
            currentY += 32f
        }

        // Section E: Structured Carton Packing Breakdown Table
        if (exportData.exportedCartons.isNotEmpty()) {
            if (currentY + 66f > maxContentBottom) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                drawSupplierHeaderBanner(canvas, "P.$pageNumber")
                currentY = 122f
            }
            currentY = drawCartonTableHeader(canvas, currentY)

            exportData.exportedCartons.forEachIndexed { idx, ctn ->
                val detailSummary = ctn.lines.joinToString("; ") { l ->
                    val spec = listOfNotNull(
                        l.color.takeIf { it != "-" },
                        l.size.takeIf { it != "-" }
                    ).joinToString("/")
                    val prodTitle = if (spec.isNotBlank()) "${l.productName} ($spec)" else l.productName
                    "$prodTitle × ${l.quantity} pcs"
                }
                paint.textSize = 7.4f
                paint.typeface = Typeface.DEFAULT
                val detailLines = wrapText(detailSummary, paint, 254f, maxLines = maxOf(5, ctn.lines.size + 1)).ifEmpty { listOf("-") }
                val ctnRowH = maxOf(22f, detailLines.size * 9.5f + 8f)

                if (currentY + ctnRowH > maxContentBottom) {
                    document.finishPage(page)
                    pageNumber++
                    page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    canvas = page.canvas
                    drawSupplierHeaderBanner(canvas, "P.$pageNumber")
                    currentY = drawCartonTableHeader(canvas, 122f)
                }

                if (idx % 2 == 1) {
                    paint.color = Color.parseColor("#F8FAFC")
                    canvas.drawRect(marginX, currentY, contentRight, currentY + ctnRowH, paint)
                }
                paint.color = Color.parseColor("#E2E8F0")
                canvas.drawLine(marginX, currentY + ctnRowH, contentRight, currentY + ctnRowH, paint)

                paint.color = if (ctn.isMixed) tealAccent else purpleAccent
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 7.8f
                canvas.drawText(ctn.cartonNumber, marginX + 6f, currentY + 13.5f, paint)

                paint.color = Color.parseColor("#1E293B")
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 7.4f
                detailLines.forEachIndexed { lineIdx, dLine ->
                    canvas.drawText(dLine, 94f, currentY + 12f + lineIdx * 9.5f, paint)
                }

                paint.color = Color.parseColor("#0F172A")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                drawRightAlignedText(canvas, "${ctn.totalQty}", 402f, currentY + 13.5f, paint, 46f)
                paint.typeface = Typeface.DEFAULT
                val ctnGwStr = ctn.grossWeightKg.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString()
                drawRightAlignedText(canvas, ctnGwStr, 450f, currentY + 13.5f, paint, 46f)
                drawRightAlignedText(canvas, ctn.dimensionsDisplay, 520f, currentY + 13.5f, paint, 66f)
                val ctnCbmStr = ctn.cbm.setScale(3, java.math.RoundingMode.HALF_UP).toPlainString()
                drawRightAlignedText(canvas, ctnCbmStr, contentRight - 4f, currentY + 13.5f, paint, 40f)

                currentY += ctnRowH
            }

            // Carton Totals Row
            val footerRows = if (exportData.isPartialCartonExport) 2 else 1
            if (currentY + footerRows * 21f > maxContentBottom) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                drawSupplierHeaderBanner(canvas, "P.$pageNumber")
                currentY = 122f
            }
            paint.color = Color.parseColor("#FEF3C7")
            canvas.drawRect(marginX, currentY, contentRight, currentY + 21f, paint)
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val totalLabel = if (exportData.isPartialCartonExport) "Selected Cartons Subtotal" else "Total Packing"
            canvas.drawText(totalLabel, marginX + 8f, currentY + 14f, paint)
            drawRightAlignedText(canvas, "${exportData.exportedCartonCount} CTN", 345f, currentY + 14f, paint, 60f)
            drawRightAlignedText(canvas, "${exportData.exportedPackedQuantity}", 402f, currentY + 14f, paint, 46f)
            drawRightAlignedText(canvas, exportData.exportedGrossWeightKg.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString(), 450f, currentY + 14f, paint, 46f)
            drawRightAlignedText(canvas, exportData.exportedCbm.setScale(3, java.math.RoundingMode.HALF_UP).toPlainString(), contentRight - 4f, currentY + 14f, paint, 42f)
            currentY += 21f

            if (exportData.isPartialCartonExport) {
                paint.color = Color.parseColor("#F1F5F9")
                canvas.drawRect(marginX, currentY, contentRight, currentY + 19f, paint)
                paint.color = Color.parseColor("#475569")
                paint.textSize = 7.4f
                canvas.drawText("Complete PO Reference Total", marginX + 8f, currentY + 13f, paint)
                drawRightAlignedText(canvas, "${exportData.poTotalCartons} CTN", 345f, currentY + 13f, paint, 60f)
                drawRightAlignedText(canvas, "${exportData.poTotalQuantity}", 402f, currentY + 13f, paint, 46f)
                drawRightAlignedText(canvas, exportData.poTotalGrossWeightKg.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString(), 450f, currentY + 13f, paint, 46f)
                drawRightAlignedText(canvas, exportData.poTotalCbm.setScale(3, java.math.RoundingMode.HALF_UP).toPlainString(), contentRight - 4f, currentY + 13f, paint, 42f)
                currentY += 19f
            }
            currentY += 12f
        }

        // Section F: Side-by-Side DELIVERY INFO & PAYMENT TERMS + NOTE + Signatures
        paint.textSize = 7.8f
        paint.typeface = Typeface.DEFAULT
        val addrLines = wrapText(
            "${if (exportData.isLocalOrder) "Delivery Address" else "China Warehouse Address"}: ${exportData.deliveryAddress}",
            paint, halfW - 18f, maxLines = 3
        )
        val markLines = wrapText(
            "Shipping Mark: ${exportData.defaultShippingMark.replace("\n", " / ")}",
            paint, halfW - 18f, maxLines = 3
        )
        val payLines = wrapText(exportData.paymentTerms, paint, halfW - 18f, maxLines = 3)
        val delPayH = maxOf(
            26f + addrLines.size * 10.5f + markLines.size * 10.5f + 8f,
            26f + payLines.size * 10.5f + 12f,
            58f
        )

        val defaultInstructions = listOf(
            "• Please pack strictly according to the carton breakdown above.",
            "• Ensure correct color, size, SKU, and quantity in every carton.",
            exportData.userNotes.takeIf { it.isNotBlank() }?.let { "• $it" }
        ).filterNotNull().joinToString("\n")
        val noteLines = wrapText(defaultInstructions, paint, contentW - 24f, maxLines = 4)
        val noteH = 22f + noteLines.size * 10.5f + 8f

        val footerRequiredH = delPayH + 8f + noteH + 14f
        if (currentY + footerRequiredH > PAGE_HEIGHT - 74f) {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            drawSupplierHeaderBanner(canvas, "P.$pageNumber")
            currentY = 122f
        }

        // DELIVERY INFO card
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(RectF(marginX, currentY, leftRight, currentY + delPayH), 7f, 7f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = navyDark
        paint.textSize = 8.4f
        canvas.drawText("DELIVERY INFO", marginX + 10f, currentY + 15f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 7.8f
        paint.color = Color.parseColor("#334155")
        var dy = currentY + 27f
        addrLines.forEach { line -> canvas.drawText(line, marginX + 10f, dy, paint); dy += 10.5f }
        markLines.forEach { line -> canvas.drawText(line, marginX + 10f, dy, paint); dy += 10.5f }

        // PAYMENT TERMS card
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(RectF(rightStart, currentY, contentRight, currentY + delPayH), 7f, 7f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = navyDark
        paint.textSize = 8.4f
        canvas.drawText("PAYMENT TERMS", rightStart + 10f, currentY + 15f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 7.8f
        paint.color = Color.parseColor("#334155")
        var py = currentY + 27f
        payLines.forEach { line -> canvas.drawText(line, rightStart + 10f, py, paint); py += 10.5f }

        val noteY = currentY + delPayH + 8f
        paint.color = Color.parseColor("#FFFBEB")
        canvas.drawRoundRect(RectF(marginX, noteY, contentRight, noteY + noteH), 7f, 7f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#FDE68A")
        canvas.drawRoundRect(RectF(marginX, noteY, contentRight, noteY + noteH), 7f, 7f, paint)
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#92400E")
        paint.textSize = 8.2f
        canvas.drawText("NOTE & SUPPLIER INSTRUCTIONS", marginX + 10f, noteY + 14f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#78350F")
        paint.textSize = 7.6f
        var ny2 = noteY + 26f
        noteLines.forEach { line -> canvas.drawText(line, marginX + 10f, ny2, paint); ny2 += 10.5f }

        drawSignatures(canvas, paint, "Prepared by Purchasing", "Confirmed by Supplier", context, signLeft = true, signRight = false)

        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val suffix = if (exportData.isPartialCartonExport) "_Selected_${exportData.exportedCartonCount}CTN" else ""
        val file = File(dir, "Supplier_${sanitizeFileName(order.poNumber)}$suffix.pdf")
        try {
            FileOutputStream(file).use { out -> document.writeTo(out) }
        } finally {
            document.close()
        }
        return file
    }

    /**
     * Freight Forwarder Packing List PDF (Privacy-focused).
     * Includes embedded Product Photos, Quantity, Carton Dimensions (L/W/H cm), Gross/Net Weight (KG),
     * CBM (m³), and Shipping Marks — strictly omitting Color and all Prices/Amounts/Payment Terms.
     */
    fun generateFreightForwarderPackingListPdf(
        context: Context,
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        variantsMap: Map<String, ProductVariant> = emptyMap(),
        itemImagePaths: Map<String, String> = emptyMap(),
        cartons: List<ShipmentCarton> = emptyList(),
        selectedCartonNumbers: Set<String>? = null
    ): File {
        val exportData = PoExportDataBuilder.build(
            order = order,
            items = items,
            productsMap = productsMap,
            supplier = supplier,
            businessProfile = businessProfile,
            variantsMap = variantsMap,
            itemImagePaths = itemImagePaths,
            cartons = cartons,
            selectedCartonNumbers = selectedCartonNumbers
        )

        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val marginX = 30f
        val contentRight = PAGE_WIDTH - marginX
        val contentW = contentRight - marginX
        val maxContentBottom = PAGE_HEIGHT - 86f
        val navyDark = Color.parseColor("#1B2A4A")
        val tealAccent = Color.parseColor("#0D9488")
        val purpleAccent = Color.parseColor("#6D28D9")

        fun drawBanner(canvas: Canvas, pageLabel: String) {
            paint.style = Paint.Style.FILL
            paint.color = navyDark
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 106f, paint)
            paint.color = tealAccent
            canvas.drawRect(0f, 106f, PAGE_WIDTH * 0.52f, 111f, paint)
            paint.color = purpleAccent
            canvas.drawRect(PAGE_WIDTH * 0.52f, 106f, PAGE_WIDTH.toFloat(), 111f, paint)

            val logoBitmap = CompanyBrandingHelper.loadCompanyLogoBitmap(
                context = context,
                businessProfile = businessProfile,
                fallbackDrawableRes = R.drawable.logo_monochrome_white
            )
            if (logoBitmap != null) {
                try {
                    if (logoBitmap.height > 0 && logoBitmap.width > 0) {
                        val logoH = 28f
                        val logoW = logoH * (logoBitmap.width.toFloat() / logoBitmap.height.toFloat())
                        val logoDest = RectF(marginX, 14f, (marginX + logoW).coerceAtMost(310f), 14f + logoH)
                        canvas.drawBitmap(logoBitmap, null, logoDest, paint)
                    }
                } finally {
                    logoBitmap.recycle()
                }
            }

            paint.color = Color.WHITE
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(fitSingleLineText(exportData.companyName, paint, 305f), marginX, 58f, paint)

            paint.textSize = 7.8f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#E2E8F0")
            val contactLine = listOfNotNull(
                exportData.companyAddress.takeIf { it.isNotBlank() },
                exportData.companyPhone.takeIf { it.isNotBlank() }?.let { "Tel: $it" }
            ).joinToString("  |  ")
            if (contactLine.isNotBlank()) {
                wrapText(contactLine, paint, 305f, maxLines = 2).forEachIndexed { idx, line ->
                    canvas.drawText(line, marginX, 72f + idx * 10.5f, paint)
                }
            }

            paint.color = Color.parseColor("#5EEAD4")
            paint.textSize = 12.2f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawRightAlignedText(canvas, "FREIGHT FORWARDER PACKING LIST", contentRight, 34f, paint, 215f)

            val badgeText = if (pageLabel.isBlank()) "REF #: ${exportData.poNumber}" else "REF #: ${exportData.poNumber} ($pageLabel)"
            paint.textSize = 9.0f
            val badgeW = (paint.measureText(badgeText) + 18f).coerceAtMost(215f)
            paint.color = Color.parseColor("#0F172A")
            canvas.drawRoundRect(RectF(contentRight - badgeW, 48f, contentRight, 67f), 9f, 9f, paint)
            paint.color = Color.WHITE
            drawRightAlignedText(canvas, badgeText, contentRight - 9f, 61f, paint, badgeW - 14f)

            paint.textSize = 8.4f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#CBD5E1")
            drawRightAlignedText(canvas, "Date: ${exportData.orderDateFormatted}", contentRight, 84f, paint, 215f)
        }

        // Columns (No Color, No Prices):
        // CTN(30..80) Photo(82..114) Product/SKU(116..270) Size(272..314) Qty(316..358) G.W./N.W.(360..428) LxWxH cm(430..514) CBM(516..565)
        fun drawForwarderTableHeader(canvas: Canvas, y: Float): Float {
            var curY = y
            paint.color = navyDark
            canvas.drawRoundRect(RectF(marginX, curY, contentRight, curY + 20f), 4f, 4f, paint)
            paint.color = Color.WHITE
            paint.textSize = 8.6f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val secTitle = if (exportData.isPartialCartonExport) {
                "CARGO PACKING MANIFEST (${exportData.exportedCartonCount} OF ${exportData.allCartons.size} CTN)"
            } else {
                "CARGO PACKING MANIFEST (${exportData.exportedCartonCount.coerceAtLeast(exportData.poTotalCartons)} CTN)"
            }
            canvas.drawText(secTitle, marginX + 8f, curY + 13.5f, paint)
            curY += 23f

            paint.color = Color.parseColor("#EDF2F7")
            canvas.drawRect(marginX, curY, contentRight, curY + 21f, paint)
            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 7.3f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("CTN #", marginX + 4f, curY + 13.5f, paint)
            canvas.drawText("Photo", 84f, curY + 13.5f, paint)
            canvas.drawText("Product Name / SKU", 118f, curY + 13.5f, paint)
            canvas.drawText("Size", 274f, curY + 13.5f, paint)
            drawRightAlignedText(canvas, "Qty", 356f, curY + 13.5f, paint, 38f)
            drawRightAlignedText(canvas, "G.W./N.W.", 426f, curY + 13.5f, paint, 66f)
            drawRightAlignedText(canvas, "L×W×H (cm)", 512f, curY + 13.5f, paint, 82f)
            drawRightAlignedText(canvas, "CBM (m³)", contentRight - 4f, curY + 13.5f, paint, 46f)
            return curY + 21f
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        drawBanner(canvas, "")

        val boxY = 122f
        val gap = 10f
        val halfW = (contentW - gap) / 2f
        val leftRight = marginX + halfW
        val rightStart = leftRight + gap
        val cardsH = 102f

        // Left Card: Pickup / Forwarder Warehouse & Shipper
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(RectF(marginX, boxY, leftRight, boxY + cardsH), 8f, 8f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.9f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawRoundRect(RectF(marginX, boxY, leftRight, boxY + cardsH), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        var ly = boxY + 16f
        paint.color = navyDark
        paint.textSize = 8.4f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FORWARDER & PICKUP ADDRESS", marginX + 10f, ly, paint)
        ly += 13f
        paint.textSize = 7.9f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#334155")
        val forwarderLines = wrapText("Warehouse: ${exportData.deliveryAddress}", paint, halfW - 20f, maxLines = 2)
        forwarderLines.forEach { line -> canvas.drawText(line, marginX + 10f, ly, paint); ly += 10.5f }
        val markLines = wrapText("Shipping Mark: ${exportData.defaultShippingMark.replace("\n", " / ")}", paint, halfW - 20f, maxLines = 2)
        markLines.forEach { line -> canvas.drawText(line, marginX + 10f, ly, paint); ly += 10.5f }
        canvas.drawText(fitSingleLineText("Shipper: ${exportData.supplierName} (${exportData.supplierPhone})", paint, halfW - 20f), marginX + 10f, ly, paint)

        // Right Card: Cargo Measurement Totals (No Money)
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(RectF(rightStart, boxY, contentRight, boxY + cardsH), 8f, 8f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawRoundRect(RectF(rightStart, boxY, contentRight, boxY + cardsH), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        var ry = boxY + 16f
        paint.color = navyDark
        paint.textSize = 8.4f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CARGO WEIGHT & VOLUME SUMMARY", rightStart + 10f, ry, paint)
        ry += 15f
        val showQty = if (exportData.exportedCartons.isNotEmpty()) exportData.exportedPackedQuantity else exportData.poTotalQuantity
        val showCtn = if (exportData.exportedCartons.isNotEmpty()) exportData.exportedCartonCount else exportData.poTotalCartons
        val showGw = if (exportData.exportedCartons.isNotEmpty()) exportData.exportedGrossWeightKg else exportData.poTotalGrossWeightKg
        val showNw = if (exportData.exportedCartons.isNotEmpty()) exportData.exportedNetWeightKg else exportData.poTotalNetWeightKg
        val showCbm = if (exportData.exportedCartons.isNotEmpty()) exportData.exportedCbm else exportData.poTotalCbm

        val summaryPairs = listOf(
            "Total Cartons:" to "${if (showCtn > 0) showCtn else 0} CTN",
            "Total Units:" to "$showQty pcs",
            "Total Gross Weight:" to "${showGw.setScale(2, java.math.RoundingMode.HALF_UP)} KG",
            "Total Net Weight:" to "${showNw.setScale(2, java.math.RoundingMode.HALF_UP)} KG",
            "Total Volume (CBM):" to "${showCbm.setScale(4, java.math.RoundingMode.HALF_UP)} m³"
        )
        summaryPairs.forEachIndexed { sIdx, (lbl, valText) ->
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8.2f
            paint.color = Color.parseColor("#475569")
            canvas.drawText(lbl, rightStart + 10f, ry, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = if (sIdx == 4) tealAccent else Color.parseColor("#0F172A")
            drawRightAlignedText(canvas, valText, contentRight - 10f, ry, paint, halfW * 0.5f)
            ry += 14f
        }

        var currentY = drawForwarderTableHeader(canvas, boxY + cardsH + 12f)

        data class ForwarderDisplayRow(
            val ctnLabel: String,
            val imagePath: String?,
            val productName: String,
            val sku: String,
            val size: String,
            val qty: Int,
            val gwText: String,
            val nwText: String,
            val dimText: String,
            val cbmText: String
        )

        val rowsToRender: List<ForwarderDisplayRow> = if (exportData.exportedCartons.isNotEmpty()) {
            exportData.exportedCartons.flatMap { ctn ->
                ctn.lines.mapIndexed { lIdx, line ->
                    ForwarderDisplayRow(
                        ctnLabel = if (lIdx == 0) ctn.cartonNumber else "  ↳",
                        imagePath = line.imagePath,
                        productName = line.productName,
                        sku = line.sku,
                        size = line.size,
                        qty = line.quantity,
                        gwText = line.allocatedGrossWeightKg.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                        nwText = line.allocatedNetWeightKg.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                        dimText = if (lIdx == 0) ctn.dimensionsDisplay else "-",
                        cbmText = if (lIdx == 0) ctn.cbm.setScale(4, java.math.RoundingMode.HALF_UP).toPlainString() else "-"
                    )
                }
            }
        } else {
            exportData.items.map { itemRow ->
                val gwVal = itemRow.item.grossWeightKg.trim().toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
                val nwVal = itemRow.item.netWeightKg.trim().toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
                ForwarderDisplayRow(
                    ctnLabel = itemRow.cartonDisplay,
                    imagePath = itemRow.imagePath,
                    productName = itemRow.productName,
                    sku = itemRow.sku,
                    size = itemRow.size,
                    qty = itemRow.quantity,
                    gwText = gwVal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                    nwText = nwVal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                    dimText = "-",
                    cbmText = "-"
                )
            }
        }

        rowsToRender.forEachIndexed { idx, row ->
            paint.textSize = 7.7f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val nameLines = wrapText(row.productName, paint, 150f, maxLines = 2).ifEmpty { listOf("-") }
            val dynamicH = maxOf(38f, nameLines.size * 9.5f + 16f)

            if (currentY + dynamicH > maxContentBottom) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                drawBanner(canvas, "P.$pageNumber")
                currentY = drawForwarderTableHeader(canvas, 122f)
            }

            if (idx % 2 == 1) {
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(marginX, currentY, contentRight, currentY + dynamicH, paint)
            }
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(marginX, currentY + dynamicH, contentRight, currentY + dynamicH, paint)

            paint.color = purpleAccent
            paint.textSize = 7.6f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(fitSingleLineText(row.ctnLabel, paint, 48f), marginX + 4f, currentY + 18f, paint)

            val thumbTop = currentY + (dynamicH - 28f) / 2f
            val thumbRect = RectF(84f, thumbTop, 112f, thumbTop + 28f)
            val thumbBmp = loadSquareThumbnailBitmap(row.imagePath, targetPx = 120)
            if (thumbBmp != null) {
                try {
                    canvas.drawBitmap(thumbBmp, Rect(0, 0, thumbBmp.width, thumbBmp.height), thumbRect, paint)
                } finally {
                    thumbBmp.recycle()
                }
            } else {
                paint.color = Color.parseColor("#F1F5F9")
                canvas.drawRoundRect(thumbRect, 3f, 3f, paint)
                paint.color = Color.parseColor("#94A3B8")
                paint.textSize = 7f
                canvas.drawText("-", 96f, thumbTop + 17f, paint)
            }

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 7.7f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            var ny = currentY + 13f
            nameLines.forEach { line -> canvas.drawText(line, 118f, ny, paint); ny += 9.5f }
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 6.6f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(fitSingleLineText("SKU: ${row.sku}", paint, 150f), 118f, (ny + 1f).coerceAtMost(currentY + dynamicH - 4f), paint)

            paint.color = Color.parseColor("#334155")
            paint.textSize = 7.5f
            canvas.drawText(fitSingleLineText(row.size, paint, 40f), 274f, currentY + 18f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#0F172A")
            drawRightAlignedText(canvas, "${row.qty}", 356f, currentY + 18f, paint, 38f)

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 7.3f
            drawRightAlignedText(canvas, "${row.gwText} / ${row.nwText}", 426f, currentY + 18f, paint, 66f)
            drawRightAlignedText(canvas, row.dimText, 512f, currentY + 18f, paint, 82f)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = tealAccent
            drawRightAlignedText(canvas, row.cbmText, contentRight - 4f, currentY + 18f, paint, 46f)

            currentY += dynamicH
        }

        if (currentY + 62f > PAGE_HEIGHT - 74f) {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            drawBanner(canvas, "P.$pageNumber")
            currentY = 122f
        }

        currentY += 10f
        paint.color = Color.parseColor("#FEF3C7")
        canvas.drawRoundRect(RectF(marginX, currentY, contentRight, currentY + 24f), 6f, 6f, paint)
        paint.color = Color.parseColor("#92400E")
        paint.textSize = 8.4f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(
            "TOTAL: ${if (showCtn > 0) showCtn else 0} CTN  •  $showQty pcs  •  G.W. ${showGw.setScale(2, java.math.RoundingMode.HALF_UP)} KG  •  N.W. ${showNw.setScale(2, java.math.RoundingMode.HALF_UP)} KG  •  ${showCbm.setScale(4, java.math.RoundingMode.HALF_UP)} m³",
            marginX + 10f, currentY + 15.5f, paint
        )

        drawSignatures(canvas, paint, "Prepared by Shipper / Logistics", "Received by Freight Forwarder", context, signLeft = true, signRight = false)
        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val suffix = if (exportData.isPartialCartonExport) "_Selected_${exportData.exportedCartonCount}CTN" else ""
        val file = File(dir, "Forwarder_PackingList_${sanitizeFileName(order.poNumber)}$suffix.pdf")
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

        drawSignatures(canvas, paint, "Remittance Officer / Cashier", "Chief Financial Officer / Authorized Bank Signatory", context, signLeft = false, signRight = true)

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

        drawSignatures(canvas, paint, "Forwarding Agent / Inspector", "Receiving Warehouse Officer", context, signLeft = false, signRight = true)

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

        drawSignatures(canvas, paint, "Costing & Allocation Accountant", "Chief Financial Officer", context, signLeft = false, signRight = true)

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

        drawSignatures(canvas, paint, "Warehouse Officer / Storekeeper", "Inventory Manager / Auditor", context, signLeft = false, signRight = true)

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

        drawSignatures(canvas, paint, "Cost Accountant / Analyst", "Chief Financial Officer (CFO)", context, signLeft = false, signRight = true)
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

        drawSignatures(canvas, paint, "Warehouse Inspector", "Insurance / Claim Officer", context, signLeft = true, signRight = false)
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

        drawSignatures(canvas, paint, "Warehouse Inventory Head", "Internal Audit / Controller", context, signLeft = false, signRight = true)
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

        drawSignatures(canvas, paint, "Shipping Forwarder / Agent", "Receiving Port Logistics", context, signLeft = false, signRight = true)
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

    fun launchShareFilesChooser(
        context: Context,
        files: List<Pair<File, String>>,
        chooserTitle: String,
        extraText: String? = null
    ) {
        val validFiles = files.filter { it.first.exists() && it.first.length() > 0L }
        require(validFiles.isNotEmpty()) { "Generated file is empty or missing" }

        if (validFiles.size == 1 && extraText.isNullOrBlank()) {
            launchShareChooser(context, validFiles[0].first, validFiles[0].second, chooserTitle)
            return
        }

        val uris = ArrayList<Uri>(validFiles.size)
        var clipData: ClipData? = null
        for ((file, _) in validFiles) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            uris.add(uri)
            if (clipData == null) {
                clipData = ClipData.newRawUri("", uri)
            } else {
                clipData.addItem(ClipData.Item(uri))
            }
        }

        val sendIntent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = validFiles[0].second
                putExtra(Intent.EXTRA_STREAM, uris[0])
            }
        } else {
            val distinctMimes = validFiles.map { it.second }.distinct()
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = if (distinctMimes.size == 1) distinctMimes[0] else "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }.apply {
            this.clipData = clipData
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (!extraText.isNullOrBlank()) {
                putExtra(Intent.EXTRA_TEXT, extraText)
            }
        }

        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context.findActivity() == null) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(chooser)
    }
}
