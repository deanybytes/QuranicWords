package com.quranicwords.app.feature.intro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.domain.model.LocalizedText
import com.quranicwords.app.core.domain.repository.ContentRepository
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
 * pattern as `LessonViewModel.lessonId`/Review. All stats are precomputed at content-ingestion
 * time ([com.quranicwords.app.core.data.local.entity.ChapterEntity]/[com.quranicwords.app.core
 * .data.local.entity.SectionEntity]'s `wordCount`/`quranOccurrencePercent`) - this ViewModel only
 * sums the already-stored per-unit percentages of earlier siblings, never recomputes them from
 * word-level data.
 */
@HiltViewModel
class IntroViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chapterId: String? = savedStateHandle["chapterId"]
    private val sectionId: String? = savedStateHandle["sectionId"]

    private val _uiState = MutableStateFlow(IntroUiState())
    val uiState: StateFlow<IntroUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val allChapters = contentRepository.observeChapters().first().sortedBy { it.sortOrder }

            if (chapterId != null) {
                val chapter = allChapters.first { it.id == chapterId }
                val cumulative = allChapters.filter { it.sortOrder <= chapter.sortOrder }
                    .sumOf { it.quranOccurrencePercent }
                _uiState.value = IntroUiState(
                    isLoading = false,
                    isChapter = true,
                    title = chapter.title,
                    wordCount = chapter.wordCount,
                    occurrencePercent = chapter.quranOccurrencePercent,
                    cumulativePercent = cumulative
                )
            } else if (sectionId != null) {
                val sections = allChapters.flatMap { c -> contentRepository.observeSections(c.id).first() }
                val section = sections.first { it.id == sectionId }
                val thisChapter = allChapters.first { it.id == section.chapterId }
                val priorChaptersTotal = allChapters.filter { it.sortOrder < thisChapter.sortOrder }
                    .sumOf { it.quranOccurrencePercent }
                val sectionsInChapter = contentRepository.observeSections(thisChapter.id).first()
                val sectionsSoFarTotal = sectionsInChapter.filter { it.sortOrder <= section.sortOrder }
                    .sumOf { it.quranOccurrencePercent }
                _uiState.value = IntroUiState(
                    isLoading = false,
                    isChapter = false,
                    title = section.title,
                    wordCount = section.wordCount,
                    occurrencePercent = section.quranOccurrencePercent,
                    cumulativePercent = priorChaptersTotal + sectionsSoFarTotal
                )
            }
        }
    }
}
