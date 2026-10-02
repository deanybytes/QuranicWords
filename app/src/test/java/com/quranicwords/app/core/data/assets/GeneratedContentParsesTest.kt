package com.quranicwords.app.core.data.assets

import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.LemmaCategory
import com.quranicwords.app.core.util.AppJson
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Invariants of the bundled curriculum as the app decodes it. Expected counts come from the
 * pipeline's own build report (tools/pipeline/reports/build_report.json), never hardcoded - the
 * previous content passed hardcoded-count tests while teaching 90 verbs 1,479 times over.
 */
class GeneratedContentParsesTest {

    private val assetsDir = File("src/main/assets/content")
    private val report = Json.parseToJsonElement(File("../tools/pipeline/reports/build_report.json").readText()).jsonObject
    private val contentLanguages = setOf("en", "bn", "ur", "hi", "in", "tr", "fa", "fr")

    private fun readAsset(name: String): String = File(assetsDir, name).readText()
    private fun reported(key: String) = report.getValue(key).jsonPrimitive.int

    private val exercises: List<ExerciseSeedDto> by lazy {
        AppJson.decodeFromString(ListSerializer(ExerciseSeedDto.serializer()), readAsset("exercises_vocabulary.json"))
    }
    private val intros by lazy { exercises.map { it.content }.filterIsInstance<ExerciseContent.WordIntro>() }

    @Test
    fun `counts match the build report`() {
        assertEquals(reported("chapters"), AppJson.decodeFromString<ChaptersFile>(readAsset("chapters.json")).chapters.size)
        assertEquals(reported("sections"), AppJson.decodeFromString<SectionsFile>(readAsset("sections.json")).sections.size)
        assertEquals(reported("lessons"), AppJson.decodeFromString<LessonsFile>(readAsset("lessons_vocabulary.json")).lessons.size)
        assertEquals(reported("exercises"), exercises.size)
        assertEquals(reported("words"), AppJson.decodeFromString<WordFrequencyFile>(readAsset("word_frequency.json")).words.size)
    }

    @Test
    fun `chapter word counts add up to the word list`() {
        val chapters = AppJson.decodeFromString<ChaptersFile>(readAsset("chapters.json")).chapters
        assertEquals(reported("words"), chapters.sumOf { it.wordCount })
        assertTrue(chapters.sumOf { it.quranOccurrencePercent } <= 100.0)
    }

    @Test
    fun `every word is taught exactly once and every word is distinct`() {
        assertEquals(reported("words"), intros.size)
        assertEquals(intros.size, intros.map { it.wordId }.toSet().size)
        assertEquals(intros.size, intros.map { it.arabicWord to it.lemmaCategory }.toSet().size)
    }

    @Test
    fun `every lesson has a valid shape`() {
        val lessons = AppJson.decodeFromString<LessonsFile>(readAsset("lessons_vocabulary.json")).lessons
        lessons.forEach { lesson ->
            val chapterScoped = lesson.kind == LessonKind.CHAPTER_EXAM || lesson.kind == LessonKind.CHAPTER_FLASHBACK
            assertEquals(chapterScoped, lesson.sectionId == null)
        }
        val scoredLessons = exercises.filter { it.content !is ExerciseContent.WordIntro && it.content !is ExerciseContent.ChapterIntro }
            .map { it.lessonId }.toSet()
        lessons.filter { it.kind != LessonKind.CHAPTER_INTRO }.forEach { assertTrue(it.id, it.id in scoredLessons) }
        val categories = lessons.map { it.category }.toSet()
        assertTrue(LemmaCategory.NOUN in categories && LemmaCategory.VERB in categories)
    }

    @Test
    fun `meanings cover every content language and the word list agrees with the intros`() {
        val words = AppJson.decodeFromString<WordFrequencyFile>(readAsset("word_frequency.json")).words.associateBy { it.id }
        intros.forEach { intro ->
            assertEquals(intro.wordId, contentLanguages, intro.meaning.keys)
            intro.meaning.values.forEach { assertTrue(intro.wordId, it.isNotBlank()) }
            assertEquals(intro.wordId, words.getValue(intro.wordId).meaning, intro.meaning)
        }
    }

    @Test
    fun `verse spans and highlights are exact`() {
        intros.forEach { intro ->
            val verse = intro.exampleVerseArabic!!
            val start = intro.arabicWordStart!!
            val end = intro.arabicWordEnd!!
            assertTrue(intro.wordId, start in 0 until end && end <= verse.length)
            assertEquals(intro.wordId, contentLanguages, intro.exampleVerseTranslation.keys)
            intro.meaningHighlight.forEach { (lang, hl) ->
                assertTrue("${intro.wordId} $lang", intro.exampleVerseTranslation.getValue(lang).contains(hl))
            }
            intro.polysemyEntries.forEach { pe ->
                val v = pe.verseArabic!!
                assertTrue(intro.wordId, pe.arabicWordStart!! in 0 until pe.arabicWordEnd!! && pe.arabicWordEnd <= v.length)
            }
        }
        exercises.map { it.content }.filterIsInstance<ExerciseContent.TapWordInVerse>().forEach { t ->
            assertTrue(t.wordId, t.tappableSpans.any { it.start == t.correctWordStart && it.end == t.correctWordEnd })
        }
    }

    @Test
    fun `option-bearing exercises always contain the answer and never two equal meanings`() {
        exercises.map { it.content }.filterIsInstance<com.quranicwords.app.core.domain.model.OptionsBearing>().forEach { ex ->
            assertTrue(ex.wordId, ex.options.any { it.id == ex.correctOptionId })
            contentLanguages.forEach { lang ->
                val labels = ex.options.map { it.label[lang]?.lowercase() }
                assertEquals("${ex.wordId} $lang", labels.size, labels.toSet().size)
            }
        }
    }

    @Test
    fun `legacy map covers both released content versions`() {
        val text = readAsset("legacy_progress_map.json")
        val map = AppJson.decodeFromString<Map<String, com.quranicwords.app.core.data.migration.LegacyContentMap>>(text)
        assertTrue(map.keys.containsAll(setOf("34", "35")))
        val wordIds = intros.map { it.wordId }.toSet()
        map.values.forEach { m -> assertTrue(m.words.values.all { it in wordIds }) }
    }
}
