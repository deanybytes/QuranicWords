package com.quranicwords.app.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.ui.motion.rememberReducedMotion
import com.quranicwords.app.core.ui.theme.StreakAccent

/**
 * The QuranicWords brand mark (`drawable-nodpi/ic_qw_mark.png`, sourced from `assets/image/LOGO.png`
 * - a circular badge: dark-green field, gold ring, geometric leaf/diamond motif and minaret, no
 * human faces), used everywhere the app shows its identity (splash, onboarding, Settings About,
 * chapter/section intro, word-browse). A slow, subtle glow pulse behind the mark is the animation
 * this is meant to carry - not a spin or bounce, since a logo should read as calm/premium rather
 * than playful. No-ops to a static mark under reduced motion.
 *
 * Two-layer glow (a tight inner pulse + a larger, softer, phase-offset outer bloom) rather than
 * one flat ring, so the mark reads as genuinely luminous against the surrounding background
 * instead of blending into it.
 */
@Composable
fun QwLogo(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    val reducedMotion = rememberReducedMotion()
    val infiniteTransition = rememberInfiniteTransition(label = "qwLogoGlow")
    val innerGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = if (reducedMotion) 0.20f else 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "qwLogoInnerGlowAlpha"
    )
    val outerGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = if (reducedMotion) 0.06f else 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3100),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(650, StartOffsetType.FastForward)
        ),
        label = "qwLogoOuterGlowAlpha"
    )

    Image(
        painter = painterResource(R.drawable.ic_qw_mark),
        contentDescription = stringResource(R.string.app_logo_content_description),
        modifier = modifier
            .size(size)
            .drawBehind {
                drawCircle(color = StreakAccent, radius = size.toPx() * 0.95f, alpha = outerGlowAlpha)
                drawCircle(color = StreakAccent, radius = size.toPx() * 0.68f, alpha = innerGlowAlpha)
            }
    )
}
