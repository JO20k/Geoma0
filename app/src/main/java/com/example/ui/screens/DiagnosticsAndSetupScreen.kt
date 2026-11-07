package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

@Composable
fun DiagnosticsAndSetupScreen(
    uiState: SpooferUiState,
    telemetry: TelemetryRuntimeState,
    hasFineLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onRefreshMockStatus: () -> Unit,
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
            .testTag("diagnostics_screen")
    ) {
        item {
            Text(
                text = "Mock Provider Setup & Diagnostics",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Android TestProvider & FusedLocationProviderClient status monitor",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Visual Radar Banner Illustration (Custom Canvas Artwork)
        item {
            Surface(
                color = TacticalSurface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .border(1.dp, TacticalBorder, RoundedCornerShape(20.dp))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width * 0.78f, size.height * 0.5f)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    TacticalSurface,
                                    Color(0xFF0A2433)
                                )
                            )
                        )
                        for (r in listOf(30f, 65f, 105f, 145f)) {
                            drawCircle(
                                color = CyanPulse.copy(alpha = 0.22f),
                                radius = r,
                                center = center,
                                style = Stroke(width = 1.5f)
                            )
                        }
                        drawCircle(
                            color = if (telemetry.isSpoofingActive) EmeraldLive else CyanPulse,
                            radius = 10f,
                            center = center
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "GEOPULSE KERNEL BRIDGE",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanPulse
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = telemetry.activeProviderStatus,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (telemetry.systemMockPermissionGranted) EmeraldLive else AmberRoute
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Packets Injected: ${telemetry.packetsInjectedCount} • Accuracy ±${
                                String.format(Locale.US, "%.1fm", telemetry.currentAccuracyMeters)
                            }",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Step-by-Step Permission & Developer Options Verification Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TacticalBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "System Readiness Checklist",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = onRefreshMockStatus,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("refresh_diagnostics_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Re-Check")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Check 1: Fine Location Runtime Permission
                    DiagnosticStatusRow(
                        title = "1. Runtime Fine Location Permission",
                        subtitle = if (hasFineLocationPermission) {
                            "Granted • Ready for Fused & GPS provider access"
                        } else {
                            "Required to initialize mock location providers"
                        },
                        isReady = hasFineLocationPermission,
                        actionLabel = if (!hasFineLocationPermission) "Grant" else null,
                        onAction = onRequestLocationPermission,
                        testTag = "grant_location_permission_button"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Check 2: Mock Location App Selection in Developer Options
                    val mockReady = uiState.isMockAppSelectedInSystem || telemetry.systemMockPermissionGranted
                    DiagnosticStatusRow(
                        title = "2. Android Developer Options Mock App",
                        subtitle = if (mockReady) {
                            "GeoPulse is authorized as the system Mock Location App"
                        } else {
                            "Select 'GeoPulse' under Developer Options → Select mock location app so Snapchat, Telegram & WhatsApp receive spoofed coordinates"
                        },
                        isReady = mockReady,
                        actionLabel = "Dev Options",
                        onAction = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(fallback)
                                } catch (_: Exception) {
                                }
                            }
                        },
                        testTag = "open_developer_options_button"
                    )
                }
            }
        }

        // Live Raw Location Packet Inspector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TacticalBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = CyanPulse,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Injected Location Packet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = DeepSpaceBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TacticalBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            TelemetryKeyValueRow(
                                key = "PROVIDER_TARGETS",
                                value = "gps, network, fused_location_client"
                            )
                            TelemetryKeyValueRow(
                                key = "LATITUDE",
                                value = String.format(Locale.US, "%.6f°", telemetry.currentPoint.latitude)
                            )
                            TelemetryKeyValueRow(
                                key = "LONGITUDE",
                                value = String.format(Locale.US, "%.6f°", telemetry.currentPoint.longitude)
                            )
                            TelemetryKeyValueRow(
                                key = "ALTITUDE (WGS84)",
                                value = String.format(Locale.US, "%.2f m", telemetry.currentAltitudeMeters)
                            )
                            TelemetryKeyValueRow(
                                key = "GROUND_SPEED",
                                value = String.format(
                                    Locale.US,
                                    "%.2f km/h (%.2f m/s)",
                                    telemetry.currentSpeedKmh,
                                    telemetry.currentSpeedKmh / 3.6f
                                )
                            )
                            TelemetryKeyValueRow(
                                key = "HEADING_BEARING",
                                value = String.format(Locale.US, "%.1f°", telemetry.currentBearingDegrees)
                            )
                            TelemetryKeyValueRow(
                                key = "HORIZONTAL_ACCURACY",
                                value = String.format(Locale.US, "±%.2f m", telemetry.currentAccuracyMeters)
                            )
                            TelemetryKeyValueRow(
                                key = "ELAPSED_REALTIME_SYNC",
                                value = "SystemClock.elapsedRealtimeNanos() [VALID]"
                            )
                        }
                    }
                }
            }
        }

        // How to Use with Snapchat, Telegram & WhatsApp Guide
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TacticalSurfaceElevated),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TacticalBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = EmeraldLive,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tips for Snapchat, Telegram & WhatsApp",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "• Snapchat Snap Map: Enable 'GPS Micro-Jitter' and use Walking (5.2 km/h) or City Driving (48 km/h) routes so your Bitmoji updates naturally along streets.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Telegram Live Location: Start route simulation in GeoPulse before sharing your Live Location in a Telegram chat; the heading arrow will track your route bearing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• WhatsApp Live Share: Keep GeoPulse running in the foreground notification while sharing live location for uninterrupted background coordinate updates.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DiagnosticStatusRow(
    title: String,
    subtitle: String,
    isReady: Boolean,
    actionLabel: String?,
    onAction: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TacticalSurfaceElevated)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isReady) EmeraldLive else AmberRoute,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (actionLabel != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isReady) TacticalSurface else CyanPulse,
                    contentColor = if (isReady) CyanPulse else Color(0xFF002229)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag(testTag)
            ) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TelemetryKeyValueRow(key: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = CyanPulse,
            fontWeight = FontWeight.Bold
        )
    }
}
