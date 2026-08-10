package com.loe159.rekordbot.mobile.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.loe159.rekordbot.mobile.data.local.SharedPreferencesAirtableConfigurationRepository
import com.loe159.rekordbot.mobile.data.local.queue.RekordbotDatabase
import com.loe159.rekordbot.mobile.data.local.queue.RoomPendingTrackRepository
import com.loe159.rekordbot.mobile.data.remote.airtable.DirectAirtableGateway
import com.loe159.rekordbot.mobile.domain.queue.QueueProcessingResult
import com.loe159.rekordbot.mobile.domain.queue.QueuedTrackProcessor

class QueuedTrackWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val pendingRepository = RoomPendingTrackRepository(
            RekordbotDatabase.getInstance(applicationContext).queuedTrackDao(),
        )
        pendingRepository.recoverInterrupted()
        val processor = QueuedTrackProcessor(
            pendingTrackRepository = pendingRepository,
            configurationRepository = SharedPreferencesAirtableConfigurationRepository(
                applicationContext,
            ),
            airtableGateway = DirectAirtableGateway(),
        )

        while (!isStopped) {
            when (processor.processNext()) {
                QueueProcessingResult.Empty -> return Result.success()
                QueueProcessingResult.Sent,
                QueueProcessingResult.PermanentlyFailed -> Unit
                QueueProcessingResult.RetryLater -> return Result.retry()
            }
        }
        return Result.retry()
    }
}
