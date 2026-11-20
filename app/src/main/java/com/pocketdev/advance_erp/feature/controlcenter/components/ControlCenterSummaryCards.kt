package com.pocketdev.advance_erp.feature.controlcenter.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusInfo
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.domain.model.BusinessSummaryDashboardData
import com.pocketdev.advance_erp.domain.model.ReportCategory

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlCenterSummaryCards(
    summary: BusinessSummaryDashboardData,
    canViewProfit: Boolean,
    language: AppLanguage,
    currencyCode: CurrencyCode,
    onCategoryClick: (ReportCategory) -> Unit,
    modifier: Modifier = Modifier,
    isOwnerAdmin: Boolean = false,
    isExpanded: Boolean? = null,
    onToggleExpand: (() -> Unit)? = null,
    onUnlockProfit: (() -> Unit)? = null
) {
    val isBn = language == AppLanguage.BANGLA
    var internalExpanded by rememberSaveable { mutableStateOf(false) }
    val expanded = isExpanded ?: internalExpanded
    val toggleExpand = onToggleExpand ?: { internalExpanded = !internalExpanded }

    Column(modifier = modifier.fillMaxWidth()) {
        // Primary 4 Financial Indicators in 2x2 Grid
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Total Sales
            ControlCenterCardItem(
                modifier = Modifier.weight(1f),
                title = if (isBn) "মোট বিক্রয়" else "Total Sales",
                amountMinor = summary.totalSalesMinor,
                currencyCode = currencyCode,
                language = language,
                icon = Icons.Outlined.TrendingUp,
                accentColor = StatusSuccess,
                onClick = { onCategoryClick(ReportCategory.SALES) }
            )

            // 2. Total Payments Received
            ControlCenterCardItem(
                modifier = Modifier.weight(1f),
                title = if (isBn) "মোট টাকা গ্রহণ" else "Payments Received",
                amountMinor = summary.totalReceivedMinor,
                currencyCode = currencyCode,
                language = language,
                icon = Icons.Outlined.ArrowDownward,
                accentColor = StatusSuccess,
                onClick = { onCategoryClick(ReportCategory.RECEIPT) }
            )

            // 3. Total Purchases
            ControlCenterCardItem(
                modifier = Modifier.weight(1f),
                title = if (isBn) "মোট ক্রয়" else "Total Purchases",
                amountMinor = summary.totalPurchaseMinor,
                currencyCode = currencyCode,
                language = language,
                icon = Icons.Outlined.ShoppingCart,
                accentColor = PrimaryPurple,
                onClick = { onCategoryClick(ReportCategory.PURCHASE) }
            )

            // 4. Total Payable (separate from Payments Made)
            ControlCenterCardItem(
                modifier = Modifier.weight(1f),
                title = if (isBn) "মোট দেনা" else "Total Payable",
                amountMinor = summary.totalOutstandingPayableMinor,
                currencyCode = currencyCode,
                language = language,
                icon = Icons.Outlined.MonetizationOn,
                accentColor = StatusError,
                onClick = { onCategoryClick(ReportCategory.SUPPLIER_LEDGER) }
            )
        }

        // Expandable Additional KPIs
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 5. Total Payments Made (separate metric from Payable)
                    ControlCenterCardItem(
                        modifier = Modifier.weight(1f),
                        title = if (isBn) "মোট পরিশোধ" else "Payments Made",
                        amountMinor = summary.totalPaidMinor,
                        currencyCode = currencyCode,
                        language = language,
                        icon = Icons.Outlined.ArrowUpward,
                        accentColor = StatusInfo,
                        onClick = { onCategoryClick(ReportCategory.PAYMENT) }
                    )

                    // 6. Total Outstanding Receivable
                    ControlCenterCardItem(
                        modifier = Modifier.weight(1f),
                        title = if (isBn) "মোট পাওনা (বকেয়া)" else "Receivable Due",
                        amountMinor = summary.totalOutstandingReceivableMinor,
                        currencyCode = currencyCode,
                        language = language,
                        icon = Icons.Outlined.AttachMoney,
                        accentColor = StatusError,
                        onClick = { onCategoryClick(ReportCategory.CUSTOMER_LEDGER) }
                    )

                    // 7. Total Expenses
                    ControlCenterCardItem(
                        modifier = Modifier.weight(1f),
                        title = if (isBn) "মোট ব্যয়" else "Total Expenses",
                        amountMinor = summary.totalExpensesMinor,
                        currencyCode = currencyCode,
                        language = language,
                        icon = Icons.Outlined.MoneyOff,
                        accentColor = StatusWarning,
                        onClick = { onCategoryClick(ReportCategory.EXPENSE) }
                    )

                    // 8. Total Deliveries
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCategoryClick(ReportCategory.DELIVERY) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.6.dp, ERPDivider.copy(alpha = 0.7f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SecondaryTeal.copy(alpha = 0.12f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocalShipping,
                                        contentDescription = null,
                                        tint = SecondaryTeal,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isBn) "মোট ডেলিভারি" else "Total Deliveries",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${Formatters.formatNumber(summary.totalDeliveryCount.toLong(), language)} ${if (isBn) "টি" else "Orders"}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryTeal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // 9. Sales Return
                    ControlCenterCardItem(
                        modifier = Modifier.weight(1f),
                        title = if (isBn) "বিক্রয় ফেরত" else "Sales Return",
                        amountMinor = summary.totalSalesReturnMinor,
                        currencyCode = currencyCode,
                        language = language,
                        icon = Icons.Outlined.Replay,
                        accentColor = StatusError,
                        onClick = { onCategoryClick(ReportCategory.SALES_RETURN) }
                    )

                    // 10. Purchase Return
                    ControlCenterCardItem(
                        modifier = Modifier.weight(1f),
                        title = if (isBn) "ক্রয় ফেরত" else "Purchase Return",
                        amountMinor = summary.totalPurchaseReturnMinor,
                        currencyCode = currencyCode,
                        language = language,
                        icon = Icons.Outlined.Replay,
                        accentColor = StatusWarning,
                        onClick = { onCategoryClick(ReportCategory.PURCHASE_RETURN) }
                    )

                    // Profit Cards: STRICTLY Owner / Admin only + Biometric/PIN protected
                    if (isOwnerAdmin) {
                        if (canViewProfit) {
                            ControlCenterCardItem(
                                modifier = Modifier.weight(1f),
                                title = if (isBn) "মোট মুনাফা (Gross)" else "Gross Profit",
                                amountMinor = summary.grossProfitMinor,
                                currencyCode = currencyCode,
                                language = language,
                                icon = if (summary.grossProfitMinor >= 0) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown,
                                accentColor = if (summary.grossProfitMinor >= 0) StatusSuccess else StatusError,
                                onClick = { onCategoryClick(ReportCategory.SALES) }
                            )
                            ControlCenterCardItem(
                                modifier = Modifier.weight(1f),
                                title = if (isBn) "নিট মুনাফা (Net)" else "Net Profit",
                                amountMinor = summary.netProfitMinor,
                                currencyCode = currencyCode,
                                language = language,
                                icon = if (summary.netProfitMinor >= 0) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown,
                                accentColor = if (summary.netProfitMinor >= 0) StatusSuccess else StatusError,
                                onClick = { onCategoryClick(ReportCategory.SALES) }
                            )
                        } else if (onUnlockProfit != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUnlockProfit() },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PrimaryPurple.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Fingerprint,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isBn) "লাভের হিসাব দেখতে ফিঙ্গারপ্রিন্ট যাচাই করুন" else "Verify biometric to unlock Profit KPIs",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryPurple
                                    )
                                }
                            }
                        }
                    }
                }

                if (isOwnerAdmin && canViewProfit && onUnlockProfit != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUnlockProfit() }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBn) "লাভের তথ্য লুকান" else "Lock profit visibility",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Toggle Expand / Collapse Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clickable { toggleExpand() }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (expanded) {
                    if (isBn) "সারসংক্ষেপ সংক্ষেপ করুন" else "Collapse Summary"
                } else {
                    if (isBn) "আরও সারসংক্ষেপ দেখুন" else "View More Summary"
                },
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                color = PrimaryPurple,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ControlCenterCardItem(
    modifier: Modifier = Modifier,
    title: String,
    amountMinor: Long,
    currencyCode: CurrencyCode,
    language: AppLanguage,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.6.dp, ERPDivider.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = Formatters.formatMoney(amountMinor, currencyCode, language),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
