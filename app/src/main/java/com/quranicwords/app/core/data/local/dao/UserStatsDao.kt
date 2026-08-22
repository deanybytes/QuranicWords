package com.quranicwords.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: UserStatsEntity)

    @Query("SELECT * FROM user_stats WHERE userId = :userId LIMIT 1")
    suspend fun get(userId: String): UserStatsEntity?

    @Query("SELECT * FROM user_stats WHERE userId = :userId LIMIT 1")
    fun observe(userId: String): Flow<UserStatsEntity?>

    /** Used by [com.quranicwords.app.core.domain.repository.ProgressRepository.resetProgress] -
     * the Settings "reset progress" action's per-table wipe. */
    @Query("DELETE FROM user_stats WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}
