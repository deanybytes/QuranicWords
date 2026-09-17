package com.quranicwords.app.feature.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.local.entity.WordFrequencyEntity
import com.quranicwords.app.core.domain.AchievementDef
import com.quranicwords.app.core.domain.AdaptiveSequencer
import com.quranicwords.app.core.domain.DistractorGenerator
import com.quranicwords.app.core.domain.LessonContentRepeater
import com.quranicwords.app.core.domain.StreakRecovery
import com.quranicwords.app.core.domain.WordCandidatePool
import com.quranicwords.app.core.domain.model.ChoiceOption
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.ItemKind
import com.quranicwords.app.core.domain.model.LessonResult
import com.quranicwords.app.core.domain.model.LessonSessionType
import com.quranicwords.app.core.domain.model.OptionsBearing
import com.quranicwords.app.core.domain.model.REVIEW_SESSION_LESSON_ID
import com.quranicwords.app.core.domain.model.WordSpan
import com.quranicwords.app.core.domain.model.isScored
import com.quranicwords.app.core.domain.model.practicedItemId
import com.quranicwords.app.core.domain.repository.AchievementRepository
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.domain.repository.ProgressRepository
import com.quranicwords.app.core.util.AppJson
import com.quranicwords.app.core.util.SfxEffect
import com.quranicwords.app.core.util.SfxPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
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
    val result: LessonResult? = null,
    val newlyUnlockedAchievements: List<AchievementDef> = emptyList()
) {
    val currentContent: ExerciseContent? get() = contents.getOrNull(currentIndex)
    val progressFraction: Float get() = if (contents.isEmpty()) 0f else (currentIndex + if (isChecked) 1 else 0).toFloat() / contents.size
}

@HiltViewModel
class LessonViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
    private val achievementRepository: AchievementRepository,
    private val userIdProvider: CurrentUserIdProvider,
    private val sfxPlayer: SfxPlayer,
    private val preferences: UserPreferencesDataStore,
    private val clock: Clock,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    // Null exactly when this ViewModel was reached via Route.Review (a distinct destination with
    // no lessonId argument at all) rather than Route.Lesson - a type-safe alternative to the
    // sentinel-string approach this used to take, which risked colliding with a real lesson id.
    private val lessonId: String? = savedStateHandle["lessonId"]

    // Captured once at construction - finishLesson() diffs against this to report how long the
    // learner actually spent, threaded into ProgressRepository's duration tracking.
    private val sessionStartMillis: Long = clock.millis()

    /** True when reached via Route.OpenPractice - see that route's doc comment for why this is a
     * `SavedStateHandle` field rather than another null-lessonId inference. Checked before
     * [isReviewSession] everywhere the two matter, since both leave [lessonId] null. */
    private val isOpenPractice: Boolean = savedStateHandle["isOpenPractice"] ?: false

    val openPracticeMode: String? = savedStateHandle["mode"]

    /** True when reached via Route.StreakRecovery - same `SavedStateHandle` marker shape as
     * [isOpenPractice], for the same reason (a fourth distinguishable state, all sharing the
     * null-[lessonId] convention). See [StreakRecovery]. */
    private val isStreakRecovery: Boolean = savedStateHandle["isStreakRecovery"] ?: false

    /** True when reached via Route.Review rather than a real lesson - [init] then assembles its
     * exercise list from missed items instead of a fixed lesson, and [finishLesson] records the
     * result via `completeReviewSession` instead of `completeLesson`. False for Open Practice and
     * Streak Recovery too (see [isOpenPractice]/[isStreakRecovery]) even though Open Practice
     * shares `completeReviewSession`'s semantics - all three differ in exercise-sourcing ([init])
     * and in what [finishLesson] reports back as the result's `sessionType`. */
    private val isReviewSession: Boolean = lessonId == null && !isOpenPractice && !isStreakRecovery

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
                when {
                    isStreakRecovery -> {
                        val currentStreak = progressRepository.observeStats(userId).first()?.currentStreak ?: 0
                        val count = StreakRecovery.recoveryQuestionCount(currentStreak) ?: 3
                        progressRepository.getStreakRecoveryExercises(userId, count)
                    }
                    isOpenPractice -> {
                        val mode: String = savedStateHandle["mode"] ?: "RANDOM"
                        progressRepository.getOpenPracticeExercises(userId, mode = mode)
                    }
                    isReviewSession -> progressRepository.getReviewExercises(missedItemIdsDeferred.await().toList())
                    else -> contentRepository.getExercisesForLesson(checkNotNull(lessonId))
                }
            }
            val candidatesDeferred = async {
                if (itemKind == ItemKind.WORD) contentRepository.getWordCandidates() else emptyList()
            }
            val exercises = exercisesDeferred.await()
            val practicedIds = exercises.mapNotNull { it.practicedItemId }
            val wordIntrosDeferred = async { contentRepository.getWordIntrosForItems(practicedIds) }
            val missedItemIds = missedItemIdsDeferred.await()
            val candidates = candidatesDeferred.await()
            val wordIntros = wordIntrosDeferred.await()
            val learningStyle = preferences.learningStyleFlow.first()


            val contents = withContext(Dispatchers.Default) {
                // Built once per lesson load and reused for every options-bearing exercise below,
                // instead of DistractorGenerator re-sorting the full candidate list from scratch
                // per exercise (see WordCandidatePool's doc comment).
                val candidatePool = WordCandidatePool.from(candidates)
                val decoded = exercises
                    .map { AppJson.decodeFromString(ExerciseContent.serializer(), it.contentJson) }
                    .filter { content -> content !is ExerciseContent.TapWhatYouHear && content !is ExerciseContent.ListenAndType }
                // Applied on decoded, pre-distractor content (see LessonContentRepeater's doc
                // comment) so each repeated instance gets its own independent distractor/shuffle
                // pass below, rather than N identical copies of the same regenerated exercise.
                // Open Practice and Streak Recovery always use a 1x multiplier (no-op) regardless
                // of the learner's stored LearningStyle - each batch is already freshly
                // randomized, so repeating a word within one batch wouldn't add the same "extra
                // reinforcement" it does for a fixed lesson's authored word set.
                val repeatCount = if (isOpenPractice || isStreakRecovery) 1 else learningStyle.repeatCount
                val sequenced = AdaptiveSequencer.reorderForAdaptivePractice(decoded, missedItemIds)
                LessonContentRepeater.apply(sequenced, repeatCount)
                    .map { content -> resolveCanonicalMeaning(content, candidatePool, wordIntros) }
                    .map { content -> regenerateDistractors(content, candidatePool, missedItemIds) }
            }
            _uiState.update { it.copy(isLoading = false, contents = contents) }
        }
    }

    /** Overrides every baked, content-pipeline-authored copy of a word's meaning with the single
     * canonical value from [WordFrequencyEntity] (via [candidatePool]) and enriches quiz types
     * with their canonical Quran example verses from [wordIntros].
     */
    private fun resolveCanonicalMeaning(
        content: ExerciseContent,
        candidatePool: WordCandidatePool,
        wordIntros: Map<String, ExerciseContent.WordIntro>
    ): ExerciseContent =
        when (content) {
            is ExerciseContent.WordIntro ->
                candidatePool.get(content.wordId)?.let { content.copy(meaning = it.meaning) } ?: content
            is ExerciseContent.TapWordInVerse ->
                candidatePool.get(content.wordId)?.let { content.copy(meaning = it.meaning) } ?: content
            is ExerciseContent.MultipleChoice -> {
                val intro = wordIntros[content.wordId]
                if (intro != null) {
                    content.copy(
                        exampleVerseArabic = intro.exampleVerseArabic,
                        exampleVerseTranslation = intro.exampleVerseTranslation,
                        exampleVerseReference = intro.exampleVerseReference,
                        arabicWordStart = intro.arabicWordStart,
                        arabicWordEnd = intro.arabicWordEnd,
                        meaningHighlight = intro.meaningHighlight
                    )
                } else content
            }
            is ExerciseContent.Matching -> content.copy(
                pairs = content.pairs.map { pair ->
                    var updated = pair.wordId?.let { candidatePool.get(it) }?.let { pair.copy(right = it.meaning) } ?: pair
                    val intro = pair.wordId?.let { wordIntros[it] }
                    if (intro != null) {
                        updated = updated.copy(
                            exampleVerseArabic = intro.exampleVerseArabic,
                            exampleVerseTranslation = intro.exampleVerseTranslation,
                            exampleVerseReference = intro.exampleVerseReference,
                            arabicWordStart = intro.arabicWordStart,
                            arabicWordEnd = intro.arabicWordEnd,
                            meaningHighlight = intro.meaningHighlight
                        )
                    }
                    updated
                }
            )
            else -> content
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
            content.withOptions(
                rebuildOptions(content.options, content.wordId, content.correctOptionId, candidatePool, missedItemIds)
            )
        is ExerciseContent.Matching ->
            content.copy(distractorRight = pickMatchingDistractor(content, candidatePool, missedItemIds))
        else -> content
    }

    /** One extra, never-matchable meaning-side tile for the Matching exercise (see
     * ExerciseContent.Matching.distractorRight's doc comment) - maps
     * DistractorGenerator.pickMatchingDistractor's plain id result to a renderable ChoiceOption. */
    private fun pickMatchingDistractor(
        content: ExerciseContent.Matching,
        candidatePool: WordCandidatePool,
        missedItemIds: Set<String>
    ): ChoiceOption? {
        val usedWordIds = content.pairs.mapNotNull { it.wordId }.toSet()
        val distractorId = DistractorGenerator.pickMatchingDistractor(usedWordIds, candidatePool, missedItemIds)
            ?: return null
        val word = candidatePool.get(distractorId) ?: return null
        return ChoiceOption(id = word.id, labelArabic = word.arabicWord, label = word.meaning)
    }

    /** Regenerates distractors from [candidatePool] and tops up from the pre-baked [baked] pool
     * when Room doesn't have enough siblings yet (e.g. alphabet items, which aren't in
     * [WordFrequencyEntity] at all - [candidatePool] is empty for those, so this falls through to
     * [baked] entirely). The fallback stays visible here rather than hidden in the generator.
     *
     * The correct option's label is resolved fresh from [candidatePool] via [wordId] rather than
     * trusting [baked]'s copy verbatim - the same canonical-meaning discipline as
     * [resolveCanonicalMeaning], applied here since this is where the correct option is otherwise
     * the one baked value nothing else in the pipeline touches. */
    private fun rebuildOptions(
        baked: List<ChoiceOption>,
        wordId: String,
        correctOptionId: String,
        candidatePool: WordCandidatePool,
        missedItemIds: Set<String>
    ): List<ChoiceOption> {
        val bakedCorrectOption = baked.find { it.id == correctOptionId } ?: return baked
        val correctOption = candidatePool.get(wordId)
            ?.let { ChoiceOption(id = correctOptionId, labelArabic = it.arabicWord, label = it.meaning) }
            ?: bakedCorrectOption

        val generated = DistractorGenerator.pickDistractors(wordId, candidatePool, missedItemIds, count = 3)
            .mapNotNull { id -> candidatePool.get(id) }
            .map { ChoiceOption(id = it.id, labelArabic = it.arabicWord, label = it.meaning) }

        val selectedOptions = mutableListOf<ChoiceOption>()
        selectedOptions.add(correctOption)

        for (gen in generated) {
            if (selectedOptions.none { optionsCollide(it, gen) }) {
                selectedOptions.add(gen)
            }
        }

        // Top up from baked if needed, strictly rejecting any duplicate meanings or labels
        if (selectedOptions.size < 4) {
            for (bakedOpt in baked) {
                if (selectedOptions.size >= 4) break
                if (selectedOptions.none { optionsCollide(it, bakedOpt) }) {
                    selectedOptions.add(bakedOpt)
                }
            }
        }

        // Fallback to wider pool if still needed to ensure 4 strictly unique options
        if (selectedOptions.size < 4) {
            val extraCandidates = DistractorGenerator.pickDistractors(wordId, candidatePool, missedItemIds, count = 12)
                .mapNotNull { candidatePool.get(it) }
            for (cand in extraCandidates) {
                if (selectedOptions.size >= 4) break
                val opt = ChoiceOption(id = cand.id, labelArabic = cand.arabicWord, label = cand.meaning)
                if (selectedOptions.none { optionsCollide(it, opt) }) {
                    selectedOptions.add(opt)
                }
            }
        }

        return selectedOptions.shuffled()
    }

    private fun optionsCollide(a: ChoiceOption, b: ChoiceOption): Boolean {
        if (a.id == b.id) return true

        val arabicA = a.labelArabic?.trim().orEmpty()
        val arabicB = b.labelArabic?.trim().orEmpty()
        if (arabicA.isNotEmpty() && arabicB.isNotEmpty() && arabicA == arabicB) return true

        val commonKeys = a.label.keys.intersect(b.label.keys)
        for (k in commonKeys) {
            val valA = a.label[k]?.trim()?.lowercase().orEmpty()
            val valB = b.label[k]?.trim()?.lowercase().orEmpty()
            if (valA.isNotEmpty() && valB.isNotEmpty()) {
                if (valA == valB) return true
                val tokensA = valA.split('/').map { it.trim() }.filter { it.isNotEmpty() }
                val tokensB = valB.split('/').map { it.trim() }.filter { it.isNotEmpty() }
                if (tokensA.any { it in tokensB }) return true
            }
        }

        val enA = (a.label["en"] ?: a.label.values.firstOrNull())?.trim()?.lowercase().orEmpty()
        val enB = (b.label["en"] ?: b.label.values.firstOrNull())?.trim()?.lowercase().orEmpty()
        if (enA.isNotEmpty() && enB.isNotEmpty()) {
            if (enA == enB) return true
            val tokensA = enA.split('/').map { it.trim() }.filter { it.isNotEmpty() }
            val tokensB = enB.split('/').map { it.trim() }.filter { it.isNotEmpty() }
            if (tokensA.any { it in tokensB }) return true
        }

        return false
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
            is ExerciseContent.FillInTheBlank -> state.attempt.selectedOptionId == content.correctOptionId
            is ExerciseContent.WordOrderBuilder ->
                state.attempt.orderedChipIds == content.orderedChips.map { it.id }
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
            else it.copy(attempt = it.attempt.copy(pendingLeftId = if (it.attempt.pendingLeftId == pairId) null else pairId))
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
            is ExerciseContent.FillInTheBlank -> ExerciseType.FILL_IN_THE_BLANK
            is ExerciseContent.WordOrderBuilder -> ExerciseType.WORD_ORDER
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

    fun onTryAgainPressed() {
        _uiState.update {
            it.copy(
                attempt = ExerciseAttemptState(),
                isChecked = false,
                lastAnswerCorrect = null
            )
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
            val durationMillis = (clock.millis() - sessionStartMillis).coerceAtLeast(0L)
            val result = if (isStreakRecovery) {
                // Deliberately not completeLesson/completeReviewSession - see
                // attemptStreakRecovery's doc comment for why. No points awarded either: this is
                // a "prove you know it" gate, not practice, so it shouldn't double as a way to
                // farm bonus points on top of getting the streak back.
                val passed = progressRepository.attemptStreakRecovery(userId, state.correctCount, totalCount)
                val stats = progressRepository.observeStats(userId).first()
                LessonResult(
                    lessonId = REVIEW_SESSION_LESSON_ID,
                    correctCount = state.correctCount,
                    totalCount = totalCount,
                    pointsAwarded = 0,
                    newTotalPoints = stats?.totalPoints ?: 0,
                    currentStreak = stats?.currentStreak ?: 0,
                    // Reused for this session type only to mean "recovery succeeded" - see
                    // LessonSummaryScreen's streakIncreased handling under STREAK_RECOVERY.
                    streakIncreased = passed,
                    durationMillis = durationMillis,
                    sessionType = LessonSessionType.STREAK_RECOVERY
                )
            } else if (isReviewSession || isOpenPractice) {
                progressRepository.completeReviewSession(
                    userId = userId,
                    correctCount = state.correctCount,
                    totalCount = totalCount,
                    durationMillis = durationMillis,
                    sessionType = if (isOpenPractice) LessonSessionType.OPEN_PRACTICE else LessonSessionType.REVIEW
                )
            } else {
                progressRepository.completeLesson(
                    userId = userId,
                    lessonId = checkNotNull(lessonId),
                    correctCount = state.correctCount,
                    totalCount = totalCount,
                    durationMillis = durationMillis
                )
            }
            // Checked after both completion paths (a Review session can cross a word-mastery-style
            // milestone too, not just a real lesson/exam) - never blocks showing the result itself,
            // an empty list here just means nothing newly unlocked this time.
            val newlyUnlocked = achievementRepository.checkAndUnlock(userId)
            _uiState.update { it.copy(isFinished = true, result = result, newlyUnlockedAchievements = newlyUnlocked) }
        }
    }
}
