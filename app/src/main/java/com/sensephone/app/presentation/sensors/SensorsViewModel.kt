package com.sensephone.app.presentation.sensors

import android.app.Application
import com.sensephone.app.data.sensor.SensorKind
import com.sensephone.app.presentation.SensorBoundViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Row shown in the full sensor list. */
data class SensorRow(
    val kind: SensorKind,
    val available: Boolean,
    val platformName: String?
)

data class SensorsUiState(
    val rows: List<SensorRow> = emptyList(),
    val availableCount: Int = 0
)

/**
 * Full catalog of supported sensors on this device.  Availability is stable for
 * the lifetime of the process, so no continuous observation is required here.
 */
class SensorsViewModel(application: Application) : SensorBoundViewModel(application) {

    private val rows = SensorKind.entries.sortedBy { it.priority }.map { kind ->
        SensorRow(
            kind = kind,
            available = repository.isAvailable(kind),
            platformName = repository.getPlatformSensor(kind)?.name
        )
    }

    private val _state = MutableStateFlow(
        SensorsUiState(
            rows = rows,
            availableCount = rows.count { it.available }
        )
    )
    val state: StateFlow<SensorsUiState> = _state.asStateFlow()
}