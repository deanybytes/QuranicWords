package com.quranicwords.app.core.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.QuranFontStyle

/**
 * Maps a [QuranFontStyle] to the actual bundled typeface.
 */
fun QuranFontStyle.toFontFamily(): FontFamily = when (fontKey) {
    "kfgqpc_hafs" -> FontFamily(Font(R.font.kfgqpc_hafs_regular))
    "lateef" -> FontFamily(Font(R.font.lateef_regular))
    "hafs_nastaleeq" -> FontFamily(Font(R.font.hafs_nastaleeq_regular))
    "amiri" -> FontFamily(Font(R.font.amiri_regular))
    "scheherazade" -> FontFamily(Font(R.font.scheherazade_regular))
    "kitab" -> FontFamily(Font(R.font.kitab_regular))
    "kfgqpc_warsh" -> FontFamily(Font(R.font.kfgqpc_warsh_regular))
    "kfgqpc_qaloun" -> FontFamily(Font(R.font.kfgqpc_qaloun_regular))
    "noto_naskh" -> FontFamily(Font(R.font.noto_naskh_regular))
    "noto_nastaliq_urdu" -> FontFamily(Font(R.font.noto_nastaliq_urdu_regular))
    else -> FontFamily(Font(R.font.kfgqpc_hafs_regular))
}

val DefaultQuranArabicFontFamily: FontFamily = FontFamily(Font(R.font.kfgqpc_hafs_regular))
val QuranCitationFontFamily: FontFamily = FontFamily.Serif

/**
 * CompositionLocal providing the active [FontFamily] selected by the user for all Arabic Quranic text.
 */
val LocalQuranFontFamily = compositionLocalOf { DefaultQuranArabicFontFamily }

/**
 * CompositionLocal providing the active [QuranFontStyle] selected by the user.
 */
val LocalQuranFontStyle = compositionLocalOf { QuranFontStyle.DEFAULT }
