package com.pocketdev.advance_erp.core.security

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

internal class BiometricCostVisibility(
    private val gate: CostRevealGate,
    val toggle: () -> Unit
) {
    val isVisible: Boolean get() = gate.isVisible
}

private fun Context.fragmentActivity(): FragmentActivity? {
    var candidate = this
    while (candidate is ContextWrapper) {
        if (candidate is FragmentActivity) return candidate
        val base = candidate.baseContext
        if (base === candidate) break
        candidate = base
    }
    return candidate as? FragmentActivity
}

/** Strong system biometrics only: no app PIN or device-credential fallback for cost disclosure. */
@Composable
internal fun rememberBiometricCostVisibility(
    key: Any?,
    allowed: Boolean,
    isBangla: Boolean,
    promptTitle: String? = null,
    promptSubtitle: String? = null
): BiometricCostVisibility {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val activity = remember(context) { context.fragmentActivity() }
    val gate = remember(key, allowed, owner) { CostRevealGate() }
    val promptHolder = remember(gate) { arrayOfNulls<BiometricPrompt>(1) }

    DisposableEffect(gate, owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                gate.lock()
                promptHolder[0]?.cancelAuthentication()
                promptHolder[0] = null
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            gate.lock()
            promptHolder[0]?.cancelAuthentication()
            promptHolder[0] = null
        }
    }

    return remember(gate, activity, isBangla, promptTitle, promptSubtitle) {
        BiometricCostVisibility(gate) toggle@{
            if (gate.isVisible) {
                gate.lock()
                return@toggle
            }
            if (!allowed || gate.isAuthenticating) return@toggle
            val token = gate.begin(allowed) ?: return@toggle
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
            val status = BiometricManager.from(context).canAuthenticate(authenticators)
            if (activity == null || status != BiometricManager.BIOMETRIC_SUCCESS ||
                !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                gate.rejected(token)
                val message = if (status == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                    if (isBangla) "ফোনের Settings-এ fingerprint সেট করুন।" else "Set up fingerprint in your phone settings."
                } else {
                    if (isBangla) "Fingerprint যাচাই এখন পাওয়া যাচ্ছে না। খরচ ঢাকা থাকবে।" else "Biometric verification is unavailable. Cost remains hidden."
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                return@toggle
            }
            lateinit var prompt: BiometricPrompt
            prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) gate.authenticated(token)
                        else gate.rejected(token)
                        if (promptHolder[0] === prompt) promptHolder[0] = null
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        gate.rejected(token)
                        if (promptHolder[0] === prompt) promptHolder[0] = null
                        if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                            errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_CANCELED) {
                            Toast.makeText(context, errString, Toast.LENGTH_LONG).show()
                        }
                    }
                    // A mismatched fingerprint leaves the gate locked while the system allows retry.
                })
            promptHolder[0] = prompt
            try {
                prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
                    .setTitle(promptTitle ?: if (isBangla) "ল্যান্ডেড খরচ দেখুন" else "View landed cost")
                    .setSubtitle(promptSubtitle ?: if (isBangla) "খরচ দেখতে fingerprint দিয়ে যাচাই করুন" else "Verify your biometric to reveal cost")
                    .setAllowedAuthenticators(authenticators)
                    .setNegativeButtonText(if (isBangla) "বাতিল" else "Cancel")
                    .build())
            } catch (_: RuntimeException) {
                gate.rejected(token)
                promptHolder[0] = null
                Toast.makeText(context, if (isBangla) "যাচাই চালু হয়নি। আবার চেষ্টা করুন।" else "Could not start verification. Try again.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
