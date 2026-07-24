package com.example.nervoscompanion.ui.news

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nervoscompanion.data.NewsItem
import com.example.nervoscompanion.data.NewsRepository
import com.example.nervoscompanion.data.SettingsStore
import com.example.nervoscompanion.ui.components.TabHeader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewsScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val settingsStore = remember { SettingsStore(context) }
  val newsRepository = remember {
    val db = com.example.nervoscompanion.data.cache.AppDatabase.getDatabase(context)
    NewsRepository(db.newsDao(), settingsStore)
  }
  val coroutineScope = rememberCoroutineScope()

  var newsList by remember { mutableStateOf<List<NewsItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  var searchQuery by remember { mutableStateOf("") }
  var selectedSource by remember { mutableStateOf("All") }

  fun loadNews() {
    isLoading = true
    errorMsg = null
    coroutineScope.launch {
      try {
        newsList = newsRepository.fetchAllNews()
      } catch (e: Exception) {
        errorMsg = "Failed to load news: ${e.localizedMessage}"
      } finally {
        isLoading = false
      }
    }
  }

  LaunchedEffect(Unit) {
    loadNews()
  }

  val filteredNews = newsList.filter { item ->
    val matchesSearch = item.title.contains(searchQuery, ignoreCase = true) ||
        item.summary.contains(searchQuery, ignoreCase = true)
    val matchesSource = selectedSource == "All" || item.source == selectedSource
    matchesSearch && matchesSource
  }

  val sources = listOf("All", "Nervos Talk", "GitHub Curated", "X Mirror")

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      TabHeader(title = "News Feed")
      IconButton(onClick = { loadNews() }, enabled = !isLoading) {
        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
      }
    }

    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search articles...") },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true
    )

    // Sources filter chips
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      sources.forEach { source ->
        val isSelected = selectedSource == source
        ElevatedAssistChip(
          onClick = { selectedSource = source },
          label = { Text(source) },
          colors = if (isSelected) {
            androidx.compose.material3.AssistChipDefaults.elevatedAssistChipColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              labelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
          } else {
            androidx.compose.material3.AssistChipDefaults.elevatedAssistChipColors()
          }
        )
      }
    }

    if (isLoading) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else if (errorMsg != null) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = errorMsg ?: "", color = MaterialTheme.colorScheme.error)
      }
    } else if (filteredNews.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "No articles found.")
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(filteredNews) { item ->
          NewsItemCard(item = item) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
            context.startActivity(intent)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewsItemCard(item: NewsItem, onClick: () -> Unit) {
  val date = Date(item.publishedAt)
  val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
  val formattedDate = sdf.format(date)

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = item.source,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = formattedDate,
          style = MaterialTheme.typography.bodySmall,
          color = Color.Gray
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = item.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )

      if (item.summary.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = item.summary,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      if (item.tags.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          item.tags.forEach { tag ->
            Box(
              modifier = Modifier
                .background(
                  color = MaterialTheme.colorScheme.secondaryContainer,
                  shape = androidx.compose.foundation.shape.AbsoluteRoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "#$tag",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
              )
            }
          }
        }
      }
    }
  }
}
