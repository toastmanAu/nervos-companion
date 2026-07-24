package com.example.nervoscompanion

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
import com.example.nervoscompanion.ui.tools.FiberHomeScreen
import com.example.nervoscompanion.ui.tools.TxCalculatorScreen
import com.example.nervoscompanion.ui.tools.DaoDashboardScreen
import com.example.nervoscompanion.ui.tools.RfcViewerScreen
import com.example.nervoscompanion.ui.tools.RfcDetailScreen
import com.example.nervoscompanion.ui.settings.SettingsScreen
import com.example.nervoscompanion.theme.currentTheme

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
          Tools, is WebBrowser, CkbConsole, FiberHome, TxCalculator, DaoDashboard, RfcViewer, is RfcDetail -> Tools
          Settings -> Settings
          else -> Home
        }

        val theme = currentTheme
        val navItemColors = NavigationBarItemDefaults.colors(
          selectedTextColor = theme.colorScheme.primary,
          unselectedTextColor = Color.Gray,
          indicatorColor = theme.colorScheme.primary.copy(alpha = 0.15f)
        )

        NavigationBarItem(
          selected = selectedTab == Home,
          onClick = {
            if (currentKey != Home) {
              backStack.clear()
              backStack.add(Home)
            }
          },
          icon = { Icon(painterResource(id = theme.homeIconResId), contentDescription = "Home", tint = Color.Unspecified, modifier = Modifier.size(28.dp)) },
          label = { Text("Home") },
          colors = navItemColors
        )

        NavigationBarItem(
          selected = selectedTab == News,
          onClick = {
            if (currentKey != News) {
              backStack.clear()
              backStack.add(News)
            }
          },
          icon = { Icon(painterResource(id = theme.newsIconResId), contentDescription = "News", tint = Color.Unspecified, modifier = Modifier.size(28.dp)) },
          label = { Text("News") },
          colors = navItemColors
        )

        NavigationBarItem(
          selected = selectedTab == Apps,
          onClick = {
            if (currentKey != Apps) {
              backStack.clear()
              backStack.add(Apps)
            }
          },
          icon = { Icon(painterResource(id = theme.appsIconResId), contentDescription = "Apps", tint = Color.Unspecified, modifier = Modifier.size(28.dp)) },
          label = { Text("Apps") },
          colors = navItemColors
        )

        NavigationBarItem(
          selected = selectedTab == Tools,
          onClick = {
            if (selectedTab != Tools) {
              backStack.clear()
              backStack.add(Tools)
            }
          },
          icon = { Icon(painterResource(id = theme.toolsIconResId), contentDescription = "Tools", tint = Color.Unspecified, modifier = Modifier.size(28.dp)) },
          label = { Text("Tools") },
          colors = navItemColors
        )

        NavigationBarItem(
          selected = selectedTab == Settings,
          onClick = {
            if (currentKey != Settings) {
              backStack.clear()
              backStack.add(Settings)
            }
          },
          icon = { Icon(painterResource(id = theme.settingsIconResId), contentDescription = "Settings", tint = Color.Unspecified, modifier = Modifier.size(28.dp)) },
          label = { Text("Settings") },
          colors = navItemColors
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
          AppsScreen(onNavigate = { key -> backStack.add(key) })
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
        entry<FiberHome> {
          FiberHomeScreen(onNavigate = { key -> backStack.add(key) })
        }
        entry<TxCalculator> {
          TxCalculatorScreen(onNavigate = { key -> backStack.add(key) })
        }
        entry<DaoDashboard> {
          DaoDashboardScreen(onNavigate = { key -> backStack.add(key) })
        }
        entry<RfcViewer> {
          RfcViewerScreen(onNavigate = { key -> backStack.add(key) })
        }
        entry<RfcDetail> { key ->
          RfcDetailScreen(rfcNumber = key.rfcNumber, onNavigate = { key -> backStack.add(key) })
        }
      }
    )
  }
}
