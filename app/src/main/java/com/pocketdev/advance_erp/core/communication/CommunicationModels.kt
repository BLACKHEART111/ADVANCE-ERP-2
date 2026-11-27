package com.pocketdev.advance_erp.core.communication

import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import java.io.File

/** Only recipient-safe values enter this boundary; never pass accounting/COGS/cost entities to templates or exports. */
enum class ShareOutputFormat {
    PDF,
    IMAGE_CARD,
    TEXT_MESSAGE
}

fun CommunicationSettings.defaultSelectedFormats(): Set<ShareOutputFormat> = buildSet {
    if (includePdf) add(ShareOutputFormat.PDF)
    if (includeImageCard) add(ShareOutputFormat.IMAGE_CARD)
    if (includeTextMessage) add(ShareOutputFormat.TEXT_MESSAGE)
}.ifEmpty { setOf(ShareOutputFormat.TEXT_MESSAGE) }

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
    OVERDUE("Overdue", "মেয়াদোত্তীর্ণ বকেয়া"),
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

data class CommunicationCacheStats(
    val fileCount: Int = 0,
    val totalBytes: Long = 0L
)

data class CompanyBrandInfo(
    val companyName: String,
    val tagline: String? = null,
    val address: String = "",
    val phone: String = "",
    val email: String? = null,
    val website: String? = null,
    val binNumber: String? = null,
    val currencyCode: String = "BDT",
    val logoPath: String? = null
)

data class RecipientPartyInfo(
    val id: String,
    val code: String? = null,
    val name: String,
    val phone: String = "",
    val address: String? = null,
    val email: String? = null,
    val isSupplier: Boolean = false
)

data class RichDocumentLineItem(
    val index: Int,
    val productId: String,
    val variantId: String? = null,
    val titleEn: String,
    val titleBn: String,
    val sku: String? = null,
    val color: String? = null,
    val size: String? = null,
    val unit: String = "PCS",
    val thumbnailPath: String? = null,
    val quantity: Int,
    val secondaryQuantityText: String? = null,
    val unitPriceMinor: Long = 0L,
    val discountMinor: Long = 0L,
    val discountPercentBps: Long? = null,
    val lineTotalMinor: Long = 0L,
    val currencyCode: String = "BDT"
)

data class InvoiceFinancialBreakdown(
    val salesType: String = "WHOLESALE",
    val grossItemsTotalMinor: Long,
    val itemDiscountTotalMinor: Long,
    val subtotalMinor: Long,
    val invoiceDiscountMinor: Long,
    val invoiceDiscountPercentBps: Long? = null,
    val additionalChargesMinor: Long,
    val netInvoiceAmountMinor: Long,
    val creditReturnMinor: Long,
    val allocatedPaymentMinor: Long,
    val invoiceDueMinor: Long,
    val dueDateMillis: Long? = null,
    val paymentTerms: String? = null,
    val notes: String? = null,
    val qrVerificationPayload: String = "",
    val hasHistoricalSnapshot: Boolean,
    val historicalAsOfMillis: Long? = null,
    val historicalPreviousBalanceMinor: Long? = null,
    val historicalAllocatedPaymentMinor: Long? = null,
    val historicalInvoiceDueMinor: Long? = null,
    val historicalClosingBalanceMinor: Long? = null,
    val currentAccountBalanceMinor: Long,
    val asOfMillis: Long,
    val isReshareAfterLaterActivity: Boolean
)

data class PaymentReceiptAllocation(
    val invoiceId: String,
    val invoiceNumber: String,
    val amountMinor: Long
)

data class PaymentReceiptBreakdown(
    val paymentNumber: String,
    val paymentDateMillis: Long,
    val paymentAmountMinor: Long,
    val paymentMethod: String,
    val referenceNote: String? = null,
    val allocatedToInvoicesMinor: Long,
    val unallocatedAdvanceMinor: Long,
    val allocations: List<PaymentReceiptAllocation> = emptyList(),
    val previousBalanceMinor: Long? = null,
    val balanceAfterPaymentMinor: Long? = null,
    val currentAccountBalanceMinor: Long,
    val hasLaterActivityAfterPayment: Boolean = false,
    val currencyCode: String = "BDT"
)

data class DueReminderBreakdown(
    val currentTotalDueMinor: Long,
    val earliestDueDateMillis: Long? = null,
    val unpaidInvoiceCount: Int = 0,
    val priorBalanceMinor: Long,
    val latestInvoiceNumber: String? = null,
    val latestInvoiceDateMillis: Long? = null,
    val latestInvoiceAmountMinor: Long? = null,
    val latestPaymentNumber: String? = null,
    val latestPaymentDateMillis: Long? = null,
    val latestPaymentAmountMinor: Long? = null,
    val otherNetAdjustmentMinor: Long = 0L,
    val asOfMillis: Long
)

data class LedgerStatementRow(
    val dateMillis: Long,
    val referenceNumber: String,
    val transactionType: String,
    val description: String,
    val debitMinor: Long,
    val creditMinor: Long,
    val runningBalanceMinor: Long
)

data class LedgerStatementBreakdown(
    val fromMillis: Long,
    val toMillis: Long,
    val isAllTime: Boolean,
    val openingBalanceMinor: Long,
    val totalSalesOrPurchasesMinor: Long,
    val totalReturnMinor: Long,
    val totalRefundPaidMinor: Long,
    val totalPaymentMinor: Long,
    val netAdjustmentMinor: Long,
    val closingBalanceMinor: Long,
    val transactions: List<LedgerStatementRow> = emptyList(),
    val currencyCode: String = "BDT"
)

data class ReturnOrExchangeBreakdown(
    val referenceInvoiceNumber: String? = null,
    val returnAmountMinor: Long = 0L,
    val cashOrBankRefundMinor: Long = 0L,
    val creditedToAccountMinor: Long = 0L,
    val netPriceDifferenceMinor: Long = 0L,
    val refundOrPaymentMethod: String? = null,
    val currentAccountBalanceMinor: Long
)

data class DeliveryChallanBreakdown(
    val invoiceNumber: String,
    val deliveryPerson: String? = null,
    val transportInfo: String? = null,
    val notes: String? = null,
    val totalDispatchedQuantity: Int = 0
)

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
    val scheduledDeliveryInfo: String? = null,
    val documentDateMillis: Long? = null,
    val asOfMillis: Long = System.currentTimeMillis(),
    val brand: CompanyBrandInfo? = null,
    val party: RecipientPartyInfo? = null,
    val lineItems: List<RichDocumentLineItem> = emptyList(),
    val invoiceBreakdown: InvoiceFinancialBreakdown? = null,
    val paymentBreakdown: PaymentReceiptBreakdown? = null,
    val reminderBreakdown: DueReminderBreakdown? = null,
    val statementBreakdown: LedgerStatementBreakdown? = null,
    val returnExchangeBreakdown: ReturnOrExchangeBreakdown? = null,
    val deliveryBreakdown: DeliveryChallanBreakdown? = null
)

data class PreparedCommunication(
    val document: SafeDocument,
    val language: String,
    val message: String,
    val template: String,
    val isCustomTemplate: Boolean = false,
    val attachment: File? = null,
    val attachmentError: String? = null,
    val pdfFile: File? = attachment,
    val imageCardFile: File? = null,
    val imageCardPages: List<File> = emptyList(),
    val imageError: String? = null,
    val selectedFormats: Set<ShareOutputFormat> = emptySet()
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
