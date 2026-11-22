package com.pocketdev.advance_erp.feature.imports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.unit.sp
import com.pocketdev.advance_erp.core.designsystem.LocalIsDarkTheme

private val PoPurpleLight = Color(0xFF7D288C)
private val PoTealLight = Color(0xFF087D88)
private val PoNavyDark = Color(0xFF101A2D)
private val PoPurpleDark = Color(0xFF54216D)
private val PoTealDark = Color(0xFF0A5361)

@Composable
internal fun PremiumPoHero(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    icon: ImageVector = Icons.Outlined.ShoppingCart,
    compact: Boolean = false,
    onClose: (() -> Unit)? = null
) {
    val dark = LocalIsDarkTheme.current
    val background = if (dark) {
        Brush.linearGradient(listOf(PoNavyDark, PoPurpleDark, PoTealDark))
    } else {
        Brush.linearGradient(listOf(PoPurpleLight, Color(0xFF76268C), PoTealLight))
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, if (dark) Color(0x665C9CB0) else Color(0x33FFFFFF),)
    ) {
        Box(
            Modifier
                .background(background)
                .padding(horizontal = if (compact) 12.dp else 15.dp, vertical = if (compact) 7.dp else 12.dp)
        ) {
            // Quiet layered highlights echo the reference's purple/teal waves without bitmap assets.
            if (!compact) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .size(136.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (dark) 0.045f else 0.09f))
                )
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .width(180.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(topStart = 100.dp))
                        .background(Color(0xFF2AD0CC).copy(alpha = if (dark) 0.20f else 0.28f))
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(if (compact) 13.dp else 18.dp),
                    color = Color.White.copy(alpha = if (dark) 0.12f else 0.19f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                    modifier = Modifier.padding(if (compact) 9.dp else 12.dp).size(if (compact) 20.dp else 24.dp)
                    )
                }
                Spacer(Modifier.width(if (compact) 10.dp else 14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = Color.White,
                        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(if (compact) 1.dp else 3.dp))
                    Text(
                        subtitle,
                        color = Color.White.copy(alpha = 0.88f),
                        style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(4.dp))
                PurchaseCubeArtwork(Modifier.size(width = if (compact) 34.dp else 48.dp, height = if (compact) 38.dp else 52.dp))
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF101A2D).copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(
                            badge,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
                if (onClose != null) {
                    IconButton(onClick = onClose, modifier = Modifier.size(if (compact) 42.dp else 48.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseCubeArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        fun cube(centerX: Float, centerY: Float, half: Float, color: Color) {
            val top = Path().apply {
                moveTo(centerX, centerY - half)
                lineTo(centerX + half, centerY - half / 2)
                lineTo(centerX, centerY)
                lineTo(centerX - half, centerY - half / 2)
                close()
            }
            val left = Path().apply {
                moveTo(centerX - half, centerY - half / 2)
                lineTo(centerX, centerY)
                lineTo(centerX, centerY + half)
                lineTo(centerX - half, centerY + half / 2)
                close()
            }
            val right = Path().apply {
                moveTo(centerX, centerY)
                lineTo(centerX + half, centerY - half / 2)
                lineTo(centerX + half, centerY + half / 2)
                lineTo(centerX, centerY + half)
                close()
            }
            drawPath(left, color.copy(alpha = 0.74f))
            drawPath(right, color.copy(alpha = 0.94f))
            drawPath(top, color.copy(alpha = 0.9f))
            drawPath(left, Color.White.copy(alpha = 0.58f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx()))
            drawPath(right, Color.White.copy(alpha = 0.58f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx()))
            drawPath(top, Color.White.copy(alpha = 0.72f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx()))
        }
        cube(size.width * 0.56f, size.height * 0.36f, size.minDimension * 0.22f, Color(0xFFCF5BFF))
        cube(size.width * 0.34f, size.height * 0.64f, size.minDimension * 0.20f, Color(0xFF28D5CF))
        cube(size.width * 0.75f, size.height * 0.67f, size.minDimension * 0.18f, Color(0xFF8A74FF))
    }
}

@Composable
internal fun PremiumPoStepper(
    labels: List<String>,
    currentStep: Int,
    enabled: Boolean,
    onStepSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalIsDarkTheme.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            labels.forEachIndexed { index, label ->
                val isCurrent = index == currentStep
                val completed = index < currentStep
                val background = when {
                    isCurrent -> Brush.horizontalGradient(
                        if (dark) listOf(Color(0xFF8D33A5), Color(0xFF4B2676))
                        else listOf(Color(0xFF8A27A0), Color(0xFF692078))
                    )
                    else -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }
                Surface(
                    onClick = { if (enabled) onStepSelected(index) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                        .semantics { selected = isCurrent }
                        .testTag("po_step_$index"),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Transparent,
                    border = null
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical=7.dp).clip(RoundedCornerShape(9.dp)).background(background).padding(horizontal = 3.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}. " + label,
                            color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PremiumPoCostLine(label: String, amount: String, amountColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            amount,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = amountColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
internal fun PremiumPoDateField(
    label: String,
    dateMillis: Long?,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    allowClear: Boolean = false,
    onDateSelected: (Long?) -> Unit
) {
    var showPicker by rememberSaveable(label) { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis ?: System.currentTimeMillis())
    val displayDate = dateMillis?.let { java.text.SimpleDateFormat("d MMM yyyy",language.locale).format(java.util.Date(it)) }
        ?: if (language == AppLanguage.BANGLA) "নির্বাচন করুন" else "Select date"
    PoValueCard(label,displayDate,Icons.Outlined.CalendarMonth,modifier,onClick={showPicker=true})
    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateSelected(datePickerState.selectedDateMillis)
                    showPicker = false
                }) { Text(if (language == AppLanguage.BANGLA) "নির্বাচন" else "Select") }
            },
            dismissButton = {
                Row {
                    if (allowClear && dateMillis != null) {
                        TextButton(onClick = { onDateSelected(null); showPicker = false }) {
                            Text(if (language == AppLanguage.BANGLA) "মুছুন" else "Clear")
                        }
                    }
                    TextButton(onClick = { showPicker = false }) {
                        Text(if (language == AppLanguage.BANGLA) "বাতিল" else "Cancel")
                    }
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
internal fun PremiumPoMetric(
    label: String,
    value: String,
    icon: ImageVector = Icons.Outlined.Inventory2,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
