package com.quranicwords.app.feature.wordbrowse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.LocalizedWord
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.repository.WordExampleLocalizer
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.domain.repository.ProgressRepository
import com.quranicwords.app.core.domain.srs.WordStrength
import com.quranicwords.app.core.util.ArabicSearch
import com.quranicwords.app.core.util.decodeExerciseContentOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WordBrowseUiState(
    val isLoading: Boolean = true,
    val words: List<ExerciseContent.WordIntro> = emptyList(),
    /** Memory strength per word id; absent means the learner hasn't met the word yet. */
    val strengths: Map<String, WordStrength> = emptyMap(),
    /** wordId -> meaning and senses in the learner's current language (see WordExampleLocalizer). */
    val localized: Map<String, LocalizedWord> = emptyMap()
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
    private val progressRepository: ProgressRepository,
    private val userIdProvider: CurrentUserIdProvider,
    private val preferences: UserPreferencesDataStore,
    private val exampleLocalizer: WordExampleLocalizer,
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
                        decodeExerciseContentOrNull(exercise.contentJson) as? ExerciseContent.WordIntro
                    }
            }

            val strengths = progressRepository.getWordStrengths(userIdProvider.get())
            // Re-localized whenever the learner switches language.
            preferences.languageFlow.map { it ?: Language.ENGLISH }.distinctUntilChanged().collectLatest { language ->
                val localized = exampleLocalizer.localizeAll(words, language)
                _uiState.value = WordBrowseUiState(isLoading = false, words = words, strengths = strengths, localized = localized)
            }
        }
    }

    /** First card matching [query] - Arabic without diacritics, or any meaning - or null. */
    fun indexOfMatch(query: String): Int? {
        if (query.isBlank()) return null
        return _uiState.value.words.indexOfFirst { word ->
            ArabicSearch.matches(word.arabicWord, query) || word.meaning.values.any { ArabicSearch.matches(it, query) }
        }.takeIf { it >= 0 }
    }
}
