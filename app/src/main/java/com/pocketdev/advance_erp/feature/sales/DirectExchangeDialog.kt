package com.pocketdev.advance_erp.feature.sales

import com.pocketdev.advance_erp.core.designsystem.StatusSuccessContainer
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusWarningContainer
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.core.util.firstInStockProduct
import com.pocketdev.advance_erp.core.util.firstInStockVariant
import com.pocketdev.advance_erp.core.util.sellableStockForProduct
import com.pocketdev.advance_erp.core.util.sellableStockForVariant
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import com.pocketdev.advance_erp.data.entity.StockBalance
import com.pocketdev.advance_erp.domain.repository.DirectExchangeDraft
import com.pocketdev.advance_erp.domain.repository.ReturnItemDraft
import com.pocketdev.advance_erp.domain.repository.InvoiceItemDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectExchangeDialog(
    invoice: SalesInvoice,
    invoiceItems: List<SalesInvoiceItem>,
    allProducts: List<Product>,
    allVariants: List<ProductVariant>,
    stockBalances: List<StockBalance> = emptyList(),
    onDismiss: () -> Unit,
    onExchangeConfirmed: (DirectExchangeDraft) -> Unit
) {
    val variantsByProduct = remember(allVariants) { allVariants.groupBy { it.productId } }
    var selectedReturnItem by remember { mutableStateOf(invoiceItems.firstOrNull()) }
    var returnQtyText by rememberSaveable { mutableStateOf("1") }
    var isDamagedReturn by rememberSaveable { mutableStateOf(false) }

    var selectedReplacementProduct by remember {
        mutableStateOf(firstInStockProduct(stockBalances, invoice.warehouseId, allProducts, variantsByProduct))
    }
    var selectedReplacementVariant by remember {
        mutableStateOf(
            selectedReplacementProduct?.let { p ->
                firstInStockVariant(stockBalances, invoice.warehouseId, p.id, variantsByProduct[p.id].orEmpty())
            }
        )
    }
    var replacementVariantDropdown by rememberSaveable { mutableStateOf(false) }
    var replacementQtyText by rememberSaveable { mutableStateOf("1") }
    var notes by rememberSaveable { mutableStateOf("") }

    val returnQty = returnQtyText.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val replacementQty = replacementQtyText.toIntOrNull()?.coerceAtLeast(1) ?: 1

    val returnedCreditMinor = (selectedReturnItem?.unitPriceMinor ?: 0L) * returnQty
    val replacementUnitPriceMinor = selectedReplacementVariant?.variantPriceMinor ?: selectedReplacementProduct?.sellingPriceMinor ?: 0L
    val replacementDebitMinor = replacementUnitPriceMinor * replacementQty
    val priceDiffMinor = replacementDebitMinor - returnedCreditMinor // > 0 means customer pays, < 0 means refund

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f),
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
                        Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Direct Product Exchange / পণ্য বিনিময়",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Original Invoice: ${invoice.invoiceNumber} • Customer: ${invoice.customerName}",
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

                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Left Column: Returned Item
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("1. Returned Item / ফেরত পণ্য", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)

                            Text("Select item from invoice:", style = MaterialTheme.typography.bodySmall)
                            invoiceItems.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedReturnItem?.id == item.id,
                                        onClick = { selectedReturnItem = item }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(item.productNameEn, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                        Text("Qty: ${item.quantity} | ৳${Formatters.formatMinorUnit(item.unitPriceMinor)}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = returnQtyText,
                                onValueChange = { input ->
                                    val q = input.filter { it.isDigit() }
                                    val maxQ = selectedReturnItem?.quantity ?: 1
                                    if ((q.toIntOrNull() ?: 0) <= maxQ) returnQtyText = q
                                },
                                label = { Text("Return Quantity / ফেরতের সংখ্যা") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Condition toggle
                            Text("Product Condition / পণ্যের অবস্থা:", style = MaterialTheme.typography.labelMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = !isDamagedReturn,
                                    onClick = { isDamagedReturn = false },
                                    label = { Text("Good / Sellable (পুনর্বিক্রয়যোগ্য)") }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = isDamagedReturn,
                                    onClick = { isDamagedReturn = true },
                                    label = { Text("Damaged (কোয়ারেন্টাইন)") }
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                "Return Credit: ৳${Formatters.formatMinorUnit(returnedCreditMinor)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Right Column: Replacement Item
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("2. Replacement Item / প্রতিস্থাপন পণ্য", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                            // Product selector
                            Text("Select Replacement Product:", style = MaterialTheme.typography.bodySmall)
                            val washedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.36f)
                            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(allProducts) { prod ->
                                    val pVars = variantsByProduct[prod.id].orEmpty()
                                    val prodStock = sellableStockForProduct(stockBalances, invoice.warehouseId, prod.id, pVars)
                                    val isProdInStock = prodStock > 0
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = isProdInStock,
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedReplacementProduct?.id == prod.id) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            },
                                            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                                        ),
                                        onClick = {
                                            if (isProdInStock) {
                                                selectedReplacementProduct = prod
                                                selectedReplacementVariant = firstInStockVariant(stockBalances, invoice.warehouseId, prod.id, pVars)
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = prod.nameEn,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isProdInStock) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isProdInStock) MaterialTheme.colorScheme.onSurface else washedColor,
                                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                                            )
                                            Text(
                                                text = if (isProdInStock) {
                                                    "৳${Formatters.formatMinorUnit(prod.sellingPriceMinor)} • স্টক: $prodStock"
                                                } else {
                                                    "স্টক নেই"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isProdInStock) MaterialTheme.colorScheme.primary else washedColor
                                            )
                                        }
                                    }
                                }
                            }

                            val repVariants = selectedReplacementProduct?.let { variantsByProduct[it.id].orEmpty() }.orEmpty()
                            if (repVariants.isNotEmpty() && selectedReplacementProduct != null) {
                                ExposedDropdownMenuBox(
                                    expanded = replacementVariantDropdown,
                                    onExpandedChange = { replacementVariantDropdown = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedReplacementVariant?.let { v ->
                                            listOfNotNull(v.color, v.size, v.sku).joinToString(" - ")
                                        } ?: "Select Variant / ভেরিয়েন্ট",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Replacement Variant") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = replacementVariantDropdown) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = replacementVariantDropdown,
                                        onDismissRequest = { replacementVariantDropdown = false }
                                    ) {
                                        repVariants.forEach { v ->
                                            val varStock = sellableStockForVariant(
                                                stockBalances,
                                                invoice.warehouseId,
                                                selectedReplacementProduct!!.id,
                                                v.id
                                            )
                                            val isVarInStock = varStock > 0
                                            DropdownMenuItem(
                                                enabled = isVarInStock,
                                                colors = MenuDefaults.itemColors(
                                                    textColor = MaterialTheme.colorScheme.onSurface,
                                                    disabledTextColor = washedColor
                                                ),
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = listOfNotNull(v.color, v.size, v.sku).joinToString(" • "),
                                                            color = if (isVarInStock) MaterialTheme.colorScheme.onSurface else washedColor,
                                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                                        )
                                                        Text(
                                                            text = if (isVarInStock) "স্টক: $varStock" else "স্টক নেই",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isVarInStock) MaterialTheme.colorScheme.primary else washedColor
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    if (!isVarInStock) return@DropdownMenuItem
                                                    selectedReplacementVariant = v
                                                    replacementVariantDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = replacementQtyText,
                                onValueChange = { replacementQtyText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Replacement Quantity") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                "Replacement Debit: ৳${Formatters.formatMinorUnit(replacementDebitMinor)}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Settlement Summary Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (priceDiffMinor >= 0) StatusSuccessContainer else StatusWarningContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Net Price Settlement / সমন্বয়:", fontWeight = FontWeight.Bold)
                            Text(
                                if (priceDiffMinor > 0) "Customer to pay difference / গ্রাহক অতিরিক্ত পরিশোধ করবেন"
                                else if (priceDiffMinor < 0) "Store refund to customer / দোকান থেকে ফেরতযোগ্য"
                                else "Equal exchange / সমমূল্যে পরিবর্তন",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            "৳${Formatters.formatMinorUnit(kotlin.math.abs(priceDiffMinor))}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (priceDiffMinor >= 0) StatusSuccess else StatusWarning
                        )
                    }
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
                        enabled = selectedReturnItem != null && selectedReplacementProduct != null,
                        onClick = {
                            val retItem = selectedReturnItem ?: return@Button
                            val repProduct = selectedReplacementProduct ?: return@Button

                            val draft = DirectExchangeDraft(
                                customerId = invoice.customerId,
                                warehouseId = invoice.warehouseId,
                                originalInvoiceId = invoice.id,
                                returnedItems = listOf(
                                    ReturnItemDraft(
                                        invoiceItemId = retItem.id,
                                        productId = retItem.productId,
                                        variantId = retItem.variantId,
                                        returnQuantity = returnQty,
                                        isDamaged = isDamagedReturn,
                                        damagedQuantity = if (isDamagedReturn) returnQty else 0
                                    )
                                ),
                                replacementItems = listOf(
                                    InvoiceItemDraft(
                                        productId = repProduct.id,
                                        variantId = selectedReplacementVariant?.id,
                                        quantity = replacementQty,
                                        unitPriceMinor = replacementUnitPriceMinor
                                    )
                                ),
                                netDifferenceMinor = priceDiffMinor,
                                paymentMethod = "CASH",
                                notes = "Direct Exchange for ${invoice.invoiceNumber}. $notes"
                            )
                            onExchangeConfirmed(draft)
                        }
                    ) {
                        Text("Confirm Exchange / বিনিময় নিশ্চিত করুন")
                    }
                }
            }
        }
    }
}
