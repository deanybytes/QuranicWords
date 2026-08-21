package com.quranicwords.app.core.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.QuranFontStyle

/**
 * Maps a [QuranFontStyle] to the actual bundled typeface. Only styles backed by a genuinely
 * open-licensed (SIL OFL) font - see app/src/main/assets/font_licenses/ - are bundled; the rest
 * fall back to the system default Arabic font until their real licensed file is sourced.
 */
fun QuranFontStyle.toFontFamily(): FontFamily = when (fontKey) {
    "amiri" -> FontFamily(Font(R.font.amiri_regular))
    "scheherazade" -> FontFamily(Font(R.font.scheherazade_regular))
    "noto_naskh" -> FontFamily(Font(R.font.noto_naskh_regular))
    "lateef" -> FontFamily(Font(R.font.lateef_regular))
    "noto_nastaliq_urdu" -> FontFamily(Font(R.font.noto_nastaliq_urdu_regular))
    else -> FontFamily.Default
}
