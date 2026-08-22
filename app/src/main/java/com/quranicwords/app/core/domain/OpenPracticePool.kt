package com.quranicwords.app.core.domain

import com.quranicwords.app.core.data.local.entity.ExerciseEntity

/**
 * Pure helpers behind `ProgressRepository.getOpenPracticeExercises`/streak-recovery sourcing -
 * kept out of the repository method itself so the sampling/collapsing logic is unit-testable
 * without a Room database. Both steps are randomized, so tests check structural invariants
 * (sizes, uniqueness, subset membership) rather than exact output.
 */
object OpenPracticePool {

    /** Randomly samples up to [count] ids from [pool] - fewer than [count] only when [pool]
     * itself has fewer entries than that. */
    fun sampleIds(pool: List<String>, count: Int): List<String> =
        if (pool.size <= count) pool.shuffled() else pool.shuffled().take(count)

    /** Collapses possibly-multiple [ExerciseEntity] rows for the same word down to one each (see
     * [com.quranicwords.app.core.data.local.dao.ExerciseDao.getScoredExercisesForItems]'s doc
     * comment - today always exactly one row per word, but this stays correct if that ever
     * changes), then shuffles final presentation order so a batch doesn't play back in whatever
     * order SQLite happened to return rows. */
    fun oneExercisePerWord(exercises: List<ExerciseEntity>): List<ExerciseEntity> =
        exercises.groupBy { it.practicedItemId }.values.map { it.random() }.shuffled()
}
