package com.quranicwords.app.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.quranicwords.app.R
import com.quranicwords.app.feature.home.HomeScreen
import com.quranicwords.app.feature.progress.ProgressScreen
import com.quranicwords.app.feature.settings.SettingsScreen

private enum class BottomTab { HOME, PROGRESS, SETTINGS }

/**
 * The app's first-ever bottom navigation bar: Home / Progress / Settings. Tab switches happen
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
    onOpenAchievements: () -> Unit,
    onOpenRoadmap: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }
    var expandedChapterIds by remember { mutableStateOf<Set<String>?>(null) }
    var expandedSectionIds by remember { mutableStateOf<Set<String>?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == BottomTab.HOME,
                    onClick = { selectedTab = BottomTab.HOME },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.bottom_nav_home)) }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.PROGRESS,
                    onClick = { selectedTab = BottomTab.PROGRESS },
                    icon = { Icon(Icons.Filled.Insights, contentDescription = null) },
                    label = { Text(stringResource(R.string.bottom_nav_progress)) }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.SETTINGS,
                    onClick = { selectedTab = BottomTab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.home_open_settings)) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                BottomTab.HOME -> HomeScreen(
                    onOpenLesson = onOpenLesson,
                    onOpenReview = onOpenReview,
                    onOpenChapterIntro = onOpenChapterIntro,
                    onOpenSectionIntro = onOpenSectionIntro,
                    onOpenWordBrowse = onOpenWordBrowse,
                    onOpenAchievements = onOpenAchievements,
                    onOpenRoadmap = onOpenRoadmap,
                    expandedChapterIds = expandedChapterIds,
                    onExpandedChapterIdsChange = { expandedChapterIds = it },
                    expandedSectionIds = expandedSectionIds,
                    onExpandedSectionIdsChange = { expandedSectionIds = it }
                )
                BottomTab.PROGRESS -> ProgressScreen()
                BottomTab.SETTINGS -> SettingsScreen(onBack = {}, showBackButton = false)
            }
        }
    }
}
