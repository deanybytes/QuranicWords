package com.quranicwords.app.core.util

import com.quranicwords.app.core.domain.model.Language

/**
 * Formats a Qur'an verse reference by removing the word "Surah" / "surah" / "সূরা" / "سورۃ"
 * and localizing the chapter and verse numbers according to the user's selected [Language].
 *
 * For example:
 * - "Surah 36:62" in BANGLA -> "৩৬:৬২"
 * - "Surah 36:62" in URDU -> "۳۶:۶۲"
 * - "Surah 36:62" in ENGLISH / INDONESIAN / TURKISH / FRENCH -> "36:62"
 */
object VerseReferenceFormatter {

    private val BANGLA_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    private val ARABIC_INDIC_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val DEVANAGARI_DIGITS = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')

    fun format(reference: String?, language: Language): String {
        if (reference.isNullOrBlank()) return ""

        val cleaned = reference
            .replace("(?i)surah".toRegex(), "")
            .replace("সূরা", "")
            .replace("سورۃ", "")
            .replace("سورة", "")
            .trim()

        return formatDigits(cleaned, language)
    }

    /**
     * Converts any ASCII digits in [input] to localized digits if [language] requires it.
     */
    fun formatDigits(input: String, language: Language): String {
        return when (language) {
            Language.BANGLA -> convertDigits(input, BANGLA_DIGITS)
            Language.URDU, Language.PERSIAN -> convertDigits(input, ARABIC_INDIC_DIGITS)
            Language.HINDI -> convertDigits(input, DEVANAGARI_DIGITS)
            else -> input
        }
    }

    private fun convertDigits(input: String, digitMap: CharArray): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(digitMap[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
