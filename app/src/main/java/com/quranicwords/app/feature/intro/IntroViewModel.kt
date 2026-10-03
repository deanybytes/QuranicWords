package com.quranicwords.app.feature.intro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.domain.CoverageCalculator
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.domain.model.LocalizedText
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IntroUiState(
    val isLoading: Boolean = true,
    val isChapter: Boolean = false,
    val title: LocalizedText = emptyMap(),
    val wordCount: Int = 0,
    val occurrencePercent: Double = 0.0,
    val cumulativePercent: Double = 0.0,
    /** The requested chapter/section isn't in the content (stale or bad id) - show a way back. */
    val isUnavailable: Boolean = false
)

/**
 * Backs both [com.quranicwords.app.core.navigation.Route.ChapterIntro] and
 * [com.quranicwords.app.core.navigation.Route.SectionIntro] - exactly one of [chapterId]/
 * [sectionId] is non-null depending on which route reached this ViewModel, same nullable-arg
 * pattern as `LessonViewModel.lessonId`/Review. [IntroUiState.wordCount]/[IntroUiState
 * .occurrencePercent] are precomputed at content-ingestion time
 * ([com.quranicwords.app.core.data.local.entity.ChapterEntity]/[com.quranicwords.app.core.data
 * .local.entity.SectionEntity]'s `wordCount`/`quranOccurrencePercent`) and used as-is.
 * [IntroUiState.cumulativePercent], though, is **not** a position-only sum of earlier
 * siblings' percentages - it is the learner's real coverage so far ([CoverageCalculator], the
 * same figure Home/Progress show) plus whatever of the unit being introduced is still uncovered,
 * since the copy ("you'll have covered X% ... so far") is a post-completion projection.
 */
@HiltViewModel
class IntroViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
    private val userIdProvider: CurrentUserIdProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chapterId: String? = savedStateHandle["chapterId"]
    private val sectionId: String? = savedStateHandle["sectionId"]

    private val _uiState = MutableStateFlow(IntroUiState())
    val uiState: StateFlow<IntroUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = userIdProvider.get()
            val tree = contentRepository.getFullCurriculumTree()
            val completedLessonIds = progressRepository.observeProgress(userId).first()
                .filter { it.status == LessonStatus.COMPLETED }
                .map { it.lessonId }
                .toSet()
            // Same numbers Home/Progress show - see CoverageCalculator.
            val coverageByChapter = CoverageCalculator.coverageByChapter(tree, completedLessonIds)
            val coveredSoFar = coverageByChapter.values.sum()

            // A stale/deep-linked id that no longer exists in the (re)seeded content must not
            // crash - fall back to a "not available" state with a way back.
            val state = if (chapterId != null) {
                tree.firstOrNull { it.chapter.id == chapterId }?.let { node ->
                    val chapter = node.chapter
                    val stillUncovered = (chapter.quranOccurrencePercent - (coverageByChapter[chapter.id] ?: 0.0)).coerceAtLeast(0.0)
                    IntroUiState(
                        isLoading = false,
                        isChapter = true,
                        title = chapter.title,
                        wordCount = chapter.wordCount,
                        occurrencePercent = chapter.quranOccurrencePercent,
                        cumulativePercent = coveredSoFar + stillUncovered
                    )
                }
            } else if (sectionId != null) {
                tree.asSequence().flatMap { it.sections.asSequence() }.firstOrNull { it.section.id == sectionId }?.let { node ->
                    val section = node.section
                    val covered = CoverageCalculator.sectionCoverage(section, node.lessons, completedLessonIds)
                    val stillUncovered = (section.quranOccurrencePercent - covered).coerceAtLeast(0.0)
                    IntroUiState(
                        isLoading = false,
                        isChapter = false,
                        title = section.title,
                        wordCount = section.wordCount,
                        occurrencePercent = section.quranOccurrencePercent,
                        cumulativePercent = coveredSoFar + stillUncovered
                    )
                }
            } else {
                null
            }
            _uiState.value = state ?: IntroUiState(isLoading = false, isUnavailable = true)
        }
    }
}
