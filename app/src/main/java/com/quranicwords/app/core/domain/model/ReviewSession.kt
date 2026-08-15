package com.quranicwords.app.core.domain.model

/**
 * Display-only placeholder `lessonId` for a completed Review session's [LessonResult]/
 * `Route.LessonSummary` (which has no real lesson to name). Routing itself no longer depends on
 * this - `Route.Review` (see `core/navigation/Routes.kt`) is a distinct, type-safe destination
 * from `Route.Lesson`, so there's no sentinel-string collision risk there anymore.
 */
const val REVIEW_SESSION_LESSON_ID = "review"
