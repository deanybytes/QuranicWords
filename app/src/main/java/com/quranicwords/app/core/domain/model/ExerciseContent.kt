package com.quranicwords.app.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The full content of one exercise, shaped differently per [ExerciseType]. Stored as a single
 * JSON blob in [com.quranicwords.app.core.data.local.entity.ExerciseEntity] since
 * the shape genuinely varies by type rather than sharing a fixed set of columns.
 */
@Serializable
sealed interface ExerciseContent {
    val prompt: LocalizedText

    @Serializable
    @SerialName("multiple_choice")
    data class MultipleChoice(
        override val prompt: LocalizedText,
        val promptArabic: String? = null,
        override val options: List<ChoiceOption>,
        override val correctOptionId: String
    ) : OptionsBearing {
        override fun withOptions(newOptions: List<ChoiceOption>): ExerciseContent = copy(options = newOptions)
    }

    @Serializable
    @SerialName("tap_what_you_hear")
    data class TapWhatYouHear(
        override val prompt: LocalizedText,
        val audioAssetPath: String,
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
     * A non-scored teach step shown before a word's quiz exercises. [meaningReviewed] tracks,
     * per language tag, whether that language's [meaning] entry has been independently verified
     * against a real source rather than AI-drafted (see docs/CONTENT_SOURCES.md) - a tag missing
     * from this map means "not yet verified", the same honest default the old `meaningBnReviewed`
     * flag used. This is tracked in the data rather than silently presented as verified; it is
     * deliberately not surfaced as an in-lesson warning (see Settings > About instead).
     */
    @Serializable
    @SerialName("word_intro")
    data class WordIntro(
        override val prompt: LocalizedText,
        val wordId: String,
        val arabicWord: String,
        val meaning: LocalizedText,
        val meaningReviewed: Map<String, Boolean> = emptyMap(),
        val root: String? = null,
        val exampleVerseArabic: String,
        val exampleVerseTranslation: LocalizedText,
        val exampleVerseReference: String,
        // True when [exampleVerseArabic] was confirmed to literally contain [arabicWord]'s exact
        // surface form (tools/ingestion/15_reverify_example_verses.py's word-boundary-aware
        // search), false when no such occurrence exists anywhere in the Quran and the verse shown
        // is still the older root-derived "illustrates the concept" fallback - never silently
        // presented as an exact citation. See docs/CONTENT_SOURCES.md.
        val exampleVerseVerified: Boolean = false,
        val audioAssetPath: String? = null,
        // Char offsets of [arabicWord]'s occurrence within [exampleVerseArabic] (original string,
        // diacritics included), null when the ingestion pipeline's diacritic-normalized matcher
        // couldn't confidently locate it - never guessed. See tools/ingestion/10_add_highlight_spans.py.
        val arabicWordStart: Int? = null,
        val arabicWordEnd: Int? = null,
        // Best-effort literal substrings of the translation fields corresponding to [meaning] -
        // null (per-language, via LocalizedText.getOrNull) when no confident match was found
        // (translations are idiomatic full sentences, not word-aligned, so this is deliberately
        // partial rather than fabricated).
        val meaningHighlight: LocalizedText = emptyMap()
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
        val wordId: String,
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
     * short phrase. Correctness is a chip-id sequence match, not an option pick - see
     * `LessonViewModel.onCheckPressed`'s dedicated branch.
     *
     * Deliberately has no verse-reference field (unlike [FillInTheBlank.sentenceReference]):
     * these phrases are pedagogically-constructed practice strings built around [wordId], not
     * excerpts of a specific ayah, so a reference field would imply a citation this type doesn't
     * make. If a future content pass wants real verse-excerpt phrases instead, add a reference
     * field at that point rather than treating the current shape as citable.
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
     * Plays [audioAssetPath], the learner types the transliteration. [acceptedAnswers] covers
     * reasonable alternate spellings; comparison is trimmed + case-insensitive (see
     * `LessonViewModel.onCheckPressed`). Ships fully wired but is filtered out of a lesson's
     * exercise list entirely when the audio asset isn't bundled (see `LessonViewModel.init`) -
     * no content is generated for this type yet (a pre-existing, acknowledged content gap), so
     * this type is invisible until that changes, same pattern as the Lottie/Rive assets in the
     * UI-motion pass.
     */
    @Serializable
    @SerialName("listen_and_type")
    data class ListenAndType(
        override val prompt: LocalizedText,
        val wordId: String,
        val audioAssetPath: String,
        val correctAnswer: String,
        val acceptedAnswers: List<String> = emptyList()
    ) : ExerciseContent

    /**
     * The "reverse direction" quiz: [meaning] is shown as the prompt (instead of the Arabic
     * word), and the learner taps the matching word directly inside [verseArabic] - every
     * whitespace-delimited word in the verse is one entry in [tappableSpans], so the UI can
     * render each as its own tappable region rather than only the correct one (otherwise this
     * isn't really a "find the word" interaction). [correctWordStart]/[correctWordEnd] identify
     * which span is correct; one-shot like [MultipleChoice] (no retry within the same exercise
     * instance) for consistent scoring semantics across types.
     */
    @Serializable
    @SerialName("word_in_verse_tap")
    data class TapWordInVerse(
        override val prompt: LocalizedText,
        val wordId: String,
        val verseArabic: String,
        val verseReference: String,
        val correctWordStart: Int,
        val correctWordEnd: Int,
        val tappableSpans: List<WordSpan>,
        val meaning: LocalizedText
    ) : ExerciseContent
}

/** A char `[start, end)` range within [ExerciseContent.TapWordInVerse.verseArabic] - a plain
 * serializable pair rather than [IntRange], which kotlinx.serialization doesn't support out of
 * the box. */
@Serializable
data class WordSpan(val start: Int, val end: Int)

/** Shared shape for the three quiz types whose distractor options get regenerated at runtime
 * (see `LessonViewModel.regenerateDistractors`) - lets that call site handle all three with one
 * branch instead of one per concrete type, and keeps growing to a fourth options-bearing type a
 * one-line addition instead of a new branch to remember everywhere. [withOptions] exists because
 * `copy()` isn't part of the interface contract - each implementer forwards to its own `copy`. */
sealed interface OptionsBearing : ExerciseContent {
    val options: List<ChoiceOption>
    val correctOptionId: String
    fun withOptions(newOptions: List<ChoiceOption>): ExerciseContent
}

/** Teach steps ([ExerciseContent.WordIntro]) are instructional, not quizzed - they must be
 * excluded from a lesson's scored total or the perfect-lesson bonus becomes unreachable.
 * Exhaustive `when` (not a negative `!is` check) so adding a future teach type forces an explicit
 * scoring decision instead of silently defaulting to "scored". */
val ExerciseContent.isScored: Boolean
    get() = when (this) {
        is ExerciseContent.WordIntro -> false
        is ExerciseContent.MultipleChoice, is ExerciseContent.TapWhatYouHear, is ExerciseContent.Matching,
        is ExerciseContent.FillInTheBlank, is ExerciseContent.WordOrderBuilder, is ExerciseContent.ListenAndType,
        is ExerciseContent.TapWordInVerse -> true
    }

/**
 * The id of the single word this exercise quizzes, for attempt logging - `correctOptionId`
 * doubles as the practiced item's id for [ExerciseContent.MultipleChoice]/[ExerciseContent.TapWhatYouHear].
 * `null` for teach steps (nothing scored yet) and for [ExerciseContent.Matching], which quizzes
 * multiple pairs in one exercise and is logged per-pair at the call site instead (see
 * `LessonViewModel.selectMatchingRight`) rather than through this single-id extension.
 */
fun ExerciseContent.practicedItemId(): String? = when (this) {
    is ExerciseContent.MultipleChoice -> correctOptionId
    is ExerciseContent.TapWhatYouHear -> correctOptionId
    is ExerciseContent.FillInTheBlank -> wordId
    is ExerciseContent.WordOrderBuilder -> wordId
    is ExerciseContent.ListenAndType -> wordId
    is ExerciseContent.TapWordInVerse -> wordId
    is ExerciseContent.Matching, is ExerciseContent.WordIntro -> null
}

@Serializable
data class ChoiceOption(
    val id: String,
    val labelArabic: String? = null,
    val label: LocalizedText = emptyMap()
)

@Serializable
data class MatchPair(
    /** Lesson-scoped presentation id ("p1", "p2", ...) - NOT globally unique, and NOT a
     * [com.quranicwords.app.core.data.local.entity.WordFrequencyEntity] id. Every
     * Matching exercise's pairs restart numbering from p1, so this must never be used as an
     * attempt-log itemId (it would collide across lessons and never match any other exercise's
     * `practicedItemId`) - see [wordId] for that. */
    val id: String,
    val leftArabic: String,
    val right: LocalizedText,
    /** The actual word this pair quizzes, for attempt logging - nullable/defaulted since existing
     * authored content predates this field; a null here means the pair's match can't be
     * attributed to a specific word yet (see `LessonViewModel.selectMatchingRight`, which skips
     * logging rather than risk logging under the colliding [id] instead). */
    val wordId: String? = null
)

fun ExerciseContent.localizedPrompt(language: Language): String = prompt.get(language)
fun ChoiceOption.localizedLabel(language: Language): String =
    label.getOrNull(language) ?: labelArabic.orEmpty()
fun MatchPair.localizedRight(language: Language): String = right.get(language)
