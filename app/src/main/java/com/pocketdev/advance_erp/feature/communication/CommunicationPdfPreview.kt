package com.pocketdev.advance_erp.feature.communication

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CommunicationPdfPreview(file: File, bn: Boolean, onDismiss: () -> Unit) {
    var page by rememberSaveable(file.absolutePath) { mutableIntStateOf(0) }
    var count by remember(file.absolutePath) { mutableIntStateOf(0) }
    var bitmap by remember(file.absolutePath) { mutableStateOf<Bitmap?>(null) }
    var error by remember(file.absolutePath) { mutableStateOf<String?>(null) }

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
                            val targetW = 1000
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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = if (bn) "বন্ধ" else "Close")
                    }
                    TextButton(onClick = { page = (page - 1).coerceAtLeast(0) }, enabled = page > 0) {
                        Text(if (bn) "আগে" else "Prev")
                    }
                    Text("${(page + 1).coerceAtLeast(1)} / ${count.coerceAtLeast(1)}", Modifier.weight(1f))
                    TextButton(onClick = { page = (page + 1).coerceAtMost((count - 1).coerceAtLeast(0)) }, enabled = page + 1 < count) {
                        Text(if (bn) "পরে" else "Next")
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
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
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
}
