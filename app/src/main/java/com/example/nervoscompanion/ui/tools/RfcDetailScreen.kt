package com.example.nervoscompanion.ui.tools

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation3.runtime.NavKey
import com.example.nervoscompanion.RfcDetail
import com.example.nervoscompanion.data.MarkdownParser
import com.example.nervoscompanion.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RfcDetailScreen(rfcNumber: String, onNavigate: (NavKey) -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val settingsStore = remember { SettingsStore(context) }
  val coroutineScope = rememberCoroutineScope()

  val rfcPath = settingsStore.localRfcPath

  // States
  var htmlContent by remember { mutableStateOf<String?>(null) }
  var rfcTitle by remember { mutableStateOf("RFC $rfcNumber") }
  var rfcFolderUri by remember { mutableStateOf<String?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(rfcNumber, rfcPath) {
    isLoading = true
    errorMsg = null
    coroutineScope.launch(Dispatchers.IO) {
      try {
        val baseDir = File(rfcPath)
        val rfcsDir = File(baseDir, "rfcs")
        if (!rfcsDir.exists() || !rfcsDir.isDirectory) {
          throw Exception("RFC folder directory not found at $rfcPath")
        }

        // Find subfolder matching rfcNumber
        val matchedFolder = rfcsDir.listFiles { file ->
          file.isDirectory && (file.name == rfcNumber || file.name.startsWith("$rfcNumber-"))
        }?.firstOrNull()

        if (matchedFolder == null) {
          throw Exception("RFC $rfcNumber folder not found in $rfcPath")
        }

        val mdFile = File(matchedFolder, "${matchedFolder.name}.md")
        if (!mdFile.exists() || !mdFile.isFile) {
          throw Exception("Markdown file not found inside ${matchedFolder.name}")
        }

        val metadata = MarkdownParser.parseFrontmatterAndTitle(mdFile)
        val markdown = mdFile.readText()
        val html = MarkdownParser.toHtml(markdown, metadata)

        withContext(Dispatchers.Main) {
          rfcTitle = metadata.title
          htmlContent = html
          rfcFolderUri = "file://" + matchedFolder.absolutePath + "/"
          isLoading = false
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          errorMsg = e.localizedMessage ?: "Error loading RFC"
          isLoading = false
        }
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("RFC $rfcNumber", fontWeight = FontWeight.Bold)
            Text(
              text = rfcTitle,
              style = MaterialTheme.typography.bodySmall,
              color = Color.LightGray,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = { onNavigate(com.example.nervoscompanion.RfcViewer) }) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color(0xFF070B0E),
          titleContentColor = Color.White,
          navigationIconContentColor = Color.White
        )
      )
    }
  ) { paddingValues ->
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(Color(0xFF0B0F13))
        .padding(paddingValues)
    ) {
      if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = Color(0xFF38EF7D))
        }
      } else if (errorMsg != null) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Text("Failed to load specification", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
          Spacer(modifier = Modifier.height(8.dp))
          Text(errorMsg ?: "", color = Color.Gray, textAlign = TextAlign.Center)
        }
      } else {
        AndroidView(
          factory = { ctx ->
            WebView(ctx).apply {
              webViewClient = object : WebViewClient() {
                
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                  return handleUrl(url)
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                  val url = request?.url?.toString()
                  return handleUrl(url)
                }

                private fun handleUrl(url: String?): Boolean {
                  if (url == null) return false
                  
                  // Regex matchers for RFC numbers (e.g. rfcs/0024 or rfcs-0024 or /0024-name)
                  val rfcMatch = "/rfcs/(\\d{4})".toRegex().find(url) ?:
                                 "rfcs-(\\d{4})".toRegex().find(url) ?:
                                 "/(\\d{4})-[a-zA-Z]".toRegex().find(url)
                  
                  if (rfcMatch != null) {
                    val number = rfcMatch.groupValues[1]
                    onNavigate(RfcDetail(rfcNumber = number))
                    return true
                  }

                  // External link opening
                  try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                  } catch (e: Exception) {
                    e.printStackTrace()
                  }
                  return true
                }
              }

              settings.apply {
                javaScriptEnabled = false
                domStorageEnabled = true
                builtInZoomControls = true
                displayZoomControls = false
                textZoom = 100
              }
            }
          },
          update = { webView ->
            htmlContent?.let { html ->
              webView.loadDataWithBaseURL(
                rfcFolderUri,
                html,
                "text/html",
                "UTF-8",
                null
              )
            }
          },
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}
