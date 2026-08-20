package com.quranicwords.app.core.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quranicwords.app.core.ui.motion.pressDepth

/**
 * Primary call-to-action button. Height matches the 48dp minimum touch target for accessibility.
 * Carries the shared `pressDepth` tilt/scale feedback (see `docs/UI_GUIDELINES.md`) so every
 * screen that uses this component gets the "3D box" press feel for free, rather than each screen
 * re-implementing it inline.
 */
@Composable
fun QwPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .pressDepth(interactionSource),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        shape = ButtonDefaults.shape
    ) {
        Text(text)
    }
}

@Composable
fun QwSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .pressDepth(interactionSource),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text)
    }
}
