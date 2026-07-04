package com.bytethrux.loadr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** User-selectable theme preference. [SYSTEM] follows the phone setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK;

    companion object {
        fun fromString(value: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}

@Composable
fun LoadrTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val loadrColors = if (darkTheme) DarkLoadrColors else LightLoadrColors

    val materialScheme = if (darkTheme) {
        darkColorScheme(
            primary = loadrColors.green,
            onPrimary = loadrColors.onGreen,
            background = loadrColors.background,
            onBackground = loadrColors.textPrimary,
            surface = loadrColors.background,
            onSurface = loadrColors.textPrimary,
            surfaceVariant = loadrColors.surface,
            onSurfaceVariant = loadrColors.slate,
            error = loadrColors.red,
        )
    } else {
        lightColorScheme(
            primary = loadrColors.green,
            onPrimary = loadrColors.onGreen,
            background = loadrColors.background,
            onBackground = loadrColors.textPrimary,
            surface = loadrColors.background,
            onSurface = loadrColors.textPrimary,
            surfaceVariant = loadrColors.surface,
            onSurfaceVariant = loadrColors.slate,
            error = loadrColors.red,
        )
    }

    CompositionLocalProvider(LocalLoadrColors provides loadrColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = Typography(),
            content = content
        )
    }
}
