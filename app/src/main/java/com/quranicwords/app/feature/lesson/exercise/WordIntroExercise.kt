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
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.LemmaCategory
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.domain.model.getOrNull
import com.quranicwords.app.core.domain.model.localizedPrompt
import com.quranicwords.app.core.domain.model.localizedPartOfSpeech
import com.quranicwords.app.core.domain.model.localizedVerbForm
import com.quranicwords.app.core.domain.model.cleanArabicDisplay
import com.quranicwords.app.core.ui.components.GlassSurface
import com.quranicwords.app.core.domain.model.LocalizedWord
import com.quranicwords.app.core.ui.components.WordSenseExamples
import com.quranicwords.app.core.ui.components.WordStrengthMeter
import com.quranicwords.app.core.domain.srs.WordStrength
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.theme.LocalQuranFontFamily
import com.quranicwords.app.core.ui.theme.QuranCitationFontFamily
import com.quranicwords.app.core.util.VerseReferenceFormatter

/**
 * Non-scored teach step shown before a word's quiz exercises: the word, its grammatical category
 * (Noun vs. Verb), its meaning, and one verse example per proven sense in the learner's language
 * ([word], from `WordExampleLocalizer`) - numbered tabs switch between senses.
 */
@Composable
fun WordIntroExerciseContent(
    content: ExerciseContent.WordIntro,
    word: LocalizedWord?,
    strength: WordStrength? = null,
    selectedMeaningIndex: Int = 0,
    onMeaningSelected: (Int) -> Unit = {}
) {
    val language = rememberSelectedLanguage()

    val displayedMeaning = word?.meaning?.takeIf { it.isNotBlank() } ?: content.meaning.get(language)

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(content.localizedPrompt(language), style = MaterialTheme.typography.titleMedium)

        val category = content.lemmaCategory
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
                // Only once the word has a memory - a first meeting has nothing to report yet.
                if (strength != null && strength != WordStrength.NEW) {
                    WordStrengthMeter(strength = strength)
                }

                Text(
                    text = content.arabicWord.cleanArabicDisplay(),
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
                        val verbFormLabel = content.localizedVerbForm(language)
                        if (!verbFormLabel.isNullOrBlank()) {
                            Text(
                                text = verbFormLabel,
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
                            // particleType is the same English string as partOfSpeechDetail, so its
                            // learner-language label is partOfSpeechLabel.
                            text = content.localizedPartOfSpeech(language)
                                ?.takeIf { content.particleType == null || content.particleType == content.partOfSpeechDetail }
                                ?: content.particleType ?: content.grammaticalCategory ?: "",
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

        if (word != null) {
            WordSenseExamples(
                word = word,
                selectedIndex = selectedMeaningIndex,
                onSenseSelected = onMeaningSelected,
                language = language
            )
        }
    }
}
