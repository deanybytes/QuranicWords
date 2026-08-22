package com.quranicwords.app.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.model.LearningPath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Just enough to let [QwBottomNavShell] decide which Home composable to render for its Home
 * tab - see [LearningPath]. */
@HiltViewModel
class QwBottomNavShellViewModel @Inject constructor(
    preferences: UserPreferencesDataStore
) : ViewModel() {
    val learningPath: StateFlow<LearningPath> = preferences.learningPathFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearningPath.DEFAULT)
}
