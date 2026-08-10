package com.loe159.rekordbot.mobile.domain.repository

import com.loe159.rekordbot.mobile.domain.shazam.ShazamInboxCounts
import com.loe159.rekordbot.mobile.domain.shazam.ShazamInboxTrack
import com.loe159.rekordbot.mobile.domain.shazam.ShazamPlaylistTrack
import com.loe159.rekordbot.mobile.domain.shazam.ShazamSyncResult
import kotlinx.coroutines.flow.Flow

interface ShazamInboxRepository {
    fun observeAll(): Flow<List<ShazamInboxTrack>>

    fun observePending(): Flow<List<ShazamInboxTrack>>

    fun observeIgnored(): Flow<List<ShazamInboxTrack>>

    fun observeCounts(): Flow<ShazamInboxCounts>

    suspend fun synchronize(tracks: List<ShazamPlaylistTrack>): ShazamSyncResult

    suspend fun markSaved(spotifyTrackId: String): Boolean

    suspend fun markIgnored(spotifyTrackId: String): Boolean

    suspend fun markAlreadyPresent(spotifyTrackId: String): Boolean

    suspend fun reopen(spotifyTrackId: String): Boolean
}
