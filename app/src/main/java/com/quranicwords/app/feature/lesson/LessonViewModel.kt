package com.quranicwords.app.feature.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.data.local.entity.WordFrequencyEntity
import com.quranicwords.app.core.domain.AdaptiveSequencer
import com.quranicwords.app.core.domain.DistractorGenerator
import com.quranicwords.app.core.domain.WordCandidatePool
import com.quranicwords.app.core.domain.model.ChoiceOption
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.ItemKind
import com.quranicwords.app.core.domain.model.LessonResult
import com.quranicwords.app.core.domain.model.OptionsBearing
import com.quranicwords.app.core.domain.model.WordSpan
import com.quranicwords.app.core.domain.model.isScored
import com.quranicwords.app.core.domain.model.practicedItemId
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.domain.repository.ProgressRepository
import com.quranicwords.app.core.util.AppJson
import com.quranicwords.app.core.util.AudioPlayer
import com.quranicwords.app.core.util.SfxEffect
import com.quranicwords.app.core.util.SfxPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** One mismatched-tap moment in a Matching exercise, for the shake animation - [token] is
 * monotonically incrementing so a repeat mismatch of the same pair still re-triggers the shake
 * (a plain data-class equality check wouldn't distinguish "the same wrong tap happened again"
 * from "nothing changed"). */
data class MismatchEvent(val token: Int, val leftId: String, val rightId: String)

data class ExerciseAttemptState(
    val selectedOptionId: String? = null,
    val matchedPairIds: Set<String> = emptySet(),
    val pendingLeftId: String? = null,
    val orderedChipIds: List<String> = emptyList(),
    val typedAnswer: String = "",
    val lastMismatch: MismatchEvent? = null,
    val selectedSpan: WordSpan? = null
)

data class LessonUiState(
    val isLoading: Boolean = true,
    val contents: List<ExerciseContent> = emptyList(),
    val currentIndex: Int = 0,
    val correctCount: Int = 0,
    val attempt: ExerciseAttemptState = ExerciseAttemptState(),
    val isChecked: Boolean = false,
    val lastAnswerCorrect: Boolean? = null,
    val isFinished: Boolean = false,
    val result: LessonResult? = null
) {
    val currentContent: ExerciseContent? get() = contents.getOrNull(currentIndex)
    val progressFraction: Float get() = if (contents.isEmpty()) 0f else (currentIndex + if (isChecked) 1 else 0).toFloat() / contents.size
}

@HiltViewModel
class LessonViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
    private val userIdProvider: CurrentUserIdProvider,
    private val audioPlayer: AudioPlayer,
    private val sfxPlayer: SfxPlayer,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    // Null exactly when this ViewModel was reached via Route.Review (a distinct destination with
    // no lessonId argument at all) rather than Route.Lesson - a type-safe alternative to the
    // sentinel-string approach this used to take, which risked colliding with a real lesson id.
    private val lessonId: String? = savedStateHandle["lessonId"]

    /** True when reached via Route.Review rather than a real lesson - [init] then assembles its
     * exercise list from missed items instead of a fixed lesson, and [finishLesson] records the
     * result via `completeReviewSession` instead of `completeLesson`. */
    private val isReviewSession: Boolean = lessonId == null

    // Single-value today (vocabulary words only) - see ItemKind's doc comment.
    private val itemKind: ItemKind = ItemKind.WORD

    private val _uiState = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = userIdProvider.get()

            // Independent reads, all started concurrently. For a review session, exercisesDeferred
            // depends on missedItemIdsDeferred (awaited inside its own block, not before starting
            // it) - fetched exactly once and threaded into getReviewExercises, rather than that
            // repository call re-querying it itself; the non-review path never touches it, so it
            // stays fully concurrent with the other two reads exactly as before.
            val missedItemIdsDeferred = async { progressRepository.getMissedItemIds(userId).toSet() }
            val exercisesDeferred = async {
                if (isReviewSession) progressRepository.getReviewExercises(missedItemIdsDeferred.await().toList())
                else contentRepository.getExercisesForLesson(checkNotNull(lessonId))
            }
            val candidatesDeferred = async {
                if (itemKind == ItemKind.WORD) contentRepository.getWordCandidates() else emptyList()
            }
            val exercises = exercisesDeferred.await()
            val missedItemIds = missedItemIdsDeferred.await()
            val candidates = candidatesDeferred.await()

            val contents = withContext(Dispatchers.Default) {
                // Built once per lesson load and reused for every options-bearing exercise below,
                // instead of DistractorGenerator re-sorting the full candidate list from scratch
                // per exercise (see WordCandidatePool's doc comment).
                val candidatePool = WordCandidatePool.from(candidates)
                val decoded = exercises
                    .map { AppJson.decodeFromString(ExerciseContent.serializer(), it.contentJson) }
                    // ListenAndType has no valid interaction without its audio clip - filtered
                    // out here rather than shown broken, same spirit as the existing
                    // "audio unavailable" note but applied before the exercise is ever reached.
                    .filter { content -> content !is ExerciseContent.ListenAndType || audioPlayer.isAvailable(content.audioAssetPath) }
                    .map { content -> regenerateDistractors(content, candidatePool, missedItemIds) }
                AdaptiveSequencer.reorderForAdaptivePractice(decoded, missedItemIds)
            }
            _uiState.update { it.copy(isLoading = false, contents = contents) }
        }
    }

    /** Replaces the content-pipeline's fixed, recency-baked options with a freshly-picked set
     * per lesson entry (see [DistractorGenerator]) - content that isn't [OptionsBearing]
     * (Matching, teach steps, WordOrderBuilder, ListenAndType) passes through unchanged. */
    private fun regenerateDistractors(
        content: ExerciseContent,
        candidatePool: WordCandidatePool,
        missedItemIds: Set<String>
    ): ExerciseContent = when (content) {
        is OptionsBearing ->
            content.withOptions(rebuildOptions(content.options, content.correctOptionId, candidatePool, missedItemIds))
        else -> content
    }

    /** Regenerates distractors from [candidatePool] and tops up from the pre-baked [baked] pool
     * when Room doesn't have enough siblings yet (e.g. alphabet items, which aren't in
     * [WordFrequencyEntity] at all - [candidatePool] is empty for those, so this falls through to
     * [baked] entirely). The fallback stays visible here rather than hidden in the generator. */
    private fun rebuildOptions(
        baked: List<ChoiceOption>,
        correctOptionId: String,
        candidatePool: WordCandidatePool,
        missedItemIds: Set<String>
    ): List<ChoiceOption> {
        val correctOption = baked.find { it.id == correctOptionId } ?: return baked
        val generated = DistractorGenerator.pickDistractors(correctOptionId, candidatePool, missedItemIds)
            .mapNotNull { id -> candidatePool.get(id) }
            .map { ChoiceOption(id = it.id, labelArabic = it.arabicWord, labelEn = it.meaningEn, labelBn = it.meaningBn) }

        val usedIds = generated.map { it.id }.toSet() + correctOptionId
        val stillNeeded = 3 - generated.size
        val topUp = if (stillNeeded > 0) baked.filter { it.id !in usedIds }.take(stillNeeded) else emptyList()

        return (generated + topUp + correctOption).shuffled()
    }

    /** Returns false (no throw) if the clip isn't bundled - callers show a subtle "unavailable"
     * indication rather than nothing happening silently. */
    fun playAudio(assetPath: String): Boolean = audioPlayer.play(assetPath)

    override fun onCleared() {
        audioPlayer.release()
    }

    fun selectOption(optionId: String) {
        _uiState.update {
            if (it.isChecked) it else it.copy(attempt = it.attempt.copy(selectedOptionId = optionId))
        }
    }

    fun onCheckPressed() {
        val state = _uiState.value
        val content = state.currentContent ?: return
        val correct = when (content) {
            is ExerciseContent.MultipleChoice -> state.attempt.selectedOptionId == content.correctOptionId
            is ExerciseContent.TapWhatYouHear -> state.attempt.selectedOptionId == content.correctOptionId
            is ExerciseContent.FillInTheBlank -> state.attempt.selectedOptionId == content.correctOptionId
            is ExerciseContent.WordOrderBuilder ->
                state.attempt.orderedChipIds == content.orderedChips.map { it.id }
            is ExerciseContent.ListenAndType -> {
                val typed = state.attempt.typedAnswer.trim()
                typed.equals(content.correctAnswer, ignoreCase = true) ||
                    content.acceptedAnswers.any { typed.equals(it, ignoreCase = true) }
            }
            else -> return // matching self-validates as it's solved
        }
        finalizeCheck(correct)
    }

    fun selectWordOrderChip(chipId: String) {
        _uiState.update {
            if (it.isChecked || chipId in it.attempt.orderedChipIds) it
            else it.copy(attempt = it.attempt.copy(orderedChipIds = it.attempt.orderedChipIds + chipId))
        }
    }

    fun deselectWordOrderChip(chipId: String) {
        _uiState.update {
            if (it.isChecked) it
            else it.copy(attempt = it.attempt.copy(orderedChipIds = it.attempt.orderedChipIds - chipId))
        }
    }

    fun updateTypedAnswer(text: String) {
        _uiState.update {
            if (it.isChecked) it else it.copy(attempt = it.attempt.copy(typedAnswer = text))
        }
    }

    fun selectMatchingLeft(pairId: String) {
        _uiState.update {
            if (it.isChecked || pairId in it.attempt.matchedPairIds) it
            else it.copy(attempt = it.attempt.copy(pendingLeftId = pairId))
        }
    }

    fun selectMatchingRight(pairId: String) {
        val state = _uiState.value
        val content = state.currentContent as? ExerciseContent.Matching ?: return
        val pendingLeft = state.attempt.pendingLeftId ?: return
        if (pairId in state.attempt.matchedPairIds) return

        if (pendingLeft == pairId) {
            val newMatched = state.attempt.matchedPairIds + pairId
            _uiState.update { it.copy(attempt = it.attempt.copy(matchedPairIds = newMatched, pendingLeftId = null)) }
            // Matching quizzes several pairs in one exercise, so each is logged individually here
            // (only the correct-match moment - a mismatched tap doesn't cleanly attribute to one
            // specific pair, see practicedItemId's doc comment) rather than through finalizeCheck.
            // pair.id ("p1", "p2"...) is lesson-scoped, not a global word id (see MatchPair's doc
            // comment) - logging under it would collide across lessons and corrupt missed-item
            // tracking, so this only logs when the pair actually carries its real wordId.
            val matchedPair = content.pairs.find { it.id == pairId }
            matchedPair?.wordId?.let { wordId ->
                logAttempt(itemId = wordId, exerciseType = ExerciseType.MATCHING, correct = true)
            }
            if (newMatched.size == content.pairs.size) {
                finalizeCheck(true)
            } else {
                // Immediate per-pair feedback, not just at full-exercise completion - the last
                // pair's ding is already covered by finalizeCheck above.
                viewModelScope.launch { sfxPlayer.play(SfxEffect.CORRECT) }
            }
        } else {
            val mismatchToken = (state.attempt.lastMismatch?.token ?: 0) + 1
            _uiState.update {
                it.copy(
                    attempt = it.attempt.copy(
                        pendingLeftId = null,
                        lastMismatch = MismatchEvent(mismatchToken, pendingLeft, pairId)
                    )
                )
            }
            viewModelScope.launch { sfxPlayer.play(SfxEffect.WRONG) }
        }
    }

    fun selectVerseWord(span: WordSpan) {
        val state = _uiState.value
        val content = state.currentContent as? ExerciseContent.TapWordInVerse ?: return
        if (state.attempt.selectedSpan != null) return // one-shot, no retry
        _uiState.update { it.copy(attempt = it.attempt.copy(selectedSpan = span)) }
        val correct = span.start == content.correctWordStart && span.end == content.correctWordEnd
        finalizeCheck(correct)
    }

    private fun finalizeCheck(correct: Boolean) {
        val content = _uiState.value.currentContent
        _uiState.update {
            it.copy(
                isChecked = true,
                lastAnswerCorrect = correct,
                correctCount = it.correctCount + if (correct) 1 else 0
            )
        }
        viewModelScope.launch { sfxPlayer.play(if (correct) SfxEffect.CORRECT else SfxEffect.WRONG) }
        val exerciseType = when (content) {
            is ExerciseContent.MultipleChoice -> ExerciseType.MULTIPLE_CHOICE
            is ExerciseContent.TapWhatYouHear -> ExerciseType.TAP_WHAT_YOU_HEAR
            is ExerciseContent.FillInTheBlank -> ExerciseType.FILL_IN_THE_BLANK
            is ExerciseContent.WordOrderBuilder -> ExerciseType.WORD_ORDER
            is ExerciseContent.ListenAndType -> ExerciseType.LISTEN_AND_TYPE
            is ExerciseContent.TapWordInVerse -> ExerciseType.WORD_IN_VERSE_TAP
            else -> return // Matching logs per-pair in selectMatchingRight; teach steps never reach here
        }
        val itemId = content.practicedItemId() ?: return
        logAttempt(itemId, exerciseType, correct)
    }

    private fun logAttempt(itemId: String, exerciseType: ExerciseType, correct: Boolean) {
        viewModelScope.launch {
            progressRepository.logAttempt(userIdProvider.get(), itemId, itemKind, exerciseType, correct)
        }
    }

    fun onContinuePressed() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.contents.size) {
            finishLesson()
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = nextIndex,
                    attempt = ExerciseAttemptState(),
                    isChecked = false,
                    lastAnswerCorrect = null
                )
            }
        }
    }

    private fun finishLesson() {
        viewModelScope.launch {
            val state = _uiState.value
            val userId = userIdProvider.get()
            val totalCount = state.contents.count { it.isScored }
            val result = if (isReviewSession) {
                progressRepository.completeReviewSession(
                    userId = userId,
                    correctCount = state.correctCount,
                    totalCount = totalCount
                )
            } else {
                progressRepository.completeLesson(
                    userId = userId,
                    lessonId = checkNotNull(lessonId),
                    correctCount = state.correctCount,
                    totalCount = totalCount
                )
            }
            _uiState.update { it.copy(isFinished = true, result = result) }
        }
    }
}
