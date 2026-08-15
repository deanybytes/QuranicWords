package com.quranicwords.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.R
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.QuranFontStyle
import com.quranicwords.app.core.domain.model.ThemeMode
import com.quranicwords.app.core.domain.repository.BackupRepository
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
    private val backupRepository: BackupRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        preferences.themeModeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val language: StateFlow<Language?> =
        preferences.languageFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val fontStyle: StateFlow<QuranFontStyle> =
        preferences.fontStyleFlow.stateIn(viewModelScope, SharingStarted.Eagerly, QuranFontStyle.DEFAULT)

    val reduceMotion: StateFlow<Boolean> =
        preferences.reduceMotionFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _backupUiState = MutableStateFlow(BackupUiState())
    val backupUiState: StateFlow<BackupUiState> = _backupUiState.asStateFlow()

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { preferences.setReduceMotion(enabled) }
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
