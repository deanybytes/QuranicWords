package com.quranicwords.app.core.data.repository

import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.domain.model.BACKUP_SCHEMA_VERSION
import com.quranicwords.app.core.domain.model.BackupPayload
import com.quranicwords.app.core.domain.model.BackupPreferences
import com.quranicwords.app.core.domain.model.Language
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
            preferences = BackupPreferences(
                languageTag = preferences.languageFlow.first()?.tag,
                themeMode = preferences.themeModeFlow.first().name,
                fontStyle = preferences.fontStyleFlow.first().name,
                reduceMotion = preferences.reduceMotionFlow.first()
            )
        )
        output.use { it.write(AppJson.encodeToString(payload).toByteArray()) }
    }

    override suspend fun importBackup(input: InputStream): Result<Unit> = runCatching {
        val json = input.use { it.readBytes().decodeToString() }
        val payload = AppJson.decodeFromString<BackupPayload>(json)

        preferences.setLocalUserId(payload.userId)

        payload.stats?.let { database.userStatsDao().upsert(it) }
        database.userProgressDao().upsertAll(payload.progress)
        database.exerciseAttemptDao().deleteForUser(payload.userId)
        database.exerciseAttemptDao().insertAll(payload.attempts)

        Language.fromTag(payload.preferences.languageTag)?.let { preferences.setLanguage(it) }
        preferences.setThemeMode(ThemeMode.fromName(payload.preferences.themeMode))
        preferences.setFontStyle(QuranFontStyle.fromName(payload.preferences.fontStyle))
        preferences.setReduceMotion(payload.preferences.reduceMotion)
    }
}
