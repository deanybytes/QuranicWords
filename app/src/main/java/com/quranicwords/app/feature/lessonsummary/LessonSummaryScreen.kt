package com.quranicwords.app.feature.lessonsummary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.navigation.Route
import com.quranicwords.app.core.ui.components.CelebrationBurst
import com.quranicwords.app.core.ui.components.CelebrationIntensity
import com.quranicwords.app.core.ui.components.PointsBadge
import com.quranicwords.app.core.ui.components.StreakFlame
import com.quranicwords.app.core.ui.components.StreakBadge
import com.quranicwords.app.core.ui.components.Qw3DFlipCard
import com.quranicwords.app.core.ui.components.QwPrimaryButton
import com.quranicwords.app.core.ui.components.StaggeredEntrance
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.theme.MedallionShapeDefault
import com.quranicwords.app.core.util.GamificationConfig

@Composable
fun LessonSummaryScreen(route: Route.LessonSummary, onContinue: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val animatedPointsEarned by animateIntAsState(
        targetValue = if (visible) route.pointsAwarded else 0,
        animationSpec = MotionSpecs.countUp,
        label = "pointsEarned"
    )
    val passed = route.accuracyPercent >= GamificationConfig.PASSING_SCORE_PERCENT
    val celebrationIntensity = if (route.accuracyPercent >= 100) CelebrationIntensity.PERFECT else CelebrationIntensity.PASSED

    Box(modifier = Modifier.fillMaxSize()) {
        // Fired once (visible only ever flips false->true, never back), scaled by lesson
        // accuracy - never overlaid on Arabic/verse content, only on this gamification screen.
        // No confetti on a below-threshold score - reserved for genuine celebration, same
        // guardrail AnswerFeedbackOverlay applies per-question.
        if (visible && passed) {
            CelebrationBurst(intensity = celebrationIntensity, modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StaggeredEntrance(index = 0) {
                Text(
                    stringResource(if (passed) R.string.lesson_summary_title_pass else R.string.lesson_summary_title_retry),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            StaggeredEntrance(index = 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Qw3DFlipCard(
                        flipped = visible,
                        front = { BadgeMedallionPlaceholder() },
                        back = { PointsBadge(route.newTotalPoints) }
                    )
                    Qw3DFlipCard(
                        flipped = visible,
                        front = { BadgeMedallionPlaceholder() },
                        back = { StreakBadge(route.currentStreak) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            StaggeredEntrance(index = 2) {
                Text(
                    stringResource(R.string.lesson_summary_points_earned, animatedPointsEarned),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            StaggeredEntrance(index = 3) {
                Text(
                    stringResource(R.string.lesson_summary_accuracy, route.accuracyPercent),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (route.streakIncreased) {
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn(initialScale = 0.7f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StreakFlame(streakDays = route.currentStreak)
                        Text(
                            stringResource(R.string.lesson_summary_streak_increased),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            StaggeredEntrance(index = 4) {
                QwPrimaryButton(
                    text = stringResource(
                        if (route.nextLessonId != null) R.string.lesson_summary_continue
                        else R.string.lesson_summary_back_to_home
                    ),
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Blank "unrevealed" face shown by [Qw3DFlipCard] before it flips to the real badge - the
 * scalloped medallion shape reads as a coin/medallion about to be revealed. */
@Composable
private fun BadgeMedallionPlaceholder() {
    Surface(
        modifier = Modifier.size(56.dp),
        shape = MedallionShapeDefault,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {}
}

private val Route.LessonSummary.accuracyPercent: Int
    get() = GamificationConfig.percentOf(correctCount, totalCount)
