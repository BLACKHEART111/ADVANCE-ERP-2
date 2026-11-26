package com.pocketdev.advance_erp.feature.communication

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CommunicationPdfPreview(file: File, bn: Boolean, onDismiss: () -> Unit) {
    var page by remember(file) { mutableStateOf(0) }
    var count by remember(file) { mutableStateOf(0) }
    var bitmap by remember(file) { mutableStateOf<Bitmap?>(null) }
    var error by remember(file) { mutableStateOf<String?>(null) }
    LaunchedEffect(file, page) {
        bitmap = null; error = null
        try {
            val rendered = withContext(Dispatchers.IO) {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd -> PdfRenderer(fd).use { renderer ->
                    val pages = renderer.pageCount
                    renderer.openPage(page.coerceIn(0, pages - 1)).use { pdfPage ->
                        val image = Bitmap.createBitmap(1000, (1000f * pdfPage.height / pdfPage.width).toInt(), Bitmap.Config.ARGB_8888)
                        image.eraseColor(android.graphics.Color.WHITE)
                        pdfPage.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pages to image
                    }
                } }
            }
            count = rendered.first; bitmap = rendered.second
        } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; error = e.message }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text(if(bn) "বন্ধ" else "Close") }
                TextButton(onClick = { page-- }, enabled = page > 0) { Text("←") }
                Text("${page + 1} / $count", Modifier.padding(12.dp))
                TextButton(onClick = { page++ }, enabled = page + 1 < count) { Text("→") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if(bitmap == null && error == null) CircularProgressIndicator()
            bitmap?.let { image -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Image(image.asImageBitmap(), contentDescription = if(bn) "PDF পৃষ্ঠা" else "PDF page", modifier = Modifier.fillMaxWidth().aspectRatio(image.width.toFloat() / image.height))
            } }
        } }
    }
}
