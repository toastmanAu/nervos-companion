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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nervoscompanion.data.RpcClient
import com.example.nervoscompanion.data.SettingsStore
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class ConsoleLog(
  val command: String,
  val result: String,
  val isError: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConsoleScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val settingsStore = remember { SettingsStore(context) }
  val coroutineScope = rememberCoroutineScope()

  val rpcUrl = settingsStore.rpcUrl

  var commandInput by remember { mutableStateOf("") }
  val consoleLogs = remember { mutableStateListOf<ConsoleLog>() }
  var isExecuting by remember { mutableStateOf(false) }

  val listState = rememberLazyListState()

  // Scroll to bottom when new logs are added
  LaunchedEffect(consoleLogs.size) {
    if (consoleLogs.isNotEmpty()) {
      listState.animateScrollToItem(consoleLogs.size - 1)
    }
  }

  val knownMethods = listOf(
    "get_tip_block_number",
    "get_tip_header",
    "get_current_epoch",
    "local_node_info",
    "get_blockchain_info",
    "get_block_by_number",
    "get_transaction",
    "get_live_cell"
  )

  val autocompleteSuggestions = if (commandInput.isNotEmpty()) {
    knownMethods.filter { it.startsWith(commandInput, ignoreCase = true) && it != commandInput }
  } else {
    emptyList()
  }

  fun parseParams(argString: String): List<Any> {
    val trimmedArgs = argString.trim()
    if (trimmedArgs.isEmpty()) return emptyList()

    // Try parsing as JSON array
    try {
      if (trimmedArgs.startsWith("[") && trimmedArgs.endsWith("]")) {
        val arr = JSONArray(trimmedArgs)
        val list = mutableListOf<Any>()
        for (i in 0 until arr.length()) {
          list.add(arr.get(i))
        }
        return list
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
        else cleaned.toLongOrNull() ?: cleaned.toDoubleOrNull() ?: cleaned
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

    coroutineScope.launch {
      try {
        val params = parseParams(argString)
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
          color = Color(0xFF00CC99),
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
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
        .padding(8.dp)
    ) {
      if (consoleLogs.isEmpty()) {
        Text(
          text = "Terminal initialized. Type a CKB JSON-RPC command below or click a quick-template chip.",
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
                color = Color(0xFF00CC99),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = log.result,
                color = if (log.isError) Color(0xFFE94057) else Color(0xFFE2E8F0),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
              )
            }
          }
        }
      }
    }

    // Quick Templates
    Text(
      text = "Method Templates:",
      color = Color.LightGray,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      knownMethods.take(5).forEach { method ->
        Box(
          modifier = Modifier
            .background(Color(0xFF203A43), RoundedCornerShape(4.dp))
            .clickable { executeCommand(method) }
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = method,
            color = Color(0xFF00CC99),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    // Autocomplete Suggestions
    if (autocompleteSuggestions.isNotEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        autocompleteSuggestions.take(3).forEach { suggestion ->
          Box(
            modifier = Modifier
              .background(Color(0xFF1F1F1F), RoundedCornerShape(4.dp))
              .clickable { commandInput = suggestion }
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = suggestion,
              color = Color.Yellow,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }
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
        textStyle = androidx.compose.ui.text.TextStyle(
          color = Color.White,
          fontSize = 13.sp,
          fontFamily = FontFamily.Monospace
        ),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = Color(0xFF00CC99),
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
          containerColor = Color(0xFF00CC99),
          contentColor = Color.Black
        ),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Run", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
      }
    }
  }
}
