package com.sensephone.app.domain.metrics

import kotlin.math.sqrt

/**
 * Estimates how much the phone is being moved, based on the accelerometer.
 *
 * Approach:
 *  1. A low-pass filter extracts the gravity component from the raw signal.
 *  2. The high-pass (linear) acceleration is then `raw - gravity`.
 *  3. The magnitude of the linear acceleration is exponentially smoothed.
 *  4. The smoothed magnitude (m/s²) is folded into a 0–100 percentage using a
 *     saturating curve:  `percent = 100 * mag / (mag + 4)`.
 *
 * This avoids a naive, unbounded mapping of raw values to a percentage: a
 * perfectly still phone yields ~0%, while sustained jostling (> ~4 m/s² of
 * linear acceleration) approaches 100%.
 */
class MovementIntensityCalculator {

    private val gravityFilter = LowPassFilter(alpha = 0.15f)
    private var smoothedMagnitude = 0f

    fun reset() {
        gravityFilter.reset()
        smoothedMagnitude = 0f
    }

    /**
     * @return movement intensity as a percentage (0–100)
     */
    fun update(rawX: Float, rawY: Float, rawZ: Float): Float {
        val (gx, gy, gz) = gravityFilter.apply(rawX, rawY, rawZ)
        val lx = rawX - gx
        val ly = rawY - gy
        val lz = rawZ - gz
        val magnitude = sqrt(lx * lx + ly * ly + lz * lz)
        smoothedMagnitude += 0.15f * (magnitude - smoothedMagnitude)
        return 100f * smoothedMagnitude / (smoothedMagnitude + 4f)
    }
}