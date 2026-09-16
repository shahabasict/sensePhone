package com.sensephone.app.domain.metrics

/**
 * Determines the basic physical orientation of the device from the gravity
 * vector obtained via a low-pass filtered accelerometer (or gravity sensor).
 *
 * Transitions are smoothed with a small hysteresis: a new orientation is only
 * reported once it has been observed for [confidenceThreshold] consecutive
 * samples.  This prevents flicker while moving between orientations.
 */
class OrientationDetector(private val confidenceThreshold: Int = 3) {

    private var current = DeviceOrientation.TILTED
    private var confidence = 0

    fun reset() {
        current = DeviceOrientation.TILTED
        confidence = 0
    }

    /** @return the currently reported (hysteresis-smoothed) orientation */
    fun update(gx: Float, gy: Float, gz: Float): DeviceOrientation {
        val candidate = DeviceOrientation.fromGravity(gx, gy, gz)
        if (candidate == current) {
            confidence = confidenceThreshold
            return current
        }
        confidence++
        if (confidence >= confidenceThreshold) {
            current = candidate
            confidence = 0
        }
        return current
    }
}