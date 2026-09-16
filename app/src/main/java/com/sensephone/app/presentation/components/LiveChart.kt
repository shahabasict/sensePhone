package com.sensephone.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sensephone.app.presentation.theme.SenseColors

/**
 * Real-time multi-series line chart rendered with a single [Canvas].
 *
 * Each entry of [seriesData] is one channel (e.g. X, Y, Z).  Values are scaled
 * to the window's min/max with padding; the chart never stores external state
 * and stays cheap to recompose (one draw per frame).
 */
@Composable
fun LiveChart(
    seriesData: List<List<Float>>,
    seriesColors: List<Color>,
    modifier: Modifier = Modifier,
    gridColor: Color = SenseColors.Grid,
    lineWidth: Float = 2f,
    showGrid: Boolean = true
) {
    Canvas(
        modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        if (seriesData.isEmpty()) return@Canvas

        val numSamples = seriesData.maxOfOrNull { it.size } ?: 0
        if (numSamples < 2) return@Canvas

        // Dynamic y-range across all channels.
        var minV = Float.MAX_VALUE
        var maxV = -Float.MAX_VALUE
        seriesData.forEach { series ->
            series.forEach { v ->
                if (v < minV) minV = v
                if (v > maxV) maxV = v
            }
        }
        if (minV > maxV) { minV = 0f; maxV = 1f }
        if (minV == maxV) { minV -= 1f; maxV += 1f }
        val range = maxV - minV
        val pad = range * 0.1f
        minV -= pad
        maxV += pad
        val effectiveRange = (maxV - minV).coerceAtLeast(1e-6f)

        val w = size.width
        val h = size.height

        // Horizontal grid lines + labels.
        if (showGrid) {
            val gridLines = 3
            for (i in 0..gridLines) {
                val t = i / gridLines.toFloat()
                val y = h * t
                drawLine(
                    color = gridColor.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }
        }

        val xStep = if (numSamples > 1) w / (numSamples - 1) else w

        seriesData.forEachIndexed { index, series ->
            if (series.size < 2) return@forEachIndexed
            val color = seriesColors.getOrElse(index) { SenseColors.Cyan }
            val path = Path()
            val startOffset = numSamples - series.size
            series.forEachIndexed { i, v ->
                val x = (startOffset + i) * xStep
                val y = h - ((v - minV) / effectiveRange) * h
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = lineWidth, cap = StrokeCap.Round)
            )
            // Endpoint dot
            val lastX = (numSamples - 1) * xStep
            val lastY = h - ((series.last() - minV) / effectiveRange) * h
            drawCircle(color = color, radius = 3f, center = Offset(lastX, lastY))
        }
    }
}