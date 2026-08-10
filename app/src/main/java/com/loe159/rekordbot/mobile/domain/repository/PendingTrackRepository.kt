package com.loe159.rekordbot.mobile.domain.repository

import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import kotlinx.coroutines.flow.Flow

interface PendingTrackRepository {
    fun observePendingTracks(): Flow<List<TrackDraft>>

    suspend fun enqueue(track: TrackDraft): Result<Unit>
}

