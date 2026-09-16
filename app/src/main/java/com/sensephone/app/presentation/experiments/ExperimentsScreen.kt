package com.sensephone.app.presentation.experiments

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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sensephone.app.presentation.components.DotGridBackground
import com.sensephone.app.presentation.components.GlassCard
import com.sensephone.app.presentation.components.ScreenHeader
import com.sensephone.app.presentation.components.StatusPill
import com.sensephone.app.presentation.theme.SenseColors

enum class ExperimentId { BUBBLE, COMPASS, MAGNETIC, LIGHT }

private data class ExperimentMeta(
    val id: ExperimentId,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun ExperimentsScreen(
    availability: (ExperimentId) -> Boolean,
    onOpen: (ExperimentId) -> Unit
) {
    val experiments = listOf(
        ExperimentMeta(
            id = ExperimentId.BUBBLE,
            title = "Bubble Level",
            description = "Tilt the phone and watch the bubble drift; aim for dead-centre.",
            icon = Icons.Filled.WaterDrop,
            accent = SenseColors.Cyan
        ),
        ExperimentMeta(
            id = ExperimentId.COMPASS,
            title = "Compass",
            description = "Live heading with smooth needle, bearings and direction names.",
            icon = Icons.Filled.Explore,
            accent = SenseColors.Green
        ),
        ExperimentMeta(
            id = ExperimentId.MAGNETIC,
            title = "Magnetic Field",
            description = "Watch X/Y/Z fields and total magnitude; bring a magnet close.",
            icon = Icons.Filled.Bolt,
            accent = SenseColors.Orange
        ),
        ExperimentMeta(
            id = ExperimentId.LIGHT,
            title = "Light Meter",
            description = "Measure ambient light in lux with a human-readable scale.",
            icon = Icons.Filled.Lightbulb,
            accent = SenseColors.Amber
        )
    )

    Box(Modifier.fillMaxSize()) {
        DotGridBackground()
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 110.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Experiments",
                    subtitle = "Interactive sensor playground"
                )
            }
            items(experiments.size) { index ->
                val meta = experiments[index]
                ExperimentCard(
                    meta = meta,
                    available = availability(meta.id),
                    onClick = { onOpen(meta.id) }
                )
            }
            item {
                Text(
                    text = "Experiments need the matching physical sensors. Unavailable experiments stay safely disabled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SenseColors.TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ExperimentCard(
    meta: ExperimentMeta,
    available: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        accent = if (available) meta.accent else SenseColors.Red,
        modifier = Modifier.fillMaxWidth().clickable(enabled = available, onClick = onClick)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                meta.icon,
                contentDescription = null,
                tint = if (available) meta.accent else SenseColors.TextMuted,
                modifier = Modifier.size(32.dp)
            )
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(text = meta.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = meta.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SenseColors.TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(
                    available = available,
                    liveText = "READY",
                    unavailableText = "NO SENSOR"
                )
                if (available) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = meta.accent,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}