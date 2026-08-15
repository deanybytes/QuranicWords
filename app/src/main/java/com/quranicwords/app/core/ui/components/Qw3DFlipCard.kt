package com.quranicwords.app.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.motion.rememberReducedMotion
import kotlin.math.abs

/**
 * Reusable 3D flip-card: shows [front] until [flipped] becomes true, then rotates 180deg around
 * the Y axis to reveal [back]. Content is swapped at exactly 90deg (edge-on, so the swap is
 * invisible), and `scaleX` is flipped to -1 past 90deg to compensate for the mirror-image artifact
 * `graphicsLayer`'s `rotationY` otherwise produces. Used for lesson-summary badge reveals. Under
 * reduced motion, the flip is instant.
 */
@Composable
fun Qw3DFlipCard(
    flipped: Boolean,
    modifier: Modifier = Modifier,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit
) {
    val reducedMotion = rememberReducedMotion()
    val density = LocalDensity.current
    val targetAngle = if (flipped) 180f else 0f
    val angle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = if (reducedMotion) tween(durationMillis = 0) else MotionSpecs.celebratory(),
        label = "flipCardAngle"
    )

    Box(
        modifier = modifier.graphicsLayer {
            rotationY = angle
            cameraDistance = 12f * density.density
            scaleX = if (abs(angle) > 90f) -1f else 1f
        }
    ) {
        if (angle <= 90f) front() else back()
    }
}
