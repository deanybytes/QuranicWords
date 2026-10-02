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
 * Listen-and-select exercise: plays the word's bundled pronunciation clip (only authored for
 * words with verified audio) and the learner taps the Arabic word they heard. Only shown while
 * pronunciation audio is on (see `LessonViewModel`); [onPlay] returning false still surfaces an
 * "audio unavailable" note rather than failing silently.
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
        val category = com.quranicwords.app.core.ui.components.wordCategory(content.wordId)
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
                        // The learner matches a sound to its written word, so the options are
                        // the Arabic words themselves, not their meanings.
                        showArabic = true,
                        onClick = { onSelect(option.id) }
                    )
                }
            }
        }
    }
}
