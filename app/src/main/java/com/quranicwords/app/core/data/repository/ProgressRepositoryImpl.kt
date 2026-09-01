package com.quranicwords.app.core.data.repository

import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.data.local.entity.DailyPracticeEntity
import com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity
import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import com.quranicwords.app.core.domain.CurriculumUnlockResolver
import com.quranicwords.app.core.domain.OpenPracticePool
import com.quranicwords.app.core.domain.requiresPassingScore
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.ItemKind
import com.quranicwords.app.core.domain.model.LessonResult
import com.quranicwords.app.core.domain.model.LessonSessionType
import com.quranicwords.app.core.domain.model.REVIEW_SESSION_LESSON_ID
import com.quranicwords.app.core.domain.repository.ProgressRepository
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.util.GamificationConfig
import com.quranicwords.app.core.util.StreakCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val database: QwDatabase,
    private val streakCalculator: StreakCalculator,
    private val clock: Clock,
    private val preferences: UserPreferencesDataStore,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ProgressRepository {

    /** Rounds up so any real, non-zero session registers at least one minute - a 40-second
     * Review session shouldn't silently contribute 0 toward the daily goal. */
    private suspend fun recordDailyPracticeMinutes(userId: String, durationMillis: Long) {
        val minutes = ((durationMillis + 59_999L) / 60_000L).toInt()
        if (minutes <= 0) return
        database.dailyPracticeDao().addMinutes(userId, LocalDate.now(clock).toString(), minutes)
    }

    override fun observeStats(userId: String): Flow<UserStatsEntity?> =
        database.userStatsDao().observe(userId)

    override fun observeProgress(userId: String): Flow<List<UserProgressEntity>> =
        database.userProgressDao().observeForUser(userId)

    override suspend fun ensureCurriculumStarted(userId: String) = withContext(Dispatchers.IO) {
        val firstChapter = database.chapterDao().getAll().minByOrNull { it.sortOrder } ?: return@withContext
        val firstSection = database.sectionDao().getForChapter(firstChapter.id).minByOrNull { it.sortOrder }
            ?: return@withContext
        val firstLesson = database.lessonDao().getForSection(firstSection.id).minByOrNull { it.sortOrder }
            ?: return@withContext
        unlockIfNeeded(userId, firstLesson.id)
    }

    override suspend fun completeLesson(
        userId: String,
        lessonId: String,
        correctCount: Int,
        totalCount: Int,
        durationMillis: Long
    ): LessonResult = withContext(Dispatchers.IO) {
        val points = GamificationConfig.pointsForLesson(correctCount, totalCount)
        val previousStats = database.userStatsDao().get(userId)
        val update = streakCalculator.recordActivity(previousStats, userId, points)
        database.userStatsDao().upsert(update.stats)
        recordDailyPracticeMinutes(userId, durationMillis)

        val scorePercent = GamificationConfig.percentOf(correctCount, totalCount)
        val progress = UserProgressEntity(
            userId = userId,
            lessonId = lessonId,
            status = LessonStatus.COMPLETED,
            bestScorePercent = maxOf(
                scorePercent,
                database.userProgressDao().get(userId, lessonId)?.bestScorePercent ?: 0
            ),
            completedAtEpochMillis = System.currentTimeMillis(),
            durationMillis = durationMillis
        )
        database.userProgressDao().upsert(progress)

        val lesson = database.lessonDao().getById(lessonId)
        val passed = lesson != null &&
            (!lesson.kind.requiresPassingScore() || scorePercent >= GamificationConfig.PASSING_SCORE_PERCENT)
        val nextLessonId = if (lesson != null && passed) unlockNextLesson(userId, lesson) else null

        runCatching {
            com.quranicwords.app.feature.widget.WidgetUpdateScheduler.updateAllWidgets(context, advanceRotation = false)
        }

        LessonResult(
            lessonId = lessonId,
            correctCount = correctCount,
            totalCount = totalCount,
            pointsAwarded = points,
            newTotalPoints = update.stats.totalPoints,
            currentStreak = update.stats.currentStreak,
            streakIncreased = update.streakIncreased,
            nextLessonId = nextLessonId,
            lessonKind = lesson?.kind,
            durationMillis = durationMillis
        )
    }

    /** Loads the whole curriculum tree and delegates to the pure [CurriculumUnlockResolver] for
     * the actual "what's next" decision - see that object's doc comment for the three-tier
     * fallback it walks. Unlocks whatever it resolves to (if anything) and returns its id. Caller
     * has already confirmed [completedLesson] was actually passed (see [completeLesson]). */
    private suspend fun unlockNextLesson(userId: String, completedLesson: LessonEntity): String? {
        val nextId = CurriculumUnlockResolver.resolveNextLessonId(
            completedLessonId = completedLesson.id,
            lessons = database.lessonDao().getAll(),
            sections = database.sectionDao().getAll(),
            chapters = database.chapterDao().getAll()
        ) ?: return null
        unlockIfNeeded(userId, nextId)
        return nextId
    }

    private suspend fun unlockIfNeeded(userId: String, lessonId: String) {
        val existing = database.userProgressDao().get(userId, lessonId)
        if (existing == null || existing.status == LessonStatus.LOCKED) {
            database.userProgressDao().upsert(
                UserProgressEntity(
                    userId = userId,
                    lessonId = lessonId,
                    status = LessonStatus.UNLOCKED,
                    bestScorePercent = existing?.bestScorePercent ?: 0,
                    completedAtEpochMillis = existing?.completedAtEpochMillis
                )
            )
        }
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

    override fun observeMissedItemIds(userId: String): Flow<List<String>> =
        database.exerciseAttemptDao().observeMissedItemIds(userId)

    override suspend fun getMasteredItemIds(userId: String): List<String> = withContext(Dispatchers.IO) {
        database.exerciseAttemptDao().getMasteredItemIds(userId)
    }

    override suspend fun getDailyPracticeHistory(userId: String): List<DailyPracticeEntity> = withContext(Dispatchers.IO) {
        database.dailyPracticeDao().getAllForUserOnce(userId)
    }

    override fun observePracticeHistoryForRange(
        userId: String,
        startDate: String,
        endDate: String
    ): Flow<List<DailyPracticeEntity>> =
        database.dailyPracticeDao().observeForRange(userId, startDate, endDate)

    override fun observeTodayPractice(userId: String, localDate: String): Flow<DailyPracticeEntity?> =
        database.dailyPracticeDao().observe(userId, localDate)

    override suspend fun getReviewExercises(missedItemIds: List<String>, limit: Int): List<ExerciseEntity> =
        withContext(Dispatchers.IO) {
            if (missedItemIds.isEmpty()) return@withContext emptyList()
            database.exerciseDao().getScoredExercisesForItems(missedItemIds).take(limit)
        }

    override suspend fun completeReviewSession(
        userId: String,
        correctCount: Int,
        totalCount: Int,
        durationMillis: Long,
        sessionType: LessonSessionType
    ): LessonResult = withContext(Dispatchers.IO) {
        val points = GamificationConfig.pointsForLesson(correctCount, totalCount)
        val previousStats = database.userStatsDao().get(userId)
        val update = streakCalculator.recordActivity(previousStats, userId, points)
        database.userStatsDao().upsert(update.stats)
        recordDailyPracticeMinutes(userId, durationMillis)

        // Unlike completeLesson, there's no single lessonId here to attach a user_progress write
        // to. Stats/points/streak (and now daily practice minutes) are still recorded locally the
        // same way - a Review-only day still counts toward the daily goal.
        LessonResult(
            lessonId = REVIEW_SESSION_LESSON_ID,
            correctCount = correctCount,
            totalCount = totalCount,
            pointsAwarded = points,
            newTotalPoints = update.stats.totalPoints,
            currentStreak = update.stats.currentStreak,
            streakIncreased = update.streakIncreased,
            nextLessonId = null,
            durationMillis = durationMillis,
            sessionType = sessionType
        )
    }

    override suspend fun getOpenPracticeExercises(
        userId: String,
        mode: String,
        batchSize: Int
    ): List<ExerciseEntity> = withContext(Dispatchers.IO) {
        val allWords = database.wordFrequencyDao().observeAllByFrequency().first()
        if (allWords.isEmpty()) return@withContext emptyList()

        val upMode = mode.uppercase()
        val itemIds = when {
            upMode == "ISM" || upMode == "NOUN" -> {
                val ismWords = allWords.filter { it.id.startsWith("wn_") }
                val allIsmIds = ismWords.map { it.id }
                val covered = preferences.testIsmCoveredWordIdsFlow.first()
                val remaining = allIsmIds.filter { it !in covered }
                val pool = if (remaining.size < batchSize) {
                    preferences.resetTestIsmCoveredWordIds()
                    allIsmIds
                } else {
                    remaining
                }
                val sampled = pool.shuffled().take(batchSize)
                preferences.addTestIsmCoveredWordIds(sampled)
                sampled
            }
            upMode == "FIL" || upMode == "VERB" -> {
                val filWords = allWords.filter { it.id.startsWith("wv_") }
                val allFilIds = filWords.map { it.id }
                val covered = preferences.testFilCoveredWordIdsFlow.first()
                val remaining = allFilIds.filter { it !in covered }
                val pool = if (remaining.size < batchSize) {
                    preferences.resetTestFilCoveredWordIds()
                    allFilIds
                } else {
                    remaining
                }
                val sampled = pool.shuffled().take(batchSize)
                preferences.addTestFilCoveredWordIds(sampled)
                sampled
            }
            upMode == "HARF" || upMode == "PARTICLE" -> {
                val harfWords = allWords.filter { it.id.startsWith("wp_") }
                val allHarfIds = harfWords.map { it.id }
                val covered = preferences.testHarfCoveredWordIdsFlow.first()
                val remaining = allHarfIds.filter { it !in covered }
                val pool = if (remaining.size < batchSize) {
                    preferences.resetTestHarfCoveredWordIds()
                    allHarfIds
                } else {
                    remaining
                }
                val sampled = pool.shuffled().take(batchSize)
                preferences.addTestHarfCoveredWordIds(sampled)
                sampled
            }
            upMode == "MISTAKES" -> {
                val missed = database.exerciseAttemptDao().getMissedItemIds(userId)
                if (missed.isEmpty()) return@withContext emptyList()
                missed.shuffled().take(batchSize)
            }
            upMode == "FREQUENCY" -> {
                val offset = preferences.testFrequencyOffsetFlow.first()
                val safeOffset = if (offset >= allWords.size) 0 else offset
                val slice = allWords.drop(safeOffset).take(batchSize).map { it.id }
                preferences.setTestFrequencyOffset((safeOffset + slice.size) % allWords.size)
                slice
            }
            upMode.startsWith("CHAPTER:") || upMode.startsWith("CHAPTER_") -> {
                val rawChapter = if (upMode.startsWith("CHAPTER:")) mode.substring(8) else mode.substring(8)
                val cleanChapterId = if (rawChapter.startsWith("ch_")) rawChapter else "ch_${rawChapter.padStart(2, '0')}"
                val lessons = database.lessonDao().getForChapter(cleanChapterId)
                val lessonIds = lessons.map { it.id }
                val exercises = database.exerciseDao().getForLessons(lessonIds)
                val chapterWordIds = exercises.mapNotNull { it.practicedItemId }.distinct()
                if (chapterWordIds.isEmpty()) return@withContext emptyList()
                val covered = preferences.testChapterCoveredWordIdsFlow(cleanChapterId).first()
                val remaining = chapterWordIds.filter { it !in covered }
                val pool = if (remaining.size < batchSize) {
                    preferences.resetTestChapterCoveredWordIds(cleanChapterId)
                    chapterWordIds
                } else {
                    remaining
                }
                val sampled = pool.shuffled().take(batchSize)
                preferences.addTestChapterCoveredWordIds(cleanChapterId, sampled)
                sampled
            }
            else -> { // "RANDOM", "MIX"
                val allIds = allWords.map { it.id }
                val covered = preferences.testRandomCoveredWordIdsFlow.first()
                val remaining = allIds.filter { it !in covered }
                val pool = if (remaining.size < batchSize) {
                    preferences.resetTestRandomCoveredWordIds()
                    allIds
                } else {
                    remaining
                }
                val sampled = pool.shuffled().take(batchSize)
                preferences.addTestRandomCoveredWordIds(sampled)
                sampled
            }
        }

        val exercises = database.exerciseDao().getScoredExercisesForItems(itemIds)
        if (mode.uppercase() == "FREQUENCY") {
            val byItem = exercises.groupBy { it.practicedItemId }
            itemIds.mapNotNull { byItem[it]?.random() }
        } else {
            OpenPracticePool.oneExercisePerWord(exercises)
        }
    }

    override suspend fun getStreakRecoveryExercises(userId: String, count: Int): List<ExerciseEntity> =
        withContext(Dispatchers.IO) {
            // No full-corpus fallback here, unlike getOpenPracticeExercises - a recovery quiz must
            // only ever test words this specific learner has actually studied.
            val pool = database.exerciseAttemptDao().getAllPracticedItemIds(userId)
            sampleExercises(pool, count)
        }

    private suspend fun sampleExercises(pool: List<String>, count: Int): List<ExerciseEntity> {
        if (pool.isEmpty()) return emptyList()
        val sampledIds = OpenPracticePool.sampleIds(pool, count)
        val exercises = database.exerciseDao().getScoredExercisesForItems(sampledIds)
        return OpenPracticePool.oneExercisePerWord(exercises)
    }

    override suspend fun attemptStreakRecovery(userId: String, correctCount: Int, totalCount: Int): Boolean =
        withContext(Dispatchers.IO) {
            val passed = GamificationConfig.percentOf(correctCount, totalCount) >= GamificationConfig.PASSING_SCORE_PERCENT
            if (passed) {
                val stats = database.userStatsDao().get(userId) ?: return@withContext false
                database.userStatsDao().upsert(
                    stats.copy(
                        lastActivityLocalDate = LocalDate.now(clock).toString(),
                        longestStreak = maxOf(stats.longestStreak, stats.currentStreak)
                    )
                )
            }
            passed
        }

    override suspend fun resetProgress(userId: String): Unit = withContext(Dispatchers.IO) {
        database.userProgressDao().deleteForUser(userId)
        database.userStatsDao().deleteForUser(userId)
        database.exerciseAttemptDao().deleteForUser(userId)
        database.dailyPracticeDao().deleteForUser(userId)
        database.achievementDao().deleteForUser(userId)
        ensureCurriculumStarted(userId)
    }
}
