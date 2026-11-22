package com.pocketdev.advance_erp.core.util

import com.pocketdev.advance_erp.domain.model.PaymentTermType
import java.nio.charset.StandardCharsets
import java.util.Base64

data class ParsedPoNotes(
    val paymentTerm: PaymentTermType,
    val forwarderAddress: String,
    val userNotes: String,
    val rawTermsText: String = paymentTerm.labelEn,
    val defaultShippingMark: String = "",
    val cartonShippingMarks: Map<String, String> = emptyMap(),
    val customAdvanceMinor: Long? = null
)

/**
 * Shared parser and serializer for structured metadata stored inside [PurchaseOrder.notes]
 * (`[Terms: ...]`, `[Forwarder: ...]`, `[ShippingMark: ...]`, `[CartonMarks: ...]`, and clean user remarks).
 * Pure Kotlin + java.util.Base64 (no org.json dependency) so both Android runtime and JVM unit tests execute identically.
 */
object PoNotesMetadataParser {

    const val DEFAULT_CHINA_FORWARDER = "Guangzhou Baiyun Cargo Terminal / Yiwu Port Warehouse, China"
    private val TERMS_REGEX = Regex("""\[Terms:\s*([^\]]+)]""")
    private val CUSTOM_ADV_REGEX = Regex("""\[CustomAdvanceMinor:\s*([0-9]+)]""")
    private val FORWARDER_REGEX = Regex("""\[Forwarder:\s*([^\]]+)]""")
    private val SHIPPING_MARK_REGEX = Regex("""\[ShippingMark:\s*([^\]]*)]""")
    private val CARTON_MARKS_REGEX = Regex("""\[CartonMarks:\s*([A-Za-z0-9+/=_-]+)]""")

    fun parse(
        rawNotes: String?,
        defaultForwarder: String = DEFAULT_CHINA_FORWARDER
    ): ParsedPoNotes {
        if (rawNotes.isNullOrBlank()) {
            return ParsedPoNotes(
                paymentTerm = PaymentTermType.THIRTY_SEVENTY,
                forwarderAddress = defaultForwarder,
                userNotes = "",
                rawTermsText = PaymentTermType.THIRTY_SEVENTY.labelEn
            )
        }

        val termsText = TERMS_REGEX.find(rawNotes)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val resolvedTerm = PaymentTermType.entries.firstOrNull {
            termsText.equals(it.labelEn, ignoreCase = true) || termsText.equals(it.labelBn, ignoreCase = true)
        } ?: when {
            termsText.contains("100% Advance", ignoreCase = true) || rawNotes.contains("100% Advance", ignoreCase = true) -> PaymentTermType.HUNDRED_PERCENT_ADVANCE
            termsText.contains("50%", ignoreCase = true) || rawNotes.contains("50% Advance", ignoreCase = true) -> PaymentTermType.FIFTY_FIFTY
            termsText.contains("Custom", ignoreCase = true) || rawNotes.contains("Custom Partial", ignoreCase = true) -> PaymentTermType.CUSTOM_PARTIAL
            termsText.contains("Full Due", ignoreCase = true) || rawNotes.contains("Full Due", ignoreCase = true) -> PaymentTermType.FULL_DUE
            else -> PaymentTermType.THIRTY_SEVENTY
        }

        val resolvedForwarder = FORWARDER_REGEX.find(rawNotes)?.groupValues?.getOrNull(1)?.trim()
            ?.ifBlank { defaultForwarder } ?: defaultForwarder

        val resolvedShippingMark = SHIPPING_MARK_REGEX.find(rawNotes)?.groupValues?.getOrNull(1)
            ?.replace("\\n", "\n")
            ?.replace("\\r", "")
            ?.trim()
            .orEmpty()

        val resolvedCartonMarks = CARTON_MARKS_REGEX.find(rawNotes)?.groupValues?.getOrNull(1)
            ?.let(::decodeCartonMarksBase64)
            ?: emptyMap()

        val resolvedCustomAdvance = CUSTOM_ADV_REGEX.find(rawNotes)?.groupValues?.getOrNull(1)?.toLongOrNull()

        val cleanUserNotes = rawNotes
            .replace(TERMS_REGEX, "")
            .replace(CUSTOM_ADV_REGEX, "")
            .replace(FORWARDER_REGEX, "")
            .replace(SHIPPING_MARK_REGEX, "")
            .replace(CARTON_MARKS_REGEX, "")
            .split("•")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" • ")

        return ParsedPoNotes(
            paymentTerm = resolvedTerm,
            forwarderAddress = resolvedForwarder,
            userNotes = cleanUserNotes,
            rawTermsText = termsText.ifBlank { resolvedTerm.labelEn },
            defaultShippingMark = resolvedShippingMark,
            cartonShippingMarks = resolvedCartonMarks,
            customAdvanceMinor = resolvedCustomAdvance
        )
    }

    fun formatCustomPaymentTermsLabel(
        currency: String,
        totalAmountMinor: Long,
        customAdvanceMinor: Long
    ): String {
        val advMinor = customAdvanceMinor.coerceAtLeast(0L).let { if (totalAmountMinor > 0L) it.coerceAtMost(totalAmountMinor) else it }
        val remMinor = if (totalAmountMinor > 0L) (totalAmountMinor - advMinor).coerceAtLeast(0L) else 0L
        val advText = Formatters.formatMinorUnit(advMinor)
        val remText = Formatters.formatMinorUnit(remMinor)
        return if (totalAmountMinor > 0L) {
            "Custom Partial: Advance $currency $advText + Balance $currency $remText"
        } else {
            "Custom Partial: Advance $currency $advText"
        }
    }

    fun serialize(
        paymentTermLabel: String,
        forwarderAddress: String,
        userNotes: String,
        defaultShippingMark: String = "",
        cartonShippingMarks: Map<String, String> = emptyMap(),
        customAdvanceMinor: Long? = null
    ): String {
        val parts = mutableListOf<String>()
        val cleanTerms = paymentTermLabel.trim()
        if (cleanTerms.isNotEmpty()) {
            parts += "[Terms: ${sanitizeBracketValue(cleanTerms)}]"
        }
        if (customAdvanceMinor != null && customAdvanceMinor > 0L) {
            parts += "[CustomAdvanceMinor: $customAdvanceMinor]"
        }
        val cleanForwarder = forwarderAddress.trim()
        if (cleanForwarder.isNotEmpty()) {
            parts += "[Forwarder: ${sanitizeBracketValue(cleanForwarder)}]"
        }
        val cleanDefaultMark = defaultShippingMark.trim()
        if (cleanDefaultMark.isNotEmpty()) {
            val encodedMark = sanitizeBracketValue(cleanDefaultMark).replace("\r\n", "\n").replace("\n", "\\n")
            parts += "[ShippingMark: $encodedMark]"
        }
        val cleanCartonMarks = cartonShippingMarks
            .mapNotNull { (k, v) ->
                val ck = k.trim()
                val cv = v.trim()
                if (ck.isNotEmpty() && cv.isNotEmpty()) ck to cv else null
            }
            .toMap(LinkedHashMap())
        if (cleanCartonMarks.isNotEmpty()) {
            parts += "[CartonMarks: ${encodeCartonMarksBase64(cleanCartonMarks)}]"
        }
        val cleanNotes = userNotes.trim()
        if (cleanNotes.isNotEmpty()) {
            parts += cleanNotes
        }
        return parts.joinToString(" • ")
    }

    fun remapCartonShippingMarks(
        marks: Map<String, String>,
        oldToNewCartonNumbers: Map<String, String>
    ): Map<String, String> {
        if (marks.isEmpty()) return emptyMap()
        val remapped = LinkedHashMap<String, String>()
        marks.forEach { (oldKey, value) ->
            val newKey = oldToNewCartonNumbers[oldKey]
            if (newKey != null && value.isNotBlank()) {
                remapped[newKey] = value
            }
        }
        return remapped
    }

    fun buildShippingMark(
        companyName: String?,
        poNumber: String,
        isLocalOrder: Boolean
    ): String {
        val cleanCompany = companyName?.trim()?.ifBlank { null } ?: if (isLocalOrder) "BUYER" else "IMPORT CO"
        return if (isLocalOrder) {
            "$cleanCompany / $poNumber"
        } else {
            "$cleanCompany / $poNumber / CTG PORT"
        }
    }

    private fun sanitizeBracketValue(value: String): String =
        value.replace("[", "(").replace("]", ")")

    private fun encodeCartonMarksBase64(marks: Map<String, String>): String {
        val json = buildString {
            append('{')
            marks.entries.forEachIndexed { idx, (k, v) ->
                if (idx > 0) append(',')
                append('"').append(escapeJsonString(k)).append("\":\"")
                append(escapeJsonString(v)).append('"')
            }
            append('}')
        }
        return Base64.getEncoder().encodeToString(json.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decodeCartonMarksBase64(encoded: String): Map<String, String> {
        return runCatching {
            val decoded = String(Base64.getDecoder().decode(encoded.trim()), StandardCharsets.UTF_8)
            parseJsonStringMap(decoded)
        }.getOrDefault(emptyMap())
    }

    private fun escapeJsonString(input: String): String = buildString(input.length + 8) {
        input.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }

    private fun parseJsonStringMap(json: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        var i = 0
        val len = json.length
        fun skipWs() {
            while (i < len && json[i].isWhitespace()) i++
        }
        fun readJsonString(): String? {
            skipWs()
            if (i >= len || json[i] != '"') return null
            i++
            val sb = StringBuilder()
            while (i < len) {
                val ch = json[i++]
                if (ch == '"') return sb.toString()
                if (ch == '\\' && i < len) {
                    when (val esc = json[i++]) {
                        '"', '\\', '/' -> sb.append(esc)
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> if (i + 4 <= len) {
                            val code = json.substring(i, i + 4).toIntOrNull(16)
                            if (code != null) {
                                sb.append(code.toChar())
                                i += 4
                            }
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(ch)
                }
            }
            return null
        }
        skipWs()
        if (i >= len || json[i] != '{') return emptyMap()
        i++
        while (i < len) {
            skipWs()
            if (i < len && json[i] == '}') break
            val key = readJsonString() ?: break
            skipWs()
            if (i >= len || json[i] != ':') break
            i++
            val value = readJsonString() ?: break
            result[key] = value
            skipWs()
            if (i < len && json[i] == ',') i++
        }
        return result
    }
}
