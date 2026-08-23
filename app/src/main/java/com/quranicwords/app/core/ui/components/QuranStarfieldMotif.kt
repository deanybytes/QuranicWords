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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp
import com.quranicwords.app.core.ui.motion.rememberReducedMotion
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * A scatter of small open-Quran-on-a-rehel marks, placed exactly like [StarfieldMotif]'s stars -
 * uniformly random across the *entire* draw area (not clustered to one region), sharing
 * [QuranOnRehelMotif]'s exact traced silhouette ([QuranOnRehelPath]) instead of an abstract
 * 8-point star. [seed] defaults to a value rolled once per call site via `remember` (not a fixed
 * constant, not [kotlin.random.Random.Default] re-rolled every frame) - genuinely different each
 * time this composable enters the tree (new screen visit, app relaunch), but stable for as long as
 * that instance stays on screen so the layout doesn't reshuffle mid-twinkle. Sizes are
 * squared-random (`random * random`) rather than uniform, so most icons land small with only a few
 * standing out larger - the same "mostly faint, a few bright" density a real night sky reads as,
 * instead of a field of same-sized dots. Each icon's twinkle phase is also fully random per-icon
 * (not `index % N`), so the pulses don't fall into a visible repeating rhythm across the field.
 *
 * Each icon is otherwise static (no float, no rotation) and only glows and unglows: a phase-offset
 * alpha pulse on the icon's own fill, nothing more - deliberately no separate glow-halo layer,
 * since a soft radial halo in the same color behind such a fine, small silhouette (thin legs,
 * thin connector) washes out those details and reads as a blurry blob rather than a crisp shape.
 * No-op under reduced motion (alpha holds its resting value) rather than freezing mid-pulse.
 */
@Composable
fun QuranStarfieldMotif(
    modifier: Modifier = Modifier,
    count: Int = 22,
    color: Color = Color.Unspecified,
    seed: Long = remember { Random.nextLong() }
) {
    val effectiveColor = if (color.isSpecified) color else MaterialTheme.colorScheme.tertiary
    val reducedMotion = rememberReducedMotion()

    val infiniteTransition = rememberInfiniteTransition(label = "quranStarfieldTwinkle")
    val twinkle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 2600), repeatMode = RepeatMode.Reverse),
        label = "twinkle"
    )

    val path = QuranOnRehelPath.path
    val bounds = QuranOnRehelPath.bounds
    val pivot = Offset(bounds.left + bounds.width / 2f, bounds.top + bounds.height / 2f)

    // A soft blur on each icon's edges - "shine like a star" rather than a crisp cutout - while
    // staying gentle enough (a couple dp against icons this small) that the book/stand silhouette
    // is still readable, not smeared into an unrecognizable blob.
    Canvas(modifier = modifier.blur(1.5.dp)) {
        val random = Random(seed)
        val minSizePx = 10.dp.toPx()
        val sizeSpanPx = 24.dp.toPx()
        repeat(count) { index ->
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            val iconSize = minSizePx + (random.nextFloat() * random.nextFloat()) * sizeSpanPx
            val baseAlpha = 0.4f + random.nextFloat() * 0.5f
            val phase = random.nextFloat()

            val alpha = if (reducedMotion) {
                baseAlpha
            } else {
                (baseAlpha * (0.45f + 0.55f * sin((twinkle + phase) * PI.toFloat() * 2f))).coerceIn(0f, 1f)
            }

            val scale = iconSize / maxOf(bounds.width, bounds.height)

            withTransform({
                translate(left = x, top = y)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
                translate(left = -pivot.x, top = -pivot.y)
            }) {
                drawPath(path = path, color = effectiveColor, alpha = alpha)
            }
        }
    }
}
