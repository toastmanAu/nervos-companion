package com.example.nervoscompanion.data.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nervoscompanion.data.RpcClient
import com.example.nervoscompanion.data.SettingsStore
import com.example.nervoscompanion.data.cache.AppDatabase
import com.example.nervoscompanion.data.cache.ChainStatsEntity
import org.json.JSONObject
import com.example.nervoscompanion.data.CkbAddressParser
import com.example.nervoscompanion.ui.tools.DaoMath
import java.net.HttpURLConnection
import java.net.URL
import com.example.nervoscompanion.ui.widget.NervosCompanionWidgetProvider

class BlockchainWorker(
  appContext: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val context = applicationContext
    val settingsStore = SettingsStore(context)

    // Verify background sync is enabled
    if (!settingsStore.isBackgroundSyncEnabled) {
      return Result.success()
    }

    val db = AppDatabase.getDatabase(context)
    val chainStatsDao = db.chainStatsDao()

    try {
      // 1. Fetch latest stats from RPC
      val client = RpcClient(settingsStore.rpcUrl)
      val responseNum = client.call("get_tip_block_number")
      val blockHex = JSONObject(responseNum).getString("result")
      val blockNumber = blockHex.substring(2).toLong(16)

      val responseHeader = client.call("get_tip_header")
      val headerResult = JSONObject(responseHeader).getJSONObject("result")
      val epochHex = headerResult.getString("epoch")
      val epochVal = epochHex.substring(2).toLong(16)
      val epNum = epochVal and 0xFFFFFFL
      val epIdx = (epochVal shr 24) and 0xFFFFL
      val epLen = (epochVal shr 40) and 0xFFFFL
      val epochNumber = epNum
      val epochProgress = "$epIdx / $epLen"

      var nodeVersion = "Public Node"
      try {
        val responseNode = client.call("local_node_info")
        nodeVersion = JSONObject(responseNode).getJSONObject("result").getString("version")
      } catch (e: Exception) {
        e.printStackTrace()
      }

      // 2. Fetch CoinGecko prices
      var ckbPrice: Double? = null
      var ckbChange: Double? = null
      var ckbMarketCap: Double? = null
      var ckbVolume: Double? = null

      try {
        val priceUrl = "https://api.coingecko.com/api/v3/simple/price?ids=nervos-network&vs_currencies=usd&include_24hr_change=true&include_market_cap=true&include_24hr_vol=true"
        val conn = URL(priceUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
          val text = conn.inputStream.bufferedReader().use { it.readText() }
          val data = JSONObject(text).getJSONObject("nervos-network")
          ckbPrice = data.getDouble("usd")
          ckbChange = data.getDouble("usd_24h_change")
          ckbMarketCap = data.getDouble("usd_market_cap")
          ckbVolume = data.getDouble("usd_24h_vol")
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }

      // Get current cached stats
      val cached = chainStatsDao.getStats()
      val previousEpoch = cached?.epochNumber
      val previousBlock = cached?.blockNumber
      val threshold = settingsStore.notificationBlockThreshold

      // 3. Save new stats to database cache
      val newStats = ChainStatsEntity(
        id = 1,
        blockNumber = blockNumber,
        epochNumber = epochNumber,
        epochProgress = epochProgress,
        nodeVersion = nodeVersion,
        ckbPrice = ckbPrice ?: cached?.ckbPrice,
        ckbChange = ckbChange ?: cached?.ckbChange,
        ckbMarketCap = ckbMarketCap ?: cached?.ckbMarketCap,
        ckbVolume = ckbVolume ?: cached?.ckbVolume,
        priceHistory = cached?.priceHistory,
        lastUpdated = System.currentTimeMillis()
      )
      chainStatsDao.insertStats(newStats)

      // 4. Send notification if conditions are met
      if (previousEpoch != null && epochNumber > previousEpoch) {
        // Trigger Epoch transition notification
        sendNotification(
          context,
          "Nervos Network Epoch Event",
          "Epoch #$epochNumber has started! Tip Block: #$blockNumber",
          101
        )
      } else if (previousBlock != null && blockNumber / threshold > previousBlock / threshold) {
        // Trigger block height milestone notification on boundary crossing
        val milestone = (blockNumber / threshold) * threshold
        sendNotification(
          context,
          "Block Height Milestone",
          "Mined block milestone reached! Current block: #$blockNumber (crossed #$milestone)",
          102
        )
      }

      // 5. Check Nervos Talk Forum
      if (settingsStore.isForumNotificationsEnabled) {
        try {
          val url = URL("https://talk.nervos.org/latest.json")
          val conn = url.openConnection() as HttpURLConnection
          conn.connectTimeout = 10000
          conn.readTimeout = 10000
          conn.setRequestProperty("User-Agent", "NervosCompanionApp")
          if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val topicList = JSONObject(text).getJSONObject("topic_list").getJSONArray("topics")
            var maxId = settingsStore.lastSeenForumPostId
            var newPostTitle: String? = null
            
            for (i in 0 until topicList.length()) {
              val topic = topicList.getJSONObject(i)
              val id = topic.getInt("id")
              val title = topic.getString("title")
              if (id > maxId) {
                maxId = id
                if (settingsStore.lastSeenForumPostId > 0) {
                  newPostTitle = title
                }
              }
            }
            
            if (settingsStore.lastSeenForumPostId == 0) {
              settingsStore.lastSeenForumPostId = maxId
            } else if (newPostTitle != null) {
              settingsStore.lastSeenForumPostId = maxId
              sendNotification(
                context = context,
                title = "New Forum Post",
                message = newPostTitle,
                notificationId = 201,
                channelId = "nervos_forum_alerts",
                channelName = "Forum Alerts",
                channelDesc = "Notifications regarding new posts on talk.nervos.org."
              )
            }
          }
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }

      // 6. Check GitHub Software Releases
      if (settingsStore.isReleaseNotificationsEnabled) {
        val reposToCheck = mutableListOf<Triple<String, String, String>>() // Name, Owner, Repo
        
        // Add hardcoded defaults to ensure coverage of CKB and Fiber
        reposToCheck.add(Triple("Nervos CKB", "nervosnetwork", "ckb"))
        reposToCheck.add(Triple("Fiber Network", "nervosnetwork", "fiber"))

        // Add from ecosystem apps in DB if user enabled alerts for them
        try {
          val apps = db.ecosystemAppDao().getAllApps()
          val alertApps = settingsStore.getReleaseAlertApps()
          for (app in apps) {
            val githubUrl = app.githubUrl
            if (alertApps.contains(app.name) && !githubUrl.isNullOrEmpty()) {
              val match = Regex("https?://github\\.com/([^/]+)/([^/]+)/?.*").find(githubUrl)
              if (match != null) {
                val owner = match.groupValues[1]
                val repo = match.groupValues[2].removeSuffix(".git").trim()
                // Avoid duplicating default repos
                if (reposToCheck.none { it.second.lowercase() == owner.lowercase() && it.third.lowercase() == repo.lowercase() }) {
                  reposToCheck.add(Triple(app.name, owner, repo))
                }
              }
            }
          }
        } catch (e: Exception) {
          e.printStackTrace()
        }

        // Loop over all collected repos to fetch latest releases
        for (item in reposToCheck) {
          val appName = item.first
          val owner = item.second
          val repo = item.third
          
          try {
            val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "NervosCompanionApp")
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
              val text = conn.inputStream.bufferedReader().use { it.readText() }
              val json = JSONObject(text)
              val tag = json.getString("tag_name")
              val name = json.optString("name", tag)
              val lastTag = settingsStore.getLastSeenReleaseTag(owner, repo)
              
              if (lastTag.isEmpty()) {
                // Initialize tag on first check
                settingsStore.setLastSeenReleaseTag(owner, repo, tag)
              } else if (tag != lastTag) {
                // Save new tag and trigger notification
                settingsStore.setLastSeenReleaseTag(owner, repo, tag)
                sendNotification(
                  context = context,
                  title = "New $appName Release: $name",
                  message = "$appName version $tag is now available on GitHub.",
                  notificationId = (appName.hashCode() and 0xFFFF) + 1000,
                  channelId = "nervos_release_alerts",
                  channelName = "Software Releases",
                  channelDesc = "Notifications regarding new CKB and Fiber software releases."
                )
              }
            }
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
      }

      // 7. Check DAO Staking Maturity alerts
      checkDaoMaturityAlerts(context, settingsStore)
      
      // 8. Update home screen widgets
      NervosCompanionWidgetProvider.triggerUpdate(context)

      return Result.success()
    } catch (e: Exception) {
      e.printStackTrace()
      return Result.retry()
    }
  }

  private fun sendNotification(
    context: Context,
    title: String,
    message: String,
    notificationId: Int,
    channelId: String = "nervos_chain_alerts",
    channelName: String = "Chain Status Alerts",
    channelDesc: String = "Notifications regarding blockchain epoch transitions and block milestones."
  ) {
    // Create channel for API 26+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel = NotificationChannel(channelId, channelName, importance).apply {
        description = channelDesc
      }
      val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }

    // Build notification
    val builder = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(android.R.drawable.stat_notify_sync)
      .setContentTitle(title)
      .setContentText(message)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)

    // Send
    try {
      val notificationManager = NotificationManagerCompat.from(context)
      // Check permission on Android 13+ (checked dynamically, handled by try-catch for SecurityException)
      notificationManager.notify(notificationId, builder.build())
    } catch (e: SecurityException) {
      e.printStackTrace()
    }
  }

  private suspend fun checkDaoMaturityAlerts(context: Context, settingsStore: SettingsStore) {
    val addresses = settingsStore.getTrackedAddresses()
    if (addresses.isEmpty()) return

    val client = RpcClient(settingsStore.rpcUrl)
    val db = AppDatabase.getDatabase(context)
    val daoCellDao = db.daoCellDao()
    
    try {
      val tipHeader = fetchDaoHeader(client, null) ?: return
      val currentEpochFraction = tipHeader.epochFraction
      val notifiedOutpoints = settingsStore.getNotifiedOutpoints()

      for (address in addresses) {
        try {
          val lockScript = CkbAddressParser.parseAddress(address)
          
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

          val responseCells = client.call("get_cells", listOf(searchKey, "asc", "0x64"))
          val resultObj = JSONObject(responseCells).optJSONObject("result") ?: continue
          val objectsArray = resultObj.getJSONArray("objects")

          val cachedCells = mutableListOf<com.example.nervoscompanion.data.cache.CachedDaoCell>()

          for (i in 0 until objectsArray.length()) {
            val obj = objectsArray.getJSONObject(i)
            val outPoint = obj.getJSONObject("out_point")
            val txHash = outPoint.getString("tx_hash")
            val indexHex = outPoint.getString("index")
            val index = indexHex.substring(2).toInt(16)
            
            val output = obj.getJSONObject("output")
            val capacityHex = output.getString("capacity")
            val totalCapacity = capacityHex.substring(2).toBigInteger(16)
            val capacityCkb = totalCapacity.toBigDecimal().movePointLeft(8).toDouble()

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

            val depHeader = fetchDaoHeader(client, depositBlockNumber) ?: continue
            val witHeader = if (withdrawBlockNumber != null) fetchDaoHeader(client, withdrawBlockNumber) else null
            
            val depositEpoch = depHeader.epochFraction
            
            val refAr = witHeader?.ar ?: tipHeader.ar
            val accruedYield = DaoMath.computeDaoAccrual(totalCapacity, occupiedCapacity, depHeader.ar, refAr)

            val remainingEpochs: Double
            val remainingDays: Double
            val progress: Double
            val cycleIndex: Int
            val isMatured: Boolean
            val nextBoundary: Double

            if (status == "deposited") {
              val diff = currentEpochFraction - depositEpoch
              val cycles = if (diff <= 0.0) 1.0 else Math.ceil(diff / 180.0)
              nextBoundary = depositEpoch + 180.0 * cycles
              remainingEpochs = (nextBoundary - currentEpochFraction).coerceAtLeast(0.0)
              remainingDays = remainingEpochs / 6.0
              val startEpoch = nextBoundary - 180.0
              progress = ((currentEpochFraction - startEpoch) / 180.0).coerceIn(0.0, 1.0)
              cycleIndex = cycles.toInt()
              isMatured = false
            } else {
              val withdrawEpoch = witHeader?.epochFraction ?: currentEpochFraction
              val diff = withdrawEpoch - depositEpoch
              val cycles = if (diff <= 0.0) 1.0 else Math.ceil(diff / 180.0)
              nextBoundary = depositEpoch + 180.0 * cycles
              remainingEpochs = (nextBoundary - currentEpochFraction).coerceAtLeast(0.0)
              remainingDays = remainingEpochs / 6.0
              val startEpoch = nextBoundary - 180.0
              progress = ((currentEpochFraction - startEpoch) / 180.0).coerceIn(0.0, 1.0)
              cycleIndex = cycles.toInt()
              isMatured = currentEpochFraction >= nextBoundary
            }

            val cachedCell = com.example.nervoscompanion.data.cache.CachedDaoCell(
              address = address,
              txHash = txHash,
              index = index,
              status = status,
              totalCapacity = totalCapacity.toString(),
              occupiedCapacity = occupiedCapacity.toString(),
              depositBlockNumber = depositBlockNumber,
              withdrawBlockNumber = withdrawBlockNumber,
              blockNumber = blockNumber,
              accruedYield = accruedYield.toString(),
              nextBoundary = nextBoundary,
              remainingEpochs = remainingEpochs,
              remainingDays = remainingDays,
              progress = progress,
              cycleIndex = cycleIndex,
              isMatured = isMatured,
              depositEpochFraction = depositEpoch,
              withdrawEpochFraction = witHeader?.epochFraction,
              lastUpdated = System.currentTimeMillis()
            )
            cachedCells.add(cachedCell)

            if (status == "deposited") {
              if (remainingEpochs in 0.0..1.5) {
                val outpointKey = "${txHash}:${index}:boundary"
                if (!notifiedOutpoints.contains(outpointKey)) {
                  sendNotification(
                    context = context,
                    title = "DAO Rollover Boundary Alert",
                    message = "Staked cell of ${String.format("%.2f", capacityCkb)} CKB is rolling over in ${String.format("%.1f", remainingEpochs * 4.0)} hours! Withdraw now if you want to unlock it.",
                    notificationId = outpointKey.hashCode(),
                    channelId = "nervos_staking_alerts",
                    channelName = "Staking Maturity Alerts",
                    channelDesc = "Notifications regarding Nervos DAO cell lock cycles and unlock maturities."
                  )
                  settingsStore.addNotifiedOutpoint(outpointKey)
                }
              }
            } else {
              if (isMatured) {
                val outpointKey = "${txHash}:${index}:matured"
                if (!notifiedOutpoints.contains(outpointKey)) {
                  sendNotification(
                    context = context,
                    title = "DAO Staked Cell Matured",
                    message = "Your staked cell of ${String.format("%.2f", capacityCkb)} CKB is fully unlocked and ready to withdraw!",
                    notificationId = outpointKey.hashCode(),
                    channelId = "nervos_staking_alerts",
                    channelName = "Staking Alerts",
                    channelDesc = "Notifications regarding Nervos DAO cell lock cycles and unlock maturities."
                  )
                  settingsStore.addNotifiedOutpoint(outpointKey)
                }
              }
            }
          }

          daoCellDao.updateCellsForAddress(address, cachedCells)

        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private data class WorkerDaoHeader(
    val ar: java.math.BigInteger,
    val epochFraction: Double
  )

  private suspend fun fetchDaoHeader(client: RpcClient, blockNumber: Long?): WorkerDaoHeader? {
    return try {
      val response = if (blockNumber == null) {
        client.call("get_tip_header", emptyList())
      } else {
        val numHex = "0x" + blockNumber.toString(16)
        client.call("get_header_by_number", listOf(numHex))
      }
      val resultObj = JSONObject(response).optJSONObject("result") ?: return null
      val epochHex = resultObj.getString("epoch")
      val epochVal = epochHex.substring(2).toLong(16)
      val epochNumber = epochVal and 0xFFFFFFL
      val epochIndex = (epochVal shr 24) and 0xFFFFL
      val epochLength = (epochVal shr 40) and 0xFFFFL
      val epochFraction = epochNumber.toDouble() + if (epochLength > 0L) epochIndex.toDouble() / epochLength.toDouble() else 0.0

      val daoHex = resultObj.getString("dao")
      val daoFields = DaoMath.parseDaoField(daoHex)
      
      WorkerDaoHeader(
        ar = daoFields.accumulatedRate,
        epochFraction = epochFraction
      )
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }
}
