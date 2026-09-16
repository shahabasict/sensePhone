package com.sensephone.app.domain.metrics

import kotlin.math.sqrt

/**
 * Estimates rotational activity from gyroscope data (angular velocity in rad/s).
 *
 * 1. Magnitude of the angular-velocity vector is computed and exponentially
 *    smoothed.
 * 2. The smoothed magnitude is folded into a 0–100 percentage using a
 *    saturating curve:  `percent = 100 * mag / (mag + 3)`.
 *
 * A phone lying still (~0 rad/s) maps to ~0%, a brisk spin
 * (~ >3 rad/s sustained) approaches 100%.
 */
class RotationIntensityCalculator {

    private var smoothedMagnitude = 0f

    fun reset() { smoothedMagnitude = 0f }

    /** @return rotation intensity as a percentage (0–100) */
    fun update(wx: Float, wy: Float, wz: Float): Float {
        val magnitude = sqrt(wx * wx + wy * wy + wz * wz)
        smoothedMagnitude += 0.15f * (magnitude - smoothedMagnitude)
        return 100f * smoothedMagnitude / (smoothedMagnitude + 3f)
    }
}