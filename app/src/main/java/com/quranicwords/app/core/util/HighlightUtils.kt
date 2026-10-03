package com.quranicwords.app.core.util

/**
 * Arabic text helpers. Example highlights are never searched for: they are explicit spans from
 * the content data (see `WordExampleLocalizer`). This only normalizes Arabic for deciding whether
 * two spellings would read as the same choice.
 */
object HighlightUtils {
    private val SUKUN_AND_QURANIC_MARKS = Regex("[\u0652\u06D6-\u06ED\uFEFF\u0640]")

    /**
     * How [text] reads to a learner: vowels (fatha/kasra/damma, tanwin, shadda) are kept - مَن and
     * مِن stay different - while sukun (plain or Uthmani ۡ), tatweel and Qur'anic annotation marks
     * are dropped and alif variants unified, so the Uthmani مَن and the dictionary مَنْ compare equal.
     */
    fun arabicReadingKey(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return text
            .replace(SUKUN_AND_QURANIC_MARKS, "")
            .replace('ٱ', 'ا')
            .trim()
    }
}
