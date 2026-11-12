package com.pocketdev.advance_erp.core.util

import android.content.Context
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
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

    /**
     * Supplier Commercial Order Sheet (.xlsx)
     * Strictly in CNY / USD, hides internal BDT, exchange rates, and margins.
     */
    fun generateSupplierOrderExcel(
        context: Context,
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?
    ): File {
        val file = File(getExcelDir(context), "Supplier_PO_${order.poNumber}.xlsx")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        val sheetXml = buildSupplierSheetXml(order, items, productsMap, supplier, businessProfile, dateFormat)

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // [Content_Types].xml
            val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
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

            // xl/worksheets/sheet1.xml
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", sheetXml)
        }
        return file
    }

    private fun buildSupplierSheetXml(
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

        addRow(listOf("COMMERCIAL PURCHASE ORDER / 采购订单", order.poNumber), isBold = true)
        addRow(listOf("Buyer Enterprise", businessProfile?.companyName ?: "Import Enterprise"))
        addRow(listOf("Supplier / Factory", supplier?.companyName ?: "China Manufacturing Supplier"))
        addRow(listOf("Contact Person", supplier?.contactPerson ?: "Export Dept"))
        addRow(listOf("Supplier Phone", supplier?.phone ?: "-"))
        addRow(listOf("Order Date", dateFormat.format(Date(order.orderDate))))
        addRow(listOf("Order Status", order.status))
        addRow(listOf("Commercial Currency", order.currency))
        addRow(listOf("Total Cartons", "${order.totalCartons} CTN"))
        addRow(listOf("Est Gross Weight", "${order.totalGrossWeightKg} KG"))
        addRow(listOf("Payment Terms", order.notes ?: "30% Advance Deposit, 70% before dispatch"))
        addRow(listOf("Forwarder Delivery Address", "Guangzhou Baiyun / Yiwu Port Cargo Terminal Warehouse"))
        addRow(listOf("Shipping Marks", "[${businessProfile?.companyName ?: "IMPORT CO"}] / ${order.poNumber} / CTG PORT"))
        addRow(listOf("", ""))

        // Header
        addRow(
            listOf("#", "Product Description", "Model / Base SKU", "Packing (Cartons)", "Quantity (PCS)", "Unit Price (${order.currency})", "Total Amount (${order.currency})"),
            isBold = true
        )

        items.forEachIndexed { index, item ->
            val prod = productsMap[item.productId]
            val pName = prod?.nameEn ?: item.productId
            val unitStr = Formatters.formatMinorUnit(item.unitPriceMinor)
            val totalStr = Formatters.formatMinorUnit(item.totalAmountMinor)
            val ctnStr = if (item.cartonQuantity > 0) "${item.cartonQuantity} CTN" else "-"
            addRow(
                listOf(
                    (index + 1).toString(),
                    pName,
                    prod?.baseSku ?: "SKU",
                    ctnStr,
                    item.quantity.toString(),
                    unitStr,
                    totalStr
                )
            )
        }

        addRow(listOf("", ""))
        val totalQty = items.sumOf { it.quantity }
        val grandTotalStr = "${order.currency} ${Formatters.formatMinorUnit(order.totalAmountMinor)}"
        addRow(listOf("TOTAL PCS", totalQty.toString(), "", "", "", "GRAND TOTAL", grandTotalStr), isBold = true)

        sb.append("""  </sheetData>
</worksheet>""")
        return sb.toString()
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
        val file = File(getExcelDir(context), "Internal_PO_${order.poNumber}.xlsx")
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
