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

    val reduceGlassEffects: StateFlow<Boolean> = preferences.reduceGlassEffectsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val fontScale: StateFlow<FontScale> = preferences.fontScaleFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, FontScale.DEFAULT)

    val fontStyle: StateFlow<com.quranicwords.app.core.domain.model.QuranFontStyle> = preferences.fontStyleFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.quranicwords.app.core.domain.model.QuranFontStyle.DEFAULT)

    init {
        // If the system-level per-app language (Android 13+ Settings > App languages, or a prior
        // session's AppCompatDelegate persistence) already differs from what's in DataStore,
        // treat it as authoritative rather than silently overwriting it back on the next
        // MainActivity LaunchedEffect(language) pass. Only applies once onboarding's LanguageSelect
        // step has actually run (DataStore language non-null) - MainActivity's LaunchedEffect
        // forces AppCompatDelegate to English as a display-only default before that step, and that
        // forced value persists across process restarts (AppCompatDelegate's own compat shim), so
        // without this guard a user who quits mid-onboarding, before ever picking a language, would
        // come back to find the picker silently skipped with English "chosen" on their behalf.
        val systemLanguage = AppCompatDelegate.getApplicationLocales().get(0)?.language
            ?.let { tag -> Language.entries.find { it.tag == tag } }
        if (systemLanguage != null) {
            viewModelScope.launch {
                val current = preferences.languageFlow.first()
                if (current != null && current != systemLanguage) {
                    preferences.setLanguage(systemLanguage)
                }
            }
        }
    }
}
