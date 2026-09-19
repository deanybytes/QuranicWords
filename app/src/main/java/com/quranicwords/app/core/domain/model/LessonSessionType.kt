package com.quranicwords.app.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Which of the four things [com.quranicwords.app.feature.lesson.LessonViewModel]/`LessonScreen`
 * are actually running - all four share the same exercise-answering UI and `LessonResult`/
 * `Route.LessonSummary` shape, but need distinct completion semantics (which repository method to
 * call) and distinct summary-screen framing (what the primary button says and does next):
 * - [LESSON]: a real lesson/exam/flashback (`Route.Lesson`) - `nextLessonId` drives "Continue".
 * - [REVIEW]: the dynamic Review session (`Route.Review`) - always "Back to Home" next.
 * - [OPEN_PRACTICE]: an unbounded random-word-pool quiz (`Route.OpenPractice`) - "Keep Practicing"
 *   starts a fresh batch instead of going home, which is how Test/Quiz-only mode and the
 *   post-100%-completion loop both avoid a dead end.
 * - [STREAK_RECOVERY]: a pass/fail quiz to restore a locked streak (`Route.StreakRecovery`) -
 *   distinct "Streak Restored"/"not recovered" messaging instead of the normal points/accuracy
 *   framing.
 */
@Serializable
@androidx.annotation.Keep
enum class LessonSessionType { LESSON, REVIEW, OPEN_PRACTICE, STREAK_RECOVERY }
