package com.sensephone.app.presentation.experiments

import android.app.Application
import com.sensephone.app.data.sensor.SamplingRate
import com.sensephone.app.data.sensor.SensorKind
import com.sensephone.app.presentation.SensorBoundViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MagneticFieldUiState(
    val available: Boolean = false,
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val magnitude: Float = 0f,
    val maxObserved: Float = 0f,
    val intensityFraction: Float = 0f
)

/**
 * Magnetic Field experiment: live X/Y/Z values, total magnitude and a
 * normalized intensity indicator that makes it easy to bring a magnet near
 * the phone and watch the response.
 */
class MagneticFieldViewModel(application: Application) : SensorBoundViewModel(application) {

    private val _state = MutableStateFlow(
        MagneticFieldUiState(available = repository.isAvailable(SensorKind.MAGNETIC_FIELD))
    )
    val state: StateFlow<MagneticFieldUiState> = _state.asStateFlow()

    override fun onScreenActive() {
        if (!repository.isAvailable(SensorKind.MAGNETIC_FIELD)) return
        collect(repository.observe(SensorKind.MAGNETIC_FIELD, SamplingRate.FAST)) { sample ->
            _state.update { state ->
                val magnitude = sample.magnitude
                val max = maxOf(state.maxObserved, magnitude)
                MagneticFieldUiState(
                    available = true,
                    x = sample.x,
                    y = sample.y,
                    z = sample.z,
                    magnitude = magnitude,
                    maxObserved = max,
                    // Normalized intensity: Earth's field ≈ 45 µT.  Normalize so
                    // typical values sit in the lower half and a strong magnet
                    // pushes toward 100%.
                    intensityFraction = (magnitude / 200f).coerceIn(0f, 1f)
                )
            }
        }
    }
}