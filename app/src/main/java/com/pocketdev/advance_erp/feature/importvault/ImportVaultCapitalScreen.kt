@file:OptIn(ExperimentalLayoutApi::class)

package com.pocketdev.advance_erp.feature.importvault

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.unit.sp
import com.pocketdev.advance_erp.core.designsystem.AutoShrinkSingleLineText
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.core.designsystem.LocalDashboardTokens
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.StatusWarningContainer
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.ExpenseAllocationRule
import com.pocketdev.advance_erp.domain.model.AgingSummary
import com.pocketdev.advance_erp.domain.model.CashRecoverySummary
import com.pocketdev.advance_erp.domain.model.ExpectedRemainingProfitEstimate
import com.pocketdev.advance_erp.domain.model.ExpenseAllocationMode
import com.pocketdev.advance_erp.domain.model.UnknownAttributionBucket
import com.pocketdev.advance_erp.domain.model.VaultCostLayerRow
import com.pocketdev.advance_erp.domain.model.VaultSaleSourceRow
import com.pocketdev.advance_erp.domain.model.VaultScopeType

@Composable
fun ImportVaultPortfolioCapitalScreen(
    uiState: ImportVaultUiState,
    onBack: () -> Unit,
    onSelectAllocationRule: (String?) -> Unit,
    onOpenExport: () -> Unit = {},
    onCreateAllocationRule: (
        ruleName: String,
        allocationBasis: String,
        periodStartMillis: Long,
        periodEndMillis: Long,
        targetScopeType: VaultScopeType,
        activateImmediately: Boolean,
        notes: String?
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onToggleAllocationRuleActive: (ruleId: String, isActive: Boolean) -> Unit = { _, _ -> },
    onNavigateToRoute: (String) -> Unit
) {
    val language = uiState.language
    val isBn = language == AppLanguage.BANGLA
    val overview = uiState.overview
    val tokens = LocalDashboardTokens.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(tokens.pageBgTop, tokens.pageBgBottom)))
    ) {
        VaultSubScreenTopBar(
            title = if (isBn) "মূলধন ও স্টক বয়স (Capital Intel)" else "Capital Intelligence",
            subtitle = overview?.let {
                if (isBn) "তারিখ: ${Formatters.formatDate(it.asOfMillis, language)}" else "As of ${Formatters.formatDate(it.asOfMillis, language)}"
            }.orEmpty(),
            onBack = onBack
        ) {
            IconButton(onClick = onOpenExport, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Outlined.Download,
                    contentDescription = if (isBn) "এক্সপোর্ট" else "Export Report",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (overview != null) {
            val totalSupplierPaid = overview.purchaseOrders.sumOf { it.supplierPaidBdtMinor }
            val totalSupplierPayable = overview.purchaseOrders.sumOf { it.supplierPayableBdtMinor }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                capitalIntelligenceItems(
                    scopeTitle = if (isBn) "পোর্টফোলিও মূলধন" else "Portfolio Capital",
                    cashRecovery = overview.cashRecovery,
                    lockedInventoryCapitalMinor = overview.totalLockedInventoryCapitalMinor,
                    supplierPaidMinor = totalSupplierPaid,
                    supplierPayableMinor = totalSupplierPayable,
                    expectedRemainingProfit = overview.expectedRemainingProfit,
                    agingSummary = overview.agingSummary,
                    unknownBucket = overview.unknownAttribution,
                    expenseAllocationMode = overview.expenseAllocationMode,
                    activeRuleId = overview.activeAllocationRuleId,
                    unallocatedSharedOverheadMinor = overview.unallocatedSharedOverheadMinor,
                    allocatedSharedOverheadMinor = overview.totalAllocatedSharedOverheadMinor,
                    availableRules = uiState.filterOptions.allocationRules,
                    costLayers = overview.portfolioCostLayers,
                    saleSources = overview.portfolioSaleSources,
                    language = language,
                    onSelectAllocationRule = onSelectAllocationRule,
                    onCreateAllocationRule = onCreateAllocationRule,
                    onToggleAllocationRuleActive = onToggleAllocationRuleActive,
                    onNavigateToRoute = onNavigateToRoute
                )
            }
        }
    }
}

fun LazyListScope.capitalIntelligenceItems(
    scopeTitle: String,
    cashRecovery: CashRecoverySummary,
    lockedInventoryCapitalMinor: Long,
    supplierPaidMinor: Long?,
    supplierPayableMinor: Long?,
    expectedRemainingProfit: ExpectedRemainingProfitEstimate,
    agingSummary: AgingSummary,
    unknownBucket: UnknownAttributionBucket?,
    expenseAllocationMode: ExpenseAllocationMode,
    activeRuleId: String?,
    unallocatedSharedOverheadMinor: Long?,
    allocatedSharedOverheadMinor: Long,
    availableRules: List<ExpenseAllocationRule>,
    costLayers: List<VaultCostLayerRow>,
    saleSources: List<VaultSaleSourceRow>,
    language: AppLanguage,
    onSelectAllocationRule: ((String?) -> Unit)?,
    onCreateAllocationRule: ((
        ruleName: String,
        allocationBasis: String,
        periodStartMillis: Long,
        periodEndMillis: Long,
        targetScopeType: VaultScopeType,
        activateImmediately: Boolean,
        notes: String?
    ) -> Unit)? = null,
    onToggleAllocationRuleActive: ((ruleId: String, isActive: Boolean) -> Unit)? = null,
    onNavigateToRoute: (String) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA

    item(key = "cap_hero_$scopeTitle") {
        VaultHeroCard {
            Text(
                text = scopeTitle,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
            AutoShrinkSingleLineText(
                text = ImportVaultUiLogic.formatBdtNoBreak(cashRecovery.totalInvestedCapitalMinor, language),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                minFontSizeSp = 16f
            )
            Text(
                text = if (isBn) {
                    "বদ্ধ স্টক মূল্য ${ImportVaultUiLogic.formatBdt(lockedInventoryCapitalMinor, language)} · আদায়কৃত ${ImportVaultUiLogic.formatBdt(cashRecovery.attributableCashCollectedMinor, language)}"
                } else {
                    "Locked Stock ${ImportVaultUiLogic.formatBdt(lockedInventoryCapitalMinor, language)} · Collected ${ImportVaultUiLogic.formatBdt(cashRecovery.attributableCashCollectedMinor, language)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
    }

    item(key = "cap_recovery") {
        CapitalRecoveryProgressCard(cashRecovery = cashRecovery, language = language)
    }

    item(key = "cap_positions") {
        VaultCard {
            Text(
                text = if (isBn) "মূলধন, স্টক ও সরবরাহকারী হিসাব" else "Capital, Stock & Payables Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            VaultKpiPairRow(
                leftLabel = if (isBn) "বদ্ধ ইনভেন্টরি মূলধন" else "Locked Stock Capital",
                leftValue = ImportVaultUiLogic.formatBdt(lockedInventoryCapitalMinor, language),
                leftColor = MaterialTheme.colorScheme.primary,
                rightLabel = if (isBn) "আদায়যোগ্য বকেয়া (AR)" else "Attributable Receivable",
                rightValue = ImportVaultUiLogic.formatBdt(cashRecovery.attributableReceivableMinor, language),
                rightColor = StatusWarning
            )
            if (supplierPaidMinor != null && supplierPayableMinor != null) {
                VaultKpiPairRow(
                    leftLabel = if (isBn) "সরবরাহকারী পরিশোধ" else "Supplier Cash Paid",
                    leftValue = ImportVaultUiLogic.formatBdt(supplierPaidMinor, language),
                    rightLabel = if (isBn) "সরবরাহকারী বকেয়া (AP)" else "Supplier Payable",
                    rightValue = ImportVaultUiLogic.formatBdt(supplierPayableMinor, language),
                    rightColor = if (supplierPayableMinor > 0L) StatusError else StatusSuccess
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onNavigateToRoute("accounts") },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(if (isBn) "হিসাব/পেমেন্ট" else "Accounts")
                }
                OutlinedButton(
                    onClick = { onNavigateToRoute("ledger") },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(if (isBn) "কেন্দ্রীয় লেজার" else "Central Ledger")
                }
            }
        }
    }

    item(key = "cap_expected_profit") {
        VaultCard {
            Text(
                text = if (isBn) "অবশিষ্ট স্টক থেকে প্রত্যাশিত গ্রস লাভ" else "Expected Remaining Gross Profit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${if (isBn) "ভিত্তি" else "Price Basis"}: ${expectedRemainingProfit.priceBasisLabel} · ${Formatters.formatDate(expectedRemainingProfit.asOfMillis, language)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            VaultKpiPairRow(
                leftLabel = if (isBn) "বিক্রয়যোগ্য অবশিষ্ট" else "Sellable Remaining Qty",
                leftValue = Formatters.formatNumber(expectedRemainingProfit.remainingGoodQty.toLong(), language),
                rightLabel = if (isBn) "বহন খরচ (Carrying Cost)" else "Carrying Cost",
                rightValue = ImportVaultUiLogic.formatBdt(expectedRemainingProfit.lockedGoodInventoryCostMinor, language)
            )
            VaultKpiPairRow(
                leftLabel = if (isBn) "প্রত্যাশিত বিক্রয় মূল্য" else "Expected Selling Revenue",
                leftValue = ImportVaultUiLogic.formatBdt(expectedRemainingProfit.expectedSellingRevenueMinor, language),
                rightLabel = if (isBn) "প্রত্যাশিত গ্রস লাভ" else "Expected Gross Profit",
                rightValue = ImportVaultUiLogic.formatBdt(expectedRemainingProfit.expectedGrossProfitMinor, language),
                rightColor = if (expectedRemainingProfit.expectedGrossProfitMinor >= 0L) StatusSuccess else StatusError
            )
            if (expectedRemainingProfit.missingPriceSkuCount > 0) {
                Text(
                    text = if (isBn) {
                        "সতর্কতা: ${Formatters.formatNumber(expectedRemainingProfit.missingPriceSkuCount.toLong(), language)}টি SKU-তে বিক্রয়মূল্য সেট করা নেই।"
                    } else {
                        "Warning: ${expectedRemainingProfit.missingPriceSkuCount} SKUs have no configured selling price."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusWarning
                )
            }
        }
    }

    item(key = "cap_aging") {
        StockAgingBucketCard(agingSummary = agingSummary, language = language)
    }

    if (unknownBucket != null) {
        item(key = "cap_unknown_bucket") {
            VaultCard(borderColor = StatusWarning.copy(alpha = 0.4f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = StatusWarning)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBn) "লিগ্যাসি / অনির্ধারিত ও নন-মার্চেন্ডাইজ বাকেট" else "Legacy / Unattributed & Non-Merchandise",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${if (isBn) "বিক্রয় অ্যাট্রিবিউশন কভারেজ" else "Attribution Coverage"}: ${ImportVaultUiLogic.formatPercent(unknownBucket.revenueAttributionCoveragePercent, language)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                VaultKpiPairRow(
                    leftLabel = if (isBn) "লিগ্যাসি/অনির্ধারিত আয়" else "Unattributed Revenue",
                    leftValue = ImportVaultUiLogic.formatBdt(unknownBucket.unattributedRevenueMinor, language),
                    rightLabel = if (isBn) "অনির্ধারিত স্টক (Opening/Adj)" else "Unattributed Inventory",
                    rightValue = ImportVaultUiLogic.formatBdt(unknownBucket.unattributedInventoryCostMinor, language)
                )
                VaultKpiPairRow(
                    leftLabel = if (isBn) "ইনভয়েস-বহির্ভূত কালেকশন" else "Unattributed Collections",
                    leftValue = ImportVaultUiLogic.formatBdt(unknownBucket.unattributedCollectionsMinor, language),
                    rightLabel = if (isBn) "পূর্বের ওপেনিং বকেয়া" else "Opening Due Receivable",
                    rightValue = ImportVaultUiLogic.formatBdt(unknownBucket.openingDueReceivableMinor, language)
                )
                VaultKpiTile(
                    label = if (isBn) "নন-মার্চেন্ডাইজ চার্জ (Invoice taxMinor)" else "Non-Merchandise Charges (taxMinor)",
                    value = ImportVaultUiLogic.formatBdt(unknownBucket.nonMerchandiseAdditionalChargesMinor, language)
                )
            }
        }
    }

    if (onCreateAllocationRule != null || unallocatedSharedOverheadMinor != null || allocatedSharedOverheadMinor > 0L || availableRules.isNotEmpty()) {
        item(key = "cap_overhead_rules") {
            SharedOverheadPolicySection(
                expenseAllocationMode = expenseAllocationMode,
                activeRuleId = activeRuleId,
                unallocatedSharedOverheadMinor = unallocatedSharedOverheadMinor ?: 0L,
                allocatedSharedOverheadMinor = allocatedSharedOverheadMinor,
                availableRules = availableRules,
                language = language,
                onSelectAllocationRule = onSelectAllocationRule,
                onCreateAllocationRule = onCreateAllocationRule,
                onToggleAllocationRuleActive = onToggleAllocationRuleActive
            )
        }
    }

    if (costLayers.isNotEmpty()) {
        item(key = "cap_layers_drill") {
            VaultExpandableSection(
                title = if (isBn) "সক্রিয় FIFO স্টক লেয়ারসমূহ" else "Source FIFO Cost Layers",
                subtitle = if (isBn) "উৎপত্তি তারিখ ও গুদামভিত্তিক অবশিষ্ট স্টক" else "Carrying cost & aging by origin receipt date",
                badgeText = Formatters.formatNumber(costLayers.size.toLong(), language)
            ) {
                costLayers.forEach { layer ->
                    val prodTitle = if (isBn) layer.productNameBn.ifBlank { layer.productNameEn } else layer.productNameEn
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "$prodTitle ${layer.variantLabel}".trim(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${if (isBn) layer.agingBucket.labelBn else layer.agingBucket.labelEn} (${layer.ageDays}d)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "${layer.warehouseName} · ${layer.receiptNumber} · ${Formatters.formatDate(layer.originReceiptDate, language)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${if (isBn) "অবশিষ্ট" else "Rem"}: ${layer.remainingQuantity}/${layer.initialQuantity} @ ${ImportVaultUiLogic.formatBdt(layer.unitLandedCostBdtMinor, language)} = ${ImportVaultUiLogic.formatBdt(layer.carryingValueMinor, language)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    if (saleSources.isNotEmpty()) {
        item(key = "cap_settlements_drill") {
            VaultExpandableSection(
                title = if (isBn) "ইনভয়েস সেটেলমেন্ট ও নগদ আদায়" else "Attributable Invoice Settlements",
                subtitle = if (isBn) "প্রকৃত payment_allocations অনুযায়ী" else "Backed by invoice-linked payment allocations",
                badgeText = Formatters.formatNumber(saleSources.size.toLong(), language)
            ) {
                saleSources.forEach { row ->
                    val prodName = if (isBn) row.productNameBn.ifBlank { row.productNameEn } else row.productNameEn
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "${row.invoiceNumber} · $prodName",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = ImportVaultUiLogic.formatBdt(row.netRevenueMinor, language),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${if (isBn) "আদায়" else "Paid"} ${ImportVaultUiLogic.formatBdt(row.collectedCashMinor, language)} · ${if (isBn) "বকেয়া" else "Due"} ${ImportVaultUiLogic.formatBdt(row.receivableMinor, language)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedOverheadPolicySection(
    expenseAllocationMode: ExpenseAllocationMode,
    activeRuleId: String?,
    unallocatedSharedOverheadMinor: Long,
    allocatedSharedOverheadMinor: Long,
    availableRules: List<ExpenseAllocationRule>,
    language: AppLanguage,
    onSelectAllocationRule: ((String?) -> Unit)?,
    onCreateAllocationRule: ((
        ruleName: String,
        allocationBasis: String,
        periodStartMillis: Long,
        periodEndMillis: Long,
        targetScopeType: VaultScopeType,
        activateImmediately: Boolean,
        notes: String?
    ) -> Unit)?,
    onToggleAllocationRuleActive: ((ruleId: String, isActive: Boolean) -> Unit)?
) {
    val isBn = language == AppLanguage.BANGLA
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog && onCreateAllocationRule != null) {
        CreateOverheadRuleDialog(
            language = language,
            onDismiss = { showCreateDialog = false },
            onSave = { name, basis, startMs, endMs, target, activate, notes ->
                showCreateDialog = false
                onCreateAllocationRule(name, basis, startMs, endMs, target, activate, notes)
            }
        )
    }

    VaultCard {
        Text(
            text = if (isBn) "শেয়ারড ওভারহেড বণ্টন নীতি" else "Shared Overhead Policy",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (expenseAllocationMode == ExpenseAllocationMode.UNALLOCATED) {
                if (isBn) "বর্তমান মোড: ডিফল্ট (UNALLOCATED) — Attributed Net Profit হিসেবে দেখানো হচ্ছে।" else "Current Mode: Default (UNALLOCATED) — shown as Attributed Net Profit."
            } else {
                if (isBn) "বর্তমান মোড: Opt-in Allocated — নির্বাচিত রুল অনুযায়ী বণ্টনকৃত।" else "Current Mode: Opt-in Allocated — apportioned by active rule."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        VaultKpiPairRow(
            leftLabel = if (isBn) "অবন্টিত শেয়ারড ওভারহেড" else "Unallocated Pool",
            leftValue = ImportVaultUiLogic.formatBdt(unallocatedSharedOverheadMinor, language),
            rightLabel = if (isBn) "বণ্টনকৃত ওভারহেড" else "Allocated Overhead",
            rightValue = ImportVaultUiLogic.formatBdt(allocatedSharedOverheadMinor, language)
        )
        if (onSelectAllocationRule != null && availableRules.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterOptionChip(
                    label = if (isBn) "ডিফল্ট (Unallocated)" else "Default (Unallocated)",
                    selected = activeRuleId == null,
                    onClick = { onSelectAllocationRule(null) }
                )
                availableRules.forEach { rule ->
                    val statusTag = if (rule.isActive) " ★" else ""
                    FilterOptionChip(
                        label = "${rule.ruleName} (${rule.allocationBasis})$statusTag",
                        selected = activeRuleId == rule.id,
                        onClick = { onSelectAllocationRule(rule.id) }
                    )
                }
            }
            val selectedRule = availableRules.find { it.id == activeRuleId }
            if (selectedRule != null && onToggleAllocationRuleActive != null) {
                OutlinedButton(
                    onClick = { onToggleAllocationRuleActive(selectedRule.id, !selectedRule.isActive) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (selectedRule.isActive) {
                            if (isBn) "'${selectedRule.ruleName}' স্থায়ীভাবে নিষ্ক্রিয় করুন" else "Deactivate '${selectedRule.ruleName}' as Default"
                        } else {
                            if (isBn) "'${selectedRule.ruleName}' স্থায়ীভাবে সক্রিয় করুন" else "Set '${selectedRule.ruleName}' Active"
                        }
                    )
                }
            }
        }
        if (onCreateAllocationRule != null) {
            OutlinedButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isBn) "+ ওভারহেড বণ্টন রুল তৈরি করুন (Opt-in)" else "+ Create Shared Overhead Rule (Opt-in)",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CreateOverheadRuleDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (
        ruleName: String,
        allocationBasis: String,
        periodStartMillis: Long,
        periodEndMillis: Long,
        targetScopeType: VaultScopeType,
        activateImmediately: Boolean,
        notes: String?
    ) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val context = LocalContext.current
    val now = remember { System.currentTimeMillis() }

    var ruleName by remember { mutableStateOf(if (isBn) "মাসিক ওভারহেড রুল" else "Monthly Overhead Rule") }
    var selectedBasis by remember { mutableStateOf("LANDED_VALUE") }
    var selectedTarget by remember { mutableStateOf(VaultScopeType.SHIPMENT) }
    var periodStartMillis by remember {
        mutableStateOf(
            ImportVaultUiLogic.computeDateRangeForPreset(VaultDatePreset.THIS_MONTH, nowMillis = now).first
                ?: (now - 30L * 24 * 3600 * 1000)
        )
    }
    var periodEndMillis by remember { mutableStateOf(ImportVaultUiLogic.endOfDayMillis(now)) }
    var activateImmediately by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    val bases = listOf(
        "LANDED_VALUE" to (if (isBn) "ল্যান্ডেড মূল্য (Landed Value)" else "Landed Value"),
        "NET_REVENUE" to (if (isBn) "নিট বিক্রয় (Net Revenue)" else "Net Revenue"),
        "QUANTITY" to (if (isBn) "পরিমাণ (Same Unit Qty)" else "Quantity (Same Unit Only)"),
        "WEIGHT" to (if (isBn) "যাচাইকৃত ওজন (Billable Weight)" else "Billable Weight (Shipment)")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isBn) "নতুন শেয়ারড ওভারহেড রুল" else "Create Shared Overhead Rule",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text(if (isBn) "রুলের নাম" else "Rule Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (isBn) "বণ্টনের ভিত্তি (Allocation Basis)" else "Allocation Basis",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bases.forEach { (code, label) ->
                        FilterOptionChip(
                            label = label,
                            selected = selectedBasis == code,
                            onClick = {
                                selectedBasis = code
                                if (code == "WEIGHT") selectedTarget = VaultScopeType.SHIPMENT
                            }
                        )
                    }
                }
                if (selectedBasis == "QUANTITY") {
                    Text(
                        text = if (isBn) "নোট: শুধু একই একক (যেমন সব PCS বা সব PAIR) হলে Quantity বণ্টন অনুমোদিত।" else "Note: QUANTITY allocation is only allowed when all scopes share a single uniform UOM.",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusWarning
                    )
                }
                Text(
                    text = if (isBn) "লক্ষ্য স্কোপ (Target Scope)" else "Target Scope",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterOptionChip(
                        label = if (isBn) "চালান (Shipments)" else "Shipments",
                        selected = selectedTarget == VaultScopeType.SHIPMENT,
                        onClick = { selectedTarget = VaultScopeType.SHIPMENT }
                    )
                    if (selectedBasis != "WEIGHT") {
                        FilterOptionChip(
                            label = if (isBn) "ক্রয় আদেশ (POs)" else "Purchase Orders",
                            selected = selectedTarget == VaultScopeType.PURCHASE_ORDER,
                            onClick = { selectedTarget = VaultScopeType.PURCHASE_ORDER }
                        )
                    }
                }
                Text(
                    text = if (isBn) "খরচের সময়সীমা (Period)" else "Expense Period",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            openPlatformDatePicker(context, periodStartMillis) { picked ->
                                periodStartMillis = ImportVaultUiLogic.startOfDayMillis(picked)
                            }
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${if (isBn) "শুরু" else "From"}: ${Formatters.formatDate(periodStartMillis, language)}", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            openPlatformDatePicker(context, periodEndMillis) { picked ->
                                periodEndMillis = ImportVaultUiLogic.endOfDayMillis(picked)
                            }
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${if (isBn) "শেষ" else "To"}: ${Formatters.formatDate(periodEndMillis, language)}", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isBn) "তৈরির সাথে সাথে সক্রিয় করুন" else "Activate Immediately",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = activateImmediately, onCheckedChange = { activateImmediately = it })
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isBn) "নোট (ঐচ্ছিক)" else "Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = minOf(periodStartMillis, periodEndMillis)
                    val e = maxOf(periodStartMillis, periodEndMillis)
                    onSave(
                        ruleName.trim().ifBlank { "Overhead Rule" },
                        selectedBasis,
                        s,
                        e,
                        selectedTarget,
                        activateImmediately,
                        notes.trim().ifBlank { null }
                    )
                },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(if (isBn) "সংরক্ষণ করুন" else "Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (isBn) "বাতিল" else "Cancel")
            }
        }
    )
}
