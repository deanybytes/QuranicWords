package com.quranicwords.app.feature.testonlyhome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.ui.components.DailyGoalBadge
import com.quranicwords.app.core.ui.components.GeometricPatternBackground
import com.quranicwords.app.core.ui.components.GlassSurface
import com.quranicwords.app.core.ui.components.PointsBadge
import com.quranicwords.app.core.ui.components.QwLogo
import com.quranicwords.app.core.ui.components.QuranStarfieldMotif
import com.quranicwords.app.core.ui.components.StreakBadge
import com.quranicwords.app.core.ui.components.StreakLockedChip
import com.quranicwords.app.core.ui.components.StreakLockedDialog
import com.quranicwords.app.core.ui.components.charts.DonutChart
import com.quranicwords.app.core.ui.components.charts.Home30DayActivityTrendChart
import com.quranicwords.app.core.ui.motion.pressDepth
import com.quranicwords.app.core.ui.motion.rememberReducedGlass
import com.quranicwords.app.core.ui.theme.BrandGold
import java.util.Locale

/**
 * Premium Home for a Test/Quiz-only learner. Matches the Learn module's visual excellence,
 * glassmorphism, 3D glossy press depth, and animations with 3 dedicated practice modes:
 * 1. Frequency Order Mode (sequential Quranic frequency rank)
 * 2. Full Random Mode (non-repeating until 3,680 words are covered)
 * 3. Mistaken Words Review (adaptive retry of missed vocabulary)
 */
@Composable
fun TestOnlyHomeScreen(
    onStartQuiz: (mode: String) -> Unit,
    onOpenReview: () -> Unit,
    onOpenStreakRecovery: () -> Unit,
    onOpenRoadmap: () -> Unit = {},
    onOpenLearnedWords: () -> Unit = {},
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Rich ambient background matching Learn module
        GeometricPatternBackground(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.09f,
            color = MaterialTheme.colorScheme.tertiary,
            tileSize = 48.dp
        )
        QuranStarfieldMotif(
            modifier = Modifier.fillMaxSize(),
            count = 26,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Rich Hero Header matching Learn module
            item {
                TestHeroHeader(
                    totalPoints = uiState.totalPoints,
                    currentStreak = uiState.currentStreak,
                    isStreakLocked = uiState.isStreakLocked,
                    streakRecoveryQuestionCount = uiState.streakRecoveryQuestionCount,
                    isDailyGoalMetToday = uiState.isDailyGoalMetToday,
                    quranCoveragePercent = uiState.quranCoveragePercent,
                    last30DaysMinutes = uiState.last30DaysMinutes,
                    activeDaysCount = uiState.last30DaysActiveCount,
                    totalMinutes = uiState.last30DaysTotalMinutes,
                    onOpenRoadmap = onOpenRoadmap,
                    onOpenStreakRecovery = onOpenStreakRecovery,
                    onOpenLearnedWords = onOpenLearnedWords
                )
            }

            // 2. Mode 1: Frequency Order Mode (3D Glossy Card)
            item {
                GlossyTestModeCard(
                    icon = Icons.Filled.FormatListNumbered,
                    accentColor = MaterialTheme.colorScheme.primary,
                    gradientColors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
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

            // 3. Mode 2: Full Random Mode (3D Glossy Card)
            item {
                GlossyTestModeCard(
                    icon = Icons.Filled.Shuffle,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    gradientColors = listOf(
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
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

            // 4. Mode 3: Mistaken Words Review (3D Glossy Card)
            item {
                GlossyMistakesReviewCard(
                    missedCount = uiState.missedWordsCount,
                    onReview = onOpenReview
                )
            }
        }
    }
}

/**
 * Rich Hero Header with Quran Coverage Donut, 30-Day Activity Chart, Points, Streak, and Daily Goal.
 */
@Composable
private fun TestHeroHeader(
    totalPoints: Int,
    currentStreak: Int,
    isStreakLocked: Boolean,
    streakRecoveryQuestionCount: Int,
    isDailyGoalMetToday: Boolean,
    quranCoveragePercent: Double,
    last30DaysMinutes: List<Int> = emptyList(),
    activeDaysCount: Int = 0,
    totalMinutes: Int = 0,
    onOpenRoadmap: () -> Unit,
    onOpenStreakRecovery: () -> Unit,
    onOpenLearnedWords: () -> Unit = {}
) {
    val shape = RoundedCornerShape(28.dp)
    val reducedGlass = rememberReducedGlass()

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .then(
                    if (reducedGlass) {
                        Modifier
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.32f), Color.Transparent)
                            ),
                            shape = shape
                        )
                    }
                )
        ) {
            Box {
                GeometricPatternBackground(
                    modifier = Modifier.matchParentSize(),
                    color = MaterialTheme.colorScheme.tertiary,
                    alpha = 0.14f,
                    tileSize = 44.dp
                )
                QuranStarfieldMotif(
                    modifier = Modifier.matchParentSize(),
                    count = 12,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f)
                )

                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    // Top Bar Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        QwLogo(size = 40.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.home_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        val roadmapInteractionSource = remember { MutableInteractionSource() }
                        Surface(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = roadmapInteractionSource,
                                    indication = null,
                                    onClick = onOpenRoadmap
                                )
                                .pressDepth(roadmapInteractionSource),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shadowElevation = 0.dp
                        ) {
                            Icon(
                                Icons.Filled.Map,
                                contentDescription = stringResource(R.string.roadmap_title),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Donut Chart & Metrics Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenLearnedWords)
                            .padding(vertical = 4.dp)
                    ) {
                        DonutChart(
                            percent = (quranCoveragePercent / 100.0).toFloat(),
                            size = 68.dp,
                            color = MaterialTheme.colorScheme.tertiary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f),
                            centerLabel = "${formatPercent(quranCoveragePercent)}%",
                            labelStyle = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.progress_quran_coverage),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PointsBadge(totalPoints)
                                if (isStreakLocked) {
                                    StreakLockedChip(questionCount = streakRecoveryQuestionCount, onClick = onOpenStreakRecovery)
                                } else {
                                    StreakBadge(currentStreak)
                                }
                                DailyGoalBadge(visible = isDailyGoalMetToday)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 30-Day Activity Bar / Trend Chart & Time-Spent Spline Curve
                    Home30DayActivityTrendChart(
                        last30DaysMinutes = last30DaysMinutes,
                        activeDaysCount = activeDaysCount,
                        totalMinutes = totalMinutes
                    )
                }
            }
        }
    }
}

/**
 * 3D Glossy Card for Test Modes with tactile press depth, specular glass reflection, and glowing orb.
 */
@Composable
private fun GlossyTestModeCard(
    icon: ImageVector,
    accentColor: Color,
    gradientColors: List<Color>,
    title: String,
    description: String,
    progressText: String,
    progressFraction: Float,
    actionButtonText: String,
    onAction: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val reducedGlass = rememberReducedGlass()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pressDepth(interactionSource)
            .clip(shape)
            .background(Brush.linearGradient(gradientColors))
            .then(
                if (reducedGlass) Modifier else Modifier.border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.08f), Color.Transparent)
                    ),
                    shape = shape
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onAction
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 3D Glowing Icon Orb
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.22f),
                    modifier = Modifier
                        .size(50.dp)
                        .border(1.dp, accentColor.copy(alpha = 0.45f), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }
            }

            // Glossy Progress Section
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.16f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(progressFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }
            }

            // Glossy 3D Button
            val btnInteractionSource = remember { MutableInteractionSource() }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .pressDepth(btnInteractionSource)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = btnInteractionSource,
                        indication = null,
                        onClick = onAction
                    ),
                shape = RoundedCornerShape(14.dp),
                color = accentColor
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)
                            ),
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            actionButtonText,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3D Glossy Card for Mistaken Words Review with dynamic status orb and action button.
 */
@Composable
private fun GlossyMistakesReviewCard(
    missedCount: Int,
    onReview: () -> Unit
) {
    val hasMistakes = missedCount > 0
    val shape = RoundedCornerShape(24.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val reducedGlass = rememberReducedGlass()

    val accentColor = if (hasMistakes) MaterialTheme.colorScheme.error else BrandGold
    val gradientColors = if (hasMistakes) {
        listOf(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (hasMistakes) Modifier.pressDepth(interactionSource) else Modifier)
            .clip(shape)
            .background(Brush.linearGradient(gradientColors))
            .then(
                if (reducedGlass) Modifier else Modifier.border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.08f), Color.Transparent)
                    ),
                    shape = shape
                )
            )
            .then(
                if (hasMistakes) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onReview
                ) else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 3D Glowing Icon Orb
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.22f),
                    modifier = Modifier
                        .size(50.dp)
                        .border(1.dp, accentColor.copy(alpha = 0.45f), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (hasMistakes) Icons.Filled.Refresh else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.test_mode_mistakes_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (hasMistakes) {
                            stringResource(R.string.test_mode_mistakes_count, missedCount)
                        } else {
                            stringResource(R.string.test_mode_mistakes_empty)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (hasMistakes) MaterialTheme.colorScheme.error else BrandGold
                    )
                }
            }

            Text(
                text = stringResource(R.string.test_mode_mistakes_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hasMistakes) {
                val btnInteractionSource = remember { MutableInteractionSource() }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .pressDepth(btnInteractionSource)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = btnInteractionSource,
                            indication = null,
                            onClick = onReview
                        ),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.error
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)
                                ),
                                RoundedCornerShape(14.dp)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.test_mode_review_btn),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.test_mode_mistakes_empty))
                }
            }
        }
    }
}

private fun formatPercent(percent: Double): String = String.format(Locale.US, "%.1f", percent)
