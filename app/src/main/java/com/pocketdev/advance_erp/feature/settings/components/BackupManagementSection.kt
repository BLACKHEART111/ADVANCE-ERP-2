package com.pocketdev.advance_erp.feature.settings.components

import androidx.compose.runtime.saveable.rememberSaveable
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.LocalSettingsTokens
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.BackupRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupManagementSection(
    backupRecords: List<BackupRecord>,
    isFridayAutoBackupEnabled: Boolean,
    lastAutoBackupTime: Long?,
    backupFolderUri: String? = null,
    language: AppLanguage,
    onCreateBackup: (onFinished: () -> Unit) -> Unit,
    onRestoreBackupFile: (file: File) -> Unit,
    onRestoreFromUri: (uri: Uri) -> Unit,
    onShareBackup: (file: File) -> Unit,
    onDeleteBackup: (recordId: String) -> Unit,
    onToggleFridayAutoBackup: (enabled: Boolean) -> Unit,
    onSelectBackupFolder: ((uri: Uri?) -> Unit)? = null,
    onResetTestDataKeepMasters: ((onFinished: () -> Unit) -> Unit)? = null,
    embeddedInAccordion: Boolean = false
) {
    val context = LocalContext.current
    val tokens = LocalSettingsTokens.current
    val isBn = language == AppLanguage.BANGLA
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }

    var isCreatingBackup by rememberSaveable { mutableStateOf(false) }
    var isResettingTestData by rememberSaveable { mutableStateOf(false) }
    var showResetTestDataConfirm by rememberSaveable { mutableStateOf(false) }
    var backupToRestore by remember { mutableStateOf<BackupRecord?>(null) }
    var backupToDelete by remember { mutableStateOf<BackupRecord?>(null) }
    var pendingUriToRestore by remember { mutableStateOf<Uri?>(null) }

    // File Picker Launcher for Restoring .zip
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingUriToRestore = uri
        }
    }

    // Folder Picker Launcher for Selecting Phone File Manager Backup Destination
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            onSelectBackupFolder?.invoke(treeUri)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Main Actions Panel: Instant Backup + File Restore + Friday Auto Backup + Location
        val mainContent: @Composable () -> Unit = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Instant Backup Block (full-width readable button below description)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isBn) "তাত্ক্ষণিক সম্পূর্ণ ব্যাকআপ (.zip)" else "Instant Full Backup (.zip)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isBn) "সকল পণ্যের ছবি, প্রোফাইল ও ডাটাবেজ নিরাপদ জিপ ফাইলে সেভ হবে"
                        else "Bundles all photos, profile & SQLite database into a verified ZIP archive",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            isCreatingBackup = true
                            onCreateBackup {
                                isCreatingBackup = false
                                Toast.makeText(
                                    context,
                                    if (isBn) "সম্পূর্ণ ব্যাকআপ ফাইল তৈরি হয়েছে!" else "Full ZIP backup created!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = !isCreatingBackup,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        if (isCreatingBackup) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isBn) "ব্যাকআপ তৈরি হচ্ছে..." else "Creating Backup...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(imageVector = Icons.Outlined.Archive, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isBn) "এখনই ব্যাকআপ নিন" else "Backup Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = ERPDivider)

                // Pick from Storage Button
                OutlinedButton(
                    onClick = {
                        try {
                            zipPickerLauncher.launch("*/*")
                        } catch (e: Exception) {
                            zipPickerLauncher.launch("*/*")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp), tint = SecondaryTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBn) "ডিভাইস থেকে ব্যাকআপ ফাইল রিস্টোর করুন (.zip)" else "Choose Backup ZIP from Storage to Restore",
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryTeal,
                        fontSize = 12.5.sp
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = ERPDivider)

                // Friday Auto-Backup Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EventRepeat,
                            contentDescription = null,
                            tint = if (isFridayAutoBackupEnabled) StatusSuccess else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBn) "প্রতি শুক্রবার স্বয়ংক্রিয় ব্যাকআপ" else "Every Friday Auto-Backup",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isFridayAutoBackupEnabled) {
                                    if (isBn) "সক্রিয় • প্রতি শুক্রবার রাত ১০:০০ টায় ছবিসহ ব্যাকআপ তৈরি হবে"
                                    else "Active • Auto creates full ZIP backup every Friday at 10 PM"
                                } else {
                                    if (isBn) "নিষ্ক্রিয়" else "Disabled"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isFridayAutoBackupEnabled) StatusSuccess else TextMuted
                            )
                            if (lastAutoBackupTime != null && lastAutoBackupTime > 0L) {
                                val timeStr = dateFormat.format(Date(lastAutoBackupTime))
                                Text(
                                    text = "${if (isBn) "সর্বশেষ ব্যাকআপ: " else "Last backup: "}${if (isBn) Formatters.toBanglaDigits(timeStr) else timeStr}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    Switch(
                        checked = isFridayAutoBackupEnabled,
                        onCheckedChange = { onToggleFridayAutoBackup(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = StatusSuccess
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = ERPDivider)

                // Phone File Manager Backup Folder Location Picker
                val readableFolderPath = remember(backupFolderUri) {
                    if (backupFolderUri.isNullOrBlank()) {
                        "Downloads/AdvanceERP_Backups"
                    } else {
                        try {
                            Uri.decode(Uri.parse(backupFolderUri).lastPathSegment ?: backupFolderUri)
                        } catch (_: Exception) {
                            backupFolderUri
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderZip,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isBn)
                                        "ফোন ফাইল ম্যানেজারে অটো সেভ লোকেশন"
                                    else
                                        "Phone File Manager Auto-Save Location",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isBn)
                                        "ফোল্ডার: $readableFolderPath (শুক্রবারের অটো ব্যাকআপ ও ম্যানুয়াল ব্যাকআপ এখানে সরাসরি সেভ হবে)"
                                    else
                                        "Folder: $readableFolderPath (Friday auto-backup & manual backups are saved here)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryTeal,
                                    fontWeight = FontWeight.SemiBold
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
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryPurple)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "ফাইল ম্যানেজার থেকে ফোল্ডার সিলেক্ট করুন" else "Select Folder in File Manager",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPurple,
                                fontSize = 11.5.sp
                            )
                        }

                        if (!backupFolderUri.isNullOrBlank() && onSelectBackupFolder != null) {
                            TextButton(
                                onClick = { onSelectBackupFolder(null) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (isBn) "ডিফল্ট (Downloads)" else "Reset Default",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = ERPDivider)

                if (onResetTestDataKeepMasters != null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isBn) {
                                    "টেস্ট ডাটা রিসেট (প্রোডাক্ট ও কন্টাক্ট অক্ষত রাখুন)"
                                } else {
                                    "Reset Test Data (Keep Products & Contacts)"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBn) {
                                "আপনার যোগ করা সকল প্রোডাক্ট, ক্যাটাগরি, ভ্যারিয়েন্ট, ছবি এবং কাস্টমার/সাপ্লায়ার অক্ষত থাকবে। শুধুমাত্র সকল ইনভয়েস, পিও, শিপমেন্ট, পেমেন্ট, লেজার হিসাব এবং স্টক (০) ক্লিয়ার হবে।"
                            } else {
                                "Keeps all products, categories, variants, photos, customers & suppliers intact while clearing test invoices, POs, shipments, payments, ledgers & stock back to 0."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showResetTestDataConfirm = true },
                            enabled = !isResettingTestData && !isCreatingBackup,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f))
                        ) {
                            if (isResettingTestData) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.error,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBn) "টেস্ট ডাটা ক্লিয়ার হচ্ছে..." else "Clearing Test Data...",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.5.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBn) {
                                        "প্রোডাক্ট ও কন্টাক্ট রেখে টেস্ট ডাটা ক্লিয়ার করুন"
                                    } else {
                                        "Clear Test Data (Keep Products & Contacts)"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (embeddedInAccordion) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = tokens.innerPanelBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, tokens.innerPanelBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    mainContent()
                }
            }
        } else {
            ERPCard {
                Box(modifier = Modifier.padding(4.dp)) {
                    mainContent()
                }
            }
        }

        // Previous Backups History List
        if (backupRecords.isNotEmpty()) {
            val historyContent: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.History, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (isBn) "পূর্ববর্তী ব্যাকআপের তালিকা (${backupRecords.size})" else "Backup History (${backupRecords.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    HorizontalDivider(color = ERPDivider)

                    backupRecords.take(8).forEach { bak ->
                        val file = File(bak.filePath)
                        val sizeKb = bak.fileSizeBytes / 1024
                        val sizeStr = if (sizeKb > 1024) "%.1f MB".format(sizeKb / 1024.0) else "$sizeKb KB"
                        val dateFormatted = dateFormat.format(Date(bak.createdAt))
                        val displayDate = if (isBn) Formatters.toBanglaDigits(dateFormatted) else dateFormatted

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FolderZip,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = bak.fileName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (bak.isAutomatic) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(StatusSuccess.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isBn) "স্বয়ংক্রিয়" else "Auto",
                                                fontSize = 9.5.sp,
                                                color = StatusSuccess,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = sizeStr,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryPurple,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = TextMuted
                                        )
                                        Text(
                                            text = displayDate,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (file.exists()) {
                                                    onShareBackup(file)
                                                } else {
                                                    Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Share,
                                                contentDescription = "Share",
                                                tint = PrimaryPurple,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                backupToRestore = bak
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Restore,
                                                contentDescription = "Restore",
                                                tint = SecondaryTeal,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { backupToDelete = bak },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (embeddedInAccordion) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = tokens.innerPanelBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, tokens.innerPanelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        historyContent()
                    }
                }
            } else {
                ERPCard {
                    historyContent()
                }
            }
        }
    }

    // Confirmation Dialog for Restoring from History
    backupToRestore?.let { bak ->
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            icon = { Icon(Icons.Outlined.Restore, contentDescription = null, tint = SecondaryTeal) },
            title = {
                Text(
                    text = if (isBn) "ব্যাকআপ ফাইল রিস্টোর নিশ্চিতকরণ" else "Confirm Database Restore",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBn) "আপনি কি নিশ্চিত যে '${bak.fileName}' ফাইল থেকে ডাটা রিস্টোর করতে চান?\n\nরিস্টোরের পূর্বে বর্তমান ডাটার একটি সেফটি কপি তৈরি করা হবে এবং ব্যাকআপে থাকা সকল ছবি ও ডাটা পুনরুদ্ধার হবে।"
                        else "Are you sure you want to restore from '${bak.fileName}'?\n\nA safety backup of your active data will be created before restoring."
                    )

                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = File(bak.filePath)
                        onRestoreBackupFile(file)
                        backupToRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                ) {
                    Text(if (isBn) "হ্যাঁ, রিস্টোর করুন" else "Yes, Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Confirmation Dialog for Restoring from picked Uri
    pendingUriToRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingUriToRestore = null },
            icon = { Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = SecondaryTeal) },
            title = {
                Text(
                    text = if (isBn) "নির্বাচিত জিপ ফাইল রিস্টোর করুন" else "Restore Selected ZIP File",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isBn) "ফাইলটি যাচাই করে ডাটা ও ছবি রিস্টোর হবে। কোনো পাসওয়ার্ড লাগবে না।" else "The backup will be verified before restoring data and images. No password is needed.")

                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreFromUri(uri)
                        pendingUriToRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                ) {
                    Text(if (isBn) "রিস্টোর শুরু করুন" else "Start Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUriToRestore = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }



    // Confirmation Dialog for Deleting
    backupToDelete?.let { bak ->
        AlertDialog(
            onDismissRequest = { backupToDelete = null },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = {
                Text(
                    text = if (isBn) "ব্যাকআপ ফাইল মুছবেন?" else "Delete Backup File?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBn) "আপনি কি '${bak.fileName}' ব্যাকআপ ফাইলটি স্থায়ীভাবে মুছে ফেলতে চান?"
                    else "Are you sure you want to permanently delete '${bak.fileName}'?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBackup(bak.id)
                        backupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isBn) "মুছে ফেলুন" else "Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToDelete = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Confirmation Dialog for Resetting Test Transactions & Stock While Keeping Products & Contacts
    if (showResetTestDataConfirm && onResetTestDataKeepMasters != null) {
        AlertDialog(
            onDismissRequest = { showResetTestDataConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = if (isBn) "টেস্ট ডাটা রিসেট নিশ্চিত করুন" else "Confirm Test Data Reset",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBn) {
                            "অক্ষত থাকবে:\n• সকল প্রোডাক্ট, ক্যাটাগরি, ভ্যারিয়েন্ট ও ছবি\n• সকল কাস্টমার ও সাপ্লায়ার (বকেয়া ৳০ হবে)\n• ব্যবসা প্রোফাইল, ওয়্যারহাউস, অ্যাকাউন্ট তালিকা ও ব্যাকআপ হিস্টোরি\n\nমুছে যাবে (০ হবে):\n• সকল স্টক ব্যালেন্স ও মুভমেন্ট\n• সকল ক্রয় অর্ডার (PO), শিপমেন্ট ও রিসিপ্ট\n• সকল সেলস ইনভয়েস, পেমেন্ট, খরচ ও জার্নাল লেজার\n\nরিসেটের আগে বর্তমান ডাটাবেজের একটি সেফটি কপি স্বয়ংক্রিয়ভাবে সংরক্ষণ করা হবে।"
                        } else {
                            "Will remain intact:\n• All products, categories, variants & product images\n• All customers & suppliers (balances reset to 0)\n• Business profile, warehouses, chart of accounts & backup history\n\nWill be cleared to 0:\n• All stock balances & stock movements\n• All purchase orders (PO), shipments & receipts\n• All sales invoices, payments, expenses & accounting journals\n\nA safety copy of your current database will be saved automatically before resetting."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetTestDataConfirm = false
                        isResettingTestData = true
                        onResetTestDataKeepMasters {
                            isResettingTestData = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = if (isBn) "হ্যাঁ, টেস্ট ডাটা ক্লিয়ার করুন" else "Yes, Clear Test Data",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetTestDataConfirm = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

}
