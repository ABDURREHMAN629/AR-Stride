package com.stride.fit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Colour is used as a code throughout the app, so the same hue always means the
 * same thing: mint for steps, blue for running, amber for strength, violet for posture.
 */
object StrideColors {
    val Steps = Color(0xFF22D3A5)
    val Run = Color(0xFF4C8DFF)
    val Strength = Color(0xFFFF9A4C)
    val Posture = Color(0xFFA78BFA)
    val Warning = Color(0xFFF87171)
    val Rejected = Color(0xFF6B7A8C)

    val DarkBackground = Color(0xFF0B0F14)
    val DarkSurface = Color(0xFF141B24)
    val DarkSurfaceHigh = Color(0xFF1D2733)
    val DarkOutline = Color(0xFF2C3846)
    val DarkOnSurface = Color(0xFFE6EDF3)
    val DarkMuted = Color(0xFF8B9AAB)

    val LightBackground = Color(0xFFF6F8FA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceHigh = Color(0xFFEDF1F5)
    val LightOutline = Color(0xFFD5DDE5)
    val LightOnSurface = Color(0xFF10161D)
    val LightMuted = Color(0xFF5C6B7A)
}

private val DarkScheme = darkColorScheme(
    primary = StrideColors.Steps,
    onPrimary = Color(0xFF00281D),
    primaryContainer = Color(0xFF10352B),
    onPrimaryContainer = StrideColors.Steps,
    secondary = StrideColors.Run,
    onSecondary = Color(0xFF001A3D),
    secondaryContainer = Color(0xFF16294A),
    onSecondaryContainer = StrideColors.Run,
    tertiary = StrideColors.Strength,
    onTertiary = Color(0xFF3A1B00),
    tertiaryContainer = Color(0xFF3D2716),
    onTertiaryContainer = StrideColors.Strength,
    background = StrideColors.DarkBackground,
    onBackground = StrideColors.DarkOnSurface,
    surface = StrideColors.DarkSurface,
    onSurface = StrideColors.DarkOnSurface,
    surfaceVariant = StrideColors.DarkSurfaceHigh,
    onSurfaceVariant = StrideColors.DarkMuted,
    outline = StrideColors.DarkOutline,
    outlineVariant = Color(0xFF232E3B),
    error = StrideColors.Warning,
    onError = Color(0xFF3A0A0A)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0E9E7C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3F5EA),
    onPrimaryContainer = Color(0xFF00382A),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE7FF),
    onSecondaryContainer = Color(0xFF0B2C6B),
    tertiary = Color(0xFFD97430),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE2CE),
    onTertiaryContainer = Color(0xFF562100),
    background = StrideColors.LightBackground,
    onBackground = StrideColors.LightOnSurface,
    surface = StrideColors.LightSurface,
    onSurface = StrideColors.LightOnSurface,
    surfaceVariant = StrideColors.LightSurfaceHigh,
    onSurfaceVariant = StrideColors.LightMuted,
    outline = StrideColors.LightOutline,
    outlineVariant = Color(0xFFE2E8EE),
    error = Color(0xFFD03A3A),
    onError = Color.White
)

private val StrideTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp
    )
)

@Composable
fun StrideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = StrideTypography,
        content = content
    )
}

/** Consistent spacing scale, so padding is never picked at random. */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}
