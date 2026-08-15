package com.quranicwords.app.core.domain

import com.quranicwords.app.core.data.local.entity.WordFrequencyEntity
import kotlin.math.abs

/**
 * A word candidate pool sorted by [WordFrequencyEntity.frequencyRank] once, with an id->index
 * lookup built alongside it - built once per lesson load and reused for every options-bearing
 * exercise in that lesson, rather than [DistractorGenerator] re-filtering/re-sorting the full
 * corpus from scratch on every single call (real repeated work: a ~10-exercise lesson against
 * the full seeded vocabulary was ~10 independent O(n log n) sorts over the same data).
 */
class WordCandidatePool private constructor(
    private val sortedByRank: List<WordFrequencyEntity>,
    private val indexById: Map<String, Int>
) {
    fun get(id: String): WordFrequencyEntity? = indexById[id]?.let { sortedByRank[it] }

    /** The `radius` nearest-by-list-position entries around [id] (i.e. nearest by frequency rank,
     * since the pool is sorted by rank) - a bounded slice [DistractorGenerator] filters/sorts
     * instead of the full pool, since a distractor is never chosen from far outside this range in
     * practice. `null` if [id] isn't in this pool. */
    fun windowAround(id: String, radius: Int): List<WordFrequencyEntity>? {
        val index = indexById[id] ?: return null
        val start = (index - radius).coerceAtLeast(0)
        val end = (index + radius).coerceAtMost(sortedByRank.lastIndex)
        return sortedByRank.subList(start, end + 1)
    }

    companion object {
        fun from(candidates: List<WordFrequencyEntity>): WordCandidatePool {
            val sorted = candidates.sortedBy { it.frequencyRank }
            return WordCandidatePool(sorted, sorted.withIndex().associate { (i, c) -> c.id to i })
        }
    }
}

/**
 * Picks multiple-choice distractors at runtime instead of the fixed pool the content pipeline
 * bakes into each exercise's JSON (see `tools/ingestion/09_fix_distractor_pools.py`) - a
 * stateless object since selection is pure data-in/data-out with no dependencies to inject.
 *
 * Every word in the currently-seeded corpus shares one `tierLevel`, so that filter is a no-op
 * today; it stays in place for when a wider tier range is seeded. The only live signal is
 * `frequencyRank` proximity, with a soft tie-break toward the learner's past confusions. Only
 * searches within [WINDOW_RADIUS] positions of the correct item in [WordCandidatePool] (bounded
 * work per call) rather than the whole pool - if a sparser future tier means fewer than [count]
 * same-tier siblings fall inside that window, the caller's existing baked-options fallback (see
 * `LessonViewModel.rebuildOptions`) already covers topping up the remainder.
 */
object DistractorGenerator {
    private const val WINDOW_RADIUS = 60

    fun pickDistractors(
        correctId: String,
        pool: WordCandidatePool,
        missedItemIds: Set<String>,
        count: Int = 3
    ): List<String> {
        val correct = pool.get(correctId) ?: return emptyList()
        val window = pool.windowAround(correctId, WINDOW_RADIUS) ?: return emptyList()
        return window
            .asSequence()
            .filter { it.id != correctId && it.tierLevel == correct.tierLevel }
            .sortedWith(
                compareBy(
                    { abs(it.frequencyRank - correct.frequencyRank) },
                    { if (it.id in missedItemIds) 0 else 1 }
                )
            )
            .take(count)
            .map { it.id }
            .toList()
    }
}
