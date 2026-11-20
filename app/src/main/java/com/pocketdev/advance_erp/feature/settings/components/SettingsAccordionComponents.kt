package com.pocketdev.advance_erp.feature.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.advance_erp.core.designsystem.SettingsColorTokens
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode

enum class SettingsCategory {
    APPEARANCE_LANGUAGE,
    BUSINESS_PROFILE,
    SECURITY_LOCK,
    DATA_BACKUP,
    SYSTEM_INFO
}

internal fun toggleCategoryExpanded(
    expandedSet: Set<SettingsCategory>,
    category: SettingsCategory
): Set<SettingsCategory> {
    return if (category in expandedSet) expandedSet - category else expandedSet + category
}

internal fun validateBusinessProfileName(companyName: String): Boolean {
    return companyName.trim().isNotEmpty()
}

internal fun requiresCurrencyChangeConfirmation(
    current: CurrencyCode,
    target: CurrencyCode
): Boolean {
    return current != target
}

@Composable
fun PremiumSettingsHeader(
    language: AppLanguage,
    tokens: SettingsColorTokens,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 84.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(tokens.headerBgStart, tokens.headerBgEnd)
                )
            )
    ) {
        // Right-side layered purple + teal wave matching reference images
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            val purpleWave = Path().apply {
                moveTo(w * 0.42f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.88f)
                cubicTo(
                    w * 0.82f, h * 0.60f,
                    w * 0.65f, h * 0.25f,
                    w * 0.42f, 0f
                )
                close()
            }
            drawPath(
                path = purpleWave,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        tokens.headerWavePurple.copy(alpha = if (tokens.isDark) 0.42f else 0.72f),
                        tokens.headerWavePurple.copy(alpha = if (tokens.isDark) 0.68f else 0.85f)
                    )
                )
            )

            val tealWave = Path().apply {
                moveTo(w * 0.48f, h)
                cubicTo(
                    w * 0.68f, h * 0.68f,
                    w * 0.85f, h * 0.62f,
                    w, h * 0.42f
                )
                lineTo(w, h)
                close()
            }
            drawPath(
                path = tealWave,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        tokens.headerWaveTeal.copy(alpha = if (tokens.isDark) 0.16f else 0.35f),
                        tokens.headerWaveTeal.copy(alpha = if (tokens.isDark) 0.56f else 0.78f)
                    )
                )
            )
        }

        // Soft decorative Gear watermark tucked on the right
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = null,
            tint = tokens.headerGearWatermark,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-10).dp, y = 2.dp)
                .size(66.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(
                        elevation = if (tokens.isDark) 4.dp else 6.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = tokens.headerBadgeStart.copy(alpha = 0.4f),
                        spotColor = tokens.headerBadgeEnd.copy(alpha = 0.45f)
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(tokens.headerBadgeStart, tokens.headerBadgeEnd)
                        )
                    )
                    .then(
                        if (tokens.isDark) {
                            Modifier.border(
                                width = 1.dp,
                                color = Color(0x80CE93D8),
                                shape = RoundedCornerShape(14.dp)
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBn) "সেটিংস" else "Settings",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = tokens.headerTitle
                )
                Text(
                    text = if (isBn) "অ্যাপ ও ব্যবসার কনফিগারেশন" else "App & business configuration",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = tokens.headerSubtitle
                )
            }
        }
    }
}

@Composable
fun SettingsAccordionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    tokens: SettingsColorTokens,
    testTag: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "accordionChevronRotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!tokens.isDark) {
                    Modifier.shadow(
                        elevation = 3.dp,
                        shape = shape,
                        ambientColor = tokens.headerWavePurple.copy(alpha = 0.14f),
                        spotColor = tokens.headerWaveTeal.copy(alpha = 0.16f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(if (expanded) tokens.cardExpandedBg else tokens.cardBg)
            .border(
                width = if (tokens.isDark) 1.dp else 0.9.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(tokens.cardBorderStart, tokens.cardBorderEnd)
                ),
                shape = shape
            )
            .animateContentSize(animationSpec = tween(durationMillis = 220))
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Accordion Header Row with subtle wave decoration when collapsed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    val bottomWave = Path().apply {
                        moveTo(0f, h * 0.78f)
                        cubicTo(
                            w * 0.35f, h * 0.98f,
                            w * 0.72f, h * 0.62f,
                            w, h * 0.74f
                        )
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = bottomWave,
                        color = tokens.cardBottomWave.copy(alpha = if (tokens.isDark) 0.45f else 0.65f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 68.dp)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rounded Icon Container
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(tokens.iconContainerStart, tokens.iconContainerEnd)
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = tokens.iconContainerBorder,
                                shape = RoundedCornerShape(13.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tokens.iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = tokens.titleText
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = tokens.subtitleText
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = tokens.chevronTint,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(chevronRotation)
                    )
                }
            }

            // Expandable Body
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = tokens.dividerColor, thickness = 0.9.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        content = content
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeSegmentedSelector(
    selectedMode: String,
    language: AppLanguage,
    tokens: SettingsColorTokens,
    onSelectMode: (String) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val options = listOf(
        Triple("system", if (isBn) "সিস্টেম" else "System", Icons.Outlined.DesktopWindows),
        Triple("light", if (isBn) "লাইট" else "Light", Icons.Outlined.LightMode),
        Triple("dark", if (isBn) "ডার্ক" else "Dark", Icons.Outlined.DarkMode)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (mode, label, icon) ->
            val isSelected = selectedMode == mode
            val shape = RoundedCornerShape(12.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(shape)
                    .background(
                        brush = if (isSelected) {
                            Brush.horizontalGradient(
                                colors = listOf(tokens.themeSelectedBgStart, tokens.themeSelectedBgEnd)
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(tokens.themeBtnBg, tokens.themeBtnBg)
                            )
                        }
                    )
                    .border(
                        width = if (isSelected) 1.3.dp else 1.dp,
                        color = if (isSelected) tokens.themeSelectedBorder else tokens.themeBtnBorder,
                        shape = shape
                    )
                    .clickable { onSelectMode(mode) }
                    .padding(horizontal = 8.dp, vertical = 10.dp)
                    .testTag("theme_option_$mode"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) tokens.themeSelectedContent else tokens.themeBtnText,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) tokens.themeSelectedContent else tokens.themeBtnText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumLanguageRow(
    languageOption: AppLanguage,
    selected: Boolean,
    tokens: SettingsColorTokens,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val bgBrush = if (selected) {
        Brush.horizontalGradient(listOf(tokens.langSelectedBgStart, tokens.langSelectedBgEnd))
    } else {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(brush = bgBrush)
            .then(
                if (selected) {
                    Modifier.border(1.dp, tokens.langSelectedBorder, shape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp)
            .testTag("lang_option_${languageOption.code}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            PremiumRadioIndicator(
                selected = selected,
                selectedColor = if (tokens.isDark) Color.White else tokens.radioSelectedColor,
                unselectedColor = tokens.radioUnselectedColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = languageOption.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (selected) tokens.langSelectedText else tokens.titleText
            )
        }

        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = tokens.langCheckTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PremiumCurrencyRow(
    currency: CurrencyCode,
    displayLanguage: AppLanguage,
    selected: Boolean,
    tokens: SettingsColorTokens,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val bgBrush = if (selected) {
        Brush.horizontalGradient(listOf(tokens.currencySelectedBgStart, tokens.currencySelectedBgEnd))
    } else {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    val currencyLabel = buildString {
        append(currency.code)
        append(" (")
        append(currency.symbol)
        append(") — ")
        append(if (displayLanguage == AppLanguage.BANGLA) currency.displayNameBn else currency.displayNameEn)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(brush = bgBrush)
            .then(
                if (selected) {
                    Modifier.border(1.dp, tokens.currencySelectedBorder, shape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp)
            .testTag("currency_option_${currency.code}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PremiumRadioIndicator(
            selected = selected,
            selectedColor = if (tokens.isDark) Color.White else tokens.radioSelectedColor,
            unselectedColor = tokens.radioUnselectedColor
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = currencyLabel,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 14.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (selected) tokens.currencySelectedText else tokens.titleText
        )
    }
}

@Composable
private fun PremiumRadioIndicator(
    selected: Boolean,
    selectedColor: Color,
    unselectedColor: Color
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .border(
                width = 1.8.dp,
                color = if (selected) selectedColor else unselectedColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(selectedColor)
            )
        }
    }
}
