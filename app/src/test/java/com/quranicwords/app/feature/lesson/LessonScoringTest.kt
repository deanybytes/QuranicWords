package com.quranicwords.app.feature.lesson

import com.quranicwords.app.core.domain.model.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The scoring rules [LessonViewModel] applies, exercised on the pure [LessonScoring] it delegates
 * to: each scenario replays the exact grade/log calls the ViewModel makes for that interaction.
 */
class LessonScoringTest {

    @Test
    fun `a correct retry after a wrong answer does not score`() {
        // Wrong first check, "Try again", then correct on the same exercise index.
        val scoring = LessonScoring()
            .grade(index = 0, correct = false)
            .grade(index = 0, correct = true)

        assertEquals(0, scoring.correctCount)
        assertTrue(scoring.isGraded(0))
    }

    @Test
    fun `a first-try correct answer scores exactly once`() {
        // Correct, then "Previous" back to it and answered correctly again.
        val scoring = LessonScoring()
            .grade(index = 0, correct = true)
            .grade(index = 0, correct = true)
            .grade(index = 1, correct = true)

        assertEquals(2, scoring.correctCount)
    }

    @Test
    fun `only the first attempt per word is logged as a first try`() {
        val (afterWrong, wrong) = LessonScoring().log("w1", ExerciseType.MULTIPLE_CHOICE, correct = false)
        val (afterRetry, retry) = afterWrong.log("w1", ExerciseType.MULTIPLE_CHOICE, correct = true)
        val (_, otherWord) = afterRetry.log("w2", ExerciseType.MULTIPLE_CHOICE, correct = true)

        assertTrue(wrong.isFirstTry)
        assertFalse(wrong.correct)
        // The retry is still logged, but can't clear w1 from the mistakes list.
        assertFalse(retry.isFirstTry)
        assertTrue(otherWord.isFirstTry)
    }

    @Test
    fun `an in-session repeat of the same word is not a first try`() {
        // LessonContentRepeater places the same word at a later index - it scores (new index)
        // but its attempt row is not a fresh recall signal.
        var scoring = LessonScoring().grade(0, correct = true)
        val first = scoring.log("w1", ExerciseType.MULTIPLE_CHOICE, correct = true).also { scoring = it.first }.second
        scoring = scoring.grade(3, correct = true)
        val repeat = scoring.log("w1", ExerciseType.FILL_IN_THE_BLANK, correct = true).second

        assertEquals(2, scoring.correctCount)
        assertTrue(first.isFirstTry)
        assertFalse(repeat.isFirstTry)
    }

    @Test
    fun `a matching mismatch makes the exercise wrong and logs the mistake`() {
        // Mismatch on w1's tile, then every pair solved: w1's first-try row is the wrong one, its
        // eventual correct match is a retry, and the exercise grades as wrong.
        var scoring = LessonScoring()
        val mismatches = mutableSetOf<String>()

        val (s1, mismatchLog) = scoring.log("w1", ExerciseType.MATCHING, correct = false)
        scoring = s1
        mismatches += "p1"
        val (s2, w1Match) = scoring.log("w1", ExerciseType.MATCHING, correct = true)
        val (s3, w2Match) = s2.log("w2", ExerciseType.MATCHING, correct = true)
        scoring = s3.grade(index = 0, correct = mismatches.isEmpty())

        assertEquals(AttemptLog("w1", ExerciseType.MATCHING, correct = false, isFirstTry = true), mismatchLog)
        assertFalse(w1Match.isFirstTry)
        assertTrue(w2Match.isFirstTry)
        assertEquals(0, scoring.correctCount)
    }

    @Test
    fun `a skipped exercise counts as wrong and cannot be re-scored`() {
        val scoring = LessonScoring()
            .grade(index = 0, correct = false) // Skip
            .grade(index = 0, correct = true) // Try again, solved

        assertEquals(0, scoring.correctCount)
    }

    @Test
    fun `exam gating uses the first-try score, not retries`() {
        // 5-question exam: 3 right first time, 2 wrong then fixed on retry. Retry-scoring would
        // report 5/5 (pass); first-try scoring reports 3/5 = 60% (fail, threshold 80%).
        var scoring = LessonScoring()
        listOf(true, true, true, false, false).forEachIndexed { i, correct -> scoring = scoring.grade(i, correct) }
        scoring = scoring.grade(3, correct = true).grade(4, correct = true)

        assertEquals(3, scoring.correctCount)
        assertEquals(60, scoring.scorePercent(totalCount = 5))
        assertFalse(scoring.meetsPassingScore(totalCount = 5))

        var passing = LessonScoring()
        listOf(true, true, true, true, false).forEachIndexed { i, correct -> passing = passing.grade(i, correct) }
        assertTrue(passing.meetsPassingScore(totalCount = 5))
    }
}
