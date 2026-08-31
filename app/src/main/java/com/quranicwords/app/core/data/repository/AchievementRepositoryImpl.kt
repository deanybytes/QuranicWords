package com.quranicwords.app.core.data.repository

import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.data.local.entity.AchievementEntity
import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.domain.AchievementCatalog
import com.quranicwords.app.core.domain.AchievementDef
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

    /** Calculates real proportional Quran coverage percent over completed lessons/sections,
     * with fallback to chapter exams if section occurrence percentages are unpopulated.
     * Shared by [checkAndUnlock] (coverage-band achievements) and [getCumulativeCoveragePercent]
     * (the Progress tab's coverage donut). */
    private fun coverageForCompletedLessons(
        lessons: List<LessonEntity>,
        sections: List<com.quranicwords.app.core.data.local.entity.SectionEntity>,
        chapters: List<ChapterEntity>,
        completedLessonIds: Set<String>
    ): Double {
        val hasSectionPercents = sections.any { it.quranOccurrencePercent > 0.0 }
        if (hasSectionPercents) {
            val lessonsBySection = lessons.filter { it.sectionId != null }.groupBy { it.sectionId!! }
            var total = 0.0
            sections.forEach { section ->
                val sectionLessons = lessonsBySection[section.id] ?: emptyList()
                if (sectionLessons.isNotEmpty()) {
                    val completed = sectionLessons.count { it.id in completedLessonIds }
                    total += (completed.toDouble() / sectionLessons.size) * section.quranOccurrencePercent
                }
            }
            return total
        }
        val completedChapterExamChapterIds = lessons
            .filter { it.kind == LessonKind.CHAPTER_EXAM && it.id in completedLessonIds }
            .map { it.chapterId }
            .toSet()
        return chapters
            .filter { it.id in completedChapterExamChapterIds }
            .sumOf { it.quranOccurrencePercent }
    }

    override suspend fun checkAndUnlock(userId: String): List<AchievementDef> = withContext(Dispatchers.IO) {
        val alreadyUnlocked = database.achievementDao().getAllForUserOnce(userId).map { it.achievementId }.toSet()
        val candidates = AchievementCatalog.all.filterNot { it.id in alreadyUnlocked }
        if (candidates.isEmpty()) return@withContext emptyList()

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
        val cumulativeCoveragePercent = coverageForCompletedLessons(lessons, sections, chapters, completedLessonIds)

        val hasCompletedRegularLesson = completedLessonIds.any { id -> lessonById[id]?.kind == LessonKind.REGULAR }
        val hasPassedAnExam = progress.any { row ->
            val kind = lessonById[row.lessonId]?.kind
            (kind == LessonKind.SECTION_EXAM || kind == LessonKind.CHAPTER_EXAM) &&
                row.bestScorePercent >= GamificationConfig.PASSING_SCORE_PERCENT
        }

        val newlyUnlocked = candidates.filter { def ->
            when {
                def.id.startsWith("streak_") -> {
                    val threshold = def.id.removePrefix("streak_").toIntOrNull() ?: return@filter false
                    (stats?.longestStreak ?: 0) >= threshold
                }
                AchievementCatalog.chapterIdFor(def.id) != null -> {
                    AchievementCatalog.chapterIdFor(def.id) in completedChapterExamChapterIds
                }
                def.id.startsWith("coverage_") -> {
                    val band = def.id.removePrefix("coverage_").toIntOrNull() ?: return@filter false
                    cumulativeCoveragePercent >= band
                }
                def.id == "first_lesson" -> hasCompletedRegularLesson
                def.id == "first_exam_passed" -> hasPassedAnExam
                else -> false
            }
        }

        if (newlyUnlocked.isNotEmpty()) {
            val now = System.currentTimeMillis()
            database.achievementDao().insertAll(
                newlyUnlocked.map { AchievementEntity(userId = userId, achievementId = it.id, unlockedAtEpochMillis = now) }
            )
        }
        newlyUnlocked
    }
}
