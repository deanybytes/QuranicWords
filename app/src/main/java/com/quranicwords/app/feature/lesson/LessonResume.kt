package com.quranicwords.app.feature.lesson

import com.quranicwords.app.core.util.AppJson
import kotlinx.serialization.Serializable

/**
 * Enough to put an interrupted Learn lesson back exactly where it was: the session's exercise
 * order (exercise ids, repeats included, after adaptive sequencing - distractors are regenerated
 * on resume, which is fine), the current position and the first-try score so far. Kept in the
 * ViewModel's SavedStateHandle (process death) and mirrored to DataStore so Home can offer
 * "Resume lesson" after the app was closed.
 */
@Serializable
data class LessonResumeRecord(
    val lessonId: String,
    val exerciseOrder: List<String>,
    val index: Int,
    val scoring: LessonScoring,
    val savedAtEpochMillis: Long
) {
    /** Recent enough to offer - a lesson abandoned days ago is better started fresh. */
    fun isFresh(nowMillis: Long): Boolean =
        nowMillis - savedAtEpochMillis in 0..MAX_AGE_MILLIS && index in 1 until exerciseOrder.size

    fun encode(): String = AppJson.encodeToString(serializer(), this)

    companion object {
        const val MAX_AGE_MILLIS = 24L * 60 * 60 * 1000

        fun decodeOrNull(json: String?): LessonResumeRecord? =
            json?.let { runCatching { AppJson.decodeFromString(serializer(), it) }.getOrNull() }
    }
}
