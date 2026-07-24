package com.example.nervoscompanion.ui.tools

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.nervoscompanion.data.RpcClient
import com.example.nervoscompanion.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// 1. Math, Protocols & Parsing Structures
// ==========================================

data class DaoFields(
  val totalIssuance: BigInteger,     // C_i
  val accumulatedRate: BigInteger,   // AR_i
  val secondaryIssuance: BigInteger, // S_i
  val occupiedCapacity: BigInteger   // U_i
)

data class DaoHeaderSnapshot(
  val blockHash: String,
  val blockNumber: Long,
  val epochNumber: Long,
  val timestamp: Long,
  val ar: BigInteger,
  val occupiedCapacity: BigInteger
)

data class ParsedOutput(
  val index: Int,
  val capacity: BigInteger,
  val lockArgs: String,
  val lockCodeHash: String,
  val typeArgs: String?,
  val typeCodeHash: String?,
  val data: String,
  val isDao: Boolean,
  val occupiedCapacity: BigInteger
)

data class ParsedInput(
  val prevTxHash: String,
  val prevIndex: Int,
  val id: String = "$prevTxHash:$prevIndex"
)

data class ParsedTransaction(
  val txHash: String,
  val blockHash: String,
  val blockNumber: Long?,
  val status: String,
  val timestamp: Long?,
  val inputs: List<ParsedInput>,
  val outputs: List<ParsedOutput>,
  val size: Int
)

data class LiveDaoCellResult(
  val txHash: String,
  val index: Int,
  val status: String, // "deposited" | "withdrawing" | "unlocked/dead" | "unknown"
  val totalCapacity: BigInteger,
  val occupiedCapacity: BigInteger,
  val depositBlockNumber: Long?,
  val depositHeader: DaoHeaderSnapshot?,
  val withdrawHeader: DaoHeaderSnapshot?,
  val tipHeader: DaoHeaderSnapshot?
)

object DaoMath {
  private val SHANNONS_PER_CKB = BigInteger.valueOf(100_000_000L)

  fun parseLittleEndianHex(hexStr: String): BigInteger {
    val clean = if (hexStr.startsWith("0x")) hexStr.substring(2) else hexStr
    val reversedBytes = StringBuilder()
    for (i in clean.length - 2 downTo 0 step 2) {
      reversedBytes.append(clean[i])
      reversedBytes.append(clean[i + 1])
    }
    return BigInteger(reversedBytes.toString(), 16)
  }

  fun parseDaoField(daoHex: String): DaoFields {
    val clean = if (daoHex.startsWith("0x")) daoHex.substring(2) else daoHex
    if (clean.length != 64) {
      throw IllegalArgumentException("Invalid DAO field length: ${clean.length}")
    }
    return DaoFields(
      totalIssuance = parseLittleEndianHex(clean.substring(0, 16)),
      accumulatedRate = parseLittleEndianHex(clean.substring(16, 32)),
      secondaryIssuance = parseLittleEndianHex(clean.substring(32, 48)),
      occupiedCapacity = parseLittleEndianHex(clean.substring(48, 64))
    )
  }

  fun shannonsToCkb(shannons: BigInteger): String {
    val whole = shannons.divide(SHANNONS_PER_CKB)
    val frac = shannons.mod(SHANNONS_PER_CKB)
    return "$whole.${frac.toString().padStart(8, '0')}"
  }

  fun ckbToShannons(ckb: Double): BigInteger {
    return BigDecimal.valueOf(ckb)
      .multiply(BigDecimal.valueOf(100_000_000L))
      .toBigInteger()
  }

  fun hexToByteLen(hex: String): Long {
    val clean = if (hex.startsWith("0x")) hex.substring(2) else hex
    return clean.length.toLong() / 2
  }

  fun computeCellOccupiedCapacity(
    lockArgs: String,
    typeCodeHash: String?,
    typeArgs: String?,
    outputData: String
  ): BigInteger {
    val CAPACITY_FIELD_BYTES = 8L
    val CODE_HASH_BYTES = 32L
    val HASH_TYPE_BYTES = 1L

    val lockBytes = CODE_HASH_BYTES + HASH_TYPE_BYTES + hexToByteLen(lockArgs)
    val typeBytes = if (!typeCodeHash.isNullOrEmpty()) {
      CODE_HASH_BYTES + HASH_TYPE_BYTES + hexToByteLen(typeArgs ?: "")
    } else {
      0L
    }
    val dataBytes = hexToByteLen(outputData)
    val totalBytes = CAPACITY_FIELD_BYTES + lockBytes + typeBytes + dataBytes
    return BigInteger.valueOf(totalBytes).multiply(SHANNONS_PER_CKB)
  }

  // CKB Issuance & Forecast Schedule Constants
  const val EPOCHS_PER_ERA = 8760L
  val ERA_0_PRIMARY_EPOCH_REWARD = BigInteger("191780821917808")
  val SECONDARY_EPOCH_REWARD = BigInteger("61369863013698")
  val GENESIS_TOTAL_SUPPLY = BigInteger("3360000000000000000") // 33.6B CKB

  fun totalIssuedAtEpoch(epoch: Long): BigInteger {
    var total = GENESIS_TOTAL_SUPPLY
    var remaining = epoch
    var reward = ERA_0_PRIMARY_EPOCH_REWARD
    while (remaining > 0L) {
      val take = if (remaining < EPOCHS_PER_ERA) remaining else EPOCHS_PER_ERA
      val rewardSum = reward.add(SECONDARY_EPOCH_REWARD)
      total = total.add(rewardSum.multiply(BigInteger.valueOf(take)))
      remaining -= take
      reward = reward.shiftRight(1)
    }
    return total
  }

  fun forecastAr(
    fromAr: BigInteger,
    fromEpoch: Long,
    toEpoch: Long,
    observedOccupied: BigInteger
  ): BigInteger {
    if (toEpoch <= fromEpoch) return fromAr

    var ar = fromAr
    var totalIssued = totalIssuedAtEpoch(fromEpoch)
    var currentEra = fromEpoch / EPOCHS_PER_ERA
    var primaryReward = ERA_0_PRIMARY_EPOCH_REWARD.shiftRight(currentEra.toInt())
    var epochInEra = fromEpoch % EPOCHS_PER_ERA

    for (i in fromEpoch until toEpoch) {
      val freeCapacity = totalIssued.subtract(observedOccupied)
      if (freeCapacity <= BigInteger.ZERO) break
      val delta = ar.multiply(SECONDARY_EPOCH_REWARD).divide(freeCapacity)
      ar = ar.add(delta)

      totalIssued = totalIssued.add(primaryReward).add(SECONDARY_EPOCH_REWARD)
      epochInEra += 1L
      if (epochInEra == EPOCHS_PER_ERA) {
        currentEra += 1L
        primaryReward = primaryReward.shiftRight(1)
        epochInEra = 0L
      }
    }
    return ar
  }

  fun calculateApc(totalIssuance: BigInteger, occupiedCapacity: BigInteger): Double {
    val freeCapacity = totalIssuance.subtract(occupiedCapacity)
    if (freeCapacity <= BigInteger.ZERO) return 0.0
    val secondaryPerYear = BigInteger("134400000000000000") // 1.344B CKB Shannons
    return secondaryPerYear.toBigDecimal()
      .divide(freeCapacity.toBigDecimal(), 8, RoundingMode.HALF_UP)
      .multiply(BigDecimal("100"))
      .toDouble()
  }

  fun computeDaoAccrual(
    totalCapacity: BigInteger,
    occupiedCapacity: BigInteger,
    depositAr: BigInteger,
    targetAr: BigInteger
  ): BigInteger {
    if (occupiedCapacity >= totalCapacity || depositAr <= BigInteger.ZERO || targetAr < depositAr) {
      return BigInteger.ZERO
    }
    val countedCapacity = totalCapacity.subtract(occupiedCapacity)
    val arDiff = targetAr.subtract(depositAr)
    return countedCapacity.multiply(arDiff).divide(depositAr)
  }
}

// ==========================================
// 2. RPC Client Operations
// ==========================================

object CkbRpcOps {
  private fun parseHeader(headerObj: JSONObject): DaoHeaderSnapshot {
    val blockHash = headerObj.getString("hash")
    val numberHex = headerObj.getString("number")
    val blockNumber = numberHex.substring(2).toLong(16)

    val epochHex = headerObj.getString("epoch")
    val epochVal = epochHex.substring(2).toLong(16)
    val epochNumber = epochVal and 0xFFFFFFL

    val timestampHex = headerObj.getString("timestamp")
    val timestamp = timestampHex.substring(2).toLong(16)

    val daoHex = headerObj.getString("dao")

    return DaoHeaderSnapshot(
      blockHash = blockHash,
      blockNumber = blockNumber,
      epochNumber = epochNumber,
      timestamp = timestamp,
      ar = DaoMath.parseDaoField(daoHex).accumulatedRate,
      occupiedCapacity = DaoMath.parseDaoField(daoHex).occupiedCapacity
    )
  }

  suspend fun getTransaction(rpcUrl: String, txHash: String): ParsedTransaction = withContext(Dispatchers.IO) {
    val client = RpcClient(rpcUrl)
    val response = client.call("get_transaction", listOf(txHash))
    val result = JSONObject(response).optJSONObject("result") ?: throw Exception("Transaction not found")

    val txObj = result.getJSONObject("transaction")
    val size = txObj.toString().length // rough estimate of bytes size in JSON

    val txStatus = result.getJSONObject("tx_status")
    val status = txStatus.getString("status")
    val blockHash = txStatus.optString("block_hash", "")

    var blockNum: Long? = null
    var timestamp: Long? = null

    if (blockHash.isNotEmpty()) {
      try {
        val headerRes = client.call("get_header", listOf(blockHash))
        val headerObj = JSONObject(headerRes).getJSONObject("result")
        val snapshot = parseHeader(headerObj)
        blockNum = snapshot.blockNumber
        timestamp = snapshot.timestamp
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    val inputsArray = txObj.getJSONArray("inputs")
    val inputs = mutableListOf<ParsedInput>()
    for (i in 0 until inputsArray.length()) {
      val inp = inputsArray.getJSONObject(i)
      val prevOut = inp.getJSONObject("previous_output")
      val prevHash = prevOut.getString("tx_hash")
      val prevIndexHex = prevOut.getString("index")
      val prevIdx = prevIndexHex.substring(2).toInt(16)
      inputs.add(ParsedInput(prevHash, prevIdx))
    }

    val outputsArray = txObj.getJSONArray("outputs")
    val outputsDataArray = txObj.getJSONArray("outputs_data")
    val outputs = mutableListOf<ParsedOutput>()

    val daoCodeHash = "0x82d76d1b75fe2fd9a27dfbaa65a039221a380d76c926f378d3f81cf3e7e13f2e"

    for (i in 0 until outputsArray.length()) {
      val out = outputsArray.getJSONObject(i)
      val capHex = out.getString("capacity")
      val capacity = capHex.substring(2).toBigInteger(16)

      val lockObj = out.getJSONObject("lock")
      val lockArgs = lockObj.getString("args")
      val lockCodeHash = lockObj.getString("code_hash")

      val typeObj = out.optJSONObject("type")
      val typeArgs = typeObj?.optString("args")
      val typeCodeHash = typeObj?.optString("code_hash")

      val data = outputsDataArray.optString(i, "0x")
      val isDao = typeCodeHash?.lowercase() == daoCodeHash.lowercase()

      val occupied = DaoMath.computeCellOccupiedCapacity(
        lockArgs = lockArgs,
        typeCodeHash = typeCodeHash,
        typeArgs = typeArgs,
        outputData = data
      )

      outputs.add(
        ParsedOutput(
          index = i,
          capacity = capacity,
          lockArgs = lockArgs,
          lockCodeHash = lockCodeHash,
          typeArgs = typeArgs,
          typeCodeHash = typeCodeHash,
          data = data,
          isDao = isDao,
          occupiedCapacity = occupied
        )
      )
    }

    ParsedTransaction(
      txHash = txHash,
      blockHash = blockHash,
      blockNumber = blockNum,
      status = status,
      timestamp = timestamp,
      inputs = inputs,
      outputs = outputs,
      size = size
    )
  }

  suspend fun getHeaderByNumber(rpcUrl: String, blockNumber: Long): DaoHeaderSnapshot = withContext(Dispatchers.IO) {
    val client = RpcClient(rpcUrl)
    val numHex = "0x" + blockNumber.toString(16)
    val response = client.call("get_header_by_number", listOf(numHex))
    val result = JSONObject(response).optJSONObject("result") ?: throw Exception("Block header #$blockNumber not found")
    parseHeader(result)
  }

  suspend fun getTipHeader(rpcUrl: String): DaoHeaderSnapshot = withContext(Dispatchers.IO) {
    val client = RpcClient(rpcUrl)
    val response = client.call("get_tip_header", emptyList())
    val result = JSONObject(response).getJSONObject("result")
    parseHeader(result)
  }

  suspend fun getLiveCell(rpcUrl: String, txHash: String, index: Int): LiveDaoCellResult = withContext(Dispatchers.IO) {
    val client = RpcClient(rpcUrl)
    val outPointObj = JSONObject().apply {
      put("tx_hash", txHash)
      put("index", "0x" + index.toString(16))
    }
    // Call get_live_cell with true to get output data details
    val response = client.call("get_live_cell", listOf(outPointObj, true))
    val resultObj = JSONObject(response).getJSONObject("result")
    val cellStatus = resultObj.getString("status") // "live" | "dead" | "unknown"

    if (cellStatus != "live") {
      // Cell is spent/dead, but we can still check the original tx details to get the capacities
      val origTx = getTransaction(rpcUrl, txHash)
      val output = origTx.outputs.find { it.index == index } ?: throw Exception("Output cell not found in original transaction")
      return@withContext LiveDaoCellResult(
        txHash = txHash,
        index = index,
        status = "unlocked/dead",
        totalCapacity = output.capacity,
        occupiedCapacity = output.occupiedCapacity,
        depositBlockNumber = null,
        depositHeader = null,
        withdrawHeader = null,
        tipHeader = null
      )
    }

    // Cell is live
    val cellObj = resultObj.getJSONObject("cell")
    val outputObj = cellObj.getJSONObject("output")
    val capacityHex = outputObj.getString("capacity")
    val totalCapacity = capacityHex.substring(2).toBigInteger(16)

    val lockObj = outputObj.getJSONObject("lock")
    val lockArgs = lockObj.getString("args")
    val lockCodeHash = lockObj.getString("code_hash")

    val typeObj = outputObj.optJSONObject("type")
    val typeArgs = typeObj?.optString("args")
    val typeCodeHash = typeObj?.optString("code_hash")

    val dataObj = cellObj.getJSONObject("data")
    val contentData = dataObj.optString("content", "0x")

    val occupiedCapacity = DaoMath.computeCellOccupiedCapacity(
      lockArgs = lockArgs,
      typeCodeHash = typeCodeHash,
      typeArgs = typeArgs,
      outputData = contentData
    )

    // Check if cell is DAO cell
    val daoCodeHash = "0x82d76d1b75fe2fd9a27dfbaa65a039221a380d76c926f378d3f81cf3e7e13f2e"
    if (typeCodeHash?.lowercase() != daoCodeHash.lowercase()) {
      throw Exception("Not a DAO cell (type code hash does not match standard Nervos DAO)")
    }

    // Determine lock state from data:
    // Deposited: 8 bytes of zero (0x0000000000000000)
    // Phase 1 Withdraw: 8 bytes little-endian deposit block number
    val cleanData = if (contentData.startsWith("0x")) contentData.substring(2) else contentData
    val isDeposited = cleanData.all { it == '0' } || cleanData.isEmpty()

    val tip = getTipHeader(rpcUrl)
    var depositBlockNum: Long? = null
    var depositHeader: DaoHeaderSnapshot? = null
    var withdrawHeader: DaoHeaderSnapshot? = null

    val transactionRes = client.call("get_transaction", listOf(txHash))
    val txResultObj = JSONObject(transactionRes).getJSONObject("result")
    val txStatusObj = txResultObj.getJSONObject("tx_status")
    val blockHash = txStatusObj.getString("block_hash")

    val currentHeaderRes = client.call("get_header", listOf(blockHash))
    val currentHeader = parseHeader(JSONObject(currentHeaderRes).getJSONObject("result"))

    val kind: String
    if (isDeposited) {
      kind = "deposited"
      depositHeader = currentHeader
      depositBlockNum = currentHeader.blockNumber
    } else {
      kind = "withdrawing"
      // parse deposit block number from little endian data
      val depBlockNum = DaoMath.parseLittleEndianHex(cleanData).toLong()
      depositBlockNum = depBlockNum
      depositHeader = getHeaderByNumber(rpcUrl, depBlockNum)
      withdrawHeader = currentHeader
    }

    LiveDaoCellResult(
      txHash = txHash,
      index = index,
      status = kind,
      totalCapacity = totalCapacity,
      occupiedCapacity = occupiedCapacity,
      depositBlockNumber = depositBlockNum,
      depositHeader = depositHeader,
      withdrawHeader = withdrawHeader,
      tipHeader = tip
    )
  }
}

// ==========================================
// 3. UI Presentation Layer
// ==========================================

@Composable
fun TxCalculatorScreen(
  onNavigate: (NavKey) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val settingsStore = remember { SettingsStore(context) }
  val coroutineScope = rememberCoroutineScope()
  val clipboardManager = LocalClipboardManager.current

  val rpcUrl = settingsStore.rpcUrl
  var selectedTab by remember { mutableStateOf(0) } // 0 = Tx Viewer, 1 = DAO Calculator

  // TAB 0: Tx Viewer States
  var txHashInput by remember { mutableStateOf("") }
  var recentTxs by remember { mutableStateOf(settingsStore.getRecentTransactions()) }
  var txLoading by remember { mutableStateOf(false) }
  var txErrorMsg by remember { mutableStateOf<String?>(null) }
  var parsedTx by remember { mutableStateOf<ParsedTransaction?>(null) }
  val resolvedInputCapacities = remember { mutableStateMapOf<String, BigInteger>() }

  // TAB 1: DAO Calculator States
  var lookupTxHashInput by remember { mutableStateOf("") }
  var lookupIndexInput by remember { mutableStateOf("0") }
  var lookupLoading by remember { mutableStateOf(false) }
  var lookupErrorMsg by remember { mutableStateOf<String?>(null) }
  var cellResult by remember { mutableStateOf<LiveDaoCellResult?>(null) }

  // Interactive Forecasting Estimator States
  var estAmountInput by remember { mutableStateOf("10000") }
  var estDurationMonths by remember { mutableStateOf(12) } // 12 months = 1 year
  var estApcInput by remember { mutableStateOf("2.25") }

  // Fetch tip details on load to prefill estimator defaults
  var tipHeaderState by remember { mutableStateOf<DaoHeaderSnapshot?>(null) }
  LaunchedEffect(Unit) {
    launch {
      try {
        val tip = CkbRpcOps.getTipHeader(rpcUrl)
        tipHeaderState = tip
        val calcApc = DaoMath.calculateApc(tip.ar, tip.occupiedCapacity)
        if (calcApc > 0.0) {
          estApcInput = String.format(Locale.US, "%.2f", calcApc)
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  // Trigger input capacity resolution when parsedTx changes
  LaunchedEffect(parsedTx) {
    resolvedInputCapacities.clear()
    val tx = parsedTx ?: return@LaunchedEffect
    // Resolve capacities asynchronously in background for up to 10 inputs automatically
    if (tx.inputs.isNotEmpty()) {
      val limit = if (tx.inputs.size <= 10) tx.inputs.size else 10
      for (i in 0 until limit) {
        val input = tx.inputs[i]
        launch {
          try {
            val cell = CkbRpcOps.getTransaction(rpcUrl, input.prevTxHash)
            val output = cell.outputs.find { it.index == input.prevIndex }
            if (output != null) {
              resolvedInputCapacities[input.id] = output.capacity
            }
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
      }
    }
  }

  fun executeTxSearch(hash: String) {
    val cleanHash = hash.trim()
    if (cleanHash.length != 66 || !cleanHash.startsWith("0x")) {
      Toast.makeText(context, "Invalid CKB hash format", Toast.LENGTH_SHORT).show()
      return
    }
    txLoading = true
    txErrorMsg = null
    parsedTx = null
    coroutineScope.launch {
      try {
        val tx = CkbRpcOps.getTransaction(rpcUrl, cleanHash)
        parsedTx = tx
        settingsStore.addRecentTransaction(cleanHash)
        recentTxs = settingsStore.getRecentTransactions()
      } catch (e: Exception) {
        txErrorMsg = e.localizedMessage ?: "Failed to fetch transaction"
      } finally {
        txLoading = false
      }
    }
  }

  fun executeCellLookup(hash: String, indexStr: String) {
    val cleanHash = hash.trim()
    val idx = indexStr.toIntOrNull() ?: 0
    if (cleanHash.length != 66 || !cleanHash.startsWith("0x") || idx < 0) {
      Toast.makeText(context, "Invalid parameters", Toast.LENGTH_SHORT).show()
      return
    }
    lookupLoading = true
    lookupErrorMsg = null
    cellResult = null
    coroutineScope.launch {
      try {
        val result = CkbRpcOps.getLiveCell(rpcUrl, cleanHash, idx)
        cellResult = result
      } catch (e: Exception) {
        lookupErrorMsg = e.localizedMessage ?: "Failed to query cell"
      } finally {
        lookupLoading = false
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF070B0E)) // Stealth rich dark theme
  ) {
    // 1. TOP BAR
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = { onNavigate(com.example.nervoscompanion.Tools) }) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(
          text = "Tx & DAO Yield Utility",
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "RPC: $rpcUrl",
          color = Color.Gray,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    // 2. MODERN TAB BAR
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = Color(0xFF0F161E),
      contentColor = MaterialTheme.colorScheme.primary,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = Color(0xFF8E2DE2) // Custom purple active line indicator
        )
      }
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("Tx Viewer", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("DAO Calculator", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
      )
    }

    // 3. TAB CONTENT
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      if (selectedTab == 0) {
        // TAB 0: TRANSACTION VIEWER
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // SEARCH FORM
          item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = txHashInput,
                onValueChange = { txHashInput = it },
                label = { Text("Transaction Hash") },
                placeholder = { Text("0x...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                  if (txHashInput.isNotEmpty()) executeTxSearch(txHashInput)
                }),
                trailingIcon = {
                  IconButton(onClick = { if (txHashInput.isNotEmpty()) executeTxSearch(txHashInput) }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                  }
                }
              )

              // RECENT LIST
              if (recentTxs.isNotEmpty()) {
                Text(
                  text = "Recent Searches",
                  color = Color.Gray,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  items(recentTxs) { hash ->
                    SuggestionChip(
                      onClick = {
                        txHashInput = hash
                        executeTxSearch(hash)
                      },
                      label = {
                        Text(
                          text = hash.take(8) + "..." + hash.takeLast(6),
                          fontFamily = FontFamily.Monospace,
                          fontSize = 11.sp
                        )
                      }
                    )
                  }
                }
              }
            }
          }

          // LOADING STATE
          if (txLoading) {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(200.dp),
                contentAlignment = Alignment.Center
              ) {
                CircularProgressIndicator(color = Color(0xFF8E2DE2))
              }
            }
          }

          // ERROR STATE
          if (txErrorMsg != null) {
            item {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Text(
                    text = txErrorMsg ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 14.sp
                  )
                }
              }
            }
          }

          // RENDER PARSED TX DETAILS
          val tx = parsedTx
          if (tx != null && !txLoading) {
            // TX OVERVIEW CARD
            item {
              Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F161E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "Transaction Info",
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      fontSize = 16.sp
                    )
                    Box(
                      modifier = Modifier
                        .background(
                          if (tx.status == "committed") Color(0xFF00CC99).copy(alpha = 0.2f) else Color.Yellow.copy(alpha = 0.2f),
                          RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                      Text(
                        text = tx.status.uppercase(),
                        color = if (tx.status == "committed") Color(0xFF00CC99) else Color.Yellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }

                  HorizontalDivider(color = Color.DarkGray)

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Block Height", color = Color.Gray, fontSize = 13.sp)
                    Text(
                      text = tx.blockNumber?.toString() ?: "Pending",
                      color = Color.White,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Timestamp", color = Color.Gray, fontSize = 13.sp)
                    val dateText = remember(tx.timestamp) {
                      if (tx.timestamp != null) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        sdf.format(Date(tx.timestamp))
                      } else {
                        "Pending"
                      }
                    }
                    Text(
                      text = dateText,
                      color = Color.White,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Data Size", color = Color.Gray, fontSize = 13.sp)
                    Text(
                      text = "${tx.size} bytes",
                      color = Color.White,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }

                  // TX FEE (Calculated only if inputs resolved)
                  val totalOutput = remember(tx.outputs) {
                    tx.outputs.fold(BigInteger.ZERO) { acc, out -> acc.add(out.capacity) }
                  }
                  val inputsCount = tx.inputs.size
                  val resolvedCount = resolvedInputCapacities.size
                  val totalInput = if (resolvedCount == inputsCount) {
                    tx.inputs.fold(BigInteger.ZERO) { acc, inp ->
                      acc.add(resolvedInputCapacities[inp.id] ?: BigInteger.ZERO)
                    }
                  } else {
                    null
                  }

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tx Fee", color = Color.Gray, fontSize = 13.sp)
                    if (totalInput != null) {
                      val fee = totalInput.subtract(totalOutput)
                      Text(
                        text = "${DaoMath.shannonsToCkb(fee)} CKB",
                        color = Color(0xFF00CC99),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                      )
                    } else {
                      Text(
                        text = "Resolving fees ($resolvedCount/$inputsCount)...",
                        color = Color.Yellow,
                        fontSize = 13.sp
                      )
                    }
                  }
                }
              }
            }

            // INPUTS SECTION
            item {
              Text(
                text = "Inputs (${tx.inputs.size})",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }

            items(tx.inputs) { input ->
              val capacity = resolvedInputCapacities[input.id]
              Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F14)),
                modifier = Modifier
                  .fillMaxWidth()
                  .border(0.5.dp, Color.DarkGray, RoundedCornerShape(8.dp))
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "Outpoint: ${input.prevTxHash.take(6)}...${input.prevTxHash.takeLast(6)} # ${input.prevIndex}",
                      fontFamily = FontFamily.Monospace,
                      fontSize = 12.sp,
                      color = Color.White,
                      maxLines = 1
                    )
                    IconButton(
                      onClick = {
                        clipboardManager.setText(AnnotatedString(input.prevTxHash))
                        Toast.makeText(context, "Tx Hash Copied", Toast.LENGTH_SHORT).show()
                      },
                      modifier = Modifier.size(24.dp)
                    ) {
                      Icon(
                        Icons.Default.Share,
                        contentDescription = "Copy hash",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text("Resolved Capacity", color = Color.Gray, fontSize = 12.sp)
                    Text(
                      text = if (capacity != null) "${DaoMath.shannonsToCkb(capacity)} CKB" else "Loading...",
                      color = if (capacity != null) Color.White else Color.Yellow,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }
              }
            }

            // OUTPUTS SECTION
            item {
              Text(
                text = "Outputs (${tx.outputs.size})",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }

            items(tx.outputs) { output ->
              Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F14)),
                modifier = Modifier
                  .fillMaxWidth()
                  .border(0.5.dp, Color.DarkGray, RoundedCornerShape(8.dp))
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = "Output Index #${output.index}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                      )
                      if (output.isDao) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                          modifier = Modifier
                            .background(Color(0xFF8E2DE2).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                          Text(
                            text = "NERVOS DAO",
                            color = Color(0xFF8E2DE2),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                          )
                        }
                      }
                    }
                    Text(
                      text = "${DaoMath.shannonsToCkb(output.capacity)} CKB",
                      color = Color.White,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Occupied storage", color = Color.Gray, fontSize = 12.sp)
                    Text(
                      text = "${DaoMath.shannonsToCkb(output.occupiedCapacity)} CKB",
                      color = Color.Gray,
                      fontSize = 12.sp
                    )
                  }

                  if (output.isDao) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                      onClick = {
                        lookupTxHashInput = tx.txHash
                        lookupIndexInput = output.index.toString()
                        selectedTab = 1 // Switch to calculator tab
                        executeCellLookup(tx.txHash, output.index.toString())
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E2DE2)),
                      modifier = Modifier.align(Alignment.End),
                      shape = RoundedCornerShape(6.dp),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                      Text("Calculate DAO Staking Yield", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      } else {
        // TAB 1: DAO YIELD CALCULATOR
        val scrollState = rememberScrollState()
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
          // SECTION A: ON-CHAIN CELL LOOKUP & ACCRUAL CALCULATOR
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F161E)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Text(
                text = "On-Chain Cell Lookup",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedTextField(
                  value = lookupTxHashInput,
                  onValueChange = { lookupTxHashInput = it },
                  label = { Text("Tx Hash") },
                  placeholder = { Text("0x...") },
                  modifier = Modifier.weight(1f),
                  singleLine = true
                )
                OutlinedTextField(
                  value = lookupIndexInput,
                  onValueChange = { lookupIndexInput = it },
                  label = { Text("Idx") },
                  modifier = Modifier.width(60.dp),
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
              }

              Button(
                onClick = { executeCellLookup(lookupTxHashInput, lookupIndexInput) },
                enabled = !lookupLoading && lookupTxHashInput.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CC99), contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = if (lookupLoading) "Loading cell details..." else "Lookup Cell & Calculate Accrued Yield",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
              }

              if (lookupErrorMsg != null) {
                Text(
                  text = lookupErrorMsg ?: "",
                  color = MaterialTheme.colorScheme.error,
                  fontSize = 12.sp
                )
              }

              // RENDER DETAILED DAO ACCRUAL BREAKDOWN CARD
              val cell = cellResult
              if (cell != null && !lookupLoading) {
                HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))

                Text(
                  text = "DAO Cell Accrual Breakdown",
                  color = Color.LightGray,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold
                )

                // STAKING LOCK STATUS BADGE
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("Lock Status", color = Color.Gray, fontSize = 12.sp)
                  val badgeColor: Color
                  val badgeText: String
                  when (cell.status) {
                    "deposited" -> {
                      badgeColor = Color(0xFF00CC99)
                      badgeText = "Deposited (Live, Accruing)"
                    }
                    "withdrawing" -> {
                      badgeColor = Color(0xFF00C6FF)
                      badgeText = "Phase 1 Withdrawal (Frozen)"
                    }
                    else -> {
                      badgeColor = Color.Gray
                      badgeText = "Unlocked/Fully Withdrawn"
                    }
                  }
                  Box(
                    modifier = Modifier
                      .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = badgeText.uppercase(),
                      color = badgeColor,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }

                // CAPACITY FIELD SPLITS
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Total Deposited Capacity", color = Color.Gray, fontSize = 12.sp)
                  Text(
                    text = "${DaoMath.shannonsToCkb(cell.totalCapacity)} CKB",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Occupied storage capacity", color = Color.Gray, fontSize = 12.sp)
                  Text(
                    text = "${DaoMath.shannonsToCkb(cell.occupiedCapacity)} CKB",
                    color = Color.Gray,
                    fontSize = 12.sp
                  )
                }

                val countedCapacity = cell.totalCapacity.subtract(cell.occupiedCapacity)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Counted capacity (generating yield)", color = Color.Gray, fontSize = 12.sp)
                  Text(
                    text = "${DaoMath.shannonsToCkb(countedCapacity)} CKB",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }

                // INTEREST ACCRUAL CALCULATOR (RFC-0023)
                if (cell.depositHeader != null) {
                  val targetHeader = cell.withdrawHeader ?: cell.tipHeader
                  if (targetHeader != null) {
                    val accrual = DaoMath.computeDaoAccrual(
                      totalCapacity = cell.totalCapacity,
                      occupiedCapacity = cell.occupiedCapacity,
                      depositAr = cell.depositHeader.ar,
                      targetAr = targetHeader.ar
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                      Text("Accrued secondary reward", color = Color.Gray, fontSize = 12.sp)
                      Text(
                        text = "+${DaoMath.shannonsToCkb(accrual)} CKB",
                        color = Color(0xFF00CC99),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                      Text("Max unlockable capacity", color = Color.Gray, fontSize = 12.sp)
                      val maxVal = cell.totalCapacity.add(accrual)
                      Text(
                        text = "${DaoMath.shannonsToCkb(maxVal)} CKB",
                        color = Color(0xFF8E2DE2),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                    ) {
                      Text(
                        text = "Staked from Block #${cell.depositHeader.blockNumber} (Epoch #${cell.depositHeader.epochNumber}) " +
                          "to ${if (cell.status == "withdrawing") "Withdrawal Block #${targetHeader.blockNumber}" else "Tip Block #${targetHeader.blockNumber}"}.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                      )
                    }
                  }
                }
              }
            }
          }

          // SECTION B: INTERACTIVE FUTURE FORECAST ESTIMATOR
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F161E)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Text(
                text = "Future Yield Estimator (Compounding Projection)",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )

              OutlinedTextField(
                value = estAmountInput,
                onValueChange = { estAmountInput = it },
                label = { Text("Initial Staking Amount (CKB)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
              )

              // LOCK-UP DURATION BUTTONS
              Text("Projected Lock Duration", color = Color.Gray, fontSize = 12.sp)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                listOf(1, 6, 12, 36, 60).forEach { months ->
                  val label = when (months) {
                    1 -> "1 Mon"
                    6 -> "6 Mon"
                    12 -> "1 Yr"
                    36 -> "3 Yrs"
                    else -> "5 Yrs"
                  }
                  val isSelected = estDurationMonths == months
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .background(
                        if (isSelected) Color(0xFF8E2DE2) else Color.DarkGray.copy(alpha = 0.4f),
                        RoundedCornerShape(6.dp)
                      )
                      .clickable { estDurationMonths = months }
                      .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = label,
                      color = Color.White,
                      fontSize = 12.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                  }
                }
              }

              OutlinedTextField(
                value = estApcInput,
                onValueChange = { estApcInput = it },
                label = { Text("Manual APC Rate % (Estimated annual compensation)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
              )

              // RUN FORECAST CALCULATIONS
              val initialAmountDouble = estAmountInput.toDoubleOrNull() ?: 10000.0
              val apcPercent = estApcInput.toDoubleOrNull() ?: 2.25

              val projectedReward = remember(initialAmountDouble, estDurationMonths, apcPercent, tipHeaderState) {
                val tip = tipHeaderState
                val initialShannons = DaoMath.ckbToShannons(initialAmountDouble)
                val epochs = estDurationMonths.toLong() * 180L

                if (tip != null) {
                  // Forecast AR precisely based on protocol rules
                  val futureEpoch = tip.epochNumber + epochs
                  val futureAr = DaoMath.forecastAr(
                    fromAr = tip.ar,
                    fromEpoch = tip.epochNumber,
                    toEpoch = futureEpoch,
                    observedOccupied = tip.occupiedCapacity
                  )
                  // Use 102 CKB storage occupancy default for projection
                  val occupiedShannons = DaoMath.ckbToShannons(102.0)
                  val stakeCapacity = if (initialShannons > occupiedShannons) {
                    initialShannons.subtract(occupiedShannons)
                  } else {
                    initialShannons
                  }
                  stakeCapacity.multiply(futureAr.subtract(tip.ar)).divide(tip.ar)
                } else {
                  // Fallback calculation using custom compounding formula:
                  // Amount * ((1 + apc/12)^months - 1)
                  val monthlyRate = (apcPercent / 100.0) / 12.0
                  val compoundFactor = Math.pow(1.0 + monthlyRate, estDurationMonths.toDouble())
                  val expected = initialAmountDouble * (compoundFactor - 1.0)
                  DaoMath.ckbToShannons(expected)
                }
              }

              HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))

              Text(
                text = "Estimated Projected Returns",
                color = Color.LightGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Staking duration", color = Color.Gray, fontSize = 12.sp)
                Text(text = "$estDurationMonths months (~${estDurationMonths * 180} epochs)", color = Color.White, fontSize = 12.sp)
              }

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Interest reward earned", color = Color.Gray, fontSize = 12.sp)
                Text(
                  text = "+${DaoMath.shannonsToCkb(projectedReward)} CKB",
                  color = Color(0xFF00CC99),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Projected value", color = Color.Gray, fontSize = 12.sp)
                val finalProj = DaoMath.ckbToShannons(initialAmountDouble).add(projectedReward)
                Text(
                  text = "${DaoMath.shannonsToCkb(finalProj)} CKB",
                  color = Color(0xFF8E2DE2),
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold
                )
              }

              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                  .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Info, contentDescription = "Note", tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Note: Realized yield may vary. Compounding is automatic. 102 CKB storage cell base capacity subtraction is factored in for accurate net return forecasts.",
                  color = Color.Gray,
                  fontSize = 10.sp,
                  lineHeight = 12.sp,
                  modifier = Modifier.weight(1f)
                )
              }
            }
          }
        }
      }
    }
  }
}
