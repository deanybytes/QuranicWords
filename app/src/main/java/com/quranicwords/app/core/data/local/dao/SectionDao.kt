package com.quranicwords.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranicwords.app.core.data.local.entity.SectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sections: List<SectionEntity>)

    /** Whole-curriculum snapshot for [com.quranicwords.app.core.domain.CurriculumUnlockResolver] -
     * see [com.quranicwords.app.core.data.local.dao.LessonDao.getAll]'s doc comment. */
    @Query("SELECT * FROM sections")
    suspend fun getAll(): List<SectionEntity>

    @Query("SELECT * FROM sections WHERE chapterId = :chapterId ORDER BY sortOrder ASC")
    fun observeForChapter(chapterId: String): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE chapterId = :chapterId ORDER BY sortOrder ASC")
    suspend fun getForChapter(chapterId: String): List<SectionEntity>

    @Query("SELECT * FROM sections WHERE id = :sectionId LIMIT 1")
    suspend fun getById(sectionId: String): SectionEntity?

    @Query("SELECT COUNT(*) FROM sections")
    suspend fun count(): Int

    @Query("DELETE FROM sections")
    suspend fun deleteAll()
}
