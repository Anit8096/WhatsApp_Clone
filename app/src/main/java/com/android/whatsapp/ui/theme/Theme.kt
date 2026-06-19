package com.android.whatsapp.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary             = Green500,
    onPrimary           = Color.Black,
    primaryContainer    = BubbleOut,
    onPrimaryContainer  = TextPrimary,
    secondary           = TealAccent,
    onSecondary         = Color.Black,
    secondaryContainer  = Surface700,
    onSecondaryContainer = TextPrimary,
    tertiary            = Green700,
    onTertiary          = Color.White,
    tertiaryContainer   = Surface700,
    onTertiaryContainer = TextPrimary,
    background          = Surface900,
    onBackground        = TextPrimary,
    surface             = Surface900,
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
    primaryContainer    = BubbleOutLight,
    onPrimaryContainer  = TextPrimaryLight,
    secondary           = TealAccent,
    onSecondary         = Color.Black,
    secondaryContainer  = Color(0xFFE7FCE3),
    onSecondaryContainer = TextPrimaryLight,
    tertiary            = Green900,
    onTertiary          = Color.White,
    tertiaryContainer   = Surface700Light,
    onTertiaryContainer = TextPrimaryLight,
    background          = Surface900Light,
    onBackground        = TextPrimaryLight,
    surface             = Surface900Light,
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

    WhatsAppSystemBars(useDark = useDark)

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = WhatsAppTypography,
            content     = content
        )
    }
}

@Composable
private fun WhatsAppSystemBars(useDark: Boolean) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        DisposableEffect(useDark, view) {
            val window = view.context.findActivity()?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !useDark
                    isAppearanceLightNavigationBars = !useDark
                }
            }
            onDispose { }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
