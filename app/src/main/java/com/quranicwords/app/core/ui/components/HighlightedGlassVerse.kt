package com.quranicwords.app.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.LocalizedSense
import com.quranicwords.app.core.domain.model.LocalizedWord
import com.quranicwords.app.core.ui.theme.BrandGold
import com.quranicwords.app.core.ui.theme.LocalQuranFontFamily
import com.quranicwords.app.core.ui.theme.QuranCitationFontFamily
import com.quranicwords.app.core.util.VerseReferenceFormatter

/**
 * [text] with only `[start, end)` styled by [highlight] - the explicit span from the content data.
 * A missing or out-of-range span yields the plain text (no guessing, no substring search).
 */
fun spanHighlighted(text: String, start: Int?, end: Int?, highlight: SpanStyle): AnnotatedString {
    if (start == null || end == null || start !in 0 until end || end > text.length) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, start))
        withStyle(highlight) { append(text.substring(start, end)) }
        append(text.substring(end))
    }
}

/** Text direction of content written in [language]: Urdu and Persian read right-to-left. */
fun Language.textDirection(): TextDirection =
    if (this == Language.URDU || this == Language.PERSIAN) TextDirection.Rtl else TextDirection.Ltr

/**
 * A complete Qur'anic ayah, right-to-left, with only `[start, end)` - the taught word itself -
 * highlighted inline (gold, bold, tinted background) without breaking the line.
 */
@Composable
fun HighlightedGlassArabic(
    verseArabic: String,
    start: Int?,
    end: Int?,
    modifier: Modifier = Modifier
) {
    val style = TextStyle(
        fontFamily = LocalQuranFontFamily.current,
        fontSize = 24.sp,
        lineHeight = 44.sp,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Right,
        textDirection = TextDirection.Rtl
    )
    val annotated = remember(verseArabic, start, end) {
        spanHighlighted(
            verseArabic, start, end,
            SpanStyle(color = BrandGold, fontWeight = FontWeight.Bold, background = BrandGold.copy(alpha = 0.20f))
        )
    }
    Text(text = annotated, style = style, modifier = modifier.fillMaxWidth())
}

/**
 * A full verse translation in [textLanguage]'s direction, highlighted only by `[start, end)` - and
 * not at all when the content gives no translation span.
 */
@Composable
fun HighlightedGlassTranslation(
    verseTranslation: String,
    start: Int?,
    end: Int?,
    textLanguage: Language,
    modifier: Modifier = Modifier
) {
    val style = MaterialTheme.typography.bodyMedium.copy(
        fontFamily = QuranCitationFontFamily,
        fontStyle = FontStyle.Italic,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
        textDirection = textLanguage.textDirection()
    )
    val annotated = remember(verseTranslation, start, end) {
        spanHighlighted(
            verseTranslation, start, end,
            SpanStyle(
                color = BrandGold,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Normal,
                background = BrandGold.copy(alpha = 0.18f)
            )
        )
    }
    Text(text = annotated, style = style, modifier = modifier.fillMaxWidth())
}

/** The verse's word-by-word line, with only `[start, end)` - the word's own gloss - highlighted. */
@Composable
fun HighlightedGlassWordByWord(
    wordByWord: String,
    start: Int?,
    end: Int?,
    textLanguage: Language,
    modifier: Modifier = Modifier
) {
    val style = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 15.sp,
        lineHeight = 23.sp,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Start,
        textDirection = textLanguage.textDirection()
    )
    val annotated = remember(wordByWord, start, end) {
        spanHighlighted(
            wordByWord, start, end,
            SpanStyle(color = BrandGold, fontWeight = FontWeight.Bold, background = BrandGold.copy(alpha = 0.18f))
        )
    }
    Text(text = annotated, style = style, modifier = modifier.fillMaxWidth())
}

/**
 * The one translation line shown under an ayah - never two. When the content proves the meaning
 * occurs in the verse's full translation ([LocalizedSense.translationStart] non-null), that
 * translation with exactly that span highlighted; otherwise the verse's word-by-word line, labelled
 * "Word by word", with only the word's own gloss highlighted.
 */
@Composable
fun SenseTranslationLine(sense: LocalizedSense, modifier: Modifier = Modifier) {
    if (sense.translationStart != null && sense.translationEnd != null) {
        HighlightedGlassTranslation(
            verseTranslation = sense.translationText,
            start = sense.translationStart,
            end = sense.translationEnd,
            textLanguage = sense.textLanguage,
            modifier = modifier
        )
    } else {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.verse_word_by_word_label),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HighlightedGlassWordByWord(
                wordByWord = sense.wbwText,
                start = sense.wbwStart,
                end = sense.wbwEnd,
                textLanguage = sense.textLanguage
            )
        }
    }
}

/**
 * The one verse-example layout every screen uses: (1) the citation in the learner's language,
 * (2) the complete ayah with only the taught word highlighted, then - when [showTranslation] -
 * (3) exactly one translation line ([SenseTranslationLine]). Quiz screens pass
 * `showTranslation = false` until answered, since the highlighted meaning gives the answer away.
 */
@Composable
fun VerseExampleCard(
    sense: LocalizedSense,
    modifier: Modifier = Modifier,
    showTranslation: Boolean = true,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant
) {
    GlassSurface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), tint = tint) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VerseCitation(sense.reference)
            HighlightedGlassArabic(verseArabic = sense.verseArabic, start = sense.wordStart, end = sense.wordEnd)
            if (showTranslation) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SenseTranslationLine(sense)
            }
        }
    }
}

/** A verse citation, already localized (see `VerseReferenceFormatter.format`). */
@Composable
fun VerseCitation(reference: String, modifier: Modifier = Modifier) {
    if (reference.isBlank()) return
    Text(
        text = reference,
        style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = QuranCitationFontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

/**
 * A word's senses in the learner's language: numbered tabs (label = the sense's meaning) when it
 * has more than one, and the selected sense's [VerseExampleCard] below, sliding in the reading
 * direction on a switch.
 */
@Composable
fun WordSenseExamples(
    word: LocalizedWord,
    selectedIndex: Int,
    onSenseSelected: (Int) -> Unit,
    language: Language,
    modifier: Modifier = Modifier,
    title: String? = null
) {
    val senses = word.senses
    if (senses.isEmpty()) return
    val index = selectedIndex.coerceIn(0, senses.lastIndex)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (senses.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title ?: stringResource(R.string.polysemy_meanings_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${VerseReferenceFormatter.formatNumber(index + 1, language)} / ${VerseReferenceFormatter.formatNumber(senses.size, language)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                senses.forEachIndexed { idx, sense ->
                    SenseTab(
                        number = idx + 1,
                        total = senses.size,
                        meaning = sense.meaning,
                        selected = idx == index,
                        language = language,
                        onClick = { onSenseSelected(idx) }
                    )
                }
            }
        }

        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                // "Next" slides in from the reading-direction end: from the right in LTR, from the
                // left in RTL (Urdu/Persian UIs).
                if ((targetState > initialState) != isRtl) {
                    (slideInHorizontally { width -> width / 4 } + fadeIn(tween(250)))
                        .togetherWith(slideOutHorizontally { width -> -width / 4 } + fadeOut(tween(200)))
                } else {
                    (slideInHorizontally { width -> -width / 4 } + fadeIn(tween(250)))
                        .togetherWith(slideOutHorizontally { width -> width / 4 } + fadeOut(tween(200)))
                }.using(SizeTransform(clip = false))
            },
            label = "WordSenseVerseTransition"
        ) { targetIdx ->
            senses.getOrNull(targetIdx)?.let { VerseExampleCard(sense = it) }
        }
    }
}

@Composable
private fun SenseTab(
    number: Int,
    total: Int,
    meaning: String,
    selected: Boolean,
    language: Language,
    onClick: () -> Unit
) {
    val tabBg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "senseTabBg"
    )
    val tabTextColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "senseTabText"
    )
    val tabLabel = stringResource(
        R.string.a11y_meaning_tab,
        VerseReferenceFormatter.formatNumber(number, language),
        VerseReferenceFormatter.formatNumber(total, language)
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tabBg)
            // A tab, not an anonymous clickable: announced as "Meaning 2 of 3, <meaning>,
            // selected/not selected".
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$tabLabel, $meaning" }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "[${VerseReferenceFormatter.formatNumber(number, language)}]",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = tabTextColor
            )
            Text(
                text = meaning,
                style = MaterialTheme.typography.labelMedium,
                color = tabTextColor
            )
        }
    }
}
