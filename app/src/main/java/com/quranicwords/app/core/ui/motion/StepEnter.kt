package com.quranicwords.app.core.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Plays an entrance for [content] every time [stepKey] changes: a short slide in from the reading
 * end plus a fade, so moving through a lesson reads as a forward step rather than a hard cut.
 * Entrance-only on purpose - an outgoing copy would be drawn with the new step's state. Instant
 * under reduced motion.
 */
@Composable
fun StepEnter(stepKey: Any, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val progress = remember(stepKey) { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(stepKey) { progress.animateTo(1f, MotionSpecs.enter()) }
    val shift = with(LocalDensity.current) { 28.dp.toPx() }
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    Box(
        modifier.graphicsLayer {
            alpha = progress.value
            translationX = (1f - progress.value) * shift * direction
        }
    ) {
        content()
    }
}
