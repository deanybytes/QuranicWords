package com.quranicwords.app.feature.walkthrough

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.QuranFontStyle
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.domain.srs.WordStrength
import com.quranicwords.app.core.ui.components.HighlightedGlassArabic
import com.quranicwords.app.core.ui.components.QwPrimaryButton
import com.quranicwords.app.core.ui.components.QwSecondaryButton
import com.quranicwords.app.core.ui.components.WordStrengthMeter
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.components.verseHighlightColor
import com.quranicwords.app.core.ui.theme.quranText
import com.quranicwords.app.core.ui.theme.toFontFamily
import com.quranicwords.app.core.util.VerseReferenceFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Curriculum figures quoted by the tour; `GeneratedContentParsesTest` checks them against the
 * pipeline's build report so the tour can never advertise stale numbers. */
object TourFacts {
    const val WORDS = 3833
    const val COVERAGE_PERCENT = 97
}

private const val PAGE_COUNT = 6

/**
 * First-run tour (also replayable from Settings): six steps, each with a small live preview of the
 * real feature - the coverage ring filling, a highlighted ayah, an answer being checked, the memory
 * meter growing, the motivation stats and the fonts - plus a progress bar, a step counter and Skip.
 * The previews animate only while their step is on screen.
 */
@Composable
fun WalkthroughScreen(
    onFinished: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val language = rememberSelectedLanguage()
    val page = pagerState.currentPage
    val progress by animateFloatAsState((page + 1f) / PAGE_COUNT, tween(350), label = "tourProgress")

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Progress: bar + "step 2 of 6" + Skip.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.walkthrough_step,
                        VerseReferenceFormatter.formatNumber(page + 1, language),
                        VerseReferenceFormatter.formatNumber(PAGE_COUNT, language)
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (page < PAGE_COUNT - 1) {
                    TextButton(onClick = onFinished) { Text(stringResource(R.string.walkthrough_skip)) }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { index ->
                TourPage(index = index, active = index == page, language = language)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (page > 0) {
                    QwSecondaryButton(
                        text = stringResource(R.string.walkthrough_back),
                        onClick = { scope.launch { pagerState.animateScrollToPage(page - 1) } }
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                if (page < PAGE_COUNT - 1) {
                    QwPrimaryButton(
                        text = stringResource(R.string.walkthrough_next),
                        onClick = { scope.launch { pagerState.animateScrollToPage(page + 1) } }
                    )
                } else {
                    QwPrimaryButton(text = stringResource(R.string.walkthrough_finish), onClick = onFinished)
                }
            }
        }
    }
}

@Composable
private fun TourPage(index: Int, active: Boolean, language: Language) {
    val (title, desc) = when (index) {
        0 -> stringResource(R.string.walkthrough_page1_title) to stringResource(
            R.string.walkthrough_page1_desc,
            VerseReferenceFormatter.formatNumber(TourFacts.WORDS, language),
            VerseReferenceFormatter.formatDigits("${TourFacts.COVERAGE_PERCENT}%", language)
        )
        1 -> stringResource(R.string.walkthrough_page2_title) to stringResource(R.string.walkthrough_page2_desc)
        2 -> stringResource(R.string.walkthrough_page3_title) to stringResource(R.string.walkthrough_page3_desc)
        3 -> stringResource(R.string.walkthrough_page4_title) to stringResource(R.string.walkthrough_page4_desc)
        4 -> stringResource(R.string.walkthrough_page5_title) to stringResource(R.string.walkthrough_page5_desc)
        else -> stringResource(R.string.walkthrough_page6_title) to stringResource(
            R.string.walkthrough_page6_desc,
            VerseReferenceFormatter.formatNumber(QuranFontStyle.entries.size, language)
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            when (index) {
                0 -> CoveragePreview(active, language)
                1 -> AyahPreview(language)
                2 -> QuizPreview(active)
                3 -> MemoryPreview(active)
                4 -> MotivationPreview(active, language)
                else -> FontPreview(active, language)
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** The share of the Qur'an the curriculum covers, filling up. */
@Composable
private fun CoveragePreview(active: Boolean, language: Language) {
    val target = TourFacts.COVERAGE_PERCENT / 100f
    val fill = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            fill.snapTo(0f)
            fill.animateTo(target, tween(1600, easing = FastOutSlowInEasing))
        }
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(170.dp)) {
        CircularProgressIndicator(
            progress = { fill.value },
            modifier = Modifier.size(170.dp),
            strokeWidth = 14.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = VerseReferenceFormatter.formatDigits("${(fill.value * 100).toInt()}%", language),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** A real example card: the complete ayah with only the taught word highlighted. */
@Composable
private fun AyahPreview(language: Language) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = VerseReferenceFormatter.format("20:34", language),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        HighlightedGlassArabic(verseArabic = "وَنَذۡكُرَكَ كَثِيرًا", start = 0, end = 2, ayah = 34)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "${quranText("وَ")} = ${stringResource(R.string.walkthrough_sample_meaning)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = verseHighlightColor()
        )
    }
}

/** A question being answered: the right option turns green after a moment. */
@Composable
private fun QuizPreview(active: Boolean) {
    var answered by remember { mutableIntStateOf(0) }
    LaunchedEffect(active) {
        answered = 0
        while (active) {
            delay(1200); answered = 1
            delay(1600); answered = 0
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.walkthrough_sample_meaning),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        listOf("فِي", "وَ", "مِنْ").forEachIndexed { i, option ->
            val correct = i == 1 && answered == 1
            val bg by animateColorAsState(
                if (correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                label = "option$i"
            )
            Row(
                modifier = Modifier
                    .width(200.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = quranText(option),
                    fontSize = 24.sp,
                    color = if (correct) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
                if (correct) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

/** The memory meter growing from New to Mastered as a word is reviewed. */
@Composable
private fun MemoryPreview(active: Boolean) {
    var level by remember { mutableIntStateOf(0) }
    LaunchedEffect(active) {
        level = 0
        while (active) {
            delay(900)
            level = (level + 1) % WordStrength.entries.size
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = quranText("رَحْمَة"), fontSize = 40.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(16.dp))
        AnimatedContent(
            targetState = WordStrength.entries[level],
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
            label = "strength"
        ) { strength -> WordStrengthMeter(strength = strength) }
    }
}

/** Streak, XP, hearts and quests counting up. */
@Composable
private fun MotivationPreview(active: Boolean, language: Language) {
    val count = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            count.snapTo(0f)
            count.animateTo(1f, tween(1400, easing = FastOutSlowInEasing))
        }
    }
    val t = count.value
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        StatPill(Icons.Filled.LocalFireDepartment, Color(0xFFE8710A), VerseReferenceFormatter.formatNumber((7 * t).toInt(), language))
        StatPill(Icons.Filled.Bolt, MaterialTheme.colorScheme.primary, VerseReferenceFormatter.formatNumber((240 * t).toInt(), language))
        StatPill(Icons.Filled.Favorite, Color(0xFFD93A4E), VerseReferenceFormatter.formatNumber(5, language))
        StatPill(
            Icons.Filled.TaskAlt, MaterialTheme.colorScheme.primary,
            VerseReferenceFormatter.formatDigits("${(3 * t).toInt()}/3", language)
        )
    }
}

@Composable
private fun StatPill(icon: ImageVector, tint: Color, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .semantics { contentDescription = value }
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** The same ayah cycling through every bundled Qur'an font, with its name. */
@Composable
private fun FontPreview(active: Boolean, language: Language) {
    var i by remember { mutableIntStateOf(0) }
    LaunchedEffect(active) {
        i = 0
        while (active) {
            delay(1500)
            i = (i + 1) % QuranFontStyle.entries.size
        }
    }
    AnimatedContent(
        targetState = QuranFontStyle.entries[i],
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        label = "font"
    ) { style ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = style.script("بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"),
                fontFamily = style.toFontFamily(),
                fontSize = (28 * style.verseScale).sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = style.displayName.get(language),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}
