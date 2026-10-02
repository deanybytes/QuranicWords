package com.quranicwords.app.core.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R

/** Pure Canvas-drawn bar chart - one bar per (value, label) pair, e.g. lessons completed per day
 * over the last 7 days. Bars at value 0 still draw a faint sliver (not nothing) so an empty day
 * reads as "zero," not as a missing/broken bar. */
@Composable
fun BarChart(
    values: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    /** Spoken names for each bar (e.g. full weekday names where [labels] are narrow letters). */
    accessibilityLabels: List<String> = labels,
    valueFormatter: (Int) -> String = { it.toString() }
) {
    val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    // A Canvas is invisible to screen readers - announce the data as one "label: value" list.
    val summary = values.indices.map { i ->
        stringResource(R.string.a11y_chart_item, accessibilityLabels.getOrElse(i) { "" }, valueFormatter(values[i]))
    }.joinToString(", ")
    Column(modifier = modifier.semantics(mergeDescendants = true) { contentDescription = summary }) {
        Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
            if (values.isEmpty()) return@Canvas
            val slotWidth = size.width / values.size
            val barWidth = slotWidth * 0.5f
            values.forEachIndexed { index, value ->
                val fraction = value.toFloat() / maxValue
                val barHeight = (size.height * fraction).coerceAtLeast(4f)
                val x = slotWidth * index + (slotWidth - barWidth) / 2f
                drawRoundRect(
                    color = if (value > 0) barColor else barColor.copy(alpha = 0.18f),
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            labels.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
