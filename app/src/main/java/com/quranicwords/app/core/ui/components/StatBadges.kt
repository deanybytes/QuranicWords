package com.quranicwords.app.core.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.ui.motion.MotionSpecs

@Composable
fun PointsBadge(points: Int, modifier: Modifier = Modifier) {
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
            Text(animatedPoints.toString(), color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
fun StreakBadge(streakDays: Int, modifier: Modifier = Modifier) {
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
            // Rive-driven flame (falls back to a static icon until a .riv asset is bundled - see
            // RiveStreakFlame). Its own state-machine input reacts live to streakDays.
            RiveStreakFlame(streakDays = streakDays)
            Text(animatedStreak.toString(), color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}
