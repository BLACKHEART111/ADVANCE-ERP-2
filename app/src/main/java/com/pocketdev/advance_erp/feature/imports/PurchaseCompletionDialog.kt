package com.pocketdev.advance_erp.feature.imports

import com.pocketdev.advance_erp.core.designsystem.StatusWarningContainer
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Warning
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
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.PurchaseOrder

@Composable
fun PurchaseCompletionDialog(
    order: PurchaseOrder,
    totalOrdered: Int,
    totalReceived: Int,
    goodQuantity: Int,
    damagedQuantity: Int,
    missingQuantity: Int,
    paymentDueMinor: Long,
    isCostFinalized: Boolean,
    isBangla: Boolean = false,
    isSaving: Boolean = false,
    saveError: String? = null,
    onDismiss: () -> Unit,
    onConfirmComplete: (disposition: String?, reason: String?, notes: String?) -> Unit
) {
    val remainingQuantity = (totalOrdered - totalReceived).coerceAtLeast(0)
    var selectedDisposition by rememberSaveable { mutableStateOf("SHORTAGE") }
    var reason by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dispositions = listOf(
        "SHORTAGE" to if (isBangla) "ঘাটতি অমীমাংসিত (Shortage Discrepancy)" else "Shortage Discrepancy",
        "CANCELLED_BALANCE" to if (isBangla) "অবশিষ্ট অর্ডার বাতিল (Cancelled Balance)" else "Cancelled Unfulfilled Balance",
        "SUPPLIER_CLAIM" to if (isBangla) "সরবরাহকারী ক্ষতিপূরণ দাবি (Supplier Claim)" else "Supplier Claim"
    )

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
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
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "ক্রয়াদেশ সমাপ্তিকরণ (Manual Completion)" else "Manual Purchase Completion",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = if (isBangla) "ক্রয়াদেশ: ${order.poNumber}" else "Purchase: ${order.poNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary Audit Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isBangla) "চূড়ান্ত নিরীক্ষা ও স্থিতি সারসংক্ষেপ:" else "Final Order Reconciliation Summary:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            val unitLabel = if (isBangla) "পিস" else "units"
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isBangla) "অর্ডারকৃত পরিমাণ:" else "Ordered Quantity:", color = TextSecondary, fontSize = 13.sp)
                                Text("$totalOrdered $unitLabel", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isBangla) "মোট রিসিভড:" else "Total Received:", color = TextSecondary, fontSize = 13.sp)
                                Text("$totalReceived $unitLabel", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isBangla) "ভালো (স্টকে যুক্ত):" else "Good (In Stock):", color = TextSecondary, fontSize = 13.sp)
                                Text("$goodQuantity $unitLabel", fontWeight = FontWeight.Bold, color = StatusSuccess)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isBangla) "ক্ষতিগ্রস্ত (রাইট-অফ):" else "Damaged (Written Off):", color = TextSecondary, fontSize = 13.sp)
                                Text("$damagedQuantity $unitLabel", fontWeight = FontWeight.Bold, color = StatusError)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isBangla) "নিখোঁজ (ঘাটতি দাবি):" else "Missing (Shortage Claim):", color = TextSecondary, fontSize = 13.sp)
                                Text("$missingQuantity $unitLabel", fontWeight = FontWeight.Bold, color = StatusWarning)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isBangla) "অবশিষ্ট অপ্রাপ্ত:" else "Remaining Unreceived:", color = TextSecondary, fontSize = 13.sp)
                                Text("$remainingQuantity $unitLabel", fontWeight = FontWeight.Bold, color = if (remainingQuantity > 0) StatusError else StatusSuccess)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isBangla) "বকেয়া পেমেন্ট:" else "Payment Due Balance:", color = TextSecondary, fontSize = 13.sp)
                                Text("${order.currency} ${Formatters.formatMinorUnit(paymentDueMinor)}", fontWeight = FontWeight.Bold, color = if (paymentDueMinor > 0) StatusWarning else StatusSuccess)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isBangla) "প্রকৃত খরচ স্থিতি:" else "Actual Cost Status:", color = TextSecondary, fontSize = 13.sp)
                                Text(
                                    text = if (isBangla) {
                                        if (isCostFinalized) "চূড়ান্ত" else "অপেক্ষমাণ"
                                    } else {
                                        if (isCostFinalized) "FINALIZED" else "PENDING"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCostFinalized) StatusSuccess else StatusWarning
                                )
                            }
                        }
                    }

                    // If remaining unreceived units exist: require disposition and reason!
                    if (remainingQuantity > 0) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StatusWarningContainer),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarningContainer)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBangla)
                                            "সতর্কতা: $remainingQuantity টি পণ্য অপূর্ণ রয়েছে! নিষ্পত্তি কারণ বাধ্যতামূলক।"
                                        else
                                            "Notice: $remainingQuantity units unreceived! Explicit disposition required.",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusWarning
                                    )
                                }

                                Text(
                                    text = if (isBangla) "অপূর্ণ অংশের নিষ্পত্তি নির্বাচন করুন:" else "Select Unreceived Disposition:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )

                                dispositions.forEach { (dispKey, dispLabel) ->
                                    val isSel = dispKey == selectedDisposition
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) PrimaryPurple.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                            .border(1.dp, if (isSel) PrimaryPurple else ERPDivider, RoundedCornerShape(8.dp))
                                            .clickable { selectedDisposition = dispKey }
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = dispLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) PrimaryPurple else TextPrimary
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = reason,
                                    onValueChange = { reason = it },
                                    label = { Text(if (isBangla) "নিষ্পত্তির কারণ (বাধ্যতামূলক)" else "Disposition Reason (Required)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    if (errorMessage != null || saveError != null) {
                        Text(
                            text = saveError ?: errorMessage.orEmpty(),
                            color = StatusError,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(if (isBangla) "সমাপ্তি মন্তব্য (বাধ্যতামূলক নয়)" else "Completion Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = if (isBangla) "বাতিল" else "Cancel")
                        }

                        Button(
                            onClick = {
                                if (remainingQuantity > 0 && reason.isBlank()) {
                                    errorMessage = if (isBangla)
                                        "অবশিষ্ট $remainingQuantity পণ্যের নিষ্পত্তির কারণ উল্লেখ করা বাধ্যতামূলক।"
                                    else
                                        "Reason is required for $remainingQuantity unreceived units."
                                    return@Button
                                }
                                onConfirmComplete(
                                    if (remainingQuantity > 0) selectedDisposition else null,
                                    reason.ifBlank { null },
                                    notes.ifBlank { null }
                                )
                            },
                            enabled = !isSaving,
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Text(text = if (isBangla) "সম্পন্ন নিশ্চিত করুন" else "Confirm Complete")
                        }
                    }
                }
            }
        }
    }
}
