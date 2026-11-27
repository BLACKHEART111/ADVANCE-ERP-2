package com.pocketdev.advance_erp.core.communication

import androidx.room.withTransaction
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.engine.PaymentAllocationValidation
import com.pocketdev.advance_erp.core.security.SecurityManager
import com.pocketdev.advance_erp.core.util.resolveVariantPhotoUris
import com.pocketdev.advance_erp.data.entity.*
import com.pocketdev.advance_erp.domain.model.PartyLedgerStatement
import com.pocketdev.advance_erp.domain.repository.BusinessControlCenterRepository
import com.pocketdev.advance_erp.domain.repository.CustomerLedgerEntry
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

    suspend fun generatePdf(document: SafeDocument, language: String): File {
        checkAccess(document.request.module)
        return attachments.pdf(document, language, settings())
    }

    suspend fun generateImageCards(document: SafeDocument, language: String): List<File> {
        checkAccess(document.request.module)
        return attachments.imageCards(document, language, settings())
    }

    suspend fun saveImagesToGallery(files: List<File>): List<String> {
        requireReportAccess()
        return attachments.saveImagesToGallery(files)
    }

    suspend fun saveAttachmentToDownloads(file: File): String {
        requireReportAccess()
        return attachments.saveDocumentToDownloads(file)
    }

    suspend fun getAttachmentCacheStats(): CommunicationCacheStats {
        requireReportAccess()
        return attachments.getCacheStats()
    }

    suspend fun clearAttachmentCache(): Int {
        requireReportAccess()
        return attachments.clearCache()
    }

    suspend fun clearHistory() {
        requireReportAccess()
        dao.clearHistory()
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
        useSystemDefaultTemplate: Boolean = false,
        forceImage: Boolean = false
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
                settings = settings,
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
                    settings = settings,
                    preloadedSupplierFullStmt = preloadedSupplierFullStmt,
                    preloadedSupplierRangeStmt = preloadedSupplierRangeStmt,
                    resolvedRangeToMillis = resolvedRangeToMillis
                )
            }
        }

        // Contextual status preference without silently hiding a saved custom template for localized.status
        val isOverdue = when (request.module) {
            CommunicationModule.INVOICE -> localized.invoiceBreakdown?.let { inv ->
                inv.invoiceDueMinor > 0L && inv.dueDateMillis != null && inv.dueDateMillis < localized.asOfMillis
            } == true
            CommunicationModule.REMINDER -> localized.reminderBreakdown?.let { rem ->
                rem.currentTotalDueMinor > 0L && rem.earliestDueDateMillis != null && rem.earliestDueDateMillis < localized.asOfMillis
            } == true
            else -> false
        }
        val contextualStatus = when {
            (localized.balanceMinor ?: 0L) < 0L -> CommunicationStatus.CREDIT
            isOverdue -> CommunicationStatus.OVERDUE
            (localized.previousMinor ?: 0L) > 0L && localized.status in setOf(CommunicationStatus.UNPAID, CommunicationStatus.PARTIAL) ->
                CommunicationStatus.PREVIOUS_DUE
            else -> localized.status
        }

        val resolvedDocument = when {
            (localized.balanceMinor ?: 0L) < 0L && localized.status != CommunicationStatus.CREDIT ->
                localized.copy(status = CommunicationStatus.CREDIT)
            isOverdue && localized.status != CommunicationStatus.OVERDUE ->
                localized.copy(status = CommunicationStatus.OVERDUE)
            else -> localized
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
            val savedLocalized = dao.template(request.module.name, resolvedDocument.status.name, language)
            val savedContextual = dao.template(request.module.name, contextualStatus.name, language)
            if (savedLocalized != null) {
                chosenStatus = resolvedDocument.status
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
            DynamicTemplateEngine.render(template, DynamicTemplateEngine.fields(resolvedDocument, settings, language))
        } else {
            ""
        }
        var attachmentError: String? = null
        val file = if (settings.includePdf || forcePdf) {
            try {
                attachments.pdf(resolvedDocument, language, settings)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                attachmentError = e.message ?: "PDF generation failed"
                null
            }
        } else null

        var imageError: String? = null
        val imagePages = if (settings.includeImageCard || forceImage) {
            try {
                attachments.imageCards(resolvedDocument, language, settings)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                imageError = e.message ?: "Image card generation failed"
                emptyList()
            }
        } else emptyList()

        PreparedCommunication(
            document = resolvedDocument,
            language = language,
            message = message,
            template = template,
            isCustomTemplate = customExact != null,
            attachment = file,
            attachmentError = attachmentError,
            pdfFile = file,
            imageCardFile = imagePages.firstOrNull(),
            imageCardPages = imagePages,
            imageError = imageError,
            selectedFormats = settings.defaultSelectedFormats()
        )
    }

    private suspend fun readDocument(
        request: CommunicationRequest,
        language: String,
        settings: CommunicationSettings,
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

        val nowAsOf = System.currentTimeMillis()
        val f = linkedMapOf<String, String>()
        val profile = db.businessProfileDao().getProfileSync()
        val brand = CompanyBrandInfo(
            companyName = profile?.companyName?.trim()?.ifBlank { "ADVANCE ERP" } ?: "ADVANCE ERP",
            tagline = profile?.tagline?.trim()?.takeIf { it.isNotEmpty() },
            address = profile?.address.orEmpty(),
            phone = profile?.phone.orEmpty(),
            email = profile?.email?.trim()?.takeIf { it.isNotEmpty() },
            website = profile?.website?.trim()?.takeIf { it.isNotEmpty() },
            binNumber = profile?.taxNumber?.trim()?.takeIf { it.isNotEmpty() },
            currencyCode = profile?.defaultCurrency?.ifBlank { "BDT" } ?: "BDT",
            logoPath = profile?.logoPath?.trim()?.takeIf { it.isNotEmpty() }
        )
        f["company_name"] = profile?.companyName.orEmpty()
        f["company_phone"] = profile?.phone.orEmpty()
        f["company_address"] = profile?.address.orEmpty()
        brand.website?.let { f["company_website"] = it }
        brand.binNumber?.let { f["company_bin"] = it }
        brand.tagline?.let { f["company_tagline"] = it }
        f["as_of"] = date(nowAsOf)

        var resolvedDocumentId = request.documentId
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
        var documentDateMillis: Long? = null
        var partyInfo: RecipientPartyInfo? = null
        var lineItems = emptyList<RichDocumentLineItem>()
        var invoiceBreakdown: InvoiceFinancialBreakdown? = null
        var paymentBreakdown: PaymentReceiptBreakdown? = null
        var reminderBreakdown: DueReminderBreakdown? = null
        var statementBreakdown: LedgerStatementBreakdown? = null
        var returnExchangeBreakdown: ReturnOrExchangeBreakdown? = null
        var deliveryBreakdown: DeliveryChallanBreakdown? = null
        var cachedCustomerLedger: List<CustomerLedgerEntry> = emptyList()

        var catalogLoaded = false
        var productsMap: Map<String, Product> = emptyMap()
        var variantsMap: Map<String, ProductVariant> = emptyMap()
        var primaryProductPhotos: Map<String, String> = emptyMap()
        var resolvedVariantPhotos: Map<String, String?> = emptyMap()

        fun readableImagePath(raw: String?): String? {
            val clean = raw?.trim()?.takeIf { it.isNotEmpty() }?.removePrefix("file://") ?: return null
            val file = File(clean)
            return if (file.isFile && file.length() > 0L) file.absolutePath else null
        }

        suspend fun ensureCatalogLoaded() {
            if (catalogLoaded) return
            catalogLoaded = true
            productsMap = db.productDao().getAllProductsIncludingInactiveSync().associateBy { it.id }
            val allVariants = db.productVariantDao().getAllVariantsIncludingInactiveSync()
            variantsMap = allVariants.associateBy { it.id }
            val allImages = db.productImageDao().getAllImagesSync()
            primaryProductPhotos = allImages
                .filter { it.variantId == null }
                .groupBy { it.productId }
                .mapValues { (_, list) -> list.firstNotNullOfOrNull { readableImagePath(it.imageUri) } }
                .filterValues { it != null }
                .mapValues { it.value!! }
            val variantOwnPhotos = allImages
                .filter { it.variantId != null }
                .groupBy { it.variantId!! }
                .mapValues { (_, list) -> list.firstNotNullOfOrNull { readableImagePath(it.imageUri) } }
                .filterValues { it != null }
                .mapValues { it.value!! }
            resolvedVariantPhotos = resolveVariantPhotoUris(allVariants, variantOwnPhotos)
        }

        fun resolveThumbnail(productId: String, variantId: String?): String? {
            if (!settings.showProductPhoto) return null
            val variantMatch = variantId?.let { resolvedVariantPhotos[it] }
            return readableImagePath(variantMatch) ?: primaryProductPhotos[productId]
        }

        suspend fun customer(id: String) {
            val c = requireNotNull(db.customerDao().getCustomerById(id)) { "Customer unavailable / গ্রাহক পাওয়া যায়নি" }
            partyId = id
            name = c.name
            phone = c.phone
            val ledger = sales.getCustomerLedger(id)
            cachedCustomerLedger = ledger
            outstanding = ledger.lastOrNull()?.runningBalanceMinor ?: c.outstandingBalanceMinor
            partyInfo = RecipientPartyInfo(
                id = c.id,
                code = c.customerCode,
                name = c.name,
                phone = c.phone,
                address = c.address.trim().takeIf { it.isNotEmpty() },
                email = c.email?.trim()?.takeIf { it.isNotEmpty() },
                isSupplier = false
            )
            f["customer_name"] = name
            f["customer_phone"] = phone
            if (c.customerCode.isNotBlank()) f["customer_code"] = c.customerCode
            if (c.address.isNotBlank()) f["customer_address"] = c.address
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
            partyInfo = RecipientPartyInfo(
                id = s.id,
                code = s.supplierCode,
                name = s.companyName,
                phone = s.phone,
                address = s.address.trim().takeIf { it.isNotEmpty() },
                email = s.email?.trim()?.takeIf { it.isNotEmpty() },
                isSupplier = true
            )
            f["supplier_name"] = name
            if (s.address.isNotBlank()) f["customer_address"] = s.address
            f["total_outstanding"] = balance(outstanding!!)
        }

        fun itemColumns() = if (bn) listOf("পণ্য", "পরিমাণ", "মূল্য") else listOf("Product", "Quantity", "Amount")

        when (request.module) {
            CommunicationModule.INVOICE, CommunicationModule.QUOTATION, CommunicationModule.SALES_ORDER -> {
                val inv = requireNotNull(db.salesDao().getInvoiceById(request.documentId)) { "Invoice unavailable" }
                require(inv.status != InvoiceStatus.CANCELLED.name) { "Cancelled document cannot be shared as active" }
                if (request.module == CommunicationModule.INVOICE) require(inv.isPosted) { "Post the invoice first" }
                customer(inv.customerId)
                ensureCatalogLoaded()
                number = inv.invoiceNumber
                documentDateMillis = inv.invoiceDate
                f["document_date"] = date(inv.invoiceDate)
                val items = db.salesDao().getItemsForInvoice(inv.id)
                rows = items.map {
                    listOf(
                        if (bn) it.productNameBn.ifBlank { it.productNameEn } else it.productNameEn,
                        it.quantity.toString(),
                        money(it.lineTotalMinor)
                    )
                }
                lineItems = items.mapIndexed { idx, item ->
                    val prod = productsMap[item.productId]
                    val variant = item.variantId?.let { variantsMap[it] }
                    val resolvedSku = if (settings.showVariantSku) {
                        item.sku.ifBlank { variant?.sku ?: prod?.baseSku ?: "" }.takeIf { it.isNotBlank() }
                    } else null
                    RichDocumentLineItem(
                        index = idx + 1,
                        productId = item.productId,
                        variantId = item.variantId,
                        titleEn = item.productNameEn,
                        titleBn = item.productNameBn.ifBlank { item.productNameEn },
                        sku = resolvedSku,
                        color = variant?.color?.trim()?.takeIf { it.isNotEmpty() },
                        size = variant?.size?.trim()?.takeIf { it.isNotEmpty() },
                        unit = prod?.unit?.ifBlank { "PCS" } ?: "PCS",
                        thumbnailPath = resolveThumbnail(item.productId, item.variantId),
                        quantity = item.quantity,
                        unitPriceMinor = item.unitPriceMinor,
                        discountMinor = item.discountMinor,
                        discountPercentBps = item.discountPercentBps,
                        lineTotalMinor = item.lineTotalMinor,
                        currencyCode = brand.currencyCode
                    )
                }
                columns = itemColumns()
                f["total_quantity"] = items.sumOf { it.quantity }.toString()
                f["sales_type"] = if (inv.salesType.equals("RETAIL", ignoreCase = true)) {
                    if (bn) "খুচরা (Retail)" else "Retail"
                } else {
                    if (bn) "পাইকারি (Wholesale)" else "Wholesale"
                }
                f["subtotal_amount"] = money(inv.subtotalMinor)
                if (inv.discountMinor > 0L) f["discount_amount"] = money(inv.discountMinor)
                if (inv.taxMinor > 0L) f["additional_charges"] = money(inv.taxMinor)
                val effectiveTerms = inv.paymentTerms?.trim()?.takeIf { it.isNotEmpty() }
                    ?: settings.defaultPaymentTerms.trim().takeIf { it.isNotEmpty() }
                if (effectiveTerms != null) f["payment_terms"] = effectiveTerms

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
                    invoiceBreakdown = CommunicationAccountingReconciler.buildInvoiceBreakdown(
                        invoice = inv,
                        items = items,
                        allocatedPaymentMinor = allocated,
                        creditReturnMinor = creditReturns,
                        snapshot = snap,
                        currentAccountBalanceMinor = outstanding ?: 0L,
                        status = status,
                        showQrCode = settings.showQrCode,
                        defaultPaymentTerms = settings.defaultPaymentTerms,
                        asOfMillis = nowAsOf
                    )
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
                val p = requireNotNull(
                    db.customerPaymentDao().getPaymentById(request.documentId)
                        ?: db.customerPaymentDao().getPaymentByNumber(request.documentId)
                ) { "Payment unavailable" }
                resolvedDocumentId = p.id
                require(p.isPosted && !p.isVoided) { "Voided / unposted payment cannot be shared as a receipt" }
                customer(p.customerId)
                number = p.paymentNumber
                documentDateMillis = p.paymentDate
                val payAllocations = db.customerPaymentDao().getAllocationsForPayment(p.id)
                val paySnap = dao.snapshot(CommunicationModule.PAYMENT.name, p.id)
                val payBreakdown = CommunicationAccountingReconciler.buildPaymentBreakdown(
                    payment = p,
                    allocations = payAllocations,
                    snapshot = paySnap,
                    customerLedger = cachedCustomerLedger,
                    currentAccountBalanceMinor = outstanding ?: 0L,
                    currencyCode = brand.currencyCode
                )
                paymentBreakdown = payBreakdown
                payBreakdown.previousBalanceMinor?.let { prev ->
                    previous = prev
                    f["previous_balance"] = balance(prev)
                }
                payBreakdown.balanceAfterPaymentMinor?.let { balAfter ->
                    f["balance_after_payment"] = balance(balAfter)
                }
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
                rows = payAllocations.map { listOf(it.invoiceNumber, money(it.amountMinor)) }
            }

            CommunicationModule.LEDGER, CommunicationModule.STATEMENT, CommunicationModule.REMINDER -> {
                customer(request.documentId)
                number = "${request.module.name}-$name"
                if (request.module != CommunicationModule.REMINDER) {
                    val from = request.from ?: 0L
                    val to = request.to ?: nowAsOf
                    require(from <= to) { "Invalid date range" }
                    documentDateMillis = to
                    val stmt = CommunicationAccountingReconciler.buildCustomerStatementBreakdown(
                        customerLedger = cachedCustomerLedger,
                        fromMillis = from,
                        toMillis = to,
                        isAllTime = (request.from == null || request.from == 0L),
                        currencyCode = brand.currencyCode
                    )
                    statementBreakdown = stmt
                    f["opening_balance"] = balance(stmt.openingBalanceMinor)
                    f["closing_balance"] = balance(stmt.closingBalanceMinor)
                    f["total_sales"] = money(stmt.totalSalesOrPurchasesMinor)
                    f["total_returns"] = money(stmt.totalReturnMinor)
                    f["total_payments"] = money(stmt.totalPaymentMinor)
                    f["date_range"] = (if (from == 0L) (if (bn) "শুরু থেকে" else "Beginning") else shortDate(from)) + " — " + shortDate(to)
                    columns = if (bn) listOf("তারিখ", "রেফারেন্স", "ডেবিট", "ক্রেডিট", "ব্যালেন্স") else listOf("Date", "Reference", "Debit", "Credit", "Balance")
                    rows = stmt.transactions.map {
                        listOf(
                            shortDate(it.dateMillis),
                            it.referenceNumber,
                            money(it.debitMinor),
                            money(it.creditMinor),
                            balance(it.runningBalanceMinor)
                        )
                    }
                } else {
                    val unpaidList = db.salesDao().getUnpaidInvoicesForCustomer(partyId)
                    val earliestDue = unpaidList.mapNotNull { it.dueDate }.minOrNull()
                    earliestDue?.let { f["due_date"] = shortDate(it) }
                    reminderBreakdown = CommunicationAccountingReconciler.buildDueReminderBreakdown(
                        customerLedger = cachedCustomerLedger,
                        customerOutstandingFallback = outstanding ?: 0L,
                        unpaidInvoiceDueDates = unpaidList.map { it.dueDate },
                        asOfMillis = nowAsOf
                    )
                    documentDateMillis = nowAsOf
                }
            }

            CommunicationModule.RETURN -> {
                val r = requireNotNull(
                    db.salesReturnDao().getReturnById(request.documentId)
                        ?: db.salesReturnDao().getReturnByNumber(request.documentId)
                ) { "Return unavailable" }
                resolvedDocumentId = r.id
                require(r.isPosted) { "Unposted return cannot be shared" }
                customer(r.customerId)
                ensureCatalogLoaded()
                number = r.returnNumber
                documentDateMillis = r.returnDate
                f["document_date"] = date(r.returnDate)
                f["return_amount"] = money(r.totalReturnAmountMinor)
                val cashRefund = if (r.refundOption == ReturnRefundOption.CUSTOMER_CREDIT.name) 0L else r.totalReturnAmountMinor
                val creditToAccount = if (r.refundOption == ReturnRefundOption.CUSTOMER_CREDIT.name) r.totalReturnAmountMinor else 0L
                f["refund_amount"] = money(cashRefund)
                f["payment_method"] = r.refundOption
                val returnItems = db.salesReturnDao().getItemsForReturn(r.id)
                rows = returnItems.map {
                    listOf(
                        if (bn) it.productNameBn.ifBlank { it.productNameEn } else it.productNameEn,
                        it.returnQuantity.toString(),
                        money(it.returnTotalMinor)
                    )
                }
                lineItems = returnItems.mapIndexed { idx, item ->
                    val prod = productsMap[item.productId]
                    val variant = item.variantId?.let { variantsMap[it] }
                    val resolvedSku = if (settings.showVariantSku) {
                        variant?.sku ?: prod?.baseSku
                    } else null
                    RichDocumentLineItem(
                        index = idx + 1,
                        productId = item.productId,
                        variantId = item.variantId,
                        titleEn = item.productNameEn,
                        titleBn = item.productNameBn.ifBlank { item.productNameEn },
                        sku = resolvedSku,
                        color = variant?.color?.trim()?.takeIf { it.isNotEmpty() },
                        size = variant?.size?.trim()?.takeIf { it.isNotEmpty() },
                        unit = prod?.unit?.ifBlank { "PCS" } ?: "PCS",
                        thumbnailPath = resolveThumbnail(item.productId, item.variantId),
                        quantity = item.returnQuantity,
                        unitPriceMinor = item.unitPriceMinor,
                        lineTotalMinor = item.returnTotalMinor,
                        currencyCode = brand.currencyCode
                    )
                }
                returnExchangeBreakdown = ReturnOrExchangeBreakdown(
                    referenceInvoiceNumber = r.invoiceNumber,
                    returnAmountMinor = r.totalReturnAmountMinor,
                    cashOrBankRefundMinor = cashRefund,
                    creditedToAccountMinor = creditToAccount,
                    refundOrPaymentMethod = r.refundOption,
                    currentAccountBalanceMinor = outstanding ?: 0L
                )
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
                ensureCatalogLoaded()
                number = d.deliveryNumber
                documentDateMillis = d.deliveryDate
                f.remove("total_outstanding")
                f["document_date"] = date(d.deliveryDate)
                f["delivery_information"] = listOfNotNull(d.deliveryPerson, d.transportInfo).filter { it.isNotBlank() }.joinToString(" · ")
                rows = d.items.map {
                    listOf(
                        if (bn) it.productNameBn.ifBlank { it.productNameEn } else it.productNameEn,
                        it.dispatchedQuantity.toString()
                    )
                }
                lineItems = d.items.mapIndexed { idx, item ->
                    val prod = productsMap[item.productId]
                    val variant = item.variantId?.let { variantsMap[it] }
                    val resolvedSku = if (settings.showVariantSku) {
                        item.sku.ifBlank { variant?.sku ?: prod?.baseSku ?: "" }.takeIf { it.isNotBlank() }
                    } else null
                    RichDocumentLineItem(
                        index = idx + 1,
                        productId = item.productId,
                        variantId = item.variantId,
                        titleEn = item.productNameEn,
                        titleBn = item.productNameBn.ifBlank { item.productNameEn },
                        sku = resolvedSku,
                        color = variant?.color?.trim()?.takeIf { it.isNotEmpty() },
                        size = variant?.size?.trim()?.takeIf { it.isNotEmpty() },
                        unit = prod?.unit?.ifBlank { "PCS" } ?: "PCS",
                        thumbnailPath = resolveThumbnail(item.productId, item.variantId),
                        quantity = item.dispatchedQuantity,
                        currencyCode = brand.currencyCode
                    )
                }
                val totalDispQty = d.items.sumOf { it.dispatchedQuantity }
                deliveryBreakdown = DeliveryChallanBreakdown(
                    invoiceNumber = inv.invoiceNumber,
                    deliveryPerson = d.deliveryPerson,
                    transportInfo = d.transportInfo,
                    notes = d.notes,
                    totalDispatchedQuantity = totalDispQty
                )
                columns = if (bn) listOf("পণ্য", "পরিমাণ") else listOf("Product", "Quantity")
                f["total_quantity"] = totalDispQty.toString()
            }

            CommunicationModule.EXCHANGE -> {
                val snap = requireNotNull(dao.snapshot(CommunicationModule.EXCHANGE.name, request.documentId)) {
                    "Original exchange details unavailable; no fabricated history"
                }
                customer(snap.partyId)
                ensureCatalogLoaded()
                number = snap.number
                documentDateMillis = snap.createdAt
                f["document_date"] = date(snap.createdAt)
                val json = JSONObject(snap.payload)
                f["price_difference"] = money(snap.originalAmountMinor)
                val method = json.optString("method")
                f["payment_method"] = method
                val a = json.getJSONArray("rows")
                rows = (0 until a.length()).map { i ->
                    val row = a.getJSONArray(i)
                    (0 until row.length()).map { row.getString(it) }
                }
                val itemsJson = json.optJSONArray("items")
                lineItems = if (itemsJson != null) {
                    (0 until itemsJson.length()).map { idx ->
                        val obj = itemsJson.getJSONObject(idx)
                        val prodId = obj.optString("productId", "")
                        val varId = obj.optString("variantId", "").takeIf { it.isNotBlank() }
                        val prod = productsMap[prodId]
                        val variant = varId?.let { variantsMap[it] }
                        val dir = obj.optString("direction", "")
                        RichDocumentLineItem(
                            index = idx + 1,
                            productId = prodId,
                            variantId = varId,
                            titleEn = obj.optString("nameEn", prod?.nameEn ?: prodId),
                            titleBn = obj.optString("nameBn", prod?.nameBn ?: prodId),
                            sku = if (settings.showVariantSku) {
                                obj.optString("sku", "").ifBlank { variant?.sku ?: prod?.baseSku ?: "" }.takeIf { it.isNotBlank() }
                            } else null,
                            color = obj.optString("color", "").ifBlank { variant?.color.orEmpty() }.takeIf { it.isNotBlank() },
                            size = obj.optString("size", "").ifBlank { variant?.size.orEmpty() }.takeIf { it.isNotBlank() },
                            unit = obj.optString("unit", prod?.unit ?: "PCS"),
                            thumbnailPath = if (prodId.isNotBlank()) resolveThumbnail(prodId, varId) else null,
                            quantity = obj.optInt("qty", 0),
                            secondaryQuantityText = dir.takeIf { it.isNotBlank() },
                            unitPriceMinor = obj.optLong("unitPriceMinor", 0L),
                            lineTotalMinor = obj.optLong("lineTotalMinor", 0L),
                            currencyCode = brand.currencyCode
                        )
                    }
                } else {
                    rows.mapIndexed { idx, row ->
                        val dir = row.getOrNull(0).orEmpty()
                        val title = row.getOrNull(1).orEmpty()
                        val qty = row.getOrNull(2)?.toIntOrNull() ?: 0
                        RichDocumentLineItem(
                            index = idx + 1,
                            productId = "",
                            titleEn = title,
                            titleBn = title,
                            quantity = qty,
                            secondaryQuantityText = dir.takeIf { it.isNotBlank() },
                            currencyCode = brand.currencyCode
                        )
                    }
                }
                returnExchangeBreakdown = ReturnOrExchangeBreakdown(
                    referenceInvoiceNumber = json.optString("originalInvoiceNumber", "").takeIf { it.isNotBlank() },
                    netPriceDifferenceMinor = snap.originalAmountMinor,
                    refundOrPaymentMethod = method.takeIf { it.isNotBlank() },
                    currentAccountBalanceMinor = outstanding ?: 0L
                )
                columns = if (bn) listOf("ধরন", "পণ্য", "পরিমাণ") else listOf("Direction", "Product", "Quantity")
            }

            CommunicationModule.PURCHASE_ORDER, CommunicationModule.PACKING -> {
                val po = requireNotNull(db.purchaseOrderDao().getOrderById(request.documentId)) { "Purchase order unavailable" }
                require(po.status != PurchaseOrderStatus.CANCELLED.name) { "Cancelled order unavailable" }
                supplierParty(po.supplierId)
                ensureCatalogLoaded()
                number = po.poNumber
                documentDateMillis = po.orderDate
                f["document_date"] = date(po.orderDate)
                val items = db.purchaseOrderDao().getItemsForOrderSync(po.id)
                rows = items.map { item ->
                    val product = productsMap[item.productId]
                    listOf(
                        if (bn) product?.nameBn?.ifBlank { product.nameEn } ?: item.productId else product?.nameEn ?: item.productId,
                        item.quantity.toString(),
                        if (request.module == CommunicationModule.PACKING) item.cartonQuantity.toString() else money(item.totalAmountMinor, po.currency)
                    )
                }
                lineItems = items.mapIndexed { idx, item ->
                    val prod = productsMap[item.productId]
                    val variant = item.variantId?.let { variantsMap[it] }
                    RichDocumentLineItem(
                        index = idx + 1,
                        productId = item.productId,
                        variantId = item.variantId,
                        titleEn = prod?.nameEn ?: item.productId,
                        titleBn = prod?.nameBn?.ifBlank { prod.nameEn } ?: item.productId,
                        sku = if (settings.showVariantSku) (variant?.sku ?: prod?.baseSku)?.takeIf { it.isNotBlank() } else null,
                        color = variant?.color?.trim()?.takeIf { it.isNotEmpty() },
                        size = variant?.size?.trim()?.takeIf { it.isNotEmpty() },
                        unit = prod?.unit?.ifBlank { "PCS" } ?: "PCS",
                        thumbnailPath = resolveThumbnail(item.productId, item.variantId),
                        quantity = item.quantity,
                        secondaryQuantityText = if (item.cartonQuantity > 0) "${item.cartonQuantity} CTN" else null,
                        unitPriceMinor = item.unitPriceMinor,
                        lineTotalMinor = item.totalAmountMinor,
                        currencyCode = po.currency
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
                documentDateMillis = p.paymentDate
                paymentBreakdown = PaymentReceiptBreakdown(
                    paymentNumber = p.paymentNumber,
                    paymentDateMillis = p.paymentDate,
                    paymentAmountMinor = p.amountMinor,
                    paymentMethod = p.paymentMethod,
                    referenceNote = p.referenceNumber?.trim()?.takeIf { it.isNotEmpty() },
                    allocatedToInvoicesMinor = p.amountMinor,
                    unallocatedAdvanceMinor = 0L,
                    currentAccountBalanceMinor = outstanding ?: 0L,
                    currencyCode = p.currency
                )
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
                val to = resolvedRangeToMillis ?: request.to ?: nowAsOf
                require(from <= to) { "Invalid date range" }
                documentDateMillis = to
                number = "SUP-STMT-$name"
                val stmt = preloadedSupplierRangeStmt
                val breakdown = CommunicationAccountingReconciler.buildSupplierStatementBreakdown(
                    statement = stmt,
                    fromMillis = from,
                    toMillis = to,
                    fallbackBalanceMinor = outstanding ?: 0L,
                    currencyCode = brand.currencyCode
                )
                statementBreakdown = breakdown
                f["opening_balance"] = balance(breakdown.openingBalanceMinor)
                f["closing_balance"] = balance(breakdown.closingBalanceMinor)
                f["total_sales"] = money(breakdown.totalSalesOrPurchasesMinor)
                f["total_payments"] = money(breakdown.totalPaymentMinor)
                f["date_range"] = (if (from == 0L) (if (bn) "শুরু থেকে" else "Beginning") else shortDate(from)) + " — " + shortDate(to)
                columns = if (bn) listOf("তারিখ", "রেফারেন্স", "ডেবিট", "ক্রেডিট", "ব্যালেন্স") else listOf("Date", "Reference", "Debit", "Credit", "Balance")
                rows = breakdown.transactions.map { tx ->
                    listOf(
                        shortDate(tx.dateMillis),
                        tx.referenceNumber,
                        money(tx.debitMinor),
                        money(tx.creditMinor),
                        balance(tx.runningBalanceMinor)
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
            request = request.copy(documentId = resolvedDocumentId),
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
            scheduledDeliveryInfo = scheduledDeliveryInfo,
            documentDateMillis = documentDateMillis,
            asOfMillis = nowAsOf,
            brand = brand,
            party = partyInfo,
            lineItems = lineItems,
            invoiceBreakdown = invoiceBreakdown,
            paymentBreakdown = paymentBreakdown,
            reminderBreakdown = reminderBreakdown,
            statementBreakdown = statementBreakdown,
            returnExchangeBreakdown = returnExchangeBreakdown,
            deliveryBreakdown = deliveryBreakdown
        )
    }
}
