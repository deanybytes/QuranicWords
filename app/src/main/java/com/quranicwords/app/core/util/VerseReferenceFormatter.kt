package com.quranicwords.app.core.util

import com.quranicwords.app.core.domain.model.Language
import java.text.NumberFormat

/**
 * The single place a Qur'an verse citation is rendered: `<Surah word> <surah name> <s:v>`, with
 * the surah word, name and digits localized for the learner's [Language]. Every screen and widget
 * shows citations through [format] so the pattern is identical everywhere.
 *
 * For example, "Al-Baqarah 2:22" (or "Surah 2:22", or "2:22") becomes:
 * - ENGLISH -> "Surah Al-Baqarah 2:22"
 * - BANGLA  -> "সূরা আল-বাকারা ২:২২"
 * - URDU    -> "سورۃ البقرۃ ۲:۲۲"
 */
object VerseReferenceFormatter {

    private val BANGLA_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    private val ARABIC_INDIC_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val DEVANAGARI_DIGITS = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')

    private val VERSE_REF_REGEX = Regex("""(\d+)\s*:\s*(\d+)""")

    fun format(reference: String?, language: Language): String {
        if (reference.isNullOrBlank()) return ""

        val match = VERSE_REF_REGEX.find(reference)
        if (match != null) {
            val surahNum = match.groupValues[1].toIntOrNull()
            val verseNum = match.groupValues[2]
            if (surahNum != null && surahNum in 1..114) {
                val surahName = SurahNames.getSurahName(surahNum, language)
                val localizedSurah = formatDigits(surahNum.toString(), language)
                val localizedVerse = formatDigits(verseNum, language)
                val name = if (surahName.isNotBlank()) "$surahName " else ""
                return "${surahWord(language)} $name$localizedSurah:$localizedVerse"
            }
        }

        val cleaned = reference
            .replace("(?i)surah".toRegex(), "")
            .replace("সূরা", "")
            .replace("سورۃ", "")
            .replace("سورة", "")
            .trim()

        return formatDigits(cleaned, language)
    }

    /** The word for "Surah" in each language, placed before the surah's name. */
    fun surahWord(language: Language): String = when (language) {
        Language.BANGLA -> "সূরা"
        Language.URDU -> "سورۃ"
        Language.HINDI -> "सूरह"
        Language.PERSIAN -> "سوره"
        Language.TURKISH -> "Sure"
        Language.FRENCH -> "Sourate"
        Language.SWAHILI -> "Sura"
        else -> "Surah"
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

    /** A whole number with the language's own grouping separators (via [NumberFormat]) and then
     * its native digits (via [formatDigits]) - the one way counts are shown to the learner. */
    fun formatNumber(value: Long, language: Language): String =
        formatDigits(NumberFormat.getIntegerInstance(language.locale).format(value), language)

    fun formatNumber(value: Int, language: Language): String = formatNumber(value.toLong(), language)

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
