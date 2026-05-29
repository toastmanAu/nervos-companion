package com.example.nervoscompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.nervoscompanion.data.SettingsStore
import com.example.nervoscompanion.theme.NervosCompanionTheme
import com.example.nervoscompanion.theme.currentThemeName

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val settingsStore = SettingsStore(this)
    currentThemeName = settingsStore.themeName

    enableEdgeToEdge()
    setContent {
      NervosCompanionTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
