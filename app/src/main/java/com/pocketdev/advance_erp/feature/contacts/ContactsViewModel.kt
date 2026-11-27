package com.pocketdev.advance_erp.feature.contacts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.communication.CommunicationModule
import com.pocketdev.advance_erp.core.communication.CommunicationRequest
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.domain.model.PartyLedgerStatement
import com.pocketdev.advance_erp.domain.repository.AccountingRepository
import com.pocketdev.advance_erp.domain.repository.BusinessControlCenterRepository
import com.pocketdev.advance_erp.domain.repository.ContactsRepository
import com.pocketdev.advance_erp.domain.repository.CustomerDeleteResult
import com.pocketdev.advance_erp.domain.repository.CustomerLedgerEntry
import com.pocketdev.advance_erp.domain.repository.CustomerPaymentDraft
import com.pocketdev.advance_erp.domain.repository.SalesRepository
import com.pocketdev.advance_erp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class ContactsTab {
    CUSTOMERS,
    SUPPLIERS
}

data class ContactDialogState(
    val showAddDialog: Boolean = false,
    val editingCustomer: Customer? = null,
    val editingSupplier: Supplier? = null,
    val canEditSupplierCurrency: Boolean = true
)

private data class CustomerPaymentModalState(
    val targetCustomer: Customer? = null,
    val nextPaymentNumber: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null
)

private data class ContactsListBundle(
    val customers: List<Customer>,
    val suppliers: List<Supplier>,
    val totalCustomerReceivedMinor: Long,
    val totalSupplierPaidBdtMinor: Long
)

private data class ContactsPrefsBundle(
    val language: AppLanguage,
    val currency: CurrencyCode,
    val companyName: String
)

private data class ContactsControlsBundle(
    val selectedTab: ContactsTab,
    val searchQuery: String,
    val filterState: ContactsFilterState,
    val sortOption: ContactSortOption,
    val dialogState: ContactDialogState
)

private data class ContactsSalesAuxBundle(
    val allInvoices: List<SalesInvoice>,
    val liquidAccounts: List<ChartOfAccount>,
    val paymentModal: CustomerPaymentModalState
)

private data class ContactsOverlaysBundle(
    val userMessage: String?,
    val communicationSuccessRequest: CommunicationRequest?,
    val activeCustomerLedger: Pair<Customer, List<CustomerLedgerEntry>>?,
    val activeSupplierLedger: Pair<Supplier, PartyLedgerStatement>?
)

data class ContactsUiState(
    val selectedTab: ContactsTab = ContactsTab.CUSTOMERS,
    val customers: List<Customer> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val totalCustomerReceivedMinor: Long = 0L,
    val totalSupplierPaidBdtMinor: Long = 0L,
    val companyName: String = "ADVANCE ERP",
    val searchQuery: String = "",
    val filterState: ContactsFilterState = ContactsFilterState(),
    val sortOption: ContactSortOption = ContactSortOption.NAME_ASC,
    val language: AppLanguage = AppLanguage.BANGLA,
    val currency: CurrencyCode = CurrencyCode.BDT,
    val showAddDialog: Boolean = false,
    val editingCustomer: Customer? = null,
    val editingSupplier: Supplier? = null,
    val canEditSupplierCurrency: Boolean = true,
    val userMessage: String? = null,
    val communicationSuccessRequest: CommunicationRequest? = null,
    val activeCustomerLedger: Pair<Customer, List<CustomerLedgerEntry>>? = null,
    val activeSupplierLedger: Pair<Supplier, PartyLedgerStatement>? = null,
    val paymentTargetCustomer: Customer? = null,
    val allInvoices: List<SalesInvoice> = emptyList(),
    val liquidAccounts: List<ChartOfAccount> = emptyList(),
    val nextPaymentNumber: String? = null,
    val isSavingPayment: Boolean = false,
    val paymentSaveError: String? = null
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactsRepository: ContactsRepository,
    private val salesRepository: SalesRepository,
    private val settingsRepository: SettingsRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val businessControlCenterRepository: BusinessControlCenterRepository,
    private val accountingRepository: AccountingRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ContactsTab.CUSTOMERS)
    private val _searchQuery = MutableStateFlow("")
    private val _filterState = MutableStateFlow(ContactsFilterState())
    private val _sortOption = MutableStateFlow(ContactSortOption.NAME_ASC)
    private val _dialogState = MutableStateFlow(ContactDialogState())
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _communicationSuccessRequest = MutableStateFlow<CommunicationRequest?>(null)
    private val _activeCustomerLedger = MutableStateFlow<Pair<Customer, List<CustomerLedgerEntry>>?>(null)
    private val _activeSupplierLedger = MutableStateFlow<Pair<Supplier, PartyLedgerStatement>?>(null)
    private val _paymentModalState = MutableStateFlow(CustomerPaymentModalState())

    private val _listsFlow = combine(
        contactsRepository.getAllCustomers(),
        contactsRepository.getAllSuppliers(),
        contactsRepository.getTotalCustomerReceivedMinor(),
        contactsRepository.getTotalSupplierPaidBdtMinor()
    ) { customers, suppliers, custReceived, suppPaid ->
        ContactsListBundle(
            customers = customers,
            suppliers = suppliers,
            totalCustomerReceivedMinor = custReceived,
            totalSupplierPaidBdtMinor = suppPaid
        )
    }

    private val _prefsFlow = combine(
        preferencesRepository.selectedLanguage,
        preferencesRepository.selectedCurrency,
        settingsRepository.getBusinessProfile().onStart { emit(null) }
    ) { language, currency, profile ->
        ContactsPrefsBundle(
            language = language,
            currency = currency,
            companyName = profile?.companyName?.ifBlank { null } ?: "ADVANCE ERP"
        )
    }

    private val _controlsFlow = combine(
        _selectedTab,
        _searchQuery,
        _filterState,
        _sortOption,
        _dialogState
    ) { tab, query, filter, sort, dialog ->
        ContactsControlsBundle(
            selectedTab = tab,
            searchQuery = query,
            filterState = filter,
            sortOption = sort,
            dialogState = dialog
        )
    }

    private val _salesAuxFlow = combine(
        salesRepository.getAllInvoices().onStart { emit(emptyList()) },
        accountingRepository.getLiquidAccounts().onStart { emit(emptyList()) },
        _paymentModalState
    ) { invoices, accounts, payModal ->
        ContactsSalesAuxBundle(
            allInvoices = invoices,
            liquidAccounts = accounts,
            paymentModal = payModal
        )
    }

    private val _overlaysFlow = combine(
        _userMessage,
        _communicationSuccessRequest,
        _activeCustomerLedger,
        _activeSupplierLedger
    ) { msg, commReq, custLedger, suppLedger ->
        ContactsOverlaysBundle(
            userMessage = msg,
            communicationSuccessRequest = commReq,
            activeCustomerLedger = custLedger,
            activeSupplierLedger = suppLedger
        )
    }

    val uiState: StateFlow<ContactsUiState> = combine(
        _listsFlow,
        _prefsFlow,
        _controlsFlow,
        _salesAuxFlow,
        _overlaysFlow
    ) { lists, prefs, controls, salesAux, overlays ->
        ContactsUiState(
            selectedTab = controls.selectedTab,
            customers = lists.customers,
            suppliers = lists.suppliers,
            totalCustomerReceivedMinor = lists.totalCustomerReceivedMinor,
            totalSupplierPaidBdtMinor = lists.totalSupplierPaidBdtMinor,
            companyName = prefs.companyName,
            searchQuery = controls.searchQuery,
            filterState = controls.filterState,
            sortOption = controls.sortOption,
            language = prefs.language,
            currency = prefs.currency,
            showAddDialog = controls.dialogState.showAddDialog,
            editingCustomer = controls.dialogState.editingCustomer,
            editingSupplier = controls.dialogState.editingSupplier,
            canEditSupplierCurrency = controls.dialogState.canEditSupplierCurrency,
            userMessage = overlays.userMessage,
            communicationSuccessRequest = overlays.communicationSuccessRequest,
            activeCustomerLedger = overlays.activeCustomerLedger,
            activeSupplierLedger = overlays.activeSupplierLedger,
            paymentTargetCustomer = salesAux.paymentModal.targetCustomer,
            allInvoices = salesAux.allInvoices,
            liquidAccounts = salesAux.liquidAccounts,
            nextPaymentNumber = salesAux.paymentModal.nextPaymentNumber,
            isSavingPayment = salesAux.paymentModal.isSaving,
            paymentSaveError = salesAux.paymentModal.saveError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ContactsUiState()
    )

    fun onTabSelect(tab: ContactsTab) {
        if (_selectedTab.value != tab) {
            _selectedTab.value = tab
            _filterState.value = ContactsFilterState()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilterState(filter: ContactsFilterState) {
        _filterState.value = filter
    }

    fun clearFilters() {
        _filterState.value = ContactsFilterState()
    }

    fun updateSortOption(option: ContactSortOption) {
        _sortOption.value = option
    }

    fun setAddDialogVisible(visible: Boolean) {
        _dialogState.value = if (visible) ContactDialogState(showAddDialog = true) else ContactDialogState()
    }

    fun openEditCustomer(customer: Customer) {
        _dialogState.value = ContactDialogState(editingCustomer = customer)
    }

    fun openEditSupplier(supplier: Supplier) {
        viewModelScope.launch {
            val canChangeCurrency = runCatching {
                contactsRepository.canChangeSupplierCurrency(supplier.id)
            }.getOrDefault(false)
            _dialogState.value = ContactDialogState(
                editingSupplier = supplier,
                canEditSupplierCurrency = canChangeCurrency
            )
        }
    }

    fun dismissContactDialog() {
        _dialogState.value = ContactDialogState()
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun addCustomer(
        name: String,
        phone: String,
        email: String?,
        address: String,
        creditLimitMinor: Long,
        profileImagePath: String? = null,
        openingDueMinor: Long = 0L,
        openingNote: String? = null
    ) {
        viewModelScope.launch {
            try {
                val customer = Customer(
                    id = UUID.randomUUID().toString(),
                    customerCode = "CUST-${System.currentTimeMillis() % 10000}",
                    name = name.trim(),
                    phone = phone.trim(),
                    email = email?.trim()?.ifBlank { null },
                    address = address.trim(),
                    creditLimitMinor = creditLimitMinor,
                    outstandingBalanceMinor = 0L,
                    profileImagePath = profileImagePath,
                    isActive = true
                )
                contactsRepository.addCustomerWithOptionalOpeningDue(
                    customer = customer,
                    openingDueMinor = openingDueMinor,
                    openingNote = openingNote
                )
                dismissContactDialog()
                _userMessage.value = if (uiState.value.language == AppLanguage.BANGLA) {
                    if (openingDueMinor > 0L) "গ্রাহক ও পূর্বের বকেয়া সফলভাবে সংরক্ষিত হয়েছে" else "গ্রাহক সফলভাবে যোগ করা হয়েছে"
                } else {
                    if (openingDueMinor > 0L) "Customer and opening due recorded successfully" else "Customer added successfully"
                }
            } catch (e: Exception) {
                _userMessage.value = e.message ?: if (uiState.value.language == AppLanguage.BANGLA) "গ্রাহক সংরক্ষণ করা যায়নি" else "Failed to save customer"
            }
        }
    }

    fun updateCustomerDetails(
        customer: Customer,
        name: String,
        phone: String,
        email: String?,
        address: String,
        creditLimitMinor: Long,
        profileImagePath: String?,
        additionalOpeningDueMinor: Long = 0L,
        openingNote: String? = null
    ) {
        viewModelScope.launch {
            try {
                val updated = customer.copy(
                    name = name.trim(),
                    phone = phone.trim(),
                    email = email?.trim()?.ifBlank { null },
                    address = address.trim(),
                    creditLimitMinor = creditLimitMinor,
                    profileImagePath = profileImagePath
                )
                contactsRepository.updateCustomer(updated)
                if (additionalOpeningDueMinor > 0L) {
                    contactsRepository.recordCustomerOpeningDue(
                        customerId = updated.id,
                        amountMinor = additionalOpeningDueMinor,
                        note = openingNote
                    )
                }
                dismissContactDialog()
                _userMessage.value = if (uiState.value.language == AppLanguage.BANGLA) {
                    if (additionalOpeningDueMinor > 0L) "গ্রাহকের তথ্য ও পূর্বের বকেয়া আপডেট হয়েছে" else "গ্রাহকের তথ্য সফলভাবে আপডেট হয়েছে"
                } else {
                    if (additionalOpeningDueMinor > 0L) "Customer details & opening due updated" else "Customer details updated"
                }
            } catch (e: Exception) {
                _userMessage.value = e.message ?: if (uiState.value.language == AppLanguage.BANGLA) "গ্রাহকের তথ্য আপডেট করা যায়নি" else "Failed to update customer"
            }
        }
    }

    fun updateCustomerPhoto(customer: Customer, photoPath: String?) {
        viewModelScope.launch {
            contactsRepository.updateCustomer(customer.copy(profileImagePath = photoPath))
            _userMessage.value = if (uiState.value.language == AppLanguage.BANGLA) {
                if (photoPath != null) "গ্রাহকের প্রোফাইল ছবি সংরক্ষিত হয়েছে" else "ছবি সরানো হয়েছে"
            } else {
                if (photoPath != null) "Customer profile photo saved" else "Photo removed"
            }
        }
    }

    fun dismissCommunicationSuccess() {
        _communicationSuccessRequest.value = null
    }

    fun openCustomerLedger(customer: Customer) {
        viewModelScope.launch {
            try {
                val entries = salesRepository.getCustomerLedger(customer.id)
                _activeCustomerLedger.value = customer to entries
            } catch (e: Exception) {
                _userMessage.value = e.message ?: "Failed to load customer ledger"
            }
        }
    }

    fun dismissCustomerLedger() {
        _activeCustomerLedger.value = null
    }

    fun openRecordCustomerPayment(customer: Customer) {
        viewModelScope.launch {
            val nextNum = runCatching { salesRepository.previewNextCustomerPaymentNumber() }.getOrNull()
            _paymentModalState.value = CustomerPaymentModalState(
                targetCustomer = customer,
                nextPaymentNumber = nextNum
            )
        }
    }

    fun dismissRecordCustomerPayment() {
        _paymentModalState.value = CustomerPaymentModalState()
    }

    fun submitCustomerPayment(draft: CustomerPaymentDraft) {
        viewModelScope.launch {
            _paymentModalState.value = _paymentModalState.value.copy(isSaving = true, saveError = null)
            try {
                val paymentNumber = salesRepository.recordCustomerPayment(draft)
                _paymentModalState.value = CustomerPaymentModalState()
                _communicationSuccessRequest.value = CommunicationRequest(CommunicationModule.PAYMENT, paymentNumber)
                _userMessage.value = if (uiState.value.language == AppLanguage.BANGLA) {
                    "গ্রাহকের পেমেন্ট সফলভাবে সংরক্ষিত হয়েছে"
                } else {
                    "Customer payment recorded successfully"
                }
            } catch (e: Exception) {
                _paymentModalState.value = _paymentModalState.value.copy(
                    isSaving = false,
                    saveError = e.message ?: "Failed to record payment"
                )
            }
        }
    }

    fun deleteOrArchiveCustomer(customerId: String) {
        viewModelScope.launch {
            when (val result = contactsRepository.deleteOrArchiveCustomer(customerId)) {
                is CustomerDeleteResult.Blocked -> _userMessage.value = result.reason
                is CustomerDeleteResult.Archived -> _userMessage.value = result.message
                is CustomerDeleteResult.HardDeleted -> {
                    _userMessage.value = if (uiState.value.language == AppLanguage.BANGLA) {
                        "গ্রাহক স্থায়ীভাবে মুছে ফেলা হয়েছে"
                    } else {
                        "Customer deleted permanently (no financial records found)"
                    }
                }
            }
        }
    }

    fun openSupplierLedger(supplier: Supplier) {
        viewModelScope.launch {
            try {
                val stmt = businessControlCenterRepository.getSupplierLedgerStatement(
                    supplierId = supplier.id,
                    fromDate = 0L,
                    toDate = System.currentTimeMillis()
                )
                if (stmt != null) {
                    _activeSupplierLedger.value = supplier to stmt
                } else {
                    _userMessage.value = if (uiState.value.language == AppLanguage.BANGLA) {
                        "সরবরাহকারীর লেজার পাওয়া যায়নি"
                    } else {
                        "Supplier ledger statement unavailable"
                    }
                }
            } catch (e: Exception) {
                _userMessage.value = e.message ?: "Failed to load supplier ledger"
            }
        }
    }

    fun dismissSupplierLedger() {
        _activeSupplierLedger.value = null
    }

    fun deleteOrArchiveSupplier(supplierId: String) {
        viewModelScope.launch {
            when (val result = contactsRepository.deleteOrArchiveSupplier(supplierId)) {
                is CustomerDeleteResult.Blocked -> _userMessage.value = result.reason
                is CustomerDeleteResult.Archived -> _userMessage.value = result.message
                is CustomerDeleteResult.HardDeleted -> {
                    _userMessage.value = if (uiState.value.language == AppLanguage.BANGLA) {
                        "সরবরাহকারী স্থায়ীভাবে মুছে ফেলা হয়েছে"
                    } else {
                        "Supplier deleted permanently (no financial records found)"
                    }
                }
            }
        }
    }

    fun addSupplier(
        companyName: String,
        contactPerson: String?,
        phone: String,
        email: String?,
        address: String,
        country: String,
        currency: String,
        openingPayableBdtMinor: Long = 0L,
        openingNote: String? = null
    ) {
        viewModelScope.launch {
            try {
                val supplier = Supplier(
                    id = UUID.randomUUID().toString(),
                    supplierCode = "SUP-${System.currentTimeMillis() % 10000}",
                    companyName = companyName.trim(),
                    contactPerson = contactPerson?.trim()?.ifBlank { null },
                    phone = phone.trim(),
                    email = email?.trim()?.ifBlank { null },
                    address = address.trim(),
                    country = country.trim(),
                    currency = currency.trim(),
                    outstandingPayableMinor = 0L,
                    isActive = true
                )
                contactsRepository.addSupplierWithOptionalOpeningPayable(
                    supplier = supplier,
                    openingPayableBdtMinor = openingPayableBdtMinor,
                    openingNote = openingNote
                )
                dismissContactDialog()
                _userMessage.value = if (uiState.value.language == AppLanguage.BANGLA) {
                    if (openingPayableBdtMinor > 0L) "সরবরাহকারী ও পূর্বের প্রদেয় বকেয়া সংরক্ষিত হয়েছে" else "সরবরাহকারী সফলভাবে যোগ করা হয়েছে"
                } else {
                    if (openingPayableBdtMinor > 0L) "Supplier and opening payable recorded" else "Supplier added successfully"
                }
            } catch (e: Exception) {
                _userMessage.value = e.message ?: if (uiState.value.language == AppLanguage.BANGLA) "সরবরাহকারী সংরক্ষণ করা যায়নি" else "Failed to save supplier"
            }
        }
    }

    fun updateSupplierDetails(
        supplier: Supplier,
        companyName: String,
        contactPerson: String?,
        phone: String,
        email: String?,
        address: String,
        country: String,
        currency: String,
        additionalOpeningPayableBdtMinor: Long = 0L,
        openingNote: String? = null
    ) {
        viewModelScope.launch {
            try {
                val updated = supplier.copy(
                    companyName = companyName.trim(),
                    contactPerson = contactPerson?.trim()?.ifBlank { null },
                    phone = phone.trim(),
                    email = email?.trim()?.ifBlank { null },
                    address = address.trim(),
                    country = country.trim(),
                    currency = currency.trim()
                )
                contactsRepository.updateSupplier(updated)
                if (additionalOpeningPayableBdtMinor > 0L) {
                    contactsRepository.recordSupplierOpeningPayable(
                        supplierId = updated.id,
                        amountBdtMinor = additionalOpeningPayableBdtMinor,
                        note = openingNote
                    )
                }
                dismissContactDialog()
                _userMessage.value = if (uiState.value.language == AppLanguage.BANGLA) {
                    if (additionalOpeningPayableBdtMinor > 0L) "সরবরাহকারীর তথ্য ও পূর্বের বকেয়া আপডেট হয়েছে" else "সরবরাহকারীর তথ্য সফলভাবে আপডেট হয়েছে"
                } else {
                    if (additionalOpeningPayableBdtMinor > 0L) "Supplier details & opening payable updated" else "Supplier details updated"
                }
            } catch (e: Exception) {
                _userMessage.value = e.message ?: if (uiState.value.language == AppLanguage.BANGLA) "সরবরাহকারীর তথ্য আপডেট করা যায়নি" else "Failed to update supplier"
            }
        }
    }
}
