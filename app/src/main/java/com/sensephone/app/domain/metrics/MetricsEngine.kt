package com.sensephone.app.domain.metrics

import com.sensephone.app.data.sensor.SamplingRate
import com.sensephone.app.data.sensor.SensorKind
import com.sensephone.app.data.sensor.SensorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Pure, deterministic, on-device engine that turns raw accelerometer and
 * gyroscope samples into derived metrics.
 *
 * All calculations run locally; no network, no AI.  The engine owns the
 * calculators and filters, so metric history is preserved across emissions
 * while the underlying flows are collected.
 *
 * [pitchDeg] / [rollDeg] follow the convention: roll > 0  => right side lower,
 * pitch > 0 => top edge lower.  Both are 0 when the device is flat.
 */
class MetricsEngine(
    private val repository: SensorRepository
) {

    private val movementCalc = MovementIntensityCalculator()
    private val rotationCalc = RotationIntensityCalculator()
    private val stabilityCalc = StabilityCalculator()
    private val orientationDetector = OrientationDetector()
    private val gravityFilter = LowPassFilter(alpha = 0.1f)

    private val hasAccelerometer = repository.isAvailable(SensorKind.ACCELEROMETER)
    private val hasGyroscope = repository.isAvailable(SensorKind.GYROSCOPE)
    private val hasMagnetometer = repository.isAvailable(SensorKind.MAGNETIC_FIELD)

    private var pendingX = 0f
    private var pendingY = 0f
    private var pendingZ = 0f
    private var receivedAny = false

    fun observe(): Flow<MetricsSnapshot> {
        if (!hasAccelerometer) {
            return flowOf(
                MetricsSnapshot(
                    hasAccelerometer = false,
                    hasGyroscope = hasGyroscope,
                    hasMagnetometer = hasMagnetometer,
                    isLevel = false
                )
            )
        }

        val accelFlow = repository.observe(SensorKind.ACCELEROMETER, SamplingRate.NORMAL)
        val gyroFlow = if (hasGyroscope) {
            repository.observe(SensorKind.GYROSCOPE, SamplingRate.NORMAL)
        } else {
            flowOf(null)
        }

        return combine(accelFlow, gyroFlow) { accel, gyro ->
            pendingX = accel.x; pendingY = accel.y; pendingZ = accel.z
            receivedAny = true

            val (gx, gy, gz) = gravityFilter.apply(accel.x, accel.y, accel.z)

            val movementPercent = movementCalc.update(accel.x, accel.y, accel.z)

            val rotationPercent = if (gyro != null) {
                rotationCalc.update(gyro.x, gyro.y, gyro.z)
            } else {
                rotationCalc.update(0f, 0f, 0f)
            }

            val stabilityPercent = if (gyro != null) {
                // Incorporate rotational activity so a spinning-but-still phone is
                // not reported as perfectly stable.
                val base = stabilityCalc.update(accel.x, accel.y, accel.z)
                (base - rotationPercent * 0.15f).coerceIn(0f, 100f)
            } else {
                stabilityCalc.update(accel.x, accel.y, accel.z)
            }

            val orientation = orientationDetector.update(gx, gy, gz)

            val pitchDeg = Math.toDegrees(atan2(-gy, gz).toDouble()).toFloat()
            val rollDeg = Math.toDegrees(atan2(gx, gz).toDouble()).toFloat()

            MetricsSnapshot(
                movementPercent = movementPercent,
                movement = MovementLevel.classify(movementPercent),
                rotationPercent = rotationPercent,
                rotation = RotationLevel.classify(rotationPercent),
                stabilityPercent = stabilityPercent,
                stability = StabilityLevel.classify(stabilityPercent),
                orientation = orientation,
                pitchDeg = pitchDeg,
                rollDeg = rollDeg,
                isLevel = abs(pitchDeg) < 2f && abs(rollDeg) < 2f,
                rawAccelX = accel.x,
                rawAccelY = accel.y,
                rawAccelZ = accel.z,
                hasAccelerometer = true,
                hasGyroscope = hasGyroscope,
                hasMagnetometer = hasMagnetometer
            )
        }
    }

    /**
     * Latest raw accelerometer vector (used by the orientation/level panels).
     * Only meaningful after [receivedAny] is true.
     */
    fun currentAcceleration(): FloatArray = floatArrayOf(pendingX, pendingY, pendingZ)
}