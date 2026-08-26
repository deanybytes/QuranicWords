package com.quranicwords.app.feature.testonlyhome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.ui.components.DailyGoalBadge
import com.quranicwords.app.core.ui.components.GeometricPatternBackground
import com.quranicwords.app.core.ui.components.GlassSurface
import com.quranicwords.app.core.ui.components.PointsBadge
import com.quranicwords.app.core.ui.components.StarfieldMotif
import com.quranicwords.app.core.ui.components.StreakBadge
import com.quranicwords.app.core.ui.components.StreakLockedChip
import com.quranicwords.app.core.ui.components.StreakLockedDialog
import com.quranicwords.app.core.ui.theme.BrandGold

/**
 * Home for a Test/Quiz-only learner with 3 dedicated practice modes:
 * 1. Frequency Order Mode (sequential Quranic frequency rank)
 * 2. Full Random Mode (non-repeating until 3,680 words are covered)
 * 3. Mistaken Words Review (adaptive retry of missed vocabulary)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestOnlyHomeScreen(
    onStartQuiz: (mode: String) -> Unit,
    onOpenReview: () -> Unit,
    onOpenStreakRecovery: () -> Unit,
    viewModel: TestOnlyHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var streakLockedDialogDismissed by rememberSaveable(uiState.isStreakLocked) { mutableStateOf(false) }
    if (uiState.isStreakLocked && !streakLockedDialogDismissed) {
        StreakLockedDialog(
            inactivityDuration = uiState.streakInactivityDuration,
            onTakeTest = {
                streakLockedDialogDismissed = true
                onOpenStreakRecovery()
            },
            onDismiss = { streakLockedDialogDismissed = true }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.home_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            GeometricPatternBackground(modifier = Modifier.fillMaxSize(), alpha = 0.08f)
            StarfieldMotif(
                modifier = Modifier.fillMaxSize(),
                starCount = 24,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.04f)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Status Badges Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PointsBadge(uiState.totalPoints)
                        if (uiState.isStreakLocked) {
                            StreakLockedChip(questionCount = uiState.streakRecoveryQuestionCount, onClick = onOpenStreakRecovery)
                        } else {
                            StreakBadge(uiState.currentStreak)
                        }
                        DailyGoalBadge(visible = uiState.isDailyGoalMetToday)
                    }
                }

                // Mode 1: Frequency Order Mode
                item {
                    TestModeCard(
                        icon = Icons.Filled.FormatListNumbered,
                        iconTint = MaterialTheme.colorScheme.primary,
                        containerTint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        title = stringResource(R.string.test_mode_frequency_title),
                        description = stringResource(R.string.test_mode_frequency_desc),
                        progressText = stringResource(
                            R.string.test_mode_frequency_progress,
                            uiState.frequencyCoveredCount,
                            uiState.totalWordsCount
                        ),
                        progressFraction = if (uiState.totalWordsCount > 0) {
                            (uiState.frequencyCoveredCount.toFloat() / uiState.totalWordsCount).coerceIn(0f, 1f)
                        } else 0f,
                        actionButtonText = stringResource(R.string.test_mode_start_btn),
                        onAction = { onStartQuiz("FREQUENCY") }
                    )
                }

                // Mode 2: Full Random Mode
                item {
                    TestModeCard(
                        icon = Icons.Filled.Shuffle,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        containerTint = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        title = stringResource(R.string.test_mode_random_title),
                        description = stringResource(R.string.test_mode_random_desc),
                        progressText = stringResource(
                            R.string.test_mode_random_progress,
                            uiState.randomCoveredCount,
                            uiState.totalWordsCount
                        ),
                        progressFraction = if (uiState.totalWordsCount > 0) {
                            (uiState.randomCoveredCount.toFloat() / uiState.totalWordsCount).coerceIn(0f, 1f)
                        } else 0f,
                        actionButtonText = stringResource(R.string.test_mode_start_btn),
                        onAction = { onStartQuiz("RANDOM") }
                    )
                }

                // Mode 3: Mistaken Words Review
                item {
                    MistakesReviewCard(
                        missedCount = uiState.missedWordsCount,
                        onReview = onOpenReview
                    )
                }
            }
        }
    }
}

@Composable
private fun TestModeCard(
    icon: ImageVector,
    iconTint: Color,
    containerTint: Color,
    title: String,
    description: String,
    progressText: String,
    progressFraction: Float,
    actionButtonText: String,
    onAction: () -> Unit
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAction),
        shape = RoundedCornerShape(22.dp),
        tint = containerTint
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.18f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = iconTint,
                    trackColor = iconTint.copy(alpha = 0.2f)
                )
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iconTint),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(actionButtonText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MistakesReviewCard(
    missedCount: Int,
    onReview: () -> Unit
) {
    val hasMistakes = missedCount > 0
    val tint = if (hasMistakes) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val iconColor = if (hasMistakes) MaterialTheme.colorScheme.error else BrandGold

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (hasMistakes) Modifier.clickable(onClick = onReview) else Modifier),
        shape = RoundedCornerShape(22.dp),
        tint = tint
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.18f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (hasMistakes) Icons.Filled.Refresh else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = stringResource(R.string.test_mode_mistakes_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (hasMistakes) {
                            stringResource(R.string.test_mode_mistakes_count, missedCount)
                        } else {
                            stringResource(R.string.test_mode_mistakes_empty)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasMistakes) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = stringResource(R.string.test_mode_mistakes_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hasMistakes) {
                Button(
                    onClick = onReview,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.test_mode_review_btn), fontWeight = FontWeight.SemiBold)
                }
            } else {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.test_mode_mistakes_empty))
                }
            }
        }
    }
}
