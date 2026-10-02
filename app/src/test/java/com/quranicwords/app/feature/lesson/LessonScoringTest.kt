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

    @Test
    fun `combo counts consecutive first-check corrects and pays tiered bonuses`() {
        var scoring = LessonScoring()
        repeat(10) { scoring = scoring.grade(it, correct = true) }

        assertEquals(10, scoring.combo)
        assertEquals(10, scoring.bestCombo)
        // Answers 5-9 pay +2 each, the 10th pays +5.
        assertEquals(5 * 2 + 5, scoring.comboBonusXp)

        scoring = scoring.grade(10, correct = false)
        assertEquals(0, scoring.combo)
        assertEquals(10, scoring.bestCombo)
    }

    @Test
    fun `a re-check never changes the combo`() {
        val scoring = LessonScoring()
            .grade(0, correct = true)
            .grade(1, correct = false)
            .grade(1, correct = true) // "Try again" on the same exercise

        assertEquals(0, scoring.combo)
        assertEquals(1, scoring.bestCombo)
    }

    @Test
    fun `missed words, listening answers and new words feed the session stats`() {
        var scoring = LessonScoring()
        scoring = scoring.log("w1", ExerciseType.TAP_WHAT_YOU_HEAR, correct = false).first
        scoring = scoring.log("w1", ExerciseType.MULTIPLE_CHOICE, correct = true).first // retry - not first try
        scoring = scoring.log("w2", ExerciseType.MULTIPLE_CHOICE, correct = true).first
        scoring = scoring.grade(0, correct = true)

        assertEquals(setOf("w1"), scoring.missedItemIds)
        val stats = scoring.sessionStats(knownItemIds = setOf("w2"))
        assertEquals(2, stats.firstTryAnswers)
        assertEquals(1, stats.listeningAnswers)
        assertEquals(1, stats.newWords)
        assertEquals(1, stats.bestCombo)
    }

    @Test
    fun `a resume record round-trips and expires`() {
        val record = LessonResumeRecord("l1", listOf("a", "b", "c"), index = 1, scoring = LessonScoring().grade(0, true), savedAtEpochMillis = 1_000L)

        assertEquals(record, LessonResumeRecord.decodeOrNull(record.encode()))
        assertTrue(record.isFresh(1_000L + 60_000L))
        assertFalse(record.isFresh(1_000L + LessonResumeRecord.MAX_AGE_MILLIS + 1))
        assertFalse("nothing to resume at the very start", record.copy(index = 0).isFresh(1_000L))
        assertEquals(null, LessonResumeRecord.decodeOrNull("not json"))
    }
}
