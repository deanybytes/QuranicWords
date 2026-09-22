package com.quranicwords.app.feature.roadmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.domain.repository.ProgressRepository
import com.quranicwords.app.feature.home.ChapterWithSections
import com.quranicwords.app.feature.home.SectionWithLessons
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.quranicwords.app.feature.home.findCurrentLessonId

data class RoadmapUiState(
    val chapters: List<ChapterWithSections> = emptyList(),
    val progressByLessonId: Map<String, UserProgressEntity> = emptyMap(),
    val currentLessonId: String? = null,
    val isLoading: Boolean = true
)

/** A completed or unlocked unit can be jumped to for learning or review on the Roadmap.
 * Only locked units remain inert. Pure and unit-tested (`RoadmapViewModelTest`) since it gates every tap
 * target on this screen. */
fun isRoadmapReachable(status: LessonStatus): Boolean =
    status == LessonStatus.COMPLETED || status == LessonStatus.UNLOCKED

/**
 * Full-curriculum timeline for viewing learning progress and jumping straight to any *completed*
 * or *unlocked* chapter/section/lesson for learning or review.
 */
@HiltViewModel
class RoadmapViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
    private val userIdProvider: CurrentUserIdProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoadmapUiState())
    val uiState: StateFlow<RoadmapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = userIdProvider.get()
            progressRepository.ensureCurriculumStarted(userId)
            val chapters = contentRepository.getFullCurriculumTree()
            progressRepository.observeProgress(userId).collect { progress ->
                val progressByLessonId = progress.associateBy { row -> row.lessonId }
                val currentLessonId = findCurrentLessonId(chapters, progressByLessonId)
                _uiState.update {
                    it.copy(
                        chapters = chapters,
                        progressByLessonId = progressByLessonId,
                        currentLessonId = currentLessonId,
                        isLoading = false
                    )
                }
            }
        }
    }
}

