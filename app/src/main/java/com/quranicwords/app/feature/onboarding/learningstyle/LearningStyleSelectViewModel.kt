package com.quranicwords.app.feature.onboarding.learningstyle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.model.LearningStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LearningStyleSelectViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore
) : ViewModel() {
    fun selectStyle(style: LearningStyle, onSaved: () -> Unit) {
        viewModelScope.launch {
            preferences.setLearningStyle(style)
            onSaved()
        }
    }
}
