package com.quranicwords.app.core.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.ui.motion.rememberReducedMotion
import com.quranicwords.app.core.ui.theme.BrandDarkGreen
import com.quranicwords.app.core.ui.theme.BrandGold
import com.quranicwords.app.core.ui.theme.BrandGreen
import com.quranicwords.app.core.ui.theme.BrandLightGreen

/**
 * The QuranicWords brand mark (`drawable-nodpi/ic_qw_mark.png`, sourced from `assets/image/LOGO.png`
 * - a circular badge: dark-green field, gold ring, geometric leaf/diamond motif and minaret, no
 * human faces), used everywhere the app shows its identity (splash, onboarding, Settings About,
 * chapter/section intro, word-browse). A slow glow pulse behind the mark is the animation this is
 * meant to carry - not a spin or bounce, since a logo should read as calm/premium rather than
 * playful. No-ops to a static mark under reduced motion.
 *
 * Two-layer glow (a tight inner pulse + a larger, softer, phase-offset outer bloom) rather than
 * one flat ring, so the mark reads as genuinely luminous against the surrounding background
 * instead of blending into it. The glow's *color* itself slowly cycles through the full logo
 * palette (gold -> green -> light green -> dark green -> back to gold, see docs/UI_GUIDELINES.md's
 * "Brand colors" section) rather than staying a fixed gold, so the mark reads as alive rather than
 * a static badge with a single pulsing halo.
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
    val glowColor: Color by infiniteTransition.animateColor(
        initialValue = BrandGold,
        targetValue = BrandGold,
        animationSpec = infiniteRepeatable(
            animation = if (reducedMotion) {
                tween(durationMillis = 0)
            } else {
                keyframes {
                    durationMillis = 7000
                    BrandGold at 0
                    BrandGreen at 1750
                    BrandLightGreen at 3500
                    BrandDarkGreen at 5250
                    BrandGold at 7000
                }
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "qwLogoGlowColor"
    )

    Image(
        painter = painterResource(R.drawable.ic_qw_mark),
        contentDescription = stringResource(R.string.app_logo_content_description),
        modifier = modifier
            .size(size)
            .drawBehind {
                drawCircle(color = glowColor, radius = size.toPx() * 0.95f, alpha = outerGlowAlpha)
                drawCircle(color = glowColor, radius = size.toPx() * 0.68f, alpha = innerGlowAlpha)
            }
    )
}
