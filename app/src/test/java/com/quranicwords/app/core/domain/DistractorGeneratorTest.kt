package com.quranicwords.app.core.domain

import com.quranicwords.app.core.data.local.entity.WordFrequencyEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DistractorGeneratorTest {

    private fun word(id: String, rank: Int, tier: Int = 2) = WordFrequencyEntity(
        id = id, arabicWord = id, frequencyRank = rank, frequencyCount = 0,
        meaning = mapOf("en" to id, "bn" to id), audioAssetPath = null, tierLevel = tier
    )

    private fun pool(vararg words: WordFrequencyEntity) = WordCandidatePool.from(words.toList())

    @Test
    fun `picks the closest siblings by frequency rank within the same tier`() {
        val candidates = pool(
            word("correct", rank = 100),
            word("near1", rank = 99),
            word("near2", rank = 102),
            word("near3", rank = 105),
            word("far", rank = 900),
            word("otherTier", rank = 101, tier = 3)
        )
        val distractors = DistractorGenerator.pickDistractors("correct", candidates, emptySet())

        assertEquals(3, distractors.size)
        assertEquals(setOf("near1", "near2", "near3"), distractors.toSet())
        assertFalse("otherTier" in distractors)
        assertFalse("far" in distractors)
    }

    @Test
    fun `never includes the correct answer itself`() {
        val candidates = pool(word("correct", rank = 1), word("sibling", rank = 2))
        val distractors = DistractorGenerator.pickDistractors("correct", candidates, emptySet())
        assertFalse("correct" in distractors)
    }

    @Test
    fun `returns fewer than count when too few siblings exist - caller is responsible for topping up`() {
        val candidates = pool(word("correct", rank = 1), word("only-sibling", rank = 2))
        val distractors = DistractorGenerator.pickDistractors("correct", candidates, emptySet())
        assertEquals(1, distractors.size)
        assertTrue("only-sibling" in distractors)
    }

    @Test
    fun `unknown correctId yields no distractors`() {
        val candidates = pool(word("a", rank = 1), word("b", rank = 2))
        assertTrue(DistractorGenerator.pickDistractors("missing", candidates, emptySet()).isEmpty())
    }

    @Test
    fun `ties on rank distance prefer previously-missed items`() {
        // "left" and "right" are equidistant from "correct" (rank 50) - "left" was missed before.
        val candidates = pool(
            word("correct", rank = 50),
            word("left", rank = 49),
            word("right", rank = 51)
        )
        val distractors = DistractorGenerator.pickDistractors(
            "correct", candidates, missedItemIds = setOf("left"), count = 1
        )
        assertEquals(listOf("left"), distractors)
    }

    @Test
    fun `only searches within the window around the correct item's rank position`() {
        // "correct" sits at list-position 0 after sorting by rank; "justOutside" is more than
        // WINDOW_RADIUS (60) rank-sorted positions away and must not be picked even though
        // nothing else is closer.
        val words = mutableListOf(word("correct", rank = 0))
        repeat(70) { i -> words += word("filler$i", rank = i + 1) }
        words += word("justOutside", rank = 1000)
        val distractors = DistractorGenerator.pickDistractors("correct", pool(*words.toTypedArray()), emptySet(), count = 100)
        assertFalse("justOutside" in distractors)
    }

    @Test
    fun `pickMatchingDistractor excludes every id in the used set, not just the anchor`() {
        val candidates = pool(
            word("p1", rank = 100),
            word("p2", rank = 101),
            word("p3", rank = 102),
            word("extra", rank = 103)
        )
        val distractor = DistractorGenerator.pickMatchingDistractor(
            usedWordIds = setOf("p1", "p2", "p3"),
            pool = candidates,
            missedItemIds = emptySet()
        )
        assertEquals("extra", distractor)
    }

    @Test
    fun `pickMatchingDistractor returns null when no candidate exists outside the used set`() {
        val candidates = pool(word("p1", rank = 1), word("p2", rank = 2))
        val distractor = DistractorGenerator.pickMatchingDistractor(
            usedWordIds = setOf("p1", "p2"),
            pool = candidates,
            missedItemIds = emptySet()
        )
        assertEquals(null, distractor)
    }

    @Test
    fun `pickMatchingDistractor returns null for an empty used set - legacy content with no wordIds`() {
        val candidates = pool(word("p1", rank = 1), word("p2", rank = 2))
        val distractor = DistractorGenerator.pickMatchingDistractor(
            usedWordIds = emptySet(),
            pool = candidates,
            missedItemIds = emptySet()
        )
        assertEquals(null, distractor)
    }
}
