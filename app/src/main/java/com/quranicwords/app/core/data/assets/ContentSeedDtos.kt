package com.quranicwords.app.core.data.assets

import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.ModuleEntity
import com.quranicwords.app.core.data.local.entity.WordFrequencyEntity
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.practicedItemId
import com.quranicwords.app.core.util.AppJson
import kotlinx.serialization.Serializable

@Serializable
internal data class ModulesFile(val modules: List<ModuleEntity>)

@Serializable
internal data class LessonsFile(val lessons: List<LessonEntity>)

@Serializable
internal data class WordFrequencyFile(val words: List<WordFrequencyEntity>)

@Serializable
internal data class ExercisesFile(val exercises: List<ExerciseSeedDto>)

/**
 * Exercise shape genuinely differs from [ExerciseEntity] (nested, polymorphic [content] vs. a
 * flattened [ExerciseEntity.contentJson] string), so - unlike the other seed files - this one
 * earns a real DTO + mapping step rather than annotating the entity directly.
 */
@Serializable
internal data class ExerciseSeedDto(
    val id: String,
    val lessonId: String,
    val orderIndex: Int,
    val exerciseType: ExerciseType,
    val content: ExerciseContent
) {
    fun toEntity(): ExerciseEntity = ExerciseEntity(
        id = id,
        lessonId = lessonId,
        orderIndex = orderIndex,
        type = exerciseType,
        contentJson = AppJson.encodeToString(ExerciseContent.serializer(), content),
        practicedItemId = content.practicedItemId()
    )
}
