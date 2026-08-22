package com.quranicwords.app.feature.onboarding.font

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.model.LearningPath
import com.quranicwords.app.core.domain.model.QuranFontStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FontSelectViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore
) : ViewModel() {
    /** [onSaved] receives the learner's already-chosen [LearningPath] so the caller can pick the
     * right next onboarding step - Test/Quiz-only skips [com.quranicwords.app.feature.onboarding
     * .learningstyle.LearningStyleSelectScreen] entirely (its repeat-count setting doesn't apply
     * to a mode with no teach step), everyone else goes there next. */
    fun selectFont(style: QuranFontStyle, onSaved: (LearningPath) -> Unit) {
        viewModelScope.launch {
            preferences.setFontStyle(style)
            onSaved(preferences.learningPathFlow.first())
        }
    }
}
