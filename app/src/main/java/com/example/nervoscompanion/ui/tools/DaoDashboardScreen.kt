package com.example.nervoscompanion.ui.tools

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.nervoscompanion.data.CkbAddressParser
import com.example.nervoscompanion.data.RpcClient
import com.example.nervoscompanion.data.SettingsStore
import com.example.nervoscompanion.data.cache.AppDatabase
import com.example.nervoscompanion.data.cache.CachedDaoCell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Local structures specifically for this dashboard's rich calculations
data class DaoHeader(
  val blockHash: String,
  val blockNumber: Long,
  val epochNumber: Long,
  val epochIndex: Long,
  val epochLength: Long,
  val timestamp: Long,
  val ar: BigInteger,
  val occupiedCapacity: BigInteger
) {
  val epochFraction: Double
    get() = epochNumber.toDouble() + if (epochLength > 0L) epochIndex.toDouble() / epochLength.toDouble() else 0.0
}

data class LiveDaoCell(
  val txHash: String,
  val index: Int,
  val status: String, // "deposited" | "withdrawing"
  val totalCapacity: BigInteger,
  val occupiedCapacity: BigInteger,
  val depositBlockNumber: Long,
  val withdrawBlockNumber: Long?,
  val blockNumber: Long // the block number of the cell itself (creation/phase1)
)

data class EvaluatedDaoCell(
  val cell: LiveDaoCell,
  val depositHeader: DaoHeader?,
  val withdrawHeader: DaoHeader?,
  val accruedYield: BigInteger,
  val nextBoundary: Double,
  val remainingEpochs: Double,
  val remainingDays: Double,
  val progress: Double,
  val cycleIndex: Int,
  val isMatured: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DaoDashboardScreen(onNavigate: (NavKey) -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val settingsStore = remember { SettingsStore(context) }
  val coroutineScope = rememberCoroutineScope()
  val clipboardManager = LocalClipboardManager.current

  val rpcUrl = settingsStore.rpcUrl
  val rpcNetwork = settingsStore.rpcNetwork

  // Tracked Addresses States
  var trackedAddresses by remember { mutableStateOf(settingsStore.getTrackedAddresses()) }
  var selectedAddress by remember { mutableStateOf(trackedAddresses.firstOrNull() ?: "") }
  var addressInput by remember { mutableStateOf("") }
  var addressInputError by remember { mutableStateOf<String?>(null) }

  // Query States
  var isLoading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }
  var tipHeaderState by remember { mutableStateOf<DaoHeader?>(null) }

  // Reconstruct tipHeader when tipHeaderState is null using lastScannedTipEpochFraction
  val tipHeader = tipHeaderState ?: run {
    val savedEpochFraction = settingsStore.lastScannedTipEpochFraction.toDouble()
    if (savedEpochFraction > 0.0) {
      val epochNumber = savedEpochFraction.toLong()
      val epochIndex = Math.round((savedEpochFraction - epochNumber) * 1000000.0)
      val epochLength = 1000000L
      DaoHeader(
        blockHash = "",
        blockNumber = 0L,
        epochNumber = epochNumber,
        epochIndex = epochIndex,
        epochLength = epochLength,
        timestamp = 0L,
        ar = BigInteger.ZERO,
        occupiedCapacity = BigInteger.ZERO
      )
    } else {
      null
    }
  }

  // Room database and watchlist cells flow
  val db = remember { AppDatabase.getDatabase(context) }
  val cachedCellsFlow = remember(selectedAddress) {
    db.daoCellDao().getCellsForAddress(selectedAddress)
  }
  val cachedCells by cachedCellsFlow.collectAsState(initial = emptyList())

  val daoCells = remember(cachedCells) {
    cachedCells.map { cached ->
      val totalCapacity = cached.totalCapacity.toBigIntegerOrNull() ?: BigInteger.ZERO
      val occupiedCapacity = cached.occupiedCapacity.toBigIntegerOrNull() ?: BigInteger.ZERO
      val accruedYield = cached.accruedYield.toBigIntegerOrNull() ?: BigInteger.ZERO
      
      EvaluatedDaoCell(
        cell = LiveDaoCell(
          txHash = cached.txHash,
          index = cached.index,
          status = cached.status,
          totalCapacity = totalCapacity,
          occupiedCapacity = occupiedCapacity,
          depositBlockNumber = cached.depositBlockNumber,
          withdrawBlockNumber = cached.withdrawBlockNumber,
          blockNumber = cached.blockNumber
        ),
        depositHeader = DaoHeader(
          blockHash = "",
          blockNumber = cached.depositBlockNumber,
          epochNumber = cached.depositEpochFraction.toLong(),
          epochIndex = Math.round((cached.depositEpochFraction - cached.depositEpochFraction.toLong()) * 1000000.0),
          epochLength = 1000000L,
          timestamp = 0L,
          ar = BigInteger.ZERO,
          occupiedCapacity = BigInteger.ZERO
        ),
        withdrawHeader = cached.withdrawEpochFraction?.let { witFraction ->
          DaoHeader(
            blockHash = "",
            blockNumber = cached.withdrawBlockNumber ?: 0L,
            epochNumber = witFraction.toLong(),
            epochIndex = Math.round((witFraction - witFraction.toLong()) * 1000000.0),
            epochLength = 1000000L,
            timestamp = 0L,
            ar = BigInteger.ZERO,
            occupiedCapacity = BigInteger.ZERO
          )
        },
        accruedYield = accruedYield,
        nextBoundary = cached.nextBoundary,
        remainingEpochs = cached.remainingEpochs,
        remainingDays = cached.remainingDays,
        progress = cached.progress,
        cycleIndex = cached.cycleIndex,
        isMatured = cached.isMatured
      )
    }.sortedWith(
      compareBy<EvaluatedDaoCell> { !it.isMatured }
        .thenBy { it.remainingDays }
    )
  }

  // Total Portfolio Stats calculated reactively
  val totalDeposited = remember(daoCells) {
    daoCells.fold(BigInteger.ZERO) { acc, item -> acc.add(item.cell.totalCapacity) }
  }
  val totalYield = remember(daoCells) {
    daoCells.fold(BigInteger.ZERO) { acc, item -> acc.add(item.accruedYield) }
  }
  val totalOccupied = remember(daoCells) {
    daoCells.fold(BigInteger.ZERO) { acc, item -> acc.add(item.cell.occupiedCapacity) }
  }

  // Helper to format values nicely
  fun formatCkb(shannons: BigInteger): String {
    val whole = shannons.divide(BigInteger.valueOf(100_000_000L))
    val frac = shannons.mod(BigInteger.valueOf(100_000_000L))
    val wholeFormatted = try {
      NumberFormat.getNumberInstance(Locale.US).format(whole.toLong())
    } catch (e: Exception) {
      whole.toString()
    }
    val fracStr = frac.toString().padStart(8, '0').take(4) // 4 decimals is neat
    return "$wholeFormatted.$fracStr CKB"
  }

  fun shortenAddress(address: String): String {
    if (address.length <= 16) return address
    return address.take(8) + "..." + address.takeLast(8)
  }

  fun handleAddAddress() {
    val input = addressInput.trim()
    if (input.isEmpty()) {
      addressInputError = "Please enter an address"
      return
    }

    try {
      // Validate address
      val decoded = CkbAddressParser.decode(input)

      // Network verification
      val addressNetwork = decoded.hrp
      val expectedNetworkHrp = if (rpcNetwork == "mainnet") "ckb" else "ckt"
      if (addressNetwork != expectedNetworkHrp) {
        addressInputError = "This is a ${if (addressNetwork == "ckb") "mainnet" else "testnet"} address, but your app is configured for $rpcNetwork"
        return
      }

      // Add to tracked settings
      settingsStore.addTrackedAddress(input)
      trackedAddresses = settingsStore.getTrackedAddresses()
      selectedAddress = input
      addressInput = ""
      addressInputError = null
      Toast.makeText(context, "Address added to watchlist", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
      addressInputError = e.localizedMessage ?: "Invalid CKB Address"
    }
  }

  // Effect to pull data
  // Effect to pull data
  LaunchedEffect(selectedAddress, rpcUrl, rpcNetwork) {
    if (selectedAddress.isEmpty()) {
      tipHeaderState = null
      return@LaunchedEffect
    }

    isLoading = true
    errorMsg = null
    try {
      val client = RpcClient(rpcUrl)

      // Parse lock script
      val lockScript = CkbAddressParser.parseAddress(selectedAddress)

      // Fetch tip
      val tip = fetchDaoHeaderByNumber(client, null)
      tipHeaderState = tip
      settingsStore.lastScannedTipEpochFraction = tip.epochFraction.toFloat()

      // Query get_cells filtering by lock script and DAO type script
      val searchKey = JSONObject().apply {
        put("script", JSONObject().apply {
          put("code_hash", lockScript.codeHash)
          put("hash_type", lockScript.hashType)
          put("args", lockScript.args)
        })
        put("script_type", "lock")
        put("filter", JSONObject().apply {
          put("script", JSONObject().apply {
            put("code_hash", "0x82d76d1b75fe2fd9a27dfbaa65a039221a380d76c926f378d3f81cf3e7e13f2e")
            put("hash_type", "type")
            put("args", "0x")
          })
        })
      }

      val response = client.call("get_cells", listOf(searchKey, "asc", "0x64"))
      val resultObj = JSONObject(response).optJSONObject("result") ?: throw Exception("Invalid get_cells response")
      val objectsArray = resultObj.getJSONArray("objects")

      val tempCells = mutableListOf<LiveDaoCell>()
      val blockNumbers = mutableSetOf<Long>()

      for (i in 0 until objectsArray.length()) {
        val obj = objectsArray.getJSONObject(i)
        val outPoint = obj.getJSONObject("out_point")
        val txHash = outPoint.getString("tx_hash")
        val indexHex = outPoint.getString("index")
        val index = indexHex.substring(2).toInt(16)

        val output = obj.getJSONObject("output")
        val capacityHex = output.getString("capacity")
        val totalCapacity = capacityHex.substring(2).toBigInteger(16)

        val lock = output.getJSONObject("lock")
        val lockArgs = lock.getString("args")

        val typeObj = output.optJSONObject("type")
        val typeArgs = typeObj?.optString("args")
        val typeCodeHash = typeObj?.optString("code_hash")

        val outputData = obj.getString("output_data")
        val blockNumberHex = obj.getString("block_number")
        val blockNumber = blockNumberHex.substring(2).toLong(16)

        val occupiedCapacity = DaoMath.computeCellOccupiedCapacity(
          lockArgs = lockArgs,
          typeCodeHash = typeCodeHash,
          typeArgs = typeArgs,
          outputData = outputData
        )

        val cleanData = if (outputData.startsWith("0x")) outputData.substring(2) else outputData
        val isDeposited = cleanData.all { it == '0' } || cleanData.isEmpty()
        val status = if (isDeposited) "deposited" else "withdrawing"

        val depositBlockNumber = if (isDeposited) {
          blockNumber
        } else {
          DaoMath.parseLittleEndianHex(cleanData).toLong()
        }

        val withdrawBlockNumber = if (isDeposited) null else blockNumber

        blockNumbers.add(depositBlockNumber)
        withdrawBlockNumber?.let { blockNumbers.add(it) }

        tempCells.add(
          LiveDaoCell(
            txHash = txHash,
            index = index,
            status = status,
            totalCapacity = totalCapacity,
            occupiedCapacity = occupiedCapacity,
            depositBlockNumber = depositBlockNumber,
            withdrawBlockNumber = withdrawBlockNumber,
            blockNumber = blockNumber
          )
        )
      }

      // Fetch block headers in parallel
      val headersMap = mutableMapOf<Long, DaoHeader>()
      withContext(Dispatchers.IO) {
        blockNumbers.map { num ->
          async {
            try {
              val header = fetchDaoHeaderByNumber(client, num)
              synchronized(headersMap) {
                headersMap[num] = header
              }
            } catch (e: Exception) {
              e.printStackTrace()
            }
          }
        }.awaitAll()
      }

      // Accrue yield & calculate maturity progress
      val evaluatedList = tempCells.map { cell ->
        val depHeader = headersMap[cell.depositBlockNumber]
        val witHeader = cell.withdrawBlockNumber?.let { headersMap[it] }
        val refHeader = witHeader ?: tip

        val accruedYield = if (depHeader != null) {
          DaoMath.computeDaoAccrual(cell.totalCapacity, cell.occupiedCapacity, depHeader.ar, refHeader.ar)
        } else {
          BigInteger.ZERO
        }

        val depositEpoch = depHeader?.epochFraction ?: 0.0
        val currentEpoch = tip.epochFraction

        if (cell.status == "deposited") {
          val diff = currentEpoch - depositEpoch
          val cycles = if (diff <= 0.0) 1.0 else Math.ceil(diff / 180.0)
          val nextBoundary = depositEpoch + 180.0 * cycles
          val remainingEpochs = (nextBoundary - currentEpoch).coerceAtLeast(0.0)
          val remainingDays = remainingEpochs / 6.0
          val startEpoch = nextBoundary - 180.0
          val progress = ((currentEpoch - startEpoch) / 180.0).coerceIn(0.0, 1.0)
          EvaluatedDaoCell(
            cell = cell,
            depositHeader = depHeader,
            withdrawHeader = null,
            accruedYield = accruedYield,
            nextBoundary = nextBoundary,
            remainingEpochs = remainingEpochs,
            remainingDays = remainingDays,
            progress = progress,
            cycleIndex = cycles.toInt(),
            isMatured = false
          )
        } else {
          val withdrawEpoch = witHeader?.epochFraction ?: currentEpoch
          val diff = withdrawEpoch - depositEpoch
          val cycles = if (diff <= 0.0) 1.0 else Math.ceil(diff / 180.0)
          val maturityEpoch = depositEpoch + 180.0 * cycles
          val remainingEpochs = (maturityEpoch - currentEpoch).coerceAtLeast(0.0)
          val remainingDays = remainingEpochs / 6.0
          val startEpoch = maturityEpoch - 180.0
          val progress = ((currentEpoch - startEpoch) / 180.0).coerceIn(0.0, 1.0)
          val isMatured = currentEpoch >= maturityEpoch
          EvaluatedDaoCell(
            cell = cell,
            depositHeader = depHeader,
            withdrawHeader = witHeader,
            accruedYield = accruedYield,
            nextBoundary = maturityEpoch,
            remainingEpochs = remainingEpochs,
            remainingDays = remainingDays,
            progress = progress,
            cycleIndex = cycles.toInt(),
            isMatured = isMatured
          )
        }
      }

      val cachedList = evaluatedList.map { item ->
        CachedDaoCell(
          address = selectedAddress,
          txHash = item.cell.txHash,
          index = item.cell.index,
          status = item.cell.status,
          totalCapacity = item.cell.totalCapacity.toString(),
          occupiedCapacity = item.cell.occupiedCapacity.toString(),
          depositBlockNumber = item.cell.depositBlockNumber,
          withdrawBlockNumber = item.cell.withdrawBlockNumber,
          blockNumber = item.cell.blockNumber,
          accruedYield = item.accruedYield.toString(),
          nextBoundary = item.nextBoundary,
          remainingEpochs = item.remainingEpochs,
          remainingDays = item.remainingDays,
          progress = item.progress,
          cycleIndex = item.cycleIndex,
          isMatured = item.isMatured,
          depositEpochFraction = item.depositHeader?.epochFraction ?: 0.0,
          withdrawEpochFraction = item.withdrawHeader?.epochFraction,
          lastUpdated = System.currentTimeMillis()
        )
      }
      db.daoCellDao().updateCellsForAddress(selectedAddress, cachedList)

    } catch (e: Exception) {
      errorMsg = e.localizedMessage ?: "Error querying CKB Indexer"
      e.printStackTrace()
    } finally {
      isLoading = false
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF070B0E)) // Stealth rich dark theme
  ) {
    // 1. TOP HEADER BAR
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
          text = "DAO Portfolio Tracker",
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Network: ${rpcNetwork.uppercase()} (${rpcUrl})",
          color = Color.Gray,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 2. WATCH ADDRESS FORM & INPUT
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Track CKB Watch-Only Address",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
          )

          OutlinedTextField(
            value = addressInput,
            onValueChange = {
              addressInput = it
              addressInputError = null
            },
            placeholder = { Text("Paste ckb... or ckt... address", color = Color.Gray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = Color(0xFF11998E),
              unfocusedBorderColor = Color(0xFF1F2E3A),
              focusedContainerColor = Color(0xFF0F161E),
              unfocusedContainerColor = Color(0xFF0F161E)
            ),
            isError = addressInputError != null,
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Text,
              imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
              onDone = { handleAddAddress() }
            ),
            trailingIcon = {
              IconButton(onClick = { handleAddAddress() }) {
                Icon(Icons.Default.Add, contentDescription = "Track Address", tint = Color(0xFF38EF7D))
              }
            }
          )

          if (addressInputError != null) {
            Text(
              text = addressInputError ?: "",
              color = Color.Red,
              fontSize = 12.sp,
              modifier = Modifier.padding(start = 4.dp)
            )
          }
        }
      }

      // 3. WATCHLIST BADGES/CHIPS
      if (trackedAddresses.isNotEmpty()) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "Monitored Watchlist (${trackedAddresses.size})",
              color = Color.Gray,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )

            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(trackedAddresses) { address ->
                val isSelected = address == selectedAddress
                val borderBrush = if (isSelected) {
                  Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D)))
                } else {
                  null
                }

                Box(
                  modifier = Modifier
                    .background(
                      color = if (isSelected) Color(0xFF112521) else Color(0xFF0F161E),
                      shape = RoundedCornerShape(20.dp)
                    )
                    .then(
                      if (borderBrush != null) Modifier.border(1.dp, borderBrush, RoundedCornerShape(20.dp))
                      else Modifier.border(1.dp, Color(0xFF1F2E3A), RoundedCornerShape(20.dp))
                    )
                    .clickable { selectedAddress = address }
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    if (isSelected) {
                      Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color(0xFF38EF7D),
                        modifier = Modifier.size(14.dp)
                      )
                    }
                    Text(
                      text = shortenAddress(address),
                      color = if (isSelected) Color(0xFF38EF7D) else Color.White,
                      fontFamily = FontFamily.Monospace,
                      fontSize = 12.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )

                    IconButton(
                      onClick = {
                        settingsStore.removeTrackedAddress(address)
                        trackedAddresses = settingsStore.getTrackedAddresses()
                        if (selectedAddress == address) {
                          selectedAddress = trackedAddresses.firstOrNull() ?: ""
                        }
                      },
                      modifier = Modifier.size(24.dp)
                    ) {
                      Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 4. MAIN LOADING / EMPTY / ERROR / CONTENT
      if (selectedAddress.isEmpty()) {
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F161E)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1F2E3A))
          ) {
            Column(
              modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Icon(
                Icons.Default.Info,
                contentDescription = "Info",
                tint = Color(0xFF11998E),
                modifier = Modifier.size(48.dp)
              )
              Text(
                text = "Watchlist is empty",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Track a mainnet/testnet CKB address above. The app will auto-discover its locks, parse locked cell capacities, accrue yields, and map epoch cycles.",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
              )
            }
          }
        }
      } else if (isLoading) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(200.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              CircularProgressIndicator(color = Color(0xFF38EF7D))
              Text(
                text = "Scanning CKB Indexer for DAO cells...",
                color = Color.Gray,
                fontSize = 13.sp
              )
            }
          }
        }
      } else if (errorMsg != null) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF210F0F)),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red)
                Text("Failed to sync DAO data", color = Color.White, fontWeight = FontWeight.Bold)
              }
              Text(errorMsg ?: "", color = Color.LightGray, fontSize = 13.sp)
              Button(
                onClick = { selectedAddress = selectedAddress }, // Trigger state reload
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.3f))
              ) {
                Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry Connection", color = Color.White)
              }
            }
          }
        }
      } else {
        // SUMMARY TOTAL PORTFOLIO CARD
        item {
          val totalValue = totalDeposited.add(totalYield)
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
          ) {
            Box(
              modifier = Modifier
                .background(Brush.linearGradient(colors = listOf(Color(0xFF0C2B24), Color(0xFF041512))))
                .border(1.dp, Brush.linearGradient(colors = listOf(Color(0xFF11998E), Color(0xFF38EF7D))), RoundedCornerShape(16.dp))
                .padding(20.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                  Text(
                    text = "Consolidated Portfolio Value",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                  )
                  Text(
                    text = formatCkb(totalValue),
                    color = Color(0xFF38EF7D),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                  )
                }

                HorizontalDivider(color = Color(0xFF112E27), thickness = 1.dp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Column {
                    Text("Total Staked", color = Color.Gray, fontSize = 11.sp)
                    Text(formatCkb(totalDeposited), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text("Accrued Yield", color = Color.Gray, fontSize = 11.sp)
                    Text("+" + formatCkb(totalYield), color = Color(0xFF38EF7D), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                  }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Column {
                    Text("Net Occupied (Storage)", color = Color.Gray, fontSize = 11.sp)
                    Text(formatCkb(totalOccupied), color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text("Active Cells Count", color = Color.Gray, fontSize = 11.sp)
                    Text("${daoCells.size} cells", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                  }
                }
              }
            }
          }
        }

        // DAO CELLS LIST SECTION
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Staked Cells (${daoCells.size})",
              color = Color.White,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
            IconButton(
              onClick = { selectedAddress = selectedAddress }, // reload
              modifier = Modifier.size(24.dp)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Refresh Cells", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
          }
        }

        if (daoCells.isEmpty()) {
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = Color(0xFF0F161E)),
              border = BorderStroke(1.dp, Color(0xFF1F2E3A))
            ) {
              Box(
                modifier = Modifier
                  .padding(24.dp)
                  .fillMaxWidth(),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "No active Nervos DAO deposits found for this address. Make deposits using your wallet to see them here.",
                  color = Color.Gray,
                  fontSize = 13.sp,
                  textAlign = TextAlign.Center,
                  lineHeight = 18.sp
                )
              }
            }
          }
        } else {
          items(daoCells) { item ->
            val totalCellVal = item.cell.totalCapacity.add(item.accruedYield)
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = Color(0xFF0F161E)),
              border = BorderStroke(1.dp, Color(0xFF1F2E3A)),
              shape = RoundedCornerShape(12.dp)
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                // Outpoint / Copy Button
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(
                      Icons.Default.Info,
                      contentDescription = "Cell Outpoint",
                      tint = Color.Gray,
                      modifier = Modifier.size(14.dp)
                    )
                    Text(
                      text = "${shortenAddress(item.cell.txHash)} : ${item.cell.index}",
                      color = Color.LightGray,
                      fontSize = 12.sp,
                      fontFamily = FontFamily.Monospace,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  IconButton(
                    onClick = {
                      clipboardManager.setText(AnnotatedString("${item.cell.txHash}#${item.cell.index}"))
                      Toast.makeText(context, "Outpoint copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(
                      Icons.Default.Share, // acts as copy/share outpoint
                      contentDescription = "Copy Outpoint",
                      tint = Color.Gray,
                      modifier = Modifier.size(12.dp)
                    )
                  }
                }

                // Capacity / Yield display
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text("Total Capacity", color = Color.Gray, fontSize = 11.sp)
                    Text(formatCkb(totalCellVal), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text("Interest Earned", color = Color.Gray, fontSize = 11.sp)
                    Text("+" + formatCkb(item.accruedYield), color = Color(0xFF38EF7D), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                  }
                }

                HorizontalDivider(color = Color(0xFF1F2E3A), thickness = 0.5.dp)

                // State Badge + Epoch Details
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  // Status Badge
                  val (statusLabel, badgeBg, badgeText) = when {
                    item.cell.status == "withdrawing" && item.isMatured -> Triple("Unlockable (Matured)", Color(0xFF112521), Color(0xFF38EF7D))
                    item.cell.status == "withdrawing" -> Triple("Withdrawing (Phase-1)", Color(0xFF2C1F16), Color(0xFFFF9800))
                    else -> Triple("Accruing (Deposited)", Color(0xFF112521), Color(0xFF38EF7D))
                  }

                  Box(
                    modifier = Modifier
                      .background(badgeBg, RoundedCornerShape(4.dp))
                      .padding(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    Text(
                      text = statusLabel,
                      color = badgeText,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold
                    )
                  }

                  // Epoch range
                  val depEpochStr = String.format(Locale.US, "%.1f", item.depositHeader?.epochFraction ?: 0.0)
                  val currentEpochStr = String.format(Locale.US, "%.1f", tipHeader?.epochFraction ?: 0.0)
                  Text(
                    text = "Epoch #$depEpochStr → #$currentEpochStr",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                  )
                }

                // 180-epoch cycle lock countdown
                val cyclePercent = (item.progress * 100).toInt()
                val indicatorColor = when {
                  item.cell.status == "withdrawing" && item.isMatured -> Color(0xFF00F2FE)
                  item.cell.status == "withdrawing" -> Color(0xFFFF9800)
                  else -> Color(0xFF38EF7D)
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    val targetEpochStr = String.format(Locale.US, "%.1f", item.nextBoundary)
                    val descText = if (item.cell.status == "withdrawing") {
                      if (item.isMatured) "Fully unlocked & withdrawable"
                      else "Matures at epoch #$targetEpochStr"
                    } else {
                      "Next cycle boundary: epoch #$targetEpochStr"
                    }
                    Text(
                      text = descText,
                      color = Color.LightGray,
                      fontSize = 11.sp
                    )
                    Text(
                      text = "$cyclePercent%",
                      color = indicatorColor,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }

                  LinearProgressIndicator(
                    progress = { item.progress.toFloat() },
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(6.dp),
                    color = indicatorColor,
                    trackColor = Color(0xFF1F2E3A),
                    strokeCap = StrokeCap.Round
                  )

                  val timeRemainingText = when {
                    item.cell.status == "withdrawing" && item.isMatured -> "Matured (Ready to claim)"
                    else -> {
                      val totalHours = (item.remainingEpochs * 4.0).toInt()
                      val days = totalHours / 24
                      val hours = totalHours % 24
                      if (days > 0) "~$days days, $hours hours remaining" else "~$hours hours remaining"
                    }
                  }

                  Text(
                    text = timeRemainingText,
                    color = if (item.isMatured) Color(0xFF00F2FE) else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = if (item.isMatured) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                  )
                }
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

// RPC parsing helper
private suspend fun fetchDaoHeaderByNumber(client: RpcClient, blockNumber: Long?): DaoHeader {
  val response = if (blockNumber == null) {
    client.call("get_tip_header", emptyList())
  } else {
    val numHex = "0x" + blockNumber.toString(16)
    client.call("get_header_by_number", listOf(numHex))
  }
  val resultObj = JSONObject(response).optJSONObject("result") ?: throw Exception("Block header not found")
  val blockHash = resultObj.getString("hash")

  val numberHex = resultObj.getString("number")
  val parsedBlockNumber = numberHex.substring(2).toLong(16)

  val epochHex = resultObj.getString("epoch")
  val epochVal = epochHex.substring(2).toLongOrNull(16) ?: 0L
  val epochNumber = epochVal and 0xFFFFFFL
  val epochIndex = (epochVal shr 24) and 0xFFFFL
  val epochLength = (epochVal shr 40) and 0xFFFFL

  val timestampHex = resultObj.getString("timestamp")
  val timestamp = timestampHex.substring(2).toLong(16)

  val daoHex = resultObj.getString("dao")
  val daoFields = DaoMath.parseDaoField(daoHex)

  return DaoHeader(
    blockHash = blockHash,
    blockNumber = parsedBlockNumber,
    epochNumber = epochNumber,
    epochIndex = epochIndex,
    epochLength = epochLength,
    timestamp = timestamp,
    ar = daoFields.accumulatedRate,
    occupiedCapacity = daoFields.occupiedCapacity
  )
}
