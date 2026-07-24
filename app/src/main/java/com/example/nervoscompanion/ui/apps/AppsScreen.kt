package com.example.nervoscompanion.ui.apps

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.nervoscompanion.data.SkinType
import com.example.nervoscompanion.data.ScaleType
import com.example.nervoscompanion.data.PanelManifest
import com.example.nervoscompanion.ui.components.SkinPickerDialog
import com.example.nervoscompanion.ui.components.TabHeader
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
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
import androidx.navigation3.runtime.NavKey
import com.example.nervoscompanion.WebBrowser

@Composable
fun AppsScreen(onNavigate: (NavKey) -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val settingsStore = remember { SettingsStore(context) }
  val appsRepository = remember {
    val db = com.example.nervoscompanion.data.cache.AppDatabase.getDatabase(context)
    AppsRepository(db.ecosystemAppDao(), settingsStore)
  }
  val coroutineScope = rememberCoroutineScope()

  var appsList by remember { mutableStateOf<List<EcosystemApp>>(emptyList()) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  var searchQuery by remember { mutableStateOf("") }
  var showFavouritesOnly by remember { mutableStateOf(false) }
  var favouriteApps by remember { mutableStateOf(settingsStore.getFavouriteApps()) }
  var releaseAlertApps by remember { mutableStateOf(settingsStore.getReleaseAlertApps()) }
  var supportEmail by remember { mutableStateOf("phill@wyltek.com") }

  var showPickerDialog by remember { mutableStateOf(false) }
  var editingAppId by remember { mutableStateOf("") }
  var editingAppName by remember { mutableStateOf("") }
  var refreshTrigger by remember { mutableStateOf(0) }

  fun loadApps() {
    isLoading = true
    errorMsg = null
    coroutineScope.launch {
      try {
        // Randomize the order of apps when loading for fairness of visibility
        appsList = appsRepository.fetchApps().shuffled()
        supportEmail = appsRepository.fetchSupportEmail()
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

  val filteredApps = appsList.filter { app ->
    val matchesSearch = app.name.contains(searchQuery, ignoreCase = true) ||
        app.description.contains(searchQuery, ignoreCase = true)
    val matchesFavourite = !showFavouritesOnly || favouriteApps.contains(app.name)
    matchesSearch && matchesFavourite
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      TabHeader(title = "Ecosystem Directory")
      IconButton(onClick = { loadApps() }, enabled = !isLoading) {
        Icon(Icons.Default.Refresh, contentDescription = "Refresh & Reshuffle")
      }
    }

    // Search and Filters layout
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search apps...") },
        modifier = Modifier.weight(1f),
        singleLine = true
      )

      ElevatedFilterChip(
        selected = showFavouritesOnly,
        onClick = { showFavouritesOnly = !showFavouritesOnly },
        label = { Text("Favourites") },
        leadingIcon = {
          Icon(
            imageVector = if (showFavouritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Filter Favourites",
            tint = if (showFavouritesOnly) Color.Red else Color.Gray
          )
        }
      )
    }

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
    } else if (filteredApps.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No apps found.")
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        items(filteredApps) { app ->
          val isFavourite = favouriteApps.contains(app.name)
          val isReleaseAlert = releaseAlertApps.contains(app.name)
          EcosystemAppCard(
            app = app,
            isFavourite = isFavourite,
            onFavouriteToggle = {
              settingsStore.toggleFavouriteApp(app.name)
              favouriteApps = settingsStore.getFavouriteApps()
            },
            isReleaseAlert = isReleaseAlert,
            onReleaseAlertToggle = {
              settingsStore.toggleReleaseAlertApp(app.name)
              releaseAlertApps = settingsStore.getReleaseAlertApps()
            },
            onLinkClick = { url ->
              if (url.contains("daoview.org")) {
                onNavigate(WebBrowser(url = url, title = app.name))
              } else {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
              }
            },
            settingsStore = settingsStore,
            refreshTrigger = refreshTrigger,
            onReskinClick = {
              editingAppId = app.name
              editingAppName = app.name
              showPickerDialog = true
            }
          )
        }
        
        item {
          Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp)
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "Are you a developer?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Submit a request to add your Nervos project to this directory or update existing listing details.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                OutlinedButton(
                  onClick = {
                    val body = """
                      Please fill out the details below to request adding your application:
                      
                      Application Name: 
                      Short Description: 
                      Website URL: 
                      Twitter URL (Optional): 
                      GitHub URL (Optional): 
                      Discord URL (Optional): 
                      Banner Gradient Hex Colors (comma-separated, e.g., #8A2387, #E94057): 
                    """.trimIndent()
                    launchEmailIntent(context, supportEmail, "[CKB Directory] Ecosystem App Submission", body)
                  },
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Submit App", fontSize = 12.sp)
                }
                
                OutlinedButton(
                  onClick = {
                    val body = """
                      Please describe the details you would like to update:
                      
                      Application Name: 
                      Updated Description: 
                      Updated Website URL: 
                      Updated Twitter URL: 
                      Updated GitHub URL: 
                      Updated Discord URL: 
                      Updated Banner Gradient Hex Colors: 
                    """.trimIndent()
                    launchEmailIntent(context, supportEmail, "[CKB Directory] Ecosystem App Update Request", body)
                  },
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Update App", fontSize = 12.sp)
                }
              }
            }
          }
        }
      }
    }
  }

  if (showPickerDialog) {
    SkinPickerDialog(
      cardId = editingAppId,
      cardName = editingAppName,
      settingsStore = settingsStore,
      onDismissRequest = { showPickerDialog = false },
      onSkinApplied = { refreshTrigger++ }
    )
  }
}

fun launchEmailIntent(context: android.content.Context, to: String, subject: String, body: String) {
  val intent = Intent(Intent.ACTION_SENDTO).apply {
    data = Uri.parse("mailto:")
    putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
    putExtra(Intent.EXTRA_SUBJECT, subject)
    putExtra(Intent.EXTRA_TEXT, body)
  }
  try {
    context.startActivity(Intent.createChooser(intent, "Send Email Using..."))
  } catch (e: Exception) {
    android.widget.Toast.makeText(context, "No email client found", android.widget.Toast.LENGTH_SHORT).show()
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EcosystemAppCard(
  app: EcosystemApp,
  isFavourite: Boolean,
  onFavouriteToggle: () -> Unit,
  isReleaseAlert: Boolean,
  onReleaseAlertToggle: () -> Unit,
  onLinkClick: (String) -> Unit,
  settingsStore: SettingsStore,
  refreshTrigger: Int,
  onReskinClick: () -> Unit
) {
  val skinType = remember(app.name, refreshTrigger) { settingsStore.getCardSkinType(app.name) }
  val skinPath = remember(app.name, refreshTrigger) { settingsStore.getCardSkinPath(app.name) }
  val scaleType = remember(app.name, refreshTrigger) { settingsStore.getCardScaleType(app.name) }

  val contentScale = if (scaleType == ScaleType.CROP) ContentScale.Crop else ContentScale.FillBounds
  val defaultUrl = remember(app.name) { PanelManifest.getDefaultAssetUrl(app.name) }

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
          .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
          .background(
            Brush.linearGradient(
              colors = app.bannerGradientColors.map { Color(it) }
            )
          )
          .clickable { onLinkClick(app.websiteUrl) },
        contentAlignment = Alignment.BottomStart
      ) {
        val imageModel = when (skinType) {
          SkinType.DEFAULT -> defaultUrl
          SkinType.WEBSITE -> skinPath ?: defaultUrl
          SkinType.CUSTOM -> skinPath
        }

        if (imageModel != null) {
          AsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier.matchParentSize()
          )
        }

        Box(
          modifier = Modifier
            .matchParentSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.6f))
              )
            )
        )

        // Action Buttons in the Top-Right of the Card
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.Top
        ) {
          if (!app.githubUrl.isNullOrEmpty()) {
            IconButton(
              onClick = { onReleaseAlertToggle() }
            ) {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Toggle Release Alerts",
                tint = if (isReleaseAlert) Color(0xFF00FFCC) else Color.White,
                modifier = Modifier
                  .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(50))
                  .padding(8.dp)
              )
            }
          }
          IconButton(
            onClick = { onFavouriteToggle() }
          ) {
            Icon(
              imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
              contentDescription = "Toggle Favourite",
              tint = if (isFavourite) Color.Red else Color.White,
              modifier = Modifier
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(50))
                .padding(8.dp)
            )
          }

          // Reskin Paintbrush icon button
          IconButton(
            onClick = { onReskinClick() }
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Reskin Card",
              tint = Color.White,
              modifier = Modifier
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(50))
                .padding(8.dp)
            )
          }
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
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
      FlowRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = { onLinkClick(app.websiteUrl) },
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
          )
        ) {
          Text("Website", fontSize = 12.sp)
        }

        app.twitterUrl?.let { url ->
          OutlinedButton(
            onClick = { onLinkClick(url) }
          ) {
            Text("X / Twitter", fontSize = 12.sp)
          }
        }

        app.githubUrl?.let { url ->
          OutlinedButton(
            onClick = { onLinkClick(url) }
          ) {
            Text("GitHub", fontSize = 12.sp)
          }
        }

        app.discordUrl?.let { url ->
          OutlinedButton(
            onClick = { onLinkClick(url) }
          ) {
            Text("Discord", fontSize = 12.sp)
          }
        }
      }
    }
  }
}
