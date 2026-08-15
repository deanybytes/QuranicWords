package com.quranicwords.app.core.domain.repository

import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.SectionEntity
import com.quranicwords.app.core.data.local.entity.WordFrequencyEntity
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    suspend fun ensureSeeded()
    fun observeChapters(): Flow<List<ChapterEntity>>
    fun observeSections(chapterId: String): Flow<List<SectionEntity>>
    fun observeLessons(sectionId: String): Flow<List<LessonEntity>>
    suspend fun getLesson(lessonId: String): LessonEntity?
    suspend fun getChapter(chapterId: String): ChapterEntity?
    suspend fun getSection(sectionId: String): SectionEntity?
    suspend fun getExercisesForLesson(lessonId: String): List<ExerciseEntity>

    /** One-shot snapshot of the vocabulary corpus, ranked by frequency - the candidate pool
     * [com.quranicwords.app.core.domain.DistractorGenerator] picks siblings from. */
    suspend fun getWordCandidates(): List<WordFrequencyEntity>
}
