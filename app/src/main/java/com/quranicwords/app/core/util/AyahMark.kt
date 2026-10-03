package com.quranicwords.app.core.util

import com.quranicwords.app.core.domain.model.QuranFontStyle

/**
 * The end-of-ayah mark shown after a verse, with the ayah number in Arabic-Indic digits, as in a
 * printed mushaf. The four naskh typefaces draw U+06DD ۝ around the following digits; the KFGQPC
 * Uthmanic Hafs and the two Indo-Pak fonts have no such enclosing form (an empty circle beside the
 * digits), so they use the ornate brackets ﴿٣٤﴾ that they do draw. Verified by rendering every
 * bundled font.
 */
object AyahMark {
    private const val ARABIC_INDIC = "٠١٢٣٤٥٦٧٨٩"

    fun digits(number: Int): String = number.toString().map { ARABIC_INDIC[it - '0'] }.joinToString("")

    /** The mark for [ayah] (1-based), preceded by a no-break space so it never wraps alone. */
    fun of(ayah: Int, font: QuranFontStyle): String =
        if (font == QuranFontStyle.MADANI_KFGQPC || font.indoPak) " ﴿${digits(ayah)}﴾" else " ۝${digits(ayah)}"

    /** The ayah number of a "surah:ayah" key, or null. */
    fun ayahOf(verseKey: String?): Int? = verseKey?.substringAfter(':', "")?.toIntOrNull()?.takeIf { it > 0 }
}
