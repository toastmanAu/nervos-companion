package com.example.nervoscompanion.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.nervoscompanion.CkbConsole
import com.example.nervoscompanion.DaoViewer

@Composable
fun ToolsScreen(onNavigate: (NavKey) -> Unit, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    Text(
      text = "Network Tools",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )

    // 1. DAO Viewer tool button
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onNavigate(DaoViewer) },
      shape = RoundedCornerShape(12.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.linearGradient(
              colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
            )
          )
          .padding(24.dp)
      ) {
        Column {
          Text(
            text = "Nervos DAO Viewer",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Embed and browse daoview.org directly in the app. Monitor active DAO deposits, withdrawal epochs, and system statistics.",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp
          )
        }
      }
    }

    // 2. CKB RPC Console tool button
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onNavigate(CkbConsole) },
      shape = RoundedCornerShape(12.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.linearGradient(
              colors = listOf(Color(0xFF1D976C), Color(0xFF93F9B9))
            )
          )
          .padding(24.dp)
      ) {
        Column {
          Text(
            text = "CKB RPC Console",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Bitcoin Core console-inspired terminal. Query nodes with custom methods, view histories, and inspect pretty-printed JSON results.",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp
          )
        }
      }
    }
  }
}
