package com.quranicwords.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.ui.motion.rememberReducedGlass
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
    val reducedGlass = rememberReducedGlass()
    // Freeze on the last non-null type so the 150ms fadeOut exit renders the answer that was
    // actually given, not `type` mid-transition to null - AnimatedVisibility keeps its content
    // recomposing during exit, and a CORRECT->null transition would otherwise land on the wrong
    // (`isCorrect = false`) branch for that whole window, flashing an incorrect-answer badge
    // after a correct answer right before Continue advances.
    var lastNonNullType by remember { mutableStateOf(type) }
    if (type != null) lastNonNullType = type
    AnimatedVisibility(
        visible = type != null,
        enter = fadeIn(tween(150)) + scaleIn(tween(220), initialScale = 0.7f),
        exit = fadeOut(tween(150)),
        modifier = modifier
    ) {
        val isCorrect = lastNonNullType == FeedbackType.CORRECT
        val scrimColor = if (isCorrect) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.error.copy(alpha = 0.06f)
        }
        val badgeColor = if (isCorrect) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        val onBadgeColor = if (isCorrect) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer

        val shake = remember { Animatable(0f) }
        // One-shot sweep (not a looping ambient sheen like QwLogo's) - plays once as the correct-
        // answer badge appears, reusing that same clip-to-shape + screen-blend technique.
        val sheenProgress = remember { Animatable(0f) }
        LaunchedEffect(type) {
            if (type == FeedbackType.INCORRECT && !reducedMotion) {
                shake.snapTo(0f)
                listOf(10f, -8f, 6f, -4f, 0f).forEach { target ->
                    shake.animateTo(target, animationSpec = tween(60))
                }
            }
            if (type == FeedbackType.CORRECT && !reducedMotion && !reducedGlass) {
                sheenProgress.snapTo(0f)
                sheenProgress.animateTo(1f, animationSpec = tween(durationMillis = 900, easing = LinearEasing))
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().background(scrimColor),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 24.dp)
                    .graphicsLayer { translationX = shake.value },
                shape = RoundedCornerShape(50),
                color = badgeColor,
                shadowElevation = Elevation.floating
            ) {
                Box {
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
                    // Glossy sheen, same technique as QwLogo's: a bright diagonal band,
                    // screen-blended over the badge and clipped to its own pill shape.
                    if (isCorrect && !reducedMotion && !reducedGlass) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(50))
                                .drawBehind {
                                    val w = size.width
                                    val h = size.height
                                    val bandWidth = w * 0.35f
                                    val travel = w * 1.8f
                                    val bandCenter = -w * 0.4f + sheenProgress.value * travel
                                    rotate(degrees = 20f, pivot = Offset(w / 2f, h / 2f)) {
                                        drawRect(
                                            brush = Brush.linearGradient(
                                                colorStops = arrayOf(
                                                    0f to Color.Transparent,
                                                    0.5f to Color.White.copy(alpha = 0.65f),
                                                    1f to Color.Transparent
                                                ),
                                                start = Offset(bandCenter - bandWidth / 2f, 0f),
                                                end = Offset(bandCenter + bandWidth / 2f, 0f)
                                            ),
                                            topLeft = Offset(-w, -h),
                                            size = Size(w * 3f, h * 3f),
                                            blendMode = BlendMode.Screen
                                        )
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}
