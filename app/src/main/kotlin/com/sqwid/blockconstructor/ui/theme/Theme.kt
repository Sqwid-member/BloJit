package com.sqwid.blockconstructor.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7DD3FC),
    onPrimary = Color(0xFF06283D),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF3A2D00),
    tertiary = Color(0xFFA78BFA),
    background = Color(0xFF0B1020),
    onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF131A2E),
    onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF1E2742),
    onSurfaceVariant = Color(0xFFC8CFE0),
    outline = Color(0xFF3A4566),
    error = Color(0xFFF87171)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    secondary = Color(0xFFB45309),
    onSecondary = Color.White,
    tertiary = Color(0xFF6D28D9),
    background = Color(0xFFF7F7FB),
    onBackground = Color(0xFF101218),
    surface = Color.White,
    onSurface = Color(0xFF101218),
    surfaceVariant = Color(0xFFE6E8F0),
    onSurfaceVariant = Color(0xFF40485A),
    outline = Color(0xFFB6BCCB),
    error = Color(0xFFB91C1C)
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 36.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp)
)

enum class ThemePreference { System, Dark, Light }

@Composable
fun BlockConstructorTheme(
    preference: ThemePreference = ThemePreference.System,
    content: @Composable () -> Unit
) {
    val dark = when (preference) {
        ThemePreference.System -> isSystemInDarkTheme()
        ThemePreference.Dark -> true
        ThemePreference.Light -> false
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
