package com.pocketdev.advance_erp.core.communication

import android.content.Context
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.pdf.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentAttachmentProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AdvanceErpDatabase
) {
    private val retentionMs = 72L * 60 * 60 * 1000

    private fun cleanupExpiredCache(root: File, keepFile: File) {
        val now = System.currentTimeMillis()
        root.listFiles()?.filter { it != keepFile && now - it.lastModified() > retentionMs }?.forEach { it.delete() }
    }

    suspend fun xlsx(document: SafeDocument): File = withContext(Dispatchers.IO) {
        require(document.request.module in setOf(CommunicationModule.PURCHASE_ORDER, CommunicationModule.PACKING))
        val po = requireNotNull(db.purchaseOrderDao().getOrderById(document.request.documentId)).copy(notes = null)
        val items = db.purchaseOrderDao().getItemsForOrderSync(po.id)
        val products = items.mapNotNull { db.productDao().getProductById(it.productId) }.associateBy { it.id }
        val supplier = db.supplierDao().getSupplierById(po.supplierId)
        val profile = db.businessProfileDao().getProfileSync()
        val variants = db.productVariantDao().getAllVariantsSync().associateBy { it.id }
        val cartons = db.importShipmentDao().getAllCartonsSync().filter { it.purchaseOrderId == po.id }
        val source = if (document.request.module == CommunicationModule.PURCHASE_ORDER) {
            com.pocketdev.advance_erp.core.util.ImportExcelGenerator.generateSupplierOrderExcel(
                context, po, items, products, supplier, profile, variants, cartons = cartons
            )
        } else {
            com.pocketdev.advance_erp.core.util.ImportExcelGenerator.generateFreightForwarderPackingListExcel(
                context, po, items, products, supplier, profile, variants, cartons = cartons
            )
        }
        val root = File(context.cacheDir, "communication").apply { mkdirs() }
        val target = File(root, "${document.request.module.name}_${UUID.randomUUID()}.xlsx")
        source.copyTo(target, overwrite = true)
        source.delete()
        cleanupExpiredCache(root, target)
        target
    }

    suspend fun pdf(document: SafeDocument, language: String): File = withContext(Dispatchers.IO) {
        val bn = language == "bn"
        val fields = document.fields.filterKeys {
            it in DynamicTemplateEngine.labels && it != "items" && !it.startsWith("company_")
        }

        // Wrap long field values so addresses or snapshots never truncate important text
        val detailRows = mutableListOf<PdfReportRow>()
        for ((key, value) in fields) {
            if (value.isBlank()) continue
            val pair = DynamicTemplateEngine.labels.getValue(key)
            val labelText = if (bn) pair.second else pair.first
            val wrapped = wrapLine(value, 54)
            wrapped.forEachIndexed { idx, chunk ->
                detailRows.add(PdfReportRow(listOf(if (idx == 0) labelText else "", chunk)))
            }
        }

        val details = PdfReportSection(
            titleEn = "Summary & Reference Details",
            titleBn = "সারসংক্ষেপ ও বিবরণ",
            columns = listOf(
                PdfReportColumn("Field", "বিবরণ", 1.0f),
                PdfReportColumn("Value", "মান", 1.9f)
            ),
            rows = detailRows
        )

        val sections = mutableListOf(details)
        if (document.rows.isNotEmpty() && document.columns.isNotEmpty()) {
            val tableCols = buildColumnsWithWeights(document.request.module, document.columns)
            sections.add(
                PdfReportSection(
                    titleEn = if (document.request.module in setOf(CommunicationModule.LEDGER, CommunicationModule.STATEMENT, CommunicationModule.SUPPLIER_STATEMENT)) "Transactions" else "Line Details",
                    titleBn = if (document.request.module in setOf(CommunicationModule.LEDGER, CommunicationModule.STATEMENT, CommunicationModule.SUPPLIER_STATEMENT)) "লেনদেনসমূহ" else "তালিকা বিবরণ",
                    columns = tableCols,
                    rows = document.rows.map { PdfReportRow(it) }
                )
            )
        }
        if (document.warnings.isNotEmpty()) {
            sections.add(
                PdfReportSection(
                    titleEn = "Notices",
                    titleBn = "নোট ও সতর্কবার্তা",
                    columns = listOf(PdfReportColumn("Notice", "তথ্য", 1f)),
                    rows = document.warnings.flatMap { wrapLine(it, 75) }.map { PdfReportRow(listOf(it)) }
                )
            )
        }

        val report = CentralizedPdfReport(
            reportId = "communication_${UUID.randomUUID()}",
            category = "Communication",
            titleEn = document.request.module.en,
            titleBn = document.request.module.bn,
            dateRangeText = document.fields["date_range"],
            filterText = document.number,
            sections = sections,
            additionalNotes = document.warnings.joinToString("\n").ifBlank { null },
            isBangla = bn
        )
        val source = CentralizedPdfEngine.generatePdf(context, report, db.businessProfileDao().getProfileSync())
        val root = File(context.cacheDir, "communication").apply { mkdirs() }
        val result = File(root, "${document.request.module.name}_${UUID.randomUUID()}.pdf")
        source.copyTo(result, overwrite = true)
        source.delete()
        cleanupExpiredCache(root, result)
        result
    }

    private fun buildColumnsWithWeights(module: CommunicationModule, titles: List<String>): List<PdfReportColumn> {
        return when (module) {
            CommunicationModule.LEDGER, CommunicationModule.STATEMENT, CommunicationModule.SUPPLIER_STATEMENT -> {
                val weights = listOf(0.95f, 1.35f, 1.0f, 1.0f, 1.3f)
                val right = listOf(false, false, true, true, true)
                titles.mapIndexed { idx, t ->
                    PdfReportColumn(t, t, weights.getOrElse(idx) { 1.0f }, right.getOrElse(idx) { false })
                }
            }
            CommunicationModule.INVOICE, CommunicationModule.RETURN, CommunicationModule.PURCHASE_ORDER, CommunicationModule.PACKING, CommunicationModule.QUOTATION, CommunicationModule.SALES_ORDER -> {
                val weights = listOf(1.9f, 0.7f, 1.25f)
                val right = listOf(false, true, true)
                titles.mapIndexed { idx, t ->
                    PdfReportColumn(t, t, weights.getOrElse(idx) { 1.0f }, right.getOrElse(idx) { false })
                }
            }
            else -> titles.mapIndexed { idx, t ->
                PdfReportColumn(t, t, if (idx == 0) 1.6f else 1.0f, isRightAligned = idx == titles.lastIndex && titles.size > 1)
            }
        }
    }

    private fun wrapLine(text: String, maxChars: Int): List<String> {
        val clean = text.replace("\r", "").split('\n')
        val out = mutableListOf<String>()
        for (segment in clean) {
            if (segment.length <= maxChars) {
                out.add(segment)
                continue
            }
            var remaining = segment.trim()
            while (remaining.length > maxChars) {
                val breakIdx = remaining.lastIndexOf(' ', maxChars).takeIf { it > maxChars / 3 } ?: maxChars
                out.add(remaining.substring(0, breakIdx).trimEnd())
                remaining = remaining.substring(breakIdx).trimStart()
            }
            if (remaining.isNotEmpty()) out.add(remaining)
        }
        return if (out.isEmpty()) listOf("") else out
    }
}
