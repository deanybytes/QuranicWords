package com.quranicwords.app.feature.home

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.data.local.entity.DailyQuestEntity
import com.quranicwords.app.core.domain.QuestMetric
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.ui.components.GlassSurface
import com.quranicwords.app.core.ui.components.charts.DonutChart
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.motion.pressDepth
import com.quranicwords.app.core.ui.theme.Elevation
import com.quranicwords.app.core.ui.theme.QwShapes
import com.quranicwords.app.core.util.VerseReferenceFormatter

/** Today's practice minutes against the daily goal, visible from 0% - not only once it's met. */
@Composable
fun DailyGoalRing(minutes: Int, goalMinutes: Int, size: Dp = 40.dp) {
    val language = rememberSelectedLanguage()
    val description = stringResource(
        R.string.a11y_daily_goal_ring,
        VerseReferenceFormatter.formatNumber(minutes.coerceAtMost(goalMinutes), language),
        VerseReferenceFormatter.formatNumber(goalMinutes, language)
    )
    DonutChart(
        percent = if (goalMinutes <= 0) 0f else (minutes.toFloat() / goalMinutes).coerceIn(0f, 1f),
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        size = size,
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f),
        centerLabel = VerseReferenceFormatter.formatNumber(minutes, language),
        labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onPrimaryContainer)
    )
}

/** The quest's title for its id, with its target filled in. */
@Composable
private fun questTitle(quest: DailyQuestEntity): String {
    val language = rememberSelectedLanguage()
    val target = VerseReferenceFormatter.formatNumber(quest.target, language)
    val res = when (QuestMetric.entries.find { it.name == quest.metric }) {
        QuestMetric.REVIEW_WORDS -> R.string.quest_review_words
        QuestMetric.COMBO -> R.string.quest_combo
        QuestMetric.FINISH_LESSONS -> R.string.quest_finish_lessons
        QuestMetric.EARN_XP -> R.string.quest_earn_xp
        QuestMetric.DAILY_GOAL_MINUTES -> R.string.quest_daily_goal
        QuestMetric.LISTENING -> R.string.quest_listening
        QuestMetric.NEW_WORDS, null -> R.string.quest_new_words
    }
    return stringResource(res, target)
}

/** "Today's quests": each with its progress bar, reward and a check once done. */
@Composable
fun QuestsCard(quests: List<DailyQuestEntity>) {
    val language = rememberSelectedLanguage()
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = QwShapes.large, tint = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.home_quests_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            quests.forEach { quest ->
                val done = quest.completedAtEpochMillis != null
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                questTitle(quest),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                stringResource(R.string.quest_reward, VerseReferenceFormatter.formatNumber(quest.rewardXp, language)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { if (quest.target <= 0) 1f else (quest.progress.toFloat() / quest.target).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(QwShapes.extraSmall),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                        Text(
                            stringResource(
                                R.string.quest_progress,
                                VerseReferenceFormatter.formatNumber(quest.progress, language),
                                VerseReferenceFormatter.formatNumber(quest.target, language)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** Offers to continue an interrupted lesson exactly where it stopped. */
@Composable
fun ResumeLessonCard(resume: ResumeInfo, onResume: () -> Unit) {
    val language = rememberSelectedLanguage()
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = Modifier.fillMaxWidth().pressDepth(interactionSource),
        shape = QwShapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.flat),
        onClick = onResume,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                Icons.Filled.PlayCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(36.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_resume_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    stringResource(
                        R.string.home_resume_subtitle,
                        resume.lessonTitle.get(language),
                        VerseReferenceFormatter.formatNumber(resume.position, language),
                        VerseReferenceFormatter.formatNumber(resume.total, language)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
