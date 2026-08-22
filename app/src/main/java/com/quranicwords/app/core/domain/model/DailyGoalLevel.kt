package com.quranicwords.app.core.domain.model

/** Daily practice-time target, chosen once during onboarding (Settings can change it later) -
 * a fixed set of minute targets rather than a free slider, matching this project's other
 * enum-based settings ([ThemeMode], [QuranFontStyle], [FontScale]). Display names live in
 * `strings.xml` (`daily_goal_casual`/`daily_goal_steady`/`daily_goal_devoted`) rather than a
 * [LocalizedText] map, following [FontScale]'s pattern - this is app chrome, not seeded
 * vocabulary content. */
enum class DailyGoalLevel(val minutes: Int) {
    CASUAL(10),
    STEADY(20),
    DEVOTED(30);

    companion object {
        val DEFAULT = STEADY
        fun fromName(name: String?): DailyGoalLevel = entries.find { it.name == name } ?: DEFAULT
    }
}
