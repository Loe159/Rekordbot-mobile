package com.loe159.rekordbot.mobile.data.local.queue

import com.loe159.rekordbot.mobile.data.local.PendingTrackStore
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.domain.queue.QueuedTrackOperation
import com.loe159.rekordbot.mobile.domain.repository.PendingTrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPendingTrackRepository(
    private val dao: QueuedTrackDao,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : PendingTrackRepository, PendingTrackStore {
    override fun observeAll(): Flow<List<QueuedTrackOperation>> =
        dao.observeAll().map { entities -> entities.map(QueuedTrackEntity::toDomain) }

    override fun observeOpenCount(): Flow<Int> = dao.observeOpenCount()

    override suspend fun enqueue(
        operationId: String,
        track: TrackDraft,
        initialError: String,
    ): Result<String> = runCatching {
        require(operationId.isNotBlank()) { "Identifiant d’opération manquant." }
        require(track.isReadyForAirtable) { "Le morceau est incomplet." }
        dao.insertIfAbsent(track.toEntity(operationId, initialError, currentTimeMillis()))
        operationId
    }

    override suspend fun claimNext(): QueuedTrackOperation? =
        dao.claimNext(currentTimeMillis())?.toDomain()

    override suspend fun markSent(operationId: String, recordId: String) {
        dao.markSent(operationId, recordId, currentTimeMillis())
    }

    override suspend fun markFailed(operationId: String, error: String, retryable: Boolean) {
        dao.markFailed(operationId, error, retryable, currentTimeMillis())
    }

    override suspend fun retry(operationId: String): Boolean =
        dao.retry(operationId, currentTimeMillis()) == 1

    override suspend fun delete(operationId: String): Boolean = dao.delete(operationId) == 1

    override suspend fun updateDraft(operationId: String, draft: TrackDraft): Boolean {
        if (!draft.isReadyForAirtable) return false
        return dao.updateDraft(
            operationId = operationId,
            spotifyTrackId = draft.spotifyTrackId,
            title = draft.title,
            artist = draft.artist,
            spotifyUrl = draft.spotifyUrl,
            rawGenre = draft.rawGenre,
            comment = draft.comment,
            now = currentTimeMillis(),
        ) == 1
    }

    override suspend fun recoverInterrupted() {
        dao.recoverInterrupted(currentTimeMillis())
    }
}
