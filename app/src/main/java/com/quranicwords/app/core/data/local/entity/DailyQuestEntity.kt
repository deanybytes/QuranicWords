package com.quranicwords.app.core.data.local.entity

import androidx.room.Entity

/**
 * One of the (usually three) quests chosen for a learner's local day - see
 * [com.quranicwords.app.core.domain.QuestCatalog]. Rows are inserted lazily the first time a day's
 * quests are needed (INSERT OR IGNORE, so a re-pick never overwrites progress) and advanced from
 * session events in the same transaction that records the session. [metric] is a
 * [com.quranicwords.app.core.domain.QuestMetric] name; [completedAtEpochMillis] doubles as the
 * "reward already paid" flag, so a quest's XP lands exactly once.
 */
@Entity(tableName = "daily_quests", primaryKeys = ["userId", "localDate", "questId"])
data class DailyQuestEntity(
    val userId: String,
    /** ISO `yyyy-MM-dd` local calendar day these quests belong to. */
    val localDate: String,
    val questId: String,
    val metric: String,
    val target: Int,
    val progress: Int,
    val rewardXp: Int,
    val completedAtEpochMillis: Long?
)
