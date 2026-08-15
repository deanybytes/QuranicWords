package com.quranicwords.app.core.data.local

import androidx.room.TypeConverter
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.ItemKind

class Converters {
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
}
