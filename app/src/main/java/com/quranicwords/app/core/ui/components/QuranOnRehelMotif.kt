package com.quranicwords.app.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quranicwords.app.core.ui.motion.rememberReducedMotion

/**
 * The open-Quran-on-a-rehel mark, reproduced exactly from the project owner's own vector source
 * (`assets/image/quran.svg`) - [QURAN_ON_REHEL_PATH_DATA] is that file's single `<path d="...">`
 * copied verbatim (SVG path-data grammar is what [PathParser] parses, so no coordinate tracing or
 * hand-redrawing is involved; the path is the exact curve data Inkscape produced). Parsed once and
 * `remember`ed rather than a bitmap, so it stays in this app's "pure Path math, no shipped asset"
 * tradition ([GeometricPatternBackground]/[StreakFlame]) while matching the source file exactly -
 * fit-to-bounds at draw time so it fills [size] regardless of the source path's own coordinate
 * space. Silhouette only - no page lines or lettering, so this stays compliant with the app's
 * no-Ayat/Mushaf-as-decoration rule on its own.
 *
 * A single large instance: soft radial glow behind it, plus a slow, gentle vertical float - both
 * no-op under reduced motion (glow holds its midpoint alpha, float holds y=0) rather than
 * freezing mid-cycle. [color] defaults to the theme's tertiary role, so it tracks light/dark mode
 * automatically rather than a fixed brand hex. For the scattered-many-small-icons ambient
 * background treatment, see [QuranStarfieldMotif] instead, which shares this same traced path via
 * [QuranOnRehelPath].
 */
@Composable
fun QuranOnRehelMotif(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.tertiary,
    size: Dp = 220.dp,
    showGlow: Boolean = true
) {
    val reducedMotion = rememberReducedMotion()
    val infiniteTransition = rememberInfiniteTransition(label = "quranRehelFloat")
    val floatPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "quranRehelFloatPhase"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = if (reducedMotion) 0.14f else 0.26f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "quranRehelGlowAlpha"
    )

    val path = QuranOnRehelPath.path
    val pathBounds = QuranOnRehelPath.bounds

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (showGlow) {
            Box(
                modifier = Modifier
                    .size(size * 1.7f)
                    .drawBehind {
                        val radius = this.size.minDimension / 2f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(color.copy(alpha = glowAlpha), Color.Transparent),
                                radius = radius
                            ),
                            radius = radius
                        )
                    }
            )
        }
        Canvas(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    translationY = -floatPhase * 6.dp.toPx()
                }
        ) {
            val scale = minOf(this.size.width / pathBounds.width, this.size.height / pathBounds.height)
            val offsetX = (this.size.width - pathBounds.width * scale) / 2f - pathBounds.left * scale
            val offsetY = (this.size.height - pathBounds.height * scale) / 2f - pathBounds.top * scale
            withTransform({
                translate(left = offsetX, top = offsetY)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                drawPath(path = path, color = color)
            }
        }
    }
}

/**
 * The parsed path + its bounds, shared by [QuranOnRehelMotif] and [QuranStarfieldMotif] so the
 * ~3KB path-data string is parsed exactly once per process (`by lazy`, not per-composition
 * `remember`) rather than once per call site.
 */
internal object QuranOnRehelPath {
    val path: Path by lazy { PathParser().parsePathString(QURAN_ON_REHEL_PATH_DATA).toPath() }
    val bounds: Rect by lazy { path.getBounds() }
}

// Copied verbatim from assets/image/quran.svg's single <path d="..."> element - see the
// class-level doc comment for why this isn't hand-traced or bitmap-based.
private const val QURAN_ON_REHEL_PATH_DATA =
    "m114.24 412.06c-6.1524-3.8574-7.4249-7.2464-6.9695-18.562 0.30877-7.6726 0.89238-10.931 " +
    "2.5075-14 1.2413-2.3588 10.17-11.591 21.761-22.5 10.811-10.175 20.062-19.542 20.558-20.815 " +
    "0.4963-1.2733 0.90236-7.959 0.90236-14.857 0-10.018 0.26734-12.439 1.3285-12.032 0.7307 " +
    "0.2804 15.372 4.4628 32.536 9.2942l31.207 8.7844 1.8038 4.0628c2.0641 4.6492 8.8695 11.21 " +
    "12.599 12.146 1.3889 0.34858 2.5021 0.9225 2.4738 1.2754-0.0283 0.35287-24.785 15.873-" +
    "55.016 34.489-39.67 24.429-56.003 33.974-58.699 34.303-2.6353 0.32127-4.6938-0.14617-" +
    "6.9936-1.5881zm273.26 0.9949c-1.5467-0.63157-86.33-52.824-106.78-65.733l-3.9939-2.5213 " +
    "3.8869-1.7789c4.7548-2.1762 9.7884-7.331 11.749-12.032l1.4634-3.509 31.173-8.8051c17.145-" +
    "4.8428 31.809-9.0494 32.587-9.348 1.1733-0.45024 1.4147 1.5975 1.4147 11.999 0 6.898 " +
    "0.40606 13.584 0.90236 14.857 0.4963 1.2733 9.7474 10.64 20.558 20.815 11.635 10.95 " +
    "20.512 20.132 21.755 22.5 3.024 5.7631 3.6798 21.537 1.1008 26.48-3.2758 6.2783-10.192 " +
    "9.3728-15.816 7.0765zm-150.39-84.967c-0.76457-0.48417-2.5646-3.2898-4-6.2347s-3.9729-" +
    "6.2544-5.6389-7.3544-24.391-8.0322-50.5-15.405c-138.31-39.056-141.42-39.973-143.22-" +
    "42.174-0.9625-1.1784-1.75-2.9702-1.75-3.9819s10.356-35.274 23.014-76.139c19.256-62.166 " +
    "23.429-74.667 25.556-76.55 3.3035-2.9244 4.4532-2.8549 13.593 0.82194l7.663 3.0827-" +
    "1.3702 4.1727c-9.5899 29.204-34.456 112.33-34.456 115.18 0 6.558 2.191 11.741 6.9723 " +
    "16.493 3.887 3.8632 6.5087 5.2316 18.528 9.6704 33.597 12.408 61.213 18.254 94 19.898 " +
    "21.27 1.0667 35.433 6.0301 48.531 17.008 8.7244 7.3123 11.755 9.2531 16.459 10.539 " +
    "7.813 2.1362 15.877-0.36111 23.01-7.1262 9.7557-9.2517 23.999-16.728 36-18.898 3.3-" +
    "0.59646 13.875-1.5204 23.5-2.0532 26.619-1.4734 51.035-6.5394 80-16.599 15.349-5.3307 " +
    "23.518-9.1839 26.688-12.589 3.3343-3.5816 6.2656-10.86 6.2901-15.619 0.0174-3.3918-" +
    "24.224-85.008-34.426-115.9l-1.3778-4.1727 7.663-3.0827c9.1398-3.6768 10.295-3.7467 " +
    "13.575-0.82194 2.0988 1.8713 6.3998 14.758 25.556 76.572 12.668 40.877 23.032 75.14 " +
    "23.032 76.139 0 5.1181 0.24459 5.0361-98.429 33.006-52.211 14.8-96.086 27.745-97.5 " +
    "28.766s-3.7459 4.2467-5.1813 7.1663-3.2354 5.7045-4 6.1886-9.2651 0.8803-18.89 0.8803-" +
    "18.126-0.39613-18.89-0.8803zm6.2594-54.88c-13.083-11.748-32.715-18.762-55.243-19.737-" +
    "31.308-1.3562-58.145-6.8721-90.577-18.617-11.007-3.9861-15.549-7.1-15.549-10.661 0-" +
    "2.6486 35.93-118.43 37.493-120.82 2.4284-3.7103 5.6585-3.6707 15.193 0.18635 18.032 " +
    "7.2943 35.701 10.391 59.476 10.424 18.62 0.0255 29.54 2.4516 45.588 10.128l8.25 " +
    "3.9464v74.471c0 40.959-0.1125 74.453-0.25 74.43-0.1375-0.0226-2.1088-1.7101-4.3807-3.75z" +
    "m20.631-71.187v-73.978l9.25-4.4694c14.956-7.2265 21.867-8.6322 47.25-9.6112 28.144-" +
    "1.0855 38.321-2.9619 57.077-10.523 9.2671-3.736 12.532-3.7509 14.914-0.0682 1.5647 " +
    "2.4197 37.51 118.19 37.51 120.81 0 3.7474-4.5003 6.7014-17 11.159-32.273 11.509-" +
    "53.968 16.135-82.926 17.683-20.852 1.1148-31.279 3.1623-42.497 8.3457-7.5935 3.5084-" +
    "18.172 10.234-20.489 13.026-0.73269 0.88283-1.7271 1.6052-2.2097 1.6052-0.4955 0-" +
    "0.87756-32.207-0.87756-73.978z"
