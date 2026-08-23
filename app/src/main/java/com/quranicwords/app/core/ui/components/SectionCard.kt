package com.quranicwords.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A section heading in the "manuscript display" serif style (see `core/ui/theme/Type.kt`) used
 * throughout Settings/About - promoted here (was Settings-only) so any screen with the same
 * card-of-labeled-sections layout (About, and future ones) can reuse it rather than duplicating.
 */
@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
}

/**
 * Gives a section the "3D box" [GlassSurface] treatment instead of sitting flat against the
 * screen background - see `docs/UI_GUIDELINES.md`. Not itself tappable, so it doesn't need
 * `pressDepth`; the interactive controls inside (buttons, chips) carry their own. Promoted from
 * Settings-only `SettingsSectionCard` so About (and any future section-per-card screen) shares
 * the exact same treatment rather than a parallel `Card` copy.
 */
@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}
