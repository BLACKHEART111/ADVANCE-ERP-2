package com.pocketdev.advance_erp.feature.imports

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.SuggestTextField
import com.pocketdev.advance_erp.data.entity.SuggestionType
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
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.feature.product.AddProductDialog
import com.pocketdev.advance_erp.feature.product.GalleryItemDraft
import com.pocketdev.advance_erp.feature.product.NewVariantDraft
import java.math.BigDecimal
import java.math.RoundingMode

private class ItemDraftState(
    productId: String = "",
    variantId: String? = null,
    quantityStr: String = "100",
    unitPriceStr: String = "45.00",
    cartonQuantityStr: String = "5",
    pcsPerCartonStr: String = "20",
    grossWeightKgStr: String = "35.0",
    netWeightKgStr: String = "33.0"
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
    initialLines: List<CartonLineState> = emptyList()
) {
    var cartonNumber by mutableStateOf(cartonNumber)
    var grossWeightKgStr by mutableStateOf(grossWeightKgStr)
    val lines = mutableStateListOf<CartonLineState>().apply {
        addAll(initialLines)
    }
}

internal data class ParsedPoNotes(
    val paymentTerm: PaymentTermType,
    val forwarderAddress: String,
    val userNotes: String
)

internal fun parsePoNotesMetadata(rawNotes: String?): ParsedPoNotes {
    val defaultForwarder = "Guangzhou Baiyun Cargo Terminal / Yiwu Port Warehouse, China"
    if (rawNotes.isNullOrBlank()) {
        return ParsedPoNotes(PaymentTermType.THIRTY_SEVENTY, defaultForwarder, "")
    }
    val termsRegex = Regex("""\[Terms:\s*([^\]]+)]""")
    val termsText = termsRegex.find(rawNotes)?.groupValues?.getOrNull(1)?.trim() ?: ""
    val resolvedTerm = PaymentTermType.entries.firstOrNull {
        termsText.equals(it.labelEn, ignoreCase = true) || termsText.equals(it.labelBn, ignoreCase = true)
    } ?: when {
        termsText.contains("100% Advance", ignoreCase = true) || rawNotes.contains("100% Advance", ignoreCase = true) -> PaymentTermType.HUNDRED_PERCENT_ADVANCE
        termsText.contains("50%", ignoreCase = true) || rawNotes.contains("50% Advance", ignoreCase = true) -> PaymentTermType.FIFTY_FIFTY
        termsText.contains("Custom", ignoreCase = true) || rawNotes.contains("Custom Partial", ignoreCase = true) -> PaymentTermType.CUSTOM_PARTIAL
        termsText.contains("Full Due", ignoreCase = true) || rawNotes.contains("Full Due", ignoreCase = true) -> PaymentTermType.FULL_DUE
        else -> PaymentTermType.THIRTY_SEVENTY
    }
    val forwarderRegex = Regex("""\[Forwarder:\s*([^\]]+)]""")
    val resolvedForwarder = forwarderRegex.find(rawNotes)?.groupValues?.getOrNull(1)?.trim()
        ?.ifBlank { defaultForwarder } ?: defaultForwarder
    val cleanUserNotes = rawNotes
        .replace(termsRegex, "")
        .replace(forwarderRegex, "")
        .split("•")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(" • ")
    return ParsedPoNotes(resolvedTerm, resolvedForwarder, cleanUserNotes)
}

@Composable
fun CreatePurchaseOrderDialog(
    suppliers: List<Supplier>,
    products: List<Product>,
    variants: List<ProductVariant> = emptyList(),
    categories: List<ProductCategory> = emptyList(),
    isBangla: Boolean = false,
    onDismiss: () -> Unit,
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
        attachments: String?
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
        categoryFreightRatesBdtMinor: Map<String, Long>
    ) -> Unit
) {
    val isEditing = editingOrder != null
    val parsedNotes = remember(editingOrder?.id, editingOrder?.notes) {
        parsePoNotesMetadata(editingOrder?.notes)
    }
    var isLocalPurchase by remember(editingOrder?.id) { mutableStateOf(editingOrder?.currency == "BDT") }
    var selectedSupplierId by remember(editingOrder?.id) { mutableStateOf(editingOrder?.supplierId ?: (suppliers.firstOrNull()?.id ?: "")) }
    var selectedCurrency by remember(editingOrder?.id) {
        mutableStateOf(
            if (isLocalPurchase) "BDT"
            else editingOrder?.currency ?: (suppliers.firstOrNull()?.currency?.ifBlank { "CNY" } ?: "CNY")
        )
    }
    var exchangeRateStr by remember(editingOrder?.id) {
        mutableStateOf(
            if (isLocalPurchase) "1.00"
            else editingOrder?.exchangeRateToBdt ?: (if (selectedCurrency == "USD") "120.00" else "17.00")
        )
    }
    var chinaLocalDeliveryStr by remember(editingOrder?.id) { mutableStateOf("0") }
    var handlingCostStr by remember(editingOrder?.id) { mutableStateOf("0") }
    var sampleCostStr by remember(editingOrder?.id) { mutableStateOf("0") }
    var forwarderAddressStr by remember(editingOrder?.id) { mutableStateOf(parsedNotes.forwarderAddress) }
    var attachmentsStr by remember(editingOrder?.id) { mutableStateOf(editingOrder?.attachments ?: "") }
    var selectedPaymentTerm by remember(editingOrder?.id) { mutableStateOf(parsedNotes.paymentTerm) }
    var notes by remember(editingOrder?.id) { mutableStateOf(parsedNotes.userNotes) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var hasHydratedCosts by remember(editingOrder?.id) { mutableStateOf(false) }
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
    var showInlineAddSupplierDialog by remember { mutableStateOf(false) }
    var newSupCompany by remember { mutableStateOf("") }
    var newSupContact by remember { mutableStateOf("") }
    var newSupPhone by remember { mutableStateOf("") }
    var newSupAddress by remember { mutableStateOf("Guangzhou, China") }
    var newSupCountry by remember { mutableStateOf("China") }
    var newSupCurrency by remember { mutableStateOf("CNY") }

    // Inline Add Product state
    var showInlineAddProductDialog by remember { mutableStateOf(false) }
    var newProdNameEn by remember { mutableStateOf("") }
    var newProdNameBn by remember { mutableStateOf("") }
    var newProdSku by remember { mutableStateOf("") }
    var newProdUnit by remember { mutableStateOf("Pair") }
    var newProdPurchasePriceStr by remember { mutableStateOf("45.00") }
    var newProdSellingPriceStr by remember { mutableStateOf("95.00") }
    var selectedCategoryId by remember(categories) { mutableStateOf(categories.firstOrNull()?.id ?: "cat-footwear") }

    // Inline Add Variant state
    var showInlineAddVariantForProductId by remember { mutableStateOf<String?>(null) }
    var targetItemIndexForVariant by remember { mutableStateOf<Int?>(null) }
    var newVarColor by remember { mutableStateOf("") }
    var newVarSize by remember { mutableStateOf("") }
    var newVarSku by remember { mutableStateOf("") }
    var newVarCostStr by remember { mutableStateOf("45.00") }
    var newVarPriceStr by remember { mutableStateOf("95.00") }

    // Inline Add Category state
    var showInlineAddCategoryDialog by remember { mutableStateOf(false) }
    var newCatNameEn by remember { mutableStateOf("") }
    var newCatNameBn by remember { mutableStateOf("") }

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
            val gwStr = firstCtn?.cartonGrossWeightKg?.ifBlank { firstCtn.grossWeightKg } ?: "15.0"
            CartonBoxState(
                cartonNumber = cNum,
                grossWeightKgStr = gwStr,
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

    var hasHydratedItems by remember(editingOrder?.id) { mutableStateOf(initialEditingItems.isNotEmpty()) }
    val itemsList = remember(editingOrder?.id) {
        mutableStateListOf<ItemDraftState>().apply {
            if (initialEditingItems.isNotEmpty()) {
                addAll(initialEditingItems.map { it.toItemDraftState() })
            } else if (!isEditing) {
                add(
                    ItemDraftState(
                        productId = firstProdId,
                        variantId = firstVarId,
                        quantityStr = "100",
                        unitPriceStr = "45.00",
                        cartonQuantityStr = "5",
                        pcsPerCartonStr = "20",
                        grossWeightKgStr = "35.0",
                        netWeightKgStr = "33.0"
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
    var hasHydratedCartons by remember(editingOrder?.id) { mutableStateOf(initialEditingCartons.isNotEmpty()) }
    val cartonBoxes = remember(editingOrder?.id) {
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
    val categoryRateStrings = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(categories) {
        categories.forEach { cat ->
            if (!categoryRateStrings.containsKey(cat.id)) {
                val defaultMajor = BigDecimal(cat.defaultFreightRatePerKgBdtMinor)
                    .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString()
                categoryRateStrings[cat.id] = defaultMajor
            }
        }
    }

    fun variantLabel(v: ProductVariant?): String {
        if (v == null) return if (isBangla) "ডিফল্ট / ভ্যারিয়েন্ট ছাড়া" else "Default / Base"
        val parts = listOfNotNull(
            v.color?.takeIf { it.isNotBlank() },
            v.size?.takeIf { it.isNotBlank() }
        )
        return if (parts.isNotEmpty()) "${parts.joinToString(" / ")} (${v.sku})" else v.sku
    }

    fun submitPo(targetStatus: PurchaseOrderStatus) {
        try {
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
            val lineWeightFromCartons = mutableMapOf<Pair<String, String?>, BigDecimal>()
            val lineCartonCountFromCartons = mutableMapOf<Pair<String, String?>, Int>()
            val lineQuantityFromCartons = mutableMapOf<Pair<String, String?>, Int>()

            if (cartonBoxes.isNotEmpty()) {
                cartonBoxes.forEach { box ->
                    val boxGw = box.grossWeightKgStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val manualLines = box.lines.filter {
                        (it.manualWeightKgStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO
                    }
                    val totalManualWeight = manualLines.fold(BigDecimal.ZERO) { acc, line ->
                        acc.add(line.manualWeightKgStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO)
                    }
                    val remainingWeight = (boxGw - totalManualWeight).coerceAtLeast(BigDecimal.ZERO)
                    val nonManualLines = box.lines.filter {
                        (it.manualWeightKgStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO) <= BigDecimal.ZERO
                    }
                    val totalNonManualPcs = nonManualLines.sumOf { it.quantityStr.trim().toIntOrNull() ?: 0 }

                    box.lines.forEach { line ->
                        val lQty = line.quantityStr.trim().toIntOrNull() ?: 0
                        if (lQty > 0 && line.productId.isNotBlank()) {
                            val manualWt = line.manualWeightKgStr.trim().toBigDecimalOrNull()
                            val shareGw = if (manualWt != null && manualWt > BigDecimal.ZERO) {
                                manualWt
                            } else if (totalNonManualPcs > 0 && remainingWeight > BigDecimal.ZERO) {
                                remainingWeight.multiply(BigDecimal(lQty))
                                    .divide(BigDecimal(totalNonManualPcs), 3, RoundingMode.HALF_UP)
                            } else {
                                BigDecimal.ZERO
                            }
                            val key = line.productId to line.variantId
                            lineWeightFromCartons[key] = (lineWeightFromCartons[key] ?: BigDecimal.ZERO).add(shareGw)
                            lineCartonCountFromCartons[key] = (lineCartonCountFromCartons[key] ?: 0) + 1
                            lineQuantityFromCartons[key] = (lineQuantityFromCartons[key] ?: 0) + lQty

                            // Auto-add to itemsList if not already present
                            if (itemsList.none { it.productId == line.productId && it.variantId == line.variantId }) {
                                val v = variants.find { it.id == line.variantId }
                                val p = products.find { it.id == line.productId }
                                val costMinor = v?.variantCostMinor?.takeIf { it > 0 } ?: p?.purchasePriceMinor ?: 0L
                                itemsList.add(
                                    ItemDraftState(
                                        productId = line.productId,
                                        variantId = line.variantId,
                                        quantityStr = lQty.toString(),
                                        unitPriceStr = if (costMinor > 0) "%.2f".format(costMinor / 100.0) else "45.00"
                                    )
                                )
                            }
                        }
                    }
                }
            }

            val drafts = itemsList.mapIndexed { idx, state ->
                require(state.productId.isNotBlank()) {
                    if (isBangla) "আইটেম #${idx + 1}: পণ্য নির্বাচন করুন।" else "Item #${idx + 1}: Please select a product."
                }
                val key = state.productId to state.variantId
                val cartonPackedQty = lineQuantityFromCartons[key]
                val qty = if (cartonPackedQty != null && cartonPackedQty > 0) cartonPackedQty else (state.quantityStr.trim().toIntOrNull() ?: 0)
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
                val netKg = state.netWeightKgStr.trim().toBigDecimalOrNull() ?: grossKg

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
                                quantity = q
                            )
                        } else null
                    }
                    if (validLines.isNotEmpty()) {
                        PoMixedCartonDraft(
                            cartonNumber = box.cartonNumber.trim().ifBlank { "CTN-%02d".format(idx + 1) },
                            grossWeightKg = gw,
                            netWeightKg = gw,
                            lines = validLines
                        )
                    } else null
                }
            } else {
                // Auto-create carton drafts from item rows so Import Shipment always receives the cartons & weights
                drafts.mapIndexedNotNull { idx, d ->
                    if (d.grossWeightKg > BigDecimal.ZERO || d.cartonQuantity > 0) {
                        val prod = products.find { it.id == d.productId }
                        val v = variants.find { it.id == d.variantId }
                        PoMixedCartonDraft(
                            cartonNumber = "CTN-%02d".format(idx + 1),
                            grossWeightKg = d.grossWeightKg,
                            netWeightKg = d.netWeightKg,
                            lines = listOf(
                                MixedCartonLineDraft(
                                    productId = d.productId,
                                    variantId = d.variantId,
                                    productName = listOfNotNull(prod?.nameEn, v?.color, v?.size).joinToString(" - "),
                                    quantity = d.quantity
                                )
                            )
                        )
                    } else null
                }
            }

            val chinaDelivBd = chinaLocalDeliveryStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
            val chinaDeliveryMinor = chinaDelivBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact().coerceAtLeast(0L)

            val catRatesMinorMap = mutableMapOf<String, Long>()
            categoryRateStrings.forEach { (catId, rateStr) ->
                val rBd = rateStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                if (rBd >= BigDecimal.ZERO) {
                    catRatesMinorMap[catId] = rBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                }
            }

            val handlingBd = handlingCostStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
            val handlingMinor = handlingBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact().coerceAtLeast(0L)

            val sampleBd = sampleCostStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
            val sampleMinor = sampleBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact().coerceAtLeast(0L)

            val termsNote = "[Terms: ${selectedPaymentTerm.labelEn}]"
            val forwarderNote = if (forwarderAddressStr.isNotBlank()) "[Forwarder: ${forwarderAddressStr.trim()}]" else ""
            val noteParts = listOfNotNull(termsNote, forwarderNote.ifBlank { null }, notes.trim().ifBlank { null })
            val combinedNotes = noteParts.joinToString(" • ")

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
                    attachmentsStr.ifBlank { null }
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
                    catRatesMinorMap
                )
            }
            onDismiss()
        } catch (e: Exception) {
            errorMessage = e.message ?: if (isBangla) "তথ্য যাচাই ব্যর্থ হয়েছে" else "Validation failed"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEditing) {
                                if (isBangla) "পারচেজ অর্ডার এডিট (${editingOrder?.poNumber})" else "Edit Purchase Order (${editingOrder?.poNumber})"
                            } else {
                                if (isLocalPurchase) {
                                    if (isBangla) "নতুন স্থানীয় পারচেজ অর্ডার (বাংলাদেশ)" else "New Local Purchase Order (Bangladesh)"
                                } else {
                                    if (isBangla) "নতুন আমদানি ক্রয়াদেশ (চীন) ও প্যাকিং" else "New Import Purchase Order (China)"
                                }
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                        Text(
                            text = if (isLocalPurchase) {
                                if (isBangla) "লোকাল সরবরাহকারী • ভ্যারিয়েন্ট • পরিবহন ও আনলোডিং • গুদামে সরাসরি স্টক ইন" else "Local Supplier • Variants • Transport & Handling • Direct Warehouse Stock In"
                            } else {
                                if (isBangla) "সরবরাহকারী • ভ্যারিয়েন্ট • মিক্সড কার্টন • চীন ডেলিভারি ও ওজনভিত্তিক ফ্রেইট অটো-সিঙ্ক" else "Supplier • Variants • Mixed Cartons • Auto-Syncs to Import Shipment"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Purchase Type Toggle (International Import vs Local Purchase)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isLocalPurchase) PrimaryPurple.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isLocalPurchase) PrimaryPurple else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isLocalPurchase = false
                                if (selectedCurrency == "BDT") {
                                    selectedCurrency = "CNY"
                                    exchangeRateStr = "17.00"
                                }
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🇨🇳 " + (if (isBangla) "আন্তর্জাতিক আমদানি (চীন)" else "International Import (China)"),
                                fontWeight = if (!isLocalPurchase) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isLocalPurchase) PrimaryPurple else TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isLocalPurchase) SecondaryTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isLocalPurchase) SecondaryTeal else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isLocalPurchase = true
                                selectedCurrency = "BDT"
                                exchangeRateStr = "1.00"
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🇧🇩 " + (if (isBangla) "স্থানীয় ক্রয় (বাংলাদেশ)" else "Local Purchase (Bangladesh)"),
                                fontWeight = if (isLocalPurchase) FontWeight.Bold else FontWeight.Normal,
                                color = if (isLocalPurchase) SecondaryTeal else TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                HorizontalDivider(color = ERPDivider, modifier = Modifier.padding(vertical = 10.dp))

                // Scrollable Body
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ==========================================
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
                                    text = if (isBangla) "১. সরবরাহকারী নির্বাচন বা তৈরি (Supplier):" else "1. Select or Create Supplier:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                if (onAddNewSupplier != null) {
                                    Button(
                                        onClick = { showInlineAddSupplierDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "New Supplier", tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isBangla) "+ নতুন সরবরাহকারী" else "+ New Supplier",
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
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = StatusWarning)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isBangla) "কোনো সরবরাহকারী নেই। উপরের '+ নতুন সরবরাহকারী' বাটনে চাপ দিয়ে নতুন সরবরাহকারী তৈরি করুন।" else "No suppliers found. Tap '+ New Supplier' above to create one.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    suppliers.forEach { sup ->
                                        val isSelected = sup.id == selectedSupplierId
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) PrimaryPurple.copy(alpha = 0.12f) else Color(0xFFF1F5F9))
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
                        if (isLocalPurchase) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
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
                                            color = DeepNavy
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
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
                                    text = if (isBangla) "২. মুদ্রা, বিনিময় হার ও চীন লোকাল ডেলিভারি চার্জ:" else "2. Currency, Exchange Rate & China Local Delivery:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
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
                                            .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
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
                                                    color = if (isSel) Color.White else TextPrimary
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
                                        modifier = Modifier.weight(1f),
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
                                                "চীন লোকাল ডেলিভারি চার্জ ($selectedCurrency) — অটো শিপমেন্টে যুক্ত হবে"
                                            else
                                                "China Local Delivery Charge ($selectedCurrency) — Auto-syncs to Shipment"
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
                                    text = if (isBangla) "৩. পণ্য, ভ্যারিয়েন্ট ও দর (Products & Variants):" else "3. Products, Variants & Prices:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy,
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
                                        Icon(Icons.Default.Add, contentDescription = "New Product", tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isBangla) "+ নতুন পণ্য" else "+ New Product",
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
                                    Icon(Icons.Default.Add, contentDescription = "Add Item", tint = PrimaryPurple, modifier = Modifier.size(16.dp))
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
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = StatusWarning)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isBangla) "কোনো পণ্য পাওয়া যায়নি। ওপরের '+ নতুন পণ্য' বোতামে ক্লিক করুন।" else "No products found. Click '+ New Product' above to create one.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                    } else if (itemsList.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(18.dp))
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
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
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
                                                imageVector = Icons.Default.Delete,
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
                                                .background(if (isProdSel) PrimaryPurple.copy(alpha = 0.15f) else Color.White)
                                                .border(
                                                    width = if (isProdSel) 1.dp else 0.5.dp,
                                                    color = if (isProdSel) PrimaryPurple else Color(0xFFCBD5E1),
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
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isBangla) "ভ্যারিয়েন্ট নির্বাচন করুন (${productVariants.size}):" else "Select Variant (${productVariants.size}):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DeepNavy
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
                                                                        cartonQuantityStr = "1",
                                                                        pcsPerCartonStr = itemState.quantityStr,
                                                                        grossWeightKgStr = "10.0",
                                                                        netWeightKgStr = "9.5"
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
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
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
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
                                                .background(if (isBaseSel) SecondaryTeal.copy(alpha = 0.15f) else Color.White)
                                                .border(
                                                    width = if (isBaseSel) 1.dp else 0.5.dp,
                                                    color = if (isBaseSel) SecondaryTeal else Color(0xFFCBD5E1),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { itemState.variantId = null }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = if (isBangla) "বেস পণ্য (ভ্যারিয়েন্ট ছাড়া)" else "Base Product",
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
                                                .background(if (isVarSel) SecondaryTeal.copy(alpha = 0.18f) else Color.White)
                                                .border(
                                                    width = if (isVarSel) 1.2.dp else 0.5.dp,
                                                    color = if (isVarSel) SecondaryTeal else Color(0xFFCBD5E1),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    itemState.variantId = pv.id
                                                    if (pv.variantCostMinor > 0) {
                                                        itemState.unitPriceStr = (pv.variantCostMinor / 100.0).toString()
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
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
                    // 4. Carton-by-Carton Packaging (Mixed Variants Supported)
                    // ==========================================
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
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
                                            color = DeepNavy
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
                                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isBangla) "+ কার্টন যোগ" else "+ Add Carton",
                                                color = Color.White,
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
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isBangla) "💡 একই কার্টনে একাধিক প্রোডাক্ট বা ভ্যারিয়েন্ট (মিক্সড কার্টন) প্যাকিং গাইড:" else "💡 Mixed Carton Packing Guide:",
                                                    fontWeight = FontWeight.Bold,
                                                    color = DeepNavy,
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
                                                color = Color(0xFF1E3A8A),
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
                                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                                shape = RoundedCornerShape(10.dp),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = if (isMixedCarton) 1.5.dp else 1.dp,
                                                    color = if (isMixedCarton) SecondaryTeal else Color(0xFFCBD5E1)
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
                                                                modifier = Modifier.size(26.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.ContentCopy,
                                                                    contentDescription = "Duplicate Carton",
                                                                    tint = SecondaryTeal,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = { cartonBoxes.removeAt(cIdx) },
                                                                modifier = Modifier.size(26.dp)
                                                            ) {
                                                                Icon(Icons.Default.Delete, contentDescription = "Remove Carton", tint = StatusError, modifier = Modifier.size(17.dp))
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        OutlinedTextField(
                                                            value = box.cartonNumber,
                                                            onValueChange = { box.cartonNumber = it },
                                                            label = { Text(if (isBangla) "কার্টন নম্বর" else "Carton No") },
                                                            modifier = Modifier.weight(1f),
                                                            singleLine = true
                                                        )
                                                        OutlinedTextField(
                                                            value = box.grossWeightKgStr,
                                                            onValueChange = { box.grossWeightKgStr = it },
                                                            label = { Text(if (isBangla) "কার্টনের মোট ওজন (KG)" else "Carton Gross Wt (KG)") },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            modifier = Modifier.weight(1f),
                                                            singleLine = true
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = if (isBangla) "এই কার্টনের ভেতরের পণ্য ও ভ্যারিয়েন্ট সমূহ:" else "Products / Variants inside this Carton:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextSecondary
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))

                                                    val manualLines = box.lines.filter {
                                                        (it.manualWeightKgStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO
                                                    }
                                                    val totalManualWeight = manualLines.fold(BigDecimal.ZERO) { acc, line ->
                                                        acc.add(line.manualWeightKgStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO)
                                                    }
                                                    val remainingWeight = (boxGw - totalManualWeight).coerceAtLeast(BigDecimal.ZERO)
                                                    val nonManualLines = box.lines.filter {
                                                        (it.manualWeightKgStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO) <= BigDecimal.ZERO
                                                    }
                                                    val totalNonManualPcs = nonManualLines.sumOf { it.quantityStr.trim().toIntOrNull() ?: 0 }

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
                                                        val allocatedKg = if (manualWt != null && manualWt > BigDecimal.ZERO) {
                                                            manualWt.setScale(2, RoundingMode.HALF_UP)
                                                        } else if (totalNonManualPcs > 0 && lineQty > 0 && remainingWeight > BigDecimal.ZERO) {
                                                            remainingWeight.multiply(BigDecimal(lineQty)).divide(BigDecimal(totalNonManualPcs), 2, RoundingMode.HALF_UP)
                                                        } else {
                                                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                                                        }
                                                        val perPcKg = if (lineQty > 0) allocatedKg.divide(BigDecimal(lineQty), 3, RoundingMode.HALF_UP) else BigDecimal.ZERO

                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 4.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(Color(0xFFF8FAFC))
                                                                .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
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
                                                                            .background(if (isSel) PrimaryPurple else Color.White)
                                                                            .border(1.dp, if (isSel) PrimaryPurple else Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
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
                                                                            color = if (isSel) Color.White else TextPrimary,
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
                                                                        Icon(Icons.Default.Close, contentDescription = "Remove Line", tint = StatusError, modifier = Modifier.size(16.dp))
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
                                                        Icon(Icons.Default.Add, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(16.dp))
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
                    // 5. Base Summary: Total Weight, Category Per-KG Rate & Total Freight Calculation
                    // ==========================================
                    item {
                        // Compute category weights & totals live
                        val categoryWeightMap = mutableMapOf<String, BigDecimal>()
                        if (cartonBoxes.isNotEmpty()) {
                            cartonBoxes.forEach { box ->
                                val boxGw = box.grossWeightKgStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                                val totalBoxPcs = box.lines.sumOf { it.quantityStr.trim().toIntOrNull() ?: 0 }
                                if (totalBoxPcs > 0 && boxGw > BigDecimal.ZERO) {
                                    box.lines.forEach { line ->
                                        val lQty = line.quantityStr.trim().toIntOrNull() ?: 0
                                        if (lQty > 0 && line.productId.isNotBlank()) {
                                            val prod = products.find { it.id == line.productId }
                                            val catId = prod?.categoryId ?: "uncategorized"
                                            val shareGw = boxGw.multiply(BigDecimal(lQty))
                                                .divide(BigDecimal(totalBoxPcs), 3, RoundingMode.HALF_UP)
                                            categoryWeightMap[catId] = (categoryWeightMap[catId] ?: BigDecimal.ZERO).add(shareGw)
                                        }
                                    }
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
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryPurple.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isBangla)
                                        "৫. আর্থিক সামারি ও ওয়েট-বেসড কস্টিং বরাদ্দ (Weight-Based Costing Allocation)"
                                    else
                                        "5. Financial Summary & Weight-Based Costing Allocation",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Top Summary Pills (Total Cartons, Total PCS, Total Weight KG)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(if (isBangla) "মোট কার্টন" else "Total Cartons", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        Text("$totalCartonsCount CTN", fontWeight = FontWeight.Bold, color = DeepNavy)
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
                                        color = DeepNavy
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
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isBangla) "পণ্যের মোট ক্রয়মূল্য:" else "Products Purchase Total:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    val prodTotalDisplay = if (isLocalPurchase) "৳ $totalProductBdt" else "$selectedCurrency $totalProductForeign (৳ $totalProductBdt)"
                                    Text(prodTotalDisplay, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                if (!isLocalPurchase) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isBangla) "চীন লোকাল ডেলিভারি চার্জ:" else "China Local Delivery:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        Text("$selectedCurrency $chinaDelivForeign (৳ $chinaDelivBdt)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isBangla) "আন্তর্জাতিক ফ্রেইট ($totalGrossWeightKg KG):" else "International Freight ($totalGrossWeightKg KG):", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        Text("৳ $totalCategoryFreightBdt", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SecondaryTeal)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isBangla) "হ্যান্ডলিং ও আনলোডিং খরচ (ক্যাপিটালাইজড):" else "Handling & Porter (Capitalized):", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Text("৳ $handlingCostBdt", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = DeepNavy)
                                }
                                if (sampleCostBdt > BigDecimal.ZERO) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isBangla) "স্যাম্পল প্রসেসিং খরচ (ক্যাপিটালাইজড):" else "Sample Processing (Capitalized):", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        Text("৳ $sampleCostBdt", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = DeepNavy)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = ERPDivider)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isBangla) "সর্বমোট প্রকৃত ল্যান্ডিং কস্ট:" else "Total Landed Cost:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = DeepNavy)
                                    Text("৳ $grandTotalEstimatedBdt", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                                }
                                Text(
                                    text = if (isBangla)
                                        "গড় প্রতি পিস ল্যান্ডিং কস্ট: ৳ $avgUnitLandedBdt / PCS"
                                    else
                                        "Avg Unit Landed Cost: ৳ $avgUnitLandedBdt / PCS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SecondaryTeal,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                // Warehouse Rent OPEX Note
                                Surface(
                                    color = Color(0xFFFFFBEB),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isBangla)
                                            "ℹ️ গুদাম ভাড়া (Warehouse Rent) একটি মাসিক পরিচালন ব্যয় (OPEX) হিসেবে বিবেচিত হয় এবং পণ্যের ইউনিট খরচে যুক্ত না হয়ে নেট লাভে সমন্বয় হবে।"
                                        else
                                            "ℹ️ Warehouse Rent is treated as an Operational Expense (OPEX) and deducted from Gross Profit, NOT capitalized into unit cost.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF92400E),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 6. Delivery Address & Attachments
                    if (!isLocalPurchase) {
                        item {
                            OutlinedTextField(
                                value = forwarderAddressStr,
                                onValueChange = { forwarderAddressStr = it },
                                label = { Text(if (isBangla) "ফরোয়ার্ডার চীন গুদাম ঠিকানা ও রিসিভার ফোন" else "Forwarder China Warehouse Address & Receiver Phone") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2
                            )
                        }
                    } else {
                        item {
                            OutlinedTextField(
                                value = forwarderAddressStr,
                                onValueChange = { forwarderAddressStr = it },
                                label = { Text(if (isBangla) "গুদাম / ডেলিভারি ঠিকানা (বাংলাদেশ)" else "Warehouse / Delivery Address (Bangladesh)") },
                                placeholder = { Text(if (isBangla) "যেমন: প্রধান গুদাম, তেজগাঁও, ঢাকা" else "e.g., Main Warehouse, Tejgaon, Dhaka") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = attachmentsStr,
                            onValueChange = { attachmentsStr = it },
                            label = { Text(if (isBangla) "ইনভয়েস / প্যাকিং লিস্ট ডকুমেন্টের লিংক বা ফাইল নাম" else "Invoice / Packing List Attachment Link or Filename") },
                            supportingText = { Text(if (isBangla) "কমা দিয়ে একাধিক ফাইল যুক্ত করতে পারেন" else "Comma-separated files or links", fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // 7. Order Notes
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(if (isBangla) "অর্ডার নোট / বিবরণ (মন্তব্য)" else "Order Notes / Remarks") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }

                    // 8. Payment Terms & Schedule Calculation
                    item {
                        Column {
                            Text(
                                text = if (isBangla) "পেমেন্ট শর্ত ও সময়সূচি (Payment Terms):" else "Payment Terms & Schedule:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy
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
                                            .background(if (isSel) PrimaryPurple else Color(0xFFF1F5F9))
                                            .clickable { selectedPaymentTerm = term }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isBangla) term.labelBn else term.labelEn,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) Color.White else TextPrimary
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
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
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

                    // Error Message Banner
                    if (errorMessage != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = StatusError)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage!!,
                                        color = StatusError,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = ERPDivider, modifier = Modifier.padding(vertical = 10.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isBangla) "বাতিল" else "Cancel",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Save Draft Button
                        OutlinedButton(
                            onClick = { submitPo(PurchaseOrderStatus.DRAFT) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPurple),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "ড্রাফট রাখুন" else "Save Draft",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }

                        // Confirm PO Button
                        Button(
                            onClick = { submitPo(PurchaseOrderStatus.CONFIRMED) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEditing) {
                                    if (isBangla) "PO আপডেট করুন" else "Update PO"
                                } else {
                                    if (isBangla) "PO নিশ্চিত করুন" else "Confirm PO"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
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
                    color = DeepNavy
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
                    color = DeepNavy
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
