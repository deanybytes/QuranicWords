package com.quranicwords.app.core.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

/**
 * Shared "3D box" press feedback: a slight scale-down + forward tilt (`graphicsLayer` rotationX +
 * cameraDistance) on press, springing back on release - the app's depth language, since Compose
 * has no true 3D primitive (see [com.quranicwords.app.core.ui.theme.Elevation]'s doc comment).
 * Centralizes what was previously duplicated per-component (e.g. the old inline version in
 * `QwSelectableCard`) into one modifier any interactive surface can chain on.
 *
 * [interactionSource] must be the same instance passed to the surface's own `clickable`/
 * `selectable`/etc. so press state is read from the real gesture, not a separate one. Collapses
 * to a flat, untilted state under reduced motion.
 */
fun Modifier.pressDepth(
    interactionSource: InteractionSource,
    tiltDegrees: Float = 4f,
    scaleDown: Float = 0.97f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val density = LocalDensity.current
    val pressSpec = MotionSpecs.snappy<Float>()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = pressSpec,
        label = "pressDepthScale"
    )
    val tiltX by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) -tiltDegrees else 0f,
        animationSpec = pressSpec,
        label = "pressDepthTilt"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        rotationX = tiltX
        cameraDistance = 12f * density.density
    }
}
