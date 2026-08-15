package com.quranicwords.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: UserProgressEntity)

    @Query("SELECT * FROM user_progress WHERE userId = :userId")
    fun observeForUser(userId: String): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress WHERE userId = :userId AND lessonId = :lessonId LIMIT 1")
    suspend fun get(userId: String, lessonId: String): UserProgressEntity?

    /** One-shot read for backup export - [observeForUser] is a live [Flow], not suitable there. */
    @Query("SELECT * FROM user_progress WHERE userId = :userId")
    suspend fun getAllForUserOnce(userId: String): List<UserProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(progress: List<UserProgressEntity>)
}
