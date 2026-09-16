package com.sensephone.app.domain.metrics

/**
 * Simple first-order low-pass filter applied to a three-axis signal.
 *
 * The filter approaches the steady-state (gravity) component when applied to
 * the accelerometer signal with [alpha] in the range 0.05–0.25.
 */
class LowPassFilter(private val alpha: Float = 0.1f) {
    private var x = 0f
    private var y = 0f
    private var z = 0f
    private var initialized = false

    /** @return smoothed (x, y, z) */
    fun apply(rawX: Float, rawY: Float, rawZ: Float): Triple<Float, Float, Float> {
        if (!initialized) {
            x = rawX; y = rawY; z = rawZ
            initialized = true
        } else {
            x += alpha * (rawX - x)
            y += alpha * (rawY - y)
            z += alpha * (rawZ - z)
        }
        return Triple(x, y, z)
    }

    fun reset() { initialized = false; x = 0f; y = 0f; z = 0f }
}