package com.quranicwords.app.feature.lesson.exercise

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ChoiceOption
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.getOrNull
import com.quranicwords.app.core.domain.model.localizedLabel
import com.quranicwords.app.core.ui.components.GlassSurface
import com.quranicwords.app.core.ui.components.HighlightedGlassArabic
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.theme.QuranCitationFontFamily
import com.quranicwords.app.core.ui.theme.LocalQuranFontFamily

@Composable
fun MultipleChoiceExerciseContent(
    content: ExerciseContent.MultipleChoice,
    selectedOptionId: String?,
    isChecked: Boolean,
    onSelect: (String) -> Unit
) {
    val language = rememberSelectedLanguage()
    val highlightStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val category = com.quranicwords.app.core.ui.components.resolveCategoryFromWordId(content.wordId)
        if (category != null) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                com.quranicwords.app.core.ui.components.GrammarCategoryBadge(category = category)
            }
        }

        content.promptArabic?.let { arabic ->
            Text(
                text = arabic,
                fontFamily = LocalQuranFontFamily.current,
                fontSize = 52.sp,
                lineHeight = 64.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (content.exampleVerseArabic != null && content.exampleVerseReference != null) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.lesson_word_example_verse_label,
                            com.quranicwords.app.core.util.VerseReferenceFormatter.format(content.exampleVerseReference, language)
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = QuranCitationFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    HighlightedGlassArabic(
                        verseArabic = content.exampleVerseArabic,
                        start = content.arabicWordStart,
                        end = content.arabicWordEnd,
                        modifier = Modifier.fillMaxWidth(),
                        arabicWord = content.promptArabic
                    )
                    val verseTranslation = content.exampleVerseTranslation.get(language)
                    if (verseTranslation.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        val correctMeaning = content.options.find { it.id == content.correctOptionId }?.label?.getOrNull(language).orEmpty()
                        val range = com.quranicwords.app.core.util.HighlightUtils.findMeaningHighlightRange(
                            verseTranslation = verseTranslation,
                            meaningHighlight = content.meaningHighlight.getOrNull(language),
                            meaning = correctMeaning
                        )
                        com.quranicwords.app.core.ui.components.HighlightedGlassTranslation(
                            verseTranslation = verseTranslation,
                            range = range,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

@Composable
internal fun OptionCard(
    option: ChoiceOption,
    language: Language,
    isSelected: Boolean,
    isChecked: Boolean,
    isCorrectOption: Boolean,
    showArabic: Boolean = false,
    onClick: () -> Unit
) {
    val targetContainer = when {
        isChecked && isCorrectOption -> MaterialTheme.colorScheme.primaryContainer
        isChecked && isSelected && !isCorrectOption -> MaterialTheme.colorScheme.errorContainer
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    // Morphs instead of snapping when isChecked flips, so the correct/incorrect reveal reads as
    // a deliberate moment rather than an instant color swap.
    val container by animateColorAsState(targetValue = targetContainer, label = "optionContainer")

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val density = LocalDensity.current
    val pressSpec = MotionSpecs.snappy<Float>()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, animationSpec = pressSpec, label = "optionPressScale")
    val tiltX by animateFloatAsState(targetValue = if (isPressed) -3f else 0f, animationSpec = pressSpec, label = "optionPressTilt")

    // Correctness is otherwise conveyed by container color alone (see targetContainer above) -
    // invisible to a screen reader. stateDescription reuses the same feedback strings
    // AnswerFeedbackOverlay already shows visually, so no new translations are needed. Found via
    // the QW-16 emulator TalkBack audit - see mergeDescendants comment below for the paired fix.
    val correctLabel = stringResource(R.string.lesson_feedback_correct)
    val incorrectLabel = stringResource(R.string.lesson_feedback_incorrect_tryagain)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationX = tiltX
                cameraDistance = 12f * density.density
            }
            // Without this, the option's label lives only in a non-merged child Text and
            // TalkBack announces an unlabeled "Button" - same bug class as QwSelectableCard,
            // found in the same audit pass.
            .semantics(mergeDescendants = true) {
                if (isChecked && isCorrectOption) {
                    stateDescription = correctLabel
                } else if (isChecked && isSelected) {
                    stateDescription = incorrectLabel
                }
            },
        colors = CardDefaults.cardColors(containerColor = container),
        onClick = onClick,
        enabled = !isChecked,
        interactionSource = interactionSource
    ) {
        val isArabicText = showArabic && !option.labelArabic.isNullOrBlank()
        val text = if (isArabicText) option.labelArabic.orEmpty() else option.localizedLabel(language)
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = if (isArabicText) LocalQuranFontFamily.current else androidx.compose.ui.text.font.FontFamily.Default,
            fontSize = if (isArabicText) 24.sp else 16.sp,
            textAlign = TextAlign.Center
        )
    }
}
