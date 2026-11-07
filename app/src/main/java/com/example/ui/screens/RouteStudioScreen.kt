package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MovementProfile
import com.example.data.RouteLoopMode
import com.example.service.MockTelemetryController
import com.example.service.TelemetryRuntimeState
import com.example.ui.SpooferUiState
import com.example.ui.theme.AmberRoute
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.DeepSpaceBg
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceElevated
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RouteStudioScreen(
    uiState: SpooferUiState,
    telemetry: TelemetryRuntimeState,
    onSelectProfile: (MovementProfile) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPlaybackSpeedMultiplierChange: (Float) -> Unit,
    onLoopModeChange: (RouteLoopMode) -> Unit,
    onRemoveWaypoint: (Int) -> Unit,
    onReverseWaypoints: () -> Unit,
    onClearWaypoints: () -> Unit,
    onRebuildRoute: () -> Unit,
    onSaveRoute: (String) -> Unit,
    onStartRouteSimulation: () -> Unit,
    onJumpToMapAddWaypoint: () -> Unit,
    onNavigateBackToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBackToMap)

    var showSaveRouteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpaceBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("route_studio_screen")
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Route Movement Studio",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "OSRM road-snapped path generator & velocity telemetry",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showSaveRouteDialog = true },
                    enabled = uiState.generatedRoutePoints.size >= 2,
                    modifier = Modifier.testTag("save_route_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = "Save generated route",
                        tint = if (uiState.generatedRoutePoints.size >= 2) AmberRoute else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Route Summary Telemetry Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TacticalBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (uiState.isRoutingLoading) {
                        LinearProgressIndicator(
                            color = CyanPulse,
                            trackColor = TacticalBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "TOTAL DISTANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "%.2f km", uiState.routeDistanceMeters / 1000.0),
                                style = MaterialTheme.typography.headlineMedium,
                                color = CyanPulse
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "EST. DURATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val mins = (uiState.routeDurationSeconds / 60.0).toInt()
                            val secs = (uiState.routeDurationSeconds % 60.0).toInt()
                            Text(
                                text = "${mins}m ${secs}s",
                                style = MaterialTheme.typography.headlineMedium,
                                color = EmeraldLive
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "GEOMETRY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (uiState.isRouteRoadSnapped) "OSRM Roads" else "Direct Spline",
                                style = MaterialTheme.typography.titleMedium,
                                color = AmberRoute
                            )
                            Text(
                                text = "${uiState.generatedRoutePoints.size} nodes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                onStartRouteSimulation()
                                onNavigateBackToMap()
                            },
                            enabled = uiState.generatedRoutePoints.size >= 2,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldLive,
                                contentColor = Color(0xFF002811)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("studio_launch_route_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch Route Spoof", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onRebuildRoute,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("rebuild_route_button")
                        ) {
                            Icon(Icons.Default.Route, contentDescription = null, tint = CyanPulse)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Recalculate", color = CyanPulse)
                        }
                    }
                }
            }
        }

        // Movement Speed Profile Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TacticalBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Velocity & Transport Profile",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MovementProfile.entries.forEach { profile ->
                            val selected = uiState.movementProfile == profile
                            val icon = when (profile) {
                                MovementProfile.WALK -> Icons.Default.DirectionsWalk
                                MovementProfile.SCOOTER -> Icons.Default.DirectionsBike
                                MovementProfile.DRIVE -> Icons.Default.DirectionsCar
                                MovementProfile.HIGHWAY -> Icons.Default.FlightTakeoff
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectProfile(profile) },
                                label = { Text("${profile.title} (${profile.defaultSpeedKmh.toInt()} km/h)") },
                                leadingIcon = {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPulse.copy(alpha = 0.22f),
                                    selectedLabelColor = CyanPulse,
                                    selectedLeadingIconColor = CyanPulse
                                ),
                                modifier = Modifier.testTag("profile_chip_${profile.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Custom Cruising Speed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "%.1f km/h (%.1f m/s)",
                                uiState.customSpeedKmh,
                                uiState.customSpeedKmh / 3.6f
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldLive
                        )
                    }

                    Slider(
                        value = uiState.customSpeedKmh,
                        onValueChange = onSpeedChange,
                        valueRange = 2.0f..180.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanPulse,
                            activeTrackColor = CyanPulse,
                            inactiveTrackColor = TacticalBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_speed_slider")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Simulation Speed Multiplier",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "%.1fx (%.1f km/h effective)",
                                uiState.playbackSpeedMultiplier,
                                uiState.customSpeedKmh * uiState.playbackSpeedMultiplier
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = AmberRoute
                        )
                    }

                    Slider(
                        value = uiState.playbackSpeedMultiplier,
                        onValueChange = onPlaybackSpeedMultiplierChange,
                        valueRange = 0.5f..10.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberRoute,
                            activeTrackColor = AmberRoute,
                            inactiveTrackColor = TacticalBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_speed_multiplier_slider")
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(0.5f, 1.0f, 2.0f, 5.0f, 10.0f).forEach { mult ->
                            val selected = kotlin.math.abs(uiState.playbackSpeedMultiplier - mult) < 0.08f
                            val labelText = if (mult == 0.5f) "0.5x" else "${mult.toInt()}x"
                            FilterChip(
                                selected = selected,
                                onClick = { onPlaybackSpeedMultiplierChange(mult) },
                                label = { Text(labelText) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberRoute.copy(alpha = 0.22f),
                                    selectedLabelColor = AmberRoute
                                ),
                                modifier = Modifier.testTag("studio_multiplier_chip_${labelText}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Loop & Patrol Mode",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RouteLoopMode.entries.forEach { mode ->
                            val selected = uiState.loopMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { onLoopModeChange(mode) },
                                label = { Text(mode.label) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldLive.copy(alpha = 0.2f),
                                    selectedLabelColor = EmeraldLive,
                                    selectedLeadingIconColor = EmeraldLive
                                ),
                                modifier = Modifier.testTag("loop_mode_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // Anti-Detection Organic Sensor Simulation Toggles
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TacticalBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Natural Sensor Realism (Snap Map / Telegram / WhatsApp)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GPS Micro-Jitter (±0.8m)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Prevents frozen-coordinate flags in social live-location feeds",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = telemetry.jitterEnabled,
                            onCheckedChange = { MockTelemetryController.setJitterEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanPulse),
                            modifier = Modifier.testTag("jitter_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Barometric Altitude Wave",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Injects natural elevation & vertical accuracy fluctuations",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = telemetry.altitudeWaveEnabled,
                            onCheckedChange = { MockTelemetryController.setAltitudeWaveEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldLive),
                            modifier = Modifier.testTag("altitude_wave_switch")
                        )
                    }
                }
            }
        }

        // Waypoints List Header + Actions
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Route Waypoints (${uiState.routeWaypoints.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = onJumpToMapAddWaypoint,
                        modifier = Modifier.testTag("tap_map_add_waypoint_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLocationAlt,
                            contentDescription = null,
                            tint = CyanPulse,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick on Map", color = CyanPulse)
                    }

                    IconButton(
                        onClick = onReverseWaypoints,
                        enabled = uiState.routeWaypoints.size >= 2,
                        modifier = Modifier.testTag("reverse_waypoints_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Reverse route",
                            tint = AmberRoute
                        )
                    }

                    IconButton(
                        onClick = onClearWaypoints,
                        enabled = uiState.routeWaypoints.isNotEmpty(),
                        modifier = Modifier.testTag("clear_waypoints_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear all waypoints",
                            tint = CoralAlert
                        )
                    }
                }
            }
        }

        if (uiState.routeWaypoints.isEmpty()) {
            item {
                Surface(
                    color = TacticalSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TacticalBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            tint = CyanPulse,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Route Waypoints Selected",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Switch map tap mode to 'Add Waypoint' and tap 2 or more locations on the map to generate a road-snapped route.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            itemsIndexed(uiState.routeWaypoints) { idx, wp ->
                Surface(
                    color = TacticalSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TacticalBorder, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val badgeColor = when (idx) {
                                0 -> EmeraldLive
                                uiState.routeWaypoints.lastIndex -> AmberRoute
                                else -> CyanPulse
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor.copy(alpha = 0.2f))
                                    .border(1.dp, badgeColor, CircleShape)
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = wp.label.ifBlank { "Waypoint #${idx + 1}" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format(Locale.US, "%.5f, %.5f", wp.latitude, wp.longitude),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { onRemoveWaypoint(idx) },
                            modifier = Modifier.testTag("remove_waypoint_$idx")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove waypoint",
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

    if (showSaveRouteDialog) {
        var routeName by remember {
            val first = uiState.routeWaypoints.firstOrNull()?.label?.ifBlank { "Start" } ?: "Start"
            val last = uiState.routeWaypoints.lastOrNull()?.label?.ifBlank { "End" } ?: "End"
            mutableStateOf("$first to $last")
        }
        AlertDialog(
            onDismissRequest = { showSaveRouteDialog = false },
            title = { Text("Save Generated Route") },
            text = {
                OutlinedTextField(
                    value = routeName,
                    onValueChange = { routeName = it },
                    label = { Text("Route Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_route_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveRoute(routeName)
                        showSaveRouteDialog = false
                    },
                    modifier = Modifier.testTag("confirm_save_route_button")
                ) {
                    Text("Save Route")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveRouteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
