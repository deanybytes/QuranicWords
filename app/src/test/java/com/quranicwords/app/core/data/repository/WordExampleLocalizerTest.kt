package com.quranicwords.app.core.data.repository

import com.quranicwords.app.core.data.local.entity.VerseEntity
import com.quranicwords.app.core.domain.model.ChoiceOption
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.WordSpan
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordExampleLocalizerTest {

    // "And among the people are some who say" - مَا-free on purpose; the taught word is مَن.
    private val arabic = "وَمِنَ ٱلنَّاسِ مَن يَقُولُ"
    private val verse = VerseEntity(
        key = "2:8",
        reference = "Al-Baqarah 2:8",
        arabic = arabic,
        wordByWord = mapOf(
            "en" to "And of the people who says",
            "ur" to "اور لوگوں میں سے جو کہتا ہے",
            "fr" to "Et parmi les gens qui dit"
        ),
        translation = mapOf(
            "en" to "And of the people are some who say",
            "ur" to "اور لوگوں میں کچھ ایسے ہیں جو کہتے ہیں",
            "fr" to "Parmi les gens, il y a ceux qui disent"
        )
    )
    private val wordStart = arabic.indexOf("مَن")
    private val wordEnd = wordStart + "مَن".length

    private val intro = ExerciseContent.WordIntro(
        prompt = mapOf("en" to "Learn this word"),
        wordId = "wp_man",
        arabicWord = "مَن",
        meaning = mapOf("en" to "who", "ur" to "جو", "fr" to "qui", "bn" to "যে"),
        senses = mapOf(
            "en" to listOf(
                ExerciseContent.SenseRef(
                    meaning = "who", verse = "2:8", word = 3, wordStart = wordStart, wordEnd = wordEnd,
                    wbwStart = 18, wbwEnd = 21, translationStart = 27, translationEnd = 30
                )
            ),
            "ur" to listOf(
                ExerciseContent.SenseRef(
                    meaning = "جو", verse = "2:8", word = 3, wordStart = wordStart, wordEnd = wordEnd,
                    wbwStart = 17, wbwEnd = 19, translationStart = null, translationEnd = null
                )
            ),
            "fr" to listOf(
                ExerciseContent.SenseRef(
                    meaning = "qui", verse = "2:8", word = 3, wordStart = wordStart, wordEnd = wordEnd,
                    wbwStart = 18, wbwEnd = 21, translationStart = 28, translationEnd = 31
                )
            )
        )
    )

    private val verses = mapOf("2:8" to verse)

    @Test
    fun `spans are carried over exactly as the content gives them`() {
        val word = WordExampleLocalizer.build(intro, Language.FRENCH, verses)
        assertEquals("qui", word.meaning)
        val sense = word.senses.single()
        assertEquals(arabic, sense.verseArabic)
        assertEquals("مَن", sense.verseArabic.substring(sense.wordStart, sense.wordEnd))
        assertEquals("qui", sense.wbwText.substring(sense.wbwStart, sense.wbwEnd))
        assertEquals("qui", sense.translationText.substring(sense.translationStart!!, sense.translationEnd!!))
        assertEquals(Language.FRENCH, sense.textLanguage)
        assertEquals("2:8", sense.verseKey)
    }

    @Test
    fun `the citation is always fully in the learner's language`() {
        val urdu = WordExampleLocalizer.build(intro, Language.URDU, verses).senses.single()
        assertEquals("سورۃ البقرۃ ۲:۸", urdu.reference)
        val english = WordExampleLocalizer.build(intro, Language.ENGLISH, verses).senses.single()
        assertEquals("Surah Al-Baqarah 2:8", english.reference)
    }

    @Test
    fun `no translation span means no translation highlight`() {
        val sense = WordExampleLocalizer.build(intro, Language.URDU, verses).senses.single()
        assertNull(sense.translationStart)
        assertNull(sense.translationEnd)
        assertEquals("جو", sense.wbwText.substring(sense.wbwStart, sense.wbwEnd))
        assertEquals(verse.translation["ur"], sense.translationText)
    }

    @Test
    fun `a language without senses falls back to English senses and texts, but keeps its own meaning`() {
        val word = WordExampleLocalizer.build(intro, Language.BANGLA, verses)
        assertEquals("যে", word.meaning)
        val sense = word.senses.single()
        assertEquals(Language.ENGLISH, sense.textLanguage)
        assertEquals("who", sense.meaning)
        assertEquals("who", sense.wbwText.substring(sense.wbwStart, sense.wbwEnd))
        assertEquals("who", sense.translationText.substring(sense.translationStart!!, sense.translationEnd!!))
        // ... while the citation still follows the learner's language.
        assertEquals("সূরা আল-বাকারা ২:৮", sense.reference)
    }

    @Test
    fun `a sense whose verse is missing or whose span does not fit is left out`() {
        assertTrue(WordExampleLocalizer.build(intro, Language.ENGLISH, emptyMap()).senses.isEmpty())
        val broken = intro.copy(
            senses = mapOf("en" to listOf(intro.senses.getValue("en").single().copy(wbwEnd = 999)))
        )
        assertTrue(WordExampleLocalizer.build(broken, Language.ENGLISH, verses).senses.isEmpty())
        val badTranslation = intro.copy(
            senses = mapOf("en" to listOf(intro.senses.getValue("en").single().copy(translationEnd = 999)))
        )
        assertNull(WordExampleLocalizer.build(badTranslation, Language.ENGLISH, verses).senses.single().translationStart)
    }

    @Test
    fun `fill in the blank is rebuilt on the localized sense with the correct option equal to the blank`() {
        val baked = ExerciseContent.FillInTheBlank(
            prompt = mapOf("en" to "Complete the verse"),
            wordId = "wp_man",
            sentenceArabic = "قَالَ مَنۡ",
            blankStart = 6,
            blankEnd = 10,
            sentenceTranslation = mapOf("en" to "He said: who"),
            sentenceReference = "Taha 20:49",
            options = listOf(
                ChoiceOption(id = "opt_man", labelArabic = "مَنْ"),
                ChoiceOption(id = "opt_min", labelArabic = "مِنْ"),
                // Reads exactly like the blank once rebuilt - two identical choices would be unfair.
                ChoiceOption(id = "opt_other_man", labelArabic = "مَن"),
                ChoiceOption(id = "opt_fi", labelArabic = "فِي")
            ),
            correctOptionId = "opt_man"
        )
        val words = mapOf("wp_man" to WordExampleLocalizer.build(intro, Language.URDU, verses))
        val out = WordExampleLocalizer.localizeExercise(baked, words) as ExerciseContent.FillInTheBlank
        assertEquals(arabic, out.sentenceArabic)
        assertEquals(wordStart, out.blankStart)
        assertEquals(wordEnd, out.blankEnd)
        assertEquals("2:8", out.sentenceReference)
        assertEquals(verse.translation["ur"], out.sentenceTranslation.values.single())
        val correct = out.options.single { it.id == out.correctOptionId }
        assertEquals(out.sentenceArabic.substring(out.blankStart, out.blankEnd), correct.labelArabic)
        assertEquals(listOf("opt_man", "opt_min", "opt_fi"), out.options.map { it.id })
    }

    @Test
    fun `tap the word uses the token that holds the word`() {
        // The taught word is the attached وَ - only part of its written token وَمِنَ.
        val waIntro = intro.copy(
            wordId = "wp_wa",
            senses = mapOf(
                "en" to listOf(
                    ExerciseContent.SenseRef(
                        meaning = "And", verse = "2:8", word = 1, wordStart = 0, wordEnd = 2,
                        wbwStart = 0, wbwEnd = 3, translationStart = 0, translationEnd = 3
                    )
                )
            )
        )
        val baked = ExerciseContent.TapWordInVerse(
            prompt = mapOf("en" to "Tap the word that means this"),
            wordId = "wp_wa",
            verseArabic = "x y",
            verseReference = "1:1",
            correctWordStart = 0,
            correctWordEnd = 1,
            tappableSpans = listOf(WordSpan(0, 1), WordSpan(2, 3)),
            meaning = mapOf("en" to "and")
        )
        val words = mapOf("wp_wa" to WordExampleLocalizer.build(waIntro, Language.ENGLISH, verses))
        val out = WordExampleLocalizer.localizeExercise(baked, words) as ExerciseContent.TapWordInVerse
        assertEquals(arabic, out.verseArabic)
        assertEquals("2:8", out.verseReference)
        assertEquals(WordExampleLocalizer.tokenSpans(arabic), out.tappableSpans)
        assertEquals("وَمِنَ", out.verseArabic.substring(out.correctWordStart, out.correctWordEnd))
        assertEquals(4, out.tappableSpans.size)
    }

    @Test
    fun `a word written across two tokens is one tappable span`() {
        val verseText = "سَلَٰمٌ عَلَىٰٓ إِلۡ يَاسِينَ"
        val start = verseText.indexOf("إِلۡ")
        val sense = WordExampleLocalizer.build(
            intro.copy(
                senses = mapOf(
                    "en" to listOf(
                        ExerciseContent.SenseRef(
                            meaning = "Ilyas", verse = "37:130", word = 3, wordStart = start, wordEnd = verseText.length,
                            wbwStart = 0, wbwEnd = 5
                        )
                    )
                )
            ),
            Language.ENGLISH,
            mapOf("37:130" to VerseEntity("37:130", "As-Saffat 37:130", verseText, mapOf("en" to "Ilyas peace"), mapOf("en" to "Peace upon Elias")))
        ).primarySense!!
        val baked = ExerciseContent.TapWordInVerse(
            prompt = emptyMap(), wordId = "w", verseArabic = "", verseReference = "", correctWordStart = 0,
            correctWordEnd = 0, tappableSpans = emptyList(), meaning = emptyMap()
        )
        val out = WordExampleLocalizer.localizedTapWordInVerse(baked, sense)
        assertEquals("إِلۡ يَاسِينَ", out.verseArabic.substring(out.correctWordStart, out.correctWordEnd))
        assertEquals(3, out.tappableSpans.size)
        assertTrue(out.tappableSpans.any { it.start == out.correctWordStart && it.end == out.correctWordEnd })
    }

    @Test
    fun `a word without localized senses keeps its exercise unchanged`() {
        val baked = ExerciseContent.TapWordInVerse(
            prompt = emptyMap(), wordId = "unknown", verseArabic = "x", verseReference = "1:1", correctWordStart = 0,
            correctWordEnd = 1, tappableSpans = listOf(WordSpan(0, 1)), meaning = emptyMap()
        )
        assertEquals(baked, WordExampleLocalizer.localizeExercise(baked, emptyMap()))
    }

    @Test
    fun `verses are loaded once and then served from memory`() = runTest {
        val requested = mutableListOf<Collection<String>>()
        val localizer = WordExampleLocalizer { keys -> requested += keys.toList(); keys.mapNotNull { verses[it] } }
        val first = localizer.localize(intro, Language.ENGLISH)
        val again = localizer.localizeAll(listOf(intro), Language.FRENCH).getValue("wp_man")
        assertEquals(1, first.senses.size)
        assertEquals("qui", again.senses.single().meaning)
        assertEquals(listOf(listOf("2:8")), requested)
    }
}
