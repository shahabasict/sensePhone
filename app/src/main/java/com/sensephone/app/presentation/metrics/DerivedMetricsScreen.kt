package com.sensephone.app.presentation.metrics

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sensephone.app.domain.metrics.MetricsSnapshot
import com.sensephone.app.presentation.components.DotGridBackground
import com.sensephone.app.presentation.components.GlassCard
import com.sensephone.app.presentation.components.MetricBar
import com.sensephone.app.presentation.components.ScreenHeader
import com.sensephone.app.presentation.components.SectionTitle
import com.sensephone.app.presentation.components.SensorLifecycle
import com.sensephone.app.presentation.components.StatusPill
import com.sensephone.app.presentation.components.UnavailableMessage
import com.sensephone.app.presentation.theme.SenseColors
import kotlin.math.roundToInt

@Composable
fun DerivedMetricsScreen(
    onBack: () -> Unit,
    viewModel: DerivedMetricsViewModel = viewModel()
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
                title = "Derived Metrics",
                subtitle = "Calculated locally · no AI · no network",
                onBack = onBack
            )

            Row {
                StatusPill(available = state.hasAccelerometer, liveText = "ACCEL OK", unavailableText = "ACCEL MISSING")
                Spacer(Modifier.size(12.dp))
                StatusPill(available = state.hasGyroscope, liveText = "GYRO OK", unavailableText = "GYRO MISSING")
            }

            if (!state.hasAccelerometer) {
                UnavailableMessage(
                    message = "Derived metrics need the accelerometer,\nwhich was not found on this device."
                )
                return@Column
            }

            MetricCard(
                title = "Movement Intensity",
                value = "${state.movementPercent.roundToInt()}%",
                classification = state.movement.label,
                percent = state.movementPercent,
                color = SenseColors.Cyan,
                note = "From filtered linear acceleration."
            )

            MetricCard(
                title = "Rotation Intensity",
                value = "${state.rotationPercent.roundToInt()}%",
                classification = state.rotation.label,
                percent = state.rotationPercent,
                color = SenseColors.Purple,
                note = if (state.hasGyroscope) "From gyroscope angular velocity." else "No gyroscope — computed from accelerometer only (approximation)."
            )

            MetricCard(
                title = "Phone Stability",
                value = "${state.stabilityPercent.roundToInt()}%",
                classification = state.stability.label,
                percent = state.stabilityPercent,
                color = SenseColors.Green,
                note = "Combined noise estimate from acceleration + rotation."
            )

            GlassCard(accent = SenseColors.Amber, modifier = Modifier.fillMaxWidth()) {
                SectionTitle(text = "Basic Orientation", accent = SenseColors.Amber)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.orientation.label.uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = SenseColors.Amber,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pitch ${state.pitchDeg.roundToInt()}° · Roll ${state.rollDeg.roundToInt()}°",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = SenseColors.TextSecondary
                    )
                }
                Text(
                    text = if (state.isLevel) "The phone is approximately level." else "The phone is tilted away from level.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isLevel) SenseColors.Green else SenseColors.TextSecondary
                )
            }

            GlassCard(accent = SenseColors.Purple, modifier = Modifier.fillMaxWidth()) {
                SectionTitle(text = "About These Metrics", accent = SenseColors.Purple)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "All calculations run on-device, deterministically, from raw accelerometer and gyroscope samples.\n\nPercentages are application-level approximations that make relative activity legible — they are not scientific measurements.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SenseColors.TextSecondary
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    classification: String,
    percent: Float,
    color: Color,
    note: String
) {
    GlassCard(accent = color, modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = title, accent = color)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = classification.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = SenseColors.TextPrimary
            )
        }
        Spacer(Modifier.height(8.dp))
        MetricBar(percent = percent, color = color)
        Spacer(Modifier.height(6.dp))
        Text(
            text = note,
            style = MaterialTheme.typography.bodySmall,
            color = SenseColors.TextMuted
        )
    }
}