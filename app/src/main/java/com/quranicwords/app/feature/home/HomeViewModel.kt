package com.quranicwords.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.SectionEntity
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.data.local.entity.UserStatsEntity
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
    val initiallyExpandedSectionId: String? = null
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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
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

            combine(
                progressRepository.observeProgress(userId),
                progressRepository.observeStats(userId)
            ) { progress, stats ->
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
                    initiallyExpandedSectionId = currentSectionId
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
