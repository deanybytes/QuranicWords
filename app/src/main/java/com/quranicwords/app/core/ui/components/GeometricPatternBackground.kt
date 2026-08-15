package com.quranicwords.app.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ambient, tileable Islamic geometric lattice (8-pointed star motif) drawn purely from Path math -
 * no bundled asset required. Used as a low-opacity ambient background layer (Splash, celebration
 * moments), never as a literal manuscript/page graphic - see the redesign plan's guardrail against
 * rendering scripture as decoration. Purely static (no animation of its own), so it's safe under
 * reduced motion without a separate gate.
 */
@Composable
fun GeometricPatternBackground(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    alpha: Float = 0.06f,
    tileSize: Dp = 56.dp,
    strokeWidth: Dp = 1.dp
) {
    val effectiveColor = if (color.isSpecified) color else MaterialTheme.colorScheme.onBackground

    Canvas(modifier = modifier) {
        val tilePx = tileSize.toPx()
        val strokePx = strokeWidth.toPx()
        val cols = (size.width / tilePx).toInt() + 2
        val rows = (size.height / tilePx).toInt() + 2
        val starPath = eightPointStarPath(radius = tilePx * 0.42f)

        for (row in -1 until rows) {
            for (col in -1 until cols) {
                val rowOffset = if (row % 2 != 0) tilePx / 2f else 0f
                val cx = col * tilePx + rowOffset
                val cy = row * tilePx
                translate(left = cx, top = cy) {
                    drawPath(
                        path = starPath,
                        color = effectiveColor,
                        alpha = alpha,
                        style = Stroke(width = strokePx)
                    )
                }
            }
        }
    }
}

private fun eightPointStarPath(radius: Float): Path {
    val path = Path()
    val points = 8
    val innerRadius = radius * 0.55f
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
