package com.pocketdev.advance_erp.core.communication

import androidx.room.withTransaction
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.data.entity.*
import com.pocketdev.advance_erp.domain.repository.SalesRepository
import kotlinx.coroutines.flow.first
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunicationEngine @Inject constructor(private val db: AdvanceErpDatabase, private val sales: SalesRepository,
    private val attachments: DocumentAttachmentProvider, private val preferences: UserPreferencesRepository) {
    private suspend fun checkAccess(module: CommunicationModule) {
        val role = preferences.selectedUserRole.first()
        require(SecurityManager.canExportReport(role)) { "Your role cannot export business documents" }
        if(module in setOf(CommunicationModule.PURCHASE_ORDER, CommunicationModule.SUPPLIER_PAYMENT))
            require(SecurityManager.canViewCost(role)) { "Your role cannot export supplier financial documents" }
    }
    suspend fun xlsx(document: SafeDocument): java.io.File { checkAccess(document.request.module); return attachments.xlsx(document) }
    val dao get() = db.communicationDao()
    suspend fun settings() = dao.settings() ?: CommunicationSettings()
    suspend fun choices(module: CommunicationModule): List<CommunicationChoice> = withContext(Dispatchers.IO) {
        checkAccess(module)
        fun choice(id: String, label: String) = CommunicationChoice(CommunicationRequest(module, id), label)
        when(module) {
            CommunicationModule.INVOICE, CommunicationModule.QUOTATION, CommunicationModule.SALES_ORDER -> db.salesDao().getAllInvoicesSync().filter {
                when(module) { CommunicationModule.QUOTATION -> it.status == "QUOTATION"; CommunicationModule.SALES_ORDER -> it.status == "SALES_ORDER"; else -> it.isPosted && it.status != "CANCELLED" }
            }.map { choice(it.id, "${it.invoiceNumber} · ${it.customerName}") }
            CommunicationModule.PAYMENT -> db.customerPaymentDao().getAllPaymentsSync().filter { it.isPosted && !it.isVoided }.map { choice(it.id, "${it.paymentNumber} · ${it.customerName}") }
            CommunicationModule.RETURN -> db.salesReturnDao().getAllReturnsSync().filter { it.isPosted }.map { choice(it.id, "${it.returnNumber} · ${it.customerName}") }
            CommunicationModule.EXCHANGE -> dao.snapshots("EXCHANGE").map { choice(it.documentId, it.number) }
            CommunicationModule.LEDGER, CommunicationModule.STATEMENT, CommunicationModule.REMINDER -> db.customerDao().getLedgerCustomersSync().map { choice(it.id, "${it.name} · ${it.phone}") }
            CommunicationModule.PURCHASE_ORDER, CommunicationModule.PACKING -> db.purchaseOrderDao().getAllOrdersSync().filter { it.status != "CANCELLED" }.map { choice(it.id, it.poNumber) }
            CommunicationModule.SUPPLIER_PAYMENT -> db.supplierPaymentDao().getAllPaymentsSync().map { choice(it.id, it.paymentNumber) }
            CommunicationModule.DELIVERY -> db.salesDao().getAllInvoicesSync().filter { it.isPosted && it.status != "CANCELLED" }.flatMap { inv ->
                sales.getDeliveryChallansForInvoice(inv.id).map { choice(inv.id + "|" + it.deliveryNumber, "${it.deliveryNumber} · ${it.customerName}") }
            }
        }
    }
    suspend fun prepare(request: CommunicationRequest, languageOverride: String? = null,
        statusOverride: CommunicationStatus? = null, forcePdf: Boolean = false): PreparedCommunication = withContext(Dispatchers.IO) {
        checkAccess(request.module)
        val settings = settings()
        val document = db.withTransaction { readDocument(request, languageOverride ?: settings.defaultLanguage) }
        val preference = dao.preference((if (document.supplier) "supplier:" else "customer:") + document.partyId)
        val language = languageOverride ?: preference?.language ?: settings.defaultLanguage
        val localized = if (language == (languageOverride ?: settings.defaultLanguage)) document else db.withTransaction { readDocument(request, language) }
        val status = statusOverride ?: when { (localized.balanceMinor ?: 0) < 0 -> CommunicationStatus.CREDIT; (localized.previousMinor ?: 0) > 0 -> CommunicationStatus.PREVIOUS_DUE; else -> localized.status }
        val template = dao.template(request.module.name, status.name, language)?.body
            ?: DynamicTemplateEngine.defaultTemplate(request.module, status, language)
        val message = if (settings.autoGenerate) DynamicTemplateEngine.render(template, DynamicTemplateEngine.fields(localized, settings, language)) else ""
        var error: String? = null
        val file = if (settings.includePdf || forcePdf) try { attachments.pdf(localized, language) } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            error = e.message ?: "PDF generation failed"; null
        } else null
        PreparedCommunication(localized, language, message, template, file, error)
    }
    private suspend fun readDocument(request: CommunicationRequest, language: String): SafeDocument {
        val bn = language == "bn"
        fun date(value: Long) = SimpleDateFormat("dd MMM yyyy HH:mm", Locale(if(bn) "bn" else "en")).format(Date(value))
        fun money(value: Long, currency: String = "BDT") = "$currency ${java.math.BigDecimal.valueOf(value, 2).toPlainString()}"
        fun balance(value: Long) = if(value < 0) "${money(-value)} ${if(bn) "ক্রেডিট" else "credit"}" else "${money(value)} ${if(bn) "বকেয়া" else "due"}"
        val f = linkedMapOf<String, String>()
        val profile = db.businessProfileDao().getProfileSync()
        f["company_name"] = profile?.companyName.orEmpty(); f["company_phone"] = profile?.phone.orEmpty(); f["company_address"] = profile?.address.orEmpty()
        f["as_of"] = date(System.currentTimeMillis())
        var number = ""; var partyId = ""; var name = ""; var phone = ""; var supplier = false
        var outstanding: Long? = null; var previous: Long? = null
        var status = CommunicationStatus.GENERAL
        var rows = emptyList<List<String>>(); var columns = emptyList<String>(); val warnings = mutableListOf<String>()
        suspend fun customer(id: String) {
            val c = requireNotNull(db.customerDao().getCustomerById(id)) { "Customer unavailable / গ্রাহক পাওয়া যায়নি" }
            partyId = id; name = c.name; phone = c.phone
            outstanding = sales.getCustomerLedger(id).lastOrNull()?.runningBalanceMinor ?: c.outstandingBalanceMinor
            f["customer_name"] = name; f["customer_phone"] = phone; f["total_outstanding"] = balance(outstanding!!)
        }
        suspend fun supplier(id: String) {
            val s = requireNotNull(db.supplierDao().getSupplierById(id)) { "Supplier unavailable" }
            supplier = true; partyId = id; name = s.companyName; phone = s.phone; outstanding = s.outstandingPayableMinor
            f["supplier_name"] = name; f["total_outstanding"] = balance(outstanding!!)
        }
        fun itemColumns() = if(bn) listOf("পণ্য", "পরিমাণ", "মূল্য") else listOf("Product", "Quantity", "Amount")
        when(request.module) {
            CommunicationModule.INVOICE, CommunicationModule.QUOTATION, CommunicationModule.SALES_ORDER -> {
                val inv = requireNotNull(db.salesDao().getInvoiceById(request.documentId)) { "Invoice unavailable" }
                require(inv.status != "CANCELLED") { "Cancelled invoice cannot be shared as active" }
                if (request.module == CommunicationModule.INVOICE) require(inv.isPosted) { "Post the invoice first" }
                customer(inv.customerId); number = inv.invoiceNumber
                f["document_date"] = date(inv.invoiceDate)
                val items = db.salesDao().getItemsForInvoice(inv.id)
                rows = items.map { listOf(if(bn) it.productNameBn.ifBlank { it.productNameEn } else it.productNameEn, it.quantity.toString(), money(it.lineTotalMinor)) }; columns = itemColumns()
                f["total_quantity"] = items.sumOf { it.quantity }.toString()
                if(request.module == CommunicationModule.INVOICE) {
                    val allocations = db.customerPaymentDao().getAllocationsForInvoiceSync(inv.id)
                    val payments = db.customerPaymentDao().getPaymentsForCustomerSync(inv.customerId).associateBy { it.id }
                    val allocated = allocations.filter { payments[it.paymentId]?.let { p -> p.isPosted && !p.isVoided } == true }.sumOf { it.amountMinor }
                    val returns = db.salesReturnDao().getReturnsForCustomerSync(inv.customerId).filter { it.invoiceId == inv.id && it.isPosted }.sumOf { it.totalReturnAmountMinor }
                    // dueAmountMinor is maintained by the authoritative allocation / return repository.
                    val due = inv.dueAmountMinor.coerceAtLeast(0)
                    status = when { due == 0L && returns > 0 -> CommunicationStatus.SETTLED; due == 0L -> CommunicationStatus.PAID; allocated > 0 -> CommunicationStatus.PARTIAL; else -> CommunicationStatus.UNPAID }
                    f["invoice_number"] = number; f["invoice_date"] = date(inv.invoiceDate)
                    f["invoice_amount"] = money(inv.totalAmountMinor); f["received_payment"] = money(allocated); f["invoice_due"] = money(due)
                    inv.dueDate?.let { f["due_date"] = date(it) }
                    val snap = dao.snapshot("INVOICE", inv.id)
                    if(snap != null) {
                        previous = snap.previousBalanceMinor; f["previous_due"] = balance(snap.previousBalanceMinor)
                        f["historical_as_of"] = date(snap.createdAt); f["historical_payment"] = money(snap.allocatedPaymentMinor)
                        f["historical_due"] = money(snap.invoiceDueMinor); f["historical_balance"] = balance(snap.closingBalanceMinor)
                    } else {
                        val unavailable = if(bn) "অনুপলব্ধ — পুরোনো ইনভয়েসের snapshot নেই" else "Unavailable — no original posting snapshot"
                        f["previous_due"] = unavailable; warnings.add(unavailable)
                    }
                } else {
                    f["document_amount"] = money(inv.totalAmountMinor)
                    if(request.module == CommunicationModule.QUOTATION) {
                        // dueDate is a payment due date, not quotation validity: do not invent a validity.
                        warnings.add(if(bn) "কোটেশনের মেয়াদ মূল রেকর্ডে সংরক্ষিত নেই।" else "Quotation validity is not stored in the original record.")
                    }
                    inv.dueDate?.let { f["delivery_information"] = (if(bn) "নির্ধারিত তারিখ: " else "Scheduled date: ") + date(it) }
                    f.remove("total_outstanding")
                }
            }
            CommunicationModule.PAYMENT -> {
                val p = requireNotNull(db.customerPaymentDao().getPaymentById(request.documentId)) { "Payment unavailable" }
                require(p.isPosted && !p.isVoided) { "Voided / unposted payment cannot be shared as a receipt" }
                customer(p.customerId); number = p.paymentNumber
                f.putAll(mapOf("payment_number" to number, "payment_date" to date(p.paymentDate), "payment_amount" to money(p.amountMinor), "payment_method" to p.paymentMethod, "payment_reference" to p.referenceNote.orEmpty()))
                columns = if(bn) listOf("ইনভয়েস", "বরাদ্দ") else listOf("Invoice", "Allocated")
                rows = db.customerPaymentDao().getAllocationsForPayment(p.id).map { listOf(it.invoiceNumber, money(it.amountMinor)) }
            }
            CommunicationModule.LEDGER, CommunicationModule.STATEMENT, CommunicationModule.REMINDER -> {
                customer(request.documentId); number = request.module.name + "-" + name
                if(request.module != CommunicationModule.REMINDER) {
                    val from = request.from ?: 0L; val to = request.to ?: System.currentTimeMillis(); require(from <= to) { "Invalid date range" }
                    val ledger = sales.getCustomerLedger(partyId)
                    val opening = ledger.lastOrNull { it.date < from }?.runningBalanceMinor ?: 0L
                    val selected = ledger.filter { it.date in from..to }
                    f["opening_balance"] = balance(opening); f["closing_balance"] = balance(selected.lastOrNull()?.runningBalanceMinor ?: opening)
                    f["date_range"] = (if(from == 0L) (if(bn) "শুরু থেকে" else "Beginning") else date(from)) + " — " + date(to)
                    columns = if(bn) listOf("তারিখ", "রেফারেন্স", "ডেবিট", "ক্রেডিট", "ব্যালেন্স") else listOf("Date", "Reference", "Debit", "Credit", "Balance")
                    rows = selected.map { listOf(date(it.date), it.referenceNumber, money(it.debitMinor), money(it.creditMinor), balance(it.runningBalanceMinor)) }
                }
            }
            CommunicationModule.RETURN -> {
                val r = requireNotNull(db.salesReturnDao().getReturnById(request.documentId)) { "Return unavailable" }
                require(r.isPosted); customer(r.customerId); number = r.returnNumber
                f["document_date"] = date(r.returnDate); f["return_amount"] = money(r.totalReturnAmountMinor)
                f["refund_amount"] = money(if(r.refundOption == "CUSTOMER_CREDIT") 0 else r.totalReturnAmountMinor)
                f["payment_method"] = r.refundOption
                rows = db.salesReturnDao().getItemsForReturn(r.id).map { listOf(if(bn) it.productNameBn.ifBlank { it.productNameEn } else it.productNameEn, it.returnQuantity.toString(), money(it.returnTotalMinor)) }; columns = itemColumns()
            }
            CommunicationModule.DELIVERY -> {
                val id = request.documentId.substringBefore('|'); val no = request.documentId.substringAfter('|')
                val inv = requireNotNull(db.salesDao().getInvoiceById(id)); require(inv.status != "CANCELLED")
                val d = sales.getDeliveryChallansForInvoice(id).firstOrNull { it.deliveryNumber == no } ?: error("Delivery unavailable")
                customer(inv.customerId); number = d.deliveryNumber; f.remove("total_outstanding")
                f["document_date"] = date(d.deliveryDate); f["delivery_information"] = listOfNotNull(d.deliveryPerson, d.transportInfo).joinToString(" · ")
                rows = d.items.map { listOf(if(bn) it.productNameBn.ifBlank { it.productNameEn } else it.productNameEn, it.dispatchedQuantity.toString()) }
                columns = if(bn) listOf("পণ্য", "পরিমাণ") else listOf("Product", "Quantity")
                f["total_quantity"] = d.items.sumOf { it.dispatchedQuantity }.toString()
            }
            CommunicationModule.EXCHANGE -> {
                val snap = requireNotNull(dao.snapshot("EXCHANGE", request.documentId)) { "Original exchange details unavailable; no fabricated history" }
                customer(snap.partyId); number = snap.number; f["document_date"] = date(snap.createdAt)
                val json = JSONObject(snap.payload); f["price_difference"] = money(snap.originalAmountMinor)
                f["payment_method"] = json.optString("method")
                val a = json.getJSONArray("rows"); rows = (0 until a.length()).map { i -> val row = a.getJSONArray(i); (0 until row.length()).map { row.getString(it) } }
                columns = if(bn) listOf("ধরন", "পণ্য", "পরিমাণ") else listOf("Direction", "Product", "Quantity")
            }
            CommunicationModule.PURCHASE_ORDER, CommunicationModule.PACKING -> {
                val po = requireNotNull(db.purchaseOrderDao().getOrderById(request.documentId)) { "Purchase order unavailable" }; require(po.status != "CANCELLED")
                supplier(po.supplierId); number = po.poNumber; f["document_date"] = date(po.orderDate)
                val items = db.purchaseOrderDao().getItemsForOrderSync(po.id)
                rows = items.map { item ->
                    val product = db.productDao().getProductById(item.productId)
                    listOf(if(bn) product?.nameBn?.ifBlank { product.nameEn } ?: item.productId else product?.nameEn ?: item.productId, item.quantity.toString(),
                        if(request.module == CommunicationModule.PACKING) item.cartonQuantity.toString() else money(item.totalAmountMinor, po.currency))
                }
                f["total_quantity"] = items.sumOf { it.quantity }.toString()
                if(request.module == CommunicationModule.PACKING) {
                    f.remove("total_outstanding"); f["total_carton"] = po.totalCartons.toString(); f["gross_weight"] = po.totalGrossWeightKg; f["total_cbm"] = po.totalCbm
                    columns = if(bn) listOf("পণ্য", "পরিমাণ", "কার্টন") else listOf("Product", "Quantity", "Cartons")
                } else { f["document_amount"] = money(po.totalAmountMinor, po.currency); columns = itemColumns() }
            }
            CommunicationModule.SUPPLIER_PAYMENT -> {
                val p = requireNotNull(db.supplierPaymentDao().getPaymentById(request.documentId)) { "Payment unavailable" }
                supplier(p.supplierId); number = p.paymentNumber
                f.putAll(mapOf("payment_number" to number, "payment_date" to date(p.paymentDate), "payment_amount" to money(p.amountMinor, p.currency), "payment_method" to p.paymentMethod, "payment_reference" to p.referenceNumber.orEmpty()))
            }
        }
        f["document_number"] = number
        if(rows.isNotEmpty() && request.module !in setOf(CommunicationModule.LEDGER, CommunicationModule.STATEMENT, CommunicationModule.PAYMENT))
            f["items"] = rows.joinToString("\n") { it.joinToString(" · ") }
        return SafeDocument(request, number, partyId, name, phone, supplier, f, rows, columns, status, outstanding, previous, warnings)
    }
}
