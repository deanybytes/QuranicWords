package com.quranicwords.app.core.domain

import com.quranicwords.app.R
import com.quranicwords.app.core.ui.components.MotifKind
import com.quranicwords.app.core.util.StreakTiers

/**
 * One entry per unlockable achievement. [nameRes]/[descriptionRes] point at either a plain string
 * (chosen when [nameArg]/[descriptionArg] is null) or a `%d`-templated one (when non-null) - see
 * [com.quranicwords.app.feature.achievements.displayName]/`displayDescription` for how a
 * Composable resolves either form. [motifKind] drives which [MotifKind] badge icon renders this
 * achievement, following the category->motif mapping documented in docs/UI_GUIDELINES.md.
 *
 * This is a static, code-side catalog (not stored in Room) - [com.quranicwords.app.core.data
 * .local.entity.AchievementEntity.achievementId] is a plain string key into [byId], so adding a
 * new achievement here never needs a schema change, only a new catalog entry (+ its strings).
 */
data class AchievementDef(
    val id: String,
    val motifKind: MotifKind,
    val nameRes: Int,
    val descriptionRes: Int,
    val nameArg: Int? = null,
    val descriptionArg: Int? = null
)

object AchievementCatalog {
    /** How many chapters the bundled curriculum has (`assets/content/chapters.json`). Static
     * because this is a compile-time list; [com.quranicwords.app.core.data.repository
     * .AchievementRepositoryImpl] resolves "chapter N" to the N-th seeded chapter by sortOrder, so
     * an achievement past the real chapter count simply never unlocks. */
    private const val CHAPTER_COUNT = 10
    val coverageBands = listOf(25, 50, 75, 100)
    val COMBO_TIERS = listOf(10, 25)
    val LEVEL_TIERS = listOf(5, 10, 20)

    val all: List<AchievementDef> = buildList {
        add(AchievementDef("streak_${StreakTiers.BRONZE}", MotifKind.CRESCENT, R.string.achievement_streak_bronze_name, R.string.achievement_streak_description, descriptionArg = StreakTiers.BRONZE))
        add(AchievementDef("streak_${StreakTiers.SILVER}", MotifKind.CRESCENT, R.string.achievement_streak_silver_name, R.string.achievement_streak_description, descriptionArg = StreakTiers.SILVER))
        add(AchievementDef("streak_${StreakTiers.GOLD}", MotifKind.CRESCENT, R.string.achievement_streak_gold_name, R.string.achievement_streak_description, descriptionArg = StreakTiers.GOLD))

        for (chapterNumber in 1..CHAPTER_COUNT) {
            add(
                AchievementDef(
                    id = "chapter_${chapterNumber}_complete",
                    motifKind = MotifKind.MOSQUE,
                    nameRes = R.string.achievement_chapter_name,
                    descriptionRes = R.string.achievement_chapter_description,
                    nameArg = chapterNumber,
                    descriptionArg = chapterNumber
                )
            )
        }

        coverageBands.forEach { band ->
            add(
                AchievementDef(
                    id = "coverage_$band",
                    motifKind = MotifKind.BOOK,
                    nameRes = R.string.achievement_coverage_name,
                    descriptionRes = R.string.achievement_coverage_description,
                    nameArg = band,
                    descriptionArg = band
                )
            )
        }

        add(AchievementDef("first_lesson", MotifKind.STARFIELD, R.string.achievement_first_lesson_name, R.string.achievement_first_lesson_description))
        add(AchievementDef("first_exam_passed", MotifKind.STARFIELD, R.string.achievement_first_exam_name, R.string.achievement_first_exam_description))

        add(AchievementDef(FIRST_REVIEW_SESSION, MotifKind.BOOK, R.string.achievement_first_review_name, R.string.achievement_first_review_description))
        add(AchievementDef("reviews_$REVIEWS_TARGET", MotifKind.BOOK, R.string.achievement_reviews_name, R.string.achievement_reviews_description, descriptionArg = REVIEWS_TARGET))
        COMBO_TIERS.forEach { tier ->
            add(AchievementDef("combo_$tier", MotifKind.STARFIELD, R.string.achievement_combo_name, R.string.achievement_combo_description, nameArg = tier, descriptionArg = tier))
        }
        LEVEL_TIERS.forEach { tier ->
            add(AchievementDef("level_$tier", MotifKind.MOSQUE, R.string.achievement_level_name, R.string.achievement_level_description, nameArg = tier, descriptionArg = tier))
        }
        add(AchievementDef("strong_$STRONG_WORDS_TARGET", MotifKind.BOOK, R.string.achievement_strong_name, R.string.achievement_strong_description, descriptionArg = STRONG_WORDS_TARGET))
    }

    val byId: Map<String, AchievementDef> = all.associateBy { it.id }

    /** The 1-based chapter position a chapter-completion achievement id refers to, or null for
     * any other achievement. Position (by sortOrder), not a chapter id string: content ids have
     * changed shape before (`chapter_3` vs `ch_03`) and silently broke every chapter unlock. */
    fun chapterNumberFor(achievementId: String): Int? =
        Regex("^chapter_(\\d+)_complete$").find(achievementId)?.groupValues?.get(1)?.toIntOrNull()

    const val FIRST_REVIEW_SESSION = "first_review_session"
    const val REVIEWS_TARGET = 100
    const val STRONG_WORDS_TARGET = 100

    /**
     * How far [metrics] are towards [def], as (current, target) - unlocked once current reaches
     * target. One rule set for both unlocking and the locked items' progress bars, so a bar can
     * never show "7/7" on something still locked. Current is capped at target.
     */
    fun progressOf(def: AchievementDef, metrics: AchievementMetrics): AchievementProgress {
        val id = def.id
        fun of(current: Int, target: Int) = AchievementProgress(current.coerceIn(0, target), target)
        fun flag(done: Boolean) = of(if (done) 1 else 0, 1)
        chapterNumberFor(id)?.let { return flag(it in metrics.completedChapterPositions) }
        return when {
            id.startsWith("streak_") -> of(metrics.longestStreak, id.removePrefix("streak_").toIntOrNull() ?: Int.MAX_VALUE)
            id.startsWith("coverage_") -> of(metrics.coveragePercent.toInt(), id.removePrefix("coverage_").toIntOrNull() ?: 100)
            id == "first_lesson" -> flag(metrics.hasCompletedRegularLesson)
            id == "first_exam_passed" -> flag(metrics.hasPassedExam)
            id == FIRST_REVIEW_SESSION -> flag(metrics.hasCompletedReviewSession)
            id.startsWith("reviews_") -> of(metrics.reviewCount, REVIEWS_TARGET)
            id.startsWith("combo_") -> of(metrics.bestCombo, id.removePrefix("combo_").toIntOrNull() ?: Int.MAX_VALUE)
            id.startsWith("level_") -> of(metrics.level, id.removePrefix("level_").toIntOrNull() ?: Int.MAX_VALUE)
            id.startsWith("strong_") -> of(metrics.strongWordCount, STRONG_WORDS_TARGET)
            else -> AchievementProgress(0, 1)
        }
    }
}

/** Everything the catalog's rules read, gathered once per check (see AchievementRepositoryImpl). */
data class AchievementMetrics(
    val longestStreak: Int = 0,
    /** 1-based positions (by sortOrder) of chapters whose chapter exam is completed. */
    val completedChapterPositions: Set<Int> = emptySet(),
    val coveragePercent: Double = 0.0,
    val hasCompletedRegularLesson: Boolean = false,
    val hasPassedExam: Boolean = false,
    val hasCompletedReviewSession: Boolean = false,
    /** Spaced reviews done - every scheduled review after a word's first sighting. */
    val reviewCount: Int = 0,
    val bestCombo: Int = 0,
    val level: Int = 1,
    val strongWordCount: Int = 0
)

data class AchievementProgress(val current: Int, val target: Int) {
    val isComplete: Boolean get() = current >= target
    val fraction: Float get() = if (target <= 0) 1f else (current.toFloat() / target).coerceIn(0f, 1f)
}
