package com.quranicwords.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranicwords.app.core.data.local.entity.WordFrequencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordFrequencyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordFrequencyEntity>)

    @Query("DELETE FROM word_frequency")
    suspend fun deleteAll()

    @Query("SELECT * FROM word_frequency ORDER BY frequencyRank ASC")
    fun observeAllByFrequency(): Flow<List<WordFrequencyEntity>>

    @Query("SELECT COUNT(*) FROM word_frequency")
    suspend fun count(): Int
}
