package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val label: String = ""
)

enum class MovementProfile(
    val title: String,
    val osrmProfile: String,
    val defaultSpeedKmh: Float,
    val minSpeedKmh: Float,
    val maxSpeedKmh: Float
) {
    WALK("Walking", "foot", 5.2f, 2.0f, 9.0f),
    SCOOTER("Cycling / Scooter", "bike", 18.5f, 8.0f, 35.0f),
    DRIVE("City Driving", "car", 48.0f, 15.0f, 110.0f),
    HIGHWAY("Express / Drone", "car", 95.0f, 40.0f, 220.0f)
}

enum class RouteLoopMode(val label: String) {
    ONE_WAY("Stop at Destination"),
    PING_PONG("Ping-Pong Patrol"),
    CIRCUIT("Loop to Start")
}

data class TargetAppInfo(
    val id: String,
    val name: String,
    val packageName: String,
    val feedFeature: String,
    val recommendedProfile: String,
    val jitterRecommended: Boolean,
    val updateIntervalMs: Long,
    val badgeColorHex: Long
)

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_routes")
data class SavedRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val startLabel: String,
    val endLabel: String,
    val encodedWaypoints: String, // "lat,lon;lat,lon;..."
    val totalDistanceMeters: Double,
    val profileName: String,
    val speedKmh: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toGeoPoints(): List<GeoPoint> {
        if (encodedWaypoints.isBlank()) return emptyList()
        return encodedWaypoints.split(";").mapNotNull { token ->
            val parts = token.split(",")
            if (parts.size >= 2) {
                val lat = parts[0].toDoubleOrNull()
                val lon = parts[1].toDoubleOrNull()
                if (lat != null && lon != null) GeoPoint(lat, lon) else null
            } else null
        }
    }

    companion object {
        fun encodePoints(points: List<GeoPoint>): String {
            return points.joinToString(";") { "${it.latitude},${it.longitude}" }
        }
    }
}

@Dao
interface GeoSpoofDao {
    @Query("SELECT * FROM saved_locations ORDER BY isFavorite DESC, timestamp DESC")
    fun getAllLocations(): Flow<List<SavedLocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: SavedLocationEntity)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteLocationById(id: Int)

    @Query("SELECT COUNT(*) FROM saved_locations")
    suspend fun getLocationCount(): Int

    @Query("SELECT * FROM saved_routes ORDER BY timestamp DESC")
    fun getAllRoutes(): Flow<List<SavedRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: SavedRouteEntity)

    @Query("DELETE FROM saved_routes WHERE id = :id")
    suspend fun deleteRouteById(id: Int)

    @Query("SELECT COUNT(*) FROM saved_routes")
    suspend fun getRouteCount(): Int
}

@Database(
    entities = [SavedLocationEntity::class, SavedRouteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GeoSpoofDatabase : RoomDatabase() {
    abstract fun geoSpoofDao(): GeoSpoofDao

    companion object {
        @Volatile
        private var INSTANCE: GeoSpoofDatabase? = null

        fun getInstance(context: Context): GeoSpoofDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GeoSpoofDatabase::class.java,
                    "geopulse_spoof_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
