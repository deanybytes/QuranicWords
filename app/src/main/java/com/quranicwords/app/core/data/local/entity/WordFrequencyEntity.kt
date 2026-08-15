package com.quranicwords.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Schema is ready for the full Quran corpus; only a small hand-authored sample is seeded in
 * this increment. See the plan's "open dependency" note on sourcing the real corpus.
 */
@Serializable
@Entity(tableName = "word_frequency")
data class WordFrequencyEntity(
    @PrimaryKey val id: String,
    val arabicWord: String,
    val frequencyRank: Int,
    val frequencyCount: Int,
    val meaningEn: String,
    val meaningBn: String,
    val audioAssetPath: String?,
    val tierLevel: Int
)
