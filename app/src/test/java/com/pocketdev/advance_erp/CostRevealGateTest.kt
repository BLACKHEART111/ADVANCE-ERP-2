package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.security.CostRevealGate
import org.junit.Assert.*
import org.junit.Test

class CostRevealGateTest {
    @Test fun startsHiddenAndDeniedRoleCannotAuthenticate() {
        val gate = CostRevealGate()
        assertFalse(gate.isVisible)
        assertNull(gate.begin(false))
        assertFalse(gate.isAuthenticating)
    }
    @Test fun successfulCurrentAuthenticationRevealsCost() {
        val gate = CostRevealGate()
        val token = requireNotNull(gate.begin(true))
        assertFalse(gate.isVisible)
        gate.authenticated(token)
        assertTrue(gate.isVisible)
        assertFalse(gate.isAuthenticating)
    }
    @Test fun errorOrCancellationNeverRevealsCost() {
        val gate = CostRevealGate()
        val token = requireNotNull(gate.begin(true))
        gate.rejected(token)
        gate.authenticated(token)
        assertFalse(gate.isVisible)
    }
    @Test fun backgroundOrScopeDisposalInvalidatesLateSuccess() {
        val gate = CostRevealGate()
        val old = requireNotNull(gate.begin(true))
        gate.lock()
        gate.authenticated(old)
        assertFalse(gate.isVisible)
        val current = requireNotNull(gate.begin(true))
        gate.authenticated(old)
        assertFalse(gate.isVisible)
        gate.authenticated(current)
        assertTrue(gate.isVisible)
    }
    @Test fun hidingRequiresNewAuthenticationToRevealAgain() {
        val gate = CostRevealGate()
        val token = requireNotNull(gate.begin(true))
        gate.authenticated(token)
        gate.lock()
        gate.authenticated(token)
        assertFalse(gate.isVisible)
        assertNotNull(gate.begin(true))
    }
    @Test fun repeatedTapAndOldErrorCannotReplaceCurrentAttempt() {
        val gate = CostRevealGate()
        val old = requireNotNull(gate.begin(true))
        assertNull(gate.begin(true))
        gate.rejected(old)
        val current = requireNotNull(gate.begin(true))
        gate.rejected(old)
        assertTrue(gate.isAuthenticating)
        gate.authenticated(current)
        assertTrue(gate.isVisible)
    }
}
