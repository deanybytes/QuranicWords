package com.quranicwords.app.core.domain.model

/**
 * In-session facts only the lesson screen knows, handed to `ProgressRepository.completeLesson`/
 * `completeReviewSession` so XP, best combo and daily quests update in the same transaction as
 * the result itself. All default to zero, so callers that don't track them stay valid.
 */
data class SessionStats(
    /** Combo bonus XP earned this session (see GamificationConfig.comboBonusFor). */
    val comboBonusXp: Int = 0,
    val bestCombo: Int = 0,
    /** Distinct words answered first-try this session. */
    val firstTryAnswers: Int = 0,
    /** First-try answers to listening (tap what you hear) exercises. */
    val listeningAnswers: Int = 0,
    /** Words answered for the very first time ever (no memory before this session). */
    val newWords: Int = 0
)

/** Hearts as the UI needs them. [nextHeartAtMillis] is null when full (or hearts are off). */
data class HeartsStatus(
    val enabled: Boolean,
    val hearts: Int,
    val nextHeartAtMillis: Long?
) {
    val isEmpty: Boolean get() = enabled && hearts <= 0
}
