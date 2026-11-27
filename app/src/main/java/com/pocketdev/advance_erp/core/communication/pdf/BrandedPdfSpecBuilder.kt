package com.pocketdev.advance_erp.core.communication.pdf

import com.pocketdev.advance_erp.core.communication.CommunicationModule
import com.pocketdev.advance_erp.core.communication.CommunicationStatus
import com.pocketdev.advance_erp.core.communication.SafeDocument
import com.pocketdev.advance_erp.core.communication.graphics.BrandedDocumentFormatter
import com.pocketdev.advance_erp.core.communication.graphics.BrandedDocumentPalette
import com.pocketdev.advance_erp.core.communication.graphics.BrandedIconType
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import java.math.BigDecimal

enum class PdfColAlign {
    LEFT,
    CENTER,
    RIGHT
}

enum class PdfTableMode {
    COMMERCIAL_VARIANT_SPLIT,
    EXCHANGE_ITEMS,
    DELIVERY_CHALLAN,
    PACKING_LIST,
    PAYMENT_ALLOCATIONS,
    STATEMENT_LEDGER
}

data class PdfContactStripSpec(
    val address: String?,
    val phone: String?,
    val website: String?,
    val binNumber: String?
) {
    val isEmpty: Boolean
        get() = address.isNullOrBlank() && phone.isNullOrBlank() && website.isNullOrBlank() && binNumber.isNullOrBlank()
}

data class PdfKeyValueLine(
    val label: String,
    val value: String,
    val valueColor: Int = BrandedDocumentPalette.TextPrimary,
    val boldValue: Boolean = false
)

data class PdfInfoCardSpec(
    val title: String,
    val icon: BrandedIconType,
    val lines: List<PdfKeyValueLine>
)

data class PdfHeroBannerSpec(
    val label: String,
    val primaryValue: String,
    val subtitle: String? = null,
    val icon: BrandedIconType,
    val bgColor: Int,
    val borderColor: Int,
    val valueColor: Int
)

data class PdfKpiBoxSpec(
    val label: String,
    val value: String,
    val valueColor: Int = BrandedDocumentPalette.TextPrimary,
    val highlightedPurple: Boolean = false
)

data class PdfTableColumnSpec(
    val id: String,
    val titleTop: String,
    val titleBottom: String? = null,
    val widthPt: Float,
    val align: PdfColAlign,
    val groupHeaderTitle: String? = null
)

data class PdfTableRowSpec(
    val indexText: String,
    val showThumbnailSlot: Boolean,
    val thumbnailPath: String?,
    val primaryTitle: String,
    val skuSubtitle: String?,
    val discountBadgeText: String?,
    val extraCells: List<String>,
    val highlightAmountColor: Int = BrandedDocumentPalette.TextPrimary,
    val badgeTagText: String? = null,
    val badgeTagColor: Int = BrandedDocumentPalette.BrandPurplePrimary
)

data class PdfTotalPillSpec(
    val icon: BrandedIconType,
    val label: String,
    val amountText: String
)

data class PdfSummaryCardSpec(
    val title: String,
    val icon: BrandedIconType,
    val rows: List<PdfKeyValueLine>,
    val dividerBeforeLastRow: Boolean = false,
    val totalPill: PdfTotalPillSpec? = null,
    val footnote: String? = null
)

data class PdfBottomFooterBlockSpec(
    val notesTitle: String,
    val notesBullets: List<String>,
    val thankYouPrimary: String,
    val thankYouSecondary: String?,
    val qrPayload: String?,
    val qrCaptionPrimary: String?,
    val qrCaptionSecondary: String?,
    val showAuthorizedSignature: Boolean,
    val signaturePrimaryLabel: String,
    val signatureSecondaryLabel: String,
    val receiverSignatureLabel: String? = null
)

data class BrandedPdfDocumentSpec(
    val module: CommunicationModule,
    val safeFileStem: String,
    val isBangla: Boolean,
    val headerRightTop: String,
    val headerRightBottom: String,
    val headerRightIcon: BrandedIconType,
    val contactStrip: PdfContactStripSpec,
    val leftInfoCard: PdfInfoCardSpec,
    val rightInfoCard: PdfInfoCardSpec,
    val heroBanner: PdfHeroBannerSpec? = null,
    val kpiStrip: List<PdfKpiBoxSpec> = emptyList(),
    val tableTitle: String?,
    val tableMode: PdfTableMode?,
    val tableColumns: List<PdfTableColumnSpec> = emptyList(),
    val tableRows: List<PdfTableRowSpec> = emptyList(),
    val emptyTableMessage: String? = null,
    val leftSummaryCard: PdfSummaryCardSpec,
    val rightSummaryCard: PdfSummaryCardSpec?,
    val bottomBlock: PdfBottomFooterBlockSpec,
    val generatedOnText: String
)

data class PdfPageSlice(
    val pageNumber: Int,
    val isFirstPage: Boolean,
    val rowStartIndex: Int,
    val rowEndExclusive: Int,
    val includeSummarySection: Boolean,
    val includeBottomSection: Boolean
)

/**
 * Pure-JVM builder translating validated [SafeDocument] + [CommunicationSettings] into
 * deterministic A4 PDF specifications and multi-page pagination slices across all 14 modules.
 */
object BrandedPdfSpecBuilder {

    const val A4_WIDTH_PT = 595f
    const val A4_HEIGHT_PT = 842f
    const val MARGIN_X_PT = 28f
    const val CONTENT_WIDTH_PT = A4_WIDTH_PT - MARGIN_X_PT * 2f // 539f

    fun safeFileStem(document: SafeDocument): String {
        val cleaned = document.number.trim()
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        return cleaned.ifBlank { document.request.module.name }
    }

    fun formatMoney(minor: Long, currencyCode: String = "BDT", spacedTaka: Boolean = false): String {
        return if (currencyCode.equals("BDT", ignoreCase = true)) {
            BrandedDocumentFormatter.formatTaka(minor, spacedSymbol = spacedTaka)
        } else {
            BrandedDocumentFormatter.formatCurrencyCode(minor, currencyCode)
        }
    }

    fun formatSignedMoney(minor: Long, currencyCode: String = "BDT", spacedTaka: Boolean = false): String {
        if (currencyCode.equals("BDT", ignoreCase = true)) {
            return BrandedDocumentFormatter.formatTaka(minor, spacedSymbol = spacedTaka)
        }
        return BrandedDocumentFormatter.formatCurrencyCode(minor, currencyCode)
    }

    fun formatBalanceText(minor: Long, isBangla: Boolean, currencyCode: String = "BDT", spacedTaka: Boolean = true): String {
        return if (currencyCode.equals("BDT", ignoreCase = true)) {
            BrandedDocumentFormatter.formatSignedBalance(minor, isBangla, useTakaSymbol = true, spacedSymbol = spacedTaka).combinedText
        } else {
            val amount = BrandedDocumentFormatter.formatCurrencyCode(kotlin.math.abs(minor), currencyCode)
            val label = when {
                minor < 0L -> if (isBangla) "অগ্রিম ক্রেডিট" else "Advance Credit"
                minor > 0L -> if (isBangla) "বকেয়া" else "Due"
                else -> if (isBangla) "পরিশোধিত" else "Settled"
            }
            "$amount ($label)"
        }
    }

    fun buildSpec(
        document: SafeDocument,
        settings: CommunicationSettings,
        language: String,
        generatedAtMillis: Long = System.currentTimeMillis()
    ): BrandedPdfDocumentSpec {
        val bn = language == "bn"
        val brand = document.brand
        val contactStrip = PdfContactStripSpec(
            address = brand?.address?.trim()?.takeIf { it.isNotEmpty() },
            phone = brand?.phone?.trim()?.takeIf { it.isNotEmpty() },
            website = brand?.website?.trim()?.takeIf { it.isNotEmpty() },
            binNumber = brand?.binNumber?.trim()?.takeIf { it.isNotEmpty() }
        )
        val generatedOnText = (if (bn) "Generated on: " else "Generated on: ") +
            BrandedDocumentFormatter.formatDateTime(generatedAtMillis, isBangla = false, includeTime = true)

        return when (document.request.module) {
            CommunicationModule.INVOICE -> buildInvoiceSpec(document, settings, bn, contactStrip, generatedOnText)
            CommunicationModule.QUOTATION -> buildQuotationOrOrderSpec(document, settings, bn, contactStrip, generatedOnText, isQuotation = true)
            CommunicationModule.SALES_ORDER -> buildQuotationOrOrderSpec(document, settings, bn, contactStrip, generatedOnText, isQuotation = false)
            CommunicationModule.PURCHASE_ORDER -> buildPurchaseOrderSpec(document, settings, bn, contactStrip, generatedOnText)
            CommunicationModule.PAYMENT, CommunicationModule.SUPPLIER_PAYMENT -> buildPaymentReceiptSpec(document, settings, bn, contactStrip, generatedOnText)
            CommunicationModule.LEDGER, CommunicationModule.STATEMENT, CommunicationModule.SUPPLIER_STATEMENT -> buildStatementSpec(document, settings, bn, contactStrip, generatedOnText)
            CommunicationModule.REMINDER -> buildDueReminderSpec(document, settings, bn, contactStrip, generatedOnText)
            CommunicationModule.RETURN -> buildReturnSpec(document, settings, bn, contactStrip, generatedOnText)
            CommunicationModule.EXCHANGE -> buildExchangeSpec(document, settings, bn, contactStrip, generatedOnText)
            CommunicationModule.DELIVERY -> buildDeliverySpec(document, settings, bn, contactStrip, generatedOnText)
            CommunicationModule.PACKING -> buildPackingSpec(document, settings, bn, contactStrip, generatedOnText)
        }
    }

    private fun buildPartyInfoCard(document: SafeDocument, bn: Boolean): PdfInfoCardSpec {
        val p = document.party
        val isSup = document.supplier || p?.isSupplier == true
        val title = when {
            isSup && bn -> "সরবরাহকারীর তথ্য (Supplier Info)"
            isSup -> "Supplier Information"
            bn -> "গ্রাহকের তথ্য (Customer Information)"
            else -> "Customer Information"
        }
        val lines = buildList {
            add(
                PdfKeyValueLine(
                    label = when {
                        isSup && bn -> "প্রতিষ্ঠানের নাম"
                        isSup -> "Supplier Name"
                        bn -> "গ্রাহকের নাম"
                        else -> "Customer Name"
                    },
                    value = p?.name?.ifBlank { document.partyName } ?: document.partyName,
                    boldValue = true
                )
            )
            p?.code?.trim()?.takeIf { it.isNotEmpty() }?.let { code ->
                add(
                    PdfKeyValueLine(
                        label = when {
                            isSup && bn -> "সাপ্লায়ার কোড"
                            isSup -> "Supplier Code"
                            bn -> "কাস্টমার কোড"
                            else -> "Customer Code"
                        },
                        value = code
                    )
                )
            }
            val phoneVal = p?.phone?.trim()?.ifBlank { document.phone.trim() } ?: document.phone.trim()
            if (phoneVal.isNotEmpty()) {
                add(PdfKeyValueLine(label = if (bn) "মোবাইল" else "Mobile", value = phoneVal))
            }
            val addrVal = p?.address?.trim()?.takeIf { it.isNotEmpty() } ?: document.fields["customer_address"]?.trim()?.takeIf { it.isNotEmpty() }
            if (addrVal != null) {
                add(PdfKeyValueLine(label = if (bn) "ঠিকানা" else "Address", value = addrVal))
            }
        }
        return PdfInfoCardSpec(
            title = title,
            icon = if (isSup) BrandedIconType.BANK_BUILDING else BrandedIconType.CUSTOMER_USER,
            lines = lines
        )
    }

    private fun statusLine(status: CommunicationStatus, bn: Boolean): PdfKeyValueLine {
        val textColor = when (status) {
            CommunicationStatus.UNPAID, CommunicationStatus.OVERDUE, CommunicationStatus.PREVIOUS_DUE -> BrandedDocumentPalette.DueRedText
            CommunicationStatus.PARTIAL -> BrandedDocumentPalette.PartialAmberText
            CommunicationStatus.PAID, CommunicationStatus.SETTLED -> BrandedDocumentPalette.PaymentGreenText
            CommunicationStatus.CREDIT -> BrandedDocumentPalette.CreditBlueText
            CommunicationStatus.GENERAL -> BrandedDocumentPalette.BrandPurplePrimary
        }
        val label = when (status) {
            CommunicationStatus.UNPAID -> if (bn) "অপরিশোধিত (বকেয়া)" else "Unpaid (Due)"
            CommunicationStatus.OVERDUE -> if (bn) "মেয়াদোত্তীর্ণ (বকেয়া)" else "Overdue (Due)"
            CommunicationStatus.PARTIAL -> if (bn) "আংশিক পরিশোধিত" else "Partially Paid"
            CommunicationStatus.PAID -> if (bn) "পরিশোধিত" else "Paid"
            CommunicationStatus.SETTLED -> if (bn) "নিষ্পত্তিকৃত (ফেরত)" else "Settled (Return/Adj.)"
            CommunicationStatus.PREVIOUS_DUE -> if (bn) "পূর্বের বকেয়াসহ" else "With Previous Due"
            CommunicationStatus.CREDIT -> if (bn) "অগ্রিম ক্রেডিট" else "Advance Credit"
            CommunicationStatus.GENERAL -> if (bn) "সাধারণ" else "General"
        }
        return PdfKeyValueLine(
            label = if (bn) "পেমেন্ট স্ট্যাটাস" else "Payment Status",
            value = label,
            valueColor = textColor,
            boldValue = true
        )
    }

    private fun commercialColumns(bn: Boolean, amountTitleEn: String = "Amount", amountTitleBn: String = "মোট মূল্য"): List<PdfTableColumnSpec> {
        // Total width must equal CONTENT_WIDTH_PT (539f)
        val variantGroup = if (bn) "ভ্যারিয়েন্ট (Variant)" else "Variant"
        return listOf(
            PdfTableColumnSpec("idx", "#", null, 24f, PdfColAlign.CENTER),
            PdfTableColumnSpec("product", if (bn) "পণ্যের নাম" else "Product", if (bn) "(Product)" else null, 205f, PdfColAlign.LEFT),
            PdfTableColumnSpec("color", if (bn) "কালার" else "Color", null, 52f, PdfColAlign.CENTER, variantGroup),
            PdfTableColumnSpec("size", if (bn) "সাইজ" else "Size", null, 48f, PdfColAlign.CENTER, variantGroup),
            PdfTableColumnSpec("qty", if (bn) "পরিমাণ" else "Qty", if (bn) "(Qty)" else null, 52f, PdfColAlign.CENTER),
            PdfTableColumnSpec("unit_price", if (bn) "একক মূল্য" else "Unit Price", if (bn) "(Unit Price)" else null, 76f, PdfColAlign.RIGHT),
            PdfTableColumnSpec("amount", if (bn) amountTitleBn else amountTitleEn, if (bn) "($amountTitleEn)" else null, 82f, PdfColAlign.RIGHT)
        )
    }

    private fun mapCommercialRows(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean
    ): List<PdfTableRowSpec> {
        return document.lineItems.map { item ->
            val title = if (bn) item.titleBn.ifBlank { item.titleEn } else item.titleEn
            val skuSub = if (settings.showVariantSku && !item.sku.isNullOrBlank()) "SKU: ${item.sku}" else null
            val discBadge = BrandedDocumentFormatter.formatDiscountBadge(item.discountMinor, item.discountPercentBps)
            val qtyStr = buildString {
                append(item.quantity)
                if (item.unit.isNotBlank() && !item.unit.equals("PCS", ignoreCase = true)) {
                    append(' ').append(item.unit)
                } else if (!item.secondaryQuantityText.isNullOrBlank()) {
                    append('\n').append(item.secondaryQuantityText)
                }
            }
            PdfTableRowSpec(
                indexText = item.index.toString(),
                showThumbnailSlot = settings.showProductPhoto,
                thumbnailPath = if (settings.showProductPhoto) item.thumbnailPath else null,
                primaryTitle = title,
                skuSubtitle = skuSub,
                discountBadgeText = discBadge,
                extraCells = listOf(
                    item.color?.ifBlank { "-" } ?: "-",
                    item.size?.ifBlank { "-" } ?: "-",
                    qtyStr,
                    formatMoney(item.unitPriceMinor, item.currencyCode),
                    formatMoney(item.lineTotalMinor, item.currencyCode)
                )
            )
        }
    }

    private fun buildInvoiceSpec(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean,
        contactStrip: PdfContactStripSpec,
        generatedOnText: String
    ): BrandedPdfDocumentSpec {
        val inv = document.invoiceBreakdown
        val currency = document.brand?.currencyCode ?: "BDT"
        val dateText = document.documentDateMillis?.let {
            BrandedDocumentFormatter.formatDateTime(it, bn, includeTime = true, shortMonth = true)
        } ?: document.fields["invoice_date"].orEmpty()
        val salesTypeLabel = if ((inv?.salesType ?: "").equals("RETAIL", ignoreCase = true)) {
            if (bn) "খুচরা (Retail)" else "Retail"
        } else {
            "Wholesale"
        }

        val rightInfo = PdfInfoCardSpec(
            title = if (bn) "ইনভয়েস তথ্য (Invoice Details)" else "Invoice Details",
            icon = BrandedIconType.INVOICE_DOC,
            lines = buildList {
                add(PdfKeyValueLine(if (bn) "ইনভয়েস নং" else "Invoice No", document.number, boldValue = true))
                if (dateText.isNotEmpty()) add(PdfKeyValueLine(if (bn) "ইনভয়েস তারিখ" else "Invoice Date", dateText))
                add(PdfKeyValueLine(if (bn) "ডকুমেন্ট নং" else "Document No", document.number))
                add(PdfKeyValueLine(if (bn) "বিক্রয়ের ধরন" else "Sales Type", salesTypeLabel))
                add(statusLine(document.status, bn))
                inv?.dueDateMillis?.let { dueMs ->
                    add(
                        PdfKeyValueLine(
                            label = if (bn) "পরিশোধের শেষ তারিখ" else "Due Date",
                            value = BrandedDocumentFormatter.formatDateTime(dueMs, bn, includeTime = false, shortMonth = true),
                            valueColor = BrandedDocumentPalette.DueRedText
                        )
                    )
                }
            }
        )

        val subtotalMinor = inv?.subtotalMinor ?: 0L
        val invDiscMinor = inv?.invoiceDiscountMinor ?: 0L
        val chargesMinor = inv?.additionalChargesMinor ?: 0L
        val netMinor = inv?.netInvoiceAmountMinor ?: 0L
        val creditReturnMinor = inv?.creditReturnMinor ?: 0L
        val allocatedMinor = inv?.allocatedPaymentMinor ?: 0L
        val invDueMinor = inv?.invoiceDueMinor ?: 0L

        val discLabel = buildString {
            append(if (bn) "ডিসকাউন্ট" else "Discount")
            val pct = inv?.invoiceDiscountPercentBps
            if (pct != null && pct > 0L && invDiscMinor > 0L) {
                val pctStr = BigDecimal.valueOf(pct, 2).stripTrailingZeros().toPlainString()
                append(" (").append(pctStr).append("%)")
            }
        }

        val leftSummary = PdfSummaryCardSpec(
            title = if (bn) "ইনভয়েসের হিসাব (Invoice Summary)" else "Invoice Summary",
            icon = BrandedIconType.CALCULATOR,
            dividerBeforeLastRow = true,
            rows = buildList {
                add(PdfKeyValueLine(if (bn) "মোট বিল" else "Subtotal", formatMoney(subtotalMinor, currency, spacedTaka = true)))
                add(
                    PdfKeyValueLine(
                        label = discLabel,
                        value = if (invDiscMinor > 0L) "- ${formatMoney(invDiscMinor, currency, spacedTaka = true)}" else formatMoney(0L, currency, spacedTaka = true),
                        valueColor = if (invDiscMinor > 0L) BrandedDocumentPalette.DueRedText else BrandedDocumentPalette.TextPrimary
                    )
                )
                if (chargesMinor > 0L) {
                    add(PdfKeyValueLine(if (bn) "অতিরিক্ত চার্জ" else "Additional Charges", formatMoney(chargesMinor, currency, spacedTaka = true)))
                }
                if (invDiscMinor > 0L || chargesMinor > 0L) {
                    add(PdfKeyValueLine(if (bn) "নিট ইনভয়েস বিল" else "Net Invoice Total", formatMoney(netMinor, currency, spacedTaka = true), boldValue = true))
                }
                if (creditReturnMinor > 0L) {
                    add(
                        PdfKeyValueLine(
                            label = if (bn) "ফেরত / ক্রেডিট সমন্বয়" else "Return Credit",
                            value = "- ${formatMoney(creditReturnMinor, currency, spacedTaka = true)}",
                            valueColor = BrandedDocumentPalette.CreditBlueText
                        )
                    )
                }
                add(
                    PdfKeyValueLine(
                        label = if (bn) "পরিশোধিত" else "Paid Amount",
                        value = formatMoney(allocatedMinor, currency, spacedTaka = true),
                        valueColor = if (allocatedMinor > 0L) BrandedDocumentPalette.PaymentGreenText else BrandedDocumentPalette.TextPrimary,
                        boldValue = allocatedMinor > 0L
                    )
                )
                add(
                    PdfKeyValueLine(
                        label = if (bn) "ইনভয়েস বকেয়া" else "Invoice Due",
                        value = formatMoney(invDueMinor, currency, spacedTaka = true),
                        valueColor = if (invDueMinor > 0L) BrandedDocumentPalette.DueRedText else BrandedDocumentPalette.PaymentGreenText,
                        boldValue = true
                    )
                )
            }
        )

        val currentBal = inv?.currentAccountBalanceMinor ?: (document.balanceMinor ?: invDueMinor)
        val rightSummary = PdfSummaryCardSpec(
            title = if (bn) "গ্রাহকের সামগ্রিক হিসাব (Customer Balance)" else "Customer Balance",
            icon = BrandedIconType.COIN_TAKA,
            rows = buildList {
                if (inv == null || !inv.hasHistoricalSnapshot) {
                    add(
                        PdfKeyValueLine(
                            label = if (bn) "পূর্বের বকেয়া" else "Previous Balance",
                            value = if (bn) "অনুপলব্ধ (পুরোনো রেকর্ড)" else "Unavailable (legacy)",
                            valueColor = BrandedDocumentPalette.TextMuted
                        )
                    )
                    add(PdfKeyValueLine(if (bn) "বর্তমান ইনভয়েস বকেয়া" else "Current Invoice Due", formatMoney(invDueMinor, currency, spacedTaka = true)))
                } else if (!inv.isReshareAfterLaterActivity) {
                    val prev = inv.historicalPreviousBalanceMinor ?: 0L
                    if (prev < 0L) {
                        add(
                            PdfKeyValueLine(
                                label = if (bn) "পূর্বের অগ্রিম ক্রেডিট" else "Previous Credit",
                                value = formatMoney(-prev, currency, spacedTaka = true),
                                valueColor = BrandedDocumentPalette.CreditBlueText
                            )
                        )
                    } else {
                        add(PdfKeyValueLine(if (bn) "পূর্বের বকেয়া" else "Previous Balance", formatMoney(prev, currency, spacedTaka = true)))
                    }
                    add(PdfKeyValueLine(if (bn) "বর্তমান ইনভয়েস বকেয়া" else "Current Invoice Due", formatMoney(invDueMinor, currency, spacedTaka = true)))
                } else {
                    val histPrev = inv.historicalPreviousBalanceMinor ?: 0L
                    val histClosing = inv.historicalClosingBalanceMinor ?: 0L
                    add(
                        PdfKeyValueLine(
                            label = if (bn) "ইনভয়েসের পূর্বের জের" else "Prev. Bal (at Invoice)",
                            value = formatBalanceText(histPrev, bn, currency, spacedTaka = false)
                        )
                    )
                    add(
                        PdfKeyValueLine(
                            label = if (bn) "পোস্টিং সময়ের মোট জের" else "Closing (at Invoice)",
                            value = formatBalanceText(histClosing, bn, currency, spacedTaka = false)
                        )
                    )
                    add(PdfKeyValueLine(if (bn) "বর্তমান ইনভয়েস বকেয়া" else "Current Invoice Due", formatMoney(invDueMinor, currency, spacedTaka = true)))
                }
            },
            totalPill = balancePillSpec(currentBal, bn, currency, preferGrandTotalLabel = (inv?.isReshareAfterLaterActivity != true)),
            footnote = if (inv?.isReshareAfterLaterActivity == true) {
                (if (bn) "হালনাগাদ হিসাব: " else "Live balance as of: ") +
                    BrandedDocumentFormatter.formatDateTime(inv.asOfMillis, bn, includeTime = true, shortMonth = true)
            } else null
        )

        val notesBullets = buildList {
            add(
                when (document.status) {
                    CommunicationStatus.PAID -> if (bn) "এই ইনভয়েসের সম্পূর্ণ অর্থ পরিশোধিত হয়েছে।" else "This invoice has been paid in full."
                    CommunicationStatus.PARTIAL -> if (bn) "এই ইনভয়েসে আংশিক পেমেন্ট গ্রহণ করা হয়েছে।" else "A partial payment has been allocated to this invoice."
                    CommunicationStatus.SETTLED -> if (bn) "পণ্য ফেরত / ক্রেডিট সমন্বয়ের মাধ্যমে ইনভয়েসটি নিষ্পত্তি হয়েছে।" else "This invoice has been settled via return / credit adjustment."
                    else -> if (bn) "এই ইনভয়েসে কোনো পেমেন্ট বরাদ্দ হয়নি।" else "No payment has been allocated to this invoice."
                }
            )
            inv?.paymentTerms?.takeIf { it.isNotBlank() }?.let { terms ->
                add((if (bn) "শর্তাবলি: " else "Terms: ") + terms)
            }
            inv?.notes?.takeIf { it.isNotBlank() }?.let { note ->
                add(note)
            }
            document.warnings.distinct().forEach { add(it) }
            add(if (bn) "অনুগ্রহ করে আপনার হিসাবটি যাচাই করে নেবেন।" else "Please review and verify your account statement.")
            add(if (bn) "কোনো তথ্যের প্রয়োজন হলে আমাদের সাথে যোগাযোগ করুন।" else "Feel free to contact us if any clarification is needed.")
        }

        val qrPayload = inv?.qrVerificationPayload?.takeIf { settings.showQrCode && it.isNotBlank() }
        val bottom = PdfBottomFooterBlockSpec(
            notesTitle = if (bn) "গুরুত্বপূর্ণ তথ্য (Important Notes)" else "Important Notes",
            notesBullets = notesBullets,
            thankYouPrimary = if (bn) "আমাদের সঙ্গে ব্যবসা করার জন্য আন্তরিক ধন্যবাদ।" else "Thank you for doing business with us.",
            thankYouSecondary = if (bn) "Thank you for doing business with us." else null,
            qrPayload = qrPayload,
            qrCaptionPrimary = if (qrPayload != null) (if (bn) "ইনভয়েস যাচাই করুন" else "Verify Invoice") else null,
            qrCaptionSecondary = if (qrPayload != null) "Scan to Verify" else null,
            showAuthorizedSignature = settings.includeSignature,
            signaturePrimaryLabel = "Authorised Signature",
            signatureSecondaryLabel = document.brand?.companyName ?: "ADVANCE ERP"
        )

        return BrandedPdfDocumentSpec(
            module = CommunicationModule.INVOICE,
            safeFileStem = safeFileStem(document),
            isBangla = bn,
            headerRightTop = "বিক্রয় ইনভয়েস",
            headerRightBottom = "Sales Invoice",
            headerRightIcon = BrandedIconType.INVOICE_DOC,
            contactStrip = contactStrip,
            leftInfoCard = buildPartyInfoCard(document, bn),
            rightInfoCard = rightInfo,
            tableTitle = if (bn) "পণ্যের বিবরণ (Item Details)" else "Item Details",
            tableMode = PdfTableMode.COMMERCIAL_VARIANT_SPLIT,
            tableColumns = commercialColumns(bn),
            tableRows = mapCommercialRows(document, settings, bn),
            leftSummaryCard = leftSummary,
            rightSummaryCard = rightSummary,
            bottomBlock = bottom,
            generatedOnText = generatedOnText
        )
    }

    private fun buildQuotationOrOrderSpec(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean,
        contactStrip: PdfContactStripSpec,
        generatedOnText: String,
        isQuotation: Boolean
    ): BrandedPdfDocumentSpec {
        val inv = document.invoiceBreakdown
        val currency = document.brand?.currencyCode ?: "BDT"
        val dateText = document.documentDateMillis?.let {
            BrandedDocumentFormatter.formatDateTime(it, bn, includeTime = true, shortMonth = true)
        } ?: document.fields["document_date"].orEmpty()

        val rightTop = if (isQuotation) "কোটেশন" else "বিক্রয় আদেশ"
        val rightBottom = if (isQuotation) "Quotation" else "Sales Order"
        val rightInfo = PdfInfoCardSpec(
            title = if (isQuotation) {
                if (bn) "কোটেশন তথ্য (Quotation Details)" else "Quotation Details"
            } else {
                if (bn) "অর্ডার তথ্য (Order Details)" else "Sales Order Details"
            },
            icon = BrandedIconType.INVOICE_DOC,
            lines = buildList {
                add(PdfKeyValueLine(if (bn) "ডকুমেন্ট নং" else "Document No", document.number, boldValue = true))
                if (dateText.isNotEmpty()) add(PdfKeyValueLine(if (bn) "তারিখ" else "Date", dateText))
                document.validUntilMillis?.let { vu ->
                    add(PdfKeyValueLine(if (bn) "মেয়াদ শেষ" else "Valid Until", BrandedDocumentFormatter.formatDateTime(vu, bn, includeTime = false, shortMonth = true), valueColor = BrandedDocumentPalette.BrandPurplePrimary, boldValue = true))
                }
                document.scheduledDeliveryInfo?.takeIf { it.isNotBlank() }?.let { di ->
                    add(PdfKeyValueLine(if (bn) "ডেলিভারির তথ্য" else "Delivery Info", di))
                }
                document.fields["sales_type"]?.takeIf { it.isNotBlank() }?.let { st ->
                    add(PdfKeyValueLine(if (bn) "ধরন" else "Type", st))
                }
            }
        )

        val totalMinor = document.fields["document_amount"] ?: ""
        val subtotalStr = document.fields["subtotal_amount"].orEmpty()
        val discStr = document.fields["discount_amount"].orEmpty()
        val chargesStr = document.fields["additional_charges"].orEmpty()
        val lineSumMinor = document.lineItems.sumOf { it.lineTotalMinor }

        val leftSummary = PdfSummaryCardSpec(
            title = if (bn) "আর্থিক সারসংক্ষেপ (Estimated Total)" else "Document Summary",
            icon = BrandedIconType.CALCULATOR,
            dividerBeforeLastRow = true,
            rows = buildList {
                if (subtotalStr.isNotEmpty()) add(PdfKeyValueLine(if (bn) "উপমোট" else "Subtotal", subtotalStr))
                if (discStr.isNotEmpty()) add(PdfKeyValueLine(if (bn) "ডিসকাউন্ট" else "Discount", "- $discStr", valueColor = BrandedDocumentPalette.DueRedText))
                if (chargesStr.isNotEmpty()) add(PdfKeyValueLine(if (bn) "অতিরিক্ত চার্জ" else "Additional Charges", chargesStr))
                add(
                    PdfKeyValueLine(
                        label = if (bn) "মোট প্রাক্কলিত মূল্য" else "Total Quoted Amount",
                        value = totalMinor.ifBlank { formatMoney(lineSumMinor, currency, spacedTaka = true) },
                        valueColor = BrandedDocumentPalette.BrandPurpleDeep,
                        boldValue = true
                    )
                )
            }
        )

        val rightSummary = PdfSummaryCardSpec(
            title = if (bn) "শর্তাবলি ও ডেলিভারি (Terms & Validity)" else "Terms & Validity",
            icon = BrandedIconType.CALENDAR,
            rows = buildList {
                add(PdfKeyValueLine(if (bn) "মোট পণ্য সংখ্যা" else "Total Quantity", "${document.lineItems.sumOf { it.quantity }}"))
                document.validUntilMillis?.let { vu ->
                    add(PdfKeyValueLine(if (bn) "মেয়াদ" else "Valid Until", BrandedDocumentFormatter.formatDateTime(vu, bn, includeTime = false, shortMonth = true)))
                }
                document.scheduledDeliveryInfo?.takeIf { it.isNotBlank() }?.let { di ->
                    add(PdfKeyValueLine(if (bn) "নির্ধারিত ডেলিভারি" else "Scheduled Delivery", di))
                }
                val terms = document.fields["payment_terms"]?.takeIf { it.isNotBlank() } ?: settings.defaultPaymentTerms.takeIf { it.isNotBlank() }
                if (terms != null) {
                    add(PdfKeyValueLine(if (bn) "পেমেন্ট শর্ত" else "Payment Terms", terms))
                }
            },
            totalPill = PdfTotalPillSpec(
                icon = BrandedIconType.INVOICE_DOC,
                label = if (bn) "মোট ডকুমেন্ট মূল্য" else "Document Total",
                amountText = totalMinor.ifBlank { formatMoney(lineSumMinor, currency, spacedTaka = true) }
            )
        )

        val notesBullets = buildList {
            add(
                if (isQuotation) {
                    if (bn) "এটি একটি মূল্য প্রস্তাব (Quotation); চূড়ান্ত ইনভয়েস নয়।" else "This is an official quotation and not a tax invoice."
                } else {
                    if (bn) "এটি নিশ্চিতকৃত বিক্রয় আদেশ (Sales Order)।" else "This is a confirmed sales order awaiting dispatch/invoicing."
                }
            )
            document.warnings.distinct().forEach { add(it) }
            add(if (bn) "যেকোনো পরিবর্তনের জন্য অনুগ্রহ করে অর্ডার কনফার্মেশনের আগে জানান।" else "Please contact us for any modifications prior to confirmation.")
        }

        val kindToken = if (isQuotation) "QUO" else "SO"
        val qrPayload = if (settings.showQrCode) {
            "ADV-ERP|DOC|$kindToken|${document.number}".filter { it.code in 32..126 }.take(80)
        } else null

        val bottom = PdfBottomFooterBlockSpec(
            notesTitle = if (bn) "গুরুত্বপূর্ণ তথ্য (Important Notes)" else "Important Notes",
            notesBullets = notesBullets,
            thankYouPrimary = if (bn) "আমাদের সঙ্গে ব্যবসা করার জন্য আন্তরিক ধন্যবাদ।" else "Thank you for doing business with us.",
            thankYouSecondary = if (bn) "Thank you for doing business with us." else null,
            qrPayload = qrPayload,
            qrCaptionPrimary = if (qrPayload != null) (if (bn) "ডকুমেন্ট যাচাই করুন" else "Verify Document") else null,
            qrCaptionSecondary = if (qrPayload != null) "Scan to Verify" else null,
            showAuthorizedSignature = settings.includeSignature,
            signaturePrimaryLabel = "Authorised Signature",
            signatureSecondaryLabel = document.brand?.companyName ?: "ADVANCE ERP"
        )

        return BrandedPdfDocumentSpec(
            module = document.request.module,
            safeFileStem = safeFileStem(document),
            isBangla = bn,
            headerRightTop = if (bn) rightTop else rightBottom,
            headerRightBottom = if (bn) rightBottom else "",
            headerRightIcon = BrandedIconType.INVOICE_DOC,
            contactStrip = contactStrip,
            leftInfoCard = buildPartyInfoCard(document, bn),
            rightInfoCard = rightInfo,
            tableTitle = if (bn) "পণ্যের বিবরণ (Item Details)" else "Item Details",
            tableMode = PdfTableMode.COMMERCIAL_VARIANT_SPLIT,
            tableColumns = commercialColumns(bn),
            tableRows = mapCommercialRows(document, settings, bn),
            leftSummaryCard = leftSummary,
            rightSummaryCard = rightSummary,
            bottomBlock = bottom,
            generatedOnText = generatedOnText
        )
    }

    private fun buildPurchaseOrderSpec(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean,
        contactStrip: PdfContactStripSpec,
        generatedOnText: String
    ): BrandedPdfDocumentSpec {
        val poCurrency = document.lineItems.firstOrNull()?.currencyCode ?: "BDT"
        val dateText = document.documentDateMillis?.let {
            BrandedDocumentFormatter.formatDateTime(it, bn, includeTime = true, shortMonth = true)
        } ?: document.fields["document_date"].orEmpty()
        val rightInfo = PdfInfoCardSpec(
            title = if (bn) "ক্রয় আদেশ তথ্য (PO Details)" else "Purchase Order Details",
            icon = BrandedIconType.INVOICE_DOC,
            lines = buildList {
                add(PdfKeyValueLine(if (bn) "পিও নং" else "PO Number", document.number, boldValue = true))
                if (dateText.isNotEmpty()) add(PdfKeyValueLine(if (bn) "তারিখ" else "Order Date", dateText))
                add(PdfKeyValueLine(if (bn) "মুদ্রা" else "Currency", poCurrency))
                add(PdfKeyValueLine(if (bn) "মোট পরিমাণ" else "Total Quantity", "${document.lineItems.sumOf { it.quantity }}"))
            }
        )
        val poTotalMinor = document.lineItems.sumOf { it.lineTotalMinor }
        val leftSummary = PdfSummaryCardSpec(
            title = if (bn) "অর্ডার সারসংক্ষেপ (Order Summary)" else "Order Summary",
            icon = BrandedIconType.CALCULATOR,
            dividerBeforeLastRow = true,
            rows = listOf(
                PdfKeyValueLine(if (bn) "মোট আইটেম" else "Line Items", "${document.lineItems.size}"),
                PdfKeyValueLine(if (bn) "মোট পরিমাণ" else "Total Units", "${document.lineItems.sumOf { it.quantity }}"),
                PdfKeyValueLine(
                    label = if (bn) "মোট অর্ডার মূল্য" else "Total PO Amount",
                    value = formatMoney(poTotalMinor, poCurrency),
                    valueColor = BrandedDocumentPalette.BrandPurpleDeep,
                    boldValue = true
                )
            )
        )
        val payable = document.balanceMinor ?: 0L
        val rightSummary = PdfSummaryCardSpec(
            title = if (bn) "সরবরাহকারী হিসাব (Supplier Balance)" else "Supplier Account Summary",
            icon = BrandedIconType.BANK_BUILDING,
            rows = listOf(
                PdfKeyValueLine(if (bn) "হিসাবের ধরন" else "Ledger Currency", document.brand?.currencyCode ?: "BDT"),
                PdfKeyValueLine(if (bn) "বর্তমান প্রদেয়" else "Current Payable", formatBalanceText(payable, bn, document.brand?.currencyCode ?: "BDT"))
            ),
            totalPill = balancePillSpec(payable, bn, document.brand?.currencyCode ?: "BDT", isSupplier = true)
        )
        val qrPayload = if (settings.showQrCode) {
            "ADV-ERP|DOC|PO|${document.number}".filter { it.code in 32..126 }.take(80)
        } else null
        return BrandedPdfDocumentSpec(
            module = CommunicationModule.PURCHASE_ORDER,
            safeFileStem = safeFileStem(document),
            isBangla = bn,
            headerRightTop = if (bn) "ক্রয় আদেশ" else "Purchase Order",
            headerRightBottom = if (bn) "Purchase Order" else "",
            headerRightIcon = BrandedIconType.INVOICE_DOC,
            contactStrip = contactStrip,
            leftInfoCard = buildPartyInfoCard(document, bn),
            rightInfoCard = rightInfo,
            tableTitle = if (bn) "অর্ডারকৃত পণ্যের তালিকা (Order Items)" else "Ordered Items",
            tableMode = PdfTableMode.COMMERCIAL_VARIANT_SPLIT,
            tableColumns = commercialColumns(bn),
            tableRows = mapCommercialRows(document, settings, bn),
            leftSummaryCard = leftSummary,
            rightSummaryCard = rightSummary,
            bottomBlock = PdfBottomFooterBlockSpec(
                notesTitle = if (bn) "নির্দেশনা (Order Instructions)" else "Order Instructions",
                notesBullets = listOf(
                    if (bn) "অনুগ্রহ করে পণ্য ও পরিমাণ যাচাই করে চালান নিশ্চিত করুন।" else "Please verify item specifications, colors, sizes, and quantities prior to dispatch."
                ),
                thankYouPrimary = if (bn) "আপনার সহযোগিতার জন্য ধন্যবাদ।" else "Thank you for your continued partnership.",
                thankYouSecondary = null,
                qrPayload = qrPayload,
                qrCaptionPrimary = if (qrPayload != null) (if (bn) "অর্ডার যাচাই" else "Verify PO") else null,
                qrCaptionSecondary = if (qrPayload != null) "Scan to Verify" else null,
                showAuthorizedSignature = settings.includeSignature,
                signaturePrimaryLabel = "Authorised Signature",
                signatureSecondaryLabel = document.brand?.companyName ?: "ADVANCE ERP"
            ),
            generatedOnText = generatedOnText
        )
    }

    private fun buildPaymentReceiptSpec(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean,
        contactStrip: PdfContactStripSpec,
        generatedOnText: String
    ): BrandedPdfDocumentSpec {
        val pay = document.paymentBreakdown
        val currency = pay?.currencyCode ?: (document.brand?.currencyCode ?: "BDT")
        val amountMinor = pay?.paymentAmountMinor ?: 0L
        val payDateMs = pay?.paymentDateMillis ?: document.documentDateMillis ?: document.asOfMillis
        val isSupplier = document.request.module == CommunicationModule.SUPPLIER_PAYMENT

        val rightInfo = PdfInfoCardSpec(
            title = if (bn) "রসিদ তথ্য (Receipt Details)" else "Receipt Details",
            icon = BrandedIconType.PAYMENT_RECEIPT,
            lines = buildList {
                add(PdfKeyValueLine(if (bn) "পেমেন্ট নং" else "Receipt No", document.number, boldValue = true))
                add(PdfKeyValueLine(if (bn) "তারিখ" else "Payment Date", BrandedDocumentFormatter.formatDateTime(payDateMs, bn, includeTime = true, shortMonth = true)))
                add(PdfKeyValueLine(if (bn) "পেমেন্ট মাধ্যম" else "Payment Method", pay?.paymentMethod ?: document.fields["payment_method"].orEmpty(), boldValue = true))
                pay?.referenceNote?.takeIf { it.isNotBlank() }?.let { ref ->
                    add(PdfKeyValueLine(if (bn) "রেফারেন্স" else "Reference", ref))
                }
                add(
                    PdfKeyValueLine(
                        label = if (bn) "স্ট্যাটাস" else "Status",
                        value = if (bn) "পেমেন্ট গ্রহণ সম্পন্ন" else "Payment Confirmed",
                        valueColor = BrandedDocumentPalette.PaymentGreenText,
                        boldValue = true
                    )
                )
            }
        )

        val hero = PdfHeroBannerSpec(
            label = if (isSupplier) {
                if (bn) "পরিশোধিত পেমেন্ট (Amount Paid)" else "Amount Paid to Supplier"
            } else {
                if (bn) "প্রাপ্ত পেমেন্ট (Amount Received)" else "Amount Received"
            },
            primaryValue = formatMoney(amountMinor, currency, spacedTaka = true),
            subtitle = if (bn) "পদ্ধতি: ${pay?.paymentMethod ?: "-"}" else "Via ${pay?.paymentMethod ?: "-"}",
            icon = BrandedIconType.CHECK_CIRCLE,
            bgColor = BrandedDocumentPalette.PaymentGreenBg,
            borderColor = BrandedDocumentPalette.PaymentGreenBorder,
            valueColor = BrandedDocumentPalette.PaymentGreenText
        )

        val allocations = pay?.allocations.orEmpty()
        val cols = listOf(
            PdfTableColumnSpec("idx", "#", null, 42f, PdfColAlign.CENTER),
            PdfTableColumnSpec("inv", if (bn) "ইনভয়েস নম্বর (Applied Invoice)" else "Applied to Invoice", null, 337f, PdfColAlign.LEFT),
            PdfTableColumnSpec("amt", if (bn) "বরাদ্দকৃত পরিমাণ (Allocated Amount)" else "Allocated Amount", null, 160f, PdfColAlign.RIGHT)
        )
        val tableRows = allocations.mapIndexed { idx, alloc ->
            PdfTableRowSpec(
                indexText = (idx + 1).toString(),
                showThumbnailSlot = false,
                thumbnailPath = null,
                primaryTitle = alloc.invoiceNumber,
                skuSubtitle = null,
                discountBadgeText = null,
                extraCells = listOf(formatMoney(alloc.amountMinor, currency, spacedTaka = true)),
                highlightAmountColor = BrandedDocumentPalette.PaymentGreenText
            )
        }

        val leftSummary = PdfSummaryCardSpec(
            title = if (bn) "পেমেন্টের বিবরণ (Payment Breakdown)" else "Payment Breakdown",
            icon = BrandedIconType.PAYMENT_CARD,
            dividerBeforeLastRow = true,
            rows = buildList {
                add(PdfKeyValueLine(if (bn) "মোট পেমেন্ট" else "Total Payment", formatMoney(amountMinor, currency, spacedTaka = true), valueColor = BrandedDocumentPalette.PaymentGreenText, boldValue = true))
                if (allocations.isNotEmpty()) {
                    add(PdfKeyValueLine(if (bn) "ইনভয়েসে বরাদ্দ" else "Allocated to Invoices", formatMoney(pay?.allocatedToInvoicesMinor ?: 0L, currency, spacedTaka = true)))
                }
                if ((pay?.unallocatedAdvanceMinor ?: 0L) > 0L) {
                    add(PdfKeyValueLine(if (bn) "অগ্রিম ক্রেডিট / জমা" else "Unallocated Advance", formatMoney(pay!!.unallocatedAdvanceMinor, currency, spacedTaka = true), valueColor = BrandedDocumentPalette.CreditBlueText))
                }
                add(PdfKeyValueLine(if (bn) "পেমেন্ট মাধ্যম" else "Method", pay?.paymentMethod ?: "-"))
            }
        )

        val curBal = pay?.currentAccountBalanceMinor ?: (document.balanceMinor ?: 0L)
        val balCurrency = document.brand?.currencyCode ?: currency
        val rightSummary = PdfSummaryCardSpec(
            title = if (bn) "হালনাগাদ হিসাব (Updated Account Balance)" else "Updated Account Balance",
            icon = BrandedIconType.BALANCE_SCALE,
            rows = buildList {
                if (pay?.previousBalanceMinor != null) {
                    add(PdfKeyValueLine(if (bn) "পেমেন্ট পূর্বের ব্যালেন্স" else "Previous Balance", formatBalanceText(pay.previousBalanceMinor, bn, balCurrency)))
                }
                add(
                    PdfKeyValueLine(
                        label = if (bn) "পরিশোধিত" else "Paid",
                        value = "- ${formatMoney(amountMinor, balCurrency, spacedTaka = true)}",
                        valueColor = BrandedDocumentPalette.PaymentGreenText,
                        boldValue = true
                    )
                )
                if (pay?.hasLaterActivityAfterPayment == true && pay.balanceAfterPaymentMinor != null) {
                    add(PdfKeyValueLine(if (bn) "রসিদ সময়ের ব্যালেন্স" else "Balance Right After Payment", formatBalanceText(pay.balanceAfterPaymentMinor, bn, balCurrency)))
                }
            },
            totalPill = balancePillSpec(curBal, bn, balCurrency, isSupplier = isSupplier)
        )

        val qrPayload = if (settings.showQrCode) {
            "ADV-ERP|DOC|PAY|${document.number}|${BigDecimal.valueOf(amountMinor, 2).toPlainString()}".filter { it.code in 32..126 }.take(80)
        } else null

        return BrandedPdfDocumentSpec(
            module = document.request.module,
            safeFileStem = safeFileStem(document),
            isBangla = bn,
            headerRightTop = if (isSupplier) (if (bn) "সরবরাহকারী পেমেন্ট" else "Supplier Payment") else "পেমেন্ট গ্রহণের রসিদ",
            headerRightBottom = if (isSupplier) (if (bn) "Supplier Payment" else "") else "Payment Receipt",
            headerRightIcon = BrandedIconType.PAYMENT_RECEIPT,
            contactStrip = contactStrip,
            leftInfoCard = buildPartyInfoCard(document, bn),
            rightInfoCard = rightInfo,
            heroBanner = hero,
            tableTitle = if (allocations.isNotEmpty()) (if (bn) "ইনভয়েস অনুযায়ী বরাদ্দ (Invoice Allocations)" else "Invoice Allocations") else null,
            tableMode = if (allocations.isNotEmpty()) PdfTableMode.PAYMENT_ALLOCATIONS else null,
            tableColumns = if (allocations.isNotEmpty()) cols else emptyList(),
            tableRows = tableRows,
            emptyTableMessage = null,
            leftSummaryCard = leftSummary,
            rightSummaryCard = rightSummary,
            bottomBlock = PdfBottomFooterBlockSpec(
                notesTitle = if (bn) "গুরুত্বপূর্ণ তথ্য (Receipt Notes)" else "Receipt Notes",
                notesBullets = listOf(
                    if (bn) "আপনার হিসাব সফলভাবে হালনাগাদ করা হয়েছে।" else "Your account balance has been updated accordingly.",
                    if (bn) "ভবিষ্যতের প্রয়োজনে এই রসিদটি সংরক্ষণ করুন।" else "Please retain this money receipt for your records."
                ),
                thankYouPrimary = if (bn) "আমাদের সঙ্গে ব্যবসা করার জন্য আন্তরিক ধন্যবাদ।" else "Thank you for doing business with us.",
                thankYouSecondary = if (bn) "Thank you for doing business with us." else null,
                qrPayload = qrPayload,
                qrCaptionPrimary = if (qrPayload != null) (if (bn) "রসিদ যাচাই করুন" else "Verify Receipt") else null,
                qrCaptionSecondary = if (qrPayload != null) "Scan to Verify" else null,
                showAuthorizedSignature = settings.includeSignature,
                signaturePrimaryLabel = "Authorised Signature",
                signatureSecondaryLabel = document.brand?.companyName ?: "ADVANCE ERP"
            ),
            generatedOnText = generatedOnText
        )
    }

    private fun buildStatementSpec(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean,
        contactStrip: PdfContactStripSpec,
        generatedOnText: String
    ): BrandedPdfDocumentSpec {
        val stmt = document.statementBreakdown
        val currency = stmt?.currencyCode ?: (document.brand?.currencyCode ?: "BDT")
        val isSupplier = document.request.module == CommunicationModule.SUPPLIER_STATEMENT
        val periodStr = if (stmt != null) {
            BrandedDocumentFormatter.formatDateRange(stmt.fromMillis, stmt.toMillis, stmt.isAllTime, bn)
        } else {
            document.fields["date_range"].orEmpty()
        }

        val rightInfo = PdfInfoCardSpec(
            title = if (bn) "বিবরণীর তথ্য (Statement Details)" else "Statement Details",
            icon = BrandedIconType.LEDGER_BOOK,
            lines = buildList {
                add(PdfKeyValueLine(if (bn) "হিসাবের সময়কাল" else "Statement Period", periodStr, boldValue = true))
                add(PdfKeyValueLine(if (bn) "মোট লেনদেন" else "Transactions", "${stmt?.transactions?.size ?: document.rows.size}"))
                add(
                    PdfKeyValueLine(
                        label = if (bn) "সমাপনী অবস্থা" else "Closing Status",
                        value = formatBalanceText(stmt?.closingBalanceMinor ?: (document.balanceMinor ?: 0L), bn, currency),
                        valueColor = BrandedDocumentPalette.textColorForTone(
                            BrandedDocumentPalette.resolveBalanceTone(stmt?.closingBalanceMinor ?: (document.balanceMinor ?: 0L))
                        ),
                        boldValue = true
                    )
                )
            }
        )

        val closing = stmt?.closingBalanceMinor ?: (document.balanceMinor ?: 0L)
        val opening = stmt?.openingBalanceMinor ?: 0L
        val kpis = buildList {
            add(PdfKpiBoxSpec(if (bn) "প্রারম্ভিক জের" else "Opening", formatBalanceText(opening, bn, currency, spacedTaka = false)))
            add(
                PdfKpiBoxSpec(
                    if (isSupplier) (if (bn) "মোট ক্রয়" else "Purchases") else (if (bn) "মোট ক্রয় (Sales)" else "Total Sales"),
                    formatMoney(stmt?.totalSalesOrPurchasesMinor ?: 0L, currency)
                )
            )
            if (!isSupplier) {
                add(PdfKpiBoxSpec(if (bn) "মোট ফেরত" else "Returns", formatMoney(stmt?.totalReturnMinor ?: 0L, currency)))
            }
            add(
                PdfKpiBoxSpec(
                    if (bn) "মোট পরিশোধ" else "Payments",
                    formatMoney(stmt?.totalPaymentMinor ?: 0L, currency),
                    valueColor = BrandedDocumentPalette.PaymentGreenText
                )
            )
            add(
                PdfKpiBoxSpec(
                    if (bn) "বর্তমান ব্যালেন্স" else "Closing Bal.",
                    formatBalanceText(closing, bn, currency, spacedTaka = false),
                    highlightedPurple = true
                )
            )
        }

        val cols = listOf(
            PdfTableColumnSpec("idx", "#", null, 22f, PdfColAlign.CENTER),
            PdfTableColumnSpec("date", if (bn) "তারিখ" else "Date", null, 72f, PdfColAlign.LEFT),
            PdfTableColumnSpec("ref", if (bn) "রেফারেন্স" else "Reference", null, 88f, PdfColAlign.LEFT),
            PdfTableColumnSpec("desc", if (bn) "বিবরণ / ধরন" else "Particulars", null, 117f, PdfColAlign.LEFT),
            PdfTableColumnSpec("debit", if (bn) "ডেবিট (+)" else "Debit", null, 74f, PdfColAlign.RIGHT),
            PdfTableColumnSpec("credit", if (bn) "ক্রেডিট (-)" else "Credit", null, 74f, PdfColAlign.RIGHT),
            PdfTableColumnSpec("bal", if (bn) "ব্যালেন্স" else "Balance", null, 92f, PdfColAlign.RIGHT)
        )

        val txs = stmt?.transactions.orEmpty()
        val tableRows = txs.mapIndexed { idx, tx ->
            PdfTableRowSpec(
                indexText = (idx + 1).toString(),
                showThumbnailSlot = false,
                thumbnailPath = null,
                primaryTitle = BrandedDocumentFormatter.formatDateTime(tx.dateMillis, bn, includeTime = false, shortMonth = true),
                skuSubtitle = null,
                discountBadgeText = null,
                extraCells = listOf(
                    tx.referenceNumber,
                    tx.description.ifBlank { tx.transactionType },
                    if (tx.debitMinor > 0L) formatMoney(tx.debitMinor, currency) else "-",
                    if (tx.creditMinor > 0L) formatMoney(tx.creditMinor, currency) else "-",
                    formatBalanceText(tx.runningBalanceMinor, bn, currency, spacedTaka = false)
                ),
                highlightAmountColor = BrandedDocumentPalette.textColorForTone(BrandedDocumentPalette.resolveBalanceTone(tx.runningBalanceMinor))
            )
        }

        val leftSummary = PdfSummaryCardSpec(
            title = if (bn) "সময়কালের সারসংক্ষেপ (Period Totals)" else "Period Totals",
            icon = BrandedIconType.CALCULATOR,
            rows = buildList {
                add(PdfKeyValueLine(if (bn) "প্রারম্ভিক ব্যালেন্স" else "Opening Balance", formatBalanceText(opening, bn, currency)))
                add(PdfKeyValueLine(if (isSupplier) (if (bn) "মোট ক্রয়" else "Total Purchases") else (if (bn) "মোট ক্রয় (Sales)" else "Total Sales"), formatMoney(stmt?.totalSalesOrPurchasesMinor ?: 0L, currency, spacedTaka = true)))
                if (!isSupplier && (stmt?.totalReturnMinor ?: 0L) > 0L) {
                    add(PdfKeyValueLine(if (bn) "মোট ফেরত সমন্বয়" else "Total Returns", "- ${formatMoney(stmt!!.totalReturnMinor, currency, spacedTaka = true)}"))
                }
                add(PdfKeyValueLine(if (bn) "মোট পরিশোধ" else "Total Payments", formatMoney(stmt?.totalPaymentMinor ?: 0L, currency, spacedTaka = true), valueColor = BrandedDocumentPalette.PaymentGreenText))
            }
        )

        val rightSummary = PdfSummaryCardSpec(
            title = if (bn) "সমাপনী হিসাব (Closing Balance)" else "Closing Position",
            icon = BrandedIconType.BALANCE_SCALE,
            rows = listOf(
                PdfKeyValueLine(if (bn) "সময়কাল" else "Period", periodStr),
                PdfKeyValueLine(if (bn) "হিসাব নং" else "Account Ref", document.number)
            ),
            totalPill = balancePillSpec(closing, bn, currency, isSupplier = isSupplier)
        )

        val qrPayload = if (settings.showQrCode) {
            "ADV-ERP|DOC|STMT|${document.partyId.take(18)}|${BigDecimal.valueOf(closing, 2).toPlainString()}".filter { it.code in 32..126 }.take(80)
        } else null

        return BrandedPdfDocumentSpec(
            module = document.request.module,
            safeFileStem = safeFileStem(document),
            isBangla = bn,
            headerRightTop = if (isSupplier) (if (bn) "সরবরাহকারী স্টেটমেন্ট" else "Supplier Statement") else "গ্রাহক হিসাব বিবরণী",
            headerRightBottom = if (isSupplier) (if (bn) "Supplier Statement" else "") else "Customer Ledger Statement",
            headerRightIcon = BrandedIconType.LEDGER_BOOK,
            contactStrip = contactStrip,
            leftInfoCard = buildPartyInfoCard(document, bn),
            rightInfoCard = rightInfo,
            kpiStrip = kpis,
            tableTitle = if (bn) "লেনদেনের বিবরণ (Transaction History)" else "Transaction History",
            tableMode = PdfTableMode.STATEMENT_LEDGER,
            tableColumns = cols,
            tableRows = tableRows,
            emptyTableMessage = if (tableRows.isEmpty()) (if (bn) "নির্বাচিত সময়ে কোনো লেনদেন পাওয়া যায়নি।" else "No transactions recorded in the selected period.") else null,
            leftSummaryCard = leftSummary,
            rightSummaryCard = rightSummary,
            bottomBlock = PdfBottomFooterBlockSpec(
                notesTitle = if (bn) "গুরুত্বপূর্ণ তথ্য (Statement Notes)" else "Statement Notes",
                notesBullets = listOf(
                    if (bn) "উপরোক্ত হিসাব বিবরণীতে কোনো অসঙ্গতি থাকলে অনুগ্রহ করে আমাদের জানান।" else "Please report any discrepancy in this statement within 7 days.",
                    if (bn) "সকল লেনদেন সিস্টেম লেজার অনুযায়ী স্বয়ংক্রিয়ভাবে যাচাইকৃত।" else "All balances above are directly reconciled with the official ledger."
                ),
                thankYouPrimary = if (bn) "আমাদের সঙ্গে ব্যবসা করার জন্য আন্তরিক ধন্যবাদ।" else "Thank you for doing business with us.",
                thankYouSecondary = if (bn) "Thank you for doing business with us." else null,
                qrPayload = qrPayload,
                qrCaptionPrimary = if (qrPayload != null) (if (bn) "স্টেটমেন্ট যাচাই" else "Verify Statement") else null,
                qrCaptionSecondary = if (qrPayload != null) "Scan to Verify" else null,
                showAuthorizedSignature = settings.includeSignature,
                signaturePrimaryLabel = "Authorised Signature",
                signatureSecondaryLabel = document.brand?.companyName ?: "ADVANCE ERP"
            ),
            generatedOnText = generatedOnText
        )
    }

    private fun buildDueReminderSpec(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean,
        contactStrip: PdfContactStripSpec,
        generatedOnText: String
    ): BrandedPdfDocumentSpec {
        val rem = document.reminderBreakdown
        val currency = document.brand?.currencyCode ?: "BDT"
        val currentDue = rem?.currentTotalDueMinor ?: (document.balanceMinor ?: 0L)
        val tone = BrandedDocumentPalette.resolveBalanceTone(currentDue)

        val hero = PdfHeroBannerSpec(
            label = when {
                currentDue > 0L -> if (bn) "বর্তমান মোট বকেয়া (Current Total Due)" else "Current Outstanding Due"
                currentDue < 0L -> if (bn) "বর্তমান অগ্রিম ক্রেডিট (Advance Credit)" else "Current Advance Credit"
                else -> if (bn) "হিসাব সম্পূর্ণ পরিশোধিত (Settled)" else "Account Fully Settled"
            },
            primaryValue = formatMoney(kotlin.math.abs(currentDue), currency, spacedTaka = true),
            subtitle = if (currentDue > 0L && rem?.earliestDueDateMillis != null) {
                (if (bn) "পরিশোধের তারিখ: " else "Earliest due date: ") +
                    BrandedDocumentFormatter.formatDateTime(rem.earliestDueDateMillis, bn, includeTime = false, shortMonth = true)
            } else {
                (if (bn) "হালনাগাদ: " else "As of: ") +
                    BrandedDocumentFormatter.formatDateTime(rem?.asOfMillis ?: document.asOfMillis, bn, includeTime = true, shortMonth = true)
            },
            icon = if (currentDue > 0L) BrandedIconType.MONEY_BAG else BrandedIconType.CHECK_CIRCLE,
            bgColor = when (tone) {
                BrandedDocumentPalette.BalanceSemanticTone.DUE_RED -> BrandedDocumentPalette.DueHeroBoxBg
                BrandedDocumentPalette.BalanceSemanticTone.CREDIT_BLUE -> BrandedDocumentPalette.CreditBlueBg
                BrandedDocumentPalette.BalanceSemanticTone.SETTLED_GREEN -> BrandedDocumentPalette.PaymentGreenBg
            },
            borderColor = when (tone) {
                BrandedDocumentPalette.BalanceSemanticTone.DUE_RED -> BrandedDocumentPalette.DueRedBorder
                BrandedDocumentPalette.BalanceSemanticTone.CREDIT_BLUE -> BrandedDocumentPalette.CreditBlueBorder
                BrandedDocumentPalette.BalanceSemanticTone.SETTLED_GREEN -> BrandedDocumentPalette.PaymentGreenBorder
            },
            valueColor = BrandedDocumentPalette.textColorForTone(tone)
        )

        val rightInfo = PdfInfoCardSpec(
            title = if (bn) "বকেয়া ও সময় তথ্য (Account Status)" else "Account Status",
            icon = BrandedIconType.BELL_REMINDER,
            lines = buildList {
                add(PdfKeyValueLine(if (bn) "তারিখ" else "Statement As Of", BrandedDocumentFormatter.formatDateTime(rem?.asOfMillis ?: document.asOfMillis, bn, includeTime = true, shortMonth = true)))
                if (rem != null && rem.unpaidInvoiceCount > 0) {
                    add(PdfKeyValueLine(if (bn) "অপরিশোধিত ইনভয়েস" else "Open Invoices", "${rem.unpaidInvoiceCount}", valueColor = BrandedDocumentPalette.DueRedText, boldValue = true))
                }
                rem?.earliestDueDateMillis?.let { due ->
                    add(PdfKeyValueLine(if (bn) "পরিশোধের নির্ধারিত তারিখ" else "Due Date", BrandedDocumentFormatter.formatDateTime(due, bn, includeTime = false, shortMonth = true), valueColor = BrandedDocumentPalette.DueRedText))
                }
                add(
                    PdfKeyValueLine(
                        label = if (bn) "অবস্থা" else "Status",
                        value = when {
                            currentDue > 0L -> if (bn) "বকেয়া আছে" else "Payment Due"
                            currentDue < 0L -> if (bn) "অগ্রিম জমা" else "Advance Credit"
                            else -> if (bn) "পরিশোধিত" else "Settled"
                        },
                        valueColor = BrandedDocumentPalette.textColorForTone(tone),
                        boldValue = true
                    )
                )
            }
        )

        val leftSummary = PdfSummaryCardSpec(
            title = if (bn) "বকেয়ার সারসংক্ষেপ (Recent Reconciliation)" else "Balance Reconciliation",
            icon = BrandedIconType.CALENDAR,
            rows = buildList {
                if (rem != null) {
                    add(PdfKeyValueLine(if (bn) "পূর্বের ব্যালেন্স" else "Prior Balance", formatBalanceText(rem.priorBalanceMinor, bn, currency)))
                    if (rem.latestInvoiceNumber != null && rem.latestInvoiceAmountMinor != null) {
                        add(PdfKeyValueLine(if (bn) "সর্বশেষ ক্রয় (${rem.latestInvoiceNumber})" else "Latest Invoice (${rem.latestInvoiceNumber})", formatMoney(rem.latestInvoiceAmountMinor, currency, spacedTaka = true)))
                    }
                    if (rem.latestPaymentNumber != null && rem.latestPaymentAmountMinor != null) {
                        add(
                            PdfKeyValueLine(
                                label = if (bn) "সর্বশেষ পরিশোধ (${rem.latestPaymentNumber})" else "Latest Payment (${rem.latestPaymentNumber})",
                                value = "- ${formatMoney(rem.latestPaymentAmountMinor, currency, spacedTaka = true)}",
                                valueColor = BrandedDocumentPalette.PaymentGreenText
                            )
                        )
                    }
                    if (rem.otherNetAdjustmentMinor != 0L) {
                        add(PdfKeyValueLine(if (bn) "অন্যান্য সমন্বয় / ফেরত" else "Other Adjustments", formatBalanceText(rem.otherNetAdjustmentMinor, bn, currency)))
                    }
                }
            }
        )

        val rightSummary = PdfSummaryCardSpec(
            title = if (bn) "সর্বমোট হিসাব (Current Summary)" else "Current Balance Summary",
            icon = BrandedIconType.BALANCE_SCALE,
            rows = listOf(
                PdfKeyValueLine(
                    label = if (bn) "বর্তমান ব্যালেন্স" else "Net Balance",
                    value = formatBalanceText(currentDue, bn, currency),
                    valueColor = BrandedDocumentPalette.textColorForTone(tone),
                    boldValue = true
                )
            ),
            totalPill = balancePillSpec(currentDue, bn, currency)
        )

        val notesBullets = buildList {
            if (currentDue > 0L) {
                add(if (bn) "আপনার হিসাবে বকেয়া রয়েছে। অনুগ্রহ করে সুবিধামতো সময়ে বকেয়া পরিশোধ করে হিসাবটি হালনাগাদ করুন।" else "Please arrange payment at your earliest convenience to keep your account up to date.")
            } else if (currentDue < 0L) {
                add(if (bn) "আপনার হিসাবে অগ্রিম ক্রেডিট জমা রয়েছে; কোনো বকেয়া নেই।" else "Your account holds an advance credit balance with zero overdue balance.")
            } else {
                add(if (bn) "আপনার হিসাবে বর্তমানে কোনো বকেয়া নেই।" else "Your account currently has zero outstanding balance.")
            }
            if (settings.includeInstructions && settings.paymentInstructions.isNotBlank()) {
                add(settings.paymentInstructions)
            }
            if (settings.includeInstructions && settings.bankDetails.isNotBlank()) {
                add(settings.bankDetails)
            }
        }

        val qrPayload = if (settings.showQrCode) {
            "ADV-ERP|DOC|REM|${document.partyId.take(18)}|${BigDecimal.valueOf(currentDue, 2).toPlainString()}".filter { it.code in 32..126 }.take(80)
        } else null

        return BrandedPdfDocumentSpec(
            module = CommunicationModule.REMINDER,
            safeFileStem = safeFileStem(document),
            isBangla = bn,
            headerRightTop = "বকেয়া স্মরণিকা",
            headerRightBottom = "Payment Reminder",
            headerRightIcon = BrandedIconType.BELL_REMINDER,
            contactStrip = contactStrip,
            leftInfoCard = buildPartyInfoCard(document, bn),
            rightInfoCard = rightInfo,
            heroBanner = hero,
            tableTitle = null,
            tableMode = null,
            leftSummaryCard = leftSummary,
            rightSummaryCard = rightSummary,
            bottomBlock = PdfBottomFooterBlockSpec(
                notesTitle = if (bn) "গুরুত্বপূর্ণ তথ্য (Payment Instructions)" else "Important Information",
                notesBullets = notesBullets,
                thankYouPrimary = if (bn) "আমাদের সঙ্গে ব্যবসা করার জন্য আন্তরিক ধন্যবাদ।" else "Thank you for doing business with us.",
                thankYouSecondary = if (bn) "Thank you for doing business with us." else null,
                qrPayload = qrPayload,
                qrCaptionPrimary = if (qrPayload != null) (if (bn) "হিসাব যাচাই করুন" else "Verify Balance") else null,
                qrCaptionSecondary = if (qrPayload != null) "Scan to Verify" else null,
                showAuthorizedSignature = settings.includeSignature,
                signaturePrimaryLabel = "Authorised Signature",
                signatureSecondaryLabel = document.brand?.companyName ?: "ADVANCE ERP"
            ),
            generatedOnText = generatedOnText
        )
    }

    private fun buildReturnSpec(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean,
        contactStrip: PdfContactStripSpec,
        generatedOnText: String
    ): BrandedPdfDocumentSpec {
        val ret = document.returnExchangeBreakdown
        val currency = document.brand?.currencyCode ?: "BDT"
        val dateText = document.documentDateMillis?.let {
            BrandedDocumentFormatter.formatDateTime(it, bn, includeTime = true, shortMonth = true)
        } ?: document.fields["document_date"].orEmpty()
        val rightInfo = PdfInfoCardSpec(
            title = if (bn) "ফেরত তথ্য (Return Details)" else "Sales Return Details",
            icon = BrandedIconType.RETURN_ARROW,
            lines = buildList {
                add(PdfKeyValueLine(if (bn) "রিটার্ন নং" else "Return No", document.number, boldValue = true))
                if (dateText.isNotEmpty()) add(PdfKeyValueLine(if (bn) "তারিখ" else "Return Date", dateText))
                ret?.referenceInvoiceNumber?.takeIf { it.isNotBlank() }?.let { invNo ->
                    add(PdfKeyValueLine(if (bn) "মূল ইনভয়েস" else "Reference Invoice", invNo))
                }
                ret?.refundOrPaymentMethod?.takeIf { it.isNotBlank() }?.let { method ->
                    add(PdfKeyValueLine(if (bn) "সমন্বয় পদ্ধতি" else "Refund Mode", method))
                }
            }
        )
        val leftSummary = PdfSummaryCardSpec(
            title = if (bn) "ফেরতের হিসাব (Return Financial Summary)" else "Return Financial Summary",
            icon = BrandedIconType.CALCULATOR,
            dividerBeforeLastRow = true,
            rows = listOf(
                PdfKeyValueLine(if (bn) "মোট ফেরত মূল্য" else "Total Return Value", formatMoney(ret?.returnAmountMinor ?: 0L, currency, spacedTaka = true), boldValue = true),
                PdfKeyValueLine(if (bn) "নগদ / ব্যাংক ফেরত" else "Cash / Bank Refund", formatMoney(ret?.cashOrBankRefundMinor ?: 0L, currency, spacedTaka = true)),
                PdfKeyValueLine(if (bn) "হিসাবে ক্রেডিট সমন্বয়" else "Credited to Account", formatMoney(ret?.creditedToAccountMinor ?: 0L, currency, spacedTaka = true), valueColor = BrandedDocumentPalette.CreditBlueText, boldValue = true)
            )
        )
        val bal = ret?.currentAccountBalanceMinor ?: (document.balanceMinor ?: 0L)
        val rightSummary = PdfSummaryCardSpec(
            title = if (bn) "গ্রাহকের বর্তমান হিসাব (Account Balance)" else "Customer Balance",
            icon = BrandedIconType.COIN_TAKA,
            rows = listOf(
                PdfKeyValueLine(if (bn) "বর্তমান ব্যালেন্স" else "Updated Balance", formatBalanceText(bal, bn, currency))
            ),
            totalPill = balancePillSpec(bal, bn, currency)
        )
        val qrPayload = if (settings.showQrCode) {
            "ADV-ERP|DOC|RET|${document.number}".filter { it.code in 32..126 }.take(80)
        } else null
        return BrandedPdfDocumentSpec(
            module = CommunicationModule.RETURN,
            safeFileStem = safeFileStem(document),
            isBangla = bn,
            headerRightTop = if (bn) "বিক্রয় ফেরত" else "Sales Return",
            headerRightBottom = if (bn) "Sales Return" else "",
            headerRightIcon = BrandedIconType.RETURN_ARROW,
            contactStrip = contactStrip,
            leftInfoCard = buildPartyInfoCard(document, bn),
            rightInfoCard = rightInfo,
            tableTitle = if (bn) "ফেরতকৃত পণ্যের বিবরণ (Returned Items)" else "Returned Items",
            tableMode = PdfTableMode.COMMERCIAL_VARIANT_SPLIT,
            tableColumns = commercialColumns(bn, amountTitleEn = "Return Value", amountTitleBn = "ফেরত মূল্য"),
            tableRows = mapCommercialRows(document, settings, bn),
            leftSummaryCard = leftSummary,
            rightSummaryCard = rightSummary,
            bottomBlock = PdfBottomFooterBlockSpec(
                notesTitle = if (bn) "গুরুত্বপূর্ণ তথ্য (Return Notes)" else "Return Notes",
                notesBullets = listOf(
                    if (bn) "ফেরতকৃত পণ্য স্টকে ও গ্রাহকের হিসাবে সমন্বয় করা হয়েছে।" else "Returned items have been processed and adjusted in your account."
                ),
                thankYouPrimary = if (bn) "আমাদের সঙ্গে ব্যবসা করার জন্য আন্তরিক ধন্যবাদ।" else "Thank you for doing business with us.",
                thankYouSecondary = if (bn) "Thank you for doing business with us." else null,
                qrPayload = qrPayload,
                qrCaptionPrimary = if (qrPayload != null) (if (bn) "নথি যাচাই করুন" else "Verify Return") else null,
                qrCaptionSecondary = if (qrPayload != null) "Scan to Verify" else null,
                showAuthorizedSignature = settings.includeSignature,
                signaturePrimaryLabel = "Authorised Signature",
                signatureSecondaryLabel = document.brand?.companyName ?: "ADVANCE ERP"
            ),
            generatedOnText = generatedOnText
        )
    }

    private fun buildExchangeSpec(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean,
        contactStrip: PdfContactStripSpec,
        generatedOnText: String
    ): BrandedPdfDocumentSpec {
        val ex = document.returnExchangeBreakdown
        val currency = document.brand?.currencyCode ?: "BDT"
        val dateText = document.documentDateMillis?.let {
            BrandedDocumentFormatter.formatDateTime(it, bn, includeTime = true, shortMonth = true)
        } ?: document.fields["document_date"].orEmpty()
        val rightInfo = PdfInfoCardSpec(
            title = if (bn) "পণ্য বিনিময় তথ্য (Exchange Details)" else "Product Exchange Details",
            icon = BrandedIconType.EXCHANGE_ARROWS,
            lines = buildList {
                add(PdfKeyValueLine(if (bn) "এক্সচেঞ্জ নং" else "Exchange No", document.number, boldValue = true))
                if (dateText.isNotEmpty()) add(PdfKeyValueLine(if (bn) "তারিখ" else "Date", dateText))
                ex?.referenceInvoiceNumber?.takeIf { it.isNotBlank() }?.let { invNo ->
                    add(PdfKeyValueLine(if (bn) "মূল ইনভয়েস" else "Original Invoice", invNo))
                }
                ex?.refundOrPaymentMethod?.takeIf { it.isNotBlank() }?.let { m ->
                    add(PdfKeyValueLine(if (bn) "সমন্বয় মাধ্যম" else "Settlement Method", m))
                }
            }
        )
        val cols = listOf(
            PdfTableColumnSpec("idx", "#", null, 24f, PdfColAlign.CENTER),
            PdfTableColumnSpec("dir", if (bn) "ধরন" else "Type", null, 72f, PdfColAlign.CENTER),
            PdfTableColumnSpec("prod", if (bn) "পণ্যের নাম" else "Product", null, 185f, PdfColAlign.LEFT),
            PdfTableColumnSpec("color", if (bn) "কালার" else "Color", null, 48f, PdfColAlign.CENTER),
            PdfTableColumnSpec("size", if (bn) "সাইজ" else "Size", null, 46f, PdfColAlign.CENTER),
            PdfTableColumnSpec("qty", if (bn) "পরিমাণ" else "Qty", null, 46f, PdfColAlign.CENTER),
            PdfTableColumnSpec("amt", if (bn) "মূল্য" else "Amount", null, 118f, PdfColAlign.RIGHT)
        )
        val tableRows = document.lineItems.map { item ->
            val dirRaw = item.secondaryQuantityText.orEmpty()
            val isReturned = dirRaw.contains("RETURN", ignoreCase = true) || dirRaw.contains("ফেরত")
            val dirLabel = if (isReturned) {
                if (bn) "ফেরত (IN)" else "Returned"
            } else {
                if (bn) "বদলি (OUT)" else "Replaced"
            }
            PdfTableRowSpec(
                indexText = item.index.toString(),
                showThumbnailSlot = settings.showProductPhoto && item.productId.isNotBlank(),
                thumbnailPath = if (settings.showProductPhoto) item.thumbnailPath else null,
                primaryTitle = if (bn) item.titleBn.ifBlank { item.titleEn } else item.titleEn,
                skuSubtitle = if (settings.showVariantSku && !item.sku.isNullOrBlank()) "SKU: ${item.sku}" else null,
                discountBadgeText = null,
                extraCells = listOf(
                    dirLabel,
                    item.color?.ifBlank { "-" } ?: "-",
                    item.size?.ifBlank { "-" } ?: "-",
                    "${item.quantity}",
                    if (item.lineTotalMinor > 0L) formatMoney(item.lineTotalMinor, currency) else "-"
                ),
                badgeTagText = dirLabel,
                badgeTagColor = if (isReturned) BrandedDocumentPalette.DueRedText else BrandedDocumentPalette.PaymentGreenText
            )
        }
        val diff = ex?.netPriceDifferenceMinor ?: 0L
        val leftSummary = PdfSummaryCardSpec(
            title = if (bn) "বিনিময় হিসাব (Exchange Summary)" else "Exchange Financial Summary",
            icon = BrandedIconType.CALCULATOR,
            dividerBeforeLastRow = true,
            rows = listOf(
                PdfKeyValueLine(
                    label = if (bn) "পার্থক্য (সমন্বয়)" else "Net Price Difference",
                    value = when {
                        diff > 0L -> "+ ${formatMoney(diff, currency, spacedTaka = true)}"
                        diff < 0L -> "- ${formatMoney(-diff, currency, spacedTaka = true)}"
                        else -> formatMoney(0L, currency, spacedTaka = true)
                    },
                    valueColor = if (diff > 0L) BrandedDocumentPalette.DueRedText else BrandedDocumentPalette.PaymentGreenText,
                    boldValue = true
                ),
                PdfKeyValueLine(if (bn) "পদ্ধতি" else "Method", ex?.refundOrPaymentMethod ?: "-")
            )
        )
        val bal = ex?.currentAccountBalanceMinor ?: (document.balanceMinor ?: 0L)
        val rightSummary = PdfSummaryCardSpec(
            title = if (bn) "গ্রাহকের বর্তমান হিসাব" else "Updated Customer Balance",
            icon = BrandedIconType.COIN_TAKA,
            rows = listOf(PdfKeyValueLine(if (bn) "বর্তমান ব্যালেন্স" else "Current Balance", formatBalanceText(bal, bn, currency))),
            totalPill = balancePillSpec(bal, bn, currency)
        )
        val qrPayload = if (settings.showQrCode) {
            "ADV-ERP|DOC|EXC|${document.number}".filter { it.code in 32..126 }.take(80)
        } else null
        return BrandedPdfDocumentSpec(
            module = CommunicationModule.EXCHANGE,
            safeFileStem = safeFileStem(document),
            isBangla = bn,
            headerRightTop = if (bn) "পণ্য বিনিময়" else "Product Exchange",
            headerRightBottom = if (bn) "Product Exchange" else "",
            headerRightIcon = BrandedIconType.EXCHANGE_ARROWS,
            contactStrip = contactStrip,
            leftInfoCard = buildPartyInfoCard(document, bn),
            rightInfoCard = rightInfo,
            tableTitle = if (bn) "ফেরত ও প্রতিস্থাপিত পণ্যের তালিকা" else "Returned & Replacement Items",
            tableMode = PdfTableMode.EXCHANGE_ITEMS,
            tableColumns = cols,
            tableRows = tableRows,
            leftSummaryCard = leftSummary,
            rightSummaryCard = rightSummary,
            bottomBlock = PdfBottomFooterBlockSpec(
                notesTitle = if (bn) "গুরুত্বপূর্ণ তথ্য (Exchange Notes)" else "Exchange Notes",
                notesBullets = listOf(if (bn) "ফেরত ও বদলি করা পণ্যের হিসাব ইনভেন্টরি ও লেজারে সংরক্ষিত হয়েছে।" else "Product exchange quantities and price difference have been recorded."),
                thankYouPrimary = if (bn) "আমাদের সঙ্গে ব্যবসা করার জন্য আন্তরিক ধন্যবাদ।" else "Thank you for doing business with us.",
                thankYouSecondary = if (bn) "Thank you for doing business with us." else null,
                qrPayload = qrPayload,
                qrCaptionPrimary = if (qrPayload != null) (if (bn) "নথি যাচাই করুন" else "Verify Exchange") else null,
                qrCaptionSecondary = if (qrPayload != null) "Scan to Verify" else null,
                showAuthorizedSignature = settings.includeSignature,
                signaturePrimaryLabel = "Authorised Signature",
                signatureSecondaryLabel = document.brand?.companyName ?: "ADVANCE ERP"
            ),
            generatedOnText = generatedOnText
        )
    }

    private fun buildDeliverySpec(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean,
        contactStrip: PdfContactStripSpec,
        generatedOnText: String
    ): BrandedPdfDocumentSpec {
        val del = document.deliveryBreakdown
        val dateText = document.documentDateMillis?.let {
            BrandedDocumentFormatter.formatDateTime(it, bn, includeTime = true, shortMonth = true)
        } ?: document.fields["document_date"].orEmpty()
        val rightInfo = PdfInfoCardSpec(
            title = if (bn) "চালান ও ডেলিভারি তথ্য (Challan Info)" else "Delivery Challan Details",
            icon = BrandedIconType.DELIVERY_TRUCK,
            lines = buildList {
                add(PdfKeyValueLine(if (bn) "চালান নং" else "Challan No", document.number, boldValue = true))
                if (dateText.isNotEmpty()) add(PdfKeyValueLine(if (bn) "প্রেরণের তারিখ" else "Dispatch Date", dateText))
                del?.invoiceNumber?.takeIf { it.isNotBlank() }?.let { invNo ->
                    add(PdfKeyValueLine(if (bn) "ইনভয়েস রেফারেন্স" else "Invoice Ref", invNo))
                }
                del?.deliveryPerson?.takeIf { it.isNotBlank() }?.let { dp ->
                    add(PdfKeyValueLine(if (bn) "ডেলিভারি প্রতিনিধি" else "Delivery Person", dp))
                }
                del?.transportInfo?.takeIf { it.isNotBlank() }?.let { tr ->
                    add(PdfKeyValueLine(if (bn) "পরিবহন / মাধ্যম" else "Transport / Courier", tr))
                }
            }
        )
        val cols = listOf(
            PdfTableColumnSpec("idx", "#", null, 28f, PdfColAlign.CENTER),
            PdfTableColumnSpec("prod", if (bn) "পণ্যের নাম (Product)" else "Product", null, 275f, PdfColAlign.LEFT),
            PdfTableColumnSpec("color", if (bn) "কালার" else "Color", null, 68f, PdfColAlign.CENTER),
            PdfTableColumnSpec("size", if (bn) "সাইজ" else "Size", null, 68f, PdfColAlign.CENTER),
            PdfTableColumnSpec("qty", if (bn) "প্রেরিত পরিমাণ" else "Dispatched Qty", null, 100f, PdfColAlign.CENTER)
        )
        val tableRows = document.lineItems.map { item ->
            PdfTableRowSpec(
                indexText = item.index.toString(),
                showThumbnailSlot = settings.showProductPhoto,
                thumbnailPath = if (settings.showProductPhoto) item.thumbnailPath else null,
                primaryTitle = if (bn) item.titleBn.ifBlank { item.titleEn } else item.titleEn,
                skuSubtitle = if (settings.showVariantSku && !item.sku.isNullOrBlank()) "SKU: ${item.sku}" else null,
                discountBadgeText = null,
                extraCells = listOf(
                    item.color?.ifBlank { "-" } ?: "-",
                    item.size?.ifBlank { "-" } ?: "-",
                    "${item.quantity} ${item.unit}"
                )
            )
        }
        val totalQty = del?.totalDispatchedQuantity ?: document.lineItems.sumOf { it.quantity }
        val leftSummary = PdfSummaryCardSpec(
            title = if (bn) "ডেলিভারি সারসংক্ষেপ (Dispatch Summary)" else "Dispatch Summary",
            icon = BrandedIconType.DELIVERY_TRUCK,
            rows = buildList {
                add(PdfKeyValueLine(if (bn) "মোট আইটেম সংখ্যা" else "Total Line Items", "${document.lineItems.size}"))
                add(PdfKeyValueLine(if (bn) "সর্বমোট প্রেরিত পরিমাণ" else "Total Dispatched Qty", "$totalQty", valueColor = BrandedDocumentPalette.BrandPurpleDeep, boldValue = true))
                del?.notes?.takeIf { it.isNotBlank() }?.let { n ->
                    add(PdfKeyValueLine(if (bn) "নোট" else "Note", n))
                }
            }
        )
        val rightSummary = PdfSummaryCardSpec(
            title = if (bn) "গ্রহণ ও যাচাই (Receipt Verification)" else "Goods Verification",
            icon = BrandedIconType.CHECK_CIRCLE,
            rows = listOf(
                PdfKeyValueLine(if (bn) "চালান অবস্থা" else "Challan Type", if (bn) "মূল্যবিহীন গেট পাস / চালান" else "Non-Financial Delivery Challan"),
                PdfKeyValueLine(if (bn) "সর্বমোট পরিমাণ" else "Total Quantity", "$totalQty PCS")
            ),
            totalPill = PdfTotalPillSpec(BrandedIconType.DELIVERY_TRUCK, if (bn) "মোট ডেলিভারি" else "Dispatched Units", "$totalQty PCS")
        )
        val qrPayload = if (settings.showQrCode) {
            "ADV-ERP|DOC|DEL|${document.number}".filter { it.code in 32..126 }.take(80)
        } else null
        return BrandedPdfDocumentSpec(
            module = CommunicationModule.DELIVERY,
            safeFileStem = safeFileStem(document),
            isBangla = bn,
            headerRightTop = "ডেলিভারি চালান",
            headerRightBottom = "Delivery Challan",
            headerRightIcon = BrandedIconType.DELIVERY_TRUCK,
            contactStrip = contactStrip,
            leftInfoCard = buildPartyInfoCard(document, bn),
            rightInfoCard = rightInfo,
            tableTitle = if (bn) "প্রেরিত পণ্যের তালিকা (Dispatched Items)" else "Dispatched Items",
            tableMode = PdfTableMode.DELIVERY_CHALLAN,
            tableColumns = cols,
            tableRows = tableRows,
            leftSummaryCard = leftSummary,
            rightSummaryCard = rightSummary,
            bottomBlock = PdfBottomFooterBlockSpec(
                notesTitle = if (bn) "নির্দেশনা (Delivery Notes)" else "Delivery Instructions",
                notesBullets = listOf(
                    if (bn) "পণ্য গ্রহণের সময় অনুগ্রহ করে সংখ্যা ও অবস্থা যাচাই করে নেবেন।" else "Please inspect goods condition and verify item quantities upon delivery."
                ),
                thankYouPrimary = if (bn) "আমাদের সঙ্গে ব্যবসা করার জন্য আন্তরিক ধন্যবাদ।" else "Thank you for doing business with us.",
                thankYouSecondary = if (bn) "Thank you for doing business with us." else null,
                qrPayload = qrPayload,
                qrCaptionPrimary = if (qrPayload != null) (if (bn) "চালান যাচাই" else "Verify Challan") else null,
                qrCaptionSecondary = if (qrPayload != null) "Scan to Verify" else null,
                showAuthorizedSignature = settings.includeSignature,
                signaturePrimaryLabel = "Authorised Signature",
                signatureSecondaryLabel = document.brand?.companyName ?: "ADVANCE ERP",
                receiverSignatureLabel = if (bn) "গ্রহীতার স্বাক্ষর / Receiver's Signature" else "Receiver's Signature"
            ),
            generatedOnText = generatedOnText
        )
    }

    private fun buildPackingSpec(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean,
        contactStrip: PdfContactStripSpec,
        generatedOnText: String
    ): BrandedPdfDocumentSpec {
        val dateText = document.documentDateMillis?.let {
            BrandedDocumentFormatter.formatDateTime(it, bn, includeTime = true, shortMonth = true)
        } ?: document.fields["document_date"].orEmpty()
        val rightInfo = PdfInfoCardSpec(
            title = if (bn) "প্যাকিং তথ্য (Packing Details)" else "Packing List Details",
            icon = BrandedIconType.INVOICE_DOC,
            lines = buildList {
                add(PdfKeyValueLine(if (bn) "পিও / চালান নং" else "Reference No", document.number, boldValue = true))
                if (dateText.isNotEmpty()) add(PdfKeyValueLine(if (bn) "তারিখ" else "Date", dateText))
                add(PdfKeyValueLine(if (bn) "মোট পরিমাণ" else "Total Units", document.fields["total_quantity"] ?: "${document.lineItems.sumOf { it.quantity }}"))
                document.fields["total_carton"]?.let { ctn ->
                    add(PdfKeyValueLine(if (bn) "মোট কার্টন" else "Total Cartons", ctn, boldValue = true))
                }
            }
        )
        val cols = listOf(
            PdfTableColumnSpec("idx", "#", null, 24f, PdfColAlign.CENTER),
            PdfTableColumnSpec("prod", if (bn) "পণ্যের নাম (Product)" else "Product", null, 245f, PdfColAlign.LEFT),
            PdfTableColumnSpec("color", if (bn) "কালার" else "Color", null, 65f, PdfColAlign.CENTER),
            PdfTableColumnSpec("size", if (bn) "সাইজ" else "Size", null, 60f, PdfColAlign.CENTER),
            PdfTableColumnSpec("qty", if (bn) "পরিমাণ" else "Qty", null, 65f, PdfColAlign.CENTER),
            PdfTableColumnSpec("ctn", if (bn) "কার্টন" else "Cartons", null, 80f, PdfColAlign.CENTER)
        )
        val tableRows = document.lineItems.map { item ->
            PdfTableRowSpec(
                indexText = item.index.toString(),
                showThumbnailSlot = settings.showProductPhoto,
                thumbnailPath = if (settings.showProductPhoto) item.thumbnailPath else null,
                primaryTitle = if (bn) item.titleBn.ifBlank { item.titleEn } else item.titleEn,
                skuSubtitle = if (settings.showVariantSku && !item.sku.isNullOrBlank()) "SKU: ${item.sku}" else null,
                discountBadgeText = null,
                extraCells = listOf(
                    item.color?.ifBlank { "-" } ?: "-",
                    item.size?.ifBlank { "-" } ?: "-",
                    "${item.quantity} ${item.unit}",
                    item.secondaryQuantityText ?: "-"
                )
            )
        }
        val leftSummary = PdfSummaryCardSpec(
            title = if (bn) "প্যাকিং ও ওজন সারসংক্ষেপ" else "Weight & Volume Summary",
            icon = BrandedIconType.CALCULATOR,
            rows = buildList {
                add(PdfKeyValueLine(if (bn) "মোট পিস" else "Total Quantity", document.fields["total_quantity"] ?: "0"))
                add(PdfKeyValueLine(if (bn) "মোট কার্টন" else "Total Cartons", document.fields["total_carton"] ?: "0", boldValue = true))
                document.fields["gross_weight"]?.takeIf { it.isNotBlank() }?.let { w ->
                    add(PdfKeyValueLine(if (bn) "মোট গ্রস ওজন (কেজি)" else "Total Gross Weight (kg)", w))
                }
                document.fields["total_cbm"]?.takeIf { it.isNotBlank() }?.let { cbm ->
                    add(PdfKeyValueLine(if (bn) "মোট সিবিএম (CBM)" else "Total Volume (CBM)", cbm))
                }
            }
        )
        val rightSummary = PdfSummaryCardSpec(
            title = if (bn) "লজিস্টিকস তথ্য (Shipping Ref)" else "Shipping Reference",
            icon = BrandedIconType.DELIVERY_TRUCK,
            rows = listOf(PdfKeyValueLine(if (bn) "রেফারেন্স নং" else "PO Reference", document.number)),
            totalPill = PdfTotalPillSpec(
                icon = BrandedIconType.DELIVERY_TRUCK,
                label = if (bn) "মোট কার্টন" else "Total Cartons",
                amountText = "${document.fields["total_carton"] ?: "0"} CTN"
            )
        )
        val qrPayload = if (settings.showQrCode) {
            "ADV-ERP|DOC|PKG|${document.number}".filter { it.code in 32..126 }.take(80)
        } else null
        return BrandedPdfDocumentSpec(
            module = CommunicationModule.PACKING,
            safeFileStem = safeFileStem(document),
            isBangla = bn,
            headerRightTop = if (bn) "প্যাকিং তালিকা" else "Packing List",
            headerRightBottom = if (bn) "Packing List" else "",
            headerRightIcon = BrandedIconType.DELIVERY_TRUCK,
            contactStrip = contactStrip,
            leftInfoCard = buildPartyInfoCard(document, bn),
            rightInfoCard = rightInfo,
            tableTitle = if (bn) "কার্টন ও পণ্যের তালিকা (Packing Breakdown)" else "Packing Breakdown",
            tableMode = PdfTableMode.PACKING_LIST,
            tableColumns = cols,
            tableRows = tableRows,
            leftSummaryCard = leftSummary,
            rightSummaryCard = rightSummary,
            bottomBlock = PdfBottomFooterBlockSpec(
                notesTitle = if (bn) "নির্দেশনা (Packing Notes)" else "Packing Instructions",
                notesBullets = listOf(if (bn) "শিপমেন্ট ও কার্টনের পরিমাণ অনুযায়ী প্যাকিং তালিকা প্রস্তুত করা হয়েছে।" else "Carton counts, weights, and volumes are summarized for logistics dispatch."),
                thankYouPrimary = if (bn) "আপনার সহযোগিতার জন্য ধন্যবাদ।" else "Thank you for your business.",
                thankYouSecondary = null,
                qrPayload = qrPayload,
                qrCaptionPrimary = if (qrPayload != null) (if (bn) "তালিকা যাচাই" else "Verify Packing") else null,
                qrCaptionSecondary = if (qrPayload != null) "Scan to Verify" else null,
                showAuthorizedSignature = settings.includeSignature,
                signaturePrimaryLabel = "Authorised Signature",
                signatureSecondaryLabel = document.brand?.companyName ?: "ADVANCE ERP"
            ),
            generatedOnText = generatedOnText
        )
    }

    private fun balancePillSpec(
        balanceMinor: Long,
        bn: Boolean,
        currency: String,
        preferGrandTotalLabel: Boolean = false,
        isSupplier: Boolean = false
    ): PdfTotalPillSpec {
        return when {
            balanceMinor > 0L -> PdfTotalPillSpec(
                icon = if (preferGrandTotalLabel) BrandedIconType.MONEY_BAG else BrandedIconType.BALANCE_SCALE,
                label = when {
                    isSupplier && bn -> "মোট প্রদেয় বকেয়া"
                    isSupplier -> "Total Payable"
                    preferGrandTotalLabel && bn -> "সর্বমোট বকেয়া"
                    preferGrandTotalLabel -> "Total Outstanding"
                    bn -> "বর্তমান বকেয়া"
                    else -> "Current Balance"
                },
                amountText = formatMoney(balanceMinor, currency, spacedTaka = true)
            )
            balanceMinor < 0L -> PdfTotalPillSpec(
                icon = BrandedIconType.BALANCE_SCALE,
                label = if (bn) "বর্তমান অগ্রিম ক্রেডিট" else "Advance Credit",
                amountText = formatMoney(-balanceMinor, currency, spacedTaka = true)
            )
            else -> PdfTotalPillSpec(
                icon = BrandedIconType.CHECK_CIRCLE,
                label = if (preferGrandTotalLabel && bn) "সর্বমোট বকেয়া" else if (bn) "বর্তমান ব্যালেন্স" else "Current Balance",
                amountText = formatMoney(0L, currency, spacedTaka = true)
            )
        }
    }

    /**
     * Pure-JVM deterministic multi-page pagination ensuring:
     * 1) Repeated table headers on every continuation page that contains rows
     * 2) No mid-row splitting or orphaned summary/bottom blocks
     * 3) Guaranteed progress on any non-zero page height.
     */
    fun paginateDocument(
        rowHeights: List<Float>,
        firstPageTopHeight: Float,
        contPageTopHeight: Float,
        sectionTitleHeight: Float,
        tableHeaderHeight: Float,
        summaryHeight: Float,
        bottomHeight: Float,
        usableBottomY: Float
    ): List<PdfPageSlice> {
        val slices = mutableListOf<PdfPageSlice>()
        var pageNum = 1
        var rowIdx = 0
        var summaryPlaced = false
        var bottomPlaced = false
        val totalRows = rowHeights.size

        while (!bottomPlaced || !summaryPlaced || rowIdx < totalRows) {
            val isFirst = pageNum == 1
            var y = if (isFirst) firstPageTopHeight else contPageTopHeight
            val rowStart = rowIdx

            if (rowIdx < totalRows) {
                val neededTableChrome = sectionTitleHeight + tableHeaderHeight
                val firstRowH = rowHeights[rowIdx]
                // Only defer table start from page 1 if first page top section leaves zero room for even header + 1st row
                val canStartTableHere = !isFirst || (y + neededTableChrome + firstRowH <= usableBottomY)
                if (canStartTableHere) {
                    y += neededTableChrome
                    while (rowIdx < totalRows) {
                        val h = rowHeights[rowIdx]
                        if (y + h <= usableBottomY || rowIdx == rowStart) {
                            y += h
                            rowIdx++
                        } else {
                            break
                        }
                    }
                    y += 8f // table bottom gap
                }
            }

            val allRowsDone = rowIdx >= totalRows
            var includeSummary = false
            if (allRowsDone && !summaryPlaced) {
                val forceFit = !isFirst && rowStart == rowIdx && (y + summaryHeight > usableBottomY)
                if (y + summaryHeight <= usableBottomY || forceFit) {
                    includeSummary = true
                    summaryPlaced = true
                    y += summaryHeight + 8f
                }
            }

            var includeBottom = false
            if (allRowsDone && summaryPlaced && !bottomPlaced) {
                val forceFitBottom = !isFirst && rowStart == rowIdx && !includeSummary && (y + bottomHeight > usableBottomY)
                if (y + bottomHeight <= usableBottomY || forceFitBottom) {
                    includeBottom = true
                    bottomPlaced = true
                }
            }

            slices.add(
                PdfPageSlice(
                    pageNumber = pageNum,
                    isFirstPage = isFirst,
                    rowStartIndex = rowStart,
                    rowEndExclusive = rowIdx,
                    includeSummarySection = includeSummary,
                    includeBottomSection = includeBottom
                )
            )
            pageNum++
            if (pageNum > 500) break // safety guard
        }

        return slices
    }
}
