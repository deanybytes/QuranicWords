package com.quranicwords.app.core.domain

import com.quranicwords.app.core.domain.model.ChoiceOption
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.MatchPair
import com.quranicwords.app.core.domain.model.practicedItemId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveSequencerTest {

    private fun teachStep(wordId: String) = ExerciseContent.WordIntro(
        prompt = mapOf("en" to "p", "bn" to "p"), wordId = wordId, arabicWord = wordId,
        meaning = mapOf("en" to wordId, "bn" to wordId), exampleVerseArabic = "v",
        exampleVerseTranslation = mapOf("en" to "t", "bn" to "t"), exampleVerseReference = "1:1"
    )

    private fun quizStep(wordId: String) = ExerciseContent.MultipleChoice(
        prompt = mapOf("en" to "p", "bn" to "p"), wordId = wordId,
        options = listOf(ChoiceOption(id = wordId)), correctOptionId = wordId
    )

    private fun matching(vararg pairs: String) = ExerciseContent.Matching(
        prompt = mapOf("en" to "p", "bn" to "p"),
        pairs = pairs.map { MatchPair(id = it, leftArabic = it, right = mapOf("en" to it, "bn" to it)) }
    )

    @Test
    fun `teach step always stays before its own quiz item after reordering`() {
        val contents = listOf(
            teachStep("w1"), quizStep("w1"),
            teachStep("w2"), quizStep("w2"),
            teachStep("w3"), quizStep("w3"),
            teachStep("w4"), quizStep("w4")
        )
        // w4 was missed - naively "weighting it first" without group-stability would put its quiz
        // ahead of w1/w2/w3's teach steps if done carelessly.
        val reordered = AdaptiveSequencer.reorderForAdaptivePractice(contents, missedItemIds = setOf("w4"))

        listOf("w1", "w2", "w3", "w4").forEach { wordId ->
            val teachIndex = reordered.indexOfFirst { it is ExerciseContent.WordIntro && it.wordId == wordId }
            val quizIndex = reordered.indexOfFirst { it.practicedItemId() == wordId && it is ExerciseContent.MultipleChoice }
            assertTrue("teach step for $wordId must precede its quiz", teachIndex < quizIndex)
        }
    }

    @Test
    fun `group containing a missed item moves earlier among movable groups`() {
        val contents = listOf(
            teachStep("w1"), quizStep("w1"),
            teachStep("w2"), quizStep("w2"),
            teachStep("w3"), quizStep("w3") // w3 is the missed one, originally last
        )
        val reordered = AdaptiveSequencer.reorderForAdaptivePractice(contents, missedItemIds = setOf("w3"))
        assertEquals("w3", (reordered.first() as ExerciseContent.WordIntro).wordId)
    }

    @Test
    fun `a group containing Matching is pinned - never moved even when it has no missed item`() {
        val contents = listOf(
            teachStep("w1"), quizStep("w1"),
            teachStep("w2"), quizStep("w2"), matching("w1", "w2"),
            teachStep("w3"), quizStep("w3")
        )
        val reordered = AdaptiveSequencer.reorderForAdaptivePractice(contents, missedItemIds = setOf("w3"))
        // The Matching-containing group (teach w2, quiz w2, matching) must still start right
        // after w1's pair and keep its internal order, regardless of w3 being missed.
        val matchingIndex = reordered.indexOfFirst { it is ExerciseContent.Matching }
        val w2TeachIndex = reordered.indexOfFirst { it is ExerciseContent.WordIntro && it.wordId == "w2" }
        assertEquals(w2TeachIndex + 2, matchingIndex)
    }

    @Test
    fun `no group boundary introduces a new same-exercise-type adjacency`() {
        val contents = listOf(
            teachStep("w1"), quizStep("w1"),
            teachStep("w2"), quizStep("w2"),
            teachStep("w3"), quizStep("w3")
        )
        val reordered = AdaptiveSequencer.reorderForAdaptivePractice(contents, missedItemIds = setOf("w2", "w3"))
        for (i in 0 until reordered.size - 1) {
            val currentType = reordered[i].let { if (it is ExerciseContent.WordIntro) ExerciseType.TEACH_WORD else ExerciseType.MULTIPLE_CHOICE }
            val nextType = reordered[i + 1].let { if (it is ExerciseContent.WordIntro) ExerciseType.TEACH_WORD else ExerciseType.MULTIPLE_CHOICE }
            assertTrue("adjacent items at $i/${i + 1} must differ in type", currentType != nextType)
        }
    }

    @Test
    fun `empty or unmissed input passes through unchanged`() {
        assertEquals(emptyList<ExerciseContent>(), AdaptiveSequencer.reorderForAdaptivePractice(emptyList(), setOf("x")))
        val contents = listOf(teachStep("w1"), quizStep("w1"))
        assertEquals(contents, AdaptiveSequencer.reorderForAdaptivePractice(contents, emptySet()))
    }
}
