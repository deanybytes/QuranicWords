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
    FILL_IN_THE_BLANK,
    WORD_ORDER,
    /** Withdrawn listening exercise - see [TAP_WHAT_YOU_HEAR]. */
    @Deprecated("Listening exercises were removed; kept only to decode old attempt rows.")
    LISTEN_AND_TYPE,
    WORD_IN_VERSE_TAP,
    CHAPTER_INTRO
}

/** True for the withdrawn listening types, which nothing may create, pick or show any more. */
@Suppress("DEPRECATION")
val ExerciseType.isWithdrawn: Boolean
    get() = this == ExerciseType.TAP_WHAT_YOU_HEAR || this == ExerciseType.LISTEN_AND_TYPE
