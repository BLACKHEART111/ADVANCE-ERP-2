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
class DocumentAttachmentProvider @Inject constructor(@ApplicationContext private val context: Context, private val db: AdvanceErpDatabase) {
    suspend fun xlsx(document: SafeDocument): File = withContext(Dispatchers.IO) {
        require(document.request.module in setOf(CommunicationModule.PURCHASE_ORDER, CommunicationModule.PACKING))
        val po = requireNotNull(db.purchaseOrderDao().getOrderById(document.request.documentId)).copy(notes = null)
        val items = db.purchaseOrderDao().getItemsForOrderSync(po.id)
        val products = items.mapNotNull { db.productDao().getProductById(it.productId) }.associateBy { it.id }
        val supplier = db.supplierDao().getSupplierById(po.supplierId)
        val profile = db.businessProfileDao().getProfileSync()
        val variants = db.productVariantDao().getAllVariantsSync().associateBy { it.id }
        val cartons = db.importShipmentDao().getAllCartonsSync().filter { it.purchaseOrderId == po.id }
        val source = if(document.request.module == CommunicationModule.PURCHASE_ORDER)
            com.pocketdev.advance_erp.core.util.ImportExcelGenerator.generateSupplierOrderExcel(context, po, items, products, supplier, profile, variants, cartons = cartons)
        else com.pocketdev.advance_erp.core.util.ImportExcelGenerator.generateFreightForwarderPackingListExcel(context, po, items, products, supplier, profile, variants, cartons = cartons)
        val root = File(context.cacheDir, "communication").apply { mkdirs() }
        source.copyTo(File(root, "${document.request.module.name}_${UUID.randomUUID()}.xlsx"))
    }
    suspend fun pdf(document: SafeDocument, language: String): File = withContext(Dispatchers.IO) {
        val bn = language == "bn"
        val fields = document.fields.filterKeys { it in DynamicTemplateEngine.labels && it != "items" && !it.startsWith("company_") }
        val details = PdfReportSection(columns = listOf(PdfReportColumn("Field", "বিবরণ"), PdfReportColumn("Value", "মান", 1.5f)),
            rows = fields.filterValues { it.isNotBlank() }.map { (key, value) ->
                val label = DynamicTemplateEngine.labels.getValue(key)
                PdfReportRow(listOf(if(bn) label.second else label.first, value))
            })
        val sections = mutableListOf(details)
        if(document.rows.isNotEmpty()) sections.add(PdfReportSection(columns = document.columns.map { PdfReportColumn(it, it) }, rows = document.rows.map { PdfReportRow(it) }))
        val report = CentralizedPdfReport(reportId = "communication_${UUID.randomUUID()}", category = "Communication",
            titleEn = document.request.module.en, titleBn = document.request.module.bn,
            dateRangeText = document.fields["date_range"], filterText = document.number, sections = sections,
            additionalNotes = document.warnings.joinToString("\n").ifBlank { null }, isBangla = bn)
        val source = CentralizedPdfEngine.generatePdf(context, report, db.businessProfileDao().getProfileSync())
        val root = File(context.cacheDir, "communication").apply { mkdirs() }
        val result = File(root, "${document.request.module.name}_${UUID.randomUUID()}.pdf")
        source.copyTo(result); source.delete()
        root.listFiles()?.filter { it != result && System.currentTimeMillis() - it.lastModified() > 72L * 60 * 60 * 1000 }?.forEach { it.delete() }
        result
    }
}
