package com.quranicwords.app.core.data

import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the key used for local Room progress/stats: a locally-generated device id, persisted
 * in [UserPreferencesDataStore]. The app is local-device-only - see
 * [[com.quranicwords.app.core.data.repository.BackupRepositoryImpl]] for the export/import
 * backup flow - so this id is the single source of truth for "who" is progressing, restorable
 * from a backup file via [UserPreferencesDataStore.setLocalUserId].
 */
@Singleton
class CurrentUserIdProvider @Inject constructor(
    private val preferences: UserPreferencesDataStore
) {
    suspend fun get(): String = preferences.getOrCreateLocalUserId()
}
