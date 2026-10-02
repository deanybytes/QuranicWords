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
    }

    val byId: Map<String, AchievementDef> = all.associateBy { it.id }

    /** The 1-based chapter position a chapter-completion achievement id refers to, or null for
     * any other achievement. Position (by sortOrder), not a chapter id string: content ids have
     * changed shape before (`chapter_3` vs `ch_03`) and silently broke every chapter unlock. */
    fun chapterNumberFor(achievementId: String): Int? =
        Regex("^chapter_(\\d+)_complete$").find(achievementId)?.groupValues?.get(1)?.toIntOrNull()
}
