package com.quranicwords.app.core.data.assets

import android.content.Context
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import androidx.room.withTransaction
import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.data.migration.LegacyContentMap
import com.quranicwords.app.core.data.migration.LegacyProgressRemapper
import com.quranicwords.app.core.util.AppJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.decodeToSequence
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the read-only curriculum tables from the bundled assets produced by tools/pipeline.
 *
 * The whole reseed runs in one transaction, so the app never observes half-seeded content. When
 * the previous install was on the pre-rebuild curriculum (content version < [FIRST_REBUILT_VERSION],
 * i.e. v1.0.0/v1.0.1), the learner's progress is carried over by [LegacyProgressRemapper] inside
 * that same transaction - a crash at any point leaves the old content and progress intact and
 * the migration simply runs again on the next launch.
 */
@Singleton
class ContentSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: QwDatabase,
    private val preferences: UserPreferencesDataStore
) {
    suspend fun seedIfNeeded() {
        val previous = preferences.contentSeededVersion()
        if (previous == CONTENT_VERSION) return
        val legacyMap = if (previous != null && previous < FIRST_REBUILT_VERSION) loadLegacyMap(previous) else null
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val migration = database.legacyMigrationDao()
                val oldProgress = if (legacyMap != null) migration.getAllProgress() else emptyList()
                val oldAttempts = if (legacyMap != null) migration.getAllAttempts() else emptyList()

                database.exerciseDao().deleteAll()
                database.lessonDao().deleteAll()
                database.sectionDao().deleteAll()
                database.chapterDao().deleteAll()
                database.wordFrequencyDao().deleteAll()

                val chapters = readAsset<ChaptersFile>("content/chapters.json").chapters
                database.chapterDao().insertAll(chapters)
                val sections = readAsset<SectionsFile>("content/sections.json").sections
                sections.chunked(250).forEach { database.sectionDao().insertAll(it) }
                val lessons = readAsset<LessonsFile>("content/lessons_vocabulary.json").lessons
                lessons.chunked(250).forEach { database.lessonDao().insertAll(it) }
                streamExercises { chunk -> database.exerciseDao().insertAll(chunk.map { it.toEntity() }) }
                val words = readAsset<WordFrequencyFile>("content/word_frequency.json").words
                words.chunked(500).forEach { database.wordFrequencyDao().insertAll(it) }

                if (legacyMap != null) {
                    val lessonWords = migration.getLessonWords().groupBy({ it.lessonId }, { it.wordId })
                    val result = LegacyProgressRemapper.remap(
                        oldProgress = oldProgress,
                        oldAttempts = oldAttempts,
                        map = legacyMap,
                        newLessonsInOrder = LegacyProgressRemapper.curriculumOrder(chapters, sections, lessons),
                        newLessonWords = lessonWords
                    )
                    migration.deleteAllProgress()
                    migration.insertProgress(result.progress)
                    migration.deleteAllAttempts()
                    result.attempts.chunked(500).forEach { migration.insertAttempts(it) }
                }
            }
        }
        if (legacyMap != null) preferences.resetAllTestCoverage()
        preferences.setContentSeeded(CONTENT_VERSION)
    }

    /** Decodes the ~70 MB exercise array one element at a time so peak memory stays at one
     * chunk, not the whole file. */
    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun streamExercises(insert: suspend (List<ExerciseSeedDto>) -> Unit) {
        context.assets.open("content/exercises_vocabulary.json").use { stream ->
            val batch = ArrayList<ExerciseSeedDto>(EXERCISE_CHUNK)
            for (element in AppJson.decodeToSequence(stream, JsonElement.serializer(), DecodeSequenceMode.ARRAY_WRAPPED)) {
                // Per-element decode: an exercise of an unknown/withdrawn type is skipped rather
                // than aborting the whole seed.
                val dto: ExerciseSeedDto? = try {
                    AppJson.decodeFromJsonElement(ExerciseSeedDto.serializer(), element)
                } catch (_: IllegalArgumentException) {
                    null
                }
                if (dto == null) continue
                batch += dto
                if (batch.size == EXERCISE_CHUNK) {
                    insert(batch.toList())
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) insert(batch.toList())
        }
    }

    private fun loadLegacyMap(previousVersion: Int): LegacyContentMap? = runCatching {
        val all = readAsset<Map<String, LegacyContentMap>>("content/legacy_progress_map.json")
        all[previousVersion.toString()] ?: all[LATEST_LEGACY_VERSION.toString()]
    }.getOrNull()

    @OptIn(ExperimentalSerializationApi::class)
    private inline fun <reified T> readAsset(assetPath: String): T =
        context.assets.open(assetPath).use { AppJson.decodeFromStream(it) }

    companion object {
        /** Bump whenever tools/pipeline output changes. */
        const val CONTENT_VERSION = 36
        /** First content version built by tools/pipeline (new word/lesson ids). */
        const val FIRST_REBUILT_VERSION = 36
        private const val LATEST_LEGACY_VERSION = 35
        private const val EXERCISE_CHUNK = 400
    }
}
