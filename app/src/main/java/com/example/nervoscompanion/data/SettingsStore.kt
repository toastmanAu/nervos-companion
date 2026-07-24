package com.example.nervoscompanion.data

import android.content.Context
import android.content.SharedPreferences

class SettingsStore(private val context: Context) {
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

  var isBackgroundSyncEnabled: Boolean
    get() = prefs.getBoolean("bg_sync_enabled", true)
    set(value) = prefs.edit().putBoolean("bg_sync_enabled", value).apply()

  var notificationBlockThreshold: Long
    get() = prefs.getLong("notification_block_threshold", 100000L)
    set(value) = prefs.edit().putLong("notification_block_threshold", value).apply()

  var fiberRpcUrl: String
    get() = prefs.getString("fiber_rpc_url", "http://127.0.0.1:8231/") ?: "http://127.0.0.1:8231/"
    set(value) = prefs.edit().putString("fiber_rpc_url", value).apply()

  var fiberAuthToken: String
    get() = prefs.getString("fiber_auth_token", "") ?: ""
    set(value) = prefs.edit().putString("fiber_auth_token", value).apply()

  var isForumNotificationsEnabled: Boolean
    get() = prefs.getBoolean("forum_notifications_enabled", true)
    set(value) = prefs.edit().putBoolean("forum_notifications_enabled", value).apply()

  var isReleaseNotificationsEnabled: Boolean
    get() = prefs.getBoolean("release_notifications_enabled", true)
    set(value) = prefs.edit().putBoolean("release_notifications_enabled", value).apply()

  var localRfcPath: String
    get() {
      val saved = prefs.getString("local_rfc_path", null)
      if (saved == null || saved == "/home/phill/Downloads/rfcs-master") {
        return context.filesDir.absolutePath
      }
      return saved
    }
    set(value) = prefs.edit().putString("local_rfc_path", value).apply()

  var lastSeenForumPostId: Int
    get() = prefs.getInt("last_seen_forum_post_id", 0)
    set(value) = prefs.edit().putInt("last_seen_forum_post_id", value).apply()

  var lastSeenCkbReleaseTag: String
    get() = prefs.getString("last_seen_ckb_release_tag", "") ?: ""
    set(value) = prefs.edit().putString("last_seen_ckb_release_tag", value).apply()

  var lastSeenFiberReleaseTag: String
    get() = prefs.getString("last_seen_fiber_release_tag", "") ?: ""
    set(value) = prefs.edit().putString("last_seen_fiber_release_tag", value).apply()

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

  fun getReleaseAlertApps(): Set<String> {
    return prefs.getStringSet("release_alert_apps", emptySet())?.toSet() ?: emptySet()
  }

  fun toggleReleaseAlertApp(appName: String) {
    val current = getReleaseAlertApps().toMutableSet()
    if (current.contains(appName)) {
      current.remove(appName)
    } else {
      current.add(appName)
    }
    prefs.edit().putStringSet("release_alert_apps", current).apply()
  }

  fun getLastSeenReleaseTag(owner: String, repo: String): String {
    return prefs.getString("github_release_seen_${owner.lowercase()}_${repo.lowercase()}", "") ?: ""
  }

  fun setLastSeenReleaseTag(owner: String, repo: String, tag: String) {
    prefs.edit().putString("github_release_seen_${owner.lowercase()}_${repo.lowercase()}", tag).apply()
  }

  fun getRecentTransactions(): List<String> {
    val raw = prefs.getString("recent_transactions", "") ?: ""
    if (raw.isEmpty()) return emptyList()
    return raw.split(",")
  }

  fun addRecentTransaction(txHash: String) {
    val current = getRecentTransactions().toMutableList()
    current.remove(txHash) // avoid duplicates
    current.add(0, txHash) // add to top
    if (current.size > 10) {
      current.removeLast() // keep max 10
    }
    prefs.edit().putString("recent_transactions", current.joinToString(",")).apply()
  }

  fun getTrackedAddresses(): List<String> {
    val raw = prefs.getString("tracked_addresses", "") ?: ""
    if (raw.isEmpty()) return emptyList()
    return raw.split(",")
  }

  fun addTrackedAddress(address: String) {
    val current = getTrackedAddresses().toMutableList()
    if (!current.contains(address)) {
      current.add(address)
      prefs.edit().putString("tracked_addresses", current.joinToString(",")).apply()
    }
  }

  fun removeTrackedAddress(address: String) {
    val current = getTrackedAddresses().toMutableList()
    if (current.remove(address)) {
      prefs.edit().putString("tracked_addresses", current.joinToString(",")).apply()
    }
  }

  fun getNotifiedOutpoints(): Set<String> {
    return prefs.getStringSet("notified_outpoints", emptySet())?.toSet() ?: emptySet()
  }

  fun addNotifiedOutpoint(outpoint: String) {
    val current = getNotifiedOutpoints().toMutableSet()
    current.add(outpoint)
    prefs.edit().putStringSet("notified_outpoints", current).apply()
  }

  var lastScannedTipEpochFraction: Float
    get() = prefs.getFloat("last_scanned_tip_epoch", 0f)
    set(value) = prefs.edit().putFloat("last_scanned_tip_epoch", value).apply()

  fun getGeneratedInvoices(): List<String> {
    val raw = prefs.getString("generated_invoices", "") ?: ""
    if (raw.isEmpty()) return emptyList()
    return raw.split(",")
  }

  fun addGeneratedInvoice(invoice: String) {
    val current = getGeneratedInvoices().toMutableList()
    current.remove(invoice)
    current.add(0, invoice)
    if (current.size > 20) {
      current.removeLast()
    }
    prefs.edit().putString("generated_invoices", current.joinToString(",")).apply()
  }

  fun getGeneratedInvoice(invoice: String): String? {
    return prefs.getString("invoice_$invoice", null)
  }

  fun getCardSkinType(cardId: String): SkinType {
    val raw = prefs.getString("card_skin_type_$cardId", SkinType.DEFAULT.name) ?: SkinType.DEFAULT.name
    return try { SkinType.valueOf(raw) } catch (e: Exception) { SkinType.DEFAULT }
  }

  fun setCardSkinType(cardId: String, type: SkinType) {
    prefs.edit().putString("card_skin_type_$cardId", type.name).apply()
  }

  fun getCardSkinPath(cardId: String): String? {
    return prefs.getString("card_skin_path_$cardId", null)
  }

  fun setCardSkinPath(cardId: String, path: String?) {
    prefs.edit().putString("card_skin_path_$cardId", path).apply()
  }

  fun getCardScaleType(cardId: String): ScaleType {
    val raw = prefs.getString("card_scale_type_$cardId", ScaleType.CROP.name) ?: ScaleType.CROP.name
    return try { ScaleType.valueOf(raw) } catch (e: Exception) { ScaleType.CROP }
  }

  fun setCardScaleType(cardId: String, scale: ScaleType) {
    prefs.edit().putString("card_scale_type_$cardId", scale.name).apply()
  }

  fun clearAllReskins() {
    val editor = prefs.edit()
    val allKeys = prefs.all.keys
    for (key in allKeys) {
      if (key.startsWith("card_skin_type_") || key.startsWith("card_skin_path_") || key.startsWith("card_scale_type_")) {
        editor.remove(key)
      }
    }
    editor.apply()
  }
}

enum class SkinType { DEFAULT, WEBSITE, CUSTOM }
enum class ScaleType { CROP, STRETCH }
