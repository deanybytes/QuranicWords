package com.quranicwords.app.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.isSpecified

/**
 * Mosque silhouette (footprint + domed drum + two flanking minarets, each capped with a tiny
 * crescent finial), a single filled shape - reads as a flat coin/badge engraving, not a literal
 * photographic building. Pure Path math, no bundled asset. Deliberately a silhouette only - no
 * windows, no doors, no figures of any kind, satisfying the no-human-faces constraint by
 * construction rather than by care.
 */
@Composable
fun MosqueSilhouetteMotif(modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val effectiveColor = if (color.isSpecified) color else MaterialTheme.colorScheme.onBackground

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val silhouette = Path()

        // Base footprint: a wide, low rounded rectangle across the bottom third.
        val baseTop = h * 0.62f
        val baseRect = Rect(offset = Offset(w * 0.08f, baseTop), size = androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.30f))
        silhouette.addRoundRect(androidx.compose.ui.geometry.RoundRect(baseRect, cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f)))

        // Central domed drum, sitting on top of the base.
        val drumWidth = w * 0.30f
        val drumLeft = (w - drumWidth) / 2f
        val drumTop = h * 0.30f
        val drum = Path().apply {
            moveTo(drumLeft, baseTop)
            lineTo(drumLeft, drumTop + drumWidth / 2f)
            arcTo(
                rect = Rect(offset = Offset(drumLeft, drumTop), size = androidx.compose.ui.geometry.Size(drumWidth, drumWidth)),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(drumLeft + drumWidth, baseTop)
            close()
        }
        silhouette.addPath(drum)

        // Two flanking minarets: thin trapezoids narrowing upward, each capped with a small cone.
        val minaretWidth = w * 0.06f
        val minaretTop = h * 0.10f
        listOf(w * 0.14f, w * 0.80f).forEach { centerX ->
            val minaret = Path().apply {
                moveTo(centerX - minaretWidth / 2f, baseTop)
                lineTo(centerX - minaretWidth * 0.35f, minaretTop + minaretWidth)
                lineTo(centerX, minaretTop)
                lineTo(centerX + minaretWidth * 0.35f, minaretTop + minaretWidth)
                lineTo(centerX + minaretWidth / 2f, baseTop)
                close()
            }
            silhouette.addPath(minaret)

            // Tiny crescent finial atop each minaret, same Path-difference technique as
            // CrescentMoonMotif at a small scale.
            val finialRadius = minaretWidth * 0.5f
            val finialCenter = Offset(centerX, minaretTop - finialRadius * 0.6f)
            val outer = Path().apply { addOval(Rect(center = finialCenter, radius = finialRadius)) }
            val inner = Path().apply {
                addOval(Rect(center = finialCenter.copy(x = finialCenter.x + finialRadius * 0.5f), radius = finialRadius))
            }
            val finial = Path().apply { op(outer, inner, PathOperation.Difference) }
            silhouette.addPath(finial)
        }

        drawPath(path = silhouette, color = effectiveColor)
    }
}
