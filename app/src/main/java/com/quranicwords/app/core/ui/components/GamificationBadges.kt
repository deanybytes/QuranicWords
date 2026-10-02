package com.quranicwords.app.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.HeartsCalculator
import com.quranicwords.app.core.domain.LevelCurve
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.motion.rememberReducedMotion
import com.quranicwords.app.core.ui.theme.QwShapes
import com.quranicwords.app.core.util.VerseReferenceFormatter

/** Remaining hearts as a compact pill (filled heart + count) - error-red only when empty, since
 * running out is the one state that needs attention. */
@Composable
fun HeartsBadge(hearts: Int, modifier: Modifier = Modifier) {
    val language = rememberSelectedLanguage()
    val description = stringResource(
        R.string.a11y_hearts,
        VerseReferenceFormatter.formatNumber(hearts, language),
        VerseReferenceFormatter.formatNumber(HeartsCalculator.MAX_HEARTS, language)
    )
    val empty = hearts <= 0
    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(50),
        color = if (empty) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tint = if (empty) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
            Icon(
                imageVector = if (empty) Icons.Filled.FavoriteBorder else Icons.Filled.Favorite,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(VerseReferenceFormatter.formatNumber(hearts, language), color = tint, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Combo meter for the lesson top bar - shown from a 2-answer streak upward, with a gentle pulse
 * each time it grows (none under reduced motion). Turns gold-accented once the combo is paying a
 * bonus (see GamificationConfig.COMBO_TIER_ONE).
 */
@Composable
fun ComboMeter(combo: Int, bonusActive: Boolean, modifier: Modifier = Modifier) {
    val language = rememberSelectedLanguage()
    val reducedMotion = rememberReducedMotion()
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(combo) {
        if (!reducedMotion && combo > 1) {
            pulse.snapTo(1.15f)
            pulse.animateTo(1f, MotionSpecs.celebratory())
        }
    }
    val description = stringResource(R.string.a11y_combo, VerseReferenceFormatter.formatNumber(combo, language))
    Surface(
        modifier = modifier
            .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
            .clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(50),
        color = if (bonusActive) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tint = if (bonusActive) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(
                text = "×" + VerseReferenceFormatter.formatNumber(combo, language),
                color = tint,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Level badge plus an XP bar to the next level (see [LevelCurve]) - Home header and Progress. */
@Composable
fun LevelProgressBar(totalXp: Int, modifier: Modifier = Modifier) {
    val language = rememberSelectedLanguage()
    val progress = remember(totalXp) { LevelCurve.progressFor(totalXp) }
    val levelText = stringResource(R.string.level_label, VerseReferenceFormatter.formatNumber(progress.level, language))
    val xpText = stringResource(
        R.string.level_xp_progress,
        VerseReferenceFormatter.formatNumber(progress.xpIntoLevel, language),
        VerseReferenceFormatter.formatNumber(progress.xpForNextLevel, language)
    )
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$levelText, $xpText" },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
            Text(levelText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                xpText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
        LinearProgressIndicator(
            progress = { progress.fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(QwShapes.extraSmall),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
