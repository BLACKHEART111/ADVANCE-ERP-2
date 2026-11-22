package com.pocketdev.advance_erp.feature.imports

import com.pocketdev.advance_erp.core.designsystem.StatusInfoContainer
import com.pocketdev.advance_erp.core.designsystem.StatusInfo
import com.pocketdev.advance_erp.core.designsystem.StatusSuccessContainer
import com.pocketdev.advance_erp.core.designsystem.StatusWarningContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONArray
import com.pocketdev.advance_erp.core.designsystem.ERPLeaveEditorDialog
import com.pocketdev.advance_erp.core.util.EditorDraftStore
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.saveable.Saver
import com.pocketdev.advance_erp.core.designsystem.ERPFullScreenEditor
import com.pocketdev.advance_erp.core.designsystem.ERPSurfaceVariant
import com.pocketdev.advance_erp.core.util.CartonMeasurement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import com.pocketdev.advance_erp.core.engine.CartonWeightAllocationEngine
import com.pocketdev.advance_erp.core.engine.MixedCartonLineInput
import com.pocketdev.advance_erp.core.engine.PackingCartonLine
import com.pocketdev.advance_erp.core.engine.PackingIssueType
import com.pocketdev.advance_erp.core.engine.PackingOrderLine
import com.pocketdev.advance_erp.core.engine.PoPackingEngine
import com.pocketdev.advance_erp.core.engine.VariantKey
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.SuggestTextField
import com.pocketdev.advance_erp.data.entity.SuggestionType
import com.pocketdev.advance_erp.data.entity.ShippingMethod
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.data.entity.CostCategory
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.domain.model.PaymentTermType
import com.pocketdev.advance_erp.domain.model.PaymentTermsCalculator
import com.pocketdev.advance_erp.domain.repository.MixedCartonLineDraft
import com.pocketdev.advance_erp.domain.repository.PoMixedCartonDraft
import com.pocketdev.advance_erp.domain.repository.PurchaseOrderItemDraft
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.core.util.PoAttachmentStorage
import com.pocketdev.advance_erp.core.util.PoNotesMetadataParser
import com.pocketdev.advance_erp.feature.product.AddProductDialog
import com.pocketdev.advance_erp.feature.product.GalleryItemDraft
import com.pocketdev.advance_erp.feature.product.NewVariantDraft
import java.math.BigDecimal
import java.math.RoundingMode

private class ItemDraftState(
    productId: String = "",
    variantId: String? = null,
    quantityStr: String = "",
    unitPriceStr: String = "",
    cartonQuantityStr: String = "0",
    pcsPerCartonStr: String = "",
    grossWeightKgStr: String = "",
    netWeightKgStr: String = ""
) {
    var productId by mutableStateOf(productId)
    var variantId by mutableStateOf(variantId)
    var quantityStr by mutableStateOf(quantityStr)
    var unitPriceStr by mutableStateOf(unitPriceStr)
    var cartonQuantityStr by mutableStateOf(cartonQuantityStr)
    var pcsPerCartonStr by mutableStateOf(pcsPerCartonStr)
    var grossWeightKgStr by mutableStateOf(grossWeightKgStr)
    var netWeightKgStr by mutableStateOf(netWeightKgStr)
}

private class CartonLineState(
    productId: String = "",
    variantId: String? = null,
    quantityStr: String = "20",
    manualWeightKgStr: String = ""
) {
    var productId by mutableStateOf(productId)
    var variantId by mutableStateOf(variantId)
    var quantityStr by mutableStateOf(quantityStr)
    var manualWeightKgStr by mutableStateOf(manualWeightKgStr)
}

private class CartonBoxState(
    val uid: String = java.util.UUID.randomUUID().toString(),
    cartonNumber: String = "CTN-001",
    grossWeightKgStr: String = "15.0",
    initialLines: List<CartonLineState> = emptyList(),
    netWeightKgStr: String = "",
    lengthCm: BigDecimal = BigDecimal.ZERO,
    widthCm: BigDecimal = BigDecimal.ZERO,
    heightCm: BigDecimal = BigDecimal.ZERO,
    cbm: BigDecimal = BigDecimal.ZERO,
    customShippingMark: String = ""
) {
    var lengthCm by mutableStateOf(lengthCm)
        private set
    var widthCm by mutableStateOf(widthCm)
        private set
    var heightCm by mutableStateOf(heightCm)
        private set
    var cbm by mutableStateOf(
        if (lengthCm.signum() > 0 && widthCm.signum() > 0 && heightCm.signum() > 0)
            CartonMeasurement(lengthCm, widthCm, heightCm).cbm else cbm
    )
        private set
    var measurementStr by mutableStateOf(
        if (lengthCm.signum() > 0 && widthCm.signum() > 0 && heightCm.signum() > 0)
            listOf(lengthCm, widthCm, heightCm).joinToString(" × ") { it.stripTrailingZeros().toPlainString() }
        else ""
    )
        private set
    val measurementInvalid: Boolean
        get() = measurementStr.isNotBlank() && CartonMeasurement.parseOrNull(measurementStr) == null

    fun updateMeasurement(value: String) {
        measurementStr = value
        val measurement = CartonMeasurement.parseOrNull(value)
        lengthCm = measurement?.lengthCm ?: BigDecimal.ZERO
        widthCm = measurement?.widthCm ?: BigDecimal.ZERO
        heightCm = measurement?.heightCm ?: BigDecimal.ZERO
        cbm = measurement?.cbm ?: BigDecimal.ZERO
    }

    var cartonNumber by mutableStateOf(cartonNumber)
    var grossWeightKgStr by mutableStateOf(grossWeightKgStr)
    var netWeightKgStr by mutableStateOf(netWeightKgStr)
    var customShippingMark by mutableStateOf(customShippingMark)
    fun netWeightKg(): BigDecimal = netWeightKgStr.trim().takeIf { it.isNotEmpty() }?.toBigDecimal()
        ?: grossWeightKgStr.toBigDecimal()
    val lines = mutableStateListOf<CartonLineState>().apply {
        addAll(initialLines)
    }
}

private fun ItemDraftState.toJson() = JSONObject().apply {
    put("product", productId); put("variant", variantId ?: ""); put("qty", quantityStr)
    put("price", unitPriceStr); put("cartons", cartonQuantityStr); put("pcs", pcsPerCartonStr)
    put("gross", grossWeightKgStr); put("net", netWeightKgStr)
}
private fun itemFromJson(j: JSONObject) = ItemDraftState(j.optString("product"), j.optString("variant").ifBlank { null },
    j.optString("qty"), j.optString("price"), j.optString("cartons"), j.optString("pcs"), j.optString("gross"), j.optString("net"))
private fun CartonBoxState.toJson() = JSONObject().apply {
    put("uid", uid); put("number", cartonNumber); put("gross", grossWeightKgStr); put("net", netWeightKgStr)
    put("measurement", measurementStr); put("customMark", customShippingMark)
    put("lines", JSONArray().apply { lines.forEach { line -> put(JSONObject().apply {
        put("product", line.productId); put("variant", line.variantId ?: ""); put("qty", line.quantityStr); put("weight", line.manualWeightKgStr)
    }) } })
}
private fun cartonFromJson(j: JSONObject): CartonBoxState {
    val a = j.getJSONArray("lines")
    return CartonBoxState(
        uid = j.optString("uid").ifBlank { java.util.UUID.randomUUID().toString() },
        cartonNumber = j.optString("number"),
        grossWeightKgStr = j.optString("gross"),
        netWeightKgStr = j.optString("net"),
        customShippingMark = j.optString("customMark", ""),
        initialLines = (0 until a.length()).map { i -> a.getJSONObject(i).let {
            CartonLineState(it.optString("product"), it.optString("variant").ifBlank { null }, it.optString("qty"), it.optString("weight"))
        } }).also { it.updateMeasurement(j.optString("measurement")) }
}
private val ItemListSaver = Saver<SnapshotStateList<ItemDraftState>, String>(
    save = { JSONArray().apply { it.forEach { item -> put(item.toJson()) } }.toString() },
    restore = { raw -> JSONArray(raw).let { a -> mutableStateListOf<ItemDraftState>().apply { repeat(a.length()) { add(itemFromJson(a.getJSONObject(it))) } } } }
)
private val CartonListSaver = Saver<SnapshotStateList<CartonBoxState>, String>(
    save = { JSONArray().apply { it.forEach { box -> put(box.toJson()) } }.toString() },
    restore = { raw -> JSONArray(raw).let { a -> mutableStateListOf<CartonBoxState>().apply { repeat(a.length()) { add(cartonFromJson(a.getJSONObject(it))) } } } }
)
private val FreightMapSaver = Saver<SnapshotStateMap<String, String>, String>(
    save = { JSONObject(it.toMap()).toString() },
    restore = { raw -> JSONObject(raw).let { j -> mutableStateMapOf<String, String>().apply { j.keys().forEach { put(it, j.getString(it)) } } } }
)

internal typealias ParsedPoNotes = com.pocketdev.advance_erp.core.util.ParsedPoNotes

internal fun parsePoNotesMetadata(rawNotes: String?): ParsedPoNotes =
    PoNotesMetadataParser.parse(rawNotes)

@Composable
fun CreatePurchaseOrderDialog(
    suppliers: List<Supplier>,
    products: List<Product>,
    variants: List<ProductVariant> = emptyList(),
    categories: List<ProductCategory> = emptyList(),
    isBangla: Boolean = false,
    onDismiss: () -> Unit,
    isSaving: Boolean = false,
    saveError: String? = null,
    metadataOnly: Boolean = false,
    onUpdateMetadata: ((notes: String?, attachments: String?) -> Unit)? = null,
    onAddNewSupplier: ((companyName: String, contactPerson: String, phone: String, address: String, country: String, currency: String, onCreated: (Supplier) -> Unit) -> Unit)? = null,
    onCreateFullProduct: ((
        nameEn: String, nameBn: String, baseSku: String, barcode: String?, categoryId: String, unit: String,
        purchasePriceMinor: Long, sellingPriceMinor: Long, minStockAlert: Int,
        productPhotos: List<GalleryItemDraft>, variants: List<NewVariantDraft>, description: String?,
        onCreated: (Product, List<ProductVariant>) -> Unit
    ) -> Unit)? = null,
    onGenerateSku: (suspend (categoryId: String, nameEn: String) -> String)? = null,
    onGenerateVariantSku: (suspend (baseSku: String, color: String?, size: String?) -> String)? = null,
    onGenerateBarcode: (suspend () -> String)? = null,
    onValidateSku: (suspend (sku: String, excludeId: String?) -> Boolean)? = null,
    onValidateBarcode: (suspend (barcode: String, excludeId: String?) -> Boolean)? = null,
    onAddNewVariant: ((productId: String, color: String, size: String, sku: String, costMinor: Long, priceMinor: Long, onCreated: (ProductVariant) -> Unit) -> Unit)? = null,
    onAddNewCategory: ((nameEn: String, nameBn: String, onCreated: (ProductCategory) -> Unit) -> Unit)? = null,
    editingOrder: PurchaseOrder? = null,
    initialEditingItems: List<PurchaseOrderItem> = emptyList(),
    initialEditingCartons: List<ShipmentCarton> = emptyList(),
    initialEditingCosts: List<ImportCostItem> = emptyList(),
    initialEditingFreightRates: Map<String, Long> = emptyMap(),
    initialShippingMethod: String = ShippingMethod.SEA.name,
    resolveProductImage: (suspend (String, String?) -> String?)? = null,
    onUpdatePo: ((
        orderId: String,
        supplierId: String,
        currency: String,
        exchangeRate: BigDecimal,
        items: List<PurchaseOrderItemDraft>,
        notes: String?,
        status: PurchaseOrderStatus,
        cartons: List<PoMixedCartonDraft>,
        chinaDeliveryChargeMinor: Long,
        categoryFreightRatesBdtMinor: Map<String, Long>,
        handlingCostMinor: Long,
        sampleCostMinor: Long,
        attachments: String?,
        orderDateMillis: Long,
        expectedArrivalDateMillis: Long?,
        shippingMethod: String
    ) -> Unit)? = null,
    onSavePo: (
        supplierId: String,
        currency: String,
        exchangeRate: BigDecimal,
        items: List<PurchaseOrderItemDraft>,
        notes: String?,
        status: PurchaseOrderStatus,
        cartons: List<PoMixedCartonDraft>,
        chinaDeliveryChargeMinor: Long,
        categoryFreightRatesBdtMinor: Map<String, Long>,
        handlingCostMinor: Long,
        sampleCostMinor: Long,
        attachments: String?,
        orderDateMillis: Long,
        expectedArrivalDateMillis: Long?,
        shippingMethod: String
    ) -> Unit
) {
    var currentStep by rememberSaveable(editingOrder?.id, metadataOnly) { mutableStateOf(if (metadataOnly) 3 else 0) }
    val bodyScroll = rememberLazyListState()
    LaunchedEffect(currentStep) { bodyScroll.scrollToItem(0) }
    val isEditing = editingOrder != null
    val parsedNotes = remember(editingOrder?.id, editingOrder?.notes) {
        parsePoNotesMetadata(editingOrder?.notes)
    }
    var isLocalPurchase by rememberSaveable(editingOrder?.id) { mutableStateOf(editingOrder?.currency == "BDT") }
    var selectedSupplierId by rememberSaveable(editingOrder?.id) { mutableStateOf(editingOrder?.supplierId ?: (suppliers.firstOrNull()?.id ?: "")) }
    var selectedCurrency by rememberSaveable(editingOrder?.id) {
        mutableStateOf(
            if (isLocalPurchase) "BDT"
            else editingOrder?.currency ?: (suppliers.firstOrNull()?.currency?.ifBlank { "CNY" } ?: "CNY")
        )
    }
    var exchangeRateStr by rememberSaveable(editingOrder?.id) {
        mutableStateOf(
            if (isLocalPurchase) "1.00"
            else editingOrder?.exchangeRateToBdt ?: (if (selectedCurrency == "USD") "120.00" else "17.00")
        )
    }
    var chinaLocalDeliveryStr by rememberSaveable(editingOrder?.id) { mutableStateOf("0") }
    var handlingCostStr by rememberSaveable(editingOrder?.id) { mutableStateOf("0") }
    var sampleCostStr by rememberSaveable(editingOrder?.id) { mutableStateOf("0") }
    var forwarderAddressStr by rememberSaveable(editingOrder?.id) { mutableStateOf(parsedNotes.forwarderAddress) }
    var attachmentsList by rememberSaveable(editingOrder?.id) { mutableStateOf(PoAttachmentStorage.parseAttachments(editingOrder?.attachments)) }
    var selectedPaymentTerm by rememberSaveable(editingOrder?.id) { mutableStateOf(parsedNotes.paymentTerm) }
    var defaultShippingMarkStr by rememberSaveable(editingOrder?.id) { mutableStateOf(parsedNotes.defaultShippingMark) }
    var notes by rememberSaveable(editingOrder?.id) { mutableStateOf(parsedNotes.userNotes) }
    var orderDateMillis by rememberSaveable(editingOrder?.id) { mutableStateOf(editingOrder?.orderDate ?: System.currentTimeMillis()) }
    var expectedArrivalDateMillis by rememberSaveable(editingOrder?.id) { mutableStateOf(editingOrder?.expectedArrivalDate) }
    var selectedShippingMethod by rememberSaveable(editingOrder?.id) { mutableStateOf(initialShippingMethod) }
    var supplierSearch by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Searchable Pickers & Bulk Entry state
    var showCatalogPickerSheet by remember { mutableStateOf(false) }
    var catalogPickerReplaceItemIdx by remember { mutableStateOf<Int?>(null) }
    var cartonPickerTargetIndex by remember { mutableStateOf<Int?>(null) }
    var cartonPickerReplaceLinePair by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showBulkCreateCartonsDialog by remember { mutableStateOf(false) }

    var hasHydratedCosts by rememberSaveable(editingOrder?.id) { mutableStateOf(false) }
    LaunchedEffect(editingOrder?.id, initialEditingCosts) {
        if (isEditing && initialEditingCosts.isNotEmpty() && !hasHydratedCosts) {
            val chinaDel = initialEditingCosts
                .filter { it.costCategory == CostCategory.CHINA_DOMESTIC_TRANSPORT.name }
                .sumOf { it.amountMinor }
            if (chinaDel > 0L) {
                chinaLocalDeliveryStr = "%.2f".format(chinaDel / 100.0)
            }
            val handling = initialEditingCosts
                .filter { it.description.contains("Handling", ignoreCase = true) }
                .sumOf { it.amountMinor }
            if (handling > 0L) {
                handlingCostStr = "%.2f".format(handling / 100.0)
            }
            val sample = initialEditingCosts
                .filter { it.description.contains("Sample", ignoreCase = true) }
                .sumOf { it.amountMinor }
            if (sample > 0L) {
                sampleCostStr = "%.2f".format(sample / 100.0)
            }
            hasHydratedCosts = true
        }
    }

    // Inline Add Supplier state
    var showInlineAddSupplierDialog by rememberSaveable { mutableStateOf(false) }
    var newSupCompany by rememberSaveable { mutableStateOf("") }
    var newSupContact by rememberSaveable { mutableStateOf("") }
    var newSupPhone by rememberSaveable { mutableStateOf("") }
    var newSupAddress by rememberSaveable { mutableStateOf("Guangzhou, China") }
    var newSupCountry by rememberSaveable { mutableStateOf("China") }
    var newSupCurrency by rememberSaveable { mutableStateOf("CNY") }

    // Inline Add Product state
    var showInlineAddProductDialog by rememberSaveable { mutableStateOf(false) }
    var newProdNameEn by rememberSaveable { mutableStateOf("") }
    var newProdNameBn by rememberSaveable { mutableStateOf("") }
    var newProdSku by rememberSaveable { mutableStateOf("") }
    var newProdUnit by rememberSaveable { mutableStateOf("Pair") }
    var newProdPurchasePriceStr by rememberSaveable { mutableStateOf("45.00") }
    var newProdSellingPriceStr by rememberSaveable { mutableStateOf("95.00") }
    var selectedCategoryId by remember(categories) { mutableStateOf(categories.firstOrNull()?.id ?: "cat-footwear") }

    // Inline Add Variant state
    var showInlineAddVariantForProductId by remember { mutableStateOf<String?>(null) }
    var targetItemIndexForVariant by remember { mutableStateOf<Int?>(null) }
    var newVarColor by rememberSaveable { mutableStateOf("") }
    var newVarSize by rememberSaveable { mutableStateOf("") }
    var newVarSku by rememberSaveable { mutableStateOf("") }
    var newVarCostStr by rememberSaveable { mutableStateOf("45.00") }
    var newVarPriceStr by rememberSaveable { mutableStateOf("95.00") }

    // Inline Add Category state
    var showInlineAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var newCatNameEn by rememberSaveable { mutableStateOf("") }
    var newCatNameBn by rememberSaveable { mutableStateOf("") }

    val firstProdId = products.firstOrNull()?.id ?: ""
    val firstVarId = variants.firstOrNull { it.productId == firstProdId }?.id

    fun PurchaseOrderItem.toItemDraftState(): ItemDraftState = ItemDraftState(
        productId = productId,
        variantId = variantId,
        quantityStr = quantity.toString(),
        unitPriceStr = "%.2f".format(unitPriceMinor / 100.0),
        cartonQuantityStr = cartonQuantity.toString(),
        pcsPerCartonStr = pcsPerCarton.toString(),
        grossWeightKgStr = grossWeightKg,
        netWeightKgStr = netWeightKg
    )

    fun List<ShipmentCarton>.toCartonBoxStates(): List<CartonBoxState> =
        groupBy { it.cartonNumber }
            .entries
            .sortedWith(compareBy<Map.Entry<String, List<ShipmentCarton>>> { PoPackingEngine.cartonSortKey(it.key) }.thenBy { it.key })
            .mapIndexed { idx, (cNum, cLines) ->
                val firstCtn = cLines.firstOrNull()
                val gwStr = firstCtn?.cartonGrossWeightKg?.toBigDecimalOrNull()
                    ?.takeIf { it > BigDecimal.ZERO }?.toPlainString()
                    ?: cLines.fold(BigDecimal.ZERO) { sum, line -> sum + (line.grossWeightKg.toBigDecimalOrNull() ?: BigDecimal.ZERO) }.toPlainString()
                val normalizedNum = PoPackingEngine.formatCartonNumber(idx + 1)
                val customMark = parsedNotes.cartonShippingMarks[cNum].orEmpty()
                    .ifBlank { parsedNotes.cartonShippingMarks[normalizedNum].orEmpty() }
                CartonBoxState(
                    cartonNumber = normalizedNum,
                    grossWeightKgStr = gwStr,
                    netWeightKgStr = cLines.fold(BigDecimal.ZERO) { sum, line -> sum + BigDecimal(line.netWeightKg.ifBlank { "0" }) }.toPlainString(),
                    lengthCm = firstCtn?.lengthCm?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    widthCm = firstCtn?.widthCm?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    heightCm = firstCtn?.heightCm?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    cbm = firstCtn?.cbm?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    customShippingMark = customMark,
                    initialLines = cLines.map { line ->
                        CartonLineState(
                            productId = line.productId,
                            variantId = line.variantId,
                            quantityStr = line.quantity.toString(),
                            manualWeightKgStr = line.grossWeightKg
                        )
                    }
                )
            }

    var hasHydratedItems by rememberSaveable(editingOrder?.id) { mutableStateOf(initialEditingItems.isNotEmpty()) }
    val itemsList = rememberSaveable(editingOrder?.id, saver = ItemListSaver) {
        mutableStateListOf<ItemDraftState>().apply {
            if (initialEditingItems.isNotEmpty()) {
                addAll(initialEditingItems.map { it.toItemDraftState() })
            } else if (!isEditing) {
                add(
                    ItemDraftState(
                        productId = firstProdId,
                        variantId = firstVarId,
                        quantityStr = "",
                        unitPriceStr = "",
                        cartonQuantityStr = "0",
                        pcsPerCartonStr = "",
                        grossWeightKgStr = "",
                        netWeightKgStr = ""
                    )
                )
            }
        }
    }
    LaunchedEffect(editingOrder?.id, initialEditingItems) {
        if (isEditing && initialEditingItems.isNotEmpty() && !hasHydratedItems) {
            itemsList.clear()
            itemsList.addAll(initialEditingItems.map { it.toItemDraftState() })
            hasHydratedItems = true
        }
    }

    // Carton-by-Carton Packaging list (supports mixed variants in a single carton)
    var hasHydratedCartons by rememberSaveable(editingOrder?.id) { mutableStateOf(initialEditingCartons.isNotEmpty()) }
    val cartonBoxes = rememberSaveable(editingOrder?.id, saver = CartonListSaver) {
        mutableStateListOf<CartonBoxState>().apply {
            if (initialEditingCartons.isNotEmpty()) {
                addAll(initialEditingCartons.toCartonBoxStates())
            }
        }
    }
    LaunchedEffect(editingOrder?.id, initialEditingCartons) {
        if (isEditing && initialEditingCartons.isNotEmpty() && !hasHydratedCartons) {
            cartonBoxes.clear()
            cartonBoxes.addAll(initialEditingCartons.toCartonBoxStates())
            hasHydratedCartons = true
        }
    }

    // Category Per-KG Freight Rate (BDT/KG) state map: categoryId -> rateStr (in BDT major units, e.g. "120")
    val categoryRateStrings = rememberSaveable(saver = FreightMapSaver) { mutableStateMapOf<String, String>() }
    LaunchedEffect(initialEditingFreightRates) {
        initialEditingFreightRates.forEach { (id, rate) ->
            if (!categoryRateStrings.containsKey(id)) {
                categoryRateStrings[id] = BigDecimal(rate).movePointLeft(2).toPlainString()
            }
        }
    }
    LaunchedEffect(categories, initialEditingFreightRates) {
        categories.forEach { cat ->
            if (!categoryRateStrings.containsKey(cat.id)) {
                val defaultMajor = BigDecimal(initialEditingFreightRates[cat.id] ?: cat.defaultFreightRatePerKgBdtMinor)
                    .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString()
                categoryRateStrings[cat.id] = defaultMajor
            }
        }
    }

    val draftContext = LocalContext.current
    val draftStore = remember(draftContext) { EditorDraftStore(draftContext) }
    val draftKey = "purchase:" + (editingOrder?.id ?: "new") + if (metadataOnly) ":metadata" else ""
    val draftScope = rememberCoroutineScope()
    var draftLoaded by rememberSaveable(draftKey) { mutableStateOf(false) }
    var restoredDraft by rememberSaveable(draftKey) { mutableStateOf(false) }
    var showLeavePrompt by rememberSaveable { mutableStateOf(false) }
    var savingBuffer by remember { mutableStateOf(false) }
    fun renumberAllCartons() {
        cartonBoxes.forEachIndexed { idx, box ->
            box.cartonNumber = PoPackingEngine.formatCartonNumber(idx + 1)
        }
    }

    val liveOrderLines = itemsList
        .filter { it.productId.isNotBlank() }
        .map { PackingOrderLine(VariantKey(it.productId, it.variantId), it.quantityStr.trim().toIntOrNull() ?: 0) }
    val liveCartonLines = cartonBoxes.flatMapIndexed { idx, box ->
        val uid = box.uid
        box.lines.filter { it.productId.isNotBlank() }.map { line ->
            PackingCartonLine(
                cartonUid = uid,
                key = VariantKey(line.productId, line.variantId),
                quantity = line.quantityStr.trim().toIntOrNull() ?: 0
            )
        }
    }
    val livePackingSummary = PoPackingEngine.summarize(liveOrderLines, liveCartonLines)
    val livePackedByVariantMap = livePackingSummary.variants.associate { it.key to it.packed }

    fun snapshotEditor(includeStep: Boolean = true) = JSONObject().apply {
        put("supplier", selectedSupplierId); put("currency", selectedCurrency); put("local", isLocalPurchase)
        put("rate", exchangeRateStr); put("delivery", chinaLocalDeliveryStr); put("handling", handlingCostStr)
        put("sample", sampleCostStr); put("forwarder", forwarderAddressStr); put("notes", notes)
        put("defaultShippingMark", defaultShippingMarkStr)
        put("poOrderDate", orderDateMillis); put("expectedArrivalDate", expectedArrivalDateMillis ?: JSONObject.NULL)
        put("shippingMethod", selectedShippingMethod)
        put("paymentTerm", selectedPaymentTerm.name); put("attachments", JSONArray(attachmentsList))
        put("items", JSONArray().apply { itemsList.forEach { put(it.toJson()) } })
        put("cartons", JSONArray().apply { cartonBoxes.forEach { put(it.toJson()) } })
        put("freight", JSONObject(categoryRateStrings.toMap()))
        if (includeStep) put("step", currentStep)
    }
    var initialSnapshot by rememberSaveable(draftKey) { mutableStateOf<String?>(null) }
    LaunchedEffect(draftKey) {
        if (!draftLoaded) {
            val draft = draftStore.read(draftKey)
            if (draft != null) {
                selectedSupplierId = draft.optString("supplier"); selectedCurrency = draft.optString("currency")
                isLocalPurchase = draft.optBoolean("local"); exchangeRateStr = draft.optString("rate")
                chinaLocalDeliveryStr = draft.optString("delivery"); handlingCostStr = draft.optString("handling")
                sampleCostStr = draft.optString("sample"); forwarderAddressStr = draft.optString("forwarder")
                defaultShippingMarkStr = draft.optString("defaultShippingMark", defaultShippingMarkStr)
                orderDateMillis = draft.optLong("poOrderDate", orderDateMillis)
                expectedArrivalDateMillis = if (draft.isNull("expectedArrivalDate")) null else draft.optLong("expectedArrivalDate")
                selectedShippingMethod = draft.optString("shippingMethod", selectedShippingMethod)
                notes = draft.optString("notes")
                selectedPaymentTerm = selectedPaymentTerm.javaClass.enumConstants.firstOrNull { it.name == draft.optString("paymentTerm") } ?: selectedPaymentTerm
                draft.optJSONArray("attachments")?.let { a -> attachmentsList = (0 until a.length()).map { a.getString(it) } }
                draft.optJSONArray("items")?.let { a -> itemsList.clear(); repeat(a.length()) { itemsList.add(itemFromJson(a.getJSONObject(it))) } }
                draft.optJSONArray("cartons")?.let { a -> cartonBoxes.clear(); repeat(a.length()) { cartonBoxes.add(cartonFromJson(a.getJSONObject(it))) }; renumberAllCartons() }
                draft.optJSONObject("freight")?.let { j -> categoryRateStrings.clear(); j.keys().forEach { categoryRateStrings[it] = j.getString(it) } }
                currentStep = draft.optInt("step").coerceIn(0, 3)
                hasHydratedItems = true; hasHydratedCartons = true; hasHydratedCosts = true
                restoredDraft = true
            }
            draftLoaded = true
            initialSnapshot = snapshotEditor(includeStep = false).toString()
        }
    }
    val navigateTab=com.pocketdev.advance_erp.core.designsystem.LocalERPOnNavigate.current
    var pendingTab by remember { mutableStateOf<com.pocketdev.advance_erp.core.designsystem.BottomNavDestination?>(null) }
    val finishDismiss: () -> Unit = { onDismiss(); pendingTab?.let { navigateTab?.invoke(it) } }
    val requestClose: () -> Unit = {
        if (!isSaving && !savingBuffer) {
            if (snapshotEditor(includeStep = false).toString() != initialSnapshot) showLeavePrompt = true else finishDismiss()
        }
    }
    if (showLeavePrompt) ERPLeaveEditorDialog(
        isBangla = isBangla,
        isSaving = savingBuffer,
        onContinue = { showLeavePrompt = false; pendingTab=null },
        onDiscard = { draftStore.remove(draftKey); showLeavePrompt = false; finishDismiss() },
        onSaveDraft = {
            if (!savingBuffer) {
                val snapshot = snapshotEditor()
                savingBuffer = true
                draftScope.launch {
                    try {
                        withContext(Dispatchers.IO) { draftStore.save(draftKey, snapshot) }
                        finishDismiss()
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        errorMessage = e.message
                        showLeavePrompt = false
                    } finally { savingBuffer = false }
                }
            }
        }
    )

    fun variantLabel(v: ProductVariant?): String {
        if (v == null) return if (isBangla) "ডিফল্ট / ভ্যারিয়েন্ট ছাড়া" else "Default / Base"
        val parts = listOfNotNull(
            v.color?.takeIf { it.isNotBlank() },
            v.size?.takeIf { it.isNotBlank() }
        )
        return if (parts.isNotEmpty()) "${parts.joinToString(" / ")} (${v.sku})" else v.sku
    }

    fun serializedNotes(): String {
        val customMarks = LinkedHashMap<String, String>()
        cartonBoxes.forEachIndexed { idx, box ->
            val mark = box.customShippingMark.trim()
            if (mark.isNotEmpty()) {
                customMarks[PoPackingEngine.formatCartonNumber(idx + 1)] = mark
            }
        }
        return PoNotesMetadataParser.serialize(
            paymentTermLabel = selectedPaymentTerm.labelEn,
            forwarderAddress = forwarderAddressStr,
            userNotes = notes,
            defaultShippingMark = defaultShippingMarkStr,
            cartonShippingMarks = customMarks
        )
    }

    fun submitPo(targetStatus: PurchaseOrderStatus) {
        try {
            if (metadataOnly) {
                requireNotNull(onUpdateMetadata) { "Metadata save is not available" }(
                    serializedNotes(), PoAttachmentStorage.serializeAttachments(attachmentsList))
                errorMessage = null
                return
            }
            require(selectedSupplierId.isNotBlank()) {
                if (isBangla) "দয়া করে একজন সরবরাহকারী নির্বাচন করুন।" else "Please select a supplier."
            }
            val rate = BigDecimal(exchangeRateStr.trim().ifBlank { "0" })
            require(rate > BigDecimal.ZERO) {
                if (isBangla) "বিনিময় হার শূন্যের বেশি হতে হবে।" else "Exchange rate must be positive."
            }
            require(itemsList.isNotEmpty()) {
                if (isBangla) "অন্তত একটি পণ্য তালিকায় যোগ করুন।" else "Please add at least one item."
            }
            require(itemsList.map { it.productId to it.variantId }.distinct().size == itemsList.size) {
                if (isBangla) "অর্ডারে একই ভ্যারিয়েন্ট একাধিকবার রাখা যাবে না। সংশ্লিষ্ট লাইনের পরিমাণ আপডেট করুন।"
                else "Duplicate variants exist in Order Items. Combine them into one PO line."
            }

            // Option A: Never reduce ordered qty below already allocated carton qty
            itemsList.forEach { state ->
                val orderedQty = state.quantityStr.trim().toIntOrNull() ?: 0
                val check = PoPackingEngine.checkOrderQuantityChange(
                    VariantKey(state.productId, state.variantId),
                    orderedQty,
                    liveCartonLines
                )
                require(check.allowed) {
                    if (isBangla)
                        "এই ভ্যারিয়েন্টের ${check.packed} PCS ইতোমধ্যেই কার্টনে বরাদ্দ করা আছে। PO পরিমাণ কমানোর আগে অনুগ্রহ করে কার্টন প্যাকিং সমন্বয় করুন।"
                    else
                        "This variant already has ${check.packed} PCS allocated to cartons. Please adjust the carton packing before reducing the PO quantity."
                }
            }

            // Enforce PoPackingEngine draft vs confirm rules when cartons are used or confirming
            val draftBlockers = PoPackingEngine.draftBlockers(liveOrderLines, liveCartonLines)
            if (draftBlockers.isNotEmpty()) {
                val firstIssue = draftBlockers.first()
                val issueMsg = when (firstIssue.type) {
                    PackingIssueType.OVERPACKED -> {
                        val prodName = products.find { it.id == firstIssue.key?.productId }?.nameEn ?: "Variant"
                        if (isBangla)
                            "$prodName: কার্টনে মোট ${firstIssue.packed} PCS প্যাক করা হয়েছে, যা অর্ডারের ${firstIssue.ordered} PCS-এর চেয়ে বেশি।"
                        else
                            "$prodName: Packed quantity (${firstIssue.packed} PCS) exceeds PO quantity (${firstIssue.ordered} PCS)."
                    }
                    PackingIssueType.DUPLICATE_VARIANT_IN_CARTON ->
                        if (isBangla) "একই কার্টনে একই ভ্যারিয়েন্ট একাধিকবার যোগ করা যাবে না।"
                        else "Duplicate variant detected inside the same carton."
                    PackingIssueType.INVALID_QUANTITY ->
                        if (isBangla) "প্রতিটি কার্টন লাইনের পরিমাণ ১ বা তার বেশি হতে হবে।"
                        else "Every carton allocation quantity must be at least 1 PCS."
                    PackingIssueType.UNORDERED_VARIANT ->
                        if (isBangla) "প্যাকিংয়ের সব ভ্যারিয়েন্ট আগে পণ্যের তালিকায় থাকতে হবে।"
                        else "Add each packed variant to Order Items first."
                    else -> if (isBangla) "কার্টন প্যাকিং তথ্য যাচাই করুন।" else "Please fix invalid carton packing entries."
                }
                throw IllegalArgumentException(issueMsg)
            }

            val unallocatedQty = PoPackingEngine.unallocatedQuantity(liveOrderLines, liveCartonLines)
            if (targetStatus == PurchaseOrderStatus.CONFIRMED) {
                val blockers = PoPackingEngine.confirmBlockers(liveOrderLines, liveCartonLines)
                val usesItemCountFallback = cartonBoxes.isEmpty() && itemsList.all { (it.cartonQuantityStr.trim().toIntOrNull() ?: 0) > 0 }
                if (blockers.isNotEmpty() && !usesItemCountFallback) {
                    throw IllegalArgumentException(
                        if (isBangla)
                            "প্যাকিং অসম্পূর্ণ। এখনও $unallocatedQty PCS বরাদ্দ বাকি আছে। নিশ্চিত করার আগে সব ভ্যারিয়েন্ট কার্টনে প্যাক করুন অথবা খসড়া (Draft) সংরক্ষণ করুন।"
                        else
                            "Packing incomplete. $unallocatedQty PCS remain unallocated. Allocate all variants to cartons before confirming, or save as Draft."
                    )
                }
            } else if (targetStatus == PurchaseOrderStatus.DRAFT && unallocatedQty > 0) {
                android.widget.Toast.makeText(
                    draftContext,
                    if (isBangla) "প্যাকিং অসম্পূর্ণ। $unallocatedQty PCS এখনও বরাদ্দ বাকি আছে।"
                    else "Packing incomplete. $unallocatedQty PCS remain unallocated.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }

            if (cartonBoxes.isNotEmpty()) {
                renumberAllCartons()
            }

            // Calculate per-item weight, pcs quantity & carton share from cartonBoxes if cartonBoxes are used
            val lineNetWeightFromCartons = mutableMapOf<Pair<String, String?>, BigDecimal>()
            val lineWeightFromCartons = mutableMapOf<Pair<String, String?>, BigDecimal>()
            val lineCartonCountFromCartons = mutableMapOf<Pair<String, String?>, Int>()
            val lineQuantityFromCartons = mutableMapOf<Pair<String, String?>, Int>()

            if (cartonBoxes.isNotEmpty()) {
                require(cartonBoxes.map { it.cartonNumber.trim() }.distinct().size == cartonBoxes.size) {
                    if (isBangla) "প্রতিটি কার্টনের নম্বর আলাদা দিন।" else "Each carton needs a unique number."
                }
                cartonBoxes.forEach { box ->
                    require(!box.measurementInvalid) {
                        if (isBangla) "${box.cartonNumber}: মাপ 40*50*40 আকারে দিন অথবা খালি রাখুন।"
                        else "${box.cartonNumber}: enter dimensions as 40*50*40 or leave blank."
                    }
                    val boxGwBd = box.grossWeightKgStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                    require(boxGwBd > BigDecimal.ZERO) {
                        if (isBangla) "${box.cartonNumber}: গ্রস ওজন শূন্যের বেশি হতে হবে (KG)।"
                        else "${box.cartonNumber}: Carton gross weight must be greater than zero."
                    }
                    val allocated = CartonWeightAllocationEngine.allocateWeights(
                        boxGwBd, box.netWeightKg(),
                        box.lines.map { line ->
                            require(itemsList.any { it.productId == line.productId && it.variantId == line.variantId }) {
                                if (isBangla) "প্যাকিংয়ের প্রতিটি ভ্যারিয়েন্ট আগে পণ্যের তালিকায় যোগ করুন।" else "Add each packed variant to Order Items first."
                            }
                            MixedCartonLineInput(line.productId, line.variantId, quantity = line.quantityStr.toInt(),
                                manualGrossWeightKg = line.manualWeightKgStr.trim().takeIf { it.isNotEmpty() }?.toBigDecimal())
                        }
                    )
                    allocated.forEach { line ->
                        val key = line.productId to line.variantId
                        lineWeightFromCartons[key] = (lineWeightFromCartons[key] ?: BigDecimal.ZERO) + line.allocatedGrossWeightKg
                        lineNetWeightFromCartons[key] = (lineNetWeightFromCartons[key] ?: BigDecimal.ZERO) + line.allocatedNetWeightKg
                        lineCartonCountFromCartons[key] = (lineCartonCountFromCartons[key] ?: 0) + 1
                        lineQuantityFromCartons[key] = (lineQuantityFromCartons[key] ?: 0) + line.quantity
                    }
                }
            }

            val drafts = itemsList.mapIndexed { idx, state ->
                require(state.productId.isNotBlank()) {
                    if (isBangla) "আইটেম #${idx + 1}: পণ্য নির্বাচন করুন।" else "Item #${idx + 1}: Please select a product."
                }
                val key = state.productId to state.variantId
                val cartonPackedQty = lineQuantityFromCartons[key]
                val qty = state.quantityStr.trim().toIntOrNull() ?: 0
                require((cartonPackedQty ?: 0) <= qty) {
                    if (isBangla) "আইটেম #${idx + 1}: প্যাক করা সংখ্যা অর্ডারের পরিমাণ ছাড়িয়েছে।" else "Item #${idx + 1}: Packed quantity exceeds ordered quantity."
                }
                require(qty > 0) {
                    if (isBangla) "আইটেম #${idx + 1}: পরিমাণ ১ বা তার বেশি হতে হবে।" else "Item #${idx + 1}: Quantity must be at least 1."
                }
                val priceBd = BigDecimal(state.unitPriceStr.trim().ifEmpty { "0" })
                require(priceBd > BigDecimal.ZERO) {
                    if (isBangla) "আইটেম #${idx + 1}: একক মূল্য শূন্যের বেশি হতে হবে।" else "Item #${idx + 1}: Unit price must be positive."
                }
                val priceMinor = priceBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                val ctnQty = lineCartonCountFromCartons[key] ?: (state.cartonQuantityStr.trim().toIntOrNull() ?: 0)
                val pcsPerCtn = state.pcsPerCartonStr.trim().toIntOrNull() ?: (if (ctnQty > 0) qty / ctnQty else qty)
                val grossKg = lineWeightFromCartons[key] ?: (state.grossWeightKgStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO)
                val netKg = if (cartonBoxes.isNotEmpty()) lineNetWeightFromCartons[key] ?: BigDecimal.ZERO else state.netWeightKgStr.trim().toBigDecimalOrNull() ?: grossKg
                require(grossKg >= BigDecimal.ZERO && netKg >= BigDecimal.ZERO && netKg <= grossKg) {
                    if (isBangla) "নেট ওজন গ্রস ওজনের বেশি হতে পারে না।" else "Net weight cannot exceed gross weight."
                }

                PurchaseOrderItemDraft(
                    productId = state.productId,
                    variantId = state.variantId,
                    quantity = qty,
                    unitPriceMinor = priceMinor,
                    cartonQuantity = ctnQty,
                    pcsPerCarton = pcsPerCtn,
                    grossWeightKg = grossKg,
                    netWeightKg = netKg
                )
            }

            // Build Carton Drafts to auto-sync to Import Shipment
            val finalCartonDrafts = if (cartonBoxes.isNotEmpty()) {
                cartonBoxes.mapIndexedNotNull { idx, box ->
                    val gw = box.grossWeightKgStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val validLines = box.lines.mapNotNull { l ->
                        val q = l.quantityStr.trim().toIntOrNull() ?: 0
                        if (l.productId.isNotBlank() && q > 0) {
                            val prod = products.find { it.id == l.productId }
                            val v = variants.find { it.id == l.variantId }
                            MixedCartonLineDraft(
                                productId = l.productId,
                                variantId = l.variantId,
                                productName = listOfNotNull(prod?.nameEn, v?.color, v?.size).joinToString(" - "),
                                quantity = q,
                                manualGrossWeightKg = l.manualWeightKgStr.trim().takeIf { it.isNotEmpty() }?.toBigDecimal()
                            )
                        } else null
                    }
                    if (validLines.isNotEmpty()) {
                        PoMixedCartonDraft(
                            cartonNumber = PoPackingEngine.formatCartonNumber(idx + 1),
                            grossWeightKg = gw,
                            netWeightKg = box.netWeightKg(),
                            lengthCm = box.lengthCm, widthCm = box.widthCm, heightCm = box.heightCm,
                            cbm = box.cbm,
                            lines = validLines,
                            cartonUid = box.uid,
                            shippingMark = box.customShippingMark.trim().takeIf { it.isNotEmpty() }
                        )
                    } else null
                }
            } else {
                var cartonIndex = 0
                drafts.flatMap { d ->
                    val count = d.cartonQuantity
                    require(count >= 0 && count <= d.quantity) { "Carton count must be between zero and item quantity" }
                    if (count == 0) emptyList() else {
                        require(d.grossWeightKg > BigDecimal.ZERO) { "Enter gross weight for packed items" }
                        val gross = d.grossWeightKg.setScale(3, RoundingMode.UNNECESSARY).unscaledValue().longValueExact()
                        val net = d.netWeightKg.setScale(3, RoundingMode.UNNECESSARY).unscaledValue().longValueExact()
                        List(count) { index ->
                            val quantity = d.quantity / count + if (index < d.quantity % count) 1 else 0
                            val gw = BigDecimal.valueOf(gross / count + if (index < gross % count) 1 else 0, 3)
                            val nw = BigDecimal.valueOf(net / count + if (index < net % count) 1 else 0, 3)
                            PoMixedCartonDraft(
                                cartonNumber = String.format(java.util.Locale.ROOT, "CTN-%02d", ++cartonIndex),
                                grossWeightKg = gw, netWeightKg = nw,
                                lines = listOf(MixedCartonLineDraft(d.productId, d.variantId, quantity = quantity))
                            )
                        }
                    }
                }
            }

            val chinaDelivBd = chinaLocalDeliveryStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
            val chinaDeliveryMinor = chinaDelivBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact().coerceAtLeast(0L)

            val catRatesMinorMap = mutableMapOf<String, Long>()
            categoryRateStrings.forEach { (catId, rateStr) ->
                val rBd = if (isLocalPurchase) BigDecimal.ZERO else rateStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                if (rBd >= BigDecimal.ZERO) {
                    catRatesMinorMap[catId] = rBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                }
            }

            val handlingBd = handlingCostStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
            val handlingMinor = handlingBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact().coerceAtLeast(0L)

            val sampleBd = sampleCostStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
            val sampleMinor = sampleBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact().coerceAtLeast(0L)

            val combinedNotes = serializedNotes()

            val serializedAttachments = PoAttachmentStorage.serializeAttachments(attachmentsList)
            if (isEditing && onUpdatePo != null && editingOrder != null) {
                onUpdatePo(
                    editingOrder.id,
                    selectedSupplierId,
                    selectedCurrency,
                    rate,
                    drafts,
                    combinedNotes,
                    targetStatus,
                    finalCartonDrafts,
                    chinaDeliveryMinor,
                    catRatesMinorMap,
                    handlingMinor,
                    sampleMinor,
                    serializedAttachments,
                    orderDateMillis,
                    expectedArrivalDateMillis,
                    selectedShippingMethod
                )
            } else {
                onSavePo(
                    selectedSupplierId,
                    selectedCurrency,
                    rate,
                    drafts,
                    combinedNotes,
                    targetStatus,
                    finalCartonDrafts,
                    chinaDeliveryMinor,
                    catRatesMinorMap,
                    handlingMinor,
                    sampleMinor,
                    serializedAttachments,
                    orderDateMillis,
                    expectedArrivalDateMillis,
                    selectedShippingMethod
                )
            }
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = e.message ?: if (isBangla) "তথ্য যাচাই ব্যর্থ হয়েছে" else "Validation failed"
        }
    }

    PoReferenceTheme {
    ERPFullScreenEditor(
        onDismissRequest = requestClose
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(0.dp)),
            color = ERPBackground,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical=4.dp)
            ) {
                Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal=16.dp)) {
                PoTopBar(
                    title=if(isEditing) { if(isBangla) "ক্রয়াদেশ সম্পাদনা" else "Edit purchase order" } else if(isBangla) "নতুন ক্রয়াদেশ" else "New purchase order",
                    subtitle=if(isBangla) "তথ্য → পণ্য → প্যাকিং → হিসাব" else "Information → Products → Packing → Cost",
                    onBack=requestClose,
                    actions={ IconButton(onClick=requestClose,enabled=!isSaving&&!savingBuffer) { Icon(Icons.Outlined.Close,contentDescription=if(isBangla) "বন্ধ করুন" else "Close") } }
                )

                if (restoredDraft) Text(if (isBangla) "সংরক্ষিত খসড়া থেকে চালু হয়েছে" else "Resumed saved draft",
                    color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                if (!metadataOnly) {
                    PremiumPoStepper(
                        labels = if (isBangla) listOf("তথ্য", "পণ্য", "প্যাকিং", "হিসাব") else listOf("Info", "Items", "Pack", "Cost"),
                        currentStep = currentStep,
                        enabled = !isSaving,
                        onStepSelected = { currentStep = it },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Scrollable Body
                LazyColumn(
                    state = bodyScroll,
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ==========================================
                    if (currentStep == 0) {
                        item {
                            var supplierPicker by rememberSaveable { mutableStateOf(false) }
                            var purchaseTypePicker by rememberSaveable { mutableStateOf(false) }
                            var currencyPicker by rememberSaveable { mutableStateOf(false) }
                            var shippingExpanded by rememberSaveable { mutableStateOf(false) }
                            Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
                                Row(verticalAlignment=Alignment.CenterVertically) {
                                    Text(if(isBangla) "ক্রয়াদেশের মূল তথ্য" else "Purchase information", style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                                }
                                PoValueCard(if(isBangla) "সরবরাহকারী *" else "Supplier *", suppliers.find { it.id==selectedSupplierId }?.companyName ?: if(isBangla) "নির্বাচন করুন" else "Select supplier", Icons.Outlined.Inventory2,onClick={supplierPicker=true},trailing=Icons.Outlined.ChevronRight)
                                PoValueCard(if(isBangla) "PO নম্বর" else "PO number",editingOrder?.poNumber ?: if(isBangla) "সংরক্ষণে স্বয়ংক্রিয় নম্বর" else "Assigned when saved",Icons.Outlined.Inventory2)
                                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    PremiumPoDateField(if(isBangla) "PO তারিখ *" else "PO date *",orderDateMillis,if(isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH,Modifier.weight(1f),onDateSelected={it?.let { date -> orderDateMillis=date }})
                                    PremiumPoDateField(if(isBangla) "প্রত্যাশিত আগমন" else "Expected arrival",expectedArrivalDateMillis,if(isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH,Modifier.weight(1f),allowClear=true,onDateSelected={expectedArrivalDateMillis=it})
                                }
                                PoValueCard(if(isBangla) "ক্রয় ধরন" else "Purchase type",if(isLocalPurchase) { if(isBangla) "স্থানীয় ক্রয়" else "Local purchase" } else { if(isBangla) "আমদানি (Import)" else "Import" },Icons.Outlined.Payments,onClick={purchaseTypePicker=true},trailing=Icons.Outlined.ChevronRight)
                                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                                    PoValueCard(if(isBangla) "মুদ্রা" else "Currency",selectedCurrency,Icons.Outlined.Payments,Modifier.weight(.42f),onClick={if(!isLocalPurchase)currencyPicker=true},trailing=Icons.Outlined.ExpandMore,showIcon=false)
                                    OutlinedTextField(exchangeRateStr,{exchangeRateStr=it},modifier=Modifier.weight(.58f).testTag("po_exchange_rate"),label={Text("1 $selectedCurrency = BDT")},singleLine=true,shape=RoundedCornerShape(13.dp),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),readOnly=isLocalPurchase,textStyle=MaterialTheme.typography.bodyMedium)
                                }
                                Text(if(isBangla) "পেমেন্ট শর্ত (Payment Terms)" else "Payment terms",style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
                                LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                                    items(PaymentTermType.values().toList()) { term ->
                                        FilterChip(selected=selectedPaymentTerm==term,onClick={selectedPaymentTerm=term},label={Text(if(isBangla)term.labelBn else term.labelEn)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=PrimaryPurple,selectedLabelColor=MaterialTheme.colorScheme.onPrimary))
                                    }
                                }
                                OutlinedTextField(notes,{notes=it},label={Text(if(isBangla) "নোট / বিবরণ (ঐচ্ছিক)" else "Notes / reference (optional)")},modifier=Modifier.fillMaxWidth(),minLines=2,shape=RoundedCornerShape(13.dp))
                                TextButton(onClick={shippingExpanded=!shippingExpanded},modifier=Modifier.fillMaxWidth()) { Text(if(isBangla) "ডেলিভারি, শিপিং ও অতিরিক্ত তথ্য" else "Delivery, shipping & additional information"); Icon(if(shippingExpanded)Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,null) }
                                if(shippingExpanded) {
                                    LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) { items(ShippingMethod.values().toList()) { method -> FilterChip(selected=selectedShippingMethod==method.name,onClick={selectedShippingMethod=method.name},label={Text(if(isBangla)method.labelBn else method.labelEn)}) } }
                                    OutlinedTextField(forwarderAddressStr,{forwarderAddressStr=it},label={Text(if(isBangla) "ফরোয়ার্ডার / ডেলিভারি ঠিকানা ও ফোন" else "Forwarder / delivery address & phone")},modifier=Modifier.fillMaxWidth(),minLines=2,shape=RoundedCornerShape(13.dp))
                                    OutlinedTextField(
                                        value = defaultShippingMarkStr,
                                        onValueChange = { defaultShippingMarkStr = it },
                                        label = { Text(if (isBangla) "ডিফল্ট শিপিং মার্ক (Default Shipping Mark)" else "Default Shipping Mark (PO Level)") },
                                        placeholder = { Text("TUKTAK BD / PO-2026-004 / CTG PORT / {CTN}") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        shape = RoundedCornerShape(13.dp)
                                    )
                                    PoNumberInput(if(isBangla) "ডেলিভারি খরচ ($selectedCurrency)" else "Delivery charge ($selectedCurrency)",chinaLocalDeliveryStr,{chinaLocalDeliveryStr=it})
                                    val converted=(chinaLocalDeliveryStr.toBigDecimalOrNull() ?: BigDecimal.ZERO).multiply(exchangeRateStr.toBigDecimalOrNull() ?: BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP)
                                    Text(if(isBangla) "বাংলাদেশি টাকায়: ৳ $converted" else "Converted to BDT: ৳ $converted",color=SecondaryTeal,style=MaterialTheme.typography.bodySmall)
                                    if(isLocalPurchase) PoNumberInput(if(isBangla) "আনলোডিং / শ্রম খরচ (৳)" else "Unloading / labour (BDT)",handlingCostStr,{handlingCostStr=it})
                                }
                            }
                            if(supplierPicker) {
                                var query by rememberSaveable { mutableStateOf("") }
                                AlertDialog(onDismissRequest={supplierPicker=false},title={Text(if(isBangla) "সরবরাহকারী নির্বাচন" else "Select supplier")},text={Column { OutlinedTextField(query,{query=it},label={Text(if(isBangla) "খুঁজুন" else "Search")},singleLine=true); LazyColumn(Modifier.heightIn(max=330.dp)) { items(suppliers.filter { it.companyName.contains(query,true)||it.phone.contains(query,true) }) { supplier -> TextButton(onClick={selectedSupplierId=supplier.id;if(isLocalPurchase){selectedCurrency="BDT";exchangeRateStr="1.00"}else if(supplier.currency.isNotBlank()){selectedCurrency=supplier.currency;exchangeRateStr=when(supplier.currency){"CNY"->"17.00";"USD"->"120.00";else->"1.00"}};supplierPicker=false},modifier=Modifier.fillMaxWidth()) {Text(supplier.companyName)} } } }},confirmButton={Row {if(onAddNewSupplier!=null) TextButton(onClick={supplierPicker=false;showInlineAddSupplierDialog=true}){Text(if(isBangla) "+ নতুন সরবরাহকারী" else "+ New supplier")};TextButton(onClick={supplierPicker=false}){Text(if(isBangla) "বন্ধ" else "Close")}}})
                            }
                            if(purchaseTypePicker) AlertDialog(onDismissRequest={purchaseTypePicker=false},title={Text(if(isBangla) "ক্রয় ধরন" else "Purchase type")},text={Column { TextButton(onClick={isLocalPurchase=false;if(selectedCurrency=="BDT"){selectedCurrency="CNY";exchangeRateStr="17.00"};purchaseTypePicker=false}){Text(if(isBangla) "আমদানি" else "Import")};TextButton(onClick={isLocalPurchase=true;selectedCurrency="BDT";exchangeRateStr="1.00";purchaseTypePicker=false}){Text(if(isBangla) "স্থানীয়" else "Local")} }},confirmButton={})
                            if(currencyPicker) AlertDialog(onDismissRequest={currencyPicker=false},title={Text(if(isBangla) "মুদ্রা" else "Currency")},text={Column { listOf("CNY","USD").forEach { currency -> TextButton(onClick={selectedCurrency=currency;exchangeRateStr=if(currency=="CNY")"17.00" else "120.00";currencyPicker=false}){Text(currency)} } }},confirmButton={})
                        }
                    }
                    if (currentStep == 1) {
                    // 3. Order Items + Searchable Product & Variant Selection + Option A Validation
                    // ==========================================
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBangla) "পণ্য ও ভ্যারিয়েন্ট" else "Order items",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                if (itemsList.isNotEmpty()) {
                                    Text(
                                        text = "${itemsList.size} ${if (isBangla) "লাইন" else "lines"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (onCreateFullProduct != null) {
                                    OutlinedButton(
                                        onClick = { showInlineAddProductDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(0.9f).heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = "New Product", tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isBangla) "নতুন পণ্য" else "New product",
                                            color = PrimaryPurple,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        catalogPickerReplaceItemIdx = null
                                        showCatalogPickerSheet = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple, contentColor = MaterialTheme.colorScheme.onPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1.4f).heightIn(min = 48.dp).testTag("po_open_product_picker")
                                ) {
                                    Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isBangla) "পণ্য ও ভ্যারিয়েন্ট নির্বাচন" else "Search & Add Products",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    if(products.isNotEmpty()) item {
                        Surface(shape=RoundedCornerShape(12.dp),color=if(com.pocketdev.advance_erp.core.designsystem.LocalIsDarkTheme.current)Color(0xFF39352A) else Color(0xFFFFF4D8)) {
                            Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Info,null,Modifier.size(19.dp),tint=StatusWarning)
                                Text(if(isBangla) "উপরের সার্চ পিকার দিয়ে একসাথে একাধিক পণ্য/ভ্যারিয়েন্ট যোগ করুন, অথবা পেন্সিল আইকনে ক্লিক করে পরিমাণ ও মূল্য সম্পাদনা করুন।" else "Use 'Search & Add Products' to pick multiple variants by Name, SKU, or Barcode. Tap edit to adjust qty & price.",style=MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (products.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = StatusWarningContainer),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Info, contentDescription = null, tint = StatusWarning)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isBangla) "কোনো পণ্য পাওয়া যায়নি। ওপরের 'নতুন পণ্য' বোতামে ক্লিক করুন।" else "No products found. Click 'New product' above to create one.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StatusWarning
                                    )
                                }
                            }
                        }
                    } else if (itemsList.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ERPSurfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ERPDivider)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Info, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isBangla) "কোনো পণ্য লাইন এখনও যোগ করা হয়নি। ওপরের সার্চ পিকার দিয়ে পণ্য যোগ করুন।" else "No product lines added yet. Tap 'Search & Add Products' above.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(itemsList) { index, itemState ->
                        val productVariants = variants.filter { it.productId == itemState.productId }
                        val selectedProd = products.find { it.id == itemState.productId }
                        val selectedCat = categories.find { it.id == selectedProd?.categoryId }
                        val vKey = VariantKey(itemState.productId, itemState.variantId)
                        val packedForItem = livePackedByVariantMap[vKey] ?: 0
                        val typedOrderedQty = itemState.quantityStr.trim().toIntOrNull() ?: 0
                        val qtyReductionCheck = PoPackingEngine.checkOrderQuantityChange(vKey, typedOrderedQty, liveCartonLines)
                        val optionAError = !qtyReductionCheck.allowed && typedOrderedQty >= 0 && itemState.quantityStr.isNotBlank()

                        var expanded by rememberSaveable(index) { mutableStateOf(false) }
                        var imageUri by remember(itemState.productId,itemState.variantId) { mutableStateOf<String?>(null) }
                        LaunchedEffect(itemState.productId,itemState.variantId) { imageUri=resolveProductImage?.invoke(itemState.productId,itemState.variantId) }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (optionAError) 1.5.dp else 1.dp,
                                color = if (optionAError) StatusError else PrimaryPurple.copy(alpha = 0.18f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                                    Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.surfaceVariant) {
                                        if(imageUri!=null) AsyncImage(com.pocketdev.advance_erp.core.designsystem.rememberImageModel(imageUri),null,Modifier.size(56.dp),contentScale=ContentScale.Crop)
                                        else Icon(Icons.Outlined.Inventory2,null,Modifier.padding(13.dp).size(28.dp),tint=PrimaryPurple)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(selectedProd?.let { if(isBangla&&it.nameBn.isNotBlank())it.nameBn else it.nameEn }.orEmpty(),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.bodyMedium)
                                        Text("SKU: ${variants.find { it.id==itemState.variantId }?.sku ?: selectedProd?.baseSku.orEmpty()}",style=MaterialTheme.typography.labelSmall,color=TextSecondary)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            variants.find { it.id==itemState.variantId }?.let { variant ->
                                                Surface(shape=RoundedCornerShape(6.dp),color=SecondaryTeal.copy(alpha=.12f)) {
                                                    Text(variantLabel(variant),Modifier.padding(horizontal=5.dp,vertical=2.dp),style=MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            if (packedForItem > 0) {
                                                Surface(shape = RoundedCornerShape(6.dp), color = PrimaryPurple.copy(alpha = 0.12f)) {
                                                    Text(
                                                        text = if (isBangla) "প্যাকড: $packedForItem" else "Packed: $packedForItem",
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = PrimaryPurple
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    IconButton(onClick={expanded=!expanded},modifier=Modifier.size(48.dp)) {Icon(Icons.Outlined.Edit,if(isBangla) "সম্পাদনা" else "Edit item",tint=PrimaryPurple)}
                                    IconButton(
                                        onClick = {
                                            if (packedForItem > 0) {
                                                errorMessage = if (isBangla)
                                                    "এই ভ্যারিয়েন্টের $packedForItem PCS ইতোমধ্যেই কার্টনে বরাদ্দ করা আছে। আইটেম মুছতে আগে কার্টন প্যাকিং থেকে সরান।"
                                                else
                                                    "This variant already has $packedForItem PCS allocated to cartons. Please adjust the carton packing before deleting."
                                            } else {
                                                itemsList.removeAt(index)
                                            }
                                        },
                                        modifier=Modifier.size(48.dp)
                                    ) {Icon(Icons.Outlined.Delete,if(isBangla) "মুছুন" else "Delete item",tint=StatusError)}
                                }
                                Row(Modifier.fillMaxWidth().padding(top=7.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                                    Text("$selectedCurrency ${itemState.unitPriceStr}",style=MaterialTheme.typography.labelLarge)
                                    Text("${itemState.quantityStr} ${selectedProd?.unit ?: "PCS"}",style=MaterialTheme.typography.labelLarge)
                                    Text("$selectedCurrency ${(itemState.unitPriceStr.toBigDecimalOrNull() ?: BigDecimal.ZERO).multiply(itemState.quantityStr.toBigDecimalOrNull() ?: BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP)}",style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
                                }
                                if (optionAError) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(shape = RoundedCornerShape(8.dp), color = StatusError.copy(alpha = 0.1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isBangla)
                                                    "এই ভ্যারিয়েন্টের $packedForItem PCS ইতোমধ্যেই কার্টনে বরাদ্দ আছে। পরিমাণ কমানোর আগে কার্টন প্যাকিং সমন্বয় করুন।"
                                                else
                                                    "This variant already has $packedForItem PCS allocated to cartons. Please adjust the carton packing before reducing the PO quantity.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = StatusError,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextButton(onClick = { itemState.quantityStr = packedForItem.toString() }) {
                                                Text(if (isBangla) "ঠিক করুন ($packedForItem)" else "Restore $packedForItem", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                if(expanded) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = ERPSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isBangla) "আইটেম #${index + 1}" else "Item #${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryPurple
                                        )
                                        if (selectedCat != null) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "• ${if (isBangla && selectedCat.nameBn.isNotBlank()) selectedCat.nameBn else selectedCat.nameEn}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SecondaryTeal,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (productVariants.size > 1) {
                                            TextButton(
                                                onClick = {
                                                    val existingKeys = itemsList.map { it.productId to it.variantId }.toSet()
                                                    productVariants.forEachIndexed { vIdx, pv ->
                                                        if (vIdx == 0 && itemState.variantId == null) {
                                                            itemState.variantId = pv.id
                                                        } else if ((itemState.productId to pv.id) !in existingKeys) {
                                                            itemsList.add(
                                                                ItemDraftState(
                                                                    productId = itemState.productId,
                                                                    variantId = pv.id,
                                                                    quantityStr = itemState.quantityStr.ifBlank { "100" },
                                                                    unitPriceStr = if (pv.variantCostMinor > 0) Formatters.formatMinorUnit(pv.variantCostMinor) else itemState.unitPriceStr
                                                                )
                                                            )
                                                        }
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(if (isBangla) "+ সব ভ্যারিয়েন্ট" else "+ All Variants", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (onAddNewVariant != null && itemState.productId.isNotBlank()) {
                                            TextButton(
                                                onClick = {
                                                    targetItemIndexForVariant = index
                                                    showInlineAddVariantForProductId = itemState.productId
                                                    newVarColor = ""
                                                    newVarSize = ""
                                                    newVarSku = ""
                                                    newVarCostStr = itemState.unitPriceStr
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(if (isBangla) "+ নতুন ভ্যারিয়েন্ট" else "+ New Variant", fontSize = 11.sp, color = SecondaryTeal, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (packedForItem > 0) {
                                            errorMessage = if (isBangla)
                                                "এই ভ্যারিয়েন্টে $packedForItem PCS প্যাক করা আছে। পণ্য বা ভ্যারিয়েন্ট পাল্টানোর আগে কার্টন প্যাকিং পরিবর্তন করুন।"
                                            else
                                                "Cannot change product/variant while $packedForItem PCS are packed in cartons."
                                        } else {
                                            catalogPickerReplaceItemIdx = index
                                            showCatalogPickerSheet = true
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isBangla) "সার্চ করে পণ্য / ভ্যারিয়েন্ট পরিবর্তন করুন" else "Search & Change Product / Variant")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = itemState.quantityStr,
                                        onValueChange = { itemState.quantityStr = it },
                                        label = { Text(if (isBangla) "পরিমাণ (Qty)" else "Quantity") },
                                        isError = optionAError,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = itemState.unitPriceStr,
                                        onValueChange = { itemState.unitPriceStr = it },
                                        label = { Text(if (isBangla) "একক দর ($selectedCurrency)" else "Unit Price ($selectedCurrency)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                if (cartonBoxes.isEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = itemState.cartonQuantityStr,
                                            onValueChange = { itemState.cartonQuantityStr = it },
                                            label = { Text(if (isBangla) "কার্টন সংখ্যা" else "Cartons") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = itemState.grossWeightKgStr,
                                            onValueChange = { itemState.grossWeightKgStr = it },
                                            label = { Text(if (isBangla) "মোট ওজন (KG)" else "Gross Wt (KG)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }
                                } // expanded item editor
                            }
                        }
                    }

                    // ==========================================
                    item {
                        Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant) {
                            Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                                Text(if(isBangla) "মোট আইটেম: ${itemsList.size}" else "Items: ${itemsList.size}",style=MaterialTheme.typography.labelLarge)
                                Text((if(isBangla) "মোট পরিমাণ: " else "Quantity: ")+itemsList.sumOf { it.quantityStr.toIntOrNull() ?: 0 },style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
                            }
                        }
                    }
                    }
                    if (currentStep == 2) {
                    // 4. Smart Carton Packing (Strict Per-Variant Validation, Auto CTN-001, Shipping Marks)
                    // ==========================================
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = if (isBangla) "স্মার্ট কার্টন প্যাকিং ও শিপিং মার্ক" else "Smart Packing & Shipping Marks",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            PoShipmentMetrics(listOf(
                                (if (isBangla) "মোট কার্টন" else "Cartons") to "${cartonBoxes.size} CTN",
                                (if (isBangla) "মোট পিস" else "Packed") to "${livePackingSummary.totalPacked} / ${livePackingSummary.totalOrdered}",
                                (if (isBangla) "মোট ওজন" else "Weight") to "${cartonBoxes.fold(BigDecimal.ZERO) { sum, box -> sum.add(box.grossWeightKgStr.toBigDecimalOrNull() ?: BigDecimal.ZERO) }.stripTrailingZeros().toPlainString()} KG",
                                (if (isBangla) "মোট CBM" else "CBM") to cartonBoxes.fold(BigDecimal.ZERO) { sum, box -> sum.add(box.cbm) }.setScale(3, RoundingMode.HALF_UP).toPlainString()
                            ))

                            // Live Packing Progress Card (Phase 3.4)
                            var showVariantRemaining by rememberSaveable { mutableStateOf(false) }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (livePackingSummary.isComplete) SecondaryTeal else PrimaryPurple.copy(alpha = 0.25f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isBangla) "প্যাকিং অগ্রগতি (Packing Progress)" else "Packing Progress",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (livePackingSummary.isComplete) StatusSuccessContainer else PrimaryPurple.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "${livePackingSummary.percent}%",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (livePackingSummary.isComplete) com.pocketdev.advance_erp.core.designsystem.StatusSuccess else PrimaryPurple
                                            )
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { (livePackingSummary.percent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                                        color = if (livePackingSummary.isComplete) com.pocketdev.advance_erp.core.designsystem.StatusSuccess else PrimaryPurple,
                                        trackColor = ERPSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isBangla) "অর্ডার: ${livePackingSummary.totalOrdered} PCS" else "PO Total: ${livePackingSummary.totalOrdered} PCS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = if (isBangla) "প্যাকড: ${livePackingSummary.totalPacked} PCS" else "Packed: ${livePackingSummary.totalPacked} PCS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SecondaryTeal,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isBangla) "বাকি: ${livePackingSummary.totalRemaining} PCS" else "Remaining: ${livePackingSummary.totalRemaining} PCS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (livePackingSummary.totalRemaining == 0) com.pocketdev.advance_erp.core.designsystem.StatusSuccess else PrimaryPurple,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (livePackingSummary.variants.isNotEmpty()) {
                                        TextButton(
                                            onClick = { showVariantRemaining = !showVariantRemaining },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = if (isBangla) "ভ্যারিয়েন্ট ভিত্তিক বাকি তালিকা (${livePackingSummary.variants.size})" else "Variant-wise remaining breakdown (${livePackingSummary.variants.size})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Icon(if (showVariantRemaining) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                                        }
                                        if (showVariantRemaining) {
                                            livePackingSummary.variants.forEach { st ->
                                                val p = products.find { it.id == st.key.productId }
                                                val v = variants.find { it.id == st.key.variantId }
                                                val vName = v?.let { listOfNotNull(it.color, it.size).joinToString("/").ifBlank { it.sku } } ?: "Base"
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "${p?.nameEn ?: st.key.productId} ($vName)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextPrimary,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = if (st.remaining == 0) "✓ ${st.packed}/${st.ordered}" else "${st.packed}/${st.ordered} (বাকি ${st.remaining})",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = when {
                                                            st.isOverpacked -> StatusError
                                                            st.isComplete -> com.pocketdev.advance_erp.core.designsystem.StatusSuccess
                                                            else -> PrimaryPurple
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // PO Default Shipping Mark Card (Phase 5.1)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ERPDivider)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = if (isBangla) "PO ডিফল্ট শিপিং মার্ক (Default Shipping Mark)" else "Default Shipping Mark (Applies to Non-Overridden Cartons)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    OutlinedTextField(
                                        value = defaultShippingMarkStr,
                                        onValueChange = { defaultShippingMarkStr = it },
                                        label = { Text(if (isBangla) "ডিফল্ট শিপিং মার্ক" else "Default Shipping Mark") },
                                        placeholder = { Text("TUKTAK BD\n${editingOrder?.poNumber ?: "PO-2026-XXX"}\nCTG PORT\nMADE IN CHINA") },
                                        supportingText = {
                                            Text(
                                                if (isBangla) "স্বয়ংক্রিয় কার্টন নম্বর বসাতে {CTN} ব্যবহার করুন (যেমন: ... / {CTN})"
                                                else "Tip: Include {CTN} to auto-insert the current continuous carton number (e.g. CTN-001)"
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("po_default_shipping_mark"),
                                        minLines = 2,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            // Faster Carton Entry Action Row (Phase 3.7)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    enabled = itemsList.isNotEmpty() && livePackingSummary.variants.any { it.remaining > 0 },
                                    onClick = {
                                        val unallocated = livePackingSummary.variants.filter { it.remaining > 0 }
                                        unallocated.forEach { st ->
                                            val newUid = java.util.UUID.randomUUID().toString()
                                            val nextNum = PoPackingEngine.formatCartonNumber(cartonBoxes.size + 1)
                                            val itemDraft = itemsList.find { it.productId == st.key.productId && it.variantId == st.key.variantId }
                                            val autoPackGw = itemDraft?.grossWeightKgStr?.trim()
                                                ?.takeIf { (it.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO }
                                                ?: "15.0"
                                            cartonBoxes.add(
                                                CartonBoxState(
                                                    uid = newUid,
                                                    cartonNumber = nextNum,
                                                    grossWeightKgStr = autoPackGw,
                                                    initialLines = listOf(
                                                        CartonLineState(
                                                            productId = st.key.productId,
                                                            variantId = st.key.variantId,
                                                            quantityStr = st.remaining.toString()
                                                        )
                                                    )
                                                )
                                            )
                                        }
                                        renumberAllCartons()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                ) {
                                    Text(
                                        text = if (isBangla) "অটো-প্যাক বাকি" else "Auto-Pack Left",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryTeal
                                    )
                                }

                                OutlinedButton(
                                    enabled = itemsList.isNotEmpty() && livePackingSummary.variants.any { it.remaining > 0 },
                                    onClick = { showBulkCreateCartonsDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(0.95f).heightIn(min = 48.dp).testTag("po_bulk_carton_btn")
                                ) {
                                    Text(
                                        text = if (isBangla) "বাল্ক তৈরি" else "Bulk Create",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (livePackingSummary.variants.none { it.remaining > 0 }) {
                                            errorMessage = if (isBangla)
                                                "সব পণ্য ইতোমধ্যেই ১০০% কার্টনে প্যাক হয়ে গেছে। নতুন কার্টন যোগ করতে হলে আগের কার্টনের পরিমাণ কমান অথবা অর্ডার পরিমাণ বাড়ান।"
                                            else
                                                "All variants are already 100% allocated. Reduce existing carton quantities or use All/Completed filter to inspect."
                                        } else {
                                            errorMessage = null
                                            cartonPickerTargetIndex = -1
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple, contentColor = MaterialTheme.colorScheme.onPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1.1f).heightIn(min = 48.dp).testTag("po_add_carton_btn")
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "কার্টন যোগ" else "Add Carton",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (cartonBoxes.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    cartonBoxes.forEachIndexed { cIdx, box ->
                                        val ctnNo = PoPackingEngine.formatCartonNumber(cIdx + 1)
                                        if (box.cartonNumber != ctnNo) box.cartonNumber = ctnNo
                                        val boxTotalPcs = box.lines.sumOf { it.quantityStr.trim().toIntOrNull() ?: 0 }
                                        val boxGw = box.grossWeightKgStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                                        val distinctLinesCount = box.lines.map { it.productId to it.variantId }.distinct().size
                                        val isMixedCarton = distinctLinesCount > 1
                                        val resolvedMark = PoPackingEngine.resolveShippingMark(
                                            defaultMark = defaultShippingMarkStr.ifBlank {
                                                PoNotesMetadataParser.buildShippingMark(null, editingOrder?.poNumber ?: "PO-DRAFT", isLocalPurchase)
                                            },
                                            individualMark = box.customShippingMark,
                                            cartonNumber = ctnNo
                                        )
                                        val isCustomMark = PoPackingEngine.hasIndividualMark(box.customShippingMark)

                                        var cartonExpanded by rememberSaveable(box.uid) { mutableStateOf(cIdx == 0) }
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            shape = RoundedCornerShape(18.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = 1.dp,
                                                color = if (isMixedCarton) SecondaryTeal.copy(alpha = 0.35f) else PrimaryPurple.copy(alpha = 0.2f)
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().clickable { cartonExpanded = !cartonExpanded },
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = if (isMixedCarton)
                                                                (if (isBangla) "$ctnNo • মিশ্র ($distinctLinesCount লাইন) • $boxTotalPcs PCS"
                                                                else "$ctnNo • Mixed ($distinctLinesCount lines) • $boxTotalPcs PCS")
                                                            else
                                                                (if (isBangla) "$ctnNo • একক পণ্য • $boxTotalPcs PCS"
                                                                else "$ctnNo • Single • $boxTotalPcs PCS"),
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isMixedCarton) SecondaryTeal else PrimaryPurple
                                                        )
                                                        Text(
                                                            text = "G.W: ${box.grossWeightKgStr.ifBlank { "0" }} KG • ${box.cbm.setScale(3, RoundingMode.HALF_UP)} CBM" +
                                                                if (isCustomMark) " • Custom Mark" else "",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (cIdx > 0) {
                                                            IconButton(
                                                                onClick = {
                                                                    val item = cartonBoxes.removeAt(cIdx)
                                                                    cartonBoxes.add(cIdx - 1, item)
                                                                    renumberAllCartons()
                                                                },
                                                                modifier = Modifier.size(36.dp)
                                                            ) {
                                                                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move Carton Up", modifier = Modifier.size(18.dp))
                                                            }
                                                        }
                                                        if (cIdx < cartonBoxes.lastIndex) {
                                                            IconButton(
                                                                onClick = {
                                                                    val item = cartonBoxes.removeAt(cIdx)
                                                                    cartonBoxes.add(cIdx + 1, item)
                                                                    renumberAllCartons()
                                                                },
                                                                modifier = Modifier.size(36.dp)
                                                            ) {
                                                                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move Carton Down", modifier = Modifier.size(18.dp))
                                                            }
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                val perCartonMap = box.lines
                                                                    .mapNotNull { l ->
                                                                        val q = l.quantityStr.trim().toIntOrNull() ?: 0
                                                                        if (l.productId.isNotBlank() && q > 0) VariantKey(l.productId, l.variantId) to q else null
                                                                    }
                                                                    .groupBy({ it.first }, { it.second })
                                                                    .mapValues { (_, qs) -> qs.sum() }
                                                                val canDup = PoPackingEngine.maxIdenticalCartons(liveOrderLines, liveCartonLines, perCartonMap)
                                                                if (canDup < 1 || perCartonMap.isEmpty()) {
                                                                    errorMessage = if (isBangla)
                                                                        "$ctnNo ডুপ্লিকেট করা যাচ্ছে না: পর্যাপ্ত অবশিষ্ট পরিমাণ নেই।"
                                                                    else
                                                                        "Cannot duplicate $ctnNo: insufficient remaining PO quantity for its variants."
                                                                } else {
                                                                    errorMessage = null
                                                                    val cloned = CartonBoxState(
                                                                        cartonNumber = PoPackingEngine.formatCartonNumber(cartonBoxes.size + 1),
                                                                        grossWeightKgStr = box.grossWeightKgStr,
                                                                        netWeightKgStr = box.netWeightKgStr,
                                                                        lengthCm = box.lengthCm, widthCm = box.widthCm, heightCm = box.heightCm,
                                                                        cbm = box.cbm,
                                                                        customShippingMark = box.customShippingMark,
                                                                        initialLines = box.lines.map { l ->
                                                                            CartonLineState(l.productId, l.variantId, l.quantityStr, l.manualWeightKgStr)
                                                                        }
                                                                    )
                                                                    cartonBoxes.add(cIdx + 1, cloned)
                                                                    renumberAllCartons()
                                                                }
                                                            },
                                                            modifier = Modifier.size(40.dp)
                                                        ) {
                                                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate Carton", tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                cartonBoxes.removeAt(cIdx)
                                                                renumberAllCartons()
                                                            },
                                                            modifier = Modifier.size(40.dp).testTag("delete_carton_$cIdx")
                                                        ) {
                                                            Icon(Icons.Outlined.Delete, contentDescription = "Remove Carton", tint = StatusError, modifier = Modifier.size(17.dp))
                                                        }
                                                    }
                                                }

                                                if (cartonExpanded) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    HorizontalDivider(color = ERPSurfaceVariant)
                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        OutlinedTextField(
                                                            value = box.grossWeightKgStr,
                                                            onValueChange = { box.grossWeightKgStr = it },
                                                            label = { Text(if (isBangla) "গ্রস ওজন (KG)" else "Gross (kg)") },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            modifier = Modifier.weight(1f),
                                                            singleLine = true
                                                        )
                                                        OutlinedTextField(
                                                            value = box.netWeightKgStr,
                                                            onValueChange = { box.netWeightKgStr = it },
                                                            label = { Text(if (isBangla) "নেট ওজন (KG)" else "Net (kg)") },
                                                            placeholder = { Text(box.grossWeightKgStr) },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            modifier = Modifier.weight(1f),
                                                            singleLine = true
                                                        )
                                                    }
                                                    Spacer(Modifier.height(8.dp))
                                                    OutlinedTextField(
                                                        value = box.measurementStr,
                                                        onValueChange = box::updateMeasurement,
                                                        label = { Text(if (isBangla) "CTN মাপ (cm, ঐচ্ছিক)" else "CTN dimensions (cm, optional)") },
                                                        placeholder = { Text("40*50*40") },
                                                        supportingText = {
                                                            Text(
                                                                when {
                                                                    box.measurementInvalid -> if (isBangla) "দৈর্ঘ্য*প্রস্থ*উচ্চতা দিন, যেমন 40*50*40; অথবা খালি রাখুন।" else "Enter length*width*height, e.g. 40*50*40, or leave blank."
                                                                    box.cbm.signum() > 0 -> "CBM: ${box.cbm.stripTrailingZeros().let { it.setScale(maxOf(3, it.scale())) }.toPlainString()} m³ / CTN"
                                                                    else -> if (isBangla) "দৈর্ঘ্য × প্রস্থ × উচ্চতা (সেমি) দিলে CBM নিজে হিসাব হবে।" else "Length × width × height in cm calculates CBM automatically."
                                                                }
                                                            )
                                                        },
                                                        isError = box.measurementInvalid,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true
                                                    )

                                                    // Individual Carton Shipping Mark Override (Phase 5.2)
                                                    Spacer(Modifier.height(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = ERPSurfaceVariant.copy(alpha = 0.6f)
                                                    ) {
                                                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = if (isCustomMark) {
                                                                        if (isBangla) "কার্টন শিপিং মার্ক (কাস্টম)" else "Carton Shipping Mark (Custom Override)"
                                                                    } else {
                                                                        if (isBangla) "কার্টন শিপিং মার্ক (ডিফল্ট থেকে)" else "Carton Shipping Mark (From Default)"
                                                                    },
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (isCustomMark) SecondaryTeal else TextSecondary
                                                                )
                                                                if (isCustomMark) {
                                                                    TextButton(
                                                                        onClick = { box.customShippingMark = "" },
                                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                                                    ) {
                                                                        Text(if (isBangla) "ডিফল্টে ফেরান" else "Reset Default", fontSize = 11.sp)
                                                                    }
                                                                }
                                                            }
                                                            OutlinedTextField(
                                                                value = box.customShippingMark,
                                                                onValueChange = { box.customShippingMark = it },
                                                                placeholder = { Text(resolvedMark) },
                                                                label = { Text(if (isBangla) "কাস্টম মার্ক (খালি রাখলে ডিফল্ট বসবে)" else "Custom override (blank uses default)") },
                                                                singleLine = false,
                                                                minLines = 1,
                                                                modifier = Modifier.fillMaxWidth().testTag("carton_mark_$cIdx")
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = if (isBangla) "এই কার্টনের পণ্য ও ভ্যারিয়েন্ট বরাদ্দ:" else "Products / Variants Inside $ctnNo:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextSecondary
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))

                                                    val allocationPreview = runCatching {
                                                        CartonWeightAllocationEngine.allocateWeights(boxGw, box.netWeightKg(), lines = box.lines.map { line ->
                                                            MixedCartonLineInput(line.productId, line.variantId, quantity = line.quantityStr.toInt(),
                                                                manualGrossWeightKg = line.manualWeightKgStr.trim().takeIf { it.isNotEmpty() }?.toBigDecimal())
                                                        })
                                                    }
                                                    val totalManualWeight = box.lines.fold(BigDecimal.ZERO) { sum, line ->
                                                        sum + (line.manualWeightKgStr.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                                                    }
                                                    if (totalManualWeight > boxGw && boxGw > BigDecimal.ZERO) {
                                                        Text(
                                                            text = if (isBangla) "⚠️ সতর্কতা: ম্যানুয়াল মোট ওজন (${totalManualWeight} KG) কার্টনের মোট ওজনের (${boxGw} KG) চেয়ে বেশি!"
                                                            else "⚠️ Warning: Manual total weight (${totalManualWeight} KG) exceeds carton gross weight (${boxGw} KG)!",
                                                            color = StatusError,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                    }

                                                    box.lines.forEachIndexed { lIdx, lineState ->
                                                        val lineKey = VariantKey(lineState.productId, lineState.variantId)
                                                        val maxAllowedForLine = PoPackingEngine.maxAllowedForCarton(
                                                            orders = liveOrderLines,
                                                            cartons = liveCartonLines,
                                                            cartonUid = box.uid,
                                                            key = lineKey
                                                        )
                                                        val lineQty = lineState.quantityStr.trim().toIntOrNull() ?: 0
                                                        val lineOverpacked = lineQty > maxAllowedForLine
                                                        val lineInvalid = lineQty <= 0 || lineOverpacked
                                                        val lineProd = products.find { it.id == lineState.productId }
                                                        val lineVar = variants.find { it.id == lineState.variantId }
                                                        val manualWt = lineState.manualWeightKgStr.trim().toBigDecimalOrNull()
                                                        val allocatedKg = allocationPreview.getOrNull()?.getOrNull(lIdx)?.allocatedGrossWeightKg ?: BigDecimal.ZERO
                                                        val perPcKg = if (lineQty > 0) allocatedKg.divide(BigDecimal(lineQty), 3, RoundingMode.HALF_UP) else BigDecimal.ZERO

                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 4.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(ERPSurfaceVariant)
                                                                .border(
                                                                    width = if (lineOverpacked) 1.5.dp else 0.6.dp,
                                                                    color = if (lineOverpacked) StatusError else ERPDivider,
                                                                    shape = RoundedCornerShape(10.dp)
                                                                )
                                                                .padding(8.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        text = lineProd?.let { if (isBangla && it.nameBn.isNotBlank()) it.nameBn else it.nameEn } ?: lineState.productId,
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = TextPrimary
                                                                    )
                                                                    Text(
                                                                        text = "${variantLabel(lineVar)} • ${if (isBangla) "সর্বোচ্চ অনুমোদিত: $maxAllowedForLine PCS" else "Max allowed here: $maxAllowedForLine PCS"}",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = if (lineOverpacked) StatusError else PrimaryPurple
                                                                    )
                                                                }
                                                                TextButton(
                                                                    onClick = { cartonPickerReplaceLinePair = cIdx to lIdx },
                                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text(if (isBangla) "পরিবর্তন" else "Change", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                                if (box.lines.size > 1) {
                                                                    IconButton(
                                                                        onClick = { box.lines.removeAt(lIdx) },
                                                                        modifier = Modifier.size(28.dp)
                                                                    ) {
                                                                        Icon(Icons.Outlined.Close, contentDescription = "Remove Line", tint = StatusError, modifier = Modifier.size(16.dp))
                                                                    }
                                                                }
                                                            }

                                                            Spacer(modifier = Modifier.height(6.dp))

                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                OutlinedTextField(
                                                                    value = lineState.quantityStr,
                                                                    onValueChange = { lineState.quantityStr = it },
                                                                    label = { Text(if (isBangla) "পিস (সর্বোচ্চ $maxAllowedForLine)" else "PCS (max $maxAllowedForLine)") },
                                                                    isError = lineInvalid,
                                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                                    modifier = Modifier.weight(1f),
                                                                    singleLine = true
                                                                )
                                                                OutlinedTextField(
                                                                    value = lineState.manualWeightKgStr,
                                                                    onValueChange = { lineState.manualWeightKgStr = it },
                                                                    label = { Text(if (isBangla) "ম্যানুয়াল ওজন (KG)" else "Manual Wt (KG)") },
                                                                    placeholder = { Text(if (isBangla) "অটো" else "Auto") },
                                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                                    modifier = Modifier.weight(1.1f),
                                                                    singleLine = true
                                                                )
                                                            }
                                                            if (lineOverpacked) {
                                                                Text(
                                                                    text = if (isBangla)
                                                                        "এই ভ্যারিয়েন্টের প্যাক পরিমাণ PO অর্ডার ($maxAllowedForLine PCS বাকি সীমা) ছাড়িয়ে গেছে।"
                                                                    else
                                                                        "Packed quantity exceeds PO remaining limit (max $maxAllowedForLine PCS).",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = StatusError,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = if (isBangla) "বরাদ্দকৃত ওজন: $allocatedKg KG ($perPcKg KG/pc)" else "Line Wt: $allocatedKg KG ($perPcKg KG/pc)",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = SecondaryTeal
                                                                )
                                                                Text(
                                                                    text = if (manualWt != null && manualWt > BigDecimal.ZERO) {
                                                                        if (isBangla) "ম্যানুয়াল ওজন" else "Manual override"
                                                                    } else {
                                                                        if (isBangla) "স্বয়ংক্রিয় বণ্টন" else "Auto allocated"
                                                                    },
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = if (manualWt != null && manualWt > BigDecimal.ZERO) PrimaryPurple else TextSecondary,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Button(
                                                        enabled = livePackingSummary.variants.any { it.remaining > 0 },
                                                        onClick = { cartonPickerTargetIndex = cIdx },
                                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal.copy(alpha = 0.15f)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Outlined.Add, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = if (isBangla)
                                                                "+ কার্টনে আরও ভ্যারিয়েন্ট যোগ/মিক্স করুন"
                                                            else
                                                                "+ Add / Mix Available Variants in $ctnNo",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SecondaryTeal
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Surface(shape = RoundedCornerShape(12.dp), color = ERPSurfaceVariant) {
                                    Text(
                                        text = if (isBangla)
                                            "এখনও কোনো কার্টন তৈরি হয়নি। উপরের 'কার্টন যোগ', 'বাল্ক তৈরি' বা 'অটো-প্যাক বাকি' চাপুন।"
                                        else
                                            "No cartons packed yet. Use 'Add Carton', 'Bulk Create', or 'Auto-Pack Left' above.",
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                    }
                    if (currentStep == 3) {
                    // 5. Base Summary: Total Weight, Category Per-KG Rate & Total Freight Calculation
                    // ==========================================
                    if (!metadataOnly) item {
                        // Compute category weights & totals live
                        val categoryWeightMap = mutableMapOf<String, BigDecimal>()
                        if (cartonBoxes.isNotEmpty()) {
                            cartonBoxes.forEach { box ->
                                val shares = runCatching {
                                    CartonWeightAllocationEngine.allocateWeights(
                                        box.grossWeightKgStr.toBigDecimal(), lines = box.lines.map { line ->
                                            MixedCartonLineInput(line.productId, line.variantId, quantity = line.quantityStr.toInt(),
                                                manualGrossWeightKg = line.manualWeightKgStr.trim().takeIf { it.isNotEmpty() }?.toBigDecimal())
                                        }
                                    )
                                }.getOrDefault(emptyList())
                                shares.forEach { line ->
                                    val catId = products.find { it.id == line.productId }?.categoryId ?: "uncategorized"
                                    categoryWeightMap[catId] = (categoryWeightMap[catId] ?: BigDecimal.ZERO) + line.allocatedGrossWeightKg
                                }
                            }
                        } else {
                            itemsList.forEach { itm ->
                                if (itm.productId.isNotBlank()) {
                                    val prod = products.find { it.id == itm.productId }
                                    val catId = prod?.categoryId ?: "uncategorized"
                                    val gw = itm.grossWeightKgStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                                    categoryWeightMap[catId] = (categoryWeightMap[catId] ?: BigDecimal.ZERO).add(gw)
                                }
                            }
                        }

                        val totalCartonsCount = if (cartonBoxes.isNotEmpty()) {
                            cartonBoxes.size
                        } else {
                            itemsList.sumOf { it.cartonQuantityStr.trim().toIntOrNull() ?: 0 }
                        }
                        val totalOrderedPcs = itemsList.sumOf { it.quantityStr.trim().toIntOrNull() ?: 0 }
                        val totalGrossWeightKg = categoryWeightMap.values.fold(BigDecimal.ZERO) { acc, w -> acc.add(w) }
                            .setScale(2, RoundingMode.HALF_UP)

                        val rateBd = exchangeRateStr.trim().toBigDecimalOrNull() ?: BigDecimal.ONE
                        val totalProductForeign = itemsList.fold(BigDecimal.ZERO) { acc, itm ->
                            val q = BigDecimal(itm.quantityStr.trim().toIntOrNull() ?: 0)
                            val p = itm.unitPriceStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                            acc.add(q.multiply(p))
                        }.setScale(2, RoundingMode.HALF_UP)
                        val totalProductBdt = totalProductForeign.multiply(rateBd).setScale(2, RoundingMode.HALF_UP)

                        val chinaDelivForeign = (chinaLocalDeliveryStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
                        val chinaDelivBdt = chinaDelivForeign.multiply(rateBd).setScale(2, RoundingMode.HALF_UP)

                        var totalCategoryFreightBdt = BigDecimal.ZERO
                        categoryWeightMap.forEach { (catId, weightKg) ->
                            val ratePerKg = categoryRateStrings[catId]?.trim()?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            totalCategoryFreightBdt = totalCategoryFreightBdt.add(weightKg.multiply(ratePerKg))
                        }
                        totalCategoryFreightBdt = totalCategoryFreightBdt.setScale(2, RoundingMode.HALF_UP)

                        val handlingCostBdt = (handlingCostStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
                        val sampleCostBdt = (sampleCostStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)

                        val grandTotalEstimatedBdt = totalProductBdt
                            .add(chinaDelivBdt)
                            .add(totalCategoryFreightBdt)
                            .add(handlingCostBdt)
                            .add(sampleCostBdt)
                            .setScale(2, RoundingMode.HALF_UP)

                        val avgUnitLandedBdt = if (totalOrderedPcs > 0) {
                            grandTotalEstimatedBdt.divide(BigDecimal(totalOrderedPcs), 2, RoundingMode.HALF_UP)
                        } else BigDecimal.ZERO

                        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Text(if(isBangla) "ল্যান্ডিং কস্ট / আমদানি ব্যয়" else "Landed cost / import expenses",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isBangla)
                                        "ওজন ভিত্তিক কস্টিং (Weight-Based Costing)"
                                    else
                                        "Weight-based costing",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Top Summary Pills (Total Cartons, Total PCS, Total Weight KG)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(if (isBangla) "মোট কার্টন" else "Total Cartons", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        Text("$totalCartonsCount CTN", fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(if (isBangla) "মোট পিস (PCS)" else "Total Quantity", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        Text("$totalOrderedPcs PCS", fontWeight = FontWeight.Bold, color = PrimaryPurple)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(if (isBangla) "সর্বমোট ওজন (KG)" else "Total Gross Weight", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        Text("$totalGrossWeightKg KG", fontWeight = FontWeight.Bold, color = SecondaryTeal)
                                    }
                                }

                                if (!isLocalPurchase) {
                                    HorizontalDivider(color = ERPDivider, modifier = Modifier.padding(vertical = 10.dp))

                                    // Category Per-KG Rate Inputs
                                    Text(
                                        text = if (isBangla)
                                            "ক্যাটাগরি অনুযায়ী আন্তর্জাতিক ফ্রেইট রেট (৳/KG):"
                                        else
                                            "Category International Freight Rate (BDT/KG):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    categoryWeightMap.entries.forEach { (catId, catWeightKg) ->
                                        val catObj = categories.find { it.id == catId }
                                        val catTitle = if (catObj != null) {
                                            if (isBangla && catObj.nameBn.isNotBlank()) catObj.nameBn else catObj.nameEn
                                        } else {
                                            if (isBangla) "অন্যান্য ক্যাটাগরি" else "Uncategorized"
                                        }
                                        val rateStr = categoryRateStrings[catId] ?: "0"
                                        val rateBdPerKg = rateStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                                        val catFreightBdt = catWeightKg.multiply(rateBdPerKg).setScale(2, RoundingMode.HALF_UP)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1.1f)) {
                                                Text(catTitle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Text(
                                                    text = if (isBangla) "ওজন: ${catWeightKg.setScale(2, RoundingMode.HALF_UP)} KG" else "Weight: ${catWeightKg.setScale(2, RoundingMode.HALF_UP)} KG",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                            OutlinedTextField(
                                                value = rateStr,
                                                onValueChange = { categoryRateStrings[catId] = it },
                                                label = { Text(if (isBangla) "৳ / কেজি" else "BDT / KG") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            Column(
                                                modifier = Modifier.weight(0.9f),
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Text(if (isBangla) "ফ্রেইট চার্জ" else "Category Freight", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                                Text("৳ $catFreightBdt", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = ERPDivider, modifier = Modifier.padding(vertical = 10.dp))

                                // Handling & Sample Costs Inputs (Capitalized into Landing Cost)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = handlingCostStr,
                                        onValueChange = { handlingCostStr = it },
                                        label = { Text(if (isBangla) "হ্যান্ডলিং খরচ (৳)" else "Handling Cost (BDT)") },

                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = sampleCostStr,
                                        onValueChange = { sampleCostStr = it },
                                        label = { Text(if (isBangla) "স্যাম্পল খরচ (৳)" else "Sample Cost (BDT)") },

                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                            }
                        }
                        Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                // Base Financial Breakdown
                                val prodTotalDisplay = if (isLocalPurchase) "৳ $totalProductBdt" else "$selectedCurrency $totalProductForeign (৳ $totalProductBdt)"
                                PremiumPoCostLine(if (isBangla) "পণ্যের মোট ক্রয়মূল্য:" else "Products Purchase Total:", prodTotalDisplay)
                                if (!isLocalPurchase) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    PremiumPoCostLine(if (isBangla) "চীন লোকাল ডেলিভারি চার্জ:" else "China Local Delivery:", "$selectedCurrency $chinaDelivForeign (৳ $chinaDelivBdt)")
                                    Spacer(modifier = Modifier.height(4.dp))
                                    PremiumPoCostLine(if (isBangla) "আন্তর্জাতিক ফ্রেইট ($totalGrossWeightKg KG):" else "International Freight ($totalGrossWeightKg KG):", "৳ $totalCategoryFreightBdt", SecondaryTeal)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                PremiumPoCostLine(if (isBangla) "হ্যান্ডলিং ও আনলোডিং:" else "Handling & unloading:", "৳ $handlingCostBdt")
                                if (sampleCostBdt > BigDecimal.ZERO) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    PremiumPoCostLine(if (isBangla) "স্যাম্পল প্রসেসিং খরচ (ক্যাপিটালাইজড):" else "Sample Processing (Capitalized):", "৳ $sampleCostBdt")
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = ERPDivider)
                                Spacer(modifier = Modifier.height(6.dp))
                                PoLandedHighlight("৳ $grandTotalEstimatedBdt",(if(isBangla) "গড় প্রতি পিস: " else "Average per unit: ")+"৳ $avgUnitLandedBdt / PCS",isBangla)

                                Spacer(modifier = Modifier.height(8.dp))
                                // Warehouse Rent OPEX Note
                                Surface(
                                    color = StatusWarningContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarningContainer),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isBangla)
                                            "ℹ️ গুদাম ভাড়া (Warehouse Rent) একটি মাসিক পরিচালন ব্যয় (OPEX) হিসেবে বিবেচিত হয় এবং পণ্যের ইউনিট খরচে যুক্ত না হয়ে নেট লাভে সমন্বয় হবে।"
                                        else
                                            "ℹ️ Warehouse Rent is treated as an Operational Expense (OPEX) and deducted from Gross Profit, NOT capitalized into unit cost.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusWarning,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (metadataOnly) item {
                        Text(if (isBangla) "অর্থসংক্রান্ত তথ্য লক আছে। নোট, ঠিকানা ও সংযুক্তি সম্পাদনা করতে পারবেন।"
                            else "Financial details are locked. You can update notes, delivery address and attachments.",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    // Address is edited with the purchase information; finalized-order metadata edits retain it here.
                    if (metadataOnly) item {
                        OutlinedTextField(
                            value = forwarderAddressStr,
                            onValueChange = { forwarderAddressStr = it },
                            label = {
                                Text(
                                    if (isLocalPurchase) {
                                        if (isBangla) "গুদাম / ডেলিভারি ঠিকানা" else "Warehouse / delivery address"
                                    } else {
                                        if (isBangla) "ফরোয়ার্ডার গুদাম ঠিকানা ও রিসিভার ফোন" else "Forwarder warehouse and receiver contact"
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3
                        )
                    }

                    item {
                        PoAttachmentPickerSection(
                            attachments = attachmentsList,
                            onAttachmentsChanged = { attachmentsList = it },
                            isBangla = isBangla
                        )
                    }

                    // Notes are edited in Step 1 and reviewed here; paid/received metadata edits remain editable.
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ERPDivider)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Text(if (isBangla) "অর্ডার নোট / রেফারেন্স" else "Order notes / reference", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                                if (metadataOnly) {
                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        maxLines = 4,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                } else {
                                    Text(
                                        notes.ifBlank { if (isBangla) "কোনো নোট যোগ করা হয়নি" else "No notes added" },
                                        modifier = Modifier.padding(top = 5.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // 8. Payment Terms & Schedule Calculation
                    if (!metadataOnly) item {
                        Column {
                            Text(
                                text = if (isBangla) "পেমেন্ট শর্ত ও সময়সূচি (Payment Terms):" else "Payment Terms & Schedule:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(PaymentTermType.values()) { term ->
                                    val isSel = selectedPaymentTerm == term
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) PrimaryPurple else ERPSurfaceVariant)
                                            .clickable { selectedPaymentTerm = term }
                                            .heightIn(min = 48.dp).padding(horizontal = 10.dp, vertical = 12.dp)
                                    ) {
                                        Text(
                                            text = if (isBangla) term.labelBn else term.labelEn,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else TextPrimary
                                        )
                                    }
                                }
                            }

                            val totalMinor = itemsList.sumOf { item ->
                                val q = item.quantityStr.toIntOrNull() ?: 0
                                val p = (item.unitPriceStr.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                                    .multiply(BigDecimal("100"))
                                    .setScale(0, RoundingMode.HALF_UP)
                                    .longValueExact()
                                q * p
                            }
                            val rateBd = exchangeRateStr.trim().toBigDecimalOrNull() ?: BigDecimal.ONE
                            val schedule = remember(selectedPaymentTerm, totalMinor, rateBd) {
                                PaymentTermsCalculator.calculateSchedule(selectedPaymentTerm, totalMinor, rateBd)
                            }

                            if (schedule.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ERPSurfaceVariant),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ERPDivider)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = if (isBangla) "নির্ধারিত কিস্তি সময়সূচি:" else "Calculated Payment Schedule:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SecondaryTeal
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        schedule.forEach { inst ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = if (isBangla) inst.titleBn else inst.titleEn,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "$selectedCurrency ${BigDecimal(inst.amountMinor).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)} (৳ ${BigDecimal(inst.bdtAmountMinor).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)})",
                                                    style = MaterialTheme.typography.bodySmall,
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
                }

                HorizontalDivider(color = ERPDivider, modifier = Modifier.padding(vertical = 4.dp))

                if (!metadataOnly && currentStep == 3 && livePackingSummary.totalRemaining > 0) {
                    Surface(
                        color = StatusWarningContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = if (isBangla)
                                "⚠️ প্যাকিং অসম্পূর্ণ। এখনও ${livePackingSummary.totalRemaining} PCS বরাদ্দ বাকি আছে। খসড়া (Draft) সংরক্ষণ করা যাবে, তবে নিশ্চিত (Confirm) করতে সব পণ্য প্যাক করতে হবে।"
                            else
                                "⚠️ Packing incomplete. ${livePackingSummary.totalRemaining} PCS remain unallocated. Save Draft is allowed; allocate all items to Confirm.",
                            color = StatusWarning,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                (errorMessage ?: saveError)?.let { message ->
                    Text(message, color = StatusError, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!metadataOnly) OutlinedButton(
                        onClick = { if(currentStep>0) currentStep-- else requestClose() }, enabled = !isSaving,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) { Text(if (isBangla) "পেছনে" else "Back") }
                    if (currentStep < 3) {
                        Button(onClick = { currentStep++ }, enabled = !isSaving,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                            Text(if (isBangla) "পরবর্তী" else "Continue")
                        }
                    } else {
                        if (!metadataOnly && (editingOrder == null || editingOrder.status == PurchaseOrderStatus.DRAFT.name)) {
                            OutlinedButton(onClick = { submitPo(PurchaseOrderStatus.DRAFT) }, enabled = !isSaving,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(4.dp)) {
                                Text(if (isBangla) "ড্রাফট" else "Draft")
                            }
                        }
                        Button(onClick = { submitPo(PurchaseOrderStatus.CONFIRMED) }, enabled = !isSaving,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(4.dp)) {
                            Text(if (isSaving) { if (isBangla) "সংরক্ষণ…" else "Saving…" }
                                else if (isEditing) { if (isBangla) "আপডেট" else "Update" }
                                else if (isBangla) "নিশ্চিত করুন" else "Confirm")
                        }
                    }
                }
                } // form and fixed wizard actions
                if(navigateTab!=null) com.pocketdev.advance_erp.core.designsystem.ERPBottomNavigation("purchases", { tab ->
                    if(tab==com.pocketdev.advance_erp.core.designsystem.BottomNavDestination.PURCHASES) requestClose()
                    else {pendingTab=tab;requestClose()}
                },language=if(isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH)
            }
        }

        // In-Window Pickers (Step 1 Catalog Picker, Step 2 Carton Picker, Bulk Create Cartons)
        if (showCatalogPickerSheet) {
            val replaceIdx = catalogPickerReplaceItemIdx
            val existingQtyMap = itemsList
                .filter { it.productId.isNotBlank() }
                .associate { VariantKey(it.productId, it.variantId) to (it.quantityStr.trim().toIntOrNull() ?: 0) }
            PoCatalogProductVariantPickerSheet(
                products = products,
                variants = variants,
                categories = categories,
                currency = selectedCurrency,
                isBangla = isBangla,
                singleSelectMode = replaceIdx != null,
                existingOrderQtyByVariant = existingQtyMap,
                resolveProductImage = resolveProductImage,
                onAddNewVariant = if (onAddNewVariant != null) { prodId ->
                    showCatalogPickerSheet = false
                    targetItemIndexForVariant = replaceIdx
                    showInlineAddVariantForProductId = prodId
                    newVarColor = ""
                    newVarSize = ""
                    newVarSku = ""
                    newVarCostStr = ""
                } else null,
                onDismiss = {
                    showCatalogPickerSheet = false
                    catalogPickerReplaceItemIdx = null
                },
                onConfirmSelection = { picks, defaultQtyStr ->
                    if (replaceIdx != null && replaceIdx in itemsList.indices) {
                        picks.firstOrNull()?.let { pick ->
                            val duplicateElsewhere = itemsList.withIndex().any { (idx, st) ->
                                idx != replaceIdx && st.productId == pick.productId && st.variantId == pick.variantId
                            }
                            if (!duplicateElsewhere) {
                                itemsList[replaceIdx].productId = pick.productId
                                itemsList[replaceIdx].variantId = pick.variantId
                                if (pick.suggestedUnitPriceMajor.isNotBlank()) {
                                    itemsList[replaceIdx].unitPriceStr = pick.suggestedUnitPriceMajor
                                }
                            } else {
                                errorMessage = if (isBangla) "এই ভ্যারিয়েন্টটি ইতোমধ্যে PO তালিকায় রয়েছে।" else "This variant is already present in another PO line."
                            }
                        }
                    } else {
                        val qtyInt = defaultQtyStr.trim().toIntOrNull()?.coerceAtLeast(1) ?: 100
                        picks.forEach { pick ->
                            val existing = itemsList.find { it.productId == pick.productId && it.variantId == pick.variantId }
                            if (existing != null) {
                                val updatedQty = (existing.quantityStr.trim().toIntOrNull() ?: 0) + qtyInt
                                existing.quantityStr = updatedQty.toString()
                            } else {
                                itemsList.add(
                                    ItemDraftState(
                                        productId = pick.productId,
                                        variantId = pick.variantId,
                                        quantityStr = qtyInt.toString(),
                                        unitPriceStr = pick.suggestedUnitPriceMajor
                                    )
                                )
                            }
                        }
                    }
                    showCatalogPickerSheet = false
                    catalogPickerReplaceItemIdx = null
                }
            )
        }

        if (cartonPickerTargetIndex != null || cartonPickerReplaceLinePair != null) {
            val targetIdx = cartonPickerTargetIndex
            val replacePair = cartonPickerReplaceLinePair
            val targetBox = when {
                replacePair != null -> cartonBoxes.getOrNull(replacePair.first)
                targetIdx != null && targetIdx >= 0 -> cartonBoxes.getOrNull(targetIdx)
                else -> null
            }
            val targetUid = targetBox?.uid ?: "new-carton-preview"
            val maxAllowedMap = livePackingSummary.variants.associate { st ->
                st.key to PoPackingEngine.maxAllowedForCarton(
                    orders = liveOrderLines,
                    cartons = liveCartonLines,
                    cartonUid = targetUid,
                    key = st.key
                )
            }
            val existingKeysInTarget = targetBox?.lines
                ?.map { VariantKey(it.productId, it.variantId) }
                ?.toSet()
                ?: emptySet()

            PoCartonVariantPickerSheet(
                title = when {
                    replacePair != null -> if (isBangla) "ভ্যারিয়েন্ট পরিবর্তন" else "Change Variant in Carton"
                    targetBox != null -> if (isBangla) "${targetBox.cartonNumber}-এ ভ্যারিয়েন্ট যোগ" else "Add Variants to ${targetBox.cartonNumber}"
                    else -> if (isBangla) "নতুন কার্টনের জন্য ভ্যারিয়েন্ট নির্বাচন" else "Select Variants for New Carton"
                },
                products = products,
                variants = variants,
                statuses = livePackingSummary.variants,
                maxAllowedByVariant = maxAllowedMap,
                alreadyInCartonKeys = existingKeysInTarget,
                isBangla = isBangla,
                singleSelectMode = replacePair != null,
                resolveProductImage = resolveProductImage,
                onDismiss = {
                    cartonPickerTargetIndex = null
                    cartonPickerReplaceLinePair = null
                },
                onConfirm = { picks ->
                    if (replacePair != null) {
                        val (bIdx, lIdx) = replacePair
                        val b = cartonBoxes.getOrNull(bIdx)
                        val pick = picks.firstOrNull()
                        if (b != null && lIdx in b.lines.indices && pick != null) {
                            val duplicateInBox = b.lines.withIndex().any { (idx, l) ->
                                idx != lIdx && l.productId == pick.productId && l.variantId == pick.variantId
                            }
                            if (!duplicateInBox) {
                                b.lines[lIdx].productId = pick.productId
                                b.lines[lIdx].variantId = pick.variantId
                                b.lines[lIdx].quantityStr = pick.quantity.toString()
                            } else {
                                errorMessage = if (isBangla) "একই কার্টনে ডুপ্লিকেট ভ্যারিয়েন্ট যোগ করা যাবে না।" else "Duplicate variant cannot exist in the same carton."
                            }
                        }
                    } else if (targetIdx != null && targetIdx >= 0 && targetIdx in cartonBoxes.indices) {
                        val b = cartonBoxes[targetIdx]
                        picks.forEach { pick ->
                            if (b.lines.none { it.productId == pick.productId && it.variantId == pick.variantId }) {
                                b.lines.add(CartonLineState(pick.productId, pick.variantId, pick.quantity.toString()))
                            }
                        }
                    } else {
                        if (picks.isNotEmpty()) {
                            cartonBoxes.add(
                                CartonBoxState(
                                    cartonNumber = PoPackingEngine.formatCartonNumber(cartonBoxes.size + 1),
                                    grossWeightKgStr = "15.0",
                                    initialLines = picks.map { p ->
                                        CartonLineState(p.productId, p.variantId, p.quantity.toString())
                                    }
                                )
                            )
                            renumberAllCartons()
                        }
                    }
                    cartonPickerTargetIndex = null
                    cartonPickerReplaceLinePair = null
                }
            )
        }

        if (showBulkCreateCartonsDialog) {
            BulkCreateCartonsDialog(
                products = products,
                variants = variants,
                statuses = livePackingSummary.variants,
                isBangla = isBangla,
                onDismiss = { showBulkCreateCartonsDialog = false },
                onConfirmBulk = { count, gw, nw, measurementStr, perCartonPicks ->
                    repeat(count) {
                        val newBox = CartonBoxState(
                            cartonNumber = PoPackingEngine.formatCartonNumber(cartonBoxes.size + 1),
                            grossWeightKgStr = gw.stripTrailingZeros().toPlainString(),
                            netWeightKgStr = nw.stripTrailingZeros().toPlainString(),
                            initialLines = perCartonPicks.map { pick ->
                                CartonLineState(pick.productId, pick.variantId, pick.quantity.toString())
                            }
                        ).also {
                            if (measurementStr.isNotBlank()) it.updateMeasurement(measurementStr)
                        }
                        cartonBoxes.add(newBox)
                    }
                    renumberAllCartons()
                    showBulkCreateCartonsDialog = false
                }
            )
        }
        } // Box root for in-window overlays
    }

    } // PO palette

    // ==========================================
    // Inline Add Supplier Dialog
    // ==========================================
    if (showInlineAddSupplierDialog && onAddNewSupplier != null) {
        AlertDialog(
            onDismissRequest = { showInlineAddSupplierDialog = false },
            title = {
                Text(
                    text = if (isBangla) "নতুন সরবরাহকারী (Supplier) যোগ করুন" else "Add New Supplier",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestTextField(
                        fieldType = SuggestionType.SUPPLIER_NAME,
                        value = newSupCompany,
                        onValueChange = { newSupCompany = it },
                        label = { Text(if (isBangla) "কোম্পানি / ফ্যাক্টরির নাম *" else "Company / Factory Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    SuggestTextField(
                        fieldType = SuggestionType.CONTACT_PERSON,
                        value = newSupContact,
                        onValueChange = { newSupContact = it },
                        label = { Text(if (isBangla) "যোগাযোগের ব্যক্তি (ঐচ্ছিক)" else "Contact Person (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newSupPhone,
                        onValueChange = { newSupPhone = it },
                        label = { Text(if (isBangla) "ফোন / WeChat নম্বর" else "Phone / WeChat") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestTextField(
                            fieldType = SuggestionType.COUNTRY,
                            value = newSupCountry,
                            onValueChange = { newSupCountry = it },
                            label = { Text(if (isBangla) "দেশ" else "Country") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newSupCurrency,
                            onValueChange = { newSupCurrency = it.uppercase() },
                            label = { Text("Currency (CNY/USD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSupCompany.isNotBlank()) {
                            onAddNewSupplier(
                                newSupCompany.trim(),
                                newSupContact.trim(),
                                newSupPhone.trim(),
                                newSupAddress.trim(),
                                newSupCountry.trim().ifBlank { "China" },
                                newSupCurrency.trim().ifBlank { "CNY" }
                            ) { createdSup ->
                                selectedSupplierId = createdSup.id
                                selectedCurrency = createdSup.currency.ifBlank { "CNY" }
                                exchangeRateStr = if (selectedCurrency == "USD") "120.00" else "17.00"
                                showInlineAddSupplierDialog = false
                                newSupCompany = ""
                                newSupContact = ""
                                newSupPhone = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text(if (isBangla) "সংরক্ষণ ও নির্বাচন করুন" else "Save & Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInlineAddSupplierDialog = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // ==========================================
    // Inline Add Variant Dialog
    // ==========================================
    if (showInlineAddVariantForProductId != null && onAddNewVariant != null) {
        val targetProdId = showInlineAddVariantForProductId!!
        val targetProd = products.find { it.id == targetProdId }
        AlertDialog(
            onDismissRequest = { showInlineAddVariantForProductId = null },
            title = {
                Text(
                    text = if (isBangla) "নতুন ভ্যারিয়েন্ট যোগ (${targetProd?.nameEn ?: ""})" else "Add Variant (${targetProd?.nameEn ?: ""})",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestTextField(
                            fieldType = SuggestionType.COLOR,
                            value = newVarColor,
                            onValueChange = { newVarColor = it },
                            label = { Text(if (isBangla) "রঙ (Color)" else "Color") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        SuggestTextField(
                            fieldType = SuggestionType.SIZE,
                            value = newVarSize,
                            onValueChange = { newVarSize = it },
                            label = { Text(if (isBangla) "সাইজ (Size)" else "Size") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = newVarSku,
                        onValueChange = { newVarSku = it.uppercase() },
                        label = { Text(if (isBangla) "ভ্যারিয়েন্ট SKU (খালি রাখলে অটো তৈরি হবে)" else "Variant SKU (Auto if blank)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newVarCostStr,
                        onValueChange = { newVarCostStr = it },
                        label = { Text(if (isBangla) "একক দর ($selectedCurrency)" else "Unit Price ($selectedCurrency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newVarColor.isNotBlank() || newVarSize.isNotBlank() || newVarSku.isNotBlank()) {
                            val costMinor = (newVarCostStr.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                                .multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                            val priceMinor = (newVarPriceStr.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                                .multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                            onAddNewVariant(
                                targetProdId,
                                newVarColor.trim(),
                                newVarSize.trim(),
                                newVarSku.trim(),
                                costMinor,
                                priceMinor
                            ) { createdVar ->
                                val itemIdx = targetItemIndexForVariant
                                if (itemIdx != null && itemIdx in itemsList.indices) {
                                    itemsList[itemIdx].variantId = createdVar.id
                                    if (newVarCostStr.isNotBlank()) {
                                        itemsList[itemIdx].unitPriceStr = newVarCostStr
                                    }
                                }
                                showInlineAddVariantForProductId = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text(if (isBangla) "ভ্যারিয়েন্ট যোগ করুন" else "Add Variant")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInlineAddVariantForProductId = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // ==========================================
    // Full Add Product (same fields as Product module; stock stays 0 until receiving)
    // ==========================================
    if (showInlineAddProductDialog && onCreateFullProduct != null) {
        AddProductDialog(
            categories = categories,
            warehouses = emptyList(),
            language = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH,
            hideStock = true,
            onDismiss = { showInlineAddProductDialog = false },
            onAddNewCategory = onAddNewCategory,
            onGenerateSku = onGenerateSku,
            onGenerateVariantSku = onGenerateVariantSku,
            onGenerateBarcode = onGenerateBarcode,
            onValidateSku = onValidateSku,
            onValidateBarcode = onValidateBarcode,
            onConfirm = { nameEn, nameBn, baseSku, barcode, catId, unit, purchaseMinor, sellMinor, minStock, photos, vars, _, _, desc ->
                onCreateFullProduct(
                    nameEn, nameBn, baseSku, barcode, catId, unit, purchaseMinor, sellMinor, minStock, photos, vars, desc
                ) { created, createdVars ->
                    showInlineAddProductDialog = false
                    if (createdVars.isEmpty()) {
                        itemsList.add(ItemDraftState(productId = created.id, variantId = null, quantityStr = "100", unitPriceStr = ""))
                    } else {
                        createdVars.forEach { cv ->
                            itemsList.add(ItemDraftState(productId = created.id, variantId = cv.id, quantityStr = "100", unitPriceStr = ""))
                        }
                    }
                }
            }
        )
    }
}
