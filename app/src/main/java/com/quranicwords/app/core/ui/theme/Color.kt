package com.quranicwords.app.core.ui.theme

import androidx.compose.ui.graphics.Color

// "Illuminated manuscript" palette - deep emerald + warm gold + parchment cream, hand-authored so
// the brand identity stays consistent regardless of the user's wallpaper (dynamic color is
// deliberately disabled, see Theme.kt). Deepened from the prior flat forest-green scheme and
// warmed off pure white/black so surfaces read as parchment rather than generic Material default.

val md_theme_light_primary = Color(0xFF0B6E4F)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFB7F2D5)
val md_theme_light_onPrimaryContainer = Color(0xFF00210F)
val md_theme_light_secondary = Color(0xFF52634F)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFD5E8CF)
val md_theme_light_onSecondaryContainer = Color(0xFF101F0F)
// Tertiary is the gold family - the same accent family as StreakAccent below, used for
// premium/celebratory UI (medallions, podium framing, display-title underlines).
val md_theme_light_tertiary = Color(0xFF8A6D00)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFFFE08C)
val md_theme_light_onTertiaryContainer = Color(0xFF261A00)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFFBF6EC)
val md_theme_light_onBackground = Color(0xFF1F1B13)
val md_theme_light_surface = Color(0xFFFBF6EC)
val md_theme_light_onSurface = Color(0xFF1F1B13)
val md_theme_light_surfaceVariant = Color(0xFFEBE1CB)
val md_theme_light_onSurfaceVariant = Color(0xFF4B4636)
val md_theme_light_outline = Color(0xFF7C7564)

val md_theme_dark_primary = Color(0xFF7FDDB0)
val md_theme_dark_onPrimary = Color(0xFF00382A)
val md_theme_dark_primaryContainer = Color(0xFF00543D)
val md_theme_dark_onPrimaryContainer = Color(0xFFB7F2D5)
val md_theme_dark_secondary = Color(0xFFB9CCB3)
val md_theme_dark_onSecondary = Color(0xFF243422)
val md_theme_dark_secondaryContainer = Color(0xFF3A4B37)
val md_theme_dark_onSecondaryContainer = Color(0xFFD5E8CF)
val md_theme_dark_tertiary = Color(0xFFE8C36B)
val md_theme_dark_onTertiary = Color(0xFF3E2E00)
val md_theme_dark_tertiaryContainer = Color(0xFF574200)
val md_theme_dark_onTertiaryContainer = Color(0xFFFFE08C)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Color(0xFF15130D)
val md_theme_dark_onBackground = Color(0xFFE9E2D3)
val md_theme_dark_surface = Color(0xFF15130D)
val md_theme_dark_onSurface = Color(0xFFE9E2D3)
val md_theme_dark_surfaceVariant = Color(0xFF4B4636)
val md_theme_dark_onSurfaceVariant = Color(0xFFCFC6AE)
val md_theme_dark_outline = Color(0xFF988F79)

/** Warm gold used for streak flames/point badges - same family as the tertiary role above, kept
 * as a standalone token since components reference it directly rather than through the theme. */
val StreakAccent = Color(0xFFF9A825)
