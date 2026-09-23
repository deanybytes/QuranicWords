package com.quranicwords.app.core.data.repository

import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.domain.model.BACKUP_SCHEMA_VERSION
import com.quranicwords.app.core.domain.model.BackupPayload
import com.quranicwords.app.core.domain.model.BackupPreferences
import com.quranicwords.app.core.domain.model.DailyGoalLevel
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.LearningPath
import com.quranicwords.app.core.domain.model.LearningStyle
import com.quranicwords.app.core.domain.model.QuranFontStyle
import com.quranicwords.app.core.domain.model.ThemeMode
import com.quranicwords.app.core.domain.repository.BackupRepository
import com.quranicwords.app.core.util.AppJson
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val database: QwDatabase,
    private val preferences: UserPreferencesDataStore
) : BackupRepository {

    override suspend fun exportBackup(output: OutputStream): Result<Unit> = runCatching {
        val userId = preferences.getOrCreateLocalUserId()
        val payload = BackupPayload(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            exportedAtEpochMillis = System.currentTimeMillis(),
            userId = userId,
            stats = database.userStatsDao().get(userId),
            progress = database.userProgressDao().getAllForUserOnce(userId),
            attempts = database.exerciseAttemptDao().getAllForUser(userId),
            achievements = database.achievementDao().getAllForUserOnce(userId),
            dailyPractices = database.dailyPracticeDao().getAllForUserOnce(userId),
            preferences = BackupPreferences(
                languageTag = preferences.languageFlow.first()?.tag,
                themeMode = preferences.themeModeFlow.first().name,
                fontStyle = preferences.fontStyleFlow.first().name,
                reduceMotion = preferences.reduceMotionFlow.first(),
                learningPath = preferences.learningPathFlow.first().name,
                learningStyle = preferences.learningStyleFlow.first().name,
                dailyGoalLevel = preferences.dailyGoalLevelFlow.first().name,
                reduceGlassEffects = preferences.reduceGlassEffectsFlow.first(),
                soundEnabled = preferences.soundEnabledFlow.first()
            )
        )
        output.use { it.write(AppJson.encodeToString(payload).toByteArray()) }
    }

    override suspend fun importBackup(input: InputStream): Result<Unit> = runCatching {
        val json = input.use { it.readBytes().decodeToString() }
        val payload = AppJson.decodeFromString<BackupPayload>(json)

        val targetUserId = preferences.getOrCreateLocalUserId()

        // 1. Wipe existing user tables for targetUserId to prevent orphaned/duplicate rows
        database.userProgressDao().deleteForUser(targetUserId)
        database.userStatsDao().deleteForUser(targetUserId)
        database.exerciseAttemptDao().deleteForUser(targetUserId)
        database.achievementDao().deleteForUser(targetUserId)
        database.dailyPracticeDao().deleteForUser(targetUserId)

        // If the backup has a different userId, clean that up from local db as well
        if (payload.userId.isNotBlank() && payload.userId != targetUserId) {
            database.userProgressDao().deleteForUser(payload.userId)
            database.userStatsDao().deleteForUser(payload.userId)
            database.exerciseAttemptDao().deleteForUser(payload.userId)
            database.achievementDao().deleteForUser(payload.userId)
            database.dailyPracticeDao().deleteForUser(payload.userId)
        }

        // 2. Restore tables remapped to the active targetUserId
        payload.stats?.copy(userId = targetUserId)?.let { database.userStatsDao().upsert(it) }
        val remappedProgress = payload.progress.map { it.copy(userId = targetUserId) }
        database.userProgressDao().upsertAll(remappedProgress)

        val sanitizedAttempts = payload.attempts.map { it.copy(id = 0, userId = targetUserId) }
        database.exerciseAttemptDao().insertAll(sanitizedAttempts)

        val remappedAchievements = payload.achievements.map { it.copy(userId = targetUserId) }
        database.achievementDao().insertAll(remappedAchievements)

        val remappedDailyPractices = payload.dailyPractices.map { it.copy(userId = targetUserId) }
        database.dailyPracticeDao().insertAll(remappedDailyPractices)

        // 3. Restore all preferences
        Language.fromTag(payload.preferences.languageTag)?.let { preferences.setLanguage(it) }
        preferences.setThemeMode(ThemeMode.fromName(payload.preferences.themeMode))
        preferences.setFontStyle(QuranFontStyle.fromName(payload.preferences.fontStyle))
        preferences.setReduceMotion(payload.preferences.reduceMotion)
        payload.preferences.learningPath?.let { LearningPath.fromName(it) }?.let { preferences.setLearningPath(it) }
        payload.preferences.learningStyle?.let { LearningStyle.fromName(it) }?.let { preferences.setLearningStyle(it) }
        payload.preferences.dailyGoalLevel?.let { DailyGoalLevel.fromName(it) }?.let { preferences.setDailyGoalLevel(it) }
        preferences.setReduceGlassEffects(payload.preferences.reduceGlassEffects)
        preferences.setSoundEnabled(payload.preferences.soundEnabled)
    }
}
