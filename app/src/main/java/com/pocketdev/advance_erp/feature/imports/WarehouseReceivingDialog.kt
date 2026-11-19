package com.pocketdev.advance_erp.feature.imports

import com.pocketdev.advance_erp.core.designsystem.StatusSuccessContainer
import com.pocketdev.advance_erp.core.designsystem.StatusWarningContainer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.Warning
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
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.domain.repository.ReceiptItemDraft

private class ReceivingLineState(
    val cartonId: String?,
    val cartonNumber: String,
    val productId: String,
    val variantId: String? = null,
    val productName: String,
    val expectedQty: Int,
    receivedQtyStr: String,
    goodQtyStr: String,
    damagedQtyStr: String = "0",
    missingQtyStr: String = "0",
    pendingQtyStr: String = "0",
    conditionNotes: String = "Good Condition"
) {
    var receivedQtyStr by mutableStateOf(receivedQtyStr)
    var goodQtyStr by mutableStateOf(goodQtyStr)
    var damagedQtyStr by mutableStateOf(damagedQtyStr)
    var missingQtyStr by mutableStateOf(missingQtyStr)
    var pendingQtyStr by mutableStateOf(pendingQtyStr)
    var conditionNotes by mutableStateOf(conditionNotes)
}

private fun ReceivingLineState.toJson() = JSONObject().apply {
    put("cartonId", cartonId ?: ""); put("cartonNumber", cartonNumber); put("productId", productId)
    put("variantId", variantId ?: ""); put("productName", productName); put("expectedQty", expectedQty)
    put("receivedQtyStr", receivedQtyStr)
    put("goodQtyStr", goodQtyStr)
    put("damagedQtyStr", damagedQtyStr)
    put("missingQtyStr", missingQtyStr)
    put("pendingQtyStr", pendingQtyStr)
    put("conditionNotes", conditionNotes)
}
private fun receivingLineFromJson(j: JSONObject) = ReceivingLineState(
    j.getString("cartonId").ifBlank { null }, j.getString("cartonNumber"), j.getString("productId"),
    j.getString("variantId").ifBlank { null }, j.getString("productName"), j.getInt("expectedQty"),
j.getString("receivedQtyStr"), j.getString("goodQtyStr"), j.getString("damagedQtyStr"), j.getString("missingQtyStr"), j.getString("pendingQtyStr"), j.getString("conditionNotes"))
private val ReceivingLinesSaver = Saver<SnapshotStateList<ReceivingLineState>, String>(
    save = { JSONArray().apply { it.forEach { line -> put(line.toJson()) } }.toString() },
    restore = { raw -> JSONArray(raw).let { a -> mutableStateListOf<ReceivingLineState>().apply {
        repeat(a.length()) { add(receivingLineFromJson(a.getJSONObject(it))) }
    } } }
)

@Composable
fun WarehouseReceivingDialog(
    shipment: ImportShipment? = null,
    order: PurchaseOrder? = null,
    poItems: List<PurchaseOrderItem> = emptyList(),
    cartons: List<ShipmentCarton> = emptyList(),
    products: List<Product> = emptyList(),
    isCostFinalized: Boolean = order?.isActualCostFinalized ?: false,
    isBangla: Boolean = false,
    isSaving: Boolean = false,
    saveError: String? = null,
    onDismiss: () -> Unit,
    onSaveDraft: ((warehouseId: String, receivingMethod: String, items: List<ReceiptItemDraft>, notes: String?) -> Unit)? = null,
    onConfirmPost: ((warehouseId: String, receivingMethod: String, items: List<ReceiptItemDraft>, notes: String?, allowDiscrepancy: Boolean) -> Unit)? = null
) {
    var selectedMethod by rememberSaveable { mutableStateOf(if (cartons.isNotEmpty()) "CARTON_WISE" else "PHYSICAL_COUNT") }
    var destinationWarehouseId by rememberSaveable { mutableStateOf("wh-dhk-01") }
    var notes by rememberSaveable { mutableStateOf("Warehouse receiving inspection completed.") }
    var showBlockedCostDialog by rememberSaveable { mutableStateOf(false) }
    var showConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val productsMap = remember(products) { products.associateBy { it.id } }

    fun defaultLines(method: String): SnapshotStateList<ReceivingLineState> {
        val list = mutableStateListOf<ReceivingLineState>()
        if (method == "CARTON_WISE") {
            if (cartons.isNotEmpty()) {
                cartons.forEach { c ->
                    val prod = productsMap[c.productId]
                    val name = if (isBangla && !prod?.nameBn.isNullOrBlank()) prod?.nameBn!! else prod?.nameEn ?: c.productId
                    list.add(
                        ReceivingLineState(
                            cartonId = c.id,
                            cartonNumber = c.cartonNumber,
                            productId = c.productId,
                            variantId = c.variantId,
                            productName = name,
                            expectedQty = c.quantity,
                            receivedQtyStr = c.quantity.toString(),
                            goodQtyStr = c.quantity.toString(),
                            damagedQtyStr = "0",
                            missingQtyStr = "0"
                        )
                    )
                }
            } else {
                // Synthesize cartons from PO item packaging without exceeding recorded physical order.totalCartons
                val maxAllowedCartons = order?.totalCartons?.takeIf { it > 0 } ?: Int.MAX_VALUE
                var cartonCounter = 1
                poItems.forEach { item ->
                    val prod = productsMap[item.productId]
                    val name = if (isBangla && !prod?.nameBn.isNullOrBlank()) prod?.nameBn!! else prod?.nameEn ?: item.productId
                    val ctnCount = if (item.cartonQuantity > 0) item.cartonQuantity else 1
                    val pcsPerCtn = if (item.pcsPerCarton > 0) item.pcsPerCarton else (item.quantity / ctnCount).coerceAtLeast(1)
                    var remQty = item.quantity

                    for (i in 1..ctnCount) {
                        val thisCtnQty = if (i == ctnCount) remQty else pcsPerCtn.coerceAtMost(remQty)
                        if (thisCtnQty > 0) {
                            val assignedCartonNum = if (cartonCounter <= maxAllowedCartons) {
                                cartonCounter++
                            } else {
                                maxAllowedCartons
                            }
                            list.add(
                                ReceivingLineState(
                                    cartonId = null,
                                    cartonNumber = "CTN-%02d".format(assignedCartonNum),
                                    productId = item.productId,
                                    variantId = item.variantId,
                                    productName = name,
                                    expectedQty = thisCtnQty,
                                    receivedQtyStr = thisCtnQty.toString(),
                                    goodQtyStr = thisCtnQty.toString(),
                                    damagedQtyStr = "0",
                                    missingQtyStr = "0"
                                )
                            )
                            remQty -= thisCtnQty
                        }
                    }
                }
            }
        } else {
            // Physical Count method: lines per purchase order item
            poItems.forEach { item ->
                val prod = productsMap[item.productId]
                val name = if (isBangla && !prod?.nameBn.isNullOrBlank()) prod?.nameBn!! else prod?.nameEn ?: item.productId
                val remaining = item.quantity - item.receivedQuantity
                if (remaining <= 0) return@forEach
                list.add(
                    ReceivingLineState(
                        cartonId = null,
                        cartonNumber = "SUMMARY",
                        productId = item.productId,
                        variantId = item.variantId,
                        productName = name,
                        expectedQty = remaining,
                        receivedQtyStr = remaining.toString(),
                        goodQtyStr = remaining.toString(),
                        damagedQtyStr = "0",
                        missingQtyStr = "0"
                    )
                )
            }
        }
        return list
    }

    val expectedPhysicalLines = remember(poItems, productsMap, isBangla) { defaultLines("PHYSICAL_COUNT") }
    val expectedCartonLines = remember(cartons, poItems, order?.totalCartons, productsMap, isBangla) {
        defaultLines("CARTON_WISE")
    }
    val physicalLines = rememberSaveable(saver = ReceivingLinesSaver) {
        mutableStateListOf<ReceivingLineState>().apply { addAll(defaultLines("PHYSICAL_COUNT")) }
    }
    val cartonLines = rememberSaveable(saver = ReceivingLinesSaver) {
        mutableStateListOf<ReceivingLineState>().apply { addAll(defaultLines("CARTON_WISE")) }
    }
    val lines = if (selectedMethod == "CARTON_WISE") cartonLines else physicalLines
    val expectedLines = if (selectedMethod == "CARTON_WISE") expectedCartonLines else expectedPhysicalLines

    val draftContext = LocalContext.current
    val draftStore = remember(draftContext) { EditorDraftStore(draftContext) }
    val draftKey = "receiving:" + (order?.id ?: shipment?.id ?: "new")
    val draftScope = rememberCoroutineScope()
    var savingBuffer by remember { mutableStateOf(false) }
    var showLeavePrompt by rememberSaveable { mutableStateOf(false) }
    var draftLoaded by rememberSaveable(draftKey) { mutableStateOf(false) }
    var initialSnapshot by rememberSaveable(draftKey) { mutableStateOf<String?>(null) }
    fun List<ReceivingLineState>.toJsonArray() = JSONArray().apply {
        forEach { put(it.toJson()) }
    }
    fun snapshotEditor() = JSONObject().apply {
        put("method", selectedMethod)
        put("warehouse", destinationWarehouseId)
        put("notes", notes)
        put("physicalLines", physicalLines.toJsonArray())
        put("cartonLines", cartonLines.toJsonArray())
    }
    LaunchedEffect(draftKey) {
        if (!draftLoaded) {
            draftStore.read(draftKey)?.let { draft ->
                selectedMethod = draft.optString("method", selectedMethod)
                destinationWarehouseId = draft.optString("warehouse", destinationWarehouseId)
                notes = draft.optString("notes")
                val legacyLines = draft.optJSONArray("lines")
                val savedPhysical = draft.optJSONArray("physicalLines")
                    ?: legacyLines.takeIf { selectedMethod == "PHYSICAL_COUNT" }
                val savedCartons = draft.optJSONArray("cartonLines")
                    ?: legacyLines.takeIf { selectedMethod == "CARTON_WISE" }
                savedPhysical?.let { saved ->
                    physicalLines.clear()
                    repeat(saved.length()) { physicalLines.add(receivingLineFromJson(saved.getJSONObject(it))) }
                }
                savedCartons?.let { saved ->
                    cartonLines.clear()
                    repeat(saved.length()) { cartonLines.add(receivingLineFromJson(saved.getJSONObject(it))) }
                }
            }
            initialSnapshot = snapshotEditor().toString()
            draftLoaded = true
        }
    }
    fun ReceivingLineState.expectedIdentity() =
        listOf(cartonId, cartonNumber, productId, variantId, expectedQty)
    val hasStaleLines = lines.map { it.expectedIdentity() } != expectedLines.map { it.expectedIdentity() }
    val requestClose: () -> Unit = {
        if (!isSaving && !savingBuffer) {
            if (snapshotEditor().toString() == initialSnapshot && !hasStaleLines) onDismiss()
            else showLeavePrompt = true
        }
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

    fun validateAndBuildDrafts(): List<ReceiptItemDraft>? {
        errorMessage = null
        if (lines.isEmpty()) { errorMessage = if (isBangla) "গ্রহণের জন্য কোনো পণ্য নেই" else "No items to receive"; return null }
        if (hasStaleLines) {
            errorMessage = if (isBangla) {
                "প্রত্যাশিত পরিমাণ বদলেছে। বর্তমান পরিমাণ লোড করতে খসড়াটি বাদ দিয়ে রিসিভিং আবার খুলুন।"
            } else {
                "Expected quantities changed. Discard this draft and reopen receiving to load the current quantities."
            }
            return null
        }
        val drafts = mutableListOf<ReceiptItemDraft>()
        for ((idx, line) in lines.withIndex()) {
            val good = line.goodQtyStr.trim().toIntOrNull() ?: -1
            val damaged = line.damagedQtyStr.trim().toIntOrNull() ?: -1
            val missing = line.missingQtyStr.trim().toIntOrNull() ?: -1
            val pending = line.pendingQtyStr.trim().toIntOrNull() ?: -1
            val received = good + damaged

            if (good < 0 || damaged < 0 || missing < 0 || pending < 0) {
                errorMessage = "Row #${idx + 1} (${line.productName}): Quantities cannot be negative."
                return null
            }
            if (good.toLong() + damaged + missing + pending != line.expectedQty.toLong()) {
                errorMessage = "Row #${idx + 1} (${line.productName}): Packed (${line.expectedQty}) must equal Good ($good) + Damaged ($damaged) + Missing ($missing) + Pending ($pending)."
                return null
            }

            drafts.add(
                ReceiptItemDraft(
                    cartonId = line.cartonId,
                    productId = line.productId,
                    variantId = line.variantId,
                    expectedQuantity = line.expectedQty,
                    receivedQuantity = received,
                    damagedQuantity = damaged,
                    missingQuantity = missing,
                    excessQuantity = 0,
                    conditionNotes = line.conditionNotes,
                    goodQuantity = good,
                    pendingQuantity = pending
                )
            )
        }
        return drafts
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
                // Header Bar
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
                            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = SecondaryTealLight, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "ওয়্যারহাউস পণ্য গ্রহণ ও পরিদর্শন" else "Warehouse Receiving Inspection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = if (order != null) "Purchase Order: ${order.poNumber}" else "Shipment: ${shipment?.shipmentNumber ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = requestClose, enabled = !isSaving) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Cost Finalized Status Alert Banner
                if (!isCostFinalized) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusWarningContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isBangla) "প্রকৃত খরচ চূড়ান্তকরণ বাকি — স্টক পোস্টিং সাময়িক লক" else "Actual Landing Cost Pending — Stock Posting Locked",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusWarning
                                )
                                Text(
                                    text = if (isBangla)
                                        "আপনি খসড়া পরিদর্শন সংরক্ষণ করতে পারবেন, তবে প্রকৃত খরচ চূড়ান্ত না করা পর্যন্ত মজুদ ইনভেন্টরি পোস্টিং হবে না।"
                                    else
                                        "Draft inspections can be recorded, but stock posting to inventory is blocked until Actual Landing Cost is finalized.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusWarning
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusSuccessContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "প্রকৃত ল্যান্ডেড কস্ট চূড়ান্তকৃত! স্টক পোস্টিং অনুমোদিত।" else "Actual Landed Cost Finalized! Stock posting unlocked.",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = StatusSuccess
                            )
                        }
                    }
                }

                // Receiving Method Switcher: Method A (Physical Count) vs Method B (Carton-wise)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Method A: Physical Count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedMethod == "PHYSICAL_COUNT") PrimaryPurple else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (selectedMethod == "PHYSICAL_COUNT") PrimaryPurple else Color.LightGray, RoundedCornerShape(10.dp))
                            .clickable { selectedMethod = "PHYSICAL_COUNT" }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBangla) "পদ্ধতি ক: সরাসরি গণনা (Physical Count)" else "Method A: Physical Count",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedMethod == "PHYSICAL_COUNT") MaterialTheme.colorScheme.onPrimary else TextPrimary
                        )
                    }

                    // Method B: Carton-wise
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedMethod == "CARTON_WISE") PrimaryPurple else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (selectedMethod == "CARTON_WISE") PrimaryPurple else Color.LightGray, RoundedCornerShape(10.dp))
                            .clickable { selectedMethod = "CARTON_WISE" }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBangla) "পদ্ধতি খ: কার্টন-ভিত্তিক (Carton-wise)" else "Method B: Carton-wise",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedMethod == "CARTON_WISE") MaterialTheme.colorScheme.onPrimary else TextPrimary
                        )
                    }
                }

                // Error message
                if (errorMessage != null || saveError != null) {
                    Text(
                        text = saveError ?: errorMessage.orEmpty(),
                        color = StatusError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Physical Carton vs Product Line Summary Pill (prevents mixed-carton double counting in display)
                val distinctPhysicalCartons = remember(lines.size, selectedMethod) {
                    lines.map { it.cartonNumber }
                        .filter { it.isNotBlank() && it != "SUMMARY" }
                        .distinct()
                }
                val totalExpectedPcs = lines.sumOf { it.expectedQty }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ERPSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedMethod == "CARTON_WISE") {
                            if (isBangla)
                                "মোট কার্টন: ${distinctPhysicalCartons.size} CTN (${lines.size}টি পণ্য লাইন)"
                            else
                                "Physical Cartons: ${distinctPhysicalCartons.size} CTN (${lines.size} product lines)"
                        } else {
                            if (isBangla)
                                "মোট পণ্য লাইন: ${lines.size}টি"
                            else
                                "Product Lines: ${lines.size}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                    Text(
                        text = if (isBangla) "মোট প্যাকড: $totalExpectedPcs পিস" else "Total Packed: $totalExpectedPcs pcs",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }

                // Items Inspection Table
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedMethod == "CARTON_WISE") {
                        // Group by physical cartonNumber so mixed cartons (multi-category/product) render as 1 physical carton card
                        val groupedCartons = lines.groupBy { it.cartonNumber.ifBlank { "CTN" } }.entries.toList()
                        items(groupedCartons) { (cartonNo, cartonLines) ->
                            val isMixed = cartonLines.size > 1
                            val cartonExpectedSum = cartonLines.sumOf { it.expectedQty }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Outlined.ViewInAr,
                                                contentDescription = null,
                                                tint = if (isMixed) SecondaryTeal else PrimaryPurple,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = cartonNo,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            if (isMixed) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(SecondaryTeal.copy(alpha = 0.14f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (isBangla) "মিক্সড কার্টন (${cartonLines.size} পণ্য)" else "Mixed (${cartonLines.size} items)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SecondaryTeal
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = if (isBangla) "মোট প্যাকড: $cartonExpectedSum" else "Carton Packed: $cartonExpectedSum",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary
                                        )
                                    }

                                    cartonLines.forEachIndexed { lineIdx, line ->
                                        if (lineIdx > 0) {
                                            HorizontalDivider(
                                                color = ERPDivider,
                                                modifier = Modifier.padding(vertical = 10.dp)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = line.productName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "Expected: ${line.expectedQty}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ReceivingLineInputs(line = line, isBangla = isBangla)
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(lines) { _, line ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = line.productName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "Expected: ${line.expectedQty}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    ReceivingLineInputs(line = line, isBangla = isBangla)
                                }
                            }
                        }
                    }
                }

                // Bottom Action Footer
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
                        // Button 1: Save Draft (always available)
                        OutlinedButton(
                            enabled = !isSaving && onSaveDraft != null,
                            onClick = {
                                val drafts = validateAndBuildDrafts() ?: return@OutlinedButton
                                onSaveDraft?.invoke(destinationWarehouseId, selectedMethod, drafts, notes)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isBangla) "খসড়া সংরক্ষণ" else "Save Draft")
                        }

                        // Button 2: Post to Stock (Inventory Capitalization)
                        Button(
                            enabled = !isSaving,
                            onClick = {
                                if (!isCostFinalized) {
                                    showBlockedCostDialog = true
                                    return@Button
                                }
                                validateAndBuildDrafts() ?: return@Button
                                showConfirmationDialog = true
                            },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCostFinalized) StatusSuccess else Color.Gray
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                if (isCostFinalized) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isSaving && isBangla -> "সংরক্ষণ হচ্ছে…"
                                    isSaving -> "Saving…"
                                    isBangla -> "স্টকে পোস্টিং করুন"
                                    else -> "Post to Stock"
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Cost Blocked Warning Dialog
    if (showBlockedCostDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedCostDialog = false },
            icon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = StatusWarning) },
            title = {
                Text(
                    text = if (isBangla) "প্রকৃত খরচ চূড়ান্তকরণ বাধ্যতামূলক" else "Actual Landing Cost Finalization Required",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBangla)
                        "মজুদ পণ্যে পোস্টিং করার আগে প্রকৃত ল্যান্ডেড কস্ট (Actual Landing Cost) চূড়ান্ত করতে হবে। অনুগ্রহ করে ক্রয়াদেশের 'Landing Cost' সেকশনে গিয়ে প্রকৃত খরচ এন্ট্রি করে 'Finalize Actual Landing Cost' সম্পন্ন করুন।"
                    else
                        "Stock posting into inventory is strictly blocked until Actual Landing Cost is finalized. Please enter actual expenses and click 'Finalize Actual Landing Cost' first."
                )
            },
            confirmButton = {
                Button(onClick = { showBlockedCostDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Confirm Post Dialog
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            icon = { Icon(Icons.Outlined.Warning, contentDescription = null, tint = SecondaryTeal) },
            title = {
                Text(
                    text = if (isBangla) "মজুদ পোস্টিং নিশ্চিতকরণ" else "Confirm Stock Posting",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBangla)
                        "আপনি কি নিশ্চিত যে এই রিসিভিং মজুদ ইনভেন্টরিতে পোস্টিং করতে চান? এটি শুধুমাত্র ভালো পণ্য (Good Quantity) স্টকে যোগ করবে, নষ্ট পণ্য অবলোপন করবে এবং ঘাটতির জন্য সরবরাহকারীর কাছে দাবি রেকর্ড করবে।"
                    else
                        "Are you sure you want to post this receiving to inventory? This will debit Inventory for good units, write off damaged stock, and log shortage claims."
                )
            },
            confirmButton = {
                Button(
                    enabled = !isSaving,
                    onClick = {
                        showConfirmationDialog = false
                        val drafts = validateAndBuildDrafts() ?: return@Button
                        if (onConfirmPost != null) {
                            onConfirmPost(destinationWarehouseId, selectedMethod, drafts, notes, true)
                        } else {
                            onSaveDraft?.invoke(destinationWarehouseId, selectedMethod, drafts, notes)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                ) {
                    Text(text = if (isBangla) "হ্যাঁ, পোস্টিং করুন" else "Yes, Post to Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReceivingLineInputs(
    line: ReceivingLineState,
    isBangla: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = line.goodQtyStr,
            onValueChange = { newVal ->
                line.goodQtyStr = newVal
                val g = newVal.trim().toIntOrNull() ?: 0
                val d = line.damagedQtyStr.trim().toIntOrNull() ?: 0
                val m = line.missingQtyStr.trim().toIntOrNull() ?: 0
                line.receivedQtyStr = (g + d).toString()
                line.pendingQtyStr = maxOf(0, line.expectedQty - g - d - m).toString()
            },
            label = { Text("Good", fontSize = 11.sp, color = StatusSuccess) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = line.damagedQtyStr,
            onValueChange = { newVal ->
                line.damagedQtyStr = newVal
                val g = line.goodQtyStr.trim().toIntOrNull() ?: 0
                val d = newVal.trim().toIntOrNull() ?: 0
                val m = line.missingQtyStr.trim().toIntOrNull() ?: 0
                line.receivedQtyStr = (g + d).toString()
                line.pendingQtyStr = maxOf(0, line.expectedQty - g - d - m).toString()
            },
            label = { Text("Damaged", fontSize = 11.sp, color = StatusError) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = line.missingQtyStr,
            onValueChange = { newVal ->
                line.missingQtyStr = newVal
                val g = line.goodQtyStr.trim().toIntOrNull() ?: 0
                val d = line.damagedQtyStr.trim().toIntOrNull() ?: 0
                val m = newVal.trim().toIntOrNull() ?: 0
                line.pendingQtyStr = maxOf(0, line.expectedQty - g - d - m).toString()
            },
            label = { Text("Missing", fontSize = 11.sp, color = StatusWarning) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = line.pendingQtyStr,
            onValueChange = { line.pendingQtyStr = it },
            label = { Text("Pending", fontSize = 11.sp, color = PrimaryPurple) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = line.conditionNotes,
        onValueChange = { line.conditionNotes = it },
        label = { Text(if (isBangla) "পরিদর্শন মন্তব্য" else "Inspection notes", fontSize = 11.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
