package com.pocketdev.advance_erp.feature.imports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.engine.AllocationMethod
import com.pocketdev.advance_erp.core.engine.LandingCostEngine
import com.pocketdev.advance_erp.data.entity.CostCategory
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.InventoryCostAdjustment
import com.pocketdev.advance_erp.data.entity.LandedCostSnapshot
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.ShipmentFreightRate
import com.pocketdev.advance_erp.data.entity.ShipmentSupplier
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.domain.repository.ShipmentSupplierDraft
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun ShipmentWorkspaceDialog(
    shipment: ImportShipment,
    suppliers: List<Supplier>,
    allPurchaseOrders: List<PurchaseOrder>,
    shipmentSuppliers: List<ShipmentSupplier>,
    shipmentOrders: List<PurchaseOrder>,
    cartons: List<ShipmentCarton>,
    categories: List<ProductCategory>,
    freightRates: List<ShipmentFreightRate>,
    costItems: List<ImportCostItem>,
    landingPreview: LandingCostEngine.ShipmentLandingCostResult?,
    snapshots: List<LandedCostSnapshot>,
    costAdjustments: List<InventoryCostAdjustment>,
    products: List<Product>,
    warehouseRentHistory: List<ExpenseVoucher>,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onAddCartonClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onShareReportPdf: () -> Unit,
    onSaveSupplierChinaDelivery: (ShipmentSupplierDraft) -> Unit,
    onLinkPoToShipment: (String) -> Unit,
    onUpdateCategoryDefaultFreightRate: (categoryId: String, rateMinor: Long) -> Unit,
    onOverrideShipmentFreightRate: (categoryId: String, overrideRateMinor: Long?) -> Unit,
    onAddExpense: (
        category: CostCategory,
        description: String,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: AllocationMethod,
        docRef: String?,
        manualAllocationsBdtMinor: Map<String, Long>
    ) -> Unit,
    onFinalizeLandingCost: () -> Unit,
    onApplyPostFinalizeAdjustment: (reason: String) -> Unit,
    onRecordWarehouseRent: (warehouseId: String, rentMonth: String, amountMinor: Long, notes: String?) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showConfirmFinalize by remember { mutableStateOf(false) }
    var postFinalizeReason by remember { mutableStateOf("Post-finalization Customs / Port / Transport bill") }

    val tabs = if (isBangla) {
        listOf(
            "১. সরবরাহকারী ও চীন ডেলিভারি",
            "২. প্যাকিং ও ক্যাটাগরি ফ্রেইট",
            "৩. বিডি পরিবহন ও খরচ",
            "৪. ল্যান্ডিং কস্ট ও ফাইনাল",
            "৫. গুদাম ভাড়া (Operating)"
        )
    } else {
        listOf(
            "1. Suppliers & China Delivery",
            "2. Packaging & Category Freight",
            "3. BD Transport & Expenses",
            "4. Landing Cost & Finalize",
            "5. Warehouse Rent"
        )
    }

    val isFinalized = snapshots.isNotEmpty() && snapshots.all { it.isFinal }
    val uncapitalizedPostCosts = costItems.filter { !it.isCapitalized }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.95f)
                .clip(RoundedCornerShape(20.dp)),
            color = ERPBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepNavy)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${shipment.shipmentNumber} • ${if (isBangla) "শিপমেন্ট ও ল্যান্ডিং কস্ট ওয়ার্কস্পেস" else "Shipment & Landing Cost Workspace"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${shipment.origin} → ${shipment.destination} | Cartons: ${shipment.totalCartons} | Qty: ${shipment.totalQuantity} | Weight: ${shipment.totalGrossWeightKg} KG | CBM: ${shipment.totalCbm}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onShareReportPdf) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Share PDF", tint = SecondaryTeal)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Horizontal scrollable Tab Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { idx, title ->
                        val active = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) PrimaryPurple else Color(0xFFF1F5F9))
                                .clickable { selectedTab = idx }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else TextPrimary
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Tab Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(14.dp)) {
                    when (selectedTab) {
                        0 -> SupplierAndChinaDeliveryTab(
                            shipment = shipment,
                            suppliers = suppliers,
                            allPurchaseOrders = allPurchaseOrders,
                            shipmentSuppliers = shipmentSuppliers,
                            shipmentOrders = shipmentOrders,
                            isBangla = isBangla,
                            onSaveSupplierChinaDelivery = onSaveSupplierChinaDelivery,
                            onLinkPoToShipment = onLinkPoToShipment
                        )
                        1 -> PackagingAndFreightTab(
                            cartons = cartons,
                            categories = categories,
                            freightRates = freightRates,
                            products = products,
                            landingPreview = landingPreview,
                            isBangla = isBangla,
                            onAddCartonClick = onAddCartonClick,
                            onUpdateCategoryDefaultFreightRate = onUpdateCategoryDefaultFreightRate,
                            onOverrideShipmentFreightRate = onOverrideShipmentFreightRate
                        )
                        2 -> BdTransportAndExpensesTab(
                            costItems = costItems,
                            products = products,
                            cartons = cartons,
                            isBangla = isBangla,
                            onAddExpense = onAddExpense
                        )
                        3 -> LandingCostReviewAndFinalizeTab(
                            landingPreview = landingPreview,
                            snapshots = snapshots,
                            costAdjustments = costAdjustments,
                            uncapitalizedPostCosts = uncapitalizedPostCosts,
                            isFinalized = isFinalized,
                            postFinalizeReason = postFinalizeReason,
                            onPostFinalizeReasonChange = { postFinalizeReason = it },
                            isBangla = isBangla,
                            onFinalizeClick = { showConfirmFinalize = true },
                            onApplyPostFinalizeAdjustment = { onApplyPostFinalizeAdjustment(postFinalizeReason) },
                            onReceiveClick = onReceiveClick
                        )
                        4 -> WarehouseRentTab(
                            warehouseRentHistory = warehouseRentHistory,
                            isBangla = isBangla,
                            onRecordWarehouseRent = onRecordWarehouseRent
                        )
                    }
                }
            }
        }
    }

    if (showConfirmFinalize) {
        AlertDialog(
            onDismissRequest = { showConfirmFinalize = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess) },
            title = {
                Text(
                    text = if (isBangla) "ল্যান্ডিং কস্ট চূড়ান্তকরণ নিশ্চিত করুন" else "Confirm Landing Cost Finalization",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBangla)
                        "আপনি কি এই শিপমেন্টের ল্যান্ডিং কস্ট চূড়ান্ত করতে চান? এটি পণ্যভিত্তিক ক্রয়মূল্য, চায়না ডেলিভারি, ক্যাটাগরি ফ্রেইট, বাংলাদেশ পরিবহন ও অন্যান্য খরচ একত্রিত করে ইউনিট ল্যান্ডিং কস্ট সংরক্ষণ করবে।"
                    else
                        "Finalize Landing Cost for ${shipment.shipmentNumber}? This locks the initial landed cost snapshot and unlocks inventory receiving & post-finalization cost adjustments."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmFinalize = false
                        onFinalizeLandingCost()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                ) {
                    Text(if (isBangla) "হ্যাঁ, চূড়ান্ত করুন" else "Finalize Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmFinalize = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun SupplierAndChinaDeliveryTab(
    shipment: ImportShipment,
    suppliers: List<Supplier>,
    allPurchaseOrders: List<PurchaseOrder>,
    shipmentSuppliers: List<ShipmentSupplier>,
    shipmentOrders: List<PurchaseOrder>,
    isBangla: Boolean,
    onSaveSupplierChinaDelivery: (ShipmentSupplierDraft) -> Unit,
    onLinkPoToShipment: (String) -> Unit
) {
    var selectedSupplierId by remember { mutableStateOf(suppliers.firstOrNull()?.id ?: shipment.supplierId) }
    var currency by remember { mutableStateOf("CNY") }
    var exchangeRateStr by remember { mutableStateOf("17.00") }
    var chinaDeliveryStr by remember { mutableStateOf("300.00") }
    var allocationMethod by remember { mutableStateOf(AllocationMethod.PURCHASE_VALUE) }

    val supplierMap = remember(suppliers) { suppliers.associateBy { it.id } }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBangla) "সরবরাহকারী-ভিত্তিক চায়না লোকাল ডেলিভারি (Phase 2A & 2B)" else "Supplier-Wise China Local Delivery (Phase 2A & 2B)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                    Text(
                        text = "Formula: Product Delivery Allocation = Supplier Delivery Charge × (Product Purchase Value / Supplier Total Purchase Value)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // Supplier selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suppliers.forEach { sup ->
                            val active = sup.id == selectedSupplierId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) PrimaryPurple else Color(0xFFF1F5F9))
                                    .clickable { selectedSupplierId = sup.id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = sup.companyName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) Color.White else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = chinaDeliveryStr,
                            onValueChange = { chinaDeliveryStr = it },
                            label = { Text("China Delivery Charge") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it.uppercase() },
                            label = { Text("Currency (CNY/USD)") },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = exchangeRateStr,
                            onValueChange = { exchangeRateStr = it },
                            label = { Text("FX Rate to BDT") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            val chargeMinor = BigDecimal(chinaDeliveryStr.trim().ifEmpty { "0" })
                                .multiply(BigDecimal("100"))
                                .setScale(0, RoundingMode.HALF_UP)
                                .longValueExact()
                            val fx = BigDecimal(exchangeRateStr.trim().ifEmpty { "17.00" })
                            onSaveSupplierChinaDelivery(
                                ShipmentSupplierDraft(
                                    shipmentId = shipment.id,
                                    supplierId = selectedSupplierId,
                                    currency = currency,
                                    exchangeRateToBdt = fx,
                                    chinaDeliveryChargeMinor = chargeMinor,
                                    chinaDeliveryCurrency = currency,
                                    chinaDeliveryExchangeRateToBdt = fx,
                                    chinaDeliveryAllocationMethod = allocationMethod
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBangla) "সরবরাহকারী ও চায়না ডেলিভারি সংরক্ষণ করুন" else "Save Supplier & China Delivery")
                    }
                }
            }
        }

        // Existing Shipment Suppliers & China Delivery List
        items(shipmentSuppliers) { ss ->
            val supName = supplierMap[ss.supplierId]?.companyName ?: ss.supplierId
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(supName, fontWeight = FontWeight.Bold, color = DeepNavy)
                        Text(
                            "Currency: ${ss.currency} @ FX ${ss.exchangeRateToBdt} | Allocation: ${ss.chinaDeliveryAllocationMethod}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "China Delivery: ${ss.chinaDeliveryCurrency} ${ss.chinaDeliveryChargeMinor / 100.0}",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                        Text(
                            "BDT ৳${ss.chinaDeliveryBdtMinor / 100.0}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryTeal
                        )
                    }
                }
            }
        }

        // Link Additional Purchase Orders to this Shipment
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBangla) "একাধিক ক্রয়াদেশ সংযুক্ত করুন (Multi-PO Shipment)" else "Linked Purchase Orders (${shipmentOrders.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                    allPurchaseOrders.take(6).forEach { po ->
                        val isLinked = po.shipmentId == shipment.id || po.id == shipment.purchaseOrderId
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${po.poNumber} (${po.currency} ${po.totalAmountMinor / 100.0})", fontWeight = FontWeight.SemiBold)
                                Text("FX: ${po.exchangeRateToBdt} → ৳${po.totalBdtAmountMinor / 100.0}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            if (isLinked) {
                                Text("Linked", color = StatusSuccess, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            } else {
                                OutlinedButton(onClick = { onLinkPoToShipment(po.id) }) {
                                    Text("Attach PO", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackagingAndFreightTab(
    cartons: List<ShipmentCarton>,
    categories: List<ProductCategory>,
    freightRates: List<ShipmentFreightRate>,
    products: List<Product>,
    landingPreview: LandingCostEngine.ShipmentLandingCostResult?,
    isBangla: Boolean,
    onAddCartonClick: () -> Unit,
    onUpdateCategoryDefaultFreightRate: (categoryId: String, rateMinor: Long) -> Unit,
    onOverrideShipmentFreightRate: (categoryId: String, overrideRateMinor: Long?) -> Unit
) {
    val productMap = remember(products) { products.associateBy { it.id } }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Two-Level Category Freight Rate Management
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBangla) "২-স্তরের ক্যাটাগরি আন্তর্জাতিক ফ্রেইট রেট (Phase 2D)" else "Two-Level Category International Freight (Phase 2D)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                    Text(
                        text = "Category Freight = Total Category Gross Weight × Applicable Rate/KG (allocated to category products by Gross Weight)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    categories.forEach { cat ->
                        val shipmentRate = freightRates.find { it.categoryId == cat.id }
                        var defaultRateStr by remember(cat.defaultFreightRatePerKgBdtMinor) {
                            mutableStateOf((cat.defaultFreightRatePerKgBdtMinor / 100.0).toString())
                        }
                        var overrideRateStr by remember(shipmentRate?.overrideRatePerKgBdtMinor) {
                            mutableStateOf(shipmentRate?.overrideRatePerKgBdtMinor?.let { (it / 100.0).toString() } ?: "")
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${cat.nameEn} (${cat.nameBn})", fontWeight = FontWeight.Bold, color = DeepNavy)
                                    if (shipmentRate != null) {
                                        Text(
                                            "Weight: ${shipmentRate.totalCategoryGrossWeightKg} KG → Freight: ৳${shipmentRate.totalCategoryFreightBdtMinor / 100.0}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = PrimaryPurple,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = defaultRateStr,
                                        onValueChange = { defaultRateStr = it },
                                        label = { Text("Default Rate (BDT/KG)", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val minor = BigDecimal(defaultRateStr.trim().ifEmpty { "0" })
                                                .multiply(BigDecimal("100"))
                                                .setScale(0, RoundingMode.HALF_UP)
                                                .longValueExact()
                                            onUpdateCategoryDefaultFreightRate(cat.id, minor)
                                        }
                                    ) {
                                        Text("Save Default", fontSize = 11.sp)
                                    }
                                    OutlinedTextField(
                                        value = overrideRateStr,
                                        onValueChange = { overrideRateStr = it },
                                        label = { Text("Shipment Override", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = {
                                            val ovMinor = if (overrideRateStr.isBlank()) null else {
                                                BigDecimal(overrideRateStr.trim())
                                                    .multiply(BigDecimal("100"))
                                                    .setScale(0, RoundingMode.HALF_UP)
                                                    .longValueExact()
                                            }
                                            onOverrideShipmentFreightRate(cat.id, ovMinor)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                                    ) {
                                        Text("Apply", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Carton Packaging Summary & List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Carton & Packaging List (${cartons.size} rows)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
                Button(
                    onClick = onAddCartonClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Carton")
                }
            }
        }

        items(cartons) { c ->
            val prod = productMap[c.productId]
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${c.cartonNumber} • ${prod?.nameEn ?: c.productId}", fontWeight = FontWeight.Bold, color = DeepNavy)
                        Text(
                            "Category: ${c.categoryName ?: prod?.categoryId ?: "N/A"} | Qty: ${c.quantity} pcs",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${c.grossWeightKg} KG", fontWeight = FontWeight.Bold, color = PrimaryPurple)
                        Text("${c.cbm} CBM", style = MaterialTheme.typography.labelSmall, color = SecondaryTeal)
                    }
                }
            }
        }
    }
}

@Composable
private fun BdTransportAndExpensesTab(
    costItems: List<ImportCostItem>,
    products: List<Product>,
    cartons: List<ShipmentCarton>,
    isBangla: Boolean,
    onAddExpense: (
        category: CostCategory,
        description: String,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: AllocationMethod,
        docRef: String?,
        manualAllocationsBdtMinor: Map<String, Long>
    ) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(CostCategory.BD_LOCAL_TRANSPORT) }
    var description by remember { mutableStateOf("Bangladesh Port to Warehouse Local Transport") }
    var amountStr by remember { mutableStateOf("5000.00") }
    var currency by remember { mutableStateOf("BDT") }
    var exchangeRateStr by remember { mutableStateOf("1.00") }
    var selectedMethod by remember { mutableStateOf(AllocationMethod.GROSS_WEIGHT) }
    var docRef by remember { mutableStateOf("BD-TR-001") }
    val manualInputs = remember { mutableStateMapOf<String, String>() }
    var validationError by remember { mutableStateOf<String?>(null) }

    val shipmentProductIds = remember(cartons) { cartons.map { it.productId }.distinct() }
    val productMap = remember(products) { products.associateBy { it.id } }

    val allSixMethods = listOf(
        AllocationMethod.GROSS_WEIGHT,
        AllocationMethod.CBM,
        AllocationMethod.CARTON_COUNT,
        AllocationMethod.PURCHASE_VALUE,
        AllocationMethod.QUANTITY,
        AllocationMethod.MANUAL
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBangla) "বাংলাদেশ পরিবহন ও অতিরিক্ত আমদানি ব্যয় (Phase 2E & 2F)" else "Bangladesh Transport & Additional Import Expenses (Phase 2E & 2F)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )

                    // Category chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            CostCategory.BD_LOCAL_TRANSPORT,
                            CostCategory.CUSTOMS_DUTY,
                            CostCategory.CF_CHARGE,
                            CostCategory.PORT_CHARGE,
                            CostCategory.LOADING_UNLOADING,
                            CostCategory.OTHER_EXPENSES
                        ).forEach { cat ->
                            val active = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) PrimaryPurple else Color(0xFFF1F5F9))
                                    .clickable {
                                        selectedCategory = cat
                                        description = cat.titleEn
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat.titleEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) Color.White else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 6 Allocation Methods
                    Text(
                        text = "Select Expense Allocation Basis (6 Supported Methods):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allSixMethods.forEach { m ->
                            val active = selectedMethod == m
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) SecondaryTeal else Color(0xFFF1F5F9))
                                    .clickable { selectedMethod = m }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = m.titleEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) Color.White else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Expense Name") },
                            modifier = Modifier.weight(1.4f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = docRef,
                            onValueChange = { docRef = it },
                            label = { Text("Ref No.") },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text("Amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it.uppercase() },
                            label = { Text("Currency") },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = exchangeRateStr,
                            onValueChange = { exchangeRateStr = it },
                            label = { Text("FX Rate") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )
                    }

                    if (selectedMethod == AllocationMethod.MANUAL) {
                        Text(
                            text = "Manual Product-Wise Allocation (Sum must equal Expense BDT):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                        shipmentProductIds.forEach { pid ->
                            val pName = productMap[pid]?.nameEn ?: pid
                            OutlinedTextField(
                                value = manualInputs[pid] ?: "0.00",
                                onValueChange = { manualInputs[pid] = it },
                                label = { Text("$pName (BDT)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    if (validationError != null) {
                        Text(validationError!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            try {
                                validationError = null
                                val amtMinor = BigDecimal(amountStr.trim().ifEmpty { "0" })
                                    .multiply(BigDecimal("100"))
                                    .setScale(0, RoundingMode.HALF_UP)
                                    .longValueExact()
                                val fx = BigDecimal(exchangeRateStr.trim().ifEmpty { "1.00" })
                                val manualMap = if (selectedMethod == AllocationMethod.MANUAL) {
                                    shipmentProductIds.associateWith { pid ->
                                        BigDecimal((manualInputs[pid] ?: "0").trim().ifEmpty { "0" })
                                            .multiply(BigDecimal("100"))
                                            .setScale(0, RoundingMode.HALF_UP)
                                            .longValueExact()
                                    }
                                } else emptyMap()

                                onAddExpense(
                                    selectedCategory,
                                    description,
                                    amtMinor,
                                    currency,
                                    fx,
                                    selectedMethod,
                                    docRef.ifBlank { null },
                                    manualMap
                                )
                            } catch (e: Exception) {
                                validationError = e.message
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add & Allocate Expense")
                    }
                }
            }
        }

        items(costItems) { ci ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${ci.description} (${ci.costCategory})", fontWeight = FontWeight.Bold, color = DeepNavy)
                        Text(
                            "Basis: ${ci.allocationMethod} | Ref: ${ci.documentReference ?: "N/A"} | ${if (ci.isPostFinalization) "Post-Finalize" else "Pre-Finalize"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("৳${ci.bdtAmountMinor / 100.0}", fontWeight = FontWeight.Bold, color = PrimaryPurple)
                        Text(
                            if (ci.isCapitalized) "Allocated (${ci.costAdjustmentReference ?: "FINAL"})" else "Pending Allocation",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (ci.isCapitalized) StatusSuccess else Color(0xFFD97706)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LandingCostReviewAndFinalizeTab(
    landingPreview: LandingCostEngine.ShipmentLandingCostResult?,
    snapshots: List<LandedCostSnapshot>,
    costAdjustments: List<InventoryCostAdjustment>,
    uncapitalizedPostCosts: List<ImportCostItem>,
    isFinalized: Boolean,
    postFinalizeReason: String,
    onPostFinalizeReasonChange: (String) -> Unit,
    isBangla: Boolean,
    onFinalizeClick: () -> Unit,
    onApplyPostFinalizeAdjustment: () -> Unit,
    onReceiveClick: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBangla) "ল্যান্ডিং কস্ট সামারি ও পর্যালোচনা (Phase 4 & 5)" else "Landing Cost Summary & Review (Phase 4 & 5)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                    if (landingPreview != null) {
                        Text("1. Total Product Purchase Cost: ৳${landingPreview.totalPurchaseCostBdtMinor / 100.0}", style = MaterialTheme.typography.bodySmall)
                        Text("2. Total China Local Delivery: ৳${landingPreview.totalChinaDeliveryBdtMinor / 100.0}", style = MaterialTheme.typography.bodySmall)
                        Text("3. Total International Freight: ৳${landingPreview.totalInternationalFreightBdtMinor / 100.0}", style = MaterialTheme.typography.bodySmall)
                        Text("4. Total Bangladesh Local Transport: ৳${landingPreview.totalBdTransportBdtMinor / 100.0}", style = MaterialTheme.typography.bodySmall)
                        Text("5. Total Other Import Expenses: ৳${landingPreview.totalOtherImportExpensesBdtMinor / 100.0}", style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            "Grand Total Landing Cost: ৳${landingPreview.grandTotalLandingCostBdtMinor / 100.0}",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                        Text(
                            "Outcome Accounting Split → Good (1210): ৳${landingPreview.totalGoodInventoryAssetBdtMinor / 100.0} | Damaged (5050): ৳${landingPreview.totalDamagedWriteOffBdtMinor / 100.0} | Missing (1050): ৳${landingPreview.totalMissingShortageBdtMinor / 100.0} | Pending (1220): ৳${landingPreview.totalPendingInTransitBdtMinor / 100.0}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryTeal
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onFinalizeClick,
                            enabled = !isFinalized,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isFinalized) "Landing Cost Finalized" else "Finalize Landing Cost")
                        }
                        OutlinedButton(onClick = onReceiveClick) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Warehouse Receiving")
                        }
                    }
                }
            }
        }

        // Post-Finalization Cost Adjustment Action
        if (isFinalized && uncapitalizedPostCosts.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Post-Finalize Additional Expenses Pending (${uncapitalizedPostCosts.size})",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        OutlinedTextField(
                            value = postFinalizeReason,
                            onValueChange = onPostFinalizeReasonChange,
                            label = { Text("Cost Adjustment Reason") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = onApplyPostFinalizeAdjustment,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Cost Adjustment & Update FIFO / COGS")
                        }
                    }
                }
            }
        }

        // Product-wise Breakdown Cards
        val breakdowns = landingPreview?.productBreakdowns.orEmpty()
        items(breakdowns) { b ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${b.productNameEn} (Packed: ${b.packedQuantity})", fontWeight = FontWeight.Bold, color = DeepNavy)
                        Text(
                            "Unit Cost: ৳${b.unitImportCostBdtMinor / 100.0}",
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccess
                        )
                    }
                    Text(
                        "Purchase: ৳${b.purchaseCostBdtMinor / 100.0} | China Del: ৳${b.chinaDeliveryBdtMinor / 100.0} | Freight: ৳${b.internationalFreightBdtMinor / 100.0} | BD Trans: ৳${b.bdTransportBdtMinor / 100.0} | Other: ৳${b.otherImportExpensesBdtMinor / 100.0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        "Quantities → Good: ${b.goodQuantity} | Damaged: ${b.damagedQuantity} | Missing: ${b.missingQuantity} | Pending: ${b.pendingQuantity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryPurple
                    )
                }
            }
        }

        // Cost Adjustment History
        if (costAdjustments.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Post-Finalization Cost Adjustment History (${costAdjustments.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                }
            }
            items(costAdjustments) { adj ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${adj.adjustmentNumber} • ${adj.reason}", fontWeight = FontWeight.Bold, color = DeepNavy)
                        Text(
                            "Unit Cost: ৳${adj.oldUnitCostBdtMinor / 100.0} → ৳${adj.newUnitCostBdtMinor / 100.0} (+৳${adj.unitVarianceBdtMinor / 100.0})",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryPurple
                        )
                        Text(
                            "Remaining Qty (${adj.totalRemainingQty}) Inv Adj: ৳${adj.inventoryAdjustmentAmountMinor / 100.0} | Sold Qty (${adj.totalSoldQty}) COGS Adj: ৳${adj.cogsAdjustmentAmountMinor / 100.0}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryTeal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WarehouseRentTab(
    warehouseRentHistory: List<ExpenseVoucher>,
    isBangla: Boolean,
    onRecordWarehouseRent: (warehouseId: String, rentMonth: String, amountMinor: Long, notes: String?) -> Unit
) {
    var warehouseId by remember { mutableStateOf("wh-dhk-01") }
    var rentMonth by remember { mutableStateOf("2026-11") }
    var amountStr by remember { mutableStateOf("25000.00") }
    var notes by remember { mutableStateOf("Monthly warehouse rent (Operating Expense)") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBangla) "মাসিক গুদাম ভাড়া — পরিচালনা ব্যয় (Phase 2G)" else "Monthly Warehouse Rent — Operating Expense (Phase 2G)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                    Text(
                        text = "Recorded under Account 5110 (Operating Expense). Strictly excluded from Product Import Landing Cost.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = warehouseId,
                            onValueChange = { warehouseId = it },
                            label = { Text("Warehouse ID") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = rentMonth,
                            onValueChange = { rentMonth = it },
                            label = { Text("Month (YYYY-MM)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text("Rent (BDT)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val minor = BigDecimal(amountStr.trim().ifEmpty { "0" })
                                .multiply(BigDecimal("100"))
                                .setScale(0, RoundingMode.HALF_UP)
                                .longValueExact()
                            if (minor > 0L) {
                                onRecordWarehouseRent(warehouseId, rentMonth, minor, notes)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Record Monthly Warehouse Rent")
                    }
                }
            }
        }

        items(warehouseRentHistory) { v ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${v.voucherNumber} • ${v.descriptionEn}", fontWeight = FontWeight.Bold, color = DeepNavy)
                        Text("Warehouse: ${v.warehouseId ?: "N/A"} | Month: ${v.rentMonth ?: v.referenceNumber ?: "N/A"}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Text("৳${v.bdtAmountMinor / 100.0}", fontWeight = FontWeight.Bold, color = PrimaryPurple)
                }
            }
        }
    }
}
