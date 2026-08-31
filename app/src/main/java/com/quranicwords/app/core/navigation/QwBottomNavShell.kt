package com.quranicwords.app.core.navigation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.LearningPath
import com.quranicwords.app.core.ui.motion.rememberReducedGlass
import com.quranicwords.app.core.ui.motion.selectionBounce
import com.quranicwords.app.feature.about.AboutScreen
import com.quranicwords.app.feature.home.HomeScreen
import com.quranicwords.app.feature.progress.ProgressScreen
import com.quranicwords.app.feature.settings.SettingsScreen
import com.quranicwords.app.feature.testonlyhome.TestOnlyHomeScreen

private enum class BottomTab { HOME, PROGRESS, ABOUT, SETTINGS }

/**
 * The app's first-ever bottom navigation bar: Home / Progress / About / Settings. Tab switches happen
 * entirely inside this composable (not via Navigation-Compose route pushes), so a tab switch
 * never grows the back stack - the whole point of a tab bar. Lesson-taking, onboarding, and
 * chapter/section-intro routes stay outside this shell entirely (see `QwNavHost`'s `Route.Home`
 * entry, the only one that renders this) - a lesson in progress never shows a bottom tab bar.
 *
 * `expandedChapterIds`/`expandedSectionIds` are hoisted here (not left as [HomeScreen]-internal
 * state) specifically so switching away from Home and back doesn't lose the learner's manual
 * collapse/expand choices - `when (selectedTab)` disposes the non-selected branch's composition,
 * which would otherwise reset any `remember`-only state inside it on every tab switch.
 */
@Composable
fun QwBottomNavShell(
    onOpenLesson: (String) -> Unit,
    onOpenReview: () -> Unit,
    onOpenChapterIntro: (String) -> Unit,
    onOpenSectionIntro: (String) -> Unit,
    onOpenWordBrowse: (String) -> Unit,
    onOpenRoadmap: () -> Unit,
    onOpenOpenPractice: (String) -> Unit,
    onOpenStreakRecovery: () -> Unit,
    onOpenLearnedWords: () -> Unit = {},
    viewModel: QwBottomNavShellViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }
    var expandedChapterIds by remember { mutableStateOf<Set<String>?>(null) }
    var expandedSectionIds by remember { mutableStateOf<Set<String>?>(null) }
    val learningPath by viewModel.learningPath.collectAsStateWithLifecycle()
    val currentLessonId by viewModel.currentLessonId.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            // Rounded top corners, mirroring HomeHeroHeader's rounded-bottom panel - bookends the
            // screen between two rounded panels instead of one rounded, one sharp. Translucent
            // container color (falls back to opaque under "reduce glossy effects") so the
            // ambient pattern reads faintly through, plus the same glass-edge gradient border
            // highlight every other glass surface uses - gives the bar the "3D glass" look
            // without a drop shadow (shadows are off across every box in this app now).
            val reducedGlass = rememberReducedGlass()
            val navShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            NavigationBar(
                modifier = Modifier
                    .clip(navShape)
                    .then(
                        if (reducedGlass) {
                            Modifier
                        } else {
                            Modifier.border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)
                                ),
                                shape = navShape
                            )
                        }
                    ),
                containerColor = if (reducedGlass) {
                    MaterialTheme.colorScheme.surfaceContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
                }
            ) {
                // Icon-only (no `label`) per feedback - contentDescription still carries each
                // tab's name for TalkBack even though it's no longer shown visually.
                NavigationBarItem(
                    selected = selectedTab == BottomTab.HOME,
                    onClick = { selectedTab = BottomTab.HOME },
                    icon = {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = stringResource(R.string.bottom_nav_home),
                            modifier = Modifier.selectionBounce(selectedTab == BottomTab.HOME)
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.PROGRESS,
                    onClick = { selectedTab = BottomTab.PROGRESS },
                    icon = {
                        Icon(
                            Icons.Filled.Insights,
                            contentDescription = stringResource(R.string.bottom_nav_progress),
                            modifier = Modifier.selectionBounce(selectedTab == BottomTab.PROGRESS)
                        )
                    }
                )
                // Center "Continue Learning" action: always visible across all tabs (Home, Progress,
                // About, Settings) as long as there is an unlocked lesson to progress in.
                if (currentLessonId != null) {
                    NavigationBarItem(
                        selected = false,
                        onClick = { onOpenLesson(currentLessonId!!) },
                        icon = {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.home_continue_learning)
                            )
                        }
                    )
                }
                NavigationBarItem(
                    selected = selectedTab == BottomTab.ABOUT,
                    onClick = { selectedTab = BottomTab.ABOUT },
                    icon = {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = stringResource(R.string.bottom_nav_about),
                            modifier = Modifier.selectionBounce(selectedTab == BottomTab.ABOUT)
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.SETTINGS,
                    onClick = { selectedTab = BottomTab.SETTINGS },
                    icon = {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.home_open_settings),
                            modifier = Modifier.selectionBounce(selectedTab == BottomTab.SETTINGS)
                        )
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                BottomTab.HOME -> if (learningPath == LearningPath.TEST_ONLY) {
                    TestOnlyHomeScreen(
                        onStartQuiz = onOpenOpenPractice,
                        onOpenReview = onOpenReview,
                        onOpenStreakRecovery = onOpenStreakRecovery,
                        onOpenRoadmap = onOpenRoadmap,
                        onOpenLearnedWords = onOpenLearnedWords
                    )
                } else {
                    HomeScreen(
                        onOpenLesson = onOpenLesson,
                        onOpenReview = onOpenReview,
                        onOpenChapterIntro = onOpenChapterIntro,
                        onOpenSectionIntro = onOpenSectionIntro,
                        onOpenWordBrowse = onOpenWordBrowse,
                        onOpenRoadmap = onOpenRoadmap,
                        onOpenOpenPractice = { onOpenOpenPractice("RANDOM") },
                        onOpenStreakRecovery = onOpenStreakRecovery,
                        onOpenLearnedWords = onOpenLearnedWords,
                        expandedChapterIds = expandedChapterIds,
                        onExpandedChapterIdsChange = { expandedChapterIds = it },
                        expandedSectionIds = expandedSectionIds,
                        onExpandedSectionIdsChange = { expandedSectionIds = it }
                    )
                }
                BottomTab.PROGRESS -> ProgressScreen(onOpenLearnedWords = onOpenLearnedWords)
                BottomTab.ABOUT -> AboutScreen()
                BottomTab.SETTINGS -> SettingsScreen(onBack = {}, showBackButton = false)
            }
        }
    }
}
