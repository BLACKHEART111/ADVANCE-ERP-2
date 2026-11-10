package com.pocketdev.advance_erp.feature.settings.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
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
    backupPassphrase: String? = null,
    language: AppLanguage,
    onCreateBackup: (onFinished: () -> Unit) -> Unit,
    onRestoreBackupFile: (file: File, passphrase: String?) -> Unit,
    onRestoreFromUri: (uri: Uri, passphrase: String?) -> Unit,
    onShareBackup: (file: File, passphrase: String) -> Unit,
    onDeleteBackup: (recordId: String) -> Unit,
    onToggleFridayAutoBackup: (enabled: Boolean) -> Unit,
    onSelectBackupFolder: ((uri: Uri?) -> Unit)? = null,
    onSetBackupPassphrase: ((String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val isBn = language == AppLanguage.BANGLA
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }

    var isCreatingBackup by remember { mutableStateOf(false) }
    var backupToRestore by remember { mutableStateOf<BackupRecord?>(null) }
    var restoreHistoryPassphrase by remember { mutableStateOf("") }
    var backupToDelete by remember { mutableStateOf<BackupRecord?>(null) }
    var backupToShare by remember { mutableStateOf<BackupRecord?>(null) }
    var exportPassphrase by remember { mutableStateOf("") }
    var confirmExportPassphrase by remember { mutableStateOf("") }
    var restorePassphrase by remember { mutableStateOf("") }
    var pendingUriToRestore by remember { mutableStateOf<Uri?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var newPassphrase by remember { mutableStateOf("") }
    var confirmNewPassphrase by remember { mutableStateOf("") }

    // File Picker Launcher for Restoring .zip
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            restorePassphrase = backupPassphrase ?: ""
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
        // Main Actions Card: Instant Backup + File Restore + Friday Auto Backup
        ERPCard {
            Column(modifier = Modifier.padding(4.dp)) {
                // Instant Backup Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBn) "তাত্ক্ষণিক সম্পূর্ণ ব্যাকআপ (.zip)" else "Instant Full Backup (.zip)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isBn) "সকল পণ্যের ছবি, প্রোফাইল ও ডাটাবেজ date/mm/yy ফরম্যাটে সেভ হবে"
                            else "Bundles all photos, SQLite database & audit logs in date/mm/yy format",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

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
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        if (isCreatingBackup) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isBn) "ব্যাকআপ নিন" else "Create Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp), tint = SecondaryTeal)
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
                            imageVector = Icons.Default.EventRepeat,
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
                                imageVector = Icons.Default.FolderZip,
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
                            Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryPurple)
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

                // Backup Encryption Password Section (5 to 10 characters)
                val isPassSet = !backupPassphrase.isNullOrBlank()
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
                                imageVector = if (isPassSet) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (isPassSet) StatusSuccess else PrimaryPurple,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isBn) "ব্যাকআপ এনক্রিপশন পাসওয়ার্ড (৫-১০ অক্ষর)" else "Backup Encryption Password (5-10 chars)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isPassSet) {
                                        if (isBn) "🔒 সক্রিয় • সকল অটো ও ম্যানুয়াল ব্যাকআপ এই পাসওয়ার্ড দিয়ে সুরক্ষিত"
                                        else "🔒 Active • All auto & manual backups are encrypted with this password"
                                    } else {
                                        if (isBn) "🔓 সেট নেই • সাধারণ জিপ (.zip) হিসেবে ব্যাকআপ সংরক্ষণ হবে"
                                        else "🔓 Not set • Backups are saved as plain unencrypted .zip"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPassSet) StatusSuccess else TextSecondary,
                                    fontWeight = if (isPassSet) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            newPassphrase = backupPassphrase ?: ""
                            confirmNewPassphrase = backupPassphrase ?: ""
                            showPasswordDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isPassSet) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isPassSet) StatusSuccess else PrimaryPurple
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPassSet) {
                                if (isBn) "পাসওয়ার্ড পরিবর্তন বা বাতিল করুন" else "Change or Remove Password"
                            } else {
                                if (isBn) "পাসওয়ার্ড সেট করুন (৫-১০ অক্ষর)" else "Set Backup Password (5-10 chars)"
                            },
                            fontWeight = FontWeight.Bold,
                            color = if (isPassSet) StatusSuccess else PrimaryPurple,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }

        // Previous Backups History List
        if (backupRecords.isNotEmpty()) {
            ERPCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderZip,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = bak.fileName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = displayDate,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = TextMuted
                                            )
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = TextMuted
                                            )
                                            Text(
                                                text = sizeStr,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                fontWeight = FontWeight.SemiBold,
                                                color = PrimaryPurple
                                            )
                                            if (bak.isAutomatic) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(StatusSuccess.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = if (isBn) "স্বয়ংক্রিয়" else "Auto",
                                                        fontSize = 9.sp,
                                                        color = StatusSuccess,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Share button
                                    IconButton(
                                        onClick = {
                                            if (file.exists()) {
                                                backupToShare = bak
                                            } else {
                                                Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = PrimaryPurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Restore button
                                    IconButton(
                                        onClick = {
                                            backupToRestore = bak
                                            restoreHistoryPassphrase = backupPassphrase ?: ""
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Restore,
                                            contentDescription = "Restore",
                                            tint = SecondaryTeal,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Delete button
                                    IconButton(
                                        onClick = { backupToDelete = bak },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
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
    }

    // Confirmation Dialog for Restoring from History
    backupToRestore?.let { bak ->
        val isEnc = bak.fileName.endsWith(".enc") || bak.notes?.contains("Encrypted") == true
        AlertDialog(
            onDismissRequest = { backupToRestore = null; restoreHistoryPassphrase = "" },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = SecondaryTeal) },
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
                    if (isEnc) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBn) "🔒 এটি একটি এনক্রিপ্ট করা ব্যাকআপ। ব্যাকআপ পাসওয়ার্ড দিন:" else "🔒 This is an encrypted backup. Enter backup password:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                        OutlinedTextField(
                            value = restoreHistoryPassphrase,
                            onValueChange = { restoreHistoryPassphrase = it },
                            label = { Text(if (isBn) "ব্যাকআপ পাসওয়ার্ড" else "Backup Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = File(bak.filePath)
                        onRestoreBackupFile(file, restoreHistoryPassphrase.takeIf { it.isNotBlank() })
                        backupToRestore = null
                        restoreHistoryPassphrase = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                    enabled = !isEnc || restoreHistoryPassphrase.isNotBlank()
                ) {
                    Text(if (isBn) "হ্যাঁ, রিস্টোর করুন" else "Yes, Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null; restoreHistoryPassphrase = "" }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Confirmation Dialog for Restoring from picked Uri
    pendingUriToRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingUriToRestore = null; restorePassphrase = "" },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = SecondaryTeal) },
            title = {
                Text(
                    text = if (isBn) "নির্বাচিত জিপ ফাইল রিস্টোর করুন" else "Restore Selected ZIP File",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isBn) "ফাইলটি যাচাই করে ডাটা ও ছবি রিস্টোর হবে। এনক্রিপ্ট করা ব্যাকআপ হলে পাসফ্রেজ দিন।" else "The backup will be verified before restore. Enter a passphrase for encrypted exports.")
                    OutlinedTextField(
                        value = restorePassphrase,
                        onValueChange = { restorePassphrase = it },
                        label = { Text(if (isBn) "ব্যাকআপ পাসফ্রেজ (ঐচ্ছিক)" else "Backup passphrase (optional)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreFromUri(uri, restorePassphrase.takeIf { it.isNotBlank() })
                        pendingUriToRestore = null
                        restorePassphrase = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                ) {
                    Text(if (isBn) "রিস্টোর শুরু করুন" else "Start Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUriToRestore = null; restorePassphrase = "" }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    backupToShare?.let { backup ->
        AlertDialog(
            onDismissRequest = {
                backupToShare = null
                exportPassphrase = ""
                confirmExportPassphrase = ""
            },
            icon = { Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryPurple) },
            title = { Text(if (isBn) "এনক্রিপ্ট করা ব্যাকআপ শেয়ার" else "Share encrypted backup", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isBn) "কমপক্ষে ৮ অক্ষরের পাসফ্রেজ দিন। অন্য ডিভাইসে রিস্টোরের জন্য এটি আলাদা করে সংরক্ষণ করুন।" else "Set a passphrase of at least 8 characters. Keep it separately to restore this backup on another device.")
                    OutlinedTextField(
                        value = exportPassphrase,
                        onValueChange = { exportPassphrase = it },
                        label = { Text(if (isBn) "পাসফ্রেজ" else "Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = confirmExportPassphrase,
                        onValueChange = { confirmExportPassphrase = it },
                        label = { Text(if (isBn) "পাসফ্রেজ নিশ্চিত করুন" else "Confirm passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        isError = confirmExportPassphrase.isNotEmpty() && confirmExportPassphrase != exportPassphrase
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = exportPassphrase.length >= 8 && exportPassphrase == confirmExportPassphrase,
                    onClick = {
                        onShareBackup(File(backup.filePath), exportPassphrase)
                        backupToShare = null
                        exportPassphrase = ""
                        confirmExportPassphrase = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) { Text(if (isBn) "এনক্রিপ্ট ও শেয়ার" else "Encrypt & Share") }
            },
            dismissButton = {
                TextButton(onClick = {
                    backupToShare = null
                    exportPassphrase = ""
                    confirmExportPassphrase = ""
                }) { Text(if (isBn) "বাতিল" else "Cancel") }
            }
        )
    }

    // Confirmation Dialog for Deleting
    backupToDelete?.let { bak ->
        AlertDialog(
            onDismissRequest = { backupToDelete = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
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

    if (showPasswordDialog) {
        val isValidLength = newPassphrase.length in 5..10
        val isMatching = newPassphrase == confirmNewPassphrase
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryPurple) },
            title = {
                Text(
                    text = if (isBn) "ব্যাকআপ পাসওয়ার্ড সেট করুন" else "Set Backup Password",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBn)
                            "৫ থেকে ১০ অক্ষরের মধ্যে পাসওয়ার্ড দিন। একবার সংরক্ষণ করলে সকল অটো ও ম্যানুয়াল ব্যাকআপ এই পাসওয়ার্ড দিয়ে অটো-এনক্রিপ্ট হয়ে সেভ হবে। রিস্টোরের সময় এই পাসওয়ার্ড লাগবে।"
                        else
                            "Enter a password between 5 and 10 characters. Once saved, all automatic and manual backups will be encrypted with this password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = newPassphrase,
                        onValueChange = { if (it.length <= 10) newPassphrase = it },
                        label = { Text(if (isBn) "পাসওয়ার্ড (৫-১০ অক্ষর)" else "Password (5-10 chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = newPassphrase.isNotEmpty() && !isValidLength,
                        supportingText = {
                            Text(
                                text = "${newPassphrase.length}/10 ${if (!isValidLength && newPassphrase.isNotEmpty()) if (isBn) "(কমপক্ষে ৫ ও সর্বোচ্চ ১০ অক্ষর হতে হবে)" else "(Must be 5-10 chars)" else ""}",
                                color = if (isValidLength) StatusSuccess else TextMuted
                            )
                        }
                    )
                    OutlinedTextField(
                        value = confirmNewPassphrase,
                        onValueChange = { if (it.length <= 10) confirmNewPassphrase = it },
                        label = { Text(if (isBn) "পাসওয়ার্ড নিশ্চিত করুন" else "Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = confirmNewPassphrase.isNotEmpty() && !isMatching,
                        supportingText = {
                            if (confirmNewPassphrase.isNotEmpty() && !isMatching) {
                                Text(if (isBn) "পাসওয়ার্ড মিলছে না" else "Passwords do not match", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetBackupPassphrase?.invoke(newPassphrase.trim())
                        showPasswordDialog = false
                    },
                    enabled = isValidLength && isMatching,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text(if (isBn) "সংরক্ষণ করুন" else "Save Password", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!backupPassphrase.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                onSetBackupPassphrase?.invoke(null)
                                showPasswordDialog = false
                            }
                        ) {
                            Text(if (isBn) "পাসওয়ার্ড সরান" else "Remove", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { showPasswordDialog = false }) {
                        Text(if (isBn) "বাতিল" else "Cancel")
                    }
                }
            }
        )
    }
}
