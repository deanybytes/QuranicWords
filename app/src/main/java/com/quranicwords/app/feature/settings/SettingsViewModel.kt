package com.quranicwords.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.R
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.model.DailyGoalLevel
import com.quranicwords.app.core.domain.model.FontScale
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.LearningPath
import com.quranicwords.app.core.domain.model.LearningStyle
import com.quranicwords.app.core.domain.model.QuranFontStyle
import com.quranicwords.app.core.domain.model.ThemeMode
import com.quranicwords.app.core.domain.repository.BackupRepository
import com.quranicwords.app.core.domain.repository.ProgressRepository
import com.quranicwords.app.core.util.StreakReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

data class BackupUiState(
    val isWorking: Boolean = false,
    val lastMessageResId: Int? = null,
    val lastMessageWasError: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore,
    private val backupRepository: BackupRepository,
    private val streakReminderScheduler: StreakReminderScheduler,
    private val progressRepository: ProgressRepository,
    private val userIdProvider: CurrentUserIdProvider
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        preferences.themeModeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val language: StateFlow<Language?> =
        preferences.languageFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val fontStyle: StateFlow<QuranFontStyle> =
        preferences.fontStyleFlow.stateIn(viewModelScope, SharingStarted.Eagerly, QuranFontStyle.DEFAULT)

    val reduceMotion: StateFlow<Boolean> =
        preferences.reduceMotionFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val reduceGlassEffects: StateFlow<Boolean> =
        preferences.reduceGlassEffectsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val soundEnabled: StateFlow<Boolean> =
        preferences.soundEnabledFlow.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val pronunciationAudioEnabled: StateFlow<Boolean> =
        preferences.pronunciationAudioEnabledFlow.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val fontScale: StateFlow<FontScale> =
        preferences.fontScaleFlow.stateIn(viewModelScope, SharingStarted.Eagerly, FontScale.DEFAULT)

    val streakReminderEnabled: StateFlow<Boolean> =
        preferences.streakReminderEnabledFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val streakReminderHour: StateFlow<Int> =
        preferences.streakReminderHourFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 20)

    val streakReminderMinute: StateFlow<Int> =
        preferences.streakReminderMinuteFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val learningPath: StateFlow<LearningPath> =
        preferences.learningPathFlow.stateIn(viewModelScope, SharingStarted.Eagerly, LearningPath.DEFAULT)

    val learningStyle: StateFlow<LearningStyle> =
        preferences.learningStyleFlow.stateIn(viewModelScope, SharingStarted.Eagerly, LearningStyle.DEFAULT)

    val dailyGoalLevel: StateFlow<DailyGoalLevel> =
        preferences.dailyGoalLevelFlow.stateIn(viewModelScope, SharingStarted.Eagerly, DailyGoalLevel.DEFAULT)

    private val _backupUiState = MutableStateFlow(BackupUiState())
    val backupUiState: StateFlow<BackupUiState> = _backupUiState.asStateFlow()

    private val _progressResetDone = MutableStateFlow(false)
    val progressResetDone: StateFlow<Boolean> = _progressResetDone.asStateFlow()

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { preferences.setReduceMotion(enabled) }
    }

    fun setReduceGlassEffects(enabled: Boolean) {
        viewModelScope.launch { preferences.setReduceGlassEffects(enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setSoundEnabled(enabled) }
    }

    fun setPronunciationAudioEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setPronunciationAudioEnabled(enabled) }
    }

    fun setFontScale(scale: FontScale) {
        viewModelScope.launch { preferences.setFontScale(scale) }
    }

    /** Called only after the caller (Settings screen) has confirmed POST_NOTIFICATIONS is granted
     * on API 33+ - permission UI is a Compose/Activity concern, kept out of this ViewModel. */
    fun setStreakReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setStreakReminderEnabled(enabled)
            if (enabled) {
                streakReminderScheduler.schedule(streakReminderHour.value, streakReminderMinute.value)
            } else {
                streakReminderScheduler.cancel()
            }
        }
    }

    fun setStreakReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferences.setStreakReminderTime(hour, minute)
            if (streakReminderEnabled.value) {
                streakReminderScheduler.schedule(hour, minute)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setLanguage(language: Language) {
        viewModelScope.launch { preferences.setLanguage(language) }
    }

    fun setFontStyle(style: QuranFontStyle) {
        viewModelScope.launch { preferences.setFontStyle(style) }
    }

    fun setLearningPath(path: LearningPath) {
        viewModelScope.launch { preferences.setLearningPath(path) }
    }

    fun setLearningStyle(style: LearningStyle) {
        viewModelScope.launch { preferences.setLearningStyle(style) }
    }

    fun setDailyGoalLevel(level: DailyGoalLevel) {
        viewModelScope.launch { preferences.setDailyGoalLevel(level) }
    }

    /** Wipes lesson/exam progress, points/streak, attempt history, daily-practice minutes, and
     * achievements for the local device user - leaves language/path/font/daily-goal-level
     * preferences untouched (see [ProgressRepository.resetProgress]'s own doc comment). */
    fun resetProgress() {
        viewModelScope.launch {
            progressRepository.resetProgress(userIdProvider.get())
            _progressResetDone.value = true
        }
    }

    fun dismissProgressResetDone() {
        _progressResetDone.value = false
    }

    /** [output] is opened by the caller (Settings screen) from a SAF-picked [android.net.Uri] via
     * `ContentResolver.openOutputStream` - kept an Android framework type out of this ViewModel
     * and [BackupRepository]'s interface. */
    fun exportBackup(output: OutputStream) {
        viewModelScope.launch {
            _backupUiState.update { it.copy(isWorking = true) }
            val result = withContext(Dispatchers.IO) { backupRepository.exportBackup(output) }
            _backupUiState.update {
                BackupUiState(
                    isWorking = false,
                    lastMessageResId = if (result.isSuccess) {
                        R.string.settings_backup_export_success
                    } else {
                        R.string.settings_backup_export_failed
                    },
                    lastMessageWasError = result.isFailure
                )
            }
        }
    }

    fun importBackup(input: InputStream) {
        viewModelScope.launch {
            _backupUiState.update { it.copy(isWorking = true) }
            val result = withContext(Dispatchers.IO) { backupRepository.importBackup(input) }
            _backupUiState.update {
                BackupUiState(
                    isWorking = false,
                    lastMessageResId = if (result.isSuccess) {
                        R.string.settings_backup_import_success
                    } else {
                        R.string.settings_backup_import_failed
                    },
                    lastMessageWasError = result.isFailure
                )
            }
        }
    }

    fun dismissBackupMessage() {
        _backupUiState.update { it.copy(lastMessageResId = null) }
    }
}
