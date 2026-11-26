package com.pocketdev.advance_erp.feature.imports

import com.pocketdev.advance_erp.core.designsystem.ERPSurfaceVariant
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.communication.CommunicationModule
import com.pocketdev.advance_erp.core.communication.CommunicationRequest
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.ERPLoadingState
import com.pocketdev.advance_erp.core.designsystem.ERPSectionHeader
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.feature.communication.CommunicationDialog
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.core.util.EditorDraftStore
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt

enum class ImportModuleTab(val titleEn: String, val titleBn: String, val icon: ImageVector) {
    OVERVIEW("Overview", "সারসংক্ষেপ", Icons.Outlined.Dashboard),
    PURCHASE_ORDERS("Purchase Orders", "ক্রয়াদেশ (PO)", Icons.Outlined.ShoppingCart),
    SHIPMENTS("Shipments", "চালানসমূহ", Icons.Outlined.LocalShipping),
    COST_LAYERS("Inventory Cost Layers", "কস্ট লেয়ার্স", Icons.Outlined.Layers)
}

@Composable
fun ImportManagementScreen(
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = hiltViewModel(),
    initialTab: ImportModuleTab = ImportModuleTab.OVERVIEW
) {
    var shipmentQuery by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    val isBangla = uiState.language == AppLanguage.BANGLA

    // Dialog state controllers
    var showCreatePoDialog by rememberSaveable { mutableStateOf(false) }
    var showRecordPaymentDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateShipmentDialog by rememberSaveable { mutableStateOf(false) }
    var showAddCostDialog by rememberSaveable { mutableStateOf(false) }
    var receivingOrderId by rememberSaveable { mutableStateOf<String?>(null) }
    var receivingShipmentId by rememberSaveable { mutableStateOf<String?>(null) }
    var packingOrderId by rememberSaveable { mutableStateOf<String?>(null) }
    var packingShipmentId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddAdditionalCostDialog by rememberSaveable { mutableStateOf(false) }
    var showCompletionDialog by rememberSaveable { mutableStateOf(false) }
    var showShipmentWorkspaceDialog by rememberSaveable { mutableStateOf(false) }
    var editingPoId by rememberSaveable { mutableStateOf<String?>(null) }
    var poToShare by remember { mutableStateOf<PurchaseOrder?>(null) }

    // Preselected entities for dialogs
    var preselectedOrderIdForPayment by remember { mutableStateOf<String?>(null) }
    var preselectedSupplierIdForPayment by remember { mutableStateOf<String?>(null) }
    var preselectedOrderIdForShipment by remember { mutableStateOf<String?>(null) }
    var preselectedSupplierIdForShipment by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (uiState.isLoading && uiState.purchaseOrders.isEmpty() && uiState.shipments.isEmpty()) {
        ERPLoadingState(modifier = modifier.fillMaxSize())
        return
    }

    val selectedShipment = uiState.shipments.find { it.id == uiState.selectedShipmentId }
        ?: uiState.shipments.firstOrNull()

    PoReferenceTheme {
    var communicationRequest by remember { mutableStateOf<CommunicationRequest?>(null) }
    communicationRequest?.let { req ->
        CommunicationDialog(
            initialRequest = req,
            onDismiss = { communicationRequest = null }
        )
    }
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ERPBackground)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            com.pocketdev.advance_erp.feature.communication.CommunicationLauncher(module = com.pocketdev.advance_erp.core.communication.CommunicationModule.PURCHASE_ORDER, isBangla = isBangla)
            var showModuleNavigation by rememberSaveable {mutableStateOf(false)}
            if(selectedTab!=ImportModuleTab.PURCHASE_ORDERS || showModuleNavigation) {
            // Module Header Banner
            ERPCard(
                backgroundColor = DeepNavy,
                contentPadding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(PrimaryPurple, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Public,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.import_management),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.import_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }


                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Module Navigation Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(ImportModuleTab.entries) { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.surface)
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else PrimaryPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) tab.titleBn else tab.titleEn,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            }
            // Pre-calculate filtered orders outside LazyColumn
            val supplierMap = remember(uiState.suppliers) { uiState.suppliers.associateBy { it.id } }
            val poById = remember(uiState.purchaseOrders) { uiState.purchaseOrders.associateBy { it.id } }
            val filteredOrders = remember(uiState.purchaseOrders, uiState.searchQuery, uiState.poStatusFilter, uiState.poSupplierFilter, uiState.shipments) {
                uiState.purchaseOrders.filter { po ->
                    val supplier = supplierMap[po.supplierId]
                    val query = uiState.searchQuery.trim()
                    val matchesSearch = query.isBlank() ||
                            po.poNumber.contains(query, ignoreCase = true) ||
                            (supplier?.companyName?.contains(query, ignoreCase = true) == true) ||
                            (po.notes?.contains(query, ignoreCase = true) == true) ||
                            uiState.shipments.any { it.purchaseOrderId == po.id && (it.shipmentNumber.contains(query, ignoreCase = true) || it.trackingReference?.contains(query, ignoreCase = true) == true) }

                    val matchesStatus = uiState.poStatusFilter == null || po.status == uiState.poStatusFilter?.name
                    val matchesSupplier = uiState.poSupplierFilter == null || po.supplierId == uiState.poSupplierFilter

                    matchesSearch && matchesStatus && matchesSupplier
                }
            }

            // Active Tab Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // ==========================================
                // TAB 0: OVERVIEW DASHBOARD
                // ==========================================
                if (selectedTab == ImportModuleTab.OVERVIEW) {
                    val activeOrders = uiState.purchaseOrders.filter { it.status !in listOf(PurchaseOrderStatus.DRAFT.name, PurchaseOrderStatus.CANCELLED.name) }
                    val totalPoValueBdt = activeOrders.sumOf { it.totalBdtAmountMinor }
                    val totalPaidBdt = activeOrders.sumOf { it.paidBdtAmountMinor }
                    val totalOutstandingBdt = (totalPoValueBdt - totalPaidBdt).coerceAtLeast(0L)
                    val activeShipmentsCount = uiState.shipments.count { it.status == "IN_TRANSIT" || it.status == "CUSTOMS" }
                    val totalCbmSum = uiState.shipments.fold(java.math.BigDecimal.ZERO) { acc, s -> acc.add(java.math.BigDecimal(s.totalCbm)) }
                    val totalExpensesBdt = uiState.costItems.sumOf { it.bdtAmountMinor }

                    // Quick Actions Row
                    item {
                        ERPCard(contentPadding = 12.dp) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (isBangla) "দ্রুত কার্যক্রম (Quick Actions)" else "Quick Actions",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showCreatePoDialog = true },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Text(text = if (isBangla) "ক্রয়াদেশ" else "New PO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            preselectedOrderIdForPayment = null
                                            preselectedSupplierIdForPayment = null
                                            showRecordPaymentDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Text(text = if (isBangla) "+ পেমেন্ট" else "+ Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            selectedTab = ImportModuleTab.COST_LAYERS
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Text(text = if (isBangla) "কস্ট লেয়ার্স" else "Cost Layers", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Key KPI Cards Grid
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // PO Commitments Card
                            ERPCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = ImportModuleTab.PURCHASE_ORDERS },
                                contentPadding = 14.dp
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBangla) "মোট ক্রয়াদেশ" else "Total POs",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${uiState.purchaseOrders.size}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = Formatters.formatMoney(totalPoValueBdt, CurrencyCode.BDT, uiState.language),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple
                                    )
                                }
                            }

                            // Outstanding Payables Card
                            ERPCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = ImportModuleTab.PURCHASE_ORDERS },
                                contentPadding = 14.dp
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Payments, contentDescription = null, tint = StatusError, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBangla) "বকেয়া দেনা" else "Outstanding",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = Formatters.formatMoney(totalOutstandingBdt, CurrencyCode.BDT, uiState.language),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = if (totalOutstandingBdt > 0) StatusError else StatusSuccess
                                    )
                                    Text(
                                        text = "${if (isBangla) "পরিশোধিত: " else "Paid: "} ${Formatters.formatMoney(totalPaidBdt, CurrencyCode.BDT, uiState.language)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        val activeCostLayersCount = uiState.costLayers.count { it.remainingQuantity > 0 }
                        val totalValuationMinor = uiState.costLayers.filter { it.remainingQuantity > 0 }
                            .sumOf { it.unitLandedCostBdtMinor * it.remainingQuantity }
                        val totalRemainingPcs = uiState.costLayers.sumOf { it.remainingQuantity }

                        ERPCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTab = ImportModuleTab.COST_LAYERS },
                            contentPadding = 14.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Layers, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isBangla) "ইনভেন্টরি কস্ট লেয়ার্স ও মজুদ মূল্যায়ন" else "Inventory Cost Layers & Valuation",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$activeCostLayersCount ${if (isBangla) "টি সক্রিয় ব্যাচ • মোট $totalRemainingPcs পিস মজুদ" else "Active Batches • $totalRemainingPcs pcs in stock"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isBangla) "মোট মজুদ মূল্য" else "Total Valuation",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = Formatters.formatMoney(totalValuationMinor, CurrencyCode.BDT, uiState.language),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 1: PURCHASE ORDERS
                // ==========================================
                if (selectedTab == ImportModuleTab.PURCHASE_ORDERS) {
                    poReferenceListItems(uiState.purchaseOrders,uiState.suppliers,filteredOrders,uiState.language,uiState.searchQuery,uiState.poStatusFilter,uiState.poSupplierFilter,
                        viewModel::setSearchQuery,viewModel::setPoStatusFilter,viewModel::setPoSupplierFilter,
                        onCreate={showCreatePoDialog=true},onPayment={preselectedOrderIdForPayment=null;preselectedSupplierIdForPayment=null;showRecordPaymentDialog=true},onOpen={viewModel.selectPurchaseOrder(it)},onTools={showModuleNavigation=!showModuleNavigation})
                }

                if (selectedTab == ImportModuleTab.SHIPMENTS) {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (isBangla) "চালানসমূহ" else "Shipments", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Button(onClick = {
                                preselectedOrderIdForShipment = null
                                preselectedSupplierIdForShipment = null
                                showCreateShipmentDialog = true
                            }) { Text(if (isBangla) "নতুন চালান" else "New shipment") }
                        }
                    }
                    item {
                        OutlinedTextField(value = shipmentQuery, onValueChange = { shipmentQuery = it },
                            label = { Text(if (isBangla) "চালান, ট্র্যাকিং বা সরবরাহকারী খুঁজুন" else "Search shipment, tracking or supplier") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    val query = shipmentQuery.trim()
                    val matches = uiState.shipments.filter { shipment ->
                        val linkedPo = shipment.purchaseOrderId?.let { poById[it] }
                        val isHiddenLocalBatch = shipment.shipmentNumber.startsWith("BATCH-") &&
                            linkedPo?.currency.equals("BDT", ignoreCase = true)
                        !isHiddenLocalBatch && (
                            query.isBlank() || shipment.shipmentNumber.contains(query, true) ||
                                shipment.trackingReference.orEmpty().contains(query, true) ||
                                uiState.suppliers.find { it.id == shipment.supplierId }?.companyName.orEmpty().contains(query, true)
                        )
                    }
                    if (matches.isEmpty()) item {
                        ERPCard {
                            Text(if (query.isNotBlank()) {
                                if (isBangla) "এই অনুসন্ধানে কোনো চালান পাওয়া যায়নি।" else "No shipments match your search."
                            } else if (isBangla) "কোনো চালান নেই। ক্রয়াদেশ থেকে বা নতুন চালান বাটনে শুরু করুন।"
                                else "No shipments yet. Start from a purchase order or create a new shipment.",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    items(matches, key = { it.id }) { shipment ->
                        val linkedPoId = shipment.purchaseOrderId
                            ?: uiState.purchaseOrders.firstOrNull {
                                it.shipmentId == shipment.id && it.status != PurchaseOrderStatus.CANCELLED.name
                            }?.id
                        LiveShipmentCard(shipment, uiState.selectedShipmentId == shipment.id,
                            onSelect = {
                                viewModel.selectPurchaseOrder(null)
                                viewModel.selectShipment(shipment.id)
                                showShipmentWorkspaceDialog = true
                            }, onSharePdf = {
                                if (linkedPoId != null) {
                                    communicationRequest = CommunicationRequest(CommunicationModule.PACKING, linkedPoId)
                                } else {
                                    viewModel.sharePackingListPdf(context, shipment)
                                }
                            }, isBangla = isBangla)
                    }
                }

                // ==========================================
                // TAB 2: INVENTORY COST LAYERS
                // ==========================================
                if (selectedTab == ImportModuleTab.COST_LAYERS) {
                    item {
                        ERPSectionHeader(
                            title = "Inventory Batch Cost Layers",
                            subtitle = "Traceable FIFO / Batch Costing with Landed Cost History"
                        )
                    }

                    if (uiState.costLayers.isEmpty()) {
                        item {
                            ERPCard {
                                Text("No inventory cost layers created yet. Post a warehouse receipt to generate batch cost layers.", color = TextMuted)
                            }
                        }
                    } else {
                        items(uiState.costLayers) { layer ->
                            val product = uiState.products.find { it.id == layer.productId }
                            ERPCard(contentPadding = 14.dp) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(product?.nameEn ?: layer.productId, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("Batch: ${layer.receiptNumber} • ${layer.shipmentNumber}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Unit Landed Cost", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Text(
                                                Formatters.formatMoney(layer.unitLandedCostBdtMinor, CurrencyCode.BDT, uiState.language),
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryPurple
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Initial: ${layer.initialQuantity} pcs", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Text("Remaining: ${layer.remainingQuantity} pcs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = SecondaryTeal)
                                        Text(Formatters.formatDate(layer.receiptDate, uiState.language), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                }
                            }
                        }
                    }
                }

                // Notice Banner
                item {
                    ERPCard(backgroundColor = ERPSurfaceVariant) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = SecondaryTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.import_feature_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 70.dp)
        )
    }

    // ==========================================
    // DIALOGS
    // ==========================================

    if (showCreatePoDialog) {
        CreatePurchaseOrderDialog(
            suppliers = uiState.suppliers,
            products = uiState.products,
            variants = uiState.productVariants,
            categories = uiState.categories,
            resolveProductImage = viewModel::resolvePurchaseProductImage,
            isBangla = isBangla,
            onDismiss = { showCreatePoDialog = false },
            isSaving = uiState.isSavingPurchaseOrder,
            saveError = uiState.purchaseOrderSaveError,
            onAddNewSupplier = viewModel::addNewSupplierFromPurchase,
            onCreateFullProduct = viewModel::createFullProductFromPurchase,
            onGenerateSku = viewModel::generateBaseSku,
            onGenerateVariantSku = viewModel::generateVariantSku,
            onGenerateBarcode = viewModel::generateBarcode,
            onValidateSku = viewModel::isBaseSkuAvailable,
            onValidateBarcode = viewModel::isBarcodeAvailable,
            onAddNewVariant = viewModel::addNewVariantFromPurchase,
            onAddNewCategory = viewModel::addNewCategory,
            onSavePo = { supId, cur, rate, items, notes, status, cartons, chinaDeliveryMinor, catRatesMinor, handlingMinor, sampleMinor, attachments, orderDateMillis, expectedArrivalDateMillis, shippingMethod ->
                viewModel.createPurchaseOrder(
                    supplierId = supId,
                    currency = cur,
                    exchangeRate = rate,
                    items = items,
                    notes = notes,
                    status = status,
                    cartons = cartons,
                    chinaDeliveryChargeMinor = chinaDeliveryMinor,
                    categoryFreightRatesBdtMinor = catRatesMinor,
                    handlingCostMinor = handlingMinor,
                    sampleCostMinor = sampleMinor,
                    orderDateMillis = orderDateMillis,
                    expectedArrivalDateMillis = expectedArrivalDateMillis,
                    shippingMethod = shippingMethod,
                    onSaved = { EditorDraftStore(context).remove("purchase:new"); showCreatePoDialog = false },
                    attachments = attachments
                )
            }
        )
    }

    val currentEditingPo = uiState.purchaseOrders.find { it.id == editingPoId }
    if (currentEditingPo != null) {
        val orderId = currentEditingPo.id
        val editItems = remember(orderId) { viewModel.getItemsForOrder(orderId) }.collectAsState(initial = null).value
        val editCartons = remember(orderId) { viewModel.getCartonsForOrder(orderId) }.collectAsState(initial = null).value
        val editCosts = remember(orderId) { viewModel.getCostItemsForOrder(orderId) }.collectAsState(initial = null).value
        val editReceipts = remember(orderId) { viewModel.getReceiptsForOrder(orderId) }.collectAsState(initial = null).value
        val editFreightRates = remember(orderId) { viewModel.getFreightRatesForOrder(orderId) }.collectAsState(initial = null).value

        if (editItems != null && editCartons != null && editCosts != null &&
            editReceipts != null && editFreightRates != null
        ) {
            val onOrderSaved = {
                EditorDraftStore(context).apply {
                    remove("purchase:$orderId")
                    remove("purchase:$orderId:metadata")
                }
                editingPoId = null
            }
            CreatePurchaseOrderDialog(
                suppliers = uiState.suppliers,
                products = uiState.products,
                variants = uiState.productVariants,
                categories = uiState.categories,
            resolveProductImage = viewModel::resolvePurchaseProductImage,
                isBangla = isBangla,
                editingOrder = currentEditingPo,
                metadataOnly = currentEditingPo.isActualCostFinalized ||
                    currentEditingPo.status in setOf(
                        PurchaseOrderStatus.COMPLETED.name,
                        PurchaseOrderStatus.RECEIVED.name,
                        PurchaseOrderStatus.PARTIALLY_RECEIVED.name
                    ) ||
                    editItems.any { it.receivedQuantity > 0 } ||
                    editReceipts.isNotEmpty(),
                onUpdateMetadata = { notes, attachments ->
                    viewModel.updatePurchaseOrderMetadata(orderId, notes, attachments, onSaved = onOrderSaved)
                },
                isSaving = uiState.isSavingPurchaseOrder,
                saveError = uiState.purchaseOrderSaveError,
                initialEditingItems = editItems,
                initialEditingCartons = editCartons,
                initialEditingCosts = editCosts,
                initialEditingFreightRates = editFreightRates.associate { it.categoryId to it.appliedRatePerKgBdtMinor },
                initialShippingMethod = uiState.shipments.firstOrNull { it.purchaseOrderId == orderId }?.shippingMethod ?: "SEA",
                onDismiss = { editingPoId = null },
                onAddNewSupplier = viewModel::addNewSupplierFromPurchase,
                onCreateFullProduct = viewModel::createFullProductFromPurchase,
                onGenerateSku = viewModel::generateBaseSku,
                onGenerateVariantSku = viewModel::generateVariantSku,
                onGenerateBarcode = viewModel::generateBarcode,
                onValidateSku = viewModel::isBaseSkuAvailable,
                onValidateBarcode = viewModel::isBarcodeAvailable,
                onAddNewVariant = viewModel::addNewVariantFromPurchase,
                onAddNewCategory = viewModel::addNewCategory,
                onUpdatePo = { ordId, supId, cur, rate, items, notes, status, cartons, chinaDeliveryMinor, catRatesMinor, handlingMinor, sampleMinor, attachments, orderDateMillis, expectedArrivalDateMillis, shippingMethod ->
                    viewModel.updatePurchaseOrder(
                        orderId = ordId,
                        supplierId = supId,
                        currency = cur,
                        exchangeRate = rate,
                        items = items,
                        notes = notes,
                        status = status,
                        cartons = cartons,
                        chinaDeliveryChargeMinor = chinaDeliveryMinor,
                        categoryFreightRatesBdtMinor = catRatesMinor,
                        handlingCostMinor = handlingMinor,
                        sampleCostMinor = sampleMinor,
                        orderDateMillis = orderDateMillis,
                        expectedArrivalDateMillis = expectedArrivalDateMillis,
                        updateSchedule = true,
                        shippingMethod = shippingMethod,
                        onSaved = onOrderSaved,
                        attachments = attachments
                    )
                },
                onSavePo = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
            )
        }
    }

    if (showRecordPaymentDialog) {
        RecordSupplierPaymentDialog(
            suppliers = uiState.suppliers,
            orders = uiState.purchaseOrders,
            preselectedSupplierId = preselectedSupplierIdForPayment,
            preselectedOrderId = preselectedOrderIdForPayment,
            liquidAccounts = uiState.liquidAccounts,
            accountBalances = uiState.liquidAccountBalances,
            isBangla = isBangla,
            onConfirmWithDetails = { supId, ordId, amt, cur, rate, meth, acc, accId, pDate, ref, notes ->
                viewModel.recordSupplierPayment(
                    supplierId = supId,
                    orderId = ordId,
                    amountMinor = amt,
                    currency = cur,
                    exchangeRate = rate,
                    method = meth,
                    paymentAccount = acc,
                    reference = ref,
                    notes = notes,
                    paymentDate = pDate,
                    paymentAccountId = accId
                )
            },
            onDismiss = { showRecordPaymentDialog = false },
            onConfirm = { supId, ordId, amt, cur, rate, meth, acc, ref, notes ->
                viewModel.recordSupplierPayment(supId, ordId, amt, cur, rate, meth, acc, ref, notes)
            }
        )
    }

    if (showCreateShipmentDialog) {
        CreateShipmentDialog(
            suppliers = uiState.suppliers,
            orders = uiState.purchaseOrders,
            products = uiState.products,
            variants = uiState.productVariants,
            preselectedSupplierId = preselectedSupplierIdForShipment,
            preselectedOrderId = preselectedOrderIdForShipment,
            isBangla = isBangla,
            loadPoRemainingItems = viewModel::getPoRemainingItems,
            onDismiss = { showCreateShipmentDialog = false },
            onConfirm = { supId, ordId, meth, orig, dest, trk, shipLine, cartons, notes, onSuccess, onError ->
                viewModel.createShipment(
                    supplierId = supId,
                    orderId = ordId,
                    method = meth,
                    origin = orig,
                    destination = dest,
                    trackingRef = trk,
                    shippingLine = shipLine,
                    cartons = cartons,
                    notes = notes,
                    onSuccess = { shipmentId, shipmentNumber ->
                        onSuccess(shipmentId, shipmentNumber)
                        viewModel.selectPurchaseOrder(null)
                        viewModel.selectShipment(shipmentId)
                        shipmentQuery = ""
                        selectedTab = ImportModuleTab.SHIPMENTS
                    },
                    onError = { err ->
                        onError(err)
                    }
                )
            }
        )
    }

    val packingOrder = uiState.purchaseOrders.find { it.id == packingOrderId }
    val packingShipment = uiState.shipments.find { it.id == packingShipmentId }
    val packingTargetId = packingOrder?.id ?: packingShipment?.id
    if (packingTargetId != null) {
        val closePacking = {
            packingOrderId = null
            packingShipmentId = null
        }
        val cartonSaved = {
            EditorDraftStore(context).remove("packing:$packingTargetId")
            closePacking()
        }
        AddCartonDialog(
            shipment = packingShipment,
            orderId = packingOrder?.id,
            packedCartonCount = packingOrder?.totalCartons ?: packingShipment?.totalCartons ?: 0,
            products = uiState.products,
            variants = uiState.productVariants,
            language = uiState.language,
            isSaving = uiState.isSavingCarton,
            saveError = uiState.cartonSaveError,
            onDismiss = closePacking,
            onConfirm = { carton ->
                if (packingOrder != null) {
                    viewModel.addCartonToPurchase(packingOrder.id, carton, onSaved = cartonSaved)
                } else if (packingShipment != null) {
                    viewModel.addCartonToShipment(packingShipment.id, carton, onSaved = cartonSaved)
                }
            },
            onConfirmMixed = packingOrder?.let { order ->
                { cartonNumber, length, width, height, gross, net, lines ->
                    viewModel.addMixedCarton(order.id, cartonNumber, length, width, height, gross, net, lines, onSaved = cartonSaved)
                }
            }
        )
    }

    if (showAddCostDialog) {
        val activeOrderId = uiState.selectedPo?.id
        val activeShipmentId = selectedShipment?.id
        val grossWeight = uiState.selectedPo?.totalGrossWeightKg?.let { java.math.BigDecimal(it.ifBlank { "0" }) }
            ?: selectedShipment?.totalGrossWeightKg?.let { java.math.BigDecimal(it.ifBlank { "0" }) }
            ?: java.math.BigDecimal("350.0")
        AddCostItemDialog(
            shipmentId = activeShipmentId,
            orderId = activeOrderId,
            totalGrossWeightKg = grossWeight,
            allocationTargets = if (activeOrderId != null) uiState.selectedPoItems.associate { item ->
                (if (item.variantId != null) "${item.productId}:${item.variantId}" else item.productId) to
                    (uiState.products.find { it.id == item.productId }?.nameEn.orEmpty() + " " + uiState.productVariants.find { it.id == item.variantId }?.sku.orEmpty())
            } else uiState.selectedShipmentCartons.associate { item ->
                (if (item.variantId != null) "${item.productId}:${item.variantId}" else item.productId) to
                    (uiState.products.find { it.id == item.productId }?.nameEn.orEmpty() + " " + uiState.productVariants.find { it.id == item.variantId }?.sku.orEmpty())
            },
            isBangla = isBangla,
            onDismiss = { showAddCostDialog = false },
            onConfirm = { cat, desc, amt, cur, rate, meth, acc, ref, isEst, notes, manual ->
                if (activeOrderId != null) {
                    viewModel.addPurchaseCostItem(
                        orderId = activeOrderId,
                        category = cat,
                        description = desc,
                        amountMinor = amt,
                        currency = cur,
                        exchangeRate = rate,
                        method = meth,
                        isEstimated = isEst,
                        paymentAccount = acc,
                        docRef = ref,
                        notes = notes,
                        manualAllocationsBdtMinor = manual
                    )
                } else if (activeShipmentId != null) {
                    viewModel.addCostItem(activeShipmentId, cat, desc, amt, cur, rate, meth, acc, ref, manual)
                }
            }
        )
    }

    val receivingOrder = uiState.purchaseOrders.find { it.id == receivingOrderId }
    val receivingShipment = uiState.shipments.find { it.id == receivingShipmentId }
    val receivingTargetId = receivingOrder?.id ?: receivingShipment?.id
    if (receivingTargetId != null) {
        val receivingItems = if (receivingOrder != null) {
            remember(receivingTargetId) { viewModel.getItemsForOrder(receivingTargetId) }
                .collectAsState(initial = null).value
        } else {
            emptyList()
        }
        val receivingCartons = remember(receivingTargetId, receivingOrder != null) {
            if (receivingOrder != null) viewModel.getCartonsForOrder(receivingTargetId)
            else viewModel.getCartonsForShipment(receivingTargetId)
        }.collectAsState(initial = null).value
        if (receivingItems != null && receivingCartons != null) {
            val closeReceiving = {
                receivingOrderId = null
                receivingShipmentId = null
            }
            val onReceiptSaved = {
                EditorDraftStore(context).remove("receiving:$receivingTargetId")
                closeReceiving()
            }
            val shipmentLinkedOrder = uiState.purchaseOrders.find { it.id == receivingShipment?.purchaseOrderId }
            val shipmentSnapshots = if (receivingShipment != null) {
                remember(receivingShipment.id) { viewModel.getLandedCostSnapshots(receivingShipment.id) }
                    .collectAsState(initial = emptyList()).value
            } else {
                emptyList()
            }
            val costFinalized = (receivingOrder ?: shipmentLinkedOrder)?.isActualCostFinalized
                ?: (shipmentSnapshots.isNotEmpty() && shipmentSnapshots.all { it.isFinal })
            WarehouseReceivingDialog(
                shipment = receivingShipment,
                order = receivingOrder,
                poItems = receivingItems,
                cartons = receivingCartons,
                products = uiState.products,
                isCostFinalized = costFinalized,
                isBangla = isBangla,
                isSaving = uiState.isSavingReceipt,
                saveError = uiState.receiptSaveError,
                onDismiss = closeReceiving,
                onSaveDraft = receivingOrder?.let { po ->
                    { warehouse, method, items, notes ->
                        viewModel.createPurchaseReceiving(po.id, warehouse, method, items, notes, onSaved = onReceiptSaved)
                    }
                },
                onConfirmPost = { warehouse, method, items, notes, allowDiscrepancy ->
                    if (receivingOrder != null) {
                        viewModel.createPurchaseReceiving(
                            receivingOrder.id, warehouse, method, items, notes, postImmediately = true, onSaved = onReceiptSaved
                        )
                    } else if (receivingShipment != null) {
                        viewModel.createAndPostWarehouseReceipt(
                            receivingShipment.id, warehouse, items, notes, allowDiscrepancy, onSaved = onReceiptSaved
                        )
                    }
                }
            )
        }
    }

    if (showShipmentWorkspaceDialog && selectedShipment != null) {
        ShipmentWorkspaceDialog(
            shipment = selectedShipment,
            suppliers = uiState.suppliers,
            allPurchaseOrders = uiState.purchaseOrders,
            shipmentSuppliers = uiState.selectedShipmentSuppliers,
            shipmentOrders = uiState.selectedShipmentOrders,
            cartons = uiState.selectedShipmentCartons,
            categories = uiState.categories,
            freightRates = uiState.selectedShipmentFreightRates,
            costItems = uiState.costItems,
            landingPreview = uiState.selectedShipmentLandingPreview,
            previewError = uiState.landingPreviewError,
            previewLoading = uiState.isLoadingLandingPreview,
            onRetryPreview = { viewModel.refreshShipmentLandingPreview(selectedShipment.id) },
            snapshots = uiState.landedCostSnapshots,
            costAdjustments = uiState.selectedShipmentCostAdjustments,
            products = uiState.products,
            warehouseRentHistory = uiState.warehouseRentHistory,
            isBangla = isBangla,
            onDismiss = { showShipmentWorkspaceDialog = false },
            onAddCartonClick = { packingShipmentId = selectedShipment.id },
            onReceiveClick = { receivingShipmentId = selectedShipment.id },
            onShareReportPdf = { viewModel.shareLandedCostPdf(context, selectedShipment) },
            onSaveSupplierChinaDelivery = { draft -> viewModel.saveShipmentSupplierAndChinaDelivery(draft) },
            onLinkPoToShipment = { poId -> viewModel.linkPoToShipment(poId, selectedShipment.id) },
            onUpdateCategoryDefaultFreightRate = { catId, rate -> viewModel.updateCategoryDefaultFreightRate(catId, rate) },
            onOverrideShipmentFreightRate = { catId, ov -> viewModel.overrideShipmentCategoryFreightRate(selectedShipment.id, catId, ov) },
            onAddExpense = { cat, desc, amt, cur, fx, meth, ref, manualMap ->
                viewModel.addShipmentExpenseWithManualAllocations(
                    shipmentId = selectedShipment.id,
                    category = cat,
                    description = desc,
                    amountMinor = amt,
                    currency = cur,
                    exchangeRate = fx,
                    method = meth,
                    paymentAccount = "City Bank Corporate (Import LC)",
                    docRef = ref,
                    manualAllocationsBdtMinor = manualMap
                )
            },
            onFinalizeLandingCost = { viewModel.finalizeShipmentLandingCost(selectedShipment.id) },
            onApplyPostFinalizeAdjustment = { reason -> viewModel.applyPostFinalizationCostAdjustment(selectedShipment.id, reason) },
            onRecordWarehouseRent = { whId, month, amt, notes -> viewModel.recordWarehouseMonthlyRent(whId, month, amt, "coa-1010", notes) }
        )
    }

    if (showAddAdditionalCostDialog && uiState.selectedPo != null) {
        AddAdditionalCostDialog(
            orderId = uiState.selectedPo!!.id,
            isBangla = isBangla,
            onDismiss = { showAddAdditionalCostDialog = false },
            onConfirm = { cat, desc, amt, isCap, meth, notes ->
                viewModel.addAdditionalCost(
                    orderId = uiState.selectedPo!!.id,
                    warehouseId = null,
                    category = cat,
                    description = desc,
                    amountMinor = amt,
                    isCapitalizable = isCap,
                    allocationMethod = meth,
                    notes = notes
                )
            }
        )
    }

    if (showCompletionDialog && uiState.selectedPo != null) {
        val po = uiState.selectedPo!!
        val totalOrdered = uiState.selectedPoItems.sumOf { it.quantity }
        val totalReceived = uiState.selectedPoItems.sumOf { it.receivedQuantity }
        val dmgQty = uiState.selectedPoDamageRecords.sumOf { it.damagedQuantity }
        val misQty = uiState.selectedPoShortageRecords.sumOf { it.missingQuantity }
        val goodQty = (totalReceived - dmgQty).coerceAtLeast(0)
        val remainingDue = (po.totalAmountMinor - po.paidAmountMinor).coerceAtLeast(0L)

        PurchaseCompletionDialog(
            order = po,
            totalOrdered = totalOrdered,
            totalReceived = totalReceived,
            goodQuantity = goodQty,
            damagedQuantity = dmgQty,
            missingQuantity = misQty,
            paymentDueMinor = remainingDue,
            isCostFinalized = po.isActualCostFinalized,
            isBangla = isBangla,
            onDismiss = { showCompletionDialog = false },
            isSaving = uiState.isCompletingPurchase,
            saveError = uiState.completionError,
            onConfirmComplete = { disp, reas, nts ->
                viewModel.completePurchase(po.id, disp, reas, nts) { showCompletionDialog = false }
            }
        )
    }

    // Purchase Order Detail Dialog
    if (uiState.selectedPo != null) {
        val selectedPo = uiState.selectedPo!!
        val supplier = uiState.suppliers.find { it.id == selectedPo.supplierId }
        PurchaseOrderDetailDialog(
            order = selectedPo,
            supplier = supplier,
            items = uiState.selectedPoItems,
            products = uiState.products,
            payments = uiState.selectedPoPayments,
            shipments = uiState.selectedPoShipments,
            cartons = uiState.selectedPoCartons,
            costItems = uiState.selectedPoCostItems,
            receipts = uiState.selectedPoReceipts,
            damageRecords = uiState.selectedPoDamageRecords,
            shortageRecords = uiState.selectedPoShortageRecords,
            additionalCosts = uiState.selectedPoAdditionalCosts,
            completionRecord = uiState.selectedPoCompletionRecord,
            language = uiState.language,
            onDismiss = { viewModel.selectPurchaseOrder(null) },
            onConfirmPo = { orderId ->
                viewModel.confirmPurchaseOrder(orderId)
            },
            onMakePayment = { po ->
                preselectedSupplierIdForPayment = po.supplierId
                preselectedOrderIdForPayment = po.id
                showRecordPaymentDialog = true
            },
            onCreateShipment = { po ->
                preselectedSupplierIdForShipment = po.supplierId
                preselectedOrderIdForShipment = po.id
                showCreateShipmentDialog = true
            },
            onAddCarton = { packingOrderId = it.id },
            onAddCostItem = { showAddCostDialog = true },
            onFinalizeActualCost = { orderId ->
                viewModel.finalizePurchaseActualCost(orderId)
            },
            onReceiveProducts = { receivingOrderId = it.id },
            onPostReceiving = { receiptId ->
                viewModel.postPurchaseReceiving(receiptId)
            },
            onAddAdditionalCost = { showAddAdditionalCostDialog = true },
            onCompletePo = { showCompletionDialog = true },
            onEditFullPo = { po -> editingPoId = po.id },
            onReopenPoForEdit = { po ->
                viewModel.reopenPurchaseOrderForEditing(po.id) {
                    EditorDraftStore(context).apply {
                        remove("purchase:${po.id}")
                        remove("purchase:${po.id}:metadata")
                        remove("receiving:${po.id}")
                    }
                    viewModel.selectPurchaseOrder(null)
                    editingPoId = po.id
                }
            },
            onSharePdf = { po -> poToShare = po },
            onShareSupplierPayment = { payment ->
                communicationRequest = CommunicationRequest(CommunicationModule.SUPPLIER_PAYMENT, payment.id)
            },
            onCancelPo = { orderId, reason ->
                viewModel.cancelPurchaseOrder(orderId, reason)
            },
            onDeletePo = { orderId ->
                viewModel.deletePurchaseOrder(orderId)
            },
            onReversePayment = { paymentId ->
                viewModel.reverseSupplierPayment(paymentId)
            },
            freightPayments = uiState.selectedPoFreightPayments,
            ledgerAccruedFreightBillMinor = uiState.selectedPoAccruedFreightBillMinor,
            liquidAccounts = uiState.liquidAccounts,
            accountBalances = uiState.liquidAccountBalances,
            onRecordFreightPayment = { ordId, accId, amtMinor, pDate, nts ->
                viewModel.recordPoFreightPayment(ordId, accId, amtMinor, pDate, nts)
            },
            onReverseFreightPayment = { entryId ->
                viewModel.reversePoFreightPayment(entryId)
            },
            onDuplicatePo = { orderId ->
                viewModel.duplicatePurchaseOrder(orderId)
            },
            onSelectShipment = { shp ->
                viewModel.selectPurchaseOrder(null)
                viewModel.selectShipment(shp.id)
                showShipmentWorkspaceDialog = true
            },
            onDeleteCarton = viewModel::deleteCarton,
            onDeleteCostItem = { costId -> viewModel.deleteCostItem(selectedPo.id, costId) },
            onUpdatePacking = { ctn, gross, net, cbm -> viewModel.updateOrderPacking(selectedPo.id, ctn, gross, net, cbm) },
            onUpdatePoItem = { itemId, q, p, c, g -> viewModel.updatePoItem(selectedPo.id, itemId, q, p, c, g) },
            onDeletePoItem = { itemId -> viewModel.deletePoItem(selectedPo.id, itemId) },
            freightRates = uiState.selectedShipmentFreightRates.filter { rate ->
                val poShipmentIds = (uiState.selectedPoShipments.map { it.id } + listOfNotNull(selectedPo.shipmentId)).toSet()
                rate.shipmentId in poShipmentIds
            },
            categories = uiState.categories,
            variants = uiState.productVariants,
            snapshots = uiState.landedCostSnapshots.filter { snap ->
                val poShipmentIds = (uiState.selectedPoShipments.map { it.id } + listOfNotNull(selectedPo.shipmentId)).toSet()
                snap.shipmentId in poShipmentIds
            },
            businessProfile = uiState.businessProfile
        )
    }

    val currentPoToShare = poToShare
    if (currentPoToShare != null) {
        val shareItems by viewModel.getItemsForOrder(currentPoToShare.id).collectAsState(initial = emptyList())
        val shareCartons by viewModel.getCartonsForOrder(currentPoToShare.id).collectAsState(initial = emptyList())
        val resolvedShareItems = shareItems.ifEmpty {
            if (uiState.selectedPo?.id == currentPoToShare.id) uiState.selectedPoItems else emptyList()
        }
        val resolvedShareCartons = shareCartons.ifEmpty {
            if (uiState.selectedPo?.id == currentPoToShare.id) uiState.selectedPoCartons else emptyList()
        }
        val supplier = uiState.suppliers.find { it.id == currentPoToShare.supplierId }
        PoShareDialog(
            order = currentPoToShare,
            items = resolvedShareItems,
            productsMap = uiState.products.associateBy { it.id },
            supplier = supplier,
            businessProfile = uiState.businessProfile,
            isBangla = isBangla,
            variantsMap = uiState.productVariants.associateBy { it.id },
            cartons = resolvedShareCartons,
            onResolveItemImages = viewModel::resolveShareImagesForItems,
            onOpenSmartWhatsApp = { req ->
                poToShare = null
                communicationRequest = req
            },
            onDismiss = { poToShare = null }
        )
    }
    } // PO palette
}

@Composable
private fun FilterChipView(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else TextPrimary
        )
    }
}

@Composable
private fun LivePurchaseOrderCard(
    order: PurchaseOrder,
    supplierName: String,
    language: AppLanguage,
    onClick: () -> Unit,
    onConfirmOrder: (String) -> Unit = {},
    onSharePdf: () -> Unit = {}
) {
    PoCompactOrderCard(order,supplierName,language,onClick)
}

@Composable
private fun LiveShipmentCard(
    shipment: ImportShipment,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onSharePdf: () -> Unit = {},
    isBangla: Boolean = false
) {
    val status = com.pocketdev.advance_erp.data.entity.ShipmentStatus.entries.find { it.name == shipment.status }
    ERPCard(contentPadding = 14.dp, modifier = Modifier.clickable(onClick = onSelect),
        backgroundColor = if (isSelected) SecondaryTeal.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(shipment.shipmentNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(status?.let { if (isBangla) it.labelBn else it.labelEn } ?: shipment.status,
                        style = MaterialTheme.typography.bodySmall, color = SecondaryTeal)
                }
                IconButton(onClick = onSharePdf, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Share, contentDescription = if (isBangla) "প্যাকিং লিস্ট শেয়ার" else "Share packing list", tint = PrimaryPurple)
                }
            }
            Text("${shipment.origin} → ${shipment.destination}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    (if (isBangla) "কার্টন" else "Cartons") to shipment.totalCartons.toString(),
                    (if (isBangla) "আয়তন" else "Volume") to "${shipment.totalCbm} m³",
                    (if (isBangla) "ওজন" else "Weight") to "${shipment.totalGrossWeightKg} kg"
                ).forEach { (label, value) ->
                    Column(Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(value, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
