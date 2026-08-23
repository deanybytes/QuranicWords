package com.quranicwords.app.core.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Pure Canvas-drawn progress ring - no charting library, matching this app's existing
 * zero-external-visual-dependency precedent (`GeometricPatternBackground`, the Islamic motifs).
 * [percent] is 0f-1f. [size]/[labelStyle] default to the Progress tab's original hero-sized
 * treatment; Home's header uses a smaller instance of the same component rather than a second
 * hand-rolled ring. */
@Composable
fun DonutChart(
    percent: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    centerLabel: String? = null,
    labelStyle: TextStyle = MaterialTheme.typography.titleLarge
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = this.size.minDimension * 0.16f
            val inset = strokeWidth / 2
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * percent.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            )
        }
        if (centerLabel != null) {
            Text(centerLabel, style = labelStyle)
        }
    }
}
