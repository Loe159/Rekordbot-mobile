package com.loe159.rekordbot.mobile.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.loe159.rekordbot.mobile.domain.queue.QueueWorkScheduler
import java.util.concurrent.TimeUnit

class WorkManagerQueueScheduler(context: Context) : QueueWorkScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<QueuedTrackWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag(QUEUE_WORK_NAME)
            .build()
        workManager.enqueueUniqueWork(
            QUEUE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    private companion object {
        const val QUEUE_WORK_NAME = "airtable-track-queue"
    }
}
