package com.pocketdev.advance_erp.feature.sales

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.domain.repository.DeliveryDispatchDraft
import com.pocketdev.advance_erp.domain.repository.DeliveryDispatchItemDraft
import com.pocketdev.advance_erp.domain.repository.UndeliveredItemInfo

@Composable
fun DispatchDeliveryDialog(
    invoice: SalesInvoice,
    remainingItems: List<UndeliveredItemInfo>,
    onDismiss: () -> Unit,
    onDispatch: (DeliveryDispatchDraft) -> Unit
) {
    var deliveryPerson by rememberSaveable { mutableStateOf("") }
    var transportInfo by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    // Map itemId to dispatch quantity string
    val dispatchQtyMap = remember(remainingItems) {
        mutableStateMapOf<String, String>().apply {
            remainingItems.forEach { item ->
                put(item.invoiceItemId, item.remainingQuantity.toString())
            }
        }
    }
    val allAlreadyDispatched = remember(remainingItems) {
        remainingItems.isNotEmpty() && remainingItems.all { it.remainingQuantity <= 0 }
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Dispatch Delivery Challan / ডেলিভারি চালান",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Invoice: ${invoice.invoiceNumber} • Customer: ${invoice.customerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Metadata inputs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = deliveryPerson,
                        onValueChange = { deliveryPerson = it },
                        label = { Text("Delivery Person / ডেলিভারিম্যান") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = transportInfo,
                        onValueChange = { transportInfo = it },
                        label = { Text("Transport / গাড়ি নম্বর") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Items to Dispatch / সরবরাহের পণ্য তালিকা:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)

                Spacer(modifier = Modifier.height(6.dp))

                // Items list
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(remainingItems) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text(item.productNameEn, fontWeight = FontWeight.Bold)
                                    Text("SKU: ${item.sku}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Ordered: ${item.orderedQuantity} | Remaining: ${item.remainingQuantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Dispatch Qty:", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedTextField(
                                        value = dispatchQtyMap[item.invoiceItemId] ?: "",
                                        onValueChange = { input ->
                                            val qty = input.filter { it.isDigit() }
                                            val intVal = qty.toIntOrNull() ?: 0
                                            if (intVal <= item.remainingQuantity) {
                                                dispatchQtyMap[item.invoiceItemId] = qty
                                            }
                                        },
                                        modifier = Modifier.width(80.dp),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Delivery Notes / বিশেষ নির্দেশনা") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (allAlreadyDispatched) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "এই চালানের সকল পণ্য ইতিমধ্যেই ডেলিভারি করা হয়েছে। / All items on this invoice have already been dispatched.",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        enabled = !allAlreadyDispatched,
                        onClick = {
                            val itemDrafts = remainingItems.mapNotNull { item ->
                                val qty = dispatchQtyMap[item.invoiceItemId]?.toIntOrNull() ?: 0
                                if (qty > 0) {
                                    DeliveryDispatchItemDraft(
                                        invoiceItemId = item.invoiceItemId,
                                        productId = item.productId,
                                        variantId = item.variantId,
                                        quantityToDispatch = qty
                                    )
                                } else null
                            }

                            if (itemDrafts.isEmpty()) {
                                validationError = "অন্তত ১টি পণ্যের ডেলিভারি পরিমাণ (০-এর বেশি) দিন।"
                            } else {
                                validationError = null
                                onDispatch(
                                    DeliveryDispatchDraft(
                                        invoiceId = invoice.id,
                                        warehouseId = invoice.warehouseId,
                                        deliveryPerson = deliveryPerson.ifBlank { null },
                                        transportInfo = transportInfo.ifBlank { null },
                                        notes = notes.ifBlank { null },
                                        items = itemDrafts
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Confirm Dispatch / চালান নিশ্চিত করুন")
                    }
                }
            }
        }
    }
}
