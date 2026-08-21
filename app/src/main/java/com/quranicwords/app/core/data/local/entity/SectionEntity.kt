package com.quranicwords.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quranicwords.app.core.domain.model.LocalizedText
import kotlinx.serialization.Serializable

/**
 * Mid-level grouping within a [ChapterEntity] - a contiguous sub-slice of that chapter's words,
 * same precomputed-stats shape as the chapter level (see [ChapterEntity]'s doc comment) for the
 * section-intro screen.
 */
@Serializable
@Entity(
    tableName = "sections",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chapterId")]
)
data class SectionEntity(
    @PrimaryKey val id: String,
    val chapterId: String,
    val title: LocalizedText,
    val sortOrder: Int,
    val wordCount: Int,
    val quranOccurrenceCount: Int,
    val quranOccurrencePercent: Double
)
