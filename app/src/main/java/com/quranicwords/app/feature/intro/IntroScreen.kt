package com.quranicwords.app.feature.intro

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.ui.components.QwLogo
import com.quranicwords.app.core.ui.components.QwPrimaryButton
import com.quranicwords.app.core.ui.components.StaggeredEntrance
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.motion.MotionSpecs
import kotlin.math.roundToInt

/**
 * Shown before a chapter/section's first lesson - see [IntroViewModel]'s doc comment. Every stat
 * shown here is read straight from precomputed `ChapterEntity`/`SectionEntity` columns, never
 * recomputed - the count-up/stagger here is purely presentational.
 */
@Composable
fun IntroScreen(
    onContinue: () -> Unit,
    viewModel: IntroViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = rememberSelectedLanguage()

    Scaffold { padding ->
        if (uiState.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StaggeredEntrance(index = 0) {
                QwLogo(size = 72.dp)
            }
            StaggeredEntrance(index = 1) {
                Text(
                    text = stringResource(
                        if (uiState.isChapter) R.string.intro_chapter_label else R.string.intro_section_label
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            StaggeredEntrance(index = 2) {
                Text(
                    text = uiState.title.get(language),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).semantics { heading() }
                )
            }

            StaggeredEntrance(index = 3) {
                val animatedWordCount by animateIntAsState(
                    targetValue = uiState.wordCount,
                    animationSpec = MotionSpecs.countUp,
                    label = "introWordCount"
                )
                Text(
                    text = stringResource(R.string.intro_word_count, animatedWordCount),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp)
                )
            }
            StaggeredEntrance(index = 4) {
                Text(
                    text = stringResource(
                        R.string.intro_occurrence_percent,
                        formatPercent(uiState.occurrencePercent)
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
            StaggeredEntrance(index = 5) {
                Text(
                    text = stringResource(
                        R.string.intro_cumulative_percent,
                        formatPercent(uiState.cumulativePercent)
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            StaggeredEntrance(index = 6) {
                Column(modifier = Modifier.padding(top = 32.dp)) {
                    QwPrimaryButton(
                        text = stringResource(R.string.intro_start_button),
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.intro_skip_button))
                    }
                }
            }
        }
    }
}

private fun formatPercent(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    return if (rounded == rounded.toInt().toDouble()) rounded.toInt().toString() else rounded.toString()
}
