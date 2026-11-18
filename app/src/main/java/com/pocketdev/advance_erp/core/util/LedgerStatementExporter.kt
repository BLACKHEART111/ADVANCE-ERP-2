package com.pocketdev.advance_erp.core.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.domain.model.LedgerReport
import java.io.File
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LedgerStatementExporter {
    fun money(minor: Long) = BigDecimal.valueOf(minor, 2).toPlainString()
    fun csvCell(value: String): String = "\"" + (if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@')) "'" else "") + value.replace("\"", "\"\"") + "\""

    fun pdf(context: Context, report: LedgerReport, profile: BusinessProfile?, bn: Boolean): File {
        val statement = requireNotNull(report.statement)
        val output = File(File(context.cacheDir, "reports_pdf").apply { mkdirs() }, "party_statement_${System.currentTimeMillis()}.pdf")
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val date = SimpleDateFormat("dd MMM yyyy", Locale.US)
        var page: PdfDocument.Page? = null
        var y = 0f
        var number = 0
        fun text(value: String, x: Float, baseline: Float, width: Float, size: Float = 9f, numeric: Boolean = false) {
            paint.textSize = size; paint.color = Color.rgb(30,41,59); paint.typeface = Typeface.DEFAULT
            var fitted = value.replace('\n', ' ')
            if (numeric) {
                while (paint.measureText(fitted) > width && paint.textSize > 4f) paint.textSize -= .25f
            } else {
                while (paint.measureText(fitted) > width && fitted.length > 1) fitted = fitted.dropLast(1)
                if (fitted != value && fitted.length > 1) fitted = fitted.dropLast(1) + "…"
            }
            page!!.canvas.drawText(fitted, x, baseline, paint)
        }
        fun finish() {
            page?.let { text("ADVANCE ERP • ${if (bn) "পৃষ্ঠা" else "Page"} $number • BDT",32f,818f,530f,8f); document.finishPage(it) }
            page = null
        }
        fun start(first: Boolean) {
            number++; page = document.startPage(PdfDocument.PageInfo.Builder(595,842,number).create())
            text(profile?.companyName ?: "ADVANCE ERP",32f,38f,531f,16f)
            text(statement.partyName + " • " + statement.partyCode,32f,60f,531f,12f)
            val (from,to) = report.filter.dateRangeOption.getBoundsMillis(report.filter.customStartDate, report.filter.customEndDate)
            text(if (from == 0L && to == Long.MAX_VALUE) "${statement.partyType} • All time" else "${statement.partyType} • ${date.format(Date(from))} — ${date.format(Date(to))}",32f,79f,531f)
            y = 100f
            if (first) {
                val labels = if (bn) listOf("প্রারম্ভিক", "ডেবিট", "ক্রেডিট", "সমাপনী") else listOf("Opening", "Debit", "Credit", "Closing")
                val values = listOf(statement.openingBalanceMinor, statement.totalDebitMinor, statement.totalCreditMinor, statement.closingBalanceMinor)
                labels.forEachIndexed { i,label -> text(label,32f+i*133f,y,125f); text(money(values[i]),32f+i*133f,y+18f,125f,11f,true) }
                y += 42f
                text(if (bn) "ব্যালেন্স সম্পূর্ণ সময়সীমার; নিচে নির্বাচিত filter-এর লেনদেন।" else "Balances cover the full period; rows below follow the selected filters.",32f,y,531f,8f)
                y += 15f
                if (report.legacyOpeningMinor != 0L) { text("Opening includes legacy/reconciliation difference: ${money(report.legacyOpeningMinor)} BDT. Verify source opening balance.",32f,y,531f,8f); y+=16f }
            }
            listOf("Date" to 32f,"Reference / Details" to 98f,"Debit" to 354f,"Credit" to 425f,"Balance" to 496f).forEach { (label,x) -> text(label,x,y,if(x==98f)245f else 64f,9f) }
            y+=20f
        }
        try {
            start(true)
            for (row in report.transactions) {
                if (y > 773f) { finish(); start(false) }
                text(date.format(Date(row.date)),32f,y,63f,8f)
                text(row.referenceNumber + " • " + row.status,98f,y,246f,9f)
                text(money(row.ledgerDebitMinor ?: 0),354f,y,66f,8f,true)
                text(money(row.ledgerCreditMinor ?: 0),425f,y,66f,8f,true)
                text(row.runningBalanceMinor?.let(::money) ?: "—",496f,y,67f,8f,true)
                text(row.particulars,98f,y+13f,246f,8f)
                val extra = row.originalCurrency?.let { "$it ${money(row.originalAmountMinor ?: 0)} @ ${row.exchangeRate ?: "—"} BDT" }
                if (extra != null) text(extra,98f,y+26f,246f,8f)
                y += if (extra == null) 35f else 47f
            }
            if (report.transactions.isEmpty()) text(if (bn) "এই filter-এ কোনো লেনদেন নেই।" else "No transactions match these filters.",32f,y,531f)
            finish()
            output.outputStream().use { document.writeTo(it) }
        } finally { document.close() }
        return output
    }
}
