package com.quranicwords.app.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.quranicwords.app.core.ui.motion.rememberReducedMotion
import com.quranicwords.app.core.ui.theme.StreakAccent
import kotlin.math.min

/**
 * Streak flame, drawn purely from Path math (no bundled asset, same approach as
 * [GeometricPatternBackground]) rather than depending on a licensed `.riv`/Lottie file that was
 * never sourced (see QW-21 - the real remaining Quran-font styles are a genuine, still-open
 * external-content-sourcing gap, but this flame doesn't need to be one). [streakDays] scales both
 * the flame's size and its flicker amplitude in fixed tiers, standing in for what a Rive
 * state-machine input would have driven live. No-ops the flicker animation under reduced motion,
 * settling on a static mid-flicker shape instead.
 */
@Composable
fun StreakFlame(
    streakDays: Int,
    modifier: Modifier = Modifier,
    color: Color = StreakAccent
) {
    val reducedMotion = rememberReducedMotion()
    val intensity = streakIntensity(streakDays)

    val infiniteTransition = rememberInfiniteTransition(label = "streakFlameFlicker")
    val flicker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650 - intensity.flickerSpeedBoostMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )
    val flickerAmount = if (reducedMotion) 0.5f else flicker

    Canvas(modifier = modifier.size(intensity.sizeDp.dp)) {
        val scaleY = 0.92f + 0.16f * flickerAmount * intensity.flickerAmplitude
        val swayDegrees = (flickerAmount - 0.5f) * 6f * intensity.flickerAmplitude
        scale(scaleX = 1f, scaleY = scaleY, pivot = center.copy(y = size.height)) {
            rotate(degrees = swayDegrees, pivot = center.copy(y = size.height)) {
                drawPath(path = flamePath(size.width, size.height), color = color)
                drawPath(
                    path = flamePath(size.width * 0.55f, size.height * 0.62f, offsetX = size.width * 0.225f, offsetY = size.height * 0.3f),
                    color = Color.White.copy(alpha = 0.35f)
                )
            }
        }
    }
}

private data class StreakIntensity(val sizeDp: Float, val flickerAmplitude: Float, val flickerSpeedBoostMs: Int)

private fun streakIntensity(streakDays: Int): StreakIntensity = when {
    streakDays >= 100 -> StreakIntensity(sizeDp = 32f, flickerAmplitude = 1f, flickerSpeedBoostMs = 250)
    streakDays >= 30 -> StreakIntensity(sizeDp = 28f, flickerAmplitude = 0.8f, flickerSpeedBoostMs = 150)
    streakDays >= 7 -> StreakIntensity(sizeDp = 25f, flickerAmplitude = 0.6f, flickerSpeedBoostMs = 80)
    else -> StreakIntensity(sizeDp = 22f, flickerAmplitude = 0.4f, flickerSpeedBoostMs = 0)
}

/** A simple teardrop/flame silhouette: rounded tip, wider flared base, via two cubic curves. */
private fun flamePath(width: Float, height: Float, offsetX: Float = 0f, offsetY: Float = 0f): Path {
    val w = min(width, height * 0.75f)
    return Path().apply {
        moveTo(offsetX + w * 0.5f, offsetY)
        cubicTo(
            offsetX + w * 0.95f, offsetY + height * 0.4f,
            offsetX + w * 0.85f, offsetY + height * 0.75f,
            offsetX + w * 0.5f, offsetY + height
        )
        cubicTo(
            offsetX + w * 0.15f, offsetY + height * 0.75f,
            offsetX + w * 0.05f, offsetY + height * 0.4f,
            offsetX + w * 0.5f, offsetY
        )
        close()
    }
}
