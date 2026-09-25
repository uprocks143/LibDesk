package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.data.repository.NcertRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class NcertCatalogSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "NcertCatalogSyncWorker"
        const val WORK_NAME = "weekly_ncert_catalog_sync"

        fun scheduleWeekly(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<NcertCatalogSyncWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repo = NcertRepository(applicationContext)
        val result = repo.refreshCatalog()
        if (result.isSuccess) {
            Log.d(TAG, "Weekly NCERT catalog refresh succeeded.")
            Result.success()
        } else {
            Log.w(TAG, "Weekly NCERT catalog refresh failed, retrying.")
            Result.retry()
        }
    }
}
