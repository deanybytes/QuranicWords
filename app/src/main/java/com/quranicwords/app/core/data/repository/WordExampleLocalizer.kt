package com.quranicwords.app.core.data.repository

import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.data.local.entity.VerseEntity
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.LocalizedSense
import com.quranicwords.app.core.domain.model.LocalizedWord
import com.quranicwords.app.core.domain.model.WordSpan
import com.quranicwords.app.core.domain.model.cleanArabicDisplay
import com.quranicwords.app.core.util.HighlightUtils
import com.quranicwords.app.core.util.VerseReferenceFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single place a word's example is localized: turns a [ExerciseContent.WordIntro] plus the
 * learner's [Language] into a [LocalizedWord] - its meaning and its proven senses in that
 * language, each joined with the verse it cites (from the `verses` table). Every screen that shows
 * a word's example (teach card, quiz feedback, browse, learned words, widget) goes through here,
 * and the quiz exercises built on a word's example are rewritten from it by [localizeExercise].
 *
 * Highlights are exactly the spans the content pipeline proved - nothing is ever searched for.
 * Verses are cached in memory (bounded); content is static between reseeds and a reseed only
 * happens at startup, before any screen reads examples.
 */
@Singleton
class WordExampleLocalizer(
    private val loadVerses: suspend (Collection<String>) -> List<VerseEntity>
) {
    @Inject
    constructor(database: QwDatabase) : this({ keys -> database.verseDao().getAll(keys) })

    private val cache = object : LinkedHashMap<String, VerseEntity>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, VerseEntity>?): Boolean =
            size > MAX_CACHED_VERSES
    }

    suspend fun localize(intro: ExerciseContent.WordIntro, language: Language): LocalizedWord =
        localizeAll(listOf(intro), language).getValue(intro.wordId)

    /** wordId -> localized word for every one of [intros], loading all their verses in one go. */
    suspend fun localizeAll(intros: Collection<ExerciseContent.WordIntro>, language: Language): Map<String, LocalizedWord> {
        if (intros.isEmpty()) return emptyMap()
        val keys = intros.flatMapTo(HashSet()) { intro -> sensesFor(intro, language).second.map { it.verse } }
        val verses = versesFor(keys)
        return intros.associate { it.wordId to build(it, language, verses) }
    }

    private suspend fun versesFor(keys: Set<String>): Map<String, VerseEntity> {
        if (keys.isEmpty()) return emptyMap()
        val found = HashMap<String, VerseEntity>()
        synchronized(cache) { keys.forEach { key -> cache[key]?.let { found[key] = it } } }
        val missing = keys - found.keys
        if (missing.isNotEmpty()) {
            val loaded = loadVerses(missing)
            synchronized(cache) { loaded.forEach { cache[it.key] = it } }
            loaded.forEach { found[it.key] = it }
        }
        return found
    }

    companion object {
        private const val MAX_CACHED_VERSES = 1024

        /** The senses to show for [language]: its own, else English's - with the language their
         * texts are written in. */
        fun sensesFor(
            intro: ExerciseContent.WordIntro,
            language: Language
        ): Pair<Language, List<ExerciseContent.SenseRef>> {
            intro.senses[language.tag]?.takeIf { it.isNotEmpty() }?.let { return language to it }
            return Language.ENGLISH to intro.senses[Language.ENGLISH.tag].orEmpty()
        }

        /** Pure assembly of a [LocalizedWord] from already-loaded [verses] (keyed "surah:ayah").
         * A sense whose verse is missing, or whose word / word-by-word span does not fit its text,
         * breaks the content contract and is left out rather than shown wrongly highlighted; a
         * translation span that does not fit degrades to "no highlight". */
        fun build(
            intro: ExerciseContent.WordIntro,
            language: Language,
            verses: Map<String, VerseEntity>
        ): LocalizedWord {
            val (textLanguage, refs) = sensesFor(intro, language)
            val senses = refs.mapNotNull { ref ->
                val verse = verses[ref.verse] ?: return@mapNotNull null
                val arabic = verse.arabic
                if (!isValidSpan(ref.wordStart, ref.wordEnd, arabic.length)) return@mapNotNull null
                val wbw = verse.wordByWord[textLanguage.tag] ?: return@mapNotNull null
                if (!isValidSpan(ref.wbwStart, ref.wbwEnd, wbw.length)) return@mapNotNull null
                val translation = verse.translation[textLanguage.tag].orEmpty()
                val tStart = ref.translationStart
                val tEnd = ref.translationEnd
                val translationSpanOk = tStart != null && tEnd != null && isValidSpan(tStart, tEnd, translation.length)
                LocalizedSense(
                    meaning = ref.meaning,
                    verseKey = ref.verse,
                    // Always from the "surah:ayah" key, so surah word, name and digits are all in the
                    // learner's language - verses.json's English `ref` is never shown.
                    reference = VerseReferenceFormatter.format(ref.verse, language),
                    verseArabic = arabic,
                    wordStart = ref.wordStart,
                    wordEnd = ref.wordEnd,
                    wbwText = wbw,
                    wbwStart = ref.wbwStart,
                    wbwEnd = ref.wbwEnd,
                    translationText = translation,
                    translationStart = if (translationSpanOk) tStart else null,
                    translationEnd = if (translationSpanOk) tEnd else null,
                    textLanguage = textLanguage
                )
            }
            val meaning = intro.meaning[language.tag] ?: intro.meaning[Language.ENGLISH.tag].orEmpty()
            return LocalizedWord(wordId = intro.wordId, meaning = meaning, senses = senses)
        }

        private fun isValidSpan(start: Int, end: Int, length: Int): Boolean = start in 0 until end && end <= length

        /**
         * Rewrites the verse-based quiz types onto the word's localized main sense (the verse the
         * learner's language actually proves the word in), leaving everything else untouched:
         * - [ExerciseContent.FillInTheBlank]: sentence, blank = exactly the word's span,
         *   translation and reference from the sense; the correct option's Arabic becomes the
         *   blanked text itself, and any other option that would read identically is dropped.
         * - [ExerciseContent.TapWordInVerse]: verse, reference and translation from the sense;
         *   tappable spans re-split from that verse, the correct one being the token holding the
         *   word (widened over any further tokens the word spans).
         * A word without a localized sense keeps its English-default content.
         */
        fun localizeExercise(content: ExerciseContent, words: Map<String, LocalizedWord>): ExerciseContent = when (content) {
            is ExerciseContent.FillInTheBlank ->
                words[content.wordId]?.primarySense?.let { localizedFillInTheBlank(content, it) } ?: content
            is ExerciseContent.TapWordInVerse ->
                words[content.wordId]?.primarySense?.let { localizedTapWordInVerse(content, it) } ?: content
            else -> content
        }

        fun localizedFillInTheBlank(content: ExerciseContent.FillInTheBlank, sense: LocalizedSense): ExerciseContent.FillInTheBlank {
            val word = sense.arabicWord
            val wordKey = HighlightUtils.arabicReadingKey(word)
            val options = content.options.mapNotNull { option ->
                when {
                    option.id == content.correctOptionId -> option.copy(labelArabic = word)
                    option.labelArabic?.let { HighlightUtils.arabicReadingKey(it.cleanArabicDisplay()) } == wordKey -> null
                    else -> option
                }
            }
            return content.copy(
                sentenceArabic = sense.verseArabic,
                blankStart = sense.wordStart,
                blankEnd = sense.wordEnd,
                sentenceTranslation = mapOf(sense.textLanguage.tag to sense.translationText),
                sentenceReference = sense.verseKey,
                options = options
            )
        }

        fun localizedTapWordInVerse(content: ExerciseContent.TapWordInVerse, sense: LocalizedSense): ExerciseContent.TapWordInVerse {
            val tokens = tokenSpans(sense.verseArabic)
            val first = tokens.indexOfFirst { sense.wordStart >= it.start && sense.wordStart < it.end }
            if (first < 0) return content
            val last = tokens.indexOfLast { it.start < sense.wordEnd }.coerceAtLeast(first)
            val correct = WordSpan(tokens[first].start, tokens[last].end)
            val spans = tokens.subList(0, first) + correct + tokens.subList(last + 1, tokens.size)
            return content.copy(
                verseArabic = sense.verseArabic,
                verseReference = sense.verseKey,
                correctWordStart = correct.start,
                correctWordEnd = correct.end,
                tappableSpans = spans,
                verseTranslation = mapOf(sense.textLanguage.tag to sense.translationText)
            )
        }

        /** The verse's written tokens: the text split on single spaces (empty pieces skipped). */
        fun tokenSpans(verse: String): List<WordSpan> {
            val spans = ArrayList<WordSpan>()
            var start = 0
            while (start <= verse.length) {
                val space = verse.indexOf(' ', start).let { if (it < 0) verse.length else it }
                if (space > start) spans += WordSpan(start, space)
                start = space + 1
            }
            return spans
        }
    }
}
