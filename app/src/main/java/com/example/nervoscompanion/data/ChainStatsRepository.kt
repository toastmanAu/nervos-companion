package com.example.nervoscompanion.data

import com.example.nervoscompanion.data.cache.ChainStatsDao
import com.example.nervoscompanion.data.cache.ChainStatsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ChainStats(
  val blockNumber: Long?,
  val epochNumber: Long?,
  val epochProgress: String?,
  val nodeVersion: String?,
  val ckbPrice: Double?,
  val ckbChange: Double?,
  val ckbMarketCap: Double?,
  val ckbVolume: Double?,
  val priceHistory: String?,
  val lastUpdated: Long
)

fun ChainStatsEntity.toDomain() = ChainStats(
  blockNumber = blockNumber,
  epochNumber = epochNumber,
  epochProgress = epochProgress,
  nodeVersion = nodeVersion,
  ckbPrice = ckbPrice,
  ckbChange = ckbChange,
  ckbMarketCap = ckbMarketCap,
  ckbVolume = ckbVolume,
  priceHistory = priceHistory,
  lastUpdated = lastUpdated
)

class ChainStatsRepository(
  private val chainStatsDao: ChainStatsDao,
  private val settingsStore: SettingsStore
) {

  suspend fun getStats(forceRefresh: Boolean): ChainStats = withContext(Dispatchers.IO) {
    val cached = try {
      chainStatsDao.getStats()
    } catch (e: Exception) {
      null
    }

    // Cache expiration: 60 seconds (60000 ms)
    val cacheExpired = cached == null || (System.currentTimeMillis() - cached.lastUpdated > 60000)

    if (!forceRefresh && !cacheExpired && cached != null) {
      return@withContext cached.toDomain()
    }

    // Refresh data
    var blockNumber: Long? = cached?.blockNumber
    var epochNumber: Long? = cached?.epochNumber
    var epochProgress: String? = cached?.epochProgress
    var nodeVersion: String? = cached?.nodeVersion

    var ckbPrice: Double? = cached?.ckbPrice
    var ckbChange: Double? = cached?.ckbChange
    var ckbMarketCap: Double? = cached?.ckbMarketCap
    var ckbVolume: Double? = cached?.ckbVolume
    var priceHistory: String? = cached?.priceHistory

    var fetchSuccess = false

    try {
      // 1. CKB RPC Stats
      val client = RpcClient(settingsStore.rpcUrl)
      
      try {
        val responseNum = client.call("get_tip_block_number")
        val blockHex = JSONObject(responseNum).getString("result")
        blockNumber = blockHex.substring(2).toLong(16)

        val responseHeader = client.call("get_tip_header")
        val headerResult = JSONObject(responseHeader).getJSONObject("result")
        val epochHex = headerResult.getString("epoch")
        val epochVal = epochHex.substring(2).toLong(16)
        val epNum = epochVal and 0xFFFFFFL
        val epIdx = (epochVal shr 24) and 0xFFFFL
        val epLen = (epochVal shr 40) and 0xFFFFL
        epochNumber = epNum
        epochProgress = "$epIdx / $epLen"
        fetchSuccess = true
      } catch (e: Exception) {
        e.printStackTrace()
      }

      try {
        val responseNode = client.call("local_node_info")
        nodeVersion = JSONObject(responseNode).getJSONObject("result").getString("version")
      } catch (e: Exception) {
        nodeVersion = "Public Node"
      }

      // 2. CoinGecko Price Details
      try {
        val priceUrl = "https://api.coingecko.com/api/v3/simple/price?ids=nervos-network&vs_currencies=usd&include_24hr_change=true&include_market_cap=true&include_24hr_vol=true"
        val conn = URL(priceUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        conn.setRequestProperty("Accept", "application/json")
        
        val code = conn.responseCode
        if (code == HttpURLConnection.HTTP_OK) {
          val text = conn.inputStream.bufferedReader().use { it.readText() }
          val data = JSONObject(text).getJSONObject("nervos-network")
          ckbPrice = data.getDouble("usd")
          ckbChange = data.getDouble("usd_24h_change")
          ckbMarketCap = data.getDouble("usd_market_cap")
          ckbVolume = data.getDouble("usd_24h_vol")
          fetchSuccess = true
        } else {
          System.err.println("CoinGecko simple price API failed with code: $code")
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }

      // 3. CoinGecko Price History (7 days)
      try {
        val historyUrl = "https://api.coingecko.com/api/v3/coins/nervos-network/market_chart?vs_currency=usd&days=7"
        val conn = URL(historyUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        conn.setRequestProperty("Accept", "application/json")
        
        val code = conn.responseCode
        if (code == HttpURLConnection.HTTP_OK) {
          val text = conn.inputStream.bufferedReader().use { it.readText() }
          val pricesArray = JSONObject(text).getJSONArray("prices")
          val priceHistoryPoints = mutableListOf<String>()
          // 7 days of hourly points is ~168 points. Downsample by taking every 2nd point to have ~84 points.
          for (i in 0 until pricesArray.length() step 2) {
            val point = pricesArray.getJSONArray(i)
            val price = point.getDouble(1)
            priceHistoryPoints.add(price.toString())
          }
          priceHistory = priceHistoryPoints.joinToString(",")
          fetchSuccess = true
        } else {
          System.err.println("CoinGecko price history API failed with code: $code")
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    if (!fetchSuccess && cached != null) {
      // Return cache if fetch failed completely
      return@withContext cached.toDomain()
    }

    val updatedStats = ChainStatsEntity(
      id = 1,
      blockNumber = blockNumber,
      epochNumber = epochNumber,
      epochProgress = epochProgress,
      nodeVersion = nodeVersion,
      ckbPrice = ckbPrice,
      ckbChange = ckbChange,
      ckbMarketCap = ckbMarketCap,
      ckbVolume = ckbVolume,
      priceHistory = priceHistory,
      lastUpdated = System.currentTimeMillis()
    )

    try {
      chainStatsDao.insertStats(updatedStats)
    } catch (e: Exception) {
      e.printStackTrace()
    }

    updatedStats.toDomain()
  }
}
