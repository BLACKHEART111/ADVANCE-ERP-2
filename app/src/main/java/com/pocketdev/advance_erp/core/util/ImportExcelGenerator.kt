package com.pocketdev.advance_erp.core.util

import android.content.Context
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.Supplier
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Generates a 4-sheet functional OpenXML (.xlsx) Supplier Purchase Order workbook
 * using [PoExportDataBuilder] as the single source of truth alongside Supplier PDF:
 *   1. Purchase Order (Header, Supplier/Summary blocks, Photos, 10-col Table with Formulas)
 *   2. Packing List (Carton Wise) (Per carton-variant allocation rows, non-double-counted CBM, formulas)
 *   3. Carton Summary (1-row per physical CTN with dimensions, weights, CBM & totals)
 *   4. Shipping Mark & Delivery (Default + individual carton marks, address, terms, notes)
 */
object ImportExcelGenerator {

    private fun escapeXml(text: String): String {
        val sb = StringBuilder(text.length + 16)
        for (ch in text) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                '\t', '\n', '\r' -> sb.append(ch)
                else -> if (ch.code >= 0x20) sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun getExcelDir(context: Context): File =
        File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }

    private fun writeZipEntry(zos: ZipOutputStream, path: String, content: String) {
        zos.putNextEntry(ZipEntry(path))
        zos.write(content.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
    }

    private fun writeZipBinaryEntry(zos: ZipOutputStream, path: String, bytes: ByteArray) {
        zos.putNextEntry(ZipEntry(path))
        zos.write(bytes)
        zos.closeEntry()
    }

    private data class EmbeddedSheetImage(
        val zeroBasedRow: Int,
        val zeroBasedCol: Int,
        val mediaName: String,
        val jpegBytes: ByteArray
    )

    private fun colLetter(zeroBasedCol: Int): String = ('A' + zeroBasedCol).toString()

    private fun inlineStrCell(colIdx: Int, row: Int, text: String, styleId: Int = 0): String {
        val ref = "${colLetter(colIdx)}$row"
        val sAttr = if (styleId > 0) " s=\"$styleId\"" else ""
        return "<c r=\"$ref\" t=\"inlineStr\"$sAttr><is><t>${escapeXml(text)}</t></is></c>"
    }

    private fun numberCell(colIdx: Int, row: Int, numericValue: String, styleId: Int = 0, formula: String? = null): String {
        val ref = "${colLetter(colIdx)}$row"
        val sAttr = if (styleId > 0) " s=\"$styleId\"" else ""
        return if (formula != null) {
            "<c r=\"$ref\"$sAttr><f>${escapeXml(formula)}</f><v>$numericValue</v></c>"
        } else {
            "<c r=\"$ref\" t=\"n\"$sAttr><v>$numericValue</v></c>"
        }
    }

    private fun blankCell(colIdx: Int, row: Int, styleId: Int = 0): String {
        val ref = "${colLetter(colIdx)}$row"
        val sAttr = if (styleId > 0) " s=\"$styleId\"" else ""
        return "<c r=\"$ref\"$sAttr/>"
    }

    fun generateSupplierOrderExcel(
        context: Context,
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        variantsMap: Map<String, ProductVariant> = emptyMap(),
        itemImagePaths: Map<String, String> = emptyMap(),
        cartons: List<ShipmentCarton> = emptyList(),
        selectedCartonNumbers: Set<String>? = null
    ): File {
        val isPartial = selectedCartonNumbers != null && cartons.isNotEmpty()
        val suffix = if (isPartial) "_Selected_${selectedCartonNumbers!!.size}CTN" else ""
        val file = File(getExcelDir(context), "Supplier_PO_${ImportPdfGenerator.sanitizeFileName(order.poNumber)}$suffix.xlsx")
        return generateSupplierOrderExcelToFile(
            file = file,
            order = order,
            items = items,
            productsMap = productsMap,
            supplier = supplier,
            businessProfile = businessProfile,
            variantsMap = variantsMap,
            itemImagePaths = itemImagePaths,
            cartons = cartons,
            selectedCartonNumbers = selectedCartonNumbers
        )
    }

    internal fun generateSupplierOrderExcelToFile(
        file: File,
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        variantsMap: Map<String, ProductVariant> = emptyMap(),
        itemImagePaths: Map<String, String> = emptyMap(),
        cartons: List<ShipmentCarton> = emptyList(),
        thumbnailBytesProvider: (String) -> ByteArray? = { path ->
            ImportPdfGenerator.loadSquareThumbnailJpegBytes(path, targetPx = 120)
        },
        selectedCartonNumbers: Set<String>? = null
    ): File {
        val exportData = PoExportDataBuilder.build(
            order = order,
            items = items,
            productsMap = productsMap,
            supplier = supplier,
            businessProfile = businessProfile,
            variantsMap = variantsMap,
            itemImagePaths = itemImagePaths,
            cartons = cartons,
            selectedCartonNumbers = selectedCartonNumbers
        )

        val (sheet1Xml, embeddedImages) = buildPurchaseOrderSheetXml(exportData, thumbnailBytesProvider)
        val sheet2Xml = buildPackingListSheetXml(exportData)
        val sheet3Xml = buildCartonSummarySheetXml(exportData)
        val sheet4Xml = buildShippingMarkSheetXml(exportData)
        val hasImages = embeddedImages.isNotEmpty()

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            val jpgDefaultXml = if (hasImages) "  <Default Extension=\"jpg\" ContentType=\"image/jpeg\"/>\n" else ""
            val drawingOverrideXml = if (hasImages) {
                "  <Override PartName=\"/xl/drawings/drawing1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/>\n"
            } else ""

            val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-officedocument.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
$jpgDefaultXml  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
$drawingOverrideXml</Types>"""
            writeZipEntry(zos, "[Content_Types].xml", contentTypesXml)

            val rootRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            writeZipEntry(zos, "_rels/.rels", rootRelsXml)

            val wbRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
  <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", wbRelsXml)

            val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Purchase Order" sheetId="1" r:id="rId1"/>
    <sheet name="Packing List (Carton Wise)" sheetId="2" r:id="rId2"/>
    <sheet name="Carton Summary" sheetId="3" r:id="rId3"/>
    <sheet name="Shipping Mark &amp; Delivery" sheetId="4" r:id="rId4"/>
  </sheets>
</workbook>"""
            writeZipEntry(zos, "xl/workbook.xml", workbookXml)

            writeZipEntry(zos, "xl/styles.xml", buildSupplierStylesXml())
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", sheet1Xml)
            writeZipEntry(zos, "xl/worksheets/sheet2.xml", sheet2Xml)
            writeZipEntry(zos, "xl/worksheets/sheet3.xml", sheet3Xml)
            writeZipEntry(zos, "xl/worksheets/sheet4.xml", sheet4Xml)

            if (hasImages) {
                val sheetRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing" Target="../drawings/drawing1.xml"/>
</Relationships>"""
                writeZipEntry(zos, "xl/worksheets/_rels/sheet1.xml.rels", sheetRelsXml)

                val uniqueMedia = LinkedHashMap<String, Pair<String, ByteArray>>()
                embeddedImages.forEach { img ->
                    if (!uniqueMedia.containsKey(img.mediaName)) {
                        val relId = "rId${uniqueMedia.size + 1}"
                        uniqueMedia[img.mediaName] = relId to img.jpegBytes
                    }
                }

                val drawingRelsSb = StringBuilder()
                drawingRelsSb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
""")
                uniqueMedia.forEach { (mediaName, relPair) ->
                    drawingRelsSb.append("  <Relationship Id=\"${relPair.first}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"../media/$mediaName\"/>\n")
                }
                drawingRelsSb.append("</Relationships>")
                writeZipEntry(zos, "xl/drawings/_rels/drawing1.xml.rels", drawingRelsSb.toString())

                uniqueMedia.forEach { (mediaName, relPair) ->
                    writeZipBinaryEntry(zos, "xl/media/$mediaName", relPair.second)
                }

                val drawingXml = buildSupplierDrawingXml(embeddedImages, uniqueMedia.mapValues { it.value.first })
                writeZipEntry(zos, "xl/drawings/drawing1.xml", drawingXml)
            }
        }
        return file
    }

    /**
     * Freight Forwarder Packing List Excel (.xlsx, 3 sheets).
     * Includes embedded product photos, quantity (pcs), carton L×W×H (cm), gross/net weight (KG),
     * CBM (m³), and shipping marks — strictly excluding Color column, Unit Prices, Total Money, and Payment Terms.
     */
    fun generateFreightForwarderPackingListExcel(
        context: Context,
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        variantsMap: Map<String, ProductVariant> = emptyMap(),
        itemImagePaths: Map<String, String> = emptyMap(),
        cartons: List<ShipmentCarton> = emptyList(),
        selectedCartonNumbers: Set<String>? = null
    ): File {
        val isPartial = selectedCartonNumbers != null && cartons.isNotEmpty()
        val suffix = if (isPartial) "_Selected_${selectedCartonNumbers!!.size}CTN" else ""
        val file = File(
            getExcelDir(context),
            "Forwarder_PackingList_${ImportPdfGenerator.sanitizeFileName(order.poNumber)}$suffix.xlsx"
        )
        return generateFreightForwarderPackingListExcelToFile(
            file = file,
            order = order,
            items = items,
            productsMap = productsMap,
            supplier = supplier,
            businessProfile = businessProfile,
            variantsMap = variantsMap,
            itemImagePaths = itemImagePaths,
            cartons = cartons,
            selectedCartonNumbers = selectedCartonNumbers
        )
    }

    internal fun generateFreightForwarderPackingListExcelToFile(
        file: File,
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        variantsMap: Map<String, ProductVariant> = emptyMap(),
        itemImagePaths: Map<String, String> = emptyMap(),
        cartons: List<ShipmentCarton> = emptyList(),
        thumbnailBytesProvider: (String) -> ByteArray? = { path ->
            ImportPdfGenerator.loadSquareThumbnailJpegBytes(path, targetPx = 120)
        },
        selectedCartonNumbers: Set<String>? = null
    ): File {
        val exportData = PoExportDataBuilder.build(
            order = order,
            items = items,
            productsMap = productsMap,
            supplier = supplier,
            businessProfile = businessProfile,
            variantsMap = variantsMap,
            itemImagePaths = itemImagePaths,
            cartons = cartons,
            selectedCartonNumbers = selectedCartonNumbers
        )

        val (sheet1Xml, embeddedImages) = buildForwarderPackingSheetXml(exportData, thumbnailBytesProvider)
        val sheet2Xml = buildForwarderCartonSummarySheetXml(exportData)
        val sheet3Xml = buildForwarderDeliverySheetXml(exportData)
        val hasImages = embeddedImages.isNotEmpty()

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            val jpgDefaultXml = if (hasImages) "  <Default Extension=\"jpg\" ContentType=\"image/jpeg\"/>\n" else ""
            val drawingOverrideXml = if (hasImages) {
                "  <Override PartName=\"/xl/drawings/drawing1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/>\n"
            } else ""

            val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-officedocument.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
$jpgDefaultXml  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
$drawingOverrideXml</Types>"""
            writeZipEntry(zos, "[Content_Types].xml", contentTypesXml)

            val rootRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            writeZipEntry(zos, "_rels/.rels", rootRelsXml)

            val wbRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", wbRelsXml)

            val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Packing List" sheetId="1" r:id="rId1"/>
    <sheet name="Carton Summary" sheetId="2" r:id="rId2"/>
    <sheet name="Forwarder &amp; Delivery" sheetId="3" r:id="rId3"/>
  </sheets>
</workbook>"""
            writeZipEntry(zos, "xl/workbook.xml", workbookXml)

            writeZipEntry(zos, "xl/styles.xml", buildSupplierStylesXml())
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", sheet1Xml)
            writeZipEntry(zos, "xl/worksheets/sheet2.xml", sheet2Xml)
            writeZipEntry(zos, "xl/worksheets/sheet3.xml", sheet3Xml)

            if (hasImages) {
                val sheetRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing" Target="../drawings/drawing1.xml"/>
</Relationships>"""
                writeZipEntry(zos, "xl/worksheets/_rels/sheet1.xml.rels", sheetRelsXml)

                val uniqueMedia = LinkedHashMap<String, Pair<String, ByteArray>>()
                embeddedImages.forEach { img ->
                    if (!uniqueMedia.containsKey(img.mediaName)) {
                        val relId = "rId${uniqueMedia.size + 1}"
                        uniqueMedia[img.mediaName] = relId to img.jpegBytes
                    }
                }

                val drawingRelsSb = StringBuilder()
                drawingRelsSb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
""")
                uniqueMedia.forEach { (mediaName, relPair) ->
                    drawingRelsSb.append("  <Relationship Id=\"${relPair.first}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"../media/$mediaName\"/>\n")
                }
                drawingRelsSb.append("</Relationships>")
                writeZipEntry(zos, "xl/drawings/_rels/drawing1.xml.rels", drawingRelsSb.toString())

                uniqueMedia.forEach { (mediaName, relPair) ->
                    writeZipBinaryEntry(zos, "xl/media/$mediaName", relPair.second)
                }

                val drawingXml = buildSupplierDrawingXml(embeddedImages, uniqueMedia.mapValues { it.value.first })
                writeZipEntry(zos, "xl/drawings/drawing1.xml", drawingXml)
            }
        }
        return file
    }

    /**
     * Style table indices:
     * 0: Default left wrap
     * 1: Bold left wrap
     * 2: Deep Navy banner title (white 14pt bold)
     * 3: Deep Navy banner subtitle (slate-200 10pt)
     * 4: Section header (Teal/Navy soft pill, #E0F2FE / #0F172A bold)
     * 5: Table header (Deep Navy #1B2A4A, white bold centered, bordered)
     * 6: Bordered body left (wrap)
     * 7: Bordered body center (wrap)
     * 8: Bordered CTN purple bold center
     * 9: Bordered int right (#,##0)
     * 10: Bordered currency right (#,##0.00)
     * 11: Bordered weight right (0.000)
     * 12: Bordered CBM right (0.0000)
     * 13: Summary/Total gold row bold left (#FEF3C7)
     * 14: Summary/Total gold row bold right int (#,##0)
     * 15: Summary/Total gold row bold right currency (#,##0.00)
     * 16: Summary/Total gold row bold right weight (0.000)
     * 17: Summary/Total gold row bold right CBM (0.0000)
     * 18: Status badge green (#DCFCE7, #166534 bold centered)
     * 19: Key label soft grey bold (#F1F5F9)
     * 20: Zebra body left (#F8FAFC)
     * 21: Zebra body center (#F8FAFC)
     * 22: Zebra int right (#,##0)
     * 23: Zebra currency right (#,##0.00)
     */
    private fun buildSupplierStylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="4">
    <numFmt numFmtId="164" formatCode="#,##0"/>
    <numFmt numFmtId="165" formatCode="#,##0.00"/>
    <numFmt numFmtId="166" formatCode="0.000"/>
    <numFmt numFmtId="167" formatCode="0.0000"/>
  </numFmts>
  <fonts count="7">
    <font><sz val="10"/><color rgb="FF1E293B"/><name val="Calibri"/></font>
    <font><b/><sz val="10"/><color rgb="FF0F172A"/><name val="Calibri"/></font>
    <font><b/><sz val="14"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    <font><sz val="10"/><color rgb="FFE2E8F0"/><name val="Calibri"/></font>
    <font><b/><sz val="10"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    <font><b/><sz val="10"/><color rgb="FF792B87"/><name val="Calibri"/></font>
    <font><b/><sz val="10"/><color rgb="FF166534"/><name val="Calibri"/></font>
  </fonts>
  <fills count="8">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF1B2A4A"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFEDE9FE"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFFEF3C7"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFDCFCE7"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF1F5F9"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF8FAFC"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/></border>
    <border>
      <left style="thin"><color rgb="FFCBD5E1"/></left>
      <right style="thin"><color rgb="FFCBD5E1"/></right>
      <top style="thin"><color rgb="FFCBD5E1"/></top>
      <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="24">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="2" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="3" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment horizontal="right" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="1" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="4" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="5" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <xf numFmtId="165" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <xf numFmtId="166" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <xf numFmtId="167" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <xf numFmtId="0" fontId="1" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="164" fontId="1" fillId="4" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <xf numFmtId="165" fontId="5" fillId="4" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <xf numFmtId="166" fontId="1" fillId="4" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <xf numFmtId="167" fontId="1" fillId="4" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <xf numFmtId="0" fontId="6" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    <xf numFmtId="0" fontId="1" fillId="6" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="0" fillId="7" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="0" fillId="7" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="164" fontId="0" fillId="7" borderId="1" xfId="0" applyNumberFormat="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <xf numFmtId="165" fontId="0" fillId="7" borderId="1" xfId="0" applyNumberFormat="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
  </cellXfs>
  <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""

    private fun buildSupplierDrawingXml(
        embeddedImages: List<EmbeddedSheetImage>,
        mediaRelIds: Map<String, String>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/openxmlformats/drawingml/2006/spreadsheetDrawing" xmlns:a="http://schemas.openxmlformats.org/openxmlformats/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
""")
        val imgSizeEmu = 457200L // 36pt (~48px) square inside 48pt row height
        val colOffEmu = 190500L
        val rowOffEmu = 76200L
        embeddedImages.forEachIndexed { idx, img ->
            val relId = mediaRelIds[img.mediaName] ?: return@forEachIndexed
            val picId = idx + 1
            sb.append("""  <xdr:oneCellAnchor>
    <xdr:from>
      <xdr:col>${img.zeroBasedCol}</xdr:col>
      <xdr:colOff>$colOffEmu</xdr:colOff>
      <xdr:row>${img.zeroBasedRow}</xdr:row>
      <xdr:rowOff>$rowOffEmu</xdr:rowOff>
    </xdr:from>
    <xdr:ext cx="$imgSizeEmu" cy="$imgSizeEmu"/>
    <xdr:pic>
      <xdr:nvPicPr>
        <xdr:cNvPr id="$picId" name="ProductPhoto$picId"/>
        <xdr:cNvPicPr><a:picLocks noChangeAspect="1"/></xdr:cNvPicPr>
      </xdr:nvPicPr>
      <xdr:blipFill>
        <a:blip r:embed="$relId"/>
        <a:stretch><a:fillRect/></a:stretch>
      </xdr:blipFill>
      <xdr:spPr>
        <a:xfrm><a:off x="0" y="0"/><a:ext cx="$imgSizeEmu" cy="$imgSizeEmu"/></a:xfrm>
        <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
      </xdr:spPr>
    </xdr:pic>
    <xdr:clientData/>
  </xdr:oneCellAnchor>
""")
        }
        sb.append("</xdr:wsDr>")
        return sb.toString()
    }

    private fun buildPurchaseOrderSheetXml(
        exportData: ValidatedSupplierPoExport,
        thumbnailBytesProvider: (String) -> ByteArray?
    ): Pair<String, List<EmbeddedSheetImage>> {
        val sb = StringBuilder()
        val merges = mutableListOf<String>()
        val embeddedImages = mutableListOf<EmbeddedSheetImage>()
        val pathToMediaName = mutableMapOf<String, String>()
        var row = 1

        fun emitRow(cells: List<String>, heightPt: Int = 20) {
            sb.append("    <row r=\"$row\" ht=\"$heightPt\" customHeight=\"1\">")
            cells.forEach { sb.append(it) }
            sb.append("</row>\n")
            row++
        }

        val docTitle = if (exportData.isLocalOrder) "LOCAL PURCHASE ORDER" else "SUPPLIER PURCHASE ORDER / 采购订单"
        // Row 1: Deep Navy Title Banner
        emitRow(
            listOf(
                inlineStrCell(0, row, exportData.companyName, 2),
                blankCell(1, row, 2),
                blankCell(2, row, 2),
                blankCell(3, row, 2),
                blankCell(4, row, 2),
                blankCell(5, row, 2),
                inlineStrCell(6, row, docTitle, 3),
                blankCell(7, row, 3),
                blankCell(8, row, 3),
                blankCell(9, row, 3)
            ),
            heightPt = 28
        )
        merges += "A1:F1"
        merges += "G1:J1"

        // Row 2: Company Subtitle + PO # & Date
        val compSub = listOfNotNull(
            exportData.companyAddress.takeIf { it.isNotBlank() },
            exportData.companyPhone.takeIf { it.isNotBlank() }?.let { "Tel: $it" },
            exportData.companyBin.takeIf { it.isNotBlank() }?.let { "BIN: $it" }
        ).joinToString(" | ").ifBlank { exportData.companyName }
        val poRightSub = if (exportData.isPartialCartonExport) {
            "PO #: ${exportData.poNumber} | Date: ${exportData.orderDateFormatted} | PARTIAL (${exportData.exportedCartonCount} CTN)"
        } else {
            "PO #: ${exportData.poNumber} | Date: ${exportData.orderDateFormatted}"
        }
        emitRow(
            listOf(
                inlineStrCell(0, row, compSub, 3),
                blankCell(1, row, 3),
                blankCell(2, row, 3),
                blankCell(3, row, 3),
                blankCell(4, row, 3),
                blankCell(5, row, 3),
                inlineStrCell(6, row, poRightSub, 3),
                blankCell(7, row, 3),
                blankCell(8, row, 3),
                blankCell(9, row, 3)
            ),
            heightPt = 20
        )
        merges += "A2:F2"
        merges += "G2:J2"

        // Blank Spacer Row 3
        emitRow(listOf(blankCell(0, row)), heightPt = 8)

        // Row 4: Section Headers Side-by-Side
        emitRow(
            listOf(
                inlineStrCell(0, row, if (exportData.isLocalOrder) "SUPPLIER / LOCAL VENDOR" else "SUPPLIER / OVERSEAS FACTORY", 4),
                blankCell(1, row, 4),
                blankCell(2, row, 4),
                blankCell(3, row, 4),
                blankCell(4, row, 4),
                inlineStrCell(5, row, "ORDER SUMMARY", 4),
                blankCell(6, row, 4),
                blankCell(7, row, 4),
                blankCell(8, row, 4),
                blankCell(9, row, 4)
            ),
            heightPt = 22
        )
        merges += "A4:E4"
        merges += "F4:J4"

        // Rows 5-9: Supplier Details (Left A-E) & Order Summary (Right F-J)
        val totalCtnText = if (exportData.poTotalCartons > 0) "${exportData.poTotalCartons} CTN" else "-"
        val totalGwText = if (exportData.poTotalGrossWeightKg > BigDecimal.ZERO) {
            "${exportData.poTotalGrossWeightKg.setScale(1, RoundingMode.HALF_UP)} KG"
        } else "-"
        val totalAmountStr = "${exportData.currency} ${Formatters.formatMinorUnit(exportData.poGrandTotalMinor)}"

        emitRow(
            listOf(
                inlineStrCell(0, row, "Name", 19),
                inlineStrCell(1, row, exportData.supplierName, 6),
                blankCell(2, row, 6),
                blankCell(3, row, 6),
                blankCell(4, row, 6),
                inlineStrCell(5, row, "PO Status", 19),
                blankCell(6, row, 19),
                inlineStrCell(7, row, exportData.status, 18),
                blankCell(8, row, 18),
                blankCell(9, row, 18)
            )
        )
        merges += "B5:E5"
        merges += "F5:G5"
        merges += "H5:J5"

        emitRow(
            listOf(
                inlineStrCell(0, row, "Attn", 19),
                inlineStrCell(1, row, exportData.supplierContactPerson, 6),
                blankCell(2, row, 6),
                blankCell(3, row, 6),
                blankCell(4, row, 6),
                inlineStrCell(5, row, "Currency", 19),
                blankCell(6, row, 19),
                inlineStrCell(7, row, exportData.currency, 6),
                blankCell(8, row, 6),
                blankCell(9, row, 6)
            )
        )
        merges += "B6:E6"
        merges += "F6:G6"
        merges += "H6:J6"

        emitRow(
            listOf(
                inlineStrCell(0, row, "Tel", 19),
                inlineStrCell(1, row, exportData.supplierPhone, 6),
                blankCell(2, row, 6),
                blankCell(3, row, 6),
                blankCell(4, row, 6),
                inlineStrCell(5, row, "Total Quantity", 19),
                blankCell(6, row, 19),
                inlineStrCell(7, row, "${exportData.poTotalQuantity} pcs", 6),
                blankCell(8, row, 6),
                blankCell(9, row, 6)
            )
        )
        merges += "B7:E7"
        merges += "F7:G7"
        merges += "H7:J7"

        emitRow(
            listOf(
                inlineStrCell(0, row, "Address", 19),
                inlineStrCell(1, row, exportData.supplierAddress, 6),
                blankCell(2, row, 6),
                blankCell(3, row, 6),
                blankCell(4, row, 6),
                inlineStrCell(5, row, "Total Cartons / G.W.", 19),
                blankCell(6, row, 19),
                inlineStrCell(7, row, "$totalCtnText • $totalGwText", 6),
                blankCell(8, row, 6),
                blankCell(9, row, 6)
            ),
            heightPt = 28
        )
        merges += "B8:E8"
        merges += "F8:G8"
        merges += "H8:J8"

        emitRow(
            listOf(
                inlineStrCell(0, row, "Order Date", 19),
                inlineStrCell(1, row, exportData.orderDateFormatted, 6),
                blankCell(2, row, 6),
                blankCell(3, row, 6),
                blankCell(4, row, 6),
                inlineStrCell(5, row, "Total Amount", 13),
                blankCell(6, row, 13),
                inlineStrCell(7, row, totalAmountStr, 15),
                blankCell(8, row, 15),
                blankCell(9, row, 15)
            )
        )
        merges += "B9:E9"
        merges += "F9:G9"
        merges += "H9:J9"

        // Blank Spacer Row 10
        emitRow(listOf(blankCell(0, row)), heightPt = 8)

        // Row 11: 10-Column Product Table Header (Frozen Pane & AutoFilter Header Row)
        val tableHeaderRow = row
        emitRow(
            listOf(
                inlineStrCell(0, row, "#", 5),
                inlineStrCell(1, row, "Product Name", 5),
                inlineStrCell(2, row, "SKU", 5),
                inlineStrCell(3, row, "Photo", 5),
                inlineStrCell(4, row, "Color", 5),
                inlineStrCell(5, row, "Size", 5),
                inlineStrCell(6, row, "CTN", 5),
                inlineStrCell(7, row, "Qty (pcs)", 5),
                inlineStrCell(8, row, "Unit (${exportData.currency})", 5),
                inlineStrCell(9, row, "Total (${exportData.currency})", 5)
            ),
            heightPt = 24
        )

        val firstDataRow = row
        exportData.items.forEachIndexed { idx, itemRow ->
            val currentZeroRow = row - 1
            val imagePath = itemRow.imagePath
            val imgBytes = if (!imagePath.isNullOrBlank()) thumbnailBytesProvider(imagePath) else null
            val hasRowPhoto = imgBytes != null && imgBytes.isNotEmpty()
            if (hasRowPhoto && !imagePath.isNullOrBlank()) {
                val mediaName = pathToMediaName.getOrPut(imagePath) { "image${pathToMediaName.size + 1}.jpg" }
                embeddedImages += EmbeddedSheetImage(
                    zeroBasedRow = currentZeroRow,
                    zeroBasedCol = 3, // Column D = Photo
                    mediaName = mediaName,
                    jpegBytes = imgBytes!!
                )
            }
            val zebra = idx % 2 == 1
            val leftStyle = if (zebra) 20 else 6
            val centerStyle = if (zebra) 21 else 7
            val qtyStyle = if (zebra) 22 else 9
            val moneyStyle = if (zebra) 23 else 10
            val unitStr = BigDecimal(itemRow.unitPriceMinor).movePointLeft(2).toPlainString()
            val totalStr = BigDecimal(itemRow.lineTotalMinor).movePointLeft(2).toPlainString()

            emitRow(
                listOf(
                    numberCell(0, row, itemRow.index.toString(), centerStyle),
                    inlineStrCell(1, row, itemRow.productName, leftStyle),
                    inlineStrCell(2, row, itemRow.sku, leftStyle),
                    inlineStrCell(3, row, if (hasRowPhoto) "" else "-", centerStyle),
                    inlineStrCell(4, row, itemRow.color, centerStyle),
                    inlineStrCell(5, row, itemRow.size, centerStyle),
                    inlineStrCell(6, row, itemRow.cartonDisplay, 8),
                    numberCell(7, row, itemRow.quantity.toString(), qtyStyle),
                    numberCell(8, row, unitStr, moneyStyle),
                    numberCell(9, row, totalStr, moneyStyle, formula = "H$row*I$row")
                ),
                heightPt = if (hasRowPhoto) 48 else 26
            )
        }
        val lastDataRow = (row - 1).coerceAtLeast(firstDataRow)

        // Totals Row using live Excel SUM formulas
        val grandTotalPlain = BigDecimal(exportData.poGrandTotalMinor).movePointLeft(2).toPlainString()
        emitRow(
            listOf(
                inlineStrCell(0, row, "TOTAL", 13),
                inlineStrCell(1, row, if (exportData.isPartialCartonExport) "Complete PO Line Totals (Selected Cartons: ${exportData.exportedCartonCount})" else "Grand Total", 13),
                blankCell(2, row, 13),
                blankCell(3, row, 13),
                blankCell(4, row, 13),
                blankCell(5, row, 13),
                inlineStrCell(6, row, totalCtnText, 13),
                numberCell(7, row, exportData.poTotalQuantity.toString(), 14, formula = if (exportData.items.isNotEmpty()) "SUM(H$firstDataRow:H$lastDataRow)" else null),
                blankCell(8, row, 13),
                numberCell(9, row, grandTotalPlain, 15, formula = if (exportData.items.isNotEmpty()) "SUM(J$firstDataRow:J$lastDataRow)" else null)
            ),
            heightPt = 24
        )
        merges += "B${row - 1}:F${row - 1}"

        // Delivery, Payment & Notes summary section below
        emitRow(listOf(blankCell(0, row)), heightPt = 8)
        emitRow(listOf(inlineStrCell(0, row, "DELIVERY & COMMERCIAL TERMS", 4), blankCell(1, row, 4), blankCell(2, row, 4), blankCell(3, row, 4), blankCell(4, row, 4), blankCell(5, row, 4), blankCell(6, row, 4), blankCell(7, row, 4), blankCell(8, row, 4), blankCell(9, row, 4)), heightPt = 20)
        merges += "A${row - 1}:J${row - 1}"

        val addressLabel = if (exportData.isLocalOrder) "Delivery Address" else "China Warehouse Address"
        emitRow(listOf(inlineStrCell(0, row, addressLabel, 19), blankCell(1, row, 19), inlineStrCell(2, row, exportData.deliveryAddress, 6), blankCell(3, row, 6), blankCell(4, row, 6), blankCell(5, row, 6), blankCell(6, row, 6), blankCell(7, row, 6), blankCell(8, row, 6), blankCell(9, row, 6)))
        merges += "A${row - 1}:B${row - 1}"
        merges += "C${row - 1}:J${row - 1}"

        emitRow(listOf(inlineStrCell(0, row, "Default Shipping Mark", 19), blankCell(1, row, 19), inlineStrCell(2, row, exportData.defaultShippingMark.replace("\n", " | "), 6), blankCell(3, row, 6), blankCell(4, row, 6), blankCell(5, row, 6), blankCell(6, row, 6), blankCell(7, row, 6), blankCell(8, row, 6), blankCell(9, row, 6)))
        merges += "A${row - 1}:B${row - 1}"
        merges += "C${row - 1}:J${row - 1}"

        emitRow(listOf(inlineStrCell(0, row, "Payment Terms", 19), blankCell(1, row, 19), inlineStrCell(2, row, exportData.paymentTerms, 6), blankCell(3, row, 6), blankCell(4, row, 6), blankCell(5, row, 6), blankCell(6, row, 6), blankCell(7, row, 6), blankCell(8, row, 6), blankCell(9, row, 6)))
        merges += "A${row - 1}:B${row - 1}"
        merges += "C${row - 1}:J${row - 1}"

        val noteText = exportData.userNotes.ifBlank { "Please pack strictly according to Sheet 2 (Packing List) & Sheet 4 (Shipping Marks)." }
        emitRow(listOf(inlineStrCell(0, row, "Note", 19), blankCell(1, row, 19), inlineStrCell(2, row, noteText, 6), blankCell(3, row, 6), blankCell(4, row, 6), blankCell(5, row, 6), blankCell(6, row, 6), blankCell(7, row, 6), blankCell(8, row, 6), blankCell(9, row, 6)))
        merges += "A${row - 1}:B${row - 1}"
        merges += "C${row - 1}:J${row - 1}"

        val mergeXml = if (merges.isNotEmpty()) {
            buildString {
                append("  <mergeCells count=\"${merges.size}\">\n")
                merges.forEach { append("    <mergeCell ref=\"$it\"/>\n") }
                append("  </mergeCells>\n")
            }
        } else ""

        val drawingXml = if (embeddedImages.isNotEmpty()) "  <drawing r:id=\"rId1\"/>\n" else ""

        val fullXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheetPr><pageSetUpPr fitToPage="1"/></sheetPr>
  <sheetViews>
    <sheetView tabSelected="1" workbookViewId="0">
      <pane ySplit="$tableHeaderRow" topLeftCell="A${tableHeaderRow + 1}" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="20"/>
  <cols>
    <col min="1" max="1" width="6" customWidth="1"/>
    <col min="2" max="2" width="32" customWidth="1"/>
    <col min="3" max="3" width="22" customWidth="1"/>
    <col min="4" max="4" width="12" customWidth="1"/>
    <col min="5" max="5" width="12" customWidth="1"/>
    <col min="6" max="6" width="11" customWidth="1"/>
    <col min="7" max="7" width="18" customWidth="1"/>
    <col min="8" max="8" width="11" customWidth="1"/>
    <col min="9" max="9" width="13" customWidth="1"/>
    <col min="10" max="10" width="15" customWidth="1"/>
  </cols>
  <sheetData>
$sb  </sheetData>
$mergeXml  <autoFilter ref="A$tableHeaderRow:J$lastDataRow"/>
  <pageMargins left="0.4" right="0.4" top="0.5" bottom="0.5" header="0.2" footer="0.2"/>
  <pageSetup paperSize="9" orientation="landscape" fitToWidth="1" fitToHeight="0"/>
$drawingXml</worksheet>"""
        return fullXml to embeddedImages
    }

    private fun buildPackingListSheetXml(exportData: ValidatedSupplierPoExport): String {
        val sb = StringBuilder()
        val merges = mutableListOf<String>()
        var row = 1
        fun emitRow(cells: List<String>, heightPt: Int = 20) {
            sb.append("    <row r=\"$row\" ht=\"$heightPt\" customHeight=\"1\">")
            cells.forEach { sb.append(it) }
            sb.append("</row>\n")
            row++
        }

        val banner = if (exportData.isPartialCartonExport) {
            "PACKING LIST (CARTON WISE) — SELECTED ${exportData.exportedCartonCount} OF ${exportData.allCartons.size} CTN (${exportData.poNumber})"
        } else {
            "PACKING LIST (CARTON WISE) — ${exportData.poNumber}"
        }
        emitRow(
            listOf(inlineStrCell(0, row, banner, 2)) + (1..11).map { blankCell(it, row, 2) },
            heightPt = 26
        )
        merges += "A1:L1"

        val headerRow = row
        emitRow(
            listOf(
                inlineStrCell(0, row, "CTN Number", 5),
                inlineStrCell(1, row, "Carton Type", 5),
                inlineStrCell(2, row, "Product Name", 5),
                inlineStrCell(3, row, "SKU", 5),
                inlineStrCell(4, row, "Color", 5),
                inlineStrCell(5, row, "Size", 5),
                inlineStrCell(6, row, "Packed Qty (pcs)", 5),
                inlineStrCell(7, row, "Allocated G.W. (KG)", 5),
                inlineStrCell(8, row, "Allocated N.W. (KG)", 5),
                inlineStrCell(9, row, "Dimensions (cm)", 5),
                inlineStrCell(10, row, "Carton CBM (m³)", 5),
                inlineStrCell(11, row, "Shipping Mark", 5)
            ),
            heightPt = 24
        )

        val firstDataRow = row
        exportData.exportedCartons.forEach { ctn ->
            val typeLabel = if (ctn.isMixed) "Mixed (${ctn.lines.size})" else "Single"
            ctn.lines.forEachIndexed { lIdx, line ->
                val zebra = (ctn.sequence % 2 == 0)
                val ls = if (zebra) 20 else 6
                val cs = if (zebra) 21 else 7
                val qs = if (zebra) 22 else 9
                // Record Carton CBM only on the first line of each carton so SUM(K:K) never double-counts!
                val cbmCell = if (lIdx == 0) {
                    numberCell(10, row, ctn.cbm.setScale(4, RoundingMode.HALF_UP).toPlainString(), 12)
                } else {
                    blankCell(10, row, cs)
                }
                val dimText = if (lIdx == 0) ctn.dimensionsDisplay else ""
                val markText = if (lIdx == 0) ctn.shippingMark.replace("\n", " | ") else ""

                emitRow(
                    listOf(
                        inlineStrCell(0, row, ctn.cartonNumber, 8),
                        inlineStrCell(1, row, typeLabel, cs),
                        inlineStrCell(2, row, line.productName, ls),
                        inlineStrCell(3, row, line.sku, ls),
                        inlineStrCell(4, row, line.color, cs),
                        inlineStrCell(5, row, line.size, cs),
                        numberCell(6, row, line.quantity.toString(), qs),
                        numberCell(7, row, line.allocatedGrossWeightKg.setScale(3, RoundingMode.HALF_UP).toPlainString(), 11),
                        numberCell(8, row, line.allocatedNetWeightKg.setScale(3, RoundingMode.HALF_UP).toPlainString(), 11),
                        inlineStrCell(9, row, dimText, cs),
                        cbmCell,
                        inlineStrCell(11, row, markText, ls)
                    ),
                    heightPt = 22
                )
            }
        }
        val lastDataRow = (row - 1).coerceAtLeast(firstDataRow)
        val hasRows = exportData.exportedCartons.isNotEmpty()

        emitRow(
            listOf(
                inlineStrCell(0, row, if (exportData.isPartialCartonExport) "SELECTED TOTAL" else "TOTAL", 13),
                inlineStrCell(1, row, "${exportData.exportedCartonCount} CTN", 13),
                blankCell(2, row, 13),
                blankCell(3, row, 13),
                blankCell(4, row, 13),
                blankCell(5, row, 13),
                numberCell(6, row, exportData.exportedPackedQuantity.toString(), 14, formula = if (hasRows) "SUM(G$firstDataRow:G$lastDataRow)" else null),
                numberCell(7, row, exportData.exportedGrossWeightKg.setScale(3, RoundingMode.HALF_UP).toPlainString(), 16, formula = if (hasRows) "SUM(H$firstDataRow:H$lastDataRow)" else null),
                numberCell(8, row, exportData.exportedNetWeightKg.setScale(3, RoundingMode.HALF_UP).toPlainString(), 16, formula = if (hasRows) "SUM(I$firstDataRow:I$lastDataRow)" else null),
                blankCell(9, row, 13),
                numberCell(10, row, exportData.exportedCbm.setScale(4, RoundingMode.HALF_UP).toPlainString(), 17, formula = if (hasRows) "SUM(K$firstDataRow:K$lastDataRow)" else null),
                blankCell(11, row, 13)
            ),
            heightPt = 24
        )
        merges += "C${row - 1}:F${row - 1}"

        val mergeXml = buildString {
            append("  <mergeCells count=\"${merges.size}\">\n")
            merges.forEach { append("    <mergeCell ref=\"$it\"/>\n") }
            append("  </mergeCells>\n")
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheetPr><pageSetUpPr fitToPage="1"/></sheetPr>
  <sheetViews>
    <sheetView workbookViewId="0">
      <pane ySplit="$headerRow" topLeftCell="A${headerRow + 1}" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="20"/>
  <cols>
    <col min="1" max="1" width="12" customWidth="1"/>
    <col min="2" max="2" width="12" customWidth="1"/>
    <col min="3" max="3" width="28" customWidth="1"/>
    <col min="4" max="4" width="22" customWidth="1"/>
    <col min="5" max="5" width="12" customWidth="1"/>
    <col min="6" max="6" width="11" customWidth="1"/>
    <col min="7" max="7" width="15" customWidth="1"/>
    <col min="8" max="8" width="17" customWidth="1"/>
    <col min="9" max="9" width="17" customWidth="1"/>
    <col min="10" max="10" width="16" customWidth="1"/>
    <col min="11" max="11" width="15" customWidth="1"/>
    <col min="12" max="12" width="32" customWidth="1"/>
  </cols>
  <sheetData>
$sb  </sheetData>
$mergeXml  <autoFilter ref="A$headerRow:L$lastDataRow"/>
  <pageMargins left="0.4" right="0.4" top="0.5" bottom="0.5" header="0.2" footer="0.2"/>
  <pageSetup paperSize="9" orientation="landscape" fitToWidth="1" fitToHeight="0"/>
</worksheet>"""
    }

    private fun buildCartonSummarySheetXml(exportData: ValidatedSupplierPoExport): String {
        val sb = StringBuilder()
        val merges = mutableListOf<String>()
        var row = 1
        fun emitRow(cells: List<String>, heightPt: Int = 20) {
            sb.append("    <row r=\"$row\" ht=\"$heightPt\" customHeight=\"1\">")
            cells.forEach { sb.append(it) }
            sb.append("</row>\n")
            row++
        }

        val banner = if (exportData.isPartialCartonExport) {
            "CARTON SUMMARY (SELECTED ${exportData.exportedCartonCount} OF ${exportData.allCartons.size} CTN) — ${exportData.poNumber}"
        } else {
            "CARTON SUMMARY — ${exportData.poNumber}"
        }
        emitRow(listOf(inlineStrCell(0, row, banner, 2)) + (1..11).map { blankCell(it, row, 2) }, heightPt = 26)
        merges += "A1:L1"

        // Top KPI Row
        emitRow(
            listOf(
                inlineStrCell(0, row, "Total Cartons", 19),
                inlineStrCell(1, row, "${exportData.exportedCartonCount} CTN", 8),
                inlineStrCell(2, row, "Total Quantity", 19),
                inlineStrCell(3, row, "${exportData.exportedPackedQuantity} pcs", 13),
                inlineStrCell(4, row, "Total Gross Weight", 19),
                inlineStrCell(5, row, "${exportData.exportedGrossWeightKg.setScale(3, RoundingMode.HALF_UP)} KG", 13),
                inlineStrCell(6, row, "Total Net Weight", 19),
                inlineStrCell(7, row, "${exportData.exportedNetWeightKg.setScale(3, RoundingMode.HALF_UP)} KG", 13),
                inlineStrCell(8, row, "Total CBM", 19),
                inlineStrCell(9, row, "${exportData.exportedCbm.setScale(4, RoundingMode.HALF_UP)} m³", 13),
                blankCell(10, row, 19),
                blankCell(11, row, 19)
            ),
            heightPt = 22
        )
        merges += "K2:L2"

        emitRow(listOf(blankCell(0, row)), heightPt = 8)

        val headerRow = row
        emitRow(
            listOf(
                inlineStrCell(0, row, "CTN Number", 5),
                inlineStrCell(1, row, "Carton Type", 5),
                inlineStrCell(2, row, "Carton Contents", 5),
                inlineStrCell(3, row, "Qty (pcs)", 5),
                inlineStrCell(4, row, "Gross Wt (KG)", 5),
                inlineStrCell(5, row, "Net Wt (KG)", 5),
                inlineStrCell(6, row, "Length (cm)", 5),
                inlineStrCell(7, row, "Width (cm)", 5),
                inlineStrCell(8, row, "Height (cm)", 5),
                inlineStrCell(9, row, "Dimensions", 5),
                inlineStrCell(10, row, "CBM (m³)", 5),
                inlineStrCell(11, row, "Shipping Mark", 5)
            ),
            heightPt = 24
        )

        val firstDataRow = row
        exportData.exportedCartons.forEachIndexed { idx, ctn ->
            val zebra = idx % 2 == 1
            val ls = if (zebra) 20 else 6
            val cs = if (zebra) 21 else 7
            val qs = if (zebra) 22 else 9
            val contentsSummary = ctn.lines.joinToString("; ") { l ->
                val spec = listOfNotNull(l.color.takeIf { it != "-" }, l.size.takeIf { it != "-" }).joinToString("/")
                val title = if (spec.isNotBlank()) "${l.productName} ($spec)" else l.productName
                "$title × ${l.quantity} pcs"
            }
            emitRow(
                listOf(
                    inlineStrCell(0, row, ctn.cartonNumber, 8),
                    inlineStrCell(1, row, if (ctn.isMixed) "Mixed" else "Single", cs),
                    inlineStrCell(2, row, contentsSummary, ls),
                    numberCell(3, row, ctn.totalQty.toString(), qs),
                    numberCell(4, row, ctn.grossWeightKg.setScale(3, RoundingMode.HALF_UP).toPlainString(), 11),
                    numberCell(5, row, ctn.netWeightKg.setScale(3, RoundingMode.HALF_UP).toPlainString(), 11),
                    numberCell(6, row, ctn.lengthCm.stripTrailingZeros().toPlainString(), cs),
                    numberCell(7, row, ctn.widthCm.stripTrailingZeros().toPlainString(), cs),
                    numberCell(8, row, ctn.heightCm.stripTrailingZeros().toPlainString(), cs),
                    inlineStrCell(9, row, ctn.dimensionsDisplay, cs),
                    numberCell(10, row, ctn.cbm.setScale(4, RoundingMode.HALF_UP).toPlainString(), 12),
                    inlineStrCell(11, row, ctn.shippingMark.replace("\n", " | "), ls)
                ),
                heightPt = 28
            )
        }
        val lastDataRow = (row - 1).coerceAtLeast(firstDataRow)
        val hasRows = exportData.exportedCartons.isNotEmpty()

        emitRow(
            listOf(
                inlineStrCell(0, row, if (exportData.isPartialCartonExport) "SELECTED TOTAL" else "TOTAL", 13),
                inlineStrCell(1, row, "${exportData.exportedCartonCount} CTN", 13),
                inlineStrCell(2, row, if (exportData.isPartialCartonExport) "Complete PO: ${exportData.allCartons.size} CTN (${exportData.poTotalQuantity} pcs)" else "All Cartons Reconciled", 13),
                numberCell(3, row, exportData.exportedPackedQuantity.toString(), 14, formula = if (hasRows) "SUM(D$firstDataRow:D$lastDataRow)" else null),
                numberCell(4, row, exportData.exportedGrossWeightKg.setScale(3, RoundingMode.HALF_UP).toPlainString(), 16, formula = if (hasRows) "SUM(E$firstDataRow:E$lastDataRow)" else null),
                numberCell(5, row, exportData.exportedNetWeightKg.setScale(3, RoundingMode.HALF_UP).toPlainString(), 16, formula = if (hasRows) "SUM(F$firstDataRow:F$lastDataRow)" else null),
                blankCell(6, row, 13),
                blankCell(7, row, 13),
                blankCell(8, row, 13),
                blankCell(9, row, 13),
                numberCell(10, row, exportData.exportedCbm.setScale(4, RoundingMode.HALF_UP).toPlainString(), 17, formula = if (hasRows) "SUM(K$firstDataRow:K$lastDataRow)" else null),
                blankCell(11, row, 13)
            ),
            heightPt = 24
        )
        merges += "G${row - 1}:J${row - 1}"

        val mergeXml = buildString {
            append("  <mergeCells count=\"${merges.size}\">\n")
            merges.forEach { append("    <mergeCell ref=\"$it\"/>\n") }
            append("  </mergeCells>\n")
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheetPr><pageSetUpPr fitToPage="1"/></sheetPr>
  <sheetViews>
    <sheetView workbookViewId="0">
      <pane ySplit="$headerRow" topLeftCell="A${headerRow + 1}" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="20"/>
  <cols>
    <col min="1" max="1" width="12" customWidth="1"/>
    <col min="2" max="2" width="11" customWidth="1"/>
    <col min="3" max="3" width="38" customWidth="1"/>
    <col min="4" max="4" width="12" customWidth="1"/>
    <col min="5" max="5" width="14" customWidth="1"/>
    <col min="6" max="6" width="14" customWidth="1"/>
    <col min="7" max="7" width="11" customWidth="1"/>
    <col min="8" max="8" width="11" customWidth="1"/>
    <col min="9" max="9" width="11" customWidth="1"/>
    <col min="10" max="10" width="16" customWidth="1"/>
    <col min="11" max="11" width="13" customWidth="1"/>
    <col min="12" max="12" width="30" customWidth="1"/>
  </cols>
  <sheetData>
$sb  </sheetData>
$mergeXml  <autoFilter ref="A$headerRow:L$lastDataRow"/>
  <pageMargins left="0.4" right="0.4" top="0.5" bottom="0.5" header="0.2" footer="0.2"/>
  <pageSetup paperSize="9" orientation="landscape" fitToWidth="1" fitToHeight="0"/>
</worksheet>"""
    }

    private fun buildShippingMarkSheetXml(exportData: ValidatedSupplierPoExport): String {
        val sb = StringBuilder()
        val merges = mutableListOf<String>()
        var row = 1
        fun emitRow(cells: List<String>, heightPt: Int = 20) {
            sb.append("    <row r=\"$row\" ht=\"$heightPt\" customHeight=\"1\">")
            cells.forEach { sb.append(it) }
            sb.append("</row>\n")
            row++
        }

        emitRow(listOf(inlineStrCell(0, row, "SHIPPING MARK & DELIVERY INSTRUCTIONS — ${exportData.poNumber}", 2), blankCell(1, row, 2), blankCell(2, row, 2), blankCell(3, row, 2)), heightPt = 26)
        merges += "A1:D1"

        emitRow(listOf(inlineStrCell(0, row, "Default PO Shipping Mark", 19), inlineStrCell(1, row, exportData.defaultShippingMark, 6), blankCell(2, row, 6), blankCell(3, row, 6)), heightPt = 34)
        merges += "B2:D2"

        val addressLabel = if (exportData.isLocalOrder) "Delivery Warehouse Address" else "China Warehouse Address"
        emitRow(listOf(inlineStrCell(0, row, addressLabel, 19), inlineStrCell(1, row, exportData.deliveryAddress, 6), blankCell(2, row, 6), blankCell(3, row, 6)), heightPt = 28)
        merges += "B3:D3"

        emitRow(listOf(inlineStrCell(0, row, "Payment Terms", 19), inlineStrCell(1, row, exportData.paymentTerms, 6), blankCell(2, row, 6), blankCell(3, row, 6)))
        merges += "B4:D4"

        val instr = exportData.userNotes.ifBlank { "Pack strictly according to Carton List and verify individual shipping marks." }
        emitRow(listOf(inlineStrCell(0, row, "Supplier Instructions", 19), inlineStrCell(1, row, instr, 6), blankCell(2, row, 6), blankCell(3, row, 6)), heightPt = 28)
        merges += "B5:D5"

        emitRow(listOf(blankCell(0, row)), heightPt = 8)

        val headerRow = row
        emitRow(
            listOf(
                inlineStrCell(0, row, "CTN Number", 5),
                inlineStrCell(1, row, "Mark Source", 5),
                inlineStrCell(2, row, "Carton Shipping Mark", 5),
                inlineStrCell(3, row, "Carton Contents & Qty", 5)
            ),
            heightPt = 24
        )

        exportData.exportedCartons.forEachIndexed { idx, ctn ->
            val zebra = idx % 2 == 1
            val ls = if (zebra) 20 else 6
            val cs = if (zebra) 21 else 7
            val contents = ctn.lines.joinToString("; ") { "${it.productName} (${it.color}/${it.size}) × ${it.quantity} pcs" }
            emitRow(
                listOf(
                    inlineStrCell(0, row, ctn.cartonNumber, 8),
                    inlineStrCell(1, row, if (ctn.isCustomShippingMark) "Custom Override" else "PO Default", cs),
                    inlineStrCell(2, row, ctn.shippingMark, ls),
                    inlineStrCell(3, row, contents, ls)
                ),
                heightPt = 34
            )
        }

        val lastDataRow = (row - 1).coerceAtLeast(headerRow)
        val mergeXml = buildString {
            append("  <mergeCells count=\"${merges.size}\">\n")
            merges.forEach { append("    <mergeCell ref=\"$it\"/>\n") }
            append("  </mergeCells>\n")
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheetPr><pageSetUpPr fitToPage="1"/></sheetPr>
  <sheetViews>
    <sheetView workbookViewId="0">
      <pane ySplit="$headerRow" topLeftCell="A${headerRow + 1}" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="20"/>
  <cols>
    <col min="1" max="1" width="14" customWidth="1"/>
    <col min="2" max="2" width="18" customWidth="1"/>
    <col min="3" max="3" width="38" customWidth="1"/>
    <col min="4" max="4" width="46" customWidth="1"/>
  </cols>
  <sheetData>
$sb  </sheetData>
$mergeXml  <autoFilter ref="A$headerRow:D$lastDataRow"/>
  <pageMargins left="0.4" right="0.4" top="0.5" bottom="0.5" header="0.2" footer="0.2"/>
  <pageSetup paperSize="9" orientation="landscape" fitToWidth="1" fitToHeight="0"/>
</worksheet>"""
    }

    private fun buildForwarderPackingSheetXml(
        exportData: ValidatedSupplierPoExport,
        thumbnailBytesProvider: (String) -> ByteArray?
    ): Pair<String, List<EmbeddedSheetImage>> {
        val sb = StringBuilder()
        val merges = mutableListOf<String>()
        val embeddedImages = mutableListOf<EmbeddedSheetImage>()
        val pathToMediaName = LinkedHashMap<String, String>()
        var row = 1

        fun emitRow(cells: List<String>, heightPt: Int = 20) {
            sb.append("    <row r=\"$row\" ht=\"$heightPt\" customHeight=\"1\">")
            cells.forEach { sb.append(it) }
            sb.append("</row>\n")
            row++
        }

        // Row 1: Title Banner (13 columns A..M, no monetary fields)
        emitRow(
            listOf(
                inlineStrCell(0, row, exportData.companyName, 2),
                blankCell(1, row, 2), blankCell(2, row, 2), blankCell(3, row, 2), blankCell(4, row, 2),
                blankCell(5, row, 2), blankCell(6, row, 2),
                inlineStrCell(7, row, "FREIGHT FORWARDER PACKING LIST", 3),
                blankCell(8, row, 3), blankCell(9, row, 3), blankCell(10, row, 3), blankCell(11, row, 3), blankCell(12, row, 3)
            ),
            heightPt = 28
        )
        merges += "A1:G1"
        merges += "H1:M1"

        val refText = if (exportData.isPartialCartonExport) {
            "Ref #: ${exportData.poNumber} | Date: ${exportData.orderDateFormatted} | SELECTED ${exportData.exportedCartonCount} OF ${exportData.allCartons.size} CTN"
        } else {
            "Ref #: ${exportData.poNumber} | Date: ${exportData.orderDateFormatted}"
        }
        emitRow(
            listOf(inlineStrCell(0, row, refText, 3)) + (1..12).map { blankCell(it, row, 3) },
            heightPt = 20
        )
        merges += "A2:M2"

        val showQty = if (exportData.exportedCartons.isNotEmpty()) exportData.exportedPackedQuantity else exportData.poTotalQuantity
        val showCtn = if (exportData.exportedCartons.isNotEmpty()) exportData.exportedCartonCount else exportData.poTotalCartons
        val showGw = if (exportData.exportedCartons.isNotEmpty()) exportData.exportedGrossWeightKg else exportData.poTotalGrossWeightKg
        val showNw = if (exportData.exportedCartons.isNotEmpty()) exportData.exportedNetWeightKg else exportData.poTotalNetWeightKg
        val showCbm = if (exportData.exportedCartons.isNotEmpty()) exportData.exportedCbm else exportData.poTotalCbm

        emitRow(
            listOf(
                inlineStrCell(0, row, "Forwarder Warehouse", 19),
                inlineStrCell(1, row, exportData.deliveryAddress, 6),
                blankCell(2, row, 6), blankCell(3, row, 6), blankCell(4, row, 6), blankCell(5, row, 6),
                inlineStrCell(6, row, "Total Cartons", 19),
                inlineStrCell(7, row, "$showCtn CTN", 8),
                inlineStrCell(8, row, "Total Qty", 19),
                inlineStrCell(9, row, "$showQty pcs", 13),
                inlineStrCell(10, row, "Total CBM", 19),
                inlineStrCell(11, row, "${showCbm.setScale(4, RoundingMode.HALF_UP)} m³", 13),
                blankCell(12, row, 13)
            ),
            heightPt = 26
        )
        merges += "B3:F3"
        merges += "L3:M3"

        emitRow(
            listOf(
                inlineStrCell(0, row, "Shipper / Origin", 19),
                inlineStrCell(1, row, "${exportData.supplierName} (${exportData.supplierPhone}) — ${exportData.supplierAddress}", 6),
                blankCell(2, row, 6), blankCell(3, row, 6), blankCell(4, row, 6), blankCell(5, row, 6),
                inlineStrCell(6, row, "Total G.W.", 19),
                inlineStrCell(7, row, "${showGw.setScale(3, RoundingMode.HALF_UP)} KG", 13),
                inlineStrCell(8, row, "Total N.W.", 19),
                inlineStrCell(9, row, "${showNw.setScale(3, RoundingMode.HALF_UP)} KG", 13),
                inlineStrCell(10, row, "Shipping Mark", 19),
                inlineStrCell(11, row, exportData.defaultShippingMark.replace("\n", " | "), 6),
                blankCell(12, row, 6)
            ),
            heightPt = 26
        )
        merges += "B4:F4"
        merges += "L4:M4"

        emitRow(listOf(blankCell(0, row)), heightPt = 8)

        // 13-Column Forwarder Table Header (NO Color, NO Unit Price / Total Money)
        // A: CTN No., B: Photo, C: Product Name, D: SKU, E: Size, F: Qty (pcs),
        // G: Gross Wt (KG), H: Net Wt (KG), I: Length (cm), J: Width (cm), K: Height (cm), L: CBM (m³), M: Shipping Mark
        val headerRow = row
        emitRow(
            listOf(
                inlineStrCell(0, row, "CTN No.", 5),
                inlineStrCell(1, row, "Photo", 5),
                inlineStrCell(2, row, "Product Name", 5),
                inlineStrCell(3, row, "SKU", 5),
                inlineStrCell(4, row, "Size", 5),
                inlineStrCell(5, row, "Qty (pcs)", 5),
                inlineStrCell(6, row, "Gross Wt (KG)", 5),
                inlineStrCell(7, row, "Net Wt (KG)", 5),
                inlineStrCell(8, row, "Length (cm)", 5),
                inlineStrCell(9, row, "Width (cm)", 5),
                inlineStrCell(10, row, "Height (cm)", 5),
                inlineStrCell(11, row, "CBM (m³)", 5),
                inlineStrCell(12, row, "Shipping Mark", 5)
            ),
            heightPt = 24
        )

        val firstDataRow = row
        if (exportData.exportedCartons.isNotEmpty()) {
            exportData.exportedCartons.forEach { ctn ->
                ctn.lines.forEachIndexed { lIdx, line ->
                    val currentZeroRow = row - 1
                    val imagePath = line.imagePath
                    val imgBytes = if (!imagePath.isNullOrBlank()) thumbnailBytesProvider(imagePath) else null
                    val hasPhoto = imgBytes != null && imgBytes.isNotEmpty()
                    if (hasPhoto && !imagePath.isNullOrBlank()) {
                        val mediaName = pathToMediaName.getOrPut(imagePath) { "image${pathToMediaName.size + 1}.jpg" }
                        embeddedImages += EmbeddedSheetImage(
                            zeroBasedRow = currentZeroRow,
                            zeroBasedCol = 1, // Column B = Photo
                            mediaName = mediaName,
                            jpegBytes = imgBytes!!
                        )
                    }

                    val zebra = ctn.sequence % 2 == 0
                    val ls = if (zebra) 20 else 6
                    val cs = if (zebra) 21 else 7
                    val qs = if (zebra) 22 else 9
                    val cbmCell = if (lIdx == 0) {
                        numberCell(11, row, ctn.cbm.setScale(4, RoundingMode.HALF_UP).toPlainString(), 12)
                    } else {
                        blankCell(11, row, cs)
                    }
                    val lenCell = if (lIdx == 0 && ctn.lengthCm > BigDecimal.ZERO) {
                        numberCell(8, row, ctn.lengthCm.stripTrailingZeros().toPlainString(), cs)
                    } else inlineStrCell(8, row, if (lIdx == 0) "-" else "", cs)
                    val widCell = if (lIdx == 0 && ctn.widthCm > BigDecimal.ZERO) {
                        numberCell(9, row, ctn.widthCm.stripTrailingZeros().toPlainString(), cs)
                    } else inlineStrCell(9, row, if (lIdx == 0) "-" else "", cs)
                    val heiCell = if (lIdx == 0 && ctn.heightCm > BigDecimal.ZERO) {
                        numberCell(10, row, ctn.heightCm.stripTrailingZeros().toPlainString(), cs)
                    } else inlineStrCell(10, row, if (lIdx == 0) "-" else "", cs)

                    emitRow(
                        listOf(
                            inlineStrCell(0, row, if (lIdx == 0) ctn.cartonNumber else ctn.cartonNumber, 8),
                            inlineStrCell(1, row, if (hasPhoto) "" else "No Photo", cs),
                            inlineStrCell(2, row, line.productName, ls),
                            inlineStrCell(3, row, line.sku, ls),
                            inlineStrCell(4, row, line.size, cs),
                            numberCell(5, row, line.quantity.toString(), qs),
                            numberCell(6, row, line.allocatedGrossWeightKg.setScale(3, RoundingMode.HALF_UP).toPlainString(), 11),
                            numberCell(7, row, line.allocatedNetWeightKg.setScale(3, RoundingMode.HALF_UP).toPlainString(), 11),
                            lenCell,
                            widCell,
                            heiCell,
                            cbmCell,
                            inlineStrCell(12, row, if (lIdx == 0) ctn.shippingMark.replace("\n", " | ") else "", ls)
                        ),
                        heightPt = if (hasPhoto) 48 else 24
                    )
                }
            }
        } else {
            exportData.items.forEachIndexed { idx, itemRow ->
                val currentZeroRow = row - 1
                val imagePath = itemRow.imagePath
                val imgBytes = if (!imagePath.isNullOrBlank()) thumbnailBytesProvider(imagePath) else null
                val hasPhoto = imgBytes != null && imgBytes.isNotEmpty()
                if (hasPhoto && !imagePath.isNullOrBlank()) {
                    val mediaName = pathToMediaName.getOrPut(imagePath) { "image${pathToMediaName.size + 1}.jpg" }
                    embeddedImages += EmbeddedSheetImage(
                        zeroBasedRow = currentZeroRow,
                        zeroBasedCol = 1,
                        mediaName = mediaName,
                        jpegBytes = imgBytes!!
                    )
                }
                val zebra = idx % 2 == 1
                val ls = if (zebra) 20 else 6
                val cs = if (zebra) 21 else 7
                val qs = if (zebra) 22 else 9
                val gwVal = (itemRow.item.grossWeightKg.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP).toPlainString()
                val nwVal = (itemRow.item.netWeightKg.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP).toPlainString()
                emitRow(
                    listOf(
                        inlineStrCell(0, row, itemRow.cartonDisplay, 8),
                        inlineStrCell(1, row, if (hasPhoto) "" else "No Photo", cs),
                        inlineStrCell(2, row, itemRow.productName, ls),
                        inlineStrCell(3, row, itemRow.sku, ls),
                        inlineStrCell(4, row, itemRow.size, cs),
                        numberCell(5, row, itemRow.quantity.toString(), qs),
                        numberCell(6, row, gwVal, 11),
                        numberCell(7, row, nwVal, 11),
                        inlineStrCell(8, row, "-", cs),
                        inlineStrCell(9, row, "-", cs),
                        inlineStrCell(10, row, "-", cs),
                        inlineStrCell(11, row, "-", cs),
                        inlineStrCell(12, row, exportData.defaultShippingMark.replace("\n", " | "), ls)
                    ),
                    heightPt = if (hasPhoto) 48 else 24
                )
            }
        }
        val lastDataRow = (row - 1).coerceAtLeast(firstDataRow)
        val hasRows = exportData.exportedCartons.isNotEmpty() || exportData.items.isNotEmpty()

        emitRow(
            listOf(
                inlineStrCell(0, row, "TOTAL", 13),
                inlineStrCell(1, row, "$showCtn CTN", 13),
                blankCell(2, row, 13),
                blankCell(3, row, 13),
                blankCell(4, row, 13),
                numberCell(5, row, showQty.toString(), 14, formula = if (hasRows) "SUM(F$firstDataRow:F$lastDataRow)" else null),
                numberCell(6, row, showGw.setScale(3, RoundingMode.HALF_UP).toPlainString(), 16, formula = if (hasRows) "SUM(G$firstDataRow:G$lastDataRow)" else null),
                numberCell(7, row, showNw.setScale(3, RoundingMode.HALF_UP).toPlainString(), 16, formula = if (hasRows) "SUM(H$firstDataRow:H$lastDataRow)" else null),
                blankCell(8, row, 13),
                blankCell(9, row, 13),
                blankCell(10, row, 13),
                numberCell(11, row, showCbm.setScale(4, RoundingMode.HALF_UP).toPlainString(), 17, formula = if (hasRows) "SUM(L$firstDataRow:L$lastDataRow)" else null),
                blankCell(12, row, 13)
            ),
            heightPt = 24
        )
        merges += "C${row - 1}:E${row - 1}"
        merges += "I${row - 1}:K${row - 1}"

        val mergeXml = buildString {
            append("  <mergeCells count=\"${merges.size}\">\n")
            merges.forEach { append("    <mergeCell ref=\"$it\"/>\n") }
            append("  </mergeCells>\n")
        }
        val drawingXml = if (embeddedImages.isNotEmpty()) "  <drawing r:id=\"rId1\"/>\n" else ""

        val fullXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheetPr><pageSetUpPr fitToPage="1"/></sheetPr>
  <sheetViews>
    <sheetView tabSelected="1" workbookViewId="0">
      <pane ySplit="$headerRow" topLeftCell="A${headerRow + 1}" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="20"/>
  <cols>
    <col min="1" max="1" width="12" customWidth="1"/>
    <col min="2" max="2" width="12" customWidth="1"/>
    <col min="3" max="3" width="30" customWidth="1"/>
    <col min="4" max="4" width="22" customWidth="1"/>
    <col min="5" max="5" width="11" customWidth="1"/>
    <col min="6" max="6" width="12" customWidth="1"/>
    <col min="7" max="7" width="15" customWidth="1"/>
    <col min="8" max="8" width="15" customWidth="1"/>
    <col min="9" max="9" width="12" customWidth="1"/>
    <col min="10" max="10" width="12" customWidth="1"/>
    <col min="11" max="11" width="12" customWidth="1"/>
    <col min="12" max="12" width="14" customWidth="1"/>
    <col min="13" max="13" width="32" customWidth="1"/>
  </cols>
  <sheetData>
$sb  </sheetData>
$mergeXml  <autoFilter ref="A$headerRow:M$lastDataRow"/>
  <pageMargins left="0.4" right="0.4" top="0.5" bottom="0.5" header="0.2" footer="0.2"/>
  <pageSetup paperSize="9" orientation="landscape" fitToWidth="1" fitToHeight="0"/>
$drawingXml</worksheet>"""
        return fullXml to embeddedImages
    }

    private fun buildForwarderCartonSummarySheetXml(exportData: ValidatedSupplierPoExport): String {
        val sb = StringBuilder()
        val merges = mutableListOf<String>()
        var row = 1
        fun emitRow(cells: List<String>, heightPt: Int = 20) {
            sb.append("    <row r=\"$row\" ht=\"$heightPt\" customHeight=\"1\">")
            cells.forEach { sb.append(it) }
            sb.append("</row>\n")
            row++
        }

        val banner = if (exportData.isPartialCartonExport) {
            "FORWARDER CARTON SUMMARY (SELECTED ${exportData.exportedCartonCount} OF ${exportData.allCartons.size} CTN) — ${exportData.poNumber}"
        } else {
            "FORWARDER CARTON SUMMARY — ${exportData.poNumber}"
        }
        emitRow(listOf(inlineStrCell(0, row, banner, 2)) + (1..11).map { blankCell(it, row, 2) }, heightPt = 26)
        merges += "A1:L1"

        val headerRow = row
        emitRow(
            listOf(
                inlineStrCell(0, row, "CTN Number", 5),
                inlineStrCell(1, row, "Carton Type", 5),
                inlineStrCell(2, row, "Contents (Product & Size / Qty)", 5),
                inlineStrCell(3, row, "Qty (pcs)", 5),
                inlineStrCell(4, row, "Gross Wt (KG)", 5),
                inlineStrCell(5, row, "Net Wt (KG)", 5),
                inlineStrCell(6, row, "Length (cm)", 5),
                inlineStrCell(7, row, "Width (cm)", 5),
                inlineStrCell(8, row, "Height (cm)", 5),
                inlineStrCell(9, row, "Dimensions", 5),
                inlineStrCell(10, row, "CBM (m³)", 5),
                inlineStrCell(11, row, "Shipping Mark", 5)
            ),
            heightPt = 24
        )

        val firstDataRow = row
        exportData.exportedCartons.forEachIndexed { idx, ctn ->
            val zebra = idx % 2 == 1
            val ls = if (zebra) 20 else 6
            val cs = if (zebra) 21 else 7
            val qs = if (zebra) 22 else 9
            // Deliberately omits Color for freight forwarder privacy
            val contentsSummary = ctn.lines.joinToString("; ") { l ->
                val sizePart = l.size.takeIf { it != "-" }
                val title = if (sizePart != null) "${l.productName} ($sizePart)" else l.productName
                "$title × ${l.quantity} pcs"
            }
            emitRow(
                listOf(
                    inlineStrCell(0, row, ctn.cartonNumber, 8),
                    inlineStrCell(1, row, if (ctn.isMixed) "Mixed" else "Single", cs),
                    inlineStrCell(2, row, contentsSummary, ls),
                    numberCell(3, row, ctn.totalQty.toString(), qs),
                    numberCell(4, row, ctn.grossWeightKg.setScale(3, RoundingMode.HALF_UP).toPlainString(), 11),
                    numberCell(5, row, ctn.netWeightKg.setScale(3, RoundingMode.HALF_UP).toPlainString(), 11),
                    numberCell(6, row, ctn.lengthCm.stripTrailingZeros().toPlainString(), cs),
                    numberCell(7, row, ctn.widthCm.stripTrailingZeros().toPlainString(), cs),
                    numberCell(8, row, ctn.heightCm.stripTrailingZeros().toPlainString(), cs),
                    inlineStrCell(9, row, ctn.dimensionsDisplay, cs),
                    numberCell(10, row, ctn.cbm.setScale(4, RoundingMode.HALF_UP).toPlainString(), 12),
                    inlineStrCell(11, row, ctn.shippingMark.replace("\n", " | "), ls)
                ),
                heightPt = 28
            )
        }
        val lastDataRow = (row - 1).coerceAtLeast(firstDataRow)
        val hasRows = exportData.exportedCartons.isNotEmpty()
        emitRow(
            listOf(
                inlineStrCell(0, row, "TOTAL", 13),
                inlineStrCell(1, row, "${exportData.exportedCartonCount} CTN", 13),
                inlineStrCell(2, row, "Freight Cargo Summary", 13),
                numberCell(3, row, exportData.exportedPackedQuantity.toString(), 14, formula = if (hasRows) "SUM(D$firstDataRow:D$lastDataRow)" else null),
                numberCell(4, row, exportData.exportedGrossWeightKg.setScale(3, RoundingMode.HALF_UP).toPlainString(), 16, formula = if (hasRows) "SUM(E$firstDataRow:E$lastDataRow)" else null),
                numberCell(5, row, exportData.exportedNetWeightKg.setScale(3, RoundingMode.HALF_UP).toPlainString(), 16, formula = if (hasRows) "SUM(F$firstDataRow:F$lastDataRow)" else null),
                blankCell(6, row, 13), blankCell(7, row, 13), blankCell(8, row, 13), blankCell(9, row, 13),
                numberCell(10, row, exportData.exportedCbm.setScale(4, RoundingMode.HALF_UP).toPlainString(), 17, formula = if (hasRows) "SUM(K$firstDataRow:K$lastDataRow)" else null),
                blankCell(11, row, 13)
            ),
            heightPt = 24
        )
        merges += "G${row - 1}:J${row - 1}"

        val mergeXml = buildString {
            append("  <mergeCells count=\"${merges.size}\">\n")
            merges.forEach { append("    <mergeCell ref=\"$it\"/>\n") }
            append("  </mergeCells>\n")
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheetPr><pageSetUpPr fitToPage="1"/></sheetPr>
  <sheetViews>
    <sheetView workbookViewId="0">
      <pane ySplit="$headerRow" topLeftCell="A${headerRow + 1}" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="20"/>
  <cols>
    <col min="1" max="1" width="12" customWidth="1"/>
    <col min="2" max="2" width="11" customWidth="1"/>
    <col min="3" max="3" width="38" customWidth="1"/>
    <col min="4" max="4" width="12" customWidth="1"/>
    <col min="5" max="5" width="14" customWidth="1"/>
    <col min="6" max="6" width="14" customWidth="1"/>
    <col min="7" max="7" width="11" customWidth="1"/>
    <col min="8" max="8" width="11" customWidth="1"/>
    <col min="9" max="9" width="11" customWidth="1"/>
    <col min="10" max="10" width="16" customWidth="1"/>
    <col min="11" max="11" width="13" customWidth="1"/>
    <col min="12" max="12" width="30" customWidth="1"/>
  </cols>
  <sheetData>
$sb  </sheetData>
$mergeXml  <autoFilter ref="A$headerRow:L$lastDataRow"/>
  <pageMargins left="0.4" right="0.4" top="0.5" bottom="0.5" header="0.2" footer="0.2"/>
  <pageSetup paperSize="9" orientation="landscape" fitToWidth="1" fitToHeight="0"/>
</worksheet>"""
    }

    private fun buildForwarderDeliverySheetXml(exportData: ValidatedSupplierPoExport): String {
        val sb = StringBuilder()
        val merges = mutableListOf<String>()
        var row = 1
        fun emitRow(cells: List<String>, heightPt: Int = 20) {
            sb.append("    <row r=\"$row\" ht=\"$heightPt\" customHeight=\"1\">")
            cells.forEach { sb.append(it) }
            sb.append("</row>\n")
            row++
        }

        emitRow(listOf(inlineStrCell(0, row, "FORWARDER PICKUP & SHIPPING MARK — ${exportData.poNumber}", 2), blankCell(1, row, 2), blankCell(2, row, 2), blankCell(3, row, 2)), heightPt = 26)
        merges += "A1:D1"

        emitRow(listOf(inlineStrCell(0, row, "Consignee / Company", 19), inlineStrCell(1, row, "${exportData.companyName} | Tel: ${exportData.companyPhone}", 6), blankCell(2, row, 6), blankCell(3, row, 6)), heightPt = 26)
        merges += "B2:D2"

        emitRow(listOf(inlineStrCell(0, row, "Forwarder Warehouse", 19), inlineStrCell(1, row, exportData.deliveryAddress, 6), blankCell(2, row, 6), blankCell(3, row, 6)), heightPt = 28)
        merges += "B3:D3"

        emitRow(listOf(inlineStrCell(0, row, "Pickup From Shipper", 19), inlineStrCell(1, row, "${exportData.supplierName} | Tel: ${exportData.supplierPhone} | ${exportData.supplierAddress}", 6), blankCell(2, row, 6), blankCell(3, row, 6)), heightPt = 28)
        merges += "B4:D4"

        emitRow(listOf(inlineStrCell(0, row, "Default Shipping Mark", 19), inlineStrCell(1, row, exportData.defaultShippingMark, 6), blankCell(2, row, 6), blankCell(3, row, 6)), heightPt = 32)
        merges += "B5:D5"

        emitRow(listOf(blankCell(0, row)), heightPt = 8)

        val headerRow = row
        emitRow(
            listOf(
                inlineStrCell(0, row, "CTN Number", 5),
                inlineStrCell(1, row, "Mark Source", 5),
                inlineStrCell(2, row, "Carton Shipping Mark", 5),
                inlineStrCell(3, row, "Cargo Contents & Qty", 5)
            ),
            heightPt = 24
        )

        exportData.exportedCartons.forEachIndexed { idx, ctn ->
            val zebra = idx % 2 == 1
            val ls = if (zebra) 20 else 6
            val cs = if (zebra) 21 else 7
            val contents = ctn.lines.joinToString("; ") { l ->
                val sz = l.size.takeIf { it != "-" }
                if (sz != null) "${l.productName} ($sz) × ${l.quantity} pcs" else "${l.productName} × ${l.quantity} pcs"
            }
            emitRow(
                listOf(
                    inlineStrCell(0, row, ctn.cartonNumber, 8),
                    inlineStrCell(1, row, if (ctn.isCustomShippingMark) "Custom Override" else "PO Default", cs),
                    inlineStrCell(2, row, ctn.shippingMark, ls),
                    inlineStrCell(3, row, contents, ls)
                ),
                heightPt = 34
            )
        }
        val lastDataRow = (row - 1).coerceAtLeast(headerRow)
        val mergeXml = buildString {
            append("  <mergeCells count=\"${merges.size}\">\n")
            merges.forEach { append("    <mergeCell ref=\"$it\"/>\n") }
            append("  </mergeCells>\n")
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheetPr><pageSetUpPr fitToPage="1"/></sheetPr>
  <sheetViews>
    <sheetView workbookViewId="0">
      <pane ySplit="$headerRow" topLeftCell="A${headerRow + 1}" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="20"/>
  <cols>
    <col min="1" max="1" width="14" customWidth="1"/>
    <col min="2" max="2" width="18" customWidth="1"/>
    <col min="3" max="3" width="38" customWidth="1"/>
    <col min="4" max="4" width="46" customWidth="1"/>
  </cols>
  <sheetData>
$sb  </sheetData>
$mergeXml  <autoFilter ref="A$headerRow:D$lastDataRow"/>
  <pageMargins left="0.4" right="0.4" top="0.5" bottom="0.5" header="0.2" footer="0.2"/>
  <pageSetup paperSize="9" orientation="landscape" fitToWidth="1" fitToHeight="0"/>
</worksheet>"""
    }

    /**
     * Company Internal Audit Order Sheet (.xlsx)
     * Includes BDT amounts, FX rates, weight allocation, handling, sample costs, and landed costs.
     */
    fun generateCompanyInternalOrderExcel(
        context: Context,
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?
    ): File {
        val file = File(getExcelDir(context), "Internal_PO_${ImportPdfGenerator.sanitizeFileName(order.poNumber)}.xlsx")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        val sheetXml = buildInternalSheetXml(order, items, productsMap, supplier, businessProfile, dateFormat)

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-officedocument.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
            writeZipEntry(zos, "[Content_Types].xml", contentTypesXml)

            val rootRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            writeZipEntry(zos, "_rels/.rels", rootRelsXml)

            val wbRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", wbRelsXml)

            val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Internal PO Audit" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""
            writeZipEntry(zos, "xl/workbook.xml", workbookXml)

            val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><name val="Calibri"/><sz val="11"/></font>
    <font><b/><name val="Calibri"/><sz val="11"/></font>
  </fonts>
  <fills count="2">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/></border>
  </borders>
  <cellStyleXfs count="1"><xf/></cellStyleXfs>
  <cellXfs count="2">
    <xf fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>"""
            writeZipEntry(zos, "xl/styles.xml", stylesXml)

            writeZipEntry(zos, "xl/worksheets/sheet1.xml", sheetXml)
        }
        return file
    }

    private fun buildInternalSheetXml(
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        dateFormat: SimpleDateFormat
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
""")
        var row = 1
        fun addRow(cols: List<String>, isBold: Boolean = false) {
            val s = if (isBold) " s=\"1\"" else ""
            sb.append("    <row r=\"$row\">")
            cols.forEachIndexed { idx, col ->
                val colLetter = ('A' + idx).toString()
                sb.append("<c r=\"$colLetter$row\" t=\"inlineStr\"$s><is><t>${escapeXml(col)}</t></is></c>")
            }
            sb.append("</row>\n")
            row++
        }

        addRow(listOf("INTERNAL PURCHASE ORDER AUDIT & COSTING SHEET", order.poNumber), isBold = true)
        addRow(listOf("Company", businessProfile?.companyName ?: "ADVANCE ERP ENTERPRISE"))
        addRow(listOf("Supplier", supplier?.companyName ?: "China Supplier"))
        addRow(listOf("Order Date", dateFormat.format(Date(order.orderDate))))
        addRow(listOf("Status", order.status))
        addRow(listOf("Base Currency", order.currency))
        addRow(listOf("Exchange Rate (1 ${order.currency} -> BDT)", order.exchangeRateToBdt))
        addRow(listOf("Total Cartons", "${order.totalCartons} CTN"))
        addRow(listOf("Gross Weight", "${order.totalGrossWeightKg} KG"))
        addRow(listOf("Net Weight", "${order.totalNetWeightKg} KG"))
        addRow(listOf("Volume (CBM)", "${order.totalCbm} CBM"))
        addRow(listOf("Internal OPEX Policy", "Warehouse Rent is recognized as operational expense (OPEX) and deducted from Net Profit."))
        addRow(listOf("", ""))

        addRow(
            listOf(
                "#",
                "Product Name",
                "SKU",
                "Qty",
                "Unit Price (${order.currency})",
                "Total (${order.currency})",
                "Total (BDT)",
                "Cartons",
                "Weight (KG)"
            ),
            isBold = true
        )

        items.forEachIndexed { index, item ->
            val prod = productsMap[item.productId]
            val pName = prod?.nameEn ?: item.productId
            val unitStr = Formatters.formatMinorUnit(item.unitPriceMinor)
            val totalForeign = Formatters.formatMinorUnit(item.totalAmountMinor)
            val totalBdt = Formatters.formatMinorUnit(item.totalBdtAmountMinor)
            val ctnStr = "${item.cartonQuantity}"
            val wtStr = item.grossWeightKg

            addRow(
                listOf(
                    (index + 1).toString(),
                    pName,
                    prod?.baseSku ?: "SKU",
                    item.quantity.toString(),
                    unitStr,
                    totalForeign,
                    "৳ $totalBdt",
                    ctnStr,
                    wtStr
                )
            )
        }

        addRow(listOf("", ""))
        val totalQty = items.sumOf { it.quantity }
        val origTotal = "${order.currency} ${Formatters.formatMinorUnit(order.totalAmountMinor)}"
        val bdtTotal = "৳ ${Formatters.formatMinorUnit(order.totalBdtAmountMinor)}"
        addRow(listOf("TOTALS", "", "", totalQty.toString(), "", origTotal, bdtTotal, "${order.totalCartons} CTN", "${order.totalGrossWeightKg} KG"), isBold = true)

        sb.append("""  </sheetData>
</worksheet>""")
        return sb.toString()
    }
}
