package com.pocketdev.advance_erp.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF792B87), onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E8F6), onPrimaryContainer = Color(0xFF4E1D59),
    secondary = Color(0xFF1B7180), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2F3), onSecondaryContainer = Color(0xFF164A53),
    tertiary = Color(0xFF55465E), onTertiary = Color.White,
    background = Color(0xFFF7F5F9), onBackground = Color(0xFF241E2B),
    surface = Color.White, onSurface = Color(0xFF241E2B),
    surfaceVariant = Color(0xFFF0EBF3), onSurfaceVariant = Color(0xFF655B6D),
    outline = Color(0xFF82758A), outlineVariant = Color(0xFFE3DCE8),
    error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFFCE4E1), onErrorContainer = Color(0xFF641C16)
)
internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD5A4E0), onPrimary = Color(0xFF391242),
    primaryContainer = Color(0xFF4D2858), onPrimaryContainer = Color(0xFFF4D8FC),
    secondary = Color(0xFF81CDD4), onSecondary = Color(0xFF07373E),
    secondaryContainer = Color(0xFF21464C), onSecondaryContainer = Color(0xFFB4ECF0),
    tertiary = Color(0xFFD1BDDD), onTertiary = Color(0xFF352740),
    background = Color(0xFF151218), onBackground = Color(0xFFF4EEF7),
    surface = Color(0xFF211B27), onSurface = Color(0xFFF4EEF7),
    surfaceVariant = Color(0xFF302735), onSurfaceVariant = Color(0xFFCBBFD1),
    outline = Color(0xFF9B8DA3), outlineVariant = Color(0xFF483D50),
    error = Color(0xFFFFB4AB), onError = Color(0xFF601410),
    errorContainer = Color(0xFF522522), onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun AdvanceErpTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            window.statusBarColor = colors.surface.toArgb()
            window.navigationBarColor = colors.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    val drawerTokens = if (darkTheme) DarkDrawerTokens else LightDrawerTokens
    val settingsTokens = if (darkTheme) DarkSettingsTokens else LightSettingsTokens
    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalDrawerTokens provides drawerTokens,
        LocalSettingsTokens provides settingsTokens
    ) {
        MaterialTheme(colorScheme = colors, typography = ERPTypography, shapes = ERPShapes, content = content)
    }
}
