package com.sensephone.app.data.sensor

/**
 * A single, immutable sensor reading.
 *
 * For vector sensors (accelerometer, gyroscope, magnetometer …) [x], [y], [z]
 * carry the axis values.  For scalar sensors (light, proximity …) only [scalar]
 * is meaningful.
 *
 * [timestampNanos] is the event timestamp Android delivers alongside the sample
 * and is used only for ordering and the live-graph window.
 */
data class SensorSample(
    val timestampNanos: Long,
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val scalar: Float = 0f
) {
    val magnitude: Float
        get() = (x * x + y * y + z * z).let { if (it > 0f) Math.sqrt(it.toDouble()).toFloat() else 0f }
}