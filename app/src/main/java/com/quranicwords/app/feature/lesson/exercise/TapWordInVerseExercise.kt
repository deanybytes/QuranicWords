package com.quranicwords.app.feature.lesson.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.WordSpan
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.domain.model.localizedPrompt
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.motion.MotionSpecs
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.quranicwords.app.core.ui.theme.QuranCitationFontFamily
import com.quranicwords.app.core.ui.theme.LocalQuranFontFamily

/**
 * The "reverse direction" quiz (see [ExerciseContent.TapWordInVerse]'s doc comment): the meaning
 * is shown as the prompt, and every word in the verse is its own tappable region - tap the one
 * that means what's shown. One-shot: [selectedSpan] being non-null means this exercise is
 * answered (correct or not), matching [MultipleChoice]'s no-retry semantics.
 */
@Composable
fun TapWordInVerseExerciseContent(
    content: ExerciseContent.TapWordInVerse,
    selectedSpan: WordSpan?,
    onSelectWord: (WordSpan) -> Unit
) {
    val language = rememberSelectedLanguage()

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val category = com.quranicwords.app.core.ui.components.resolveCategoryFromWordId(content.wordId)
        if (category != null) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                com.quranicwords.app.core.ui.components.GrammarCategoryBadge(category = category)
            }
        }

        Text(content.localizedPrompt(language), style = MaterialTheme.typography.titleMedium)
        Text(
            text = content.meaning.get(language),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            content.tappableSpans.forEach { span ->
                val isCorrectSpan = span.start == content.correctWordStart && span.end == content.correctWordEnd
                val isSelectedSpan = span == selectedSpan
                val answered = selectedSpan != null
                TappableWord(
                    text = content.verseArabic.substring(span.start, span.end),
                    highlight = when {
                        answered && isCorrectSpan -> WordHighlight.CORRECT
                        answered && isSelectedSpan -> WordHighlight.INCORRECT
                        else -> WordHighlight.NONE
                    },
                    enabled = !answered,
                    onClick = { onSelectWord(span) }
                )
            }
        }

        Text(
            text = stringResource(
                R.string.lesson_word_example_verse_label,
                com.quranicwords.app.core.util.VerseReferenceFormatter.format(content.verseReference, language)
            ),
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = QuranCitationFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )

        val verseTranslation = content.verseTranslation.get(language)
        if (verseTranslation.isNotBlank()) {
            val range = com.quranicwords.app.core.util.HighlightUtils.findMeaningHighlightRange(
                verseTranslation = verseTranslation,
                meaningHighlight = content.meaningHighlight.getOrNull(language),
                meaning = content.meaning.get(language)
            )
            com.quranicwords.app.core.ui.components.HighlightedGlassTranslation(
                verseTranslation = verseTranslation,
                range = range,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private enum class WordHighlight { NONE, CORRECT, INCORRECT }

@Composable
private fun TappableWord(
    text: String,
    highlight: WordHighlight,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (highlight) {
        WordHighlight.CORRECT -> MaterialTheme.colorScheme.primaryContainer
        WordHighlight.INCORRECT -> MaterialTheme.colorScheme.errorContainer
        WordHighlight.NONE -> MaterialTheme.colorScheme.surfaceVariant
    }
    val scale by animateFloatAsState(
        targetValue = if (highlight == WordHighlight.NONE) 1f else 1.08f,
        animationSpec = MotionSpecs.celebratory(),
        label = "tapWordScale"
    )

    Text(
        text = text,
        fontFamily = LocalQuranFontFamily.current,
        fontSize = 28.sp,
        lineHeight = 40.sp,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(enabled = enabled, onClick = onClick)
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
