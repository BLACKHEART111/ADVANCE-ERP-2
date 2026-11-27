package com.pocketdev.advance_erp.core.communication.graphics

import com.pocketdev.advance_erp.core.communication.CommunicationStatus

/**
 * Shared light-branded document export color palette and semantic financial tokens.
 * Used identically across A4 PDF documents and HD PNG Image Cards.
 */
object BrandedDocumentPalette {
    // Brand Identity (Purple + Teal Wave)
    val BrandPurpleDeep = 0xFF3B0E68.toInt()
    val BrandPurplePrimary = 0xFF6A1B9A.toInt()
    val BrandPurpleBright = 0xFF8E24AA.toInt()
    val BrandTealAccent = 0xFF299DB2.toInt()
    val BrandTealBright = 0xFF4DD0E1.toInt()
    val BrandLavenderWave = 0xFFD1B3FF.toInt()

    // Document Surfaces & Cards
    val PageWhite = 0xFFFFFFFF.toInt()
    val CardOuterBackdrop = 0xFFF5F0FA.toInt()
    val SectionBoxBg = 0xFFF7F4FC.toInt()
    val SectionBoxAltBg = 0xFFF1EBF9.toInt()
    val SectionBoxBorder = 0xFFE5DBF3.toInt()
    val TableHeaderStart = 0xFF5B168C.toInt()
    val TableHeaderEnd = 0xFF8A2BE2.toInt()
    val TableRowAltBg = 0xFFFAF8FD.toInt()
    val DividerSubtle = 0xFFE8E2F2.toInt()

    // Semantic Financial Colors
    val PaymentGreenText = 0xFF15803D.toInt()
    val PaymentGreenBg = 0xFFDCFCE7.toInt()
    val PaymentGreenBorder = 0xFF86EFAC.toInt()

    val DueRedText = 0xFFC62828.toInt()
    val DueRedBg = 0xFFFEE2E2.toInt()
    val DueRedBorder = 0xFFFCA5A5.toInt()
    val DueHeroBoxBg = 0xFFFCE8EE.toInt()

    val CreditBlueText = 0xFF1D4ED8.toInt()
    val CreditBlueBg = 0xFFDBEAFE.toInt()
    val CreditBlueBorder = 0xFF93C5FD.toInt()

    val PartialAmberText = 0xFFB45309.toInt()
    val PartialAmberBg = 0xFFFEF3C7.toInt()
    val PartialAmberBorder = 0xFFFCD34D.toInt()

    // Typography Colors
    val TextPrimary = 0xFF1E1B2E.toInt()
    val TextSecondary = 0xFF4B445C.toInt()
    val TextMuted = 0xFF6E6782.toInt()
    val TextOnPurple = 0xFFFFFFFF.toInt()

    enum class BalanceSemanticTone {
        DUE_RED,
        CREDIT_BLUE,
        SETTLED_GREEN
    }

    data class StatusBadgeSpec(
        val text: String,
        val textColor: Int,
        val bgColor: Int,
        val borderColor: Int,
        val isCheckIcon: Boolean,
        val isAlertIcon: Boolean
    )

    fun resolveBalanceTone(minor: Long): BalanceSemanticTone = when {
        minor > 0L -> BalanceSemanticTone.DUE_RED
        minor < 0L -> BalanceSemanticTone.CREDIT_BLUE
        else -> BalanceSemanticTone.SETTLED_GREEN
    }

    fun textColorForTone(tone: BalanceSemanticTone): Int = when (tone) {
        BalanceSemanticTone.DUE_RED -> DueRedText
        BalanceSemanticTone.CREDIT_BLUE -> CreditBlueText
        BalanceSemanticTone.SETTLED_GREEN -> PaymentGreenText
    }

    fun resolveStatusBadge(status: CommunicationStatus, isBangla: Boolean): StatusBadgeSpec = when (status) {
        CommunicationStatus.UNPAID -> StatusBadgeSpec(
            text = if (isBangla) "বকেয়া (UNPAID)" else "UNPAID",
            textColor = DueRedText,
            bgColor = DueRedBg,
            borderColor = DueRedBorder,
            isCheckIcon = false,
            isAlertIcon = true
        )
        CommunicationStatus.OVERDUE -> StatusBadgeSpec(
            text = if (isBangla) "মেয়াদোত্তীর্ণ (OVERDUE)" else "OVERDUE",
            textColor = DueRedText,
            bgColor = DueRedBg,
            borderColor = DueRedBorder,
            isCheckIcon = false,
            isAlertIcon = true
        )
        CommunicationStatus.PAID -> StatusBadgeSpec(
            text = if (isBangla) "পরিশোধিত (PAID)" else "PAID",
            textColor = PaymentGreenText,
            bgColor = PaymentGreenBg,
            borderColor = PaymentGreenBorder,
            isCheckIcon = true,
            isAlertIcon = false
        )
        CommunicationStatus.PARTIAL -> StatusBadgeSpec(
            text = if (isBangla) "আংশিক পরিশোধিত" else "PARTIALLY PAID",
            textColor = PartialAmberText,
            bgColor = PartialAmberBg,
            borderColor = PartialAmberBorder,
            isCheckIcon = false,
            isAlertIcon = false
        )
        CommunicationStatus.SETTLED -> StatusBadgeSpec(
            text = if (isBangla) "নিষ্পত্তিকৃত" else "SETTLED",
            textColor = PaymentGreenText,
            bgColor = PaymentGreenBg,
            borderColor = PaymentGreenBorder,
            isCheckIcon = true,
            isAlertIcon = false
        )
        CommunicationStatus.CREDIT -> StatusBadgeSpec(
            text = if (isBangla) "অগ্রিম ক্রেডিট" else "ADVANCE CREDIT",
            textColor = CreditBlueText,
            bgColor = CreditBlueBg,
            borderColor = CreditBlueBorder,
            isCheckIcon = false,
            isAlertIcon = false
        )
        CommunicationStatus.PREVIOUS_DUE -> StatusBadgeSpec(
            text = if (isBangla) "বকেয়া আছে" else "DUE BALANCE",
            textColor = DueRedText,
            bgColor = DueRedBg,
            borderColor = DueRedBorder,
            isCheckIcon = false,
            isAlertIcon = true
        )
        CommunicationStatus.GENERAL -> StatusBadgeSpec(
            text = if (isBangla) "সাধারণ" else "GENERAL",
            textColor = BrandPurplePrimary,
            bgColor = SectionBoxAltBg,
            borderColor = SectionBoxBorder,
            isCheckIcon = false,
            isAlertIcon = false
        )
    }
}
