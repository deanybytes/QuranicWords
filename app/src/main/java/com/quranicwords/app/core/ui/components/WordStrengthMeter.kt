package com.quranicwords.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.srs.WordStrength
import com.quranicwords.app.core.ui.theme.QwShapes

/** Localized label for a [WordStrength] bucket. */
@Composable
fun WordStrength.label(): String = stringResource(
    when (this) {
        WordStrength.NEW -> R.string.word_strength_new
        WordStrength.LEARNING -> R.string.word_strength_learning
        WordStrength.FAMILIAR -> R.string.word_strength_familiar
        WordStrength.STRONG -> R.string.word_strength_strong
        WordStrength.MASTERED -> R.string.word_strength_mastered
    }
)

/**
 * Four-segment memory-strength meter (see [WordStrength]) - filled segments in the one green
 * identity, empty ones in the surface-variant track, plus the bucket name. Static on purpose: it
 * reports a fact about memory, not a reward moment, so it has nothing to animate. One
 * accessibility node reading "Memory strength: Familiar".
 */
@Composable
fun WordStrengthMeter(
    strength: WordStrength,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    /** "Memory strength: Familiar" on its own; just "Familiar" in a list already titled "Word strength". */
    prefixed: Boolean = true
) {
    val label = strength.label()
    val description = stringResource(R.string.a11y_word_strength, label)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(SEGMENTS) { index ->
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(6.dp)
                    .clip(QwShapes.extraSmall)
                    .background(
                        if (index < strength.level) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
        if (showLabel) {
            Text(
                // "Memory strength: Familiar" - the bare level word alone did not say what it measures.
                text = if (prefixed) description else label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

private const val SEGMENTS = 4
