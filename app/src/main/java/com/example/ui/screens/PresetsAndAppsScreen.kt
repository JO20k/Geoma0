package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SavedLocationEntity
import com.example.data.SavedRouteEntity
import com.example.data.TargetAppInfo
import com.example.ui.theme.AmberRoute
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.DeepSpaceBg
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceElevated
import java.util.Locale

@Composable
fun PresetsAndAppsScreen(
    compatibleApps: List<TargetAppInfo>,
    activeTargetAppId: String,
    savedLocations: List<SavedLocationEntity>,
    savedRoutes: List<SavedRouteEntity>,
    onOptimizeForApp: (TargetAppInfo) -> Unit,
    onLoadLocation: (SavedLocationEntity, Boolean) -> Unit,
    onDeleteLocation: (Int) -> Unit,
    onLoadRoute: (SavedRouteEntity, Boolean) -> Unit,
    onDeleteRoute: (Int) -> Unit,
    onNavigateBackToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBackToMap)
    val context = LocalContext.current

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpaceBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("presets_and_apps_screen")
    ) {
        // Section 1: Target Social & Messaging Apps (Snapchat, Telegram, WhatsApp)
        item {
            Text(
                text = "Target Social & Live Map Profiles",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "One-tap telemetry tuning for Snapchat Snap Map, Telegram Live Location, and WhatsApp Live Share",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(compatibleApps, key = { it.id }) { app ->
            val isSelected = activeTargetAppId == app.id
            val accentColor = Color(app.badgeColorHex)
            Card(
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) CyanPulse else TacticalBorder,
                        shape = RoundedCornerShape(18.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.22f))
                                    .border(1.dp, accentColor, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = app.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = app.feedFeature,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isSelected) {
                            Surface(
                                color = EmeraldLive.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldLive,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "TUNED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldLive,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Recommended Profile: ${app.recommendedProfile} • Packet Interval: ${app.updateIntervalMs}ms",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanPulse
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onOptimizeForApp(app) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) EmeraldLive else CyanPulse,
                                contentColor = Color(0xFF002229)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("optimize_app_${app.id}")
                        ) {
                            Text(
                                text = if (isSelected) "Active Telemetry Profile" else "Apply ${app.name.substringBefore(" ")} Profile",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        if (launchIntent != null) {
                            OutlinedButton(
                                onClick = { launchTargetAppIfInstalled(context, app.packageName) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("launch_app_${app.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Launch ${app.name}",
                                    tint = CyanPulse,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open", color = CyanPulse)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Saved Routes
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Saved Spoofing Routes (${savedRoutes.size})",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(savedRoutes, key = { it.id }) { route ->
            Surface(
                color = TacticalSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TacticalBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = null,
                                tint = EmeraldLive,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = route.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${route.startLabel} → ${route.endLabel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "%.2f km • %.1f km/h (%s)",
                                        route.totalDistanceMeters / 1000.0,
                                        route.speedKmh,
                                        route.profileName
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyanPulse
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteRoute(route.id) },
                            modifier = Modifier.testTag("delete_saved_route_${route.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete saved route",
                                tint = CoralAlert
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { onLoadRoute(route, false) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preview_route_${route.id}")
                        ) {
                            Text("Preview on Map", color = CyanPulse)
                        }

                        Button(
                            onClick = { onLoadRoute(route, true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldLive,
                                contentColor = Color(0xFF002811)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("run_saved_route_${route.id}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simulate Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section 3: Saved Location Presets
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Saved Coordinate Hotspots (${savedLocations.size})",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(savedLocations, key = { it.id }) { loc ->
            Surface(
                color = TacticalSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TacticalBorder, RoundedCornerShape(16.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PinDrop,
                            contentDescription = null,
                            tint = AmberRoute,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = loc.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = loc.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "%.5f, %.5f • %s", loc.latitude, loc.longitude, loc.category),
                                style = MaterialTheme.typography.labelMedium,
                                color = CyanPulse
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { onLoadLocation(loc, true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanPulse,
                                contentColor = Color(0xFF002229)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("teleport_preset_${loc.id}")
                        ) {
                            Text("Teleport", fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = { onDeleteLocation(loc.id) },
                            modifier = Modifier.testTag("delete_preset_${loc.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete preset",
                                tint = CoralAlert
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun launchTargetAppIfInstalled(context: Context, packageName: String) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    } catch (_: Exception) {
    }
}
