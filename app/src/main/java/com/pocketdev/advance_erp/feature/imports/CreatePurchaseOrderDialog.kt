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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import com.pocketdev.advance_erp.core.engine.CartonWeightAllocationEngine
import com.pocketdev.advance_erp.core.engine.MixedCartonLineInput
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
    cartonNumber: String = "CTN-01",
    grossWeightKgStr: String = "15.0",
    initialLines: List<CartonLineState> = emptyList(),
    netWeightKgStr: String = "",
    lengthCm: BigDecimal = BigDecimal.ZERO,
    widthCm: BigDecimal = BigDecimal.ZERO,
    heightCm: BigDecimal = BigDecimal.ZERO,
    cbm: BigDecimal = BigDecimal.ZERO
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
    put("number", cartonNumber); put("gross", grossWeightKgStr); put("net", netWeightKgStr)
    put("measurement", measurementStr)
    put("lines", JSONArray().apply { lines.forEach { line -> put(JSONObject().apply {
        put("product", line.productId); put("variant", line.variantId ?: ""); put("qty", line.quantityStr); put("weight", line.manualWeightKgStr)
    }) } })
}
private fun cartonFromJson(j: JSONObject): CartonBoxState {
    val a = j.getJSONArray("lines")
    return CartonBoxState(cartonNumber = j.optString("number"), grossWeightKgStr = j.optString("gross"),
        netWeightKgStr = j.optString("net"), initialLines = (0 until a.length()).map { i -> a.getJSONObject(i).let {
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
        productPhotos: List<GalleryItemDraft>, variants: List<NewVariantDraft>,
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
    var notes by rememberSaveable(editingOrder?.id) { mutableStateOf(parsedNotes.userNotes) }
    var orderDateMillis by rememberSaveable(editingOrder?.id) { mutableStateOf(editingOrder?.orderDate ?: System.currentTimeMillis()) }
    var expectedArrivalDateMillis by rememberSaveable(editingOrder?.id) { mutableStateOf(editingOrder?.expectedArrivalDate) }
    var selectedShippingMethod by rememberSaveable(editingOrder?.id) { mutableStateOf(initialShippingMethod) }
    var supplierSearch by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
        groupBy { it.cartonNumber }.map { (cNum, cLines) ->
            val firstCtn = cLines.firstOrNull()
            val gwStr = firstCtn?.cartonGrossWeightKg?.toBigDecimalOrNull()
                ?.takeIf { it > BigDecimal.ZERO }?.toPlainString()
                ?: cLines.fold(BigDecimal.ZERO) { sum, line -> sum + (line.grossWeightKg.toBigDecimalOrNull() ?: BigDecimal.ZERO) }.toPlainString()
            CartonBoxState(
                cartonNumber = cNum,
                grossWeightKgStr = gwStr,
                netWeightKgStr = cLines.fold(BigDecimal.ZERO) { sum, line -> sum + BigDecimal(line.netWeightKg.ifBlank { "0" }) }.toPlainString(),
                lengthCm = firstCtn?.lengthCm?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                widthCm = firstCtn?.widthCm?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                heightCm = firstCtn?.heightCm?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                cbm = firstCtn?.cbm?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
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
    fun snapshotEditor(includeStep: Boolean = true) = JSONObject().apply {
        put("supplier", selectedSupplierId); put("currency", selectedCurrency); put("local", isLocalPurchase)
        put("rate", exchangeRateStr); put("delivery", chinaLocalDeliveryStr); put("handling", handlingCostStr)
        put("sample", sampleCostStr); put("forwarder", forwarderAddressStr); put("notes", notes)
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
                orderDateMillis = draft.optLong("poOrderDate", orderDateMillis)
                expectedArrivalDateMillis = if (draft.isNull("expectedArrivalDate")) null else draft.optLong("expectedArrivalDate")
                selectedShippingMethod = draft.optString("shippingMethod", selectedShippingMethod)
                notes = draft.optString("notes")
                selectedPaymentTerm = selectedPaymentTerm.javaClass.enumConstants.firstOrNull { it.name == draft.optString("paymentTerm") } ?: selectedPaymentTerm
                draft.optJSONArray("attachments")?.let { a -> attachmentsList = (0 until a.length()).map { a.getString(it) } }
                draft.optJSONArray("items")?.let { a -> itemsList.clear(); repeat(a.length()) { itemsList.add(itemFromJson(a.getJSONObject(it))) } }
                draft.optJSONArray("cartons")?.let { a -> cartonBoxes.clear(); repeat(a.length()) { cartonBoxes.add(cartonFromJson(a.getJSONObject(it))) } }
                draft.optJSONObject("freight")?.let { j -> categoryRateStrings.clear(); j.keys().forEach { categoryRateStrings[it] = j.getString(it) } }
                currentStep = draft.optInt("step").coerceIn(0, 3)
                hasHydratedItems = true; hasHydratedCartons = true; hasHydratedCosts = true
                restoredDraft = true
            }
            draftLoaded = true
            initialSnapshot = snapshotEditor(includeStep = false).toString()
        }
    }
    val requestClose: () -> Unit = {
        if (!isSaving && !savingBuffer) {
            if (snapshotEditor(includeStep = false).toString() != initialSnapshot) showLeavePrompt = true else onDismiss()
        }
    }
    if (showLeavePrompt) ERPLeaveEditorDialog(
        isBangla = isBangla,
        isSaving = savingBuffer,
        onContinue = { showLeavePrompt = false },
        onDiscard = { draftStore.remove(draftKey); showLeavePrompt = false; onDismiss() },
        onSaveDraft = {
            if (!savingBuffer) {
                val snapshot = snapshotEditor()
                savingBuffer = true
                draftScope.launch {
                    try {
                        withContext(Dispatchers.IO) { draftStore.save(draftKey, snapshot) }
                        onDismiss()
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
        val terms = "[Terms: ${selectedPaymentTerm.labelEn}]"
        val forwarder = forwarderAddressStr.trim().takeIf { it.isNotEmpty() }?.let { "[Forwarder: $it]" }
        return listOfNotNull(terms, forwarder, notes.trim().takeIf { it.isNotEmpty() }).joinToString(" • ")
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
                    val allocated = CartonWeightAllocationEngine.allocateWeights(
                        box.grossWeightKgStr.toBigDecimal(), box.netWeightKg(),
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
                            cartonNumber = box.cartonNumber.trim().ifBlank { "CTN-%02d".format(idx + 1) },
                            grossWeightKg = gw,
                            netWeightKg = box.netWeightKg(),
                            lengthCm = box.lengthCm, widthCm = box.widthCm, heightCm = box.heightCm,
                            cbm = box.cbm,
                            lines = validLines
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

    ERPFullScreenEditor(
        onDismissRequest = requestClose
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(0.dp)),
            color = ERPBackground,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryPurple.copy(alpha = 0.10f)
                    ) {
                        Icon(
                            Icons.Outlined.ShoppingCart,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.padding(9.dp).size(22.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (isEditing) {
                                if (isBangla) "ক্রয়াদেশ সম্পাদনা" else "Edit purchase order"
                            } else if (isBangla) "নতুন ক্রয়াদেশ" else "New purchase order",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            editingOrder?.poNumber
                                ?: if (isBangla) "তথ্য → পণ্য → প্যাকিং → হিসাব" else "Information → Products → Packing → Cost",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = requestClose, enabled = !isSaving && !savingBuffer) {
                        Icon(Icons.Outlined.Close, contentDescription = if (isBangla) "বন্ধ করুন" else "Close", tint = TextSecondary)
                    }
                }

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
                    // 1. Supplier Selection + Inline New Supplier
                    // ==========================================
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBangla) "সরবরাহকারী" else "Supplier",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (onAddNewSupplier != null) {
                                    Button(
                                        onClick = { showInlineAddSupplierDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = "New Supplier", tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isBangla) "নতুন" else "New",
                                            color = SecondaryTeal,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            if (suppliers.isEmpty()) {
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
                                            text = if (isBangla) "কোনো সরবরাহকারী নেই। উপরের '+ নতুন সরবরাহকারী' বাটনে চাপ দিয়ে নতুন সরবরাহকারী তৈরি করুন।" else "No suppliers found. Tap '+ New Supplier' above to create one.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = StatusWarning
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (suppliers.size > 4) {
                                        OutlinedTextField(
                                            value = supplierSearch,
                                            onValueChange = { supplierSearch = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                            label = { Text(if (isBangla) "সরবরাহকারী খুঁজুন" else "Search suppliers") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                    }
                                    val filteredSuppliers = suppliers.filter { supplier ->
                                        supplierSearch.isBlank() || supplier.companyName.contains(supplierSearch, ignoreCase = true) ||
                                            supplier.contactPerson.orEmpty().contains(supplierSearch, ignoreCase = true) ||
                                            supplier.country.contains(supplierSearch, ignoreCase = true)
                                    }
                                    filteredSuppliers.forEach { sup ->
                                        val isSelected = sup.id == selectedSupplierId
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) PrimaryPurple.copy(alpha = 0.12f) else ERPSurfaceVariant)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.dp,
                                                    color = if (isSelected) PrimaryPurple else Color.Transparent,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    selectedSupplierId = sup.id
                                                    if (!isLocalPurchase && sup.currency.isNotBlank()) {
                                                        selectedCurrency = sup.currency
                                                        exchangeRateStr = when (sup.currency) {
                                                            "CNY" -> "17.00"
                                                            "USD" -> "120.00"
                                                            else -> "1.00"
                                                        }
                                                    } else if (isLocalPurchase) {
                                                        selectedCurrency = "BDT"
                                                        exchangeRateStr = "1.00"
                                                    }
                                                }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(if (isSelected) PrimaryPurple else Color.Gray, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = sup.companyName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) PrimaryPurple else TextPrimary
                                                )
                                                Text(
                                                    text = "${sup.country} • Currency: ${sup.currency}" + (if (!sup.contactPerson.isNullOrBlank()) " • ${sup.contactPerson}" else ""),
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

                    // ==========================================
                    // 2. Currency, Exchange Rate & Delivery Charges
                    // ==========================================
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.18f))
                        ) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    if (isBangla) "ক্রয়াদেশের তথ্য" else "Purchase order information",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = {
                                            isLocalPurchase = false
                                            if (selectedCurrency == "BDT") {
                                                selectedCurrency = "CNY"
                                                exchangeRateStr = "17.00"
                                            }
                                        },
                                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (!isLocalPurchase) PrimaryPurple else MaterialTheme.colorScheme.surfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (!isLocalPurchase) PrimaryPurple else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                if (isBangla) "আমদানি" else "Import",
                                                color = if (!isLocalPurchase) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = {
                                            isLocalPurchase = true
                                            selectedCurrency = "BDT"
                                            exchangeRateStr = "1.00"
                                        },
                                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isLocalPurchase) SecondaryTeal else MaterialTheme.colorScheme.surfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isLocalPurchase) SecondaryTeal else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                if (isBangla) "স্থানীয়" else "Local",
                                                color = if (isLocalPurchase) Color.White else TextPrimary,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = editingOrder?.poNumber ?: if (isBangla) "সংরক্ষণের পর স্বয়ংক্রিয়ভাবে তৈরি হবে" else "Assigned automatically when saved",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(if (isBangla) "PO নম্বর" else "PO number") },
                                    supportingText = { Text(if (isBangla) "সিস্টেমের অনন্য নম্বর" else "Unique system reference") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PremiumPoDateField(
                                        label = if (isBangla) "ক্রয়ের তারিখ" else "Purchase date",
                                        dateMillis = orderDateMillis,
                                        language = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH,
                                        modifier = Modifier.weight(1f),
                                        onDateSelected = { it?.let { selected -> orderDateMillis = selected } }
                                    )
                                    PremiumPoDateField(
                                        label = if (isBangla) "সম্ভাব্য পৌঁছানোর তারিখ" else "Expected arrival",
                                        dateMillis = expectedArrivalDateMillis,
                                        language = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH,
                                        modifier = Modifier.weight(1f),
                                        allowClear = true,
                                        onDateSelected = { expectedArrivalDateMillis = it }
                                    )
                                }
                                Text(if (isBangla) "শিপিং পদ্ধতি" else "Shipping method", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(ShippingMethod.values().toList()) { method ->
                                        val selected = selectedShippingMethod == method.name
                                        Surface(
                                            onClick = { selectedShippingMethod = method.name },
                                            shape = RoundedCornerShape(13.dp),
                                            color = if (selected) PrimaryPurple.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (selected) PrimaryPurple else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text(
                                                if (isBangla) method.labelBn else method.labelEn,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selected) PrimaryPurple else TextPrimary,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
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
                                    maxLines = 3,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text(if (isBangla) "অর্ডার নোট / রেফারেন্স" else "Order notes / reference") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    maxLines = 4,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Text(if (isBangla) "পেমেন্ট শর্ত" else "Payment terms", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(PaymentTermType.values().toList()) { term ->
                                        val selected = selectedPaymentTerm == term
                                        Surface(
                                            onClick = { selectedPaymentTerm = term },
                                            shape = RoundedCornerShape(13.dp),
                                            color = if (selected) PrimaryPurple else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text(
                                                if (isBangla) term.labelBn else term.labelEn,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        if (isLocalPurchase) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = StatusSuccessContainer),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isBangla) "২. স্থানীয় ক্রয় খরচ ও পরিবহন (বাংলাদেশ):" else "2. Local Purchase & Transport Costs (BD):",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SecondaryTeal.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "মুদ্রা: ৳ BDT • রেট: ১.০০",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = SecondaryTeal,
                                                modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 12.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = chinaLocalDeliveryStr,
                                            onValueChange = { chinaLocalDeliveryStr = it },
                                            label = { Text(if (isBangla) "স্থানীয় পরিবহন বা ডেলিভারি খরচ (৳)" else "Local Transport / Courier (৳)") },
                                            placeholder = { Text("0") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = handlingCostStr,
                                            onValueChange = { handlingCostStr = it },
                                            label = { Text(if (isBangla) "আনলোডিং বা লেবার খরচ (৳)" else "Unloading / Labour Cost (৳)") },
                                            placeholder = { Text("0") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        } else {
                            Column {
                                Text(
                                    text = if (isBangla) "মুদ্রা ও বিনিময় হার" else "Currency & exchange rate",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Currency Toggle (CNY / USD)
                                    Row(
                                        modifier = Modifier
                                            .background(ERPSurfaceVariant, RoundedCornerShape(10.dp))
                                            .padding(3.dp)
                                    ) {
                                        listOf("CNY", "USD").forEach { cur ->
                                            val isSel = selectedCurrency == cur
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSel) PrimaryPurple else Color.Transparent)
                                                    .clickable {
                                                        selectedCurrency = cur
                                                        exchangeRateStr = if (cur == "CNY") "17.00" else "120.00"
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = cur,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else TextPrimary
                                                )
                                            }
                                        }
                                    }

                                    // Rate Input
                                    OutlinedTextField(
                                        value = exchangeRateStr,
                                        onValueChange = { exchangeRateStr = it },
                                        label = { Text("1 $selectedCurrency = ? BDT") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f).testTag("po_exchange_rate"),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // China Local Delivery Charge Input
                                val rateBd = exchangeRateStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                                val chinaDelivBd = chinaLocalDeliveryStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                                val chinaDelivBdt = chinaDelivBd.multiply(rateBd).setScale(2, RoundingMode.HALF_UP)

                                OutlinedTextField(
                                    value = chinaLocalDeliveryStr,
                                    onValueChange = { chinaLocalDeliveryStr = it },
                                    label = {
                                        Text(
                                            if (isBangla)
                                                "চীন লোকাল ডেলিভারি ($selectedCurrency)"
                                            else
                                                "China delivery ($selectedCurrency)"
                                        )
                                    },
                                    supportingText = {
                                        Text(
                                            text = if (isBangla)
                                                "বাংলাদেশি টাকায় রূপান্তরিত: ৳ $chinaDelivBdt (পরিমাণ/PCS অনুপাতে পণ্যে বণ্টিত হবে)"
                                            else
                                                "Converted to BDT: ৳ $chinaDelivBdt (Allocated to products by quantity)",
                                            color = SecondaryTeal,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // ==========================================
                    }
                    if (currentStep == 1) {
                    // 3. Order Items + Product Variant Selection
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
                                    Button(
                                        onClick = { showInlineAddProductDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = "New Product", tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isBangla) "নতুন পণ্য" else "New product",
                                            color = SecondaryTeal,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (products.isNotEmpty()) {
                                            val pId = products.first().id
                                            val vId = variants.firstOrNull { it.productId == pId }?.id
                                            itemsList.add(ItemDraftState(productId = pId, variantId = vId))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = "Add Item", tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "+ আইটেম/ভ্যারিয়েন্ট লাইন" else "+ Add Line",
                                        color = PrimaryPurple,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
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
                                        text = if (isBangla) "কোনো পণ্য পাওয়া যায়নি। ওপরের '+ নতুন পণ্য' বোতামে ক্লিক করুন।" else "No products found. Click '+ New Product' above to create one.",
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
                                        text = if (isBangla) "কোনো পণ্য লাইন এখনও যোগ করা হয়নি। ওপরের বোতাম ব্যবহার করে পণ্য বা লাইন যোগ করুন।" else "No product lines added yet. Click '+ New Product' or '+ Add Line' above.",
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

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.18f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
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
                                    if (itemsList.size > 1) {
                                        IconButton(
                                            onClick = { itemsList.removeAt(index) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Remove",
                                                tint = StatusError,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Product Selector Chips
                                Text(
                                    text = if (isBangla) "পণ্য নির্বাচন করুন:" else "Select Product:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(products) { prod ->
                                        val isProdSel = itemState.productId == prod.id
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isProdSel) PrimaryPurple.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                                .border(
                                                    width = if (isProdSel) 1.dp else 0.5.dp,
                                                    color = if (isProdSel) PrimaryPurple else ERPDivider,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    itemState.productId = prod.id
                                                    val firstV = variants.firstOrNull { it.productId == prod.id }
                                                    itemState.variantId = firstV?.id
                                                    if (firstV != null && firstV.variantCostMinor > 0) {
                                                        itemState.unitPriceStr = (firstV.variantCostMinor / 100.0).toString()
                                                    } else if (prod.purchasePriceMinor > 0) {
                                                        itemState.unitPriceStr = (prod.purchasePriceMinor / 100.0).toString()
                                                    }
                                                }
                                                .heightIn(min = 48.dp).padding(horizontal = 10.dp, vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = if (isBangla && prod.nameBn.isNotBlank()) prod.nameBn else prod.nameEn,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isProdSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isProdSel) PrimaryPurple else TextPrimary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Variant Selector Chips + Add Variant / Add All Variants
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isBangla) "ভ্যারিয়েন্ট (${productVariants.size})" else "Variant (${productVariants.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (productVariants.size > 1) {
                                            Text(
                                                text = if (isBangla) "+ সব ভ্যারিয়েন্ট যোগ" else "+ All Variants",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryPurple,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(PrimaryPurple.copy(alpha = 0.1f))
                                                    .clickable {
                                                        val existingKeys = itemsList.map { it.productId to it.variantId }.toSet()
                                                        productVariants.forEachIndexed { vIdx, pv ->
                                                            if (vIdx == 0 && itemState.variantId == null) {
                                                                itemState.variantId = pv.id
                                                            } else if ((itemState.productId to pv.id) !in existingKeys) {
                                                                itemsList.add(
                                                                    ItemDraftState(
                                                                        productId = itemState.productId,
                                                                        variantId = pv.id,
                                                                        quantityStr = itemState.quantityStr,
                                                                        unitPriceStr = if (pv.variantCostMinor > 0) (pv.variantCostMinor / 100.0).toString() else itemState.unitPriceStr,
                                                                        cartonQuantityStr = "0",
                                                                        pcsPerCartonStr = "",
                                                                        grossWeightKgStr = "",
                                                                        netWeightKgStr = ""
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                    .heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 12.dp)
                                            )
                                        }
                                        if (onAddNewVariant != null && itemState.productId.isNotBlank()) {
                                            Text(
                                                text = if (isBangla) "+ নতুন ভ্যারিয়েন্ট" else "+ New Variant",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = SecondaryTeal,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(SecondaryTeal.copy(alpha = 0.12f))
                                                    .clickable {
                                                        targetItemIndexForVariant = index
                                                        showInlineAddVariantForProductId = itemState.productId
                                                        newVarColor = ""
                                                        newVarSize = ""
                                                        newVarSku = ""
                                                        newVarCostStr = itemState.unitPriceStr
                                                    }
                                                    .heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 12.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Base / No-variant chip
                                    item {
                                        val isBaseSel = itemState.variantId == null
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isBaseSel) SecondaryTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                                .border(
                                                    width = if (isBaseSel) 1.dp else 0.5.dp,
                                                    color = if (isBaseSel) SecondaryTeal else ERPDivider,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { itemState.variantId = null }
                                                .heightIn(min = 48.dp).padding(horizontal = 10.dp, vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = if (isBangla) "বেস পণ্য" else "Base product",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isBaseSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isBaseSel) SecondaryTeal else TextSecondary
                                            )
                                        }
                                    }
                                    items(productVariants) { pv ->
                                        val isVarSel = itemState.variantId == pv.id
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isVarSel) SecondaryTeal.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface)
                                                .border(
                                                    width = if (isVarSel) 1.2.dp else 0.5.dp,
                                                    color = if (isVarSel) SecondaryTeal else ERPDivider,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    itemState.variantId = pv.id
                                                    if (pv.variantCostMinor > 0) {
                                                        itemState.unitPriceStr = (pv.variantCostMinor / 100.0).toString()
                                                    }
                                                }
                                                .heightIn(min = 48.dp).padding(horizontal = 10.dp, vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = variantLabel(pv),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isVarSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isVarSel) SecondaryTeal else TextPrimary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = itemState.quantityStr,
                                        onValueChange = { itemState.quantityStr = it },
                                        label = { Text(if (isBangla) "পরিমাণ (Qty)" else "Quantity") },
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
                            }
                        }
                    }

                    // ==========================================
                    }
                    if (currentStep == 2) {
                    // 4. Carton-by-Carton Packaging (Mixed Variants Supported)
                    // ==========================================
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StatusSuccessContainer),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBangla) "৪. কার্টন অনুযায়ী প্যাকিং ও ওজন (একই কার্টনে একাধিক ভ্যারিয়েন্ট)" else "4. Carton-by-Carton Packing (Mixed Variants Supported)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (isBangla) "প্রতিটি কার্টনে কোন ভ্যারিয়েন্ট কত পিস ও কার্টনের ওজন (KG) দিন — শিপমেন্টে অটো যুক্ত হবে" else "Assign single or multiple variants per carton with carton weight (KG)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (cartonBoxes.isEmpty() && itemsList.isNotEmpty()) {
                                            OutlinedButton(
                                                onClick = {
                                                    itemsList.forEachIndexed { idx, itm ->
                                                        if (itm.productId.isNotBlank()) {
                                                            cartonBoxes.add(
                                                                CartonBoxState(
                                                                    cartonNumber = "CTN-%02d".format(idx + 1),
                                                                    grossWeightKgStr = itm.grossWeightKgStr.ifBlank { "15.0" },
                                                                    initialLines = listOf(
                                                                        CartonLineState(
                                                                            productId = itm.productId,
                                                                            variantId = itm.variantId,
                                                                            quantityStr = itm.quantityStr.ifBlank { "20" }
                                                                        )
                                                                    )
                                                                )
                                                            )
                                                        }
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (isBangla) "অটো-কার্টন" else "Auto-Fill",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SecondaryTeal
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                val nextNum = "CTN-%02d".format(cartonBoxes.size + 1)
                                                val defItem = itemsList.firstOrNull()
                                                cartonBoxes.add(
                                                    CartonBoxState(
                                                        cartonNumber = nextNum,
                                                        grossWeightKgStr = "15.0",
                                                        initialLines = listOf(
                                                            CartonLineState(
                                                                productId = defItem?.productId ?: firstProdId,
                                                                variantId = defItem?.variantId ?: firstVarId,
                                                                quantityStr = "20"
                                                            )
                                                        )
                                                    )
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal, contentColor = MaterialTheme.colorScheme.onSecondary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isBangla) "+ কার্টন যোগ" else "+ Add Carton",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                if (cartonBoxes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Mixed Carton Explanatory Guide
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = StatusInfoContainer),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusInfoContainer),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Outlined.Info, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isBangla) "💡 একই কার্টনে একাধিক প্রোডাক্ট বা ভ্যারিয়েন্ট (মিক্সড কার্টন) প্যাকিং গাইড:" else "💡 Mixed Carton Packing Guide:",
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (isBangla)
                                                    "• প্রতিটি কার্টনে যত ইচ্ছা ভিন্ন ভিন্ন পণ্য বা রঙের/সাইজের ভ্যারিয়েন্ট একসাথে রাখতে পারেন।\n• নিচে '+ এই একই কার্টনে অন্য প্রোডাক্ট / ভ্যারিয়েন্ট মিক্স করুন' চাপলে নতুন লাইন যুক্ত হবে।\n• কার্টনের মোট ওজন (KG) দিলে প্রতিটি পণ্যের পিস (PCS) অনুপাতে সিস্টেম নিজে থেকেই স্বয়ংক্রিয়ভাবে ওজন ও ফ্রেইট বণ্টন করবে।"
                                                else
                                                    "• Pack single or multiple different products and variants inside any carton.\n• Tap '+ Mix Another Product/Variant' below to add another item to this box.\n• Enter gross weight (KG); weights are allocated proportionally per piece.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = StatusInfo,
                                                fontSize = 10.5.sp,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        cartonBoxes.forEachIndexed { cIdx, box ->
                                            val boxTotalPcs = box.lines.sumOf { it.quantityStr.trim().toIntOrNull() ?: 0 }
                                            val boxGw = box.grossWeightKgStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                                            val distinctLinesCount = box.lines.map { it.productId to it.variantId }.distinct().size
                                            val isMixedCarton = distinctLinesCount > 1

                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                shape = RoundedCornerShape(18.dp),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = if (isMixedCarton) 1.5.dp else 1.dp,
                                                    color = if (isMixedCarton) SecondaryTeal else PrimaryPurple.copy(alpha = 0.18f)
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = if (isMixedCarton)
                                                                    (if (isBangla) "🔀 কার্টন #${cIdx + 1} (মিক্সড কার্টন • ${distinctLinesCount}টি আইটেম • $boxTotalPcs PCS)"
                                                                    else "🔀 Carton #${cIdx + 1} (Mixed • ${distinctLinesCount} items • $boxTotalPcs PCS)")
                                                                else
                                                                    (if (isBangla) "📦 কার্টন #${cIdx + 1} (একক পণ্য • $boxTotalPcs PCS)"
                                                                    else "📦 Carton #${cIdx + 1} (Single • $boxTotalPcs PCS)"),
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isMixedCarton) SecondaryTeal else PrimaryPurple
                                                            )
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            // Duplicate Carton Button
                                                            IconButton(
                                                                onClick = {
                                                                    val nextNum = "CTN-%02d".format(cartonBoxes.size + 1)
                                                                    cartonBoxes.add(
                                                                        CartonBoxState(
                                                                            cartonNumber = nextNum,
                                                                            grossWeightKgStr = box.grossWeightKgStr,
                                                                            netWeightKgStr = box.netWeightKgStr,
                                                                            lengthCm = box.lengthCm, widthCm = box.widthCm, heightCm = box.heightCm,
                                                                            cbm = box.cbm,
                                                                            initialLines = box.lines.map { l ->
                                                                                CartonLineState(
                                                                                    productId = l.productId,
                                                                                    variantId = l.variantId,
                                                                                    quantityStr = l.quantityStr,
                                                                                    manualWeightKgStr = l.manualWeightKgStr
                                                                                )
                                                                            }
                                                                        )
                                                                    )
                                                                },
                                                                modifier = Modifier.size(48.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Outlined.ContentCopy,
                                                                    contentDescription = "Duplicate Carton",
                                                                    tint = SecondaryTeal,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = { cartonBoxes.removeAt(cIdx) },
                                                                modifier = Modifier.size(48.dp)
                                                            ) {
                                                                Icon(Icons.Outlined.Delete, contentDescription = "Remove Carton", tint = StatusError, modifier = Modifier.size(17.dp))
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    OutlinedTextField(
                                                        value = box.cartonNumber, onValueChange = { box.cartonNumber = it },
                                                        label = { Text(if (isBangla) "কার্টন নম্বর" else "Carton no.") },
                                                        modifier = Modifier.fillMaxWidth(), singleLine = true
                                                    )
                                                    Spacer(Modifier.height(8.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        OutlinedTextField(
                                                            value = box.grossWeightKgStr, onValueChange = { box.grossWeightKgStr = it },
                                                            label = { Text(if (isBangla) "গ্রস ওজন (KG)" else "Gross (kg)") },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            modifier = Modifier.weight(1f), singleLine = true
                                                        )
                                                        OutlinedTextField(
                                                            value = box.netWeightKgStr, onValueChange = { box.netWeightKgStr = it },
                                                            label = { Text(if (isBangla) "নেট ওজন (KG)" else "Net (kg)") },
                                                            placeholder = { Text(box.grossWeightKgStr) },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            modifier = Modifier.weight(1f), singleLine = true
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

                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = if (isBangla) "এই কার্টনের ভেতরের পণ্য ও ভ্যারিয়েন্ট সমূহ:" else "Products / Variants inside this Carton:",
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
                                                        val lineQty = lineState.quantityStr.trim().toIntOrNull() ?: 0
                                                        val manualWt = lineState.manualWeightKgStr.trim().toBigDecimalOrNull()
                                                        val allocatedKg = allocationPreview.getOrNull()?.getOrNull(lIdx)?.allocatedGrossWeightKg ?: BigDecimal.ZERO
                                                        val perPcKg = if (lineQty > 0) allocatedKg.divide(BigDecimal(lineQty), 3, RoundingMode.HALF_UP) else BigDecimal.ZERO

                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 4.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(ERPSurfaceVariant)
                                                                .border(0.5.dp, ERPDivider, RoundedCornerShape(8.dp))
                                                                .padding(8.dp)
                                                        ) {
                                                            // Select from PO Order Items & Catalog Variants
                                                            val allCartonOptions = remember(itemsList.size, products.size, variants.size) {
                                                                val list = mutableListOf<Triple<String, String?, String>>()
                                                                val addedKeys = mutableSetOf<Pair<String, String?>>()

                                                                // 1. Items currently in PO
                                                                itemsList.forEach { itm ->
                                                                    val key = itm.productId to itm.variantId
                                                                    if (addedKeys.add(key)) {
                                                                        val p = products.find { it.id == itm.productId }
                                                                        val v = variants.find { it.id == itm.variantId }
                                                                        val pName = if (isBangla && !p?.nameBn.isNullOrBlank()) p!!.nameBn else (p?.nameEn ?: "Item")
                                                                        val vName = if (v != null) listOfNotNull(v.color, v.size).joinToString("/").ifBlank { v.sku } else (if (isBangla) "বেস" else "Base")
                                                                        list.add(Triple(itm.productId, itm.variantId, "$pName ($vName)"))
                                                                    }
                                                                }

                                                                // 2. All other variants for products in PO
                                                                val poProductIds = itemsList.map { it.productId }.toSet()
                                                                poProductIds.forEach { pId ->
                                                                    val p = products.find { it.id == pId }
                                                                    val pName = if (isBangla && !p?.nameBn.isNullOrBlank()) p!!.nameBn else (p?.nameEn ?: "Item")
                                                                    variants.filter { it.productId == pId }.forEach { pv ->
                                                                        val key = pId to pv.id
                                                                        if (addedKeys.add(key)) {
                                                                            val vName = listOfNotNull(pv.color, pv.size).joinToString("/").ifBlank { pv.sku }
                                                                            list.add(Triple(pId, pv.id, "$pName ($vName) [নতুন]"))
                                                                        }
                                                                    }
                                                                }

                                                                list
                                                            }

                                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                items(allCartonOptions) { opt ->
                                                                    val isSel = lineState.productId == opt.first && lineState.variantId == opt.second
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(6.dp))
                                                                            .background(if (isSel) PrimaryPurple else MaterialTheme.colorScheme.surface)
                                                                            .border(1.dp, if (isSel) PrimaryPurple else ERPDivider, RoundedCornerShape(6.dp))
                                                                            .clickable {
                                                                                lineState.productId = opt.first
                                                                                lineState.variantId = opt.second
                                                                                // Auto-add to itemsList if not already present
                                                                                if (itemsList.none { it.productId == opt.first && it.variantId == opt.second }) {
                                                                                    val pv = variants.find { it.id == opt.second }
                                                                                    val prod = products.find { it.id == opt.first }
                                                                                    val cost = pv?.variantCostMinor?.takeIf { it > 0 } ?: prod?.purchasePriceMinor ?: 0L
                                                                                    itemsList.add(
                                                                                        ItemDraftState(
                                                                                            productId = opt.first,
                                                                                            variantId = opt.second,
                                                                                            quantityStr = lineState.quantityStr.ifBlank { "20" },
                                                                                            unitPriceStr = if (cost > 0) "%.2f".format(cost / 100.0) else "45.00"
                                                                                        )
                                                                                    )
                                                                                }
                                                                            }
                                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = opt.third,
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                                                        )
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
                                                                    label = { Text(if (isBangla) "পিস (PCS)" else "PCS") },
                                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                                    modifier = Modifier.weight(1f),
                                                                    singleLine = true
                                                                )
                                                                OutlinedTextField(
                                                                    value = lineState.manualWeightKgStr,
                                                                    onValueChange = { lineState.manualWeightKgStr = it },
                                                                    label = { Text(if (isBangla) "ম্যানুয়াল ওজন (KG)" else "Manual Wt (KG)") },
                                                                    placeholder = { Text("ঐচ্ছিক") },
                                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                                    modifier = Modifier.weight(1.2f),
                                                                    singleLine = true
                                                                )
                                                                if (box.lines.size > 1) {
                                                                    IconButton(
                                                                        onClick = { box.lines.removeAt(lIdx) },
                                                                        modifier = Modifier.size(24.dp)
                                                                    ) {
                                                                        Icon(Icons.Outlined.Close, contentDescription = "Remove Line", tint = StatusError, modifier = Modifier.size(16.dp))
                                                                    }
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = if (isBangla) "মোট ওজন: $allocatedKg KG ($perPcKg KG/pc)" else "Line Wt: $allocatedKg KG ($perPcKg KG/pc)",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = SecondaryTeal
                                                                )
                                                                if (manualWt != null && manualWt > BigDecimal.ZERO) {
                                                                    Text(
                                                                        text = if (isBangla) "ম্যানুয়াল নির্ধারিত" else "Manual override",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = PrimaryPurple,
                                                                        fontWeight = FontWeight.SemiBold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Button(
                                                        onClick = {
                                                            val nextPoItem = itemsList.lastOrNull()
                                                            box.lines.add(
                                                                CartonLineState(
                                                                    productId = nextPoItem?.productId ?: firstProdId,
                                                                    variantId = nextPoItem?.variantId,
                                                                    quantityStr = "10"
                                                                )
                                                            )
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal.copy(alpha = 0.15f)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Outlined.Add, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = if (isBangla)
                                                                "+ এই কার্টনে আরেকটি ভ্যারিয়েন্ট/পণ্য মিক্স করুন"
                                                            else
                                                                "+ Mix Another Variant/Product in this Carton",
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
                            }
                        }
                    }

                    // ==========================================
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

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(22.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryPurple.copy(alpha = 0.30f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isBangla)
                                        "৫. আর্থিক সামারি ও ওয়েট-বেসড কস্টিং বরাদ্দ (Weight-Based Costing Allocation)"
                                    else
                                        "5. Financial Summary & Weight-Based Costing Allocation",
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
                                        supportingText = { Text(if (isBangla) "আনলোডিং ও পোর্টার চার্জ" else "Unloading & Porter charge", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = sampleCostStr,
                                        onValueChange = { sampleCostStr = it },
                                        label = { Text(if (isBangla) "স্যাম্পল খরচ (৳)" else "Sample Cost (BDT)") },
                                        supportingText = { Text(if (isBangla) "প্রোটোটাইপ স্যাম্পল খরচ" else "Pre-order sample cost", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

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
                                PremiumPoCostLine(if (isBangla) "হ্যান্ডলিং ও আনলোডিং খরচ (ক্যাপিটালাইজড):" else "Handling & Porter (Capitalized):", "৳ $handlingCostBdt")
                                if (sampleCostBdt > BigDecimal.ZERO) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    PremiumPoCostLine(if (isBangla) "স্যাম্পল প্রসেসিং খরচ (ক্যাপিটালাইজড):" else "Sample Processing (Capitalized):", "৳ $sampleCostBdt")
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = ERPDivider)
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.22f))
                                ) {
                                    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                        Text(
                                            if (isBangla) "সর্বমোট ল্যান্ডেড কস্ট" else "Total landed cost",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "৳ $grandTotalEstimatedBdt",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = if (isBangla)
                                                "গড় প্রতি পিস: ৳ $avgUnitLandedBdt / PCS"
                                            else
                                                "Average per unit: ৳ $avgUnitLandedBdt / PCS",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

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

                HorizontalDivider(color = ERPDivider, modifier = Modifier.padding(vertical = 10.dp))

                (errorMessage ?: saveError)?.let { message ->
                    Text(message, color = StatusError, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (currentStep > 0 && !metadataOnly) OutlinedButton(
                        onClick = { currentStep-- }, enabled = !isSaving,
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
            }
        }
    }

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
            onConfirm = { nameEn, nameBn, baseSku, barcode, catId, unit, purchaseMinor, sellMinor, minStock, photos, vars, _, _ ->
                onCreateFullProduct(
                    nameEn, nameBn, baseSku, barcode, catId, unit, purchaseMinor, sellMinor, minStock, photos, vars
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
