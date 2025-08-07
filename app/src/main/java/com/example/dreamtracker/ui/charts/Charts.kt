package com.example.dreamtracker.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun BarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 180.dp
) {
    val max = (data.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)
    val measurer = rememberTextMeasurer()
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val padding = 16.dp.toPx()
        val barSpace = 8.dp.toPx()
        val contentWidth = size.width - padding * 2
        val contentHeight = size.height - padding * 2
        val count = data.size.coerceAtLeast(1)
        val barWidth = (contentWidth - barSpace * (count - 1)) / count
        data.forEachIndexed { index, (label, value) ->
            val h = if (max == 0f) 0f else (value / max) * contentHeight
            val left = padding + index * (barWidth + barSpace)
            val top = size.height - padding - h
            drawRect(color = barColor, topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(barWidth, h))
            // label (truncated)
            val text = if (label.length > 5) label.take(5) + "…" else label
            drawLabel(measurer, text, Offset(left + barWidth / 2, size.height - padding / 2))
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    center: Offset
) {
    val layout = measurer.measure(buildAnnotatedString { append(text) }, style = MaterialTheme.typography.labelSmall.toSpanStyle())
    drawIntoCanvas {
        it.nativeCanvas.drawText(
            text,
            center.x - layout.size.width / 2f,
            center.y,
            android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 10.dp.toPx()
                textAlign = android.graphics.Paint.Align.LEFT
                isAntiAlias = true
            }
        )
    }
}

@Composable
fun PieChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
    ),
    height: Dp = 220.dp
) {
    val total = data.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)
    val measurer = rememberTextMeasurer()
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val radius = min(size.width, size.height) * 0.35f
        val center = Offset(size.width / 2, size.height / 2)
        var startAngle = -90f
        data.forEachIndexed { index, (label, value) ->
            val sweep = (value / total) * 360f
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            val mid = startAngle + sweep / 2f
            val lx = center.x + (radius + 12.dp.toPx()) * cos(mid * PI / 180).toFloat()
            val ly = center.y + (radius + 12.dp.toPx()) * sin(mid * PI / 180).toFloat()
            val txt = if (label.length > 8) label.take(8) + "…" else label
            drawLabel(measurer, txt, Offset(lx, ly))
            startAngle += sweep
        }
    }
}