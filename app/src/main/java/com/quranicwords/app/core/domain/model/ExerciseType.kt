package com.quranicwords.app.core.domain.model

import kotlinx.serialization.Serializable

/** [Serializable] so [com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity]
 * can round-trip through [com.quranicwords.app.core.domain.repository.BackupRepository]. */
@Serializable
enum class ExerciseType {
    WORD_INTRO,
    MULTIPLE_CHOICE,
    /** Withdrawn listening exercise - kept only so existing `exercise_attempts` rows (stored by
     * name) and backups still decode. Nothing creates it any more. */
    @Deprecated("Listening exercises were removed; kept only to decode old attempt rows.")
    TAP_WHAT_YOU_HEAR,
    MATCHING,
    TEACH_WORD,
    /** Withdrawn in 1.0.1: lessons and quizzes use multiple choice and matching only. Kept so
     * old `exercise_attempts` rows (stored by name) and backups still decode. */
    @Deprecated("Removed in 1.0.1; kept only to decode old attempt rows.")
    FILL_IN_THE_BLANK,
    /** Withdrawn - see [FILL_IN_THE_BLANK]. */
    @Deprecated("Removed in 1.0.1; kept only to decode old attempt rows.")
    WORD_ORDER,
    /** Withdrawn listening exercise - see [TAP_WHAT_YOU_HEAR]. */
    @Deprecated("Listening exercises were removed; kept only to decode old attempt rows.")
    LISTEN_AND_TYPE,
    /** Withdrawn - see [FILL_IN_THE_BLANK]. */
    @Deprecated("Removed in 1.0.1; kept only to decode old attempt rows.")
    WORD_IN_VERSE_TAP,
    CHAPTER_INTRO
}

/** True for every withdrawn type (listening, fill-in-the-blank, word order, tap-the-word), which
 * nothing may create, pick or show any more. Only multiple choice and matching are quizzed. */
@Suppress("DEPRECATION")
val ExerciseType.isWithdrawn: Boolean
    get() = when (this) {
        ExerciseType.TAP_WHAT_YOU_HEAR, ExerciseType.LISTEN_AND_TYPE,
        ExerciseType.FILL_IN_THE_BLANK, ExerciseType.WORD_ORDER, ExerciseType.WORD_IN_VERSE_TAP -> true
        else -> false
    }
