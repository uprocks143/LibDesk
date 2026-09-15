package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalIsDarkTheme = compositionLocalOf { false }

// =========================================================================
// LibDesk Vibrant Colorful Modern Design System — Material Design 3 Light Theme
// =========================================================================
private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldPrimaryLight,
    onPrimaryContainer = EmeraldPrimaryDark,
    
    secondary = LibDeskSecondary,
    onSecondary = Color.White,
    secondaryContainer = LibDeskSecondaryLight,
    onSecondaryContainer = Color(0xFF92400E),
    
    tertiary = LibDeskTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF5B21B6),
    
    background = EmeraldBackground,
    onBackground = EmeraldPrimaryText,
    
    surface = EmeraldSurface,
    onSurface = EmeraldPrimaryText,
    
    surfaceVariant = EmeraldSurfaceSoft,
    onSurfaceVariant = EmeraldSecondaryText,
    
    outline = EmeraldDivider,
    outlineVariant = EmeraldDivider,
    
    error = EmeraldError,
    onError = Color.White,
    errorContainer = EmeraldErrorSoft,
    onErrorContainer = EmeraldError
)

// =========================================================================
// LibDesk Vibrant Colorful Modern Design System — Material Design 3 Dark Theme
// =========================================================================
private val DarkColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = EmeraldDarkBackground,
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = EmeraldDarkPrimaryText,
    
    secondary = EmeraldDarkWarning,
    onSecondary = EmeraldDarkBackground,
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    
    tertiary = Color(0xFFA78BFA),
    onTertiary = EmeraldDarkBackground,
    tertiaryContainer = Color(0xFF4C1D95),
    onTertiaryContainer = Color(0xFFEDE9FE),
    
    background = EmeraldDarkBackground,
    onBackground = EmeraldDarkPrimaryText,
    
    surface = EmeraldDarkSurface,
    onSurface = EmeraldDarkPrimaryText,
    
    surfaceVariant = EmeraldDarkSurfaceElevated,
    onSurfaceVariant = EmeraldDarkSecondaryText,
    
    outline = EmeraldDarkDivider,
    outlineVariant = EmeraldDarkDivider,
    
    error = EmeraldDarkError,
    onError = EmeraldDarkBackground,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA)
)

/**
 * LibDeskTheme with Material Design 3 and dynamic color support for Android 12+.
 */
@Composable
fun LibDeskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Semantic (success/warning/info) colors that are NOT part of Material3's
 * ColorScheme roles but still need to flip between light/dark like everything
 * else. Previously screens referenced fixed constants like `EmeraldSuccess`
 * directly — those never changed in dark mode, which is why some screens
 * looked broken/inconsistent after toggling the theme. Use these instead of
 * the raw Emerald*/Sleek*/Oxford* constants from Color.kt in any @Composable.
 */
object LibDeskColors {
    val success: Color @Composable get() = if (LocalIsDarkTheme.current) EmeraldDarkSuccess else EmeraldSuccess
    val warning: Color @Composable get() = if (LocalIsDarkTheme.current) EmeraldDarkWarning else EmeraldWarning
    val info: Color @Composable get() = if (LocalIsDarkTheme.current) EmeraldDarkPrimary else EmeraldInfo
    val successSoft: Color @Composable get() = if (LocalIsDarkTheme.current) EmeraldDarkSurfaceElevated else EmeraldSuccessSoft
    val warningSoft: Color @Composable get() = if (LocalIsDarkTheme.current) EmeraldDarkSurfaceElevated else EmeraldWarningSoft
    val infoSoft: Color @Composable get() = if (LocalIsDarkTheme.current) EmeraldDarkSurfaceElevated else EmeraldInfoSoft
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    LibDeskTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
