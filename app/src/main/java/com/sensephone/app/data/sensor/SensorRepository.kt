package com.sensephone.app.data.sensor

import kotlinx.coroutines.flow.Flow

/** How aggressively the underlying [SensorManager] should deliver samples. */
enum class SamplingRate {
    /** Fastest, used by interactive experiments (bubble level, compass). */
    FAST,

    /** Balanced rate for live dashboards and graphs. */
    NORMAL,

    /** Conservative rate for glanceable overviews. */
    UI;

    fun toAndroidDelay(): Int = when (this) {
        FAST -> android.hardware.SensorManager.SENSOR_DELAY_GAME
        NORMAL -> android.hardware.SensorManager.SENSOR_DELAY_NORMAL
        UI -> android.hardware.SensorManager.SENSOR_DELAY_UI
    }
}

/**
 * Contract between the UI/domain layer and the Android sensor subsystem.
 *
 * The [SensorRepository] implementation is the only place that touches
 * [android.hardware.SensorManager] directly.
 */
interface SensorRepository {

    /** Whether a physical/default sensor of [kind] exists on this device. */
    fun isAvailable(kind: SensorKind): Boolean

    /** The platform [android.hardware.Sensor] instance for [kind], or null. */
    fun getPlatformSensor(kind: SensorKind): android.hardware.Sensor?

    /**
     * Returns a cold, never-completing [Flow] of [SensorSample]s for [kind].
     *
     * The underlying SensorEventListener is registered when a collector starts
     * and unregistered as soon as the collection is cancelled, so sensor
     * lifecycle is tied to flow collection.
     *
     * If the sensor is unavailable the flow never emits.
     */
    fun observe(kind: SensorKind, rate: SamplingRate): Flow<SensorSample>

    /**
     * List of all [SensorKind]s present on this device, ordered by application
     * priority (required sensors first).
     */
    fun discoverAvailable(): List<SensorKind>
}