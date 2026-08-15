package com.quranicwords.app.feature.lesson.exercise

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.quranicwords.app.R

/**
 * Play button + "unavailable" fallback text, shared by every exercise that plays a bundled audio
 * clip ([TapWhatYouHearExerciseContent], [ListenAndTypeExerciseContent]) - previously duplicated
 * verbatim in each. [onPlay] returning false surfaces the note rather than failing silently.
 */
@Composable
internal fun AudioPlayButton(onPlay: () -> Boolean) {
    var audioUnavailable by remember { mutableStateOf(false) }

    FilledIconButton(
        onClick = { audioUnavailable = !onPlay() },
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
    }

    if (audioUnavailable) {
        Text(
            text = stringResource(R.string.exercise_audio_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}
