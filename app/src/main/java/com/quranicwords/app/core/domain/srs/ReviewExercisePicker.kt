package com.quranicwords.app.core.domain.srs

import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.isWithdrawn

/**
 * Chooses one exercise per due word for the Daily Review. Every word is reviewed with multiple
 * choice; for a stronger memory the word's second multiple-choice variant (fresh distractors) is
 * preferred, so a familiar word is not met with the same four options it saw in its lesson.
 * A withdrawn exercise type (a stale row of a removed type) is never picked. Pure (no Room) so
 * the choice is unit-testable; [orderedItemIds] order is preserved.
 */
object ReviewExercisePicker {

    fun pick(
        orderedItemIds: List<String>,
        exercisesByItem: Map<String, List<ExerciseEntity>>,
        strengthByItem: Map<String, WordStrength>
    ): List<ExerciseEntity> = orderedItemIds.mapNotNull { itemId ->
        val usable = exercisesByItem[itemId].orEmpty().filter { !it.type.isWithdrawn }
        if (usable.isEmpty()) return@mapNotNull null
        val strength = strengthByItem[itemId] ?: WordStrength.NEW
        val choices = usable.filter { it.type == ExerciseType.MULTIPLE_CHOICE }.sortedBy { it.id }
        val picked = if (strength.isLearned) choices.lastOrNull() else choices.firstOrNull()
        picked ?: usable.minByOrNull { it.id }
    }
}
