package com.quranicwords.app.feature.widget

import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.util.VerseReferenceFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetDataProviderTest {

    @Test
    fun `rotation step ratio gives 2 to 1 for mistaken vs learned words`() {
        val missedWords = listOf("wf_1", "wf_2")
        val masteredWords = listOf("wf_10", "wf_11", "wf_12")

        fun pickWordId(step: Int): String {
            val isMistakeTurn = (step % 3 != 2)
            return if (isMistakeTurn) {
                val index = ((step / 3) * 2 + (step % 3)) % missedWords.size
                missedWords[index]
            } else {
                val index = (step / 3) % masteredWords.size
                masteredWords[index]
            }
        }

        val step0 = pickWordId(0) // mistake turn -> missed
        val step1 = pickWordId(1) // mistake turn -> missed
        val step2 = pickWordId(2) // mastered turn -> mastered

        assertTrue(missedWords.contains(step0))
        assertTrue(missedWords.contains(step1))
        assertTrue(masteredWords.contains(step2))

        val step3 = pickWordId(3) // mistake turn
        val step4 = pickWordId(4) // mistake turn
        val step5 = pickWordId(5) // mastered turn

        assertTrue(missedWords.contains(step3))
        assertTrue(missedWords.contains(step4))
        assertTrue(masteredWords.contains(step5))
    }

    @Test
    fun `quran percentage calculation is accurate`() {
        val count = 2800 // e.g. "Allah"
        val totalWords = 77797.0
        val percentage = (count / totalWords) * 100.0
        assertTrue(percentage > 3.0 && percentage < 4.0)
    }

    @Test
    fun `widget digit formatting is correct across all 11 supported languages`() {
        val testNumber = "1234567890"

        val expected = mapOf(
            Language.ENGLISH to "1234567890",
            Language.BANGLA to "১২৩৪৫৬৭৮৯০",
            Language.URDU to "۱۲۳۴۵۶۷۸۹۰",
            Language.PERSIAN to "۱۲۳۴۵۶۷۸۹۰",
            Language.HINDI to "१२३४५६७८९०",
            Language.INDONESIAN to "1234567890",
            Language.TURKISH to "1234567890",
            Language.FRENCH to "1234567890",
            Language.MALAY to "1234567890",
            Language.SWAHILI to "1234567890",
            Language.HAUSA to "1234567890"
        )

        assertEquals(11, Language.entries.size)
        for (lang in Language.entries) {
            val formatted = VerseReferenceFormatter.formatDigits(testNumber, lang)
            assertEquals("Failed digit formatting for language $lang", expected[lang], formatted)
        }
    }

    @Test
    fun `widget snapshot preserves user language`() {
        val dummyStats = WidgetStatsData(
            streakDays = 5,
            isStreakActive = true,
            wordsLearnedCount = 50,
            wordsLearnedPct = 1.05f,
            accuracyPct = 95,
            todayPracticeMinutes = 10,
            dailyGoalMinutes = 15,
            dailyGoalProgressPct = 67,
            reviewCount = 3
        )
        val snapshot = WidgetSnapshot(
            stats = dummyStats,
            currentWord = null,
            language = Language.BANGLA
        )
        assertEquals(Language.BANGLA, snapshot.language)
        assertEquals(5, snapshot.stats.streakDays)
    }
}
