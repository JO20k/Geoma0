package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.data.GeoPoint
import com.example.ui.theme.AmberRoute
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.DeepSpaceBg
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.TacticalBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

private object OsmTileLoader {
    val cache = mutableStateMapOf<String, ImageBitmap>()
    private val loadingKeys = mutableSetOf<String>()
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun loadTileIfNeeded(z: Int, x: Int, y: Int) {
        val maxTile = 1 shl z
        val wrappedX = ((x % maxTile) + maxTile) % maxTile
        if (y !in 0 until maxTile) return
        val key = "$z/$wrappedX/$y"
        if (cache.containsKey(key)) return
        synchronized(loadingKeys) {
            if (loadingKeys.contains(key)) return
            loadingKeys.add(key)
        }
        withContext(Dispatchers.IO) {
            try {
                // Dark CartoDB / OSM fallback tiles for crisp dark-mode street topology
                val url = "https://a.basemaps.cartocdn.com/dark_all/$z/$wrappedX/$y.png"
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "GeoPulseAndroidSpoofer/1.0")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bytes = resp.body?.bytes()
                        if (bytes != null) {
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bmp != null) {
                                withContext(Dispatchers.Main) {
                                    cache[key] = bmp.asImageBitmap()
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                synchronized(loadingKeys) {
                    loadingKeys.remove(key)
                }
            }
        }
    }
}

object WebMercator {
    private const val TILE_SIZE = 256.0

    fun latLonToWorldPixel(lat: Double, lon: Double, zoom: Int): Pair<Double, Double> {
        val scale = TILE_SIZE * (1 shl zoom).toDouble()
        val x = (lon + 180.0) / 360.0 * scale
        val latRad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
        val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * scale
        return x to y
    }

    fun worldPixelToLatLon(px: Double, py: Double, zoom: Int): GeoPoint {
        val scale = TILE_SIZE * (1 shl zoom).toDouble()
        val lon = (px / scale) * 360.0 - 180.0
        val n = PI - 2.0 * PI * (py / scale)
        val lat = Math.toDegrees(atan(0.5 * (exp(n) - exp(-n))))
        return GeoPoint(
            latitude = lat.coerceIn(-85.0, 85.0),
            longitude = ((lon + 540.0) % 360.0) - 180.0
        )
    }
}

@Composable
fun InteractiveSpoofMapCanvas(
    center: GeoPoint,
    zoom: Int,
    selectedPin: GeoPoint,
    activeLivePoint: GeoPoint,
    activeBearingDegrees: Float,
    isSpoofingActive: Boolean,
    waypoints: List<GeoPoint>,
    routePoints: List<GeoPoint>,
    onMapTap: (GeoPoint) -> Unit,
    onMapPan: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentCenter by rememberUpdatedState(center)
    val currentZoom by rememberUpdatedState(zoom)
    val currentOnTap by rememberUpdatedState(onMapTap)
    val currentOnPan by rememberUpdatedState(onMapPan)
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
    val pulseRadiusFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_ring"
    )

    // Preload visible tiles whenever center or zoom changes
    LaunchedEffect(center.latitude, center.longitude, zoom) {
        val (cx, cy) = WebMercator.latLonToWorldPixel(center.latitude, center.longitude, zoom)
        val centerTileX = floor(cx / 256.0).toInt()
        val centerTileY = floor(cy / 256.0).toInt()
        for (dx in -2..2) {
            for (dy in -3..3) {
                scope.launch {
                    OsmTileLoader.loadTileIfNeeded(zoom, centerTileX + dx, centerTileY + dy)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpaceBg)
            .testTag("interactive_spoof_map")
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val (centerWorldX, centerWorldY) = WebMercator.latLonToWorldPixel(
                        currentCenter.latitude,
                        currentCenter.longitude,
                        currentZoom
                    )
                    val tapWorldX = centerWorldX + (offset.x - size.width / 2f)
                    val tapWorldY = centerWorldY + (offset.y - size.height / 2f)
                    val tappedGeo = WebMercator.worldPixelToLatLon(tapWorldX, tapWorldY, currentZoom)
                    currentOnTap(tappedGeo)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val (centerWorldX, centerWorldY) = WebMercator.latLonToWorldPixel(
                        currentCenter.latitude,
                        currentCenter.longitude,
                        currentZoom
                    )
                    val newWorldX = centerWorldX - dragAmount.x
                    val newWorldY = centerWorldY - dragAmount.y
                    val newCenter = WebMercator.worldPixelToLatLon(newWorldX, newWorldY, currentZoom)
                    currentOnPan(newCenter)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val (centerWorldX, centerWorldY) = WebMercator.latLonToWorldPixel(
                center.latitude,
                center.longitude,
                zoom
            )

            // 1. Draw Tactical Grid Background Fallback
            drawTacticalCoordinateGrid(centerWorldX, centerWorldY)

            // 2. Draw Cached OSM / Carto Dark Tiles
            val startTileX = floor((centerWorldX - canvasWidth / 2.0) / 256.0).toInt() - 1
            val endTileX = floor((centerWorldX + canvasWidth / 2.0) / 256.0).toInt() + 1
            val startTileY = floor((centerWorldY - canvasHeight / 2.0) / 256.0).toInt() - 1
            val endTileY = floor((centerWorldY + canvasHeight / 2.0) / 256.0).toInt() + 1
            val maxTile = 1 shl zoom

            for (tx in startTileX..endTileX) {
                for (ty in startTileY..endTileY) {
                    if (ty in 0 until maxTile) {
                        val wrappedTx = ((tx % maxTile) + maxTile) % maxTile
                        val key = "$zoom/$wrappedTx/$ty"
                        val bitmap = OsmTileLoader.cache[key]
                        val screenX = (tx * 256.0 - centerWorldX + canvasWidth / 2.0).toFloat()
                        val screenY = (ty * 256.0 - centerWorldY + canvasHeight / 2.0).toFloat()
                        if (bitmap != null) {
                            drawImage(
                                image = bitmap,
                                topLeft = Offset(screenX, screenY)
                            )
                        }
                    }
                }
            }

            // Helper to project GeoPoint to Canvas Offset
            fun project(pt: GeoPoint): Offset {
                val (wx, wy) = WebMercator.latLonToWorldPixel(pt.latitude, pt.longitude, zoom)
                return Offset(
                    x = (wx - centerWorldX + canvasWidth / 2.0).toFloat(),
                    y = (wy - centerWorldY + canvasHeight / 2.0).toFloat()
                )
            }

            // 3. Draw Generated Route Polyline
            if (routePoints.size >= 2) {
                val routePath = Path()
                routePoints.forEachIndexed { index, pt ->
                    val screenPt = project(pt)
                    if (index == 0) routePath.moveTo(screenPt.x, screenPt.y)
                    else routePath.lineTo(screenPt.x, screenPt.y)
                }

                // Outer glow
                drawPath(
                    path = routePath,
                    color = CyanPulse.copy(alpha = 0.28f),
                    style = Stroke(width = 20f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Core route line
                drawPath(
                    path = routePath,
                    color = CyanPulse,
                    style = Stroke(width = 7.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Animated directional dashes
                drawPath(
                    path = routePath,
                    color = Color.White.copy(alpha = 0.75f),
                    style = Stroke(
                        width = 3f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(14f, 18f),
                            phase = (1f - pulseRadiusFraction) * 64f
                        )
                    )
                )
            }

            // 4. Draw Waypoint Nodes
            waypoints.forEachIndexed { idx, wp ->
                val wpOffset = project(wp)
                val nodeColor = when (idx) {
                    0 -> EmeraldLive
                    waypoints.lastIndex -> AmberRoute
                    else -> CyanPulse
                }
                drawCircle(
                    color = nodeColor.copy(alpha = 0.3f),
                    radius = 22f,
                    center = wpOffset
                )
                drawCircle(
                    color = DeepSpaceBg,
                    radius = 12f,
                    center = wpOffset
                )
                drawCircle(
                    color = nodeColor,
                    radius = 8.5f,
                    center = wpOffset
                )
            }

            // 5. Draw Selected Target Pin (when different or in static mode)
            val pinOffset = project(selectedPin)
            drawCircle(
                color = AmberRoute.copy(alpha = 0.24f),
                radius = 30f,
                center = pinOffset
            )
            drawCircle(
                color = AmberRoute,
                radius = 16f,
                center = pinOffset,
                style = Stroke(width = 3.5f)
            )
            drawCircle(
                color = AmberRoute,
                radius = 5.5f,
                center = pinOffset
            )

            // 6. Draw Live Spoofed Position Beacon (Green/Cyan Pulsing Radar + Heading Cone)
            val liveOffset = if (isSpoofingActive) project(activeLivePoint) else pinOffset
            val beaconColor = if (isSpoofingActive) EmeraldLive else CyanPulse

            // Expanding sonar pulse ring
            drawCircle(
                color = beaconColor.copy(alpha = (1f - pulseRadiusFraction) * 0.55f),
                radius = 22f + pulseRadiusFraction * 78f,
                center = liveOffset
            )
            drawCircle(
                color = beaconColor.copy(alpha = (1f - pulseRadiusFraction) * 0.85f),
                radius = 22f + pulseRadiusFraction * 78f,
                center = liveOffset,
                style = Stroke(width = 2.5f)
            )

            // Directional heading cone
            rotate(degrees = activeBearingDegrees, pivot = liveOffset) {
                val conePath = Path().apply {
                    moveTo(liveOffset.x, liveOffset.y)
                    lineTo(liveOffset.x - 26f, liveOffset.y - 68f)
                    lineTo(liveOffset.x + 26f, liveOffset.y - 68f)
                    close()
                }
                drawPath(
                    path = conePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, beaconColor.copy(alpha = 0.45f)),
                        startY = liveOffset.y - 70f,
                        endY = liveOffset.y
                    )
                )
            }

            // Inner GPS precision dot
            drawCircle(
                color = Color.White,
                radius = 13f,
                center = liveOffset
            )
            drawCircle(
                color = beaconColor,
                radius = 9.5f,
                center = liveOffset
            )

            // 7. Subtle Center Reticle Crosshair
            val cx = canvasWidth / 2f
            val cy = canvasHeight / 2f
            drawCircle(
                color = CyanPulse.copy(alpha = 0.35f),
                radius = 18f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )
            drawLine(
                color = CyanPulse.copy(alpha = 0.4f),
                start = Offset(cx - 26f, cy),
                end = Offset(cx - 10f, cy),
                strokeWidth = 1.5f
            )
            drawLine(
                color = CyanPulse.copy(alpha = 0.4f),
                start = Offset(cx + 10f, cy),
                end = Offset(cx + 26f, cy),
                strokeWidth = 1.5f
            )
            drawLine(
                color = CyanPulse.copy(alpha = 0.4f),
                start = Offset(cx, cy - 26f),
                end = Offset(cx, cy - 10f),
                strokeWidth = 1.5f
            )
            drawLine(
                color = CyanPulse.copy(alpha = 0.4f),
                start = Offset(cx, cy + 10f),
                end = Offset(cx, cy + 26f),
                strokeWidth = 1.5f
            )
        }
    }
}

private fun DrawScope.drawTacticalCoordinateGrid(centerWorldX: Double, centerWorldY: Double) {
    val gridSpacing = 64f
    val offsetX = ((-centerWorldX) % gridSpacing).toFloat()
    val offsetY = ((-centerWorldY) % gridSpacing).toFloat()

    var x = offsetX
    while (x < size.width) {
        if (x >= 0f) {
            drawLine(
                color = TacticalBorder.copy(alpha = 0.45f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
        }
        x += gridSpacing
    }

    var y = offsetY
    while (y < size.height) {
        if (y >= 0f) {
            drawLine(
                color = TacticalBorder.copy(alpha = 0.45f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }
        y += gridSpacing
    }

    // Subtle radial radar vignette
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                CyanPulse.copy(alpha = 0.05f),
                DeepSpaceBg.copy(alpha = 0.65f)
            ),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.maxDimension * 0.7f
        ),
        size = Size(size.width, size.height)
    )
}
