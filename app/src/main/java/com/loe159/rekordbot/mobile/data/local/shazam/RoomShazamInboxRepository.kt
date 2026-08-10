package com.loe159.rekordbot.mobile.data.local.shazam

import com.loe159.rekordbot.mobile.domain.repository.ShazamInboxRepository
import com.loe159.rekordbot.mobile.domain.shazam.ShazamDecision
import com.loe159.rekordbot.mobile.domain.shazam.ShazamInboxCounts
import com.loe159.rekordbot.mobile.domain.shazam.ShazamInboxTrack
import com.loe159.rekordbot.mobile.domain.shazam.ShazamPlaylistTrack
import com.loe159.rekordbot.mobile.domain.shazam.ShazamSyncResult
import com.loe159.rekordbot.mobile.domain.shazam.distinctForInbox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomShazamInboxRepository(
    private val dao: ShazamInboxDao,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : ShazamInboxRepository {
    override fun observeAll(): Flow<List<ShazamInboxTrack>> =
        dao.observeAll().map { entities -> entities.map(ShazamInboxEntity::toDomain) }

    override fun observePending(): Flow<List<ShazamInboxTrack>> =
        observeByDecision(ShazamDecision.PENDING)

    override fun observeIgnored(): Flow<List<ShazamInboxTrack>> =
        observeByDecision(ShazamDecision.IGNORED)

    override fun observeCounts(): Flow<ShazamInboxCounts> =
        dao.observeCounts().map { counts ->
            ShazamInboxCounts(
                pending = counts.pending,
                saved = counts.saved,
                ignored = counts.ignored,
                alreadyPresent = counts.alreadyPresent,
            )
        }

    override suspend fun synchronize(tracks: List<ShazamPlaylistTrack>): ShazamSyncResult {
        val distinctTracks = tracks.distinctForInbox()
        val now = currentTimeMillis()
        val addedCount = dao.synchronize(distinctTracks.map { it.toEntity(now) })
        return ShazamSyncResult(
            receivedCount = tracks.size,
            distinctCount = distinctTracks.size,
            addedCount = addedCount,
        )
    }

    override suspend fun markSaved(spotifyTrackId: String): Boolean =
        markDecision(spotifyTrackId, ShazamDecision.SAVED)

    override suspend fun markIgnored(spotifyTrackId: String): Boolean =
        markDecision(spotifyTrackId, ShazamDecision.IGNORED)

    override suspend fun markAlreadyPresent(spotifyTrackId: String): Boolean =
        markDecision(spotifyTrackId, ShazamDecision.ALREADY_PRESENT)

    override suspend fun reopen(spotifyTrackId: String): Boolean =
        markDecision(spotifyTrackId, ShazamDecision.PENDING)

    private fun observeByDecision(decision: ShazamDecision): Flow<List<ShazamInboxTrack>> =
        dao.observeByDecision(decision)
            .map { entities -> entities.map(ShazamInboxEntity::toDomain) }

    private suspend fun markDecision(
        spotifyTrackId: String,
        decision: ShazamDecision,
    ): Boolean {
        if (spotifyTrackId.isBlank()) return false
        return dao.markDecision(
            spotifyTrackId = spotifyTrackId.trim(),
            decision = decision,
            now = currentTimeMillis(),
        ) == 1
    }
}
