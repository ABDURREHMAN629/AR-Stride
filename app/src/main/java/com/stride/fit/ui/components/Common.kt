package com.stride.fit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.stride.fit.ui.theme.Space
import kotlin.math.max
import kotlin.math.min

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg, vertical = Space.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = if (onTrailingClick != null)
                    Modifier.clickable { onTrailingClick() } else Modifier
            )
        }
    }
}

@Composable
fun StrideCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val base = modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    Card(
        modifier = base,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(Space.lg)) { content() }
    }
}

/** A single number with its label and unit. The workhorse of every summary row. */
@Composable
fun MetricTile(
    label: String,
    value: String,
    unit: String = "",
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = Space.xs)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = accent
            )
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun Pill(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

/** The step ring. Past 100 % it draws a second, brighter lap rather than clamping. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 16.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    overflowColor: Color = MaterialTheme.colorScheme.tertiary,
    content: @Composable () -> Unit = {}
) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(700),
        label = "ring"
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * min(animated, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (animated > 1f) {
                drawArc(
                    color = overflowColor,
                    startAngle = -90f,
                    sweepAngle = 360f * min(animated - 1f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke * 0.5f, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

/** Hourly or weekly bars with an optional goal line. */
@Composable
fun BarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList(),
    color: Color = MaterialTheme.colorScheme.primary,
    goal: Float? = null,
    height: Dp = 120.dp
) {
    val maxValue = max(values.maxOrNull() ?: 0f, goal ?: 0f).coerceAtLeast(1f)
    val outline = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            if (values.isEmpty()) return@Canvas
            val gap = size.width * 0.012f
            val barWidth = (size.width - gap * (values.size - 1)) / values.size
            val radius = min(barWidth / 2.2f, 6.dp.toPx())

            goal?.let {
                val y = size.height * (1f - it / maxValue)
                drawLine(
                    color = outline,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            values.forEachIndexed { index, value ->
                val fraction = (value / maxValue).coerceIn(0f, 1f)
                val barHeight = size.height * fraction
                if (barHeight <= 0f) return@forEachIndexed
                drawRoundRect(
                    color = color,
                    topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
            }
        }
        if (labels.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(top = Space.xs)) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Trend line, used for bodyweight. Sparse data is drawn as points, not invented. */
@Composable
fun LineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 140.dp,
    fillBelow: Boolean = true
) {
    val fillColor = color.copy(alpha = 0.16f)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (values.size < 2) return@Canvas
        val minV = values.min()
        val maxV = values.max()
        val span = (maxV - minV).takeIf { it > 0.0001f } ?: 1f
        val stepX = size.width / (values.size - 1)

        fun pointAt(index: Int): Offset {
            val v = values[index]
            val y = size.height * (1f - (v - minV) / span)
            return Offset(index * stepX, y.coerceIn(0f, size.height))
        }

        val line = Path().apply {
            moveTo(pointAt(0).x, pointAt(0).y)
            for (i in 1 until values.size) {
                val p = pointAt(i)
                lineTo(p.x, p.y)
            }
        }

        if (fillBelow) {
            val area = Path().apply {
                addPath(line)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(area, fillColor)
        }
        drawPath(line, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

        val last = pointAt(values.size - 1)
        drawCircle(color, radius = 4.dp.toPx(), center = last)
    }
}

/**
 * The recorded route drawn straight from the GPS coordinates, for when no offline tiles
 * are available. Longitude is scaled by the cosine of the latitude so the shape of the
 * route stays true rather than stretching east to west.
 */
@Composable
fun RoutePreview(
    points: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    height: Dp = 180.dp
) {
    val startColor = MaterialTheme.colorScheme.primary
    val endColor = MaterialTheme.colorScheme.tertiary
    val emptyColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (points.size < 2) {
            drawCircle(
                color = emptyColor.copy(alpha = 0.25f),
                radius = 6.dp.toPx(),
                center = Offset(size.width / 2, size.height / 2)
            )
            return@Canvas
        }

        val latitudes = points.map { it.first }
        val longitudes = points.map { it.second }
        val midLat = (latitudes.min() + latitudes.max()) / 2.0
        val lonScale = kotlin.math.cos(Math.toRadians(midLat)).coerceAtLeast(0.05)

        val xs = longitudes.map { it * lonScale }
        val minX = xs.min()
        val maxX = xs.max()
        val minY = latitudes.min()
        val maxY = latitudes.max()

        val spanX = (maxX - minX).takeIf { it > 1e-9 } ?: 1e-9
        val spanY = (maxY - minY).takeIf { it > 1e-9 } ?: 1e-9

        val padding = 12.dp.toPx()
        val usableW = size.width - padding * 2
        val usableH = size.height - padding * 2
        // One scale for both axes preserves the true shape of the route.
        val scale = min(usableW / spanX, usableH / spanY)
        val offsetX = padding + (usableW - spanX * scale) / 2
        val offsetY = padding + (usableH - spanY * scale) / 2

        fun project(index: Int): Offset {
            val px = offsetX + (xs[index] - minX) * scale
            val py = offsetY + (maxY - latitudes[index]) * scale
            return Offset(px.toFloat(), py.toFloat())
        }

        val path = Path().apply {
            val first = project(0)
            moveTo(first.x, first.y)
            for (i in 1 until points.size) {
                val p = project(i)
                lineTo(p.x, p.y)
            }
        }
        drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(startColor, radius = 5.dp.toPx(), center = project(0))
        drawCircle(endColor, radius = 5.dp.toPx(), center = project(points.size - 1))
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Space.sm))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Horizontal progress bar used for goal rows. */
@Composable
fun ThinProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "bar"
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}
