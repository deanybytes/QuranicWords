package com.quranicwords.app.core.data.repository

import com.quranicwords.app.core.data.local.entity.VerseEntity
import com.quranicwords.app.core.domain.model.ChoiceOption
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.Language
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
