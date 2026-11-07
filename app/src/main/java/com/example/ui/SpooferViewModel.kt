package com.example.ui

import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GeoMath
import com.example.data.GeoNetworkClient
import com.example.data.GeoPoint
import com.example.data.GeoSpoofDatabase
import com.example.data.GeoSpoofRepository
import com.example.data.MovementProfile
import com.example.data.NominatimPlace
import com.example.data.RouteLoopMode
import com.example.data.SavedLocationEntity
import com.example.data.SavedRouteEntity
import com.example.data.TargetAppInfo
import com.example.service.MockLocationService
import com.example.service.MockTelemetryController
import com.example.service.TelemetryRuntimeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MapTapMode(val title: String) {
    SINGLE_PIN("Pin Target"),
    ADD_WAYPOINT("Add Waypoint")
}

enum class MainTab(val route: String, val label: String) {
    LIVE_MAP("live_map", "Live Map"),
    ROUTE_STUDIO("route_studio", "Route Studio"),
    PRESETS_APPS("presets_apps", "Presets & Apps"),
    DIAGNOSTICS("diagnostics", "Setup & Radar")
}

data class SpooferUiState(
    val selectedTab: MainTab = MainTab.LIVE_MAP,
    val mapCenter: GeoPoint = GeoPoint(35.6595, 139.7004, "Shibuya Crossing"),
    val mapZoom: Int = 15,
    val mapTapMode: MapTapMode = MapTapMode.SINGLE_PIN,
    val selectedTargetPin: GeoPoint = GeoPoint(35.6595, 139.7004, "Shibuya Crossing, Tokyo"),
    val routeWaypoints: List<GeoPoint> = listOf(
        GeoPoint(35.6595, 139.7004, "Shibuya Scramble"),
        GeoPoint(35.6702, 139.7027, "Harajuku Station")
    ),
    val generatedRoutePoints: List<GeoPoint> = emptyList(),
    val routeDistanceMeters: Double = 0.0,
    val routeDurationSeconds: Double = 0.0,
    val isRouteRoadSnapped: Boolean = false,
    val isRoutingLoading: Boolean = false,
    val movementProfile: MovementProfile = MovementProfile.SCOOTER,
    val customSpeedKmh: Float = MovementProfile.SCOOTER.defaultSpeedKmh,
    val playbackSpeedMultiplier: Float = 1.0f,
    val loopMode: RouteLoopMode = RouteLoopMode.PING_PONG,
    val searchQuery: String = "",
    val searchResults: List<NominatimPlace> = emptyList(),
    val isSearching: Boolean = false,
    val showJoystickOverlay: Boolean = true,
    val isMockAppSelectedInSystem: Boolean = false,
    val activeTargetAppId: String = "snapchat",
    val statusBannerMessage: String? = null
)

class SpooferViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GeoSpoofRepository
    val savedLocations: StateFlow<List<SavedLocationEntity>>
    val savedRoutes: StateFlow<List<SavedRouteEntity>>
    val compatibleApps: List<TargetAppInfo>
    val runtimeTelemetry: StateFlow<TelemetryRuntimeState> = MockTelemetryController.state

    private val _uiState = MutableStateFlow(SpooferUiState())
    val uiState: StateFlow<SpooferUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        val dao = GeoSpoofDatabase.getInstance(application).geoSpoofDao()
        repository = GeoSpoofRepository(dao)
        compatibleApps = repository.compatibleTargetApps

        savedLocations = repository.savedLocations.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        savedRoutes = repository.savedRoutes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.seedDefaultsIfEmpty()
            refreshMockPermissionCheck()
            buildRouteFromWaypoints(autoStartSimulation = true)
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        refreshMockPermissionCheck()
    }

    fun refreshMockPermissionCheck() {
        val ctx = getApplication<Application>()
        val granted = try {
            val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_MOCK_LOCATION,
                Process.myUid(),
                ctx.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
        _uiState.update { it.copy(isMockAppSelectedInSystem = granted) }
    }

    fun setMapTapMode(mode: MapTapMode) {
        _uiState.update { it.copy(mapTapMode = mode) }
    }

    fun setZoom(zoom: Int) {
        _uiState.update { it.copy(mapZoom = zoom.coerceIn(3, 18)) }
    }

    fun panMapCenter(newCenter: GeoPoint) {
        _uiState.update { it.copy(mapCenter = newCenter) }
    }

    fun recenterOnActiveCoordinate() {
        val target = if (runtimeTelemetry.value.isSpoofingActive) {
            runtimeTelemetry.value.currentPoint
        } else {
            _uiState.value.selectedTargetPin
        }
        _uiState.update { it.copy(mapCenter = target) }
    }

    fun onMapTapped(point: GeoPoint) {
        val mode = _uiState.value.mapTapMode
        if (mode == MapTapMode.SINGLE_PIN) {
            val labeled = point.copy(label = "Custom Coordinate Pin")
            _uiState.update {
                it.copy(
                    selectedTargetPin = labeled,
                    statusBannerMessage = "Target pin locked at %.5f, %.5f".format(point.latitude, point.longitude)
                )
            }
            if (runtimeTelemetry.value.isSpoofingActive && !runtimeTelemetry.value.isRouteMoving) {
                MockLocationService.startStaticSpoof(getApplication(), labeled)
            } else {
                MockTelemetryController.setStaticCoordinate(labeled)
            }
            reverseGeocodePin(point)
        } else {
            val idx = _uiState.value.routeWaypoints.size + 1
            val wp = point.copy(label = "Waypoint #$idx")
            val updatedList = _uiState.value.routeWaypoints + wp
            _uiState.update {
                it.copy(
                    routeWaypoints = updatedList,
                    statusBannerMessage = "Added Waypoint #$idx to route"
                )
            }
            buildRouteFromWaypoints()
        }
    }

    private fun reverseGeocodePin(point: GeoPoint) {
        viewModelScope.launch {
            try {
                val place = GeoNetworkClient.nominatimApi.reverseGeocode(point.latitude, point.longitude)
                if (place.displayName.isNotBlank()) {
                    val label = place.shortTitle
                    _uiState.update { state ->
                        state.copy(selectedTargetPin = state.selectedTargetPin.copy(label = label))
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun setManualCoordinates(lat: Double, lon: Double, label: String = "Manual Coordinate") {
        val clampedLat = lat.coerceIn(-85.0, 85.0)
        val clampedLon = lon.coerceIn(-180.0, 180.0)
        val pt = GeoPoint(clampedLat, clampedLon, label)
        _uiState.update {
            it.copy(
                selectedTargetPin = pt,
                mapCenter = pt,
                statusBannerMessage = "Teleported pin to $label"
            )
        }
        if (runtimeTelemetry.value.isSpoofingActive && !runtimeTelemetry.value.isRouteMoving) {
            MockLocationService.startStaticSpoof(getApplication(), pt)
        } else {
            MockTelemetryController.setStaticCoordinate(pt)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _uiState.update { it.copy(isSearching = true) }
            try {
                val results = GeoNetworkClient.nominatimApi.searchPlaces(query.trim())
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            }
        }
    }

    fun selectSearchResult(place: NominatimPlace, addAsWaypoint: Boolean) {
        val pt = GeoPoint(place.latitude, place.longitude, place.shortTitle)
        if (addAsWaypoint) {
            val updated = _uiState.value.routeWaypoints + pt
            _uiState.update {
                it.copy(
                    routeWaypoints = updated,
                    mapCenter = pt,
                    searchQuery = "",
                    searchResults = emptyList(),
                    statusBannerMessage = "Added ${place.shortTitle} to route waypoints"
                )
            }
            buildRouteFromWaypoints()
        } else {
            _uiState.update {
                it.copy(
                    selectedTargetPin = pt,
                    mapCenter = pt,
                    searchQuery = "",
                    searchResults = emptyList(),
                    statusBannerMessage = "Selected ${place.shortTitle}"
                )
            }
            if (runtimeTelemetry.value.isSpoofingActive && !runtimeTelemetry.value.isRouteMoving) {
                MockLocationService.startStaticSpoof(getApplication(), pt)
            } else {
                MockTelemetryController.setStaticCoordinate(pt)
            }
        }
    }

    fun clearWaypoints() {
        _uiState.update {
            it.copy(
                routeWaypoints = emptyList(),
                generatedRoutePoints = emptyList(),
                routeDistanceMeters = 0.0,
                routeDurationSeconds = 0.0,
                statusBannerMessage = "Cleared all route waypoints"
            )
        }
    }

    fun removeWaypointAt(index: Int) {
        val current = _uiState.value.routeWaypoints.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uiState.update { it.copy(routeWaypoints = current) }
            buildRouteFromWaypoints()
        }
    }

    fun reverseWaypoints() {
        val reversed = _uiState.value.routeWaypoints.reversed()
        _uiState.update {
            it.copy(
                routeWaypoints = reversed,
                statusBannerMessage = "Reversed route direction"
            )
        }
        buildRouteFromWaypoints()
    }

    fun buildRouteFromWaypoints(autoStartSimulation: Boolean = false) {
        val waypoints = _uiState.value.routeWaypoints
        val profile = _uiState.value.movementProfile
        if (waypoints.size < 2) {
            _uiState.update {
                it.copy(
                    generatedRoutePoints = waypoints,
                    routeDistanceMeters = 0.0,
                    routeDurationSeconds = 0.0,
                    isRouteRoadSnapped = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRoutingLoading = true) }
            val result = GeoNetworkClient.fetchRoadRouteOrInterpolate(waypoints, profile)
            val effectiveKmh = _uiState.value.customSpeedKmh * _uiState.value.playbackSpeedMultiplier
            val speedMps = (effectiveKmh / 3.6).coerceAtLeast(0.5)
            val customDurationSec = result.distanceMeters / speedMps

            _uiState.update {
                it.copy(
                    generatedRoutePoints = result.points,
                    routeDistanceMeters = result.distanceMeters,
                    routeDurationSeconds = customDurationSec,
                    isRouteRoadSnapped = result.isRoadSnapped,
                    isRoutingLoading = false
                )
            }

            MockTelemetryController.configureRoute(
                points = result.points,
                speedKmh = _uiState.value.customSpeedKmh,
                loopMode = _uiState.value.loopMode
            )

            if (autoStartSimulation && !runtimeTelemetry.value.isSpoofingActive && result.points.size >= 2) {
                MockLocationService.startRouteSpoof(getApplication())
            }
        }
    }

    fun selectMovementProfile(profile: MovementProfile) {
        _uiState.update {
            it.copy(
                movementProfile = profile,
                customSpeedKmh = profile.defaultSpeedKmh
            )
        }
        MockTelemetryController.setSpeedKmh(profile.defaultSpeedKmh)
        buildRouteFromWaypoints()
    }

    fun setCustomSpeedKmh(speedKmh: Float) {
        val clamped = speedKmh.coerceIn(1.5f, 240f)
        val dist = _uiState.value.routeDistanceMeters
        val effectiveKmh = clamped * _uiState.value.playbackSpeedMultiplier
        val durationSec = dist / (effectiveKmh / 3.6).coerceAtLeast(0.5)
        _uiState.update {
            it.copy(
                customSpeedKmh = clamped,
                routeDurationSeconds = durationSec
            )
        }
        MockTelemetryController.setSpeedKmh(clamped)
    }

    fun setPlaybackSpeedMultiplier(multiplier: Float) {
        val clamped = multiplier.coerceIn(0.5f, 10.0f)
        val dist = _uiState.value.routeDistanceMeters
        val effectiveKmh = _uiState.value.customSpeedKmh * clamped
        val durationSec = dist / (effectiveKmh / 3.6).coerceAtLeast(0.5)
        _uiState.update {
            it.copy(
                playbackSpeedMultiplier = clamped,
                routeDurationSeconds = durationSec
            )
        }
        MockTelemetryController.setPlaybackSpeedMultiplier(clamped)
    }

    fun setLoopMode(mode: RouteLoopMode) {
        _uiState.update { it.copy(loopMode = mode) }
        MockTelemetryController.setLoopMode(mode)
    }

    fun toggleJoystickOverlay(enabled: Boolean) {
        _uiState.update { it.copy(showJoystickOverlay = enabled) }
    }

    fun onJoystickMoved(angleRadians: Float, intensity: Float) {
        MockTelemetryController.applyJoystickVector(
            angleRadians = angleRadians,
            intensity = intensity,
            maxSpeedKmh = _uiState.value.customSpeedKmh
        )
        val livePt = MockTelemetryController.state.value.currentPoint
        _uiState.update {
            it.copy(
                selectedTargetPin = livePt,
                mapCenter = livePt
            )
        }
    }

    fun startStaticSpoofing() {
        refreshMockPermissionCheck()
        val pin = _uiState.value.selectedTargetPin
        MockLocationService.startStaticSpoof(getApplication(), pin)
        _uiState.update {
            it.copy(statusBannerMessage = "Static GPS Spoofing active at ${pin.label.ifBlank { "Pinned Coordinate" }}")
        }
    }

    fun startRouteSimulation() {
        refreshMockPermissionCheck()
        val routePts = _uiState.value.generatedRoutePoints
        if (routePts.size < 2) {
            _uiState.update {
                it.copy(statusBannerMessage = "Add at least 2 waypoints to simulate route movement")
            }
            return
        }
        MockTelemetryController.configureRoute(
            points = routePts,
            speedKmh = _uiState.value.customSpeedKmh,
            loopMode = _uiState.value.loopMode
        )
        MockLocationService.startRouteSpoof(getApplication())
        _uiState.update {
            it.copy(
                mapCenter = routePts.first(),
                statusBannerMessage = "Simulating route movement at %.1f km/h".format(_uiState.value.customSpeedKmh)
            )
        }
    }

    fun stopAllSpoofing() {
        MockLocationService.stopSpoof(getApplication())
        _uiState.update {
            it.copy(statusBannerMessage = "Mock Location Spoofing terminated")
        }
    }

    fun togglePauseRoute() {
        MockTelemetryController.toggleRoutePause()
    }

    fun seekRouteProgress(fraction: Float) {
        MockTelemetryController.seekRouteFraction(fraction)
        val pt = MockTelemetryController.state.value.currentPoint
        _uiState.update { it.copy(mapCenter = pt) }
    }

    fun saveCurrentPinToPresets(customName: String, category: String = "Custom Pin") {
        val pin = _uiState.value.selectedTargetPin
        val name = customName.trim().ifEmpty { pin.label.ifEmpty { "Saved Coordinate" } }
        viewModelScope.launch {
            repository.saveLocation(
                SavedLocationEntity(
                    name = name,
                    subtitle = String.format("%.5f, %.5f", pin.latitude, pin.longitude),
                    latitude = pin.latitude,
                    longitude = pin.longitude,
                    category = category,
                    isFavorite = true
                )
            )
            _uiState.update { it.copy(statusBannerMessage = "Saved '$name' to presets") }
        }
    }

    fun saveCurrentRouteToPresets(customName: String) {
        val points = _uiState.value.generatedRoutePoints
        if (points.size < 2) return
        val wps = _uiState.value.routeWaypoints
        val startLabel = wps.firstOrNull()?.label?.ifBlank { "Origin" } ?: "Origin"
        val endLabel = wps.lastOrNull()?.label?.ifBlank { "Destination" } ?: "Destination"
        val name = customName.trim().ifEmpty { "$startLabel → $endLabel" }
        // Downsample up to 80 points for compact storage
        val step = (points.size / 80).coerceAtLeast(1)
        val compactPoints = points.filterIndexed { index, _ -> index % step == 0 || index == points.lastIndex }

        viewModelScope.launch {
            repository.saveRoute(
                SavedRouteEntity(
                    name = name,
                    startLabel = startLabel,
                    endLabel = endLabel,
                    encodedWaypoints = SavedRouteEntity.encodePoints(compactPoints),
                    totalDistanceMeters = _uiState.value.routeDistanceMeters.coerceAtLeast(
                        GeoMath.calculateTotalDistance(compactPoints)
                    ),
                    profileName = _uiState.value.movementProfile.name,
                    speedKmh = _uiState.value.customSpeedKmh
                )
            )
            _uiState.update { it.copy(statusBannerMessage = "Saved route '$name'") }
        }
    }

    fun loadPresetLocation(entity: SavedLocationEntity, deployImmediately: Boolean) {
        val pt = GeoPoint(entity.latitude, entity.longitude, entity.name)
        _uiState.update {
            it.copy(
                selectedTargetPin = pt,
                mapCenter = pt,
                selectedTab = MainTab.LIVE_MAP,
                statusBannerMessage = "Loaded preset: ${entity.name}"
            )
        }
        if (deployImmediately) {
            MockLocationService.startStaticSpoof(getApplication(), pt)
        } else {
            MockTelemetryController.setStaticCoordinate(pt)
        }
    }

    fun loadSavedRoute(entity: SavedRouteEntity, startImmediately: Boolean) {
        val pts = entity.toGeoPoints()
        if (pts.size < 2) return
        val profile = MovementProfile.entries.find { it.name == entity.profileName } ?: MovementProfile.SCOOTER
        val endpoints = listOf(
            pts.first().copy(label = entity.startLabel),
            pts.last().copy(label = entity.endLabel)
        )
        val durationSec = entity.totalDistanceMeters / (entity.speedKmh / 3.6).coerceAtLeast(0.5)

        _uiState.update {
            it.copy(
                routeWaypoints = endpoints,
                generatedRoutePoints = pts,
                routeDistanceMeters = entity.totalDistanceMeters,
                routeDurationSeconds = durationSec,
                movementProfile = profile,
                customSpeedKmh = entity.speedKmh,
                mapCenter = pts.first(),
                selectedTab = MainTab.LIVE_MAP,
                statusBannerMessage = "Loaded route: ${entity.name}"
            )
        }

        MockTelemetryController.configureRoute(
            points = pts,
            speedKmh = entity.speedKmh,
            loopMode = _uiState.value.loopMode
        )

        if (startImmediately) {
            MockLocationService.startRouteSpoof(getApplication())
        }
    }

    fun deletePresetLocation(id: Int) {
        viewModelScope.launch { repository.deleteLocation(id) }
    }

    fun deleteSavedRoute(id: Int) {
        viewModelScope.launch { repository.deleteRoute(id) }
    }

    fun applyTargetAppOptimization(appInfo: TargetAppInfo) {
        val profile = when (appInfo.id) {
            "snapchat" -> MovementProfile.WALK
            "telegram" -> MovementProfile.SCOOTER
            "whatsapp" -> MovementProfile.DRIVE
            else -> MovementProfile.SCOOTER
        }
        MockTelemetryController.setJitterEnabled(appInfo.jitterRecommended)
        MockTelemetryController.setAltitudeWaveEnabled(true)
        selectMovementProfile(profile)
        _uiState.update {
            it.copy(
                activeTargetAppId = appInfo.id,
                statusBannerMessage = "Optimized telemetry profile for ${appInfo.name}"
            )
        }
    }

    fun dismissBannerMessage() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }
}
