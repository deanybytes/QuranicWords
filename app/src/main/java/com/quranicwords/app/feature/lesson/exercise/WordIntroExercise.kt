package com.quranicwords.app.feature.lesson.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.domain.model.getOrNull
import com.quranicwords.app.core.domain.model.localizedPrompt
import com.quranicwords.app.core.ui.components.GlassSurface
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.theme.QuranCitationFontFamily
import com.quranicwords.app.core.ui.theme.LocalQuranFontFamily

/**
 * Non-scored teach step shown before a word's quiz exercises: the word, its meaning, and one
 * example verse it appears in, with no selection state and no Check button (see
 * [com.quranicwords.app.core.domain.model.isScored]).
 */
@Composable
fun WordIntroExerciseContent(
    content: ExerciseContent.WordIntro,
    onPlay: (String) -> Boolean
) {
    val language = rememberSelectedLanguage()
    var audioUnavailable by remember(content) { mutableStateOf(false) }
    val meaning = content.meaning.get(language)
    val verseTranslation = content.exampleVerseTranslation.get(language)
    val meaningHighlight = content.meaningHighlight.getOrNull(language)
    val highlightStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        background = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(content.localizedPrompt(language), style = MaterialTheme.typography.titleMedium)

        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tint = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = content.arabicWord,
                    fontFamily = LocalQuranFontFamily.current,
                    fontSize = 56.sp,
                    lineHeight = 68.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = meaning,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
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

        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            tint = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.lesson_word_example_verse_label, content.exampleVerseReference),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = QuranCitationFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = buildAnnotatedString {
                        append(content.exampleVerseArabic)
                        val start = content.arabicWordStart
                        val end = content.arabicWordEnd
                        if (start != null && end != null &&
                            start in 0..content.exampleVerseArabic.length &&
                            end in start..content.exampleVerseArabic.length
                        ) {
                            addStyle(highlightStyle, start, end)
                        }
                    },
                    fontFamily = LocalQuranFontFamily.current,
                    fontSize = 22.sp,
                    lineHeight = 36.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(
                    text = buildAnnotatedString {
                        append(verseTranslation)
                        val idx = meaningHighlight?.let { verseTranslation.indexOf(it) } ?: -1
                        if (idx >= 0 && meaningHighlight != null) {
                            addStyle(highlightStyle, idx, idx + meaningHighlight.length)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = QuranCitationFontFamily,
                        fontStyle = FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
