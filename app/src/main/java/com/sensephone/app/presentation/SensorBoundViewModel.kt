package com.sensephone.app.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sensephone.app.data.sensor.AndroidSensorRepository
import com.sensephone.app.data.sensor.SensorRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Base ViewModel that owns the [SensorRepository] and tracks active collection
 * jobs so that sensors are only sampled while the associated screen is active.
 */
abstract class SensorBoundViewModel(application: Application) : AndroidViewModel(application) {

    protected val repository: SensorRepository by lazy { AndroidSensorRepository(application) }

    private val activeJobs = mutableListOf<Job>()

    /** Launch a sensor-collection coroutine; it is cancelled on [onScreenInactive]. */
    protected fun <T> collect(flow: Flow<T>, onEach: suspend (T) -> Unit) {
        val job = viewModelScope.launch { flow.collect { onEach(it) } }
        activeJobs += job
    }

    /** Called by the UI when the screen enters composition. */
    open fun onScreenActive() = Unit

    /** Called by the UI when the screen leaves composition — stops all collectors. */
    open fun onScreenInactive() {
        activeJobs.forEach { it.cancel() }
        activeJobs.clear()
    }

    override fun onCleared() {
        activeJobs.forEach { it.cancel() }
        activeJobs.clear()
        super.onCleared()
    }
}