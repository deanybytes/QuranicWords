package com.quranicwords.app.core.domain.repository

import java.io.InputStream
import java.io.OutputStream

/**
 * Exports/imports this device's progress as a single JSON file - the local-device-only mechanism
 * for carrying progress across a reinstall or device (see docs/ARCHITECTURE.md). Takes plain JDK
 * streams rather than an `android.net.Uri` so this interface stays free of Android framework
 * types; the caller (SettingsViewModel) opens the stream from a Storage-Access-Framework-picked
 * Uri via `ContentResolver`.
 */
interface BackupRepository {
    suspend fun exportBackup(output: OutputStream): Result<Unit>

    /** On success, the imported backup's user id becomes this device's local user id (see
     * [com.quranicwords.app.core.data.datastore.UserPreferencesDataStore.setLocalUserId]) -
     * so restored progress is what every screen reads back immediately. */
    suspend fun importBackup(input: InputStream): Result<Unit>
}
