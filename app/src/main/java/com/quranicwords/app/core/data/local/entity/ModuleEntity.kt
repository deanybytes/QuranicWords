package com.quranicwords.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quranicwords.app.core.domain.model.ItemKind
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "modules")
data class ModuleEntity(
    @PrimaryKey val id: String,
    val titleEn: String,
    val titleBn: String,
    val descriptionEn: String,
    val descriptionBn: String,
    val tierMin: Int,
    val sortOrder: Int,
    val isImplemented: Boolean,
    /** What kind of item this module's lessons quiz (letters vs. words) - lets
     * `LessonViewModel` resolve `ItemKind` for attempt logging from real seeded data instead of
     * a hardcoded module-id string comparison. Defaults to WORD for any content authored before
     * this field existed. */
    val contentKind: ItemKind = ItemKind.WORD
)
