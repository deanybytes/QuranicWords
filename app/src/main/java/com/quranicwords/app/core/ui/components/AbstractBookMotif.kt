package com.quranicwords.app.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A closed-book silhouette: two angled trapezoidal covers meeting at a center spine, a small
 * embossed geometric mark on the spine, and a few short curved lines along the outer edge
 * suggesting a stack of closed pages. Pure Path math, no bundled asset.
 *
 * Deliberately abstract - a closed-book silhouette with a page-edge suggestion and an embossed
 * geometric spine mark, never literal Ayat/Mushaf text used as a visual skin. No letterforms, no
 * Arabic shaping, no glyphs of any kind are drawn here. If a future change to this file adds any
 * text/glyph rendering, that is exactly the guardrail this doc comment exists to stop - don't.
 */
@Composable
fun AbstractBookMotif(modifier: Modifier = Modifier, color: Color = Color.Unspecified, spineAccent: Color = Color.Unspecified) {
    val effectiveColor = if (color.isSpecified) color else MaterialTheme.colorScheme.onBackground
    val effectiveSpineAccent = if (spineAccent.isSpecified) spineAccent else MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val spineWidth = w * 0.06f

        // Left cover: a quadrilateral whose right edge slants slightly toward center-top,
        // suggesting a book viewed at a slight angle rather than a flat rectangle.
        val leftCover = Path().apply {
            moveTo(w * 0.08f, h * 0.12f)
            lineTo(centerX - spineWidth / 2f, h * 0.08f)
            lineTo(centerX - spineWidth / 2f, h * 0.92f)
            lineTo(w * 0.08f, h * 0.88f)
            close()
        }
        val rightCover = Path().apply {
            moveTo(centerX + spineWidth / 2f, h * 0.08f)
            lineTo(w * 0.92f, h * 0.12f)
            lineTo(w * 0.92f, h * 0.88f)
            lineTo(centerX + spineWidth / 2f, h * 0.92f)
            close()
        }
        drawPath(path = leftCover, color = effectiveColor)
        drawPath(path = rightCover, color = effectiveColor)

        // Spine, with a tiny embossed diamond/4-point star mark at its center.
        val spine = Path().apply {
            moveTo(centerX - spineWidth / 2f, h * 0.08f)
            lineTo(centerX + spineWidth / 2f, h * 0.08f)
            lineTo(centerX + spineWidth / 2f, h * 0.92f)
            lineTo(centerX - spineWidth / 2f, h * 0.92f)
            close()
        }
        drawPath(path = spine, color = effectiveSpineAccent)
        drawPath(path = tinyStarPath(Offset(centerX, h * 0.5f), spineWidth * 0.35f), color = effectiveColor)

        // A few short parallel curves along the right (outer) edge, suggesting closed page edges.
        val pageStroke = Stroke(width = w * 0.01f)
        for (i in 0 until 4) {
            val t = 0.2f + i * 0.2f
            val yTop = h * (0.12f + t * 0.1f)
            val yBottom = h * (0.88f - t * 0.1f)
            val xEdge = w * (0.90f - i * 0.01f)
            val pageCurve = Path().apply {
                moveTo(xEdge, yTop)
                quadraticTo(xEdge + w * 0.02f, h * 0.5f, xEdge, yBottom)
            }
            drawPath(path = pageCurve, color = effectiveColor.copy(alpha = 0.5f), style = pageStroke)
        }
    }
}

private fun tinyStarPath(center: Offset, radius: Float): Path {
    val path = Path()
    val points = 4
    val innerRadius = radius * 0.45f
    val totalPoints = points * 2
    for (i in 0 until totalPoints) {
        val angle = (PI * 2 * i / totalPoints - PI / 2).toFloat()
        val r = if (i % 2 == 0) radius else innerRadius
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
