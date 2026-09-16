package com.sensephone.app.presentation.experiments

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun BubbleLevelScreen(
    onBack: () -> Unit,
    viewModel: BubbleLevelViewModel = viewModel()
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
            ScreenHeader(title = "Bubble Level", subtitle = "Accelerometer", onBack = onBack)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusPill(
                    available = state.available,
                    liveText = "LIVE",
                    unavailableText = "UNAVAILABLE"
                )
                if (state.available) {
                    LevelStatusPill(isLevel = state.isLevel)
                }
            }

            if (!state.available) {
                UnavailableMessage(
                    message = "Bubble Level needs the accelerometer,\nwhich is missing on this device."
                )
                return@Column
            }

            Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                BubbleLevelView(
                    roll = state.roll,
                    pitch = state.pitch,
                    isLevel = state.isLevel,
                    tolerance = state.tolerance.toleranceDeg,
                    modifier = Modifier.size(280.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LevelTolerance.entries.forEach { mode ->
                    FilterChip(
                        selected = state.tolerance == mode,
                        onClick = { viewModel.setTolerance(mode) },
                        label = {
                            Text(mode.label, fontSize = 12.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SenseColors.Cyan.copy(alpha = 0.2f),
                            selectedLabelColor = SenseColors.Cyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            GlassCard(accent = SenseColors.Cyan, modifier = Modifier.fillMaxWidth()) {
                SectionTitle(text = "Tilt Readings", accent = SenseColors.Cyan)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TiltReadout("ROLL", state.roll, "Left ↔ Right")
                    TiltReadout("PITCH", state.pitch, "Back ↔ Front")
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Place the phone flat on a surface and let the bubble rest in the centre ring. Tolerance: ±${fmt(state.tolerance.toleranceDeg, 1)}°",
                    style = MaterialTheme.typography.bodySmall,
                    color = SenseColors.TextSecondary
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LevelStatusPill(isLevel: Boolean) {
    val color = if (isLevel) SenseColors.Green else SenseColors.Amber
    androidx.compose.material3.Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Text(
            text = if (isLevel) "LEVEL" else "NOT LEVEL",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
private fun TiltReadout(label: String, value: Float, rangeHint: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = SenseColors.TextMuted)
        Text(
            text = "${fmt(value, 1)}°",
            style = MaterialTheme.typography.headlineLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = SenseColors.Cyan
        )
        Text(rangeHint, style = MaterialTheme.typography.bodySmall, color = SenseColors.TextMuted)
    }
}

/**
 * Circular bubble visualization.
 *
 * roll > 0 (right edge lower) moves the bubble right; pitch > 0 (top lower)
 * moves the bubble up.  The bubble is clamped inside the ring and its colour
 * shifts from amber (close) to green (dead centre).
 */
@Composable
private fun BubbleLevelView(
    roll: Float,
    pitch: Float,
    isLevel: Boolean,
    tolerance: Float,
    modifier: Modifier = Modifier
) {
    val ringColor by animateColorAsState(
        targetValue = when {
            isLevel -> SenseColors.Green
            kotlin.math.abs(roll) < tolerance * 2 && kotlin.math.abs(pitch) < tolerance * 2 -> SenseColors.Amber
            else -> SenseColors.Red
        },
        label = "ringColor",
        animationSpec = androidx.compose.animation.core.tween(150)
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f - 8f

        // Outer glow ring.
        drawCircle(color = ringColor.copy(alpha = 0.08f), radius = radius)
        drawCircle(color = ringColor.copy(alpha = 0.5f), radius = radius * 0.8f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
        drawCircle(color = SenseColors.Grid, radius = radius * 0.6f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
        drawCircle(color = SenseColors.Grid, radius = radius * 0.4f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
        drawCircle(color = SenseColors.Grid, radius = radius * 0.2f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))

        // Crosshair ticks.
        drawLine(color = SenseColors.Grid, start = Offset(center.x - radius, center.y), end = Offset(center.x + radius, center.y), strokeWidth = 1f)
        drawLine(color = SenseColors.Grid, start = Offset(center.x, center.y - radius), end = Offset(center.x, center.y + radius), strokeWidth = 1f)

        // Dead-centre target.
        drawCircle(color = SenseColors.Green.copy(alpha = 0.35f), radius = 10f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))

        // Bubble position from tilt.
        val maxOffset = radius * 0.6f
        val bubbleX = center.x + (roll / 45f).coerceIn(-1f, 1f) * maxOffset
        val bubbleY = center.y - (pitch / 45f).coerceIn(-1f, 1f) * maxOffset

        val distance = kotlin.math.sqrt((bubbleX - center.x) * (bubbleX - center.x) + (bubbleY - center.y) * (bubbleY - center.y))
        val clamped = if (distance > maxOffset) {
            val angle = atan2(bubbleY - center.y, bubbleX - center.x)
            Offset(center.x + cos(angle) * maxOffset, center.y + sin(angle) * maxOffset)
        } else {
            Offset(bubbleX, bubbleY)
        }

        val bubbleRadius = 26f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.95f), ringColor.copy(alpha = 0.55f), ringColor.copy(alpha = 0.1f)),
                center = Offset(clamped.x - bubbleRadius * 0.35f, clamped.y - bubbleRadius * 0.35f),
                radius = bubbleRadius
            ),
            radius = bubbleRadius,
            center = clamped
        )
        drawCircle(color = ringColor.copy(alpha = 0.8f), radius = bubbleRadius, center = clamped, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
    }
}