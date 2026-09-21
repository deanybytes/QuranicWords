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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.quranicwords.app.core.ui.components.resolveCategoryFromWordId
import com.quranicwords.app.core.ui.theme.LocalQuranFontFamily
import com.quranicwords.app.core.ui.theme.QuranCitationFontFamily
import com.quranicwords.app.core.util.VerseReferenceFormatter

/**
 * Non-scored teach step shown before a word's quiz exercises: the word, its grammatical category
 * (Noun vs. Verb), all contextual meanings (Wujūh al-Qur'an polysemy), and interactive verse examples.
 */
@Composable
fun WordIntroExerciseContent(
    content: ExerciseContent.WordIntro,
    selectedMeaningIndex: Int = 0,
    onMeaningSelected: (Int) -> Unit = {}
) {
    val language = rememberSelectedLanguage()

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

        val category = resolveCategoryFromWordId(content.wordId) ?: content.lemmaCategory
        val categoryAccent = com.quranicwords.app.core.ui.components.categoryAccentColor(category)

        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tint = MaterialTheme.colorScheme.surfaceContainer,
            accentBorderColor = categoryAccent,
            accentBorderWidth = 1.5.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Grammatical Category Tag (Noun vs Verb vs Particle) at the top
                com.quranicwords.app.core.ui.components.GrammarCategoryBadge(category = category)

                Text(
                    text = content.arabicWord,
                    fontFamily = LocalQuranFontFamily.current,
                    fontSize = 54.sp,
                    lineHeight = 66.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Verb Conjugation Infobox (Forms I-X, Past, Present, Masdar)
        if (content.lemmaCategory == LemmaCategory.VERB && (!content.pastArabic.isNullOrBlank() || !content.verbForm.isNullOrBlank())) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                tint = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.verb_conjugation_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        if (!content.verbForm.isNullOrBlank()) {
                            Text(
                                text = content.verbForm,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!content.pastArabic.isNullOrBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.past_tense_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = content.pastArabic,
                                    fontFamily = LocalQuranFontFamily.current,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (!content.presentArabic.isNullOrBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.present_tense_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = content.presentArabic,
                                    fontFamily = LocalQuranFontFamily.current,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (!content.masdarArabic.isNullOrBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.masdar_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = content.masdarArabic,
                                    fontFamily = LocalQuranFontFamily.current,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Particle Syntactic / Functional Category Infobox
        if (content.lemmaCategory == LemmaCategory.PARTICLE && (!content.particleType.isNullOrBlank() || !content.grammaticalCategory.isNullOrBlank())) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                tint = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.particle_type_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = content.particleType ?: content.grammaticalCategory ?: "",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = com.quranicwords.app.core.ui.components.categoryAccentColor(LemmaCategory.PARTICLE)
                        )
                    }
                    if (!content.grammaticalCategory.isNullOrBlank() && content.grammaticalCategory != content.particleType) {
                        Text(
                            text = content.grammaticalCategory,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Multi-Meaning Polysemy Selection Carousel / Tabs
        if (content.polysemyEntries.size > 1) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.polysemy_meanings_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${VerseReferenceFormatter.formatDigits((selectedMeaningIndex + 1).toString(), language)} / ${VerseReferenceFormatter.formatDigits(content.polysemyEntries.size.toString(), language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content.polysemyEntries.forEachIndexed { idx, entry ->
                        val isSelected = selectedMeaningIndex == idx
                        val tabBg by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            label = "tabBg"
                        )
                        val tabTextColor by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "tabText"
                        )
                        val senseMeaning = entry.contextualMeaning.get(language).ifBlank { content.meaning.get(language) }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(tabBg)
                                .clickable { onMeaningSelected(idx) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "[${VerseReferenceFormatter.formatDigits((idx + 1).toString(), language)}]",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = tabTextColor
                                )
                                if (senseMeaning.isNotBlank()) {
                                    Text(
                                        text = senseMeaning.take(24) + if (senseMeaning.length > 24) "…" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tabTextColor.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedContent(
            targetState = selectedMeaningIndex,
            transitionSpec = {
                if (targetState > initialState) {
                    (androidx.compose.animation.slideInHorizontally { width -> width / 4 } + fadeIn(androidx.compose.animation.core.tween(250)))
                        .togetherWith(androidx.compose.animation.slideOutHorizontally { width -> -width / 4 } + fadeOut(androidx.compose.animation.core.tween(200)))
                } else {
                    (androidx.compose.animation.slideInHorizontally { width -> -width / 4 } + fadeIn(androidx.compose.animation.core.tween(250)))
                        .togetherWith(androidx.compose.animation.slideOutHorizontally { width -> width / 4 } + fadeOut(androidx.compose.animation.core.tween(200)))
                }.using(androidx.compose.animation.SizeTransform(clip = false))
            },
            label = "PolysemyVerseTransition"
        ) { targetIdx ->
            val entry = content.polysemyEntries.getOrNull(targetIdx)
            val activeVerseArabic = entry?.verseArabic ?: content.exampleVerseArabic
            val activeVerseReference = entry?.verseReference ?: content.exampleVerseReference
            val activeVerseTranslation = entry?.verseTranslation?.get(language) ?: content.exampleVerseTranslation.get(language)
            val activeArabicWordStart = entry?.arabicWordStart ?: content.arabicWordStart
            val activeArabicWordEnd = entry?.arabicWordEnd ?: content.arabicWordEnd
            val activeMeaningHighlight = entry?.translationHighlight?.getOrNull(language) ?: content.meaningHighlight.getOrNull(language)
            val activeMeaningText = if (entry != null && entry.contextualMeaning.isNotEmpty()) {
                entry.contextualMeaning.get(language)
            } else {
                content.meaning.get(language)
            }

            if (!activeVerseArabic.isNullOrBlank()) {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    tint = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!activeVerseReference.isNullOrBlank()) {
                            Text(
                                text = stringResource(
                                    R.string.lesson_word_example_verse_label,
                                    com.quranicwords.app.core.util.VerseReferenceFormatter.format(activeVerseReference, language)
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
                            verseArabic = activeVerseArabic,
                            start = activeArabicWordStart,
                            end = activeArabicWordEnd,
                            modifier = Modifier.fillMaxWidth(),
                            arabicWord = content.arabicWord
                        )

                        if (activeVerseTranslation.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            val range = com.quranicwords.app.core.util.HighlightUtils.findMeaningHighlightRange(
                                verseTranslation = activeVerseTranslation,
                                meaningHighlight = activeMeaningHighlight,
                                meaning = activeMeaningText
                            )
                            HighlightedGlassTranslation(
                                verseTranslation = activeVerseTranslation,
                                range = range,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

