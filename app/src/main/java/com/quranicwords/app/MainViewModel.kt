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
        // Malay, Hausa and Swahili were withdrawn: a learner who had one stored is read as English
        // (Language.fromTag) - persist that so the stored tag, backups and widgets agree, and the
        // English language flow re-applies the AppCompat per-app locale in MainActivity.
        viewModelScope.launch { preferences.migrateRetiredLanguage() }
    }
}
