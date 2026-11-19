package com.pocketdev.advance_erp.feature.sales

import com.pocketdev.advance_erp.core.designsystem.StatusError
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.ReceiptLong
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.BangladeshBanks
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.AccountType
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.LiquidAccountKind
import com.pocketdev.advance_erp.data.entity.NormalBalance
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.domain.repository.CustomerPaymentDraft
import com.pocketdev.advance_erp.domain.repository.InvoiceAllocationDraft

private enum class PaymentChannelFilter(val titleEn: String, val titleBn: String) {
    ALL("All", "সকল"),
    CASH("Cash", "নগদ"),
    BANK("Bank", "ব্যাংক"),
    MFS("bKash / Nagad / MFS", "বিকাশ / নগদ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordCustomerPaymentDialog(
    customers: List<Customer>,
    invoices: List<SalesInvoice>,
    liquidAccounts: List<ChartOfAccount> = emptyList(),
    preselectedInvoice: SalesInvoice? = null,
    preselectedCustomerId: String? = null,
    nextPaymentNumber: String? = null,
    isBangla: Boolean = false,
    isSaving: Boolean = false,
    saveError: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (CustomerPaymentDraft) -> Unit
) {
    val availableAccounts = remember(liquidAccounts) {
        liquidAccounts.filter { it.isActive }.ifEmpty {
            listOf(
                ChartOfAccount("coa-1010", "1010", "Cash in Hand", "নগদ তহবিল", AccountType.ASSET.name, NormalBalance.DEBIT.name, accountSubType = LiquidAccountKind.CASH.name),
                ChartOfAccount("coa-1020", "1020", "City Bank Corporate (Import LC)", "সিটি ব্যাংক কর্পোরেট", AccountType.ASSET.name, NormalBalance.DEBIT.name, accountSubType = LiquidAccountKind.BANK.name, institutionName = "The City Bank PLC"),
                ChartOfAccount("coa-1030", "1030", "Islami Bank Bangladesh Ltd.", "ইসলামী ব্যাংক বাংলাদেশ", AccountType.ASSET.name, NormalBalance.DEBIT.name, accountSubType = LiquidAccountKind.BANK.name, institutionName = "Islami Bank Bangladesh PLC"),
                ChartOfAccount("coa-1060", "1060", "bKash Account", "বিকাশ অ্যাকাউন্ট", AccountType.ASSET.name, NormalBalance.DEBIT.name, accountSubType = LiquidAccountKind.MOBILE_BANKING.name, institutionName = "bKash"),
                ChartOfAccount("coa-1061", "1061", "Nagad Account", "নগদ অ্যাকাউন্ট", AccountType.ASSET.name, NormalBalance.DEBIT.name, accountSubType = LiquidAccountKind.MOBILE_BANKING.name, institutionName = "Nagad")
            )
        }
    }

    var selectedCustomerId by remember {
        mutableStateOf(preselectedInvoice?.customerId ?: preselectedCustomerId ?: customers.firstOrNull()?.id ?: "")
    }
    var customerExpanded by rememberSaveable { mutableStateOf(false) }

    var amountStr by remember {
        mutableStateOf(
            if (preselectedInvoice != null) {
                (preselectedInvoice.dueAmountMinor / 100.0).toString()
            } else {
                val cid = preselectedCustomerId ?: customers.firstOrNull()?.id
                val due = customers.find { it.id == cid }?.outstandingBalanceMinor ?: 0L
                if (due > 0L) (due / 100.0).toString() else "0.00"
            }
        )
    }
    var amountManuallyEdited by rememberSaveable { mutableStateOf(false) }

    var channelFilter by remember { mutableStateOf(PaymentChannelFilter.ALL) }
    var selectedAccountId by remember(availableAccounts) {
        mutableStateOf(availableAccounts.firstOrNull()?.id ?: "coa-1010")
    }
    var selectedAccount = availableAccounts.find { it.id == selectedAccountId } ?: availableAccounts.first()
    var isChequeForBank by rememberSaveable { mutableStateOf(false) }

    val selectedPaymentMethod = remember(selectedAccount, isChequeForBank) {
        if (selectedAccount.liquidKind == LiquidAccountKind.BANK && isChequeForBank) {
            "CHEQUE"
        } else {
            BangladeshBanks.inferPaymentMethod(selectedAccount)
        }
    }

    val filteredAccounts = remember(availableAccounts, channelFilter) {
        when (channelFilter) {
            PaymentChannelFilter.ALL -> availableAccounts
            PaymentChannelFilter.CASH -> availableAccounts.filter { it.liquidKind == LiquidAccountKind.CASH }
            PaymentChannelFilter.BANK -> availableAccounts.filter { it.liquidKind == LiquidAccountKind.BANK }
            PaymentChannelFilter.MFS -> availableAccounts.filter { it.liquidKind == LiquidAccountKind.MOBILE_BANKING }
        }
    }

    var referenceNote by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var allocationInvoiceId by remember(selectedCustomerId, preselectedInvoice?.id) {
        mutableStateOf(preselectedInvoice?.takeIf { it.customerId == selectedCustomerId }?.id)
    }
    var invoiceExpanded by rememberSaveable { mutableStateOf(false) }
    val allocationInvoice = invoices.firstOrNull { it.id == allocationInvoiceId && it.customerId == selectedCustomerId && it.isPosted && it.status != "CANCELLED" && it.dueAmountMinor > 0 }
    val selectedCustomer = customers.find { it.id == selectedCustomerId }
    val unpaidInvoicesForCustomer = remember(selectedCustomerId, invoices) {
        invoices.filter {
            it.customerId == selectedCustomerId &&
                it.isPosted &&
                it.dueAmountMinor > 0
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .widthIn(max = 640.dp)
                .clip(RoundedCornerShape(16.dp))
                .navigationBarsPadding()
                .imePadding(),
            color = ERPBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "গ্রাহকের পেমেন্ট গ্রহণ" else "Record Customer Payment",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isBangla) "বকেয়া বিল আদায় অথবা অগ্রিম জমা" else "Collect receivables or advance deposit",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Tracking / Receipt Number Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PrimaryPurple.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "পেমেন্ট ট্র্যাকিং / রশিদ নং:" else "Payment Tracking / Receipt #:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryPurple
                        ) {
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DeepNavy.copy(alpha = 0.1f))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Customer Selector
                    item {
                        Text(
                            text = if (isBangla) "গ্রাহক / CUSTOMER *" else "CUSTOMER *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = customerExpanded,
                            onExpandedChange = { customerExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCustomer?.let { "${it.name} (${it.phone})" } ?: "Select Customer",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = customerExpanded,
                                onDismissRequest = { customerExpanded = false }
                            ) {
                                customers.forEach { cust ->
                                    DropdownMenuItem(
                                        text = { Text("${cust.name} (Due: ৳${Formatters.formatMinorUnit(cust.outstandingBalanceMinor)})") },
                                        onClick = {
                                            selectedCustomerId = cust.id
                                            customerExpanded = false
                                            if (!amountManuallyEdited && preselectedInvoice == null) {
                                                amountStr = if (cust.outstandingBalanceMinor > 0L) {
                                                    (cust.outstandingBalanceMinor / 100.0).toString()
                                                } else "0.00"
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Amount
                    item {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = {
                                amountStr = it
                                amountManuallyEdited = true
                            },
                            label = { Text(if (isBangla) "আদায়কৃত টাকার পরিমাণ (৳) *" else "Amount Received (৳) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Receiving Channel & Account Picker
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBangla) "কোন হিসাবে জমা হবে (Cash / Bank / bKash / Nagad) *" else "RECEIVE INTO ACCOUNT (CASH / BANK / MFS) *",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(PaymentChannelFilter.entries) { filter ->
                                    val isSel = channelFilter == filter
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSel) PrimaryPurple else MaterialTheme.colorScheme.surface)
                                            .border(1.dp, if (isSel) PrimaryPurple else ERPDivider, RoundedCornerShape(16.dp))
                                            .clickable {
                                                channelFilter = filter
                                                val firstMatch = when (filter) {
                                                    PaymentChannelFilter.ALL -> availableAccounts.firstOrNull()
                                                    PaymentChannelFilter.CASH -> availableAccounts.firstOrNull { it.liquidKind == LiquidAccountKind.CASH }
                                                    PaymentChannelFilter.BANK -> availableAccounts.firstOrNull { it.liquidKind == LiquidAccountKind.BANK }
                                                    PaymentChannelFilter.MFS -> availableAccounts.firstOrNull { it.liquidKind == LiquidAccountKind.MOBILE_BANKING }
                                                }
                                                if (firstMatch != null && selectedAccount.id != firstMatch.id) {
                                                    selectedAccountId = firstMatch.id
                                                    isChequeForBank = false
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isBangla) filter.titleBn else filter.titleEn,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            if (filteredAccounts.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, ERPDivider),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isBangla) "এই ক্যাটাগরিতে কোনো সক্রিয় অ্যাকাউন্ট নেই। Accounts ট্যাব থেকে যোগ করুন।" else "No active accounts in this category. Add one from Accounts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    filteredAccounts.forEach { acc ->
                                        val isSel = acc.id == selectedAccount.id
                                        val methodTag = BangladeshBanks.inferPaymentMethod(acc)
                                        val accent = when (acc.liquidKind) {
                                            LiquidAccountKind.CASH -> DeepNavy
                                            LiquidAccountKind.BANK -> SecondaryTeal
                                            LiquidAccountKind.MOBILE_BANKING -> PrimaryPurple
                                            null -> DeepNavy
                                        }
                                        val icon = when (acc.liquidKind) {
                                            LiquidAccountKind.CASH -> Icons.Outlined.AccountBalanceWallet
                                            LiquidAccountKind.BANK -> Icons.Outlined.AccountBalance
                                            LiquidAccountKind.MOBILE_BANKING -> Icons.Outlined.PhoneAndroid
                                            null -> Icons.Outlined.AccountBalanceWallet
                                        }
                                        val subtitle = buildList {
                                            acc.institutionName?.takeIf { it !in acc.nameEn }?.let { add(it) }
                                            acc.branchName?.let { add(it) }
                                            acc.accountNumber?.let { add("A/C: $it") }
                                            add("GL ${acc.accountCode}")
                                        }.joinToString(" • ")

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    selectedAccountId = acc.id
                                                    if (acc.liquidKind != LiquidAccountKind.BANK) {
                                                        isChequeForBank = false
                                                    }
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(
                                                width = if (isSel) 1.5.dp else 1.dp,
                                                color = if (isSel) accent else ERPDivider
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .background(accent.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (isBangla && acc.nameBn.isNotBlank()) acc.nameBn else acc.nameEn,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (subtitle.isNotBlank()) {
                                                        Text(
                                                            text = subtitle,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextSecondary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(accent.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                                ) {
                                                    Text(methodTag, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                                if (isSel) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (selectedAccount.liquidKind == LiquidAccountKind.BANK) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isBangla) "ব্যাংক মোড:" else "Bank Mode:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    listOf(
                                        false to (if (isBangla) "ব্যাংক ট্রান্সফার / জমা" else "Bank Transfer / Deposit"),
                                        true to (if (isBangla) "চেক (Cheque)" else "Cheque")
                                    ).forEach { (chequeMode, label) ->
                                        val isSel = isChequeForBank == chequeMode
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) SecondaryTeal else MaterialTheme.colorScheme.surface)
                                                .border(1.dp, if (isSel) SecondaryTeal else ERPDivider, RoundedCornerShape(8.dp))
                                                .clickable { isChequeForBank = chequeMode }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = if (isBangla) {
                                    "পোস্ট হবে: $selectedPaymentMethod → ${selectedAccount.nameBn.ifBlank { selectedAccount.nameEn }}"
                                } else {
                                    "Entry posts to: $selectedPaymentMethod → ${selectedAccount.nameEn}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Reference Note
                    item {
                        OutlinedTextField(
                            value = referenceNote,
                            onValueChange = { referenceNote = it },
                            label = {
                                Text(
                                    when (selectedAccount.liquidKind) {
                                        LiquidAccountKind.MOBILE_BANKING ->
                                            if (isBangla) "TrxID / মোবাইল নম্বর (ঐচ্ছিক)" else "TrxID / Sender Mobile # (Optional)"
                                        LiquidAccountKind.BANK ->
                                            if (isBangla) "চেক নং / ব্যাংক স্লিপ / রেফারেন্স" else "Cheque # / Deposit Slip # / Ref"
                                        else ->
                                            if (isBangla) "মানি রিসিট / রেফারেন্স নোট" else "Money Receipt # / Reference"
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    item {
                        ExposedDropdownMenuBox(expanded=invoiceExpanded,onExpandedChange={invoiceExpanded=it}) {
                            OutlinedTextField(value=allocationInvoice?.invoiceNumber ?: if(isBangla) "সাধারণ আদায় / unallocated credit" else "Standalone / unallocated credit",
                                onValueChange={},readOnly=true, label={Text(if(isBangla) "ইনভয়েসে বরাদ্দ (ঐচ্ছিক)" else "Allocate to invoice (optional)")},
                                modifier=Modifier.menuAnchor().fillMaxWidth(),trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(invoiceExpanded)})
                            ExposedDropdownMenu(expanded=invoiceExpanded,onDismissRequest={invoiceExpanded=false}) {
                                DropdownMenuItem(text={Text(if(isBangla)"কোনো invoice-এ বরাদ্দ নয়" else "Do not allocate")},onClick={allocationInvoiceId=null;invoiceExpanded=false})
                                unpaidInvoicesForCustomer.filter { it.isPosted && it.status!="CANCELLED" }.forEach { invoice ->
                                    DropdownMenuItem(text={Text(invoice.invoiceNumber+" • ৳"+Formatters.formatMinorUnit(invoice.dueAmountMinor))},onClick={allocationInvoiceId=invoice.id;invoiceExpanded=false})
                                }
                            }
                        }
                    }
                    // Open Invoices preview
                    if (unpaidInvoicesForCustomer.isNotEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = DeepNavy.copy(alpha = 0.05f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (isBangla) "বকেয়া ইনভয়েসসমূহ (${unpaidInvoicesForCustomer.size}):" else "Open Invoices (${unpaidInvoicesForCustomer.size}):",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    unpaidInvoicesForCustomer.take(3).forEach { inv ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(inv.invoiceNumber, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            Text("Due: ৳" + Formatters.formatMinorUnit(inv.dueAmountMinor), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = StatusError)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isBangla) "শুধু নির্বাচিত invoice-এ বরাদ্দ হবে। না বাছলে আলাদা credit থাকবে; স্বয়ংক্রিয় FIFO নয়।" else "Only the selected invoice is allocated. Otherwise the receipt remains unallocated credit; no automatic FIFO.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    if (errorMessage != null || saveError != null) {
                        item {
                            Text(saveError ?: errorMessage.orEmpty(), color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DeepNavy.copy(alpha = 0.1f))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isBangla) "বাতিল" else "Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = !isSaving,
                        onClick = {
                            val amount = runCatching { Money.parseFromString(amountStr).minorUnits }.getOrDefault(0L)
                            if (amount <= 0) {
                                errorMessage = if (isBangla) "সঠিক টাকার পরিমাণ (> ০) দিন" else "Please enter a valid amount > 0"
                                return@Button
                            }
                            if (selectedCustomerId.isBlank()) {
                                errorMessage = if (isBangla) "একজন গ্রাহক নির্বাচন করুন" else "Please select a customer"
                                return@Button
                            }

                            if (allocationInvoiceId != null && allocationInvoice == null) {
                                errorMessage = if (isBangla) "নির্বাচিত invoice-এর বকেয়া বদলেছে। আবার নির্বাচন করুন।" else "The selected invoice due has changed. Select it again."
                                return@Button
                            }
                            val allocations = if (allocationInvoice != null) {
                                listOf(InvoiceAllocationDraft(allocationInvoice.id, minOf(amount, allocationInvoice.dueAmountMinor)))
                            } else emptyList()

                            onSubmit(
                                CustomerPaymentDraft(
                                    customerId = selectedCustomerId,
                                    amountMinor = amount,
                                    paymentMethod = selectedPaymentMethod,
                                    accountId = selectedAccount.id,
                                    paymentDate = System.currentTimeMillis(),
                                    referenceNote = referenceNote.ifBlank { null },
                                    allocations = allocations
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isBangla) "পেমেন্ট সংরক্ষণ করুন" else "Record & Post Payment")
                    }
                }
            }
        }
    }
}
