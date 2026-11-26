package com.pocketdev.advance_erp.core.communication

import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Native handoff only. A successful launch does NOT prove a message was sent. */
object WhatsAppShareManager {
    const val MESSENGER = "com.whatsapp"
    const val BUSINESS = "com.whatsapp.w4b"

    fun installed(context: Context): List<String> = listOf(MESSENGER, BUSINESS).filter { pkg ->
        try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isAttachmentReady(context: Context, file: File?): Boolean {
        if (file == null || !file.isFile || file.length() <= 0L) return false
        val root = runCatching { File(context.cacheDir, "communication").canonicalFile }.getOrNull() ?: return false
        val canonical = runCatching { file.canonicalFile }.getOrNull() ?: return false
        return canonical.path.startsWith(root.path + File.separator)
    }

    fun sendIntent(context: Context, file: File?, text: String, application: String?): Intent {
        require(text.isNotBlank() || file != null) { "Message or attachment required / বার্তা অথবা সংযুক্তি প্রয়োজন" }
        return Intent(Intent.ACTION_SEND).apply {
            type = if (file == null) {
                "text/plain"
            } else if (file.extension == "xlsx") {
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            } else {
                "application/pdf"
            }
            if (file != null) {
                require(isAttachmentReady(context, file)) { "Attachment unavailable; prepare again / সংযুক্তি অনুপলব্ধ; পুনরায় তৈরি করুন" }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, "Business document", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            putExtra(Intent.EXTRA_TEXT, text)
            application?.let {
                require(it in listOf(MESSENGER, BUSINESS))
                setPackage(it)
            }
        }
    }

    fun share(context: Context, file: File?, text: String, application: String?): String {
        if (application != null) {
            require(application in installed(context)) { "WhatsApp is not installed / WhatsApp ইনস্টল নেই" }
        }
        val intent = sendIntent(context, file, text, application)
        context.startActivity(
            if (application == null) Intent.createChooser(intent, "Share / শেয়ার") else intent
        )
        return "SHARE_INITIATED"
    }

    fun openChat(context: Context, phone: String, application: String): String {
        val normalized = requireNotNull(CommunicationPhone.normalize(phone)) { "Invalid phone / সঠিক আন্তর্জাতিক নম্বর দিন" }
        require(application in installed(context)) { "WhatsApp is not installed / WhatsApp ইনস্টল নেই" }
        // Public click-to-chat link; no undocumented jid/direct-recipient extras.
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${normalized.drop(1)}")).setPackage(application)
        )
        return "WHATSAPP_OPENED"
    }

    fun copy(context: Context, message: String) {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("Business message", message))
    }
}
