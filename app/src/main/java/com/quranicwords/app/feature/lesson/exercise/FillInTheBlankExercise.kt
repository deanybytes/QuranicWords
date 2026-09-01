package com.quranicwords.app.feature.lesson.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.theme.QuranCitationFontFamily
import com.quranicwords.app.core.ui.theme.LocalQuranFontFamily

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
    val language = rememberSelectedLanguage()

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
            fontFamily = LocalQuranFontFamily.current,
            fontSize = 32.sp,
            lineHeight = 46.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = content.sentenceTranslation.get(language),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = QuranCitationFontFamily,
                fontStyle = FontStyle.Italic
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(
                R.string.lesson_word_example_verse_label,
                com.quranicwords.app.core.util.VerseReferenceFormatter.format(content.sentenceReference, language)
            ),
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = QuranCitationFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
