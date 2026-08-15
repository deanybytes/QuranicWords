package com.quranicwords.app.core.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Serializable
enum class LessonStatus { LOCKED, UNLOCKED, COMPLETED }

/** [Serializable] so this can round-trip through [com.quranicwords.app.core.domain.repository.BackupRepository]'s
 * JSON export/import - reused directly rather than a parallel DTO, matching this project's
 * pattern of domain repositories returning Room entities directly (see [com.quranicwords.app.core.domain.repository.ProgressRepository]). */
@Serializable
@Entity(tableName = "user_progress", primaryKeys = ["userId", "lessonId"])
data class UserProgressEntity(
    val userId: String,
    val lessonId: String,
    val status: LessonStatus,
    val bestScorePercent: Int,
    val completedAtEpochMillis: Long?
)
