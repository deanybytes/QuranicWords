package com.quranicwords.app.core.domain.model

/** Which of the two onboarding branches this user chose (`Route.PathSelect`, right after
 * language) - [LEARN] is the original guided chapter/section/lesson experience; [TEST_ONLY] is
 * for someone who already knows the vocabulary and only wants scored quiz practice, with no
 * teach step and no chapter/lesson unlock progression. A fixed choice rather than a free-form
 * setting, matching this project's other enum-based onboarding prefs ([DailyGoalLevel],
 * [LearningStyle]). Changeable later in Settings, same as those. */
enum class LearningPath {
    LEARN,
    TEST_ONLY;

    companion object {
        val DEFAULT = LEARN
        fun fromName(name: String?): LearningPath = entries.find { it.name == name } ?: DEFAULT
    }
}
