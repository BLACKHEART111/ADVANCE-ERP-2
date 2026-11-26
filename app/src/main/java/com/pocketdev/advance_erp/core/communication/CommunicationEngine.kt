package com.pocketdev.advance_erp.core.communication

import androidx.room.withTransaction
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.engine.PaymentAllocationValidation
import com.pocketdev.advance_erp.core.security.SecurityManager
import com.pocketdev.advance_erp.data.entity.*
import com.pocketdev.advance_erp.domain.model.PartyLedgerStatement
import com.pocketdev.advance_erp.domain.repository.BusinessControlCenterRepository
import com.pocketdev.advance_erp.domain.repository.SalesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunicationEngine @Inject constructor(
    private val db: AdvanceErpDatabase,
    private val sales: SalesRepository,
    private val controlCenter: BusinessControlCenterRepository,
    private val attachments: DocumentAttachmentProvider,
    private val preferences: UserPreferencesRepository
) {
    val dao get() = db.communicationDao()

    suspend fun currentRole() = preferences.selectedUserRole.first()

    suspend fun requireReportAccess() {
        val role = currentRole()
        require(SecurityManager.canExportReport(role)) {
            "Your role cannot access business document sharing"
        }
    }

    suspend fun checkAccess(module: CommunicationModule) {
        val role = currentRole()
        require(SecurityManager.canExportReport(role)) { "Your role cannot export business documents" }
        if (module.isSupplierModule()) {
            require(SecurityManager.canViewCost(role)) { "Your role cannot export supplier financial documents" }
        }
    }

    suspend fun visibleModules(): List<CommunicationModule> {
        val role = currentRole()
        if (!SecurityManager.canExportReport(role)) return emptyList()
        val canViewSupplier = SecurityManager.canViewCost(role)
        return CommunicationModule.entries.filter { canViewSupplier || !it.isSupplierModule() }
    }

    suspend fun filterAuthorizedHistory(rows: List<CommunicationHistory>): List<CommunicationHistory> {
        val role = currentRole()
        if (!SecurityManager.canExportReport(role)) return emptyList()
        if (SecurityManager.canViewCost(role)) return rows
        return rows.filterNot { row ->
            runCatching { CommunicationModule.valueOf(row.module).isSupplierModule() }.getOrDefault(false)
        }
    }

    suspend fun xlsx(document: SafeDocument): File {
        checkAccess(document.request.module)
        return attachments.xlsx(document)
    }

    suspend fun settings(): CommunicationSettings = dao.settings() ?: CommunicationSettings()

    suspend fun choices(module: CommunicationModule): List<CommunicationChoice> = withContext(Dispatchers.IO) {
        checkAccess(module)
        fun choice(id: String, label: String) = CommunicationChoice(CommunicationRequest(module, id), label)
        when (module) {
            CommunicationModule.INVOICE, CommunicationModule.QUOTATION, CommunicationModule.SALES_ORDER ->
                db.salesDao().getAllInvoicesSync().filter {
                    when (module) {
                        CommunicationModule.QUOTATION -> it.status == InvoiceStatus.QUOTATION.name
                        CommunicationModule.SALES_ORDER -> it.status == InvoiceStatus.SALES_ORDER.name
                        else -> it.isPosted && it.status != InvoiceStatus.CANCELLED.name
                    }
                }.map { choice(it.id, "${it.invoiceNumber} · ${it.customerName}") }
            CommunicationModule.PAYMENT ->
                db.customerPaymentDao().getAllPaymentsSync().filter { it.isPosted && !it.isVoided }
                    .map { choice(it.id, "${it.paymentNumber} · ${it.customerName}") }
            CommunicationModule.RETURN ->
                db.salesReturnDao().getAllReturnsSync().filter { it.isPosted }
                    .map { choice(it.id, "${it.returnNumber} · ${it.customerName}") }
            CommunicationModule.EXCHANGE ->
                dao.snapshots(CommunicationModule.EXCHANGE.name).map { choice(it.documentId, it.number) }
            CommunicationModule.LEDGER, CommunicationModule.STATEMENT, CommunicationModule.REMINDER ->
                db.customerDao().getLedgerCustomersSync().map { choice(it.id, "${it.name} · ${it.phone}") }
            CommunicationModule.PURCHASE_ORDER, CommunicationModule.PACKING ->
                db.purchaseOrderDao().getAllOrdersSync().filter { it.status != PurchaseOrderStatus.CANCELLED.name }
                    .map { choice(it.id, it.poNumber) }
            CommunicationModule.SUPPLIER_PAYMENT ->
                db.supplierPaymentDao().getAllPaymentsSync().map { choice(it.id, it.paymentNumber) }
            CommunicationModule.SUPPLIER_STATEMENT ->
                db.supplierDao().getAllSuppliersIncludingInactiveSync().map { choice(it.id, "${it.companyName} · ${it.phone}") }
            CommunicationModule.DELIVERY ->
                db.salesDao().getAllInvoicesSync().filter { it.isPosted && it.status != InvoiceStatus.CANCELLED.name }.flatMap { inv ->
                    sales.getDeliveryChallansForInvoice(inv.id)
                        .map { choice("${inv.id}|${it.deliveryNumber}", "${it.deliveryNumber} · ${it.customerName}") }
                }
        }
    }

    suspend fun prepare(
        request: CommunicationRequest,
        languageOverride: String? = null,
        statusOverride: CommunicationStatus? = null,
        forcePdf: Boolean = false,
        useSystemDefaultTemplate: Boolean = false
    ): PreparedCommunication = withContext(Dispatchers.IO) {
        checkAccess(request.module)
        val settings = settings()

        // Pre-fetch Room Flow-based supplier ledger statements OUTSIDE db.withTransaction to avoid SQLite connection-pool deadlock.
        var preloadedSupplierFullStmt: PartyLedgerStatement? = null
        var preloadedSupplierRangeStmt: PartyLedgerStatement? = null
        var resolvedRangeToMillis: Long? = null
        if (request.module.isSupplierModule()) {
            val supplierId = when (request.module) {
                CommunicationModule.PURCHASE_ORDER, CommunicationModule.PACKING ->
                    db.purchaseOrderDao().getOrderById(request.documentId)?.supplierId
                CommunicationModule.SUPPLIER_PAYMENT ->
                    db.supplierPaymentDao().getPaymentById(request.documentId)?.supplierId
                CommunicationModule.SUPPLIER_STATEMENT -> request.documentId
                else -> null
            }
            if (supplierId != null) {
                val allTimeUpper = maxOf(System.currentTimeMillis() + 365L * 86_400_000L, 4_102_444_800_000L)
                preloadedSupplierFullStmt = controlCenter.getSupplierLedgerStatement(supplierId, 0L, allTimeUpper)
                if (request.module == CommunicationModule.SUPPLIER_STATEMENT) {
                    val from = request.from ?: 0L
                    val to = minOf(request.to ?: System.currentTimeMillis(), allTimeUpper)
                    require(from <= to) { "Invalid date range" }
                    resolvedRangeToMillis = to
                    preloadedSupplierRangeStmt = controlCenter.getSupplierLedgerStatement(supplierId, from, to)
                }
            }
        }

        val initialDoc = db.withTransaction {
            readDocument(
                request = request,
                language = languageOverride ?: settings.defaultLanguage,
                preloadedSupplierFullStmt = preloadedSupplierFullStmt,
                preloadedSupplierRangeStmt = preloadedSupplierRangeStmt,
                resolvedRangeToMillis = resolvedRangeToMillis
            )
        }
        val preference = dao.preference((if (initialDoc.supplier) "supplier:" else "customer:") + initialDoc.partyId)
        val language = languageOverride ?: preference?.language ?: settings.defaultLanguage
        val localized = if (language == (languageOverride ?: settings.defaultLanguage)) initialDoc else {
            db.withTransaction {
                readDocument(
                    request = request,
                    language = language,
                    preloadedSupplierFullStmt = preloadedSupplierFullStmt,
                    preloadedSupplierRangeStmt = preloadedSupplierRangeStmt,
                    resolvedRangeToMillis = resolvedRangeToMillis
                )
            }
        }

        // Contextual status preference without silently hiding a saved custom template for localized.status
        val contextualStatus = when {
            (localized.balanceMinor ?: 0L) < 0L -> CommunicationStatus.CREDIT
            (localized.previousMinor ?: 0L) > 0L -> CommunicationStatus.PREVIOUS_DUE
            else -> localized.status
        }

        val customExact: CommunicationTemplate?
        val chosenStatus: CommunicationStatus
        if (useSystemDefaultTemplate) {
            customExact = null
            chosenStatus = statusOverride ?: contextualStatus
        } else if (statusOverride != null) {
            chosenStatus = statusOverride
            customExact = dao.template(request.module.name, statusOverride.name, language)
        } else {
            val savedLocalized = dao.template(request.module.name, localized.status.name, language)
            val savedContextual = dao.template(request.module.name, contextualStatus.name, language)
            if (savedLocalized != null) {
                chosenStatus = localized.status
                customExact = savedLocalized
            } else if (savedContextual != null) {
                chosenStatus = contextualStatus
                customExact = savedContextual
            } else {
                chosenStatus = contextualStatus
                customExact = null
            }
        }

        val template = customExact?.body ?: DynamicTemplateEngine.defaultTemplate(request.module, chosenStatus, language)
        val message = if (settings.autoGenerate) {
            DynamicTemplateEngine.render(template, DynamicTemplateEngine.fields(localized, settings, language))
        } else {
            ""
        }
        var attachmentError: String? = null
        val file = if (settings.includePdf || forcePdf) {
            try {
                attachments.pdf(localized, language)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                attachmentError = e.message ?: "PDF generation failed"
                null
            }
        } else null

        PreparedCommunication(
            document = localized,
            language = language,
            message = message,
            template = template,
            isCustomTemplate = customExact != null,
            attachment = file,
            attachmentError = attachmentError
        )
    }

    private suspend fun readDocument(
        request: CommunicationRequest,
        language: String,
        preloadedSupplierFullStmt: PartyLedgerStatement? = null,
        preloadedSupplierRangeStmt: PartyLedgerStatement? = null,
        resolvedRangeToMillis: Long? = null
    ): SafeDocument {
        val bn = language == "bn"
        val locale = Locale(if (bn) "bn" else "en")
        fun date(value: Long) = SimpleDateFormat("dd MMM yyyy HH:mm", locale).format(Date(value))
        fun shortDate(value: Long) = SimpleDateFormat("dd MMM yyyy", locale).format(Date(value))
        fun money(value: Long, currency: String = "BDT") = "$currency ${BigDecimal.valueOf(value, 2).toPlainString()}"
        fun balance(value: Long) = if (value < 0) "${money(-value)} ${if (bn) "ক্রেডিট" else "credit"}" else "${money(value)} ${if (bn) "বকেয়া" else "due"}"

        val f = linkedMapOf<String, String>()
        val profile = db.businessProfileDao().getProfileSync()
        f["company_name"] = profile?.companyName.orEmpty()
        f["company_phone"] = profile?.phone.orEmpty()
        f["company_address"] = profile?.address.orEmpty()
        f["as_of"] = date(System.currentTimeMillis())

        var number = ""
        var partyId = ""
        var name = ""
        var phone = ""
        var supplier = false
        var outstanding: Long? = null
        var previous: Long? = null
        var status = CommunicationStatus.GENERAL
        var rows = emptyList<List<String>>()
        var columns = emptyList<String>()
        val warnings = mutableListOf<String>()
        var validUntilMillis: Long? = null
        var scheduledDeliveryInfo: String? = null

        suspend fun customer(id: String) {
            val c = requireNotNull(db.customerDao().getCustomerById(id)) { "Customer unavailable / গ্রাহক পাওয়া যায়নি" }
            partyId = id
            name = c.name
            phone = c.phone
            outstanding = sales.getCustomerLedger(id).lastOrNull()?.runningBalanceMinor ?: c.outstandingBalanceMinor
            f["customer_name"] = name
            f["customer_phone"] = phone
            f["total_outstanding"] = balance(outstanding!!)
        }

        suspend fun supplierParty(id: String) {
            val s = requireNotNull(db.supplierDao().getSupplierById(id)) { "Supplier unavailable" }
            supplier = true
            partyId = id
            name = s.companyName
            phone = s.phone
            // Authoritative supplier payable pre-fetched outside transaction to prevent Flow/pool deadlock
            outstanding = preloadedSupplierFullStmt?.closingBalanceMinor ?: s.outstandingPayableMinor
            f["supplier_name"] = name
            f["total_outstanding"] = balance(outstanding!!)
        }

        fun itemColumns() = if (bn) listOf("পণ্য", "পরিমাণ", "মূল্য") else listOf("Product", "Quantity", "Amount")

        when (request.module) {
            CommunicationModule.INVOICE, CommunicationModule.QUOTATION, CommunicationModule.SALES_ORDER -> {
                val inv = requireNotNull(db.salesDao().getInvoiceById(request.documentId)) { "Invoice unavailable" }
                require(inv.status != InvoiceStatus.CANCELLED.name) { "Cancelled document cannot be shared as active" }
                if (request.module == CommunicationModule.INVOICE) require(inv.isPosted) { "Post the invoice first" }
                customer(inv.customerId)
                number = inv.invoiceNumber
                f["document_date"] = date(inv.invoiceDate)
                val items = db.salesDao().getItemsForInvoice(inv.id)
                rows = items.map {
                    listOf(
                        if (bn) it.productNameBn.ifBlank { it.productNameEn } else it.productNameEn,
                        it.quantity.toString(),
                        money(it.lineTotalMinor)
                    )
                }
                columns = itemColumns()
                f["total_quantity"] = items.sumOf { it.quantity }.toString()

                if (request.module == CommunicationModule.INVOICE) {
                    val allocations = db.customerPaymentDao().getAllocationsForInvoiceSync(inv.id)
                    val payments = db.customerPaymentDao().getPaymentsForCustomerSync(inv.customerId).associateBy { it.id }
                    val allocated = allocations
                        .filter { payments[it.paymentId]?.let { p -> p.isPosted && !p.isVoided } == true }
                        .sumOf { it.amountMinor }
                    val creditReturns = db.salesReturnDao().getReturnsForCustomerSync(inv.customerId)
                        .filter { it.invoiceId == inv.id && it.isPosted && it.refundOption == ReturnRefundOption.CUSTOMER_CREDIT.name }
                        .sumOf { it.totalReturnAmountMinor }
                    val settlement = PaymentAllocationValidation.computeInvoiceSettlement(
                        totalAmountMinor = inv.totalAmountMinor,
                        activeAllocatedMinor = allocated,
                        creditReturnMinor = creditReturns,
                        isPosted = inv.isPosted,
                        currentStatus = inv.status
                    )
                    status = when {
                        settlement.dueAmountMinor == 0L && creditReturns > 0L -> CommunicationStatus.SETTLED
                        settlement.dueAmountMinor == 0L -> CommunicationStatus.PAID
                        allocated > 0L -> CommunicationStatus.PARTIAL
                        else -> CommunicationStatus.UNPAID
                    }
                    f["invoice_number"] = number
                    f["invoice_date"] = date(inv.invoiceDate)
                    f["invoice_amount"] = money(inv.totalAmountMinor)
                    if (creditReturns > 0L) f["return_amount"] = money(creditReturns)
                    f["received_payment"] = money(allocated)
                    f["invoice_due"] = money(settlement.dueAmountMinor)
                    inv.dueDate?.let { f["due_date"] = shortDate(it) }

                    val snap = dao.snapshot(CommunicationModule.INVOICE.name, inv.id)
                    if (snap != null) {
                        previous = snap.previousBalanceMinor
                        f["previous_due"] = balance(snap.previousBalanceMinor)
                        f["historical_as_of"] = date(snap.createdAt)
                        f["historical_payment"] = money(snap.allocatedPaymentMinor)
                        f["historical_due"] = money(snap.invoiceDueMinor)
                        f["historical_balance"] = balance(snap.closingBalanceMinor)
                    } else {
                        val unavailable = if (bn) "অনুপলব্ধ — পুরোনো ইনভয়েসের snapshot নেই" else "Unavailable — no original posting snapshot"
                        f["previous_due"] = unavailable
                        warnings.add(unavailable)
                    }
                } else if (request.module == CommunicationModule.QUOTATION) {
                    f["document_amount"] = money(inv.totalAmountMinor)
                    validUntilMillis = inv.validUntilDate
                    if (inv.validUntilDate != null) {
                        f["valid_until"] = shortDate(inv.validUntilDate)
                    } else {
                        warnings.add(if (bn) "কোটেশনের মেয়াদ এখনও সেট করা হয়নি।" else "Quotation validity date is not set.")
                    }
                    f.remove("total_outstanding")
                } else {
                    // SALES_ORDER
                    f["document_amount"] = money(inv.totalAmountMinor)
                    scheduledDeliveryInfo = inv.scheduledDeliveryInfo
                    if (!inv.scheduledDeliveryInfo.isNullOrBlank()) {
                        f["delivery_information"] = inv.scheduledDeliveryInfo
                    } else {
                        warnings.add(if (bn) "বিক্রয় আদেশের ডেলিভারির তথ্য এখনও সংরক্ষিত নয়।" else "Sales order delivery schedule is not set.")
                    }
                    f.remove("total_outstanding")
                }
            }

            CommunicationModule.PAYMENT -> {
                val p = requireNotNull(db.customerPaymentDao().getPaymentById(request.documentId)) { "Payment unavailable" }
                require(p.isPosted && !p.isVoided) { "Voided / unposted payment cannot be shared as a receipt" }
                customer(p.customerId)
                number = p.paymentNumber
                f.putAll(
                    mapOf(
                        "payment_number" to number,
                        "payment_date" to date(p.paymentDate),
                        "payment_amount" to money(p.amountMinor),
                        "payment_method" to p.paymentMethod,
                        "payment_reference" to p.referenceNote.orEmpty()
                    )
                )
                columns = if (bn) listOf("ইনভয়েস", "বরাদ্দ") else listOf("Invoice", "Allocated")
                rows = db.customerPaymentDao().getAllocationsForPayment(p.id).map { listOf(it.invoiceNumber, money(it.amountMinor)) }
            }

            CommunicationModule.LEDGER, CommunicationModule.STATEMENT, CommunicationModule.REMINDER -> {
                customer(request.documentId)
                number = "${request.module.name}-$name"
                if (request.module != CommunicationModule.REMINDER) {
                    val from = request.from ?: 0L
                    val to = request.to ?: System.currentTimeMillis()
                    require(from <= to) { "Invalid date range" }
                    val ledger = sales.getCustomerLedger(partyId)
                    val opening = ledger.lastOrNull { it.date < from }?.runningBalanceMinor ?: 0L
                    val selected = ledger.filter { it.date in from..to }
                    f["opening_balance"] = balance(opening)
                    f["closing_balance"] = balance(selected.lastOrNull()?.runningBalanceMinor ?: opening)
                    f["date_range"] = (if (from == 0L) (if (bn) "শুরু থেকে" else "Beginning") else shortDate(from)) + " — " + shortDate(to)
                    columns = if (bn) listOf("তারিখ", "রেফারেন্স", "ডেবিট", "ক্রেডিট", "ব্যালেন্স") else listOf("Date", "Reference", "Debit", "Credit", "Balance")
                    rows = selected.map {
                        listOf(
                            shortDate(it.date),
                            it.referenceNumber,
                            money(it.debitMinor),
                            money(it.creditMinor),
                            balance(it.runningBalanceMinor)
                        )
                    }
                } else {
                    val earliestDue = db.salesDao().getUnpaidInvoicesForCustomer(partyId)
                        .mapNotNull { it.dueDate }
                        .minOrNull()
                    earliestDue?.let { f["due_date"] = shortDate(it) }
                }
            }

            CommunicationModule.RETURN -> {
                val r = requireNotNull(db.salesReturnDao().getReturnById(request.documentId)) { "Return unavailable" }
                require(r.isPosted) { "Unposted return cannot be shared" }
                customer(r.customerId)
                number = r.returnNumber
                f["document_date"] = date(r.returnDate)
                f["return_amount"] = money(r.totalReturnAmountMinor)
                f["refund_amount"] = money(if (r.refundOption == ReturnRefundOption.CUSTOMER_CREDIT.name) 0L else r.totalReturnAmountMinor)
                f["payment_method"] = r.refundOption
                rows = db.salesReturnDao().getItemsForReturn(r.id).map {
                    listOf(
                        if (bn) it.productNameBn.ifBlank { it.productNameEn } else it.productNameEn,
                        it.returnQuantity.toString(),
                        money(it.returnTotalMinor)
                    )
                }
                columns = itemColumns()
            }

            CommunicationModule.DELIVERY -> {
                val id = request.documentId.substringBefore('|')
                val no = request.documentId.substringAfter('|')
                val inv = requireNotNull(db.salesDao().getInvoiceById(id)) { "Invoice unavailable" }
                require(inv.status != InvoiceStatus.CANCELLED.name) { "Cancelled invoice delivery unavailable" }
                val d = sales.getDeliveryChallansForInvoice(id).firstOrNull { it.deliveryNumber == no }
                    ?: error("Delivery challan unavailable")
                customer(inv.customerId)
                number = d.deliveryNumber
                f.remove("total_outstanding")
                f["document_date"] = date(d.deliveryDate)
                f["delivery_information"] = listOfNotNull(d.deliveryPerson, d.transportInfo).filter { it.isNotBlank() }.joinToString(" · ")
                rows = d.items.map {
                    listOf(
                        if (bn) it.productNameBn.ifBlank { it.productNameEn } else it.productNameEn,
                        it.dispatchedQuantity.toString()
                    )
                }
                columns = if (bn) listOf("পণ্য", "পরিমাণ") else listOf("Product", "Quantity")
                f["total_quantity"] = d.items.sumOf { it.dispatchedQuantity }.toString()
            }

            CommunicationModule.EXCHANGE -> {
                val snap = requireNotNull(dao.snapshot(CommunicationModule.EXCHANGE.name, request.documentId)) {
                    "Original exchange details unavailable; no fabricated history"
                }
                customer(snap.partyId)
                number = snap.number
                f["document_date"] = date(snap.createdAt)
                val json = JSONObject(snap.payload)
                f["price_difference"] = money(snap.originalAmountMinor)
                f["payment_method"] = json.optString("method")
                val a = json.getJSONArray("rows")
                rows = (0 until a.length()).map { i ->
                    val row = a.getJSONArray(i)
                    (0 until row.length()).map { row.getString(it) }
                }
                columns = if (bn) listOf("ধরন", "পণ্য", "পরিমাণ") else listOf("Direction", "Product", "Quantity")
            }

            CommunicationModule.PURCHASE_ORDER, CommunicationModule.PACKING -> {
                val po = requireNotNull(db.purchaseOrderDao().getOrderById(request.documentId)) { "Purchase order unavailable" }
                require(po.status != PurchaseOrderStatus.CANCELLED.name) { "Cancelled order unavailable" }
                supplierParty(po.supplierId)
                number = po.poNumber
                f["document_date"] = date(po.orderDate)
                val items = db.purchaseOrderDao().getItemsForOrderSync(po.id)
                rows = items.map { item ->
                    val product = db.productDao().getProductById(item.productId)
                    listOf(
                        if (bn) product?.nameBn?.ifBlank { product.nameEn } ?: item.productId else product?.nameEn ?: item.productId,
                        item.quantity.toString(),
                        if (request.module == CommunicationModule.PACKING) item.cartonQuantity.toString() else money(item.totalAmountMinor, po.currency)
                    )
                }
                f["total_quantity"] = items.sumOf { it.quantity }.toString()
                if (request.module == CommunicationModule.PACKING) {
                    f.remove("total_outstanding")
                    f["total_carton"] = po.totalCartons.toString()
                    f["gross_weight"] = po.totalGrossWeightKg
                    f["total_cbm"] = po.totalCbm
                    columns = if (bn) listOf("পণ্য", "পরিমাণ", "কার্টন") else listOf("Product", "Quantity", "Cartons")
                } else {
                    f["document_amount"] = money(po.totalAmountMinor, po.currency)
                    columns = itemColumns()
                }
            }

            CommunicationModule.SUPPLIER_PAYMENT -> {
                val p = requireNotNull(db.supplierPaymentDao().getPaymentById(request.documentId)) { "Supplier payment unavailable" }
                supplierParty(p.supplierId)
                number = p.paymentNumber
                f.putAll(
                    mapOf(
                        "payment_number" to number,
                        "payment_date" to date(p.paymentDate),
                        "payment_amount" to money(p.amountMinor, p.currency),
                        "payment_method" to p.paymentMethod,
                        "payment_reference" to p.referenceNumber.orEmpty()
                    )
                )
            }

            CommunicationModule.SUPPLIER_STATEMENT -> {
                supplierParty(request.documentId)
                val from = request.from ?: 0L
                val to = resolvedRangeToMillis ?: request.to ?: System.currentTimeMillis()
                require(from <= to) { "Invalid date range" }
                number = "SUP-STMT-$name"
                val stmt = preloadedSupplierRangeStmt
                val opening = stmt?.openingBalanceMinor ?: 0L
                val closing = stmt?.closingBalanceMinor ?: (outstanding ?: 0L)
                f["opening_balance"] = balance(opening)
                f["closing_balance"] = balance(closing)
                f["date_range"] = (if (from == 0L) (if (bn) "শুরু থেকে" else "Beginning") else shortDate(from)) + " — " + shortDate(to)
                columns = if (bn) listOf("তারিখ", "রেফারেন্স", "ডেবিট", "ক্রেডিট", "ব্যালেন্স") else listOf("Date", "Reference", "Debit", "Credit", "Balance")
                rows = stmt?.transactions.orEmpty().asReversed().map { tx ->
                    listOf(
                        shortDate(tx.date),
                        tx.referenceNumber,
                        money(tx.ledgerDebitMinor ?: 0L),
                        money(tx.ledgerCreditMinor ?: 0L),
                        balance(tx.runningBalanceMinor ?: 0L)
                    )
                }
            }
        }

        f["document_number"] = number
        if (rows.isNotEmpty() && request.module !in setOf(
                CommunicationModule.LEDGER,
                CommunicationModule.STATEMENT,
                CommunicationModule.SUPPLIER_STATEMENT,
                CommunicationModule.PAYMENT
            )
        ) {
            f["items"] = rows.joinToString("\n") { it.joinToString(" · ") }
        }
        return SafeDocument(
            request = request,
            number = number,
            partyId = partyId,
            partyName = name,
            phone = phone,
            supplier = supplier,
            fields = f,
            rows = rows,
            columns = columns,
            status = status,
            balanceMinor = outstanding,
            previousMinor = previous,
            warnings = warnings,
            validUntilMillis = validUntilMillis,
            scheduledDeliveryInfo = scheduledDeliveryInfo
        )
    }
}
