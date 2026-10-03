package com.quranicwords.app.core.domain.model

/**
 * A word as shown to a learner in one [Language]: its meaning and its proven senses, each with a
 * complete verse example. Built only by `WordExampleLocalizer` - from the word's own
 * `senses[lang]` (falling back to English when that language has none) and the verses those
 * senses cite - so every highlight below is an explicit span from the content data, never the
 * result of a substring search.
 */
data class LocalizedWord(
    val wordId: String,
    /** `meaning[lang]`, falling back to English. */
    val meaning: String,
    val senses: List<LocalizedSense>
) {
    /** The first (main) sense - the one quiz exercises are built on. */
    val primarySense: LocalizedSense? get() = senses.firstOrNull()
}

/**
 * One sense of a word with its verse example. All ranges are char `[start, end)` offsets:
 * [wordStart]/[wordEnd] into [verseArabic], [wbwStart]/[wbwEnd] into [wbwText], and - only when
 * non-null - [translationStart]/[translationEnd] into [translationText]. A null translation span
 * means the translation is shown without any highlight.
 */
data class LocalizedSense(
    val meaning: String,
    /** "surah:ayah". */
    val verseKey: String,
    /** The citation formatted for display in the learner's language (see `VerseReferenceFormatter`). */
    val reference: String,
    val verseArabic: String,
    val wordStart: Int,
    val wordEnd: Int,
    val wbwText: String,
    val wbwStart: Int,
    val wbwEnd: Int,
    val translationText: String,
    val translationStart: Int?,
    val translationEnd: Int?,
    /** The language [meaning], [wbwText] and [translationText] are written in - the requested
     * language, or English when the word had no senses in it. Drives text direction. */
    val textLanguage: Language
) {
    /** The taught word exactly as written in this verse (e.g. مَا inside وَمَا). */
    val arabicWord: String get() = verseArabic.substring(wordStart, wordEnd)
}
