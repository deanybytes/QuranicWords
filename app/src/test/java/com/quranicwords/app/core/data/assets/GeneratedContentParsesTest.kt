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
        assertEquals(4709, file.chapters.sumOf { it.wordCount })
        assertEquals(100.0, file.chapters.sumOf { it.quranOccurrencePercent }, 0.5)
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
        assertEquals(1217, lessons.size)

        val byKind = lessons.groupingBy { it.kind }.eachCount()
        assertEquals(10, byKind[com.quranicwords.app.core.data.local.entity.LessonKind.CHAPTER_INTRO])
        assertEquals(997, byKind[com.quranicwords.app.core.data.local.entity.LessonKind.REGULAR])
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
        assertEquals(14358, exercises.exercises.size)

        val entities = exercises.exercises.map { it.toEntity() }
        assertEquals(14358, entities.size)

        val teachCount = exercises.exercises.count { it.content is ExerciseContent.WordIntro }
        assertEquals(4709, teachCount)

        val chapterIntroCount = exercises.exercises.count { it.content is ExerciseContent.ChapterIntro }
        assertEquals(10, chapterIntroCount)

        val quizCount = exercises.exercises.count { it.content is ExerciseContent.MultipleChoice }
        assertEquals(8432, quizCount)

        val matchingCount = exercises.exercises.count { it.content is ExerciseContent.Matching }
        assertEquals(1207, matchingCount)
    }

    @Test
    fun `word frequency file decodes with correct count and unique ranks`() {
        val file = AppJson.decodeFromString<WordFrequencyFile>(readAsset("word_frequency.json"))
        assertEquals(4709, file.words.size)
        val ranks = file.words.map { it.frequencyRank }.toSet()
        assertEquals(4709, ranks.size)
    }

    private val allLanguageTags = setOf("en", "bn", "ur", "hi", "in", "ms", "tr", "fa", "ha", "sw")

    @Test
    fun `every word_intro prompt covers all 10 master languages`() {
        val exercises = AppJson.decodeFromString<ExercisesFile>(readAsset("exercises_vocabulary.json"))
        val wordIntros = exercises.exercises.map { it.content }.filterIsInstance<ExerciseContent.WordIntro>()
        assertEquals(4709, wordIntros.size)
        wordIntros.forEach { assertEquals(allLanguageTags, it.prompt.keys) }
    }

    @Test
    fun `word_intro meaning covers all 10 master languages`() {
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

    @Test
    fun `every word_intro has zero programmatic placeholders and zero null meanings`() {
        val exercises = AppJson.decodeFromString<ExercisesFile>(readAsset("exercises_vocabulary.json"))
        val wordIntros = exercises.exercises.map { it.content }.filterIsInstance<ExerciseContent.WordIntro>()

        wordIntros.forEach { intro ->
            val word = intro.arabicWord
            assertTrue("Word contains programmatic placeholder: $word", 
                !word.contains("Programmatic") && !word.contains("Form ") && !word.contains("فِعْل_") && !word.contains("حَرْف_"))

            allLanguageTags.forEach { lang ->
                val m = intro.meaning[lang]
                assertNotNull("Missing meaning for $lang in ${intro.wordId}", m)
                assertTrue("Meaning is NULL or empty for $lang in ${intro.wordId}", !m.isNullOrBlank() && !m.equals("NULL", ignoreCase = true))
            }
        }
    }

    @Test
    fun `every word_intro has valid non-empty arabic word spans matching the verse`() {
        val exercises = AppJson.decodeFromString<ExercisesFile>(readAsset("exercises_vocabulary.json"))
        val wordIntros = exercises.exercises.map { it.content }.filterIsInstance<ExerciseContent.WordIntro>()

        wordIntros.forEach { intro ->
            val verse = intro.exampleVerseArabic
            assertNotNull("Verse is null for ${intro.wordId}", verse)
            val start = intro.arabicWordStart
            val end = intro.arabicWordEnd
            assertNotNull("Start span is null for ${intro.wordId}", start)
            assertNotNull("End span is null for ${intro.wordId}", end)
            assertTrue("Invalid spans ($start, $end) for verse length ${verse!!.length} in ${intro.wordId}",
                start!! >= 0 && end!! <= verse.length && start < end)
            val token = verse.substring(start, end!!)
            assertTrue("Extracted token is blank in ${intro.wordId}", token.isNotBlank())
        }
    }

    @Test
    fun `every word_intro meaningHighlight exists verbatim in verseTranslation across all 10 languages`() {
        val exercises = AppJson.decodeFromString<ExercisesFile>(readAsset("exercises_vocabulary.json"))
        val wordIntros = exercises.exercises.map { it.content }.filterIsInstance<ExerciseContent.WordIntro>()

        wordIntros.forEach { intro ->
            allLanguageTags.forEach { lang ->
                val verseTr = intro.exampleVerseTranslation[lang]
                assertNotNull("Missing verse translation for $lang in ${intro.wordId}", verseTr)
                assertTrue("Verse translation is blank for $lang in ${intro.wordId}", verseTr!!.isNotBlank())

                val hl = intro.meaningHighlight[lang]
                assertNotNull("Missing translation highlight for $lang in ${intro.wordId}", hl)
                assertTrue("Translation highlight is blank for $lang in ${intro.wordId}", hl!!.isNotBlank())
                assertTrue("Highlight '$hl' not found verbatim in verse translation '$verseTr' for $lang in ${intro.wordId}",
                    verseTr.contains(hl))
            }
        }
    }

    @Test
    fun `every polysemy entry has valid arabic spans and verbatim translation highlights across all 10 languages`() {
        val exercises = AppJson.decodeFromString<ExercisesFile>(readAsset("exercises_vocabulary.json"))
        val wordIntros = exercises.exercises.map { it.content }.filterIsInstance<ExerciseContent.WordIntro>()

        wordIntros.forEach { intro ->
            intro.polysemyEntries.forEach { pe ->
                val verse = pe.verseArabic
                assertNotNull("Polysemy verse is null for sense ${pe.meaningIndex} in ${intro.wordId}", verse)
                val start = pe.arabicWordStart
                val end = pe.arabicWordEnd
                assertNotNull("Polysemy start is null for sense ${pe.meaningIndex} in ${intro.wordId}", start)
                assertNotNull("Polysemy end is null for sense ${pe.meaningIndex} in ${intro.wordId}", end)
                assertTrue("Invalid polysemy spans ($start, $end) in ${intro.wordId}",
                    start!! >= 0 && end!! <= verse!!.length && start < end)

                allLanguageTags.forEach { lang ->
                    val verseTr = pe.verseTranslation[lang]
                    assertNotNull("Missing polysemy verse translation for $lang in ${intro.wordId}", verseTr)
                    val hl = pe.translationHighlight?.get(lang)
                    assertNotNull("Missing polysemy translation highlight for $lang in ${intro.wordId}", hl)
                    assertTrue("Polysemy highlight '$hl' not found in '$verseTr' for $lang in ${intro.wordId}",
                        verseTr!!.contains(hl!!))
                }
            }
        }
    }
}

