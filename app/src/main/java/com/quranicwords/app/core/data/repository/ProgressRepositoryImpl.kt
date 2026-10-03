package com.quranicwords.app.core.data.repository

import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.data.local.entity.DailyPracticeEntity
import com.quranicwords.app.core.data.local.entity.DailyQuestEntity
import com.quranicwords.app.core.domain.HeartsCalculator
import com.quranicwords.app.core.domain.QuestCatalog
import com.quranicwords.app.core.domain.QuestEligibility
import com.quranicwords.app.core.domain.model.isWithdrawn
import com.quranicwords.app.core.domain.QuestEvent
import com.quranicwords.app.core.domain.model.HeartsStatus
import com.quranicwords.app.core.domain.model.LearningPath
import com.quranicwords.app.core.domain.model.SessionStats
import com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity
import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import com.quranicwords.app.core.domain.srs.FsrsScheduler
import com.quranicwords.app.core.domain.srs.ReviewExercisePicker
import com.quranicwords.app.core.domain.srs.WordMemoryRules
import com.quranicwords.app.core.domain.srs.WordStrength
import com.quranicwords.app.core.domain.CurriculumUnlockResolver
import com.quranicwords.app.core.domain.OpenPracticePool
import com.quranicwords.app.core.domain.StreakRecovery
import com.quranicwords.app.core.data.local.chunkedInQuery
import com.quranicwords.app.core.domain.model.ChoiceOption
import com.quranicwords.app.core.domain.model.GeneratedExercisePrompts
import com.quranicwords.app.core.domain.model.cleanArabicDisplay
import com.quranicwords.app.core.domain.requiresPassingScore
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.ItemKind
import com.quranicwords.app.core.domain.model.LemmaCategory
import com.quranicwords.app.core.domain.model.LessonResult
import com.quranicwords.app.core.domain.model.LessonSessionType
import com.quranicwords.app.core.domain.model.REVIEW_SESSION_LESSON_ID
import com.quranicwords.app.core.domain.repository.ProgressRepository
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.util.GamificationConfig
import com.quranicwords.app.core.util.StreakCalculator
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.WordSpan
import com.quranicwords.app.core.util.AppJson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val database: QwDatabase,
    private val streakCalculator: StreakCalculator,
    private val clock: Clock,
    private val preferences: UserPreferencesDataStore,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val contentRepository: ContentRepository? = null,
    private val applicationScope: CoroutineScope? = null
) : ProgressRepository {

    private val curriculumMutex = Mutex()

    /** Pure FSRS scheduler on the repository's own clock - see [WordMemoryRules]. */
    private val scheduler = FsrsScheduler(clock)

    /** Floors to whole minutes - rounding up let a string of quick sessions (or a lesson left
     * open in the background) inflate the daily goal well past the time actually practiced.
     * [durationMillis] is already active foreground time only (see LessonViewModel's timer). */
    private suspend fun recordDailyPracticeMinutes(userId: String, durationMillis: Long) {
        val minutes = (durationMillis / 60_000L).toInt()
        if (minutes <= 0) return
        database.dailyPracticeDao().addMinutes(userId, LocalDate.now(clock).toString(), minutes)
    }

    override fun observeStats(userId: String): Flow<UserStatsEntity?> =
        database.userStatsDao().observe(userId)

    override fun observeProgress(userId: String): Flow<List<UserProgressEntity>> =
        database.userProgressDao().observeForUser(userId)

    override suspend fun ensureCurriculumStarted(userId: String) = withContext(Dispatchers.IO) {
        // Home, Roadmap and the bottom-nav shell all call this on open; serialize them so their
        // read-modify-write unlock passes can't interleave.
        curriculumMutex.withLock {
            database.withTransaction {
                ensureStatsRow(userId)
                repairUnlockChain(userId)
            }
            backfillWordMemoryIfNeeded(userId)
        }
    }

    /** Creates [userId]'s stats row the first time the curriculum is opened. Hearts start on only
     * for a genuinely new learner (nothing answered, nothing completed) - a learner upgrading
     * from a build without hearts already has a row (migrated with hearts off) or, at worst,
     * history that marks them as not new. Must run inside a transaction. */
    private suspend fun ensureStatsRow(userId: String) {
        if (database.userStatsDao().get(userId) != null) return
        val hasHistory = database.exerciseAttemptDao().hasAnyForUser(userId) ||
            database.userProgressDao().getAllForUserOnce(userId).any { it.status == LessonStatus.COMPLETED }
        database.userStatsDao().upsert(
            UserStatsEntity(
                userId = userId,
                totalPoints = 0,
                currentStreak = 0,
                longestStreak = 0,
                lastActivityLocalDate = null,
                heartsUpdatedAtEpochMillis = clock.millis(),
                heartsEnabled = !hasHistory
            )
        )
    }

    /** One-time replay of pre-v7 attempt history into `word_memory` (see
     * [WordMemoryRules.backfill]). Insert-if-absent, so a row a live session already wrote wins,
     * and the DataStore marker is only set once the rows are committed - an interrupted backfill
     * simply runs again next launch and produces the same rows. */
    private suspend fun backfillWordMemoryIfNeeded(userId: String) {
        if (preferences.isWordMemoryBackfilled()) return
        database.withTransaction {
            val rows = WordMemoryRules.backfill(
                userId = userId,
                attempts = database.exerciseAttemptDao().getAllForUser(userId),
                zone = clock.zone,
                nowMillis = clock.millis(),
                scheduler = scheduler
            )
            if (rows.isNotEmpty()) database.wordMemoryDao().insertAllIfAbsent(rows)
        }
        preferences.setWordMemoryBackfilled(true)
    }

    /** Single pass over the learner's progress: unlock the very first lesson, auto-complete a
     * chapter's intro once any of its lessons is done, and make sure every passed lesson has its
     * successor unlocked. Must run inside a transaction. */
    private suspend fun repairUnlockChain(userId: String) {
        val allLessons = database.lessonDao().getAll()
        val allSections = database.sectionDao().getAll()
        val allChapters = database.chapterDao().getAll()

        val firstChapter = allChapters.minByOrNull { it.sortOrder } ?: return
        val firstSection = allSections.filter { it.chapterId == firstChapter.id }.minByOrNull { it.sortOrder }
            ?: return
        val lessonsBySection = allLessons.groupBy { it.sectionId }
        val sectionLessons = lessonsBySection[firstSection.id].orEmpty().sortedBy { it.sortOrder }
        val firstLesson = sectionLessons.firstOrNull() ?: return
        unlockIfNeeded(userId, firstLesson.id)

        // If the first lesson is CHAPTER_INTRO, ensure the first playable content lesson is also unlocked
        if (firstLesson.kind == LessonKind.CHAPTER_INTRO && sectionLessons.size > 1) {
            unlockIfNeeded(userId, sectionLessons[1].id)
        }

        val progressByLesson = database.userProgressDao().getAllForUserOnce(userId).associateBy { it.lessonId }
        val completedProgress = progressByLesson.values.filter { it.status == LessonStatus.COMPLETED }
        val lessonsById = allLessons.associateBy { it.id }
        val introsByChapter = allLessons.filter { it.kind == LessonKind.CHAPTER_INTRO }.groupBy { it.chapterId }
        val healedIntros = mutableSetOf<String>()

        for (prog in completedProgress) {
            val les = lessonsById[prog.lessonId] ?: continue
            // If any non-intro lesson in a chapter is completed, its CHAPTER_INTRO counts as completed too
            if (les.kind != LessonKind.CHAPTER_INTRO) {
                for (intro in introsByChapter[les.chapterId].orEmpty()) {
                    if (intro.id in healedIntros) continue
                    if (progressByLesson[intro.id]?.status != LessonStatus.COMPLETED) {
                        database.userProgressDao().upsert(
                            UserProgressEntity(
                                userId = userId,
                                lessonId = intro.id,
                                status = LessonStatus.COMPLETED,
                                bestScorePercent = 100,
                                completedAtEpochMillis = prog.completedAtEpochMillis ?: clock.millis(),
                                durationMillis = 0L
                            )
                        )
                    }
                    healedIntros += intro.id
                }
            }
            val passed = !les.kind.requiresPassingScore() || prog.bestScorePercent >= GamificationConfig.PASSING_SCORE_PERCENT
            if (passed) {
                val nextId = CurriculumUnlockResolver.resolveNextLessonId(
                    completedLessonId = les.id,
                    lessons = allLessons,
                    sections = allSections,
                    chapters = allChapters
                )
                if (nextId != null) {
                    unlockIfNeeded(userId, nextId)
                    val nextLesson = lessonsById[nextId]
                    if (nextLesson?.kind == LessonKind.CHAPTER_INTRO && nextLesson.sectionId != null) {
                        val secLessons = lessonsBySection[nextLesson.sectionId].orEmpty().sortedBy { it.sortOrder }
                        if (secLessons.size > 1) {
                            unlockIfNeeded(userId, secLessons[1].id)
                        }
                    }
                }
            }
        }
    }

    override suspend fun completeLesson(
        userId: String,
        lessonId: String,
        correctCount: Int,
        totalCount: Int,
        durationMillis: Long,
        session: SessionStats
    ): LessonResult = withContext(Dispatchers.IO) {
        val eligibility = questEligibility(userId)
        val result = database.withTransaction {
            val lesson = database.lessonDao().getById(lessonId)
            val scorePercent = GamificationConfig.percentOf(correctCount, totalCount)
            val passed = lesson != null &&
                (!lesson.kind.requiresPassingScore() || scorePercent >= GamificationConfig.PASSING_SCORE_PERCENT)
            val existing = database.userProgressDao().get(userId, lessonId)
            // A failed exam/flashback still counts as practice (streak, minutes) but earns nothing
            // and must not be recorded as completed - Home checkmarks, chapter completion and
            // coverage all key off COMPLETED. A replay of a completed lesson pays half.
            val xp = sessionXp(correctCount, totalCount, session, paid = passed, replay = existing?.status == LessonStatus.COMPLETED)

            val previousStats = database.userStatsDao().get(userId)
            val update = streakCalculator.recordActivity(previousStats, userId, xp.points)
            recordDailyPracticeMinutes(userId, durationMillis)
            val finishedALesson = passed && lesson.kind != LessonKind.CHAPTER_INTRO
            val quests = advanceQuests(userId, eligibility, session, xp.points, LessonSessionType.LESSON, lessonsFinished = if (finishedALesson) 1 else 0)
            val stats = update.stats.copy(
                totalPoints = update.stats.totalPoints + quests.rewardXp,
                bestCombo = maxOf(update.stats.bestCombo, session.bestCombo)
            )
            database.userStatsDao().upsert(stats)

            val now = clock.millis()
            val progress = if (passed) {
                UserProgressEntity(
                    userId = userId,
                    lessonId = lessonId,
                    status = LessonStatus.COMPLETED,
                    bestScorePercent = maxOf(scorePercent, existing?.bestScorePercent ?: 0),
                    completedAtEpochMillis = now,
                    durationMillis = durationMillis
                )
            } else {
                UserProgressEntity(
                    userId = userId,
                    lessonId = lessonId,
                    status = if (existing?.status == LessonStatus.COMPLETED) LessonStatus.COMPLETED else LessonStatus.UNLOCKED,
                    bestScorePercent = maxOf(scorePercent, existing?.bestScorePercent ?: 0),
                    completedAtEpochMillis = existing?.completedAtEpochMillis,
                    durationMillis = existing?.durationMillis ?: durationMillis
                )
            }
            database.userProgressDao().upsert(progress)

            // If a non-intro lesson in a chapter is completed, auto-complete preceding CHAPTER_INTRO in that chapter
            if (lesson != null && passed && lesson.kind != LessonKind.CHAPTER_INTRO) {
                val intros = database.lessonDao().getForChapter(lesson.chapterId)
                    .filter { it.kind == LessonKind.CHAPTER_INTRO }
                for (intro in intros) {
                    val existingIntro = database.userProgressDao().get(userId, intro.id)
                    if (existingIntro == null || existingIntro.status != LessonStatus.COMPLETED) {
                        database.userProgressDao().upsert(
                            UserProgressEntity(
                                userId = userId,
                                lessonId = intro.id,
                                status = LessonStatus.COMPLETED,
                                bestScorePercent = 100,
                                completedAtEpochMillis = now,
                                durationMillis = 0L
                            )
                        )
                    }
                }
            }

            val nextLessonId = if (lesson != null && passed) unlockNextLesson(userId, lesson) else null

            LessonResult(
                lessonId = lessonId,
                correctCount = correctCount,
                totalCount = totalCount,
                pointsAwarded = xp.points,
                newTotalPoints = stats.totalPoints,
                currentStreak = stats.currentStreak,
                streakIncreased = update.streakIncreased,
                nextLessonId = nextLessonId,
                lessonKind = lesson?.kind,
                durationMillis = durationMillis,
                basePoints = xp.base,
                perfectBonus = xp.perfect,
                comboBonus = xp.combo,
                replayDeduction = xp.replayDeduction,
                questRewardXp = quests.rewardXp,
                completedQuestIds = quests.completedQuestIds,
                bestCombo = session.bestCombo
            )
        }
        refreshWidgetsInBackground()
        result
    }

    /** One session's XP: base + perfect bonus + combo bonus, nothing when [paid] is false (a
     * failed gated lesson), and [GamificationConfig.REPLAY_POINTS_PERCENT] of it on a [replay]. */
    private fun sessionXp(correctCount: Int, totalCount: Int, session: SessionStats, paid: Boolean, replay: Boolean): SessionXp {
        if (!paid) return SessionXp(0, 0, 0, 0)
        val base = correctCount * GamificationConfig.POINTS_PER_CORRECT_ANSWER
        val perfect = GamificationConfig.perfectBonus(correctCount, totalCount)
        val combo = session.comboBonusXp.coerceAtLeast(0)
        val earned = base + perfect + combo
        val paidOut = if (replay) earned * GamificationConfig.REPLAY_POINTS_PERCENT / 100 else earned
        return SessionXp(base, perfect, combo, earned - paidOut)
    }

    private data class SessionXp(val base: Int, val perfect: Int, val combo: Int, val replayDeduction: Int) {
        val points: Int get() = base + perfect + combo - replayDeduction
    }

    /** Preference reads for quest selection - done before a transaction opens, not inside it. */
    private suspend fun questEligibility(userId: String): QuestEligibility = QuestEligibility(
        dueReviewCount = database.wordMemoryDao().getDueCount(userId, clock.millis()),
        lessonsAvailable = preferences.learningPathFlow.first() == LearningPath.LEARN,
        dailyGoalMinutes = preferences.dailyGoalLevelFlow.first().minutes
    )

    /** Today's quest rows, picking them (and pruning older days) on first use. Must run inside a
     * transaction, so two sessions finishing at once can't both pick. */
    private suspend fun ensureQuests(userId: String, localDate: String, eligibility: QuestEligibility): List<DailyQuestEntity> {
        val dao = database.dailyQuestDao()
        val existing = dao.getForDay(userId, localDate)
        if (existing.isNotEmpty()) return existing
        dao.deleteBefore(userId, localDate)
        dao.insertAllIfAbsent(QuestCatalog.toEntities(userId, localDate, QuestCatalog.questsFor(userId, localDate, eligibility)))
        return dao.getForDay(userId, localDate)
    }

    /** Applies a finished session to today's quests and returns the XP their completion pays -
     * each quest's reward is paid once (see [QuestCatalog.apply]). Must run inside the session's
     * transaction, after today's practice minutes were recorded. */
    private suspend fun advanceQuests(
        userId: String,
        eligibility: QuestEligibility,
        session: SessionStats,
        xpEarned: Int,
        sessionType: LessonSessionType,
        lessonsFinished: Int
    ): com.quranicwords.app.core.domain.QuestUpdate {
        val today = LocalDate.now(clock).toString()
        val quests = ensureQuests(userId, today, eligibility)
        val event = QuestEvent(
            reviewedWords = if (sessionType == LessonSessionType.REVIEW) session.firstTryAnswers else 0,
            bestCombo = session.bestCombo,
            lessonsFinished = lessonsFinished,
            xpEarned = xpEarned,
            todayMinutes = database.dailyPracticeDao().get(userId, today)?.minutesPracticed ?: 0,
            newWords = session.newWords
        )
        val update = QuestCatalog.apply(quests, event, clock.millis())
        if (update.changed.isNotEmpty()) database.dailyQuestDao().updateAll(update.changed)
        return update
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeQuests(userId: String, localDate: String): Flow<List<DailyQuestEntity>> =
        flow {
            val eligibility = questEligibility(userId)
            database.withTransaction { ensureQuests(userId, localDate, eligibility) }
            emit(Unit)
        }.flatMapLatest { database.dailyQuestDao().observeForDay(userId, localDate) }

    private fun heartsStatusOf(stats: UserStatsEntity?, nowMillis: Long): HeartsStatus {
        if (stats == null || !stats.heartsEnabled) return HeartsStatus(enabled = false, hearts = HeartsCalculator.MAX_HEARTS, nextHeartAtMillis = null)
        val current = HeartsCalculator.current(stats.hearts, stats.heartsUpdatedAtEpochMillis, nowMillis)
        val next = HeartsCalculator.millisUntilNext(current.hearts, current.anchorMillis, nowMillis)?.let { nowMillis + it }
        return HeartsStatus(enabled = true, hearts = current.hearts, nextHeartAtMillis = next)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeHearts(userId: String): Flow<HeartsStatus> =
        database.userStatsDao().observe(userId).flatMapLatest { stats ->
            flow {
                while (true) {
                    emit(heartsStatusOf(stats, clock.millis()))
                    delay(HEARTS_REFRESH_MILLIS)
                }
            }
        }.distinctUntilChanged()

    override suspend fun getHearts(userId: String): HeartsStatus = withContext(Dispatchers.IO) {
        heartsStatusOf(database.userStatsDao().get(userId), clock.millis())
    }

    override suspend fun loseHeart(userId: String): HeartsStatus = withContext(Dispatchers.IO) {
        database.withTransaction {
            val stats = database.userStatsDao().get(userId)
            val now = clock.millis()
            if (stats == null || !stats.heartsEnabled) return@withTransaction heartsStatusOf(stats, now)
            val next = HeartsCalculator.lose(stats.hearts, stats.heartsUpdatedAtEpochMillis, now)
            val updated = stats.copy(hearts = next.hearts, heartsUpdatedAtEpochMillis = next.anchorMillis)
            database.userStatsDao().upsert(updated)
            heartsStatusOf(updated, now)
        }
    }

    override suspend fun setHeartsEnabled(userId: String, enabled: Boolean): Unit = withContext(Dispatchers.IO) {
        database.withTransaction {
            ensureStatsRow(userId)
            val stats = database.userStatsDao().get(userId) ?: return@withTransaction
            database.userStatsDao().upsert(
                stats.copy(
                    heartsEnabled = enabled,
                    hearts = if (enabled && !stats.heartsEnabled) HeartsCalculator.MAX_HEARTS else stats.hearts,
                    heartsUpdatedAtEpochMillis = clock.millis()
                )
            )
        }
    }

    /** Widgets decode a lot of content to render; never make the lesson summary wait on that. */
    private fun refreshWidgetsInBackground() {
        val refresh: suspend () -> Unit = {
            runCatching {
                com.quranicwords.app.feature.widget.WidgetUpdateScheduler.updateAllWidgets(context, advanceRotation = false)
            }
        }
        applicationScope?.launch(Dispatchers.IO) { refresh() }
    }

    /** Loads the whole curriculum tree and delegates to the pure [CurriculumUnlockResolver] for
     * the actual "what's next" decision - see that object's doc comment for the three-tier
     * fallback it walks. Unlocks whatever it resolves to (if anything) and returns its id. Caller
     * has already confirmed [completedLesson] was actually passed (see [completeLesson]). */
    private suspend fun unlockNextLesson(userId: String, completedLesson: LessonEntity): String? {
        val allLessons = database.lessonDao().getAll()
        val nextId = CurriculumUnlockResolver.resolveNextLessonId(
            completedLessonId = completedLesson.id,
            lessons = allLessons,
            sections = database.sectionDao().getAll(),
            chapters = database.chapterDao().getAll()
        ) ?: return null
        unlockIfNeeded(userId, nextId)

        val nextLesson = allLessons.find { it.id == nextId }
        if (nextLesson?.kind == LessonKind.CHAPTER_INTRO && nextLesson.sectionId != null) {
            val sectionLessons = allLessons.filter { it.sectionId == nextLesson.sectionId }.sortedBy { it.sortOrder }
            if (sectionLessons.size > 1) {
                unlockIfNeeded(userId, sectionLessons[1].id)
            }
        }

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
        wasCorrect: Boolean,
        isFirstTry: Boolean
    ) = withContext(Dispatchers.IO) {
        val now = clock.millis()
        // Attempt row and memory update commit together: a crash between them would otherwise
        // leave a first try in history that the scheduler never saw (or the reverse).
        database.withTransaction {
            database.exerciseAttemptDao().insert(
                ExerciseAttemptEntity(
                    userId = userId,
                    itemId = itemId,
                    itemKind = itemKind,
                    exerciseType = exerciseType,
                    wasCorrect = wasCorrect,
                    attemptedAtEpochMillis = now,
                    isFirstTry = isFirstTry
                )
            )
            if (isFirstTry) {
                val memory = database.wordMemoryDao()
                memory.upsert(
                    WordMemoryRules.applyFirstTry(
                        existing = memory.get(userId, itemId),
                        userId = userId,
                        itemId = itemId,
                        correct = wasCorrect,
                        nowMillis = now,
                        localDate = LocalDate.now(clock).toString(),
                        scheduler = scheduler
                    )
                )
            }
        }
    }

    override suspend fun getMissedItemIds(userId: String): List<String> = withContext(Dispatchers.IO) {
        database.wordMemoryDao().getWeakItemIds(userId)
    }

    override fun observeMissedItemIds(userId: String): Flow<List<String>> =
        database.wordMemoryDao().observeWeakItemIds(userId)

    override suspend fun getMasteredItemIds(userId: String): List<String> = withContext(Dispatchers.IO) {
        database.wordMemoryDao().getItemIdsWithMinStability(userId, WordStrength.LEARNED_MIN_DAYS)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeDueCount(userId: String): Flow<Int> =
        flow {
            while (true) {
                emit(clock.millis())
                delay(DUE_COUNT_REFRESH_MILLIS)
            }
        }.flatMapLatest { now -> database.wordMemoryDao().observeDueCount(userId, now) }
            .distinctUntilChanged()

    override suspend fun getDueItemIds(userId: String, limit: Int): List<String> = withContext(Dispatchers.IO) {
        database.wordMemoryDao().getDueItemIds(userId, clock.millis(), limit)
    }

    override suspend fun getDailyReviewExercises(
        userId: String,
        limit: Int
    ): List<ExerciseEntity> = withContext(Dispatchers.IO) {
        val dueIds = database.wordMemoryDao().getDueItemIds(userId, clock.millis(), limit)
        if (dueIds.isEmpty()) return@withContext emptyList()
        val strengths = database.wordMemoryDao().getStabilities(userId)
            .associate { it.itemId to WordStrength.fromStability(it.stability) }
        ReviewExercisePicker.pick(
            orderedItemIds = dueIds,
            exercisesByItem = scoredExercisesFor(dueIds).groupBy { it.practicedItemId.orEmpty() },
            strengthByItem = strengths
        )
    }

    override fun observeWordStrengths(userId: String): Flow<Map<String, WordStrength>> =
        database.wordMemoryDao().observeStabilities(userId).map { rows ->
            rows.associate { it.itemId to WordStrength.fromStability(it.stability) }
        }

    override suspend fun getWordStrengths(userId: String): Map<String, WordStrength> = withContext(Dispatchers.IO) {
        database.wordMemoryDao().getStabilities(userId).associate { it.itemId to WordStrength.fromStability(it.stability) }
    }

    override suspend fun getStrengthCounts(userId: String): Map<WordStrength, Int> = withContext(Dispatchers.IO) {
        database.wordMemoryDao().getStabilities(userId)
            .groupingBy { WordStrength.fromStability(it.stability) }
            .eachCount()
            .filterKeys { it != WordStrength.NEW }
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
            // Chunked (a long mistake history overflows SQLite's IN-parameter limit), one exercise
            // per word (a word drilled by several exercises would otherwise crowd out the rest of
            // the list) and shuffled before the cap so a session isn't always the same first N.
            val exercises = scoredExercisesFor(missedItemIds)
            OpenPracticePool.oneExercisePerWord(exercises).take(limit)
        }

    private suspend fun scoredExercisesFor(itemIds: Collection<String>): List<ExerciseEntity> =
        chunkedInQuery(itemIds) { database.exerciseDao().getScoredExercisesForItems(it) }
            .filter { !it.type.isWithdrawn }

    override suspend fun completeReviewSession(
        userId: String,
        correctCount: Int,
        totalCount: Int,
        durationMillis: Long,
        sessionType: LessonSessionType,
        session: SessionStats
    ): LessonResult = withContext(Dispatchers.IO) {
        val eligibility = questEligibility(userId)
        database.withTransaction {
            val xp = sessionXp(correctCount, totalCount, session, paid = true, replay = false)
            val previousStats = database.userStatsDao().get(userId)
            val update = streakCalculator.recordActivity(previousStats, userId, xp.points)
            recordDailyPracticeMinutes(userId, durationMillis)
            val quests = advanceQuests(userId, eligibility, session, xp.points, sessionType, lessonsFinished = 0)

            // Review and practice never cost hearts and each finished one restores a heart - the
            // friendly way back after running out in a lesson.
            val now = clock.millis()
            val withStreak = update.stats
            val refill = if (withStreak.heartsEnabled && totalCount > 0) {
                HeartsCalculator.gain(withStreak.hearts, withStreak.heartsUpdatedAtEpochMillis, now, GamificationConfig.HEARTS_PER_REVIEW_SESSION)
            } else {
                null
            }
            val stats = withStreak.copy(
                totalPoints = withStreak.totalPoints + quests.rewardXp,
                bestCombo = maxOf(withStreak.bestCombo, session.bestCombo),
                hearts = refill?.hearts ?: withStreak.hearts,
                heartsUpdatedAtEpochMillis = refill?.anchorMillis ?: withStreak.heartsUpdatedAtEpochMillis
            )
            database.userStatsDao().upsert(stats)

            // Unlike completeLesson, there's no single lessonId here to attach a user_progress write
            // to. Stats/points/streak (and daily practice minutes) are still recorded locally the
            // same way - a Review-only day still counts toward the daily goal.
            LessonResult(
                lessonId = REVIEW_SESSION_LESSON_ID,
                correctCount = correctCount,
                totalCount = totalCount,
                pointsAwarded = xp.points,
                newTotalPoints = stats.totalPoints,
                currentStreak = stats.currentStreak,
                streakIncreased = update.streakIncreased,
                nextLessonId = null,
                durationMillis = durationMillis,
                sessionType = sessionType,
                basePoints = xp.base,
                perfectBonus = xp.perfect,
                comboBonus = xp.combo,
                questRewardXp = quests.rewardXp,
                completedQuestIds = quests.completedQuestIds,
                bestCombo = session.bestCombo
            )
        }
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
            upMode == "ISM" || upMode == "NOUN" -> sampleCategory(
                allWords.map { it.id }, LemmaCategory.NOUN, batchSize,
                covered = preferences.testIsmCoveredWordIdsFlow.first(),
                reset = { preferences.resetTestIsmCoveredWordIds() },
                markCovered = { preferences.addTestIsmCoveredWordIds(it) }
            )
            upMode == "FIL" || upMode == "VERB" -> sampleCategory(
                allWords.map { it.id }, LemmaCategory.VERB, batchSize,
                covered = preferences.testFilCoveredWordIdsFlow.first(),
                reset = { preferences.resetTestFilCoveredWordIds() },
                markCovered = { preferences.addTestFilCoveredWordIds(it) }
            )
            upMode == "HARF" || upMode == "PARTICLE" -> sampleCategory(
                allWords.map { it.id }, LemmaCategory.PARTICLE, batchSize,
                covered = preferences.testHarfCoveredWordIdsFlow.first(),
                reset = { preferences.resetTestHarfCoveredWordIds() },
                markCovered = { preferences.addTestHarfCoveredWordIds(it) }
            )
            upMode == "MISTAKES" -> {
                val missed = database.wordMemoryDao().getWeakItemIds(userId)
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
                val exercises = chunkedInQuery(lessonIds) { database.exerciseDao().getForLessons(it) }
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

        val dbExercises = scoredExercisesFor(itemIds)
        val wordIntros = contentRepository?.getWordIntrosForItems(itemIds).orEmpty()

        if (wordIntros.isEmpty()) {
            return@withContext if (mode.uppercase() == "FREQUENCY") {
                val byItem = dbExercises.groupBy { it.practicedItemId }
                itemIds.mapNotNull { byItem[it]?.random() }
            } else {
                OpenPracticePool.oneExercisePerWord(dbExercises)
            }
        }

        val dbByItem = dbExercises.groupBy { it.practicedItemId }
        val orderedItems = if (mode.uppercase() == "FREQUENCY") itemIds else itemIds.shuffled()

        orderedItems.mapIndexedNotNull { index, wordId ->
            val intro = wordIntros[wordId]
            val existing = dbByItem[wordId]?.randomOrNull()

            if (intro != null && !intro.exampleVerseArabic.isNullOrBlank() && intro.arabicWordStart != null && intro.arabicWordEnd != null) {
                val spans = computeWordSpans(intro.exampleVerseArabic)
                val wStart = intro.arabicWordStart
                val wEnd = intro.arabicWordEnd
                val targetSpan = spans.firstOrNull { it.start == wStart && it.end == wEnd }
                    ?: spans.firstOrNull { it.start <= wStart && wEnd <= it.end }

                when (index % 3) {
                    1 -> {
                        // TapWordInVerse (Reverse Verse Quiz)
                        if (targetSpan != null) {
                            val tapContent = ExerciseContent.TapWordInVerse(
                                prompt = GeneratedExercisePrompts.TAP_WORD_PROMPT,
                                wordId = wordId,
                                verseArabic = intro.exampleVerseArabic,
                                verseReference = intro.exampleVerseReference.orEmpty(),
                                correctWordStart = targetSpan.start,
                                correctWordEnd = targetSpan.end,
                                tappableSpans = spans,
                                meaning = intro.meaning,
                                verseTranslation = intro.exampleVerseTranslation,
                                meaningHighlight = intro.meaningHighlight
                            )
                            ExerciseEntity(
                                id = "test_tap_${wordId}_$index",
                                lessonId = REVIEW_SESSION_LESSON_ID,
                                orderIndex = index,
                                type = ExerciseType.WORD_IN_VERSE_TAP,
                                contentJson = AppJson.encodeToString(ExerciseContent.serializer(), tapContent),
                                practicedItemId = wordId
                            )
                        } else {
                            existing ?: buildDefaultMultipleChoice(wordId, intro, index)
                        }
                    }
                    2 -> {
                        // FillInTheBlank (Verse Completion)
                        if (targetSpan != null) {
                            val fillContent = ExerciseContent.FillInTheBlank(
                                prompt = GeneratedExercisePrompts.FILL_BLANK_PROMPT,
                                wordId = wordId,
                                sentenceArabic = intro.exampleVerseArabic,
                                blankStart = targetSpan.start,
                                blankEnd = targetSpan.end,
                                sentenceTranslation = intro.exampleVerseTranslation,
                                sentenceReference = intro.exampleVerseReference.orEmpty(),
                                options = listOf(correctOptionFor(wordId, intro)),
                                correctOptionId = wordId
                            )
                            ExerciseEntity(
                                id = "test_fill_${wordId}_$index",
                                lessonId = REVIEW_SESSION_LESSON_ID,
                                orderIndex = index,
                                type = ExerciseType.FILL_IN_THE_BLANK,
                                contentJson = AppJson.encodeToString(ExerciseContent.serializer(), fillContent),
                                practicedItemId = wordId
                            )
                        } else {
                            existing ?: buildDefaultMultipleChoice(wordId, intro, index)
                        }
                    }
                    else -> {
                        // MultipleChoice (with Verse & Translation)
                        existing ?: buildDefaultMultipleChoice(wordId, intro, index)
                    }
                }
            } else {
                existing ?: intro?.let { buildDefaultMultipleChoice(wordId, it, index) }
            }
        }
    }

    /** One batch from a single grammatical category, without repeats until the whole category has
     * been covered. Category membership comes from the content's own word categories (see
     * `ContentRepository.getWordCategories`), never from id prefixes or numeric id ranges. */
    private suspend fun sampleCategory(
        allWordIds: List<String>,
        category: LemmaCategory,
        batchSize: Int,
        covered: Set<String>,
        reset: suspend () -> Unit,
        markCovered: suspend (List<String>) -> Unit
    ): List<String> {
        val categories = contentRepository?.getWordCategories().orEmpty()
        val allInCategory = allWordIds.filter { categories[it] == category }
        val remaining = allInCategory.filter { it !in covered }
        val pool = if (remaining.size < batchSize) {
            reset()
            allInCategory
        } else {
            remaining
        }
        val sampled = pool.shuffled().take(batchSize)
        markCovered(sampled)
        return sampled
    }

    private fun computeWordSpans(verse: String): List<WordSpan> {
        val spans = mutableListOf<WordSpan>()
        var i = 0
        val n = verse.length
        while (i < n) {
            while (i < n && verse[i].isWhitespace()) i++
            if (i >= n) break
            val start = i
            while (i < n && !verse[i].isWhitespace()) i++
            spans.add(WordSpan(start, i))
        }
        return spans
    }

    /** Generated exercises bake only their correct option (from the word's own intro) -
     * `LessonViewModel.rebuildOptions` adds fresh distractors on load. Never an empty list, which
     * would leave the exercise with nothing to tap. */
    private fun correctOptionFor(wordId: String, intro: ExerciseContent.WordIntro): ChoiceOption =
        ChoiceOption(id = wordId, labelArabic = intro.arabicWord.cleanArabicDisplay(), label = intro.meaning)

    private fun buildDefaultMultipleChoice(
        wordId: String,
        intro: ExerciseContent.WordIntro,
        index: Int
    ): ExerciseEntity {
        val mcContent = ExerciseContent.MultipleChoice(
            prompt = GeneratedExercisePrompts.MULTIPLE_CHOICE_PROMPT,
            wordId = wordId,
            promptArabic = intro.arabicWord,
            options = listOf(correctOptionFor(wordId, intro)),
            correctOptionId = wordId,
            exampleVerseArabic = intro.exampleVerseArabic,
            exampleVerseReference = intro.exampleVerseReference,
            arabicWordStart = intro.arabicWordStart,
            arabicWordEnd = intro.arabicWordEnd,
            exampleVerseTranslation = intro.exampleVerseTranslation,
            meaningHighlight = intro.meaningHighlight
        )
        return ExerciseEntity(
            id = "test_mc_${wordId}_$index",
            lessonId = REVIEW_SESSION_LESSON_ID,
            orderIndex = index,
            type = ExerciseType.MULTIPLE_CHOICE,
            contentJson = AppJson.encodeToString(ExerciseContent.serializer(), mcContent),
            practicedItemId = wordId
        )
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
        return OpenPracticePool.oneExercisePerWord(scoredExercisesFor(sampledIds))
    }

    override suspend fun attemptStreakRecovery(userId: String, correctCount: Int, totalCount: Int): Boolean =
        withContext(Dispatchers.IO) {
            val stats = database.userStatsDao().get(userId) ?: return@withContext false
            // Only a genuinely locked streak, lapsed within the recovery window, can be restored -
            // otherwise a recovery quiz could revive a long-dead streak (or "extend" a live one).
            if (!StreakRecovery.canRecover(stats, LocalDate.now(clock))) return@withContext false
            val passed = GamificationConfig.percentOf(correctCount, totalCount) >= GamificationConfig.PASSING_SCORE_PERCENT
            if (passed) {
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
        curriculumMutex.withLock {
            database.withTransaction {
                // The hearts choice is a setting, not progress - it survives the reset.
                val heartsEnabled = database.userStatsDao().get(userId)?.heartsEnabled
                database.userProgressDao().deleteForUser(userId)
                database.userStatsDao().deleteForUser(userId)
                database.exerciseAttemptDao().deleteForUser(userId)
                database.dailyPracticeDao().deleteForUser(userId)
                database.achievementDao().deleteForUser(userId)
                database.wordMemoryDao().deleteForUser(userId)
                database.dailyQuestDao().deleteForUser(userId)
                ensureStatsRow(userId)
                if (heartsEnabled != null) {
                    database.userStatsDao().get(userId)?.let { database.userStatsDao().upsert(it.copy(heartsEnabled = heartsEnabled)) }
                }
                repairUnlockChain(userId)
            }
        }
    }

    private companion object {
        /** How often [observeDueCount] re-checks the clock - a lapse comes due ten minutes after
         * the miss, so a minute keeps Home's count honest without busy polling. */
        const val DUE_COUNT_REFRESH_MILLIS = 60_000L

        /** Hearts regenerate every 30 minutes; checking every 15 s keeps the badge current. */
        const val HEARTS_REFRESH_MILLIS = 15_000L
    }
}
