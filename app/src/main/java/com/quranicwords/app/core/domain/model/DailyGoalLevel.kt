package com.quranicwords.app.core.domain.model

/** Daily practice-time target, chosen once during onboarding (Settings can change it later) -
 * a fixed set of minute targets rather than a free slider, matching this project's other
 * enum-based settings ([ThemeMode], [QuranFontStyle], [FontScale]). Display names live in
 * `strings.xml` (`daily_goal_casual`/`daily_goal_steady`/`daily_goal_devoted`) rather than a
 * [LocalizedText] map, following [FontScale]'s pattern - this is app chrome, not seeded
 * vocabulary content. [dailyReviewCap] bounds one Daily Review session (spaced-repetition due
 * words) so a backlog after time away is worked through over several days, not in one sitting. */
enum class DailyGoalLevel(val minutes: Int, val dailyReviewCap: Int) {
    CASUAL(10, dailyReviewCap = 30),
    STEADY(20, dailyReviewCap = 50),
    DEVOTED(30, dailyReviewCap = 80);

    companion object {
        val DEFAULT = STEADY
        fun fromName(name: String?): DailyGoalLevel = entries.find { it.name == name } ?: DEFAULT
    }
}
