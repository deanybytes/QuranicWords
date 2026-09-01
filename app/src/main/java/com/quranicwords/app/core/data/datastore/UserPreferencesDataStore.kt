package com.quranicwords.app.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.quranicwords.app.core.domain.model.DailyGoalLevel
import com.quranicwords.app.core.domain.model.FontScale
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.LearningPath
import com.quranicwords.app.core.domain.model.LearningStyle
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
        val REDUCE_GLASS_EFFECTS = booleanPreferencesKey("reduce_glass_effects")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val PRONUNCIATION_AUDIO_ENABLED = booleanPreferencesKey("pronunciation_audio_enabled")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val STREAK_REMINDER_ENABLED = booleanPreferencesKey("streak_reminder_enabled")
        val STREAK_REMINDER_HOUR = intPreferencesKey("streak_reminder_hour")
        val STREAK_REMINDER_MINUTE = intPreferencesKey("streak_reminder_minute")
        val DAILY_GOAL_LEVEL = stringPreferencesKey("daily_goal_level")
        val DAILY_GOAL_CHOICE_MADE = booleanPreferencesKey("daily_goal_choice_made")
        val LEARNING_STYLE = stringPreferencesKey("learning_style")
        val LEARNING_STYLE_CHOICE_MADE = booleanPreferencesKey("learning_style_choice_made")
        val LEARNING_PATH = stringPreferencesKey("learning_path")
        val LEARNING_PATH_CHOICE_MADE = booleanPreferencesKey("learning_path_choice_made")
        val TEST_FREQUENCY_OFFSET = intPreferencesKey("test_frequency_offset")
        val TEST_RANDOM_COVERED_IDS = stringSetPreferencesKey("test_random_covered_ids")
        val TEST_ISM_COVERED_IDS = stringSetPreferencesKey("test_ism_covered_ids")
        val TEST_FIL_COVERED_IDS = stringSetPreferencesKey("test_fil_covered_ids")
        val TEST_HARF_COVERED_IDS = stringSetPreferencesKey("test_harf_covered_ids")
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

    /** User-facing "Reduce glossy effects" setting (Settings screen) - same shape as
     * [reduceMotionFlow], consumed by [com.quranicwords.app.core.ui.motion.rememberReducedGlass]
     * to gate [com.quranicwords.app.core.ui.components.GlassSurface]'s translucency/sheen and the
     * correct-answer sheen in `AnswerFeedbackOverlay`. Off by default. */
    val reduceGlassEffectsFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.REDUCE_GLASS_EFFECTS] == true }

    suspend fun setReduceGlassEffects(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCE_GLASS_EFFECTS] = enabled }
    }

    /** Master sound-effects toggle (Settings screen) - checked once inside
     * [com.quranicwords.app.core.util.SfxPlayer.play] rather than at every call site. Defaults
     * to on. */
    val soundEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.SOUND_ENABLED] != false }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    /** Word-pronunciation audio toggle (Settings screen, "Sound" section) - gates
     * [com.quranicwords.app.core.util.AudioPlayer.play] directly, independent of [soundEnabledFlow]
     * (which only gates the short SFX chimes via `SfxPlayer`). Defaults to on. */
    val pronunciationAudioEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.PRONUNCIATION_AUDIO_ENABLED] != false }

    suspend fun setPronunciationAudioEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PRONUNCIATION_AUDIO_ENABLED] = enabled }
    }

    val fontScaleFlow: Flow<FontScale> =
        context.dataStore.data.map { FontScale.fromName(it[Keys.FONT_SCALE]) }

    suspend fun setFontScale(scale: FontScale) {
        context.dataStore.edit { it[Keys.FONT_SCALE] = scale.name }
    }

    /** Opt-in "streak at risk" local reminder (Settings screen) - off by default, unlike
     * [soundEnabledFlow]. See [com.quranicwords.app.core.util.StreakReminderScheduler]. */
    val streakReminderEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.STREAK_REMINDER_ENABLED] == true }

    /** Hour/minute (24h, local time) the reminder fires at - defaults to 20:00. Stored separately
     * rather than as one packed value so each half can default independently if only one is ever
     * written (shouldn't happen via [setStreakReminderTime], but keeps the flow robust either way). */
    val streakReminderHourFlow: Flow<Int> =
        context.dataStore.data.map { it[Keys.STREAK_REMINDER_HOUR] ?: 20 }
    val streakReminderMinuteFlow: Flow<Int> =
        context.dataStore.data.map { it[Keys.STREAK_REMINDER_MINUTE] ?: 0 }

    suspend fun setStreakReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STREAK_REMINDER_ENABLED] = enabled }
    }

    suspend fun setStreakReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.STREAK_REMINDER_HOUR] = hour
            it[Keys.STREAK_REMINDER_MINUTE] = minute
        }
    }

    /** Daily practice-time goal (onboarding step, changeable later) - defaults to
     * [DailyGoalLevel.DEFAULT] before a choice is ever made, same shape as [fontStyleFlow]. */
    val dailyGoalLevelFlow: Flow<DailyGoalLevel> =
        context.dataStore.data.map { DailyGoalLevel.fromName(it[Keys.DAILY_GOAL_LEVEL]) }

    suspend fun setDailyGoalLevel(level: DailyGoalLevel) {
        context.dataStore.edit {
            it[Keys.DAILY_GOAL_LEVEL] = level.name
            it[Keys.DAILY_GOAL_CHOICE_MADE] = true
        }
    }

    val dailyGoalChoiceMadeFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.DAILY_GOAL_CHOICE_MADE] == true }

    /** How many times each word (and each Matching "exam" cycle) repeats within a lesson - see
     * [LearningStyle] and [com.quranicwords.app.core.domain.LessonContentRepeater]. Onboarding
     * step, changeable later in Settings, same shape as [dailyGoalLevelFlow]. */
    val learningStyleFlow: Flow<LearningStyle> =
        context.dataStore.data.map { LearningStyle.fromName(it[Keys.LEARNING_STYLE]) }

    suspend fun setLearningStyle(style: LearningStyle) {
        context.dataStore.edit {
            it[Keys.LEARNING_STYLE] = style.name
            it[Keys.LEARNING_STYLE_CHOICE_MADE] = true
        }
    }

    val learningStyleChoiceMadeFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.LEARNING_STYLE_CHOICE_MADE] == true }

    /** Learn vs. Test/Quiz-only onboarding branch (`Route.PathSelect`, right after language) - see
     * [LearningPath]. Changeable later in Settings, same shape as [dailyGoalLevelFlow]. */
    val learningPathFlow: Flow<LearningPath> =
        context.dataStore.data.map { LearningPath.fromName(it[Keys.LEARNING_PATH]) }

    suspend fun setLearningPath(path: LearningPath) {
        context.dataStore.edit {
            it[Keys.LEARNING_PATH] = path.name
            it[Keys.LEARNING_PATH_CHOICE_MADE] = true
        }
    }

    val learningPathChoiceMadeFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.LEARNING_PATH_CHOICE_MADE] == true }

    val testFrequencyOffsetFlow: Flow<Int> =
        context.dataStore.data.map { it[Keys.TEST_FREQUENCY_OFFSET] ?: 0 }

    suspend fun setTestFrequencyOffset(offset: Int) {
        context.dataStore.edit { it[Keys.TEST_FREQUENCY_OFFSET] = offset }
    }

    val testRandomCoveredWordIdsFlow: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.TEST_RANDOM_COVERED_IDS] ?: emptySet() }

    suspend fun addTestRandomCoveredWordIds(ids: Collection<String>) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.TEST_RANDOM_COVERED_IDS] ?: emptySet()
            prefs[Keys.TEST_RANDOM_COVERED_IDS] = current + ids
        }
    }

    suspend fun resetTestRandomCoveredWordIds() {
        context.dataStore.edit { prefs ->
            prefs[Keys.TEST_RANDOM_COVERED_IDS] = emptySet()
        }
    }

    val testIsmCoveredWordIdsFlow: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.TEST_ISM_COVERED_IDS] ?: emptySet() }

    suspend fun addTestIsmCoveredWordIds(ids: Collection<String>) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.TEST_ISM_COVERED_IDS] ?: emptySet()
            prefs[Keys.TEST_ISM_COVERED_IDS] = current + ids
        }
    }

    suspend fun resetTestIsmCoveredWordIds() {
        context.dataStore.edit { prefs ->
            prefs[Keys.TEST_ISM_COVERED_IDS] = emptySet()
        }
    }

    val testFilCoveredWordIdsFlow: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.TEST_FIL_COVERED_IDS] ?: emptySet() }

    suspend fun addTestFilCoveredWordIds(ids: Collection<String>) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.TEST_FIL_COVERED_IDS] ?: emptySet()
            prefs[Keys.TEST_FIL_COVERED_IDS] = current + ids
        }
    }

    suspend fun resetTestFilCoveredWordIds() {
        context.dataStore.edit { prefs ->
            prefs[Keys.TEST_FIL_COVERED_IDS] = emptySet()
        }
    }

    val testHarfCoveredWordIdsFlow: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.TEST_HARF_COVERED_IDS] ?: emptySet() }

    suspend fun addTestHarfCoveredWordIds(ids: Collection<String>) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.TEST_HARF_COVERED_IDS] ?: emptySet()
            prefs[Keys.TEST_HARF_COVERED_IDS] = current + ids
        }
    }

    suspend fun resetTestHarfCoveredWordIds() {
        context.dataStore.edit { prefs ->
            prefs[Keys.TEST_HARF_COVERED_IDS] = emptySet()
        }
    }

    fun testChapterCoveredWordIdsFlow(chapterId: String): Flow<Set<String>> =
        context.dataStore.data.map { prefs ->
            prefs[androidx.datastore.preferences.core.stringSetPreferencesKey("test_chapter_${chapterId}_covered_ids")] ?: emptySet()
        }

    suspend fun addTestChapterCoveredWordIds(chapterId: String, ids: Collection<String>) {
        context.dataStore.edit { prefs ->
            val key = androidx.datastore.preferences.core.stringSetPreferencesKey("test_chapter_${chapterId}_covered_ids")
            val current = prefs[key] ?: emptySet()
            prefs[key] = current + ids
        }
    }

    suspend fun resetTestChapterCoveredWordIds(chapterId: String) {
        context.dataStore.edit { prefs ->
            val key = androidx.datastore.preferences.core.stringSetPreferencesKey("test_chapter_${chapterId}_covered_ids")
            prefs[key] = emptySet()
        }
    }
}

