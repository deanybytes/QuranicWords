package com.quranicwords.app.core.domain.model

import com.quranicwords.app.core.util.AppJson
import com.quranicwords.app.core.util.GamificationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseContentTest {

    private fun teachStep(wordId: String) = ExerciseContent.WordIntro(
        promptEn = "Meet a new word",
        promptBn = "একটি নতুন শব্দ চিনুন",
        wordId = wordId,
        arabicWord = "مِن",
        meaningEn = "from",
        meaningBn = "থেকে",
        exampleVerseArabic = "بِسْمِ ٱللَّهِ",
        exampleVerseTranslationEn = "In the name of Allah",
        exampleVerseTranslationBn = "আল্লাহর নামে",
        exampleVerseReference = "1:1"
    )

    private fun quizStep(id: String) = ExerciseContent.MultipleChoice(
        promptEn = "What does this word mean?",
        promptBn = "এই শব্দের অর্থ কী?",
        options = listOf(ChoiceOption(id = "o1", labelEn = "from")),
        correctOptionId = "o1"
    )

    private fun fillInTheBlank(wordId: String) = ExerciseContent.FillInTheBlank(
        promptEn = "Fill in the blank", promptBn = "শূন্যস্থান পূরণ করুন",
        wordId = wordId, sentenceArabic = "بِسْمِ ٱللَّهِ", blankStart = 5, blankEnd = 10,
        sentenceTranslationEn = "In the name of Allah", sentenceTranslationBn = "আল্লাহর নামে",
        sentenceReference = "1:1",
        options = listOf(ChoiceOption(id = wordId, labelEn = "Allah")), correctOptionId = wordId
    )

    private fun wordOrder(wordId: String) = ExerciseContent.WordOrderBuilder(
        promptEn = "Put in order", promptBn = "সাজান",
        wordId = wordId,
        orderedChips = listOf(ExerciseContent.WordChip("c1", "بِسْمِ"), ExerciseContent.WordChip("c2", "ٱللَّهِ")),
        translationEn = "In the name of Allah", translationBn = "আল্লাহর নামে"
    )

    private fun listenAndType(wordId: String) = ExerciseContent.ListenAndType(
        promptEn = "Type what you hear", promptBn = "যা শুনলেন তা লিখুন",
        wordId = wordId, audioAssetPath = "audio/$wordId.mp3", correctAnswer = "Allah",
        acceptedAnswers = listOf("allah")
    )

    @Test
    fun `teach steps are not scored, quiz and matching are`() {
        assertFalse(teachStep("alif").isScored)
        assertTrue(quizStep("q1").isScored)
        assertTrue(ExerciseContent.Matching(promptEn = "", promptBn = "", pairs = emptyList()).isScored)
        assertTrue(
            ExerciseContent.TapWhatYouHear(
                promptEn = "", promptBn = "", audioAssetPath = "a.mp3",
                options = emptyList(), correctOptionId = "x"
            ).isScored
        )
        assertTrue(fillInTheBlank("w1").isScored)
        assertTrue(wordOrder("w1").isScored)
        assertTrue(listenAndType("w1").isScored)
    }

    @Test
    fun `practicedItemId resolves for scored types and is null for teach steps and matching`() {
        assertNull(teachStep("alif").practicedItemId())
        assertEquals("o1", quizStep("q1").practicedItemId())
        assertEquals("w1", fillInTheBlank("w1").practicedItemId())
        assertEquals("w1", wordOrder("w1").practicedItemId())
        assertEquals("w1", listenAndType("w1").practicedItemId())
        assertNull(ExerciseContent.Matching(promptEn = "", promptBn = "", pairs = emptyList()).practicedItemId())
    }

    @Test
    fun `new exercise subtypes round-trip through serialization`() {
        listOf(fillInTheBlank("w1"), wordOrder("w1"), listenAndType("w1")).forEach { content ->
            val json = AppJson.encodeToString(ExerciseContent.serializer(), content)
            val decoded = AppJson.decodeFromString(ExerciseContent.serializer(), json)
            assertEquals(content, decoded)
        }
    }

    @Test
    fun `perfect-lesson bonus is reachable when teach steps are mixed into a lesson`() {
        // 4 teach steps + 4 quiz steps, all answered correctly - mirrors a real vocabulary lesson.
        val contents = listOf(
            teachStep("alif"), quizStep("q1"),
            teachStep("ba"), quizStep("q2"),
            teachStep("ta"), quizStep("q3"),
            teachStep("tha"), quizStep("q4")
        )
        val scoredTotal = contents.count { it.isScored }
        val correctCount = scoredTotal // every quiz answered correctly

        // Using the raw content count (8) instead of the isScored-filtered count (4) would make
        // this bonus unreachable, since correctCount can never exceed the number of scored items.
        assertEquals(4, scoredTotal)
        val points = GamificationConfig.pointsForLesson(correctCount, scoredTotal)
        assertEquals(4 * GamificationConfig.POINTS_PER_CORRECT_ANSWER + GamificationConfig.PERFECT_LESSON_BONUS, points)
    }

    @Test
    fun `WordIntro highlight fields round-trip through serialization, defaulting to null`() {
        val withHighlights = ExerciseContent.WordIntro(
            promptEn = "Meet a new word",
            promptBn = "একটি নতুন শব্দ চিনুন",
            wordId = "wf_1",
            arabicWord = "مِن",
            meaningEn = "from",
            meaningBn = "থেকে",
            exampleVerseArabic = "بِسْمِ ٱللَّهِ",
            exampleVerseTranslationEn = "In the name of Allah",
            exampleVerseTranslationBn = "আল্লাহর নামে",
            exampleVerseReference = "1:1",
            arabicWordStart = 5,
            arabicWordEnd = 10,
            meaningHighlightEn = "In",
            meaningHighlightBn = "নামে"
        )
        val json = AppJson.encodeToString(ExerciseContent.serializer(), withHighlights)
        val decoded = AppJson.decodeFromString(ExerciseContent.serializer(), json) as ExerciseContent.WordIntro
        assertEquals(withHighlights, decoded)

        // Old-shaped content (no highlight fields present at all) must still parse cleanly.
        val legacyJson = """
            {"type":"word_intro","promptEn":"p","promptBn":"p","wordId":"w","arabicWord":"a",
             "meaningEn":"m","meaningBn":"m","exampleVerseArabic":"v","exampleVerseTranslationEn":"t",
             "exampleVerseTranslationBn":"t","exampleVerseReference":"1:1"}
        """.trimIndent()
        val legacyDecoded = AppJson.decodeFromString(ExerciseContent.serializer(), legacyJson) as ExerciseContent.WordIntro
        assertNull(legacyDecoded.arabicWordStart)
        assertNull(legacyDecoded.meaningHighlightEn)
    }
}
