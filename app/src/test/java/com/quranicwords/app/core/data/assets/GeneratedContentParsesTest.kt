package com.quranicwords.app.core.data.assets

import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.util.AppJson
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Decodes every bundled content JSON asset through the real kotlinx.serialization types
 * ContentSeeder uses, catching shape mismatches (missing/misnamed fields) that a plain JSON
 * structural check can't - especially important for the vocabulary content, which was generated
 * by an offline script rather than hand-authored.
 */
class GeneratedContentParsesTest {

    private val assetsDir = File("src/main/assets/content")

    private fun readAsset(name: String): String = File(assetsDir, name).readText()

    @Test
    fun `modules json decodes and vocabulary is implemented`() {
        val file = AppJson.decodeFromString<ModulesFile>(readAsset("modules.json"))
        assertEquals(1, file.modules.size)
        val vocab = file.modules.first { it.id == "module_vocabulary" }
        assertTrue(vocab.isImplemented)
    }

    @Test
    fun `vocabulary lessons decode with correct count`() {
        val lessons = AppJson.decodeFromString<LessonsFile>(readAsset("lessons_vocabulary.json"))
        assertEquals(368, lessons.lessons.size)
        assertTrue(lessons.lessons.all { it.moduleId == "module_vocabulary" })
    }

    @Test
    fun `vocabulary exercises decode and every teach step is a WordIntro`() {
        val exercises = AppJson.decodeFromString<ExercisesFile>(readAsset("exercises_vocabulary.json"))
        assertEquals(8096, exercises.exercises.size)

        val entities = exercises.exercises.map { it.toEntity() }
        assertEquals(8096, entities.size)

        val teachCount = exercises.exercises.count { it.content is ExerciseContent.WordIntro }
        assertEquals(3680, teachCount)

        val quizCount = exercises.exercises.count { it.content is ExerciseContent.MultipleChoice }
        assertEquals(3680, quizCount)

        val matchCount = exercises.exercises.count { it.content is ExerciseContent.Matching }
        assertEquals(736, matchCount)
    }

    @Test
    fun `word frequency file decodes with correct count and unique ranks`() {
        val file = AppJson.decodeFromString<WordFrequencyFile>(readAsset("word_frequency.json"))
        assertEquals(3680, file.words.size)
        val ranks = file.words.map { it.frequencyRank }.toSet()
        assertEquals(3680, ranks.size)
    }
}
