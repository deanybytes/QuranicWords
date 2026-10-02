package com.quranicwords.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** [Serializable] for [com.quranicwords.app.core.domain.repository.BackupRepository]'s
 * JSON export/import - see [UserProgressEntity] for why this is the entity directly, not a DTO.
 *
 * The v7 gamification columns all default (in SQL and in Kotlin) so pre-v7 rows and older backup
 * files decode unchanged. [heartsEnabled] defaults off for exactly that reason - learners who
 * upgrade keep the rules they signed up for; a brand-new learner's first row is created with it
 * on (see `ProgressRepositoryImpl.ensureCurriculumStarted`). */
@Serializable
@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val userId: String,
    /** Also the learner's XP - levels are derived from it (see LevelCurve). */
    val totalPoints: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    /** ISO "yyyy-MM-dd" local calendar date, not a UTC epoch - see StreakCalculator. */
    val lastActivityLocalDate: String?,
    /** Stored heart count as of [heartsUpdatedAtEpochMillis]; regeneration since then is derived
     * on read by HeartsCalculator rather than written on a timer. */
    @ColumnInfo(defaultValue = "5") val hearts: Int = 5,
    @ColumnInfo(defaultValue = "0") val heartsUpdatedAtEpochMillis: Long = 0L,
    @ColumnInfo(defaultValue = "0") val bestCombo: Int = 0,
    @ColumnInfo(defaultValue = "0") val heartsEnabled: Boolean = false
)
