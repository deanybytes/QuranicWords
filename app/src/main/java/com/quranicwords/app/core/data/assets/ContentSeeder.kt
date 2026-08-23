package com.quranicwords.app.core.data.assets

import android.content.Context
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.util.AppJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: QwDatabase,
    private val preferences: UserPreferencesDataStore
) {
    suspend fun seedIfNeeded() {
        if (preferences.isContentSeeded(CONTENT_VERSION)) return
        withContext(Dispatchers.IO) {
            val chapters = readAsset<ChaptersFile>("content/chapters.json").chapters
            val sections = readAsset<SectionsFile>("content/sections.json").sections
            val vocabularyLessons = readAsset<LessonsFile>("content/lessons_vocabulary.json").lessons
            val vocabularyExercises = readAsset<ExercisesFile>("content/exercises_vocabulary.json").exercises
            val words = readAsset<WordFrequencyFile>("content/word_frequency.json").words

            // REPLACE-on-insert only overwrites rows whose id reappears in the new seed data; it
            // never removes rows whose id is now gone (e.g. old lessons after a content
            // restructure). Clear seeded-content tables first so re-seeding on a CONTENT_VERSION
            // bump is a clean reset, not an accumulation - harmless no-op on a first install since
            // these tables start empty. Deleting chapters cascades to sections/lessons/exercises
            // (FK ON DELETE CASCADE); word_frequency has no dependents and is cleared directly.
            // user_progress/user_stats are real user data and are never touched here.
            database.chapterDao().deleteAll()
            database.wordFrequencyDao().deleteAll()

            database.chapterDao().insertAll(chapters)
            database.sectionDao().insertAll(sections)
            database.lessonDao().insertAll(vocabularyLessons)
            database.exerciseDao().insertAll(vocabularyExercises.map { it.toEntity() })
            database.wordFrequencyDao().insertAll(words)
        }
        preferences.setContentSeeded(CONTENT_VERSION)
    }

    private inline fun <reified T> readAsset(assetPath: String): T {
        val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        return AppJson.decodeFromString(text)
    }

    companion object {
        // Fresh fork, own content-version history - bump whenever bundled content JSON changes
        // shape in a way that needs a full reseed (see the wipe-and-reseed comment above).
        const val CONTENT_VERSION = 10
    }
}
