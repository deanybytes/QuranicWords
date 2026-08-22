package com.quranicwords.app.core.navigation

import com.quranicwords.app.core.data.local.entity.LessonKind
import kotlinx.serialization.Serializable

/** Type-safe Navigation-Compose destinations. */
sealed interface Route {
    @Serializable data object Splash : Route
    @Serializable data object LanguageSelect : Route
    @Serializable data object PathSelect : Route
    @Serializable data object FontSelect : Route
    @Serializable data object LearningStyleSelect : Route
    @Serializable data object DailyGoalSelect : Route
    @Serializable data object Home : Route
    @Serializable data class ChapterIntro(val chapterId: String) : Route
    @Serializable data class SectionIntro(val sectionId: String) : Route
    @Serializable data class WordBrowse(val sectionId: String) : Route
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
        val nextLessonId: String? = null,
        /** Null for a Review session. See [com.quranicwords.app.core.domain.model.LessonResult
         * .lessonKind] - carried straight through so this screen can show pass/fail messaging
         * tied to the real per-kind gating rule instead of a hardcoded accuracy threshold. */
        val lessonKind: LessonKind? = null,
        /** [com.quranicwords.app.core.domain.AchievementDef.id]s unlocked by finishing this
         * lesson/exam/Review, if any - ids rather than full defs so this stays a plain
         * String list; resolve back via [com.quranicwords.app.core.domain.AchievementCatalog
         * .byId] at the point of use. */
        val newlyUnlockedAchievementIds: List<String> = emptyList(),
        /** See [com.quranicwords.app.core.domain.model.LessonResult.durationMillis]. */
        val durationMillis: Long = 0L
    ) : Route
    @Serializable data object Roadmap : Route
}
