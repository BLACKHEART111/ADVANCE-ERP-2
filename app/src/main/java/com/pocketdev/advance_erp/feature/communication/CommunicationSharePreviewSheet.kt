package com.pocketdev.advance_erp.feature.communication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.communication.CommunicationModule
import com.pocketdev.advance_erp.core.communication.CommunicationStatus
import com.pocketdev.advance_erp.core.communication.PreparedCommunication
import com.pocketdev.advance_erp.core.communication.ShareOutputFormat
import com.pocketdev.advance_erp.core.communication.SharePreviewTab
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPSurface
import com.pocketdev.advance_erp.core.designsystem.ERPSurfaceVariant
import com.pocketdev.advance_erp.core.designsystem.LocalIsDarkTheme
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusSuccessContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal val WhatsAppCtaGreen = Color(0xFF1FAF5A)

/**
 * Approved Reference Screen 2 ("Share Preview"): 3-way segmented viewer (`PDF` | `Image` | `Text`)
 * with language toggle, live editable message, and `Share` / `Save` / `Print` bottom action bar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CommunicationSharePreviewSheet(
    state: CommunicationUiState,
    prepared: PreparedCommunication,
    bn: Boolean,
    hasWhatsApp: Boolean,
    onDismiss: () -> Unit,
    onSelectTab: (SharePreviewTab) -> Unit,
    onMessageChange: (String) -> Unit,
    onRequestOverwrite: (PendingPreviewOverwrite) -> Unit,
    onEnsurePdf: () -> Unit,
    onActivatePdf: () -> Unit,
    onGenerateXlsx: () -> Unit,
    onEnsureImage: () -> Unit,
    onCopyMessage: () -> Unit,
    onShareActiveTab: (SharePreviewTab) -> Unit,
    onSaveActiveTab: (SharePreviewTab) -> Unit,
    onPrintPdf: () -> Unit,
    onSendViaWhatsApp: () -> Unit
) {
    fun label(en: String, bangla: String) = if (bn) bangla else en
    val isDark = LocalIsDarkTheme.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ERPBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .imePadding()
            ) {
                // Top header matching Reference #2
                Surface(
                    color = ERPSurface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = label("Close preview", "প্রিভিউ বন্ধ")
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = label("Preview", "প্রিভিউ"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${prepared.document.number} · ${prepared.document.partyName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            SingleChoiceLanguageBadge(
                                activeLanguage = prepared.language,
                                onSelect = { code ->
                                    if (prepared.language != code) {
                                        onRequestOverwrite(PendingPreviewOverwrite.SwitchLanguage(code))
                                    }
                                }
                            )
                        }

                        // 3-Way Segmented Pill Selector: PDF | Image | Text
                        Surface(
                            color = ERPSurfaceVariant,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val tabs = listOf(
                                    SharePreviewTab.PDF to label("PDF", "PDF"),
                                    SharePreviewTab.IMAGE to label("Image", "ইমেজ"),
                                    SharePreviewTab.TEXT to label("Text", "টেক্সট")
                                )
                                tabs.forEach { (tab, title) ->
                                    val selected = state.activePreviewTab == tab
                                    Surface(
                                        color = if (selected) PrimaryPurple else Color.Transparent,
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 40.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable { onSelectTab(tab) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = title,
                                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                state.error?.let { err ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                state.notice?.let { noticeText ->
                    Surface(
                        color = StatusSuccessContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = noticeText,
                            color = StatusSuccess,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Scrollable / weighted live preview viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    when (state.activePreviewTab) {
                        SharePreviewTab.PDF -> PreviewPdfPane(
                            state = state,
                            prepared = prepared,
                            bn = bn,
                            onEnsurePdf = onEnsurePdf,
                            onActivatePdf = onActivatePdf,
                            onGenerateXlsx = onGenerateXlsx,
                            modifier = Modifier.fillMaxSize()
                        )
                        SharePreviewTab.IMAGE -> PreviewImagePane(
                            state = state,
                            prepared = prepared,
                            bn = bn,
                            onEnsureImage = onEnsureImage,
                            modifier = Modifier.fillMaxSize()
                        )
                        SharePreviewTab.TEXT -> PreviewTextPane(
                            state = state,
                            prepared = prepared,
                            bn = bn,
                            isDark = isDark,
                            onMessageChange = onMessageChange,
                            onRequestOverwrite = onRequestOverwrite,
                            onCopyMessage = onCopyMessage,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Bottom Action Bar matching Reference #2: Share | Save | Print + WhatsApp CTA
                Surface(
                    color = ERPSurface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PreviewBottomIconAction(
                                icon = Icons.Outlined.Share,
                                label = label("Share", "শেয়ার"),
                                enabled = !state.busy && state.generatingFormat == null,
                                onClick = { onShareActiveTab(state.activePreviewTab) }
                            )
                            PreviewBottomIconAction(
                                icon = Icons.Outlined.Download,
                                label = when (state.activePreviewTab) {
                                    SharePreviewTab.TEXT -> label("Copy", "কপি")
                                    else -> label("Save", "সেভ")
                                },
                                enabled = !state.busy && state.generatingFormat == null,
                                onClick = { onSaveActiveTab(state.activePreviewTab) }
                            )
                            PreviewBottomIconAction(
                                icon = Icons.Outlined.Print,
                                label = label("Print", "প্রিন্ট"),
                                enabled = !state.busy && state.generatingFormat == null,
                                onClick = onPrintPdf
                            )
                        }
                        if (hasWhatsApp) {
                            Button(
                                onClick = onSendViaWhatsApp,
                                enabled = !state.busy && state.generatingFormat == null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WhatsAppCtaGreen,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Send, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = label("Send via WhatsApp", "WhatsApp-এ শেয়ার করুন"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleChoiceLanguageBadge(
    activeLanguage: String,
    onSelect: (String) -> Unit
) {
    Surface(
        color = ERPSurfaceVariant,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(2.dp)) {
            listOf("bn" to "বাংলা", "en" to "EN").forEach { (code, text) ->
                val selected = activeLanguage == code
                Surface(
                    color = if (selected) PrimaryPurple else Color.Transparent,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelect(code) }
                ) {
                    Text(
                        text = text,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewBottomIconAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewPdfPane(
    state: CommunicationUiState,
    prepared: PreparedCommunication,
    bn: Boolean,
    onEnsurePdf: () -> Unit,
    onActivatePdf: () -> Unit,
    onGenerateXlsx: () -> Unit,
    modifier: Modifier = Modifier
) {
    fun label(en: String, bangla: String) = if (bn) bangla else en
    val supportsXlsx = prepared.document.request.module in setOf(CommunicationModule.PURCHASE_ORDER, CommunicationModule.PACKING)
    val activeAttachment = prepared.attachment
    val pdfToShow = prepared.pdfFile ?: activeAttachment?.takeIf { it.extension.equals("pdf", ignoreCase = true) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (supportsXlsx) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = activeAttachment?.extension.equals("pdf", ignoreCase = true) || activeAttachment == null,
                    onClick = onActivatePdf,
                    label = { Text("PDF") }
                )
                FilterChip(
                    selected = activeAttachment?.extension.equals("xlsx", ignoreCase = true),
                    onClick = onGenerateXlsx,
                    label = { Text(label("XLSX Spreadsheet", "XLSX স্প্রেডশিট")) }
                )
            }
        }
        when {
            state.generatingFormat == ShareOutputFormat.PDF -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator()
                        Text(label("Generating branded PDF...", "ব্র্যান্ডেড PDF তৈরি হচ্ছে..."))
                    }
                }
            }
            activeAttachment != null && activeAttachment.extension.equals("xlsx", ignoreCase = true) && pdfToShow == null -> {
                Surface(
                    color = ERPSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "XLSX · ${activeAttachment.name} (${activeAttachment.length() / 1024} KB)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            label(
                                "Excel file is ready to share or save. Switch to PDF to preview pages or print.",
                                "XLSX ফাইল শেয়ার বা সেভের জন্য প্রস্তুত। প্রিভিউ বা প্রিন্টের জন্য PDF-এ যান।"
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onActivatePdf) {
                            Text(label("Generate & Preview PDF", "PDF তৈরি ও প্রিভিউ দেখুন"))
                        }
                    }
                }
            }
            pdfToShow != null && pdfToShow.isFile && pdfToShow.length() > 0L -> {
                CommunicationPdfPageViewer(
                    file = pdfToShow,
                    bn = bn,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        prepared.attachmentError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = onEnsurePdf) {
                            Text(label("Generate Branded PDF", "ব্র্যান্ডেড PDF তৈরি করুন"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewImagePane(
    state: CommunicationUiState,
    prepared: PreparedCommunication,
    bn: Boolean,
    onEnsureImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    fun label(en: String, bangla: String) = if (bn) bangla else en
    val pages = prepared.imageCardPages
    var pageIdx by rememberSaveable(prepared.document.request.documentId, pages.size) { mutableIntStateOf(0) }
    val activeFile = pages.getOrNull(pageIdx.coerceIn(0, (pages.size - 1).coerceAtLeast(0)))
    var decoded by remember(activeFile?.absolutePath, activeFile?.lastModified()) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(activeFile?.absolutePath, activeFile?.lastModified()) {
        decoded = null
        if (activeFile != null && activeFile.isFile) {
            decoded = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(activeFile.absolutePath)
            }
        }
    }

    DisposableEffect(decoded) {
        val current = decoded
        onDispose {
            if (current != null && !current.isRecycled) {
                current.recycle()
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (pages.size > 1) {
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
                    TextButton(onClick = { pageIdx = (pageIdx - 1).coerceAtLeast(0) }, enabled = pageIdx > 0) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, modifier = Modifier.size(18.dp))
                        Text(label("Prev", "আগে"))
                    }
                    Text(
                        label("Card ${pageIdx + 1} / ${pages.size}", "কার্ড ${pageIdx + 1} / ${pages.size}"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { pageIdx = (pageIdx + 1).coerceAtMost(pages.size - 1) }, enabled = pageIdx + 1 < pages.size) {
                        Text(label("Next", "পরে"))
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        when {
            state.generatingFormat == ShareOutputFormat.IMAGE_CARD -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator()
                        Text(label("Rendering branded Image Card...", "ব্র্যান্ডেড ইমেজ কার্ড তৈরি হচ্ছে..."))
                    }
                }
            }
            activeFile == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        prepared.imageError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = onEnsureImage) {
                            Text(label("Generate Image Card", "ইমেজ কার্ড তৈরি করুন"))
                        }
                    }
                }
            }
            decoded == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                val bmp = decoded!!
                if (!bmp.isRecycled && bmp.width > 0 && bmp.height > 0) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = label("Branded Image Card", "ব্র্যান্ডেড ইমেজ কার্ড"),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewTextPane(
    state: CommunicationUiState,
    prepared: PreparedCommunication,
    bn: Boolean,
    isDark: Boolean,
    onMessageChange: (String) -> Unit,
    onRequestOverwrite: (PendingPreviewOverwrite) -> Unit,
    onCopyMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    fun label(en: String, bangla: String) = if (bn) bangla else en
    val waBubbleBg = if (isDark) Color(0xFF134D37) else Color(0xFFE7FFDB)
    val waBubbleText = if (isDark) Color(0xFFEAF7EF) else Color(0xFF172B1C)

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live WhatsApp Bubble Preview
        Surface(
            color = waBubbleBg,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, WhatsAppCtaGreen.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label("WhatsApp Message Preview", "WhatsApp বার্তার প্রিভিউ"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppCtaGreen
                    )
                    TextButton(onClick = onCopyMessage, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(label("Copy", "কপি"))
                    }
                }
                Text(
                    text = state.message.ifBlank { label("(Empty message)", "(খালি বার্তা)") },
                    style = MaterialTheme.typography.bodyMedium,
                    color = waBubbleText
                )
            }
        }

        // Conditional Status Variant Switchers
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = label("Message tone / condition:", "অবস্থা অনুযায়ী বার্তা পরিবর্তন:"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val statuses = listOfNotNull(
                    CommunicationStatus.GENERAL,
                    prepared.document.status,
                    CommunicationStatus.UNPAID.takeIf { prepared.document.request.module == CommunicationModule.INVOICE },
                    CommunicationStatus.PARTIAL.takeIf { prepared.document.request.module == CommunicationModule.INVOICE },
                    CommunicationStatus.PAID.takeIf { prepared.document.request.module == CommunicationModule.INVOICE },
                    CommunicationStatus.OVERDUE.takeIf { prepared.document.request.module in setOf(CommunicationModule.INVOICE, CommunicationModule.REMINDER) },
                    CommunicationStatus.PREVIOUS_DUE.takeIf { (prepared.document.previousMinor ?: 0L) > 0L },
                    CommunicationStatus.CREDIT.takeIf { (prepared.document.balanceMinor ?: 0L) < 0L }
                ).distinct()
                statuses.forEach { st ->
                    AssistChip(
                        onClick = { onRequestOverwrite(PendingPreviewOverwrite.SwitchStatusTemplate(st)) },
                        label = { Text(st.title(prepared.language)) }
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.message,
            onValueChange = onMessageChange,
            label = { Text(label("Edit message before sharing", "শেয়ারের আগে বার্তা সম্পাদনা করুন")) },
            minLines = 7,
            modifier = Modifier.fillMaxWidth()
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = { onRequestOverwrite(PendingPreviewOverwrite.ReloadCurrentTemplate) },
                enabled = !state.busy
            ) {
                Text(label("Reload custom template", "কাস্টম টেমপ্লেট রিলোড"))
            }
            TextButton(
                onClick = { onRequestOverwrite(PendingPreviewOverwrite.ResetSystemDefault) },
                enabled = !state.busy
            ) {
                Text(label("Reset to system default", "সিস্টেম ডিফল্টে ফিরুন"))
            }
        }
    }
}
