package com.pocketdev.advance_erp.feature.imports

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.ERPSurfaceVariant
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.LiquidAccountKind
import com.pocketdev.advance_erp.data.entity.PaymentMethod
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.Supplier
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun RecordSupplierPaymentDialog(
    suppliers: List<Supplier>,
    orders: List<PurchaseOrder>,
    preselectedSupplierId: String? = null,
    preselectedOrderId: String? = null,
    isSaving: Boolean = false,
    dismissOnSubmit: Boolean = true,
    saveError: String? = null,
    liquidAccounts: List<ChartOfAccount> = emptyList(),
    nextPaymentNumber: String? = null,
    isBangla: Boolean = false,
    onSwitchToOtherPayment: (() -> Unit)? = null,
    onConfirmWithDetails: ((
        supplierId: String,
        orderId: String?,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: PaymentMethod,
        paymentAccount: String,
        paymentAccountId: String?,
        paymentDate: Long,
        reference: String?,
        notes: String?
    ) -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        supplierId: String,
        orderId: String?,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: PaymentMethod,
        paymentAccount: String,
        reference: String?,
        notes: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val lang = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH
    val initialOrder = orders.find { it.id == preselectedOrderId }
    val initialSupplierId = preselectedSupplierId ?: initialOrder?.supplierId ?: suppliers.firstOrNull()?.id ?: ""
    val initialAmount = if (initialOrder != null) {
        val remaining = (initialOrder.totalAmountMinor - initialOrder.paidAmountMinor).coerceAtLeast(0L)
        val major = remaining / 100
        val minor = remaining % 100
        String.format(Locale.US, "%d.%02d", major, minor)
    } else ""

    var selectedSupplierId by remember { mutableStateOf(initialSupplierId) }
    var supplierSearchQuery by rememberSaveable { mutableStateOf("") }
    var selectedOrderId by remember { mutableStateOf(initialOrder?.id) }
    var amountStr by remember { mutableStateOf(initialAmount) }
    var selectedCurrency by remember { mutableStateOf(initialOrder?.currency ?: suppliers.find { it.id == initialSupplierId }?.currency ?: "BDT") }
    var exchangeRateStr by remember { mutableStateOf(initialOrder?.exchangeRateToBdt ?: if (selectedCurrency == "BDT") "1" else "") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.BANK_TT) }
    val activeLiquidAccounts = remember(liquidAccounts) { liquidAccounts.filter { it.isActive } }
    var selectedAccountId by remember(activeLiquidAccounts) {
        mutableStateOf(
            activeLiquidAccounts.firstOrNull { it.liquidKind == LiquidAccountKind.BANK }?.id
                ?: activeLiquidAccounts.firstOrNull()?.id
        )
    }
    var paymentAccount by rememberSaveable {
        mutableStateOf(
            activeLiquidAccounts.firstOrNull { it.id == selectedAccountId }?.nameEn
                ?: "City Bank Corporate (Import LC)"
        )
    }
    var selectedPaymentDateMillis by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var referenceNumber by rememberSaveable { mutableStateOf("") }
    var notes by remember { mutableStateOf(if (initialOrder != null) "Payment for ${initialOrder.poNumber}" else "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }
    fun openDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedPaymentDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedPaymentDateMillis = chosen.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val filteredSuppliers = remember(suppliers, supplierSearchQuery) {
        val q = supplierSearchQuery.trim()
        if (q.isBlank()) suppliers else suppliers.filter {
            it.companyName.contains(q, true) || it.supplierCode.contains(q, true) || it.phone.contains(q, true)
        }
    }
    val openOrders = remember(orders, selectedSupplierId) {
        orders.filter { it.supplierId == selectedSupplierId && it.status !in listOf("DRAFT", "CANCELLED") && it.totalAmountMinor > it.paidAmountMinor }
    }
    val selectedOrder = orders.find { it.id == selectedOrderId }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp))
                .navigationBarsPadding()
                .imePadding(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "পেমেন্ট প্রদান (Make Payment)" else "Make Outgoing Payment",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isBangla) "সরবরাহকারী পেমেন্ট ও অগ্রিম / বকেয়া পরিশোধ" else "Supplier remittance, advance, partial or balance payment",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PrimaryPurple.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "পেমেন্ট নম্বর:" else "Payment Number:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = PrimaryPurple) {
                            Text(
                                text = nextPaymentNumber ?: "AUTO-GEN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (onSwitchToOtherPayment != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryPurple,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isBangla) "সরবরাহকারী পেমেন্ট" else "Supplier Payment",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ERPSurfaceVariant,
                            border = BorderStroke(1.dp, ERPDivider),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSwitchToOtherPayment() }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isBangla) "অন্যান্য ব্যয় পেমেন্ট" else "Other Business Payment",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Supplier Search & Selection
                    item {
                        Text(
                            text = if (isBangla) "সরবরাহকারী / প্রাপক নির্বাচন করুন:" else "Select Supplier / Payee:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (suppliers.size > 3) {
                            OutlinedTextField(
                                value = supplierSearchQuery,
                                onValueChange = { supplierSearchQuery = it },
                                placeholder = { Text(if (isBangla) "নাম বা কোড দিয়ে খুঁজুন..." else "Search supplier...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                trailingIcon = {
                                    if (supplierSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { supplierSearchQuery = "" }, modifier = Modifier.size(26.dp)) {
                                            Icon(Icons.Outlined.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            filteredSuppliers.forEach { sup ->
                                val isSel = sup.id == selectedSupplierId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) PrimaryPurple.copy(alpha = 0.14f) else ERPSurfaceVariant)
                                        .border(1.dp, if (isSel) PrimaryPurple else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedSupplierId = sup.id
                                            selectedOrderId = null
                                            selectedCurrency = sup.currency
                                            exchangeRateStr = if (sup.currency == "BDT") "1" else ""
                                            amountStr = ""
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sup.companyName, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(sup.supplierCode, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                    Text(
                                        text = "${if (isBangla) "দেনা:" else "Due:"} ${Formatters.formatMoney(sup.outstandingPayableMinor, CurrencyCode.BDT, lang)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sup.outstandingPayableMinor > 0L) StatusError else SecondaryTeal
                                    )
                                }
                            }
                        }
                    }

                    // Link PO (Optional: Advance / Partial / Balance)
                    item {
                        Text(
                            text = if (isBangla) "ক্রয়াদেশ (PO) নির্বাচন (অগ্রিম বা বকেয়া):" else "Link Purchase Order (Advance / Partial / Balance):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedOrderId == null) PrimaryPurple.copy(alpha = 0.14f) else ERPSurfaceVariant)
                                    .clickable { selectedOrderId = null }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = if (isBangla) "সাধারণ অগ্রিম / বকেয়া পেমেন্ট (PO ছাড়া)" else "General Supplier Advance / Payment (No specific PO)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (selectedOrderId == null) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            openOrders.forEach { po ->
                                val isPoSel = selectedOrderId == po.id
                                val remainingMinor = (po.totalAmountMinor - po.paidAmountMinor).coerceAtLeast(0L)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isPoSel) PrimaryPurple.copy(alpha = 0.14f) else ERPSurfaceVariant)
                                        .clickable {
                                            selectedOrderId = po.id
                                            selectedCurrency = po.currency
                                            exchangeRateStr = po.exchangeRateToBdt
                                            amountStr = BigDecimal(remainingMinor).movePointLeft(2).toPlainString()
                                        }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${po.poNumber} (${po.status})", style = MaterialTheme.typography.bodySmall, fontWeight = if (isPoSel) FontWeight.Bold else FontWeight.Normal)
                                    Text("${po.currency} ${BigDecimal(remainingMinor).movePointLeft(2).toPlainString()} due", style = MaterialTheme.typography.bodySmall, color = StatusError)
                                }
                            }
                        }
                        if (selectedOrder != null) {
                            val remainingMinor = (selectedOrder.totalAmountMinor - selectedOrder.paidAmountMinor).coerceAtLeast(0L)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        amountStr = BigDecimal(remainingMinor / 2).movePointLeft(2).toPlainString()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isBangla) "৫০% আংশিক" else "50% Partial", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        amountStr = BigDecimal(remainingMinor).movePointLeft(2).toPlainString()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isBangla) "১০০% বকেয়া" else "100% Balance", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Amount & Currency
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = amountStr,
                                onValueChange = { amountStr = it },
                                label = { Text("${if (isBangla) "পরিমাণ" else "Amount"} ($selectedCurrency) *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = if (selectedCurrency == "BDT") "1" else exchangeRateStr,
                                enabled = selectedCurrency != "BDT",
                                onValueChange = { exchangeRateStr = it },
                                label = { Text(if (isBangla) "BDT হার" else "Rate to BDT") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Payment Date Picker
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { openDatePicker() },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, ERPDivider)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isBangla) "পেমেন্টের তারিখ" else "Payment Date", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                }
                                Text(
                                    text = if (isBangla) Formatters.toBanglaDigits(dateFormat.format(Date(selectedPaymentDateMillis))) else dateFormat.format(Date(selectedPaymentDateMillis)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Payment Method Chips
                    item {
                        Text(if (isBangla) "পেমেন্ট মাধ্যম" else "Payment Method", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(PaymentMethod.entries) { pm ->
                                val isSel = selectedMethod == pm
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) PrimaryPurple else ERPDivider)
                                        .clickable {
                                            selectedMethod = pm
                                            if (pm == PaymentMethod.CASH) {
                                                activeLiquidAccounts.firstOrNull { it.liquidKind == LiquidAccountKind.CASH }?.let { acc ->
                                                    selectedAccountId = acc.id
                                                    paymentAccount = acc.nameEn
                                                }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isBangla) pm.labelBn.substringBefore(" ") else pm.labelEn.substringBefore(" "),
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Cash/Bank Account Selection
                    item {
                        if (activeLiquidAccounts.isNotEmpty()) {
                            Text(if (isBangla) "নগদ / ব্যাংক / MFS হিসাব" else "Cash / Bank / MFS Account", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(activeLiquidAccounts, key = { it.id }) { acc ->
                                    val isSel = selectedAccountId == acc.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) SecondaryTeal else ERPSurfaceVariant)
                                            .border(1.dp, if (isSel) SecondaryTeal else ERPDivider, RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedAccountId = acc.id
                                                paymentAccount = if (isBangla && acc.nameBn.isNotBlank()) acc.nameBn else acc.nameEn
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isBangla && acc.nameBn.isNotBlank()) acc.nameBn else acc.nameEn,
                                            color = if (isSel) Color.White else TextPrimary,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        OutlinedTextField(
                            value = paymentAccount,
                            onValueChange = { paymentAccount = it },
                            label = { Text(if (isBangla) "পেমেন্ট অ্যাকাউন্ট / ব্যাংক" else "Payment Account / Bank") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = referenceNumber,
                            onValueChange = { referenceNumber = it },
                            label = { Text(if (isBangla) "রেফারেন্স / চেক / SWIFT নং" else "SWIFT / Bank Reference Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(if (isBangla) "নোট (ঐচ্ছিক)" else "Notes (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (errorMessage != null || saveError != null) {
                        item {
                            Text(saveError ?: errorMessage.orEmpty(), color = StatusError, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(if (isBangla) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        enabled = !isSaving,
                        onClick = {
                            try {
                                require(selectedSupplierId.isNotBlank()) {
                                    if (isBangla) "একজন সরবরাহকারী নির্বাচন করুন" else "Please select a supplier"
                                }
                                val rate = if (selectedCurrency == "BDT") BigDecimal.ONE else BigDecimal(exchangeRateStr.trim())
                                val amtBd = BigDecimal(amountStr.trim())
                                require(amtBd > BigDecimal.ZERO) { "Amount must be greater than zero" }
                                require(rate > BigDecimal.ZERO) { "Rate must be positive" }

                                val minor = amtBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()

                                if (onConfirmWithDetails != null) {
                                    onConfirmWithDetails(
                                        selectedSupplierId,
                                        selectedOrderId,
                                        minor,
                                        selectedCurrency,
                                        rate,
                                        selectedMethod,
                                        paymentAccount,
                                        selectedAccountId,
                                        selectedPaymentDateMillis,
                                        referenceNumber.ifBlank { null },
                                        notes.ifBlank { null }
                                    )
                                } else {
                                    onConfirm(
                                        selectedSupplierId,
                                        selectedOrderId,
                                        minor,
                                        selectedCurrency,
                                        rate,
                                        selectedMethod,
                                        paymentAccount,
                                        referenceNumber.ifBlank { null },
                                        notes.ifBlank { null }
                                    )
                                }
                                if (dismissOnSubmit) onDismiss()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Invalid payment parameters"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text(if (isBangla) "পেমেন্ট সংরক্ষণ করুন" else "Record & Post Payment")
                    }
                }
            }
        }
    }
}
