package com.pocketdev.advance_erp.feature.contacts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.pdf.CentralizedPdfEngine
import com.pocketdev.advance_erp.core.pdf.CentralizedPdfReportRegistry
import com.pocketdev.advance_erp.core.util.DueReminderStatementData
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.domain.repository.ContactsRepository
import com.pocketdev.advance_erp.domain.repository.CustomerDeleteResult
import com.pocketdev.advance_erp.domain.repository.SalesRepository
import com.pocketdev.advance_erp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
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

data class ContactsUiState(
    val selectedTab: ContactsTab = ContactsTab.CUSTOMERS,
    val customers: List<Customer> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val language: AppLanguage = AppLanguage.BANGLA,
    val currency: CurrencyCode = CurrencyCode.BDT,
    val showAddDialog: Boolean = false,
    val editingCustomer: Customer? = null,
    val editingSupplier: Supplier? = null,
    val canEditSupplierCurrency: Boolean = true,
    val userMessage: String? = null,
    val activeDueReminderData: DueReminderStatementData? = null
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactsRepository: ContactsRepository,
    private val salesRepository: SalesRepository,
    private val settingsRepository: SettingsRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ContactsTab.CUSTOMERS)
    private val _dialogState = MutableStateFlow(ContactDialogState())
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _activeDueReminderData = MutableStateFlow<DueReminderStatementData?>(null)

    val uiState: StateFlow<ContactsUiState> = combine(
        _selectedTab,
        combine(contactsRepository.getAllCustomers(), contactsRepository.getAllSuppliers()) { c, s -> Pair(c, s) },
        combine(preferencesRepository.selectedLanguage, preferencesRepository.selectedCurrency) { l, c -> Pair(l, c) },
        _dialogState,
        _userMessage,
        _activeDueReminderData
    ) { args: Array<Any?> ->
        val tab = args[0] as ContactsTab
        val (customers, suppliers) = args[1] as Pair<List<Customer>, List<Supplier>>
        val (language, currency) = args[2] as Pair<AppLanguage, CurrencyCode>
        val dialog = args[3] as ContactDialogState
        val msg = args[4] as? String
        val dueReminder = args[5] as? DueReminderStatementData
        ContactsUiState(
            selectedTab = tab,
            customers = customers,
            suppliers = suppliers,
            language = language,
            currency = currency,
            showAddDialog = dialog.showAddDialog,
            editingCustomer = dialog.editingCustomer,
            editingSupplier = dialog.editingSupplier,
            canEditSupplierCurrency = dialog.canEditSupplierCurrency,
            userMessage = msg,
            activeDueReminderData = dueReminder
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ContactsUiState()
    )

    fun onTabSelect(tab: ContactsTab) {
        _selectedTab.value = tab
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
            _userMessage.value = if (photoPath != null) "গ্রাহকের প্রোফাইল ছবি সংরক্ষিত হয়েছে" else "ছবি সরানো হয়েছে"
        }
    }

    fun openDueReminder(customer: Customer) {
        viewModelScope.launch {
            try {
                val ledger = salesRepository.getCustomerLedger(customer.id)
                val isBn = uiState.value.language == AppLanguage.BANGLA
                val profile = settingsRepository.getBusinessProfile().firstOrNull()
                val pdfReport = CentralizedPdfReportRegistry.buildCustomerLedgerReport(
                    customer = customer,
                    entries = ledger,
                    isBangla = isBn
                )
                val pdfFile = CentralizedPdfEngine.generatePdf(context, pdfReport, profile)
                val currentDue = customer.outstandingBalanceMinor
                val invoiceCount = ledger.count { it.transactionType == "INVOICE" }

                val reminderData = DueReminderStatementData(
                    customerId = customer.id,
                    customerName = customer.name,
                    customerPhone = customer.phone,
                    customerCode = customer.customerCode,
                    companyName = profile?.companyName ?: "ADVANCE ERP",
                    companyPhone = profile?.phone ?: "",
                    totalDueMinor = currentDue,
                    invoiceCount = invoiceCount,
                    asOfDate = System.currentTimeMillis(),
                    paymentInstructions = if (isBn) "বিকাশ / নগদ / ব্যাংক অ্যাকাউন্টের মাধ্যমে পরিশোধ করা যাবে।" else "Payments accepted via Bank Transfer, bKash or Cash.",
                    pdfFile = pdfFile
                )
                _activeDueReminderData.value = reminderData
            } catch (e: Exception) {
                _userMessage.value = "বকেয়া তাগাদা প্রস্তুত করতে ব্যর্থ: ${e.message}"
            }
        }
    }

    fun dismissDueReminderDialog() {
        _activeDueReminderData.value = null
    }

    fun deleteOrArchiveCustomer(customerId: String) {
        viewModelScope.launch {
            when (val result = contactsRepository.deleteOrArchiveCustomer(customerId)) {
                is CustomerDeleteResult.Blocked -> {
                    _userMessage.value = result.reason
                }
                is CustomerDeleteResult.Archived -> {
                    _userMessage.value = result.message
                }
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
