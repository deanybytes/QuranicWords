package com.quranicwords.app.feature.lessonsummary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.LemmaCategory
import com.quranicwords.app.core.domain.model.LocalizedText
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.domain.repository.ProgressRepository
import com.quranicwords.app.core.util.AppJson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WordSummaryItem(
    val wordId: String,
    val arabicWord: String,
    val meaning: LocalizedText,
    val category: LemmaCategory
)

data class LessonSummaryUiState(
    val isLoading: Boolean = true,
    // Current Lesson Details
    val lessonTitle: LocalizedText? = null,
    val lessonCategory: LemmaCategory? = null,
    val lessonKind: LessonKind? = null,
    val wordsCoveredCount: Int = 0,
    val wordsCoveredList: List<WordSummaryItem> = emptyList(),
    // Cumulative Qur'an stats
    val totalWordsLearned: Int = 0,
    val totalQuranOccurrencesLearned: Int = 0,
    val quranCoveragePercent: Double = 0.0,
    // Next Lesson Details
    val nextLessonId: String? = null,
    val nextLessonTitle: LocalizedText? = null,
    val nextLessonCategory: LemmaCategory? = null,
    val nextLessonKind: LessonKind? = null,
    val nextLessonWordCount: Int = 0,
    val nextLessonSectionTitle: LocalizedText? = null
)

@HiltViewModel
class LessonSummaryViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
    private val userIdProvider: CurrentUserIdProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val lessonId: String? = savedStateHandle["lessonId"]
    private val nextLessonId: String? = savedStateHandle["nextLessonId"]

    private val _uiState = MutableStateFlow(LessonSummaryUiState())
    val uiState: StateFlow<LessonSummaryUiState> = _uiState.asStateFlow()

    init {
        loadSummaryDetails()
    }

    private fun loadSummaryDetails() {
        viewModelScope.launch {
            val userId = userIdProvider.get()

            // 1. Current lesson words & info
            var lessonTitle: LocalizedText? = null
            var lessonCategory: LemmaCategory? = null
            var lessonKind: LessonKind? = null
            val wordsList = mutableListOf<WordSummaryItem>()

            if (!lessonId.isNullOrBlank() && lessonId != "review_session") {
                val currentLesson = contentRepository.getLesson(lessonId)
                if (currentLesson != null) {
                    lessonTitle = currentLesson.title
                    lessonCategory = currentLesson.category
                    lessonKind = currentLesson.kind
                }

                val exercises = contentRepository.getExercisesForLesson(lessonId)
                val wordCandidates = contentRepository.getWordCandidates().associateBy { it.id }

                exercises.forEach { ex ->
                    runCatching {
                        val content = AppJson.decodeFromString(ExerciseContent.serializer(), ex.contentJson)
                        if (content is ExerciseContent.WordIntro) {
                            val candidate = wordCandidates[content.wordId]
                            val meaning = candidate?.meaning ?: content.meaning
                            wordsList.add(
                                WordSummaryItem(
                                    wordId = content.wordId,
                                    arabicWord = content.arabicWord,
                                    meaning = meaning,
                                    category = content.lemmaCategory
                                )
                            )
                        }
                    }
                }
            }

            // 2. Cumulative progress stats
            val masteredWordIds = progressRepository.getMasteredItemIds(userId).toSet()
            val allWords = contentRepository.getWordCandidates()
            val totalWordsLearned = masteredWordIds.size
            val masteredOccurrences = allWords.filter { it.id in masteredWordIds }.sumOf { it.frequencyCount }
            val totalOccurrences = allWords.sumOf { it.frequencyCount }.coerceAtLeast(1)
            val coveragePercent = (masteredOccurrences.toDouble() / totalOccurrences.toDouble()) * 100.0

            // 3. Next lesson info
            var nextTitle: LocalizedText? = null
            var nextCategory: LemmaCategory? = null
            var nextKind: LessonKind? = null
            var nextWordCount = 0
            var nextSectionTitle: LocalizedText? = null

            if (!nextLessonId.isNullOrBlank()) {
                val nextLesson = contentRepository.getLesson(nextLessonId)
                if (nextLesson != null) {
                    nextTitle = nextLesson.title
                    nextCategory = nextLesson.category
                    nextKind = nextLesson.kind

                    val nextExercises = contentRepository.getExercisesForLesson(nextLessonId)
                    nextWordCount = nextExercises.count { ex ->
                        runCatching {
                            AppJson.decodeFromString(ExerciseContent.serializer(), ex.contentJson) is ExerciseContent.WordIntro
                        }.getOrDefault(false)
                    }

                    if (nextLesson.sectionId != null) {
                        val section = contentRepository.getSection(nextLesson.sectionId)
                        nextSectionTitle = section?.title
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    lessonTitle = lessonTitle,
                    lessonCategory = lessonCategory,
                    lessonKind = lessonKind,
                    wordsCoveredCount = wordsList.size,
                    wordsCoveredList = wordsList,
                    totalWordsLearned = totalWordsLearned,
                    totalQuranOccurrencesLearned = masteredOccurrences,
                    quranCoveragePercent = coveragePercent,
                    nextLessonId = nextLessonId,
                    nextLessonTitle = nextTitle,
                    nextLessonCategory = nextCategory,
                    nextLessonKind = nextKind,
                    nextLessonWordCount = nextWordCount,
                    nextLessonSectionTitle = nextSectionTitle
                )
            }
        }
    }
}
