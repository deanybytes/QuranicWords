package com.quranicwords.app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

/** Base Material 3 type scale - the system font already covers Latin, Bangla, and Arabic shaping
 * correctly, so body/label styles stay on the system default (Arabic *content* fonts are handled
 * separately, see core/ui/theme/QuranFont.kt). */
private val baseTypography = Typography()

/**
 * Headline/title styles are set to [FontFamily.Serif] (the guaranteed system generic serif, no
 * bundled font file required) to give screen titles and ceremonial moments a distinct "manuscript
 * display" feel versus the plain body text. Swap this for a licensed display font (e.g. Marcellus,
 * Cormorant, Spectral) once one is bundled under res/font/ - same drop-in-asset pattern already
 * used for the Arabic content fonts in QuranFont.kt.
 */
val QwTypography = baseTypography.copy(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = FontFamily.Serif),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = FontFamily.Serif),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = FontFamily.Serif),
    headlineLarge = baseTypography.headlineLarge.copy(fontFamily = FontFamily.Serif),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = FontFamily.Serif),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = FontFamily.Serif),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = FontFamily.Serif)
)
