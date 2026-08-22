package com.quranicwords.app.core.navigation

import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.domain.model.LessonSessionType
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
        val durationMillis: Long = 0L,
        /** See [LessonSessionType]. */
        val sessionType: LessonSessionType = LessonSessionType.LESSON
    ) : Route
    @Serializable data object Roadmap : Route
    /** An unbounded random-word-pool quiz - see [LessonSessionType.OPEN_PRACTICE]. [isOpenPractice]
     * is always true - a `data class` rather than `data object` purely so this field lands in
     * `LessonViewModel`'s `SavedStateHandle` (keyed by property name, same as [Lesson.lessonId]),
     * giving that shared ViewModel a third distinguishable state alongside "has a lessonId" and
     * "reached via [Review]" without a bigger refactor of how it tells its modes apart. */
    @Serializable data class OpenPractice(val isOpenPractice: Boolean = true) : Route
    /** Pass/fail quiz to restore a locked streak - see [LessonSessionType.STREAK_RECOVERY] and
     * [com.quranicwords.app.core.domain.StreakRecovery]. Same `data class`-for-a-SavedStateHandle-
     * marker shape as [OpenPractice], for the same reason. */
    @Serializable data class StreakRecovery(val isStreakRecovery: Boolean = true) : Route
}
