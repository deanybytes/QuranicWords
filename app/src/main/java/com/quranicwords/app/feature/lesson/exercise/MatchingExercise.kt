package com.quranicwords.app.feature.lesson.exercise

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.MatchPair
import com.quranicwords.app.core.domain.model.localizedLabel
import com.quranicwords.app.core.domain.model.localizedPrompt
import com.quranicwords.app.core.domain.model.localizedRight
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.motion.pressDepth
import com.quranicwords.app.core.ui.motion.rememberReducedMotion
import com.quranicwords.app.core.ui.theme.Elevation
import com.quranicwords.app.feature.lesson.MismatchEvent
import kotlinx.coroutines.delay

/** Sentinel id for the extra, never-matchable meaning-side tile (see
 * [ExerciseContent.Matching.distractorRight]) - never equals a real [MatchPair.id] ("p1", "p2",
 * ...), so tapping it always resolves as a mismatch against whatever is pending on the left. */
private const val DISTRACTOR_TILE_ID = "__distractor__"

/** One right-column entry: either a real, matchable pair or the one unmatchable distractor -
 * unified so both render through the same [LazyColumn]/[MatchTile] path. */
private sealed interface RightEntry {
    data class Pair(val pair: MatchPair) : RightEntry
    data class Distractor(val option: com.quranicwords.app.core.domain.model.ChoiceOption) : RightEntry
}

private fun RightEntry.id(): String = when (this) {
    is RightEntry.Pair -> pair.id
    is RightEntry.Distractor -> DISTRACTOR_TILE_ID
}

private fun RightEntry.text(language: Language): String = when (this) {
    is RightEntry.Pair -> pair.localizedRight(language)
    is RightEntry.Distractor -> option.localizedLabel(language)
}

/**
 * Redesigned matching exercise: an extra unmatchable meaning-side tile, a green glow + retire-
 * to-top-and-fade animation on a correct match, a shake + haptic buzz on a wrong one, and no
 * connecting lines between matched pairs (matched tiles land at the same row in both columns
 * instead, communicating pairing through position alone).
 */
@Composable
fun MatchingExerciseContent(
    content: ExerciseContent.Matching,
    matchedPairIds: Set<String>,
    pendingLeftId: String?,
    lastMismatch: MismatchEvent?,
    onSelectLeft: (String) -> Unit,
    onSelectRight: (String) -> Unit
) {
    val language = rememberSelectedLanguage()
    val reducedMotion = rememberReducedMotion()

    val leftShuffled = remember(content) { content.pairs.shuffled() }
    val rightShuffled = remember(content) {
        val entries: List<RightEntry> = content.pairs.map { RightEntry.Pair(it) }
        val withDistractor = content.distractorRight?.let { entries + RightEntry.Distractor(it) } ?: entries
        withDistractor.shuffled()
    }

    // Grows in match order - both columns retire matched tiles to the top in this same shared
    // order, so a pair's left and right tile always land at the same row ("side by side" per the
    // redesign spec), regardless of each column's own independent original shuffle.
    val matchOrder = remember(content) { mutableStateListOf<String>() }
    LaunchedEffect(matchedPairIds) {
        matchedPairIds.forEach { id -> if (id !in matchOrder) matchOrder.add(id) }
    }

    val leftItems by remember {
        derivedStateOf {
            val matched = matchOrder.mapNotNull { id -> leftShuffled.find { it.id == id } }
            matched + leftShuffled.filter { it.id !in matchOrder }
        }
    }
    val rightItems by remember {
        derivedStateOf {
            val matched = matchOrder.mapNotNull { id -> rightShuffled.find { it.id() == id } }
            matched + rightShuffled.filter { it.id() !in matchOrder }
        }
    }

    val shakeOffsets = remember(content) { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }
    LaunchedEffect(lastMismatch) {
        val mismatch = lastMismatch ?: return@LaunchedEffect
        if (reducedMotion) return@LaunchedEffect
        listOf(mismatch.leftId, mismatch.rightId).forEach { id ->
            val animatable = shakeOffsets.getOrPut(id) { Animatable(0f) }
            animatable.snapTo(0f)
            listOf(10f, -8f, 6f, -4f, 0f).forEach { target ->
                animatable.animateTo(target, animationSpec = tween(50))
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(content.localizedPrompt(language), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.lesson_matching_instruction),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(leftItems, key = { it.id }) { pair ->
                    MatchTile(
                        text = pair.leftArabic,
                        isMatched = pair.id in matchedPairIds,
                        isSelected = pair.id == pendingLeftId,
                        shakeOffset = shakeOffsets[pair.id],
                        reducedMotion = reducedMotion,
                        onClick = { onSelectLeft(pair.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rightItems, key = { it.id() }) { entry ->
                    MatchTile(
                        text = entry.text(language),
                        isMatched = entry.id() in matchedPairIds,
                        isSelected = false,
                        shakeOffset = shakeOffsets[entry.id()],
                        reducedMotion = reducedMotion,
                        onClick = { onSelectRight(entry.id()) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchTile(
    text: String,
    isMatched: Boolean,
    isSelected: Boolean,
    shakeOffset: Animatable<Float, AnimationVector1D>?,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Brief "just matched" window right after a correct match - drives the green glow border and
    // delays the fade-to-retired alpha, so the correctness moment reads clearly before the tile
    // settles into its faded, side-by-side resting state.
    var justMatched by remember { mutableStateOf(false) }
    LaunchedEffect(isMatched) {
        if (isMatched) {
            justMatched = true
            delay(if (reducedMotion) 0L else 450L)
            justMatched = false
        }
    }

    val targetContainer = when {
        justMatched -> MaterialTheme.colorScheme.primary
        isMatched -> MaterialTheme.colorScheme.primaryContainer
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val container by animateColorAsState(targetContainer, tween(250), label = "matchTileContainer")
    val contentColor = if (justMatched) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    val tileAlpha by animateFloatAsState(
        targetValue = if (isMatched) 0.45f else 1f,
        animationSpec = tween(durationMillis = 400, delayMillis = if (isMatched && !reducedMotion) 300 else 0),
        label = "matchTileAlpha"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (justMatched) 0.9f else 0f,
        animationSpec = tween(durationMillis = if (justMatched) 150 else 350),
        label = "matchTileGlow"
    )
    val glowColor = MaterialTheme.colorScheme.primary

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = MotionSpecs.celebratory(),
        label = "matchTileSelectionScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = shakeOffset?.value ?: 0f
                scaleX = selectionScale
                scaleY = selectionScale
                alpha = tileAlpha
            }
            .border(width = 2.dp, color = glowColor.copy(alpha = glowAlpha), shape = RoundedCornerShape(12.dp))
            .pressDepth(interactionSource),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) Elevation.pressed else if (isSelected) Elevation.floating else Elevation.raised
        ),
        onClick = onClick,
        enabled = !isMatched,
        interactionSource = interactionSource
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}
