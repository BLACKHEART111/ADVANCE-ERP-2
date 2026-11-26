@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.pocketdev.advance_erp.feature.contacts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocketdev.advance_erp.core.designsystem.AdvanceErpLogo
import com.pocketdev.advance_erp.core.designsystem.BrandConfig
import com.pocketdev.advance_erp.core.designsystem.DashboardColorTokens
import com.pocketdev.advance_erp.core.designsystem.LocalDashboardTokens
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.domain.model.PartyLedgerStatement
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CARD_ACCENT_COLORS = listOf(
    Color(0xFF7B2BB0),
    Color(0xFF14A38B),
    Color(0xFFE59718),
    Color(0xFF2B74D4)
)

fun contactsHeroStatusBarColor(isDark: Boolean): Color =
    if (isDark) Color(0xFF150926) else Color(0xFF3E0E60)

fun contactsPageBgColors(tokens: DashboardColorTokens): Pair<Color, Color> =
    if (tokens.isDark) {
        tokens.pageBgTop to tokens.pageBgBottom
    } else {
        Color(0xFFF4EFFA) to Color(0xFFEDE6F7)
    }

@Composable
fun ContactsHeroHeader(
    companyName: String,
    selectedTab: ContactsTab,
    language: AppLanguage,
    dueAlertCount: Int,
    onOpenDrawer: () -> Unit,
    onToggleLanguage: () -> Unit,
    onDueAlertsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val tokens = LocalDashboardTokens.current
    val gradientColors = if (tokens.isDark) {
        listOf(Color(0xFF150926), Color(0xFF2B114E), Color(0xFF471B74))
    } else {
        listOf(Color(0xFF3E0E60), Color(0xFF601988), Color(0xFF8C2CB8))
    }

    val headerTitle = when (selectedTab) {
        ContactsTab.CUSTOMERS -> if (isBn) "গ্রাহকবৃন্দ" else "Customers"
        ContactsTab.SUPPLIERS -> if (isBn) "সরবরাহকারীগণ" else "Suppliers"
    }
    val headerSubtitle = when (selectedTab) {
        ContactsTab.CUSTOMERS -> "Customer Management"
        ContactsTab.SUPPLIERS -> "Supplier Management"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(gradientColors))
            .statusBarsPadding()
    ) {
        // Glossy translucent right-side sweeping wave ribbons matching the reference image
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            val backWave = Path().apply {
                moveTo(w * 0.34f, h)
                cubicTo(w * 0.56f, h * 0.52f, w * 0.80f, h * 0.24f, w, h * 0.26f)
                lineTo(w, h)
                close()
            }
            drawPath(
                path = backWave,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFC24CEB).copy(alpha = if (tokens.isDark) 0.18f else 0.22f),
                        Color(0xFFF06FD8).copy(alpha = if (tokens.isDark) 0.24f else 0.32f)
                    )
                )
            )

            val frontWave = Path().apply {
                moveTo(w * 0.48f, h)
                cubicTo(w * 0.68f, h * 0.62f, w * 0.84f, h * 0.44f, w, h * 0.52f)
                lineTo(w, h)
                close()
            }
            drawPath(
                path = frontWave,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFA936DC).copy(alpha = if (tokens.isDark) 0.22f else 0.26f),
                        Color(0xFFFF7BD7).copy(alpha = if (tokens.isDark) 0.28f else 0.36f)
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 22.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }

                AdvanceErpLogo(
                    variant = BrandConfig.LogoVariant.SYMBOL,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = companyName,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    onClick = onToggleLanguage,
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.38f)),
                    modifier = Modifier.heightIn(min = 34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = "Language",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isBn) "EN" else "বাং",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onDueAlertsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = if (isBn) "বকেয়া সতর্কতা" else "Due Alerts",
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                    if (dueAlertCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 9.dp, end = 9.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3366))
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp)
            ) {
                Text(
                    text = headerTitle,
                    color = Color.White,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = headerSubtitle,
                    color = Color.White.copy(alpha = 0.84f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ContactsSummaryKpiRow(
    metrics: ContactsSummaryMetrics,
    selectedTab: ContactsTab,
    currency: CurrencyCode,
    language: AppLanguage,
    activeBalanceFilter: ContactBalanceFilter,
    activeRegFilter: ContactRegistrationFilter,
    onTotalCardClick: () -> Unit,
    onNewMonthCardClick: () -> Unit,
    onDueCardClick: () -> Unit,
    onSettledCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val tokens = LocalDashboardTokens.current
    val isCustomers = selectedTab == ContactsTab.CUSTOMERS

    val totalLabel = if (isCustomers) {
        if (isBn) "মোট গ্রাহক" else "Total"
    } else {
        if (isBn) "সরবরাহকারী" else "Suppliers"
    }
    val newMonthLabel = if (isBn) "নতুন (এই মাস)" else "New (Month)"
    val dueLabel = if (isCustomers) {
        if (isBn) "মোট বকেয়া" else "Total Due"
    } else {
        if (isBn) "মোট দেনা" else "Total Payable"
    }
    val settledLabel = if (isCustomers) {
        if (isBn) "পরিশোধিত" else "Received"
    } else {
        if (isBn) "পরিশোধিত" else "Total Paid"
    }

    val totalCountText = ContactsUiLogic.formatCount(metrics.totalCount, language)
    val newCountText = ContactsUiLogic.formatCount(metrics.newThisMonthCount, language)
    val dueText = ContactsUiLogic.formatCompactSummaryMoney(metrics.totalOutstandingMinor, currency, language)
    val settledText = ContactsUiLogic.formatCompactSummaryMoney(metrics.totalSettledPaymentsMinor, currency, language)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .testTag("contacts_summary_row"),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        SummaryPastelCard(
            icon = Icons.Outlined.Group,
            label = totalLabel,
            value = totalCountText,
            bgColors = if (tokens.isDark) listOf(Color(0xFF24173E), Color(0xFF1A132F)) else listOf(Color(0xFFF2E6FC), Color(0xFFFAF5FF)),
            borderColor = if (tokens.isDark) Color(0xFF4C3378) else Color(0xFFE3CCF9),
            iconTint = if (tokens.isDark) Color(0xFFD5A4E0) else Color(0xFF6B1E8F),
            valueColor = if (tokens.isDark) Color(0xFFE6C4FC) else Color(0xFF551178),
            labelColor = tokens.sectionSubtitleText,
            selected = false,
            onClick = onTotalCardClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        SummaryPastelCard(
            icon = Icons.Outlined.PersonAdd,
            label = newMonthLabel,
            value = newCountText,
            bgColors = if (tokens.isDark) listOf(Color(0xFF102A28), Color(0xFF111E24)) else listOf(Color(0xFFE3F7F0), Color(0xFFF4FCF9)),
            borderColor = if (tokens.isDark) Color(0xFF1E574D) else Color(0xFFC5ECE0),
            iconTint = if (tokens.isDark) Color(0xFF42E8B5) else Color(0xFF0E936C),
            valueColor = if (tokens.isDark) Color(0xFF8FF7D8) else Color(0xFF142420),
            labelColor = tokens.sectionSubtitleText,
            selected = activeRegFilter == ContactRegistrationFilter.THIS_MONTH,
            onClick = onNewMonthCardClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        SummaryPastelCard(
            icon = Icons.Outlined.AccountBalanceWallet,
            label = dueLabel,
            value = dueText,
            bgColors = if (tokens.isDark) listOf(Color(0xFF341621), Color(0xFF23131E)) else listOf(Color(0xFFFDE8EC), Color(0xFFFFF6F7)),
            borderColor = if (tokens.isDark) Color(0xFF6E293C) else Color(0xFFF8CCD4),
            iconTint = if (tokens.isDark) Color(0xFFFB7185) else Color(0xFFDC3E4E),
            valueColor = if (tokens.isDark) Color(0xFFFCA5A5) else Color(0xFFA9192B),
            labelColor = tokens.sectionSubtitleText,
            selected = activeBalanceFilter == ContactBalanceFilter.WITH_DUE,
            onClick = onDueCardClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        SummaryPastelCard(
            icon = Icons.Outlined.CheckCircle,
            label = settledLabel,
            value = settledText,
            bgColors = if (tokens.isDark) listOf(Color(0xFF112A1F), Color(0xFF12201E)) else listOf(Color(0xFFE4F6EB), Color(0xFFF4FCF7)),
            borderColor = if (tokens.isDark) Color(0xFF1F5D40) else Color(0xFFC5EAD2),
            iconTint = if (tokens.isDark) Color(0xFF34D399) else Color(0xFF1A9D58),
            valueColor = if (tokens.isDark) Color(0xFF6EE7B7) else Color(0xFF156E3D),
            labelColor = tokens.sectionSubtitleText,
            selected = activeBalanceFilter == ContactBalanceFilter.SETTLED,
            onClick = onSettledCardClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun SummaryPastelCard(
    icon: ImageVector,
    label: String,
    value: String,
    bgColors: List<Color>,
    borderColor: Color,
    iconTint: Color,
    valueColor: Color,
    labelColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalDashboardTokens.current
    val shape = RoundedCornerShape(14.dp)
    val valueFontSize = when {
        value.length >= 12 -> 10.5.sp
        value.length >= 9 -> 12.sp
        else -> 15.5.sp
    }

    Box(
        modifier = modifier
            .then(
                if (!tokens.isDark) {
                    Modifier.shadow(2.dp, shape, ambientColor = iconTint.copy(alpha = 0.16f))
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(Brush.verticalGradient(bgColors))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) iconTint.copy(alpha = 0.78f) else borderColor,
                shape = shape
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                fontSize = valueFontSize,
                lineHeight = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ContactsSegmentedTabBar(
    selectedTab: ContactsTab,
    language: AppLanguage,
    onSelectTab: (ContactsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val tokens = LocalDashboardTokens.current
    val shape = RoundedCornerShape(16.dp)
    val activeGradient = if (tokens.isDark) {
        Brush.horizontalGradient(listOf(Color(0xFF6B219E), Color(0xFFA039D0)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF5A1683), Color(0xFF9C2AB8)))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!tokens.isDark) {
                    Modifier.shadow(3.dp, shape, ambientColor = tokens.accentPurple.copy(alpha = 0.14f))
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = tokens.cardBgStart,
        border = BorderStroke(1.dp, tokens.cardBorderStart)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                Triple(ContactsTab.CUSTOMERS, Icons.Outlined.Group, if (isBn) "গ্রাহকবৃন্দ" else "Customers"),
                Triple(ContactsTab.SUPPLIERS, Icons.Outlined.Business, if (isBn) "সরবরাহকারীগণ" else "Suppliers")
            )
            tabs.forEach { (tab, icon, label) ->
                val selected = selectedTab == tab
                val pillShape = RoundedCornerShape(13.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .clip(pillShape)
                        .then(
                            if (selected) Modifier.background(activeGradient)
                            else Modifier.background(Color.Transparent)
                        )
                        .clickable(role = Role.Tab) { onSelectTab(tab) }
                        .padding(horizontal = 8.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) Color.White else tokens.accentPurple,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = label,
                            fontSize = 13.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (selected) Color.White else tokens.sectionTitleText,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsSearchFilterSortRow(
    query: String,
    activeFilterCount: Int,
    isSortActive: Boolean,
    language: AppLanguage,
    onQueryChange: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onOpenSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val tokens = LocalDashboardTokens.current
    val colors = MaterialTheme.colorScheme
    val searchShape = RoundedCornerShape(14.dp)
    val btnShape = RoundedCornerShape(13.dp)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .then(
                    if (!tokens.isDark) {
                        Modifier.shadow(2.dp, searchShape, ambientColor = tokens.accentPurple.copy(alpha = 0.08f))
                    } else {
                        Modifier
                    }
                ),
            shape = searchShape,
            color = tokens.dateCardBg,
            border = BorderStroke(1.dp, tokens.cardBorderStart)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = tokens.sectionSubtitleText,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = if (isBn) "নাম, কোড বা ফোন দিয়ে খুঁজুন..." else "Search name, code or phone...",
                            color = tokens.sectionSubtitleText.copy(alpha = 0.8f),
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = tokens.cardValueText,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(tokens.accentPurple),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contacts_search_input")
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear",
                            tint = tokens.sectionSubtitleText,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        // Filter icon button
        Box {
            Surface(
                onClick = onOpenFilter,
                shape = btnShape,
                color = when {
                    activeFilterCount > 0 && tokens.isDark -> Color(0xFF281D4B)
                    activeFilterCount > 0 -> colors.primaryContainer
                    else -> tokens.dateCardBg
                },
                border = BorderStroke(
                    1.dp,
                    if (activeFilterCount > 0) tokens.accentPurple else tokens.cardBorderStart
                ),
                shadowElevation = if (tokens.isDark) 0.dp else 2.dp,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("contacts_filter_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.FilterAlt,
                        contentDescription = if (isBn) "ফিল্টার" else "Filter",
                        tint = tokens.accentPurple,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            if (activeFilterCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(tokens.accentPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Formatters.formatNumber(activeFilterCount.toLong(), language),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Sort icon button
        Surface(
            onClick = onOpenSort,
            shape = btnShape,
            color = when {
                isSortActive && tokens.isDark -> Color(0xFF281D4B)
                isSortActive -> colors.primaryContainer
                else -> tokens.dateCardBg
            },
            border = BorderStroke(
                1.dp,
                if (isSortActive) tokens.accentPurple else tokens.cardBorderStart
            ),
            shadowElevation = if (tokens.isDark) 0.dp else 2.dp,
            modifier = Modifier
                .size(46.dp)
                .testTag("contacts_sort_btn")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.SwapVert,
                    contentDescription = if (isBn) "সাজান" else "Sort",
                    tint = tokens.accentPurple,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
fun ActiveContactsFilterChipsRow(
    filterState: ContactsFilterState,
    sortOption: ContactSortOption,
    filteredCount: Int,
    totalCount: Int,
    language: AppLanguage,
    onRemoveBalance: () -> Unit,
    onRemoveLocation: () -> Unit,
    onRemoveRegistration: () -> Unit,
    onResetSort: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val tokens = LocalDashboardTokens.current
    val hasActive = filterState.activeCount > 0 || sortOption != ContactSortOption.NAME_ASC
    if (!hasActive) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (filterState.balanceFilter != ContactBalanceFilter.ALL) {
                RemovableFilterChip(
                    label = balanceFilterLabel(filterState.balanceFilter, isBn),
                    tokens = tokens,
                    onRemove = onRemoveBalance
                )
            }
            filterState.selectedLocation?.takeIf { it.isNotBlank() }?.let { loc ->
                RemovableFilterChip(
                    label = loc,
                    tokens = tokens,
                    onRemove = onRemoveLocation
                )
            }
            if (filterState.registrationFilter != ContactRegistrationFilter.ALL) {
                RemovableFilterChip(
                    label = registrationFilterLabel(filterState.registrationFilter, isBn),
                    tokens = tokens,
                    onRemove = onRemoveRegistration
                )
            }
            if (sortOption != ContactSortOption.NAME_ASC) {
                RemovableFilterChip(
                    label = sortOptionLabel(sortOption, isBn),
                    tokens = tokens,
                    onRemove = onResetSort
                )
            }
            Surface(
                onClick = onClearAll,
                shape = RoundedCornerShape(50),
                color = StatusError.copy(alpha = 0.10f),
                border = BorderStroke(0.8.dp, StatusError.copy(alpha = 0.35f)),
                modifier = Modifier.heightIn(min = 30.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBn) "সব রিসেট" else "Reset",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusError
                    )
                }
            }
        }
        Text(
            text = if (isBn) {
                "${ContactsUiLogic.formatCount(filteredCount, language)} / ${ContactsUiLogic.formatCount(totalCount, language)} জন দেখানো হচ্ছে"
            } else {
                "Showing $filteredCount of $totalCount"
            },
            fontSize = 11.sp,
            color = tokens.sectionSubtitleText
        )
    }
}

@Composable
private fun RemovableFilterChip(
    label: String,
    tokens: DashboardColorTokens,
    onRemove: () -> Unit
) {
    Surface(
        onClick = onRemove,
        shape = RoundedCornerShape(50),
        color = tokens.accentPurple.copy(alpha = if (tokens.isDark) 0.24f else 0.11f),
        border = BorderStroke(0.8.dp, tokens.accentPurple.copy(alpha = 0.45f)),
        modifier = Modifier.heightIn(min = 30.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = tokens.accentPurple,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove",
                tint = tokens.accentPurple,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
fun CustomerContactCard(
    customer: Customer,
    index: Int,
    currency: CurrencyCode,
    language: AppLanguage,
    onCardClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onDialPhone: (String) -> Unit,
    onDueReminderClick: () -> Unit,
    onRecordPaymentClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val tokens = LocalDashboardTokens.current
    val context = LocalContext.current
    val accent = CARD_ACCENT_COLORS[index % CARD_ACCENT_COLORS.size]
    val shape = RoundedCornerShape(16.dp)
    val hasDue = customer.outstandingBalanceMinor > 0L
    val isAdvance = customer.outstandingBalanceMinor < 0L
    val balanceColor = when {
        hasDue -> StatusError
        isAdvance -> tokens.accentTeal
        else -> StatusSuccess
    }
    val balanceSubtitle = when {
        hasDue -> if (isBn) "বকেয়া ব্যালেন্স" else "Due Balance"
        isAdvance -> if (isBn) "অগ্রিম ক্রেডিট" else "Advance Credit"
        else -> if (isBn) "পরিশোধিত" else "Settled"
    }

    val cleanAddress = customer.address.trim()
    val cleanPhone = customer.phone.trim()
    val putAddressOnSeparateRow = cleanAddress.isNotEmpty() &&
        (cleanAddress.length > 16 || (cleanPhone.length + cleanAddress.length) > 23)

    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!tokens.isDark) {
                    Modifier.shadow(3.dp, shape, ambientColor = tokens.accentPurple.copy(alpha = 0.11f))
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(tokens.cardBgStart, tokens.cardBgEnd)))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(tokens.cardBorderStart, tokens.cardBorderEnd)),
                shape = shape
            )
            .clickable(role = Role.Button, onClick = onCardClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
        ) {
            // Left 4dp color accent stripe matching the reference image
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 11.dp, end = 10.dp, top = 11.dp, bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar (saved photo or pastel initial)
                    val photoPath = customer.profileImagePath
                    val hasPhoto = !photoPath.isNullOrBlank() && File(photoPath).exists()
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = if (tokens.isDark) 0.24f else 0.13f))
                            .border(0.8.dp, accent.copy(alpha = 0.25f), CircleShape)
                            .clickable(role = Role.Button, onClick = onAvatarClick),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasPhoto) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(photoPath!!))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = customer.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = customer.name.trim().take(1).uppercase(Locale.US).ifBlank { "C" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tokens.isDark) Color.White else accent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = customer.name,
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.cardValueText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (tokens.isDark) Color(0xFF1F2442) else Color(0xFFF3EEFA)
                            ) {
                                Text(
                                    text = "${if (isBn) "কোড:" else "Code:"} ${customer.customerCode}",
                                    fontSize = 10.5.sp,
                                    color = tokens.sectionSubtitleText,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.HowToReg,
                                contentDescription = null,
                                tint = tokens.accentPurple,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = Formatters.formatMoney(customer.outstandingBalanceMinor, currency, language),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = balanceColor,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = balanceSubtitle,
                            fontSize = 10.5.sp,
                            color = tokens.sectionSubtitleText,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = if (isBn) "আরও অপশন" else "More options",
                                tint = tokens.sectionSubtitleText,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isBn) "লেজার বিবরণী" else "Customer Ledger") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ReceiptLong, null) },
                                onClick = { menuExpanded = false; onCardClick() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isBn) "পেমেন্ট গ্রহণ" else "Record Payment") },
                                leadingIcon = { Icon(Icons.Outlined.Payments, null) },
                                onClick = { menuExpanded = false; onRecordPaymentClick() }
                            )
                            if (hasDue) {
                                DropdownMenuItem(
                                    text = { Text(if (isBn) "বকেয়া তাগাদা পাঠান" else "Send Due Reminder") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Send, null) },
                                    onClick = { menuExpanded = false; onDueReminderClick() }
                                )
                            }
                            if (cleanPhone.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text(if (isBn) "কল করুন ($cleanPhone)" else "Call ($cleanPhone)") },
                                    leadingIcon = { Icon(Icons.Outlined.Call, null) },
                                    onClick = { menuExpanded = false; onDialPhone(cleanPhone) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (isBn) "প্রোফাইল ছবি পরিবর্তন" else "Update Photo") },
                                leadingIcon = { Icon(Icons.Outlined.CameraAlt, null) },
                                onClick = { menuExpanded = false; onAvatarClick() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isBn) "তথ্য এডিট করুন" else "Edit Details") },
                                leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                onClick = { menuExpanded = false; onEditClick() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isBn) "মুছুন / আর্কাইভ" else "Delete / Archive", color = StatusError) },
                                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = StatusError) },
                                onClick = { menuExpanded = false; onDeleteClick() }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(thickness = 0.8.dp, color = tokens.dividerColor)
                Spacer(modifier = Modifier.height(6.dp))

                if (putAddressOnSeparateRow) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = tokens.sectionSubtitleText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = cleanAddress,
                            fontSize = 11.5.sp,
                            color = tokens.sectionSubtitleText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = cleanPhone.isNotBlank()) { onDialPhone(cleanPhone) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Call,
                                contentDescription = null,
                                tint = tokens.accentPurple,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = cleanPhone.ifBlank { "-" },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = tokens.cardValueText,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (!putAddressOnSeparateRow && cleanAddress.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = tokens.sectionSubtitleText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = cleanAddress,
                                fontSize = 11.5.sp,
                                color = tokens.sectionSubtitleText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (hasDue) {
                            GradientPillButton(
                                icon = Icons.AutoMirrored.Outlined.Send,
                                label = if (isBn) "বকেয়া তাগাদা" else "Remind",
                                gradient = Brush.horizontalGradient(listOf(Color(0xFF0A8F4D), Color(0xFF17B567))),
                                onClick = onDueReminderClick
                            )
                        } else {
                            GradientPillButton(
                                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                                label = if (isBn) "লেজার" else "Ledger",
                                gradient = Brush.horizontalGradient(listOf(Color(0xFF0E7C8B), Color(0xFF15A194))),
                                onClick = onCardClick
                            )
                        }

                        SquareIconActionButton(
                            icon = Icons.Outlined.Edit,
                            contentDescription = if (isBn) "এডিট" else "Edit",
                            bgColor = if (tokens.isDark) Color(0xFF241B40) else Color(0xFFF3ECFB),
                            iconTint = tokens.accentPurple,
                            onClick = onEditClick
                        )

                        SquareIconActionButton(
                            icon = Icons.Outlined.DeleteOutline,
                            contentDescription = if (isBn) "মুছুন" else "Delete",
                            bgColor = if (tokens.isDark) Color(0xFF351923) else Color(0xFFFDECEF),
                            iconTint = StatusError,
                            onClick = onDeleteClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SupplierContactCard(
    supplier: Supplier,
    index: Int,
    currency: CurrencyCode,
    language: AppLanguage,
    onCardClick: () -> Unit,
    onDialPhone: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val tokens = LocalDashboardTokens.current
    val accent = CARD_ACCENT_COLORS[(index + 1) % CARD_ACCENT_COLORS.size]
    val shape = RoundedCornerShape(16.dp)
    val hasPayable = supplier.outstandingPayableMinor > 0L
    val payableColor = if (hasPayable) StatusError else StatusSuccess
    val payableLabel = if (hasPayable) {
        if (isBn) "প্রদেয় বকেয়া" else "Payable Due"
    } else {
        if (isBn) "পরিশোধিত" else "Settled"
    }
    val cleanPhone = supplier.phone.trim()
    val cleanAddress = supplier.address.trim()
    val cleanPerson = supplier.contactPerson?.trim().orEmpty()
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!tokens.isDark) {
                    Modifier.shadow(3.dp, shape, ambientColor = tokens.accentPurple.copy(alpha = 0.11f))
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(tokens.cardBgStart, tokens.cardBgEnd)))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(tokens.cardBorderStart, tokens.cardBorderEnd)),
                shape = shape
            )
            .clickable(role = Role.Button, onClick = onCardClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 11.dp, end = 10.dp, top = 11.dp, bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = if (tokens.isDark) 0.24f else 0.13f))
                            .border(0.8.dp, accent.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = supplier.companyName.trim().take(1).uppercase(Locale.US).ifBlank { "S" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tokens.isDark) Color.White else accent
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = supplier.companyName,
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.cardValueText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (tokens.isDark) Color(0xFF1F2442) else Color(0xFFF3EEFA)
                            ) {
                                Text(
                                    text = supplier.supplierCode,
                                    fontSize = 10.5.sp,
                                    color = tokens.sectionSubtitleText,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = tokens.accentTeal.copy(alpha = if (tokens.isDark) 0.24f else 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Public,
                                        contentDescription = null,
                                        tint = tokens.accentTeal,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "${supplier.country} (${supplier.currency})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tokens.accentTeal,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = Formatters.formatMoney(supplier.outstandingPayableMinor, currency, language),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = payableColor,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = payableLabel,
                            fontSize = 10.5.sp,
                            color = tokens.sectionSubtitleText,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = if (isBn) "আরও অপশন" else "More options",
                                tint = tokens.sectionSubtitleText,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isBn) "সরবরাহকারী লেজার" else "Supplier Ledger") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ReceiptLong, null) },
                                onClick = { menuExpanded = false; onCardClick() }
                            )
                            if (cleanPhone.isNotBlank() && !cleanPhone.equals("N/A", ignoreCase = true)) {
                                DropdownMenuItem(
                                    text = { Text(if (isBn) "কল করুন ($cleanPhone)" else "Call ($cleanPhone)") },
                                    leadingIcon = { Icon(Icons.Outlined.Call, null) },
                                    onClick = { menuExpanded = false; onDialPhone(cleanPhone) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (isBn) "তথ্য এডিট করুন" else "Edit Details") },
                                leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                onClick = { menuExpanded = false; onEditClick() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isBn) "মুছুন / আর্কাইভ" else "Delete / Archive", color = StatusError) },
                                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = StatusError) },
                                onClick = { menuExpanded = false; onDeleteClick() }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(thickness = 0.8.dp, color = tokens.dividerColor)
                Spacer(modifier = Modifier.height(6.dp))

                if (cleanAddress.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = tokens.sectionSubtitleText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = cleanAddress,
                            fontSize = 11.5.sp,
                            color = tokens.sectionSubtitleText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (cleanPerson.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = tokens.accentTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = cleanPerson,
                            fontSize = 11.5.sp,
                            color = tokens.sectionSubtitleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val canDial = cleanPhone.isNotBlank() && !cleanPhone.equals("N/A", ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = canDial) { onDialPhone(cleanPhone) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Call,
                            contentDescription = null,
                            tint = if (canDial) tokens.accentPurple else tokens.sectionSubtitleText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (canDial) cleanPhone else (if (isBn) "নম্বর নেই" else "No phone"),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (canDial) tokens.cardValueText else tokens.sectionSubtitleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        GradientPillButton(
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            label = if (isBn) "লেজার" else "Ledger",
                            gradient = Brush.horizontalGradient(listOf(Color(0xFF0E7C8B), Color(0xFF15A194))),
                            onClick = onCardClick
                        )
                        SquareIconActionButton(
                            icon = Icons.Outlined.Edit,
                            contentDescription = if (isBn) "এডিট" else "Edit",
                            bgColor = if (tokens.isDark) Color(0xFF241B40) else Color(0xFFF3ECFB),
                            iconTint = tokens.accentPurple,
                            onClick = onEditClick
                        )
                        SquareIconActionButton(
                            icon = Icons.Outlined.DeleteOutline,
                            contentDescription = if (isBn) "মুছুন" else "Delete",
                            bgColor = if (tokens.isDark) Color(0xFF351923) else Color(0xFFFDECEF),
                            iconTint = StatusError,
                            onClick = onDeleteClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GradientPillButton(
    icon: ImageVector,
    label: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(shape)
            .background(gradient)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun SquareIconActionButton(
    icon: ImageVector,
    contentDescription: String,
    bgColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(shape)
            .background(bgColor)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun ContactsAddFab(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalDashboardTokens.current
    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(
                elevation = if (tokens.isDark) 12.dp else 8.dp,
                shape = CircleShape,
                ambientColor = tokens.accentPurple,
                spotColor = tokens.accentPurple
            )
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(tokens.fabBgStart, tokens.fabBgEnd)))
            .border(1.2.dp, Color.White.copy(alpha = 0.28f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("contacts_add_fab"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.PersonAdd,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(25.dp)
        )
    }
}

@Composable
fun ContactsFilterBottomSheet(
    currentFilter: ContactsFilterState,
    availableLocations: List<String>,
    selectedTab: ContactsTab,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onApply: (ContactsFilterState) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val tokens = LocalDashboardTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draftBalance by remember(currentFilter) { mutableStateOf(currentFilter.balanceFilter) }
    var draftLocation by remember(currentFilter) { mutableStateOf(currentFilter.selectedLocation) }
    var draftRegistration by remember(currentFilter) { mutableStateOf(currentFilter.registrationFilter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.cardBgStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBn) "উন্নত ফিল্টার" else "Advanced Filters",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = tokens.sectionTitleText
                )
                TextButton(
                    onClick = {
                        onApply(ContactsFilterState())
                        onDismiss()
                    }
                ) {
                    Text(if (isBn) "সব রিসেট" else "Reset All", color = StatusError, fontWeight = FontWeight.SemiBold)
                }
            }

            Text(
                text = if (selectedTab == ContactsTab.CUSTOMERS) {
                    if (isBn) "বকেয়া ও ব্যালেন্স অবস্থা" else "Balance & Due Status"
                } else {
                    if (isBn) "প্রদেয় বকেয়া অবস্থা" else "Payable Balance Status"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = tokens.sectionTitleText
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContactBalanceFilter.entries.forEach { opt ->
                    SheetSelectChip(
                        label = balanceFilterLabel(opt, isBn),
                        selected = draftBalance == opt,
                        tokens = tokens,
                        onClick = { draftBalance = opt }
                    )
                }
            }

            if (availableLocations.isNotEmpty()) {
                HorizontalDivider(color = tokens.dividerColor)
                Text(
                    text = if (selectedTab == ContactsTab.CUSTOMERS) {
                        if (isBn) "অবস্থান / এলাকা" else "Location / Area"
                    } else {
                        if (isBn) "দেশ / অবস্থান" else "Country / Location"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = tokens.sectionTitleText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SheetSelectChip(
                        label = if (isBn) "সব এলাকা" else "All Locations",
                        selected = draftLocation == null,
                        tokens = tokens,
                        onClick = { draftLocation = null }
                    )
                    availableLocations.forEach { loc ->
                        SheetSelectChip(
                            label = loc,
                            selected = draftLocation.equals(loc, ignoreCase = true),
                            tokens = tokens,
                            onClick = {
                                draftLocation = if (draftLocation.equals(loc, ignoreCase = true)) null else loc
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = tokens.dividerColor)
            Text(
                text = if (isBn) "নিবন্ধনের সময়" else "Registration Date",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = tokens.sectionTitleText
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContactRegistrationFilter.entries.forEach { reg ->
                    SheetSelectChip(
                        label = registrationFilterLabel(reg, isBn),
                        selected = draftRegistration == reg,
                        tokens = tokens,
                        onClick = { draftRegistration = reg }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(tokens.heroBgStart, tokens.accentPurple)))
                        .clickable(role = Role.Button) {
                            onApply(
                                ContactsFilterState(
                                    balanceFilter = draftBalance,
                                    selectedLocation = draftLocation,
                                    registrationFilter = draftRegistration
                                )
                            )
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBn) "ফিল্টার প্রয়োগ করুন" else "Apply Filters",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ContactsSortBottomSheet(
    currentSort: ContactSortOption,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSelectSort: (ContactSortOption) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val tokens = LocalDashboardTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.cardBgStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isBn) "তালিকা সাজান" else "Sort Contacts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = tokens.sectionTitleText
            )
            ContactSortOption.entries.forEach { opt ->
                val selected = opt == currentSort
                Surface(
                    onClick = {
                        onSelectSort(opt)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) tokens.accentPurple.copy(alpha = if (tokens.isDark) 0.25f else 0.11f) else Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (selected) tokens.accentPurple else tokens.dividerColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sortOptionLabel(opt, isBn),
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) tokens.accentPurple else tokens.sectionTitleText
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = tokens.accentPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsDueAlertsSheet(
    selectedTab: ContactsTab,
    dueCustomers: List<Customer>,
    dueSuppliers: List<Supplier>,
    currency: CurrencyCode,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onRemindCustomer: (Customer) -> Unit,
    onOpenSupplierLedger: (Supplier) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val tokens = LocalDashboardTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.cardBgStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (selectedTab == ContactsTab.CUSTOMERS) {
                    if (isBn) "বকেয়া তাগাদা সতর্কতা" else "Customer Due Alerts"
                } else {
                    if (isBn) "প্রদেয় বকেয়া সতর্কতা" else "Supplier Payable Alerts"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = tokens.sectionTitleText
            )

            if (selectedTab == ContactsTab.CUSTOMERS) {
                if (dueCustomers.isEmpty()) {
                    Text(
                        text = if (isBn) "কোনো গ্রাহকের বকেয়া নেই।" else "No pending customer dues.",
                        color = tokens.sectionSubtitleText
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(dueCustomers, key = { it.id }) { cust ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (tokens.isDark) Color(0xFF1A1F3C) else Color(0xFFF7F3FC),
                                border = BorderStroke(0.8.dp, tokens.cardBorderStart)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cust.name, fontWeight = FontWeight.Bold, color = tokens.cardValueText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${cust.customerCode} · ${cust.phone}", fontSize = 11.5.sp, color = tokens.sectionSubtitleText, maxLines = 1)
                                    }
                                    Text(
                                        text = Formatters.formatMoney(cust.outstandingBalanceMinor, currency, language),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StatusError,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    GradientPillButton(
                                        icon = Icons.AutoMirrored.Outlined.Send,
                                        label = if (isBn) "তাগাদা" else "Remind",
                                        gradient = Brush.horizontalGradient(listOf(Color(0xFF0A8F4D), Color(0xFF17B567))),
                                        onClick = { onDismiss(); onRemindCustomer(cust) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                if (dueSuppliers.isEmpty()) {
                    Text(
                        text = if (isBn) "কোনো সরবরাহকারীর বকেয়া নেই।" else "No pending supplier payables.",
                        color = tokens.sectionSubtitleText
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(dueSuppliers, key = { it.id }) { sup ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (tokens.isDark) Color(0xFF1A1F3C) else Color(0xFFF7F3FC),
                                border = BorderStroke(0.8.dp, tokens.cardBorderStart),
                                modifier = Modifier.clickable { onDismiss(); onOpenSupplierLedger(sup) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sup.companyName, fontWeight = FontWeight.Bold, color = tokens.cardValueText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${sup.supplierCode} · ${sup.country}", fontSize = 11.5.sp, color = tokens.sectionSubtitleText, maxLines = 1)
                                    }
                                    Text(
                                        text = Formatters.formatMoney(sup.outstandingPayableMinor, currency, language),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StatusError,
                                        maxLines = 1,
                                        softWrap = false
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
fun SupplierLedgerDialog(
    supplier: Supplier,
    statement: PartyLedgerStatement,
    currency: CurrencyCode,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val tokens = LocalDashboardTokens.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .widthIn(max = 620.dp)
                .clip(RoundedCornerShape(18.dp)),
            color = tokens.pageBgTop
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBn) "সরবরাহকারী লেজার বিবরণী" else "Supplier Ledger Statement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tokens.sectionTitleText
                        )
                        Text(
                            text = "${supplier.companyName} · ${supplier.supplierCode} · ${supplier.country} (${supplier.currency})",
                            fontSize = 12.sp,
                            color = tokens.sectionSubtitleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = tokens.sectionSubtitleText)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(tokens.overviewBgTop, tokens.overviewBgMid)))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(if (isBn) "মোট বিল (Credit)" else "Total Billed", fontSize = 11.sp, color = Color.White.copy(alpha = 0.78f))
                            Text(
                                text = Formatters.formatMoney(statement.totalCreditMinor, currency, language),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isBn) "পরিশোধ (Debit)" else "Total Paid", fontSize = 11.sp, color = Color.White.copy(alpha = 0.78f))
                            Text(
                                text = Formatters.formatMoney(statement.totalDebitMinor, currency, language),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF42F5AA)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (isBn) "প্রদেয় বকেয়া" else "Closing Payable", fontSize = 11.sp, color = Color.White.copy(alpha = 0.78f))
                            Text(
                                text = Formatters.formatMoney(statement.closingBalanceMinor, currency, language),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (statement.closingBalanceMinor > 0L) Color(0xFFFF9DB0) else Color(0xFF42F5AA)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (statement.transactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBn) "এই সরবরাহকারীর কোনো লেনদেন পাওয়া যায়নি।" else "No supplier ledger transactions found.",
                                    color = tokens.sectionSubtitleText
                                )
                            }
                        }
                    } else {
                        items(statement.transactions, key = { "${it.rawType}_${it.id}" }) { row ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = tokens.cardBgStart,
                                border = BorderStroke(0.8.dp, tokens.cardBorderStart)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${row.referenceNumber} · ${row.status}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = tokens.cardValueText
                                        )
                                        Text(
                                            text = "${dateFormat.format(Date(row.date))} · ${row.particulars}",
                                            fontSize = 11.sp,
                                            color = tokens.sectionSubtitleText,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = Formatters.formatMoney(row.amountMinor, currency, language),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (row.isInflow) tokens.accentTeal else tokens.accentPurple,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        row.runningBalanceMinor?.let { bal ->
                                            Text(
                                                text = "${if (isBn) "জের:" else "Bal:"} ${Formatters.formatMoney(bal, currency, language)}",
                                                fontSize = 10.5.sp,
                                                color = tokens.sectionSubtitleText,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GradientPillButton(
                        icon = Icons.AutoMirrored.Outlined.Send,
                        label = if (isBn) "হোয়াটসঅ্যাপে শেয়ার" else "Share Statement",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFF0A8F4D), Color(0xFF17B567))),
                        onClick = onShareWhatsApp
                    )
                    TextButton(onClick = onDismiss) {
                        Text(if (isBn) "বন্ধ করুন" else "Close", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetSelectChip(
    label: String,
    selected: Boolean,
    tokens: DashboardColorTokens,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = when {
            selected && tokens.isDark -> Color(0xFF281D4B)
            selected -> colors.primaryContainer
            else -> tokens.cardBgEnd
        },
        border = BorderStroke(
            1.dp,
            if (selected) tokens.accentPurple else tokens.cardBorderStart
        ),
        modifier = Modifier.heightIn(min = 42.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) tokens.accentPurple else tokens.sectionTitleText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun balanceFilterLabel(opt: ContactBalanceFilter, isBn: Boolean): String = when (opt) {
    ContactBalanceFilter.ALL -> if (isBn) "সবাই" else "All"
    ContactBalanceFilter.WITH_DUE -> if (isBn) "বকেয়া আছে" else "With Due"
    ContactBalanceFilter.SETTLED -> if (isBn) "পরিশোধিত (০)" else "Settled (0)"
    ContactBalanceFilter.ADVANCE_CREDIT -> if (isBn) "অগ্রিম ক্রেডিট" else "Advance Credit"
}

private fun registrationFilterLabel(opt: ContactRegistrationFilter, isBn: Boolean): String = when (opt) {
    ContactRegistrationFilter.ALL -> if (isBn) "সব সময়" else "All Dates"
    ContactRegistrationFilter.THIS_MONTH -> if (isBn) "এই মাস" else "This Month"
    ContactRegistrationFilter.LAST_30_DAYS -> if (isBn) "গত ৩০ দিন" else "Last 30 Days"
    ContactRegistrationFilter.OLDER -> if (isBn) "পুরনো (৩০+ দিন)" else "Older (30d+)"
}

private fun sortOptionLabel(opt: ContactSortOption, isBn: Boolean): String = when (opt) {
    ContactSortOption.NAME_ASC -> if (isBn) "নাম (A → Z)" else "Name (A → Z)"
    ContactSortOption.NAME_DESC -> if (isBn) "নাম (Z → A)" else "Name (Z → A)"
    ContactSortOption.BALANCE_DESC -> if (isBn) "বকেয়া (সর্বোচ্চ → সর্বনিম্ন)" else "Balance (High → Low)"
    ContactSortOption.BALANCE_ASC -> if (isBn) "বকেয়া (সর্বনিম্ন → সর্বোচ্চ)" else "Balance (Low → High)"
    ContactSortOption.LOCATION_ASC -> if (isBn) "অবস্থান অনুযায়ী (A → Z)" else "Location (A → Z)"
    ContactSortOption.CREATED_DESC -> if (isBn) "নতুন নিবন্ধন প্রথমে" else "Newest Registered"
    ContactSortOption.CREATED_ASC -> if (isBn) "পুরনো নিবন্ধন প্রথমে" else "Oldest Registered"
}
