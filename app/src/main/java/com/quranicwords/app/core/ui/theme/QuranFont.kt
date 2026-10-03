package com.quranicwords.app.core.ui.theme

import androidx.compose.runtime.Composable
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
    "amiri" -> FontFamily(Font(R.font.amiri_regular))
    "scheherazade" -> FontFamily(Font(R.font.scheherazade_regular))
    "noto_naskh" -> FontFamily(Font(R.font.noto_naskh_regular))
    "noorehuda" -> FontFamily(Font(R.font.noorehuda_regular))
    "noorehira" -> FontFamily(Font(R.font.noorehira_regular))
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

/** Qur'anic [text] as the learner's chosen font must receive it (see [QuranFontStyle.script]). */
@Composable
fun quranText(text: String): String = LocalQuranFontStyle.current.script(text)
