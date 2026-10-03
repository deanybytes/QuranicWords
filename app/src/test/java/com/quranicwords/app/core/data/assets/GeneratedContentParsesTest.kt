package com.quranicwords.app.core.data.assets

import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.VerseEntity
import com.quranicwords.app.core.data.repository.WordExampleLocalizer
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.LemmaCategory
import com.quranicwords.app.core.util.AppJson
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
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

    // Decoded per element exactly as ContentSeeder does: an exercise of a withdrawn type (the
    // old listening exercises, until the content pipeline stops emitting them) is skipped.
    private val exercises: List<ExerciseSeedDto> by lazy {
        AppJson.decodeFromString(ListSerializer(JsonElement.serializer()), readAsset("exercises_vocabulary.json"))
            .mapNotNull { runCatching { AppJson.decodeFromJsonElement(ExerciseSeedDto.serializer(), it) }.getOrNull() }
    }
    private val intros by lazy { exercises.map { it.content }.filterIsInstance<ExerciseContent.WordIntro>() }

    /** verses.json decoded into the same entity the seeder stores. */
    private val verses: Map<String, VerseEntity> by lazy {
        Json.parseToJsonElement(readAsset("verses.json")).jsonObject.mapValues { (key, value) ->
            val obj = value.jsonObject
            fun strings(name: String) = obj.getValue(name).jsonObject.mapValues { it.value.jsonPrimitive.content }
            VerseEntity(
                key = key,
                reference = obj.getValue("ref").jsonPrimitive.content,
                arabic = obj.getValue("ar").jsonPrimitive.content,
                wordByWord = strings("wbw"),
                translation = strings("tr")
            )
        }
    }

    @Test
    fun `the first-run tour quotes the current curriculum figures`() {
        assertEquals(reported("words"), com.quranicwords.app.feature.walkthrough.TourFacts.WORDS)
        val coverage = report.getValue("coverage_percent").jsonPrimitive.content.toDouble()
        assertEquals(coverage.toInt(), com.quranicwords.app.feature.walkthrough.TourFacts.COVERAGE_PERCENT)
    }

    @Test
    fun `counts match the build report`() {
        assertEquals(reported("chapters"), AppJson.decodeFromString<ChaptersFile>(readAsset("chapters.json")).chapters.size)
        assertEquals(reported("sections"), AppJson.decodeFromString<SectionsFile>(readAsset("sections.json")).sections.size)
        assertEquals(reported("lessons"), AppJson.decodeFromString<LessonsFile>(readAsset("lessons_vocabulary.json")).lessons.size)
        assertEquals(reported("exercises"), exercises.size + withdrawnExerciseCount(readAsset("exercises_vocabulary.json")))
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
    fun `verses cover the build report and every language`() {
        assertEquals(reported("verses"), verses.size)
        verses.forEach { (key, verse) ->
            assertTrue(key, verse.arabic.isNotBlank())
            assertEquals(key, contentLanguages, verse.wordByWord.keys)
            assertEquals(key, contentLanguages, verse.translation.keys)
        }
    }

    @Test
    fun `every sense is proven against its verse`() {
        intros.forEach { intro ->
            assertEquals(intro.wordId, contentLanguages, intro.senses.keys)
            intro.senses.forEach { (lang, senses) ->
                val where = "${intro.wordId} $lang"
                assertTrue(where, senses.size in 1..3)
                assertEquals(where, intro.meaning.getValue(lang), senses.joinToString(" / ") { it.meaning })
                senses.forEach { sense ->
                    val verse = verses[sense.verse]
                    assertTrue("$where cites missing verse ${sense.verse}", verse != null)
                    verse!!
                    assertTrue(where, sense.wordStart in 0 until sense.wordEnd && sense.wordEnd <= verse.arabic.length)
                    val token = WordExampleLocalizer.tokenSpans(verse.arabic)
                        .indexOfFirst { sense.wordStart >= it.start && sense.wordStart < it.end }
                    assertEquals("$where token index", sense.word, token + 1)
                    val wbw = verse.wordByWord.getValue(lang)
                    assertTrue(where, sense.wbwStart in 0 until sense.wbwEnd && sense.wbwEnd <= wbw.length)
                    assertEquals(where, sense.meaning, wbw.substring(sense.wbwStart, sense.wbwEnd))
                    val tStart = sense.translationStart
                    val tEnd = sense.translationEnd
                    assertEquals(where, tStart == null, tEnd == null)
                    if (tStart != null && tEnd != null) {
                        val translation = verse.translation.getValue(lang)
                        assertTrue(where, tStart in 0 until tEnd && tEnd <= translation.length)
                        assertTrue(where, translation.substring(tStart, tEnd).equals(sense.meaning, ignoreCase = true))
                    }
                }
            }
        }
    }

    @Test
    fun `localizing never drops a sense and keeps every verse quiz answerable in every language`() {
        Language.entries.forEach { language ->
            val localized = intros.associate { it.wordId to WordExampleLocalizer.build(it, language, verses) }
            intros.forEach { intro ->
                assertEquals("${intro.wordId} $language", intro.senses.getValue(language.tag).size, localized.getValue(intro.wordId).senses.size)
            }
            exercises.map { it.content }.forEach { content ->
                when (val out = WordExampleLocalizer.localizeExercise(content, localized)) {
                    is ExerciseContent.FillInTheBlank -> {
                        val correct = out.options.single { it.id == out.correctOptionId }
                        assertEquals(out.wordId, out.sentenceArabic.substring(out.blankStart, out.blankEnd), correct.labelArabic)
                        assertTrue(out.wordId, out.options.size >= 2)
                    }
                    is ExerciseContent.TapWordInVerse -> {
                        val sense = localized.getValue(out.wordId).primarySense!!
                        assertTrue(out.wordId, out.tappableSpans.any { it.start == out.correctWordStart && it.end == out.correctWordEnd })
                        assertTrue(out.wordId, out.correctWordStart <= sense.wordStart && sense.wordEnd <= out.correctWordEnd)
                    }
                    else -> Unit
                }
            }
        }
    }

    @Test
    fun `grammar labels exist in every language wherever an English one is shown`() {
        intros.forEach { intro ->
            if (!intro.verbForm.isNullOrBlank()) {
                assertEquals(intro.wordId, contentLanguages, intro.verbFormLabel.keys)
                assertEquals(intro.wordId, intro.verbForm, intro.verbFormLabel.getValue("en"))
            }
            if (!intro.partOfSpeechDetail.isNullOrBlank()) {
                assertEquals(intro.wordId, contentLanguages, intro.partOfSpeechLabel.keys)
                assertEquals(intro.wordId, intro.partOfSpeechDetail, intro.partOfSpeechLabel.getValue("en"))
            }
        }
    }

    @Test
    fun `bundled verse quizzes match their English default verse exactly`() {
        exercises.map { it.content }.filterIsInstance<ExerciseContent.FillInTheBlank>().forEach { f ->
            val correct = f.options.single { it.id == f.correctOptionId }
            assertEquals(f.wordId, f.sentenceArabic.substring(f.blankStart, f.blankEnd), correct.labelArabic)
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

/** Exercises of the withdrawn listening types still present in the bundled seed JSON - the app
 * skips them on seeding, so they count toward the pipeline report but never reach the database. */
internal fun withdrawnExerciseCount(exercisesJson: String): Int =
    Regex(""""type"\s*:\s*"(tap_what_you_hear|listen_and_type)"""").findAll(exercisesJson).count()
