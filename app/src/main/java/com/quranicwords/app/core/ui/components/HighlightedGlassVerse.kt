package com.quranicwords.app.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.quranicwords.app.core.ui.theme.BrandGold
import com.quranicwords.app.core.ui.theme.LocalQuranFontFamily
import com.quranicwords.app.core.ui.theme.QuranCitationFontFamily

/**
 * Renders an Arabic Qur'anic Ayah with seamless text continuity, highlighting the targeted
 * word inline using rich accent styling and subtle background tint without breaking lines.
 */
@Composable
fun HighlightedGlassArabic(
    verseArabic: String,
    start: Int?,
    end: Int?,
    modifier: Modifier = Modifier
) {
    val quranFont = LocalQuranFontFamily.current
    val baseStyle = TextStyle(
        fontFamily = quranFont,
        fontSize = 24.sp,
        lineHeight = 44.sp,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.End
    )

    if (start == null || end == null || start !in 0..verseArabic.length || end !in start..verseArabic.length || start == end) {
        Text(
            text = verseArabic,
            style = baseStyle,
            textAlign = TextAlign.End,
            modifier = modifier.fillMaxWidth()
        )
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val annotated = remember(verseArabic, start, end, primaryColor) {
        buildAnnotatedString {
            append(verseArabic.substring(0, start))
            withStyle(
                SpanStyle(
                    color = BrandGold,
                    fontWeight = FontWeight.Bold,
                    background = BrandGold.copy(alpha = 0.20f)
                )
            ) {
                append(verseArabic.substring(start, end))
            }
            append(verseArabic.substring(end))
        }
    }

    Text(
        text = annotated,
        style = baseStyle,
        textAlign = TextAlign.End,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Renders a localized Qur'anic verse translation with seamless sentence continuity,
 * highlighting the targeted translated phrase inline without line breaks.
 */
@Composable
fun HighlightedGlassTranslation(
    verseTranslation: String,
    range: Pair<Int, Int>?,
    modifier: Modifier = Modifier
) {
    val baseStyle = MaterialTheme.typography.bodyMedium.copy(
        fontFamily = QuranCitationFontFamily,
        fontStyle = FontStyle.Italic,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (range == null || range.first !in 0..verseTranslation.length || range.second !in range.first..verseTranslation.length || range.first == range.second) {
        Text(
            text = verseTranslation,
            style = baseStyle,
            modifier = modifier.fillMaxWidth()
        )
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val annotated = remember(verseTranslation, range, primaryColor) {
        buildAnnotatedString {
            append(verseTranslation.substring(0, range.first))
            withStyle(
                SpanStyle(
                    color = BrandGold,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Normal,
                    background = BrandGold.copy(alpha = 0.18f)
                )
            ) {
                append(verseTranslation.substring(range.first, range.second))
            }
            append(verseTranslation.substring(range.second))
        }
    }

    Text(
        text = annotated,
        style = baseStyle,
        modifier = modifier.fillMaxWidth()
    )
}

