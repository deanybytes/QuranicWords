package com.quranicwords.app.core.domain

import com.quranicwords.app.core.data.local.entity.WordFrequencyEntity
import com.quranicwords.app.core.domain.model.LocalizedText
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

    val all: List<WordFrequencyEntity> get() = sortedByRank

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
 * Guarantees that every distractor has a distinct meaning (and Arabic word) from the correct
 * option and from all other chosen distractors, so all 4 options presented to the learner
 * are strictly unique.
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

        val candidates = window
            .asSequence()
            .filter { it.id != correctId && it.tierLevel == correct.tierLevel }
            .sortedWith(
                compareBy(
                    { abs(it.frequencyRank - correct.frequencyRank) },
                    { if (it.id in missedItemIds) 0 else 1 }
                )
            )

        val selected = mutableListOf<WordFrequencyEntity>()
        for (cand in candidates) {
            if (selected.size >= count) break
            if (!wordsCollide(cand, correct) && selected.none { wordsCollide(it, cand) }) {
                selected.add(cand)
            }
        }

        return selected.map { it.id }
    }

    /** One extra, never-matchable id for the Matching exercise's meaning-side distractor tile
     * (see [com.quranicwords.app.core.domain.model.ExerciseContent.Matching.distractorRight]).
     * Excludes all [usedWordIds] and any words sharing the same meaning or Arabic word with any
     * of the used words. */
    fun pickMatchingDistractor(
        usedWordIds: Set<String>,
        pool: WordCandidatePool,
        missedItemIds: Set<String>
    ): String? {
        val anchorId = usedWordIds.firstOrNull() ?: return null
        val anchor = pool.get(anchorId) ?: return null
        val usedWords = usedWordIds.mapNotNull { pool.get(it) }

        val window = pool.windowAround(anchorId, WINDOW_RADIUS) ?: return null
        val candidates = window
            .asSequence()
            .filter { it.id !in usedWordIds && it.tierLevel == anchor.tierLevel }
            .sortedWith(
                compareBy(
                    { abs(it.frequencyRank - anchor.frequencyRank) },
                    { if (it.id in missedItemIds) 0 else 1 }
                )
            )

        return candidates.firstOrNull { cand ->
            usedWords.none { used -> wordsCollide(used, cand) }
        }?.id
    }

    fun wordsCollide(a: WordFrequencyEntity, b: WordFrequencyEntity): Boolean {
        if (a.id == b.id) return true
        val arabicA = a.arabicWord.trim()
        val arabicB = b.arabicWord.trim()
        if (arabicA.isNotEmpty() && arabicB.isNotEmpty() && arabicA == arabicB) return true

        return meaningsCollide(a.meaning, b.meaning)
    }

    fun meaningsCollide(a: LocalizedText, b: LocalizedText): Boolean {
        val commonKeys = a.keys.intersect(b.keys)
        for (k in commonKeys) {
            val valA = a[k]?.trim()?.lowercase().orEmpty()
            val valB = b[k]?.trim()?.lowercase().orEmpty()
            if (valA.isNotEmpty() && valB.isNotEmpty()) {
                if (valA == valB) return true
                val tokensA = valA.split('/').map { it.trim() }.filter { it.isNotEmpty() }
                val tokensB = valB.split('/').map { it.trim() }.filter { it.isNotEmpty() }
                if (tokensA.any { it in tokensB }) return true
            }
        }
        val enA = (a["en"] ?: a.values.firstOrNull())?.trim()?.lowercase().orEmpty()
        val enB = (b["en"] ?: b.values.firstOrNull())?.trim()?.lowercase().orEmpty()
        if (enA.isNotEmpty() && enB.isNotEmpty()) {
            if (enA == enB) return true
            val tokensA = enA.split('/').map { it.trim() }.filter { it.isNotEmpty() }
            val tokensB = enB.split('/').map { it.trim() }.filter { it.isNotEmpty() }
            if (tokensA.any { it in tokensB }) return true
        }
        return false
    }
}
