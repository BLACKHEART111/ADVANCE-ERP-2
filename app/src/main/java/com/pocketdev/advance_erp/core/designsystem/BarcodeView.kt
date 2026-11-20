package com.pocketdev.advance_erp.core.designsystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.advance_erp.core.util.BarcodeGenerator

@Composable
fun BarcodeView(
    code: String,
    modifier: Modifier = Modifier,
    showActions: Boolean = true,
    collapsible: Boolean = false,
    title: String? = null,
    initiallyExpanded: Boolean = true
) {
    val sanitizedCode = code.trim().ifBlank { "0000000000" }

    val modules = remember(sanitizedCode) {
        try {
            BarcodeGenerator.encodeCode128(sanitizedCode)
        } catch (_: Exception) {
            BarcodeGenerator.encodeCode128(sanitizedCode.filter { it.code in 32..126 }.ifEmpty { "BARCODE" })
        }
    }

    if (collapsible) {
        CollapsibleCodeContainer(
            title = title,
            codePreview = sanitizedCode,
            leadingIcon = Icons.Outlined.ViewAgenda,
            copyLabel = "Barcode",
            copyText = sanitizedCode,
            copyToastMessage = "Barcode copied: $sanitizedCode",
            shareChooserTitle = "Share Barcode",
            copyContentDescription = "Copy Barcode",
            shareContentDescription = "Share Barcode",
            expandContentDescription = "Expand Barcode",
            collapseContentDescription = "Collapse Barcode",
            showActions = showActions,
            initiallyExpanded = initiallyExpanded,
            modifier = modifier
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                BarcodeCanvas(modules = modules)
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, ERPDivider, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            BarcodeCanvas(modules = modules)
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (showActions) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sanitizedCode,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                CodeCopyShareActions(
                    copyLabel = "Barcode",
                    textToCopy = sanitizedCode,
                    toastMessage = "Barcode copied: $sanitizedCode",
                    shareChooserTitle = "Share Barcode",
                    copyContentDescription = "Copy Barcode",
                    shareContentDescription = "Share Barcode"
                )
            }
        } else {
            Text(
                text = sanitizedCode,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun BarcodeCanvas(modules: BooleanArray) {
    Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
        val totalModules = modules.size
        val moduleWidth = size.width / totalModules.toFloat()

        for (i in modules.indices) {
            if (modules[i]) {
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(i * moduleWidth, 0f),
                    size = Size(moduleWidth + 0.5f, size.height)
                )
            }
        }
    }
}

@Composable
internal fun CollapsibleCodeContainer(
    title: String?,
    codePreview: String,
    leadingIcon: ImageVector,
    copyLabel: String,
    copyText: String,
    copyToastMessage: String,
    shareChooserTitle: String,
    copyContentDescription: String,
    shareContentDescription: String,
    expandContentDescription: String,
    collapseContentDescription: String,
    showActions: Boolean,
    initiallyExpanded: Boolean,
    modifier: Modifier = Modifier,
    expandedContent: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ERPSurface)
            .border(1.dp, ERPDivider, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (!title.isNullOrBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = codePreview,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showActions) {
                    CodeCopyShareActions(
                        copyLabel = copyLabel,
                        textToCopy = copyText,
                        toastMessage = copyToastMessage,
                        shareChooserTitle = shareChooserTitle,
                        copyContentDescription = copyContentDescription,
                        shareContentDescription = shareContentDescription
                    )
                }
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) collapseContentDescription else expandContentDescription,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                expandedContent()
            }
        }
    }
}

@Composable
internal fun CodeCopyShareActions(
    copyLabel: String,
    textToCopy: String,
    toastMessage: String,
    shareChooserTitle: String,
    copyContentDescription: String,
    shareContentDescription: String
) {
    val context = LocalContext.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(copyLabel, textToCopy)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = copyContentDescription,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, textToCopy)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, shareChooserTitle)
                context.startActivity(shareIntent)
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = shareContentDescription,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
