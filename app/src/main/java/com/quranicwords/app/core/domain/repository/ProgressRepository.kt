package com.quranicwords.app.core.domain.repository

import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.ItemKind
import com.quranicwords.app.core.domain.model.LessonResult
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
        totalCount: Int
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

    /** Assembles a session from [missedItemIds] instead of a fixed lesson - drills words the
     * learner has gotten wrong. Takes the ids directly (rather than a userId + re-querying
     * [getMissedItemIds] itself) since callers already have them from that call. Empty if
     * [missedItemIds] is empty. */
    suspend fun getReviewExercises(missedItemIds: List<String>, limit: Int = 20): List<ExerciseEntity>

    /** Sibling to [completeLesson] for a Review session, which has no single lesson to mark
     * complete: still awards points/streak the same way, but never writes to `user_progress` -
     * there's no lessonId for that write to attach to. */
    suspend fun completeReviewSession(userId: String, correctCount: Int, totalCount: Int): LessonResult
}
