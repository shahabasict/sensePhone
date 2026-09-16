package com.sensephone.app.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensephone.app.presentation.theme.SenseColors
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun DotGridBackground(modifier: Modifier = Modifier, gridColor: Color = SenseColors.Grid) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SenseColors.Void, SenseColors.Surface, SenseColors.Void)
                )
            )
    ) {
        DotGrid(gridColor = gridColor)
    }
}

@Composable
private fun DotGrid(gridColor: Color, dotSize: Float = 1.4f, spacing: Int = 32) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                drawCircle(
                    color = gridColor,
                    radius = dotSize,
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
                y += spacing
            }
            x += spacing
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color = SenseColors.Cyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = SenseColors.Surface.copy(alpha = 0.72f),
    ) {
        Box(
            Modifier
                .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, accent: Color = SenseColors.Cyan) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(accent, CircleShape))
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = accent
        )
    }
}

@Composable
fun StatusPill(
    available: Boolean,
    modifier: Modifier = Modifier,
    liveText: String = "LIVE",
    unavailableText: String = "UNAVAILABLE"
) {
    val pulse = rememberInfiniteTransition(label = "pulse")
        .animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    val alpha by pulse
    val color = if (available) SenseColors.Green else SenseColors.Red
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = alpha * 0.12f))
            .border(1.dp, color.copy(alpha = alpha), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Text(
            text = if (available) liveText else unavailableText,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
fun AxisValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = SenseColors.TextPrimary
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SenseColors.TextMuted,
            modifier = Modifier.width(22.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = valueColor
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = SenseColors.Cyan)
            }
        }
        Column {
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun QuickLinkCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = SenseColors.Purple
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = SenseColors.Surface.copy(alpha = 0.9f),
            contentColor = SenseColors.TextPrimary
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = accent)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = accent)
        }
    }
}

@Composable
fun UnavailableMessage(
    message: String,
    modifier: Modifier = Modifier,
    accent: Color = SenseColors.Red
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = accent.copy(alpha = 0.8f),
            modifier = Modifier.size(42.dp)
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = SenseColors.TextSecondary
        )
    }
}

@Composable
fun MetricBar(
    percent: Float,
    modifier: Modifier = Modifier,
    color: Color = SenseColors.Cyan,
    label: String? = null
) {
    val animated by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 100f),
        label = "metricBar",
        animationSpec = tween(200)
    )
    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = SenseColors.TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(SenseColors.SurfaceRaised)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animated / 100f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color)))
            )
        }
    }
}

fun fmt(value: Float, decimals: Int = 2): String {
    val pattern = when (decimals) {
        0 -> "%.0f"
        1 -> "%.1f"
        else -> "%.2f"
    }
    return String.format(java.util.Locale.US, pattern, value)
}

@Composable
fun ScanningDot(modifier: Modifier = Modifier, color: Color = SenseColors.Cyan) {
    val transition = rememberInfiniteTransition(label = "scan")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    Box(
        modifier = modifier
            .size(4.dp)
            .background(
                color.copy(alpha = (0.3f + 0.7f * sin(phase * PI.toFloat() * 2f)).coerceIn(0.2f, 1f)),
                CircleShape
            )
    )
}