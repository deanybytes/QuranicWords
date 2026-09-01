package com.quranicwords.app.core.data.assets

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.local.QwDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FullDatabaseSeedingTest {

    private lateinit var context: Context
    private lateinit var database: QwDatabase
    private lateinit var preferences: UserPreferencesDataStore
    private lateinit var seeder: ContentSeeder

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, QwDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferencesDataStore(context)
        seeder = ContentSeeder(context, database, preferences)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun fullSeedingRunsSuccessfullyIntoDatabase() = runBlocking {
        seeder.seedIfNeeded()

        val chapters = database.chapterDao().getAll()
        val sections = database.sectionDao().getAll()
        val lessons = database.lessonDao().getAll()
        val wordsCount = database.wordFrequencyDao().count()
        val exercisesCount = database.exerciseDao().count()

        println("Seeded successfully: chapters=${chapters.size}, sections=${sections.size}, lessons=${lessons.size}, words=$wordsCount, exercises=$exercisesCount")
        assertEquals(10, chapters.size)
        assertEquals(100, sections.size)
        assertEquals(1202, lessons.size)
        assertEquals(4616, wordsCount)
        assertEquals(9242, exercisesCount)
    }
}
