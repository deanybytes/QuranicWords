package com.quranicwords.app.core.domain

import com.quranicwords.app.core.data.local.entity.DailyQuestEntity
import kotlin.random.Random

/** What a quest measures. [accumulates] = summed across sessions; otherwise the best value
 * seen today counts (a combo, today's practice minutes). */
enum class QuestMetric(val accumulates: Boolean) {
    REVIEW_WORDS(true),
    COMBO(false),
    FINISH_LESSONS(true),
    EARN_XP(true),
    DAILY_GOAL_MINUTES(false),
    NEW_WORDS(true)
}

data class QuestDef(val id: String, val metric: QuestMetric, val target: Int, val rewardXp: Int)

/** Whether [DailyQuestEntity.metric] names a metric this build still tracks. */
val DailyQuestEntity.hasKnownMetric: Boolean
    get() = QuestMetric.entries.any { it.name == metric }

/** Facts about the learner's day that decide which quests are fair to offer. */
data class QuestEligibility(
    val dueReviewCount: Int,
    /** False on the Test/Quiz-only path, which has no lessons to finish. */
    val lessonsAvailable: Boolean,
    val dailyGoalMinutes: Int
)

/** What one finished session contributes to today's quests. */
data class QuestEvent(
    val reviewedWords: Int = 0,
    val bestCombo: Int = 0,
    val lessonsFinished: Int = 0,
    val xpEarned: Int = 0,
    val todayMinutes: Int = 0,
    val newWords: Int = 0
) {
    fun valueFor(metric: QuestMetric): Int = when (metric) {
        QuestMetric.REVIEW_WORDS -> reviewedWords
        QuestMetric.COMBO -> bestCombo
        QuestMetric.FINISH_LESSONS -> lessonsFinished
        QuestMetric.EARN_XP -> xpEarned
        QuestMetric.DAILY_GOAL_MINUTES -> todayMinutes
        QuestMetric.NEW_WORDS -> newWords
    }
}

/** Result of applying a [QuestEvent]: the rows that changed and the XP their completion pays. */
data class QuestUpdate(val changed: List<DailyQuestEntity>, val rewardXp: Int, val completedQuestIds: List<String>)

/**
 * Daily quests: [QUESTS_PER_DAY] small goals per local day, chosen deterministically from
 * `hash(userId + date)` so the same learner always sees the same three for a given day (and a
 * re-pick after a crash is identical). A review quest is always included while words are due;
 * quests the learner couldn't possibly finish (lessons on the Test-only path, reviews with
 * nothing due) are never offered. A stored row whose metric is no longer known (the withdrawn
 * "listening" quest, or one written by a newer build) is neither advanced nor shown.
 */
object QuestCatalog {
    const val QUESTS_PER_DAY = 3
    private const val REVIEW_TARGET_CAP = 10

    const val REVIEW_ID = "review_due"

    fun pool(eligibility: QuestEligibility): List<QuestDef> = buildList {
        if (eligibility.dueReviewCount > 0) {
            add(QuestDef(REVIEW_ID, QuestMetric.REVIEW_WORDS, eligibility.dueReviewCount.coerceAtMost(REVIEW_TARGET_CAP), rewardXp = 20))
        }
        add(QuestDef("combo_10", QuestMetric.COMBO, 10, rewardXp = 25))
        if (eligibility.lessonsAvailable) add(QuestDef("finish_2_lessons", QuestMetric.FINISH_LESSONS, 2, rewardXp = 20))
        add(QuestDef("earn_50_xp", QuestMetric.EARN_XP, 50, rewardXp = 15))
        if (eligibility.dailyGoalMinutes > 0) {
            add(QuestDef("daily_goal", QuestMetric.DAILY_GOAL_MINUTES, eligibility.dailyGoalMinutes, rewardXp = 20))
        }
        add(QuestDef("new_words_5", QuestMetric.NEW_WORDS, 5, rewardXp = 20))
    }

    fun questsFor(userId: String, localDate: String, eligibility: QuestEligibility): List<QuestDef> {
        val pool = pool(eligibility)
        val random = Random("$userId|$localDate".hashCode())
        val review = pool.firstOrNull { it.id == REVIEW_ID }
        val others = pool.filter { it.id != REVIEW_ID }.shuffled(random)
        return (listOfNotNull(review) + others).take(QUESTS_PER_DAY)
    }

    fun toEntities(userId: String, localDate: String, quests: List<QuestDef>): List<DailyQuestEntity> =
        quests.map { DailyQuestEntity(userId, localDate, it.id, it.metric.name, it.target, 0, it.rewardXp, null) }

    /**
     * Advances [quests] by [event]. A quest's reward is paid exactly once: only on the update that
     * first reaches its target, after which `completedAtEpochMillis` is set and the row is never
     * advanced again. Rows with an unknown metric (written by a newer build) are left alone.
     */
    fun apply(quests: List<DailyQuestEntity>, event: QuestEvent, nowMillis: Long): QuestUpdate {
        val changed = mutableListOf<DailyQuestEntity>()
        val completed = mutableListOf<String>()
        var reward = 0
        for (quest in quests) {
            if (quest.completedAtEpochMillis != null) continue
            val metric = QuestMetric.entries.find { it.name == quest.metric } ?: continue
            val value = event.valueFor(metric)
            val progress = if (metric.accumulates) quest.progress + value else maxOf(quest.progress, value)
            val capped = progress.coerceAtMost(quest.target)
            if (capped == quest.progress) continue
            val done = capped >= quest.target
            changed += quest.copy(progress = capped, completedAtEpochMillis = if (done) nowMillis else null)
            if (done) {
                reward += quest.rewardXp
                completed += quest.questId
            }
        }
        return QuestUpdate(changed, reward, completed)
    }
}
