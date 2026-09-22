package com.quranicwords.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.util.VerseReferenceFormatter

@Composable
fun PointsBadge(points: Int, modifier: Modifier = Modifier) {
    val language = rememberSelectedLanguage()
    val description = stringResource(R.string.points_content_description, points)
    val animatedPoints by animateIntAsState(targetValue = points, animationSpec = MotionSpecs.countUp, label = "points")
    Surface(
        modifier = modifier.semantics { contentDescription = description },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = VerseReferenceFormatter.formatDigits(animatedPoints.toString(), language),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun StreakBadge(streakDays: Int, modifier: Modifier = Modifier) {
    val language = rememberSelectedLanguage()
    val description = stringResource(R.string.streak_content_description, streakDays)
    val animatedStreak by animateIntAsState(targetValue = streakDays, animationSpec = MotionSpecs.countUp, label = "streak")
    Surface(
        modifier = modifier.semantics { contentDescription = description },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StreakFlame(streakDays = streakDays)
            Text(
                text = VerseReferenceFormatter.formatDigits(animatedStreak.toString(), language),
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/** Replaces [StreakBadge] when [com.quranicwords.app.core.domain.StreakRecovery.isLocked] is
 * true - tapping it starts the recovery quiz (`Route.StreakRecovery`). Deliberately not animated
 * the way [DailyGoalBadge] is: a lock appearing isn't a reward moment worth celebrating, it's a
 * warning that needs to be noticed and acted on. */
@Composable
fun StreakLockedChip(questionCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.streak_locked_recover_hint, questionCount)
    Surface(
        modifier = modifier.clickable(onClick = onClick).semantics { contentDescription = description },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(stringResource(R.string.streak_locked_label), color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

/** The "daily challenge completed" indicator (item 5 of the original redesign request) - a
 * celebratory scale+fade reveal (matching the reward-moment vocabulary already used at lesson-
 * complete/achievement-unlock) rather than a plain conditional row, since crossing today's goal
 * is itself a small reward moment worth marking distinctly from the neutral points/streak badges
 * beside it. [visible] is expected to flip live as the learner practices (see
 * [com.quranicwords.app.feature.home.HomeUiState.isDailyGoalMetToday]), not just on first load. */
@Composable
fun DailyGoalBadge(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(animationSpec = MotionSpecs.celebratory()) + fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    stringResource(R.string.home_daily_challenge_completed),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/** A small icon + short label pill (e.g. a repeat icon with "3x", or a clock icon with "10 min")
 * used to make a quantity legible at a glance regardless of the current app language, alongside
 * (not instead of) the fully translated sentence describing it. [label] is deliberately not a
 * string resource - it's a language-agnostic numeral+unit shorthand, not prose. */
@Composable
fun IconLabelChip(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
