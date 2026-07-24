package com.example.nervoscompanion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nervoscompanion.theme.currentTheme

@Composable
fun TabHeader(
  title: String,
  modifier: Modifier = Modifier
) {
  val theme = currentTheme
  val style = theme.headerStyle

  when (style.fontStyle) {
    "neon" -> {
      Text(
        text = title,
        modifier = modifier,
        style = TextStyle(
          brush = Brush.horizontalGradient(
            style.textGradient ?: listOf(theme.colorScheme.primary, theme.colorScheme.secondary)
          ),
          fontSize = 26.sp,
          fontWeight = FontWeight.ExtraBold,
          shadow = Shadow(
            color = theme.colorScheme.primary.copy(alpha = 0.6f),
            blurRadius = 10f
          )
        )
      )
    }
    "ocean" -> {
      Column(modifier = modifier) {
        Text(
          text = title,
          style = TextStyle(
            brush = Brush.horizontalGradient(
              style.textGradient ?: listOf(theme.colorScheme.primary, Color.White)
            ),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
          )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
          modifier = Modifier
            .width(50.dp)
            .height(3.dp)
            .background(theme.colorScheme.primary)
        )
      }
    }
    "stealth" -> {
      Text(
        text = title.uppercase(),
        modifier = modifier,
        color = theme.colorScheme.primary,
        fontSize = 22.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace
      )
    }
    else -> {
      // Classic Emerald / Default
      Text(
        text = title,
        modifier = modifier,
        color = theme.colorScheme.primary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}
