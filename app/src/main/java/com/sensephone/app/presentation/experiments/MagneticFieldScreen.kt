package com.sensephone.app.presentation.experiments

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sensephone.app.presentation.components.AxisValue
import com.sensephone.app.presentation.components.DotGridBackground
import com.sensephone.app.presentation.components.GlassCard
import com.sensephone.app.presentation.components.ScreenHeader
import com.sensephone.app.presentation.components.SectionTitle
import com.sensephone.app.presentation.components.SensorLifecycle
import com.sensephone.app.presentation.components.StatusPill
import com.sensephone.app.presentation.components.UnavailableMessage
import com.sensephone.app.presentation.components.fmt
import com.sensephone.app.presentation.theme.SenseColors

@Composable
fun MagneticFieldScreen(
    onBack: () -> Unit,
    viewModel: MagneticFieldViewModel = viewModel()
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
            ScreenHeader(title = "Magnetic Field", subtitle = "Magnetometer · µT", onBack = onBack)
            Row {
                StatusPill(available = state.available, liveText = "LIVE", unavailableText = "UNAVAILABLE")
            }

            if (!state.available) {
                UnavailableMessage(
                    message = "The magnetic field sensor is not available on this device."
                )
                return@Column
            }

            // Intensity ring visual.
            val intensityColor by animateColorAsState(
                targetValue = when {
                    state.intensityFraction < 0.35f -> SenseColors.Green
                    state.intensityFraction < 0.6f -> SenseColors.Amber
                    else -> SenseColors.Red
                },
                label = "magColor",
                animationSpec = androidx.compose.animation.core.tween(120)
            )

            Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                MagneticFieldRing(
                    fraction = state.intensityFraction,
                    color = intensityColor,
                    modifier = Modifier.size(200.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = fmt(state.magnitude),
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = intensityColor
                    )
                    Text("µT", style = MaterialTheme.typography.labelMedium, color = SenseColors.TextSecondary)
                }
            }

            GlassCard(accent = SenseColors.Orange, modifier = Modifier.fillMaxWidth()) {
                SectionTitle(text = "Axis Values", accent = SenseColors.Orange)
                Spacer(Modifier.height(10.dp))
                AxisValue("X", "${fmt(state.x)} µT", valueColor = SenseColors.Cyan)
                Spacer(Modifier.height(6.dp))
                AxisValue("Y", "${fmt(state.y)} µT", valueColor = SenseColors.Green)
                Spacer(Modifier.height(6.dp))
                AxisValue("Z", "${fmt(state.z)} µT", valueColor = SenseColors.Amber)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Total  ${fmt(state.magnitude)} µT   ·   Peak observed  ${fmt(state.maxObserved)} µT",
                    style = MaterialTheme.typography.bodySmall,
                    color = SenseColors.TextSecondary
                )
            }

            GlassCard(accent = SenseColors.Cyan, modifier = Modifier.fillMaxWidth()) {
                SectionTitle(text = "Try This", accent = SenseColors.Cyan)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Slowly bring a magnet close to the phone. The ring pulses and the total magnitude jumps.\n\nEarth's local field is roughly 25–65 µT; a strong neodymium magnet can push it well past 200 µT.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SenseColors.TextSecondary
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MagneticFieldRing(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        label = "magRing",
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 200f)
    )
    Canvas(modifier = modifier) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f

        drawCircle(color = SenseColors.SurfaceRaised, radius = radius)
        drawCircle(
            color = color.copy(alpha = 0.15f),
            radius = radius - 10f,
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
        // Pulsing intensity ring.
        drawCircle(
            color = color.copy(alpha = 0.7f),
            radius = (radius - 10f) * (0.25f + 0.75f * animatedFraction.coerceIn(0f, 1f)),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f)
        )
        drawCircle(brush = Brush.radialGradient(listOf(color.copy(alpha = 0.5f), Color.Transparent)), radius = radius * 0.9f)
    }
}