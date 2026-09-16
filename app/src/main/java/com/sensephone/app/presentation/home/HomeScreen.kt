package com.sensephone.app.presentation.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sensephone.app.data.sensor.SensorKind
import com.sensephone.app.domain.metrics.DeviceOrientation
import com.sensephone.app.presentation.components.AxisValue
import com.sensephone.app.presentation.components.DotGridBackground
import com.sensephone.app.presentation.components.GlassCard
import com.sensephone.app.presentation.components.MetricBar
import com.sensephone.app.presentation.components.QuickLinkCard
import com.sensephone.app.presentation.components.SectionTitle
import com.sensephone.app.presentation.components.SensorLifecycle
import com.sensephone.app.presentation.components.StatusPill
import com.sensephone.app.presentation.components.fmt
import com.sensephone.app.presentation.components.sensorIcon
import com.sensephone.app.presentation.theme.SenseColors
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    onOpenSensor: (SensorKind) -> Unit,
    onOpenMetrics: () -> Unit,
    onOpenSensors: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SensorLifecycle(viewModel)

    Box(Modifier.fillMaxSize()) {
        DotGridBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 110.dp)
        ) {
            item { HomeHeader(availableCount = state.availableCount) }

            item {
                QuickLinkCard(
                    title = "Derived Metrics",
                    subtitle = "Movement · Rotation · Stability · Orientation",
                    onClick = onOpenMetrics,
                    accent = SenseColors.Purple
                )
            }

            item { MetricsCard(state.metrics.movementPercent, state.metrics.stabilityPercent, state.metrics.orientation) }

            item {
                SectionTitle(text = "Featured Sensors", accent = SenseColors.Cyan)
            }

            state.featured.forEach { kind ->
                item {
                    FeaturedSensorCard(
                        kind = kind,
                        available = state.availableKinds.contains(kind),
                        valueText = featuredValueText(kind, state),
                        onClick = { onOpenSensor(kind) }
                    )
                }
            }

            item {
                QuickLinkCard(
                    title = "All Sensors",
                    subtitle = "Browse every sensor on this device",
                    onClick = onOpenSensors,
                    accent = SenseColors.Cyan
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(availableCount: Int) {
    Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "SENSEPHONE",
                style = MaterialTheme.typography.headlineLarge,
                color = SenseColors.Cyan,
                letterSpacing = 4.sp
            )
        }
        Text(
            text = "SENSOR EXPLORATION LAB",
            style = MaterialTheme.typography.labelMedium,
            color = SenseColors.TextMuted,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(12.dp))
        StatusPill(
            available = availableCount > 0,
            liveText = "$availableCount SENSORS DETECTED",
            unavailableText = "NO SENSORS DETECTED"
        )
    }
}

@Composable
private fun MetricsCard(
    movementPercent: Float,
    stabilityPercent: Float,
    orientation: DeviceOrientation
) {
    GlassCard(accent = SenseColors.Green, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(text = "Live State", accent = SenseColors.Green)
            Text(
                text = orientation.label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = SenseColors.Amber
            )
        }
        Spacer(Modifier.height(12.dp))
        MetricBar(percent = movementPercent, label = "Movement Intensity  ${movementPercent.roundToInt()}%", color = SenseColors.Cyan)
        Spacer(Modifier.height(12.dp))
        MetricBar(percent = stabilityPercent, label = "Stability  ${stabilityPercent.roundToInt()}%", color = SenseColors.Green)
    }
}

@Composable
private fun FeaturedSensorCard(
    kind: SensorKind,
    available: Boolean,
    valueText: String,
    onClick: () -> Unit
) {
    GlassCard(
        accent = if (available) SenseColors.Cyan else SenseColors.Red,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                sensorIcon(kind),
                contentDescription = null,
                tint = if (available) SenseColors.Cyan else SenseColors.TextMuted,
                modifier = Modifier.size(28.dp)
            )
            Column(
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            ) {
                Text(
                    text = kind.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (available) valueText else "Not available on this device",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (available) SenseColors.TextSecondary else SenseColors.Red
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(available = available, liveText = "LIVE", unavailableText = "N/A")
                IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open ${kind.displayName}",
                        tint = if (available) SenseColors.Cyan else SenseColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Compact one-line value string for the featured cards. */
private fun featuredValueText(kind: SensorKind, state: HomeUiState): String {
    val sample = state.featuredReadings[kind] ?: return "--"
    return when {
        kind == SensorKind.LIGHT -> "{ ${fmt(sample.scalar)} ${kind.unit} }"
        kind.isVector -> "X ${fmt(sample.x)}  Y ${fmt(sample.y)}  Z ${fmt(sample.z)}  [${kind.unit}]"
        else -> "${fmt(sample.scalar)} ${kind.unit}"
    }
}