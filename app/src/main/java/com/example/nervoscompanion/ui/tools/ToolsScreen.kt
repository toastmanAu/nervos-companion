package com.example.nervoscompanion.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.nervoscompanion.CkbConsole
import com.example.nervoscompanion.WebBrowser
import com.example.nervoscompanion.FiberHome
import com.example.nervoscompanion.TxCalculator
import com.example.nervoscompanion.DaoDashboard
import com.example.nervoscompanion.RfcViewer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.nervoscompanion.data.SettingsStore
import com.example.nervoscompanion.data.SkinType
import com.example.nervoscompanion.data.ScaleType
import com.example.nervoscompanion.data.PanelManifest
import com.example.nervoscompanion.ui.components.SkinPickerDialog
import com.example.nervoscompanion.ui.components.TabHeader

data class ToolItem(
  val id: String,
  val name: String,
  val description: String,
  val url: String?, // null for local Console
  val tags: List<String>,
  val gradientColors: List<Color>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolsScreen(onNavigate: (NavKey) -> Unit, modifier: Modifier = Modifier) {
  var selectedTag by remember { mutableStateOf("All") }
  val context = LocalContext.current
  val settingsStore = remember { SettingsStore(context) }

  var showPickerDialog by remember { mutableStateOf(false) }
  var editingToolId by remember { mutableStateOf("") }
  var editingToolName by remember { mutableStateOf("") }
  var refreshTrigger by remember { mutableStateOf(0) }

  val tags = listOf("All", "dev", "defi", "chain", "nft/dob", "fun", "learning")

  val tools = listOf(
    ToolItem(
      id = "dao_dashboard",
      name = "DAO Portfolio Tracker",
      description = "Monitor multiple watch-only wallet addresses, auto-discover active DAO cells, aggregate yields, and track 180-epoch maturity lock countdowns.",
      url = null,
      tags = listOf("defi", "chain"),
      gradientColors = listOf(Color(0xFF11998E), Color(0xFF38EF7D))
    ),
    ToolItem(
      id = "tx_calculator",
      name = "Tx & DAO Yield Calculator",
      description = "Query CKB transaction details, view resolved inputs/outputs, check live DAO cell locking states, and run yield estimators.",
      url = null,
      tags = listOf("dev", "chain", "defi"),
      gradientColors = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
    ),

    ToolItem(
      id = "rpc_console",
      name = "CKB RPC Console",
      description = "Bitcoin Core console-inspired terminal. Query nodes with custom methods, view histories, and inspect pretty-printed JSON results.",
      url = null,
      tags = listOf("dev", "chain"),
      gradientColors = listOf(Color(0xFF1D976C), Color(0xFF93F9B9))
    ),
    ToolItem(
      id = "local_rfc",
      name = "Offline RFC Reader",
      description = "Browse, search, and read the official CKB technical specifications and RFCs offline from your local filesystem.",
      url = null,
      tags = listOf("chain", "learning"),
      gradientColors = listOf(Color(0xFFF27121), Color(0xFFE94057), Color(0xFF8A2387))
    ),
    ToolItem(
      id = "fiber_node",
      name = "Fiber Channel Monitor",
      description = "Monitor your local Fiber Network Node (fnn). Track active channels, connected peers, node status, and invoice payments.",
      url = null,
      tags = listOf("dev", "chain"),
      gradientColors = listOf(Color(0xFF00F2FE), Color(0xFF4FACFE))
    ),

    ToolItem(
      id = "cklibrary",
      name = "CKLibrary",
      description = "An interactive 3D, on-chain RFC viewer. Read CKB specifications in an engaging spatial environment.",
      url = "https://cklibrary.xyz/",
      tags = listOf("chain", "fun", "learning"),
      gradientColors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
    ),
    ToolItem(
      id = "wyltek",
      name = "Wyltek Industries",
      description = "A personal education and developer tooling platform for building on the Nervos ecosystem.",
      url = "https://wyltekindustries.com/",
      tags = listOf("fun", "learning"),
      gradientColors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
    ),
    ToolItem(
      id = "faucet",
      name = "Testnet Faucet",
      description = "The official Nervos testnet faucet. Claim test CKBytes to deploy and test your smart contracts.",
      url = "https://faucet.nervos.org/",
      tags = listOf("dev"),
      gradientColors = listOf(Color(0xFFED213A), Color(0xFF93291E))
    ),
    ToolItem(
      id = "explorer",
      name = "CKB Explorer",
      description = "The official Nervos Network explorer. Search and inspect blocks, transactions, cells, and lock scripts.",
      url = "https://explorer.nervos.org/",
      tags = listOf("dev", "chain"),
      gradientColors = listOf(Color(0xFF1F1C2C), Color(0xFF928DAB))
    ),
    ToolItem(
      id = "home_base",
      name = "Nervos Website",
      description = "The official website for the Nervos Network. Explore the foundations, tokenomics, and global ecosystem.",
      url = "https://www.nervos.org/",
      tags = listOf("learning", "chain"),
      gradientColors = listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B))
    ),
    ToolItem(
      id = "ckba",
      name = "CKB Association (CKBA)",
      description = "The official non-profit foundation supporting blockchain research, developer tooling, education, and global ecosystem growth for the Nervos Network.",
      url = "https://www.nervos.org/",
      tags = listOf("chain", "learning"),
      gradientColors = listOf(Color(0xFF1F1C2C), Color(0xFF928DAB))
    ),
    ToolItem(
      id = "talk_forum",
      name = "Talk Forum",
      description = "The official Nervos community talk forum. Engage in research discussions, governance, and updates.",
      url = "https://talk.nervos.org/",
      tags = listOf("chain", "learning"),
      gradientColors = listOf(Color(0xFF141E30), Color(0xFF243B55))
    )
  )

  val filteredTools = tools.filter { tool ->
    selectedTag == "All" || tool.tags.contains(selectedTag)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    TabHeader(title = "Network Tools")

    // Tag filter chips
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      tags.forEach { tag ->
        val isSelected = selectedTag == tag
        ElevatedFilterChip(
          selected = isSelected,
          onClick = { selectedTag = tag },
          label = { Text(tag) }
        )
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      items(filteredTools) { tool ->
        val skinType = remember(tool.id, refreshTrigger) { settingsStore.getCardSkinType(tool.id) }
        val skinPath = remember(tool.id, refreshTrigger) { settingsStore.getCardSkinPath(tool.id) }
        val scaleType = remember(tool.id, refreshTrigger) { settingsStore.getCardScaleType(tool.id) }

        val contentScale = if (scaleType == ScaleType.CROP) ContentScale.Crop else ContentScale.FillBounds
        val defaultUrl = remember(tool.id) { PanelManifest.getDefaultAssetUrl(tool.id) }

        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              when (tool.id) {
                "rpc_console" -> onNavigate(CkbConsole)
                "fiber_node" -> onNavigate(FiberHome)
                "tx_calculator" -> onNavigate(TxCalculator)
                "dao_dashboard" -> onNavigate(DaoDashboard)
                "local_rfc" -> onNavigate(RfcViewer)
                else -> {
                  val url = tool.url
                  if (url != null) {
                    onNavigate(WebBrowser(url = url, title = tool.name))
                  }
                }
              }
            },
          shape = RoundedCornerShape(12.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Brush.linearGradient(colors = tool.gradientColors))
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

            // Darkening overlay gradient for text contrast
            Box(
              modifier = Modifier
                .matchParentSize()
                .background(
                  Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.7f))
                  )
                )
            )

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
              ) {
                Text(
                  text = tool.name,
                  color = Color.White,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.weight(1f)
                )

                IconButton(
                  onClick = {
                    editingToolId = tool.id
                    editingToolName = tool.name
                    showPickerDialog = true
                  },
                  modifier = Modifier
                    .size(32.dp)
                    .offset(x = 8.dp, y = (-8).dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Customize Background",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = tool.description,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp
              )
              Spacer(modifier = Modifier.height(12.dp))
              
              // Tags under the description
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tool.tags.forEach { tag ->
                  Box(
                    modifier = Modifier
                      .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                      )
                      .padding(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    Text(
                      text = tag,
                      color = Color.White,
                      style = MaterialTheme.typography.labelSmall
                    )
                  }
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
      cardId = editingToolId,
      cardName = editingToolName,
      settingsStore = settingsStore,
      onDismissRequest = { showPickerDialog = false },
      onSkinApplied = { refreshTrigger++ }
    )
  }
}
