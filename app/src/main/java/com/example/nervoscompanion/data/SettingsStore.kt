package com.example.nervoscompanion.data

import android.content.Context
import android.content.SharedPreferences

class SettingsStore(context: Context) {
  private val prefs: SharedPreferences = context.getSharedPreferences("nervos_prefs", Context.MODE_PRIVATE)

  var rpcUrl: String
    get() = prefs.getString("rpc_url", "https://mainnet.ckb.dev/") ?: "https://mainnet.ckb.dev/"
    set(value) = prefs.edit().putString("rpc_url", value).apply()

  var rpcNetwork: String
    get() = prefs.getString("rpc_network", "mainnet") ?: "mainnet"
    set(value) = prefs.edit().putString("rpc_network", value).apply()

  var themeName: String
    get() = prefs.getString("theme_name", "emerald") ?: "emerald"
    set(value) = prefs.edit().putString("theme_name", value).apply()

  var configBaseUrl: String
    get() = prefs.getString("config_base_url", "https://raw.githubusercontent.com/nervosnetwork/community/main/") ?: "https://raw.githubusercontent.com/nervosnetwork/community/main/"
    set(value) = prefs.edit().putString("config_base_url", value).apply()

  fun getFavouriteApps(): Set<String> {
    return prefs.getStringSet("favourite_apps", emptySet())?.toSet() ?: emptySet()
  }

  fun toggleFavouriteApp(appName: String) {
    val current = getFavouriteApps().toMutableSet()
    if (current.contains(appName)) {
      current.remove(appName)
    } else {
      current.add(appName)
    }
    prefs.edit().putStringSet("favourite_apps", current).apply()
  }
}
