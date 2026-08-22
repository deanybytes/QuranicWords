package com.quranicwords.app.core.domain

import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.domain.model.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenPracticePoolTest {

    private fun exercise(id: String, practicedItemId: String?) = ExerciseEntity(
        id = id,
        lessonId = "lesson_1",
        orderIndex = 0,
        type = ExerciseType.MULTIPLE_CHOICE,
        contentJson = "{}",
        practicedItemId = practicedItemId
    )

    // --- sampleIds ---

    @Test
    fun `sampleIds returns exactly count entries when the pool is larger`() {
        val pool = (1..50).map { "word_$it" }
        val result = OpenPracticePool.sampleIds(pool, count = 18)
        assertEquals(18, result.size)
        assertTrue(pool.containsAll(result))
    }

    @Test
    fun `sampleIds returns every entry, not more, when the pool is smaller than count`() {
        val pool = listOf("word_1", "word_2", "word_3")
        val result = OpenPracticePool.sampleIds(pool, count = 18)
        assertEquals(pool.toSet(), result.toSet())
    }

    @Test
    fun `sampleIds returns every entry when the pool exactly matches count`() {
        val pool = (1..18).map { "word_$it" }
        val result = OpenPracticePool.sampleIds(pool, count = 18)
        assertEquals(pool.toSet(), result.toSet())
    }

    @Test
    fun `sampleIds on an empty pool is empty`() {
        assertEquals(emptyList<String>(), OpenPracticePool.sampleIds(emptyList(), count = 18))
    }

    // --- oneExercisePerWord ---

    @Test
    fun `oneExercisePerWord collapses multiple rows for the same word down to one`() {
        val exercises = listOf(
            exercise("ex1", "word_a"),
            exercise("ex2", "word_a"),
            exercise("ex3", "word_b")
        )
        val result = OpenPracticePool.oneExercisePerWord(exercises)
        assertEquals(2, result.size)
        assertEquals(setOf("word_a", "word_b"), result.map { it.practicedItemId }.toSet())
    }

    @Test
    fun `oneExercisePerWord passes through one row per word unchanged in count`() {
        val exercises = listOf(exercise("ex1", "word_a"), exercise("ex2", "word_b"), exercise("ex3", "word_c"))
        val result = OpenPracticePool.oneExercisePerWord(exercises)
        assertEquals(3, result.size)
    }

    @Test
    fun `oneExercisePerWord on an empty list is empty`() {
        assertEquals(emptyList<ExerciseEntity>(), OpenPracticePool.oneExercisePerWord(emptyList()))
    }
}
