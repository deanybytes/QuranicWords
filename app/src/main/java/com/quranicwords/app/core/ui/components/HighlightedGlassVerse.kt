package com.quranicwords.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranicwords.app.core.ui.theme.BrandGold
import com.quranicwords.app.core.ui.theme.BrandGreen
import com.quranicwords.app.core.ui.theme.LocalQuranFontFamily
import com.quranicwords.app.core.ui.theme.QuranCitationFontFamily

/**
 * Renders an Arabic Qur'anic Ayah with the targeted word encased in a luminous 3D glass box.
 */
@OptIn(ExperimentalLayoutApi::class)
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
        fontSize = 22.sp,
        lineHeight = 40.sp,
        color = MaterialTheme.colorScheme.onSurface
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

    val prefix = verseArabic.substring(0, start)
    val highlighted = verseArabic.substring(start, end).trim()
    val suffix = verseArabic.substring(end)

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalArrangement = Arrangement.Center
    ) {
        if (suffix.isNotEmpty()) {
            Text(text = suffix, style = baseStyle, textAlign = TextAlign.End)
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(10.dp),
                    ambientColor = BrandGold.copy(alpha = 0.35f),
                    spotColor = BrandGold.copy(alpha = 0.45f)
                )
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BrandGold.copy(alpha = 0.24f),
                            BrandGreen.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            BrandGold.copy(alpha = 0.70f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                        )
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = highlighted,
                style = baseStyle.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    shadow = Shadow(
                        color = BrandGold.copy(alpha = 0.50f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 4f
                    )
                ),
                textAlign = TextAlign.Center
            )
        }

        if (prefix.isNotEmpty()) {
            Text(text = prefix, style = baseStyle, textAlign = TextAlign.End)
        }
    }
}

/**
 * Renders a localized Qur'anic verse translation with the targeted word/phrase encased in a
 * 3D frosted glass transparent box.
 */
@OptIn(ExperimentalLayoutApi::class)
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

    val prefix = verseTranslation.substring(0, range.first)
    val highlighted = verseTranslation.substring(range.first, range.second).trim()
    val suffix = verseTranslation.substring(range.second)

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Center
    ) {
        if (prefix.isNotEmpty()) {
            Text(text = prefix, style = baseStyle)
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 3.dp, vertical = 2.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(8.dp),
                    ambientColor = BrandGold.copy(alpha = 0.30f),
                    spotColor = BrandGold.copy(alpha = 0.40f)
                )
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BrandGold.copy(alpha = 0.22f),
                            BrandGreen.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            BrandGold.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 7.dp, vertical = 2.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = highlighted,
                style = baseStyle.copy(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    shadow = Shadow(
                        color = BrandGold.copy(alpha = 0.45f),
                        offset = Offset(0f, 1f),
                        blurRadius = 3f
                    )
                )
            )
        }

        if (suffix.isNotEmpty()) {
            Text(text = suffix, style = baseStyle)
        }
    }
}
