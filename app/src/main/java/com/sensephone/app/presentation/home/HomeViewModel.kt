package com.sensephone.app.presentation.home

import android.app.Application
import com.sensephone.app.data.sensor.SamplingRate
import com.sensephone.app.data.sensor.SensorKind
import com.sensephone.app.data.sensor.SensorRepository
import com.sensephone.app.data.sensor.SensorSample
import com.sensephone.app.domain.metrics.MetricsEngine
import com.sensephone.app.domain.metrics.MetricsSnapshot
import com.sensephone.app.presentation.SensorBoundViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val availableKinds: List<SensorKind> = emptyList(),
    val featuredReadings: Map<SensorKind, SensorSample> = emptyMap(),
    val metrics: MetricsSnapshot = MetricsSnapshot()
) {
    val featured: List<SensorKind>
        get() = listOf(
            SensorKind.ACCELEROMETER,
            SensorKind.GYROSCOPE,
            SensorKind.MAGNETIC_FIELD,
            SensorKind.LIGHT
        )
    val availableCount: Int get() = availableKinds.size
}

/**
 * Powers the Home dashboard: sensor availability, live values for the four
 * featured sensors and the derived-metrics panel.  All observation starts on
 * [onScreenActive] and stops on [onScreenInactive].
 */
class HomeViewModel(application: Application) : SensorBoundViewModel(application) {

    private val _repo: SensorRepository get() = repository
    private val engine = MetricsEngine(repository)

    private val _state = MutableStateFlow(HomeUiState(availableKinds = repository.discoverAvailable()))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    override fun onScreenActive() {
        if (_state.value.metrics != MetricsSnapshot()) return

        collect(engine.observe()) { snapshot ->
            _state.update { it.copy(metrics = snapshot) }
        }

        _state.value.featured.forEach { kind ->
            if (repository.isAvailable(kind)) {
                collect(repository.observe(kind, SamplingRate.NORMAL)) { sample ->
                    _state.update { it.copy(featuredReadings = it.featuredReadings + (kind to sample)) }
                }
            }
        }
    }

    override fun onScreenInactive() {
        super.onScreenInactive()
    }
}