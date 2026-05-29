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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.nervoscompanion.CkbConsole
import com.example.nervoscompanion.WebBrowser

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

  val tags = listOf("All", "dev", "defi", "chain", "nft/dob", "fun", "learning")

  val tools = listOf(
    ToolItem(
      id = "dao_viewer",
      name = "Nervos DAO Viewer",
      description = "Embed and browse daoview.org directly in the app. Monitor active DAO deposits, withdrawal epochs, and system statistics.",
      url = "https://daoview.org",
      tags = listOf("defi", "chain"),
      gradientColors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
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
      id = "cklibrary",
      name = "CKLibrary",
      description = "An interactive 3D, on-chain RFC viewer. Read CKB specifications in an engaging spatial environment.",
      url = "https://cklibrary.xyz/",
      tags = listOf("chain", "fun", "learning"),
      gradientColors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
    ),
    ToolItem(
      id = "cellswap",
      name = "CellSwap",
      description = "A working proof-of-concept DOB/CKBFS cell storage demo site. Store and manage cell resources.",
      url = "https://cellswap.xyz/",
      tags = listOf("chain", "fun", "nft/dob"),
      gradientColors = listOf(Color(0xFF11998E), Color(0xFF38EF7D))
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
      id = "byterent",
      name = "ByteRent",
      description = "An early proof-of-concept demo of on-chain space/storage rental services for smart contracts.",
      url = "https://byterent.xyz/",
      tags = listOf("chain", "dev"),
      gradientColors = listOf(Color(0xFF7F00FF), Color(0xFFE100FF))
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
    Text(
      text = "Network Tools",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )

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
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              val url = tool.url
              if (url != null) {
                onNavigate(WebBrowser(url = url, title = tool.name))
              } else {
                onNavigate(CkbConsole)
              }
            },
          shape = RoundedCornerShape(12.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Brush.linearGradient(colors = tool.gradientColors))
              .padding(24.dp)
          ) {
            Column {
              Text(
                text = tool.name,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
              )
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
}
