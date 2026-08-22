package com.quranicwords.app.core.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Pure Canvas-drawn calendar heatmap - [practicedDays] is oldest-first, one entry per day,
 * laid out in a 7-column grid (one column per weekday) so complete weeks form visually even
 * rows, the same shape GitHub-style practice-streak heatmaps use. */
@Composable
fun HeatmapChart(
    practicedDays: List<Boolean>,
    modifier: Modifier = Modifier,
    filledColor: Color = MaterialTheme.colorScheme.primary,
    emptyColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val columns = 7
    Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
        if (practicedDays.isEmpty()) return@Canvas
        val rows = (practicedDays.size + columns - 1) / columns
        val cellWidth = size.width / columns
        val cellHeight = size.height / rows
        val cellSize = minOf(cellWidth, cellHeight) * 0.78f
        practicedDays.forEachIndexed { index, practiced ->
            val column = index % columns
            val row = index / columns
            val cx = column * cellWidth + (cellWidth - cellSize) / 2f
            val cy = row * cellHeight + (cellHeight - cellSize) / 2f
            drawRoundRect(
                color = if (practiced) filledColor else emptyColor,
                topLeft = Offset(cx, cy),
                size = Size(cellSize, cellSize),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}
