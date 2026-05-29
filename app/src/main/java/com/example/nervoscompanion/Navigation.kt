package com.example.nervoscompanion

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.nervoscompanion.ui.home.HomeScreen
import com.example.nervoscompanion.ui.news.NewsScreen
import com.example.nervoscompanion.ui.apps.AppsScreen
import com.example.nervoscompanion.ui.tools.ToolsScreen
import com.example.nervoscompanion.ui.tools.ConsoleScreen
import com.example.nervoscompanion.ui.tools.DaoViewerScreen
import com.example.nervoscompanion.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Home)
  val currentKey = backStack.lastOrNull() ?: Home

  Scaffold(
    bottomBar = {
      NavigationBar {
        val selectedTab = when (currentKey) {
          Home -> Home
          News -> News
          Apps -> Apps
          Tools, is WebBrowser, CkbConsole -> Tools
          Settings -> Settings
          else -> Home
        }

        NavigationBarItem(
          selected = selectedTab == Home,
          onClick = {
            if (currentKey != Home) {
              backStack.clear()
              backStack.add(Home)
            }
          },
          icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
          label = { Text("Home") }
        )

        NavigationBarItem(
          selected = selectedTab == News,
          onClick = {
            if (currentKey != News) {
              backStack.clear()
              backStack.add(News)
            }
          },
          icon = { Icon(Icons.Default.List, contentDescription = "News") },
          label = { Text("News") }
        )

        NavigationBarItem(
          selected = selectedTab == Apps,
          onClick = {
            if (currentKey != Apps) {
              backStack.clear()
              backStack.add(Apps)
            }
          },
          icon = { Icon(Icons.Default.Info, contentDescription = "Apps") },
          label = { Text("Apps") }
        )

        NavigationBarItem(
          selected = selectedTab == Tools,
          onClick = {
            if (selectedTab != Tools) {
              backStack.clear()
              backStack.add(Tools)
            }
          },
          icon = { Icon(Icons.Default.Build, contentDescription = "Tools") },
          label = { Text("Tools") }
        )

        NavigationBarItem(
          selected = selectedTab == Settings,
          onClick = {
            if (currentKey != Settings) {
              backStack.clear()
              backStack.add(Settings)
            }
          },
          icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
          label = { Text("Settings") }
        )
      }
    }
  ) { innerPadding ->
    NavDisplay(
      backStack = backStack,
      onBack = {
        if (backStack.size > 1) {
          backStack.removeLastOrNull()
        }
      },
      modifier = Modifier.padding(innerPadding),
      entryProvider = entryProvider {
        entry<Home> {
          HomeScreen(onNavigate = { key -> backStack.add(key) })
        }
        entry<News> {
          NewsScreen()
        }
        entry<Apps> {
          AppsScreen()
        }
        entry<Tools> {
          ToolsScreen(onNavigate = { key -> backStack.add(key) })
        }
        entry<Settings> {
          SettingsScreen()
        }
        entry<WebBrowser> { key ->
          DaoViewerScreen(url = key.url)
        }
        entry<CkbConsole> {
          ConsoleScreen()
        }
      }
    )
  }
}
