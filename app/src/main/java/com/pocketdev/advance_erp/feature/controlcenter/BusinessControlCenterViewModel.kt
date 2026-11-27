package com.pocketdev.advance_erp.feature.controlcenter

import com.pocketdev.advance_erp.domain.model.LedgerReport
import com.pocketdev.advance_erp.domain.repository.AccountingRepository
import com.pocketdev.advance_erp.domain.repository.FinancialRepository
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import com.pocketdev.advance_erp.domain.repository.SalesRepository
import com.pocketdev.advance_erp.domain.repository.ImportRepository
import com.pocketdev.advance_erp.domain.repository.CustomerPaymentDraft
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.PaymentMethod
import com.pocketdev.advance_erp.data.entity.Product
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.CancellationException
import java.math.BigDecimal
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.security.SecurityManager
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.core.util.ControlCenterExcelGenerator
import com.pocketdev.advance_erp.core.util.ControlCenterPdfGenerator
import com.pocketdev.advance_erp.core.util.ControlCenterShareService
import com.pocketdev.advance_erp.core.util.CustomReportPdfConfig
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.model.BusinessSummaryDashboardData
import com.pocketdev.advance_erp.domain.model.ControlCenterFilterState
import com.pocketdev.advance_erp.domain.model.DateRangeOption
import com.pocketdev.advance_erp.domain.model.ReportCategory
import com.pocketdev.advance_erp.domain.model.SavedFilterPreset
import com.pocketdev.advance_erp.domain.model.TransactionDetailRecord
import com.pocketdev.advance_erp.domain.model.UnifiedTransactionItem
import com.pocketdev.advance_erp.domain.repository.BusinessControlCenterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class BusinessControlCenterViewModel @Inject constructor(
    private val repository: BusinessControlCenterRepository,
    private val salesRepository: SalesRepository,
    private val importRepository: ImportRepository,
    private val financialRepository: FinancialRepository,
    private val accountingRepository: AccountingRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(ControlCenterFilterState())
    val filterState: StateFlow<ControlCenterFilterState> = _filterState.asStateFlow()

    private val refresh = MutableStateFlow(0)
    val report: StateFlow<LedgerReport> = kotlinx.coroutines.flow.combine(_filterState.debounce(180), refresh) { filter, _ -> filter }
        .flatMapLatest { filter -> repository.getLedgerReport(filter)
            .onStart { emit(LedgerReport(filter = filter, isLoading = true)) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(LedgerReport(filter = filter, error = error.message ?: "Ledger loading failed"))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LedgerReport(isLoading = true))
    val transactions = report.map { it.transactions }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val summaryDashboard = report.map { it.summary }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessSummaryDashboardData())
    val paymentInvoices = salesRepository.getAllInvoices().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val paymentOrders = importRepository.getAllPurchaseOrders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _paymentSaving = MutableStateFlow(false)
    val paymentSaving = _paymentSaving.asStateFlow()
    private val _actionError = MutableStateFlow<String?>(null)
    val actionError = _actionError.asStateFlow()
    fun dismissActionError() { _actionError.value = null }
    private var detailJob: kotlinx.coroutines.Job? = null
    private var detailRequest = 0L
    private val _detailLoading = MutableStateFlow(false)
    val detailLoading = _detailLoading.asStateFlow()
    private val _paymentError = MutableStateFlow<String?>(null)
    val paymentError = _paymentError.asStateFlow()

    fun retry() { refresh.value++ }
    fun clearPaymentError() { _paymentError.value = null }
    fun selectParty(id: String?, supplier: Boolean) {
        updateFilter(_filterState.value.copy(category = if (supplier) ReportCategory.SUPPLIER_LEDGER else ReportCategory.CUSTOMER_LEDGER,
            selectedCustomerId = if (supplier) null else id, selectedSupplierId = if (supplier) id else null))
    }
    private fun submitPayment(action: suspend () -> Unit, onSuccess: () -> Unit) {
        if (_paymentSaving.value) return
        _paymentSaving.value = true
        _paymentError.value = null
        viewModelScope.launch {
            try { action(); retry(); onSuccess() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { _paymentError.value = error.message ?: "Payment could not be saved" }
            finally { _paymentSaving.value = false }
        }
    }
    fun collectPayment(draft: CustomerPaymentDraft, onSuccess: () -> Unit) =
        submitPayment({
            salesRepository.recordCustomerPayment(draft)
            refreshNextPaymentNumbers()
        }, onSuccess)

    fun paySupplier(
        supplierId: String,
        orderId: String?,
        amount: Long,
        currency: String,
        rate: BigDecimal,
        method: PaymentMethod,
        account: String,
        reference: String?,
        notes: String?,
        paymentDate: Long = System.currentTimeMillis(),
        paymentAccountId: String? = null,
        onSuccess: () -> Unit
    ) = submitPayment({
        importRepository.recordSupplierPayment(
            supplierId = supplierId,
            orderId = orderId,
            amountMinor = amount,
            currency = currency,
            exchangeRate = rate,
            method = method,
            paymentAccount = account,
            reference = reference,
            notes = notes,
            paymentDate = paymentDate,
            paymentAccountId = paymentAccountId
        )
        refreshNextPaymentNumbers()
    }, onSuccess)

    private fun readyReport(includeSensitive: Boolean = false): LedgerReport {
        val snapshot = report.value
        check(!snapshot.isLoading && snapshot.error == null && snapshot.filter == _filterState.value) { "Wait for the current ledger to finish loading" }
        check(snapshot.filter.financialMetric !in setOf(com.pocketdev.advance_erp.domain.model.FinancialMetricFilter.PROFIT,com.pocketdev.advance_erp.domain.model.FinancialMetricFilter.LOSS) ||
            (includeSensitive && SecurityManager.canViewProfit(userRole.value))) { "Authenticate profit access before using this filtered report" }
        return snapshot
    }

    val customers: StateFlow<List<Customer>> = repository.getAllCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<Supplier>> = repository.getAllSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val warehouses: StateFlow<List<Warehouse>> = repository.getAllWarehouses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<ProductCategory>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<ChartOfAccount>> = accountingRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liquidAccounts: StateFlow<List<ChartOfAccount>> = accountingRepository.getLiquidAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedPresets: StateFlow<List<SavedFilterPreset>> = repository.getSavedPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _nextCustomerPaymentNumber = MutableStateFlow<String?>(null)
    val nextCustomerPaymentNumber: StateFlow<String?> = _nextCustomerPaymentNumber.asStateFlow()

    private val _nextSupplierPaymentNumber = MutableStateFlow<String?>(null)
    val nextSupplierPaymentNumber: StateFlow<String?> = _nextSupplierPaymentNumber.asStateFlow()

    private val _previewPdfFile = MutableStateFlow<Pair<File, String>?>(null)
    val previewPdfFile: StateFlow<Pair<File, String>?> = _previewPdfFile.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun clearPreviewPdf() { _previewPdfFile.value = null }
    fun clearActionMessage() { _actionMessage.value = null }

    fun refreshNextPaymentNumbers() {
        viewModelScope.launch {
            _nextCustomerPaymentNumber.value = runCatching { salesRepository.previewNextCustomerPaymentNumber() }.getOrNull()
            val supplierCount = paymentOrders.value.let { _ ->
                runCatching {
                    val year = java.text.SimpleDateFormat("yyyy", java.util.Locale.US).format(java.util.Date())
                    "SP-$year-AUTO"
                }.getOrNull()
            }
            _nextSupplierPaymentNumber.value = supplierCount
        }
    }

    fun recordExpense(
        category: String,
        descEn: String,
        descBn: String,
        amountMinor: Long,
        currency: String,
        exchangeRateToBdt: String,
        paymentAccountId: String,
        expenseAccountId: String,
        voucherDate: Long,
        referenceNumber: String?,
        notes: String?,
        onSuccess: () -> Unit
    ) {
        if (!SecurityManager.canRecordExpense(userRole.value)) {
            _actionError.value = if (appLanguage.value == AppLanguage.BANGLA) "ব্যয় এন্ট্রি করার অনুমতি নেই।" else "Permission denied to record expense."
            return
        }
        submitPayment({
            financialRepository.recordExpense(
                category = category,
                descEn = descEn,
                descBn = descBn,
                amountMinor = amountMinor,
                currency = currency,
                exchangeRateToBdt = exchangeRateToBdt,
                paymentAccountId = paymentAccountId,
                expenseAccountId = expenseAccountId,
                voucherDate = voucherDate,
                referenceNumber = referenceNumber,
                notes = notes
            )
            _actionMessage.value = if (appLanguage.value == AppLanguage.BANGLA) "ব্যয় ভাউচার সফলভাবে সংরক্ষিত হয়েছে" else "Expense voucher posted successfully"
        }, onSuccess)
    }

    fun postManualJournalEntry(
        entryDate: Long,
        descEn: String,
        descBn: String,
        lines: List<JournalLineDraft>,
        onSuccess: () -> Unit
    ) {
        if (!SecurityManager.canPostJournal(userRole.value)) {
            _actionError.value = if (appLanguage.value == AppLanguage.BANGLA) "জার্নাল এন্ট্রি করার অনুমতি নেই।" else "Permission denied to post journal entry."
            return
        }
        submitPayment({
            require(lines.size >= 2) { "At least one debit and one credit line are required" }
            val debitTotal = lines.filter { it.isDebit }.sumOf { it.amountMinor }
            val creditTotal = lines.filter { !it.isDebit }.sumOf { it.amountMinor }
            require(debitTotal > 0L && debitTotal == creditTotal) { "Debit and Credit totals must be positive and balanced" }
            val entryId = accountingRepository.postJournalEntry(
                entryDate = entryDate,
                descEn = descEn,
                descBn = descBn,
                referenceType = "MANUAL_LEDGER",
                referenceId = null,
                lines = lines
            )
            financialRepository.logAudit(
                recordType = "JOURNAL_ENTRY",
                recordId = entryId,
                action = "MANUAL_POST",
                details = "Manual journal posted from Central Ledger: $descEn (Dr/Cr BDT ${debitTotal / 100.0})"
            )
            _actionMessage.value = if (appLanguage.value == AppLanguage.BANGLA) "জার্নাল এন্ট্রি সফলভাবে পোস্ট হয়েছে" else "Journal entry posted successfully"
        }, onSuccess)
    }

    fun previewReportPdf(context: Context, includeSensitive: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val snapshot = readyReport(includeSensitive)
                val canView = includeSensitive && SecurityManager.canViewProfit(userRole.value)
                val title = getReportTitle(snapshot.filter)
                val file = if (snapshot.statement != null) {
                    com.pocketdev.advance_erp.core.util.LedgerStatementExporter.pdf(
                        context, snapshot, businessProfile.value, appLanguage.value == AppLanguage.BANGLA
                    )
                } else {
                    ControlCenterPdfGenerator.generateReportPdf(
                        context = context,
                        title = title,
                        filterState = snapshot.filter,
                        summary = snapshot.summary,
                        transactions = safeExportRows(snapshot, includeSensitive),
                        businessProfile = businessProfile.value,
                        canViewProfit = canView,
                        isBangla = appLanguage.value == AppLanguage.BANGLA
                    )
                }
                _previewPdfFile.value = file to title
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "PDF Preview failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    val userRole: StateFlow<UserRole> = preferencesRepository.selectedUserRole
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserRole.SALES_OPERATOR)

    val appLanguage: StateFlow<AppLanguage> = preferencesRepository.selectedLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.BANGLA)

    val currencyCode: StateFlow<CurrencyCode> = preferencesRepository.selectedCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrencyCode.BDT)

    val businessProfile: StateFlow<BusinessProfile?> = repository.getBusinessProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedTransactionDetail = MutableStateFlow<TransactionDetailRecord?>(null)
    val selectedTransactionDetail: StateFlow<TransactionDetailRecord?> = _selectedTransactionDetail.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun updateFilter(requested: ControlCenterFilterState) {
        val filter = when {
            requested.selectedSupplierId != null && requested.category == ReportCategory.CUSTOMER_LEDGER -> requested.copy(category=ReportCategory.SUPPLIER_LEDGER)
            requested.selectedCustomerId != null && requested.category == ReportCategory.SUPPLIER_LEDGER && requested.selectedSupplierId == null -> requested.copy(category=ReportCategory.CUSTOMER_LEDGER)
            else -> requested
        }
        _filterState.value = filter.copy(
            selectedCustomerId = if (filter.category == ReportCategory.SUPPLIER_LEDGER) null else filter.selectedCustomerId,
            selectedSupplierId = when { filter.category == ReportCategory.SUPPLIER_LEDGER -> filter.selectedSupplierId; filter.category == ReportCategory.CUSTOMER_LEDGER || filter.selectedCustomerId != null -> null; else -> filter.selectedSupplierId },
            financialMetric = if (filter.financialMetric in setOf(com.pocketdev.advance_erp.domain.model.FinancialMetricFilter.OPENING_BALANCE, com.pocketdev.advance_erp.domain.model.FinancialMetricFilter.CLOSING_BALANCE)) com.pocketdev.advance_erp.domain.model.FinancialMetricFilter.ALL else filter.financialMetric)
    }

    fun selectCategory(category: ReportCategory) {
        updateFilter(_filterState.value.copy(category = category,
            selectedCustomerId = if(category==ReportCategory.SUPPLIER_LEDGER) null else _filterState.value.selectedCustomerId,
            selectedSupplierId = if(category==ReportCategory.CUSTOMER_LEDGER) null else _filterState.value.selectedSupplierId))
    }

    fun selectDateRange(option: DateRangeOption, customStart: Long? = null, customEnd: Long? = null) {
        _filterState.value = _filterState.value.copy(
            dateRangeOption = option,
            customStartDate = customStart,
            customEndDate = customEnd
        )
    }

    fun setCustomStartDate(startMillis: Long) {
        val currentEnd = _filterState.value.customEndDate ?: System.currentTimeMillis()
        val adjustedEnd = if (startMillis > currentEnd) startMillis else currentEnd
        _filterState.value = _filterState.value.copy(
            dateRangeOption = DateRangeOption.CUSTOM,
            customStartDate = startMillis,
            customEndDate = adjustedEnd
        )
    }

    fun setCustomEndDate(endMillis: Long) {
        val currentStart = _filterState.value.customStartDate ?: (endMillis - (7L * 24 * 60 * 60 * 1000L))
        val adjustedStart = if (endMillis < currentStart) endMillis else currentStart
        _filterState.value = _filterState.value.copy(
            dateRangeOption = DateRangeOption.CUSTOM,
            customStartDate = adjustedStart,
            customEndDate = endMillis
        )
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        val s = minOf(startMillis, endMillis)
        val e = maxOf(startMillis, endMillis)
        _filterState.value = _filterState.value.copy(
            dateRangeOption = DateRangeOption.CUSTOM,
            customStartDate = s,
            customEndDate = e
        )
    }

    fun setSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun resetFilters() {
        _filterState.value = ControlCenterFilterState(category = _filterState.value.category)
    }

    fun savePreset(name: String) {
        viewModelScope.launch {
            repository.savePreset(name, _filterState.value)
        }
    }

    fun applyPreset(preset: SavedFilterPreset) {
        updateFilter(preset.filterState)
    }

    fun deletePreset(presetId: String) {
        viewModelScope.launch {
            repository.deletePreset(presetId)
        }
    }

    fun openTransactionDetail(rawType: String, id: String) {
        detailJob?.cancel()
        val request = ++detailRequest
        _selectedTransactionDetail.value = null
        _detailLoading.value = true
        _actionError.value = null
        detailJob = viewModelScope.launch {
            try {
                val row = report.value.transactions.firstOrNull { it.rawType == rawType && it.sourceId == id }
                val record = repository.getTransactionDetails(rawType.removeSuffix("_REVERSAL"), id)
                if (request != detailRequest) return@launch
                _selectedTransactionDetail.value = if (record != null && row != null) record.copy(transaction = row,
                    auditTrailInfo = listOfNotNull(record.auditTrailInfo, row.notes).joinToString("\n"))
                    else record ?: row?.let { TransactionDetailRecord(it, auditTrailInfo = it.notes) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { _actionError.value = error.message ?: "Could not load transaction" }
            finally { if(request==detailRequest) _detailLoading.value=false }
        }
    }

    fun dismissTransactionDetail() {
        detailRequest++
        detailJob?.cancel()
        _detailLoading.value=false
        _selectedTransactionDetail.value = null
    }

    private fun getReportTitle(filter: ControlCenterFilterState = _filterState.value): String {
        val cat = filter.category
        val isBn = appLanguage.value == AppLanguage.BANGLA
        return if (cat == ReportCategory.ALL_TRANSACTIONS) {
            if (isBn) "ব্যবসা নিয়ন্ত্রণ কেন্দ্র - সম্পূর্ণ বিবরণী" else "Business Control Center - Master Ledger"
        } else {
            if (isBn) "${cat.titleBn} প্রতিবেদন" else "${cat.titleEn} Report"
        }
    }

    private fun safeExportRows(snapshot: LedgerReport, includeSensitive: Boolean) : List<UnifiedTransactionItem> {
        check(snapshot.filter.financialMetric !in setOf(com.pocketdev.advance_erp.domain.model.FinancialMetricFilter.PROFIT, com.pocketdev.advance_erp.domain.model.FinancialMetricFilter.LOSS) || (includeSensitive && SecurityManager.canViewProfit(userRole.value))) { "Authenticate profit access before exporting this filter" }
        return snapshot.transactions.map { row ->
        if (row.rawType in setOf("STOCK_MOVEMENT", "STOCK_TRANSFER") && !(includeSensitive && SecurityManager.canViewCost(userRole.value))) row.copy(amountMinor = 0, profitMinor = null, cogsMinor = 0, secondaryAmountMinor = null,
            particulars = row.particulars + " [cost hidden]") else row.copy(profitMinor = null, cogsMinor = 0)
        }
    }

    fun exportAndSharePdf(context: Context, includeSensitive: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val snapshot = readyReport(includeSensitive)
                val canView = includeSensitive && SecurityManager.canViewProfit(userRole.value)
                val file = if (snapshot.statement != null) com.pocketdev.advance_erp.core.util.LedgerStatementExporter.pdf(context, snapshot, businessProfile.value, appLanguage.value == AppLanguage.BANGLA)
                else ControlCenterPdfGenerator.generateReportPdf(
                    context = context,
                    title = getReportTitle(snapshot.filter),
                    filterState = snapshot.filter,
                    summary = snapshot.summary,
                    transactions = safeExportRows(snapshot, includeSensitive),
                    businessProfile = businessProfile.value,
                    canViewProfit = canView,
                    isBangla = appLanguage.value == AppLanguage.BANGLA
                )
                launch(Dispatchers.Main) {
                    ControlCenterShareService.shareFile(context, file, "application/pdf", "Share PDF Report via")
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "PDF Generation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportAndShareExcel(context: Context, includeSensitive: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val snapshot = readyReport(includeSensitive)
                val canView = includeSensitive && SecurityManager.canViewProfit(userRole.value)
                val file = ControlCenterExcelGenerator.exportToXlsx(
                    context = context,
                    reportTitle = getReportTitle(snapshot.filter),
                    filterState = snapshot.filter,
                    summary = snapshot.summary,
                    transactions = safeExportRows(snapshot, includeSensitive),
                    customers = customers.value.filter { snapshot.statement == null || (snapshot.statement.partyType == "CUSTOMER" && it.id == snapshot.statement.partyId) },
                    suppliers = suppliers.value.filter { snapshot.statement == null || (snapshot.statement.partyType == "SUPPLIER" && it.id == snapshot.statement.partyId) },
                    businessProfile = businessProfile.value,
                    canViewProfit = canView,
                    statement = snapshot.statement
                )
                launch(Dispatchers.Main) {
                    ControlCenterShareService.shareFile(
                        context,
                        file,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "Share Excel (XLSX) Workbook via"
                    )
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Excel Generation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportAndShareCsv(context: Context, includeSensitive: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val snapshot = readyReport(includeSensitive)
                val file = ControlCenterExcelGenerator.exportToCsv(
                    context = context,
                    reportTitle = getReportTitle(snapshot.filter),
                    transactions = safeExportRows(snapshot, includeSensitive)
                )
                launch(Dispatchers.Main) {
                    ControlCenterShareService.shareFile(context, file, "text/csv", "Share CSV Report via")
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "CSV Generation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun printReport(context: Context, includeSensitive: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val snapshot = readyReport(includeSensitive)
                val canView = includeSensitive && SecurityManager.canViewProfit(userRole.value)
                val file = if (snapshot.statement != null) com.pocketdev.advance_erp.core.util.LedgerStatementExporter.pdf(context, snapshot, businessProfile.value, appLanguage.value == AppLanguage.BANGLA)
                else ControlCenterPdfGenerator.generateReportPdf(
                    context = context,
                    title = getReportTitle(snapshot.filter),
                    filterState = snapshot.filter,
                    summary = snapshot.summary,
                    transactions = safeExportRows(snapshot, includeSensitive),
                    businessProfile = businessProfile.value,
                    canViewProfit = canView,
                    isBangla = appLanguage.value == AppLanguage.BANGLA
                )
                launch(Dispatchers.Main) {
                    ControlCenterShareService.printPdf(context, file, "Advance_ERP_${System.currentTimeMillis()}")
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun savePdfToStorage(context: Context, includeSensitive: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val snapshot = readyReport(includeSensitive)
                val canView = includeSensitive && SecurityManager.canViewProfit(userRole.value)
                val file = if (snapshot.statement != null) com.pocketdev.advance_erp.core.util.LedgerStatementExporter.pdf(context, snapshot, businessProfile.value, appLanguage.value == AppLanguage.BANGLA)
                else ControlCenterPdfGenerator.generateReportPdf(
                    context = context,
                    title = getReportTitle(snapshot.filter),
                    filterState = snapshot.filter,
                    summary = snapshot.summary,
                    transactions = safeExportRows(snapshot, includeSensitive),
                    businessProfile = businessProfile.value,
                    canViewProfit = canView,
                    isBangla = appLanguage.value == AppLanguage.BANGLA
                )
                launch(Dispatchers.Main) {
                    ControlCenterShareService.saveToDownloads(context, file)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Save error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun saveExcelToStorage(context: Context, includeSensitive: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val snapshot = readyReport(includeSensitive)
                val canView = includeSensitive && SecurityManager.canViewProfit(userRole.value)
                val file = ControlCenterExcelGenerator.exportToXlsx(
                    context = context,
                    reportTitle = getReportTitle(snapshot.filter),
                    filterState = snapshot.filter,
                    summary = snapshot.summary,
                    transactions = safeExportRows(snapshot, includeSensitive),
                    customers = customers.value.filter { snapshot.statement == null || (snapshot.statement.partyType == "CUSTOMER" && it.id == snapshot.statement.partyId) },
                    suppliers = suppliers.value.filter { snapshot.statement == null || (snapshot.statement.partyType == "SUPPLIER" && it.id == snapshot.statement.partyId) },
                    businessProfile = businessProfile.value,
                    canViewProfit = canView,
                    statement = snapshot.statement
                )
                launch(Dispatchers.Main) {
                    ControlCenterShareService.saveToDownloads(context, file)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Save error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun previewCustomReportPdf(context: Context, config: CustomReportPdfConfig, includeSensitive: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val snapshot = readyReport(includeSensitive)
                val canView = includeSensitive && SecurityManager.canViewProfit(userRole.value) && config.includeExpensesAndProfit
                val isBn = appLanguage.value == AppLanguage.BANGLA
                val file = ControlCenterPdfGenerator.generateCustomModularReportPdf(
                    context = context,
                    config = config,
                    filterState = snapshot.filter,
                    summary = snapshot.summary,
                    transactions = safeExportRows(snapshot, includeSensitive),
                    customers = customers.value,
                    suppliers = suppliers.value,
                    products = products.value,
                    businessProfile = businessProfile.value,
                    canViewProfit = canView,
                    isBangla = isBn
                )
                _previewPdfFile.value = file to config.title.ifBlank {
                    if (isBn) "কাস্টম বিজনেস রিপোর্ট" else "Custom Modular Report"
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Custom PDF failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun saveCustomReportPdf(context: Context, config: CustomReportPdfConfig, includeSensitive: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val snapshot = readyReport(includeSensitive)
                val canView = includeSensitive && SecurityManager.canViewProfit(userRole.value) && config.includeExpensesAndProfit
                val file = ControlCenterPdfGenerator.generateCustomModularReportPdf(
                    context = context,
                    config = config,
                    filterState = snapshot.filter,
                    summary = snapshot.summary,
                    transactions = safeExportRows(snapshot, includeSensitive),
                    customers = customers.value,
                    suppliers = suppliers.value,
                    products = products.value,
                    businessProfile = businessProfile.value,
                    canViewProfit = canView,
                    isBangla = appLanguage.value == AppLanguage.BANGLA
                )
                launch(Dispatchers.Main) {
                    ControlCenterShareService.saveToDownloads(context, file)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Save Custom PDF failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun shareCustomReportPdf(context: Context, config: CustomReportPdfConfig, includeSensitive: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val snapshot = readyReport(includeSensitive)
                val canView = includeSensitive && SecurityManager.canViewProfit(userRole.value) && config.includeExpensesAndProfit
                val file = ControlCenterPdfGenerator.generateCustomModularReportPdf(
                    context = context,
                    config = config,
                    filterState = snapshot.filter,
                    summary = snapshot.summary,
                    transactions = safeExportRows(snapshot, includeSensitive),
                    customers = customers.value,
                    suppliers = suppliers.value,
                    products = products.value,
                    businessProfile = businessProfile.value,
                    canViewProfit = canView,
                    isBangla = appLanguage.value == AppLanguage.BANGLA
                )
                launch(Dispatchers.Main) {
                    ControlCenterShareService.shareFile(context, file, "application/pdf", "Share Custom Report PDF")
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Share Custom PDF failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }
}
