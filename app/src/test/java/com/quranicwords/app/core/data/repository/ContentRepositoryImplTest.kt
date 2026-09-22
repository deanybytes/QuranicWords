package com.quranicwords.app.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.quranicwords.app.core.data.assets.ContentSeeder
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.SectionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ContentRepositoryImplTest {

    private lateinit var database: QwDatabase
    private lateinit var repository: ContentRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, QwDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val preferences = UserPreferencesDataStore(context)
        val seeder = ContentSeeder(context, database, preferences)
        repository = ContentRepositoryImpl(database, seeder)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `getFullCurriculumTree returns ordered chapters sections and lessons and caches result`() = runTest {
        // Insert sample chapters
        database.chapterDao().insertAll(
            listOf(
                ChapterEntity("ch_01", mapOf("en" to "Chapter 1"), emptyMap(), sortOrder = 1, wordCount = 10, quranOccurrenceCount = 100, quranOccurrencePercent = 5.0),
                ChapterEntity("ch_02", mapOf("en" to "Chapter 2"), emptyMap(), sortOrder = 2, wordCount = 20, quranOccurrenceCount = 200, quranOccurrencePercent = 10.0)
            )
        )

        // Insert sections
        database.sectionDao().insertAll(
            listOf(
                SectionEntity("sec_01", "ch_01", mapOf("en" to "Sec 1"), sortOrder = 1, wordCount = 5, quranOccurrenceCount = 50, quranOccurrencePercent = 2.5),
                SectionEntity("sec_02", "ch_01", mapOf("en" to "Sec 2"), sortOrder = 2, wordCount = 5, quranOccurrenceCount = 50, quranOccurrencePercent = 2.5)
            )
        )

        // Insert lessons
        database.lessonDao().insertAll(
            listOf(
                LessonEntity("les_01", "ch_01", "sec_01", mapOf("en" to "Lesson 1"), sortOrder = 1, kind = LessonKind.REGULAR),
                LessonEntity("les_02", "ch_01", "sec_01", mapOf("en" to "Lesson 2"), sortOrder = 2, kind = LessonKind.REGULAR),
                LessonEntity("exam_01", "ch_01", null, mapOf("en" to "Chapter Exam"), sortOrder = 3, kind = LessonKind.CHAPTER_EXAM)
            )
        )

        val tree1 = repository.getFullCurriculumTree()
        assertEquals(2, tree1.size)
        assertEquals("ch_01", tree1[0].chapter.id)
        assertEquals(2, tree1[0].sections.size)
        assertEquals(2, tree1[0].sections[0].lessons.size)
        assertEquals(1, tree1[0].chapterLevelLessons.size)
        assertEquals("exam_01", tree1[0].chapterLevelLessons[0].id)

        // Verify caching: second call returns the exact same cached reference
        val tree2 = repository.getFullCurriculumTree()
        assertSame(tree1, tree2)

        // Verify single lesson lookup
        val lesson = repository.getLesson("les_01")
        assertNotNull(lesson)
        assertEquals("les_01", lesson?.id)
        assertEquals(LessonKind.REGULAR, lesson?.kind)
    }
}
