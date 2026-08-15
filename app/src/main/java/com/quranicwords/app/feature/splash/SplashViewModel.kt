package com.quranicwords.app.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.navigation.Route
import com.quranicwords.app.core.util.SfxEffect
import com.quranicwords.app.core.util.SfxPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val preferences: UserPreferencesDataStore,
    private val sfxPlayer: SfxPlayer
) : ViewModel() {

    private val _destination = MutableStateFlow<Route?>(null)
    val destination: StateFlow<Route?> = _destination.asStateFlow()

    init {
        viewModelScope.launch { sfxPlayer.play(SfxEffect.OPENING) }
        viewModelScope.launch {
            contentRepository.ensureSeeded()

            val language = preferences.languageFlow.first()
            val fontChoiceMade = preferences.fontChoiceMadeFlow.first()

            _destination.value = when {
                language == null -> Route.LanguageSelect
                !fontChoiceMade -> Route.FontSelect
                else -> Route.Home
            }
        }
    }
}
