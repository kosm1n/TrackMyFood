package com.cosmin.trackmyfood.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = WellnessDarkPrimary,
    primaryContainer = WellnessDarkPrimaryContainer,
    secondary = WellnessSecondary,
    background = WellnessDarkBackground,
    surface = WellnessDarkSurface,
    onPrimary = WellnessDarkBackground,
    onSurface = WellnessDarkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = WellnessPrimary,
    primaryContainer = WellnessPrimaryContainer,
    secondary = WellnessSecondary,
    secondaryContainer = WellnessSecondaryContainer,
    background = WellnessBackground,
    surface = WellnessSurface,
    onPrimary = WellnessSurface,
    onSurface = WellnessOnSurface
)

@Composable
fun TrackMyFoodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // disabled by default to maintain signature FERA wellness palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
