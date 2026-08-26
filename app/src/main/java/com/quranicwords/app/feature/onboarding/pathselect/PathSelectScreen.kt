package com.quranicwords.app.feature.onboarding.pathselect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.LearningPath
import com.quranicwords.app.core.ui.components.StaggeredEntrance
import com.quranicwords.app.core.ui.components.QwSelectableCard

/**
 * Onboarding step right after language selection - forks the whole app experience into [LearningPath.LEARN]
 * (the original guided chapter/section/lesson path) or [LearningPath.TEST_ONLY] (no teach content,
 * just scored quiz practice from the whole word pool - see `Route.OpenPractice`). Changeable later
 * in Settings, same as every other onboarding choice.
 */
@Composable
fun PathSelectScreen(
    onContinue: () -> Unit,
    viewModel: PathSelectViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(top = 24.dp, start = 24.dp, end = 24.dp)
    ) {
        Text(
            stringResource(R.string.onboarding_path_select_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            stringResource(R.string.onboarding_path_select_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(LearningPath.entries) { index, path ->
                StaggeredEntrance(index = index) {
                    PathOptionCard(path = path, onClick = { viewModel.selectPath(path, onContinue) })
                }
            }
        }
    }
}

@Composable
private fun PathOptionCard(path: LearningPath, onClick: () -> Unit) {
    val (icon, titleRes, descriptionRes) = when (path) {
        LearningPath.LEARN -> Triple(Icons.Filled.AutoStories, R.string.path_learn_title, R.string.path_learn_description)
        LearningPath.TEST_ONLY -> Triple(Icons.Filled.Quiz, R.string.path_test_only_title, R.string.path_test_only_description)
    }
    QwSelectableCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
