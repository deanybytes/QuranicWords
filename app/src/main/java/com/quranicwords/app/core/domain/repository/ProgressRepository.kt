package com.quranicwords.app.core.domain.repository

import com.quranicwords.app.core.data.local.entity.DailyPracticeEntity
import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.ItemKind
import com.quranicwords.app.core.domain.model.LessonResult
import com.quranicwords.app.core.domain.model.LessonSessionType
import com.quranicwords.app.core.domain.srs.WordStrength
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeStats(userId: String): Flow<UserStatsEntity?>
    fun observeProgress(userId: String): Flow<List<UserProgressEntity>>

    /** Unlocks the very first lesson of the curriculum (chapter 1, section 1, lesson 1) for
     * [userId] if it has no progress row yet - the one bootstrap unlock every other unlock in the
     * chapter -> section -> lesson tree chains from via [completeLesson]. Also creates the
     * learner's stats row on first run (hearts on only for a genuinely new learner) and runs the
     * one-time `word_memory` backfill from attempt history for learners who predate it. */
    suspend fun ensureCurriculumStarted(userId: String)

    /**
     * Records a completed lesson/exam/flashback attempt: updates score/points/streak, persists
     * locally, and unlocks what comes next - unconditionally for a
     * [com.quranicwords.app.core.data.local.entity.LessonKind.REGULAR] lesson, only on a passing
     * score (see [com.quranicwords.app.core.util.GamificationConfig.PASSING_SCORE_PERCENT]) for
     * every other kind. A failed exam/flashback still records the attempt (score visible, retry
     * available) but leaves whatever comes next locked.
     */
    suspend fun completeLesson(
        userId: String,
        lessonId: String,
        correctCount: Int,
        totalCount: Int,
        durationMillis: Long
    ): LessonResult

    /** Logs one scored check against a single letter/word - the per-item signal
     * [completeLesson]'s aggregate `correctCount`/`totalCount` doesn't provide. [isFirstTry] is
     * false for a retry or an in-session repeat of an item already logged this session; such
     * rows are kept for history but never feed the missed/mastered sets. A first try also updates
     * the word's spaced-repetition memory (`word_memory`) in the same transaction - see
     * [com.quranicwords.app.core.domain.srs.WordMemoryRules.applyFirstTry]. */
    suspend fun logAttempt(
        userId: String,
        itemId: String,
        itemKind: ItemKind,
        exerciseType: ExerciseType,
        wasCorrect: Boolean,
        isFirstTry: Boolean = true
    )

    /** Words whose most recent first try was wrong (FSRS grade Again in `word_memory`), most
     * recently missed first - drives adaptive sequencing and the mistakes Review session. Empty if
     * [userId] has no memory yet. */
    suspend fun getMissedItemIds(userId: String): List<String>

    /** Live Flow of [getMissedItemIds] - keeps Home and Review entry points reactive to every
     * new attempt. */
    fun observeMissedItemIds(userId: String): Flow<List<String>>

    /** "Strong+" words (memory stability of at least
     * [com.quranicwords.app.core.domain.srs.WordStrength.STRONG_MIN_DAYS]) - the app's single
     * definition of a learned word, used by Progress, the lesson summary and Learned Words. */
    suspend fun getMasteredItemIds(userId: String): List<String>

    /** Live number of words whose review is due now - re-evaluated as time passes, not only when
     * the table changes, since a lapse becomes due again minutes later. */
    fun observeDueCount(userId: String): Flow<Int>

    /** Up to [limit] due word ids, most overdue (then least likely to be recalled) first. */
    suspend fun getDueItemIds(userId: String, limit: Int): List<String>

    /** The Daily Review session: one exercise per due word (capped at [limit]), its type chosen
     * by memory strength - see [com.quranicwords.app.core.domain.srs.ReviewExercisePicker].
     * [listeningEnabled] allows tap-what-you-hear for strong words with audio. */
    suspend fun getDailyReviewExercises(userId: String, limit: Int, listeningEnabled: Boolean): List<ExerciseEntity>

    /** Strength of every word [userId] has memory for (words absent from the map are NEW). */
    fun observeWordStrengths(userId: String): Flow<Map<String, WordStrength>>

    /** One-shot form of [observeWordStrengths]. */
    suspend fun getWordStrengths(userId: String): Map<String, WordStrength>

    /** How many remembered words sit in each strength bucket (NEW is never counted). */
    suspend fun getStrengthCounts(userId: String): Map<WordStrength, Int>

    /** Full per-day practice-minutes history for the Progress tab's days-practiced heatmap and
     * daily-goal-streak tiles - a one-shot read (this screen doesn't need it to be live-observed
     * the way [observeProgress] does). */
    suspend fun getDailyPracticeHistory(userId: String): List<DailyPracticeEntity>

    /** Live Flow of per-day practice rows for a date range - backs the 30-day activity trend chart. */
    fun observePracticeHistoryForRange(userId: String, startDate: String, endDate: String): Flow<List<DailyPracticeEntity>>

    /** Live today's-practice row, unlike [getDailyPracticeHistory] - backs Home's "daily challenge
     * completed" indicator, which needs to flip on the instant a lesson finishing today pushes
     * the learner over their goal, without waiting for [HomeViewModel] to be recreated. Null means
     * zero minutes practiced today (no row written yet), not an error. */
    fun observeTodayPractice(userId: String, localDate: String): Flow<DailyPracticeEntity?>

    /** Assembles a session from [missedItemIds] instead of a fixed lesson - drills words the
     * learner has gotten wrong. Takes the ids directly (rather than a userId + re-querying
     * [getMissedItemIds] itself) since callers already have them from that call. Empty if
     * [missedItemIds] is empty. */
    suspend fun getReviewExercises(missedItemIds: List<String>, limit: Int = 20): List<ExerciseEntity>

    /** Sibling to [completeLesson] for a Review session, which has no single lesson to mark
     * complete: still awards points/streak the same way, but never writes to `user_progress` -
     * there's no lessonId for that write to attach to. Also reused as-is for Open Practice (see
     * [getOpenPracticeExercises]) - identical completion semantics, distinguished only by the
     * [sessionType] the caller reports back on the result. */
    suspend fun completeReviewSession(
        userId: String,
        correctCount: Int,
        totalCount: Int,
        durationMillis: Long,
        sessionType: LessonSessionType = LessonSessionType.REVIEW
    ): LessonResult

    /** A batch of up to [batchSize] scored exercises drawn from a flexible word pool according
     * to the requested mode:
     * - "FREQUENCY": Sequential Quranic frequency order (Rank #1 upwards) advancing monotonically.
     * - "RANDOM": Random sampling across the whole word corpus without repetition until the full corpus is completed.
     * - "MISTAKES": Sourced from user's current mistaken/missed word list. */
    suspend fun getOpenPracticeExercises(userId: String, mode: String = "RANDOM", batchSize: Int = 18): List<ExerciseEntity>

    /** A strictly-scoped sibling of [getOpenPracticeExercises] for the streak-recovery quiz - only
     * ever draws from words [userId] has actually practiced (see
     * `ExerciseAttemptDao.getAllPracticedItemIds`), never the full-corpus fallback, since a
     * recovery quiz must never test a word the learner has never seen. */
    suspend fun getStreakRecoveryExercises(userId: String, count: Int): List<ExerciseEntity>

    /** Records a streak-recovery attempt - deliberately **not** routed through
     * [com.quranicwords.app.core.util.StreakCalculator.recordActivity] (see
     * [com.quranicwords.app.core.domain.StreakRecovery]'s doc comment for why that would clobber
     * the very streak this is trying to restore). On a pass (>= [com.quranicwords.app.core.util
     * .GamificationConfig.PASSING_SCORE_PERCENT]), stamps today as the last-activity date so the
     * streak is "alive" again and returns true; on a fail, leaves stats untouched and returns
     * false - the locked state simply persists until the next successful attempt or the streak
     * naturally resets via a normal lesson/review completion. */
    suspend fun attemptStreakRecovery(userId: String, correctCount: Int, totalCount: Int): Boolean

    /** The Settings "reset progress" action - wipes every progress/stats table for [userId]
     * (lesson unlocks/scores, points/streak, per-item attempt history, daily practice minutes,
     * achievements) and re-bootstraps lesson 1 via [ensureCurriculumStarted], leaving onboarding
     * preferences (language, learning path, font, etc.) untouched - this is a progress reset, not
     * a full app reset. */
    suspend fun resetProgress(userId: String)
}
