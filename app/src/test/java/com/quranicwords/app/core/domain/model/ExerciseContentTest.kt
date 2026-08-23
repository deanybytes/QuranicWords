package com.quranicwords.app.core.domain.model

import com.quranicwords.app.core.util.AppJson
import com.quranicwords.app.core.util.GamificationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val EN_BN = mapOf("en" to "p", "bn" to "p")

class ExerciseContentTest {

    private fun teachStep(wordId: String) = ExerciseContent.WordIntro(
        prompt = mapOf("en" to "Meet a new word", "bn" to "একটি নতুন শব্দ চিনুন"),
        wordId = wordId,
        arabicWord = "مِن",
        meaning = mapOf("en" to "from", "bn" to "থেকে"),
        exampleVerseArabic = "بِسْمِ ٱللَّهِ",
        exampleVerseTranslation = mapOf("en" to "In the name of Allah", "bn" to "আল্লাহর নামে"),
        exampleVerseReference = "1:1"
    )

    private fun quizStep(wordId: String) = ExerciseContent.MultipleChoice(
        prompt = mapOf("en" to "What does this word mean?", "bn" to "এই শব্দের অর্থ কী?"),
        wordId = wordId,
        options = listOf(ChoiceOption(id = "o1", label = mapOf("en" to "from"))),
        correctOptionId = "o1"
    )

    private fun fillInTheBlank(wordId: String) = ExerciseContent.FillInTheBlank(
        prompt = mapOf("en" to "Fill in the blank", "bn" to "শূন্যস্থান পূরণ করুন"),
        wordId = wordId, sentenceArabic = "بِسْمِ ٱللَّهِ", blankStart = 5, blankEnd = 10,
        sentenceTranslation = mapOf("en" to "In the name of Allah", "bn" to "আল্লাহর নামে"),
        sentenceReference = "1:1",
        options = listOf(ChoiceOption(id = wordId, label = mapOf("en" to "Allah"))), correctOptionId = wordId
    )

    private fun wordOrder(wordId: String) = ExerciseContent.WordOrderBuilder(
        prompt = mapOf("en" to "Put in order", "bn" to "সাজান"),
        wordId = wordId,
        orderedChips = listOf(ExerciseContent.WordChip("c1", "بِسْمِ"), ExerciseContent.WordChip("c2", "ٱللَّهِ")),
        translation = mapOf("en" to "In the name of Allah", "bn" to "আল্লাহর নামে")
    )

    private fun listenAndType(wordId: String) = ExerciseContent.ListenAndType(
        prompt = mapOf("en" to "Type what you hear", "bn" to "যা শুনলেন তা লিখুন"),
        wordId = wordId, audioAssetPath = "audio/$wordId.mp3", correctAnswer = "Allah",
        acceptedAnswers = listOf("allah")
    )

    @Test
    fun `teach steps are not scored, quiz and matching are`() {
        assertFalse(teachStep("alif").isScored)
        assertTrue(quizStep("q1").isScored)
        assertTrue(ExerciseContent.Matching(prompt = EN_BN, pairs = emptyList()).isScored)
        assertTrue(
            ExerciseContent.TapWhatYouHear(
                prompt = EN_BN, audioAssetPath = "a.mp3", wordId = "x",
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
        assertEquals("q1", quizStep("q1").practicedItemId())
        assertEquals("w1", fillInTheBlank("w1").practicedItemId())
        assertEquals("w1", wordOrder("w1").practicedItemId())
        assertEquals("w1", listenAndType("w1").practicedItemId())
        assertNull(ExerciseContent.Matching(prompt = EN_BN, pairs = emptyList()).practicedItemId())
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
    fun `WordIntro highlight fields round-trip through serialization, defaulting to empty`() {
        val withHighlights = ExerciseContent.WordIntro(
            prompt = mapOf("en" to "Meet a new word", "bn" to "একটি নতুন শব্দ চিনুন"),
            wordId = "wf_1",
            arabicWord = "مِن",
            meaning = mapOf("en" to "from", "bn" to "থেকে"),
            exampleVerseArabic = "بِسْمِ ٱللَّهِ",
            exampleVerseTranslation = mapOf("en" to "In the name of Allah", "bn" to "আল্লাহর নামে"),
            exampleVerseReference = "1:1",
            arabicWordStart = 5,
            arabicWordEnd = 10,
            meaningHighlight = mapOf("en" to "In", "bn" to "নামে")
        )
        val json = AppJson.encodeToString(ExerciseContent.serializer(), withHighlights)
        val decoded = AppJson.decodeFromString(ExerciseContent.serializer(), json) as ExerciseContent.WordIntro
        assertEquals(withHighlights, decoded)

        // Content predating the highlight/reviewed fields (no such keys present at all) must
        // still parse cleanly, defaulting to empty maps rather than failing to decode.
        val olderShapedJson = """
            {"type":"word_intro","prompt":{"en":"p","bn":"p"},"wordId":"w","arabicWord":"a",
             "meaning":{"en":"m","bn":"m"},"exampleVerseArabic":"v",
             "exampleVerseTranslation":{"en":"t","bn":"t"},"exampleVerseReference":"1:1"}
        """.trimIndent()
        val olderDecoded = AppJson.decodeFromString(ExerciseContent.serializer(), olderShapedJson) as ExerciseContent.WordIntro
        assertNull(olderDecoded.arabicWordStart)
        assertTrue(olderDecoded.meaningHighlight.isEmpty())
        assertTrue(olderDecoded.meaningReviewed.isEmpty())
    }
}
