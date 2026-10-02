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
    @Query("SELECT * FROM exercises WHERE practicedItemId IN (:itemIds) AND type != 'WORD_INTRO' AND type != 'CHAPTER_INTRO'")
    suspend fun getScoredExercisesForItems(itemIds: List<String>): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE (type = 'TEACH_WORD' OR type = 'WORD_INTRO') AND practicedItemId IN (:itemIds)")
    suspend fun getTeachWordsForItems(itemIds: List<String>): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE lessonId IN (:lessonIds) ORDER BY orderIndex ASC")
    suspend fun getForLessons(lessonIds: List<String>): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE type = 'TEACH_WORD' OR type = 'WORD_INTRO'")
    suspend fun getAllTeachWords(): List<ExerciseEntity>

    /** Which lesson(s) each practiced word appears in - the fallback source for a word's
     * category (its lesson's category) when it has no WORD_INTRO to read one from. */
    @Query("SELECT DISTINCT practicedItemId, lessonId FROM exercises WHERE practicedItemId IS NOT NULL")
    suspend fun getPracticedItemLessons(): List<PracticedItemLesson>

    @Query("DELETE FROM exercises")
    suspend fun deleteAll()
}

/** Projection row for [ExerciseDao.getPracticedItemLessons]. */
data class PracticedItemLesson(val practicedItemId: String, val lessonId: String)
