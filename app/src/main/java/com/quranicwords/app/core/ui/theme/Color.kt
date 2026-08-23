package com.quranicwords.app.core.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette, derived from the app logo's actual colors - gold #ebc971, dark green #053827,
// light green #7ed957, green #00bf63, white - hand-authored so the brand identity stays
// consistent regardless of the user's wallpaper (dynamic color is deliberately disabled, see
// Theme.kt). `primary` is a deepened variant of the brand green (#00bf63 itself fails WCAG AA
// contrast with white text, ~2.4:1 - #007A42 clears 5.4:1) so filled buttons stay legible;
// #00bf63/#7ed957 still appear directly as container/dark-scheme accents where contrast allows.
// Every on*/* pair below was checked against WCAG AA (4.5:1 text) before being finalized.

/** The 5 raw logo colors, named and exposed directly (not just baked into the M3 scheme below)
 * for anything that wants the literal brand hex rather than a theme role - e.g. QwLogo's glow,
 * which cycles through these rather than using a single theme color. See docs/UI_GUIDELINES.md's
 * "Brand colors" section for the canonical record of these 5 values. */
val BrandGold = Color(0xFFEBC971)
val BrandDarkGreen = Color(0xFF053827)
val BrandLightGreen = Color(0xFF7ED957)
val BrandGreen = Color(0xFF00BF63)
val BrandWhite = Color(0xFFFFFFFF)

val md_theme_light_primary = Color(0xFF007A42)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFD3F5C4)
val md_theme_light_onPrimaryContainer = Color(0xFF053827)
val md_theme_light_secondary = Color(0xFF4C7A44)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFDCEFD2)
val md_theme_light_onSecondaryContainer = Color(0xFF1B3213)
// Tertiary is the gold family - the same accent family as StreakAccent below, used for
// premium/celebratory UI (medallions, podium framing, display-title underlines). #ebc971 is
// light enough that it needs a dark (not white) on-color, unlike the darker gold this replaces.
val md_theme_light_tertiary = Color(0xFFEBC971)
val md_theme_light_onTertiary = Color(0xFF053827)
val md_theme_light_tertiaryContainer = Color(0xFFFBEFD1)
val md_theme_light_onTertiaryContainer = Color(0xFF053827)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFFBF6EC)
val md_theme_light_onBackground = Color(0xFF053827)
val md_theme_light_surface = Color(0xFFFBF6EC)
val md_theme_light_onSurface = Color(0xFF053827)
val md_theme_light_surfaceVariant = Color(0xFFEAE4D2)
val md_theme_light_onSurfaceVariant = Color(0xFF3F4A3F)
val md_theme_light_outline = Color(0xFF74806F)
// M3's surface-container family (NavigationBar, Card, BottomSheet, menu backgrounds, ...) falls
// back to a neutral gray baseline if left unset - these keep it in the cream/green brand family
// instead, since a plain lightColorScheme()/darkColorScheme() call never derives them from the
// primary/surface colors given above.
val md_theme_light_surfaceDim = Color(0xFFDCD6C8)
val md_theme_light_surfaceBright = Color(0xFFFBF6EC)
val md_theme_light_surfaceContainerLowest = Color(0xFFFFFFFF)
val md_theme_light_surfaceContainerLow = Color(0xFFF5EFE2)
val md_theme_light_surfaceContainer = Color(0xFFEFE9DB)
val md_theme_light_surfaceContainerHigh = Color(0xFFE9E3D5)
val md_theme_light_surfaceContainerHighest = Color(0xFFE3DDCE)
val md_theme_light_inverseSurface = Color(0xFF053827)
val md_theme_light_inverseOnSurface = Color(0xFFF2F7F0)
val md_theme_light_inversePrimary = Color(0xFF7ED957)

// Dark background/surface is the brand dark green itself (#053827) - it already equals
// colors.xml's ic_launcher_background, so dark mode now literally matches the launcher icon.
val md_theme_dark_primary = Color(0xFF7ED957)
val md_theme_dark_onPrimary = Color(0xFF053827)
val md_theme_dark_primaryContainer = Color(0xFF0B5A38)
val md_theme_dark_onPrimaryContainer = Color(0xFFCFF5DC)
val md_theme_dark_secondary = Color(0xFFA9D9A0)
val md_theme_dark_onSecondary = Color(0xFF1B3213)
val md_theme_dark_secondaryContainer = Color(0xFF2E4A28)
val md_theme_dark_onSecondaryContainer = Color(0xFFD6EFC9)
val md_theme_dark_tertiary = Color(0xFFEBC971)
val md_theme_dark_onTertiary = Color(0xFF053827)
val md_theme_dark_tertiaryContainer = Color(0xFF4A3B0F)
val md_theme_dark_onTertiaryContainer = Color(0xFFFBEFD1)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Color(0xFF053827)
val md_theme_dark_onBackground = Color(0xFFF2F7F0)
val md_theme_dark_surface = Color(0xFF053827)
val md_theme_dark_onSurface = Color(0xFFF2F7F0)
val md_theme_dark_surfaceVariant = Color(0xFF1E3B2E)
val md_theme_dark_onSurfaceVariant = Color(0xFFC4D6C9)
val md_theme_dark_outline = Color(0xFF7FA08D)
// See the light-scheme comment above - same reasoning, kept in the dark green family.
val md_theme_dark_surfaceDim = Color(0xFF04291D)
val md_theme_dark_surfaceBright = Color(0xFF2B5745)
val md_theme_dark_surfaceContainerLowest = Color(0xFF021D14)
val md_theme_dark_surfaceContainerLow = Color(0xFF0A4433)
val md_theme_dark_surfaceContainer = Color(0xFF0F4C3A)
val md_theme_dark_surfaceContainerHigh = Color(0xFF1A5A46)
val md_theme_dark_surfaceContainerHighest = Color(0xFF245F4C)
val md_theme_dark_inverseSurface = Color(0xFFFBF6EC)
val md_theme_dark_inverseOnSurface = Color(0xFF053827)
val md_theme_dark_inversePrimary = Color(0xFF007A42)

/** Brand gold, used for streak flames/point badges - same family as the tertiary role above, kept
 * as a standalone token since components reference it directly rather than through the theme.
 * (QwLogo's own glow no longer uses this - it cycles through all 4 [BrandGold]/[BrandGreen]/
 * [BrandLightGreen]/[BrandDarkGreen] tones directly instead of a single fixed color.) */
val StreakAccent = BrandGold
