package com.quranicwords.app.core.domain.srs

/**
 * Learner-facing memory strength, bucketed from FSRS stability (days until recall drops to 90%).
 * [level] is the number of filled segments in the strength meter (0-4). A word counts as
 * *learned* once it is [FAMILIAR] or better - recalled correctly across at least two days - so
 * the "words learned" figures move as soon as review starts working, while [STRONG]/[MASTERED]
 * stay meaningful milestones of their own.
 */
enum class WordStrength(val level: Int) {
    NEW(0),
    LEARNING(1),
    FAMILIAR(2),
    STRONG(3),
    MASTERED(4);

    val isStrongOrBetter: Boolean get() = this == STRONG || this == MASTERED
    val isLearned: Boolean get() = level >= FAMILIAR.level

    companion object {
        const val FAMILIAR_MIN_DAYS = 2.0
        /** Stability from which a word counts as learned (see [isLearned]). */
        const val LEARNED_MIN_DAYS = FAMILIAR_MIN_DAYS
        const val STRONG_MIN_DAYS = 7.0
        const val MASTERED_MIN_DAYS = 30.0

        /** [stability] <= 0 (or no memory row at all) is [NEW]. */
        fun fromStability(stability: Double?): WordStrength = when {
            stability == null || stability <= 0.0 -> NEW
            stability < FAMILIAR_MIN_DAYS -> LEARNING
            stability < STRONG_MIN_DAYS -> FAMILIAR
            stability < MASTERED_MIN_DAYS -> STRONG
            else -> MASTERED
        }
    }
}
