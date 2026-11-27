package com.pocketdev.advance_erp.core.communication.graphics

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Pure-JVM deterministic formatting helpers for branded PDFs and Image Cards.
 * Keeps currency grouping, signed balances, Bangla/English dates, and discount badges consistent.
 */
object BrandedDocumentFormatter {

    private val fullBnMonths = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    private val shortBnMonths = arrayOf(
        "জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে"
    )

    private val usSymbols = DecimalFormatSymbols(Locale.US)

    data class FormattedBalance(
        val amountText: String,
        val labelText: String,
        val combinedText: String,
        val tone: BrandedDocumentPalette.BalanceSemanticTone
    )

    fun formatNumberGrouped(minor: Long): String {
        val absBd = BigDecimal.valueOf(minor, 2).abs()
        val df = DecimalFormat("#,##0.00", usSymbols)
        return df.format(absBd)
    }

    fun formatTaka(
        minor: Long,
        spacedSymbol: Boolean = false,
        explicitPlus: Boolean = false
    ): String {
        val symbol = if (spacedSymbol) "৳ " else "৳"
        val mag = formatNumberGrouped(minor)
        return when {
            minor < 0L -> "- $symbol$mag"
            minor > 0L && explicitPlus -> "+ $symbol$mag"
            else -> "$symbol$mag"
        }
    }

    fun formatCurrencyCode(minor: Long, currencyCode: String = "BDT"): String {
        val mag = formatNumberGrouped(minor)
        val cleanCode = currencyCode.trim().ifBlank { "BDT" }
        return if (minor < 0L) "- $cleanCode $mag" else "$cleanCode $mag"
    }

    fun formatSignedBalance(
        minor: Long,
        isBangla: Boolean,
        useTakaSymbol: Boolean = true,
        spacedSymbol: Boolean = false
    ): FormattedBalance {
        val tone = BrandedDocumentPalette.resolveBalanceTone(minor)
        val mag = formatNumberGrouped(minor)
        val amountText = if (useTakaSymbol) {
            (if (spacedSymbol) "৳ " else "৳") + mag
        } else {
            "BDT $mag"
        }
        val labelText = when {
            minor < 0L -> if (isBangla) "অগ্রিম ক্রেডিট" else "Advance Credit"
            minor > 0L -> if (isBangla) "বকেয়া" else "Due"
            else -> if (isBangla) "পরিশোধিত" else "Settled"
        }
        return FormattedBalance(
            amountText = amountText,
            labelText = labelText,
            combinedText = "$amountText ($labelText)",
            tone = tone
        )
    }

    fun toBanglaDigits(value: String): String {
        val out = StringBuilder(value.length)
        for (ch in value) {
            out.append(if (ch in '0'..'9') ('০'.code + (ch.code - '0'.code)).toChar() else ch)
        }
        return out.toString()
    }

    fun formatDateTime(
        millis: Long,
        isBangla: Boolean,
        includeTime: Boolean = true,
        shortMonth: Boolean = false
    ): String {
        if (!isBangla) {
            val pattern = if (includeTime) "dd MMM yyyy, hh:mm a" else "dd MMM yyyy"
            return SimpleDateFormat(pattern, Locale.US).format(Date(millis))
        }
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val day = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val monthIdx = cal.get(Calendar.MONTH).coerceIn(0, 11)
        val monthName = if (shortMonth) shortBnMonths[monthIdx] else fullBnMonths[monthIdx]
        val year = cal.get(Calendar.YEAR).toString()
        val datePart = toBanglaDigits("$day") + " $monthName " + toBanglaDigits(year)
        if (!includeTime) return datePart
        val hour = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val minute = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
        return "$datePart, ${toBanglaDigits("$hour:$minute")}"
    }

    fun formatDateRange(
        fromMillis: Long,
        toMillis: Long,
        isAllTime: Boolean,
        isBangla: Boolean
    ): String {
        val endPart = formatDateTime(toMillis, isBangla, includeTime = false, shortMonth = true)
        val startPart = if (isAllTime && fromMillis <= 0L) {
            if (isBangla) "শুরু থেকে" else "Beginning"
        } else {
            formatDateTime(fromMillis, isBangla, includeTime = false, shortMonth = true)
        }
        return "$startPart – $endPart"
    }

    fun formatDiscountBadge(discountMinor: Long, discountPercentBps: Long?): String? {
        if (discountMinor <= 0L) return null
        val amountPart = formatNumberGrouped(discountMinor)
        return if (discountPercentBps != null && discountPercentBps > 0L) {
            val pctStr = BigDecimal.valueOf(discountPercentBps, 2)
                .stripTrailingZeros()
                .toPlainString()
            "$pctStr% (-৳$amountPart)"
        } else {
            "-৳$amountPart"
        }
    }
}
