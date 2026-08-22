package com.quranicwords.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranicwords.app.core.data.local.entity.DailyPracticeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyPracticeDao {
    /** REPLACE - used only by backup restore, which deletes-then-reinserts a whole known set. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<DailyPracticeEntity>)

    @Query("SELECT * FROM daily_practice WHERE userId = :userId AND localDate = :localDate")
    suspend fun get(userId: String, localDate: String): DailyPracticeEntity?

    @Query("SELECT * FROM daily_practice WHERE userId = :userId AND localDate = :localDate")
    fun observe(userId: String, localDate: String): Flow<DailyPracticeEntity?>

    @Query(
        """
        INSERT INTO daily_practice (userId, localDate, minutesPracticed)
        VALUES (:userId, :localDate, :minutes)
        ON CONFLICT(userId, localDate) DO UPDATE SET minutesPracticed = minutesPracticed + :minutes
        """
    )
    suspend fun addMinutes(userId: String, localDate: String, minutes: Int)

    @Query("SELECT * FROM daily_practice WHERE userId = :userId ORDER BY localDate ASC")
    suspend fun getAllForUserOnce(userId: String): List<DailyPracticeEntity>

    @Query("SELECT * FROM daily_practice WHERE userId = :userId AND localDate BETWEEN :startDate AND :endDate ORDER BY localDate ASC")
    fun observeForRange(userId: String, startDate: String, endDate: String): Flow<List<DailyPracticeEntity>>

    /** Used by [com.quranicwords.app.core.domain.repository.ProgressRepository.resetProgress] -
     * the Settings "reset progress" action's per-table wipe. */
    @Query("DELETE FROM daily_practice WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}
