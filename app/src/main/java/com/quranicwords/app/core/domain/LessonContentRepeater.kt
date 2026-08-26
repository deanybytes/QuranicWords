package com.quranicwords.app.core.domain

import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.isScored
import com.quranicwords.app.core.domain.model.practicedItemId
import kotlin.random.Random

/**
 * Applies the learner's chosen [com.quranicwords.app.core.domain.model.LearningStyle] repeat
 * count to a lesson's decoded exercise list - a pure runtime multiplier on top of whatever the
 * content pipeline authored, not a change to the baked content JSON itself.
 *
 * Repetitions are distributed and interleaved across the session so that the same word does NOT
 * repeat multiple times in a row ("at a go"). Instead:
 * - For lessons with teach steps ([ExerciseContent.WordIntro]), each word is introduced and
 *   immediately tested once (retrieval practice).
 * - Subsequent repetitions (rounds 2 through [repeatCount]) are distributed across review rounds,
 *   with each round containing one repetition per word in randomized order.
 * - Words are shuffled within each round such that adjacent exercises never quiz the same word
 *   (including across round boundaries, whenever there are at least 2 distinct words).
 * - Per word: cycles through that word's existing scored exercise variants (typically multiple-choice
 *   + tap-in-verse) to reach [repeatCount] total reps.
 * - Matching exercises aren't tied to a single word - the whole Matching set is repeated
 *   [repeatCount] times as a unit at the end of the lesson.
 * - Teach steps ([ExerciseContent.WordIntro]) are never repeated.
 *
 * Deliberately operates on decoded, pre-distractor-regeneration content (call this before
 * `LessonViewModel.regenerateDistractors`, not after) - each repeated instance then gets its own
 * independent distractor/option-shuffle pass, so repeats don't render as pixel-identical screens.
 */
object LessonContentRepeater {
    fun apply(
        contents: List<ExerciseContent>,
        repeatCount: Int,
        random: Random = Random.Default
    ): List<ExerciseContent> {
        if (contents.isEmpty()) return emptyList()

        val safeRepeatCount = repeatCount.coerceAtLeast(1)
        val wordGroups: Map<String, List<ExerciseContent>> = contents
            .filter { it.isScored && it !is ExerciseContent.Matching }
            .groupBy { it.practicedItemId() ?: "" }
        val matchingExercises = contents.filterIsInstance<ExerciseContent.Matching>()
        val hasWordIntros = contents.any { it is ExerciseContent.WordIntro }

        // Ordered list of unique word IDs as they first appear in contents
        val distinctWordIds = mutableListOf<String>()
        for (content in contents) {
            val wordId = when (content) {
                is ExerciseContent.WordIntro -> content.wordId
                else -> if (content.isScored && content !is ExerciseContent.Matching) content.practicedItemId() else null
            }
            if (wordId != null && wordId !in distinctWordIds && wordGroups[wordId]?.isNotEmpty() == true) {
                distinctWordIds += wordId
            }
        }

        val output = mutableListOf<ExerciseContent>()

        if (hasWordIntros) {
            // Phase 1: Teach and initial quiz (rep index 0) for each word
            val round0EmittedWordIds = mutableSetOf<String>()
            for (content in contents) {
                when {
                    content is ExerciseContent.Matching -> Unit // Handled at the end
                    content is ExerciseContent.WordIntro -> {
                        output += content
                        val firstQuiz = wordGroups[content.wordId]?.firstOrNull()
                        if (firstQuiz != null && round0EmittedWordIds.add(content.wordId)) {
                            output += firstQuiz
                        }
                    }
                    content.isScored -> {
                        val wordId = content.practicedItemId()
                        if (wordId != null && round0EmittedWordIds.add(wordId)) {
                            val group = wordGroups[wordId].orEmpty()
                            if (group.isNotEmpty()) {
                                output += group.first()
                            }
                        }
                    }
                    else -> output += content
                }
            }

            var lastWordId = output.lastOrNull { it.isScored && it !is ExerciseContent.Matching }?.practicedItemId()

            // Phase 2: Subsequent repetition rounds (rounds 1 until safeRepeatCount)
            for (rep in 1 until safeRepeatCount) {
                val roundExercises = distinctWordIds.mapNotNull { wordId ->
                    val group = wordGroups[wordId].orEmpty()
                    if (group.isNotEmpty()) group[rep % group.size] else null
                }
                val shuffledRound = shuffleRound(roundExercises, avoidFirstWordId = lastWordId, random = random)
                output += shuffledRound
                lastWordId = shuffledRound.lastOrNull()?.practicedItemId()
            }
        } else {
            // Lessons without WordIntro (e.g. Flashback / Exam / Quiz-only)
            var lastWordId: String? = null
            for (rep in 0 until safeRepeatCount) {
                val roundExercises = distinctWordIds.mapNotNull { wordId ->
                    val group = wordGroups[wordId].orEmpty()
                    if (group.isNotEmpty()) group[rep % group.size] else null
                }
                val shuffledRound = if (rep == 0 && distinctWordIds.size <= 1) {
                    roundExercises
                } else {
                    shuffleRound(roundExercises, avoidFirstWordId = lastWordId, random = random)
                }
                output += shuffledRound
                lastWordId = shuffledRound.lastOrNull()?.practicedItemId()
            }

            // Include any pass-through non-scored / non-matching items
            for (content in contents) {
                if (!content.isScored && content !is ExerciseContent.Matching && content !is ExerciseContent.WordIntro) {
                    output += content
                }
            }
        }

        // Phase 3: Matching exercises (if any) repeated safeRepeatCount times at the end
        if (matchingExercises.isNotEmpty()) {
            repeat(safeRepeatCount) { output += matchingExercises }
        }

        return output
    }

    /**
     * Shuffles [items] (which contains one exercise per distinct word) while ensuring the first
     * item does not quiz [avoidFirstWordId] whenever there are at least 2 items in the list.
     */
    private fun shuffleRound(
        items: List<ExerciseContent>,
        avoidFirstWordId: String?,
        random: Random
    ): List<ExerciseContent> {
        if (items.size <= 1) return items
        val shuffled = items.shuffled(random).toMutableList()
        if (avoidFirstWordId != null && shuffled.first().practicedItemId() == avoidFirstWordId) {
            val swapIdx = (1 until shuffled.size).firstOrNull {
                shuffled[it].practicedItemId() != avoidFirstWordId
            }
            if (swapIdx != null) {
                val temp = shuffled[0]
                shuffled[0] = shuffled[swapIdx]
                shuffled[swapIdx] = temp
            }
        }
        return shuffled
    }
}
