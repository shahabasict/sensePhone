package com.sensephone.app.data.sensor

import android.hardware.Sensor

/**
 * The sensor types SensePhone knows how to exhibit and visualize.
 *
 * Each entry maps to an Android [Sensor.TYPE_*] constant and carries display
 * metadata (name, unit, number of values / axes).
 */
enum class SensorKind(
    val androidType: Int,
    val displayName: String,
    val unit: String,
    val valueCount: Int,
    val priority: Int
) {
    ACCELEROMETER(Sensor.TYPE_ACCELEROMETER, "Accelerometer", "m/s²", 3, 0),
    GYROSCOPE(Sensor.TYPE_GYROSCOPE, "Gyroscope", "rad/s", 3, 1),
    MAGNETIC_FIELD(Sensor.TYPE_MAGNETIC_FIELD, "Magnetometer", "µT", 3, 2),
    LIGHT(Sensor.TYPE_LIGHT, "Light Sensor", "lux", 1, 3),
    PROXIMITY(Sensor.TYPE_PROXIMITY, "Proximity Sensor", "cm", 1, 4),
    PRESSURE(Sensor.TYPE_PRESSURE, "Barometer", "hPa", 1, 5),
    GRAVITY(Sensor.TYPE_GRAVITY, "Gravity", "m/s²", 3, 6),
    LINEAR_ACCELERATION(Sensor.TYPE_LINEAR_ACCELERATION, "Linear Acceleration", "m/s²", 3, 7),
    ROTATION_VECTOR(Sensor.TYPE_ROTATION_VECTOR, "Rotation Vector", "", 3, 8),
    RELATIVE_HUMIDITY(Sensor.TYPE_RELATIVE_HUMIDITY, "Relative Humidity", "% RH", 1, 9);

    val isVector: Boolean get() = valueCount >= 3

    companion object {
        fun fromAndroidType(type: Int): SensorKind? = entries.firstOrNull { it.androidType == type }
    }
}