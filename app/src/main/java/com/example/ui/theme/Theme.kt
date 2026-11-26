package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LastSurvivorColorScheme = darkColorScheme(
    primary = AmberStrike,
    onPrimary = TacticalMidnight,
    primaryContainer = TacticalSurfaceElevated,
    onPrimaryContainer = AmberGlow,
    secondary = CyberCyan,
    onSecondary = TacticalMidnight,
    secondaryContainer = Color(0xFF123344),
    onSecondaryContainer = CyberCyan,
    tertiary = EmeraldReady,
    onTertiary = TacticalMidnight,
    background = TacticalMidnight,
    onBackground = TextPrimary,
    surface = TacticalSurface,
    onSurface = TextPrimary,
    surfaceVariant = TacticalSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = TacticalBorder,
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun LastSurvivorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LastSurvivorColorScheme,
        typography = Typography,
        content = content
    )
}
