package com.quranicwords.app.core.domain.repository

import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.ModuleEntity
import com.quranicwords.app.core.data.local.entity.WordFrequencyEntity
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    suspend fun ensureSeeded()
    fun observeModules(): Flow<List<ModuleEntity>>
    fun observeLessons(moduleId: String): Flow<List<LessonEntity>>
    suspend fun getLesson(lessonId: String): LessonEntity?
    suspend fun getModule(moduleId: String): ModuleEntity?
    suspend fun getExercisesForLesson(lessonId: String): List<ExerciseEntity>

    /** One-shot snapshot of the vocabulary corpus, ranked by frequency - the candidate pool
     * [com.quranicwords.app.core.domain.DistractorGenerator] picks siblings from. */
    suspend fun getWordCandidates(): List<WordFrequencyEntity>
}
