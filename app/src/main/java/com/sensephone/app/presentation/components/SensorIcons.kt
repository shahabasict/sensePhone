package com.sensephone.app.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import com.sensephone.app.data.sensor.SensorKind

/** Icon associated with each supported sensor kind. */
fun sensorIcon(kind: SensorKind): ImageVector = when (kind) {
    SensorKind.ACCELEROMETER -> Icons.Filled.Speed
    SensorKind.GYROSCOPE -> Icons.Filled.RotateRight
    SensorKind.MAGNETIC_FIELD -> Icons.Filled.Explore
    SensorKind.LIGHT -> Icons.Filled.Lightbulb
    SensorKind.PROXIMITY -> Icons.Filled.Radar
    SensorKind.PRESSURE -> Icons.Filled.GraphicEq
    SensorKind.GRAVITY -> Icons.Filled.ArrowDownward
    SensorKind.LINEAR_ACCELERATION -> Icons.Filled.TrendingUp
    SensorKind.ROTATION_VECTOR -> Icons.Filled.Refresh
    SensorKind.RELATIVE_HUMIDITY -> Icons.Filled.WaterDrop
}