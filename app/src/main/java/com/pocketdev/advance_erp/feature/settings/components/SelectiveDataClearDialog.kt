package com.pocketdev.advance_erp.feature.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.LocalSettingsTokens
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.security.PinSecurityHelper
import com.pocketdev.advance_erp.domain.model.DataClearImpactPreview
import com.pocketdev.advance_erp.domain.model.DataClearModule
import com.pocketdev.advance_erp.domain.model.DataClearPreset
import com.pocketdev.advance_erp.domain.model.SelectiveDataClearPlanner

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectiveDataClearDialog(
    language: AppLanguage,
    preview: DataClearImpactPreview?,
    isAppLockEnabled: Boolean,
    pinCodeHash: String?,
    pinSalt: String?,
    onExecuteClear: (selectedModules: Set<DataClearModule>, createSafetyBackup: Boolean, onFinished: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalSettingsTokens.current
    val isBn = language == AppLanguage.BANGLA

    var selectedNames by rememberSaveable {
        mutableStateOf(
            SelectiveDataClearPlanner.modulesForPreset(DataClearPreset.SMART_FULL_RESET)
                .map { it.name }
                .toList()
        )
    }
    var createSafetyBackup by rememberSaveable { mutableStateOf(true) }
    var confirmStep by rememberSaveable { mutableStateOf(false) }
    var confirmInput by rememberSaveable { mutableStateOf("") }
    var isExecuting by rememberSaveable { mutableStateOf(false) }

    val selectedModules = remember(selectedNames) {
        selectedNames.mapNotNull { name -> runCatching { DataClearModule.valueOf(name) }.getOrNull() }.toSet()
    }
    val activePreset = remember(selectedModules) {
        SelectiveDataClearPlanner.detectMatchingPreset(selectedModules)
    }
    val warnings = remember(selectedModules, preview) {
        SelectiveDataClearPlanner.buildWarnings(selectedModules, preview)
    }
    val requiresPin = isAppLockEnabled && !pinCodeHash.isNullOrBlank()
    val isInputValid = SelectiveDataClearPlanner.validateConfirmationInput(
        isAppLockEnabled = isAppLockEnabled,
        storedPinHash = pinCodeHash,
        storedPinSalt = pinSalt,
        rawInput = confirmInput,
        pinVerifier = PinSecurityHelper::verifyPin
    )

    val safeGreenBg = if (tokens.isDark) Color(0xFF102A1E) else Color(0xFFECFDF5)
    val safeGreenText = if (tokens.isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
    val warnBg = if (tokens.isDark) Color(0xFF2B2010) else Color(0xFFFFFBEB)
    val warnBorder = if (tokens.isDark) Color(0xFF785118) else Color(0xFFFCD34D)
    val warnText = if (tokens.isDark) Color(0xFFFCD34D) else Color(0xFF92400E)
    val dangerBg = if (tokens.isDark) Color(0xFF2D1418) else Color(0xFFFEF2F2)
    val dangerText = if (tokens.isDark) Color(0xFFFCA5A5) else MaterialTheme.colorScheme.error

    Dialog(
        onDismissRequest = { if (!isExecuting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .imePadding(),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, tokens.cardBorderStart),
            color = tokens.pageBgTop,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Surface(
                    color = tokens.headerBgStart,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (!isExecuting) onDismiss() },
                                enabled = !isExecuting
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = if (isBn) "বন্ধ" else "Close",
                                    tint = tokens.titleText
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBn) "মডিউল ভিত্তিক ডাটা রিসেট" else "Selective Module Data Reset",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.titleText
                                )
                                Text(
                                    text = if (confirmStep) {
                                        if (isBn) "ধাপ ২/২ — নিরাপত্তা যাচাই ও চূড়ান্ত নিশ্চিতকরণ" else "Step 2/2 — Security Verification & Final Confirm"
                                    } else {
                                        if (isBn) "ধাপ ১/২ — ইচ্ছামতো রিসেট করার মডিউল নির্বাচন করুন" else "Step 1/2 — Choose Modules to Reset or Clear"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.subtitleText
                                )
                            }
                        }
                        HorizontalDivider(color = tokens.dividerColor)
                    }
                }

                // Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!confirmStep) {
                        // Preserved Masters Summary
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = safeGreenBg,
                            border = BorderStroke(1.dp, safeGreenText.copy(alpha = 0.28f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Shield,
                                        contentDescription = null,
                                        tint = safeGreenText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (isBn) "যা সর্বদা অক্ষত থাকবে (Preserved Masters)" else "Always Preserved Safe Data",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = safeGreenText
                                    )
                                }
                                if (preview == null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = safeGreenText
                                        )
                                        Text(
                                            text = if (isBn) "লাইভ ডাটাবেজ কাউন্ট লোড হচ্ছে..." else "Loading live database impact counts...",
                                            fontSize = 11.5.sp,
                                            color = tokens.subtitleText
                                        )
                                    }
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val chips = if (isBn) {
                                            listOf(
                                                "${preview.preservedProductCount} প্রোডাক্ট • ${preview.preservedVariantCount} ভ্যারিয়েন্ট",
                                                "${preview.preservedPoCount} PO স্ট্রাকচার",
                                                "${preview.preservedAccountCount} হিসাব খাতের নাম",
                                                "${preview.preservedCustomerCount} গ্রাহক • ${preview.preservedSupplierCount} সাপ্লায়ার"
                                            )
                                        } else {
                                            listOf(
                                                "${preview.preservedProductCount} Products • ${preview.preservedVariantCount} Variants",
                                                "${preview.preservedPoCount} PO Structures",
                                                "${preview.preservedAccountCount} Ledger Accounts",
                                                "${preview.preservedCustomerCount} Customers • ${preview.preservedSupplierCount} Suppliers"
                                            )
                                        }
                                        chips.forEach { text ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = tokens.cardBg,
                                                border = BorderStroke(1.dp, safeGreenText.copy(alpha = 0.25f))
                                            ) {
                                                Text(
                                                    text = text,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = tokens.titleText,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Quick Preset Row
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isBn) "দ্রুত প্রিসেট নির্বাচন" else "Quick Presets",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = tokens.titleText
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = activePreset == DataClearPreset.SMART_FULL_RESET,
                                    onClick = {
                                        selectedNames = SelectiveDataClearPlanner
                                            .modulesForPreset(DataClearPreset.SMART_FULL_RESET)
                                            .map { it.name }
                                    },
                                    label = {
                                        Text(
                                            if (isBn) "স্মার্ট ফুল রিসেট" else "Smart Full Reset",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryPurple.copy(alpha = 0.16f),
                                        selectedLabelColor = if (tokens.isDark) Color(0xFFE9D5FF) else PrimaryPurple
                                    )
                                )
                                FilterChip(
                                    selected = activePreset == DataClearPreset.CORE_OPERATIONS_RESET,
                                    onClick = {
                                        selectedNames = SelectiveDataClearPlanner
                                            .modulesForPreset(DataClearPreset.CORE_OPERATIONS_RESET)
                                            .map { it.name }
                                    },
                                    label = {
                                        Text(
                                            if (isBn) "শুধু মূল ৪টি মডিউল" else "Core 4 Only",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SecondaryTeal.copy(alpha = 0.16f),
                                        selectedLabelColor = if (tokens.isDark) Color(0xFF99F6E4) else SecondaryTeal
                                    )
                                )
                                FilterChip(
                                    selected = selectedModules.isEmpty(),
                                    onClick = { selectedNames = emptyList() },
                                    label = {
                                        Text(
                                            if (isBn) "সব আনচেক" else "Unselect All",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                )
                            }
                        }

                        // Selectable Module Cards
                        DataClearModule.entries.forEach { module ->
                            val isSelected = module in selectedModules
                            val countBadge = preview?.moduleCounts?.get(module)?.let {
                                if (isBn) it.badgeTextBn else it.badgeTextEn
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) tokens.cardExpandedBg else tokens.cardBg,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) PrimaryPurple else tokens.innerPanelBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedNames = if (isSelected) {
                                            selectedNames - module.name
                                        } else {
                                            selectedNames + module.name
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedNames = if (checked) {
                                                selectedNames + module.name
                                            } else {
                                                selectedNames - module.name
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = PrimaryPurple,
                                            uncheckedColor = tokens.subtitleText
                                        )
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = if (isBn) module.titleBn else module.titleEn,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = tokens.titleText
                                        )
                                        if (!countBadge.isNullOrBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = tokens.innerPanelBg,
                                                border = BorderStroke(1.dp, tokens.innerPanelBorder)
                                            ) {
                                                Text(
                                                    text = countBadge,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isSelected) {
                                                        if (tokens.isDark) Color(0xFFE9D5FF) else PrimaryPurple
                                                    } else {
                                                        tokens.subtitleText
                                                    },
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "✓ ${if (isBn) module.keepNoteBn else module.keepNoteEn}",
                                            fontSize = 11.5.sp,
                                            color = safeGreenText,
                                            lineHeight = 16.sp
                                        )
                                        Text(
                                            text = "↻ ${if (isBn) module.resetNoteBn else module.resetNoteEn}",
                                            fontSize = 11.5.sp,
                                            color = if (isSelected) dangerText else tokens.subtitleText,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Smart Dependency & Ledger Sync Warnings
                        if (warnings.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = warnBg,
                                border = BorderStroke(1.dp, warnBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = warnText,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (isBn) "স্মার্ট সমন্বয় ও ডিপেন্ডেন্সি তথ্য" else "Smart Dependency & Ledger Sync Notice",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = warnText
                                        )
                                    }
                                    warnings.forEach { (en, bn) ->
                                        Text(
                                            text = "• ${if (isBn) bn else en}",
                                            fontSize = 11.5.sp,
                                            color = tokens.titleText,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Automatic Safety Backup Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = tokens.innerPanelBg,
                            border = BorderStroke(1.dp, tokens.innerPanelBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Backup,
                                    contentDescription = null,
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBn) "স্বয়ংক্রিয় সেফটি ব্যাকআপ (.zip)" else "Automatic Safety Backup (.zip)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = tokens.titleText
                                    )
                                    Text(
                                        text = if (isBn) {
                                            "ডাটা রিসেট শুরুর আগে বর্তমান ডাটাবেজের পূর্ণাঙ্গ ব্যাকআপ তৈরি হবে।"
                                        } else {
                                            "Saves a full .zip restore point before executing the reset."
                                        },
                                        fontSize = 11.sp,
                                        color = tokens.subtitleText
                                    )
                                }
                                Switch(
                                    checked = createSafetyBackup,
                                    onCheckedChange = { createSafetyBackup = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = PrimaryPurple)
                                )
                            }
                        }
                    } else {
                        // Stage 2: Confirmation & Security Verification
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = dangerBg,
                            border = BorderStroke(1.dp, dangerText.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.WarningAmber,
                                        contentDescription = null,
                                        tint = dangerText,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (isBn) "নির্বাচিত ${selectedModules.size}টি মডিউল রিসেট হবে" else "${selectedModules.size} Selected Modules Will Be Reset",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = dangerText
                                    )
                                }
                                selectedModules.forEach { mod ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.RestartAlt,
                                            contentDescription = null,
                                            tint = dangerText,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (isBn) mod.titleBn else mod.titleEn,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = tokens.titleText
                                        )
                                    }
                                }
                                HorizontalDivider(color = dangerText.copy(alpha = 0.25f))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (createSafetyBackup) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                                        contentDescription = null,
                                        tint = if (createSafetyBackup) safeGreenText else warnText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (createSafetyBackup) {
                                            if (isBn) "রিসেটের আগে সেফটি ব্যাকআপ স্বয়ংক্রিয়ভাবে সংরক্ষণ করা হবে।" else "Safety .zip backup will be saved before clearing."
                                        } else {
                                            if (isBn) "সতর্কতা: সেফটি ব্যাকআপ বন্ধ রাখা হয়েছে।" else "Warning: Automatic safety backup is turned OFF."
                                        },
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = tokens.titleText
                                    )
                                }
                            }
                        }

                        if (warnings.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = warnBg,
                                border = BorderStroke(1.dp, warnBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    warnings.forEach { (en, bn) ->
                                        Text(
                                            text = "• ${if (isBn) bn else en}",
                                            fontSize = 11.5.sp,
                                            color = tokens.titleText
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = tokens.cardBg,
                            border = BorderStroke(1.dp, tokens.innerPanelBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (requiresPin) {
                                            if (isBn) "অ্যাপ লক পিন যাচাই করুন" else "Verify App Lock PIN"
                                        } else {
                                            if (isBn) "চূড়ান্ত নিশ্চিতকরণ লিখুন" else "Enter Final Confirmation"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = tokens.titleText
                                    )
                                }
                                Text(
                                    text = if (requiresPin) {
                                        if (isBn) {
                                            "আপনার অ্যাপ লক চালু রয়েছে। রিসেট সম্পন্ন করতে বর্তমান পিন কোড লিখুন।"
                                        } else {
                                            "App Lock is enabled. Enter your current PIN to authorize this reset."
                                        }
                                    } else {
                                        if (isBn) {
                                            "ভুলবশত রিসেট এড়াতে নিচের বক্সে ইংরেজিতে CLEAR লিখুন।"
                                        } else {
                                            "Type CLEAR below to confirm this module data reset."
                                        }
                                    },
                                    fontSize = 12.sp,
                                    color = tokens.subtitleText
                                )
                                OutlinedTextField(
                                    value = confirmInput,
                                    onValueChange = { if (!isExecuting) confirmInput = it },
                                    singleLine = true,
                                    enabled = !isExecuting,
                                    label = {
                                        Text(
                                            if (requiresPin) {
                                                if (isBn) "অ্যাপ পিন" else "App Lock PIN"
                                            } else {
                                                if (isBn) "CLEAR লিখুন" else "Type CLEAR"
                                            }
                                        )
                                    },
                                    visualTransformation = if (requiresPin) PasswordVisualTransformation() else VisualTransformation.None,
                                    keyboardOptions = if (requiresPin) {
                                        KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                                    } else {
                                        KeyboardOptions.Default
                                    },
                                    isError = confirmInput.isNotEmpty() && !isInputValid,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (confirmInput.isNotEmpty() && !isInputValid) {
                                    Text(
                                        text = if (requiresPin) {
                                            if (isBn) "পিন সঠিক নয়" else "Incorrect PIN"
                                        } else {
                                            if (isBn) "ঠিক CLEAR লিখুন" else "Must match CLEAR"
                                        },
                                        fontSize = 11.5.sp,
                                        color = dangerText,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Action Bar
                Surface(
                    color = tokens.cardBg,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        HorizontalDivider(color = tokens.dividerColor)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (confirmStep) {
                                        confirmStep = false
                                        confirmInput = ""
                                    } else {
                                        onDismiss()
                                    }
                                },
                                enabled = !isExecuting,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(0.38f)
                            ) {
                                Text(
                                    text = if (confirmStep) {
                                        if (isBn) "পিছনে" else "Back"
                                    } else {
                                        if (isBn) "বাতিল" else "Cancel"
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (!confirmStep) {
                                Button(
                                    onClick = {
                                        confirmInput = ""
                                        confirmStep = true
                                    },
                                    enabled = selectedModules.isNotEmpty(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    modifier = Modifier.weight(0.62f)
                                ) {
                                    Text(
                                        text = if (isBn) {
                                            "পরবর্তী: যাচাই (${selectedModules.size})"
                                        } else {
                                            "Next: Verify (${selectedModules.size})"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        isExecuting = true
                                        onExecuteClear(selectedModules, createSafetyBackup) { success ->
                                            isExecuting = false
                                            if (success) {
                                                onDismiss()
                                            }
                                        }
                                    },
                                    enabled = isInputValid && !isExecuting && selectedModules.isNotEmpty(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(0.62f)
                                ) {
                                    if (isExecuting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (isBn) "রিসেট হচ্ছে..." else "Resetting...",
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteSweep,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (isBn) "হ্যাঁ, রিসেট করুন" else "Confirm & Reset",
                                            fontWeight = FontWeight.Bold
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
}
