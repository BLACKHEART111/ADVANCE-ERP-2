package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.util.PoAttachmentKind
import com.pocketdev.advance_erp.core.util.PoAttachmentStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class PoAttachmentStorageTest {

    @Test
    fun parseAndSerializeAttachmentsDeduplicatesAndHandlesBlankValues() {
        assertEquals(emptyList<String>(), PoAttachmentStorage.parseAttachments(null))
        assertEquals(emptyList<String>(), PoAttachmentStorage.parseAttachments("  , , "))

        val parsed = PoAttachmentStorage.parseAttachments(" /a/b.jpg , https://example.com/inv.pdf , /a/b.jpg ")
        assertEquals(listOf("/a/b.jpg", "https://example.com/inv.pdf"), parsed)

        assertNull(PoAttachmentStorage.serializeAttachments(emptyList()))
        assertEquals(
            "/a/b.jpg,https://example.com/inv.pdf",
            PoAttachmentStorage.serializeAttachments(listOf(" /a/b.jpg ", "https://example.com/inv.pdf", "/a/b.jpg"))
        )
    }

    @Test
    fun inspectAttachmentDetectsKindsAndFriendlyNames() {
        val linkInfo = PoAttachmentStorage.inspectAttachment("https://drive.google.com/file/d/123")
        assertEquals(PoAttachmentKind.LINK, linkInfo.kind)
        assertNull(linkInfo.localFile)

        val tempPdf = File.createTempFile("po_uuid__supplier_invoice", ".pdf")
        try {
            val pdfInfo = PoAttachmentStorage.inspectAttachment(tempPdf.absolutePath)
            assertEquals(PoAttachmentKind.PDF, pdfInfo.kind)
            assertNotNull(pdfInfo.localFile)
            assertEquals(true, pdfInfo.displayName.startsWith("supplier_invoice"))
        } finally {
            tempPdf.delete()
        }

        val tempPhoto = File.createTempFile("po_uuid__photo_receipt", ".jpg")
        try {
            val imgInfo = PoAttachmentStorage.inspectAttachment(tempPhoto.absolutePath)
            assertEquals(PoAttachmentKind.IMAGE, imgInfo.kind)
            assertNotNull(imgInfo.localFile)
        } finally {
            tempPhoto.delete()
        }
    }
}
