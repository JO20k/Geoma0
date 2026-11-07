package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.service.TelemetryRuntimeState
import com.example.ui.theme.AmberRoute
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.DeepSpaceBg
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun TacticalJoystickPad(
    onVectorTick: (angleRadians: Float, intensity: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var currentAngle by remember { mutableFloatStateOf(0f) }
    var currentIntensity by remember { mutableFloatStateOf(0f) }
    val currentOnTick by rememberUpdatedState(onVectorTick)

    LaunchedEffect(isDragging) {
        while (isActive && isDragging) {
            if (currentIntensity > 0.08f) {
                currentOnTick(currentAngle, currentIntensity)
            }
            delay(180L)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(124.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        TacticalSurface.copy(alpha = 0.92f),
                        DeepSpaceBg.copy(alpha = 0.96f)
                    )
                )
            )
            .border(1.5.dp, if (isDragging) CyanPulse else TacticalBorder, CircleShape)
            .testTag("tactical_joystick_pad")
            .pointerInput(Unit) {
                val maxRadius = size.width / 2f * 0.68f
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        knobOffset = Offset.Zero
                        currentIntensity = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        knobOffset = Offset.Zero
                        currentIntensity = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val raw = knobOffset + dragAmount
                        val dist = hypot(raw.x, raw.y)
                        val clampedDist = dist.coerceAtMost(maxRadius)
                        // Screen angle: North (up, -Y) is 0 radians, East (+X) is +PI/2
                        val angle = atan2(raw.x, -raw.y)
                        val scale = if (dist > 0.001f) clampedDist / dist else 0f
                        knobOffset = Offset(raw.x * scale, raw.y * scale)
                        currentAngle = angle
                        currentIntensity = (clampedDist / maxRadius).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.size(124.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRing = size.minDimension * 0.42f
            val innerRing = size.minDimension * 0.22f

            drawCircle(
                color = TacticalBorder.copy(alpha = 0.7f),
                radius = outerRing,
                center = center,
                style = Stroke(width = 1.5f)
            )
            drawCircle(
                color = TacticalBorder.copy(alpha = 0.4f),
                radius = innerRing,
                center = center,
                style = Stroke(width = 1f)
            )
            drawLine(
                color = TacticalBorder.copy(alpha = 0.5f),
                start = Offset(center.x, center.y - outerRing),
                end = Offset(center.x, center.y + outerRing),
                strokeWidth = 1f
            )
            drawLine(
                color = TacticalBorder.copy(alpha = 0.5f),
                start = Offset(center.x - outerRing, center.y),
                end = Offset(center.x + outerRing, center.y),
                strokeWidth = 1f
            )

            val thumbPos = center + knobOffset
            drawLine(
                color = CyanPulse.copy(alpha = 0.65f),
                start = center,
                end = thumbPos,
                strokeWidth = 4f
            )
            drawCircle(
                color = if (isDragging) EmeraldLive.copy(alpha = 0.35f) else CyanPulse.copy(alpha = 0.25f),
                radius = 24f,
                center = thumbPos
            )
            drawCircle(
                color = if (isDragging) EmeraldLive else CyanPulse,
                radius = 15f,
                center = thumbPos
            )
        }
    }
}

@Composable
fun TelemetryHudTopBar(
    telemetry: TelemetryRuntimeState,
    activeTargetAppId: String,
    modifier: Modifier = Modifier
) {
    val activeColor = when {
        telemetry.isSpoofingActive && telemetry.isRouteMoving && !telemetry.isRoutePaused -> EmeraldLive
        telemetry.isSpoofingActive -> CyanPulse
        else -> AmberRoute
    }

    val statusText = when {
        telemetry.isSpoofingActive && telemetry.isRouteMoving && !telemetry.isRoutePaused -> "LIVE ROUTE STREAM"
        telemetry.isSpoofingActive && telemetry.isRoutePaused -> "ROUTE PAUSED • LOCKED"
        telemetry.isSpoofingActive -> "STATIC GPS SPOOFING"
        else -> "STANDBY • READY"
    }

    val targetLabel = when (activeTargetAppId) {
        "snapchat" -> "SNAP MAP"
        "telegram" -> "TELEGRAM LIVE"
        "whatsapp" -> "WHATSAPP LIVE"
        else -> "GLOBAL GPS"
    }

    Surface(
        color = TacticalSurface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, activeColor.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .testTag("telemetry_hud_top_bar")
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(activeColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = activeColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = DeepSpaceBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "$targetLabel • #${telemetry.packetsInjectedCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "Coordinates",
                        tint = CyanPulse,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format(
                            Locale.US,
                            "%.5f, %.5f",
                            telemetry.currentPoint.latitude,
                            telemetry.currentPoint.longitude
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Velocity",
                        tint = EmeraldLive,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(
                            Locale.US,
                            "%.1f km/h (%.1fx)",
                            telemetry.currentSpeedKmh,
                            telemetry.playbackSpeedMultiplier
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Bearing",
                        tint = AmberRoute,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.US, "%03.0f°", telemetry.currentBearingDegrees),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
