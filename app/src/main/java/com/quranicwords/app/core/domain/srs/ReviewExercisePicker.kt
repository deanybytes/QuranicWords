package com.quranicwords.app.core.domain.srs

import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.domain.model.ExerciseType

/**
 * Chooses one exercise per due word for the Daily Review, harder as the memory gets stronger:
 * - New/Learning: recognition (multiple choice).
 * - Familiar: recall in context - fill-in-the-blank or tap-the-word-in-the-verse, alternating so a
 *   session mixes both.
 * - Strong/Mastered: listening (tap what you hear) when the word has verified audio and
 *   pronunciation audio is on, else multiple choice.
 * Each tier falls back through the others, so a word with any scored exercise always gets one.
 * Pure (no Room) so the tiering is unit-testable; [orderedItemIds] order is preserved.
 */
object ReviewExercisePicker {

    fun pick(
        orderedItemIds: List<String>,
        exercisesByItem: Map<String, List<ExerciseEntity>>,
        strengthByItem: Map<String, WordStrength>,
        listeningEnabled: Boolean
    ): List<ExerciseEntity> {
        var contextIndex = 0
        return orderedItemIds.mapNotNull { itemId ->
            val candidates = exercisesByItem[itemId].orEmpty()
            if (candidates.isEmpty()) return@mapNotNull null
            val strength = strengthByItem[itemId] ?: WordStrength.NEW
            val preferred = when {
                strength.isStrongOrBetter && listeningEnabled ->
                    listOf(ExerciseType.TAP_WHAT_YOU_HEAR, ExerciseType.MULTIPLE_CHOICE, ExerciseType.FILL_IN_THE_BLANK, ExerciseType.WORD_IN_VERSE_TAP)
                strength.isStrongOrBetter ->
                    listOf(ExerciseType.MULTIPLE_CHOICE, ExerciseType.FILL_IN_THE_BLANK, ExerciseType.WORD_IN_VERSE_TAP)
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
            // Listening is never a fallback: without audio (or with it switched off) it has no
            // valid interaction.
            val usable = candidates.filter { listeningEnabled || it.type != ExerciseType.TAP_WHAT_YOU_HEAR }
            preferred.firstNotNullOfOrNull { type -> usable.filter { it.type == type }.minByOrNull { it.id } }
                ?: usable.minByOrNull { it.id }
        }
    }
}
