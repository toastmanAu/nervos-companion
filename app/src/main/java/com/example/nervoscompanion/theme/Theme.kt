package com.example.nervoscompanion.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Global state for theme switching dynamically
var currentThemeName by mutableStateOf("emerald")

private val EmeraldColorScheme = darkColorScheme(
  primary = EmeraldPrimary,
  secondary = EmeraldSecondary,
  background = EmeraldBackground,
  surface = EmeraldSurface,
  surfaceVariant = EmeraldSurface,
  onPrimary = Color.Black,
  onSecondary = Color.White,
  onBackground = Color.White,
  onSurface = Color.White
)

private val CyberpunkColorScheme = darkColorScheme(
  primary = CyberpunkSecondary,
  secondary = CyberpunkPrimary,
  background = CyberpunkBackground,
  surface = CyberpunkSurface,
  surfaceVariant = CyberpunkSurface,
  onPrimary = Color.Black,
  onSecondary = Color.White,
  onBackground = Color.White,
  onSurface = Color.White
)

private val OceanColorScheme = darkColorScheme(
  primary = OceanPrimary,
  secondary = OceanSecondary,
  background = OceanBackground,
  surface = OceanSurface,
  surfaceVariant = OceanSurface,
  onPrimary = Color.Black,
  onSecondary = Color.White,
  onBackground = Color.White,
  onSurface = Color.White
)

private val ObsidianColorScheme = darkColorScheme(
  primary = ObsidianPrimary,
  secondary = ObsidianSecondary,
  background = ObsidianBackground,
  surface = ObsidianSurface,
  surfaceVariant = ObsidianSurface,
  onPrimary = Color.Black,
  onSecondary = Color.White,
  onBackground = Color.White,
  onSurface = Color.White
)

@Composable
fun NervosCompanionTheme(
  content: @Composable () -> Unit,
) {
  val colorScheme = when (currentThemeName) {
    "cyberpunk" -> CyberpunkColorScheme
    "ocean" -> OceanColorScheme
    "stealth" -> ObsidianColorScheme
    else -> EmeraldColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
