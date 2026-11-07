package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.NominatimPlace
import com.example.data.SavedLocationEntity
import com.example.service.TelemetryRuntimeState
import com.example.ui.MapTapMode
import com.example.ui.SpooferUiState
import com.example.ui.components.InteractiveSpoofMapCanvas
import com.example.ui.components.TacticalJoystickPad
import com.example.ui.components.TelemetryHudTopBar
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
fun LiveMapCockpitScreen(
    uiState: SpooferUiState,
    telemetry: TelemetryRuntimeState,
    savedLocations: List<SavedLocationEntity>,
    onSearchQueryChange: (String) -> Unit,
    onSelectSearchResult: (NominatimPlace, Boolean) -> Unit,
    onMapTap: (com.example.data.GeoPoint) -> Unit,
    onMapPan: (com.example.data.GeoPoint) -> Unit,
    onSetZoom: (Int) -> Unit,
    onRecenter: () -> Unit,
    onSetTapMode: (MapTapMode) -> Unit,
    onToggleJoystick: (Boolean) -> Unit,
    onJoystickMoved: (Float, Float) -> Unit,
    onStartStaticSpoof: () -> Unit,
    onStartRouteSpoof: () -> Unit,
    onStopSpoof: () -> Unit,
    onTogglePauseRoute: () -> Unit,
    onSeekRouteProgress: (Float) -> Unit,
    onPlaybackSpeedMultiplierChange: (Float) -> Unit,
    onSetManualCoords: (Double, Double, String) -> Unit,
    onSavePinToPresets: (String) -> Unit,
    onLoadPreset: (SavedLocationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showManualCoordDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Interactive Web Mercator Spoof Map
        InteractiveSpoofMapCanvas(
            center = uiState.mapCenter,
            zoom = uiState.mapZoom,
            selectedPin = uiState.selectedTargetPin,
            activeLivePoint = telemetry.currentPoint,
            activeBearingDegrees = telemetry.currentBearingDegrees,
            isSpoofingActive = telemetry.isSpoofingActive,
            waypoints = uiState.routeWaypoints,
            routePoints = uiState.generatedRoutePoints,
            onMapTap = onMapTap,
            onMapPan = onMapPan,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Floating Telemetry HUD + Search Bar + Quick Teleport Chips
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            TelemetryHudTopBar(
                telemetry = telemetry,
                activeTargetAppId = uiState.activeTargetAppId
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Geocoding Search Box
            Surface(
                color = TacticalSurface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TacticalBorder, RoundedCornerShape(16.dp))
            ) {
                Column {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_hint),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search place",
                                tint = CyanPulse
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = CyanPulse
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onSearchQueryChange("") },
                                        modifier = Modifier.testTag("clear_search_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { showManualCoordDialog = true },
                                    modifier = Modifier.testTag("manual_coordinates_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditLocationAlt,
                                        contentDescription = "Enter exact coordinates",
                                        tint = AmberRoute
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("place_search_input")
                    )

                    AnimatedVisibility(visible = uiState.searchResults.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepSpaceBg.copy(alpha = 0.98f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            uiState.searchResults.forEach { place ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onSelectSearchResult(place, false) }
                                        .padding(vertical = 8.dp, horizontal = 8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = place.shortTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = place.subtitle.ifEmpty {
                                                String.format(Locale.US, "%.4f, %.4f", place.latitude, place.longitude)
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    TextButton(
                                        onClick = { onSelectSearchResult(place, true) },
                                        modifier = Modifier.testTag("add_search_waypoint_${place.placeId}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddLocationAlt,
                                            contentDescription = "Add to route",
                                            tint = EmeraldLive,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+Route", color = EmeraldLive)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick City Hotspots Strip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(savedLocations.take(6), key = { it.id }) { preset ->
                    FilterChip(
                        selected = false,
                        onClick = { onLoadPreset(preset) },
                        label = {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PinDrop,
                                contentDescription = null,
                                tint = CyanPulse,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = TacticalSurface.copy(alpha = 0.90f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = TacticalBorder
                        ),
                        modifier = Modifier.testTag("quick_preset_chip_${preset.id}")
                    )
                }
            }
        }

        // 3. Right Side Map Zoom + Recenter + Tap Mode + Joystick Controls
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        ) {
            FilledTonalIconButton(
                onClick = { onSetZoom(uiState.mapZoom + 1) },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = TacticalSurface.copy(alpha = 0.92f),
                    contentColor = CyanPulse
                ),
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, TacticalBorder, CircleShape)
                    .testTag("zoom_in_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom in")
            }

            FilledTonalIconButton(
                onClick = { onSetZoom(uiState.mapZoom - 1) },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = TacticalSurface.copy(alpha = 0.92f),
                    contentColor = CyanPulse
                ),
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, TacticalBorder, CircleShape)
                    .testTag("zoom_out_button")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom out")
            }

            FilledTonalIconButton(
                onClick = onRecenter,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = TacticalSurface.copy(alpha = 0.92f),
                    contentColor = EmeraldLive
                ),
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, TacticalBorder, CircleShape)
                    .testTag("recenter_map_button")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recenter on active pin")
            }

            FilledTonalIconButton(
                onClick = { onToggleJoystick(!uiState.showJoystickOverlay) },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (uiState.showJoystickOverlay) CyanPulse.copy(alpha = 0.22f)
                    else TacticalSurface.copy(alpha = 0.92f),
                    contentColor = if (uiState.showJoystickOverlay) CyanPulse else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .size(48.dp)
                    .border(
                        1.dp,
                        if (uiState.showJoystickOverlay) CyanPulse else TacticalBorder,
                        CircleShape
                    )
                    .testTag("toggle_joystick_button")
            ) {
                Icon(Icons.Default.Gamepad, contentDescription = "Toggle 360 live joystick")
            }
        }

        // 4. Left Side Floating 360° Joystick Pad
        AnimatedVisibility(
            visible = uiState.showJoystickOverlay,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TacticalJoystickPad(onVectorTick = onJoystickMoved)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = TacticalSurface.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "360° DRIFT",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanPulse,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 5. Bottom Tactical Control Deck
        Surface(
            color = TacticalSurface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 12.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .border(
                    1.dp,
                    TacticalBorder,
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Tap Mode Toggle + Save Preset Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MapTapMode.entries.forEach { mode ->
                            val isSelected = uiState.mapTapMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSetTapMode(mode) },
                                label = { Text(mode.title) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (mode == MapTapMode.SINGLE_PIN) Icons.Default.PinDrop else Icons.Default.Route,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPulse.copy(alpha = 0.2f),
                                    selectedLabelColor = CyanPulse,
                                    selectedLeadingIconColor = CyanPulse
                                ),
                                modifier = Modifier.testTag("map_tap_mode_${mode.name.lowercase()}")
                            )
                        }
                    }

                    IconButton(
                        onClick = { showSavePresetDialog = true },
                        modifier = Modifier.testTag("save_pin_preset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Save current coordinate to presets",
                            tint = AmberRoute
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Selected Pin / Route Summary Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.selectedTargetPin.label.ifBlank { "Selected Target Coordinate" },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "LAT %.5f  •  LON %.5f  •  ALT %.1fm",
                                uiState.selectedTargetPin.latitude,
                                uiState.selectedTargetPin.longitude,
                                telemetry.currentAltitudeMeters
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.routeDistanceMeters > 0) {
                        Surface(
                            color = TacticalSurfaceElevated,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.border(1.dp, TacticalBorder, RoundedCornerShape(10.dp))
                        ) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%.2f km route",
                                    uiState.routeDistanceMeters / 1000.0
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldLive,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Live Route Progress Scrubber & Simulation Speed Multiplier Slider when route points exist
                if (uiState.generatedRoutePoints.size >= 2) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${(telemetry.routeProgressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanPulse,
                            modifier = Modifier.width(40.dp)
                        )
                        Slider(
                            value = telemetry.routeProgressFraction,
                            onValueChange = onSeekRouteProgress,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanPulse,
                                activeTrackColor = EmeraldLive,
                                inactiveTrackColor = TacticalBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .testTag("route_progress_scrubber")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Route Playback Speed Multiplier Control (0.5x - 10x + Quick Preset Chips)
                    Surface(
                        color = TacticalSurfaceElevated.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TacticalBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "PLAYBACK SPEED MULTIPLIER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "%.1fx  •  %.1f km/h",
                                        uiState.playbackSpeedMultiplier,
                                        uiState.customSpeedKmh * uiState.playbackSpeedMultiplier
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AmberRoute,
                                    fontWeight = FontWeight.Bold
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
                                    .height(28.dp)
                                    .testTag("playback_speed_multiplier_slider")
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(0.5f, 1.0f, 2.0f, 5.0f, 10.0f).forEach { presetMultiplier ->
                                    val isSelected = kotlin.math.abs(uiState.playbackSpeedMultiplier - presetMultiplier) < 0.08f
                                    val labelText = if (presetMultiplier == 0.5f) "0.5x" else "${presetMultiplier.toInt()}x"
                                    Surface(
                                        color = if (isSelected) AmberRoute.copy(alpha = 0.22f) else DeepSpaceBg,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) AmberRoute else TacticalBorder,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onPlaybackSpeedMultiplierChange(presetMultiplier) }
                                            .testTag("speed_multiplier_chip_${labelText}")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = labelText,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isSelected) AmberRoute else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Action Buttons Row: Static Spoof, Route Simulation, Stop
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!telemetry.isSpoofingActive) {
                        Button(
                            onClick = onStartStaticSpoof,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanPulse,
                                contentColor = Color(0xFF002229)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("deploy_static_spoof_button")
                        ) {
                            Icon(Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Teleport Pin",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onStartRouteSpoof,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldLive,
                                contentColor = Color(0xFF002912)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.15f)
                                .height(50.dp)
                                .testTag("start_route_simulation_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Simulate Route",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        if (telemetry.isRouteMoving) {
                            OutlinedButton(
                                onClick = onTogglePauseRoute,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("pause_resume_route_button")
                            ) {
                                Icon(
                                    imageVector = if (telemetry.isRoutePaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    tint = AmberRoute
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (telemetry.isRoutePaused) "Resume Route" else "Hold Position",
                                    color = AmberRoute,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = onStartRouteSpoof,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldLive,
                                    contentColor = Color(0xFF002912)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("switch_to_route_button")
                            ) {
                                Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Run Route", fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onStopSpoof,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralAlert,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("stop_spoofing_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Stop Spoof",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Manual Coordinate Entry Dialog
    if (showManualCoordDialog) {
        var latText by remember {
            mutableStateOf(String.format(Locale.US, "%.5f", uiState.selectedTargetPin.latitude))
        }
        var lonText by remember {
            mutableStateOf(String.format(Locale.US, "%.5f", uiState.selectedTargetPin.longitude))
        }
        var labelText by remember { mutableStateOf("Custom GPS Lock") }

        AlertDialog(
            onDismissRequest = { showManualCoordDialog = false },
            title = { Text("Enter Custom Coordinates") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = labelText,
                        onValueChange = { labelText = it },
                        label = { Text("Location Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Latitude (-85.0 to +85.0)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_lat_input")
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it },
                        label = { Text("Longitude (-180.0 to +180.0)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_lon_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = latText.toDoubleOrNull()
                        val lon = lonText.toDoubleOrNull()
                        if (lat != null && lon != null) {
                            onSetManualCoords(lat, lon, labelText)
                            showManualCoordDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_manual_coords_button")
                ) {
                    Text("Teleport Pin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualCoordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Save Current Pin Dialog
    if (showSavePresetDialog) {
        var presetName by remember {
            mutableStateOf(uiState.selectedTargetPin.label.ifBlank { "My Spoof Spot" })
        }
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save Coordinate Preset") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Label") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preset_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSavePinToPresets(presetName)
                        showSavePresetDialog = false
                    },
                    modifier = Modifier.testTag("confirm_save_preset_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
