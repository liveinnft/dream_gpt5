package com.example.dreamtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors: ColorScheme = lightColorScheme(
    primary = PastelBlue,
    onPrimary = OnPastelBlue,
    background = BackgroundLight,
    onBackground = PrimaryTextLight,
    surface = SurfaceLight,
    onSurface = PrimaryTextLight,
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = PastelBlueDark,
    onPrimary = PrimaryTextDark,
    background = BackgroundDark,
    onBackground = PrimaryTextDark,
    surface = SurfaceDark,
    onSurface = PrimaryTextDark,
)

@Composable
fun DreamTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}