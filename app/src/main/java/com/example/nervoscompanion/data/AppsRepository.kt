package com.example.nervoscompanion.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.example.nervoscompanion.data.cache.EcosystemAppDao
import com.example.nervoscompanion.data.cache.EcosystemAppEntity

data class EcosystemApp(
  val name: String,
  val description: String,
  val websiteUrl: String,
  val twitterUrl: String?,
  val githubUrl: String?,
  val discordUrl: String?,
  val bannerGradientColors: List<Long>
)

fun EcosystemAppEntity.toDomain() = EcosystemApp(
  name = name,
  description = description,
  websiteUrl = websiteUrl,
  twitterUrl = twitterUrl,
  githubUrl = githubUrl,
  discordUrl = discordUrl,
  bannerGradientColors = bannerGradientColors
)

fun EcosystemApp.toEntity() = EcosystemAppEntity(
  name = name,
  description = description,
  websiteUrl = websiteUrl,
  twitterUrl = twitterUrl,
  githubUrl = githubUrl,
  discordUrl = discordUrl,
  bannerGradientColors = bannerGradientColors
)

class AppsRepository(
  private val ecosystemAppDao: EcosystemAppDao,
  private val settingsStore: SettingsStore
) {

  private fun parseHexColor(hex: String): Long {
    return try {
      val cleanHex = hex.trim().removePrefix("#")
      if (cleanHex.length == 6) {
        ("FF$cleanHex").toLong(16)
      } else {
        cleanHex.toLong(16)
      }
    } catch (e: Exception) {
      0xFF00CC99 // Default fallback color (Nervos Green)
    }
  }

  suspend fun fetchApps(): List<EcosystemApp> = withContext(Dispatchers.IO) {
    // 1. Check local cache first
    val cachedEntities = try {
      ecosystemAppDao.getAllApps()
    } catch (e: Exception) {
      emptyList()
    }

    val apps = mutableListOf<EcosystemApp>()
    val baseConfigUrl = settingsStore.configBaseUrl
    val targetUrl = baseConfigUrl.trim().removeSuffix("/") + "/apps.json"

    try {
      val url = URL(targetUrl)
      val conn = url.openConnection() as HttpURLConnection
      conn.connectTimeout = 5000
      conn.readTimeout = 5000
      
      if (conn.responseCode == HttpURLConnection.HTTP_OK) {
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val array = JSONArray(text)
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val colorsList = mutableListOf<Long>()
          val colorsArr = obj.optJSONArray("bannerGradientColors")
          
          if (colorsArr != null) {
            for (j in 0 until colorsArr.length()) {
              val colorVal = colorsArr.get(j)
              if (colorVal is Number) {
                colorsList.add(colorVal.toLong())
              } else if (colorVal is String) {
                colorsList.add(parseHexColor(colorVal))
              }
            }
          }
          
          if (colorsList.isEmpty()) {
            colorsList.addAll(listOf(0xFF0F2027, 0xFF203A43, 0xFF2C5364))
          }

          apps.add(
            EcosystemApp(
              name = obj.getString("name"),
              description = obj.optString("description", ""),
              websiteUrl = obj.getString("websiteUrl"),
              twitterUrl = obj.optString("twitterUrl").takeIf { it.isNotEmpty() },
              githubUrl = obj.optString("githubUrl").takeIf { it.isNotEmpty() },
              discordUrl = obj.optString("discordUrl").takeIf { it.isNotEmpty() },
              bannerGradientColors = colorsList
            )
          )
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    val resultList = apps.distinctBy { it.name }

    if (resultList.isEmpty()) {
      if (cachedEntities.isNotEmpty()) {
        val cachedDomains = cachedEntities.map { it.toDomain() }
        val mergedOffline = (cachedDomains + getLocalPresets()).distinctBy { it.name }
        if (mergedOffline.size > cachedEntities.size) {
          try {
            ecosystemAppDao.deleteAllApps()
            ecosystemAppDao.insertApps(mergedOffline.map { it.toEntity() })
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
        return@withContext mergedOffline
      } else {
        val presets = getLocalPresets()
        try {
          ecosystemAppDao.insertApps(presets.map { it.toEntity() })
        } catch (e: Exception) {
          e.printStackTrace()
        }
        return@withContext presets
      }
    }

    // Merge fetched apps with local presets to ensure new apps are always included,
    // with remote configuration updates taking precedence for duplicates.
    val mergedList = (resultList + getLocalPresets()).distinctBy { it.name }

    // Save to cache
    try {
      ecosystemAppDao.deleteAllApps()
      ecosystemAppDao.insertApps(mergedList.map { it.toEntity() })
    } catch (e: Exception) {
      e.printStackTrace()
    }

    mergedList
  }

  suspend fun fetchSupportEmail(): String = withContext(Dispatchers.IO) {
    val baseConfigUrl = settingsStore.configBaseUrl
    val targetUrl = baseConfigUrl.trim().removeSuffix("/") + "/support.json"
    try {
      val url = URL(targetUrl)
      val conn = url.openConnection() as HttpURLConnection
      conn.connectTimeout = 5000
      conn.readTimeout = 5000
      
      if (conn.responseCode == HttpURLConnection.HTTP_OK) {
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val obj = JSONObject(text)
        obj.optString("supportEmail", "phill@wyltek.com")
      } else {
        "phill@wyltek.com"
      }
    } catch (e: Exception) {
      e.printStackTrace()
      "phill@wyltek.com"
    }
  }

  private fun getLocalPresets(): List<EcosystemApp> {
    return listOf(
      EcosystemApp(
        name = "JoyID Wallet",
        description = "A passwordless passkey wallet on Nervos CKB supporting multi-chain assets.",
        websiteUrl = "https://joy.id",
        twitterUrl = "https://x.com/joy_protocol",
        githubUrl = "https://github.com/nervina-labs/joyid",
        discordUrl = "https://discord.gg/joyid",
        bannerGradientColors = listOf(0xFF8A2387, 0xFFE94057, 0xFFF27121)
      ),
      EcosystemApp(
        name = "Nervos DAO",
        description = "Deposit your CKB into the system smart contract to offset secondary issuance inflation.",
        websiteUrl = "https://explorer.nervos.org/nervosdao",
        twitterUrl = "https://x.com/NervosNetwork",
        githubUrl = "https://github.com/nervosnetwork",
        discordUrl = "https://discord.gg/nervos",
        bannerGradientColors = listOf(0xFF1F1C2C, 0xFF928DAB)
      ),
      EcosystemApp(
        name = "iCKB",
        description = "A liquid staking protocol for the Nervos DAO allowing users to obtain tradeable liquidity.",
        websiteUrl = "https://ickb.org",
        twitterUrl = null,
        githubUrl = "https://github.com/ickb",
        discordUrl = null,
        bannerGradientColors = listOf(0xFF00C6FF, 0xFF0072FF)
      ),
      EcosystemApp(
        name = "Quantum Purse",
        description = "A self-custodial open-source wallet featuring quantum-resistant post-quantum cryptography signature protection.",
        websiteUrl = "https://github.com/QuantumPurse",
        twitterUrl = null,
        githubUrl = "https://github.com/QuantumPurse",
        discordUrl = null,
        bannerGradientColors = listOf(0xFF7F00FF, 0xFFE100FF)
      ),
      EcosystemApp(
        name = "Pocket Node",
        description = "A mobile CKB light client wallet on Android running a localized node on-device for sovereignty.",
        websiteUrl = "https://pocket-node.com",
        twitterUrl = null,
        githubUrl = "https://github.com/pocket-node",
        discordUrl = null,
        bannerGradientColors = listOf(0xFF11998E, 0xFF38EF7D)
      ),
      EcosystemApp(
        name = "mobit.app",
        description = "A decentralized Web3 ecosystem wallet interface supporting asset management and RGB++ Leap.",
        websiteUrl = "https://mobit.app",
        twitterUrl = null,
        githubUrl = null,
        discordUrl = null,
        bannerGradientColors = listOf(0xFF1D976C, 0xFF93F9B9)
      ),
      EcosystemApp(
        name = "NervDAO",
        description = "A user-friendly decentralized staking portal to easily interact with the Nervos DAO smart contract.",
        websiteUrl = "https://nervdao.com",
        twitterUrl = null,
        githubUrl = "https://github.com/nervdao",
        discordUrl = null,
        bannerGradientColors = listOf(0xFF141E30, 0xFF243B55)
      ),
      EcosystemApp(
        name = "Hold'em Bulls Poker",
        description = "A community-driven, provably fair Texas Hold'em poker application built natively on Nervos CKB.",
        websiteUrl = "https://holdembulls.poker",
        twitterUrl = null,
        githubUrl = null,
        discordUrl = null,
        bannerGradientColors = listOf(0xFFED213A, 0xFF93291E)
      ),
      EcosystemApp(
        name = "Rosen Bridge",
        description = "A decentralized, trustless, and multi-layered bridge connecting Nervos CKB with Cardano, Ergo, Bitcoin, and other networks.",
        websiteUrl = "https://rosen.tech",
        twitterUrl = "https://x.com/RosenBridge_erg",
        githubUrl = "https://github.com/rosen-bridge",
        discordUrl = null,
        bannerGradientColors = listOf(0xFF1F1C2C, 0xFF928DAB)
      ),
      EcosystemApp(
        name = "Perun Network",
        description = "State channel protocol implementation on CKB to enable real-time, micro-transaction channels off-chain.",
        websiteUrl = "https://perun.network",
        twitterUrl = "https://x.com/PerunNetwork",
        githubUrl = "https://github.com/hyperledger-labs/perun-ckb-backend",
        discordUrl = null,
        bannerGradientColors = listOf(0xFF11998E, 0xFF38EF7D)
      ),
      EcosystemApp(
        name = "CKBoost",
        description = "A decentralized launchpad and suite of tools providing liquidity and developer resources for the Nervos ecosystem.",
        websiteUrl = "https://github.com/ckboost",
        twitterUrl = null,
        githubUrl = "https://github.com/ckboost",
        discordUrl = null,
        bannerGradientColors = listOf(0xFFF7971E, 0xFFFFD200)
      ),
      EcosystemApp(
        name = "Scryve",
        description = "A decentralized publishing platform and blog archive built on top of Nervos CKB cell storage.",
        websiteUrl = "https://github.com/scryve",
        twitterUrl = null,
        githubUrl = "https://github.com/scryve",
        discordUrl = null,
        bannerGradientColors = listOf(0xFF4A00E0, 0xFF8E2DE2)
      ),
      EcosystemApp(
        name = "CellSwap",
        description = "A working proof-of-concept DOB/CKBFS cell storage demo site. Store and manage cell resources.",
        websiteUrl = "https://cellswap.xyz/",
        twitterUrl = null,
        githubUrl = "https://github.com/cellswap",
        discordUrl = null,
        bannerGradientColors = listOf(0xFF11998E, 0xFF38EF7D)
      ),
      EcosystemApp(
        name = "ByteRent",
        description = "An early proof-of-concept demo of on-chain space/storage rental services for smart contracts.",
        websiteUrl = "https://byterent.xyz/",
        twitterUrl = null,
        githubUrl = "https://github.com/byterent",
        discordUrl = null,
        bannerGradientColors = listOf(0xFF7F00FF, 0xFFE100FF)
      ),
      EcosystemApp(
        name = "Nervos DAO Viewer",
        description = "Embed and browse daoview.org directly in the app. Monitor active DAO deposits, withdrawal epochs, and system statistics.",
        websiteUrl = "https://daoview.org",
        twitterUrl = null,
        githubUrl = null,
        discordUrl = null,
        bannerGradientColors = listOf(0xFF0F2027, 0xFF203A43, 0xFF2C5364)
      ),
      EcosystemApp(
        name = "CellScript",
        description = "A type-safe domain-specific language (DSL) for the Cell model on Nervos CKB, simplifying smart contract development.",
        websiteUrl = "https://github.com/a19q3/CellScript",
        twitterUrl = null,
        githubUrl = "https://github.com/a19q3/CellScript",
        discordUrl = null,
        bannerGradientColors = listOf(0xFF2C3E50, 0xFF000000)
      ),
      EcosystemApp(
        name = "Fiber Storybook",
        description = "An interactive, narrative-driven demo explaining the features and advantages of the CKB Fiber Network.",
        websiteUrl = "https://fiber-storybook-seven.vercel.app/",
        twitterUrl = null,
        githubUrl = "https://github.com/yfeng2824/fiber-storybook",
        discordUrl = null,
        bannerGradientColors = listOf(0xFF11998E, 0xFF38EF7D)
      )
    )
  }

  suspend fun checkForUpdates(): AppUpdate? = withContext(Dispatchers.IO) {
    val baseConfigUrl = settingsStore.configBaseUrl
    val targetUrl = baseConfigUrl.trim().removeSuffix("/") + "/version.json"
    try {
      val url = URL(targetUrl)
      val conn = url.openConnection() as HttpURLConnection
      conn.connectTimeout = 5000
      conn.readTimeout = 5000
      
      if (conn.responseCode == HttpURLConnection.HTTP_OK) {
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val obj = JSONObject(text)
        AppUpdate(
          versionCode = obj.getInt("versionCode"),
          versionName = obj.getString("versionName"),
          downloadUrl = obj.getString("downloadUrl"),
          changelog = obj.optString("changelog", "")
        )
      } else {
        null
      }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }
}

data class AppUpdate(
  val versionCode: Int,
  val versionName: String,
  val downloadUrl: String,
  val changelog: String
)
