package com.example.cardpulse.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Indigo500,
    onPrimary = Slate50,
    primaryContainer = Indigo600,
    onPrimaryContainer = Indigo400,
    secondary = Emerald500,
    onSecondary = Slate950,
    secondaryContainer = Emerald500.copy(alpha = 0.2f),
    onSecondaryContainer = Emerald400,
    tertiary = Coral500,
    onTertiary = Slate950,
    tertiaryContainer = Coral600.copy(alpha = 0.2f),
    onTertiaryContainer = Coral500,
    error = Red500,
    onError = Slate50,
    errorContainer = Red500.copy(alpha = 0.2f),
    onErrorContainer = Red400,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    surfaceContainerLowest = Slate950,
    surfaceContainerLow = Slate900,
    surfaceContainer = Slate800,
    surfaceContainerHigh = Slate700,
    surfaceContainerHighest = Slate600,
    outline = Slate700,
    outlineVariant = Slate800,
    inverseSurface = Slate100,
    inverseOnSurface = Slate900,
    inversePrimary = Indigo600
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Slate50,
    primaryContainer = Indigo400.copy(alpha = 0.15f),
    onPrimaryContainer = Indigo600,
    secondary = Emerald500,
    onSecondary = Slate50,
    secondaryContainer = Emerald400.copy(alpha = 0.15f),
    onSecondaryContainer = Emerald500,
    tertiary = Coral600,
    onTertiary = Slate50,
    tertiaryContainer = Coral500.copy(alpha = 0.15f),
    onTertiaryContainer = Coral600,
    error = Red500,
    onError = Slate50,
    errorContainer = Red400.copy(alpha = 0.15f),
    onErrorContainer = Red500,
    background = Slate50,
    onBackground = Slate900,
    surface = Slate50,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    surfaceContainerLowest = Slate50,
    surfaceContainerLow = Slate100,
    surfaceContainer = Slate200,
    surfaceContainerHigh = Slate300,
    surfaceContainerHighest = Slate400,
    outline = Slate300,
    outlineVariant = Slate200,
    inverseSurface = Slate900,
    inverseOnSurface = Slate100,
    inversePrimary = Indigo400
)

@Composable
fun CardPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CardPulseTypography,
        content = content
    )
}
