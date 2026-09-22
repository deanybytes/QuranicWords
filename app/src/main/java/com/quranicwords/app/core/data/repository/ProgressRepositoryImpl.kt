package com.quranicwords.app.core.data.repository

import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.data.local.entity.DailyPracticeEntity
import com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity
import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
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
    private val contentRepository: ContentRepository? = null
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
        val allLessons = database.lessonDao().getAll()
        val allSections = database.sectionDao().getAll()
        val allChapters = database.chapterDao().getAll()

        val firstChapter = allChapters.minByOrNull { it.sortOrder } ?: return@withContext
        val firstSection = allSections.filter { it.chapterId == firstChapter.id }.minByOrNull { it.sortOrder }
            ?: return@withContext
        val sectionLessons = allLessons.filter { it.sectionId == firstSection.id }.sortedBy { it.sortOrder }
        val firstLesson = sectionLessons.firstOrNull() ?: return@withContext
        unlockIfNeeded(userId, firstLesson.id)

        // If the first lesson is CHAPTER_INTRO, ensure the first playable content lesson is also unlocked
        if (firstLesson.kind == com.quranicwords.app.core.data.local.entity.LessonKind.CHAPTER_INTRO && sectionLessons.size > 1) {
            unlockIfNeeded(userId, sectionLessons[1].id)
        }

        // Auto-heal / repair unlock chain:
        // Any completed lesson that was passed should have its next lesson unlocked.
        val completedProgress = database.userProgressDao().getAllForUserOnce(userId)
            .filter { it.status == LessonStatus.COMPLETED }
        val lessonsById = allLessons.associateBy { it.id }
        for (prog in completedProgress) {
            val les = lessonsById[prog.lessonId] ?: continue
            // If any non-intro lesson in a chapter is completed, ensure the chapter's CHAPTER_INTRO is also completed
            if (les.kind != LessonKind.CHAPTER_INTRO) {
                val chapterIntros = allLessons.filter { it.chapterId == les.chapterId && it.kind == LessonKind.CHAPTER_INTRO }
                for (intro in chapterIntros) {
                    val introProg = database.userProgressDao().get(userId, intro.id)
                    if (introProg == null || introProg.status != LessonStatus.COMPLETED) {
                        database.userProgressDao().upsert(
                            UserProgressEntity(
                                userId = userId,
                                lessonId = intro.id,
                                status = LessonStatus.COMPLETED,
                                bestScorePercent = 100,
                                completedAtEpochMillis = prog.completedAtEpochMillis ?: System.currentTimeMillis(),
                                durationMillis = 0L
                            )
                        )
                    }
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
                        val secLessons = allLessons.filter { it.sectionId == nextLesson.sectionId }.sortedBy { it.sortOrder }
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
        // If a non-intro lesson in a chapter is completed, auto-complete preceding CHAPTER_INTRO in that chapter
        if (lesson != null && lesson.kind != LessonKind.CHAPTER_INTRO) {
            val chapterLessons = database.lessonDao().getForChapter(lesson.chapterId)
            val intros = chapterLessons.filter { it.kind == LessonKind.CHAPTER_INTRO }
            for (intro in intros) {
                val existingIntro = database.userProgressDao().get(userId, intro.id)
                if (existingIntro == null || existingIntro.status != LessonStatus.COMPLETED) {
                    database.userProgressDao().upsert(
                        UserProgressEntity(
                            userId = userId,
                            lessonId = intro.id,
                            status = LessonStatus.COMPLETED,
                            bestScorePercent = 100,
                            completedAtEpochMillis = System.currentTimeMillis(),
                            durationMillis = 0L
                        )
                    )
                }
            }
        }

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
                val ismWords = allWords.filter {
                    it.id.startsWith("wn_") || run {
                        val num = it.id.removePrefix("w_").toIntOrNull()
                        num != null && num in 1653..4709
                    }
                }
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
                val filWords = allWords.filter {
                    it.id.startsWith("wv_") || run {
                        val num = it.id.removePrefix("w_").toIntOrNull()
                        num != null && num in 174..1652
                    }
                }
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
                val harfWords = allWords.filter {
                    it.id.startsWith("wp_") || run {
                        val num = it.id.removePrefix("w_").toIntOrNull()
                        num != null && num in 1..173
                    }
                }
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

        val dbExercises = database.exerciseDao().getScoredExercisesForItems(itemIds)
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
                                prompt = TAP_WORD_PROMPT,
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
                                lessonId = "review_session",
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
                                prompt = FILL_BLANK_PROMPT,
                                wordId = wordId,
                                sentenceArabic = intro.exampleVerseArabic,
                                blankStart = targetSpan.start,
                                blankEnd = targetSpan.end,
                                sentenceTranslation = intro.exampleVerseTranslation,
                                sentenceReference = intro.exampleVerseReference.orEmpty(),
                                options = emptyList(),
                                correctOptionId = wordId
                            )
                            ExerciseEntity(
                                id = "test_fill_${wordId}_$index",
                                lessonId = "review_session",
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

    private fun buildDefaultMultipleChoice(
        wordId: String,
        intro: ExerciseContent.WordIntro,
        index: Int
    ): ExerciseEntity {
        val mcContent = ExerciseContent.MultipleChoice(
            prompt = MULTIPLE_CHOICE_PROMPT,
            wordId = wordId,
            promptArabic = intro.arabicWord,
            options = emptyList(),
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
            lessonId = "review_session",
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

    companion object {
        private val TAP_WORD_PROMPT = mapOf(
            "en" to "Tap the Arabic word in the verse",
            "bn" to "আয়াত থেকে সঠিক আরবি শব্দটি স্পর্শ করুন",
            "ur" to "آیت میں سے درست عربی لفظ منتخب کریں",
            "hi" to "आयत में से सही अरबी शब्द चुनें",
            "in" to "Ketuk kata Arab yang benar dalam ayat",
            "ms" to "Ketik perkataan Arab yang betul dalam ayat",
            "tr" to "Ayetteki doğru Arapça kelimeye dokunun",
            "fa" to "کلمه عربی درست را در آیه لمس کنید",
            "ha" to "Taba kalmar Larabci daidai a cikin ayar",
            "sw" to "Gusa neno sahihi la Kiarabu katika aya",
            "fr" to "Touchez le mot arabe correct dans le verset"
        )

        private val FILL_BLANK_PROMPT = mapOf(
            "en" to "Complete the verse",
            "bn" to "আয়াতটি সম্পূর্ণ করুন",
            "ur" to "آیت مکمل کریں",
            "hi" to "आयत पूरी करें",
            "in" to "Lengkapi ayat berikut",
            "ms" to "Lengkapkan ayat ini",
            "tr" to "Ayeti tamamlayın",
            "fa" to "آیه را کامل کنید",
            "ha" to "Kammala ayar",
            "sw" to "Kamilisha aya",
            "fr" to "Complétez le verset"
        )

        private val MULTIPLE_CHOICE_PROMPT = mapOf(
            "en" to "Choose the correct meaning",
            "bn" to "সঠিক অর্থ নির্বাচন করুন",
            "ur" to "درست معنی کا انتخاب کریں",
            "hi" to "सही अर्थ चुनें",
            "in" to "Pilih arti yang benar",
            "ms" to "Pilih maksud yang betul",
            "tr" to "Doğru anlamı seçin",
            "fa" to "معنی درست را انتخاب کنید",
            "ha" to "Zabi ma'anar da ta dace",
            "sw" to "Chagua maana sahihi",
            "fr" to "Choisissez la bonne signification"
        )
    }
}
