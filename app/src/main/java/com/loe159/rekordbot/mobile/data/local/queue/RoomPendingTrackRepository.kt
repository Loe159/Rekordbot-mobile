package com.loe159.rekordbot.mobile.data.local.queue

import com.loe159.rekordbot.mobile.data.local.PendingTrackStore
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.domain.queue.QueuedTrackOperation
import com.loe159.rekordbot.mobile.domain.queue.QueueStatus
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

    override suspend fun saveDraft(operationId: String, track: TrackDraft): Result<String> =
        runCatching {
            require(operationId.isNotBlank()) { "Identifiant de brouillon manquant." }
            dao.insertIfAbsent(
                track.toEntity(
                    operationId = operationId,
                    initialError = "",
                    now = currentTimeMillis(),
                    status = QueueStatus.DRAFT,
                ),
            )
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
        val status = dao.get(operationId)?.status ?: return false
        if (status !in setOf(QueueStatus.DRAFT, QueueStatus.PENDING, QueueStatus.FAILED)) {
            return false
        }
        if (status != QueueStatus.DRAFT && !draft.isReadyForAirtable) return false
        return dao.updateDraft(
            operationId = operationId,
            spotifyTrackId = draft.spotifyTrackId,
            title = draft.title,
            artist = draft.artist,
            spotifyUrl = draft.spotifyUrl,
            isrc = draft.isrc,
            rawGenre = draft.rawGenre,
            energy = draft.energy,
            moods = draft.moods,
            situations = draft.situations,
            inspirationalDjs = draft.inspirationalDjs,
            comment = draft.comment,
            now = currentTimeMillis(),
        ) == 1
    }

    override suspend fun sendDraft(operationId: String): Boolean {
        val draft = dao.get(operationId)
            ?.takeIf { it.status == QueueStatus.DRAFT }
            ?.toDomain()
            ?.draft ?: return false
        if (!draft.isReadyForAirtable) return false
        return dao.sendDraft(operationId, currentTimeMillis()) == 1
    }

    override suspend fun recoverInterrupted() {
        dao.recoverInterrupted(currentTimeMillis())
    }
}
