package com.quranicwords.app.feature.lesson.exercise

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.domain.model.LocalizedSense
import com.quranicwords.app.core.ui.components.HighlightedGlassTranslation
import com.quranicwords.app.core.ui.components.SenseTranslationLine

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
    sense: LocalizedSense?,
    selectedSpan: WordSpan?,
    onSelectWord: (WordSpan) -> Unit
) {
    val language = rememberSelectedLanguage()

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
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

        Text(content.localizedPrompt(language), style = MaterialTheme.typography.titleMedium)
        Text(
            text = content.meaning.get(language),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Verse words are Arabic and read right-to-left whatever the UI language - without forcing
        // RTL, an English/French UI laid the verse out in reversed word order.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                content.tappableSpans.forEach { span ->
                    val isCorrectSpan = span.start == content.correctWordStart && span.end == content.correctWordEnd
                    val isSelectedSpan = span == selectedSpan
                    val answered = selectedSpan != null
                    // Clamped like FillInTheBlankExercise's blank - span offsets that drift past the
                    // verse text must degrade to an empty chip, not crash the lesson.
                    val start = span.start.coerceIn(0, content.verseArabic.length)
                    val end = span.end.coerceIn(start, content.verseArabic.length)
                    TappableWord(
                        text = content.verseArabic.substring(start, end),
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
        }

        Text(
            text = com.quranicwords.app.core.util.VerseReferenceFormatter.format(content.verseReference, language),
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = QuranCitationFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )

        // Exactly one translation line. While answering: the full translation, highlighted only
        // when the content proves the meaning in it (the meaning is the prompt, so that gives
        // nothing away) - the word-by-word fallback would point at the word's position, so it is
        // shown only once answered.
        if (sense != null) {
            if (selectedSpan != null || sense.translationStart != null) {
                SenseTranslationLine(sense = sense, modifier = Modifier.fillMaxWidth())
            } else if (sense.translationText.isNotBlank()) {
                HighlightedGlassTranslation(
                    verseTranslation = sense.translationText,
                    start = null,
                    end = null,
                    textLanguage = sense.textLanguage,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            val verseTranslation = content.verseTranslation.get(language)
            if (verseTranslation.isNotBlank()) {
                HighlightedGlassTranslation(
                    verseTranslation = verseTranslation,
                    start = null,
                    end = null,
                    textLanguage = language,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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

    val stateText = when (highlight) {
        WordHighlight.CORRECT -> stringResource(R.string.a11y_state_correct)
        WordHighlight.INCORRECT -> stringResource(R.string.a11y_state_incorrect)
        WordHighlight.NONE -> ""
    }
    val clickLabel = stringResource(R.string.a11y_action_select_word)

    Text(
        text = text,
        fontFamily = LocalQuranFontFamily.current,
        fontSize = 28.sp,
        lineHeight = 40.sp,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(enabled = enabled, onClickLabel = clickLabel, role = Role.Button, onClick = onClick)
            .semantics { if (stateText.isNotEmpty()) stateDescription = stateText }
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
