package com.quranicwords.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import com.quranicwords.app.core.domain.srs.MemoryCard
import com.quranicwords.app.core.domain.srs.MemoryState
import kotlinx.serialization.Serializable

/**
 * Per-word spaced-repetition memory (FSRS state, see
 * [com.quranicwords.app.core.domain.srs.FsrsScheduler]) - one row per word a learner has given a
 * first-try answer to. Learner progress data like [ExerciseAttemptEntity], never touched by the
 * content reseed. [lastReviewLocalDate] (ISO `yyyy-MM-dd`) is the local day the scheduler last ran
 * for this word: later first tries on that same day only update [lastGrade], so drilling a word
 * five times in one evening can't masquerade as five days of spaced recall.
 *
 * [Serializable] for backup export/import, same discipline as every other progress table.
 */
@Serializable
@Entity(
    tableName = "word_memory",
    primaryKeys = ["userId", "itemId"],
    indices = [
        Index(value = ["userId", "dueAtEpochMillis"]),
        Index(value = ["userId", "lastGrade"])
    ]
)
data class WordMemoryEntity(
    val userId: String,
    val itemId: String,
    val state: MemoryState,
    val stability: Double,
    val difficulty: Double,
    val dueAtEpochMillis: Long,
    val reps: Int,
    val lapses: Int,
    /** FSRS rating of the most recent first try (1 = Again, 3 = Good) - drives the mistakes list. */
    val lastGrade: Int,
    val lastReviewedAtEpochMillis: Long?,
    val lastReviewLocalDate: String?
) {
    fun toCard(): MemoryCard = MemoryCard(
        state = state,
        stability = stability,
        difficulty = difficulty,
        dueAtEpochMillis = dueAtEpochMillis,
        reps = reps,
        lapses = lapses,
        lastReviewedAtEpochMillis = lastReviewedAtEpochMillis
    )

    companion object {
        fun from(
            userId: String,
            itemId: String,
            card: MemoryCard,
            lastGrade: Int,
            lastReviewLocalDate: String?
        ): WordMemoryEntity = WordMemoryEntity(
            userId = userId,
            itemId = itemId,
            state = card.state,
            stability = card.stability,
            difficulty = card.difficulty,
            dueAtEpochMillis = card.dueAtEpochMillis,
            reps = card.reps,
            lapses = card.lapses,
            lastGrade = lastGrade,
            lastReviewedAtEpochMillis = card.lastReviewedAtEpochMillis,
            lastReviewLocalDate = lastReviewLocalDate
        )
    }
}
