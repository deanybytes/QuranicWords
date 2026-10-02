package com.quranicwords.app.feature.wordbrowse

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranicwords.app.core.ui.theme.QuranCitationFontFamily
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.domain.model.getOrNull
import com.quranicwords.app.core.ui.components.GlassSurface
import com.quranicwords.app.core.ui.components.HighlightedGlassArabic
import com.quranicwords.app.core.ui.components.HighlightedGlassTranslation
import com.quranicwords.app.core.ui.components.QwIconButton
import com.quranicwords.app.core.ui.components.QwLogo
import com.quranicwords.app.core.ui.components.Qw3DFlipCard
import com.quranicwords.app.core.ui.components.WordStrengthMeter
import com.quranicwords.app.core.ui.components.QwPrimaryButton
import com.quranicwords.app.core.ui.theme.QwShapes
import com.quranicwords.app.feature.lesson.exercise.AudioPlayButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.domain.srs.WordStrength
import com.quranicwords.app.core.ui.theme.LocalQuranFontFamily
import com.quranicwords.app.core.util.HighlightUtils
import com.quranicwords.app.core.util.VerseReferenceFormatter

/**
 * Card-flip, "story-fold" browsing of a section's words - tap a card to flip it and reveal the
 * meaning/example verse, swipe to the next (which resets to its own unflipped front), rather than
 * one long scrolling list. See [WordBrowseViewModel]'s doc comment for why this is an additional
 * entry point alongside the structured lesson flow, not a replacement of it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordBrowseScreen(
    onBack: () -> Unit,
    viewModel: WordBrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = rememberSelectedLanguage()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.word_browse_title)) },
                navigationIcon = {
                    QwIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (uiState.words.isEmpty()) {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Text(stringResource(R.string.word_browse_empty), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(
                    stringResource(R.string.word_browse_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                QwPrimaryButton(text = stringResource(R.string.action_back), onClick = onBack)
            }
            return@Scaffold
        }

        val pagerState = rememberPagerState(pageCount = { uiState.words.size })
        // One flip-state slot per page, keyed by word id - a fresh, unflipped front whenever the
        // pager lands on a page it hasn't flipped yet, per the "story-fold" reveal rhythm.
        val flippedByWordId = remember { mutableStateMapOf<String, Boolean>() }

        var query by rememberSaveable { mutableStateOf("") }
        var noMatch by remember { mutableStateOf(false) }
        LaunchedEffect(query) {
            val match = viewModel.indexOfMatch(query)
            noMatch = query.isNotBlank() && match == null
            if (match != null) pagerState.animateScrollToPage(match)
        }

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.word_browse_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                isError = noMatch,
                supportingText = if (noMatch) {
                    { Text(stringResource(R.string.word_browse_no_match)) }
                } else {
                    null
                },
                singleLine = true,
                shape = QwShapes.medium,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Text(
                text = "${VerseReferenceFormatter.formatNumber(pagerState.currentPage + 1, language)} / " +
                    VerseReferenceFormatter.formatNumber(uiState.words.size, language),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                pageSpacing = 16.dp
            ) { page ->
                val word = uiState.words[page]
                val flipped = flippedByWordId[word.wordId] == true
                val flipLabel = stringResource(R.string.a11y_action_flip_card)
                val sideLabel = stringResource(if (flipped) R.string.a11y_card_showing_meaning else R.string.a11y_card_showing_word)
                Qw3DFlipCard(
                    flipped = flipped,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .clickable(onClickLabel = flipLabel, role = Role.Button) { flippedByWordId[word.wordId] = !flipped }
                        .semantics { stateDescription = sideLabel },
                    front = {
                        WordCardFront(
                            word = word,
                            strength = uiState.strengths[word.wordId],
                            onPlayAudio = word.audioAssetPath?.let { path -> { viewModel.playPronunciation(path) } }
                        )
                    },
                    back = { WordCardBack(word, language) }
                )
            }
        }
    }
}

@Composable
private fun WordCardFront(word: ExerciseContent.WordIntro, strength: WordStrength?, onPlayAudio: (() -> Boolean)?) {
    GlassSurface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        tint = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            QwLogo(size = 40.dp)
            Text(
                text = word.arabicWord,
                fontSize = 64.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 24.dp)
            )
            if (strength != null && strength != WordStrength.NEW) {
                WordStrengthMeter(strength = strength, modifier = Modifier.padding(top = 16.dp))
            }
            if (onPlayAudio != null) {
                Box(modifier = Modifier.padding(top = 16.dp)) { AudioPlayButton(onPlay = onPlayAudio) }
            }
        }
    }
}

@Composable
private fun WordCardBack(
    word: ExerciseContent.WordIntro,
    language: Language
) {
    var selectedMeaningIndex by remember(word) { androidx.compose.runtime.mutableIntStateOf(0) }
    val activePolysemyEntry = word.polysemyEntries.getOrNull(selectedMeaningIndex)

    val displayedMeaning = if (activePolysemyEntry != null && activePolysemyEntry.contextualMeaning.isNotEmpty()) {
        activePolysemyEntry.contextualMeaning.get(language)
    } else {
        word.meaning.get(language)
    }

    val verseTranslation = activePolysemyEntry?.verseTranslation?.get(language) ?: word.exampleVerseTranslation.get(language)
    val arabicVerse = activePolysemyEntry?.verseArabic ?: word.exampleVerseArabic
    val verseRef = activePolysemyEntry?.verseReference ?: word.exampleVerseReference

    GlassSurface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        tint = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = displayedMeaning,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Polysemy tabs if multiple meanings exist
            if (word.polysemyEntries.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center
                ) {
                    word.polysemyEntries.forEachIndexed { idx, _ ->
                        val isSelected = selectedMeaningIndex == idx
                        val tabBg by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            label = "tabBg"
                        )
                        val tabTextColor by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            label = "tabText"
                        )

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(tabBg)
                                .selectable(selected = isSelected, role = Role.Tab, onClick = { selectedMeaningIndex = idx })
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "[${VerseReferenceFormatter.formatDigits((idx + 1).toString(), language)}]",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = tabTextColor
                            )
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = selectedMeaningIndex,
                transitionSpec = {
                    (fadeIn(androidx.compose.animation.core.tween(200))).togetherWith(fadeOut(androidx.compose.animation.core.tween(150)))
                },
                label = "BrowseCardVerseTransition"
            ) { targetIdx ->
                val entry = word.polysemyEntries.getOrNull(targetIdx)
                val activeArabicVerse = entry?.verseArabic ?: word.exampleVerseArabic
                val activeArabicStart = entry?.arabicWordStart ?: word.arabicWordStart
                val activeArabicEnd = entry?.arabicWordEnd ?: word.arabicWordEnd
                val activeVerseTranslation = entry?.verseTranslation?.get(language) ?: word.exampleVerseTranslation.get(language)
                val activeVerseRef = entry?.verseReference ?: word.exampleVerseReference
                val activeMeaningHighlight = entry?.translationHighlight?.get(language) ?: word.meaningHighlight.getOrNull(language)
                val activeMeaningText = entry?.contextualMeaning?.get(language) ?: word.meaning.get(language)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!activeArabicVerse.isNullOrBlank()) {
                        HighlightedGlassArabic(
                            verseArabic = activeArabicVerse,
                            start = activeArabicStart,
                            end = activeArabicEnd,
                            arabicWord = word.arabicWord,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (activeVerseTranslation.isNotBlank()) {
                        val range = HighlightUtils.findMeaningHighlightRange(
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
                    if (!activeVerseRef.isNullOrBlank()) {
                        Text(
                            text = com.quranicwords.app.core.util.VerseReferenceFormatter.format(activeVerseRef, language),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = QuranCitationFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
