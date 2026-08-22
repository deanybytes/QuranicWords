package com.quranicwords.app.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.InactivityDuration
import com.quranicwords.app.core.domain.InactivityUnit

/**
 * Shown once when [com.quranicwords.app.core.domain.StreakRecovery.isLocked] flips true. Names the
 * actual inactivity duration (not a generic "streak lost" line) and pairs it with the consistency
 * hadith (Sahih al-Bukhari 6465) as the encouraging note - a purposeful citation about the app's
 * own core mechanic (regular practice, even in small amounts), not scripture-as-decoration; the
 * same reasoning that already justifies the every-launch opening invocation.
 */
@Composable
fun StreakLockedDialog(
    inactivityDuration: InactivityDuration?,
    onTakeTest: () -> Unit,
    onDismiss: () -> Unit
) {
    val durationText = inactivityDuration?.let {
        when (it.unit) {
            InactivityUnit.DAYS -> stringResource(R.string.streak_locked_inactivity_days, it.value)
            InactivityUnit.MONTHS -> stringResource(R.string.streak_locked_inactivity_months, it.value)
            InactivityUnit.YEARS -> stringResource(R.string.streak_locked_inactivity_years, it.value)
        }
    }.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        title = { Text(stringResource(R.string.streak_locked_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.streak_locked_dialog_message, durationText))
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.hadith_consistency_quote),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.hadith_consistency_reference),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onTakeTest) { Text(stringResource(R.string.streak_locked_take_test)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.streak_locked_later)) }
        }
    )
}
