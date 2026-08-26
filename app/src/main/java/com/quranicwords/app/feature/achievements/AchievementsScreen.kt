package com.quranicwords.app.feature.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.quranicwords.app.core.ui.components.AchievementBadge
import com.quranicwords.app.core.ui.components.StaggeredEntrance
import com.quranicwords.app.core.ui.components.displayDescription
import com.quranicwords.app.core.ui.components.displayName
import com.quranicwords.app.core.ui.theme.Elevation

/**
 * The achievements grid, embedded as a scrollable section inside the Progress tab
 * (`feature/progress/ProgressScreen.kt`) rather than a separately-pushed screen - a plain
 * [Column] (not lazily windowed), since the catalog only ever has ~15-20 entries. Hosted inside
 * ProgressScreen's own outer `LazyColumn`, so this must not introduce a second scrolling
 * container of its own.
 */
@Composable
fun AchievementsSection(viewModel: AchievementsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.achievements_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }.padding(bottom = 8.dp)
        )
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            uiState.items.forEachIndexed { index, item ->
                StaggeredEntrance(index = index.coerceAtMost(8)) {
                    AchievementRow(item)
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(item: AchievementUiItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.unlocked) Elevation.raised else Elevation.flat)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AchievementBadge(motifKind = item.def.motifKind, unlocked = item.unlocked)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    item.def.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (item.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (item.unlocked) item.def.displayDescription() else stringResource(R.string.achievements_locked_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
