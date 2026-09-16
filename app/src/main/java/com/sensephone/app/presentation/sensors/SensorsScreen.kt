package com.sensephone.app.presentation.sensors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sensephone.app.data.sensor.SensorKind
import com.sensephone.app.presentation.components.DotGridBackground
import com.sensephone.app.presentation.components.GlassCard
import com.sensephone.app.presentation.components.ScreenHeader
import com.sensephone.app.presentation.components.StatusPill
import com.sensephone.app.presentation.components.sensorIcon
import com.sensephone.app.presentation.theme.SenseColors

@Composable
fun SensorsScreen(
    onOpenSensor: (SensorKind) -> Unit,
    viewModel: SensorsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        DotGridBackground()
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 110.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Sensors",
                    subtitle = "${state.availableCount} available on this device"
                )
            }
            items(state.rows.size) { index ->
                val row = state.rows[index]
                SensorListItem(
                    kind = row.kind,
                    available = row.available,
                    platformName = row.platformName,
                    onClick = { onOpenSensor(row.kind) }
                )
            }
        }
    }
}

@Composable
private fun SensorListItem(
    kind: SensorKind,
    available: Boolean,
    platformName: String?,
    onClick: () -> Unit
) {
    GlassCard(
        accent = if (available) SenseColors.Cyan else SenseColors.Red,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
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
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(text = kind.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = kind.unit.ifEmpty { "unit-less" },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
                if (available && platformName != null) {
                    Text(
                        text = platformName,
                        style = MaterialTheme.typography.labelSmall,
                        color = SenseColors.TextMuted
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(available = available, liveText = "AVAILABLE", unavailableText = "MISSING")
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = if (available) SenseColors.Cyan else SenseColors.TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}