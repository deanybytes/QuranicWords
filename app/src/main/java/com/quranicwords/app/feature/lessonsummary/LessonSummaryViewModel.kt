package com.quranicwords.app.feature.lessonsummary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.domain.CoverageCalculator
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.LemmaCategory
import com.quranicwords.app.core.domain.model.LocalizedText
import com.quranicwords.app.core.domain.model.REVIEW_SESSION_LESSON_ID
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.domain.repository.ProgressRepository
import com.quranicwords.app.core.navigation.Route
import com.quranicwords.app.core.util.AppJson
import com.quranicwords.app.core.domain.LevelCurve
import com.quranicwords.app.core.domain.model.LessonSessionType
import com.quranicwords.app.core.domain.requiresPassingScore
import com.quranicwords.app.core.util.GamificationConfig
import com.quranicwords.app.core.util.SfxEffect
import com.quranicwords.app.core.util.SfxPlayer
import com.quranicwords.app.core.util.StreakTiers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    /** Review/Open Practice/Streak Recovery - no [lessonTitle]; titled via
     * `R.string.lesson_summary_practice_session_title` instead. */
    val isPracticeSession: Boolean = false,
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
    val nextLessonSectionTitle: LocalizedText? = null,
    /** Words whose first try this session was wrong. */
    val missedWords: List<WordSummaryItem> = emptyList()
)

/** The summary's celebration moments, derived once from the route. */
data class SummaryCelebration(
    /** The new level, when this session crossed a level boundary. */
    val leveledUpTo: Int? = null,
    /** 7, 30 or 100 when this session's streak day landed exactly on a milestone. */
    val streakMilestone: Int? = null
)

@HiltViewModel
class LessonSummaryViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
    private val userIdProvider: CurrentUserIdProvider,
    private val sfxPlayer: SfxPlayer,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route: Route.LessonSummary? = runCatching { savedStateHandle.toRoute<Route.LessonSummary>() }.getOrNull()
    private val lessonId: String? = route?.lessonId ?: savedStateHandle["lessonId"]
    private val nextLessonId: String? = route?.nextLessonId ?: savedStateHandle["nextLessonId"]
    private val practicedWordIds: List<String> = route?.practicedWordIds ?: savedStateHandle.get<List<String>>("practicedWordIds") ?: emptyList()

    private val _uiState = MutableStateFlow(LessonSummaryUiState())
    val uiState: StateFlow<LessonSummaryUiState> = _uiState.asStateFlow()

    val celebration: SummaryCelebration = route?.let { celebrationFor(it) } ?: SummaryCelebration()

    init {
        loadSummaryDetails()
        playCompletionSoundOnce()
    }

    /** One sound for the biggest moment - a streak milestone outranks a level-up or exam pass,
     * which outrank a plain completion. Once per summary (survives rotation via the handle). */
    private fun playCompletionSoundOnce() {
        val r = route ?: return
        if (savedStateHandle.get<Boolean>(SOUND_PLAYED_KEY) == true) return
        savedStateHandle[SOUND_PLAYED_KEY] = true
        val passed = summaryPassed(r)
        val examPassed = passed && r.lessonKind?.requiresPassingScore() == true
        val effect = when {
            celebration.streakMilestone != null -> SfxEffect.STREAK_MILESTONE
            celebration.leveledUpTo != null || examPassed -> SfxEffect.EXAM_PASS
            passed && r.totalCount > 0 -> SfxEffect.LESSON_COMPLETE
            else -> null
        } ?: return
        viewModelScope.launch { sfxPlayer.play(effect) }
    }

    private fun loadSummaryDetails() {
        viewModelScope.launch {
            try {
                val userId = userIdProvider.get()

                val stateUpdate = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    // 1. Current lesson words & info
                    var lessonTitle: LocalizedText? = null
                    var lessonCategory: LemmaCategory? = null
                    var lessonKind: LessonKind? = null
                    var isPracticeSession = false
                    val wordsList = mutableListOf<WordSummaryItem>()
                    val wordCategories = contentRepository.getWordCategories()

                    if (!lessonId.isNullOrBlank() && lessonId != REVIEW_SESSION_LESSON_ID) {
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
                                    val category = content.lemmaCategory
                                    wordsList.add(
                                        WordSummaryItem(
                                            wordId = content.wordId,
                                            arabicWord = content.arabicWord,
                                            meaning = meaning,
                                            category = category
                                        )
                                    )
                                }
                            }
                        }
                    } else if (practicedWordIds.isNotEmpty() || lessonId == REVIEW_SESSION_LESSON_ID) {
                        // No lesson to name - the screen titles it from a string resource.
                        isPracticeSession = true
                        val wordCandidates = contentRepository.getWordCandidates().associateBy { it.id }
                        val wordIntros = contentRepository.getWordIntrosForItems(practicedWordIds)

                        practicedWordIds.distinct().forEach { wordId ->
                            val candidate = wordCandidates[wordId]
                            val intro = wordIntros[wordId]
                            if (candidate != null || intro != null) {
                                val arabicWord = candidate?.arabicWord ?: intro?.arabicWord ?: ""
                                val meaning = candidate?.meaning ?: intro?.meaning ?: emptyMap()
                                val category = wordCategories[wordId]
                                    ?: intro?.lemmaCategory
                                    ?: LemmaCategory.NOUN
                                wordsList.add(
                                    WordSummaryItem(
                                        wordId = wordId,
                                        arabicWord = arabicWord,
                                        meaning = meaning,
                                        category = category
                                    )
                                )
                            }
                        }
                    }

                    val missedWords = route?.missedWordIds.orEmpty().distinct().let { ids ->
                        if (ids.isEmpty()) emptyList() else {
                            val candidates = contentRepository.getWordCandidates().associateBy { it.id }
                            ids.mapNotNull { id ->
                                val candidate = candidates[id] ?: return@mapNotNull null
                                WordSummaryItem(id, candidate.arabicWord, candidate.meaning, wordCategories[id] ?: LemmaCategory.NOUN)
                            }
                        }
                    }

                    // 2. Cumulative progress stats
                    val masteredWordIds = progressRepository.getMasteredItemIds(userId).toSet()
                    val allWords = contentRepository.getWordCandidates()
                    val totalWordsLearned = masteredWordIds.size
                    val masteredOccurrences = allWords.filter { it.id in masteredWordIds }.sumOf { it.frequencyCount }
                    // Same coverage figure as Home/Progress (completed lessons) - see CoverageCalculator.
                    val completedLessonIds = progressRepository.observeProgress(userId).first()
                        .filter { it.status == LessonStatus.COMPLETED }
                        .map { it.lessonId }
                        .toSet()
                    val coveragePercent = CoverageCalculator.coverageByChapter(
                        contentRepository.getFullCurriculumTree(), completedLessonIds
                    ).values.sum()

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

                    LessonSummaryUiState(
                        isLoading = false,
                        lessonTitle = lessonTitle,
                        lessonCategory = lessonCategory,
                        lessonKind = lessonKind,
                        isPracticeSession = isPracticeSession,
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
                        nextLessonSectionTitle = nextSectionTitle,
                        missedWords = missedWords
                    )
                }

                _uiState.value = stateUpdate
            } catch (e: Throwable) {
                android.util.Log.e("LessonSummaryViewModel", "Failed to load summary details", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

private const val SOUND_PLAYED_KEY = "summarySoundPlayed"

/** Same pass rule the screen and `ProgressRepository.completeLesson` use. */
fun summaryPassed(route: Route.LessonSummary): Boolean =
    if (route.sessionType == LessonSessionType.STREAK_RECOVERY) {
        route.streakIncreased
    } else {
        route.lessonKind?.requiresPassingScore() != true ||
            GamificationConfig.percentOf(route.correctCount, route.totalCount) >= GamificationConfig.PASSING_SCORE_PERCENT
    }

/** Pure: which celebration moments a finished session earned. */
fun celebrationFor(route: Route.LessonSummary): SummaryCelebration {
    val before = LevelCurve.levelFor(route.previousTotalPoints)
    val after = LevelCurve.levelFor(route.newTotalPoints)
    val milestone = route.currentStreak.takeIf {
        route.streakIncreased && it in setOf(StreakTiers.BRONZE, StreakTiers.SILVER, StreakTiers.GOLD)
    }
    return SummaryCelebration(leveledUpTo = after.takeIf { it > before }, streakMilestone = milestone)
}
