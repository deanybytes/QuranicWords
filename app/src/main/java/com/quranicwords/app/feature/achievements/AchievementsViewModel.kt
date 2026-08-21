package com.quranicwords.app.feature.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.domain.AchievementCatalog
import com.quranicwords.app.core.domain.AchievementDef
import com.quranicwords.app.core.domain.repository.AchievementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AchievementUiItem(
    val def: AchievementDef,
    val unlocked: Boolean,
    val unlockedAtEpochMillis: Long? = null
)

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val items: List<AchievementUiItem> = emptyList()
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementRepository: AchievementRepository,
    private val userIdProvider: CurrentUserIdProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = userIdProvider.get()
            achievementRepository.observeUnlocked(userId).collectLatest { unlocked ->
                val unlockedById = unlocked.associateBy { it.achievementId }
                val items = AchievementCatalog.all.map { def ->
                    val row = unlockedById[def.id]
                    AchievementUiItem(def = def, unlocked = row != null, unlockedAtEpochMillis = row?.unlockedAtEpochMillis)
                }
                _uiState.value = AchievementsUiState(isLoading = false, items = items)
            }
        }
    }
}
