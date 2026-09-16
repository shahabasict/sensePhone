package com.sensephone.app.presentation.sensors

import android.app.Application
import com.sensephone.app.data.sensor.SamplingRate
import com.sensephone.app.data.sensor.SensorKind
import com.sensephone.app.data.sensor.SensorSample
import com.sensephone.app.presentation.SensorBoundViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

const val MAX_HISTORY_SAMPLES = 240
const val HISTORY_THROTTLE_MS = 100L

data class SensorDetailUiState(
    val latest: SensorSample? = null,
    val history: List<SensorSample> = emptyList(),
    val available: Boolean = false
)

/**
 * Live view for a single sensor: latest sample plus a bounded, in-memory
 * history window used by the real-time graph.  History is capped at
 * [MAX_HISTORY_SAMPLES] entries and only appended at ~10 Hz to keep
 * recomposition and drawing cheap.
 */
class SensorDetailViewModel(application: Application) : SensorBoundViewModel(application) {

    private val _state = MutableStateFlow(SensorDetailUiState())
    val state: StateFlow<SensorDetailUiState> = _state.asStateFlow()

    fun onScreenActive(kind: SensorKind) {
        val available = repository.isAvailable(kind)
        _state.update { it.copy(available = available) }
        if (!available) return

        var lastAppend = 0L
        collect(repository.observe(kind, SamplingRate.NORMAL)) { sample ->
            _state.update { state ->
                val now = System.currentTimeMillis()
                val history = if (now - lastAppend >= HISTORY_THROTTLE_MS) {
                    lastAppend = now
                    (state.history + sample).takeLast(MAX_HISTORY_SAMPLES)
                } else {
                    state.history
                }
                state.copy(latest = sample, history = history)
            }
        }
    }
}