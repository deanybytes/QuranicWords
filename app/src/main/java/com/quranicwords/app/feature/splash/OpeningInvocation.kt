package com.quranicwords.app.feature.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranicwords.app.R
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.motion.rememberQwHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class InvocationPhase { TAAWWUDH, BASMALA, RABBI_ZIDNI }

private data class InvocationText(val arabicRes: Int, val translationRes: Int, val referenceRes: Int? = null)

private val PHASE_TEXT = mapOf(
    InvocationPhase.TAAWWUDH to InvocationText(
        R.string.splash_invocation_taawwudh_arabic,
        R.string.splash_invocation_taawwudh_translation
    ),
    InvocationPhase.BASMALA to InvocationText(
        R.string.splash_invocation_basmala_arabic,
        R.string.splash_invocation_basmala_translation
    ),
    InvocationPhase.RABBI_ZIDNI to InvocationText(
        R.string.splash_invocation_rabbizidni_arabic,
        R.string.splash_invocation_rabbizidni_translation,
        R.string.splash_invocation_rabbizidni_reference
    )
)

/**
 * The app's opening devotional sequence, shown every launch (not just first-run): the Ta'awwudh,
 * then the Basmala, then "Rabbi zidni ilma" (Qur'an 20:114) pulsed three times, matching the
 * traditional practice of repeating that particular dua thrice. These are functional openers -
 * said before any act of learning, not decorative - a deliberate, narrow exception to this app's
 * usual "no rendering of Ayat/Mushaf text as decoration" guardrail (see [SplashScreen]'s doc
 * comment): nothing here is used to skin a loading spinner or gamification flourish, it plays
 * once at the start of the session and then gets out of the way.
 *
 * Tap-anywhere advances immediately (to the next phase, or finishes on the last one) - a 10-ish
 * second animation on every single app open needs an escape hatch for daily returning users, not
 * just first-time ones. [onFinished] fires once, either when the sequence completes naturally or
 * is skipped early. Uses `Modifier.clickable` (not a raw `pointerInput`/`detectTapGestures`
 * gesture detector, which produces no accessibility node at all) so this is a real, focusable,
 * TalkBack-activatable control with its own announced label - a screen-reader user on a real
 * device wouldn't otherwise have any way to discover or trigger the skip action every single
 * launch, found during the QW-16 accessibility pass.
 */
@Composable
fun OpeningInvocationSequence(reducedMotion: Boolean, onFinished: () -> Unit) {
    val haptics = rememberQwHaptics()
    val coroutineScope = rememberCoroutineScope()
    var phaseIndex by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(false) }
    var pulseCount by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    val phases = InvocationPhase.entries
    val currentPhase = phases[phaseIndex]

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = 500),
        label = "invocationAlpha"
    )

    fun finish() {
        if (finished) return
        finished = true
        onFinished()
    }

    fun advance() {
        if (finished) return
        if (phaseIndex < phases.lastIndex) {
            visible = false
            phaseIndex += 1
        } else {
            visible = false
            finish()
        }
    }

    LaunchedEffect(phaseIndex) {
        pulseCount = 0
        visible = true
        val holdMillis = if (reducedMotion) 700L else if (currentPhase == InvocationPhase.BASMALA) 2000L else 1800L
        if (currentPhase == InvocationPhase.RABBI_ZIDNI && !reducedMotion) {
            // Three gentle pulses in place, echoing the tradition of repeating this dua thrice,
            // rather than literally re-showing the block three times in a row.
            repeat(3) {
                delay(900)
                pulseCount += 1
            }
            delay(400)
        } else {
            delay(holdMillis)
        }
        if (!finished) {
            visible = false
            delay(if (reducedMotion) 0 else 350)
            advance()
        }
    }

    val pulseScale by animateFloatAsState(
        targetValue = 1f + (pulseCount % 2) * 0.06f,
        animationSpec = MotionSpecs.gentle(),
        label = "invocationPulse"
    )

    val skipInteractionSource = remember { MutableInteractionSource() }
    val skipLabel = stringResource(R.string.splash_invocation_skip_hint)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = skipInteractionSource,
                indication = null,
                onClickLabel = skipLabel
            ) {
                coroutineScope.launch {
                    haptics.onSelect()
                    advance()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val text = PHASE_TEXT.getValue(currentPhase)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .alpha(alpha)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(text.arabicRes),
                fontSize = 34.sp,
                lineHeight = 52.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(text.translationRes),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            text.referenceRes?.let { refRes ->
                Text(
                    text = stringResource(refRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (currentPhase == InvocationPhase.RABBI_ZIDNI) {
                Spacer(modifier = Modifier.height(4.dp))
                RepeatDots(filled = pulseCount)
            }
        }

        Text(
            text = stringResource(R.string.splash_invocation_skip_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(alpha)
        )
    }
}

/** Three small dots that fill in as [filled] (0-3) increases - a quiet visual echo of "said three
 * times" instead of literally re-rendering the phrase three times over. */
@Composable
private fun RepeatDots(filled: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            val filledIn = index < filled
            val dotAlpha by animateFloatAsState(
                targetValue = if (filledIn) 1f else 0.25f,
                animationSpec = tween(durationMillis = 300),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(dotAlpha)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}
