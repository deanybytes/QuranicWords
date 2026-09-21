package com.quranicwords.app.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.ui.components.charts.BarChart
import com.quranicwords.app.core.ui.components.charts.DonutChart
import com.quranicwords.app.core.ui.components.charts.HeatmapChart
import com.quranicwords.app.core.ui.components.charts.MetricTile
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.util.VerseReferenceFormatter
import com.quranicwords.app.core.util.formatPercent
import com.quranicwords.app.feature.achievements.AchievementsSection

/**
 * The Progress tab: charts and metric tiles derived from data that already exists (see
 * [ProgressViewModel]'s doc comment), plus the achievements grid folded in as a scrollable
 * section below the charts rather than a separately-pushed screen.
 */
@Composable
fun ProgressScreen(
    onOpenLearnedWords: () -> Unit = {},
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = rememberSelectedLanguage()

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                stringResource(R.string.progress_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTile(
                    icon = Icons.Filled.LocalFireDepartment,
                    value = VerseReferenceFormatter.formatDigits(uiState.currentStreak.toString(), language),
                    label = stringResource(R.string.progress_current_streak),
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    icon = Icons.Filled.Star,
                    value = VerseReferenceFormatter.formatDigits(uiState.totalPoints.toString(), language),
                    label = stringResource(R.string.progress_total_points),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTile(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    value = VerseReferenceFormatter.formatDigits(uiState.wordsLearnedCount.toString(), language),
                    label = stringResource(R.string.progress_words_learned),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLearnedWords
                )
                MetricTile(
                    icon = Icons.Filled.CheckCircle,
                    value = VerseReferenceFormatter.formatDigits(stringResource(R.string.progress_goal_days_value, uiState.goalMetDaysLast7), language),
                    label = stringResource(R.string.progress_goal_days_label),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.progress_quran_coverage), style = MaterialTheme.typography.titleMedium)
                DonutChart(
                    percent = (uiState.quranCoveragePercent / 100.0).toFloat(),
                    centerLabel = VerseReferenceFormatter.formatDigits("${formatPercent(uiState.quranCoveragePercent)}%", language),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.progress_lessons_this_week), style = MaterialTheme.typography.titleMedium)
                BarChart(
                    values = uiState.lessonsCompletedLast7Days,
                    labels = uiState.last7DayLabels,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.progress_days_practiced, uiState.daysPracticedLast28Count),
                    style = MaterialTheme.typography.titleMedium
                )
                HeatmapChart(practicedDays = uiState.practiceDaysLast28, modifier = Modifier.padding(top = 8.dp))
            }
        }

        item {
            AchievementsSection()
        }
    }
}
