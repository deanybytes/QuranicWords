package com.quranicwords.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranicwords.app.core.data.local.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<ChapterEntity>)

    /** Cascades to sections/lessons/exercises (ON DELETE CASCADE) - used when re-seeding bundled
     * content on a [com.quranicwords.app.core.data.assets.ContentSeeder.CONTENT_VERSION]
     * bump, never touches user_progress/user_stats. */
    @Query("DELETE FROM chapters")
    suspend fun deleteAll()

    @Query("SELECT * FROM chapters ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters ORDER BY sortOrder ASC")
    suspend fun getAll(): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE id = :chapterId LIMIT 1")
    suspend fun getById(chapterId: String): ChapterEntity?

    @Query("SELECT COUNT(*) FROM chapters")
    suspend fun count(): Int
}
