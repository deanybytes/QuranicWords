package com.quranicwords.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity
import com.quranicwords.app.core.data.local.entity.UserProgressEntity

/** Bulk reads/writes across every user, used only by the one-time content-epoch migration
 * ([com.quranicwords.app.core.data.migration.LegacyProgressRemapper]). */
@Dao
interface LegacyMigrationDao {
    @Query("SELECT * FROM user_progress")
    suspend fun getAllProgress(): List<UserProgressEntity>

    @Query("DELETE FROM user_progress")
    suspend fun deleteAllProgress()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(rows: List<UserProgressEntity>)

    @Query("SELECT * FROM exercise_attempts")
    suspend fun getAllAttempts(): List<ExerciseAttemptEntity>

    @Query("DELETE FROM exercise_attempts")
    suspend fun deleteAllAttempts()

    @Insert
    suspend fun insertAttempts(rows: List<ExerciseAttemptEntity>)

    @Query("SELECT lessonId, practicedItemId AS wordId FROM exercises WHERE type = 'WORD_INTRO' AND practicedItemId IS NOT NULL ORDER BY lessonId, orderIndex")
    suspend fun getLessonWords(): List<LessonWordRow>
}

data class LessonWordRow(val lessonId: String, val wordId: String)
