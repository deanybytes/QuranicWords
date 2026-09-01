package com.quranicwords.app.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.model.LearningPath
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.domain.repository.ProgressRepository
import com.quranicwords.app.feature.home.ChapterWithSections
import com.quranicwords.app.feature.home.SectionWithLessons
import com.quranicwords.app.feature.home.findCurrentLessonId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Shell view model providing live learning path and current lesson id to [QwBottomNavShell]
 * so the center "Continue Learning" action is always accessible across all tabs (Home, Progress,
 * About, Settings).
 */
@HiltViewModel
class QwBottomNavShellViewModel @Inject constructor(
    preferences: UserPreferencesDataStore,
    contentRepository: ContentRepository,
    progressRepository: ProgressRepository,
    userIdProvider: CurrentUserIdProvider
) : ViewModel() {
    val learningPath: StateFlow<LearningPath> = preferences.learningPathFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearningPath.DEFAULT)

    val currentLessonId: StateFlow<String?> = flow {
        val userId = userIdProvider.get()
        progressRepository.ensureCurriculumStarted(userId)
        val chapters = contentRepository.getFullCurriculumTree()
        progressRepository.observeProgress(userId).collect { progressList ->
            val progressByLessonId = progressList.associateBy { it.lessonId }
            emit(findCurrentLessonId(chapters, progressByLessonId))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

}
