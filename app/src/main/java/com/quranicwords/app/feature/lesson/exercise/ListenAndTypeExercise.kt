package com.quranicwords.app.feature.lesson.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.localizedPrompt
import com.quranicwords.app.core.ui.components.rememberIsBanglaSelected

/**
 * Listen-and-type exercise. Only ever shown when its audio asset is actually bundled -
 * `LessonViewModel.init` filters this type out of the session otherwise (see
 * [ExerciseContent.ListenAndType]'s doc comment), so [onPlay] failing here would be a genuine
 * runtime asset regression rather than the expected "not sourced yet" state.
 */
@Composable
fun ListenAndTypeExerciseContent(
    content: ExerciseContent.ListenAndType,
    typedAnswer: String,
    isChecked: Boolean,
    onPlay: (String) -> Boolean,
    onTypedAnswerChange: (String) -> Unit
) {
    val isBangla = rememberIsBanglaSelected()

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(content.localizedPrompt(isBangla), style = MaterialTheme.typography.titleMedium)

        AudioPlayButton(onPlay = { onPlay(content.audioAssetPath) })

        OutlinedTextField(
            value = typedAnswer,
            onValueChange = onTypedAnswerChange,
            enabled = !isChecked,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.listen_and_type_hint)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
