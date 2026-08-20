package com.quranicwords.app.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.quranicwords.app.R
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.sync.AudioBulkDownloadWorker
import com.quranicwords.app.core.domain.model.FontScale
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.QuranFontStyle
import com.quranicwords.app.core.domain.model.ThemeMode
import com.quranicwords.app.core.domain.repository.BackupRepository
import com.quranicwords.app.core.util.StreakReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

data class AudioDownloadUiState(
    val isWorking: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
    val isComplete: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val preferences: UserPreferencesDataStore,
    private val backupRepository: BackupRepository,
    private val streakReminderScheduler: StreakReminderScheduler
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    val themeMode: StateFlow<ThemeMode> =
        preferences.themeModeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val language: StateFlow<Language?> =
        preferences.languageFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val fontStyle: StateFlow<QuranFontStyle> =
        preferences.fontStyleFlow.stateIn(viewModelScope, SharingStarted.Eagerly, QuranFontStyle.DEFAULT)

    val reduceMotion: StateFlow<Boolean> =
        preferences.reduceMotionFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val soundEnabled: StateFlow<Boolean> =
        preferences.soundEnabledFlow.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val fontScale: StateFlow<FontScale> =
        preferences.fontScaleFlow.stateIn(viewModelScope, SharingStarted.Eagerly, FontScale.DEFAULT)

    val streakReminderEnabled: StateFlow<Boolean> =
        preferences.streakReminderEnabledFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val streakReminderHour: StateFlow<Int> =
        preferences.streakReminderHourFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 20)

    val streakReminderMinute: StateFlow<Int> =
        preferences.streakReminderMinuteFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _backupUiState = MutableStateFlow(BackupUiState())
    val backupUiState: StateFlow<BackupUiState> = _backupUiState.asStateFlow()

    private val _audioDownloadUiState = MutableStateFlow(AudioDownloadUiState())
    val audioDownloadUiState: StateFlow<AudioDownloadUiState> = _audioDownloadUiState.asStateFlow()

    init {
        // Observes the worker's own WorkInfo (survives this ViewModel being recreated while a
        // download is mid-flight, e.g. after a config change) rather than tracking progress in
        // purely in-memory state.
        workManager.getWorkInfosForUniqueWorkFlow(AudioBulkDownloadWorker.UNIQUE_WORK_NAME)
            .onEach { infos ->
                val info = infos.firstOrNull() ?: return@onEach
                val done = info.progress.getInt(AudioBulkDownloadWorker.KEY_DONE, 0)
                val total = info.progress.getInt(AudioBulkDownloadWorker.KEY_TOTAL, 0)
                _audioDownloadUiState.value = AudioDownloadUiState(
                    isWorking = info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED,
                    done = done,
                    total = total,
                    isComplete = info.state == WorkInfo.State.SUCCEEDED
                )
            }
            .launchIn(viewModelScope)
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { preferences.setReduceMotion(enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setSoundEnabled(enabled) }
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

    fun downloadAllAudio() {
        val request = OneTimeWorkRequestBuilder<AudioBulkDownloadWorker>().build()
        workManager.enqueueUniqueWork(
            AudioBulkDownloadWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
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
