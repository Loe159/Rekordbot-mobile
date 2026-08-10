package com.loe159.rekordbot.mobile.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class WorkManagerShazamSyncScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<ShazamSyncWorker>(
            MINIMUM_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag(PERIODIC_WORK_NAME)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun scheduleNow() {
        val request = OneTimeWorkRequestBuilder<ShazamSyncWorker>()
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag(IMMEDIATE_WORK_NAME)
            .build()
        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun networkConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private companion object {
        const val MINIMUM_INTERVAL_MINUTES = 15L
        const val PERIODIC_WORK_NAME = "shazam-playlist-periodic-sync"
        const val IMMEDIATE_WORK_NAME = "shazam-playlist-immediate-sync"
    }
}
