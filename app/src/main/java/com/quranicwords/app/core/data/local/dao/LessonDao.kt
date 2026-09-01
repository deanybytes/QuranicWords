package com.quranicwords.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<LessonEntity>)

    /** Whole-curriculum snapshot for [com.quranicwords.app.core.domain.CurriculumUnlockResolver] -
     * small enough (hundreds of rows) to load in full rather than walking the tree with one query
     * per hop. */
    @Query("SELECT * FROM lessons")
    suspend fun getAll(): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    fun observeForSection(sectionId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    suspend fun getForSection(sectionId: String): List<LessonEntity>

    /** The chapter-scoped [LessonKind.CHAPTER_EXAM]/[LessonKind.CHAPTER_FLASHBACK] row for
     * [chapterId], if one exists (flashback rows don't exist for chapter 1 - see [LessonKind]'s
     * doc comment). `sectionId IS NULL` distinguishes these from section-scoped lessons that
     * merely happen to share the chapter. */
    @Query("SELECT * FROM lessons WHERE chapterId = :chapterId AND sectionId IS NULL AND kind = :kind LIMIT 1")
    suspend fun getChapterLevelLesson(chapterId: String, kind: LessonKind): LessonEntity?

    @Query("SELECT * FROM lessons WHERE chapterId = :chapterId ORDER BY sortOrder ASC")
    suspend fun getForChapter(chapterId: String): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    suspend fun getById(lessonId: String): LessonEntity?

    @Query("SELECT COUNT(*) FROM lessons")
    suspend fun count(): Int
}

