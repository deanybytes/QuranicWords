package com.quranicwords.app.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.motion.rememberReducedMotion

/**
 * A full-width option card that scales down and tilts into a subtle pseudo-3D angle on press
 * (`graphicsLayer` rotationX + cameraDistance, springing back on release), shared by the
 * onboarding selection screens (language/auth/tier/font) so a single tap always feels responsive
 * rather than static Material defaults. Tilt/scale collapse to a flat press state under reduced
 * motion.
 */
@Composable
fun QwSelectableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val density = LocalDensity.current
    val pressSpec = MotionSpecs.snappy<Float>()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = pressSpec,
        label = "cardPressScale"
    )
    val tiltX by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) -4f else 0f,
        animationSpec = pressSpec,
        label = "cardPressTiltX"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationX = tiltX
                cameraDistance = 12f * density.density
            }
            .selectable(
                selected = false,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                role = Role.Button
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPressed) 0.dp else 2.dp)
    ) {
        content()
    }
}
