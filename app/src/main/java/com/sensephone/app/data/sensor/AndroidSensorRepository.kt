package com.sensephone.app.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Thin wrapper around [SensorManager] that exposes sensor data as cold Kotlin
 * [Flow]s.
 *
 * Lifecycle correctness is achieved by registering the [SensorEventListener] on
 * flow collection start and unregistering on cancellation.  This guarantees that
 * a sensor is sampled only while a collector is active, which in turn means only
 * while the relevant screen is visible.
 */
class AndroidSensorRepository(context: Context) : SensorRepository {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // -----------------------------------------------------------------
    //  Platform queries
    // -----------------------------------------------------------------

    override fun isAvailable(kind: SensorKind): Boolean =
        sensorManager.getDefaultSensor(kind.androidType) != null

    override fun getPlatformSensor(kind: SensorKind): Sensor? =
        sensorManager.getDefaultSensor(kind.androidType)

    // -----------------------------------------------------------------
    //  Reactive observation via callbackFlow
    // -----------------------------------------------------------------

    override fun observe(kind: SensorKind, rate: SamplingRate): Flow<SensorSample> =
        callbackFlow {
            val sensor = sensorManager.getDefaultSensor(kind.androidType)
            if (sensor == null) {
                close()
                return@callbackFlow
            }

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type != kind.androidType) return

                    val sample = SensorSample(
                        timestampNanos = event.timestamp,
                        x = if (kind.isVector) event.values[0] else 0f,
                        y = if (kind.isVector) event.values[1] else 0f,
                        z = if (kind.isVector) event.values[2] else 0f,
                        scalar = if (kind.isVector) 0f else event.values[0]
                    )
                    trySend(sample)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            sensorManager.registerListener(listener, sensor, rate.toAndroidDelay())

            awaitClose {
                sensorManager.unregisterListener(listener)
            }
        }

    // -----------------------------------------------------------------
    //  Discovery
    // -----------------------------------------------------------------

    override fun discoverAvailable(): List<SensorKind> =
        SensorKind.entries
            .filter { sensorManager.getDefaultSensor(it.androidType) != null }
            .sortedBy { it.priority }
}