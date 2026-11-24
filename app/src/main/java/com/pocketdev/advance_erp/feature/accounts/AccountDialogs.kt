package com.pocketdev.advance_erp.feature.accounts

import androidx.compose.runtime.saveable.rememberSaveable
import com.pocketdev.advance_erp.core.designsystem.ERPSurfaceVariant
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.WarningAmber
import com.pocketdev.advance_erp.core.designsystem.StatusErrorContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.pocketdev.advance_erp.core.designsystem.ERPSearchBar
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.BangladeshBankInfo
import com.pocketdev.advance_erp.core.model.BangladeshBanks
import com.pocketdev.advance_erp.core.model.BankCategory
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.AccountType
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.LiquidAccountKind
import com.pocketdev.advance_erp.data.entity.NormalBalance
import com.pocketdev.advance_erp.data.entity.TrialBalanceItem
import com.pocketdev.advance_erp.data.repository.AccountingRepositoryImpl
import java.math.BigDecimal
import java.math.RoundingMode

private enum class AccountCreateCategory {
    MONEY_ACCOUNT,
    GENERAL_LEDGER
}

/**
 * Unified dialog for creating a Cash/Bank/MFS account (with full Bangladesh banks list),
 * creating a General Ledger account, or editing an existing [ChartOfAccount].
 */
@Composable
fun CreateOrEditAccountDialog(
    editingAccount: ChartOfAccount? = null,
    initialLiquidKind: LiquidAccountKind? = LiquidAccountKind.BANK,
    initialGeneralMode: Boolean = false,
    isBangla: Boolean,
    suggestCode: suspend (AccountType, LiquidAccountKind?) -> String,
    onDismiss: () -> Unit,
    onCreateLiquidAccount: (
        kind: LiquidAccountKind,
        nameEn: String,
        nameBn: String,
        institutionName: String?,
        branchName: String?,
        accountNumber: String?,
        description: String?,
        openingBalanceMinor: Long
    ) -> Unit,
    onCreateGeneralAccount: (
        code: String,
        nameEn: String,
        nameBn: String,
        type: AccountType,
        normalBalance: NormalBalance,
        description: String?
    ) -> Unit,
    onUpdateAccount: (ChartOfAccount) -> Unit
) {
    val isEditing = editingAccount != null
    val initialCategory = when {
        isEditing && editingAccount?.liquidKind != null -> AccountCreateCategory.MONEY_ACCOUNT
        isEditing -> AccountCreateCategory.GENERAL_LEDGER
        initialGeneralMode -> AccountCreateCategory.GENERAL_LEDGER
        else -> AccountCreateCategory.MONEY_ACCOUNT
    }

    var category by remember { mutableStateOf(initialCategory) }
    var liquidKind by remember {
        mutableStateOf(editingAccount?.liquidKind ?: initialLiquidKind ?: LiquidAccountKind.BANK)
    }

    // Bank / MFS fields
    var selectedBank by remember {
        mutableStateOf(
            BangladeshBanks.allBanks.firstOrNull {
                it.nameEn.equals(editingAccount?.institutionName, ignoreCase = true)
            }
        )
    }
    var customInstitutionName by remember { mutableStateOf(editingAccount?.institutionName ?: "") }
    var useCustomBank by remember {
        mutableStateOf(
            editingAccount != null &&
                editingAccount.liquidKind == LiquidAccountKind.BANK &&
                !editingAccount.institutionName.isNullOrBlank() &&
                selectedBank == null
        )
    }
    var showBankPicker by rememberSaveable { mutableStateOf(false) }

    var selectedMfs by remember {
        mutableStateOf(
            BangladeshBanks.mfsProviders.firstOrNull {
                it.nameEn.equals(editingAccount?.institutionName, ignoreCase = true)
            } ?: BangladeshBanks.mfsProviders.first()
        )
    }

    var branchName by remember { mutableStateOf(editingAccount?.branchName ?: "") }
    var accountNumber by remember { mutableStateOf(editingAccount?.accountNumber ?: "") }
    var openingBalanceStr by rememberSaveable { mutableStateOf("") }

    // Common / GL fields
    var accountCode by remember { mutableStateOf(editingAccount?.accountCode ?: "") }
    var nameEn by remember { mutableStateOf(editingAccount?.nameEn ?: "") }
    var nameBn by remember { mutableStateOf(editingAccount?.nameBn ?: "") }
    var nameAutoSynced by remember { mutableStateOf(!isEditing) }
    var description by remember { mutableStateOf(editingAccount?.description ?: "") }
    var isActive by remember { mutableStateOf(editingAccount?.isActive ?: true) }

    var accountType by remember {
        mutableStateOf(
            AccountType.entries.firstOrNull { it.name == editingAccount?.accountType }
                ?: AccountType.EXPENSE
        )
    }
    var normalBalance by remember {
        mutableStateOf(
            NormalBalance.entries.firstOrNull { it.name == editingAccount?.normalBalance }
                ?: NormalBalance.DEBIT
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Auto-suggest next free account code whenever category / type / kind changes in create mode
    LaunchedEffect(category, liquidKind, accountType) {
        if (!isEditing) {
            accountCode = if (category == AccountCreateCategory.MONEY_ACCOUNT) {
                suggestCode(AccountType.ASSET, liquidKind)
            } else {
                suggestCode(accountType, null)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 580.dp)
                .clip(RoundedCornerShape(18.dp))
                .navigationBarsPadding()
                .imePadding(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isEditing -> if (isBangla) "অ্যাকাউন্ট সম্পাদনা" else "Edit Account"
                                category == AccountCreateCategory.MONEY_ACCOUNT ->
                                    if (isBangla) "নতুন ব্যাংক / নগদ / MFS হিসাব" else "New Bank / Cash / MFS Account"
                                else -> if (isBangla) "নতুন লেজার অ্যাকাউন্ট" else "New Ledger Account"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isEditing) {
                                "${editingAccount?.accountCode} • ${editingAccount?.accountType}"
                            } else {
                                if (isBangla) "চার্ট অব অ্যাকাউন্টসে নতুন হিসাব যুক্ত করুন" else "Add account to Chart of Accounts"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = ERPDivider)

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isEditing) {
                        // Mode switcher: Cash/Bank/MFS vs General Ledger Account
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ERPBackground)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val moneySel = category == AccountCreateCategory.MONEY_ACCOUNT
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (moneySel) PrimaryPurple else Color.Transparent)
                                    .clickable {
                                        category = AccountCreateCategory.MONEY_ACCOUNT
                                        errorMessage = null
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBangla) "ব্যাংক / নগদ / বিকাশ" else "Bank / Cash / MFS",
                                    color = if (moneySel) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            val glSel = category == AccountCreateCategory.GENERAL_LEDGER
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (glSel) PrimaryPurple else Color.Transparent)
                                    .clickable {
                                        category = AccountCreateCategory.GENERAL_LEDGER
                                        errorMessage = null
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBangla) "সাধারণ লেজার (GL)" else "General Ledger",
                                    color = if (glSel) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (category == AccountCreateCategory.MONEY_ACCOUNT) {
                        if (!isEditing) {
                            Text(
                                text = if (isBangla) "হিসাবের ধরন:" else "Account Channel:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple(LiquidAccountKind.BANK, if (isBangla) "ব্যাংক" else "Bank", Icons.Outlined.AccountBalance),
                                    Triple(LiquidAccountKind.CASH, if (isBangla) "নগদ" else "Cash", Icons.Outlined.AccountBalanceWallet),
                                    Triple(LiquidAccountKind.MOBILE_BANKING, if (isBangla) "বিকাশ/নগদ" else "MFS", Icons.Outlined.PhoneAndroid)
                                ).forEach { (kindOption, label, icon) ->
                                    val isSel = liquidKind == kindOption
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                liquidKind = kindOption
                                                errorMessage = null
                                                if (kindOption == LiquidAccountKind.MOBILE_BANKING && nameAutoSynced) {
                                                    nameEn = "${selectedMfs.nameEn} Account"
                                                    nameBn = "${selectedMfs.nameBn} অ্যাকাউন্ট"
                                                } else if (kindOption == LiquidAccountKind.BANK && nameAutoSynced) {
                                                    nameEn = selectedBank?.nameEn ?: ""
                                                    nameBn = selectedBank?.nameBn ?: ""
                                                } else if (kindOption == LiquidAccountKind.CASH && nameAutoSynced) {
                                                    nameEn = ""
                                                    nameBn = ""
                                                }
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) SecondaryTeal else ERPBackground,
                                        border = if (isSel) null else BorderStroke(1.dp, ERPDivider)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSel) Color.White else SecondaryTeal,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = label,
                                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // BANK SPECIFIC PICKER (ALL BANGLADESH BANKS)
                        if (liquidKind == LiquidAccountKind.BANK) {
                            Text(
                                text = if (isBangla) "বাংলাদেশের ব্যাংক নির্বাচন করুন *" else "Select Bangladesh Bank *",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showBankPicker = true },
                                shape = RoundedCornerShape(10.dp),
                                color = ERPBackground,
                                border = BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountBalance,
                                            contentDescription = null,
                                            tint = SecondaryTeal,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = when {
                                                    useCustomBank -> if (isBangla) "অন্যান্য / কাস্টম ব্যাংক" else "Custom / Other Bank"
                                                    selectedBank != null -> if (isBangla) selectedBank!!.nameBn else selectedBank!!.nameEn
                                                    else -> if (isBangla) "ব্যাংক তালিকা থেকে নির্বাচন করুন (৬৫টি ব্যাংক)" else "Tap to choose from BD Banks list (65 banks)"
                                                },
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (selectedBank != null || useCustomBank) TextPrimary else TextMuted,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (selectedBank != null && !useCustomBank) {
                                                Text(
                                                    text = "${selectedBank!!.category.titleEn} • ${selectedBank!!.shortName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    }
                                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Pick bank", tint = TextSecondary)
                                }
                            }

                            if (useCustomBank) {
                                OutlinedTextField(
                                    value = customInstitutionName,
                                    onValueChange = {
                                        customInstitutionName = it
                                        if (nameAutoSynced) {
                                            nameEn = it
                                            nameBn = it
                                        }
                                    },
                                    label = { Text(if (isBangla) "ব্যাংকের নাম *" else "Custom Bank Name *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = branchName,
                                    onValueChange = { branchName = it },
                                    label = { Text(if (isBangla) "শাখা (Branch)" else "Branch Name") },
                                    placeholder = { Text("e.g. Gulshan") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = accountNumber,
                                    onValueChange = { accountNumber = it },
                                    label = { Text(if (isBangla) "অ্যাকাউন্ট নং" else "Account No.") },
                                    placeholder = { Text("A/C Number") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        // MOBILE BANKING (MFS) PICKER
                        if (liquidKind == LiquidAccountKind.MOBILE_BANKING) {
                            Text(
                                text = if (isBangla) "মোবাইল ব্যাংকিং সেবা নির্বাচন করুন *" else "Select Mobile Banking Provider *",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(BangladeshBanks.mfsProviders) { provider ->
                                    val isSel = selectedMfs.code == provider.code
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) PrimaryPurple else ERPBackground)
                                            .border(
                                                1.dp,
                                                if (isSel) PrimaryPurple else ERPDivider,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedMfs = provider
                                                if (nameAutoSynced) {
                                                    val numSuffix = accountNumber.trim().takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                                                    nameEn = "${provider.nameEn} Account$numSuffix"
                                                    nameBn = "${provider.nameBn} অ্যাকাউন্ট$numSuffix"
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = if (isBangla) provider.nameBn else provider.nameEn,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = accountNumber,
                                onValueChange = {
                                    accountNumber = it
                                    if (nameAutoSynced) {
                                        val numSuffix = it.trim().takeIf { n -> n.isNotBlank() }?.let { n -> " ($n)" } ?: ""
                                        nameEn = "${selectedMfs.nameEn} Account$numSuffix"
                                        nameBn = "${selectedMfs.nameBn} অ্যাকাউন্ট$numSuffix"
                                    }
                                },
                                label = { Text(if (isBangla) "ওয়ালেট / মার্চেন্ট নম্বর" else "Wallet / Merchant Number") },
                                placeholder = { Text("01XXXXXXXXX") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        // Account display name + Bangla name
                        OutlinedTextField(
                            value = nameEn,
                            onValueChange = {
                                nameEn = it
                                nameAutoSynced = false
                            },
                            label = {
                                Text(
                                    when (liquidKind) {
                                        LiquidAccountKind.CASH -> if (isBangla) "নগদ হিসাবের নাম (English) *" else "Cash Account Name *"
                                        LiquidAccountKind.BANK -> if (isBangla) "হিসাবের শিরোনাম (English) *" else "Account Display Name *"
                                        LiquidAccountKind.MOBILE_BANKING -> if (isBangla) "অ্যাকাউন্ট নাম (English) *" else "Wallet Account Name *"
                                    }
                                )
                            },
                            placeholder = {
                                Text(
                                    when (liquidKind) {
                                        LiquidAccountKind.CASH -> "e.g. Uttara Showroom Cash"
                                        LiquidAccountKind.BANK -> "e.g. Dutch-Bangla Bank - Gulshan"
                                        LiquidAccountKind.MOBILE_BANKING -> "e.g. bKash Merchant (017...)"
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = nameBn,
                            onValueChange = {
                                nameBn = it
                                nameAutoSynced = false
                            },
                            label = { Text(if (isBangla) "বাংলা নাম (ঐচ্ছিক)" else "Bangla Name (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (!isEditing) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = openingBalanceStr,
                                    onValueChange = { openingBalanceStr = it },
                                    label = { Text(if (isBangla) "প্রারম্ভিক জের (৳)" else "Opening Balance (৳)") },
                                    placeholder = { Text("0.00") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1.4f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = accountCode,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(if (isBangla) "কোড" else "GL Code") },
                                    modifier = Modifier.weight(0.8f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    } else {
                        // GENERAL LEDGER ACCOUNT
                        if (!isEditing) {
                            Text(
                                text = if (isBangla) "অ্যাকাউন্টের ধরন (Type) *" else "Account Type *",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(AccountType.entries) { t ->
                                    val isSel = accountType == t
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) PrimaryPurple else ERPBackground)
                                            .border(
                                                1.dp,
                                                if (isSel) PrimaryPurple else ERPDivider,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                accountType = t
                                                normalBalance = if (t == AccountType.ASSET || t == AccountType.EXPENSE) {
                                                    NormalBalance.DEBIT
                                                } else {
                                                    NormalBalance.CREDIT
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = if (isBangla) "${t.titleBn} (${t.titleEn})" else t.titleEn,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = accountCode,
                                    onValueChange = { ch -> accountCode = ch.filter { it.isDigit() } },
                                    label = { Text(if (isBangla) "অ্যাকাউন্ট কোড *" else "Account Code *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(
                                        text = if (isBangla) "স্বাভাবিক ব্যালেন্স:" else "Normal Balance:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        NormalBalance.entries.forEach { nb ->
                                            val isSel = normalBalance == nb
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSel) SecondaryTeal else ERPBackground)
                                                    .clickable { normalBalance = nb }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = nb.name,
                                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = nameEn,
                            onValueChange = { nameEn = it },
                            label = { Text(if (isBangla) "হিসাবের নাম (English) *" else "Account Name (English) *") },
                            placeholder = { Text("e.g. Delivery & Courier Expense") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = nameBn,
                            onValueChange = { nameBn = it },
                            label = { Text(if (isBangla) "বাংলা নাম (ঐচ্ছিক)" else "Bangla Name (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(if (isBangla) "বিবরণ / নোট" else "Description / Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (isEditing && editingAccount != null) {
                        val isProtected = editingAccount.id in AccountingRepositoryImpl.PROTECTED_ACCOUNT_IDS
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ERPBackground,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    if (isProtected) {
                                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Column {
                                        Text(
                                            text = if (isBangla) "সক্রিয় অবস্থা (Active)" else "Account Active",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (isProtected) {
                                                if (isBangla) "সিস্টেম অ্যাকাউন্ট নিষ্ক্রিয় করা যাবে না" else "System account must stay active"
                                            } else {
                                                if (isBangla) "নিষ্ক্রিয় করলে পেমেন্ট তালিকায় দেখাবে না" else "Inactive accounts are hidden from payment dropdowns"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                }
                                Switch(
                                    checked = isActive,
                                    onCheckedChange = { if (!isProtected) isActive = it },
                                    enabled = !isProtected
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(errorMessage!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(if (isBangla) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            errorMessage = null
                            if (isEditing && editingAccount != null) {
                                if (nameEn.isBlank()) {
                                    errorMessage = if (isBangla) "হিসাবের নাম দিন" else "Account name is required"
                                    return@Button
                                }
                                val instName = when (editingAccount.liquidKind) {
                                    LiquidAccountKind.BANK -> if (useCustomBank) customInstitutionName.trim().ifBlank { null } else selectedBank?.nameEn ?: editingAccount.institutionName
                                    LiquidAccountKind.MOBILE_BANKING -> selectedMfs.nameEn
                                    else -> editingAccount.institutionName
                                }
                                onUpdateAccount(
                                    editingAccount.copy(
                                        nameEn = nameEn.trim(),
                                        nameBn = nameBn.trim().ifBlank { nameEn.trim() },
                                        description = description.trim().ifBlank { null },
                                        isActive = isActive,
                                        institutionName = instName,
                                        branchName = branchName.trim().ifBlank { null },
                                        accountNumber = accountNumber.trim().ifBlank { null }
                                    )
                                )
                                return@Button
                            }

                            if (category == AccountCreateCategory.MONEY_ACCOUNT) {
                                val instName: String? = when (liquidKind) {
                                    LiquidAccountKind.CASH -> null
                                    LiquidAccountKind.BANK -> {
                                        if (useCustomBank) customInstitutionName.trim() else selectedBank?.nameEn
                                    }
                                    LiquidAccountKind.MOBILE_BANKING -> selectedMfs.nameEn
                                }
                                if (liquidKind == LiquidAccountKind.BANK && instName.isNullOrBlank()) {
                                    errorMessage = if (isBangla) "একটি ব্যাংক নির্বাচন করুন" else "Please select a Bangladesh bank"
                                    return@Button
                                }
                                val resolvedNameEn = nameEn.trim().ifBlank { instName ?: "" }
                                if (resolvedNameEn.isBlank()) {
                                    errorMessage = if (isBangla) "অ্যাকাউন্টের নাম দিন" else "Account name is required"
                                    return@Button
                                }
                                val openingMinor = try {
                                    if (openingBalanceStr.isBlank()) 0L
                                    else {
                                        val bd = BigDecimal(openingBalanceStr.trim())
                                        require(bd >= BigDecimal.ZERO) { "Opening balance cannot be negative" }
                                        bd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Invalid opening balance"
                                    return@Button
                                }

                                onCreateLiquidAccount(
                                    liquidKind,
                                    resolvedNameEn,
                                    nameBn.trim().ifBlank { resolvedNameEn },
                                    instName,
                                    branchName.trim().ifBlank { null },
                                    accountNumber.trim().ifBlank { null },
                                    description.trim().ifBlank { null },
                                    openingMinor
                                )
                            } else {
                                if (accountCode.isBlank()) {
                                    errorMessage = if (isBangla) "অ্যাকাউন্ট কোড দিন" else "Account code is required"
                                    return@Button
                                }
                                if (nameEn.isBlank()) {
                                    errorMessage = if (isBangla) "অ্যাকাউন্টের নাম দিন" else "Account name is required"
                                    return@Button
                                }
                                onCreateGeneralAccount(
                                    accountCode.trim(),
                                    nameEn.trim(),
                                    nameBn.trim().ifBlank { nameEn.trim() },
                                    accountType,
                                    normalBalance,
                                    description.trim().ifBlank { null }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text(
                            when {
                                isEditing -> if (isBangla) "সংরক্ষণ করুন" else "Save Changes"
                                else -> if (isBangla) "হিসাব তৈরি করুন" else "Create Account"
                            }
                        )
                    }
                }
            }
        }
    }

    if (showBankPicker) {
        BangladeshBankPickerDialog(
            selectedBank = selectedBank,
            isBangla = isBangla,
            onDismiss = { showBankPicker = false },
            onSelectBank = { bank ->
                selectedBank = bank
                useCustomBank = false
                if (nameAutoSynced) {
                    val branchSuffix = branchName.trim().takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                    nameEn = "${bank.nameEn}$branchSuffix"
                    nameBn = "${bank.nameBn}$branchSuffix"
                }
                showBankPicker = false
            },
            onSelectCustom = {
                selectedBank = null
                useCustomBank = true
                showBankPicker = false
            }
        )
    }
}

/**
 * Searchable dialog displaying all scheduled & major domestic banks of Bangladesh,
 * categorized by BankCategory, plus a custom-bank fallback option.
 */
@Composable
fun BangladeshBankPickerDialog(
    selectedBank: BangladeshBankInfo?,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSelectBank: (BangladeshBankInfo) -> Unit,
    onSelectCustom: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<BankCategory?>(null) }

    val filteredBanks = remember(query, selectedCategory) {
        val q = query.trim().lowercase()
        BangladeshBanks.allBanks.filter { bank ->
            val matchesCat = selectedCategory == null || bank.category == selectedCategory
            val matchesQuery = q.isBlank() ||
                bank.nameEn.lowercase().contains(q) ||
                bank.nameBn.contains(q) ||
                bank.shortName.lowercase().contains(q) ||
                bank.code.lowercase().contains(q)
            matchesCat && matchesQuery
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.86f)
                .widthIn(max = 580.dp)
                .clip(RoundedCornerShape(18.dp)),
            color = ERPBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "বাংলাদেশের সকল ব্যাংক (${BangladeshBanks.allBanks.size})" else "Bangladesh Banks Directory (${BangladeshBanks.allBanks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isBangla) "বাংলাদেশ ব্যাংক তফসিলি ও অন্যান্য ব্যাংক" else "Bangladesh Bank Scheduled & Domestic Banks",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ERPSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = if (isBangla) "ব্যাংক খুঁজুন (যেমন: DBBL, BRAC, সোনালী, ইসলামী)..." else "Search bank (e.g. DBBL, BRAC, City, Sonali)..."
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        val isAll = selectedCategory == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isAll) PrimaryPurple else MaterialTheme.colorScheme.surface)
                                .border(1.dp, if (isAll) PrimaryPurple else ERPDivider, RoundedCornerShape(16.dp))
                                .clickable { selectedCategory = null }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isBangla) "সকল (${BangladeshBanks.allBanks.size})" else "All (${BangladeshBanks.allBanks.size})",
                                color = if (isAll) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    items(BankCategory.entries) { cat ->
                        val isSel = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSel) SecondaryTeal else MaterialTheme.colorScheme.surface)
                                .border(1.dp, if (isSel) SecondaryTeal else ERPDivider, RoundedCornerShape(16.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isBangla) cat.titleBn else cat.titleEn,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(filteredBanks, key = { it.code }) { bank ->
                        val isSel = selectedBank?.code == bank.code
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectBank(bank) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) SecondaryTeal.copy(alpha = 0.12f) else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (isSel) SecondaryTeal else ERPDivider
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(SecondaryTeal.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = bank.code.take(4),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = SecondaryTeal
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBangla) bank.nameBn else bank.nameEn,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isBangla) bank.nameEn else bank.nameBn,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Box(
                                        modifier = Modifier
                                            .background(ERPSurfaceVariant, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = bank.category.titleEn.substringBefore(" "),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    if (isSel) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Icon(Icons.Outlined.Check, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectCustom() },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBangla) "+ অন্যান্য / কাস্টম ব্যাংক যোগ করুন" else "+ Other / Custom Bank Name",
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (isBangla) "তালিকায় না থাকলে নিজে লিখুন" else "Enter bank name manually if not listed",
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
    }
}

/**
 * Clean account inspection modal showing balance, bank details, and edit trigger.
 */
@Composable
fun AccountDetailDialog(
    account: ChartOfAccount,
    trialItem: TrialBalanceItem?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDeleteAccount: ((onError: (String) -> Unit) -> Unit)? = null,
    onDepositCapital: (() -> Unit)? = null,
    onPayAccruedFreight: (() -> Unit)? = null,
    onWithdrawOrProfit: (() -> Unit)? = null
) {
    val isBangla = language == AppLanguage.BANGLA
    val netBalanceMinor = if (account.normalBalance == NormalBalance.DEBIT.name) {
        (trialItem?.totalDebitMinor ?: 0L) - (trialItem?.totalCreditMinor ?: 0L)
    } else {
        (trialItem?.totalCreditMinor ?: 0L) - (trialItem?.totalDebitMinor ?: 0L)
    }
    val canDeleteAccount = onDeleteAccount != null &&
        account.isLiquid &&
        account.id != AccountingRepositoryImpl.MAIN_CASH_ACCOUNT_ID &&
        account.id !in AccountingRepositoryImpl.PROTECTED_ACCOUNT_IDS

    var deleteErrorBanner by remember(account.id) { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember(account.id) { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(18.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .background(DeepNavy, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(account.accountCode, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBangla && account.nameBn.isNotBlank()) account.nameBn else account.nameEn,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (account.nameBn != account.nameEn) {
                                Text(
                                    text = if (isBangla) account.nameEn else account.nameBn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ERPBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isBangla) "বর্তমান ব্যালেন্স:" else "Current Balance:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                text = Formatters.formatMoney(netBalanceMinor, CurrencyCode.BDT, language),
                                fontWeight = FontWeight.Black,
                                color = if (netBalanceMinor >= 0) StatusSuccess else StatusError,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isBangla) "মোট ডেবিট:" else "Total Debit:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(Formatters.formatMoney(trialItem?.totalDebitMinor ?: 0L, CurrencyCode.BDT, language), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isBangla) "মোট ক্রেডিট:" else "Total Credit:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(Formatters.formatMoney(trialItem?.totalCreditMinor ?: 0L, CurrencyCode.BDT, language), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                        val capitalizedMinor = if (account.accountCode in setOf("5020", "5030", "5040", "5140") && netBalanceMinor == 0L) {
                            minOf(trialItem?.totalDebitMinor ?: 0L, trialItem?.totalCreditMinor ?: 0L)
                        } else 0L
                        if (capitalizedMinor > 0L) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isBangla) "স্টকে যুক্ত আমদানি ব্যয়:" else "Capitalized Import Cost:", style = MaterialTheme.typography.labelSmall, color = SecondaryTeal)
                                Text(Formatters.formatMoney(capitalizedMinor, CurrencyCode.BDT, language), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SecondaryTeal)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(if (isBangla) "ধরন" else "Type", "${account.accountType} (${account.normalBalance})")
                account.liquidKind?.let { DetailRow(if (isBangla) "চ্যানেল" else "Channel", if (isBangla) it.titleBn else it.titleEn) }
                account.institutionName?.let { DetailRow(if (isBangla) "প্রতিষ্ঠান" else "Institution", it) }
                account.branchName?.let { DetailRow(if (isBangla) "শাখা" else "Branch", it) }
                account.accountNumber?.let { DetailRow(if (isBangla) "অ্যাকাউন্ট নং" else "Account #", it) }
                account.description?.let { DetailRow(if (isBangla) "বিবরণ" else "Description", it) }
                DetailRow(
                    if (isBangla) "অবস্থা" else "Status",
                    if (account.isActive) (if (isBangla) "সক্রিয়" else "Active") else (if (isBangla) "নিষ্ক্রিয়" else "Inactive")
                )

                if (deleteErrorBanner != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StatusErrorContainer,
                        border = BorderStroke(1.dp, StatusError.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = StatusError,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = deleteErrorBanner ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusError,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (showDeleteConfirm) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StatusErrorContainer,
                        border = BorderStroke(1.dp, StatusError.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (isBangla)
                                    "এই হিসাবটি স্থায়ীভাবে ডিলিট/সরাতে চান? (ব্যালেন্স ৳০.০০)"
                                else
                                    "Delete this account permanently? (Balance is ৳0.00)",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusError,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text(if (isBangla) "বাতিল" else "Cancel")
                                }
                                Button(
                                    onClick = {
                                        showDeleteConfirm = false
                                        onDeleteAccount?.invoke { err ->
                                            deleteErrorBanner = err
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                                ) {
                                    Text(if (isBangla) "হ্যাঁ, ডিলিট করুন" else "Confirm Delete")
                                }
                            }
                        }
                    }
                }

                val isInvestorEquity = account.accountType == AccountType.EQUITY.name && account.accountCode != "3020"
                if (account.liquidKind != null && onDepositCapital != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onDepositCapital,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) "এই হিসাবে মূলধন / টাকা জমা করুন" else "Add Capital / Deposit to Account",
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isInvestorEquity && (onDepositCapital != null || onWithdrawOrProfit != null)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onDepositCapital != null) {
                            Button(
                                onClick = onDepositCapital,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (isBangla) "মূলধন জমা" else "Deposit",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (onWithdrawOrProfit != null) {
                            Button(
                                onClick = onWithdrawOrProfit,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (isBangla) "উত্তোলন / মুনাফা" else "Withdraw / Profit",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                if (account.id == "coa-2020" && netBalanceMinor > 0L && onPayAccruedFreight != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onPayAccruedFreight,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) "বকেয়া ফ্রেইট / শুল্ক পরিশোধ করুন" else "Pay Accrued Freight & Customs",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canDeleteAccount) {
                        OutlinedButton(
                            onClick = {
                                if (netBalanceMinor != 0L) {
                                    val balFmt = Formatters.formatMoney(kotlin.math.abs(netBalanceMinor), CurrencyCode.BDT, language)
                                    deleteErrorBanner = if (isBangla) {
                                        "⚠️ এই অ্যাকাউন্টে বর্তমানে $balFmt রয়েছে। অ্যাকাউন্ট ফাঁকা (৳০.০০) না হওয়া পর্যন্ত ডিলিট করা যাবে না। আগে ফান্ড ট্রান্সফার করুন।"
                                    } else {
                                        "⚠️ Account still holds $balFmt. Empty the balance (৳0.00) before deleting."
                                    }
                                    showDeleteConfirm = false
                                } else {
                                    deleteErrorBanner = null
                                    showDeleteConfirm = true
                                }
                            },
                            border = BorderStroke(1.dp, StatusError.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBangla) "ডিলিট" else "Delete", fontSize = 12.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) {
                            Text(if (isBangla) "বন্ধ করুন" else "Close")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = onEdit,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isBangla) "সম্পাদনা" else "Edit")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun LiquidAccountSelectorList(
    accounts: List<ChartOfAccount>,
    selectedAccountId: String,
    trialBalanceMap: Map<String, TrialBalanceItem>,
    accentColor: Color,
    isBangla: Boolean,
    onSelect: (String) -> Unit
) {
    val lang = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        accounts.forEach { acc ->
            val isSel = acc.id == selectedAccountId
            val tb = trialBalanceMap[acc.id]
            val bal = (tb?.totalDebitMinor ?: 0L) - (tb?.totalCreditMinor ?: 0L)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(acc.id) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSel) accentColor.copy(alpha = 0.12f) else ERPBackground,
                border = BorderStroke(1.dp, if (isSel) accentColor else ERPDivider)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = when (acc.liquidKind) {
                                LiquidAccountKind.CASH -> Icons.Outlined.AccountBalanceWallet
                                LiquidAccountKind.BANK -> Icons.Outlined.AccountBalance
                                LiquidAccountKind.MOBILE_BANKING -> Icons.Outlined.PhoneAndroid
                                null -> Icons.Outlined.AccountBalanceWallet
                            },
                            contentDescription = null,
                            tint = if (isSel) accentColor else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${acc.accountCode} • ${if (isBangla && acc.nameBn.isNotBlank()) acc.nameBn else acc.nameEn}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${if (isBangla) "বর্তমান ব্যালেন্স:" else "Current:"} ${Formatters.formatMoney(bal, CurrencyCode.BDT, lang)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (bal < 0L) StatusError else TextSecondary,
                                fontWeight = if (bal < 0L) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    if (isSel) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun InvestorEquitySelectorSection(
    title: String,
    equityAccounts: List<ChartOfAccount>,
    selectedEquityId: String,
    trialBalanceMap: Map<String, TrialBalanceItem>,
    accentColor: Color,
    isBangla: Boolean,
    onSelect: (String) -> Unit,
    onCreateInvestor: ((nameEn: String, nameBn: String, onCreated: (String) -> Unit) -> Unit)?
) {
    val lang = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH
    var showAddInvestor by rememberSaveable { mutableStateOf(false) }
    var newInvestorName by rememberSaveable { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        if (onCreateInvestor != null) {
            TextButton(
                onClick = { showAddInvestor = !showAddInvestor },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = accentColor)
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isBangla) "নতুন ইনভেস্টর" else "New Investor",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }
    }

    if (showAddInvestor && onCreateInvestor != null) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = ERPBackground,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newInvestorName,
                    onValueChange = { newInvestorName = it },
                    label = { Text(if (isBangla) "ইনভেস্টরের নাম * (যেমন: রহিম / Karim)" else "Investor Name * (e.g. Karim)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showAddInvestor = false; newInvestorName = "" }) {
                        Text(if (isBangla) "বাতিল" else "Cancel", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        enabled = newInvestorName.trim().isNotBlank() && !isCreating,
                        onClick = {
                            val clean = newInvestorName.trim()
                            isCreating = true
                            onCreateInvestor(clean, clean) { createdId ->
                                onSelect(createdId)
                                newInvestorName = ""
                                showAddInvestor = false
                                isCreating = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isBangla) "যোগ ও নির্বাচন" else "Create & Select",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        equityAccounts.forEach { acc ->
            val isSel = acc.id == selectedEquityId
            val tb = trialBalanceMap[acc.id]
            val capBal = (tb?.totalCreditMinor ?: 0L) - (tb?.totalDebitMinor ?: 0L)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(acc.id) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSel) accentColor.copy(alpha = 0.12f) else ERPBackground,
                border = BorderStroke(1.dp, if (isSel) accentColor else ERPDivider)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${acc.accountCode} • ${if (isBangla && acc.nameBn.isNotBlank()) acc.nameBn else acc.nameEn}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${if (isBangla) "বর্তমান মূলধন:" else "Current Capital:"} ${Formatters.formatMoney(capBal, CurrencyCode.BDT, lang)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (capBal > 0L) StatusSuccess else TextSecondary,
                            fontWeight = if (capBal > 0L) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    if (isSel) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DepositCapitalDialog(
    liquidAccounts: List<ChartOfAccount>,
    trialBalanceMap: Map<String, TrialBalanceItem>,
    preselectedAccountId: String? = null,
    equityAccounts: List<ChartOfAccount> = emptyList(),
    preselectedEquityAccountId: String? = null,
    onCreateInvestor: ((nameEn: String, nameBn: String, onCreated: (String) -> Unit) -> Unit)? = null,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (accountId: String, equityAccountId: String, amountMinor: Long, notes: String?) -> Unit
) {
    val activeAccounts = remember(liquidAccounts) { liquidAccounts.filter { it.isActive } }
    val activeEquityAccounts = remember(equityAccounts) {
        equityAccounts.filter { it.isActive && it.accountType == AccountType.EQUITY.name && it.accountCode != "3020" }
    }
    var selectedAccountId by remember(preselectedAccountId, activeAccounts) {
        mutableStateOf(
            preselectedAccountId?.takeIf { id -> activeAccounts.any { it.id == id } }
                ?: activeAccounts.find { it.accountCode == "1010" }?.id
                ?: activeAccounts.firstOrNull()?.id.orEmpty()
        )
    }
    var selectedEquityId by remember(preselectedEquityAccountId) {
        mutableStateOf(
            preselectedEquityAccountId?.takeIf { id -> activeEquityAccounts.any { it.id == id } }
                ?: activeEquityAccounts.find { it.id == AccountingRepositoryImpl.OWNER_CAPITAL_ACCOUNT_ID }?.id
                ?: activeEquityAccounts.firstOrNull()?.id
                ?: AccountingRepositoryImpl.OWNER_CAPITAL_ACCOUNT_ID
        )
    }
    var pendingEquityId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(activeEquityAccounts) {
        val pending = pendingEquityId
        if (pending != null && activeEquityAccounts.any { it.id == pending }) {
            selectedEquityId = pending
            pendingEquityId = null
        } else if (activeEquityAccounts.isNotEmpty() && activeEquityAccounts.none { it.id == selectedEquityId }) {
            selectedEquityId = activeEquityAccounts.find { it.id == AccountingRepositoryImpl.OWNER_CAPITAL_ACCOUNT_ID }?.id
                ?: activeEquityAccounts.first().id
        }
    }

    var amountStr by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val selectedAcc = activeAccounts.find { it.id == selectedAccountId }
    val selectedBalMinor = selectedAcc?.let { acc ->
        val tb = trialBalanceMap[acc.id]
        (tb?.totalDebitMinor ?: 0L) - (tb?.totalCreditMinor ?: 0L)
    } ?: 0L
    val selectedEquityAcc = activeEquityAccounts.find { it.id == selectedEquityId }
    val selectedEquityLabel = selectedEquityAcc?.let {
        "${it.accountCode} ${if (isBangla && it.nameBn.isNotBlank()) it.nameBn else it.nameEn}"
    } ?: if (isBangla) "৩০১০ মালিকের মূলধন" else "3010 Owner's Capital"
    val lang = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 540.dp)
                .clip(RoundedCornerShape(18.dp))
                .navigationBarsPadding()
                .imePadding(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "মূলধন / ইনভেস্টমেন্ট জমা" else "Add Capital / Investor Deposit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isBangla) {
                                "$selectedEquityLabel থেকে নগদ/ব্যাংকে তহবিল যোগ"
                            } else {
                                "Credit $selectedEquityLabel to Cash / Bank"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                HorizontalDivider(color = ERPDivider)

                if (activeEquityAccounts.isNotEmpty() || onCreateInvestor != null) {
                    InvestorEquitySelectorSection(
                        title = if (isBangla) "কার মূলধন জমা হচ্ছে? (মালিক / ইনভেস্টর) *" else "Investor / Equity Account *",
                        equityAccounts = activeEquityAccounts,
                        selectedEquityId = selectedEquityId,
                        trialBalanceMap = trialBalanceMap,
                        accentColor = PrimaryPurple,
                        isBangla = isBangla,
                        onSelect = { id ->
                            selectedEquityId = id
                            errorMessage = null
                        },
                        onCreateInvestor = if (onCreateInvestor != null) {
                            { nameEn, nameBn, done ->
                                onCreateInvestor(nameEn, nameBn) { createdId ->
                                    pendingEquityId = createdId
                                    selectedEquityId = createdId
                                    done(createdId)
                                }
                            }
                        } else null
                    )
                }

                Text(
                    text = if (isBangla) "কোন হিসাবে জমা হবে *" else "Deposit To Account *",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )

                LiquidAccountSelectorList(
                    accounts = activeAccounts,
                    selectedAccountId = selectedAccountId,
                    trialBalanceMap = trialBalanceMap,
                    accentColor = SecondaryTeal,
                    isBangla = isBangla,
                    onSelect = { id ->
                        selectedAccountId = id
                        errorMessage = null
                    }
                )

                if (selectedBalMinor < 0L) {
                    val deficitMinor = -selectedBalMinor
                    val deficitPlain = BigDecimal(deficitMinor).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                        .stripTrailingZeros().toPlainString()
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StatusErrorContainer,
                        border = BorderStroke(1.dp, StatusError.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isBangla) {
                                    "এই হিসাবে ${Formatters.formatMoney(deficitMinor, CurrencyCode.BDT, lang)} ঘাটতি রয়েছে। ১ ক্লিকে ঘাটতি পূরণ করতে নিচের বাটনে চাপুন:"
                                } else {
                                    "This account has a deficit of ${Formatters.formatMoney(deficitMinor, CurrencyCode.BDT, lang)}. Tap below to auto-fill:"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusError,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedButton(
                                onClick = {
                                    amountStr = deficitPlain
                                    errorMessage = null
                                },
                                border = BorderStroke(1.dp, StatusError),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isBangla) {
                                        "ঘাটতি পূরণ করুন (${Formatters.formatMoney(deficitMinor, CurrencyCode.BDT, lang)})"
                                    } else {
                                        "Cover Deficit (${Formatters.formatMoney(deficitMinor, CurrencyCode.BDT, lang)})"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        errorMessage = null
                    },
                    label = { Text(if (isBangla) "জমার পরিমাণ (BDT ৳) *" else "Deposit Amount (BDT ৳) *") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isBangla) "মন্তব্য / বিবরণ (ঐচ্ছিক)" else "Notes / Source (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(if (isBangla) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                require(selectedAccountId.isNotBlank()) {
                                    if (isBangla) "একটি হিসাব নির্বাচন করুন" else "Please select an account"
                                }
                                val amtBd = BigDecimal(Formatters.normalizeAsciiNumberInput(amountStr).trim())
                                require(amtBd > BigDecimal.ZERO) {
                                    if (isBangla) "জমার পরিমাণ শূন্যের বেশি হতে হবে" else "Amount must be greater than zero"
                                }
                                val minor = amtBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                                onConfirm(
                                    selectedAccountId,
                                    selectedEquityId.ifBlank { AccountingRepositoryImpl.OWNER_CAPITAL_ACCOUNT_ID },
                                    minor,
                                    notes.ifBlank { null }
                                )
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: if (isBangla) "সঠিক পরিমাণ লিখুন" else "Enter a valid amount"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                    ) {
                        Text(if (isBangla) "মূলধন জমা করুন" else "Deposit Capital")
                    }
                }
            }
        }
    }
}

@Composable
fun CapitalWithdrawalAndProfitDialog(
    initialModeProfit: Boolean = false,
    liquidAccounts: List<ChartOfAccount>,
    equityAccounts: List<ChartOfAccount>,
    trialBalanceMap: Map<String, TrialBalanceItem>,
    cumulativeNetProfitMinor: Long,
    alreadyDistributedMinor: Long,
    availableProfitMinor: Long,
    preselectedEquityAccountId: String? = null,
    isBangla: Boolean,
    onCreateInvestor: (nameEn: String, nameBn: String, onCreated: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onConfirmWithdrawal: (liquidAccountId: String, equityAccountId: String, amountMinor: Long, notes: String?) -> Unit,
    onConfirmProfitDistribution: (investorEquityAccountId: String, amountMinor: Long, reinvestToCapital: Boolean, paymentAccountId: String?, notes: String?) -> Unit
) {
    val activeLiquidAccounts = remember(liquidAccounts) { liquidAccounts.filter { it.isActive } }
    val activeEquityAccounts = remember(equityAccounts) {
        equityAccounts.filter { it.isActive && it.accountType == AccountType.EQUITY.name && it.accountCode != "3020" }
    }
    var isProfitMode by rememberSaveable(initialModeProfit) { mutableStateOf(initialModeProfit) }
    var reinvestToCapital by rememberSaveable { mutableStateOf(false) }

    var selectedEquityId by remember(preselectedEquityAccountId) {
        mutableStateOf(
            preselectedEquityAccountId?.takeIf { id -> activeEquityAccounts.any { it.id == id } }
                ?: activeEquityAccounts.find { it.id == AccountingRepositoryImpl.OWNER_CAPITAL_ACCOUNT_ID }?.id
                ?: activeEquityAccounts.firstOrNull()?.id
                ?: AccountingRepositoryImpl.OWNER_CAPITAL_ACCOUNT_ID
        )
    }
    var pendingEquityId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(activeEquityAccounts) {
        val pending = pendingEquityId
        if (pending != null && activeEquityAccounts.any { it.id == pending }) {
            selectedEquityId = pending
            pendingEquityId = null
        } else if (activeEquityAccounts.isNotEmpty() && activeEquityAccounts.none { it.id == selectedEquityId }) {
            selectedEquityId = activeEquityAccounts.find { it.id == AccountingRepositoryImpl.OWNER_CAPITAL_ACCOUNT_ID }?.id
                ?: activeEquityAccounts.first().id
        }
    }

    var selectedLiquidId by remember(activeLiquidAccounts) {
        mutableStateOf(
            activeLiquidAccounts.maxByOrNull { acc ->
                val tb = trialBalanceMap[acc.id]
                (tb?.totalDebitMinor ?: 0L) - (tb?.totalCreditMinor ?: 0L)
            }?.id ?: activeLiquidAccounts.firstOrNull()?.id.orEmpty()
        )
    }

    var profitPercentStr by rememberSaveable { mutableStateOf("") }
    var amountStr by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val lang = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH

    val selectedLiquidBalMinor = trialBalanceMap[selectedLiquidId]?.let { it.totalDebitMinor - it.totalCreditMinor } ?: 0L
    val selectedEquityAcc = activeEquityAccounts.find { it.id == selectedEquityId }
    val selectedEquityCapitalMinor = (trialBalanceMap[selectedEquityId]?.let {
        it.totalCreditMinor - it.totalDebitMinor
    } ?: 0L).coerceAtLeast(0L)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 540.dp)
                .clip(RoundedCornerShape(18.dp))
                .navigationBarsPadding()
                .imePadding(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "উত্তোলন ও মুনাফা বণ্টন" else "Withdrawal & Profit Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isProfitMode) {
                                if (isBangla) "অর্জিত মুনাফা (৩০২০) থেকে ইনভেস্টরকে অংশ প্রদান" else "Distribute from Retained Earnings (3020)"
                            } else {
                                if (isBangla) "মালিক / ইনভেস্টরের মূলধন উত্তোলন (Drawings)" else "Owner / Investor Capital Withdrawal"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                isProfitMode = false
                                errorMessage = null
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isProfitMode) PrimaryPurple else ERPBackground,
                        border = BorderStroke(1.dp, if (!isProfitMode) PrimaryPurple else ERPDivider)
                    ) {
                        Text(
                            text = if (isBangla) "মূলধন উত্তোলন" else "Capital Withdraw",
                            color = if (!isProfitMode) Color.White else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                isProfitMode = true
                                errorMessage = null
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isProfitMode) SecondaryTeal else ERPBackground,
                        border = BorderStroke(1.dp, if (isProfitMode) SecondaryTeal else ERPDivider)
                    ) {
                        Text(
                            text = if (isBangla) "মুনাফা বণ্টন (Profit)" else "Profit Share",
                            color = if (isProfitMode) Color.White else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                        )
                    }
                }

                HorizontalDivider(color = ERPDivider)

                if (isProfitMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SecondaryTeal.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DetailRow(
                                if (isBangla) "অর্জিত নিট মুনাফা:" else "Cumulative Net Profit:",
                                Formatters.formatMoney(cumulativeNetProfitMinor, CurrencyCode.BDT, lang)
                            )
                            DetailRow(
                                if (isBangla) "পূর্বে বণ্টনকৃত:" else "Already Distributed:",
                                Formatters.formatMoney(alreadyDistributedMinor, CurrencyCode.BDT, lang)
                            )
                            HorizontalDivider(color = SecondaryTeal.copy(alpha = 0.25f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (isBangla) "বণ্টনযোগ্য মুনাফা:" else "Distributable Profit:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = Formatters.formatMoney(availableProfitMinor, CurrencyCode.BDT, lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (availableProfitMinor > 0L) StatusSuccess else StatusError
                                )
                            }
                        }
                    }
                }

                InvestorEquitySelectorSection(
                    title = if (isProfitMode) {
                        if (isBangla) "কোন ইনভেস্টর/মালিককে মুনাফা দেবেন? *" else "Recipient Investor / Owner *"
                    } else {
                        if (isBangla) "কে টাকা উত্তোলন করছেন? (মালিক / ইনভেস্টর) *" else "Withdraw From Owner / Investor *"
                    },
                    equityAccounts = activeEquityAccounts,
                    selectedEquityId = selectedEquityId,
                    trialBalanceMap = trialBalanceMap,
                    accentColor = if (isProfitMode) SecondaryTeal else PrimaryPurple,
                    isBangla = isBangla,
                    onSelect = { id ->
                        selectedEquityId = id
                        errorMessage = null
                    },
                    onCreateInvestor = { nameEn, nameBn, done ->
                        onCreateInvestor(nameEn, nameBn) { createdId ->
                            pendingEquityId = createdId
                            selectedEquityId = createdId
                            done(createdId)
                        }
                    }
                )

                if (isProfitMode) {
                    Text(
                        text = if (isBangla) "পেআউট ধরন *" else "Distribution Method *",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    reinvestToCapital = false
                                    errorMessage = null
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (!reinvestToCapital) PrimaryPurple.copy(alpha = 0.14f) else ERPBackground,
                            border = BorderStroke(1.dp, if (!reinvestToCapital) PrimaryPurple else ERPDivider)
                        ) {
                            Text(
                                text = if (isBangla) "নগদ / ব্যাংক পেআউট" else "Cash / Bank Payout",
                                color = if (!reinvestToCapital) PrimaryPurple else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    reinvestToCapital = true
                                    errorMessage = null
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (reinvestToCapital) SecondaryTeal.copy(alpha = 0.14f) else ERPBackground,
                            border = BorderStroke(1.dp, if (reinvestToCapital) SecondaryTeal else ERPDivider)
                        ) {
                            Text(
                                text = if (isBangla) "মূলধনে পুনঃবিনিয়োগ" else "Reinvest to Capital",
                                color = if (reinvestToCapital) SecondaryTeal else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                            )
                        }
                    }
                }

                if (!isProfitMode || !reinvestToCapital) {
                    Text(
                        text = if (isBangla) "কোন নগদ/ব্যাংক হিসাব থেকে টাকা যাবে? *" else "Pay From Liquid Account *",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    LiquidAccountSelectorList(
                        accounts = activeLiquidAccounts,
                        selectedAccountId = selectedLiquidId,
                        trialBalanceMap = trialBalanceMap,
                        accentColor = if (isProfitMode) SecondaryTeal else PrimaryPurple,
                        isBangla = isBangla,
                        onSelect = { id ->
                            selectedLiquidId = id
                            errorMessage = null
                        }
                    )
                }

                if (isProfitMode) {
                    OutlinedTextField(
                        value = profitPercentStr,
                        onValueChange = { rawPct ->
                            profitPercentStr = rawPct
                            errorMessage = null
                            val clean = Formatters.normalizeAsciiNumberInput(rawPct).trim()
                            if (clean.isNotBlank() && availableProfitMinor > 0L) {
                                runCatching {
                                    val pct = BigDecimal(clean)
                                    if (pct > BigDecimal.ZERO && pct <= BigDecimal("100")) {
                                        val shareMinor = BigDecimal(availableProfitMinor)
                                            .multiply(pct)
                                            .divide(BigDecimal("100"), 0, RoundingMode.HALF_UP)
                                        amountStr = shareMinor.divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
                                            .stripTrailingZeros().toPlainString()
                                    }
                                }
                            }
                        },
                        label = { Text(if (isBangla) "মুনাফার অংশ % (ঐচ্ছিক, যেমন: 25)" else "Profit Share % (Optional, e.g. 25)") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        profitPercentStr = ""
                        errorMessage = null
                    },
                    label = {
                        Text(
                            if (isProfitMode) {
                                if (isBangla) "লভ্যাংশের পরিমাণ (BDT ৳) *" else "Profit Amount (BDT ৳) *"
                            } else {
                                if (isBangla) "উত্তোলনের পরিমাণ (BDT ৳) *" else "Withdrawal Amount (BDT ৳) *"
                            }
                        )
                    },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isBangla) "নোট / কারণ (ঐচ্ছিক)" else "Notes / Reason (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(if (isBangla) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val amtBd = BigDecimal(Formatters.normalizeAsciiNumberInput(amountStr).trim())
                                require(amtBd > BigDecimal.ZERO) {
                                    if (isBangla) "পরিমাণ শূন্যের বেশি হতে হবে" else "Amount must be positive"
                                }
                                val minor = amtBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                                val cleanNotes = notes.ifBlank { null }

                                if (isProfitMode) {
                                    require(availableProfitMinor > 0L && minor <= availableProfitMinor) {
                                        if (isBangla) {
                                            "বণ্টনযোগ্য মুনাফার (${Formatters.formatMoney(availableProfitMinor, CurrencyCode.BDT, lang)}) চেয়ে বেশি দেওয়া যাবে না!"
                                        } else {
                                            "Cannot exceed distributable profit (${Formatters.formatMoney(availableProfitMinor, CurrencyCode.BDT, lang)})!"
                                        }
                                    }
                                    if (!reinvestToCapital) {
                                        require(selectedLiquidId.isNotBlank()) {
                                            if (isBangla) "একটি নগদ/ব্যাংক হিসাব নির্বাচন করুন" else "Select a liquid account"
                                        }
                                        require(selectedLiquidBalMinor >= minor) {
                                            if (isBangla) {
                                                "নির্বাচিত নগদ/ব্যাংক হিসাবে পর্যাপ্ত ব্যালেন্স নেই!"
                                            } else {
                                                "Insufficient balance in selected account!"
                                            }
                                        }
                                    }
                                    onConfirmProfitDistribution(
                                        selectedEquityId,
                                        minor,
                                        reinvestToCapital,
                                        if (reinvestToCapital) null else selectedLiquidId,
                                        cleanNotes
                                    )
                                } else {
                                    require(selectedLiquidId.isNotBlank()) {
                                        if (isBangla) "একটি নগদ/ব্যাংক হিসাব নির্বাচন করুন" else "Select a liquid account"
                                    }
                                    require(selectedLiquidBalMinor >= minor) {
                                        if (isBangla) {
                                            "নির্বাচিত নগদ/ব্যাংক হিসাবে পর্যাপ্ত ব্যালেন্স নেই!"
                                        } else {
                                            "Insufficient balance in selected account!"
                                        }
                                    }
                                    if (selectedEquityId != AccountingRepositoryImpl.OWNER_CAPITAL_ACCOUNT_ID) {
                                        val eqLabel = selectedEquityAcc?.let {
                                            if (isBangla && it.nameBn.isNotBlank()) it.nameBn else it.nameEn
                                        } ?: ""
                                        require(minor <= selectedEquityCapitalMinor) {
                                            if (isBangla) {
                                                "$eqLabel-এর মূলধন (${Formatters.formatMoney(selectedEquityCapitalMinor, CurrencyCode.BDT, lang)}) এর চেয়ে বেশি উত্তোলন করা যাবে না!"
                                            } else {
                                                "Cannot withdraw more than $eqLabel's capital!"
                                            }
                                        }
                                    }
                                    onConfirmWithdrawal(selectedLiquidId, selectedEquityId, minor, cleanNotes)
                                }
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: if (isBangla) "সঠিক পরিমাণ লিখুন" else "Invalid amount"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isProfitMode) SecondaryTeal else PrimaryPurple
                        )
                    ) {
                        Text(
                            if (isProfitMode) {
                                if (isBangla) "মুনাফা বণ্টন নিশ্চিত" else "Distribute Profit"
                            } else {
                                if (isBangla) "উত্তোলন নিশ্চিত" else "Confirm Withdraw"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PayAccruedFreightDialog(
    accruedDueMinor: Long,
    liquidAccounts: List<ChartOfAccount>,
    trialBalanceMap: Map<String, TrialBalanceItem>,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (paymentAccountId: String, amountMinor: Long, notes: String?) -> Unit
) {
    val activeAccounts = remember(liquidAccounts) { liquidAccounts.filter { it.isActive } }
    var selectedAccountId by remember(activeAccounts) {
        mutableStateOf(
            activeAccounts.maxByOrNull { acc ->
                val tb = trialBalanceMap[acc.id]
                (tb?.totalDebitMinor ?: 0L) - (tb?.totalCreditMinor ?: 0L)
            }?.id ?: activeAccounts.firstOrNull()?.id.orEmpty()
        )
    }
    var amountStr by rememberSaveable {
        mutableStateOf(
            BigDecimal(accruedDueMinor.coerceAtLeast(0L)).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString()
        )
    }
    var notes by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val lang = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH
    val selectedBalMinor = trialBalanceMap[selectedAccountId]?.let { it.totalDebitMinor - it.totalCreditMinor } ?: 0L

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 540.dp)
                .clip(RoundedCornerShape(18.dp))
                .navigationBarsPadding()
                .imePadding(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isBangla) "বকেয়া ফ্রেইট ও শুল্ক পরিশোধ (২০২০)" else "Pay Accrued Freight & Customs (2020)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (isBangla) {
                        "মোট বকেয়া: ${Formatters.formatMoney(accruedDueMinor, CurrencyCode.BDT, lang)}"
                    } else {
                        "Total Accrued Due: ${Formatters.formatMoney(accruedDueMinor, CurrencyCode.BDT, lang)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    activeAccounts.forEach { acc ->
                        val isSel = acc.id == selectedAccountId
                        val tb = trialBalanceMap[acc.id]
                        val bal = (tb?.totalDebitMinor ?: 0L) - (tb?.totalCreditMinor ?: 0L)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedAccountId = acc.id
                                    errorMessage = null
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) PrimaryPurple.copy(alpha = 0.1f) else ERPBackground,
                            border = BorderStroke(1.dp, if (isSel) PrimaryPurple else ERPDivider)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${acc.accountCode} • ${if (isBangla && acc.nameBn.isNotBlank()) acc.nameBn else acc.nameEn}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = Formatters.formatMoney(bal, CurrencyCode.BDT, lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (bal <= 0L) StatusError else StatusSuccess
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        errorMessage = null
                    },
                    label = { Text(if (isBangla) "পরিশোধের পরিমাণ (BDT ৳) *" else "Payment Amount (BDT ৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isBangla) "নোট / ফরওয়ার্ডার রেফারেন্স" else "Notes / Forwarder Ref") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(if (isBangla) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                require(selectedAccountId.isNotBlank()) {
                                    if (isBangla) "একটি হিসাব নির্বাচন করুন" else "Select a payment account"
                                }
                                val amtBd = BigDecimal(Formatters.normalizeAsciiNumberInput(amountStr).trim())
                                require(amtBd > BigDecimal.ZERO) {
                                    if (isBangla) "পরিমাণ শূন্যের বেশি হতে হবে" else "Amount must be positive"
                                }
                                val minor = amtBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                                require(minor <= accruedDueMinor) {
                                    if (isBangla) "মোট বকেয়ার চেয়ে বেশি পরিশোধ করা যাবে না" else "Cannot pay more than accrued balance"
                                }
                                require(selectedBalMinor >= minor) {
                                    if (isBangla) {
                                        "নির্বাচিত হিসাবে পর্যাপ্ত ব্যালেন্স নেই! আগে মূলধন জমা করুন।"
                                    } else {
                                        "Insufficient balance in selected account!"
                                    }
                                }
                                onConfirm(selectedAccountId, minor, notes.ifBlank { null })
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Invalid payment amount"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text(if (isBangla) "পরিশোধ করুন" else "Pay Now")
                    }
                }
            }
        }
    }
}
