package com.quranicwords.app.core.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable

/**
 * One row per user per calendar day, accumulating minutes practiced that day - needed because a
 * Review session (see [com.quranicwords.app.core.data.repository.ProgressRepositoryImpl.completeReviewSession])
 * never writes a [UserProgressEntity] row, so per-lesson duration alone can't answer "how many
 * minutes did the user practice today" on a Review-only day. [localDate] is an ISO-8601 date
 * string (`yyyy-MM-dd`), not an epoch millis timestamp, since this row aggregates a whole
 * calendar day rather than a single instant. [Serializable] for
 * [com.quranicwords.app.core.domain.repository.BackupRepository]'s JSON export/import, same
 * discipline as every other progress table.
 */
@Serializable
@Entity(tableName = "daily_practice", primaryKeys = ["userId", "localDate"])
data class DailyPracticeEntity(
    val userId: String,
    val localDate: String,
    val minutesPracticed: Int
)
