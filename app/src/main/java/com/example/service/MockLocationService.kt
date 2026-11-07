package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.GeoMath
import com.example.data.GeoPoint
import com.example.data.RouteLoopMode
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class TelemetryRuntimeState(
    val isSpoofingActive: Boolean = false,
    val isRouteMoving: Boolean = false,
    val isRoutePaused: Boolean = false,
    val systemMockPermissionGranted: Boolean = false,
    val activeProviderStatus: String = "STANDBY",
    val currentPoint: GeoPoint = GeoPoint(35.6595, 139.7004, "Shibuya Crossing"),
    val currentBearingDegrees: Float = 0f,
    val currentSpeedKmh: Float = 0f,
    val playbackSpeedMultiplier: Float = 1.0f,
    val currentAltitudeMeters: Double = 42.5,
    val currentAccuracyMeters: Float = 3.2f,
    val distanceTraveledMeters: Double = 0.0,
    val totalRouteDistanceMeters: Double = 0.0,
    val routeProgressFraction: Float = 0f,
    val activeSegmentIndex: Int = 0,
    val packetsInjectedCount: Long = 0L,
    val lastInjectionTimestampMs: Long = 0L,
    val jitterEnabled: Boolean = true,
    val altitudeWaveEnabled: Boolean = true,
    val loopMode: RouteLoopMode = RouteLoopMode.PING_PONG,
    val lastSystemMessage: String = "Ready to deploy mock coordinates"
)

object MockTelemetryController {
    private val _state = MutableStateFlow(TelemetryRuntimeState())
    val state: StateFlow<TelemetryRuntimeState> = _state.asStateFlow()

    private var activeRoutePoints: List<GeoPoint> = emptyList()
    private var targetSpeedKmh: Float = 18.5f
    private var playbackMultiplier: Float = 1.0f
    private var pingPongForward: Boolean = true

    fun updateState(transform: (TelemetryRuntimeState) -> TelemetryRuntimeState) {
        _state.update(transform)
    }

    fun setStaticCoordinate(point: GeoPoint) {
        _state.update {
            it.copy(
                currentPoint = point,
                isRouteMoving = false,
                isRoutePaused = false,
                currentSpeedKmh = if (it.isSpoofingActive) 0.4f else 0f
            )
        }
    }

    fun applyJoystickVector(angleRadians: Float, intensity: Float, maxSpeedKmh: Float) {
        val current = _state.value.currentPoint
        val effectiveMaxKmh = maxSpeedKmh * playbackMultiplier
        val speedMps = (effectiveMaxKmh / 3.6) * intensity.coerceIn(0f, 1f)
        // Step for ~180ms joystick tick
        val stepMeters = speedMps * 0.18
        val north = cos(angleRadians.toDouble()) * stepMeters
        val east = sin(angleRadians.toDouble()) * stepMeters
        val next = GeoMath.offsetByMeters(current, north, east)
        val bearing = ((Math.toDegrees(angleRadians.toDouble()) + 360.0) % 360.0).toFloat()

        _state.update {
            it.copy(
                currentPoint = next,
                currentBearingDegrees = bearing,
                currentSpeedKmh = (speedMps * 3.6).toFloat(),
                isRoutePaused = if (it.isRouteMoving) true else it.isRoutePaused
            )
        }
    }

    fun configureRoute(points: List<GeoPoint>, speedKmh: Float, loopMode: RouteLoopMode) {
        activeRoutePoints = points
        targetSpeedKmh = speedKmh
        pingPongForward = true
        val total = GeoMath.calculateTotalDistance(points)
        _state.update {
            it.copy(
                totalRouteDistanceMeters = total,
                distanceTraveledMeters = 0.0,
                routeProgressFraction = 0f,
                loopMode = loopMode,
                currentPoint = points.firstOrNull() ?: it.currentPoint
            )
        }
    }

    fun setSpeedKmh(speedKmh: Float) {
        targetSpeedKmh = speedKmh.coerceIn(1f, 300f)
        _state.update {
            val effectiveSpeed = targetSpeedKmh * playbackMultiplier
            it.copy(
                currentSpeedKmh = if (it.isRouteMoving && !it.isRoutePaused) effectiveSpeed else it.currentSpeedKmh
            )
        }
    }

    fun setPlaybackSpeedMultiplier(multiplier: Float) {
        val clamped = multiplier.coerceIn(0.5f, 10.0f)
        playbackMultiplier = clamped
        _state.update {
            val effectiveSpeed = targetSpeedKmh * clamped
            it.copy(
                playbackSpeedMultiplier = clamped,
                currentSpeedKmh = if (it.isRouteMoving && !it.isRoutePaused) effectiveSpeed else it.currentSpeedKmh
            )
        }
    }

    fun setLoopMode(mode: RouteLoopMode) {
        _state.update { it.copy(loopMode = mode) }
    }

    fun setJitterEnabled(enabled: Boolean) {
        _state.update { it.copy(jitterEnabled = enabled) }
    }

    fun setAltitudeWaveEnabled(enabled: Boolean) {
        _state.update { it.copy(altitudeWaveEnabled = enabled) }
    }

    fun toggleRoutePause() {
        _state.update {
            val nowPaused = !it.isRoutePaused
            it.copy(
                isRoutePaused = nowPaused,
                currentSpeedKmh = if (nowPaused) 0f else getEffectiveSpeedKmh(),
                lastSystemMessage = if (nowPaused) "Route simulation paused (Holding GPS lock)" else "Route simulation resumed"
            )
        }
    }

    fun seekRouteFraction(fraction: Float) {
        val points = activeRoutePoints
        if (points.size < 2) return
        val total = GeoMath.calculateTotalDistance(points).coerceAtLeast(1.0)
        val newDist = total * fraction.coerceIn(0f, 1f)
        val sample = GeoMath.sampleAlongRoute(points, newDist)
        _state.update {
            it.copy(
                distanceTraveledMeters = newDist,
                routeProgressFraction = sample.progressFraction,
                currentPoint = sample.point,
                currentBearingDegrees = sample.bearingDegrees,
                activeSegmentIndex = sample.segmentIndex
            )
        }
    }

    internal fun getRoutePoints(): List<GeoPoint> = activeRoutePoints
    internal fun getTargetSpeedKmh(): Float = targetSpeedKmh
    internal fun getPlaybackMultiplier(): Float = playbackMultiplier
    internal fun getEffectiveSpeedKmh(): Float = (targetSpeedKmh * playbackMultiplier).coerceIn(0.5f, 1200f)
    internal fun isPingPongForward(): Boolean = pingPongForward
    internal fun setPingPongForward(forward: Boolean) {
        pingPongForward = forward
    }
}

class MockLocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var simulationJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSpoofingEngine()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_STATIC -> {
                val lat = intent.getDoubleExtra(EXTRA_LAT, 35.6595)
                val lon = intent.getDoubleExtra(EXTRA_LON, 139.7004)
                val label = intent.getStringExtra(EXTRA_LABEL) ?: "Pinned Coordinate"
                MockTelemetryController.setStaticCoordinate(GeoPoint(lat, lon, label))
                startForegroundWithNotification()
                startSpoofingLoop(moveAlongRoute = false)
            }
            ACTION_START_ROUTE -> {
                startForegroundWithNotification()
                startSpoofingLoop(moveAlongRoute = true)
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification(MockTelemetryController.state.value)
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // In case foreground service permission is restricted on specific Android builds
        }
    }

    private fun startSpoofingLoop(moveAlongRoute: Boolean) {
        simulationJob?.cancel()
        MockTelemetryController.updateState {
            it.copy(
                isSpoofingActive = true,
                isRouteMoving = moveAlongRoute,
                isRoutePaused = false,
                currentSpeedKmh = if (moveAlongRoute) MockTelemetryController.getEffectiveSpeedKmh() else 0.3f,
                lastSystemMessage = if (moveAlongRoute) {
                    "Injecting live route movement into GPS & Fused providers"
                } else {
                    "Injecting static coordinate lock into GPS & Fused providers"
                }
            )
        }

        simulationJob = serviceScope.launch {
            val tickMs = 350L
            var phase = 0.0
            setupAndroidTestProviders()

            while (isActive) {
                val currentState = MockTelemetryController.state.value
                phase += 0.25

                if (currentState.isRouteMoving && !currentState.isRoutePaused) {
                    val points = MockTelemetryController.getRoutePoints()
                    val totalDist = currentState.totalRouteDistanceMeters
                    if (points.size >= 2 && totalDist > 1.0) {
                        val effectiveKmh = MockTelemetryController.getEffectiveSpeedKmh()
                        val speedMps = (effectiveKmh / 3.6).coerceAtLeast(0.5)
                        val deltaMeters = speedMps * (tickMs / 1000.0)
                        val forward = MockTelemetryController.isPingPongForward()
                        var nextDist = if (forward) {
                            currentState.distanceTraveledMeters + deltaMeters
                        } else {
                            currentState.distanceTraveledMeters - deltaMeters
                        }

                        when (currentState.loopMode) {
                            RouteLoopMode.ONE_WAY -> {
                                if (nextDist >= totalDist) {
                                    nextDist = totalDist
                                    MockTelemetryController.updateState {
                                        it.copy(
                                            isRoutePaused = true,
                                            currentSpeedKmh = 0f,
                                            lastSystemMessage = "Destination reached • Holding position at endpoint"
                                        )
                                    }
                                }
                            }
                            RouteLoopMode.PING_PONG -> {
                                if (nextDist >= totalDist) {
                                    nextDist = totalDist
                                    MockTelemetryController.setPingPongForward(false)
                                } else if (nextDist <= 0.0) {
                                    nextDist = 0.0
                                    MockTelemetryController.setPingPongForward(true)
                                }
                            }
                            RouteLoopMode.CIRCUIT -> {
                                if (nextDist >= totalDist) {
                                    nextDist %= totalDist
                                }
                            }
                        }

                        val sample = GeoMath.sampleAlongRoute(points, nextDist)
                        val effectiveBearing = if (MockTelemetryController.isPingPongForward()) {
                            sample.bearingDegrees
                        } else {
                            (sample.bearingDegrees + 180f) % 360f
                        }

                        MockTelemetryController.updateState {
                            it.copy(
                                currentPoint = sample.point,
                                currentBearingDegrees = effectiveBearing,
                                currentSpeedKmh = effectiveKmh,
                                distanceTraveledMeters = nextDist,
                                routeProgressFraction = sample.progressFraction,
                                activeSegmentIndex = sample.segmentIndex
                            )
                        }
                    }
                }

                val updated = MockTelemetryController.state.value
                // Apply realistic micro-jitter so Snapchat/Telegram/WhatsApp see organic sensor readings
                val jitterMetersNorth = if (updated.jitterEnabled) Random.Default.nextDouble(-0.85, 0.85) else 0.0
                val jitterMetersEast = if (updated.jitterEnabled) Random.Default.nextDouble(-0.85, 0.85) else 0.0
                val injectedPoint = GeoMath.offsetByMeters(
                    updated.currentPoint,
                    jitterMetersNorth,
                    jitterMetersEast
                )

                val altitude = if (updated.altitudeWaveEnabled) {
                    42.0 + sin(phase * 0.4) * 2.8 + Random.Default.nextDouble(-0.2, 0.2)
                } else {
                    42.0
                }
                val accuracy = if (updated.jitterEnabled) {
                    (2.8f + Random.Default.nextFloat() * 1.6f)
                } else {
                    3.0f
                }

                val systemAccepted = pushMockLocationToSystem(
                    point = injectedPoint,
                    bearing = updated.currentBearingDegrees,
                    speedMps = (updated.currentSpeedKmh / 3.6f),
                    altitude = altitude,
                    accuracy = accuracy
                )

                MockTelemetryController.updateState {
                    it.copy(
                        systemMockPermissionGranted = systemAccepted,
                        activeProviderStatus = if (systemAccepted) {
                            "KERNEL MOCK + TELEMETRY LIVE"
                        } else {
                            "VIRTUAL STREAM (ENABLE MOCK APP IN DEV OPTIONS FOR OS LOCK)"
                        },
                        currentAltitudeMeters = altitude,
                        currentAccuracyMeters = accuracy,
                        packetsInjectedCount = it.packetsInjectedCount + 1,
                        lastInjectionTimestampMs = System.currentTimeMillis()
                    )
                }

                if (updated.packetsInjectedCount % 6L == 0L) {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, buildNotification(MockTelemetryController.state.value))
                }

                delay(tickMs)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupAndroidTestProviders(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var success = false
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            try {
                try {
                    lm.removeTestProvider(provider)
                } catch (_: Exception) {
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val props = ProviderProperties.Builder()
                        .setHasNetworkRequirement(false)
                        .setHasSatelliteRequirement(true)
                        .setHasCellRequirement(false)
                        .setHasMonetaryCost(false)
                        .setHasAltitudeSupport(true)
                        .setHasSpeedSupport(true)
                        .setHasBearingSupport(true)
                        .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                        .setAccuracy(ProviderProperties.ACCURACY_FINE)
                        .build()
                    lm.addTestProvider(provider, props)
                } else {
                    @Suppress("DEPRECATION")
                    lm.addTestProvider(
                        provider,
                        false,
                        true,
                        false,
                        false,
                        true,
                        true,
                        true,
                        Criteria.POWER_LOW,
                        Criteria.ACCURACY_FINE
                    )
                }
                lm.setTestProviderEnabled(provider, true)
                success = true
            } catch (_: SecurityException) {
                // App not selected as Mock Location App in Developer Options yet
            } catch (_: IllegalArgumentException) {
            }
        }
        return success
    }

    @SuppressLint("MissingPermission")
    private fun pushMockLocationToSystem(
        point: GeoPoint,
        bearing: Float,
        speedMps: Float,
        altitude: Double,
        accuracy: Float
    ): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var injectedAny = false
        val nowWall = System.currentTimeMillis()
        val nowElapsedNanos = SystemClock.elapsedRealtimeNanos()

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            try {
                val mockLoc = Location(provider).apply {
                    latitude = point.latitude
                    longitude = point.longitude
                    this.altitude = altitude
                    this.bearing = bearing
                    this.speed = speedMps
                    this.accuracy = accuracy
                    time = nowWall
                    elapsedRealtimeNanos = nowElapsedNanos
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        bearingAccuracyDegrees = 2.5f
                        speedAccuracyMetersPerSecond = 0.25f
                        verticalAccuracyMeters = 1.8f
                    }
                }
                lm.setTestProviderLocation(provider, mockLoc)
                injectedAny = true
            } catch (_: SecurityException) {
            } catch (_: IllegalArgumentException) {
            }
        }

        // Also inject into Google Play Services FusedLocationProviderClient for Snapchat, WhatsApp, and Telegram
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(this)
            fusedClient.setMockMode(true)
            val fusedLoc = Location(LocationManager.GPS_PROVIDER).apply {
                latitude = point.latitude
                longitude = point.longitude
                this.altitude = altitude
                this.bearing = bearing
                this.speed = speedMps
                this.accuracy = accuracy
                time = nowWall
                elapsedRealtimeNanos = nowElapsedNanos
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    bearingAccuracyDegrees = 2.0f
                    speedAccuracyMetersPerSecond = 0.2f
                    verticalAccuracyMeters = 1.5f
                }
            }
            fusedClient.setMockLocation(fusedLoc)
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }

        return injectedAny
    }

    @SuppressLint("MissingPermission")
    private fun stopSpoofingEngine() {
        simulationJob?.cancel()
        simulationJob = null

        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            try {
                lm.setTestProviderEnabled(provider, false)
                lm.removeTestProvider(provider)
            } catch (_: Exception) {
            }
        }
        try {
            LocationServices.getFusedLocationProviderClient(this).setMockMode(false)
        } catch (_: Exception) {
        }

        MockTelemetryController.updateState {
            it.copy(
                isSpoofingActive = false,
                isRouteMoving = false,
                isRoutePaused = false,
                currentSpeedKmh = 0f,
                activeProviderStatus = "STANDBY",
                lastSystemMessage = "Spoofing stopped • Restored real hardware GPS"
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GeoPulse Mock Location Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active GPS & Fused Location spoofing feed"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(state: TelemetryRuntimeState): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MockLocationService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val coordFormatted = String.format(
            Locale.US,
            "%.5f, %.5f • %.1f km/h",
            state.currentPoint.latitude,
            state.currentPoint.longitude,
            state.currentSpeedKmh
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("GeoPulse Live Spoofer Active")
            .setContentText(coordFormatted)
            .setOngoing(true)
            .setContentIntent(pendingOpen)
            .addAction(0, "Stop Spoofing", pendingStop)
            .build()
    }

    override fun onDestroy() {
        stopSpoofingEngine()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "geopulse_spoof_channel"
        const val NOTIFICATION_ID = 4092
        const val ACTION_START_STATIC = "com.example.action.START_STATIC"
        const val ACTION_START_ROUTE = "com.example.action.START_ROUTE"
        const val ACTION_STOP = "com.example.action.STOP"
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LON = "extra_lon"
        const val EXTRA_LABEL = "extra_label"

        fun startStaticSpoof(context: Context, point: GeoPoint) {
            val intent = Intent(context, MockLocationService::class.java).apply {
                action = ACTION_START_STATIC
                putExtra(EXTRA_LAT, point.latitude)
                putExtra(EXTRA_LON, point.longitude)
                putExtra(EXTRA_LABEL, point.label)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startRouteSpoof(context: Context) {
            val intent = Intent(context, MockLocationService::class.java).apply {
                action = ACTION_START_ROUTE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopSpoof(context: Context) {
            val intent = Intent(context, MockLocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
