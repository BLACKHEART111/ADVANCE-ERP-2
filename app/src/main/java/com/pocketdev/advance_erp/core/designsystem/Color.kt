package com.pocketdev.advance_erp.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.luminance

// Primary Purple tokens
val PrimaryPurple: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val PrimaryPurpleDark = Color(0xFF5B1F67)
val PrimaryPurpleLight = Color(0xFF9944A9)
val PrimaryPurpleContainer: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primaryContainer
val OnPrimaryPurpleContainer: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onPrimaryContainer

// Secondary Teal tokens
val SecondaryTeal: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary
val SecondaryTealDark = Color(0xFF1B6E7D)
val SecondaryTealLight = Color(0xFF4DBFD4)
val SecondaryTealContainer: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondaryContainer
val OnSecondaryTealContainer: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSecondaryContainer

// Deep Navy tokens
val DeepNavy = Color(0xFF263849)
val DeepNavyDark = Color(0xFF1B2835)
val DeepNavyLight = Color(0xFF384F66)
val DeepNavySurface = Color(0xFF1E2D3D)

// Drawer / Sidebar Theme tokens (Color Grading & Accents)
val DrawerNavyHeader = Color(0xFF243242)
val DrawerNavyHeaderDark = Color(0xFF1B2835)
val DrawerTeal = Color(0xFF25849D)
val DrawerTealDark = Color(0xFF1E6F84)
val DrawerTealLight = Color(0xFF339BB6)
val DrawerEditBadge = Color(0xFF22A2BC)
val DrawerDivider = Color(0x33FFFFFF)
val DrawerTextMuted = Color(0xB3FFFFFF)
val DrawerIconTint = Color(0xF2FFFFFF)

// Surfaces & Backgrounds
val ERPBackground: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
val ERPSurface: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
val ERPSurfaceVariant: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceVariant
val ERPDivider: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outlineVariant

// Text tokens
val TextPrimary: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
val TextSecondary: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val TextMuted: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val TextOnDark = Color(0xFFFFFFFF)

// Accent / Status tokens
val StatusSuccess: Color
    @Composable @ReadOnlyComposable get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFF83D8A7) else Color(0xFF167347)
val StatusSuccessContainer: Color
    @Composable @ReadOnlyComposable get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFF173E2A) else Color(0xFFE3F3E9)
val StatusWarning: Color
    @Composable @ReadOnlyComposable get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFFF5CD81) else Color(0xFF855500)
val StatusWarningContainer: Color
    @Composable @ReadOnlyComposable get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFF47371B) else Color(0xFFFFF1D2)
val StatusError: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.error
val StatusErrorContainer: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.errorContainer
val StatusInfo: Color
    @Composable @ReadOnlyComposable get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFFA8C7FA) else Color(0xFF245FB0)
val StatusInfoContainer: Color
    @Composable @ReadOnlyComposable get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFF203A60) else Color(0xFFE4EDFA)
