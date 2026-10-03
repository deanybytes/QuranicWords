package com.quranicwords.app.core.data.repository

import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.data.local.entity.AchievementEntity
import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.domain.AchievementCatalog
import com.quranicwords.app.core.domain.CoverageCalculator
import com.quranicwords.app.core.domain.AchievementDef
import com.quranicwords.app.core.domain.AchievementMetrics
import com.quranicwords.app.core.domain.AchievementProgress
import com.quranicwords.app.core.domain.LevelCurve
import com.quranicwords.app.core.domain.srs.WordStrength
import com.quranicwords.app.core.domain.repository.AchievementRepository
import com.quranicwords.app.core.util.GamificationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepositoryImpl @Inject constructor(
    private val database: QwDatabase
) : AchievementRepository {

    override fun observeUnlocked(userId: String): Flow<List<AchievementEntity>> =
        database.achievementDao().observeForUser(userId)

    override suspend fun getCumulativeCoveragePercent(userId: String): Double = withContext(Dispatchers.IO) {
        val progress = database.userProgressDao().getAllForUserOnce(userId)
        val completedLessonIds = progress.filter { it.status == LessonStatus.COMPLETED }.map { it.lessonId }.toSet()
        val lessons = database.lessonDao().getAll()
        val sections = database.sectionDao().getAll()
        val chapters = database.chapterDao().getAll()
        coverageForCompletedLessons(lessons, sections, chapters, completedLessonIds)
    }

    /** Shared by [checkAndUnlock] (coverage-band achievements) and [getCumulativeCoveragePercent]
     * (the Progress tab's coverage donut) - see [CoverageCalculator]. */
    private fun coverageForCompletedLessons(
        lessons: List<LessonEntity>,
        sections: List<com.quranicwords.app.core.data.local.entity.SectionEntity>,
        chapters: List<ChapterEntity>,
        completedLessonIds: Set<String>
    ): Double = CoverageCalculator.totalCoverage(chapters, sections, lessons, completedLessonIds)

    override suspend fun checkAndUnlock(userId: String, completedReviewSession: Boolean): List<AchievementDef> = withContext(Dispatchers.IO) {
        val alreadyUnlocked = database.achievementDao().getAllForUserOnce(userId).map { it.achievementId }.toSet()
        val candidates = AchievementCatalog.all.filterNot { it.id in alreadyUnlocked }
        if (candidates.isEmpty()) return@withContext emptyList()

        val metrics = metricsFor(userId, completedReviewSession)
        val newlyUnlocked = candidates.filter { AchievementCatalog.progressOf(it, metrics).isComplete }

        if (newlyUnlocked.isNotEmpty()) {
            val now = System.currentTimeMillis()
            database.achievementDao().insertAll(
                newlyUnlocked.map { AchievementEntity(userId = userId, achievementId = it.id, unlockedAtEpochMillis = now) }
            )
        }
        newlyUnlocked
    }

    override suspend fun getProgress(userId: String): Map<String, AchievementProgress> = withContext(Dispatchers.IO) {
        val metrics = metricsFor(userId, completedReviewSession = false)
        AchievementCatalog.all.associate { it.id to AchievementCatalog.progressOf(it, metrics) }
    }

    /** One read pass over stats, progress, curriculum and memory for [AchievementCatalog.progressOf]. */
    private suspend fun metricsFor(userId: String, completedReviewSession: Boolean): AchievementMetrics {
        val stats = database.userStatsDao().get(userId)
        val progress = database.userProgressDao().getAllForUserOnce(userId)
        val completedLessonIds = progress.filter { it.status == LessonStatus.COMPLETED }.map { it.lessonId }.toSet()
        val lessons = database.lessonDao().getAll()
        val lessonById = lessons.associateBy { it.id }
        val sections = database.sectionDao().getAll()
        val chapters = database.chapterDao().getAll()

        val completedChapterExamChapterIds = lessons
            .filter { it.kind == LessonKind.CHAPTER_EXAM && it.id in completedLessonIds }
            .map { it.chapterId }
            .toSet()
        val chapterPositions = chapters.sortedBy { it.sortOrder }
            .mapIndexedNotNull { index, chapter -> (index + 1).takeIf { chapter.id in completedChapterExamChapterIds } }
            .toSet()
        val hasPassedAnExam = progress.any { row ->
            val kind = lessonById[row.lessonId]?.kind
            (kind == LessonKind.SECTION_EXAM || kind == LessonKind.CHAPTER_EXAM) &&
                row.bestScorePercent >= GamificationConfig.PASSING_SCORE_PERCENT
        }
        val alreadyReviewed = database.achievementDao().getAllForUserOnce(userId)
            .any { it.achievementId == AchievementCatalog.FIRST_REVIEW_SESSION }
        val memory = database.wordMemoryDao().getAllForUser(userId)

        return AchievementMetrics(
            longestStreak = stats?.longestStreak ?: 0,
            completedChapterPositions = chapterPositions,
            coveragePercent = coverageForCompletedLessons(lessons, sections, chapters, completedLessonIds),
            hasCompletedRegularLesson = completedLessonIds.any { id -> lessonById[id]?.kind == LessonKind.REGULAR },
            hasPassedExam = hasPassedAnExam,
            hasCompletedReviewSession = completedReviewSession || alreadyReviewed,
            reviewCount = memory.sumOf { (it.reps - 1).coerceAtLeast(0) },
            bestCombo = stats?.bestCombo ?: 0,
            level = LevelCurve.levelFor(stats?.totalPoints ?: 0),
            strongWordCount = memory.count { WordStrength.fromStability(it.stability).isStrongOrBetter }
        )
    }
}
