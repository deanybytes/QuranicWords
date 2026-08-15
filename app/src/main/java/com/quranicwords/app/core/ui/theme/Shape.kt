package com.quranicwords.app.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val QwShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Scalloped "medallion" outline (an N-petal ring pulled inward by a quadratic curve through an
 * inner radius) evoking an Islamic geometric medallion, drawn purely from Path math - no bundled
 * asset required. Reserved for hero/celebratory containers only (streak flame backdrop,
 * lesson-complete badge) - not a general-purpose shape token.
 */
class MedallionShape(
    private val petals: Int = 12,
    private val scallopDepth: Float = 0.12f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val outerRadius = minOf(centerX, centerY)
        val innerRadius = outerRadius * (1f - scallopDepth)
        val step = (2 * PI / petals).toFloat()
        val startOffset = -(PI / 2).toFloat()

        for (i in 0 until petals) {
            val outerAngle = i * step + startOffset
            val nextOuterAngle = (i + 1) * step + startOffset
            val controlAngle = outerAngle + step / 2f

            val outerX = centerX + outerRadius * cos(outerAngle)
            val outerY = centerY + outerRadius * sin(outerAngle)
            val controlX = centerX + innerRadius * cos(controlAngle)
            val controlY = centerY + innerRadius * sin(controlAngle)
            val nextOuterX = centerX + outerRadius * cos(nextOuterAngle)
            val nextOuterY = centerY + outerRadius * sin(nextOuterAngle)

            if (i == 0) path.moveTo(outerX, outerY)
            path.quadraticTo(controlX, controlY, nextOuterX, nextOuterY)
        }
        path.close()
        return Outline.Generic(path)
    }
}

val MedallionShapeDefault = MedallionShape()
