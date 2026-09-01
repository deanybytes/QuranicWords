package com.quranicwords.app.core.domain.model

import kotlinx.serialization.Serializable

/** [Serializable] so [com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity]
 * can round-trip through [com.quranicwords.app.core.domain.repository.BackupRepository]. */
@Serializable
enum class ExerciseType {
    WORD_INTRO,
    MULTIPLE_CHOICE,
    TAP_WHAT_YOU_HEAR,
    MATCHING,
    TEACH_WORD,
    FILL_IN_THE_BLANK,
    WORD_ORDER,
    LISTEN_AND_TYPE,
    WORD_IN_VERSE_TAP,
    CHAPTER_INTRO
}
