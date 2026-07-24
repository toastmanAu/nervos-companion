package com.example.nervoscompanion.ui.tools

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.nervoscompanion.RfcDetail
import com.example.nervoscompanion.data.MarkdownParser
import com.example.nervoscompanion.data.RfcMetadata
import com.example.nervoscompanion.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class RfcSearchResult(
  val file: File,
  val metadata: RfcMetadata,
  val snippet: String? = null,
  val keywordIndex: Int = -1,
  val keywordLength: Int = 0
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RfcViewerScreen(onNavigate: (NavKey) -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val settingsStore = remember { SettingsStore(context) }
  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  val rfcPath = settingsStore.localRfcPath

  // States
  var rfcsList by remember { mutableStateOf<List<RfcSearchResult>>(emptyList()) }
  var filteredRfcs by remember { mutableStateOf<List<RfcSearchResult>>(emptyList()) }
  var isScanning by remember { mutableStateOf(false) }
  var isSyncing by remember { mutableStateOf(false) }
  var syncProgressText by remember { mutableStateOf("") }
  var directoryExists by remember { mutableStateOf(false) }

  // Filter & Search states
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }
  var selectedStatus by remember { mutableStateOf("All") }

  val categories = listOf("All", "Standards Track", "Process", "Informational")
  val statuses = listOf("All", "Active", "Draft", "Deprecated")

  // Function to scan local directory
  fun scanRfcs() {
    isScanning = true
    coroutineScope.launch(Dispatchers.IO) {
      val baseDir = File(rfcPath)
      val rfcsDir = File(baseDir, "rfcs")
      
      if (!rfcsDir.exists() || !rfcsDir.isDirectory) {
        withContext(Dispatchers.Main) {
          directoryExists = false
          rfcsList = emptyList()
          isScanning = false
        }
        return@launch
      }

      withContext(Dispatchers.Main) {
        directoryExists = true
      }

      val subDirs = rfcsDir.listFiles { file -> file.isDirectory } ?: emptyArray()
      val tempResults = mutableListOf<RfcSearchResult>()

      for (subDir in subDirs) {
        val mdFile = File(subDir, "${subDir.name}.md")
        if (mdFile.exists() && mdFile.isFile) {
          val metadata = MarkdownParser.parseFrontmatterAndTitle(mdFile)
          tempResults.add(
            RfcSearchResult(
              file = mdFile,
              metadata = metadata
            )
          )
        }
      }

      // Sort by RFC number numerically
      val sortedResults = tempResults.sortedBy { result ->
        result.metadata.number.toIntOrNull() ?: 9999
      }

      withContext(Dispatchers.Main) {
        rfcsList = sortedResults
        isScanning = false
      }
    }
  }

  // Scan on mount or rfcPath changes
  LaunchedEffect(rfcPath) {
    scanRfcs()
  }

  // Apply filters and perform full-text search in background
  LaunchedEffect(searchQuery, selectedCategory, selectedStatus, rfcsList) {
    coroutineScope.launch(Dispatchers.Default) {
      val query = searchQuery.trim().lowercase(Locale.US)
      val isQueryActive = query.isNotEmpty()

      val filtered = rfcsList.filter { item ->
        val catMatches = selectedCategory == "All" || item.metadata.category.equals(selectedCategory, ignoreCase = true)
        val statusMatches = selectedStatus == "All" || item.metadata.status.equals(selectedStatus, ignoreCase = true)
        catMatches && statusMatches
      }.mapNotNull { item ->
        if (!isQueryActive) {
          return@mapNotNull item
        }

        // 1. Search metadata first
        val metaMatch = item.metadata.number.lowercase(Locale.US).contains(query) ||
            item.metadata.title.lowercase(Locale.US).contains(query) ||
            item.metadata.author.lowercase(Locale.US).contains(query) ||
            item.metadata.category.lowercase(Locale.US).contains(query) ||
            item.metadata.status.lowercase(Locale.US).contains(query)

        if (metaMatch) {
          return@mapNotNull item.copy(snippet = null)
        }

        // 2. Perform full-text search inside the file content
        try {
          val content = item.file.readText()
          val contentLower = content.lowercase(Locale.US)
          val idx = contentLower.indexOf(query)
          if (idx != -1) {
            // Extract snippet: 40 chars before and 40 chars after keyword
            val start = (idx - 45).coerceAtLeast(0)
            val end = (idx + query.length + 45).coerceAtMost(content.length)
            
            var snippetText = content.substring(start, end)
              .replace('\n', ' ')
              .replace('\r', ' ')
              .trim()
            
            if (start > 0) snippetText = "...$snippetText"
            if (end < content.length) snippetText = "$snippetText..."

            val snippetLower = snippetText.lowercase(Locale.US)
            val highlightStart = snippetLower.indexOf(query)

            RfcSearchResult(
              file = item.file,
              metadata = item.metadata,
              snippet = snippetText,
              keywordIndex = highlightStart,
              keywordLength = query.length
            )
          } else {
            null
          }
        } catch (e: Exception) {
          null
        }
      }

      withContext(Dispatchers.Main) {
        filteredRfcs = filtered
      }
    }
  }

  // GitHub Sync Logic
  fun runSync() {
    isSyncing = true
    syncProgressText = "Connecting to GitHub..."
    coroutineScope.launch(Dispatchers.IO) {
      try {
        val url = URL("https://api.github.com/repos/nervosnetwork/rfcs/contents/rfcs")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "NervosCompanionApp")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
          val text = conn.inputStream.bufferedReader().use { it.readText() }
          val jsonArr = JSONArray(text)

          val baseDir = File(rfcPath)
          val rfcsDir = File(baseDir, "rfcs")
          if (!rfcsDir.exists()) {
            rfcsDir.mkdirs()
          }

          val count = jsonArr.length()
          for (i in 0 until count) {
            val item = jsonArr.getJSONObject(i)
            val type = item.getString("type")
            if (type == "dir") {
              val dirName = item.getString("name")
              val localSubDir = File(rfcsDir, dirName)
              if (!localSubDir.exists()) {
                localSubDir.mkdirs()
              }

              val rfcFileName = "$dirName.md"
              val localFile = File(localSubDir, rfcFileName)

              withContext(Dispatchers.Main) {
                syncProgressText = "Syncing $dirName (${i + 1}/$count)..."
              }

              // Fetch the file if missing
              if (!localFile.exists()) {
                val dlUrlStr = "https://raw.githubusercontent.com/nervosnetwork/rfcs/master/rfcs/$dirName/$rfcFileName"
                try {
                  val dlUrl = URL(dlUrlStr)
                  val dlConn = dlUrl.openConnection() as HttpURLConnection
                  dlConn.setRequestProperty("User-Agent", "NervosCompanionApp")
                  dlConn.connectTimeout = 5000
                  dlConn.readTimeout = 5000
                  if (dlConn.responseCode == HttpURLConnection.HTTP_OK) {
                    val fileContent = dlConn.inputStream.bufferedReader().use { it.readText() }
                    localFile.writeText(fileContent)
                  }
                } catch (e: Exception) {
                  e.printStackTrace()
                }
              }
            }
          }

          withContext(Dispatchers.Main) {
            Toast.makeText(context, "RFC synchronization completed!", Toast.LENGTH_SHORT).show()
            scanRfcs()
          }
        } else {
          withContext(Dispatchers.Main) {
            Toast.makeText(context, "GitHub API returned code ${conn.responseCode}", Toast.LENGTH_LONG).show()
          }
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          Toast.makeText(context, "Sync failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
      } finally {
        withContext(Dispatchers.Main) {
          isSyncing = false
          syncProgressText = ""
        }
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Offline RFC Reader", fontWeight = FontWeight.Bold, color = Color.White)
            Text(
              text = if (directoryExists) "Folder: $rfcPath" else "Directory Not Found",
              style = MaterialTheme.typography.bodySmall,
              color = Color.Gray,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = { onNavigate(com.example.nervoscompanion.Tools) }) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
        },
        actions = {
          if (directoryExists) {
            IconButton(onClick = { runSync() }, enabled = !isSyncing) {
              if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF38EF7D))
              } else {
                Icon(Icons.Default.Refresh, contentDescription = "Sync Online Updates", tint = Color(0xFF38EF7D))
              }
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF070B0E))
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF070B0E))
        .padding(paddingValues)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      
      // Dynamic Sync Progress banner
      if (isSyncing) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF112521)),
          border = BorderStroke(1.dp, Color(0xFF11998E))
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF38EF7D))
            Text(syncProgressText, color = Color(0xFF38EF7D), fontSize = 13.sp)
          }
        }
      }

      if (!directoryExists) {
        // ONBOARDING GUIDE FOR CONFIGURE PATH
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F161E)),
          border = BorderStroke(1.dp, Color(0xFF1F2E3A))
        ) {
          Column(
            modifier = Modifier
              .padding(24.dp)
              .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Icon(
              Icons.Default.Warning,
              contentDescription = "Folder Missing",
              tint = Color.Red,
              modifier = Modifier.size(56.dp)
            )
            Text(
              text = "RFC Directory Not Found",
              color = Color.White,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "The specified local path is empty or does not exist:\n\"$rfcPath\"\n\nYou can configure a new folder path in settings, or initialize the local repository by syncing it from GitHub.",
              color = Color.Gray,
              fontSize = 14.sp,
              textAlign = TextAlign.Center,
              lineHeight = 20.sp
            )

            Row(
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Button(
                onClick = { onNavigate(com.example.nervoscompanion.Settings) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F2FE))
              ) {
                Text("Go to Settings", color = Color.Black, fontWeight = FontWeight.Bold)
              }

              Button(
                onClick = {
                  // Force initialize directories and sync
                  val baseDir = File(rfcPath)
                  if (!baseDir.exists()) baseDir.mkdirs()
                  directoryExists = true
                  runSync()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38EF7D))
              ) {
                Text("Sync & Init", color = Color.Black, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      } else {
        // SEARCH INPUT BAR
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search title, authors, or keyword content...", color = Color.Gray) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
              }
            }
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF38EF7D),
            unfocusedBorderColor = Color(0xFF1F2E3A),
            focusedContainerColor = Color(0xFF0F161E),
            unfocusedContainerColor = Color(0xFF0F161E)
          ),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )

        // CHIP FILTERS Row 1: Categories
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Filter Category", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
              val isSelected = selectedCategory == category
              FilterChip(
                selected = isSelected,
                onClick = { selectedCategory = category },
                label = { Text(category, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFF112521),
                  selectedLabelColor = Color(0xFF38EF7D),
                  containerColor = Color(0xFF0F161E),
                  labelColor = Color.Gray
                ),
                border = FilterChipDefaults.filterChipBorder(
                  selected = isSelected,
                  enabled = true,
                  borderColor = Color(0xFF1F2E3A),
                  selectedBorderColor = Color(0xFF38EF7D),
                  borderWidth = 1.dp
                )
              )
            }
          }
        }

        // CHIP FILTERS Row 2: Statuses
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Filter Status", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(statuses) { status ->
              val isSelected = selectedStatus == status
              FilterChip(
                selected = isSelected,
                onClick = { selectedStatus = status },
                label = { Text(status, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFF0F2027),
                  selectedLabelColor = Color(0xFF00F2FE),
                  containerColor = Color(0xFF0F161E),
                  labelColor = Color.Gray
                ),
                border = FilterChipDefaults.filterChipBorder(
                  selected = isSelected,
                  enabled = true,
                  borderColor = Color(0xFF1F2E3A),
                  selectedBorderColor = Color(0xFF00F2FE),
                  borderWidth = 1.dp
                )
              )
            }
          }
        }

        // LIST RESULTS SECTION
        Text(
          text = "RFC Specifications (${filteredRfcs.size})",
          color = Color.Gray,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(top = 4.dp)
        )

        if (isScanning) {
          Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
              CircularProgressIndicator(color = Color(0xFF38EF7D))
              Text("Scanning local files...", color = Color.Gray, fontSize = 13.sp)
            }
          }
        } else if (filteredRfcs.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (searchQuery.isNotEmpty()) "No RFCs matched your search query." else "No RFC files found.",
              color = Color.Gray,
              fontSize = 14.sp
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(filteredRfcs) { item ->
              RfcCard(item = item, onClick = {
                onNavigate(RfcDetail(rfcNumber = item.metadata.number))
              })
            }
            item {
              Spacer(modifier = Modifier.height(16.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
fun RfcCard(item: RfcSearchResult, onClick: () -> Unit) {
  val (badgeBg, badgeText) = when (item.metadata.status.lowercase()) {
    "active" -> Pair(Color(0xFF112521), Color(0xFF38EF7D))
    "draft" -> Pair(Color(0xFF2C1F16), Color(0xFFFF9800))
    "deprecated" -> Pair(Color(0xFF2E1212), Color(0xFFEF5350))
    else -> Pair(Color(0xFF0F161E), Color.Gray)
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F161E)),
    border = BorderStroke(1.dp, Color(0xFF1F2E3A)),
    shape = RoundedCornerShape(12.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // RFC Number Label
        Box(
          modifier = Modifier
            .background(Color(0xFF1B2A32), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = "RFC ${item.metadata.number}",
            color = Color(0xFF00F2FE),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
          )
        }

        // Status Badge
        Box(
          modifier = Modifier
            .background(badgeBg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = item.metadata.status.uppercase(),
            color = badgeText,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
          )
        }
      }

      // Title
      Text(
        text = item.metadata.title,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      // KEYWORD SEARCH SNIPPET ROW
      if (item.snippet != null) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF070B0E), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF1F2E3A), RoundedCornerShape(6.dp))
            .padding(10.dp)
        ) {
          val annotatedSnippet = buildAnnotatedString {
            val fullSnippet = item.snippet
            val startIdx = item.keywordIndex
            val length = item.keywordLength
            
            if (startIdx >= 0 && startIdx + length <= fullSnippet.length) {
              append(fullSnippet.substring(0, startIdx))
              withStyle(style = SpanStyle(color = Color.Black, background = Color(0xFF38EF7D), fontWeight = FontWeight.Bold)) {
                append(fullSnippet.substring(startIdx, startIdx + length))
              }
              append(fullSnippet.substring(startIdx + length))
            } else {
              append(fullSnippet)
            }
          }
          Text(
            text = annotatedSnippet,
            color = Color.LightGray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 16.sp
          )
        }
      }

      HorizontalDivider(color = Color(0xFF1F2E3A), thickness = 0.5.dp)

      // Info rows
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Author", color = Color.Gray, fontSize = 10.sp)
          Text(
            text = item.metadata.author,
            color = Color.LightGray,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
          Text("Created", color = Color.Gray, fontSize = 10.sp)
          Text(
            text = item.metadata.created,
            color = Color.LightGray,
            fontSize = 12.sp
          )
        }
      }
    }
  }
}
