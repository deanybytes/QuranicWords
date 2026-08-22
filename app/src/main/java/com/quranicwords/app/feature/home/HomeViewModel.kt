package com.quranicwords.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.SectionEntity
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import com.quranicwords.app.core.domain.DailyGoalCalculator
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class SectionWithLessons(val section: SectionEntity, val lessons: List<LessonEntity>)
data class ChapterWithSections(
    val chapter: ChapterEntity,
    val sections: List<SectionWithLessons>,
    /** CHAPTER_EXAM (+ CHAPTER_FLASHBACK where one exists) - sectionId == null, so these can
     * never come back from a per-section lessons query. Rendered after this chapter's last
     * section. See ContentRepository.getChapterLevelLessons. */
    val chapterLevelLessons: List<LessonEntity> = emptyList()
)

data class HomeUiState(
    /** The whole curriculum tree, fetched once per Home session - static content that never
     * changes mid-session (seeding always completes before Home is first shown, see
     * SplashViewModel), so it's a one-shot snapshot rather than an observed Flow (unlike
     * per-lesson progress below, which genuinely changes live as the learner completes things). */
    val chapters: List<ChapterWithSections> = emptyList(),
    val progressByLessonId: Map<String, UserProgressEntity> = emptyMap(),
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val isLoading: Boolean = true,
    /** Whether the Review entry point should show - fetched once per Home session, not live
     * (the missed-items query is a one-shot suspend read, not a Flow), so it can go stale if a
     * Review/lesson session completes without this ViewModel being recreated. Acceptable given
     * the entry point itself re-checks emptiness before assembling a session either way. */
    val hasReviewableItems: Boolean = false,
    /** The chapter/section containing the learner's actual current lesson (first UNLOCKED-but-
     * not-COMPLETED one) - the collapse/expand tree auto-opens to here on load rather than
     * requiring a tap first, softening the collapse-by-default UX trade-off. Null/null when there
     * is no such lesson (fresh install before ensureCurriculumStarted's bootstrap has produced
     * any progress rows yet, or the whole curriculum is already complete). */
    val initiallyExpandedChapterId: String? = null,
    val initiallyExpandedSectionId: String? = null,
    /** Running-total Quran coverage percent through and including each chapter (chapter id ->
     * cumulative %), for the "own share · running total" stat line on each chapter's summary
     * card - see [cumulativeCoveragePercentByChapter]. Every chapter in this curriculum has the
     * same word count (460), which is why chapter cards show coverage %, not word count - a
     * constant repeated on every card carries no information. */
    val cumulativeCoveragePercentByChapter: Map<String, Double> = emptyMap(),
    /** Every COMPLETED lesson across the whole curriculum, most-recently-completed first - backs
     * the swipeable history card (see [completedLessonsHistory]). */
    val completedHistory: List<Pair<LessonEntity, UserProgressEntity>> = emptyList(),
    /** Whether today's [com.quranicwords.app.core.domain.model.DailyGoalLevel] target has been
     * met - drives the "daily challenge completed" indicator alongside the top status badges.
     * Live (see [ProgressRepository.observeTodayPractice]), not a one-shot snapshot, so it flips
     * true the moment a lesson finishing today crosses the goal without needing this ViewModel
     * to be recreated. */
    val isDailyGoalMetToday: Boolean = false
)

/** Pure derivation, no DB access - the first lesson (in tree order: chapter by chapter, section
 * by section, then this chapter's own trailing exam/flashback) whose progress status is
 * [LessonStatus.UNLOCKED] rather than [LessonStatus.COMPLETED] or missing entirely. A lesson with
 * no progress row at all is not "current" - only [com.quranicwords.app.core.data.repository
 * .ProgressRepositoryImpl.unlockIfNeeded] ever creates one, so an absent row means "not reached
 * yet", not "in progress". */
fun findCurrentPosition(chapters: List<ChapterWithSections>, progressByLessonId: Map<String, UserProgressEntity>): Pair<String?, String?> {
    for (chapterWithSections in chapters) {
        for (sectionWithLessons in chapterWithSections.sections) {
            val hasCurrent = sectionWithLessons.lessons.any { progressByLessonId[it.id]?.status == LessonStatus.UNLOCKED }
            if (hasCurrent) return chapterWithSections.chapter.id to sectionWithLessons.section.id
        }
        val hasCurrentChapterLevel = chapterWithSections.chapterLevelLessons
            .any { progressByLessonId[it.id]?.status == LessonStatus.UNLOCKED }
        if (hasCurrentChapterLevel) return chapterWithSections.chapter.id to null
    }
    return null to null
}

/** Pure derivation for a chapter/section summary node's own status, from its children's real
 * per-lesson progress rows - COMPLETED only when every child is; UNLOCKED if any child has been
 * reached at all; LOCKED (the default for an absent progress row - see [findCurrentPosition]'s
 * doc comment) otherwise. Used by [HomeScreen]'s collapse/expand tree (QW-22). */
fun aggregateStatus(lessonIds: List<String>, progressByLessonId: Map<String, UserProgressEntity>): LessonStatus {
    if (lessonIds.isEmpty()) return LessonStatus.LOCKED
    val statuses = lessonIds.map { progressByLessonId[it]?.status ?: LessonStatus.LOCKED }
    return when {
        statuses.all { it == LessonStatus.COMPLETED } -> LessonStatus.COMPLETED
        statuses.any { it != LessonStatus.LOCKED } -> LessonStatus.UNLOCKED
        else -> LessonStatus.LOCKED
    }
}

/** Fraction of [lessonIds] that are COMPLETED - drives the thin progress bar on a chapter/section
 * summary card. Distinct from [aggregateStatus] (a three-state category) since a card benefits
 * from showing granular "6 of 10 done" progress even while its overall status is still UNLOCKED. */
fun progressFraction(lessonIds: List<String>, progressByLessonId: Map<String, UserProgressEntity>): Float {
    if (lessonIds.isEmpty()) return 0f
    val completed = lessonIds.count { progressByLessonId[it]?.status == LessonStatus.COMPLETED }
    return completed.toFloat() / lessonIds.size
}

/** Pure derivation, no DB access - running-total Quran coverage percent through and including
 * each chapter, in curriculum order. Chapter 1 -> its own percent; chapter 2 -> chapter 1 + 2;
 * and so on, mirroring the exact cumulative sum [com.quranicwords.app.feature.intro.IntroViewModel]
 * already computes for the chapter-intro screen, just keyed by chapter id for every chapter at
 * once instead of one running total for a single chapter. */
fun cumulativeCoveragePercentByChapter(chapters: List<ChapterWithSections>): Map<String, Double> {
    var running = 0.0
    return chapters.associate { chapterWithSections ->
        running += chapterWithSections.chapter.quranOccurrencePercent
        chapterWithSections.chapter.id to running
    }
}

/** Pure derivation, no DB access - every COMPLETED lesson across the whole curriculum tree with a
 * real completion timestamp, most-recently-completed first. Backs the swipeable history card on
 * Home (a lightweight peek through the learner's own completed lessons, not free navigation -
 * that's [com.quranicwords.app.feature.roadmap.RoadmapScreen]'s job). A lesson with a COMPLETED
 * status row but a null [UserProgressEntity.completedAtEpochMillis] can't happen in practice (every
 * writer that sets COMPLETED also stamps the timestamp) but is defensively excluded rather than
 * assumed. */
fun completedLessonsHistory(
    chapters: List<ChapterWithSections>,
    progressByLessonId: Map<String, UserProgressEntity>
): List<Pair<LessonEntity, UserProgressEntity>> {
    val allLessons = chapters.flatMap { chapterWithSections ->
        chapterWithSections.sections.flatMap { it.lessons } + chapterWithSections.chapterLevelLessons
    }
    return allLessons.mapNotNull { lesson ->
        val progress = progressByLessonId[lesson.id] ?: return@mapNotNull null
        if (progress.status != LessonStatus.COMPLETED || progress.completedAtEpochMillis == null) return@mapNotNull null
        lesson to progress
    }.sortedByDescending { (_, progress) -> progress.completedAtEpochMillis }
}

/** Pure, no DB access - steps a history-browse index by [delta], clamped to `[0, size-1]` rather
 * than wrapping, so swiping past either end of the learner's completed-lesson history stops
 * instead of silently looping back around. */
fun stepHistoryIndex(currentIndex: Int, size: Int, delta: Int): Int {
    if (size <= 0) return 0
    return (currentIndex + delta).coerceIn(0, size - 1)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
    private val preferences: UserPreferencesDataStore,
    private val userIdProvider: CurrentUserIdProvider,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = userIdProvider.get()

            // Bootstraps chapter 1 / section 1 / lesson 1 if this user has no progress at all yet
            // - every later unlock chains from there via ProgressRepositoryImpl.completeLesson.
            progressRepository.ensureCurriculumStarted(userId)

            val hasReviewableItems = progressRepository.getMissedItemIds(userId).isNotEmpty()
            val chapters = loadCurriculumTree()
            val cumulativeCoverage = cumulativeCoveragePercentByChapter(chapters)
            val today = LocalDate.now(clock).toString()

            combine(
                progressRepository.observeProgress(userId),
                progressRepository.observeStats(userId),
                progressRepository.observeTodayPractice(userId, today),
                preferences.dailyGoalLevelFlow
            ) { progress, stats, todayPractice, goalLevel ->
                val progressByLessonId = progress.associateBy { it.lessonId }
                val (currentChapterId, currentSectionId) = findCurrentPosition(chapters, progressByLessonId)
                HomeUiState(
                    chapters = chapters,
                    progressByLessonId = progressByLessonId,
                    totalPoints = stats?.totalPoints ?: 0,
                    currentStreak = displayedStreak(stats),
                    isLoading = false,
                    hasReviewableItems = hasReviewableItems,
                    initiallyExpandedChapterId = currentChapterId,
                    initiallyExpandedSectionId = currentSectionId,
                    cumulativeCoveragePercentByChapter = cumulativeCoverage,
                    completedHistory = completedLessonsHistory(chapters, progressByLessonId),
                    isDailyGoalMetToday = DailyGoalCalculator.isGoalMetToday(
                        todayPractice?.minutesPracticed ?: 0,
                        goalLevel.minutes
                    )
                )
            }.collect { _uiState.value = it }
        }
    }

    private suspend fun loadCurriculumTree(): List<ChapterWithSections> {
        val chapters = contentRepository.observeChapters().first()
        return chapters.map { chapter ->
            val sections = contentRepository.observeSections(chapter.id).first()
            val sectionsWithLessons = sections.map { section ->
                SectionWithLessons(section, contentRepository.observeLessons(section.id).first())
            }
            ChapterWithSections(chapter, sectionsWithLessons, contentRepository.getChapterLevelLessons(chapter.id))
        }
    }

    /** [UserStatsEntity.currentStreak] is only ever recomputed by StreakCalculator when a
     * lesson completes, so a stored streak from days ago would otherwise still show as "alive"
     * here even though the user missed a day - it only silently drops the next time they finish
     * a lesson. Treat a gap of more than one day as already broken for display purposes. */
    private fun displayedStreak(stats: UserStatsEntity?): Int {
        if (stats == null) return 0
        val lastActivity = stats.lastActivityLocalDate
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return 0
        val dayGap = ChronoUnit.DAYS.between(lastActivity, LocalDate.now(clock))
        return if (dayGap <= 1) stats.currentStreak else 0
    }
}
