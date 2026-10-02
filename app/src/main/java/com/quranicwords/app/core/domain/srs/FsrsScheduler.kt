package com.quranicwords.app.core.domain.srs

import kotlinx.serialization.Serializable
import java.time.Clock
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt

/** Where a card sits in the FSRS lifecycle. Stored by name in `word_memory.state`. */
@Serializable
enum class MemoryState { NEW, LEARNING, REVIEW, RELEARNING }

/**
 * Binary grading: the app only ever knows whether a first try was right, so the FSRS
 * Hard/Easy grades are never produced. [value] is the FSRS rating number the formulas use.
 */
enum class ReviewGrade(val value: Int) {
    AGAIN(1),
    GOOD(3);

    companion object {
        fun of(correct: Boolean): ReviewGrade = if (correct) GOOD else AGAIN
        fun fromValue(value: Int): ReviewGrade = if (value >= GOOD.value) GOOD else AGAIN
    }
}

/** The scheduler's view of one card - storage-agnostic so it stays a pure value. */
data class MemoryCard(
    val state: MemoryState = MemoryState.NEW,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val dueAtEpochMillis: Long = 0L,
    val reps: Int = 0,
    val lapses: Int = 0,
    val lastReviewedAtEpochMillis: Long? = null
)

/**
 * FSRS-4.5 ("Free Spaced Repetition Scheduler") with its published default weights, reduced to
 * the two grades this app can observe ([ReviewGrade]). Pure and deterministic: every method takes
 * the review instant explicitly, and the no-argument overloads read the injected [clock] - so the
 * same history always produces the same schedule, which the backfill in
 * `WordMemorySeeding` depends on.
 *
 * Lifecycle (see docs/ALGORITHMS.md):
 * - NEW + Good -> LEARNING, due in [LEARNING_STEP_MILLIS] (one day).
 * - NEW + Again, or any lapse -> LEARNING/RELEARNING, due in [RELEARNING_STEP_MILLIS] (ten minutes).
 * - LEARNING/RELEARNING + Good -> REVIEW, interval from stability.
 * - REVIEW + Good -> REVIEW with grown stability; REVIEW + Again -> RELEARNING, lapses + 1.
 *
 * Review intervals are whole days in `1..maxIntervalDays`, chosen so predicted recall at the due
 * moment equals [requestRetention].
 */
class FsrsScheduler(
    private val clock: Clock = Clock.systemUTC(),
    private val weights: DoubleArray = DEFAULT_WEIGHTS,
    private val requestRetention: Double = 0.9,
    private val maxIntervalDays: Int = 365
) {
    init {
        require(weights.size == DEFAULT_WEIGHTS.size) { "FSRS-4.5 needs ${DEFAULT_WEIGHTS.size} weights" }
    }

    fun review(card: MemoryCard, grade: ReviewGrade): MemoryCard = review(card, grade, clock.millis())

    fun review(card: MemoryCard, grade: ReviewGrade, nowMillis: Long): MemoryCard {
        if (card.state == MemoryState.NEW || card.stability <= 0.0) {
            val stability = initialStability(grade)
            return MemoryCard(
                state = MemoryState.LEARNING,
                stability = stability,
                difficulty = initialDifficulty(grade),
                dueAtEpochMillis = nowMillis + if (grade == ReviewGrade.GOOD) LEARNING_STEP_MILLIS else RELEARNING_STEP_MILLIS,
                reps = card.reps + 1,
                lapses = card.lapses,
                lastReviewedAtEpochMillis = nowMillis
            )
        }

        // A clock that went backwards must not produce a negative gap (or a "time travel" boost).
        val elapsedDays = card.lastReviewedAtEpochMillis
            ?.let { ((nowMillis - it).coerceAtLeast(0L)).toDouble() / DAY_MILLIS }
            ?: 0.0
        val recall = retrievability(elapsedDays, card.stability)
        val difficulty = nextDifficulty(card.difficulty, grade)

        return when (grade) {
            ReviewGrade.GOOD -> {
                val stability = successStability(card.stability, card.difficulty, recall)
                MemoryCard(
                    state = MemoryState.REVIEW,
                    stability = stability,
                    difficulty = difficulty,
                    dueAtEpochMillis = nowMillis + nextIntervalDays(stability) * DAY_MILLIS,
                    reps = card.reps + 1,
                    lapses = card.lapses,
                    lastReviewedAtEpochMillis = nowMillis
                )
            }
            ReviewGrade.AGAIN -> {
                val wasReview = card.state == MemoryState.REVIEW
                MemoryCard(
                    state = if (wasReview) MemoryState.RELEARNING else card.state,
                    stability = forgetStability(card.stability, card.difficulty, recall),
                    difficulty = difficulty,
                    dueAtEpochMillis = nowMillis + RELEARNING_STEP_MILLIS,
                    reps = card.reps + 1,
                    lapses = card.lapses + if (wasReview) 1 else 0,
                    lastReviewedAtEpochMillis = nowMillis
                )
            }
        }
    }

    /** Whole-day interval at which predicted recall decays to [requestRetention]. */
    fun nextIntervalDays(stability: Double): Int {
        val raw = stability / FACTOR * (requestRetention.pow(1.0 / DECAY) - 1.0)
        return raw.roundToInt().coerceIn(1, maxIntervalDays)
    }

    private fun initialStability(grade: ReviewGrade): Double =
        weights[grade.value - 1].coerceAtLeast(MIN_STABILITY)

    private fun initialDifficulty(grade: ReviewGrade): Double =
        (weights[4] - (grade.value - 3) * weights[5]).coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)

    /** Linear step by grade, then mean reversion towards the "Good" starting difficulty. */
    private fun nextDifficulty(difficulty: Double, grade: ReviewGrade): Double {
        val stepped = difficulty - weights[6] * (grade.value - 3)
        val reverted = weights[7] * weights[4] + (1 - weights[7]) * stepped
        return reverted.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
    }

    private fun successStability(stability: Double, difficulty: Double, recall: Double): Double {
        val growth = exp(weights[8]) *
            (11 - difficulty) *
            stability.pow(-weights[9]) *
            (exp((1 - recall) * weights[10]) - 1)
        return (stability * (1 + growth)).coerceAtLeast(MIN_STABILITY)
    }

    /** Post-lapse stability, never above what the card had before forgetting it. */
    private fun forgetStability(stability: Double, difficulty: Double, recall: Double): Double {
        val next = weights[11] *
            difficulty.pow(-weights[12]) *
            ((stability + 1).pow(weights[13]) - 1) *
            exp((1 - recall) * weights[14])
        return next.coerceIn(MIN_STABILITY, stability.coerceAtLeast(MIN_STABILITY))
    }

    companion object {
        /** FSRS-4.5 default parameters, as published by the open-spaced-repetition project. */
        val DEFAULT_WEIGHTS = doubleArrayOf(
            0.4872, 1.4003, 3.7145, 13.8206, 5.1618, 1.2298, 0.8975, 0.031, 1.6474,
            0.1367, 1.0461, 2.1072, 0.0793, 0.3246, 1.587, 0.2272, 2.8755
        )
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
        const val LEARNING_STEP_MILLIS = DAY_MILLIS
        const val RELEARNING_STEP_MILLIS = 10L * 60 * 1000
        private const val DECAY = -0.5
        private const val FACTOR = 19.0 / 81.0
        private const val MIN_STABILITY = 0.1
        private const val MIN_DIFFICULTY = 1.0
        private const val MAX_DIFFICULTY = 10.0

        /** Probability of recall [elapsedDays] after the last review: R(t,S) = (1 + 19/81·t/S)^-0.5. */
        fun retrievability(elapsedDays: Double, stability: Double): Double =
            if (stability <= 0.0) 0.0 else (1 + FACTOR * elapsedDays.coerceAtLeast(0.0) / stability).pow(DECAY)
    }
}
