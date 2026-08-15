package com.quranicwords.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.ModuleEntity
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class HomeUiState(
    val modules: List<ModuleEntity> = emptyList(),
    val lessonsByModuleId: Map<String, List<LessonEntity>> = emptyMap(),
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

@OptIn(ExperimentalCoroutinesApi::class)
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

            // Unlock the first lesson of every implemented module so a newly-implemented module
            // has somewhere to start - single-tier app, no self-placement gating anymore.
            contentRepository.observeModules().first()
                .filter { it.isImplemented }
                .forEach { progressRepository.ensureModuleStarted(userId, it.id) }

            val hasReviewableItems = progressRepository.getMissedItemIds(userId).isNotEmpty()

            val modulesAndLessons = contentRepository.observeModules().flatMapLatest { modules ->
                val accessible = modules.filter { it.isImplemented }
                val lessonFlows: List<Flow<Pair<String, List<LessonEntity>>>> = accessible.map { module ->
                    contentRepository.observeLessons(module.id).map { module.id to it }
                }
                val lessonsByModuleId = if (lessonFlows.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    combine(lessonFlows) { pairs -> pairs.toMap() }
                }
                lessonsByModuleId.map { modules to it }
            }

            combine(
                modulesAndLessons,
                progressRepository.observeProgress(userId),
                progressRepository.observeStats(userId)
            ) { (modules, lessonsByModuleId), progress, stats ->
                HomeUiState(
                    modules = modules,
                    lessonsByModuleId = lessonsByModuleId,
                    progressByLessonId = progress.associateBy { it.lessonId },
                    totalPoints = stats?.totalPoints ?: 0,
                    currentStreak = displayedStreak(stats),
                    isLoading = false,
                    hasReviewableItems = hasReviewableItems
                )
            }.collect { _uiState.value = it }
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
