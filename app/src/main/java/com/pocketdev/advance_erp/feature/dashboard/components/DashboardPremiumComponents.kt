package com.pocketdev.advance_erp.feature.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.DashboardColorTokens
import com.pocketdev.advance_erp.core.designsystem.ERPCalendar
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.domain.repository.DashboardTransaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DashboardSection {
    OVERVIEW,
    DAILY_SUMMARY,
    DAILY_EXTRA_METRICS,
    CLOSING_POSITION
}

data class DashboardCollapseState(
    val overviewExpanded: Boolean = true,
    val dailySummaryExpanded: Boolean = true,
    val dailyExtraExpanded: Boolean = false,
    val closingPositionExpanded: Boolean = false
)

fun DashboardCollapseState.toggle(section: DashboardSection): DashboardCollapseState = when (section) {
    DashboardSection.OVERVIEW -> copy(overviewExpanded = !overviewExpanded)
    DashboardSection.DAILY_SUMMARY -> copy(dailySummaryExpanded = !dailySummaryExpanded)
    DashboardSection.DAILY_EXTRA_METRICS -> copy(dailyExtraExpanded = !dailyExtraExpanded)
    DashboardSection.CLOSING_POSITION -> copy(closingPositionExpanded = !closingPositionExpanded)
}

private val BANGLA_MONTHS_FULL = listOf(
    "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
    "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
)

private val BANGLA_MONTHS_SHORT = listOf(
    "জানু", "ফেব্রু", "মার্চ", "এপ্রি", "মে", "জুন",
    "জুলা", "আগ", "সেপ্টে", "অক্টো", "নভে", "ডিসে"
)

fun formatDashboardDate(
    millis: Long,
    language: AppLanguage,
    shortMonth: Boolean = false
): String {
    if (language == AppLanguage.ENGLISH) {
        val pattern = if (shortMonth) "dd MMM yyyy" else "dd MMMM yyyy"
        return SimpleDateFormat(pattern, Locale.US).format(Date(millis))
    }
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    val day = Formatters.toBanglaDigits(cal.get(Calendar.DAY_OF_MONTH).toString())
    val monthIdx = cal.get(Calendar.MONTH).coerceIn(0, 11)
    val month = if (shortMonth) BANGLA_MONTHS_SHORT[monthIdx] else BANGLA_MONTHS_FULL[monthIdx]
    val year = Formatters.toBanglaDigits(cal.get(Calendar.YEAR).toString())
    return "$day $month $year"
}

@Composable
fun PremiumDashboardHeader(
    companyName: String,
    language: AppLanguage,
    tokens: DashboardColorTokens,
    modifier: Modifier = Modifier
) {
    val isBangla = language == AppLanguage.BANGLA
    val bannerHeight = if (tokens.isDark) 104.dp else 116.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(tokens.heroBgStart, tokens.heroBgMid, tokens.heroBgEnd)
                )
            )
    ) {
        // Right-aligned local shipping port illustration with smooth horizontal fade
        Image(
            painter = painterResource(
                id = if (tokens.isDark) R.drawable.bg_drawer_port_dark else R.drawable.bg_drawer_port_light
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomEnd,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (tokens.isDark) 0.90f else 0.88f)
        )

        // Left-side protective gradient scrim fading smoothly into the port artwork
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to tokens.heroBgStart,
                        0.36f to tokens.heroBgMid.copy(alpha = 0.96f),
                        0.60f to tokens.heroBgMid.copy(alpha = 0.45f),
                        0.82f to tokens.heroBgEnd.copy(alpha = 0.10f),
                        1.0f to Color.Transparent
                    )
                )
        )

        // Decorative sweeping Purple -> Teal curved wave
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val fillWave = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.86f)
                cubicTo(
                    w * 0.24f, h * 0.98f,
                    w * 0.56f, h * 0.62f,
                    w, h * 0.74f
                )
                lineTo(w, h)
                close()
            }
            drawPath(
                path = fillWave,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        tokens.heroWaveStart.copy(alpha = if (tokens.isDark) 0.42f else 0.34f),
                        tokens.heroWaveEnd.copy(alpha = if (tokens.isDark) 0.48f else 0.42f)
                    )
                )
            )

            val glowStroke = Path().apply {
                moveTo(0f, h * 0.87f)
                cubicTo(
                    w * 0.26f, h * 0.99f,
                    w * 0.58f, h * 0.63f,
                    w, h * 0.74f
                )
            }
            drawPath(
                path = glowStroke,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        tokens.heroWaveStart.copy(alpha = if (tokens.isDark) 0.85f else 0.6f),
                        tokens.heroWaveEnd.copy(alpha = if (tokens.isDark) 0.95f else 0.75f)
                    )
                ),
                style = Stroke(width = if (tokens.isDark) 3.5f else 2.5f, cap = StrokeCap.Round)
            )
        }

        if (tokens.isDark) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(8.dp, CircleShape, ambientColor = tokens.accentPurple, spotColor = tokens.accentPurple)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(tokens.overviewIconBgStart, tokens.overviewIconBgEnd)
                            )
                        )
                        .border(1.2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GridView,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(0.68f)) {
                    Text(
                        text = if (isBangla) "ড্যাশবোর্ড" else "Dashboard",
                        color = tokens.heroTitleColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = companyName,
                        color = tokens.heroSubtitleColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isBangla) "স্বাগতম! 👋" else "Welcome! 👋",
                    color = tokens.heroTitleColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = companyName,
                    color = tokens.heroTitleColor,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isBangla) "আপনার ব্যবসার সারাংশ এক নজরে" else "Your business overview at a glance",
                    color = tokens.heroSubtitleColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CompactDateSelectorCard(
    formattedDate: String,
    language: AppLanguage,
    selectedDateMillis: Long,
    showCalendarPicker: Boolean,
    tokens: DashboardColorTokens,
    onSelectPreviousDay: () -> Unit,
    onSelectNextDay: () -> Unit,
    onSelectToday: () -> Unit,
    onToggleCalendarPicker: () -> Unit,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    endDateMillis: Long = selectedDateMillis,
    isCustomRange: Boolean = false,
    onDateRangeSelected: (Long, Long) -> Unit = { _, _ -> }
) {
    val isBangla = language == AppLanguage.BANGLA
    val shape = RoundedCornerShape(16.dp)
    val borderBrush = Brush.horizontalGradient(
        listOf(tokens.dateCardBorderStart, tokens.dateCardBorderEnd)
    )
    var rangeMode by rememberSaveable(isCustomRange) { mutableStateOf(isCustomRange) }
    var draftStartMillis by rememberSaveable(selectedDateMillis, showCalendarPicker) { mutableStateOf(selectedDateMillis) }
    var draftEndMillis by rememberSaveable(endDateMillis, showCalendarPicker) { mutableStateOf(endDateMillis) }
    var pickingEndDate by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!tokens.isDark) {
                    Modifier.shadow(4.dp, shape, ambientColor = tokens.accentPurple.copy(alpha = 0.14f))
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = tokens.dateCardBg,
        border = BorderStroke(1.dp, borderBrush)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onSelectPreviousDay,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Prev Day",
                            tint = tokens.dateArrowTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onToggleCalendarPicker)
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isCustomRange) Icons.Outlined.DateRange else Icons.Outlined.CalendarMonth,
                            contentDescription = "Calendar",
                            tint = tokens.dateArrowTint,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isCustomRange && isBangla -> "রেঞ্জ: $formattedDate"
                                isCustomRange -> "Range: $formattedDate"
                                isBangla -> "তারিখ: $formattedDate"
                                else -> "Date: $formattedDate"
                            },
                            color = tokens.dateTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onSelectNextDay,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Day",
                            tint = tokens.dateArrowTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    onClick = {
                        rangeMode = true
                        if (!showCalendarPicker) onToggleCalendarPicker()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCustomRange) tokens.accentPurple.copy(alpha = 0.14f) else tokens.todayBtnBg,
                    border = BorderStroke(1.dp, if (isCustomRange) tokens.accentPurple else tokens.todayBtnBorder),
                    modifier = Modifier.heightIn(min = 34.dp).testTag("dashboard_range_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DateRange,
                            contentDescription = null,
                            tint = if (isCustomRange) tokens.accentPurple else tokens.todayBtnContent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isBangla) "রেঞ্জ" else "Range",
                            color = if (isCustomRange) tokens.accentPurple else tokens.todayBtnContent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    onClick = onSelectToday,
                    shape = RoundedCornerShape(12.dp),
                    color = tokens.todayBtnBg,
                    border = BorderStroke(1.dp, tokens.todayBtnBorder),
                    modifier = Modifier.heightIn(min = 34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Today,
                            contentDescription = null,
                            tint = tokens.todayBtnContent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isBangla) "আজ" else "Today",
                            color = tokens.todayBtnContent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showCalendarPicker,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = tokens.dividerColor)

                    // Mode toggle: Single Day vs Custom Date Range
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { rangeMode = false },
                            shape = RoundedCornerShape(10.dp),
                            color = if (!rangeMode) tokens.accentPurple.copy(alpha = 0.14f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (!rangeMode) tokens.accentPurple else tokens.dividerColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isBangla) "নির্দিষ্ট দিন" else "Single Day",
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!rangeMode) tokens.accentPurple else tokens.dateTextColor
                            )
                        }
                        Surface(
                            onClick = { rangeMode = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (rangeMode) tokens.accentPurple.copy(alpha = 0.14f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (rangeMode) tokens.accentPurple else tokens.dividerColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isBangla) "কাস্টম তারিখ রেঞ্জ" else "Custom Date Range",
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (rangeMode) tokens.accentPurple else tokens.dateTextColor
                            )
                        }
                    }

                    if (!rangeMode) {
                        ERPCalendar(
                            language = language,
                            selectedTimestamp = selectedDateMillis,
                            onDateSelected = onDateSelected
                        )
                    } else {
                        // Quick range presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                (if (isBangla) "গত ৭ দিন" else "Last 7 Days") to 6,
                                (if (isBangla) "গত ১৫ দিন" else "Last 15 Days") to 14,
                                (if (isBangla) "গত ৩০ দিন" else "Last 30 Days") to 29
                            ).forEach { (label, daysBack) ->
                                Surface(
                                    onClick = {
                                        val endNow = System.currentTimeMillis()
                                        val start = Calendar.getInstance().apply {
                                            timeInMillis = endNow
                                            add(Calendar.DAY_OF_YEAR, -daysBack)
                                        }.timeInMillis
                                        onDateRangeSelected(start, endNow)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = tokens.todayBtnBg,
                                    border = BorderStroke(1.dp, tokens.todayBtnBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tokens.todayBtnContent,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Start & End selector pills
                        val startFmt = rememberFormattedDashboardDate(draftStartMillis, language)
                        val endFmt = rememberFormattedDashboardDate(draftEndMillis, language)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { pickingEndDate = false },
                                shape = RoundedCornerShape(10.dp),
                                color = if (!pickingEndDate) tokens.accentTeal.copy(alpha = 0.14f) else Color.Transparent,
                                border = BorderStroke(1.2.dp, if (!pickingEndDate) tokens.accentTeal else tokens.dividerColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(
                                        text = if (isBangla) "শুরুর তারিখ (From)" else "Start Date",
                                        fontSize = 10.sp,
                                        color = if (!pickingEndDate) tokens.accentTeal else tokens.dateTextColor.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = startFmt,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tokens.dateTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Surface(
                                onClick = { pickingEndDate = true },
                                shape = RoundedCornerShape(10.dp),
                                color = if (pickingEndDate) tokens.accentPurple.copy(alpha = 0.14f) else Color.Transparent,
                                border = BorderStroke(1.2.dp, if (pickingEndDate) tokens.accentPurple else tokens.dividerColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(
                                        text = if (isBangla) "শেষের তারিখ (To)" else "End Date",
                                        fontSize = 10.sp,
                                        color = if (pickingEndDate) tokens.accentPurple else tokens.dateTextColor.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = endFmt,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tokens.dateTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        key(pickingEndDate) {
                            ERPCalendar(
                                language = language,
                                selectedTimestamp = if (pickingEndDate) draftEndMillis else draftStartMillis,
                                onDateSelected = { picked ->
                                    if (!pickingEndDate) {
                                        draftStartMillis = picked
                                        if (draftEndMillis < picked) draftEndMillis = picked
                                        pickingEndDate = true
                                    } else {
                                        draftEndMillis = picked
                                        if (draftStartMillis > picked) draftStartMillis = picked
                                    }
                                }
                            )
                        }

                        Button(
                            onClick = { onDateRangeSelected(draftStartMillis, draftEndMillis) },
                            colors = ButtonDefaults.buttonColors(containerColor = tokens.accentPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("dashboard_apply_range_btn")
                        ) {
                            Text(
                                text = if (isBangla) "নির্বাচিত রেঞ্জ প্রয়োগ করুন ($startFmt – $endFmt)" else "Apply Range ($startFmt – $endFmt)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollapsibleBusinessOverviewCard(
    productCount: Int,
    lowStockCount: Int,
    customerCount: Int,
    supplierCount: Int,
    isLiveData: Boolean,
    language: AppLanguage,
    expanded: Boolean,
    tokens: DashboardColorTokens,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBangla = language == AppLanguage.BANGLA
    val shape = RoundedCornerShape(20.dp)
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "overview_chevron"
    )

    val productText = Formatters.formatNumber(productCount.toLong(), language)
    val lowStockText = Formatters.formatNumber(lowStockCount.toLong(), language)
    val customerText = Formatters.formatNumber(customerCount.toLong(), language)
    val supplierText = Formatters.formatNumber(supplierCount.toLong(), language)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (tokens.isDark) 8.dp else 6.dp,
                shape = shape,
                ambientColor = tokens.overviewBorderEnd.copy(alpha = 0.25f),
                spotColor = tokens.overviewBorderStart.copy(alpha = 0.3f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(tokens.overviewBgTop, tokens.overviewBgMid, tokens.overviewBgBottom)
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    listOf(tokens.overviewBorderStart, tokens.overviewBorderEnd)
                ),
                shape = shape
            )
    ) {
        // Subtle lower-edge teal glow accent
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.78f to Color.Transparent,
                    1.0f to tokens.overviewBorderEnd.copy(alpha = if (tokens.isDark) 0.22f else 0.16f)
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggleExpand)
                    .padding(vertical = 2.dp)
                    .testTag("dashboard_overview_header"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(tokens.overviewIconBgStart, tokens.overviewIconBgEnd)
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Layers,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "ব্যবসায়িক পর্যালোচনা" else "Business Overview",
                            color = Color.White,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isLiveData) {
                                if (isBangla) "ডাটাবেজের হালনাগাদ তথ্য" else "Live database metrics"
                            } else {
                                if (isBangla) "সংরক্ষিত ডাটাবেজ তথ্য" else "Saved database metrics"
                            },
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = tokens.liveBadgeBg,
                        border = BorderStroke(1.dp, tokens.liveBadgeBorder.copy(alpha = 0.65f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(tokens.liveBadgeContent, CircleShape)
                            )
                            Text(
                                text = if (isLiveData) {
                                    if (isBangla) "হালনাগাদ" else "LIVE"
                                } else {
                                    if (isBangla) "সংরক্ষিত" else "SAVED"
                                },
                                color = tokens.liveBadgeContent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(chevronRotation)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.Top
                    ) {
                        OverviewIndicatorColumn(
                            icon = Icons.Outlined.Inventory2,
                            value = productText,
                            label = if (isBangla) "পণ্যসমূহ" else "Products",
                            iconTint = tokens.overviewMetricIcon,
                            valueColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        OverviewVerticalDivider(tokens.overviewDivider)
                        OverviewIndicatorColumn(
                            icon = Icons.Outlined.Category,
                            value = lowStockText,
                            label = if (isBangla) "স্বল্প মজুদের পণ্য" else "Low Stock Items",
                            iconTint = if (lowStockCount > 0) tokens.accentRed else tokens.overviewMetricIcon,
                            valueColor = if (lowStockCount > 0) tokens.accentRed else Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        OverviewVerticalDivider(tokens.overviewDivider)
                        OverviewIndicatorColumn(
                            icon = Icons.Outlined.Groups,
                            value = customerText,
                            label = if (isBangla) "গ্রাহকবৃন্দ" else "Customers",
                            iconTint = tokens.overviewMetricIcon,
                            valueColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        OverviewVerticalDivider(tokens.overviewDivider)
                        OverviewIndicatorColumn(
                            icon = Icons.Outlined.Storefront,
                            value = supplierText,
                            label = if (isBangla) "সরবরাহকারীগণ" else "Suppliers",
                            iconTint = tokens.overviewMetricIcon,
                            valueColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Text(
                        text = if (isBangla) {
                            "পণ্য: $productText • স্বল্প মজুদ: $lowStockText • গ্রাহক: $customerText • সরবরাহকারী: $supplierText"
                        } else {
                            "Products: $productText • Low: $lowStockText • Customers: $customerText • Suppliers: $supplierText"
                        },
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewVerticalDivider(color: Color) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(52.dp)
            .background(color)
    )
}

@Composable
private fun OverviewIndicatorColumn(
    icon: ImageVector,
    value: String,
    label: String,
    iconTint: Color,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.84f),
            fontSize = 10.5.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CollapsibleSectionHeader(
    icon: ImageVector,
    title: String,
    tokens: DashboardColorTokens,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    expanded: Boolean? = null,
    onToggleExpand: (() -> Unit)? = null,
    collapsedHint: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    headerTestTag: String? = null
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded == true) 180f else 0f,
        label = "section_chevron"
    )
    val clickableModifier = when {
        onToggleExpand != null -> Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggleExpand)
        onActionClick != null -> Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onActionClick)
        else -> Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (headerTestTag != null) Modifier.testTag(headerTestTag) else Modifier)
            .then(clickableModifier)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.sectionIconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = tokens.sectionTitleText,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = tokens.sectionSubtitleText,
                        fontSize = 11.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (expanded != null && onToggleExpand != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = if (tokens.isDark) Color(0xFF1D203D) else Color(0xFFF0E8F9),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (tokens.isDark) Color(0xFF3E4575) else Color(0xFFE0D1F2)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (!expanded && !collapsedHint.isNullOrBlank()) {
                        Text(
                            text = collapsedHint,
                            color = tokens.sectionIconTint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = tokens.sectionIconTint,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(rotation)
                    )
                }
            }
        } else if (!actionText.isNullOrBlank() && onActionClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    color = tokens.sectionIconTint,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = tokens.sectionIconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun PremiumDashboardStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    badgeText: String,
    tokens: DashboardColorTokens,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null
) {
    val shape = RoundedCornerShape(18.dp)
    val borderBrush = if (tokens.isDark) {
        Brush.horizontalGradient(listOf(tokens.cardBorderStart, accentColor.copy(alpha = 0.72f)))
    } else {
        Brush.horizontalGradient(listOf(tokens.cardBorderStart, tokens.cardBorderEnd))
    }

    Box(
        modifier = modifier
            .then(
                if (!tokens.isDark) {
                    Modifier.shadow(3.dp, shape, ambientColor = accentColor.copy(alpha = 0.14f))
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(tokens.cardBgStart, tokens.cardBgEnd)
                )
            )
            .border(1.dp, borderBrush, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        // Subtle corner decorative wave matching both references
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val wavePath = Path().apply {
                moveTo(w * 0.18f, h)
                cubicTo(
                    w * 0.45f, h * 0.92f,
                    w * 0.68f, h * 0.56f,
                    w, h * 0.66f
                )
                lineTo(w, h)
                close()
            }
            drawPath(
                path = wavePath,
                color = accentColor.copy(alpha = if (tokens.isDark) 0.17f else 0.12f)
            )
            if (tokens.isDark) {
                val glowCenter = Offset(w * 0.22f, h * 0.26f)
                val glowRadius = w * 0.44f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.22f),
                            accentColor.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = glowCenter,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = glowCenter
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = if (tokens.isDark) 0.24f else 0.14f))
                        .border(
                            width = if (tokens.isDark) 1.dp else 0.dp,
                            color = if (tokens.isDark) accentColor.copy(alpha = 0.55f) else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (tokens.isDark) Color.White else accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = accentColor.copy(alpha = if (tokens.isDark) 0.20f else 0.12f),
                    border = if (tokens.isDark) BorderStroke(0.8.dp, accentColor.copy(alpha = 0.55f)) else null
                ) {
                    Text(
                        text = badgeText,
                        color = if (tokens.isDark) accentColor.copy(alpha = 0.98f) else accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                color = tokens.cardTitleText,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = value,
                    color = tokens.cardValueText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandMoreMetricsButton(
    expanded: Boolean,
    extraCount: Int,
    language: AppLanguage,
    tokens: DashboardColorTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBangla = language == AppLanguage.BANGLA
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "extra_metrics_chevron"
    )
    val countText = Formatters.formatNumber(extraCount.toLong(), language)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (tokens.isDark) Color(0xFF1A1C38) else Color(0xFFF4ECFB),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(tokens.cardBorderStart, tokens.accentPurple.copy(alpha = 0.55f))
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_daily_extra_toggle")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (expanded) {
                    if (isBangla) "অতিরিক্ত সূচক লুকান" else "Hide additional metrics"
                } else {
                    if (isBangla) "আরও ${countText}টি আর্থিক সূচক দেখুন" else "Show $extraCount more financial metrics"
                },
                color = tokens.sectionIconTint,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = tokens.sectionIconTint,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(rotation)
            )
        }
    }
}

@Composable
fun StockAndTransactionStatsBar(
    stockInUnits: Int,
    stockOutUnits: Int,
    transactionCount: Int,
    language: AppLanguage,
    tokens: DashboardColorTokens,
    modifier: Modifier = Modifier
) {
    val isBangla = language == AppLanguage.BANGLA
    val shape = RoundedCornerShape(16.dp)
    val borderBrush = Brush.horizontalGradient(
        listOf(tokens.cardBorderStart, tokens.cardBorderEnd)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!tokens.isDark) {
                    Modifier.shadow(2.dp, shape, ambientColor = tokens.accentPurple.copy(alpha = 0.1f))
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(tokens.cardBgStart, tokens.cardBgEnd)))
            .border(1.dp, borderBrush, shape)
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StockStatColumn(
                icon = Icons.Outlined.Inventory2,
                iconTint = if (tokens.isDark) Color(0xFFD8D3EA) else tokens.sectionSubtitleText,
                label = if (isBangla) "স্টক ইন" else "Stock In",
                value = "+$stockInUnits units",
                valueColor = tokens.accentGreen,
                labelColor = tokens.sectionSubtitleText,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(tokens.dividerColor)
            )
            StockStatColumn(
                icon = Icons.Outlined.ArrowUpward,
                iconTint = if (tokens.isDark) Color(0xFFD8D3EA) else tokens.sectionSubtitleText,
                label = if (isBangla) "স্টক আউট" else "Stock Out",
                value = "-$stockOutUnits units",
                valueColor = tokens.accentRed,
                labelColor = tokens.sectionSubtitleText,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(tokens.dividerColor)
            )
            StockStatColumn(
                icon = Icons.Outlined.EventAvailable,
                iconTint = if (tokens.isDark) Color(0xFFD8D3EA) else tokens.sectionSubtitleText,
                label = if (isBangla) "লেনদেন সংখ্যা" else "Transactions",
                value = "$transactionCount events",
                valueColor = tokens.cardValueText,
                labelColor = tokens.sectionSubtitleText,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StockStatColumn(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    valueColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = labelColor,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DashboardPdfShortcutsRow(
    isBangla: Boolean,
    tokens: DashboardColorTokens,
    onGenerateDailySnapshotPdf: () -> Unit,
    onGenerateMonthlyPerformancePdf: () -> Unit,
    modifier: Modifier = Modifier,
    isCustomRange: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PdfShortcutPillButton(
            label = if (isCustomRange) {
                if (isBangla) "নির্বাচিত সময়ের পিডিএফ" else "Range PDF Snapshot"
            } else {
                if (isBangla) "দৈনিক পিডিএফ চিত্র" else "Daily PDF Snapshot"
            },
            iconTint = if (tokens.isDark) tokens.accentPurple else tokens.accentTeal,
            tokens = tokens,
            onClick = onGenerateDailySnapshotPdf,
            modifier = Modifier.weight(1f)
        )
        PdfShortcutPillButton(
            label = if (isBangla) "মাসিক রিপোর্ট" else "Monthly Review",
            iconTint = tokens.reportBtnIcon,
            tokens = tokens,
            onClick = onGenerateMonthlyPerformancePdf,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PdfShortcutPillButton(
    label: String,
    iconTint: Color,
    tokens: DashboardColorTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val borderBrush = Brush.horizontalGradient(
        listOf(tokens.reportBtnBorderStart, tokens.reportBtnBorderEnd)
    )

    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .then(
                if (!tokens.isDark) {
                    Modifier.shadow(2.dp, shape, ambientColor = tokens.accentPurple.copy(alpha = 0.1f))
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(tokens.reportBtnBgStart, tokens.reportBtnBgEnd)))
            .border(1.dp, borderBrush, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.PictureAsPdf,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = tokens.reportBtnText,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = tokens.reportBtnIcon,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun DashboardEmptyTransactionsCard(
    isBangla: Boolean,
    tokens: DashboardColorTokens,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val borderBrush = Brush.horizontalGradient(
        listOf(tokens.cardBorderStart, tokens.cardBorderEnd)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!tokens.isDark) {
                    Modifier.shadow(3.dp, shape, ambientColor = tokens.accentPurple.copy(alpha = 0.12f))
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(tokens.emptyCardBgStart, tokens.emptyCardBgEnd)))
            .border(1.dp, borderBrush, shape)
    ) {
        // Bottom sweeping waves matching both Light & Dark reference empty-state cards
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            val leftWave = Path().apply {
                moveTo(0f, h * 0.78f)
                cubicTo(w * 0.25f, h * 0.94f, w * 0.55f, h * 0.72f, w, h * 0.90f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = leftWave,
                brush = Brush.horizontalGradient(
                    listOf(
                        tokens.emptyWaveLeft.copy(alpha = if (tokens.isDark) 0.42f else 0.75f),
                        tokens.emptyWaveRight.copy(alpha = if (tokens.isDark) 0.45f else 0.75f)
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .then(
                        if (tokens.isDark) {
                            Modifier.shadow(
                                14.dp,
                                CircleShape,
                                ambientColor = tokens.emptyOrbBorder,
                                spotColor = tokens.emptyOrbBorder
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(tokens.emptyOrbStart, tokens.emptyOrbEnd)))
                    .border(1.4.dp, tokens.emptyOrbBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = tokens.emptyOrbIcon,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isBangla) "এই তারিখে কোনো লেনদেন নেই" else "No transactions on this date",
                color = tokens.sectionTitleText,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isBangla) {
                    "নির্বাচিত তারিখে কোনো বিক্রয় বা স্টক স্থানান্তর রেকর্ড করা হয়নি।"
                } else {
                    "No sales or stock movements recorded on the selected date."
                },
                color = tokens.sectionSubtitleText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DashboardTransactionsCard(
    transactions: List<DashboardTransaction>,
    language: AppLanguage,
    tokens: DashboardColorTokens,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val borderBrush = Brush.horizontalGradient(
        listOf(tokens.cardBorderStart, tokens.cardBorderEnd)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!tokens.isDark) {
                    Modifier.shadow(3.dp, shape, ambientColor = tokens.accentPurple.copy(alpha = 0.1f))
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(tokens.cardBgStart, tokens.cardBgEnd)))
            .border(1.dp, borderBrush, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column {
            transactions.forEachIndexed { index, tx ->
                val itemColor = if (tx.isCredit) tokens.accentGreen else tokens.accentPurple
                val amountText = Formatters.formatMoney(tx.amountMinor, tx.currency, language)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(itemColor.copy(alpha = if (tokens.isDark) 0.22f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (tx.isCredit) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                            contentDescription = null,
                            tint = if (tokens.isDark) Color.White else itemColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tx.title,
                            color = tokens.cardValueText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${tx.subtitle} • ${Formatters.formatDate(tx.timestamp, language)}",
                            color = tokens.sectionSubtitleText,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = (if (tx.isCredit) "+" else "") + amountText,
                            color = if (tx.isCredit) tokens.accentGreen else tokens.cardValueText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (tx.statusText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = CircleShape,
                                color = itemColor.copy(alpha = if (tokens.isDark) 0.2f else 0.12f)
                            ) {
                                Text(
                                    text = tx.statusText,
                                    color = itemColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                if (index < transactions.lastIndex) {
                    HorizontalDivider(color = tokens.dividerColor)
                }
            }
        }
    }
}

@Composable
fun rememberFormattedDashboardDate(millis: Long, language: AppLanguage): String =
    remember(millis, language) {
        formatDashboardDate(
            millis = millis,
            language = language,
            shortMonth = (language == AppLanguage.ENGLISH)
        )
    }
