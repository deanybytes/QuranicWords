package com.quranicwords.app.feature.testonlyhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.DailyGoalCalculator
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

data class TestOnlyHomeUiState(
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val isDailyGoalMetToday: Boolean = false,
    /** See [StreakRecovery.isLocked]. */
    val isStreakLocked: Boolean = false,
    val streakRecoveryQuestionCount: Int = 0
)

/**
 * Backs [TestOnlyHomeScreen] - deliberately minimal next to [com.quranicwords.app.feature.home
 * .HomeViewModel]: a Test/Quiz-only user has no chapter/section/lesson tree to load at all (no
 * `ensureCurriculumStarted` call either - there's nothing for it to bootstrap), just the same
 * status badges Learn-path Home already shows plus one entry point into Open Practice.
 */
@HiltViewModel
class TestOnlyHomeViewModel @Inject constructor(
    progressRepository: ProgressRepository,
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

            combine(
                progressRepository.observeStats(userId),
                progressRepository.observeTodayPractice(userId, today),
                preferences.dailyGoalLevelFlow
            ) { stats, todayPractice, goalLevel ->
                TestOnlyHomeUiState(
                    totalPoints = stats?.totalPoints ?: 0,
                    currentStreak = stats?.currentStreak ?: 0,
                    isDailyGoalMetToday = DailyGoalCalculator.isGoalMetToday(
                        todayPractice?.minutesPracticed ?: 0,
                        goalLevel.minutes
                    ),
                    isStreakLocked = StreakRecovery.isLocked(stats, todayDate),
                    streakRecoveryQuestionCount = StreakRecovery.recoveryQuestionCount(stats?.currentStreak ?: 0) ?: 0
                )
            }.collect { _uiState.value = it }
        }
    }
}
