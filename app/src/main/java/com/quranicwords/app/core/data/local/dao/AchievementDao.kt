package com.quranicwords.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranicwords.app.core.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    /** REPLACE - idempotent, so an unlock-check re-run for an already-unlocked achievement is a
     * harmless no-op rather than a constraint violation. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(achievement: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<AchievementEntity>)

    @Query("SELECT * FROM achievements WHERE userId = :userId")
    fun observeForUser(userId: String): Flow<List<AchievementEntity>>

    /** One-shot read for backup export - [observeForUser] is a live [Flow], not suitable there. */
    @Query("SELECT * FROM achievements WHERE userId = :userId")
    suspend fun getAllForUserOnce(userId: String): List<AchievementEntity>
}
