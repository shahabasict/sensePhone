package com.sensephone.app.presentation.experiments

import android.app.Application
import android.hardware.SensorManager
import com.sensephone.app.data.sensor.SamplingRate
import com.sensephone.app.data.sensor.SensorKind
import com.sensephone.app.domain.metrics.LowPassFilter
import com.sensephone.app.presentation.SensorBoundViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

enum class LevelTolerance(val toleranceDeg: Float, val label: String) {
    PRECISION(0.5f, "Precision"),
    STANDARD(2.0f, "Standard"),
    ROUGH(5.0f, "Rough")
}

data class BubbleLevelUiState(
    val available: Boolean = false,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val isLevel: Boolean = false,
    val tolerance: LevelTolerance = LevelTolerance.STANDARD,
    val gravityMagnitude: Float = 0f,
    val usingMagnetometer: Boolean = false
)

/**
 * Bubble Level experiment.
 *
 * Pitch/roll are derived with the rotation-matrix approach when a magnetometer
 * is present (accelerometer + magnetometer fusion, exactly as the reference
 * project), falling back to pure gravity-vector trigonometry otherwise.
 */
class BubbleLevelViewModel(application: Application) : SensorBoundViewModel(application) {

    private val lowPass = LowPassFilter(alpha = 0.1f)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val magnetVector = FloatArray(3)
    private var smoothedPitch = 0f
    private var smoothedRoll = 0f

    private val _state = MutableStateFlow(
        BubbleLevelUiState(available = repository.isAvailable(SensorKind.ACCELEROMETER))
    )
    val state: StateFlow<BubbleLevelUiState> = _state.asStateFlow()

    override fun onScreenActive() {
        if (!repository.isAvailable(SensorKind.ACCELEROMETER)) return

        collect(repository.observe(SensorKind.ACCELEROMETER, SamplingRate.FAST)) { sample ->
            update(sample.x, sample.y, sample.z)
        }

        if (repository.isAvailable(SensorKind.MAGNETIC_FIELD)) {
            collect(repository.observe(SensorKind.MAGNETIC_FIELD, SamplingRate.FAST)) { sample ->
                magnetVector[0] = sample.x
                magnetVector[1] = sample.y
                magnetVector[2] = sample.z
            }
            _state.update { it.copy(usingMagnetometer = true) }
        }
    }

    fun setTolerance(tolerance: LevelTolerance) {
        _state.update { it.copy(tolerance = tolerance) }
    }

    private fun update(ax: Float, ay: Float, az: Float) {
        val (gx, gy, gz) = lowPass.apply(ax, ay, az)
        val magnitude = sqrt(gx * gx + gy * gy + gz * gz)

        val rawPitch: Float
        val rawRoll: Float

        val useFusion = _state.value.usingMagnetometer
        if (useFusion && SensorManager.getRotationMatrix(rotationMatrix, null, floatArrayOf(gx, gy, gz), magnetVector)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            rawPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
            rawRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
        } else {
            // Gravity-vector fallback.
            rawPitch = Math.toDegrees(atan2(-gy, gz).toDouble()).toFloat()
            rawRoll = Math.toDegrees(atan2(gx, gz).toDouble()).toFloat()
        }

        smoothedPitch += 0.1f * (rawPitch - smoothedPitch)
        smoothedRoll += 0.1f * (rawRoll - smoothedRoll)

        val tolerance = _state.value.tolerance.toleranceDeg
        _state.update {
            it.copy(
                pitch = smoothedPitch,
                roll = smoothedRoll,
                isLevel = abs(smoothedPitch) < tolerance && abs(smoothedRoll) < tolerance,
                gravityMagnitude = magnitude
            )
        }
    }
}