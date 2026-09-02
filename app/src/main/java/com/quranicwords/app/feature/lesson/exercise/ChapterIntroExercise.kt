package com.quranicwords.app.feature.lesson.exercise

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.ui.components.GlassSurface
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.theme.BrandGold
import com.quranicwords.app.core.ui.theme.BrandGreen
import com.quranicwords.app.core.ui.theme.QuranCitationFontFamily

@Composable
fun ChapterIntroExerciseContent(
    content: ExerciseContent.ChapterIntro,
    modifier: Modifier = Modifier
) {
    val language = rememberSelectedLanguage()

    var animatedProgress by remember { mutableStateOf(0f) }
    val animatedProgressValue by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "coverage_progress"
    )

    LaunchedEffect(Unit) {
        animatedProgress = (content.accumulatedCoveragePercent / 100f).coerceIn(0f, 1f)
    }

    val localizedChapterNumber = com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits(content.chapterNumber.toString(), language)
    val localizedWordCount = com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits(content.wordCount.toString(), language)
    val localizedOccurrences = com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits(String.format("%,d", content.quranOccurrenceCount), language)
    val localizedChapterCoverage = com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits(String.format("%.1f%%", content.chapterCoveragePercent), language)
    val localizedAccumulatedCoverage = com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits(String.format("%.1f%%", content.accumulatedCoveragePercent), language)
    val localizedAccumulatedWords = com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits(content.accumulatedWords.toString(), language)
    val localizedNouns = com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits(content.nounCount.toString(), language)
    val localizedVerbs = com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits(content.verbCount.toString(), language)
    val localizedParticles = com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits(content.particleCount.toString(), language)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Hero Chapter Header Card
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            tint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Chapter Badge Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    BrandGold.copy(alpha = 0.25f),
                                    BrandGreen.copy(alpha = 0.25f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    BrandGold.copy(alpha = 0.7f),
                                    Color.White.copy(alpha = 0.6f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoStories,
                            contentDescription = null,
                            tint = BrandGold,
                            modifier = Modifier.size(16.dp)
                        )
                        val bnOrdinals = arrayOf("", "১ম", "২য়", "৩য়", "৪র্থ", "৫ম", "৬ষ্ঠ", "৭ম", "৮ম", "৯ম", "১০ম")
                        val urOrdinals = arrayOf("", "پہلا", "دوسرا", "تیسرا", "چوتھا", "پانچواں", "چھٹا", "ساتواں", "آٹھواں", "نواں", "دسواں")
                        val badgeText = when (language) {
                            com.quranicwords.app.core.domain.model.Language.BANGLA -> if (content.chapterNumber in 1..10) "${bnOrdinals[content.chapterNumber]} অধ্যায় পরিচিতি" else "অধ্যায় $localizedChapterNumber পরিচিতি"
                            com.quranicwords.app.core.domain.model.Language.URDU -> if (content.chapterNumber in 1..10) "${urOrdinals[content.chapterNumber]} باب کا تعارف" else "باب $localizedChapterNumber کا تعارف"
                            else -> stringResource(R.string.chapter_intro_label, localizedChapterNumber)
                        }

                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = QuranCitationFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Chapter Title
                Text(
                    text = content.chapterTitle.get(language),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = QuranCitationFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 30.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Chapter Description
                val desc = content.chapterDescription.get(language)
                if (desc.isNotBlank()) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = QuranCitationFontFamily,
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4 Key Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                icon = Icons.Filled.MenuBook,
                title = stringResource(R.string.chapter_intro_words_count_label),
                value = localizedWordCount,
                accentColor = BrandGreen,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Filled.TrendingUp,
                title = stringResource(R.string.chapter_intro_occurrences_label),
                value = localizedOccurrences,
                accentColor = BrandGold,
                modifier = Modifier.weight(1f)
            )
        }



        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                icon = Icons.Filled.PieChart,
                title = stringResource(R.string.chapter_intro_chapter_coverage_label),
                value = localizedChapterCoverage,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Filled.EmojiEvents,
                title = stringResource(R.string.chapter_intro_accumulated_coverage_label),
                value = localizedAccumulatedCoverage,
                accentColor = BrandGold,
                modifier = Modifier.weight(1f)
            )
        }

        // Cumulative Milestone Card
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            tint = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.chapter_intro_accumulated_coverage_label),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = QuranCitationFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = localizedAccumulatedCoverage,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = QuranCitationFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = BrandGold
                    )
                }

                LinearProgressIndicator(
                    progress = { animatedProgressValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = BrandGold,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )

                Text(
                    text = stringResource(
                        R.string.chapter_intro_accumulated_desc,
                        localizedAccumulatedCoverage,
                        localizedAccumulatedWords
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = QuranCitationFontFamily,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Parts of Speech Breakdown Card (أقسام الكلمة)
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            tint = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.chapter_intro_pos_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = QuranCitationFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val totalWords = (content.nounCount + content.verbCount + content.particleCount).coerceAtLeast(1)
                val nounPct = (content.nounCount.toFloat() / totalWords.toFloat()) * 100f
                val verbPct = (content.verbCount.toFloat() / totalWords.toFloat()) * 100f
                val particlePct = (content.particleCount.toFloat() / totalWords.toFloat()) * 100f
                val particleColor = Color(0xFF0288D1)

                // Proportional Multi-Color Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    if (content.nounCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(nounPct.coerceAtLeast(0.1f))
                                .height(8.dp)
                                .background(BrandGreen)
                        )
                    }
                    if (content.verbCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(verbPct.coerceAtLeast(0.1f))
                                .height(8.dp)
                                .background(BrandGold)
                        )
                    }
                    if (content.particleCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(particlePct.coerceAtLeast(0.1f))
                                .height(8.dp)
                                .background(particleColor)
                        )
                    }
                }

                // POS Items (Ism, Fi'l, Harf)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PosItem(
                        dotColor = BrandGreen,
                        label = stringResource(R.string.chapter_intro_ism_label),
                        count = localizedNouns,
                        percentage = com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits(String.format("%.1f%%", nounPct), language)
                    )
                    PosItem(
                        dotColor = BrandGold,
                        label = stringResource(R.string.chapter_intro_fil_label),
                        count = localizedVerbs,
                        percentage = com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits(String.format("%.1f%%", verbPct), language)
                    )
                    PosItem(
                        dotColor = particleColor,
                        label = stringResource(R.string.chapter_intro_harf_label),
                        count = localizedParticles,
                        percentage = com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits(String.format("%.1f%%", particlePct), language)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier,
        tint = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = QuranCitationFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = QuranCitationFontFamily,
                    lineHeight = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PosItem(
    dotColor: Color,
    label: String,
    count: String,
    percentage: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = QuranCitationFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$count ($percentage)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = QuranCitationFontFamily
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
