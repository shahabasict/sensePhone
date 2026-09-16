package com.sensephone.app.presentation.experiments

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sensephone.app.presentation.components.DotGridBackground
import com.sensephone.app.presentation.components.GlassCard
import com.sensephone.app.presentation.components.ScreenHeader
import com.sensephone.app.presentation.components.SectionTitle
import com.sensephone.app.presentation.components.SensorLifecycle
import com.sensephone.app.presentation.components.StatusPill
import com.sensephone.app.presentation.components.UnavailableMessage
import com.sensephone.app.presentation.components.fmt
import com.sensephone.app.presentation.theme.SenseColors

private val LIGHT_BAR_COLORS = listOf(
    SenseColors.Purple.copy(alpha = 0.6f),
    SenseColors.Cyan,
    SenseColors.Green,
    SenseColors.Amber,
    SenseColors.Red
)

@Composable
fun LightMeterScreen(
    onBack: () -> Unit,
    viewModel: LightMeterViewModel = viewModel()
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
            ScreenHeader(title = "Light Meter", subtitle = "Ambient Light Sensor · lux", onBack = onBack)
            Row {
                StatusPill(available = state.available, liveText = "LIVE", unavailableText = "UNAVAILABLE")
            }

            if (!state.available) {
                UnavailableMessage(
                    message = "The ambient light sensor is not available on this device."
                )
                return@Column
            }

            val levelIndex = LightLevel.entries.indexOf(state.level).coerceAtLeast(0)
            val levelColor by animateColorAsState(
                targetValue = LIGHT_BAR_COLORS.getOrElse(levelIndex) { SenseColors.Amber },
                label = "lightColor",
                animationSpec = androidx.compose.animation.core.tween(200)
            )

            GlassCard(accent = levelColor, modifier = Modifier.fillMaxWidth()) {
                SectionTitle(text = "Current Light", accent = levelColor)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = fmt(state.lux),
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = levelColor
                    )
                    Text(
                        text = "lux",
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = SenseColors.TextSecondary
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = state.level.label.uppercase() + "  ·  " + state.level.hint,
                    style = MaterialTheme.typography.titleMedium,
                    color = levelColor
                )
            }

            LightBar(fraction = state.levelFraction, color = levelColor)

            GlassCard(accent = SenseColors.Amber, modifier = Modifier.fillMaxWidth()) {
                SectionTitle(text = "Scale (lux)", accent = SenseColors.Amber)
                Spacer(Modifier.height(8.dp))
                LightLevel.entries.forEach { level ->
                    val active = level == state.level
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = level.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (active) levelColor else SenseColors.TextMuted,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "≥ ${luxBound(level)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = SenseColors.TextMuted
                        )
                    }
                }
                Text(
                    text = "Peak observed: ${fmt(state.maxObserved)} lux in this session.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = SenseColors.TextSecondary
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LightBar(fraction: Float, color: Color) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        label = "lightBar",
        animationSpec = androidx.compose.animation.core.tween(200)
    )
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .background(SenseColors.SurfaceRaised, RoundedCornerShape(50))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .height(22.dp)
                    .background(
                        Brush.horizontalGradient(LIGHT_BAR_COLORS),
                        RoundedCornerShape(50)
                    )
            )
        }
        Text(
            text = "LOGARITHMIC METER · 1 → 100,000 lux",
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = SenseColors.TextMuted
        )
    }
}

private fun luxBound(level: LightLevel): String = when (level) {
    LightLevel.VERY_DARK -> "0"
    else -> "${level.lowerBoundLux.toInt()} lux"
}