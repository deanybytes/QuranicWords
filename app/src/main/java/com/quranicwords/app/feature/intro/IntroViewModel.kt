package com.quranicwords.app.feature.intro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.data.local.entity.LessonKind
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
    val cumulativePercent: Double = 0.0
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
 * siblings' percentages - it reflects [userId]'s actual completed chapter/section exams (via
 * [progressRepository]), so a learner who has skipped or not yet finished an earlier unit doesn't
 * see credit for it. The unit currently being introduced is always added on top, since the copy
 * ("you'll have covered X% ... so far") is a post-completion projection.
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
            val allChapters = contentRepository.observeChapters().first().sortedBy { it.sortOrder }
            val completedLessonIds = progressRepository.observeProgress(userId).first()
                .filter { it.status == LessonStatus.COMPLETED }
                .map { it.lessonId }
                .toSet()

            if (chapterId != null) {
                val chapter = allChapters.first { it.id == chapterId }
                val completedPriorChaptersPercent = allChapters
                    .filter { it.sortOrder < chapter.sortOrder }
                    .sumOf { c -> if (isChapterExamCompleted(c.id, completedLessonIds)) c.quranOccurrencePercent else 0.0 }
                _uiState.value = IntroUiState(
                    isLoading = false,
                    isChapter = true,
                    title = chapter.title,
                    wordCount = chapter.wordCount,
                    occurrencePercent = chapter.quranOccurrencePercent,
                    cumulativePercent = completedPriorChaptersPercent + chapter.quranOccurrencePercent
                )
            } else if (sectionId != null) {
                val sections = allChapters.flatMap { c -> contentRepository.observeSections(c.id).first() }
                val section = sections.first { it.id == sectionId }
                val thisChapter = allChapters.first { it.id == section.chapterId }

                val completedPriorChaptersPercent = allChapters
                    .filter { it.sortOrder < thisChapter.sortOrder }
                    .sumOf { c -> if (isChapterExamCompleted(c.id, completedLessonIds)) c.quranOccurrencePercent else 0.0 }

                val sectionsInChapter = contentRepository.observeSections(thisChapter.id).first()
                val completedPriorSectionsPercent = sectionsInChapter
                    .filter { it.sortOrder < section.sortOrder }
                    .sumOf { s -> if (isSectionExamCompleted(s.id, completedLessonIds)) s.quranOccurrencePercent else 0.0 }

                _uiState.value = IntroUiState(
                    isLoading = false,
                    isChapter = false,
                    title = section.title,
                    wordCount = section.wordCount,
                    occurrencePercent = section.quranOccurrencePercent,
                    cumulativePercent = completedPriorChaptersPercent + completedPriorSectionsPercent + section.quranOccurrencePercent
                )
            }
        }
    }

    private suspend fun isChapterExamCompleted(chapterId: String, completedLessonIds: Set<String>): Boolean =
        contentRepository.getChapterLevelLessons(chapterId)
            .any { it.kind == LessonKind.CHAPTER_EXAM && it.id in completedLessonIds }

    private suspend fun isSectionExamCompleted(sectionId: String, completedLessonIds: Set<String>): Boolean =
        contentRepository.observeLessons(sectionId).first()
            .any { it.kind == LessonKind.SECTION_EXAM && it.id in completedLessonIds }
}
