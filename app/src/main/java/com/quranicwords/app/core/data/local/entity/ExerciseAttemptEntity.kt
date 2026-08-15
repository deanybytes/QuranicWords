package com.quranicwords.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.ItemKind
import kotlinx.serialization.Serializable

/**
 * One row per scored check against a single letter/word - the per-item mastery signal
 * [UserProgressEntity] doesn't provide (that table only tracks pass/fail per whole lesson). Not
 * part of [com.quranicwords.app.core.data.assets.ContentSeeder]'s wipe-and-reseed
 * set: this is learner progress data, like `user_progress`/`user_stats`, not authored content.
 *
 * [Serializable] for [com.quranicwords.app.core.domain.repository.BackupRepository]'s
 * JSON export/import - see [UserProgressEntity] for why this is the entity directly, not a DTO.
 */
@Serializable
@Entity(
    tableName = "exercise_attempts",
    indices = [Index(value = ["userId", "itemId"])]
)
data class ExerciseAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val itemId: String,
    val itemKind: ItemKind,
    val exerciseType: ExerciseType,
    val wasCorrect: Boolean,
    val attemptedAtEpochMillis: Long
)
