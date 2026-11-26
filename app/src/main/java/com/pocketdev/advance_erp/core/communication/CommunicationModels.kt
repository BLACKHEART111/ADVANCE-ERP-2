package com.pocketdev.advance_erp.core.communication

import java.io.File

/** Only recipient-safe values enter this boundary; never pass accounting entities to templates. */
enum class CommunicationModule(val en: String, val bn: String) {
    INVOICE("Sales Invoice", "বিক্রয় ইনভয়েস"), PAYMENT("Payment Receipt", "পেমেন্ট রসিদ"),
    LEDGER("Customer Ledger", "গ্রাহক লেজার"), REMINDER("Due Reminder", "বকেয়া স্মরণিকা"),
    DELIVERY("Delivery Challan", "ডেলিভারি চালান"), RETURN("Sales Return", "বিক্রয় ফেরত"),
    EXCHANGE("Product Exchange", "পণ্য বিনিময়"), QUOTATION("Quotation", "কোটেশন"),
    SALES_ORDER("Sales Order", "বিক্রয় আদেশ"), PURCHASE_ORDER("Purchase Order", "ক্রয় আদেশ"),
    PACKING("Packing List", "প্যাকিং তালিকা"), SUPPLIER_PAYMENT("Supplier Payment", "সরবরাহকারী পেমেন্ট"),
    STATEMENT("Customer Statement", "গ্রাহক বিবরণী");
    fun title(language: String) = if (language == "bn") bn else en
}
enum class CommunicationStatus { GENERAL, PAID, PARTIAL, UNPAID, SETTLED, PREVIOUS_DUE, CREDIT }
data class CommunicationRequest(val module: CommunicationModule, val documentId: String, val from: Long? = null, val to: Long? = null)
data class CommunicationChoice(val request: CommunicationRequest, val label: String)
data class SafeDocument(
    val request: CommunicationRequest, val number: String, val partyId: String,
    val partyName: String, val phone: String, val supplier: Boolean = false,
    val fields: Map<String, String>, val rows: List<List<String>> = emptyList(),
    val columns: List<String> = emptyList(), val status: CommunicationStatus = CommunicationStatus.GENERAL,
    val balanceMinor: Long? = null, val previousMinor: Long? = null,
    val warnings: List<String> = emptyList()
)
data class PreparedCommunication(val document: SafeDocument, val language: String, val message: String,
    val template: String, val attachment: File? = null, val attachmentError: String? = null)

object CommunicationPhone {
    fun normalize(raw: String): String? {
        val latin = raw.map { if (it in '০'..'৯') ('0'.code + it.code - '০'.code).toChar() else it }.joinToString("")
        if (latin.any { !it.isDigit() && it !in "+ -()." }) return null
        var phone = latin.filter { it.isDigit() || it == '+' }
        if (phone.startsWith("00")) phone = "+" + phone.drop(2)
        if (phone.matches(Regex("01[3-9][0-9]{8}"))) phone = "+88$phone"
        else if (phone.matches(Regex("8801[3-9][0-9]{8}"))) phone = "+$phone"
        if (!phone.matches(Regex("\\+[1-9][0-9]{7,14}"))) return null
        if (phone.startsWith("+880") && !phone.matches(Regex("\\+8801[3-9][0-9]{8}"))) return null
        return phone
    }
}
