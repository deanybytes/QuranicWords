package com.quranicwords.app.feature.learnedwords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.LocalizedText
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.LocalizedWord
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.repository.WordExampleLocalizer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
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
    /** The word's teach step - the source of its localized senses and examples. */
    val intro: ExerciseContent.WordIntro? = null,
    val strength: WordStrength = WordStrength.NEW
) {
    /** "surah:ayah" of every verse any of the word's senses cites, for search by reference. */
    val verseKeys: Set<String> get() = intro?.senses?.values?.flatMapTo(HashSet()) { senses -> senses.map { it.verse } }.orEmpty()
}

data class LearnedWordsUiState(
    val isLoading: Boolean = true,
    val allLearnedWords: List<LearnedWordItem> = emptyList(),
    val filteredWords: List<LearnedWordItem> = emptyList(),
    val searchQuery: String = "",
    val selectedWordForDetail: LearnedWordItem? = null,
    /** [selectedWordForDetail] in the learner's current language (see WordExampleLocalizer). */
    val selectedWordLocalized: LocalizedWord? = null
) {
    /** "Strong+" - the same learned-word definition Progress and the lesson summary count. */
    val strongCount: Int get() = allLearnedWords.count { it.strength.isStrongOrBetter }
}

@HiltViewModel
class LearnedWordsViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val contentRepository: ContentRepository,
    private val userIdProvider: CurrentUserIdProvider,
    private val preferences: UserPreferencesDataStore,
    private val exampleLocalizer: WordExampleLocalizer
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedWordForDetail = MutableStateFlow<LearnedWordItem?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _learnedWords = MutableStateFlow<List<LearnedWordItem>>(emptyList())

    /** Only the opened word is localized - on opening it, and again on a language switch. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _selectedLocalized: Flow<LocalizedWord?> = combine(
        _selectedWordForDetail,
        preferences.languageFlow.map { it ?: Language.ENGLISH }.distinctUntilChanged()
    ) { word, language -> word to language }
        .mapLatest { (word, language) -> word?.intro?.let { exampleLocalizer.localize(it, language) } }

    val uiState: StateFlow<LearnedWordsUiState> = combine(
        _isLoading,
        _learnedWords,
        _searchQuery,
        _selectedWordForDetail,
        _selectedLocalized
    ) { isLoading, words, query, selectedWord, selectedLocalized ->
        val filtered = if (query.isBlank()) {
            words
        } else {
            // Arabic is matched without its diacritics, so "كتب" finds "كَتَبَ".
            words.filter { item ->
                ArabicSearch.matches(item.arabicWord, query) ||
                    item.meaning.values.any { ArabicSearch.matches(it, query) } ||
                    ArabicSearch.matches(item.root, query) ||
                    query.trim().let { q -> item.verseKeys.any { it == q } }
            }
        }
        LearnedWordsUiState(
            isLoading = isLoading,
            allLearnedWords = words,
            filteredWords = filtered,
            searchQuery = query,
            selectedWordForDetail = selectedWord,
            selectedWordLocalized = selectedLocalized?.takeIf { it.wordId == selectedWord?.wordId }
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
                        intro = intro,
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
