package com.quranicwords.app.core.data.local

import androidx.room.TypeConverter
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.ItemKind
import com.quranicwords.app.core.domain.model.LocalizedText
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
    fun toExerciseType(value: String): ExerciseType = ExerciseType.valueOf(value)

    @TypeConverter
    fun fromLessonStatus(value: LessonStatus): String = value.name

    @TypeConverter
    fun toLessonStatus(value: String): LessonStatus = LessonStatus.valueOf(value)

    @TypeConverter
    fun fromItemKind(value: ItemKind): String = value.name

    @TypeConverter
    fun toItemKind(value: String): ItemKind = ItemKind.valueOf(value)

    @TypeConverter
    fun fromLessonKind(value: LessonKind): String = value.name

    @TypeConverter
    fun toLessonKind(value: String): LessonKind = LessonKind.valueOf(value)

    @TypeConverter
    fun fromLemmaCategory(value: com.quranicwords.app.core.domain.model.LemmaCategory): String = value.name

    @TypeConverter
    fun toLemmaCategory(value: String): com.quranicwords.app.core.domain.model.LemmaCategory =
        com.quranicwords.app.core.domain.model.LemmaCategory.valueOf(value)
}
