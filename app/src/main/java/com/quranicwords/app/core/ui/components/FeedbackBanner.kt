package com.quranicwords.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class FeedbackType { CORRECT, INCORRECT }

/** Bottom feedback banner for lesson exercises. Uses an assertive live region so TalkBack
 * announces correctness immediately, matching the on-screen color/text feedback. */
@Composable
fun FeedbackBanner(
    type: FeedbackType?,
    message: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = type != null,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier
    ) {
        val containerColor = when (type) {
            FeedbackType.CORRECT -> MaterialTheme.colorScheme.primaryContainer
            FeedbackType.INCORRECT -> MaterialTheme.colorScheme.errorContainer
            null -> MaterialTheme.colorScheme.surface
        }
        val contentColor = when (type) {
            FeedbackType.CORRECT -> MaterialTheme.colorScheme.onPrimaryContainer
            FeedbackType.INCORRECT -> MaterialTheme.colorScheme.onErrorContainer
            null -> MaterialTheme.colorScheme.onSurface
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Assertive },
            color = containerColor,
            contentColor = contentColor
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(message, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
