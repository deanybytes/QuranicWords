package com.quranicwords.app.feature.onboarding.dailygoal

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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.DailyGoalLevel
import com.quranicwords.app.core.ui.components.IconLabelChip
import com.quranicwords.app.core.ui.components.StaggeredEntrance
import com.quranicwords.app.core.ui.components.QwSelectableCard

/**
 * Onboarding step between font selection and Home - a daily practice-time goal (Casual/Steady/
 * Devoted, 10/20/30 minutes), tracked via [com.quranicwords.app.core.data.local.entity.DailyPracticeEntity]
 * and surfaced as a "daily challenge completed" indicator on Home once the day's practice minutes
 * meet the chosen target. Changeable later in Settings, same as language/font.
 */
@Composable
fun DailyGoalSelectScreen(
    onContinue: () -> Unit,
    viewModel: DailyGoalSelectViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(top = 24.dp, start = 24.dp, end = 24.dp)
    ) {
        Text(
            stringResource(R.string.onboarding_daily_goal_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            stringResource(R.string.onboarding_daily_goal_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(DailyGoalLevel.entries) { index, level ->
                StaggeredEntrance(index = index) {
                    DailyGoalOptionCard(level = level, onClick = { viewModel.selectGoal(level, onContinue) })
                }
            }
        }
    }
}

@Composable
private fun DailyGoalOptionCard(level: DailyGoalLevel, onClick: () -> Unit) {
    val nameRes = when (level) {
        DailyGoalLevel.CASUAL -> R.string.daily_goal_casual
        DailyGoalLevel.STEADY -> R.string.daily_goal_steady
        DailyGoalLevel.DEVOTED -> R.string.daily_goal_devoted
    }
    QwSelectableCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(nameRes), style = MaterialTheme.typography.titleMedium)
                IconLabelChip(icon = Icons.Filled.Schedule, label = "${level.minutes} min")
            }
            Text(
                stringResource(R.string.daily_goal_minutes_suffix, level.minutes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
