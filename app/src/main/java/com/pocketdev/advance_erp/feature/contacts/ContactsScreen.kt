package com.pocketdev.advance_erp.feature.contacts

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.communication.CommunicationModule
import com.pocketdev.advance_erp.core.communication.CommunicationRequest
import com.pocketdev.advance_erp.core.designsystem.ERPActionButton
import com.pocketdev.advance_erp.core.designsystem.ERPButtonVariant
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.ERPEmptyState
import com.pocketdev.advance_erp.core.designsystem.LocalDashboardTokens
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.SuggestTextField
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.core.util.AppMediaStorage
import com.pocketdev.advance_erp.core.util.ContactPickerHelper
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.SuggestionType
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.feature.communication.CommunicationDialog
import com.pocketdev.advance_erp.feature.sales.CustomerLedgerDialog
import com.pocketdev.advance_erp.feature.sales.RecordCustomerPaymentDialog
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    initialTab: ContactsTab = ContactsTab.CUSTOMERS,
    modifier: Modifier = Modifier,
    companyName: String = "ADVANCE ERP",
    onOpenDrawer: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    onSwitchTabRoute: ((ContactsTab) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tokens = LocalDashboardTokens.current
    val isBn = state.language == AppLanguage.BANGLA

    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var supplierToDelete by remember { mutableStateOf<Supplier?>(null) }
    var customerForPhotoUpdate by remember { mutableStateOf<Customer?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showDueAlertsSheet by remember { mutableStateOf(false) }
    var communicationRequest by remember { mutableStateOf<CommunicationRequest?>(null) }
    val activeCommunicationRequest = communicationRequest ?: state.communicationSuccessRequest
    val isSuccessCommunication = communicationRequest == null && state.communicationSuccessRequest != null

    activeCommunicationRequest?.let { req ->
        CommunicationDialog(
            initialRequest = req,
            success = isSuccessCommunication,
            onDismiss = {
                communicationRequest = null
                if (isSuccessCommunication) viewModel.dismissCommunicationSuccess()
            }
        )
    }

    val customerPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { pickedUri ->
            customerForPhotoUpdate?.let { targetCust ->
                coroutineScope.launch {
                    try {
                        val savedPath = AppMediaStorage.saveCustomerPhoto(context, pickedUri, targetCust.id)
                        viewModel.updateCustomerPhoto(targetCust, savedPath)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    LaunchedEffect(initialTab) {
        viewModel.onTabSelect(initialTab)
    }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val displayedCompany = state.companyName.takeIf { it.isNotBlank() && it != "ADVANCE ERP" } ?: companyName

    val customerMetrics = remember(state.customers, state.totalCustomerReceivedMinor) {
        ContactsUiLogic.computeCustomerSummaryMetrics(
            customers = state.customers,
            totalReceivedMinor = state.totalCustomerReceivedMinor
        )
    }
    val supplierMetrics = remember(state.suppliers, state.totalSupplierPaidBdtMinor) {
        ContactsUiLogic.computeSupplierSummaryMetrics(
            suppliers = state.suppliers,
            totalPaidBdtMinor = state.totalSupplierPaidBdtMinor
        )
    }

    val filteredCustomers = remember(state.customers, state.searchQuery, state.filterState, state.sortOption) {
        ContactsUiLogic.filterAndSortCustomers(
            customers = state.customers,
            query = state.searchQuery,
            filterState = state.filterState,
            sortOption = state.sortOption
        )
    }
    val filteredSuppliers = remember(state.suppliers, state.searchQuery, state.filterState, state.sortOption) {
        ContactsUiLogic.filterAndSortSuppliers(
            suppliers = state.suppliers,
            query = state.searchQuery,
            filterState = state.filterState,
            sortOption = state.sortOption
        )
    }

    val availableLocations = remember(state.selectedTab, state.customers, state.suppliers) {
        if (state.selectedTab == ContactsTab.CUSTOMERS) {
            ContactsUiLogic.extractCustomerLocations(state.customers)
        } else {
            ContactsUiLogic.extractSupplierLocations(state.suppliers)
        }
    }

    val activeMetrics = if (state.selectedTab == ContactsTab.CUSTOMERS) customerMetrics else supplierMetrics
    val (pageBgTop, pageBgBottom) = contactsPageBgColors(tokens)

    val dialPhone: (String) -> Unit = { rawPhone ->
        val clean = rawPhone.trim()
        if (clean.isNotBlank() && !clean.equals("N/A", ignoreCase = true)) {
            runCatching {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
                context.startActivity(intent)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = pageBgTop,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ContactsAddFab(
                contentDescription = if (state.selectedTab == ContactsTab.CUSTOMERS) {
                    if (isBn) "নতুন গ্রাহক যোগ করুন" else "Add Customer"
                } else {
                    if (isBn) "নতুন সরবরাহকারী যোগ করুন" else "Add Supplier"
                },
                onClick = { viewModel.setAddDialogVisible(true) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(pageBgTop, pageBgBottom)))
                .padding(padding)
        ) {
            // Integrated Purple Gradient Hero Header with curved light overlap cap
            Box(modifier = Modifier.fillMaxWidth()) {
                ContactsHeroHeader(
                    companyName = displayedCompany,
                    selectedTab = state.selectedTab,
                    language = state.language,
                    dueAlertCount = activeMetrics.duePartyCount,
                    onOpenDrawer = onOpenDrawer,
                    onToggleLanguage = onToggleLanguage,
                    onDueAlertsClick = { showDueAlertsSheet = true }
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                        .background(pageBgTop)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 0.dp, bottom = 148.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "summary_kpis") {
                    ContactsSummaryKpiRow(
                        metrics = activeMetrics,
                        selectedTab = state.selectedTab,
                        currency = state.currency,
                        language = state.language,
                        activeBalanceFilter = state.filterState.balanceFilter,
                        activeRegFilter = state.filterState.registrationFilter,
                        onTotalCardClick = { viewModel.clearFilters() },
                        onNewMonthCardClick = {
                            val nextReg = if (state.filterState.registrationFilter == ContactRegistrationFilter.THIS_MONTH) {
                                ContactRegistrationFilter.ALL
                            } else {
                                ContactRegistrationFilter.THIS_MONTH
                            }
                            viewModel.updateFilterState(state.filterState.copy(registrationFilter = nextReg))
                        },
                        onDueCardClick = {
                            val nextBal = if (state.filterState.balanceFilter == ContactBalanceFilter.WITH_DUE) {
                                ContactBalanceFilter.ALL
                            } else {
                                ContactBalanceFilter.WITH_DUE
                            }
                            viewModel.updateFilterState(state.filterState.copy(balanceFilter = nextBal))
                        },
                        onSettledCardClick = {
                            val nextBal = if (state.filterState.balanceFilter == ContactBalanceFilter.SETTLED) {
                                ContactBalanceFilter.ALL
                            } else {
                                ContactBalanceFilter.SETTLED
                            }
                            viewModel.updateFilterState(state.filterState.copy(balanceFilter = nextBal))
                        }
                    )
                }

                item(key = "segmented_tabs") {
                    ContactsSegmentedTabBar(
                        selectedTab = state.selectedTab,
                        language = state.language,
                        onSelectTab = { tab ->
                            viewModel.onTabSelect(tab)
                            onSwitchTabRoute?.invoke(tab)
                        }
                    )
                }

                item(key = "search_filter_sort") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ContactsSearchFilterSortRow(
                            query = state.searchQuery,
                            activeFilterCount = state.filterState.activeCount,
                            isSortActive = state.sortOption != ContactSortOption.NAME_ASC,
                            language = state.language,
                            onQueryChange = viewModel::updateSearchQuery,
                            onOpenFilter = { showFilterSheet = true },
                            onOpenSort = { showSortSheet = true }
                        )
                        ActiveContactsFilterChipsRow(
                            filterState = state.filterState,
                            sortOption = state.sortOption,
                            filteredCount = if (state.selectedTab == ContactsTab.CUSTOMERS) filteredCustomers.size else filteredSuppliers.size,
                            totalCount = if (state.selectedTab == ContactsTab.CUSTOMERS) state.customers.size else state.suppliers.size,
                            language = state.language,
                            onRemoveBalance = {
                                viewModel.updateFilterState(state.filterState.copy(balanceFilter = ContactBalanceFilter.ALL))
                            },
                            onRemoveLocation = {
                                viewModel.updateFilterState(state.filterState.copy(selectedLocation = null))
                            },
                            onRemoveRegistration = {
                                viewModel.updateFilterState(state.filterState.copy(registrationFilter = ContactRegistrationFilter.ALL))
                            },
                            onResetSort = { viewModel.updateSortOption(ContactSortOption.NAME_ASC) },
                            onClearAll = {
                                viewModel.clearFilters()
                                viewModel.updateSortOption(ContactSortOption.NAME_ASC)
                            }
                        )
                    }
                }

                item(key = "smart_communication_launcher") {
                    com.pocketdev.advance_erp.feature.communication.CommunicationLauncher(
                        module = if (state.selectedTab == ContactsTab.CUSTOMERS) {
                            com.pocketdev.advance_erp.core.communication.CommunicationModule.LEDGER
                        } else {
                            com.pocketdev.advance_erp.core.communication.CommunicationModule.SUPPLIER_STATEMENT
                        },
                        isBangla = isBn
                    )
                }

                if (state.selectedTab == ContactsTab.CUSTOMERS) {
                    if (state.customers.isEmpty()) {
                        item(key = "empty_customers") {
                            ERPEmptyState(
                                icon = Icons.Outlined.Group,
                                title = stringResource(R.string.no_contacts),
                                message = if (isBn) "এখনও কোনো গ্রাহক নিবন্ধিত হয়নি।" else "No customers registered yet.",
                                actionText = stringResource(R.string.add_customer),
                                onActionClick = { viewModel.setAddDialogVisible(true) }
                            )
                        }
                    } else if (filteredCustomers.isEmpty()) {
                        item(key = "no_match_customers") {
                            ERPEmptyState(
                                icon = Icons.Outlined.Search,
                                title = if (isBn) "কোনো গ্রাহক পাওয়া যায়নি" else "No matching customers",
                                message = if (isBn) "অনুসন্ধান বা ফিল্টার পরিবর্তন করে আবার চেষ্টা করুন।" else "Try clearing search or adjusting your filters.",
                                actionText = if (isBn) "ফিল্টার রিসেট করুন" else "Clear Filters",
                                onActionClick = {
                                    viewModel.updateSearchQuery("")
                                    viewModel.clearFilters()
                                }
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = filteredCustomers,
                            key = { _, c -> c.id }
                        ) { idx, c ->
                            CustomerContactCard(
                                customer = c,
                                index = idx,
                                currency = state.currency,
                                language = state.language,
                                onCardClick = { viewModel.openCustomerLedger(c) },
                                onAvatarClick = {
                                    customerForPhotoUpdate = c
                                    customerPhotoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onDialPhone = dialPhone,
                                onDueReminderClick = {
                                    communicationRequest = CommunicationRequest(CommunicationModule.REMINDER, c.id)
                                },
                                onRecordPaymentClick = { viewModel.openRecordCustomerPayment(c) },
                                onEditClick = { viewModel.openEditCustomer(c) },
                                onDeleteClick = { customerToDelete = c }
                            )
                        }
                    }
                } else {
                    if (state.suppliers.isEmpty()) {
                        item(key = "empty_suppliers") {
                            ERPEmptyState(
                                icon = Icons.Outlined.Storefront,
                                title = stringResource(R.string.no_contacts),
                                message = if (isBn) "এখনও কোনো সরবরাহকারী যোগ করা হয়নি।" else "No suppliers recorded yet.",
                                actionText = stringResource(R.string.add_supplier),
                                onActionClick = { viewModel.setAddDialogVisible(true) }
                            )
                        }
                    } else if (filteredSuppliers.isEmpty()) {
                        item(key = "no_match_suppliers") {
                            ERPEmptyState(
                                icon = Icons.Outlined.Search,
                                title = if (isBn) "কোনো সরবরাহকারী পাওয়া যায়নি" else "No matching suppliers",
                                message = if (isBn) "অনুসন্ধান বা ফিল্টার পরিবর্তন করে আবার চেষ্টা করুন।" else "Try clearing search or adjusting your filters.",
                                actionText = if (isBn) "ফিল্টার রিসেট করুন" else "Clear Filters",
                                onActionClick = {
                                    viewModel.updateSearchQuery("")
                                    viewModel.clearFilters()
                                }
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = filteredSuppliers,
                            key = { _, s -> s.id }
                        ) { idx, s ->
                            SupplierContactCard(
                                supplier = s,
                                index = idx,
                                currency = state.currency,
                                language = state.language,
                                onCardClick = { viewModel.openSupplierLedger(s) },
                                onDialPhone = dialPhone,
                                onEditClick = { viewModel.openEditSupplier(s) },
                                onDeleteClick = { supplierToDelete = s },
                                onShareStatementClick = {
                                    communicationRequest = CommunicationRequest(CommunicationModule.SUPPLIER_STATEMENT, s.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ContactsFilterBottomSheet(
            currentFilter = state.filterState,
            availableLocations = availableLocations,
            selectedTab = state.selectedTab,
            language = state.language,
            onDismiss = { showFilterSheet = false },
            onApply = viewModel::updateFilterState
        )
    }

    if (showSortSheet) {
        ContactsSortBottomSheet(
            currentSort = state.sortOption,
            language = state.language,
            onDismiss = { showSortSheet = false },
            onSelectSort = viewModel::updateSortOption
        )
    }

    if (showDueAlertsSheet) {
        ContactsDueAlertsSheet(
            selectedTab = state.selectedTab,
            dueCustomers = state.customers.filter { it.outstandingBalanceMinor > 0L }
                .sortedByDescending { it.outstandingBalanceMinor },
            dueSuppliers = state.suppliers.filter { it.outstandingPayableMinor > 0L }
                .sortedByDescending { it.outstandingPayableMinor },
            currency = state.currency,
            language = state.language,
            onDismiss = { showDueAlertsSheet = false },
            onRemindCustomer = { cust ->
                communicationRequest = CommunicationRequest(CommunicationModule.REMINDER, cust.id)
            },
            onOpenSupplierLedger = viewModel::openSupplierLedger
        )
    }

    val isDialogVisible = state.showAddDialog || state.editingCustomer != null || state.editingSupplier != null
    if (isDialogVisible) {
        val isCustomerForm = when {
            state.editingCustomer != null -> true
            state.editingSupplier != null -> false
            else -> state.selectedTab == ContactsTab.CUSTOMERS
        }
        ContactFormDialog(
            isCustomer = isCustomerForm,
            editingCustomer = state.editingCustomer,
            editingSupplier = state.editingSupplier,
            canEditSupplierCurrency = state.canEditSupplierCurrency,
            isBangla = isBn,
            onDismiss = { viewModel.dismissContactDialog() },
            onSaveCustomer = { name, phone, email, address, limit, photoPath, openingDueMinor, openingNote ->
                val editing = state.editingCustomer
                if (editing != null) {
                    viewModel.updateCustomerDetails(
                        customer = editing,
                        name = name,
                        phone = phone,
                        email = email,
                        address = address,
                        creditLimitMinor = limit,
                        profileImagePath = photoPath,
                        additionalOpeningDueMinor = openingDueMinor,
                        openingNote = openingNote
                    )
                } else {
                    viewModel.addCustomer(
                        name = name,
                        phone = phone,
                        email = email,
                        address = address,
                        creditLimitMinor = limit,
                        profileImagePath = photoPath,
                        openingDueMinor = openingDueMinor,
                        openingNote = openingNote
                    )
                }
            },
            onSaveSupplier = { name, contact, phone, email, address, country, curr, openingPayableBdtMinor, openingNote ->
                val editing = state.editingSupplier
                if (editing != null) {
                    viewModel.updateSupplierDetails(
                        supplier = editing,
                        companyName = name,
                        contactPerson = contact,
                        phone = phone,
                        email = email,
                        address = address,
                        country = country,
                        currency = curr,
                        additionalOpeningPayableBdtMinor = openingPayableBdtMinor,
                        openingNote = openingNote
                    )
                } else {
                    viewModel.addSupplier(
                        companyName = name,
                        contactPerson = contact,
                        phone = phone,
                        email = email,
                        address = address,
                        country = country,
                        currency = curr,
                        openingPayableBdtMinor = openingPayableBdtMinor,
                        openingNote = openingNote
                    )
                }
            }
        )
    }

    state.activeCustomerLedger?.let { (cust, entries) ->
        CustomerLedgerDialog(
            customer = cust,
            ledger = entries,
            isBangla = isBn,
            onDismiss = viewModel::dismissCustomerLedger,
            onShareWhatsAppStatement = { c ->
                viewModel.dismissCustomerLedger()
                communicationRequest = CommunicationRequest(CommunicationModule.STATEMENT, c.id)
            },
            onSendDueReminder = { c ->
                viewModel.dismissCustomerLedger()
                communicationRequest = CommunicationRequest(CommunicationModule.REMINDER, c.id)
            }
        )
    }

    state.activeSupplierLedger?.let { (sup, stmt) ->
        SupplierLedgerDialog(
            supplier = sup,
            statement = stmt,
            currency = state.currency,
            language = state.language,
            onDismiss = viewModel::dismissSupplierLedger,
            onShareWhatsApp = {
                viewModel.dismissSupplierLedger()
                communicationRequest = CommunicationRequest(CommunicationModule.SUPPLIER_STATEMENT, sup.id)
            }
        )
    }

    state.paymentTargetCustomer?.let { targetCust ->
        RecordCustomerPaymentDialog(
            customers = state.customers,
            invoices = state.allInvoices,
            liquidAccounts = state.liquidAccounts,
            preselectedCustomerId = targetCust.id,
            nextPaymentNumber = state.nextPaymentNumber,
            isBangla = isBn,
            isSaving = state.isSavingPayment,
            saveError = state.paymentSaveError,
            onDismiss = viewModel::dismissRecordCustomerPayment,
            onSubmit = viewModel::submitCustomerPayment
        )
    }

    if (customerToDelete != null) {
        val cust = customerToDelete!!
        val isBlocked = cust.outstandingBalanceMinor != 0L
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = {
                Text(
                    text = if (isBn) "গ্রাহক মুছুন / আর্কাইভ" else "Delete / Archive Customer",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                if (isBlocked) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isBn)
                                "⚠️ বকেয়া রয়েছে: '${cust.name}' এর নিকট ${Formatters.formatMoney(cust.outstandingBalanceMinor, state.currency, state.language)} বকেয়া রয়েছে।"
                            else
                                "⚠️ Outstanding Balance: '${cust.name}' has an outstanding balance of ${Formatters.formatMoney(cust.outstandingBalanceMinor, state.currency, state.language)}.",
                            color = StatusError,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBn)
                                "বকেয়া সম্পূর্ণরূপে পরিশোধ বা সমন্বয় না হওয়া পর্যন্ত গ্রাহক মুছে ফেলা বা আর্কাইভ করা যাবে না।"
                            else
                                "Customers with non-zero outstanding balances cannot be deleted or archived. Settle all dues first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                } else {
                    Text(
                        text = if (isBn)
                            "আপনি কি '${cust.name}' কে মুছে ফেলতে চান? যদি গ্রাহকের কোনো বিগত সেলস চালান বা পেমেন্ট রেকর্ড থাকে, তবে অডিট হিস্ট্রি সুরক্ষায় গ্রাহকটি নিরাপদে আর্কাইভ করা হবে।"
                        else
                            "Are you sure you want to delete '${cust.name}'? If the customer has past sales invoices or payments, they will be safely archived to preserve financial audit history.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                if (!isBlocked) {
                    TextButton(
                        onClick = {
                            val cId = cust.id
                            customerToDelete = null
                            viewModel.deleteOrArchiveCustomer(cId)
                        }
                    ) {
                        Text(
                            text = if (isBn) "হ্যাঁ, মুছুন" else "Delete / Archive",
                            color = StatusError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    TextButton(onClick = { customerToDelete = null }) {
                        Text(if (isBn) "বুঝেছি" else "Understood", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isBlocked) {
                    TextButton(onClick = { customerToDelete = null }) {
                        Text(if (isBn) "বাতিল" else "Cancel", color = TextSecondary)
                    }
                }
            }
        )
    }

    if (supplierToDelete != null) {
        val sup = supplierToDelete!!
        val isBlocked = sup.outstandingPayableMinor != 0L
        AlertDialog(
            onDismissRequest = { supplierToDelete = null },
            title = {
                Text(
                    text = if (isBn) "সরবরাহকারী মুছুন / আর্কাইভ" else "Delete / Archive Supplier",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                if (isBlocked) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isBn)
                                "⚠️ প্রদেয় বকেয়া রয়েছে: '${sup.companyName}' এর নিকট ${Formatters.formatMoney(sup.outstandingPayableMinor, state.currency, state.language)} বকেয়া রয়েছে।"
                            else
                                "⚠️ Outstanding Payable: '${sup.companyName}' has an outstanding payable of ${Formatters.formatMoney(sup.outstandingPayableMinor, state.currency, state.language)}.",
                            color = StatusError,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBn)
                                "প্রদেয় বকেয়া শূন্য না হওয়া পর্যন্ত সরবরাহকারী মুছে ফেলা বা আর্কাইভ করা যাবে না।"
                            else
                                "Suppliers with non-zero payable balances cannot be deleted or archived. Settle all payables first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                } else {
                    Text(
                        text = if (isBn)
                            "আপনি কি '${sup.companyName}' কে মুছে ফেলতে চান? যদি পূর্বে ক্রয় আদেশ বা পেমেন্ট রেকর্ড থাকে, তবে অডিট হিস্ট্রির জন্য নিরাপদে আর্কাইভ করা হবে।"
                        else
                            "Are you sure you want to delete '${sup.companyName}'? If purchase orders or payments exist, the supplier will be safely archived to preserve audit history.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                if (!isBlocked) {
                    TextButton(
                        onClick = {
                            val sId = sup.id
                            supplierToDelete = null
                            viewModel.deleteOrArchiveSupplier(sId)
                        }
                    ) {
                        Text(
                            text = if (isBn) "হ্যাঁ, মুছুন" else "Delete / Archive",
                            color = StatusError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    TextButton(onClick = { supplierToDelete = null }) {
                        Text(if (isBn) "বুঝেছি" else "Understood", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isBlocked) {
                    TextButton(onClick = { supplierToDelete = null }) {
                        Text(if (isBn) "বাতিল" else "Cancel", color = TextSecondary)
                    }
                }
            }
        )
    }
}

@Composable
private fun ContactFormDialog(
    isCustomer: Boolean,
    editingCustomer: Customer? = null,
    editingSupplier: Supplier? = null,
    canEditSupplierCurrency: Boolean = true,
    isBangla: Boolean = false,
    onDismiss: () -> Unit,
    onSaveCustomer: (
        name: String,
        phone: String,
        email: String?,
        address: String,
        creditLimitMinor: Long,
        profileImagePath: String?,
        openingDueMinor: Long,
        openingNote: String?
    ) -> Unit,
    onSaveSupplier: (
        companyName: String,
        contactPerson: String?,
        phone: String,
        email: String?,
        address: String,
        country: String,
        currency: String,
        openingPayableBdtMinor: Long,
        openingNote: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val formKey = editingCustomer?.id ?: editingSupplier?.id ?: "new-$isCustomer"
    val isEditing = editingCustomer != null || editingSupplier != null

    val initialLimitText = remember(formKey) {
        val minor = editingCustomer?.creditLimitMinor ?: 0L
        if (minor > 0L) java.math.BigDecimal.valueOf(minor, 2).stripTrailingZeros().toPlainString() else ""
    }

    var name by rememberSaveable(formKey) {
        mutableStateOf(editingCustomer?.name ?: editingSupplier?.companyName ?: "")
    }
    var contactPerson by rememberSaveable(formKey) {
        mutableStateOf(editingSupplier?.contactPerson.orEmpty())
    }
    var phone by rememberSaveable(formKey) {
        mutableStateOf(editingCustomer?.phone ?: editingSupplier?.phone ?: "")
    }
    var email by rememberSaveable(formKey) {
        mutableStateOf(editingCustomer?.email ?: editingSupplier?.email ?: "")
    }
    var address by rememberSaveable(formKey) {
        mutableStateOf(editingCustomer?.address ?: editingSupplier?.address ?: "")
    }
    var creditLimitStr by rememberSaveable(formKey) {
        mutableStateOf(initialLimitText)
    }
    var country by rememberSaveable(formKey) {
        mutableStateOf(editingSupplier?.country ?: "China")
    }
    var currency by rememberSaveable(formKey) {
        mutableStateOf(editingSupplier?.currency ?: "CNY")
    }
    var openingBalanceStr by rememberSaveable(formKey) { mutableStateOf("") }
    var openingNote by rememberSaveable(formKey) { mutableStateOf("") }
    var profileImageUri by remember(formKey) { mutableStateOf<android.net.Uri?>(null) }
    var validationError by remember(formKey) { mutableStateOf<String?>(null) }
    val existingPhotoPath = editingCustomer?.profileImagePath?.takeIf { it.isNotBlank() && File(it).exists() }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val (pickedName, pickedPhone) = ContactPickerHelper.extractContactInfo(context, uri)
                if (!pickedName.isNullOrBlank()) name = pickedName
                if (!pickedPhone.isNullOrBlank()) phone = pickedPhone
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        profileImageUri = uri
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 760.dp)
                .clip(RoundedCornerShape(20.dp))
                .navigationBarsPadding()
                .imePadding(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isCustomer && editingCustomer != null ->
                                if (isBangla) "গ্রাহক তথ্য সম্পাদনা" else "Edit Customer"
                            isCustomer ->
                                if (isBangla) "নতুন গ্রাহক নিবন্ধন" else stringResource(R.string.add_customer)
                            editingSupplier != null ->
                                if (isBangla) "সরবরাহকারী তথ্য সম্পাদনা" else "Edit Supplier"
                            else ->
                                if (isBangla) "নতুন সরবরাহকারী যোগ" else stringResource(R.string.add_supplier)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = ERPDivider, modifier = Modifier.padding(vertical = 12.dp))

                if (isCustomer) {
                    // Profile Photo & Phone Contact Import Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryPurple.copy(alpha = 0.12f))
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    profileImageUri != null -> {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(profileImageUri)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Customer Photo",
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                    existingPhotoPath != null -> {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(File(existingPhotoPath))
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Customer Photo",
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            Icons.Outlined.CameraAlt,
                                            contentDescription = "Add Photo",
                                            tint = PrimaryPurple,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (profileImageUri != null || existingPhotoPath != null) {
                                        if (isBangla) "ছবি নির্বাচিত" else "Photo Selected"
                                    } else {
                                        if (isBangla) "প্রোফাইল ছবি" else "Add Profile Photo"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isBangla) "ট্যাপ করে ছবি বাছুন" else "Tap avatar to select",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Phone Contacts Picker Button
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                contactPickerLauncher.launch(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Outlined.ContactPhone, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "কন্টাক্ট থেকে আনুন" else "Pick from Contacts",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = StatusError,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                SuggestTextField(
                    fieldType = if (isCustomer) SuggestionType.CUSTOMER_NAME else SuggestionType.SUPPLIER_NAME,
                    value = name,
                    onValueChange = { name = it; validationError = null },
                    label = { Text(if (isCustomer) (if (isBangla) "গ্রাহকের নাম *" else stringResource(R.string.contact_name)) else (if (isBangla) "প্রতিষ্ঠানের নাম *" else stringResource(R.string.company_name))) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (!isCustomer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SuggestTextField(
                        fieldType = SuggestionType.CONTACT_PERSON,
                        value = contactPerson,
                        onValueChange = { contactPerson = it },
                        label = { Text(if (isBangla) "যোগাযোগকারী ব্যক্তির নাম" else "Contact Person") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; validationError = null },
                    label = { Text(if (isBangla) "মোবাইল নম্বর *" else stringResource(R.string.contact_phone)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = validationError != null && phone.isBlank(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(if (isBangla) "ইমেইল (ঐচ্ছিক)" else "Email (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.contact_address)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (!isCustomer) {
                    val currencyLocked = editingSupplier != null && !canEditSupplierCurrency
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isBangla) "সরবরাহকারীর দেশ ও মুদ্রা" else "Supplier Country & Currency",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val presets = listOf(
                        Triple(if (isBangla) "বাংলাদেশ (BDT)" else "Bangladesh (BDT)", "Bangladesh", "BDT"),
                        Triple(if (isBangla) "চীন (CNY)" else "China (CNY)", "China", "CNY"),
                        Triple(if (isBangla) "অন্যান্য (USD)" else "Other (USD)", "International", "USD")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (label, presetCountry, presetCurrency) ->
                            val isSelected = currency.equals(presetCurrency, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isSelected -> PrimaryPurple
                                    currencyLocked -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(1.dp, if (isSelected) PrimaryPurple else ERPDivider),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = !currencyLocked) {
                                        country = presetCountry
                                        currency = presetCurrency
                                    }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        currencyLocked -> TextMuted
                                        else -> TextPrimary
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    if (currencyLocked) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBangla) {
                                "এই সরবরাহকারীর নামে ইতিমধ্যে ক্রয় আদেশ (PO) বা পেমেন্ট থাকায় মুদ্রা (${editingSupplier?.currency}) লক করা আছে।"
                            } else {
                                "Currency (${editingSupplier?.currency}) is locked because purchase orders or payments exist for this supplier."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text(if (isBangla) "দেশ *" else "Country *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (isCustomer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = creditLimitStr,
                        onValueChange = { creditLimitStr = it; validationError = null },
                        label = { Text(stringResource(R.string.credit_limit)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryPurple.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isEditing) {
                            val currentBalance = if (isCustomer) {
                                editingCustomer?.outstandingBalanceMinor ?: 0L
                            } else {
                                editingSupplier?.outstandingPayableMinor ?: 0L
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isCustomer) {
                                        if (isBangla) "বর্তমান মোট বকেয়া:" else "Current Total Due:"
                                    } else {
                                        if (isBangla) "বর্তমান প্রদেয় বকেয়া:" else "Current Payable Due:"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = Formatters.formatMoney(
                                        currentBalance,
                                        CurrencyCode.BDT,
                                        if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentBalance > 0L) StatusError else StatusSuccess
                                )
                            }
                        }

                        OutlinedTextField(
                            value = openingBalanceStr,
                            onValueChange = { openingBalanceStr = it; validationError = null },
                            label = {
                                Text(
                                    when {
                                        isCustomer && isEditing ->
                                            if (isBangla) "পূর্বের বকেয়া যোগ করুন (BDT ৳) (ঐচ্ছিক)" else "Add Previous / Opening Due (BDT ৳) (Optional)"
                                        isCustomer ->
                                            if (isBangla) "পূর্বের বকেয়া / Opening Due (BDT ৳) (ঐচ্ছিক)" else "Opening / Previous Due (BDT ৳) (Optional)"
                                        isEditing ->
                                            if (isBangla) "পূর্বের প্রদেয় বকেয়া যোগ করুন (BDT ৳) (ঐচ্ছিক)" else "Add Previous Payable Due (BDT ৳) (Optional)"
                                        else ->
                                            if (isBangla) "পূর্বের প্রদেয় বকেয়া / Opening Payable (BDT ৳) (ঐচ্ছিক)" else "Opening Payable Due (BDT ৳) (Optional)"
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )

                        if (openingBalanceStr.isNotBlank()) {
                            OutlinedTextField(
                                value = openingNote,
                                onValueChange = { openingNote = it },
                                label = {
                                    Text(
                                        if (isBangla) "পূর্বের বকেয়ার নোট / রেফারেন্স (ঐচ্ছিক)" else "Opening balance note / ref (Optional)"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        Text(
                            text = if (isCustomer) {
                                if (isBangla) {
                                    "আগের বকেয়া দিলে স্বয়ংক্রিয়ভাবে ডাবল-এন্ট্রি জাবেদা (Dr 1040 প্রাপ্য হিসাব / Cr 3010 মূলধন) পোস্ট হবে—স্টক বা বিক্রয়ে কোনো প্রভাব পড়বে না।"
                                } else {
                                    "Posts a balanced opening entry (Dr 1040 Accounts Receivable / Cr 3010 Equity) without affecting inventory or sales."
                                }
                            } else {
                                if (isBangla) {
                                    "সরবরাহকারীর আগের প্রদেয় বকেয়া দিলে স্বয়ংক্রিয় জাবেদা (Dr 3010 মূলধন / Cr 2010 প্রদেয় হিসাব) পোস্ট হবে—স্টক বা PO-তে কোনো প্রভাব পড়বে না।"
                                } else {
                                    "Posts a balanced opening payable entry (Dr 3010 Equity / Cr 2010 Accounts Payable) without affecting inventory or POs."
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    ERPActionButton(text = stringResource(R.string.cancel), onClick = onDismiss, variant = ERPButtonVariant.GHOST)
                    Spacer(modifier = Modifier.width(8.dp))
                    ERPActionButton(
                        text = stringResource(R.string.save),
                        onClick = {
                            if (name.isBlank()) {
                                validationError = if (isBangla) "নাম লেখা আবশ্যক" else "Name is required"
                                return@ERPActionButton
                            }
                            if (phone.isBlank()) {
                                validationError = if (isBangla) "মোবাইল নম্বর দেওয়া আবশ্যক" else "Phone number is required"
                                return@ERPActionButton
                            }

                            val normalizedOpening = Formatters.normalizeAsciiNumberInput(openingBalanceStr)
                            val openingMinor = if (normalizedOpening.isBlank()) {
                                0L
                            } else {
                                val parsed = runCatching { Money.parseFromString(normalizedOpening).minorUnits }.getOrNull()
                                if (parsed == null || parsed < 0L) {
                                    validationError = if (isBangla) "পূর্বের বকেয়ার পরিমাণ সঠিক লিখুন (ঋণাত্মক নয়)" else "Please enter a valid non-negative opening amount"
                                    return@ERPActionButton
                                }
                                parsed
                            }

                            if (isCustomer) {
                                val normalizedLimit = Formatters.normalizeAsciiNumberInput(creditLimitStr)
                                val limit = if (normalizedLimit.isBlank()) {
                                    0L
                                } else {
                                    val parsedLimit = runCatching { Money.parseFromString(normalizedLimit).minorUnits }.getOrNull()
                                    if (parsedLimit == null || parsedLimit < 0L) {
                                        validationError = if (isBangla) "ক্রেডিট লিমিট সঠিক লিখুন" else "Please enter a valid credit limit"
                                        return@ERPActionButton
                                    }
                                    parsedLimit
                                }
                                coroutineScope.launch {
                                    var savedPhotoPath: String? = editingCustomer?.profileImagePath
                                    if (profileImageUri != null) {
                                        try {
                                            savedPhotoPath = if (editingCustomer != null) {
                                                AppMediaStorage.saveCustomerPhoto(context, profileImageUri!!, editingCustomer.id)
                                            } else {
                                                AppMediaStorage.saveCustomerPhoto(context, profileImageUri!!)
                                            }
                                        } catch (_: Exception) {}
                                    }
                                    onSaveCustomer(
                                        name,
                                        phone,
                                        email,
                                        address,
                                        limit,
                                        savedPhotoPath,
                                        openingMinor,
                                        openingNote.trim().ifBlank { null }
                                    )
                                }
                            } else {
                                val safeCurrency = if (editingSupplier != null && !canEditSupplierCurrency) {
                                    editingSupplier.currency
                                } else currency
                                onSaveSupplier(
                                    name,
                                    contactPerson,
                                    phone,
                                    email,
                                    address,
                                    country.ifBlank { "Bangladesh" },
                                    safeCurrency,
                                    openingMinor,
                                    openingNote.trim().ifBlank { null }
                                )
                            }
                        },
                        variant = ERPButtonVariant.PRIMARY
                    )
                }
            }
        }
    }
}
