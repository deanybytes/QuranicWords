package com.quranicwords.app.core.domain

import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.practicedItemId

/**
 * Reorders a lesson's fixed, authored exercise list to surface previously-missed items sooner,
 * without breaking the "teach before its own quiz" invariant the authored order guarantees.
 *
 * The list is split into groups, each starting at a teach step ([ExerciseContent.WordIntro]) and
 * absorbing every item up to (not including) the next teach step - this keeps each word's
 * teach-then-quiz pair together as one unit. Any group containing a [ExerciseContent.Matching]
 * exercise is pinned in place: Matching quizzes several words at once, so its correct position
 * depends on every one of those words already having been taught somewhere earlier in the list -
 * reordering it (or anything relative to it) risks moving a word's quiz ahead of its own teach
 * step. Movable groups (plain single-word teach+quiz pairs) are then stably reordered so ones
 * containing a missed item come first, preserving original relative order otherwise.
 *
 * Since every authored group already alternates teach-step -> quiz-step internally, and this
 * function only ever permutes whole groups (never splits one), a group boundary is always
 * quiz-type -> teach-type - so no same-exercise-type run can be introduced at a boundary that
 * wasn't already present in the input.
 */
object AdaptiveSequencer {
    fun reorderForAdaptivePractice(
        contents: List<ExerciseContent>,
        missedItemIds: Set<String>
    ): List<ExerciseContent> {
        if (contents.isEmpty() || missedItemIds.isEmpty()) return contents

        val groups = mutableListOf<List<ExerciseContent>>()
        var current = mutableListOf<ExerciseContent>()
        for (item in contents) {
            val startsNewGroup = item is ExerciseContent.WordIntro
            if (startsNewGroup && current.isNotEmpty()) {
                groups += current
                current = mutableListOf()
            }
            current += item
        }
        if (current.isNotEmpty()) groups += current

        val movableIndices = groups.indices.filter { !isPinned(groups[it]) }
        val movableGroupsSorted = movableIndices
            .map { groups[it] }
            .sortedByDescending { hasMissedItem(it, missedItemIds) } // stable: ties keep original order

        val result = groups.toMutableList()
        movableIndices.forEachIndexed { slot, originalIndex -> result[originalIndex] = movableGroupsSorted[slot] }

        return result.flatten()
    }

    private fun isPinned(group: List<ExerciseContent>): Boolean =
        group.any { it is ExerciseContent.Matching }

    private fun hasMissedItem(group: List<ExerciseContent>, missedItemIds: Set<String>): Boolean =
        group.any { item -> item.practicedItemId()?.let { it in missedItemIds } == true }
}
