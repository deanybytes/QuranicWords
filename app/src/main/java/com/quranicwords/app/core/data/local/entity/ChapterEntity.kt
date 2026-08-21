package com.quranicwords.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quranicwords.app.core.domain.model.LocalizedText
import kotlinx.serialization.Serializable

/**
 * Top-level grouping of the vocabulary curriculum - a contiguous, frequency-rank-ordered slice of
 * [WordFrequencyEntity] rows, computed once at content-authoring time (see
 * `tools/ingestion/11_build_curriculum.py`) and never reshuffled at runtime. [wordCount]/
 * [quranOccurrenceCount]/[quranOccurrencePercent] are precomputed so the chapter-intro screen can
 * show "these N words cover X% of the Quran's word occurrences" without a runtime aggregation
 * query.
 */
@Serializable
@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val id: String,
    val title: LocalizedText,
    val description: LocalizedText,
    val sortOrder: Int,
    val wordCount: Int,
    val quranOccurrenceCount: Int,
    val quranOccurrencePercent: Double
)
