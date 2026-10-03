package com.quranicwords.app.feature.widget

import com.quranicwords.app.core.domain.LevelProgress
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.LemmaCategory
import com.quranicwords.app.core.domain.model.QuranFontStyle
import com.quranicwords.app.core.domain.model.ThemeMode
import com.quranicwords.app.core.domain.srs.WordStrength

/** Why a word is on the widget - decides its badge, whether its meaning starts hidden, and where
 * a tap goes. */
enum class WordKind { DUE, LEARNED, NEW }

/** The three pools the "word of the moment" is drawn from, in priority order (see
 * [WidgetWordPicker]). Ids are `word_memory.itemId` / `word_frequency.id`. */
data class WordPools(
    /** Due by now, most overdue first (WordMemoryDao.getDueItemIds' order). */
    val due: List<String>,
    /** Practised words that are not due, strongest first. */
    val learned: List<String>,
    /** The curriculum's next not-yet-practised word, or null once there is none. */
    val nextNew: String?
) {
    val isEmpty: Boolean get() = due.isEmpty() && learned.isEmpty() && nextNew == null
}

data class WordPick(val wordId: String, val kind: WordKind)

/** One word's display data. [example] is the word's own verse context, never decoration. */
data class WidgetWord(
    val id: String,
    val arabic: String,
    val meaning: String,
    val kind: WordKind,
    val strength: WordStrength,
    val category: LemmaCategory?,
    val occurrences: Int,
    val rank: Int,
    val example: WidgetExample?
)

data class WidgetExample(val arabic: String, val translation: String?, val reference: String?)

data class WidgetStats(
    val streakDays: Int,
    val practicedToday: Boolean,
    val todayMinutes: Int,
    val goalMinutes: Int,
    val dueCount: Int,
    val learnedCount: Int,
    val level: LevelProgress,
    /** Null when the learner has hearts switched off. */
    val hearts: Int?,
    val maxHearts: Int,
    val questsDone: Int,
    val questsTotal: Int
) {
    val goalMet: Boolean get() = goalMinutes in 1..todayMinutes

    companion object {
        val EMPTY = WidgetStats(
            streakDays = 0, practicedToday = false, todayMinutes = 0, goalMinutes = 0, dueCount = 0,
            learnedCount = 0, level = LevelProgress(1, 0, 100), hearts = null, maxHearts = 5,
            questsDone = 0, questsTotal = 0
        )
    }
}

/** Learner settings that change how every widget renders. */
data class WidgetEnvironment(
    val language: Language = Language.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontStyle: QuranFontStyle = QuranFontStyle.DEFAULT
)

/** Everything one refresh needs, loaded once and shared by every widget instance. */
data class WidgetSnapshot(
    val environment: WidgetEnvironment,
    /** False until the bundled content has been seeded (first launch not finished yet). */
    val contentReady: Boolean,
    val stats: WidgetStats,
    val pools: WordPools,
    /** Memory strength of every practised word (absent = [WordStrength.NEW]). */
    val strengths: Map<String, WordStrength> = emptyMap()
) {
    companion object {
        fun notReady(environment: WidgetEnvironment = WidgetEnvironment()) =
            WidgetSnapshot(environment, contentReady = false, stats = WidgetStats.EMPTY, pools = WordPools(emptyList(), emptyList(), null))
    }
}

/** Which of the three widgets a RemoteViews is for. */
enum class WidgetKind { WORD, STATS, COMBINED }
