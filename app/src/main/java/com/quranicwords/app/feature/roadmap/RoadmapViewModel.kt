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

data class RoadmapUiState(
    val chapters: List<ChapterWithSections> = emptyList(),
    val progressByLessonId: Map<String, UserProgressEntity> = emptyMap(),
    val isLoading: Boolean = true
)

/** A completed unit can be jumped to for review; anything not yet completed (UNLOCKED or LOCKED)
 * is inert on the Roadmap - full free navigation to an in-progress/future unit is explicitly not
 * this screen's job (that's the ordinary Home path), only reachable via actually completing the
 * curriculum in order. Pure and unit-tested (`RoadmapViewModelTest`) since it gates every tap
 * target on this screen. */
fun isRoadmapReachable(status: LessonStatus): Boolean = status == LessonStatus.COMPLETED

/**
 * Full-curriculum timeline for jumping straight to any *completed* chapter/section/lesson for
 * review - a separate destination from Home's collapse/expand tree, which is about progressing
 * forward, not browsing backward. Reuses [ChapterWithSections]/[SectionWithLessons] and the same
 * repository composition [com.quranicwords.app.feature.home.HomeViewModel] already uses, rather
 * than a parallel data shape.
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
            val chapters = loadCurriculumTree()
            progressRepository.observeProgress(userId).collect { progress ->
                _uiState.update {
                    it.copy(
                        chapters = chapters,
                        progressByLessonId = progress.associateBy { row -> row.lessonId },
                        isLoading = false
                    )
                }
            }
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
}
