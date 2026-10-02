package com.quranicwords.app.core.util

/**
 * Diacritic-insensitive matching for Arabic search boxes: a learner typing "كتب" must find
 * "كَتَبَ". Comparison only - displayed text always keeps its full tashkīl (see
 * docs/UI_GUIDELINES.md on verse/diacritic fidelity).
 */
object ArabicSearch {

    /** Strips harakat, tanwīn, shadda, sukūn, dagger alif, Quranic annotation marks and tatweel,
     * and folds the alif variants (أ إ آ ٱ) to bare alif, then lowercases what's left. */
    fun normalize(text: String): String {
        val out = StringBuilder(text.length)
        for (ch in text) {
            when {
                isMark(ch) -> Unit
                ch in ALIF_VARIANTS -> out.append('ا')
                else -> out.append(ch.lowercaseChar())
            }
        }
        return out.toString().trim()
    }

    /** True when [query] (normalized) occurs anywhere in [text] (normalized). Blank queries match. */
    fun matches(text: String?, query: String): Boolean {
        val needle = normalize(query)
        if (needle.isEmpty()) return true
        return text != null && normalize(text).contains(needle)
    }

    private fun isMark(ch: Char): Boolean {
        val c = ch.code
        return c in 0x0610..0x061A ||
            c in 0x064B..0x065F ||
            c == 0x0670 ||
            c == 0x0640 ||
            c in 0x06D6..0x06DC ||
            c in 0x06DF..0x06E8 ||
            c in 0x06EA..0x06ED
    }

    private val ALIF_VARIANTS = setOf('آ', 'أ', 'إ', 'ٱ')
}
