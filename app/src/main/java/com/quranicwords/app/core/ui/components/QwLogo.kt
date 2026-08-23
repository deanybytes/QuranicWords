package com.quranicwords.app.core.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
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
 * chapter/section intro, word-browse). No-ops every animation below to a static end-state under
 * reduced motion.
 *
 * Three animated layers, back to front - all built from `Brush.radialGradient`/`linearGradient`
 * fades to `Color.Transparent`, never a flat `drawCircle(color, alpha)` fill: a solid-color disc
 * has a hard cutoff at its radius and just fades in/out in place, which reads as a dull flicker
 * rather than light. A true glow needs a soft falloff, so every layer here also breathes its own
 * radius/spread alongside alpha, not alpha alone - motion in extent, not just intensity, is what
 * keeps it from reading as static.
 * 1. An ambient bloom (tight inner glow + larger, softer, phase-offset outer glow) behind the
 *    mark, each a radial gradient fading to transparent, both alpha and radius animated.
 * 2. A rim glow: a radial gradient whose brightness peaks right at the badge's own edge and fades
 *    smoothly to transparent on both sides of it - unlike the bloom (which could sit behind any
 *    circular shape), this one hugs the actual boundary, so it reads as genuine edge-lit glow
 *    rather than a generic backdrop blob. The peak position itself drifts slightly for a living,
 *    breathing rim rather than a fixed ring.
 * 3. A diagonal glossy sheen that sweeps across the badge's face on a loop (hold, glint, hold),
 *    clipped to the badge's own circle and screen-blended over the artwork - the coin/glass-like
 *    highlight that was missing entirely before.
 *
 * All layers share the glow color cycle (gold -> green -> light green -> dark green -> gold, see
 * docs/UI_GUIDELINES.md's "Brand colors" section) so the mark reads as one alive object rather than
 * independently-animated parts.
 */
@Composable
fun QwLogo(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    val reducedMotion = rememberReducedMotion()
    val infiniteTransition = rememberInfiniteTransition(label = "qwLogoGlow")
    val innerGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = if (reducedMotion) 0.28f else 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "qwLogoInnerGlowAlpha"
    )
    val innerGlowSpread by infiniteTransition.animateFloat(
        initialValue = 0.62f,
        targetValue = if (reducedMotion) 0.62f else 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "qwLogoInnerGlowSpread"
    )
    val outerGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = if (reducedMotion) 0.10f else 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3100),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(650, StartOffsetType.FastForward)
        ),
        label = "qwLogoOuterGlowAlpha"
    )
    val outerGlowSpread by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (reducedMotion) 0.95f else 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3100),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(650, StartOffsetType.FastForward)
        ),
        label = "qwLogoOuterGlowSpread"
    )
    // Where the rim glow's brightness peaks, as a fraction of the badge radius - drifts across the
    // badge's own edge (around 1.0) rather than sitting fixed, so the rim reads as breathing light
    // instead of a static ring.
    val rimGlowPeak by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = if (reducedMotion) 0.94f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "qwLogoRimGlowPeak"
    )
    val rimGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = if (reducedMotion) 0.45f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "qwLogoRimGlowAlpha"
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
    // 0 -> 1 sweep of the glossy sheen across the badge's diameter, held at rest on both ends so
    // it reads as a periodic glint (like light catching glass/metal) rather than a looping scroll.
    val sheenProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = if (reducedMotion) {
                tween(durationMillis = 0)
            } else {
                keyframes {
                    durationMillis = 3400
                    0f at 0 using LinearEasing
                    0f at 500
                    1f at 1500 using LinearEasing
                    1f at 3400
                }
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "qwLogoSheenProgress"
    )

    Box(modifier = modifier.size(size)) {
        // Ambient bloom: two soft radial fades, no hard edge anywhere in either gradient.
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val r = this.size.minDimension / 2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = outerGlowAlpha), Color.Transparent),
                            radius = r * outerGlowSpread
                        ),
                        radius = r * outerGlowSpread
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = innerGlowAlpha), Color.Transparent),
                            radius = r * innerGlowSpread
                        ),
                        radius = r * innerGlowSpread
                    )
                }
        )
        // Rim glow: brightness peaks right at the badge's edge and fades smoothly on both sides of
        // it, via gradient color stops rather than a stroked ring - a real soft falloff, not a
        // sharp-perimeter circle appearing/disappearing.
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val r = this.size.minDimension / 2f
                    // Fixed draw extent, comfortably wider than rimGlowPeak's whole range, so the
                    // gradient's stop fractions below stay stable as the peak itself drifts.
                    val outerExtent = r * 1.3f
                    val peakStop = (r * rimGlowPeak / outerExtent).coerceIn(0.1f, 0.9f)
                    val innerStop = (peakStop - 0.14f).coerceAtLeast(0f)
                    val outerStop = (peakStop + 0.14f).coerceAtMost(1f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                innerStop to Color.Transparent,
                                peakStop to glowColor.copy(alpha = rimGlowAlpha),
                                outerStop to Color.Transparent,
                                1f to Color.Transparent
                            ),
                            radius = outerExtent
                        ),
                        radius = outerExtent
                    )
                }
        )
        Image(
            painter = painterResource(R.drawable.ic_qw_mark),
            contentDescription = stringResource(R.string.app_logo_content_description),
            modifier = Modifier.matchParentSize()
        )
        // Glossy sheen: a bright diagonal band, screen-blended over the artwork and clipped to the
        // badge's own circle so it reads as light glinting off the coin's face.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .drawBehind {
                    val diameter = this.size.minDimension
                    val bandWidth = diameter * 0.4f
                    // Travels from just off the top-left corner to just off the bottom-right one,
                    // in the rotated frame below, so the band fully clears the circle at both ends.
                    val travel = diameter * 1.8f
                    val bandCenter = -diameter * 0.4f + sheenProgress * travel
                    rotate(degrees = 25f, pivot = Offset(diameter / 2f, diameter / 2f)) {
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
                            topLeft = Offset(-diameter, -diameter),
                            size = Size(diameter * 3f, diameter * 3f),
                            blendMode = BlendMode.Screen
                        )
                    }
                }
        )
    }
}
