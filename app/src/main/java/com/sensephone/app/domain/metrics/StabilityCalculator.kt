package com.sensephone.app.domain.metrics

import java.util.ArrayDeque
import kotlin.math.sqrt

/**
 * Estimates how stable the phone currently is.
 *
 * The stability score is derived from a short rolling window of linear-acceleration
 * magnitudes (the same high-pass principle used by [MovementIntensityCalculator]):
 *
 *   instability = windowMean + windowStdDev          (both in m/s²)
 *   percent     = 100 - clamp(instability * SCALE, 0, 100)
 *
 * A perfectly still phone produces instability ≈ 0 → 100% stability; during
 * shaking, both mean and variance rise and the score drops.
 *
 * NOTE: this is an application-level approximation, not a scientific measurement.
 */
class StabilityCalculator(private val windowSize: Int = 24) {

    private val gravityFilter = LowPassFilter(alpha = 0.15f)
    private val magnitudes = ArrayDeque<Float>()
    private var smoothedMagnitude = 0f

    fun reset() {
        gravityFilter.reset()
        magnitudes.clear()
        smoothedMagnitude = 0f
    }

    /** @return stability as a percentage (0–100, 100 = most stable) */
    fun update(rawX: Float, rawY: Float, rawZ: Float): Float {
        val (gx, gy, gz) = gravityFilter.apply(rawX, rawY, rawZ)
        val lx = rawX - gx
        val ly = rawY - gy
        val lz = rawZ - gz
        val magnitude = sqrt(lx * lx + ly * ly + lz * lz)
        smoothedMagnitude += 0.2f * (magnitude - smoothedMagnitude)

        if (magnitudes.size == windowSize) magnitudes.removeFirst()
        magnitudes.addLast(smoothedMagnitude)

        val mean = magnitudes.average().toFloat()
        val variance = magnitudes
            .map { (it - mean) * (it - mean) }
            .average()
            .toFloat()
        val stdDev = sqrt(variance)

        val instability = ((mean + stdDev) * 18f).coerceIn(0f, 100f)
        return 100f - instability
    }
}