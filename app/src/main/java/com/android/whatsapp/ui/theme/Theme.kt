package com.android.whatsapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary          = Green500,
    onPrimary        = Color.Black,
    primaryContainer = Green900,
    onPrimaryContainer = TextPrimary,

    secondary        = TealAccent,
    onSecondary      = Color.Black,

    background       = Surface900,
    onBackground     = TextPrimary,

    surface          = Surface800,
    onSurface        = TextPrimary,
    surfaceVariant   = Surface700,
    onSurfaceVariant = TextSecondary,

    outline          = Surface600,
    outlineVariant   = DividerColor,

    error            = ErrorRed,
    onError          = Color.White,
)

@Composable
fun WhatsAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = WhatsAppTypography,
        content     = content
    )
}