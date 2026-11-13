package com.pocketdev.advance_erp.feature.imports

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.FullScreenImageDialog
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.designsystem.rememberImageModel
import com.pocketdev.advance_erp.core.util.PoAttachmentKind
import com.pocketdev.advance_erp.core.util.PoAttachmentStorage
import kotlinx.coroutines.launch
import java.io.File

/**
 * Interactive picker section for PO invoices, photos, documents, and optional URLs.
 */
@Composable
fun PoAttachmentPickerSection(
    attachments: List<String>,
    onAttachmentsChanged: (List<String>) -> Unit,
    isBangla: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var showLinkInput by remember { mutableStateOf(false) }
    var linkDraft by remember { mutableStateOf("") }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        coroutineScope.launch {
            isProcessing = true
            try {
                val added = mutableListOf<String>()
                for (uri in uris) {
                    val savedPath = PoAttachmentStorage.saveDocumentOrImageFromUri(context, uri)
                    added.add(savedPath)
                }
                if (added.isNotEmpty()) {
                    onAttachmentsChanged((attachments + added).distinct())
                }
            } catch (t: Throwable) {
                Toast.makeText(
                    context,
                    if (isBangla) "ফাইল সংরক্ষণ ব্যর্থ: ${t.message}" else "Failed to attach file: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isProcessing = false
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        val uri = pendingCameraUri
        pendingCameraFile = null
        pendingCameraUri = null
        if (success && uri != null) {
            coroutineScope.launch {
                isProcessing = true
                try {
                    val savedPath = PoAttachmentStorage.saveImageFromUri(
                        context = context,
                        sourceUri = uri,
                        preferredName = "po_invoice_cam.jpg"
                    )
                    onAttachmentsChanged((attachments + savedPath).distinct())
                } catch (t: Throwable) {
                    Toast.makeText(
                        context,
                        if (isBangla) "ছবি সংরক্ষণ ব্যর্থ: ${t.message}" else "Failed to save photo: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    file?.delete()
                    isProcessing = false
                }
            }
        } else {
            file?.delete()
        }
    }

    fun launchCameraCapture() {
        try {
            val (tmpFile, tmpUri) = PoAttachmentStorage.createCameraTempUri(context)
            pendingCameraFile = tmpFile
            pendingCameraUri = tmpUri
            takePictureLauncher.launch(tmpUri)
        } catch (t: Throwable) {
            Toast.makeText(
                context,
                if (isBangla) "ক্যামেরা চালু করা যায়নি: ${t.message}" else "Unable to open camera: ${t.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            Toast.makeText(
                context,
                if (isBangla) "ছবি তোলার জন্য ক্যামেরা অনুমতি প্রয়োজন" else "Camera permission is required to take photos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        importUris(uris)
    }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        importUris(uris)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, ERPDivider)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBangla) "ইনভয়েস / ডকুমেন্ট / ছবি সংযুক্তি (${attachments.size})" else "Invoice / Document / Photo Attachments (${attachments.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                }
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryPurple)
                } else {
                    TextButton(
                        onClick = { showLinkInput = !showLinkInput },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isBangla) "লিংক" else "Link",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                        if (hasCam) {
                            launchCameraCapture()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp), tint = PrimaryPurple)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isBangla) "ক্যামেরা" else "Camera", fontSize = 11.sp, color = DeepNavy, maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(15.dp), tint = SecondaryTeal)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isBangla) "গ্যালারি" else "Gallery", fontSize = 11.sp, color = DeepNavy, maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        documentLauncher.launch(arrayOf("application/pdf", "image/*", "*/*"))
                    },
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isBangla) "ফাইল/PDF" else "File/PDF", fontSize = 11.sp, color = DeepNavy, maxLines = 1)
                }
            }

            if (showLinkInput) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = linkDraft,
                        onValueChange = { linkDraft = it },
                        placeholder = { Text(if (isBangla) "https://... বা ফাইল রেফারেন্স" else "https://... or file reference", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            val clean = linkDraft.trim().replace(",", " ")
                            if (clean.isNotBlank()) {
                                onAttachmentsChanged((attachments + clean).distinct())
                                linkDraft = ""
                                showLinkInput = false
                            }
                        }
                    ) {
                        Text(if (isBangla) "যোগ" else "Add", fontWeight = FontWeight.Bold, color = PrimaryPurple)
                    }
                }
            }

            if (attachments.isEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isBangla) "ইনভয়েসের ছবি তুলুন অথবা গ্যালারি/ফাইল থেকে বেছে নিন" else "Snap an invoice photo or pick images/PDFs from device",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                PoAttachmentCards(
                    attachments = attachments,
                    isBangla = isBangla,
                    onRemove = { target ->
                        onAttachmentsChanged(attachments.filterNot { it == target })
                    }
                )
            }
        }
    }
}

/**
 * Shared attachment list renderer supporting image preview, PDF/document opening, and optional removal.
 */
@Composable
fun PoAttachmentCards(
    attachments: List<String>,
    isBangla: Boolean,
    onRemove: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var previewImageUri by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        attachments.forEach { rawRef ->
            val info = remember(rawRef) { PoAttachmentStorage.inspectAttachment(rawRef) }
            val canPreviewImage = info.kind == PoAttachmentKind.IMAGE &&
                (info.localFile != null || info.rawValue.startsWith("http", ignoreCase = true))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, ERPDivider, RoundedCornerShape(8.dp))
                    .clickable {
                        if (canPreviewImage) {
                            previewImageUri = info.rawValue
                        } else {
                            PoAttachmentStorage.openAttachment(context, info.rawValue, isBangla)
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canPreviewImage) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(rememberImageModel(info.rawValue))
                            .crossfade(true)
                            .build(),
                        contentDescription = info.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF1F5F9))
                    )
                } else {
                    val (icon, tint, bg) = when (info.kind) {
                        PoAttachmentKind.PDF -> Triple(Icons.Default.PictureAsPdf, Color(0xFFDC2626), Color(0xFFFEE2E2))
                        PoAttachmentKind.LINK -> Triple(Icons.Default.Link, PrimaryPurple, PrimaryPurple.copy(alpha = 0.12f))
                        else -> Triple(Icons.Default.Description, SecondaryTeal, SecondaryTeal.copy(alpha = 0.12f))
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(bg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = when {
                        info.kind == PoAttachmentKind.LINK -> if (isBangla) "ওয়েব লিংক • খুলতে ট্যাপ করুন" else "Web link • Tap to open"
                        info.localFile == null -> if (isBangla) "রেফারেন্স নাম" else "File reference"
                        info.kind == PoAttachmentKind.IMAGE -> if (isBangla) "ছবি • বড় করে দেখতে ট্যাপ করুন" else "Photo • Tap to preview"
                        info.kind == PoAttachmentKind.PDF -> if (isBangla) "PDF ডকুমেন্ট • খুলতে ট্যাপ করুন" else "PDF document • Tap to open"
                        else -> if (isBangla) "ফাইল • খুলতে ট্যাপ করুন" else "File • Tap to open"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                if (onRemove != null) {
                    IconButton(
                        onClick = { onRemove(rawRef) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = StatusError,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    previewImageUri?.let { imagePath ->
        FullScreenImageDialog(
            imageUriOrPath = imagePath,
            onDismiss = { previewImageUri = null }
        )
    }
}
