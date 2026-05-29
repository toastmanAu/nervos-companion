package com.example.nervoscompanion.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.navigation3.runtime.NavKey
import com.example.nervoscompanion.News
import com.example.nervoscompanion.R
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.nervoscompanion.data.RpcClient
import com.example.nervoscompanion.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun HomeScreen(onNavigate: (NavKey) -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val settingsStore = remember { SettingsStore(context) }
  val coroutineScope = rememberCoroutineScope()

  var rpcUrl by remember { mutableStateOf(settingsStore.rpcUrl) }
  var rpcNetwork by remember { mutableStateOf(settingsStore.rpcNetwork) }

  // Load RPC status & price details
  var blockNumber by remember { mutableStateOf<Long?>(null) }
  var epochNumber by remember { mutableStateOf<Long?>(null) }
  var epochProgress by remember { mutableStateOf<String?>(null) }
  var nodeVersion by remember { mutableStateOf<String?>(null) }

  var ckbPrice by remember { mutableStateOf<Double?>(null) }
  var ckbChange by remember { mutableStateOf<Double?>(null) }
  var ckbMarketCap by remember { mutableStateOf<Double?>(null) }
  var ckbVolume by remember { mutableStateOf<Double?>(null) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  fun loadData() {
    isLoading = true
    errorMsg = null
    coroutineScope.launch {
      try {
        // Fetch CKB RPC details
        val client = RpcClient(rpcUrl)
        val responseNum = client.call("get_tip_block_number")
        val blockHex = JSONObject(responseNum).getString("result")
        blockNumber = blockHex.substring(2).toLong(16)

        val responseHeader = client.call("get_tip_header")
        val headerResult = JSONObject(responseHeader).getJSONObject("result")
        val epochHex = headerResult.getString("epoch")
        val epochVal = epochHex.substring(2).toLong(16)
        val epNum = epochVal shr 32
        val epIdx = (epochVal shr 16) and 0xFFFF
        val epLen = epochVal and 0xFFFF
        epochNumber = epNum
        epochProgress = "$epIdx / $epLen"

        try {
          val responseNode = client.call("local_node_info")
          nodeVersion = JSONObject(responseNode).getJSONObject("result").getString("version")
        } catch (e: Exception) {
          nodeVersion = "Public Node"
        }

        // Fetch CKB CoinGecko Price details
        withContext(Dispatchers.IO) {
          val priceUrl = "https://api.coingecko.com/api/v3/simple/price?ids=nervos-network&vs_currencies=usd&include_24hr_change=true&include_market_cap=true&include_24hr_vol=true"
          val conn = URL(priceUrl).openConnection() as HttpURLConnection
          conn.connectTimeout = 5000
          conn.readTimeout = 5000
          if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val data = JSONObject(text).getJSONObject("nervos-network")
            ckbPrice = data.getDouble("usd")
            ckbChange = data.getDouble("usd_24h_change")
            ckbMarketCap = data.getDouble("usd_market_cap")
            ckbVolume = data.getDouble("usd_24h_vol")
          }
        }
      } catch (e: Exception) {
        errorMsg = "Failed to load data: ${e.localizedMessage}"
      } finally {
        isLoading = false
      }
    }
  }

  // Load configuration updates when settings might have changed
  LaunchedEffect(Unit) {
    rpcUrl = settingsStore.rpcUrl
    rpcNetwork = settingsStore.rpcNetwork
    loadData()
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {

    // 1. Hero Logo Card (Premium Graphic Visual Theme)
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp)
    ) {
      Box(
        modifier = Modifier.fillMaxWidth()
      ) {
        Image(
          painter = painterResource(id = R.drawable.panel_hero),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.matchParentSize()
        )
        Box(
          modifier = Modifier
            .matchParentSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.85f))
              )
            )
        )
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
              painter = painterResource(id = R.drawable.logo_white),
              contentDescription = "Nervos Logo",
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Nervos Network",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Companion App",
                color = Color.LightGray,
                fontSize = 14.sp
              )
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Connected RPC: $rpcUrl",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp
          )
          Text(
            text = "Network: ${rpcNetwork.uppercase()}",
            color = Color(0xFF00CC99),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    if (isLoading) {
      Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else if (errorMsg != null) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(text = errorMsg ?: "", color = MaterialTheme.colorScheme.onErrorContainer)
          Spacer(modifier = Modifier.height(8.dp))
          Button(onClick = { loadData() }) {
            Text("Retry")
          }
        }
      }
    }

    // 2. Chain Statistics Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Chain Statistics",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(text = "Tip Block", style = MaterialTheme.typography.bodySmall)
            Text(
              text = blockNumber?.toString() ?: "N/A",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.SemiBold
            )
          }
          Column {
            Text(text = "Epoch Number", style = MaterialTheme.typography.bodySmall)
            Text(
              text = epochNumber?.toString() ?: "N/A",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.SemiBold
            )
          }
          Column {
            Text(text = "Epoch Progress", style = MaterialTheme.typography.bodySmall)
            Text(
              text = epochProgress ?: "N/A",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }

    // 3. Price Summary Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Price Summary",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (ckbPrice != null) "$${String.format("%.6f", ckbPrice)}" else "N/A",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold
            )
            val change = ckbChange
            if (change != null) {
              val isPositive = change >= 0
              Text(
                text = "${if (isPositive) "+" else ""}${String.format("%.2f", change)}% (24h)",
                color = if (isPositive) Color(0xFF00CC99) else Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
              )
            }
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "Volume: " + (ckbVolume?.let { "$${String.format("%,.0f", it)}" } ?: "N/A"),
              style = MaterialTheme.typography.bodySmall
            )
            Text(
              text = "Mcap: " + (ckbMarketCap?.let { "$${String.format("%,.0f", it)}" } ?: "N/A"),
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }
    }

    // 4. Latest News Highlight Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Latest News",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Fiber Network: Scaling Nervos Network Layer 2 Channels",
          fontWeight = FontWeight.SemiBold,
          style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Read the latest update on Nervos Talk regarding Fiber Network development, channel mechanics, and next milestones.",
          style = MaterialTheme.typography.bodyMedium,
          color = Color.Gray
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
          onClick = { onNavigate(News) },
          modifier = Modifier.align(Alignment.End)
        ) {
          Text("Go to News")
        }
      }
    }

    Button(
      onClick = { loadData() },
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
      Text("Refresh Dashboard")
    }
  }
}
