@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.pocketdev.advance_erp.feature.importvault

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.core.designsystem.StatusInfo
import com.pocketdev.advance_erp.core.designsystem.StatusInfoContainer
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.StatusWarningContainer
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.core.util.VaultExportFormat
import com.pocketdev.advance_erp.core.util.VaultExportPackage
import com.pocketdev.advance_erp.core.util.VaultExportScope
import com.pocketdev.advance_erp.domain.model.VaultDocumentCategory
import com.pocketdev.advance_erp.domain.model.VaultScopeType

data class VaultExportSheetLaunchConfig(
    val scope: VaultExportScope = VaultExportScope.PORTFOLIO,
    val scopeId: String? = null,
    val variantId: String? = null,
    val parentScopeType: VaultScopeType? = null,
    val parentScopeId: String? = null,
    val targetTitle: String = ""
)

@Composable
fun ImportVaultExportSheet(
    launchConfig: VaultExportSheetLaunchConfig,
    uiState: ImportVaultUiState,
    onDismiss: () -> Unit,
    onRequestPreview: (VaultExportSheetLaunchConfig, VaultDatePreset?) -> Unit,
    onGenerateAndPreview: (VaultExportSheetLaunchConfig, VaultExportFormat, VaultDatePreset?, Boolean) -> Unit,
    onGenerateAndShare: (VaultExportSheetLaunchConfig, VaultExportFormat, VaultDatePreset?) -> Unit
) {
    val language = uiState.language
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var activeConfig by remember(launchConfig) { mutableStateOf(launchConfig) }
    var selectedFormat by remember { mutableStateOf(VaultExportFormat.PDF) }
    var datePresetOverride by remember { mutableStateOf<VaultDatePreset?>(null) }
    var saveImmutableSnapshot by remember { mutableStateOf(false) }

    LaunchedEffect(activeConfig, datePresetOverride) {
        onRequestPreview(activeConfig, datePresetOverride)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isBn) "আর্কাইভ রিপোর্ট এক্সপোর্ট (PDF / Excel)" else "Export Vault Report (PDF / XLSX)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Scope options
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isBn) "রিপোর্ট স্কোপ" else "Report Scope",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (launchConfig.scope != VaultExportScope.PORTFOLIO) {
                        val scopeLabel = when (launchConfig.scope) {
                            VaultExportScope.SHIPMENT -> if (isBn) "এই চালান" else "This Shipment"
                            VaultExportScope.PURCHASE_ORDER -> if (isBn) "এই PO" else "This PO"
                            VaultExportScope.PRODUCT -> if (isBn) "এই পণ্য" else "This Product"
                            VaultExportScope.VARIANT -> if (isBn) "এই ভ্যারিয়েন্ট" else "This Variant"
                            VaultExportScope.PORTFOLIO -> ""
                        }
                        FilterOptionChip(
                            label = "$scopeLabel (${launchConfig.targetTitle.take(18)})",
                            selected = activeConfig == launchConfig,
                            onClick = { activeConfig = launchConfig }
                        )
                    }
                    FilterOptionChip(
                        label = if (isBn) "সম্পূর্ণ পোর্টফোলিও" else "Portfolio",
                        selected = activeConfig.scope == VaultExportScope.PORTFOLIO,
                        onClick = {
                            activeConfig = VaultExportSheetLaunchConfig(
                                scope = VaultExportScope.PORTFOLIO,
                                targetTitle = if (isBn) "পোর্টফোলিও" else "Portfolio"
                            )
                        }
                    )
                    if (launchConfig.scope == VaultExportScope.PRODUCT &&
                        uiState.portfolioProducts.find { it.productId == launchConfig.scopeId }?.variants?.isNotEmpty() == true
                    ) {
                        val prod = uiState.portfolioProducts.find { it.productId == launchConfig.scopeId }
                        prod?.variants?.take(4)?.forEach { v ->
                            FilterOptionChip(
                                label = v.variantLabel,
                                selected = activeConfig.scope == VaultExportScope.VARIANT && activeConfig.variantId == v.variantId,
                                onClick = {
                                    activeConfig = launchConfig.copy(
                                        scope = VaultExportScope.VARIANT,
                                        variantId = v.variantId ?: v.sku,
                                        targetTitle = "${prod.productNameEn} (${v.variantLabel})"
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Format toggle
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isBn) "ফাইল ফরম্যাট" else "File Format",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormatToggleButton(
                        selected = selectedFormat == VaultExportFormat.PDF,
                        icon = Icons.Outlined.PictureAsPdf,
                        title = if (isBn) "PDF রিপোর্ট" else "PDF Report",
                        subtitle = "A4 Bangla & English",
                        onClick = { selectedFormat = VaultExportFormat.PDF },
                        modifier = Modifier.weight(1f)
                    )
                    FormatToggleButton(
                        selected = selectedFormat == VaultExportFormat.XLSX,
                        icon = Icons.Outlined.TableChart,
                        title = if (isBn) "Excel (.xlsx)" else "Excel (.xlsx)",
                        subtitle = if (isBn) "৮টি পূর্ণাঙ্গ শিট" else "8 Structured Sheets",
                        onClick = { selectedFormat = VaultExportFormat.XLSX },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Date range override toggle
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isBn) "তারিখ পরিসর" else "Date Range",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterOptionChip(
                        label = if (isBn) "বর্তমান ফিল্টার" else "Current View",
                        selected = datePresetOverride == null,
                        onClick = { datePresetOverride = null }
                    )
                    listOf(
                        VaultDatePreset.LIFETIME to (if (isBn) "সর্বমোট (Lifetime)" else "Lifetime"),
                        VaultDatePreset.TODAY to (if (isBn) "আজ" else "Today"),
                        VaultDatePreset.THIS_MONTH to (if (isBn) "এই মাস" else "This Month"),
                        VaultDatePreset.LAST_30_DAYS to (if (isBn) "৩০ দিন" else "30 Days")
                    ).forEach { (preset, label) ->
                        FilterOptionChip(
                            label = label,
                            selected = datePresetOverride == preset,
                            onClick = { datePresetOverride = preset }
                        )
                    }
                }
            }

            // Immutable Snapshot Switch (only meaningful when scope maps to Vault folder or Portfolio)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, colors.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = if (isBn) "স্থায়ী স্ন্যাপশট সংরক্ষণ করুন" else "Save Immutable Vault Snapshot",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBn) {
                                "ফোল্ডারের নথিতে অপরিবর্তনীয় কপি সেভ হবে ও ব্যাকআপে যুক্ত হবে।"
                            } else {
                                "Stores a frozen timestamped copy under Vault Documents & Backup."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = saveImmutableSnapshot,
                        onCheckedChange = { saveImmutableSnapshot = it }
                    )
                }
            }

            // Live Reconciled Totals & Warnings Preview
            val preview = uiState.exportPreviewPackage
            if (preview == null) {
                VaultCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            text = if (isBn) "লাইভ হিসাব সমন্বয় করা হচ্ছে..." else "Reconciling live totals for export...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                ExportPreviewSummaryCard(pkg = preview, language = language)
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onGenerateAndShare(activeConfig, selectedFormat, datePresetOverride) },
                    enabled = !uiState.isExporting,
                    modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isBn) "শেয়ার" else "Share", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        onGenerateAndPreview(
                            activeConfig,
                            selectedFormat,
                            datePresetOverride,
                            saveImmutableSnapshot
                        )
                    },
                    enabled = !uiState.isExporting,
                    modifier = Modifier.weight(1.4f).heightIn(min = 50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    if (uiState.isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = colors.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = if (saveImmutableSnapshot) Icons.Outlined.BookmarkAdded else Icons.Outlined.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isBn) {
                            if (selectedFormat == VaultExportFormat.PDF) "PDF তৈরি ও দেখুন" else "XLSX তৈরি ও খুলুন"
                        } else {
                            if (selectedFormat == VaultExportFormat.PDF) "Generate & Preview" else "Generate & Open"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.heightIn(min = 12.dp))
        }
    }
}

@Composable
private fun FormatToggleButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) colors.primary else colors.onSurfaceVariant)
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) colors.primary else colors.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExportPreviewSummaryCard(
    pkg: VaultExportPackage,
    language: AppLanguage
) {
    val isBn = language == AppLanguage.BANGLA
    val t = pkg.totals
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = StatusInfoContainer,
            border = BorderStroke(1.dp, StatusInfo.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isBn) pkg.titleBn else pkg.titleEn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = StatusInfo
                )
                Text(
                    text = pkg.filterSummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VaultKpiPairRow(
                    leftLabel = if (isBn) "বিনিয়োগ" else "Invested",
                    leftValue = ImportVaultUiLogic.formatBdt(t.totalInvestedCapitalMinor, language),
                    rightLabel = if (isBn) "নিট আয়" else "Net Revenue",
                    rightValue = ImportVaultUiLogic.formatBdt(t.netSalesRevenueMinor, language)
                )
                VaultKpiPairRow(
                    leftLabel = t.profitLabel,
                    leftValue = ImportVaultUiLogic.formatBdt(t.attributedNetProfitMinor, language),
                    rightLabel = if (isBn) "বদ্ধ স্টক" else "Locked Stock",
                    rightValue = ImportVaultUiLogic.formatBdt(t.lockedInventoryCapitalMinor, language)
                )
                Text(
                    text = if (isBn) {
                        "অন্তর্ভুক্ত সারি: ${pkg.products.size} পণ্য · ${pkg.variants.size} ভ্যারিয়েন্ট · ${pkg.salesAllocations.size} বিক্রয় · ${pkg.documentsIndex.size} নথি"
                    } else {
                        "Rows included: ${pkg.products.size} products · ${pkg.variants.size} variants · ${pkg.salesAllocations.size} sales · ${pkg.documentsIndex.size} docs"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (pkg.warnings.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = StatusWarningContainer,
                border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (isBn) "সতর্কতা (এক্সপোর্টে অন্তর্ভুক্ত হবে)" else "Warnings Included in Export",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusWarning
                        )
                    }
                    pkg.warnings.forEach { warning ->
                        Text(
                            text = "• $warning",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VaultSaveAttachmentDialog(
    initialTitle: String,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (title: String, category: VaultDocumentCategory) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var selectedCategory by remember { mutableStateOf(VaultDocumentCategory.ATTACHMENTS) }

    val selectableCategories = listOf(
        VaultDocumentCategory.PURCHASE,
        VaultDocumentCategory.PACKING,
        VaultDocumentCategory.FREIGHT,
        VaultDocumentCategory.RECEIVING,
        VaultDocumentCategory.SALES,
        VaultDocumentCategory.PAYMENTS,
        VaultDocumentCategory.ATTACHMENTS
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isBn) "আর্কাইভে নথি যোগ করুন" else "Save Vault Attachment")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isBn) "নথির শিরোনাম" else "Document Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (isBn) "ক্যাটাগরি নির্বাচন করুন" else "Select Category",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    selectableCategories.forEach { cat ->
                        FilterOptionChip(
                            label = vaultDocCategoryLabel(cat, language),
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }
                Text(
                    text = if (isBn) {
                        "নিরাপদ লোকাল কপি অ্যাপের vault_attachments/ ফোল্ডারে সংরক্ষিত হবে।"
                    } else {
                        "A private copy will be saved in app vault_attachments/ and included in backups."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim().ifBlank { initialTitle.ifBlank { "Vault Document" } }
                    onSave(cleanTitle, selectedCategory)
                },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(if (isBn) "সংরক্ষণ করুন" else "Save File")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (isBn) "বাতিল" else "Cancel")
            }
        }
    )
}

@Composable
fun VaultAttachmentDeleteConfirmDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isBn) "শুধু অ্যাটাচমেন্ট মুছবেন?" else "Delete Attachment Only?")
        },
        text = {
            Text(
                text = if (isBn) {
                    "এতে শুধু Import Vault-এর এই সংযুক্ত ফাইল/স্ন্যাপশট মুছে যাবে। মূল ক্রয় আদেশ, চালান, বিক্রয় বা হিসাবের কোনো লেনদেন পরিবর্তন বা ডিলিট হবে না।"
                } else {
                    "This deletes ONLY this Vault attachment/snapshot copy. Underlying ERP Purchase Orders, Shipments, Invoices, or Ledger records are never touched."
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(if (isBn) "হ্যাঁ, শুধু ফাইল মুছুন" else "Delete Attachment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (isBn) "বাতিল" else "Cancel")
            }
        }
    )
}

internal fun vaultDocCategoryLabel(cat: VaultDocumentCategory, language: AppLanguage): String {
    val isBn = language == AppLanguage.BANGLA
    return when (cat) {
        VaultDocumentCategory.PURCHASE -> if (isBn) "ক্রয়" else "Purchase"
        VaultDocumentCategory.PACKING -> if (isBn) "প্যাকিং" else "Packing"
        VaultDocumentCategory.FREIGHT -> if (isBn) "ফ্রেইট" else "Freight"
        VaultDocumentCategory.RECEIVING -> if (isBn) "গ্রহণ" else "Receiving"
        VaultDocumentCategory.SALES -> if (isBn) "বিক্রয়" else "Sales"
        VaultDocumentCategory.PAYMENTS -> if (isBn) "পেমেন্ট" else "Payments"
        VaultDocumentCategory.REPORTS -> if (isBn) "রিপোর্ট" else "Reports"
        VaultDocumentCategory.ATTACHMENTS -> if (isBn) "অ্যাটাচমেন্ট" else "Attachments"
    }
}
