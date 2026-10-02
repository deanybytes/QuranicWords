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
import com.quranicwords.app.core.domain.model.LemmaCategory
import com.quranicwords.app.core.domain.model.LessonResult
import com.quranicwords.app.core.domain.model.LessonSessionType
import com.quranicwords.app.core.domain.model.OptionsBearing
import com.quranicwords.app.core.domain.model.REVIEW_SESSION_LESSON_ID
import com.quranicwords.app.core.domain.model.WordSpan
import com.quranicwords.app.core.domain.model.cleanArabicDisplay
import com.quranicwords.app.core.domain.model.isScored
import com.quranicwords.app.core.domain.model.practicedItemId
import com.quranicwords.app.core.domain.repository.AchievementRepository
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.domain.repository.ProgressRepository
import com.quranicwords.app.core.domain.srs.WordStrength
import com.quranicwords.app.core.util.AudioPlayer
import com.quranicwords.app.core.util.AppJson
import com.quranicwords.app.core.util.SfxEffect
import com.quranicwords.app.core.util.SessionTimer
import com.quranicwords.app.core.util.SfxPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    val selectedSpan: WordSpan? = null,
    /** Left-tile pair ids that took at least one wrong tap during this Matching attempt - any
     * entry makes the whole exercise grade as wrong once solved. */
    val mismatchedPairIds: Set<String> = emptySet()
)

data class LessonUiState(
    val isLoading: Boolean = true,
    val contents: List<ExerciseContent> = emptyList(),
    val currentIndex: Int = 0,
    val scoring: LessonScoring = LessonScoring(),
    val attempt: ExerciseAttemptState = ExerciseAttemptState(),
    val isChecked: Boolean = false,
    val lastAnswerCorrect: Boolean? = null,
    val isFinished: Boolean = false,
    val result: LessonResult? = null,
    val newlyUnlockedAchievements: List<AchievementDef> = emptyList(),
    /** For the exercises' grammar-category badges - see `LocalWordCategories`. */
    val wordCategories: Map<String, LemmaCategory> = emptyMap(),
    /** Memory strength per word as of session start, for the teach step's strength meter - a
     * snapshot, so the meter doesn't jump mid-lesson as answers update `word_memory`. */
    val wordStrengths: Map<String, WordStrength> = emptyMap()
) {
    /** First-try correct answers only - see [LessonScoring]. */
    val correctCount: Int get() = scoring.correctCount
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
    private val audioPlayer: AudioPlayer,
    private val preferences: UserPreferencesDataStore,
    private val clock: Clock,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    // Null exactly when this ViewModel was reached via Route.Review (a distinct destination with
    // no lessonId argument at all) rather than Route.Lesson - a type-safe alternative to the
    // sentinel-string approach this used to take, which risked colliding with a real lesson id.
    private val lessonId: String? = savedStateHandle["lessonId"]

    // Active practice time only: started on first content render ([onContentShown]) and paused
    // while the app is backgrounded ([onPause]/[onResume]), then threaded into
    // ProgressRepository's duration/daily-minutes tracking by finishLesson().
    private val sessionTimer = SessionTimer(clock::millis)

    // Re-entry guard for onContinuePressed: a fast double tap would otherwise advance twice and
    // silently skip the next exercise. Main-thread only, like every other UI entry point here.
    private var lastAdvanceMillis = Long.MIN_VALUE

    /** True when reached via Route.OpenPractice - see that route's doc comment for why this is a
     * `SavedStateHandle` field rather than another null-lessonId inference. Checked before
     * [isReviewSession] everywhere the two matter, since both leave [lessonId] null. */
    private val isOpenPractice: Boolean = savedStateHandle["isOpenPractice"] ?: false

    val openPracticeMode: String? = savedStateHandle["mode"]

    /** True when reached via Route.StreakRecovery - same `SavedStateHandle` marker shape as
     * [isOpenPractice], for the same reason (a fourth distinguishable state, all sharing the
     * null-[lessonId] convention). See [StreakRecovery]. */
    private val isStreakRecovery: Boolean = savedStateHandle["isStreakRecovery"] ?: false

    /** True when reached via Route.DailyReview - the spaced-repetition session of due words. A
     * Review session in every respect except where its exercises come from. */
    private val isDailyReview: Boolean = savedStateHandle["isDailyReview"] ?: false

    /** True when reached via Route.Review rather than a real lesson - [init] then assembles its
     * exercise list from missed items instead of a fixed lesson, and [finishLesson] records the
     * result via `completeReviewSession` instead of `completeLesson`. False for Open Practice and
     * Streak Recovery too (see [isOpenPractice]/[isStreakRecovery]) even though Open Practice
     * shares `completeReviewSession`'s semantics - all three differ in exercise-sourcing ([init])
     * and in what [finishLesson] reports back as the result's `sessionType`. */
    private val isReviewSession: Boolean = lessonId == null && !isOpenPractice && !isStreakRecovery

    private var isFinishing = false

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
            // Listening exercises need both a bundled clip (only authored for words with verified
            // audio) and pronunciation audio switched on; without it they have no valid interaction.
            val listeningEnabled = preferences.pronunciationAudioEnabledFlow.first()
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
                    isDailyReview -> progressRepository.getDailyReviewExercises(
                        userId = userId,
                        limit = preferences.dailyGoalLevelFlow.first().dailyReviewCap,
                        listeningEnabled = listeningEnabled
                    )
                    isReviewSession -> progressRepository.getReviewExercises(missedItemIdsDeferred.await().toList())
                    else -> contentRepository.getExercisesForLesson(checkNotNull(lessonId))
                }
            }
            val categoriesDeferred = async { contentRepository.getWordCategories() }
            val strengthsDeferred = async { progressRepository.getWordStrengths(userId) }
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
                    // ListenAndType has no authored content yet; TapWhatYouHear only with audio on.
                    .filter { content ->
                        content !is ExerciseContent.ListenAndType &&
                            (listeningEnabled || content !is ExerciseContent.TapWhatYouHear)
                    }
                // Applied on decoded, pre-distractor content (see LessonContentRepeater's doc
                // comment) so each repeated instance gets its own independent distractor/shuffle
                // pass below, rather than N identical copies of the same regenerated exercise.
                // Open Practice and Streak Recovery always use a 1x multiplier (no-op) regardless
                // of the learner's stored LearningStyle - each batch is already freshly
                // randomized, so repeating a word within one batch wouldn't add the same "extra
                // reinforcement" it does for a fixed lesson's authored word set.
                val repeatCount = if (isOpenPractice || isStreakRecovery || isDailyReview) 1 else learningStyle.repeatCount
                val sequenced = AdaptiveSequencer.reorderForAdaptivePractice(decoded, missedItemIds)
                LessonContentRepeater.apply(sequenced, repeatCount)
                    .map { content -> resolveCanonicalMeaning(content, candidatePool, wordIntros) }
                    .map { content -> regenerateDistractors(content, candidatePool, missedItemIds, wordIntros) }
                    // An exercise that still can't offer a real choice (no resolvable correct
                    // option, or no distractors at all) would render an empty, unanswerable card -
                    // drop it rather than strand the learner.
                    .filter { content -> isPlayable(content) }
            }
            val wordCategories = categoriesDeferred.await()
            val wordStrengths = strengthsDeferred.await()
            _uiState.update {
                it.copy(isLoading = false, contents = contents, wordCategories = wordCategories, wordStrengths = wordStrengths)
            }
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
            is ExerciseContent.TapWordInVerse -> {
                val intro = wordIntros[content.wordId]
                val meaning = candidatePool.get(content.wordId)?.meaning ?: content.meaning
                if (intro != null) {
                    content.copy(
                        meaning = meaning,
                        verseTranslation = intro.exampleVerseTranslation,
                        meaningHighlight = intro.meaningHighlight
                    )
                } else {
                    content.copy(meaning = meaning)
                }
            }
            is ExerciseContent.FillInTheBlank -> {
                val intro = wordIntros[content.wordId]
                if (intro != null) {
                    content.copy(
                        sentenceArabic = intro.exampleVerseArabic ?: content.sentenceArabic,
                        blankStart = intro.arabicWordStart ?: content.blankStart,
                        blankEnd = intro.arabicWordEnd ?: content.blankEnd,
                        sentenceTranslation = if (intro.exampleVerseTranslation.isNotEmpty()) intro.exampleVerseTranslation else content.sentenceTranslation,
                        sentenceReference = intro.exampleVerseReference ?: content.sentenceReference
                    )
                } else content
            }
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
        missedItemIds: Set<String>,
        wordIntros: Map<String, ExerciseContent.WordIntro>
    ): ExerciseContent = when (content) {
        is OptionsBearing -> {
            // Last-resort source for the correct option when neither the candidate pool nor the
            // baked options know this word (e.g. generated practice exercises, which bake none).
            val introCorrect = wordIntros[content.wordId]?.let {
                ChoiceOption(id = content.correctOptionId, labelArabic = it.arabicWord.cleanArabicDisplay(), label = it.meaning)
            }
            content.withOptions(
                rebuildOptions(content.options, content.wordId, content.correctOptionId, candidatePool, missedItemIds, introCorrect)
            )
        }
        is ExerciseContent.Matching -> {
            val shuffledPairs = content.pairs.shuffled()
            val withShuffled = content.copy(pairs = shuffledPairs)
            withShuffled.copy(distractorRight = pickMatchingDistractor(withShuffled, candidatePool, missedItemIds))
        }
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
        return ChoiceOption(id = word.id, labelArabic = word.arabicWord.cleanArabicDisplay(), label = word.meaning)
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
        missedItemIds: Set<String>,
        fallbackCorrect: ChoiceOption?
    ): List<ChoiceOption> {
        val bakedCorrectOption = baked.find { it.id == correctOptionId }
        val correctWord = candidatePool.get(wordId)
        val correctOption = correctWord
            ?.let { ChoiceOption(id = correctOptionId, labelArabic = it.arabicWord.cleanArabicDisplay(), label = it.meaning) }
            ?: bakedCorrectOption
            ?: fallbackCorrect
            ?: return baked

        val generated = DistractorGenerator.pickDistractors(wordId, candidatePool, missedItemIds, count = 3)
            .mapNotNull { id -> candidatePool.get(id) }
            .map { ChoiceOption(id = it.id, labelArabic = it.arabicWord.cleanArabicDisplay(), label = it.meaning) }

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
                val opt = ChoiceOption(id = cand.id, labelArabic = cand.arabicWord.cleanArabicDisplay(), label = cand.meaning)
                if (selectedOptions.none { optionsCollide(it, opt) }) {
                    selectedOptions.add(opt)
                }
            }
        }

        return selectedOptions.shuffled()
    }

    private fun isPlayable(content: ExerciseContent): Boolean = when (content) {
        is OptionsBearing -> content.options.size >= 2 && content.options.any { it.id == content.correctOptionId }
        is ExerciseContent.Matching -> content.pairs.isNotEmpty()
        else -> true
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
            is ExerciseContent.TapWhatYouHear -> state.attempt.selectedOptionId == content.correctOptionId
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
        if (state.isChecked) return
        val content = state.currentContent as? ExerciseContent.Matching ?: return
        val pendingLeft = state.attempt.pendingLeftId ?: return
        if (pairId in state.attempt.matchedPairIds) return
        // Ids as the tiles report them (MatchPair.effectiveId), so pairs authored without an
        // explicit id still resolve.
        val leftPair = content.pairs.find { it.effectiveId == pendingLeft }

        if (pendingLeft == pairId) {
            val newMatched = state.attempt.matchedPairIds + pairId
            _uiState.update { it.copy(attempt = it.attempt.copy(matchedPairIds = newMatched, pendingLeftId = null)) }
            // Matching quizzes several pairs in one exercise, so each pair's word is logged
            // individually here rather than through finalizeCheck. A pair that was mismatched
            // first already logged its wrong first try below, so this correct match is then a
            // retry (isFirstTry = false) and can't clear that mistake. pair.id ("p1", "p2"...) is
            // lesson-scoped, not a global word id (see MatchPair's doc comment) - logging under it
            // would collide across lessons, so this only logs when the pair carries its wordId.
            leftPair?.wordId?.let { wordId -> logAttempt(wordId, ExerciseType.MATCHING, correct = true) }
            if (newMatched.size == content.pairs.size) {
                // Solved - but only "correct" if no pair ever took a wrong tap this attempt.
                finalizeCheck(state.attempt.mismatchedPairIds.isEmpty())
            } else {
                // Immediate per-pair feedback, not just at full-exercise completion - the last
                // pair's ding is already covered by finalizeCheck above.
                viewModelScope.launch { sfxPlayer.play(SfxEffect.CORRECT) }
            }
        } else {
            val mismatchToken = (state.attempt.lastMismatch?.token ?: 0) + 1
            val firstMismatchForPair = pendingLeft !in state.attempt.mismatchedPairIds
            _uiState.update {
                it.copy(
                    attempt = it.attempt.copy(
                        pendingLeftId = null,
                        lastMismatch = MismatchEvent(mismatchToken, pendingLeft, pairId),
                        mismatchedPairIds = it.attempt.mismatchedPairIds + pendingLeft
                    )
                )
            }
            // The selected left word is the one the learner didn't know - log its first mismatch
            // as a wrong attempt (repeat mismatches of the same pair add nothing new).
            if (firstMismatchForPair) {
                leftPair?.wordId?.let { wordId -> logAttempt(wordId, ExerciseType.MATCHING, correct = false) }
            }
            viewModelScope.launch { sfxPlayer.play(SfxEffect.WRONG) }
        }
    }

    /** Gives up on the current Matching exercise so a learner who can't finish it is never stuck
     * - graded as wrong, and every still-unmatched pair's word logged as a miss. */
    fun onSkipPressed() {
        val state = _uiState.value
        if (state.isChecked) return
        val content = state.currentContent as? ExerciseContent.Matching ?: return
        content.pairs
            .filter { it.effectiveId !in state.attempt.matchedPairIds && it.effectiveId !in state.attempt.mismatchedPairIds }
            .forEach { pair -> pair.wordId?.let { logAttempt(it, ExerciseType.MATCHING, correct = false) } }
        finalizeCheck(false)
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
        val state = _uiState.value
        val content = state.currentContent
        val currentIndex = state.currentIndex
        _uiState.update {
            it.copy(
                isChecked = true,
                lastAnswerCorrect = correct,
                // No-op for an index already graded once - see LessonScoring.
                scoring = it.scoring.grade(currentIndex, correct)
            )
        }
        viewModelScope.launch { sfxPlayer.play(if (correct) SfxEffect.CORRECT else SfxEffect.WRONG) }
        val exerciseType = when (content) {
            is ExerciseContent.MultipleChoice -> ExerciseType.MULTIPLE_CHOICE
            is ExerciseContent.TapWhatYouHear -> ExerciseType.TAP_WHAT_YOU_HEAR
            is ExerciseContent.FillInTheBlank -> ExerciseType.FILL_IN_THE_BLANK
            is ExerciseContent.WordOrderBuilder -> ExerciseType.WORD_ORDER
            is ExerciseContent.TapWordInVerse -> ExerciseType.WORD_IN_VERSE_TAP
            else -> return // Matching logs per-pair in selectMatchingRight; teach steps never reach here
        }
        val itemId = content.practicedItemId() ?: return
        logAttempt(itemId, exerciseType, correct)
    }

    /** Flags the row first-try only the first time this session logs [itemId] (see
     * [LessonScoring.log]) - retries and in-session repeats are kept for history but can't move
     * the missed/mastered sets. */
    private fun logAttempt(itemId: String, exerciseType: ExerciseType, correct: Boolean) {
        var log: AttemptLog? = null
        _uiState.update {
            val (scoring, entry) = it.scoring.log(itemId, exerciseType, correct)
            log = entry
            it.copy(scoring = scoring)
        }
        val entry = log ?: return
        viewModelScope.launch {
            progressRepository.logAttempt(
                userIdProvider.get(), entry.itemId, itemKind, entry.exerciseType, entry.correct, entry.isFirstTry
            )
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

    fun onPreviousPressed() {
        val state = _uiState.value
        val prevIndex = state.currentIndex - 1
        if (prevIndex >= 0) {
            _uiState.update {
                it.copy(
                    currentIndex = prevIndex,
                    attempt = ExerciseAttemptState(),
                    isChecked = false,
                    lastAnswerCorrect = null
                )
            }
        }
    }

    /** Plays a bundled pronunciation clip; false (no throw) when it isn't bundled or pronunciation
     * audio is off - the caller shows its "unavailable" note then. */
    fun playPronunciation(assetPath: String): Boolean = audioPlayer.play(assetPath)

    override fun onCleared() {
        audioPlayer.release()
        super.onCleared()
    }

    /** Called once the first exercise is actually on screen - starts the active-time clock. */
    fun onContentShown() = sessionTimer.start()

    /** App backgrounded (screen stopped) - stop counting practice time. */
    fun onPause() = sessionTimer.pause()

    fun onResume() = sessionTimer.resume()

    fun onContinuePressed() {
        if (isFinishing) return
        val now = clock.millis()
        if (lastAdvanceMillis != Long.MIN_VALUE && now - lastAdvanceMillis in 0 until CONTINUE_DEBOUNCE_MILLIS) return
        val state = _uiState.value
        val content = state.currentContent ?: return
        // A scored exercise can only be left once it's been checked - a stray second tap landing
        // on the next (unchecked) exercise must not skip it.
        if (content.isScored && !state.isChecked) return
        lastAdvanceMillis = now
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

    private val sessionType: LessonSessionType
        get() = when {
            isStreakRecovery -> LessonSessionType.STREAK_RECOVERY
            isOpenPractice -> LessonSessionType.OPEN_PRACTICE
            isReviewSession -> LessonSessionType.REVIEW
            else -> LessonSessionType.LESSON
        }

    private fun finishLesson() {
        if (isFinishing) return
        isFinishing = true
        sessionTimer.pause()
        val durationMillis = sessionTimer.elapsedMillis()
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val userId = userIdProvider.get()
                val totalCount = state.contents.count { it.isScored }
                val result = if (isStreakRecovery) {
                    val passed = progressRepository.attemptStreakRecovery(userId, state.correctCount, totalCount)
                    val stats = progressRepository.observeStats(userId).first()
                    LessonResult(
                        lessonId = REVIEW_SESSION_LESSON_ID,
                        correctCount = state.correctCount,
                        totalCount = totalCount,
                        pointsAwarded = 0,
                        newTotalPoints = stats?.totalPoints ?: 0,
                        currentStreak = stats?.currentStreak ?: 0,
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
                        sessionType = sessionType
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
                val newlyUnlocked = try {
                    achievementRepository.checkAndUnlock(userId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.w("LessonViewModel", "Achievement check failed", e)
                    emptyList()
                }
                _uiState.update { it.copy(isFinished = true, result = result, newlyUnlockedAchievements = newlyUnlocked) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LessonViewModel", "Error finishing lesson", e)
                // Persisting the result failed, so nothing was actually awarded - report zero
                // points, but show the learner's real (unchanged) totals/streak where readable.
                val state = _uiState.value
                val stats = try {
                    progressRepository.observeStats(userIdProvider.get()).first()
                } catch (c: CancellationException) {
                    throw c
                } catch (statsError: Exception) {
                    android.util.Log.w("LessonViewModel", "Stats unavailable for fallback result", statsError)
                    null
                }
                val fallbackResult = LessonResult(
                    lessonId = lessonId ?: REVIEW_SESSION_LESSON_ID,
                    correctCount = state.correctCount,
                    totalCount = state.contents.count { it.isScored },
                    pointsAwarded = 0,
                    newTotalPoints = stats?.totalPoints ?: 0,
                    currentStreak = stats?.currentStreak ?: 0,
                    streakIncreased = false,
                    durationMillis = durationMillis,
                    sessionType = sessionType
                )
                _uiState.update { it.copy(isFinished = true, result = fallbackResult, newlyUnlockedAchievements = emptyList()) }
            }
        }
    }

    private companion object {
        const val CONTINUE_DEBOUNCE_MILLIS = 400L
    }
}
