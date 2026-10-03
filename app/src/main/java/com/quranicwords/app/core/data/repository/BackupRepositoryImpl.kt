package com.quranicwords.app.core.data.repository

import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.domain.model.BACKUP_SCHEMA_VERSION
import com.quranicwords.app.core.domain.model.BackupPayload
import com.quranicwords.app.core.domain.model.BackupPreferences
import com.quranicwords.app.core.domain.model.DailyGoalLevel
import com.quranicwords.app.core.domain.model.FontScale
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.LearningPath
import com.quranicwords.app.core.domain.model.LearningStyle
import com.quranicwords.app.core.domain.model.QuranFontStyle
import com.quranicwords.app.core.domain.model.ThemeMode
import com.quranicwords.app.core.domain.repository.BackupRepository
import com.quranicwords.app.core.util.AppJson
import androidx.room.withTransaction
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
            wordMemory = database.wordMemoryDao().getAllForUser(userId),
            preferences = BackupPreferences(
                languageTag = preferences.languageFlow.first()?.tag,
                themeMode = preferences.themeModeFlow.first().name,
                fontStyle = preferences.fontStyleFlow.first().name,
                reduceMotion = preferences.reduceMotionFlow.first(),
                learningPath = preferences.learningPathFlow.first().name,
                learningStyle = preferences.learningStyleFlow.first().name,
                dailyGoalLevel = preferences.dailyGoalLevelFlow.first().name,
                reduceGlassEffects = preferences.reduceGlassEffectsFlow.first(),
                soundEnabled = preferences.soundEnabledFlow.first(),
                fontScale = preferences.fontScaleFlow.first().name,
                requireExitConfirmation = preferences.requireExitConfirmationFlow.first()
            )
        )
        output.use { it.write(AppJson.encodeToString(payload).toByteArray()) }
    }

    override suspend fun importBackup(input: InputStream): Result<Unit> = runCatching {
        val json = input.use { readCapped(it) }.decodeToString()
        val payload = AppJson.decodeFromString<BackupPayload>(json)
        require(payload.schemaVersion in 1..BACKUP_SCHEMA_VERSION) {
            "Backup schema ${payload.schemaVersion} is newer than this app supports ($BACKUP_SCHEMA_VERSION)"
        }

        val targetUserId = preferences.getOrCreateLocalUserId()

        // Wipe-and-restore is all-or-nothing: a decode/insert failure part-way through must leave
        // the learner's existing progress exactly as it was.
        database.withTransaction {
            val userIds = listOf(targetUserId, payload.userId).filter { it.isNotBlank() }.distinct()
            for (id in userIds) {
                database.userProgressDao().deleteForUser(id)
                database.userStatsDao().deleteForUser(id)
                database.exerciseAttemptDao().deleteForUser(id)
                database.achievementDao().deleteForUser(id)
                database.dailyPracticeDao().deleteForUser(id)
                database.wordMemoryDao().deleteForUser(id)
            }

            payload.stats?.copy(userId = targetUserId)?.let { database.userStatsDao().upsert(it) }
            database.userProgressDao().upsertAll(payload.progress.map { it.copy(userId = targetUserId) })
            database.exerciseAttemptDao().insertAll(payload.attempts.map { it.copy(id = 0, userId = targetUserId) })
            database.achievementDao().insertAll(payload.achievements.map { it.copy(userId = targetUserId) })
            database.dailyPracticeDao().insertAll(payload.dailyPractices.map { it.copy(userId = targetUserId) })
            database.wordMemoryDao().insertAll(payload.wordMemory.map { it.copy(userId = targetUserId) })
        }
        // A pre-v3 backup has attempts but no memory: re-arm the one-time backfill so the next
        // curriculum load replays the restored history (a v3 backup's rows are already exact).
        preferences.setWordMemoryBackfilled(payload.wordMemory.isNotEmpty() || payload.attempts.isEmpty())

        val prefs = payload.preferences
        Language.fromTag(prefs.languageTag)?.let { preferences.setLanguage(it) }
        preferences.setThemeMode(ThemeMode.fromName(prefs.themeMode))
        preferences.setFontStyle(QuranFontStyle.fromName(prefs.fontStyle))
        preferences.setReduceMotion(prefs.reduceMotion)
        prefs.learningPath?.let { LearningPath.fromName(it) }?.let { preferences.setLearningPath(it) }
        prefs.learningStyle?.let { LearningStyle.fromName(it) }?.let { preferences.setLearningStyle(it) }
        prefs.dailyGoalLevel?.let { DailyGoalLevel.fromName(it) }?.let { preferences.setDailyGoalLevel(it) }
        preferences.setReduceGlassEffects(prefs.reduceGlassEffects)
        preferences.setSoundEnabled(prefs.soundEnabled)
        prefs.fontScale?.let { preferences.setFontScale(FontScale.fromName(it)) }
        prefs.requireExitConfirmation?.let { preferences.setRequireExitConfirmation(it) }
    }

    private fun readCapped(input: InputStream): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= MAX_BACKUP_BYTES) { "Backup file is larger than ${MAX_BACKUP_BYTES / (1024 * 1024)} MB" }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private companion object {
        const val MAX_BACKUP_BYTES = 64L * 1024 * 1024
    }
}
