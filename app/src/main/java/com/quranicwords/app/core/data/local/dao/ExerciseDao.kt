package com.quranicwords.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranicwords.app.core.data.local.entity.ExerciseEntity

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Query("SELECT * FROM exercises WHERE lessonId = :lessonId ORDER BY orderIndex ASC")
    suspend fun getForLesson(lessonId: String): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    /** Every scored exercise whose practiced item is one of [itemIds] - the Review session's
     * source list. Each word/letter is authored with exactly one scored exercise today, so this
     * naturally returns one row per id; if a future content type adds a second exercise for the
     * same item, showing both is a feature (more variety on a missed item), not a bug - the
     * caller (`ProgressRepository.getReviewExercises`) is what applies the session's size cap. */
    @Query("SELECT * FROM exercises WHERE practicedItemId IN (:itemIds)")
    suspend fun getScoredExercisesForItems(itemIds: List<String>): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE type = 'TEACH_WORD'")
    suspend fun getAllTeachWords(): List<ExerciseEntity>
}
