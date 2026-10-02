package com.quranicwords.app.feature.testonlyhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.DailyGoalCalculator
import com.quranicwords.app.core.domain.InactivityDuration
import com.quranicwords.app.core.domain.StreakRecovery
import com.quranicwords.app.core.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import com.quranicwords.app.core.domain.DisplayedStreak
import com.quranicwords.app.core.domain.model.LemmaCategory
import com.quranicwords.app.core.util.currentDateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.domain.repository.AchievementRepository
import com.quranicwords.app.core.domain.repository.ContentRepository

data class ChapterTestItem(
    val chapter: ChapterEntity,
    val coveredCount: Int,
    val totalCount: Int
)

data class TestOnlyHomeUiState(
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val isDailyGoalMetToday: Boolean = false,
    /** See [StreakRecovery.isLocked]. */
    val isStreakLocked: Boolean = false,
    val streakRecoveryQuestionCount: Int = 0,
    val streakInactivityDuration: InactivityDuration? = null,
    // Totals are counted from the seeded content (see wordCategoryCounts), never hardcoded -
    // the curriculum's word set changes between content builds.
    val ismCoveredCount: Int = 0,
    val totalIsmCount: Int = 0,
    val filCoveredCount: Int = 0,
    val totalFilCount: Int = 0,
    val harfCoveredCount: Int = 0,
    val totalHarfCount: Int = 0,
    val randomCoveredCount: Int = 0,
    val totalWordsCount: Int = 0,
    val missedWordsCount: Int = 0,
    /** See [com.quranicwords.app.feature.home.HomeUiState.dueReviewCount]. */
    val dueReviewCount: Int = 0,
    val quests: List<com.quranicwords.app.core.data.local.entity.DailyQuestEntity> = emptyList(),
    val quranCoveragePercent: Double = 0.0,
    val last30DaysMinutes: List<Int> = emptyList(),
    val last30DaysActiveCount: Int = 0,
    val last30DaysTotalMinutes: Int = 0,
    val chapters: List<ChapterTestItem> = emptyList()
)

/**
 * Backs [TestOnlyHomeScreen] - manages test status badges and real-time progress for all
 * Test/Quiz-only modes:
 * 1. Ism (Nouns) Mode
 * 2. Fi'l (Verbs) Mode
 * 3. Ḥarf (Particles) Mode
 * 4. Mix / Random Mode (the whole corpus)
 * 5. Mistaken Words Review (adaptive retry of missed vocabulary)
 * 6. Chapterwise Test Mode
 * Every count comes from the seeded content - see [WordPools].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TestOnlyHomeViewModel @Inject constructor(
    progressRepository: ProgressRepository,
    contentRepository: ContentRepository,
    achievementRepository: AchievementRepository,
    preferences: UserPreferencesDataStore,
    userIdProvider: CurrentUserIdProvider,
    clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(TestOnlyHomeUiState())
    val uiState: StateFlow<TestOnlyHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = userIdProvider.get()
            val pools = WordPools.from(
                allWordIds = contentRepository.getWordCandidates().map { it.id },
                categories = contentRepository.getWordCategories()
            )

            // Re-subscribed at local midnight - see currentDateFlow.
            currentDateFlow(clock).flatMapLatest { todayDate ->
                val today = todayDate.toString()
                val startDate = todayDate.minusDays(29).toString()

                val progressFlow = progressRepository.observeProgress(userId)
                val statsFlow = progressRepository.observeStats(userId)
                val todayPracticeFlow = progressRepository.observeTodayPractice(userId, today)
                val dailyGoalFlow = preferences.dailyGoalLevelFlow
                val ismCoveredFlow = preferences.testIsmCoveredWordIdsFlow
                val filCoveredFlow = preferences.testFilCoveredWordIdsFlow
                val harfCoveredFlow = preferences.testHarfCoveredWordIdsFlow
                val randomCoveredFlow = preferences.testRandomCoveredWordIdsFlow
                val missedIdsFlow = progressRepository.observeMissedItemIds(userId)
                val practiceRangeFlow = progressRepository.observePracticeHistoryForRange(userId, startDate, today)
                val chaptersFlow = contentRepository.observeChapters()

                combine(
                    combine(statsFlow, todayPracticeFlow, dailyGoalFlow, progressFlow) { stats, practice, goal, _ ->
                        Triple(stats, practice, goal)
                    },
                    combine(ismCoveredFlow, filCoveredFlow, harfCoveredFlow, randomCoveredFlow) { ism, fil, harf, random ->
                        TestPosCoveredState(ism, fil, harf, random)
                    },
                    combine(missedIdsFlow, practiceRangeFlow, chaptersFlow) { missed, range, chapters ->
                        Triple(missed, range, chapters)
                    },
                    combine(progressRepository.observeDueCount(userId), progressRepository.observeQuests(userId, today)) { due, quests -> due to quests }
                ) { (stats, todayPractice, goalLevel), posCovered, (missedIds, rangeHistory, chapters), (dueCount, quests) ->
                    val coveragePercent = achievementRepository.getCumulativeCoveragePercent(userId)
                    val practiceMap = rangeHistory.associate { it.localDate to it.minutesPracticed }
                    val last30DaysMinutes = (29 downTo 0).map { offset ->
                        val d = todayDate.minusDays(offset.toLong()).toString()
                        practiceMap[d] ?: 0
                    }
                    val total30DaysMins = last30DaysMinutes.sum()
                    val active30DaysDays = last30DaysMinutes.count { it > 0 }

                    val chapterTestItems = chapters.map { ch ->
                        val coveredIds = preferences.testChapterCoveredWordIdsFlow(ch.id).first()
                        ChapterTestItem(
                            chapter = ch,
                            coveredCount = coveredIds.size.coerceAtMost(ch.wordCount),
                            totalCount = ch.wordCount
                        )
                    }

                    TestOnlyHomeUiState(
                        totalPoints = stats?.totalPoints ?: 0,
                        currentStreak = DisplayedStreak.of(stats, todayDate),
                        isDailyGoalMetToday = DailyGoalCalculator.isGoalMetToday(
                            todayPractice?.minutesPracticed ?: 0,
                            goalLevel.minutes
                        ),
                        isStreakLocked = StreakRecovery.canRecover(stats, todayDate),
                        streakRecoveryQuestionCount = StreakRecovery.recoveryQuestionCount(stats?.currentStreak ?: 0) ?: 0,
                        streakInactivityDuration = StreakRecovery.inactivityDuration(stats, todayDate),
                        ismCoveredCount = pools.coveredCount(posCovered.ismCovered, pools.nouns),
                        totalIsmCount = pools.nouns.size,
                        filCoveredCount = pools.coveredCount(posCovered.filCovered, pools.verbs),
                        totalFilCount = pools.verbs.size,
                        harfCoveredCount = pools.coveredCount(posCovered.harfCovered, pools.particles),
                        totalHarfCount = pools.particles.size,
                        randomCoveredCount = pools.coveredCount(posCovered.randomCovered, pools.all),
                        totalWordsCount = pools.all.size,
                        missedWordsCount = missedIds.size,
                        dueReviewCount = dueCount,
                        quests = quests,
                        quranCoveragePercent = coveragePercent,
                        last30DaysMinutes = last30DaysMinutes,
                        last30DaysActiveCount = active30DaysDays,
                        last30DaysTotalMinutes = total30DaysMins,
                        chapters = chapterTestItems
                    )
                }
            }.collect { _uiState.value = it }
        }
    }
}

/** The Open Practice word pools, built from the content's own word list and per-word categories
 * (the same sources `ProgressRepositoryImpl.getOpenPracticeExercises` samples from), so the
 * totals shown here always match what a mode can actually serve. */
data class WordPools(
    val all: Set<String>,
    val nouns: Set<String>,
    val verbs: Set<String>,
    val particles: Set<String>
) {
    /** Covered ids still in [pool] - ids from an older content build no longer count. */
    fun coveredCount(covered: Set<String>, pool: Set<String>): Int = covered.count { it in pool }

    companion object {
        fun from(allWordIds: List<String>, categories: Map<String, LemmaCategory>): WordPools {
            val all = allWordIds.toSet()
            fun of(category: LemmaCategory) = all.filterTo(mutableSetOf()) { categories[it] == category }
            return WordPools(all, of(LemmaCategory.NOUN), of(LemmaCategory.VERB), of(LemmaCategory.PARTICLE))
        }
    }
}

private data class TestPosCoveredState(
    val ismCovered: Set<String>,
    val filCovered: Set<String>,
    val harfCovered: Set<String>,
    val randomCovered: Set<String>
)

