package com.quranicwords.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.model.FontScale
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = preferences.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val language: StateFlow<Language?> = preferences.languageFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val reduceMotion: StateFlow<Boolean> = preferences.reduceMotionFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val fontScale: StateFlow<FontScale> = preferences.fontScaleFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, FontScale.DEFAULT)

    init {
        // If the system-level per-app language (Android 13+ Settings > App languages, or a prior
        // session's AppCompatDelegate persistence) already differs from what's in DataStore,
        // treat it as authoritative rather than silently overwriting it back on the next
        // MainActivity LaunchedEffect(language) pass.
        val systemLanguage = AppCompatDelegate.getApplicationLocales().get(0)?.language
            ?.let { tag -> Language.entries.find { it.tag == tag } }
        if (systemLanguage != null) {
            viewModelScope.launch {
                if (preferences.languageFlow.first() != systemLanguage) {
                    preferences.setLanguage(systemLanguage)
                }
            }
        }
    }
}
