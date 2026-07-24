package com.example.nervoscompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.nervoscompanion.data.SettingsStore
import com.example.nervoscompanion.theme.NervosCompanionTheme
import com.example.nervoscompanion.theme.currentThemeName
import kotlinx.coroutines.delay
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.nervoscompanion.data.work.WorkManagerHelper

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val settingsStore = SettingsStore(this)
    currentThemeName = settingsStore.themeName

    // Check and request runtime notification permissions on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
      }
    }

    // Automatically schedule polling task if enabled
    if (settingsStore.isBackgroundSyncEnabled) {
      WorkManagerHelper.schedule(this)
    }

    enableEdgeToEdge()
    setContent {
      NervosCompanionTheme {
        var showSplash by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
          delay(2000)
          showSplash = false
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          if (showSplash) {
            SplashScreen()
          } else {
            MainNavigation()
          }
        }
      }
    }
  }
}

@Composable
fun SplashScreen() {
  val theme = com.example.nervoscompanion.theme.currentTheme
  Box(
    modifier = Modifier.fillMaxSize().background(theme.splashBgColor),
    contentAlignment = Alignment.Center
  ) {
    Image(
      painter = painterResource(id = theme.splashImageResId),
      contentDescription = "Splash Screen",
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop
    )
  }
}
