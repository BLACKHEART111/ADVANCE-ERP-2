package com.pocketdev.advance_erp.core.designsystem

import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/** A native full-screen editor: system bars and keyboard never cover its actions. */
@Composable
fun ERPFullScreenEditor(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(decorFitsSystemWindows = false, dismissOnClickOutside = false)
    ) {
        val window = (LocalView.current.parent as DialogWindowProvider).window
        SideEffect {
            // Compose 1.6's non-default-width mode fixes the window to the full display size.
            // Let the native window measure against the available space when the IME opens.
            val matchParent = WindowManager.LayoutParams.MATCH_PARENT
            if (window.attributes.width != matchParent || window.attributes.height != matchParent) {
                window.setLayout(matchParent, matchParent)
            }
            @Suppress("DEPRECATION")
            val resize = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            if (window.attributes.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST != resize) {
                window.setSoftInputMode(resize)
            }
        }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) { content() }
        }
    }
}

@Composable
fun ERPLeaveEditorDialog(
    isBangla: Boolean,
    onSaveDraft: () -> Unit,
    onDiscard: () -> Unit,
    onContinue: () -> Unit,
    isSaving: Boolean = false
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onContinue() },
        title = { Text(if (isBangla) "পরিবর্তন সংরক্ষণ করবেন?" else "Save your changes?") },
        text = { Text(if (isBangla) "খসড়া রাখলে পরে এখান থেকে কাজ চালাতে পারবেন। এটি অর্ডার নিশ্চিত বা স্টকে পোস্ট করবে না।" else "Keep a draft to continue later. This does not confirm the order or post stock.") },
        confirmButton = {
            Button(onClick = onSaveDraft, enabled = !isSaving) {
                Text(when {
                    isSaving && isBangla -> "সংরক্ষণ হচ্ছে…"
                    isSaving -> "Saving…"
                    isBangla -> "খসড়া রাখুন"
                    else -> "Save draft"
                })
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onContinue, enabled = !isSaving) { Text(if (isBangla) "সম্পাদনা চালান" else "Continue editing") }
                TextButton(onClick = onDiscard, enabled = !isSaving) { Text(if (isBangla) "বাদ দিন" else "Discard", color = MaterialTheme.colorScheme.error) }
            }
        }
    )
}
