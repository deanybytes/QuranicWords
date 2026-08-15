package com.quranicwords.app.feature.wordbrowse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.util.AppJson
import com.quranicwords.app.core.util.AudioPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WordBrowseUiState(
    val isLoading: Boolean = true,
    val words: List<ExerciseContent.WordIntro> = emptyList()
)

/**
 * Card-flip story-fold browsing (see [com.quranicwords.app.feature.wordbrowse.WordBrowseScreen]) -
 * an alternate, on-demand way to page through a section's words outside the structured
 * teach-then-quiz lesson flow, which stays exactly as it is (this doesn't replace
 * `WordIntroExerciseContent`'s in-lesson teach step - see that composable's own doc comment for
 * why the teach-then-quiz-per-word interleaving is deliberate and not something this screen
 * should disturb).
 */
@HiltViewModel
class WordBrowseViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val audioPlayer: AudioPlayer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sectionId: String = checkNotNull(savedStateHandle["sectionId"])

    private val _uiState = MutableStateFlow(WordBrowseUiState())
    val uiState: StateFlow<WordBrowseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val lessons = contentRepository.observeLessons(sectionId).first()
                .filter { it.kind == LessonKind.REGULAR }
                .sortedBy { it.sortOrder }

            val words = lessons.flatMap { lesson ->
                contentRepository.getExercisesForLesson(lesson.id)
                    .sortedBy { it.orderIndex }
                    .mapNotNull { exercise ->
                        runCatching {
                            AppJson.decodeFromString(ExerciseContent.serializer(), exercise.contentJson)
                        }.getOrNull() as? ExerciseContent.WordIntro
                    }
            }

            _uiState.value = WordBrowseUiState(isLoading = false, words = words)
        }
    }

    /** Returns false (no throw) if the clip isn't bundled - mirrors `LessonViewModel.playAudio`. */
    fun playAudio(assetPath: String): Boolean = audioPlayer.play(assetPath)

    override fun onCleared() {
        audioPlayer.release()
    }
}
