package com.quranicwords.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranicwords.app.core.data.local.entity.WordMemoryEntity
import kotlinx.coroutines.flow.Flow

/** Projection for strength lookups - a word's stability without the rest of its FSRS state. */
data class ItemStability(val itemId: String, val stability: Double)

@Dao
interface WordMemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: WordMemoryEntity)

    /** Backup restore - deletes-then-reinserts a whole known set, like the other progress DAOs. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<WordMemoryEntity>)

    /** The one-time backfill from attempt history: a row that already exists (written live by a
     * session that raced the backfill) always wins over the replayed one. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfAbsent(rows: List<WordMemoryEntity>)

    @Query("SELECT * FROM word_memory WHERE userId = :userId AND itemId = :itemId LIMIT 1")
    suspend fun get(userId: String, itemId: String): WordMemoryEntity?

    @Query("SELECT * FROM word_memory WHERE userId = :userId")
    suspend fun getAllForUser(userId: String): List<WordMemoryEntity>

    @Query("SELECT COUNT(*) FROM word_memory WHERE userId = :userId")
    suspend fun countForUser(userId: String): Int

    @Query("SELECT COUNT(*) FROM word_memory WHERE userId = :userId AND dueAtEpochMillis <= :nowMillis")
    fun observeDueCount(userId: String, nowMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM word_memory WHERE userId = :userId AND dueAtEpochMillis <= :nowMillis")
    suspend fun getDueCount(userId: String, nowMillis: Long): Int

    /**
     * Words due by [nowMillis], most overdue first; ties (e.g. a batch backfilled to the same
     * instant) go to the lowest retrievability - R(t,S) falls as elapsed/stability grows, so
     * ordering by that ratio descending is ordering by R ascending without computing the power.
     */
    @Query(
        """
        SELECT itemId FROM word_memory
        WHERE userId = :userId AND dueAtEpochMillis <= :nowMillis
        ORDER BY dueAtEpochMillis ASC,
            (CAST(:nowMillis - COALESCE(lastReviewedAtEpochMillis, :nowMillis) AS REAL) / MAX(stability, 0.1)) DESC,
            itemId ASC
        LIMIT :limit
        """
    )
    suspend fun getDueItemIds(userId: String, nowMillis: Long, limit: Int): List<String>

    /** Words whose most recent first try was wrong (FSRS Again), most recently missed first. */
    @Query(
        """
        SELECT itemId FROM word_memory
        WHERE userId = :userId AND lastGrade = 1
        ORDER BY lastReviewedAtEpochMillis DESC, itemId ASC
        """
    )
    suspend fun getWeakItemIds(userId: String): List<String>

    @Query(
        """
        SELECT itemId FROM word_memory
        WHERE userId = :userId AND lastGrade = 1
        ORDER BY lastReviewedAtEpochMillis DESC, itemId ASC
        """
    )
    fun observeWeakItemIds(userId: String): Flow<List<String>>

    /** Words at or above [minStability] days - "Strong+" when called with
     * [com.quranicwords.app.core.domain.srs.WordStrength.STRONG_MIN_DAYS]. */
    @Query("SELECT itemId FROM word_memory WHERE userId = :userId AND stability >= :minStability")
    suspend fun getItemIdsWithMinStability(userId: String, minStability: Double): List<String>

    @Query("SELECT itemId, stability FROM word_memory WHERE userId = :userId")
    suspend fun getStabilities(userId: String): List<ItemStability>

    @Query("SELECT itemId, stability FROM word_memory WHERE userId = :userId")
    fun observeStabilities(userId: String): Flow<List<ItemStability>>

    @Query("DELETE FROM word_memory WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}
