package com.uc3m.travelapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TravelBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = TravelBlueDark,
    secondary = TravelCoral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFECB3),
    onSecondaryContainer = TravelCoralDark,
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = Color.White,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = Color(0xFFB0BEC5),
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = TravelBlueDarkTheme,
    onPrimary = Color(0xFF0D1B2A),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = TravelCoralDarkTheme,
    onSecondary = Color(0xFF0D1B2A),
    background = SurfaceDark,
    onBackground = Color(0xFFE8F0FE),
    surface = SurfaceVariantDark,
    onSurface = Color(0xFFE8F0FE),
    surfaceVariant = Color(0xFF1E2A3E),
    onSurfaceVariant = Color(0xFF90A4AE),
    outline = Color(0xFF37474F),
    error = ErrorRedDark,
    onError = Color(0xFF0D1B2A)
)

@Composable
fun TravelAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Fixed colors ensure consistent travel-themed identity across all devices (MA-02)
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}