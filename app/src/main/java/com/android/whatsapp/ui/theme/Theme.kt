package com.android.whatsapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary             = Green500,
    onPrimary           = Color.Black,
    primaryContainer    = Green900,
    onPrimaryContainer  = TextPrimary,
    secondary           = TealAccent,
    onSecondary         = Color.Black,
    background          = Surface900,
    onBackground        = TextPrimary,
    surface             = Surface800,
    onSurface           = TextPrimary,
    surfaceVariant      = Surface700,
    onSurfaceVariant    = TextSecondary,
    outline             = Surface600,
    outlineVariant      = DividerColor,
    error               = ErrorRed,
    onError             = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary             = Green700,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFFD9FDD3),
    onPrimaryContainer  = TextPrimaryLight,
    secondary           = TealAccent,
    onSecondary         = Color.White,
    background          = Surface900Light,
    onBackground        = TextPrimaryLight,
    surface             = Surface800Light,
    onSurface           = TextPrimaryLight,
    surfaceVariant      = Surface700Light,
    onSurfaceVariant    = TextSecondaryLight,
    outline             = Surface600Light,
    outlineVariant      = DividerColorLight,
    error               = ErrorRedLight,
    onError             = Color.White,
)

// themeMode: the user's saved preference (LIGHT / DARK / SYSTEM).
// When SYSTEM, falls back to isSystemInDarkTheme().
@Composable
fun WhatsAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content  : @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK   -> true
        ThemeMode.LIGHT  -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val appColors    = if (useDark) DarkAppColors else LightAppColors
    val colorScheme  = if (useDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = WhatsAppTypography,
            content     = content
        )
    }
}