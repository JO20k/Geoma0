package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class NominatimPlace(
    @Json(name = "place_id") val placeId: Long = 0L,
    @Json(name = "display_name") val displayName: String = "",
    @Json(name = "lat") val lat: String = "0.0",
    @Json(name = "lon") val lon: String = "0.0",
    @Json(name = "type") val type: String? = null
) {
    val latitude: Double get() = lat.toDoubleOrNull() ?: 0.0
    val longitude: Double get() = lon.toDoubleOrNull() ?: 0.0
    val shortTitle: String
        get() = displayName.split(",").firstOrNull()?.trim()?.ifEmpty { "Coordinate Pin" }
            ?: "Coordinate Pin"
    val subtitle: String
        get() = displayName.split(",").drop(1).take(2).joinToString(", ").trim()
}

data class OsrmRouteResponse(
    @Json(name = "code") val code: String = "",
    @Json(name = "routes") val routes: List<OsrmRoute> = emptyList()
)

data class OsrmRoute(
    @Json(name = "distance") val distance: Double = 0.0,
    @Json(name = "duration") val duration: Double = 0.0,
    @Json(name = "geometry") val geometry: OsrmGeometry? = null
)

data class OsrmGeometry(
    @Json(name = "coordinates") val coordinates: List<List<Double>> = emptyList()
)

interface NominatimApi {
    @GET("search")
    suspend fun searchPlaces(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 6,
        @Query("addressdetails") addressDetails: Int = 1
    ): List<NominatimPlace>

    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json"
    ): NominatimPlace
}

interface OsrmApi {
    @GET("route/v1/{profile}/{coordinates}")
    suspend fun getRoute(
        @Path("profile") profile: String,
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson"
    ): OsrmRouteResponse
}

data class GeneratedRouteResult(
    val points: List<GeoPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val isRoadSnapped: Boolean
)

object GeoNetworkClient {
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req: Request = chain.request().newBuilder()
                    .header("User-Agent", "GeoPulseAndroidSpoofer/1.0 (Telemetry Suite)")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .build()
                chain.proceed(req)
            }
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val nominatimApi: NominatimApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NominatimApi::class.java)
    }

    val osrmApi: OsrmApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OsrmApi::class.java)
    }

    suspend fun fetchRoadRouteOrInterpolate(
        waypoints: List<GeoPoint>,
        profile: MovementProfile
    ): GeneratedRouteResult = withContext(Dispatchers.IO) {
        if (waypoints.size < 2) {
            return@withContext GeneratedRouteResult(
                points = waypoints,
                distanceMeters = 0.0,
                durationSeconds = 0.0,
                isRoadSnapped = false
            )
        }

        val coordString = waypoints.joinToString(";") { "${it.longitude},${it.latitude}" }
        try {
            val response = osrmApi.getRoute(
                profile = profile.osrmProfile,
                coordinates = coordString
            )
            val bestRoute = response.routes.firstOrNull()
            val coords = bestRoute?.geometry?.coordinates.orEmpty()
            if (response.code == "Ok" && coords.size >= 2) {
                val points = coords.mapNotNull { pair ->
                    if (pair.size >= 2) {
                        GeoPoint(latitude = pair[1], longitude = pair[0])
                    } else null
                }
                if (points.size >= 2) {
                    return@withContext GeneratedRouteResult(
                        points = points,
                        distanceMeters = bestRoute?.distance ?: GeoMath.calculateTotalDistance(points),
                        durationSeconds = bestRoute?.duration ?: 0.0,
                        isRoadSnapped = true
                    )
                }
            }
        } catch (_: Exception) {
            // Fall back to high-density great-circle curve interpolation if offline or rate-limited
        }

        val densePoints = GeoMath.interpolateWaypoints(waypoints, stepMeters = 18.0)
        val totalDist = GeoMath.calculateTotalDistance(densePoints)
        val speedMps = (profile.defaultSpeedKmh / 3.6).coerceAtLeast(1.0)
        GeneratedRouteResult(
            points = densePoints,
            distanceMeters = totalDist,
            durationSeconds = totalDist / speedMps,
            isRoadSnapped = false
        )
    }
}

object GeoMath {
    private const val EARTH_RADIUS_METERS = 6371000.0

    fun haversineMeters(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)

        val sinDLat = sin(dLat / 2)
        val sinDLon = sin(dLon / 2)
        val h = sinDLat * sinDLat + cos(lat1) * cos(lat2) * sinDLon * sinDLon
        val c = 2 * atan2(sqrt(h), sqrt((1 - h).coerceAtLeast(0.0)))
        return EARTH_RADIUS_METERS * c
    }

    fun bearingDegrees(a: GeoPoint, b: GeoPoint): Float {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val brng = Math.toDegrees(atan2(y, x))
        return ((brng + 360.0) % 360.0).toFloat()
    }

    fun offsetByMeters(origin: GeoPoint, northMeters: Double, eastMeters: Double): GeoPoint {
        val dLat = northMeters / EARTH_RADIUS_METERS
        val dLon = eastMeters / (EARTH_RADIUS_METERS * cos(Math.toRadians(origin.latitude)).coerceAtLeast(0.0001))
        val newLat = (origin.latitude + Math.toDegrees(dLat)).coerceIn(-85.0, 85.0)
        val newLon = ((origin.longitude + Math.toDegrees(dLon) + 540.0) % 360.0) - 180.0
        return GeoPoint(newLat, newLon, origin.label)
    }

    fun calculateTotalDistance(points: List<GeoPoint>): Double {
        if (points.size < 2) return 0.0
        var sum = 0.0
        for (i in 0 until points.size - 1) {
            sum += haversineMeters(points[i], points[i + 1])
        }
        return sum
    }

    fun interpolateWaypoints(waypoints: List<GeoPoint>, stepMeters: Double = 15.0): List<GeoPoint> {
        if (waypoints.size < 2) return waypoints
        val result = mutableListOf<GeoPoint>()
        for (i in 0 until waypoints.size - 1) {
            val start = waypoints[i]
            val end = waypoints[i + 1]
            val segmentDist = haversineMeters(start, end)
            val steps = (segmentDist / stepMeters).toInt().coerceIn(1, 250)
            for (s in 0 until steps) {
                val fraction = s.toDouble() / steps.toDouble()
                val lat = start.latitude + (end.latitude - start.latitude) * fraction
                val lon = start.longitude + (end.longitude - start.longitude) * fraction
                result.add(GeoPoint(lat, lon))
            }
        }
        result.add(waypoints.last())
        return result
    }

    fun sampleAlongRoute(points: List<GeoPoint>, distanceAlongMeters: Double): RouteSample {
        if (points.isEmpty()) {
            return RouteSample(GeoPoint(37.7749, -122.4194), 0f, 0, 0f)
        }
        if (points.size == 1) {
            return RouteSample(points.first(), 0f, 0, 1f)
        }
        val total = calculateTotalDistance(points).coerceAtLeast(0.1)
        val clampedDist = distanceAlongMeters.coerceIn(0.0, total)
        var accumulated = 0.0
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            val seg = haversineMeters(a, b)
            if (accumulated + seg >= clampedDist || i == points.size - 2) {
                val localDist = (clampedDist - accumulated).coerceIn(0.0, seg)
                val t = if (seg > 0.0001) (localDist / seg) else 0.0
                val lat = a.latitude + (b.latitude - a.latitude) * t
                val lon = a.longitude + (b.longitude - a.longitude) * t
                val bearing = bearingDegrees(a, b)
                val progress = (clampedDist / total).toFloat().coerceIn(0f, 1f)
                return RouteSample(
                    point = GeoPoint(lat, lon),
                    bearingDegrees = bearing,
                    segmentIndex = i,
                    progressFraction = progress
                )
            }
            accumulated += seg
        }
        return RouteSample(points.last(), 0f, points.size - 1, 1f)
    }
}

data class RouteSample(
    val point: GeoPoint,
    val bearingDegrees: Float,
    val segmentIndex: Int,
    val progressFraction: Float
)
