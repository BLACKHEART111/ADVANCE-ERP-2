package com.pocketdev.advance_erp.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.core.util.QrCodeGenerator

@Composable
fun QrCodeView(
    payload: String,
    modifier: Modifier = Modifier,
    qrSizeDp: Int = 180,
    showActions: Boolean = true,
    collapsible: Boolean = false,
    title: String? = null,
    initiallyExpanded: Boolean = true
) {
    val sanitized = payload.trim().ifBlank { "ADV-ERP" }

    val matrix = remember(sanitized) {
        QrCodeGenerator.generateQrMatrix(sanitized)
    }

    if (collapsible) {
        CollapsibleCodeContainer(
            title = title,
            codePreview = sanitized,
            leadingIcon = Icons.Outlined.QrCode,
            copyLabel = "QR Code",
            copyText = sanitized,
            copyToastMessage = "QR Code data copied",
            shareChooserTitle = "Share QR Code",
            copyContentDescription = "Copy QR Data",
            shareContentDescription = "Share QR Code",
            expandContentDescription = "Expand QR Code",
            collapseContentDescription = "Collapse QR Code",
            showActions = showActions,
            initiallyExpanded = initiallyExpanded,
            modifier = modifier
        ) {
            Box(
                modifier = Modifier
                    .size((qrSizeDp + 12).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                QrMatrixCanvas(matrix = matrix, qrSizeDp = qrSizeDp)
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
                .size(qrSizeDp.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            QrMatrixCanvas(matrix = matrix, qrSizeDp = qrSizeDp)
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (showActions) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sanitized,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                CodeCopyShareActions(
                    copyLabel = "QR Code",
                    textToCopy = sanitized,
                    toastMessage = "QR Code data copied",
                    shareChooserTitle = "Share QR Code",
                    copyContentDescription = "Copy QR Data",
                    shareContentDescription = "Share QR Code"
                )
            }
        } else {
            Text(
                text = sanitized,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QrMatrixCanvas(
    matrix: Array<BooleanArray>,
    qrSizeDp: Int
) {
    val matrixSize = matrix.size
    val quietZone = 3
    val totalModules = matrixSize + quietZone * 2

    Canvas(modifier = Modifier.size(qrSizeDp.dp)) {
        val moduleSize = size.width / totalModules.toFloat()

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (matrix[r][c]) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset((c + quietZone) * moduleSize, (r + quietZone) * moduleSize),
                        size = Size(moduleSize + 0.3f, moduleSize + 0.3f)
                    )
                }
            }
        }
    }
}
