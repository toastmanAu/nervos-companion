package com.example.nervoscompanion.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class EcosystemApp(
  val name: String,
  val description: String,
  val websiteUrl: String,
  val twitterUrl: String?,
  val githubUrl: String?,
  val discordUrl: String?,
  val bannerGradientColors: List<Long>
)

class AppsRepository(private val settingsStore: SettingsStore) {

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
      } else {
        apps.addAll(getLocalPresets())
      }
    } catch (e: Exception) {
      e.printStackTrace()
      apps.addAll(getLocalPresets())
    }

    apps.distinctBy { it.name }
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
        obj.optString("supportEmail", "developer@example.com")
      } else {
        "developer@example.com"
      }
    } catch (e: Exception) {
      e.printStackTrace()
      "developer@example.com"
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
        websiteUrl = "https://docs.nervos.org/docs/basics/concepts/nervos-dao",
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
      )
    )
  }
}
