package com.quranicwords.app.core.ui.components

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import com.quranicwords.app.core.ui.motion.pressDepth
import com.quranicwords.app.core.ui.theme.Elevation

/**
 * A full-width option card that scales down and tilts into a subtle pseudo-3D angle on press
 * (see [pressDepth]), shared by the onboarding selection screens (language/font) so a single tap
 * always feels responsive rather than static Material defaults.
 */
@Composable
fun QwSelectableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Card(
        modifier = modifier
            .pressDepth(interactionSource)
            .selectable(
                selected = false,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                role = Role.Button
            )
            // Without this, the card's own accessibility node carries no name of its own -
            // the visible label lives in a separate, non-focusable child Text - so TalkBack
            // announced every language/font option as an unlabeled "Button". mergeDescendants
            // folds that child text into this node's spoken name. Found via the QW-16 emulator
            // TalkBack audit (uiautomator dump showed content-desc="" on every option card).
            .semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPressed) Elevation.pressed else Elevation.raised)
    ) {
        content()
    }
}
