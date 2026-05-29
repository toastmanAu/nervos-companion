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

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val settingsStore = SettingsStore(this)
    currentThemeName = settingsStore.themeName

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
  Box(
    modifier = Modifier.fillMaxSize().background(Color.Black),
    contentAlignment = Alignment.Center
  ) {
    Image(
      painter = painterResource(id = R.drawable.splash_screen),
      contentDescription = "Splash Screen",
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop
    )
  }
}
