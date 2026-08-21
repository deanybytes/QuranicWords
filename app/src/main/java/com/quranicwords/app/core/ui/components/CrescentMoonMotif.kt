package com.quranicwords.app.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.isSpecified

/**
 * Crescent moon, drawn purely from two overlapping circles combined via [Path.op] (no bundled
 * asset, same discipline as [GeometricPatternBackground]/[StreakFlame]). Safe under both hard
 * constraints - it's a boolean shape operation on two circles, no human face is suggested by the
 * crescent's negative space, and nothing here renders text of any kind.
 */
@Composable
fun CrescentMoonMotif(modifier: Modifier = Modifier, color: Color = Color.Unspecified, biteFraction: Float = 0.38f) {
    val effectiveColor = if (color.isSpecified) color else MaterialTheme.colorScheme.onBackground

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = this.center

        val outer = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(center = center, radius = radius))
        }
        val innerCenter = center.copy(x = center.x + radius * biteFraction)
        val inner = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(center = innerCenter, radius = radius))
        }

        val crescent = Path()
        crescent.op(outer, inner, PathOperation.Difference)
        drawPath(path = crescent, color = effectiveColor)
    }
}
