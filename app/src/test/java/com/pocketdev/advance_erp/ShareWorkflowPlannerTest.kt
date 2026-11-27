package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.communication.ShareOutputFormat
import com.pocketdev.advance_erp.core.communication.SharePreviewTab
import com.pocketdev.advance_erp.core.communication.ShareStepKind
import com.pocketdev.advance_erp.core.communication.ShareWorkflowPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareWorkflowPlannerTest {

    @Test
    fun toggleFormatSelection_blocksUncheckingLastSelectedFormatAndAllowsMultiSelect() {
        val singlePdf = setOf(ShareOutputFormat.PDF)
        val blocked = ShareWorkflowPlanner.toggleFormatSelection(singlePdf, ShareOutputFormat.PDF)
        assertTrue(blocked.blockedLastUncheck)
        assertEquals(singlePdf, blocked.selectedFormats)

        val addedImage = ShareWorkflowPlanner.toggleFormatSelection(singlePdf, ShareOutputFormat.IMAGE_CARD)
        assertFalse(addedImage.blockedLastUncheck)
        assertEquals(setOf(ShareOutputFormat.PDF, ShareOutputFormat.IMAGE_CARD), addedImage.selectedFormats)

        val removedPdf = ShareWorkflowPlanner.toggleFormatSelection(addedImage.selectedFormats, ShareOutputFormat.PDF)
        assertFalse(removedPdf.blockedLastUncheck)
        assertEquals(setOf(ShareOutputFormat.IMAGE_CARD), removedPdf.selectedFormats)
    }

    @Test
    fun buildShareSteps_handlesAllCombinationsWithoutSilentlyDroppingContent() {
        // 1. Single formats
        val textOnly = ShareWorkflowPlanner.buildShareSteps(setOf(ShareOutputFormat.TEXT_MESSAGE))
        assertEquals(listOf(ShareStepKind.SEND_TEXT), textOnly.map { it.kind })

        val pdfOnly = ShareWorkflowPlanner.buildShareSteps(setOf(ShareOutputFormat.PDF))
        assertEquals(listOf(ShareStepKind.SEND_DOCUMENT), pdfOnly.map { it.kind })

        val xlsxOnly = ShareWorkflowPlanner.buildShareSteps(setOf(ShareOutputFormat.PDF), documentExtension = "xlsx")
        assertTrue(xlsxOnly.single().titleEn.contains("XLSX"))

        val imageOnly = ShareWorkflowPlanner.buildShareSteps(setOf(ShareOutputFormat.IMAGE_CARD))
        assertEquals(listOf(ShareStepKind.SEND_IMAGE_CARDS), imageOnly.map { it.kind })

        // 2. Image + Text (single-page combines image+caption; multi-page splits cleanly)
        val imageAndText1 = ShareWorkflowPlanner.buildShareSteps(
            setOf(ShareOutputFormat.IMAGE_CARD, ShareOutputFormat.TEXT_MESSAGE),
            imagePageCount = 1
        )
        assertEquals(listOf(ShareStepKind.SEND_IMAGE_WITH_CAPTION), imageAndText1.map { it.kind })

        val imageAndText2 = ShareWorkflowPlanner.buildShareSteps(
            setOf(ShareOutputFormat.IMAGE_CARD, ShareOutputFormat.TEXT_MESSAGE),
            imagePageCount = 2
        )
        assertEquals(listOf(ShareStepKind.SEND_IMAGE_CARDS, ShareStepKind.SEND_TEXT), imageAndText2.map { it.kind })

        // 3. PDF + Text -> separate Text then PDF because WhatsApp strips PDF captions
        val pdfAndText = ShareWorkflowPlanner.buildShareSteps(setOf(ShareOutputFormat.PDF, ShareOutputFormat.TEXT_MESSAGE))
        assertEquals(listOf(ShareStepKind.SEND_TEXT, ShareStepKind.SEND_DOCUMENT), pdfAndText.map { it.kind })

        // 4. PDF + Image -> separate Document then Image cards (no heterogeneous MIME intent crash)
        val pdfAndImage = ShareWorkflowPlanner.buildShareSteps(setOf(ShareOutputFormat.PDF, ShareOutputFormat.IMAGE_CARD))
        assertEquals(listOf(ShareStepKind.SEND_DOCUMENT, ShareStepKind.SEND_IMAGE_CARDS), pdfAndImage.map { it.kind })

        // 5. All 3 selected -> exact Text -> PDF -> Image Card sequence matching Reference #5
        val allThree = ShareWorkflowPlanner.buildShareSteps(ShareOutputFormat.entries.toSet(), imagePageCount = 1)
        assertEquals(
            listOf(ShareStepKind.SEND_TEXT, ShareStepKind.SEND_DOCUMENT, ShareStepKind.SEND_IMAGE_CARDS),
            allThree.map { it.kind }
        )
        assertEquals(listOf(0, 1, 2), allThree.map { it.stepIndex })
        assertTrue(allThree.all { it.totalSteps == 3 })
    }

    @Test(expected = IllegalArgumentException::class)
    fun buildShareSteps_rejectsEmptyFormatSelection() {
        ShareWorkflowPlanner.buildShareSteps(emptySet())
    }

    @Test
    fun previewTabAndSharedFormatsCsv_resolveDeterministically() {
        assertEquals(
            SharePreviewTab.IMAGE,
            ShareWorkflowPlanner.resolveInitialPreviewTab(setOf(ShareOutputFormat.IMAGE_CARD, ShareOutputFormat.TEXT_MESSAGE))
        )
        assertEquals(
            SharePreviewTab.PDF,
            ShareWorkflowPlanner.resolveInitialPreviewTab(ShareOutputFormat.entries.toSet())
        )
        assertEquals(
            SharePreviewTab.TEXT,
            ShareWorkflowPlanner.resolveInitialPreviewTab(setOf(ShareOutputFormat.TEXT_MESSAGE))
        )
        assertEquals(
            "PDF,IMAGE_CARD,TEXT_MESSAGE",
            ShareWorkflowPlanner.formatSharedFormatsCsv(
                setOf(ShareOutputFormat.TEXT_MESSAGE, ShareOutputFormat.PDF, ShareOutputFormat.IMAGE_CARD),
                "pdf"
            )
        )
        assertEquals(
            "XLSX,TEXT_MESSAGE",
            ShareWorkflowPlanner.formatSharedFormatsCsv(
                setOf(ShareOutputFormat.PDF, ShareOutputFormat.TEXT_MESSAGE),
                "xlsx"
            )
        )
    }
}
