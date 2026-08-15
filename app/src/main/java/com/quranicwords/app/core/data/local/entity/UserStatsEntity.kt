package com.quranicwords.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** [Serializable] for [com.quranicwords.app.core.domain.repository.BackupRepository]'s
 * JSON export/import - see [UserProgressEntity] for why this is the entity directly, not a DTO. */
@Serializable
@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val userId: String,
    val totalPoints: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    /** ISO "yyyy-MM-dd" local calendar date, not a UTC epoch - see StreakCalculator. */
    val lastActivityLocalDate: String?
)
