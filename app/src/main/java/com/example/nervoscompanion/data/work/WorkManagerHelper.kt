package com.example.nervoscompanion.data.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkManagerHelper {
  private const val WORK_NAME = "blockchain_polling_work"

  fun schedule(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<BlockchainWorker>(
      15, TimeUnit.MINUTES
    )
    .build()

    WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
      WORK_NAME,
      ExistingPeriodicWorkPolicy.KEEP,
      workRequest
    )
  }

  fun cancel(context: Context) {
    WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
  }
}
