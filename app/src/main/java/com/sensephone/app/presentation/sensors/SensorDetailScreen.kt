package com.sensephone.app.presentation.sensors

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
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
import com.sensephone.app.data.sensor.SensorKind
import com.sensephone.app.data.sensor.SensorSample
import com.sensephone.app.presentation.components.AxisValue
import com.sensephone.app.presentation.components.DotGridBackground
import com.sensephone.app.presentation.components.GlassCard
import com.sensephone.app.presentation.components.LiveChart
import com.sensephone.app.presentation.components.ScreenHeader
import com.sensephone.app.presentation.components.SectionTitle
import com.sensephone.app.presentation.components.SensorLifecycle
import com.sensephone.app.presentation.components.StatusPill
import com.sensephone.app.presentation.components.UnavailableMessage
import com.sensephone.app.presentation.components.fmt
import com.sensephone.app.presentation.components.sensorIcon
import com.sensephone.app.presentation.theme.SenseColors

private val AXIS_COLORS = listOf(SenseColors.Cyan, SenseColors.Green, SenseColors.Amber)

@Composable
fun SensorDetailScreen(
    kind: SensorKind,
    onBack: () -> Unit,
    viewModel: SensorDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SensorLifecycle(viewModel, onActive = { viewModel.onScreenActive(kind) })

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
                title = kind.displayName,
                subtitle = "Sensor ${kind.androidType} · ${kind.unit.ifEmpty { "unit-less" }}",
                onBack = onBack
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusPill(available = state.available, liveText = "LIVE", unavailableText = "UNAVAILABLE")
                Text(
                    text = if (state.available) "Sampling at real-time rate" else "No default sensor found",
                    style = MaterialTheme.typography.bodySmall,
                    color = SenseColors.TextMuted
                )
            }

            if (!state.available) {
                UnavailableMessage(
                    message = "This sensor is not available on your device.\nSensePhone will keep working with the sensors you do have."
                )
                return@Column
            }

            CurrentValuesCard(kind = kind, sample = state.latest)

            GraphCard(kind = kind, state = state)

            HistoryStatsCard(kind = kind, history = state.history)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CurrentValuesCard(kind: SensorKind, sample: SensorSample?) {
    GlassCard(accent = SenseColors.Cyan, modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = "Current Values", accent = SenseColors.Cyan)
        Spacer(Modifier.height(12.dp))
        if (sample == null) {
            Text(
                text = "Waiting for first reading…",
                style = MaterialTheme.typography.bodyMedium,
                color = SenseColors.TextMuted
            )
        } else if (kind.isVector) {
            AxisValue("X", fmt(sample.x), valueColor = AXIS_COLORS[0])
            Spacer(Modifier.height(6.dp))
            AxisValue("Y", fmt(sample.y), valueColor = AXIS_COLORS[1])
            Spacer(Modifier.height(6.dp))
            AxisValue("Z", fmt(sample.z), valueColor = AXIS_COLORS[2])
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Magnitude  ${fmt(sample.magnitude)}  ${kind.unit}",
                style = MaterialTheme.typography.labelMedium,
                color = SenseColors.TextSecondary
            )
        } else {
            AxisValue("·", fmt(sample.scalar) + "  ${kind.unit}", valueColor = SenseColors.Cyan)
        }
    }
}

@Composable
private fun GraphCard(kind: SensorKind, state: SensorDetailUiState) {
    GlassCard(accent = SenseColors.Purple, modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = "Live History (last ~24s)", accent = SenseColors.Purple)
        Spacer(Modifier.height(10.dp))

        val series = if (kind.isVector) {
            listOf(
                state.history.map { it.x },
                state.history.map { it.y },
                state.history.map { it.z }
            )
        } else {
            listOf(state.history.map { it.scalar })
        }

        Box(Modifier.fillMaxWidth().height(180.dp)) {
            LiveChart(seriesData = series, seriesColors = AXIS_COLORS)
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (kind.isVector) {
                LegendDot(AXIS_COLORS[0], "X")
                LegendDot(AXIS_COLORS[1], "Y")
                LegendDot(AXIS_COLORS[2], "Z")
            } else {
                LegendDot(AXIS_COLORS[0], kind.unit)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, androidx.compose.foundation.shape.CircleShape))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 6.dp),
            color = SenseColors.TextSecondary
        )
    }
}

@Composable
private fun HistoryStatsCard(kind: SensorKind, history: List<SensorSample>) {
    GlassCard(accent = SenseColors.Green, modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = "Window Statistics", accent = SenseColors.Green)
        Spacer(Modifier.height(4.dp))
        if (history.isEmpty()) {
            Text("Collecting data…", style = MaterialTheme.typography.bodySmall, color = SenseColors.TextMuted)
            return@GlassCard
        }

        val source: List<Pair<String, Float>> = if (kind.isVector) {
            listOf(
                "X" to history.map { it.x },
                "Y" to history.map { it.y },
                "Z" to history.map { it.z }
            ).map { (label, values) -> label to values.average().toFloat() }
        } else {
            listOf(kind.displayName to history.map { it.scalar }.average().toFloat())
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            source.forEach { (label, avg) ->
                Column(horizontalAlignment = Alignment.Start) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = SenseColors.TextMuted)
                    Text(fmt(avg), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Average of last ${history.size}/${MAX_HISTORY_SAMPLES} samples in the bounded window.",
            style = MaterialTheme.typography.bodySmall,
            color = SenseColors.TextMuted
        )
    }
}