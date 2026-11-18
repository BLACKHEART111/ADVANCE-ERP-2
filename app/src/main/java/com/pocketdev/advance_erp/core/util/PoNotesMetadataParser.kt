package com.pocketdev.advance_erp.core.util

import com.pocketdev.advance_erp.domain.model.PaymentTermType

data class ParsedPoNotes(
    val paymentTerm: PaymentTermType,
    val forwarderAddress: String,
    val userNotes: String,
    val rawTermsText: String = paymentTerm.labelEn
)

/**
 * Shared parser for structured metadata stored inside [PurchaseOrder.notes]
 * (`[Terms: ...]`, `[Forwarder: ...]`, and clean user remarks).
 */
object PoNotesMetadataParser {

    const val DEFAULT_CHINA_FORWARDER = "Guangzhou Baiyun Cargo Terminal / Yiwu Port Warehouse, China"
    private val TERMS_REGEX = Regex("""\[Terms:\s*([^\]]+)]""")
    private val FORWARDER_REGEX = Regex("""\[Forwarder:\s*([^\]]+)]""")

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

        val cleanUserNotes = rawNotes
            .replace(TERMS_REGEX, "")
            .replace(FORWARDER_REGEX, "")
            .split("•")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" • ")

        return ParsedPoNotes(
            paymentTerm = resolvedTerm,
            forwarderAddress = resolvedForwarder,
            userNotes = cleanUserNotes,
            rawTermsText = termsText.ifBlank { resolvedTerm.labelEn }
        )
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
}
