package com.quranicwords.app.feature.lesson.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.localizedPrompt
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage

/**
 * Listen-and-select exercise. Playback audio (male voice, per the app's Islamic-values
 * constraint) is a bundled content dependency not yet sourced for this increment - [onPlay]
 * returning false surfaces an "audio unavailable" note rather than failing silently.
 */
@Composable
fun TapWhatYouHearExerciseContent(
    content: ExerciseContent.TapWhatYouHear,
    selectedOptionId: String?,
    isChecked: Boolean,
    onPlay: (String) -> Boolean,
    onSelect: (String) -> Unit
) {
    val language = rememberSelectedLanguage()

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        val category = com.quranicwords.app.core.ui.components.resolveCategoryFromWordId(content.wordId)
        if (category != null) {
            com.quranicwords.app.core.ui.components.GrammarCategoryBadge(category = category)
        }

        Text(content.localizedPrompt(language), style = MaterialTheme.typography.titleMedium)

        AudioPlayButton(onPlay = { onPlay(content.audioAssetPath) })

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            content.options.forEach { option ->
                key(option.id) {
                    OptionCard(
                        option = option,
                        language = language,
                        isSelected = option.id == selectedOptionId,
                        isChecked = isChecked,
                        isCorrectOption = option.id == content.correctOptionId,
                        onClick = { onSelect(option.id) }
                    )
                }
            }
        }
    }
}
