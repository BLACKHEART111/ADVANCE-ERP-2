package com.pocketdev.advance_erp.feature.communication

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.core.designsystem.ERPFullScreenEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CommunicationPdfPageViewer(
    file: File,
    bn: Boolean,
    modifier: Modifier = Modifier
) {
    var page by rememberSaveable(file.absolutePath, file.lastModified()) { mutableIntStateOf(0) }
    var count by remember(file.absolutePath, file.lastModified()) { mutableIntStateOf(0) }
    var bitmap by remember(file.absolutePath, file.lastModified()) { mutableStateOf<Bitmap?>(null) }
    var error by remember(file.absolutePath, file.lastModified()) { mutableStateOf<String?>(null) }

    LaunchedEffect(file.absolutePath, file.lastModified(), page) {
        bitmap = null
        error = null
        try {
            val rendered = withContext(Dispatchers.IO) {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { renderer ->
                        val pages = renderer.pageCount
                        require(pages > 0) { if (bn) "PDF-এ কোনো পৃষ্ঠা নেই" else "PDF has no pages" }
                        val safePage = page.coerceIn(0, pages - 1)
                        renderer.openPage(safePage).use { pdfPage ->
                            val targetW = 1200
                            val targetH = ((targetW.toFloat() * pdfPage.height) / pdfPage.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
                            val image = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                            image.eraseColor(android.graphics.Color.WHITE)
                            pdfPage.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            pages to image
                        }
                    }
                }
            }
            count = rendered.first
            if (page >= count) page = (count - 1).coerceAtLeast(0)
            bitmap = rendered.second
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            error = e.message ?: if (bn) "PDF প্রিভিউ ত্রুটি" else "PDF preview failed"
        }
    }

    DisposableEffect(bitmap) {
        val current = bitmap
        onDispose {
            if (current != null && !current.isRecycled) {
                current.recycle()
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (count > 1) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { page = (page - 1).coerceAtLeast(0) },
                        enabled = page > 0
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(if (bn) "আগে" else "Prev")
                    }
                    Text(
                        text = if (bn) "পৃষ্ঠা ${(page + 1).coerceAtLeast(1)} / $count" else "Page ${(page + 1).coerceAtLeast(1)} / $count",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = { page = (page + 1).coerceAtMost((count - 1).coerceAtLeast(0)) },
                        enabled = page + 1 < count
                    ) {
                        Text(if (bn) "পরে" else "Next")
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        }
        if (bitmap == null && error == null) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        bitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }?.let { image ->
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                ) {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = if (bn) "PDF পৃষ্ঠা" else "PDF page",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(image.width.toFloat() / image.height.toFloat())
                    )
                }
            }
        }
    }
}

@Composable
fun CommunicationPdfPreview(file: File, bn: Boolean, onDismiss: () -> Unit) {
    ERPFullScreenEditor(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = if (bn) "বন্ধ" else "Close")
                }
                Text(
                    text = if (bn) "PDF প্রিভিউ · ${file.name}" else "PDF Preview · ${file.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(4.dp))
            CommunicationPdfPageViewer(
                file = file,
                bn = bn,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}
