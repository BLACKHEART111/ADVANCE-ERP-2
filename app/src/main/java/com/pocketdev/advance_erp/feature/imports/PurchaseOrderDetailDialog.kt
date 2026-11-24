package com.pocketdev.advance_erp.feature.imports

import com.pocketdev.advance_erp.core.designsystem.StatusSuccessContainer
import com.pocketdev.advance_erp.core.designsystem.StatusWarningContainer
import com.pocketdev.advance_erp.core.designsystem.StatusErrorContainer
import androidx.compose.runtime.saveable.rememberSaveable
import com.pocketdev.advance_erp.core.designsystem.ERPSurfaceVariant
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.PriceCheck
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.core.util.PoAttachmentStorage
import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.CostCategory
import com.pocketdev.advance_erp.data.entity.DamageRecord
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.LandedCostSnapshot
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.PurchaseCompletionRecord
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.core.engine.PoPackingEngine
import com.pocketdev.advance_erp.core.util.PoNotesMetadataParser
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.ShipmentFreightRate
import com.pocketdev.advance_erp.data.entity.ShortageRecord
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.SupplierPayment
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.domain.model.PaymentTermType
import com.pocketdev.advance_erp.domain.model.PaymentTermsCalculator
import com.pocketdev.advance_erp.domain.repository.PoFreightPaymentRecord
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun PurchaseOrderDetailDialog(
    order: PurchaseOrder,
    supplier: Supplier?,
    items: List<PurchaseOrderItem>,
    products: List<Product>,
    payments: List<SupplierPayment>,
    shipments: List<ImportShipment>,
    cartons: List<ShipmentCarton> = emptyList(),
    costItems: List<ImportCostItem> = emptyList(),
    receipts: List<WarehouseReceipt> = emptyList(),
    damageRecords: List<DamageRecord> = emptyList(),
    shortageRecords: List<ShortageRecord> = emptyList(),
    additionalCosts: List<AdditionalProductCost> = emptyList(),
    completionRecord: PurchaseCompletionRecord? = null,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirmPo: (orderId: String) -> Unit,
    onMakePayment: (order: PurchaseOrder) -> Unit,
    onCreateShipment: (order: PurchaseOrder) -> Unit,
    onAddCarton: (order: PurchaseOrder) -> Unit = {},
    onAddCostItem: (order: PurchaseOrder) -> Unit = {},
    onFinalizeActualCost: (orderId: String) -> Unit = {},
    onReceiveProducts: (order: PurchaseOrder) -> Unit = {},
    onPostReceiving: (receiptId: String) -> Unit = {},
    onAddAdditionalCost: (order: PurchaseOrder) -> Unit = {},
    onCompletePo: (order: PurchaseOrder) -> Unit = {},
    onSharePdf: (order: PurchaseOrder) -> Unit,
    onCancelPo: (orderId: String, reason: String?) -> Unit,
    onDeletePo: ((orderId: String) -> Unit)? = null,
    onReversePayment: ((paymentId: String) -> Unit)? = null,
    freightPayments: List<PoFreightPaymentRecord> = emptyList(),
    liquidAccounts: List<ChartOfAccount> = emptyList(),
    accountBalances: Map<String, Long> = emptyMap(),
    onRecordFreightPayment: ((orderId: String, paymentAccountId: String, amountBdtMinor: Long, paymentDate: Long, notes: String?) -> Unit)? = null,
    onReverseFreightPayment: ((journalEntryId: String) -> Unit)? = null,
    onDuplicatePo: ((orderId: String) -> Unit)? = null,
    onEditFullPo: ((order: PurchaseOrder) -> Unit)? = null,
    onSelectShipment: ((ImportShipment) -> Unit)? = null,
    onDeleteCarton: ((orderId: String, cartonNumber: String) -> Unit)? = null,
    onDeleteCostItem: ((costItemId: String) -> Unit)? = null,
    onUpdatePacking: ((cartons: Int, grossWeight: String, netWeight: String, cbm: String) -> Unit)? = null,
    onUpdatePoItem: ((itemId: String, quantity: Int, unitPriceMinor: Long, cartonQuantity: Int, grossWeightKg: String) -> Unit)? = null,
    onDeletePoItem: ((itemId: String) -> Unit)? = null,
    freightRates: List<ShipmentFreightRate> = emptyList(),
    categories: List<ProductCategory> = emptyList(),
    variants: List<ProductVariant> = emptyList(),
    snapshots: List<LandedCostSnapshot> = emptyList(),
    businessProfile: BusinessProfile? = null
) {
    var detailTab by rememberSaveable { mutableStateOf(0) }
    var showLifecycleActions by rememberSaveable { mutableStateOf(false) }
    val isBangla = language == AppLanguage.BANGLA
    val productMap = remember(products) { products.associateBy { it.id } }
    var showCancelConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showDeletePoConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var reversingPayment by remember { mutableStateOf<SupplierPayment?>(null) }
    var showFreightPaymentDialog by rememberSaveable { mutableStateOf(false) }
    var reversingFreightPayment by remember { mutableStateOf<PoFreightPaymentRecord?>(null) }
    var cancelReason by rememberSaveable { mutableStateOf("") }
    var showEditPackingDialog by rememberSaveable { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<PurchaseOrderItem?>(null) }
    var deletingItemId by remember { mutableStateOf<String?>(null) }
    var deletingCartonNumber by remember { mutableStateOf<String?>(null) }
    var deletingCostItemId by remember { mutableStateOf<String?>(null) }

    val paymentTerm = remember(order.notes) {
        val notes = order.notes ?: ""
        when {
            notes.contains("100% Advance", ignoreCase = true) -> PaymentTermType.HUNDRED_PERCENT_ADVANCE
            notes.contains("50% Advance", ignoreCase = true) -> PaymentTermType.FIFTY_FIFTY
            notes.contains("Full Due", ignoreCase = true) -> PaymentTermType.FULL_DUE
            notes.contains("Custom", ignoreCase = true) -> PaymentTermType.CUSTOM_PARTIAL
            else -> PaymentTermType.THIRTY_SEVENTY
        }
    }
    val rateBd = BigDecimal(order.exchangeRateToBdt.ifBlank { "17.00" })
    val scheduledInstallments = remember(paymentTerm, order.totalAmountMinor, rateBd) {
        PaymentTermsCalculator.calculateSchedule(paymentTerm, order.totalAmountMinor, rateBd)
    }

    val remainingBalanceMinor = (order.totalAmountMinor - order.paidAmountMinor).coerceAtLeast(0L)
    val remainingBalanceBdtMinor = (order.totalBdtAmountMinor - order.paidBdtAmountMinor).coerceAtLeast(0L)

    val distinctRecordedCartons = remember(cartons) { cartons.map { it.cartonNumber }.distinct() }
    val recordedCartonsCount = distinctRecordedCartons.size
    val displayCartons = if (recordedCartonsCount > 0) recordedCartonsCount else order.totalCartons

    val recordedGrossWeight = remember(cartons) {
        cartons.groupBy { it.cartonNumber }.values.fold(BigDecimal.ZERO) { acc, list ->
            val wStr = list.firstOrNull()?.cartonGrossWeightKg?.ifBlank { list.firstOrNull()?.grossWeightKg?.ifBlank { "0" } ?: "0" } ?: "0"
            acc.add(BigDecimal(wStr))
        }
    }
    val displayGrossWeight = if (recordedGrossWeight > BigDecimal.ZERO) {
        recordedGrossWeight.setScale(3, RoundingMode.HALF_UP).toPlainString()
    } else {
        BigDecimal(order.totalGrossWeightKg.ifBlank { "0" }).setScale(3, RoundingMode.HALF_UP).toPlainString()
    }

    val recordedNetWeight = remember(cartons) {
        cartons.fold(BigDecimal.ZERO) { acc, c ->
            acc.add(BigDecimal(c.netWeightKg.ifBlank { "0" }))
        }
    }
    val displayNetWeight = if (recordedNetWeight > BigDecimal.ZERO) {
        recordedNetWeight.setScale(3, RoundingMode.HALF_UP).toPlainString()
    } else {
        BigDecimal(order.totalNetWeightKg.ifBlank { "0" }).setScale(3, RoundingMode.HALF_UP).toPlainString()
    }

    val recordedCbm = remember(cartons) {
        cartons.groupBy { it.cartonNumber }.values.fold(BigDecimal.ZERO) { acc, list ->
            acc.add(BigDecimal(list.firstOrNull()?.cbm?.ifBlank { "0" } ?: "0"))
        }
    }
    val displayCbmBd = if (recordedCbm > BigDecimal.ZERO) {
        recordedCbm
    } else {
        BigDecimal(order.totalCbm.ifBlank { "0" })
    }
    val displayCbmFormatted = displayCbmBd.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    val totalOrderedQuantity = items.sumOf { it.quantity }
    val totalReceivedQuantity = items.sumOf { it.receivedQuantity }
    val receivingProgress = if (totalOrderedQuantity > 0) {
        (totalReceivedQuantity.toFloat() / totalOrderedQuantity.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val paymentProgress = if (order.totalAmountMinor > 0) {
        (order.paidAmountMinor.toFloat() / order.totalAmountMinor.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val canCancel = order.status != PurchaseOrderStatus.CANCELLED.name &&
            payments.isEmpty() &&
            shipments.all { it.shipmentNumber.startsWith("BATCH-") && it.status == "DRAFT" } &&
            receipts.isEmpty()

    val isOrderReceived = !PoPackingEngine.canDeletePurchaseOrder(
        status = order.status,
        itemsReceivedQuantities = items.map { it.receivedQuantity },
        hasPostedWarehouseReceipt = receipts.any { it.status == "POSTED" },
        completionReceivedQuantity = completionRecord?.receivedQuantity ?: 0
    )
    val canDeletePo = onDeletePo != null && !isOrderReceived

    val isCostFinalized = order.isActualCostFinalized || order.status == PurchaseOrderStatus.ACTUAL_COST_FINALIZED.name
    var collapsedDetailProductIds by remember(order.id) { mutableStateOf(emptySet<String>()) }
    val landedSummary = remember(
        order,
        items,
        cartons,
        costItems,
        additionalCosts,
        freightRates,
        categories,
        products,
        variants,
        snapshots,
        isBangla,
        isCostFinalized
    ) {
        calculatePoLandedCostSummary(
            order = order,
            items = items,
            cartons = cartons,
            costItems = costItems,
            additionalCosts = additionalCosts,
            freightRates = freightRates,
            categories = categories,
            products = products,
            variants = variants,
            snapshots = snapshots,
            isBangla = isBangla,
            isCostFinalized = isCostFinalized
        )
    }
    val freightPayableSummary = remember(landedSummary, costItems, additionalCosts, freightPayments) {
        calculatePoFreightPayableSummary(
            landedSummary = landedSummary,
            costItems = costItems,
            additionalCosts = additionalCosts,
            paidFreightBillMinor = freightPayments.sumOf { it.amountBdtMinor }
        )
    }

    val navigateTab=com.pocketdev.advance_erp.core.designsystem.LocalERPOnNavigate.current
    PoReferenceTheme {
    com.pocketdev.advance_erp.core.designsystem.ERPFullScreenEditor(onDismissRequest=onDismiss) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.padding(horizontal=16.dp)) {
                    PoTopBar(if(isBangla) "PO বিস্তারিত" else "PO details",onBack=onDismiss,actions={
                        if(onEditFullPo!=null && order.status !in setOf("COMPLETED","CANCELLED")) IconButton(onClick={onEditFullPo(order)}) { Icon(Icons.Outlined.Edit,if(isBangla) "সম্পাদনা" else "Edit") }
                        IconButton(onClick={onSharePdf(order)}) { Icon(Icons.Outlined.Print,if(isBangla) "PDF প্রিন্ট / শেয়ার" else "Print / share PDF") }
                        if (canDeletePo) {
                            IconButton(onClick = { showDeletePoConfirmDialog = true }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = if (isBangla) "PO ডিলিট করুন" else "Delete PO",
                                    tint = StatusError
                                )
                            }
                        }
                        IconButton(onClick={showLifecycleActions=!showLifecycleActions}) { Icon(Icons.Outlined.MoreVert,if(isBangla) "আরও কার্যক্রম" else "More actions") }
                    })
                }
                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Surface(shape=RoundedCornerShape(19.dp),color=Color.Transparent) {
                            Column(Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF9D28BD),Color(0xFF4B1974),Color(0xFF008F95)))).padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                    Surface(shape=RoundedCornerShape(12.dp),color=Color.White.copy(alpha=.15f)) {Icon(Icons.Outlined.ViewInAr,null,Modifier.padding(9.dp).size(28.dp),tint=Color.White)}
                                    Column(Modifier.weight(1f)) {Text(order.poNumber,color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium);Text(supplier?.companyName.orEmpty(),color=Color.White,style=MaterialTheme.typography.bodySmall)}
                                    Surface(shape=RoundedCornerShape(20.dp),color=when(order.status){"DRAFT"->Color(0xFF9B6A18);"CANCELLED"->Color(0xFFB74444);"PARTIALLY_PAID"->Color(0xFF8742AA);else->Color(0xFF2C9B61)}) { Text(com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus.entries.find {it.name==order.status}?.let {if(isBangla)it.labelBn else it.labelEn} ?: order.status,Modifier.padding(horizontal=9.dp,vertical=4.dp),color=Color.White,style=MaterialTheme.typography.labelSmall) }
                                }
                                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                    PoHeroMetadata(if(isBangla) "PO তারিখ" else "PO date",Formatters.formatDate(order.orderDate,language),Icons.Outlined.CalendarMonth,Modifier.weight(1f))
                                    PoHeroMetadata(if(isBangla) "প্রত্যাশিত আগমন" else "Expected arrival",order.expectedArrivalDate?.let {Formatters.formatDate(it,language)} ?: "—",Icons.Outlined.LocalShipping,Modifier.weight(1f))
                                }
                                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                    PoHeroMetadata(if(isBangla) "মুদ্রা" else "Currency",order.currency,Icons.Outlined.Payments,Modifier.weight(1f))
                                    PoHeroMetadata(if(isBangla) "পেমেন্ট শর্ত" else "Payment terms",if(isBangla)paymentTerm.labelBn else paymentTerm.labelEn,Icons.Outlined.Handshake,Modifier.weight(1f))
                                }

                            }
                        }
                    }
                    item {
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            val tabs=if(isBangla)listOf("সাধারণ","পণ্য (${items.size})","প্যাকিং ($displayCartons)","খরচ","পেমেন্ট")else listOf("General","Products (${items.size})","Packing ($displayCartons)","Costs","Payments")
                            items(tabs.size) { index -> androidx.compose.material3.FilterChip(selected=detailTab==index,onClick={detailTab=index},label={Text(tabs[index])}) }
                        }
                    }
                    if(detailTab==0) item {
                        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            PremiumPoCostLine(if(isBangla) "পণ্যের মোট ক্রয়মূল্য" else "Purchase value","${order.currency} ${Formatters.formatMinorUnit(order.totalAmountMinor)}")
                            costItems.groupBy { it.costCategory }.forEach { (category,costs) ->
                                val cat=runCatching {com.pocketdev.advance_erp.data.entity.CostCategory.valueOf(category)}.getOrNull()
                                PremiumPoCostLine(cat?.let {if(isBangla)it.titleBn else it.titleEn} ?: category,"৳ ${Formatters.formatMinorUnit(costs.sumOf {it.bdtAmountMinor})}")
                            }
                            additionalCosts.filter { it.isCapitalizable && it.bdtAmountMinor > 0L }.forEach { ac ->
                                PremiumPoCostLine(
                                    ac.description.ifBlank { ac.category.replace("_", " ") },
                                    "৳ ${Formatters.formatMinorUnit(ac.bdtAmountMinor)}"
                                )
                            }
                            val hasAnyFreightCostItem = costItems.any {
                                it.costCategory in setOf(
                                    CostCategory.INTERNATIONAL_FREIGHT.name,
                                    CostCategory.AIR_FREIGHT.name,
                                    CostCategory.SEA_FREIGHT.name
                                )
                            }
                            val displayRateFreightMinor = landedSummary.derivedRateFreightMinor.takeIf { it > 0L }
                                ?: if (!hasAnyFreightCostItem) landedSummary.totalFreightBdtMinor else 0L
                            if (!hasAnyFreightCostItem && displayRateFreightMinor > 0L) {
                                PremiumPoCostLine(
                                    if (isBangla) "আন্তর্জাতিক ফ্রেইট ($displayGrossWeight KG)" else "International Freight ($displayGrossWeight KG)",
                                    "৳ ${Formatters.formatMinorUnit(displayRateFreightMinor)}"
                                )
                            }
                            PoLandedHighlight(
                                total = "৳ ${Formatters.formatMinorUnit(landedSummary.totalLandedBdtMinor)}",
                                unit = landedSummary.overallUnitLandedBdt?.let {
                                    (if (isBangla) "সর্বমোট গড় প্রতি পিস: " else "Overall average per unit: ") + "৳ $it / PCS"
                                },
                                isBangla = isBangla,
                                categoryBreakdowns = landedSummary.categoryBreakdowns,
                                productBreakdowns = landedSummary.productBreakdowns
                            )
                            Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                                if(remainingBalanceMinor>0 && order.status!="CANCELLED") PoDetailAction(if(isBangla) "পেমেন্ট রেকর্ড" else "Payment",Icons.Outlined.Payment,Modifier.weight(1f)){onMakePayment(order)}
                                if(order.status !in setOf("DRAFT","CANCELLED","COMPLETED")) PoDetailAction(if(isBangla) "রিসিভ করুন" else "Receive",Icons.Outlined.LocalShipping,Modifier.weight(1f)){onReceiveProducts(order)}
                                PoDetailAction(if(isBangla) "PDF প্রিন্ট/শেয়ার" else "PDF / Share",Icons.Outlined.PictureAsPdf,Modifier.weight(1f)){onSharePdf(order)}
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showLifecycleActions = !showLifecycleActions }) {
                                    Text(if (isBangla) "অর্ডারের সব কার্যক্রম" else "All order operations")
                                }
                                if (canDeletePo) {
                                    OutlinedButton(
                                        onClick = { showDeletePoConfirmDialog = true },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isBangla) "PO ডিলিট করুন" else "Delete PO",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                } else if (onDeletePo != null && isOrderReceived) {
                                    Text(
                                        text = if (isBangla) "পণ্য রিসিভ হওয়ায় এই PO ডিলিট করা যাবে না" else "Received PO cannot be deleted",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                    if(detailTab==0) {
                    // 1. Completion Banner (if completed)
                    if (completionRecord != null || order.status == PurchaseOrderStatus.COMPLETED.name) {
                        item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (onSelectShipment != null && shipments.isNotEmpty()) {
                                OutlinedButton(onClick = { onSelectShipment(shipments.first()) }, modifier = Modifier.weight(1f)) {
                                    Text(if (isBangla) "শিপমেন্ট খুলুন" else "Open shipment")
                                }
                            }
                            if (order.status !in listOf(PurchaseOrderStatus.DRAFT.name, PurchaseOrderStatus.CANCELLED.name)) {
                                OutlinedButton(onClick = { onCreateShipment(order) }, modifier = Modifier.weight(1f)) {
                                    Text(if (isBangla) "নতুন শিপমেন্ট" else "New shipment")
                                }
                            }
                        }
                    }
                    item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = StatusSuccessContainer),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.TaskAlt, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isBangla) "ক্রয়াদেশ সম্পূর্ণ করা হয়েছে" else "Purchase Order Completed",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        if (completionRecord != null) {
                                            Text(
                                                text = if (isBangla) {
                                                    "সমাপ্তির তারিখ: ${Formatters.formatDate(completionRecord.completedAt, language)} • নিষ্পত্তি: ${completionRecord.unreceivedDisposition ?: "নেই"}"
                                                } else {
                                                    "Completed on ${Formatters.formatDate(completionRecord.completedAt, language)} • Disposition: ${completionRecord.unreceivedDisposition ?: "None"}"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                            if (!completionRecord.notes.isNullOrBlank()) {
                                                Text(
                                                    text = if (isBangla) "অডিট নোট: ${completionRecord.notes}" else "Audit Note: ${completionRecord.notes}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    }
                    if(detailTab==0 && showLifecycleActions) {
                    // 2. Supplier & FX Profile Card
                    item {
                        ERPCard {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isBangla) "সরবরাহকারী ও বিনিময় হার" else "Supplier & Foreign Exchange",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = supplier?.companyName ?: order.supplierId,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${supplier?.country ?: "China"} • ${supplier?.phone ?: "N/A"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "1 ${order.currency} = ${order.exchangeRateToBdt} BDT",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryPurple
                                        )
                                        Text(
                                            text = Formatters.formatDate(order.orderDate, language),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                    }
                                }

                                if (!order.notes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = ERPDivider)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${if (isBangla) "মন্তব্য: " else "Notes: "}${order.notes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }

                                if (!order.attachments.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = ERPDivider)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isBangla) "সংযুক্ত ইনভয়েস / ডকুমেন্টস:" else "Attached Documents / Invoices:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val files = PoAttachmentStorage.parseAttachments(order.attachments)
                                    PoAttachmentCards(
                                        attachments = files,
                                        isBangla = isBangla
                                    )
                                }
                            }
                        }
                    }

                    }
                    if(detailTab==4) {
                    // 3. Financial & Payment Summary Card with Installment Schedule
                    item {
                        ERPCard {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isBangla) "সরবরাহকারী পেমেন্ট (পণ্য মূল্য • ২০১০)" else "Supplier Payment (Goods • 2010)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${(paymentProgress * 100).toInt()}% ${if (isBangla) "পরিশোধিত" else "Paid"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (paymentProgress >= 1f) StatusSuccess else PrimaryPurple
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { paymentProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (paymentProgress >= 1f) StatusSuccess else PrimaryPurple,
                                    trackColor = ERPBackground
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Total PO Value
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBangla) "মোট মূল্য" else "Total PO Value",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${order.currency} ${Formatters.formatMinorUnit(order.totalAmountMinor)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = Formatters.formatMoney(order.totalBdtAmountMinor, CurrencyCode.BDT, language),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Total Paid
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (isBangla) "পরিশোধিত" else "Total Paid",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${order.currency} ${Formatters.formatMinorUnit(order.paidAmountMinor)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusSuccess,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = Formatters.formatMoney(order.paidBdtAmountMinor, CurrencyCode.BDT, language),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = StatusSuccess,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Balance Due
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = if (isBangla) "বকেয়া" else "Balance Due",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${order.currency} ${Formatters.formatMinorUnit(remainingBalanceMinor)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (remainingBalanceMinor > 0) StatusError else StatusSuccess,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = Formatters.formatMoney(remainingBalanceBdtMinor, CurrencyCode.BDT, language),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (remainingBalanceBdtMinor > 0) StatusError else StatusSuccess,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (remainingBalanceMinor > 0L && order.status !in setOf(PurchaseOrderStatus.DRAFT.name, PurchaseOrderStatus.CANCELLED.name)) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { onMakePayment(order) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Outlined.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBangla) "সাপ্লায়ার পেমেন্ট দিন" else "Pay Supplier",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Scheduled Payment Terms Breakdown
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = ERPDivider)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${if (isBangla) "পেমেন্ট শর্ত: " else "Payment Terms: "}${if (isBangla) paymentTerm.labelBn else paymentTerm.labelEn}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryTeal
                                )
                                scheduledInstallments.forEach { inst ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isBangla) inst.titleBn else inst.titleEn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "${order.currency} ${Formatters.formatMinorUnit(inst.amountMinor)} (${Formatters.formatMoney(inst.bdtAmountMinor, CurrencyCode.BDT, language)})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    }
                                }

                                // Linked payments list if any
                                if (payments.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = ERPDivider)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isBangla) "পরিশোধের ইতিহাস (${payments.size}):" else "Payment History (${payments.size}):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    payments.forEach { p ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${p.paymentNumber} • ${p.paymentMethod}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = Formatters.formatDate(p.paymentDate, language),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${p.currency} ${Formatters.formatMinorUnit(p.amountMinor)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusSuccess
                                                )
                                                Text(
                                                    text = Formatters.formatMoney(p.bdtAmountMinor, CurrencyCode.BDT, language),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                            if (onReversePayment != null) {
                                                TextButton(
                                                    onClick = { reversingPayment = p },
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (isBangla) "রিভার্স" else "Reverse",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = StatusError
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (freightPayableSummary.totalFreightBillMinor > 0L || freightPayments.isNotEmpty()) {
                        item {
                            ERPCard {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isBangla) "ফ্রেইট ফরোয়ার্ডার ও শিপিং পেমেন্ট (২০২০)" else "Freight Forwarder & Shipping Payment (2020)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        )
                                        Text(
                                            text = "${(freightPayableSummary.progress * 100).toInt()}% ${if (isBangla) "পরিশোধিত" else "Paid"}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (freightPayableSummary.progress >= 1f) StatusSuccess else SecondaryTeal
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { freightPayableSummary.progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = if (freightPayableSummary.progress >= 1f) StatusSuccess else SecondaryTeal,
                                        trackColor = ERPBackground
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isBangla) "মোট ফ্রেইট বিল" else "Total Freight Bill",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = Formatters.formatMoney(freightPayableSummary.totalFreightBillMinor, CurrencyCode.BDT, language),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = if (isBangla) "পরিশোধিত" else "Paid",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = Formatters.formatMoney(freightPayableSummary.paidFreightBillMinor, CurrencyCode.BDT, language),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusSuccess,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = if (isBangla) "বকেয়া" else "Due",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = Formatters.formatMoney(freightPayableSummary.remainingFreightBillMinor, CurrencyCode.BDT, language),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (freightPayableSummary.remainingFreightBillMinor > 0L) StatusError else StatusSuccess,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (onRecordFreightPayment != null &&
                                        freightPayableSummary.remainingFreightBillMinor > 0L &&
                                        order.status !in setOf(PurchaseOrderStatus.DRAFT.name, PurchaseOrderStatus.CANCELLED.name)
                                    ) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = { showFreightPaymentDialog = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Outlined.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isBangla) "ফ্রেইট ফরোয়ার্ডার পেমেন্ট রেকর্ড করুন" else "Pay Freight Forwarder",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (freightPayments.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = ERPDivider)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (isBangla) "ফ্রেইট পেমেন্ট ইতিহাস (${freightPayments.size}):" else "Freight Payment History (${freightPayments.size}):",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary
                                        )
                                        freightPayments.forEach { fp ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${fp.entryNumber} • ${fp.paymentAccountName.ifBlank { "Cash / Bank" }}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium,
                                                        color = TextPrimary
                                                    )
                                                    val dateLine = Formatters.formatDate(fp.paymentDate, language) +
                                                        (fp.notes?.let { " • $it" } ?: "")
                                                    Text(
                                                        text = dateLine,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextMuted
                                                    )
                                                }
                                                Text(
                                                    text = Formatters.formatMoney(fp.amountBdtMinor, CurrencyCode.BDT, language),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusSuccess
                                                )
                                                if (onReverseFreightPayment != null) {
                                                    TextButton(
                                                        onClick = { reversingFreightPayment = fp },
                                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isBangla) "রিভার্স" else "Reverse",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = StatusError
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    }
                    if(detailTab==1) {
                    // 4. Product-grouped Line Items Card with Product & Variant Landed Cost
                    item {
                        val itemsById = remember(items) { items.associateBy { it.id } }
                        val prodBreakdowns = landedSummary.productBreakdowns
                        val hasExpandableProducts = prodBreakdowns.any { it.itemBreakdowns.size > 1 }
                        val allProductsCollapsed = hasExpandableProducts && prodBreakdowns
                            .filter { it.itemBreakdowns.size > 1 }
                            .all { it.productId in collapsedDetailProductIds }

                        ERPCard {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBangla) {
                                                "পণ্য ও ল্যান্ডিং কস্ট (${prodBreakdowns.size} পণ্য • ${items.size} আইটেম)"
                                            } else {
                                                "Products & Landed Cost (${prodBreakdowns.size} Products • ${items.size} Items)"
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (isBangla) {
                                                if (isCostFinalized) "চূড়ান্ত ল্যান্ডেড কস্ট অনুযায়ী হিসাবকৃত" else "বর্তমান ফ্রেইট ও খরচ অনুযায়ী প্রাক্কলিত ল্যান্ডেড কস্ট"
                                            } else {
                                                if (isCostFinalized) "Based on finalized actual landed cost" else "Estimated from current freight & import charges"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    if (hasExpandableProducts) {
                                        TextButton(
                                            onClick = {
                                                collapsedDetailProductIds = if (allProductsCollapsed) {
                                                    emptySet()
                                                } else {
                                                    prodBreakdowns.filter { it.itemBreakdowns.size > 1 }.map { it.productId }.toSet()
                                                }
                                            },
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (allProductsCollapsed) {
                                                    if (isBangla) "সব দেখুন" else "Expand All"
                                                } else {
                                                    if (isBangla) "সব মিনিমাইজ" else "Collapse All"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryPurple
                                            )
                                        }
                                    }
                                }

                                prodBreakdowns.forEachIndexed { prodIdx, prod ->
                                    val isCollapsed = prod.productId in collapsedDetailProductIds
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = ERPSurfaceVariant.copy(alpha = 0.65f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ERPDivider)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Product Header & Product-wise Landed Cost
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                    Text(
                                                        text = "${prodIdx + 1}. ${prod.productTitle}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                    val wtStr = prod.grossWeightKg.setScale(2, RoundingMode.HALF_UP).toPlainString()
                                                    Text(
                                                        text = buildString {
                                                            append("SKU: ${prod.baseSku.ifBlank { "N/A" }} • ${prod.totalPcs} PCS • $wtStr KG")
                                                            if (prod.totalCartons > 0) append(" • ${prod.totalCartons} CTN")
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextSecondary
                                                    )
                                                    if (prod.freightBdtMinor > 0L || prod.otherOverheadBdtMinor > 0L) {
                                                        Text(
                                                            text = buildString {
                                                                append(if (isBangla) "ক্রয়: ৳ " else "Buy: ৳ ")
                                                                append(Formatters.formatMinorUnit(prod.purchaseBdtMinor))
                                                                if (prod.freightBdtMinor > 0L) {
                                                                    append(if (isBangla) " + ফ্রেইট: ৳ " else " + Freight: ৳ ")
                                                                    append(Formatters.formatMinorUnit(prod.freightBdtMinor))
                                                                }
                                                                if (prod.otherOverheadBdtMinor > 0L) {
                                                                    append(if (isBangla) " + খরচ: ৳ " else " + Costs: ৳ ")
                                                                    append(Formatters.formatMinorUnit(prod.otherOverheadBdtMinor))
                                                                }
                                                            },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextMuted
                                                        )
                                                    }
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (isCostFinalized) StatusSuccessContainer else PrimaryPurple.copy(alpha = 0.12f)
                                                    ) {
                                                        Text(
                                                            text = if (isBangla) {
                                                                if (isCostFinalized) "চূড়ান্ত" else "প্রাক্কলিত"
                                                            } else {
                                                                if (isCostFinalized) "Final" else "Est."
                                                            },
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isCostFinalized) StatusSuccess else PrimaryPurple
                                                        )
                                                    }
                                                    if (prod.itemBreakdowns.size > 1) {
                                                        IconButton(
                                                            onClick = {
                                                                collapsedDetailProductIds = if (isCollapsed) {
                                                                    collapsedDetailProductIds - prod.productId
                                                                } else {
                                                                    collapsedDetailProductIds + prod.productId
                                                                }
                                                            },
                                                            modifier = Modifier.size(26.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isCollapsed) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                                                                contentDescription = null,
                                                                tint = TextSecondary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Product Total & Per-Piece Landed Cost Bar
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.35f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Text(
                                                            text = if (isBangla) "মোট ল্যান্ডিং কস্ট" else "Product Landed Cost",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextMuted
                                                        )
                                                        Text(
                                                            text = "৳ ${Formatters.formatMinorUnit(prod.totalLandedBdtMinor)}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary
                                                        )
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = if (isBangla) "প্রতি পিস ল্যান্ডিং কস্ট" else "Per-Unit Landed Cost",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextMuted
                                                        )
                                                        Text(
                                                            text = if (isBangla) "৳ ${prod.unitLandedBdt} / পিস" else "৳ ${prod.unitLandedBdt} / PCS",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = SecondaryTeal
                                                        )
                                                    }
                                                }
                                            }

                                            if (!isCollapsed) {
                                                HorizontalDivider(color = ERPDivider)
                                                prod.itemBreakdowns.forEachIndexed { vIdx, itemBreakdown ->
                                                    val rawItem = itemsById[itemBreakdown.itemId]
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 3.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                            val lineTitle = when {
                                                                itemBreakdown.variantLabel.isNotBlank() ->
                                                                    "${vIdx + 1}. ${itemBreakdown.variantLabel}"
                                                                prod.itemBreakdowns.size > 1 ->
                                                                    if (isBangla) "লাইন ${vIdx + 1}" else "Line ${vIdx + 1}"
                                                                else ->
                                                                    itemBreakdown.productTitle
                                                            }
                                                            Text(
                                                                text = lineTitle,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = TextPrimary
                                                            )
                                                            if (rawItem != null) {
                                                                Text(
                                                                    text = "SKU: ${itemBreakdown.sku.ifBlank { "N/A" }} • ${rawItem.quantity} units @ ${order.currency} ${Formatters.formatMinorUnit(rawItem.unitPriceMinor)}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = TextSecondary
                                                                )
                                                            }
                                                            if (itemBreakdown.cartonCount > 0 || itemBreakdown.grossWeightKg > BigDecimal.ZERO) {
                                                                Text(
                                                                    text = "Packing: ${itemBreakdown.cartonCount} Cartons • ${itemBreakdown.grossWeightKg} KG Gross",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = SecondaryTeal
                                                                )
                                                            }
                                                            Text(
                                                                text = if (isBangla) {
                                                                    "ল্যান্ডিং কস্ট: ৳ ${itemBreakdown.unitLandedBdt}/পিস • মোট ল্যান্ডেড: ৳ ${Formatters.formatMinorUnit(itemBreakdown.totalLandedBdtMinor)}"
                                                                } else {
                                                                    "Landed: ৳ ${itemBreakdown.unitLandedBdt}/pcs • Total: ৳ ${Formatters.formatMinorUnit(itemBreakdown.totalLandedBdtMinor)}"
                                                                },
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = PrimaryPurple
                                                            )
                                                        }
                                                        if (rawItem != null) {
                                                            Column(horizontalAlignment = Alignment.End) {
                                                                Text(
                                                                    text = "${order.currency} ${Formatters.formatMinorUnit(rawItem.totalAmountMinor)}",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = PrimaryPurple
                                                                )
                                                                Text(
                                                                    text = Formatters.formatMoney(rawItem.totalBdtAmountMinor, CurrencyCode.BDT, language),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = TextMuted
                                                                )
                                                                if (order.status == PurchaseOrderStatus.DRAFT.name || order.status == PurchaseOrderStatus.CONFIRMED.name) {
                                                                    Row(
                                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        modifier = Modifier.padding(top = 2.dp)
                                                                    ) {
                                                                        IconButton(
                                                                            onClick = { editingItem = rawItem },
                                                                            modifier = Modifier.size(24.dp)
                                                                        ) {
                                                                            Icon(
                                                                                imageVector = Icons.Outlined.Edit,
                                                                                contentDescription = "Edit Item",
                                                                                tint = PrimaryPurple,
                                                                                modifier = Modifier.size(15.dp)
                                                                            )
                                                                        }
                                                                        IconButton(
                                                                            onClick = { deletingItemId = rawItem.id },
                                                                            modifier = Modifier.size(24.dp)
                                                                        ) {
                                                                            Icon(
                                                                                imageVector = Icons.Outlined.Delete,
                                                                                contentDescription = "Delete Item",
                                                                                tint = StatusError,
                                                                                modifier = Modifier.size(15.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (vIdx < prod.itemBreakdowns.lastIndex) {
                                                        HorizontalDivider(color = ERPDivider.copy(alpha = 0.6f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    }
                    if(detailTab==2) {
                    // 5. Carton & Packing Management Card (Single + Mixed, Auto Weight Allocation)
                    item {
                        ERPCard {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBangla) "কার্টন ও প্যাকিং ব্যবস্থাপনা" else "Carton & Packing Management",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (isBangla) "সিঙ্গেল ও মিক্সড কার্টন • স্বয়ংক্রিয় ওজন বিভাজন" else "Single & Mixed Cartons • Auto Weight Allocation",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (order.status != PurchaseOrderStatus.COMPLETED.name && order.status != PurchaseOrderStatus.CANCELLED.name) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = { showEditPackingDialog = true },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isBangla) "প্যাকিং এডিট" else "Edit Packing",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            Button(
                                                onClick = { onAddCarton(order) },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isBangla) "+ কার্টন" else "+ Carton",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Packing Summary 2x2 Responsive Grid
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ERPSurfaceVariant, RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PackingMetricBox(
                                            label = if (isBangla) "মোট কার্টন" else "Total Cartons",
                                            primaryValue = "${displayCartons} CTN",
                                            subtitle = if (order.totalCartons > 0 && order.totalCartons != displayCartons) {
                                                "${recordedCartonsCount} of ${order.totalCartons} Planned"
                                            } else if (recordedCartonsCount > 0) {
                                                "$recordedCartonsCount Entered"
                                            } else "Planned Order Count",
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        PackingMetricBox(
                                            label = if (isBangla) "গ্রস ওজন" else "Gross Weight",
                                            primaryValue = "${displayGrossWeight} KG",
                                            subtitle = "Shipping Basis",
                                            color = PrimaryPurple,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PackingMetricBox(
                                            label = if (isBangla) "নেট ওজন" else "Net Weight",
                                            primaryValue = "${displayNetWeight} KG",
                                            subtitle = "Product Mass",
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        PackingMetricBox(
                                            label = if (isBangla) "মোট সিবিএম" else "Total CBM",
                                            primaryValue = "${displayCbmFormatted} m³",
                                            subtitle = "Calculated Volume",
                                            color = SecondaryTeal,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (cartons.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val parsedNotesMeta = remember(order.notes) { PoNotesMetadataParser.parse(order.notes, defaultForwarder = "") }
                                    val defaultMarkText = parsedNotesMeta.defaultShippingMark.ifBlank {
                                        PoNotesMetadataParser.buildShippingMark(businessProfile?.companyName, order.poNumber, order.currency == "BDT")
                                    }
                                    val groupedCartons = remember(cartons) {
                                        cartons.groupBy { it.cartonNumber }
                                            .entries
                                            .sortedWith(compareBy<Map.Entry<String, List<ShipmentCarton>>> { PoPackingEngine.cartonSortKey(it.key) }.thenBy { it.key })
                                    }
                                    val variantMap = remember(variants) { variants.associateBy { it.id } }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isBangla) "যুক্তকৃত কার্টন (${groupedCartons.size} CTN • ${cartons.size} লাইন):" else "Entered Cartons (${groupedCartons.size} CTN • ${cartons.size} allocations):",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary
                                        )
                                        TextButton(onClick = { onSharePdf(order) }) {
                                            Text(
                                                text = if (isBangla) "CTN লেবেল ও এক্সপোর্ট" else "CTN Labels & Export",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryPurple
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        groupedCartons.forEachIndexed { idx, (rawCtnNo, cLines) ->
                                            val displayCtnNo = PoPackingEngine.formatCartonNumber(idx + 1)
                                            val customMark = parsedNotesMeta.cartonShippingMarks[rawCtnNo]
                                                ?: parsedNotesMeta.cartonShippingMarks[displayCtnNo]
                                            val resolvedMark = PoPackingEngine.resolveShippingMark(defaultMarkText, customMark, displayCtnNo)
                                            val lineSummaries = cLines.map { row ->
                                                val p = productMap[row.productId]
                                                val v = variantMap[row.variantId]
                                                val pName = if (isBangla && !p?.nameBn.isNullOrBlank()) p?.nameBn!! else p?.nameEn ?: row.productId
                                                val vSpec = v?.let { listOfNotNull(it.color, it.size).joinToString("/") }.orEmpty()
                                                val label = if (vSpec.isNotBlank()) "$pName ($vSpec)" else pName
                                                "$label × ${row.quantity} pcs"
                                            }
                                            GroupedCartonCard(
                                                displayNumber = displayCtnNo,
                                                cartonRows = cLines,
                                                lineSummaries = lineSummaries,
                                                shippingMark = resolvedMark,
                                                isCustomMark = !customMark.isNullOrBlank(),
                                                onDelete = if (order.status != PurchaseOrderStatus.COMPLETED.name && order.status != PurchaseOrderStatus.CANCELLED.name) {
                                                    { deletingCartonNumber = rawCtnNo }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    }
                    if(detailTab==3) {
                    // 6. Landing Cost & Gross Weight Shipping Card
                    item {
                        ERPCard {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBangla) "ল্যান্ডেড কস্ট ও শিপিং ক্যালকুলেশন" else "Landed Cost & Shipping Calculation",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (isBangla) "গ্রস ওয়েট ভিত্তিক শিপিং • ক্যাটাগরিভিত্তিক ব্যয় বিভাজন" else "Gross Weight Shipping • Category Allocation",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (order.status != PurchaseOrderStatus.COMPLETED.name && order.status != PurchaseOrderStatus.CANCELLED.name) {
                                        Button(
                                            onClick = { onAddCostItem(order) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isBangla) "+ খরচ যোগ" else "+ Add Cost",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Shipping Cost Calculator Display: Gross Weight * Rate/KG
                                val poGrossWeightBd = runCatching { BigDecimal(displayGrossWeight.ifBlank { "0" }) }.getOrDefault(BigDecimal.ZERO)
                                val persistedRateBd = runCatching { BigDecimal(order.shippingRatePerKgBdt.ifBlank { "0" }) }.getOrDefault(BigDecimal.ZERO)
                                val freightCostItemsBdtMinor = costItems.filter {
                                    it.costCategory in setOf(
                                        CostCategory.INTERNATIONAL_FREIGHT.name,
                                        CostCategory.AIR_FREIGHT.name,
                                        CostCategory.SEA_FREIGHT.name
                                    )
                                }.sumOf { it.bdtAmountMinor }
                                val activeFreightRates = freightRates.mapNotNull { r ->
                                    val rMinor = r.overrideRatePerKgBdtMinor?.takeIf { it > 0L }
                                        ?: r.appliedRatePerKgBdtMinor.takeIf { it > 0L }
                                        ?: r.defaultRatePerKgBdtMinor.takeIf { it > 0L }
                                    if (rMinor != null) r to rMinor else null
                                }
                                val isLocalOrderNoShipping = order.currency.equals("BDT", ignoreCase = true) &&
                                    poGrossWeightBd.compareTo(BigDecimal.ZERO) == 0 &&
                                    freightCostItemsBdtMinor == 0L &&
                                    persistedRateBd.compareTo(BigDecimal.ZERO) == 0 &&
                                    activeFreightRates.isEmpty()

                                val (effectiveShippingRate, calculatedShipping) = when {
                                    persistedRateBd > BigDecimal.ZERO -> {
                                        val rateScaled = persistedRateBd.setScale(2, RoundingMode.HALF_UP)
                                        rateScaled to poGrossWeightBd.multiply(rateScaled).setScale(2, RoundingMode.HALF_UP)
                                    }
                                    freightCostItemsBdtMinor > 0L && poGrossWeightBd > BigDecimal.ZERO -> {
                                        val freightBdt = BigDecimal(freightCostItemsBdtMinor).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
                                        val derivedRate = freightBdt.divide(poGrossWeightBd, 2, RoundingMode.HALF_UP)
                                        derivedRate to freightBdt
                                    }
                                    activeFreightRates.isNotEmpty() -> {
                                        val weightedBdtMinor = activeFreightRates.fold(BigDecimal.ZERO) { acc, (r, rMinor) ->
                                            val w = runCatching { BigDecimal(r.totalCategoryGrossWeightKg.ifBlank { "0" }) }.getOrDefault(BigDecimal.ZERO)
                                            acc.add(if (w > BigDecimal.ZERO) BigDecimal(rMinor).multiply(w) else BigDecimal(rMinor))
                                        }
                                        val weightedKg = activeFreightRates.fold(BigDecimal.ZERO) { acc, (r, _) ->
                                            val w = runCatching { BigDecimal(r.totalCategoryGrossWeightKg.ifBlank { "0" }) }.getOrDefault(BigDecimal.ZERO)
                                            acc.add(if (w > BigDecimal.ZERO) w else BigDecimal.ONE)
                                        }
                                        val derivedRate = if (weightedKg > BigDecimal.ZERO) {
                                            weightedBdtMinor.divide(BigDecimal("100")).divide(weightedKg, 2, RoundingMode.HALF_UP)
                                        } else {
                                            BigDecimal.ZERO.setScale(2)
                                        }
                                        derivedRate to poGrossWeightBd.multiply(derivedRate).setScale(2, RoundingMode.HALF_UP)
                                    }
                                    else -> {
                                        BigDecimal.ZERO.setScale(2) to BigDecimal.ZERO.setScale(2)
                                    }
                                }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = ERPSurfaceVariant),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isBangla) "শিপিং খরচ হিসাব (গ্রস ওজন)" else "Shipping Cost Formula (Gross Weight)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = if (isLocalOrderNoShipping) {
                                                    if (isBangla) "স্থানীয় ক্রয় — আন্তর্জাতিক শিপিং প্রযোজ্য নয়" else "Local Purchase — International shipping N/A"
                                                } else {
                                                    "${displayGrossWeight} KG × ৳${effectiveShippingRate}/KG"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isLocalOrderNoShipping) "—" else "৳ $calculatedShipping BDT",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryPurple
                                        )
                                    }
                                }

                                if (onRecordFreightPayment != null &&
                                    freightPayableSummary.remainingFreightBillMinor > 0L &&
                                    order.status !in setOf(PurchaseOrderStatus.DRAFT.name, PurchaseOrderStatus.CANCELLED.name)
                                ) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { showFreightPaymentDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, SecondaryTeal)
                                    ) {
                                        Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBangla) {
                                                "ফ্রেইট বিল পরিশোধ করুন (বকেয়া: ${Formatters.formatMoney(freightPayableSummary.remainingFreightBillMinor, CurrencyCode.BDT, language)})"
                                            } else {
                                                "Pay Freight Bill (Due: ${Formatters.formatMoney(freightPayableSummary.remainingFreightBillMinor, CurrencyCode.BDT, language)})"
                                            },
                                            color = SecondaryTeal,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }

                                if (costItems.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (isBangla) "নথিভুক্ত ল্যান্ডেড খরচ (${costItems.size}):" else "Recorded Landed Costs (${costItems.size}):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        costItems.forEach { cost ->
                                            CostItemCard(
                                                cost = cost,
                                                language = language,
                                                isBangla = isBangla,
                                                onDelete = if (!order.isActualCostFinalized && order.status != PurchaseOrderStatus.COMPLETED.name && order.status != PurchaseOrderStatus.CANCELLED.name) {
                                                    { deletingCostItemId = cost.id }
                                                } else null
                                            )
                                        }
                                    }
                                }

                                if (landedSummary.productBreakdowns.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = ERPDivider)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (isBangla) "পণ্যভিত্তিক ল্যান্ডিং কস্ট বিভাজন:" else "Product-wise Landed Cost Breakdown:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        landedSummary.productBreakdowns.forEach { prod ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                color = ERPSurfaceVariant.copy(alpha = 0.7f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, ERPDivider)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Text(
                                                            text = prod.productTitle,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary
                                                        )
                                                        val wtStr = prod.grossWeightKg.setScale(2, RoundingMode.HALF_UP).toPlainString()
                                                        Text(
                                                            text = if (isBangla) {
                                                                "${prod.totalPcs} PCS • $wtStr KG • ক্রয় ৳${Formatters.formatMinorUnit(prod.purchaseBdtMinor)} + ফ্রেইট/খরচ ৳${Formatters.formatMinorUnit(prod.freightBdtMinor + prod.otherOverheadBdtMinor)}"
                                                            } else {
                                                                "${prod.totalPcs} PCS • $wtStr KG • Buy ৳${Formatters.formatMinorUnit(prod.purchaseBdtMinor)} + Freight/Costs ৳${Formatters.formatMinorUnit(prod.freightBdtMinor + prod.otherOverheadBdtMinor)}"
                                                            },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextSecondary
                                                        )
                                                        Text(
                                                            text = if (isBangla) {
                                                                "মোট ল্যান্ডেড: ৳ ${Formatters.formatMinorUnit(prod.totalLandedBdtMinor)}"
                                                            } else {
                                                                "Total Landed: ৳ ${Formatters.formatMinorUnit(prod.totalLandedBdtMinor)}"
                                                            },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = TextPrimary
                                                        )
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = if (isBangla) "৳ ${prod.unitLandedBdt} / পিস" else "৳ ${prod.unitLandedBdt} / PCS",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = SecondaryTeal
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = ERPDivider)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Finalize Actual Landing Cost Action
                                if (isCostFinalized) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = StatusSuccessContainer),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = if (isBangla) "প্রকৃত ল্যান্ডেড কস্ট ফাইনাল করা হয়েছে!" else "Actual Landing Cost Finalized!",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusSuccess
                                                )
                                                Text(
                                                    text = if (isBangla) "স্টক ইনভেন্টরি পোস্টিং ও FIFO লেয়ার আনলকড" else "Inventory Posting & FIFO Layers Unlocked",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = StatusErrorContainer),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = StatusError, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isBangla) "প্রকৃত ব্যয় ফাইনাল করার পূর্বে স্টক পোস্টিং ব্লক থাকবে।" else "Stock posting is strictly BLOCKED until Actual Landing Cost is finalized.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = StatusError
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = { onFinalizeActualCost(order.id) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Outlined.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isBangla) "প্রকৃত ল্যান্ডেড কস্ট ফাইনাল করুন (Finalize Actual Cost)" else "Finalize Actual Landing Cost",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    }
                    if(detailTab==2) {
                    // 7. Warehouse Receiving Progress & Stock Posting Card
                    item {
                        ERPCard {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBangla) "ওয়্যারহাউস মালামাল গ্রহণ ও স্টক পোস্টিং" else "Warehouse Receiving & Stock Posting",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (isBangla) "মেথড এ (গণনা) ও বি (কার্টনভিত্তিক) • আংশিক গ্রহণ" else "Method A (Count) & B (Carton) • Partial Receiving",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (order.status != PurchaseOrderStatus.COMPLETED.name && order.status != PurchaseOrderStatus.CANCELLED.name) {
                                        Button(
                                            onClick = { onReceiveProducts(order) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Outlined.Warehouse, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isBangla) "+ পণ্য গ্রহণ" else "Receive Products",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isBangla) "ইনভেন্টরিতে পোস্টকৃত: $totalReceivedQuantity / $totalOrderedQuantity একক" else "Posted to Stock: $totalReceivedQuantity / $totalOrderedQuantity units",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalReceivedQuantity >= totalOrderedQuantity && totalOrderedQuantity > 0) StatusSuccess else PrimaryPurple
                                    )
                                    Text(
                                        text = "${(receivingProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalReceivedQuantity >= totalOrderedQuantity && totalOrderedQuantity > 0) StatusSuccess else SecondaryTeal
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { receivingProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (totalReceivedQuantity >= totalOrderedQuantity && totalOrderedQuantity > 0) StatusSuccess else SecondaryTeal,
                                    trackColor = ERPBackground
                                )

                                val draftReceiptsCount = receipts.count { it.status == "DRAFT" }
                                if (draftReceiptsCount > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isBangla) "⚠️ $draftReceiptsCount টি ড্রাফট চালান অপেক্ষমান (স্টক পোস্টিংয়ের জন্য ল্যান্ডেড কস্ট ফাইনাল আবশ্যক)" else "⚠️ $draftReceiptsCount draft receipt(s) pending final cost & stock posting",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusWarning,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Receipts List with Post Action
                                if (receipts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (isBangla) "ওয়্যারহাউস চালান তালিকা (${receipts.size}):" else "Warehouse Receipts (${receipts.size}):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        receipts.forEach { rec ->
                                            WarehouseReceiptCard(
                                                receipt = rec,
                                                isCostFinalized = isCostFinalized,
                                                isPurchaseClosed = order.status == PurchaseOrderStatus.COMPLETED.name ||
                                                    order.status == PurchaseOrderStatus.CANCELLED.name,
                                                isBangla = isBangla,
                                                onPostReceiving = onPostReceiving
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    }
                    if(detailTab==2) {
                    // 8. Damage & Shortage Discrepancy Accounting Card
                    if (damageRecords.isNotEmpty() || shortageRecords.isNotEmpty()) {
                        item {
                            ERPCard {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = if (isBangla) "ক্ষতিগ্রস্ত ও ঘাটতি পণ্যের হিসাব (Write-off & Claims)" else "Damage & Shortage Discrepancy Accounting",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Damage records: Dr 5050 Cr 1220
                                    damageRecords.forEach { dmg ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Damage Write-off: ${dmg.damagedQuantity} pcs",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusError
                                                )
                                                Text(
                                                    text = "Dr. 5050 Damaged Write-Off / Cr. 1220 In-Transit",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "৳ ${Formatters.formatMinorUnit(dmg.totalLossMinor)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusError
                                            )
                                        }
                                    }

                                    // Shortage records: Dr 1050 Cr 1220
                                    shortageRecords.forEach { sht ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Shortage Claim: ${sht.missingQuantity} pcs",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusWarning
                                                )
                                                Text(
                                                    text = "Dr. 1050 Supplier Claims Receivable / Cr. 1220 In-Transit",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "৳ ${Formatters.formatMinorUnit(sht.totalAmountMinor)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusWarning
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    }
                    if(detailTab==3) {
                    // 9. Post-Arrival Additional Costs Card
                    item {
                        ERPCard {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBangla) "অতিরিক্ত পরিচালন ব্যয়" else "Post-Arrival Operational Costs",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (isBangla) "মূলধনীকরণ (অ্যাসেট) বনাম সাময়িক খরচ (P&L)" else "Capitalized to Inventory vs Period Expense",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (order.status != PurchaseOrderStatus.COMPLETED.name && order.status != PurchaseOrderStatus.CANCELLED.name) {
                                        Button(
                                            onClick = { onAddAdditionalCost(order) },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isBangla) "+ খরচ" else "+ Add Cost",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (additionalCosts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        additionalCosts.forEach { ac ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                shape = RoundedCornerShape(8.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, ERPDivider)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = ac.category.replace("_", " "),
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .background(
                                                                    if (ac.isCapitalizable) StatusSuccessContainer else StatusWarningContainer,
                                                                    RoundedCornerShape(4.dp)
                                                                )
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = if (ac.isCapitalizable) "CAPITALIZED" else "PERIOD EXPENSE",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (ac.isCapitalizable) StatusSuccess else StatusWarning
                                                            )
                                                        }
                                                    }
                                                    if (ac.description.isNotBlank()) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(ac.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    HorizontalDivider(color = ERPSurfaceVariant)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = if (isBangla) "খরচের পরিমাণ:" else "Recorded Amount:",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextMuted
                                                        )
                                                        Text(
                                                            text = "৳ ${Formatters.formatMinorUnit(ac.amountMinor)} BDT",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = PrimaryPurple
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }                    }

                    if(showLifecycleActions) item {
                // Bottom Action Bar - Responsive 2-Row Layout
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val hasPrimaryActions = order.status == PurchaseOrderStatus.DRAFT.name ||
                            (remainingBalanceMinor > 0 && order.status != PurchaseOrderStatus.CANCELLED.name) ||
                            (order.status != PurchaseOrderStatus.COMPLETED.name && order.status != PurchaseOrderStatus.CANCELLED.name)

                        // Row 1: Primary Action Buttons (only rendered if actionable)
                        if (hasPrimaryActions) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (order.status == PurchaseOrderStatus.DRAFT.name) {
                                    Button(
                                        onClick = { onConfirmPo(order.id) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 44.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBangla) "ক্রয়াদেশ নিশ্চিত করুন (Confirm PO)" else "Confirm Purchase Order",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                } else {
                                    val showPaymentBtn = remainingBalanceMinor > 0 && order.status != PurchaseOrderStatus.CANCELLED.name
                                    val showCompleteBtn = order.status != PurchaseOrderStatus.COMPLETED.name && order.status != PurchaseOrderStatus.CANCELLED.name

                                    if (showPaymentBtn) {
                                        Button(
                                            onClick = { onMakePayment(order) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .defaultMinSize(minHeight = 44.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Outlined.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isBangla) "পেমেন্ট পরিশোধ" else "Make Payment",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (showCompleteBtn) {
                                        Button(
                                            onClick = { onCompletePo(order) },
                                            modifier = (if (showPaymentBtn) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                                .defaultMinSize(minHeight = 44.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Outlined.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isBangla) "ক্রয়াদেশ সমাপ্ত" else "Complete PO",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Row 2: Secondary Utility Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onSharePdf(order) },
                                modifier = Modifier
                                    .widthIn(min = 104.dp)
                                    .defaultMinSize(minHeight = 48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "শেয়ার PO" else "Share PO",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (onEditFullPo != null && order.status != PurchaseOrderStatus.CANCELLED.name) {
                                OutlinedButton(
                                    onClick = {
                                        onEditFullPo(order)
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .widthIn(min = 104.dp)
                                        .defaultMinSize(minHeight = 48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPurple),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "PO এডিট" else "Edit PO",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (onDuplicatePo != null) {
                                OutlinedButton(
                                    onClick = {
                                        onDuplicatePo(order.id)
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .widthIn(min = 104.dp)
                                        .defaultMinSize(minHeight = 48.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "কপি করুন" else "Duplicate",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (canCancel) {
                                OutlinedButton(
                                    onClick = { showCancelConfirmDialog = true },
                                    modifier = Modifier
                                        .widthIn(min = 104.dp)
                                        .defaultMinSize(minHeight = 48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "বাতিল" else "Cancel",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (canDeletePo) {
                                OutlinedButton(
                                    onClick = { showDeletePoConfirmDialog = true },
                                    modifier = Modifier
                                        .widthIn(min = 104.dp)
                                        .defaultMinSize(minHeight = 48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "PO ডিলিট" else "Delete PO",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (!hasPrimaryActions) {
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .widthIn(min = 104.dp)
                                        .defaultMinSize(minHeight = 48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "বন্ধ করুন" else "Close",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .widthIn(min = 104.dp)
                                        .defaultMinSize(minHeight = 48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "বন্ধ করুন" else "Close",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                    }

                }
                if(navigateTab!=null) com.pocketdev.advance_erp.core.designsystem.ERPBottomNavigation("purchases", { onDismiss();navigateTab(it) },language=language)

            }
        }
    }

    } // PO palette

    // Cancel PO Confirmation Alert with Reason input
    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = {
                Text(
                    text = if (isBangla) "ক্রয়াদেশ বাতিল নিশ্চিতকরণ" else "Confirm Cancel Purchase Order",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isBangla)
                            "আপনি কি নিশ্চিত যে ক্রয়াদেশ ${order.poNumber} বাতিল করতে চান? এটি সরবরাহকারীর বকেয়া থেকে সংশ্লিষ্ট পরিমাণ প্রত্যাহার করবে।"
                        else
                            "Are you sure you want to cancel Purchase Order ${order.poNumber}? This will revert any supplier payable commitment."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text(if (isBangla) "বাতিলের কারণ (বাধ্যতামূলক নয়)" else "Cancellation Reason (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirmDialog = false
                        onCancelPo(order.id, cancelReason.ifBlank { null })
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(text = if (isBangla) "হ্যাঁ, বাতিল করুন" else "Yes, Cancel Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text(text = if (isBangla) "না" else "Dismiss")
                }
            }
        )
    }

    // Delete PO Confirmation Alert (allowed for any non-received PO)
    if (showDeletePoConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePoConfirmDialog = false },
            title = {
                Text(
                    text = if (isBangla) "ক্রয়াদেশ ডিলিট নিশ্চিতকরণ" else "Delete Purchase Order?",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                Text(
                    text = if (isBangla)
                        "আপনি কি নিশ্চিত যে ক্রয়াদেশ ${order.poNumber} স্থায়ীভাবে ডিলিট করতে চান? এর সাথে যুক্ত প্যাকিং ও হিসাব প্রত্যাহার করা হবে।"
                    else
                        "Are you sure you want to permanently delete Purchase Order ${order.poNumber}? Linked packing and payable records will be removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeletePoConfirmDialog = false
                        onDeletePo?.invoke(order.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(text = if (isBangla) "হ্যাঁ, ডিলিট করুন" else "Yes, Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePoConfirmDialog = false }) {
                    Text(text = if (isBangla) "না" else "Cancel")
                }
            }
        )
    }

    reversingPayment?.let { targetPayment ->
        AlertDialog(
            onDismissRequest = { reversingPayment = null },
            title = {
                Text(
                    text = if (isBangla) "পেমেন্ট বাতিল / রিভার্স করুন?" else "Reverse Supplier Payment?",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                Text(
                    text = if (isBangla)
                        "আপনি কি পেমেন্ট ${targetPayment.paymentNumber} (${Formatters.formatMoney(targetPayment.bdtAmountMinor, CurrencyCode.BDT, language)}) রিভার্স করতে চান? এতে ক্যাশ/ব্যাংক ব্যালেন্স ফিরে আসবে এবং PO-র বকেয়া আপডেট হবে।"
                    else
                        "Reverse payment ${targetPayment.paymentNumber} (${Formatters.formatMoney(targetPayment.bdtAmountMinor, CurrencyCode.BDT, language)})? Cash/Bank balance and PO payable will be restored.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pid = targetPayment.id
                        reversingPayment = null
                        onReversePayment?.invoke(pid)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(text = if (isBangla) "হ্যাঁ, রিভার্স করুন" else "Yes, Reverse")
                }
            },
            dismissButton = {
                TextButton(onClick = { reversingPayment = null }) {
                    Text(text = if (isBangla) "না" else "Cancel")
                }
            }
        )
    }

    if (showFreightPaymentDialog && onRecordFreightPayment != null) {
        RecordPoFreightPaymentDialog(
            poNumber = order.poNumber,
            totalFreightBillMinor = freightPayableSummary.totalFreightBillMinor,
            paidFreightBillMinor = freightPayableSummary.paidFreightBillMinor,
            remainingDueMinor = freightPayableSummary.remainingFreightBillMinor,
            liquidAccounts = liquidAccounts,
            accountBalances = accountBalances,
            isBangla = isBangla,
            onDismiss = { showFreightPaymentDialog = false },
            onConfirm = { accId, amtMinor, notesStr ->
                onRecordFreightPayment.invoke(order.id, accId, amtMinor, System.currentTimeMillis(), notesStr)
            }
        )
    }

    reversingFreightPayment?.let { targetFp ->
        AlertDialog(
            onDismissRequest = { reversingFreightPayment = null },
            title = {
                Text(
                    text = if (isBangla) "ফ্রেইট পেমেন্ট বাতিল / রিভার্স করুন?" else "Reverse Freight Payment?",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                Text(
                    text = if (isBangla)
                        "আপনি কি ফ্রেইট পেমেন্ট ${targetFp.entryNumber} (${Formatters.formatMoney(targetFp.amountBdtMinor, CurrencyCode.BDT, language)}) রিভার্স করতে চান? এতে ক্যাশ/ব্যাংক ব্যালেন্স ফিরে আসবে এবং ফ্রেইট বকেয়া আপডেট হবে।"
                    else
                        "Reverse freight payment ${targetFp.entryNumber} (${Formatters.formatMoney(targetFp.amountBdtMinor, CurrencyCode.BDT, language)})? Cash/Bank balance and freight payable will be restored.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val jeId = targetFp.journalEntryId
                        reversingFreightPayment = null
                        onReverseFreightPayment?.invoke(jeId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(text = if (isBangla) "হ্যাঁ, রিভার্স করুন" else "Yes, Reverse")
                }
            },
            dismissButton = {
                TextButton(onClick = { reversingFreightPayment = null }) {
                    Text(text = if (isBangla) "না" else "Cancel")
                }
            }
        )
    }

    // Edit Packing Dialog (Allows resetting or modifying cartons e.g. 5 CTN -> 0)
    if (showEditPackingDialog) {
        var editCartonsStr by remember(order) { mutableStateOf(order.totalCartons.toString()) }
        var editGrossWeightStr by remember(order) { mutableStateOf(order.totalGrossWeightKg) }
        var editNetWeightStr by remember(order) { mutableStateOf(order.totalNetWeightKg) }
        var editCbmStr by remember(order) { mutableStateOf(order.totalCbm) }

        AlertDialog(
            onDismissRequest = { showEditPackingDialog = false },
            title = {
                Text(
                    text = if (isBangla) "প্যাকিং বিবরণ ও কার্টন সম্পাদনা" else "Edit Packing & Cartons",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBangla) "মোট কার্টন সংখ্যা বা প্যাকিং পরিমাপ পরিবর্তন করুন (যেমন ৫ কার্টন সরাতে ০ দিন):" 
                               else "Update total cartons or packing measurements (e.g. enter 0 to reset cartons):",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = editCartonsStr,
                        onValueChange = { editCartonsStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isBangla) "মোট কার্টন (Cartons)" else "Total Cartons") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editGrossWeightStr,
                        onValueChange = { editGrossWeightStr = it },
                        label = { Text(if (isBangla) "মোট গ্রস ওজন (KG)" else "Total Gross Weight (KG)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNetWeightStr,
                        onValueChange = { editNetWeightStr = it },
                        label = { Text(if (isBangla) "মোট নেট ওজন (KG)" else "Total Net Weight (KG)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCbmStr,
                        onValueChange = { editCbmStr = it },
                        label = { Text(if (isBangla) "মোট সিবিএম (CBM)" else "Total CBM (m³)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ctn = editCartonsStr.toIntOrNull() ?: 0
                        onUpdatePacking?.invoke(ctn, editGrossWeightStr.trim(), editNetWeightStr.trim(), editCbmStr.trim())
                        showEditPackingDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text(if (isBangla) "সংরক্ষণ করুন" else "Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPackingDialog = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Edit PO Line Item Dialog (Allows modifying quantity from 100 pcs, unit price, cartons)
    editingItem?.let { item ->
        val product = productMap[item.productId]
        val prodName = if (isBangla && !product?.nameBn.isNullOrBlank()) product?.nameBn!! else product?.nameEn ?: item.productId
        var editQtyStr by remember(item) { mutableStateOf(item.quantity.toString()) }
        var editPriceStr by remember(item) {
            val priceMajor = BigDecimal(item.unitPriceMinor).movePointLeft(2).stripTrailingZeros().toPlainString()
            mutableStateOf(priceMajor)
        }
        var editCartonQtyStr by remember(item) { mutableStateOf(item.cartonQuantity.toString()) }
        var editGrossWeightStr by remember(item) { mutableStateOf(item.grossWeightKg) }

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = {
                Text(
                    text = if (isBangla) "পণ্য আইটেম পরিবর্তন: $prodName" else "Edit Item: $prodName",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBangla) "অর্ডার পরিমাণ (pcs) ও কার্টন সংখ্যা পরিবর্তন করুন:" 
                               else "Update ordered quantity (pcs) and carton count:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = editQtyStr,
                        onValueChange = { editQtyStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isBangla) "পরিমাণ (Pieces)" else "Quantity (Pieces)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPriceStr,
                        onValueChange = { editPriceStr = it },
                        label = { Text(if (isBangla) "ইউনিট মূল্য (${order.currency})" else "Unit Price (${order.currency})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCartonQtyStr,
                        onValueChange = { editCartonQtyStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isBangla) "কার্টন সংখ্যা (Cartons)" else "Carton Count") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editGrossWeightStr,
                        onValueChange = { editGrossWeightStr = it },
                        label = { Text(if (isBangla) "গ্রস ওজন (KG)" else "Gross Weight (KG)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = editQtyStr.toIntOrNull() ?: item.quantity
                        val priceBd = editPriceStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        val priceMinor = priceBd.movePointRight(2).toLong()
                        val ctn = editCartonQtyStr.toIntOrNull() ?: item.cartonQuantity
                        onUpdatePoItem?.invoke(item.id, qty, priceMinor, ctn, editGrossWeightStr.trim())
                        editingItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text(if (isBangla) "সংরক্ষণ করুন" else "Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Delete PO Line Item Confirmation Alert
    deletingItemId?.let { itemId ->
        val item = items.find { it.id == itemId }
        val product = productMap[item?.productId]
        val prodName = if (isBangla && !product?.nameBn.isNullOrBlank()) product?.nameBn!! else product?.nameEn ?: item?.productId ?: "Item"
        AlertDialog(
            onDismissRequest = { deletingItemId = null },
            title = {
                Text(
                    text = if (isBangla) "পণ্য মুছে ফেলতে চান?" else "Delete Item?",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                Text(
                    text = if (isBangla) "আপনি কি নিশ্চিত যে '$prodName' পিও থেকে মুছে ফেলতে চান?" 
                           else "Are you sure you want to remove '$prodName' from this Purchase Order?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePoItem?.invoke(itemId)
                        deletingItemId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(if (isBangla) "মুছে ফেলুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItemId = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Delete Carton Confirmation Alert
    deletingCartonNumber?.let { ctnNum ->
        AlertDialog(
            onDismissRequest = { deletingCartonNumber = null },
            title = {
                Text(
                    text = if (isBangla) "কার্টন মুছে ফেলতে চান?" else "Delete Carton?",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                Text(
                    text = if (isBangla) "আপনি কি নিশ্চিত যে কার্টন '$ctnNum' মুছে ফেলতে চান?" 
                           else "Are you sure you want to delete carton '$ctnNum'?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCarton?.invoke(order.id, ctnNum)
                        deletingCartonNumber = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(if (isBangla) "মুছে ফেলুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCartonNumber = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Delete Cost Item Confirmation Alert
    deletingCostItemId?.let { cId ->
        val cost = costItems.find { it.id == cId }
        val costName = cost?.costCategory?.replace("_", " ") ?: "Cost Item"
        AlertDialog(
            onDismissRequest = { deletingCostItemId = null },
            title = {
                Text(
                    text = if (isBangla) "ব্যয় আইটেম মুছুন" else "Delete Cost Item?",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                Text(
                    text = if (isBangla) "আপনি কি নিশ্চিত যে ব্যয় '$costName' মুছে ফেলতে চান?" 
                           else "Are you sure you want to delete cost item '$costName'?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCostItem?.invoke(cId)
                        deletingCostItemId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(if (isBangla) "মুছে ফেলুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCostItemId = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun PoStatusBadge(statusStr: String, isBangla: Boolean) {
    val (bgColor, textColor, label) = when (statusStr) {
        PurchaseOrderStatus.DRAFT.name -> Triple(
            ERPDivider,
            TextPrimary,
            if (isBangla) PurchaseOrderStatus.DRAFT.labelBn else PurchaseOrderStatus.DRAFT.labelEn
        )
        PurchaseOrderStatus.CONFIRMED.name -> Triple(
            SecondaryTeal.copy(alpha = 0.15f),
            SecondaryTeal,
            if (isBangla) PurchaseOrderStatus.CONFIRMED.labelBn else PurchaseOrderStatus.CONFIRMED.labelEn
        )
        PurchaseOrderStatus.PARTIALLY_PAID.name -> Triple(
            StatusWarning.copy(alpha = 0.15f),
            StatusWarning,
            if (isBangla) PurchaseOrderStatus.PARTIALLY_PAID.labelBn else PurchaseOrderStatus.PARTIALLY_PAID.labelEn
        )
        PurchaseOrderStatus.PAID.name -> Triple(
            StatusSuccess.copy(alpha = 0.15f),
            StatusSuccess,
            if (isBangla) PurchaseOrderStatus.PAID.labelBn else PurchaseOrderStatus.PAID.labelEn
        )
        PurchaseOrderStatus.ACTUAL_COST_FINALIZED.name -> Triple(
            PrimaryPurple.copy(alpha = 0.15f),
            PrimaryPurple,
            if (isBangla) "কস্ট ফাইনাল" else "Cost Finalized"
        )
        PurchaseOrderStatus.SHIPPED.name -> Triple(
            PrimaryPurple.copy(alpha = 0.15f),
            PrimaryPurple,
            if (isBangla) PurchaseOrderStatus.SHIPPED.labelBn else PurchaseOrderStatus.SHIPPED.labelEn
        )
        PurchaseOrderStatus.COMPLETED.name -> Triple(
            StatusSuccess.copy(alpha = 0.2f),
            StatusSuccess,
            if (isBangla) PurchaseOrderStatus.COMPLETED.labelBn else PurchaseOrderStatus.COMPLETED.labelEn
        )
        PurchaseOrderStatus.CANCELLED.name -> Triple(
            StatusError.copy(alpha = 0.15f),
            StatusError,
            if (isBangla) PurchaseOrderStatus.CANCELLED.labelBn else PurchaseOrderStatus.CANCELLED.labelEn
        )
        else -> Triple(Color.LightGray, Color.DarkGray, statusStr)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun PackingMetricBox(
    label: String,
    primaryValue: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, ERPDivider, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CostItemCard(
    cost: ImportCostItem,
    language: AppLanguage,
    isBangla: Boolean,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ERPDivider)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cost.costCategory.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (cost.isEstimated) StatusWarningContainer else StatusSuccessContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (cost.isEstimated) "ESTIMATED" else "ACTUAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cost.isEstimated) StatusWarning else StatusSuccess
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(ERPDivider, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = cost.allocationMethod.replace("_", " "),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete Cost Item",
                                tint = StatusError,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            if (cost.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = cost.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = ERPSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBangla) "বরাদ্দকৃত খরচ:" else "Allocated Cost:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${cost.currency} ${Formatters.formatMinorUnit(cost.amountMinor)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${Formatters.formatMoney(cost.bdtAmountMinor, CurrencyCode.BDT, language)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupedCartonCard(
    displayNumber: String,
    cartonRows: List<ShipmentCarton>,
    lineSummaries: List<String>,
    shippingMark: String,
    isCustomMark: Boolean,
    onDelete: (() -> Unit)? = null
) {
    val first = cartonRows.firstOrNull() ?: return
    val isMixed = cartonRows.size > 1 || cartonRows.any { it.isMixedCarton }
    val totalQty = cartonRows.sumOf { it.quantity }
    val cartonGross = first.cartonGrossWeightKg.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
        ?: cartonRows.fold(BigDecimal.ZERO) { acc, r -> acc + (r.grossWeightKg.toBigDecimalOrNull() ?: BigDecimal.ZERO) }
    val cartonNet = cartonRows.fold(BigDecimal.ZERO) { acc, r -> acc + (r.netWeightKg.toBigDecimalOrNull() ?: BigDecimal.ZERO) }
    val dims = listOf(first.lengthCm, first.widthCm, first.heightCm)
        .mapNotNull { it.toBigDecimalOrNull()?.takeIf { d -> d > BigDecimal.ZERO }?.stripTrailingZeros()?.toPlainString() }
    val dimText = if (dims.size == 3) "${dims.joinToString("×")} cm" else "-"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ERPDivider)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = displayNumber,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (isMixed) SecondaryTeal.copy(alpha = 0.15f) else PrimaryPurple.copy(alpha = 0.15f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isMixed) "MIXED CARTON (${cartonRows.size})" else "SINGLE PRODUCT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMixed) SecondaryTeal else PrimaryPurple
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$totalQty pcs",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (onDelete != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete Carton",
                                tint = StatusError,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            lineSummaries.forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${if (isCustomMark) "Custom Mark" else "Shipping Mark"}: ${shippingMark.replace("\n", " | ")}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isCustomMark) SecondaryTeal else TextMuted
            )

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = ERPSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "G.W: ${cartonGross.stripTrailingZeros().toPlainString()} KG • N.W: ${cartonNet.stripTrailingZeros().toPlainString()} KG • Dim: $dimText",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryPurple
                )
                Text(
                    text = "${first.cbm} CBM",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SecondaryTeal
                )
            }
        }
    }
}

@Composable
private fun WarehouseReceiptCard(
    receipt: WarehouseReceipt,
    isCostFinalized: Boolean,
    isPurchaseClosed: Boolean,
    isBangla: Boolean,
    onPostReceiving: (String) -> Unit
) {
    val canPost = isCostFinalized && !isPurchaseClosed
    val isPosted = receipt.status == "POSTED"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ERPDivider)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Receipt Number on Left, Status Badge on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = receipt.receiptNumber,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall
                )

                Box(
                    modifier = Modifier
                        .background(
                            if (isPosted) StatusSuccessContainer else StatusWarningContainer,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isPosted) Icons.Outlined.CheckCircle else Icons.Outlined.Info,
                            contentDescription = null,
                            tint = if (isPosted) StatusSuccess else StatusWarning,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isPosted) {
                                if (isBangla) "স্টকে পোস্টকৃত" else "POSTED TO STOCK"
                            } else {
                                if (isBangla) "খসড়া (অপেক্ষমাণ)" else "DRAFT (PENDING)"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPosted) StatusSuccess else StatusWarning,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-row: Receiving Method Chip + Received By
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(ERPSurfaceVariant, RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isBangla) {
                            if (receipt.receivingMethod == "CARTON_WISE") "পদ্ধতি: কার্টুনভিত্তিক" else "পদ্ধতি: গণনা (Physical Count)"
                        } else {
                            "Method: " + receipt.receivingMethod.replace("_", " ")
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (receipt.receivedBy.isNotBlank()) {
                    Text(
                        text = receipt.receivedBy,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!receipt.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = receipt.notes ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (receipt.hasDiscrepancies) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StatusErrorContainer, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = StatusError,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (receipt.discrepancyApproved) {
                            if (isBangla) "ঘাটতি/ক্ষতি অনুমোদন সম্পন্ন" else "Discrepancies Approved"
                        } else {
                            if (isBangla) "ঘাটতি/ক্ষতি অনুমোদনের অপেক্ষায়" else "Discrepancies Pending Approval"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusError
                    )
                }
            }

            // Post to Stock Button (full width, comfortable height)
            if (receipt.status == "DRAFT") {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onPostReceiving(receipt.id) },
                    enabled = canPost,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPurple
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = if (canPost) Icons.Outlined.Inventory2 else Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            canPost -> if (isBangla) "স্টক ইনভেন্টরিতে পোস্ট করুন" else "Post Receipt to Inventory Stock"
                            isPurchaseClosed -> if (isBangla) "ক্রয়াদেশ বন্ধ হয়েছে" else "Purchase Order Closed"
                            else -> if (isBangla) "পোস্টিং লক করা (প্রথমে ল্যান্ডেড কস্ট ফাইনাল করুন)" else "Posting Locked (Finalize Landed Cost First)"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordPoFreightPaymentDialog(
    poNumber: String,
    totalFreightBillMinor: Long,
    paidFreightBillMinor: Long,
    remainingDueMinor: Long,
    liquidAccounts: List<ChartOfAccount>,
    accountBalances: Map<String, Long>,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (paymentAccountId: String, amountBdtMinor: Long, notes: String?) -> Unit
) {
    val activeAccounts = remember(liquidAccounts) { liquidAccounts.filter { it.isActive } }
    var selectedAccountId by remember(activeAccounts, accountBalances) {
        mutableStateOf(
            activeAccounts.maxByOrNull { accountBalances[it.id] ?: 0L }?.id
                ?: activeAccounts.firstOrNull()?.id.orEmpty()
        )
    }
    var amountStr by rememberSaveable(poNumber, remainingDueMinor) {
        mutableStateOf(
            BigDecimal(remainingDueMinor.coerceAtLeast(0L))
                .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        )
    }
    var notes by rememberSaveable(poNumber) { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val lang = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH
    val selectedBalMinor = accountBalances[selectedAccountId] ?: 0L

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 540.dp)
                .clip(RoundedCornerShape(18.dp))
                .navigationBarsPadding()
                .imePadding(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isBangla) "ফ্রেইট ফরোয়ার্ডার পেমেন্ট ($poNumber)" else "Pay Freight Forwarder ($poNumber)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ERPSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isBangla) "মোট ফ্রেইট বিল:" else "Total Freight Bill:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(Formatters.formatMoney(totalFreightBillMinor, CurrencyCode.BDT, lang), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isBangla) "ইতিমধ্যে পরিশোধিত:" else "Already Paid:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(Formatters.formatMoney(paidFreightBillMinor, CurrencyCode.BDT, lang), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = StatusSuccess)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isBangla) "অবশিষ্ট বকেয়া:" else "Remaining Due:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(Formatters.formatMoney(remainingDueMinor, CurrencyCode.BDT, lang), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = StatusError)
                        }
                    }
                }

                Text(
                    text = if (isBangla) "পেমেন্টের হিসাব নির্বাচন করুন (Cash / Bank / MFS):" else "Select Payment Account (Cash / Bank / MFS):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (activeAccounts.isEmpty()) {
                    Text(
                        text = if (isBangla) "কোনো সক্রিয় নগদ বা ব্যাংক হিসাব পাওয়া যায়নি।" else "No active cash or bank account found.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusError
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        activeAccounts.forEach { acc ->
                            val isSel = acc.id == selectedAccountId
                            val bal = accountBalances[acc.id] ?: 0L
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedAccountId = acc.id
                                        errorMessage = null
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) SecondaryTeal.copy(alpha = 0.1f) else ERPBackground,
                                border = BorderStroke(1.dp, if (isSel) SecondaryTeal else ERPDivider)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${acc.accountCode} • ${if (isBangla && acc.nameBn.isNotBlank()) acc.nameBn else acc.nameEn}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = Formatters.formatMoney(bal, CurrencyCode.BDT, lang),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (bal <= 0L) StatusError else StatusSuccess
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        errorMessage = null
                    },
                    label = { Text(if (isBangla) "পরিশোধের পরিমাণ (BDT ৳) *" else "Payment Amount (BDT ৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                amountStr = BigDecimal(remainingDueMinor.coerceAtLeast(0L))
                                    .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                                    .stripTrailingZeros()
                                    .toPlainString()
                                errorMessage = null
                            }
                        ) {
                            Text(
                                text = if (isBangla) "সম্পূর্ণ বকেয়া" else "Full Due",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryTeal
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isBangla) "ফরোয়ার্ডার রেফারেন্স / নোট" else "Forwarder Ref / Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = StatusError, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(if (isBangla) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                require(selectedAccountId.isNotBlank()) {
                                    if (isBangla) "একটি পেমেন্ট হিসাব নির্বাচন করুন" else "Select a payment account"
                                }
                                val amtBd = BigDecimal(Formatters.normalizeAsciiNumberInput(amountStr).trim())
                                require(amtBd > BigDecimal.ZERO) {
                                    if (isBangla) "পরিমাণ শূন্যের বেশি হতে হবে" else "Amount must be positive"
                                }
                                val minor = amtBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                                require(minor <= remainingDueMinor) {
                                    if (isBangla) "বকেয়া ফ্রেইট বিলের চেয়ে বেশি পরিশোধ করা যাবে না" else "Cannot exceed remaining freight bill"
                                }
                                require(selectedBalMinor >= minor) {
                                    if (isBangla) {
                                        "নির্বাচিত হিসাবে পর্যাপ্ত ব্যালেন্স নেই! আগে মূলধন / তহবিল জমা করুন।"
                                    } else {
                                        "Insufficient balance in selected account! Deposit capital/funds first."
                                    }
                                }
                                onConfirm(selectedAccountId, minor, notes.ifBlank { null })
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Invalid payment amount"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                    ) {
                        Text(if (isBangla) "পরিশোধ করুন" else "Pay Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
