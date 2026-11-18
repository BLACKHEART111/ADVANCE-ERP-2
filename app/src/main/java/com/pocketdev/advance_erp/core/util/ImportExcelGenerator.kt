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
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ImportExcelGenerator {

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun getExcelDir(context: Context): File {
        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        return dir
    }

    private fun writeZipEntry(zos: ZipOutputStream, path: String, content: String) {
        val entry = ZipEntry(path)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes)
        zos.closeEntry()
    }

    private data class EmbeddedSheetImage(
        val zeroBasedRow: Int,
        val zeroBasedCol: Int,
        val mediaName: String,
        val jpegBytes: ByteArray
    )

    private fun writeZipBinaryEntry(zos: ZipOutputStream, path: String, bytes: ByteArray) {
        val entry = ZipEntry(path)
        zos.putNextEntry(entry)
        zos.write(bytes)
        zos.closeEntry()
    }

    /**
     * Supplier Commercial Order Sheet (.xlsx)
     * Strictly in CNY / USD, hides internal BDT, exchange rates, and margins.
     * Follows the exact same columns, info sections, and data rules as Supplier Commercial PDF.
     */
    fun generateSupplierOrderExcel(
        context: Context,
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        variantsMap: Map<String, ProductVariant> = emptyMap(),
        itemImagePaths: Map<String, String> = emptyMap(),
        cartons: List<ShipmentCarton> = emptyList()
    ): File {
        val file = File(getExcelDir(context), "Supplier_PO_${ImportPdfGenerator.sanitizeFileName(order.poNumber)}.xlsx")
        return generateSupplierOrderExcelToFile(
            file = file,
            order = order,
            items = items,
            productsMap = productsMap,
            supplier = supplier,
            businessProfile = businessProfile,
            variantsMap = variantsMap,
            itemImagePaths = itemImagePaths,
            cartons = cartons
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
        }
    ): File {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        val (sheetXml, embeddedImages) = buildSupplierSheetXml(
            order = order,
            items = items,
            productsMap = productsMap,
            supplier = supplier,
            businessProfile = businessProfile,
            dateFormat = dateFormat,
            variantsMap = variantsMap,
            itemImagePaths = itemImagePaths,
            cartons = cartons,
            thumbnailBytesProvider = thumbnailBytesProvider
        )
        val hasImages = embeddedImages.isNotEmpty()

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // [Content_Types].xml
            val jpgDefaultXml = if (hasImages) "  <Default Extension=\"jpg\" ContentType=\"image/jpeg\"/>\n" else ""
            val drawingOverrideXml = if (hasImages) "  <Override PartName=\"/xl/drawings/drawing1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/>\n" else ""
            val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-officedocument.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
$jpgDefaultXml  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
$drawingOverrideXml</Types>"""
            writeZipEntry(zos, "[Content_Types].xml", contentTypesXml)

            // _rels/.rels
            val rootRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            writeZipEntry(zos, "_rels/.rels", rootRelsXml)

            // xl/_rels/workbook.xml.rels
            val wbRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", wbRelsXml)

            // xl/workbook.xml
            val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Supplier Commercial Order" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""
            writeZipEntry(zos, "xl/workbook.xml", workbookXml)

            // xl/styles.xml
            val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="3">
    <font><name val="Calibri"/><sz val="11"/></font>
    <font><b/><name val="Calibri"/><sz val="11"/></font>
    <font><b/><color rgb="FF312E81"/><name val="Calibri"/><sz val="11"/></font>
  </fonts>
  <fills count="4">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFEDE9FE"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF3F4F6"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/></border>
    <border>
      <left style="thin"><color rgb="FFE5E7EB"/></left>
      <right style="thin"><color rgb="FFE5E7EB"/></right>
      <top style="thin"><color rgb="FFE5E7EB"/></top>
      <bottom style="thin"><color rgb="FFE5E7EB"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1"><xf/></cellStyleXfs>
  <cellXfs count="8">
    <xf fontId="0" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
    <xf fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
    <xf fontId="2" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    <xf fontId="1" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
    <xf fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
    <xf fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    <xf fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <xf fontId="1" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
  </cellXfs>
</styleSheet>"""
            writeZipEntry(zos, "xl/styles.xml", stylesXml)

            // xl/worksheets/sheet1.xml
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", sheetXml)

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
                    val (relId, _) = relPair
                    drawingRelsSb.append("  <Relationship Id=\"$relId\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"../media/$mediaName\"/>\n")
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

    private fun buildSupplierDrawingXml(
        embeddedImages: List<EmbeddedSheetImage>,
        mediaRelIds: Map<String, String>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/openxmlformats/drawingml/2006/spreadsheetDrawing" xmlns:a="http://schemas.openxmlformats.org/openxmlformats/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
""")
        val imgSizeEmu = 457200L // 36pt (~48px) square
        val colOffEmu = 133350L
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

    private fun buildSupplierSheetXml(
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        dateFormat: SimpleDateFormat,
        variantsMap: Map<String, ProductVariant>,
        itemImagePaths: Map<String, String>,
        cartons: List<ShipmentCarton>,
        thumbnailBytesProvider: (String) -> ByteArray?
    ): Pair<String, List<EmbeddedSheetImage>> {
        val isLocalOrder = order.currency == "BDT"
        val defaultForwarder = if (isLocalOrder) {
            "Main Warehouse, Dhaka, Bangladesh"
        } else {
            PoNotesMetadataParser.DEFAULT_CHINA_FORWARDER
        }
        val parsedNotes = PoNotesMetadataParser.parse(order.notes, defaultForwarder)
        val shippingMark = PoNotesMetadataParser.buildShippingMark(
            companyName = businessProfile?.companyName,
            poNumber = order.poNumber,
            isLocalOrder = isLocalOrder
        )

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <cols>
    <col min="1" max="1" width="28" customWidth="1"/>
    <col min="2" max="2" width="12" customWidth="1"/>
    <col min="3" max="3" width="14" customWidth="1"/>
    <col min="4" max="4" width="12" customWidth="1"/>
    <col min="5" max="5" width="18" customWidth="1"/>
    <col min="6" max="6" width="13" customWidth="1"/>
    <col min="7" max="7" width="16" customWidth="1"/>
    <col min="8" max="8" width="18" customWidth="1"/>
  </cols>
  <sheetData>
""")
        var row = 1
        fun addStyledRow(cols: List<Pair<String, Int>>, heightPt: Int? = null) {
            val htAttr = if (heightPt != null) " ht=\"$heightPt\" customHeight=\"1\"" else ""
            sb.append("    <row r=\"$row\"$htAttr>")
            cols.forEachIndexed { idx, (text, styleId) ->
                val colLetter = ('A' + idx).toString()
                val styleAttr = if (styleId > 0) " s=\"$styleId\"" else ""
                sb.append("<c r=\"$colLetter$row\" t=\"inlineStr\"$styleAttr><is><t>${escapeXml(text)}</t></is></c>")
            }
            sb.append("</row>\n")
            row++
        }

        fun addRow(cols: List<String>, isBold: Boolean = false, heightPt: Int? = null) {
            val styleId = if (isBold) 1 else 0
            addStyledRow(cols.map { it to styleId }, heightPt)
        }

        val embeddedImages = mutableListOf<EmbeddedSheetImage>()
        val pathToMediaName = mutableMapOf<String, String>()

        // Header Block
        val docTitle = if (isLocalOrder) "LOCAL PURCHASE ORDER" else "SUPPLIER PURCHASE ORDER / 采购订单"
        addRow(listOf(docTitle, order.poNumber), isBold = true)
        addRow(listOf("Buyer Enterprise", businessProfile?.companyName ?: "Import Enterprise"))
        addRow(listOf("Supplier / Factory", supplier?.companyName ?: if (isLocalOrder) "Local Supplier" else "China Manufacturing Supplier"))
        addRow(listOf("Contact Person", supplier?.contactPerson ?: "Export Dept"))
        addRow(listOf("Supplier Phone", supplier?.phone ?: "-"))
        addRow(listOf("Order Date", dateFormat.format(Date(order.orderDate))))
        addRow(listOf("Order Status", order.status))
        addRow(listOf("Commercial Currency", order.currency))
        val hasGrossWeight = (order.totalGrossWeightKg.toDoubleOrNull() ?: 0.0) > 0.0
        if (order.totalCartons > 0) {
            addRow(listOf("Total Cartons", "${order.totalCartons} CTN"))
        }
        if (hasGrossWeight) {
            addRow(listOf("Est. Gross Weight", "${order.totalGrossWeightKg} KG"))
        }
        addRow(listOf("", ""))

        // 8-Column Product Table Header (matches PDF 100%)
        addStyledRow(
            listOf(
                "PRODUCT NAME" to 2,
                "PRODUCT PHOTO" to 2,
                "COLOUR" to 2,
                "SIZE" to 2,
                "PACKING" to 2,
                "QUANTITY" to 2,
                "UNIT PRICE (${order.currency})" to 2,
                "TOTAL (${order.currency})" to 2
            ),
            heightPt = 24
        )

        items.forEachIndexed { index, item ->
            val prod = productsMap[item.productId]
            val variant = item.variantId?.let { variantsMap[it] }
            val pName = prod?.nameEn ?: item.productId
            val skuStr = variant?.sku?.takeIf { it.isNotBlank() } ?: prod?.baseSku?.takeIf { it.isNotBlank() } ?: "-"
            val nameWithSku = "${index + 1}. $pName\nSKU: $skuStr"

            val colorStr = variant?.color?.takeIf { it.isNotBlank() } ?: "-"
            val sizeStr = variant?.size?.takeIf { it.isNotBlank() } ?: "-"
            val packingStr = when {
                item.cartonQuantity > 0 && item.pcsPerCarton > 0 -> "${item.cartonQuantity} CTN (${item.pcsPerCarton}/ctn)"
                item.cartonQuantity > 0 -> "${item.cartonQuantity} CTN"
                item.pcsPerCarton > 0 -> "${item.pcsPerCarton} pcs/ctn"
                else -> "-"
            }
            val unitStr = Formatters.formatMinorUnit(item.unitPriceMinor)
            val totalStr = Formatters.formatMinorUnit(item.totalAmountMinor)

            val imagePath = itemImagePaths[item.id]
            val imgBytes = if (!imagePath.isNullOrBlank()) thumbnailBytesProvider(imagePath) else null
            val currentZeroRow = row - 1
            val hasRowPhoto = imgBytes != null && imgBytes.isNotEmpty()
            if (hasRowPhoto && !imagePath.isNullOrBlank()) {
                val mediaName = pathToMediaName.getOrPut(imagePath) {
                    "image${pathToMediaName.size + 1}.jpg"
                }
                embeddedImages.add(
                    EmbeddedSheetImage(
                        zeroBasedRow = currentZeroRow,
                        zeroBasedCol = 1, // Column B = Product Photo
                        mediaName = mediaName,
                        jpegBytes = imgBytes!!
                    )
                )
            }

            addStyledRow(
                listOf(
                    nameWithSku to 4,
                    (if (hasRowPhoto) "" else "-") to 5,
                    colorStr to 5,
                    sizeStr to 5,
                    packingStr to 5,
                    item.quantity.toString() to 5,
                    unitStr to 6,
                    totalStr to 6
                ),
                heightPt = if (hasRowPhoto) 48 else 30
            )
        }

        // Totals Row
        val totalQty = items.sumOf { it.quantity }
        val totalCtn = order.totalCartons
        val grandTotalStr = "${order.currency} ${Formatters.formatMinorUnit(order.totalAmountMinor)}"
        addRow(listOf("", ""))
        addStyledRow(
            listOf(
                "TOTAL QUANTITY" to 3,
                "$totalQty PCS" to 3,
                (if (totalCtn > 0) "TOTAL CARTONS" else "") to 3,
                (if (totalCtn > 0) "$totalCtn CTN" else "") to 3,
                (if (hasGrossWeight) "GROSS WEIGHT: ${order.totalGrossWeightKg} KG" else "") to 3,
                "" to 3,
                "GRAND TOTAL" to 7,
                grandTotalStr to 7
            ),
            heightPt = 22
        )

        // Delivery Info Section
        addRow(listOf("", ""))
        addStyledRow(listOf("DELIVERY INFO" to 3, "" to 3), heightPt = 20)
        val addressLabel = if (isLocalOrder) "Delivery Address" else "China Warehouse Address"
        addRow(listOf(addressLabel, parsedNotes.forwarderAddress))
        addRow(listOf("Shipping Mark", shippingMark))

        // Payment Terms Section
        addRow(listOf("", ""))
        addStyledRow(listOf("PAYMENT TERMS" to 3, "" to 3), heightPt = 20)
        val paymentTermsValue = parsedNotes.rawTermsText.ifBlank {
            if (isLocalOrder) "Cash / Bank Transfer as agreed" else "30% Advance + 70% Balance"
        }
        addRow(listOf("Payment Terms", paymentTermsValue))

        // Note Section
        addRow(listOf("", ""))
        addStyledRow(listOf("NOTE" to 3, "" to 3), heightPt = 20)
        val noteValue = parsedNotes.userNotes.ifBlank {
            "Please confirm product colour, size & packing details before dispatch."
        }
        addRow(listOf("Note", noteValue))

        // Mixed Carton Packing Breakdown (if applicable)
        val mixedCartonGroups = cartons
            .groupBy { it.cartonNumber }
            .filter { (_, entries) -> entries.any { it.isMixedCarton } || entries.size > 1 }
            .toSortedMap()
        if (mixedCartonGroups.isNotEmpty()) {
            addRow(listOf("", ""))
            addStyledRow(listOf("MIXED CARTON PACKING BREAKDOWN" to 3, "" to 3), heightPt = 20)
            mixedCartonGroups.forEach { (cartonNo, entries) ->
                val summaryText = entries.joinToString(", ") { entry ->
                    val prodName = productsMap[entry.productId]?.nameEn ?: entry.productId
                    val v = entry.variantId?.let { variantsMap[it] }
                    val vDesc = listOfNotNull(v?.color?.takeIf { it.isNotBlank() }, v?.size?.takeIf { it.isNotBlank() })
                        .joinToString("/")
                    val label = if (vDesc.isNotBlank()) "$prodName ($vDesc)" else prodName
                    "$label x ${entry.quantity} pcs"
                }
                addRow(listOf("Carton #$cartonNo", summaryText))
            }
        }

        sb.append("  </sheetData>\n")
        if (embeddedImages.isNotEmpty()) {
            sb.append("  <drawing r:id=\"rId1\"/>\n")
        }
        sb.append("</worksheet>")
        return sb.toString() to embeddedImages
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
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
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

        // Table Header
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
