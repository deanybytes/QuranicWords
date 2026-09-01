package com.quranicwords.app.feature.lesson.exercise

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.LemmaCategory
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.domain.model.getOrNull
import com.quranicwords.app.core.domain.model.localizedPrompt
import com.quranicwords.app.core.ui.components.GlassSurface
import com.quranicwords.app.core.ui.components.HighlightedGlassArabic
import com.quranicwords.app.core.ui.components.HighlightedGlassTranslation
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.theme.LocalQuranFontFamily
import com.quranicwords.app.core.ui.theme.QuranCitationFontFamily

/**
 * Non-scored teach step shown before a word's quiz exercises: the word, its grammatical category
 * (Noun vs. Verb), all contextual meanings (Wujūh al-Qur'an polysemy), and interactive verse examples.
 */
@Composable
fun WordIntroExerciseContent(
    content: ExerciseContent.WordIntro,
    onPlay: (String) -> Boolean
) {
    val language = rememberSelectedLanguage()
    var audioUnavailable by remember(content) { mutableStateOf(false) }
    var selectedMeaningIndex by remember(content) { mutableIntStateOf(0) }

    val activePolysemyEntry = content.polysemyEntries.getOrNull(selectedMeaningIndex)

    val displayedMeaning = if (activePolysemyEntry != null && activePolysemyEntry.contextualMeaning.isNotEmpty()) {
        activePolysemyEntry.contextualMeaning.get(language)
    } else {
        content.meaning.get(language)
    }

    val verseArabic = activePolysemyEntry?.verseArabic ?: content.exampleVerseArabic
    val verseReference = activePolysemyEntry?.verseReference ?: content.exampleVerseReference
    val verseTranslation = activePolysemyEntry?.verseTranslation?.get(language) ?: content.exampleVerseTranslation.get(language)
    val arabicWordStart = activePolysemyEntry?.arabicWordStart ?: content.arabicWordStart
    val arabicWordEnd = activePolysemyEntry?.arabicWordEnd ?: content.arabicWordEnd
    val meaningHighlight = activePolysemyEntry?.translationHighlight?.getOrNull(language) ?: content.meaningHighlight.getOrNull(language)

    val highlightStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        background = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(content.localizedPrompt(language), style = MaterialTheme.typography.titleMedium)

        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tint = when (content.lemmaCategory) {
                LemmaCategory.VERB -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Grammatical Category Tag (Noun vs Verb)
                val isVerb = content.lemmaCategory == LemmaCategory.VERB
                val categoryIcon = if (isVerb) Icons.Filled.FlashOn else Icons.Filled.AutoStories
                val categoryLabel = if (isVerb) stringResource(R.string.word_category_verb) else stringResource(R.string.word_category_noun)
                val categoryTint = if (isVerb) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(categoryTint.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = categoryTint,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = categoryLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = categoryTint
                    )
                }

                Text(
                    text = content.arabicWord,
                    fontFamily = LocalQuranFontFamily.current,
                    fontSize = 54.sp,
                    lineHeight = 66.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                AnimatedContent(
                    targetState = displayedMeaning,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "meaningAnim"
                ) { targetMeaning ->
                    Text(
                        text = targetMeaning,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Multi-Meaning Polysemy Selection Carousel / Tabs
        if (content.polysemyEntries.size > 1) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.polysemy_meanings_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content.polysemyEntries.forEachIndexed { idx, entry ->
                        val isSelected = selectedMeaningIndex == idx
                        val tabBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        val tabTextColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(tabBg)
                                .clickable { selectedMeaningIndex = idx }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.polysemy_tab_format, idx + 1),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = tabTextColor
                            )
                        }
                    }
                }
            }
        }

        content.audioAssetPath?.let { assetPath ->
            FilledIconButton(
                onClick = { audioUnavailable = !onPlay(assetPath) },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }

        if (audioUnavailable) {
            Text(
                text = stringResource(R.string.exercise_audio_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        if (!verseArabic.isNullOrBlank()) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                tint = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!verseReference.isNullOrBlank()) {
                        Text(
                            text = stringResource(
                                R.string.lesson_word_example_verse_label,
                                com.quranicwords.app.core.util.VerseReferenceFormatter.format(verseReference, language)
                            ),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = QuranCitationFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HighlightedGlassArabic(
                        verseArabic = verseArabic,
                        start = arabicWordStart,
                        end = arabicWordEnd,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (verseTranslation.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        val range = com.quranicwords.app.core.util.HighlightUtils.findMeaningHighlightRange(
                            verseTranslation = verseTranslation,
                            meaningHighlight = meaningHighlight,
                            meaning = displayedMeaning
                        )
                        HighlightedGlassTranslation(
                            verseTranslation = verseTranslation,
                            range = range,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

