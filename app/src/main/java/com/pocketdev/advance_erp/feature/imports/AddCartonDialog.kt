package com.pocketdev.advance_erp.feature.imports

import org.json.JSONObject
import org.json.JSONArray
import com.pocketdev.advance_erp.core.designsystem.ERPLeaveEditorDialog
import com.pocketdev.advance_erp.core.util.EditorDraftStore
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.saveable.Saver
import com.pocketdev.advance_erp.core.designsystem.ERPFullScreenEditor
import androidx.compose.runtime.saveable.rememberSaveable
import com.pocketdev.advance_erp.core.designsystem.ERPSurfaceVariant
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ViewInAr
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
import androidx.compose.runtime.mutableStateListOf
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
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.SecondaryTealLight
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.engine.CartonWeightAllocationEngine
import com.pocketdev.advance_erp.core.engine.CbmCalculator
import com.pocketdev.advance_erp.core.engine.MixedCartonLineInput
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.domain.repository.MixedCartonLineDraft
import com.pocketdev.advance_erp.domain.repository.ShipmentCartonDraft
import java.math.BigDecimal
import java.math.RoundingMode

private class MixedLineDraftState(productId: String, quantityStr: String = "20") {
    var productId by mutableStateOf(productId)
    var variantId by mutableStateOf<String?>(null)
    var quantityStr by mutableStateOf(quantityStr)
}

private val MixedLinesSaver = Saver<SnapshotStateList<MixedLineDraftState>, String>(
    save = { JSONArray().apply { it.forEach { line -> put(JSONObject().apply {
        put("product", line.productId); put("variant", line.variantId ?: ""); put("quantity", line.quantityStr)
    }) } }.toString() },
    restore = { raw -> JSONArray(raw).let { a -> mutableStateListOf<MixedLineDraftState>().apply {
        repeat(a.length()) { i -> val j = a.getJSONObject(i)
            add(MixedLineDraftState(j.getString("product"), j.getString("quantity")).also { it.variantId = j.optString("variant").ifBlank { null } })
        }
    } } }
)

@Composable
fun AddCartonDialog(
    shipment: ImportShipment? = null,
    orderId: String? = null,
    packedCartonCount: Int = shipment?.totalCartons ?: 0,
    products: List<Product>,
    variants: List<com.pocketdev.advance_erp.data.entity.ProductVariant> = emptyList(),
    language: AppLanguage = AppLanguage.BANGLA,
    onDismiss: () -> Unit,
    isSaving: Boolean = false,
    saveError: String? = null,
    onConfirm: (carton: ShipmentCartonDraft) -> Unit,
    onConfirmMixed: ((cartonNumber: String, length: BigDecimal, width: BigDecimal, height: BigDecimal, grossWeight: BigDecimal, netWeight: BigDecimal, lines: List<MixedCartonLineDraft>) -> Unit)? = null
) {
    val isBangla = language == AppLanguage.BANGLA
    var isMixedTab by rememberSaveable { mutableStateOf(false) }

    var cartonNumber by rememberSaveable { mutableStateOf("CTN-%03d".format(packedCartonCount + 1)) }
    var singleProductId by rememberSaveable { mutableStateOf(products.firstOrNull()?.id ?: "") }
    var singleVariantId by rememberSaveable { mutableStateOf<String?>(null) }
    var singleQuantityStr by rememberSaveable { mutableStateOf("50") }

    var lengthStr by rememberSaveable { mutableStateOf("50") }
    var widthStr by rememberSaveable { mutableStateOf("40") }
    var heightStr by rememberSaveable { mutableStateOf("30") }
    var grossWeightStr by rememberSaveable { mutableStateOf("15.0") }
    var netWeightStr by rememberSaveable { mutableStateOf("14.0") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Multi-product lines for Mixed Carton
    val mixedLines = rememberSaveable(saver = MixedLinesSaver) {
        val list = mutableStateListOf<MixedLineDraftState>()
        if (products.isNotEmpty()) {
            list.add(MixedLineDraftState(products[0].id, "20"))
            if (products.size > 1) {
                list.add(MixedLineDraftState(products[1].id, "10"))
            }
        }
        list
    }

    val draftContext = LocalContext.current
    val draftStore = remember(draftContext) { EditorDraftStore(draftContext) }
    val draftKey = "packing:" + (orderId ?: shipment?.id ?: "new")
    var draftLoaded by rememberSaveable(draftKey) { mutableStateOf(false) }
    val draftScope = rememberCoroutineScope()
    var savingBuffer by remember { mutableStateOf(false) }
    var showLeavePrompt by rememberSaveable { mutableStateOf(false) }
    var initialSnapshot by rememberSaveable(draftKey) { mutableStateOf<String?>(null) }
    fun snapshotEditor() = JSONObject().apply {
        put("cartonNumber", cartonNumber)
        put("singleProductId", singleProductId)
        put("singleQuantityStr", singleQuantityStr)
        put("lengthStr", lengthStr)
        put("widthStr", widthStr)
        put("heightStr", heightStr)
        put("grossWeightStr", grossWeightStr)
        put("netWeightStr", netWeightStr)

        put("mixed", isMixedTab); put("variant", singleVariantId ?: "")
        put("lines", JSONArray().apply { mixedLines.forEach { line -> put(JSONObject().apply {
            put("product", line.productId); put("variant", line.variantId ?: ""); put("quantity", line.quantityStr)
        }) } })
    }
    LaunchedEffect(draftKey) {
        if (!draftLoaded) {
            draftStore.read(draftKey)?.let { draft ->
                cartonNumber = draft.optString("cartonNumber", cartonNumber)
                singleProductId = draft.optString("singleProductId", singleProductId)
                singleQuantityStr = draft.optString("singleQuantityStr", singleQuantityStr)
                lengthStr = draft.optString("lengthStr", lengthStr)
                widthStr = draft.optString("widthStr", widthStr)
                heightStr = draft.optString("heightStr", heightStr)
                grossWeightStr = draft.optString("grossWeightStr", grossWeightStr)
                netWeightStr = draft.optString("netWeightStr", netWeightStr)

                isMixedTab = draft.optBoolean("mixed"); singleVariantId = draft.optString("variant").ifBlank { null }
                draft.optJSONArray("lines")?.let { a -> mixedLines.clear(); repeat(a.length()) { i ->
                    val j = a.getJSONObject(i)
                    mixedLines.add(MixedLineDraftState(j.getString("product"), j.getString("quantity")).also {
                        it.variantId = j.optString("variant").ifBlank { null }
                    })
                } }
            }
            initialSnapshot = snapshotEditor().toString(); draftLoaded = true
        }
    }
    val requestClose: () -> Unit = {
        if (!isSaving && !savingBuffer) { if (snapshotEditor().toString() == initialSnapshot) onDismiss() else showLeavePrompt = true }
    }
    if (showLeavePrompt) ERPLeaveEditorDialog(
        isBangla = isBangla,
        isSaving = savingBuffer,
        onContinue = { showLeavePrompt = false },
        onDiscard = { draftStore.remove(draftKey); onDismiss() },
        onSaveDraft = {
            val snapshot = snapshotEditor()
            savingBuffer = true
            draftScope.launch {
                try {
                    withContext(Dispatchers.IO) { draftStore.save(draftKey, snapshot) }
                    onDismiss()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    errorMessage = e.message
                    showLeavePrompt = false
                } finally {
                    savingBuffer = false
                }
            }
        }
    )

    val calculatedCbm = try {
        val l = BigDecimal(lengthStr.ifBlank { "0" })
        val w = BigDecimal(widthStr.ifBlank { "0" })
        val h = BigDecimal(heightStr.ifBlank { "0" })
        if (l > BigDecimal.ZERO && w > BigDecimal.ZERO && h > BigDecimal.ZERO) {
            CbmCalculator.calculateCbm(l, w, h, 1).toPlainString()
        } else "0.000000"
    } catch (e: Exception) {
        "0.000000"
    }

    val productsMap = remember(products) { products.associateBy { it.id } }

    val totalMixedQty = mixedLines.sumOf { it.quantityStr.trim().toIntOrNull() ?: 0 }
    val grossWeightBd = grossWeightStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val netWeightBd = netWeightStr.toBigDecimalOrNull() ?: BigDecimal.ZERO

    val allocatedPreview = remember(mixedLines.map { Triple(it.productId, it.variantId, it.quantityStr) }, grossWeightStr, netWeightStr) {
        try {
            if (totalMixedQty > 0 && grossWeightBd > BigDecimal.ZERO) {
                val inputs = mixedLines.map {
                    val p = productsMap[it.productId]
                    val name = if (isBangla && !p?.nameBn.isNullOrBlank()) p?.nameBn!! else p?.nameEn ?: it.productId
                    MixedCartonLineInput(
                        productId = it.productId,
                        variantId = it.variantId,
                        productName = name,
                        quantity = it.quantityStr.trim().toIntOrNull() ?: 0
                    )
                }
                CartonWeightAllocationEngine.allocateWeights(grossWeightBd, netWeightBd, inputs)
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
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
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ERPBackground)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepNavy)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ViewInAr, contentDescription = null, tint = SecondaryTealLight, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "কার্টন ও প্যাকিং যুক্ত করুন" else "Add Carton & Packing",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = if (isBangla) "একক পণ্য অথবা মিশ্র কার্টন (স্বয়ংক্রিয় ওজন বণ্টন)" else "Single or Mixed Product Carton (Auto-allocated Weight)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = requestClose, enabled = !isSaving) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Tab Switcher: Single vs Mixed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isMixedTab) PrimaryPurple else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (!isMixedTab) PrimaryPurple else Color.LightGray, RoundedCornerShape(10.dp))
                            .clickable { isMixedTab = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBangla) "A. একক পণ্য কার্টন" else "A. Single Product Carton",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (!isMixedTab) MaterialTheme.colorScheme.onPrimary else TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isMixedTab) PrimaryPurple else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (isMixedTab) PrimaryPurple else Color.LightGray, RoundedCornerShape(10.dp))
                            .clickable(enabled = onConfirmMixed != null) { isMixedTab = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBangla) "B. মিশ্র কার্টন (Mixed)" else "B. Mixed Product Carton",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isMixedTab) MaterialTheme.colorScheme.onPrimary else TextPrimary
                        )
                    }
                }

                if (errorMessage != null || saveError != null) {
                    Text(
                        text = saveError ?: errorMessage.orEmpty(),
                        color = StatusError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Carton Number
                    item {
                        OutlinedTextField(
                            value = cartonNumber,
                            onValueChange = { cartonNumber = it },
                            label = { Text(if (isBangla) "কার্টন নম্বর" else "Carton Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Dimensions & Gross / Net Weight
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (isBangla) "কার্টনের মাপ ও মোট ওজন" else "Dimensions & Weights",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = lengthStr,
                                        onValueChange = { lengthStr = it },
                                        label = { Text("L (cm)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = widthStr,
                                        onValueChange = { widthStr = it },
                                        label = { Text("W (cm)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = heightStr,
                                        onValueChange = { heightStr = it },
                                        label = { Text("H (cm)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                                Text(
                                    text = "Auto CBM: $calculatedCbm m³",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryTeal,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = grossWeightStr,
                                        onValueChange = { grossWeightStr = it },
                                        label = { Text("Gross Wt (KG)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = netWeightStr,
                                        onValueChange = { netWeightStr = it },
                                        label = { Text("Net Wt (KG)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    // Mode A: Single Product
                    if (!isMixedTab) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (isBangla) "পণ্য নির্বাচন করুন:" else "Select Product:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    products.forEach { prod ->
                                        val isSelected = prod.id == singleProductId
                                        val name = if (isBangla && !prod.nameBn.isNullOrBlank()) prod.nameBn else prod.nameEn
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) PrimaryPurple.copy(alpha = 0.15f) else ERPSurfaceVariant)
                                                .border(1.dp, if (isSelected) PrimaryPurple else ERPDivider, RoundedCornerShape(8.dp))
                                                .clickable { singleProductId = prod.id; singleVariantId = null }
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) PrimaryPurple else TextPrimary
                                            )
                                        }
                                    }

                                    PackingVariantSelector(singleProductId, singleVariantId, variants, isBangla) { singleVariantId = it }
                                    OutlinedTextField(
                                        value = singleQuantityStr,
                                        onValueChange = { singleQuantityStr = it },
                                        label = { Text(if (isBangla) "পরিমাণ (পিস/জোড়া)" else "Carton Quantity (PCS/Pair)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    } else {
                        // Mode B: Mixed Carton Lines
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBangla) "কার্টনের পণ্যসমূহ ও স্বয়ংক্রিয় ওজন বণ্টন:" else "Carton Items & Automatic Weight Allocation:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                TextButton(
                                    onClick = {
                                        if (products.isNotEmpty()) {
                                            mixedLines.add(MixedLineDraftState(products.first().id, "10"))
                                        }
                                    }
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = if (isBangla) "পণ্য যোগ" else "Add Line")
                                }
                            }
                        }

                        itemsIndexed(mixedLines) { index, line ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Item #${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryPurple
                                        )
                                        if (mixedLines.size > 1) {
                                            IconButton(onClick = { mixedLines.removeAt(index) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = StatusError, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }

                                    // Product selection
                                    LazyColumn(modifier = Modifier.height(80.dp)) {
                                        itemsIndexed(products) { _, p ->
                                            val isSel = p.id == line.productId
                                            val name = if (isBangla && !p.nameBn.isNullOrBlank()) p.nameBn else p.nameEn
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isSel) PrimaryPurple.copy(alpha = 0.15f) else ERPSurfaceVariant)
                                                    .clickable { line.productId = p.id; line.variantId = null }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSel) PrimaryPurple else TextPrimary
                                                )
                                            }
                                        }
                                    }

                                    PackingVariantSelector(line.productId, line.variantId, variants, isBangla) { line.variantId = it }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = line.quantityStr,
                                            onValueChange = { line.quantityStr = it },
                                            label = { Text("Quantity", fontSize = 11.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )

                                        val alloc = allocatedPreview.getOrNull(index)
                                        Column(modifier = Modifier.weight(1.2f)) {
                                            Text(
                                                text = "Allocated Weight:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = "${alloc?.allocatedGrossWeightKg?.toPlainString() ?: "0.000"} KG",
                                                style = MaterialTheme.typography.bodyMedium,
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

                // Bottom Action Button
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = requestClose,
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = if (isBangla) "বাতিল" else "Cancel")
                        }

                        Button(
                            enabled = !isSaving,
                            onClick = {
                                try {
                                    val l = BigDecimal(lengthStr.trim().ifEmpty { "50" })
                                    val w = BigDecimal(widthStr.trim().ifEmpty { "40" })
                                    val h = BigDecimal(heightStr.trim().ifEmpty { "30" })
                                    val gw = BigDecimal(grossWeightStr.trim().ifEmpty { "15" })
                                    val nw = BigDecimal(netWeightStr.trim().ifEmpty { "14" })

                                    require(cartonNumber.isNotBlank()) { "Carton number is required" }
                                    require(gw > BigDecimal.ZERO) { "Gross weight must be greater than zero" }

                                    if (!isMixedTab) {
                                        val qty = singleQuantityStr.trim().toIntOrNull() ?: 0
                                        require(qty > 0) { "Quantity must be greater than zero" }
                                        onConfirm(
                                            ShipmentCartonDraft(
                                                cartonNumber = cartonNumber,
                                                productId = singleProductId,
                                                variantId = singleVariantId,
                                                quantity = qty,
                                                lengthCm = l,
                                                widthCm = w,
                                                heightCm = h,
                                                grossWeightKg = gw,
                                                netWeightKg = nw
                                            )
                                        )
                                    } else {
                                        require(mixedLines.isNotEmpty()) { "At least one product line required" }
                                        val lineDrafts = mixedLines.map {
                                            val q = it.quantityStr.trim().toIntOrNull() ?: 0
                                            require(q > 0) { "Quantity for each product must be positive" }
                                            val p = productsMap[it.productId]
                                            MixedCartonLineDraft(
                                                productId = it.productId,
                                                variantId = it.variantId,
                                                productName = p?.nameEn ?: it.productId,
                                                quantity = q
                                            )
                                        }
                                        val saveMixed = requireNotNull(onConfirmMixed) {
                                            if (isBangla) "মিশ্র কার্টন ক্রয় অর্ডার থেকে সংরক্ষণ করুন।"
                                            else "Save mixed cartons from the purchase order."
                                        }
                                        saveMixed(cartonNumber, l, w, h, gw, nw, lineDrafts)
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Validation error"
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Text(when {
                                isSaving && isBangla -> "সংরক্ষণ হচ্ছে…"
                                isSaving -> "Saving…"
                                isBangla -> "কার্টন সংরক্ষণ করুন"
                                else -> "Save Carton"
                            })
                        }
                    }
                }
            }
        }
    }
}
