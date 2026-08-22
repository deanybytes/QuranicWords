package com.quranicwords.app.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.local.entity.DailyPracticeEntity
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.domain.DailyGoalCalculator
import com.quranicwords.app.core.domain.repository.AchievementRepository
import com.quranicwords.app.core.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class ProgressUiState(
    val isLoading: Boolean = true,
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val wordsLearnedCount: Int = 0,
    val quranCoveragePercent: Double = 0.0,
    /** Oldest-first, one entry per of the last 7 days. */
    val lessonsCompletedLast7Days: List<Int> = List(7) { 0 },
    val last7DayLabels: List<String> = List(7) { "" },
    /** Oldest-first, one entry per of the last 28 days - [HeatmapChart] lays these out 7-per-row. */
    val practiceDaysLast28: List<Boolean> = List(28) { false },
    val daysPracticedLast28Count: Int = 0,
    val goalMetDaysLast7: Int = 0
)

/** Pure, no DB access - one entry per day in [days], counting how many rows in [progress] have a
 * [UserProgressEntity.completedAtEpochMillis] that falls on that local date. Days with zero
 * completions still get an explicit `0` entry (not omitted), so [BarChart] can draw a full week
 * every time regardless of activity. */
fun lessonsCompletedPerDay(progress: List<UserProgressEntity>, days: List<LocalDate>, zone: ZoneId): List<Int> {
    val counts = HashMap<LocalDate, Int>()
    progress.forEach { row ->
        val completedAtMillis = row.completedAtEpochMillis ?: return@forEach
        val date = Instant.ofEpochMilli(completedAtMillis).atZone(zone).toLocalDate()
        counts[date] = (counts[date] ?: 0) + 1
    }
    return days.map { counts[it] ?: 0 }
}

/** Pure, no DB access - one entry per day in [days], true when that date has a
 * [DailyPracticeEntity] row with at least one minute practiced. */
fun practiceDaysGrid(dailyPractice: List<DailyPracticeEntity>, days: List<LocalDate>): List<Boolean> {
    val practicedDates = dailyPractice.filter { it.minutesPracticed > 0 }.map { it.localDate }.toSet()
    return days.map { it.toString() in practicedDates }
}

/** Pure, no DB access - how many of [days] met [goalMinutes] according to [dailyPractice]. */
fun countGoalMetDays(dailyPractice: List<DailyPracticeEntity>, days: List<LocalDate>, goalMinutes: Int): Int {
    val minutesByDate = dailyPractice.associate { it.localDate to it.minutesPracticed }
    return days.count { date -> DailyGoalCalculator.isGoalMetToday(minutesByDate[date.toString()] ?: 0, goalMinutes) }
}

/**
 * Backs the Progress tab: charts (Quran coverage donut, lessons-completed bar, practice-days
 * heatmap) plus metric tiles (streak, points, words learned, goal-met days) - all derived from
 * data that already exists (no new tracking beyond what Task 4.1's DailyPracticeEntity already
 * added). Chart *data derivation* is pure and unit-tested (see the top-level functions above);
 * the Canvas drawing itself is not meaningfully testable, same as this app's other custom motifs.
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val achievementRepository: AchievementRepository,
    private val preferences: UserPreferencesDataStore,
    private val userIdProvider: CurrentUserIdProvider,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = userIdProvider.get()
            val zone = clock.zone

            val stats = progressRepository.observeStats(userId).first()
            val progress = progressRepository.observeProgress(userId).first()
            val masteredCount = progressRepository.getMasteredItemIds(userId).size
            val dailyPractice = progressRepository.getDailyPracticeHistory(userId)
            val coveragePercent = achievementRepository.getCumulativeCoveragePercent(userId)
            val goalMinutes = preferences.dailyGoalLevelFlow.first().minutes

            val today = LocalDate.now(clock)
            val last7Days = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val last28Days = (27 downTo 0).map { today.minusDays(it.toLong()) }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    totalPoints = stats?.totalPoints ?: 0,
                    currentStreak = stats?.currentStreak ?: 0,
                    longestStreak = stats?.longestStreak ?: 0,
                    wordsLearnedCount = masteredCount,
                    quranCoveragePercent = coveragePercent,
                    lessonsCompletedLast7Days = lessonsCompletedPerDay(progress, last7Days, zone),
                    last7DayLabels = last7Days.map { date -> date.dayOfWeek.name.take(1) },
                    practiceDaysLast28 = practiceDaysGrid(dailyPractice, last28Days),
                    daysPracticedLast28Count = practiceDaysGrid(dailyPractice, last28Days).count { it },
                    goalMetDaysLast7 = countGoalMetDays(dailyPractice, last7Days, goalMinutes)
                )
            }
        }
    }
}
