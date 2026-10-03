package com.quranicwords.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranicwords.app.core.data.local.SQLITE_IN_CHUNK_SIZE
import com.quranicwords.app.core.data.local.chunkedInQuery
import com.quranicwords.app.core.data.local.entity.VerseEntity

@Dao
interface VerseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(verses: List<VerseEntity>)

    @Query("DELETE FROM verses")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM verses")
    suspend fun count(): Int

    @Query("SELECT * FROM verses WHERE `key` = :key")
    suspend fun get(key: String): VerseEntity?

    /** One IN-query slice - callers go through [getAll], which keeps each slice within
     * [SQLITE_IN_CHUNK_SIZE] bound parameters. */
    @Query("SELECT * FROM verses WHERE `key` IN (:keys)")
    suspend fun getChunk(keys: List<String>): List<VerseEntity>

    /** Every verse among [keys], in no particular order; unknown keys are simply absent. */
    suspend fun getAll(keys: Collection<String>): List<VerseEntity> = chunkedInQuery(keys) { getChunk(it) }
}
