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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranicwords.app.core.ui.components.verseHighlightStyle
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.domain.model.LocalizedSense
import com.quranicwords.app.core.ui.components.SenseTranslationLine
import com.quranicwords.app.core.ui.components.spanHighlighted
import com.quranicwords.app.core.ui.components.textDirection
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.theme.QuranCitationFontFamily
import com.quranicwords.app.core.ui.theme.LocalQuranFontFamily
import com.quranicwords.app.core.ui.theme.LocalQuranFontStyle
import com.quranicwords.app.core.ui.theme.quranText
import com.quranicwords.app.core.util.AyahMark

/**
 * Shows [ExerciseContent.FillInTheBlank]'s sentence with the target word replaced by a blank
 * placeholder, then the same [OptionCard] picker `MultipleChoiceExerciseContent` uses.
 */
@Composable
fun FillInTheBlankExerciseContent(
    content: ExerciseContent.FillInTheBlank,
    sense: LocalizedSense?,
    selectedOptionId: String?,
    isChecked: Boolean,
    onSelect: (String) -> Unit
) {
    val language = rememberSelectedLanguage()

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        val category = com.quranicwords.app.core.ui.components.wordCategory(content.wordId)
        if (category != null) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                com.quranicwords.app.core.ui.components.GrammarCategoryBadge(category = category)
            }
        }

        val sentenceArabic = quranText(content.sentenceArabic)   // one-for-one: spans stay exact
        val start = content.blankStart.coerceIn(0, sentenceArabic.length)
        val end = content.blankEnd.coerceIn(start, sentenceArabic.length)
        // Once answered the blank is filled with the word itself, highlighted by its exact span.
        val sentence = if (isChecked) {
            spanHighlighted(
                sentenceArabic, start, end,
                verseHighlightStyle()
            )
        } else {
            buildAnnotatedString {
                append(sentenceArabic.substring(0, start))
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append("____")
                }
                append(sentenceArabic.substring(end))
            }
        }
        val ayah = AyahMark.ayahOf(Regex("""(\d+:\d+)""").find(content.sentenceReference)?.value)
        val fontStyle = LocalQuranFontStyle.current
        Text(
            text = if (ayah == null) sentence else sentence + AnnotatedString(AyahMark.of(ayah, fontStyle)),
            fontFamily = LocalQuranFontFamily.current,
            fontSize = 32.sp * fontStyle.verseScale,
            lineHeight = 46.sp * fontStyle.verseScale,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        // Exactly one translation line: the plain translation while answering, then the sense's
        // proven line (highlighted translation, or the word-by-word fallback) as feedback.
        if (isChecked && sense != null) {
            SenseTranslationLine(sense = sense)
        } else {
            Text(
                text = sense?.translationText ?: content.sentenceTranslation.get(language),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = QuranCitationFontFamily,
                    fontStyle = FontStyle.Italic,
                    textDirection = (sense?.textLanguage ?: language).textDirection()
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = com.quranicwords.app.core.util.VerseReferenceFormatter.format(content.sentenceReference, language),
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
                        showArabic = true,
                        onClick = { onSelect(option.id) }
                    )
                }
            }
        }
    }
}
