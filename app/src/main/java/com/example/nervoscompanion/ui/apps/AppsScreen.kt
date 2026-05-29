package com.example.nervoscompanion.ui.apps

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nervoscompanion.data.AppsRepository
import com.example.nervoscompanion.data.EcosystemApp
import com.example.nervoscompanion.data.SettingsStore
import kotlinx.coroutines.launch

@Composable
fun AppsScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val settingsStore = remember { SettingsStore(context) }
  val appsRepository = remember { AppsRepository(settingsStore) }
  val coroutineScope = rememberCoroutineScope()

  var appsList by remember { mutableStateOf<List<EcosystemApp>>(emptyList()) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  fun loadApps() {
    isLoading = true
    errorMsg = null
    coroutineScope.launch {
      try {
        appsList = appsRepository.fetchApps()
      } catch (e: Exception) {
        errorMsg = "Failed to load apps: ${e.localizedMessage}"
      } finally {
        isLoading = false
      }
    }
  }

  LaunchedEffect(Unit) {
    loadApps()
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "Ecosystem Directory",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )

    if (isLoading) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else if (errorMsg != null) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = errorMsg ?: "", color = MaterialTheme.colorScheme.error)
          Spacer(modifier = Modifier.height(8.dp))
          Button(onClick = { loadApps() }) {
            Text("Retry")
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        items(appsList) { app ->
          EcosystemAppCard(app = app) { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
          }
        }
      }
    }
  }
}

@Composable
fun EcosystemAppCard(app: EcosystemApp, onLinkClick: (String) -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Column {
      // 800x320 (8:3) Aspect Ratio Artwork Panel
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(8f / 3f)
          .background(
            Brush.linearGradient(
              colors = app.bannerGradientColors.map { Color(it) }
            )
          )
          .clickable { onLinkClick(app.websiteUrl) }
          .padding(16.dp),
        contentAlignment = Alignment.BottomStart
      ) {
        Column {
          Text(
            text = app.name,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = app.description,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            lineHeight = 16.sp
          )
        }
      }

      // Action Buttons Under the Panel
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = { onLinkClick(app.websiteUrl) },
          modifier = Modifier.padding(horizontal = 4.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
          )
        ) {
          Text("Website", fontSize = 12.sp)
        }

        app.twitterUrl?.let { url ->
          OutlinedButton(
            onClick = { onLinkClick(url) },
            modifier = Modifier.padding(horizontal = 4.dp)
          ) {
            Text("X / Twitter", fontSize = 12.sp)
          }
        }

        app.githubUrl?.let { url ->
          OutlinedButton(
            onClick = { onLinkClick(url) },
            modifier = Modifier.padding(horizontal = 4.dp)
          ) {
            Text("GitHub", fontSize = 12.sp)
          }
        }

        app.discordUrl?.let { url ->
          OutlinedButton(
            onClick = { onLinkClick(url) },
            modifier = Modifier.padding(horizontal = 4.dp)
          ) {
            Text("Discord", fontSize = 12.sp)
          }
        }
      }
    }
  }
}
