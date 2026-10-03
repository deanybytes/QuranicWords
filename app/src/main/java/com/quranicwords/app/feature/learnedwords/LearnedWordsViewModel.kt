package com.quranicwords.app.feature.learnedwords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.LocalizedText
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.domain.repository.ProgressRepository
import com.quranicwords.app.core.domain.srs.WordStrength
import com.quranicwords.app.core.util.ArabicSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LearnedWordItem(
    val wordId: String,
    val arabicWord: String,
    val meaning: LocalizedText,
    val root: String? = null,
    val frequencyRank: Int,
    val frequencyCount: Int,
    val exampleVerseArabic: String? = null,
    val exampleVerseTranslation: LocalizedText = emptyMap(),
    val exampleVerseReference: String? = null,
    val arabicWordStart: Int? = null,
    val arabicWordEnd: Int? = null,
    val meaningHighlight: LocalizedText = emptyMap(),
    val polysemyEntries: List<ExerciseContent.PolysemyEntry> = emptyList(),
    val strength: WordStrength = WordStrength.NEW
)

data class LearnedWordsUiState(
    val isLoading: Boolean = true,
    val allLearnedWords: List<LearnedWordItem> = emptyList(),
    val filteredWords: List<LearnedWordItem> = emptyList(),
    val searchQuery: String = "",
    val selectedWordForDetail: LearnedWordItem? = null
) {
    /** "Strong+" - the same learned-word definition Progress and the lesson summary count. */
    val strongCount: Int get() = allLearnedWords.count { it.strength.isStrongOrBetter }
}

@HiltViewModel
class LearnedWordsViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val contentRepository: ContentRepository,
    private val userIdProvider: CurrentUserIdProvider
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedWordForDetail = MutableStateFlow<LearnedWordItem?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _learnedWords = MutableStateFlow<List<LearnedWordItem>>(emptyList())

    val uiState: StateFlow<LearnedWordsUiState> = combine(
        _isLoading,
        _learnedWords,
        _searchQuery,
        _selectedWordForDetail
    ) { isLoading, words, query, selectedWord ->
        val filtered = if (query.isBlank()) {
            words
        } else {
            // Arabic is matched without its diacritics, so "كتب" finds "كَتَبَ".
            words.filter { item ->
                ArabicSearch.matches(item.arabicWord, query) ||
                    item.meaning.values.any { ArabicSearch.matches(it, query) } ||
                    ArabicSearch.matches(item.root, query) ||
                    item.exampleVerseReference?.contains(query.trim()) == true
            }
        }
        LearnedWordsUiState(
            isLoading = isLoading,
            allLearnedWords = words,
            filteredWords = filtered,
            searchQuery = query,
            selectedWordForDetail = selectedWord
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LearnedWordsUiState())

    init {
        viewModelScope.launch {
            val userId = userIdProvider.get()
            val allCandidates = contentRepository.getWordCandidates().associateBy { it.id }
            val wordIntros = contentRepository.getAllWordIntros()

            // Every word with a memory, strongest first - each card shows its own strength, so the
            // list doubles as a "what to firm up next" view rather than only listing Strong+ words.
            progressRepository.observeWordStrengths(userId).collect { strengths ->
                val items = strengths.mapNotNull { (wordId, strength) ->
                    val cand = allCandidates[wordId] ?: return@mapNotNull null
                    val intro = wordIntros[wordId]
                    LearnedWordItem(
                        wordId = wordId,
                        arabicWord = cand.arabicWord,
                        meaning = cand.meaning,
                        root = intro?.root,
                        frequencyRank = cand.frequencyRank,
                        frequencyCount = cand.frequencyCount,
                        exampleVerseArabic = intro?.exampleVerseArabic,
                        exampleVerseTranslation = intro?.exampleVerseTranslation ?: emptyMap(),
                        exampleVerseReference = intro?.exampleVerseReference,
                        arabicWordStart = intro?.arabicWordStart,
                        arabicWordEnd = intro?.arabicWordEnd,
                        meaningHighlight = intro?.meaningHighlight ?: emptyMap(),
                        polysemyEntries = intro?.polysemyEntries ?: emptyList(),
                        strength = strength
                    )
                }.sortedWith(compareByDescending<LearnedWordItem> { it.strength.level }.thenBy { it.frequencyRank })

                _learnedWords.value = items
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSelectWordForDetail(word: LearnedWordItem?) {
        _selectedWordForDetail.value = word
    }
}
