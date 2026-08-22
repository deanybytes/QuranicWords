package com.quranicwords.app.feature.testonlyhome

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.ui.components.DailyGoalBadge
import com.quranicwords.app.core.ui.components.GeometricPatternBackground
import com.quranicwords.app.core.ui.components.PointsBadge
import com.quranicwords.app.core.ui.components.StarfieldMotif
import com.quranicwords.app.core.ui.components.StreakBadge
import com.quranicwords.app.core.ui.components.StreakLockedChip
import com.quranicwords.app.core.ui.motion.pressDepth
import com.quranicwords.app.core.ui.theme.Elevation

/**
 * Home for a Test/Quiz-only learner (see [com.quranicwords.app.core.domain.model.LearningPath
 * .TEST_ONLY]) - no chapter/section/lesson path at all, since this learner already knows the
 * vocabulary and only wants scored practice. Same top status badges as the Learn-path Home
 * ([com.quranicwords.app.feature.home.HomeScreen]), just one hero entry point into
 * `Route.OpenPractice` instead of a curriculum tree.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestOnlyHomeScreen(
    onStartQuiz: () -> Unit,
    onOpenStreakRecovery: () -> Unit,
    viewModel: TestOnlyHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.home_title)) }) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            GeometricPatternBackground(modifier = Modifier.fillMaxSize(), alpha = 0.08f)
            StarfieldMotif(
                modifier = Modifier.fillMaxSize(),
                starCount = 24,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.04f)
            )

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PointsBadge(uiState.totalPoints)
                    if (uiState.isStreakLocked) {
                        StreakLockedChip(questionCount = uiState.streakRecoveryQuestionCount, onClick = onOpenStreakRecovery)
                    } else {
                        StreakBadge(uiState.currentStreak)
                    }
                    DailyGoalBadge(visible = uiState.isDailyGoalMetToday)
                }

                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    StartQuizCard(onClick = onStartQuiz)
                }
            }
        }
    }
}

/** Same "3D box" hero treatment as [com.quranicwords.app.feature.home.HomeScreen]'s
 * `ReviewEntryCard` (see docs/UI_GUIDELINES.md) - the single most prominent thing on this
 * screen, so it earns the full elevated-card-plus-shadow treatment rather than a plain button. */
@Composable
private fun StartQuizCard(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 5.dp)
                .blur(10.dp)
                .background(Color.Black.copy(alpha = 0.18f), shape)
        )
        Card(
            modifier = Modifier.fillMaxWidth().pressDepth(interactionSource),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.floating),
            onClick = onClick,
            interactionSource = interactionSource
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.height(56.dp)
                )
                Text(
                    stringResource(R.string.test_only_start_quiz_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    stringResource(R.string.test_only_start_quiz_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
