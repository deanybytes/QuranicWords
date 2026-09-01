package com.quranicwords.app.feature.onboarding.learningstyle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
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
import com.quranicwords.app.core.domain.model.LearningStyle
import com.quranicwords.app.core.ui.components.IconLabelChip
import com.quranicwords.app.core.ui.components.StaggeredEntrance
import com.quranicwords.app.core.ui.components.QwSelectableCard

/**
 * Onboarding step between the font step and Daily Goal - how many times each word (and each
 * Matching "exam" cycle) repeats within a lesson. See [LearningStyle] and
 * [com.quranicwords.app.core.domain.LessonContentRepeater] for how the choice is actually
 * applied at runtime. Changeable later in Settings, same as language/font/daily goal.
 */
@Composable
fun LearningStyleSelectScreen(
    onContinue: () -> Unit,
    viewModel: LearningStyleSelectViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(top = 24.dp, start = 24.dp, end = 24.dp)
    ) {
        Text(
            stringResource(R.string.onboarding_learning_style_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            stringResource(R.string.onboarding_learning_style_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(LearningStyle.entries) { index, style ->
                StaggeredEntrance(index = index) {
                    LearningStyleOptionCard(style = style, onClick = { viewModel.selectStyle(style, onContinue) })
                }
            }
        }
    }
}

@Composable
private fun LearningStyleOptionCard(style: LearningStyle, onClick: () -> Unit) {
    val (nameRes, descriptionRes) = when (style) {
        LearningStyle.SHARP -> R.string.learning_style_sharp to R.string.learning_style_sharp_description
        LearningStyle.SLOW -> R.string.learning_style_slow to R.string.learning_style_slow_description
        LearningStyle.COZY -> R.string.learning_style_cozy to R.string.learning_style_cozy_description
    }
    QwSelectableCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(nameRes), style = MaterialTheme.typography.titleMedium)
                IconLabelChip(icon = Icons.Filled.Repeat, label = stringResource(R.string.format_repeat_count, style.repeatCount))
            }
            Text(
                stringResource(descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
