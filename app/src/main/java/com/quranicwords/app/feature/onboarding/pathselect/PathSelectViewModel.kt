package com.quranicwords.app.feature.onboarding.pathselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.model.LearningPath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PathSelectViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore
) : ViewModel() {
    fun selectPath(path: LearningPath, onSaved: () -> Unit) {
        viewModelScope.launch {
            preferences.setLearningPath(path)
            onSaved()
        }
    }
}
