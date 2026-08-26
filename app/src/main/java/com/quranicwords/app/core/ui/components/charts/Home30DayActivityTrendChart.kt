package com.quranicwords.app.core.ui.components.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranicwords.app.R
import com.quranicwords.app.core.ui.theme.BrandGold
import kotlin.math.max

/**
 * A vibrant, borderless 30-day activity trend & time-spent curve visualizer designed specifically
 * for the Home Hero Header. Combines smooth Bézier time-spent curves with a 30-day daily activity
 * bar matrix, interactive touch inspection, and motivational insights.
 */
@Composable
fun Home30DayActivityTrendChart(
    last30DaysMinutes: List<Int>,
    activeDaysCount: Int,
    totalMinutes: Int,
    modifier: Modifier = Modifier
) {
    val rawValues = if (last30DaysMinutes.isEmpty()) List(30) { 0 } else last30DaysMinutes.takeLast(30)
    val values = if (rawValues.size < 30) List(30 - rawValues.size) { 0 } + rawValues else rawValues

    val maxVal = max(values.maxOrNull() ?: 0, 15).toFloat()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val todayMinutes = values.lastOrNull() ?: 0
    val primaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = Color(0xFF2DD4BF) // Radiant Teal
    val textColor = MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Micro Header Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.home_chart_30d_title),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = textColor
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = primaryColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = stringResource(R.string.home_chart_30d_summary, activeDaysCount, totalMinutes),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Selected Day Tooltip / Indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selectedIndex != null) {
                val idx = selectedIndex!!
                val daysAgo = 29 - idx
                val dayLabel = if (daysAgo == 0) stringResource(R.string.home_chart_today) else "$daysAgo" + "d ago"
                val mins = values[idx]
                Text(
                    text = "• $dayLabel: $mins mins •",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = secondaryColor
                )
            } else {
                Text(
                    text = stringResource(R.string.home_chart_today_stat, todayMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.75f)
                )
            }
        }

        // Smooth Bézier Line & 30-Day Activity Bar Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                val step = size.width / 30f
                                val idx = (offset.x / step).toInt().coerceIn(0, 29)
                                selectedIndex = idx
                                tryAwaitRelease()
                                selectedIndex = null
                            }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height
                val stepX = width / 29f

                // 1. Draw 30 Daily Activity Indicator Bars at bottom
                val barWidth = (width / 30f) * 0.55f
                for (i in 0 until 30) {
                    val x = (i * (width / 30f)) + (width / 60f)
                    val mins = values[i]
                    val isToday = (i == 29)
                    val isSelected = (selectedIndex == i)

                    if (mins > 0) {
                        val barHeight = ((mins / maxVal) * 28.dp.toPx()).coerceIn(6.dp.toPx(), 32.dp.toPx())
                        val barBrush = Brush.verticalGradient(
                            listOf(
                                if (mins >= 15) BrandGold else secondaryColor,
                                primaryColor
                            )
                        )
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(x - barWidth / 2, height - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                        if (isToday) {
                            drawCircle(
                                color = BrandGold,
                                radius = 2.5.dp.toPx(),
                                center = Offset(x, height - barHeight - 4.dp.toPx())
                            )
                        }
                    } else {
                        drawRoundRect(
                            color = textColor.copy(alpha = if (isSelected) 0.4f else 0.15f),
                            topLeft = Offset(x - barWidth / 2, height - 4.dp.toPx()),
                            size = Size(barWidth, 4.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                        )
                    }
                }

                // 2. Draw Smooth Bézier Spline Curve for Minutes Spent
                val points = values.indices.map { i ->
                    val x = i * stepX
                    val y = (height - 32.dp.toPx()) - ((values[i] / maxVal) * (height - 40.dp.toPx()))
                    Offset(x, y.coerceIn(4.dp.toPx(), height - 30.dp.toPx()))
                }

                if (points.size >= 2) {
                    val linePath = Path()
                    val fillPath = Path()

                    linePath.moveTo(points[0].x, points[0].y)
                    fillPath.moveTo(points[0].x, height - 28.dp.toPx())
                    fillPath.lineTo(points[0].x, points[0].y)

                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val controlX1 = (p0.x + p1.x) / 2f
                        val controlY1 = p0.y
                        val controlX2 = (p0.x + p1.x) / 2f
                        val controlY2 = p1.y

                        linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                        fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                    }

                    fillPath.lineTo(points.last().x, height - 28.dp.toPx())
                    fillPath.close()

                    // Draw Area Gradient Fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                secondaryColor.copy(alpha = 0.28f),
                                primaryColor.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw Glowing Spline Stroke
                    drawPath(
                        path = linePath,
                        brush = Brush.horizontalGradient(
                            listOf(
                                secondaryColor.copy(alpha = 0.7f),
                                BrandGold,
                                secondaryColor
                            )
                        ),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Selected Point Marker
                    if (selectedIndex != null && selectedIndex!! in points.indices) {
                        val pt = points[selectedIndex!!]
                        drawCircle(
                            color = BrandGold,
                            radius = 4.5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }

        // Timeline axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.home_chart_30d_ago),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = textColor.copy(alpha = 0.6f)
            )
            Text(
                text = stringResource(R.string.home_chart_today),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = textColor.copy(alpha = 0.9f)
            )
        }

        // Motivational Insight Row
        val motivationalQuote = when {
            activeDaysCount >= 20 -> stringResource(R.string.home_chart_quote_high)
            activeDaysCount >= 8 -> stringResource(R.string.home_chart_quote_mid)
            else -> stringResource(R.string.home_chart_quote_start)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = BrandGold,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = motivationalQuote,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = textColor.copy(alpha = 0.88f)
            )
        }
    }
}
