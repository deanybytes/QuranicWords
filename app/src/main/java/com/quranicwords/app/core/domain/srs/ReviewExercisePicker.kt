package com.quranicwords.app.core.domain.srs

import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.isWithdrawn

/**
 * Chooses one exercise per due word for the Daily Review, harder as the memory gets stronger:
 * - New/Learning: recognition (multiple choice).
 * - Familiar: recall in context - fill-in-the-blank or tap-the-word-in-the-verse, alternating so a
 *   session mixes both.
 * - Strong/Mastered: recall in context as well (fill-in-the-blank, then tap-the-word-in-the-verse),
 *   with multiple choice as the fallback.
 * Each tier falls back through the others, so a word with any scored exercise always gets one.
 * A withdrawn exercise type (the old listening exercises, should a stale row survive) is never
 * picked. Pure (no Room) so the tiering is unit-testable; [orderedItemIds] order is preserved.
 */
object ReviewExercisePicker {

    fun pick(
        orderedItemIds: List<String>,
        exercisesByItem: Map<String, List<ExerciseEntity>>,
        strengthByItem: Map<String, WordStrength>
    ): List<ExerciseEntity> {
        var contextIndex = 0
        return orderedItemIds.mapNotNull { itemId ->
            val usable = exercisesByItem[itemId].orEmpty().filter { !it.type.isWithdrawn }
            if (usable.isEmpty()) return@mapNotNull null
            val strength = strengthByItem[itemId] ?: WordStrength.NEW
            val preferred = when {
                strength.isStrongOrBetter ->
                    listOf(ExerciseType.FILL_IN_THE_BLANK, ExerciseType.WORD_IN_VERSE_TAP, ExerciseType.MULTIPLE_CHOICE)
                strength == WordStrength.FAMILIAR -> {
                    val inContext = if (contextIndex++ % 2 == 0) {
                        listOf(ExerciseType.FILL_IN_THE_BLANK, ExerciseType.WORD_IN_VERSE_TAP)
                    } else {
                        listOf(ExerciseType.WORD_IN_VERSE_TAP, ExerciseType.FILL_IN_THE_BLANK)
                    }
                    inContext + ExerciseType.MULTIPLE_CHOICE
                }
                else -> listOf(ExerciseType.MULTIPLE_CHOICE, ExerciseType.FILL_IN_THE_BLANK, ExerciseType.WORD_IN_VERSE_TAP)
            }
            preferred.firstNotNullOfOrNull { type -> usable.filter { it.type == type }.minByOrNull { it.id } }
                ?: usable.minByOrNull { it.id }
        }
    }
}
