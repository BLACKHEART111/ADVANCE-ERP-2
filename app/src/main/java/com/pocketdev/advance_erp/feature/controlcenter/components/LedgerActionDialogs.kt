package com.pocketdev.advance_erp.feature.controlcenter.components

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.mutableStateListOf
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
import com.pocketdev.advance_erp.core.designsystem.StatusErrorContainer
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusSuccessContainer
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.ExpenseCategory
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerExpenseEntryDialog(
    accounts: List<ChartOfAccount>,
    isBangla: Boolean,
    isSaving: Boolean,
    saveError: String?,
    onDismiss: () -> Unit,
    onConfirm: (
        category: String,
        descEn: String,
        descBn: String,
        amountMinor: Long,
        currency: String,
        exchangeRate: String,
        paymentAccountId: String,
        expenseAccountId: String,
        voucherDate: Long,
        ref: String?,
        notes: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val categories = ExpenseCategory.entries
    var selectedCat by remember { mutableStateOf(categories.first()) }
    var descEn by rememberSaveable { mutableStateOf("") }
    var amountStr by rememberSaveable { mutableStateOf("") }
    var voucherDateMillis by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    val paymentAccounts = remember(accounts) {
        val active = accounts.filter { it.isActive }
        val liquid = active.filter { it.liquidKind != null }
        val others = active.filter { it.liquidKind == null && it.accountType in listOf("ASSET", "LIABILITY") }
        (liquid + others).ifEmpty {
            listOf(
                ChartOfAccount("coa-1010", "1010", "Cash in Hand", "নগদ তহবিল", "ASSET", "DEBIT"),
                ChartOfAccount("coa-1020", "1020", "Bank Account", "ব্যাংক অ্যাকাউন্ট", "ASSET", "DEBIT")
            )
        }
    }
    var paymentAccountId by remember(paymentAccounts) {
        mutableStateOf(paymentAccounts.find { it.accountCode == "1010" }?.id ?: paymentAccounts.first().id)
    }
    var accountMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var refNumber by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }
    fun openDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = voucherDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                voucherDateMillis = chosen.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(18.dp))
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
                            text = if (isBangla) "ব্যয় এন্ট্রি (Expense Entry)" else "Record Business Expense",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isBangla) "ক্যাটাগরি, পরিমাণ ও পেমেন্ট পদ্ধতি নির্বাচন করুন" else "Posts expense voucher & double-entry ledger",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = ERPDivider)

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = if (isBangla) "ব্যয়ের খাত (Category) *" else "Expense Category *",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categories) { cat ->
                                val isSel = selectedCat == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) PrimaryPurple else ERPSurfaceVariant)
                                        .clickable { selectedCat = cat }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = if (isBangla) cat.titleBn else cat.titleEn,
                                        color = if (isSel) Color.White else TextPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = descEn,
                            onValueChange = { descEn = it },
                            label = { Text(if (isBangla) "বিবরণ (Description) *" else "Description *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text(if (isBangla) "টাকার পরিমাণ (৳) *" else "Amount (BDT ৳) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

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
                                    Text(if (isBangla) "ব্যয়ের তারিখ" else "Expense Date", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                }
                                Text(
                                    text = if (isBangla) Formatters.toBanglaDigits(dateFormat.format(Date(voucherDateMillis))) else dateFormat.format(Date(voucherDateMillis)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    item {
                        val selectedAcc = paymentAccounts.find { it.id == paymentAccountId }
                        ExposedDropdownMenuBox(
                            expanded = accountMenuExpanded,
                            onExpandedChange = { accountMenuExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedAcc?.let { "${it.accountCode} • ${if (isBangla && it.nameBn.isNotBlank()) it.nameBn else it.nameEn}" }
                                    ?: if (isBangla) "পেমেন্ট হিসাব নির্বাচন করুন" else "Select Payment Account",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (isBangla) "পেমেন্ট মাধ্যম / হিসাব *" else "Payment Account / Method *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = accountMenuExpanded,
                                onDismissRequest = { accountMenuExpanded = false }
                            ) {
                                paymentAccounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text("${acc.accountCode} • ${if (isBangla && acc.nameBn.isNotBlank()) acc.nameBn else acc.nameEn}") },
                                        onClick = {
                                            paymentAccountId = acc.id
                                            accountMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = refNumber,
                            onValueChange = { refNumber = it },
                            label = { Text(if (isBangla) "রশিদ / ভাউচার রেফারেন্স (ঐচ্ছিক)" else "Receipt / Ref # (Optional)") },
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
                            maxLines = 2,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (errorMessage != null || saveError != null) {
                        item {
                            Text(saveError ?: errorMessage.orEmpty(), color = StatusError, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(if (isBangla) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = !isSaving,
                        onClick = {
                            try {
                                val amtBd = BigDecimal(amountStr.trim())
                                require(amtBd > BigDecimal.ZERO) {
                                    if (isBangla) "সঠিক পরিমাণ (> ০) দিন" else "Amount must be positive"
                                }
                                require(descEn.isNotBlank()) {
                                    if (isBangla) "ব্যয়ের বিবরণ লিখুন" else "Description is required"
                                }
                                val minor = amtBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                                onConfirm(
                                    selectedCat.name,
                                    descEn.trim(),
                                    descEn.trim(),
                                    minor,
                                    "BDT",
                                    "1.0000",
                                    paymentAccountId,
                                    selectedCat.defaultAccountId,
                                    voucherDateMillis,
                                    refNumber.trim().ifBlank { null },
                                    notes.trim().ifBlank { null }
                                )
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Invalid expense details"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text(if (isBangla) "ব্যয় সংরক্ষণ করুন" else "Post Expense")
                    }
                }
            }
        }
    }
}

private data class EditableJournalLine(
    val accountId: String,
    val isDebit: Boolean,
    val amountText: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerJournalEntryDialog(
    accounts: List<ChartOfAccount>,
    isBangla: Boolean,
    isSaving: Boolean,
    saveError: String?,
    onDismiss: () -> Unit,
    onConfirm: (entryDate: Long, descEn: String, descBn: String, lines: List<JournalLineDraft>) -> Unit
) {
    val context = LocalContext.current
    val activeAccounts = remember(accounts) { accounts.filter { it.isActive } }
    var entryDateMillis by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var descEn by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val lines = remember(activeAccounts) {
        mutableStateListOf(
            EditableJournalLine(activeAccounts.firstOrNull()?.id ?: "", isDebit = true, amountText = ""),
            EditableJournalLine(activeAccounts.getOrNull(1)?.id ?: activeAccounts.firstOrNull()?.id ?: "", isDebit = false, amountText = "")
        )
    }

    val parsedLines = lines.map { line ->
        val minor = runCatching {
            val bd = BigDecimal(line.amountText.trim())
            if (bd > BigDecimal.ZERO) bd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact() else 0L
        }.getOrDefault(0L)
        line to minor
    }
    val totalDebitMinor = parsedLines.filter { it.first.isDebit }.sumOf { it.second }
    val totalCreditMinor = parsedLines.filter { !it.first.isDebit }.sumOf { it.second }
    val isBalanced = totalDebitMinor > 0L && totalDebitMinor == totalCreditMinor &&
        parsedLines.all { it.first.accountId.isNotBlank() && it.second > 0L }

    val lang = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    fun openDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = entryDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                entryDateMillis = chosen.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(18.dp))
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
                            text = if (isBangla) "সমন্বয় জার্নাল এন্ট্রি" else "Adjustment Journal Entry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isBangla) "সুষম ডেবিট ও ক্রেডিট দাখিলা (অডিট ট্রেইল সংরক্ষিত)" else "Balanced double-entry adjustment with audit trail",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = ERPDivider)

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                    Text(if (isBangla) "জার্নাল তারিখ" else "Entry Date", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                }
                                Text(
                                    text = if (isBangla) Formatters.toBanglaDigits(dateFormat.format(Date(entryDateMillis))) else dateFormat.format(Date(entryDateMillis)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = descEn,
                            onValueChange = { descEn = it },
                            label = { Text(if (isBangla) "জার্নাল বিবরণ / কারণ *" else "Journal Description / Reason *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    itemsIndexed(lines) { index, line ->
                        var expanded by remember { mutableStateOf(false) }
                        val selectedAcc = activeAccounts.find { it.id == line.accountId }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ERPSurfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, ERPDivider),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(true to "Dr (ডেবিট)", false to "Cr (ক্রেডিট)").forEach { (isDr, label) ->
                                            val sel = line.isDebit == isDr
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (sel) (if (isDr) PrimaryPurple else SecondaryTeal) else MaterialTheme.colorScheme.surface)
                                                    .clickable { lines[index] = line.copy(isDebit = isDr) }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (sel) Color.White else TextPrimary
                                                )
                                            }
                                        }
                                    }
                                    if (lines.size > 2) {
                                        IconButton(
                                            onClick = { lines.removeAt(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove line", tint = StatusError, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedAcc?.let { "${it.accountCode} • ${if (isBangla && it.nameBn.isNotBlank()) it.nameBn else it.nameEn}" }
                                            ?: if (isBangla) "হিসাব নির্বাচন করুন" else "Select Account",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        activeAccounts.forEach { acc ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "${acc.accountCode} • ${if (isBangla && acc.nameBn.isNotBlank()) acc.nameBn else acc.nameEn}",
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                onClick = {
                                                    lines[index] = line.copy(accountId = acc.id)
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = line.amountText,
                                    onValueChange = { lines[index] = line.copy(amountText = it) },
                                    label = { Text(if (isBangla) "টাকার পরিমাণ (৳)" else "Amount (BDT ৳)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = {
                                lines.add(
                                    EditableJournalLine(
                                        accountId = activeAccounts.firstOrNull()?.id ?: "",
                                        isDebit = totalDebitMinor < totalCreditMinor,
                                        amountText = ""
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBangla) "আরও একটি সারি যোগ করুন" else "Add Journal Line", fontSize = 12.sp)
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isBalanced) StatusSuccessContainer else StatusErrorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${if (isBangla) "মোট ডেবিট:" else "Total Debit:"} ${Formatters.formatMoney(totalDebitMinor, CurrencyCode.BDT, lang)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${if (isBangla) "মোট ক্রেডিট:" else "Total Credit:"} ${Formatters.formatMoney(totalCreditMinor, CurrencyCode.BDT, lang)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (isBalanced) (if (isBangla) "সুষম ✓" else "Balanced ✓") else (if (isBangla) "অসমান ✕" else "Unbalanced ✕"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isBalanced) StatusSuccess else StatusError
                                )
                            }
                        }
                    }

                    if (errorMessage != null || saveError != null) {
                        item {
                            Text(saveError ?: errorMessage.orEmpty(), color = StatusError, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(if (isBangla) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = !isSaving && isBalanced,
                        onClick = {
                            if (descEn.isBlank()) {
                                errorMessage = if (isBangla) "জার্নাল বিবরণ আবশ্যক" else "Journal description is required"
                                return@Button
                            }
                            if (!isBalanced) {
                                errorMessage = if (isBangla) "ডেবিট ও ক্রেডিট সমান হতে হবে" else "Debit and Credit must be balanced"
                                return@Button
                            }
                            val draftLines = parsedLines.map { (line, minor) ->
                                JournalLineDraft(
                                    accountId = line.accountId,
                                    isDebit = line.isDebit,
                                    amountMinor = minor,
                                    description = descEn.trim()
                                )
                            }
                            onConfirm(entryDateMillis, descEn.trim(), descEn.trim(), draftLines)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text(if (isBangla) "জার্নাল পোস্ট করুন" else "Post Entry")
                    }
                }
            }
        }
    }
}
