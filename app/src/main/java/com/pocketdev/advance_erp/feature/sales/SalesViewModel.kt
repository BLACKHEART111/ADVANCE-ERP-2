package com.pocketdev.advance_erp.feature.sales

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.communication.CommunicationModule
import com.pocketdev.advance_erp.core.communication.CommunicationRequest
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.core.util.InvoicePdfGenerator
import com.pocketdev.advance_erp.data.dao.BusinessProfileDao
import com.pocketdev.advance_erp.data.dao.CustomerDao
import com.pocketdev.advance_erp.data.dao.CustomerPaymentDao
import com.pocketdev.advance_erp.data.dao.ProductImageDao
import com.pocketdev.advance_erp.data.dao.ProductVariantDao
import com.pocketdev.advance_erp.data.dao.StockBalanceDao
import com.pocketdev.advance_erp.data.dao.WarehouseDao
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.data.entity.PaymentAllocation
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import com.pocketdev.advance_erp.data.entity.SalesReturn
import com.pocketdev.advance_erp.data.dao.InventoryCostLayerDao
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.StockBalance
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.repository.AccountingRepository
import com.pocketdev.advance_erp.domain.repository.ContactsRepository
import com.pocketdev.advance_erp.domain.repository.CustomerLedgerEntry
import com.pocketdev.advance_erp.domain.repository.CustomerPaymentDraft
import com.pocketdev.advance_erp.domain.repository.CustomerSummary
import com.pocketdev.advance_erp.domain.repository.CustomerWiseSalesItem
import com.pocketdev.advance_erp.domain.repository.DeliveryChallanInfo
import com.pocketdev.advance_erp.domain.repository.NewCustomerDraft
import com.pocketdev.advance_erp.domain.repository.ProductRepository
import com.pocketdev.advance_erp.domain.repository.ProductWiseSalesItem
import com.pocketdev.advance_erp.domain.repository.SalesInvoiceDraft
import com.pocketdev.advance_erp.domain.repository.SalesReportSummary
import com.pocketdev.advance_erp.domain.repository.SalesRepository
import com.pocketdev.advance_erp.domain.repository.SalesReturnDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SalesUiState(
    val communicationSuccessRequest: CommunicationRequest? = null,
    val invoices: List<SalesInvoice> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val warehouses: List<Warehouse> = emptyList(),
    val products: List<Product> = emptyList(),
    val variants: List<ProductVariant> = emptyList(),
    val stockBalances: List<StockBalance> = emptyList(),
    val costLayers: List<InventoryCostLayer> = emptyList(),
    val payments: List<CustomerPayment> = emptyList(),
    val returns: List<SalesReturn> = emptyList(),
    val liquidAccounts: List<ChartOfAccount> = emptyList(),
    val selectedCustomerSummary: CustomerSummary? = null,
    val selectedInvoice: SalesInvoice? = null,
    val selectedInvoiceItems: List<SalesInvoiceItem> = emptyList(),
    val selectedInvoiceAllocations: List<PaymentAllocation> = emptyList(),
    val selectedInvoiceChallans: List<DeliveryChallanInfo> = emptyList(),
    val selectedCustomerLedger: List<CustomerLedgerEntry> = emptyList(),
    val selectedCustomerIdForLedger: String? = null,
    val salesReport: SalesReportSummary? = null,
    val productWiseReport: List<ProductWiseSalesItem> = emptyList(),
    val customerWiseReport: List<CustomerWiseSalesItem> = emptyList(),
    val businessProfile: BusinessProfile? = null,
    val productImages: Map<String, String> = emptyMap(),
    val variantImages: Map<String, String> = emptyMap(),
    val userRole: UserRole = UserRole.OWNER_ADMIN,
    val nextPaymentNumber: String? = null,
    val language: AppLanguage = AppLanguage.BANGLA,
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

@HiltViewModel
class SalesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val salesRepository: SalesRepository,
    private val productRepository: ProductRepository,
    private val productVariantDao: ProductVariantDao,
    private val productImageDao: ProductImageDao,
    private val customerDao: CustomerDao,
    private val customerPaymentDao: CustomerPaymentDao,
    private val warehouseDao: WarehouseDao,
    private val stockBalanceDao: StockBalanceDao,
    private val inventoryCostLayerDao: InventoryCostLayerDao,
    private val businessProfileDao: BusinessProfileDao,
    private val accountingRepository: AccountingRepository,
    private val contactsRepository: ContactsRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesUiState(isLoading = true))
    val uiState: StateFlow<SalesUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        loadDefaultReport()
        refreshNextPaymentNumber()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Combine flows in groups to respect Kotlin's combine parameter limits
            val streamA = combine(
                salesRepository.getAllInvoices(),
                customerDao.getAllCustomers(),
                warehouseDao.getAllWarehouses()
            ) { invoices, customers, warehouses ->
                Triple(invoices, customers, warehouses)
            }

            val streamB = combine(
                productRepository.getAllProducts(),
                salesRepository.getAllPayments(),
                salesRepository.getAllReturns()
            ) { productsWithDetails, payments, returns ->
                val fallbackImages = mutableMapOf<String, String>()
                val products = productsWithDetails.map { pwd ->
                    pwd.primaryImageUri?.takeIf { it.isNotBlank() }?.let { fallbackImages[pwd.id] = it }
                    Product(
                        id = pwd.id,
                        categoryId = pwd.categoryId,
                        nameEn = pwd.nameEn,
                        nameBn = pwd.nameBn,
                        baseSku = pwd.baseSku,
                        barcode = pwd.barcode,
                        description = pwd.description,
                        unit = pwd.unit,
                        purchasePriceMinor = pwd.purchasePriceMinor,
                        sellingPriceMinor = pwd.sellingPriceMinor,
                        minStockAlert = pwd.minStockAlert,
                        isActive = pwd.isActive,
                        createdAt = pwd.createdAt,
                        updatedAt = pwd.createdAt
                    )
                }
                Triple(products to fallbackImages, payments, returns)
            }

            combine(
                streamA,
                streamB,
                preferencesRepository.selectedLanguage
            ) { (invoices, customers, warehouses), (productsAndImages, payments, returns), lang ->
                val (products, fallbackImages) = productsAndImages
                val currentSelectedId = _uiState.value.selectedInvoice?.id
                val updatedSelectedInvoice = if (currentSelectedId != null) {
                    invoices.find { it.id == currentSelectedId }
                } else null
                val mergedProdImages = fallbackImages + _uiState.value.productImages

                _uiState.value.copy(
                    invoices = invoices,
                    customers = customers,
                    warehouses = warehouses,
                    products = products,
                    productImages = mergedProdImages,
                    payments = payments,
                    returns = returns,
                    selectedInvoice = updatedSelectedInvoice ?: _uiState.value.selectedInvoice,
                    language = lang,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }

        viewModelScope.launch {
            productImageDao.getAllImages().collect { allImages ->
                val prodMap = mutableMapOf<String, String>()
                val varMap = mutableMapOf<String, String>()
                for (img in allImages) {
                    if (img.imageUri.isBlank()) continue
                    if (img.variantId == null) {
                        if (!prodMap.containsKey(img.productId)) prodMap[img.productId] = img.imageUri
                    } else {
                        if (!varMap.containsKey(img.variantId)) varMap[img.variantId] = img.imageUri
                    }
                }
                _uiState.update { state ->
                    val merged = state.productImages.toMutableMap().apply { putAll(prodMap) }
                    state.copy(productImages = merged, variantImages = varMap)
                }
            }
        }

        viewModelScope.launch {
            preferencesRepository.selectedUserRole.collect { role ->
                _uiState.update { it.copy(userRole = role) }
            }
        }

        viewModelScope.launch {
            productVariantDao.getAllVariants().collect { vars ->
                _uiState.update { it.copy(variants = vars) }
            }
        }

        viewModelScope.launch {
            stockBalanceDao.getAllBalances().collect { balances ->
                _uiState.update { it.copy(stockBalances = balances) }
            }
        }
        viewModelScope.launch {
            inventoryCostLayerDao.getAllCostLayers().collect { layers ->
                _uiState.update { it.copy(costLayers = layers) }
            }
        }

        viewModelScope.launch {
            businessProfileDao.getProfile().collect { profile ->
                _uiState.update { it.copy(businessProfile = profile) }
            }
        }

        viewModelScope.launch {
            accountingRepository.getLiquidAccounts().collect { accounts ->
                _uiState.update { it.copy(liquidAccounts = accounts.filter { acc -> acc.isActive }) }
            }
        }
    }

    fun loadDefaultReport() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
            val summary = salesRepository.getSalesReportSummary(thirtyDaysAgo, now)
            val productReport = salesRepository.getProductWiseSalesReport(thirtyDaysAgo, now)
            val customerReport = salesRepository.getCustomerWiseSalesReport(thirtyDaysAgo, now)

            _uiState.update {
                it.copy(
                    salesReport = summary,
                    productWiseReport = productReport,
                    customerWiseReport = customerReport
                )
            }
        }
    }

    fun selectInvoice(invoice: SalesInvoice) {
        viewModelScope.launch {
            val items = salesRepository.getInvoiceItems(invoice.id)
            val allocations = salesRepository.getAllocationsForInvoiceSync(invoice.id)
            val challans = runCatching { salesRepository.getDeliveryChallansForInvoice(invoice.id) }.getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    selectedInvoice = invoice,
                    selectedInvoiceItems = items,
                    selectedInvoiceAllocations = allocations,
                    selectedInvoiceChallans = challans
                )
            }
        }
    }

    fun clearSelectedInvoice() {
        _uiState.update {
            it.copy(
                selectedInvoice = null,
                selectedInvoiceItems = emptyList(),
                selectedInvoiceAllocations = emptyList(),
                selectedInvoiceChallans = emptyList()
            )
        }
    }

    fun loadCustomerSummary(customerId: String) {
        viewModelScope.launch {
            val summary = salesRepository.getCustomerSummary(customerId)
            _uiState.update { it.copy(selectedCustomerSummary = summary) }
        }
    }

    fun createCustomerInline(
        draft: NewCustomerDraft,
        onCreated: (Customer) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val normalizedPhone = com.pocketdev.advance_erp.core.util.Formatters
                    .toEnglishDigits(draft.phone)
                    .replace("[^0-9+]".toRegex(), "")
                    .trim()
                val existing = _uiState.value.customers.find {
                    com.pocketdev.advance_erp.core.util.Formatters.toEnglishDigits(it.phone)
                        .replace("[^0-9+]".toRegex(), "") == normalizedPhone
                } ?: customerDao.getCustomerByPhone(normalizedPhone)
                if (existing != null) {
                    val isBn = _uiState.value.language == AppLanguage.BANGLA
                    onError(
                        if (isBn) "এই মোবাইল নম্বরে ইতিমধ্যে গ্রাহক নিবন্ধিত আছে: ${existing.name}"
                        else "Customer already exists with this phone: ${existing.name}"
                    )
                    return@launch
                }

                val existingCodes = _uiState.value.customers.map { it.customerCode.uppercase() }.toSet()
                var suffix = ((System.currentTimeMillis() % 9000L) + 1000L).toInt()
                var guard = 0
                while ("CUST-$suffix" in existingCodes && guard < 9000) {
                    suffix = ((suffix - 1000 + 1) % 9000) + 1000
                    guard++
                }

                val newCustomer = Customer(
                    id = java.util.UUID.randomUUID().toString(),
                    customerCode = "CUST-$suffix",
                    name = draft.name.trim(),
                    phone = normalizedPhone,
                    email = draft.email?.trim()?.takeIf { it.isNotBlank() },
                    address = draft.address.trim(),
                    creditLimitMinor = 0L,
                    outstandingBalanceMinor = 0L,
                    profileImagePath = draft.profileImagePath,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
                contactsRepository.addCustomerWithOptionalOpeningDue(
                    customer = newCustomer,
                    openingDueMinor = draft.openingBalanceMinor
                )
                val saved = customerDao.getCustomerById(newCustomer.id)
                    ?: newCustomer.copy(outstandingBalanceMinor = draft.openingBalanceMinor)
                loadCustomerSummary(saved.id)
                onCreated(saved)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to create customer")
            }
        }
    }

    fun createInvoice(draft: SalesInvoiceDraft, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val newInvoiceId = salesRepository.createInvoice(draft)
                _uiState.update { it.copy(userMessage = "Invoice created successfully: $newInvoiceId") }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Failed to create invoice: ${e.message}") }
            }
        }
    }

    fun createAndConfirmInvoice(
        draft: SalesInvoiceDraft,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = salesRepository.createAndPostInvoice(draft)
            result.onSuccess { newInvoiceId ->
                _uiState.update {
                    it.copy(
                        userMessage = "Sale confirmed and posted: $newInvoiceId",
                        communicationSuccessRequest = CommunicationRequest(CommunicationModule.INVOICE, newInvoiceId)
                    )
                }
                loadDefaultReport()
                onSuccess()
            }.onFailure { err ->
                val message = err.message ?: "Failed to confirm sale"
                _uiState.update { it.copy(userMessage = "Failed to confirm sale: $message") }
                onError(message)
            }
        }
    }

    fun postInvoice(invoiceId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = salesRepository.postInvoice(invoiceId)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        userMessage = "Invoice posted! Stock and Accounting journals updated.",
                        communicationSuccessRequest = CommunicationRequest(CommunicationModule.INVOICE, invoiceId)
                    )
                }
                val updated = salesRepository.getInvoiceById(invoiceId)
                if (updated != null) {
                    selectInvoice(updated)
                }
                loadDefaultReport()
                onSuccess()
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Post failed: ${err.message}") }
            }
        }
    }

    fun refreshNextPaymentNumber() {
        viewModelScope.launch {
            runCatching {
                val nextNum = salesRepository.previewNextCustomerPaymentNumber()
                _uiState.update { it.copy(nextPaymentNumber = nextNum) }
            }
        }
    }

    fun recordPayment(draft: CustomerPaymentDraft, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val paymentNumber = salesRepository.recordCustomerPayment(draft)
                val savedPaymentId = runCatching { customerPaymentDao.getPaymentByNumber(paymentNumber)?.id }.getOrNull()
                val isBn = _uiState.value.language == AppLanguage.BANGLA
                val nextNum = runCatching { salesRepository.previewNextCustomerPaymentNumber() }.getOrNull()

                _uiState.update {
                    it.copy(
                        userMessage = if (isBn) "পেমেন্ট রশিদ সফলভাবে সংরক্ষিত হয়েছে: #$paymentNumber"
                                      else "Payment receipt recorded successfully: #$paymentNumber",
                        nextPaymentNumber = nextNum,
                        communicationSuccessRequest = CommunicationRequest(
                            CommunicationModule.PAYMENT,
                            savedPaymentId ?: paymentNumber
                        )
                    )
                }
                if (_uiState.value.selectedInvoice != null) {
                    val updated = salesRepository.getInvoiceById(_uiState.value.selectedInvoice!!.id)
                    if (updated != null) selectInvoice(updated)
                }
                loadDefaultReport()
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Payment failed: ${e.message}") }
            }
        }
    }

    fun processReturn(draft: SalesReturnDraft, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val returnId = salesRepository.processSalesReturn(draft)
                _uiState.update {
                    it.copy(
                        userMessage = "Sales return processed and stock restored: $returnId",
                        communicationSuccessRequest = CommunicationRequest(CommunicationModule.RETURN, returnId)
                    )
                }
                if (_uiState.value.selectedInvoice != null) {
                    val updated = salesRepository.getInvoiceById(_uiState.value.selectedInvoice!!.id)
                    if (updated != null) selectInvoice(updated)
                }
                loadDefaultReport()
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Return failed: ${e.message}") }
            }
        }
    }

    fun loadCustomerLedger(customerId: String) {
        viewModelScope.launch {
            val ledger = salesRepository.getCustomerLedger(customerId)
            _uiState.update {
                it.copy(
                    selectedCustomerIdForLedger = customerId,
                    selectedCustomerLedger = ledger
                )
            }
        }
    }

    fun dismissCommunicationSuccess() { _uiState.update { it.copy(communicationSuccessRequest = null) } }

    fun updateInvoiceCommunicationMetadata(
        invoiceId: String,
        validUntilDate: Long?,
        scheduledDeliveryInfo: String?
    ) {
        viewModelScope.launch {
            val isBn = _uiState.value.language == AppLanguage.BANGLA
            runCatching {
                salesRepository.updateDocumentCommunicationMetadata(invoiceId, validUntilDate, scheduledDeliveryInfo)
            }.onSuccess {
                val updated = salesRepository.getInvoiceById(invoiceId)
                if (updated != null && _uiState.value.selectedInvoice?.id == invoiceId) {
                    selectInvoice(updated)
                }
                _uiState.update {
                    it.copy(
                        userMessage = if (isBn) "নথির মেয়াদ ও ডেলিভারি তথ্য সংরক্ষিত হয়েছে"
                        else "Document validity & delivery details saved"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        userMessage = if (isBn) "সংরক্ষণ ব্যর্থ: ${err.message}" else "Save failed: ${err.message}"
                    )
                }
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun dispatchDelivery(draft: com.pocketdev.advance_erp.domain.repository.DeliveryDispatchDraft, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = salesRepository.dispatchDelivery(draft)
            result.onSuccess { challanNo ->
                _uiState.update {
                    it.copy(
                        userMessage = "Delivery challan dispatched: $challanNo",
                        communicationSuccessRequest = CommunicationRequest(
                            CommunicationModule.DELIVERY,
                            "${draft.invoiceId}|$challanNo"
                        )
                    )
                }
                if (_uiState.value.selectedInvoice != null) {
                    val updated = salesRepository.getInvoiceById(_uiState.value.selectedInvoice!!.id)
                    if (updated != null) selectInvoice(updated)
                }
                onSuccess(challanNo)
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Delivery failed: ${err.message}") }
            }
        }
    }

    fun processDirectExchange(draft: com.pocketdev.advance_erp.domain.repository.DirectExchangeDraft, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = salesRepository.processDirectExchange(draft)
            result.onSuccess { exchNo ->
                _uiState.update {
                    it.copy(
                        userMessage = "Direct exchange completed successfully: $exchNo",
                        communicationSuccessRequest = CommunicationRequest(CommunicationModule.EXCHANGE, exchNo)
                    )
                }
                if (_uiState.value.selectedInvoice != null) {
                    val updated = salesRepository.getInvoiceById(_uiState.value.selectedInvoice!!.id)
                    if (updated != null) selectInvoice(updated)
                }
                loadDefaultReport()
                onSuccess(exchNo)
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Direct exchange failed: ${err.message}") }
            }
        }
    }

    fun convertQuotationToSalesOrder(quotationId: String, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = salesRepository.convertQuotationToSalesOrder(quotationId)
            result.onSuccess { orderId ->
                _uiState.update { it.copy(userMessage = "Quotation converted to Sales Order: $orderId") }
                val updated = salesRepository.getInvoiceById(quotationId)
                if (updated != null) selectInvoice(updated)
                onSuccess(orderId)
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Conversion failed: ${err.message}") }
            }
        }
    }

    fun convertSalesOrderToInvoice(salesOrderId: String, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = salesRepository.convertSalesOrderToInvoice(salesOrderId)
            result.onSuccess { invId ->
                _uiState.update {
                    it.copy(
                        userMessage = "Sales Order converted to Commercial Invoice: $invId",
                        communicationSuccessRequest = CommunicationRequest(CommunicationModule.INVOICE, invId)
                    )
                }
                val updated = salesRepository.getInvoiceById(salesOrderId)
                if (updated != null) selectInvoice(updated)
                onSuccess(invId)
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Conversion failed: ${err.message}") }
            }
        }
    }

    suspend fun getDeliveryChallansForInvoice(invoiceId: String) = salesRepository.getDeliveryChallansForInvoice(invoiceId)

    suspend fun getRemainingUndeliveredItems(invoiceId: String) = salesRepository.getRemainingUndeliveredItems(invoiceId)

    suspend fun generateThermalReceiptPdf(
        context: Context,
        invoice: SalesInvoice,
        cashTenderedMinor: Long = invoice.paidAmountMinor,
        changeReturnedMinor: Long = 0L,
        is80mm: Boolean = true
    ): File = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val items = salesRepository.getInvoiceItems(invoice.id)
        InvoicePdfGenerator.generateThermalReceiptPdf(
            context = context,
            invoice = invoice,
            items = items,
            businessProfile = _uiState.value.businessProfile,
            cashTenderedMinor = cashTenderedMinor,
            changeReturnedMinor = changeReturnedMinor,
            isBangla = _uiState.value.language == AppLanguage.BANGLA,
            is80mm = is80mm
        )
    }

    fun voidCustomerPayment(paymentId: String, reason: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = salesRepository.voidCustomerPayment(paymentId, reason)
            if (result.isSuccess) {
                _uiState.update { it.copy(userMessage = "Payment receipt voided successfully") }
                loadDefaultReport()
                onSuccess()
            } else {
                _uiState.update { it.copy(userMessage = "Failed to void payment: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun cancelOrDeleteInvoice(invoiceId: String, reason: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = salesRepository.cancelOrDeleteInvoice(invoiceId, reason)
            if (result.isSuccess) {
                _uiState.update { it.copy(userMessage = "Invoice cancelled/deleted successfully", selectedInvoice = null) }
                loadDefaultReport()
                onSuccess()
            } else {
                _uiState.update { it.copy(userMessage = "Failed to cancel invoice: ${result.exceptionOrNull()?.message}") }
            }
        }
    }
}
