package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.communication.CommunicationStatus
import com.pocketdev.advance_erp.core.communication.graphics.BrandedDocumentFormatter
import com.pocketdev.advance_erp.core.communication.graphics.BrandedDocumentPalette
import com.pocketdev.advance_erp.core.util.QrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Calendar
import java.util.TimeZone

class BrandedGraphicsAndQrTest {

    @Test
    fun embeddedFontAssets_existWithValidOpenTypeTables() {
        val fontDir = File("src/main/assets/fonts")
        assertTrue("Font assets folder must exist at ${fontDir.absolutePath}", fontDir.isDirectory)

        val expectedFonts = listOf(
            "NotoSansBengaliUI-VF.ttf",
            "Roboto-Regular.ttf",
            "Roboto-Medium.ttf",
            "Roboto-Bold.ttf"
        )
        for (name in expectedFonts) {
            val file = File(fontDir, name)
            assertTrue("Missing font asset: $name", file.isFile)
            assertTrue("Font asset too small: $name (${file.length()})", file.length() > 100_000L)
            val bytes = file.readBytes()
            val sfnt = ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.BIG_ENDIAN).int
            assertTrue("Invalid TTF sfnt header for $name", sfnt == 0x00010000 || sfnt == 0x74727565)
        }

        val bengaliBytes = File(fontDir, "NotoSansBengaliUI-VF.ttf").readBytes()
        val buf = ByteBuffer.wrap(bengaliBytes).order(ByteOrder.BIG_ENDIAN)
        val numTables = buf.getShort(4).toInt() and 0xFFFF
        val tableTags = (0 until numTables).map { idx ->
            val off = 12 + idx * 16
            String(bengaliBytes, off, 4, Charsets.US_ASCII)
        }.toSet()
        assertTrue("NotoSansBengaliUI-VF.ttf must contain GSUB", "GSUB" in tableTags)
        assertTrue("NotoSansBengaliUI-VF.ttf must contain GPOS", "GPOS" in tableTags)
        assertTrue("NotoSansBengaliUI-VF.ttf must contain fvar", "fvar" in tableTags)
        assertTrue("NotoSansBengaliUI-VF.ttf must contain cmap", "cmap" in tableTags)
    }

    @Test
    fun documentFormatter_formatsCurrencySignedBalancesDatesAndDiscountsAccurately() {
        assertEquals("৳1,380.00", BrandedDocumentFormatter.formatTaka(1380_00L))
        assertEquals("৳ 30,940.00", BrandedDocumentFormatter.formatTaka(30940_00L, spacedSymbol = true))
        assertEquals("- ৳5,000.00", BrandedDocumentFormatter.formatTaka(-5000_00L))
        assertEquals("+ ৳1,380.00", BrandedDocumentFormatter.formatTaka(1380_00L, explicitPlus = true))
        assertEquals("৳0.00", BrandedDocumentFormatter.formatTaka(0L))

        assertEquals("BDT 29,560.00", BrandedDocumentFormatter.formatCurrencyCode(29560_00L))
        assertEquals("- BDT 5,000.00", BrandedDocumentFormatter.formatCurrencyCode(-5000_00L))

        val dueBal = BrandedDocumentFormatter.formatSignedBalance(25940_00L, isBangla = true)
        assertEquals("৳25,940.00", dueBal.amountText)
        assertEquals("বকেয়া", dueBal.labelText)
        assertEquals(BrandedDocumentPalette.BalanceSemanticTone.DUE_RED, dueBal.tone)

        val creditBal = BrandedDocumentFormatter.formatSignedBalance(-4500_00L, isBangla = true)
        assertEquals("৳4,500.00", creditBal.amountText)
        assertEquals("অগ্রিম ক্রেডিট", creditBal.labelText)
        assertEquals(BrandedDocumentPalette.BalanceSemanticTone.CREDIT_BLUE, creditBal.tone)

        val creditEn = BrandedDocumentFormatter.formatSignedBalance(-4500_00L, isBangla = false, useTakaSymbol = false)
        assertEquals("BDT 4,500.00", creditEn.amountText)
        assertEquals("Advance Credit", creditEn.labelText)

        val zeroBal = BrandedDocumentFormatter.formatSignedBalance(0L, isBangla = false)
        assertEquals("Settled", zeroBal.labelText)
        assertEquals(BrandedDocumentPalette.BalanceSemanticTone.SETTLED_GREEN, zeroBal.tone)

        assertEquals("২৭ নভেম্বর ২০২৬", BrandedDocumentFormatter.toBanglaDigits("27 নভেম্বর 2026"))

        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(2026, Calendar.NOVEMBER, 27, 2, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val bnDt = BrandedDocumentFormatter.formatDateTime(cal.timeInMillis, isBangla = true, includeTime = true)
        assertEquals("২৭ নভেম্বর ২০২৬, ০২:৩০", bnDt)
        val enDt = BrandedDocumentFormatter.formatDateTime(cal.timeInMillis, isBangla = false, includeTime = false)
        assertEquals("27 Nov 2026", enDt)

        val rangeBn = BrandedDocumentFormatter.formatDateRange(0L, cal.timeInMillis, isAllTime = true, isBangla = true)
        assertEquals("শুরু থেকে – ২৭ নভে ২০২৬", rangeBn)

        assertNull(BrandedDocumentFormatter.formatDiscountBadge(0L, 500L))
        assertEquals("5% (-৳100.00)", BrandedDocumentFormatter.formatDiscountBadge(100_00L, 500L))
        assertEquals("12.5% (-৳250.00)", BrandedDocumentFormatter.formatDiscountBadge(250_00L, 1250L))
        assertEquals("-৳150.00", BrandedDocumentFormatter.formatDiscountBadge(150_00L, null))
    }

    @Test
    fun documentPalette_enforcesDistinctSemanticColorsAndAllStatusBadges() {
        val colors = setOf(
            BrandedDocumentPalette.PaymentGreenText,
            BrandedDocumentPalette.DueRedText,
            BrandedDocumentPalette.CreditBlueText,
            BrandedDocumentPalette.BrandPurplePrimary
        )
        assertEquals("Semantic financial colors must remain distinct", 4, colors.size)
        assertNotEquals(BrandedDocumentPalette.PaymentGreenBg, BrandedDocumentPalette.DueRedBg)

        for (status in CommunicationStatus.entries) {
            val bn = BrandedDocumentPalette.resolveStatusBadge(status, isBangla = true)
            val en = BrandedDocumentPalette.resolveStatusBadge(status, isBangla = false)
            assertTrue("BN badge empty for $status", bn.text.isNotBlank())
            assertTrue("EN badge empty for $status", en.text.isNotBlank())
        }
        assertTrue(BrandedDocumentPalette.resolveStatusBadge(CommunicationStatus.UNPAID, true).isAlertIcon)
        assertTrue(BrandedDocumentPalette.resolveStatusBadge(CommunicationStatus.PAID, true).isCheckIcon)
        assertFalse(BrandedDocumentPalette.resolveStatusBadge(CommunicationStatus.PAID, true).isAlertIcon)
    }

    @Test
    fun qrCodeGenerator_supportsVersions1To10SafeTruncationAndDocumentPayloadParsing() {
        val cases = listOf(
            "INV-1" to 21, // V1 (<=14 bytes)
            "ADV-ERP|PRODUCT|prod-123|SKU-001" to 29, // V3 (32 bytes <=42)
            "ADV-ERP|INV|INV-2026-0001|20261127|1380.00|UNPAID" to 33, // V4 (51 bytes <=62)
            "A".repeat(95) to 41, // V6 (95 bytes <=106)
            "B".repeat(140) to 49, // V8 (140 bytes <=152)
            "C".repeat(205) to 57, // V10 (205 bytes <=213)
            ("বাংলা-" + "X".repeat(260)) to 57 // >250 bytes safely truncated to V10
        )

        for ((payload, expectedSize) in cases) {
            val matrix = QrCodeGenerator.generateQrMatrix(payload)
            assertEquals("Unexpected QR matrix size for payload length ${payload.length}", expectedSize, matrix.size)
            assertEquals(expectedSize, matrix[0].size)
            // Finder checks
            assertTrue(matrix[0][0])
            assertTrue(matrix[3][3])
            assertFalse(matrix[1][1])
            assertTrue(matrix[0][expectedSize - 1])
            assertTrue(matrix[expectedSize - 1][0])
            // Fixed dark module at (size - 8, 8)
            assertTrue("Dark module must stay true at (${expectedSize - 8}, 8)", matrix[expectedSize - 8][8])
        }

        // Verify V8 (size 49, align coords [6, 24, 42]) alignment center at (24, 24) and (42, 42)
        val v8Matrix = QrCodeGenerator.generateQrMatrix("B".repeat(140))
        assertTrue(v8Matrix[24][24])
        assertFalse(v8Matrix[23][23])
        assertTrue(v8Matrix[42][42])

        val parsed = QrCodeGenerator.parsePayload("ADV-ERP|INV|INV-2026-0001|20261127|1380.00|UNPAID")
        assertEquals(QrCodeGenerator.QrType.DOCUMENT, parsed.type)
        assertEquals("INV-2026-0001", parsed.targetId)
        assertEquals("INV", parsed.secondaryId)
        assertEquals("20261127|1380.00|UNPAID", parsed.code)
    }
}
