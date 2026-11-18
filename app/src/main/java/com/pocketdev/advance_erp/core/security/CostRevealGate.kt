package com.pocketdev.advance_erp.core.security

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** One disclosure, one successful authentication. Stale callbacks cannot unlock a new scope. */
internal class CostRevealGate {
    var isVisible by mutableStateOf(false)
        private set
    var isAuthenticating by mutableStateOf(false)
        private set
    private var generation = 0L

    fun begin(allowed: Boolean): Long? {
        if (!allowed || isAuthenticating || isVisible) return null
        isAuthenticating = true
        return ++generation
    }

    fun authenticated(token: Long) {
        if (token != generation || !isAuthenticating) return
        isAuthenticating = false
        isVisible = true
    }

    fun rejected(token: Long) {
        if (token == generation && isAuthenticating) lock()
    }

    fun lock() {
        generation++
        isVisible = false
        isAuthenticating = false
    }
}
