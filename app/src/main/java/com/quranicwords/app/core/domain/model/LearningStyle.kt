package com.quranicwords.app.core.domain.model

/** How many times each word (and each Matching "exam" cycle) repeats within a lesson - a fixed
 * set of levels rather than a free slider, matching this project's other enum-based settings
 * ([FontScale], [DailyGoalLevel]). Applied at runtime by
 * [com.quranicwords.app.core.domain.LessonContentRepeater] rather than baked into content JSON,
 * so it's a pure per-session multiplier on top of whatever the content pipeline authored. */
enum class LearningStyle(val repeatCount: Int) {
    SHARP(1),
    SLOW(3),
    COZY(5);

    companion object {
        val DEFAULT = SHARP
        fun fromName(name: String?): LearningStyle = entries.find { it.name == name } ?: DEFAULT
    }
}
