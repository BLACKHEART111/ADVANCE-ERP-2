@file:OptIn(ExperimentalLayoutApi::class)

package com.pocketdev.advance_erp.feature.importvault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.core.designsystem.LocalDashboardTokens
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.domain.model.ProductVaultRow
import com.pocketdev.advance_erp.domain.model.VariantVaultRow
import kotlin.math.abs

@Composable
fun ImportVaultProductDrillDownScreen(
    product: ProductVaultRow?,
    scopeTitle: String,
    language: AppLanguage,
    sortOption: ProfitSortOption,
    onBack: () -> Unit,
    onSortChange: (ProfitSortOption) -> Unit,
    onOpenExportProduct: () -> Unit = {},
    onOpenExportVariant: (VariantVaultRow) -> Unit = {},
    onNavigateToRoute: (String) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val tokens = LocalDashboardTokens.current
    val colors = MaterialTheme.colorScheme

    var lossOnly by remember { mutableStateOf(false) }
    var inStockOnly by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val prodTopTitle = product?.let {
        if (isBn) it.productNameBn.ifBlank { it.productNameEn } else it.productNameEn
    } ?: (if (isBn) "পণ্যের ভ্যারিয়েন্ট বিশ্লেষণ" else "Product Drill-Down")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(tokens.pageBgTop, tokens.pageBgBottom)))
    ) {
        VaultSubScreenTopBar(
            title = prodTopTitle,
            subtitle = scopeTitle,
            onBack = onBack
        ) {
            if (product != null) {
                IconButton(onClick = onOpenExportProduct, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = if (isBn) "পণ্য রিপোর্ট এক্সপোর্ট" else "Export Product Report",
                        tint = colors.primary
                    )
                }
            }
        }

        if (product == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                VaultCard {
                    Text(
                        text = if (isBn) "পণ্যটির ডাটা বর্তমান ফিল্টারে পাওয়া যায়নি।" else "Product data unavailable under current filters.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(if (isBn) "ফিরে যান" else "Go Back")
                    }
                }
            }
            return
        }

        val filteredVariants = remember(product.variants, sortOption, lossOnly, inStockOnly) {
            val base = product.variants.filter { v ->
                (!lossOnly || v.attributedNetProfitMinor < 0L) &&
                    (!inStockOnly || (v.remainingGoodQty + v.remainingInTransitTransferQty) > 0)
            }
            ImportVaultUiLogic.sortVariants(base, sortOption)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "product_hero") {
                val prodTitle = if (isBn) product.productNameBn.ifBlank { product.productNameEn } else product.productNameEn
                VaultHeroCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VaultProductThumbnail(
                            imageUri = product.primaryImageUri,
                            contentDescription = prodTitle,
                            size = 64.dp
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(prodTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text(
                                text = "${product.baseSku} · ${Formatters.formatNumber(product.variants.size.toLong(), language)} ${if (isBn) "ভ্যারিয়েন্ট" else "variants"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = ImportVaultUiLogic.formatBdt(product.attributedNetProfitMinor, language),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (product.attributedNetProfitMinor >= 0L) Color(0xFF42F5AA) else Color(0xFFFF9DB0)
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${if (isBn) "মার্জিন" else "Margin"}: ${ImportVaultUiLogic.formatPercent(product.netMarginPercent, language)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                        Text(
                            text = "ROI: ${ImportVaultUiLogic.formatPercent(product.roiPercent, language)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF42F5AA)
                        )
                        Text(
                            text = "${if (isBn) "বিক্রয় গতি" else "Velocity"}: ${ImportVaultUiLogic.formatVelocity(product.sellingVelocityPerDay, product.uom, language)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "${if (isBn) "PO-তে অংশ" else "PO Share"}: ${ImportVaultUiLogic.formatPercent(product.profitContributionToPo?.contributionPercent, language)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF81CDD4)
                        )
                        Text(
                            text = "${if (isBn) "চালানে অংশ" else "Shipment Share"}: ${ImportVaultUiLogic.formatPercent(product.profitContributionToShipment?.contributionPercent, language)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFD5A4E0)
                        )
                    }
                }
            }

            item(key = "variant_chart") {
                val slices = product.variants.map { v ->
                    ContributionSlice(
                        id = "${v.productId}:${v.variantId.orEmpty()}",
                        label = v.variantLabel,
                        amountMinor = v.attributedNetProfitMinor,
                        percent = v.profitContributionToProduct?.contributionPercent,
                        marginPercent = v.netMarginPercent
                    )
                }
                ProfitContributionChartCard(
                    title = if (isBn) "ভ্যারিয়েন্ট অনুযায়ী মুনাফা বণ্টন (পণ্যের ভেতরে)" else "Variant Profit Share (Within Product)",
                    parentProfitMinor = product.attributedNetProfitMinor,
                    slices = slices,
                    language = language
                )
            }

            item(key = "variant_sort_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "${if (isBn) "সাজানো:" else "Sort:"} ${variantSortLabel(sortOption, isBn)}",
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            ProfitSortOption.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(variantSortLabel(opt, isBn)) },
                                    onClick = { sortMenuExpanded = false; onSortChange(opt) }
                                )
                            }
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterOptionChip(
                            label = if (isBn) "শুধু লোকসান" else "Loss Only",
                            selected = lossOnly,
                            onClick = { lossOnly = !lossOnly }
                        )
                        FilterOptionChip(
                            label = if (isBn) "শুধু স্টক আছে" else "In Stock Only",
                            selected = inStockOnly,
                            onClick = { inStockOnly = !inStockOnly }
                        )
                    }
                }
            }

            if (filteredVariants.isEmpty()) {
                item(key = "empty_filtered_variants") {
                    VaultCard {
                        Text(
                            text = if (isBn) "নির্বাচিত ফিল্টারে কোনো ভ্যারিয়েন্ট নেই।" else "No variants match the selected filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            } else {
                val maxVariantAbs = maxOf(1L, filteredVariants.maxOf { abs(it.attributedNetProfitMinor) })
                items(filteredVariants, key = { "var_detail_${it.variantId.orEmpty()}_${it.sku}" }) { variant ->
                    ExpandableVariantDetailCard(
                        variant = variant,
                        maxVariantAbs = maxVariantAbs,
                        language = language,
                        onOpenExportVariant = { onOpenExportVariant(variant) },
                        onNavigateToRoute = onNavigateToRoute
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableVariantDetailCard(
    variant: VariantVaultRow,
    maxVariantAbs: Long,
    language: AppLanguage,
    onOpenExportVariant: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    VaultCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VaultProductThumbnail(
                imageUri = variant.primaryImageUri,
                contentDescription = variant.variantLabel,
                size = 50.dp
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = variant.variantLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(variant.sku, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = ImportVaultUiLogic.formatBdt(variant.attributedNetProfitMinor, language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (variant.attributedNetProfitMinor >= 0L) StatusSuccess else StatusError
                    )
                    Text(
                        text = "${if (isBn) "মার্জিন" else "Margin"} ${ImportVaultUiLogic.formatPercent(variant.netMarginPercent, language)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = colors.primary
            )
        }

        // Spec §13 Screen D: Product Share, PO Share, and Shipment Share MUST stay clearly separated
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VaultStatusBadge(
                label = "${if (isBn) "পণ্যে অংশ" else "Product Share"}: ${ImportVaultUiLogic.formatPercent(variant.profitContributionToProduct?.contributionPercent, language)}",
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer
            )
            VaultStatusBadge(
                label = "${if (isBn) "PO-তে অংশ" else "PO Share"}: ${ImportVaultUiLogic.formatPercent(variant.profitContributionToPo?.contributionPercent, language)}",
                containerColor = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer
            )
            VaultStatusBadge(
                label = "${if (isBn) "চালানে অংশ" else "Shipment Share"}: ${ImportVaultUiLogic.formatPercent(variant.profitContributionToShipment?.contributionPercent, language)}",
                containerColor = colors.surfaceVariant,
                contentColor = colors.onSurfaceVariant
            )
        }

        val barFraction = (abs(variant.attributedNetProfitMinor).toFloat() / maxVariantAbs.toFloat()).coerceIn(0.03f, 1f)
        LinearProgressIndicator(
            progress = { barFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (variant.attributedNetProfitMinor >= 0L) StatusSuccess else StatusError,
            trackColor = colors.surfaceVariant
        )

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider()
                StockDispositionBar(
                    soldQty = variant.netSoldQty,
                    remainingGoodQty = variant.remainingGoodQty,
                    inTransitQty = variant.remainingInTransitTransferQty,
                    damagedOrWriteOffQty = variant.returnedDamagedQty + variant.writeOffLossQty,
                    uom = variant.uom,
                    language = language
                )
                val avgSellPriceMinor = if (variant.netSoldQty > 0) variant.netSalesRevenueMinor / variant.netSoldQty else null
                VaultKpiPairRow(
                    leftLabel = if (isBn) "একক ল্যান্ডেড কস্ট" else "Weighted Landed Cost",
                    leftValue = ImportVaultUiLogic.formatBdt(variant.unitLandedCostMinor, language),
                    rightLabel = if (isBn) "গড় বিক্রয় মূল্য" else "Avg Net Sell Price",
                    rightValue = avgSellPriceMinor?.let { ImportVaultUiLogic.formatBdt(it, language) }
                        ?: (if (isBn) "প্রযোজ্য নয়" else "N/A")
                )
                VaultKpiPairRow(
                    leftLabel = if (isBn) "গৃহীত / নিট বিক্রীত" else "Received / Net Sold",
                    leftValue = "${Formatters.formatNumber(variant.receivedGoodQty.toLong(), language)} / ${Formatters.formatNumber(variant.netSoldQty.toLong(), language)} ${variant.uom}",
                    rightLabel = if (isBn) "ফেরত (ভালো / নষ্ট)" else "Returns (Good / Dmg)",
                    rightValue = "${Formatters.formatNumber(variant.returnedGoodQty.toLong(), language)} / ${Formatters.formatNumber(variant.returnedDamagedQty.toLong(), language)} ${variant.uom}"
                )
                VaultKpiPairRow(
                    leftLabel = if (isBn) "নিট বিক্রয়" else "Net Revenue",
                    leftValue = ImportVaultUiLogic.formatBdt(variant.netSalesRevenueMinor, language),
                    rightLabel = if (isBn) "বিক্রীত ব্যয় (COGS)" else "COGS",
                    rightValue = ImportVaultUiLogic.formatBdt(variant.cogsMinor, language)
                )
                VaultKpiPairRow(
                    leftLabel = if (isBn) "গ্রস মুনাফা" else "Gross Profit",
                    leftValue = ImportVaultUiLogic.formatBdt(variant.grossProfitMinor, language),
                    rightLabel = if (isBn) "ব্যয় ও ক্ষতি" else "Expenses + Loss",
                    rightValue = ImportVaultUiLogic.formatBdt(
                        variant.directExpensesMinor + variant.allocatedSharedOverheadMinor + variant.inventoryLossMinor,
                        language
                    )
                )
                VaultKpiPairRow(
                    leftLabel = if (isBn) "বিক্রি হার / গতি" else "Sell-Through / Velocity",
                    leftValue = "${ImportVaultUiLogic.formatPercent(variant.sellThroughPercent, language)} · ${ImportVaultUiLogic.formatVelocity(variant.sellingVelocityPerDay, variant.uom, language)}",
                    rightLabel = if (isBn) "ROI ও বদ্ধ স্টক" else "ROI % · Locked Cost",
                    rightValue = "${ImportVaultUiLogic.formatPercent(variant.roiPercent, language)} · ${ImportVaultUiLogic.formatBdt(variant.lockedInventoryCostMinor, language)}"
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.secondaryContainer.copy(alpha = 0.45f)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isBn) {
                                "সম্ভাব্য অবশিষ্ট গ্রস লাভ (${variant.expectedRemainingProfit.priceBasisLabel} · ${Formatters.formatDate(variant.expectedRemainingProfit.asOfMillis, language)})"
                            } else {
                                "Expected Remaining Gross Profit (${variant.expectedRemainingProfit.priceBasisLabel} · ${Formatters.formatDate(variant.expectedRemainingProfit.asOfMillis, language)})"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = ImportVaultUiLogic.formatBdt(variant.expectedRemainingProfit.expectedGrossProfitMinor, language),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.secondary
                        )
                    }
                }
                StockAgingBucketCard(agingSummary = variant.agingSummary, language = language)
                OutlinedButton(
                    onClick = onOpenExportVariant,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isBn) "এই ভ্যারিয়েন্ট এক্সপোর্ট (PDF / XLSX)" else "Export This Variant (PDF / XLSX)")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onNavigateToRoute("inventory") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text(if (isBn) "ইনভেন্টরি দেখুন" else "Open Inventory")
                    }
                    OutlinedButton(
                        onClick = { onNavigateToRoute("sales") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text(if (isBn) "বিক্রয় দেখুন" else "Open Sales")
                    }
                }
            }
        }
    }
}

private fun variantSortLabel(option: ProfitSortOption, isBn: Boolean): String = when (option) {
    ProfitSortOption.HIGHEST_PROFIT -> if (isBn) "সর্বোচ্চ মুনাফা" else "Highest Profit"
    ProfitSortOption.LOWEST_PROFIT -> if (isBn) "সর্বনিম্ন / লোকসান" else "Lowest Profit"
    ProfitSortOption.LOSS_MAKING_FIRST -> if (isBn) "প্রথমে লোকসানকারী (Loss First)" else "Loss-Making First"
    ProfitSortOption.HIGHEST_MARGIN -> if (isBn) "সর্বোচ্চ মার্জিন %" else "Highest Margin %"
    ProfitSortOption.HIGHEST_ROI -> if (isBn) "সর্বোচ্চ ROI %" else "Highest ROI %"
    ProfitSortOption.HIGHEST_CONTRIBUTION -> if (isBn) "সর্বোচ্চ শেয়ার %" else "Highest Share %"
    ProfitSortOption.HIGHEST_SELL_THROUGH -> if (isBn) "সর্বোচ্চ বিক্রি হার %" else "Highest Sell-Through"
    ProfitSortOption.FASTEST_SELLING -> if (isBn) "সবচেয়ে দ্রুত বিক্রি (Velocity)" else "Fastest Selling (u/day)"
    ProfitSortOption.SLOWEST_SELLING -> if (isBn) "সবচেয়ে ধীর বিক্রি" else "Slowest Selling (u/day)"
    ProfitSortOption.HIGHEST_LOCKED_CAPITAL -> if (isBn) "সর্বোচ্চ বদ্ধ মূলধন" else "Highest Locked Capital"
}
