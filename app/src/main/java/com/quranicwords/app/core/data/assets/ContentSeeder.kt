package com.quranicwords.app.core.data.assets

import android.content.Context
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.util.AppJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
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
            database.chapterDao().deleteAll()
            database.wordFrequencyDao().deleteAll()

            // 1. Chapters
            val chapters = readAssetStream<ChaptersFile>("content/chapters.json").chapters
            database.chapterDao().insertAll(chapters)

            // 2. Sections
            val sections = readAssetStream<SectionsFile>("content/sections.json").sections
            sections.chunked(250).forEach { database.sectionDao().insertAll(it) }

            // 3. Lessons
            val vocabularyLessons = readAssetStream<LessonsFile>("content/lessons_vocabulary.json").lessons
            vocabularyLessons.chunked(250).forEach { database.lessonDao().insertAll(it) }

            // 4. Exercises (Stream-decode and insert in chunks of 500)
            val vocabularyExercises = readAssetStream<ExercisesFile>("content/exercises_vocabulary.json").exercises
            vocabularyExercises.chunked(500).forEach { chunk ->
                database.exerciseDao().insertAll(chunk.map { it.toEntity() })
            }

            // 5. Words
            val words = readAssetStream<WordFrequencyFile>("content/word_frequency.json").words
            words.chunked(500).forEach { database.wordFrequencyDao().insertAll(it) }
        }
        preferences.setContentSeeded(CONTENT_VERSION)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private inline fun <reified T> readAssetStream(assetPath: String): T {
        return context.assets.open(assetPath).use { stream ->
            AppJson.decodeFromStream(stream)
        }
    }

    companion object {
        const val CONTENT_VERSION = 23
    }
}


