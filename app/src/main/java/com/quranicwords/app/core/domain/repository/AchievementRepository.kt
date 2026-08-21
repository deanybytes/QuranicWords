package com.quranicwords.app.core.domain.repository

import com.quranicwords.app.core.data.local.entity.AchievementEntity
import com.quranicwords.app.core.domain.AchievementDef
import kotlinx.coroutines.flow.Flow

interface AchievementRepository {
    fun observeUnlocked(userId: String): Flow<List<AchievementEntity>>

    /**
     * Re-checks every [com.quranicwords.app.core.domain.AchievementCatalog] entry against
     * [userId]'s current stats/progress and unlocks any that have newly crossed their threshold.
     * Idempotent - safe to call after every lesson/exam/Review completion regardless of whether
     * anything actually changed. Returns only the ones that unlocked *this call* (empty if none),
     * so the caller can drive a "newly unlocked" reveal without re-deriving that from the full set.
     */
    suspend fun checkAndUnlock(userId: String): List<AchievementDef>
}
