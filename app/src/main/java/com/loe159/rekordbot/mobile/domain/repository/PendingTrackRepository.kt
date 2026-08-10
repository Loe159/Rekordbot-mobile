package com.loe159.rekordbot.mobile.domain.repository

import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.domain.queue.QueuedTrackOperation
import kotlinx.coroutines.flow.Flow

interface PendingTrackRepository {
    fun observeAll(): Flow<List<QueuedTrackOperation>>

    fun observeOpenCount(): Flow<Int>

    suspend fun enqueue(
        operationId: String,
        track: TrackDraft,
        initialError: String,
    ): Result<String>

    suspend fun claimNext(): QueuedTrackOperation?

    suspend fun markSent(operationId: String, recordId: String)

    suspend fun markFailed(operationId: String, error: String, retryable: Boolean)

    suspend fun retry(operationId: String): Boolean

    suspend fun delete(operationId: String): Boolean

    suspend fun updateDraft(operationId: String, draft: TrackDraft): Boolean

    suspend fun recoverInterrupted()
}
