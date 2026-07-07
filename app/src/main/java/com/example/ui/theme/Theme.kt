package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkGlassPrimary,
    secondary = DarkGlassSecondary,
    tertiary = DarkGlassTertiary,
    background = DarkGlassBackground,
    surface = DarkGlassSurface,
    surfaceVariant = DarkGlassSurfaceVariant,
    onPrimary = Color.Black,
    onPrimaryContainer = DarkGlassOnPrimaryContainer,
    primaryContainer = DarkGlassPrimaryContainer,
    onBackground = DarkGlassOnBackground,
    onSurface = DarkGlassOnSurface,
    onSurfaceVariant = DarkGlassOnSurfaceVariant,
    outline = DarkGlassBorder
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GlassPrimary,
    secondary = GlassSecondary,
    tertiary = GlassTertiary,
    background = GlassBackground,
    surface = GlassSurface,
    surfaceVariant = GlassSurfaceVariant,
    onPrimary = Color.White,
    onPrimaryContainer = GlassOnPrimaryContainer,
    primaryContainer = GlassPrimaryContainer,
    onBackground = GlassOnBackground,
    onSurface = GlassOnSurface,
    onSurfaceVariant = GlassOnSurfaceVariant,
    outline = GlassBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Custom theme-first, default to false so our glorious "Frosted Glass" theme is seen immediately
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
