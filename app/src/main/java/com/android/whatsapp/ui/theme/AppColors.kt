package com.android.whatsapp.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Holds all theme-dependent colors. Screens read these via LocalAppColors.current
// instead of the old top-level vals, so they automatically respond to
// dark/light/system changes without each screen needing isSystemInDarkTheme() checks.
data class AppColors(
    val surface900   : Color,
    val surface800   : Color,
    val surface700   : Color,
    val surface600   : Color,
    val bubbleOut    : Color,
    val bubbleIn     : Color,
    val textPrimary  : Color,
    val textSecondary: Color,
    val textTertiary : Color,
    val dividerColor : Color,
    val onlineGreen  : Color,
    val errorRed     : Color,
    val readBlue     : Color,
    val unreadBadge  : Color,
    val isDark       : Boolean
)

val DarkAppColors = AppColors(
    surface900    = Surface900,
    surface800    = Surface800,
    surface700    = Surface700,
    surface600    = Surface600,
    bubbleOut     = BubbleOut,
    bubbleIn      = BubbleIn,
    textPrimary   = TextPrimary,
    textSecondary = TextSecondary,
    textTertiary  = TextTertiary,
    dividerColor  = DividerColor,
    onlineGreen   = OnlineGreen,
    errorRed      = ErrorRed,
    readBlue      = ReadBlue,
    unreadBadge   = UnreadBadge,
    isDark        = true
)

val LightAppColors = AppColors(
    surface900    = Surface900Light,
    surface800    = Surface800Light,
    surface700    = Surface700Light,
    surface600    = Surface600Light,
    bubbleOut     = BubbleOutLight,
    bubbleIn      = BubbleInLight,
    textPrimary   = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textTertiary  = TextTertiaryLight,
    dividerColor  = DividerColorLight,
    onlineGreen   = OnlineGreenLight,
    errorRed      = ErrorRedLight,
    readBlue      = ReadBlueLight,
    unreadBadge   = UnreadBadgeLight,
    isDark        = false
)

val LocalAppColors = compositionLocalOf { DarkAppColors }