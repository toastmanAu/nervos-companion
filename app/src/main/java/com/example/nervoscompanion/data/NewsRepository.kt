package com.example.nervoscompanion.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.nervoscompanion.data.cache.NewsDao
import com.example.nervoscompanion.data.cache.NewsEntity

data class NewsItem(
  val id: String,
  val source: String,
  val title: String,
  val summary: String,
  val url: String,
  val publishedAt: Long,
  val tags: List<String>
)

fun NewsEntity.toDomain() = NewsItem(
  id = id,
  source = source,
  title = title,
  summary = summary,
  url = url,
  publishedAt = publishedAt,
  tags = tags
)

fun NewsItem.toEntity() = NewsEntity(
  id = id,
  source = source,
  title = title,
  summary = summary,
  url = url,
  publishedAt = publishedAt,
  tags = tags
)

class NewsRepository(
  private val newsDao: NewsDao,
  private val settingsStore: SettingsStore
) {

  suspend fun fetchAllNews(): List<NewsItem> = withContext(Dispatchers.IO) {
    // 1. Load cached news first (if any)
    val cachedEntities = try {
      newsDao.getAllNews()
    } catch (e: Exception) {
      emptyList()
    }

    val items = mutableListOf<NewsItem>()

    // 1. Fetch Nervos Talk JSON feed
    try {
      val url = URL("https://talk.nervos.org/latest.json")
      val conn = url.openConnection() as HttpURLConnection
      conn.connectTimeout = 5000
      conn.readTimeout = 5000
      if (conn.responseCode == HttpURLConnection.HTTP_OK) {
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val topicList = JSONObject(text).getJSONObject("topic_list").getJSONArray("topics")
        for (i in 0 until topicList.length()) {
          val topic = topicList.getJSONObject(i)
          val id = topic.getInt("id").toString()
          val title = topic.getString("title")
          val slug = topic.optString("slug", "topic")
          val dateStr = topic.getString("created_at")
          // parse ISO date
          val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
          val date = format.parse(dateStr) ?: java.util.Date()
          
          items.add(
            NewsItem(
              id = "nervostalk-$id",
              source = "Nervos Talk",
              title = title,
              summary = "Discourse thread with ${topic.getInt("posts_count")} posts and ${topic.getInt("views")} views.",
              url = "https://talk.nervos.org/t/$slug/$id",
              publishedAt = date.time,
              tags = listOf("forum", "community")
            )
          )
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    // 2. Fetch Curated Links and X Mirror Feeds from configurable Repository
    val baseRepoUrl = settingsStore.configBaseUrl
    val targetUrl = baseRepoUrl.trim().removeSuffix("/") + "/featured_links.json"
    
    // Fetch featured links
    try {
      val url = URL(targetUrl) // We can mock this if it fails
      val conn = url.openConnection() as HttpURLConnection
      conn.connectTimeout = 3000
      conn.readTimeout = 3000
      if (conn.responseCode == HttpURLConnection.HTTP_OK) {
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val array = JSONArray(text)
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val tagsList = mutableListOf<String>()
          val tagsArr = obj.optJSONArray("tags")
          if (tagsArr != null) {
            for (j in 0 until tagsArr.length()) {
              tagsList.add(tagsArr.getString(j))
            }
          }
          items.add(
            NewsItem(
              id = "curated-$i",
              source = obj.optString("source", "GitHub Curated"),
              title = obj.getString("title"),
              summary = obj.optString("summary", ""),
              url = obj.getString("url"),
              publishedAt = obj.optLong("publishedAt", System.currentTimeMillis() - i * 3600000),
              tags = tagsList
            )
          )
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    // If both fetches failed and we have cache, return cache
    if (items.isEmpty()) {
      if (cachedEntities.isNotEmpty()) {
        return@withContext cachedEntities.map { it.toDomain() }
      } else {
        // Fallback mock items
        val mock = getMockFeaturedLinks()
        try {
          newsDao.insertNews(mock.map { it.toEntity() })
        } catch (e: Exception) {
          e.printStackTrace()
        }
        return@withContext mock
      }
    }

    // Sort descending by time
    items.sortByDescending { it.publishedAt }

    // Update cache
    try {
      newsDao.deleteAllNews()
      newsDao.insertNews(items.map { it.toEntity() })
    } catch (e: Exception) {
      e.printStackTrace()
    }

    items
  }

  private fun getMockFeaturedLinks(): List<NewsItem> {
    return listOf(
      NewsItem(
        id = "mock-1",
        source = "GitHub Curated",
        title = "Fiber Network Update: Next Gen Payment Channels",
        summary = "Read about the lighting network extension spec on CKB, detailing channel opens and state updates.",
        url = "https://github.com/nervosnetwork/fiber",
        publishedAt = System.currentTimeMillis() - 86400000,
        tags = listOf("fiber", "layer2")
      ),
      NewsItem(
        id = "mock-2",
        source = "X Mirror",
        title = "RGB++ Protocol goes Live on Mainnet",
        summary = "RGB++ allows Bitcoin UTXOs to directly bind to CKB cells, enabling BTC smart contracts.",
        url = "https://github.com/ckb-cell/RGB-plus-plus-RFC",
        publishedAt = System.currentTimeMillis() - 172800000,
        tags = listOf("rgb++", "bitcoin")
      )
    )
  }
}
