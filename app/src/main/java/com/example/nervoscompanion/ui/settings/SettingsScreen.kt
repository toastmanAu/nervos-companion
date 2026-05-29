package com.example.nervoscompanion.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nervoscompanion.data.RpcClient
import com.example.nervoscompanion.data.SettingsStore
import com.example.nervoscompanion.data.AppsRepository
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.System

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val settingsStore = remember { SettingsStore(context) }
  val appsRepository = remember { AppsRepository(settingsStore) }
  val coroutineScope = rememberCoroutineScope()

  var supportEmail by remember { mutableStateOf("developer@example.com") }

  LaunchedEffect(Unit) {
    coroutineScope.launch {
      try {
        supportEmail = appsRepository.fetchSupportEmail()
      } catch (e: Exception) {
        // Fallback already handled inside AppsRepository
      }
    }
  }

  var rpcUrl by remember { mutableStateOf(settingsStore.rpcUrl) }
  var rpcNetwork by remember { mutableStateOf(settingsStore.rpcNetwork) }
  var selectedTheme by remember { mutableStateOf(settingsStore.themeName) }
  var configBaseUrl by remember { mutableStateOf(settingsStore.configBaseUrl) }

  var testStatus by remember { mutableStateOf("") }
  var isTesting by remember { mutableStateOf(false) }

  val presets = listOf(
    Pair("Nervos Public Mainnet", "https://mainnet.ckb.dev/"),
    Pair("Nervos Public Testnet", "https://testnet.ckb.dev/"),
    Pair("Ankr Mainnet", "https://rpc.ankr.com/nervos_ckb")
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "RPC Settings",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )

    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Preset Node Providers",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        presets.forEach { (name, url) ->
          OutlinedButton(
            onClick = {
              rpcUrl = url
              rpcNetwork = if (url.contains("testnet")) "testnet" else "mainnet"
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (rpcUrl == url) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            )
          ) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Custom RPC Configuration",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = rpcUrl,
          onValueChange = { rpcUrl = it },
          label = { Text("RPC URL") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "Network Mode", style = MaterialTheme.typography.bodyMedium)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Start,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
              selected = rpcNetwork == "mainnet",
              onClick = { rpcNetwork = "mainnet" }
            )
            Text(text = "Mainnet")
          }
          Spacer(modifier = Modifier.width(16.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
              selected = rpcNetwork == "testnet",
              onClick = { rpcNetwork = "testnet" }
            )
            Text(text = "Testnet")
          }
        }
      }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Remote Configuration Repository",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = configBaseUrl,
          onValueChange = { configBaseUrl = it },
          label = { Text("Config Base URL") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Points to directory hosting apps.json, featured_links.json, etc. Default is the official Nervos community repo.",
          style = MaterialTheme.typography.bodySmall,
          color = Color.Gray
        )
      }
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "App Theme Palette",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        val themes = listOf(
          Pair("Emerald Forest", "emerald"),
          Pair("Cyberpunk Neon", "cyberpunk"),
          Pair("Midnight Ocean", "ocean"),
          Pair("Obsidian Stealth", "stealth")
        )

        themes.forEach { (name, id) ->
          OutlinedButton(
            onClick = {
              selectedTheme = id
              com.example.nervoscompanion.theme.currentThemeName = id
              settingsStore.themeName = id
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (selectedTheme == id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            )
          ) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Button(
        onClick = {
          isTesting = true
          testStatus = "Testing connection..."
          coroutineScope.launch {
            try {
              val startTime = System.currentTimeMillis()
              val client = RpcClient(rpcUrl)
              val response = client.call("get_tip_block_number")
              val latency = System.currentTimeMillis() - startTime
              val json = JSONObject(response)
              if (json.has("result")) {
                val blockHex = json.getString("result")
                val blockNum = blockHex.substring(2).toLong(16)
                testStatus = "Connected! Tip Block: #$blockNum (${latency}ms)"
              } else {
                testStatus = "Connected, but returned unexpected response."
              }
            } catch (e: Exception) {
              testStatus = "Connection failed: ${e.localizedMessage}"
            } finally {
              isTesting = false
            }
          }
        },
        enabled = !isTesting && rpcUrl.isNotEmpty(),
        modifier = Modifier.weight(1f)
      ) {
        Text("Test Link")
      }

      Button(
        onClick = {
          settingsStore.rpcUrl = rpcUrl
          settingsStore.rpcNetwork = rpcNetwork
          settingsStore.configBaseUrl = configBaseUrl
          Toast.makeText(context, "Settings saved!", Toast.LENGTH_SHORT).show()
        },
        enabled = rpcUrl.isNotEmpty(),
        modifier = Modifier.weight(1f)
      ) {
        Text("Save")
      }
    }

    if (testStatus.isNotEmpty()) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = if (testStatus.startsWith("Connected"))
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
          else
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
      ) {
        Text(
          text = testStatus,
          modifier = Modifier.padding(12.dp),
          style = MaterialTheme.typography.bodyMedium,
          color = if (testStatus.startsWith("Connected"))
            MaterialTheme.colorScheme.onPrimaryContainer
          else
            MaterialTheme.colorScheme.onErrorContainer
        )
      }
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Feedback & Bug Report",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Help us improve the companion app. Report bugs, suggest features, or submit feedback directly to the team.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
          onClick = {
            val body = """
              Device Information:
              Android Version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})
              Device Model: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
              App Version: 1.0
              
              Description of Bug / Feedback:
              
              
              Steps to Reproduce (if bug):
              1.
              2.
            """.trimIndent()
            
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
              data = android.net.Uri.parse("mailto:")
              putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(supportEmail))
              putExtra(android.content.Intent.EXTRA_SUBJECT, "[Nervos Companion] Feedback & Bug Report")
              putExtra(android.content.Intent.EXTRA_TEXT, body)
            }
            try {
              context.startActivity(android.content.Intent.createChooser(intent, "Send Feedback Using..."))
            } catch (e: Exception) {
              Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Send Feedback / Bug Report")
        }
      }
    }
  }
}
