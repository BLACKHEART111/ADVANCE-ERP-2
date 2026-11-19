package com.pocketdev.advance_erp.core.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** A native full-screen editor: system bars and keyboard never cover its actions. */
@Composable
fun ERPFullScreenEditor(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) { content() }
        }
    }
}

@Composable
fun ERPLeaveEditorDialog(isBangla: Boolean, onSaveDraft: () -> Unit, onDiscard: () -> Unit, onContinue: () -> Unit) {
    AlertDialog(
        onDismissRequest = onContinue,
        title = { Text(if (isBangla) "পরিবর্তন সংরক্ষণ করবেন?" else "Save your changes?") },
        text = { Text(if (isBangla) "খসড়া রাখলে পরে এখান থেকে কাজ চালাতে পারবেন। এটি অর্ডার নিশ্চিত বা স্টকে পোস্ট করবে না।" else "Keep a draft to continue later. This does not confirm the order or post stock.") },
        confirmButton = { Button(onClick = onSaveDraft) { Text(if (isBangla) "খসড়া রাখুন" else "Save draft") } },
        dismissButton = {
            Column {
                TextButton(onClick = onContinue) { Text(if (isBangla) "সম্পাদনা চালান" else "Continue editing") }
                TextButton(onClick = onDiscard) { Text(if (isBangla) "বাদ দিন" else "Discard", color = MaterialTheme.colorScheme.error) }
            }
        }
    )
}
