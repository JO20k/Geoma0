package com.example.data

import kotlinx.coroutines.flow.Flow

class GeoSpoofRepository(private val dao: GeoSpoofDao) {
    val savedLocations: Flow<List<SavedLocationEntity>> = dao.getAllLocations()
    val savedRoutes: Flow<List<SavedRouteEntity>> = dao.getAllRoutes()

    val compatibleTargetApps: List<TargetAppInfo> = listOf(
        TargetAppInfo(
            id = "snapchat",
            name = "Snapchat (Snap Map)",
            packageName = "com.snapchat.android",
            feedFeature = "Snap Map Live Bitmoji & Actionmojis",
            recommendedProfile = "Walking (5.2 km/h) or City Driving (45 km/h)",
            jitterRecommended = true,
            updateIntervalMs = 500L,
            badgeColorHex = 0xFFFFFC00
        ),
        TargetAppInfo(
            id = "telegram",
            name = "Telegram (Live Location)",
            packageName = "org.telegram.messenger",
            feedFeature = "Real-Time Chat Live Location & Proximity Alerts",
            recommendedProfile = "Cycling / Scooter (18.5 km/h) with Heading Bearing",
            jitterRecommended = true,
            updateIntervalMs = 450L,
            badgeColorHex = 0xFF24A1DE
        ),
        TargetAppInfo(
            id = "whatsapp",
            name = "WhatsApp (Live Share)",
            packageName = "com.whatsapp",
            feedFeature = "End-to-End Encrypted Live Location Pin & Static Drop",
            recommendedProfile = "Walking (5.0 km/h) or City Driving (48 km/h)",
            jitterRecommended = true,
            updateIntervalMs = 500L,
            badgeColorHex = 0xFF25D366
        )
    )

    suspend fun saveLocation(location: SavedLocationEntity) {
        dao.insertLocation(location)
    }

    suspend fun deleteLocation(id: Int) {
        dao.deleteLocationById(id)
    }

    suspend fun saveRoute(route: SavedRouteEntity) {
        dao.insertRoute(route)
    }

    suspend fun deleteRoute(id: Int) {
        dao.deleteRouteById(id)
    }

    suspend fun seedDefaultsIfEmpty() {
        if (dao.getLocationCount() == 0) {
            val defaults = listOf(
                SavedLocationEntity(
                    name = "Shibuya Crossing",
                    subtitle = "Tokyo, Japan • High-Density Snap Map Hub",
                    latitude = 35.6595,
                    longitude = 139.7004,
                    category = "Social Hotspot",
                    isFavorite = true
                ),
                SavedLocationEntity(
                    name = "Times Square Plaza",
                    subtitle = "Manhattan, New York, USA",
                    latitude = 40.7580,
                    longitude = -73.9855,
                    category = "Metropolitan",
                    isFavorite = true
                ),
                SavedLocationEntity(
                    name = "SoHo & Carnaby Street",
                    subtitle = "Westminster, London, UK",
                    latitude = 51.5136,
                    longitude = -0.1365,
                    category = "Europe Hub",
                    isFavorite = false
                ),
                SavedLocationEntity(
                    name = "Marina Bay Waterfront",
                    subtitle = "Downtown Core, Singapore",
                    latitude = 1.2834,
                    longitude = 103.8607,
                    category = "Asia Hub",
                    isFavorite = false
                ),
                SavedLocationEntity(
                    name = "Embarcadero & Pier 14",
                    subtitle = "San Francisco, CA, USA",
                    latitude = 37.7955,
                    longitude = -122.3937,
                    category = "Tech Corridor",
                    isFavorite = true
                ),
                SavedLocationEntity(
                    name = "Champs-Élysées Arc",
                    subtitle = "8th Arrondissement, Paris, France",
                    latitude = 48.8738,
                    longitude = 2.2950,
                    category = "Europe Hub",
                    isFavorite = false
                )
            )
            defaults.forEach { dao.insertLocation(it) }
        }

        if (dao.getRouteCount() == 0) {
            val sfPatrolPoints = GeoMath.interpolateWaypoints(
                listOf(
                    GeoPoint(37.7955, -122.3937, "Ferry Building"),
                    GeoPoint(37.8014, -122.4018, "Telegraph Hill"),
                    GeoPoint(37.8060, -122.4103, "North Beach"),
                    GeoPoint(37.8087, -122.4098, "Pier 39")
                ),
                stepMeters = 20.0
            )
            dao.insertRoute(
                SavedRouteEntity(
                    name = "SF Waterfront Cruiser",
                    startLabel = "Embarcadero Ferry Building",
                    endLabel = "Pier 39 Boardwalk",
                    encodedWaypoints = SavedRouteEntity.encodePoints(sfPatrolPoints),
                    totalDistanceMeters = GeoMath.calculateTotalDistance(sfPatrolPoints),
                    profileName = MovementProfile.SCOOTER.name,
                    speedKmh = 18.5f
                )
            )

            val tokyoWalkPoints = GeoMath.interpolateWaypoints(
                listOf(
                    GeoPoint(35.6595, 139.7004, "Shibuya Scramble"),
                    GeoPoint(35.6628, 139.6990, "Center-Gai"),
                    GeoPoint(35.6664, 139.7020, "Miyashita Park"),
                    GeoPoint(35.6702, 139.7027, "Harajuku Station")
                ),
                stepMeters = 15.0
            )
            dao.insertRoute(
                SavedRouteEntity(
                    name = "Shibuya to Harajuku Walk",
                    startLabel = "Shibuya Crossing",
                    endLabel = "Harajuku Takeshita St",
                    encodedWaypoints = SavedRouteEntity.encodePoints(tokyoWalkPoints),
                    totalDistanceMeters = GeoMath.calculateTotalDistance(tokyoWalkPoints),
                    profileName = MovementProfile.WALK.name,
                    speedKmh = 5.4f
                )
            )

            val nycDrivePoints = GeoMath.interpolateWaypoints(
                listOf(
                    GeoPoint(40.7580, -73.9855, "Times Square"),
                    GeoPoint(40.7614, -73.9776, "5th Ave Midtown"),
                    GeoPoint(40.7644, -73.9730, "Grand Army Plaza"),
                    GeoPoint(40.7712, -73.9742, "Central Park Lake")
                ),
                stepMeters = 22.0
            )
            dao.insertRoute(
                SavedRouteEntity(
                    name = "Manhattan Midtown Loop",
                    startLabel = "Times Square 42nd St",
                    endLabel = "Central Park South",
                    encodedWaypoints = SavedRouteEntity.encodePoints(nycDrivePoints),
                    totalDistanceMeters = GeoMath.calculateTotalDistance(nycDrivePoints),
                    profileName = MovementProfile.DRIVE.name,
                    speedKmh = 38.0f
                )
            )
        }
    }
}
