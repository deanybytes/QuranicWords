package com.quranicwords.app.core.domain

import com.quranicwords.app.core.domain.model.ChoiceOption
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.MatchPair
import com.quranicwords.app.core.domain.model.practicedItemId
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonContentRepeaterTest {

    private fun teach(wordId: String) = ExerciseContent.WordIntro(
        prompt = mapOf("en" to "Meet a new word"),
        wordId = wordId,
        arabicWord = wordId,
        meaning = mapOf("en" to wordId),
        exampleVerseArabic = wordId,
        exampleVerseTranslation = mapOf("en" to wordId),
        exampleVerseReference = "1:1"
    )

    private fun multipleChoice(wordId: String) = ExerciseContent.MultipleChoice(
        prompt = mapOf("en" to "Tap the meaning"),
        wordId = wordId,
        options = listOf(ChoiceOption(id = wordId)),
        correctOptionId = wordId
    )

    private fun tapInVerse(wordId: String) = ExerciseContent.TapWordInVerse(
        prompt = mapOf("en" to "Tap the word"),
        wordId = wordId,
        verseArabic = wordId,
        verseReference = "1:1",
        correctWordStart = 0,
        correctWordEnd = 1,
        tappableSpans = emptyList(),
        meaning = mapOf("en" to wordId)
    )

    private fun matching(id: String) = ExerciseContent.Matching(
        prompt = mapOf("en" to "Match the pairs"),
        pairs = listOf(MatchPair(id = "p1", leftArabic = id, right = mapOf("en" to id), wordId = id))
    )

    @Test
    fun `SHARP keeps exactly one exercise per word - the first authored variant`() {
        val contents = listOf(teach("w1"), multipleChoice("w1"), tapInVerse("w1"))

        val result = LessonContentRepeater.apply(contents, repeatCount = 1)

        val scored = result.filter { it !is ExerciseContent.WordIntro }
        assertEquals(1, scored.size)
        assertTrue(scored.first() is ExerciseContent.MultipleChoice)
    }

    @Test
    fun `SLOW cycles through a word's exercise variants to reach 3 reps`() {
        val contents = listOf(teach("w1"), multipleChoice("w1"), tapInVerse("w1"))

        val result = LessonContentRepeater.apply(contents, repeatCount = 3)

        val scored = result.filter { it !is ExerciseContent.WordIntro }
        assertEquals(3, scored.size)
        // Cycles: MC, TapInVerse, MC (wraps back to the first variant for the 3rd rep).
        assertTrue(scored[0] is ExerciseContent.MultipleChoice)
        assertTrue(scored[1] is ExerciseContent.TapWordInVerse)
        assertTrue(scored[2] is ExerciseContent.MultipleChoice)
    }

    @Test
    fun `COZY reaches 5 reps for a word with only 2 authored variants`() {
        val contents = listOf(teach("w1"), multipleChoice("w1"), tapInVerse("w1"))

        val result = LessonContentRepeater.apply(contents, repeatCount = 5)

        val scored = result.filter { it !is ExerciseContent.WordIntro }
        assertEquals(5, scored.size)
    }

    @Test
    fun `teach steps are never repeated regardless of repeat count`() {
        val contents = listOf(teach("w1"), multipleChoice("w1"))

        val result = LessonContentRepeater.apply(contents, repeatCount = 5)

        assertEquals(1, result.count { it is ExerciseContent.WordIntro })
    }

    @Test
    fun `repeated exercises for multiple words are interleaved so no word repeats consecutively`() {
        val contents = listOf(
            teach("w1"), multipleChoice("w1"), tapInVerse("w1"),
            teach("w2"), multipleChoice("w2"), tapInVerse("w2"),
            teach("w3"), multipleChoice("w3"), tapInVerse("w3"),
            teach("w4"), multipleChoice("w4"), tapInVerse("w4")
        )

        // Run multiple times with different seeds to verify no consecutive repeats occur under any permutation
        for (seed in 1..20) {
            val result = LessonContentRepeater.apply(contents, repeatCount = 3, random = Random(seed))

            // Every word is practiced 3 times
            for (id in listOf("w1", "w2", "w3", "w4")) {
                assertEquals("Word $id should be practiced 3 times", 3, result.count { it.practicedItemId() == id })
            }

            // Scored exercises should never have the same word ID adjacent to each other
            val scored = result.filter { it !is ExerciseContent.WordIntro }
            for (i in 0 until scored.size - 1) {
                assertNotEquals(
                    "Adjacent scored exercises at $i and ${i + 1} must not quiz the same word (seed=$seed)",
                    scored[i].practicedItemId(),
                    scored[i + 1].practicedItemId()
                )
            }
        }
    }

    @Test
    fun `repeat round boundary avoids the word tested last in the previous round`() {
        val contents = listOf(
            teach("w1"), multipleChoice("w1"),
            teach("w2"), multipleChoice("w2")
        )

        // With 2 words (w1, w2), the intro phase ends on w2.
        // Round 1 must start with w1 and end on w2.
        // Round 2 must start with w1 and end on w2.
        val result = LessonContentRepeater.apply(contents, repeatCount = 3)
        val scoredWordIds = result.mapNotNull { it.practicedItemId() }
        assertEquals(listOf("w1", "w2", "w1", "w2", "w1", "w2"), scoredWordIds)
    }

    @Test
    fun `matching exercises repeat as a whole cycle, not grouped per word`() {
        val contents = listOf(matching("m1"), matching("m2"))

        val result = LessonContentRepeater.apply(contents, repeatCount = 3)

        assertEquals(6, result.filterIsInstance<ExerciseContent.Matching>().size)
    }

    @Test
    fun `a repeat count under 1 is coerced to at least 1`() {
        val contents = listOf(teach("w1"), multipleChoice("w1"))

        val result = LessonContentRepeater.apply(contents, repeatCount = 0)

        assertEquals(1, result.count { it !is ExerciseContent.WordIntro })
    }

    @Test
    fun `word order in the output follows first-appearance order from the input`() {
        val contents = listOf(
            teach("w1"), multipleChoice("w1"),
            teach("w2"), multipleChoice("w2")
        )

        val result = LessonContentRepeater.apply(contents, repeatCount = 1)

        assertEquals(listOf("w1", "w1", "w2", "w2"), result.mapNotNull { it.practicedItemId() ?: (it as? ExerciseContent.WordIntro)?.wordId })
    }

    @Test
    fun `quiz-only lesson without teach steps interleaves repetitions without consecutive duplicates`() {
        val contents = listOf(
            multipleChoice("w1"),
            multipleChoice("w2"),
            multipleChoice("w3")
        )

        for (seed in 1..10) {
            val result = LessonContentRepeater.apply(contents, repeatCount = 5, random = Random(seed))
            for (id in listOf("w1", "w2", "w3")) {
                assertEquals(5, result.count { it.practicedItemId() == id })
            }

            for (i in 0 until result.size - 1) {
                assertNotEquals(
                    "Adjacent exercises at $i and ${i + 1} must not quiz same word",
                    result[i].practicedItemId(),
                    result[i + 1].practicedItemId()
                )
            }
        }
    }
}
