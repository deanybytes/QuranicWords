package com.quranicwords.app.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.quranicwords.app.core.domain.model.FontScale
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.QuranFontStyle
import com.quranicwords.app.core.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/**
 * Single source of truth for onboarding progress and user-chosen settings that must be available
 * before the app's data layer (Room) is otherwise ready - language, theme, and Quran font are all
 * read here directly rather than through a repository indirection, since this increment has no
 * additional logic to layer on top of plain persisted preferences.
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_STYLE = stringPreferencesKey("quran_font_style")
        val FONT_CHOICE_MADE = booleanPreferencesKey("font_choice_made")
        val CONTENT_SEEDED_VERSION = intPreferencesKey("content_seeded_version")
        val LOCAL_USER_ID = stringPreferencesKey("local_user_id")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val AUDIO_OFFLINE_MODE = booleanPreferencesKey("audio_offline_mode")
        val FONT_SCALE = stringPreferencesKey("font_scale")
    }

    /**
     * Stable local key for Room progress/stats - generated once per device and persisted (see
     * [CurrentUserIdProvider]). The app is local-device-only, so this is the sole identity a
     * learner's progress is keyed by.
     */
    suspend fun getOrCreateLocalUserId(): String {
        val existing = context.dataStore.data.first()[Keys.LOCAL_USER_ID]
        if (existing != null) return existing
        val generated = java.util.UUID.randomUUID().toString()
        context.dataStore.edit { it[Keys.LOCAL_USER_ID] = generated }
        return generated
    }

    /** Overwrites the local user id - used by [com.quranicwords.app.core.domain.repository.BackupRepository]'s
     * import flow to adopt a restored backup's original id, so Room rows keyed by that id (all
     * still tagged with it, untouched on import) line up with what [getOrCreateLocalUserId]
     * returns afterwards. */
    suspend fun setLocalUserId(id: String) {
        context.dataStore.edit { it[Keys.LOCAL_USER_ID] = id }
    }

    val languageFlow: Flow<Language?> =
        context.dataStore.data.map { Language.fromTag(it[Keys.LANGUAGE]) }

    suspend fun setLanguage(language: Language) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.tag }
    }

    val themeModeFlow: Flow<ThemeMode> =
        context.dataStore.data.map { ThemeMode.fromName(it[Keys.THEME_MODE]) }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    val fontStyleFlow: Flow<QuranFontStyle> =
        context.dataStore.data.map { QuranFontStyle.fromName(it[Keys.FONT_STYLE]) }

    suspend fun setFontStyle(style: QuranFontStyle) {
        context.dataStore.edit {
            it[Keys.FONT_STYLE] = style.name
            it[Keys.FONT_CHOICE_MADE] = true
        }
    }

    val fontChoiceMadeFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.FONT_CHOICE_MADE] == true }

    suspend fun isContentSeeded(version: Int): Boolean =
        context.dataStore.data.first()[Keys.CONTENT_SEEDED_VERSION] == version

    suspend fun setContentSeeded(version: Int) {
        context.dataStore.edit { it[Keys.CONTENT_SEEDED_VERSION] = version }
    }

    /** User-facing "Reduce motion" setting (Settings screen) - independent of and additive to the
     * system-level animator-duration-scale check in [com.quranicwords.app.core.ui.motion.rememberReducedMotion]. */
    val reduceMotionFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.REDUCE_MOTION] == true }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCE_MOTION] = enabled }
    }

    /** Master sound-effects toggle (Settings screen) - checked once inside
     * [com.quranicwords.app.core.util.SfxPlayer.play] rather than at every call site. Defaults
     * to on. */
    val soundEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.SOUND_ENABLED] != false }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    /** Whether every word's pronunciation clip has been bulk-downloaded to local storage (see
     * [com.quranicwords.app.core.data.repository.WordAudioRepository.downloadAll]) - when true,
     * playback resolves from the on-device cache without touching the network at all. Defaults
     * to off (stream-on-demand). */
    val audioOfflineModeFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.AUDIO_OFFLINE_MODE] == true }

    suspend fun setAudioOfflineMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUDIO_OFFLINE_MODE] = enabled }
    }

    val fontScaleFlow: Flow<FontScale> =
        context.dataStore.data.map { FontScale.fromName(it[Keys.FONT_SCALE]) }

    suspend fun setFontScale(scale: FontScale) {
        context.dataStore.edit { it[Keys.FONT_SCALE] = scale.name }
    }
}
