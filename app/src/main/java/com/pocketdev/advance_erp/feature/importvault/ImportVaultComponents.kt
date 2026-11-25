@file:OptIn(ExperimentalLayoutApi::class)

package com.pocketdev.advance_erp.feature.importvault

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocketdev.advance_erp.core.designsystem.LocalDashboardTokens
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusErrorContainer
import com.pocketdev.advance_erp.core.designsystem.StatusInfo
import com.pocketdev.advance_erp.core.designsystem.StatusInfoContainer
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusSuccessContainer
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.StatusWarningContainer
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.domain.model.AgingSummary
import com.pocketdev.advance_erp.domain.model.CashRecoverySummary
import com.pocketdev.advance_erp.domain.model.CommercialStatus
import com.pocketdev.advance_erp.domain.model.CostFinalizationStatus
import com.pocketdev.advance_erp.domain.model.ImportVaultFilter
import com.pocketdev.advance_erp.domain.model.ImportVaultFilterOptions
import com.pocketdev.advance_erp.domain.model.OperationalStatus
import com.pocketdev.advance_erp.domain.model.StockAgingBucket
import com.pocketdev.advance_erp.domain.model.VaultDateFilterTarget
import com.pocketdev.advance_erp.domain.model.VaultSaleSourceRow
import com.pocketdev.advance_erp.domain.model.VaultTrendPoint
import java.io.File
import java.util.Calendar
import kotlin.math.abs

val VAULT_CHART_PALETTE = listOf(
    Color(0xFF792B87),
    Color(0xFF1B7180),
    Color(0xFF23845B),
    Color(0xFFC96F0A),
    Color(0xFF3977B8),
    Color(0xFFD04C72),
    Color(0xFF6B5B95),
    Color(0xFF0E8899)
)

@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalDashboardTokens.current
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val baseModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }
    Surface(
        modifier = baseModifier,
        shape = shape,
        color = if (tokens.isDark) Color(0xFF151830) else colors.surface,
        border = BorderStroke(1.dp, borderColor ?: colors.outlineVariant),
        tonalElevation = if (tokens.isDark) 1.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun VaultHeroCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalDashboardTokens.current
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(tokens.overviewBorderStart, tokens.overviewBorderEnd))
        ),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(tokens.overviewBgTop, tokens.overviewBgMid, tokens.overviewBgBottom)
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
fun VaultWrappedTabBar(
    options: List<Pair<Int, String>>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalDashboardTokens.current
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (tokens.isDark) Color(0xFF151830) else colors.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (idx, label) ->
                val selected = idx == selectedIndex
                Surface(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Tab) { onSelected(idx) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) colors.primary else Color.Transparent,
                    border = if (selected) null else BorderStroke(0.8.dp, colors.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) colors.onPrimary else colors.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VaultKpiPairRow(
    leftLabel: String,
    leftValue: String,
    leftColor: Color? = null,
    rightLabel: String,
    rightValue: String,
    rightColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        VaultKpiTile(
            label = leftLabel,
            value = leftValue,
            valueColor = leftColor ?: colors.onSurface,
            modifier = Modifier.weight(1f)
        )
        VaultKpiTile(
            label = rightLabel,
            value = rightValue,
            valueColor = rightColor ?: colors.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun VaultKpiTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitle: String? = null
) {
    val tokens = LocalDashboardTokens.current
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (tokens.isDark) Color(0xFF1B1F3A) else colors.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(0.8.dp, colors.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            if (!subtitle.isNullOrBlank()) {
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
fun VaultStatusBadge(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun VaultStatusBadgeRow(
    operationalStatus: OperationalStatus,
    commercialStatus: CommercialStatus,
    costFinalizationStatus: CostFinalizationStatus,
    language: AppLanguage
) {
    val colors = MaterialTheme.colorScheme
    val (opBg, opFg) = when (operationalStatus) {
        OperationalStatus.CANCELLED -> StatusErrorContainer to StatusError
        OperationalStatus.CLOSED, OperationalStatus.RECEIVED -> StatusSuccessContainer to StatusSuccess
        OperationalStatus.PARTIALLY_RECEIVED -> StatusWarningContainer to StatusWarning
        OperationalStatus.IN_TRANSIT, OperationalStatus.ORDERED -> StatusInfoContainer to StatusInfo
        OperationalStatus.DRAFT -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    val (comBg, comFg) = when (commercialStatus) {
        CommercialStatus.LOSS_MAKING -> StatusErrorContainer to StatusError
        CommercialStatus.PROFITABLE, CommercialStatus.CASH_RECOVERED, CommercialStatus.SOLD_OUT ->
            StatusSuccessContainer to StatusSuccess
        CommercialStatus.UNATTRIBUTED -> StatusWarningContainer to StatusWarning
        CommercialStatus.SELLING -> colors.secondaryContainer to colors.onSecondaryContainer
        CommercialStatus.UNSOLD -> colors.surfaceVariant to colors.onSurfaceVariant
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        VaultStatusBadge(operationalStatusLabel(operationalStatus, language), opBg, opFg)
        VaultStatusBadge(commercialStatusLabel(commercialStatus, language), comBg, comFg)
        if (costFinalizationStatus == CostFinalizationStatus.PROVISIONAL) {
            VaultStatusBadge(
                label = if (language == AppLanguage.BANGLA) "খরচ অসমাপ্ত (Provisional)" else "Pending / Provisional Cost",
                containerColor = StatusWarningContainer,
                contentColor = StatusWarning
            )
        }
    }
}

fun operationalStatusLabel(status: OperationalStatus, language: AppLanguage): String {
    val isBn = language == AppLanguage.BANGLA
    return when (status) {
        OperationalStatus.DRAFT -> if (isBn) "খসড়া" else "Draft"
        OperationalStatus.ORDERED -> if (isBn) "অর্ডারকৃত" else "Ordered"
        OperationalStatus.IN_TRANSIT -> if (isBn) "পথিমধ্যে" else "In Transit"
        OperationalStatus.PARTIALLY_RECEIVED -> if (isBn) "আংশিক গৃহীত" else "Partial Receipt"
        OperationalStatus.RECEIVED -> if (isBn) "গৃহীত" else "Received"
        OperationalStatus.CLOSED -> if (isBn) "সম্পন্ন" else "Completed"
        OperationalStatus.CANCELLED -> if (isBn) "বাতিল" else "Cancelled"
    }
}

fun commercialStatusLabel(status: CommercialStatus, language: AppLanguage): String {
    val isBn = language == AppLanguage.BANGLA
    return when (status) {
        CommercialStatus.UNSOLD -> if (isBn) "অবিক্রীত" else "Unsold"
        CommercialStatus.SELLING -> if (isBn) "বিক্রয় চলছে" else "Selling"
        CommercialStatus.PROFITABLE -> if (isBn) "লাভজনক" else "Profitable"
        CommercialStatus.LOSS_MAKING -> if (isBn) "লোকসান" else "Loss Making"
        CommercialStatus.SOLD_OUT -> if (isBn) "স্টক শেষ" else "Sold Out"
        CommercialStatus.CASH_RECOVERED -> if (isBn) "মূলধন উদ্ধার" else "Cash Recovered"
        CommercialStatus.UNATTRIBUTED -> if (isBn) "অনির্ধারিত" else "Unattributed"
    }
}

@Composable
fun VaultExpandableSection(
    title: String,
    subtitle: String? = null,
    initiallyExpanded: Boolean = false,
    badgeText: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val colors = MaterialTheme.colorScheme
    VaultCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            if (!badgeText.isNullOrBlank()) {
                VaultStatusBadge(
                    label = badgeText,
                    containerColor = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = colors.primary
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

@Composable
fun VaultProductThumbnail(
    imageUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val resolvedFile = remember(imageUri) {
        imageUri?.takeIf { it.isNotBlank() }?.let { raw ->
            val file = File(raw.removePrefix("file://"))
            if (file.exists()) file else null
        }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.primaryContainer.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        if (resolvedFile != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(resolvedFile)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Inventory2,
                contentDescription = contentDescription,
                tint = colors.primary,
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}

@Composable
fun ProfitContributionChartCard(
    title: String,
    parentProfitMinor: Long,
    slices: List<ContributionSlice>,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onSliceClick: ((ContributionSlice) -> Unit)? = null
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    val profitColor = StatusSuccess
    val lossColor = StatusError

    var showTableView by remember { mutableStateOf(false) }

    val mode = remember(slices, parentProfitMinor) {
        ImportVaultChartPolicy.resolveContributionChartMode(slices, parentProfitMinor)
    }

    VaultCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            VaultStatusBadge(
                label = ImportVaultUiLogic.formatBdt(parentProfitMinor, language),
                containerColor = if (parentProfitMinor >= 0L) StatusSuccessContainer else StatusErrorContainer,
                contentColor = if (parentProfitMinor >= 0L) profitColor else lossColor
            )
        }

        if (slices.isNotEmpty() && mode != ContributionChartMode.ZERO_BASE_NA) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterOptionChip(
                    label = if (isBn) "চার্ট ভিউ" else "Chart View",
                    selected = !showTableView,
                    onClick = { showTableView = false }
                )
                FilterOptionChip(
                    label = if (isBn) "টেবিল ভিউ" else "Table Fallback",
                    selected = showTableView,
                    onClick = { showTableView = true }
                )
            }
        }

        if (showTableView && slices.isNotEmpty() && mode != ContributionChartMode.ZERO_BASE_NA) {
            val displayRows = if (mode == ContributionChartMode.DONUT) {
                ImportVaultChartPolicy.buildDonutDisplaySlices(
                    slices = slices,
                    parentProfitMinor = parentProfitMinor,
                    residualLabel = if (isBn) "অবন্টিত অবশিষ্ট" else "Unallocated Residual"
                )
            } else slices
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(if (isBn) "আইটেম" else "Item", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f))
                    Text(if (isBn) "মুনাফা" else "Profit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    Text(if (isBn) "অংশ %" else "Share", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                }
                HorizontalDivider()
                displayRows.forEach { sl ->
                    val canTap = onSliceClick != null && sl.id != "__residual__"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .then(if (canTap) Modifier.clickable(role = Role.Button) { onSliceClick?.invoke(sl) } else Modifier)
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.4f)) {
                            Text(sl.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            sl.marginPercent?.let { m ->
                                Text(
                                    text = "${if (isBn) "মার্জিন" else "Margin"} ${ImportVaultUiLogic.formatPercent(m, language)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = ImportVaultUiLogic.formatBdt(sl.amountMinor, language),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (sl.amountMinor >= 0L) profitColor else lossColor,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = ImportVaultUiLogic.formatPercent(sl.percent, language),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary,
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.End
                        )
                    }
                    HorizontalDivider()
                }
            }
        } else when (mode) {
            ContributionChartMode.ZERO_BASE_NA -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.primary)
                        Text(
                            text = if (isBn) {
                                "মোট মুনাফা শূন্য (০) হওয়ায় শতাংশ শেয়ার প্রযোজ্য নয় (N/A)।"
                            } else {
                                "Contribution share % is N/A because total parent net profit is 0."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            ContributionChartMode.DONUT -> {
                val donutSlices = remember(slices, parentProfitMinor, isBn) {
                    ImportVaultChartPolicy.buildDonutDisplaySlices(
                        slices = slices,
                        parentProfitMinor = parentProfitMinor,
                        residualLabel = if (isBn) "অবন্টিত অবশিষ্ট" else "Unallocated Residual"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(136.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val totalAmt = maxOf(1L, donutSlices.sumOf { it.amountMinor }).toFloat()
                        Canvas(modifier = Modifier.size(132.dp)) {
                            val strokeWidth = 22.dp.toPx()
                            val inset = strokeWidth / 2f
                            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                            var startAngle = -90f
                            donutSlices.forEachIndexed { idx, sl ->
                                val sweep = (sl.amountMinor.toFloat() / totalAmt) * 360f
                                val tint = VAULT_CHART_PALETTE[idx % VAULT_CHART_PALETTE.size]
                                drawArc(
                                    color = tint,
                                    startAngle = startAngle,
                                    sweepAngle = maxOf(1f, sweep - 2f),
                                    useCenter = false,
                                    topLeft = Offset(inset, inset),
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                startAngle += sweep
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isBn) "মোট" else "Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                            Text(
                                text = ImportVaultUiLogic.formatBdt(parentProfitMinor, language),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        donutSlices.forEachIndexed { idx, sl ->
                            val tint = VAULT_CHART_PALETTE[idx % VAULT_CHART_PALETTE.size]
                            val canTap = onSliceClick != null && sl.id != "__residual__"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(if (canTap) Modifier.clickable(role = Role.Button) { onSliceClick?.invoke(sl) } else Modifier)
                                    .padding(vertical = 3.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(tint)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sl.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.onSurface
                                    )
                                    val shareStr = ImportVaultUiLogic.formatPercent(sl.percent, language)
                                    val marginStr = sl.marginPercent?.let {
                                        val ml = if (isBn) "মার্জিন" else "Margin"
                                        " · $ml ${ImportVaultUiLogic.formatPercent(it, language)}"
                                    }.orEmpty()
                                    Text(
                                        text = "${ImportVaultUiLogic.formatBdt(sl.amountMinor, language)} ($shareStr)$marginStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ContributionChartMode.DIVERGING_BARS, ContributionChartMode.LOSS_BASE_BARS -> {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusWarningContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = StatusWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (mode == ContributionChartMode.LOSS_BASE_BARS) {
                                if (isBn) {
                                    "মোট ফলাফল লোকসান ভিত্তিক (Loss Base) হওয়ায় পাই চার্টের বদলে সাইনড বার দেখানো হচ্ছে।"
                                } else {
                                    "Loss-base parent net profit: showing signed diverging bars instead of a pie chart."
                                }
                            } else {
                                if (isBn) {
                                    "লাভ ও লোকসান উভয় আইটেম থাকায় ডাইভার্জিং বার দেখানো হচ্ছে (১০০%-এর বেশি শেয়ার হতে পারে)।"
                                } else {
                                    "Mixed profit & loss contributors: diverging bars shown (shares can exceed 100%)."
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusWarning
                        )
                    }
                }
                val maxAbs = maxOf(1L, slices.maxOfOrNull { abs(it.amountMinor) } ?: 1L)
                val dividerColor = colors.outlineVariant
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    slices.forEach { sl ->
                        val isNeg = sl.amountMinor < 0L
                        val fraction = (abs(sl.amountMinor).toFloat() / maxAbs.toFloat()).coerceIn(0.03f, 1f)
                        val canTap = onSliceClick != null && sl.id != "__residual__"
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .then(if (canTap) Modifier.clickable(role = Role.Button) { onSliceClick?.invoke(sl) } else Modifier)
                                .padding(vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sl.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                val shareStr = ImportVaultUiLogic.formatPercent(sl.percent, language)
                                Text(
                                    text = "${ImportVaultUiLogic.formatBdt(sl.amountMinor, language)} ($shareStr)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNeg) lossColor else profitColor
                                )
                            }
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                            ) {
                                val centerX = size.width / 2f
                                drawLine(
                                    color = dividerColor,
                                    start = Offset(centerX, 0f),
                                    end = Offset(centerX, size.height),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                                val barW = (size.width / 2f) * fraction
                                if (isNeg) {
                                    drawRect(
                                        color = lossColor,
                                        topLeft = Offset(centerX - barW, 2.dp.toPx()),
                                        size = Size(barW, size.height - 4.dp.toPx())
                                    )
                                } else {
                                    drawRect(
                                        color = profitColor,
                                        topLeft = Offset(centerX, 2.dp.toPx()),
                                        size = Size(barW, size.height - 4.dp.toPx())
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

@Composable
fun RevenueCogsProfitComparisonCard(
    title: String,
    netRevenueMinor: Long,
    cogsMinor: Long,
    expenseAndLossMinor: Long,
    netProfitMinor: Long,
    profitLabel: String,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onDrillTransactions: (() -> Unit)? = null
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    val maxVal = maxOf(1L, netRevenueMinor, cogsMinor, expenseAndLossMinor, abs(netProfitMinor))

    VaultCard(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )
        val rows = listOf(
            Triple(if (isBn) "নিট বিক্রয় আয়" else "Net Revenue", netRevenueMinor, colors.primary),
            Triple(if (isBn) "বিক্রীত পণ্যের খরচ (COGS)" else "COGS", cogsMinor, colors.secondary),
            Triple(if (isBn) "পরিচালন ব্যয় ও ক্ষতি" else "Expenses & Losses", expenseAndLossMinor, StatusWarning),
            Triple(profitLabel, netProfitMinor, if (netProfitMinor >= 0L) StatusSuccess else StatusError)
        )
        rows.forEach { (lbl, amt, barColor) ->
            val ratio = (abs(amt).toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(lbl, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Text(
                        text = ImportVaultUiLogic.formatBdt(amt, language),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (lbl == profitLabel) barColor else colors.onSurface
                    )
                }
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = barColor,
                    trackColor = colors.surfaceVariant
                )
            }
        }
        if (onDrillTransactions != null) {
            OutlinedButton(
                onClick = onDrillTransactions,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isBn) "সংশ্লিষ্ট বিক্রয় লেনদেন দেখুন" else "Drill Into Source Sales")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun SalesProfitTrendChartCard(
    title: String,
    trendPoints: List<VaultTrendPoint>,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onPointClick: ((VaultTrendPoint) -> Unit)? = null
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    var showTable by remember { mutableStateOf(false) }

    VaultCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.ShowChart, contentDescription = null, tint = colors.primary)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
            }
            if (trendPoints.isNotEmpty()) {
                VaultStatusBadge(
                    label = "${Formatters.formatNumber(trendPoints.size.toLong(), language)} ${if (isBn) "ধাপ" else "pts"}",
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer
                )
            }
        }

        if (trendPoints.isEmpty()) {
            Text(
                text = if (isBn) "নির্বাচিত সময়সীমায় বিক্রয় ট্রেন্ড দেখানোর মতো কোনো পোষ্টেড বিক্রয় নেই।" else "No posted sales data in this scope/period for a time-series trend.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterOptionChip(
                    label = if (isBn) "বার চার্ট" else "Trend Chart",
                    selected = !showTable,
                    onClick = { showTable = false }
                )
                FilterOptionChip(
                    label = if (isBn) "টেবিল ভিউ" else "Table Fallback",
                    selected = showTable,
                    onClick = { showTable = true }
                )
            }

            if (showTable) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isBn) "সময়" else "Period", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(if (isBn) "বিক্রয়" else "Revenue", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text(if (isBn) "গ্রস লাভ" else "GP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    HorizontalDivider()
                    trendPoints.forEach { pt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .then(if (onPointClick != null) Modifier.clickable(role = Role.Button) { onPointClick(pt) } else Modifier)
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isBn) pt.labelBn else pt.labelEn, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${pt.invoiceCount} inv · ${pt.netQtySold}u",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            Text(
                                text = ImportVaultUiLogic.formatBdt(pt.netRevenueMinor, language),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = ImportVaultUiLogic.formatBdt(pt.grossProfitMinor, language),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (pt.grossProfitMinor >= 0L) StatusSuccess else StatusError,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                        HorizontalDivider()
                    }
                }
            } else {
                val visiblePoints = trendPoints.takeLast(8)
                val maxMag = maxOf(1L, visiblePoints.maxOf { maxOf(it.netRevenueMinor, abs(it.grossProfitMinor)) })
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(colors.primary))
                        Text(if (isBn) "নিট বিক্রয়" else "Revenue", style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(StatusSuccess))
                        Text(if (isBn) "গ্রস মুনাফা" else "Gross Profit", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visiblePoints.forEach { pt ->
                        val revFrac = (pt.netRevenueMinor.toFloat() / maxMag.toFloat()).coerceIn(0.03f, 1f)
                        val gpFrac = (abs(pt.grossProfitMinor).toFloat() / maxMag.toFloat()).coerceIn(0.03f, 1f)
                        val gpColor = if (pt.grossProfitMinor >= 0L) StatusSuccess else StatusError
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .then(if (onPointClick != null) Modifier.clickable(role = Role.Button) { onPointClick(pt) } else Modifier)
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "${if (isBn) pt.labelBn else pt.labelEn} (${pt.invoiceCount} inv · ${pt.netQtySold}u)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${ImportVaultUiLogic.formatBdt(pt.netRevenueMinor, language)} / GP ${ImportVaultUiLogic.formatBdt(pt.grossProfitMinor, language)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = gpColor
                                )
                            }
                            val revColor = colors.primary
                            val track = colors.surfaceVariant
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            ) {
                                drawRect(color = track, topLeft = Offset.Zero, size = Size(size.width, 5.dp.toPx()))
                                drawRect(color = revColor, topLeft = Offset.Zero, size = Size(size.width * revFrac, 5.dp.toPx()))
                                drawRect(color = track, topLeft = Offset(0f, 8.dp.toPx()), size = Size(size.width, 5.dp.toPx()))
                                drawRect(color = gpColor, topLeft = Offset(0f, 8.dp.toPx()), size = Size(size.width * gpFrac, 5.dp.toPx()))
                            }
                        }
                    }
                }
                if (onPointClick != null) {
                    Text(
                        text = if (isBn) "যেকোনো ধাপে ট্যাপ করে ইনভয়েস তালিকা দেখুন" else "Tap any period bar above to inspect matching invoices",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary
                    )
                }
            }
        }
    }
}

@Composable
fun VaultSourceTransactionsDrillDialog(
    title: String,
    sales: List<VaultSaleSourceRow>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onOpenSalesModule: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (sales.isEmpty()) {
                    Text(
                        text = if (isBn) "এই পরিসরে কোনো বিক্রয় লেনদেন পাওয়া যায়নি।" else "No attributed sales transactions found for this selection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                } else {
                    val totRev = sales.sumOf { it.netRevenueMinor }
                    val totGp = sales.sumOf { it.grossProfitMinor }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.primaryContainer.copy(alpha = 0.55f)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                            Text(
                                text = "${sales.size} ${if (isBn) "সেলস লাইন" else "sale lines"} · ${ImportVaultUiLogic.formatBdt(totRev, language)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${if (isBn) "মোট গ্রস মুনাফা" else "Gross Profit"}: ${ImportVaultUiLogic.formatBdt(totGp, language)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (totGp >= 0L) StatusSuccess else StatusError
                            )
                        }
                    }
                    sales.forEach { row ->
                        val prodName = if (isBn) row.productNameBn.ifBlank { row.productNameEn } else row.productNameEn
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "${row.invoiceNumber} · ${Formatters.formatDate(row.invoiceDate, language)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = ImportVaultUiLogic.formatBdt(row.netRevenueMinor, language),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary
                                )
                            }
                            Text(
                                text = "$prodName ${row.variantLabel} · ${row.quantity}u · COGS ${ImportVaultUiLogic.formatBdt(row.cogsMinor, language)} · GP ${ImportVaultUiLogic.formatBdt(row.grossProfitMinor, language)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                            Text(
                                text = "${if (isBn) "নগদ" else "Cash"} ${ImportVaultUiLogic.formatBdt(row.collectedCashMinor, language)} · ${if (isBn) "বকেয়া" else "Due"} ${ImportVaultUiLogic.formatBdt(row.receivableMinor, language)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (row.receivableMinor > 0L) StatusWarning else StatusSuccess
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDismiss(); onOpenSalesModule() },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(if (isBn) "বিক্রয় মডিউলে খুলুন" else "Open Sales Module")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (isBn) "বন্ধ করুন" else "Close")
            }
        }
    )
}

@Composable
fun CapitalRecoveryProgressCard(
    cashRecovery: CashRecoverySummary,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    val ratioPct = cashRecovery.cashRecoveryRatioPercent
    val progress = ((ratioPct ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    val barColor = if (cashRecovery.breakEvenReached) StatusSuccess else colors.primary

    VaultCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBn) "মূলধন উদ্ধার অনুপাত (Cash Recovery)" else "Cash Recovery Ratio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = if (isBn) {
                        "ইনভয়েস-ভিত্তিক নগদ সংগ্রহ ÷ বিনিয়োগকৃত মূলধন (১০০%+ হতে পারে)"
                    } else {
                        "Attributable cash collected ÷ finalized investment (can exceed 100%)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            VaultStatusBadge(
                label = ImportVaultUiLogic.formatPercent(ratioPct, language),
                containerColor = if (cashRecovery.breakEvenReached) StatusSuccessContainer else colors.primaryContainer,
                contentColor = if (cashRecovery.breakEvenReached) StatusSuccess else colors.onPrimaryContainer
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = barColor,
            trackColor = colors.surfaceVariant
        )

        VaultKpiPairRow(
            leftLabel = if (isBn) "মোট বিনিয়োগ" else "Invested Capital",
            leftValue = ImportVaultUiLogic.formatBdt(cashRecovery.totalInvestedCapitalMinor, language),
            rightLabel = if (isBn) "আদায়কৃত নগদ" else "Cash Collected",
            rightValue = ImportVaultUiLogic.formatBdt(cashRecovery.attributableCashCollectedMinor, language),
            rightColor = StatusSuccess
        )
        VaultKpiPairRow(
            leftLabel = if (isBn) "আদায়যোগ্য বকেয়া" else "Attributable Receivable",
            leftValue = ImportVaultUiLogic.formatBdt(cashRecovery.attributableReceivableMinor, language),
            leftColor = StatusWarning,
            rightLabel = if (isBn) "অনুদ্ধারকৃত মূলধন" else "Unrecovered Capital",
            rightValue = ImportVaultUiLogic.formatBdt(cashRecovery.unrecoveredCapitalMinor, language)
        )
        Text(
            text = if (isBn) {
                "নোট: নগদ সংগ্রহ মানেই অর্জিত মুনাফা নয়; স্টকের মূল্য (Locked Capital) বকেয়া থেকে আলাদা।"
            } else {
                "Note: Cash collected does not imply realized profit; locked stock value is tracked separately from receivables."
            },
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
fun StockAgingBucketCard(
    agingSummary: AgingSummary,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    val totalVal = maxOf(1L, agingSummary.bucketValuesMinor.values.sum())
    val totalRawVal = agingSummary.bucketValuesMinor.values.sum()

    VaultCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBn) "স্টক বয়স বিশ্লেষণ (Aging)" else "Stock Aging Buckets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            VaultStatusBadge(
                label = if (isBn) {
                    "সর্বোচ্চ ${Formatters.formatNumber(agingSummary.oldestAgeDays.toLong(), language)} দিন"
                } else {
                    "Oldest ${agingSummary.oldestAgeDays}d"
                },
                containerColor = if (agingSummary.oldestAgeDays > 90) StatusErrorContainer else colors.surfaceVariant,
                contentColor = if (agingSummary.oldestAgeDays > 90) StatusError else colors.onSurfaceVariant
            )
        }

        if (totalRawVal <= 0L && agingSummary.bucketQuantities.values.sum() <= 0) {
            Text(
                text = if (isBn) "বর্তমানে কোনো অবিক্রীত স্টক নেই।" else "No remaining owned stock to age.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        } else {
            StockAgingBucket.entries.forEach { bucket ->
                val qty = agingSummary.bucketQuantities[bucket] ?: 0
                val valueMinor = agingSummary.bucketValuesMinor[bucket] ?: 0L
                val frac = (valueMinor.toFloat() / totalVal.toFloat()).coerceIn(0f, 1f)
                val barColor = when (bucket) {
                    StockAgingBucket.DAYS_0_30 -> StatusSuccess
                    StockAgingBucket.DAYS_31_60 -> colors.secondary
                    StockAgingBucket.DAYS_61_90 -> StatusWarning
                    StockAgingBucket.DAYS_90_PLUS -> StatusError
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isBn) bucket.labelBn else bucket.labelEn,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "${Formatters.formatNumber(qty.toLong(), language)} · ${ImportVaultUiLogic.formatBdt(valueMinor, language)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = barColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { frac },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = barColor,
                        trackColor = colors.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StockDispositionBar(
    soldQty: Int,
    remainingGoodQty: Int,
    inTransitQty: Int,
    damagedOrWriteOffQty: Int,
    uom: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    val total = maxOf(1, soldQty + remainingGoodQty + inTransitQty + damagedOrWriteOffQty)
    val soldColor = StatusSuccess
    val remColor = colors.primary
    val trColor = StatusInfo
    val dmgColor = StatusError

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            var x = 0f
            val segments = listOf(
                soldQty to soldColor,
                remainingGoodQty to remColor,
                inTransitQty to trColor,
                damagedOrWriteOffQty to dmgColor
            )
            segments.forEach { (q, c) ->
                if (q > 0) {
                    val w = size.width * (q.toFloat() / total.toFloat())
                    drawRect(color = c, topLeft = Offset(x, 0f), size = Size(w, size.height))
                    x += w
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${if (isBn) "বিক্রীত" else "Sold"} ${Formatters.formatNumber(soldQty.toLong(), language)} $uom",
                style = MaterialTheme.typography.labelSmall,
                color = soldColor
            )
            Text(
                text = "${if (isBn) "মজুদ" else "Good Stock"} ${Formatters.formatNumber(remainingGoodQty.toLong(), language)} $uom",
                style = MaterialTheme.typography.labelSmall,
                color = remColor
            )
            if (inTransitQty > 0) {
                Text(
                    text = "${if (isBn) "ট্রান্সফার" else "In-Transit"} ${Formatters.formatNumber(inTransitQty.toLong(), language)} $uom",
                    style = MaterialTheme.typography.labelSmall,
                    color = trColor
                )
            }
            if (damagedOrWriteOffQty > 0) {
                Text(
                    text = "${if (isBn) "নষ্ট/রাইট-অফ" else "Damage/Write-Off"} ${Formatters.formatNumber(damagedOrWriteOffQty.toLong(), language)} $uom",
                    style = MaterialTheme.typography.labelSmall,
                    color = dmgColor
                )
            }
        }
    }
}

@Composable
fun VaultMetadataEditDialog(
    initialTitle: String,
    initialNotes: String?,
    initialTags: List<String>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (title: String, notes: String, tags: List<String>) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    var title by remember { mutableStateOf(initialTitle) }
    var notes by remember { mutableStateOf(initialNotes.orEmpty()) }
    var tagsInput by remember { mutableStateOf(initialTags.joinToString(", ")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isBn) "আর্কাইভ তথ্য সম্পাদনা" else "Edit Vault Folder Metadata",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isBn) {
                        "এটি শুধু প্রদর্শনের শিরোনাম, ট্যাগ ও নোট পরিবর্তন করে — কোনো হিসাব বা আর্থিক ডাটা বদলায় না।"
                    } else {
                        "Changes display title, tags, and notes only — financial and stock ledgers remain untouched."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isBn) "শিরোনাম" else "Display Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text(if (isBn) "ট্যাগ (কমা দিয়ে আলাদা করুন)" else "Tags (comma-separated)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isBn) "নোট" else "Notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedTags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                    onSave(title.trim().ifEmpty { initialTitle }, notes.trim(), parsedTags)
                },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(if (isBn) "সংরক্ষণ" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (isBn) "বাতিল" else "Cancel")
            }
        }
    )
}

fun openPlatformDatePicker(
    context: android.content.Context,
    initialMillis: Long,
    maxDateMillis: Long? = System.currentTimeMillis(),
    onDatePicked: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
    val dlg = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val picked = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            onDatePicked(picked)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    )
    if (maxDateMillis != null) {
        dlg.datePicker.maxDate = maxDateMillis
    }
    dlg.show()
}

@Composable
fun VaultFilterDialog(
    currentFilter: ImportVaultFilter,
    currentDatePreset: VaultDatePreset,
    filterOptions: ImportVaultFilterOptions,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onApply: (
        datePreset: VaultDatePreset,
        customFromMillis: Long?,
        customToMillis: Long?,
        customAsOfMillis: Long?,
        dateFilterTarget: VaultDateFilterTarget,
        supplierId: String?,
        warehouseId: String?,
        operationalStatus: OperationalStatus?,
        commercialStatus: CommercialStatus?,
        onlyProfitable: Boolean,
        onlyLossMaking: Boolean,
        onlyHighAging: Boolean,
        onlyPendingCost: Boolean,
        allocationRuleId: String?
    ) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val context = LocalContext.current
    var preset by remember { mutableStateOf(currentDatePreset) }
    var customFromMillis by remember {
        mutableStateOf(currentFilter.dateFromMillis ?: (System.currentTimeMillis() - 7L * 24 * 3600 * 1000))
    }
    var customToMillis by remember {
        mutableStateOf(currentFilter.dateToMillis ?: System.currentTimeMillis())
    }
    var customAsOfMillis by remember { mutableStateOf(currentFilter.asOfMillis) }
    var dateFilterTarget by remember { mutableStateOf(currentFilter.dateFilterTarget) }
    var supplierId by remember { mutableStateOf(currentFilter.supplierId) }
    var warehouseId by remember { mutableStateOf(currentFilter.warehouseId) }
    var opStatus by remember { mutableStateOf(currentFilter.operationalStatus) }
    var comStatus by remember { mutableStateOf(currentFilter.commercialStatus) }
    var onlyProfitable by remember { mutableStateOf(currentFilter.onlyProfitable) }
    var onlyLossMaking by remember { mutableStateOf(currentFilter.onlyLossMaking) }
    var onlyHighAging by remember { mutableStateOf(currentFilter.onlyHighAging) }
    var onlyPendingCost by remember { mutableStateOf(currentFilter.onlyPendingCost) }
    var allocationRuleId by remember { mutableStateOf(currentFilter.allocationRuleId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBn) "ফিল্টার ও সময়সীমা" else "Filters & As-Of Date",
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onClear(); onDismiss() }) {
                    Text(if (isBn) "সব রিসেট" else "Reset All")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isBn) "সময়সীমা (Date Scope)" else "Date Scope",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VaultDatePreset.entries.forEach { p ->
                        val label = when (p) {
                            VaultDatePreset.LIFETIME -> if (isBn) "সর্বমোট (Lifetime)" else "Lifetime"
                            VaultDatePreset.TODAY -> if (isBn) "আজ" else "Today"
                            VaultDatePreset.THIS_MONTH -> if (isBn) "এই মাস" else "This Month"
                            VaultDatePreset.LAST_30_DAYS -> if (isBn) "৩০ দিন" else "Last 30d"
                            VaultDatePreset.LAST_90_DAYS -> if (isBn) "৯০ দিন" else "Last 90d"
                            VaultDatePreset.CUSTOM_RANGE -> if (isBn) "কাস্টম পরিসর" else "Custom Range"
                            VaultDatePreset.CUSTOM_AS_OF -> if (isBn) "নির্দিষ্ট তারিখ (As-Of)" else "As-Of Date"
                        }
                        FilterOptionChip(
                            label = label,
                            selected = preset == p,
                            onClick = {
                                preset = p
                                if (p == VaultDatePreset.CUSTOM_AS_OF && customAsOfMillis == null) {
                                    customAsOfMillis = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
                                }
                            }
                        )
                    }
                }
                if (preset == VaultDatePreset.CUSTOM_RANGE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                openPlatformDatePicker(context, customFromMillis) { customFromMillis = it }
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${if (isBn) "শুরু" else "From"}: ${Formatters.formatDate(customFromMillis, language)}", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = {
                                openPlatformDatePicker(context, customToMillis) { customToMillis = it }
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${if (isBn) "শেষ" else "To"}: ${Formatters.formatDate(customToMillis, language)}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                if (preset == VaultDatePreset.CUSTOM_AS_OF) {
                    val asOfVal = customAsOfMillis ?: System.currentTimeMillis()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                openPlatformDatePicker(context, asOfVal) { customAsOfMillis = it }
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("${if (isBn) "তারিখ:" else "As Of:"} ${Formatters.formatDate(asOfVal, language)}")
                        }
                        OutlinedButton(
                            onClick = { customAsOfMillis = asOfVal - 7L * 24 * 3600 * 1000 },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("-7d")
                        }
                        OutlinedButton(
                            onClick = { customAsOfMillis = minOf(System.currentTimeMillis(), asOfVal + 7L * 24 * 3600 * 1000) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("+7d")
                        }
                    }
                }
                if (preset != VaultDatePreset.LIFETIME && preset != VaultDatePreset.CUSTOM_AS_OF) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isBn) "তারিখ ফিল্টারের ধরন" else "Period Filter Target",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterOptionChip(
                                label = if (isBn) "বিক্রয় ও ব্যয়ের তারিখ (Activity)" else "Activity Dates (Sales/Exp)",
                                selected = dateFilterTarget == VaultDateFilterTarget.ACTIVITY_DATES,
                                onClick = { dateFilterTarget = VaultDateFilterTarget.ACTIVITY_DATES }
                            )
                            FilterOptionChip(
                                label = if (isBn) "PO/চালান শুরুর তারিখ (Cohort)" else "Import Cohort Dates",
                                selected = dateFilterTarget == VaultDateFilterTarget.IMPORT_COHORT_DATES,
                                onClick = { dateFilterTarget = VaultDateFilterTarget.IMPORT_COHORT_DATES }
                            )
                        }
                    }
                }

                HorizontalDivider()
                Text(
                    text = if (isBn) "দ্রুত ফিল্টার" else "Quick Performance Filters",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterOptionChip(
                        label = if (isBn) "শুধু লাভজনক" else "Profitable Only",
                        selected = onlyProfitable,
                        onClick = { onlyProfitable = !onlyProfitable; if (onlyProfitable) onlyLossMaking = false }
                    )
                    FilterOptionChip(
                        label = if (isBn) "শুধু লোকসান" else "Loss Making Only",
                        selected = onlyLossMaking,
                        onClick = { onlyLossMaking = !onlyLossMaking; if (onlyLossMaking) onlyProfitable = false }
                    )
                    FilterOptionChip(
                        label = if (isBn) "৯০+ দিন পুরনো স্টক" else "90+ Days Aging",
                        selected = onlyHighAging,
                        onClick = { onlyHighAging = !onlyHighAging }
                    )
                    FilterOptionChip(
                        label = if (isBn) "অসমাপ্ত খরচ (Provisional)" else "Pending / Provisional Cost",
                        selected = onlyPendingCost,
                        onClick = { onlyPendingCost = !onlyPendingCost }
                    )
                }

                if (filterOptions.suppliers.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = if (isBn) "সরবরাহকারী" else "Supplier",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterOptionChip(
                            label = if (isBn) "সবাই" else "All Suppliers",
                            selected = supplierId == null,
                            onClick = { supplierId = null }
                        )
                        filterOptions.suppliers.forEach { (id, name) ->
                            FilterOptionChip(
                                label = name,
                                selected = supplierId == id,
                                onClick = { supplierId = if (supplierId == id) null else id }
                            )
                        }
                    }
                }

                if (filterOptions.warehouses.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = if (isBn) "গুদাম" else "Warehouse",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterOptionChip(
                            label = if (isBn) "সব গুদাম" else "All Warehouses",
                            selected = warehouseId == null,
                            onClick = { warehouseId = null }
                        )
                        filterOptions.warehouses.forEach { (id, name) ->
                            FilterOptionChip(
                                label = name,
                                selected = warehouseId == id,
                                onClick = { warehouseId = if (warehouseId == id) null else id }
                            )
                        }
                    }
                }

                HorizontalDivider()
                Text(
                    text = if (isBn) "অপারেশনাল স্ট্যাটাস" else "Operational Status",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterOptionChip(
                        label = if (isBn) "সব" else "All",
                        selected = opStatus == null,
                        onClick = { opStatus = null }
                    )
                    OperationalStatus.entries.forEach { st ->
                        FilterOptionChip(
                            label = operationalStatusLabel(st, language),
                            selected = opStatus == st,
                            onClick = { opStatus = if (opStatus == st) null else st }
                        )
                    }
                }

                HorizontalDivider()
                Text(
                    text = if (isBn) "বাণিজ্যিক অবস্থা" else "Commercial Status",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterOptionChip(
                        label = if (isBn) "সব" else "All",
                        selected = comStatus == null,
                        onClick = { comStatus = null }
                    )
                    CommercialStatus.entries.forEach { st ->
                        FilterOptionChip(
                            label = commercialStatusLabel(st, language),
                            selected = comStatus == st,
                            onClick = { comStatus = if (comStatus == st) null else st }
                        )
                    }
                }

                if (filterOptions.allocationRules.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = if (isBn) "শেয়ারড ওভারহেড নিয়ম (Opt-in)" else "Shared Overhead Rule (Opt-in)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterOptionChip(
                            label = if (isBn) "ডিফল্ট (Unallocated)" else "Default (Unallocated)",
                            selected = allocationRuleId == null,
                            onClick = { allocationRuleId = null }
                        )
                        filterOptions.allocationRules.forEach { rule ->
                            FilterOptionChip(
                                label = "${rule.ruleName} (${rule.allocationBasis})",
                                selected = allocationRuleId == rule.id,
                                onClick = { allocationRuleId = if (allocationRuleId == rule.id) null else rule.id }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(
                        preset,
                        if (preset == VaultDatePreset.CUSTOM_RANGE) customFromMillis else null,
                        if (preset == VaultDatePreset.CUSTOM_RANGE) customToMillis else null,
                        customAsOfMillis,
                        dateFilterTarget,
                        supplierId,
                        warehouseId,
                        opStatus,
                        comStatus,
                        onlyProfitable,
                        onlyLossMaking,
                        onlyHighAging,
                        onlyPendingCost,
                        allocationRuleId
                    )
                    onDismiss()
                },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isBn) "প্রয়োগ করুন" else "Apply Filters")
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
fun FilterOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Checkbox, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) colors.onPrimaryContainer else colors.onSurface
            )
        }
    }
}
