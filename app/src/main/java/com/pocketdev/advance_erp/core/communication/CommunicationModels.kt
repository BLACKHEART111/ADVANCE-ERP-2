package com.pocketdev.advance_erp.core.communication

import java.io.File

/** Only recipient-safe values enter this boundary; never pass accounting entities to templates. */
enum class CommunicationModule(val en: String, val bn: String) {
    INVOICE("Sales Invoice", "বিক্রয় ইনভয়েস"),
    PAYMENT("Payment Receipt", "পেমেন্ট রসিদ"),
    LEDGER("Customer Ledger", "গ্রাহক লেজার"),
    REMINDER("Due Reminder", "বকেয়া স্মরণিকা"),
    DELIVERY("Delivery Challan", "ডেলিভারি চালান"),
    RETURN("Sales Return", "বিক্রয় ফেরত"),
    EXCHANGE("Product Exchange", "পণ্য বিনিময়"),
    QUOTATION("Quotation", "কোটেশন"),
    SALES_ORDER("Sales Order", "বিক্রয় আদেশ"),
    PURCHASE_ORDER("Purchase Order", "ক্রয় আদেশ"),
    PACKING("Packing List", "প্যাকিং তালিকা"),
    SUPPLIER_PAYMENT("Supplier Payment", "সরবরাহকারী পেমেন্ট"),
    STATEMENT("Customer Statement", "গ্রাহক বিবরণী"),
    SUPPLIER_STATEMENT("Supplier Statement", "সরবরাহকারী বিবরণী");

    fun title(language: String) = if (language == "bn") bn else en

    fun isSupplierModule(): Boolean =
        this in setOf(PURCHASE_ORDER, PACKING, SUPPLIER_PAYMENT, SUPPLIER_STATEMENT)
}

enum class CommunicationStatus(val en: String, val bn: String) {
    GENERAL("General", "সাধারণ"),
    PAID("Paid", "পরিশোধিত"),
    PARTIAL("Partially Paid", "আংশিক পরিশোধিত"),
    UNPAID("Unpaid", "অপরিশোধিত"),
    SETTLED("Settled (Return/Adj.)", "নিষ্পত্তি (ফেরত/সমন্বয়)"),
    PREVIOUS_DUE("With Previous Due", "পূর্বের বকেয়াসহ"),
    CREDIT("Advance / Credit", "অগ্রিম / ক্রেডিট");

    fun title(language: String): String = if (language == "bn") bn else en
}

data class CommunicationRequest(
    val module: CommunicationModule,
    val documentId: String,
    val from: Long? = null,
    val to: Long? = null
)

data class CommunicationChoice(val request: CommunicationRequest, val label: String)

data class SafeDocument(
    val request: CommunicationRequest,
    val number: String,
    val partyId: String,
    val partyName: String,
    val phone: String,
    val supplier: Boolean = false,
    val fields: Map<String, String>,
    val rows: List<List<String>> = emptyList(),
    val columns: List<String> = emptyList(),
    val status: CommunicationStatus = CommunicationStatus.GENERAL,
    val balanceMinor: Long? = null,
    val previousMinor: Long? = null,
    val warnings: List<String> = emptyList(),
    val validUntilMillis: Long? = null,
    val scheduledDeliveryInfo: String? = null
)

data class PreparedCommunication(
    val document: SafeDocument,
    val language: String,
    val message: String,
    val template: String,
    val isCustomTemplate: Boolean = false,
    val attachment: File? = null,
    val attachmentError: String? = null
)

object CommunicationPhone {
    /**
     * Normalizes Bangladesh (mobile 013-019, landline e.g. 02...) and international E.164 numbers,
     * translating Bangla digits (০-৯) to ASCII. Returns null if invalid or contains text.
     */
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val latin = trimmed.map { ch ->
            if (ch in '০'..'৯') ('0'.code + ch.code - '০'.code).toChar() else ch
        }.joinToString("")
        if (latin.any { !it.isDigit() && it !in "+ -()." }) return null
        var phone = latin.filter { it.isDigit() || it == '+' }
        if (phone.count { it == '+' } > 1 || ('+' in phone && !phone.startsWith('+'))) return null
        if (phone.startsWith("00")) phone = "+" + phone.drop(2)
        if (!phone.startsWith("+")) {
            when {
                phone.matches(Regex("01[3-9][0-9]{8}")) -> phone = "+88$phone"
                phone.matches(Regex("0[2-9][0-9]{6,8}")) -> phone = "+88$phone" // Bangladesh landline e.g. 029551234
                phone.matches(Regex("880[1-9][0-9]{7,9}")) -> phone = "+$phone"
                else -> return null
            }
        }
        if (!phone.matches(Regex("\\+[1-9][0-9]{7,14}"))) return null
        if (phone.startsWith("+880")) {
            val isBdMobile = phone.matches(Regex("\\+8801[3-9][0-9]{8}"))
            val isBdLandline = phone.matches(Regex("\\+880[2-9][0-9]{6,8}"))
            if (!isBdMobile && !isBdLandline) return null
        }
        return phone
    }
}
