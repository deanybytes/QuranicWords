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
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

import com.quranicwords.app.core.domain.repository.AchievementRepository

data class TestOnlyHomeUiState(
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val isDailyGoalMetToday: Boolean = false,
    /** See [StreakRecovery.isLocked]. */
    val isStreakLocked: Boolean = false,
    val streakRecoveryQuestionCount: Int = 0,
    val streakInactivityDuration: InactivityDuration? = null,
    val frequencyCoveredCount: Int = 0,
    val totalWordsCount: Int = 3680,
    val randomCoveredCount: Int = 0,
    val missedWordsCount: Int = 0,
    val quranCoveragePercent: Double = 0.0,
    val last30DaysMinutes: List<Int> = emptyList(),
    val last30DaysActiveCount: Int = 0,
    val last30DaysTotalMinutes: Int = 0
)

/**
 * Backs [TestOnlyHomeScreen] - manages test status badges and real-time progress for all 3
 * Test/Quiz-only modes:
 * 1. Frequency Order Mode (sequential Quranic frequency)
 * 2. Full Random Mode (non-repeating until corpus is exhausted)
 * 3. Mistaken Words Review (adaptive retry of missed vocabulary)
 */
@HiltViewModel
class TestOnlyHomeViewModel @Inject constructor(
    progressRepository: ProgressRepository,
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

            val coveragePercent = achievementRepository.getCumulativeCoveragePercent(userId)
            val statsFlow = progressRepository.observeStats(userId)
            val todayPracticeFlow = progressRepository.observeTodayPractice(userId, today)
            val dailyGoalFlow = preferences.dailyGoalLevelFlow
            val freqOffsetFlow = preferences.testFrequencyOffsetFlow
            val randomCoveredFlow = preferences.testRandomCoveredWordIdsFlow
            val missedIdsFlow = progressRepository.observeMissedItemIds(userId)
            val practiceRangeFlow = progressRepository.observePracticeHistoryForRange(userId, startDate, today)

            combine(
                combine(statsFlow, todayPracticeFlow, dailyGoalFlow) { stats, practice, goal ->
                    Triple(stats, practice, goal)
                },
                combine(freqOffsetFlow, randomCoveredFlow, missedIdsFlow, practiceRangeFlow) { freq, random, missed, range ->
                    TestModeTrackerState(freq, random, missed, range)
                }
            ) { (stats, todayPractice, goalLevel), tracker ->
                val practiceMap = tracker.rangeHistory.associate { it.localDate to it.minutesPracticed }
                val last30DaysMinutes = (29 downTo 0).map { offset ->
                    val d = todayDate.minusDays(offset.toLong()).toString()
                    practiceMap[d] ?: 0
                }
                val total30DaysMins = last30DaysMinutes.sum()
                val active30DaysDays = last30DaysMinutes.count { it > 0 }

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
                    frequencyCoveredCount = tracker.freqOffset.coerceAtMost(3680),
                    totalWordsCount = 3680,
                    randomCoveredCount = tracker.randomCovered.size.coerceAtMost(3680),
                    missedWordsCount = tracker.missedIds.size,
                    quranCoveragePercent = coveragePercent,
                    last30DaysMinutes = last30DaysMinutes,
                    last30DaysActiveCount = active30DaysDays,
                    last30DaysTotalMinutes = total30DaysMins
                )
            }.collect { _uiState.value = it }
        }
    }
}

private data class TestModeTrackerState(
    val freqOffset: Int,
    val randomCovered: Set<String>,
    val missedIds: List<String>,
    val rangeHistory: List<com.quranicwords.app.core.data.local.entity.DailyPracticeEntity>
)
