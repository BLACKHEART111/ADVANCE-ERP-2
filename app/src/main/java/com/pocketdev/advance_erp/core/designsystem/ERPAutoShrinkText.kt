package com.pocketdev.advance_erp.core.designsystem

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Single-line text that shrinks smoothly when content overflows narrow multi-column cards,
 * keeping currency symbols and formatted digits together on one crisp line.
 */
@Composable
fun AutoShrinkSingleLineText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    minFontSizeSp: Float = 10f,
    keepSpacesNonBreaking: Boolean = true
) {
    val displayText = remember(text, keepSpacesNonBreaking) {
        if (keepSpacesNonBreaking) text.replace(' ', '\u00A0') else text
    }
    val initialFontSize = if (style.fontSize != TextUnit.Unspecified) style.fontSize else 14.sp
    var scaledFontSize by remember(displayText, initialFontSize) { mutableStateOf(initialFontSize) }

    val resolvedLineHeight = if (style.lineHeight != TextUnit.Unspecified && initialFontSize.value > 0f) {
        val ratio = scaledFontSize.value / initialFontSize.value
        (style.lineHeight.value * ratio).sp
    } else {
        TextUnit.Unspecified
    }

    Text(
        text = displayText,
        modifier = modifier,
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        style = style.copy(
            fontSize = scaledFontSize,
            lineHeight = resolvedLineHeight
        ),
        onTextLayout = { result ->
            if (result.didOverflowWidth && scaledFontSize.value > minFontSizeSp) {
                val nextSize = (scaledFontSize.value * 0.90f).coerceAtLeast(minFontSizeSp)
                if (nextSize < scaledFontSize.value) {
                    scaledFontSize = nextSize.sp
                }
            }
        }
    )
}
