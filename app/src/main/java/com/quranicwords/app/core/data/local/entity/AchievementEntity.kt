package com.quranicwords.app.core.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable

/**
 * One row per achievement a user has unlocked - [achievementId] is a plain string key into
 * [com.quranicwords.app.core.domain.AchievementCatalog] (code-side, not a Room enum/FK), so
 * adding a new achievement to the catalog never needs a schema change. [Serializable] for
 * [com.quranicwords.app.core.domain.repository.BackupRepository]'s JSON export/import, same
 * discipline as [UserStatsEntity]/[UserProgressEntity] - the entity itself round-trips directly,
 * no parallel DTO.
 */
@Serializable
@Entity(tableName = "achievements", primaryKeys = ["userId", "achievementId"])
data class AchievementEntity(
    val userId: String,
    val achievementId: String,
    val unlockedAtEpochMillis: Long
)
