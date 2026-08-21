package com.quranicwords.app.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp
import com.quranicwords.app.core.ui.motion.rememberReducedMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A scatter of small 8-point stars, pure Path math (no bundled asset) - an ambient companion to
 * [CrescentMoonMotif]. [seed] is fixed (not [kotlin.random.Random.Default]) so the same screen
 * renders an identical layout every time rather than a new random scatter per composition -
 * deterministic, not decorative noise. Gentle twinkle via alpha oscillation, no-op (static alpha)
 * under reduced motion.
 */
@Composable
fun StarfieldMotif(
    modifier: Modifier = Modifier,
    starCount: Int = 18,
    color: Color = Color.Unspecified,
    seed: Long = 42L
) {
    val effectiveColor = if (color.isSpecified) color else MaterialTheme.colorScheme.onBackground
    val reducedMotion = rememberReducedMotion()

    val infiniteTransition = rememberInfiniteTransition(label = "starfieldTwinkle")
    val twinkle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 2400), repeatMode = RepeatMode.Reverse),
        label = "twinkle"
    )

    Canvas(modifier = modifier) {
        val random = Random(seed)
        val minStarRadiusPx = 2.dp.toPx()
        val starRadiusSpanPx = 4.dp.toPx()
        repeat(starCount) { index ->
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            val starRadius = minStarRadiusPx + random.nextFloat() * starRadiusSpanPx
            val baseAlpha = 0.4f + random.nextFloat() * 0.6f
            val phase = (index % 5) * 0.2f
            val alpha = if (reducedMotion) baseAlpha else baseAlpha * (0.5f + 0.5f * sin((twinkle + phase) * PI.toFloat() * 2f))

            translate(left = x, top = y) {
                drawPath(path = smallStarPath(starRadius), color = effectiveColor, alpha = alpha.coerceIn(0f, 1f))
            }
        }
    }
}

private fun smallStarPath(radius: Float): Path {
    val path = Path()
    val points = 8
    val innerRadius = radius * 0.45f
    val totalPoints = points * 2
    for (i in 0 until totalPoints) {
        val angle = (PI * 2 * i / totalPoints - PI / 2).toFloat()
        val r = if (i % 2 == 0) radius else innerRadius
        val x = r * cos(angle)
        val y = r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
