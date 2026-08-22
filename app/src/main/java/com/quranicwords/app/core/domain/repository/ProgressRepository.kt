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

    /** Ids whose most recent attempt was correct - the Progress tab's "words learned" metric.
     * See [com.quranicwords.app.core.data.local.dao.ExerciseAttemptDao.getMasteredItemIds]. */
    suspend fun getMasteredItemIds(userId: String): List<String>

    /** Full per-day practice-minutes history for the Progress tab's days-practiced heatmap and
     * daily-goal-streak tiles - a one-shot read (this screen doesn't need it to be live-observed
     * the way [observeProgress] does). */
    suspend fun getDailyPracticeHistory(userId: String): List<DailyPracticeEntity>

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

    /** A batch of up to [batchSize] scored exercises drawn from a flexible word pool rather than
     * one fixed lesson - the shared engine behind Test/Quiz-only mode's Home, the post-100%-
     * completion practice loop, and (via a stricter variant) streak recovery. Pool is every word
     * [userId] has ever attempted (`ExerciseAttemptDao.getAllPracticedItemIds`), falling back to
     * the full corpus only when that's empty (a brand-new Test/Quiz-only user's very first batch,
     * before they've attempted anything at all). See [com.quranicwords.app.core.domain
     * .OpenPracticePool] for the pure sampling/collapsing steps this delegates to. */
    suspend fun getOpenPracticeExercises(userId: String, batchSize: Int = 18): List<ExerciseEntity>
}
