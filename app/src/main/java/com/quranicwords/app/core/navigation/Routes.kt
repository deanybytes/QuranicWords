package com.quranicwords.app.core.navigation

import kotlinx.serialization.Serializable

/** Type-safe Navigation-Compose destinations. */
sealed interface Route {
    @Serializable data object Splash : Route
    @Serializable data object LanguageSelect : Route
    @Serializable data object FontSelect : Route
    @Serializable data object Home : Route
    @Serializable data class Lesson(val lessonId: String) : Route
    /** The dynamic Review session (see `LessonViewModel.isReviewSession`) - a distinct route
     * rather than a sentinel `lessonId` string, so it can never collide with a real lesson id. */
    @Serializable data object Review : Route
    @Serializable data class LessonSummary(
        val lessonId: String,
        val correctCount: Int,
        val totalCount: Int,
        val pointsAwarded: Int,
        val newTotalPoints: Int,
        val currentStreak: Int,
        val streakIncreased: Boolean,
        val nextLessonId: String? = null
    ) : Route
    @Serializable data object Settings : Route
}
