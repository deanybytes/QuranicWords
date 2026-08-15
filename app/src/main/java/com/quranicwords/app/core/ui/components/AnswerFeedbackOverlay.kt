package com.quranicwords.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.ui.motion.rememberReducedMotion
import com.quranicwords.app.core.ui.theme.Elevation

/**
 * The big, celebratory (or gently encouraging) moment shown right after checking an answer -
 * distinct from [FeedbackBanner], which stays around to show *what the correct answer was*.
 * This overlay is the emotional beat: a scaled-in badge over a soft color scrim, "Alhamdulillah!"
 * on correct with a light confetti burst, "In-Sha-Allah, next time!" on wrong with a gentle
 * shake and no confetti (per [CelebrationBurst]'s own guardrail against overusing celebration).
 * Purely decorative chrome, positioned over gamification UI - never over verse/Arabic text.
 */
@Composable
fun AnswerFeedbackOverlay(type: FeedbackType?, modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    AnimatedVisibility(
        visible = type != null,
        enter = fadeIn(tween(150)) + scaleIn(tween(220), initialScale = 0.7f),
        exit = fadeOut(tween(150)),
        modifier = modifier
    ) {
        val isCorrect = type == FeedbackType.CORRECT
        val scrimColor = if (isCorrect) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.error.copy(alpha = 0.06f)
        }
        val badgeColor = if (isCorrect) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        val onBadgeColor = if (isCorrect) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer

        val shake = remember { Animatable(0f) }
        LaunchedEffect(type) {
            if (type == FeedbackType.INCORRECT && !reducedMotion) {
                shake.snapTo(0f)
                listOf(10f, -8f, 6f, -4f, 0f).forEach { target ->
                    shake.animateTo(target, animationSpec = tween(60))
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().background(scrimColor),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isCorrect) {
                CelebrationBurst(intensity = CelebrationIntensity.PASSED)
            }
            Surface(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .graphicsLayer { translationX = shake.value },
                shape = RoundedCornerShape(50),
                color = badgeColor,
                shadowElevation = Elevation.floating
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = onBadgeColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(
                            if (isCorrect) R.string.lesson_feedback_correct_alhamdulillah
                            else R.string.lesson_feedback_incorrect_tryagain
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = onBadgeColor,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
        }
    }
}
