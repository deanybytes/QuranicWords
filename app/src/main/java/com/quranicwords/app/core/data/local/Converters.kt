package com.quranicwords.app.core.data.local

import androidx.room.TypeConverter
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.ItemKind
import com.quranicwords.app.core.domain.model.LocalizedText
import com.quranicwords.app.core.domain.srs.MemoryState
import com.quranicwords.app.core.util.AppJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

class Converters {
    @TypeConverter
    fun fromLocalizedText(value: LocalizedText): String =
        AppJson.encodeToString(MapSerializer(String.serializer(), String.serializer()), value)

    @TypeConverter
    fun toLocalizedText(value: String): LocalizedText =
        AppJson.decodeFromString(MapSerializer(String.serializer(), String.serializer()), value)

    @TypeConverter
    fun fromExerciseType(value: ExerciseType): String = value.name

    @TypeConverter
    fun toExerciseType(value: String): ExerciseType =
        // Lenient like [toMemoryState]: a name this build doesn't know degrades instead of
        // crashing the read. (The withdrawn listening types are still enum constants, so old
        // attempt rows decode to themselves.)
        ExerciseType.entries.find { it.name == value } ?: ExerciseType.MULTIPLE_CHOICE

    @TypeConverter
    fun fromLessonStatus(value: LessonStatus): String = value.name

    @TypeConverter
    fun toLessonStatus(value: String): LessonStatus =
        LessonStatus.entries.find { it.name == value } ?: LessonStatus.UNLOCKED

    @TypeConverter
    fun fromItemKind(value: ItemKind): String = value.name

    @TypeConverter
    fun toItemKind(value: String): ItemKind =
        ItemKind.entries.find { it.name == value } ?: ItemKind.WORD

    @TypeConverter
    fun fromLessonKind(value: LessonKind): String = value.name

    @TypeConverter
    fun toLessonKind(value: String): LessonKind =
        LessonKind.entries.find { it.name == value } ?: LessonKind.REGULAR

    @TypeConverter
    fun fromLemmaCategory(value: com.quranicwords.app.core.domain.model.LemmaCategory): String = value.name

    @TypeConverter
    fun toLemmaCategory(value: String): com.quranicwords.app.core.domain.model.LemmaCategory =
        com.quranicwords.app.core.domain.model.LemmaCategory.entries.find { it.name == value }
            ?: com.quranicwords.app.core.domain.model.LemmaCategory.NOUN

    @TypeConverter
    fun fromMemoryState(value: MemoryState): String = value.name

    /** Unknown names (a row written by a newer build) degrade to NEW rather than crashing a read. */
    @TypeConverter
    fun toMemoryState(value: String): MemoryState =
        MemoryState.entries.find { it.name == value } ?: MemoryState.NEW
}
