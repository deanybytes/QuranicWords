package com.quranicwords.app.feature.lesson.exercise

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.localizedPrompt
import com.quranicwords.app.core.domain.model.localizedRight
import com.quranicwords.app.core.ui.components.rememberIsBanglaSelected
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.motion.pressDepth
import com.quranicwords.app.core.ui.motion.rememberReducedMotion
import com.quranicwords.app.core.ui.theme.Elevation
import com.quranicwords.app.feature.lesson.MismatchEvent

@Composable
fun MatchingExerciseContent(
    content: ExerciseContent.Matching,
    matchedPairIds: Set<String>,
    pendingLeftId: String?,
    lastMismatch: MismatchEvent?,
    onSelectLeft: (String) -> Unit,
    onSelectRight: (String) -> Unit
) {
    val isBangla = rememberIsBanglaSelected()
    val reducedMotion = rememberReducedMotion()
    val leftOrder = remember(content) { content.pairs.shuffled() }
    val rightOrder = remember(content) { content.pairs.shuffled() }
    val lineColor = MaterialTheme.colorScheme.primary

    // Tile edge positions (relative to the enclosing Box) and per-pair draw progress, used to
    // paint an animated connector line the moment a pair is matched - previously a match was
    // indicated only by the tiles' own color change, with no line at all. All keyed on [content]
    // like leftOrder/rightOrder above - without that key, these would carry stale pair-id entries
    // (and live Animatable instances) over from a previous Matching exercise in the same lesson,
    // since this composable is reused across exercises in the same composition slot.
    var boxCoordinates by remember(content) { mutableStateOf<LayoutCoordinates?>(null) }
    val leftPositions = remember(content) { mutableStateMapOf<String, Offset>() }
    val rightPositions = remember(content) { mutableStateMapOf<String, Offset>() }
    val connectorProgress = remember(content) { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }
    val shakeOffsets = remember(content) { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }

    LaunchedEffect(matchedPairIds) {
        matchedPairIds.forEach { id ->
            val animatable = connectorProgress.getOrPut(id) { Animatable(0f) }
            if (animatable.value < 1f) {
                if (reducedMotion) animatable.snapTo(1f) else animatable.animateTo(1f, tween(durationMillis = 400))
            }
        }
    }

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
        Text(content.localizedPrompt(isBangla), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.lesson_matching_instruction),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.onGloballyPositioned { boxCoordinates = it },
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    leftOrder.forEach { pair ->
                        val isMatched = pair.id in matchedPairIds
                        MatchTile(
                            text = pair.leftArabic,
                            isMatched = isMatched,
                            isSelected = pair.id == pendingLeftId,
                            shakeOffset = shakeOffsets[pair.id],
                            onClick = { onSelectLeft(pair.id) },
                            modifier = Modifier.onGloballyPositioned { coords ->
                                boxCoordinates?.let { parent ->
                                    val edge = Offset(coords.size.width.toFloat(), coords.size.height / 2f)
                                    leftPositions[pair.id] = parent.localPositionOf(coords, edge)
                                }
                            }
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rightOrder.forEach { pair ->
                        val isMatched = pair.id in matchedPairIds
                        MatchTile(
                            text = pair.localizedRight(isBangla),
                            isMatched = isMatched,
                            isSelected = false,
                            shakeOffset = shakeOffsets[pair.id],
                            onClick = { onSelectRight(pair.id) },
                            modifier = Modifier.onGloballyPositioned { coords ->
                                boxCoordinates?.let { parent ->
                                    val edge = Offset(0f, coords.size.height / 2f)
                                    rightPositions[pair.id] = parent.localPositionOf(coords, edge)
                                }
                            }
                        )
                    }
                }
            }

            Canvas(modifier = Modifier.matchParentSize()) {
                matchedPairIds.forEach { id ->
                    val start = leftPositions[id]
                    val end = rightPositions[id]
                    val progress = connectorProgress[id]?.value ?: 0f
                    if (start != null && end != null && progress > 0f) {
                        val currentEnd = Offset(
                            x = start.x + (end.x - start.x) * progress,
                            y = start.y + (end.y - start.y) * progress
                        )
                        drawLine(
                            color = lineColor,
                            start = start,
                            end = currentEnd,
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = when {
        isMatched -> MaterialTheme.colorScheme.primaryContainer
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
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
            .graphicsLayer { translationX = shakeOffset?.value ?: 0f; scaleX = selectionScale; scaleY = selectionScale }
            .pressDepth(interactionSource),
        colors = CardDefaults.cardColors(containerColor = container),
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
