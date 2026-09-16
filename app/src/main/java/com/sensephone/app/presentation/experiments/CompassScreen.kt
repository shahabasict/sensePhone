package com.sensephone.app.presentation.experiments

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sensephone.app.presentation.components.DotGridBackground
import com.sensephone.app.presentation.components.GlassCard
import com.sensephone.app.presentation.components.ScreenHeader
import com.sensephone.app.presentation.components.SectionTitle
import com.sensephone.app.presentation.components.SensorLifecycle
import com.sensephone.app.presentation.components.StatusPill
import com.sensephone.app.presentation.components.UnavailableMessage
import com.sensephone.app.presentation.components.fmt
import com.sensephone.app.presentation.theme.SenseColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CompassScreen(
    onBack: () -> Unit,
    viewModel: CompassViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SensorLifecycle(viewModel)

    Box(Modifier.fillMaxSize()) {
        DotGridBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            ScreenHeader(
                title = "Compass",
                subtitle = if (state.usingRotationVector) "Rotation Vector" else "Accelerometer + Magnetometer",
                onBack = onBack
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(available = state.available, liveText = "LIVE", unavailableText = "UNAVAILABLE")
                if (state.available) {
                    Text(
                        text = "Heading  ${state.direction}",
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = SenseColors.Green
                    )
                }
            }

            if (!state.available) {
                UnavailableMessage(
                    message = "Compass needs a magnetic-field sensor (or rotation-vector sensor),\nneither of which was found on this device."
                )
                return@Column
            }

            Box(Modifier.fillMaxWidth().height(360.dp), contentAlignment = Alignment.Center) {
                CompassRose(
                    headingDeg = state.headingDeg,
                    modifier = Modifier.size(300.dp)
                )
            }

            GlassCard(accent = SenseColors.Green, modifier = Modifier.fillMaxWidth()) {
                SectionTitle(text = "Heading", accent = SenseColors.Green)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${fmt(state.headingDeg, 1)}°  ${state.direction}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SenseColors.Green
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Point the top of the phone away from you and rotate slowly to read bearings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SenseColors.TextSecondary
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CompassRose(headingDeg: Float, modifier: Modifier = Modifier) {
    val drawTextColor = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 26f
        textAlign = android.graphics.Paint.Align.LEFT
        isFakeBoldText = true
    }
    val northPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(0xFF, 0x33, 0x55)
        textSize = 26f
        textAlign = android.graphics.Paint.Align.LEFT
        isFakeBoldText = true
    }
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f - 8f

        // Outer ring (fixed to the device).
        drawCircle(color = SenseColors.SurfaceRaised, radius = radius)
        drawCircle(color = SenseColors.Cyan.copy(alpha = 0.25f), radius = radius, style = Stroke(width = 2f))

        // The dial rotates so that N points toward magnetic north.
        rotate(degrees = -headingDeg, pivot = center) {
            // Inner ring + degree ticks.
            drawCircle(color = SenseColors.Grid, radius = radius * 0.86f, style = Stroke(width = 1f))
            for (deg in 0 until 360 step 5) {
                val major = deg % 30 == 0
                val radiusOuter = if (major) radius * 0.94f else radius * 0.975f
                val rad = Math.toRadians(deg.toDouble())
                val (cosA, sinA) = Pair(kotlin.math.cos(rad).toFloat(), kotlin.math.sin(rad).toFloat())
                drawLine(
                    color = if (major) SenseColors.Cyan.copy(alpha = 0.6f) else SenseColors.Grid,
                    start = Offset(center.x + cosA * radiusOuter, center.y + sinA * radiusOuter),
                    end = Offset(center.x + cosA * radius, center.y + sinA * radius),
                    strokeWidth = if (major) 2.5f else 1f
                )
            }

            // Cardinal letters drawn on the dial.
            val labelRadius = radius * 0.72f
            listOf("N" to northPaint, "E" to drawTextColor, "S" to drawTextColor, "W" to drawTextColor)
                .forEachIndexed { index, (label, paint) ->
                    val deg = index * 90.0
                    val rad = Math.toRadians(deg)
                    val (cosA, sinA) = Pair(kotlin.math.cos(rad).toFloat(), kotlin.math.sin(rad).toFloat())
                    if (index == 0) {
                        drawCircle(
                            color = SenseColors.Red,
                            radius = 4f,
                            center = Offset(center.x + cosA * labelRadius, center.y + sinA * labelRadius - 18f)
                        )
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        center.x + cosA * labelRadius - 6f,
                        center.y + sinA * labelRadius + 6f,
                        paint
                    )
                }
        }

        // Fixed lubber line at the top indicates the current heading.
        val lip = center.y - radius + 4f
        drawLine(
            color = SenseColors.TextPrimary,
            start = Offset(center.x, lip),
            end = Offset(center.x, center.y - radius * 0.82f),
            strokeWidth = 6f
        )

        drawCircle(color = SenseColors.Cyan, radius = 6f, center = center)
        drawCircle(color = SenseColors.Void, radius = 3f, center = center)
    }
}