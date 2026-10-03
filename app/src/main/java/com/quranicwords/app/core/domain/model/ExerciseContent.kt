package com.quranicwords.app.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The full content of one exercise, shaped differently per [ExerciseType]. Stored as a single
 * JSON blob in [com.quranicwords.app.core.data.local.entity.ExerciseEntity] since
 * the shape genuinely varies by type rather than sharing a fixed set of columns.
 *
 * The listening types ("tap_what_you_hear", "listen_and_type") were withdrawn along with word
 * pronunciation audio; a stored blob of either no longer decodes, and every reader goes through
 * [com.quranicwords.app.core.util.decodeExerciseContentOrNull] so such a row is skipped.
 */
@Serializable
sealed interface ExerciseContent {
    val prompt: LocalizedText

    @Serializable
    @SerialName("multiple_choice")
    data class MultipleChoice(
        override val prompt: LocalizedText,
        val promptArabic: String? = null,
        override val wordId: String,
        override val options: List<ChoiceOption>,
        override val correctOptionId: String
    ) : OptionsBearing {
        override fun withOptions(newOptions: List<ChoiceOption>): ExerciseContent = copy(options = newOptions)
    }

    @Serializable
    @SerialName("matching")
    data class Matching(
        override val prompt: LocalizedText,
        val pairs: List<MatchPair>,
        /** An extra, never-matchable meaning-side option - runtime-regenerated per lesson entry
         * (see `LessonViewModel.regenerateDistractors`), same "not baked into content JSON"
         * discipline as [OptionsBearing]'s options. `null` for legacy content whose pairs carry no
         * [MatchPair.wordId] to search a distractor around, or if content is too sparse to find
         * one - the extra tile simply doesn't render in that case. */
        val distractorRight: ChoiceOption? = null
    ) : ExerciseContent

    /**
     * One proven sense of a word in one language: [meaning] is exactly the word-by-word gloss of
     * the word in verse [verse] ("surah:ayah", a key of `content/verses.json`). Every span is a
     * char `[start, end)` range validated by tools/pipeline:
     * - `arabic[wordStart, wordEnd)` is exactly the taught word (the segment inside its written
     *   token, e.g. مَا inside وَمَا), [word] being that token's 1-based index;
     * - `wordByWord[lang][wbwStart, wbwEnd)` equals [meaning];
     * - when [translationStart] is non-null, `translation[lang][translationStart, translationEnd)`
     *   equals [meaning] ignoring case and occurs once; when null the translation is shown with
     *   no highlight at all.
     * Displayed only through `WordExampleLocalizer`, never by searching for substrings.
     */
    @Serializable
    data class SenseRef(
        val meaning: String,
        val verse: String,
        val word: Int,
        val wordStart: Int,
        val wordEnd: Int,
        val wbwStart: Int,
        val wbwEnd: Int,
        val translationStart: Int? = null,
        val translationEnd: Int? = null
    )

    /**
     * A non-scored teach step shown before a word's quiz exercises.
     */
    @Serializable
    @SerialName("word_intro")
    data class WordIntro(
        override val prompt: LocalizedText,
        val wordId: String,
        val arabicWord: String,
        val meaning: LocalizedText,
        val lemmaCategory: LemmaCategory = LemmaCategory.NOUN,
        /** Language tag -> this word's senses in that language (1-3, in teaching order);
         * `meaning[lang]` is their meanings joined with " / ". */
        val senses: Map<String, List<SenseRef>> = emptyMap(),
        val meaningReviewed: Map<String, Boolean> = emptyMap(),
        val root: String? = null,
        // Legacy English-default example (sense 1 of `senses["en"]`), kept for the generated
        // Open Practice exercises - every displayed example comes from [senses] instead.
        val exampleVerseArabic: String? = null,
        val exampleVerseTranslation: LocalizedText = emptyMap(),
        val exampleVerseReference: String? = null,
        val exampleVerseVerified: Boolean = false,
        val arabicWordStart: Int? = null,
        val arabicWordEnd: Int? = null,
        val meaningHighlight: LocalizedText = emptyMap(),
        val verbForm: String? = null,
        val pastArabic: String? = null,
        val presentArabic: String? = null,
        val masdarArabic: String? = null,
        val particleType: String? = null,
        val grammaticalCategory: String? = null,
        val partOfSpeechDetail: String? = null,
        /** [partOfSpeechDetail] in every content language (e.g. "حرفِ عطف" for Urdu). */
        val partOfSpeechLabel: LocalizedText = emptyMap(),
        /** [verbForm] in every content language (e.g. "Forme I (فَعَلَ)" for French). */
        val verbFormLabel: LocalizedText = emptyMap()
    ) : ExerciseContent

    /**
     * A non-scored chapter overview shown as the first lesson of every chapter.
     */
    @Serializable
    @SerialName("chapter_intro")
    data class ChapterIntro(
        override val prompt: LocalizedText = emptyMap(),
        val chapterId: String,
        val chapterNumber: Int,
        val chapterTitle: LocalizedText = emptyMap(),
        val chapterDescription: LocalizedText = emptyMap(),
        val wordCount: Int = 0,
        val quranOccurrenceCount: Int = 0,
        val chapterCoveragePercent: Float = 0f,
        val accumulatedCoveragePercent: Float = 0f,
        val accumulatedWords: Int = 0,
        val nounCount: Int = 0,
        val verbCount: Int = 0,
        val particleCount: Int = 0,
        val learningObjectives: LocalizedText = emptyMap()
    ) : ExerciseContent

    /**
     * Blanks [wordId]'s own occurrence (at [blankStart]/[blankEnd]) out of [sentenceArabic] -
     * authored from the same source data as [WordIntro] (its example verse + highlight span),
     * just packaged as its own quiz type rather than reusing WordIntro's fields directly, since
     * the two serve different roles (teach step vs. scored quiz). [options]/[correctOptionId]
     * follow the same shape as [MultipleChoice] and go through the same runtime distractor
     * regeneration (see `LessonViewModel.rebuildOptions`).
     */
    @Serializable
    @SerialName("fill_in_the_blank")
    data class FillInTheBlank(
        override val prompt: LocalizedText,
        override val wordId: String,
        val sentenceArabic: String,
        val blankStart: Int,
        val blankEnd: Int,
        val sentenceTranslation: LocalizedText,
        /** Sura:ayah this sentence is quoted from - every verse shown anywhere in the app must
         * carry its reference (see the verse-reference completeness pass this field closed a gap
         * in; previously this type showed quoted Qur'anic text with no citation at all). */
        val sentenceReference: String,
        override val options: List<ChoiceOption>,
        override val correctOptionId: String
    ) : OptionsBearing {
        override fun withOptions(newOptions: List<ChoiceOption>): ExerciseContent = copy(options = newOptions)
    }

    /** One chip in a [WordOrderBuilder] - [id] is stable/unique even when two chips show the
     * same Arabic text, so a submitted order can be compared unambiguously. */
    @Serializable
    data class WordChip(val id: String, val arabicText: String)

    /**
     * Arrange [orderedChips] (shown shuffled by the UI) into their authored order to build a
     * complete Quranic phrase.
     */
    @Serializable
    @SerialName("word_order")
    data class WordOrderBuilder(
        override val prompt: LocalizedText,
        val wordId: String,
        val orderedChips: List<WordChip>,
        val translation: LocalizedText
    ) : ExerciseContent

    /**
     * The "reverse direction" quiz: [meaning] is shown as the prompt (instead of the Arabic
     * word), and the learner taps the matching word directly inside [verseArabic].
     */
    @Serializable
    @SerialName("tap_word_in_verse")
    data class TapWordInVerse(
        override val prompt: LocalizedText,
        val wordId: String,
        val verseArabic: String,
        val verseReference: String,
        val correctWordStart: Int,
        val correctWordEnd: Int,
        val tappableSpans: List<WordSpan>,
        val meaning: LocalizedText,
        val verseTranslation: LocalizedText = emptyMap()
    ) : ExerciseContent
}

/** A char `[start, end)` range within [ExerciseContent.TapWordInVerse.verseArabic] - a plain
 * serializable pair rather than [IntRange], which kotlinx.serialization doesn't support out of
 * the box. */
@Serializable
data class WordSpan(val start: Int, val end: Int)

/** Common interface for exercises that bear an options list whose distractor candidates are
 * regenerated at runtime via `LessonViewModel.rebuildOptions` - lets [OptionsBearing] be handled
 * polymorphically in that pipeline rather than branching per concrete type; new options-bearing
 * types (e.g. [ExerciseContent.FillInTheBlank] when added alongside [ExerciseContent.MultipleChoice]) are a
 * one-line addition instead of a new branch to remember everywhere. [withOptions] exists because
 * `copy()` isn't part of the interface contract - each implementer forwards to its own `copy`. */
sealed interface OptionsBearing : ExerciseContent {
    val wordId: String
    val options: List<ChoiceOption>
    val correctOptionId: String
    fun withOptions(newOptions: List<ChoiceOption>): ExerciseContent
}

/** Teach steps ([ExerciseContent.WordIntro], [ExerciseContent.ChapterIntro]) are instructional, not quizzed - they must be
 * excluded from a lesson's scored total or the perfect-lesson bonus becomes unreachable.
 * Exhaustive `when` (not a negative `!is` check) so adding a future teach type forces an explicit
 * scoring decision instead of silently defaulting to "scored". */
val ExerciseContent.isScored: Boolean
    get() = when (this) {
        is ExerciseContent.WordIntro, is ExerciseContent.ChapterIntro -> false
        is ExerciseContent.MultipleChoice, is ExerciseContent.Matching,
        is ExerciseContent.FillInTheBlank, is ExerciseContent.WordOrderBuilder,
        is ExerciseContent.TapWordInVerse -> true
    }

/**
 * The id of the single word this exercise quizzes, for attempt logging - [OptionsBearing.wordId]
 * for [ExerciseContent.MultipleChoice]/[ExerciseContent.FillInTheBlank] (NOT `correctOptionId`,
 * which is only a per-exercise-local option id like "o3" and would fragment attempt logging for
 * the same word across different exercises that happen to number their baked options
 * differently). `null` for teach steps (nothing scored yet) and for [ExerciseContent.Matching],
 * which quizzes multiple pairs in one exercise and is logged per-pair at the call site instead
 * (see `LessonViewModel.selectMatchingRight`) rather than through this single-id extension.
 */
fun ExerciseContent.practicedItemId(): String? = when (this) {
    is ExerciseContent.MultipleChoice -> wordId
    is ExerciseContent.FillInTheBlank -> wordId
    is ExerciseContent.WordOrderBuilder -> wordId
    is ExerciseContent.TapWordInVerse -> wordId
    is ExerciseContent.Matching, is ExerciseContent.WordIntro, is ExerciseContent.ChapterIntro -> null
}

@Serializable
data class ChoiceOption(
    val id: String,
    val labelArabic: String? = null,
    val label: LocalizedText = emptyMap()
)

@Serializable
data class MatchPair(
    val id: String = "",
    val leftArabic: String = "",
    val left: String? = null,
    val right: LocalizedText = emptyMap(),
    val wordId: String? = null
) {
    val effectiveLeftArabic: String get() = leftArabic.ifBlank { left.orEmpty() }.cleanArabicDisplay()
    val effectiveId: String get() = id.ifBlank { wordId.orEmpty() }
}

private val LEMMA_ID_REGEX = Regex("""\s*\(\d+\)""")

fun String.cleanArabicDisplay(): String = this.replace(LEMMA_ID_REGEX, "").trim()

fun ExerciseContent.localizedPrompt(language: Language): String = prompt.get(language)

/** The verb form ("Form IV" ...) in the learner's language, falling back to the English string. */
fun ExerciseContent.WordIntro.localizedVerbForm(language: Language): String? =
    verbFormLabel.getOrNull(language)?.takeIf { it.isNotBlank() } ?: verbForm

/** The part of speech ("Noun (Ism)" ...) in the learner's language, falling back to the English string. */
fun ExerciseContent.WordIntro.localizedPartOfSpeech(language: Language): String? =
    partOfSpeechLabel.getOrNull(language)?.takeIf { it.isNotBlank() } ?: partOfSpeechDetail
fun ChoiceOption.localizedLabel(language: Language): String =
    label.getOrNull(language) ?: labelArabic.orEmpty().cleanArabicDisplay()
fun MatchPair.localizedRight(language: Language): String = right.get(language)
