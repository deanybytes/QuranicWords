package com.quranicwords.app.core.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.quranicwords.app.core.ui.motion.pressDepth
import com.quranicwords.app.core.ui.motion.selectionBounce

/**
 * A drop-in replacement for Material3's `IconButton` that adds this app's "3D box" press feedback
 * ([Modifier.pressDepth]) plus a bouncy scale-up while [selected] is true (reusing
 * [MotionSpecs.celebratory] rather than a bespoke spring, matching the app's existing "bouncy,
 * attention-grabbing" vocabulary). [selected] defaults to false for plain (non-toggle) icon
 * buttons, which then only get the press-depth feedback. Collapses to a flat, unscaled state
 * under reduced motion via the same two building blocks it composes.
 */
@Composable
fun QwIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    icon: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        modifier = modifier
            .pressDepth(interactionSource)
            .selectionBounce(selected),
        enabled = enabled,
        interactionSource = interactionSource,
        content = icon
    )
}
