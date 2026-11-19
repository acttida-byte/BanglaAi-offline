package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = ObsidianBg,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = IndigoSecondary,
    onSecondary = ObsidianBg,
    secondaryContainer = IndigoContainer,
    onSecondaryContainer = OnIndigoContainer,
    tertiary = CoralTertiary,
    onTertiary = ObsidianBg,
    tertiaryContainer = CoralContainer,
    onTertiaryContainer = OnCoralContainer,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = SlateSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = SlateOutline,
    error = MoodFuriousColor,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DeepTealPrimary,
    onPrimary = Color.White,
    primaryContainer = SoftTealContainer,
    onPrimaryContainer = OnSoftTealContainer,
    secondary = IndigoSecondary,
    onSecondary = Color.White,
    secondaryContainer = OnIndigoContainer,
    onSecondaryContainer = IndigoContainer,
    tertiary = CoralTertiary,
    onTertiary = Color.White,
    tertiaryContainer = OnCoralContainer,
    onTertiaryContainer = CoralContainer,
    background = WarmPaperBg,
    onBackground = TextPrimaryLight,
    surface = CleanCardSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = SoftSlateVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    error = MoodFuriousColor,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
