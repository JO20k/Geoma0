package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainTab
import com.example.ui.MapTapMode
import com.example.ui.SpooferViewModel
import com.example.ui.screens.DiagnosticsAndSetupScreen
import com.example.ui.screens.LiveMapCockpitScreen
import com.example.ui.screens.PresetsAndAppsScreen
import com.example.ui.screens.RouteStudioScreen
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceElevated

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                GeoPulseApp()
            }
        }
    }
}

@Composable
fun GeoPulseApp(
    spooferViewModel: SpooferViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by spooferViewModel.uiState.collectAsStateWithLifecycle()
    val telemetry by spooferViewModel.runtimeTelemetry.collectAsStateWithLifecycle()
    val savedLocations by spooferViewModel.savedLocations.collectAsStateWithLifecycle()
    val savedRoutes by spooferViewModel.savedRoutes.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        spooferViewModel.refreshMockPermissionCheck()
    }

    val requestPermissionsAction = {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            spooferViewModel.dismissBannerMessage()
        }
    }

    val configuration = LocalConfiguration.current
    val isExpandedScreen = configuration.screenWidthDp >= 600

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (!isExpandedScreen) {
                NavigationBar(
                    containerColor = TacticalSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    MainTab.entries.forEach { tab ->
                        val selected = uiState.selectedTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { spooferViewModel.selectTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = getTabIcon(tab, selected),
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyanPulse,
                                selectedTextColor = CyanPulse,
                                indicatorColor = TacticalSurfaceElevated,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isExpandedScreen) {
                NavigationRail(
                    containerColor = TacticalSurface,
                    modifier = Modifier.testTag("side_navigation_rail")
                ) {
                    MainTab.entries.forEach { tab ->
                        val selected = uiState.selectedTab == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { spooferViewModel.selectTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = getTabIcon(tab, selected),
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = CyanPulse,
                                selectedTextColor = CyanPulse,
                                indicatorColor = TacticalSurfaceElevated
                            ),
                            modifier = Modifier.testTag("nav_rail_${tab.route}")
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (uiState.selectedTab) {
                    MainTab.LIVE_MAP -> {
                        LiveMapCockpitScreen(
                            uiState = uiState,
                            telemetry = telemetry,
                            savedLocations = savedLocations,
                            onSearchQueryChange = spooferViewModel::onSearchQueryChanged,
                            onSelectSearchResult = spooferViewModel::selectSearchResult,
                            onMapTap = spooferViewModel::onMapTapped,
                            onMapPan = spooferViewModel::panMapCenter,
                            onSetZoom = spooferViewModel::setZoom,
                            onRecenter = spooferViewModel::recenterOnActiveCoordinate,
                            onSetTapMode = spooferViewModel::setMapTapMode,
                            onToggleJoystick = spooferViewModel::toggleJoystickOverlay,
                            onJoystickMoved = spooferViewModel::onJoystickMoved,
                            onStartStaticSpoof = {
                                if (!hasLocationPermission) requestPermissionsAction()
                                spooferViewModel.startStaticSpoofing()
                            },
                            onStartRouteSpoof = {
                                if (!hasLocationPermission) requestPermissionsAction()
                                spooferViewModel.startRouteSimulation()
                            },
                            onStopSpoof = spooferViewModel::stopAllSpoofing,
                            onTogglePauseRoute = spooferViewModel::togglePauseRoute,
                            onSeekRouteProgress = spooferViewModel::seekRouteProgress,
                            onPlaybackSpeedMultiplierChange = spooferViewModel::setPlaybackSpeedMultiplier,
                            onSetManualCoords = spooferViewModel::setManualCoordinates,
                            onSavePinToPresets = spooferViewModel::saveCurrentPinToPresets,
                            onLoadPreset = { preset ->
                                spooferViewModel.loadPresetLocation(preset, deployImmediately = false)
                            }
                        )
                    }

                    MainTab.ROUTE_STUDIO -> {
                        RouteStudioScreen(
                            uiState = uiState,
                            telemetry = telemetry,
                            onSelectProfile = spooferViewModel::selectMovementProfile,
                            onSpeedChange = spooferViewModel::setCustomSpeedKmh,
                            onPlaybackSpeedMultiplierChange = spooferViewModel::setPlaybackSpeedMultiplier,
                            onLoopModeChange = spooferViewModel::setLoopMode,
                            onRemoveWaypoint = spooferViewModel::removeWaypointAt,
                            onReverseWaypoints = spooferViewModel::reverseWaypoints,
                            onClearWaypoints = spooferViewModel::clearWaypoints,
                            onRebuildRoute = { spooferViewModel.buildRouteFromWaypoints(false) },
                            onSaveRoute = spooferViewModel::saveCurrentRouteToPresets,
                            onStartRouteSimulation = {
                                if (!hasLocationPermission) requestPermissionsAction()
                                spooferViewModel.startRouteSimulation()
                            },
                            onJumpToMapAddWaypoint = {
                                spooferViewModel.setMapTapMode(MapTapMode.ADD_WAYPOINT)
                                spooferViewModel.selectTab(MainTab.LIVE_MAP)
                            },
                            onNavigateBackToMap = {
                                spooferViewModel.selectTab(MainTab.LIVE_MAP)
                            }
                        )
                    }

                    MainTab.PRESETS_APPS -> {
                        PresetsAndAppsScreen(
                            compatibleApps = spooferViewModel.compatibleApps,
                            activeTargetAppId = uiState.activeTargetAppId,
                            savedLocations = savedLocations,
                            savedRoutes = savedRoutes,
                            onOptimizeForApp = spooferViewModel::applyTargetAppOptimization,
                            onLoadLocation = spooferViewModel::loadPresetLocation,
                            onDeleteLocation = spooferViewModel::deletePresetLocation,
                            onLoadRoute = spooferViewModel::loadSavedRoute,
                            onDeleteRoute = spooferViewModel::deleteSavedRoute,
                            onNavigateBackToMap = {
                                spooferViewModel.selectTab(MainTab.LIVE_MAP)
                            }
                        )
                    }

                    MainTab.DIAGNOSTICS -> {
                        DiagnosticsAndSetupScreen(
                            uiState = uiState,
                            telemetry = telemetry,
                            hasFineLocationPermission = hasLocationPermission,
                            onRequestLocationPermission = requestPermissionsAction,
                            onRefreshMockStatus = spooferViewModel::refreshMockPermissionCheck,
                            onNavigateBackToMap = {
                                spooferViewModel.selectTab(MainTab.LIVE_MAP)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun getTabIcon(tab: MainTab, selected: Boolean) = when (tab) {
    MainTab.LIVE_MAP -> if (selected) Icons.Filled.Map else Icons.Outlined.Map
    MainTab.ROUTE_STUDIO -> if (selected) Icons.Filled.Route else Icons.Outlined.Route
    MainTab.PRESETS_APPS -> if (selected) Icons.Filled.Apps else Icons.Outlined.Apps
    MainTab.DIAGNOSTICS -> if (selected) Icons.Filled.Radar else Icons.Outlined.Radar
}
