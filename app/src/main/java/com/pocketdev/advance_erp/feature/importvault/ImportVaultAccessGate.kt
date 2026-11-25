package com.pocketdev.advance_erp.feature.importvault

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pocketdev.advance_erp.core.designsystem.LocalDashboardTokens
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.security.BiometricHelper
import com.pocketdev.advance_erp.core.security.BiometricStatus
import com.pocketdev.advance_erp.core.security.CostRevealGate
import com.pocketdev.advance_erp.core.security.ImportVaultAccessGuard
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.core.security.VaultUnlockPolicy

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
fun ImportVaultAccessGate(
    userRole: UserRole,
    language: AppLanguage,
    onUnlocked: (biometricFallbackActive: Boolean) -> Unit,
    onLocked: () -> Unit,
    onBack: () -> Unit,
    content: @Composable (biometricFallbackActive: Boolean) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findFragmentActivity() }
    val gate = remember(userRole, owner) { CostRevealGate() }
    val promptHolder = remember(gate) { arrayOfNulls<BiometricPrompt>(1) }
    var biometricStatus by remember { mutableStateOf(BiometricHelper.checkBiometricStatus(context)) }
    var hasPromptedOnEntry by remember(userRole) { mutableStateOf(false) }

    val policy = remember(userRole, biometricStatus) {
        ImportVaultAccessGuard.evaluateAccess(userRole, biometricStatus)
    }

    val launchBiometricPrompt = remember(gate, activity, isBn, policy) {
        launch@{
            if (policy != VaultUnlockPolicy.REQUIRE_BIOMETRIC || gate.isVisible || gate.isAuthenticating) {
                return@launch
            }
            val token = gate.begin(true) ?: return@launch
            if (activity == null || !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                gate.rejected(token)
                return@launch
            }
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            lateinit var prompt: BiometricPrompt
            prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            gate.authenticated(token)
                            onUnlocked(false)
                        } else {
                            gate.rejected(token)
                            onLocked()
                        }
                        if (promptHolder[0] === prompt) promptHolder[0] = null
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        gate.rejected(token)
                        if (promptHolder[0] === prompt) promptHolder[0] = null
                        if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                            errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.ERROR_CANCELED
                        ) {
                            Toast.makeText(context, errString, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
            promptHolder[0] = prompt
            try {
                prompt.authenticate(
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle(if (isBn) "আমদানি আর্কাইভ আনলক করুন" else "Unlock Import Vault")
                        .setSubtitle(
                            if (isBn) {
                                "মুনাফা, খরচ ও মূলধনের তথ্য দেখতে ফিঙ্গারপ্রিন্ট দিয়ে যাচাই করুন"
                            } else {
                                "Verify biometric to access import profits and capital intelligence"
                            }
                        )
                        .setAllowedAuthenticators(authenticators)
                        .setNegativeButtonText(if (isBn) "বাতিল" else "Cancel")
                        .build()
                )
            } catch (_: RuntimeException) {
                gate.rejected(token)
                promptHolder[0] = null
                Toast.makeText(
                    context,
                    if (isBn) "বায়োমেট্রিক যাচাই চালু করা যায়নি। আবার চেষ্টা করুন।" else "Unable to start biometric verification. Try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    DisposableEffect(gate, owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    biometricStatus = BiometricHelper.checkBiometricStatus(context)
                }
                Lifecycle.Event.ON_STOP -> {
                    gate.lock()
                    promptHolder[0]?.cancelAuthentication()
                    promptHolder[0] = null
                    hasPromptedOnEntry = false
                    onLocked()
                }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            gate.lock()
            promptHolder[0]?.cancelAuthentication()
            promptHolder[0] = null
            onLocked()
        }
    }

    LaunchedEffect(policy, gate.isVisible) {
        when (policy) {
            VaultUnlockPolicy.DENIED_ROLE -> {
                gate.lock()
                onLocked()
            }
            VaultUnlockPolicy.SAFE_FALLBACK_NO_BIOMETRIC -> {
                onUnlocked(true)
            }
            VaultUnlockPolicy.REQUIRE_BIOMETRIC -> {
                if (!gate.isVisible && !hasPromptedOnEntry) {
                    hasPromptedOnEntry = true
                    launchBiometricPrompt()
                }
            }
        }
    }

    when (policy) {
        VaultUnlockPolicy.DENIED_ROLE -> {
            VaultLockedStateCard(
                isRoleDenied = true,
                isBn = isBn,
                onVerifyClick = {},
                onBack = onBack
            )
        }
        VaultUnlockPolicy.SAFE_FALLBACK_NO_BIOMETRIC -> {
            content(true)
        }
        VaultUnlockPolicy.REQUIRE_BIOMETRIC -> {
            if (gate.isVisible) {
                content(false)
            } else {
                VaultLockedStateCard(
                    isRoleDenied = false,
                    isBn = isBn,
                    onVerifyClick = { launchBiometricPrompt() },
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun VaultLockedStateCard(
    isRoleDenied: Boolean,
    isBn: Boolean,
    onVerifyClick: () -> Unit,
    onBack: () -> Unit
) {
    val tokens = LocalDashboardTokens.current
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.pageBgTop)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, if (isRoleDenied) colors.error.copy(alpha = 0.4f) else colors.outlineVariant),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRoleDenied) colors.errorContainer else colors.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRoleDenied) Icons.Outlined.Lock else Icons.Outlined.Fingerprint,
                        contentDescription = null,
                        tint = if (isRoleDenied) colors.error else colors.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = when {
                        isRoleDenied && isBn -> "অ্যাক্সেস সংরক্ষিত"
                        isRoleDenied -> "Access Restricted"
                        isBn -> "আমদানি আর্কাইভ সুরক্ষিত"
                        else -> "Import Vault Locked"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when {
                        isRoleDenied && isBn -> "শুধুমাত্র Owner / Admin রোল ব্যবহারকারীরা আমদানি আর্কাইভ ও প্রকৃত মুনাফার তথ্য দেখতে পারবেন।"
                        isRoleDenied -> "Only Owner / Admin accounts can view Import Vault cost, profit, and capital intelligence."
                        isBn -> "প্রকৃত ক্রয়মূল্য, ল্যান্ডেড কস্ট ও মুনাফার হিসাব আনলক করতে বায়োমেট্রিক দিয়ে যাচাই করুন।"
                        else -> "Verify your biometric to unlock import cost layers, net profit, and capital analytics."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (!isRoleDenied) {
                    Button(
                        onClick = onVerifyClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isBn) "ফিঙ্গারপ্রিন্ট দিয়ে আনলক করুন" else "Verify Biometric",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isBn) "ফিরে যান" else "Go Back")
                }
            }
        }
    }
}
