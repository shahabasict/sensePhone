package com.sensephone.app.domain.metrics

import com.sensephone.app.data.sensor.SensorKind

/**
 * Enumerations and data classes that represent derived metrics produced by the
 * metrics engine.  No Android/framework dependencies here.
 */
enum class MovementLevel(val label: String) {
    STILL("Still"),
    LOW("Low"),
    MODERATE("Moderate"),
    HIGH("High"),
    VERY_HIGH("Very High");

    companion object {
        fun classify(percent: Float): MovementLevel = when {
            percent < 5f  -> STILL
            percent < 20f -> LOW
            percent < 45f -> MODERATE
            percent < 75f -> HIGH
            else          -> VERY_HIGH
        }
    }
}

enum class RotationLevel(val label: String) {
    NONE("None"),
    LOW("Low"),
    MODERATE("Moderate"),
    HIGH("High"),
    VERY_HIGH("Very High");

    companion object {
        fun classify(percent: Float): RotationLevel = when {
            percent < 5f  -> NONE
            percent < 20f -> LOW
            percent < 45f -> MODERATE
            percent < 75f -> HIGH
            else          -> VERY_HIGH
        }
    }
}

enum class StabilityLevel(val label: String) {
    VERY_STABLE("Very Stable"),
    STABLE("Stable"),
    MOVING("Moving"),
    UNSTABLE("Unstable");

    companion object {
        fun classify(percent: Float): StabilityLevel = when {
            percent >= 85f -> VERY_STABLE
            percent >= 65f -> STABLE
            percent >= 40f -> MOVING
            else           -> UNSTABLE
        }
    }
}

enum class DeviceOrientation(val label: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    FACE_UP("Face Up"),
    FACE_DOWN("Face Down"),
    TILTED("Tilted");

    companion object {
        fun fromGravity(gx: Float, gy: Float, gz: Float): DeviceOrientation {
            val ax = Math.abs(gx.toDouble()).toFloat()
            val ay = Math.abs(gy.toDouble()).toFloat()
            val az = Math.abs(gz.toDouble()).toFloat()
            return when {
                az > 7.5f && az > ax && az > ay -> if (gz > 0) FACE_UP else FACE_DOWN
                ay > 7.0f && ay >= ax && ay >= az -> if (gy > 0) PORTRAIT else PORTRAIT
                ax > 7.0f && ax >= ay && ax >= az -> LANDSCAPE
                else -> TILTED
            }
        }
    }
}

/**
 * Snapshot of all derived metrics produced by [MetricsEngine].
 *
 * Values are raw percentages where applicable; use the corresponding
 * `classify()` function to obtain a human-readable label.
 */
data class MetricsSnapshot(
    val movementPercent: Float = 0f,
    val movement: MovementLevel = MovementLevel.STILL,
    val rotationPercent: Float = 0f,
    val rotation: RotationLevel = RotationLevel.NONE,
    val stabilityPercent: Float = 100f,
    val stability: StabilityLevel = StabilityLevel.VERY_STABLE,
    val orientation: DeviceOrientation = DeviceOrientation.PORTRAIT,
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val isLevel: Boolean = true,
    val rawAccelX: Float = 0f,
    val rawAccelY: Float = 0f,
    val rawAccelZ: Float = 0f,
    val hasAccelerometer: Boolean = false,
    val hasGyroscope: Boolean = false,
    val hasMagnetometer: Boolean = false
)