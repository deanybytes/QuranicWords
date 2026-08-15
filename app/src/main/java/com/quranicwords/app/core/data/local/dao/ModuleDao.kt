package com.quranicwords.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranicwords.app.core.data.local.entity.ModuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(modules: List<ModuleEntity>)

    /** Cascades to lessons/exercises (ON DELETE CASCADE) - used when re-seeding bundled content
     * on a [com.quranicwords.app.core.data.assets.ContentSeeder.CONTENT_VERSION]
     * bump, never touches user_progress/user_stats. */
    @Query("DELETE FROM modules")
    suspend fun deleteAll()

    @Query("SELECT * FROM modules ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<ModuleEntity>>

    @Query("SELECT * FROM modules WHERE id = :moduleId LIMIT 1")
    suspend fun getById(moduleId: String): ModuleEntity?

    @Query("SELECT COUNT(*) FROM modules")
    suspend fun count(): Int
}
