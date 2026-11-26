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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.pocketdev.advance_erp.core.designsystem.StatusSuccessContainer
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.domain.model.ProductVaultRow
import com.pocketdev.advance_erp.domain.model.PurchaseOrderVaultSummary
import com.pocketdev.advance_erp.domain.model.ShipmentVaultSummary
import com.pocketdev.advance_erp.domain.model.VaultSaleSourceRow
import com.pocketdev.advance_erp.domain.model.VaultTrendPoint
import com.pocketdev.advance_erp.domain.model.VariantVaultRow

@Composable
fun ImportVaultPortfolioProfitScreen(
    uiState: ImportVaultUiState,
    onBack: () -> Unit,
    onSortChange: (ProfitSortOption) -> Unit,
    onOpenProduct: (String) -> Unit,
    onOpenExport: () -> Unit = {},
    onNavigateToRoute: (String) -> Unit = {}
) {
    val language = uiState.language
    val isBn = language == AppLanguage.BANGLA
    val overview = uiState.overview
    val tokens = LocalDashboardTokens.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var compareScopeIdx by remember { mutableIntStateOf(0) }
    var drillDialogState by remember { mutableStateOf<Pair<String, List<VaultSaleSourceRow>>?>(null) }

    drillDialogState?.let { (title, rows) ->
        VaultSourceTransactionsDrillDialog(
            title = title,
            sales = rows,
            language = language,
            onDismiss = { drillDialogState = null },
            onOpenSalesModule = { onNavigateToRoute("sales") }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(tokens.pageBgTop, tokens.pageBgBottom)))
    ) {
        VaultSubScreenTopBar(
            title = if (isBn) "মুনাফা বিশ্লেষণ (Profit Intelligence)" else "Profit Intelligence",
            subtitle = overview?.profitLabel ?: "Attributed Net Profit",
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                profitIntelligenceItems(
                    scopeTitle = if (isBn) "সর্বমোট পোর্টফোলিও" else "Portfolio Overview",
                    parentNetProfitMinor = overview.totalAttributedNetProfitMinor,
                    netRevenueMinor = overview.totalAttributedNetRevenueMinor,
                    cogsMinor = overview.totalAttributedCogsMinor,
                    expenseAndLossMinor = overview.totalDirectAttributedExpensesMinor +
                        overview.totalAllocatedSharedOverheadMinor +
                        overview.totalInventoryLossesMinor,
                    profitLabel = overview.profitLabel,
                    products = uiState.portfolioProducts,
                    shipments = overview.shipments,
                    purchaseOrders = overview.purchaseOrders,
                    sortOption = uiState.profitSort,
                    selectedTab = selectedTab,
                    compareScopeIdx = compareScopeIdx,
                    language = language,
                    onTabSelected = { selectedTab = it },
                    onCompareScopeSelected = { compareScopeIdx = it },
                    onSortChange = onSortChange,
                    onOpenProduct = onOpenProduct,
                    salesTrend = overview.salesTrend,
                    saleSources = overview.portfolioSaleSources,
                    onDrillTransactions = { title, rows -> drillDialogState = title to rows }
                )
            }
        }
    }
}

fun LazyListScope.profitIntelligenceItems(
    scopeTitle: String,
    parentNetProfitMinor: Long,
    netRevenueMinor: Long,
    cogsMinor: Long,
    expenseAndLossMinor: Long,
    profitLabel: String,
    products: List<ProductVaultRow>,
    shipments: List<ShipmentVaultSummary>,
    purchaseOrders: List<PurchaseOrderVaultSummary>,
    sortOption: ProfitSortOption,
    selectedTab: Int,
    compareScopeIdx: Int,
    language: AppLanguage,
    onTabSelected: (Int) -> Unit,
    onCompareScopeSelected: (Int) -> Unit,
    onSortChange: (ProfitSortOption) -> Unit,
    onOpenProduct: (String) -> Unit,
    salesTrend: List<VaultTrendPoint> = emptyList(),
    saleSources: List<VaultSaleSourceRow> = emptyList(),
    onDrillTransactions: ((String, List<VaultSaleSourceRow>) -> Unit)? = null
) {
    val isBn = language == AppLanguage.BANGLA

    item(key = "profit_hero_$scopeTitle") {
        VaultHeroCard {
            Text(
                text = "$scopeTitle · $profitLabel",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = ImportVaultUiLogic.formatBdt(parentNetProfitMinor, language),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (parentNetProfitMinor >= 0L) Color(0xFF42F5AA) else Color(0xFFFF9DB0)
            )
            val marginPct = if (netRevenueMinor > 0L) {
                (parentNetProfitMinor.toDouble() / netRevenueMinor.toDouble()) * 100.0
            } else null
            Text(
                text = "${if (isBn) "নিট বিক্রয়" else "Net Revenue"} ${ImportVaultUiLogic.formatBdt(netRevenueMinor, language)} · ${if (isBn) "মার্জিন" else "Margin"} ${ImportVaultUiLogic.formatPercent(marginPct, language)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
    }

    item(key = "profit_tabs") {
        VaultWrappedTabBar(
            options = listOf(
                0 to (if (isBn) "পণ্য (${Formatters.formatNumber(products.size.toLong(), language)})" else "Products (${products.size})"),
                1 to (if (isBn) "ভ্যারিয়েন্ট" else "Variants"),
                2 to (if (isBn) "তুলনা (Compare)" else "Compare")
            ),
            selectedIndex = selectedTab,
            onSelected = onTabSelected
        )
    }

    if (selectedTab == 0 || selectedTab == 1) {
        item(key = "profit_sort_bar") {
            ProfitSortDropdownBar(
                sortOption = sortOption,
                language = language,
                onSortChange = onSortChange
            )
        }
    }

    when (selectedTab) {
        0 -> {
            val sortedProducts = ImportVaultUiLogic.sortProducts(products, sortOption)
            if (sortedProducts.isEmpty()) {
                item(key = "profit_empty_products") {
                    VaultCard {
                        Text(
                            text = if (isBn) "এই স্কোপে কোনো পণ্য বা বিক্রয় তথ্য নেই।" else "No product performance data in this scope.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val slices = sortedProducts.take(7).map { p ->
                    val share = if (parentNetProfitMinor > 0L) {
                        (p.attributedNetProfitMinor.toDouble() / parentNetProfitMinor.toDouble()) * 100.0
                    } else null
                    ContributionSlice(
                        id = p.productId,
                        label = if (isBn) p.productNameBn.ifBlank { p.productNameEn } else p.productNameEn,
                        amountMinor = p.attributedNetProfitMinor,
                        percent = p.profitContributionToParentScope?.contributionPercent ?: share,
                        marginPercent = p.netMarginPercent
                    )
                }
                item(key = "profit_contrib_chart") {
                    ProfitContributionChartCard(
                        title = if (isBn) "পণ্যভিত্তিক মুনাফা অবদান" else "Product Profit Contribution",
                        parentProfitMinor = parentNetProfitMinor,
                        slices = slices,
                        language = language,
                        onSliceClick = { sl -> onOpenProduct(sl.id) }
                    )
                }

                if (salesTrend.isNotEmpty() || saleSources.isNotEmpty()) {
                    item(key = "profit_sales_trend_chart") {
                        SalesProfitTrendChartCard(
                            title = if (isBn) "সময়ভিত্তিক বিক্রয় ও গ্রস মুনাফা ট্রেন্ড" else "Sales & Profit Time-Series Trend",
                            trendPoints = salesTrend,
                            language = language,
                            onPointClick = if (onDrillTransactions != null && saleSources.isNotEmpty()) {
                                { pt ->
                                    val matching = saleSources.filter { it.invoiceDate in pt.startMillis..pt.endMillis }
                                    val lbl = if (isBn) "${pt.labelBn} — বিক্রয় লেনদেন" else "${pt.labelEn} — Source Invoices"
                                    onDrillTransactions(lbl, matching)
                                }
                            } else null
                        )
                    }
                }

                val topProfitProd = sortedProducts.maxByOrNull { it.attributedNetProfitMinor }
                val topMarginProd = sortedProducts.filter { it.netMarginPercent != null }.maxByOrNull { it.netMarginPercent!! }
                if (topProfitProd != null) {
                    item(key = "profit_leaders_callout") {
                        ProfitLeadersCard(
                            topProfitProduct = topProfitProd,
                            topMarginProduct = topMarginProd,
                            language = language
                        )
                    }
                }

                item(key = "profit_rev_cogs_chart") {
                    RevenueCogsProfitComparisonCard(
                        title = if (isBn) "আয় বনাম খরচ বনাম মুনাফা" else "Revenue vs COGS vs Profit",
                        netRevenueMinor = netRevenueMinor,
                        cogsMinor = cogsMinor,
                        expenseAndLossMinor = expenseAndLossMinor,
                        netProfitMinor = parentNetProfitMinor,
                        profitLabel = profitLabel,
                        language = language,
                        onDrillTransactions = if (onDrillTransactions != null && saleSources.isNotEmpty()) {
                            {
                                val hdr = if (isBn) "$scopeTitle — বিক্রয় লেনদেন" else "$scopeTitle — Source Invoices"
                                onDrillTransactions(hdr, saleSources)
                            }
                        } else null
                    )
                }

                items(sortedProducts, key = { "prod_card_${it.productId}" }) { product ->
                    ProfitProductRowCard(
                        product = product,
                        language = language,
                        onOpenProduct = { onOpenProduct(product.productId) }
                    )
                }
            }
        }

        1 -> {
            val productMap = products.associateBy { it.productId }
            val allVariants = products.flatMap { it.variants }
            val sortedVariants = ImportVaultUiLogic.sortVariants(allVariants, sortOption)
            if (sortedVariants.isEmpty()) {
                item(key = "profit_empty_variants") {
                    VaultCard {
                        Text(
                            text = if (isBn) "কোনো ভ্যারিয়েন্ট তথ্য পাওয়া যায়নি।" else "No variant profitability records found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val varSlices = sortedVariants.take(7).map { v ->
                    ContributionSlice(
                        id = "${v.productId}:${v.variantId.orEmpty()}",
                        label = "${v.variantLabel} (${v.sku})",
                        amountMinor = v.attributedNetProfitMinor,
                        percent = v.profitContributionToParentScope?.contributionPercent,
                        marginPercent = v.netMarginPercent
                    )
                }
                item(key = "variant_contrib_chart") {
                    ProfitContributionChartCard(
                        title = if (isBn) "ভ্যারিয়েন্ট মুনাফা র‌্যাংকিং" else "Variant Profit Contribution",
                        parentProfitMinor = parentNetProfitMinor,
                        slices = varSlices,
                        language = language,
                        onSliceClick = { sl ->
                            sl.id.substringBefore(':').takeIf { it.isNotBlank() }?.let(onOpenProduct)
                        }
                    )
                }

                items(sortedVariants, key = { "var_rank_${it.productId}_${it.variantId.orEmpty()}_${it.sku}" }) { variant ->
                    val parentProd = productMap[variant.productId]
                    val prodTitle = if (isBn) {
                        parentProd?.productNameBn?.ifBlank { parentProd.productNameEn } ?: variant.productId
                    } else {
                        parentProd?.productNameEn ?: variant.productId
                    }
                    VariantRankingRowCard(
                        productTitle = prodTitle,
                        variant = variant,
                        language = language,
                        onClick = { onOpenProduct(variant.productId) }
                    )
                }
            }
        }

        2 -> {
            item(key = "profit_compare_panel") {
                CompareEntitiesCard(
                    compareScopeIdx = compareScopeIdx,
                    onCompareScopeSelected = onCompareScopeSelected,
                    shipments = shipments,
                    purchaseOrders = purchaseOrders,
                    products = products,
                    language = language
                )
            }
        }
    }
}

@Composable
private fun ProfitSortDropdownBar(
    sortOption: ProfitSortOption,
    language: AppLanguage,
    onSortChange: (ProfitSortOption) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    var expanded by remember { mutableStateOf(false) }

    fun labelFor(opt: ProfitSortOption): String = when (opt) {
        ProfitSortOption.HIGHEST_PROFIT -> if (isBn) "সর্বোচ্চ মুনাফা" else "Highest Profit"
        ProfitSortOption.LOWEST_PROFIT -> if (isBn) "সর্বনিম্ন / লোকসান" else "Lowest Profit"
        ProfitSortOption.LOSS_MAKING_FIRST -> if (isBn) "প্রথমে লোকসানকারী (Loss First)" else "Loss-Making First"
        ProfitSortOption.HIGHEST_MARGIN -> if (isBn) "সর্বোচ্চ মার্জিন %" else "Highest Margin %"
        ProfitSortOption.HIGHEST_ROI -> if (isBn) "সর্বোচ্চ ROI %" else "Highest ROI %"
        ProfitSortOption.HIGHEST_CONTRIBUTION -> if (isBn) "সর্বোচ্চ শেয়ার %" else "Highest Share %"
        ProfitSortOption.HIGHEST_SELL_THROUGH -> if (isBn) "সর্বোচ্চ বিক্রি হার %" else "Highest Sell-Through"
        ProfitSortOption.FASTEST_SELLING -> if (isBn) "সবচেয়ে দ্রুত বিক্রি (Velocity)" else "Fastest Selling (u/day)"
        ProfitSortOption.SLOWEST_SELLING -> if (isBn) "সবচেয়ে ধীর বিক্রি" else "Slowest Selling (u/day)"
        ProfitSortOption.HIGHEST_LOCKED_CAPITAL -> if (isBn) "সর্বোচ্চ বদ্ধ স্টক" else "Highest Locked Capital"
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "${if (isBn) "সাজানো:" else "Sort:"} ${labelFor(sortOption)}",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold
            )
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ProfitSortOption.entries.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(labelFor(opt)) },
                    onClick = { expanded = false; onSortChange(opt) }
                )
            }
        }
    }
}

@Composable
private fun ProfitLeadersCard(
    topProfitProduct: ProductVaultRow,
    topMarginProduct: ProductVaultRow?,
    language: AppLanguage
) {
    val isBn = language == AppLanguage.BANGLA
    val topProfitName = if (isBn) topProfitProduct.productNameBn.ifBlank { topProfitProduct.productNameEn } else topProfitProduct.productNameEn
    val topMarginName = topMarginProduct?.let {
        if (isBn) it.productNameBn.ifBlank { it.productNameEn } else it.productNameEn
    }
    VaultCard {
        Text(
            text = if (isBn) "শীর্ষ মুনাফা বনাম শীর্ষ মার্জিন" else "Highest Absolute Profit vs Highest Margin",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        VaultKpiPairRow(
            leftLabel = if (isBn) "সর্বোচ্চ মোট মুনাফা" else "Top Profit Winner",
            leftValue = ImportVaultUiLogic.formatBdt(topProfitProduct.attributedNetProfitMinor, language),
            leftColor = if (topProfitProduct.attributedNetProfitMinor >= 0L) StatusSuccess else StatusError,
            rightLabel = if (isBn) "সর্বোচ্চ নিট মার্জিন" else "Top Margin Winner",
            rightValue = ImportVaultUiLogic.formatPercent(topMarginProduct?.netMarginPercent, language),
            rightColor = MaterialTheme.colorScheme.primary
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = topProfitName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = topMarginName ?: "-",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProfitProductRowCard(
    product: ProductVaultRow,
    language: AppLanguage,
    onOpenProduct: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    val name = if (isBn) product.productNameBn.ifBlank { product.productNameEn } else product.productNameEn

    VaultCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VaultProductThumbnail(imageUri = product.primaryImageUri, contentDescription = name, size = 54.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.onSurface)
                Text(
                    text = "${product.baseSku} · ${Formatters.formatNumber(product.variants.size.toLong(), language)} ${if (isBn) "ভ্যারিয়েন্ট" else "variants"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = ImportVaultUiLogic.formatBdt(product.attributedNetProfitMinor, language),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (product.attributedNetProfitMinor >= 0L) StatusSuccess else StatusError
                    )
                    Text(
                        text = "${if (isBn) "শেয়ার" else "Share"} ${ImportVaultUiLogic.formatPercent(product.profitContributionToParentScope?.contributionPercent, language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary
                    )
                    Text(
                        text = "${if (isBn) "মার্জিন" else "Margin"} ${ImportVaultUiLogic.formatPercent(product.netMarginPercent, language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = "ROI ${ImportVaultUiLogic.formatPercent(product.roiPercent, language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondary
                    )
                    Text(
                        text = "${if (isBn) "গতি" else "Vel"} ${ImportVaultUiLogic.formatVelocity(product.sellingVelocityPerDay, product.uom, language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = colors.primary
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StockDispositionBar(
                    soldQty = product.netSoldQty,
                    remainingGoodQty = product.remainingGoodQty,
                    inTransitQty = product.remainingInTransitTransferQty,
                    damagedOrWriteOffQty = product.returnedDamagedQty + product.writeOffLossQty,
                    uom = product.uom,
                    language = language
                )
                VaultKpiPairRow(
                    leftLabel = if (isBn) "নিট বিক্রয়" else "Net Revenue",
                    leftValue = ImportVaultUiLogic.formatBdt(product.netSalesRevenueMinor, language),
                    rightLabel = if (isBn) "COGS ও ব্যয়" else "COGS + Expense",
                    rightValue = ImportVaultUiLogic.formatBdt(
                        product.cogsMinor + product.directExpensesMinor + product.allocatedSharedOverheadMinor + product.inventoryLossMinor,
                        language
                    )
                )
                VaultKpiPairRow(
                    leftLabel = if (isBn) "বদ্ধ স্টক মূল্য" else "Locked Capital",
                    leftValue = ImportVaultUiLogic.formatBdt(product.lockedInventoryCostMinor, language),
                    rightLabel = if (isBn) "সম্ভাব্য গ্রস লাভ" else "Expected Gross Profit",
                    rightValue = ImportVaultUiLogic.formatBdt(product.expectedRemainingProfit.expectedGrossProfitMinor, language),
                    rightColor = colors.secondary
                )
                Button(
                    onClick = onOpenProduct,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isBn) "ভ্যারিয়েন্ট ড্রিল-ডাউন দেখুন" else "View Variants Drill-Down")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun VariantRankingRowCard(
    productTitle: String,
    variant: VariantVaultRow,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    VaultCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VaultProductThumbnail(imageUri = variant.primaryImageUri, contentDescription = variant.variantLabel, size = 48.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "$productTitle · ${variant.variantLabel}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(variant.sku, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = ImportVaultUiLogic.formatBdt(variant.attributedNetProfitMinor, language),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (variant.attributedNetProfitMinor >= 0L) StatusSuccess else StatusError
                    )
                    Text(
                        text = "${if (isBn) "মার্জিন" else "Margin"} ${ImportVaultUiLogic.formatPercent(variant.netMarginPercent, language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary
                    )
                    Text(
                        text = "${if (isBn) "পণ্য %" else "Prod %"} ${ImportVaultUiLogic.formatPercent(variant.profitContributionToProduct?.contributionPercent, language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = "ROI ${ImportVaultUiLogic.formatPercent(variant.roiPercent, language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondary
                    )
                    Text(
                        text = ImportVaultUiLogic.formatVelocity(variant.sellingVelocityPerDay, variant.uom, language),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                    variant.profitContributionToPo?.let {
                        Text(
                            text = "${if (isBn) "PO %" else "PO %"} ${ImportVaultUiLogic.formatPercent(it.contributionPercent, language)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondary
                        )
                    }
                }
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = colors.primary)
        }
    }
}

@Composable
private fun CompareEntitiesCard(
    compareScopeIdx: Int,
    onCompareScopeSelected: (Int) -> Unit,
    shipments: List<ShipmentVaultSummary>,
    purchaseOrders: List<PurchaseOrderVaultSummary>,
    products: List<ProductVaultRow>,
    language: AppLanguage
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme

    VaultCard {
        Text(
            text = if (isBn) "পাশাপাশি তুলনা (Side-by-Side Comparison)" else "Side-by-Side Comparison",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        VaultWrappedTabBar(
            options = listOf(
                0 to (if (isBn) "পণ্য" else "Products"),
                1 to (if (isBn) "PO" else "POs"),
                2 to (if (isBn) "চালান" else "Shipments")
            ),
            selectedIndex = compareScopeIdx,
            onSelected = onCompareScopeSelected
        )

        data class CompareCandidate(
            val id: String,
            val title: String,
            val investmentOrCogs: Long,
            val revenue: Long,
            val netProfit: Long,
            val margin: Double?,
            val sellThrough: Double?,
            val lockedCapital: Long
        )

        val candidates: List<CompareCandidate> = when (compareScopeIdx) {
            1 -> purchaseOrders.map {
                CompareCandidate(it.purchaseOrderId, it.displayTitle, it.totalInvestedCapitalMinor, it.netSalesRevenueMinor, it.attributedNetProfitMinor, it.netMarginPercent, it.sellThroughPercent, it.lockedInventoryCapitalMinor)
            }
            2 -> shipments.map {
                CompareCandidate(it.shipmentId, it.displayTitle, it.totalInvestedCapitalMinor, it.netSalesRevenueMinor, it.attributedNetProfitMinor, it.netMarginPercent, it.sellThroughPercent, it.lockedInventoryCapitalMinor)
            }
            else -> products.map {
                CompareCandidate(
                    it.productId,
                    if (isBn) it.productNameBn.ifBlank { it.productNameEn } else it.productNameEn,
                    it.cogsMinor + it.lockedInventoryCostMinor,
                    it.netSalesRevenueMinor,
                    it.attributedNetProfitMinor,
                    it.netMarginPercent,
                    it.sellThroughPercent,
                    it.lockedInventoryCostMinor
                )
            }
        }

        if (candidates.size < 2) {
            Text(
                text = if (isBn) "তুলনা করার জন্য কমপক্ষে ২টি আইটেম প্রয়োজন।" else "At least 2 items are required to compare side-by-side.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        } else {
            var leftId by remember(compareScopeIdx, candidates) { mutableStateOf(candidates[0].id) }
            var rightId by remember(compareScopeIdx, candidates) { mutableStateOf(candidates[1].id) }
            val leftItem = candidates.find { it.id == leftId } ?: candidates[0]
            val rightItem = candidates.find { it.id == rightId } ?: candidates[1]

            var expandLeft by remember { mutableStateOf(false) }
            var expandRight by remember { mutableStateOf(false) }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expandLeft = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text(leftItem.title, modifier = Modifier.weight(1f))
                        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = expandLeft, onDismissRequest = { expandLeft = false }) {
                        candidates.forEach { c ->
                            DropdownMenuItem(text = { Text(c.title) }, onClick = { expandLeft = false; leftId = c.id })
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expandRight = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text(rightItem.title, modifier = Modifier.weight(1f))
                        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = expandRight, onDismissRequest = { expandRight = false }) {
                        candidates.forEach { c ->
                            DropdownMenuItem(text = { Text(c.title) }, onClick = { expandRight = false; rightId = c.id })
                        }
                    }
                }
            }

            HorizontalDivider()
            CompareMetricRow(
                label = if (compareScopeIdx == 0) (if (isBn) "মূল্য + স্টক বেস" else "Cost Basis") else (if (isBn) "বিনিয়োগ" else "Investment"),
                leftVal = ImportVaultUiLogic.formatBdt(leftItem.investmentOrCogs, language),
                rightVal = ImportVaultUiLogic.formatBdt(rightItem.investmentOrCogs, language)
            )
            CompareMetricRow(
                label = if (isBn) "নিট বিক্রয়" else "Net Revenue",
                leftVal = ImportVaultUiLogic.formatBdt(leftItem.revenue, language),
                rightVal = ImportVaultUiLogic.formatBdt(rightItem.revenue, language)
            )
            CompareMetricRow(
                label = if (isBn) "নিট মুনাফা" else "Net Profit",
                leftVal = ImportVaultUiLogic.formatBdt(leftItem.netProfit, language),
                rightVal = ImportVaultUiLogic.formatBdt(rightItem.netProfit, language),
                leftColor = if (leftItem.netProfit >= 0L) StatusSuccess else StatusError,
                rightColor = if (rightItem.netProfit >= 0L) StatusSuccess else StatusError
            )
            CompareMetricRow(
                label = if (isBn) "নিট মার্জিন %" else "Net Margin %",
                leftVal = ImportVaultUiLogic.formatPercent(leftItem.margin, language),
                rightVal = ImportVaultUiLogic.formatPercent(rightItem.margin, language)
            )
            CompareMetricRow(
                label = if (isBn) "বিক্রি হার %" else "Sell-Through %",
                leftVal = ImportVaultUiLogic.formatPercent(leftItem.sellThrough, language),
                rightVal = ImportVaultUiLogic.formatPercent(rightItem.sellThrough, language)
            )
            CompareMetricRow(
                label = if (isBn) "বদ্ধ স্টক" else "Locked Capital",
                leftVal = ImportVaultUiLogic.formatBdt(leftItem.lockedCapital, language),
                rightVal = ImportVaultUiLogic.formatBdt(rightItem.lockedCapital, language)
            )
        }
    }
}

@Composable
private fun CompareMetricRow(
    label: String,
    leftVal: String,
    rightVal: String,
    leftColor: Color = MaterialTheme.colorScheme.onSurface,
    rightColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = leftVal,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = leftColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = rightVal,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = rightColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
