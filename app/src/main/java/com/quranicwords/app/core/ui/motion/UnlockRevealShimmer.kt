package com.quranicwords.app.core.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import com.quranicwords.app.core.data.local.entity.LessonStatus

/**
 * Shared "just unlocked" glow, extracted from what was originally inline/private to `HomeScreen`'s
 * lesson-node composable so chapter- and section-summary nodes (added for the collapse/expand
 * branching tree, QW-22) can reuse the identical state machine instead of three copies of it.
 * Detects the actual LOCKED->non-LOCKED transition (not just "this happens to be unlocked when it
 * first composes") so the glow only plays once, right when it happens - never replayed on
 * scroll-in/out of a lazy-list item or on an expand/collapse toggle. No-ops under reduced motion.
 */
fun Modifier.unlockRevealShimmer(status: LessonStatus, glowColor: Color): Modifier = composed {
    val reducedMotion = rememberReducedMotion()
    var previousStatus by remember { mutableStateOf<LessonStatus?>(null) }
    var justUnlocked by remember { mutableStateOf(false) }
    LaunchedEffect(status) {
        if (previousStatus == LessonStatus.LOCKED && status != LessonStatus.LOCKED && !reducedMotion) {
            justUnlocked = true
        }
        previousStatus = status
    }
    val shimmer = remember { Animatable(0f) }
    LaunchedEffect(justUnlocked) {
        if (justUnlocked) {
            shimmer.snapTo(1f)
            shimmer.animateTo(0f, animationSpec = tween(durationMillis = 600))
            justUnlocked = false
        }
    }

    this.drawBehind {
        if (shimmer.value > 0f) {
            drawCircle(
                color = glowColor,
                radius = (size.minDimension / 2f) * (1f + shimmer.value * 0.7f),
                alpha = shimmer.value * 0.45f
            )
        }
    }
}
