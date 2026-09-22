package com.quranicwords.app.feature.widget

import android.content.Context
import androidx.core.content.edit
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.data.local.entity.WordFrequencyEntity
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.util.AppJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class WidgetStatsData(
    val streakDays: Int,
    val isStreakActive: Boolean,
    val wordsLearnedCount: Int,
    val wordsLearnedPct: Float,
    val accuracyPct: Int,
    val todayPracticeMinutes: Int,
    val dailyGoalMinutes: Int,
    val dailyGoalProgressPct: Int,
    val reviewCount: Int
)

data class WidgetWordData(
    val wordId: String,
    val arabicWord: String,
    val meaning: String,
    val occurrenceCount: Int,
    val quranPercentage: Double,
    val frequencyRank: Int,
    val exampleVerseArabic: String?,
    val exampleVerseTranslation: String?,
    val exampleVerseReference: String?,
    val isMistaken: Boolean
)

data class WidgetSnapshot(
    val stats: WidgetStatsData,
    val currentWord: WidgetWordData?
)

object WidgetDataProvider {
    private const val PREFS_WIDGET_STATE = "quranic_words_widget_state"
    private const val KEY_ROTATION_STEP = "widget_rotation_step"
    private const val TOTAL_QURAN_WORDS = 77797.0
    private const val TOTAL_VOCABULARY_TARGET = 4709.0

    suspend fun getWidgetData(context: Context, advanceRotation: Boolean = false): WidgetSnapshot = withContext(Dispatchers.IO) {
        val database = QwDatabase.getInstance(context)
        val prefs = UserPreferencesDataStore(context)
        val userId = prefs.getOrCreateLocalUserId()
        val language = prefs.languageFlow.first() ?: Language.ENGLISH

        // 1. Compute Stats
        val statsEntity = database.userStatsDao().get(userId)
        val streak = statsEntity?.currentStreak ?: 0
        val isStreakActive = streak > 0

        val masteredIds = database.exerciseAttemptDao().getMasteredItemIds(userId)
        val missedIds = database.exerciseAttemptDao().getMissedItemIds(userId)
        val allPracticedIds = database.exerciseAttemptDao().getAllPracticedItemIds(userId)

        val wordsLearnedCount = masteredIds.size
        val wordsLearnedPct = ((wordsLearnedCount / TOTAL_VOCABULARY_TARGET) * 100).toFloat().coerceIn(0f, 100f)

        val recentAttempts = database.exerciseAttemptDao().getAllForUser(userId)
        val accuracyPct = if (recentAttempts.isNotEmpty()) {
            val correct = recentAttempts.count { it.wasCorrect }
            ((correct.toDouble() / recentAttempts.size) * 100).toInt().coerceIn(0, 100)
        } else {
            100
        }

        val todayStr = LocalDate.now().toString()
        val dailyPractice = database.dailyPracticeDao().get(userId, todayStr)
        val todayPracticeMinutes = dailyPractice?.minutesPracticed ?: 0
        val goalLevel = prefs.dailyGoalLevelFlow.first()
        val dailyGoalMinutes = goalLevel.minutes
        val dailyGoalProgressPct = if (dailyGoalMinutes > 0) {
            ((todayPracticeMinutes.toDouble() / dailyGoalMinutes) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val reviewCount = missedIds.size

        val stats = WidgetStatsData(
            streakDays = streak,
            isStreakActive = isStreakActive,
            wordsLearnedCount = wordsLearnedCount,
            wordsLearnedPct = wordsLearnedPct,
            accuracyPct = accuracyPct,
            todayPracticeMinutes = todayPracticeMinutes,
            dailyGoalMinutes = dailyGoalMinutes,
            dailyGoalProgressPct = dailyGoalProgressPct,
            reviewCount = reviewCount
        )

        // 2. Select Word with 2:1 Mistaken vs Learned Rotation (never show unlearned words)
        val validPracticedWords = allPracticedIds.toSet()
        val validMissedWords = missedIds.filter { it in validPracticedWords }
        val validMasteredWords = masteredIds.filter { it in validPracticedWords }

        val wordData = if (validPracticedWords.isEmpty()) {
            null
        } else {
            val sp = context.getSharedPreferences(PREFS_WIDGET_STATE, Context.MODE_PRIVATE)
            var currentStep = sp.getInt(KEY_ROTATION_STEP, 0)
            if (advanceRotation) {
                currentStep = (currentStep + 1) % 1000
                sp.edit { putInt(KEY_ROTATION_STEP, currentStep) }
            }

            // 2:1 ratio logic:
            // step % 3 == 0 -> Missed word
            // step % 3 == 1 -> Missed word
            // step % 3 == 2 -> Mastered word
            val isMistakeTurn = (currentStep % 3 != 2)
            val targetId = if (isMistakeTurn) {
                if (validMissedWords.isNotEmpty()) {
                    val index = ((currentStep / 3) * 2 + (currentStep % 3)) % validMissedWords.size
                    validMissedWords[index]
                } else if (validMasteredWords.isNotEmpty()) {
                    validMasteredWords[currentStep % validMasteredWords.size]
                } else {
                    validPracticedWords.elementAt(currentStep % validPracticedWords.size)
                }
            } else {
                if (validMasteredWords.isNotEmpty()) {
                    val index = (currentStep / 3) % validMasteredWords.size
                    validMasteredWords[index]
                } else if (validMissedWords.isNotEmpty()) {
                    validMissedWords[currentStep % validMissedWords.size]
                } else {
                    validPracticedWords.elementAt(currentStep % validPracticedWords.size)
                }
            }

            val isMistaken = targetId in validMissedWords

            // Load WordIntro from exerciseDao
            val teachExercises = database.exerciseDao().getAllTeachWords()
            val introMap = teachExercises.mapNotNull {
                runCatching {
                    AppJson.decodeFromString(
                        ExerciseContent.serializer(),
                        it.contentJson
                    ) as? ExerciseContent.WordIntro
                }.getOrNull()
            }.associateBy { it.wordId }

            val intro = introMap[targetId]

            // Load WordFrequency entity
            val freqList = database.wordFrequencyDao().observeAllByFrequency().first()
            val freq = freqList.find { it.id == targetId }

            if (intro != null) {
                val arabic = intro.arabicWord
                val meaning = intro.meaning.get(language)
                val count = freq?.frequencyCount ?: 1
                val rank = freq?.frequencyRank ?: 1
                val quranPct = (count / TOTAL_QURAN_WORDS) * 100.0

                WidgetWordData(
                    wordId = targetId,
                    arabicWord = arabic,
                    meaning = meaning,
                    occurrenceCount = count,
                    quranPercentage = quranPct,
                    frequencyRank = rank,
                    exampleVerseArabic = intro.exampleVerseArabic,
                    exampleVerseTranslation = intro.exampleVerseTranslation.get(language),
                    exampleVerseReference = com.quranicwords.app.core.util.VerseReferenceFormatter.format(intro.exampleVerseReference, language),
                    isMistaken = isMistaken
                )
            } else if (freq != null) {
                WidgetWordData(
                    wordId = targetId,
                    arabicWord = freq.arabicWord,
                    meaning = freq.meaning.get(language),
                    occurrenceCount = freq.frequencyCount,
                    quranPercentage = (freq.frequencyCount / TOTAL_QURAN_WORDS) * 100.0,
                    frequencyRank = freq.frequencyRank,
                    exampleVerseArabic = null,
                    exampleVerseTranslation = null,
                    exampleVerseReference = null,
                    isMistaken = isMistaken
                )
            } else {
                null
            }
        }

        WidgetSnapshot(stats = stats, currentWord = wordData)
    }
}
