package com.example.nervoscompanion.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nervoscompanion.data.RpcClient
import com.example.nervoscompanion.data.SettingsStore
import com.example.nervoscompanion.theme.currentTheme
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class ConsoleLog(
  val command: String,
  val result: String,
  val isError: Boolean = false
)

data class RpcMethodSpec(
  val name: String,
  val category: String, // Chain, Net, Pool, Stats, Indexer
  val paramsSignature: String,
  val paramPlaceholder: String,
  val description: String
)

val rpcMethodsList = listOf(
  RpcMethodSpec(
    name = "get_tip_block_number",
    category = "Chain",
    paramsSignature = "()",
    paramPlaceholder = "",
    description = "Returns the block number of the tip block in the longest chain."
  ),
  RpcMethodSpec(
    name = "get_tip_header",
    category = "Chain",
    paramsSignature = "()",
    paramPlaceholder = "",
    description = "Returns the header of the tip block in the longest chain."
  ),
  RpcMethodSpec(
    name = "get_current_epoch",
    category = "Chain",
    paramsSignature = "()",
    paramPlaceholder = "",
    description = "Returns the current epoch information."
  ),
  RpcMethodSpec(
    name = "get_epoch_by_number",
    category = "Chain",
    paramsSignature = "(epoch_number)",
    paramPlaceholder = "0",
    description = "Returns the epoch information by epoch number."
  ),
  RpcMethodSpec(
    name = "get_block",
    category = "Chain",
    paramsSignature = "(block_hash, verbosity?)",
    paramPlaceholder = "\"0xa5f5c8511677c776077559e86c02ef902e4d0d0f5e1e24cc8ba0aee3f9606000\"",
    description = "Returns the block content matching the given block hash."
  ),
  RpcMethodSpec(
    name = "get_block_by_number",
    category = "Chain",
    paramsSignature = "(block_number, verbosity?)",
    paramPlaceholder = "100",
    description = "Returns the block content matching the given block number."
  ),
  RpcMethodSpec(
    name = "get_block_hash",
    category = "Chain",
    paramsSignature = "(block_number)",
    paramPlaceholder = "100",
    description = "Returns the block hash matching the given block number."
  ),
  RpcMethodSpec(
    name = "get_header",
    category = "Chain",
    paramsSignature = "(block_hash)",
    paramPlaceholder = "\"0xa5f5c8511677c776077559e86c02ef902e4d0d0f5e1e24cc8ba0aee3f9606000\"",
    description = "Returns the block header matching the given block hash."
  ),
  RpcMethodSpec(
    name = "get_header_by_number",
    category = "Chain",
    paramsSignature = "(block_number)",
    paramPlaceholder = "100",
    description = "Returns the block header matching the given block number."
  ),
  RpcMethodSpec(
    name = "get_transaction",
    category = "Chain",
    paramsSignature = "(tx_hash)",
    paramPlaceholder = "\"0xa0b10639e0895502b5688a6be8cf69460d76541bfa4821629d86d62ba0aae3f9600\"",
    description = "Returns transaction detail information and execution status matching the hash."
  ),
  RpcMethodSpec(
    name = "get_live_cell",
    category = "Chain",
    paramsSignature = "(out_point, with_data)",
    paramPlaceholder = "{\"tx_hash\":\"0xa0b10639e0895502b5688a6be8cf69460d76541bfa4821629d86d62ba0aae3f9600\",\"index\":\"0x0\"} true",
    description = "Returns live cell content and status. Out-point must include transaction hash and index."
  ),
  RpcMethodSpec(
    name = "get_block_economic_state",
    category = "Chain",
    paramsSignature = "(block_hash)",
    paramPlaceholder = "\"0xa5f5c8511677c776077559e86c02ef902e4d0d0f5e1e24cc8ba0aee3f9606000\"",
    description = "Returns economic states for a block, such as transaction fees and block issuance rewards."
  ),
  RpcMethodSpec(
    name = "local_node_info",
    category = "Net",
    paramsSignature = "()",
    paramPlaceholder = "",
    description = "Returns local node info, including peer ID, network addresses, and active protocol versions."
  ),
  RpcMethodSpec(
    name = "get_peers",
    category = "Net",
    paramsSignature = "()",
    paramPlaceholder = "",
    description = "Returns information about connected peer nodes."
  ),
  RpcMethodSpec(
    name = "ping_peers",
    category = "Net",
    paramsSignature = "()",
    paramPlaceholder = "",
    description = "Sends ping request packets to all connected network nodes."
  ),
  RpcMethodSpec(
    name = "get_banned_addresses",
    category = "Net",
    paramsSignature = "()",
    paramPlaceholder = "",
    description = "Returns the list of currently banned IP addresses and subnets."
  ),
  RpcMethodSpec(
    name = "clear_banned_addresses",
    category = "Net",
    paramsSignature = "()",
    paramPlaceholder = "",
    description = "Clears all IP address / subnet bans from the system."
  ),
  RpcMethodSpec(
    name = "set_ban",
    category = "Net",
    paramsSignature = "(address, command, ban_time?, absolute?, reason?)",
    paramPlaceholder = "\"192.168.0.1\" \"insert\" 86400000 false \"Spamming transactions\"",
    description = "Bans or unbans an IP address or subnet from connecting to this node."
  ),
  RpcMethodSpec(
    name = "tx_pool_info",
    category = "Pool",
    paramsSignature = "()",
    paramPlaceholder = "",
    description = "Returns transaction statistics inside the pending, proposed, and orphan queues."
  ),
  RpcMethodSpec(
    name = "get_raw_tx_pool",
    category = "Pool",
    paramsSignature = "()",
    paramPlaceholder = "",
    description = "Returns lists of all transaction hashes queued inside the local pool."
  ),
  RpcMethodSpec(
    name = "clear_tx_pool",
    category = "Pool",
    paramsSignature = "()",
    paramPlaceholder = "",
    description = "Removes all transactions from the pool queue."
  ),
  RpcMethodSpec(
    name = "send_transaction",
    category = "Pool",
    paramsSignature = "(tx_object)",
    paramPlaceholder = "{\"version\":\"0x0\",\"cell_deps\":[],\"header_deps\":[],\"inputs\":[],\"outputs\":[],\"outputs_data\":[],\"witnesses\":[]}",
    description = "Submits a signed raw transaction to the transaction pool."
  ),
  RpcMethodSpec(
    name = "get_blockchain_info",
    category = "Stats",
    paramsSignature = "()",
    paramPlaceholder = "",
    description = "Returns node synchronization info, hash rate, alerts, difficulty, and network state."
  ),
  RpcMethodSpec(
    name = "get_cells",
    category = "Indexer",
    paramsSignature = "(search_key, order, limit, after_cursor?)",
    paramPlaceholder = "{\"script\":{\"code_hash\":\"0x9bd7e06f3ecf4ccc0f7db2ddc42d189957265653624d89047462631522f9ec51\",\"hash_type\":\"type\",\"args\":\"0x\"},\"script_type\":\"lock\"} \"asc\" \"0x64\"",
    description = "Returns indexer cells matching search script criteria."
  ),
  RpcMethodSpec(
    name = "get_transactions",
    category = "Indexer",
    paramsSignature = "(search_key, order, limit, after_cursor?)",
    paramPlaceholder = "{\"script\":{\"code_hash\":\"0x9bd7e06f3ecf4ccc0f7db2ddc42d189957265653624d89047462631522f9ec51\",\"hash_type\":\"type\",\"args\":\"0x\"},\"script_type\":\"lock\"} \"asc\" \"0x64\"",
    description = "Returns indexer transaction history matching search script criteria."
  ),
  RpcMethodSpec(
    name = "get_cells_capacity",
    category = "Indexer",
    paramsSignature = "(search_key)",
    paramPlaceholder = "{\"script\":{\"code_hash\":\"0x9bd7e06f3ecf4ccc0f7db2ddc42d189957265653624d89047462631522f9ec51\",\"hash_type\":\"type\",\"args\":\"0x\"},\"script_type\":\"lock\"}",
    description = "Returns total CKB capacity of all indexer cells matching search script criteria."
  )
)

fun convertDecimalsToHexStrings(value: Any): Any {
  return when (value) {
    is JSONObject -> {
      val newObj = JSONObject()
      val keys = value.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        newObj.put(key, convertDecimalsToHexStrings(value.get(key)))
      }
      newObj
    }
    is JSONArray -> {
      val newArr = JSONArray()
      for (i in 0 until value.length()) {
        newArr.put(convertDecimalsToHexStrings(value.get(i)))
      }
      newArr
    }
    is Int -> "0x" + value.toString(16)
    is Long -> "0x" + value.toString(16)
    is Double -> {
      if (value % 1 == 0.0) {
        "0x" + value.toLong().toString(16)
      } else {
        value
      }
    }
    is String -> {
      val longVal = value.toLongOrNull()
      if (longVal != null) {
        "0x" + longVal.toString(16)
      } else {
        value
      }
    }
    else -> value
  }
}

fun buildAnnotatedTerminalText(text: String, primaryColor: Color): AnnotatedString {
  val builder = AnnotatedString.Builder()
  val regex = Regex("0x[0-9a-fA-F]+")
  var lastIndex = 0
  
  regex.findAll(text).forEach { result ->
    val matchStart = result.range.first
    val matchEnd = result.range.last + 1
    
    if (matchStart > lastIndex) {
      builder.append(text.substring(lastIndex, matchStart))
    }
    
    builder.pushStringAnnotation(tag = "HEX_NUMBER", annotation = result.value)
    builder.pushStyle(
      SpanStyle(
        color = primaryColor,
        fontWeight = FontWeight.Bold,
        textDecoration = TextDecoration.Underline
      )
    )
    builder.append(result.value)
    builder.pop()
    builder.pop()
    
    lastIndex = matchEnd
  }
  
  if (lastIndex < text.length) {
    builder.append(text.substring(lastIndex))
  }
  
  return builder.toAnnotatedString()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConsoleScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val settingsStore = remember { SettingsStore(context) }
  val coroutineScope = rememberCoroutineScope()

  val rpcUrl = settingsStore.rpcUrl
  val theme = currentTheme

  var commandInput by remember { mutableStateOf("") }
  val consoleLogs = remember { mutableStateListOf<ConsoleLog>() }
  var isExecuting by remember { mutableStateOf(false) }
  var isDropdownExpanded by remember { mutableStateOf(false) }
  var isHexConversionEnabled by remember { mutableStateOf(true) }
  val commandHistory = remember { mutableStateListOf<String>() }

  var selectedHexOriginal by remember { mutableStateOf("") }
  var selectedHexDecimal by remember { mutableStateOf("") }
  var showHexDialog by remember { mutableStateOf(false) }

  val listState = rememberLazyListState()

  // Scroll to bottom when new logs are added
  LaunchedEffect(consoleLogs.size) {
    if (consoleLogs.isNotEmpty()) {
      listState.animateScrollToItem(consoleLogs.size - 1)
    }
  }

  // Parse first word of command input to extract method name
  val enteredMethodPrefix = remember(commandInput) {
    commandInput.trim().split(Regex("\\s+")).firstOrNull() ?: ""
  }


  // Exact matching specification for syntax help
  val exactMatch = remember(enteredMethodPrefix) {
    rpcMethodsList.find { it.name.equals(enteredMethodPrefix, ignoreCase = true) }
  }


  fun parseParams(argString: String, convertDecimals: Boolean): List<Any> {
    val trimmedArgs = argString.trim()
    if (trimmedArgs.isEmpty()) return emptyList()

    // Try parsing as JSON array
    try {
      if (trimmedArgs.startsWith("[") && trimmedArgs.endsWith("]")) {
        val arr = JSONArray(trimmedArgs)
        val list = mutableListOf<Any>()
        for (i in 0 until arr.length()) {
          val item = arr.get(i)
          list.add(if (convertDecimals) convertDecimalsToHexStrings(item) else item)
        }
        return list
      }
    } catch (e: Exception) {
      // ignore
    }

    // Try parsing as single JSON object
    try {
      if (trimmedArgs.startsWith("{") && trimmedArgs.endsWith("}")) {
        val obj = JSONObject(trimmedArgs)
        val converted = if (convertDecimals) convertDecimalsToHexStrings(obj) else obj
        return listOf(converted)
      }
    } catch (e: Exception) {
      // ignore
    }

    // Fallback: parse as single parameter or space-split
    return trimmedArgs.split("\\s+".toRegex()).map { arg ->
      val cleaned = arg.removeSurrounding("\"", "\"").removeSurrounding("'", "'")
      if (cleaned.startsWith("0x")) {
        cleaned // hex string
      } else {
        if (cleaned.equals("true", ignoreCase = true)) true
        else if (cleaned.equals("false", ignoreCase = true)) false
        else {
          val longVal = cleaned.toLongOrNull()
          if (longVal != null) {
            if (convertDecimals) {
              "0x" + longVal.toString(16)
            } else {
              longVal
            }
          } else {
            cleaned.toDoubleOrNull() ?: cleaned
          }
        }
      }
    }
  }

  fun executeCommand(cmd: String) {
    if (cmd.trim().isEmpty() || isExecuting) return
    isExecuting = true
    val fullCmd = cmd.trim()

    // Extract method and args
    val parts = fullCmd.split(Regex("\\s+"), 2)
    val method = parts[0]
    val argString = if (parts.size > 1) parts[1] else ""

    // Add to session command history
    if (commandHistory.contains(fullCmd)) {
      commandHistory.remove(fullCmd)
    }
    commandHistory.add(0, fullCmd)
    if (commandHistory.size > 12) {
      commandHistory.removeLastOrNull()
    }

    coroutineScope.launch {
      try {
        val params = parseParams(argString, isHexConversionEnabled)
        val client = RpcClient(rpcUrl)
        val response = client.call(method, params)

        // Try to pretty-print response JSON
        val prettyResult = try {
          val json = JSONObject(response)
          json.toString(2)
        } catch (e: Exception) {
          try {
            val jsonArr = JSONArray(response)
            jsonArr.toString(2)
          } catch (e2: Exception) {
            response
          }
        }

        consoleLogs.add(ConsoleLog(command = fullCmd, result = prettyResult))
      } catch (e: Exception) {
        consoleLogs.add(ConsoleLog(command = fullCmd, result = "Error: ${e.localizedMessage}", isError = true))
      } finally {
        isExecuting = false
      }
    }
    commandInput = ""
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0F1419)) // Slick terminal dark background
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "CKB Interactive Console",
          color = theme.colorScheme.primary,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = "Target node: $rpcUrl",
          color = Color.Gray,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace
        )
      }

      Button(
        onClick = { consoleLogs.clear() },
        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Text("Clear", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
      }
    }

    // Console logs output
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
        .padding(8.dp)
    ) {
      if (consoleLogs.isEmpty()) {
        Text(
          text = "Terminal initialized. Type a CKB JSON-RPC command below, click a template chip, or search with autocomplete.",
          color = Color.DarkGray,
          fontSize = 13.sp,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(8.dp)
        )
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(consoleLogs) { log ->
            Column {
              Text(
                text = "> ${log.command}",
                color = theme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
              )
              Spacer(modifier = Modifier.height(4.dp))
              if (log.isError) {
                Text(
                  text = log.result,
                  color = Color(0xFFE94057),
                  fontSize = 12.sp,
                  fontFamily = FontFamily.Monospace,
                  lineHeight = 16.sp
                )
              } else {
                val annotatedResult = remember(log.result) {
                  buildAnnotatedTerminalText(log.result, theme.colorScheme.primary)
                }
                ClickableText(
                  text = annotatedResult,
                  style = TextStyle(
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                  ),
                  onClick = { offset ->
                    annotatedResult.getStringAnnotations(tag = "HEX_NUMBER", start = offset, end = offset)
                      .firstOrNull()?.let { annotation ->
                        val hexValue = annotation.item
                        val decimalValue = try {
                          val cleanHex = hexValue.removePrefix("0x")
                          val bigInt = java.math.BigInteger(cleanHex, 16)
                          var display = String.format(java.util.Locale.US, "%,d", bigInt)
                          
                          if (bigInt >= java.math.BigInteger.valueOf(100_000L)) {
                            val ckbValue = bigInt.toBigDecimal().movePointLeft(8)
                            display += "\nCKB Capacity: $ckbValue CKB"
                          }
                          
                          val rawDecimalOnly = display.split("\n").first().replace(",", "")
                          if (rawDecimalOnly.length == 13 && rawDecimalOnly.startsWith("1")) {
                            val date = java.util.Date(bigInt.toLong())
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS 'UTC'", java.util.Locale.US).apply {
                              timeZone = java.util.TimeZone.getTimeZone("UTC")
                            }
                            display += "\nTimestamp: ${sdf.format(date)}"
                          }
                          display
                        } catch (e: Exception) {
                          "Invalid hex representation"
                        }
                        
                        selectedHexOriginal = hexValue
                        selectedHexDecimal = decimalValue
                        showHexDialog = true
                      }
                  }
                )
              }
            }
          }
        }
      }
    }

    // Dynamic Parameter Helper Tooltip Card
    if (exactMatch != null) {
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16202C)),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, theme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp)
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Syntax: ${exactMatch.name}${exactMatch.paramsSignature}",
              color = theme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              fontFamily = FontFamily.Monospace
            )
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(theme.colorScheme.primary.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = exactMatch.category,
                color = theme.colorScheme.primary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
          Text(
            text = exactMatch.description,
            color = Color.LightGray,
            fontSize = 11.sp,
            lineHeight = 14.sp
          )
          if (exactMatch.paramPlaceholder.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Parameters format: ${exactMatch.paramPlaceholder}",
              color = Color.Yellow.copy(alpha = 0.8f),
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }
    }

    // Session Command History
    if (commandInput.isEmpty() && commandHistory.isNotEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Recent Commands:",
          color = Color.LightGray,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          commandHistory.forEach { histCmd ->
            SuggestionChip(
              onClick = { commandInput = histCmd },
              label = {
                Text(
                  text = if (histCmd.length > 25) histCmd.take(22) + "..." else histCmd,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 11.sp,
                  color = Color.LightGray,
                  maxLines = 1,
                  softWrap = false
                )
              }
            )
          }
        }
      }
    }

    // Method templates dropdown picker section
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = "Method Templates:",
        color = Color.LightGray,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
      Box(modifier = Modifier.fillMaxWidth()) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1B222A)),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { isDropdownExpanded = true }
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Select CKB RPC Template...",
              color = Color.LightGray,
              fontSize = 13.sp,
              fontFamily = FontFamily.Monospace
            )
            Text(text = "▼", color = theme.colorScheme.primary, fontSize = 10.sp)
          }
        }

        DropdownMenu(
          expanded = isDropdownExpanded,
          onDismissRequest = { isDropdownExpanded = false },
          modifier = Modifier
            .fillMaxWidth(0.9f)
            .background(Color(0xFF1B222A))
            .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
            .heightIn(max = 280.dp)
        ) {
          val categoriesGrouped = rpcMethodsList.groupBy { it.category }
          categoriesGrouped.forEach { (catName, methods) ->
            DropdownMenuItem(
              text = {
                Text(
                  text = "-- $catName Module --",
                  color = Color.Gray,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace
                )
              },
              onClick = {},
              enabled = false
            )
            methods.forEach { spec ->
              DropdownMenuItem(
                text = {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = spec.name,
                      color = theme.colorScheme.primary,
                      fontSize = 12.sp,
                      fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                      text = spec.paramsSignature,
                      color = Color.Gray,
                      fontSize = 10.sp,
                      fontFamily = FontFamily.Monospace
                    )
                  }
                },
                onClick = {
                  commandInput = spec.name + if (spec.paramPlaceholder.isNotEmpty()) " " + spec.paramPlaceholder else ""
                  isDropdownExpanded = false
                }
              )
            }
          }
        }
      }
    }

    // Input Mode Toggle
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Input Mode:",
          color = Color.LightGray,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = if (isHexConversionEnabled) "Translate Decimal to 0x-Hex" else "Raw 0x Hex (Pass-Through)",
          color = theme.colorScheme.primary,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace
        )
      }
      Switch(
        checked = isHexConversionEnabled,
        onCheckedChange = { isHexConversionEnabled = it },
        colors = SwitchDefaults.colors(
          checkedThumbColor = theme.colorScheme.primary,
          checkedTrackColor = theme.colorScheme.primary.copy(alpha = 0.5f)
        )
      )
    }

    // Input layout
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = commandInput,
        onValueChange = { commandInput = it },
        placeholder = { Text("Enter RPC command...", color = Color.Gray, fontFamily = FontFamily.Monospace) },
        modifier = Modifier.weight(1f),
        singleLine = true,
        textStyle = TextStyle(
          color = Color.White,
          fontSize = 13.sp,
          fontFamily = FontFamily.Monospace
        ),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = theme.colorScheme.primary,
          unfocusedBorderColor = Color.DarkGray
        ),
        keyboardOptions = KeyboardOptions(
          imeAction = ImeAction.Send
        ),
        keyboardActions = KeyboardActions(
          onSend = {
            if (commandInput.isNotEmpty()) {
              executeCommand(commandInput)
            }
          }
        )
      )

      Spacer(modifier = Modifier.width(8.dp))

      Button(
        onClick = { executeCommand(commandInput) },
        enabled = !isExecuting && commandInput.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(
          containerColor = theme.colorScheme.primary,
          contentColor = Color.Black
        ),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Run", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
      }
    }

    if (showHexDialog) {
      val clipboardManager = LocalClipboardManager.current
      AlertDialog(
        onDismissRequest = { showHexDialog = false },
        title = {
          Text(
            text = "Hexadecimal Resolver",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = theme.colorScheme.primary,
            fontSize = 16.sp
          )
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
              Text(text = "Original Hex:", color = Color.Gray, fontSize = 11.sp)
              Text(
                text = selectedHexOriginal,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
            HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)
            Column {
              Text(text = "Resolved Value(s):", color = Color.Gray, fontSize = 11.sp)
              Text(
                text = selectedHexDecimal,
                color = theme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val rawDecimal = selectedHexDecimal.split("\n").first().replace(",", "")
              clipboardManager.setText(AnnotatedString(rawDecimal))
              showHexDialog = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = theme.colorScheme.primary, contentColor = Color.Black)
          ) {
            Text("Copy Decimal", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { showHexDialog = false }) {
            Text("Close", color = Color.LightGray, fontFamily = FontFamily.Monospace)
          }
        },
        containerColor = Color(0xFF16202C),
        shape = RoundedCornerShape(12.dp)
      )
    }
  }
}
