package com.sensephone.app.presentation.metrics

import android.app.Application
import com.sensephone.app.domain.metrics.MetricsEngine
import com.sensephone.app.domain.metrics.MetricsSnapshot
import com.sensephone.app.presentation.SensorBoundViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dedicated live screen for the four derived metrics:
 * movement, rotation, stability and orientation.
 */
class DerivedMetricsViewModel(application: Application) : SensorBoundViewModel(application) {

    private val engine = MetricsEngine(repository)

    private val _state = MutableStateFlow(MetricsSnapshot())
    val state: StateFlow<MetricsSnapshot> = _state.asStateFlow()

    override fun onScreenActive() {
        collect(engine.observe()) { snapshot -> _state.value = snapshot }
    }
}