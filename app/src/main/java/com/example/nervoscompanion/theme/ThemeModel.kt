package com.example.nervoscompanion.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

data class AppTheme(
  val id: String,
  val displayName: String,
  val colorScheme: androidx.compose.material3.ColorScheme,
  val splashBgColor: Color,
  val splashImageResId: Int,
  val heroImageResId: Int,
  val bgGradientColors: List<Color>?,
  val headerStyle: HeaderStyle,
  
  // Custom theme-specific tab icons
  val homeIconResId: Int,
  val newsIconResId: Int,
  val appsIconResId: Int,
  val toolsIconResId: Int,
  val settingsIconResId: Int,
  val homeHeaderImageResId: Int? = null
)

data class HeaderStyle(
  val fontStyle: String, // "classic", "neon", "ocean", "stealth"
  val showGlow: Boolean,
  val textGradient: List<Color>? = null
)
