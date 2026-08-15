package com.quranicwords.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
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
data class ChapterWithSections(val chapter: ChapterEntity, val sections: List<SectionWithLessons>)

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
    val hasReviewableItems: Boolean = false
)

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
                HomeUiState(
                    chapters = chapters,
                    progressByLessonId = progress.associateBy { it.lessonId },
                    totalPoints = stats?.totalPoints ?: 0,
                    currentStreak = displayedStreak(stats),
                    isLoading = false,
                    hasReviewableItems = hasReviewableItems
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
            ChapterWithSections(chapter, sectionsWithLessons)
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
