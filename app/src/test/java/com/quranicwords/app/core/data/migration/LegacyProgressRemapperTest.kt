package com.quranicwords.app.core.data.migration

import com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.ItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyProgressRemapperTest {

    private fun lesson(id: String, kind: LessonKind = LessonKind.REGULAR) =
        LessonEntity(id, "ch_01", "s1", mapOf("en" to id), sortOrder = 0, kind = kind)

    private val lessons = listOf(
        lesson("intro", LessonKind.CHAPTER_INTRO), lesson("n1"), lesson("n2"), lesson("review", LessonKind.SECTION_FLASHBACK),
        lesson("n3"), lesson("n4")
    )
    private val lessonWords = mapOf(
        "n1" to listOf("a", "b", "c", "d", "e"),     // contains 2 words the old curriculum never taught
        "n2" to listOf("f", "g", "h", "i", "j"),
        "n3" to listOf("k", "l", "m", "n", "o"),
        "n4" to listOf("p", "q", "r", "s", "t")
    )
    private val map = LegacyContentMap(
        words = mapOf("w_1" to "c", "w_2" to "c", "w_3" to "f"),
        lessonWords = mapOf("old1" to listOf("c", "d", "e", "f", "g"), "old2" to listOf("h", "i", "j", "k"))
    )

    private fun progress(vararg ids: String) = ids.map { UserProgressEntity("u", it, LessonStatus.COMPLETED, 90, 1000L) }

    @Test
    fun `completed old lessons become a contiguous completed prefix with the next lesson unlocked`() {
        val result = LegacyProgressRemapper.remap(progress("old1", "old2"), emptyList(), map, lessons, lessonWords)
        val status = result.progress.associate { it.lessonId to it.status }
        assertEquals(LessonStatus.COMPLETED, status["intro"])
        assertEquals(LessonStatus.COMPLETED, status["n1"])     // 3/5 known: new particles don't block it
        assertEquals(LessonStatus.COMPLETED, status["n2"])
        // n3 (1/5 known) is not part of the prefix, so the section review right after n2 is next.
        assertEquals(LessonStatus.UNLOCKED, status["review"])
        assertTrue("n3" !in status && "n4" !in status)
    }

    @Test
    fun `attempts follow their word and old duplicates merge onto one id`() {
        val attempts = listOf("w_1", "w_2", "w_3", "w_unknown").mapIndexed { i, item ->
            ExerciseAttemptEntity(id = i + 1L, userId = "u", itemId = item, itemKind = ItemKind.WORD,
                exerciseType = ExerciseType.MULTIPLE_CHOICE, wasCorrect = true, attemptedAtEpochMillis = 1L)
        }
        val result = LegacyProgressRemapper.remap(emptyList(), attempts, map, lessons, lessonWords)
        assertEquals(listOf("c", "c", "f"), result.attempts.map { it.itemId })
        assertTrue(result.attempts.all { it.id == 0L })
    }

    @Test
    fun `a learner who completed nothing gets no lesson rows`() {
        val unlockedOnly = listOf(UserProgressEntity("u", "old1", LessonStatus.UNLOCKED, 0, null))
        assertTrue(LegacyProgressRemapper.remap(unlockedOnly, emptyList(), map, lessons, lessonWords).progress.isEmpty())
    }

    @Test
    fun `remapping is deterministic`() {
        val a = LegacyProgressRemapper.remap(progress("old1"), emptyList(), map, lessons, lessonWords)
        val b = LegacyProgressRemapper.remap(progress("old1"), emptyList(), map, lessons, lessonWords)
        assertEquals(a, b)
    }
}
