package com.example.nervoscompanion.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.nervoscompanion.R

// Global state for theme switching dynamically
var currentThemeName by mutableStateOf("emerald")

val EmeraldColorScheme = darkColorScheme(
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

val CyberpunkColorScheme = darkColorScheme(
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

val OceanColorScheme = darkColorScheme(
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

val ObsidianColorScheme = darkColorScheme(
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

val EmeraldTheme = AppTheme(
  id = "emerald",
  displayName = "Emerald Forest",
  colorScheme = EmeraldColorScheme,
  splashBgColor = Color.Black,
  splashImageResId = R.drawable.splash_screen,
  heroImageResId = R.drawable.hero_emerald,
  bgGradientColors = null,
  headerStyle = HeaderStyle("classic", false, null),
  homeIconResId = R.drawable.ic_home_bespoke,
  newsIconResId = R.drawable.ic_news_bespoke,
  appsIconResId = R.drawable.ic_apps_bespoke,
  toolsIconResId = R.drawable.ic_tools_bespoke,
  settingsIconResId = R.drawable.ic_settings_bespoke,
  homeHeaderImageResId = R.drawable.header_home_emerald
)

val CyberpunkTheme = AppTheme(
  id = "cyberpunk",
  displayName = "Cyberpunk Neon",
  colorScheme = CyberpunkColorScheme,
  splashBgColor = Color(0xFF220822),
  splashImageResId = R.drawable.splash_cyberpunk,
  heroImageResId = R.drawable.hero_cyberpunk,
  bgGradientColors = listOf(Color(0xFF0D020D), Color(0xFF220822)),
  headerStyle = HeaderStyle("neon", true, listOf(Color(0xFFF27121), Color(0xFFE94057))),
  homeIconResId = R.drawable.ic_home_bespoke,
  newsIconResId = R.drawable.ic_news_bespoke,
  appsIconResId = R.drawable.ic_apps_bespoke,
  toolsIconResId = R.drawable.ic_tools_bespoke,
  settingsIconResId = R.drawable.ic_settings_bespoke
)

val OceanTheme = AppTheme(
  id = "ocean",
  displayName = "Midnight Ocean",
  colorScheme = OceanColorScheme,
  splashBgColor = Color(0xFF0E1A2B),
  splashImageResId = R.drawable.splash_ocean,
  heroImageResId = R.drawable.hero_ocean,
  bgGradientColors = listOf(Color(0xFF040B14), Color(0xFF0E1A2B)),
  headerStyle = HeaderStyle("ocean", false, listOf(Color(0xFF00C6FF), Color(0xFF0072FF))),
  homeIconResId = R.drawable.ic_home_bespoke,
  newsIconResId = R.drawable.ic_news_bespoke,
  appsIconResId = R.drawable.ic_apps_bespoke,
  toolsIconResId = R.drawable.ic_tools_bespoke,
  settingsIconResId = R.drawable.ic_settings_bespoke
)

val StealthTheme = AppTheme(
  id = "stealth",
  displayName = "Obsidian Stealth",
  colorScheme = ObsidianColorScheme,
  splashBgColor = Color(0xFF141414),
  splashImageResId = R.drawable.splash_stealth,
  heroImageResId = R.drawable.hero_stealth,
  bgGradientColors = listOf(Color(0xFF0A0A0A), Color(0xFF141414)),
  headerStyle = HeaderStyle("stealth", false, listOf(Color(0xFFECEFF1), Color(0xFF37474F))),
  homeIconResId = R.drawable.ic_home_bespoke,
  newsIconResId = R.drawable.ic_news_bespoke,
  appsIconResId = R.drawable.ic_apps_bespoke,
  toolsIconResId = R.drawable.ic_tools_bespoke,
  settingsIconResId = R.drawable.ic_settings_bespoke
)

val currentTheme: AppTheme
  get() = when (currentThemeName) {
    "cyberpunk" -> CyberpunkTheme
    "ocean" -> OceanTheme
    "stealth" -> StealthTheme
    else -> EmeraldTheme
  }

@Composable
fun NervosCompanionTheme(
  content: @Composable () -> Unit,
) {
  val theme = currentTheme
  val colorScheme = theme.colorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = {
      val bgModifier = if (theme.bgGradientColors != null) {
        Modifier.background(Brush.verticalGradient(theme.bgGradientColors))
      } else {
        Modifier.background(colorScheme.background)
      }
      Box(modifier = Modifier.fillMaxSize().then(bgModifier)) {
        content()
      }
    }
  )
}
