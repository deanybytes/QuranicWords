package com.quranicwords.app.feature.lesson.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.ui.components.rememberIsBanglaSelected

/**
 * Shows [ExerciseContent.FillInTheBlank]'s sentence with the target word replaced by a blank
 * placeholder, then the same [OptionCard] picker `MultipleChoiceExerciseContent` uses.
 */
@Composable
fun FillInTheBlankExerciseContent(
    content: ExerciseContent.FillInTheBlank,
    selectedOptionId: String?,
    isChecked: Boolean,
    onSelect: (String) -> Unit
) {
    val isBangla = rememberIsBanglaSelected()

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        val sentenceWithBlank = buildAnnotatedString {
            val start = content.blankStart.coerceIn(0, content.sentenceArabic.length)
            val end = content.blankEnd.coerceIn(start, content.sentenceArabic.length)
            append(content.sentenceArabic.substring(0, start))
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                append("____")
            }
            append(content.sentenceArabic.substring(end))
        }
        Text(
            text = sentenceWithBlank,
            fontSize = 32.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = if (isBangla) content.sentenceTranslationBn else content.sentenceTranslationEn,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content.options.forEach { option ->
                OptionCard(
                    option = option,
                    isBangla = isBangla,
                    isSelected = option.id == selectedOptionId,
                    isChecked = isChecked,
                    isCorrectOption = option.id == content.correctOptionId,
                    onClick = { onSelect(option.id) }
                )
            }
        }
    }
}
