package com.sensephone.app.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.sensephone.app.presentation.SensorBoundViewModel

/**
 * Binds a [SensorBoundViewModel] to the composition lifecycle: sensors start
 * streaming when the screen enters, and all listeners are unregistered when it
 * leaves, so no sensor is sampled while the screen is not visible.
 */
@Composable
fun SensorLifecycle(
    viewModel: SensorBoundViewModel,
    onActive: () -> Unit = { viewModel.onScreenActive() }
) {
    LaunchedEffect(Unit) { onActive() }
    DisposableEffect(Unit) {
        onDispose { viewModel.onScreenInactive() }
    }
}