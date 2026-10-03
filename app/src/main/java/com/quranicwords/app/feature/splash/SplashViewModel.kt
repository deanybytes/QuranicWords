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
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val preferences: UserPreferencesDataStore,
    private val sfxPlayer: SfxPlayer,
    private val clock: Clock
) : ViewModel() {

    private val _destination = MutableStateFlow<Route?>(null)
    val destination: StateFlow<Route?> = _destination.asStateFlow()

    /** True when first-launch seeding threw (e.g. low storage) - the screen offers a retry
     * instead of the app crashing at launch. */
    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed.asStateFlow()

    /** Whether this launch opens with the invocation: the first launch of each local day, or
     * every launch when the learner asked for that in Settings. Null until decided. */
    private val _playInvocation = MutableStateFlow<Boolean?>(null)
    val playInvocation: StateFlow<Boolean?> = _playInvocation.asStateFlow()

    init {
        viewModelScope.launch {
            val today = LocalDate.now(clock).toString()
            val play = preferences.invocationEveryLaunchFlow.first() || preferences.lastInvocationDate() != today
            if (play) {
                preferences.setLastInvocationDate(today)
                sfxPlayer.play(SfxEffect.OPENING)
            }
            _playInvocation.value = play
        }
        resolveDestination()
    }

    fun retry() {
        if (!_loadFailed.value) return
        _loadFailed.value = false
        resolveDestination()
    }

    private fun resolveDestination() {
        viewModelScope.launch {
            try {
                contentRepository.ensureSeeded()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SplashViewModel", "Content seeding failed", e)
                _loadFailed.value = true
                return@launch
            }

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
