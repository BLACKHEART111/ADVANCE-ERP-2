package com.pocketdev.advance_erp.feature.sales

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.pocketdev.advance_erp.core.designsystem.LocalIsDarkTheme
import com.pocketdev.advance_erp.core.designsystem.rememberImageModel
import com.pocketdev.advance_erp.core.engine.InvoiceTotalsCalculation
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.core.util.sellableStockForProduct
import com.pocketdev.advance_erp.core.util.sellableStockForVariant
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.StockBalance
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.repository.CustomerSummary
import com.pocketdev.advance_erp.domain.repository.NewCustomerDraft
import java.io.File
import java.util.Locale

@Immutable
internal data class SalesInvoicePalette(
    val isDark: Boolean,
    val sheetBg: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val subtleSurface: Color,
    val inputBg: Color,
    val inputBorder: Color,
    val primaryPurple: Color,
    val primaryPurpleStrong: Color,
    val secondaryTeal: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val customerGradientStart: Color,
    val customerGradientEnd: Color,
    val customerBorder: Color,
    val avatarBg: Color,
    val iconBadgeBg: Color,
    val variantChipBg: Color,
    val variantChipText: Color,
    val itemTotalBg: Color,
    val itemTotalLabel: Color,
    val itemTotalValue: Color,
    val kpiSalesBg: Color,
    val kpiSalesIconBg: Color,
    val kpiSalesIcon: Color,
    val kpiPaidBg: Color,
    val kpiPaidIconBg: Color,
    val kpiPaidIcon: Color,
    val kpiPaidText: Color,
    val kpiDueBg: Color,
    val kpiDueIconBg: Color,
    val kpiDueIcon: Color,
    val kpiDueText: Color,
    val kpiReturnBg: Color,
    val kpiReturnIconBg: Color,
    val kpiReturnIcon: Color,
    val fullPayBg: Color,
    val fullPayBorder: Color,
    val fullPayText: Color,
    val chevronBoxBg: Color,
    val stockOkText: Color,
    val errorText: Color,
    val profitText: Color,
    val footerBg: Color,
    val cancelBorder: Color,
    val draftBg: Color,
    val draftBorder: Color,
    val draftText: Color,
    val confirmGreen: Color
)

@Composable
internal fun rememberSalesInvoicePalette(): SalesInvoicePalette {
    val dark = LocalIsDarkTheme.current
    return remember(dark) {
        if (!dark) {
            SalesInvoicePalette(
                isDark = false,
                sheetBg = Color(0xFFF6F4FA),
                cardBg = Color(0xFFFFFFFF),
                cardBorder = Color(0xFFE4DCF1),
                subtleSurface = Color(0xFFF3EFF9),
                inputBg = Color(0xFFFFFFFF),
                inputBorder = Color(0xFFD5CAE7),
                primaryPurple = Color(0xFF7B1FA2),
                primaryPurpleStrong = Color(0xFF6A1B9A),
                secondaryTeal = Color(0xFF0E7C7B),
                textPrimary = Color(0xFF1F1630),
                textSecondary = Color(0xFF534966),
                textMuted = Color(0xFF7A708B),
                customerGradientStart = Color(0xFFEFE8FB),
                customerGradientEnd = Color(0xFFF9F6FE),
                customerBorder = Color(0xFFDAD0EE),
                avatarBg = Color(0xFFE4D6FA),
                iconBadgeBg = Color(0xFFF1E9FC),
                variantChipBg = Color(0xFFD6F2EE),
                variantChipText = Color(0xFF0F6B61),
                itemTotalBg = Color(0xFFF3EAFD),
                itemTotalLabel = Color(0xFF5E4E7B),
                itemTotalValue = Color(0xFF6A1B9A),
                kpiSalesBg = Color(0xFFF2ECFD),
                kpiSalesIconBg = Color(0xFFE1D2FA),
                kpiSalesIcon = Color(0xFF7B1FA2),
                kpiPaidBg = Color(0xFFEAF8F0),
                kpiPaidIconBg = Color(0xFFCCEEDB),
                kpiPaidIcon = Color(0xFF15803D),
                kpiPaidText = Color(0xFF0E7437),
                kpiDueBg = Color(0xFFFDF0F2),
                kpiDueIconBg = Color(0xFFFAD8DE),
                kpiDueIcon = Color(0xFFC62828),
                kpiDueText = Color(0xFFC62828),
                kpiReturnBg = Color(0xFFEDF5FD),
                kpiReturnIconBg = Color(0xFFD3E6FA),
                kpiReturnIcon = Color(0xFF1D6FB8),
                fullPayBg = Color(0xFFE2F6F3),
                fullPayBorder = Color(0xFF4DB6AC),
                fullPayText = Color(0xFF00695C),
                chevronBoxBg = Color(0xFFF1E8FB),
                stockOkText = Color(0xFF167347),
                errorText = Color(0xFFDC2626),
                profitText = Color(0xFF15803D),
                footerBg = Color(0xFFFFFFFF),
                cancelBorder = Color(0xFFD7CEE6),
                draftBg = Color(0xFFEDF1F7),
                draftBorder = Color(0xFFCBD5E1),
                draftText = Color(0xFF1E293B),
                confirmGreen = Color(0xFF159947)
            )
        } else {
            SalesInvoicePalette(
                isDark = true,
                sheetBg = Color(0xFF14111A),
                cardBg = Color(0xFF201A29),
                cardBorder = Color(0xFF392E49),
                subtleSurface = Color(0xFF2A2236),
                inputBg = Color(0xFF191421),
                inputBorder = Color(0xFF4B3D60),
                primaryPurple = Color(0xFFD08BE8),
                primaryPurpleStrong = Color(0xFF9C27B0),
                secondaryTeal = Color(0xFF64D8D6),
                textPrimary = Color(0xFFF5EFFC),
                textSecondary = Color(0xFFC9BEDB),
                textMuted = Color(0xFF9B8EB0),
                customerGradientStart = Color(0xFF281E38),
                customerGradientEnd = Color(0xFF211A2E),
                customerBorder = Color(0xFF44335D),
                avatarBg = Color(0xFF3B2858),
                iconBadgeBg = Color(0xFF332547),
                variantChipBg = Color(0xFF164341),
                variantChipText = Color(0xFF7BE8DF),
                itemTotalBg = Color(0xFF312147),
                itemTotalLabel = Color(0xFFCBB8E6),
                itemTotalValue = Color(0xFFE2AEF6),
                kpiSalesBg = Color(0xFF271D38),
                kpiSalesIconBg = Color(0xFF3C2B57),
                kpiSalesIcon = Color(0xFFD79CF0),
                kpiPaidBg = Color(0xFF162B21),
                kpiPaidIconBg = Color(0xFF1F4733),
                kpiPaidIcon = Color(0xFF6EE7A6),
                kpiPaidText = Color(0xFF7CE8AF),
                kpiDueBg = Color(0xFF341B22),
                kpiDueIconBg = Color(0xFF532530),
                kpiDueIcon = Color(0xFFFF8A8A),
                kpiDueText = Color(0xFFFF8A8A),
                kpiReturnBg = Color(0xFF182538),
                kpiReturnIconBg = Color(0xFF233958),
                kpiReturnIcon = Color(0xFF7DC0FF),
                fullPayBg = Color(0xFF163C3B),
                fullPayBorder = Color(0xFF3B9D96),
                fullPayText = Color(0xFF7FE7DF),
                chevronBoxBg = Color(0xFF35264B),
                stockOkText = Color(0xFF7CE8AF),
                errorText = Color(0xFFFF8A8A),
                profitText = Color(0xFF7CE8AF),
                footerBg = Color(0xFF1B1523),
                cancelBorder = Color(0xFF4A3D5C),
                draftBg = Color(0xFF292434),
                draftBorder = Color(0xFF4A425B),
                draftText = Color(0xFFEBE4F5),
                confirmGreen = Color(0xFF169E4B)
            )
        }
    }
}

internal val purplePillGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF9C27B0), Color(0xFF6A1B9A))
)

internal val activeTileGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF9426AD), Color(0xFF5C157B))
)

@Composable
internal fun SalesInvoiceHeroBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColors = if (isDark) {
        listOf(Color(0xFF2C0744), Color(0xFF551573), Color(0xFF135C5E))
    } else {
        listOf(Color(0xFF450B67), Color(0xFF7E1FA4), Color(0xFF9C27B0), Color(0xFF26A69A))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(colors = bgColors))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawPath(
                path = Path().apply {
                    moveTo(w * 0.48f, 0f)
                    cubicTo(w * 0.68f, h * 0.45f, w * 0.78f, h * 0.25f, w, h * 0.65f)
                    lineTo(w, 0f)
                    close()
                },
                color = Color(0xFF4DD0E1).copy(alpha = if (isDark) 0.16f else 0.24f)
            )
            drawPath(
                path = Path().apply {
                    moveTo(w * 0.22f, h)
                    cubicTo(w * 0.42f, h * 0.48f, w * 0.68f, h * 0.78f, w * 0.92f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h)
                    close()
                },
                color = Color.White.copy(alpha = if (isDark) 0.04f else 0.08f)
            )
        }
    }
}

@Composable
internal fun SalesInvoiceHeroHeader(
    title: String,
    subtitle: String,
    canToggleCostOrProfit: Boolean,
    showCostAndProfit: Boolean,
    onBack: () -> Unit,
    onToggleCostProfit: () -> Unit,
    onScanBarcode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 10.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(modifier = Modifier.width(2.dp))

        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 11.sp,
                letterSpacing = (-0.15).sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canToggleCostOrProfit) {
                HeroCircleActionButton(
                    icon = if (showCostAndProfit) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = "Cost & Profit Visibility",
                    isActive = showCostAndProfit,
                    onClick = onToggleCostProfit
                )
            }
            HeroCircleActionButton(
                icon = Icons.Outlined.QrCodeScanner,
                contentDescription = "Scan Barcode",
                isActive = true,
                onClick = onScanBarcode
            )
        }
    }
}

@Composable
private fun HeroCircleActionButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f))
            .padding(2.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) Color(0xFF6A1B9A) else Color(0xFF8C7FA3),
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
internal fun SalesCustomerHeroSection(
    customer: Customer?,
    pendingNewCustomer: NewCustomerDraft?,
    summary: CustomerSummary?,
    isBangla: Boolean,
    palette: SalesInvoicePalette,
    onChangeCustomerClick: () -> Unit
) {
    val displayName = pendingNewCustomer?.name ?: customer?.name
        ?: if (isBangla) "গ্রাহক নির্বাচন করুন" else "Select Customer"
    val displayPhone = pendingNewCustomer?.phone ?: customer?.phone ?: "-"
    val displayAddress = pendingNewCustomer?.address ?: customer?.address.orEmpty()
    val photoPath = pendingNewCustomer?.profileImagePath ?: customer?.profileImagePath
    val validPhoto = remember(photoPath) {
        photoPath?.takeIf { it.isNotBlank() && File(it).exists() }
    }

    val totalSalesMinor = summary?.totalSalesMinor ?: 0L
    val lifetimePaidMinor = summary?.lifetimePaidMinor ?: 0L
    val historicalDueMinor = pendingNewCustomer?.openingBalanceMinor
        ?: summary?.outstandingBalanceMinor
        ?: customer?.outstandingBalanceMinor
        ?: 0L
    val totalReturnsMinor = summary?.totalReturnsMinor ?: 0L

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Customer Banner Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, palette.customerBorder),
            shadowElevation = if (palette.isDark) 0.dp else 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(palette.customerGradientStart, palette.customerGradientEnd)
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(palette.avatarBg)
                        .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (validPhoto != null) {
                        AsyncImage(
                            model = rememberImageModel(validPhoto),
                            contentDescription = displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.PersonOutline,
                            contentDescription = "Customer",
                            tint = palette.primaryPurple,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(9.dp))

                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = if (pendingNewCustomer != null) "$displayName (${if (isBangla) "নতুন" else "New"})" else displayName,
                        fontSize = 14.sp,
                        letterSpacing = (-0.2).sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 2,
                        lineHeight = 17.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = "Phone",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = displayPhone,
                            fontSize = 11.5.sp,
                            color = palette.textSecondary,
                            maxLines = 1
                        )
                    }
                    if (displayAddress.isNotBlank()) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = "Address",
                                tint = palette.textMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = displayAddress,
                                fontSize = 11.sp,
                                letterSpacing = (-0.15).sp,
                                color = palette.textSecondary,
                                maxLines = 2,
                                lineHeight = 14.sp,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Surface(
                    onClick = onChangeCustomerClick,
                    shape = RoundedCornerShape(10.dp),
                    color = palette.cardBg,
                    border = BorderStroke(1.dp, palette.customerBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Change",
                            tint = palette.primaryPurple,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBangla) "পরিবর্তন" else "Change",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary
                        )
                    }
                }
            }
        }

        // 4 Single-Row Historical KPI Badges (kept strictly separate from current invoice due!)
        fun kpiWeight(title: String, minor: Long): Float {
            val vLen = ("৳" + Formatters.formatMinorUnit(minor)).length
            return maxOf(title.length * 0.72f, vLen * 1.02f).coerceIn(6.8f, 11.5f)
        }
        val salesTitle = if (isBangla) "মোট বিক্রয়" else "Total Sales"
        val paidTitle = if (isBangla) "পরিশোধ" else "Paid"
        val dueTitle = if (isBangla) "আগের বকেয়া" else "Due"
        val returnsTitle = if (isBangla) "ফেরত" else "Returns"

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            CustomerKpiPill(
                title = salesTitle,
                amountMinor = totalSalesMinor,
                icon = Icons.Outlined.ShoppingCart,
                cardBg = palette.kpiSalesBg,
                iconBg = palette.kpiSalesIconBg,
                iconTint = palette.kpiSalesIcon,
                valueColor = palette.textPrimary,
                labelColor = palette.textSecondary,
                modifier = Modifier.weight(kpiWeight(salesTitle, totalSalesMinor))
            )
            CustomerKpiPill(
                title = paidTitle,
                amountMinor = lifetimePaidMinor,
                icon = Icons.Outlined.AccountBalanceWallet,
                cardBg = palette.kpiPaidBg,
                iconBg = palette.kpiPaidIconBg,
                iconTint = palette.kpiPaidIcon,
                valueColor = palette.kpiPaidText,
                labelColor = palette.textSecondary,
                modifier = Modifier.weight(kpiWeight(paidTitle, lifetimePaidMinor))
            )
            CustomerKpiPill(
                title = dueTitle,
                amountMinor = historicalDueMinor,
                icon = Icons.Outlined.Payments,
                cardBg = palette.kpiDueBg,
                iconBg = palette.kpiDueIconBg,
                iconTint = palette.kpiDueIcon,
                valueColor = if (historicalDueMinor > 0L) palette.kpiDueText else palette.textPrimary,
                labelColor = palette.textSecondary,
                modifier = Modifier.weight(kpiWeight(dueTitle, historicalDueMinor))
            )
            CustomerKpiPill(
                title = returnsTitle,
                amountMinor = totalReturnsMinor,
                icon = Icons.Outlined.Replay,
                cardBg = palette.kpiReturnBg,
                iconBg = palette.kpiReturnIconBg,
                iconTint = palette.kpiReturnIcon,
                valueColor = palette.textPrimary,
                labelColor = palette.textSecondary,
                modifier = Modifier.weight(kpiWeight(returnsTitle, totalReturnsMinor))
            )
        }
    }
}

@Composable
private fun CustomerKpiPill(
    title: String,
    amountMinor: Long,
    icon: ImageVector,
    cardBg: Color,
    iconBg: Color,
    iconTint: Color,
    valueColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    val valueStr = "৳" + Formatters.formatMinorUnit(amountMinor)
    val valueFontSize = when {
        valueStr.length >= 11 -> 8.5.sp
        valueStr.length >= 9 -> 9.5.sp
        else -> 10.5.sp
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = cardBg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(19.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(11.5.dp)
                )
            }
            Spacer(modifier = Modifier.width(3.5.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 8.5.sp,
                    letterSpacing = (-0.2).sp,
                    color = labelColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 10.sp
                )
                Text(
                    text = valueStr,
                    fontSize = valueFontSize,
                    letterSpacing = (-0.25).sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
internal fun SalesWarehouseSelectorCard(
    warehouses: List<Warehouse>,
    selectedWarehouse: Warehouse?,
    isBangla: Boolean,
    palette: SalesInvoicePalette,
    onSelectWarehouse: (Warehouse) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true },
            shape = RoundedCornerShape(14.dp),
            color = palette.cardBg,
            border = BorderStroke(1.dp, palette.cardBorder),
            shadowElevation = if (palette.isDark) 0.dp else 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(palette.iconBadgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warehouse,
                        contentDescription = null,
                        tint = palette.primaryPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBangla) "বিতরণ ওয়্যারহাউস *" else "Dispatch Warehouse *",
                        fontSize = 10.sp,
                        color = palette.textMuted,
                        lineHeight = 12.sp
                    )
                    Text(
                        text = selectedWarehouse?.let { "${it.name} (${it.code})" }
                            ?: if (isBangla) "ওয়্যারহাউস নির্বাচন করুন" else "Select Warehouse",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = "Select Warehouse",
                    tint = palette.textSecondary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(palette.cardBg)
        ) {
            warehouses.forEach { wh ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Storefront,
                                contentDescription = null,
                                tint = palette.primaryPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${wh.name} (${wh.code})",
                                color = palette.textPrimary,
                                fontWeight = if (wh.id == selectedWarehouse?.id) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    onClick = {
                        onSelectWarehouse(wh)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun SalesProductThumbnail(
    imageUri: String?,
    contentDescription: String,
    size: Dp,
    cornerRadius: Dp,
    palette: SalesInvoicePalette
) {
    val model = remember(imageUri) { rememberImageModel(imageUri) }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(palette.subtleSurface)
            .border(1.dp, palette.cardBorder, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Inventory2,
                contentDescription = contentDescription,
                tint = palette.primaryPurple.copy(alpha = 0.65f),
                modifier = Modifier.size(size * 0.45f)
            )
        }
    }
}

@Composable
internal fun DiscountModeToggle(
    isPercent: Boolean,
    palette: SalesInvoicePalette,
    height: Dp = 33.dp,
    segmentWidth: Dp = 23.dp,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.subtleSurface)
            .border(1.dp, palette.inputBorder, RoundedCornerShape(8.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(true to "%", false to "৳").forEach { (pctMode, label) ->
            val selected = isPercent == pctMode
            Box(
                modifier = Modifier
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (selected) Modifier.background(purplePillGradient)
                        else Modifier.background(Color.Transparent)
                    )
                    .clickable { onToggle(pctMode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else palette.textSecondary
                )
            }
        }
    }
}

@Composable
internal fun CompactNumericInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    palette: SalesInvoicePalette,
    modifier: Modifier = Modifier,
    height: Dp = 33.dp,
    fontSize: TextUnit = 12.5.sp,
    textAlign: TextAlign = TextAlign.Center,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    suffix: String? = null,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier.height(height),
        shape = RoundedCornerShape(9.dp),
        color = if (enabled) palette.inputBg else palette.subtleSurface,
        border = BorderStroke(1.dp, palette.inputBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    color = if (enabled) palette.textPrimary else palette.textMuted,
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = textAlign
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = SolidColor(palette.primaryPurple),
                modifier = Modifier.weight(1f)
            )
            if (!suffix.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = suffix,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textSecondary
                )
            }
        }
    }
}

@Composable
internal fun SalesPaymentSettlementCard(
    paymentMethod: String,
    selectedAccountId: String,
    liquidAccounts: List<ChartOfAccount>,
    paidAmountStr: String,
    isFullPaySelected: Boolean,
    grandTotalMinor: Long,
    isBangla: Boolean,
    palette: SalesInvoicePalette,
    onSelectPaymentMethod: (String) -> Unit,
    onSelectAccountId: (String) -> Unit,
    onPaidAmountChange: (String) -> Unit,
    onFullPayClick: () -> Unit
) {
    val channelAccounts = remember(paymentMethod, liquidAccounts) {
        when (paymentMethod) {
            "CASH" -> liquidAccounts.filter { it.liquidKind?.name == "CASH" }
            "BANK" -> liquidAccounts.filter { it.liquidKind?.name == "BANK" }
            "MFS" -> liquidAccounts.filter { it.liquidKind?.name == "MOBILE_BANKING" }
            else -> emptyList()
        }
    }
    val activeAccount = channelAccounts.find { it.id == selectedAccountId } ?: channelAccounts.firstOrNull()
    var accountMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val options = listOf(
        Triple("CASH", "Cash", "(নগদ)") to Icons.Outlined.Payments,
        Triple("BANK", "Bank", "(ব্যাংক)") to Icons.Outlined.AccountBalance,
        Triple("MFS", "MFS", "(bKash/Nagad)") to Icons.Outlined.Smartphone,
        Triple("CREDIT", "Credit", "(বাকি)") to Icons.Outlined.EventNote
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = palette.cardBg,
        border = BorderStroke(1.dp, palette.cardBorder),
        shadowElevation = if (palette.isDark) 0.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Payments,
                    contentDescription = null,
                    tint = palette.primaryPurple,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBangla) "পেমেন্ট মাধ্যম ও পরিশোধ" else "Payment Method & Settlement",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4-Across Payment Tiles matching reference
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                options.forEach { (meta, icon) ->
                    val (code, titleEn, subBn) = meta
                    val isSelected = paymentMethod == code
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .clickable { onSelectPaymentMethod(code) },
                        shape = RoundedCornerShape(11.dp),
                        color = Color.Transparent,
                        border = if (isSelected) null else BorderStroke(1.dp, palette.cardBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (isSelected) Modifier.background(activeTileGradient)
                                    else Modifier.background(palette.subtleSurface.copy(alpha = 0.45f))
                                )
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(13.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = titleEn,
                                    tint = if (isSelected) Color.White else palette.primaryPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = titleEn,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else palette.textPrimary,
                                    maxLines = 1,
                                    lineHeight = 13.sp
                                )
                                Text(
                                    text = subBn,
                                    fontSize = 8.5.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.88f) else palette.textMuted,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Deposit to Account | Paid Amount (৳) | Full Pay
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Deposit to Account selector
                Box(modifier = Modifier.weight(1.05f)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                tint = palette.primaryPurple,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "জমার হিসাব" else "Deposit to Account",
                                fontSize = 10.sp,
                                color = palette.textSecondary,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val accLabel = when {
                            paymentMethod == "CREDIT" -> if (isBangla) "বকেয়া (হিসাব নেই)" else "Credit (No Cash)"
                            activeAccount == null -> if (isBangla) "হিসাব নেই" else "No Account"
                            isBangla && activeAccount.nameBn.isNotBlank() -> activeAccount.nameBn
                            else -> activeAccount.nameEn
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            onClick = { if (paymentMethod != "CREDIT" && channelAccounts.isNotEmpty()) accountMenuExpanded = true },
                            shape = RoundedCornerShape(9.dp),
                            color = if (paymentMethod == "CREDIT") palette.subtleSurface else palette.inputBg,
                            border = BorderStroke(1.dp, palette.inputBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = accLabel,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (paymentMethod == "CREDIT") palette.textMuted else palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDropDown,
                                    contentDescription = null,
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = accountMenuExpanded,
                        onDismissRequest = { accountMenuExpanded = false },
                        modifier = Modifier.background(palette.cardBg)
                    ) {
                        channelAccounts.forEach { acc ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isBangla && acc.nameBn.isNotBlank()) acc.nameBn else acc.nameEn,
                                        color = palette.textPrimary,
                                        fontWeight = if (acc.id == activeAccount?.id) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSelectAccountId(acc.id)
                                    accountMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Vertical Hairline Divider
                VerticalDivider(
                    modifier = Modifier
                        .height(36.dp)
                        .padding(bottom = 2.dp),
                    color = palette.cardBorder
                )

                // Paid Amount input
                Column(modifier = Modifier.weight(0.95f)) {
                    Text(
                        text = if (isBangla) "পরিশোধ (৳)" else "Paid Amount (৳)",
                        fontSize = 10.sp,
                        color = palette.textSecondary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CompactNumericInputBox(
                        value = if (paymentMethod == "CREDIT") "0.00" else if (isFullPaySelected) Formatters.formatMinorUnit(grandTotalMinor).replace(",", "") else paidAmountStr,
                        onValueChange = onPaidAmountChange,
                        palette = palette,
                        textAlign = TextAlign.Start,
                        enabled = paymentMethod != "CREDIT",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Full Pay button
                Surface(
                    modifier = Modifier.height(36.dp),
                    onClick = onFullPayClick,
                    shape = RoundedCornerShape(9.dp),
                    color = palette.fullPayBg,
                    border = BorderStroke(1.dp, palette.fullPayBorder)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBangla) "সম্পূর্ণ" else "Full Pay",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.fullPayText,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SalesInvoiceSummaryCard(
    itemCount: Int,
    totals: InvoiceTotalsCalculation,
    showProfit: Boolean,
    hasItems: Boolean,
    isBangla: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        shadowElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF390B54), Color(0xFF5A177C), Color(0xFF28073D))
                    )
                )
        ) {
            // Subtle magenta wave curves along the bottom-left matching reference
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawPath(
                    path = Path().apply {
                        moveTo(0f, h * 0.72f)
                        cubicTo(w * 0.25f, h * 0.45f, w * 0.55f, h * 1.05f, w * 0.85f, h * 0.78f)
                        lineTo(w * 0.85f, h)
                        lineTo(0f, h)
                        close()
                    },
                    color = Color(0xFFC026D3).copy(alpha = 0.20f)
                )
                drawPath(
                    path = Path().apply {
                        moveTo(0f, h * 0.86f)
                        cubicTo(w * 0.18f, h * 0.65f, w * 0.40f, h * 1.05f, w * 0.62f, h)
                        lineTo(0f, h)
                        close()
                    },
                    color = Color(0xFFE879F9).copy(alpha = 0.16f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 12.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Pane: Icon + Invoice Summary title
                Row(
                    modifier = Modifier.weight(0.41f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Equalizer,
                        contentDescription = null,
                        tint = Color(0xFFE879F9),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isBangla) "চালান সারাংশ" else "Invoice Summary",
                        color = Color.White,
                        fontSize = 12.sp,
                        letterSpacing = (-0.2).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Vertical Hairline Divider
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    thickness = 1.dp,
                    color = Color.White.copy(alpha = 0.22f)
                )

                // Right Pane: Financial Breakdown
                Column(
                    modifier = Modifier.weight(0.59f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val itemsWord = if (itemCount == 1) "item" else "items"
                    SummaryMetricRow(
                        label = if (isBangla) "উপমোট ($itemCount টি) :" else "Subtotal ($itemCount $itemsWord) :",
                        value = "৳" + Formatters.formatMinorUnit(totals.subtotalMinor),
                        labelColor = Color.White.copy(alpha = 0.85f),
                        valueColor = Color.White,
                        fontSize = 11.sp
                    )
                    SummaryMetricRow(
                        label = if (isBangla) "চালান বাট্টা :" else "Invoice Discount :",
                        value = "(-) ৳" + Formatters.formatMinorUnit(totals.invoiceDiscountMinor),
                        labelColor = Color.White.copy(alpha = 0.85f),
                        valueColor = if (totals.invoiceDiscountMinor > 0L) Color(0xFFFFB4AB) else Color.White,
                        fontSize = 11.sp
                    )
                    SummaryMetricRow(
                        label = if (isBangla) "অতিরিক্ত চার্জ :" else "Additional Charges :",
                        value = "(+) ৳" + Formatters.formatMinorUnit(totals.additionalChargesMinor),
                        labelColor = Color.White.copy(alpha = 0.85f),
                        valueColor = Color.White,
                        fontSize = 11.sp
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color.White.copy(alpha = 0.22f)
                    )

                    SummaryMetricRow(
                        label = if (isBangla) "সর্বমোট বিল :" else "Grand Total :",
                        value = "৳" + Formatters.formatMinorUnit(totals.grandTotalMinor),
                        labelColor = Color.White,
                        valueColor = Color(0xFF4ADE80),
                        fontSize = 12.5.sp,
                        isBold = true
                    )
                    SummaryMetricRow(
                        label = if (isBangla) "পরিশোধিত :" else "Paid Amount :",
                        value = "৳" + Formatters.formatMinorUnit(totals.paidMinor),
                        labelColor = Color.White.copy(alpha = 0.85f),
                        valueColor = Color.White,
                        fontSize = 11.sp
                    )
                    SummaryMetricRow(
                        label = if (isBangla) "বকেয়া (এই বিল) :" else "Due Amount :",
                        value = "৳" + Formatters.formatMinorUnit(totals.dueMinor),
                        labelColor = if (totals.dueMinor > 0L) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.85f),
                        valueColor = if (totals.dueMinor > 0L) Color(0xFFFF6B6B) else Color(0xFF4ADE80),
                        fontSize = 11.sp,
                        isBold = totals.dueMinor > 0L
                    )

                    if (showProfit && hasItems) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = Color.White.copy(alpha = 0.22f)
                        )
                        val profitSignColor = if (totals.estimatedProfitMinor >= 0L) Color.White else Color(0xFFFF8A8A)
                        val marginFormatted = String.format(Locale.US, "%.1f", totals.estimatedProfitMarginPercent)
                        val profitValue = "৳${Formatters.formatMinorUnit(totals.estimatedProfitMinor)} ($marginFormatted%)"
                        SummaryMetricRow(
                            label = if (isBangla) "আনুমানিক লাভ :" else "Estimated Profit :",
                            value = profitValue,
                            labelColor = Color.White,
                            valueColor = profitSignColor,
                            fontSize = if (profitValue.length > 14) 10.5.sp else 11.sp,
                            isBold = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricRow(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
    fontSize: TextUnit,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            color = labelColor,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 6.dp)
        )
        Text(
            text = value,
            fontSize = fontSize,
            color = valueColor,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
internal fun SalesInvoiceStickyFooter(
    isBangla: Boolean,
    palette: SalesInvoicePalette,
    onCancel: () -> Unit,
    onSaveDraft: () -> Unit,
    onConfirmSale: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.footerBg,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            HorizontalDivider(color = palette.cardBorder, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, palette.cancelBorder),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(0.92f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = palette.primaryPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBangla) "বাতিল" else "Cancel",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primaryPurple,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Button(
                    onClick = onSaveDraft,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.draftBg,
                        contentColor = palette.draftText
                    ),
                    border = BorderStroke(1.dp, palette.draftBorder),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1.05f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = null,
                        tint = palette.draftText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isBangla) "খসড়া" else "Save Draft",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onConfirmSale,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.confirmGreen,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1.28f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isBangla) "নিশ্চিত বিক্রয়" else "Confirm Sale",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun SalesProductVariantPickerDialog(
    products: List<Product>,
    variantsByProduct: Map<String, List<ProductVariant>>,
    stockBalances: List<StockBalance>,
    warehouseId: String,
    productImages: Map<String, String>,
    variantImages: Map<String, String>,
    isBangla: Boolean,
    palette: SalesInvoicePalette,
    onDismiss: () -> Unit,
    onSelectProductAndVariant: (product: Product, variant: ProductVariant?) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredProducts = remember(products, variantsByProduct, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) products
        else products.filter { prod ->
            val pVars = variantsByProduct[prod.id].orEmpty()
            prod.nameEn.contains(q, ignoreCase = true) ||
                prod.nameBn.contains(q, ignoreCase = true) ||
                prod.baseSku.contains(q, ignoreCase = true) ||
                prod.barcode?.contains(q, ignoreCase = true) == true ||
                pVars.any { v ->
                    v.sku.contains(q, ignoreCase = true) ||
                        v.color?.contains(q, ignoreCase = true) == true ||
                        v.size?.contains(q, ignoreCase = true) == true
                }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.84f)
                .clip(RoundedCornerShape(20.dp))
                .imePadding(),
            color = palette.sheetBg
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
                    Text(
                        text = if (isBangla) "স্টক থেকে পণ্য ও ভেরিয়েন্ট বাছুন" else "Pick In-Stock Product / Variant",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = palette.textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isBangla) "নাম, SKU বা বারকোড দিয়ে খুঁজুন..." else "Search by product, variant, SKU or barcode...",
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = palette.primaryPurple) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { prod ->
                        val prodVars = variantsByProduct[prod.id].orEmpty()
                        val prodStock = sellableStockForProduct(stockBalances, warehouseId, prod.id, prodVars)
                        val prodImage = productImages[prod.id]

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = palette.cardBg,
                            border = BorderStroke(1.dp, palette.cardBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = prodVars.isEmpty() && prodStock > 0) {
                                            onSelectProductAndVariant(prod, null)
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SalesProductThumbnail(
                                        imageUri = prodImage,
                                        contentDescription = prod.nameEn,
                                        size = 46.dp,
                                        cornerRadius = 10.dp,
                                        palette = palette
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBangla && prod.nameBn.isNotBlank()) prod.nameBn else prod.nameEn,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (prodStock > 0) palette.textPrimary else palette.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${prod.baseSku} • Price: ৳${Formatters.formatMinorUnit(prod.sellingPriceMinor)}",
                                            fontSize = 11.5.sp,
                                            color = palette.textSecondary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (prodStock > 0) palette.variantChipBg else palette.kpiDueBg
                                    ) {
                                        Text(
                                            text = if (prodStock > 0) {
                                                if (isBangla) "স্টক: $prodStock" else "Stock: $prodStock"
                                            } else {
                                                if (isBangla) "স্টক নেই" else "Out"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (prodStock > 0) palette.variantChipText else palette.errorText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (prodVars.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        prodVars.forEach { variant ->
                                            val varStock = sellableStockForVariant(stockBalances, warehouseId, prod.id, variant.id)
                                            val varInStock = varStock > 0
                                            val varLabel = listOfNotNull(variant.color, variant.size).joinToString("-").ifBlank { variant.sku }
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                onClick = { if (varInStock) onSelectProductAndVariant(prod, variant) },
                                                enabled = varInStock,
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (varInStock) palette.subtleSurface else palette.subtleSurface.copy(alpha = 0.4f),
                                                border = BorderStroke(1.dp, palette.cardBorder)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    SalesProductThumbnail(
                                                        imageUri = variantImages[variant.id] ?: prodImage,
                                                        contentDescription = varLabel,
                                                        size = 34.dp,
                                                        cornerRadius = 8.dp,
                                                        palette = palette
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = varLabel,
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (varInStock) palette.textPrimary else palette.textMuted
                                                        )
                                                        Text(
                                                            text = "${variant.sku} • ৳${Formatters.formatMinorUnit(variant.variantPriceMinor)}",
                                                            fontSize = 11.sp,
                                                            color = palette.textSecondary
                                                        )
                                                    }
                                                    Text(
                                                        text = if (varInStock) {
                                                            if (isBangla) "যোগ ($varStock)" else "+ Add ($varStock)"
                                                        } else {
                                                            if (isBangla) "স্টক নেই" else "Out"
                                                        },
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (varInStock) palette.primaryPurple else palette.textMuted
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
        }
    }
}
