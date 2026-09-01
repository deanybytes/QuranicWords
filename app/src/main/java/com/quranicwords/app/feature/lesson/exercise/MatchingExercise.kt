package com.quranicwords.app.feature.lesson.exercise

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.MatchPair
import com.quranicwords.app.core.domain.model.getOrNull
import com.quranicwords.app.core.domain.model.localizedLabel
import com.quranicwords.app.core.domain.model.localizedPrompt
import com.quranicwords.app.core.domain.model.localizedRight
import com.quranicwords.app.core.ui.components.GlassSurface
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.motion.pressDepth
import com.quranicwords.app.core.ui.motion.rememberQwHaptics
import com.quranicwords.app.core.ui.motion.rememberReducedGlass
import com.quranicwords.app.core.ui.motion.rememberReducedMotion
import com.quranicwords.app.core.ui.theme.BrandGold
import com.quranicwords.app.core.ui.theme.QuranCitationFontFamily
import com.quranicwords.app.feature.lesson.MismatchEvent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.quranicwords.app.core.ui.theme.LocalQuranFontFamily

/** Sentinel id for the extra, never-matchable meaning-side tile (see
 * [ExerciseContent.Matching.distractorRight]) - never equals a real [MatchPair.id] ("p1", "p2",
 * ...), so tapping it always resolves as a mismatch against whatever is pending on the left. */
private const val DISTRACTOR_TILE_ID = "__distractor__"

/** One right-column entry: either a real, matchable pair or the one unmatchable distractor -
 * unified so both render through the same row/tile path. */
private sealed interface RightEntry {
    data class Pair(val pair: MatchPair) : RightEntry
    data class Distractor(val option: com.quranicwords.app.core.domain.model.ChoiceOption) : RightEntry
}

private fun RightEntry.id(): String = when (this) {
    is RightEntry.Pair -> pair.effectiveId
    is RightEntry.Distractor -> DISTRACTOR_TILE_ID
}

private fun RightEntry.text(language: Language): String = when (this) {
    is RightEntry.Pair -> pair.localizedRight(language)
    is RightEntry.Distractor -> option.localizedLabel(language)
}

/**
 * Matching exercise with aligned row grid, golden border on selection, glass reflect animation
 * on correct match, and red glow + vibration shake on mismatch.
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
    val reducedGlass = rememberReducedGlass()
    val haptics = rememberQwHaptics()

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

    val leftItems by remember(content) {
        derivedStateOf {
            val matched = matchOrder.mapNotNull { id -> leftShuffled.find { it.effectiveId == id } }
            matched + leftShuffled.filter { it.effectiveId !in matchOrder }
        }
    }
    val rightItems by remember(content) {
        derivedStateOf {
            val matched = matchOrder.mapNotNull { id -> rightShuffled.find { it.id() == id } }
            matched + rightShuffled.filter { it.id() !in matchOrder }
        }
    }

    val mismatchState = remember(content) { mutableStateMapOf<String, Boolean>() }
    val shakeOffsets = remember(content) { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }

    LaunchedEffect(lastMismatch) {
        val mismatch = lastMismatch ?: return@LaunchedEffect
        val mismatchIds = listOf(mismatch.leftId, mismatch.rightId)
        mismatchIds.forEach { id -> mismatchState[id] = true }
        haptics.onIncorrectAnswer()

        if (!reducedMotion) {
            coroutineScope {
                mismatchIds.forEach { id ->
                    launch {
                        val animatable = shakeOffsets.getOrPut(id) { Animatable(0f) }
                        animatable.snapTo(0f)
                        listOf(12f, -10f, 8f, -6f, 4f, -2f, 0f).forEach { target ->
                            animatable.animateTo(target, animationSpec = tween(50))
                        }
                    }
                }
            }
        }
        delay(550L)
        mismatchIds.forEach { id -> mismatchState[id] = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = content.localizedPrompt(language),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        val maxCount = maxOf(leftItems.size, rightItems.size)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (i in 0 until maxCount) {
                val left = leftItems.getOrNull(i)
                val right = rightItems.getOrNull(i)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (left != null) {
                            key(left.effectiveId) {
                                MatchTile(
                                    text = left.effectiveLeftArabic,
                                    isArabic = true,
                                    isMatched = left.effectiveId in matchedPairIds,
                                    isSelected = left.effectiveId == pendingLeftId,
                                    isMismatch = mismatchState[left.effectiveId] == true,
                                    shakeOffset = shakeOffsets[left.effectiveId],
                                    reducedMotion = reducedMotion,
                                    reducedGlass = reducedGlass,
                                    onClick = { onSelectLeft(left.effectiveId) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (right != null) {
                            key(right.id()) {
                                MatchTile(
                                    text = right.text(language),
                                    isArabic = false,
                                    isMatched = right.id() in matchedPairIds,
                                    isSelected = false,
                                    isMismatch = mismatchState[right.id()] == true,
                                    shakeOffset = shakeOffsets[right.id()],
                                    reducedMotion = reducedMotion,
                                    reducedGlass = reducedGlass,
                                    onClick = { onSelectRight(right.id()) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }

        val selectedPair = leftItems.find { it.effectiveId == pendingLeftId }
        val exampleArabic = selectedPair?.exampleVerseArabic
        val exampleRef = selectedPair?.exampleVerseReference

        AnimatedVisibility(
            visible = selectedPair != null && exampleArabic != null && exampleRef != null,
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 }
        ) {
            if (selectedPair != null && exampleArabic != null && exampleRef != null) {
                val highlightStyle = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                GlassSurface(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    tint = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.lesson_word_example_verse_label,
                                com.quranicwords.app.core.util.VerseReferenceFormatter.format(exampleRef, language)
                            ),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = QuranCitationFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = buildAnnotatedString {
                                append(exampleArabic)
                                val start = selectedPair.arabicWordStart
                                val end = selectedPair.arabicWordEnd
                                if (start != null && end != null &&
                                    start in 0..exampleArabic.length &&
                                    end in start..exampleArabic.length
                                ) {
                                    addStyle(highlightStyle, start, end)
                                }
                            },
                            fontFamily = LocalQuranFontFamily.current,
                            fontSize = 19.sp,
                            lineHeight = 32.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface
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
    isArabic: Boolean,
    isMatched: Boolean,
    isSelected: Boolean,
    isMismatch: Boolean,
    shakeOffset: Animatable<Float, AnimationVector1D>?,
    reducedMotion: Boolean,
    reducedGlass: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Brief "just matched" celebration window right after a correct match - drives the green glow border,
    // Alhamdulillah-style glass reflect sheen sweep animation, and delays the fade-to-retired alpha.
    var justMatched by remember { mutableStateOf(false) }
    val sheenProgress = remember { Animatable(0f) }
    var tapClickCount by remember { mutableIntStateOf(0) }
    val tapReflectionProgress = remember { Animatable(0f) }

    LaunchedEffect(isMatched) {
        if (isMatched) {
            justMatched = true
            if (!reducedMotion && !reducedGlass) {
                sheenProgress.snapTo(0f)
                sheenProgress.animateTo(1f, animationSpec = tween(durationMillis = 850, easing = LinearEasing))
            }
            delay(if (reducedMotion) 0L else 850L)
            justMatched = false
        }
    }

    LaunchedEffect(tapClickCount) {
        if (tapClickCount > 0 && !reducedMotion && !reducedGlass && !isMatched && !justMatched) {
            tapReflectionProgress.snapTo(0f)
            tapReflectionProgress.animateTo(1f, animationSpec = tween(durationMillis = 450, easing = LinearEasing))
        }
    }

    val targetContainer = when {
        justMatched -> if (reducedGlass) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        isMismatch -> if (reducedGlass) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        isMatched -> if (reducedGlass) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
        isSelected -> if (reducedGlass) MaterialTheme.colorScheme.secondaryContainer else BrandGold.copy(alpha = 0.22f)
        else -> if (reducedGlass) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val container by animateColorAsState(targetContainer, tween(250), label = "matchTileContainer")

    val targetContentColor = when {
        justMatched -> MaterialTheme.colorScheme.onPrimaryContainer
        isMismatch -> MaterialTheme.colorScheme.onErrorContainer
        isMatched -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        isSelected -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface
    }
    val contentColor by animateColorAsState(targetContentColor, tween(200), label = "matchTileContentColor")

    val tileAlpha by animateFloatAsState(
        targetValue = if (isMatched && !justMatched) 0.55f else 1f,
        animationSpec = tween(durationMillis = 400, delayMillis = if (isMatched && !reducedMotion) 150 else 0),
        label = "matchTileAlpha"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = MotionSpecs.celebratory(),
        label = "matchTileSelectionScale"
    )

    val tileShape = RoundedCornerShape(14.dp)

    val borderModifier = when {
        justMatched -> Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, tileShape)
        isMismatch -> Modifier.border(2.5.dp, MaterialTheme.colorScheme.error, tileShape)
        isSelected -> Modifier.border(2.5.dp, BrandGold, tileShape)
        isMatched -> Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), tileShape)
        reducedGlass -> Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), tileShape)
        else -> Modifier.border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)
            ),
            shape = tileShape
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .graphicsLayer {
                translationX = shakeOffset?.value ?: 0f
                scaleX = selectionScale
                scaleY = selectionScale
                alpha = tileAlpha
            }
            .pressDepth(interactionSource)
            .clip(tileShape)
            .background(container)
            .then(borderModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isMatched && !isMismatch,
                onClick = {
                    tapClickCount++
                    onClick()
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontFamily = if (isArabic) LocalQuranFontFamily.current else null,
                style = if (isArabic) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = contentColor
            )
        }

        // Tap glass reflection sweep on click (same technique as GlassSurface)
        if (tapReflectionProgress.value > 0f && !reducedMotion && !reducedGlass && !justMatched) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(tileShape)
                    .drawBehind {
                        val w = size.width
                        val h = size.height
                        val bandWidth = w * 0.4f
                        val travel = w * 1.8f
                        val bandCenter = -w * 0.4f + tapReflectionProgress.value * travel
                        rotate(degrees = 20f, pivot = Offset(w / 2f, h / 2f)) {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colorStops = arrayOf(
                                        0f to Color.Transparent,
                                        0.5f to Color.White.copy(alpha = 0.5f),
                                        1f to Color.Transparent
                                    ),
                                    start = Offset(bandCenter - bandWidth / 2f, 0f),
                                    end = Offset(bandCenter + bandWidth / 2f, 0f)
                                ),
                                topLeft = Offset(-w, -h),
                                size = Size(w * 3f, h * 3f),
                                blendMode = BlendMode.Screen
                            )
                        }
                    }
            )
        }

        // Glossy celebratory glass reflect sheen sweep on correct match (exact technique as AnswerFeedbackOverlay / "Alhamdulillah")
        if (justMatched && !reducedMotion && !reducedGlass) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(tileShape)
                    .drawBehind {
                        val w = size.width
                        val h = size.height
                        val bandWidth = w * 0.35f
                        val travel = w * 1.8f
                        val bandCenter = -w * 0.4f + sheenProgress.value * travel
                        rotate(degrees = 20f, pivot = Offset(w / 2f, h / 2f)) {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colorStops = arrayOf(
                                        0f to Color.Transparent,
                                        0.5f to Color.White.copy(alpha = 0.65f),
                                        1f to Color.Transparent
                                    ),
                                    start = Offset(bandCenter - bandWidth / 2f, 0f),
                                    end = Offset(bandCenter + bandWidth / 2f, 0f)
                                ),
                                topLeft = Offset(-w, -h),
                                size = Size(w * 3f, h * 3f),
                                blendMode = BlendMode.Screen
                            )
                        }
                    }
            )
        }
    }
}

