package com.pocketdev.advance_erp.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Thermal ESC/POS receipt PDF generator (58mm/80mm) and PDF share Intent helper.
 * Commercial A4 invoices, receipts, statements, and multi-module documents are generated
 * centrally via BrandedCommunicationPdfGenerator.
 */
object InvoicePdfGenerator {

    /**
     * Generates a thermal receipt PDF formatted for standard 80mm (226pt) or 58mm (164pt) ESC/POS thermal printers.
     */
    fun generateThermalReceiptPdf(
        context: Context,
        invoice: SalesInvoice,
        items: List<SalesInvoiceItem>,
        businessProfile: BusinessProfile?,
        cashTenderedMinor: Long = invoice.paidAmountMinor,
        changeReturnedMinor: Long = 0L,
        isBangla: Boolean = false,
        is80mm: Boolean = true
    ): File {
        val width = if (is80mm) 226 else 164
        val baseHeight = 380
        val itemsHeight = items.size * 24
        val height = baseHeight + itemsHeight

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)

        val leftMargin = 8f
        val rightMargin = width - 8f
        val centerX = width / 2f
        var currentY = 18f

        // Company Header
        paint.color = Color.BLACK
        paint.textSize = if (is80mm) 11f else 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        val companyName = businessProfile?.companyName ?: "ADVANCE IMPORT & EXPORT ERP"
        canvas.drawText(companyName, centerX, currentY, paint)
        currentY += 12f

        paint.textSize = 7.5f
        paint.typeface = Typeface.DEFAULT
        val address = businessProfile?.address ?: "Tejgaon CA, Dhaka-1208"
        canvas.drawText(address, centerX, currentY, paint)
        currentY += 10f

        val phone = "Tel: ${businessProfile?.phone ?: "+880 1700-000000"}"
        canvas.drawText(phone, centerX, currentY, paint)
        currentY += 10f

        val bin = "BIN: ${businessProfile?.taxNumber ?: "002891928-0101"}"
        canvas.drawText(bin, centerX, currentY, paint)
        currentY += 12f

        // Divider
        fun drawDashedLine(y: Float) {
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 7f
            val dash = if (is80mm) "------------------------------------------------------------" else "------------------------------------------"
            canvas.drawText(dash, leftMargin, y, paint)
        }

        drawDashedLine(currentY)
        currentY += 10f

        // Meta info
        paint.textSize = 7.5f
        paint.textAlign = Paint.Align.LEFT
        val titleText = if (isBangla) "নগদ বিক্রয় রশিদ / CASH RECEIPT" else "RETAIL SALES RECEIPT"
        canvas.drawText(titleText, leftMargin, currentY, paint)
        currentY += 10f

        canvas.drawText("Inv: ${invoice.invoiceNumber}", leftMargin, currentY, paint)
        currentY += 10f
        canvas.drawText("Date: ${dateFormat.format(Date(invoice.invoiceDate))}", leftMargin, currentY, paint)
        currentY += 10f
        canvas.drawText("Cust: ${invoice.customerName}", leftMargin, currentY, paint)
        currentY += 12f

        drawDashedLine(currentY)
        currentY += 10f

        // Table Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Item", leftMargin, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Total", rightMargin, currentY, paint)
        currentY += 10f

        // Items
        paint.typeface = Typeface.DEFAULT
        items.forEach { item ->
            paint.textAlign = Paint.Align.LEFT
            val itemName = if (item.productNameEn.length > 22) item.productNameEn.take(20) + ".." else item.productNameEn
            canvas.drawText(itemName, leftMargin, currentY, paint)
            currentY += 9f

            val subDesc = "${item.quantity} x ৳${Formatters.formatMinorUnit(item.unitPriceMinor)}"
            canvas.drawText(subDesc, leftMargin + 6f, currentY, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("৳" + Formatters.formatMinorUnit(item.lineTotalMinor), rightMargin, currentY, paint)
            currentY += 13f
        }

        drawDashedLine(currentY)
        currentY += 10f

        // Totals
        fun drawTotalRow(label: String, amountStr: String, isBold: Boolean = false) {
            paint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(label, leftMargin + 20f, currentY, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(amountStr, rightMargin, currentY, paint)
            currentY += 11f
        }

        drawTotalRow("Subtotal:", "৳" + Formatters.formatMinorUnit(invoice.subtotalMinor))
        if (invoice.discountMinor > 0) {
            drawTotalRow("Discount:", "-৳" + Formatters.formatMinorUnit(invoice.discountMinor))
        }
        drawTotalRow("NET TOTAL:", "৳" + Formatters.formatMinorUnit(invoice.totalAmountMinor), isBold = true)
        currentY += 2f
        drawTotalRow("Cash Paid:", "৳" + Formatters.formatMinorUnit(cashTenderedMinor))
        if (changeReturnedMinor > 0) {
            drawTotalRow("Change Due:", "৳" + Formatters.formatMinorUnit(changeReturnedMinor))
        }

        drawDashedLine(currentY)
        currentY += 14f

        // Barcode
        try {
            val barcodeBitmap = BarcodeGenerator.generateBarcodeBitmap(
                content = invoice.invoiceNumber,
                width = (width - 24),
                height = 36,
                includeText = true
            )
            canvas.drawBitmap(barcodeBitmap, 12f, currentY, paint)
            currentY += 46f
        } catch (e: Exception) {
            currentY += 10f
        }

        // Footer Message
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 7.5f
        paint.typeface = Typeface.DEFAULT
        val footerMsg = if (isBangla) "ধন্যবাদ, আবার আসবেন!" else "Thank you for shopping with us!"
        canvas.drawText(footerMsg, centerX, currentY, paint)

        document.finishPage(page)

        val dir = File(context.cacheDir, "receipts")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Receipt_${invoice.invoiceNumber}.pdf")
        FileOutputStream(file).use { outputStream ->
            document.writeTo(outputStream)
            outputStream.flush()
        }
        document.close()

        return file
    }

    fun createSharePdfIntent(context: Context, pdfFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
