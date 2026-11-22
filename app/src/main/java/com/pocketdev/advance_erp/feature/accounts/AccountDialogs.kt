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
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
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
    onDeleteAccount: ((onError: (String) -> Unit) -> Unit)? = null
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
