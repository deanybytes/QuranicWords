package com.quranicwords.app.core.data.local.migration

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

/**
 * Copies the database file aside before Room runs a schema upgrade on it, so a migration bug can
 * never cost a learner their progress. A single rolling `<name>.v<oldVersion>.bak` set is kept
 * under `noBackupFilesDir`, which Android Auto Backup never uploads.
 */
object PreMigrationBackup {
    private const val TAG = "PreMigrationBackup"
    private const val SNAPSHOT_DIR = "db-snapshots"
    private val SIDE_FILES = listOf("", "-wal", "-shm")

    fun snapshotIfUpgrading(context: Context, databaseName: String, targetVersion: Int) {
        val dbFile = context.getDatabasePath(databaseName)
        if (!dbFile.exists()) return
        val currentVersion = runCatching { readUserVersion(dbFile) }.getOrElse {
            Log.w(TAG, "Could not read schema version; skipping snapshot", it)
            return
        }
        if (currentVersion <= 0 || currentVersion >= targetVersion) return

        val sourceDir = dbFile.parentFile ?: return
        val snapshotDir = File(context.noBackupFilesDir, SNAPSHOT_DIR)
        runCatching {
            snapshotDir.deleteRecursively()
            snapshotDir.mkdirs()
            for (suffix in SIDE_FILES) {
                val src = File(sourceDir, databaseName + suffix)
                if (src.exists()) src.copyTo(File(snapshotDir, "$databaseName.v$currentVersion.bak$suffix"), overwrite = true)
            }
        }.onFailure { Log.w(TAG, "Snapshot before migrating v$currentVersion failed", it) }
    }

    private fun readUserVersion(dbFile: File): Int =
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
}
