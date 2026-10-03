package com.quranicwords.app.core.domain.srs

import com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity
import com.quranicwords.app.core.data.local.entity.WordMemoryEntity
import java.time.Instant
import java.time.ZoneId
import kotlin.math.min

/**
 * The storage-facing rules around [FsrsScheduler] - pure, so the live attempt path
 * (`ProgressRepositoryImpl.logAttempt`) and the one-time history backfill share exactly one
 * implementation and both are unit-testable without Room.
 */
object WordMemoryRules {

    /** Upgraded learners' replayed memory is capped this low so every word they knew comes back
     * for a real review soon, rather than trusting a schedule the app never actually ran. */
    const val BACKFILL_MAX_STABILITY_DAYS = 3.0

    /** Backfilled due dates are spread over this many days so an upgrade doesn't land every
     * known word in today's review pile at once. */
    const val BACKFILL_SPREAD_DAYS = 7

    /**
     * Applies one first-try answer for [itemId] answered at [nowMillis] on local day [localDate].
     *
     * The scheduler runs at most once per word per local day: a later first try on the same day
     * (a second lesson, a review session) only records [WordMemoryEntity.lastGrade], so a burst of
     * same-evening repetition can't inflate stability. One exception keeps the due list honest -
     * a correct same-day answer to a word that is already due (a lapse waiting out its ten-minute
     * relearning step) pushes it to tomorrow instead of leaving it "due" all day. A [localDate]
     * earlier than the stored one means the clock went backwards; that is treated as the same day.
     */
    fun applyFirstTry(
        existing: WordMemoryEntity?,
        userId: String,
        itemId: String,
        correct: Boolean,
        nowMillis: Long,
        localDate: String,
        scheduler: FsrsScheduler
    ): WordMemoryEntity {
        val grade = ReviewGrade.of(correct)
        val lastDate = existing?.lastReviewLocalDate
        if (existing != null && lastDate != null && localDate <= lastDate) {
            val deferredDue = if (correct && existing.dueAtEpochMillis <= nowMillis) {
                nowMillis + FsrsScheduler.LEARNING_STEP_MILLIS
            } else {
                existing.dueAtEpochMillis
            }
            return existing.copy(lastGrade = grade.value, dueAtEpochMillis = deferredDue)
        }
        val card = scheduler.review(existing?.toCard() ?: MemoryCard(), grade, nowMillis)
        return WordMemoryEntity.from(userId, itemId, card, grade.value, localDate)
    }

    /**
     * Rebuilds memory rows for a learner who predates `word_memory`: each word's first-try
     * attempts are replayed in order through [applyFirstTry] (so only the first attempt of each
     * local day reaches the scheduler), then stability is capped at [BACKFILL_MAX_STABILITY_DAYS]
     * and the due date is spread across the next [BACKFILL_SPREAD_DAYS] days by a stable hash of
     * the word id - deterministic, so a retried backfill produces identical rows.
     */
    fun backfill(
        userId: String,
        attempts: List<ExerciseAttemptEntity>,
        zone: ZoneId,
        nowMillis: Long,
        scheduler: FsrsScheduler
    ): List<WordMemoryEntity> =
        attempts
            .filter { it.isFirstTry }
            .groupBy { it.itemId }
            .toSortedMap()
            .map { (itemId, rows) ->
                var memory: WordMemoryEntity? = null
                rows.sortedWith(compareBy({ it.attemptedAtEpochMillis }, { it.id })).forEach { attempt ->
                    val day = Instant.ofEpochMilli(attempt.attemptedAtEpochMillis).atZone(zone).toLocalDate().toString()
                    memory = applyFirstTry(memory, userId, itemId, attempt.wasCorrect, attempt.attemptedAtEpochMillis, day, scheduler)
                }
                val replayed = checkNotNull(memory)
                replayed.copy(
                    stability = min(replayed.stability, BACKFILL_MAX_STABILITY_DAYS),
                    dueAtEpochMillis = nowMillis + spreadDays(itemId) * FsrsScheduler.DAY_MILLIS
                )
            }

    /** 0 until [BACKFILL_SPREAD_DAYS] - String.hashCode is specified by the JLS, so this is stable
     * across devices and app versions. */
    fun spreadDays(itemId: String): Int = Math.floorMod(itemId.hashCode(), BACKFILL_SPREAD_DAYS)
}
