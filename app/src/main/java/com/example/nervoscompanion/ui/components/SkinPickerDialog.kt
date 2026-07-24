package com.example.nervoscompanion.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.nervoscompanion.data.PanelManifest
import com.example.nervoscompanion.data.ScaleType
import com.example.nervoscompanion.data.SettingsStore
import com.example.nervoscompanion.data.SkinType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinPickerDialog(
  cardId: String,
  cardName: String,
  settingsStore: SettingsStore,
  onDismissRequest: () -> Unit,
  onSkinApplied: () -> Unit
) {
  val context = LocalContext.current
  val group = remember(cardId) { PanelManifest.getGroupForCard(cardId) }

  var selectedSkinType by remember { mutableStateOf(settingsStore.getCardSkinType(cardId)) }
  var selectedPath by remember { mutableStateOf(settingsStore.getCardSkinPath(cardId)) }
  var selectedScale by remember { mutableStateOf(settingsStore.getCardScaleType(cardId)) }

  val defaultUrl = remember(cardId) { PanelManifest.getDefaultAssetUrl(cardId) }

  // Media picker launcher
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        try {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
          )
        } catch (e: Exception) {
          e.printStackTrace()
        }
        selectedPath = uri.toString()
        selectedSkinType = SkinType.CUSTOM
      }
    }
  )

  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.9f)
        .padding(16.dp),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
      ) {
        // Header
        Text(
          text = "Reskin Card: $cardName",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 1. Live Preview Panel
        Text(
          text = "Live Preview",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        ) {
          val contentScale = if (selectedScale == ScaleType.CROP) ContentScale.Crop else ContentScale.FillBounds

          // Background renderer
          when (selectedSkinType) {
            SkinType.DEFAULT -> {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(
                    Brush.linearGradient(
                      colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                    )
                  )
              )
            }
            SkinType.WEBSITE -> {
              val url = selectedPath ?: defaultUrl
              if (url != null) {
                SubcomposeAsyncImage(
                  model = url,
                  contentDescription = "Preview",
                  contentScale = contentScale,
                  modifier = Modifier.fillMaxSize(),
                  loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                      CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    }
                  },
                  error = {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                      Text("Failed to load image", color = Color.Red, fontSize = 12.sp)
                    }
                  }
                )
              } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Gray))
              }
            }
            SkinType.CUSTOM -> {
              if (selectedPath != null) {
                SubcomposeAsyncImage(
                  model = Uri.parse(selectedPath),
                  contentDescription = "Custom Preview",
                  contentScale = contentScale,
                  modifier = Modifier.fillMaxSize(),
                  loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                      CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    }
                  },
                  error = {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                      Text("Failed to load image", color = Color.Red, fontSize = 12.sp)
                    }
                  }
                )
              }
            }
          }

          // Card content overlay mockup
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
              .padding(16.dp),
            contentAlignment = Alignment.BottomStart
          ) {
            Column {
              Text(text = cardName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
              Text(text = "Themed layout preview", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Scale Type Selector
        Text(
          text = "Display Style",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          listOf(ScaleType.CROP to "Center Crop (Scale)", ScaleType.STRETCH to "Stretch Fill (Fit)").forEach { (type, label) ->
            val isSelected = selectedScale == type
            OutlinedButton(
              onClick = { selectedScale = type },
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
              ),
              border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            ) {
              Text(label, fontSize = 12.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Selection Tabs
        Text(
          text = "Skin Source",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Preset designs list from github
        if (group != null && group.panels.isNotEmpty()) {
          Text(
            text = "Website Designs (ckb.directory)",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp)
          )

          LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            items(group.panels) { panel ->
              val panelUrl = "${PanelManifest.GITHUB_ASSETS_BASE_URL}${panel.filePath}"
              val isSelected = selectedSkinType == SkinType.WEBSITE && selectedPath == panelUrl

              Box(
                modifier = Modifier
                  .aspectRatio(1.5f)
                  .clip(RoundedCornerShape(8.dp))
                  .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp)
                  )
                  .clickable {
                    selectedSkinType = SkinType.WEBSITE
                    selectedPath = panelUrl
                  }
              ) {
                AsyncImage(
                  model = panelUrl,
                  contentDescription = panel.name,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
                if (isSelected) {
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = "Selected",
                      tint = Color.White
                    )
                  }
                }
              }
            }
          }
        } else {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
              .padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.Info, contentDescription = "No presets", tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "No preset website designs available for this card.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Custom Upload Button
          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.weight(1.1f)
          ) {
            Text("Pick Custom Photo", fontSize = 11.sp, maxLines = 1)
          }

          // Reset Gradient Button
          OutlinedButton(
            onClick = {
              selectedSkinType = SkinType.DEFAULT
              selectedPath = null
            },
            modifier = Modifier.weight(0.9f)
          ) {
            Text("Default Gradient", fontSize = 11.sp, maxLines = 1)
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bottom Apply / Cancel Controls
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismissRequest) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              // Apply configurations
              settingsStore.setCardSkinType(cardId, selectedSkinType)
              settingsStore.setCardSkinPath(cardId, selectedPath)
              settingsStore.setCardScaleType(cardId, selectedScale)

              onSkinApplied()
              onDismissRequest()
            }
          ) {
            Text("Apply Skin")
          }
        }
      }
    }
  }
}
