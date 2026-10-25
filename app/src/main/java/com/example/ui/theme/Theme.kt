package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val MbdLightColorScheme = lightColorScheme(
    primary = MbdCrimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE8E9),
    onPrimaryContainer = MbdCrimsonDark,
    secondary = MbdCallGreen,
    onSecondary = Color.White,
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF181C24),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF181C24),
    surfaceVariant = Color(0xFFEEF1F6),
    onSurfaceVariant = Color(0xFF5E6573),
    outline = Color(0xFFE2E6EE)
)

private val MbdDarkColorScheme = darkColorScheme(
    primary = MbdCrimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3E1418),
    onPrimaryContainer = Color(0xFFFFB3B6),
    secondary = MbdCallGreen,
    onSecondary = Color.White,
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF1A1D24),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF262B36),
    onSurfaceVariant = Color(0xFFA3AAB8),
    outline = Color(0xFF2E3440)
)

@Composable
fun MyApplicationTheme(
    themeModeOverride: MbdThemeMode? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    MbdPreferencesManager.ensureInitialized(context)

    val persistedThemeMode by MbdPreferencesManager.themeModeFlow.collectAsStateWithLifecycle()
    val activeMode = themeModeOverride ?: persistedThemeMode
    val systemDark = isSystemInDarkTheme()

    val isDark = when (activeMode) {
        MbdThemeMode.DARK -> true
        MbdThemeMode.LIGHT -> false
        MbdThemeMode.SYSTEM -> systemDark
    }

    // Update snapshot-backed theme state synchronously to avoid any flicker
    MbdThemeState.isDarkEffective = isDark

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            if (activity != null) {
                val controller = WindowCompat.getInsetsController(activity.window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = if (isDark) MbdDarkColorScheme else MbdLightColorScheme,
        typography = Typography,
        content = content
    )
}
