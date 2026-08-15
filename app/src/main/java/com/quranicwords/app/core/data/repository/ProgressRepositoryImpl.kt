package com.quranicwords.app.core.data.repository

import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity
import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.ItemKind
import com.quranicwords.app.core.domain.model.LessonResult
import com.quranicwords.app.core.domain.model.REVIEW_SESSION_LESSON_ID
import com.quranicwords.app.core.domain.repository.ProgressRepository
import com.quranicwords.app.core.util.GamificationConfig
import com.quranicwords.app.core.util.StreakCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val database: QwDatabase,
    private val streakCalculator: StreakCalculator
) : ProgressRepository {

    override fun observeStats(userId: String): Flow<UserStatsEntity?> =
        database.userStatsDao().observe(userId)

    override fun observeProgress(userId: String): Flow<List<UserProgressEntity>> =
        database.userProgressDao().observeForUser(userId)

    override suspend fun ensureModuleStarted(userId: String, moduleId: String) = withContext(Dispatchers.IO) {
        val lessons = database.lessonDao().getForModule(moduleId)
        val firstLesson = lessons.minByOrNull { it.sortOrder } ?: return@withContext
        val existing = database.userProgressDao().get(userId, firstLesson.id)
        if (existing == null) {
            database.userProgressDao().upsert(
                UserProgressEntity(
                    userId = userId,
                    lessonId = firstLesson.id,
                    status = LessonStatus.UNLOCKED,
                    bestScorePercent = 0,
                    completedAtEpochMillis = null
                )
            )
        }
    }

    override suspend fun completeLesson(
        userId: String,
        lessonId: String,
        correctCount: Int,
        totalCount: Int
    ): LessonResult = withContext(Dispatchers.IO) {
        val points = GamificationConfig.pointsForLesson(correctCount, totalCount)
        val previousStats = database.userStatsDao().get(userId)
        val update = streakCalculator.recordActivity(previousStats, userId, points)
        database.userStatsDao().upsert(update.stats)

        val scorePercent = GamificationConfig.percentOf(correctCount, totalCount)
        val progress = UserProgressEntity(
            userId = userId,
            lessonId = lessonId,
            status = LessonStatus.COMPLETED,
            bestScorePercent = maxOf(
                scorePercent,
                database.userProgressDao().get(userId, lessonId)?.bestScorePercent ?: 0
            ),
            completedAtEpochMillis = System.currentTimeMillis()
        )
        database.userProgressDao().upsert(progress)
        val nextLessonId = unlockNextLesson(userId, lessonId)

        LessonResult(
            lessonId = lessonId,
            correctCount = correctCount,
            totalCount = totalCount,
            pointsAwarded = points,
            newTotalPoints = update.stats.totalPoints,
            currentStreak = update.stats.currentStreak,
            streakIncreased = update.streakIncreased,
            nextLessonId = nextLessonId
        )
    }

    /** Returns the id of the lesson it just unlocked (or that was already unlocked/completed),
     * or `null` if [completedLessonId] was the last lesson in its module - the caller uses this
     * to offer "continue straight into the next lesson" instead of always routing back to Home. */
    private suspend fun unlockNextLesson(userId: String, completedLessonId: String): String? {
        val completedLesson = database.lessonDao().getById(completedLessonId) ?: return null
        val lessonsInModule = database.lessonDao().getForModule(completedLesson.moduleId)
        val next = lessonsInModule
            .filter { it.sortOrder > completedLesson.sortOrder }
            .minByOrNull { it.sortOrder } ?: return null

        val existing = database.userProgressDao().get(userId, next.id)
        if (existing == null || existing.status == LessonStatus.LOCKED) {
            database.userProgressDao().upsert(
                UserProgressEntity(
                    userId = userId,
                    lessonId = next.id,
                    status = LessonStatus.UNLOCKED,
                    bestScorePercent = existing?.bestScorePercent ?: 0,
                    completedAtEpochMillis = existing?.completedAtEpochMillis
                )
            )
        }
        return next.id
    }

    override suspend fun logAttempt(
        userId: String,
        itemId: String,
        itemKind: ItemKind,
        exerciseType: ExerciseType,
        wasCorrect: Boolean
    ) = withContext(Dispatchers.IO) {
        database.exerciseAttemptDao().insert(
            ExerciseAttemptEntity(
                userId = userId,
                itemId = itemId,
                itemKind = itemKind,
                exerciseType = exerciseType,
                wasCorrect = wasCorrect,
                attemptedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getMissedItemIds(userId: String): List<String> = withContext(Dispatchers.IO) {
        database.exerciseAttemptDao().getMissedItemIds(userId)
    }

    override suspend fun getReviewExercises(missedItemIds: List<String>, limit: Int): List<ExerciseEntity> =
        withContext(Dispatchers.IO) {
            if (missedItemIds.isEmpty()) return@withContext emptyList()
            database.exerciseDao().getScoredExercisesForItems(missedItemIds).take(limit)
        }

    override suspend fun completeReviewSession(
        userId: String,
        correctCount: Int,
        totalCount: Int
    ): LessonResult = withContext(Dispatchers.IO) {
        val points = GamificationConfig.pointsForLesson(correctCount, totalCount)
        val previousStats = database.userStatsDao().get(userId)
        val update = streakCalculator.recordActivity(previousStats, userId, points)
        database.userStatsDao().upsert(update.stats)

        // Unlike completeLesson, there's no single lessonId here to attach a user_progress write
        // to. Stats/points/streak are still recorded locally the same way.
        LessonResult(
            lessonId = REVIEW_SESSION_LESSON_ID,
            correctCount = correctCount,
            totalCount = totalCount,
            pointsAwarded = points,
            newTotalPoints = update.stats.totalPoints,
            currentStreak = update.stats.currentStreak,
            streakIncreased = update.streakIncreased,
            nextLessonId = null
        )
    }
}
