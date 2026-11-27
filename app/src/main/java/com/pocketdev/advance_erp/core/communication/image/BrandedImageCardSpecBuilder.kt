package com.pocketdev.advance_erp.core.communication.image

import com.pocketdev.advance_erp.core.communication.CommunicationModule
import com.pocketdev.advance_erp.core.communication.CommunicationStatus
import com.pocketdev.advance_erp.core.communication.SafeDocument
import com.pocketdev.advance_erp.core.communication.graphics.BrandedDocumentFormatter
import com.pocketdev.advance_erp.core.communication.graphics.BrandedDocumentPalette
import com.pocketdev.advance_erp.core.communication.graphics.BrandedIconType
import com.pocketdev.advance_erp.core.communication.pdf.BrandedPdfSpecBuilder
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import kotlin.math.abs

data class ImageCardRightBadgeSpec(
    val topChipText: String? = null,
    val topChipIcon: BrandedIconType? = null,
    val bottomSubText: String? = null,
    val statusBadge: BrandedDocumentPalette.StatusBadgeSpec? = null
)

data class ImageCardGreetingSpec(
    val salutationLine: String,
    val recipientLine: String,
    val introParagraph: String
)

data class ImageCardItemRowSpec(
    val showThumbnailSlot: Boolean,
    val thumbnailPath: String?,
    val title: String,
    val variantAndSkuLine: String?,
    val qtyAndPriceLine: String,
    val discountBadgeText: String? = null,
    val rightAmountText: String?,
    val directionTagText: String? = null,
    val directionTagColor: Int = BrandedDocumentPalette.BrandPurplePrimary
)

data class ImageCardMetricRowSpec(
    val icon: BrandedIconType? = null,
    val label: String,
    val value: String,
    val valueColor: Int = BrandedDocumentPalette.TextPrimary,
    val boldValue: Boolean = false
)

sealed interface ImageCardBodySection {
    data class ReferencePillSection(
        val icon: BrandedIconType,
        val primaryLine: String,
        val secondaryLine: String? = null
    ) : ImageCardBodySection

    data class HeroAmountSection(
        val icon: BrandedIconType,
        val label: String,
        val amountText: String,
        val subtitle: String? = null,
        val bgColor: Int,
        val borderColor: Int,
        val amountColor: Int
    ) : ImageCardBodySection

    data class ItemsListSection(
        val icon: BrandedIconType,
        val title: String,
        val subtitleRight: String? = null,
        val items: List<ImageCardItemRowSpec>
    ) : ImageCardBodySection

    data class MetricListCardSection(
        val icon: BrandedIconType? = null,
        val title: String? = null,
        val rows: List<ImageCardMetricRowSpec>,
        val bgColor: Int = BrandedDocumentPalette.SectionBoxBg
    ) : ImageCardBodySection

    data class SplitSummaryCardsSection(
        val leftIcon: BrandedIconType,
        val leftTitle: String,
        val leftRows: List<ImageCardMetricRowSpec>,
        val rightIcon: BrandedIconType,
        val rightTitle: String,
        val rightRows: List<ImageCardMetricRowSpec>,
        val rightFootnote: String? = null
    ) : ImageCardBodySection

    data class PurpleTotalPillSection(
        val icon: BrandedIconType,
        val label: String,
        val amountText: String
    ) : ImageCardBodySection

    data class NoticeStripSection(
        val icon: BrandedIconType,
        val text: String,
        val bgColor: Int = BrandedDocumentPalette.SectionBoxBg,
        val borderColor: Int = BrandedDocumentPalette.SectionBoxBorder,
        val iconAndTextColor: Int = BrandedDocumentPalette.TextSecondary
    ) : ImageCardBodySection
}

data class BrandedImageCardPageSpec(
    val module: CommunicationModule,
    val pageIndex: Int,
    val totalPages: Int,
    val safeFileName: String,
    val isBangla: Boolean,
    val titleIcon: BrandedIconType,
    val titlePrimary: String,
    val titleSecondary: String,
    val rightBadge: ImageCardRightBadgeSpec,
    val greeting: ImageCardGreetingSpec?,
    val sections: List<ImageCardBodySection>,
    val thankYouText: String?,
    val footerPhone: String,
    val footerAddress: String
)

/**
 * Pure-JVM specification builder translating validated [SafeDocument] + [CommunicationSettings]
 * into approved portrait HD PNG card pages (REF-02..REF-05 + extended ERP modules) with
 * deterministic chunking for long item lists.
 */
object BrandedImageCardSpecBuilder {

    const val MAX_ITEMS_PER_IMAGE_CARD = 8

    fun buildPages(
        document: SafeDocument,
        settings: CommunicationSettings,
        language: String
    ): List<BrandedImageCardPageSpec> {
        val bn = language == "bn"
        return when (document.request.module) {
            CommunicationModule.INVOICE -> buildInvoicePages(document, settings, bn)
            CommunicationModule.PAYMENT, CommunicationModule.SUPPLIER_PAYMENT -> listOf(buildPaymentCard(document, bn))
            CommunicationModule.REMINDER -> listOf(buildReminderCard(document, bn))
            CommunicationModule.LEDGER, CommunicationModule.STATEMENT, CommunicationModule.SUPPLIER_STATEMENT -> listOf(buildStatementCard(document, bn))
            CommunicationModule.DELIVERY -> buildDeliveryPages(document, settings, bn)
            CommunicationModule.RETURN -> buildReturnPages(document, settings, bn)
            CommunicationModule.EXCHANGE -> buildExchangePages(document, settings, bn)
            CommunicationModule.QUOTATION -> buildQuotationOrOrderPages(document, settings, bn, isQuotation = true)
            CommunicationModule.SALES_ORDER -> buildQuotationOrOrderPages(document, settings, bn, isQuotation = false)
            CommunicationModule.PURCHASE_ORDER -> buildPurchaseOrderPages(document, settings, bn)
            CommunicationModule.PACKING -> buildPackingPages(document, settings, bn)
        }
    }

    private fun pageFileName(stem: String, pageIndex: Int, totalPages: Int): String {
        return if (totalPages <= 1) "${stem}_card.png" else "${stem}_card_${pageIndex}.png"
    }

    private fun footerPhone(document: SafeDocument): String =
        document.brand?.phone?.trim().orEmpty()

    private fun footerAddress(document: SafeDocument): String =
        document.brand?.address?.trim().orEmpty()

    private fun partyName(document: SafeDocument): String =
        document.party?.name?.trim()?.ifBlank { document.partyName } ?: document.partyName

    private fun salutationGreeting(document: SafeDocument, bn: Boolean, introBn: String, introEn: String): ImageCardGreetingSpec {
        val pName = partyName(document)
        return ImageCardGreetingSpec(
            salutationLine = if (bn) "আসসালামু আলাইকুম," else "Dear Customer,",
            recipientLine = if (bn) "সম্মানিত $pName," else "$pName,",
            introParagraph = if (bn) introBn else introEn
        )
    }

    private fun supplierGreeting(document: SafeDocument, bn: Boolean, introBn: String, introEn: String): ImageCardGreetingSpec {
        val sName = partyName(document)
        return ImageCardGreetingSpec(
            salutationLine = if (bn) "আসসালামু আলাইকুম," else "Dear Partner,",
            recipientLine = if (bn) "$sName," else "$sName,",
            introParagraph = if (bn) introBn else introEn
        )
    }

    private fun standardThankYou(bn: Boolean): String =
        if (bn) "আমাদের সঙ্গে ব্যবসা করার জন্য আন্তরিক ধন্যবাদ।" else "Thank you for doing business with us."

    private fun mapLineItems(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean,
        includePrices: Boolean,
        includeDirectionTag: Boolean = false
    ): List<ImageCardItemRowSpec> {
        return document.lineItems.map { item ->
            val variantParts = buildList {
                item.color?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
                item.size?.trim()?.takeIf { it.isNotEmpty() }?.let { add("Size: $it") }
                if (settings.showVariantSku) {
                    item.sku?.trim()?.takeIf { it.isNotEmpty() }?.let { add("SKU: $it") }
                }
            }
            val qtyStr = if (item.unit.equals("PCS", ignoreCase = true) || item.unit.isBlank()) {
                "${item.quantity}"
            } else {
                "${item.quantity} ${item.unit}"
            }
            val qtyAndPrice = if (includePrices) {
                "Qty: $qtyStr × ${BrandedPdfSpecBuilder.formatMoney(item.unitPriceMinor, item.currencyCode)}"
            } else if (!item.secondaryQuantityText.isNullOrBlank()) {
                "Qty: $qtyStr · ${item.secondaryQuantityText}"
            } else {
                if (bn) "পরিমাণ: $qtyStr ${item.unit}" else "Dispatched: $qtyStr ${item.unit}"
            }
            val dirTag = if (includeDirectionTag && !item.secondaryQuantityText.isNullOrBlank()) {
                val isRet = item.secondaryQuantityText.contains("RETURN", ignoreCase = true) ||
                    item.secondaryQuantityText.contains("ফেরত")
                (if (isRet) (if (bn) "ফেরত" else "RETURN") else (if (bn) "বদলি" else "REPLACE")) to
                    (if (isRet) BrandedDocumentPalette.DueRedText else BrandedDocumentPalette.PaymentGreenText)
            } else null

            ImageCardItemRowSpec(
                showThumbnailSlot = settings.showProductPhoto,
                thumbnailPath = if (settings.showProductPhoto) item.thumbnailPath else null,
                title = if (bn) item.titleBn.ifBlank { item.titleEn } else item.titleEn,
                variantAndSkuLine = variantParts.joinToString(" | ").takeIf { it.isNotEmpty() },
                qtyAndPriceLine = qtyAndPrice,
                discountBadgeText = if (includePrices) {
                    BrandedDocumentFormatter.formatDiscountBadge(item.discountMinor, item.discountPercentBps)
                } else null,
                rightAmountText = when {
                    includePrices && item.lineTotalMinor > 0L -> BrandedPdfSpecBuilder.formatMoney(item.lineTotalMinor, item.currencyCode)
                    !includePrices && !item.secondaryQuantityText.isNullOrBlank() -> item.secondaryQuantityText
                    else -> null
                },
                directionTagText = dirTag?.first,
                directionTagColor = dirTag?.second ?: BrandedDocumentPalette.BrandPurplePrimary
            )
        }
    }

    // region REF-02: Sales Invoice Summary Card

    private fun buildInvoicePages(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean
    ): List<BrandedImageCardPageSpec> {
        val stem = BrandedPdfSpecBuilder.safeFileStem(document)
        val currency = document.brand?.currencyCode ?: "BDT"
        val inv = document.invoiceBreakdown
        val allRows = mapLineItems(document, settings, bn, includePrices = true)
        val chunks = if (allRows.isEmpty()) listOf(emptyList()) else allRows.chunked(MAX_ITEMS_PER_IMAGE_CARD)
        val totalPages = chunks.size

        val statusBadge = BrandedDocumentPalette.resolveStatusBadge(document.status, bn)
        val netBillMinor = inv?.netInvoiceAmountMinor ?: 0L
        val paidMinor = inv?.allocatedPaymentMinor ?: 0L
        val invDueMinor = inv?.invoiceDueMinor ?: 0L
        val invDiscMinor = inv?.invoiceDiscountMinor ?: 0L
        val creditReturnMinor = inv?.creditReturnMinor ?: 0L
        val liveBal = inv?.currentAccountBalanceMinor ?: (document.balanceMinor ?: invDueMinor)

        val leftSummaryRows = buildList {
            add(
                ImageCardMetricRowSpec(
                    label = if (bn) "মোট বিল" else "Invoice Total",
                    value = BrandedPdfSpecBuilder.formatMoney(netBillMinor, currency),
                    boldValue = true
                )
            )
            if (invDiscMinor > 0L) {
                add(
                    ImageCardMetricRowSpec(
                        label = if (bn) "ডিসকাউন্ট" else "Discount",
                        value = "- ${BrandedPdfSpecBuilder.formatMoney(invDiscMinor, currency)}",
                        valueColor = BrandedDocumentPalette.DueRedText
                    )
                )
            }
            if (creditReturnMinor > 0L) {
                add(
                    ImageCardMetricRowSpec(
                        label = if (bn) "ফেরত সমন্বয়" else "Return Credit",
                        value = "- ${BrandedPdfSpecBuilder.formatMoney(creditReturnMinor, currency)}",
                        valueColor = BrandedDocumentPalette.CreditBlueText
                    )
                )
            }
            add(
                ImageCardMetricRowSpec(
                    label = if (bn) "পরিশোধ" else "Paid",
                    value = BrandedPdfSpecBuilder.formatMoney(paidMinor, currency),
                    valueColor = BrandedDocumentPalette.PaymentGreenText,
                    boldValue = paidMinor > 0L
                )
            )
            add(
                ImageCardMetricRowSpec(
                    label = if (bn) "ইনভয়েস বকেয়া" else "Invoice Due",
                    value = BrandedPdfSpecBuilder.formatMoney(invDueMinor, currency),
                    valueColor = if (invDueMinor > 0L) BrandedDocumentPalette.DueRedText else BrandedDocumentPalette.PaymentGreenText,
                    boldValue = true
                )
            )
        }

        val rightSummaryRows = buildList {
            if (inv == null || !inv.hasHistoricalSnapshot) {
                add(
                    ImageCardMetricRowSpec(
                        label = if (bn) "পূর্বের বকেয়া" else "Previous Due",
                        value = if (bn) "অনুপলব্ধ" else "Unavailable",
                        valueColor = BrandedDocumentPalette.TextMuted
                    )
                )
                add(
                    ImageCardMetricRowSpec(
                        label = if (bn) "বর্তমান বকেয়া" else "Invoice Due",
                        value = BrandedPdfSpecBuilder.formatMoney(invDueMinor, currency),
                        boldValue = true
                    )
                )
            } else {
                val prev = inv.historicalPreviousBalanceMinor ?: 0L
                add(
                    ImageCardMetricRowSpec(
                        label = if (prev < 0L) (if (bn) "পূর্বের ক্রেডিট" else "Prev. Credit") else (if (bn) "পূর্বের বকেয়া" else "Previous Due"),
                        value = BrandedPdfSpecBuilder.formatMoney(abs(prev), currency),
                        valueColor = if (prev < 0L) BrandedDocumentPalette.CreditBlueText else BrandedDocumentPalette.TextPrimary
                    )
                )
                add(
                    ImageCardMetricRowSpec(
                        label = if (bn) "বর্তমান বকেয়া" else "Current Inv. Due",
                        value = BrandedPdfSpecBuilder.formatMoney(invDueMinor, currency),
                        boldValue = true
                    )
                )
            }
        }
        val rightFootnote = if (inv?.isReshareAfterLaterActivity == true) {
            (if (bn) "হালনাগাদ: " else "Live as of ") +
                BrandedDocumentFormatter.formatDateTime(inv.asOfMillis, bn, includeTime = true, shortMonth = true)
        } else null

        return chunks.mapIndexed { idx, chunk ->
            val pageNum = idx + 1
            val isFirst = pageNum == 1
            val isLast = pageNum == totalPages
            val sections = buildList<ImageCardBodySection> {
                if (chunk.isNotEmpty()) {
                    add(
                        ImageCardBodySection.ItemsListSection(
                            icon = BrandedIconType.SHOPPING_CART,
                            title = if (bn) "ক্রয়ের বিবরণ" else "Purchased Items",
                            subtitleRight = if (totalPages > 1) {
                                if (bn) "পৃষ্ঠা $pageNum/$totalPages" else "Page $pageNum/$totalPages"
                            } else null,
                            items = chunk
                        )
                    )
                }
                if (isLast) {
                    add(
                        ImageCardBodySection.SplitSummaryCardsSection(
                            leftIcon = BrandedIconType.CALCULATOR,
                            leftTitle = if (bn) "ইনভয়েসের হিসাব" else "Invoice Summary",
                            leftRows = leftSummaryRows,
                            rightIcon = BrandedIconType.CALENDAR,
                            rightTitle = if (bn) "আপনার সামগ্রিক হিসাব" else "Account Summary",
                            rightRows = rightSummaryRows,
                            rightFootnote = rightFootnote
                        )
                    )
                    add(balancePillSection(liveBal, bn, currency, preferGrandTotal = true))
                    add(
                        ImageCardBodySection.NoticeStripSection(
                            icon = BrandedIconType.INFO_CIRCLE,
                            text = if (bn) "বিস্তারিত জানতে সংযুক্ত PDF ইনভয়েস দেখুন।" else "See the attached PDF invoice for complete line details."
                        )
                    )
                }
            }
            BrandedImageCardPageSpec(
                module = CommunicationModule.INVOICE,
                pageIndex = pageNum,
                totalPages = totalPages,
                safeFileName = pageFileName(stem, pageNum, totalPages),
                isBangla = bn,
                titleIcon = BrandedIconType.INVOICE_DOC,
                titlePrimary = "বিক্রয় ইনভয়েস",
                titleSecondary = "Sales Invoice",
                rightBadge = ImageCardRightBadgeSpec(
                    topChipText = if (totalPages > 1) "${document.number} ($pageNum/$totalPages)" else document.number,
                    statusBadge = statusBadge
                ),
                greeting = if (isFirst) {
                    salutationGreeting(
                        document,
                        bn,
                        introBn = "আপনার ক্রয়ের ইনভয়েসের সাথে পূর্বের হিসাব ও বর্তমান ব্যালেন্স নিচে দেওয়া হলো।",
                        introEn = "Please find your invoice summary along with your previous and current account balances below."
                    )
                } else null,
                sections = sections,
                thankYouText = if (isLast) standardThankYou(bn) else null,
                footerPhone = footerPhone(document),
                footerAddress = footerAddress(document)
            )
        }
    }

    // endregion

    // region REF-03: Payment Received Card

    private fun buildPaymentCard(document: SafeDocument, bn: Boolean): BrandedImageCardPageSpec {
        val stem = BrandedPdfSpecBuilder.safeFileStem(document)
        val pay = document.paymentBreakdown
        val currency = pay?.currencyCode ?: (document.brand?.currencyCode ?: "BDT")
        val balCurrency = document.brand?.currencyCode ?: currency
        val amountMinor = pay?.paymentAmountMinor ?: 0L
        val payDateMs = pay?.paymentDateMillis ?: document.documentDateMillis ?: document.asOfMillis
        val curBal = pay?.currentAccountBalanceMinor ?: (document.balanceMinor ?: 0L)
        val isSupplier = document.request.module == CommunicationModule.SUPPLIER_PAYMENT

        val greenStatus = BrandedDocumentPalette.StatusBadgeSpec(
            text = when {
                isSupplier && bn -> "পেমেন্ট সম্পন্ন হয়েছে"
                isSupplier -> "Payment Paid"
                bn -> "পেমেন্ট গ্রহণ করা হয়েছে"
                else -> "Payment Received"
            },
            textColor = BrandedDocumentPalette.PaymentGreenText,
            bgColor = BrandedDocumentPalette.PaymentGreenBg,
            borderColor = BrandedDocumentPalette.PaymentGreenBorder,
            isCheckIcon = true,
            isAlertIcon = false
        )

        val paymentDetailsRows = buildList {
            add(
                ImageCardMetricRowSpec(
                    icon = BrandedIconType.PAYMENT_CARD,
                    label = if (bn) "পরিশোধের পরিমাণ" else "Amount Paid",
                    value = BrandedPdfSpecBuilder.formatMoney(amountMinor, currency),
                    valueColor = BrandedDocumentPalette.PaymentGreenText,
                    boldValue = true
                )
            )
            add(
                ImageCardMetricRowSpec(
                    icon = BrandedIconType.BANK_BUILDING,
                    label = if (bn) "পেমেন্ট মাধ্যম" else "Payment Method",
                    value = pay?.paymentMethod ?: document.fields["payment_method"] ?: "-"
                )
            )
            add(
                ImageCardMetricRowSpec(
                    icon = BrandedIconType.INVOICE_DOC,
                    label = if (bn) "রেফারেন্স" else "Reference",
                    value = pay?.referenceNote?.ifBlank { "-" } ?: "-"
                )
            )
            if (pay != null && pay.allocations.isNotEmpty()) {
                val allocText = pay.allocations.joinToString(", ") { it.invoiceNumber }
                add(
                    ImageCardMetricRowSpec(
                        icon = BrandedIconType.CHECK_CIRCLE,
                        label = if (bn) "সমন্বিত ইনভয়েস" else "Applied To",
                        value = allocText
                    )
                )
            }
        }

        val balanceRows = buildList {
            if (pay?.previousBalanceMinor != null) {
                val prev = pay.previousBalanceMinor
                add(
                    ImageCardMetricRowSpec(
                        label = if (prev < 0L) (if (bn) "পূর্বের ক্রেডিট" else "Prev. Credit") else (if (bn) "পূর্বের বকেয়া" else "Previous Balance"),
                        value = BrandedPdfSpecBuilder.formatMoney(abs(prev), balCurrency),
                        valueColor = if (prev < 0L) BrandedDocumentPalette.CreditBlueText else BrandedDocumentPalette.TextPrimary
                    )
                )
            }
            add(
                ImageCardMetricRowSpec(
                    label = if (bn) "পরিশোধিত" else "Paid",
                    value = "- ${BrandedPdfSpecBuilder.formatMoney(amountMinor, balCurrency)}",
                    valueColor = BrandedDocumentPalette.PaymentGreenText,
                    boldValue = true
                )
            )
            if (pay?.hasLaterActivityAfterPayment == true && pay.balanceAfterPaymentMinor != null) {
                add(
                    ImageCardMetricRowSpec(
                        label = if (bn) "রসিদ সময়ের জের" else "Balance After Payment",
                        value = BrandedPdfSpecBuilder.formatBalanceText(pay.balanceAfterPaymentMinor, bn, balCurrency, spacedTaka = false)
                    )
                )
            }
        }

        val sections = listOf(
            ImageCardBodySection.ReferencePillSection(
                icon = BrandedIconType.COIN_TAKA,
                primaryLine = (if (bn) "পেমেন্ট নং: " else "Payment No: ") + document.number,
                secondaryLine = (if (bn) "তারিখ: " else "Date: ") +
                    BrandedDocumentFormatter.formatDateTime(payDateMs, bn, includeTime = true, shortMonth = false)
            ),
            ImageCardBodySection.MetricListCardSection(
                icon = BrandedIconType.PAYMENT_CARD,
                title = if (bn) "পেমেন্টের বিবরণ" else "Payment Details",
                rows = paymentDetailsRows
            ),
            ImageCardBodySection.MetricListCardSection(
                icon = BrandedIconType.COIN_TAKA,
                title = if (bn) "হালনাগাদ হিসাব" else "Updated Account",
                rows = balanceRows
            ),
            balancePillSection(curBal, bn, balCurrency, isSupplier = isSupplier),
            ImageCardBodySection.NoticeStripSection(
                icon = BrandedIconType.CHECK_CIRCLE,
                text = if (bn) "আপনার হিসাব সফলভাবে আপডেট করা হয়েছে।" else "Your account balance has been updated successfully.",
                bgColor = BrandedDocumentPalette.PaymentGreenBg,
                borderColor = BrandedDocumentPalette.PaymentGreenBorder,
                iconAndTextColor = BrandedDocumentPalette.PaymentGreenText
            )
        )

        return BrandedImageCardPageSpec(
            module = document.request.module,
            pageIndex = 1,
            totalPages = 1,
            safeFileName = pageFileName(stem, 1, 1),
            isBangla = bn,
            titleIcon = BrandedIconType.PAYMENT_RECEIPT,
            titlePrimary = if (isSupplier) (if (bn) "সরবরাহকারী পেমেন্ট" else "Supplier Payment") else "পেমেন্ট গ্রহণের রসিদ",
            titleSecondary = if (isSupplier) "Supplier Payment" else "Payment Received",
            rightBadge = ImageCardRightBadgeSpec(statusBadge = greenStatus),
            greeting = if (isSupplier) {
                supplierGreeting(
                    document,
                    bn,
                    introBn = "আপনার অ্যাকাউন্টে প্রদত্ত পেমেন্টের বিবরণ নিচে দেওয়া হলো।",
                    introEn = "Please find the payment details recorded to your supplier account below."
                )
            } else {
                salutationGreeting(
                    document,
                    bn,
                    introBn = "আপনার পেমেন্ট সফলভাবে গ্রহণ করা হয়েছে।",
                    introEn = "Thank you! Your payment has been received successfully."
                )
            },
            sections = sections,
            thankYouText = standardThankYou(bn),
            footerPhone = footerPhone(document),
            footerAddress = footerAddress(document)
        )
    }

    // endregion

    // region REF-04: Due Reminder Card

    private fun buildReminderCard(document: SafeDocument, bn: Boolean): BrandedImageCardPageSpec {
        val stem = BrandedPdfSpecBuilder.safeFileStem(document)
        val rem = document.reminderBreakdown
        val currency = document.brand?.currencyCode ?: "BDT"
        val currentDue = rem?.currentTotalDueMinor ?: (document.balanceMinor ?: 0L)
        val tone = BrandedDocumentPalette.resolveBalanceTone(currentDue)

        val statusBadge = when (tone) {
            BrandedDocumentPalette.BalanceSemanticTone.DUE_RED ->
                BrandedDocumentPalette.resolveStatusBadge(CommunicationStatus.PREVIOUS_DUE, bn)
            BrandedDocumentPalette.BalanceSemanticTone.CREDIT_BLUE ->
                BrandedDocumentPalette.resolveStatusBadge(CommunicationStatus.CREDIT, bn)
            BrandedDocumentPalette.BalanceSemanticTone.SETTLED_GREEN ->
                BrandedDocumentPalette.resolveStatusBadge(CommunicationStatus.PAID, bn)
        }

        val heroSection = ImageCardBodySection.HeroAmountSection(
            icon = if (currentDue > 0L) BrandedIconType.MONEY_BAG else BrandedIconType.CHECK_CIRCLE,
            label = when {
                currentDue > 0L -> if (bn) "বর্তমান মোট বকেয়া" else "Current Total Due"
                currentDue < 0L -> if (bn) "বর্তমান অগ্রিম ক্রেডিট" else "Current Advance Credit"
                else -> if (bn) "হিসাব সম্পূর্ণ পরিশোধিত" else "Account Settled"
            },
            amountText = BrandedPdfSpecBuilder.formatMoney(abs(currentDue), currency, spacedTaka = true),
            subtitle = if (currentDue > 0L && rem?.earliestDueDateMillis != null) {
                (if (bn) "নির্ধারিত তারিখ: " else "Due by: ") +
                    BrandedDocumentFormatter.formatDateTime(rem.earliestDueDateMillis, bn, includeTime = false, shortMonth = true)
            } else null,
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
            amountColor = BrandedDocumentPalette.textColorForTone(tone)
        )

        val summaryRows = buildList {
            if (rem != null) {
                val prior = rem.priorBalanceMinor
                add(
                    ImageCardMetricRowSpec(
                        icon = BrandedIconType.CALENDAR,
                        label = if (prior < 0L) (if (bn) "পূর্বের ক্রেডিট" else "Prior Credit") else (if (bn) "পূর্বের বকেয়া" else "Prior Balance"),
                        value = BrandedPdfSpecBuilder.formatMoney(abs(prior), currency),
                        valueColor = if (prior < 0L) BrandedDocumentPalette.CreditBlueText else BrandedDocumentPalette.TextPrimary
                    )
                )
                if (rem.latestInvoiceNumber != null && rem.latestInvoiceAmountMinor != null) {
                    add(
                        ImageCardMetricRowSpec(
                            icon = BrandedIconType.INVOICE_DOC,
                            label = if (bn) "সর্বশেষ ক্রয় (${rem.latestInvoiceNumber})" else "Latest Invoice (${rem.latestInvoiceNumber})",
                            value = BrandedPdfSpecBuilder.formatMoney(rem.latestInvoiceAmountMinor, currency)
                        )
                    )
                }
                if (rem.latestPaymentNumber != null && rem.latestPaymentAmountMinor != null) {
                    add(
                        ImageCardMetricRowSpec(
                            icon = BrandedIconType.COIN_TAKA,
                            label = if (bn) "সর্বশেষ পরিশোধ (${rem.latestPaymentNumber})" else "Latest Payment (${rem.latestPaymentNumber})",
                            value = "- ${BrandedPdfSpecBuilder.formatMoney(rem.latestPaymentAmountMinor, currency)}",
                            valueColor = BrandedDocumentPalette.PaymentGreenText,
                            boldValue = true
                        )
                    )
                }
                if (rem.otherNetAdjustmentMinor != 0L) {
                    val adj = rem.otherNetAdjustmentMinor
                    add(
                        ImageCardMetricRowSpec(
                            icon = BrandedIconType.RETURN_ARROW,
                            label = if (bn) "অন্যান্য সমন্বয়" else "Other Adjustments",
                            value = if (adj < 0L) "- ${BrandedPdfSpecBuilder.formatMoney(-adj, currency)}" else "+ ${BrandedPdfSpecBuilder.formatMoney(adj, currency)}",
                            valueColor = if (adj < 0L) BrandedDocumentPalette.CreditBlueText else BrandedDocumentPalette.TextPrimary
                        )
                    )
                }
            } else {
                add(
                    ImageCardMetricRowSpec(
                        icon = BrandedIconType.BALANCE_SCALE,
                        label = if (bn) "বর্তমান হিসাব" else "Account Balance",
                        value = BrandedPdfSpecBuilder.formatBalanceText(currentDue, bn, currency, spacedTaka = false)
                    )
                )
            }
        }

        val noticeSection = when {
            currentDue > 0L -> ImageCardBodySection.NoticeStripSection(
                icon = BrandedIconType.CALENDAR,
                text = if (bn) "অনুগ্রহ করে বকেয়া পরিশোধের জন্য আমাদের সঙ্গে দ্রুত যোগাযোগ করুন।" else "Please contact us or arrange payment at your convenience.",
                bgColor = BrandedDocumentPalette.DueHeroBoxBg,
                borderColor = BrandedDocumentPalette.DueRedBorder,
                iconAndTextColor = BrandedDocumentPalette.TextPrimary
            )
            currentDue < 0L -> ImageCardBodySection.NoticeStripSection(
                icon = BrandedIconType.CHECK_CIRCLE,
                text = if (bn) "আপনার হিসাবে অগ্রিম ক্রেডিট জমা রয়েছে; কোনো বকেয়া নেই।" else "Your account holds an advance credit balance with zero overdue due.",
                bgColor = BrandedDocumentPalette.CreditBlueBg,
                borderColor = BrandedDocumentPalette.CreditBlueBorder,
                iconAndTextColor = BrandedDocumentPalette.CreditBlueText
            )
            else -> ImageCardBodySection.NoticeStripSection(
                icon = BrandedIconType.CHECK_CIRCLE,
                text = if (bn) "আপনার হিসাব সম্পূর্ণ পরিশোধিত রয়েছে।" else "Your account is completely settled with zero balance due.",
                bgColor = BrandedDocumentPalette.PaymentGreenBg,
                borderColor = BrandedDocumentPalette.PaymentGreenBorder,
                iconAndTextColor = BrandedDocumentPalette.PaymentGreenText
            )
        }

        val sections = listOf(
            heroSection,
            ImageCardBodySection.MetricListCardSection(
                icon = BrandedIconType.CALENDAR,
                title = if (bn) "বকেয়ার সারসংক্ষেপ" else "Balance Summary",
                rows = summaryRows
            ),
            balancePillSection(currentDue, bn, currency),
            noticeSection
        )

        val introBn = when {
            currentDue > 0L -> "আপনার হিসাবে কিছু বকেয়া রয়েছে। অনুগ্রহ করে সুবিধামতো সময়ে পরিশোধ করে হিসাবটি আপডেট করুন।"
            currentDue < 0L -> "আপনার হিসাবে বর্তমানে কোনো বকেয়া নেই; অগ্রিম ক্রেডিট সংরক্ষিত রয়েছে।"
            else -> "আপনার বর্তমান হিসাবের সারসংক্ষেপ নিচে দেওয়া হলো।"
        }
        val introEn = when {
            currentDue > 0L -> "A gentle reminder regarding your outstanding balance. Please review and update your account at your convenience."
            currentDue < 0L -> "Please find your account balance summary below showing your advance credit."
            else -> "Please find your current settled account summary below."
        }

        return BrandedImageCardPageSpec(
            module = CommunicationModule.REMINDER,
            pageIndex = 1,
            totalPages = 1,
            safeFileName = pageFileName(stem, 1, 1),
            isBangla = bn,
            titleIcon = BrandedIconType.BELL_REMINDER,
            titlePrimary = if (currentDue > 0L) "বকেয়া পরিশোধের স্মরণিকা" else "হিসাবের অবস্থা",
            titleSecondary = if (currentDue > 0L) "Payment Reminder" else "Account Status",
            rightBadge = ImageCardRightBadgeSpec(statusBadge = statusBadge),
            greeting = salutationGreeting(document, bn, introBn, introEn),
            sections = sections,
            thankYouText = standardThankYou(bn),
            footerPhone = footerPhone(document),
            footerAddress = footerAddress(document)
        )
    }

    // endregion

    // region REF-05: Customer / Supplier Ledger Summary Card

    private fun buildStatementCard(document: SafeDocument, bn: Boolean): BrandedImageCardPageSpec {
        val stem = BrandedPdfSpecBuilder.safeFileStem(document)
        val stmt = document.statementBreakdown
        val currency = stmt?.currencyCode ?: (document.brand?.currencyCode ?: "BDT")
        val isSupplier = document.request.module == CommunicationModule.SUPPLIER_STATEMENT

        val periodLabel = if (stmt != null) {
            BrandedDocumentFormatter.formatDateRange(stmt.fromMillis, stmt.toMillis, stmt.isAllTime, isBangla = false)
        } else {
            document.fields["date_range"].orEmpty()
        }

        val opening = stmt?.openingBalanceMinor ?: 0L
        val sales = stmt?.totalSalesOrPurchasesMinor ?: 0L
        val returns = stmt?.totalReturnMinor ?: 0L
        val payments = stmt?.totalPaymentMinor ?: 0L
        val closing = stmt?.closingBalanceMinor ?: (document.balanceMinor ?: 0L)
        val otherNet = if (stmt != null) stmt.totalRefundPaidMinor + stmt.netAdjustmentMinor else 0L

        val metricRows = buildList {
            add(
                ImageCardMetricRowSpec(
                    icon = BrandedIconType.BANK_BUILDING,
                    label = if (opening < 0L) {
                        if (bn) "প্রারম্ভিক ক্রেডিট (Opening Credit)" else "Opening Credit"
                    } else {
                        if (bn) "প্রারম্ভিক বকেয়া (Opening Balance)" else "Opening Balance"
                    },
                    value = BrandedPdfSpecBuilder.formatMoney(abs(opening), currency),
                    valueColor = if (opening < 0L) BrandedDocumentPalette.CreditBlueText else BrandedDocumentPalette.TextPrimary,
                    boldValue = true
                )
            )
            add(
                ImageCardMetricRowSpec(
                    icon = BrandedIconType.SHOPPING_CART,
                    label = if (isSupplier) {
                        if (bn) "মোট ক্রয় (Total Purchases)" else "Total Purchases"
                    } else {
                        if (bn) "মোট ক্রয় (Total Sales)" else "Total Sales"
                    },
                    value = BrandedPdfSpecBuilder.formatMoney(sales, currency),
                    boldValue = true
                )
            )
            if (!isSupplier) {
                add(
                    ImageCardMetricRowSpec(
                        icon = BrandedIconType.RETURN_ARROW,
                        label = if (bn) "মোট ফেরত (Total Return)" else "Total Return",
                        value = BrandedPdfSpecBuilder.formatMoney(returns, currency),
                        valueColor = if (returns > 0L) BrandedDocumentPalette.CreditBlueText else BrandedDocumentPalette.TextPrimary,
                        boldValue = true
                    )
                )
            }
            add(
                ImageCardMetricRowSpec(
                    icon = BrandedIconType.PAYMENT_CARD,
                    label = if (bn) "মোট পরিশোধ (Total Payment)" else "Total Payment",
                    value = BrandedPdfSpecBuilder.formatMoney(payments, currency),
                    valueColor = BrandedDocumentPalette.PaymentGreenText,
                    boldValue = true
                )
            )
            if (otherNet != 0L) {
                add(
                    ImageCardMetricRowSpec(
                        icon = BrandedIconType.CALCULATOR,
                        label = if (bn) "রিফান্ড / সমন্বয়" else "Refunds / Adjustments",
                        value = if (otherNet < 0L) "- ${BrandedPdfSpecBuilder.formatMoney(-otherNet, currency)}" else "+ ${BrandedPdfSpecBuilder.formatMoney(otherNet, currency)}"
                    )
                )
            }
        }

        val closingPill = when {
            closing < 0L -> ImageCardBodySection.PurpleTotalPillSection(
                icon = BrandedIconType.BALANCE_SCALE,
                label = if (bn) "বর্তমান ক্রেডিট (Credit Balance)" else "Closing Advance Credit",
                amountText = BrandedPdfSpecBuilder.formatMoney(-closing, currency)
            )
            else -> ImageCardBodySection.PurpleTotalPillSection(
                icon = BrandedIconType.BALANCE_SCALE,
                label = if (bn) "বর্তমান ব্যালেন্স (Closing Balance)" else "Closing Balance",
                amountText = BrandedPdfSpecBuilder.formatMoney(closing, currency)
            )
        }

        val sections = listOf(
            ImageCardBodySection.MetricListCardSection(
                icon = null,
                title = null,
                rows = metricRows
            ),
            closingPill,
            ImageCardBodySection.NoticeStripSection(
                icon = BrandedIconType.INFO_CIRCLE,
                text = if (bn) "বিস্তারিত লেনদেনের জন্য সংযুক্ত স্টেটমেন্ট (PDF) দেখুন বা আমাদের সঙ্গে যোগাযোগ করুন।" else "See the attached PDF statement for full transaction history or contact us."
            )
        )

        return BrandedImageCardPageSpec(
            module = document.request.module,
            pageIndex = 1,
            totalPages = 1,
            safeFileName = pageFileName(stem, 1, 1),
            isBangla = bn,
            titleIcon = BrandedIconType.LEDGER_BOOK,
            titlePrimary = if (isSupplier) (if (bn) "সরবরাহকারী হিসাব বিবরণী" else "Supplier Statement") else "গ্রাহক হিসাব বিবরণী",
            titleSecondary = if (isSupplier) "Supplier Ledger Summary" else "Customer Ledger Summary",
            rightBadge = ImageCardRightBadgeSpec(
                topChipText = if (bn) "হিসাবের সময়কাল" else "Statement Period",
                topChipIcon = BrandedIconType.CALENDAR,
                bottomSubText = periodLabel
            ),
            greeting = if (isSupplier) {
                supplierGreeting(
                    document,
                    bn,
                    introBn = "নির্বাচিত সময়ের আপনার সরবরাহকারী হিসাবের সারসংক্ষেপ নিচে দেওয়া হলো।",
                    introEn = "Please find your supplier account summary for the selected period below."
                )
            } else {
                salutationGreeting(
                    document,
                    bn,
                    introBn = "নির্বাচিত সময়ের আপনার হিসাবের সারসংক্ষেপ নিচে দেওয়া হলো।",
                    introEn = "Please find your account ledger summary for the selected period below."
                )
            },
            sections = sections,
            thankYouText = standardThankYou(bn),
            footerPhone = footerPhone(document),
            footerAddress = footerAddress(document)
        )
    }

    // endregion

    // region Extended Module Cards (Delivery, Return, Exchange, Quotation, Sales Order, PO, Packing)

    private fun buildDeliveryPages(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean
    ): List<BrandedImageCardPageSpec> {
        val stem = BrandedPdfSpecBuilder.safeFileStem(document)
        val del = document.deliveryBreakdown
        val allRows = mapLineItems(document, settings, bn, includePrices = false)
        val chunks = if (allRows.isEmpty()) listOf(emptyList()) else allRows.chunked(MAX_ITEMS_PER_IMAGE_CARD)
        val totalPages = chunks.size
        val totalQty = del?.totalDispatchedQuantity ?: document.lineItems.sumOf { it.quantity }

        return chunks.mapIndexed { idx, chunk ->
            val pageNum = idx + 1
            val isFirst = pageNum == 1
            val isLast = pageNum == totalPages
            val sections = buildList<ImageCardBodySection> {
                if (chunk.isNotEmpty()) {
                    add(
                        ImageCardBodySection.ItemsListSection(
                            icon = BrandedIconType.DELIVERY_TRUCK,
                            title = if (bn) "প্রেরিত পণ্যের বিবরণ" else "Dispatched Items",
                            subtitleRight = if (totalPages > 1) "$pageNum/$totalPages" else null,
                            items = chunk
                        )
                    )
                }
                if (isLast) {
                    add(
                        ImageCardBodySection.MetricListCardSection(
                            icon = BrandedIconType.DELIVERY_TRUCK,
                            title = if (bn) "ডেলিভারির তথ্য" else "Dispatch Details",
                            rows = buildList {
                                del?.invoiceNumber?.takeIf { it.isNotBlank() }?.let { inv ->
                                    add(ImageCardMetricRowSpec(label = if (bn) "ইনভয়েস নং" else "Invoice Ref", value = inv, boldValue = true))
                                }
                                del?.deliveryPerson?.takeIf { it.isNotBlank() }?.let { dp ->
                                    add(ImageCardMetricRowSpec(label = if (bn) "ডেলিভারি প্রতিনিধি" else "Delivery Person", value = dp))
                                }
                                del?.transportInfo?.takeIf { it.isNotBlank() }?.let { tr ->
                                    add(ImageCardMetricRowSpec(label = if (bn) "পরিবহন" else "Transport", value = tr))
                                }
                            }
                        )
                    )
                    add(
                        ImageCardBodySection.PurpleTotalPillSection(
                            icon = BrandedIconType.DELIVERY_TRUCK,
                            label = if (bn) "সর্বমোট প্রেরিত পরিমাণ" else "Total Dispatched",
                            amountText = "$totalQty PCS"
                        )
                    )
                    add(
                        ImageCardBodySection.NoticeStripSection(
                            icon = BrandedIconType.INFO_CIRCLE,
                            text = if (bn) "বিস্তারিত চালান দেখতে সংযুক্ত PDF দেখুন।" else "See the attached PDF challan for full dispatch details."
                        )
                    )
                }
            }
            BrandedImageCardPageSpec(
                module = CommunicationModule.DELIVERY,
                pageIndex = pageNum,
                totalPages = totalPages,
                safeFileName = pageFileName(stem, pageNum, totalPages),
                isBangla = bn,
                titleIcon = BrandedIconType.DELIVERY_TRUCK,
                titlePrimary = "ডেলিভারি চালান",
                titleSecondary = "Delivery Challan",
                rightBadge = ImageCardRightBadgeSpec(
                    topChipText = document.number,
                    statusBadge = BrandedDocumentPalette.StatusBadgeSpec(
                        text = if (bn) "প্রেরিত (DISPATCHED)" else "DISPATCHED",
                        textColor = BrandedDocumentPalette.PaymentGreenText,
                        bgColor = BrandedDocumentPalette.PaymentGreenBg,
                        borderColor = BrandedDocumentPalette.PaymentGreenBorder,
                        isCheckIcon = true,
                        isAlertIcon = false
                    )
                ),
                greeting = if (isFirst) {
                    salutationGreeting(
                        document,
                        bn,
                        introBn = "আপনার ডেলিভারি চালান ও প্রেরিত পণ্যের তালিকা নিচে দেওয়া হলো।",
                        introEn = "Please find your delivery challan and dispatched items below."
                    )
                } else null,
                sections = sections,
                thankYouText = if (isLast) standardThankYou(bn) else null,
                footerPhone = footerPhone(document),
                footerAddress = footerAddress(document)
            )
        }
    }

    private fun buildReturnPages(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean
    ): List<BrandedImageCardPageSpec> {
        val stem = BrandedPdfSpecBuilder.safeFileStem(document)
        val ret = document.returnExchangeBreakdown
        val currency = document.brand?.currencyCode ?: "BDT"
        val allRows = mapLineItems(document, settings, bn, includePrices = true)
        val chunks = if (allRows.isEmpty()) listOf(emptyList()) else allRows.chunked(MAX_ITEMS_PER_IMAGE_CARD)
        val totalPages = chunks.size
        val bal = ret?.currentAccountBalanceMinor ?: (document.balanceMinor ?: 0L)

        return chunks.mapIndexed { idx, chunk ->
            val pageNum = idx + 1
            val isFirst = pageNum == 1
            val isLast = pageNum == totalPages
            val sections = buildList<ImageCardBodySection> {
                if (chunk.isNotEmpty()) {
                    add(
                        ImageCardBodySection.ItemsListSection(
                            icon = BrandedIconType.RETURN_ARROW,
                            title = if (bn) "ফেরতকৃত পণ্য" else "Returned Items",
                            items = chunk
                        )
                    )
                }
                if (isLast) {
                    add(
                        ImageCardBodySection.MetricListCardSection(
                            icon = BrandedIconType.CALCULATOR,
                            title = if (bn) "ফেরতের হিসাব" else "Return Summary",
                            rows = listOf(
                                ImageCardMetricRowSpec(
                                    label = if (bn) "মোট ফেরত মূল্য" else "Total Return Value",
                                    value = BrandedPdfSpecBuilder.formatMoney(ret?.returnAmountMinor ?: 0L, currency),
                                    boldValue = true
                                ),
                                ImageCardMetricRowSpec(
                                    label = if (bn) "হিসাবে ক্রেডিট" else "Credited to Account",
                                    value = BrandedPdfSpecBuilder.formatMoney(ret?.creditedToAccountMinor ?: 0L, currency),
                                    valueColor = BrandedDocumentPalette.CreditBlueText
                                ),
                                ImageCardMetricRowSpec(
                                    label = if (bn) "নগদ/ব্যাংক রিফান্ড" else "Cash / Bank Refund",
                                    value = BrandedPdfSpecBuilder.formatMoney(ret?.cashOrBankRefundMinor ?: 0L, currency),
                                    valueColor = BrandedDocumentPalette.PaymentGreenText
                                )
                            )
                        )
                    )
                    add(balancePillSection(bal, bn, currency))
                    add(
                        ImageCardBodySection.NoticeStripSection(
                            icon = BrandedIconType.CHECK_CIRCLE,
                            text = if (bn) "পণ্য ফেরত ও হিসাব সমন্বয় সম্পন্ন হয়েছে।" else "Sales return and account adjustment have been completed."
                        )
                    )
                }
            }
            BrandedImageCardPageSpec(
                module = CommunicationModule.RETURN,
                pageIndex = pageNum,
                totalPages = totalPages,
                safeFileName = pageFileName(stem, pageNum, totalPages),
                isBangla = bn,
                titleIcon = BrandedIconType.RETURN_ARROW,
                titlePrimary = "বিক্রয় ফেরত",
                titleSecondary = "Sales Return",
                rightBadge = ImageCardRightBadgeSpec(topChipText = document.number),
                greeting = if (isFirst) {
                    salutationGreeting(
                        document,
                        bn,
                        introBn = "আপনার বিক্রয় ফেরত (রিটার্ন) ও সমন্বয়ের বিবরণ নিচে দেওয়া হলো।",
                        introEn = "Please find your sales return and adjustment details below."
                    )
                } else null,
                sections = sections,
                thankYouText = if (isLast) standardThankYou(bn) else null,
                footerPhone = footerPhone(document),
                footerAddress = footerAddress(document)
            )
        }
    }

    private fun buildExchangePages(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean
    ): List<BrandedImageCardPageSpec> {
        val stem = BrandedPdfSpecBuilder.safeFileStem(document)
        val ex = document.returnExchangeBreakdown
        val currency = document.brand?.currencyCode ?: "BDT"
        val allRows = mapLineItems(document, settings, bn, includePrices = true, includeDirectionTag = true)
        val chunks = if (allRows.isEmpty()) listOf(emptyList()) else allRows.chunked(MAX_ITEMS_PER_IMAGE_CARD)
        val totalPages = chunks.size
        val diff = ex?.netPriceDifferenceMinor ?: 0L
        val bal = ex?.currentAccountBalanceMinor ?: (document.balanceMinor ?: 0L)

        return chunks.mapIndexed { idx, chunk ->
            val pageNum = idx + 1
            val isFirst = pageNum == 1
            val isLast = pageNum == totalPages
            val sections = buildList<ImageCardBodySection> {
                if (chunk.isNotEmpty()) {
                    add(
                        ImageCardBodySection.ItemsListSection(
                            icon = BrandedIconType.EXCHANGE_ARROWS,
                            title = if (bn) "ফেরত ও বদলি পণ্য" else "Exchanged Items",
                            items = chunk
                        )
                    )
                }
                if (isLast) {
                    add(
                        ImageCardBodySection.MetricListCardSection(
                            icon = BrandedIconType.CALCULATOR,
                            title = if (bn) "বিনিময়ের হিসাব" else "Exchange Summary",
                            rows = buildList {
                                add(
                                    ImageCardMetricRowSpec(
                                        label = if (bn) "মূল্যের পার্থক্য" else "Price Difference",
                                        value = when {
                                            diff > 0L -> "+ ${BrandedPdfSpecBuilder.formatMoney(diff, currency)}"
                                            diff < 0L -> "- ${BrandedPdfSpecBuilder.formatMoney(-diff, currency)}"
                                            else -> BrandedPdfSpecBuilder.formatMoney(0L, currency)
                                        },
                                        valueColor = if (diff > 0L) BrandedDocumentPalette.DueRedText else BrandedDocumentPalette.PaymentGreenText,
                                        boldValue = true
                                    )
                                )
                                ex?.refundOrPaymentMethod?.takeIf { it.isNotBlank() }?.let { method ->
                                    add(ImageCardMetricRowSpec(label = if (bn) "সমন্বয় মাধ্যম" else "Method", value = method))
                                }
                            }
                        )
                    )
                    add(balancePillSection(bal, bn, currency))
                    add(
                        ImageCardBodySection.NoticeStripSection(
                            icon = BrandedIconType.CHECK_CIRCLE,
                            text = if (bn) "পণ্য বিনিময় ও হিসাব সমন্বয় সম্পন্ন হয়েছে।" else "Product exchange details have been updated in your account."
                        )
                    )
                }
            }
            BrandedImageCardPageSpec(
                module = CommunicationModule.EXCHANGE,
                pageIndex = pageNum,
                totalPages = totalPages,
                safeFileName = pageFileName(stem, pageNum, totalPages),
                isBangla = bn,
                titleIcon = BrandedIconType.EXCHANGE_ARROWS,
                titlePrimary = "পণ্য বিনিময়",
                titleSecondary = "Product Exchange",
                rightBadge = ImageCardRightBadgeSpec(topChipText = document.number),
                greeting = if (isFirst) {
                    salutationGreeting(
                        document,
                        bn,
                        introBn = "আপনার পণ্য বিনিময়ের (এক্সচেঞ্জ) বিবরণ নিচে দেওয়া হলো।",
                        introEn = "Please review your product exchange summary below."
                    )
                } else null,
                sections = sections,
                thankYouText = if (isLast) standardThankYou(bn) else null,
                footerPhone = footerPhone(document),
                footerAddress = footerAddress(document)
            )
        }
    }

    private fun buildQuotationOrOrderPages(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean,
        isQuotation: Boolean
    ): List<BrandedImageCardPageSpec> {
        val stem = BrandedPdfSpecBuilder.safeFileStem(document)
        val currency = document.brand?.currencyCode ?: "BDT"
        val allRows = mapLineItems(document, settings, bn, includePrices = true)
        val chunks = if (allRows.isEmpty()) listOf(emptyList()) else allRows.chunked(MAX_ITEMS_PER_IMAGE_CARD)
        val totalPages = chunks.size
        val amountStr = document.fields["document_amount"]?.takeIf { it.isNotBlank() }
            ?: BrandedPdfSpecBuilder.formatMoney(document.lineItems.sumOf { it.lineTotalMinor }, currency)

        return chunks.mapIndexed { idx, chunk ->
            val pageNum = idx + 1
            val isFirst = pageNum == 1
            val isLast = pageNum == totalPages
            val sections = buildList<ImageCardBodySection> {
                if (chunk.isNotEmpty()) {
                    add(
                        ImageCardBodySection.ItemsListSection(
                            icon = BrandedIconType.SHOPPING_CART,
                            title = if (bn) "পণ্যের বিবরণ" else "Item Details",
                            items = chunk
                        )
                    )
                }
                if (isLast) {
                    add(
                        ImageCardBodySection.MetricListCardSection(
                            icon = BrandedIconType.INVOICE_DOC,
                            title = if (isQuotation) {
                                if (bn) "কোটেশনের তথ্য" else "Quotation Summary"
                            } else {
                                if (bn) "অর্ডারের তথ্য" else "Order Summary"
                            },
                            rows = buildList {
                                document.validUntilMillis?.let { vu ->
                                    add(
                                        ImageCardMetricRowSpec(
                                            label = if (bn) "মেয়াদ শেষ" else "Valid Until",
                                            value = BrandedDocumentFormatter.formatDateTime(vu, bn, includeTime = false, shortMonth = true),
                                            boldValue = true
                                        )
                                    )
                                }
                                document.scheduledDeliveryInfo?.takeIf { it.isNotBlank() }?.let { di ->
                                    add(ImageCardMetricRowSpec(label = if (bn) "ডেলিভারির তথ্য" else "Delivery Info", value = di))
                                }
                                add(
                                    ImageCardMetricRowSpec(
                                        label = if (bn) "মোট আইটেম" else "Total Items",
                                        value = "${document.lineItems.size}"
                                    )
                                )
                            }
                        )
                    )
                    add(
                        ImageCardBodySection.PurpleTotalPillSection(
                            icon = BrandedIconType.INVOICE_DOC,
                            label = if (bn) "মোট প্রাক্কলিত মূল্য" else "Total Document Value",
                            amountText = amountStr
                        )
                    )
                }
            }
            BrandedImageCardPageSpec(
                module = document.request.module,
                pageIndex = pageNum,
                totalPages = totalPages,
                safeFileName = pageFileName(stem, pageNum, totalPages),
                isBangla = bn,
                titleIcon = BrandedIconType.INVOICE_DOC,
                titlePrimary = if (isQuotation) "কোটেশন" else "বিক্রয় আদেশ",
                titleSecondary = if (isQuotation) "Quotation" else "Sales Order",
                rightBadge = ImageCardRightBadgeSpec(topChipText = document.number),
                greeting = if (isFirst) {
                    salutationGreeting(
                        document,
                        bn,
                        introBn = if (isQuotation) "আপনার জন্য প্রস্তুতকৃত কোটেশনের বিবরণ নিচে দেওয়া হলো।" else "আপনার বিক্রয় আদেশের বিবরণ নিচে দেওয়া হলো।",
                        introEn = if (isQuotation) "Please find the quotation prepared for you below." else "Please review your confirmed sales order details below."
                    )
                } else null,
                sections = sections,
                thankYouText = if (isLast) standardThankYou(bn) else null,
                footerPhone = footerPhone(document),
                footerAddress = footerAddress(document)
            )
        }
    }

    private fun buildPurchaseOrderPages(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean
    ): List<BrandedImageCardPageSpec> {
        val stem = BrandedPdfSpecBuilder.safeFileStem(document)
        val poCurrency = document.lineItems.firstOrNull()?.currencyCode ?: "BDT"
        val allRows = mapLineItems(document, settings, bn, includePrices = true)
        val chunks = if (allRows.isEmpty()) listOf(emptyList()) else allRows.chunked(MAX_ITEMS_PER_IMAGE_CARD)
        val totalPages = chunks.size
        val poTotal = BrandedPdfSpecBuilder.formatMoney(document.lineItems.sumOf { it.lineTotalMinor }, poCurrency)

        return chunks.mapIndexed { idx, chunk ->
            val pageNum = idx + 1
            val isFirst = pageNum == 1
            val isLast = pageNum == totalPages
            val sections = buildList<ImageCardBodySection> {
                if (chunk.isNotEmpty()) {
                    add(
                        ImageCardBodySection.ItemsListSection(
                            icon = BrandedIconType.SHOPPING_CART,
                            title = if (bn) "অর্ডারকৃত পণ্য" else "Ordered Items",
                            items = chunk
                        )
                    )
                }
                if (isLast) {
                    add(
                        ImageCardBodySection.PurpleTotalPillSection(
                            icon = BrandedIconType.INVOICE_DOC,
                            label = if (bn) "মোট ক্রয় আদেশ মূল্য" else "Total Purchase Order",
                            amountText = poTotal
                        )
                    )
                }
            }
            BrandedImageCardPageSpec(
                module = CommunicationModule.PURCHASE_ORDER,
                pageIndex = pageNum,
                totalPages = totalPages,
                safeFileName = pageFileName(stem, pageNum, totalPages),
                isBangla = bn,
                titleIcon = BrandedIconType.INVOICE_DOC,
                titlePrimary = "ক্রয় আদেশ",
                titleSecondary = "Purchase Order",
                rightBadge = ImageCardRightBadgeSpec(topChipText = document.number),
                greeting = if (isFirst) {
                    supplierGreeting(
                        document,
                        bn,
                        introBn = "আমাদের ক্রয় আদেশের সারসংক্ষেপ নিচে দেওয়া হলো।",
                        introEn = "Please review the summary of our purchase order below."
                    )
                } else null,
                sections = sections,
                thankYouText = if (isLast) standardThankYou(bn) else null,
                footerPhone = footerPhone(document),
                footerAddress = footerAddress(document)
            )
        }
    }

    private fun buildPackingPages(
        document: SafeDocument,
        settings: CommunicationSettings,
        bn: Boolean
    ): List<BrandedImageCardPageSpec> {
        val stem = BrandedPdfSpecBuilder.safeFileStem(document)
        val allRows = mapLineItems(document, settings, bn, includePrices = false)
        val chunks = if (allRows.isEmpty()) listOf(emptyList()) else allRows.chunked(MAX_ITEMS_PER_IMAGE_CARD)
        val totalPages = chunks.size

        return chunks.mapIndexed { idx, chunk ->
            val pageNum = idx + 1
            val isFirst = pageNum == 1
            val isLast = pageNum == totalPages
            val sections = buildList<ImageCardBodySection> {
                if (chunk.isNotEmpty()) {
                    add(
                        ImageCardBodySection.ItemsListSection(
                            icon = BrandedIconType.DELIVERY_TRUCK,
                            title = if (bn) "প্যাকিং ও কার্টনের বিবরণ" else "Packing Breakdown",
                            items = chunk
                        )
                    )
                }
                if (isLast) {
                    add(
                        ImageCardBodySection.MetricListCardSection(
                            icon = BrandedIconType.CALCULATOR,
                            title = if (bn) "কার্টন ও ওজন" else "Carton & Weight Summary",
                            rows = buildList {
                                add(ImageCardMetricRowSpec(label = if (bn) "মোট পরিমাণ" else "Total Units", value = document.fields["total_quantity"] ?: "0"))
                                add(ImageCardMetricRowSpec(label = if (bn) "মোট কার্টন" else "Total Cartons", value = document.fields["total_carton"] ?: "0", boldValue = true))
                                document.fields["gross_weight"]?.takeIf { it.isNotBlank() }?.let { w ->
                                    add(ImageCardMetricRowSpec(label = if (bn) "মোট ওজন (কেজি)" else "Gross Weight (kg)", value = w))
                                }
                                document.fields["total_cbm"]?.takeIf { it.isNotBlank() }?.let { c ->
                                    add(ImageCardMetricRowSpec(label = if (bn) "মোট সিবিএম" else "Total CBM", value = c))
                                }
                            }
                        )
                    )
                    add(
                        ImageCardBodySection.PurpleTotalPillSection(
                            icon = BrandedIconType.DELIVERY_TRUCK,
                            label = if (bn) "মোট কার্টন" else "Total Cartons",
                            amountText = "${document.fields["total_carton"] ?: "0"} CTN"
                        )
                    )
                }
            }
            BrandedImageCardPageSpec(
                module = CommunicationModule.PACKING,
                pageIndex = pageNum,
                totalPages = totalPages,
                safeFileName = pageFileName(stem, pageNum, totalPages),
                isBangla = bn,
                titleIcon = BrandedIconType.DELIVERY_TRUCK,
                titlePrimary = "প্যাকিং তালিকা",
                titleSecondary = "Packing List",
                rightBadge = ImageCardRightBadgeSpec(topChipText = document.number),
                greeting = if (isFirst) {
                    supplierGreeting(
                        document,
                        bn,
                        introBn = "প্যাকিং ও শিপমেন্ট কার্টনের বিবরণ নিচে দেওয়া হলো।",
                        introEn = "Please review the packing breakdown and carton summary below."
                    )
                } else null,
                sections = sections,
                thankYouText = if (isLast) standardThankYou(bn) else null,
                footerPhone = footerPhone(document),
                footerAddress = footerAddress(document)
            )
        }
    }

    private fun balancePillSection(
        balanceMinor: Long,
        bn: Boolean,
        currency: String,
        preferGrandTotal: Boolean = false,
        isSupplier: Boolean = false
    ): ImageCardBodySection.PurpleTotalPillSection {
        return when {
            balanceMinor > 0L -> ImageCardBodySection.PurpleTotalPillSection(
                icon = if (preferGrandTotal) BrandedIconType.MONEY_BAG else BrandedIconType.BALANCE_SCALE,
                label = when {
                    isSupplier && bn -> "মোট প্রদেয় বকেয়া"
                    isSupplier -> "Total Payable"
                    preferGrandTotal && bn -> "সর্বমোট বকেয়া"
                    preferGrandTotal -> "Total Outstanding"
                    bn -> "বর্তমান বকেয়া"
                    else -> "Current Balance"
                },
                amountText = BrandedPdfSpecBuilder.formatMoney(balanceMinor, currency, spacedTaka = true)
            )
            balanceMinor < 0L -> ImageCardBodySection.PurpleTotalPillSection(
                icon = BrandedIconType.BALANCE_SCALE,
                label = if (bn) "বর্তমান অগ্রিম ক্রেডিট" else "Advance Credit",
                amountText = BrandedPdfSpecBuilder.formatMoney(-balanceMinor, currency, spacedTaka = true)
            )
            else -> ImageCardBodySection.PurpleTotalPillSection(
                icon = BrandedIconType.CHECK_CIRCLE,
                label = if (preferGrandTotal && bn) "সর্বমোট বকেয়া" else if (bn) "বর্তমান ব্যালেন্স" else "Current Balance",
                amountText = BrandedPdfSpecBuilder.formatMoney(0L, currency, spacedTaka = true)
            )
        }
    }
}
