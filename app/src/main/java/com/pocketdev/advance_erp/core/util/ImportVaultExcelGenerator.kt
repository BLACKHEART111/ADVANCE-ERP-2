package com.pocketdev.advance_erp.core.util

import android.content.Context
import com.pocketdev.advance_erp.core.security.ImportVaultAccessGuard
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.domain.model.StockAgingBucket
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Generates an 8-sheet OpenXML (.xlsx) workbook reconciled 1:1 with the Import Vault screens:
 * Summary, Products, Variants, Sales Allocations, Expenses, Stock, Receivables, Documents Index.
 * Pure-JVM exportPackageToXlsxFile() works directly in unit tests without Robolectric.
 */
object ImportVaultExcelGenerator {

    val REQUIRED_SHEET_NAMES = listOf(
        "Summary",
        "Products",
        "Variants",
        "Sales Allocations",
        "Expenses",
        "Stock",
        "Receivables",
        "Documents Index"
    )

    internal fun excelColName(zeroBasedIndex: Int): String {
        var idx = zeroBasedIndex
        val sb = StringBuilder()
        while (idx >= 0) {
            sb.append(('A' + (idx % 26)))
            idx = (idx / 26) - 1
        }
        return sb.reverse().toString()
    }

    internal fun minorToDecimalString(minor: Long): String =
        BigDecimal.valueOf(minor, 2).toPlainString()

    private fun escapeXml(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    fun exportToXlsx(
        context: Context,
        pkg: VaultExportPackage,
        role: UserRole = UserRole.OWNER_ADMIN
    ): File {
        ImportVaultAccessGuard.requireVaultAccess(role)
        val dir = ImportVaultAttachmentFileStore.resolveExportCacheDir(context.cacheDir)
        val slug = ControlCenterExcelGenerator.sanitizeExportFileSlug(
            pkg.scopeReference,
            pkg.scope.name.lowercase(Locale.US)
        )
        val outFile = File(dir, "vault_${pkg.scope.name.lowercase(Locale.US)}_${slug}_${System.currentTimeMillis()}.xlsx")
        return exportPackageToXlsxFile(pkg = pkg, outputFile = outFile, role = role)
    }

    fun exportPackageToXlsxFile(
        pkg: VaultExportPackage,
        outputFile: File,
        role: UserRole = UserRole.OWNER_ADMIN
    ): File {
        ImportVaultAccessGuard.requireVaultAccess(role)
        outputFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

        val dateTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val dateOnlyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val sheetsXml = listOf(
            buildSummarySheetXml(pkg, dateTimeFmt, dateOnlyFmt),
            buildProductsSheetXml(pkg),
            buildVariantsSheetXml(pkg),
            buildSalesAllocationsSheetXml(pkg, dateOnlyFmt),
            buildExpensesSheetXml(pkg, dateOnlyFmt),
            buildStockSheetXml(pkg, dateOnlyFmt),
            buildReceivablesSheetXml(pkg, dateOnlyFmt),
            buildDocumentsIndexSheetXml(pkg, dateTimeFmt)
        )

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            writeZipEntry(zos, "[Content_Types].xml", buildContentTypesXml(sheetsXml.size))
            writeZipEntry(zos, "_rels/.rels", ROOT_RELS_XML)
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml(sheetsXml.size))
            writeZipEntry(zos, "xl/workbook.xml", buildWorkbookXml(REQUIRED_SHEET_NAMES))
            writeZipEntry(zos, "xl/styles.xml", STYLES_XML)
            sheetsXml.forEachIndexed { idx, xml ->
                writeZipEntry(zos, "xl/worksheets/sheet${idx + 1}.xml", xml)
            }
        }

        return outputFile
    }

    private fun writeZipEntry(zos: ZipOutputStream, path: String, content: String) {
        zos.putNextEntry(ZipEntry(path))
        zos.write(content.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
    }

    private sealed interface XlsxCell {
        data class Text(val value: String, val bold: Boolean = false) : XlsxCell
        data class Num(val rawNumber: String, val bold: Boolean = false) : XlsxCell
    }

    private fun renderSheetXml(rows: List<List<XlsxCell>>): String {
        val sb = StringBuilder()
        sb.append(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
"""
        )
        rows.forEachIndexed { rowIdx, cells ->
            val r = rowIdx + 1
            sb.append("    <row r=\"").append(r).append("\">")
            cells.forEachIndexed { colIdx, cell ->
                val ref = "${excelColName(colIdx)}$r"
                when (cell) {
                    is XlsxCell.Text -> {
                        val s = if (cell.bold) " s=\"1\"" else ""
                        sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"").append(s)
                            .append("><is><t>").append(escapeXml(cell.value)).append("</t></is></c>")
                    }
                    is XlsxCell.Num -> {
                        val s = if (cell.bold) " s=\"1\"" else ""
                        sb.append("<c r=\"").append(ref).append("\" t=\"n\"").append(s)
                            .append("><v>").append(cell.rawNumber).append("</v></c>")
                    }
                }
            }
            sb.append("</row>\n")
        }
        sb.append("  </sheetData>\n</worksheet>")
        return sb.toString()
    }

    private fun textRow(vararg values: String, bold: Boolean = false): List<XlsxCell> =
        values.map { XlsxCell.Text(it, bold = bold) }

    private fun moneyMetricRow(label: String, minor: Long, bold: Boolean = false): List<XlsxCell> = listOf(
        XlsxCell.Text(label, bold = bold),
        XlsxCell.Num(minorToDecimalString(minor), bold = bold),
        XlsxCell.Text("BDT", bold = bold)
    )

    private fun buildSummarySheetXml(
        pkg: VaultExportPackage,
        dateTimeFmt: SimpleDateFormat,
        dateOnlyFmt: SimpleDateFormat
    ): String {
        val t = pkg.totals
        val rows = mutableListOf<List<XlsxCell>>()
        rows += textRow("ADVANCE ERP · IMPORT VAULT EXPORT", pkg.titleEn, "Metric / Value", bold = true)
        rows += textRow("Company Name", pkg.businessProfile?.companyName ?: "ADVANCE ERP ENTERPRISE")
        rows += textRow("Title (EN)", pkg.titleEn)
        rows += textRow("Title (BN)", pkg.titleBn)
        rows += textRow("Scope", pkg.scope.name)
        rows += textRow("Scope Reference", pkg.scopeReference)
        rows += textRow("Currency", pkg.currencyCode)
        rows += textRow("As-Of Date", dateOnlyFmt.format(Date(pkg.asOfMillis)))
        rows += textRow("Selected Filters", pkg.filterSummary)
        rows += textRow("Generated On", dateTimeFmt.format(Date()))
        rows += textRow("Cost Finalization Status", t.costFinalizationStatusLabel)
        rows += textRow("Pending Receiving", if (t.hasPendingReceiving) "YES" else "NO")
        rows += textRow("Overhead Allocation Mode", "${t.expenseAllocationMode.name} (${t.profitLabel})")
        rows += textRow("", "", "")

        rows += textRow("FINANCIAL RECONCILIATION SUMMARY", "AMOUNT", "CURRENCY", bold = true)
        rows += moneyMetricRow("Total Invested Capital", t.totalInvestedCapitalMinor, bold = true)
        rows += moneyMetricRow("Locked Remaining Stock Capital", t.lockedInventoryCapitalMinor)
        rows += moneyMetricRow("Attributed Net Sales Revenue", t.netSalesRevenueMinor, bold = true)
        rows += moneyMetricRow("Attributed COGS", t.cogsMinor)
        rows += moneyMetricRow("Attributed Gross Profit", t.grossProfitMinor, bold = true)
        rows += moneyMetricRow("Direct Attributed Operating Expenses", t.directAttributedExpensesMinor)
        rows += moneyMetricRow("Allocated Shared Overhead", t.allocatedSharedOverheadMinor)
        rows += moneyMetricRow("Inventory Loss (Damage/Shortage/Write-off)", t.inventoryLossesMinor)
        rows += moneyMetricRow(t.profitLabel, t.attributedNetProfitMinor, bold = true)
        rows += moneyMetricRow("Unallocated Shared Overhead (Reference)", t.unallocatedSharedOverheadMinor)
        rows += moneyMetricRow("Attributable Cash Collected", t.attributableCashCollectedMinor)
        rows += moneyMetricRow("Attributable Receivable", t.attributableReceivableMinor)
        rows += moneyMetricRow("Unrecovered Capital", t.unrecoveredCapitalMinor)
        rows += moneyMetricRow("Expected Remaining Selling Revenue (${t.priceBasisLabel})", t.expectedRemainingRevenueMinor)
        rows += moneyMetricRow("Expected Remaining Gross Profit", t.expectedRemainingGrossProfitMinor, bold = true)
        rows += listOf(
            XlsxCell.Text("Missing Price SKU Count"),
            XlsxCell.Num(t.missingPriceSkuCount.toString()),
            XlsxCell.Text("SKUs")
        )
        rows += moneyMetricRow("Unattributed Revenue", t.unattributedRevenueMinor)
        rows += moneyMetricRow("Unattributed COGS", t.unattributedCogsMinor)
        rows += moneyMetricRow("Unattributed Inventory Cost", t.unattributedInventoryCostMinor)
        rows += moneyMetricRow("Unattributed Customer Collections", t.unattributedCollectionsMinor)
        rows += moneyMetricRow("Opening Due Receivable (Excluded)", t.openingDueReceivableMinor)
        rows += moneyMetricRow("Non-Merchandise / Tax Charges (Excluded)", t.nonMerchandiseAdditionalChargesMinor)
        rows += textRow("", "", "")

        rows += textRow("STOCK AGING BUCKETS", "QUANTITY", "CARRYING VALUE (BDT)", bold = true)
        StockAgingBucket.entries.forEach { bucket ->
            val q = pkg.agingSummary.bucketQuantities[bucket] ?: 0
            val v = pkg.agingSummary.bucketValuesMinor[bucket] ?: 0L
            rows += listOf(
                XlsxCell.Text("${bucket.labelEn} / ${bucket.labelBn}"),
                XlsxCell.Num(q.toString()),
                XlsxCell.Num(minorToDecimalString(v))
            )
        }
        rows += listOf(
            XlsxCell.Text("Oldest Unsold Stock Age (Days)"),
            XlsxCell.Num(pkg.agingSummary.oldestAgeDays.toString()),
            XlsxCell.Text("Days")
        )
        rows += textRow("", "", "")

        rows += textRow("ASSUMPTIONS & DATA WARNINGS", "DETAILS", "", bold = true)
        pkg.assumptions.forEachIndexed { idx, line ->
            rows += textRow("Assumption ${idx + 1}", line)
        }
        if (pkg.warnings.isEmpty()) {
            rows += textRow("Data Quality Check", "No provisional, pending receiving, or unattributed warnings in this scope.")
        } else {
            pkg.warnings.forEachIndexed { idx, warn ->
                rows += textRow("Warning ${idx + 1}", warn)
            }
        }
        return renderSheetXml(rows)
    }

    private fun buildProductsSheetXml(pkg: VaultExportPackage): String {
        val rows = mutableListOf<List<XlsxCell>>()
        rows += textRow(
            "Product ID",
            "Base SKU",
            "Product Name (EN)",
            "Product Name (BN)",
            "UOM",
            "Variants Count",
            "Received Good Qty",
            "Net Sold Qty",
            "Returned Good Qty",
            "Returned Damaged Qty",
            "Write-Off Qty",
            "Remaining Good Qty",
            "In-Transit Qty",
            "Avg Unit Landed Cost (BDT)",
            "Net Sales Revenue (BDT)",
            "COGS (BDT)",
            "Gross Profit (BDT)",
            "Gross Margin %",
            "Direct Expenses (BDT)",
            "Shared Overhead (BDT)",
            "Inventory Loss (BDT)",
            "${pkg.totals.profitLabel} (BDT)",
            "Net Margin %",
            "Locked Capital (BDT)",
            "Sell-Through %",
            "Expected Remaining Revenue (BDT)",
            "Expected Remaining GP (BDT)",
            "Share of Scope %",
            "Share of PO %",
            "Share of Shipment %",
            bold = true
        )
        pkg.products.forEach { p ->
            rows += listOf(
                XlsxCell.Text(p.productId),
                XlsxCell.Text(p.baseSku),
                XlsxCell.Text(p.productNameEn),
                XlsxCell.Text(p.productNameBn),
                XlsxCell.Text(p.uom),
                XlsxCell.Num(p.variants.size.toString()),
                XlsxCell.Num(p.receivedGoodQty.toString()),
                XlsxCell.Num(p.netSoldQty.toString()),
                XlsxCell.Num(p.returnedGoodQty.toString()),
                XlsxCell.Num(p.returnedDamagedQty.toString()),
                XlsxCell.Num(p.writeOffLossQty.toString()),
                XlsxCell.Num(p.remainingGoodQty.toString()),
                XlsxCell.Num(p.remainingInTransitTransferQty.toString()),
                XlsxCell.Num(minorToDecimalString(p.averageUnitLandedCostMinor ?: 0L)),
                XlsxCell.Num(minorToDecimalString(p.netSalesRevenueMinor)),
                XlsxCell.Num(minorToDecimalString(p.cogsMinor)),
                XlsxCell.Num(minorToDecimalString(p.grossProfitMinor)),
                XlsxCell.Num(formatPct(p.grossMarginPercent)),
                XlsxCell.Num(minorToDecimalString(p.directExpensesMinor)),
                XlsxCell.Num(minorToDecimalString(p.allocatedSharedOverheadMinor)),
                XlsxCell.Num(minorToDecimalString(p.inventoryLossMinor)),
                XlsxCell.Num(minorToDecimalString(p.attributedNetProfitMinor)),
                XlsxCell.Num(formatPct(p.netMarginPercent)),
                XlsxCell.Num(minorToDecimalString(p.lockedInventoryCostMinor)),
                XlsxCell.Num(formatPct(p.sellThroughPercent)),
                XlsxCell.Num(minorToDecimalString(p.expectedRemainingProfit.expectedSellingRevenueMinor)),
                XlsxCell.Num(minorToDecimalString(p.expectedRemainingProfit.expectedGrossProfitMinor)),
                XlsxCell.Num(formatPct(p.profitContributionToParentScope?.contributionPercent)),
                XlsxCell.Num(formatPct(p.profitContributionToPo?.contributionPercent)),
                XlsxCell.Num(formatPct(p.profitContributionToShipment?.contributionPercent))
            )
        }
        return renderSheetXml(rows)
    }

    private fun buildVariantsSheetXml(pkg: VaultExportPackage): String {
        val rows = mutableListOf<List<XlsxCell>>()
        rows += textRow(
            "Product ID",
            "Product Name (EN)",
            "Variant ID",
            "SKU",
            "Variant Label",
            "UOM",
            "PO ID",
            "Shipment ID",
            "Received Good Qty",
            "Net Sold Qty",
            "Returned Good Qty",
            "Returned Damaged Qty",
            "Write-Off Qty",
            "Remaining Good Qty",
            "In-Transit Qty",
            "Unit Landed Cost (BDT)",
            "Net Sales Revenue (BDT)",
            "COGS (BDT)",
            "Gross Profit (BDT)",
            "Direct Expenses (BDT)",
            "Shared Overhead (BDT)",
            "Inventory Loss (BDT)",
            "${pkg.totals.profitLabel} (BDT)",
            "Locked Capital (BDT)",
            "Expected Remaining GP (BDT)",
            "Share of Product %",
            "Share of PO %",
            "Share of Shipment %",
            bold = true
        )
        pkg.variants.forEach { (p, v) ->
            rows += listOf(
                XlsxCell.Text(p.productId),
                XlsxCell.Text(p.productNameEn),
                XlsxCell.Text(v.variantId.orEmpty()),
                XlsxCell.Text(v.sku),
                XlsxCell.Text(v.variantLabel),
                XlsxCell.Text(v.uom),
                XlsxCell.Text(v.purchaseOrderId.orEmpty()),
                XlsxCell.Text(v.shipmentId.orEmpty()),
                XlsxCell.Num(v.receivedGoodQty.toString()),
                XlsxCell.Num(v.netSoldQty.toString()),
                XlsxCell.Num(v.returnedGoodQty.toString()),
                XlsxCell.Num(v.returnedDamagedQty.toString()),
                XlsxCell.Num(v.writeOffLossQty.toString()),
                XlsxCell.Num(v.remainingGoodQty.toString()),
                XlsxCell.Num(v.remainingInTransitTransferQty.toString()),
                XlsxCell.Num(minorToDecimalString(v.unitLandedCostMinor)),
                XlsxCell.Num(minorToDecimalString(v.netSalesRevenueMinor)),
                XlsxCell.Num(minorToDecimalString(v.cogsMinor)),
                XlsxCell.Num(minorToDecimalString(v.grossProfitMinor)),
                XlsxCell.Num(minorToDecimalString(v.directExpensesMinor)),
                XlsxCell.Num(minorToDecimalString(v.allocatedSharedOverheadMinor)),
                XlsxCell.Num(minorToDecimalString(v.inventoryLossMinor)),
                XlsxCell.Num(minorToDecimalString(v.attributedNetProfitMinor)),
                XlsxCell.Num(minorToDecimalString(v.lockedInventoryCostMinor)),
                XlsxCell.Num(minorToDecimalString(v.expectedRemainingProfit.expectedGrossProfitMinor)),
                XlsxCell.Num(formatPct(v.profitContributionToProduct?.contributionPercent)),
                XlsxCell.Num(formatPct(v.profitContributionToPo?.contributionPercent)),
                XlsxCell.Num(formatPct(v.profitContributionToShipment?.contributionPercent))
            )
        }
        return renderSheetXml(rows)
    }

    private fun buildSalesAllocationsSheetXml(
        pkg: VaultExportPackage,
        dateOnlyFmt: SimpleDateFormat
    ): String {
        val rows = mutableListOf<List<XlsxCell>>()
        rows += textRow(
            "Allocation ID",
            "Invoice Number",
            "Invoice Date",
            "Product Name (EN)",
            "Product Name (BN)",
            "Variant",
            "Net Sold Qty",
            "Net Revenue (BDT)",
            "COGS (BDT)",
            "Gross Profit (BDT)",
            "Collected Cash (BDT)",
            "Receivable (BDT)",
            "Cost Layer ID",
            "PO ID",
            "Shipment ID",
            bold = true
        )
        pkg.salesAllocations.forEach { s ->
            rows += listOf(
                XlsxCell.Text(s.allocationId),
                XlsxCell.Text(s.invoiceNumber),
                XlsxCell.Text(dateOnlyFmt.format(Date(s.invoiceDate))),
                XlsxCell.Text(s.productNameEn),
                XlsxCell.Text(s.productNameBn),
                XlsxCell.Text(s.variantLabel),
                XlsxCell.Num(s.quantity.toString()),
                XlsxCell.Num(minorToDecimalString(s.netRevenueMinor)),
                XlsxCell.Num(minorToDecimalString(s.cogsMinor)),
                XlsxCell.Num(minorToDecimalString(s.grossProfitMinor)),
                XlsxCell.Num(minorToDecimalString(s.collectedCashMinor)),
                XlsxCell.Num(minorToDecimalString(s.receivableMinor)),
                XlsxCell.Text(s.costLayerId.orEmpty()),
                XlsxCell.Text(s.purchaseOrderId.orEmpty()),
                XlsxCell.Text(s.shipmentId.orEmpty())
            )
        }
        return renderSheetXml(rows)
    }

    private fun buildExpensesSheetXml(
        pkg: VaultExportPackage,
        dateOnlyFmt: SimpleDateFormat
    ): String {
        val rows = mutableListOf<List<XlsxCell>>()
        rows += textRow(
            "Expense Type",
            "Record ID",
            "Reference Number",
            "Date",
            "Category",
            "Description",
            "Treatment / Basis",
            "Scope Target",
            "Amount (BDT)",
            "Notes",
            bold = true
        )
        pkg.expenses.forEach { e ->
            rows += listOf(
                XlsxCell.Text(e.expenseType),
                XlsxCell.Text(e.id),
                XlsxCell.Text(e.referenceNumber),
                XlsxCell.Text(dateOnlyFmt.format(Date(e.timestampMillis))),
                XlsxCell.Text(e.category),
                XlsxCell.Text(e.description),
                XlsxCell.Text(e.treatmentLabel),
                XlsxCell.Text(e.targetScopeId),
                XlsxCell.Num(minorToDecimalString(e.amountBdtMinor)),
                XlsxCell.Text(e.notes)
            )
        }
        return renderSheetXml(rows)
    }

    private fun buildStockSheetXml(
        pkg: VaultExportPackage,
        dateOnlyFmt: SimpleDateFormat
    ): String {
        val rows = mutableListOf<List<XlsxCell>>()
        rows += textRow(
            "Layer ID",
            "Product Name (EN)",
            "Product Name (BN)",
            "Variant",
            "Warehouse",
            "Receipt Number",
            "Origin Receipt Date",
            "Initial Qty",
            "Remaining Qty",
            "Unit Landed Cost (BDT)",
            "Carrying Value (BDT)",
            "Source Type",
            "Age (Days)",
            "Aging Bucket",
            "PO ID",
            "Shipment ID",
            bold = true
        )
        pkg.stockLayers.forEach { layer ->
            rows += listOf(
                XlsxCell.Text(layer.layerId),
                XlsxCell.Text(layer.productNameEn),
                XlsxCell.Text(layer.productNameBn),
                XlsxCell.Text(layer.variantLabel),
                XlsxCell.Text(layer.warehouseName),
                XlsxCell.Text(layer.receiptNumber),
                XlsxCell.Text(dateOnlyFmt.format(Date(layer.originReceiptDate))),
                XlsxCell.Num(layer.initialQuantity.toString()),
                XlsxCell.Num(layer.remainingQuantity.toString()),
                XlsxCell.Num(minorToDecimalString(layer.unitLandedCostBdtMinor)),
                XlsxCell.Num(minorToDecimalString(layer.carryingValueMinor)),
                XlsxCell.Text(layer.layerSourceType),
                XlsxCell.Num(layer.ageDays.toString()),
                XlsxCell.Text(layer.agingBucket.labelEn),
                XlsxCell.Text(layer.purchaseOrderId.orEmpty()),
                XlsxCell.Text(layer.shipmentId.orEmpty())
            )
        }
        return renderSheetXml(rows)
    }

    private fun buildReceivablesSheetXml(
        pkg: VaultExportPackage,
        dateOnlyFmt: SimpleDateFormat
    ): String {
        val rows = mutableListOf<List<XlsxCell>>()
        rows += textRow(
            "Invoice Number",
            "Invoice Date",
            "Customer Name",
            "Attributed Revenue (BDT)",
            "Attributable Cash Collected (BDT)",
            "Attributable Receivable (BDT)",
            "Age (Days)",
            "Attribution Notes",
            bold = true
        )
        pkg.receivables.forEach { rec ->
            rows += listOf(
                XlsxCell.Text(rec.invoiceNumber),
                XlsxCell.Text(dateOnlyFmt.format(Date(rec.invoiceDateMillis))),
                XlsxCell.Text(rec.customerName),
                XlsxCell.Num(minorToDecimalString(rec.attributedRevenueMinor)),
                XlsxCell.Num(minorToDecimalString(rec.collectedCashMinor)),
                XlsxCell.Num(minorToDecimalString(rec.receivableMinor)),
                XlsxCell.Num(rec.ageDays.toString()),
                XlsxCell.Text(rec.notes)
            )
        }
        return renderSheetXml(rows)
    }

    private fun buildDocumentsIndexSheetXml(
        pkg: VaultExportPackage,
        dateTimeFmt: SimpleDateFormat
    ): String {
        val rows = mutableListOf<List<XlsxCell>>()
        rows += textRow(
            "Document ID",
            "Category",
            "Record Type",
            "Title (EN)",
            "Title (BN)",
            "Reference",
            "Timestamp",
            "Amount (BDT)",
            "Subtitle",
            "File Path / Target",
            "Mime Type",
            "File Size (Bytes)",
            bold = true
        )
        pkg.documentsIndex.forEach { doc ->
            val recordType = when {
                doc.isLiveRegeneratedReport -> "Live Regenerated Report"
                doc.isImmutableSnapshot -> "Immutable Saved Snapshot"
                doc.attachmentId != null -> "Local Vault Attachment"
                else -> "Linked ERP Document"
            }
            rows += listOf(
                XlsxCell.Text(doc.id),
                XlsxCell.Text(doc.category.name),
                XlsxCell.Text(recordType),
                XlsxCell.Text(doc.titleEn),
                XlsxCell.Text(doc.titleBn),
                XlsxCell.Text(doc.referenceNumber),
                XlsxCell.Text(dateTimeFmt.format(Date(doc.timestampMillis))),
                if (doc.amountMinor != null) XlsxCell.Num(minorToDecimalString(doc.amountMinor)) else XlsxCell.Text("-"),
                XlsxCell.Text(doc.subtitle),
                XlsxCell.Text(doc.localFilePath ?: doc.targetRoute.orEmpty()),
                XlsxCell.Text(doc.mimeType.orEmpty()),
                if (doc.fileSizeBytes != null) XlsxCell.Num(doc.fileSizeBytes.toString()) else XlsxCell.Text("-")
            )
        }
        return renderSheetXml(rows)
    }

    private fun formatPct(value: Double?): String =
        if (value == null) "0.00" else String.format(Locale.US, "%.2f", value)

    private fun buildContentTypesXml(sheetCount: Int): String = buildString {
        append(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
"""
        )
        for (i in 1..sheetCount) {
            append("  <Override PartName=\"/xl/worksheets/sheet$i.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n")
        }
        append("</Types>")
    }

    private fun buildWorkbookRelsXml(sheetCount: Int): String = buildString {
        append(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
"""
        )
        for (i in 1..sheetCount) {
            append("  <Relationship Id=\"rId$i\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$i.xml\"/>\n")
        }
        val styleRelId = sheetCount + 1
        append("  <Relationship Id=\"rId$styleRelId\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>\n")
        append("</Relationships>")
    }

    private fun buildWorkbookXml(sheetNames: List<String>): String = buildString {
        append(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
"""
        )
        sheetNames.forEachIndexed { idx, name ->
            val id = idx + 1
            append("    <sheet name=\"${escapeXml(name)}\" sheetId=\"$id\" r:id=\"rId$id\"/>\n")
        }
        append("  </sheets>\n</workbook>")
    }

    private const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
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
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>"""
}
