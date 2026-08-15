package com.quranicwords.app.feature.onboarding.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.model.Language
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageSelectViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore
) : ViewModel() {
    fun selectLanguage(language: Language, onSaved: () -> Unit) {
        viewModelScope.launch {
            preferences.setLanguage(language)
            onSaved()
        }
    }
}
