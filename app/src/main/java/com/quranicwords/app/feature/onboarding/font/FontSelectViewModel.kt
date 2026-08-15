package com.quranicwords.app.feature.onboarding.font

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.model.QuranFontStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FontSelectViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore
) : ViewModel() {
    fun selectFont(style: QuranFontStyle, onSaved: () -> Unit) {
        viewModelScope.launch {
            preferences.setFontStyle(style)
            onSaved()
        }
    }
}
