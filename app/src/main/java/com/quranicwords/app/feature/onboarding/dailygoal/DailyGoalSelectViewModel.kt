package com.quranicwords.app.feature.onboarding.dailygoal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.model.DailyGoalLevel
import com.quranicwords.app.core.util.StreakReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyGoalSelectViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore,
    private val streakReminderScheduler: StreakReminderScheduler
) : ViewModel() {

    /** The streak-reminder opt-in offered alongside the goal - same setting as in Settings. */
    val reminderEnabled: StateFlow<Boolean> =
        preferences.streakReminderEnabledFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun selectGoal(level: DailyGoalLevel, onSaved: () -> Unit) {
        viewModelScope.launch {
            preferences.setDailyGoalLevel(level)
            onSaved()
        }
    }

    /** Called only once POST_NOTIFICATIONS is granted (API 33+) - the screen asks first, exactly
     * as Settings does. Schedules at the stored reminder time (20:00 unless changed). */
    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setStreakReminderEnabled(enabled)
            if (enabled) {
                streakReminderScheduler.schedule(
                    preferences.streakReminderHourFlow.first(),
                    preferences.streakReminderMinuteFlow.first()
                )
            } else {
                streakReminderScheduler.cancel()
            }
        }
    }
}
