package com.quranicwords.app.core.data.assets

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.local.QwDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        val report = kotlinx.serialization.json.Json.parseToJsonElement(
            java.io.File("../tools/pipeline/reports/build_report.json").readText()
        ).jsonObject
        fun reported(key: String) = report.getValue(key).jsonPrimitive.int
        assertEquals(reported("chapters"), chapters.size)
        assertEquals(reported("sections"), sections.size)
        assertEquals(reported("lessons"), lessons.size)
        assertEquals(reported("words"), wordsCount)
        val withdrawn = withdrawnExerciseCount(java.io.File("src/main/assets/content/exercises_vocabulary.json").readText())
        assertEquals(reported("exercises"), exercisesCount + withdrawn)
    }

    @Test
    fun upgradingFromTheOldCurriculumCarriesProgressOntoNewIds() = runBlocking {
        // Looks like a v1.0.0 install: content version 35, first particle lessons done.
        preferences.setContentSeeded(35)
        val userId = "learner"
        val oldCompleted = (2..5).map { "les_%04d".format(it) }
        database.legacyMigrationDao().insertProgress(oldCompleted.map {
            com.quranicwords.app.core.data.local.entity.UserProgressEntity(
                userId, it, com.quranicwords.app.core.data.local.entity.LessonStatus.COMPLETED, 90, 1_000L
            )
        })
        database.exerciseAttemptDao().insertAll(listOf(
            com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity(
                userId = userId, itemId = "w_0001", itemKind = com.quranicwords.app.core.domain.model.ItemKind.WORD,
                exerciseType = com.quranicwords.app.core.domain.model.ExerciseType.MULTIPLE_CHOICE,
                wasCorrect = true, attemptedAtEpochMillis = 1_000L
            )
        ))

        seeder.seedIfNeeded()

        val progress = database.userProgressDao().getAllForUserOnce(userId)
        org.junit.Assert.assertTrue("old lesson ids must be gone", progress.none { it.lessonId.startsWith("les_") })
        org.junit.Assert.assertTrue("some new lessons completed", progress.any {
            it.status == com.quranicwords.app.core.data.local.entity.LessonStatus.COMPLETED
        })
        val attempt = database.exerciseAttemptDao().getAllForUser(userId).single()
        org.junit.Assert.assertTrue(attempt.itemId.startsWith("wp_"))
        org.junit.Assert.assertTrue(preferences.isContentSeeded(ContentSeeder.CONTENT_VERSION))
    }
}
