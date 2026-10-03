package com.quranicwords.app.feature.widget

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.util.LruCache
import androidx.sqlite.db.SimpleSQLiteQuery
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.local.Converters
import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.data.local.dao.ItemStability
import com.quranicwords.app.core.di.SystemZoneClock
import com.quranicwords.app.core.domain.DisplayedStreak
import com.quranicwords.app.core.domain.HeartsCalculator
import com.quranicwords.app.core.domain.LevelCurve
import com.quranicwords.app.core.domain.hasKnownMetric
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.LemmaCategory
import com.quranicwords.app.core.domain.model.LocalizedText
import com.quranicwords.app.core.domain.model.ThemeMode
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.domain.model.getOrNull
import com.quranicwords.app.core.domain.srs.WordStrength
import com.quranicwords.app.core.util.decodeExerciseContentOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate

/** [this] with [language]'s locale and, for a forced [themeMode], that night mode - so strings
 * resolve in the app's language (not the launcher's) and colours in the chosen theme. */
fun Context.getThemedAndLocalizedWidgetContext(language: Language, themeMode: ThemeMode): Context {
    val config = Configuration(resources.configuration)
    config.setLocale(language.locale)
    config.setLayoutDirection(language.locale)
    val night = when (themeMode) {
        ThemeMode.DARK -> Configuration.UI_MODE_NIGHT_YES
        ThemeMode.LIGHT -> Configuration.UI_MODE_NIGHT_NO
        ThemeMode.SYSTEM -> null
    }
    if (night != null) {
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
    }
    return createConfigurationContext(config)
}

/**
 * Reads what the widgets show. Kept deliberately cheap - widgets refresh on every session end,
 * every half hour and on date/locale changes - so it never decodes the whole curriculum: the
 * snapshot is a handful of indexed queries (counts, the due list, stabilities, today's rows) and
 * only the one word on screen has its content looked up, through a small in-memory cache.
 */
object WidgetDataProvider {
    private const val TAG = "WidgetData"
    private const val DUE_POOL_LIMIT = 50

    /** Widgets aren't Hilt-injected, so this mirrors ClockModule's zone-following clock. */
    private val clock: Clock = SystemZoneClock()

    private val converters = Converters()

    /** "wordId|language" -> display data, dropped whenever the vocabulary size changes (a reseed). */
    private val wordCache = LruCache<String, WidgetWord>(32)

    @Volatile
    private var cachedVocabularySize = -1

    suspend fun loadSnapshot(context: Context): WidgetSnapshot = withContext(Dispatchers.IO) {
        val prefs = UserPreferencesDataStore(context.applicationContext)
        val environment = try {
            WidgetEnvironment(
                language = prefs.languageFlow.first() ?: Language.ENGLISH,
                themeMode = prefs.themeModeFlow.first(),
                fontStyle = prefs.fontStyleFlow.first()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Preferences unavailable", e)
            WidgetEnvironment()
        }
        try {
            load(context, prefs, environment)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A widget must never crash the launcher's view of the app: a database that isn't
            // ready (first launch still seeding, a migration mid-flight) shows the friendly
            // "almost ready" state instead, and the next refresh tries again.
            Log.w(TAG, "Widget data unavailable", e)
            WidgetSnapshot.notReady(environment)
        }
    }

    private suspend fun load(context: Context, prefs: UserPreferencesDataStore, environment: WidgetEnvironment): WidgetSnapshot {
        val db = QwDatabase.getInstance(context.applicationContext)
        val vocabularySize = db.wordFrequencyDao().count()
        if (vocabularySize == 0) return WidgetSnapshot.notReady(environment)
        if (vocabularySize != cachedVocabularySize) {
            wordCache.evictAll()
            cachedVocabularySize = vocabularySize
        }

        val userId = prefs.getOrCreateLocalUserId()
        val nowMillis = clock.millis()
        val today = LocalDate.now(clock)
        val memoryDao = db.wordMemoryDao()

        val statsEntity = db.userStatsDao().get(userId)
        val todayMinutes = db.dailyPracticeDao().get(userId, today.toString())?.minutesPracticed ?: 0
        val goalMinutes = prefs.dailyGoalLevelFlow.first().minutes
        val dueCount = memoryDao.getDueCount(userId, nowMillis)
        val dueIds = memoryDao.getDueItemIds(userId, nowMillis, DUE_POOL_LIMIT)
        val stabilities = memoryDao.getStabilities(userId)
        val dueSet = dueIds.toSet()
        val learned = stabilities
            .filter { it.itemId !in dueSet }
            .sortedWith(compareByDescending<ItemStability> { it.stability }.thenBy { it.itemId })
            .map { it.itemId }
        val nextNew = if (dueIds.isEmpty() && learned.isEmpty()) nextWordToLearn(db, userId) else null

        val quests = db.dailyQuestDao().getForDay(userId, today.toString()).filter { it.hasKnownMetric }
        val hearts = statsEntity
            ?.takeIf { it.heartsEnabled }
            ?.let { HeartsCalculator.current(it.hearts, it.heartsUpdatedAtEpochMillis, nowMillis).hearts }

        val stats = WidgetStats(
            streakDays = DisplayedStreak.of(statsEntity, today),
            practicedToday = DisplayedStreak.practicedToday(statsEntity, today),
            todayMinutes = todayMinutes,
            goalMinutes = goalMinutes,
            dueCount = dueCount,
            learnedCount = stabilities.count { it.stability >= WordStrength.LEARNED_MIN_DAYS },
            level = LevelCurve.progressFor(statsEntity?.totalPoints ?: 0),
            hearts = hearts,
            maxHearts = HeartsCalculator.MAX_HEARTS,
            questsDone = quests.count { it.completedAtEpochMillis != null },
            questsTotal = quests.size
        )
        return WidgetSnapshot(
            environment = environment,
            contentReady = true,
            stats = stats,
            pools = WordPools(due = dueIds, learned = learned, nextNew = nextNew),
            strengths = stabilities.associate { it.itemId to WordStrength.fromStability(it.stability) }
        )
    }

    /**
     * The word for one widget instance at rotation [step]. If the picked id has no content any
     * more (a word dropped by a newer content build), the next ones in the same pool are tried
     * before giving up.
     */
    suspend fun wordFor(context: Context, snapshot: WidgetSnapshot, step: Int): WidgetWord? = withContext(Dispatchers.IO) {
        if (!snapshot.contentReady) return@withContext null
        try {
            val db = QwDatabase.getInstance(context.applicationContext)
            repeat(MAX_PICK_ATTEMPTS) { attempt ->
                val pick = WidgetWordPicker.pick(snapshot.pools, step + attempt) ?: return@withContext null
                val strength = snapshot.strengths[pick.wordId] ?: WordStrength.NEW
                val word = details(db, pick, strength, snapshot.environment.language)
                if (word != null) return@withContext word
            }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Word lookup failed", e)
            null
        }
    }

    private const val MAX_PICK_ATTEMPTS = 3

    private fun details(db: QwDatabase, pick: WordPick, strength: WordStrength, language: Language): WidgetWord? {
        val key = "${pick.wordId}|${language.tag}"
        wordCache.get(key)?.let { return it.copy(kind = pick.kind, strength = strength) }

        val row = db.query(
            SimpleSQLiteQuery(
                "SELECT arabicWord, frequencyRank, frequencyCount, meaning FROM word_frequency WHERE id = ? LIMIT 1",
                arrayOf(pick.wordId)
            )
        ).use { c ->
            if (!c.moveToFirst()) return null
            FrequencyRow(c.getString(0), c.getInt(1), c.getInt(2), converters.toLocalizedText(c.getString(3)))
        }
        val intro = introFor(db, pick.wordId)
        val word = WidgetWord(
            id = pick.wordId,
            arabic = intro?.arabic?.takeIf { it.isNotBlank() } ?: row.arabic,
            meaning = (intro?.meaning?.takeIf { it.isNotEmpty() } ?: row.meaning).get(language),
            kind = pick.kind,
            strength = strength,
            category = intro?.category,
            occurrences = row.count,
            rank = row.rank,
            example = intro?.example?.let { ex ->
                WidgetExample(arabic = ex.arabic, translation = ex.translation.getOrNull(language), reference = ex.reference)
            }
        )
        wordCache.put(key, word)
        return word
    }

    private data class FrequencyRow(val arabic: String, val rank: Int, val count: Int, val meaning: LocalizedText)

    private data class IntroExample(val arabic: String, val translation: LocalizedText, val reference: String?)
    private data class IntroDetails(val arabic: String, val meaning: LocalizedText, val category: LemmaCategory, val example: IntroExample?)

    /**
     * The word's WORD_INTRO teach step, found without decoding the curriculum: content JSON is
     * written compactly by AppJson, so a LIKE on its `"wordId":"…"` pair narrows the scan to the
     * candidates, and decoding then confirms the exact id (`_` in ids is a LIKE wildcard).
     *
     * This is the widgets' only reader of [ExerciseContent] - if the way examples are stored
     * changes, this mapping is the one place to follow it.
     */
    private fun introFor(db: QwDatabase, wordId: String): IntroDetails? {
        val pattern = "%\"wordId\":\"$wordId\"%"
        db.query(
            SimpleSQLiteQuery(
                "SELECT contentJson FROM exercises WHERE (type = 'WORD_INTRO' OR type = 'TEACH_WORD') AND contentJson LIKE ? LIMIT 8",
                arrayOf(pattern)
            )
        ).use { c ->
            while (c.moveToNext()) {
                val intro = decodeExerciseContentOrNull(c.getString(0)) as? ExerciseContent.WordIntro ?: continue
                if (intro.wordId != wordId) continue
                val example = intro.exampleVerseArabic?.takeIf { it.isNotBlank() }?.let {
                    IntroExample(it, intro.exampleVerseTranslation, intro.exampleVerseReference)
                }
                return IntroDetails(intro.arabicWord, intro.meaning, intro.lemmaCategory, example)
            }
        }
        return null
    }

    /**
     * For a learner with nothing practised yet: the first word of the curriculum (chapter,
     * section, lesson, then exercise order) they have no memory of. Falls back to the most
     * frequent such word if the curriculum tables are empty or shaped unexpectedly.
     */
    private fun nextWordToLearn(db: QwDatabase, userId: String): String? {
        val curriculumOrder = runCatching {
            db.query(
                SimpleSQLiteQuery(
                    """
                    SELECT e.practicedItemId FROM exercises e
                    JOIN lessons l ON l.id = e.lessonId
                    JOIN chapters c ON c.id = l.chapterId
                    LEFT JOIN sections s ON s.id = l.sectionId
                    JOIN word_frequency w ON w.id = e.practicedItemId
                    WHERE l.kind = 'REGULAR'
                      AND e.practicedItemId NOT IN (SELECT itemId FROM word_memory WHERE userId = ?)
                    ORDER BY c.sortOrder, COALESCE(s.sortOrder, 0), l.sortOrder, e.orderIndex
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(userId)
                )
            ).use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.onFailure { Log.w(TAG, "Curriculum order lookup failed", it) }.getOrNull()
        if (curriculumOrder != null) return curriculumOrder
        return db.query(
            SimpleSQLiteQuery(
                "SELECT id FROM word_frequency WHERE id NOT IN (SELECT itemId FROM word_memory WHERE userId = ?) ORDER BY frequencyRank LIMIT 1",
                arrayOf(userId)
            )
        ).use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }
}
