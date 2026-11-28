package com.pocketdev.advance_erp.core.communication

/** Active preview tab inside the 3-tab Share Preview screen. */
enum class SharePreviewTab {
    PDF,
    IMAGE,
    TEXT
}

/** Concrete Android share action executed in sequence when sharing one or multiple formats. */
enum class ShareStepKind {
    SEND_TEXT,
    SEND_DOCUMENT,
    SEND_DOCUMENT_WITH_CAPTION,
    SEND_IMAGE_CARDS,
    SEND_IMAGE_WITH_CAPTION
}

/** Result of toggling a format checkbox in the Share Document bottom sheet. */
data class FormatToggleOutcome(
    val selectedFormats: Set<ShareOutputFormat>,
    val blockedLastUncheck: Boolean
)

/** Single actionable step in a sequential WhatsApp or system share handoff flow. */
data class PlannedShareStep(
    val stepIndex: Int,
    val totalSteps: Int,
    val kind: ShareStepKind,
    val format: ShareOutputFormat,
    val titleEn: String,
    val titleBn: String,
    val subtitleEn: String,
    val subtitleBn: String
)

/**
 * Pure-JVM planner resolving format checkboxes, preview default tabs, and sequential WhatsApp
 * multi-format handoff steps so no selected format or caption is silently dropped.
 */
object ShareWorkflowPlanner {

    fun toggleFormatSelection(
        current: Set<ShareOutputFormat>,
        target: ShareOutputFormat
    ): FormatToggleOutcome {
        return if (target in current) {
            if (current.size <= 1) {
                FormatToggleOutcome(selectedFormats = current, blockedLastUncheck = true)
            } else {
                FormatToggleOutcome(selectedFormats = current - target, blockedLastUncheck = false)
            }
        } else {
            FormatToggleOutcome(selectedFormats = current + target, blockedLastUncheck = false)
        }
    }

    fun resolveInitialPreviewTab(selectedFormats: Set<ShareOutputFormat>): SharePreviewTab = when {
        ShareOutputFormat.IMAGE_CARD in selectedFormats && ShareOutputFormat.PDF !in selectedFormats -> SharePreviewTab.IMAGE
        ShareOutputFormat.PDF in selectedFormats -> SharePreviewTab.PDF
        ShareOutputFormat.IMAGE_CARD in selectedFormats -> SharePreviewTab.IMAGE
        else -> SharePreviewTab.TEXT
    }

    fun buildShareSteps(
        selectedFormats: Set<ShareOutputFormat>,
        imagePageCount: Int = 1,
        documentExtension: String = "pdf"
    ): List<PlannedShareStep> {
        require(selectedFormats.isNotEmpty()) {
            "Select at least one share format / কমপক্ষে একটি শেয়ার ফরম্যাট নির্বাচন করুন"
        }
        val docLabelUpper = if (documentExtension.equals("xlsx", ignoreCase = true)) "XLSX" else "PDF"
        val hasPdf = ShareOutputFormat.PDF in selectedFormats
        val hasImage = ShareOutputFormat.IMAGE_CARD in selectedFormats
        val hasText = ShareOutputFormat.TEXT_MESSAGE in selectedFormats

        val raw = mutableListOf<ShareStepKind>()
        when {
            hasPdf && hasImage && hasText -> {
                raw += ShareStepKind.SEND_DOCUMENT_WITH_CAPTION
                raw += ShareStepKind.SEND_IMAGE_CARDS
            }
            hasPdf && hasText -> {
                raw += ShareStepKind.SEND_DOCUMENT_WITH_CAPTION
            }
            hasPdf && hasImage -> {
                raw += ShareStepKind.SEND_DOCUMENT
                raw += ShareStepKind.SEND_IMAGE_CARDS
            }
            hasImage && hasText -> {
                if (imagePageCount <= 1) {
                    raw += ShareStepKind.SEND_IMAGE_WITH_CAPTION
                } else {
                    raw += ShareStepKind.SEND_IMAGE_CARDS
                    raw += ShareStepKind.SEND_TEXT
                }
            }
            hasPdf -> raw += ShareStepKind.SEND_DOCUMENT
            hasImage -> raw += ShareStepKind.SEND_IMAGE_CARDS
            hasText -> raw += ShareStepKind.SEND_TEXT
        }

        val total = raw.size
        return raw.mapIndexed { idx, kind ->
            when (kind) {
                ShareStepKind.SEND_TEXT -> PlannedShareStep(
                    stepIndex = idx,
                    totalSteps = total,
                    kind = kind,
                    format = ShareOutputFormat.TEXT_MESSAGE,
                    titleEn = "Send Text Message",
                    titleBn = "টেক্সট মেসেজ পাঠান",
                    subtitleEn = "Opens chat/share with formatted summary text (also copied to clipboard)",
                    subtitleBn = "বার্তাটি চ্যাট/শেয়ারে পাঠাবে (ক্লিপবোর্ডেও কপি করা আছে)"
                )
                ShareStepKind.SEND_DOCUMENT -> PlannedShareStep(
                    stepIndex = idx,
                    totalSteps = total,
                    kind = kind,
                    format = ShareOutputFormat.PDF,
                    titleEn = "Send $docLabelUpper Document",
                    titleBn = "$docLabelUpper ডকুমেন্ট পাঠান",
                    subtitleEn = "Shares the detailed $docLabelUpper file via WhatsApp / chooser",
                    subtitleBn = "বিস্তারিত $docLabelUpper ফাইলটি WhatsApp / শেয়ার শিটে পাঠাবে"
                )
                ShareStepKind.SEND_DOCUMENT_WITH_CAPTION -> PlannedShareStep(
                    stepIndex = idx,
                    totalSteps = total,
                    kind = kind,
                    format = ShareOutputFormat.PDF,
                    titleEn = "Send $docLabelUpper + Caption",
                    titleBn = "$docLabelUpper ও বার্তা পাঠান",
                    subtitleEn = "Attaches $docLabelUpper file with text caption (text also copied to clipboard)",
                    subtitleBn = "$docLabelUpper ফাইলের সাথে ক্যাপশন পাঠাবে (বার্তা ক্লিপবোর্ডেও কপি করা আছে)"
                )
                ShareStepKind.SEND_IMAGE_CARDS -> PlannedShareStep(
                    stepIndex = idx,
                    totalSteps = total,
                    kind = kind,
                    format = ShareOutputFormat.IMAGE_CARD,
                    titleEn = if (imagePageCount > 1) "Send Image Cards ($imagePageCount pages)" else "Send Image Card",
                    titleBn = if (imagePageCount > 1) "ইমেজ কার্ড পাঠান ($imagePageCount পৃষ্ঠা)" else "ইমেজ কার্ড পাঠান",
                    subtitleEn = "Shares high-resolution branded PNG summary card",
                    subtitleBn = "হাই-রেজোলিউশন ব্র্যান্ডেড PNG সারাংশ কার্ড পাঠাবে"
                )
                ShareStepKind.SEND_IMAGE_WITH_CAPTION -> PlannedShareStep(
                    stepIndex = idx,
                    totalSteps = total,
                    kind = kind,
                    format = ShareOutputFormat.IMAGE_CARD,
                    titleEn = "Send Image Card + Caption",
                    titleBn = "ইমেজ কার্ড ও বার্তা পাঠান",
                    subtitleEn = "Attaches branded PNG card with text caption (text also copied to clipboard)",
                    subtitleBn = "ব্র্যান্ডেড PNG কার্ডের সাথে ক্যাপশন পাঠাবে (বার্তা কপিও করা হয়েছে)"
                )
            }
        }
    }

    fun formatSharedFormatsCsv(
        selectedFormats: Set<ShareOutputFormat>,
        documentExtension: String = "pdf"
    ): String {
        val docToken = if (documentExtension.equals("xlsx", ignoreCase = true)) "XLSX" else "PDF"
        val ordered = buildList {
            if (ShareOutputFormat.PDF in selectedFormats) add(docToken)
            if (ShareOutputFormat.IMAGE_CARD in selectedFormats) add(ShareOutputFormat.IMAGE_CARD.name)
            if (ShareOutputFormat.TEXT_MESSAGE in selectedFormats) add(ShareOutputFormat.TEXT_MESSAGE.name)
        }
        return ordered.joinToString(",")
    }
}
