package com.bytethrux.loadr.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * Full set of colour tokens used across the app. Two instances exist —
 * dark (the original navy look) and light — selected by [LoadrTheme]
 * according to the user's theme preference.
 */
@Immutable
data class LoadrColorScheme(
    val background: Color,
    val card: Color,
    val surface: Color,
    val green: Color,
    val greenDim: Color,
    val mint: Color,
    val slate: Color,
    val textPrimary: Color,
    val onGreen: Color,
    val red: Color,
    val successBg: Color,
    val errorBg: Color,
    val infoText: Color,
    val infoBorder: Color,
    val infoBg: Color,
    val drawerBg: Color,
    val navText: Color,
    val chartDot: Color,
    val isDark: Boolean,
)

val DarkLoadrColors = LoadrColorScheme(
    background = Color(0xFF0A1628),
    card = Color(0xFF111D2E),
    surface = Color(0xFF1A2E4A),
    green = Color(0xFF00C853),
    greenDim = Color(0xFF0D2818),
    mint = Color(0xFFB8F5D0),
    slate = Color(0xFF4A7C9E),
    textPrimary = Color(0xFFFFFFFF),
    onGreen = Color(0xFF0A1628),
    red = Color(0xFFFF4757),
    successBg = Color(0xFF0D2818),
    errorBg = Color(0xFF2A1018),
    infoText = Color(0xFF4A9EF5),
    infoBorder = Color(0xFF1A6FBF),
    infoBg = Color(0xFF0A1F35),
    drawerBg = Color(0xFF0D1E30),
    navText = Color(0xFFB8D0E8),
    chartDot = Color(0xFF0A1628),
    isDark = true,
)

val LightLoadrColors = LoadrColorScheme(
    background = Color(0xFFF4F8F4),
    card = Color(0xFFFFFFFF),
    surface = Color(0xFFD7E2DC),
    green = Color(0xFF00A54F),
    greenDim = Color(0xFFE3F5E9),
    mint = Color(0xFF14202E),
    slate = Color(0xFF5A7184),
    textPrimary = Color(0xFF14202E),
    onGreen = Color(0xFFFFFFFF),
    red = Color(0xFFDD3E48),
    successBg = Color(0xFFE6F6EC),
    errorBg = Color(0xFFFCE9EB),
    infoText = Color(0xFF1A6FBF),
    infoBorder = Color(0xFF7FB5E8),
    infoBg = Color(0xFFE9F2FC),
    drawerBg = Color(0xFFFFFFFF),
    navText = Color(0xFF3A4C5E),
    chartDot = Color(0xFFF4F8F4),
    isDark = false,
)

val LocalLoadrColors = staticCompositionLocalOf { DarkLoadrColors }

// Backwards-compatible named accessors. Existing screens keep referring to
// these; the values now resolve against the active theme.
val LoadrNavy: Color
    @Composable @ReadOnlyComposable get() = LocalLoadrColors.current.background
val LoadrNavyCard: Color
    @Composable @ReadOnlyComposable get() = LocalLoadrColors.current.card
val LoadrNavySurface: Color
    @Composable @ReadOnlyComposable get() = LocalLoadrColors.current.surface
val LoadrGreen: Color
    @Composable @ReadOnlyComposable get() = LocalLoadrColors.current.green
val LoadrGreenDim: Color
    @Composable @ReadOnlyComposable get() = LocalLoadrColors.current.greenDim
val LoadrMint: Color
    @Composable @ReadOnlyComposable get() = LocalLoadrColors.current.mint
val LoadrSlate: Color
    @Composable @ReadOnlyComposable get() = LocalLoadrColors.current.slate
val LoadrWhite: Color
    @Composable @ReadOnlyComposable get() = LocalLoadrColors.current.textPrimary
val LoadrOnGreen: Color
    @Composable @ReadOnlyComposable get() = LocalLoadrColors.current.onGreen
val LoadrRed: Color
    @Composable @ReadOnlyComposable get() = LocalLoadrColors.current.red
