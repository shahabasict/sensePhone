package com.sensephone.app.presentation.experiments

import android.app.Application
import com.sensephone.app.data.sensor.SamplingRate
import com.sensephone.app.data.sensor.SensorKind
import com.sensephone.app.presentation.SensorBoundViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.log10

/** Human-readable light level categories. */
enum class LightLevel(val label: String, val hint: String, val lowerBoundLux: Float) {
    VERY_DARK("Very Dark", "Near complete darkness", 0f),
    DARK("Dark", "Dim indoor lighting", 5f),
    NORMAL("Normal", "Typical indoor lighting", 50f),
    BRIGHT("Bright", "Well lit / daylight", 300f),
    VERY_BRIGHT("Very Bright", "Strong daylight", 1000f)
}

data class LightMeterUiState(
    val available: Boolean = false,
    val lux: Float = 0f,
    val level: LightLevel = LightLevel.VERY_DARK,
    val levelFraction: Float = 0f,
    val maxObserved: Float = 0f
)

/**
 * Light Meter experiment using the ambient light sensor.
 *
 * Thresholds (lux):
 *   Very Dark  < 5
 *   Dark       5 – 50
 *   Normal     50 – 300
 *   Bright     300 – 1000
 *   Very Bright ≥ 1000
 *
 * The meter bar uses a logarithmic scale so typical indoor (≈100–300 lux) and
 * sunny-day (≈10,000+ lux) values are both visually distinguishable.
 */
class LightMeterViewModel(application: Application) : SensorBoundViewModel(application) {

    private val _state = MutableStateFlow(
        LightMeterUiState(available = repository.isAvailable(SensorKind.LIGHT))
    )
    val state: StateFlow<LightMeterUiState> = _state.asStateFlow()

    override fun onScreenActive() {
        if (!repository.isAvailable(SensorKind.LIGHT)) return
        collect(repository.observe(SensorKind.LIGHT, SamplingRate.NORMAL)) { sample ->
            _state.update { state ->
                val lux = sample.scalar
                val level = LightLevel.entries
                    .filter { lux >= it.lowerBoundLux }
                    .maxByOrNull { it.lowerBoundLux } ?: LightLevel.VERY_DARK
                val fraction = (log10(lux + 1f) / 5f).coerceIn(0f, 1f)
                LightMeterUiState(
                    available = true,
                    lux = lux,
                    level = level,
                    levelFraction = fraction,
                    maxObserved = maxOf(state.maxObserved, lux)
                )
            }
        }
    }
}