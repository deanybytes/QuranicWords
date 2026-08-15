package com.quranicwords.app.core.data.repository

import com.quranicwords.app.core.data.assets.ContentSeeder
import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.ModuleEntity
import com.quranicwords.app.core.data.local.entity.WordFrequencyEntity
import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val database: QwDatabase,
    private val contentSeeder: ContentSeeder
) : ContentRepository {

    override suspend fun ensureSeeded() = contentSeeder.seedIfNeeded()

    override fun observeModules(): Flow<List<ModuleEntity>> = database.moduleDao().observeAll()

    override fun observeLessons(moduleId: String): Flow<List<LessonEntity>> =
        database.lessonDao().observeForModule(moduleId)

    override suspend fun getLesson(lessonId: String): LessonEntity? =
        database.lessonDao().getById(lessonId)

    override suspend fun getModule(moduleId: String): ModuleEntity? =
        database.moduleDao().getById(moduleId)

    override suspend fun getExercisesForLesson(lessonId: String): List<ExerciseEntity> =
        database.exerciseDao().getForLesson(lessonId)

    // The vocabulary corpus is static content, re-fetched in full (all 3680+ rows) on every
    // lesson entry otherwise - cached for this repository's process lifetime once loaded. Safe
    // to cache indefinitely: seeding always completes before any lesson is opened (see
    // ContentSeeder), so this never observes a stale pre-reseed snapshot in practice. A benign
    // double-fetch is possible if two callers race before the first populates this, which is
    // harmless (just redundant work, not incorrect data).
    @Volatile
    private var wordCandidatesCache: List<WordFrequencyEntity>? = null

    override suspend fun getWordCandidates(): List<WordFrequencyEntity> =
        wordCandidatesCache ?: database.wordFrequencyDao().observeAllByFrequency().first()
            .also { wordCandidatesCache = it }
}
