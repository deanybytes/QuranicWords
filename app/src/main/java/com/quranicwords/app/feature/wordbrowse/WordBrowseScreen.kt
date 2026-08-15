package com.quranicwords.app.feature.wordbrowse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.ui.components.QwLogo
import com.quranicwords.app.core.ui.components.Qw3DFlipCard
import com.quranicwords.app.core.ui.components.rememberIsBanglaSelected
import com.quranicwords.app.core.ui.theme.Elevation

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
    val isBangla = rememberIsBanglaSelected()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.word_browse_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.word_browse_empty))
            }
            return@Scaffold
        }

        val pagerState = rememberPagerState(pageCount = { uiState.words.size })
        // One flip-state slot per page, keyed by word id - a fresh, unflipped front whenever the
        // pager lands on a page it hasn't flipped yet, per the "story-fold" reveal rhythm.
        val flippedByWordId = remember { mutableStateMapOf<String, Boolean>() }

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                text = "${pagerState.currentPage + 1} / ${uiState.words.size}",
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
                Qw3DFlipCard(
                    flipped = flipped,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .clickable { flippedByWordId[word.wordId] = !flipped },
                    front = { WordCardFront(word) },
                    back = { WordCardBack(word, isBangla, onPlay = viewModel::playAudio) }
                )
            }
        }
    }
}

@Composable
private fun WordCardFront(word: ExerciseContent.WordIntro) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.floating)
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
        }
    }
}

@Composable
private fun WordCardBack(
    word: ExerciseContent.WordIntro,
    isBangla: Boolean,
    onPlay: (String) -> Boolean
) {
    val meaning = if (isBangla) word.meaningBn else word.meaningEn
    val verseTranslation = if (isBangla) word.exampleVerseTranslationBn else word.exampleVerseTranslationEn

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.floating)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = meaning,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            word.audioAssetPath?.let { assetPath ->
                FilledIconButton(
                    onClick = { onPlay(assetPath) },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Text(
                text = word.exampleVerseArabic,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(12.dp)
                ).padding(12.dp)
            )
            Text(
                text = verseTranslation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.lesson_word_example_verse_label, word.exampleVerseReference),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
