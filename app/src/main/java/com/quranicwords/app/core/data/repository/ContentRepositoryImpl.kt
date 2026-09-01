package com.quranicwords.app.core.data.repository

import com.quranicwords.app.core.data.assets.ContentSeeder
import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.SectionEntity
import com.quranicwords.app.core.data.local.entity.WordFrequencyEntity
import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.domain.model.ChapterWithSections
import com.quranicwords.app.core.domain.model.SectionWithLessons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val database: QwDatabase,
    private val contentSeeder: ContentSeeder
) : ContentRepository {

    override suspend fun ensureSeeded() = contentSeeder.seedIfNeeded()

    override fun observeChapters(): Flow<List<ChapterEntity>> = database.chapterDao().observeAll()

    override fun observeSections(chapterId: String): Flow<List<SectionEntity>> =
        database.sectionDao().observeForChapter(chapterId)

    override fun observeLessons(sectionId: String): Flow<List<LessonEntity>> =
        database.lessonDao().observeForSection(sectionId)

    override suspend fun getChapterLevelLessons(chapterId: String): List<LessonEntity> =
        listOfNotNull(
            database.lessonDao().getChapterLevelLesson(chapterId, LessonKind.CHAPTER_EXAM),
            database.lessonDao().getChapterLevelLesson(chapterId, LessonKind.CHAPTER_FLASHBACK)
        ).sortedBy { it.sortOrder }

    override suspend fun getLesson(lessonId: String): LessonEntity? =
        database.lessonDao().getById(lessonId)

    override suspend fun getChapter(chapterId: String): ChapterEntity? =
        database.chapterDao().getById(chapterId)

    override suspend fun getSection(sectionId: String): SectionEntity? =
        database.sectionDao().getById(sectionId)

    override suspend fun getExercisesForLesson(lessonId: String): List<ExerciseEntity> =
        database.exerciseDao().getForLesson(lessonId)

    @Volatile
    private var fullCurriculumTreeCache: List<ChapterWithSections>? = null

    override suspend fun getFullCurriculumTree(): List<ChapterWithSections> {
        fullCurriculumTreeCache?.let { return it }
        return withContext(Dispatchers.IO) {
            val allChapters = database.chapterDao().getAll().sortedBy { it.sortOrder }
            val allSections = database.sectionDao().getAll().sortedBy { it.sortOrder }
            val allLessons = database.lessonDao().getAll().sortedBy { it.sortOrder }

            val lessonsBySection = allLessons.filter { it.sectionId != null }.groupBy { it.sectionId!! }
            val chapterLevelLessons = allLessons.filter { it.sectionId == null }.groupBy { it.chapterId }
            val sectionsByChapter = allSections.groupBy { it.chapterId }

            val tree = allChapters.map { chapter ->
                val sections = sectionsByChapter[chapter.id].orEmpty().map { section ->
                    SectionWithLessons(section, lessonsBySection[section.id].orEmpty())
                }
                ChapterWithSections(
                    chapter = chapter,
                    sections = sections,
                    chapterLevelLessons = chapterLevelLessons[chapter.id].orEmpty()
                )
            }
            fullCurriculumTreeCache = tree
            tree
        }
    }

    // The vocabulary corpus is static content, re-fetched in full (all 4616 rows) on every
    // lesson entry otherwise - cached for this repository's process lifetime once loaded. Safe
    // to cache indefinitely: seeding always completes before any lesson is opened (see
    // ContentSeeder), so this never observes a stale pre-reseed snapshot in practice.
    @Volatile
    private var wordCandidatesCache: List<WordFrequencyEntity>? = null

    @Volatile
    private var wordIntrosCache: Map<String, com.quranicwords.app.core.domain.model.ExerciseContent.WordIntro>? = null

    override suspend fun getWordCandidates(): List<WordFrequencyEntity> =
        wordCandidatesCache ?: database.wordFrequencyDao().observeAllByFrequency().first()
            .also { wordCandidatesCache = it }

    override suspend fun getWordIntrosForItems(itemIds: List<String>): Map<String, com.quranicwords.app.core.domain.model.ExerciseContent.WordIntro> {
        if (itemIds.isEmpty()) return emptyMap()
        val missingIds = itemIds.filter { wordIntrosCache?.containsKey(it) != true }
        if (missingIds.isNotEmpty()) {
            val loaded = withContext(Dispatchers.IO) {
                database.exerciseDao().getTeachWordsForItems(missingIds).mapNotNull {
                    runCatching {
                        com.quranicwords.app.core.util.AppJson.decodeFromString(
                            com.quranicwords.app.core.domain.model.ExerciseContent.serializer(),
                            it.contentJson
                        )
                    }.getOrNull() as? com.quranicwords.app.core.domain.model.ExerciseContent.WordIntro
                }.associateBy { it.wordId }
            }
            val updated = (wordIntrosCache ?: emptyMap()) + loaded
            wordIntrosCache = updated
        }
        return wordIntrosCache?.filterKeys { it in itemIds } ?: emptyMap()
    }

    override suspend fun getAllWordIntros(): Map<String, com.quranicwords.app.core.domain.model.ExerciseContent.WordIntro> {
        wordIntrosCache?.let { return it }
        val intros = withContext(Dispatchers.IO) {
            database.exerciseDao().getAllTeachWords().mapNotNull {
                runCatching {
                    com.quranicwords.app.core.util.AppJson.decodeFromString(
                        com.quranicwords.app.core.domain.model.ExerciseContent.serializer(),
                        it.contentJson
                    )
                }.getOrNull() as? com.quranicwords.app.core.domain.model.ExerciseContent.WordIntro
            }.associateBy { it.wordId }
        }
        wordIntrosCache = intros
        return intros
    }
}

