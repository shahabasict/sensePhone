package com.sensephone.app.presentation.navigation

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sensephone.app.data.sensor.AndroidSensorRepository
import com.sensephone.app.data.sensor.SensorKind
import com.sensephone.app.presentation.experiments.BubbleLevelScreen
import com.sensephone.app.presentation.experiments.CompassScreen
import com.sensephone.app.presentation.experiments.ExperimentId
import com.sensephone.app.presentation.experiments.ExperimentsScreen
import com.sensephone.app.presentation.experiments.LightMeterScreen
import com.sensephone.app.presentation.experiments.MagneticFieldScreen
import com.sensephone.app.presentation.home.HomeScreen
import com.sensephone.app.presentation.metrics.DerivedMetricsScreen
import com.sensephone.app.presentation.sensors.SensorDetailScreen
import com.sensephone.app.presentation.sensors.SensorsScreen
import com.sensephone.app.presentation.theme.SenseColors

private const val ROUTE_HOME = "home"
private const val ROUTE_EXPERIMENTS = "experiments"
private const val ROUTE_SENSORS = "sensors"
private const val ROUTE_METRICS = "metrics"
private const val ROUTE_SENSOR_DETAIL = "sensor/{type}"
private const val ROUTE_BUBBLE = "experiment/bubble"
private const val ROUTE_COMPASS = "experiment/compass"
private const val ROUTE_MAGNETIC = "experiment/magnetic"
private const val ROUTE_LIGHT = "experiment/light"

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val TABS = listOf(
    TabItem(ROUTE_HOME, "Home", Icons.Filled.Home),
    TabItem(ROUTE_EXPERIMENTS, "Experiments", Icons.Filled.Science),
    TabItem(ROUTE_SENSORS, "Sensors", Icons.Filled.GraphicEq)
)

fun sensorDetailRoute(kind: SensorKind): String = "sensor/${kind.androidType}"

/**
 * Root composable: bottom navigation for the three top-level tabs plus the
 * stacked detail screens (sensor detail, experiments, derived metrics).
 */
@Composable
fun SenseAppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(ROUTE_HOME, ROUTE_EXPERIMENTS, ROUTE_SENSORS)

    // Availability is resolved once per composition from the repository.
    val availability = rememberExperimentAvailability()

    Scaffold(
        containerColor = SenseColors.Void,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = SenseColors.Surface,
                    contentColor = SenseColors.Cyan
                ) {
                    TABS.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (selected) SenseColors.Cyan else SenseColors.TextMuted
                                )
                            },
                            label = {
                                Text(tab.label, fontSize = 11.sp)
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ROUTE_HOME) {
                HomeScreen(
                    onOpenSensor = { kind -> navController.navigate(sensorDetailRoute(kind)) },
                    onOpenMetrics = { navController.navigate(ROUTE_METRICS) },
                    onOpenSensors = { navController.navigate(ROUTE_SENSORS) }
                )
            }

            composable(ROUTE_EXPERIMENTS) {
                ExperimentsScreen(
                    availability = availability,
                    onOpen = { id ->
                        when (id) {
                            ExperimentId.BUBBLE -> navController.navigate(ROUTE_BUBBLE)
                            ExperimentId.COMPASS -> navController.navigate(ROUTE_COMPASS)
                            ExperimentId.MAGNETIC -> navController.navigate(ROUTE_MAGNETIC)
                            ExperimentId.LIGHT -> navController.navigate(ROUTE_LIGHT)
                        }
                    }
                )
            }

            composable(ROUTE_SENSORS) {
                SensorsScreen(
                    onOpenSensor = { kind -> navController.navigate(sensorDetailRoute(kind)) }
                )
            }

            composable(ROUTE_METRICS) {
                DerivedMetricsScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = ROUTE_SENSOR_DETAIL,
                arguments = listOf(navArgument("type") { type = NavType.IntType })
            ) { entry ->
                val type = entry.arguments?.getInt("type") ?: return@composable
                val kind = SensorKind.fromAndroidType(type)
                if (kind == null) {
                    navController.popBackStack()
                    return@composable
                }
                SensorDetailScreen(kind = kind, onBack = { navController.popBackStack() })
            }

            composable(ROUTE_BUBBLE) {
                BubbleLevelScreen(onBack = { navController.popBackStack() })
            }
            composable(ROUTE_COMPASS) {
                CompassScreen(onBack = { navController.popBackStack() })
            }
            composable(ROUTE_MAGNETIC) {
                MagneticFieldScreen(onBack = { navController.popBackStack() })
            }
            composable(ROUTE_LIGHT) {
                LightMeterScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/** Experiment availability rules (compass has a sensor-fusion fallback). */
@Composable
private fun rememberExperimentAvailability(): (ExperimentId) -> Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember(context) { AndroidSensorRepository(context) }
    return remember(repo) {
        { id ->
            when (id) {
                ExperimentId.BUBBLE -> repo.isAvailable(SensorKind.ACCELEROMETER)
                ExperimentId.COMPASS ->
                    repo.isAvailable(SensorKind.ROTATION_VECTOR) ||
                        (repo.isAvailable(SensorKind.ACCELEROMETER) &&
                            repo.isAvailable(SensorKind.MAGNETIC_FIELD))
                ExperimentId.MAGNETIC -> repo.isAvailable(SensorKind.MAGNETIC_FIELD)
                ExperimentId.LIGHT -> repo.isAvailable(SensorKind.LIGHT)
            }
        }
    }
}