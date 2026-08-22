package com.quranicwords.app.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.model.LearningPath
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
            val learningPathChoiceMade = preferences.learningPathChoiceMadeFlow.first()
            val fontChoiceMade = preferences.fontChoiceMadeFlow.first()
            val learningPath = preferences.learningPathFlow.first()
            val learningStyleChoiceMade = preferences.learningStyleChoiceMadeFlow.first()
            val dailyGoalChoiceMade = preferences.dailyGoalChoiceMadeFlow.first()

            // Test/Quiz-only never visits LearningStyleSelect (see QwNavHost's FontSelect
            // onContinue branch), so resuming mid-onboarding must skip that check for it too -
            // otherwise a Test-only user who quit right after FontSelect would get stuck being
            // resumed to a step they can never actually reach.
            val needsLearningStyle = learningPath == LearningPath.LEARN && !learningStyleChoiceMade

            _destination.value = when {
                language == null -> Route.LanguageSelect
                !learningPathChoiceMade -> Route.PathSelect
                !fontChoiceMade -> Route.FontSelect
                needsLearningStyle -> Route.LearningStyleSelect
                !dailyGoalChoiceMade -> Route.DailyGoalSelect
                else -> Route.Home
            }
        }
    }
}
