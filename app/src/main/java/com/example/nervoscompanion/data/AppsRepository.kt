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
        name = ".bit DID",
        description = "Cross-chain Web3 identities (.bit) built natively on Nervos Network.",
        websiteUrl = "https://did.id",
        twitterUrl = "https://x.com/dotbitDID",
        githubUrl = "https://github.com/dotbitDID",
        discordUrl = "https://discord.gg/dotbit",
        bannerGradientColors = listOf(0xFF11998E, 0xFF38EF7D)
      ),
      EcosystemApp(
        name = "Yokai Swap",
        description = "An AMM-based decentralized exchange (DEX) running on Godwoken Layer 2.",
        websiteUrl = "https://yokaiswap.com",
        twitterUrl = "https://x.com/yokaiswap",
        githubUrl = "https://github.com/yokaiswap",
        discordUrl = "https://discord.gg/yokaiswap",
        bannerGradientColors = listOf(0xFF00C6FF, 0xFF0072FF)
      ),
      EcosystemApp(
        name = "Nervos DAO",
        description = "Deposit your CKB into the system smart contract to offset secondary issuance inflation.",
        websiteUrl = "https://docs.nervos.org/docs/basics/concepts/nervos-dao",
        twitterUrl = "https://x.com/NervosNetwork",
        githubUrl = "https://github.com/nervosnetwork",
        discordUrl = "https://discord.gg/nervos",
        bannerGradientColors = listOf(0xFF1F1C2C, 0xFF928DAB)
      )
    )
  }
}
