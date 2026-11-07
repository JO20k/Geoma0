package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TacticalDarkColorScheme = darkColorScheme(
    primary = CyanPulse,
    onPrimary = Color(0xFF00262C),
    primaryContainer = Color(0xFF00404A),
    onPrimaryContainer = Color(0xFF9CF2FF),
    secondary = EmeraldLive,
    onSecondary = Color(0xFF002B13),
    secondaryContainer = Color(0xFF004E24),
    onSecondaryContainer = Color(0xFFA6F7C5),
    tertiary = AmberRoute,
    onTertiary = Color(0xFF332200),
    tertiaryContainer = Color(0xFF4F3600),
    onTertiaryContainer = Color(0xFFFFDF9B),
    error = CoralAlert,
    onError = Color(0xFF3B0000),
    background = DeepSpaceBg,
    onBackground = TextPrimary,
    surface = TacticalSurface,
    onSurface = TextPrimary,
    surfaceVariant = TacticalSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = TacticalBorder
)

private val TacticalLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB5EBFF),
    onPrimaryContainer = Color(0xFF001F25),
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB6F2CA),
    onSecondaryContainer = Color(0xFF00210E),
    tertiary = Color(0xFF8C5E00),
    onTertiary = Color.White,
    background = LightBg,
    onBackground = Color(0xFF0D1B2A),
    surface = LightSurface,
    onSurface = Color(0xFF0D1B2A),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF415A77),
    outline = Color(0xFFBCCCDC)
)

val TacticalShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TacticalDarkColorScheme else TacticalLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = TacticalShapes,
        content = content
    )
}
