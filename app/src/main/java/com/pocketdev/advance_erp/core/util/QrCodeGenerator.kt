package com.pocketdev.advance_erp.core.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * Self-contained Pure Kotlin QR Code Model 2 Generator.
 * Implements ISO/IEC 18004 Byte Mode with Reed-Solomon Error Correction.
 * Completely offline with zero third-party dependencies.
 */
object QrCodeGenerator {

    /**
     * QR Code Structured Payload Prefix
     */
    const val QR_PREFIX_PRODUCT = "ADV-ERP|PRODUCT"
    const val QR_PREFIX_VARIANT = "ADV-ERP|VARIANT"
    const val QR_PREFIX_INVOICE = "ADV-ERP|INV"
    const val QR_PREFIX_DOCUMENT = "ADV-ERP|DOC"

    data class QrPayload(
        val type: QrType,
        val targetId: String,
        val secondaryId: String? = null,
        val code: String? = null
    )

    enum class QrType {
        PRODUCT,
        VARIANT,
        DOCUMENT,
        PLAIN_TEXT
    }

    /**
     * Creates a structured QR code payload for a product.
     * Contains only internal reference IDs, zero customer/cost data.
     */
    fun createProductQrPayload(productId: String, sku: String): String {
        return "$QR_PREFIX_PRODUCT|$productId|$sku"
    }

    /**
     * Creates a structured QR code payload for a product variant.
     */
    fun createVariantQrPayload(variantId: String, productId: String, sku: String): String {
        return "$QR_PREFIX_VARIANT|$variantId|$productId|$sku"
    }

    /**
     * Parses a scanned QR payload or plain string into structured target.
     */
    fun parsePayload(scanned: String): QrPayload {
        val trimmed = scanned.trim()

        // 1. Colon-delimited simple format: ADV-ERP:PRD:<id>
        if (trimmed.startsWith("ADV-ERP:PRD:")) {
            val id = trimmed.removePrefix("ADV-ERP:PRD:").trim()
            return QrPayload(QrType.PRODUCT, targetId = id)
        }
        if (trimmed.startsWith("ADV-ERP:VAR:")) {
            val id = trimmed.removePrefix("ADV-ERP:VAR:").trim()
            return QrPayload(QrType.VARIANT, targetId = id)
        }

        // 2. Short prefix pipe format: ADV-ERP:PRD|<prodId>|<sku>
        if (trimmed.startsWith("ADV-ERP:PRD|")) {
            val parts = trimmed.split("|")
            val id = parts.getOrNull(1) ?: ""
            val sku = parts.getOrNull(2)
            return QrPayload(QrType.PRODUCT, targetId = id, code = sku)
        }

        // 3. Short prefix variant pipe format: ADV-ERP:VAR|<varId>|<prodId>|<sku>
        if (trimmed.startsWith("ADV-ERP:VAR|")) {
            val parts = trimmed.split("|")
            val varId = parts.getOrNull(1) ?: ""
            val prodId = parts.getOrNull(2)
            val sku = parts.getOrNull(3)
            return QrPayload(QrType.VARIANT, targetId = varId, secondaryId = prodId, code = sku)
        }

        // 4. Full pipe format: ADV-ERP|PRODUCT|<prodId>|<sku>
        if (trimmed.startsWith("ADV-ERP|PRODUCT|")) {
            val parts = trimmed.split("|")
            val id = parts.getOrNull(2) ?: ""
            val sku = parts.getOrNull(3)
            return QrPayload(QrType.PRODUCT, targetId = id, code = sku)
        }

        // 5. Full pipe format variant: ADV-ERP|VARIANT|<varId>|<prodId>|<sku>
        if (trimmed.startsWith("ADV-ERP|VARIANT|")) {
            val parts = trimmed.split("|")
            val varId = parts.getOrNull(2) ?: ""
            val prodId = parts.getOrNull(3)
            val sku = parts.getOrNull(4)
            return QrPayload(QrType.VARIANT, targetId = varId, secondaryId = prodId, code = sku)
        }

        // 6. Document/Invoice verification QR: ADV-ERP|INV|<docNumber>|... or ADV-ERP|DOC|<docNumber>|...
        if (trimmed.startsWith("$QR_PREFIX_INVOICE|") || trimmed.startsWith("$QR_PREFIX_DOCUMENT|")) {
            val parts = trimmed.split("|")
            val docNo = parts.getOrNull(2) ?: ""
            val docKind = parts.getOrNull(1)
            val meta = parts.drop(3).joinToString("|").takeIf { it.isNotEmpty() }
            return QrPayload(QrType.DOCUMENT, targetId = docNo, secondaryId = docKind, code = meta)
        }

        return QrPayload(QrType.PLAIN_TEXT, targetId = trimmed, code = trimmed)
    }

    // --- QR Code Matrix Generation Engine ---

    // GF(256) log and antilog tables (primitive polynomial 0x11D: x^8 + x^4 + x^3 + x^2 + 1)
    private val EXP_TABLE = IntArray(512)
    private val LOG_TABLE = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            EXP_TABLE[i] = x
            EXP_TABLE[i + 255] = x
            LOG_TABLE[x] = i
            x = x shl 1
            if (x and 0x100 != 0) {
                x = x xor 0x11D
            }
        }
        LOG_TABLE[0] = 0
    }

    private fun gfMultiply(x: Int, y: Int): Int {
        if (x == 0 || y == 0) return 0
        return EXP_TABLE[LOG_TABLE[x] + LOG_TABLE[y]]
    }

    // Reed-Solomon Generator Polynomial for Error Correction
    private fun getRsGeneratorPolynomial(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val factor = intArrayOf(1, EXP_TABLE[i])
            val product = IntArray(poly.size + 1)
            for (j in poly.indices) {
                for (k in factor.indices) {
                    product[j + k] = product[j + k] xor gfMultiply(poly[j], factor[k])
                }
            }
            poly = product
        }
        return poly
    }

    private fun computeRsErrorCorrection(data: IntArray, ecCount: Int): IntArray {
        val gen = getRsGeneratorPolynomial(ecCount)
        val remainder = IntArray(ecCount)
        for (b in data) {
            val factor = b xor remainder[0]
            for (i in 0 until ecCount - 1) {
                remainder[i] = remainder[i + 1] xor gfMultiply(gen[i + 1], factor)
            }
            remainder[ecCount - 1] = gfMultiply(gen[ecCount], factor)
        }
        return remainder
    }

    // Standard ISO/IEC 18004 Versions 1..10 Table 9 specs (Error Correction Level M)
    private class QrSpec(
        val version: Int,
        val size: Int,
        val alignCoords: IntArray,
        val ecPerBlock: Int,
        val g1Blocks: Int,
        val g1DataCw: Int,
        val g2Blocks: Int,
        val g2DataCw: Int,
        val maxBytes: Int,
        val versionInfo: Int? = null
    ) {
        val totalDataCodewords: Int get() = g1Blocks * g1DataCw + g2Blocks * g2DataCw
    }

    private val LEVEL_M_SPECS = listOf(
        QrSpec(1, 21, intArrayOf(), 10, 1, 16, 0, 0, 14),
        QrSpec(2, 25, intArrayOf(6, 18), 16, 1, 28, 0, 0, 26),
        QrSpec(3, 29, intArrayOf(6, 22), 26, 1, 44, 0, 0, 42),
        QrSpec(4, 33, intArrayOf(6, 26), 18, 2, 32, 0, 0, 62),
        QrSpec(5, 37, intArrayOf(6, 30), 24, 2, 43, 0, 0, 84),
        QrSpec(6, 41, intArrayOf(6, 34), 16, 4, 27, 0, 0, 106),
        QrSpec(7, 45, intArrayOf(6, 22, 38), 18, 4, 31, 0, 0, 122, versionInfo = 0x07C94),
        QrSpec(8, 49, intArrayOf(6, 24, 42), 22, 2, 38, 2, 39, 152, versionInfo = 0x085BC),
        QrSpec(9, 53, intArrayOf(6, 26, 46), 22, 3, 36, 2, 37, 180, versionInfo = 0x09A99),
        QrSpec(10, 57, intArrayOf(6, 28, 50), 26, 4, 43, 1, 44, 213, versionInfo = 0x0A4D3)
    )

    private fun selectSpec(contentLength: Int): QrSpec {
        return LEVEL_M_SPECS.firstOrNull { contentLength <= it.maxBytes } ?: LEVEL_M_SPECS.last()
    }

    private fun truncateUtf8(text: String, maxBytes: Int): ByteArray {
        val raw = text.toByteArray(Charsets.UTF_8)
        if (raw.size <= maxBytes) return raw
        val out = ArrayList<Byte>(maxBytes)
        for (ch in text) {
            val charBytes = ch.toString().toByteArray(Charsets.UTF_8)
            if (out.size + charBytes.size > maxBytes) break
            for (b in charBytes) out.add(b)
        }
        return out.toByteArray()
    }

    /**
     * Generates a 2D boolean matrix for the QR Code (true = dark module, false = light module).
     */
    fun generateQrMatrix(text: String): Array<BooleanArray> {
        val maxSupported = LEVEL_M_SPECS.last().maxBytes
        val bytes = truncateUtf8(text, maxSupported)
        val spec = selectSpec(bytes.size)
        val size = spec.size

        // 1. Encode Data in Byte Mode (0100)
        val bitBuffer = mutableListOf<Boolean>()
        bitBuffer.addAll(listOf(false, true, false, false))
        // Character count indicator: 8 bits for versions 1..9, 16 bits for versions 10..26
        val countBits = if (spec.version <= 9) 8 else 16
        val len = bytes.size
        for (i in (countBits - 1) downTo 0) {
            bitBuffer.add(((len shr i) and 1) == 1)
        }
        // Data bits
        for (b in bytes) {
            val byteVal = b.toInt() and 0xFF
            for (i in 7 downTo 0) {
                bitBuffer.add(((byteVal shr i) and 1) == 1)
            }
        }
        // Terminator (up to 4 zeroes)
        val totalDataCw = spec.totalDataCodewords
        val maxDataBits = totalDataCw * 8
        val termLen = minOf(4, maxDataBits - bitBuffer.size)
        repeat(termLen) { bitBuffer.add(false) }
        // Pad to byte boundary
        while (bitBuffer.size % 8 != 0 && bitBuffer.size < maxDataBits) {
            bitBuffer.add(false)
        }
        // Pad bytes (0xEC, 0x11)
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        while (bitBuffer.size < maxDataBits) {
            val pad = padBytes[padIdx % 2]
            for (i in 7 downTo 0) {
                bitBuffer.add(((pad shr i) and 1) == 1)
            }
            padIdx++
        }

        // Convert data bits to data codewords
        val dataCodewords = IntArray(totalDataCw)
        for (i in 0 until totalDataCw) {
            var byteVal = 0
            for (j in 0 until 8) {
                if (bitBuffer[i * 8 + j]) {
                    byteVal = byteVal or (1 shl (7 - j))
                }
            }
            dataCodewords[i] = byteVal
        }

        // Partition into Group 1 & Group 2 blocks and compute per-block EC codewords
        val blocks = mutableListOf<IntArray>()
        var offset = 0
        repeat(spec.g1Blocks) {
            blocks.add(dataCodewords.copyOfRange(offset, offset + spec.g1DataCw))
            offset += spec.g1DataCw
        }
        repeat(spec.g2Blocks) {
            blocks.add(dataCodewords.copyOfRange(offset, offset + spec.g2DataCw))
            offset += spec.g2DataCw
        }
        val ecBlocks = blocks.map { computeRsErrorCorrection(it, spec.ecPerBlock) }

        // Interleave data codewords column-wise, then EC codewords column-wise
        val interleaved = ArrayList<Int>(totalDataCw + blocks.size * spec.ecPerBlock)
        val maxDataCw = maxOf(spec.g1DataCw, spec.g2DataCw)
        for (colIdx in 0 until maxDataCw) {
            for (block in blocks) {
                if (colIdx < block.size) interleaved.add(block[colIdx])
            }
        }
        for (colIdx in 0 until spec.ecPerBlock) {
            for (ecBlock in ecBlocks) {
                interleaved.add(ecBlock[colIdx])
            }
        }

        // Convert interleaved codewords to bit stream
        val finalBits = ArrayList<Boolean>(interleaved.size * 8)
        for (cw in interleaved) {
            for (j in 7 downTo 0) {
                finalBits.add(((cw shr j) and 1) == 1)
            }
        }

        // Initialize Matrix and Reserved Functions Mask
        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        // Function: Draw Finder Pattern (7x7) + 1px separator
        fun drawFinder(r0: Int, c0: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isInner = r in 2..4 && c in 2..4
                    matrix[r0 + r][c0 + c] = isBorder || isInner
                    reserved[r0 + r][c0 + c] = true
                }
            }
            for (r in -1..7) {
                for (c in -1..7) {
                    val row = r0 + r
                    val col = c0 + c
                    if (row in 0 until size && col in 0 until size) {
                        reserved[row][col] = true
                    }
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // Draw Alignment Patterns (5x5), skipping the 3 finder corners
        val coords = spec.alignCoords
        if (coords.size >= 2) {
            val firstCoord = coords.first()
            val lastCoord = coords.last()
            for (ar in coords) {
                for (ac in coords) {
                    val hitsFinderCorner =
                        (ar == firstCoord && ac == firstCoord) ||
                            (ar == firstCoord && ac == lastCoord) ||
                            (ar == lastCoord && ac == firstCoord)
                    if (hitsFinderCorner) continue
                    for (r in -2..2) {
                        for (c in -2..2) {
                            val isOuter = r == -2 || r == 2 || c == -2 || c == 2
                            val isCenter = r == 0 && c == 0
                            matrix[ar + r][ac + c] = isOuter || isCenter
                            reserved[ar + r][ac + c] = true
                        }
                    }
                }
            }
        }

        // Draw Timing Patterns (alternating dark/light at row 6 and col 6)
        for (i in 0 until size) {
            if (!reserved[6][i]) {
                matrix[6][i] = (i % 2 == 0)
                reserved[6][i] = true
            }
            if (!reserved[i][6]) {
                matrix[i][6] = (i % 2 == 0)
                reserved[i][6] = true
            }
        }

        // Dark Module at (4 * V + 9, 8) = (size - 8, 8)
        val darkR = 4 * spec.version + 9
        matrix[darkR][8] = true
        reserved[darkR][8] = true

        // Reserve Format Info Area (around finders)
        for (i in 0..8) {
            reserved[8][i] = true
            reserved[i][8] = true
            reserved[8][size - 1 - i] = true
            reserved[size - 1 - i][8] = true
        }

        // Version Information (Versions 7..10): 18 bits in both 6x3 bottom-left and 3x6 top-right blocks
        spec.versionInfo?.let { vInfo ->
            for (i in 0..17) {
                val bit = ((vInfo shr i) and 1) == 1
                val rBl = size - 11 + (i % 3)
                val cBl = i / 3
                matrix[rBl][cBl] = bit
                reserved[rBl][cBl] = true

                val rTr = i / 3
                val cTr = size - 11 + (i % 3)
                matrix[rTr][cTr] = bit
                reserved[rTr][cTr] = true
            }
        }

        // Place Data Bits Zig-Zag from right to left
        var bitIdx = 0
        var goingUp = true
        var col = size - 1

        while (col > 0) {
            if (col == 6) col--

            val rows = if (goingUp) (size - 1 downTo 0) else (0 until size)
            for (row in rows) {
                for (c in 0..1) {
                    val currCol = col - c
                    if (!reserved[row][currCol]) {
                        val bit = if (bitIdx < finalBits.size) finalBits[bitIdx++] else false
                        // Mask Pattern 0: (row + col) % 2 == 0
                        val mask = (row + currCol) % 2 == 0
                        matrix[row][currCol] = bit xor mask
                    }
                }
            }
            goingUp = !goingUp
            col -= 2
        }

        // Format Info for Error Correction Level M (00) and Mask 0 (000)
        val formatBits = intArrayOf(1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0)
        val tlCoords = listOf(
            Pair(8, 0), Pair(8, 1), Pair(8, 2), Pair(8, 3), Pair(8, 4), Pair(8, 5),
            Pair(8, 7), Pair(8, 8), Pair(7, 8), Pair(5, 8), Pair(4, 8), Pair(3, 8),
            Pair(2, 8), Pair(1, 8), Pair(0, 8)
        )
        for (i in 0 until 15) {
            matrix[tlCoords[i].first][tlCoords[i].second] = (formatBits[i] == 1)
        }

        // Bottom-left: formatBits[0..6] along (size-1, 8) .. (size-7, 8), preserving dark module at (size-8, 8)
        for (i in 0..6) {
            matrix[size - 1 - i][8] = (formatBits[i] == 1)
        }
        // Top-right: formatBits[7..14] along (8, size-8) .. (8, size-1)
        for (i in 0..7) {
            matrix[8][size - 8 + i] = (formatBits[7 + i] == 1)
        }

        return matrix
    }

    /**
     * Renders a QR code matrix into an Android Bitmap with quiet zones.
     */
    fun generateQrBitmap(
        content: String,
        targetSize: Int = 300,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE
    ): Bitmap {
        val sanitized = content.trim().ifBlank { "ADV-ERP" }
        val matrix = generateQrMatrix(sanitized)
        val matrixSize = matrix.size
        val quietZone = 4
        val totalModules = matrixSize + quietZone * 2

        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(lightColor)

        val modulePaint = Paint().apply {
            color = darkColor
            isAntiAlias = false
        }

        val scale = targetSize.toFloat() / totalModules.toFloat()

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (matrix[r][c]) {
                    val left = (c + quietZone) * scale
                    val top = (r + quietZone) * scale
                    val right = left + scale
                    val bottom = top + scale
                    canvas.drawRect(left, top, right, bottom, modulePaint)
                }
            }
        }

        return bitmap
    }
}
