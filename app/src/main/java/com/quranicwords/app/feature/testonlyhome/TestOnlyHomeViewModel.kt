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
import java.time.LocalDate
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
    val ismCoveredCount: Int = 0,
    val totalIsmCount: Int = 3057,
    val filCoveredCount: Int = 0,
    val totalFilCount: Int = 1479,
    val harfCoveredCount: Int = 0,
    val totalHarfCount: Int = 173,
    val randomCoveredCount: Int = 0,
    val totalWordsCount: Int = 4709,
    val missedWordsCount: Int = 0,
    val quranCoveragePercent: Double = 0.0,
    val last30DaysMinutes: List<Int> = emptyList(),
    val last30DaysActiveCount: Int = 0,
    val last30DaysTotalMinutes: Int = 0,
    val chapters: List<ChapterTestItem> = emptyList()
)

/**
 * Backs [TestOnlyHomeScreen] - manages test status badges and real-time progress for all
 * Test/Quiz-only modes:
 * 1. Ism (Nouns) Mode (3,057 Quranic nouns)
 * 2. Fi'l (Verbs) Mode (1,479 Quranic verbs)
 * 3. Ḥarf (Particles) Mode (173 Quranic particles)
 * 4. Mix / Random Mode (4,709 corpus mix)
 * 5. Mistaken Words Review (adaptive retry of missed vocabulary)
 * 6. Chapterwise Test Mode (10 Quranic chapters)
 */
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
            val todayDate = LocalDate.now(clock)
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
                }
            ) { (stats, todayPractice, goalLevel), posCovered, (missedIds, rangeHistory, chapters) ->
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
                    currentStreak = stats?.currentStreak ?: 0,
                    isDailyGoalMetToday = DailyGoalCalculator.isGoalMetToday(
                        todayPractice?.minutesPracticed ?: 0,
                        goalLevel.minutes
                    ),
                    isStreakLocked = StreakRecovery.isLocked(stats, todayDate),
                    streakRecoveryQuestionCount = StreakRecovery.recoveryQuestionCount(stats?.currentStreak ?: 0) ?: 0,
                    streakInactivityDuration = StreakRecovery.inactivityDuration(stats, todayDate),
                    ismCoveredCount = posCovered.ismCovered.size.coerceAtMost(3057),
                    totalIsmCount = 3057,
                    filCoveredCount = posCovered.filCovered.size.coerceAtMost(1479),
                    totalFilCount = 1479,
                    harfCoveredCount = posCovered.harfCovered.size.coerceAtMost(173),
                    totalHarfCount = 173,
                    randomCoveredCount = posCovered.randomCovered.size.coerceAtMost(4709),
                    totalWordsCount = 4709,
                    missedWordsCount = missedIds.size,
                    quranCoveragePercent = coveragePercent,
                    last30DaysMinutes = last30DaysMinutes,
                    last30DaysActiveCount = active30DaysDays,
                    last30DaysTotalMinutes = total30DaysMins,
                    chapters = chapterTestItems
                )
            }.collect { _uiState.value = it }
        }
    }
}

private data class TestPosCoveredState(
    val ismCovered: Set<String>,
    val filCovered: Set<String>,
    val harfCovered: Set<String>,
    val randomCovered: Set<String>
)

