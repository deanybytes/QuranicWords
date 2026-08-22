package com.quranicwords.app.core.domain

import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.isScored
import com.quranicwords.app.core.domain.model.practicedItemId

/**
 * Applies the learner's chosen [com.quranicwords.app.core.domain.model.LearningStyle] repeat
 * count to a lesson's decoded exercise list - a pure runtime multiplier on top of whatever the
 * content pipeline authored, not a change to the baked content JSON itself.
 *
 * Per word (grouped by [ExerciseContent.practicedItemId]): cycles through that word's existing
 * scored exercise variants (typically multiple-choice + tap-in-verse) to reach [repeatCount]
 * total reps, wrapping around when [repeatCount] exceeds the number of authored variants (so
 * SLOW/COZY repeat a word's *existing* exercise types rather than needing new ones). SHARP
 * (repeatCount = 1) keeps only the first variant, i.e. genuinely quizzes each word once - fewer
 * exercises than today's un-repeated default, which is the whole point of a "sharp" pass.
 *
 * Matching exercises aren't tied to a single word (they quiz several pairs at once, the closest
 * thing this app has to a lesson-ending "exam" moment) - the *whole* existing Matching set is
 * treated as one cycle and repeated [repeatCount] times as a unit, rather than grouped per word.
 *
 * Teach steps ([ExerciseContent.WordIntro]) are never repeated - repeating "meet a new word"
 * three times would teach nothing extra, only the quiz that follows benefits from repetition.
 *
 * Deliberately operates on *decoded, pre-distractor-regeneration* content (call this before
 * `LessonViewModel.regenerateDistractors`, not after) - each repeated instance then gets its own
 * independent distractor/option-shuffle pass, so repeats don't render as pixel-identical screens.
 * The underlying distractor *set* is still deterministic (see `DistractorGenerator`'s own doc
 * comment), so a word repeated via the same exercise type twice will show the same options in a
 * different order, not entirely different options - an accepted, documented limitation rather
 * than a hidden one.
 */
object LessonContentRepeater {
    fun apply(contents: List<ExerciseContent>, repeatCount: Int): List<ExerciseContent> {
        val safeRepeatCount = repeatCount.coerceAtLeast(1)
        val wordGroups: Map<String, List<ExerciseContent>> = contents
            .filter { it.isScored && it !is ExerciseContent.Matching }
            .groupBy { it.practicedItemId() ?: "" }
        val matchingExercises = contents.filterIsInstance<ExerciseContent.Matching>()

        val output = mutableListOf<ExerciseContent>()
        val emittedWordIds = mutableSetOf<String>()
        for (content in contents) {
            when {
                content is ExerciseContent.Matching -> Unit // appended once, after this loop
                content is ExerciseContent.WordIntro -> output += content
                content.isScored -> {
                    val wordId = content.practicedItemId()
                    if (wordId != null && emittedWordIds.add(wordId)) {
                        val group = wordGroups[wordId].orEmpty()
                        if (group.isNotEmpty()) {
                            output += List(safeRepeatCount) { i -> group[i % group.size] }
                        }
                    }
                    // wordId == null (shouldn't happen for a non-Matching scored type) or already
                    // emitted for this word: skip, already handled at first occurrence.
                }
                else -> output += content
            }
        }
        if (matchingExercises.isNotEmpty()) {
            repeat(safeRepeatCount) { output += matchingExercises }
        }
        return output
    }
}
