package com.quranicwords.app.feature.onboarding.dailygoal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.model.DailyGoalLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyGoalSelectViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore
) : ViewModel() {
    fun selectGoal(level: DailyGoalLevel, onSaved: () -> Unit) {
        viewModelScope.launch {
            preferences.setDailyGoalLevel(level)
            onSaved()
        }
    }
}
