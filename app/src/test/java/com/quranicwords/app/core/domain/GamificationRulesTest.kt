package com.quranicwords.app.core.domain

import com.quranicwords.app.core.data.local.entity.DailyQuestEntity
import com.quranicwords.app.core.util.ArabicSearch
import com.quranicwords.app.core.util.GamificationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelCurveTest {

    @Test
    fun `levels start at 1 and follow 50 L (L-1)`() {
        assertEquals(1, LevelCurve.levelFor(0))
        assertEquals(1, LevelCurve.levelFor(99))
        assertEquals(2, LevelCurve.levelFor(100))
        assertEquals(2, LevelCurve.levelFor(299))
        assertEquals(3, LevelCurve.levelFor(300))
        assertEquals(10, LevelCurve.levelFor(4500))
        assertEquals(9, LevelCurve.levelFor(4499))
        assertEquals(1, LevelCurve.levelFor(-50))
    }

    @Test
    fun `the formula and the threshold table agree at every boundary`() {
        for (level in 1..60) {
            val xp = LevelCurve.xpForLevel(level)
            assertEquals("at $xp", level, LevelCurve.levelFor(xp))
            if (xp > 0) assertEquals("just under $xp", level - 1, LevelCurve.levelFor(xp - 1))
        }
    }

    @Test
    fun `progress within a level`() {
        val progress = LevelCurve.progressFor(350) // level 3 spans 300..600
        assertEquals(3, progress.level)
        assertEquals(50, progress.xpIntoLevel)
        assertEquals(300, progress.xpForNextLevel)
        assertEquals(50f / 300f, progress.fraction, 1e-6f)
    }
}

class HeartsCalculatorTest {

    private val regen = HeartsCalculator.REGEN_MILLIS

    @Test
    fun `hearts regenerate one per interval up to the max`() {
        assertEquals(2, HeartsCalculator.current(2, 0L, regen - 1).hearts)
        assertEquals(3, HeartsCalculator.current(2, 0L, regen).hearts)
        assertEquals(4, HeartsCalculator.current(2, 0L, 2 * regen + 5).hearts)
        assertEquals(HeartsCalculator.MAX_HEARTS, HeartsCalculator.current(0, 0L, 100 * regen).hearts)
    }

    @Test
    fun `partial progress towards the next heart is kept`() {
        val state = HeartsCalculator.current(1, 0L, regen + 10 * 60_000L)
        assertEquals(2, state.hearts)
        assertEquals(regen, state.anchorMillis)
        assertEquals(regen - 10 * 60_000L, HeartsCalculator.millisUntilNext(state.hearts, state.anchorMillis, regen + 10 * 60_000L))
    }

    @Test
    fun `losing a heart from full starts the regeneration clock now`() {
        val lost = HeartsCalculator.lose(HeartsCalculator.MAX_HEARTS, 0L, 1_000_000L)
        assertEquals(HeartsCalculator.MAX_HEARTS - 1, lost.hearts)
        assertEquals(1_000_000L, lost.anchorMillis)
        assertNull(HeartsCalculator.millisUntilNext(HeartsCalculator.MAX_HEARTS, 0L, 5L))
    }

    @Test
    fun `hearts never go below zero`() {
        assertEquals(0, HeartsCalculator.lose(0, 1_000L, 1_000L).hearts)
    }

    @Test
    fun `a clock moved backwards grants nothing and re-anchors`() {
        val rolledBack = HeartsCalculator.current(1, 10 * regen, 2 * regen)
        assertEquals(1, rolledBack.hearts)
        assertEquals(2 * regen, rolledBack.anchorMillis)
        // Moving forward again only regenerates from the new anchor.
        assertEquals(1, HeartsCalculator.current(rolledBack.hearts, rolledBack.anchorMillis, 2 * regen + regen - 1).hearts)
    }

    @Test
    fun `a review refill adds a heart and never exceeds the max`() {
        assertEquals(1, HeartsCalculator.gain(0, 0L, 0L).hearts)
        assertEquals(HeartsCalculator.MAX_HEARTS, HeartsCalculator.gain(HeartsCalculator.MAX_HEARTS, 0L, 0L).hearts)
    }
}

class QuestCatalogTest {

    private val eligibility = QuestEligibility(dueReviewCount = 12, listeningEnabled = true, lessonsAvailable = true, dailyGoalMinutes = 20)

    @Test
    fun `selection is deterministic per learner and day`() {
        val a = QuestCatalog.questsFor("user-1", "2026-10-03", eligibility)
        val b = QuestCatalog.questsFor("user-1", "2026-10-03", eligibility)
        assertEquals(a, b)
        assertEquals(QuestCatalog.QUESTS_PER_DAY, a.size)
        assertEquals(a.size, a.map { it.id }.toSet().size)
        // Different days (or learners) vary over a span of days.
        val week = (1..9).map { QuestCatalog.questsFor("user-1", "2026-10-0$it", eligibility).map { q -> q.id } }.toSet()
        assertTrue(week.size > 1)
    }

    @Test
    fun `a review quest is always included while words are due, capped at ten`() {
        repeat(20) { day ->
            val quests = QuestCatalog.questsFor("u$day", "2026-10-03", eligibility)
            val review = quests.first { it.metric == QuestMetric.REVIEW_WORDS }
            assertEquals(10, review.target)
        }
        val few = QuestCatalog.questsFor("u", "2026-10-03", eligibility.copy(dueReviewCount = 3))
        assertEquals(3, few.first { it.metric == QuestMetric.REVIEW_WORDS }.target)
    }

    @Test
    fun `impossible quests are never offered`() {
        val pool = QuestCatalog.pool(QuestEligibility(dueReviewCount = 0, listeningEnabled = false, lessonsAvailable = false, dailyGoalMinutes = 10))
        assertTrue(pool.none { it.metric == QuestMetric.REVIEW_WORDS || it.metric == QuestMetric.LISTENING || it.metric == QuestMetric.FINISH_LESSONS })
    }

    private fun quest(id: String, metric: QuestMetric, target: Int, progress: Int = 0, completedAt: Long? = null) =
        DailyQuestEntity("u", "2026-10-03", id, metric.name, target, progress, rewardXp = 20, completedAtEpochMillis = completedAt)

    @Test
    fun `progress accumulates or keeps the best value, and the reward is paid exactly once`() {
        val quests = listOf(
            quest("xp", QuestMetric.EARN_XP, 50),
            quest("combo", QuestMetric.COMBO, 10, progress = 6),
            quest("done", QuestMetric.FINISH_LESSONS, 2, progress = 2, completedAt = 1L)
        )

        val first = QuestCatalog.apply(quests, QuestEvent(xpEarned = 30, bestCombo = 4, lessonsFinished = 1), nowMillis = 5L)
        assertEquals(listOf("xp"), first.changed.map { it.questId })
        assertEquals(30, first.changed.single().progress)
        assertEquals(0, first.rewardXp)

        val afterFirst = quests.map { q -> first.changed.find { it.questId == q.questId } ?: q }
        val second = QuestCatalog.apply(afterFirst, QuestEvent(xpEarned = 40, bestCombo = 11), nowMillis = 9L)
        assertEquals(setOf("xp", "combo"), second.completedQuestIds.toSet())
        assertEquals(40, second.rewardXp)
        assertTrue(second.changed.all { it.progress == it.target && it.completedAtEpochMillis == 9L })

        val afterSecond = afterFirst.map { q -> second.changed.find { it.questId == q.questId } ?: q }
        val third = QuestCatalog.apply(afterSecond, QuestEvent(xpEarned = 500, bestCombo = 30, lessonsFinished = 5), nowMillis = 10L)
        assertEquals(0, third.rewardXp)
        assertTrue(third.changed.isEmpty())
    }
}

class GamificationConfigComboTest {

    @Test
    fun `combo bonus tiers`() {
        assertEquals(0, GamificationConfig.comboBonusFor(4))
        assertEquals(2, GamificationConfig.comboBonusFor(5))
        assertEquals(2, GamificationConfig.comboBonusFor(9))
        assertEquals(5, GamificationConfig.comboBonusFor(10))
        assertEquals(5, GamificationConfig.comboBonusFor(40))
    }
}

class ArabicSearchTest {

    @Test
    fun `matching ignores harakat, tatweel and alif variants`() {
        assertTrue(ArabicSearch.matches("كَتَبَ", "كتب"))
        assertTrue(ArabicSearch.matches("الْكِتَابُ", "كتاب"))
        assertTrue(ArabicSearch.matches("أَنزَلَ", "انزل"))
        assertTrue(ArabicSearch.matches("ٱلرَّحْمَٰنِ", "الرحمن"))
        assertTrue(ArabicSearch.matches("بـــسم", "بسم"))
        assertTrue(ArabicSearch.matches("Mercy", "merc"))
        assertFalse(ArabicSearch.matches("كَتَبَ", "قرأ"))
        assertTrue(ArabicSearch.matches("anything", "  "))
        assertFalse(ArabicSearch.matches(null, "x"))
    }
}

class AchievementProgressTest {

    private fun def(id: String) = checkNotNull(AchievementCatalog.byId[id])

    @Test
    fun `locked achievements report progress towards their requirement`() {
        val metrics = AchievementMetrics(longestStreak = 4, bestCombo = 12, level = 6, strongWordCount = 40, reviewCount = 120)

        assertEquals(AchievementProgress(4, 7), AchievementCatalog.progressOf(def("streak_7"), metrics))
        assertTrue(AchievementCatalog.progressOf(def("combo_10"), metrics).isComplete)
        assertEquals(AchievementProgress(12, 25), AchievementCatalog.progressOf(def("combo_25"), metrics))
        assertTrue(AchievementCatalog.progressOf(def("level_5"), metrics).isComplete)
        assertEquals(AchievementProgress(6, 10), AchievementCatalog.progressOf(def("level_10"), metrics))
        assertEquals(AchievementProgress(40, 100), AchievementCatalog.progressOf(def("strong_100"), metrics))
        assertTrue(AchievementCatalog.progressOf(def("reviews_100"), metrics).isComplete)
        assertEquals(AchievementProgress(0, 1), AchievementCatalog.progressOf(def(AchievementCatalog.FIRST_REVIEW_SESSION), metrics))
        assertTrue(AchievementCatalog.progressOf(def(AchievementCatalog.FIRST_REVIEW_SESSION), metrics.copy(hasCompletedReviewSession = true)).isComplete)
    }

    @Test
    fun `chapter achievements are keyed by chapter position`() {
        val metrics = AchievementMetrics(completedChapterPositions = setOf(2))
        assertFalse(AchievementCatalog.progressOf(def("chapter_1_complete"), metrics).isComplete)
        assertTrue(AchievementCatalog.progressOf(def("chapter_2_complete"), metrics).isComplete)
    }
}
