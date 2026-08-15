package com.quranicwords.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranicwords.app.core.data.local.entity.LessonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<LessonEntity>)

    @Query("SELECT * FROM lessons WHERE moduleId = :moduleId ORDER BY sortOrder ASC")
    fun observeForModule(moduleId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE moduleId = :moduleId ORDER BY sortOrder ASC")
    suspend fun getForModule(moduleId: String): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    suspend fun getById(lessonId: String): LessonEntity?

    @Query("SELECT COUNT(*) FROM lessons")
    suspend fun count(): Int
}
