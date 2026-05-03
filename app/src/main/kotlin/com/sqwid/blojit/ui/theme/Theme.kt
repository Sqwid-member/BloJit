package com.sqwid.blojit.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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

/**
 * Material 3 color schemes. The dark scheme is the default for BloJit (per spec) — light is
 * available for users who pick it explicitly. On Android 12+ we honor system dynamic color
 * unless the user has overridden it.
 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF7DD3FC),
    onPrimary = Color(0xFF06283D),
    primaryContainer = Color(0xFF0E4F77),
    onPrimaryContainer = Color(0xFFD0EAFF),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF3A2D00),
    secondaryContainer = Color(0xFF6B4F00),
    onSecondaryContainer = Color(0xFFFFE5A6),
    tertiary = Color(0xFFA78BFA),
    onTertiary = Color(0xFF1E1240),
    background = Color(0xFF0B1020),
    onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF131A2E),
    onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF1E2742),
    onSurfaceVariant = Color(0xFFC8CFE0),
    outline = Color(0xFF3A4566),
    outlineVariant = Color(0xFF22304E),
    error = Color(0xFFF87171),
    onError = Color(0xFF330000),
    errorContainer = Color(0xFF7A0F0F),
    onErrorContainer = Color(0xFFFFD7D7),
    inverseSurface = Color(0xFFE6EAF2),
    inverseOnSurface = Color(0xFF1E2742),
    surfaceTint = Color(0xFF7DD3FC)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0EAFF),
    onPrimaryContainer = Color(0xFF002438),
    secondary = Color(0xFFB45309),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE5A6),
    onSecondaryContainer = Color(0xFF291800),
    tertiary = Color(0xFF6D28D9),
    onTertiary = Color.White,
    background = Color(0xFFF7F7FB),
    onBackground = Color(0xFF101218),
    surface = Color.White,
    onSurface = Color(0xFF101218),
    surfaceVariant = Color(0xFFE6E8F0),
    onSurfaceVariant = Color(0xFF40485A),
    outline = Color(0xFFB6BCCB),
    outlineVariant = Color(0xFFD9DCE5),
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFFD7D7),
    onErrorContainer = Color(0xFF330000),
    surfaceTint = Color(0xFF0284C7)
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

enum class ThemePreference { Dark, Light, System }

@Composable
fun BloJitTheme(
    preference: ThemePreference = ThemePreference.Dark,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val dark = when (preference) {
        ThemePreference.Dark -> true
        ThemePreference.Light -> false
        ThemePreference.System -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
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
