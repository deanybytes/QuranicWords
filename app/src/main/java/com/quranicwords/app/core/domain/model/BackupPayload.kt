package com.quranicwords.app.core.domain.model

import com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import kotlinx.serialization.Serializable

/**
 * Everything [com.quranicwords.app.core.domain.repository.BackupRepository] writes
 * to / reads from a local backup file - the app is local-device-only (no Google Sign-In /
 * Firebase), so this is the sole mechanism for a learner to carry progress across a reinstall or
 * device. Deliberately excludes content tables (modules/lessons/exercises): those are re-derived
 * by ContentSeeder from bundled assets on next launch, never user data.
 */
@Serializable
data class BackupPayload(
    val schemaVersion: Int,
    val exportedAtEpochMillis: Long,
    val userId: String,
    val stats: UserStatsEntity?,
    val progress: List<UserProgressEntity>,
    val attempts: List<ExerciseAttemptEntity>,
    val preferences: BackupPreferences
)

@Serializable
data class BackupPreferences(
    val languageTag: String?,
    val themeMode: String,
    val fontStyle: String,
    val reduceMotion: Boolean
)

const val BACKUP_SCHEMA_VERSION = 1
