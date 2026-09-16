package com.sensephone.app.presentation.experiments

import android.app.Application
import android.hardware.SensorManager
import com.sensephone.app.data.sensor.SamplingRate
import com.sensephone.app.data.sensor.SensorKind
import com.sensephone.app.data.sensor.SensorSample
import com.sensephone.app.presentation.SensorBoundViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.atan2

data class CompassUiState(
    val available: Boolean = false,
    val headingDeg: Float = 0f,
    val direction: String = "N",
    val usingRotationVector: Boolean = false
)

private val DIRECTIONS = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

/**
 * Compass experiment.
 *
 * Prefers the rotation-vector sensor (fused sensor-fusion azimuth).  Falls back
 * to the classic accelerometer + magnetometer rotation-matrix fusion when the
 * rotation vector is unavailable.
 */
class CompassViewModel(application: Application) : SensorBoundViewModel(application) {

    private val useRotationVector = repository.isAvailable(SensorKind.ROTATION_VECTOR)

    private var lastAccel: SensorSample? = null
    private var lastMag: SensorSample? = null
    private var smoothedHeading = 0f

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val _state = MutableStateFlow(
        CompassUiState(
            available = useRotationVector ||
                (repository.isAvailable(SensorKind.ACCELEROMETER) &&
                    repository.isAvailable(SensorKind.MAGNETIC_FIELD)),
            usingRotationVector = useRotationVector
        )
    )
    val state: StateFlow<CompassUiState> = _state.asStateFlow()

    override fun onScreenActive() {
        if (useRotationVector) {
            collect(repository.observe(SensorKind.ROTATION_VECTOR, SamplingRate.FAST)) { sample ->
                updateFromRotationVector(sample)
            }
        } else {
            collect(repository.observe(SensorKind.ACCELEROMETER, SamplingRate.FAST)) { sample ->
                lastAccel = sample
                updateFusion()
            }
            if (repository.isAvailable(SensorKind.MAGNETIC_FIELD)) {
                collect(repository.observe(SensorKind.MAGNETIC_FIELD, SamplingRate.FAST)) { sample ->
                    lastMag = sample
                    updateFusion()
                }
            }
        }
    }

    private fun updateFromRotationVector(sample: SensorSample) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, floatArrayOf(sample.x, sample.y, sample.z))
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        publish(azimuthDeg)
    }

    private fun updateFusion() {
        val accel = lastAccel ?: return
        val mag = lastMag ?: return
        if (SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                floatArrayOf(accel.x, accel.y, accel.z),
                floatArrayOf(mag.x, mag.y, mag.z)
            )
        ) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            publish(azimuthDeg)
        }
    }

    private fun publish(azimuthDeg: Float) {
        val normalized = ((azimuthDeg % 360f) + 360f) % 360f
        // Shortest-path smoothing to avoid north-wrap jumps.
        val diff = (normalized - smoothedHeading + 540f) % 360f - 180f
        smoothedHeading = (smoothedHeading + 0.25f * diff + 360f) % 360f
        if (smoothedHeading < 0f) smoothedHeading += 360f

        val direction = DIRECTIONS[((normalized + 22.5f) / 45f).toInt() % 8]
        _state.update { it.copy(headingDeg = smoothedHeading, direction = direction) }
    }
}