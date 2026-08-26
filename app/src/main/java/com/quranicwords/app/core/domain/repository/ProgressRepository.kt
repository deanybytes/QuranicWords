package com.quranicwords.app.core.domain.repository

import com.quranicwords.app.core.data.local.entity.DailyPracticeEntity
import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.ItemKind
import com.quranicwords.app.core.domain.model.LessonResult
import com.quranicwords.app.core.domain.model.LessonSessionType
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeStats(userId: String): Flow<UserStatsEntity?>
    fun observeProgress(userId: String): Flow<List<UserProgressEntity>>

    /** Unlocks the very first lesson of the curriculum (chapter 1, section 1, lesson 1) for
     * [userId] if it has no progress row yet - the one bootstrap unlock every other unlock in the
     * chapter -> section -> lesson tree chains from via [completeLesson]. */
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
     * [completeLesson]'s aggregate `correctCount`/`totalCount` doesn't provide. */
    suspend fun logAttempt(
        userId: String,
        itemId: String,
        itemKind: ItemKind,
        exerciseType: ExerciseType,
        wasCorrect: Boolean
    )

    /** Ids whose most recent attempt was incorrect - drives adaptive sequencing and the Review
     * session. Empty if [userId] has no attempt history yet. */
    suspend fun getMissedItemIds(userId: String): List<String>

    /** Live Flow of [getMissedItemIds] - keeps Home and Review entry points reactive to every
     * new attempt. */
    fun observeMissedItemIds(userId: String): Flow<List<String>>

    /** Ids whose most recent attempt was correct - the Progress tab's "words learned" metric.
     * See [com.quranicwords.app.core.data.local.dao.ExerciseAttemptDao.getMasteredItemIds]. */
    suspend fun getMasteredItemIds(userId: String): List<String>

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
     * - "RANDOM": Random sampling across the entire 3,680-word corpus without repetition until the full corpus is completed.
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
