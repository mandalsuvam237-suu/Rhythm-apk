package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.CategoryUsageStat
import com.example.domain.DateTimeUtils
import com.example.domain.TimeBucketStat
import com.example.ui.theme.parseHexColor

@Composable
fun PerformanceScoreRing(
    score: Int,
    size: Dp = 74.dp,
    strokeWidth: Dp = 7.dp,
    modifier: Modifier = Modifier
) {
    val animatedSweep by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f) * 360f,
        animationSpec = tween(durationMillis = 650),
        label = "score_ring"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = when {
        score >= 80 -> MaterialTheme.colorScheme.secondary
        score >= 50 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = strokeWidth.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "SCORE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDonutChart(
    stats: List<CategoryUsageStat>,
    modifier: Modifier = Modifier
) {
    if (stats.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No activity time logged in this range yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedIndex by remember(stats) { mutableIntStateOf(0) }
    val safeIndex = selectedIndex.coerceIn(0, stats.lastIndex)
    val totalMinutes = stats.sumOf { maxOf(it.plannedMinutes, it.actualMinutes) }.coerceAtLeast(1)
    val selected = stats[safeIndex]
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(196.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val strokePx = 28.dp.toPx()
                val pad = strokePx / 2f + 6.dp.toPx()
                val arcSize = Size(this.size.width - pad * 2, this.size.height - pad * 2)
                val topLeft = Offset(pad, pad)

                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx)
                )

                var startAngle = -90f
                stats.forEachIndexed { idx, stat ->
                    val mins = maxOf(stat.plannedMinutes, stat.actualMinutes)
                    val sweep = (mins.toFloat() / totalMinutes.toFloat()) * 360f
                    val sliceColor = parseHexColor(stat.category.colorHex)
                    val width = if (idx == safeIndex) strokePx + 6.dp.toPx() else strokePx
                    drawArc(
                        color = sliceColor,
                        startAngle = startAngle,
                        sweepAngle = (sweep - 2f).coerceAtLeast(2f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = width, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = selected.category.name,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = DateTimeUtils.formatDuration(maxOf(selected.plannedMinutes, selected.actualMinutes)),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = parseHexColor(selected.category.colorHex)
                )
                Text(
                    text = "${selected.completedCount}/${selected.totalCount} done",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            stats.forEachIndexed { index, stat ->
                val sliceColor = parseHexColor(stat.category.colorHex)
                val isSelected = index == safeIndex
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isSelected) sliceColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { selectedIndex = index }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(sliceColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${stat.category.name} · ${DateTimeUtils.formatDuration(maxOf(stat.plannedMinutes, stat.actualMinutes))}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlannedVsActualBarChart(
    buckets: List<TimeBucketStat>,
    modifier: Modifier = Modifier
) {
    if (buckets.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Schedule activities to compare planned vs. actual focus time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxMins = buckets.maxOf { maxOf(it.plannedMinutes, it.actualMinutes) }.coerceAtLeast(60)
    val plannedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val actualColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = plannedColor, label = "Planned")
            LegendDot(color = actualColor, label = "Actual / Completed")
        }
        Spacer(modifier = Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val count = buckets.size.coerceAtLeast(1)
            val groupWidth = size.width / count
            val barWidth = (groupWidth * 0.28f).coerceIn(6.dp.toPx(), 18.dp.toPx())
            val chartHeight = size.height - 8.dp.toPx()

            buckets.forEachIndexed { idx, bucket ->
                val centerX = idx * groupWidth + groupWidth / 2f
                val plannedH = (bucket.plannedMinutes.toFloat() / maxMins) * chartHeight
                val actualH = (bucket.actualMinutes.toFloat() / maxMins) * chartHeight

                drawRoundRect(
                    color = plannedColor,
                    topLeft = Offset(centerX - barWidth - 2.dp.toPx(), chartHeight - plannedH),
                    size = Size(barWidth, plannedH.coerceAtLeast(3.dp.toPx())),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = actualColor,
                    topLeft = Offset(centerX + 2.dp.toPx(), chartHeight - actualH),
                    size = Size(barWidth, actualH.coerceAtLeast(3.dp.toPx())),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            buckets.forEach { bucket ->
                Text(
                    text = bucket.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

enum class WeeklyChartStyle(val label: String) {
    BAR("Bar Chart"),
    AREA("Trend Area")
}

@Composable
fun WeeklyCompletionChart(
    days: List<com.example.domain.WeeklyDayCompletionStat>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    chartStyle: WeeklyChartStyle,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No schedule activity found for these 7 days.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val safeIndex = selectedIndex.coerceIn(0, days.lastIndex)
    val selectedDay = days[safeIndex]

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = primaryColor, label = "Completion Rate (%)")
            LegendDot(color = secondaryColor, label = "80% Target")
        }

        // Interactive Tooltip Callout
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("weekly_chart_tooltip")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${selectedDay.dayLabel} · ${selectedDay.dateLabel}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedDay.totalActivities == 0) {
                            "No scheduled activities"
                        } else {
                            "${selectedDay.completedActivities} of ${selectedDay.totalActivities} completed · Score ${selectedDay.performanceScore}/100"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.85f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = when {
                        selectedDay.completionRatePercent >= 80 -> secondaryColor
                        selectedDay.completionRatePercent >= 50 -> primaryColor
                        else -> tertiaryColor
                    }
                ) {
                    Text(
                        text = "${selectedDay.completionRatePercent}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Chart Canvas + Y-Axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Y-axis ticks
            Column(
                modifier = Modifier
                    .height(180.dp)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                listOf("100%", "75%", "50%", "25%", "0%").forEach { tick ->
                    Text(
                        text = tick,
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.TopCenter)
                ) {
                    val count = days.size.coerceAtLeast(1)
                    val topPad = 10.dp.toPx()
                    val bottomPad = 6.dp.toPx()
                    val plotHeight = (size.height - topPad - bottomPad).coerceAtLeast(20f)
                    val dashEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)

                    // Horizontal Cartesian grid lines at 0, 25, 50, 75, 100
                    for (step in 0..4) {
                        val y = topPad + (step / 4f) * plotHeight
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (step == 4) null else dashEffect
                        )
                    }

                    // 80% target reference line
                    val goalY = topPad + (1f - 0.80f) * plotHeight
                    drawLine(
                        color = secondaryColor.copy(alpha = 0.75f),
                        start = Offset(0f, goalY),
                        end = Offset(size.width, goalY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = dashEffect
                    )

                    val slotWidth = size.width / count

                    if (chartStyle == WeeklyChartStyle.BAR) {
                        val barWidth = (slotWidth * 0.46f).coerceIn(14.dp.toPx(), 32.dp.toPx())
                        days.forEachIndexed { idx, stat ->
                            val isSelected = idx == safeIndex
                            val centerX = idx * slotWidth + slotWidth / 2f
                            val barLeft = centerX - barWidth / 2f

                            // Full-height background track
                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset(barLeft, topPad),
                                size = Size(barWidth, plotHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )

                            val frac = (stat.completionRatePercent / 100f).coerceIn(0f, 1f)
                            val barHeight = (frac * plotHeight).coerceAtLeast(
                                if (stat.totalActivities > 0) 6.dp.toPx() else 0f
                            )
                            if (barHeight > 0f) {
                                val topY = topPad + plotHeight - barHeight
                                val barColor = when {
                                    stat.completionRatePercent >= 80 -> secondaryColor
                                    stat.completionRatePercent >= 50 -> primaryColor
                                    else -> tertiaryColor
                                }
                                drawRoundRect(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(barColor, barColor.copy(alpha = 0.72f)),
                                        startY = topY,
                                        endY = topPad + plotHeight
                                    ),
                                    topLeft = Offset(barLeft, topY),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )
                            }

                            if (isSelected) {
                                drawRoundRect(
                                    color = primaryColor,
                                    topLeft = Offset(barLeft - 2.dp.toPx(), topPad - 2.dp.toPx()),
                                    size = Size(barWidth + 4.dp.toPx(), plotHeight + 4.dp.toPx()),
                                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    } else {
                        // AREA trend line chart (Recharts AreaChart aesthetic)
                        val points = days.mapIndexed { idx, stat ->
                            val x = idx * slotWidth + slotWidth / 2f
                            val frac = (stat.completionRatePercent / 100f).coerceIn(0f, 1f)
                            val y = topPad + (1f - frac) * plotHeight
                            Offset(x, y)
                        }

                        if (points.size >= 2) {
                            val linePath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(points.first().x, points.first().y)
                                for (i in 0 until points.lastIndex) {
                                    val p0 = points[i]
                                    val p1 = points[i + 1]
                                    val midX = (p0.x + p1.x) / 2f
                                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                }
                            }
                            val areaPath = androidx.compose.ui.graphics.Path().apply {
                                addPath(linePath)
                                lineTo(points.last().x, topPad + plotHeight)
                                lineTo(points.first().x, topPad + plotHeight)
                                close()
                            }

                            drawPath(
                                path = areaPath,
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.34f),
                                        primaryColor.copy(alpha = 0.02f)
                                    ),
                                    startY = topPad,
                                    endY = topPad + plotHeight
                                )
                            )
                            drawPath(
                                path = linePath,
                                color = primaryColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Vertical guide line for selected point
                        val selPt = points[safeIndex]
                        drawLine(
                            color = primaryColor.copy(alpha = 0.45f),
                            start = Offset(selPt.x, topPad),
                            end = Offset(selPt.x, topPad + plotHeight),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = dashEffect
                        )

                        // Data dots
                        points.forEachIndexed { idx, pt ->
                            val isSelected = idx == safeIndex
                            if (isSelected) {
                                drawCircle(
                                    color = primaryColor.copy(alpha = 0.22f),
                                    radius = 10.dp.toPx(),
                                    center = pt
                                )
                            }
                            drawCircle(
                                color = surfaceColor,
                                radius = if (isSelected) 6.dp.toPx() else 4.5.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = if (isSelected) secondaryColor else primaryColor,
                                radius = if (isSelected) 4.dp.toPx() else 3.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }

                // Tappable day columns aligned over each bar/point + X-axis day labels
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    days.forEachIndexed { idx, stat ->
                        val isSelected = idx == safeIndex
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectIndex(idx) }
                                .testTag("weekly_chart_day_$idx"),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stat.dayLabel.take(3),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) primaryColor else onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

