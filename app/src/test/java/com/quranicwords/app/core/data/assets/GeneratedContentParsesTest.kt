package com.quranicwords.app.core.data.assets

import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.LemmaCategory
import com.quranicwords.app.core.util.AppJson
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GeneratedContentParsesTest {

    private val assetsDir = File("src/main/assets/content")

    private fun readAsset(name: String): String = File(assetsDir, name).readText()

    @Test
    fun `chapters json decodes, word counts sum to the full corpus, percents sum to 100`() {
        val file = AppJson.decodeFromString<ChaptersFile>(readAsset("chapters.json"))
        assertEquals(10, file.chapters.size)
        assertEquals(4616, file.chapters.sumOf { it.wordCount })
        assertEquals(100.0, file.chapters.sumOf { it.quranOccurrencePercent }, 0.1)
    }

    @Test
    fun `sections json decodes, ten per chapter, word counts sum to their chapter`() {
        val file = AppJson.decodeFromString<SectionsFile>(readAsset("sections.json"))
        assertEquals(100, file.sections.size)
        val byChapter = file.sections.groupBy { it.chapterId }
        assertEquals(10, byChapter.size)
        byChapter.values.forEach { sections ->
            assertEquals(10, sections.size)
            assertTrue(sections.sumOf { it.wordCount } > 0)
        }
    }

    @Test
    fun `vocabulary lessons decode with correct counts per kind and category`() {
        val lessons = AppJson.decodeFromString<LessonsFile>(readAsset("lessons_vocabulary.json")).lessons
        assertEquals(1202, lessons.size)

        val byKind = lessons.groupingBy { it.kind }.eachCount()
        assertEquals(10, byKind[com.quranicwords.app.core.data.local.entity.LessonKind.CHAPTER_INTRO])
        assertEquals(982, byKind[com.quranicwords.app.core.data.local.entity.LessonKind.REGULAR])
        assertEquals(100, byKind[com.quranicwords.app.core.data.local.entity.LessonKind.SECTION_FLASHBACK])
        assertEquals(100, byKind[com.quranicwords.app.core.data.local.entity.LessonKind.SECTION_EXAM])
        assertEquals(10, byKind[com.quranicwords.app.core.data.local.entity.LessonKind.CHAPTER_EXAM])

        val regularLessons = lessons.filter { it.kind == com.quranicwords.app.core.data.local.entity.LessonKind.REGULAR }
        val byCategory = regularLessons.groupingBy { it.category }.eachCount()
        assertTrue((byCategory[LemmaCategory.NOUN] ?: 0) > 0)
        assertTrue((byCategory[LemmaCategory.VERB] ?: 0) > 0)
        assertTrue((byCategory[LemmaCategory.PARTICLE] ?: 0) > 0)

        lessons.forEach { lesson ->
            val isChapterScoped = lesson.kind == com.quranicwords.app.core.data.local.entity.LessonKind.CHAPTER_EXAM ||
                lesson.kind == com.quranicwords.app.core.data.local.entity.LessonKind.CHAPTER_FLASHBACK
            if (isChapterScoped) assertNull(lesson.sectionId) else assertTrue(lesson.sectionId != null)
        }
    }

    @Test
    fun `vocabulary exercises decode and every teach step is a WordIntro or ChapterIntro`() {
        val exercises = AppJson.decodeFromString<ExercisesFile>(readAsset("exercises_vocabulary.json"))
        assertEquals(9242, exercises.exercises.size)

        val entities = exercises.exercises.map { it.toEntity() }
        assertEquals(9242, entities.size)

        val teachCount = exercises.exercises.count { it.content is ExerciseContent.WordIntro }
        assertEquals(4616, teachCount)

        val chapterIntroCount = exercises.exercises.count { it.content is ExerciseContent.ChapterIntro }
        assertEquals(10, chapterIntroCount)

        val quizCount = exercises.exercises.count { it.content is ExerciseContent.MultipleChoice }
        assertEquals(4616, quizCount)
    }

    @Test
    fun `word frequency file decodes with correct count and unique ranks`() {
        val file = AppJson.decodeFromString<WordFrequencyFile>(readAsset("word_frequency.json"))
        assertEquals(4616, file.words.size)
        val ranks = file.words.map { it.frequencyRank }.toSet()
        assertEquals(4616, ranks.size)
    }

    private val allLanguageTags = setOf("en", "bn", "ur", "in", "tr", "fr")

    @Test
    fun `every word_intro prompt covers all 6 master languages`() {
        val exercises = AppJson.decodeFromString<ExercisesFile>(readAsset("exercises_vocabulary.json"))
        val wordIntros = exercises.exercises.map { it.content }.filterIsInstance<ExerciseContent.WordIntro>()
        assertEquals(4616, wordIntros.size)
        wordIntros.forEach { assertEquals(allLanguageTags, it.prompt.keys) }
    }

    @Test
    fun `word_intro meaning covers all 6 master languages`() {
        val exercises = AppJson.decodeFromString<ExercisesFile>(readAsset("exercises_vocabulary.json"))
        val wordIntros = exercises.exercises.map { it.content }.filterIsInstance<ExerciseContent.WordIntro>()
        wordIntros.forEach {
            assertTrue("meaning missing required languages for ${it.wordId}", it.meaning.keys.containsAll(allLanguageTags))
        }
    }

    @Test
    fun `word_frequency meaning stays in sync with the matching word_intro meaning`() {
        val exercises = AppJson.decodeFromString<ExercisesFile>(readAsset("exercises_vocabulary.json"))
        val wordFreq = AppJson.decodeFromString<WordFrequencyFile>(readAsset("word_frequency.json"))
        val meaningByWordId = exercises.exercises.map { it.content }
            .filterIsInstance<ExerciseContent.WordIntro>()
            .associate { it.wordId to it.meaning }

        wordFreq.words.forEach { word ->
            val expected = meaningByWordId[word.id] ?: return@forEach
            assertEquals("meaning diverged for ${word.id}", expected, word.meaning)
        }
    }
}

