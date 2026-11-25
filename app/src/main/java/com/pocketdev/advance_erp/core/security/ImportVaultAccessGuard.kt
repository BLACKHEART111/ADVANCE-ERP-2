package com.pocketdev.advance_erp.core.security

enum class VaultUnlockPolicy {
    DENIED_ROLE,
    REQUIRE_BIOMETRIC,
    SAFE_FALLBACK_NO_BIOMETRIC
}

/**
 * Policy Gate #7: Enforces OWNER_ADMIN role check + biometric protection
 * with safe fallback when biometric hardware is unavailable or not enrolled.
 */
object ImportVaultAccessGuard {

    fun evaluateAccess(role: UserRole, biometricStatus: BiometricStatus): VaultUnlockPolicy {
        if (!SecurityManager.canAccessImportVault(role)) {
            return VaultUnlockPolicy.DENIED_ROLE
        }
        return when (biometricStatus) {
            BiometricStatus.AVAILABLE -> VaultUnlockPolicy.REQUIRE_BIOMETRIC
            BiometricStatus.NOT_ENROLLED,
            BiometricStatus.NOT_SUPPORTED,
            BiometricStatus.UNAVAILABLE -> VaultUnlockPolicy.SAFE_FALLBACK_NO_BIOMETRIC
        }
    }

    fun canRevealVaultWithBiometricFallback(role: UserRole, biometricStatus: BiometricStatus): Boolean {
        return evaluateAccess(role, biometricStatus) == VaultUnlockPolicy.SAFE_FALLBACK_NO_BIOMETRIC
    }

    fun requireVaultAccess(role: UserRole) {
        if (!SecurityManager.canAccessImportVault(role)) {
            throw SecurityException("Access denied: Only OWNER_ADMIN may access Import Vault & Profit Intelligence (current role: ${role.code})")
        }
    }
}
