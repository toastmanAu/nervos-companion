package com.example.nervoscompanion.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.Toast
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.nervoscompanion.MainActivity
import com.example.nervoscompanion.R
import com.example.nervoscompanion.data.cache.AppDatabase
import com.example.nervoscompanion.data.work.BlockchainWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class NervosCompanionWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    // Launch coroutine to retrieve latest database stats and update views
    CoroutineScope(Dispatchers.Main).launch {
      val db = AppDatabase.getDatabase(context)
      val stats = db.chainStatsDao().getStats()

      for (appWidgetId in appWidgetIds) {
        val views = RemoteViews(context.packageName, R.layout.nervos_companion_widget)

        // Setup click intent for Refresh Button
        val refreshIntent = Intent(context, NervosCompanionWidgetProvider::class.java).apply {
          action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
          context,
          appWidgetId,
          refreshIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

        // Setup click intent for Widget Body (Opens app)
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
          context,
          appWidgetId,
          mainIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)

        // Populate dynamic views from cached stats
        if (stats != null) {
          // 1. CKB Price
          if (stats.ckbPrice != null) {
            views.setTextViewText(R.id.widget_price, "$%.4f".format(stats.ckbPrice))
          } else {
            views.setTextViewText(R.id.widget_price, "$--.----")
          }

          // 2. 24h Change
          if (stats.ckbChange != null) {
            val changeText = "%+.2f%%".format(stats.ckbChange)
            views.setTextViewText(R.id.widget_price_change, changeText)
            if (stats.ckbChange >= 0.0) {
              views.setTextColor(R.id.widget_price_change, Color.parseColor("#00CC99"))
            } else {
              views.setTextColor(R.id.widget_price_change, Color.parseColor("#FF3366"))
            }
          } else {
            views.setTextViewText(R.id.widget_price_change, "--.--%")
            views.setTextColor(R.id.widget_price_change, Color.parseColor("#88FFFFFF"))
          }

          // 3. Block Height
          if (stats.blockNumber != null) {
            views.setTextViewText(R.id.widget_block_height, "#%,d".format(stats.blockNumber))
          } else {
            views.setTextViewText(R.id.widget_block_height, "#-------")
          }

          // 4. Node Version
          views.setTextViewText(R.id.widget_node_version, stats.nodeVersion ?: "Public Node")

          // 5. Epoch Info
          if (stats.epochNumber != null) {
            views.setTextViewText(R.id.widget_epoch, "Epoch ${stats.epochNumber}")
          } else {
            views.setTextViewText(R.id.widget_epoch, "Epoch ----")
          }
          views.setTextViewText(R.id.widget_epoch_progress, stats.epochProgress ?: "--- / ---")

          // 6. Halving Countdown
          val ep = stats.epochNumber
          if (ep != null) {
            val nextHalvingEpoch = ((ep / 8760L) + 1L) * 8760L
            val epochsRemaining = nextHalvingEpoch - ep
            val hoursRemaining = epochsRemaining * 4L
            val daysRemaining = hoursRemaining / 24L
            val remainingHours = hoursRemaining % 24L

            views.setTextViewText(R.id.widget_halving_countdown, "~$daysRemaining days, ${remainingHours}h")

            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.add(Calendar.HOUR_OF_DAY, hoursRemaining.toInt())
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
              timeZone = TimeZone.getTimeZone("UTC")
            }
            views.setTextViewText(R.id.widget_halving_est_date, "Est: ${sdf.format(calendar.time)}")
          } else {
            views.setTextViewText(R.id.widget_halving_countdown, "~--- days")
            views.setTextViewText(R.id.widget_halving_est_date, "Est: ----")
          }

          // 7. Last Updated Time
          val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
          views.setTextViewText(R.id.widget_last_updated, "Updated: ${timeSdf.format(stats.lastUpdated)}")

        } else {
          // Defaults if database is empty/null
          views.setTextViewText(R.id.widget_price, "$--.----")
          views.setTextViewText(R.id.widget_price_change, "--.--%")
          views.setTextColor(R.id.widget_price_change, Color.parseColor("#88FFFFFF"))
          views.setTextViewText(R.id.widget_block_height, "#-------")
          views.setTextViewText(R.id.widget_node_version, "No data synced")
          views.setTextViewText(R.id.widget_epoch, "Epoch ----")
          views.setTextViewText(R.id.widget_epoch_progress, "--- / ---")
          views.setTextViewText(R.id.widget_halving_countdown, "~--- days")
          views.setTextViewText(R.id.widget_halving_est_date, "Est: ----")
          views.setTextViewText(R.id.widget_last_updated, "Updated: --:--")
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
      }
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    if (intent.action == ACTION_REFRESH) {
      // Trigger background sync immediately via WorkManager
      val request = OneTimeWorkRequestBuilder<BlockchainWorker>().build()
      WorkManager.getInstance(context).enqueueUniqueWork(
        "BlockchainWidgetRefresh",
        ExistingWorkPolicy.REPLACE,
        request
      )
      Toast.makeText(context, "Refreshing Blockchain Stats...", Toast.LENGTH_SHORT).show()
    }
  }

  companion object {
    const val ACTION_REFRESH = "com.example.nervoscompanion.ui.widget.ACTION_REFRESH"

    fun triggerUpdate(context: Context) {
      val intent = Intent(context, NervosCompanionWidgetProvider::class.java).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
      }
      val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
        ComponentName(context, NervosCompanionWidgetProvider::class.java)
      )
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
      context.sendBroadcast(intent)
    }
  }
}
