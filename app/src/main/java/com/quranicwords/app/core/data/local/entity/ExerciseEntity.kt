package com.quranicwords.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quranicwords.app.core.domain.model.ExerciseType

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("lessonId")]
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val orderIndex: Int,
    val type: ExerciseType,
    /** JSON-encoded [com.quranicwords.app.core.domain.model.ExerciseContent]. */
    val contentJson: String,
    /** Flat copy of `content.practicedItemId()` (see
     * [com.quranicwords.app.core.domain.model.practicedItemId]), computed once at
     * seed time - `contentJson` is opaque to SQL, so this is what makes "find every scored
     * exercise for these missed word/letter ids" (the Review session) a real query instead of a
     * full-table decode. Null for teach steps and Matching, same as the extension it mirrors. */
    val practicedItemId: String? = null
)
