package com.example.nervoscompanion.ui.tools

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.nervoscompanion.Settings
import com.example.nervoscompanion.data.CkbAddressParser
import com.example.nervoscompanion.data.RpcClient
import com.example.nervoscompanion.data.SettingsStore
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode

data class FiberChannel(
  val id: String,
  val peerId: String,
  val localBalance: Double,
  val remoteBalance: Double,
  val capacity: Double,
  val state: String,
  val enabled: Boolean,
  val assetName: String
)

data class FiberPeer(
  val peerId: String,
  val addresses: List<String>
)

data class FiberPayment(
  val paymentHash: String,
  val status: String,
  val amount: Double,
  val fee: Double,
  val createdAt: Long,
  val destination: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiberHomeScreen(onNavigate: (NavKey) -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val settingsStore = remember { SettingsStore(context) }
  val coroutineScope = rememberCoroutineScope()

  fun parseHexAmount(hex: String?): Double {
    if (hex == null) return 0.0
    val cleanHex = if (hex.startsWith("0x")) hex.substring(2) else hex
    if (cleanHex.isEmpty()) return 0.0
    return try {
      val shannons = BigInteger(cleanHex, 16)
      shannons.toBigDecimal().movePointLeft(8).toDouble()
    } catch (e: Exception) {
      0.0
    }
  }

  val rpcUrl = settingsStore.fiberRpcUrl
  val rpcToken = settingsStore.fiberAuthToken

  // States
  var isAlive by remember { mutableStateOf(false) }
  var nodeId by remember { mutableStateOf("N/A") }
  var version by remember { mutableStateOf("N/A") }
  var chainHash by remember { mutableStateOf("N/A") }
  var channelsList by remember { mutableStateOf<List<FiberChannel>>(emptyList()) }
  var peersList by remember { mutableStateOf<List<FiberPeer>>(emptyList()) }
  var isRefreshing by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }
  var fundingAddress by remember { mutableStateOf<String?>(null) }
  var showFundingQr by remember { mutableStateOf(false) }

  // Action Panel states
  var openChannelExpanded by remember { mutableStateOf(false) }
  var connectPeerExpanded by remember { mutableStateOf(false) }
  var createInvoiceExpanded by remember { mutableStateOf(false) }

  // Form input fields
  var inputPeerId by remember { mutableStateOf("") }
  var inputAmount by remember { mutableStateOf("") }

  var inputConnectPeerId by remember { mutableStateOf("") }
  var inputConnectAddress by remember { mutableStateOf("") }

  var inputInvoiceAmount by remember { mutableStateOf("") }
  var inputInvoiceDesc by remember { mutableStateOf("") }
  var generatedInvoice by remember { mutableStateOf<String?>(null) }

  var isActionLoading by remember { mutableStateOf(false) }

  var scannedInvoice by remember { mutableStateOf<String?>(null) }
  var showPayInvoiceDialog by remember { mutableStateOf(false) }
  var parsingInvoice by remember { mutableStateOf(false) }
  var parsedInvoiceAmount by remember { mutableStateOf<Double?>(null) }
  var parsedInvoiceDesc by remember { mutableStateOf("") }
  var parsedInvoiceDestination by remember { mutableStateOf("") }
  var paymentsList by remember { mutableStateOf<List<FiberPayment>>(emptyList()) }
  var isLoadingPayments by remember { mutableStateOf(false) }
  var paymentsErrorMsg by remember { mutableStateOf<String?>(null) }
  var generatedInvoicesList by remember { mutableStateOf(settingsStore.getGeneratedInvoices()) }

  fun loadPaymentHistory() {
    isLoadingPayments = true
    paymentsErrorMsg = null
    coroutineScope.launch {
      try {
        val client = RpcClient(rpcUrl, rpcToken)
        val response = client.call("list_payments", listOf(emptyMap<String, Any>()))
        val responseObj = JSONObject(response)
        if (responseObj.has("error")) {
          val errorObj = responseObj.optJSONObject("error")
          val msg = errorObj?.optString("message") ?: "Unknown RPC error"
          throw Exception(msg)
        }
        val resultObj = responseObj.optJSONObject("result")
        val paymentsArr = resultObj?.optJSONArray("payments")
        val tempPayments = mutableListOf<FiberPayment>()
        if (paymentsArr != null) {
          for (i in 0 until paymentsArr.length()) {
            val item = paymentsArr.getJSONObject(i)
            val amountHex = item.optString("amount", "0x0")
            val feeHex = item.optString("fee", "0x0")
            val statusObj = item.optJSONObject("status")
            val statusStr = if (statusObj != null) {
              statusObj.optString("status", statusObj.optString("state_name", "UNKNOWN"))
            } else {
              item.optString("status", "UNKNOWN")
            }
            tempPayments.add(
              FiberPayment(
                paymentHash = item.optString("payment_hash", ""),
                status = statusStr,
                amount = parseHexAmount(amountHex),
                fee = parseHexAmount(feeHex),
                createdAt = item.optLong("created_at", 0L),
                destination = item.optString("payee_public_key", item.optString("peer_id", "N/A"))
              )
            )
          }
        }
        paymentsList = tempPayments.sortedByDescending { it.createdAt }
      } catch (e: Exception) {
        paymentsList = emptyList()
        paymentsErrorMsg = e.localizedMessage
      } finally {
        isLoadingPayments = false
      }
    }
  }

  LaunchedEffect(showPayInvoiceDialog, scannedInvoice) {
    if (showPayInvoiceDialog && scannedInvoice != null) {
      parsingInvoice = true
      parsedInvoiceAmount = null
      parsedInvoiceDesc = ""
      parsedInvoiceDestination = ""
      try {
        val client = RpcClient(rpcUrl, rpcToken)
        val response = client.call("parse_invoice", listOf(mapOf("invoice" to scannedInvoice)))
        val result = JSONObject(response).optJSONObject("result")
        if (result != null) {
          val invoiceObj = result.optJSONObject("invoice")
          if (invoiceObj != null) {
            val amountHex = invoiceObj.optString("amount", "0x0")
            parsedInvoiceAmount = parseHexAmount(amountHex)
            val dataObj = invoiceObj.optJSONObject("data")
            if (dataObj != null) {
              val attrsArr = dataObj.optJSONArray("attrs")
              if (attrsArr != null) {
                for (i in 0 until attrsArr.length()) {
                  val attr = attrsArr.optJSONObject(i) ?: continue
                  if (attr.has("description")) {
                    parsedInvoiceDesc = attr.optString("description")
                  }
                  if (attr.has("payee_public_key")) {
                    parsedInvoiceDestination = attr.optString("payee_public_key")
                  }
                }
              }
            }
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        parsingInvoice = false
      }
    }
  }


  fun loadNodeData() {
    isRefreshing = true
    errorMsg = null
    loadPaymentHistory()
    coroutineScope.launch {
      try {
        val client = RpcClient(rpcUrl, rpcToken)
        
        // 1. Fetch node_info
        val infoResponse = client.call("node_info")
        val infoResponseObj = JSONObject(infoResponse)
        if (infoResponseObj.has("error")) {
          val errorObj = infoResponseObj.optJSONObject("error")
          val msg = errorObj?.optString("message") ?: "Unknown RPC error"
          throw Exception(msg)
        }
        val infoObj = infoResponseObj.getJSONObject("result")
        nodeId = infoObj.optString("pubkey", infoObj.optString("node_id", "N/A"))
        version = infoObj.optString("version", "N/A")
        chainHash = infoObj.optString("chain_hash", "N/A")
        isAlive = true

        val fundingScriptObj = infoObj.optJSONObject("default_funding_lock_script")
        fundingAddress = if (fundingScriptObj != null) {
          try {
            val codeHash = fundingScriptObj.getString("code_hash")
            val hashType = fundingScriptObj.getString("hash_type")
            val args = fundingScriptObj.getString("args")
            val hrp = if (chainHash == "0x10639e0895502b5688a6be8cf69460d76541bfa4821629d86d62ba0aae3f9606") "ckt" else "ckb"
            CkbAddressParser.encodeAddress(codeHash, hashType, args, hrp)
          } catch (e: Exception) {
            e.printStackTrace()
            null
          }
        } else {
          null
        }

        // Parse whitelist UDTs
        val udtMap = mutableMapOf<String, String>()
        val udtArr = infoObj.optJSONArray("udt_cfg_infos")
        if (udtArr != null) {
          for (i in 0 until udtArr.length()) {
            val udtItem = udtArr.getJSONObject(i)
            val name = udtItem.optString("name", "UDT")
            val scriptObj = udtItem.optJSONObject("script")
            val args = scriptObj?.optString("args")
            if (!args.isNullOrEmpty()) {
              udtMap[args.lowercase()] = name
            }
          }
        }

        // 2. Fetch list_channels
        try {
          val channelsResponse = client.call("list_channels", listOf(emptyMap<String, Any>()))
          val channelsResponseObj = JSONObject(channelsResponse)
          if (channelsResponseObj.has("error")) {
            val errorObj = channelsResponseObj.optJSONObject("error")
            val msg = errorObj?.optString("message") ?: "Unknown RPC error"
            throw Exception(msg)
          }
          val channelsArr = channelsResponseObj.getJSONObject("result").getJSONArray("channels")
          val tempChannels = mutableListOf<FiberChannel>()
          for (i in 0 until channelsArr.length()) {
            val item = channelsArr.getJSONObject(i)
            val stateObj = item.optJSONObject("state")
            val stateName = stateObj?.optString("state_name", "UNKNOWN") ?: "UNKNOWN"
            val localBal = parseHexAmount(item.optString("local_balance", "0x0"))
            val remoteBal = parseHexAmount(item.optString("remote_balance", "0x0"))
            val cap = localBal + remoteBal
            
            val udtScript = item.optJSONObject("funding_udt_type_script")
            val assetName = if (udtScript == null) {
              "CKB"
            } else {
              val args = udtScript.optString("args").lowercase()
              udtMap[args] ?: "UDT"
            }

            tempChannels.add(
              FiberChannel(
                id = item.optString("channel_id", ""),
                peerId = item.optString("pubkey", item.optString("peer_id", "")),
                localBalance = localBal,
                remoteBalance = remoteBal,
                capacity = cap,
                state = stateName,
                enabled = item.optBoolean("enabled", true),
                assetName = assetName
              )
            )
          }
          channelsList = tempChannels
        } catch (e: Exception) {
          channelsList = emptyList()
        }

        // 3. Fetch list_peers
        try {
          val peersResponse = client.call("list_peers", listOf(emptyMap<String, Any>()))
          val peersResponseObj = JSONObject(peersResponse)
          if (peersResponseObj.has("error")) {
            val errorObj = peersResponseObj.optJSONObject("error")
            val msg = errorObj?.optString("message") ?: "Unknown RPC error"
            throw Exception(msg)
          }
          val peersArr = peersResponseObj.getJSONObject("result").getJSONArray("peers")
          val tempPeers = mutableListOf<FiberPeer>()
          for (i in 0 until peersArr.length()) {
            val item = peersArr.getJSONObject(i)
            val addrs = mutableListOf<String>()
            val addrArr = item.optJSONArray("addresses")
            if (addrArr != null) {
              for (j in 0 until addrArr.length()) {
                addrs.add(addrArr.getString(j))
              }
            } else {
              val singleAddr = item.optString("address", item.optString("addresses", ""))
              if (singleAddr.isNotEmpty()) {
                addrs.add(singleAddr)
              }
            }
            tempPeers.add(
              FiberPeer(
                peerId = item.optString("pubkey", item.optString("peer_id", "")),
                addresses = addrs
              )
            )
          }
          peersList = tempPeers
        } catch (e: Exception) {
          peersList = emptyList()
        }

      } catch (e: Exception) {
        isAlive = false
        errorMsg = "Connection failed: ${e.localizedMessage}"
        fundingAddress = null
      } finally {
        isRefreshing = false
      }
    }
  }

  LaunchedEffect(rpcUrl, rpcToken) {
    loadNodeData()
    loadPaymentHistory()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Fiber Channel Monitor", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = { onNavigate(com.example.nervoscompanion.Tools) }) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          TextButton(onClick = { loadNodeData() }, enabled = !isRefreshing) {
            Text(if (isRefreshing) "Loading..." else "Refresh")
          }
        }
      )
    }
  ) { paddingValues ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

      // 1. Status Indicator Banner
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = if (isAlive) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
          else 
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(16.dp)
              .clip(CircleShape)
              .background(if (isAlive) Color(0xFF00CC99) else Color.Red)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (isAlive) "Fiber Node Online" else "Fiber Node Offline",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.bodyLarge
            )
            Text(
              text = "Endpoint: $rpcUrl",
              style = MaterialTheme.typography.bodySmall,
              color = Color.Gray
            )
          }
          if (!isAlive) {
            Button(
              onClick = { onNavigate(Settings) },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
              Text("Config")
            }
          }
        }
      }

      if (isAlive) {
        // 2. Node Information Card
        Card(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Node Details",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
              Text("Version", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
              Text(version, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
              Text("Node Peer ID", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = nodeId,
                  style = MaterialTheme.typography.bodySmall,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Copy",
                  color = MaterialTheme.colorScheme.primary,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.clickable {
                    clipboardManager.setText(AnnotatedString(nodeId))
                    Toast.makeText(context, "Node ID copied!", Toast.LENGTH_SHORT).show()
                  }
                )
              }
            }
          }
        }

        // 2b. Node Funding Address Card
        fundingAddress?.let { address ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
            )
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Node Funding Address",
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.secondary
                )
                Row(
                  horizontalArrangement = Arrangement.spacedBy(16.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Copy",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                      clipboardManager.setText(AnnotatedString(address))
                      Toast.makeText(context, "Funding Address copied!", Toast.LENGTH_SHORT).show()
                    }
                  )
                  Text(
                    text = if (showFundingQr) "Hide QR" else "Show QR",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                      showFundingQr = !showFundingQr
                    }
                  )
                }
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = address,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
              )
              
              AnimatedVisibility(visible = showFundingQr) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.padding(top = 12.dp)
                ) {
                  val qrBitmap = remember(address) {
                    try {
                      com.example.nervoscompanion.data.QrCodeGenerator.generateQrCode(address, 350)
                    } catch (e: Exception) {
                      null
                    }
                  }
                  if (qrBitmap != null) {
                    Image(
                      bitmap = qrBitmap.asImageBitmap(),
                      contentDescription = "Funding Address QR Code",
                      modifier = Modifier
                        .size(180.dp)
                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .padding(6.dp)
                    )
                  }
                }
              }
            }
          }
        }

        // 3. Liquidity Capacity Visual Progress Bar Grouped by Asset
        val channelsByAsset = remember(channelsList) { channelsList.groupBy { it.assetName } }

        if (channelsList.isEmpty()) {
          Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "CKB Liquidity Allocation",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Local: 0.00 CKB", style = MaterialTheme.typography.bodyMedium)
                Text("Remote: 0.00 CKB", style = MaterialTheme.typography.bodyMedium)
              }
              Spacer(modifier = Modifier.height(8.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(14.dp)
                  .clip(RoundedCornerShape(7.dp))
                  .background(Color.Gray.copy(alpha = 0.2f))
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Total Capacity: 0.00 CKB",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
              )
            }
          }
        } else {
          channelsByAsset.forEach { (assetName, assetChannels) ->
            val totalLocal = assetChannels.sumOf { it.localBalance }
            val totalRemote = assetChannels.sumOf { it.remoteBalance }
            val totalCapacity = totalLocal + totalRemote

            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = if (assetName == "CKB") "CKB Liquidity Allocation" else "$assetName Liquidity Allocation",
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                  Text("Local: ${String.format("%.2f", totalLocal)} $assetName", style = MaterialTheme.typography.bodyMedium)
                  Text("Remote: ${String.format("%.2f", totalRemote)} $assetName", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Liquidity visual split bar
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.Gray.copy(alpha = 0.2f))
                ) {
                  if (totalCapacity > 0) {
                    val localFraction = (totalLocal / totalCapacity).toFloat()
                    Row(modifier = Modifier.fillMaxSize()) {
                      Box(
                        modifier = Modifier
                          .fillMaxHeight()
                          .weight(localFraction.coerceAtLeast(0.001f))
                          .background(
                            Brush.horizontalGradient(
                              colors = if (assetName == "CKB") 
                                listOf(Color(0xFF00FFCC), Color(0xFF0099FF))
                              else 
                                listOf(Color(0xFFFFCC00), Color(0xFFFF9900))
                            )
                          )
                      )
                      Box(
                        modifier = Modifier
                          .fillMaxHeight()
                          .weight((1f - localFraction).coerceAtLeast(0.001f))
                          .background(
                            Brush.horizontalGradient(
                              colors = if (assetName == "CKB")
                                listOf(Color(0xFFFF007F), Color(0xFF8A2387))
                              else
                                listOf(Color(0xFFCC00FF), Color(0xFF6600CC))
                            )
                          )
                      )
                    }
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Total Capacity: ${String.format("%.2f", totalCapacity)} $assetName",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.Gray,
                  modifier = Modifier.align(Alignment.CenterHorizontally)
                )
              }
            }
          }
        }

        // 4. Interactive Action Panels (Collapsible Expandable)
        // Action: Open Channel
        Card(modifier = Modifier.fillMaxWidth()) {
          Column {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { openChannelExpanded = !openChannelExpanded }
                .padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Open Payment Channel", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
              Text(if (openChannelExpanded) "Collapse" else "Expand", color = MaterialTheme.colorScheme.primary)
            }
            AnimatedVisibility(visible = openChannelExpanded) {
              Column(
                modifier = Modifier
                  .padding(horizontal = 16.dp)
                  .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = inputPeerId,
                  onValueChange = { inputPeerId = it },
                  label = { Text("Remote Peer Node ID (Pubkey)") },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true
                )
                OutlinedTextField(
                  value = inputAmount,
                  onValueChange = { inputAmount = it },
                  label = { Text("Funding Amount (CKB)") },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true
                )
                Button(
                  onClick = {
                    val amountVal = inputAmount.toDoubleOrNull()
                    if (inputPeerId.isEmpty() || amountVal == null || amountVal <= 0.0) {
                      Toast.makeText(context, "Invalid Peer ID or Amount", Toast.LENGTH_SHORT).show()
                      return@Button
                    }
                    isActionLoading = true
                    coroutineScope.launch {
                      try {
                        val client = RpcClient(rpcUrl, rpcToken)
                        val shannonsVal = BigInteger(Math.round(amountVal * 1e8).toString())
                        val amountHex = "0x" + shannonsVal.toString(16)
                        val params = mapOf(
                          "pubkey" to inputPeerId,
                          "funding_amount" to amountHex,
                          "public" to true
                        )
                        client.call("open_channel", listOf(params))
                        Toast.makeText(context, "Channel open request sent successfully!", Toast.LENGTH_LONG).show()
                        inputPeerId = ""
                        inputAmount = ""
                        openChannelExpanded = false
                        loadNodeData()
                      } catch (e: Exception) {
                        Toast.makeText(context, "Failed to open channel: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                      } finally {
                        isActionLoading = false
                      }
                    }
                  },
                  enabled = !isActionLoading,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(if (isActionLoading) "Processing..." else "Submit Open Request")
                }
              }
            }
          }
        }

        // Action: Connect Peer
        Card(modifier = Modifier.fillMaxWidth()) {
          Column {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { connectPeerExpanded = !connectPeerExpanded }
                .padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Connect Peer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
              Text(if (connectPeerExpanded) "Collapse" else "Expand", color = MaterialTheme.colorScheme.primary)
            }
            AnimatedVisibility(visible = connectPeerExpanded) {
              Column(
                modifier = Modifier
                  .padding(horizontal = 16.dp)
                  .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = inputConnectPeerId,
                  onValueChange = { inputConnectPeerId = it },
                  label = { Text("Peer Node ID") },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true
                )
                OutlinedTextField(
                  value = inputConnectAddress,
                  onValueChange = { inputConnectAddress = it },
                  label = { Text("Multiaddress (e.g. /ip4/127.0.0.1/tcp/8228)") },
                  placeholder = { Text("/ip4/127.0.0.1/tcp/8228") },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true
                )
                Button(
                  onClick = {
                    if (inputConnectPeerId.isEmpty() || inputConnectAddress.isEmpty()) {
                      Toast.makeText(context, "Peer ID and Address required", Toast.LENGTH_SHORT).show()
                      return@Button
                    }
                    isActionLoading = true
                    coroutineScope.launch {
                      try {
                        val client = RpcClient(rpcUrl, rpcToken)
                        val params = mapOf(
                          "pubkey" to inputConnectPeerId,
                          "address" to inputConnectAddress
                        )
                        client.call("connect_peer", listOf(params))
                        Toast.makeText(context, "Connected to peer successfully!", Toast.LENGTH_SHORT).show()
                        inputConnectPeerId = ""
                        inputConnectAddress = ""
                        connectPeerExpanded = false
                        loadNodeData()
                      } catch (e: Exception) {
                        Toast.makeText(context, "Connection failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                      } finally {
                        isActionLoading = false
                      }
                    }
                  },
                  enabled = !isActionLoading,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(if (isActionLoading) "Connecting..." else "Connect")
                }
              }
            }
          }
        }

        // Action: Generate Invoice
        Card(modifier = Modifier.fillMaxWidth()) {
          Column {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { createInvoiceExpanded = !createInvoiceExpanded }
                .padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Create Payment Invoice", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
              Text(if (createInvoiceExpanded) "Collapse" else "Expand", color = MaterialTheme.colorScheme.primary)
            }
            AnimatedVisibility(visible = createInvoiceExpanded) {
              Column(
                modifier = Modifier
                  .padding(horizontal = 16.dp)
                  .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = inputInvoiceAmount,
                  onValueChange = { inputInvoiceAmount = it },
                  label = { Text("Invoice Amount (CKB)") },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true
                )
                OutlinedTextField(
                  value = inputInvoiceDesc,
                  onValueChange = { inputInvoiceDesc = it },
                  label = { Text("Description") },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true
                )
                 Button(
                  onClick = {
                    val amountVal = inputInvoiceAmount.toDoubleOrNull()
                    if (amountVal == null || amountVal <= 0.0) {
                      Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show()
                      return@Button
                    }
                    isActionLoading = true
                    coroutineScope.launch {
                      try {
                        val client = RpcClient(rpcUrl, rpcToken)
                        val shannonsVal = BigInteger(Math.round(amountVal * 1e8).toString())
                        val amountHex = "0x" + shannonsVal.toString(16)
                        
                        // Pick Fibt for testnet genesis, Fibh/Fibb for mainnet
                        val currencyStr = if (chainHash == "0x10639e0895502b5688a6be8cf69460d76541bfa4821629d86d62ba0aae3f9606") "Fibt" else "Fibb"
                        val params = mapOf(
                          "amount" to amountHex,
                          "description" to inputInvoiceDesc,
                          "currency" to currencyStr,
                          "expiry" to "0xe10"
                        )
                        val response = client.call("new_invoice", listOf(params))
                        val resObj = JSONObject(response).getJSONObject("result")
                        val invoiceAddr = resObj.getString("invoice_address")
                        generatedInvoice = invoiceAddr
                        settingsStore.addGeneratedInvoice(invoiceAddr)
                        generatedInvoicesList = settingsStore.getGeneratedInvoices()
                        Toast.makeText(context, "Invoice created!", Toast.LENGTH_SHORT).show()
                      } catch (e: Exception) {
                        Toast.makeText(context, "Invoice generation failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                      } finally {
                        isActionLoading = false
                      }
                    }
                  },
                  enabled = !isActionLoading,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(if (isActionLoading) "Creating..." else "Generate Invoice")
                }

                generatedInvoice?.let { invoice ->
                  Spacer(modifier = Modifier.height(8.dp))
                  Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                  ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                      Text(
                        text = "BOLT11 Invoice Address",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = invoice,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                      )
                      
                      Spacer(modifier = Modifier.height(12.dp))
                      
                      val qrBitmap = remember(invoice) {
                        try {
                          com.example.nervoscompanion.data.QrCodeGenerator.generateQrCode(invoice, 350)
                        } catch (e: Exception) {
                          null
                        }
                      }
                      
                      if (qrBitmap != null) {
                        Image(
                          bitmap = qrBitmap.asImageBitmap(),
                          contentDescription = "Invoice QR Code",
                          modifier = Modifier
                            .size(180.dp)
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            .background(Color.White)
                            .padding(6.dp)
                        )
                      }
                      
                      Spacer(modifier = Modifier.height(12.dp))
                      Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                          onClick = {
                            clipboardManager.setText(AnnotatedString(invoice))
                            Toast.makeText(context, "Invoice copied to clipboard!", Toast.LENGTH_SHORT).show()
                          }
                        ) {
                          Text("Copy Address")
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // Action: Scan to Pay Invoice
        Card(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Text("Scan to Pay", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Scan a BOLT11 invoice QR code via camera to pay instantly over Fiber Network.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                        scannedInvoice = rawValue
                        showPayInvoiceDialog = true
                      }
                    }
                    .addOnFailureListener { e ->
                      Toast.makeText(context, "Scanning failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                  Toast.makeText(context, "Scanner unavailable: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Scan Invoice to Pay")
            }
          }
        }

        // 5. Active Channels List
        Text(
          text = "Active Channels (${channelsList.size})",
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary
        )

        if (channelsList.isEmpty()) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
          ) {
            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
              Text("No active payment channels", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
          }
        } else {
          channelsList.forEach { channel ->
            val isChannelReady = channel.state.uppercase() == "CHANNEL_READY" || channel.state.uppercase() == "CHANNELREADY" || channel.state == "ChannelReady"
            Card(
              modifier = Modifier.fillMaxWidth(),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "ID: ${channel.id.take(8)}...${channel.id.takeLast(8)}",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium
                  )
                  Box(
                    modifier = Modifier
                      .background(
                        color = if (isChannelReady) Color(0xFF00CC99).copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                      )
                      .padding(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    Text(
                      text = channel.state,
                      color = if (isChannelReady) Color(0xFF00CC99) else Color.Red,
                      fontWeight = FontWeight.Bold,
                      style = MaterialTheme.typography.labelSmall
                    )
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Remote Peer: ${channel.peerId.take(16)}...",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Copy Peer ID",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                      clipboardManager.setText(AnnotatedString(channel.peerId))
                      Toast.makeText(context, "Peer ID copied!", Toast.LENGTH_SHORT).show()
                    }
                  )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text("Local Balance", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("${String.format("%.2f", channel.localBalance)} ${channel.assetName}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text("Remote Balance", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("${String.format("%.2f", channel.remoteBalance)} ${channel.assetName}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.End
                ) {
                  TextButton(
                    onClick = {
                      isActionLoading = true
                      coroutineScope.launch {
                        try {
                          val client = RpcClient(rpcUrl, rpcToken)
                          val params = mapOf("channel_id" to channel.id)
                          client.call("shutdown_channel", listOf(params))
                          Toast.makeText(context, "Channel shutdown request submitted!", Toast.LENGTH_LONG).show()
                          loadNodeData()
                        } catch (e: Exception) {
                          Toast.makeText(context, "Shutdown failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        } finally {
                          isActionLoading = false
                        }
                      }
                    },
                    enabled = !isActionLoading && isChannelReady,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                  ) {
                    Text("Close Channel")
                  }
                }
              }
            }
          }
        }

        // 6. Connected Peers List
        Text(
          text = "Connected Peers (${peersList.size})",
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary
        )

        if (peersList.isEmpty()) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
          ) {
            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
              Text("No connected peers", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
          }
        } else {
          peersList.forEach { peer ->
            Card(modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Peer: ${peer.peerId}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Copy ID",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                      clipboardManager.setText(AnnotatedString(peer.peerId))
                      Toast.makeText(context, "Peer ID copied!", Toast.LENGTH_SHORT).show()
                    }
                  )
                }
                peer.addresses.forEach { addr ->
                  Text(
                    text = addr,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray
                  )
                }
              }
            }
          }
        }

        // 7. Transaction & Invoice Logs
        var selectedLogTab by remember { mutableStateOf(0) } // 0 = Payments, 1 = Created Invoices

        Card(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Logs & History",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            TabRow(selectedTabIndex = selectedLogTab, containerColor = Color.Transparent) {
              Tab(
                selected = selectedLogTab == 0,
                onClick = { selectedLogTab = 0 },
                text = { Text("Payments Sent") }
              )
              Tab(
                selected = selectedLogTab == 1,
                onClick = { selectedLogTab = 1 },
                text = { Text("Invoices Created") }
              )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (selectedLogTab == 0) {
              // Payments Sent
              if (isLoadingPayments) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                  CircularProgressIndicator()
                }
              } else if (paymentsErrorMsg != null) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                  Text("Query not supported or requires authorization: $paymentsErrorMsg", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
              } else if (paymentsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                  Text("No payments found", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
              } else {
                paymentsList.forEach { payment ->
                  Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                  ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "Hash: ${payment.paymentHash.take(8)}...${payment.paymentHash.takeLast(8)}",
                          fontWeight = FontWeight.Bold,
                          fontFamily = FontFamily.Monospace,
                          style = MaterialTheme.typography.bodySmall
                        )
                        Box(
                          modifier = Modifier
                            .background(
                              color = when (payment.status.uppercase()) {
                                "SUCCESS", "SUCCEEDED" -> Color(0xFF00CC99).copy(alpha = 0.2f)
                                "FAILED" -> Color.Red.copy(alpha = 0.2f)
                                else -> Color.Yellow.copy(alpha = 0.2f)
                              },
                              shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                          Text(
                            text = payment.status,
                            color = when (payment.status.uppercase()) {
                              "SUCCESS", "SUCCEEDED" -> Color(0xFF00CC99)
                              "FAILED" -> Color.Red
                              else -> Color(0xFFFFCC00)
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                          )
                        }
                      }
                      Spacer(modifier = Modifier.height(6.dp))
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                      ) {
                        Text("Amount: ${String.format("%.2f", payment.amount)} CKB", style = MaterialTheme.typography.bodyMedium)
                        Text("Fee: ${String.format("%.4f", payment.fee)} CKB", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                      }
                      if (payment.destination.isNotEmpty() && payment.destination != "N/A") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text(
                            text = "To: ${payment.destination}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                          )
                          Spacer(modifier = Modifier.width(8.dp))
                          Text(
                            text = "Copy",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                              clipboardManager.setText(AnnotatedString(payment.destination))
                              Toast.makeText(context, "Recipient ID copied!", Toast.LENGTH_SHORT).show()
                            }
                          )
                        }
                      }
                    }
                  }
                }
              }
            } else {
              // Invoices Created
              if (generatedInvoicesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                  Text("No created invoices", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
              } else {
                generatedInvoicesList.forEach { invoice ->
                  Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                  ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                      Text(
                        text = invoice,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                      )
                      Spacer(modifier = Modifier.height(6.dp))
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                      ) {
                        TextButton(
                          onClick = {
                            clipboardManager.setText(AnnotatedString(invoice))
                            Toast.makeText(context, "Invoice copied!", Toast.LENGTH_SHORT).show()
                          }
                        ) {
                          Text("Copy")
                        }
                        TextButton(
                          onClick = {
                            generatedInvoice = invoice
                            createInvoiceExpanded = true
                          }
                        ) {
                          Text("Show QR")
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        if (showPayInvoiceDialog && scannedInvoice != null) {
          AlertDialog(
            onDismissRequest = {
              showPayInvoiceDialog = false
              scannedInvoice = null
            },
            title = { Text("Pay Invoice") },
            text = {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (parsingInvoice) {
                  CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                  Text("Parsing invoice details...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                } else {
                  Text("Invoice Address:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                  Text(
                    text = scannedInvoice ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                  )
                  
                  Spacer(modifier = Modifier.height(8.dp))
                  
                  Text("Amount:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                  Text(
                    text = if (parsedInvoiceAmount != null) "${String.format("%.2f", parsedInvoiceAmount)} CKB" else "Not specified",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                  
                  if (parsedInvoiceDesc.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Description:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(parsedInvoiceDesc, style = MaterialTheme.typography.bodyMedium)
                  }
                  
                  if (parsedInvoiceDestination.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Destination:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                      text = parsedInvoiceDestination,
                      style = MaterialTheme.typography.bodySmall,
                      fontFamily = FontFamily.Monospace,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            },
            confirmButton = {
              Button(
                onClick = {
                  val invoice = scannedInvoice ?: return@Button
                  isActionLoading = true
                  coroutineScope.launch {
                    try {
                      val client = RpcClient(rpcUrl, rpcToken)
                      val response = client.call("send_payment", listOf(mapOf("invoice" to invoice)))
                      val resObj = JSONObject(response)
                      if (resObj.has("result")) {
                        Toast.makeText(context, "Payment initiated successfully!", Toast.LENGTH_SHORT).show()
                        val result = resObj.getJSONObject("result")
                        val paymentHash = result.optString("payment_hash", "")
                        if (paymentHash.isNotEmpty()) {
                          settingsStore.addRecentTransaction(paymentHash)
                        }
                        loadNodeData()
                        loadPaymentHistory()
                      } else {
                        val error = resObj.optJSONObject("error")
                        val errMsg = error?.optString("message", "Unknown error") ?: "Unknown error"
                        Toast.makeText(context, "Payment failed: $errMsg", Toast.LENGTH_LONG).show()
                      }
                    } catch (e: Exception) {
                      Toast.makeText(context, "Payment failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    } finally {
                      isActionLoading = false
                      showPayInvoiceDialog = false
                      scannedInvoice = null
                    }
                  }
                },
                enabled = !isActionLoading && !parsingInvoice
              ) {
                Text(if (isActionLoading) "Paying..." else "Confirm & Pay")
              }
            },
            dismissButton = {
              TextButton(
                onClick = {
                  showPayInvoiceDialog = false
                  scannedInvoice = null
                },
                enabled = !isActionLoading
              ) {
                Text("Cancel")
              }
            }
          )
        }

      } else {
        // Offline Warning state details
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Offline Check Results", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = errorMsg ?: "Unable to establish contact with the Fiber Network node at $rpcUrl. Please verify the fnn process is running and its RPC server is active.",
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Button(
                onClick = { loadNodeData() },
                modifier = Modifier.weight(1f)
              ) {
                Text("Retry Connection")
              }
              Button(
                onClick = { onNavigate(Settings) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f)
              ) {
                Text("Check Settings")
              }
            }
          }
        }
      }
    }
  }
}
