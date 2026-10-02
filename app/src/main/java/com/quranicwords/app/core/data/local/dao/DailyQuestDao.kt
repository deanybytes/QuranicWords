package com.quranicwords.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quranicwords.app.core.data.local.entity.DailyQuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyQuestDao {
    /** Lazy creation of a day's quests - a concurrent second pick can never overwrite progress. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfAbsent(rows: List<DailyQuestEntity>)

    @Query("SELECT * FROM daily_quests WHERE userId = :userId AND localDate = :localDate ORDER BY questId ASC")
    suspend fun getForDay(userId: String, localDate: String): List<DailyQuestEntity>

    @Query("SELECT * FROM daily_quests WHERE userId = :userId AND localDate = :localDate ORDER BY questId ASC")
    fun observeForDay(userId: String, localDate: String): Flow<List<DailyQuestEntity>>

    @Update
    suspend fun updateAll(rows: List<DailyQuestEntity>)

    /** Quests are only ever shown for today; older days are pruned when a new day's are made. */
    @Query("DELETE FROM daily_quests WHERE userId = :userId AND localDate < :localDate")
    suspend fun deleteBefore(userId: String, localDate: String)

    @Query("DELETE FROM daily_quests WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}
