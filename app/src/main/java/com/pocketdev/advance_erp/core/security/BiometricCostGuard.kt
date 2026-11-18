package com.pocketdev.advance_erp.core.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun LockedCostPanel(allowed: Boolean, isBangla: Boolean, onVerify: () -> Unit, onDismiss: (() -> Unit)? = null) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (isBangla) "খরচের তথ্য সুরক্ষিত" else "Cost information is locked", style = MaterialTheme.typography.titleMedium)
            Text(if (!allowed) {
                if (isBangla) "এই তথ্য দেখার অনুমতি নেই।" else "You do not have permission to view this information."
            } else {
                if (isBangla) "Cost Layer ও খরচের তথ্য দেখতে fingerprint দিয়ে যাচাই করুন।" else "Verify your biometric to view cost layers and cost information."
            }, style = MaterialTheme.typography.bodyMedium)
            if (allowed) Button(onClick = onVerify, modifier = Modifier.fillMaxWidth()) {
                Text(if (isBangla) "Fingerprint দিয়ে যাচাই করুন" else "Verify biometric")
            }
            if (onDismiss != null) TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(if (isBangla) "বন্ধ করুন" else "Close")
            }
        }
    }
}

/** Locked content is not composed, so layer values and export/adjust controls cannot leak. */
@Composable
internal fun BiometricCostGuard(scopeKey: Any?, allowed: Boolean, isBangla: Boolean, content: @Composable () -> Unit) {
    val visibility = rememberBiometricCostVisibility(scopeKey, allowed, isBangla)
    if (visibility.isVisible) content()
    else LockedCostPanel(allowed, isBangla, visibility.toggle)
}
