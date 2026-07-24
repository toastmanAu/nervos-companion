package com.example.nervoscompanion.ui.settings

import android.widget.Toast
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import android.util.Base64
import java.nio.charset.StandardCharsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.TextButton
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
import com.example.nervoscompanion.data.work.WorkManagerHelper
import com.example.nervoscompanion.ui.components.TabHeader
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.System

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val settingsStore = remember { SettingsStore(context) }
  val appsRepository = remember {
    val db = com.example.nervoscompanion.data.cache.AppDatabase.getDatabase(context)
    AppsRepository(db.ecosystemAppDao(), settingsStore)
  }
  val coroutineScope = rememberCoroutineScope()

  fun parseFiberConnectUri(uri: String): Pair<String, String>? {
    if (!uri.startsWith("fiberconnect://")) return null
    val base64Payload = uri.substring("fiberconnect://".length)
    return try {
      val decodedBytes = Base64.decode(base64Payload, Base64.URL_SAFE or Base64.NO_PADDING)
      val decodedString = String(decodedBytes, StandardCharsets.UTF_8)
      val json = JSONObject(decodedString)
      val rpcUrl = json.getString("rpc_url")
      val authToken = json.getString("auth_token")
      Pair(rpcUrl, authToken)
    } catch (e: Exception) {
      null
    }
  }

  fun parsePairingPayload(scanned: String): Pair<String, String>? {
    if (scanned.startsWith("fiberconnect://")) {
      return parseFiberConnectUri(scanned)
    }
    return try {
      val json = JSONObject(scanned)
      val rpcUrl = json.getString("rpc_url")
      val authToken = json.getString("auth_token")
      Pair(rpcUrl, authToken)
    } catch (e: Exception) {
      null
    }
  }

  var supportEmail by remember { mutableStateOf("phill@wyltek.com") }

  var currentVersionCode by remember { mutableStateOf(1) }
  var currentVersionName by remember { mutableStateOf("1.0") }

  LaunchedEffect(Unit) {
    try {
      val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
      currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo.longVersionCode.toInt()
      } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode
      }
      currentVersionName = packageInfo.versionName ?: "1.0"
    } catch (e: Exception) {
      // Keep defaults
    }
  }

  var isCheckingUpdates by remember { mutableStateOf(false) }
  var updateInfo by remember { mutableStateOf<com.example.nervoscompanion.data.AppUpdate?>(null) }
  var showUpdateDialog by remember { mutableStateOf(false) }

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
  var isBackgroundSync by remember { mutableStateOf(settingsStore.isBackgroundSyncEnabled) }
  var blockThreshold by remember { mutableStateOf(settingsStore.notificationBlockThreshold) }
  var fiberRpcUrl by remember { mutableStateOf(settingsStore.fiberRpcUrl) }
  var fiberAuthToken by remember { mutableStateOf(settingsStore.fiberAuthToken) }
  var isForumNotifications by remember { mutableStateOf(settingsStore.isForumNotificationsEnabled) }
  var isReleaseNotifications by remember { mutableStateOf(settingsStore.isReleaseNotificationsEnabled) }
  var localRfcPath by remember { mutableStateOf(settingsStore.localRfcPath) }

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
    TabHeader(title = "RPC Settings")

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

        OutlinedTextField(
          value = fiberRpcUrl,
          onValueChange = { fiberRpcUrl = it },
          label = { Text("Fiber RPC URL") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = fiberAuthToken,
          onValueChange = { fiberAuthToken = it },
          label = { Text("Fiber Auth Token (Biscuit)") },
          modifier = Modifier.fillMaxWidth(),
          placeholder = { Text("Omit for local nodes") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = {
            try {
              val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
              val scanner = GmsBarcodeScanning.getClient(context, options)
              scanner.startScan()
                .addOnSuccessListener { barcode ->
                  val rawValue = barcode.rawValue
                  if (rawValue != null) {
                    val parsed = parsePairingPayload(rawValue)
                    if (parsed != null) {
                      fiberRpcUrl = parsed.first
                      fiberAuthToken = parsed.second
                      Toast.makeText(context, "Pairing details imported! Save to apply.", Toast.LENGTH_SHORT).show()
                    } else {
                      Toast.makeText(context, "Invalid pairing code", Toast.LENGTH_LONG).show()
                    }
                  }
                }
                .addOnFailureListener { e ->
                  Toast.makeText(context, "Scanning failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
              Toast.makeText(context, "Scanner unavailable: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = Color.White)
        ) {
          Text("Scan Pairing QR Code")
        }

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

    Card(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Local RFCs Repository Path",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = localRfcPath,
          onValueChange = { localRfcPath = it },
          label = { Text("RFCs Local Path") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Local folder path where the cloned or downloaded Nervos RFCs ('rfcs-master') are stored on your device.",
          style = MaterialTheme.typography.bodySmall,
          color = Color.Gray
        )
      }
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "Background Monitoring",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Background Chain Status",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Periodically check blockchain epochs and send local notification alerts",
              style = MaterialTheme.typography.bodySmall,
              color = Color.Gray
            )
          }
          Switch(
            checked = isBackgroundSync,
            onCheckedChange = { checked ->
              isBackgroundSync = checked
              settingsStore.isBackgroundSyncEnabled = checked
              if (checked) {
                WorkManagerHelper.schedule(context)
              } else {
                WorkManagerHelper.cancel(context)
              }
            }
          )
        }

        if (isBackgroundSync) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Notification Block Milestone",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Receive alerts when mining crosses every 10k, 100k, or 1M blocks",
              style = MaterialTheme.typography.bodySmall,
              color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              listOf(10000L, 100000L, 1000000L).forEach { threshold ->
                val label = when (threshold) {
                  10000L -> "10k"
                  100000L -> "100k"
                  1000000L -> "1M"
                  else -> "$threshold"
                }
                val isSelected = blockThreshold == threshold
                OutlinedButton(
                  onClick = {
                    blockThreshold = threshold
                    settingsStore.notificationBlockThreshold = threshold
                  },
                  colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                  ),
                  modifier = Modifier.weight(1f)
                ) {
                  Text(label)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Forum Topic Alerts",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Receive alerts for new posts on talk.nervos.org",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
              )
            }
            Switch(
              checked = isForumNotifications,
              onCheckedChange = { checked ->
                isForumNotifications = checked
                settingsStore.isForumNotificationsEnabled = checked
              }
            )
          }

          Spacer(modifier = Modifier.height(12.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Software Release Alerts",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Receive alerts for new CKB and Fiber releases on GitHub",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
              )
            }
            Switch(
              checked = isReleaseNotifications,
              onCheckedChange = { checked ->
                isReleaseNotifications = checked
                settingsStore.isReleaseNotificationsEnabled = checked
              }
            )
          }
        }
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

    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Card Customization Styles",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Clear all custom backgrounds, stretched layouts, and website presets applied to tool and application cards, reverting them to their original styles.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
          onClick = {
            settingsStore.clearAllReskins()
            Toast.makeText(context, "Reskins cleared!", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Reset Card Reskins", color = Color.White)
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
          settingsStore.fiberRpcUrl = fiberRpcUrl
          settingsStore.fiberAuthToken = fiberAuthToken
          settingsStore.isForumNotificationsEnabled = isForumNotifications
          settingsStore.isReleaseNotificationsEnabled = isReleaseNotifications
          settingsStore.localRfcPath = localRfcPath
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
          text = "About & Updates",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "CKB Directory Companion App\nApp Version: $currentVersionName (Build $currentVersionCode)\nDeveloper: toastmanAu",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
          onClick = {
            isCheckingUpdates = true
            coroutineScope.launch {
              val update = appsRepository.checkForUpdates()
              isCheckingUpdates = false
              if (update != null) {
                if (update.versionCode > currentVersionCode) {
                  updateInfo = update
                  showUpdateDialog = true
                } else {
                  Toast.makeText(context, "Your app is up to date (v$currentVersionName)", Toast.LENGTH_SHORT).show()
                }
              } else {
                Toast.makeText(context, "Failed to check for updates. Verify your connection/config URL.", Toast.LENGTH_LONG).show()
              }
            }
          },
          enabled = !isCheckingUpdates,
          modifier = Modifier.fillMaxWidth()
        ) {
          if (isCheckingUpdates) {
            androidx.compose.material3.CircularProgressIndicator(
              color = Color.White,
              strokeWidth = 2.dp,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Checking...")
          } else {
            Text("Check for Updates")
          }
        }
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
              App Version: $currentVersionName (Build $currentVersionCode)
              
              Description of Bug / Feedback:
              
              
              Steps to Reproduce (if bug):
              1.
              2.
            """.trimIndent()
            
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
              data = android.net.Uri.parse("mailto:")
              putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(supportEmail))
              putExtra(android.content.Intent.EXTRA_SUBJECT, "[CKB Directory] Feedback & Bug Report")
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

  if (showUpdateDialog && updateInfo != null) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { showUpdateDialog = false },
      title = { Text("Update Available: v${updateInfo!!.versionName}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("A new version of CKB Directory is available. Would you like to update?")
          if (updateInfo!!.changelog.isNotEmpty()) {
            Text("Changelog:", fontWeight = FontWeight.Bold)
            Text(updateInfo!!.changelog, style = MaterialTheme.typography.bodyMedium)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showUpdateDialog = false
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(updateInfo!!.downloadUrl))
            context.startActivity(intent)
          }
        ) {
          Text("Download")
        }
      },
      dismissButton = {
        TextButton(onClick = { showUpdateDialog = false }) {
          Text("Later")
        }
      }
    )
  }
}
